/*
 * Copyright (c) 2010-2012, KNOPFLERFISH project
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

import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.component.*;


public class ComponentConfiguration implements ServiceFactory {

  final static int STATE_ACTIVATING = 0;
  final static int STATE_REGISTERED = 1;
  final static int STATE_ACTIVE = 2;
  final static int STATE_DEACTIVATING = 3;
  final static int STATE_DEACTIVE = 4;

  final Component component;
  private PropertyDictionary ccProps = null;
  private Dictionary sProps = null;
  private String cmPid = null;
  private Dictionary cmDict = null;
  private Dictionary instanceProps;
  private ServiceRegistration serviceRegistration = null;
  private ComponentContextImpl componentContext = null;
  private Hashtable /* Bundle -> ComponentContextImpl */ factoryContexts;
  private volatile boolean unregisterInProgress = false;
  private volatile int activeCount = 0;
  private volatile int state = STATE_ACTIVATING;

  /**
   *
   */
  ComponentConfiguration(Component c, String cmPid, Dictionary cmDict,
                         Dictionary sProps, Dictionary instanceProps) {
    this.component = c;
    this.cmPid = cmPid;
    this.cmDict = cmDict;
    this.sProps = sProps;
    this.instanceProps = instanceProps;
    factoryContexts = c.compDesc.isServiceFactory() ? new Hashtable() : null;
    Activator.logDebug("Created " + toString());
  }


  /**
   *
   */
  public String toString() {
    return "[ComponentConfiguration, component#" + component.id +
      ", name = " + component.compDesc.getName() + "]";
  }


  /**
   * Activates a component configuration.
   *
   */
  ComponentContextImpl activate(final Bundle usingBundle, final boolean incrementActive) {
    ComponentContextImpl res;
    Class cclass = null;
    synchronized (this) {
      while (state == STATE_DEACTIVATING) {
        try {
          wait();
        } catch (InterruptedException _ignore) { }
      }
      res = factoryContexts != null ?
        (ComponentContextImpl)factoryContexts.get(usingBundle) :
        componentContext;
      if (res == null) {
        state = STATE_ACTIVATING;
        cclass = component.getImplementation();
        res = new ComponentContextImpl(this, usingBundle);
        if (factoryContexts != null) {
          factoryContexts.put(usingBundle, res);
        } else {
          componentContext = res;
        }
      }
    }
    if (cclass != null) {
      Activator.logDebug("CC.active start activate, this=" + this +
                         ", activateCount=" + activeCount);
      component.getMethods(cclass);
      try {
        res.setInstance(cclass.newInstance());
      }  catch (Exception e) {
        if (factoryContexts != null) {
          factoryContexts.remove(usingBundle);
        } else {
          componentContext = null;
        }
        res.activationResult(false);
        Activator.logError(component.bc, "Failed to instanciate: " + cclass, e);
        throw new ComponentException("Failed to instanciate: " + cclass, e);
      }
      try {
        bindReferences(res);
        if (component.activateMethod != null) {
          ComponentException ce = component.activateMethod.invoke(res);
          if (ce != null) {
            throw ce;
          }
        }
        synchronized (this) {
          if (incrementActive) {
            activeCount++;
          }
          state = STATE_ACTIVE;
          res.activationResult(true);
        }
      } catch (RuntimeException e) {
        unbindReferences(res);
        if (factoryContexts != null) {
          factoryContexts.remove(usingBundle);
        } else {
          componentContext = null;
        }
        res.activationResult(false);
        throw e;
      }
      return res;
    } else {
      Activator.logDebug("CC.active activate in progress wait, this=" + this +
                         ", activateCount=" + activeCount);
      if (res.waitForActivation()) {
        synchronized (this) {
          // Check that the context is still active
          if (!res.isActive()) {
            throw new ComponentException("Waited for component activation that was deactivated: " + this);
          }
          if (incrementActive) {
            activeCount++;
          }
          return res;
        }
      } else {
        return activate(usingBundle, incrementActive);
      }
    }
  }


  /**
   * Deactivates a component configuration.
   *
   */
  void deactivate(ComponentContextImpl cci, int reason, boolean disposeIfLast) {
    Activator.logDebug("CC.deactive this=" + this +
                       ", activateCount=" + activeCount);
    if (!cci.waitForActivation()) {
      // Deactivate an already deactivated
      return;
    }
    boolean last = false;
    synchronized (this) {
      if (!cci.isActive()) {
        // Deactivate an already deactivated
        return;
      }
      if (factoryContexts != null && factoryContexts.containsValue(cci)) {
        if (factoryContexts.isEmpty()) {
          last = true;
        }
        activeCount--;
      } else if (componentContext == cci) {
        last = true;
      } else {
        // Deactivate an already deactivated
        return;
      }
      if (last && disposeIfLast) {
        state = STATE_DEACTIVATING;
      }
      cci.setDeactive();
    }
    if (last && disposeIfLast) {
      Activator.logDebug("CC.deactive last, dispose this=" +
                         this + ", unregisterInProgress=" +
                         unregisterInProgress);
      if (unregisterInProgress) {
        // We are already disposing
        return;
      }
      unregisterService();
    }
    if (component.deactivateMethod != null) {
      component.deactivateMethod.invoke(cci, reason);
    }
    unbindReferences(cci);
    synchronized (this) {
      if (componentContext == cci) {
        componentContext = null;
      } else {
        factoryContexts.remove(cci);
      }
    }
    if (last && disposeIfLast) {
      synchronized (this) {
        state = STATE_DEACTIVE;
        notifyAll();
      }
      // TODO: Do we need to synchronize this?
      component.removeComponentConfiguration(this, reason);
    }
  }


  /**
   * Dispose of this component configuration.
   *
   */
  void dispose(int reason) {
    if (unregisterInProgress) {
      // We are already disposing
      return;
    }
    Activator.logDebug("CC.dispose this=" + this + ", reason=" + reason);
    ComponentContextImpl [] cci;
    synchronized (this) {
      state = STATE_DEACTIVATING;
      cci = getAllContexts();
      // Mark all as deactivated?
      activeCount = 0;
    }
    unregisterService();
    if (cci.length > 0) {
      for (int i = 0; i < cci.length; i++) {
        deactivate(cci[i], reason, true);
      }
    } else {
      // TODO: Do we need to synchronize this?
      synchronized (this) {
        state = STATE_DEACTIVE;
        notifyAll();
      }
      component.removeComponentConfiguration(this, reason);
    }
  }


  /**
   * Get CM pid used for this component configuration or NO_PID
   * if no CM data is used.
   *
   */
  String getCMPid() {
    return cmPid;
  }


  /**
   * Get state of Component Configuration.
   */
  int getState() {
    return state;
  }


  /**
   * Get component configuration properties.
   *
   */
  synchronized PropertyDictionary getProperties() {
    if (ccProps == null) {
      ccProps = new PropertyDictionary(component, cmDict, instanceProps, false);
    }
    return ccProps;
  }
  

  /**
   * Get service properties for this component configuration. Which are
   * the component configuration properties minus all properties starting
   * with a period ".".
   *
   */
  private PropertyDictionary getServiceProperties() {
    return new PropertyDictionary(component, cmDict, instanceProps, true);
  }
  

  /**
   *
   */
  void registerService() {
    final ComponentDescription cd = component.compDesc;
    String [] services = cd.getServices();
    if (services != null) {
      Dictionary sp;
      if (sProps != null) {
        sp = sProps;
        sProps = null;
      } else {
        sp = getServiceProperties();
      }
      serviceRegistration = component.bc.registerService(services, this, sp);
      synchronized (this) {
        if (state == STATE_ACTIVATING) {
          state = STATE_REGISTERED;
        }
      }
    }
  }


  /**
   * Update the service properties for service registered
   * for this component configuration.
   *
   */
  private void modifyService() {
    if (serviceRegistration != null) {
      serviceRegistration.setProperties(getServiceProperties());
    }
  }


  /**
   * Unregister service registered for this component configuration.
   *
   */
  private void unregisterService() {
    if (serviceRegistration != null) {
      unregisterInProgress = true;
      try {
        serviceRegistration.unregister();
      } catch (IllegalStateException ignored) {
        // Nevermind this, it might have been unregistered previously.
      }
      serviceRegistration = null;
      unregisterInProgress = false;
    }
  }


  /**
   * Get ComponentContext for component configuration.
   */
  ComponentContextImpl getContext(Bundle b) {
    ComponentContextImpl cci = factoryContexts != null ?
      (ComponentContextImpl)factoryContexts.get(b) : componentContext;
    return cci != null && cci.isActive() ? cci : null;
  }


  /**
   * Get service reference for service registered by this
   * component configuration. 
   */
  ServiceReference getServiceReference() {
    if (serviceRegistration != null) {
      return serviceRegistration.getReference();
    }
    return null;
  }


  /**
   * CM data for this component has changed. Check if we need to change state
   * for the component configuration. Also call modified method when its is specified.
   * 
   */
  void cmConfigUpdated(String pid, Dictionary dict) {
    Activator.logDebug("CC.cmConfigUpdated, " + toString() + ", pid=" + pid + ", activeCount=" + activeCount);
    int disposeReason = -1;
    ComponentContextImpl [] cci = null;
    synchronized (this) {
      cmPid = pid;
      cmDict = dict;
      ccProps = null;
      sProps = null;
      if (state == STATE_ACTIVE) { // TODO check this for FactoryComponents
        if (dict == null) {
          // Always dispose when an used config is deleted!?
          disposeReason = ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED;
        } else if (component.modifiedMethod == null ||
                   component.modifiedMethod.isMissing(true)) {
          // Dispose when we have no modify method
          disposeReason = ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED;
        } else {
          cci = getActiveContexts();
        }
      } else {
        return;
      }
    }
    if (cci != null) {
      for (int i = 0; i < cci.length; i++) {
        component.modifiedMethod.invoke(cci[i]);
      }
      modifyService();
    } else {
      dispose(disposeReason);
    }
  }


  /**
   * The tracked reference has changed.
   *
   */
  void refUpdated(ReferenceListener rl, ServiceReference s, boolean deleted, boolean wasSelected) {
    Activator.logDebug("CC.refUpdate, " + toString() + ", " + rl + ", deleted=" + deleted +
                       ", wasSelected=" + wasSelected);
    if (state == STATE_ACTIVE) { // TODO check this for FactoryComponents
      if (rl.isDynamic()) {
        if (rl.isMultiple()) {
          if (deleted) {
            unbindReference(rl, s);
          } else {
            bindReference(rl, s);
          }
        } else {
          if (wasSelected) { // Implies deleted
            ServiceReference newS = rl.getServiceReference();
            if (newS != null) {
              bindReference(rl, newS);
            }
            unbindReference(rl, s);
          } else if (!deleted) {
            if (s == rl.getServiceReference()) {
              bindReference(rl, s);
            }
          }
        }
      } else {
        // If it is a service we use, stop and check if we should restart
        if (rl.isMultiple() || wasSelected) {
          dispose(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
        }
      }
    } else {
      if (deleted) {
        synchronized (this) {
          // Keep service alive if we are deactivating
          if (!rl.isOptional() || !rl.isDynamic()) {
            while (state == STATE_DEACTIVATING) {
              try {
                wait();
              } catch (InterruptedException _ignore) { }
            }
          } else {
            // If we have an dynamic optional reference, unbind it
            // directly to avoid circular deactivation.
            if (state == STATE_DEACTIVATING) {
              unbindReference(rl, s);
            }
          }
        }
      }
    }

  }

  //
  // ServiceFactory interface
  //

  /**
   * Get service registered for component
   *
   */
  public Object getService(Bundle usingBundle,
                           ServiceRegistration reg) {
    Activator.logDebug("CC.getService(), " + toString() + ", activeCount = " + activeCount);
    ComponentContextImpl cci = activate(usingBundle, true);
    return cci.getComponentInstance().getInstance();
  }


  /**
   * Release service previously gotten via getService.
   *
   */
  public void ungetService(Bundle usingBundle,
                           ServiceRegistration reg,
                           Object obj) {
    if (!unregisterInProgress) {
      Activator.logDebug("CC.ungetService(), " + toString() + ", activeCount = " + activeCount);
      ComponentContextImpl cci = getContext(usingBundle);
      if (cci != null) {
        boolean doDeactivate = false;
        synchronized (this) {
          if (factoryContexts != null || activeCount == 1) {
            doDeactivate = true;
          } else {
            activeCount--;
          }
        }
        if (doDeactivate) {
          deactivate(cci, Component.KF_DEACTIVATION_REASON_COMPONENT_DEACTIVATED, true);
        }
      }
    }
  }

  //
  // Private
  //

  /**
   */
  private ComponentContextImpl [] getActiveContexts() {
    ComponentContextImpl [] res;
    if (factoryContexts != null) {
      Hashtable snapshot = (Hashtable)factoryContexts.clone();
      ArrayList active = new ArrayList(snapshot.size());
      for (Enumeration e = snapshot.elements(); e.hasMoreElements(); ) {
        ComponentContextImpl cci = (ComponentContextImpl)e.nextElement();
        if (cci.isActive()) {
          active.add(cci);
        }
      }
      res = new ComponentContextImpl[active.size()];
      active.toArray(res);
    } else if (componentContext != null && componentContext.isActive()) {
      res = new ComponentContextImpl [] { componentContext };
    } else {
      res = new ComponentContextImpl[0];
    }
    return res;
  }


  /**
   */
  private ComponentContextImpl [] getAllContexts() {
    ComponentContextImpl [] res;
    if (factoryContexts != null) {
      Hashtable snapshot = (Hashtable)factoryContexts.clone();
      res = new ComponentContextImpl[snapshot.size()];
      snapshot.values().toArray(res);
    } else if (componentContext != null) {
      res = new ComponentContextImpl [] { componentContext };
    } else {
      res = new ComponentContextImpl[0];
    }
    return res;
  }


  /**
   * Bind a component to its references. The references are bound
   * in order specified by the component description.
   */
  void bindReferences(ComponentContextImpl cci) {
    Reference [] refs = component.getRawReferences();
    if (refs != null) {
      for (int i = 0; i < refs.length; i++) {
        ReferenceListener rl = refs[i].getListener(getCMPid());
        if (rl.isAvailable()) {
          if (!cci.bind(rl) && !refs[i].isOptional()) {
            throw new ComponentException("Failed to bind: " + rl);
          }
        }
      }
    }
  }


  /**
   * Unbind a component from its references. The references are
   * unbound in reverse order specified by the component description.
   */
  private void unbindReferences(ComponentContextImpl cci) {
    Reference [] refs = component.getRawReferences();
    if (refs != null) {
      for (int i = refs.length - 1; i >= 0; i--) {
        cci.unbind(refs[i].refDesc.name);
      }
    }
  }


  /**
   *
   */
  private void bindReference(ReferenceListener rl, ServiceReference s) {
    ComponentContextImpl [] cci = getActiveContexts();
    for (int i = 0; i < cci.length; i++) {
      cci[i].bind(rl, s);
    }
  }


  /**
   *
   */
  private void unbindReference(ReferenceListener rl, ServiceReference s) {
    // This is only called for dynamic refs, so we also unbind
    // services during deactivation
    ComponentContextImpl [] cci = getAllContexts();
    for (int i = 0; i < cci.length; i++) {
      cci[i].unbind(rl, s);
    }
  }

}
