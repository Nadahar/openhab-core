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
package org.openhab.core.addon.marketplace;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.osgi.framework.VersionRange;

/**
 * The {@link VersionTest} contains tests for the {@link BundleVersion} class
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VersionTest {

    private static Stream<Arguments> provideCompareVersionsArguments() {
        return Stream.of( //
                Arguments.of(null, null, Result.EQUAL), // same versions are equal
                Arguments.of(null, "4.3.0", Result.OLDER), // null is older than everything
                Arguments.of("3.1.0", "3.1.0", Result.EQUAL), // same versions are equal
                Arguments.of("3.1.0", "3.0.2", Result.NEWER), // minor version is more important than micro
                Arguments.of("3.7.0", "4.0.1.202105311711", Result.OLDER), // major version is more important than minor
                Arguments.of("3.9.1.M1", "3.9.0.M5", Result.NEWER), // micro version is more important than qualifier
                Arguments.of("3.0.0.202105311032", "3.0.0.202106011144", Result.OLDER), // snapshots
                Arguments.of("3.1.0.M3", "3.1.0.M1", Result.NEWER), // milestones are compared numerically
                Arguments.of("3.1.0.M1", "3.1.0.197705310021", Result.OLDER), // snapshot is newer than milestone
                Arguments.of("3.3.0", "3.3.0.202206302115", Result.NEWER), // release is newer than snapshot
                Arguments.of("3.3.0", "3.3.0.RC1", Result.NEWER), // releases are newer than release candidates
                Arguments.of("3.3.0.M5", "3.3.0.RC1", Result.OLDER), // milestones are older than release candidates
                Arguments.of("3.3.0.RC2", "3.3.0.202305201715", Result.OLDER), // snapshots are newer than release candidates
                Arguments.of("3.3.0-SNAPSHOT", "3.3.0.202305201715", Result.NEWER),
                Arguments.of("3.3.0-RC2", "3.3.0.RC11", Result.OLDER),
                Arguments.of("3.3.0-RC4", "3.3.0.M11", Result.NEWER),
                Arguments.of("5.0.0-SNAPSHOT", "5.0.0", Result.OLDER),
                Arguments.of("5.0.0-SNAPSHOT", "5.0.0-alpha", Result.NEWER),
                Arguments.of("5.0.0-alpha", "5.0.0-beta", Result.OLDER),
                Arguments.of("5.0.0_snapshot", "5.0.0-SNAPSHOT", Result.NEWER), // remain consistent with equals()
                Arguments.of("5.0.0.M2", "5.0.0-SNAPSHOT", Result.OLDER),
                Arguments.of("5", "5.0.0", Result.EQUAL),
                Arguments.of("5.0.0", "5.0", Result.EQUAL),
                Arguments.of("3.3.0.202305201715", "3.3.0.alpha", Result.NEWER),
                Arguments.of("5.0.0_202501132145", "5.0.0.snapshot", Result.OLDER)
        );
    }

    @ParameterizedTest
    @MethodSource("provideCompareVersionsArguments")
    public void testCompareVersions(String v1, String v2, Result result) {
        Version version1 = Version.parseVersion(v1);
        Version version2 = Version.parseVersion(v2);
        switch (result) {
            case OLDER:
                assertThat(version1.compareTo(version2), lessThan(0));
                break;
            case NEWER:
                assertThat(version1.compareTo(version2), greaterThan(0));
                break;
            case EQUAL:
                assertThat(version1.compareTo(version2), is(0));
                break;
        }
    }

    @Test
    public void testOSGiVersionEquality() {
        org.osgi.framework.Version osgiVersion = org.osgi.framework.Version.parseVersion("5.0.0.RC3");
        Version version = Version.valueOf(osgiVersion);
        assertTrue(osgiVersion.equals(version));
        assertTrue(version.equals(osgiVersion));

        osgiVersion = org.osgi.framework.Version.emptyVersion;
        version = Version.parseVersion(null);
        assertTrue(osgiVersion.equals(version));
        assertTrue(version.equals(osgiVersion));

        version = Version.parseVersion("");
        assertTrue(osgiVersion.equals(version));
        assertTrue(version.equals(osgiVersion));
    }

    @Test
    public void testIllegalRangeThrowsException() {
        BundleVersion bundleVersion = new BundleVersion("3.1.0");
        assertThrows(IllegalArgumentException.class, () -> bundleVersion.inRange("illegal"));
    }

    private static Stream<Arguments> provideInRangeArguments() {
        return Stream.of(Arguments.of("3.2.0", "[3.1.0;3.2.1)", true), // in range
                Arguments.of("3.2.0", "[3.1.0;3.2.0)", false), // at end of range, non-inclusive
                Arguments.of("3.2.0", "[3.1.0;3.2.0]", true), // at end of range, inclusive
                Arguments.of("3.2.0", "[3.1.0;3.1.5)", false), // above range
                Arguments.of("3.2.0", "[3.3.0;3.4.0)", false), // below range
                Arguments.of("3.2.0", "", true), // empty range assumes in range
                Arguments.of("3.2.0", null, true)//,
//                Arguments.of("5.0.0.RC1", "[3.3.0;5.0.0)", false)

                );
    }

    @ParameterizedTest
    @MethodSource("provideInRangeArguments")
    public void inRangeTest(String version, @Nullable String range, boolean result) {
        BundleVersion frameworkVersion = new BundleVersion(version);
        assertThat(frameworkVersion.inRange(range), is(result));
    }

    private static Stream<Arguments> provideInRangeOSGiArguments() {
        return Stream.of(Arguments.of("3.2.0", "[3.1.0,3.2.1)", true), // in range
                Arguments.of("3.2.0", "[3.1.0,3.2.0)", false), // at end of range, non-inclusive
                Arguments.of("3.2.0", "[3.1.0,3.2.0]", true), // at end of range, inclusive
                Arguments.of("3.2.0", "[3.1.0,3.1.5)", false), // above range
                Arguments.of("3.2.0", "[3.3.0,3.4.0)", false), // below range
//                Arguments.of("3.2.0", "", true), // empty range assumes in range
//                Arguments.of("3.2.0", null, true),
                Arguments.of("5.0.0.RC1", "[3.3.0,5.0.0)", false)

                );
    }

    @ParameterizedTest
    @MethodSource("provideInRangeOSGiArguments")
    public void inRangeOSGiTest(String version, @Nullable String range, boolean result) {
        Version frameworkVersion = Version.parseVersion(version);
        VersionRange oRange = VersionRange.valueOf(range);
        assertThat(oRange.includes(frameworkVersion), is(result));
    }

    private enum Result {
        OLDER,
        NEWER,
        EQUAL
    }
}
