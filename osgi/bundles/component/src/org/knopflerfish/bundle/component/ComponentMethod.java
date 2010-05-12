/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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

import java.lang.reflect.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.component.*;


class ComponentMethod
{
  final String name;
  final Component comp;
  final boolean allowInt;
  final Reference ref;
  private Method method = null;
  private int prio = 0;
  private Class [] params;


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
    return invoke(cci.getComponentInstance().getInstance(), cci, 0, null);
  }


  /**
   *
   */
  ComponentException invoke(ComponentContextImpl cci, int reason) {
    return invoke(cci.getComponentInstance().getInstance(), cci, reason, null);
  }


  /**
   *
   */
  ComponentException invoke(ComponentContextImpl cci, ServiceReference s) {
    return invoke(cci.getComponentInstance().getInstance(), cci, 0, s);
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
  boolean updateMethod(boolean isSCR11, Method m, Class clazz) {
    Class [] p = m.getParameterTypes();
    int cPrio = ref != null ? checkReferenceParams(p, isSCR11, ref)
                            : checkComponentParams(p, allowInt);
    if (prio < cPrio) {
      int modifiers = m.getModifiers();
      if (checkMethodAccess(modifiers, isSCR11, clazz, clazz)) {
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
  private int checkComponentParams(Class [] p, boolean allowInt) {
    int cPrio = 1;
    for (int i = 0; i < p.length; i++) {
      if (p[i] == ComponentContext.class) {
        cPrio = 7;
      } else if (p[i] == BundleContext.class) {
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


  /**
   *
   */
  private int checkReferenceParams(Class [] p, boolean isSCR11, Reference ref) {
    int cPrio;
    if (p.length == 1) {
      if (p[0] == ServiceReference.class) {
        return 5;
      }
      cPrio = 4;
    } else if (isSCR11 && p.length == 2 && p[1] == Map.class) {
      cPrio = 2;
    } else {
      return -1;
    }
    Class clazz = null;
    try {
      clazz = comp.compDesc.bundle.loadClass(ref.refDesc.interfaceName);
      if (p[0] != clazz) {
        cPrio--;
        if (!p[0].isAssignableFrom(clazz)) {
          cPrio = -1;
        }
      }
    } catch (ClassNotFoundException _) {
      // If we can not load the service class then it can only
      // be assignable to the Object class.
      cPrio = (p[0] == Object.class) ? cPrio - 1 : -1;
    }
    return cPrio;
  }


  /**
   *
   */
  private boolean checkMethodAccess(int modifiers, boolean isSCR11, Class clazz, Class impl) {
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
  private ComponentException invoke(Object instance, ComponentContextImpl cci,
                                    int reason, ServiceReference s) {
    if (isMissing(true)) {
      return new ComponentException("Missing specified method: " + name);
    }
    method.setAccessible(true);
    Object [] args = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      if (params[i] == ComponentContext.class) {
        args[i] = cci;
      } else if (params[i] == BundleContext.class) {
        args[i] = comp.bc;
      } else if (params[i] == Map.class) {
        if (ref != null) {
          HashMap sProps = new HashMap();
          String[] keys = s.getPropertyKeys();
          for (int j = 0; j < keys.length; j++) {
            sProps.put(keys[j], s.getProperty(keys[j]));
          }
          args[i] = sProps;
        } else {
          args[i] =  cci.getProperties();
        }
      } else if (params[i] == int.class || params[i] == Integer.class) {
        args[i] = new Integer(reason);
      } else if (params[i] == ServiceReference.class) {
        args[i] = s;
      } else {
        args[i] = comp.bc.getService(s);
      }
    }
    try {
      Activator.logDebug("Call " + method + " in " + instance.getClass()
                         +  " for component " + comp.compDesc.getName() +
                         " registered for " + comp.compDesc.bundle);
      method.invoke(instance, args);
    } catch (IllegalAccessException e) {
      String msg = "Could not invoke \"" + method + "\" in: " + instance.getClass();
      Activator.logError(comp.bc, msg, e);
      return new ComponentException(msg, e);
    } catch (InvocationTargetException e) {
      String msg = "Got exception when invoking \"" + method + "\" in: " + instance.getClass();
      Activator.logError(comp.bc, msg, e);
      return new ComponentException(msg, e);
    }
    return null;
  }

}
