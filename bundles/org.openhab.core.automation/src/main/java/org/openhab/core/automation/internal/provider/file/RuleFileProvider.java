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
package org.openhab.core.automation.internal.provider.file;

import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;

/**
 * This class is implementation of {@link RuleProvider}. It extends functionality of {@link AbstractFileProvider}
 * for importing the {@link Rule}s from local files.
 *
 * @author xxx - Initial contribution
 */
@NonNullByDefault
public abstract class RuleFileProvider extends AbstractFileProvider<Rule> implements RuleProvider {

    public RuleFileProvider() { //TODO: (Nad) JavaDocs
        super("rules");
    }

    @Override
    protected String getUID(Rule providedObject) {
        return providedObject.getUID();
    }

    @Override
    public Collection<Rule> getAll() {
        return List.copyOf(providedObjectsHolder.values());
    }
}
