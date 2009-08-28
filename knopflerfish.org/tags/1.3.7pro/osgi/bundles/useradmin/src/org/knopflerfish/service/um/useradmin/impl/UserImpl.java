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

import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Implementation of Role.
 *
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class UserImpl extends RoleImpl implements User {
    protected UACredentials creds;

    UserImpl(String name, UserAdminImpl uai) {
        super(name, uai);
        creds = new UACredentials(this);
    }

    public int getType() {
        return Role.USER;
    }

    // - interface org.osgi.service.useradmin.User
    // ------------------------------
    public Dictionary getCredentials() {
        return creds;
    }

    public boolean hasCredential(String key, Object value) {
        SecurityManager sm = System.getSecurityManager();
        if (null!=sm) {
            sm.checkPermission
              (new UserAdminPermission(key,UserAdminPermission.GET_CREDENTIAL));
        }
        Object val = creds.get(key);
        if (val instanceof byte[] && value instanceof byte[]) {
            return arraysEquals((byte[]) val, (byte[]) value);
        }
        if (val instanceof String && value instanceof String) {
            return val.equals(value);
        }

        return false;
    }

    // - private helper methods
    // -------------------------------------------------
    // pJava1.2 compliant substitue for Arrays.equals
    private boolean arraysEquals(byte[] a, byte[] b) {
        if (a.length == b.length) {
            for (int i = 0; i < a.length; i++) {
                if (a[i] != b[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
