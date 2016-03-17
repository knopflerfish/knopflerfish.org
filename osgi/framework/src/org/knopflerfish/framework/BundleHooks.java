/*
 * Copyright (c) 2013-2016, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.bundle.EventHook;
import org.osgi.framework.hooks.bundle.FindHook;

class BundleHooks {

  final private FrameworkContext fwCtx;

  BundleHooks(FrameworkContext fwCtx) {
    this.fwCtx = fwCtx;
  }

  Bundle filterBundle(BundleContextImpl bc,
                      Bundle bundle) {
    if (bundle == null) {
      return bundle;
    }
    final List<ServiceRegistrationImpl<?>> srl = fwCtx.services.get(FindHook.class.getName());
    if (srl == null || srl.isEmpty()) {
      return bundle;
    } else {
      final ArrayList<Bundle> bl = new ArrayList<Bundle>();
      bl.add(bundle);
      filterBundles(bc, bl);
      return bl.isEmpty() ? null : bundle;
    }
  }

  void filterBundles(BundleContextImpl bc,
                     Collection<Bundle> bundles) {
    final List<ServiceRegistrationImpl<?>> srl = fwCtx.services.get(FindHook.class.getName());
    if (srl != null) {
      final RemoveOnlyCollection<Bundle> filtered
        = new RemoveOnlyCollection<Bundle>(bundles);

      for (final ServiceRegistrationImpl<?> serviceRegistrationImpl : srl) {
        final ServiceReferenceImpl<?> sr = serviceRegistrationImpl.reference;
        final FindHook fh = (FindHook) sr.getService();
        if (fh != null) {
          try {
            fh.find(bc, filtered);
          } catch (final Exception e) {
            fwCtx.frameworkError(bc,
                new BundleException("Failed to call Bundle FindHook  #" +
                                    sr.getProperty(Constants.SERVICE_ID), e));
          }
        }
      }
    }
  }

  void filterBundleEventReceivers(final BundleEvent evt,
                                   final HashSet<ListenerEntry> syncBundleListeners,
                                   final HashSet<ListenerEntry> bundleListeners) {

    final List<ServiceRegistrationImpl<?>> eventHooks
      = fwCtx.services.get(EventHook.class.getName());

    synchronized (fwCtx.listeners.syncBundleListeners) {
      syncBundleListeners.addAll(fwCtx.listeners.syncBundleListeners);
    }

    synchronized (fwCtx.listeners.bundleListeners) {
      if(bundleListeners != null) {
        bundleListeners.addAll(fwCtx.listeners.bundleListeners);
      }
    }

    if(eventHooks != null) {
      final HashSet<BundleContext> bundleContexts = new HashSet<BundleContext>();
      for (final ListenerEntry le : syncBundleListeners) {
        bundleContexts.add(le.bc);
      }
      if(bundleListeners != null) {
        for (final ListenerEntry le : bundleListeners) {
          bundleContexts.add(le.bc);
        }
      }

      final int unfilteredSize = bundleContexts.size();
      final RemoveOnlyCollection<BundleContext> filtered
        = new RemoveOnlyCollection<BundleContext>(bundleContexts);

      for (final ServiceRegistrationImpl<?> serviceRegistrationImpl : eventHooks) {
        final ServiceReferenceImpl<?> sr = serviceRegistrationImpl.reference;
        final EventHook eh = (EventHook)sr.getService();
        if (eh != null) {
          try {
            eh.event(evt, filtered);
          } catch (final Exception e) {
            fwCtx.frameworkError(fwCtx.systemBundle,
                                 new BundleException("Failed to call Bundle EventHook #" +
                                     sr.getProperty(Constants.SERVICE_ID), e));
          }
        }
      }

      if (unfilteredSize != bundleContexts.size()) {
        bundleContexts.add(fwCtx.systemBundle.bundleContext);
        for (Iterator<ListenerEntry> i = syncBundleListeners.iterator(); i.hasNext(); ) {
          ListenerEntry le = i.next();
          if(!bundleContexts.contains(le.bc)) {
            i.remove();
          }
        }
        if(bundleListeners != null) {
          for (Iterator<ListenerEntry> i = bundleListeners.iterator(); i.hasNext(); ) {
            ListenerEntry le = i.next();
            if(!bundleContexts.contains(le.bc)) {
              i.remove();
            }
          }
        }
      }
    }
  }
  
  void filterCollisions(int mode,
                        Bundle b,
                        Collection<Bundle> bundles) {
    final List<ServiceRegistrationImpl<?>> srl = fwCtx.services.get(CollisionHook.class.getName());
    if (srl != null) {
      final RemoveOnlyCollection<Bundle> filtered
        = new RemoveOnlyCollection<Bundle>(bundles);

      for (final ServiceRegistrationImpl<?> serviceRegistrationImpl : srl) {
        final ServiceReferenceImpl<?> sr = serviceRegistrationImpl.reference;
        final CollisionHook ch = (CollisionHook) sr.getService();
        if (ch != null) {
          try {
            ch.filterCollisions(mode, b, filtered);
          } catch (final Exception e) {
            fwCtx.frameworkError(b,
                new BundleException("Failed to call Bundle CollisionHook #" +
                                    sr.getProperty(Constants.SERVICE_ID), e));
          }
        }
      }
    }
  }

}
