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
import net.jini.core.lookup.ServiceTemplate;

import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryManager;

import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
public class JiniDriverImpl implements org.osgi.service.jini.JiniDriver {
    private static final long WAIT_TIME_MS = 1000;
    HashMap serviceCaches = new HashMap();

    /**
     * Creates a new JiniDriverImpl object.
     */
    JiniDriverImpl() {
    }

    /**
     * DOCUMENT ME!
     *
     * @param template DOCUMENT ME!
     */
    public void setServiceTemplates(ServiceTemplate[] template) {
        synchronized (this) {
            if (template != null) {
                // Building list for no longer needed services
                HashSet toRemoveCaches = new HashSet(serviceCaches.keySet());
                Iterator it = toRemoveCaches.iterator();
                List l = Arrays.asList(template);

                while (it.hasNext()) {
                    ServiceTemplate test = (ServiceTemplate) it.next();

                    if (l.contains(test)) {
                        toRemoveCaches.remove(test);
                    }
                }

                ungetServices(toRemoveCaches);

                // Building list for new needed services
                HashSet toAddCaches = new HashSet(l);
                Iterator it2 = toAddCaches.iterator();
                Set set = serviceCaches.keySet();

                while (it2.hasNext()) {
                    ServiceTemplate test = (ServiceTemplate) it2.next();

                    if (set.contains(test)) {
                        toAddCaches.remove(test);
                    }
                }

                getServices(toAddCaches);
            } else {
                Debug.printDebugInfo(10,
                    "Requested ServiceTemplates are void (null)");
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public ServiceTemplate[] getServiceTemplates() {
        synchronized (this) {
            return (serviceCaches.isEmpty()) ? null
                                             : (ServiceTemplate[]) serviceCaches.keySet()
                                                                                .toArray(new ServiceTemplate[0]);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param templates DOCUMENT ME!
     */
    void getServices(Set templates) {
        synchronized (this) {
            Iterator it = templates.iterator();

            while (it.hasNext()) {
                ServiceTemplate test = (ServiceTemplate) it.next();

                try {
                    Debug.printDebugInfo(10, "Creating LookupCache");

                    // Needed to spawn in order to find the right classes.
                    Thread curThread = Thread.currentThread();
                    ClassLoader oldClassLoader = curThread.getContextClassLoader();
                    curThread.setContextClassLoader(Activator.class.getClassLoader());

                    LookupCache lookupCache = JiniServiceFactory.serviceDiscoveryManager.createLookupCache(test,
                            null, new Listener(test.serviceTypes));
                    serviceCaches.put(test, lookupCache);

                    curThread.setContextClassLoader(oldClassLoader);
                    oldClassLoader = null;
                    curThread = null;

                    /* Registering a service into the frameworks takes some time,
                     * should wait untill that is finished. Wait a reasonable time.
                     */
                    Thread.sleep(JiniDriverImpl.WAIT_TIME_MS);
                    Debug.printDebugInfo(10, "Finished Creating LookupCache");
                } catch (RemoteException ex) {
                    Debug.printDebugInfo(10,
                        "Unable to create LookupCache for template " + test, ex);
                } catch (InterruptedException ex1) {
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    void ungetServices() {
        synchronized (this) {
            ungetServices(serviceCaches.keySet());
        }
    }

    /* List of templates for wich the services will be unregistered
     * and the cache will be terminated
     */
    void ungetServices(Set templates) {
        synchronized (this) {
            Iterator it = templates.iterator();

            while (it.hasNext()) {
                ServiceTemplate test = (ServiceTemplate) it.next();
                LookupCache lookupCache = (LookupCache) serviceCaches.remove(test);
                ServiceItem[] services = lookupCache.lookup(null,
                        Integer.MAX_VALUE);
                Debug.printDebugInfo(10,
                    "Services to discard (array object) " + services);

                for (int i = 0; i < services.length; i++) {
                    Debug.printDebugInfo(10,
                        "Discarding Service : " + services[i].service);
                    lookupCache.discard(services[i].service);
                }

                /* Terminating the cache should wait until the internal discardTask Thread
                 * gets the chance to send the events to the Listener. Wait a reasanble time.
                */
                try {
                    Thread.sleep(JiniDriverImpl.WAIT_TIME_MS);
                } catch (InterruptedException ex) {
                }

                lookupCache.terminate();

                Debug.printDebugInfo(10,
                    "LookupCache " + lookupCache + " terminated");
            }
        }
    }
}
