/*
 * Copyright (c) 2006-2012, KNOPFLERFISH project
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


public abstract class Component implements org.apache.felix.scr.Component {

  final static String NO_PID = "";
  final private static int DISABLED_OFFSET = -999999999;
  final private static int STATE_DISPOSED = 0;
  final private static int STATE_DISPOSING = 1;
  final private static int STATE_DISABLED = 2;
  final private static int STATE_DISABLING = 3;
  final private static int STATE_ENABLED = 4;
  final private static int STATE_ENABLING = 5;
  final private static int STATE_SATISFIED = 6;

  final SCR scr;
  final ComponentDescription compDesc;
  final BundleContext bc;
  final Long id;
  /**
   * UnresolvedConstraints is the number of unsatisfied contraints,
   * a negative value means that we are in the process of enabling
   * and calculating constraints.
   */
  private int unresolvedConstraints;
  HashMap /* String -> Dictionary */ cmDicts;
  HashMap /* String -> PropertyDictionary */ servProps = null;
  HashMap /* String -> ComponentConfiguration */ compConfigs = new HashMap();
  boolean cmConfigOptional;
  ComponentMethod activateMethod;
  ComponentMethod deactivateMethod;
  ComponentMethod modifiedMethod = null;
  private transient int state = 0;
  private volatile boolean getMethodsDone = false;
  private Reference [] refs = null;
  private Object lock = new Object();

  /**
   *
   */
  Component(SCR scr, ComponentDescription cd, Long id) {
    this.scr = scr;
    this.bc = cd.bundle.getBundleContext();
    this.compDesc = cd;
    this.id = id;
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
        ComponentConfiguration cc = getFirstComponentConfiguration();
        if (cc != null) {
          return org.apache.felix.scr.Component.STATE_DEACTIVATING;
        }
        return org.apache.felix.scr.Component.STATE_UNSATISFIED;
      }
    case STATE_SATISFIED:
      if (this instanceof FactoryComponent &&
          ((FactoryComponent)this).hasFactoryService()) {
        return org.apache.felix.scr.Component.STATE_FACTORY;
      } else {
        ComponentConfiguration cc = getFirstComponentConfiguration();
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
      throw new RuntimeException("Interal Error, state = " + state);
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
    String[] res = (String[])compDesc.getServices();
    return res != null ? (String[])res.clone() : null;
  }


  /**
   * @see org.apache.felix.scr.Component.getProperties
   */
  public Dictionary getProperties() {
    ComponentConfiguration cc = getFirstComponentConfiguration();
    if (cc != null) {
      return (Dictionary)cc.getProperties();
    }
    return (Dictionary)compDesc.getProperties().clone();
  }


  /**
   * @see org.apache.felix.scr.Component.getReferences
   */
  public org.apache.felix.scr.Reference [] getReferences() {
    if (refs != null) {
      return (org.apache.felix.scr.Reference [])refs.clone();
    }
    return null;
  }


  /**
   * @see org.apache.felix.scr.Component.getComponentInstance
   */
  public ComponentInstance getComponentInstance() {
    ComponentConfiguration cc = getFirstComponentConfiguration();
    if (cc != null) {
      // TODO: what about factories
      ComponentContext ctxt = cc.getContext(null);
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
    Activator.logInfo(bc, "Enable " + toString());
    synchronized (lock) {
      if (!isEnabled()) {
        enableTrackConstraints();
      }
    }
  }

  /**
   * Disable component. Dispose of all ComponentConfigurations and
   * stop listening for constraint changes.
   *
   * @see org.apache.felix.scr.Component.disable
   */
  public void disable() {
    disable(ComponentConstants.DEACTIVATION_REASON_DISABLED);
  }

  /**
   * Disable component. Dispose of all ComponentConfigurations and
   * stop listening for constraint changes.
   */
  void disable(int reason) {
    Activator.logInfo(bc, "Disable " + toString());
    synchronized (lock) {
       boolean dispose =  reason == ComponentConstants.DEACTIVATION_REASON_DISPOSED ||
         reason == ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED;
      if (isEnabled()) {
        state = dispose ? STATE_DISPOSING : STATE_DISABLING;
        untrackConstraints();
        disposeComponentConfigs(reason);
        refs = null;
        cmDicts = null;
        state = dispose ? STATE_DISPOSED : STATE_DISABLED;
      } else if (dispose && state == STATE_DISABLED) {
        state = STATE_DISPOSED;
      }
    }
  }


  /**
   * Component is satisfied. Create ComponentConfiguration and
   * register service depending on component type.
   */
  void satisfied() {
    state = STATE_SATISFIED;
    subclassSatisfied();
  }

  abstract void subclassSatisfied();


  /**
   * Component is unsatisfied dispose of all ComponentConfiguration
   * for this component.
   */
  void unsatisfied(int reason) {
    Activator.logInfo(bc, "Unsatisfied: " + toString());
    state = STATE_ENABLED;
    disposeComponentConfigs(reason);
  }


  /**
   * Start tracking services and CM config
   */
  private void enableTrackConstraints() {
    // unresolvedConstraints set to DISABLED_OFFSET, so that we don't satisfy until
    // end of this method
    unresolvedConstraints = DISABLED_OFFSET;
    state = STATE_ENABLING;
    int policy = compDesc.getConfigPolicy();
    Configuration [] config = null;
    if (policy == ComponentDescription.POLICY_IGNORE) {
      cmDicts = null;
    } else {
      cmDicts = new HashMap();
      cmConfigOptional = policy == ComponentDescription.POLICY_OPTIONAL;
      config = scr.subscribeCMConfig(this);
      if (config != null) {
        for (int i = 0; i < config.length; i++) {
          cmDicts.put(config[i].getPid(), config[i].getProperties());
        }
      } else if (!cmConfigOptional) {
        // If we have no mandatory CM data, add constraint
        unresolvedConstraints++;
      }
    }
    ArrayList rds = compDesc.getReferences();
    if (rds != null) {
      unresolvedConstraints += rds.size();
      refs = new Reference[rds.size()];
      for (int i = 0; i < refs.length; i++) {
        Reference r = new Reference(this, (ReferenceDescription)rds.get(i));
        refs[i] = r;
        if (r.isOptional()) {
          // Optional references does not need to be check
          // if they are available
          unresolvedConstraints--;
        }
        r.start(config);
      }
    } 
    // Remove blocking constraint, to see if we are satisfied
    unresolvedConstraints -= DISABLED_OFFSET;
    if (unresolvedConstraints == 0) {
      satisfied();
    } else {
      state = STATE_ENABLED;
      if (compDesc.getServices() != null) {
        // No satisfied, check if we have circular problems.
        // Only applicable if register a service.
        Activator.logDebug("Check circular: " + toString());
        String [] pids = getAllServicePids();
        // Loop through service property configurations
        String res;
        for (int px = 0; px < pids.length; px++) {
          res = scr.checkCircularReferences(this, pids[px], new ArrayList());
          if (res != null) {
            Activator.logError(bc, res, null);
            break;
          }
        }
      }
    }
  }

  /**
   * Stop tracking services and CM config
   */
  private void untrackConstraints() {
    unresolvedConstraints = DISABLED_OFFSET;
    if (refs != null) {
      for (int i = 0; i < refs.length; i++) {
        refs[i].stop();
      }
    }
    scr.unsubscribeCMConfig(this);
  }


  /**
   * Resolved a constraint check satisfied.
   */
  void resolvedConstraint() {
    synchronized (lock) {
      if (--unresolvedConstraints == 0) {
        // TODO do we need to move this outside synchronized
        satisfied();
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


  /**
   * Handle CM config updates for Immediate- & Delayed-Components.
   * FactoryComponents have overridden method
   *
   */
  void cmConfigUpdated(String pid, Configuration c) {
    Activator.logDebug("cmConfigUpdate for pid = " + pid +
                       " is first = " + cmDicts.isEmpty());
    // First mandatory config, remove constraint
    boolean first = cmDicts.isEmpty() && !cmConfigOptional;
    cmDicts.put(pid, c.getProperties());
    // Discard cached service props
    final HashMap sp = servProps;
    if (sp != null) {
      sp.remove(pid);
    }
    ComponentConfiguration cc;
    synchronized (lock) {
      cc = (ComponentConfiguration)compConfigs.remove(NO_PID);
      if (refs != null) {
        for (int i = 0; i < refs.length; i++) {
          // TODO do we need to move this outside synchronized
          refs[i].update(c, cc != null);
        }
      }
      if (cc != null) {
        // We have a first config for component configuration
        // Connect pid  and component config, pid in CC
        // changes when update it
        compConfigs.put(pid, cc);
      } else {
        cc = (ComponentConfiguration)compConfigs.get(pid);
      }
    }
    if (cc != null) {
      // We have an updated config, check it
      cc.cmConfigUpdated(pid, c.getProperties());
    } else if (first) {
      resolvedConstraint();
    } else if (unresolvedConstraints == 0) {
      // New factory pid
      satisfied();
    }
  }


  /**
   * Handle CM config deletes for Immediate- & Delayed-Components.
   * FactoryComponents have overridden method
   *
   */
  void cmConfigDeleted(String pid) {
    cmDicts.remove(pid);
    final HashMap sp = servProps;
    if (sp != null) {
      sp.remove(pid);
    }
    ComponentConfiguration cc;
    synchronized (lock) {
      if (refs != null) {
        for (int i = 0; i < refs.length; i++) {
          refs[i].remove(pid);
        }
      }
      if (cmDicts.isEmpty()) {
        if (!cmConfigOptional) {
          if (unresolvedConstraints++ == 0) {
            // TODO do we need to move this outside synchronized
            unsatisfied(ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);
            return;
          }
        }
      }
      // We remove cc here since cmConfigUpdate loses old pid info
      cc = (ComponentConfiguration)compConfigs.remove(pid);
    }
    if (cc != null) {
      cc.cmConfigUpdated(NO_PID, null);
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
    if (!r.isOptional()) {
      synchronized (lock) {
        if (--unresolvedConstraints == 0) {
          // TODO do we need to move this outside synchronized
          satisfied();
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
    if (!r.isOptional()) {
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
  ComponentConfiguration [] newComponentConfiguration() {
    ArrayList res = new ArrayList();
    if (cmDicts != null && !cmDicts.isEmpty()) {
      String [] pids = (String [])cmDicts.keySet().toArray(new String [cmDicts.size()]);
      for (int i = 0; i < pids.length; i++) {
        if (compConfigs.get(pids[i]) == null) {
          res.add(newComponentConfiguration(pids[i], null));
          if (refs != null) {
            for (int j = 0; j < refs.length; j++) {
              refs[j].updateNoPid(pids[i]);
            }
          }
        }
      }
    } else if (compConfigs.get(NO_PID) == null) {
      res.add(newComponentConfiguration(NO_PID, null));
    }
    // Release servProps since we don't need them anymore
    servProps = null;
    return (ComponentConfiguration [])res.toArray(new ComponentConfiguration [res.size()]);
  }


  /**
   * Create a new ComponentConfiguration
   */
  ComponentConfiguration newComponentConfiguration(String cmPid,
                                                   Dictionary instanceProps) {
    Dictionary cmDict = cmDicts != null ? (Dictionary)cmDicts.get(cmPid) : null;
    Dictionary sd = servProps != null ? (Dictionary)servProps.get(cmPid) : null;
    ComponentConfiguration cc = new ComponentConfiguration(this, cmPid, cmDict,
                                                           sd, instanceProps);
    compConfigs.put(cc.getCMPid(), cc);
    return cc;
  }


  /**
   * Dispose of a created ComponentConfiguration.
   */
  private void disposeComponentConfigs(int reason) {
    ArrayList cl = new ArrayList(compConfigs.values());
    for (Iterator i = cl.iterator(); i.hasNext(); ) {
      ((ComponentConfiguration)i.next()).dispose(reason);
    }
  }


  /**
   * Remove mapping for ComponentConfiguration to this Component.
   */
  void removeComponentConfiguration(ComponentConfiguration cc) {
    compConfigs.remove(cc.getCMPid());
    Activator.logDebug("Component Config removed, check if still satisfied: " + getName() + ", " + state);
    if (isSatisfied()) {
      // If still satisfied, create missing component configurations
      satisfied();
    }
  }


  /**
   * Get ComponentConfiguration belonging to specified serviceReference.
   *
   * @param sr ServiceReference belonging to component configuration
   *           we are searching for.
   */
  ComponentConfiguration getComponentConfiguration(ServiceReference sr) {
    for (Iterator i = compConfigs.values().iterator(); i.hasNext(); ) {
      ComponentConfiguration cc = (ComponentConfiguration)i.next();
      if (cc.getServiceReference() == sr) {
        return cc;
      }
    }
    return null;
  }


  /**
   * Get Implementation class for this component.
   */
  Class getImplementation() {
    String impl = compDesc.getImplementation();
    try {
      return compDesc.bundle.loadClass(impl);
    } catch (ClassNotFoundException e) {
      String msg = "Could not find class " + impl;
      Activator.logError(bc, msg, e);
      throw new ComponentException(msg, e);
    }
  }


  /**
   * Get all activate, deactivate, modify, bind and unbind
   * methods specified for this component. This is done
   * the first time this method is called.
   *
   * @param clazz Implemenation class to look for methods on.
   */
  synchronized void getMethods(Class clazz) {
    if (!getMethodsDone) {
      getMethodsDone = true;
      HashMap lookfor = new HashMap();
      String name = compDesc.getActivateMethod();
      activateMethod = new ComponentMethod(name, this, false);
      saveMethod(lookfor, name , activateMethod);
      name = compDesc.getDeactivateMethod();  
      deactivateMethod = new ComponentMethod(name, this, true);
      saveMethod(lookfor, name, deactivateMethod);
      name = compDesc.getModifiedMethod();
      if (name != null) {
        modifiedMethod = new ComponentMethod(name, this, false);
        saveMethod(lookfor, name, modifiedMethod);
      }
      if (refs != null) {
        for (int i = 0; i < refs.length; i++) {
          Reference r = refs[i];
          name = r.refDesc.bind;
          if (name != null) {
            r.bindMethod = new ComponentMethod(name, this, r);
            saveMethod(lookfor, name, r.bindMethod);
          }
          name = r.refDesc.unbind;
          if (name != null) {
            r.unbindMethod = new ComponentMethod(name, this, r);
            saveMethod(lookfor, name, r.unbindMethod);
          }
        }
      }
      boolean isSCR11 = compDesc.isSCR11();
      do {
        Method[] methods = clazz.getDeclaredMethods();
        HashMap nextLookfor = (HashMap)lookfor.clone();
        for (int i = 0; i < methods.length; i++) {
          Method m = methods[i];
          ComponentMethod [] cm = (ComponentMethod [])lookfor.get(m.getName());
          if (cm != null) {
            for (int j = 0; j < cm.length; j++) {
              if (cm[j].updateMethod(isSCR11, m, clazz)) {
                // Found one candidate, don't look in superclass
                nextLookfor.remove(m.getName());
              }
            }
          }
        }
        clazz = clazz.getSuperclass();
        lookfor = nextLookfor;
      } while (clazz != null && !lookfor.isEmpty());
      if (activateMethod.isMissing(false) && !compDesc.isActivateMethodSet()) {
        activateMethod = null;
      }
      if (deactivateMethod.isMissing(false) && !compDesc.isDeactivateMethodSet()) {
        deactivateMethod = null;
      }
    }
  }


  /**
   * Get all references for this component.
   */
  Reference [] getRawReferences() {
    return refs;
  }


  /**
   * Get service property dictionary that could
   * be registered by this component, based on
   * current CM data.
   */
  PropertyDictionary getServiceProperties(String pid) {
    PropertyDictionary pd;
    HashMap sp = servProps;
    if (sp != null) {
      pd = (PropertyDictionary)sp.get(pid);
      if (pd != null) {
        return pd;
      }
    } else {
      sp = servProps = new HashMap();
    }
    Dictionary cmDict =  cmDicts != null ? (Dictionary)cmDicts.get(pid) : null;
    pd = new PropertyDictionary(this, cmDict, null, true);
    sp.put(pid, pd);
    return pd;
  }


  /**
   * Get all service property dictionaries that could
   * be registered by this component, based on current
   * CM data.
   */
  String [] getAllServicePids() {
    String [] pids;
    if (cmDicts == null || cmDicts.isEmpty()) {
      pids = new String [] { NO_PID };
    } else {
      pids = (String [])cmDicts.keySet().toArray(new String [cmDicts.size()]);
    }
    return pids;
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

  //
  // Private
  //

  /**
   * Get first ComponentConfiguration
   */
  private ComponentConfiguration getFirstComponentConfiguration() {
    Iterator cci = compConfigs.values().iterator();
    return cci.hasNext() ? (ComponentConfiguration)cci.next() : null;
  }


  /**
   * Helper method for getMethods. Saves method name to method mapping
   * during search for methods.
   *
   */
  private void saveMethod(HashMap map, String key, ComponentMethod cm) {
    ComponentMethod [] o = (ComponentMethod [])map.get(key);
    int olen = o != null ? o.length : 0;
    ComponentMethod [] n = new ComponentMethod [olen + 1];
    n[olen] = cm;
    if (o != null) {
      System.arraycopy(o, 0, n, 0, o.length);
    }
    map.put(key, n);
  }
}
