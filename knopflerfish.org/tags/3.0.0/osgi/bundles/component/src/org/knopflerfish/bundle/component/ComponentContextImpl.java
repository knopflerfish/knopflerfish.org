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

import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.component.*;


class ComponentContextImpl implements ComponentContext
{
  final private ComponentConfiguration cc;
  final private Bundle usingBundle;
  final private ComponentInstanceImpl componentInstance;
  private HashMap /* String -> ReferenceListener */ boundReferences = null;


  /**
   *
   */
  public ComponentContextImpl(ComponentConfiguration cc, Object instance, Bundle usingBundle) {
    this.cc = cc;
    this.usingBundle = usingBundle;
    componentInstance = new ComponentInstanceImpl(this, instance);
  }


  /**
   *
   */
  public Dictionary getProperties() {
    // TBD, remove this when TCK is correct
    if (Activator.TCK_BUG_COMPLIANT) {
      return new Hashtable(cc.getProperties());
    } else {
      return cc.getProperties();
    }
  }


  /**
   *
   */
  public Object locateService(String name) {
    if (boundReferences != null) {
      ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
      if (rl != null) {
        return rl.getService(usingBundle);
      }
    }
    return null;
  }


  /**
   *
   */
  public Object locateService(String name, ServiceReference sRef) {
    if (boundReferences != null) {
      ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
      if (rl != null) {
        return rl.getService(sRef, usingBundle);
      }
    }
    return null;
  }


  /**
   *
   */
  public Object[] locateServices(String name) {
    if (boundReferences != null) {
      ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
      if (rl != null) {
        return rl.getServices(usingBundle);
      }
    }
    return null;
  }


  /**
   *
   */
  public BundleContext getBundleContext() {
    return cc.component.bc;
  }


  /**
   *
   */
  public ComponentInstance getComponentInstance() {
    return componentInstance;
  }


  /**
   *
   */
  public Bundle getUsingBundle() {
    return usingBundle;
  }


  /**
   *
   */
  public void enableComponent(String name) {
    cc.component.scr.enableComponent(name, cc.component.compDesc.bundle);
  }


  /**
   *
   */
  public void disableComponent(String name) {
    cc.component.scr.disableComponent(name, cc.component.compDesc.bundle);
  }


  /**
   *
   */
  public ServiceReference getServiceReference() {
    return cc.getServiceReference();
  }

  //
  //
  //

  /**
   * 
   */
  void dispose() {
    if (cc.isLastContext(this)) {
      cc.dispose(ComponentConstants.DEACTIVATION_REASON_DISPOSED);
    } else {
      cc.deactivate(this, ComponentConstants.DEACTIVATION_REASON_DISPOSED);
    }
  }


  /**
   * 
   */
  boolean bind(ReferenceListener rl) {
    if (rl.isMultiple()) {
      int cnt = 0;
      ServiceReference [] sr = rl.getServiceReferences();
      for (int i = 0; i < sr.length; i++) {
        if (bind(rl, sr[i])) {
          cnt++;
        }
      }
      return cnt > 0;
    } else {
      return bind(rl, rl.getServiceReference());
    }
  }


  /**
   *
   */
  boolean bind(ReferenceListener rl, ServiceReference s) {
    Activator.logDebug("Bind service " + s + " to " + cc);
    boolean res;
    if (rl.ref.bindMethod != null) {
      res = rl.ref.bindMethod.invoke(this, s) == null;
    } else {
      // Get service so that it is bound in correct order
      res = null != rl.getService(s, usingBundle);
    }
    if (res) {
      if (boundReferences == null) {
        boundReferences = new HashMap();
      }
      boundReferences.put(rl.getName(), rl);
    }
    return res;
  }


  /**
   * 
   */
  void unbind(String name) {
    if (boundReferences == null) {
      return;
    }
    ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
    if (rl == null) {
      return;
    }
    if (rl.isMultiple()) {
      ServiceReference [] sr = rl.getServiceReferences();
      for (int i = 0; i < sr.length; i++) {
        unbind(rl, sr[i]);
      }
    } else {
      ServiceReference sr = rl.getServiceReference();
      if (sr != null) {
        unbind(rl, sr);
      }
    }
  }


  /**
   *
   */
  void unbind(ReferenceListener rl, ServiceReference s) {
    Activator.logDebug("Unbind service " + s + " from " + cc);
    // TBD, should we keep track on which services we have sucessfully bound
    // and only unbind those.
    if (rl.ref.unbindMethod != null) {
      rl.ref.unbindMethod.invoke(this, s);
    }
  }

}
