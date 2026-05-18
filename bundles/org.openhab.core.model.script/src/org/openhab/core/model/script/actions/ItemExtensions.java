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
package org.openhab.core.model.script.actions;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.Metadata;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.model.script.Items;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.ModifiablePersistenceService;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.openhab.core.persistence.extensions.PersistenceExtensions.RiemannType;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;

/**
 * {@link ItemExtensions} provides DSL access to things like OSGi instances, system registries and the ability to run
 * other
 * rules.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class ItemExtensions {

    @NonNullByDefault({})
    public static void addMetadata(Item item, String namespace, String value) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        Items.addMetadata(item.getName(), namespace, value, (String) null);
    }

    @NonNullByDefault({})
    public static void addMetadata(Item item, String namespace, String value, Object... configuration) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        Items.addMetadata(item.getName(), namespace, value, parseObjectArray(configuration));
    }

    @NonNullByDefault({})
    public static void addMetadata(Item item, String namespace, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        Items.addMetadata(item.getName(), namespace, value, configuration);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata getMetadata(Item item, String namespace) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.getMetadata(item.getName(), namespace);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata removeMetadata(Item item, String namespace) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.removeMetadata(item.getName(), namespace);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.updateMetadata(item.getName(), namespace, value);
    }

    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value,
            Object... configuration) {
        return Items.updateMetadata(item.getName(), namespace, value, parseObjectArray(configuration));
    }

    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(Item item, String namespace, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null");
        }
        return Items.updateMetadata(item.getName(), namespace, value, configuration);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the average value between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.averageBetween(item, begin, end);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the average value between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return PersistenceExtensions.averageBetween(item, begin, end, type);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.averageBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} between two certain points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State averageBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return PersistenceExtensions.averageBetween(item, begin, end, type, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @return the average value since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.averageSince(item, timestamp);
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the average value since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.averageSince(item, timestamp, type);
    }


    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.averageSince(item, timestamp, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time from which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.averageSince(item, timestamp, type, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @return the average value until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.averageUntil(item, timestamp);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the average value until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.averageUntil(item, timestamp, type);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.averageUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the average value of the state of a given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the average value for
     * @param timestamp the point in time to which to search for the average value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the average value until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State averageUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.averageUntil(item, timestamp, type, serviceId);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> between two points in time.
     * The default persistence service is used.
     *
     * @param item the item to get the delta for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the difference between end and begin, or <code>null</code> if the default persistence service does not
     *         refer to an available {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> for the given points in time
     */
    public static @Nullable State deltaBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.deltaBetween(item, begin, end);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the delta for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the difference between end and begin, or <code>null</code> if the given serviceId does not refer to an
     *         available {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given points in time
     */
    public static @Nullable State deltaBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.deltaBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to get the delta state value for
     * @param timestamp the point in time from which to compute the delta
     * @return the difference between now and then, or <code>null</code> if there is no default persistence
     *         service available, the default persistence service is not a {@link QueryablePersistenceService}, or if
     *         there is no persisted state for the given <code>item</code> at the given <code>timestamp</code> available
     *         in the default persistence service
     */
    public static @Nullable State deltaSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.deltaSince(item, timestamp);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the delta for
     * @param timestamp the point in time from which to compute the delta
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the difference between now and then, or <code>null</code> if the given serviceId does not refer to an
     *         available {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service named
     *         <code>serviceId</code>
     */
    public static @Nullable State deltaSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.deltaSince(item, timestamp, serviceId);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> until a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to get the delta state value for
     * @param timestamp the point in time to which to compute the delta
     * @return the difference between then and now, or <code>null</code> if there is no default persistence
     *         service available, the default persistence service is not a {@link QueryablePersistenceService}, or if
     *         there is no persisted state for the given <code>item</code> at the given <code>timestamp</code> available
     *         in the default persistence service
     */
    public static @Nullable State deltaUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.deltaUntil(item, timestamp);
    }

    /**
     * Gets the difference value of the state of a given <code>item</code> until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the delta for
     * @param timestamp the point in time to which to compute the delta
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the difference between then and now, or <code>null</code> if the given serviceId does not refer to an
     *         available {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service named
     *         <code>serviceId</code>
     */
    public static @Nullable State deltaUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.deltaUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.deviationBetween(item, begin, end);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return PersistenceExtensions.deviationBetween(item, begin, end, type);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.deviationBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State deviationBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return PersistenceExtensions.deviationBetween(item, begin, end, type, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.deviationSince(item, timestamp);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.deviationSince(item, timestamp, type);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.deviationSince(item, timestamp, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time from which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between then and now, or <code>null</code> if <code>timestamp</code> is in the
     *         future, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.deviationSince(item, timestamp, type, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.deviationUntil(item, timestamp);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.deviationUntil(item, timestamp, type);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.deviationUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the standard deviation of the state of the given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * <b>Note:</b> If you need variance and standard deviation at the same time do not query both as it is a costly
     * operation. Get the variance only, it is the squared deviation.
     *
     * @param item the {@link Item} to get the standard deviation for
     * @param timestamp the point in time to which to compute the standard deviation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the standard deviation between now and then, or <code>null</code> if <code>timestamp</code> is in the
     *         past, if the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State deviationUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.deviationUntil(item, timestamp, type, serviceId);
    }

    /**
     * Gets the median value of the state of a given {@link Item} between two certain points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the median value between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State medianBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.medianBetween(item, begin, end);
    }

    /**
     * Gets the median value of the state of a given {@link Item} between two certain points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the median value between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State medianBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.medianBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the median value of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time from which to search for the median value
     * @return the median value since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State medianSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.medianSince(item, timestamp);
    }

    /**
     * Gets the median value of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time from which to search for the median value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the median value since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State medianSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.medianSince(item, timestamp, serviceId);
    }

    /**
     * Gets the median value of the state of a given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time to which to search for the median value
     * @return the median value until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State medianUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.medianUntil(item, timestamp);
    }

    /**
     * Gets the median value of the state of a given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the median value for
     * @param timestamp the point in time to which to search for the median value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the median value until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State medianUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.medianUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the Riemann sum between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.riemannSumBetween(item, begin, end);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the Riemann sum between <code>begin</code> and <code>end</code> or <code>null</code> if no
     *         states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}.
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return PersistenceExtensions.riemannSumBetween(item, begin, end, type);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.riemannSumBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} between two certain points in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum between <code>begin</code> and <code>end</code>, or <code>null</code> if no
     *         states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable State riemannSumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return PersistenceExtensions.riemannSumBetween(item, begin, end, type, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @return the Riemann sum since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.riemannSumSince(item, timestamp);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the Riemann sum since <code>timestamp</code> or <code>null</code> if no
     *         previous states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.riemannSumSince(item, timestamp, type);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.riemannSumSince(item, timestamp, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} since a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time from which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum since <code>timestamp</code>, or <code>null</code> if no
     *         previous states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.riemannSumSince(item, timestamp, type, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @return the Riemann sum until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.riemannSumUntil(item, timestamp);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the Riemann sum until <code>timestamp</code> or <code>null</code> if no
     *         future states could be found or if the default persistence service does not refer to an available
     *         {@link QueryablePersistenceService}. The current state is included in the calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.riemannSumUntil(item, timestamp, type);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * A left approximation type is used for the Riemann sum.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.riemannSumUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the Riemann sum of the states of a given {@link Item} until a certain point in time.
     * This can be used as an approximation for integrating the curve represented by discrete values.
     * The time dimension in the result is in seconds, therefore if you do not use QuantityType results, you may have to
     * multiply or divide to get the result in the expected scale.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the riemannSum value for
     * @param timestamp the point in time to which to search for the riemannSum value
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the Riemann sum until <code>timestamp</code>, or <code>null</code> if no
     *         future states could be found or if the persistence service given by <code>serviceId</code> does not
     *         refer to an available {@link QueryablePersistenceService}. The current state is included in the
     *         calculation.
     */
    public static @Nullable State riemannSumUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.riemannSumUntil(item, timestamp, type, serviceId);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> between two certain points in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The default persistence service is used.
     *
     * @param item the item for which we will sum its persisted state values between <code>begin</code> and
     *            <code>end</code>
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @return the sum of the state values between the given points in time, or null if <code>begin</code> is after
     *         <code>end</code> or if the default persistence service does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public @Nullable static State sumBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.sumBetween(item, begin, end);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> between two certain points in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item for which we will sum its persisted state values between <code>begin</code> and
     *            <code>end</code>
     * @param begin the point in time from which to start the summation
     * @param end the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the sum of the state values between the given points in time, or null if <code>begin</code> is after
     *         <code>end</code> or <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.sumBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> since a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The default persistence service is used.
     *
     * @param item the item for which we will sum its persisted state values since <code>timestamp</code>
     * @param timestamp the point in time from which to start the summation
     * @return the sum of the state values since <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         future or the default persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.sumSince(item, timestamp);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> since a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item for which we will sum its persisted state values since <code>timestamp</code>
     * @param timestamp the point in time from which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the sum of the state values since <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         future or <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.sumSince(item, timestamp, serviceId);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> until a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The default persistence service is used.
     *
     * @param item the item for which we will sum its persisted state values to <code>timestamp</code>
     * @param timestamp the point in time to which to start the summation
     * @return the sum of the state values until <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         past or the default persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.sumUntil(item, timestamp);
    }

    /**
     * Gets the sum of the state of a given <code>item</code> until a certain point in time.
     * This method does not calculate a Riemann sum and therefore cannot be used as an approximation for the integral
     * value.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item for which we will sum its persisted state values to <code>timestamp</code>
     * @param timestamp the point in time to which to start the summation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the sum of the state values until <code>timestamp</code>, or null if <code>timestamp</code> is in the
     *         past or <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public @Nullable static State sumUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.sumUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute the variance
     * @param end the end time for the computation
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.varianceBetween(item, begin, end);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute the variance
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if there is no default persistence service available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type) {
        return PersistenceExtensions.varianceBetween(item, begin, end, type);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two points in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.varianceBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param begin the point in time from which to compute
     * @param end the end time for the computation
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between both points of time, or <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service given by
     *         <code>serviceId</code> is not available, or it is not a {@link QueryablePersistenceService}, or it is not
     *         a {@link QueryablePersistenceService}, or if there is no persisted state for the
     *         given <code>item</code> between <code>begin</code> and <code>end</code>
     */
    public static @Nullable State varianceBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable RiemannType type, @Nullable String serviceId) {
        return PersistenceExtensions.varianceBetween(item, begin, end, type, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.varianceSince(item, timestamp);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.varianceSince(item, timestamp, type);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.varianceSince(item, timestamp, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time from which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between then and now, or <code>null</code> if <code>timestamp</code> is in the future, if
     *         the persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceSince(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.varianceSince(item, timestamp, type, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.varianceUntil(item, timestamp);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if
     *         there is no default persistence service available, or it is not a {@link QueryablePersistenceService}, or
     *         if there is no persisted state for the given <code>item</code> at the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type) {
        return PersistenceExtensions.varianceUntil(item, timestamp, type);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * A left approximation type is used for the Riemann sum.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if the
     *         persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.varianceUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the variance of the state of the given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the variance for
     * @param timestamp the point in time to which to compute the variance
     * @param type LEFT, RIGHT, MIDPOINT or TRAPEZOIDAL representing approximation types for Riemann sums
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the variance between now and then, or <code>null</code> if <code>timestamp</code> is in the past, if the
     *         persistence service given by <code>serviceId</code> is not available, or it is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>
     */
    public static @Nullable State varianceUntil(Item item, ZonedDateTime timestamp, @Nullable RiemannType type,
            @Nullable String serviceId) {
        return PersistenceExtensions.varianceUntil(item, timestamp, type, serviceId);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the item to get the evolution rate value for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the evolution rate in percent (positive and negative) in the given interval, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there are no persisted state for the given <code>item</code>
     *         at the given interval, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.evolutionRateBetween(item, begin, end);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) in the given interval, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>begin</code> and <code>end</code> using the persistence service
     *         given by <code>serviceId</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.evolutionRateBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the item to get the evolution rate value for
     * @param timestamp the point in time from which to compute the evolution rate
     * @return the evolution rate in percent (positive and negative) between now and then, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.evolutionRateSince(item, timestamp);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param timestamp the point in time from which to compute the evolution rate
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) between now and then, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service given by
     *         <code>serviceId</code>, or if there is a state but it is zero (which would cause a divide-by-zero
     *         error)
     */
    public static @Nullable DecimalType evolutionRateSince(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.evolutionRateSince(item, timestamp, serviceId);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} until a certain point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the item to get the evolution rate value for
     * @param timestamp the point in time to which to compute the evolution rate
     * @return the evolution rate in percent (positive and negative) between then and now, or <code>null</code> if
     *         there is no default persistence service available, the default persistence service is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given <code>item</code> at
     *         the given <code>timestamp</code>, or if there is a state but it is zero (which would cause a
     *         divide-by-zero error)
     */
    public static @Nullable DecimalType evolutionRateUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.evolutionRateUntil(item, timestamp);
    }

    /**
     * Gets the evolution rate of the state of a given {@link Item} until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to get the evolution rate value for
     * @param timestamp the point in time to which to compute the evolution rate
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the evolution rate in percent (positive and negative) between then and now, or <code>null</code> if
     *         the persistence service given by <code>serviceId</code> is not available or is not a
     *         {@link QueryablePersistenceService}, or if there is no persisted state for the given
     *         <code>item</code> at the given <code>timestamp</code> using the persistence service given by
     *         <code>serviceId</code>, or if there is a state but it is zero (which would cause a divide-by-zero
     *         error)
     */
    public static @Nullable DecimalType evolutionRateUntil(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.evolutionRateUntil(item, timestamp, serviceId);
    }

    /**
     * Persists the state of a given <code>item</code> through the default persistence service.
     *
     * @param item the item to store
     */
    public static void persist(Item item) {
        PersistenceExtensions.persist(item);
    }

    /**
     * Persists the state of a given <code>item</code> through a {@link PersistenceService} identified
     * by the <code>serviceId</code>.
     *
     * @param item the item to store
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, @Nullable String serviceId) {
        PersistenceExtensions.persist(item, serviceId);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through the default
     * persistence service.
     *
     * @param item the item to store
     * @param timestamp the date for the item state to be stored
     * @param state the state to be stored
     */
    public static void persist(Item item, ZonedDateTime timestamp, State state) {
        PersistenceExtensions.persist(item, timestamp, state);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item
     * @param timestamp the date for the item state to be stored
     * @param state the state to be stored
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, ZonedDateTime timestamp, State state, @Nullable String serviceId) {
        PersistenceExtensions.persist(item, timestamp, state, serviceId);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through the default
     * persistence service.
     *
     * @param item the item to store
     * @param timestamp the date for the item state to be stored
     * @param stateString the state to be stored
     */
    public static void persist(Item item, ZonedDateTime timestamp, String stateString) {
        PersistenceExtensions.persist(item, timestamp, stateString);
    }

    /**
     * Persists a <code>state</code> at a given <code>timestamp</code> of an <code>item</code> through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item
     * @param timestamp the date for the item state to be stored
     * @param stateString the state to be stored
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, ZonedDateTime timestamp, String stateString, @Nullable String serviceId) {
        PersistenceExtensions.persist(item, timestamp, stateString, serviceId);
    }

    /**
     * Persists a <code>timeSeries</code> of an <code>item</code> through the default persistence service.
     *
     * @param item the item to store
     * @param timeSeries the timeSeries of states to be stored
     */
    public static void persist(Item item, TimeSeries timeSeries) {
        PersistenceExtensions.persist(item, timeSeries);
    }

    /**
     * Persists a <code>timeSeries</code> of an <code>item</code> through a {@link PersistenceService} identified by the
     * <code>serviceId</code>.
     *
     * @param item the item
     * @param timeSeries the timeSeries of states to be stored
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void persist(Item item, TimeSeries timeSeries, @Nullable String serviceId) {
        PersistenceExtensions.persist(item, timeSeries, serviceId);
    }

    /**
     * Retrieves the persisted item for a given <code>item</code> at a certain point in time through the default
     * persistence service.
     *
     * @param item the item for which to retrieve the persisted item
     * @param timestamp the point in time for which the persisted item should be retrieved
     * @return the historic item at the given point in time, or <code>null</code> if no persisted item could be found,
     *         the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem persistedState(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.persistedState(item, timestamp);
    }

    /**
     * Retrieves the persisted item for a given <code>item</code> at a certain point in time through a
     * {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the persisted item
     * @param timestamp the point in time for which the persisted item should be retrieved
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the persisted item at the given point in time, or <code>null</code> if no persisted item could be found
     *         or if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem persistedState(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.persistedState(item, timestamp, serviceId);
    }

    /**
     * Query the last historic update time of a given <code>item</code>. The default persistence service is used.
     * Note the {@link Item#getLastStateUpdate()} is generally preferred to get the last update time of an item.
     *
     * @param item the item for which the last historic update time is to be returned
     * @return point in time of the last historic update to <code>item</code>, <code>null</code> if there are no
     *         historic persisted updates, the state has changed since the last update or the default persistence
     *         service is not available or not a {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastUpdate(Item item) {
        return PersistenceExtensions.lastUpdate(item);
    }

    /**
     * Query for the last historic update time of a given <code>item</code>.
     * Note the {@link Item#getLastStateUpdate()} is generally preferred to get the last update time of an item.
     *
     * @param item the item for which the last historic update time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the last historic update to <code>item</code>, <code>null</code> if there are no
     *         historic persisted updates, the state has changed since the last update or if persistence service given
     *         by <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastUpdate(Item item, @Nullable String serviceId) {
        return PersistenceExtensions.lastUpdate(item, serviceId);
    }

    /**
     * Query the first future update time of a given <code>item</code>. The default persistence service is used.
     *
     * @param item the item for which the first future update time is to be returned
     * @return point in time of the first future update to <code>item</code>, or <code>null</code> if there are no
     *         future persisted updates or the default persistence service is not available or not a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextUpdate(Item item) {
        return PersistenceExtensions.nextUpdate(item);
    }

    /**
     * Query for the first future update time of a given <code>item</code>.
     *
     * @param item the item for which the first future update time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the first future update to <code>item</code>, or <code>null</code> if there are no
     *         future persisted updates or if persistence service given by <code>serviceId</code> does not refer to an
     *         available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextUpdate(Item item, @Nullable String serviceId) {
        return PersistenceExtensions.nextUpdate(item, serviceId);
    }

    /**
     * Query the last historic change time of a given <code>item</code>. The default persistence service is used.
     * Note the {@link Item#getLastStateChange()} is generally preferred to get the last state change time of an item.
     *
     * @param item the item for which the last historic change time is to be returned
     * @return point in time of the last historic change to <code>item</code>, <code>null</code> if there are no
     *         historic persisted changes, the state has changed since the last update or the default persistence
     *         service is not available or not a {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastChange(Item item) {
        return PersistenceExtensions.lastChange(item);
    }

    /**
     * Query for the last historic change time of a given <code>item</code>.
     * Note the {@link Item#getLastStateChange()} is generally preferred to get the last state change time of an item.
     *
     * @param item the item for which the last historic change time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the last historic change to <code>item</code> <code>null</code> if there are no
     *         historic persisted changes, the state has changed since the last update or if persistence service given
     *         by <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime lastChange(Item item, @Nullable String serviceId) {
        return PersistenceExtensions.lastChange(item, serviceId);
    }

    /**
     * Query the first future change time of a given <code>item</code>. The default persistence service is used.
     *
     * @param item the item for which the first future change time is to be returned
     * @return point in time of the first future change to <code>item</code>, or <code>null</code> if there are no
     *         future persisted changes or the default persistence service is not available or not a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextChange(Item item) {
        return PersistenceExtensions.nextChange(item);
    }

    /**
     * Query for the first future change time of a given <code>item</code>.
     *
     * @param item the item for which the first future change time is to be returned
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return point in time of the first future change to <code>item</code>, or <code>null</code> if there are no
     *         future persisted changes or if persistence service given by <code>serviceId</code> does not refer to an
     *         available {@link QueryablePersistenceService}
     */
    public static @Nullable ZonedDateTime nextChange(Item item, @Nullable String serviceId) {
        return PersistenceExtensions.nextChange(item, serviceId);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item) {
        return PersistenceExtensions.previousState(item);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @param skipEqual if true, skips equal state values and searches the first state not equal the current state
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item, boolean skipEqual) {
        return PersistenceExtensions.previousState(item, skipEqual);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the previous state or <code>null</code> if no previous state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item, @Nullable String serviceId) {
        return PersistenceExtensions.previousState(item, serviceId);
    }

    /**
     * Returns the previous state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     * Note the {@link Item#getLastState()} is generally preferred to get the previous state of an item.
     *
     * @param item the item to get the previous state value for
     * @param skipEqual if <code>true</code>, skips equal state values and searches the first state not equal the
     *            current state
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the previous state or <code>null</code> if no previous state could be found, or if the given
     *         <code>serviceId</code> is not available or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem previousState(Item item, boolean skipEqual, @Nullable String serviceId) {
        return PersistenceExtensions.previousState(item, skipEqual, serviceId);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     *
     * @param item the item to get the next state value for
     * @return the next state or <code>null</code> if no next state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item) {
        return PersistenceExtensions.nextState(item);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     *
     * @param item the item to get the next state value for
     * @param skipEqual if true, skips equal state values and searches the first state not equal the current state
     * @return the next state or <code>null</code> if no next state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item, boolean skipEqual) {
        return PersistenceExtensions.nextState(item, skipEqual);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the next state value for
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the next state or <code>null</code> if no next state could be found, or if the default
     *         persistence service is not configured or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item, @Nullable String serviceId) {
        return PersistenceExtensions.nextState(item, serviceId);
    }

    /**
     * Returns the next state of a given <code>item</code>.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the next state value for
     * @param skipEqual if <code>true</code>, skips equal state values and searches the first state not equal the
     *            current state
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the next state or <code>null</code> if no next state could be found, or if the given
     *         <code>serviceId</code> is not available or does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem nextState(Item item, boolean skipEqual, @Nullable String serviceId) {
        return PersistenceExtensions.nextState(item, skipEqual, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> changes between two points in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state changes
     * @return <code>true</code> if item state changes, <code>false</code> if the item does not change in
     *         the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean changedBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.changedBetween(item, begin, end);
    }

    /**
     * Checks if the state of a given <code>item</code> changes between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state changed or <code>false</code> if the item does not change
     *         in the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.changedBetween(item, begin, end, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> has changed since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @return <code>true</code> if item state has changed, <code>false</code> if it has not changed, <code>null</code>
     *         if <code>timestamp</code> is in the future, if the default persistence service is not available or does
     *         not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.changedSince(item, timestamp);
    }

    /**
     * Checks if the state of a given <code>item</code> has changed since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state has changed, or <code>false</code> if it has not changed,
     *         <code>null</code> if <code>timestamp</code> is in the future, if the provided <code>serviceId</code> does
     *         not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.changedSince(item, timestamp, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> will change by a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to end the check
     * @return <code>true</code> if item state will change, <code>false</code> if it will not change, <code>null</code>
     *         if <code>timestamp></code> is in the past, if the default persistence service is not available or does
     *         not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.changedUntil(item, timestamp);
    }

    /**
     * Checks if the state of a given <code>item</code> will change by a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to end the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state will change, or <code>false</code> if it will not change,
     *         <code>null</code> if <code>timestamp</code> is in the past, if the provided <code>serviceId</code> does
     *         not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean changedUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.changedUntil(item, timestamp, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> has been updated between two points in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state updates
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @return <code>true</code> if item state was updated, <code>false</code> if the item has not been updated in
     *         the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean updatedBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.updatedBetween(item, begin, end);
    }

    /**
     * Checks if the state of a given <code>item</code> is updated between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state was updated or <code>false</code> if the item is not updated
     *         in the given interval, <code>null</code> if <code>begin</code> is after <code>end</code>, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean updatedBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.updatedBetween(item, begin, end, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> has been updated since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state updates
     * @param timestamp the point in time to start the check
     * @return <code>true</code> if item state was updated, <code>false</code> if the item has not been updated since
     *         <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the future, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean updatedSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.updatedSince(item, timestamp);
    }

    /**
     * Checks if the state of a given <code>item</code> has been updated since a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state was updated or <code>false</code> if the item has not been updated
     *         since <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the future, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean updatedSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.updatedSince(item, timestamp, serviceId);
    }

    /**
     * Checks if the state of a given <code>item</code> will be updated until a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item to check for state updates
     * @param timestamp the point in time to end the check
     * @return <code>true</code> if item state is updated, <code>false</code> if the item is not updated until
     *         <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the past, if the default
     *         persistence does not refer to a {@link QueryablePersistenceService}, or <code>null</code> if the default
     *         persistence service is not available
     */
    public static @Nullable Boolean updatedUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.updatedUntil(item, timestamp);
    }

    /**
     * Checks if the state of a given <code>item</code> will be updated until a certain point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to check for state changes
     * @param timestamp the point in time to end the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return <code>true</code> if item state was updated or <code>false</code> if the item is not updated
     *         since <code>timestamp</code>, <code>null</code> if <code>timestamp</code> is in the past, if the given
     *         <code>serviceId</code> does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable Boolean updatedUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.updatedUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> between two points in
     * time. The default persistence service is used.
     *
     * @param item the item to get the maximum state value for
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @return a {@link HistoricItem} with the maximum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the default persistence service
     *         does not refer to an available{@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.maximumBetween(item, begin, end);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> between two points in
     * time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the maximum state value for
     * @param begin the point in time to start the check
     * @param end the point in time to stop the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the maximum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the given <code>serviceId</code>
     *         does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.maximumBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> since
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to start the check
     * @return a historic item with the maximum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the future or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.maximumSince(item, timestamp);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> since
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to start the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the maximum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the future or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumSince(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.maximumSince(item, timestamp, serviceId);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> until
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to end the check
     * @return a historic item with the maximum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the past or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.maximumUntil(item, timestamp);
    }

    /**
     * Gets the historic item with the maximum value of the state of a given <code>item</code> until
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the maximum state value for
     * @param timestamp the point in time to end the check
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the maximum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         maximum value, <code>null</code> if <code>timestamp</code> is in the past or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem maximumUntil(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.maximumUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> between
     * two certain points in time. The default persistence service is used.
     *
     * @param item the item to get the minimum state value for
     * @param begin the beginning point in time
     * @param end the ending point in time to
     * @return a {@link HistoricItem} with the minimum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the default persistence service
     *         does not refer to an available{@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.minimumBetween(item, begin, end);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> between
     * two certain points in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the minimum state value for
     * @param begin the beginning point in time
     * @param end the end point in time to
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the minimum state value between two points in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if no persisted states found, or
     *         <code>null</code> if <code>begin</code> is after <code>end</end> or if the given <code>serviceId</code>
     *         does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumBetween(final Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.minimumBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> since
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time from which to search for the minimum state value
     * @return a historic item with the minimum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the future or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.minimumSince(item, timestamp);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> since
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time from which to search for the minimum state value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the minimum state value since the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the future or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumSince(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.minimumSince(item, timestamp, serviceId);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> until
     * a certain point in time. The default persistence service is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time to which to search for the minimum state value
     * @return a historic item with the minimum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the past or if the default
     *         persistence service does not refer to a {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.minimumUntil(item, timestamp);
    }

    /**
     * Gets the historic item with the minimum value of the state of a given <code>item</code> until
     * a certain point in time. The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the item to get the minimum state value for
     * @param timestamp the point in time to which to search for the minimum state value
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return a {@link HistoricItem} with the minimum state value until the given point in time, a
     *         {@link HistoricItem} constructed from the <code>item</code>'s state if <code>item</code>'s state is the
     *         minimum value, <code>null</code> if <code>timestamp</code> is in the past or if the given
     *         <code>serviceId</code> does not refer to an available {@link QueryablePersistenceService}
     */
    public static @Nullable HistoricItem minimumUntil(final Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.minimumUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the number of available data points of a given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the number of values persisted for this item, <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.countBetween(item, begin, end);
    }

    /**
     * Gets the number of available data points of a given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of values persisted for this item, <code>null</code> if <code>begin</code> is after
     *         <code>end</code>, if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.countBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the number of available historic data points of a given {@link Item} from a point in time until now.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the beginning point in time
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         future, if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.countSince(item, timestamp);
    }

    /**
     * Gets the number of available historic data points of a given {@link Item} from a point in time until now.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         future, if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countSince(Item item, ZonedDateTime begin, @Nullable String serviceId) {
        return PersistenceExtensions.countSince(item, begin, serviceId);
    }

    /**
     * Gets the number of available data points of a given {@link Item} from now to a point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         past, if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.countUntil(item, timestamp);
    }

    /**
     * Gets the number of available data points of a given {@link Item} from now to a point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of values persisted for this item, <code>null</code> if <code>timestamp</code> is in the
     *         past, if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        return PersistenceExtensions.countUntil(item, timestamp, serviceId);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} between two points in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @return the number of state changes for this item, <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        return PersistenceExtensions.countStateChangesBetween(item, begin, end);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} between two points in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param begin the beginning point in time
     * @param end the end point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of state changes for this item, <code>null</code>
     *         if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        return PersistenceExtensions.countStateChangesBetween(item, begin, end, serviceId);
    }

    /**
     * Gets the number of changes in historic data points of a given {@link Item} from a point in time until now.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the beginning point in time
     * @return the number of state changes for this item, <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.countStateChangesSince(item, timestamp);
    }

    /**
     * Gets the number of changes in historic data points of a given {@link Item} from a point in time until now.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the beginning point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of state changes for this item, <code>null</code>
     *         if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesSince(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.countStateChangesSince(item, timestamp, serviceId);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} from now until a point in time.
     * The default {@link PersistenceService} is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @return the number of state changes for this item, <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.countStateChangesUntil(item, timestamp);
    }

    /**
     * Gets the number of changes in data points of a given {@link Item} from now until a point in time.
     * The {@link PersistenceService} identified by the <code>serviceId</code> is used.
     *
     * @param item the {@link Item} to query
     * @param timestamp the ending point in time
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the number of state changes for this item, <code>null</code>
     *         if the persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Long countStateChangesUntil(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.countStateChangesUntil(item, timestamp, serviceId);
    }

    /**
     * Retrieves the historic items for a given <code>item</code> between two points in time.
     * The default persistence service is used.
     *
     * @param item the item for which to retrieve the historic item
     * @param begin the point in time from which to retrieve the states
     * @param end the point in time to which to retrieve the states
     * @return the historic items between the given points in time, or <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesBetween(Item item, ZonedDateTime begin,
            ZonedDateTime end) {
        return PersistenceExtensions.getAllStatesBetween(item, begin, end);
    }

    /**
     * Retrieves the historic items for a given <code>item</code> between two points in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the historic item
     * @param begin the point in time from which to retrieve the states
     * @param end the point in time to which to retrieve the states
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the historic items between the given points in time, or <code>null</code>
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesBetween(Item item, ZonedDateTime begin,
            ZonedDateTime end, @Nullable String serviceId) {
        return PersistenceExtensions.getAllStatesBetween(item, begin, end, serviceId);
    }

    /**
     * Retrieves the historic items for a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time from which to retrieve the states
     * @return the historic items since the given point in time, or <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     *
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesSince(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.getAllStatesSince(item, timestamp);
    }

    /**
     * Retrieves the historic items for a given <code>item</code> since a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the historic item
     * @param timestamp the point in time from which to retrieve the states
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the historic items since the given point in time, or <code>null</code>
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesSince(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.getAllStatesSince(item, timestamp, serviceId);
    }

    /**
     * Retrieves the future items for a given <code>item</code> until a certain point in time.
     * The default persistence service is used.
     *
     * @param item the item for which to retrieve the future item
     * @param timestamp the point in time to which to retrieve the states
     * @return the future items to the given point in time, or <code>null</code>
     *         if the default persistence service is not available or does not refer to a
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesUntil(Item item, ZonedDateTime timestamp) {
        return PersistenceExtensions.getAllStatesUntil(item, timestamp);
    }

    /**
     * Retrieves the future items for a given <code>item</code> until a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     *
     * @param item the item for which to retrieve the future item
     * @param timestamp the point in time to which to retrieve the states
     * @param serviceId the name of the {@link PersistenceService} to use
     * @return the future items to the given point in time, or <code>null</code>
     *         if the provided <code>serviceId</code> does not refer to an available
     *         {@link QueryablePersistenceService}
     */
    public static @Nullable Iterable<HistoricItem> getAllStatesUntil(Item item, ZonedDateTime timestamp,
            @Nullable String serviceId) {
        return PersistenceExtensions.getAllStatesUntil(item, timestamp, serviceId);
    }

    /**
     * Removes from persistence the historic items for a given <code>item</code> between two points in time.
     * The default persistence service is used.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param begin the point in time from which to remove the states
     * @param end the point in time to which to remove the states
     */
    public static void removeAllStatesBetween(Item item, ZonedDateTime begin, ZonedDateTime end) {
        PersistenceExtensions.removeAllStatesBetween(item, begin, end);
    }

    /**
     * Removes from persistence the historic items for a given <code>item</code> beetween two points in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param begin the point in time from which to remove the states
     * @param end the point in time to which to remove the states
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void removeAllStatesBetween(Item item, ZonedDateTime begin, ZonedDateTime end,
            @Nullable String serviceId) {
        PersistenceExtensions.removeAllStatesBetween(item, begin, end, serviceId);
    }

    /**
     * Removes from persistence the historic items for a given <code>item</code> since a certain point in time.
     * The default persistence service is used.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param timestamp the point in time from which to remove the states
     */
    public static void removeAllStatesSince(Item item, ZonedDateTime timestamp) {
        PersistenceExtensions.removeAllStatesSince(item, timestamp);
    }

   /**
     * Removes from persistence the historic items for a given <code>item</code> since a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the historic item
     * @param timestamp the point in time from which to remove the states
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void removeAllStatesSince(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        PersistenceExtensions.removeAllStatesSince(item, timestamp, serviceId);
    }

    /**
     * Removes from persistence the future items for a given <code>item</code> until a certain point in time.
     * The default persistence service is used.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the future item
     * @param timestamp the point in time to which to remove the states
     */
    public static void removeAllStatesUntil(Item item, ZonedDateTime timestamp) {
        PersistenceExtensions.removeAllStatesUntil(item, timestamp);
    }

    /**
     * Removes from persistence the future items for a given <code>item</code> until a certain point in time
     * through a {@link PersistenceService} identified by the <code>serviceId</code>.
     * This will only have effect if the p{@link PersistenceService} is a {@link ModifiablePersistenceService}.
     *
     * @param item the item for which to remove the future item
     * @param timestamp the point in time to which to remove the states
     * @param serviceId the name of the {@link PersistenceService} to use
     */
    public static void removeAllStatesUntil(Item item, ZonedDateTime timestamp, @Nullable String serviceId) {
        PersistenceExtensions.removeAllStatesUntil(item, timestamp, serviceId);
    }













    private static Map<String, Object> parseObjectArray(Object @Nullable [] objects) throws IllegalArgumentException {
        if (objects == null || objects.length == 0) {
            return Map.of();
        }
        if ((objects.length % 2) != 0) {
            throw new IllegalArgumentException("There must be an even number of objects (" + objects.length + ')');
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            if (objects[i] instanceof String key) {
                result.put(key, objects[i + 1]);
            } else {
                throw new IllegalArgumentException("Keys must be strings: " + objects[i]);
            }
        }
        return result;
    }
}
