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

import org.osgi.service.useradmin.User;

/**
 * Interface for for a user name/password authentication session. First, a user
 * name and a password should be supplied, then it is possible to authenticate
 * the user or to get an Authorization object.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public interface PasswdSession {
    /**
     * Set the user's user name.
     * 
     * @param username
     *            the username
     */
    void setUsername(String username);

    /**
     * Set the user's password.
     * 
     * @param password
     *            the password
     */
    void setPassword(String password);

    /**
     * Attempts to authenticate. Useful if authorization information is not
     * required, see {@link #getAuthorization}.
     * 
     * @return the user, if a user admin service is available and there is a
     *         user with the supplied user name and the password matched.
     *         Otherwise null.
     * @exception IllegalStateException
     *                if called before a user name and a password have been
     *                supplied or if the user admin service is no longer
     *                available.
     */
    User authenticate() throws IllegalStateException;

    /**
     * Attempts to authenticate and authorize the user.
     * 
     * @return authorization information, or null if authentication failed.
     * @exception IllegalStateException
     *                if called before a user name and a password have been
     *                supplied or if the user admin service is no longer
     *                available.
     */
    ContextualAuthorization getAuthorization() throws IllegalStateException;
}
