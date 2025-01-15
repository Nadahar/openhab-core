package org.openhab.core.addon.marketplace;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.rest.core.Exclude;
import org.osgi.framework.VersionRange;

import com.google.gson.annotations.SerializedName;


@NonNullByDefault
public class AddonVersion {

    protected final /*@Exclude*/ String uid; //TODO: (Nad) Excluded exclude
    protected final @Nullable @Exclude Version version;
    @SerializedName("version")
    protected final @Nullable String versionString;
    protected final @Nullable @Exclude VersionRange coreRange;
    @SerializedName("coreRange")
    protected final @Nullable String coreRangeString;
    protected final @Nullable /*@Exclude*/ String maturity;
    protected final boolean compatible;
    protected final @Nullable /*@Exclude*/ String documentationLink;
    protected final @Nullable /*@Exclude*/ String issuesLink;
    protected final boolean installed;
    protected final @Nullable /*@Exclude*/ String description;
    protected final @Nullable /*@Exclude*/ String keywords;
    protected final /*@Exclude*/ List<String> countries;
    protected final /*@Exclude*/ Map<String, Object> properties;
    protected final /*@Exclude*/ List<String> loggerPackages;

    protected AddonVersion(String uid, @Nullable Version version, @Nullable VersionRange coreRange,
        @Nullable String maturity, boolean compatible, @Nullable String documentationLink,
        @Nullable String issuesLink, boolean installed, @Nullable String description, @Nullable String keywords,
        @Nullable List<String> countries, @Nullable Map<String, Object> properties,
        @Nullable List<String> loggerPackages) {
        if (uid.isBlank()) {
            throw new IllegalArgumentException("uid cannot be blank");
        }
        this.uid = uid;
        this.version = version;
        this.versionString = version == null ? null : version.toString();
        this.coreRange = coreRange;
        this.coreRangeString = coreRange == null ? null : coreRange.toString();
        this.maturity = maturity;
        this.compatible = compatible;
        this.documentationLink = documentationLink;
        this.issuesLink = issuesLink;
        this.installed = installed;
        this.description = description;
        this.keywords = keywords;
        this.countries = countries == null ? List.of() : List.copyOf(countries);
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
        this.loggerPackages = loggerPackages == null ? List.of() : List.copyOf(loggerPackages);
    }


    public String getUid() {
        return uid;
    }

    public @Nullable Version getVersion() {
        return version;
    }

    public @Nullable VersionRange getCoreRange() {
        return coreRange;
    }

    public @Nullable String getMaturity() {
        return maturity;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public @Nullable String getDocumentationLink() {
        return documentationLink;
    }

    public @Nullable String getIssuesLink() {
        return issuesLink;
    }

    public boolean isInstalled() {
        return installed;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @Nullable String getKeywords() {
        return keywords;
    }

    public List<String> getCountries() {
        return countries;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public List<String> getLoggerPackages() {
        return loggerPackages;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        protected @Nullable String uid;
        protected @Nullable Version version;
        protected @Nullable VersionRange coreRange;
        protected @Nullable String maturity;
        protected boolean compatible;
        protected @Nullable String documentationLink;
        protected @Nullable String issuesLink;
        protected boolean installed;
        protected @Nullable String description;
        protected @Nullable String keywords;
        protected @Nullable List<String> countries;
        protected @Nullable Map<String, Object> properties;
        protected @Nullable List<String> loggerPackages;

        public Builder withUID(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder withVersion(@Nullable Version version) {
            this.version = version;
            return this;
        }

        public Builder withCoreRange(@Nullable VersionRange coreRange) {
            this.coreRange = coreRange;
            return this;
        }

        public Builder withMaturity(@Nullable String maturity) {
            this.maturity = maturity;
            return this;
        }

        public Builder withCompatible(boolean compatible) {
            this.compatible = compatible;
            return this;
        }

        public Builder withDocumentationLink(@Nullable String documentationLink) {
            this.documentationLink = documentationLink;
            return this;
        }

        public Builder withIssuesLink(@Nullable String issuesLink) {
            this.issuesLink = issuesLink;
            return this;
        }

        public Builder withInstalled(boolean installed) {
            this.installed = installed;
            return this;
        }

        public Builder withDescription(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder withKeywords(@Nullable String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder withCountries(@Nullable List<String> countries) {
            this.countries = countries;
            return this;
        }

        public Builder withProperties(@Nullable Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public Builder withLoggerPackages(@Nullable List<String> loggerPackages) {
            this.loggerPackages = loggerPackages;
            return this;
        }

        public boolean isValid() {
            if (uid == null || uid.isBlank() || version == null) {
                return false;
            }
            //TODO: (Nad) Check valid resource
            return true;
        }

        public AddonVersion build() {
            String localUID = uid;
            if (localUID == null) {
                throw new IllegalArgumentException("uid cannot be null");
            }
            return new AddonVersion(localUID, version, coreRange, maturity, compatible, documentationLink, issuesLink,
                installed, description, keywords, countries, properties, loggerPackages);
        }
    }
}
