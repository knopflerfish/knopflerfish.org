/*
 * Copyright (c) 2003, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.service.um.useradmin.impl;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

import org.osgi.service.useradmin.Role;

/**
 * Implementation of Role.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
class RoleImpl implements Role, Serializable {

    Vector /* RoleImpl */basicMemberOf = new Vector();

    Vector /* RoleImpl */reqMemberOf = new Vector();

    protected String name;

    protected UAProperties props;

    RoleImpl(String name) {
        this.name = name;
        props = new UAProperties(this);
    }

    /**
     * Checks if this role implies the specified role.
     * 
     * @param roleName
     *            the role to check for
     * @param user
     *            the user that is being checked
     * @param context
     *            the context of the check
     * @param visited
     *            list of already examined roles, to detect loops in the db
     * 
     * @return true if this role implies the specified role.
     */
    boolean hasRole(String roleName, String user, Dictionary context,
            Vector visited) {
        // System.out.print( name + "-Role.hasRole roleName: " + roleName );
        // System.out.print( " user: " + user );
        // System.out.println( " visited: " + visited );

        // role always implies itself
        if (name.equals(roleName)) {
            return true;
        }

        // check if any basic parent has the role
        for (Enumeration en = basicMemberOf.elements(); en.hasMoreElements();) {
            RoleImpl parentGroup = (RoleImpl) en.nextElement();
            if (parentGroup.hasRole(roleName, user, context, visited)) {
                return true;
            }
        }

        // check if the predefined role has the role
        if (!name.equals(Role.USER_ANYONE))
            if (Activator.uai.anyone.hasRole(roleName, user, context, visited))
                return true;

        return false;
    }

    /**
     * Checks if the specified role is implied by this role.
     * 
     * @param user
     *            the user that is being checked
     * @param context
     *            the context of the check
     * @param visited
     *            list of already examined roles, to detect loops in the db
     * 
     * @return true if the specified role is a valid member of this role.
     */
    boolean hasMember(String user, Dictionary context, Vector visited) {
        // System.out.print( name + "-Role.hasMember user: " + user );
        // System.out.println( " visited: " + visited );

        if (name.equals(user) || name.equals(Role.USER_ANYONE)) {
            return true;
        }

        return false;
    }

    /**
     * Called to remove this role from the user database. This role is removed
     * as a member of any basic or required parent group.
     */
    void remove() {
        for (Enumeration en = basicMemberOf.elements(); en.hasMoreElements();) {
            GroupImpl parentGroup = (GroupImpl) en.nextElement();
            parentGroup.removeMember(this);
        }
        for (Enumeration en = reqMemberOf.elements(); en.hasMoreElements();) {
            GroupImpl parentGroup = (GroupImpl) en.nextElement();
            parentGroup.removeMember(this);
        }
    }

    public String toString() {
        return name;
    }

    // - interface org.osgi.service.useradmin.Role
    // ------------------------------
    public String getName() {
        return name;
    }

    public int getType() {
        return Role.ROLE;
    }

    public Dictionary getProperties() {
        return props;
    }

}
