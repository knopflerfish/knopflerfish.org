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

package org.knopflerfish.bundle.cm;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;

/**
 * * This class is responsible for managing ConfigurationPlugins. * *
 * 
 * @author Per Gustafson *
 * @version 1.0
 */

final class PluginManager {
    private final static String CM_RANKING = "service.cmRanking";

    private final static String CM_TARGET = ConfigurationPlugin.CM_TARGET;

    private final static String SERVICE_PID = Constants.SERVICE_PID;

    private final static String SERVICE_ID = Constants.SERVICE_ID;

    /**
     * * Sorted Vector of ServiceReferences to ConfigurationPlugins.
     */

    Vector preModificationPlugins = new Vector();

    /**
     * * Sorted Vector of ServiceReferences to ConfigurationPlugins.
     */

    Vector modifyingPlugins = new Vector();

    /**
     * * Sorted Vector of ServiceReferences to ConfigurationPlugins.
     */

    Vector postModificationPlugins = new Vector();

    /**
     * * Hashtable mapping a ServiceReference to its ranking (Integer).
     */

    Hashtable rankings = new Hashtable();

    /**
     * * Construct a PluginManager
     */

    PluginManager() {
    }

    /**
     * * Handle ConfigurationPlugin ServiceEvents. * *
     * 
     * @param serviceReference
     *            ServiceReference of the plugin the event concerns *
     * @param eventType
     *            Type of the event that caused the change
     */

    public synchronized void configurationPluginChanged(
            ServiceReference serviceReference, int eventType) {
        Object rankingProperty = serviceReference.getProperty(CM_RANKING);
        if (rankingProperty == null) {
            rankingProperty = new Integer(0);
        } else if (rankingProperty.getClass() != Integer.class) {
            rankingProperty = new Integer(0);
        }

        Long serviceId = (Long) serviceReference.getProperty(SERVICE_ID);
        if (serviceId == null) {
            Activator.log.error("Missing service id for a ConfigurationPlugin");
            return;
        }

        int ranking = ((Integer) rankingProperty).intValue();

        switch (eventType) {
        case ServiceEvent.REGISTERED:
            rankings.put(serviceId, rankingProperty);
            insertPluginReference(serviceReference, ranking);
            break;
        case ServiceEvent.MODIFIED:
            int oldRanking = ((Integer) rankings.get(serviceId)).intValue();
            if (ranking == oldRanking) {
                return;
            }
            removePluginReference(serviceId, oldRanking);
            rankings.put(serviceId, rankingProperty);
            insertPluginReference(serviceReference, ranking);
            break;
        case ServiceEvent.UNREGISTERING:
            rankings.remove(serviceId);
            removePluginReference(serviceId, ranking);
            break;
        default:
            break;
        }
    }

    /**
     * * Insert a ServiceReference to a ConfigurationPlugin in the correct *
     * Vector based on its ranking. * *
     * 
     * @param serviceReference
     *            The ServiceReference. *
     * @param ranking
     *            The ranking the ServiceReference.
     */

    private void insertPluginReference(ServiceReference serviceReference,
            int ranking) {
        if (ranking < 0) {
            insertPluginReference(serviceReference, ranking,
                    preModificationPlugins);
        } else if (0 <= ranking && ranking <= 1000) {
            insertPluginReference(serviceReference, ranking, modifyingPlugins);
        } else if (ranking > 1000) {
            insertPluginReference(serviceReference, ranking,
                    postModificationPlugins);
        } else {
            // Shouldn't happen
        }
    }

    /**
     * * Insert a ServiceReference in a Vector sorted on cm.ranking property. * *
     * 
     * @param serviceReference
     *            The ServiceReference. *
     * @param pluginsVector
     *            The vector.
     */

    private void insertPluginReference(ServiceReference serviceReference,
            int ranking, Vector pluginsVector) {
        int i;
        for (i = 0; i < pluginsVector.size(); ++i) {
            ServiceReference nextReference = (ServiceReference) pluginsVector
                    .elementAt(i);
            Long serviceId = (Long) nextReference.getProperty(SERVICE_ID);
            Integer rankingOfNextReference = (Integer) rankings.get(serviceId);
            if (ranking < rankingOfNextReference.intValue()) {
                break;
            }
        }
        pluginsVector.insertElementAt(serviceReference, i);
    }

