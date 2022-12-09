/*
 * Copyright (c) 2006-2022, KNOPFLERFISH project
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
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;


class FactoryComponent extends Component
{
  @SuppressWarnings("rawtypes")
  private ServiceRegistration<ComponentFactory> factoryService;
  private ComponentFactoryImpl<?> componentFactory;


  FactoryComponent(SCR scr, ComponentDescription cd) {
    super(scr, cd);
  }


  @Override
  public String toString() {
    return "Factory component: " + compDesc.getName();
  }


  /**
   * Factory component satisfied, register component factory service.
   *
   */
  @Override
  ComponentConfiguration [] satisfied(ComponentConfiguration old) {
    Activator.logInfo(bc, "Satisfied: " + toString());
    state = STATE_SATISFIED;
    componentFactory = new ComponentFactoryImpl<>(this);
    final Hashtable<String, String> p = new Hashtable<>();
    p.put(ComponentConstants.COMPONENT_NAME, compDesc.getName());
    p.put(ComponentConstants.COMPONENT_FACTORY, compDesc.getFactory());
    factoryService = bc.registerService(ComponentFactory.class, componentFactory, p);
    return null;
  }


  @Override
  void activateComponentConfiguration(ComponentConfiguration cc, ComponentConfiguration old) {
    throw new IllegalStateException("Internal error! Factory component only created with newInstance");
  }


  /**
   * Factory component unsatisfied, unregister component factory service.
   *
   */
  @Override
  void unsatisfied(int reason) {
    Activator.logInfo(bc, "Unsatisfied: " + toString());
    factoryService.unregister();
    componentFactory.deactivate();
    factoryService = null;
    componentFactory = null;
  }


  /**
   *
   */
  ComponentInstance<?> newInstance(Dictionary<String, Object> instanceProps) {
    if (!isSatisfied()) {
      throw new ComponentException("Factory is not satisfied");
    }
    String ccId = cmConfig.isEmpty() ? NO_CCID : cmConfig.getCCIds()[0];
    final ComponentConfiguration cc = newComponentConfiguration(ccId, instanceProps);
    scr.postponeCheckin();
    ComponentContextImpl cci;
    try {
      cci = cc.activate(null, true);
    } finally {
      scr.postponeCheckout();
    }
    if (isSatisfied()) {
      cc.registerComponentService(null);
      return cci.getComponentInstance();
    } else {
      // Make sure it is disposed, perhaps we should "lock" protect this code instead
      cc.dispose(KF_DEACTIVATION_REASON_COMPONENT_DEACTIVATING, true);
      throw new ComponentException("Factory is/has been deactivated");
    }
  }


  /**
   *
   */
  @Override
  void cmConfigUpdated(String ccid, boolean first) {
    Activator.logDebug("Factory cmConfigUpdate for ccid = " + ccid + " first = " + first);
    final Map<String, Object> d = cmConfig.getProperties(ccid);
    incrementCMRev(ccid);
    if (first && cmConfig.isRequired()) {
      // First mandatory config, remove constraint
      resolvedConstraint();
    } else {
      for (final ComponentConfiguration [] componentConfigurations : compConfigs.values()) {
        for (final ComponentConfiguration cc : componentConfigurations) {
          cc.cmConfigUpdated(ccid, d);
        }
      }
    }
  }


  /**
   *
   */
  @Override
  void cmConfigDeleted(String ccid) {
    Activator.logDebug("cmConfigDeleted for ccid = " + ccid);
    resetCMRev(ccid);
    for (final ComponentConfiguration [] componentConfigurations : compConfigs.values()) {
      for (final ComponentConfiguration cc : componentConfigurations) {
        cc.cmConfigUpdated(ccid, null);
      }
    }
    if (cmConfig.isRequired()) {
      unresolvedConstraint(ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);
    }
  }

}

