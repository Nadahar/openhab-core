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
package org.openhab.core.addon.internal;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.openhab.core.addon.Version;
import org.openhab.core.addon.xml.XmlAddonInfoProvider;
import org.openhab.core.addon.xml.XmlAddonInfoProvider.XmlAddonInfoListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JarFileAddonService} is an add-on service that provides add-ons that are placed a .jar files in the
 * openHAB addons folder
 *
 * @author Jan N. Klug - Initial contribution
 * @author Ravi Nadahar - Refactored as XmlAddonInfoListener
 */
@NonNullByDefault
@Component(immediate = true, service = AddonService.class, name = JarFileAddonService.SERVICE_NAME)
public class JarFileAddonService implements AddonService, XmlAddonInfoListener {
    public static final String SERVICE_ID = "jar";
    public static final String SERVICE_NAME = "jar-file-add-on-service";
    private static final String ADDONS_CONTENT_TYPE = "application/vnd.openhab.bundle";

    private static final Map<String, AddonType> ADDON_TYPE_MAP = Map.of( //
            "automation", new AddonType("automation", "Automation"), //
            "binding", new AddonType("binding", "Bindings"), //
            "misc", new AddonType("misc", "Misc"), //
            "persistence", new AddonType("persistence", "Persistence"), //
            "transformation", new AddonType("transformation", "Transformations"), //
            "ui", new AddonType("ui", "User Interfaces"), //
            "voice", new AddonType("voice", "Voice"));
    private static final String ADDON_ID_PREFIX = SERVICE_ID + ":";
    private final Logger logger = LoggerFactory.getLogger(JarFileAddonService.class);

    private final XmlAddonInfoProvider xmlAddonInfoProvider;

    // Guarded by "this"
    private final Map<Locale, Map<String, Addon>> addons = new HashMap<>();

    // Guarded by "this"
    private final Map<Bundle, String> addonsIdx = new HashMap<>();

    @Activate
    public JarFileAddonService(@Reference XmlAddonInfoProvider xmlAddonInfoProvider, BundleContext context) {
        this.xmlAddonInfoProvider = xmlAddonInfoProvider;
        xmlAddonInfoProvider.addListener(this);
    }

    @Deactivate
    public void deactivate() {
        xmlAddonInfoProvider.removeListener(this);
    }

    /**
     * Checks if a bundle is loaded from a JAR file.
     *
     * @param bundle the {@link Bundle} to check.
     * @return <code>true</code> if bundle is considered relevant, <code>false</code> otherwise.
     */
    public boolean isRelevant(Bundle bundle) { // TODO: (Nad) Isn't this too broad? KARs?
        String loc = bundle.getLocation();
        if (loc == null) {
            return false;
        }
        loc = loc.toLowerCase(Locale.ROOT);
        return (loc.startsWith("file:") || loc.startsWith("http:") || loc.startsWith("https:")) && loc.endsWith(".jar");
    }

    @Override
    public void added(Bundle bundle, AddonInfo object) {
        if (isRelevant(bundle)) {
            Addon addon = toAddon(bundle, object);
            String addonUid = addon.getUid();
            synchronized (this) {
                String oldUid = addonsIdx.put(bundle, addonUid);
                if (oldUid != null) {
                    // This shouldn't happen, but in case it still does, remove cached entries
                    for (Map<String, Addon> localeAddons : addons.values()) {
                        localeAddons.remove(oldUid);
                    }
                }
                Map<String, Addon> rootAddons = addons.get(Locale.ROOT);
                if (rootAddons == null) {
                    rootAddons = new HashMap<>();
                    addons.put(Locale.ROOT, rootAddons);
                }
                rootAddons.put(addonUid, addon);
            }
            logger.debug("Added {} to JAR add-on list", bundle.getSymbolicName());
        }
    }

