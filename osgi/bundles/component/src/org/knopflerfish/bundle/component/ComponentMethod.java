/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
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
import java.lang.reflect.Proxy;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentServiceObjects;


class ComponentMethod
{
  final String name;
  final Component comp;
  final boolean allowInt;
  final Reference ref;
  private Method method = null;
  private int prio = 0;
  private Class<?> [] params;


  ComponentMethod(String name, Component comp, boolean allowInt) {
    this.name = name;
    this.comp = comp;
    this.allowInt = allowInt;
    ref = null;
  }


  /**
   *
   */
  ComponentMethod(String name, Component comp, Reference ref) {
    this.name = name;
    this.comp = comp;
    this.ref = ref;
    allowInt = false;
  }


  /**
   *
   */
  ComponentException invoke(ComponentContextImpl cci) {
    if (isMissing(true)) {
      return new ComponentException("Missing specified method: " + name);
    }
    try {
      return prepare(cci.getComponentInstance().getInstance(), cci, 0, null, null).invoke();
    } catch (final ComponentException ce) {
      return ce;
    }
  }


  /**
   *
   */
  ComponentException invoke(ComponentContextImpl cci, int reason) {
    if (isMissing(true)) {
      return new ComponentException("Missing specified method: " + name);
    }
    try {
      return prepare(cci.getComponentInstance().getInstance(), cci, reason, null, null).invoke();
    } catch (final ComponentException ce) {
      return ce;
    }
  }


  /**
   *
   */
  Operation prepare(ComponentContextImpl cci, ServiceReference<?> s, ReferenceListener rl) {
    return prepare(cci.getComponentInstance().getInstance(), cci, 0, s, rl);
  }


  /**
   *
   */
  boolean isMissing(boolean logError) {
    if (method == null) {
      if (logError) {
        Activator.logError(comp.bc, "Didn't find method \"" + name + "\" in " + comp, null);
      }
      return true;
    }
    return false;
  }


  /**
   *
   */
  boolean updateMethod(int minor, Method m, Class<?> clazz) {
    final Class<?> [] p = m.getParameterTypes();
    final int cPrio = ref != null ? checkReferenceParams(p, minor)
                            : checkComponentParams(p, allowInt);
    if (prio < cPrio) {
      final int modifiers = m.getModifiers();
      if (checkMethodAccess(modifiers, minor > 0, clazz, clazz)) {
        method = m;
        prio = cPrio;
        params = p;
      }
      return true;
    }
    return false;
  }

  //
  // Private methods
  //

  /**
   *
   */
  private int checkComponentParams(Class<?> [] p, boolean allowInt) {
    int cPrio = 1;
    for (int i = 0; i < p.length; i++) {
      if (p[i] == ComponentContext.class) {
        cPrio = 8;
      } else if (p[i] == BundleContext.class) {
        cPrio = 7;
      } else if (p[i].isAnnotation()) {
        cPrio = 6;
      } else if (p[i] == Map.class) {
        cPrio = 5;
      } else if (allowInt && p[i] == int.class) {
        cPrio = 4;
      } else if (allowInt && p[i] == Integer.class) {
        cPrio = 3;
      } else {
        return -1;
      }
      if (i > 0) {
        cPrio = 2;
      }
    }
    return cPrio;
  }


  private int checkServiceClass(Class<?> pClass) {
    int cPrio = 1;
    Class<?> clazz = null;
    try {
      clazz = comp.compDesc.bundle.loadClass(ref.getServiceName());
      if (pClass != clazz) {
        cPrio--;
        if (!pClass.isAssignableFrom(clazz)) {
          cPrio = -1;
        }
      }
    } catch (final ClassNotFoundException _ignore) {
      // If we can not load the service class then it can only
      // be assignable to the Object class.
      cPrio = (pClass == Object.class) ? cPrio - 1 : -1;
    }
    return cPrio;
  }

  /**
   *
   */
  private int checkReferenceParams(Class<?> [] p, int minor) {
    if (p.length == 0) {
      return -1;
    }
    if (p.length == 1) {
      if (p[0] == ServiceReference.class) {
        return 6;
      } else if (minor > 2 && p[0] == ComponentServiceObjects.class) {
        return 5;
      } else {
        int d = checkServiceClass(p[0]);
        if (d >= 0) {
          return 3 + d;
        }
      }
      if (minor > 2 && p[0] == Map.class) {
        return 2;
      }
      return -1;
    }
    if (minor > 0) {
      if (minor > 2) {
        int res = 0;
        for (int i = 0; i < p.length; i++) {
          int d = checkServiceClass(p[i]);
          if (d >= 0) {
            res |= d;
          } else if (p[i] != ServiceReference.class &&
                     p[i] != ComponentServiceObjects.class &&
                     p[i] != Map.class) {
            return -1;
          }
        }
        return res;
      } else {
        if (p.length == 2 && p[1] == Map.class) {
          return checkServiceClass(p[0]);
        }
      }
    }
    return -1;
  }


