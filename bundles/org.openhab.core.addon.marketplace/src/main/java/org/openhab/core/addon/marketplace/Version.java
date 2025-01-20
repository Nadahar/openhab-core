package org.openhab.core.addon.marketplace;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

// Immutable
@NonNullByDefault
public class Version implements Comparable<Version> {

    public static final Pattern VERSION_PATTERN = Pattern.compile("^\\s*(?<major>\\d+)(?:\\s*\\.\\s*(?<minor>\\d+)(?:\\s*\\.\\s*(?<micro>\\d+)(?:\\s*(?<lastSeparator>\\.|-|_)\\s*(?<qualifier>[a-zA-Z0-9_-]+))?)?)?\\s*$");
    public static final Pattern QUALIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    protected static final Pattern RC_PATTERN = Pattern.compile("(?i)rc(\\d+)");
    protected static final Pattern MILESTONE_PATTERN = Pattern.compile("(?i)m(\\d+)");
    protected static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(?i)snapshot");

    protected final int major;
    protected final int minor;
    protected final int micro;
    protected final String qualifier;
    protected final char lastSeparator;
    protected static final char SEPARATOR = '.';
    private transient @Nullable String versionString;
    private transient int hash;

    /**
     * The empty version "0.0.0".
     */
    public static final Version EMPTY_VERSION = new Version(0, 0, 0);

