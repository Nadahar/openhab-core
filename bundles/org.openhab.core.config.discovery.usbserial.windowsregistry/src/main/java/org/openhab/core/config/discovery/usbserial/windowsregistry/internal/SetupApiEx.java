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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Extra {@code setupapi.dll} mappings not defined in {@link SetupApi}.
 *
 * @author Ravi Nadahar - Initial contribution.
 */
public interface SetupApiEx extends SetupApi {

    /** The instance. */
    SetupApiEx INSTANCE = Native.load("setupapi", SetupApiEx.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean SetupDiGetDeviceInstanceId(HANDLE DeviceInfoSet, SP_DEVINFO_DATA DeviceInfoData, Memory DeviceInstanceId,
        int DeviceInstanceIdSize,
        IntByReference RequiredSize
      );
}
