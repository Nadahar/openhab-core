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
package org.openhab.core.io.rest.core.internal.addons;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.AddonVersion;
import org.openhab.core.addon.Version;

/**
 * A DTO representing an {@link Addon} in the REST API.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class AddonDTO {

    public String uid;
    public String id;
    public String label;
    public String version;
    public String maturity;
    public String defaultVersion;
    public @Nullable Set<@NonNull String> dependsOn;
    public boolean compatible;
    public String contentType;
    public String link;
    public String documentationLink;
    public String issuesLink;
    public String author;
    public boolean verifiedAuthor;
    public boolean installed;
    public String installedVersion;
    public String type;
    public String description;
    public String detailedDescription;
    public String configDescriptionURI;
    public String keywords;
    public @Nullable List<@NonNull String> countries;
    public String license;
    public String connection;
    public String backgroundColor;
    public String imageLink;
    public @Nullable Map<@NonNull String, @NonNull Object> properties;
    public List<@NonNull String> loggerPackages;
    public @Nullable LinkedHashMap<@NonNull String, @NonNull AddonVersionDTO> versions;

    /**
     * Creates a new {@link Addon} from this {@link AddonDTO}.
     *
     * @return The new {@link Addon}.
     */
    public @NonNull Addon toAddon() { //TODO: (Nad) Needed?
        Addon.Builder b = Addon.create(this.uid, false);
        if (this.id != null) {
            b.withId(this.id);
        }
        if (this.label != null) {
            b.withLabel(this.label);
        }
        if (this.version != null) {
            b.withVersion(Version.valueOf(this.version));
        }
        if (this.maturity != null) {
            b.withMaturity(this.maturity);
        }
        b.withDependsOn(this.dependsOn).withCompatible(this.compatible);
        if (this.contentType != null) {
            b.withContentType(this.contentType);
        }
        if (this.link != null) {
            b.withLink(this.link);
        }
        if (this.documentationLink != null) {
            b.withDocumentationLink(this.documentationLink);
        }
        if (this.issuesLink != null) {
            b.withIssuesLink(this.issuesLink);
        }
        if (this.author != null) {
            b.withAuthor(this.author, this.verifiedAuthor);
        }
        if (this.installedVersion != null) {
            b.withInstalled(this.installed, Version.valueOf(this.installedVersion));
        } else {
            b.withInstalled(this.installed);
        }
        if (this.type != null) {
            b.withType(this.type);
        }
        if (this.description != null) {
            b.withDescription(this.description);
        }
        if (this.detailedDescription != null) {
            b.withDetailedDescription(this.detailedDescription);
        }
        if (this.configDescriptionURI != null) {
            b.withConfigDescriptionURI(this.configDescriptionURI);
        }
        if (this.keywords != null) {
            b.withKeywords(this.keywords);
        }
        if (this.countries != null) {
            b.withCountries(this.countries);
        }
        if (this.license != null) {
            b.withLicense(this.license);
        }
        if (this.connection != null) {
            b.withConnection(this.connection);
        }
        if (this.backgroundColor != null) {
            b.withBackgroundColor(this.backgroundColor);
        }
        if (this.imageLink != null) {
            b.withImageLink(this.imageLink);
        }
        if (this.properties != null) {
            b.withProperties(this.properties);
        }
        if (this.loggerPackages != null) {
            b.withLoggerPackages(this.loggerPackages);
        }
        if (this.versions != null) {
            for (AddonVersionDTO addonVersion : this.versions.values()) {
                b.withAddonVersion(addonVersion.toAddonVersion());
            }
        }
        return b.build();
    }

    /**
     * Creates a new {@link AddonDTO} from the specified {@link Addon}.
     *
     * @param addon the {@link Addon}.
     * @return The new {@link AddonDTO}.
     */
    public static @NonNull AddonDTO fromAddon(@NonNull Addon addon) {
        AddonDTO result = new AddonDTO();

        result.uid = addon.getUid();
        result.id = addon.getId();
        result.label = addon.getLabel();
        Version v = addon.getVersion();
        if (v != null) {
            result.version = v.toString();
        }
        result.maturity = addon.getMaturity();
        v = addon.getDefaultVersion();
        result.defaultVersion = v == null ? null : v.toString();
        Set<@NonNull String> stringSet = addon.getDependsOn();
        if (!stringSet.isEmpty()) {
            result.dependsOn = stringSet;
        }
        result.compatible = addon.getCompatible();
        result.contentType = addon.getContentType();
        result.link = addon.getLink();
        result.documentationLink = addon.getDocumentationLink();
        result.issuesLink = addon.getIssuesLink();
        result.author = addon.getAuthor();
        result.verifiedAuthor = addon.isVerifiedAuthor();
        result.installed = addon.isInstalled();
        v = addon.getInstalledVersion();
        if (v != null) {
            result.installedVersion = v.toString();
        }
        result.type = addon.getType();
        result.description = addon.getDescription();
        result.detailedDescription = addon.getDetailedDescription();
        result.configDescriptionURI = addon.getConfigDescriptionURI();
        result.keywords = addon.getKeywords();
        List<@NonNull String> stringList = addon.getCountries();
        if (!stringList.isEmpty()) {
            result.countries = stringList;
        }
        result.license = addon.getLicense();
        result.connection = addon.getConnection();
        result.backgroundColor = addon.getBackgroundColor();
        result.imageLink = addon.getImageLink();
        Map<@NonNull String, @NonNull Object> map = addon.getProperties();
        if (!map.isEmpty()) {
            result.properties = map;
        }
        result.loggerPackages = addon.getLoggerPackages();
        SortedMap<@NonNull Version, @NonNull AddonVersion> versions = addon.getVersions();
        if (!versions.isEmpty()) {
            LinkedHashMap<String, AddonVersionDTO> dtoVersions = new LinkedHashMap<>();
            for (Entry<@NonNull Version, @NonNull AddonVersion> entry : versions.entrySet()) {
                dtoVersions.put(entry.getKey().toString(), AddonVersionDTO.fromAddonVersion(entry.getValue()));
            }
            result.versions = dtoVersions;
        }

        return result;
    }
}
