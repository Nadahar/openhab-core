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
package org.openhab.core.io.transport.upnp.internal;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.UserConstants;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.io.transport.upnp.UpnpIOParticipant;
import org.openhab.core.io.transport.upnp.UpnpIOService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UpnpIOServiceImpl} is the implementation of the UpnpIOService
 * interface
 *
 * @author Karel Goderis - Initial contribution; added simple polling mechanism
 * @author Kai Kreuzer - added descriptor url retrieval
 * @author Markus Rathgeb - added NP checks in subscription ended callback
 * @author Andre Fuechsel - added methods to remove subscriptions
 * @author Ivan Iliev - made sure resubscribe is only done when subscription ended CancelReason was EXPIRED or
 *         RENEW_FAILED
 */
@SuppressWarnings("rawtypes")
@NonNullByDefault
@Component(immediate = true)
public class UpnpIOServiceImpl implements UpnpIOService, RegistryListener {

    private final Logger logger = LoggerFactory.getLogger(UpnpIOServiceImpl.class);

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(POOL_NAME);

    private static final int DEFAULT_POLLING_INTERVAL = 60;
    private static final String POOL_NAME = "upnp-io";

    // Threadsafe
    private final UpnpService upnpService;

    // All access must be guarded by "this"
    final Map<UpnpIOParticipant, ParticipantData> participants = new WeakHashMap<>();

    public class UpnpSubscriptionCallback extends SubscriptionCallback {

        private final WeakReference<UpnpIOParticipant> participant;

        /**
         * Creates a new subscription callback for the specified service with a requested duration of
         * {@link UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}.
         *
         * @param participant the {@link UpnpIOParticipant} that is the subscriber.
         * @param service the {@link Service} to subscribe to.
         */
        public UpnpSubscriptionCallback(UpnpIOParticipant participant, Service service) {
            super(service);
            this.participant = new WeakReference<UpnpIOParticipant>(participant);
        }

        /**
         * Creates a new subscription callback for the specified service with a requested duration of
         * the specified number of seconds. The UPnP standard states that it "Should be greater than or equal
         * to 1800 seconds (30 minutes)".
         *
         * @param participant the {@link UpnpIOParticipant} that is the subscriber.
         * @param service the {@link Service} to subscribe to.
         * @param requestedDurationSeconds the subscription duration to request.
         */
        public UpnpSubscriptionCallback(UpnpIOParticipant participant, Service service, int requestedDurationSeconds) {
            super(service, requestedDurationSeconds);
            this.participant = new WeakReference<UpnpIOParticipant>(participant);
        }

        @SuppressWarnings("null")
        @Override
        protected void ended(@Nullable GENASubscription subscription, @Nullable CancelReason reason,
                @Nullable UpnpResponse response) {
            Service service = subscription == null ? null : subscription.getService();
            if (subscription != null && service != null) {
                if (logger.isDebugEnabled()) {
                    ServiceId serviceId = service.getServiceId();
                    Device device = service.getDevice();
                    if (device != null) {
                        DeviceIdentity identity;
                        UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                        Device deviceRoot = device.getRoot();
                        if (!device.equals(deviceRoot)) {
                            UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null
                                    ? null
                                    : identity.getUdn();
                            logger.debug("A GENA subscription '{}' for device '{}' ('{}') was ended", serviceId.getId(),
                                    deviceUdn, deviceRootUdn);
                        } else {
                            logger.debug("A GENA subscription '{}' for device '{}' was ended", serviceId.getId(),
                                    deviceUdn);
                        }
                    }
                }

                if ((CancelReason.EXPIRED.equals(reason) || CancelReason.RENEWAL_FAILED.equals(reason))) {
                    final ControlPoint cp = upnpService.getControlPoint();
                    UpnpIOParticipant participant = this.participant.get();
                    if (cp != null && participant != null) {
                        final UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(participant, service,
                                subscription.getActualDurationSeconds());
                        cp.execute(callback);
                    }
                }
            }
        }

