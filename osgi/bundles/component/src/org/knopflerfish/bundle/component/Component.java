/*
 * Copyright (c) 2006-2008, KNOPFLERFISH project
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
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

abstract class Component implements ServiceFactory {

  protected Config config;
  private Long componentId;
  private boolean active;
  private Object instance;
  protected BundleContext bundleContext;
  protected ServiceRegistration serviceRegistration;
  protected ComponentContext componentContext;
  protected ComponentInstance componentInstance;
  protected Bundle usingBundle;

  // Properties from cm. These can be discarded.
  private Hashtable effectiveProperties;

  public Component(Config config, Dictionary overriddenProps) {

    this.config = config;

    instance = null;
    componentContext = null;

    bundleContext = Backdoor.getBundleContext(config.getBundle());

    if (overriddenProps != null) {
      config.overrideProperties(overriddenProps);
    }

    config.setProperty(ComponentConstants.COMPONENT_NAME, config.getName());
    cmDeleted();
  }

  public void enable() {
    config.enable();
  }

  public void disable() {
    config.disable();
  }

  /**
   * Find a public or protected method declared by the given class or
   * one of its ancestors.
   * @param klass The (concrete) class to look for the method in.
   * @param name  The name of the method to look for.
   * @param argSign The method argument types.
   * @return The method.
   * @throws NoSuchMethodException if the given class does not
   *         implement a method with the given name and arg type.
   */
  private Method findPublicOrProtectedMethod( Class klass,
                                              String name,
                                              Class[] argSign )
    throws NoSuchMethodException
  {
    try {
      Method method = klass.getDeclaredMethod(name, argSign);
      int modifiers = method.getModifiers();
      if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
        return method;
      }
      String msg = "The method \""+name +"\" in \"" +klass.getName()
        +"\" is neither " +"public nor protected as required.";
      if (Activator.log.doDebug()) {
        Activator.log.debug(msg);
      }
      throw new NoSuchMethodException(msg);
    } catch (NoSuchMethodException nsme) {
      Class superKlass = klass.getSuperclass();
      if (klass.isArray() || null == superKlass) {
        throw nsme;
      }
      return findPublicOrProtectedMethod(superKlass, name, argSign );
    }
  }


  /** Activates a component.
      If the component isn't enabled or satisfied, nothing will happen.
      If the component is already activated nothing will happen.
  */
  public synchronized void activate() {
    // this method is described on page 297 r4

    // Synchronized because the service is registered before activation,
    // enabling another thread to get the service and thereby trigger a
    // second activate() call.

    if (!config.isEnabled() || !config.isSatisfied())
      return ;

    if(isActivated())
      return ;

    // 1. load class
    Class klass = null;
    try {

      Bundle bundle = config.getBundle();
      klass = bundle.loadClass(config.getImplementation());

    } catch (ClassNotFoundException e) {
      if (Activator.log.doError())
        Activator.log.error("Could not find class " +
                            config.getImplementation());

      return ;
    }

    try {
      // 2. create ComponentContext and ComponentInstance
      instance = klass.newInstance();
      componentInstance = new ComponentInstanceImpl();
      componentContext = new ComponentContextImpl(componentInstance);


    }  catch (IllegalAccessException e) {
      if (Activator.log.doError())
        Activator.log.error("Could not access constructor of class " +
                            config.getImplementation());
      return ;

    } catch (InstantiationException e) {
      if (Activator.log.doError())
        Activator.log.error("Could not create instance of " +
                            config.getImplementation() +
                            " isn't a proper class.");
      return ;

    } catch (ExceptionInInitializerError e) {
      if (Activator.log.doError())
        Activator.log.error("Constructor for " +
                            config.getImplementation() +
                            " threw exception.", e);
      return ;

    } catch (SecurityException e) {
      if (Activator.log.doError())
        Activator.log.error("Did not have permissions to create an "
                            +"instance of " +config.getImplementation(),
                            e);
      return ;
    }

    // 3. Bind the services. This should be sent to all the references.
    config.bindReferences(instance);
    try {

      Method method
        = findPublicOrProtectedMethod( klass,
                                       "activate",
                                       new Class[]{ ComponentContext.class });
      method.setAccessible(true);
      method.invoke(instance, new Object[]{ componentContext });
    } catch (NoSuchMethodException e) {
      // this instance does not have an activate method, (which is ok)
      if (Activator.log.doDebug()) {
        Activator.log.debug("this instance does not have an activate "
                            +"method, (which is ok)");
      }
    } catch (IllegalAccessException e) {
      Activator.log.error("Declarative Services could not invoke "
                          +"\"deactivate\" method in component \""
                          + config.getName() +"\". Got exception",
                          e);
      return ;

    } catch (InvocationTargetException e) {
      // the method threw an exception.
      Activator.log.error("Declarative Services got exception when invoking " +
                          "\"activate\" in component " + config.getName(), e);

      // if this happens the component should not be activatated
      config.unbindReferences(instance);
      instance = null;
      componentContext = null;

      return ;
    }

    active = true;
  }

  /** deactivates a component */
  public synchronized void deactivate() {
    // this method is described on page 432 r4

    if (!isActivated()) return ;

    try {
      Class klass = instance.getClass();
      Method method
        = findPublicOrProtectedMethod( klass,
                                       "deactivate",
                                       new Class[]{ ComponentContext.class });
      method.setAccessible(true);
      method.invoke(instance, new Object[]{ componentContext });
    } catch (NoSuchMethodException e) {
      // this instance does not have a deactivate method, (which is ok)
      if (Activator.log.doDebug()) {
        Activator.log.debug("this instance does not have a deactivate method, "
                            +"(which is ok)");
      }
    } catch (IllegalAccessException e) {
      Activator.log.error("Declarative Services could not invoke "
                          +"\"deactivate\" method in component \""
                          + config.getName() +"\". Got exception",
                          e);

    } catch (InvocationTargetException e) {
      // the method threw an exception.
      Activator.log.error("Declarative Services got exception when invoking " +
                          "\"deactivate\" in component " + config.getName(), e);
    }

    config.unbindReferences(instance);

    instance = null;
    componentContext = null;
    componentInstance = null;
    active = false;
  }

  public boolean isActivated() {
    return active;
  }

  public void unregisterService() {
    if (serviceRegistration != null) {
      try {
        serviceRegistration.unregister();
      } catch (IllegalStateException ignored) {
        // Nevermind this, it might have been unregistered previously.
      }
      serviceRegistration = null;
    }
  }

  public void registerService() {
    if (Activator.log.doDebug()) {
      Activator.log.debug("registerService() got BundleContext: "
                          + bundleContext);
    }

    if (!config.getShouldRegisterService())
      return ;

    String[] interfaces = config.getServices();

    if (interfaces == null) {
      return ;
    }
    serviceRegistration =
      bundleContext.registerService(interfaces, this, effectiveProperties);
  }

  /**
     This must be overridden
  */
  public Object getService(Bundle usingBundle,
                           ServiceRegistration reg) {
    this.usingBundle = usingBundle;
    return instance;
  }

  /**
     This must be overridden
  */
  public void ungetService(Bundle usingBundle,
                           ServiceRegistration reg,
                           Object obj) {
    this.usingBundle = null;
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


  public void setComponentId(Long componentId) {
    this.componentId = componentId;
    effectiveProperties.put(ComponentConstants.COMPONENT_ID, componentId);
  }

  // to provide compability with component context
  private class ComponentContextImpl implements ComponentContext {
    private ComponentInstance componentInstance;
    private Dictionary immutable;

    public ComponentContextImpl(ComponentInstance componentInstance) {
      this.componentInstance = componentInstance;
    }

    public Dictionary getProperties() {
      if (immutable == null) {
        immutable = new ImmutableDictionary(effectiveProperties);
      }

      return immutable ;
    }

    public Object locateService(String name) {
      Reference ref = config.getReference(name);
      return ref.getService();
    }

    public Object locateService(String name, ServiceReference sRef) {
      Reference ref = config.getReference(name);
      return ref.getService(sRef);
    }

    public Object[] locateServices(String name) {
      Reference ref = config.getReference(name);
      return ref.getServices();
    }

    public BundleContext getBundleContext() {
      return bundleContext;
    }

    public ComponentInstance getComponentInstance() {
      return componentInstance;
    }

    public Bundle getUsingBundle() {
      return usingBundle;
    }

    public void enableComponent(String name) {
      Collection collection =
        SCR.getInstance().getComponents(config.getBundle());

      for (Iterator i = collection.iterator();
           i.hasNext(); ) {

        Config config = (Config)i.next();

        if (name == null ||
            config.getName().equals(name)) {


          if (!config.isEnabled()) {
            Component component = config.createComponent();
            component.enable();
          }
        }
      }
    }

    public void disableComponent(String name) {
      Collection collection =
        SCR.getInstance().getComponents(config.getBundle());

      for (Iterator i = collection.iterator();
           i.hasNext(); ) {

        Config config = (Config)i.next();

        if (name == null ||
            config.getName().equals(name)) {

          if (config.isEnabled()) {
            config.disable();
          }
        }
      }
    }

    public ServiceReference getServiceReference() {
      /*
         We need to do it like this since this function might
         be called before *we* even know what it is. However,
         this value is know by the framework, hence we can
         actually retrieve it.
      */

      if (serviceRegistration == null) {

        try {
          ServiceReference[] refs = bundleContext
            .getServiceReferences(config.getImplementation(),
                                  "(" + ComponentConstants.COMPONENT_ID + "=" +
                                  componentId + ")");
          if (refs == null) {
            return null;
          }

          return refs[0];

        } catch (Exception e) {
          throw new RuntimeException("This is a bug.", e);
        }

      } else {
        return serviceRegistration.getReference();
      }
    }
  }

  private class ComponentInstanceImpl implements ComponentInstance {

    public void dispose() {
      unregisterService();
      deactivate();
    }

    public Object getInstance() {
      return instance; // will be null when the component is not activated.
    }


  }

  public Config getConfig() { return config; }
  public BundleContext getBundleContext() { return bundleContext; }
  public Object getInstance() { return instance; }
  public ComponentInstance getComponentInstance() { return componentInstance; }

  /*
     We need to keep track of the entries that has been changed by CM
     since these might have to be removed when a CM_DELETED event occurs..
  */

  public void cmUpdated(Dictionary dict) {
    if (dict == null) return ;

    for (Enumeration e = dict.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      if (!key.equals(ComponentConstants.COMPONENT_NAME) &&
          !key.equals(ComponentConstants.COMPONENT_ID)) {

        effectiveProperties.put(key, dict.get(key));
      }
    }
    if (serviceRegistration != null) {
      serviceRegistration.setProperties(effectiveProperties);
    }
  }

  public void cmDeleted() {
    Dictionary dict = config.getProperties();
    effectiveProperties = new Hashtable();
    for (Enumeration e = dict.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      effectiveProperties.put(key, dict.get(key));
    }
    if (componentId!=null){
      effectiveProperties.put(ComponentConstants.COMPONENT_ID, componentId);
    }
    if (serviceRegistration != null) {
      serviceRegistration.setProperties(effectiveProperties);
    }
  }
}
