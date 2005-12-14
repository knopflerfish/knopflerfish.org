/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentInstance;

/**
 * This class is the implementation of the ComponentContext
 * Used by a component to interact with its executioner
 *
 * @author Magnus Klack (refactoring by Björn Andersson)
 */
public class ComponentContextImpl implements ComponentContext{
  /** the bundle context */
  private BundleContext bundleContext;

  /** the component instance */
  private ComponentInstance componentInstance;

  /** the component properties */
  private Dictionary properties;

  /** the servicereference */
  private ServiceRegistration serviceRegistration;

  /** variable holding the SCR instance */
  private ComponentRuntimeImpl systemComponentRuntime;

  /** variable holding the bundle using this context */
  private Bundle usingBundle;

  /** variable holding the requesting bundle the same as using bundle
   *  but this variable is never null
   */
  private Bundle requestBundle;

  public ComponentContextImpl(ComponentInstance component,
                              BundleContext context,
                              Dictionary props,
                              ServiceRegistration registration,
                              ComponentRuntimeImpl scr,
                              Bundle useBundle,
                              Bundle reqBundle) {
    bundleContext = context;
    properties = props;
    componentInstance =component;
    serviceRegistration = registration;
    systemComponentRuntime = scr;
    usingBundle = useBundle;
    requestBundle = reqBundle;
  }

  /**
   * returns the properties for this component
   * i,e the component the context is holding
   */
  public Dictionary getProperties() {
    return properties;
  }

  /**
   * This is used by a component to activate another component in the same bundle
   *
   * @param name the component name
   */
  public void enableComponent(String name){
    try {
      EnableProcess enableProcess = new EnableProcess(name);
      enableProcess.start();
      /* tell the SCR to enable component with the given name */
      //systemComponentRuntime.enableComponent(name,requestBundle);
    } catch(ComponentException e) {
      ComponentActivator.error(e);
    }
  }

  /**
   * This is used by a component to disable another component in the same bundle
   * or the same component.
   *
   * @param name the name of the component
   */
  public void disableComponent(String name) {
    try {
      StopProcess stopper = new StopProcess(name);
      stopper.run();
    } catch(ComponentException e) {
      ComponentActivator.error("error then stopping component " + name, e);
    }
  }

  /**
   * returns the service reference of the ComponentInstance
   */
  public ServiceReference getServiceReference() {
    return serviceRegistration.getReference();
  }

  /**
   * returns the ComponentInstance object this context is holding
   */
  public ComponentInstance getComponentInstance() {
    return componentInstance;
  }

  /**
   * this method is used by a component declaring an activate() and
   * a deactivate() method it will tell SCR to locate a service with a
   * given name.
   *
   */
  public Object locateService(String name) {
    String serviceName =(String) properties.get(ComponentConstants.COMPONENT_NAME);

    if (serviceName!=null) {
      try {
        return systemComponentRuntime.locateService(name,serviceName);
      } catch (ComponentException e) {
        ComponentActivator.error(e);
        return null;
      }
    } else {
      ComponentActivator.error("no component.name specified in the context");
      return null;
    }

  }

  public Object locateService(String name, ServiceReference serviceReference) {
    //TODO
    return locateService(name);
  }

  /**
   * This is used
   */
  public Object[] locateServices(String name) {
    String serviceName =(String) properties.get(ComponentConstants.COMPONENT_NAME);

    if (serviceName != null) {
      try {
        return systemComponentRuntime.locateServices(name,serviceName);
      } catch (ComponentException e) {
        ComponentActivator.error(e);
        return null;
      }
    } else {
      ComponentActivator.error("no component.name specified in the context");
      return null;
    }
  }

  /**
   *
   */
  public BundleContext getBundleContext() {
    return ((BundleImpl) requestBundle).getBundleContext();
  }

  /**
   * this returns the bundle using this service if its a service factory
   * else it returns null
   *
   * @return Bundle the bundle using this component as a service null if
   *  the component shares context with many bundles, i.e, it is a delayed
   *  service component.
   */
  public Bundle getUsingBundle() {
    return usingBundle;
  }



  /**
   * this class will enable a component after a given name
   * it will do it in a thread just to prevent the calling
   * bundle to hang.
   *
   * @author Magnus Klack
   */
  private class EnableProcess extends Thread {
    private String componentName;

    public EnableProcess(String name) {
      componentName = name;
    }

    public void run() {
      systemComponentRuntime.enableComponent(componentName, requestBundle);
    }
  }

  /**
   * This class will stop a specific component
   *
   * @author Magnus Klack
   */
  private class StopProcess extends Thread {
    private String componentName;

    public StopProcess(String name){
      componentName = name;
    }

    public void run() {
      try {
        systemComponentRuntime.disableComponent(componentName, requestBundle, false);
      } catch(Exception e) {
        throw new ComponentException(e.getMessage(), e.getCause());
      }
    }
  }

}