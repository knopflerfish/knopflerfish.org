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

package org.knopflerfish.service.um.useradmin;

import java.util.Dictionary;

import org.osgi.service.useradmin.Authorization;

/**
 * This subclass to Authorization adds authentication context to the
 * authorization information. The authentication context is information about
 * how the user was authenticated. When checking the set of roles that the user
 * is authorized as ({@link org.osgi.service.useradmin.Authorization} getRoles
 * or hasRole), evaluation of Conditions can compare the value of context
 * parameters with the corresponding values in their filter expressions.
 * 
 */
public interface ContextualAuthorization extends Authorization {
    /**
     * Context parameter for authentication date, the parameter string is
     * <code>"auth_date"</code>. A value should be on the format
     * <code>yyyy-MM-dd</code>. This format makes it possible for example to
     * create a filter expression that evaluates to true when the authentication
     * date is between a start and end date. For example:
     * <code>(&amp;(auth_date&gt;=2001-06-01)(auth_date&lt;=2001-07-01))</code>
     */
    String CONTEXT_AUTH_DATE = "auth_date";

    /**
     * Context parameter for authentication time, the parameter string is
     * <code>"auth_time"</code>. A value should be on the format
     * <code>HH:mm:ss</code>, that is 24-hour with minutes and seconds.
     */
    String CONTEXT_AUTH_TIME = "auth_time";

    /**
     * Context parameter for authentication day of week, the parameter string is
     * <code>"auth_day"</code>. A value should be one of the days of the
     * week, in the environment's current locale.
     */
    String CONTEXT_AUTH_DAY = "auth_day";

    /**
     * Context parameter for current date, the parameter string is
     * <code>"date"</code>. A value should be on the format
     * <code>yyyy-MM-dd</code>.
     */
    String CONTEXT_DATE = "date";

    /**
     * Context parameter for current time, the parameter string is
     * <code>"time"</code>. A value should be on the format
     * <code>HH:mm:ss</code>, that is 24-hour with minutes and seconds.
     */
    String CONTEXT_TIME = "time";

    /**
     * Context parameter for current day of week, the parameter string is
     * <code>"day"</code>. A value should be one of the days of the week, in
     * the environment's current locale.
     */
    String CONTEXT_DAY = "day";

    /**
     * Context parameter for authentication level, the parameter string is
     * <code>"auth_lvl"</code>. Authentication level is a quality measurement
     * of the authentication method that was used. For example, authentication
     * with a PIN code should probably have a lower <code>auth_lvl</code> than
     * authentication with a finger print. The value is an integer between 0
     * (lowest) and 3 (highest). For example: <code>(auth_lvl>=2)</code>.
     */
    String CONTEXT_AUTH_LEVEL = "auth_lvl";

    /**
     * Context parameter for confidentiality level, the parameter string is
     * <code>"conf_lvl"</code>. Confidentiality level is a quality
     * measurement of the input path when the user was authenticated. How
     * difficult is it for some other party to eavesdrop? For example, a session
     * using HTTPS should have a higher <code>conf_lvl</code> than an ordinary
     * http session. The value is an integer between 0 (lowest) and 3 (highest).
     */
    String CONTEXT_CONF_LEVEL = "conf_lvl";

    /**
     * Context parameter for integrity level, the parameter string is
     * <code>"integr_lvl"</code>. Integrity level is a quality measurement of
     * the input path when the user was authenticated. Can data be trusted not
     * to be falsified? For example, a connection from a terminal in the local
     * home network should perhaps result in a higher <code>integr_lvl</code>
     * than a connection from a public terminal on the internet. The value is an
     * integer between 0 (lowest) and 3 (highest).
     */
    String CONTEXT_INTEGR_LEVEL = "integr_lvl";

    /**
     * Returns the authentication context for this authorization object. The
     * returned Dictionary can be modified to update the context.
     * 
     * @return the context
     */
    Dictionary getContext();

    /**
     * Set context parameter using IPAM. The supplied authentication method and
     * input path strings are translated to a set of context parameters.
     * 
     * @param authMethod
     *            authentication method
     * @param inputPath
     *            input path
     * @see org.knopflerfish.service.um.ipam.IPAMValuationService
     */
    void setIPAMContext(String authMethod, String inputPath);
}
