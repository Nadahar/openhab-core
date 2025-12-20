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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonVersion;
import org.openhab.core.addon.Version;
import org.openhab.core.addon.VersionRange;

/**
 * A DTO representing an {@link AddonVersion}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class AddonVersionDTO {

    public String version;
    public String coreRange;
    public String maturity;
    public boolean stable;
    public @Nullable Set<@NonNull String> dependsOn;
    public boolean compatible;
    public String documentationLink;
    public String issuesLink;
    public String description;
    public String keywords;
    public @Nullable List<@NonNull String> countries;
    public Map<@NonNull String, @NonNull Object> properties;
    public List<@NonNull String> loggerPackages;

    /**
     * Creates a new {@link AddonVersion} from this {@link AddonVersionDTO}.
     *
     * @return The new {@link AddonVersion}.
     */
    public @NonNull AddonVersion toAddonVersion() { //TODO: (Nad) Needed?
        AddonVersion.Builder b = AddonVersion.create();
        if (this.version != null) {
            b.withVersion(Version.valueOf(this.version));
        }
        if (this.coreRange != null) {
            b.withCoreRange(VersionRange.valueOf(this.coreRange));
        }
        if (this.maturity != null) {
            b.withMaturity(this.maturity);
        }
        if (this.dependsOn != null) {
            b.withDependsOn(this.dependsOn);
        }
        b.withCompatible(this.compatible);
        if (this.documentationLink != null) {
            b.withDocumentationLink(this.documentationLink);
        }
        if (this.issuesLink != null) {
            b.withIssuesLink(this.issuesLink);
        }
        if (this.description != null) {
            b.withDescription(this.description);
        }
        if (this.keywords != null) {
            b.withKeywords(this.keywords);
        }
        if (this.countries != null) {
            b.withCountries(this.countries);
        }
        if (this.properties != null) {
            b.withProperties(this.properties);
        }
        if (this.loggerPackages != null) {
            b.withLoggerPackages(this.loggerPackages);
        }

        return b.build();
    }

    /**
     * Creates a new {@link AddonVersionDTO} from the specified {@link AddonVersion}.
     *
     * @param addonVersion the {@link AddonVersion}.
     * @return The new {@link AddonVersionDTO}.
     */
    public static @NonNull AddonVersionDTO fromAddonVersion(@NonNull AddonVersion addonVersion) {
        AddonVersionDTO result = new AddonVersionDTO();

        result.version = addonVersion.getVersion().toString();
        VersionRange vr = addonVersion.getCoreRange();
        if (vr != null) {
            result.coreRange = vr.toString();
        }
        result.maturity = addonVersion.getMaturity();
        result.stable = addonVersion.isStable();
        Set<@NonNull String> stringSet = addonVersion.getDependsOn();
        if (!stringSet.isEmpty()) {
            result.dependsOn = stringSet;
        }
        result.compatible = addonVersion.isCompatible();
        result.documentationLink = addonVersion.getDocumentationLink();
        result.issuesLink = addonVersion.getIssuesLink();
        result.description = addonVersion.getDescription();
        result.keywords = addonVersion.getKeywords();
        List<@NonNull String> stringList = addonVersion.getCountries();
        if (!stringList.isEmpty()) {
            result.countries = stringList;
        }
        Map<@NonNull String, @NonNull Object> map = addonVersion.getProperties();
        if (!map.isEmpty()) {
            result.properties = map;
        }
        stringList = addonVersion.getLoggerPackages();
        if (!stringList.isEmpty()) {
            result.loggerPackages = stringList;
        }

        return result;
    }
}