    /**
     * * Remove a ServiceReference to a ConfigurationPlugin given * a service.id
     * and a ranking. * *
     * 
     * @param serviceId
     *            The service.id of the ConfigurationPlugin. *
     * @param ranking
     *            The ranking of the ConfigurationPlugin.
     */

    private void removePluginReference(Object serviceId, int ranking) {
        if (ranking < 0) {
            removePluginReference(serviceId, preModificationPlugins);
        } else if (0 <= ranking && ranking <= 1000) {
            removePluginReference(serviceId, modifyingPlugins);
        } else if (ranking > 1000) {
            removePluginReference(serviceId, postModificationPlugins);
        } else {
            // Shouldn't happen
        }
    }

    private void removePluginReference(Object serviceId, Vector pluginsVector) {
        for (int i = 0; i < pluginsVector.size(); ++i) {
            ServiceReference serviceReference = (ServiceReference) pluginsVector
                    .elementAt(i);
            Long currentId = (Long) serviceReference.getProperty(SERVICE_ID);
            if (currentId.equals(serviceId)) {
                pluginsVector.removeElementAt(i);
                return;
            }
        }
    }

    /**
     * * Call all applicable ConfigurationPlugins given a pid and a dictionary. * *
     * 
     * @param pid
     *            The pid of the target ManagedService(Factory). *
     * @param dictionary
     *            The configuration dictionary to be modified. * *
     * @return The description of what the method returns.
     */

    public synchronized ConfigurationDictionary callPluginsAndCreateACopy(
            ServiceReference targetServiceReference,
            ConfigurationDictionary dictionary) {
        if (targetServiceReference == null) {
            return dictionary;
        }
        if (dictionary == null) {
            return null;
        }
        callPlugins(targetServiceReference, dictionary, preModificationPlugins,
                false);

        dictionary = callPlugins(targetServiceReference, dictionary,
                modifyingPlugins, true);

        callPlugins(targetServiceReference, dictionary,
                postModificationPlugins, false);

        if (dictionary != null) {
            dictionary = dictionary.createCopyIfRealAndRemoveLocation();
        }
        return dictionary;
    }

    /**
     * * Call all plugins contained in a Vector and * optionally allow
     * modifications. * *
     * 
     * @param targetServiceReference
     *            Reference to the target ManagedService(Factory). *
     * @param dictionary
     *            The configuration dictionary to process. *
     * @param plugins
     *            Vector of references to ConfigurationPlugins. *
     * @param allowModification
     *            Should modifications to the configuration dictionary be
     *            allowed. * *
     * @return The modified configuration dictionary.
     */

    private ConfigurationDictionary callPlugins(
            ServiceReference targetServiceReference,
            ConfigurationDictionary dictionary, Vector plugins,
            boolean allowModification) {
        String pid = (String) targetServiceReference.getProperty(SERVICE_PID);
        ConfigurationDictionary currentDictionary = dictionary;
        Enumeration e = plugins.elements();
        while (e.hasMoreElements()) {
            ServiceReference pluginReference = (ServiceReference) e
                    .nextElement();

            // Only call the plugin if no cm.target is specified or if it
            // matches the pid of the target service
            String cmTarget = (String) pluginReference.getProperty(CM_TARGET);
            if (cmTarget == null || cmTarget.equals(pid)) {
                ConfigurationPlugin plugin = (ConfigurationPlugin) Activator.bc
                        .getService(pluginReference);
                if (plugin == null) {
                    continue;
                }
                ConfigurationDictionary dictionaryCopy = currentDictionary
                        .createCopyAndRemoveLocation();
                try {
                    plugin.modifyConfiguration(targetServiceReference,
                            dictionaryCopy);
                    if (allowModification && validateDictionary(dictionaryCopy)) {
                        currentDictionary = dictionaryCopy;
                    }
                } catch (Exception exception) {
                    Activator.log.error("[CM] Exception thrown by plugin: "
                            + exception.getMessage());
                }
            }
        }
        return currentDictionary;
    }

    /**
     * * Verify that a dictionary that has been modified by * a plugin still is
     * valid. * * Might restore some keys. * *
     * 
     * @param dictionary
     *            The dictionary to validate. * *
     * @return true if valid, false otherwise.
     */

    private boolean validateDictionary(Dictionary dictionary) {
        ConfigurationDictionary.validateDictionary(dictionary);
        return true;
    }
}
