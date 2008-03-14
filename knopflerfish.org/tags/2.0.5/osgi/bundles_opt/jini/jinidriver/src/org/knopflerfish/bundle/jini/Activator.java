/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
package org.knopflerfish.bundle.jini;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import java.rmi.RMISecurityManager;

import java.util.Properties;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
public class Activator implements BundleActivator {
    public static BundleContext bc = null;
    private static ServiceRegistration jiniServiceFactory = null;
    Osgi2Jini O2J = null;
    JiniServiceFactory J2O = null;

    /**
     * DOCUMENT ME!
     *
     * @param context DOCUMENT ME!
     *
     * @throws BundleException DOCUMENT ME!
     */
    public void start(BundleContext context) throws BundleException {
        ensureSecurityManager();
        this.bc = context;

        try {
            O2J = new Osgi2Jini();
            O2J.open();
        } catch (Exception o2j) {
            Debug.printDebugInfo(10, "OSGi to Jini bridge not started", o2j);
        }

        try {
            J2O = new JiniServiceFactory();
            jiniServiceFactory = bc.registerService("org.osgi.service.jini.JiniDriver",
                    (ServiceFactory) J2O, null);
        } catch (Exception j2o) {
            Debug.printDebugInfo(10, "Jini to OSGi bridge not started", j2o);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param context DOCUMENT ME!
     *
     * @throws BundleException DOCUMENT ME!
     */
    public void stop(BundleContext context) throws BundleException {
        if (jiniServiceFactory != null) {
            jiniServiceFactory.unregister();
        }

        jiniServiceFactory = null;

        try {
            O2J.close();
            O2J = null;
        } catch (Exception o2j) {
            Debug.printDebugInfo(10, "OSGi to Jini bridge not stoped", o2j);
        }

        try {
            J2O.terminate();
            J2O = null;
        } catch (Exception j2o) {
            Debug.printDebugInfo(10, "Jini to OSGi bridge not stoped", j2o);
        }

        bc = null;
    }

    synchronized static void ensureSecurityManager() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }
}
