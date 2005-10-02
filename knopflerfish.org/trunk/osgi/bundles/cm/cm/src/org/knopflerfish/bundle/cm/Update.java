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

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

final class Update {
    final ServiceReference sr;

    final String pid;

    final String factoryPid;

    final ConfigurationDictionary configuration;

    ConfigurationDictionary processedConfiguration = null;

    public Update(ServiceReference sr, String pid, String factoryPid,
            ConfigurationDictionary configuration) {
        this.sr = sr;
        this.pid = pid;
        this.factoryPid = factoryPid;
        this.configuration = configuration;
    }

    public void doUpdate(PluginManager pm) throws ConfigurationException {
        if (sr == null) {
            return;
        }
        Object targetService = getTargetService();
        if (targetService == null) {
            return;
        }
        processedConfiguration = pm
                .callPluginsAndCreateACopy(sr, configuration);
        if (factoryPid == null) {
            update((ManagedService) targetService);
        } else {
            update((ManagedServiceFactory) targetService);
        }
    }

    private void update(ManagedService targetService)
            throws ConfigurationException {
        if (targetService == null) {
            return;
        }
        targetService.updated(processedConfiguration);
    }

    private void update(ManagedServiceFactory targetService)
            throws ConfigurationException {
        if (targetService == null) {
            return;
        }
        if (configuration == null) {
            targetService.deleted(pid);
        } else if (processedConfiguration == null) {
            if (Activator.r3TestCompliant()) {
                targetService.updated(pid, null);
            } else {
                targetService.deleted(pid);
            }
        } else {
            targetService.updated(pid, configuration);
        }
    }

    private Object getTargetService() {
        if (sr == null) {
            return null;
        }
        return Activator.bc.getService(sr);
    }
}
