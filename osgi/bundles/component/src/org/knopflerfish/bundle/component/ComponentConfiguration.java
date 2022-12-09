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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;

abstract class ComponentConfiguration implements Comparable<ComponentConfiguration> {

  final static int STATE_ACTIVATING = 0;
  final static int STATE_REGISTERED = 1;
  final static int STATE_ACTIVE = 2;
  final static int STATE_DEACTIVATING = 3;
  final static int STATE_DEACTIVE = 4;

  final Component component;
  final int id;

  private PropertyDictionary ccProps = null;
  private String ccId;
  private Map<String, Object> cmDict;
  private final Dictionary<String, Object> instanceProps;
  private ComponentService componentService = null;
  private ComponentService blockedService = null;
  private volatile boolean unregisterInProgress = false;
  private volatile int activeCount = 0;
  private volatile int state = STATE_ACTIVATING;
  private volatile ServiceEvent lastReactivateEvent = null;

  private volatile static int count = 0;
  private final static Object countLock = new Object();

  /**
   *
   */
  ComponentConfiguration(Component c,
                         String ccId,
                         Map<String, Object> cmDict,
                         Dictionary<String, Object> instanceProps)
  {
    this.component = c;
    this.ccId = ccId;
    this.cmDict = cmDict;
    this.instanceProps = instanceProps;
    synchronized (countLock) {
      this.id = count++;
    }
    Activator.logDebug("Created " + toString());
  }

  /**
   *
   */
  @Override
  public String toString() {
    return "[ComponentConfiguration, component#" + component.id +
      ", name = " + component.compDesc.getName() + "]";
  }


