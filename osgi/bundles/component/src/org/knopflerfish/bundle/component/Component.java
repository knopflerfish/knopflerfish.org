/*
 * Copyright (c) 2006-2017, KNOPFLERFISH project
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
import java.util.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentInstance;
import org.osgi.util.promise.Deferred;


abstract class Component implements org.apache.felix.scr.Component {

  final static String NO_CCID = "";
  final private static int DISABLED_OFFSET = -999999999;
  final private static int STATE_DISPOSED = 0;
  final private static int STATE_DISPOSING = 1;
  final private static int STATE_DISABLED = 2;
  final private static int STATE_DISABLING = 3;
  final private static int STATE_ENABLED = 4;
  final private static int STATE_ENABLING = 5;
  final protected static int STATE_SATISFIED = 6;

  final static int KF_DEACTIVATION_REASON_BASE = 100;
  final static int KF_DEACTIVATION_REASON_ACTIVATION_FAILED = KF_DEACTIVATION_REASON_BASE + 1;
  final static int KF_DEACTIVATION_REASON_COMPONENT_DEACTIVATING = KF_DEACTIVATION_REASON_BASE + 2;
  final static int KF_DEACTIVATION_REASON_COMPONENT_DEACTIVATED = KF_DEACTIVATION_REASON_BASE + 3;

  final SCR scr;
  final ComponentDescription compDesc;
  final BundleContext bc;
  final CMConfig cmConfig;
  final ComponentServiceListener listener;

  /**
   * UnresolvedConstraints is the number of unsatisfied constraints,
   * a negative value means that we are in the process of enabling
   * and calculating constraints.
   */
  Long id = Long.valueOf(-1);
  private int unresolvedConstraints;
  final HashMap<String, ComponentConfiguration []> compConfigs = new HashMap<String, ComponentConfiguration []>();
  ComponentMethod activateMethod;
  ComponentMethod deactivateMethod;
  ComponentMethod modifiedMethod = null;
  private final HashMap<String, Integer> cmRevs = new HashMap<String, Integer>();
  protected transient int state = 0;
  private volatile Class<?> componentClass = null;
  private Reference [] refs = null;
  private final Object lock = new Object();

  /**
   *
   */
  Component(SCR scr, ComponentDescription cd) {
    this.scr = scr;
    this.bc = cd.bundle.getBundleContext();
    this.compDesc = cd;
    String [] cp = compDesc.getConfigurationPid();
    if (cp == null) {
      cp = new String [] { compDesc.getName() };
    }
    this.cmConfig = new CMConfig(scr.cmHandler, cp, cd.getConfigPolicy(), cd.bundle, this);
    this.listener = scr.getComponentServiceListener(bc);
  }

  // Felix Component interface impl.

  /**
   * @see org.apache.felix.scr.Component.getId
   */
  public long getId() {
    return id.longValue();
  }


  /**
   * @see org.apache.felix.scr.Component.getName
   */
  public String getName() {
    return compDesc.getName();
  }


  /**
   * @see org.apache.felix.scr.Component.getState
   */
  public int getState() {
    switch (state) {
    case STATE_DISABLED:
      return org.apache.felix.scr.Component.STATE_DISABLED;
    case STATE_ENABLED:
      {
        // TODO: How to handle factories?
        final ComponentConfiguration cc = getFirstComponentConfiguration();
        if (cc != null) {
          return org.apache.felix.scr.Component.STATE_DEACTIVATING;
        }
        return org.apache.felix.scr.Component.STATE_UNSATISFIED;
      }
    case STATE_SATISFIED:
      if (this instanceof FactoryComponent) {
        return org.apache.felix.scr.Component.STATE_FACTORY;
      } else {
        final ComponentConfiguration cc = getFirstComponentConfiguration();
        if (cc != null) {
          switch (cc.getState()) {
          case ComponentConfiguration.STATE_ACTIVE:
            return org.apache.felix.scr.Component.STATE_ACTIVE;
          case ComponentConfiguration.STATE_ACTIVATING:
            return org.apache.felix.scr.Component.STATE_ACTIVATING;
          case ComponentConfiguration.STATE_REGISTERED:
            return org.apache.felix.scr.Component.STATE_REGISTERED;
          case ComponentConfiguration.STATE_DEACTIVATING:
            return org.apache.felix.scr.Component.STATE_DEACTIVATING;
          case ComponentConfiguration.STATE_DEACTIVE:
            // TODO: Check if this can this happen?
            return org.apache.felix.scr.Component.STATE_UNSATISFIED;
          }
        }
        return org.apache.felix.scr.Component.STATE_ACTIVATING;
      }
    case STATE_DISPOSED:
      return org.apache.felix.scr.Component.STATE_DISPOSED;
    case STATE_ENABLING:
      return org.apache.felix.scr.Component.STATE_ENABLING;
    case STATE_DISABLING:
      return org.apache.felix.scr.Component.STATE_DISABLING;
    case STATE_DISPOSING:
      return org.apache.felix.scr.Component.STATE_DISPOSING;
    default:
      throw new RuntimeException("Internal Error, state = " + state);
    }
  }


  /**
   * @see org.apache.felix.scr.Component.getBundle
   */
  public Bundle getBundle() {
    return compDesc.bundle;
  }


  /**
   * @see org.apache.felix.scr.Component.getFactory
   */
  public String getFactory() {
    return compDesc.getFactory();
  }


  /**
   * @see org.apache.felix.scr.Component.isServiceFactory
   */
  public boolean isServiceFactory() {
    return compDesc.isServiceFactory();
  }


  /**
   * @see org.apache.felix.scr.Component.getClassName
   */
  public String getClassName() {
    return compDesc.getImplementation();
  }


  /**
   * @see org.apache.felix.scr.Component.isDefaultEnabled
   */
  public boolean isDefaultEnabled() {
    return compDesc.isEnabled();
  }


  /**
   * @see org.apache.felix.scr.Component.isImmediate
   */
  public boolean isImmediate() {
    return compDesc.isImmediate();
  }


  /**
   * @see org.apache.felix.scr.Component.getServices
   */
  public String[] getServices() {
    final String[] res = compDesc.getServices();
    return res != null ? (String[])res.clone() : null;
  }


  /**
   * @see org.apache.felix.scr.Component.getProperties
   */
  public Dictionary<String, Object> getProperties() {
    final ComponentConfiguration cc = getFirstComponentConfiguration();
    Dictionary<String, Object> res;
    if (cc != null) {
      res = cc.getProperties();
    } else {
      res = (Dictionary<String, Object>) compDesc.getProperties().clone();
    }
    return res;
  }


  /**
   * @see org.apache.felix.scr.Component.getReferences
   */
  public org.apache.felix.scr.Reference [] getReferences() {
    if (refs != null) {
      return refs.clone();
    }
    return null;
  }


  /**
   * @see org.apache.felix.scr.Component.getComponentInstance
   */
  public ComponentInstance getComponentInstance() {
    final ComponentConfiguration cc = getFirstComponentConfiguration();
    if (cc != null) {
      // TODO: what about factories
      final ComponentContext ctxt = cc.getActiveContext(null, null);
      if (ctxt != null) {
        return ctxt.getComponentInstance();
      }
    }
    return null;
  }


  /**
   * @see org.apache.felix.scr.Component.getActivate
   */
  public String getActivate() {
    return compDesc.getActivateMethod();
  }


  /**
   * @see org.apache.felix.scr.Component.isActivateDeclared
   */
  public boolean isActivateDeclared() {
    return compDesc.isActivateMethodSet();
  }


  /**
   * @see org.apache.felix.scr.Component.getDeactivate
   */
  public String getDeactivate() {
    return compDesc.getDeactivateMethod();
  }


  /**
   * @see org.apache.felix.scr.Component.isDeactivateDeclared
   */
  public boolean isDeactivateDeclared() {
    return compDesc.isDeactivateMethodSet();
  }


  /**
   * @see org.apache.felix.scr.Component.getModified
   */
  public String getModified() {
    return compDesc.getModifiedMethod();
  }


  /**
   * @see org.apache.felix.scr.Component.getConfigurationPolicy
   */
  public String getConfigurationPolicy() {
    return compDesc.getConfigPolicyString();
  }


  /**
   * Enable component. Start listening for constraint changes
   * so that we can activate this component when it becomes
   * satisfied.
   *
   * @see org.apache.felix.scr.Component.enable
   */
  public void enable() {
    Deferred<Void> d = new Deferred<Void>();
    enable(d);
    try {
      d.getPromise().getValue();
    } catch (InvocationTargetException e) {
    } catch (InterruptedException e) {
    }
  }


  /**
   * Disable component. Dispose of all ComponentConfigurations and
   * stop listening for constraint changes.
   *
   * @see org.apache.felix.scr.Component.disable
   */
  public void disable() {
    Deferred<Void> d = new Deferred<Void>();
    disable(d);
    try {
      d.getPromise().getValue();
    } catch (InvocationTargetException e) {
    } catch (InterruptedException e) {
    }
  }


  void disable(final Deferred<Void> d) {
    final Object disabling = new Object();
    synchronized(disabling) {
      try {
        new Thread(new Runnable() {
          @Override
          public void run()
          {
            try {
              if (isEnabled()) {
               synchronized (lock) {
                  if (isEnabled()) {
                    state = STATE_DISABLING;
                    synchronized(disabling) {
                      disabling.notifyAll();
                    }
                    disable(ComponentConstants.DEACTIVATION_REASON_DISABLED, d);
                  } else {
                    d.resolve(null);
                  }
                }
              } else {
                d.resolve(null);
              }
            } catch (Exception failure) {
              d.fail(failure);
            }
            synchronized(disabling) {
              disabling.notifyAll();
            }
          }
        }).start();
        disabling.wait();
      } catch (Exception failure) {
        d.fail(failure);
      }
    }
  }


  void enable(final Deferred<Void> d) {
    final Object enabling = new Object();
    synchronized(enabling) {
      try {
        Activator.logInfo(bc, "Enable " + toString());
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              if (!isEnabled()) {
               synchronized (lock) {
                  if (!isEnabled()) {
                    state = STATE_ENABLING;
                    synchronized(enabling) {
                      enabling.notifyAll();
                    }
                    enableTrackConstraints(d);
                  } else {
                    d.resolve(null);
                  }
                }
              } else {
                d.resolve(null);
              }
            } catch (Exception failure) {
              d.fail(failure);
            }
            synchronized(enabling) {
              enabling.notifyAll();
            }
          }
        }).start();
        enabling.wait();
      } catch (Exception failure) {
        d.fail(failure);
      }
    }
  }


  String getScope()
  {
    return compDesc.getScope();
  }


  HashMap<String, Object> getPropertiesMap()
  {
    return new HashMap(compDesc.getProperties());
  }


  /**
   * Disable component. Dispose of all ComponentConfigurations and
   * stop listening for constraint changes.
   */
  void disable(int reason, Deferred<Void> d) {
    Activator.logInfo(bc, "Disable " + toString());
    synchronized (lock) {
       final boolean dispose =  reason == ComponentConstants.DEACTIVATION_REASON_DISPOSED ||
         reason == ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED;
      if (d != null || isEnabled()) {
        state = dispose ? STATE_DISPOSING : STATE_DISABLING;
        disposeComponentConfigs(reason);
        untrackConstraints();
        refs = null;
        id = Long.valueOf(-1);
        state = dispose ? STATE_DISPOSED : STATE_DISABLED;
      } else if (dispose && state == STATE_DISABLED) {
        state = STATE_DISPOSED;
      }
    }
    if (d != null) {
      d.resolve(null);
    }
  }


  /**
   * Component is satisfied. Create ComponentConfiguration and
   * register service depending on component type.
   */
  ComponentConfiguration [] satisfied(ComponentConfiguration old) {
    Activator.logInfo(bc, "Satisfied: " + toString());
    state = STATE_SATISFIED;
    ComponentConfiguration [] ccs = newComponentConfigurations();
    for (ComponentConfiguration cc : ccs) {
      activateComponentConfiguration(cc, old);
      // TODO check if we can get multiple CCs when we have old and need to match old against new.
      old = null;
    }
    return ccs;
  }

  abstract void activateComponentConfiguration(ComponentConfiguration cc,
                                               ComponentConfiguration old);

  /**
   * Component is unsatisfied dispose of all ComponentConfiguration
   * for this component.
   */
  void unsatisfied(int reason) {
    Activator.logInfo(bc, "Unsatisfied: " + toString());
    state = STATE_ENABLED;
    disposeComponentConfigs(reason);
  }


  public String getCMPidRev(String id) {
    Integer i = cmRevs.get(id);
    return i != null ? i.toString() : "0";
  }


  /**
   * Start tracking services and CM config
   */
  private void enableTrackConstraints(Deferred<Void> d) {
    // unresolvedConstraints set to DISABLED_OFFSET, so that we don't satisfy until
    // end of this method
    unresolvedConstraints = DISABLED_OFFSET;
    id = scr.getNextComponentId();
    if (!cmConfig.subscribe()) {
      // If we have no mandatory CM data, add constraint
      unresolvedConstraints++;
    }
    final ArrayList<ReferenceDescription> rds = compDesc.getReferences();
    if (rds != null) {
      unresolvedConstraints += rds.size();
      refs = new Reference[rds.size()];
      for (int i = 0; i < refs.length; i++) {
        final Reference r = new Reference(this, rds.get(i));
        refs[i] = r;
        if (r.isRefOptional()) {
          // Optional references does not need to be check
          // if they are available
          unresolvedConstraints--;
        }
        r.start();
      }
    }
    // Remove blocking constraint, to see if we are satisfied
    unresolvedConstraints -= DISABLED_OFFSET;
    if (unresolvedConstraints == 0) {
      satisfied(null);
    } else {
      state = STATE_ENABLED;
      Activator.logDebug("Component enabled: " + toString() +
                         ", unresolvedConstraints=" + unresolvedConstraints);
      if (compDesc.getServices() != null) {
        // No satisfied, check if we have circular problems.
        // Only applicable if component registers a service.
        Activator.logDebug("Check circular: " + toString());
        String res = scr.checkCircularReferences(this, new ArrayList<Component>());
        if (res != null) {
          Activator.logError(bc, res, null);
        }
      }
    }
    d.resolve(null);
  }


  /**
   * Stop tracking services and CM config
   */
  private void untrackConstraints() {
    unresolvedConstraints = DISABLED_OFFSET;
    if (refs != null) {
      for (final Reference ref : refs) {
        ref.stop();
      }
    }
    cmConfig.unsubscribe();
  }


  /**
   * Resolved a constraint check satisfied.
   */
  void resolvedConstraint() {
    synchronized (lock) {
      if (--unresolvedConstraints == 0) {
        // TODO do we need to move this outside synchronized
        satisfied(null);
      }
    }
  }


  /**
   * Unresolved a constraint check unsatisfied.
   */
  void unresolvedConstraint(int reason) {
    synchronized (lock) {
      if (unresolvedConstraints++ == 0) {
        // TODO do we need to move this outside synchronized
        unsatisfied(reason);
      }
    }
  }


  String [] getConfigurationPid() {
    String [] res = new String[cmConfig.pids.length];
    for (int i = 0; i < res.length; i++) {
      res[i] = cmConfig.pids[i].pid;
    }
    return res;
  }


  /**
   * Handle CM config updates for Immediate- & Delayed-Components.
   * FactoryComponents have overridden method
   *
   */
  void cmConfigUpdated(String ccid, final boolean first) {
    // First mandatory config, remove constraint
    Activator.logDebug("cmConfigUpdate for ccid = " + ccid + ", comp=" +
                       toString() + " is first = " + first);
    ComponentConfiguration [] cc = null;
    Map<String, Object> dict = cmConfig.getProperties(ccid);
    synchronized (lock) {
      incrementCMRev(ccid);
      if (first) {
        cc = compConfigs.remove(NO_CCID);
      }
      if (refs != null) {
        for (final Reference ref : refs) {
          // TODO do we need to move this outside synchronized
          ref.update(ccid, dict, first);
        }
      }
      if (cc != null) {
        // We have a first config for component configuration
        // Connect ccid  and component config, ccid in CC
        // changes when update it
        compConfigs.put(ccid, cc);
      } else {
        cc = compConfigs.get(ccid);
      }
    }
    if (cc != null) {
      // We have an updated config, check it
      cc[0].cmConfigUpdated(ccid, dict);
    } else if (first && cmConfig.isRequired()) {
      resolvedConstraint();
    } else if (unresolvedConstraints == 0) {
      // New factory ccid
      ComponentConfiguration ncc = newComponentConfiguration(ccid, null);
      if (ncc != null) {
        activateComponentConfiguration(ncc, null);
      }
    }
  }


  void incrementCMRev(String ccid) {
    Integer i = cmRevs.get(ccid);
    int cnt = i == null ? 0 : i.intValue();
    cmRevs.put(ccid, Integer.valueOf(cnt + 1));
  }


  void resetCMRev(String ccid) {
    cmRevs.remove(ccid);
  }


  /**
   * Handle CM config deletes for Immediate- & Delayed-Components.
   * FactoryComponents have overridden method
   *
   */
  void cmConfigDeleted(String ccid) {
    Activator.logDebug("ConfigurationEvent deleted, ccid=" + ccid + ", comp=" + toString());
    ComponentConfiguration [] cc;
    synchronized (lock) {
      resetCMRev(ccid);
      if (refs != null) {
        for (final Reference ref : refs) {
          ref.remove(ccid);
        }
      }
      if (!cmConfig.isSatisfied()) {
        if (cmConfig.isRequired()) {
          if (unresolvedConstraints++ == 0) {
            // TODO do we need to move this outside synchronized
            unsatisfied(ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);
            return;
          }
        }
      }
      // We move cc here since cmConfigUpdate loses old ccid info
      cc = compConfigs.remove(ccid);
      compConfigs.put(NO_CCID, cc);
    }
    if (cc != null) {
      cc[0].cmConfigUpdated(NO_CCID, null);
    }
  }


  /**
   * The tracked reference has become available.
   * Check if component becomes satisfied. If already
   * satisfied and reference is dynamic, then bind.
   *
   * @return True, if component was satisfied otherwise false.
   */
  boolean refAvailable(Reference r) {
    if (!r.isRefOptional()) {
      synchronized (lock) {
        if (--unresolvedConstraints == 0) {
          // TODO do we need to move this outside synchronized
          satisfied(null);
          return true;
        }
      }
    }
    return false;
  }


  /**
   * The tracked reference has become unavailable.
   */
  boolean refUnavailable(Reference r) {
    Activator.logDebug("Reference unavailable, unresolved=" + unresolvedConstraints);
    if (!r.isRefOptional()) {
      synchronized (lock) {
        if (unresolvedConstraints++ == 0) {
          // TODO do we need to move this outside synchronized
          unsatisfied(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Create new ComponentConfigurations that are missing for this component.
   * There should be one for each CM config available, if no config is available
   * there should be only one.
   *
   */
  private ComponentConfiguration [] newComponentConfigurations() {
    final ArrayList<ComponentConfiguration> res = new ArrayList<ComponentConfiguration>();
    if (!cmConfig.isEmpty()) {
      for (final String ccid : cmConfig.getCCIds()) {
        if (compConfigs.get(ccid) == null) {
          ComponentConfiguration cc = newComponentConfiguration(ccid, null);
          if (cc != null) {
            res.add(cc);
          }
        }
      }
    } else if (compConfigs.get(NO_CCID) == null) {
      res.add(newComponentConfiguration(NO_CCID, null));
    }
    return res.toArray(new ComponentConfiguration [res.size()]);
  }


  /**
   * Create a new ComponentConfiguration.
   */
  ComponentConfiguration newComponentConfiguration(String ccId,
                                                   Dictionary<String, Object> instanceProps)
  {
    if (refs != null && !ccId.equals(NO_CCID) && instanceProps == null) {
      for (final Reference ref : refs) {
        ref.updateNoPid(ccId);
        if (ref.getListener(ccId).numAvailable() < ref.getMinimumCardinality()) {
          return null;
        }
      }
    }
    Map<String, Object> props = cmConfig.getProperties(ccId);
    ComponentConfiguration cc;
    final String scope = getScope();
    if (Constants.SCOPE_SINGLETON.equals(scope)) {
      cc = new SingletonComponentConfiguration(this, ccId, props, instanceProps);
    } else if (Constants.SCOPE_BUNDLE.equals(scope)) {
      cc = new FactoryComponentConfiguration(this, ccId, props);
    } else if (Constants.SCOPE_PROTOTYPE.equals(scope)) {
      cc = new PrototypeComponentConfiguration(this, ccId, props);
    } else {
      throw new RuntimeException("Internal error");
    }
    synchronized (compConfigs) {
      ComponentConfiguration [] next;
      ComponentConfiguration [] old = compConfigs.get(ccId);
      
      if (old != null) {
        next = new ComponentConfiguration [old.length + 1];
        int i = -1 - Arrays.binarySearch(old, cc);
        if (i > 0) {
          System.arraycopy(old, 0, next, 0, i);
        }
        if (i < old.length) {
          System.arraycopy(old, i, next, i+1, old.length - i);
        }
        next[i] = cc;
      } else {
        next = new ComponentConfiguration [] { cc };
      }
      compConfigs.put(ccId, next);
    }
    return cc;
  }

  /**
   * Dispose of a created ComponentConfiguration.
   */
  private void disposeComponentConfigs(int reason) {
    final ArrayList<ComponentConfiguration []> cl = new ArrayList<ComponentConfiguration []>(compConfigs.values());
    for (final ComponentConfiguration [] componentConfigurations : cl) {
      for (final ComponentConfiguration cc : componentConfigurations) {
        cc.dispose(reason, true);
      }
    }
  }

  /**
   * Remove mapping for ComponentConfiguration to this Component.
   */
  ComponentConfiguration [] removeComponentConfiguration(ComponentConfiguration cc, int reason) {
    synchronized (compConfigs) {
      ComponentConfiguration [] ccs = compConfigs.remove(cc.getCCId());
      if (ccs != null && ccs.length > 1) {
        int i = Arrays.binarySearch(ccs, cc);
        ComponentConfiguration [] next = new ComponentConfiguration [ccs.length - 1];
        if (i > 0) {
          System.arraycopy(ccs, 0, next, 0, i);
        }
        if (i < next.length) {
          System.arraycopy(ccs, i + 1, next, i, next.length - i);
        }
        compConfigs.put(cc.getCCId(), next);
      }
    }
    Activator.logDebug("Component Config removed, check if still satisfied: " + getName() + ", " + state);
    if (keepService(reason)) {
      // If still satisfied, create missing component configurations
      return satisfied(cc);
    }
    return null;
  }

  /**
   * Check if component still is satisfied and the deactivation reason is
   * one of REFERENCE, CONFIGURATION_MODIFIED, CONFIGURATION_DELETED or
   * COMPONENT_DEACTIVATED.
   *
   * @param reason Deactivation reason code
   * @return True if we should keep component configuration service.
   */
  boolean keepService(int reason) {
    return isSatisfied() &&
        (reason == ComponentConstants.DEACTIVATION_REASON_REFERENCE ||
         reason == ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED ||
         reason == ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED ||
         reason == KF_DEACTIVATION_REASON_COMPONENT_DEACTIVATED);
  }


  /**
   * Get ComponentConfiguration belonging to specified serviceReference.
   *
   * @param sr ServiceReference belonging to component configuration
   *           we are searching for.
   */
  ComponentConfiguration getComponentConfiguration(ServiceReference<?> sr) {
    synchronized (compConfigs) {
      for (final ComponentConfiguration [] ccs : compConfigs.values()) {
        for (final ComponentConfiguration cc : ccs) {
          if (cc.getServiceReference() == sr) {
            return cc;
          }
        }
      }
    }
    return null;
  }


  /**
   * Get Implementation class for this component.
   */
  synchronized Class<?> getImplementation() {
    if (componentClass == null) {
      final String impl = compDesc.getImplementation();
      try {
        componentClass = compDesc.bundle.loadClass(impl);
      } catch (final ClassNotFoundException e) {
        final String msg = "Could not find class " + impl;
        Activator.logError(bc, msg, e);
        throw new ComponentException(msg, e);
      }
      getMethods();
    }
    return componentClass;
  }


  /**
   * Helper method for getMethods. Saves method name to method mapping
   * during search for methods.
   *
   */
  void saveMethod(HashMap<String, ComponentMethod[]> map, String key, ComponentMethod cm) {
    final ComponentMethod [] o = map.get(key);
    final int olen = o != null ? o.length : 0;
    final ComponentMethod [] n = new ComponentMethod [olen + 1];
    n[olen] = cm;
    if (o != null) {
      System.arraycopy(o, 0, n, 0, o.length);
    }
    map.put(key, n);
  }


  /**
   * Scan through class stack for methods
   */
  void scanForMethods(HashMap<String, ComponentMethod[]> lookFor)
  {
    final int minor = compDesc.getScrNSminor();
    Class<?> clazz = componentClass;
    while (clazz != null && !lookFor.isEmpty()) {
      final Method[] methods = clazz.getDeclaredMethods();
      final HashMap<String, ComponentMethod[]> nextLookfor
        = new HashMap<String, ComponentMethod[]>(lookFor);
      for (final Method m : methods) {
        final ComponentMethod[] cm = lookFor.get(m.getName());
        if (cm != null) {
          for (final ComponentMethod element : cm) {
            if (element.updateMethod(minor, m, clazz)) {
              // Found one candidate, don't look in superclass
              nextLookfor.remove(m.getName());
            }
          }
        }
      }
      clazz = clazz.getSuperclass();
      lookFor = nextLookfor;
    }
  }

  /**
   * Get all references for this component.
   */
  Reference [] getRawReferences() {
    return refs;
  }

  /**
   * Is this component enabled
   */
  boolean isEnabled() {
    return state >= STATE_ENABLED;
  }

  /**
   * Is this component satisfied
   */
  boolean isSatisfied() {
    return state == STATE_SATISFIED;
  }

  /**
   * Get all ComponentConfigurations.
   *
   */
  Collection<ComponentConfiguration> getComponentConfigurations() {
    Collection<ComponentConfiguration> res = new ArrayList<ComponentConfiguration>();
    synchronized (compConfigs) {
      for (final ComponentConfiguration [] ccs : compConfigs.values()) {
        Collections.addAll(res, ccs);
      }
    }
    return res;
  }

  //
  // Private
  //

  /**
   * Get first ComponentConfiguration
   */
  private ComponentConfiguration getFirstComponentConfiguration() {
    final Iterator<ComponentConfiguration []> cci = compConfigs.values().iterator();
    return cci.hasNext() ? cci.next()[0] : null;
  }

  /**
   * Get all activate, deactivate, modify, bind and unbind
   * methods specified for this component. This is done
   * the first time this method is called.
   *
   * @param clazz Implementation class to look for methods on.
   */
  private void getMethods() {
    final HashMap<String, ComponentMethod[]> lookFor = new HashMap<String, ComponentMethod[]>();
    String name = compDesc.getActivateMethod();
    activateMethod = new ComponentMethod(name, this, false);
    saveMethod(lookFor, name , activateMethod);
    name = compDesc.getDeactivateMethod();
    deactivateMethod = new ComponentMethod(name, this, true);
    saveMethod(lookFor, name, deactivateMethod);
    name = compDesc.getModifiedMethod();
    if (name != null) {
      modifiedMethod = new ComponentMethod(name, this, false);
      saveMethod(lookFor, name, modifiedMethod);
    }
    if (refs != null) {
      for (final Reference r : refs) {
        ComponentMethod method = r.getBindMethod();
        if (method != null) {
          saveMethod(lookFor, r.refDesc.bind, method);
        }
        method = r.getUnbindMethod();
        if (method != null) {
          saveMethod(lookFor, r.refDesc.unbind, method);
        }
      }
    }
    scanForMethods(lookFor);
    if (activateMethod.isMissing(false) && !compDesc.isActivateMethodSet()) {
      activateMethod = null;
    }
    if (deactivateMethod.isMissing(false) && !compDesc.isDeactivateMethodSet()) {
      deactivateMethod = null;
    }
  }

}
