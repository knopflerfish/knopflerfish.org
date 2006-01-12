/*
 * Copyright (c) 2006, KNOPFLERFISH project
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Enumeration;


import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

public abstract class Component {

  protected Config config; 
  private boolean enabled;
  private boolean active;
  private Dictionary properties;
  private Object instance;
  private BundleContext bundleContext;
  private ServiceRegistration serviceRegistration;
  private ComponentContext componentContext;

  public Component(Config config, Dictionary overriddenProps) {

    this.config = config;
    properties = config.getProperties();

    instance = null;
    componentContext = null;
    
    bundleContext = Backdoor.getBundleContext(config.getBundle());

    if (overriddenProps != null) {

      for (Enumeration e = overriddenProps.keys(); 
           e.hasMoreElements(); ) {
        Object key = e.nextElement();
        properties.put(key, overriddenProps.get(key));
      }
    }
    
  }
  
  /** activates a component, returns true if the registration succeeds (or already was activated) */
  public boolean activate() {
    // this method is described on page 297 r4
    
    if (!config.isEnabled() || !config.isSatisfied())
      return false;

    if(isActivated()) 
      return true;

    // 1. load class

    Class klass = null;
    try {

      Bundle bundle = config.getBundle();       
      ClassLoader loader = Backdoor.getClassLoader(bundle); 
      loader.loadClass(config.getImplementation());

    } catch (ClassNotFoundException e) {
      if (Activator.log.doError())
        Activator.log.error("Could not find class " + 
                            config.getImplementation());

      return false;
    }

    ComponentInstance cInstance = null;
    
    try {
      // 2. create ComponentContext and ComponentInstance
      instance = klass.newInstance();
      cInstance = new ComponentInstanceImpl();
      componentContext = new ComponentContextImpl(cInstance);
            
      
    }  catch (IllegalAccessException e) {
      if (Activator.log.doError())
        Activator.log.error("Could not access constructor of class " + 
                            config.getImplementation());
      return false;

    } catch (InstantiationException e) {
      if (Activator.log.doError())
        Activator.log.error("Could not create instance of " + 
                            config.getImplementation() + 
                            " isn't a proper class.");
      return false;
      
    } catch (ExceptionInInitializerError e) {
      if (Activator.log.doError())
        Activator.log.error("Constructor for " + 
                            config.getImplementation() + 
                            " threw exception.", e);
      return false;
	    
    } catch (SecurityException e) {
      if (Activator.log.doError())
        Activator.log.error("Did not have permissions to create an instance of " + 
                            config.getImplementation(), e);
      return false;
    }
    
    // 3. Bind the services. This should be sent to all the references.
    config.bindReferences(instance);
    
    try {

      Method method = klass.getMethod("activate", 
                                      new Class[]{ ComponentContext.class });
      method.invoke(instance, new Object[]{ componentContext });

    } catch (NoSuchMethodException e) {
      // this instance does not have an activate method, (which is ok)
    } catch (IllegalAccessException e) {
      Activator.log.error("Declarative Services could not invoke \"deactivate\""  + 
                          " method in component \""+ config.getName() + 
                          "\". Got exception", e);
      return false;
  
    } catch (InvocationTargetException e) {
      // the method threw an exception.
      Activator.log.error("Declarative Services got exception when invoking " + 
                          "\"activate\" in component " + config.getName(), e); 
      
      // if this happens the component should not be activatated
      config.unbindReferences(instance);
      instance = null;
      componentContext = null;
      
      return false;
    }

    active = true;
    return true;
  }

  /** deactivates a component */
  public void deactivate() {
    // this method is described on page 432 r4
    
    if (!isActivated())
      return ;
    
    try {
      Class klass = instance.getClass();
      Method method = klass.getMethod("deactivate", 
                                      new Class[]{ ComponentContext.class });
      
      method.invoke(instance, new Object[]{ componentContext });

    } catch (NoSuchMethodException e) {
      // this instance does not have an deactivate method, (which is ok)
    } catch (IllegalAccessException e) {
      Activator.log.error("Declarative Services could not invoke \"deactivate\"" + 
                          " method in component \""+ config.getName() + 
                          "\". Got exception", e);
  
    } catch (InvocationTargetException e) {
      // the method threw an exception.
      Activator.log.error("Declarative Services got exception when invoking " + 
                          "\"deactivate\" in component " + config.getName(), e); 
    }
    
    config.unbindReferences(instance);
    instance = null;
    componentContext = null;
    active = false;
  }
  
  public boolean isActivated() {
    return active;
  }

  public Object getInstance() {
    return instance;
  }


  public void unregisterService() {
    serviceRegistration.unregister();    
  }

  public void registerService() {
    Bundle bundle = config.getBundle();
    BundleContext bc = Backdoor.getBundleContext(bundle);
    if (Activator.log.doDebug()) {
      Activator.log.debug("registerService() got BundleContext: " + bc);
    }
    String[] interfaces = config.getServices();
    
    if (interfaces == null) {
      return ;
    }

    serviceRegistration = 
      bc.registerService(interfaces, instance, properties);
        
  }

  /** 
      this method is called whenever this components configuration
      becomes satisfied.
   */
  public abstract void satisfied();

  /** 
      this method is called whenever this components configuration
      becomes unsatisfied.
   */

  public abstract void unsatisfied();

  // to provide compability with component context
  private class ComponentContextImpl implements ComponentContext {
    private ComponentInstance componentInstance;
    
    public ComponentContextImpl(ComponentInstance componentInstance) {
      this.componentInstance = componentInstance;
    }
   
    public Dictionary getProperties() {
      return properties; // TODO: wrap inside an immutable-dictionary class.
    }
    
    public Object locateService(String name) {
      throw new RuntimeException("not yet implemented");      
    }
    
    public Object[] locateServices(String name) {
      throw new RuntimeException("not yet implemented");
    }
   
    
    public BundleContext getBundleContext() {
      // maybe keep this as an variable instead?
      return bundleContext;
    }
    
    public ComponentInstance getComponentInstance() {
      return componentInstance;
    }

    public Bundle getUsingBundle() {
      throw new RuntimeException("not yet implemented");
    }


    public void enableComponent(String name) {
      throw new RuntimeException("not yet implemented");
    }
    
    public void disableComponent(String name) {
      throw new RuntimeException("not yet implemented");
    }

    public ServiceReference getServiceReference() {
      /* 
	 We need to do it like this since this function might
	 be called before *we* even know what it is. However,
	 this value is know by the framework.
      */
      if (serviceRegistration == null) {
	return null; // is being fixed atm.
      } else {
	return serviceRegistration.getReference();
      }
    }
  }
  
  private class ComponentInstanceImpl implements ComponentInstance {

    public void dispose() {
      deactivate();
    }

    public Object getInstance() {
      return instance; // will be null when the component is not activated.
    }

  }

}
