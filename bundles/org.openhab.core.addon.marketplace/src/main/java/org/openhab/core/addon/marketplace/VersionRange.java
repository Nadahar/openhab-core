package org.openhab.core.addon.marketplace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class VersionRange extends org.osgi.framework.VersionRange {

    public static final VersionRange ANY = new VersionRange('[', Version.valueOf("0.0.0"), null, ']');
    protected static final Pattern WHITESPACE = Pattern.compile("\\s+");
    protected static final Pattern SEPARATORS = Pattern.compile(":|;|\\.\\.");
    public static final Pattern RANGE_PATTERN = Pattern.compile(
            "\\s*(?<leftType>[\\[\\(])(?<left>\\d+(\\.\\d+(\\.\\d+(\\.[^\\)\\]]+)?)?)?)(?:,|;|:|\\.\\.)(?<right>\\d+(\\.\\d+(\\.\\d+(\\.[^\\)\\]]+)?)?)?)?(?<rightType>[\\]\\)])\\s*$");

    public VersionRange(char leftType, Version leftEndpoint, @Nullable Version rightEndpoint, char rightType) {
        super(leftType, leftEndpoint, rightEndpoint, rightType);
    }

    @Override
    public boolean includes(@SuppressWarnings("null") org.osgi.framework.Version version) {
        org.osgi.framework.Version right;
        if (!version.getQualifier().isEmpty() && getRightType() == RIGHT_OPEN && (right = getRight()) != null && right.getQualifier().isEmpty()) {
            // This is a special case where, although technically correct, we don't want e.g 5.0.0.RC1 to be included in [x.x.x,5.0.0)
            if (version instanceof Version) {
                return super.includes(new Version(version.getMajor(), version.getMinor(), version.getMicro()));
            }
            return super.includes(new org.osgi.framework.Version(version.getMajor(), version.getMinor(), version.getMicro()));
        }
        return super.includes(version);
    }

    @Override
    public String toString() {
        if (getRight() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLeftType()).append(getLeft().toString()).append(",]");
            return sb.toString();
        }
        return super.toString();
    }

    public static VersionRange valueOf(@Nullable String range) {
        if (range == null || range.isBlank()) {
            return ANY;
        }
        String r = range;
        Matcher matcher = WHITESPACE.matcher(r);
        if (matcher.find()) {
            r = matcher.replaceAll("");
        }
        matcher = SEPARATORS.matcher(r);
        if (matcher.find()) {
            r = matcher.replaceAll(",");
        }
        matcher = RANGE_PATTERN.matcher(r);
        if (matcher.find()) {
            String right = matcher.group("right");
            return new VersionRange(matcher.group("leftType").charAt(0), Version.parseVersion(matcher.group("left")), right == null ? null : Version.parseVersion(right), matcher.group("rightType").charAt(0));
        }
        throw new IllegalArgumentException("Invalid range \"" + range + '"');
    }
}
