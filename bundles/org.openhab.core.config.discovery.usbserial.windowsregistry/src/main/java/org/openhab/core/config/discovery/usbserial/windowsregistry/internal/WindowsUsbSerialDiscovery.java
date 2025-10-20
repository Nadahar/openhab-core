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

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;
import static java.lang.Long.parseLong;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadFactoryBuilder;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.openhab.core.config.discovery.usbserial.windowsregistry.internal.WindowMessageHandler.WindowMessageListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.SetupApi.SP_DEVINFO_DATA;
import com.sun.jna.platform.win32.SetupApi.SP_DEVICE_INTERFACE_DATA;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.platform.win32.Guid.GUID;

/**
 * This is a {@link UsbSerialDiscovery} implementation component for Windows.
 * It parses the Windows registry for USB device entries.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = UsbSerialDiscovery.class, name = WindowsUsbSerialDiscovery.SERVICE_NAME, configurationPid = "discovery.usbserial.windows")
public class WindowsUsbSerialDiscovery implements UsbSerialDiscovery, WindowMessageListener {

    protected static final String SERVICE_NAME = "usb-serial-discovery-windows";
    public static final String SCAN_INTERVAL_PROPERTY = "scanInterval";
    public static final int DEFAULT_SCAN_INTERVAL_SECONDS = 15;

    private final String DEVICE_PATH_PATTERN = "^\\\\\\\\\\?\\\\usb#vid_(?<vid>[0-9a-f]{4})&pid_(?<pid>[0-9a-f]{4})(?:&mi_(?<mi>[0-9a-f]{2}))?#(?<id>.*?)(?:#(?<guid>\\{[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\}))$";
    private final Pattern devicePathPattern = Pattern.compile(DEVICE_PATH_PATTERN);
    private record DevicePathData (int vendorId, int productId, String id, int interfaceNumber) {}
    private static final boolean IS_64_BIT = Platform.is64Bit();
    private static final int ERROR_NO_SUCH_DEVINST = 0xe000020b;

    // registry accessor strings
    private static final String USB_REGISTRY_ROOT = "SYSTEM\\CurrentControlSet\\Enum\\USB";
    private static final String BACKSLASH = "\\";
    private static final String PREFIX_PID = "PID_";
    private static final String PREFIX_VID = "VID_";
    private static final String PREFIX_HEX = "0x";
    private static final String SPLIT_IDS = "&";
    private static final String SPLIT_VALUES = ";";
    private static final String KEY_MANUFACTURER = "Mfg";
    private static final String KEY_PRODUCT = "DeviceDesc";
    private static final String KEY_DEVICE_PARAMETERS = "Device Parameters";
    private static final String KEY_SERIAL_PORT = "PortName";

    private final Logger logger = LoggerFactory.getLogger(WindowsUsbSerialDiscovery.class);
    private final Set<UsbSerialDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();
    private volatile Duration scanInterval = Duration.ofSeconds(DEFAULT_SCAN_INTERVAL_SECONDS);
    private final ScheduledExecutorService scheduler;

    // All access must be guarded by "this"
    private Set<UsbSerialDeviceInformation> lastScanResult = new HashSet<>();

    // All access must be guarded by "this"
    private @Nullable ScheduledFuture<?> scanTask;

    // All access must be guarded by "this"
    private @Nullable WindowMessageHandler windowMessageHandler;

    /** Indicated that listening for device changes using window messages failed */
    private volatile boolean windowMessageFailed;

    @Activate
    public WindowsUsbSerialDiscovery(Map<String, Object> config) {
        Object value = config.get(SCAN_INTERVAL_PROPERTY);
        if (value instanceof String s) {
            try {
                scanInterval = Duration.ofSeconds(parseLong(s));
            } catch (NumberFormatException e) {
                logger.warn("Invalid configuration value for '{}': {}", SCAN_INTERVAL_PROPERTY, s);
            }
        } else if (value instanceof Number n) {
            scanInterval = Duration.ofSeconds(n.longValue());
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(
                ThreadFactoryBuilder.create().withName(SERVICE_NAME).withDaemonThreads(true).build());
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        Object value = config.get(SCAN_INTERVAL_PROPERTY);
        Duration newScanInterval = null;
        if (value instanceof String s) {
            try {
                newScanInterval = Duration.ofSeconds(parseLong(s));
            } catch (NumberFormatException e) {
                logger.warn("Invalid configuration value for '{}': {}", SCAN_INTERVAL_PROPERTY, s);
            }
        } else if (value instanceof Number n) {
            newScanInterval = Duration.ofSeconds(n.longValue());
        }

        synchronized (this) {
            if (!Objects.equals(newScanInterval, scanInterval)) {
                if (newScanInterval == null) {
                    scanInterval = Duration.ofSeconds(DEFAULT_SCAN_INTERVAL_SECONDS);
                } else {
                    scanInterval = newScanInterval;
                }
                if (scanTask != null) {
                    stopBackgroundScanning();
                    startBackgroundScanning();
                }
            }
        }
    }

    @Deactivate
    public void deactivate() {
        synchronized (this) {
            stopBackgroundScanning();
            lastScanResult.clear();
        }
    }

    private void announceAddedDevice(UsbSerialDeviceInformation deviceInfo) {
        for (UsbSerialDiscoveryListener listener : discoveryListeners) {
            listener.usbSerialDeviceDiscovered(deviceInfo);
        }
    }

    private void announceRemovedDevice(UsbSerialDeviceInformation deviceInfo) {
        for (UsbSerialDiscoveryListener listener : discoveryListeners) {
            listener.usbSerialDeviceRemoved(deviceInfo);
        }
    }

    @Override
    public void doSingleScan() {
        doSingleScanInternal(true);
    }

    protected void doSingleScanInternal(boolean includeExisting) {
        Set<UsbSerialDeviceInformation> scanResult;
        Set<UsbSerialDeviceInformation> added;
        Set<UsbSerialDeviceInformation> removed;
        Set<UsbSerialDeviceInformation> unchanged;
        synchronized (this) {
            scanResult = scanAllUsbDevicesInformation();
            added = setDifference(scanResult, lastScanResult);
            removed = setDifference(lastScanResult, scanResult);
            unchanged = includeExisting ? setDifference(scanResult, added) :  Set.of();

            lastScanResult = scanResult;
        }

        removed.forEach(this::announceRemovedDevice);
        added.forEach(this::announceAddedDevice);
        unchanged.forEach(this::announceAddedDevice);
    }

    private <T> Set<T> setDifference(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.removeAll(set2);
        return result;
    }

    @Override
    public void registerDiscoveryListener(UsbSerialDiscoveryListener listener) {
        discoveryListeners.add(listener);
        Set<UsbSerialDeviceInformation> lastScanResult;
        synchronized (this) {
             lastScanResult = Set.copyOf(this.lastScanResult);
        }
        for (UsbSerialDeviceInformation deviceInfo : lastScanResult) {
            listener.usbSerialDeviceDiscovered(deviceInfo);
        }
    }

    @Override
    public void unregisterDiscoveryListener(UsbSerialDiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    /**
     * Traverse the USB tree in Windows registry and return a set of USB device information.
     *
     * @return a set of USB device information.
     */
    public Set<UsbSerialDeviceInformation> scanAllUsbDevicesInformation() {
        if (!Platform.isWindows()) {
            return Set.of();
        }

        GUID GUID_DEVINTERFACE_USB_DEVICE = new GUID("A5DCBF10-6530-11D2-901F-00C04FB951ED");
        int SPDRP_SERVICE = 0x00000004;
        int SPDRP_CLASS = 0x00000007;
        int SPDRP_COMPATIBLEIDS = 0x00000002;
        int SPDRP_HARDWAREID = 0x00000001;
        int SPDRP_ENUMERATOR_NAME = 0x00000016;
        int SPDRP_FRIENDLYNAME = 0x0000000C;
        int SPDRP_MFG = 0x0000000B;
        int SPDRP_PHYSICAL_DEVICE_OBJECT_NAME = 0x0000000E;

        SetupApi apiInst = SetupApi.INSTANCE;

        WinNT.HANDLE deviceInfoSet = apiInst.SetupDiGetClassDevs(GUID_DEVINTERFACE_USB_DEVICE, null, null, SetupApi.DIGCF_DEVICEINTERFACE | SetupApi.DIGCF_PRESENT);
        String serialPort;
        if (!WinBase.INVALID_HANDLE_VALUE.equals(deviceInfoSet)) {
            try {
                SP_DEVINFO_DATA deviceInfoData = new SP_DEVINFO_DATA();
                SP_DEVICE_INTERFACE_DATA deviceInterfaceData = new SP_DEVICE_INTERFACE_DATA();

                int devIdx = 0;
                int intIdx;
                while (apiInst.SetupDiEnumDeviceInfo(deviceInfoSet, devIdx, deviceInfoData)) {

                    Memory propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SetupApi.SPDRP_DEVICEDESC, deviceInfoData);
                    String name = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_FRIENDLYNAME, deviceInfoData);
                    String friendlyName = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_ENUMERATOR_NAME, deviceInfoData);
                    String enumName = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_MFG, deviceInfoData);
                    String mfg = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_PHYSICAL_DEVICE_OBJECT_NAME, deviceInfoData);
                    String pdoName = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_SERVICE, deviceInfoData);
                    String service = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_CLASS, deviceInfoData);
                    String clazz = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_COMPATIBLEIDS, deviceInfoData);
                    String compIds = propertyBuffer == null ? null : propertyBuffer.getWideString(0L);
                    propertyBuffer = getDeviceRegistryProperty(apiInst, deviceInfoSet, SPDRP_HARDWAREID, deviceInfoData);
                    if (propertyBuffer != null) {
                        List<String> ids = readRegMultiSz(propertyBuffer);
                        logger.error("name: {}, friendlyName: {}, enumName: {}, mfg: {}, pdoName: {}, service: {}, class: {}, compIds: {}, ids: {}", name, friendlyName, enumName, mfg, pdoName, service, clazz, compIds, ids);
                    }
                    //TODO: (Nad) Handle Win32Exception

                    intIdx = 0;
                    while (apiInst.SetupDiEnumDeviceInterfaces(deviceInfoSet, deviceInfoData.getPointer(), GUID_DEVINTERFACE_USB_DEVICE, intIdx, deviceInterfaceData)) {
                        List<String> devicePaths = getDeviceInterfaceDetails(apiInst, deviceInfoSet, deviceInfoData, deviceInterfaceData);
                        logger.error("devicePaths: {}", devicePaths);
                        DevicePathData data;
                        for (String devicePath : devicePaths) {
                            data = parseDevicePath(devicePath);
                            if (data != null) {
                                logger.error("parsed details: {}", data);

                                WinReg.HKEY hKey = apiInst.SetupDiOpenDevRegKey(deviceInfoSet, deviceInfoData, SetupApi.DICS_FLAG_GLOBAL, 0, SetupApi.DIREG_DEV, WinNT.KEY_READ);
                                if (hKey != WinBase.INVALID_HANDLE_VALUE) {
                                    try {
                                        serialPort = Advapi32Util.registryGetStringValue(hKey, KEY_SERIAL_PORT);
                                    } catch (Win32Exception e) {
                                        serialPort = null;
                                    } finally {
                                        Advapi32.INSTANCE.RegCloseKey(hKey);
                                    }
                                    logger.error("PortName: {}", serialPort);
                                } else {
                                    serialPort = null;
                                }

                                UsbSerialDeviceInformation usbSerialDeviceInformation = new UsbSerialDeviceInformation(
                                    data.vendorId, data.productId, data.id, mfg, friendlyName == null || friendlyName.isBlank() ? name : friendlyName,
                                    data.interfaceNumber, data.id, serialPort == null ? "" : serialPort);

                                logger.debug("Ndd {}", usbSerialDeviceInformation);

                            }
                        }

                        intIdx++;
                    }
                    //TODO: GetLastError / ERROR_NO_MORE_ITEMS

                    devIdx++;
                }
                //TODO: GetLastError / ERROR_NO_MORE_ITEMS

            } finally {
                apiInst.SetupDiDestroyDeviceInfoList(deviceInfoSet);
            }
        } else {
            //TODO: Log error
        }

        Set<UsbSerialDeviceInformation> result = new HashSet<>();
        String[] deviceKeys;
        try {
            deviceKeys = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, USB_REGISTRY_ROOT);
        } catch (Win32Exception e) {
            logger.debug("registryGetKeys failed for {}", USB_REGISTRY_ROOT, e);
            return result;
        }

        for (String deviceKey : deviceKeys) {
            logger.trace("{}", deviceKey);

            if (!deviceKey.startsWith(PREFIX_VID)) {
                continue;
            }

            String[] ids = deviceKey.split(SPLIT_IDS);
            if (ids.length < 2) {
                continue;
            }

            if (!ids[1].startsWith(PREFIX_PID)) {
                continue;
            }

            int vendorId;
            int productId;
            try {
                vendorId = Integer.decode(PREFIX_HEX + ids[0].substring(4));
                productId = Integer.decode(PREFIX_HEX + ids[1].substring(4));
            } catch (NumberFormatException e) {
                continue;
            }

            String serialNumber = ids.length > 2 ? ids[2] : null;

            String devicePath = USB_REGISTRY_ROOT + BACKSLASH + deviceKey;
            String[] interfaceNames;
            try {
                interfaceNames = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, devicePath);
            } catch (Win32Exception e) {
                logger.debug("registryGetKeys failed for {}", devicePath, e);
                continue;
            }

            int interfaceId = 0;
            for (String interfaceName : interfaceNames) {
                logger.trace("  interfaceId:{}, interfaceName:{}", interfaceId, interfaceName);

                String interfacePath = devicePath + BACKSLASH + interfaceName;
                TreeMap<String, Object> values;
                try {
                    values = Advapi32Util.registryGetValues(HKEY_LOCAL_MACHINE, interfacePath);
                } catch (Win32Exception e) {
                    logger.debug("registryGetValues failed for {}", interfacePath, e);
                    continue;
                }

                if (logger.isTraceEnabled()) {
                    for (Entry<String, Object> value : values.entrySet()) {
                        logger.trace("    {}={}", value.getKey(), value.getValue());
                    }
                }

                String manufacturer;
                Object manufacturerValue = values.get(KEY_MANUFACTURER);
                if (manufacturerValue instanceof String manufacturerString) {
                    String[] manufacturerData = manufacturerString.split(SPLIT_VALUES);
                    if (manufacturerData.length < 2) {
                        continue;
                    }
                    manufacturer = manufacturerData[1];
                } else {
                    continue;
                }

                String product;
                Object productValue = values.get(KEY_PRODUCT);
                if (productValue instanceof String productString) {
                    String[] productData = productString.split(SPLIT_VALUES);
                    if (productData.length < 2) {
                        continue;
                    }
                    product = productData[1];
                } else {
                    continue;
                }

                serialPort = "";
                String[] interfaceSubKeys;
                try {
                    interfaceSubKeys = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, interfacePath);
                } catch (Win32Exception e) {
                    logger.debug("registryGetKeys failed for {}", interfacePath, e);
                    continue;
                }

                for (String interfaceSubKey : interfaceSubKeys) {
                    if (!KEY_DEVICE_PARAMETERS.equals(interfaceSubKey)) {
                        continue;
                    }
                    String deviceParametersPath = interfacePath + BACKSLASH + interfaceSubKey;
                    TreeMap<String, Object> deviceParameterValues;
                    try {
                        deviceParameterValues = Advapi32Util.registryGetValues(HKEY_LOCAL_MACHINE,
                                deviceParametersPath);
                    } catch (Win32Exception e) {
                        logger.debug("registryGetValues failed for {}", deviceParametersPath, e);
                        continue;
                    }
                    Object serialPortValue = deviceParameterValues.get(KEY_SERIAL_PORT);
                    if (serialPortValue instanceof String serialPortString) {
                        serialPort = serialPortString;
                    }
                    break;
                }

                UsbSerialDeviceInformation usbSerialDeviceInformation = new UsbSerialDeviceInformation(vendorId,
                        productId, serialNumber, manufacturer, product, interfaceId, interfaceName, serialPort);

                logger.debug("Add {}", usbSerialDeviceInformation);
                result.add(usbSerialDeviceInformation);

                interfaceId++;
            }
        }
        return result;
    }

    // TODO: Doc: Win32Exception
    @Nullable
    protected Memory getDeviceRegistryProperty(SetupApi apiInst, WinNT.HANDLE deviceInfoSet, int property, SP_DEVINFO_DATA deviceInfoData) {
        IntByReference size = new IntByReference();
        int lastError;
        if (!apiInst.SetupDiGetDeviceRegistryProperty(deviceInfoSet, deviceInfoData, property, null, null, 0, size) && (lastError = Native.getLastError()) != WinError.ERROR_INSUFFICIENT_BUFFER) {
            if (lastError == WinError.ERROR_INVALID_DATA || lastError == ERROR_NO_SUCH_DEVINST) {
                return null;
            }
            throw new Win32Exception(lastError);
        }
        int sizeValue = size.getValue();
        if (sizeValue == 0) {
            return null;
        }
        Memory buffer = new Memory(sizeValue);
        if (!apiInst.SetupDiGetDeviceRegistryProperty(deviceInfoSet, deviceInfoData, property, null, buffer, sizeValue, null)) {
            lastError = Native.getLastError();
            if (lastError == WinError.ERROR_INVALID_DATA) {
                return null;
            }
            throw new Win32Exception(lastError);
        }
        return buffer;
    }

    // TODO: Doc: Win32Exception
    protected List<String> getDeviceInterfaceDetails(SetupApi apiInst, WinNT.HANDLE deviceInfoSet, SP_DEVINFO_DATA deviceInfoData, SP_DEVICE_INTERFACE_DATA deviceInterfaceData) {
        IntByReference size = new IntByReference();
        int lastError;
        if (!apiInst.SetupDiGetDeviceInterfaceDetail(deviceInfoSet, deviceInterfaceData, null, 0, size, deviceInfoData) && (lastError = Native.getLastError()) != WinError.ERROR_INSUFFICIENT_BUFFER) {
            if (lastError == WinError.ERROR_INVALID_DATA) {
                return List.of();
            }
            throw new Win32Exception(lastError);
        }
        int sizeValue = size.getValue();
        if (sizeValue == 0) {
            return List.of();
        }
        Memory result = new Memory(sizeValue);

        /*
         *  The DWORD (uint) must contain the "size of the structure", which is only logical for those that
         *  know how C compilers handle padding (64-bit pads where 32-bit doesn't).
         *
         *  The 32-bit value represents: sizeOf(DWORD) + sizeOf(UTF16 char) = 4 + 2
         *  The 64-bit value represents: sizeOf(DWORD) + sizeOf(UTF16 char) + padding = 4 + 2 + 2
         *
         *  See https://stackoverflow.com/a/10729517 for further details.
         */
        result.setInt(0L, IS_64_BIT ? 8 : 6);
        if (!apiInst.SetupDiGetDeviceInterfaceDetail(deviceInfoSet, deviceInterfaceData, result, sizeValue, null, deviceInfoData)) {
            lastError = Native.getLastError();
            if (lastError == WinError.ERROR_INVALID_DATA) {
                return List.of();
            }
            throw new Win32Exception(lastError);
        }
        return readRegMultiSz(result, 4L);
    }

    @Nullable
    protected DevicePathData parseDevicePath(String devicePath) {
        Matcher m = devicePathPattern.matcher(devicePath.toLowerCase(Locale.ROOT));
        if (m.find()) {
            try {
                int vendorId = Integer.valueOf(m.group("vid"), 16);
                int productId = Integer.valueOf(m.group("pid"), 16);
                String s = m.group("mi");
                int interfaceNumber = s == null || s.isBlank() ? 0 : Integer.valueOf(s, 10);
                s = m.group("id");
                return new DevicePathData(vendorId, productId, s, interfaceNumber);
            } catch (NumberFormatException e) {
                // TODO: (Nad) LOg?
                return null;
            }
        }
        return null;
    }


    @Override
    public void startBackgroundScanning() {
        if (Platform.isWindows()) {
            boolean initScan = false;
            synchronized (this) {
                ScheduledFuture<?> scanTask = this.scanTask;
                WindowMessageHandler messageHandler = this.windowMessageHandler;
                if (windowMessageFailed) {
                    if (messageHandler != null) {
                        messageHandler.removeListener(this);
                        // Should not be necessary, but it doesn't hurt to make sure
                        messageHandler.terminate();
                        this.windowMessageHandler = null;
                    }
                    if (scanTask == null || scanTask.isDone()) {
                        this.scanTask = scheduler.scheduleWithFixedDelay(() -> {
                            doSingleScanInternal(false);
                        },
                                0, scanInterval.toSeconds(),
                                TimeUnit.SECONDS);
                    }
                } else {
                    if (scanTask != null) {
                        scanTask.cancel(true);
                        this.scanTask = null;
                    }
                    if (messageHandler == null) {
                         messageHandler = new WindowMessageHandler();
                         messageHandler.addListener(this);
                         this.windowMessageHandler = messageHandler;
                         scheduler.submit(messageHandler);
                         initScan = true;
                    }
                }
            }
            if (initScan) {
                doSingleScanInternal(false);
            }
        }
    }

    @Override
    public synchronized void stopBackgroundScanning() {
        WindowMessageHandler messageHandler = this.windowMessageHandler;
        if (messageHandler != null) {
            messageHandler.removeListener(this);
            messageHandler.terminate();
            this.windowMessageHandler = null;
        }
        ScheduledFuture<?> scanTask = this.scanTask;
        if (scanTask != null) {
            scanTask.cancel(true);
            this.scanTask = null;
        }
    }

    public static List<String> readRegMultiSz(Memory buffer) {
        int size = (int) buffer.size() / 2;
        if (size == 0) {
            return List.of();
        }
        return readRegMultiSz(buffer.getCharArray(0L, size));
    }

    public static List<String> readRegMultiSz(Memory buffer, long offset) {
        long bufferSize = buffer.size();
        if (offset >= bufferSize) {
            throw new IllegalArgumentException("Invalid offset " + offset + "for buffer of size " + bufferSize);
        }
        int size = (int) (bufferSize - offset) / 2;
        if (size == 0) {
            return List.of();
        }
        return readRegMultiSz(buffer.getCharArray(offset, size));
    }

    public static List<String> readRegMultiSz(char[] chars) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != 0) {
                continue;
            }
            if (start < i) {
                result.add(String.valueOf(chars, start, i - start));
            }
            start = i + 1;
        }
        return result;
    }

    @Override
    public void deviceAdded(String devicePath) {
        logger.debug("New USB device discovered: {}", devicePath);
        doSingleScan();
    }

    @Override
    public void deviceRemoved(String devicePath) {
        logger.debug("USB device removed: {}", devicePath);
        doSingleScan();
    }

    @Override
    public void portAdded(String portName) {
        logger.debug("New serial port discovered: {}", portName);
    }

    @Override
    public void portRemoved(String portName) {
        logger.debug("Serial port removed: {}", portName);
    }

    @Override
    public void serviceTerminated() {
        logger.debug("Listening for window messages failed, falling back to interval scanning");
        synchronized (this) {
            if (windowMessageHandler != null) {
                startBackgroundScanning();
            }
        }
    }
}
