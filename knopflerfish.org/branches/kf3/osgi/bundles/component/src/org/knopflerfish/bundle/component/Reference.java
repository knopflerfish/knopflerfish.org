/*
 * Copyright (c) 2006-2009, KNOPFLERFISH project
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


public class Reference extends ExtendedServiceTracker {

  private boolean optional;
  private boolean multiple;
  private boolean dynamic;

  private String bindMethodName;
  private String unbindMethodName;
  private String refName;
  private String interfaceName;

  /* If unary, this is the choosen one. */
  private ServiceReference bound;
  private boolean doneBound = false;

  private Collection instances = new ArrayList();

  private Config config;

  private boolean overrideUnsatisfied = false;

  public Reference(String refName, Filter filter, String interfaceName,
                   boolean optional, boolean multiple, boolean dynamic,
                   String bindMethodName, String unbindMethodName,
                   BundleContext bc) {

    super(bc, filter);
    this.refName = refName;
    this.optional = optional;
    this.multiple = multiple;
    this.dynamic = dynamic;
    this.bindMethodName = bindMethodName;
    this.unbindMethodName = unbindMethodName;
    this.interfaceName = interfaceName;
  }

  public void setConfig(Config config) {
    this.config = config;
  }

  public void addedService(ServiceReference ref, Object service) {
    if (doneBound) {
      if (multiple || (null==bound && dynamic && optional)) {
        for (Iterator iter = instances.iterator(); iter.hasNext();) {
          invokeEventMethod(iter.next(), bindMethodName, ref);
        }
        if (!multiple) {
          bound = ref;
        }
      }
    } else if (bound != null && !multiple) {
      ServiceReference newBound = getServiceReference();
      if (!bound.equals(newBound)) {
        if (dynamic) { // Bind new and unbind old
          for (Iterator iter = instances.iterator(); iter.hasNext();) {
            Object instance = iter.next();
            invokeEventMethod(instance, unbindMethodName, bound);
            invokeEventMethod(instance, bindMethodName, newBound);
            ungetService(bound); // this is new.
          }
          bound = newBound;
        } else { // Static: deactivate and reactivate
          overrideUnsatisfied = true;
          config.referenceUnsatisfied();
          overrideUnsatisfied = false;
          config.referenceSatisfied();
        }
      }
    }

    if (!isSatisfied(1) && isSatisfied() && config != null) {
      config.referenceSatisfied();
    }
  }

  /* TODO? public void modifiedService() */

  public void removingService(ServiceReference ref, Object service) {
    if (!isSatisfied(1)) { // Will be unsatisfied by this remove.
      overrideUnsatisfied = true;
      config.referenceUnsatisfied();
      overrideUnsatisfied = false;
    } else { // Will continue to be satisfied.
      if (doneBound && multiple) {
        for (Iterator iter = instances.iterator(); iter.hasNext();) {
          invokeEventMethod(iter.next(), unbindMethodName, ref);
        }
        ungetService(ref);
      } else if (ref.equals(bound)) { // The bound one is removed.
        if (dynamic) { // Unbind and let removedService bind the new one.
          for (Iterator iter = instances.iterator(); iter.hasNext();) {
            invokeEventMethod(iter.next(), unbindMethodName, ref);
          }
          ungetService(ref);
          bound = null;
        } else { // Static. Deactivate and let removedService re-activate.
          overrideUnsatisfied = true;
          config.referenceUnsatisfied();
          overrideUnsatisfied = false;
        }
      }
    }
  }

  public void removedService(ServiceReference ref, Object service) {
    if (multiple) return;
    // See if we're inte the process of replacing a service.
    if (dynamic && doneBound && bound == null && isSatisfied()) {
      // Dynamic. Bind the new service.
      bound = getServiceReference();
      for (Iterator iter = instances.iterator(); iter.hasNext();) {
        invokeEventMethod(iter.next(), bindMethodName, bound);
      }
    } else if (!dynamic && !doneBound && isSatisfied()) {
      // Static. Try to activate.
      config.referenceSatisfied();
    }
  }

  public boolean isSatisfied() {
    if (overrideUnsatisfied) return false;
    return isSatisfied(0);
  }
  public boolean isSatisfied(int sizeLimit) {
    return size() > sizeLimit || optional;
  }

  public void bind(Object instance) {
    instances.add(instance);
    doneBound = true;
    if (multiple) {
      ServiceReference[] serviceReferences = getServiceReferences();
      if (serviceReferences != null) {
        for (int i=0; i<serviceReferences.length; i++) {
          bound = serviceReferences[i];
          invokeEventMethod(instance, bindMethodName, bound);
        }
      }
    } else { // unary
      bound = getServiceReference();
      invokeEventMethod(instance, bindMethodName, bound);
    }
  }


  public void unbind(Object instance) {
    instances.remove(instance);
    if (!doneBound) return;
    doneBound = false;
    if (multiple) {
      ServiceReference[] serviceReferences = getServiceReferences();
      if (serviceReferences != null) {
        for (int i = serviceReferences.length - 1; i >= 0; i--) {
          invokeEventMethod(instance, unbindMethodName, serviceReferences[i]);
          ungetService(serviceReferences[i]);
        }
      }
    } else { // unary
      invokeEventMethod(instance, unbindMethodName, bound);
      ungetService(bound);
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

    if(ref == null) {
      if(Activator.log.doDebug()) {
        Activator.log.debug("ignore invokeMethod due to NULL reference when calling " + instance + "." + methodName + ", bundle=" + config.getBundle().getBundleId());
      }
      return;
    }

    if (methodName == null) {
      return ;
    }

    Class instanceClass = instance.getClass();
    Method method = null;

    Bundle bundle = ref.getBundle(); // the bundle where the service is located
    Class serviceClass = null;

    try {
      serviceClass = bundle.loadClass(getInterfaceName());
    } catch (ClassNotFoundException e) {
      Activator.log.error("Declarative Services could not load class", e);
      return ;
    }

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
                Object service = getService(ref);
                ms[i].setAccessible(true);
                ms[i].invoke(instance, new Object[] { service });
                return ;
              }
            } catch (IllegalAccessException e) {
              Activator.log.error("Declarative Services could not access the method \"" + methodName +
                                  "\" used by component \"" + config.getName() + "\". Got exception.", e);
            } catch (InvocationTargetException e) {
              Activator.log.error("Declarative Services got exception while invoking \"" + methodName
                                  + "\" used by component \"" + config.getName() + "\". Got exception.", e);
            }
          }
        }
      }

      instanceClass = instanceClass.getSuperclass();
    }

    // did not find any such method.
    Activator.log.error("Declarative Services could not find bind/unbind method \"" + methodName +
                        "\" in class \"" + config.getImplementation() + "\" used by component " + config.getName() + "\".");
  }

  public Reference copy() {
    return new Reference(refName, filter, interfaceName, optional, multiple, dynamic,
                         bindMethodName, unbindMethodName, context);
  }

  public String getName() {
    return refName;
  }

  public String getInterfaceName() {
    return interfaceName;
  }
}
