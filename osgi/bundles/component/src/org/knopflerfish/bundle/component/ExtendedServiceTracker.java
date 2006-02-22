/*
 * Copyright (c) 2006, KNOPFLERFISH project
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

/* This class works more or less like the org.osgi.framework.util.ServiceTracker
 * except that it adds a few extra "events". The code is partly based on the current
 * a in-house modification of the current osgi ServiceTracker.
 */
package org.knopflerfish.bundle.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;


class ExtendedServiceTracker implements ServiceListener {
  /* the filter */
  protected Filter filter;
  
  /* the current bundle context */
  protected BundleContext context;
  
  /* all tracked service references */
  private ArrayList tracking = new ArrayList();
  
  /* all service objects */
  private HashMap objects = new HashMap();
  
  ExtendedServiceTracker(BundleContext context, Filter filter) {
    this.context = context;
    this.filter = filter;
    
  }
  
  void open() {
    try {
      ServiceReference[] refs = 
        context.getServiceReferences(null, filter.toString());      
      context.addServiceListener(this, filter.toString());
      
      if (refs == null) {
        return ;
      }
      
      for (int i = 0; i < refs.length; i++) {
        serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, refs[i]));
      }
    } catch (InvalidSyntaxException e) {
      /* this can not happen, since we 
       * could have created the filter in that case 
       */
    }
  }
  
  void close() {
    context.removeServiceListener(this);
    try{
      synchronized (tracking) {
        for (Iterator iter = tracking.iterator(); iter.hasNext(); ) {
          context.ungetService((ServiceReference)iter.next());
        }
        
        tracking = new ArrayList();
        objects = new HashMap();
      }
    } catch (IllegalStateException e) {
      tracking = null;
      objects = null;
    }
    
  }

  public void serviceChanged(ServiceEvent event) {
    ServiceReference ref = event.getServiceReference();
    
    switch (event.getType()) {
    case ServiceEvent.REGISTERED: {
      Object cache = null;
      try {
        cache = addingService(ref);
      } catch(Throwable e) {}
      
      if (cache != null) {
        includeService(ref, cache);
      }
      
      addedService(ref, cache);
    // Should be safe to throw the exception 
     

    }; break;
    
    case ServiceEvent.UNREGISTERING: {
      Object object = objects.get(ref);
      if (object == null) {
        // in this case we have removed this reference from the tracker.
        return ;
      }
      
      try {
        removingService(ref, object);
      } catch (Throwable ignored) { }
      
      excludeService(ref);
      
      removedService(ref, object);
      
    }
    }
  }

  
  protected Object addingService(ServiceReference ref) {
    return context.getService(ref);    
  }
  
  protected void addedService(ServiceReference ref, Object service) {
    
  } 
  
  protected void removingService(ServiceReference ref, Object service) {
    
  }
  
  protected void removedService(ServiceReference ref, Object object) {
    context.ungetService(ref);
  }
  
  private void excludeService(ServiceReference ref) {
    synchronized (tracking) {
      tracking.remove(ref);
      objects.remove(ref);
    }
  }
  
  private void includeService(ServiceReference ref, Object cached) {
    synchronized(tracking) {
      if (tracking.isEmpty()) {
        tracking.add(0, ref);
        objects.put(ref, cached);
        return ;
      }
      
      Object tmp = ref.getProperty(Constants.SERVICE_RANKING);
      int refInt = (tmp instanceof Integer) ? ((Integer)tmp).intValue() : 0;
      Long refId = (Long)ref.getProperty(Constants.SERVICE_ID);

      int i = 0;
      for (int n = tracking.size(); i < n; i++) {
        ServiceReference challenger = (ServiceReference)tracking.get(i);
        tmp = challenger.getProperty(Constants.SERVICE_RANKING);
        int challengerInt = (tmp instanceof Integer) ? ((Integer)tmp).intValue() : 0;
        
        if (challengerInt < refInt) {
          break ;
        } else if (challengerInt == refInt) {
          Long challengerId = (Long)challenger.getProperty(Constants.SERVICE_ID);
          if (refId.compareTo(challengerId) < 0) {
            break;
          }
        } 
      }
      tracking.add(i, ref);
      objects.put(ref, cached);
    }
  }
  
  Object getService() {
    return tracking.isEmpty() ? null : objects.get(tracking.get(0));    
  }
  
  Object getService(ServiceReference ref) {
    return objects.get(ref);
  }
  
  ServiceReference getServiceReference() {
    return (ServiceReference) (tracking.isEmpty() ? null : tracking.get(0));
  }
  
  ServiceReference[] getServiceReferences() {
    synchronized (tracking) {
      int n = tracking.size();
      ServiceReference[] refs = new ServiceReference[n];
      
      for (int i = 0; i < n; i++) {
        refs[i] = (ServiceReference)tracking.get(i);
      }
      
      return refs;
    }   
  }
  
  Object[] getServices() {
    synchronized(tracking) {
      Object[] ret = new Object[objects.size()];
      int i = 0;
      for (Iterator iter = objects.values().iterator();
           iter.hasNext(); i++) {
        ret[i] = iter.next();
      }
      return ret;
    }   
  }
  
  void remove(ServiceReference ref) {
    serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref));
  }
  
  int size() {
    return tracking.size();
  }
}