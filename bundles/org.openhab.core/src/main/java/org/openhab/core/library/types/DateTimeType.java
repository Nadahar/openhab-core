/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.library.types;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.zone.ZoneRulesException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.Command;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * A primitive immutable type that holds a date, time and timezone using the Christian/Gregorian calendar.
 *
 * @implNote This type has the concept of <i>authoritative</i> timezone. An authoritative timezone is the originating
 * timezone for the date and time data. If the originating timezone is unknown, an arbitrary timezone can be used,
 * in which case the timezone is non-authoritative. A non-authoritative {@link DateTimeType} will be converted to
 * the configured timezone, and made authoritative, before being published on the event bus.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Erdoan Hadzhiyusein - Refactored to use ZonedDateTime
 * @author Jan N. Klug - add ability to use time or date only
 * @author Wouter Born - increase parsing and formatting precision
 * @author Laurent Garnier - added methods toLocaleZone and toZone
 * @author Gaël L'hopital - added ability to use second and milliseconds unix time
 * @author Jimmy Tanagra - implement Comparable
 * @author Jacob Laursen - Refactored to use {@link Instant} internally
 * @author Ravi Nadahar - Resurrected timezone
 */
@NonNullByDefault
public class DateTimeType implements PrimitiveType, State, Command, Comparable<DateTimeType> {

    // external format patterns for output
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_PATTERN_WITH_TZ = "yyyy-MM-dd'T'HH:mm:ssz";
    // this pattern returns the time zone in RFC822 format
    public static final String DATE_PATTERN_WITH_TZ_AND_MS = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATE_PATTERN_WITH_TZ_AND_MS_GENERAL = "yyyy-MM-dd'T'HH:mm:ss.SSSz";
    public static final String DATE_PATTERN_WITH_TZ_AND_MS_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    // serialization of Date, Java 17 compatible format
    public static final String DATE_PATTERN_JSON_COMPAT = "MMM d, yyyy, h:mm:ss aaa";

    // internal patterns for parsing
    private static final String DATE_PARSE_PATTERN_WITHOUT_TZ = "yyyy-MM-dd'T'HH:mm"
            + "[:ss[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]]";
    private static final String DATE_PARSE_PATTERN_WITH_TZ = DATE_PARSE_PATTERN_WITHOUT_TZ + "z";
    private static final String DATE_PARSE_PATTERN_WITH_TZ_RFC = DATE_PARSE_PATTERN_WITHOUT_TZ + "Z";
    private static final String DATE_PARSE_PATTERN_WITH_TZ_ISO = DATE_PARSE_PATTERN_WITHOUT_TZ + "X";

