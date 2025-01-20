package org.openhab.core.addon.marketplace;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

// Immutable
@NonNullByDefault
public class VersionRange {

    public static final VersionRange ANY = new VersionRange(true, Version.valueOf("0.0.0"), null, true);

    /** The left endpoint is open and is excluded from the range ({@code '('}) */
    public static final char LEFT_OPEN = '(';

    /** The left endpoint is closed and is included in the range ({@code '['}) */
    public static final char LEFT_CLOSED = '[';

    /** The right endpoint is open and is excluded from the range ({@code ')'}) */
    public static final char RIGHT_OPEN = ')';

    /** The right endpoint is closed and is included in the range ({@code ']'}) */
    public static final char RIGHT_CLOSED = ']';
    public static final Pattern RANGE_PATTERN = Pattern.compile(
        "\\s*(?<leftType>[\\[\\(])(?<left>\\d+(\\.\\d+(\\.\\d+(\\.[^\\)\\]]+)?)?)?)(?:,|;|:|\\.\\.)(?<right>\\d+(\\.\\d+(\\.\\d+(\\.[^\\)\\]]+)?)?)?)?(?<rightType>[\\]\\)])\\s*$");

    protected static final Pattern WHITESPACE = Pattern.compile("\\s+");
    protected static final Pattern SEPARATORS = Pattern.compile(":|;|\\.\\.");

    protected final boolean leftClosed;
    protected final Version left;
    protected final @Nullable Version right;
    protected final boolean rightClosed;
    private transient @Nullable String versionRangeString;
    private transient int hash;

    @SuppressWarnings("null")
    public VersionRange(String range) {
        if (range == null || range.isBlank()) {
            throw new IllegalArgumentException("range cannot be blank");
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
        if (!matcher.find() || matcher.group("left").isBlank()) {
            throw new IllegalArgumentException("Invalid range \"" + range + '"');
        }
        String right = matcher.group("right");
        this.leftClosed = "[".equals(matcher.group("leftType"));
        this.left = Version.valueOf(matcher.group("left"));
        this.right = right == null ? null : Version.valueOf(right);
        this.rightClosed = "]".equals(matcher.group("rightType"));
    }

    public VersionRange(char leftType, Version leftEndpoint, @Nullable Version rightEndpoint, char rightType) {
        if ((leftType != LEFT_CLOSED) && (leftType != LEFT_OPEN)) {
            throw new IllegalArgumentException("Invalid leftType \"" + leftType + "\"");
        }
        if ((rightType != RIGHT_OPEN) && (rightType != RIGHT_CLOSED)) {
            throw new IllegalArgumentException("Invalid rightType \"" + rightType + "\"");
        }
        this.leftClosed = leftType == LEFT_CLOSED;
        this.left = leftEndpoint;
        this.right = rightEndpoint;
        this.rightClosed = rightType == RIGHT_CLOSED;
    }

    public VersionRange(boolean leftClosed, Version leftEndpoint, @Nullable Version rightEndpoint, boolean rightClosed) {
        this.leftClosed = leftClosed;
        this.left = leftEndpoint;
        this.right = rightEndpoint;
        this.rightClosed = rightClosed;
    }

    public boolean includes(Version version) {
        Version v = version;
        Version right = this.right;
        if (!v.getQualifier().isEmpty() && !this.rightClosed && right != null && right.getQualifier().isEmpty()) {
            // This is a special case where, although technically correct, we don't want e.g 5.0.0.RC1 to be included in [x.x.x,5.0.0)
            v = new Version(version.getMajor(), version.getMinor(), version.getMicro());
        }
        if (left.compareTo(v) >= (leftClosed ? 1 : 0)) {
            return false;
        }
        if (right == null) {
            return true;
        }
        return right.compareTo(v) >= (rightClosed ? 0 : 1);
    }


    @Override
    public int hashCode() {
        int h = hash;
        if (h != 0) {
            return h;
        }
        return h = Objects.hash(leftClosed, left, right, rightClosed);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VersionRange)) {
            return false;
        }
        VersionRange other = (VersionRange) obj;
        return Objects.equals(left, other.left) && leftClosed == other.leftClosed && Objects.equals(right, other.right)
            && rightClosed == other.rightClosed;
    }

    @Override
    public String toString() {
        String s = versionRangeString;
        if (s != null) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(leftClosed ? LEFT_CLOSED : LEFT_OPEN).append(left.toString()).append(',');
        Version r;
        if ((r = right) == null) {
            sb.append(RIGHT_CLOSED);
        } else {
            sb.append(r.toString()).append(rightClosed ? RIGHT_CLOSED : RIGHT_OPEN);
        }
        return versionRangeString = sb.toString();
    }

    public static VersionRange valueOf(@Nullable String range) {
        if (range == null || range.isBlank()) {
            return ANY;
        }
        return new VersionRange(range);
    }
}
