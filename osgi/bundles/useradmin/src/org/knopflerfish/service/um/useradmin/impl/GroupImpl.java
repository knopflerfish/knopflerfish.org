/*
 * Copyright (c) 2003-2007, KNOPFLERFISH project
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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * Implementation of Group.
 *
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class GroupImpl extends UserImpl implements Group {
    protected Vector<RoleImpl> basicMembers = new Vector<>();

    protected Vector<RoleImpl> reqMembers = new Vector<>();

    GroupImpl(String name) {
        super(name);
    }

    boolean hasRole(String roleName, String user, Dictionary<Object, Object> context,
            Vector<Group> visited) {

        // System.out.print( name + "-Group.hasRole roleName: " + roleName );
        // System.out.print( " user: " + user );
        // System.out.println( " visited: " + visited );

        if (visited.contains(this)) {
            // with Role.USER_ANYONE, we get loops as soon as
            // Role.USER_ANYONE is added to a group stop and return
            // false
            return false;
        }
        visited.addElement(this);

        for (Enumeration<RoleImpl> en = reqMembers.elements(); en.hasMoreElements();) {
            RoleImpl member = en.nextElement();
            if (!member.hasMember(user, context, new Vector<>())) {
                return false;
            }
        }

        return super.hasRole(roleName, user, context, visited);
    }

    boolean hasMember(String user, Dictionary<Object, Object> context, Vector<Group> visited) {

        // System.out.print( name + "-Group.hasMember user: " + user );
        // System.out.println( " visited: " + visited );

        // for the case where user is a group
        if (name.equals(user)) {
            return true;
        }

        if (visited.contains(this)) {
            return false;
            // throw new IllegalStateException( "UserAdmin database loops: " +
            // name + " points back to itself.");
        }
        visited.addElement(this);

        for (Enumeration<RoleImpl> en = reqMembers.elements(); en.hasMoreElements();) {
            RoleImpl member = en.nextElement();
            if (!member.hasMember(user, context, visited)) {
                return false;
            }
        }

        for (Enumeration<RoleImpl> en = basicMembers.elements(); en.hasMoreElements();) {
            RoleImpl member = en.nextElement();
            if (member.hasMember(user, context, visited)) {
                return true;
            }
        }

        return false;
    }

    void remove() {
        super.remove();
        for (Enumeration<RoleImpl> en = basicMembers.elements(); en.hasMoreElements();) {
            RoleImpl role = en.nextElement();
            role.basicMemberOf.removeElement(this);
        }
        for (Enumeration<RoleImpl> en = reqMembers.elements(); en.hasMoreElements();) {
            RoleImpl role = en.nextElement();
            role.reqMemberOf.removeElement(this);
        }

    }

    public int getType() {
        return Role.GROUP;
    }

    // - interface org.osgi.service.useradmin.Group
    // -----------------------------
    public boolean addMember(Role role) {
        SecurityManager sm = System.getSecurityManager();
        if (null != sm) {
            sm.checkPermission(UserAdminImpl.getAdminPermission());
        }

        RoleImpl roleImpl = (RoleImpl) role;
        if (basicMembers.contains(roleImpl)) {
            return false;
        }

        basicMembers.addElement(roleImpl);
        roleImpl.basicMemberOf.addElement(this);

        return true;
    }

    public boolean addRequiredMember(Role role) {
        SecurityManager sm = System.getSecurityManager();
        if (null != sm) {
            sm.checkPermission(UserAdminImpl.getAdminPermission());
        }

        RoleImpl roleImpl = (RoleImpl) role;
        if (reqMembers.contains(roleImpl)) {
            return false;
        }

        reqMembers.addElement(roleImpl);
        roleImpl.reqMemberOf.addElement(this);

        return true;
    }

    public boolean removeMember(Role role) {
        SecurityManager sm = System.getSecurityManager();
        if (null != sm) {
            sm.checkPermission(UserAdminImpl.getAdminPermission());
        }

        RoleImpl roleImpl = (RoleImpl) role;
        if (basicMembers.removeElement(roleImpl)) {
            roleImpl.basicMemberOf.removeElement(this);

            return true;
        }
        if (reqMembers.removeElement(roleImpl)) {
            roleImpl.reqMemberOf.removeElement(this);

            return true;
        }

        return false;
    }

    public Role[] getMembers() {
        Vector<RoleImpl> v = new Vector<>();
        for (Enumeration<RoleImpl> en = basicMembers.elements(); en.hasMoreElements();) {
            v.addElement(en.nextElement());
        }

        if (v.size() == 0) {
            return null;
        }

        Role[] result = new Role[v.size()];
        v.copyInto(result);
        return result;
    }

    public Role[] getRequiredMembers() {
        Vector<Role> v = new Vector<>();
        for (Enumeration<RoleImpl> en = reqMembers.elements(); en.hasMoreElements();) {
            v.addElement(en.nextElement());
        }

        if (v.size() == 0) {
            return null;
        }

        Role[] result = new Role[v.size()];
        v.copyInto(result);
        return result;
    }
}
