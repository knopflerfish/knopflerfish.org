/*
 * Copyright (c) 2006-2013, KNOPFLERFISH project
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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;


/**
 *
 */
class Reference implements org.apache.felix.scr.Reference
{
  final Component comp;
  final ReferenceDescription refDesc;
  final Filter targetFilter;

  private volatile ComponentMethod bindMethod = null;
  private volatile ComponentMethod unbindMethod = null;
  private volatile ComponentMethod updatedMethod = null;
  private volatile boolean methodsSet = false;

  private ReferenceListener listener = null;
  private TreeMap<String, ReferenceListener> factoryListeners = null;
  private int numListeners;
  private int available;


  /**
   *
   */
  Reference(Component comp, ReferenceDescription refDesc) {
    this.comp = comp;
    this.refDesc = refDesc;
    Filter target = getTarget(comp.compDesc.getProperties(),
                              "component description for " + comp);
    if (target == null) {
      target = refDesc.targetFilter;
    }
    targetFilter = target;
  }

  //
  // org.apache.felix.scr.Reference interface
  //

  /**
   * @see org.apache.felix.scr.Reference.getName
   */
  public String getName() {
    return refDesc.name;
  }


  /**
   * @see org.apache.felix.scr.Reference.getServiceName
   */
  public String getServiceName() {
    return refDesc.interfaceName;
  }


  /**
   * @see org.apache.felix.scr.Reference.getServiceReferences
   */
  public ServiceReference<?>[] getServiceReferences() {
    final ReferenceListener l = listener;
    ServiceReference<?>[] res = null;
    if (l != null)  {
      res = l.getBoundServiceReferences();
    }
    return res;
  }


  /**
   * @see org.apache.felix.scr.Reference.isSatisfied
   */
  public boolean isSatisfied() {
    return available > 0;
  }


  /**
   * @see org.apache.felix.scr.Reference.isOptional
   */
  public boolean isOptional() {
    return refDesc.optional;
  }


  /**
   * @see org.apache.felix.scr.Reference.isMultiple
   */
  public boolean isMultiple() {
    return refDesc.multiple;
  }


  /**
   * @see org.apache.felix.scr.Reference.isStatic
   */
  public boolean isStatic() {
    return !refDesc.dynamic;
  }


  /**
   * @see org.apache.felix.scr.Reference.getTarget
   */
  public String getTarget() {
    Filter target;
    final ReferenceListener l = listener;
    if (l != null)  {
      target = l.getTargetFilter();
    } else {
      target = targetFilter;
    }
    return target != null ? targetFilter.toString() : null;
  }


  /**
   * @see org.apache.felix.scr.Reference.getBindMethodName
   */
  public String getBindMethodName() {
    return refDesc.bind;
  }


  /**
   * @see org.apache.felix.scr.Reference.getUnbindMethodName
   */
  public String getUnbindMethodName() {
    return refDesc.unbind;
  }


  /**
   * @see org.apache.felix.scr.Reference.getUpdatedMethodName
   */
  public String getUpdatedMethodName() {
    return null;
  }


  /**
   * String with info about reference.
   */
  @Override
  public String toString() {
    return "Reference " + refDesc.name + " in " + comp;
  }


  //
  // Package methods
  //

  private void assertMethods() {
    if (!methodsSet) {
      final HashMap<String, ComponentMethod[]> lookFor = new HashMap<String, ComponentMethod[]>();
      if (refDesc.bind != null) {
        bindMethod = new ComponentMethod(refDesc.bind, comp, this);
        comp.saveMethod(lookFor, refDesc.bind, bindMethod);
      }
      if (refDesc.unbind != null) {
        unbindMethod = new ComponentMethod(refDesc.unbind, comp, this);
        comp.saveMethod(lookFor, refDesc.unbind, unbindMethod);
      }
      if (refDesc.updated != null) {
        updatedMethod = new ComponentMethod(refDesc.updated, comp, this);
        comp.saveMethod(lookFor, refDesc.updated, updatedMethod);
      }
      comp.scanForMethods(lookFor);
      methodsSet = true;
    }
  }

  ComponentMethod getBindMethod() {
    assertMethods();
    return bindMethod;
  }

  ComponentMethod getUnbindMethod() {
    assertMethods();
    return unbindMethod;
  }

  ComponentMethod getUpdatedMethod() {
    assertMethods();
    return updatedMethod;
  }


  /**
   * Start listening for this reference. It is a bit tricky to
   * get initial state synchronized with the listener.
   *
   */
  void start(Configuration [] config) {
    available = 0;
    numListeners = 1;
    if (config != null && config.length > 0) {
      listener = new ReferenceListener(this);
      listener.setTarget(config[0]);
      for (int i = 1; i < config.length; i++) {
        update(config[i], false);
      }
    } else {
      listener = new ReferenceListener(this);
      listener.setTarget(null);
    }
  }


