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

import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;

import net.jini.discovery.LookupDiscovery;

import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.jini.JiniDriver;

import java.io.IOException;

import java.util.Dictionary;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
public class JiniServiceFactory implements org.osgi.framework.ServiceFactory,
    org.osgi.service.cm.ManagedService {
    static ServiceDiscoveryManager serviceDiscoveryManager = null;
    LookupDiscovery lookupDiscovery = null;
    String[] lusImportGroups = LookupDiscovery.ALL_GROUPS;

    /**
     * Creates a new JiniServiceFactory object.
     *
     * @throws Exception DOCUMENT ME!
     */
    public JiniServiceFactory() throws Exception {
        // Needed to spawn in order to find the right classes.
        Thread curThread = Thread.currentThread();
        ClassLoader oldClassLoader = curThread.getContextClassLoader();
        curThread.setContextClassLoader(Activator.class.getClassLoader());

        lookupDiscovery = new LookupDiscovery(getLusImportGroups());

        serviceDiscoveryManager = new ServiceDiscoveryManager(lookupDiscovery,
                null);

        ServiceTemplate registrarTemplate = new ServiceTemplate(null,
                new Class[] { ServiceRegistrar.class }, null);

        LookupCache registrarCache = serviceDiscoveryManager.createLookupCache(registrarTemplate,
                null, new Listener(ServiceRegistrar.class));

        curThread.setContextClassLoader(oldClassLoader);
        oldClassLoader = null;
        curThread = null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param bundle DOCUMENT ME!
     * @param registration DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        JiniDriverImpl jiniDriverImpl = new JiniDriverImpl();
        Debug.printDebugInfo(10,
            "JiniDriver Requested by " + bundle.getBundleId());

        return jiniDriverImpl;
    }

    /**
     * DOCUMENT ME!
     *
     * @param bundle DOCUMENT ME!
     * @param registration DOCUMENT ME!
     * @param service DOCUMENT ME!
     */
    public void ungetService(Bundle bundle, ServiceRegistration registration,
        Object service) {
        Debug.printDebugInfo(10,
            "JiniDriver Released by " + bundle.getBundleId());

        JiniDriverImpl jiniDriverImpl = (JiniDriverImpl) service;
        jiniDriverImpl.ungetServices();
    }

    /**
     * DOCUMENT ME!
     */
    public void terminate() {
        serviceDiscoveryManager.terminate();
        lookupDiscovery.terminate();
    }

    // When CM config changes
    public void updated(Dictionary props) {
        String[] exportGroups = (String[]) props.get(JiniDriver.CM_LUS_EXPORT_GROUPS);
        Osgi2Jini.setCMLusExportGroups(exportGroups);

        String[] importGroups = (String[]) props.get(JiniDriver.CM_LUS_IMPORT_GROUPS);
        importGroups = (importGroups != null) ? importGroups
                                              : LookupDiscovery.ALL_GROUPS;
        setLusImportGroups(importGroups);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String[] getLusImportGroups() {
        return lusImportGroups;
    }

    /**
     * DOCUMENT ME!
     *
     * @param lusImportGroups DOCUMENT ME!
     */
    public void setLusImportGroups(String[] lusImportGroups) {
        this.lusImportGroups = lusImportGroups;

        try {
            lookupDiscovery.setGroups(lusImportGroups);
        } catch (IOException ex) {
        }
    }
}
