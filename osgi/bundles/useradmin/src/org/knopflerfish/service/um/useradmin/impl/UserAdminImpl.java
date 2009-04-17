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

import java.security.AccessController;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.service.um.useradmin.Condition;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Implementation of UserAdmin.
 *
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class UserAdminImpl implements ServiceFactory, UserAdmin,
        ServiceListener {
    static final String ANYONE = "user.anyone";

    static final UserAdminPermission adminPermission;

    protected ServiceReference uasr; // our service ref

    protected Hashtable /* String -> RoleImpl */roles; // role db

    protected Vector /* ServiceReferences */listeners; // UserAdminListener:s

    RoleImpl anyone; // predefined role

    BundleContext bc;

    LogRef log;

    static {
        adminPermission = new UserAdminPermission(UserAdminPermission.ADMIN,
                null);
    }

    UserAdminImpl(BundleContext bc) {
        this.bc = bc;
        roles = new Hashtable();
        listeners = new Vector();
        log = new LogRef(bc);

        // create the special roles (no events for these)
        roles.put(ANYONE, anyone = new RoleImpl(ANYONE, this));
    }

    /**
     * Initialization method for the user admin. We must wait for the service
     * reference before we can check out UserAdminListeners.
     *
     * @param sr
     *            service reference for UserAdmin.
     */
    private void init(ServiceReference sr) {
        this.uasr = sr;
        // Listen for UserAdminListeners, collect those already registered
        try {
            String clazz = UserAdminListener.class.getName();
            bc.addServiceListener(this, "(objectClass=" + clazz + ")");
            ServiceReference[] srs = bc.getServiceReferences(clazz, null);
            if (srs != null) {
                for (int i = 0; i < srs.length; i++) {
                    listeners.addElement(srs[i]);
                    if (log.doDebug())
                        log.debug("UserAdminListener found: " + srs[i]);
                }
            }
        } catch (InvalidSyntaxException ex) {
        }

        if (log.doInfo())
            log.info("Service initialized", uasr);
    }

    /**
     * Sends an event to all user admin listeners.
     *
     * @param type
     *            the event type, one of <code>UserAdminEvent.ROLE_CHANGED
     * .ROLE_CREATED or .ROLE_REMOVED</code>.
     * @param role
     *            the role that the event is generated for.
     */
    void sendEvent(int type, Role role) {
        // dont send event if type = CHANGED and role has been removed
        if (!(type == UserAdminEvent.ROLE_CHANGED && roles.get(role.getName()) == null)) {
            UserAdminEvent event = new UserAdminEvent(uasr, type, role);
            for (Enumeration en = listeners.elements(); en.hasMoreElements();) {
                ServiceReference sr = (ServiceReference) en.nextElement();
                UserAdminListener ual = (UserAdminListener) bc.getService(sr);
                if (ual != null) {
                    ual.roleChanged(event);
                }
                bc.ungetService(sr);
            }
            if (log.doDebug())
                log.debug(event.toString(), uasr);
        } else {
            if (log.doDebug())
                log.debug("Event not sent, " + role.getName()
                        + " has been removed.", uasr);
        }
    }

    // - interface org.osgi.framework.ServiceFactory
    // ----------------------------
    public synchronized Object getService(Bundle bundle,
            ServiceRegistration registration) {
        // Factory is only used to be able to register and then later
        // get hold of our own ServiceReference before any bundle have
        // a chance to use the service.
        if (uasr == null) {
            // initialize ourself with the service reference the first time
            // the service is used by a bundle
            init(registration.getReference());
        }

        return this;
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration,
            java.lang.Object service) {
    }

    // - interface org.osgi.service.useradmin.UserAdmin
    // -------------------------
    public Role createRole(String name, int type) {
        SecurityManager sm = System.getSecurityManager();
        if (null!=sm) {
          sm.checkPermission(adminPermission);
        }
        if (roles.get(name) != null) {
            // role 'name' already exists, abort and return null
            return null;
        }

        Role role;
        switch (type) {
        case Role.USER:
            role = new UserImpl(name, this);
            break;
        case Role.GROUP:
            role = new GroupImpl(name, this);
            break;
        case Condition.CONDITION:
            role = new ConditionImpl(name, this);
            break;
        default:
            throw new IllegalArgumentException("UserAdminImpl: Unknown type '"
                    + type + "'.");
        }

        roles.put(name, role);
        sendEvent(UserAdminEvent.ROLE_CREATED, role);

        return role;
    }

    public boolean removeRole(String name) {
        SecurityManager sm = System.getSecurityManager();
        if (null!=sm) {
          sm.checkPermission(adminPermission);
        }

        if (ANYONE.equals(name)) {
            // not OK to remove the predefined role
            return false;
        }

        RoleImpl role = (RoleImpl) roles.remove(name);
        if (role != null) {
            role.remove();
            sendEvent(UserAdminEvent.ROLE_REMOVED, role);

            return true;
        }

        return false;
    }

    public Role getRole(String name) {
        return (Role) roles.get(name);
    }

    public Role[] getRoles(String filter) throws InvalidSyntaxException {
        Vector v = new Vector();
        for (Enumeration en = roles.elements(); en.hasMoreElements();) {
            RoleImpl role = (RoleImpl) en.nextElement();
            if (filter == null || LDAPQuery.query(filter, role.getProperties())) {
                v.addElement(role);
            }
        }

        Role[] result = new Role[v.size()];
        v.copyInto(result);

        return result;
    }

    public User getUser(String key, String value) {
        User[] users = getUsers(key, value);

        return users.length == 1 ? users[0] : null;
    }

    private User[] getUsers(String key, String value) {
        Vector found = new Vector();
        for (Enumeration en = roles.elements(); en.hasMoreElements();) {
            Object o = en.nextElement();
            if (o instanceof UserImpl) {
                UserImpl user = (UserImpl) o;
                Object val = user.getProperties().get(key);
                if (val instanceof String && value.equals(val)) {
                    found.addElement(user);
                }
            }
        }

        User[] result = new User[found.size()];
        found.copyInto(result);

        return result;
    }

    public Authorization getAuthorization(User user) {
        RoleImpl role = user != null ? (UserImpl) user : anyone;

        return new AuthorizationImpl(role, this);
    }

    // - interface org.osgi.framework.ServiceListener --------------------------
    public void serviceChanged(ServiceEvent event) {
        ServiceReference sr = event.getServiceReference();
        switch (event.getType()) {
        case ServiceEvent.REGISTERED:
            listeners.addElement(sr);
            if (log.doDebug())
                log.debug("UserAdminListener found: " + sr);
            break;
        case ServiceEvent.UNREGISTERING:
            if (listeners.removeElement(sr) && log.doDebug())
                log.debug("UserAdminListener gone: " + sr);
            break;
        }
    }

    // remove contents of roles
    protected void zap() {
        roles = new Hashtable();
        // create the special roles (no events for these)
        roles.put(ANYONE, anyone = new RoleImpl(ANYONE, this));
    }
}
