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
package org.knopflerfish.bundle.component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

public class Reference extends ServiceTracker {
  
  private boolean optional;
  private boolean multiple;
  private boolean dynamic;

  private String bindMethodName;
  private String unbindMethodName;
  private String refName;
  private BundleContext bc;
  
  private ServiceReference bound;
  private Collection instances = new ArrayList();
  
  private Config config;
  
  public Reference(String refName, Filter filter,
                   boolean optional, boolean multiple, boolean dynamic,
                   String bindMethodName, String unbindMethodName,
                   BundleContext bc) {

    super(bc, filter, null);
    this.refName = refName;
    this.optional = optional;
    this.multiple = multiple;
    this.dynamic = dynamic;
    this.bindMethodName = bindMethodName;
    this.unbindMethodName = unbindMethodName;
    this.bc = bc;
  }

  public void setConfig(Config config) {
    this.config = config;
  }
  
  public Object addingService(ServiceReference ref, Object service) {
    boolean wasSatisfied = isSatisfied();
    if (bound != null && multiple) {
      for (Iterator iter = instances.iterator(); iter.hasNext();) {
        invokeEventMethod(iter.next(), bindMethodName, ref);
      }
    }
    Object obj = super.addingService(ref);
    if (!wasSatisfied && isSatisfied() && config != null) {
      config.referenceSatisfied();
    }
    return obj;
  }

  /* TODO? public void modifiedService() */

  public void removedService(ServiceReference ref, Object service) {
    boolean wasSatisfied = isSatisfied();
    if (bound != null) {
      for (Iterator iter = instances.iterator(); iter.hasNext();) {
        invokeEventMethod(iter.next(), unbindMethodName, ref);
      }
    }
    super.removedService(ref, service);
    /* try to remove this service,
       possibly disabling the component */
    if (wasSatisfied && !isSatisfied() && config != null) {
      config.referenceUnsatisfied();
    }
  }

  public boolean isSatisfied() {
    return getTrackingCount() > 0 || optional;
  }

  public void bind(Object instance) {
    instances.add(instance);
    if (multiple) {
      ServiceReference[] serviceReferences = getServiceReferences();
      for (int i=0; i<serviceReferences.length; i++) {
        bound = serviceReferences[i];
        invokeEventMethod(instance, bindMethodName, serviceReferences[i]);
      }
    } else { // unary
      bound = getServiceReference();
      invokeEventMethod(instance, bindMethodName, bound);
    }
  }

  
  public void unbind(Object instance) {
    instances.remove(instance);
    if (bound == null) return;
    if (multiple) {
      ServiceReference[] serviceReferences = getServiceReferences();
      for (int i=0; i<serviceReferences.length; i++) {
        invokeEventMethod(instance, unbindMethodName, serviceReferences[i]);
      }
    } else { // unary
      invokeEventMethod(instance, unbindMethodName, bound);
    }
    bound = null;
  }

  /**
   * Will search for the <methodName>(<type>) in the given class
   * by first looking in class after 
   * <methodName>(ServiceReference) then
   * <methodName>(Interface of Service).
   * 
   * If no method is found it will then continue to the super class.
   */
  private void invokeEventMethod(Object instance,
                                 String methodName, 
                                 ServiceReference ref) {
    Class instanceClass = instance.getClass();
    Method method = null;
    Object arg = null;

    Object service = bc.getService(ref); 
    // service can be null if the service is unregistering.

    Class serviceClass = null;
      
    while (instanceClass != null && method == null) {
      Method[] ms = instanceClass.getDeclaredMethods(); 

      // searches this class for a suitable method.
      for (int i = 0; i < ms.length; i++) {
        if (methodName.equals(ms[i].getName()) &&
            (Modifier.isProtected(ms[i].getModifiers()) ||
             Modifier.isPublic(ms[i].getModifiers()))) {
      
          Class[] parms = ms[i].getParameterTypes();
      
          if (parms.length == 1) {
            try {
              if (ServiceReference.class.equals(parms[0])) {
                ms[i].setAccessible(true);
                ms[i].invoke(instance, new Object[] { ref });
                return ;
              } else if (parms[0].isAssignableFrom(serviceClass)) {
                ms[i].setAccessible(true);
                ms[i].invoke(instance, new Object[] { service });
                return ;
              }
            } catch (IllegalAccessException e) {
              Activator.log.error("Could not access the method: " + methodName + " got " + e);
            } catch (InvocationTargetException e) {
              Activator.log.error("Could not invoke the method: " + methodName + " got " + e);
            }
          }
        }
      }
      
      instanceClass = instanceClass.getSuperclass();
    }
    
    // did not find any such method.
    Activator.log.error("Could not find bind/unbind method \"" + methodName + "\"");
  }

  public String getName() {
    return refName;
  }
}
