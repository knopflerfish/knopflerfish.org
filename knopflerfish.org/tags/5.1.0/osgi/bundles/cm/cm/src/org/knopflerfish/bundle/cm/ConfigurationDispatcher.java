/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.util.Hashtable;

import org.osgi.framework.ServiceReference;

/**
 * * This class is responsible for dispatching configurations * to
 * ManagedService(Factories). * * It is also responsible for calling
 * <code>ConfigurationPlugins</code>. * *
 *
 * @author Per Gustafson *
 * @version 1.0
 */

final class ConfigurationDispatcher {
    /**
     * * The PluginManager to use.
     */

    private final PluginManager pm;

  private final boolean useSharedQueue = true;
  private final UpdateQueue sharedQueue;

    /**
     * * One queue per target service.
     */

    private final Hashtable<ServiceReference<?>, Object> serviceReferenceToTargetService
      = new Hashtable<ServiceReference<?>, Object>();

    private final Hashtable<Object, UpdateQueue> targetServiceToQueue
      = new Hashtable<Object, UpdateQueue>();

    /**
     * * Construct a ConfigurationDispatcher given a *
     * ConfigurationServicesTracker. * *
     *
     * @param tracker
     *            The ConfigurationServicesTracker to use.
     */

    ConfigurationDispatcher(PluginManager pm) {
        this.pm = pm;
      this.sharedQueue = useSharedQueue ? new UpdateQueue(pm) : null;
    }

    private UpdateQueue getQueueFor(ServiceReference<?> sr) {
      if(useSharedQueue) {
        return sharedQueue;
      }
        synchronized (targetServiceToQueue) {
            Object targetService = serviceReferenceToTargetService.get(sr);
            if (targetService == null) {
                return null;
            }
            return targetServiceToQueue.get(targetService);
        }
    }

    public void addQueueFor(ServiceReference<?> sr) {
      if(useSharedQueue) {
        return;
      }
        synchronized (targetServiceToQueue) {
            Object targetService = serviceReferenceToTargetService.get(sr);
            if (targetService == null) {
                targetService = Activator.bc.getService(sr);
                if (targetService == null) {
                    Activator.log
                            .error("Failed getting target service to build new queue for.");
                    return;
                }
                serviceReferenceToTargetService.put(sr, targetService);
            }
            if (!targetServiceToQueue.containsKey(targetService)) {
                targetServiceToQueue.put(targetService, new UpdateQueue(pm));
            }
        }
    }

    public void removeQueueFor(ServiceReference<?> sr) {
      if(useSharedQueue) {
        return;
      }
        synchronized (targetServiceToQueue) {
            Object targetService = serviceReferenceToTargetService.remove(sr);
            if (targetService == null) {
                Activator.log
                        .error("Missing target service for a ServiceReference in removeQueueFor(ServiceReference)");
            } else if (!serviceReferenceToTargetService.contains(targetService)) {
                UpdateQueue uq = targetServiceToQueue
                        .remove(targetService);
                if (uq == null) {
                    Activator.log
                            .error("Missing UpdateQueue for a ServiceReference in removeQueueFor(ServiceReference)");
                }
            }
        }
    }

    public void dispatchUpdateFor(ServiceReference<?> sr,
                                  String pid,
                                  String factoryPid,
                                  ConfigurationDictionary cd)
    {
      UpdateQueue uq = getQueueFor(sr);
      if (uq == null) {
        Activator.log.error("Missing UpdateQueue for " + pid + "/" + factoryPid);
        return;
      }
      Update u = new Update(sr, pid, factoryPid, cd);

      uq.enqueue(u);
    }
}