  /**
   *
   */
  private boolean checkMethodAccess(int modifiers,
                                    boolean isSCR11,
                                    Class<?> clazz,
                                    Class<?> impl)
  {
    if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
      return true;
    }
    if (isSCR11) {
      // Implementation class allow every method
      if (clazz == impl) {
        return true;
      }
      // Class is not impl, method can not be private or
      // package private if packages differ. Must also
      // be the same classloader
      if (!Modifier.isPrivate(modifiers) &&
          clazz.getPackage() == impl.getPackage() &&
          clazz.getClassLoader() == impl.getClassLoader()) {
        return true;
      }
    }
    clazz = clazz.getSuperclass();
    if (clazz != null) {
      return checkMethodAccess(modifiers, isSCR11, clazz, impl);
    }
    return false;
  }


  /**
   *
   */
  private Operation prepare(Object instance,
                            ComponentContextImpl cci,
                            int reason,
                            ServiceReference<?> s,
                            ReferenceListener rl) {
    method.setAccessible(true);
    final Object [] args = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      if (params[i] == ComponentContext.class) {
        args[i] = cci;
      } else if (params[i] == BundleContext.class) {
        args[i] = comp.bc;
      } else if (params[i] == ComponentServiceObjects.class) {
        args[i] = cci.getComponentServiceObjects(s, rl);
      } else if (params[i].isAnnotation()) {
        args[i] = Proxy.newProxyInstance(params[i].getClassLoader(),
                                         new Class[] { params[i] },
                                         new ComponentPropertyProxy(cci));
    
      } else if (params[i] == Map.class) {
        if (ref != null) {
          args[i] = new PropertyDictionary(s);
        } else {
          args[i] =  cci.getProperties();
        }
      } else if (params[i] == int.class || params[i] == Integer.class) {
        args[i] = new Integer(reason > Component.KF_DEACTIVATION_REASON_BASE ?
                              ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED :
                              reason);
      } else if (params[i] == ServiceReference.class) {
        args[i] = s;
      } else {
        try {
          // TODO think about exceptions and circular activations
          args[i] = rl.getService(s, cci);
          if (args[i] == null) {
            Activator.logDebug("Got null service argument for " + method +
                               " in " + instance.getClass() +  " for component " +
                               comp.compDesc.getName() + " registered for " +
                               comp.compDesc.bundle);
            throw new ComponentException("Got null service, " + Activator.srInfo(s));
          }
        } catch (final Exception e) {
          Activator.logDebug("Got " + e + " when getting service argument for " + method +
                             " in " + instance.getClass() +  " for component " +
                             comp.compDesc.getName() + " registered for " +
                             comp.compDesc.bundle);
          if (e instanceof ComponentException) {
            throw (ComponentException)e;
          } else {
            throw new ComponentException("Failed to get service, " + Activator.srInfo(s), e);
          }
        }
      }
    }
    return new Operation(instance, args);
  }

  class Operation {

    final Object instance;
    /* Reason for failure */
    final Object [] args;

    Operation(Object instance, Object [] args) {
      this.instance = instance;
      this.args = args;
    }

    ComponentException invoke() {
      try {
        Activator.logDebug("Call " + method + " in " + instance.getClass()
                           +  " for component " + comp.compDesc.getName() +
                           " registered for " + comp.compDesc.bundle);
        method.invoke(instance, args);
      } catch (final IllegalAccessException e) {
        final String msg = "Could not invoke \"" + method + "\" in: " + instance.getClass();
        Activator.logError(comp.bc, msg, e);
        return new ComponentException(msg, e);
      } catch (IllegalArgumentException e) {
        StringBuilder msg = new StringBuilder();
        msg.append("Wrong arguments to \"");
        msg.append(method);
        msg.append("\" in: ");
        msg.append(instance.getClass());
        msg.append(". Got argument classes, [ ");
        Class<?> [] ac = new Class<?>[args.length];
        for (int i = 0; i < ac.length; i++) {
          if (i > 0) {
            msg.append(", ");
          }
          msg.append(args[i].getClass().getName());
        }
        msg.append(" ]");
        final String m = msg.toString();
        Activator.logError(comp.bc, m, e);
        return new ComponentException(m, e);
      } catch (final InvocationTargetException e) {
        String msg = "exception";
        final Throwable cause = e.getTargetException();
        if (cause != null) {
          msg = cause.toString();
        }
        msg = "Got " + msg + " when invoking \"" + method + "\" in: " + instance.getClass();
        Activator.logError(comp.bc, msg, e);
        return new ComponentException(msg, e);
      }
      return null;
    }

  }
}
