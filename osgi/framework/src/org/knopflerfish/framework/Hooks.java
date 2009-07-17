/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
public class Hooks {
  FrameworkContext context;
  ServiceTracker listenerHookTracker;
  ServiceTracker findHookTracker;
  ServiceTracker eventHookTracker;

  boolean bOpen;

  Hooks(FrameworkContext context) {
    this.context = context;
  }

  synchronized void open() {
    if(context.props.debug.hooks) {
      context.props.debug.println("opening hooks");
    }

    listenerHookTracker = new ServiceTracker
      (context.systemBC, 
       ListenerHook.class.getName(), 
       new ServiceTrackerCustomizer() {
         public Object addingService(ServiceReference reference) {           
           ListenerHook lh = (ListenerHook)context.systemBC.getService(reference);
           try {
             lh.added(getServiceCollection());
           } catch (Exception e) {
             context.props.debug.printStackTrace("Failed to call listener hook  #" + reference.getProperty(Constants.SERVICE_ID), e);
           }
           return lh;
         }

         public void modifiedService(ServiceReference reference, Object service) {
           // noop
         }

         public void removedService(ServiceReference reference, Object service) {
           context.systemBC.ungetService(reference);
         }

       });



    findHookTracker  = new ServiceTracker(context.systemBC, FindHook.class.getName(), null);
    eventHookTracker = new ServiceTracker(context.systemBC, EventHook.class.getName(), null);

    listenerHookTracker.open();
    findHookTracker.open();
    eventHookTracker.open();

    bOpen = true;
  }

  synchronized void close() {
    listenerHookTracker.close();
    listenerHookTracker = null;

    findHookTracker.close();
    findHookTracker = null;

    eventHookTracker.close();
    eventHookTracker = null;

    bOpen = false;
  }


  synchronized public boolean isOpen() {
    return bOpen;
  }

  public ServiceReference filterServiceReference(BundleContext bc,
                                                 String service,
                                                 String filter,
                                                 boolean allServices,
                                                 ServiceReference sr) {
    if(!isOpen() || findHookTracker.size() == 0) {
      return sr;
    }
    
    Set set = new HashSet();
    set.add(sr);
    Collection r = filterServiceReferences(bc, service, filter, allServices, set);
    return r.size() == 0 ? null : (ServiceReference)r.iterator().next();
  }

  public Collection/*<ServiceReference>*/ filterServiceReferences(BundleContext bc,
                                                                  String service,
                                                                  String filter,
                                                                  boolean allServices,
                                                                  Collection /*<ServiceReference>*/ refs) {
    if(!isOpen() || findHookTracker.size() == 0) {
      return refs;
    }

    RemoveOnlySet      filtered = new RemoveOnlySet(refs);
    ServiceReference[] srl      = findHookTracker.getServiceReferences();    
    
    for(int i = srl.length-1; i >= 0; i--) {
      FindHook fh = (FindHook)findHookTracker.getService(srl[i]);      
      try {
        fh.find(bc, service, filter, allServices, filtered);
      } catch (Exception e) {
        context.props.debug.printStackTrace("Failed to call find hook  #" + srl[i].getProperty(Constants.SERVICE_ID), e);
      }

    }
    
    return filtered;
  }

  public Collection/*<Bundle>*/ filterServiceEventReceivers(final ServiceEvent evt, 
                                                            final Collection /*<Bundle>*/ receivers) {
    if(!isOpen() || eventHookTracker.size() == 0) {
      return receivers;
    }

    RemoveOnlySet      filtered = new RemoveOnlySet(receivers);
    ServiceReference[] srl      = eventHookTracker.getServiceReferences();    
    
    for(int i = srl.length-1; i >= 0; i--) {
      EventHook fh = (EventHook)eventHookTracker.getService(srl[i]);      
      try {
        fh.event(evt, filtered);
      } catch (Exception e) {
        context.props.debug.printStackTrace("Failed to call event hook  #" + srl[i].getProperty(Constants.SERVICE_ID), e);
      }
    }
    
    return filtered;
  }


  Collection getServiceCollection() {
    Set set = new HashSet();
    for(Iterator it = context.listeners.serviceListeners.serviceSet.iterator(); it.hasNext(); ) {
      ServiceListenerEntry sle = (ServiceListenerEntry)it.next();
      set.add(sle);
    }
    return Collections.unmodifiableSet(set);
  }
  

  void handleServiceListenerReg(ServiceListenerEntry sle) {
    if(!isOpen() || listenerHookTracker.size() == 0) {
      return;
    }

    ServiceReference[] srl = listenerHookTracker.getServiceReferences();    
    
    Set set = toImmutableSet(sle);

    for(int i = 0; srl != null && i < srl.length; i++) {
      ListenerHook lh = (ListenerHook)listenerHookTracker.getService(srl[i]);
      try {
        lh.added(set);
      } catch (Exception e) {
        context.props.debug.printStackTrace("Failed to call listener hook #" + srl[i].getProperty(Constants.SERVICE_ID), e);
      }

    }
  }

  void handleServiceListenerUnreg(ServiceListenerEntry sle) {
    if(!isOpen()) {
      return;
    }
    handleServiceListenerUnreg(toImmutableSet(sle));
  }

  void handleServiceListenerUnreg(Collection /* <ServiceListenerEntry sle> */ set) {
    if(!isOpen() || listenerHookTracker.size() == 0) {
      return;
    }
    ServiceReference[] srl = listenerHookTracker.getServiceReferences();    
    
    for(int i = 0; srl != null && i < srl.length; i++) {
      ListenerHook lh = (ListenerHook)listenerHookTracker.getService(srl[i]);
      try {
        lh.removed(set);
      } catch (Exception e) {
        context.props.debug.printStackTrace("Failed to call listener hook #" + srl[i].getProperty(Constants.SERVICE_ID), e);
      }
    }
  }

  static Set toImmutableSet(Object obj) {
    Set set = new HashSet();
    set.add(obj);
    set = Collections.unmodifiableSet(set);
    return set;
  }


  static class RemoveOnlySet extends LinkedHashSet {
    
    boolean bRemOnly = false;
    
    public RemoveOnlySet(Collection values) {
      super(values);
      bRemOnly = true;
    }
    
    public boolean add(Object obj) {
      if(bRemOnly) {
        throw new UnsupportedOperationException("objects can only be removed");
      } else {
        return super.add(obj);
      }
    }
    
    public boolean addAll(Collection objs) {
      if(bRemOnly) {
        throw new UnsupportedOperationException("objects can only be removed");
      } else {
        return super.addAll(objs);
      }
    }    
  }  
}
