/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.config.discovery.usbserial.windowsregistry.internal;

import com.sun.jna.platform.win32.DBT;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_DEVICEINTERFACE;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_HANDLE;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_HDR;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_OEM;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_PORT;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_VOLUME;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HDEVNOTIFY;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import com.sun.jna.platform.win32.Wtsapi32;

public class WindowMessageHandler implements Runnable, WindowProc {

    @Override
    public void run() {
        String windowClass = "OHMessageHandlerWindowClass";
        HMODULE hInst = Kernel32.INSTANCE.GetModuleHandle("");

        WNDCLASSEX wClass = new WNDCLASSEX();
        wClass.hInstance = hInst;
        wClass.lpfnWndProc = WindowMessageHandler.this;
        wClass.lpszClassName = windowClass;

        // register window class
        User32.INSTANCE.RegisterClassEx(wClass);

        HWND hWnd = User32.INSTANCE
            .CreateWindowEx(
                    User32.WS_EX_TOPMOST,
                    windowClass,
                    "OH helper window, used only to catch window events",
                    0, 0, 0, 0, 0,
                    null, // WM_DEVICECHANGE contradicts parent=WinUser.HWND_MESSAGE
                    null, hInst, null);

        DEV_BROADCAST_DEVICEINTERFACE notificationFilter = new DEV_BROADCAST_DEVICEINTERFACE();
        notificationFilter.dbcc_size = notificationFilter.size();
        notificationFilter.dbcc_devicetype = DBT.DBT_DEVTYP_DEVICEINTERFACE;
        notificationFilter.dbcc_classguid = DBT.GUID_DEVINTERFACE_USB_DEVICE;

        HDEVNOTIFY hDevNotify = User32.INSTANCE.RegisterDeviceNotification(hWnd, notificationFilter, User32.DEVICE_NOTIFY_WINDOW_HANDLE);
        if (hDevNotify != null) {
            System.out.println("RegisterDeviceNotification was sucessfully!");
        }

        MSG msg = new MSG();
        while (User32.INSTANCE.GetMessage(msg, hWnd, 0, 0) > 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }

        User32.INSTANCE.UnregisterDeviceNotification(hDevNotify);
        Wtsapi32.INSTANCE.WTSUnRegisterSessionNotification(hWnd);
        User32.INSTANCE.UnregisterClass(windowClass, hInst);
        User32.INSTANCE.DestroyWindow(hWnd);

    }

    @Override
    public LRESULT callback(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {// WM_DEVICECHANGE
        switch (uMsg) {
            case WinUser.WM_CREATE:
                return new LRESULT(0);
            case WinUser.WM_DESTROY:
                User32.INSTANCE.PostQuitMessage(0);
                return new LRESULT(0);
            case WinUser.WM_DEVICECHANGE: {
                LRESULT lResult = this.onDeviceChange(wParam, lParam);
                return lResult != null ? lResult :
                    User32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
            }
            default:
                return User32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);

        }
    }

    protected LRESULT onDeviceChange(WPARAM wParam, LPARAM lParam) {
        switch (wParam.intValue()) {
            case DBT.DBT_DEVICEARRIVAL: {
                return onDeviceChangeArrivalOrRemoveComplete(lParam, "Arrival");
            }
            case DBT.DBT_DEVICEREMOVECOMPLETE: {
                return onDeviceChangeArrivalOrRemoveComplete(lParam, "Remove Complete");
            }
            case DBT.DBT_DEVNODES_CHANGED: {
                // LRESULT(!) aka TRUE means that the message was processed. This message is non-specific
                // (basically means "something changed"), and we don't want to take any action.
                return new LRESULT(1);
            }
            default:
                System.out.println(
                    "Message WM_DEVICECHANGE message received, value unhandled.");
        }
        return null;
    }

    protected LRESULT onDeviceChangeArrivalOrRemoveComplete(LPARAM lParam, String action) {
        DEV_BROADCAST_HDR bhdr = new DEV_BROADCAST_HDR(lParam.longValue());
        switch (bhdr.dbch_devicetype) {
            case DBT.DBT_DEVTYP_DEVICEINTERFACE: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363244.aspx
                DEV_BROADCAST_DEVICEINTERFACE bdif = new DEV_BROADCAST_DEVICEINTERFACE(bhdr.getPointer());
                System.out.println("BROADCAST_DEVICEINTERFACE: " + action);
                System.out.println("dbcc_devicetype: " + bdif.dbcc_devicetype);
                System.out.println("dbcc_name:       " + bdif.getDbcc_name());
                System.out.println("dbcc_classguid:  " + bdif.dbcc_classguid.toGuidString());
                break;
            }
            case DBT.DBT_DEVTYP_HANDLE: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363245.aspx
                DEV_BROADCAST_HANDLE bhd = new DEV_BROADCAST_HANDLE(bhdr.getPointer());
                System.out.println("BROADCAST_HANDLE: " + action);
                break;
            }
            case DBT.DBT_DEVTYP_OEM: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363247.aspx
                DEV_BROADCAST_OEM boem = new DEV_BROADCAST_OEM(bhdr.getPointer());
                System.out.println("BROADCAST_OEM: " + action);
                break;
            }
            case DBT.DBT_DEVTYP_PORT: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363248.aspx
                DEV_BROADCAST_PORT bpt = new DEV_BROADCAST_PORT(bhdr.getPointer());
                System.out.println("BROADCAST_PORT:  " + action);
                System.out.println("dbcp_devicetype: " + bpt.dbcp_devicetype);
                System.out.println("dbcp_name:       " + bpt.getDbcpName());
                break;
            }
            case DBT.DBT_DEVTYP_VOLUME: {
                // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363249.aspx
                DEV_BROADCAST_VOLUME bvl = new DEV_BROADCAST_VOLUME(bhdr.getPointer());
                int logicalDriveAffected = bvl.dbcv_unitmask;
                short flag = bvl.dbcv_flags;
                boolean isMediaNotPhysical = 0 != (flag & DBT.DBTF_MEDIA/*value is 1*/);
                boolean isNet = 0 != (flag & DBT.DBTF_NET/*value is 2*/);
                System.out.println(action);
                int driveLetterIndex = 0;
                while (logicalDriveAffected != 0) {
                    if (0 != (logicalDriveAffected & 1)) {
                        System.out.println("Logical Drive Letter: " +
                            ((char) ('A' + driveLetterIndex)));
                    }
                    logicalDriveAffected >>>= 1;
                    driveLetterIndex++;
                }
                System.out.println("isMediaNotPhysical:"+isMediaNotPhysical);
                System.out.println("isNet:"+isNet);
                break;
            }
            default:
                return null;
        }
        // return TRUE means processed message for this wParam.
        // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363205.aspx
        // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363208.aspx
        return new LRESULT(1);
    }
}
