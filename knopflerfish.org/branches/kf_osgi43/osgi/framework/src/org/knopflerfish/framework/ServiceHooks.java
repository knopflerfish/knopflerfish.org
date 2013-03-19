/*
 * Copyright (c) 2009-2013, KNOPFLERFISH project
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Handle all framework hooks, mostly dispatched from BundleImpl, Services
 * and ServiceListenerState
 *
 */
class ServiceHooks {

  final private FrameworkContext fwCtx;
  ServiceTracker<ListenerHook,ListenerHook> listenerHookTracker;

  boolean bOpen;

  // TDOD: OSGI43 Temporary solution while OSGi CT fail and don't clean up properly
  private static final HashSet<String> ignore = new HashSet<String>();
  static {
    ignore.add("org.osgi.test.cases.framework.junit.lifecycle.TestBundleControl");
    ignore.add("org.osgi.util.tracker.ServiceTracker$Tracked");
    ignore.add("aQute.launcher.Launcher");
  }
  private static boolean ignoreSLE(ServiceListenerEntry sle) {
    return ignore.contains(sle.listener.getClass().getName());
  }

  ServiceHooks(FrameworkContext fwCtx) {
    this.fwCtx = fwCtx;
  }


  /**
   *
   */
  synchronized void open() {
    if(fwCtx.debug.hooks) {
      fwCtx.debug.println("opening hooks");
    }

    listenerHookTracker = new ServiceTracker<ListenerHook,ListenerHook>
      (fwCtx.systemBundle.bundleContext,
       ListenerHook.class,
       new ServiceTrackerCustomizer<ListenerHook,ListenerHook>() {
         public ListenerHook addingService(ServiceReference<ListenerHook> reference) {
           final ListenerHook lh = fwCtx.systemBundle.bundleContext.getService(reference);
           try {
             Collection<ServiceListenerEntry> c = getServiceCollection();
             final ArrayList<ServiceListenerEntry> tmp = new ArrayList<ServiceListenerEntry>();
             for(final ServiceListenerEntry sle : c) {
               if(ignoreSLE(sle)) continue;
               tmp.add(sle);
             }
             c = tmp;
             @SuppressWarnings({ "rawtypes", "unchecked" })
             final Collection<ListenerInfo> li = (Collection) c;
             lh.added(li);
           } catch (final Exception e) {
             fwCtx.debug.printStackTrace("Failed to call listener hook  #" +
                                         reference.getProperty(Constants.SERVICE_ID), e);
           }
           return lh;
         }

         public void modifiedService(ServiceReference<ListenerHook> reference, ListenerHook service) {
           // noop
         }

         public void removedService(ServiceReference<ListenerHook> reference, ListenerHook service) {
           fwCtx.systemBundle.bundleContext.ungetService(reference);
         }

       });


    listenerHookTracker.open();

    bOpen = true;
  }


  /**
   *
   */
  synchronized void close() {
    listenerHookTracker.close();
    listenerHookTracker = null;

    bOpen = false;
  }


  /**
   *
   */
  synchronized public boolean isOpen() {
    return bOpen;
  }


  /**
   *
   */
  void filterServiceReferences(BundleContextImpl bc,
                               String service,
                               String filter,
                               boolean allServices,
                               Collection<ServiceReference<?>> refs)
  {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    final List<ServiceRegistrationImpl<FindHook>> srl
      = (List) fwCtx.services.get(FindHook.class.getName());
    if (srl != null) {
      final RemoveOnlyCollection<ServiceReference<?>> filtered
        = new RemoveOnlyCollection<ServiceReference<?>>(refs);

      for (final ServiceRegistrationImpl<FindHook> fhr : srl) {
        final ServiceReferenceImpl<FindHook> sr = fhr.reference;
        final FindHook fh = sr.getService(fwCtx.systemBundle);
        if (fh != null) {
          try {
            fh.find(bc, service, filter, allServices, filtered);
          } catch (final Exception e) {
            fwCtx.listeners.frameworkError(bc,
                new BundleException("Failed to call find hook  #" +
                                    sr.getProperty(Constants.SERVICE_ID), e));
          }
        }
      }
    }
  }


