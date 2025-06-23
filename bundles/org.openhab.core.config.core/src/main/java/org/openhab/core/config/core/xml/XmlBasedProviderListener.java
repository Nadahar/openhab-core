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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Identifiable;
import org.osgi.framework.Bundle;

/**
 * Implementations must be thread-safe.
 *
 * @author Ravi Nadahar - Initial contribution
 *
 * @param <T_ID> the key type, e.g. ThingTypeUID, ChannelUID, URI,...
 * @param <T_OBJECT> the object type, e.g. ThingType, ChannelType, ConfigDescription,...
 */
@NonNullByDefault
public interface XmlBasedProviderListener<T_ID, T_OBJECT extends Identifiable<@NonNull T_ID>> { // TODO: (Nad) JavaDocs

    void added(Bundle bundle, T_OBJECT object);

    void removed(Bundle bundle, T_OBJECT object);
}
