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

import org.osgi.service.useradmin.Role;

/**
 * This interface represents a Condition that is managed by a
 * {@link org.osgi.service.useradmin.UserAdmin} service.
 * <p>
 * A Condition may have a filter associated with it. The condition is true if
 * the condition's filter evaluates to true with a context supplied to it by a
 * {@link ContextualAuthorization}.
 */

public interface Condition extends Role {
    /**
     * The type of a Condition role.
     * 
     * <p>
     * The value of <tt>CONDITION</tt> is -1.
     */
    int CONDITION = -1;

    /**
     * Get the filter
     * 
     * @return the filter
     */
    String getFilter();

    /**
     * Set the filter
     * 
     * @param filter
     *            what it should be set to
     * @throws SecurityException
     *             If a security manager exists and the caller does not have the
     *             <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
     */
    void setFilter(String filter);

}
