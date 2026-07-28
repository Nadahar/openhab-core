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
package org.openhab.core.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.Registry;

/**
 * @author Nicolas Gennart
 */
@NonNullByDefault
public interface GroupRegistry extends Registry<Group, String> {

    /**
     * Update a group name to the registry if it exist.
     *
     * @param oldGroup to be replaced
     * @param newGroup to put
     */
    public void changeGroup(String oldGroup, String newGroup);

    /**
     * Add a group to the registry if it doesn't exist.
     *
     * @param group group to add.
     */
    public void addGroup(String group);

    /**
     * Remove the group in the registry if it exist.
     *
     * @param group group to remove
     */
    public void removeGroup(String group);

    /**
     * Add the role to the group in the registry, if the group exist and the role exist in the RoleRegistry.
     *
     * @param group that will receive a new role.
     * @param role to add to the specified group.
     */
    public void addRoleToGroup(String group, String role);

    /**
     * Remove the role to the group in the registry, if the group exist and the role exist too.
     *
     * @param group that will remove role.
     * @param role to remove to the specified group.
     */
    public void removeRoleToGroup(String group, String role);
}
