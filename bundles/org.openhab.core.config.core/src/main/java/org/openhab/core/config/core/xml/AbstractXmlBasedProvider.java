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
package org.openhab.core.config.core.xml;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;
import org.openhab.core.i18n.LocalizedKey;
import org.osgi.framework.Bundle;

/**
 * Common base class for XML based providers.
 *
 * @author Simon Kaufmann - Initial contribution, factored out of subclasses
 * @author Ravi Nadahar - Added support for listeners and notification
 *
 * @param <T_ID> the key type, e.g. ThingTypeUID, ChannelUID, URI,...
 * @param <T_OBJECT> the object type, e.g. ThingType, ChannelType, ConfigDescription,...
 */
@NonNullByDefault
public abstract class AbstractXmlBasedProvider<@NonNull T_ID, @NonNull T_OBJECT extends Identifiable<@NonNull T_ID>> {

    private final Map<Bundle, List<T_OBJECT>> bundleObjectMap = new ConcurrentHashMap<>(); // TODO: (Nad) JavaDocs

    private final Map<LocalizedKey, T_OBJECT> localizedObjectCache = new ConcurrentHashMap<>();

    private final List<WeakReference<@Nullable XmlBasedProviderListener<T_ID, T_OBJECT>>> listeners = new CopyOnWriteArrayList<WeakReference<@Nullable XmlBasedProviderListener<T_ID, T_OBJECT>>>();

    @Nullable
    private final ExecutorService executorService;

    /**
     * Creates a new instance without an {@link ExecutorService}.
     */
    public AbstractXmlBasedProvider() {
        this.executorService = null;
    }

