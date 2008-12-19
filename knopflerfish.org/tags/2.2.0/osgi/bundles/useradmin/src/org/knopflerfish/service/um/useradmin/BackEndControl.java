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

/**
 * Service that can be used to load the user admin with contents from a back end
 * system, save the user admin's current contents to the back end system and get
 * the subscription for this service gateway from the back end system.
 * <p>
 * 
 * First and foremost, this service is an internal service that the bundle
 * um_ui_sg uses to communicate with the back-end. The interface is likely to
 * change in future releases of the user management component.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public interface BackEndControl {
    /**
     * Load the user admin with contents form the back end system. All data
     * currently in the user admin is overwritten.
     * 
     * @throws SecurityException
     *             If a security manager exists and the caller does not have the
     *             <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
     */
    void load() throws BackEndException;

    /**
     * Save the user admin's current contents to the back end system. All user
     * management data in the back end system for this gateway's subscription is
     * overwritten.
     * 
     * @throws SecurityException
     *             If a security manager exists and the caller does not have the
     *             <tt>UserAdminPermission</tt> with name <tt>admin</tt>.
     */
    void save() throws BackEndException;

    /**
     * Get the subscription for this service gateway.
     * 
     * @return the subscription for this service gateway.
     * @deprecated the subscription is now communicated as the service property
     *             "subscription", getSubscription still works though.
     */
    String getSubscription();
}
