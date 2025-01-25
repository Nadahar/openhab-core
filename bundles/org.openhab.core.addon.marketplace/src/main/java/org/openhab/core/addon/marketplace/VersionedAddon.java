package org.openhab.core.addon.marketplace;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;

public class VersionedAddon extends Addon {

    protected final @NonNull SortedMap<Version, AddonVersion> versions;
    protected final @NonNull String masterUid; //TODO: (Nad) Remove..?
    protected final @Nullable Version currentVersion;

    protected VersionedAddon(String uid, String masterUid, String type, String id, @Nullable String label,
            @Nullable String version, @Nullable String maturity, @Nullable Set<String> dependsOn,
            boolean compatible, @Nullable String contentType, @Nullable String link, @Nullable String documentationLink,
            @Nullable String issuesLink, @Nullable String author, boolean verifiedAuthor, boolean installed,
            @Nullable String description, @Nullable String detailedDescription, @Nullable String configDescriptionURI,
            @Nullable String keywords, @Nullable List<String> countries, @Nullable String license,
            @Nullable String connection, @Nullable String backgroundColor, @Nullable String imageLink,
            @Nullable Map<String, Object> properties, @Nullable List<String> loggerPackages,
            @Nullable SortedMap<Version, AddonVersion> versions, @Nullable Version currentVersion) {
        super(uid, type, id, label, version, maturity, dependsOn, compatible, contentType, link, documentationLink, issuesLink,
            author, verifiedAuthor, installed, description, detailedDescription, configDescriptionURI, keywords,
            countries, license, connection, backgroundColor, imageLink, properties, loggerPackages);
        if (masterUid.isBlank()) {
            throw new IllegalArgumentException("masterUid cannot be blank");
        }
        this.masterUid = masterUid;
        this.versions = versions == null ? Collections.emptySortedMap() : Collections.unmodifiableSortedMap(versions);
        this.currentVersion = currentVersion;
    }

    public VersionedAddon consolidate() {
        return new Builder(this).build(); // TODO: (Nad) Modify what must be modified
    }

    @NonNull
    public String getMasterUid() {
        return masterUid;
    }

    @NonNull
    public SortedMap<Version, AddonVersion> getVersions() {
        return versions;
    }

    @Nullable
    public Version getCurrentVersion() {
        return currentVersion;
    }

    public static class Builder {
        protected String uid;
        protected String masterUid;
        protected String id;
        protected String label;
        protected String version = "";
        protected String maturity;
        protected Set<String> dependsOn;
        protected boolean compatible = true;
        protected String contentType;
        protected String link;
        protected String documentationLink;
        protected String issuesLink;
        protected String author = "";
        protected boolean verifiedAuthor = false;
        protected boolean installed = false;
        protected String type;
        protected String description;
        protected String detailedDescription;
        protected String configDescriptionURI = "";
        protected String keywords = "";
        protected List<String> countries = List.of();
        protected String license;
        protected String connection = "";
        protected String backgroundColor;
        protected String imageLink;
        protected Map<String, Object> properties = new HashMap<>();
        protected List<String> loggerPackages = List.of();
        protected SortedMap<Version, AddonVersion> versions;
        protected Version currentVersion;

        public Builder(@NonNull String uid) {
            this.uid = uid;
            this.masterUid = uid;
        }

        public Builder(@NonNull Addon addon) {
            this.uid = addon.getUid();
            if (addon instanceof VersionedAddon va) {
                masterUid = va.masterUid;
            } else {
                masterUid = addon.getId();
            }
            id = addon.getId();
            label = addon.getLabel();
            version = addon.getVersion();
            maturity = addon.getMaturity();
            dependsOn = addon.getDependsOn();
            compatible = addon.getCompatible();
            contentType = addon.getContentType();
            link = addon.getLink();
            documentationLink = addon.getDocumentationLink();
            issuesLink = addon.getIssuesLink();
            author = addon.getAuthor();
            verifiedAuthor = addon.isVerifiedAuthor();
            installed = addon.isInstalled();
            type = addon.getType();
            description = addon.getDescription();
            detailedDescription = addon.getDetailedDescription();
            configDescriptionURI = addon.getConfigDescriptionURI();
            keywords = addon.getKeywords();
            countries = List.copyOf(addon.getCountries());
            license = addon.getLicense();
            connection = addon.getConnection();
            backgroundColor = addon.getBackgroundColor();
            imageLink = addon.getImageLink();
            properties.putAll(addon.getProperties());
            loggerPackages = List.copyOf(addon.getLoggerPackages());
            if (addon instanceof VersionedAddon va) {
                SortedMap<Version, AddonVersion> locVersions = createVerionsMap();
                locVersions.putAll(va.versions);
                versions = locVersions;
                currentVersion = va.getCurrentVersion();
            }
        }

        public Builder withUid(String uid) {
            this.uid = uid;
            return this;
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

        public Builder withVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder withMaturity(@Nullable String maturity) {
            this.maturity = maturity;
            return this;
        }

        @Nullable
        public Set<String> getDependsOn() {
            return dependsOn;
        }

        public Builder withDependsOn(@Nullable Set<String> dependsOn) {
            this.dependsOn = dependsOn;
            return this;
        }

        public Builder withCompatible(boolean compatible) {
            this.compatible = compatible;
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
            return this;
        }

        public Builder withIssuesLink(String issuesLink) {
            this.issuesLink = issuesLink;
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

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withDetailedDescription(String detailedDescription) {
            this.detailedDescription = detailedDescription;
            return this;
        }

        public Builder withConfigDescriptionURI(@Nullable String configDescriptionURI) {
            this.configDescriptionURI = Objects.requireNonNullElse(configDescriptionURI, "");
            return this;
        }

        public Builder withKeywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        @Nullable
        public List<String> getCountries() {
            return countries;
        }

        public Builder withCountries(List<String> countries) {
            this.countries = countries;
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

        public Builder withProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder withProperties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        @Nullable
        public List<String> getLoggerPackages() {
            return loggerPackages;
        }

        public Builder withLoggerPackages(List<String> loggerPackages) {
            this.loggerPackages = loggerPackages;
            return this;
        }

        @Nullable
        public SortedMap<Version, AddonVersion> getVersions() {
            return versions;
        }

        public Builder withAddonVersion(AddonVersion addonVersion) {
            if (addonVersion.getVersion() == null) {
                throw new IllegalArgumentException("Version cannot be null");
            }
            SortedMap<Version, AddonVersion> locVersions = versions;
            if (locVersions == null) {
                locVersions = createVerionsMap();
            }
            locVersions.put(addonVersion.getVersion(), addonVersion);
            versions = locVersions;
            return this;
        }

        public Builder withCurrentVersion(@Nullable Version currentVersion) {
            this.currentVersion = currentVersion;
            return this;
        }

        protected SortedMap<Version, AddonVersion> createVerionsMap() {
            return new TreeMap<>(new Comparator<Version>() {

                @Override
                public int compare(Version o1, Version o2) {
                    // Sort newest first
                    return o2.compareTo(o1);
                }
            });
        }

        public VersionedAddon build() {
            return new VersionedAddon(uid, masterUid, type, id, label, version, maturity, dependsOn, compatible,
                contentType, link, documentationLink, issuesLink, author,
                verifiedAuthor, installed, description, detailedDescription, configDescriptionURI, keywords,
                countries, license, connection, backgroundColor, imageLink,
                properties.isEmpty() ? null : properties, loggerPackages, versions, currentVersion);
        }
    }
}
