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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.DBT;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_DEVICEINTERFACE;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_HANDLE;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_HDR;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_OEM;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_PORT;
import com.sun.jna.platform.win32.DBT.DEV_BROADCAST_VOLUME;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HDEVNOTIFY;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.platform.win32.WinUser.WindowProc;

public class WindowMessageHandler implements Runnable, WindowProc {

    private final Logger logger = LoggerFactory.getLogger(WindowMessageHandler.class);

    private final AtomicInteger threadCounter = new AtomicInteger(0);

    private final Set<WindowMessageListener> listeners = ConcurrentHashMap.newKeySet();

    /** A Windows event that can be used to stop the message loop */
    private final HANDLE terminateEvent = Kernel32.INSTANCE.CreateEvent(null, false, false, null);

    public boolean addListener(WindowMessageListener listener) {
        return listeners.add(listener);
    }

    public boolean removeListener(WindowMessageListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        currentThread.setName("OH-window-message-handler");

        // Create and register window class
        String windowClass = "OHMessageHandlerWindowClass";
        User32Ex user32 = User32Ex.INSTANCE;
        HMODULE hInst = Kernel32.INSTANCE.GetModuleHandle("");
//        if (hInst == null) {
//            logger.debug("Failed to get module handle, aborting message window creation");
//            notifyTerminate();
//            currentThread.setName(threadName);
//            return;
//        }
        WNDCLASSEX wClass = new WNDCLASSEX();
        wClass.hInstance = hInst;
        wClass.lpfnWndProc = WindowMessageHandler.this;
        wClass.lpszClassName = windowClass;
        if (user32.RegisterClassEx(wClass).intValue() == 0) {
            logger.debug("Failed to register window class, aborting message window creation");
            notifyTerminate();
            currentThread.setName(threadName);
            return;
        }

        HWND hWnd = null;
        HDEVNOTIFY hDevNotify = null;
        try {
            // Parent can't be the recommended HWND_MESSAGE, because WM_DEVICECHANGE is a broadcast message,
            // which aren't sent to message-only windows.
            hWnd = user32.CreateWindowEx(
                        User32.WS_EX_TOPMOST,
                        windowClass,
                        "OH helper window, used only to receive window events",
                        0, 0, 0, 0, 0,
                        null, null, hInst, null);
            if (hWnd == null) {
                logger.debug("Failed to create window, aborting message window creation");
                notifyTerminate();
                return;
            }

            DEV_BROADCAST_DEVICEINTERFACE notificationFilter = new DEV_BROADCAST_DEVICEINTERFACE();
            notificationFilter.dbcc_size = notificationFilter.size();
            notificationFilter.dbcc_devicetype = DBT.DBT_DEVTYP_DEVICEINTERFACE;
            notificationFilter.dbcc_classguid = DBT.GUID_DEVINTERFACE_USB_DEVICE;

            hDevNotify = user32.RegisterDeviceNotification(hWnd, notificationFilter, User32.DEVICE_NOTIFY_WINDOW_HANDLE);
            if (hDevNotify != null) {
                System.out.println("RegisterDeviceNotification was sucessfully!");
            }

            MSG msg = new MSG();
            HANDLE[] handles = new HANDLE[] {terminateEvent};
            boolean running = true;
            int waitResult;
            while (running) {
                switch (waitResult = user32.MsgWaitForMultipleObjects(handles.length, handles, false, WinBase.INFINITE, User32Ex.QS_ALLINPUT)) {
                    case User32Ex.WAIT_OBJECT_0:
                        // Terminate
                        logger.debug("Terminate event received, terminating message loop");
                        user32.PostQuitMessage(0);
                        running = false;
                        break;
                    case User32Ex.WAIT_OBJECT_0 + 1:
                        // Process the message queue
                        while (user32.PeekMessage(msg, hWnd, 0, 0, User32Ex.PM_REMOVE)) {
                            if (msg.message == WinUser.WM_QUIT) {
                                user32.PostQuitMessage(msg.wParam.intValue());
                                running = false;
                                break;
                            }
                            user32.TranslateMessage(msg);
                            user32.DispatchMessage(msg);
                        }
                        break;
                    default:
                        // TODO: (Nad) Error..
                        logger.error("Unexpected return value: {}", waitResult);
                        running = false;
                        break;
                }
            }
        } finally {
            if (hDevNotify != null) {
                user32.UnregisterDeviceNotification(hDevNotify);
            }
            user32.UnregisterClass(windowClass, hInst);
            if (hWnd != null) {
                user32.DestroyWindow(hWnd);
            }

            currentThread.setName(threadName);
        }
    }

    /**
     * Signals the event loop (the {@link #run()} method) that it should terminate.
     */
    public void terminate() {
        Kernel32.INSTANCE.SetEvent(terminateEvent);
    }

    private void notifyTerminate() {
        Set<WindowMessageListener> listeners = Set.copyOf(this.listeners);
        if (!listeners.isEmpty()) {
            createNotificationThread(() -> {
                for (WindowMessageListener listener : listeners) {
                    listener.serviceTerminated();
                }
            }).start();
        }
    }

    private Thread createNotificationThread(Runnable runnable) {
        return new Thread(runnable, "OH-window-message-notifier-" + threadCounter.incrementAndGet());
    }

    @Override
    public LRESULT callback(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {// WM_DEVICECHANGE
        switch (uMsg) {
            case WinUser.WM_CREATE:
                return new LRESULT(0);
            case WinUser.WM_DESTROY:
                User32Ex.INSTANCE.PostQuitMessage(0);
                return new LRESULT(0);
            case WinUser.WM_DEVICECHANGE: {
                LRESULT lResult = onDeviceChange(wParam, lParam);
                return lResult != null ? lResult : User32Ex.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
            }
            default:
                return User32Ex.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
        }
    }

    protected LRESULT onDeviceChange(WPARAM wParam, LPARAM lParam) {
        switch (wParam.intValue()) {
            case DBT.DBT_DEVICEARRIVAL:
                return onDeviceChangeArrivalOrRemoveComplete(lParam, "Arrival");
            case DBT.DBT_DEVICEREMOVECOMPLETE:
                return onDeviceChangeArrivalOrRemoveComplete(lParam, "Remove Complete");
            case DBT.DBT_DEVNODES_CHANGED:
                // LRESULT(1) aka TRUE means that the message was processed. This message is non-specific
                // (basically means "something changed"), so we don't want to take any action.
                return new LRESULT(1);
            default:
                return null;
        }
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

    public interface WindowMessageListener {

        void deviceAdded(String devicePath);

        void deviceRemoved(String devicePath);

        void portAdded(String portName);

        void portRemoved(String portName);

        void serviceTerminated();
    }
}
