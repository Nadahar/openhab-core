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
package org.openhab.core.io.rest.core.internal.accessControl;

import java.util.Set;

import org.openhab.core.auth.Group;
import org.openhab.core.auth.Role;

public class AccessControl {

    Set<UserAccessControl> userAccessControlSet;
    // For the dependency it ok because org.openhab.core.io.rest.core depend of org.openhab.core.io.rest which depends
    // of org.openhab.core.
    Set<Group> groups;
    Set<Role> roles;

    public AccessControl(Set<UserAccessControl> userAccessControlSet, Set<Group> groups, Set<Role> roles) {
        this.userAccessControlSet = userAccessControlSet;
        this.groups = groups;
        this.roles = roles;
    }

    public Set<UserAccessControl> getUserAccessControlSet() {
        return userAccessControlSet;
    }

    public void setUserAccessControlSet(Set<UserAccessControl> userAccessControlSet) {
        this.userAccessControlSet = userAccessControlSet;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
