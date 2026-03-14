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

import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Rule;
import org.openhab.core.converter.ObjectSerializer;

/**
 * {@link RuleSerializer} is the interface to implement by any file generator for {@link Rule} object.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public interface RuleSerializer extends ObjectSerializer<Rule> {

    /**
     * A container that holds the result of a serializability check.
     */
    public record SerializabilityResult(boolean ok, String failureReason) {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SerializabilityResult [ok=").append(ok);
            if (!ok) {
                sb.append(", failureReason=").append(failureReason);
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Checks if the specified rules are serializable with this {@link RuleSerializer}. Returned results are in the same
     * order as the specified rules, so avoid using an unordered collection if mapping a failure to a rule is desirable.
     *
     * @param rules the {@link List} of {@link Rule}s to check.
     * @return The resulting {@link List} of {@link SerializabilityResult}s.
     */
    List<SerializabilityResult> checkSerializability(Collection<Rule> rules);

    /**
     * Specify the {@link List} of {@link Rule}s to be serialized and associate them with an identifier.
     *
     * @param id the identifier of the {@link Rule} format generation.
     * @param rules the {@link List} of {@link Rule}s to serialize.
     * @param hideDefaultParameters {@code true} to hide the configuration parameters having a default value.
     */
    List<SerializabilityResult> setRulesToBeSerialized(String id, List<Rule> rules, boolean hideDefaultParameters); //TODO: (Nad) Is hide relevant?
}
