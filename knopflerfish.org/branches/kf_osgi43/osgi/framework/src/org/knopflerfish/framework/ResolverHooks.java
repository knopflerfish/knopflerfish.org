/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ResolverHooks {

  private final FrameworkContext fwCtx;
  private ServiceTracker<ResolverHookFactory, TrackedResolverHookFactory> resolverHookTracker;

  private SortedMap<ServiceReference<ResolverHookFactory>, TrackedResolverHookFactory> activeHooks;

  ResolverHooks(FrameworkContext frameworkContext) {
    this.fwCtx = frameworkContext;
  }

  synchronized void open() {
    if (fwCtx.debug.hooks) {
      fwCtx.debug.println("Begin Tracking Resolver Hooks");
    }

    resolverHookTracker = new ServiceTracker<ResolverHookFactory, TrackedResolverHookFactory>(
        (BundleContext) fwCtx.systemBundle.bundleContext, ResolverHookFactory.class,
        new ServiceTrackerCustomizer<ResolverHookFactory, TrackedResolverHookFactory>() {
          public TrackedResolverHookFactory addingService(
              ServiceReference<ResolverHookFactory> reference) {
            return new TrackedResolverHookFactory(
                (ResolverHookFactory) fwCtx.systemBundle.bundleContext
                    .getService(reference), reference);
          }

          public void modifiedService(ServiceReference<ResolverHookFactory> reference,
              TrackedResolverHookFactory service) {

          }

          public void removedService(ServiceReference<ResolverHookFactory> reference,
              TrackedResolverHookFactory service) {
          }
        });

    resolverHookTracker.open();
  }

  synchronized void close() {
    resolverHookTracker.close();
    resolverHookTracker = null;
  }

  synchronized public boolean isOpen() {
    return resolverHookTracker != null;
  }

  synchronized List<ResolverHook> beginResolve(final Collection<BundleRevision> triggers) throws Exception {
    if (!isOpen()) {
      return null;
    }
    if (activeHooks != null) {
      throw new IllegalStateException("Resolve in progress");
    }
    List<ResolverHook> res = new ArrayList<ResolverHook>();
    try {
      activeHooks = resolverHookTracker.getTracked();

      for (Iterator<TrackedResolverHookFactory> i = activeHooks.values().iterator(); i.hasNext();) {
        TrackedResolverHookFactory trhf = i.next();
        if (trhf.isBlackListed()) {
          continue;
        }
        ResolverHook rh = trhf.begin(triggers);
        if (rh != null) {
          res.add(rh);
        } else {
          i.remove();
        }
      }
    } finally {
    }
    return res;
  }

  synchronized void endResolve() {

    for (TrackedResolverHookFactory trhf : activeHooks.values()) {
    }
    activeHooks = null;
  }

  static class TrackedResolverHookFactory implements ResolverHookFactory {
    final ResolverHookFactory tracked;
    final ServiceReference<ResolverHookFactory> reference;
    boolean blacklisted = false;

    TrackedResolverHookFactory(ResolverHookFactory tracked,
        ServiceReference<ResolverHookFactory> reference) {
      this.tracked = tracked;
      this.reference = reference;
    }

    public ResolverHook begin(final Collection<BundleRevision> triggers) {
      return tracked.begin(triggers);
    }

    void blacklist() {
      blacklisted = true;
    }

    boolean isBlackListed() {
      return blacklisted;
    }
  }

}
