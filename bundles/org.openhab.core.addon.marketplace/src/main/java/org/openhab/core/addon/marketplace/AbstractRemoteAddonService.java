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
package org.openhab.core.addon.marketplace;

import static org.openhab.core.common.ThreadPoolManager.THREAD_POOL_NAME_COMMON;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonEventFactory;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.addon.Version;
import org.openhab.core.addon.VersionTypeAdapter;
import org.openhab.core.addon.dto.AddonDTO;
import org.openhab.core.cache.ExpiringCache;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.core.ConfigParser;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link AbstractRemoteAddonService} implements basic functionality of a remote add-on-service
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractRemoteAddonService implements AddonService {
    static final String CONFIG_REMOTE_ENABLED = "remote";
    static final String CONFIG_INCLUDE_INCOMPATIBLE = "includeIncompatible";
    static final Comparator<Addon> BY_COMPATIBLE_AND_VERSION = (addon1, addon2) -> {
        // prefer compatible to incompatible
        int compatible = Boolean.compare(addon2.getCompatible(), addon1.getCompatible());
        if (compatible != 0) {
            return compatible;
        }
        Version v2 = addon2.getVersion();
        return v2 == null ? 1 : v2.compareTo(addon1.getVersion());
    };

    protected final Version coreVersion;

    protected final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(Version.class, new VersionTypeAdapter()).create();
    protected final CopyOnWriteArraySet<MarketplaceAddonHandler> addonHandlers = new CopyOnWriteArraySet<>();
    // Guarded by "this"
    protected final Storage<AddonDTO> installedAddonStorage;
    protected final EventPublisher eventPublisher;
    protected final ConfigurationAdmin configurationAdmin;
    protected final ExpiringCache<List<Addon>> cachedRemoteAddons = new ExpiringCache<>(Duration.ofMinutes(15),
            this::getRemoteAddons);
    protected final AddonInfoRegistry addonInfoRegistry;
    // Guarded by "this"
    protected List<Addon> cachedAddons = List.of();
    // Guarded by "this"
    protected List<String> installedAddonIds = List.of();
    // Guarded by "this"
    protected boolean includeIncompatible;

    private final Logger logger = LoggerFactory.getLogger(AbstractRemoteAddonService.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREAD_POOL_NAME_COMMON);
    // Guarded by "scheduler"
    private @Nullable ScheduledFuture<?> scheduledRefresh;

    protected AbstractRemoteAddonService(EventPublisher eventPublisher, ConfigurationAdmin configurationAdmin,
            StorageService storageService, AddonInfoRegistry addonInfoRegistry, String servicePid) {
        this.addonInfoRegistry = addonInfoRegistry;
        this.eventPublisher = eventPublisher;
        this.configurationAdmin = configurationAdmin;
        this.installedAddonStorage = storageService.getStorage(servicePid, getClass().getClassLoader());
        this.coreVersion = getCoreVersion();
        this.includeIncompatible = includeIncompatible();
    }

    protected Version getCoreVersion() {
        return Version.valueOf(FrameworkUtil.getBundle(OpenHAB.class).getVersion());
    }

    private Addon convertFromStorage(Map.Entry<String, @Nullable AddonDTO> entry) {
        Object addonObject = entry.getValue();
        Addon storedAddon;
        if (addonObject instanceof AddonDTO dto) {
            storedAddon = dto.toAddon();
        } else if (addonObject instanceof String s) {
            // In previous versions stored items were "double wrapped" in JSON, this is here to make sure add-ons
            // stored by a previous version can still be read.
            storedAddon = Objects.requireNonNull(gson.fromJson(s, Addon.class));
        } else if (addonObject == null) {
            throw new IllegalArgumentException("Stored addon is null");
        } else {
            throw new IllegalArgumentException("Invalid stored addon type: " + addonObject.getClass().getSimpleName());
        }
        AddonInfo addonInfo = addonInfoRegistry.getAddonInfo(storedAddon.getType() + "-" + storedAddon.getId());
        if (addonInfo != null) {
            Addon.Builder builder = Addon.create(storedAddon, false);
            String s, s2;
            if ((s = addonInfo.getConfigDescriptionURI()) != null && storedAddon.getConfigDescriptionURI().isBlank()) {
                builder.withConfigDescriptionURI(s);
            }
            if ((s = addonInfo.getConnection()) != null && storedAddon.getConnection().isBlank()) {
                builder.withConnection(s);
            }
            if (!addonInfo.getCountries().isEmpty() && storedAddon.getCountries().isEmpty()) {
                builder.withCountries(addonInfo.getCountries());
            }
            if (!addonInfo.getDescription().isBlank() && ((s = storedAddon.getDescription()) == null || s.isBlank()) && ((s2 = storedAddon.getDetailedDescription()) == null || s2.isBlank())) {
                // Description overrides detaildDescription if present when presented to the user,
                // so only set it if both are blank
                builder.withDescription(addonInfo.getDescription());
            }

            return builder.build();
        }
        return storedAddon;
    }

    /**
     * Schedules {@link #refreshSource()} with a delay to allow addon handlers to register and become ready
     * before the refresh is initiated. This is intended to be used when called from the constructor,
     * where the handlers generally isn't yet up and running, which will then make the refresh "fail"
     * in that all installed addons will be considered uninstalled, because no handler can confirm that
     * they are installed.
     */
    protected void scheduleRefreshSource() {
        synchronized (scheduler) {
            if (scheduledRefresh != null) {
                scheduledRefresh.cancel(false);
            }
            scheduledRefresh = scheduler.schedule(() -> {
                synchronized (scheduler) {
                    scheduledRefresh = null;
                }
                if (addonHandlers.isEmpty() || !addonHandlers.stream().allMatch(MarketplaceAddonHandler::isReady)) {
                    scheduleRefreshSource();
                    return;
                }
                refreshSource();
            }, 500L, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void refreshSource() {
        if (includeIncompatible != includeIncompatible()) {
            includeIncompatible = !includeIncompatible;
            cachedRemoteAddons.invalidateValue();
        }

        if (addonHandlers.isEmpty() || !addonHandlers.stream().allMatch(MarketplaceAddonHandler::isReady)) {
            logger.debug("Add-on service '{}' tried to refresh source before add-on handlers ready. Exiting.",
                    getClass().getSimpleName());
            return;
        }

        List<Addon> addons = new ArrayList<>();

        // retrieve add-ons that should be available from storage and check if they are really installed
        // this is safe, because the {@link AddonHandler}s only report ready when they installed everything from the
        // cache
        try {
            installedAddonStorage.stream().map(this::convertFromStorage).forEach(addon -> {
                setInstalled(addon); //TODO: (Nad) Sets addon to if is installed according to handler
                addons.add(addon);
            });
        } catch (JsonSyntaxException e) {
            List.copyOf(installedAddonStorage.getKeys()).forEach(installedAddonStorage::remove);
            logger.error(
                    "Failed to read JSON database, trying to purge it. You might need to re-install {} from the '{}' service.",
                    installedAddonStorage.getKeys(), getId());
            refreshSource();
        }

        // remove not installed add-ons from the add-ons list, but remember their UIDs to re-install them
        List<String> missingAddons = addons.stream().filter(addon -> !addon.isInstalled()).map(Addon::getUid).toList();
        missingAddons.forEach(installedAddonStorage::remove);
        addons.removeIf(addon -> missingAddons.contains(addon.getUid()));

        // create lookup list to make sure installed addons take precedence
        List<String> currentAddonIds = addons.stream().map(Addon::getUid).toList();

        // get the remote addons
        if (remoteEnabled()) {
            List<Addon> remoteAddons = Objects.requireNonNullElse(cachedRemoteAddons.getValue(), List.of());
            remoteAddons.stream().filter(a -> !currentAddonIds.contains(a.getUid())).forEach(addon -> {
                setInstalled(addon);
                addons.add(addon);
            });
        }

        // remove incompatible add-ons if not enabled
        boolean showIncompatible = includeIncompatible();
        addons.removeIf(addon -> !addon.isInstalled() && !addon.getCompatible() && !showIncompatible);

        // check and remove duplicate uids
        Map<String, List<Addon>> addonMap = new HashMap<>();
        addons.forEach(a -> addonMap.computeIfAbsent(a.getUid(), k -> new ArrayList<>()).add(a));
        for (List<Addon> partialAddonList : addonMap.values()) {
            if (partialAddonList.size() > 1) {
                partialAddonList.stream().sorted(BY_COMPATIBLE_AND_VERSION).skip(1).forEach(addons::remove);
            }
        }

        cachedAddons = addons;
        this.installedAddonIds = currentAddonIds;

        if (!missingAddons.isEmpty()) {
            logger.info("Re-installing missing add-ons from remote repository: {}", missingAddons);
            scheduler.execute(() -> missingAddons.forEach((a) ->  install(a, null))); //TODO: (Nad) Figure out version
        }
    }

    private void setInstalled(Addon addon) {
        MarketplaceAddonHandler handler = addonHandlers.stream().filter(h -> h.supports(addon.getType(), addon.getContentType())).findFirst().orElse(null);
        if (handler != null) {
            addon.setInstalled(handler.isInstalled(addon.getUid()), addon.getInstalledVersion());
        }
    }

    /**
     * Add a {@link MarketplaceAddonHandler} to this service
     *
     * This needs to be implemented by the addon-services because the handlers are references to OSGi services and
     * the @Reference annotation is not inherited.
     * It is added here to make sure that implementations comply with that.
     *
     * @param handler the handler that shall be added
     */
    protected abstract void addAddonHandler(MarketplaceAddonHandler handler);

    /**
     * Remove a {@link MarketplaceAddonHandler} from this service
     *
     * This needs to be implemented by the addon-services because the handlers are references to OSGi services and
     * unbind methods can't be inherited.
     * It is added here to make sure that implementations comply with that.
     *
     * @param handler the handler that shall be removed
     */
    protected abstract void removeAddonHandler(MarketplaceAddonHandler handler);

    /**
     * get all addons from remote
     *
     * @return a list of {@link Addon} that are available on the remote side
     */
    protected abstract List<Addon> getRemoteAddons();

    @Override
    public synchronized List<Addon> getAddons(@Nullable Locale locale) {
        refreshSource();
        return List.copyOf(cachedAddons);
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return AddonType.DEFAULT_TYPES;
    }

    @Override
    public synchronized void install(String id, @Nullable String version) {
        Addon addon = getAddon(id, null);
        if (addon == null) {
            logger.warn("Failed to install add-on \"{}\" because it's unknown", id);
            postFailureEvent(id, "Add-on can't be installed because it is not known.");
            return;
        }
        Addon mergedAddon = null;
        Version v = null;
        if (version != null) {
            try {
                v = Version.valueOf(version);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to install add-on \"{}\" because the requested version \"{}\" is invalid", id, version);
                postFailureEvent(addon.getUid(), "Requested version is invalid: " + version);
                return;
            }
        }
        if (addon.isVersioned()) {
            if (v == null) {
                v = addon.getDefaultVersion();
            }
            if (v != null && !v.equals(addon.getVersion())) {
                try {
                    mergedAddon = addon.mergeVersion(v);
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to install add-on \"{}\" because the requested version \"{}\" doesn't exist", id, v);
                    postFailureEvent(addon.getUid(), "Requested version doesn't exist: " + v.toString());
                    return;
                }
            }
        } else if (v != null && !v.equals(addon.getVersion())) {
            logger.warn("Failed to install add-on \"{}\" because the requested version \"{}\" doesn't exist", id, v);
            postFailureEvent(addon.getUid(), "Requested version doesn't exist: " + v.toString());
            return;
        }
        if (mergedAddon == null) {
            mergedAddon = addon;
        }
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(mergedAddon.getType(), mergedAddon.getContentType())) {
                if (handler.isInstalled(mergedAddon.getUid())) {
                    if (mergedAddon.isVersioned() && v != null && !v.equals(mergedAddon.getInstalledVersion())) {
                        try {
                            handler.uninstall(addon);
                            installedAddonStorage.remove(id);
                            cachedRemoteAddons.invalidateValue();
                            postUninstalledEvent(addon.getUid());
                        } catch (MarketplaceHandlerException e) {
                            postFailureEvent(addon.getUid(), e.getMessage());
                            logger.warn("Failed to uninstall add-on \"{}\": {}", addon.getUid(), e.getMessage());
                        }
                    } else {
                        logger.warn("Failed to install add-on \"{}\" because it is already installed", id);
                        postFailureEvent(mergedAddon.getUid(), "Add-on is already installed.");
                    }
                }
                try {
                    handler.install(mergedAddon);
                    mergedAddon.setInstalled(true, v);
                    installedAddonStorage.put(id, AddonDTO.fromAddon(mergedAddon));
                    cachedRemoteAddons.invalidateValue();
                    refreshSource();
                    postInstalledEvent(mergedAddon.getUid());
                } catch (MarketplaceHandlerException e) {
                    postFailureEvent(mergedAddon.getUid(), e.getMessage());
                    logger.warn("Failed to install add-on \"{}\": {}", mergedAddon.getUid(), e.getMessage());
                }
                return;
            }
        }
        logger.warn("Failed to install add-on \"{}\" because no handler could be found", id);
        postFailureEvent(id, "Add-on can't be installed because there is no handler for it.");
    }

    @Override
    public synchronized void uninstall(String id) {
        Addon addon = getAddon(id, null);
        if (addon == null) {
            postFailureEvent(id, "Add-on can't be uninstalled because it is not known.");
            return;
        }
        for (MarketplaceAddonHandler handler : addonHandlers) {
            if (handler.supports(addon.getType(), addon.getContentType())) {
                if (handler.isInstalled(addon.getUid())) {
                    try {
                        handler.uninstall(addon);
                        installedAddonStorage.remove(id);
                        cachedRemoteAddons.invalidateValue();
                        refreshSource();
                        postUninstalledEvent(addon.getUid());
                    } catch (MarketplaceHandlerException e) {
                        postFailureEvent(addon.getUid(), e.getMessage());
                        logger.warn("Failed to uninstall add-on \"{}\": {}", addon.getUid(), e.getMessage());
                    }
                } else {
                    installedAddonStorage.remove(id);
                    postFailureEvent(addon.getUid(), "Add-on is not installed.");
                }
                return;
            }
        }
        postFailureEvent(id, "Add-on can't be uninstalled because there is no handler for it.");
    }

    /**
     * check if remote services are enabled
     *
     * @return true if network access is allowed
     */
    protected boolean remoteEnabled() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration("org.openhab.addons", null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                // if we can't determine a set property, we use true (default is remote enabled)
                return true;
            }
            return ConfigParser.valueAsOrElse(properties.get(CONFIG_REMOTE_ENABLED), Boolean.class, true);
        } catch (IOException e) {
            return true;
        }
    }

    protected boolean includeIncompatible() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration("org.openhab.addons", null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties == null) {
                // if we can't determine a set property, we use false (default is show compatible only)
                return false;
            }
            return ConfigParser.valueAsOrElse(properties.get(CONFIG_INCLUDE_INCOMPATIBLE), Boolean.class, false);
        } catch (IOException e) {
            return false;
        }
    }

    private void postInstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonInstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postUninstalledEvent(String extensionId) {
        Event event = AddonEventFactory.createAddonUninstalledEvent(extensionId);
        eventPublisher.post(event);
    }

    private void postFailureEvent(String extensionId, @Nullable String msg) {
        Event event = AddonEventFactory.createAddonFailureEvent(extensionId, msg);
        eventPublisher.post(event);
    }
}