  /**
   *
   */
  void filterServiceEventReceivers(final ServiceEvent evt,
                                   final Collection<ServiceListenerEntry> receivers) {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    final List<ServiceRegistrationImpl<EventHook>> eventHooks
      = (List) fwCtx.services.get(EventHook.class.getName());

    if (eventHooks != null) {
      final HashSet<BundleContext> ctxs = new HashSet<BundleContext>();
      for (final ServiceListenerEntry sle : receivers) {
        ctxs.add(sle.getBundleContext());
      }
      final int start_size = ctxs.size();
      final RemoveOnlyCollection<BundleContext> filtered
        = new RemoveOnlyCollection<BundleContext>(ctxs);

      for (final ServiceRegistrationImpl<EventHook> sregi : eventHooks) {
        final ServiceReferenceImpl<EventHook> sr = sregi.reference;
        final EventHook eh = sr.getService(fwCtx.systemBundle);
        if (eh != null) {
          try {
            eh.event(evt, filtered);
          } catch (final Exception e) {
            fwCtx.debug.printStackTrace("Failed to call event hook  #" +
                                        sr.getProperty(Constants.SERVICE_ID), e);
          }
        }
      }
      // TODO, refactor this for speed!?
      if (start_size != ctxs.size()) {
        for (final Iterator<ServiceListenerEntry> ir = receivers.iterator(); ir.hasNext(); ) {
          if (!ctxs.contains(ir.next().getBundleContext())) {
            ir.remove();
          }
        }
      }
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    final List<ServiceRegistrationImpl<EventListenerHook>> eventListenerHooks
      = (List) fwCtx.services.get(EventListenerHook.class.getName());
    if (eventListenerHooks != null) {
      final HashMap<BundleContext, Collection<ListenerInfo>> listeners
        = new HashMap<BundleContext, Collection<ListenerInfo>>();

      for (final ServiceListenerEntry sle : receivers) {
        if(ignoreSLE(sle)) {
          // TODO: OSGI43 Temporary hack to work around poor clean up in test suite
          continue;
        }
        if(!listeners.containsKey(sle.getBundleContext())) {
          listeners.put(sle.getBundleContext(), new ArrayList<ListenerInfo>());
        }
        listeners.get(sle.getBundleContext()).add(sle);
      }

      for(final Entry<BundleContext, Collection<ListenerInfo>> e : listeners.entrySet()) {
        e.setValue(new RemoveOnlyCollection<ListenerInfo>(e.getValue()));
      }

      final RemoveOnlyMap<BundleContext, Collection<ListenerInfo>> filtered
        = new RemoveOnlyMap<BundleContext, Collection<ListenerInfo>>(listeners);

      for(final ServiceRegistrationImpl<EventListenerHook> sri : eventListenerHooks) {
        final EventListenerHook elh = sri.reference.getService(fwCtx.systemBundle);
        if(elh != null) {
          try {
            elh.event(evt, filtered);
          } catch(final Exception e) {
            fwCtx.debug.printStackTrace("Failed to call event hook  #" +
                sri.reference.getProperty(Constants.SERVICE_ID), e);
          }
        }
      }
      receivers.clear();
      for(final Collection<ListenerInfo> li : listeners.values()) {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final Collection<ServiceListenerEntry> sles = (Collection) li;
        receivers.addAll(sles);
      }
    }
  }


  /**
   *
   */
  Collection<ServiceListenerEntry> getServiceCollection() {
    // TODO think about threads?!
    return Collections.unmodifiableSet(fwCtx.listeners.serviceListeners.serviceSet);
  }


  /**
   *
   */
  void handleServiceListenerReg(ServiceListenerEntry sle) {
    if(!isOpen() || listenerHookTracker.size() == 0) {
      return;
    }
    if(ignoreSLE(sle)) {
      // TODO: OSGI43 Temporary hack to work around poor clean up in test suite
      return;
    }

    final ServiceReference<ListenerHook>[] srl
      = listenerHookTracker.getServiceReferences();

    final Set<ListenerInfo> set = toImmutableSet((ListenerInfo) sle);

    if (srl!=null) {
    for (final ServiceReference<ListenerHook> sr : srl) {
      final ListenerHook lh = listenerHookTracker.getService(sr);
      try {
        lh.added(set);
      } catch (final Exception e) {
        fwCtx.debug.printStackTrace("Failed to call listener hook #" +
                                    sr.getProperty(Constants.SERVICE_ID), e);
      }
      }

    }
  }


  /**
   *
   */
  void handleServiceListenerUnreg(ServiceListenerEntry sle) {
    if(isOpen()) {
      handleServiceListenerUnreg(toImmutableSet(sle));
    }
  }


  /**
   *
   */
  void handleServiceListenerUnreg(Collection<ServiceListenerEntry> set) {
    if(!isOpen() || listenerHookTracker.size() == 0) {
      return;
    }
    final ServiceReference<ListenerHook>[] srl
      = listenerHookTracker.getServiceReferences();

    if (srl != null) {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      final Collection<ListenerInfo> lis = (Collection) set;

      for (final ServiceReference<ListenerHook> sr : srl) {
        final ListenerHook lh = listenerHookTracker.getService(sr);
        try {
          lh.removed(lis);
        } catch (final Exception e) {
          fwCtx.debug
              .printStackTrace("Failed to call listener hook #"
                                   + sr.getProperty(Constants.SERVICE_ID),
                               e);
        }
      }
    }
  }

  /**
   *
   */
  static <E> Set<E> toImmutableSet(E obj) {
    Set<E> set = new HashSet<E>();
    set.add(obj);
    set = Collections.unmodifiableSet(set);
    return set;
  }


  static class RemoveOnlyMap<K,V> implements Map<K,V> {
    final Map<K,V> original;
    public RemoveOnlyMap(Map<K,V> original) {
      this.original = original;
    }

    public void clear() {
      original.clear();
    }

    public boolean containsKey(Object k) {
      return original.containsKey(k);
    }


    public boolean containsValue(Object v) {
      return original.containsValue(v);
    }

    public Set<Entry<K,V>> entrySet() {
      return original.entrySet();
    }

    public V get(Object k) {
      return original.get(k);
    }

    public boolean isEmpty() {
      return original.isEmpty();
    }

    public Set<K> keySet() {
      return original.keySet();
    }

    public V put(Object k, Object v) {
      throw new UnsupportedOperationException("objects can only be removed");
    }

    public void putAll(Map<? extends K,? extends V> m) {
      throw new UnsupportedOperationException("objects can only be removed");
    }

    public V remove(Object k) {
      return original.remove(k);
    }

    public int size() {
      return original.size();
    }

    public Collection<V> values() {
      return original.values();
    }

  }

  /*
  void printSLE(String pre, Collection c, String post) {
    if(pre != null) {
      System.out.println(pre);
      System.out.flush();
    }
    for(Object o : c) {
      ServiceListenerEntry sle = (ServiceListenerEntry)o;
      System.out.println("SLE: " + sle.listener.getClass().getName() + "@" + sle.getFilter());
      System.out.flush();
    }
    if(post != null) {
      System.out.println(post);
      System.out.flush();
    }
  }

  void printSLE(String pre, Map m, String post) {
    System.out.println(pre);
    System.out.flush();
    for(Object o : m.values()) {
      Collection c = (Collection)o;
      printSLE(null, c, null);
    }
    System.out.println(post);
    System.out.flush();
  }
*/
}