    public Version(String version) {
        String s;
        Matcher m = VERSION_PATTERN.matcher(version);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid version format \"" + version + '"');
        }

        major = Integer.parseInt(m.group("major"));
        minor = (s = m.group("minor")) == null ? 0 : Integer.parseInt(s);
        micro = (s = m.group("micro")) == null ? 0 : Integer.parseInt(s);
        lastSeparator = (s = m.group("lastSeparator")) == null ? '.' : s.charAt(0);
        qualifier = (s = m.group("qualifier")) == null ? "" : s;
    }

    public Version(int major, int minor, int micro) {
        this(major, minor, micro, null);
    }

    public Version(int major, int minor, int micro, @Nullable String qualifier) {
        this(major, minor, micro, '.', qualifier);
    }

    public Version(int major, int minor, int micro, char lastSeparator, @Nullable String qualifier) {
        if (major < 0) {
            throw new IllegalArgumentException("Major version cannot be negative: " + major);
        }
        if (minor < 0) {
            throw new IllegalArgumentException("Minor version cannot be negative:" + minor);
        }
        if (micro < 0) {
            throw new IllegalArgumentException("Micro version cannot be negative:" + micro);
        }
        if (lastSeparator != '.' && lastSeparator != '-' && lastSeparator != '_') {
            throw new IllegalArgumentException("Invalid last separator: \"" + lastSeparator + '"');
        }
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.lastSeparator = lastSeparator;
        if (qualifier == null || qualifier.isEmpty()) {
            this.qualifier = "";
        } else {
            if (!QUALIFIER_PATTERN.matcher(qualifier).find()) {
                throw new IllegalArgumentException("Invalid qualifier: \"" + qualifier + '"');
            }
            this.qualifier = qualifier;
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    public char getLastSeparator() {
        return lastSeparator;
    }

    public String getQualifier() {
        return qualifier;
    }

    public org.osgi.framework.Version toOSGiVersion() {
        return new org.osgi.framework.Version(major, minor, micro, qualifier);
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h != 0) {
            return h;
        }
        return hash = Objects.hash(major, micro, minor, lastSeparator, qualifier);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        Version other = (Version) obj;
        return lastSeparator == other.lastSeparator && major == other.major && micro == other.micro
            && minor == other.minor && Objects.equals(qualifier, other.qualifier);
    }

    @Override
    public String toString() {
        String s = versionString;
        if (s != null) {
            return s;
        }
        int qLen = qualifier.length();
        StringBuilder sb = new StringBuilder(20 + qLen);
        sb.append(major).append(SEPARATOR).append(minor).append(SEPARATOR).append(micro);
        if (qLen > 0) {
            sb.append(lastSeparator).append(qualifier);
        }
        return versionString = sb.toString();
    }

    // Doc: inconsistent with equals for 'lastSeparator'
    @Override
    public int compareTo(Version other) {
        if (other == this) {
            return 0;
        }

        int result = major - other.major;
        if (result != 0) {
            return result;
        }

        result = minor - other.minor;
        if (result != 0) {
            return result;
        }

        result = micro - other.micro;
        if (result != 0) {
            return result;
        }

        if (qualifier.equals(other.qualifier)) {
            // Includes if both are empty
            return 0;
        }
        if (qualifier.isEmpty()) {
            // Release is newer
            return 1;
        }
        if (other.qualifier.isEmpty()) {
            // Release is newer
            return -1;
        }

        Matcher rcMatcher = RC_PATTERN.matcher(qualifier);
        Matcher orcMatcher = RC_PATTERN.matcher(other.qualifier);
        Matcher msMatcher = MILESTONE_PATTERN.matcher(qualifier);
        Matcher omsMatcher = MILESTONE_PATTERN.matcher(other.qualifier);
        boolean rc = rcMatcher.matches();
        boolean orc = orcMatcher.matches();
        boolean ms = msMatcher.matches();
        boolean oms = omsMatcher.matches();
        if (rc && orc) {
            // Both are release candidates
            int n = Integer.valueOf(rcMatcher.group(1));
            int on = Integer.valueOf(orcMatcher.group(1));
            return Integer.compare(n, on);
        }
        if (ms && oms) {
            // Both are milestones
            int n = Integer.valueOf(msMatcher.group(1));
            int on = Integer.valueOf(omsMatcher.group(1));
            return Integer.compare(n, on);
        }

        if (rc && oms) {
            // Release candidate is newer than milestone
            return 1;
        }
        if (ms && orc) {
            // Milestone is older than release candidate
            return -1;
        }

        long ql, oql;
        try {
            ql = Long.parseLong(qualifier);
        } catch (NumberFormatException e) {
            ql = Long.MIN_VALUE;
        }
        try {
            oql = Long.parseLong(other.qualifier);
        } catch (NumberFormatException e) {
            oql = Long.MIN_VALUE;
        }

        if (ql >= 0 && oql >= 0) {
            // Both are positive integers, compare numerically
            return Long.compare(ql, oql);
        }

        boolean ss = SNAPSHOT_PATTERN.matcher(qualifier).matches();
        boolean oss = SNAPSHOT_PATTERN.matcher(other.qualifier).matches();
        if (ql < 0 && oql < 0) {
            // Both aren't positive integers, snapshots are newer, otherwise do a simple string comparison
            if (ss) {
                // Snapshots are newer
                return 1;
            }
            if (oss) {
                // Non-snapshots are older
                return -1;
            }

            // If both are snapshots but have different case, compare them to remain consistent with equals()
            return qualifier.compareTo(other.qualifier);
        }

        if (ss) {
            // Snapshots are newer than numbers
            return 1;
        }

        if (oss) {
            // Numbers are older than snapshots
            return -1;
        }

        // Numbers are newer than non-numbers
        return ql >= 0 ? 1 : -1;
    }

    public int compareTo(org.osgi.framework.Version other) {
        int result = major - other.getMajor();
        if (result != 0) {
            return result;
        }

        result = minor - other.getMinor();
        if (result != 0) {
            return result;
        }

        result = micro - other.getMicro();
        if (result != 0) {
            return result;
        }

        String oq = other.getQualifier();

        if (qualifier.equals(oq)) {
            // Includes if both are empty
            return 0;
        }
        if (qualifier.isEmpty()) {
            // Release is newer
            return 1;
        }
        if (oq.isEmpty()) {
            // Release is newer
            return -1;
        }

        Matcher rcMatcher = RC_PATTERN.matcher(qualifier);
        Matcher orcMatcher = RC_PATTERN.matcher(oq);
        Matcher msMatcher = MILESTONE_PATTERN.matcher(qualifier);
        Matcher omsMatcher = MILESTONE_PATTERN.matcher(oq);
        boolean rc = rcMatcher.matches();
        boolean orc = orcMatcher.matches();
        boolean ms = msMatcher.matches();
        boolean oms = omsMatcher.matches();
        if (rc && orc) {
            // Both are release candidates
            int n = Integer.valueOf(rcMatcher.group(1));
            int on = Integer.valueOf(orcMatcher.group(1));
            return Integer.compare(n, on);
        }
        if (ms && oms) {
            // Both are milestones
            int n = Integer.valueOf(msMatcher.group(1));
            int on = Integer.valueOf(omsMatcher.group(1));
            return Integer.compare(n, on);
        }

        if (rc && oms) {
            // Release candidate is newer than milestone
            return 1;
        }
        if (ms && orc) {
            // Milestone is older than release candidate
            return -1;
        }

        long ql, oql;
        try {
            ql = Long.parseLong(qualifier);
        } catch (NumberFormatException e) {
            ql = Long.MIN_VALUE;
        }
        try {
            oql = Long.parseLong(oq);
        } catch (NumberFormatException e) {
            oql = Long.MIN_VALUE;
        }

        if (ql >= 0 && oql >= 0) {
            // Both are positive integers, compare numerically
            return Long.compare(ql, oql);
        }

        boolean ss = SNAPSHOT_PATTERN.matcher(qualifier).matches();
        boolean oss = SNAPSHOT_PATTERN.matcher(oq).matches();
        if (ql < 0 && oql < 0) {
            // Both aren't positive integers, snapshots are newer, otherwise do a simple string comparison
            if (ss) {
                // Snapshots are newer
                return 1;
            }
            if (oss) {
                // Non-snapshots are older
                return -1;
            }

            // If both are snapshots but have different case, compare them to remain consistent with equals()
            return qualifier.compareTo(oq);
        }

        if (ss) {
            // Snapshots are newer than numbers
            return 1;
        }

        if (oss) {
            // Numbers are older than snapshots
            return -1;
        }

        // Numbers are newer than non-numbers
        return ql >= 0 ? 1 : -1;
    }

    public static Version valueOf(@Nullable String version) {
        if (version == null) {
            return EMPTY_VERSION;
        }
        String v = version.trim();
        if (v.length() == 0) {
            return EMPTY_VERSION;
        }

        return new Version(v);
    }

    public static Version valueOf(org.osgi.framework.Version version) {
        return new Version(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
    }
}