        @SuppressWarnings({ "unused", "null" })
        @Override
        protected void established(@Nullable GENASubscription subscription) {
            Service service = subscription == null ? null : subscription.getService();
            if (service != null) {
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                ServiceId serviceId = service.getServiceId();

                if (logger.isDebugEnabled() && device != null) {
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.debug("A GENA subscription '{}' for device '{}' ('{}') was established",
                                serviceId.getId(), deviceUdn, deviceRootUdn);
                    } else {
                        logger.debug("A GENA subscription '{}' for device '{}' was established", serviceId.getId(),
                                deviceUdn);
                    }
                }

                UpnpIOParticipant participant = this.participant.get();
                if (participant != null) {
                    try {
                        participant.onServiceSubscribed(serviceId.getId(), true);
                    } catch (Exception e) {
                        logger.error("Participant threw an exception during onServiceSubscribed()", e);
                    }
                } else {
                    logger.warn("A '{}' GENA subscription was established for a non-existing participant", serviceId.getId());
                }
            }
        }

        @SuppressWarnings({ "unchecked", "unused", "null" })
        @Override
        protected void eventReceived(@Nullable GENASubscription subscription) {
            Service service = subscription == null ? null : subscription.getService();
            if (subscription != null && service != null) {
                Map<String, StateVariableValue> values = subscription.getCurrentValues();
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                ServiceId serviceId = service.getServiceId();

                if (logger.isTraceEnabled() && device != null) {
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.trace("A GENA subscription '{}' event for device '{}' ('{}') was received",
                                serviceId.getId(), deviceUdn, deviceRootUdn);
                    } else {
                        logger.trace("A GENA subscription '{}' event for device '{}' was received", serviceId.getId(),
                                deviceUdn);
                    }
                }

                UpnpIOParticipant participant = this.participant.get();
                if (participant != null) {
                    for (Entry<String, StateVariableValue> entry : values.entrySet()) {
                        Object value = entry.getValue().getValue();
                        if (value != null) {
                            try {
                                participant.onValueReceived(entry.getKey(), value.toString(), serviceId.getId());
                            } catch (Exception e) {
                                logger.error("Participant threw an exception onValueReceived()", e);
                            }
                        }
                    }
                } else {
                    logger.warn("A '{}' GENA event was received for a non-existing participant: {}", serviceId.getId(), values);
                }
            }
        }

        @Override
        protected void eventsMissed(@Nullable GENASubscription subscription, int numberOfMissedEvents) {
            Service service;
            if (logger.isDebugEnabled() && subscription != null && (service = subscription.getService()) != null) {
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                if (device != null) {
                    ServiceId serviceId = service.getServiceId();
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.debug("A GENA subscription '{}' for device '{}' ('{}') missed {} events",
                                serviceId.getId(), deviceUdn, deviceRootUdn, numberOfMissedEvents);
                    } else {
                        logger.debug("A GENA subscription '{}' for device '{}' missed {} events", serviceId.getId(),
                                deviceUdn, numberOfMissedEvents);
                    }
                }
            }
        }

        @SuppressWarnings({ "unused", "null" })
        @Override
        protected void failed(@Nullable GENASubscription subscription, @Nullable UpnpResponse response,
                @Nullable Exception e, @Nullable String defaultMsg) {
            Service service = subscription == null ? null : subscription.getService();
            if (subscription != null && service != null) {
                Device device = service.getDevice();
                Device deviceRoot = device == null ? null : device.getRoot();
                ServiceId serviceId = service.getServiceId();

                if (logger.isDebugEnabled() && device != null) {
                    DeviceIdentity identity;
                    UDN deviceUdn = (identity = device.getIdentity()) == null ? null : identity.getUdn();
                    if (!device.equals(deviceRoot)) {
                        UDN deviceRootUdn = deviceRoot == null || (identity = deviceRoot.getIdentity()) == null ? null
                                : identity.getUdn();
                        logger.debug("A GENA subscription '{}' for device '{}' ('{}') failed: {}", serviceId.getId(),
                                deviceUdn, deviceRootUdn, defaultMsg);
                    } else {
                        logger.debug("A GENA subscription '{}' for device '{}' failed: {}", serviceId.getId(),
                                deviceUdn, defaultMsg);
                    }
                }

                UpnpIOParticipant participant = this.participant.get();
                if (participant != null) {
                    try {
                        participant.onServiceSubscribed(serviceId.getId(), false);
                    } catch (Exception pe) {
                        logger.error("Participant threw an exception during onServiceSubscribed()", pe);
                    }
                } else {
                    logger.warn("A '{}' GENA subscription failed for a non-existing participant", serviceId.getId());
                }
            }
        }
    }

    @Activate
    public UpnpIOServiceImpl(final @Reference UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    @Activate
    public void activate() {
        logger.debug("Starting UPnP IO service...");
        upnpService.getRegistry().getRemoteDevices().forEach(device -> informParticipants(device, true));
        upnpService.getRegistry().addListener(this);
    }

    @Deactivate
    public void deactivate() {
        logger.debug("Stopping UPnP IO service...");
        upnpService.getRegistry().removeListener(this);
        synchronized (this) {
            for (ParticipantData data : participants.values()) {
                data.dispose();
            }
            participants.clear();
        }
    }

    @Nullable
    private Device getDevice(UpnpIOParticipant participant) {
        String participantUdn = participant.getUDN();
        if ("undefined".equals(participantUdn)) {
            return null;
        }
        Registry registry = upnpService.getRegistry();
        return registry == null ? null : registry.getDevice(new UDN(participantUdn), true);
    }

    @Override
    public void addSubscription(UpnpIOParticipant participant, String serviceID) {
        addSubscription(participant, serviceID, UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS);
    }

    // TODO: (Nad) Add to interface
    public void addSubscription(UpnpIOParticipant participant, Service service) {
        addSubscription(participant, service, UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS);
    }

    @Override
    public void addSubscription(UpnpIOParticipant participant, String serviceID, int requestedDurationSeconds) {
        Device device = getDevice(participant);
        if (device instanceof RemoteDevice remoteDevice) {
            ServiceId sid = resolveServiceId(serviceID, device.getType().getNamespace());
            // First look for the service in the root device only, and only if not found,
            // look in the embedded devices.
            Service service = enumerateServices(remoteDevice, true).stream().filter(s -> sid.equals(s.getServiceId())).findAny()
                .orElse(enumerateServices(remoteDevice, false).stream().filter(s -> sid.equals(s.getServiceId())).findAny().orElse(null));
            if (service != null) {
                logger.trace("Setting up a GENA subscription for '{}' for particpant '{}'", serviceID,
                    participant.getUDN());

                ParticipantData data;
                synchronized (this) {
                    data = Objects.requireNonNull(participants.computeIfAbsent(participant, d -> new ParticipantData()));
                }
                UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(participant, service, requestedDurationSeconds);
                UpnpSubscriptionCallback oldCallback = data.addCallback(sid, callback);
                if (oldCallback != null) {
                    logger.warn("Participant '{}' added a GENA subscription for '{}' when one already existed. Cancelling the old subscription.", participant.getUDN(), serviceID);
                }
                upnpService.getControlPoint().execute(callback);
            } else {
                logger.trace("Could not find service '{}' for device '{}'", serviceID, device.getIdentity().getUdn());
            }
        } else {
            logger.trace("Could not find an UPnP device for participant '{}'", participant.getUDN());
        }
    }

    // TODO: (Nad) Add to interface
    public void addSubscription(UpnpIOParticipant participant, Service service, int requestedDurationSeconds) {
        ServiceId serviceId = service.getServiceId();
        logger.trace("Setting up a GENA subscription for '{}' for particpant '{}'", serviceId.getId(),
            participant.getUDN());

        ParticipantData data;
        synchronized (this) {
            data = Objects.requireNonNull(participants.computeIfAbsent(participant, d -> new ParticipantData()));
        }
        UpnpSubscriptionCallback callback = new UpnpSubscriptionCallback(participant, service, requestedDurationSeconds);
        UpnpSubscriptionCallback oldCallback = data.addCallback(serviceId, callback);
        if (oldCallback != null) {
            logger.warn("Participant '{}' added a GENA subscription for '{}' when one already existed. Cancelling the old subscription.", participant.getUDN(), serviceId.getId());
        }
        upnpService.getControlPoint().execute(callback);
    }

    @Override
    public void removeSubscription(UpnpIOParticipant participant, String serviceID) {
        ParticipantData data;
        synchronized (this) {
            data = participants.get(participant);
        }
        if (data == null) {
            logger.debug("Participant '{}' is trying to remove GENA subscription for '{}', but isn't registered", participant.getUDN(), serviceID);
            return;
        }

        UpnpSubscriptionCallback callback;
        synchronized (data) {
            ServiceId sid = data.getCallbacks().keySet().stream().filter(s -> serviceID.equals(s.getId())).findAny().orElse(null);
            if (sid == null) {
                logger.debug("Could not find and cancel GENA subscription for '{}' for participant '{}'", serviceID, participant.getUDN());
                return;
            }
            callback = data.removeCallback(sid);
        }
        if (callback != null) {
            logger.trace("Removed GENA subscription for '{}' for particpant '{}'", serviceID,
                participant.getUDN());
        }
    }

    // TODO: (Nad) Add to interface
    public void removeSubscription(UpnpIOParticipant participant, ServiceId serviceId) {
        ParticipantData data;
        synchronized (this) {
            data = participants.get(participant);
        }
        if (data == null) {
            logger.debug("Participant '{}' is trying to remove GENA subscription for '{}', but isn't registered", participant.getUDN(), serviceId.getId());
            return;
        }

        UpnpSubscriptionCallback callback = data.removeCallback(serviceId);
        if (callback != null) {
            logger.trace("Removed GENA subscription for '{}' for particpant '{}'", serviceId.getId(),
                participant.getUDN());
        } else {
            logger.debug("Could not find and cancel GENA subscription for '{}' for participant '{}'", serviceId.getId(), participant.getUDN());
        }
    }

    @Override
    public Map<String, @Nullable String> invokeAction(UpnpIOParticipant participant, String serviceID, String actionID,
            @Nullable Map<String, String> inputs) {
        return invokeAction(participant, null, serviceID, actionID, inputs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, @Nullable String> invokeAction(UpnpIOParticipant participant, @Nullable String namespace,
            String serviceID, String actionID, @Nullable Map<String, String> inputs) {
        Map<String, @Nullable String> resultMap = new HashMap<>();

        registerParticipant(participant);
        Device device = getDevice(participant);

        if (device != null) {
            Service service = findService(device, namespace, serviceID);
            if (service != null) {
                Action action = service.getAction(actionID);
                if (action != null) {
                    ActionInvocation invocation = new ActionInvocation(action);
                    if (inputs != null) {
                        for (Entry<String, String> entry : inputs.entrySet()) {
                            invocation.setInput(entry.getKey(), entry.getValue());
                        }
                    }

                    logger.trace("Invoking Action '{}' of service '{}' for participant '{}'", actionID, serviceID,
                            participant.getUDN());
                    new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();

                    ActionException anException = invocation.getFailure();
                    if (anException != null && anException.getMessage() != null) {
                        logger.debug("{}", anException.getMessage());
                    }

                    Map<String, ActionArgumentValue> result = invocation.getOutputMap();
                    if (result != null) {
                        for (Entry<String, ActionArgumentValue> entry : result.entrySet()) {
                            String variable = entry.getKey();
                            final ActionArgumentValue newArgument;
                            try {
                                newArgument = entry.getValue();
                            } catch (final Exception ex) {
                                logger.debug("An exception '{}' occurred, cannot get argument for variable '{}'",
                                        ex.getMessage(), variable);
                                continue;
                            }
                            try {
                                if (newArgument.getValue() != null) {
                                    resultMap.put(variable, newArgument.getValue().toString());
                                }
                            } catch (final Exception ex) {
                                logger.debug(
                                        "An exception '{}' occurred processing ActionArgumentValue '{}' with value '{}'",
                                        ex.getMessage(), newArgument.getArgument().getName(), newArgument.getValue());
                            }
                        }
                    }
                } else {
                    logger.debug("Could not find action '{}' for participant '{}'", actionID, participant.getUDN());
                }
            } else {
                logger.debug("Could not find service '{}' for participant '{}'", serviceID, participant.getUDN());
            }
        } else {
            logger.debug("Could not find an upnp device for participant '{}'", participant.getUDN());
        }
        return resultMap;
    }

    @Override
    public boolean isRegistered(UpnpIOParticipant participant) {
        return upnpService.getRegistry().getDevice(new UDN(participant.getUDN()), true) != null;
    }

    @Override
    public void registerParticipant(UpnpIOParticipant participant) { //TODO: (Nad) Inform existing..?
        synchronized (this) {
            participants.computeIfAbsent(participant, d -> new ParticipantData()); //TODO: (Nad) Move sync
        }
    }

    @Override
    public void unregisterParticipant(UpnpIOParticipant participant) {
        ParticipantData data;
        synchronized (this) {
            data = participants.remove(participant);
        }
        if (data != null) {
            data.dispose();
        }
    }

    @Override
    @Nullable
    public URL getDescriptorURL(UpnpIOParticipant participant) {
        RemoteDevice device = upnpService.getRegistry().getRemoteDevice(new UDN(participant.getUDN()), true);
        if (device != null) {
            return device.getIdentity().getDescriptorURL();
        } else {
            return null;
        }
    }

    @Nullable
    private Service findService(Device device, @Nullable String namespace, String serviceID) {
        Service service;
        String ns = namespace == null ? device.getType().getNamespace() : namespace;
        if (UDAServiceId.DEFAULT_NAMESPACE.equals(ns) || UDAServiceId.BROKEN_DEFAULT_NAMESPACE.equals(ns)) {
            service = device.findService(new UDAServiceId(serviceID));
        } else {
            service = device.findService(new ServiceId(ns, serviceID));
        }
        return service;
    }

    private ServiceId resolveServiceId(String serviceId, String namespace) {
        return UDAServiceId.DEFAULT_NAMESPACE.equals(namespace) || UDAServiceId.BROKEN_DEFAULT_NAMESPACE.equals(namespace) ? new UDAServiceId(serviceId) : new ServiceId(namespace, serviceId);
    }

    /**
     * Propagates a device status change to all participants
     *
     * @param device the device that has changed its status
     * @param status true, if device is reachable, false otherwise
     */
    private void informParticipants(RemoteDevice device, boolean status) {
        DeviceIdentity identity;
        if ((identity = device.getIdentity()) == null) {
            return;
        }
        Map<UpnpIOParticipant, ParticipantData> snapshot;
        synchronized (this) {
            snapshot = Map.copyOf(participants);
        }
        UpnpIOParticipant participant;
        for (Entry<UpnpIOParticipant, ParticipantData> entry : snapshot.entrySet()) {
            participant = entry.getKey();
            if (participant.getUDN().equals(identity.getUdn().getIdentifierString())) { //TODO: (Nad) Document that participant must use UDN for root device
                setDeviceStatus(participant, entry.getValue(), status);
            }
        }
    }

    private void setDeviceStatus(UpnpIOParticipant participant, ParticipantData data, boolean newStatus) {
        boolean oldStatus = data.getAndSetAvailable(newStatus);
        if (oldStatus != newStatus) {
            logger.debug("Device '{}' reachability status changed to '{}'", participant.getUDN(), newStatus);
            participant.onStatusChanged(newStatus);
        }
    }

    private class UPNPPollingRunnable implements Runnable {

        private final UpnpIOParticipant participant;
        private final String serviceID;
        private final String actionID;

        public UPNPPollingRunnable(UpnpIOParticipant participant, String serviceID, String actionID) {
            this.participant = participant;
            this.serviceID = serviceID;
            this.actionID = actionID;
        }

        @Override
        public void run() {
            // It is assumed that during addStatusListener() a check is made whether the participant is correctly
            // registered
            ParticipantData data;
            synchronized (UpnpIOServiceImpl.this) {
                data = participants.get(participant);
            }
            if (data == null) {
                logger.debug("Participant '{}' scheduled for polling with Action '{}' of Service '{}', but isn't registered",
                        participant.getUDN(), actionID, serviceID);
                return;
            }
            try {
                Device device = getDevice(participant);
                String participantUdn = participant.getUDN();
                if (device instanceof RemoteDevice remoteDevice) {
                    ServiceId serviceId = resolveServiceId(serviceID, device.getType().getNamespace());

                    // First look for the service in the root device only, and only if not found,
                    // look in the embedded devices.
                    Service service = enumerateServices(remoteDevice, true).stream().filter(s -> serviceId.equals(s.getServiceId())).findAny()
                        .orElse(enumerateServices(remoteDevice, false).stream().filter(s -> serviceId.equals(s.getServiceId())).findAny().orElse(null));
                    if (service != null) {
                        Action action = service.getAction(actionID);
                        if (action != null) {
                            @SuppressWarnings("unchecked")
                            ActionInvocation invocation = new ActionInvocation(action);
                            logger.debug("Polling participant '{}' through Action '{}' of Service '{}' ",
                                    participantUdn, actionID, serviceID);
                            new ActionCallback.Default(invocation, upnpService.getControlPoint()).run();

                            ActionException anException = invocation.getFailure();
                            String message;
                            if (anException != null && (message = anException.getMessage()) != null
                                    && message.contains("Connection error or no response received")) {
                                // The UDN is not reachable anymore
                                setDeviceStatus(participant, data, false);
                            } else {
                                // The UDN functions correctly
                                setDeviceStatus(participant, data, true);
                            }
                        } else {
                            logger.debug("Could not find action '{}' for participant '{}'", actionID, participantUdn);
                        }
                    } else {
                        logger.debug("Could not find service '{}' for participant '{}'", serviceID, participantUdn);
                    }
                }
            } catch (Exception e) {
                logger.error("An exception occurred while polling an UPnP device: '{}'", e.getMessage(), e);
            }
        }
    }

    @Override
    public void addStatusListener(UpnpIOParticipant participant, String serviceID, String actionID, int interval) {
        registerParticipant(participant);

        int pollingInterval = interval == 0 ? DEFAULT_POLLING_INTERVAL : interval;

        ParticipantData data;
        synchronized (this) {
            data = participants.get(participant);
        }
        if (data == null) {
            // Should not happen
            logger.warn("Unable to add status listener for participant {}, participant isn't registered", participant.getUDN());
            return;
        }
        data.setJob(scheduler.scheduleWithFixedDelay(new UPNPPollingRunnable(participant, serviceID, actionID), 0, pollingInterval, TimeUnit.SECONDS));
    }

    private synchronized List<UpnpIOParticipant> getSubscribingParticipants(ServiceId serviceId) {
        return participants.entrySet().stream().filter(e -> e.getValue().hasCallback(serviceId)).map(e -> e.getKey()).toList();
    }

    @Override
    public void removeStatusListener(UpnpIOParticipant participant) {
        unregisterParticipant(participant);
    }

    @Override
    public void remoteDeviceAdded(@Nullable Registry registry, @Nullable RemoteDevice device) {
        if (device != null) {
            informParticipants(device, true);
        }
    }

    @Override
    public void remoteDeviceUpdated(@Nullable Registry registry, @Nullable RemoteDevice device) {
        //TODO: (Nad) Probably set status here too...
    }

    @Override
    public void remoteDeviceRemoved(@Nullable Registry registry, @Nullable RemoteDevice device) {
        if (device != null) {
            informParticipants(device, false);
        }
    }

    @Override
    public void remoteDeviceDiscoveryStarted(@Nullable Registry registry, @Nullable RemoteDevice device) {
    }

    @Override
    public void remoteDeviceDiscoveryFailed(@Nullable Registry registry, @Nullable RemoteDevice device,
            @Nullable Exception ex) {
    }

    @Override
    public void localDeviceAdded(@Nullable Registry registry, @Nullable LocalDevice device) {
    }

    @Override
    public void localDeviceRemoved(@Nullable Registry registry, @Nullable LocalDevice device) {
    }

    @Override
    public void beforeShutdown(@Nullable Registry registry) {
    }

    @Override
    public void afterShutdown() {
    }

    /**
     * Generates a {@link List} of {@link RemoteService}es offered by the specified {@link RemoteDevice},
     * and optionally, all its embedded/child devices, if any.
     *
     * @param device the {@link RemoteDevice} whose {@link Service}s to enumerate.
     * @param rootOnly whether to only enumerate the device itself or also all its children.
     * @return The resulting {@link List} of {@link RemoteService}es.
     */
    public static List<RemoteService> enumerateServices(RemoteDevice device, boolean rootOnly) {
        List<RemoteService> result = new ArrayList<>();
        List<RemoteDevice> devices = rootOnly ? List.of(device) : enumerateAllDevices(device);
        RemoteService[] services;
        for (RemoteDevice d : devices) {
            services = d.getServices();
            if (services != null && services.length > 0) {
                result.addAll(Arrays.asList(services));
            }
        }
        return result;
    }

    /**
     * Generates a {@link List} of the specified {@link RemoteDevice} itself and all embedded/child devices.
     *
     * @param device the {@link RemoteDevice} whose device tree to enumerate.
     * @return The resulting {@link List} of {@link RemoteDevice}s.
     */
    public static List<RemoteDevice> enumerateAllDevices(RemoteDevice device) { //TODO: (Nad) Check JavaDocs
        List<RemoteDevice> result = new ArrayList<>();
        result.add(device);
        enumerateChildDevices(device, result);
        return result;
    }

    /**
     * Traverses and adds child/embedded devices to the provided {@link List} recursively.
     *
     * @param device the {@link RemoteDevice} whose children to add to {@code devices}.
     * @param devices the {@link List} to add the children to.
     */
    private static void enumerateChildDevices(RemoteDevice device, List<RemoteDevice> devices) {
        for (RemoteDevice child : device.getEmbeddedDevices()) {
            devices.add(child);
            enumerateChildDevices(device, devices);
        }
    }

    // Threadsafe
    public static class ParticipantData {

        // All access must be guarded by "this"
        @Nullable
        private ScheduledFuture<?> job;

        // All access must be guarded by "this"
        private boolean available;

        // All access must be guarded by "this"
        private final Map<ServiceId, UpnpSubscriptionCallback> callbacks = new HashMap<>();

        /**
         * @return The current polling job, if any.
         */
        @Nullable
        public synchronized ScheduledFuture<?> getJob() {
            return job;
        }

        /**
         * @return {@code true} if a polling job is registered.
         */
        public synchronized boolean hasJob() {
            return job != null;
        }

        /**
         * Sets the current polling job, and cancels the old one if one already exists.
         *
         * @param job the new polling job.
         */
        public void setJob(@Nullable ScheduledFuture<?> job) {
            ScheduledFuture<?> oldJob;
            synchronized (this) {
                oldJob = this.job;
                this.job = job;
            }
            if (oldJob != null && !oldJob.isDone()) {
                oldJob.cancel(true);
            }
        }

        /**
         * Sets the current polling job to {@code null} and cancels the old one if one exists.
         */
        public void clearJob() {
            setJob(null);
        }

        public synchronized boolean isAvailable() {
            return available;
        }

        public synchronized boolean getAndSetAvailable(boolean state) {
            boolean result = available;
            available = state;
            return result;
        }
        public synchronized void setAvailable(boolean state) {
            available = state;
        }

        public synchronized boolean hasCallback(ServiceId serviceId) {
            return callbacks.containsKey(serviceId);
        }

        @Nullable
        public synchronized UpnpSubscriptionCallback getCallback(ServiceId serviceId) {
            return callbacks.get(serviceId);
        }

        public synchronized Map<ServiceId, UpnpSubscriptionCallback> getCallbacks() {
            return Map.copyOf(callbacks);
        }

        @Nullable
        public UpnpSubscriptionCallback addCallback(ServiceId serviceId, UpnpSubscriptionCallback callback) {
            UpnpSubscriptionCallback result;
            synchronized (this) {
                result = callbacks.put(serviceId, callback);
            }
            if (result != null) {
                result.end();
            }
            return result;
        }

        @Nullable
        public UpnpSubscriptionCallback removeCallback(ServiceId serviceId) {
            UpnpSubscriptionCallback result;
            synchronized (this) {
                result = callbacks.remove(serviceId);
            }
            if (result != null) {
                result.end();
            }
            return result;
        }

        public void dispose() {
            ScheduledFuture<?> job;
            Map<ServiceId, UpnpSubscriptionCallback> callbacks;
            synchronized (this) {
                job = this.job;
                this.job = null;
                callbacks = this.callbacks;
                this.callbacks.clear();
                this.available = false;
            }
            if (job != null) {
                job.cancel(true);
            }
            callbacks.forEach((i, c) -> c.end());
        }
    }
}
