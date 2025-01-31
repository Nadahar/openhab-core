package org.openhab.core.addon;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;


@NonNullByDefault
public class AddonVersion {

    protected final Version version; //TODO: (Nad) Excluded exclude
    protected final @Nullable @Exclude VersionRange coreRangeObj;
    @SerializedName("coreRange")
    protected final @Nullable String coreRangeString;
    protected final @Nullable /*@Exclude*/ String maturity;
    protected final boolean stable;
    protected final Set<String> dependsOn;
    protected final boolean compatible;
    protected final @Nullable  String documentationLink;
    protected final @Nullable /*@Exclude*/ String issuesLink;
    protected final @Nullable /*@Exclude*/ String description;
    protected final @Nullable /*@Exclude*/ String keywords;
    protected final /*@Exclude*/ List<String> countries;
    protected final /*@Exclude*/ Map<String, Object> properties;
    protected final /*@Exclude*/ List<String> loggerPackages;

    protected AddonVersion(Version version, @Nullable VersionRange coreRange,
        @Nullable String maturity, @Nullable Set<String> dependsOn, boolean compatible, @Nullable String documentationLink,
        @Nullable String issuesLink, @Nullable String description, @Nullable String keywords,
        @Nullable List<String> countries, @Nullable Map<String, Object> properties,
        @Nullable List<String> loggerPackages) {
        this.version = version;
        this.coreRangeObj = coreRange;
        this.coreRangeString = coreRange == null ? null : coreRange.toString();
        this.maturity = maturity;
        this.stable = resoleStable(version, maturity);
        this.dependsOn = dependsOn == null ? Set.of() : Set.copyOf(dependsOn);
        this.compatible = compatible;
        this.documentationLink = documentationLink;
        this.issuesLink = issuesLink;
        this.description = description;
        this.keywords = keywords;
        this.countries = countries == null ? List.of() : List.copyOf(countries);
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
        this.loggerPackages = loggerPackages == null ? List.of() : List.copyOf(loggerPackages);
    }

    public Version getVersion() {
        return version;
    }

    public @Nullable VersionRange getCoreRange() {
        return coreRangeObj;
    }

    public @Nullable String getMaturity() {
        return maturity;
    }

    public boolean isStable() {
        return stable;
    }

    public Set<String> getDependsOn() {
        return dependsOn;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" [").append("version=").append(version).append(", ");
        if (coreRangeString != null) {
            sb.append("coreRange=").append(coreRangeString).append(", ");
        }
        if (maturity != null) {
            sb.append("maturity=").append(maturity).append(", ");
        }
        sb.append("stable=").append(stable).append(", ").append("dependsOn=").append(dependsOn).append(", ")
                .append("compatible=").append(compatible).append(", ");
        if (documentationLink != null) {
            sb.append("documentationLink=").append(documentationLink).append(", ");
        }
        if (issuesLink != null) {
            sb.append("issuesLink=").append(issuesLink).append(", ");
        }
        if (description != null) {
            sb.append("description=").append(description).append(", ");
        }
        if (keywords != null) {
            sb.append("keywords=").append(keywords).append(", ");
        }
        sb.append("countries=").append(countries).append(", ").append("properties=").append(properties).append(", ")
                .append("loggerPackages=").append(loggerPackages).append("]");
        return sb.toString();
    }

    protected boolean resoleStable(@Nullable Version version, @Nullable String maturity) {
        if (version == null || Version.EMPTY_VERSION.equals(version)) {
            return false;
        }

        if (maturity != null && Addon.CODE_MATURITY_LEVELS.contains(maturity)) {
            return "stable".equals(maturity) || "mature".equals(maturity);
        }

        // Deem versions without a qualifier as stable
        return version.getQualifier().isBlank();
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        protected @Nullable Version version;
        protected @Nullable VersionRange coreRange;
        protected @Nullable String maturity;
        protected @Nullable Set<String> dependsOn;
        protected boolean compatible;
        protected @Nullable String documentationLink;
        protected @Nullable String issuesLink;
        protected @Nullable String description;
        protected @Nullable String keywords;
        protected @Nullable List<String> countries;
        protected @Nullable Map<String, Object> properties;
        protected @Nullable List<String> loggerPackages;

        public Builder withVersion(@Nullable Version version) {
            this.version = version;
            return this;
        }

        public Builder withCoreRange(@Nullable VersionRange coreRange) {
            this.coreRange = coreRange;
            return this;
        }

        public Builder withMaturity(@Nullable String maturity) {
            this.maturity = maturity == null ? null : maturity.toLowerCase(Locale.ROOT);
            return this;
        }

        public Builder withDependsOn(@Nullable Set<String> dependsOn) {
            this.dependsOn = dependsOn;
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

        public boolean isValid(@Nullable Set<String> validResourceTypes) {
            if (version == null) {
                return false;
            }
            Map<String, Object> p;
            if (validResourceTypes != null) {
                if ((p = properties) == null) {
                    return false;
                }
                return p.keySet().stream().anyMatch(prop -> validResourceTypes.contains(prop));
            }
            return true;
        }

        public AddonVersion build() {
            Version v = version;
            if (v == null) {
                v = Version.EMPTY_VERSION;
            }
            return new AddonVersion(v, coreRange, maturity, dependsOn, compatible, documentationLink, issuesLink,
                    description, keywords, countries, properties, loggerPackages);
        }
    }
}