    /**
     * Creates a new instance using the specified {@link ExecutorService}.
     *
     * @param executorService the {@link ExecutorService}.
     */
    public AbstractXmlBasedProvider(@Nullable ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Create a translated/localized copy of the given object.
     *
     * @param bundle the module to be used for the look-up of the translations
     * @param object the object to translate
     * @param locale the target locale
     * @return a translated copy of the given object or <code>null</code> if translation was not possible.
     */
    protected abstract @Nullable T_OBJECT localize(Bundle bundle, T_OBJECT object, @Nullable Locale locale);

    /**
     * Adds an object to the internal list associated with the specified module.
     * <p>
     * This method returns silently, if any of the parameters is {@code null}.
     *
     * @param bundle the module to which the object is to be added
     * @param object the object to be added
     */
    public final void add(Bundle bundle, T_OBJECT object) {
        addAll(bundle, List.of(object));
    }

    /**
     * Adds a {@link Collection} of objects to the internal list associated with the specified module.
     * <p>
     * This method returns silently, if any of the parameters is {@code null}.
     *
     * @param bundle the module to which the object is to be added
     * @param objectList the objects to be added
     */
    public final void addAll(Bundle bundle, Collection<T_OBJECT> objectList) {
        if (objectList.isEmpty()) {
            return;
        }
        List<T_OBJECT> objects;
        synchronized (this) {
            objects = Objects
                    .requireNonNull(bundleObjectMap.computeIfAbsent(bundle, k -> new CopyOnWriteArrayList<>()));
            objects.addAll(objectList);
            for (T_OBJECT object : objectList) {
                // just make sure no old entry remains in the cache
                removeCachedEntries(object);
            }
        }
        if (!listeners.isEmpty()) {
            for (T_OBJECT object : objects) {
                notifyListeners(bundle, object, true);
            }
        }
    }

    /**
     * Gets the object with the given key.
     *
     * @param key the key which identifies the object
     * @param locale the locale
     * @return the object if found, <code>null</code> otherwise
     */
    protected final @Nullable T_OBJECT get(T_ID key, @Nullable Locale locale) {
        for (Entry<Bundle, List<T_OBJECT>> objects : bundleObjectMap.entrySet()) {
            for (T_OBJECT object : objects.getValue()) {
                if (key.equals(object.getUID())) {
                    return acquireLocalizedObject(objects.getKey(), object, locale);
                }
            }
        }
        return null;
    }

    /**
     * Gets all available objects.
     *
     * @param locale the locale
     * @return a collection containing all available objects. Never <code>null</code>
     */
    protected final synchronized Collection<T_OBJECT> getAll(@Nullable Locale locale) {
        List<T_OBJECT> ret = new LinkedList<>();
        Collection<Entry<Bundle, List<T_OBJECT>>> objectList = bundleObjectMap.entrySet();
        for (Entry<Bundle, List<T_OBJECT>> objects : objectList) {
            for (T_OBJECT object : objects.getValue()) {
                ret.add(acquireLocalizedObject(objects.getKey(), object, locale));
            }
        }
        return ret;
    }

    /**
     * Removes all objects from the internal list associated with the specified module.
     * <p>
     * This method returns silently if the module is {@code null}.
     *
     * @param bundle the module for which all associated Thing types to be removed
     */
    public final void removeAll(Bundle bundle) {
        List<T_OBJECT> objects;
        synchronized (this) {
            objects = bundleObjectMap.remove(bundle);
            if (objects != null) {
                removeCachedEntries(objects);
            }
        }
        if (objects != null && !listeners.isEmpty()) {
            for (T_OBJECT object : objects) {
                notifyListeners(bundle, object, false);
            }
        }
    }

    /**
     * Registers a listener that will be notified of added and removed objects.
     *
     * @param listener the listener to register.
     */
    protected void addListener(XmlBasedProviderListener<T_ID, T_OBJECT> listener) {
        if (executorService == null) {
            throw new UnsupportedOperationException(getClass().getSimpleName() + " doesn't have an executor");
        }
        XmlBasedProviderListener<T_ID, T_OBJECT> tmpListener;
        boolean found = false;
        for (WeakReference<@Nullable XmlBasedProviderListener<T_ID, T_OBJECT>> ref : listeners) {
            tmpListener = ref.get();
            if (tmpListener == null) {
                listeners.remove(ref);
            } else if (tmpListener == listener) {
                found = true;
            }
        }
        if (!found) {
            listeners.add(new WeakReference<@Nullable XmlBasedProviderListener<T_ID,T_OBJECT>>(listener));
            // TODO: (Nad) Notify of existing objects - figure out localization
        }
    }

    /**
     * Unregisters a listener.
     *
     * @param listener the listener to unregister.
     */
    protected void removeListener(XmlBasedProviderListener<T_ID, T_OBJECT> listener) {
        XmlBasedProviderListener<T_ID, T_OBJECT> tmpListener;
        for (WeakReference<@Nullable XmlBasedProviderListener<T_ID, T_OBJECT>> ref : listeners) {
            tmpListener = ref.get();
            if (tmpListener == null || tmpListener == listener) {
                listeners.remove(ref);
            }
        }
    }

    /**
     * Notifies listeners of the added or removed bundle/object combination.
     *
     * @param bundle the {@link Bundle} to notify about.
     * @param object the object to notify about.
     * @param added {@code true} if it's an added event, {@code false} if it's a removed event.
     */
    protected void notifyListeners(Bundle bundle, T_OBJECT object, boolean added) {
        ExecutorService executor = executorService;
        if (executor == null) {
            return;
        }
        for (WeakReference<@Nullable XmlBasedProviderListener<T_ID, T_OBJECT>> ref : listeners) {
            XmlBasedProviderListener<T_ID, T_OBJECT> listener = ref.get();
            if (listener == null) {
                listeners.remove(ref);
            } else if (added) {
                executor.execute(() -> {
                    listener.added(bundle, object);
                });
            } else {
                executor.execute(() -> {
                    listener.removed(bundle, object);
                });
            }
        }
    }

    private void removeCachedEntries(List<T_OBJECT> objects) {
        for (T_OBJECT object : objects) {
            removeCachedEntries(object);
        }
    }

    private void removeCachedEntries(T_OBJECT object) {
        for (Iterator<Entry<LocalizedKey, T_OBJECT>> it = localizedObjectCache.entrySet().iterator(); it.hasNext();) {
            Entry<LocalizedKey, T_OBJECT> entry = it.next();
            if (entry.getKey().getKey().equals(object.getUID())) {
                it.remove();
            }
        }
    }

    private T_OBJECT acquireLocalizedObject(Bundle bundle, T_OBJECT object, @Nullable Locale locale) {
        final LocalizedKey localizedKey = getLocalizedKey(object, locale);

        final @Nullable T_OBJECT cacheEntry = localizedObjectCache.get(localizedKey);
        if (cacheEntry != null) {
            return cacheEntry;
        }

        final @Nullable T_OBJECT localizedObject = localize(bundle, object, locale);
        if (localizedObject != null) {
            T_OBJECT nonNullLocalizedObject = localizedObject;
            localizedObjectCache.put(localizedKey, nonNullLocalizedObject);
            return localizedObject;
        } else {
            return object;
        }
    }

    private LocalizedKey getLocalizedKey(T_OBJECT object, @Nullable Locale locale) {
        return new LocalizedKey(object.getUID(), locale != null ? locale.toLanguageTag() : null);
    }
}
