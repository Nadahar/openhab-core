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
package org.openhab.core.karaf.internal;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoRegistry;
import org.openhab.core.addon.AddonService;
import org.openhab.core.addon.AddonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is an implementation of an openHAB {@link AddonService} using the Karaf features service. This
 * exposes all openHAB add-ons through the REST API and allows UIs to dynamically install and uninstall them.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractKarafAddonService implements AddonService { // TODO: (Nad) JavaDocs
    protected static final String ADDONS_CONTENT_TYPE = "application/vnd.openhab.feature;type=karaf";

    private final Logger logger = LoggerFactory.getLogger(AbstractKarafAddonService.class);

    private final FeaturesService featuresService;
    private final FeatureInstaller featureInstaller;

    private final AddonInfoRegistry addonInfoRegistry;

    public AbstractKarafAddonService(FeatureInstaller featureInstaller,
            FeaturesService featuresService, AddonInfoRegistry addonInfoRegistry) {
        this.featureInstaller = featureInstaller;
        this.featuresService = featuresService;
        this.addonInfoRegistry = addonInfoRegistry;
    }

    @Override
    public void refreshSource() {
        // TODO: (Nad) ?
    }

    @Override
    public List<Addon> getAddons(@Nullable Locale locale) {
        try {
            return Arrays.stream(featuresService.listFeatures()).filter(this::isAddon).map(f -> getAddon(f, locale))
                    .sorted(Comparator.comparing(Addon::getLabel)).toList();
        } catch (Exception e) {
            logger.error("Exception while retrieving features: {}", e.getMessage());
            return List.of();
        }
    }

    protected abstract boolean isAddon(Feature feature);

    protected abstract String getFeaturePrefix();

    @Override
    public @Nullable Addon getAddon(String id, @Nullable Locale locale) {
        Feature feature;
        try {
            feature = featuresService.getFeature(getFeaturePrefix() + id);
            return getAddon(feature, locale);
        } catch (Exception e) {
            logger.error("Exception while querying feature '{}'", id);
            return null;
        }
    }

    protected abstract Addon toAddon(String uid, String name, String type, Feature feature, boolean isInstalled, @Nullable AddonInfo addonInfo);

    protected Addon getAddon(Feature feature, @Nullable Locale locale) {
        String name = getName(feature.getName());
        String type = getAddonType(feature.getName());
        String uid = type + Addon.ADDON_SEPARATOR + name;
        return toAddon(uid, name, type, feature, featuresService.isInstalled(feature), addonInfoRegistry.getAddonInfo(uid, locale));
    }

    @Override
    public List<AddonType> getTypes(@Nullable Locale locale) {
        return AddonType.DEFAULT_TYPES;
    }

    @Override
    public void install(String id, @Nullable String version) {
        featureInstaller.addAddon(getAddonType(id), getName(id));
    }

    @Override
    public void uninstall(String id) {
        featureInstaller.removeAddon(getAddonType(id), getName(id));
    }

    @Override
    public @Nullable String getAddonId(URI addonURI) {
        return null;
    }

    protected String getAddonType(String name) { // TODO: (Nad) Figure out these two
        String str = name.startsWith(getFeaturePrefix()) ? name.substring(FeatureInstaller.PREFIX.length()) : name;
        int index = str.indexOf(Addon.ADDON_SEPARATOR);
        return index == -1 ? str : str.substring(0, index);
    }

    protected String getName(String name) {
        String str = name.startsWith(FeatureInstaller.PREFIX) ? name.substring(FeatureInstaller.PREFIX.length()) : name;
        int index = str.indexOf(Addon.ADDON_SEPARATOR);
        return index == -1 ? "" : str.substring(index + Addon.ADDON_SEPARATOR.length());
    }
}