  /**
   *
   */
  void stop() {
    if (listener != null) {
      listener.stop();
      listener = null;
    } else {
      for (final ReferenceListener referenceListener : (new HashSet<ReferenceListener>(
                                                                                       factoryListeners
                                                                                           .values()))) {
                                                                                            referenceListener.stop();
                                                                                          }
      factoryListeners = null;
    }
  }


  /**
   *
   */
  void update(Configuration c, boolean useNoPid) {
    final String pid = c.getPid();
    if (listener != null) {
      // We only have one listener, check if it still is true;
      if (listener.checkTargetChanged(c)) {
        if (listener.isOnlyPid(useNoPid ? Component.NO_PID : pid)) {
          // Only one pid change listener target
          listener.setTarget(c);
        } else {
          // We have multiple listener we need multiple listeners
          factoryListeners = new TreeMap<String, ReferenceListener>();
          for (String p : listener.getPids()) {
            factoryListeners.put(p, listener);
          }
          listener = null;
          // NYI, optimize, we don't have to checkTargetChanged again
        }
      } else if (useNoPid) {
        // No change, just make sure that pid is registered
        listener.addPid(pid, true);
      }
    }
    if (factoryListeners != null) {
      ReferenceListener rl = factoryListeners.get(pid);
      if (rl != null) {
        // Listener found, check if we need to change it
        if (rl.checkTargetChanged(c)) {
          if (rl.isOnlyPid(pid)) {
            rl.setTarget(c);
            return;
          } else {
            rl.removePid(pid);
            // Fall through to new listener creation
          }
        } else {
          // No change
          return;
        }
      } else {
        // Pid is new, check if we already have a matching listener
        for (final Iterator<ReferenceListener> i = new HashSet<ReferenceListener>(factoryListeners.values()).iterator(); i.hasNext(); ) {
          rl = i.next();
          if (!rl.checkTargetChanged(c)) {
            rl.addPid(pid, false);
            factoryListeners.put(pid, rl);
            return;
          }
        }
      }
      numListeners++;
      rl = new ReferenceListener(this);
      factoryListeners.put(pid, rl);
      rl.setTarget(c);
    }
  }


  /**
   * We got a configuration PID update if we have a listener.
   */
  void updateNoPid(String pid) {
    if (listener != null && listener.isOnlyPid(Component.NO_PID)) {
      listener.addPid(pid, true);
    }
  }


  /**
   *
   */
  void remove(String pid) {
    if (listener != null) {
      listener.removePid(pid);
      if (listener.noPids()) {
        listener.addPid(Component.NO_PID, false);
      }
    } else {
      final ReferenceListener rl = factoryListeners.remove(pid);
      rl.removePid(pid);
      if (rl.noPids()) {
        rl.stop();
        if (--numListeners == 1) {
          listener = factoryListeners.get(factoryListeners.lastKey());
          factoryListeners = null;
        }
      }
    }
  }


  /**
   *
   */
  ReferenceListener getListener(String pid) {
    if (listener != null) {
      return listener;
    } else {
      return factoryListeners.get(pid);
    }
  }


  /**
   * Get target value for reference, if target is missing or the target
   * string is malformed return null.
   */
  Filter getTarget(Dictionary<?, Object> d, String src)
  {
    final Object prop = d.get(refDesc.name + ".target");
    if (prop != null) {
      String res = null;
      if (prop instanceof String) {
        res = (String) prop;
      } else if (prop instanceof String []) {
        String [] propArray = (String []) prop;
        if (propArray.length == 1) {
          res = propArray[0];
        }
      }
      if (res != null) {
        try {
          return FrameworkUtil.createFilter(res);
        } catch (final InvalidSyntaxException ise) {
          Activator.logError(comp.bc,
                             "Failed to parse target property. Source is " + src,
                             ise);
        }
      } else {
        Activator.logError(comp.bc,
                           "Target property is no a single string. Source is " + src,
                           null);
      }
    }
    return null;
  }

  /**
   * Notify component if reference became available.
   *
   * @return True, if component became satisfied otherwise false.
   */
  boolean refAvailable() {
    if (available++ == 0) {
      return comp.refAvailable(this);
    }
    return false;
  }


  /**
   *
   * @return True, if component became unsatisfied otherwise false.
   */
  boolean refUnavailable() {
    if (--available == 0) {
      return comp.refUnavailable(this);
    }
    return false;
  }
}
