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

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;

import net.jini.discovery.LookupDiscovery;

import net.jini.lease.LeaseRenewalManager;

import net.jini.lookup.JoinManager;

import org.osgi.framework.ServiceReference;

import org.osgi.service.jini.JiniDriver;

import java.io.IOException;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
public class JiniExportedService implements net.jini.lookup.ServiceIDListener {
    //Class LeaseRenewalManger for all leases for this exportedService
    static LeaseRenewalManager leaseRenewalManager = new LeaseRenewalManager();

    // Data related to the service
    // OSGi
    ServiceReference serviceReference = null;
    Object service = null;

    // Jini
    ServiceID serviceID = null;
    Entry[] entries = null;

    //needed for the Jini JoinProtocl
    LookupDiscovery lookupDiscovery = null;
    JoinManager joinManager = null;

    //Changing state needs to be synchroniced, canceling or updating are distinct
    boolean isTerminating = false;

    /**
     * Creates a new JiniExportedService object.
     *
     * @param serviceReference DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public JiniExportedService(ServiceReference serviceReference)
        throws IOException {
        // get All serviceProperties and store them
        init(serviceReference);

        // Needed to spawn in order to find the right classes.
        Thread curThread = Thread.currentThread();
        ClassLoader oldClassLoader = curThread.getContextClassLoader();
        curThread.setContextClassLoader(Activator.class.getClassLoader());

        // creating a lookupDiscovery
        lookupDiscovery = new LookupDiscovery(getLusExportGroups());
        joinManager = new JoinManager(this.service, this.entries, this,
                lookupDiscovery, leaseRenewalManager);

        curThread.setContextClassLoader(oldClassLoader);
        oldClassLoader = null;
        curThread = null;
    }

    /**
     * DOCUMENT ME!
     */
    public void cancel() {
        synchronized (this) {
            if (isTerminating) {
                return;
            }

            isTerminating = true;
            joinManager.terminate();
            lookupDiscovery.terminate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param serviceReference DOCUMENT ME!
     */
    public void update(ServiceReference serviceReference) {
        synchronized (this) {
            if (isTerminating) {
                return;
            }

            init(serviceReference);

            try {
                lookupDiscovery.setGroups(getLusExportGroups());
                joinManager.setAttributes(entries);
            } catch (IOException ex) {
                Debug.printDebugInfo(10,
                    "Could not update " + serviceReference, ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param serviceReference DOCUMENT ME!
     */
    private void init(ServiceReference serviceReference) {
        // OSGi
        this.serviceReference = serviceReference;
        this.service = Activator.bc.getService(serviceReference);

        // Jini
        this.serviceID = Util.getServiceID((String) serviceReference.getProperty(
                    JiniDriver.SERVICE_ID));
        this.entries = (Entry[]) serviceReference.getProperty(JiniDriver.ENTRIES);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private String[] getLusExportGroups() {
        String[] service_lusExportGroups = (String[]) serviceReference.getProperty(JiniDriver.LUS_EXPORT_GROUPS);

        if (service_lusExportGroups != null) {
            return service_lusExportGroups;
        }

        String system_String_lusExportGroups = System.getProperty(JiniDriver.CM_LUS_EXPORT_GROUPS);

        if (system_String_lusExportGroups != null) {
            return Util.splitwords(system_String_lusExportGroups);
        }

        String[] cm_lusExportGroups = Osgi2Jini.getCMLusExportGroups();

        if (cm_lusExportGroups != null) {
            return cm_lusExportGroups;
        }

        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param serviceID DOCUMENT ME!
     */
    public void serviceIDNotify(ServiceID serviceID) {
        this.serviceID = serviceID;
    }
}
