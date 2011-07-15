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
import java.text.SimpleDateFormat;
import java.util.Dictionary;
import java.util.Vector;

import org.knopflerfish.service.um.useradmin.Condition;
import org.knopflerfish.service.um.useradmin.ContextualAuthorization;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Condition implementation.
 *
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class ConditionImpl extends RoleImpl implements Condition {
    private static final SimpleDateFormat date_format = new SimpleDateFormat(
            "yyyy-MM-dd");

    private static final SimpleDateFormat time_format = new SimpleDateFormat(
            "HH:mm:ss");

    private static final SimpleDateFormat day_format = new SimpleDateFormat(
            "EEEE");

    protected String filter;

    ConditionImpl(String name) {
        super(name);
    }

    boolean hasMember(String user, Dictionary context, Vector v) {
        // System.out.print( name + "-Condition.hasMember user: " + user );
        // System.out.print( " filter: " + filter );
        // System.out.print( " context: " + context );
        // System.out.println( " visited: " + v );
        if (filter == null) {
            return true;
        }

        if (context != null) {
            // add current time to context
            long now = System.currentTimeMillis();
            context.put(ContextualAuthorization.CONTEXT_DATE, date_format
                    .format(new Long(now)).toString());
            context.put(ContextualAuthorization.CONTEXT_TIME, time_format
                    .format(new Long(now)).toString());
            context.put(ContextualAuthorization.CONTEXT_DAY, day_format.format(
                    new Long(now)).toString());
            try {
                return LDAPQuery.query(filter, context);
            } catch (InvalidSyntaxException e) {
                Activator.log.error("Bad LDAP syntax: " + filter);
            }
        }

        return false;
    }

    public int getType() {
        return CONDITION;
    }

    // - interface org.osgi.service.useradmin.Condition
    // --------------------------
    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
      SecurityManager sm = System.getSecurityManager();
      if(null!=sm){
        sm.checkPermission(UserAdminImpl.adminPermission);
      }
      this.filter = filter;
    }

}
