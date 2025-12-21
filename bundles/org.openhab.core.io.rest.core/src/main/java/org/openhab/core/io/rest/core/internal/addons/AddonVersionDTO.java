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
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonVersion;
import org.openhab.core.addon.Version;
import org.openhab.core.addon.VersionRange;

/**
 * A DTO representing an {@link AddonVersion} in the REST API.
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

        return result;
    }
}
