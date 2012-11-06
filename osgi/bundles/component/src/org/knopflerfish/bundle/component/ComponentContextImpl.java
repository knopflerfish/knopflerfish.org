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


class ComponentContextImpl implements ComponentContext
{
  final static int ACTIVATING = 0;
  final static int ACTIVE = 1;
  final static int DEACTIVE = 2;
  final static int FAILED = 3;

  final private ComponentConfiguration cc;
  final private Bundle usingBundle;
  final private Thread activatorThread;
  private volatile ComponentInstanceImpl componentInstance = null;
  private volatile int state = ACTIVATING;
  final private HashMap /* String -> ReferenceListener */ boundReferences = new HashMap();


  /**
   *
   */
  ComponentContextImpl(ComponentConfiguration cc, Bundle usingBundle) {
    this.cc = cc;
    this.usingBundle = usingBundle;
    activatorThread = Thread.currentThread();
  }


  /**
   *
   */
  public String toString() {
    return "[ComponentContext to " + cc + "]";
  }


  /**
   *
   */
  public Dictionary getProperties() {
    // TBD, remove this when TCK is correct
    if (Activator.TCK_BUG_COMPLIANT) {
      return cc.getProperties().writeableCopy();
    } else {
      return cc.getProperties();
    }
  }


  /**
   *
   */
  public Object locateService(String name) {
    ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
    if (rl != null) {
      return rl.getService(usingBundle);
    }
    return null;
  }


  /**
   *
   */
  public Object locateService(String name, ServiceReference sRef) {
    ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
    if (rl != null) {
      return rl.getService(sRef, usingBundle);
    }
    return null;
  }


  /**
   *
   */
  public Object[] locateServices(String name) {
    ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
    if (rl != null) {
      return rl.getServices(usingBundle);
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
  void setInstance(Object instance) {
    componentInstance = new ComponentInstanceImpl(this, instance);
  }


  /**
   * 
   */
  void dispose() {
    cc.deactivate(this, ComponentConstants.DEACTIVATION_REASON_DISPOSED, true);
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
    Activator.logDebug("Bind service " + Activator.srInfo(s) + " to " + cc);
    try {
      ComponentException ce = null;
      boolean res;
      ComponentMethod m = rl.ref.getBindMethod();
      if (m != null) {
        if (m.isMissing(true)) {
          // Should we fail when method is missing?
          // The specification doesn't say, but the
          // CT requires it.
          return false;
        }
        ComponentMethod.Operation bindOp = m.prepare(this, s);
        rl.bound(s);
        boundReferences.put(rl.getName(), rl);
        ce = bindOp.invoke();
      } else {
        // Get service so that it is bound in correct order
        if (null != rl.getService(s, usingBundle)) {
          rl.bound(s);
          boundReferences.put(rl.getName(), rl);
        } else {
          throw new IllegalStateException("Retry");
        }
      }
      // Check if component was disposed during bind
      if (cc.getState() == ComponentConfiguration.STATE_DEACTIVE) {
        Activator.logDebug(cc + " got deactiveated during bind of " + Activator.srInfo(s));
        return false;
      }
      cc.component.scr.clearPostponeBind(this, rl, s);
      String msg = "CCI.bind, bound " + Activator.srInfo(s) + " to " + cc + ". ";
      if (ce == null) {
        Activator.logDebug(msg + "OK");
      } else {
        Activator.logDebug(msg + "But bind method failed: " + ce);
      }
      return true;
    } catch (ComponentException ce) {
      // Possible circular chain detected in prepare method
      // TODO, improve this since it is Framework dependent
       cc.component.scr.postponeBind(this, rl, s);
    } catch (IllegalStateException ise) {
      // Possible circular chain detected in getService method
      // or it returned null.
      // TODO, improve this since it is Framework dependent
      cc.component.scr.postponeBind(this, rl, s);
    } catch (Exception e) {
      Activator.logDebug("Failed to bind service " + Activator.srInfo(s) + " to " + cc + ", " + e);
    }
    return false;
  }


  /**
   * 
   */
  void unbind(String name) {
    Activator.logDebug("Unbind " + name + " for " + cc);
    ReferenceListener rl = (ReferenceListener)boundReferences.get(name);
    if (rl == null) {
      return;
    }
    ServiceReference [] sr = rl.getBoundServiceReferences();
    for (int i = 0; i < sr.length; i++) {
      unbind(rl, sr[i]);
    }
  }


  /**
   *
   */
  void unbind(ReferenceListener rl, ServiceReference s) {
    if (rl.unbound(s)) {
      Activator.logDebug("Unbind service " + Activator.srInfo(s) + " from " + cc);
      ComponentMethod m = rl.ref.getUnbindMethod();
      if (m != null && !m.isMissing(true)) {
        try {
          m.prepare(this, s).invoke();
        } catch (ComponentException _ignore) { }
      }
    }
  }


  /**
   * Check if we have bound reference to specified service
   */
  boolean isBound(ReferenceListener rl) {
    return boundReferences.containsValue(rl);
  }


  /**
   * Tell all waiting on activation, activation result.
   *
   */
  void activationResult(boolean success) {
    synchronized (cc) {
      state = success ? ACTIVE : FAILED;
      cc.notifyAll();
    }
  }


  /**
   *
   */
  boolean isActive() {
    return state == ACTIVE;
  }


  /**
   *
   */
  void setDeactive() {
    synchronized (cc) {
      state = DEACTIVE;
    }
  }


  /**
   * Wait for activation of component or return directly
   * if no activation is in progress. If activation succeeded
   * return true, otherwise return false. Must be called
   * when holding ComponentConfiguration lock.
   *
   * @return result of activation, if activation succeeded
   * return true, otherwise return false.
   */
  boolean waitForActivation() {
    synchronized (cc) {
      while (state == ACTIVATING) {
        if (activatorThread.equals(Thread.currentThread())) {
          throw new ComponentException("Circular activate/deactivate detected: " + cc);
        }
        try {
          cc.wait();
        } catch (InterruptedException _ignore) { }
      }
      return isActive();
    }
  }

}
