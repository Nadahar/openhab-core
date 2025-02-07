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
package org.openhab.core.addon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This class defines an add-on.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Yannick Schaus - Add fields
 */
public class Addon {
    public static final Set<String> CODE_MATURITY_LEVELS = Set.of("alpha", "beta", "mature", "stable");
    public static final String ADDON_SEPARATOR = "-";
    public static final Set<String> MARKETPLACE_RESOURCE_PROPERTIES = Set.of("json_content", "yaml_content",
            "jar_download_url", "kar_download_url", "json_download_url", "yaml_download_url");

    private final @NonNull String uid;
    private final @NonNull String id;
    private final @Nullable String label;
    private final @Nullable Version version;
    private final @Nullable Version baseVersion;
    private final @Nullable String maturity;
    private final @Nullable @Exclude String baseMaturity;
    private final @Nullable Version defaultVersion;
    private final @NonNull Set<@NonNull String> dependsOn;
    private final @NonNull @Exclude Set<@NonNull String> baseDependsOn;
    private final boolean compatible;
    private final @Exclude boolean baseCompatible;
    private final @Nullable String contentType;
    private final @Nullable String link;
    private final @Nullable String documentationLink;
    private final @Nullable @Exclude String baseDocumentationLink;
    private final @Nullable String issuesLink;
    private final @Nullable @Exclude String baseIssuesLink;
    private final @NonNull String author;
    private final boolean verifiedAuthor;
    private boolean installed;
    private @Nullable Version installedVersion;
    private final @NonNull String type;
    private final @Nullable String description;
    private final @Nullable @Exclude String baseDescription;
    private final @Nullable String detailedDescription;
    private final @Nullable @Exclude String baseDetailedDescription;
    private final @NonNull String configDescriptionURI;
    private final @NonNull String keywords;
    private final @NonNull @Exclude String baseKeywords;
    private final @NonNull List<@NonNull String> countries;
    private final @NonNull @Exclude List<@NonNull String> baseCountries;
    private final @Nullable String license;
    private final @NonNull String connection;
    private final @Nullable String backgroundColor;
    private final @Nullable String imageLink;
    private final @NonNull Map<@NonNull String, @NonNull Object> properties;
    private final @NonNull @Exclude Map<@NonNull String, @NonNull Object> baseProperties;
    private final @NonNull List<@NonNull String> loggerPackages;
    private final @NonNull @Exclude List<@NonNull String> baseLoggerPackages;
    private final @NonNull SortedMap<@NonNull Version, @NonNull AddonVersion> versions;

