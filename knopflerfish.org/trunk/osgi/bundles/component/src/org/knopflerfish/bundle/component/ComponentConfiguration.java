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
  private HashMap /* Bundle -> ComponentContextImpl */ factoryContexts;
  private boolean unregisterInProgress = false;
  private int activeCount = 0;
  private int state = STATE_ACTIVATING;

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
    factoryContexts = c.compDesc.isServiceFactory() ? new HashMap() : null;
    Activator.logDebug("Created " + toString());
  }


  /**
   *
   */
  public String toString() {
    return "ComponentConfiguration, component#" + component.id +
      ", name = " + component.compDesc.getName();
  }


  /**
   * Activates a component configuration.
   *
   */
  synchronized ComponentContextImpl activate(Bundle usingBundle) {
    ComponentContextImpl res = getContext(usingBundle);
    if (res == null) {
      state = STATE_ACTIVATING;
      Class c = component.getImplementation();
      component.getMethods(c);
      try {
        res = new ComponentContextImpl(this, c.newInstance(), usingBundle);
      }  catch (Exception e) {
        Activator.logError(component.bc, "Failed to instanciate: " + c, e);
        throw new ComponentException("Failed to instanciate: " + c, e);
      }
      try {
        bindReferences(res);
        if (component.activateMethod != null) {
          ComponentException ce = component.activateMethod.invoke(res);
          if (ce != null) {
            throw ce;
          }
        }
        if (factoryContexts != null) {
          factoryContexts.put(usingBundle, res);
        } else {
          componentContext = res;
        }
        state = STATE_ACTIVE;
      } catch (ComponentException e) {
        unbindReferences(res);
        throw e;
      }
    }
    return res;
  }


  /**
   * Deactivates a component configuration.
   *
   */
  synchronized void deactivate(int reason) {
    state = STATE_DEACTIVATING;
    ComponentContextImpl [] cci = getContexts();
    for (int i = 0; i < cci.length; i++) {
      deactivate(cci[i], reason);
    }
    state = STATE_DEACTIVE;
  }


  /**
   * Deactivates a component configuration.
   *
   */
  synchronized void deactivate(ComponentContextImpl cci, int reason) {
    if (factoryContexts != null && factoryContexts.containsValue(cci)) {
      factoryContexts.remove(cci.getUsingBundle());
    } else if (componentContext == cci) {
      componentContext = null;
    } else {
      // Deactivate an already deactivated
      return;
    }
    activeCount--;
    if (component.deactivateMethod != null) {
      component.deactivateMethod.invoke(cci, reason);
    }
    unbindReferences(cci);
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
    unregisterService();
    deactivate(reason);
    component.removeComponentConfiguration(this);
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
      if (state == STATE_ACTIVATING) {
        state = STATE_REGISTERED;
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
      unregisterInProgress = false;
      serviceRegistration = null;
    }
  }


  /**
   * Is the specified ComponentContext the last active
   * connected to this component configuration.
   */
  boolean isLastContext(ComponentContextImpl cci) {
    if (factoryContexts != null) {
      return activeCount == 1 && factoryContexts.containsValue(cci);
    }
    return componentContext == cci;
  }


  /**
   * Get ComponentContext for component configuration.
   */
  ComponentContextImpl getContext(Bundle b) {
    if (factoryContexts != null) {
      return (ComponentContextImpl)factoryContexts.get(b);
    }
    return componentContext;
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
  synchronized void cmConfigUpdated(String pid, Dictionary dict) {
    Activator.logDebug("CC.cmConfigUpdated, " + toString() + ", pid=" + pid + ", active=" + activeCount);
    cmPid = pid;
    cmDict = dict;
    ccProps = null;
    sProps = null;
    if (activeCount > 0) {
      if (dict == null) {
        // Always dispose when an used config is deleted!?
        dispose(ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);
        return;
      } else if (component.modifiedMethod == null || component.modifiedMethod.isMissing(true)) {
        // Dispose when we have no modify method
        dispose(ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED);
        return;
      } else {
        ComponentContextImpl [] cci = getContexts();
        for (int i = 0; i < cci.length; i++) {
          component.modifiedMethod.invoke(cci[i]);
        }
        modifyService();
      }
    }
  }


  /**
   * The tracked reference has changed.
   *
   */
  void refUpdated(ReferenceListener rl, ServiceReference s, boolean deleted, boolean wasSelected) {
    Activator.logDebug("CC.refUpdate, " + toString() + ", " + rl + ", deleted=" + deleted +
                       ", wasSelected=" + wasSelected);
    if (activeCount > 0) {
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
    Activator.logDebug("CC.getService(), " + toString() + ", active = " + activeCount);
    ComponentContextImpl cci = activate(usingBundle);
    activeCount++;
    return cci.getComponentInstance().getInstance();
  }


  /**
   * Release service previously gotten vie getService.
   *
   */
  public void ungetService(Bundle usingBundle,
                           ServiceRegistration reg,
                           Object obj) {
    if (!unregisterInProgress) {
      Activator.logDebug("CC.ungetService(), " + toString() + ", active = " + activeCount);
      ComponentContextImpl cci = getContext(usingBundle);
      if (cci != null) {
        if (factoryContexts != null || activeCount == 1) {
          deactivate(cci, ComponentConstants.DEACTIVATION_REASON_UNSPECIFIED);
        } else {
          activeCount--;
        }
      }
    }
  }

  //
  // Private
  //

   /**
   * Bind a component to its references. The references are bound
   * in order specified by the component description.
   */
  private ComponentContextImpl [] getContexts() {
    ComponentContextImpl [] res;
    if (factoryContexts != null) {
      res = (ComponentContextImpl [])factoryContexts.values().toArray(new ComponentContextImpl [factoryContexts.size()]);
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
    ComponentContextImpl [] cci = getContexts();
    for (int i = 0; i < cci.length; i++) {
      cci[i].bind(rl, s);
    }
  }


  /**
   *
   */
  private void unbindReference(ReferenceListener rl, ServiceReference s) {
    ComponentContextImpl [] cci = getContexts();
    for (int i = 0; i < cci.length; i++) {
      cci[i].unbind(rl, s);
    }
  }

}
