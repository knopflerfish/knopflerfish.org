/*
 * Copyright (c) 2017-2022, KNOPFLERFISH project
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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * Handle for a component service
 */
class ComponentService implements ServiceFactory<Object> {
  private final Component component;
  private ComponentConfiguration componentConfiguration;
  private ServiceRegistration<?> serviceRegistration = null;
  private Thread blocked = null;
  private String servicePropertiesId;


  ComponentService(Component component, ComponentConfiguration componentConfiguration) {
    this.component = component;
    this.componentConfiguration = componentConfiguration;
  }

  /**
   * Update componentConfiguration bound to this service.
   */
  synchronized void setComponentConfiguration(ComponentConfiguration componentConfiguration) {
    this.componentConfiguration = componentConfiguration;
  }

  /**
   * Register service.
   */
  void registerService() {
    final ComponentDescription cd = component.compDesc;
    serviceRegistration = component.bc.registerService(cd.getServices(), this,
                                                       componentConfiguration.getServiceProperties());
  }

  /**
   * Unregister service registered for this component service.
   * Parked getService and ungetService calls will be released
   * with a null result.
   *
   */
  void unregisterService() {
    if (serviceRegistration != null) {
      try {
        serviceRegistration.unregister();
      } catch (final IllegalStateException _ignore) {
        // Nevermind this, it might have been unregistered previously.
      }
      serviceRegistration = null;
      // Make sure it is unblocked
      unblock();
    }
  }

  /**
   * When blocked, park getService and ungetService calls until unblock
   * or unregisterService is called.
   */
  synchronized void block() {
    if (blocked == null) {
      blocked = Thread.currentThread();
      servicePropertiesId = componentConfiguration.getServicePropertiesId();
    } else {
      throw new IllegalStateException("ComponentService.block() called while blocked = " + blocked);
    }
  }

  /**
   * Unblock and release parked getService and ungetService calls.
   */
  void unblock() {
    boolean setProps = false;
    synchronized (this) {
      if (blocked != null) {
        notifyAll();
        blocked = null;
        if (componentConfiguration != null) {
          setProps = !servicePropertiesId.equals(componentConfiguration.getServicePropertiesId());
        }
      }
    }
    if (setProps) {
      setProperties();
    }
  }

  /**
   * Check if component service is blocked.
   *
   * @return True if service is blocked, otherwise false.
   */
  boolean isBlocked() {
    return blocked != null;
  }

  /**
   * Get service reference.
   *
   * @return ServiceReference object or null if inactive.
   */
  synchronized ServiceReference<?> getReference() {
    if (serviceRegistration != null) {
      return serviceRegistration.getReference();
    }
    return null;
  }

  /**
   * Update the service properties for service registered
   * for this component configuration.
   */
  void setProperties() {
    serviceRegistration.setProperties(componentConfiguration.getServiceProperties());
  }

  //
  // ServiceFactory interface
  //

  /**
   * Get service registered for component
   *
   */
  public Object getService(Bundle usingBundle,
                           ServiceRegistration<Object> reg) {
    ComponentConfiguration cc;
    synchronized (this) {
      while (blocked != null) {
        if (blocked == Thread.currentThread()) {
          throw new IllegalStateException("ComponentService.getService() called while blocked");
        }
        try {
          this.wait();
        } catch (InterruptedException ignored) {
        }
      }
      cc = componentConfiguration;
    }
    return cc != null ? cc.getService(usingBundle) : null;
  }

  /**
   * Release service previously gotten via getService.
   *
   */
  public void ungetService(Bundle usingBundle,
                           ServiceRegistration<Object> reg,
                           Object obj) {
    ComponentConfiguration cc;
    synchronized (this) {
      while (blocked != null) {
        if (blocked == Thread.currentThread()) {
          throw new IllegalStateException("ComponentService.ungetService() called while blocked");
        }
        try {
          this.wait();
        } catch (InterruptedException _ignore) {
          // Just recheck
        }
      }
      cc = componentConfiguration;
    }
    cc.ungetService(usingBundle, obj);
  }

}
