/*
 * Copyright (c) 2009-2010, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;
import org.osgi.framework.hooks.service.*;

/**
 * Handle all framework hooks, mostly dispatched from BundleImpl, Services and ServiceListenerState
 *
 */
class Hooks {

  final private FrameworkContext fwCtx;
  ServiceTracker listenerHookTracker;

  boolean bOpen;


  Hooks(FrameworkContext fwCtx) {
    this.fwCtx = fwCtx;
  }


  /**
   *
   */
  synchronized void open() {
    if(fwCtx.debug.hooks) {
      fwCtx.debug.println("opening hooks");
    }

    listenerHookTracker = new ServiceTracker
      (fwCtx.systemBundle.bundleContext, 
       ListenerHook.class.getName(), 
       new ServiceTrackerCustomizer() {
         public Object addingService(ServiceReference reference) {           
           ListenerHook lh = (ListenerHook)fwCtx.systemBundle.bundleContext.getService(reference);
           try {
             lh.added(getServiceCollection());
           } catch (Exception e) {
             fwCtx.debug.printStackTrace("Failed to call listener hook  #" +
                                         reference.getProperty(Constants.SERVICE_ID), e);
           }
           return lh;
         }

         public void modifiedService(ServiceReference reference, Object service) {
           // noop
         }

         public void removedService(ServiceReference reference, Object service) {
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
                               Collection /*<ServiceReference>*/ refs) {
    ArrayList srl = fwCtx.services.get(FindHook.class.getName());
    if (srl != null) {
      RemoveOnlyCollection filtered = new RemoveOnlyCollection(refs);

      for (Iterator i = srl.iterator(); i.hasNext(); ) {
        ServiceReferenceImpl sr = ((ServiceRegistrationImpl)i.next()).reference;
        FindHook fh = (FindHook)sr.getService(fwCtx.systemBundle);
        if (fh != null) {
          try {
            fh.find(bc, service, filter, allServices, filtered);
          } catch (Exception e) {
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
                                   final Collection /*<ServiceListenerEntry>*/ receivers) {
    ArrayList srl = fwCtx.services.get(EventHook.class.getName());
    if (srl != null) {
      HashSet ctxs = new HashSet();
      for (Iterator ir = receivers.iterator(); ir.hasNext(); ) {
        ctxs.add(((ServiceListenerEntry)ir.next()).getBundleContext());
      }
      int start_size = ctxs.size();
      RemoveOnlyCollection filtered = new RemoveOnlyCollection(ctxs);
    
      for (Iterator i = srl.iterator(); i.hasNext(); ) {
        ServiceReferenceImpl sr = ((ServiceRegistrationImpl)i.next()).reference;
        EventHook eh = (EventHook)sr.getService(fwCtx.systemBundle);
        if (eh != null) {
          try {
            eh.event(evt, filtered);
          } catch (Exception e) {
            fwCtx.debug.printStackTrace("Failed to call event hook  #" +
                                        sr.getProperty(Constants.SERVICE_ID), e);
          }
        }
      }
      // NYI, refactor this for speed!?
      if (start_size != ctxs.size()) {
        for (Iterator ir = receivers.iterator(); ir.hasNext(); ) {
          if (!ctxs.contains(((ServiceListenerEntry)ir.next()).getBundleContext())) {
            ir.remove();
          }
        }
      }
    }
  }


  /**
   *
   */
  Collection getServiceCollection() {
    // TBD think about threads?!
    return Collections.unmodifiableSet(fwCtx.listeners.serviceListeners.serviceSet);
  }
  

  /**
   *
   */
  void handleServiceListenerReg(ServiceListenerEntry sle) {
    if(!isOpen() || listenerHookTracker.size() == 0) {
      return;
    }

    ServiceReference[] srl = listenerHookTracker.getServiceReferences();    
    
    Set set = toImmutableSet(sle);

    for (int i = 0; srl != null && i < srl.length; i++) {
      ListenerHook lh = (ListenerHook)listenerHookTracker.getService(srl[i]);
      try {
        lh.added(set);
      } catch (Exception e) {
        fwCtx.debug.printStackTrace("Failed to call listener hook #" +
                                    srl[i].getProperty(Constants.SERVICE_ID), e);
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
  void handleServiceListenerUnreg(Collection /* <ServiceListenerEntry> */ set) {
    if(!isOpen() || listenerHookTracker.size() == 0) {
      return;
    }
    ServiceReference[] srl = listenerHookTracker.getServiceReferences();    
    
    for (int i = 0; srl != null && i < srl.length; i++) {
      ListenerHook lh = (ListenerHook)listenerHookTracker.getService(srl[i]);
      try {
        lh.removed(set);
      } catch (Exception e) {
        fwCtx.debug.printStackTrace("Failed to call listener hook #" +
                                    srl[i].getProperty(Constants.SERVICE_ID), e);
      }
    }
  }


  /**
   *
   */
  static Set toImmutableSet(Object obj) {
    Set set = new HashSet();
    set.add(obj);
    set = Collections.unmodifiableSet(set);
    return set;
  }


  /**
   *
   */
  static class RemoveOnlyCollection extends AbstractCollection {
    
    final Collection org;
    public RemoveOnlyCollection(Collection values) {
      org = values;
    }
    
    public boolean add(Object obj) {
      throw new UnsupportedOperationException("objects can only be removed");
    }
    
    public boolean addAll(Collection objs) {
      throw new UnsupportedOperationException("objects can only be removed");
    }    

    public Iterator iterator() {
      return org.iterator();
    }
    
    public boolean remove(Object o) {
      return org.remove(o);
    }
    
    public int size() {
      return org.size();
    }
  }  
}
