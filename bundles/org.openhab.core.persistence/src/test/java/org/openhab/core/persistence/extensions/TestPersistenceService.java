/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.persistence.extensions;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.PersistenceItemInfo;
import org.openhab.core.persistence.QueryablePersistenceService;
import org.openhab.core.persistence.strategy.PersistenceStrategy;
import org.openhab.core.types.State;

/**
 * A simple persistence service used for unit tests
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class TestPersistenceService implements QueryablePersistenceService {

    public static final String ID = "test";

    private final ItemRegistry itemRegistry;

    public TestPersistenceService(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void store(Item item) {
    }

    @Override
    public void store(Item item, @Nullable String alias) {
    }

    @Override
    public Iterable<HistoricItem> query(FilterCriteria filter) {
        if (PersistenceExtensionsTest.TEST_SWITCH.equals(filter.getItemName())) {
            ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES),
                    nowMinusFifteenHours = now.minusHours(15),
                    beginDate = filter.getBeginDate() != null ? filter.getBeginDate() : nowMinusFifteenHours,
                    endDate = filter.getEndDate() != null ? filter.getEndDate() : now;
            if (endDate.isBefore(beginDate)) {
                return List.of();
            }

            List<HistoricItem> results = new ArrayList<>(16);
            for (int i = 0; i <= 15; i++) {
                final int hours = i;
                final ZonedDateTime theDate = nowMinusFifteenHours.plusHours(hours);
                if (!theDate.isBefore(beginDate) && !theDate.isAfter(endDate)) {
                    results.add(new HistoricItem() {
                        @Override
                        public ZonedDateTime getTimestamp() {
                            return theDate;
                        }

                        @Override
                        public State getState() {
                            return OnOffType.from(hours < 5 || hours > 10);
                        }

                        @Override
                        public String getName() {
                            return Objects.requireNonNull(filter.getItemName());
                        }
                    });
                }
            }
            if (filter.getOrdering() == Ordering.DESCENDING) {
                Collections.reverse(results);
            }
            Stream<HistoricItem> stream = results.stream();
            if (filter.getPageNumber() > 0) {
                stream = stream.skip(filter.getPageSize() * filter.getPageNumber());
            }

            if (filter.getPageSize() != Integer.MAX_VALUE) {
                stream = stream.limit(filter.getPageSize());
            }
            return stream.toList();
        } else {
            int startValue = 1950;
            int endValue = 2012;

            if (filter.getBeginDate() != null) {
                startValue = filter.getBeginDate().getYear();
            }
            if (filter.getEndDate() != null) {
                endValue = filter.getEndDate().getYear();
            }

            if (endValue <= startValue || startValue < 1950) {
                return List.of();
            }

            List<HistoricItem> results = new ArrayList<>(endValue - startValue);
            for (int i = startValue; i <= endValue; i++) {
                final int year = i;
                results.add(new HistoricItem() {
                    @Override
                    public ZonedDateTime getTimestamp() {
                        return ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
                    }

                    @Override
                    public State getState() {
                        Item item = itemRegistry.get(Objects.requireNonNull(filter.getItemName()));
                        Unit<?> unit = item instanceof NumberItem ni ? ni.getUnit() : null;
                        return unit == null ? new DecimalType(year) : QuantityType.valueOf(year, unit);
                    }

                    @Override
                    public String getName() {
                        return Objects.requireNonNull(filter.getItemName());
                    }
                });
            }
            if (filter.getOrdering() == Ordering.DESCENDING) {
                Collections.reverse(results);
            }
            Stream<HistoricItem> stream = results.stream();
            if (filter.getPageNumber() > 0) {
                stream = stream.skip(filter.getPageSize() * filter.getPageNumber());
            }

            if (filter.getPageSize() != Integer.MAX_VALUE) {
                stream = stream.limit(filter.getPageSize());
            }
            return stream.toList();
        }
    }

    @Override
    public Set<PersistenceItemInfo> getItemInfo() {
        return Set.of();
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Test Label";
    }

    @Override
    public List<PersistenceStrategy> getDefaultStrategies() {
        return List.of();
    }
}
