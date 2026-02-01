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
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Erdoan Hadzhiyusein - Refactored to use ZonedDateTime
 * @author Jan N. Klug - add ability to use time or date only
 * @author Wouter Born - increase parsing and formatting precision
 * @author Laurent Garnier - added methods toLocaleZone and toZone
 * @author Gaël L'hopital - added ability to use second and milliseconds unix time
 * @author Jimmy Tanagra - implement Comparable
 * @author Jacob Laursen - Refactored to use {@link Instant} internally
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
     * Creates a new {@link DateTimeType} representing the current
     * instant from the system clock.
     */
    public DateTimeType() {
        this(Instant.now());
    }

    /**
     * Creates a new {@link DateTimeType} with the given value without an authoritative timezone.
     * <p>
     * <b>Note:</b> For the timezone to be preserved, use one of the other constructors.
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

    // TODO: (Nad) JavaDocs everywhere
    public DateTimeType(Instant instant, ZoneId zoneId) {
        this.instant = instant;
        this.zoneId = zoneId;
        if (zoneId instanceof ZoneOffset offset) {
            this.zoneOffset = offset;
        } else {
            this.zoneOffset = zoneId.getRules().getOffset(instant);
        }
        this.authoritativeZone = true;
    }

    // Doc: Authoritative unless both null
    public DateTimeType(Instant instant, @Nullable ZoneId zoneId, @Nullable ZoneOffset zoneOffset) {
        this.instant = instant;
        ZoneId resolvedZoneId;
        ZoneOffset resolvedOffset;
        boolean resolvedAuthoritative;
        if (zoneId instanceof ZoneOffset offset) {
            resolvedZoneId = zoneId;
            resolvedOffset = offset;
            resolvedAuthoritative = true;
        } else if (zoneId == null) {
            if (zoneOffset == null) {
                resolvedZoneId = ZoneId.systemDefault();
                if (resolvedZoneId instanceof ZoneOffset offset) {
                    resolvedOffset = offset;
                } else {
                    resolvedOffset = resolvedZoneId.getRules().getOffset(instant);
                }
                resolvedAuthoritative = false;
            } else {
                resolvedZoneId = zoneOffset;
                resolvedOffset = zoneOffset;
                resolvedAuthoritative = true;
            }
        } else {
            resolvedZoneId = zoneId;
            resolvedOffset = zoneId.getRules().getOffset(instant);
            resolvedAuthoritative = true;
        }

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
     * Get object represented as a {@link ZonedDateTime} with system
     * default time-zone applied
     *
     * @return a {@link ZonedDateTime} representation of the object
     */
    public ZonedDateTime getZonedDateTime() {
        return getZonedDateTime(zoneId);
    }

    /**
     * Get object represented as a {@link ZonedDateTime} with the
     * the provided time-zone applied
     *
     * @return a {@link ZonedDateTime} representation of the object
     */
    public ZonedDateTime getZonedDateTime(ZoneId zoneId) {
        return instant.atZone(zoneId);
    }

    /**
     * Get the current object represented in UTC as an {@link Instant}.
     *
     * @return The resulting UTC {@link Instant}.
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
        return this.authoritativeZone && this.zoneId.equals(zoneId) ? this : new DateTimeType(instant, zoneId, null);
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
