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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ResolverHooks {

  private final FrameworkContext fwCtx;
  private ServiceTracker<ResolverHookFactory, TrackedResolverHookFactory> resolverHookTracker;

  private Set<TrackedResolverHookFactory> activeTrackedRHF = null;
  private List<ResolverHook> activeHooks = null;
  private BundleImpl [] currentTriggers = null;
  private Map<BundleGeneration, Boolean> resolvableBundles = null;
  private Thread blockThread = null;

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

  synchronized boolean isOpen() {
    return resolverHookTracker != null;
  }

  synchronized void beginResolve(final BundleImpl [] triggers) throws BundleException {
    if (!isOpen()) {
      return;
    }
    if (currentTriggers == null) {
      activeHooks = new ArrayList<ResolverHook>();
      activeTrackedRHF = new HashSet<TrackedResolverHookFactory>();
      Collection<BundleRevision> triggerCollection = new ArrayList<BundleRevision>();
      for (BundleImpl b : triggers) {
        triggerCollection.add(b.current().getBundleRevision());
      }
      for (TrackedResolverHookFactory trhf : resolverHookTracker.getTracked().values()) {
        if (trhf.isBlackListed()) {
          continue;
        }
        blockResolveForHooks();
        try {
          ResolverHook rh = trhf.begin(triggerCollection);
          if (rh != null) {
            activeTrackedRHF.add(trhf);
            activeHooks.add(rh);
          }
        } catch (RuntimeException re) {
          throw new BundleException("Exception during resolve hook begin",
                                    BundleException.REJECTED_BY_HOOK, re);
        } finally {
          unblockResolveForHooks();
        }
      }
      if (activeHooks.isEmpty()) {
        activeHooks = null;
        activeTrackedRHF = null;
      } else {
        currentTriggers = triggers;
        resolvableBundles = new HashMap<BundleGeneration, Boolean>();
      }
    }
  }


  synchronized void endResolve(final BundleImpl [] triggers) {
    if (triggers == currentTriggers) {
      blockResolveForHooks();
      for (ResolverHook rh : activeHooks) {
        try {
          rh.end();
        } catch (RuntimeException re) {
          // TODO, should we abort resolve?
          fwCtx.listeners.frameworkWarning(null, re);
        }
      }
      unblockResolveForHooks();
      currentTriggers = null;
      activeHooks = null;
      activeTrackedRHF = null;
      resolvableBundles = null;
    }
  }


  void filterMatches(BundleRequirement requirement , Collection<? extends BundleCapability> candidates) throws BundleException {
    if (hasHooks()) {
      @SuppressWarnings("unchecked")
      Collection<BundleCapability> c = new RemoveOnlyCollection<BundleCapability>((Collection<BundleCapability>) candidates);
      blockResolveForHooks();
      try {
        for (ResolverHook rh : activeHooks) {
          try {
            rh.filterMatches(requirement, c);
          } catch (RuntimeException re) {
            throw new BundleException("Resolver hook throw an exception",
                                      BundleException.REJECTED_BY_HOOK, re);
          }
        }
      } finally {
        unblockResolveForHooks();
      }
    }
  }


  boolean filterMatches(BundleRequirement requirement , BundleCapability candidate) throws BundleException {
    if (hasHooks()) {
      Collection<BundleCapability> c = new ShrinkableSingletonCollection<BundleCapability>(candidate);
      filterMatches(requirement, c);
      return !c.isEmpty();
    }
    return true;
  }


  boolean filterResolvable(BundleGeneration bg) throws BundleException {
    if (hasHooks()) {
      Boolean res = resolvableBundles.get(bg);
      if (res == null) {
        Collection<BundleRevision> c = new ShrinkableSingletonCollection<BundleRevision>(bg.getBundleRevision());
        blockResolveForHooks();
        try {
          for (ResolverHook rh : activeHooks) {
            try {
              rh.filterResolvable(c);
            } catch (RuntimeException re) {
              throw new BundleException("Resolver hook throw an exception",
                                        BundleException.REJECTED_BY_HOOK, re);
            }
          }
        } finally {
          unblockResolveForHooks();
        }
        res = Boolean.valueOf(!c.isEmpty());
        resolvableBundles.put(bg, res);
      }
      return res.booleanValue();
    }
    return true;
  }


  void filterSingletonCollisions(BundleCapability singleton , Collection<BundleCapability> candidates) throws BundleException {
    if (hasHooks()) {
      Collection<BundleCapability> c = new RemoveOnlyCollection<BundleCapability>(candidates);
      blockResolveForHooks();
      try {
        for (ResolverHook rh : activeHooks) {
          try {
            rh.filterSingletonCollisions(singleton, c);
          } catch (RuntimeException re) {
            throw new BundleException("Resolver hook throw an exception",
                                      BundleException.REJECTED_BY_HOOK, re);
          }
        }
      } finally {
        unblockResolveForHooks();
      }
    }
  }


  void checkResolveBlocked() {
    if (blockThread != null && Thread.currentThread() == blockThread) {
      throw new IllegalStateException("Resolve hooks aren't allowed to resolve bundle");
    }
  }
  boolean hasHooks() {
    return activeHooks != null;
  }

  private void blockResolveForHooks() {
    blockThread = Thread.currentThread();
  }

  private void unblockResolveForHooks() {
    blockThread = null;
  }

  static class TrackedResolverHookFactory implements ResolverHookFactory
  {
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


  static class ShrinkableSingletonCollection<T> extends AbstractCollection<T>
  {
    T singleton;
    
    public ShrinkableSingletonCollection(T singleton) {
      this.singleton = singleton;
    }

    @Override
    public boolean add(T br) {
      throw new UnsupportedOperationException("Add not allowed");
    }
  
    @Override
    public boolean addAll(Collection<? extends T> c) {
      throw new UnsupportedOperationException("Add not allowed");
    }
  
    @Override
    public boolean isEmpty() {
      return singleton == null;
    }
  
    @Override
    public Iterator<T> iterator() {
      return new Iterator<T>() {
        boolean hasNext = !isEmpty();
        
        @Override
        public boolean hasNext() {
          return hasNext;
        }

        @Override
        public T next() {
          if (hasNext) {
            hasNext = false;
            if (isEmpty()) {
              throw new ConcurrentModificationException();
            }
            return singleton;
          }
          throw new NoSuchElementException();
        }

        @Override
        public void remove() {
          if (hasNext || isEmpty()) {
            throw new IllegalStateException();
          }
          singleton = null;
        }
      };
    }
  
    @Override
    public boolean remove(Object o) {
      if (singleton != null && singleton.equals(o)) {
        singleton = null;
        return true;
      }
      return false;
    }
  
    @Override
    public int size() {
      return singleton != null ? 1 : 0;
    }
  
  }

}
