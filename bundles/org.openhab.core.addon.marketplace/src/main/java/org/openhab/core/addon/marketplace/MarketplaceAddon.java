package org.openhab.core.addon.marketplace;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.Addon;


public class MarketplaceAddon extends Addon {

    protected final SortedMap<Version, AddonVersion> versions; // TODO: (Nad) Null or not?

    protected MarketplaceAddon(String uid, String type, String id, String label, String version, String maturity,
        boolean compatible, String contentType, String link, String documentationLink, String issuesLink,
        String author, boolean verifiedAuthor, boolean installed,
        String description, String detailedDescription, String configDescriptionURI, String keywords,
        List<String> countries, String license, String connection, String backgroundColor, String imageLink,
        Map<String, Object> properties, List<String> loggerPackages, @Nullable SortedMap<Version, AddonVersion> versions) {
        super(uid, type, id, label, version, maturity, compatible, contentType, link, documentationLink, issuesLink,
            author, verifiedAuthor, installed, description, detailedDescription, configDescriptionURI, keywords,
            countries, license, connection, backgroundColor, imageLink, properties, loggerPackages);
        this.versions = versions == null ? Collections.emptySortedMap() : Collections.unmodifiableSortedMap(versions);
    }

    public Addon toBasicAddon() {
        return Addon.create(this).build(); // TODO: (Nad) Modify what must be modified
    }

    public static MarketplaceAddon.Builder create(String uid) {
        return new MarketplaceAddon.Builder(uid);
    }

    public static class Builder extends Addon.Builder {

        @Nullable
        protected SortedMap<Version, AddonVersion> versions;

        protected Builder(String uid) {
            super(uid);
        }

        public Builder withAddonVersion(@NonNull AddonVersion addonVersion) {
            if (addonVersion.getVersion() == null) {
                throw new IllegalArgumentException("Version cannot be null");
            }
            SortedMap<Version, AddonVersion> locVersions = versions;
            if (locVersions == null) {
                locVersions = new TreeMap<>(); //TODO: (Nad) Comparator
            }
            locVersions.put(addonVersion.getVersion(), addonVersion);
            versions = locVersions;
            return this;
        }

        @Override
        public Addon build() {
            return new MarketplaceAddon(uid, type, id, label, version, maturity, compatible, contentType, link,
                documentationLink, issuesLink, author,
                verifiedAuthor, installed, description, detailedDescription, configDescriptionURI, keywords,
                countries, license, connection, backgroundColor, imageLink,
                properties.isEmpty() ? null : properties, loggerPackages, versions);
        }
    }
}
