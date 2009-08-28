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

import java.text.SimpleDateFormat;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import org.knopflerfish.service.um.ipam.IPAMValuationService;
import org.knopflerfish.service.um.ipam.Levels;
import org.knopflerfish.service.um.useradmin.ContextualAuthorization;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Role;

/**
 * Authorization implementation.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public class AuthorizationImpl implements ContextualAuthorization {
    private static final String CONTEXT_DATE_FORMAT = "yyyy-MM-dd";

    private static final String CONTEXT_TIME_FORMAT = "HH:mm:ss";

    private static final String CONTEXT_DAY_FORMAT = "EEEE";

    protected RoleImpl user;

    protected Dictionary context;

    private UserAdminImpl uai;

    AuthorizationImpl(RoleImpl user, UserAdminImpl uai) {
        this.user = user;
        this.uai = uai;

        // Default context:
        context = new Hashtable();
        long now = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat(CONTEXT_DATE_FORMAT);
        context.put(CONTEXT_AUTH_DATE, format.format(new Long(now)).toString());
        format = new SimpleDateFormat(CONTEXT_TIME_FORMAT);
        context.put(CONTEXT_AUTH_TIME, format.format(new Long(now)).toString());
        format = new SimpleDateFormat(CONTEXT_DAY_FORMAT);
        context.put(CONTEXT_AUTH_DAY, format.format(new Long(now)).toString());
    }

    // - interface org.osgi.service.useradmin.Authorization
    // ---------------------
    public String getName() {
        if (user.getName().equals(UserAdminImpl.ANYONE))
            return null;

        return user.getName();
    }

    public boolean hasRole(String roleName) {
        return user.hasRole(roleName, user.getName(), context, new Vector());
    }

    public String[] getRoles() {
        // This is probably not the best implementation...
        Vector result = new Vector();
        try {
            Role[] roles = uai.getRoles(null);
            for (int i = 0; i < roles.length; i++) {
                if (hasRole(roles[i].getName())) {
                    result.addElement(roles[i].getName());
                }
            }
        } catch (InvalidSyntaxException ex) {
        }

        if (result.size() == 0)
            return null;

        String[] res = new String[result.size()];
        result.copyInto(res);
        return res;
    }

    // - interface org.knopflerfish.service.um.useradmin.ContextualAuthorization
    // --
    public void setIPAMContext(String inputPath, String authMethod) {
        int authLevel = Levels.LOWEST;
        int confLevel = Levels.LOWEST;
        int integrLevel = Levels.LOWEST;
        ServiceReference ipamsr = uai.bc
                .getServiceReference(IPAMValuationService.class.getName());
        if (ipamsr != null) {
            IPAMValuationService ipam = (IPAMValuationService) uai.bc
                    .getService(ipamsr);
            if (ipam != null) {
                Levels levels = ipam.getLevels(inputPath, authMethod);
                authLevel = levels.getAuthLevel();
                confLevel = levels.getConfLevel();
                integrLevel = levels.getIntegrLevel();
            } else {
                if (uai.log.doWarn())
                    uai.log.warn("IPAM service is not available. "
                            + "Using fallback IPAM context");
            }
            uai.bc.ungetService(ipamsr);
        } else {
            if (uai.log.doWarn())
                uai.log.warn("IPAM service is not available. "
                        + "Using fallback IPAM context");
        }
        context.put(CONTEXT_AUTH_LEVEL, new Integer(authLevel));
        context.put(CONTEXT_CONF_LEVEL, new Integer(confLevel));
        context.put(CONTEXT_INTEGR_LEVEL, new Integer(integrLevel));
    }

    public Dictionary getContext() {
        return context;
    }

}
