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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
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

/**
 * The {@link AddonTest} contains tests for the {@link Addon} class
 *
 * @author  - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AddonTest {

    @Test
    public void testBasics() {
        assertThrows(IllegalArgumentException.class, () -> new Addon(null, null, null, null, null, null, null, false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon(" ", null, null, null, null, null, null, false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", null, null, null, null, null, null, false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", "\t", null, null, null, null, null, false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", "binding", null, null, null, null, null, false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Addon("test", "binding", "", null, null, null, null, false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null));

        Addon addon = new Addon("testuid", "binding", "testid", null, null, null, null, false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(addon.getDependsOn().isEmpty());
        assertTrue(addon.getProperties().isEmpty());
        assertTrue(addon.getVersions().isEmpty());

        addon = new Addon("testuid", "binding", "testid", null, null, null, Set.of("dep1", "dep2"), false, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(2, addon.getDependsOn().size());
        assertTrue(addon.getProperties().isEmpty());
        assertTrue(addon.getVersions().isEmpty());

        addon = new Addon("testuid", "binding", "testid", null, null, null, Set.of("dep1", "dep2"), false, null, null, null, null, null, false, false, null, null, null, null, List.of("DE", "PL", "UA") , null, null, null, null, null, null, null, null);
        assertEquals(2, addon.getDependsOn().size());
        assertEquals(3, addon.getCountries().size());
        assertTrue(addon.getProperties().isEmpty());
        assertTrue(addon.getVersions().isEmpty());

        addon = new Addon("testuid", "binding", "testid", null, null, null, Set.of("dep1", "dep2"), false, null, null, null, null, null, false, false, null, null, null, null, List.of("DE", "PL", "UA") , null, null, null, null, Map.of("key1", "value1", "key2", "value2") , null, null, null);
        assertEquals(2, addon.getDependsOn().size());
        assertEquals(3, addon.getCountries().size());
        assertEquals(2, addon.getProperties().size());
        assertTrue(addon.getVersions().isEmpty());

        addon = new Addon("testuid", "binding", "testid", null, null, null, Set.of("dep1", "dep2"), false, null, null, null, null, null, false, false, null, null, null, null, List.of("DE", "PL", "UA") , null, null, null, null, Map.of("key1", "value1", "key2", "value2") , List.of("com.example.addon"), null, null);
        assertEquals(2, addon.getDependsOn().size());
        assertEquals(3, addon.getCountries().size());
        assertEquals(2, addon.getProperties().size());
        assertEquals(1, addon.getLoggerPackages().size());
        assertTrue(addon.getVersions().isEmpty());

        Map<Version, AddonVersion> versions = new HashMap<>();
        versions.put(Version.valueOf("1.0.0"), AddonVersion.create().withVersion(Version.valueOf("1.0.0")).build());
        addon = new Addon("testuid", "binding", "testid", null, null, null, Set.of("dep1", "dep2"), false, null, null, null, null, null, false, false, null, null, null, null, List.of("DE", "PL", "UA") , null, null, null, null, Map.of("key1", "value1", "key2", "value2") , List.of("com.example.addon"), versions, null);
        assertEquals(2, addon.getDependsOn().size());
        assertEquals(3, addon.getCountries().size());
        assertEquals(2, addon.getProperties().size());
        assertEquals(1, addon.getLoggerPackages().size());
        assertEquals(1, addon.getVersions().size());

        versions.put(Version.valueOf("1.0.1"), AddonVersion.create().withVersion(Version.valueOf("1.0.1")).build());
        versions.put(Version.valueOf("1.0.2-beta"), AddonVersion.create().withVersion(Version.valueOf("1.0.2-beta")).build());
        addon = new Addon("testuid", "automation", "testid", "Test", Version.valueOf("0.9"), "stable",
                Set.of("ui-basic"), false, "application/x-test", "http://example.com", "http://doc.example.com",
                "https://issues.example.com", "Santa", true, false, "None", "Still none", null, "nothing, none",
                List.of("US"), "GPL", "none", "red", "http://image.exammple.com", null, List.of("com.example"),
                versions, Version.valueOf("1.0.0"));
        assertEquals(1, addon.getDependsOn().size());
        assertEquals(1, addon.getCountries().size());
        assertEquals(0, addon.getProperties().size());
        assertEquals(1, addon.getLoggerPackages().size());
        assertEquals(3, addon.getVersions().size());
        assertEquals("testuid", addon.getUid());
        assertEquals("automation", addon.getType());
        assertEquals("testid", addon.getId());
        assertEquals("Test", addon.getLabel());
        assertEquals(Version.valueOf("0.9"), addon.getVersion());
        assertEquals("stable", addon.getMaturity());
        assertSetsEquals(Set.of("ui-basic"), addon.getDependsOn());
        assertFalse(addon.getCompatible());
        assertEquals("application/x-test", addon.getContentType());
        assertEquals("http://example.com", addon.getLink());
        assertEquals("http://doc.example.com", addon.getDocumentationLink());
        assertEquals("https://issues.example.com", addon.getIssuesLink());
        assertEquals("Santa", addon.getAuthor());
        assertTrue(addon.isVerifiedAuthor());
        assertFalse(addon.isInstalled());
        assertEquals("None", addon.getDescription());
        assertEquals("Still none", addon.getDetailedDescription());
        assertMapsEquals(Map.of(), addon.getProperties());
        assertEquals("nothing, none", addon.getKeywords());
        assertIterableEquals(List.of("US"), addon.getCountries());
        assertEquals("GPL", addon.getLicense());
        assertEquals("none", addon.getConnection());
        assertEquals("red", addon.getBackgroundColor());
        assertEquals("http://image.exammple.com", addon.getImageLink());
        assertEquals("", addon.getConfigDescriptionURI());
        assertIterableEquals(List.of("com.example"), addon.getLoggerPackages());
        assertMapsEquals(versions, addon.getVersions());
        assertEquals(Version.valueOf("1.0.0"), addon.getCurrentVersion());
        assertEquals(Version.valueOf("1.0.1"), addon.getDefaultVersion());
        addon.setInstalled(true);
        assertTrue(addon.isInstalled());
    }

    @Test
    public void testBuilder() {
        Addon.Builder b = Addon.create("uid");
        assertThrows(IllegalArgumentException.class, () -> b.build());
        assertThrows(IllegalArgumentException.class, () -> b.withType("ui").build());
        assertEquals("ui", b.withId("id").build().getType());
        assertEquals("TLabel", b.withLabel("TLabel").build().getLabel());
        assertEquals(Version.EMPTY_VERSION, b.withVersion(new Version(0, 0, 0)).build().getVersion());
        assertEquals("beta", b.withMaturity("beta").build().getMaturity());
        assertSetsEquals(Set.of("ui-basic", "astro", "js"), b.withDependsOn(Set.of("ui-basic", "astro", "js")).build().getDependsOn());
        assertSetsEquals(Set.of("ui-basic", "astro", "js"), b.getDependsOn());
        assertTrue(b.withCompatible(true).build().getCompatible());
        assertEquals("img/gif", b.withContentType("img/gif").build().getContentType());
        assertEquals("http://link.example.com", b.withLink("http://link.example.com").build().getLink());
        assertEquals("http://docs.example.com", b.withDocumentationLink("http://docs.example.com").build().getDocumentationLink());
        assertEquals("http://tracker.example.com", b.withIssuesLink("http://tracker.example.com").build().getIssuesLink());
        assertEquals("Nadar", b.withAuthor("Nadar").build().getAuthor());
        assertTrue(b.withInstalled(true).build().isInstalled());
        assertEquals("Nadar", b.withAuthor("Nadar", true).build().getAuthor());
        assertTrue(b.build().isVerifiedAuthor());
        assertEquals("Description", b.withDescription("Description").build().getDescription());
        assertEquals("Detailed description", b.withDetailedDescription("Detailed description").build().getDetailedDescription());
        assertEquals("", b.withConfigDescriptionURI(null).build().getConfigDescriptionURI());
        assertEquals("smart, light", b.withKeywords("smart, light").build().getKeywords());
        assertTrue(b.withCountries(null).build().getCountries().isEmpty());
        assertNull(b.getCountries());
        assertEquals("EPL", b.withLicense("EPL").build().getLicense());
        assertEquals("local", b.withConnection("local").build().getConnection());
        assertEquals("green", b.withBackgroundColor("green").build().getBackgroundColor());
        assertEquals("http://image.example.com", b.withImageLink("http://image.example.com").build().getImageLink());
        assertMapsEquals(Map.of("priority", Double.valueOf(2d)), b.withProperty("priority", Double.valueOf(2d)).build().getProperties());
        b.withProperties(Map.of("link", "http://example.com", "fresh", Boolean.FALSE));
        assertThat(b.build().getProperties(), hasEntry("link", "http://example.com"));
        assertThat(b.build().getProperties(), hasEntry("fresh", Boolean.FALSE));
        assertIterableEquals(List.of("com.example.basic", "com.example.advanced"), b.withLoggerPackages(List.of("com.example.basic", "com.example.advanced")).build().getLoggerPackages());
        assertIterableEquals(List.of("com.example.basic", "com.example.advanced"), b.getLoggerPackages());
        AddonVersion av1 = AddonVersion.create().withVersion(Version.valueOf("0.0.8")).withCompatible(false).build();
        AddonVersion av2 = AddonVersion.create().withVersion(Version.valueOf("0.0.9.alpha")).withCompatible(true).build();
        AddonVersion av3 = AddonVersion.create().withVersion(Version.valueOf("0.0.9.beta")).withCompatible(true).withMaturity("stable").build();
        assertEquals(3, b.withAddonVersion(av1).withAddonVersion(av2).withAddonVersion(av3).build().getVersions().size());
        assertThat(b.build().getVersions(), hasEntry(av1.getVersion(), av1));
        assertThat(b.build().getVersions(), hasEntry(av2.getVersion(), av2));
        assertThat(b.build().getVersions(), hasEntry(av3.getVersion(), av3));
        AddonVersion av4 = AddonVersion.create().build();
        assertThat(b.withAddonVersion(av4).build().getVersions(), hasEntry(av4.getVersion(), av4));
        assertThat(b.getVersions(), hasEntry(av1.getVersion(), av1));
        assertThat(b.getVersions(), hasEntry(av2.getVersion(), av2));
        assertThat(b.getVersions(), hasEntry(av3.getVersion(), av3));
        assertThat(b.getVersions(), hasEntry(av4.getVersion(), av4));
        assertEquals(Version.valueOf("0.0.9.alpha"), b.withCurrentVersion(Version.valueOf("0.0.9.alpha")).build().getCurrentVersion());
        assertEquals(Version.valueOf("0.0.9.beta"), b.build().getDefaultVersion());

        Addon addon = b.build();
        Addon addon2 = Addon.create(addon).withCompatible(false).build();
        assertTrue(addon.getCompatible());
        assertFalse(addon2.getCompatible());

        assertEquals(addon.getType(), addon2.getType());
        assertEquals(addon.getLabel(), addon2.getLabel());
        assertEquals(addon.getVersion(), addon2.getVersion());
        assertEquals(addon.getMaturity(), addon2.getMaturity());
        assertSetsEquals(addon.getDependsOn(), addon2.getDependsOn());
        assertEquals(addon.getContentType(), addon2.getContentType());
        assertEquals(addon.getLink(), addon2.getLink());
        assertEquals(addon.getDocumentationLink(), addon2.getDocumentationLink());
        assertEquals(addon.getIssuesLink(), addon2.getIssuesLink());
        assertEquals(addon.getAuthor(), addon2.getAuthor());
        assertEquals(addon.isVerifiedAuthor(), addon2.isVerifiedAuthor());
        assertEquals(addon.isInstalled(), addon2.isInstalled());
        assertEquals(addon.getDescription(), addon2.getDescription());
        assertEquals(addon.getDetailedDescription(), addon2.getDetailedDescription());
        assertEquals(addon.getConfigDescriptionURI(), addon2.getConfigDescriptionURI());
        assertEquals(addon.getKeywords(), addon2.getKeywords());
        assertIterableEquals(addon.getCountries(), addon2.getCountries());
        assertEquals(addon.getLicense(), addon2.getLicense());
        assertEquals(addon.getConnection(), addon2.getConnection());
        assertEquals(addon.getBackgroundColor(), addon2.getBackgroundColor());
        assertEquals(addon.getImageLink(), addon2.getImageLink());
        assertMapsEquals(addon.getProperties(), addon2.getProperties());
        assertIterableEquals(addon.getLoggerPackages(), addon2.getLoggerPackages());
        assertMapsEquals(addon.getVersions(), addon2.getVersions());
        assertEquals(addon.getCurrentVersion(), addon2.getCurrentVersion());
        assertEquals(addon.getDefaultVersion(), addon2.getDefaultVersion());
    }

    private static Stream<Arguments> provideDefaultVersionArguments() {
        AddonVersion va1 = AddonVersion.create().withDescription("va1").withVersion(Version.valueOf("2.3.0")).build();
        AddonVersion va2 = AddonVersion.create().withDescription("va2").withVersion(Version.valueOf("2.3.3.alpha")).build();
        AddonVersion va3 = AddonVersion.create().withDescription("va3").withVersion(Version.valueOf("2.3.3")).withMaturity("beta").build();
        AddonVersion va4 = AddonVersion.create().withDescription("va4").withVersion(Version.valueOf("2.3.3-SNAPSHOT")).withMaturity("mature").build();
        AddonVersion va5 = AddonVersion.create().withDescription("va5").withVersion(Version.valueOf("2.3.3-SNAPSHOT")).build();
        AddonVersion va6 = AddonVersion.create().withDescription("va6").build();
        AddonVersion va7 = AddonVersion.create().withDescription("va7").withCompatible(true).build();
        AddonVersion va8 = AddonVersion.create().withDescription("va8").withVersion(Version.valueOf("1.8.0.RC1")).withCompatible(true).build();
        AddonVersion va9 = AddonVersion.create().withDescription("va9").withVersion(Version.valueOf("1.5.0")).withCompatible(true).build();
        AddonVersion va10 = AddonVersion.create().withDescription("va10").withVersion(Version.valueOf("5.0.0")).withCompatible(false).withMaturity("mature").build();
        AddonVersion va11 = AddonVersion.create().withDescription("va11").withVersion(Version.valueOf("1.5.1")).withCompatible(false).build();
        return Stream.of(
                Arguments.of(List.of(), null),
                Arguments.of(List.of(va1), Version.valueOf("2.3.0")),
                Arguments.of(List.of(va1, va2, va3), Version.valueOf("2.3.0")),
                Arguments.of(List.of(va1, va2, va3, va4), Version.valueOf("2.3.3-SNAPSHOT")),
                Arguments.of(List.of(va1, va2, va3, va5), Version.valueOf("2.3.0")),
                Arguments.of(List.of(va1, va2, va3, va5, va6), Version.valueOf("2.3.0")),
                Arguments.of(List.of(va1, va2, va3, va4, va5, va6, va7), Version.EMPTY_VERSION),
                Arguments.of(List.of(va1, va2, va3, va4, va5, va6, va7, va8), Version.valueOf("1.8.0.RC1")),
                Arguments.of(List.of(va1, va2, va3, va4, va5, va6, va7, va8, va9), Version.valueOf("1.5.0")),
                Arguments.of(List.of(va1, va2, va3, va4, va5, va6, va7, va8, va9, va10), Version.valueOf("1.5.0")),
                Arguments.of(List.of(va1, va2, va3, va4, va5, va6, va7, va8, va9, va10, va11), Version.valueOf("1.5.0"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideDefaultVersionArguments")
    public void testDefaultVersion(Collection<AddonVersion> versions, @Nullable Version defaultVersion) {
        Addon.Builder b = Addon.create("testUid").withType("misc").withId("testId");
        for (AddonVersion addonVersion : versions) {
            b.withAddonVersion(addonVersion);
        }
        assertEquals(defaultVersion, b.build().getDefaultVersion());
    }

    // TODO: (Nad) Test mergeVersion
    private void assertSetsEquals(@Nullable Set<?> a, @Nullable Set<?> b) {
        if (a == null || b == null) {
            assertTrue(a == null && b == null);
            return;
        }
        assertEquals(a.size(), b.size());
        if (a instanceof SortedSet && b instanceof SortedSet) {
            Iterator<?> iterator = b.iterator();
            for (Object o : a) {
                assertEquals(o, iterator.next());
            }
        } else {
            for (Object o : a) {
                assertTrue(b.contains(o));
            }
        }
    }

    private void assertMapsEquals(@Nullable Map<?, ?> a, @Nullable Map<?, ?> b) {
        if (a == null || b == null) {
            assertTrue(a == null && b == null);
            return;
        }
        assertEquals(a.size(), b.size());
        if (a instanceof SortedMap && b instanceof SortedMap) {
            Iterator<?> iterator = b.entrySet().iterator();
            Object o;
            for (Entry<?, ?> entry : a.entrySet()) {
                o = iterator.next();
                assertEquals(entry.getKey(), ((Entry<?, ?>) o).getKey());
                assertEquals(entry.getValue(), ((Entry<?, ?>) o).getValue());
            }
        } else {
            for (Entry<?, ?> entry : a.entrySet()) {
                assertEquals(entry.getValue(), b.get(entry.getKey()));
            }
        }
    }
}