  /**
   * Activates a component configuration.
   *
   */
  ComponentContextImpl activate(final Bundle usingBundle,
                                final boolean incrementActive)
  {
    ComponentContextImpl res;
    ComponentException err = null;
    Class<?> cclass = null;
    synchronized (this) {
      // Components that use both factory service and CM managed service factory
      // may run into problem with concurrent activation and de-activation of
      // the component configurations. Thus if state is deactivating then we must
      // not wait for ever here.
      final long startTime = System.currentTimeMillis();
      while (state == STATE_DEACTIVATING) {
        if (System.currentTimeMillis() - startTime > 10000) {
          throw new ComponentException("Waited to long for component "
                                       +"de-activation during activate: "
                                       + this);
        }
        try {
          wait(1000L);
        } catch (final InterruptedException ignored) {
        }
      }
      res = getContext(usingBundle, null);
      if (res == null) {
        state = STATE_ACTIVATING;
        cclass = component.getImplementation();
        res = new ComponentContextImpl(this, usingBundle);
        try {
          res.setInstance(cclass.newInstance());
        } catch (final Exception e) {
          err = new ComponentException("Failed to instanciate: " + cclass, e);
        }
        addContext(res);
      }
    }
    if (err != null) {
      res.activationResult(false);
      Activator.logError(component.bc, "Failed to activate", err);
      throw err;
    }
    if (cclass != null) {
      Activator.logDebug("CC.active start activate, this=" + this
                         + ", activateCount=" + activeCount);
      try {
        bindReferences(res);
        if (component.activateMethod != null) {
          final ComponentException ce = component.activateMethod.invoke(res);
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
      } catch (final RuntimeException e) {
        unbindReferences(res, true);
        removeContext(res);
        res.activationResult(false);
        throw e;
      }
      return res;
    } else {
      Activator.logDebug("CC.active activate in progress wait, this=" + this
                         + ", activateCount=" + activeCount);
      synchronized (this) {
        if (res.waitForActivation()) {
          if (incrementActive) {
            activeCount++;
          }
          return res;
        }
      }
      return activate(usingBundle, incrementActive);
    }
  }

  /**
   * Deactivates a component configuration.
   *
   */
  void deactivate(ComponentContextImpl cci, int reason, boolean checkActive, boolean removeCompConfig) {
    Activator.logDebug("CC.deactivate this=" + this +
                       ", activateCount=" + activeCount);
    boolean last;
    synchronized (this) {
      if (checkActive && !cci.waitForActivation()) {
        // Deactivate an already deactivated
        return;
      }
      if (!containsContext(cci)) {
        // Deactivate an already deactivated
        return;
      }
      last = noContext() == 1;
      if (last) {
        state = STATE_DEACTIVATING;
      } else {
        activeCount--;
      }
      cci.setDeactive();
    }
    if (last) {
      Activator.logDebug("CC.deactivate last, dispose this=" +
                         this + ", unregisterInProgress=" +
                         unregisterInProgress);
      if (unregisterInProgress) {
        // We are already disposing
        return;
      }
      if (removeCompConfig && component.keepService(reason)) {
        Activator.logDebug("CC.deactivate last, dispose this=" +
                               this + ", block service for reuse");
        blockService();
      } else {
        unregisterComponentService(componentService);
        componentService = null;
      }
    }
    if (component.deactivateMethod != null) {
      component.deactivateMethod.invoke(cci, reason);
    }
    unbindReferences(cci, last);
    synchronized (this) {
      removeContext(cci);
    }
    if (last) {
      synchronized (this) {
        state = STATE_DEACTIVE;
        notifyAll();
      }
      if (removeCompConfig) {
        // TODO: Do we need to synchronize this?
        remove(reason);
        discardBlockedService();
      }
    }
  }


  /**
   * Discard any blocked service
   */
  private void discardBlockedService() {
    if (blockedService != null && blockedService.isBlocked()) {
      Activator.logDebug("CC.discardBlockedService, this=" + this);
      ComponentService cs = blockedService;
      blockedService = null;
      cs.setComponentConfiguration(null);
      unregisterComponentService(cs);
    }
  }


  /**
   * Dispose of this component configuration.
   *
   */
  void dispose(int reason, boolean removeCompConfig) {
    if (unregisterInProgress) {
      // We are already disposing
      return;
    }
    Activator.logDebug("CC.dispose this=" + this + ", reason=" + reason);
    ComponentContextImpl [] ccis;
    synchronized (this) {
      state = STATE_DEACTIVATING;
      ccis = getAllContexts();
      for (int i = 0; i < ccis.length; i++) {
        if (!ccis[i].setDeactivating()) {
          ccis[i] = null;
        }
      }
      // Mark all as deactivated?
      activeCount = 0;
    }
    unregisterComponentService(componentService);
    componentService = null;
    if (ccis.length > 0) {
      for (final ComponentContextImpl cci : ccis) {
        if (cci != null) {
          deactivate(cci, reason, false, removeCompConfig);
        }
      }
    } else {
      synchronized (this) {
        state = STATE_DEACTIVE;
        notifyAll();
      }
      if (removeCompConfig) {
        remove(reason);
      }
    }
  }


  /**
   * Get CM pid used for this component configuration or NO_CCID
   * if no CM data is used.
   *
   */
  String getCCId() {
    return ccId;
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
   * Get component configuration properties.
   *
   */
  Map<String, Object> getPropertiesMap()
  {
    return getProperties().getMap();
  }


  /**
   * Get service properties for this component configuration. Which are
   * the component configuration properties minus all properties starting
   * with a period ".".
   *
   */
  PropertyDictionary getServiceProperties() {
    return new PropertyDictionary(component, cmDict, instanceProps, true);
  }


  /**
   *
   */
  void registerComponentService(ComponentConfiguration old) {
    if (old != null) {
      componentService = old.getBlockedComponentService();
    }
    if (componentService != null) {
      componentService.setComponentConfiguration(this);
      componentService.unblock();
      synchronized (this) {
        if (state == STATE_ACTIVATING) {
          state = STATE_REGISTERED;
        }
      }
    } else {
      final ComponentDescription cd = component.compDesc;
      final String[] services = cd.getServices();
      if (services != null) {
        componentService = createComponentService();
        componentService.registerService();
        synchronized (this) {
          if (state == STATE_ACTIVATING) {
            state = STATE_REGISTERED;
          }
        }
      }
    }
  }


  ComponentService createComponentService() {
    return new ComponentService(component, this);
  }


  /**
   * Update the service properties for service registered
   * for this component configuration.
   *
   */
  private void modifyService() {
    if (componentService != null) {
      componentService.setProperties();
    }
  }


  /**
   * Unregister service registered for this component configuration.
   *
   */
  private void unregisterComponentService(ComponentService cs) {
    if (cs != null) {
      unregisterInProgress = true;
      try {
        cs.unregisterService();
      } catch (final IllegalStateException ignored) {
        // Nevermind this, it might have been unregistered previously.
      }
      unregisterInProgress = false;
    }
  }


  /**
   * Block registered service for this component configuration.
   *
   */
  private void blockService() {
    if (componentService != null) {
      componentService.block();
      blockedService = componentService;
      componentService = null;
    }
  }


  abstract void addContext(ComponentContextImpl cci);

  abstract boolean containsContext(ComponentContextImpl cci);

  abstract ComponentContextImpl [] getAllContexts();

  abstract ComponentContextImpl getContext(Bundle bundle, Object instance);

  abstract int noContext();

  abstract void removeContext(ComponentContextImpl cci);


  /**
   * Get active ComponentContext for component configuration.
   */
  ComponentContextImpl getActiveContext(Bundle b, Object instance) {
    final ComponentContextImpl cci = getContext(b, instance);
    return cci != null && cci.isActive() ? cci : null;
  }


  /**
   * Get service reference for service registered by this
   * component configuration.
   */
  ServiceReference<?> getServiceReference() {
    if (componentService != null) {
      return componentService.getReference();
    }
    return null;
  }


  /**
   * CM data for this component has changed. Check if we need to change state
   * for the component configuration. Also call modified method when its is specified.
   *
   */
  void cmConfigUpdated(String ccid, Map<String, Object> dict)
  {
    Activator.logDebug("CC.cmConfigUpdated, " + toString() + ", ccid=" + ccid
                       + ", activeCount=" + activeCount);
    int disposeReason = -1;
    List<ComponentContextImpl> ccis = null;
    synchronized (this) {
      if (state == STATE_ACTIVE) { // TODO check this for FactoryComponents
        ccId = ccid;
        if (dict == null && component.compDesc.getScrNSminor() < 3) {
          disposeReason = ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED;
        } else if (component.modifiedMethod == null
            || component.modifiedMethod.isMissing(true)) {
          // Dispose when we have no modify method
          disposeReason = dict == null ? ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED
                          : ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED;
        } else {
          cmDict = dict;
          ccProps = null;
          ccis = getActiveContexts();
        }
      } else {
        cmDict = dict;
        ccProps = null;
        return;
      }
    }
    if (ccis != null) {
      for (final ComponentContextImpl cci : ccis) {
        component.modifiedMethod.invoke(cci);
      }
      modifyService();
    } else {
      dispose(disposeReason, true);
    }
  }

  /**
   *
   */
  void bindReference(ReferenceListener rl, ServiceReference<?> s) {
    for (final ComponentContextImpl cci : getActiveContexts()) {
      cci.bind(rl, s);
    }
  }

  /**
   *
   */
  void unbindReference(ReferenceListener rl, ServiceReference<?> s, boolean resetField) {
    // This is only called for dynamic refs, so we also unbind
    // services during deactivation
    for (final ComponentContextImpl cci : getAllContexts()) {
      cci.unbind(rl, s, resetField);
    }
  }

  /**
   *
   */
  void updatedReference(ReferenceListener rl, ServiceReference<?> s) {
    for (final ComponentContextImpl cci : getActiveContexts()) {
      cci.updated(rl, s);
    }
  }

  /**
   *
   */
  synchronized void waitForDeactivate() {
    for (final ComponentContextImpl cci : getAllContexts()) {
      cci.waitForDeactivation();
    }
  }

  @Override
  public int compareTo(ComponentConfiguration o) {
    return id - o.id;
  }


  /**
   * Get service registered for component
   *
   */
  Object getService(Bundle usingBundle) {
    Activator.logDebug("CC.getService(), " + toString() + ", activeCount = " + activeCount);
    component.scr.postponeCheckin();
    try {
      final ComponentContextImpl cci = activate(usingBundle, true);
      return cci.getComponentInstance().getInstance();
    } catch (final ComponentException ce) {
      final Throwable cause = ce.getCause();
      if (cause != null) {
        throw ce;
      }
      Activator.logInfo("SCR getService return null because: " + ce);
      return null;
    } finally {
      component.scr.postponeCheckout();
    }
  }

  /**
   * Release service previously gotten via getService.
   *
   */
  void ungetService(Bundle usingBundle, Object obj) {
    if (!unregisterInProgress) {
      Activator.logDebug("CC.ungetService(), " + toString() + ", activeCount = " + activeCount);
      final ComponentContextImpl cci = getActiveContext(usingBundle, obj);
      if (cci != null) {
        boolean doDeactivate = false;
        synchronized (this) {
          if (activeCount == 1 || !(this instanceof SingletonComponentConfiguration)) {
            doDeactivate = true;
          } else {
            activeCount--;
          }
        }
        if (doDeactivate) {
          deactivate(cci, Component.KF_DEACTIVATION_REASON_COMPONENT_DEACTIVATED, true, true);
        }
      }
    }
  }

  //
  // Private
  //

  /**
   */
  private List<ComponentContextImpl> getActiveContexts() {
    List<ComponentContextImpl> res = new ArrayList<>();
    for (ComponentContextImpl cci : getAllContexts()) {
      if (cci.isActive()) {
        res.add(cci);
      }
    }
    return res;
  }


  /**
   * Bind a component to its references. The references are bound
   * in order specified by the component description.
   */
  private void bindReferences(ComponentContextImpl cci) {
    final Reference [] refs = component.getRawReferences();
    if (refs != null) {
      for (Reference ref : refs) {
        final ReferenceListener rl = ref.getListener(ccId);
        if (rl.isAvailable()) {
          if (!cci.bind(rl) && !ref.isRefOptional()) {
            throw new ComponentException("Failed to bind: " + rl);
          }
        } else {
          cci.nullField(rl);
        }
      }
    }
  }


  /**
   * Unbind a component from its references. The references are
   * unbound in reverse order specified by the component description.
   */
  private void unbindReferences(ComponentContextImpl cci, boolean last) {
    final Reference [] refs = component.getRawReferences();
    if (refs != null) {
      for (int i = refs.length - 1; i >= 0; i--) {
        cci.unbind(refs[i].refDesc.name, last);
      }
    }
  }


  ComponentConfiguration [] remove(int reason) {
    return component.removeComponentConfiguration(this, reason);
  }



  boolean setAndTestLastReactivateEvent(ServiceEvent se)
  {
    if (lastReactivateEvent != se) {
      lastReactivateEvent = se;
      return true;
    }
    return false;
  }


  boolean isUnregistering()
  {
    return unregisterInProgress;
  }


  private ComponentService getBlockedComponentService() {
    return blockedService;
  }


  String getServicePropertiesId() {
    String res = ccId + ":" + component.getCMPidRev(ccId);
    if (instanceProps != null) {
      res += ":" + instanceProps.hashCode();
    }
    return  res;
   }
}
