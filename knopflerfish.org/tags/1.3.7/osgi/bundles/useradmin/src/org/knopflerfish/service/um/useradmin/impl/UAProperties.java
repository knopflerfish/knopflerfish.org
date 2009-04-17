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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Dictionary for user admin properties. Security checks for put.
 *
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class UAProperties extends Dictionary {
    protected RoleImpl role;

    protected Hashtable /* String -> byte[] or String */ht = new Hashtable();

    public UAProperties(RoleImpl role) {
        this.role = role;
    }

    public Enumeration elements() {
        Vector v = new Vector();
        for (Enumeration en = ht.keys(); en.hasMoreElements();) {
            try {
                v.addElement(get(en.nextElement()));
            } catch (SecurityException e) {
                // Ignore elements we don't have access to.
            }
        }
        return v.elements();
    }

    public Object get(Object key) {
        // No security check for properties
        Object value = ht.get(key);
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }

        return value;
    }

    public boolean isEmpty() {
        return ht.isEmpty();
    }

    public Enumeration keys() {
        return ht.keys();
    }

    public int size() {
        return ht.size();
    }

    public Object remove(Object key) {
        // synchronized (role) {
        if (key instanceof String) {
            SecurityManager sm = System.getSecurityManager();
            if (null!=sm) {
              sm.checkPermission
                  (new UserAdminPermission((String) key, getChangeAction()));
            }
            Object res = ht.remove(key);
            role.uai.sendEvent(UserAdminEvent.ROLE_CHANGED, role);
            // role.um.save();
            return res;
        }
        throw new IllegalArgumentException("The key must be a String, got "
                + key.getClass());
        // }
    }

    public Object put(Object key, Object value) {
        // synchronized (role) {
        if (key instanceof String) {
            SecurityManager sm = System.getSecurityManager();
            if (null!=sm) {
              sm.checkPermission
                  (new UserAdminPermission((String) key, getChangeAction()));
            }

            Object res;
            // value of type byte[] or String is ok
            if (value instanceof byte[]) {
                res = ht.put(key, ((byte[]) value).clone());
            } else if (value instanceof String) {
                res = ht.put(key, value);
            } else
                throw new IllegalArgumentException(
                        "The value must be of type byte[]"
                                + " or String,  got " + value.getClass());
            role.uai.sendEvent(UserAdminEvent.ROLE_CHANGED, role);
            // role.uai.save();

            return res;
        }
        throw new IllegalArgumentException("The key must be a String, got "
                + key.getClass());
        // }
    }

    public String toString() {
        // return ht.toString();
        return "#Properties#";
    }

    protected String getChangeAction() {
        return UserAdminPermission.CHANGE_PROPERTY;
    }

    protected Dictionary getUnderlyingDictionary() {
        return ht;
    }

    protected void setUnderlyingDictionary(Hashtable ht) {
        this.ht = ht;
    }

}
