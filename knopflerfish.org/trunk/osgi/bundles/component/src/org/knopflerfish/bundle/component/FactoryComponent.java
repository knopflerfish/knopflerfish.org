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

import java.util.*;
import org.osgi.framework.*;

import org.osgi.service.cm.*;
import org.osgi.service.component.*;


class FactoryComponent extends Component
{
  private ServiceRegistration factoryService;
  private ComponentFactoryImpl componentFactory;


  FactoryComponent(SCR scr, ComponentDescription cd) {
    super(scr, cd);
  }


  public String toString() {
    return "Factory component: " + compDesc.getName();
  }


  /**
   * Factory component satisfied, register component factory service.
   *
   */
  void subclassSatisfied() {
    Activator.logInfo(bc, "Satisfied: " + toString());
    componentFactory = new ComponentFactoryImpl(this);
    Hashtable p = new Hashtable();
    p.put(ComponentConstants.COMPONENT_NAME, compDesc.getName());
    p.put(ComponentConstants.COMPONENT_FACTORY, compDesc.getFactory());
    factoryService = bc.registerService(ComponentFactory.class.getName(), componentFactory, p);
  }


  /**
   * Factory component unsatisfied, unregister component factory service.
   *
   */
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
  ComponentInstance newInstance(Dictionary instanceProps) {
    if (!isSatisfied()) {
      throw new ComponentException("Factory is not satisfied");
    }
    ComponentConfiguration cc = newComponentConfiguration(compDesc.getName(), instanceProps);
    scr.postponeCheckin();
    ComponentContextImpl cci;
    try {
      cci = cc.activate(null, false);
    } finally {
      scr.postponeCheckout();
    }
    if (isSatisfied()) {
      cc.registerService();
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
  void cmConfigUpdated(String pid, Configuration c) {
    if (c.getFactoryPid() != null) {
      Activator.logError(bc, "FactoryComponent can not have factory config, ignored", null);
      return;
    }
    boolean isEmpty = cmDicts.isEmpty();
    Activator.logDebug("Factory cmConfigUpdate for pid = " + pid + " is empty = " + isEmpty);
    Dictionary d = c.getProperties();
    cmDicts.put(pid, d);
    if (isEmpty && !cmConfigOptional) {
      // First mandatory config, remove constraint
      resolvedConstraint();
    } else {
      for (Iterator i = compConfigs.values().iterator(); i.hasNext(); ) {
        ComponentConfiguration cc = (ComponentConfiguration)i.next();
        cc.cmConfigUpdated(pid, d);
      }
    }
  }


  /**
   *
   */
  void cmConfigDeleted(String pid) {
    cmDicts.remove(pid);
    Activator.logDebug("cmConfigDeleted for pid = " + pid);
    for (Iterator i = compConfigs.values().iterator(); i.hasNext(); ) {
      ComponentConfiguration cc = (ComponentConfiguration)i.next();
      cc.cmConfigUpdated(pid, null);
    }
    if (!cmConfigOptional) {
      unresolvedConstraint(ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED);
    }
  }

}
  
