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
package org.openhab.core.automation.converter;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.converter.ObjectSerializer;

/**
 * {@link RuleTemplateSerializer} is the interface to implement by any file generator for {@link Rule} object.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface RuleTemplateSerializer extends ObjectSerializer<RuleTemplate> {

    /**
     * Specify the {@link List} of {@link RuleTemplate}s to be serialized and associate them with an identifier.
     *
     * @param id the identifier of the {@link RuleTemplate} format generation.
     * @param templates the {@link List} of {@link RuleTemplate}s to serialize.
     * @param hideDefaultParameters {@code true} to hide the configuration parameters having a default value.
     */
    void setTemplatesToBeSerialized(String id, List<RuleTemplate> templates, boolean hideDefaultParameters); //TODO: (Nad) Is hide relevant?
}