    /**
     * Creates a new Addon instance
     *
     * @param uid the id of the add-on (e.g. "binding-dmx", "json:transform-format" or "marketplace:123456")
     * @param type the type id of the add-on (e.g. "automation")
     * @param uid the technical name of the add-on (e.g. "influxdb")
     * @param label the label of the add-on
     * @param version the version of the add-on
     * @param maturity the maturity level of this version
     * @param dependsOn the other add-ons this add-on depends on
     * @param compatible if this add-on is compatible with the current core version
     * @param contentType the content type of the add-on
     * @param link the link to find more information about the add-on (may be null)
     * @param documentationLink the link to the add-on documentation (may be null)
     * @param issuesLink the link to the add-on issues tracker (may be null)
     * @param author the author of the add-on
     * @param verifiedAuthor true, if the author is verified
     * @param installed true, if the add-on is installed, false otherwise
     * @param description the description of the add-on (may be null)
     * @param detailedDescription the detailed description of the add-on (may be null)
     * @param configDescriptionURI the URI to the configuration description for this add-on
     * @param keywords the keywords for this add-on
     * @param countries a list of ISO 3166 codes relevant to this add-on
     * @param license the SPDX license identifier
     * @param connection a string describing the type of connection (local or cloud, push or pull...) this add-on uses,
     *            if applicable.
     * @param backgroundColor for displaying the add-on (may be null)
     * @param imageLink the link to an image (png/svg) (may be null)
     * @param properties a {@link Map} containing addition information
     * @param loggerPackages a {@link List} containing the package names belonging to this add-on
     * @param versions a {@link SortedMap} containing the {@link AddonVersion}s if applicable
     * @param installedVersion the currently installed {@link Version}, if any (may be null)
     * @throws IllegalArgumentException when a mandatory parameter is invalid
     */
    protected Addon(String uid, String type, String id, @Nullable String label, @Nullable Version version, @Nullable Version baseVersion,
            @Nullable String maturity, @Nullable String baseMaturity,
            @Nullable Set<@NonNull String> dependsOn, @Nullable Set<@NonNull String> baseDependsOn,
            boolean compatible, boolean baseCompatible,
            @Nullable String contentType, @Nullable String link,
            @Nullable String documentationLink, @Nullable String baseDocumentationLink,
            @Nullable String issuesLink, @Nullable String baseIssuesLink, @Nullable String author, boolean verifiedAuthor,
            boolean installed, @Nullable Version installedVersion, @Nullable String description, @Nullable String baseDescription,
            @Nullable String detailedDescription, @Nullable String baseDetailedDescription, @Nullable String configDescriptionURI,
            @Nullable String keywords, @Nullable String baseKeywords,
            @Nullable List<@NonNull String> countries, @Nullable List<@NonNull String> baseCountries,
            @Nullable String license, @Nullable String connection,
            @Nullable String backgroundColor, @Nullable String imageLink,
            @Nullable Map<@NonNull String, @NonNull Object> properties, @Nullable Map<@NonNull String, @NonNull Object> baseProperties,
            @Nullable List<@NonNull String> loggerPackages, @Nullable List<@NonNull String> baseLoggerPackages,
            @Nullable Map<@NonNull Version, @NonNull AddonVersion> versions) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("uid must not be empty");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be empty");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be empty");
        }

        this.uid = uid;
        this.type = type;
        this.id = id;

        this.label = label;
        this.version = version;
        this.baseVersion = baseVersion;
        this.maturity = maturity;
        this.baseMaturity = baseMaturity;
        this.dependsOn = dependsOn == null ? Set.of() : Set.copyOf(dependsOn);
        this.baseDependsOn = baseDependsOn == null ? Set.of() : Set.copyOf(baseDependsOn);
        this.compatible = compatible;
        this.baseCompatible = baseCompatible;
        this.contentType = contentType;
        this.description = description;
        this.baseDescription = baseDescription;
        this.detailedDescription = detailedDescription;
        this.baseDetailedDescription = baseDetailedDescription;
        this.configDescriptionURI = configDescriptionURI == null || configDescriptionURI.isBlank() ? "" : configDescriptionURI;
        this.keywords = keywords == null || keywords.isBlank() ? "" : keywords;
        this.baseKeywords = baseKeywords == null || baseKeywords.isBlank() ? "" : baseKeywords;
        this.countries = countries == null ? List.of() : List.copyOf(countries);
        this.baseCountries = baseCountries == null ? List.of() : List.copyOf(baseCountries);
        this.license = license;
        this.connection = connection == null || connection.isBlank() ? "" : connection;
        this.backgroundColor = backgroundColor;
        this.link = link;
        this.documentationLink = documentationLink;
        this.baseDocumentationLink = baseDocumentationLink;
        this.issuesLink = issuesLink;
        this.baseIssuesLink = baseIssuesLink;
        this.imageLink = imageLink;
        this.author = author == null || author.isBlank() ? "" : author;
        this.verifiedAuthor = verifiedAuthor;
        this.installed = installed;
        this.installedVersion = installed ? installedVersion : null;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
        this.baseProperties = baseProperties == null ? Map.of() : Map.copyOf(baseProperties);
        this.loggerPackages = loggerPackages == null ? List.of() : List.copyOf(loggerPackages);
        this.baseLoggerPackages = baseLoggerPackages == null ? List.of() : List.copyOf(baseLoggerPackages);
        if (versions == null || versions.isEmpty()) {
            this.versions = Collections.emptySortedMap();
        } else {
            SortedMap<@NonNull Version, @NonNull AddonVersion> locVersions = createVersionsMap();
            locVersions.putAll(versions);
            this.versions = Collections.unmodifiableSortedMap(locVersions);
        }
        this.defaultVersion = resolveDefaultVersion();
    }

    /**
     * The type of the addon (same as id of {@link AddonType})
     */
    public @NonNull String getType() {
        return type;
    }

    /**
     * The uid of the add-on (e.g. "binding-dmx", "json:transform-format" or "marketplace:123456")
     */
    public @NonNull String getUid() {
        return uid;
    }

    /**
     * The id of the add-on (e.g. "influxdb")
     */
    public @NonNull String getId() {
        return id;
    }

    /**
     * The label of the add-on
     */
    public @Nullable String getLabel() {
        return label;
    }

    /**
     * The (optional) link to find more information about the add-on
     */
    public @Nullable String getLink() {
        return link;
    }

    /**
     * The (optional) link to the add-on documentation
     */
    public @Nullable String getDocumentationLink() {
        return documentationLink;
    }

    /**
     * The (optional) link to the add-on issues tracker
     */
    public @Nullable String getIssuesLink() {
        return issuesLink;
    }

    /**
     * The author of the add-on
     */
    public @NonNull String getAuthor() {
        return author;
    }

    /**
     * Whether the add-on author is verified or not
     */
    public boolean isVerifiedAuthor() {
        return verifiedAuthor;
    }

    /**
     * The version of the add-on
     */
    public @Nullable Version getVersion() {
        return version;
    }

    /**
     * The "base" version of the add-on if the add-on is versioned
     */
    public @Nullable Version getBaseVersion() {
        return baseVersion;
    }

    /**
     * The maturity level of this version
     */
    public @Nullable String getMaturity() {
        return maturity;
    }

    /**
     * The default version if several, prefers compatible, released and latest
     */
    public @Nullable Version getDefaultVersion() {
        return defaultVersion;
    }

    /**
     * The other add-ons this add-on depends on
     */
    public @NonNull Set<@NonNull String> getDependsOn() {
        return dependsOn;
    }

    /**
     * The (expected) compatibility of this add-on
     */
    public boolean getCompatible() {
        return compatible;
    }

    /**
     * The content type of the add-on
     */
    public @Nullable String getContentType() {
        return contentType;
    }

    /**
     * The description of the add-on
     */
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * The detailed description of the add-on
     */
    public @Nullable String getDetailedDescription() {
        return detailedDescription;
    }

    /**
     * The URI to the configuration description for this add-on
     */
    public @NonNull String getConfigDescriptionURI() {
        return configDescriptionURI;
    }

    /**
     * The keywords for this add-on
     */
    public @NonNull String getKeywords() {
        return keywords;
    }

    /**
     * A list of ISO 3166 codes relevant to this add-on
     */
    public @NonNull List<@NonNull String> getCountries() {
        return countries;
    }

    /**
     * The SPDX License identifier for this addon
     */
    public @Nullable String getLicense() {
        return license;
    }

    /**
     * A string describing the type of connection (local, cloud, cloudDiscovery) this add-on uses, if applicable.
     */
    public @NonNull String getConnection() {
        return connection;
    }

    /**
     * A set of additional properties relative to this add-on
     */
    public @NonNull Map<@NonNull String, @NonNull Object> getProperties() {
        return properties;
    }

    /**
     * true, if the add-on is installed, false otherwise
     */
    public synchronized boolean isInstalled() {
        return installed;
    }

    /**
     * Sets the installed state
     */
    public synchronized void setInstalled(boolean installed) {
        this.installed = installed;
        this.installedVersion = null;
    }

    /**
     * Sets the installed state and version
     */
    public synchronized void setInstalled(boolean installed, @Nullable Version version) {
        this.installed = installed;
        this.installedVersion = installed ? version : null;
    }

    /**
     * the currently used {{@code versions} entry (may be null)
     */
    public synchronized @Nullable Version getInstalledVersion() {
        return installedVersion;
    }

    /**
     * The background color for rendering the add-on
     */
    public @Nullable String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * A link to an image (png/svg) for the add-on
     */
    public @Nullable String getImageLink() {
        return imageLink;
    }

    /**
     * The package names that are associated with this add-on
     */
    public @NonNull List<@NonNull String> getLoggerPackages() {
        return loggerPackages;
    }

    public boolean isVersioned() {
        return !versions.isEmpty();
    }

    /**
     * The {@link SortedMap} containing the {@link AddonVersion}s, if any
     */
    public @NonNull SortedMap<@NonNull Version, @NonNull AddonVersion> getVersions() {
        return versions;
    }

    /**
     * Merges the information from a specific {@link AddonVersion} with the "base information"
     * and returns a new combined {@link Addon}.
     * <p>
     * <b>Note:</b> This should only be called on a "base instance", not on an instance that has
     * already been "merged" with another version.
     *
     * @param version the {@link Version} whose {@link AddonVersion} to merge
     * @return The merged {@link Addon}
     * @throws IllegalArgumentException If the version doesn't exist
     */
    public @NonNull Addon mergeVersion(@NonNull Version version) {
        if (version.equals(this.version)) {
            return this;
        }
        AddonVersion addonVersion = versions.get(version);
        if (addonVersion == null) {
            throw new IllegalArgumentException("Non-existing version " + version);
        }

        String s;
        Builder builder = new Builder(this, false);
        builder.withVersion(version).withCompatible(addonVersion.isCompatible());
        if (!addonVersion.getCountries().isEmpty()) {
            List<@NonNull String> c = new ArrayList<>(baseCountries);
            for (String country : addonVersion.getCountries()) {
                if (!c.contains(country)) {
                    c.add(country);
                }
            }
            builder.withCountries(c);
        } else {
            builder.withCountries(baseCountries);
        }

        if ((s = addonVersion.getDescription()) != null && !s.isBlank()) {
            s = s.trim();
            String s2;
            if ((s2 = baseDescription) != null && !s2.isBlank()) {
                builder.withDescription(s + "\n\n" + s2);
            } else {
                // Ideally, the addonVersion description should be used as the description here. But, because the
                // logic elsewhere is "use description if exists - otherwise use detailedDescription", care must be
                // taken not to "override" detailedDescription.
                builder.withDescription((s2 = baseDetailedDescription) == null || s2.isBlank() ? s : baseDescription);
            }
            if ((s2 = baseDetailedDescription) != null && !s2.isBlank()) {
                int idx = DiscourseCookedUtil.findParagraphInsertionPoint(s2);
                if (idx >= 0) {
                    builder.withDetailedDescription(s2.substring(0, idx) + "<p>" + s + "</p>" + s2.substring(idx));
                } else {
                    // Didn't find the insertion point - just slap it at the very top
                    builder.withDetailedDescription("<p>" + s + "</p>" + s2);
                }
            } else {
                builder.withDetailedDescription("<p>" + s + "</p>");
            }
        } else {
            builder.withDescription(baseDescription);
            builder.withDetailedDescription(baseDetailedDescription);
        }

        builder.withDocumentationLink((s = addonVersion.getDocumentationLink()) != null ? s : baseDocumentationLink);
        builder.withIssuesLink((s = addonVersion.getIssuesLink()) != null ? s : baseIssuesLink);
        builder.withKeywords((s = addonVersion.getKeywords()) != null ? s : baseKeywords);
        builder.withMaturity((s = addonVersion.getMaturity()) != null && !s.isBlank() ? s : baseMaturity);

        if (!addonVersion.getLoggerPackages().isEmpty()) {
            List<@NonNull String> l = new ArrayList<>(baseLoggerPackages);
            for (String lPackage : addonVersion.getLoggerPackages()) {
                if (!l.contains(lPackage)) {
                    l.add(lPackage);
                }
            }
            builder.withLoggerPackages(l);
        } else {
            builder.withLoggerPackages(baseLoggerPackages);
        }

        if (!addonVersion.getDependsOn().isEmpty()) {
            Set<@NonNull String> deps;
            if (!(deps = baseDependsOn).isEmpty()) {
                builder.withDependsOn(Stream.concat(deps.stream(), addonVersion.getDependsOn().stream())
                        .distinct().collect(Collectors.toSet()));
            } else {
                builder.withDependsOn(addonVersion.getDependsOn());
            }
        } else {
            builder.withDependsOn(baseDependsOn);
        }

        // Remove "resource" properties, they shouldn't be part of the merge
        Map<@NonNull String, @NonNull Object> newProperties = baseProperties.entrySet().stream()
                .filter(e -> !MARKETPLACE_RESOURCE_PROPERTIES.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        newProperties.putAll(addonVersion.getProperties());
        builder.withProperties(newProperties);

        // TODO: (Nad) Make the rest
        return builder.build();
    }

    protected @Nullable Version resolveDefaultVersion() {
        Version result = this.version;
        if (this.versions.isEmpty()) {
            return result;
        }
        if (this.versions.size() == 1) {
            return this.versions.firstKey();
        }

        List<@NonNull AddonVersion> versions = new ArrayList<>(this.versions.values());
        versions.sort(new Comparator<AddonVersion>() {

            @Override
            public int compare(AddonVersion av1, AddonVersion av2) {
                Version v1 = av1.getVersion();
                Version v2 = av2.getVersion();

                // Compatible before incompatible
                if (av1.isCompatible() != av2.isCompatible()) {
                    return av1.isCompatible() ? -1 : 1;
                }

                // Stable before unstable
                if (av1.isStable()  != av2.isStable()) {
                    return av1.isStable() ? -1 : 1;
                }

                // Newest first
                return v2.compareTo(v1);
            }
        });
        return versions.get(0).getVersion();
    }

    protected SortedMap<@NonNull Version, @NonNull AddonVersion> createVersionsMap() {
        return new TreeMap<>(new Comparator<Version>() {

            @Override
            public int compare(Version o1, Version o2) {
                // Sort newest first
                return o2.compareTo(o1);
            }
        });
    }

    /**
     * Create a builder for an {@link Addon}
     *
     * @param uid the UID of the add-on (e.g. "binding-dmx", "json:transform-format" or "marketplace:123456")
     * @return the builder
     */
    public static Builder create(String uid) {
        return new Builder(uid);
    }

    public static Builder create(Addon addon) {
        return new Builder(addon, true);
    }

    public static class Builder {
        protected boolean setBase = true;
        protected @NonNull String uid;
        protected @Nullable String id;
        protected @Nullable String label;
        protected @Nullable Version version;
        protected @Nullable Version baseVersion;
        protected @Nullable String maturity;
        protected @Nullable String baseMaturity;
        protected @Nullable Set<@NonNull String> dependsOn;
        protected @Nullable Set<@NonNull String> baseDependsOn;
        protected boolean compatible = true;
        protected boolean baseCompatible = true;
        protected @Nullable String contentType;
        protected @Nullable String link;
        protected @Nullable String documentationLink;
        protected @Nullable String baseDocumentationLink;
        protected @Nullable String issuesLink;
        protected @Nullable String baseIssuesLink;
        protected @Nullable String author;
        protected boolean verifiedAuthor = false;
        protected boolean installed = false;
        protected @Nullable Version installedVersion;
        protected @Nullable String type;
        protected @Nullable String description;
        protected @Nullable String baseDescription;
        protected @Nullable String detailedDescription;
        protected @Nullable String baseDetailedDescription;
        protected @Nullable String configDescriptionURI;
        protected @Nullable String keywords;
        protected @Nullable String baseKeywords;
        protected @Nullable List<@NonNull String> countries;
        protected @Nullable List<@NonNull String> baseCountries;
        protected @Nullable String license;
        protected @Nullable String connection;
        protected @Nullable String backgroundColor;
        protected @Nullable String imageLink;
        protected @Nullable Map<@NonNull String, @NonNull Object> properties;
        protected @Nullable Map<@NonNull String, @NonNull Object> baseProperties;
        protected @Nullable List<@NonNull String> loggerPackages;
        protected @Nullable List<@NonNull String> baseLoggerPackages;
        protected @Nullable Map<@NonNull Version, @NonNull AddonVersion> versions;

        protected Builder(@NonNull String uid) {
            this.uid = uid;
        }

        protected Builder(Addon addon, boolean setBase) {
            this.setBase = setBase;
            this.uid = addon.uid;
            this.id = addon.id;
            this.label = addon.label;
            this.version = addon.version;
            this.baseVersion = addon.baseVersion;
            this.maturity = addon.maturity;
            this.baseMaturity = addon.baseMaturity;
            this.dependsOn = addon.dependsOn;
            this.baseDependsOn = addon.baseDependsOn;
            this.compatible = addon.compatible;
            this.baseCompatible = addon.baseCompatible;
            this.contentType = addon.contentType;
            this.link = addon.link;
            this.documentationLink = addon.documentationLink;
            this.baseDocumentationLink = addon.baseDocumentationLink;
            this.issuesLink = addon.issuesLink;
            this.baseIssuesLink = addon.baseIssuesLink;
            this.author = addon.author;
            this.verifiedAuthor = addon.verifiedAuthor;
            synchronized (addon) {
                this.installed = addon.installed;
                this.installedVersion = addon.installedVersion;
            }
            this.type = addon.type;
            this.description = addon.description;
            this.baseDescription = addon.baseDescription;
            this.detailedDescription = addon.detailedDescription;
            this.baseDetailedDescription = addon.baseDetailedDescription;
            this.configDescriptionURI = addon.configDescriptionURI;
            this.keywords = addon.keywords;
            this.baseKeywords = addon.baseKeywords;
            this.countries = addon.countries;
            this.baseCountries = addon.baseCountries;
            this.license = addon.license;
            this.connection = addon.connection;
            this.backgroundColor = addon.backgroundColor;
            this.imageLink = addon.imageLink;
            this.properties = addon.properties;
            this.baseProperties = addon.baseProperties;
            this.loggerPackages = addon.loggerPackages;
            this.baseLoggerPackages = addon.baseLoggerPackages;
            this.versions = new HashMap<>(addon.versions);
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder withVersion(@Nullable Version version) {
            this.version = version;
            if (this.setBase) {
                this.baseVersion = version;
            }
            return this;
        }

        public Builder withMaturity(@Nullable String maturity) {
            this.maturity = maturity;
            if (this.setBase) {
                this.baseMaturity = maturity;
            }
            return this;
        }

        public @Nullable Set<@NonNull String> getDependsOn() {
            return dependsOn;
        }

        public Builder withDependsOn(@Nullable Set<@NonNull String> dependsOn) {
            this.dependsOn = dependsOn;
            if (this.setBase) {
                this.baseDependsOn = dependsOn;
            }
            return this;
        }

        public Builder withCompatible(boolean compatible) {
            this.compatible = compatible;
            if (this.setBase) {
                this.baseCompatible = compatible;
            }
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withLink(String link) {
            this.link = link;
            return this;
        }

        public Builder withDocumentationLink(String documentationLink) {
            this.documentationLink = documentationLink;
            if (this.setBase) {
                this.baseDocumentationLink = documentationLink;
            }
            return this;
        }

        public Builder withIssuesLink(String issuesLink) {
            this.issuesLink = issuesLink;
            if (this.setBase) {
                this.baseIssuesLink = issuesLink;
            }
            return this;
        }

        public Builder withAuthor(@Nullable String author) {
            this.author = Objects.requireNonNullElse(author, "");
            return this;
        }

        public Builder withAuthor(String author, boolean verifiedAuthor) {
            this.author = author;
            this.verifiedAuthor = verifiedAuthor;
            return this;
        }

        public Builder withInstalled(boolean installed) {
            this.installed = installed;
            return this;
        }

        public Builder withInstalled(boolean installed, @Nullable Version version) {
            this.installed = installed;
            this.installedVersion = installed ? version : null;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            if (this.setBase) {
                this.baseDescription = description;
            }
            return this;
        }

        public Builder withDetailedDescription(String detailedDescription) {
            this.detailedDescription = detailedDescription;
            if (this.setBase) {
                this.baseDetailedDescription = detailedDescription;
            }
            return this;
        }

        public Builder withConfigDescriptionURI(@Nullable String configDescriptionURI) {
            this.configDescriptionURI = Objects.requireNonNullElse(configDescriptionURI, "");
            return this;
        }

        public Builder withKeywords(String keywords) {
            this.keywords = keywords;
            if (this.setBase) {
                this.baseKeywords = keywords;
            }
            return this;
        }

        public @Nullable List<@NonNull String> getCountries() {
            return countries;
        }

        public Builder withCountries(List<@NonNull String> countries) {
            this.countries = countries;
            if (this.setBase) {
                this.baseCountries = countries;
            }
            return this;
        }

        public Builder withLicense(@Nullable String license) {
            this.license = license;
            return this;
        }

        public Builder withConnection(String connection) {
            this.connection = connection;
            return this;
        }

        public Builder withBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder withImageLink(@Nullable String imageLink) {
            this.imageLink = imageLink;
            return this;
        }

        public Builder withProperty(@NonNull String key, @NonNull Object value) {
            Map<@NonNull String, @NonNull Object> props = this.properties;
            if (props == null) {
                props = new HashMap<>();
            }
            props.put(key, value);
            this.properties = props;
            if (this.setBase) {
                this.baseProperties = props;
            }
            return this;
        }

        public Builder withProperties(@Nullable Map<@NonNull String, @NonNull Object> properties) {
            this.properties = properties;
            if (this.setBase) {
                this.baseProperties = properties;
            }
            return this;
        }

        public @Nullable List<@NonNull String> getLoggerPackages() {
            return loggerPackages;
        }

        public Builder withLoggerPackages(List<@NonNull String> loggerPackages) {
            this.loggerPackages = loggerPackages;
            if (this.setBase) {
                this.baseLoggerPackages = loggerPackages;
            }
            return this;
        }

        @Nullable
        public Map<@NonNull Version, @NonNull AddonVersion> getVersions() {
            return versions;
        }

        public Builder withAddonVersion(@NonNull AddonVersion addonVersion) {
            Map<@NonNull Version, @NonNull AddonVersion> locVersions = versions;
            if (locVersions == null) {
                locVersions = new HashMap<>();
            }
            locVersions.put(addonVersion.getVersion(), addonVersion);
            versions = locVersions;
            return this;
        }

        public Builder withAddonVersions(@Nullable Map<@NonNull Version, @NonNull AddonVersion> versions) {
            this.versions = versions;
            return this;
        }

        public Addon build() {
            return new Addon(uid, type, id, label, version, baseVersion, maturity, baseMaturity,
                    dependsOn, baseDependsOn, compatible, baseCompatible, contentType, link,
                    documentationLink, baseDocumentationLink, issuesLink, baseIssuesLink, author, verifiedAuthor,
                    installed, installedVersion, description, baseDescription,
                    detailedDescription, baseDetailedDescription, configDescriptionURI, keywords, baseKeywords,
                    countries, baseCountries, license, connection,
                    backgroundColor, imageLink, properties, baseProperties, loggerPackages, baseLoggerPackages, versions);
        }
    }
}