    @Override
    public void removed(Bundle bundle, AddonInfo object) {
        synchronized (this) {
            String addonUid = addonsIdx.get(bundle);
            if (addonUid == null) {
                return;
            }
            Entry<Locale, Map<String, Addon>> entry;
            Map<String, Addon> localeAddons;
            for (Iterator<Entry<Locale, Map<String, Addon>>> iterator = addons.entrySet().iterator(); iterator.hasNext();) {
                entry = iterator.next();
                localeAddons = entry.getValue();
                localeAddons.remove(addonUid);
                if (localeAddons.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        logger.debug("Removed {} from JAR add-on list", bundle.getSymbolicName());
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public void refreshSource() {
        // This is event based, no refreshing required
    }

    private Addon toAddon(Bundle bundle, AddonInfo addonInfo) {
        String uid = ADDON_ID_PREFIX + addonInfo.getUID();
        Version v = Version.valueOf(bundle.getVersion());
        return Addon.create(uid).withId(addonInfo.getId()).withType(addonInfo.getType()).withInstalled(true, v)
                .withVersion(v).withLabel(addonInfo.getName())
                .withConnection(addonInfo.getConnection()).withCountries(addonInfo.getCountries())
                .withConfigDescriptionURI(addonInfo.getConfigDescriptionURI())
                .withDescription(Objects.requireNonNullElse(addonInfo.getDescription(), bundle.getSymbolicName()))
                .withContentType(ADDONS_CONTENT_TYPE).withLoggerPackages(List.of(bundle.getSymbolicName())).build();
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        Locale l = locale == null ? Locale.ROOT : locale;
        synchronized (this) {
            if (addonsIdx.isEmpty()) {
                return List.of();
            }
            Map<String, Addon> localeAddons = addons.get(l);
            if (localeAddons == null) {
                localeAddons = new HashMap<>();
                addons.put(l, localeAddons);
            }
            if (localeAddons.size() < addonsIdx.size()) {
                // At least one isn't cached, update the cache
                AddonInfo info;
                for (Entry<Bundle, String> entry : addonsIdx.entrySet()) {
                    info = xmlAddonInfoProvider.getAddonInfo(entry.getValue().substring(ADDON_ID_PREFIX.length()), locale);
                    if (info == null) {
                        logger.warn("AddonInfo is null for addon {} ({}), this shouldn't happen", entry.getKey().getSymbolicName(), entry.getValue());
                    } else {
                        localeAddons.put(entry.getValue(), toAddon(entry.getKey(), info));
                    }
                }
            }
            return localeAddons.values().stream().sorted(Comparator.comparing(Addon::getLabel)).toList();
        }
    }

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        final String queryId = id.startsWith(ADDON_ID_PREFIX) ? id : ADDON_ID_PREFIX + id;
        Locale l = locale == null ? Locale.ROOT : locale;
        synchronized (this) {
            if (addonsIdx.isEmpty()) {
                return null;
            }
            Map<String, Addon> localeAddons = addons.get(l);
            if (localeAddons == null) {
                localeAddons = new HashMap<>();
                addons.put(l, localeAddons);
            }
            Addon result = localeAddons.get(queryId);
            if (result != null) {
                return result;
            }
            Bundle bundle = addonsIdx.entrySet().stream().filter((e) -> queryId.equals(e.getValue())).map((e) -> e.getKey()).findAny().orElse(null);
            if (bundle == null) {
                return null;
            }
            AddonInfo info = xmlAddonInfoProvider.getAddonInfo(queryId.substring(ADDON_ID_PREFIX.length()), locale);
            if (info == null) {
                logger.warn("AddonInfo is null for addon {} ({}), this shouldn't happen", bundle.getSymbolicName(), queryId);
                return null;
            }
            result = toAddon(bundle, info);
            localeAddons.put(queryId, result);
            return result;
        }
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return List.copyOf(ADDON_TYPE_MAP.values());
    }

    @Override
    public void install(String id, @Nullable String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uninstall(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }
}