    private static final DateTimeFormatter PARSER = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITHOUT_TZ);
    private static final DateTimeFormatter PARSER_TZ = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ);
    private static final DateTimeFormatter PARSER_TZ_RFC = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ_RFC);
    private static final DateTimeFormatter PARSER_TZ_ISO = DateTimeFormatter.ofPattern(DATE_PARSE_PATTERN_WITH_TZ_ISO);

    private static final Pattern DATE_PARSE_PATTERN_WITH_SPACE = Pattern
            .compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}.*");

    // internal patterns for formatting
    private static final String DATE_FORMAT_PATTERN_WITH_TZ_RFC = "yyyy-MM-dd'T'HH:mm[:ss[.SSSSSSSSS]]Z";
    private static final DateTimeFormatter FORMATTER_TZ_RFC = DateTimeFormatter
            .ofPattern(DATE_FORMAT_PATTERN_WITH_TZ_RFC);

    private final Instant instant;
    private final ZoneOffset zoneOffset;
    private final ZoneId zoneId;
    private final boolean authoritativeZone;

    /**
     * Creates a new {@link DateTimeType} representing the current instant from the system clock with a
     * non-authoritative timezone.
     */
    public DateTimeType() {
        this(Instant.now());
    }

    /**
     * Creates a new {@link DateTimeType} representing the specified instant with a non-authoritative timezone.
     *
     * @param instant the moment in time.
     */
    public DateTimeType(Instant instant) {
        this.instant = instant;
        ZoneId zoneId = ZoneId.systemDefault();
        this.zoneId = zoneId;
        if (zoneId instanceof ZoneOffset zoneOffset) {
            this.zoneOffset = zoneOffset;
        } else {
            this.zoneOffset = zoneId.getRules().getOffset(instant);
        }
        this.authoritativeZone = false;
    }

    /**
     * Creates a new {@link DateTimeType} representing the specified instant and an authoritative timezone or offset.
     *
     * @param instant the moment in time.
     * @param zoneId the {@link ZoneId} or {@link ZoneOffset}.
     */
    public DateTimeType(Instant instant, ZoneId zoneId) {
        this.instant = instant;
        ZoneId resolvedZoneId;
        ZoneOffset resolvedOffset;
        if (zoneId instanceof ZoneOffset offset) {
            resolvedZoneId = zoneId;
            resolvedOffset = offset;
        } else {
            resolvedZoneId = zoneId;
            resolvedOffset = zoneId.getRules().getOffset(instant);
        }

        this.zoneId = resolvedZoneId;
        this.zoneOffset = resolvedOffset;
        this.authoritativeZone = true;
    }

    /**
     * Creates a new {@link DateTimeType} representing the instant dictated by the specified local date and time in
     * combination with the specified or default timezone or offset.
     * <p>
     * <b>Note:</b> The resulting {@link DateTimeType} has an authoritative timezone of offset if {@code ZoneId} is
     * specified.
     * If {@code ZoneId} is {@code null}, the JVM default timezone will be used to interpret the local date and time,
     * and the timezone will be non-authoritative.
     *
     * @param localDateTime the local date and time without timezone information.
     * @param zoneId the {@link ZoneId} or {@link ZoneOffset}.
     */
    public DateTimeType(LocalDateTime localDateTime, @Nullable ZoneId zoneId) {
        ZoneId resolvedZoneId;
        ZoneOffset resolvedOffset;
        boolean resolvedAuthoritative;
        if (zoneId instanceof ZoneOffset offset) {
            resolvedZoneId = zoneId;
            resolvedOffset = offset;
            resolvedAuthoritative = true;
        } else if (zoneId == null) {
            resolvedZoneId = ZoneId.systemDefault();
            if (resolvedZoneId instanceof ZoneOffset offset) {
                resolvedOffset = offset;
            } else {
                resolvedOffset = resolvedZoneId.getRules().getOffset(localDateTime);
            }
            resolvedAuthoritative = false;
        } else {
            resolvedZoneId = zoneId;
            resolvedOffset = zoneId.getRules().getOffset(localDateTime);
            resolvedAuthoritative = true;
        }

        this.instant = localDateTime.toInstant(resolvedOffset);
        this.zoneId = resolvedZoneId;
        this.zoneOffset = resolvedOffset;
        this.authoritativeZone = resolvedAuthoritative;
    }

    /**
     * Creates a new {@link DateTimeType} with the given value with an authoritative timezone.
     *
     * @param zoned the moment in time.
     */
    public DateTimeType(ZonedDateTime zoned) {
        this(zoned, true);
    }

    public DateTimeType(ZonedDateTime zoned, boolean authoritativeZone) {
        this.instant = zoned.toInstant();
        this.zoneId = zoned.getZone();
        this.zoneOffset = zoned.getOffset();
        this.authoritativeZone = authoritativeZone;
    }

    // doc: throws
    public DateTimeType(String zonedValue) throws IllegalArgumentException {
        ParsedDateTimeResult result = parseDateTime(zonedValue);
        authoritativeZone = result.authoritativeZone;
        instant = result.zdt.toInstant();
        zoneId = result.zdt.getZone();
        zoneOffset = result.zdt.getOffset();
    }

    // doc: throws, always auth
    public DateTimeType(String zonedValue, ZoneId zoneId) throws IllegalArgumentException {
        ParsedDateTimeResult result = parseDateTime(zonedValue);
        ZonedDateTime zdt = result.zdt.withZoneSameInstant(zoneId);
        this.authoritativeZone = true;
        this.instant = zdt.toInstant();
        this.zoneId = zdt.getZone();
        this.zoneOffset = zdt.getOffset();
    }

    /**
     * Get this date and time represented as a {@link ZonedDateTime}.
     * <p>
     * <b>Note:</b> Since `ZonedDateTime` has no authoritative timezone concept, the current timezone will be used
     * whether this {@link DateTimeType} is authoritative or not.
     *
     * @return The {@link ZonedDateTime} representation.
     */
    public ZonedDateTime getZonedDateTime() {
        return getZonedDateTime(zoneId);
    }

    /**
     * Get this date and time represented as a {@link ZonedDateTime} with the the provided timezone applied.
     *
     * @return The {@link ZonedDateTime} representation.
     */
    public ZonedDateTime getZonedDateTime(ZoneId zoneId) {
        return instant.atZone(zoneId);
    }

    /**
     * Get this date and time represented as a {@link OffsetDateTime}.
     * <p>
     * <b>Note:</b> Since `OffsetDateTime` has no authoritative timezone concept, the current offset will be used
     * whether this {@link DateTimeType} is authoritative or not.
     *
     * @return The {@link OffsetDateTime} representation.
     */
    public OffsetDateTime getOffsetDateTime() {
        return OffsetDateTime.ofInstant(instant, zoneOffset);
    }

    /**
     * Get the date and time represented in UTC as an {@link Instant}.
     *
     * @return The UTC-aligned {@link Instant}.
     */
    public Instant getInstant() {
        return instant;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    public boolean isZoneAuthoritative() {
        return authoritativeZone;
    }

    public static DateTimeType valueOf(String value) {
        return new DateTimeType(value);
    }

    public static DateTimeType now() {
        return new DateTimeType();
    }

    @Deprecated(forRemoval = false)
    @Override
    public String format(@Nullable String pattern) {
        return format(pattern, zoneId);
    }

    @Deprecated(forRemoval = false)
    public String format(@Nullable String pattern, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        if (pattern == null) {
            return DateTimeFormatter.ofPattern(DATE_PATTERN).format(zonedDateTime);
        }

        return String.format(pattern, zonedDateTime);
    }

    public String format(Locale locale, @Nullable String pattern) {
        return format(locale, pattern, zoneId);
    }

    public String format(Locale locale, @Nullable String pattern, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        if (pattern == null) {
            return DateTimeFormatter.ofPattern(DATE_PATTERN, locale).format(zonedDateTime);
        }

        return String.format(locale, pattern, zonedDateTime);
    }

    /**
     *
     * TODO: (Nad) Authoritative
     *
     * @param zone the target zone as a string
     * @return a {@link DateTimeType} translated to the given zone
     * @throws DateTimeException if the zone has an invalid format or the result exceeds the supported date range
     * @throws ZoneRulesException if the zone is a region ID that cannot be found
     */
    public DateTimeType toZone(String zone) throws DateTimeException, ZoneRulesException {
        return toZone(ZoneId.of(zone));
    }

    public DateTimeType toOffset(ZoneOffset offset) throws DateTimeException {
        return toZone(offset);
    }

    public DateTimeType toFixedOffset() {
        return zoneId instanceof ZoneOffset ? this : toZone(zoneOffset);
    }

    /**
     * Create a {@link DateTimeType} being the translation of the current object to a given zone
     * Create a {@link DateTimeType} being the translation of the current object to a given zone
     * TODO: (Nad) Authoritative
     *
     * @param zoneId the target {@link ZoneId}
     * @return a {@link DateTimeType} translated to the given zone
     * @throws DateTimeException if the result exceeds the supported date range
     */
    public DateTimeType toZone(ZoneId zoneId) throws DateTimeException {
        return this.authoritativeZone && this.zoneId.equals(zoneId) ? this : new DateTimeType(instant, zoneId);
    }

    @Override
    public String toString() {
        return toString(zoneId);
    }

    @Override
    public String toFullString() {
        return toFullString(zoneId);
    }

    public String toString(ZoneId zoneId) {
        String formatted = instant.atZone(zoneId).format(FORMATTER_TZ_RFC);
        if (formatted.contains(".")) {
            String sign = "";
            if (formatted.contains("+")) {
                sign = "+";
            } else if (formatted.contains("-")) {
                sign = "-";
            }
            if (!sign.isEmpty()) {
                // the formatted string contains 9 fraction-of-second digits
                // truncate at most 2 trailing groups of 000s
                return formatted.replace("000" + sign, sign).replace("000" + sign, sign);
            }
        }
        return formatted;
    }

    public String toFullString(ZoneId zoneId) {
        String formatted = instant.atZone(zoneId).format(DateTimeFormatter.ISO_DATE_TIME);
        if (formatted.contains(".")) {
            String sign = "";
            if (formatted.contains("+")) {
                sign = "+";
            } else if (formatted.contains("-")) {
                sign = "-";
            }
            if (!sign.isEmpty()) {
                // the formatted string contains 9 fraction-of-second digits
                // truncate at most 2 trailing groups of 000s
                return formatted.replace("000" + sign, sign).replace("000" + sign, sign);
            }
        }
        return authoritativeZone ? formatted : '?' + formatted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(authoritativeZone, instant, zoneId, zoneOffset);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DateTimeType)) {
            return false;
        }
        DateTimeType other = (DateTimeType) obj;
        return authoritativeZone == other.authoritativeZone && Objects.equals(instant, other.instant)
                && Objects.equals(zoneId, other.zoneId) && Objects.equals(zoneOffset, other.zoneOffset);
    }

    @Override
    public int compareTo(DateTimeType o) {
        return instant.compareTo(o.instant);
    }

    public static record ParsedDateTimeResult(ZonedDateTime zdt, boolean authoritativeZone) {
    }

    // doc: throws
    public static ParsedDateTimeResult parseDateTime(String value) throws IllegalArgumentException {
        String dateTime;
        boolean explicitNotAuthoritative;
        if (value.charAt(0) == '?') {
            dateTime = value.substring(1);
            explicitNotAuthoritative = true;
        } else {
            dateTime = value;
            explicitNotAuthoritative = false;
        }
        try {
            // direct parsing (date and time)
            Temporal temporal;
            try {
                if (DATE_PARSE_PATTERN_WITH_SPACE.matcher(dateTime).matches()) {
                    temporal = parse(dateTime.substring(0, 10) + "T" + dateTime.substring(11));
                } else {
                    temporal = parse(dateTime);
                }
            } catch (DateTimeParseException fullDtException) {
                // time only
                try {
                    temporal = parse("1970-01-01T" + dateTime);
                } catch (DateTimeParseException timeOnlyException) {
                    try {
                        long epoch = Double.valueOf(dateTime).longValue();
                        int length = (int) (Math.log10(epoch >= 0 ? epoch : epoch * -1) + 1);
                        // Assume that below 12 digits we're in seconds
                        if (length < 12) {
                            temporal = Instant.ofEpochSecond(epoch);
                        } else {
                            temporal = Instant.ofEpochMilli(epoch);
                        }
                    } catch (NumberFormatException notANumberException) {
                        // date only
                        if (dateTime.length() == 10) {
                            temporal = parse(dateTime + "T00:00:00");
                        } else {
                            temporal = parse(dateTime.substring(0, 10) + "T00:00:00" + dateTime.substring(10));
                        }
                    }
                }
            }

            boolean authoritativeZone;
            if (temporal instanceof LocalDateTime localDateTime) {
                temporal = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
                authoritativeZone = false;
            } else if (temporal instanceof Instant instant) {
                temporal = instant.atZone(ZoneId.systemDefault());
                authoritativeZone = false;
            } else {
                authoritativeZone = true;
            }
            return new ParsedDateTimeResult((ZonedDateTime) temporal, !explicitNotAuthoritative && authoritativeZone);
        } catch (DateTimeParseException invalidFormatException) {
            throw new IllegalArgumentException(dateTime + " is not in a valid format.", invalidFormatException);
        }
    }

    private static Temporal parse(String value) throws DateTimeParseException {
        ZonedDateTime result;
        try {
            result = ZonedDateTime.parse(value, PARSER_TZ_RFC);
        } catch (DateTimeParseException tzMsRfcException) {
            try {
                result = ZonedDateTime.parse(value, PARSER_TZ_ISO);
            } catch (DateTimeParseException tzMsIsoException) {
                try {
                    result = ZonedDateTime.parse(value, PARSER_TZ);
                } catch (DateTimeParseException tzException) {
                    try {
                        result = ZonedDateTime.parse(value);
                    } catch (DateTimeParseException e) {
                        return LocalDateTime.parse(value, PARSER);
                    }
                }
            }
        }

        return result;
    }
}
