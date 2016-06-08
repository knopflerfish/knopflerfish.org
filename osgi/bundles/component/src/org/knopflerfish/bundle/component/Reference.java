/*
 * Copyright (c) 2006-2016, KNOPFLERFISH project
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentException;


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
  private volatile ComponentField field = null;
  private volatile boolean fieldAndMethodsSet = false;

  private ReferenceListener listener = null;
  private TreeMap<String, ReferenceListener> factoryListeners = null;
  private int numListeners;
  private int available;
  private int minCardinality = -1;


  /**
   *
   */
  Reference(Component comp, ReferenceDescription refDesc) {
    this.comp = comp;
    this.refDesc = refDesc;
    Properties props = comp.compDesc.getProperties();
    Filter target = getTarget(props, "component description for " + comp);
    if (target == null) {
      target = refDesc.targetFilter;
    }
    targetFilter = target;
    if (!updateMinCardinality(props)) {
      minCardinality = refDesc.optional ? 0 : 1;
    }
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
    return available >= minCardinality;
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
   */
  public boolean isGreedy() {
    return refDesc.greedy;
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

  int getMinimumCardinality()
  {
    return minCardinality;
  }


  String getScope()
  {
    return refDesc.scope;
  }


  ComponentMethod getBindMethod() {
    assertFieldAndMethods();
    return bindMethod;
  }


  ComponentMethod getUnbindMethod() {
    assertFieldAndMethods();
    return unbindMethod;
  }


  ComponentMethod getUpdatedMethod() {
    assertFieldAndMethods();
    return updatedMethod;
  }


  ComponentField getField() {
    assertFieldAndMethods();
    return field;
  }


  boolean isRefOptional()
  {
    return minCardinality == 0;
  }


  boolean isScopeBundle()
  {
    return Constants.SCOPE_BUNDLE.equals(refDesc.scope);
  }


  /**
   * Start listening for this reference. It is a bit tricky to
   * get initial state synchronized with the listener.
   *
   */
  void start() {
    available = 0;
    numListeners = 1;
    listener = new ReferenceListener(this);
    if (comp.cmConfig.isEmpty()) {
      listener.setTarget(null, null);
    } else {
      String [] ccid = comp.cmConfig.getCCIds();
      listener.setTarget(ccid[0], comp.cmConfig.getProperties(ccid[0]));
      for (int i = 1; i < ccid.length; i++) {
        update(ccid[i], comp.cmConfig.getProperties(ccid[i]), false);
      }
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
      for (final ReferenceListener referenceListener : 
             (new HashSet<ReferenceListener>(factoryListeners.values()))) {
        referenceListener.stop();
      }
      factoryListeners = null;
    }
  }


  /**
   *
   */
  void update(String ccid, Map<String, Object> dict, boolean useNoId) {
    boolean before = isSatisfied();
    boolean doCheck = updateMinCardinality(dict) && isSatisfied() != before;
    if (listener != null) {
      // We only have one listener, check if it still is true;
      if (listener.checkTargetChanged(ccid, dict)) {
        if (listener.isOnlyId(useNoId ? Component.NO_CCID : ccid)) {
          // Only one ccid change listener target
          listener.setTarget(ccid, dict);
        } else {
          // We have multiple listener we need multiple listeners
          factoryListeners = new TreeMap<String, ReferenceListener>();
          for (String p : listener.getIds()) {
            factoryListeners.put(p, listener);
          }
          listener = null;
          // NYI, optimize, we don't have to checkTargetChanged again
        }
      } else if (useNoId) {
        // No change, just make sure that ccid is registered
        listener.addId(ccid, true);
      }
    }
    if (factoryListeners != null) {
      ReferenceListener rl = factoryListeners.get(ccid);
      boolean newListener;
      if (rl != null) {
        // Listener found, check if we need to change it
        newListener = false;
        if (rl.checkTargetChanged(ccid, dict)) {
          if (rl.isOnlyId(ccid)) {
            rl.setTarget(ccid, dict);
          } else {
            rl.removeId(ccid);
            newListener = true;
          }
        }
      } else {
        // Pid is new, check if we already have a matching listener
        newListener = true;
        for (final Iterator<ReferenceListener> i = new HashSet<ReferenceListener>(factoryListeners.values()).iterator(); i.hasNext(); ) {
          rl = i.next();
          if (!rl.checkTargetChanged(ccid, dict)) {
            rl.addId(ccid, false);
            factoryListeners.put(ccid, rl);
            newListener = false;
            break;
          }
        }
      }
      if (newListener) {
        numListeners++;
        rl = new ReferenceListener(this);
        factoryListeners.put(ccid, rl);
        rl.setTarget(ccid, dict);
      }
    }
    if (doCheck) {
      if (before) {
        if (!isSatisfied()) {
          comp.refUnavailable(this);
        }
      } else {
        if (isSatisfied()) {
          comp.refAvailable(this);
        }
      }
    }
  }


  /**
   * We got a configuration PID update if we have a listener.
   */
  void updateNoPid(String ccid) {
    if (listener != null && listener.isOnlyId(Component.NO_CCID)) {
      listener.addId(ccid, true);
    }
  }


  /**
   *
   */
  void remove(String ccid) {
    if (listener != null) {
      listener.removeId(ccid);
      if (listener.noIds()) {
        listener.addId(Component.NO_CCID, false);
      }
    } else if (factoryListeners != null) {
      final ReferenceListener rl = factoryListeners.remove(ccid);
      rl.removeId(ccid);
      if (rl.noIds()) {
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
  ReferenceListener getListener(String ccid) {
    if (listener != null) {
      return listener;
    } else {
      return factoryListeners.get(ccid);
    }
  }


  /**
   * Get target value for reference, if target is missing or the target
   * string is malformed return null.
   */
  Filter getTarget(Map<?, Object> d, String src)
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
    if (++available == (minCardinality == 0 ? 1 : minCardinality)) {
      return comp.refAvailable(this);
    }
    return false;
  }


  /**
   *
   * @return True, if component became unsatisfied otherwise false.
   */
  boolean refUnavailable() {
    if (available-- == (minCardinality == 0 ? 1 : minCardinality)) {
      return comp.refUnavailable(this);
    }
    return false;
  }



  private void assertFieldAndMethods() {
    if (!fieldAndMethodsSet) {
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
      if (refDesc.field != null) {
        try {
          field = new ComponentField(refDesc.field, comp, this);
        } catch (NoSuchFieldException nsfe) {
          Activator.logError(comp.bc,
                             "Field not found in component, name = " + refDesc.field,
                             nsfe);
        }
      }
      fieldAndMethodsSet = true;
    }
  }



  private boolean updateMinCardinality(Map<?, Object> d)
  {
    String key = refDesc.name + ".cardinality.minimum";
    final Object prop = d.get(key);
    if (prop != null) {
      try {
        int mc = ComponentPropertyProxy.coerceInteger(prop);
        int minVal = refDesc.optional ? 0 : 1;
        int maxVal = refDesc.multiple ? Integer.MAX_VALUE : 1;
        if (mc < minVal) {
          Activator.logInfo(comp.bc, "Property " + key + " too small " + mc + " < " + minVal);
        } else if (mc > maxVal) {
          Activator.logInfo(comp.bc, "Property " + key + " too large " + mc + " > " + maxVal);
        } else {
          boolean res = minCardinality != mc;
          minCardinality = mc;
          return res;
        }
      } catch (ComponentException ce) {
        Activator.logInfo(comp.bc, "Property " + key + " is not an integer: " + prop);        
      }
    }
    return false;
  }

}
