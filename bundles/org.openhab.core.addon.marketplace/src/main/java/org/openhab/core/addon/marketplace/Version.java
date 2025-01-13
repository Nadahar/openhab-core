package org.openhab.core.addon.marketplace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class Version extends org.osgi.framework.Version {

    protected static final Pattern NON_DOT_SEPARATOR = Pattern.compile("^\\d+\\.\\d+\\.\\d+(?:-|_).+$");
    protected static final Pattern RC_PATTERN = Pattern.compile("(?i)rc(\\d+)");
    protected static final Pattern MILESTONE_PATTERN = Pattern.compile("(?i)m(\\d+)");
    protected static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(?i)snapshot");

    /**
     * The empty version "0.0.0".
     */
    public static final Version emptyVersion = new Version(0, 0, 0);

    protected Version(String version) {
        super(version);
    }

    public Version(int major, int minor, int micro) {
        super(major, minor, micro);
    }

    public Version(int major, int minor, int micro, @Nullable String qualifier) {
        super(major, minor, micro, qualifier);
    }

    public static Version parseVersion(@Nullable String version) {
        if (version == null) {
            return emptyVersion;
        }

        return valueOf(version);
    }

    public static Version valueOf(String version) {
        String v = version.trim();
        if (v.length() == 0) {
            return emptyVersion;
        }

        Matcher m = NON_DOT_SEPARATOR.matcher(v);
        if (m.find()) {
            v = v.replaceFirst("-|_", ".");
        }
        return new Version(v);
    }

    public static Version valueOf(org.osgi.framework.Version version) {
        return new Version(version.getMajor(), version.getMinor(), version.getMicro(), version.getQualifier());
    }

    @Override
    public int compareTo(org.osgi.framework.Version other) {
        if (other == this) { // quicktest
            return 0;
        }


        int result = getMajor() - other.getMajor();
        if (result != 0) {
            return result;
        }

        result = getMinor() - other.getMinor();
        if (result != 0) {
            return result;
        }

        result = getMicro() - other.getMicro();
        if (result != 0) {
            return result;
        }

        String q = getQualifier();
        String oq = other.getQualifier();

        if (q.equals(oq)) {
            // Includes if both are empty
            return 0;
        }
        if (q.isEmpty()) {
            // Release is newer
            return 1;
        }
        if (oq.isEmpty()) {
            // Release is newer
            return -1;
        }

        Matcher rcMatcher = RC_PATTERN.matcher(q);
        Matcher orcMatcher = RC_PATTERN.matcher(oq);
        Matcher msMatcher = MILESTONE_PATTERN.matcher(q);
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
            ql = Long.parseLong(q);
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

        boolean ss = SNAPSHOT_PATTERN.matcher(q).matches();
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
            return q.compareTo(oq);
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
}
