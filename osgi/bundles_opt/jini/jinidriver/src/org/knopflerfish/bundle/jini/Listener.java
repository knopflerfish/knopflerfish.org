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

import net.jini.core.lookup.ServiceItem;

import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryListener;

import org.osgi.framework.ServiceRegistration;

import org.osgi.service.jini.JiniDriver;

import java.util.Hashtable;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
class Listener implements ServiceDiscoveryListener {
    Hashtable services = new Hashtable();
    Class[] clazzes = null;

    /**
     * Creates a new Listener object.
     *
     * @param clazz DOCUMENT ME!
     */
    public Listener(Class clazz) {
        this(new Class[] { clazz });
    }

    /**
     * Creates a new Listener object.
     *
     * @param clazzes DOCUMENT ME!
     */
    public Listener(Class[] clazzes) {
        this.clazzes = clazzes;
    }

    /**
     * DOCUMENT ME!
     *
     * @param event DOCUMENT ME!
     */
    public void serviceAdded(ServiceDiscoveryEvent event) {
        ServiceItem item = event.getPostEventServiceItem();
        Object service = item.service;
        Hashtable prop = new Hashtable();
        prop.put(JiniDriver.ENTRIES, item.attributeSets);
        prop.put(JiniDriver.SERVICE_ID, item.serviceID.toString());
        Debug.printDebugInfo(10,
            "Registering Jini Service in OSGi Framework " +
            item.serviceID.toString());

        ServiceRegistration reg = Activator.bc.registerService(toStrings(),
                service, prop);
        services.put(service, reg);
        Debug.printDebugInfo(10,
            "Registering Jini Service in OSGi Framework Completed " +
            item.serviceID.toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @param event DOCUMENT ME!
     */
    public void serviceChanged(ServiceDiscoveryEvent event) {
        ServiceItem item = event.getPostEventServiceItem();
        Object service = item.service;
        Hashtable prop = new Hashtable();
        prop.put(JiniDriver.ENTRIES, item.attributeSets);
        prop.put(JiniDriver.SERVICE_ID, item.serviceID.toString());
        Debug.printDebugInfo(10,
            "Changing Jini Service properties in OSGi Framework " +
            item.serviceID.toString());

        ServiceRegistration reg = (ServiceRegistration) services.get(service);

        if (reg != null) {
            reg.setProperties(prop);
        } else {
            Debug.printDebugInfo(10, "Service is no longer in OSGi Framework");
        }

        Debug.printDebugInfo(10,
            "Changing Jini Service properties in OSGi Framework Completed" +
            item.serviceID.toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @param event DOCUMENT ME!
     */
    public void serviceRemoved(ServiceDiscoveryEvent event) {
        ServiceItem item = event.getPreEventServiceItem();
        Object service = item.service;
        Debug.printDebugInfo(10,
            "Unregistering Jini Service in OSGi Framework " +
            item.serviceID.toString());

        ServiceRegistration reg = (ServiceRegistration) services.remove(service);

        if (reg != null) {
            reg.unregister();
        } else {
            Debug.printDebugInfo(10,
                "Service Already Unregistered out OSGi Framework");
        }

        Debug.printDebugInfo(10,
            "Unregistering Jini Service in OSGi Framework Completed " +
            item.serviceID.toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private String[] toStrings() {
        String[] cl = new String[clazzes.length];

        for (int i = 0; i < cl.length; i++) {
            cl[i] = clazzes[i].getName();
        }

        return cl;
    }
}
