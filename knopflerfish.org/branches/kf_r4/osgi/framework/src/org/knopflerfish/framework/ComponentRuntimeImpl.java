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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.log.LogService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class is the implementation of the declarative service feature. It will
 * locate and bind diffrent types of declared components on demand. It will also
 * listen to BundleEvents rasied within the framework and act after that. It is
 * right now very hard to get information from the declaration class and that
 * demands, in many cases, nestle loops, for or while statements. The class uses
 * a ComponentDeclarations, BundleContexts, ComponentParsers to fulfill its
 * responsibility. It keeps active components in a vector and inactive
 * components in another vector.
 *
 * @author Magnus Klack (refactoring by Björn Andersson)
 */
public class ComponentRuntimeImpl implements BundleListener, ServiceListener {
  
  private static final String CARDINAL_0_1 = "0..1";
  private static final String CARDINAL_0_N = "0..n";
  private static final String CARDINAL_1_1 = "1..1";
  private static final String CARDINAL_1_N = "1..n";
	 
  private static final String COMPONENT_FACTORY_SERVICE_PID = "org.osgi.service.component.ComponentFactory";
  private static final String COMPONENT_FACTORY_SERVICE_DESC = "Declarative component factory created by SCR";
  private static final String SERVICE_FACTORY_SERVICE_DESC = "Declarative service factory created by SCR";
  private static final String DELAYED_SERVICE_DESC = "Declarative delayed component service created by SCR";

  private static final String ACTIVATE_METHOD_NAME = "activate";
  private static final String DEACTIVATE_METHOD_NAME = "deactivate";

  private static final String DYNAMIC_POLICY = "dynamic";
  private static final String STATIC_POLICY = "static";

  private BundleContext bundleContext;

  private long componentCounter = 0;

  private Vector activeComponents = new Vector();

  private Vector inactiveComponents = new Vector();
  
  private static ServiceTracker logTracker;

  /**
   * Constructor for the SCR assigns local variable and if there are already
   * active declarative bundles within the framework an evaluation of them.
   *
   * @param context       the bundle context
   * @param alreadyActive an array with already active components
   */
  public ComponentRuntimeImpl(BundleContext context) {
    bundleContext = context;


    /* MO: TODO: this isn't that nice, need to unregister 
       this service sometime. Will do for now.
     */
    
    bundleContext.registerService(ConfigurationListener.class.getName(),
				  new ComponentConfigurationListener(), 
				  new Hashtable());


    Bundle[] bundles = context.getBundles();
    
    bundleContext.addBundleListener(this);
    bundleContext.addServiceListener(this);

    logTracker = new ServiceTracker(context, LogService.class.getName(), null);
    logTracker.open();

    for(int i=0;i<bundles.length;i++){
      bundleChanged(new BundleEvent(BundleEvent.STARTED, bundles[i]));
    }
  }
  /**
   * this method will be called when SCR is shutting down it will dispose all
   * active components handled by the SCR and return to the caller method
   */
  public synchronized void shutdown() {
    for (int i = 0; i < activeComponents.size(); i++) {
      DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(i);
      disableComponent(component,
                       component.getComponentDeclaration().getDeclaringBundle(),
                       true);
      /* decrease one element is removed */
      i--;
    }
    logTracker.close();
  }

  public static void log(int level, String msg) {
    log(level, msg, (Throwable)null);
  }

  public static void log(int level, String msg, Throwable e) {
    LogService service = (LogService) logTracker.getService();
    
    if (service == null) 
      return ;
    
    if (e != null) {
      service.log(level, msg, e);
    } else {
      service.log(level, msg);
    }
  }

  // MO: TODO: can't one do this any nicer?
  private Configuration getConfiguration(ComponentDeclaration cd) throws IOException {
    BundleContext bc = cd.getDeclaringBundle().getBundleContext();
    ServiceReference ref = bc.getServiceReference(ConfigurationAdmin.class.getName());
    
    if (ref == null)
      return null;
    
    ConfigurationAdmin admin = (ConfigurationAdmin) bc.getService(ref);
    
    if (admin == null)
      return null;

    try {
      Configuration[] confs = 
	admin.listConfigurations("(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + cd.getComponentName() + ")");
      
      if (confs != null && confs.length == 1) {
	return confs[0];
      } 
      
      confs = 
	admin.listConfigurations("(" + Constants.SERVICE_PID + "=" + cd.getComponentName() + ")");
      
      if (confs != null && confs.length == 1) {
	return confs[0];
      }
    } catch (InvalidSyntaxException e) {}

      return null;
  }

  /**
   * this method listens for service changes it will check the type of the
   * event and perform correct actions depending on the event. More
   * explanations are attatched within the method.
   *
   * @param event
   */
  public synchronized void serviceChanged(ServiceEvent event) {
    if(event.getType()== ServiceEvent.REGISTERED || event.getType()== ServiceEvent.UNREGISTERING){
      try {
        maintain(event);
      } catch (ComponentException e) {
        ComponentActivator.error(e);
      }
    }
  }

  /**
   * Listens for BundleEvents from the framework and creates a
   * ComponentDeclaration this is usually the main entrance for a bundle
   * having declarative components. Further the method will parse the declared
   * xml files and create ComponentDeclarations of the files. If the event is
   * a stop event then the method will check if the bundle has declarative
   * components and if they are activated. If that is the case the
   * configurations will be disabled and unregistered from the framework.
   *
   * @param event the event from the framework
   */
  public synchronized void bundleChanged(BundleEvent event) {
    String manifestEntry = (String) event.getBundle().getHeaders().get(ComponentConstants.SERVICE_COMPONENT);
    if (manifestEntry == null) {
      // Not a declarative component
      return;
    }

    switch (event.getType()) {
    case BundleEvent.STARTED:
      ComponentActivator.debug("Bundle #" + event.getBundle().getBundleId() + " is started");
      try {
        ComponentActivator.debug("The Declared components are:" + manifestEntry);

        String[] manifestEntries = manifestEntry.split(",");
        for (int i = 0; i < manifestEntries.length; i++) {

          URL resourceURL = event.getBundle().getResource(manifestEntries[i]);
          if (resourceURL == null) {
            ComponentActivator.error("error can't find:" + manifestEntries[i]);
          } else {
            ComponentActivator.debug("Parsing " + manifestEntries[i]);

            // parse the document and retrieve a component declaration
            ComponentParser parser = new ComponentParser();
	    ArrayList decls = parser.readXML(event.getBundle(), resourceURL);
	    
	    for (int o = 0; o < decls.size(); o++) {
	      ComponentDeclaration componentDeclaration = (ComponentDeclaration)decls.get(o);
	      componentDeclaration.setDeclaraingBundle(event.getBundle());
	      componentDeclaration.setXmlFile(manifestEntries[i]);

		try {
		  ComponentActivator.debug("Evaluate " + componentDeclaration.getComponentName());
		  evaluate(componentDeclaration, false);
		} catch (ComponentException e) {
		  ComponentActivator.error("error when evaluating started bundle with component", e);
		}
	    }
          }
        }

      } catch (Exception e) {
        ComponentActivator.error("error in bundleChanged(..)", e);
      }

      break;
    case BundleEvent.STOPPED:

      try {
        synchronized (activeComponents) {

          for (int i = 0; i < activeComponents.size(); i++) {
            try {
              DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(i);
              ComponentDeclaration componentDeclaration = component.getComponentDeclaration();

              // check if the event bundle matches the declaration bundle
              if (componentDeclaration.getDeclaringBundle().equals(event.getBundle())) {
                ComponentActivator.debug("Disable " + componentDeclaration.getComponentName() + " due to BUNDLE STOPPED");
                disableComponent(component, event.getBundle(), true);
                i--;
              }
            } catch (IndexOutOfBoundsException e) {
              ComponentActivator.error("index OOB WHEN GOING THROUGH ACTIVE", e);
              // the list can become empty before the iteration therefore catch
              // the exception but do nothing but this should not happen
            }
          }
        }
        synchronized (inactiveComponents) {
          for (int i = 0; i < inactiveComponents.size(); i++) {
            try {
              ComponentDeclaration componentDeclaration = (ComponentDeclaration) inactiveComponents.get(i);
              if (componentDeclaration.getDeclaringBundle().equals(event.getBundle())) {
                inactiveComponents.remove(i);
                i--;
              }
            } catch (IndexOutOfBoundsException e) {
              // the list can become empty before the iteration starts therefore
              // catch the exception but do nothing
              ComponentActivator.error("index OOB WHEN GOING THROUGH INACTIVE", e);
            }
          }

          // BA: Logging:
          ComponentActivator.debug("component counter:" + componentCounter);
          ComponentActivator.debug("the size of active components array:" + activeComponents.size());
          if (activeComponents.size() > 0) {
            for (int i = 0; i < activeComponents.size(); i++) {
              ComponentActivator.debug("Element:" + ((DeclarativeComponent) activeComponents.get(i)).getComponentDeclaration().getComponentName());
            }
          }
          ComponentActivator.debug("the size of inactive components array:" + inactiveComponents.size());
          if (inactiveComponents.size() > 0) {
            for (int i = 0; i < inactiveComponents.size(); i++) {
              ComponentActivator.debug("Element:" + ((ComponentDeclaration) inactiveComponents.get(i)).getComponentName());
            }
          }
          // BA: end logging

        }
      } catch (Exception e) {
        ComponentActivator.error("Error when stopping bundle " + event.getBundle(), e);
      }
    }// end switch
  }

  /**
   * this method will dispose a given component configuration it will search
   * through all active components and if a match on handed object is found
   * the method will unbind, deactivate and unregister the services if there
   * are any.
   *
   * @param componentObject the component object to be disposed
   * @throws ComponentException if failed during the process
   */
  public synchronized void disposeComponent(Object componentObject)
      throws ComponentException {
    for (int i = 0; i < activeComponents.size(); i++) {
      DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(i);
      List contexts = component.getComponentContexts();
      if (contexts != null) { // CustomComponentFactory has no contexts
        for (int j = 0; j < contexts.size(); j++) {
          ComponentContext context = (ComponentContext) contexts.get(j);
          Object element = context.getComponentInstance().getInstance();
          if (element.equals(componentObject)) {
            invoke(component, context.getComponentInstance(), false);
            deactivateContext(context);
            try {
              activeComponents.remove(component);
              if (component.getServiceRegistration() != null) { // ImmediateComponent has no service
                component.getServiceRegistration().unregister();
              }
              if (!inactiveComponents.contains(component.getComponentDeclaration())) {
                inactiveComponents.add(component.getComponentDeclaration());
              }
            } catch (Exception e) {
              throw new ComponentException("error unregistering " + component.getComponentDeclaration().getComponentName() + " due to:\n" + e, e.getCause());
            }
          }// end if(element.equals(componentObject))
        }//for(int j=0;j<contexts.size();j++)
      }// if(contexts != null)
    }// end for(int i=0;i<activeComponents.size();i++)
  }

  public synchronized void disableComponent(String componentName,
                                            Bundle requestBundle,
                                            boolean isStopping) throws ComponentException {
    for (int i = 0; i < activeComponents.size(); i++) {
      Object object = activeComponents.get(i);
      
      DeclarativeComponent component = (DeclarativeComponent) object;
      ComponentDeclaration componentDeclaration = component.getComponentDeclaration();
      
      if (componentDeclaration.getComponentName().equals(componentName)) {
        disableComponent(component, requestBundle, isStopping);
      }
    }
  }

  /**
   * this method will disable a component or components after a given name. It
   * will ensure that the component is in the same bundle as the bundle
   * calling this method. The method cannot just only check if its a
   * DeclarativeComponent type. It has to check the implementation of the
   * DeclarativeComponent because it will perfom diffrent actions depending on
   * the implementation that is to be disabled.
   *
   * @param declComponent the component(s) to be disable
   * @param requestBundle the bundle the requesting component is located in
   */
  public synchronized void disableComponent(DeclarativeComponent declComponent,
                                            Bundle requestBundle,
                                            boolean isStopping) throws ComponentException {
    ComponentActivator.debug("disable component was called in SCR");

    ComponentDeclaration componentDeclaration = declComponent.getComponentDeclaration();

    /*
     * check that the requesting bundle is in the same bundle as
     * this component declared int the componentDeclaration
     */
    if (!componentDeclaration.getDeclaringBundle().equals(requestBundle)) {
      ComponentActivator.debug("Cannot disable component from other bundle");
      return;
    }

    List contexts = declComponent.getComponentContexts();
    if (contexts != null) {
      for (int j = 0; j < contexts.size(); j++) {
        ComponentContext componentContext = (ComponentContext) contexts.remove(j);
        if (componentContext != null) {
          invoke(declComponent, componentContext.getComponentInstance(), false);
          deactivateContext(componentContext);
        }
        j--;
      }
    }

    try {
      synchronized (activeComponents) {
        activeComponents.remove(declComponent);
      }
      if (declComponent.getServiceRegistration() != null) {
        try {
          declComponent.getServiceRegistration().unregister();
        } catch (IllegalStateException e) {
          // Do nothing. The service will disappear when the bundle is stopped
        }
      }
      synchronized (inactiveComponents) {
        if (!inactiveComponents.contains(componentDeclaration)) {
          inactiveComponents.add(componentDeclaration);
        }
      }
    } catch (Exception e) {
      ComponentActivator.error(e);
      throw new ComponentException("error unregistering the component service factory " + componentDeclaration.getComponentName() + ":" + e, e.getCause());
    }
  }

  /**
   * this method will enable component(s) with a given name it will also
   * ensure that the component is in the same bundle as the requesting
   * component is before it will enable it.
   *
   * @param componentName
   *            the name of the component(s) to be enabled null enables all in the bundle
   * @param requestBundle
   *            the bundle the requesting component is located in
   */
  public synchronized void enableComponent(String componentName,
                                           Bundle requestBundle) throws ComponentException {
    // get a clone of the active components to avoid synchronize problem
    Vector currentComponents = (Vector) inactiveComponents.clone();
    for (int i = 0; i < currentComponents.size(); i++) {
      ComponentDeclaration componentDeclaration = (ComponentDeclaration) currentComponents.get(i);
      if ((componentDeclaration.getComponentName().equals(componentName) || componentName == null)
          && componentDeclaration.getDeclaringBundle().equals(requestBundle)) {
        // evalute the declaration override the componentdeclarations isAutoEnabled()
        ComponentActivator.debug("Starting evaluator");
        evaluate(componentDeclaration, true);
      }
    }
  }

  /**
   * locates a service with a given name declared in the reference element of
   * a component declaration. This method will use the internal bundle context
   * to locate services.
   *
   * @param ServiceName the name of the service declared in the XML file
   * @param componentName the name of the requesting component
   *
   * @return object the service object
   */
  public synchronized Object locateService(String serviceName,
                                           String componentName) throws ComponentException {
    // TODO Check if ID is needed here to identify the component split up in small methods
    for (int i = 0; i < activeComponents.size(); i++) {
      DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(i);
      if (component.getComponentDeclaration().getComponentName().equals(componentName)) {
        ComponentDeclaration componentDeclaration = component.getComponentDeclaration();
        Iterator referenceIterator = componentDeclaration.getReferenceInfo().iterator();
        while (referenceIterator.hasNext()) {
          ComponentReferenceInfo referenceInfo
            = (ComponentReferenceInfo) referenceIterator.next();
          if (referenceInfo.getReferenceName().equals(serviceName)) {
            String interfaceName = referenceInfo.getInterfaceType();
            ServiceReference serviceReference = bundleContext.getServiceReference(interfaceName);
            if (serviceReference != null) {
              return componentDeclaration.getDeclaringBundle()
                .getBundleContext().getService(serviceReference);
            }
          }// end if(referenceInfo.getReferenceName().equals(serviceName))
        }// end while(referenceIterator.hasNext())
      }// end if(component.getComponentDeclaration().getComponentName().equals(componentName))
    }// end for(int i=0;i<activeComponents.size();i++)
    return null;
  }

  /**
   * locates services with a given name declared in the reference element of a
   * component declaration. This method will use the internal bundle context
   * to locate services.
   *
   * @param ServiceName the name of the service declared in the XML file
   * @param componentName the name of the requesting component
   *
   * @return object the service object
   */
  public synchronized Object[] locateServices(String serviceName,
                                              String componentName) throws ComponentException {
    // TODO Check if ID is needed here to identify the component & split up in smaller methods
    for (int i = 0; i < activeComponents.size(); i++) {
      DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(i);
      if (!component.getComponentDeclaration().getComponentName().equals(componentName)) {
        continue;
      }
      ComponentDeclaration componentDeclaration = component.getComponentDeclaration();
      Iterator referenceIterator = componentDeclaration.getReferenceInfo().iterator();
      while (referenceIterator.hasNext()) {
        ComponentReferenceInfo referenceInfo
          = (ComponentReferenceInfo) referenceIterator.next();
        if (!referenceInfo.getReferenceName().equals(serviceName)) {
          continue;
        }
        String interfaceName = referenceInfo.getInterfaceType();
        /* locate the services */
        ServiceReference[] serviceReferences = null;
        try {
          serviceReferences = bundleContext.getServiceReferences(interfaceName, null);
        } catch (InvalidSyntaxException ignore) {} // We're not using a filter
        if (serviceReferences == null) {
          continue;
        }
        Object[] services = new Object[serviceReferences.length];
        for (int j = 0; j < serviceReferences.length; j++) {
          Object service = bundleContext.getService(serviceReferences[j]);
          if (service == null) {
            throw new ComponentException("fatal internal error in locateServices(..) got service reference but no service object");
          }
          services[j] = service;
        }
        return services;
      }
    }// end for(int i=0;i<activeComponents.size();i++)
    return null;
  }

  /**
   * this method checks if a ComponentDeclaration is satisfied or not. It will
   * check that all references/dependencies are available. If they are a true
   * will be returned other wise false.
   *
   * @param componentDeclaration
   * @return true if satisfies false otherwise
   */
  private boolean isSatisfied(ComponentDeclaration componentDeclaration) {
    Iterator referenceIterator = componentDeclaration.getReferenceInfo().iterator();
    while (referenceIterator.hasNext()) {
      ComponentReferenceInfo componentReference = (ComponentReferenceInfo) referenceIterator.next();
      String cardinality = componentReference.getCardinality();
      String interfaceName = componentReference.getInterfaceType();
      String filter = componentReference.getTarget();

      try {

        ServiceReference[] reference = bundleContext.getServiceReferences(interfaceName, filter);
        if (reference == null && (CARDINAL_1_1.equals(cardinality) || CARDINAL_1_N.equals(cardinality))) {
          return false;
        }
      } catch (InvalidSyntaxException e) {
        ComponentActivator.error("error when checking if " + componentDeclaration.getComponentName() + " is satisfied check the target attribute in in the component declaration", e);
      }
    }
    return true;
  }

  /**
   * this method registers a component factory into the framework. A component
   * factory is responsible for creating new components on demand.
   *
   * @param componentDeclaration the component declaration for the component
   * @return CustomComponentFactory the factory which has been created
   * @throws ComponentException if fails to register a component factory
   */
  private CustomComponentFactory registerComponentFactory(ComponentDeclaration componentDeclaration)
      throws ComponentException {
    
    Dictionary properties = new Hashtable();
    properties.put(ComponentConstants.COMPONENT_FACTORY, componentDeclaration.getFactory());
    properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName());
    // is this stated somewhere??
    properties.put(Constants.SERVICE_DESCRIPTION, COMPONENT_FACTORY_SERVICE_DESC);
    properties.put(Constants.SERVICE_PID, COMPONENT_FACTORY_SERVICE_PID);
    
   
    CustomComponentFactory factory = new CustomComponentFactory(componentDeclaration);

    try {
      ServiceRegistration registration
        = componentDeclaration.getDeclaringBundle().getBundleContext()
          .registerService(ComponentFactory.class.getName(), factory, properties);
      factory.setServiceReference(registration.getReference());
      factory.setServiceRegistration(registration);

      return factory;
    } catch (Exception e) {
      throw new ComponentException("Error registering component factory", e.getCause());
    }
  }

  /**
   * this method registers a CustomComponentServiceFactory into the framework
   * the service factory will always create new instances on demand.
   *
   * @param interfaceNames       a string array with interfaces for this services
   * @param componentDeclaration the component declaration for this component
   * @return CustomComponentServiceFactory the service factory object
   * @throws ComponentException if fails to register
   */
  private CustomComponentServiceFactory registerComponentServiceFactory
      (String[] interfaceNames, ComponentDeclaration componentDeclaration)
      throws ComponentException {

    Dictionary properties = new Hashtable();
    properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName());
    properties.put(Constants.SERVICE_DESCRIPTION, SERVICE_FACTORY_SERVICE_DESC);


    CustomComponentServiceFactory serviceFactory
      = new CustomComponentServiceFactory(interfaceNames, componentDeclaration);


    try {
      ServiceRegistration registration
        = componentDeclaration.getDeclaringBundle().getBundleContext()
          .registerService(interfaceNames, serviceFactory, properties);
      serviceFactory.setServiceReference(registration.getReference());
      serviceFactory.setServiceRegistration(registration);
      return serviceFactory;
    } catch (Exception e) {
      throw new ComponentException("Error registering component service factory:" + e, e.getCause());
    }
  }

  /**
   * this method registers a delayed component into the framework delayed
   * components declares service(s) and instances will be created on demand.
   *
   * @param interfaceNames        an array with interfaces for this component
   * @param componentDeclaration  the component declaration for this component
   * @return CustomDelayedService if managed to register else null
   * @throws ComponentException   if fails to register the services declared
   */
  private CustomDelayedService registerDelayedComponent (String[] interfaceNames, 
							 ComponentDeclaration componentDeclaration, 
							 Dictionary overriddenProps)
    throws ComponentException {

    /* Dictionary properties = new Hashtable();

       ArrayList propertyInfo = componentDeclaration.getPropertyInfo();

       for (Iterator iter = propertyInfo.iterator(); iter.hasNext();) {
        ComponentPropertyInfo compProp = (ComponentPropertyInfo) iter.next();
        properties.put(compProp.getName(), compProp.getValue());
       }
    
       properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName()); 
       MO: where is this stated? Can't find it in the spec..
       properties.put(Constants.SERVICE_DESCRIPTION, DELAYED_SERVICE_DESC); 
       properties.put(Constants.SERVICE_PID, componentDeclaration.getComponentName()); 
    */

    CustomDelayedService delayedService
      = new CustomDelayedService(interfaceNames, componentDeclaration, overriddenProps);

    try {
      ServiceRegistration registration
        = componentDeclaration.getDeclaringBundle().getBundleContext()
	.registerService(interfaceNames, delayedService, delayedService.getProperties());


      delayedService.setServiceRegistration(registration);
      delayedService.setServiceReference(registration.getReference());

      return delayedService;
    } catch (Exception e) {
      ComponentActivator.error(e); 
      throw new ComponentException("error registering delayed component service:" + e, e.getCause());
    }
  }

  /**
   * this method creates a componentContext with all features within
   *
   * @param componentDeclaration the componentDeclaration
   * @param componentID          the Components ID
   * @param registration         the Registration object if any
   * @param properties           the properties for this component
   * @param usingBundle          the bundle using the service the component offers
   * @param requestBundle        the bundle requesting the service
   * @return ComponentContext if creation succeeded else null
   * @throws ComponentException if fails to create a context
   */
  private ComponentContext createComponentContext(ComponentDeclaration componentDeclaration,
                                                  long componentID,
                                                  ServiceRegistration registration,
                                                  Dictionary properties,
                                                  Bundle usingBundle,
                                                  Bundle requestBundle) throws ComponentException {
    try {
      String componentName = componentDeclaration.getImplementation();
      // create a new factory object
      Object element = null;
      try {
        //element = Class.forName(componentName).newInstance();
        element = componentDeclaration.loadClass().newInstance();
        ComponentActivator.debug("Creating instance:" + element);
      } catch (ClassNotFoundException e) {
        ComponentActivator.error("error in Component Creation", e);
      } catch (IllegalAccessException e) {
        ComponentActivator.error("Error in Component Creation  Can't access constructor of:" + componentName, e);
      } catch (InstantiationException e) {
        ComponentActivator.error("Error in Component Creation  Can't create new instance of:" + componentName, e);
      } catch (Exception e) {
        ComponentActivator.error("Error in Component Creation", e);
      }
      return new ComponentContextImpl(new ComponentInstanceImpl(element, this),
                                      bundleContext,
                                      properties,
                                      registration,
                                      this,
                                      usingBundle,
                                      requestBundle);
    } catch (Exception e) {
      throw new ComponentException(e);
    }
  }

  /* This function has been added by MO.
     Will search for the <methodName>(<type>) in the given class.
     By first looking in klass after 
     <methodName>(ServiceReference) then
     <methodName>(Interface of Service) then
     
     If no method is found it will then continue to the super class.
     This is how it is described in the specification..

  */

  private void invokeEventMethod(DeclarativeComponent component,
				 ComponentInstance instance,
				 String methodName, 
				 ServiceReference ref,
				 String serviceInterface) {
    Class klass = instance.getInstance().getClass();
    Method method = null;
    Object arg = null;

    Object service = bundleContext.getService(ref); 
    // service can be null if the service is unregistering.

    Class serviceClass = null;
	
    try {
      serviceClass = component.getComponentDeclaration().loadClass(serviceInterface);
    } catch (ClassNotFoundException e) {
      log(LogService.LOG_ERROR, "Could not load class " + serviceInterface);
      return ;
    }
    
    while (klass != null && method == null) {
      Method[] ms = klass.getDeclaredMethods(); 

      // searches this class for a suitable method.
      for (int i = 0; i < ms.length; i++) {
	if (methodName.equals(ms[i].getName()) &&
	    (Modifier.isProtected(ms[i].getModifiers()) ||
	     Modifier.isPublic(ms[i].getModifiers()))) {

	  Class[] parms = ms[i].getParameterTypes();

	  if (parms.length == 1) {
	    
	    try {
	      if (ServiceReference.class.equals(parms[0])) {
		
		ms[i].setAccessible(true);
		ms[i].invoke(instance.getInstance(), new Object[] { ref });
		return ;
		
	      } else if (parms[0].isAssignableFrom(serviceClass)) {
		ms[i].setAccessible(true);
		ms[i].invoke(instance.getInstance(), new Object[] { service });

		return ;
	      }
	      
	    } catch (IllegalAccessException e) {
	      log(LogService.LOG_ERROR, "Could not access the method: " + methodName + " got " + e);
	    } catch (InvocationTargetException e) {
	      log(LogService.LOG_ERROR, "Could not invoke the method: " + methodName + " got " + e);
	    }
	  }
	}
      }
      
      klass = klass.getSuperclass();
    }
    
    // did not find any such method.
    log(LogService.LOG_ERROR, "Could not find bind/unbind method \"" + methodName + "\"");
  }


  /**
   * This method will track the dependencies and give them to the declarative
   * service which this class controlls. Dependencies are kept in the class
   * ComponentReferenceInfo.The class can be used for unbind as well as bind
   * actions.
   */
  public void invoke(DeclarativeComponent component,
                     ComponentInstance componentInstance,
                     boolean bind) {
    ComponentDeclaration componentDeclaration = component.getComponentDeclaration();

    ComponentActivator.debug("Tracking references for activation");
    ArrayList referenceInfo = componentDeclaration.getReferenceInfo();
    if (referenceInfo == null) {
      return;
    }

    Iterator it = referenceInfo.iterator();
    while (it.hasNext()) {
      ComponentReferenceInfo componentRef = (ComponentReferenceInfo) it.next();
      if (componentRef == null || componentRef.getInterfaceType() == null) {
        continue;
      }

      String interfaceName = componentRef.getInterfaceType();
      ComponentActivator.debug("service interface name is:" + interfaceName);
      String targetFilter = componentRef.getTarget();
      Object componentObject = componentInstance.getInstance();

      String methodName = null;
      if (bind) {
        methodName = componentRef.getBind();
        ComponentActivator.debug("The bind-method is:" + methodName);
      } else {
        methodName = componentRef.getUnbind();
        ComponentActivator.debug("The unbind-method is:" + methodName);
      }

      if (methodName != null
          && (componentRef.getCardinality().equals(CARDINAL_0_1) ||
              componentRef.getCardinality().equals(CARDINAL_1_1))) {

        ServiceReference[] serviceReferences = null;
        try {
          serviceReferences = bundleContext.getServiceReferences(interfaceName, targetFilter);
        } catch (InvalidSyntaxException e) {
          throw new ComponentException("error getting services due to:\n" + e.getMessage(), e.getCause());
        }

        // check that the reference is there if it isn't then do nothing. Null may occur if the
        // reference is in the same bundle as the component is and the bundle is stopped

        if (serviceReferences == null) {
          if (bind && componentRef.getCardinality().equals(CARDINAL_1_1)) {
            throw new ComponentException("error getting service " + interfaceName + " in invokeReferences() the reference is null");
          } else {
            continue;
          }
        }
        ComponentActivator.debug("The service reference is:" + serviceReferences[0] + " with cardinality:" + componentRef.getCardinality());

        Object reference = bundleContext.getService(serviceReferences[0]);

        if (reference == null) {
          throw new ComponentException("error getting service " + interfaceName + " in invokeReferences() the service is null");
        }
	ComponentActivator.debug("Trying to invoke " + methodName + " in class:" + componentDeclaration.getImplementation());
	invokeEventMethod(component,
			  componentInstance, 
			  methodName, 
			  serviceReferences[0],
			  componentRef.getInterfaceType()); //MO: added this

	if (bind) {
	  component.bindReference(serviceReferences[0]);
	} else {
	  component.unBindReference(serviceReferences[0]);
	}

      } else if (componentRef.getCardinality().equals(CARDINAL_0_N) ||
                 componentRef.getCardinality().equals(CARDINAL_1_N)) {

        // this is more complex the component has declared that
        // all services available in the framwork should be bounded

        try {
          ServiceReference[] serviceReferences
            = bundleContext.getServiceReferences(interfaceName, targetFilter);

          ComponentActivator.debug("cardinality:" + componentRef.getCardinality());

          if (serviceReferences == null) {
            continue;
          }
          for (int i = 0; i < serviceReferences.length; i++) {
            Object serviceInstance = bundleContext.getService(serviceReferences[i]);
            if (serviceInstance == null ||
                methodName == null ||
                componentObject == null ||
                interfaceName == null) {
              continue;
            }
            ComponentActivator.debug("Trying to invoke " + methodName + " in class:" + componentDeclaration.getImplementation());
            ComponentActivator.debug("Component is: " + componentObject);
            ComponentActivator.debug("methodName: " + methodName);
            ComponentActivator.debug("InterfaceName: " + interfaceName);
            ComponentActivator.debug("ServiceInstance: " + serviceInstance);
	    
	    // BA: This did not wait for the Reinvoker Thread
	    invokeEventMethod(component, componentInstance, 
			      methodName, serviceReferences[i],
			      componentRef.getInterfaceType());

            if (bind) {
              component.bindReference(serviceReferences[i]);
            } else {
              component.unBindReference(serviceReferences[i]);
            }
          }
        } catch (InvalidSyntaxException e) {
          ComponentActivator.error(e);
          throw new ComponentException("error getting multiple services due to:\n" + e.getMessage(), e.getCause());
        } catch (Exception e) {
          ComponentActivator.error(e);
          throw new ComponentException("error getting multiple services due to:\n" + e.getMessage(), e.getCause());
        }
      }//if
    }//end while
  }

  /**
   * This method will evaluate a componentDeclaration and handle after the
   * component declaration
   *
   * @author Magnus Klack
   */
  public void evaluate(ComponentDeclaration componentDeclaration, boolean overideEnable) {
    // variable representing if the component implements managed service
    boolean isManaged = false;

    // Variable representing if the component implements managed service factory
    boolean isManagedFactory = false;


    
    // check if this depends on some configuration admin stuff.
    /* MO: my code starts here */
    Configuration config = null;
    try {
      config = getConfiguration(componentDeclaration);

      if (config != null) {

	isManagedFactory = (config.getFactoryPid() != null);
	
	if (!isManagedFactory) {
	  isManaged = true; /* because it can't be both right? */
	}
      }
	
    } catch (IOException e) {
      /* ignore */
    }
    
    if (componentDeclaration.isAutoEnable() || overideEnable) {

      /* if it is enable when the services have to be registered */
      if (componentDeclaration.getFactory() != null) {
        /* check if the component is satisfied and enabled */
        if (isSatisfied(componentDeclaration) && componentDeclaration.isAutoEnable()) {
          ComponentActivator.debug("register component factory for:" + componentDeclaration.getComponentName());

          CustomComponentFactory componentFactory = registerComponentFactory(componentDeclaration);
          activeComponents.add(componentFactory);
          inactiveComponents.remove(componentDeclaration);

        } else {
          synchronized (inactiveComponents) {
            ComponentActivator.debug(componentDeclaration.getComponentName() + " is not satisfied");
            inactiveComponents.add(componentDeclaration);
          }
        }
      } else {
        /* this is a immediate or servicefactory component */
        if (isSatisfied(componentDeclaration)) {
          String[] allInterfaces = null;

          // check if it has references ,i.e, if its a delayed component or managed service component
          boolean handledServices = false;
          if (componentDeclaration.getServiceInfo().size() > 0
              || isManaged
              || isManagedFactory) {
            handledServices = true;

            // the tricky thing is that we have to create one CustomComponent instance for MANY provided interface
            ArrayList serviceInfos = componentDeclaration.getServiceInfo();

            Iterator serviceIterator = serviceInfos.iterator();
            Vector vectorInterfaces = new Vector();

            while (serviceIterator.hasNext()) {
              ComponentServiceInfo info = (ComponentServiceInfo) serviceIterator.next();
              ArrayList interfaces = info.getComponentInterfaces();
              vectorInterfaces.addAll(interfaces);
            } // end while(serviceIterator.hasNext())

	    allInterfaces = new String[vectorInterfaces.size()];
	    allInterfaces = (String[]) vectorInterfaces.toArray(allInterfaces);
	    
          } 

          if (componentDeclaration.isServiceFactory()) {
            ComponentActivator.debug("register component service factory for:" + componentDeclaration.getComponentName());
            CustomComponentServiceFactory serviceComponent
              = registerComponentServiceFactory(allInterfaces, componentDeclaration);
            activeComponents.add(serviceComponent);
            inactiveComponents.remove(componentDeclaration);

          } else if (!handledServices) {
            ComponentActivator.debug("register immediate component for:" + componentDeclaration.getComponentName());
            ImmediateComponent immediateComponent = 
	      new ImmediateComponent(componentDeclaration, 
				     config == null ? null : config.getProperties());
            activeComponents.add(immediateComponent);
            immediateComponent.activate();
            inactiveComponents.remove(componentDeclaration);

          } else {
            ComponentActivator.debug("register delayed service for:" + componentDeclaration.getComponentName());

            CustomDelayedService delayedComponent
              = registerDelayedComponent(allInterfaces, componentDeclaration, 
					 config == null ? null : config.getProperties());  // MO: test

            activeComponents.add(delayedComponent);
            if (componentDeclaration.isImmediate()) {
	      
              // The delayed component is really an immediate component. Activate it by getting the service.
              Object obj = delayedComponent.getService(componentDeclaration.getDeclaringBundle(),
                                                       delayedComponent.getServiceRegistration());
	      // MO: is this ok? Shouldn't this service be deactivated after the call to unget?
              delayedComponent.ungetService(componentDeclaration.getDeclaringBundle(),
                                            delayedComponent.getServiceRegistration(),
                                            obj);
	    }
            inactiveComponents.remove(componentDeclaration);
          }
        } else {
          ComponentActivator.debug(componentDeclaration.getComponentName() + " is not satisfied");
          if (!inactiveComponents.contains(componentDeclaration)) {
            inactiveComponents.add(componentDeclaration);
          }
        }
      }
    } else {
      ComponentActivator.debug("autoEnable is false in the component delcaration");
      if (!inactiveComponents.contains(componentDeclaration)) {
        inactiveComponents.add(componentDeclaration);
      }
    } // if(componentDeclaration.isAutoEnable())
  }

  /**
   */
  public void deactivateContext(ComponentContext componentContext) {
    Object componentInstance = componentContext.getComponentInstance().getInstance();
    try {
      ComponentActivator.debug("trying to invoke " + componentInstance.toString() + ".deactivate(...) method ");
      Method method = componentInstance.getClass()
        .getDeclaredMethod(DEACTIVATE_METHOD_NAME, new Class[] { ComponentContext.class });
      method.setAccessible(true);
      method.invoke(componentInstance, new Object[] { componentContext });
    } catch (NoSuchMethodException e) {
      ComponentActivator.debug(componentInstance + " does not declare an deactivate(..) method", e);
    } catch (InvocationTargetException e) {
      throw new ComponentException(e.getMessage(), e.getCause());
    } catch (IllegalAccessException e) {
      throw new ComponentException(e.getMessage(), e.getCause());
    }
  }

  /**
   */
  public void activateContext(ComponentContext componentContext) {
    if (componentContext == null) {
      throw new ComponentException("The ComponentContext is null");
    }

    Object componentInstance = null;
    try {
      componentInstance = componentContext.getComponentInstance().getInstance();
    } catch (Exception e) {
      throw new ComponentException(e.getMessage(), e.getCause());
    }

    if (componentInstance == null) {
      throw new ComponentException("Can't get the component instance");
    }

    try {
      ComponentActivator.debug("trying to invoke " + componentInstance.toString() + ".activate(...) method ");
      Method method = componentInstance.getClass()
        .getDeclaredMethod(ACTIVATE_METHOD_NAME, new Class[] { ComponentContext.class });
      method.setAccessible(true);
      method.invoke(componentInstance, new Object[] { componentContext });
    } catch (NoSuchMethodException e) {
      ComponentActivator.debug(componentInstance + " does not declare an activate(..) method", e);
    } catch (InvocationTargetException e) {
      ComponentActivator.error("\nWarning InvocationTargetException occured!\n"+
                               "When activating:\n" + componentInstance +".activate()\n"+
                               "please check your implementation", e);
    } catch (IllegalAccessException e) {
      throw new ComponentException(e.getMessage(), e.getCause());
    } catch (Exception e) {
      throw new ComponentException(e.getMessage(), e.getCause());
    }
  }

  // TODO: Understand the details of this method. 
  public void maintain(ServiceEvent event) {
    ComponentActivator.debug("Maintainer process is started");

    try {
      /* get all the object classes */
      String[] objectClasses = (String[]) event.getServiceReference().getProperty("objectClass");

      for (int i = 0; i < objectClasses.length; i++) {
        String objectClass = objectClasses[i];

        /*
         * check if the event is a REGISTERED event check if any inactive
         * component has this registered event as reference if so try to
         * evalutate the that component with the
         * evaluateComponentDeclaration(..) method. if the component is
         * satisfied,i.e has all dependencies it needs when it will be activated
         * and its services, if any , will be registered into the framework.
         */
        if (event.getType() == ServiceEvent.REGISTERED) {
          // go through all inactive components to se if any of them has a reference to this service
          Vector copyOfInactiveComponents = (Vector) inactiveComponents.clone();

          for (int j = 0; j < copyOfInactiveComponents.size(); j++) {
            ComponentDeclaration componentDeclaration = (ComponentDeclaration) copyOfInactiveComponents.get(j);

            ArrayList references = componentDeclaration.getReferenceInfo();
            Iterator referenceIterator = references.iterator();

            while (referenceIterator.hasNext()) {
              ComponentReferenceInfo referenceInfo = (ComponentReferenceInfo) referenceIterator.next();

              String filter = referenceInfo.getTarget();
              String eventTarget = (String) event.getServiceReference().getProperty("component.name");

	      if (referenceInfo.getInterfaceType().equals(objectClass))
              /*
               * check if the names of the declared interface and the object class are
               * equal to each other and that the component declaration is auto
               * enabled. Also make sure that the targets of the declaration equals the
               * component name
               */
              if (referenceInfo.getInterfaceType().equals(objectClass)
                  && componentDeclaration.isAutoEnable()) {
                try {
                  /*
                   * try to start the component again this may failed if the
                   * component has other unsatisfied references which
                   * aren't obvious here, but the evaluateComponentDeclaration(...)
                   * method will take care of it. therefore no concerns about
                   * the filter is taken here.
                   */
                  evaluate(componentDeclaration, false);
                } catch (ComponentException e) {
                  ComponentActivator.error(e);
                }
              }// end if(referenceInfo.getInterfaceType().equals(objectClass) && componentDeclaration.isAutoEnable())
            }// end while (referenceIterator.hasNext())
          }// end for(int j=0;j<inactiveComponents.size();j++)
        }//end if(event.getType()==ServiceEvent.REGISTERED)

        /*
         * always check all active components no matter if it is a REGISTERED or
         * UNREGISTERED event if an active component has a refence to the event
         * perform correct action i.e, disable the component if its necessary,
         * rebind the component and call the components activate method.
         */
        for (int j = 0; j < activeComponents.size(); j++) {

          DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(j);
          ComponentDeclaration componentDeclaration = component.getComponentDeclaration();

          ArrayList references = componentDeclaration.getReferenceInfo();
          Iterator referenceIterator = references.iterator();

          while (referenceIterator.hasNext()) {
            ComponentReferenceInfo referenceInfo = (ComponentReferenceInfo) referenceIterator.next();

            /*
             * check if the name of the interface is the same as the newly registered.
             * Also check that the cardinality is 0..n or 1..n. Also ensure that a bind
             * method is declared If that is the case the active component should be
             * binded or rebinded again. This should only be done if the event is a
             * REGISTERED event
             */
            if (referenceInfo.getInterfaceType().equals(objectClass)
                && (referenceInfo.getCardinality().equals(CARDINAL_0_N)
                    || referenceInfo.getCardinality().equals(CARDINAL_1_N))
                && (event.getType() == ServiceEvent.REGISTERED)) {

              try {
                // make sure the context has been created before trying to bind this one.
                if (component.getComponentContext() != null
                    || component.getComponentContexts() != null) {

                  if (!component.getComponentDeclaration().isServiceFactory()) {

                    ComponentActivator.debug("NOTICE:\n" + component.getComponentDeclaration().getComponentName()
                            + "\nhas declared that its instance wants to be notified about all\n"
                            + "registered service of this type therefore an instance of\n"
                            + "this service will be passed to this components bind method\n");


		    ComponentInstance componentInstance = component.getComponentContext().getComponentInstance();
                    String methodName = referenceInfo.getBind();

                    if (methodName != null) {
                      String interfaceName = referenceInfo.getInterfaceType();
		      invokeEventMethod(component, componentInstance,
					methodName, event.getServiceReference(),
					interfaceName);
		    }

                    if (!component.isBoundedTo(event.getServiceReference())) {
                      activateContext(component.getComponentContext());
                      component.bindReference(event.getServiceReference());
                    }
                  } else {// if (component.getComponentDeclaration().isServiceFactory())

                    ComponentActivator.debug("\n" + component.getComponentDeclaration().getComponentName()
                            + "\nhas declared that its instances wants to be notified about all\n"
                            + "registered service of this type therefore an instance of\n"
                            + "this service will be passed to this components bind method\n");

                    List contexts = component.getComponentContexts();
                    Object serviceObject = bundleContext.getService(event.getServiceReference());

                    for (int z = 0; z < contexts.size(); z++) {
                      ComponentContext currentContext = (ComponentContext) contexts.get(z);
		      ComponentInstance componentInstance = currentContext.getComponentInstance(); 
                      String methodName = referenceInfo.getBind();

                      if (methodName != null) {
			invokeEventMethod(component, componentInstance, 
					  methodName, event.getServiceReference(),
					  referenceInfo.getInterfaceType());

                      }

                      activateContext(currentContext);
                      component.bindReference(event.getServiceReference());
                    }
                  }
                }
              } catch (ComponentException e) {
                ComponentActivator.error("error in serviceChanged(..) when reInvoking component", e);
              }
            }

            // variable represents if the component is bounded or not
            boolean isBounded = false;
            if (component != null) {
              isBounded = component.isBoundedTo(event.getServiceReference());
            }

            /*
             * check if the objectclass matches the interface in declaration also check
             * if its a UNREGISTERING event if so check if any component has the
             * unregistered reference as dependency. if a dependency exists perform the
             * legal action depending on the policy and the cardinality
             */
            if ((event.getType() == ServiceEvent.UNREGISTERING)
                && isBounded
                && referenceInfo.getInterfaceType().equals(objectClass)) {

              String cardinality = referenceInfo.getCardinality();
              String policy = referenceInfo.getPolicy();
	      
              /*
               * check if it is a static policy. Static policy will not try to
               * dynamically rebind the component SCR will instead disable the
               * component and try to create a new one.
               */
              if (policy.equals(STATIC_POLICY)) {

                /*
                 * make sure this component doesn't belong to a stopping
                 * bundle if that is the case this reference is in the same
                 * bundle as the stopping bundle. Don't disable the
                 * component it will be disabled later.
                 */
                if (componentDeclaration.getDeclaringBundle().getState() != Bundle.STOPPING) {
                  ComponentActivator.debug("disable " + componentDeclaration.getComponentName() + " because it has a reference to " + objectClass + " with a static policy");
                  disableComponent(component,//BA -componentDeclaration.getComponentName(),
                                   componentDeclaration.getDeclaringBundle(),
                                   false);
                  j--;
                } else {
                  ComponentActivator.debug(componentDeclaration.getComponentName() + "'s bundle is stopping this is probably a bundle internal reference");
                }

                /*
                 * evaluate the component again if the component is satisfied
                 * it will restart else it will be saved for later use TODO
                 * check if the evaluation method should care about the
                 * isAutoEnable attribute
                 */
                try {
                  if (componentDeclaration.getDeclaringBundle().getState() != Bundle.STOPPING) {
                    evaluate(componentDeclaration, false);

                  } else {
                    ComponentActivator.debug("This component's bundle is stopping ignore to restart");
                  }

                } catch (Exception e) {
                  ComponentActivator.error("error occured when trying to restart" + componentDeclaration.getComponentName(), e);
                  activeComponents.remove(component);
                  j--;
                }
              }

              /*
               * dynamic policy is a little bit more complex it will behave
               * diffrent depending on the cardinality declared. if the
               * cardinality is 1..1 and no references are available then the
               * component declaring dependency to this unregistered service cannot
               * work with out a rebind action. If the cardinality is 0..1 it means
               * it will be able to work with out this reference but the standard
               * procedure is to look for another equal reference and rebind the
               * component.
               */
              if (policy.equals(DYNAMIC_POLICY)
                  && component.getComponentDeclaration().getDeclaringBundle().getState() != Bundle.STOPPING) {

                ComponentActivator.debug("Found dynamic reference to:" + component.getComponentDeclaration().getComponentName());
                /*
                 * if the cardinality is 1..1 or 0..1 when unbind the old
                 * service a try to locate a new service and bind it to the service
                 */
                if (cardinality.equals(CARDINAL_1_1)
                    || cardinality.equals(CARDINAL_0_1)) {

                  /*
                   * check if a new service of this type is available
                   */
                  ServiceReference[] serviceReferences
                    = bundleContext.getServiceReferences(referenceInfo.getInterfaceType(),
                                                         referenceInfo.getTarget());

                  Object serviceObject = null;

                  Object oldServiceObject = bundleContext.getService(event.getServiceReference());

                  if (serviceReferences != null) {
                    serviceObject = bundleContext.getService(serviceReferences[0]);
                  }

                  /*
                   * this is the easy task because those component
                   * types do only have one instance of the component object
                   */
                  if (component instanceof ImmediateComponent
                      || component instanceof CustomDelayedService
                      || component instanceof CustomComponentFactory
                      && (serviceObject != null)) {

                    ComponentContext componentContext = component.getComponentContext();

                    /*
                     * ensure that the context isn't null if so it means that the
                     * service never been requested and the component is of the
                     * type CustomDelayedService. If the context is null then
                     * there are no instance(s) to rebind.
                     */

                    if (componentContext != null) {
                      String unbindMethod = referenceInfo.getUnbind();
                      String bindMethod = referenceInfo.getBind();

                      if (unbindMethod != null) {
			invokeEventMethod(component, componentContext.getComponentInstance(),
					  unbindMethod, event.getServiceReference(),
					  referenceInfo.getInterfaceType());
			  

                      }

                      if (bindMethod != null) {
			invokeEventMethod(component, componentContext.getComponentInstance(),
					  bindMethod, event.getServiceReference(),
					  referenceInfo.getInterfaceType());
			  
			
                      }

                      try {
                        activateContext(componentContext);
                      } catch (ComponentException e) {
                        ComponentActivator.error("error calling activate method", e);
                      }

                    }// end if(componentContext!=null)

                  } else if (component instanceof CustomComponentServiceFactory
                      && serviceObject != null) {

                    /* this is a service factory get all contexts */
                    List contexts = component.getComponentContexts();
                    String unbindMethod = referenceInfo.getUnbind();
                    String bindMethod = referenceInfo.getBind();

                    for (int x = 0; x < contexts.size(); x++) {
                      ComponentContext componentContext = (ComponentContext) contexts.get(x);

                      if (unbindMethod != null) {
		
			invokeEventMethod(component, componentContext.getComponentInstance(),
					  unbindMethod, event.getServiceReference(),
					  referenceInfo.getInterfaceType());
			
                      }

                      if (bindMethod != null) {

			invokeEventMethod(component, componentContext.getComponentInstance(),
					  bindMethod, event.getServiceReference(),
					  referenceInfo.getInterfaceType());
		      }

                      try {
                        activateContext(componentContext);
                      } catch (ComponentException e) {
                        ComponentActivator.error("error calling activate method", e);
                      }
                    }// end for(int x=0;x<contexts.size();x++)
                  }// end else if(component instanceof CustomComponentServiceFactory)

                  if (cardinality.equals(CARDINAL_1_1) && serviceObject == null) {
                    /*
                     * in this case disable the component it
                     * should not work with the declared cardinality
                     */
                    try {
                      ComponentActivator.debug("disable " + componentDeclaration.getComponentName() + " unable to bind new equal reference to component");
                      /* call the disable method */
                      disableComponent(component, //BA -componentDeclaration.getComponentName(),
                                       componentDeclaration.getDeclaringBundle(),
                                       false);
                      j--;
                    } catch (ComponentException e) {
                      ComponentActivator.error(e);
                    }

                  }// end if(cardinality.equals("1..1") && serviceObject==null)
                }

                /*
                 * if cardinality is 1..n means that the component wants all
                 * references of the given type and MUST have at least one
                 * reference bounded. 0..n means that the component can work
                 * with out a reference and should not be disabled if no
                 * references are available.
                 */
                if (cardinality.equals(CARDINAL_1_N) || cardinality.equals(CARDINAL_0_N)) {
                  ServiceReference[] newReferences = null;
                  //Object oldServiceObject = bundleContext.getService(event.getServiceReference());
                  String unbindMethod = referenceInfo.getUnbind();
                  String bindMethod = referenceInfo.getBind();

                  /* those component types only have one instance */
                  if (component instanceof ImmediateComponent
                      || component instanceof CustomDelayedService
                      || component instanceof CustomComponentFactory) {

                    ComponentContext componentContext = component.getComponentContext();

                    if (unbindMethod != null) {
		      invokeEventMethod(component, componentContext.getComponentInstance(),
					unbindMethod, event.getServiceReference(),
					referenceInfo.getInterfaceType());
		      component.unBindReference(event.getServiceReference());
		      
                    }

                    try {
                      newReferences = bundleContext.getServiceReferences(referenceInfo.getInterfaceType(),
                                                                         referenceInfo.getTarget());
                    } catch (Exception e) {
                      ComponentActivator.error("error getting servicereferences", e);
                    }

                    /* check this again because the old references can be the left */
                    if (newReferences != null) {

                      for (int x = 0; x < newReferences.length; x++) {
                        if (bindMethod != null) {
                          // MO: I'm not entirely sure about these..

			  if (!component.isBoundedTo(newReferences[x])) {
			    invokeEventMethod(component, componentContext.getComponentInstance(),
					      bindMethod, newReferences[x],
					      referenceInfo.getInterfaceType());
			    
			  }

                        }

                        try {
                          activateContext(componentContext);
                        } catch (ComponentException e) {
                          ComponentActivator.error("error calling activate method", e);
                        }
                      }// end for(int x=0;x<newReferences.length;x++)
                    }// end if(newReferences!=null)

                  } else if (component instanceof CustomComponentServiceFactory) {
                    /* this is even moore tricky here we need all the component instances */
                    List contexts = component.getComponentContexts();
                    try {
                      newReferences = bundleContext.getServiceReferences(referenceInfo.getInterfaceType(), null);
                    } catch (Exception e) {
                      ComponentActivator.error("error getting servicereferences", e);
                    }

                    if (newReferences != null) {
                      for (int m = 0; m < contexts.size(); m++) {
                        ComponentContext componentContext = (ComponentContext) contexts.get(m);

                        if (unbindMethod != null) { 
			  
			  invokeEventMethod(component, componentContext.getComponentInstance(),
					    unbindMethod, event.getServiceReference(),
					    referenceInfo.getInterfaceType());
			  component.unBindReference(event.getServiceReference());

                        }

                        for (int x = 0; x < newReferences.length; x++) {
                          if (bindMethod != null) {
			    if (!component.isBoundedTo(newReferences[x])) {

			      invokeEventMethod(component, componentContext.getComponentInstance(),
						bindMethod, newReferences[x],
						referenceInfo.getInterfaceType());
			    }
                          }

                          try {
                            activateContext(componentContext);
                          } catch (ComponentException e) {
                            ComponentActivator.error("error calling activate method" + e);
                          }
                        }// end for(int x=0;x<newReferences.length;x++)
                      }//end for(int m=0; m<contexts.size();m++)
                    }// end if(newReferences!=null)
                  }// if(component instanceof CustomComponentServiceFactory)

                  /*
                   * check the cardinality if it is 1..n it should be
                   * disabled here it can't work if no service
                   * references are available
                   */
                  if (component.getAllBoundedReferences().size() == 0
                      && cardinality.equals(CARDINAL_1_N)) {
                    ComponentActivator.debug("disable " + componentDeclaration.getComponentName() + " unable to bind new equal references to component");
                    disableComponent(component, //BA -componentDeclaration.getComponentName(),
                                     componentDeclaration.getDeclaringBundle(),
                                     false);
                    j--;
                  }// end if(newReferences==null && cardinality.equals("1..n"))
                }
              } else if (policy.equals(DYNAMIC_POLICY)
                         && component.getComponentDeclaration()
                                     .getDeclaringBundle()
                                     .getState() != Bundle.STOPPING) {
                ComponentActivator.debug(component.getComponentDeclaration() + "'s bundle is stopping this is probably an internal reference");
              }
            }//end if(referenceInfo.getInterfaceType().equals(objectClass) && event.getType()==ServiceEvent.UNREGISTERING)
          }// end while (referenceIterator.hasNext())
        }// end for(int j=0
      }// end for(int i=0;i<objectClasses.length;i++)

      if (event.getType() == ServiceEvent.UNREGISTERING) {
        /*
         * check all component instances wich haven't been
         * requested yet this is problematic because an
         * instance isn't done by them yet.
         */
        for (int x = 0; x < activeComponents.size(); x++) {
          DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(x);

          if (!isSatisfied(component.getComponentDeclaration())
              && component.getComponentDeclaration().getDeclaringBundle().getState() != Bundle.STOPPING) {
            ComponentActivator.debug("found an unrequested service component wich is unsatisfied:" + component.getComponentDeclaration().getComponentName());
            disableComponent(component, //BA -.getComponentDeclaration().getComponentName(),
                             component.getComponentDeclaration().getDeclaringBundle(),
                             false);
          }
        }
      }
    } catch (Exception e) {
      ComponentActivator.error("error in ComponentRuntimeImpl.serviceChanged(..)", e);
    } // end try
  }



  /**
   * interface for wrapping components and its internal configuration this
   * interface makes it easier to generalize the fact that components behave
   * diffrent depending on what kind of component the attached XML file
   * declares.
   *
   * @author Magnus Klack
   */
  private interface DeclarativeComponent {
    /** gets the service registration */
    public ServiceRegistration getServiceRegistration();

    /** sets the service registration */
    public void setServiceRegistration(ServiceRegistration registration);

    /** returns the component declaration for this component */
    public ComponentDeclaration getComponentDeclaration();

    /** set the servicereference */
    public void setServiceReference(ServiceReference reference);

    /** get the service reference for a specifice component */
    public ServiceReference getServiceReference();

    /** use this for Component Service Factories */
    public List getComponentContexts();

    /** used for delayed and immediate components */
    public ComponentContext getComponentContext();

    /** binds a new reference to this component */
    public void bindReference(ServiceReference reference);

    /** check if a component is bounded to a reference */
    public boolean isBoundedTo(ServiceReference reference);

    /** gets all bounded reference */
    public List getAllBoundedReferences();

    /** unbinds a reference to this component */
    public void unBindReference(ServiceReference reference);
  }

  /**
   * this class will be created when a custom delayed service is declared the
   * first component instance will be created when a request for its service
   * is requested.
   *
   * @author Magnus Klack
   */
  // MO: added overridenProps
  private class CustomDelayedService implements ServiceFactory,
                                                // ManagedService,
                                                DeclarativeComponent {
    private String[] interfaceNames;
    private ComponentContext componentContext;
    private ComponentDeclaration componentDeclaration;
    private ServiceReference serviceReference;
    private ServiceRegistration serviceRegistration;
    private List boundedReferences = new ArrayList();
    private Dictionary properties;

    public CustomDelayedService(String[] serviceInterfaces,
                                ComponentDeclaration declaration) { 
      this(serviceInterfaces, declaration, null);
    }

    public CustomDelayedService(String[] serviceInterfaces,
                                ComponentDeclaration declaration,
				Dictionary overriddenProps) { 
      
      interfaceNames = serviceInterfaces;
      componentDeclaration = declaration;
      Dictionary props = componentDeclaration.getDeclaredProperties();

      if (overriddenProps != null) {
	for (Enumeration e = overriddenProps.keys(); e.hasMoreElements();) {
	  Object key = e.nextElement();
	  props.put(key, overriddenProps.get(key));
	}
      }
      
      properties = props;
    }

    public synchronized Object getService(Bundle bundle, ServiceRegistration registration) {
      ComponentActivator.debug("getService is called in CustomDelayedService");

      if (componentContext != null) {
        /* just return the instance here */
	return componentContext.getComponentInstance().getInstance();
      }

      if (!isSatisfied(componentDeclaration)) {
        ComponentActivator.error("The component is no longer satisfied");
        return null;
      }

      try {
        componentCounter++;

        properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName());
        properties.put(ComponentConstants.COMPONENT_ID, new Long(componentCounter));

	/* MO: where is it stated that the component properties 
	   should have ComponentConstants.SERVICE_COMPONENT set?
	*/ 
        if (componentDeclaration.getXmlFile()!=null) {
          properties.put(ComponentConstants.SERVICE_COMPONENT, componentDeclaration.getXmlFile());
        } else {
          ComponentActivator.error("WARNING the property SERVICE_COMPONENT is missing for delayed component:\n"+
				   componentDeclaration.getComponentName() +
				   "\nthis may happen when a component is created by a component factory");
        }

        // create a context request will come from the same bundle declaring the the component
        ComponentActivator.debug("the componentDeclaration is:" + componentDeclaration);
        ComponentActivator.debug("the componentCounter is:" + componentCounter);
        ComponentActivator.debug("the serviceRegistration is:" + serviceRegistration);
        ComponentActivator.debug("the properties is:" + properties);
        ComponentActivator.debug("the declaringBundle is:" + componentDeclaration.getDeclaringBundle());

        componentContext = createComponentContext(componentDeclaration,
                                                  componentCounter,
                                                  serviceRegistration,
                                                  properties,
                                                  null,
                                                  componentDeclaration.getDeclaringBundle());
      } catch (Exception e) {
        ComponentActivator.error("error when creating ComponentContext in CustomDelayedService.getService", e);
      }

      try {
        invoke(this, componentContext.getComponentInstance(), true);
      } catch (Exception e) {
        ComponentActivator.error("error when binding references in CustomDelayedService.getService", e);
      }

      try {
        activateContext(componentContext);
        return componentContext.getComponentInstance().getInstance();
      } catch (Exception e) {
        ComponentActivator.error("error when activating instance in CustomDelayedService.getService", e);
      }

      return null;
    }

    /**
     * The ungetService does nothing. 
     * MO: but shouldn't it? Shouldn't it remove the service if it isn't used longer? Or is done somewhere else?
     */

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
      ComponentActivator.debug("ungetService is called in CustomDelayedService");
    }

    /**
     * this method sets the service reference used by this component
     * @param reference the service reference
     */
    public void setServiceReference(ServiceReference reference) {
      serviceReference = reference;
    }

    /**
     * this method returns the service reference registered in the framework
     * @return SericeReference the serviceReference used by this object
     */
    public ServiceReference getServiceReference() {
      return serviceReference;
    }

    /**
     * sets the service registration of the component
     * @param registration the service registration
     */
    public void setServiceRegistration(ServiceRegistration registration) {
      serviceRegistration = registration;
    }

    /**
     * @return the service registration of this object
     */
    public ServiceRegistration getServiceRegistration() {
      return serviceRegistration;
    }

    /**
     * this method returns the component declaration for this service
     * @return ComponentDeclaration an instance of the component declaration
     */
    public ComponentDeclaration getComponentDeclaration() {
      return componentDeclaration;
    }

    /**
     * this returns the component context for the component
     * @return ComponentContext the context of this component
     */
    public ComponentContext getComponentContext() {
      return componentContext;
    }

    public List getComponentContexts() {
      ArrayList list = new ArrayList();
      list.add(componentContext);
      return list; // BA: was null
    }

    /**
     * binds a reference/dependency to this component
     */
    public void binReference(ServiceReference reference) {
      boundedReferences.add(reference);
    }

    /**
     * checks if this component is bounded to a specific reference
     * @return boolean true if the component is bounded to the reference
     *         otherwise false
     */
    public boolean isBoundedTo(ServiceReference reference) {
      return boundedReferences.contains(reference);
    }

    /**
     * returns all bounded references
     * @return List with all boundend references
     */
    public List getAllBoundedReferences() {
      return boundedReferences;
    }

    /**
     * binds a reference to this component
     * @param ServiceReference the reference to be bounded
     */
    public void bindReference(ServiceReference reference) {
      boundedReferences.add(reference);
    }

    /**
     * unbinds a reference from this component
     */
    public void unBindReference(ServiceReference reference) {
      boundedReferences.remove(reference);
    }
    

    /**
     * returns the dictionary for this component
     */
    public Dictionary getProperties() {
      return properties;
    }

    /**
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     *     
    public void updated(Dictionary dict) throws ConfigurationException {
      //ComponentActivator.debug("Update is called in delayed component");
    }
    */
  }

  /**
   * This class represents an immediat component it doesn't have any services
   * is normally Activated at once, but doesn't have to be. Anyways this
   * instance
   *
   * @author Magnus Klack
   */
  private class ImmediateComponent implements DeclarativeComponent {

    private ComponentDeclaration componentDeclaration;
    private ComponentContext componentContext;
    private ServiceReference serviceReference;
    private List boundedReferences = new ArrayList();
    private Dictionary properties;

    public ImmediateComponent(ComponentDeclaration declaration) {
      this(declaration, null);
    }
    
    public ImmediateComponent(ComponentDeclaration declaration, 
			      Dictionary overriddenProps) {
      componentDeclaration = declaration;
      properties = declaration.getDeclaredProperties();
      
      if (overriddenProps != null) {
	for (Enumeration e = overriddenProps.keys(); e.hasMoreElements();) {
	  Object key = e.nextElement();
	  properties.put(key, overriddenProps.get(key));
	}
      }

      properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName());
      properties.put(ComponentConstants.COMPONENT_ID, new Long(componentCounter));
      // MO: where does it say that this should be included? 
      properties.put(ComponentConstants.SERVICE_COMPONENT, componentDeclaration.getXmlFile());
    }

    /**
     * activates the component
     */
    public void activate() {
      try {
        componentCounter++;
        // create the context in this case a request will come frome the bundle declaring the component
        componentContext = createComponentContext(componentDeclaration,
                                                  componentCounter,
                                                  null,
                                                  properties,
                                                  null,
                                                  componentDeclaration.getDeclaringBundle());

      } catch (ComponentException e) {
        ComponentActivator.error("error when creating ComponentContext in ImmediatComponent", e);
      } 

      /*
       * this means that a component is not bounded to all
       * references or that the component doesn't have any
       * references if the bounded references equals the declared
       * references no activation should be done because it has
       * already been done by the serviceChanged() method.
       */
      if (boundedReferences.size() != componentDeclaration.getReferenceInfo().size()
          || componentDeclaration.getReferenceInfo().size() == 0) {

        try {
          invoke(this, componentContext.getComponentInstance(), true);
        } catch (ComponentException e) {
          ComponentActivator.error("error when binding references in ImmediateComponent", e);
        }

        try {
          activateContext(componentContext);
        } catch (ComponentException e) {
          ComponentActivator.error("error when activate the component instance in ImmediateComponent", e);
        }
      }
    }

    public ComponentDeclaration getComponentDeclaration() {
      return componentDeclaration;
    }

    public ComponentContext getComponentContext() {
      return componentContext;
    }

    public List getComponentContexts() {
      ArrayList list = new ArrayList();
      list.add(componentContext);
      return list; // BA: was null
    }

    /**
     * not used here
     */
    public ServiceRegistration getServiceRegistration() {
      return null;
    }

    /**
     * not used here
     */
    public void setServiceRegistration(ServiceRegistration registration) {
    }

    /**
     * not used here
     */
    public void setServiceReference(ServiceReference reference) {
    }

    /**
     * not used here
     */
    public ServiceReference getServiceReference() {
      return null;
    }

    /**
     * checks if this component is bounded to a specific reference
     *
     * @return boolean true if the component is bounded to the reference
     *         otherwise false
     */
    public boolean isBoundedTo(ServiceReference reference) {
      return boundedReferences.contains(reference);
    }

    /**
     * returns all bounded references
     *
     * @return List with all boundend references
     */
    public List getAllBoundedReferences() {
      return boundedReferences;
    }

    /**
     * binds a reference to this component
     *
     * @param ServiceReference
     *            the reference to be bounded
     */
    public void bindReference(ServiceReference reference) {
      boundedReferences.add(reference);
    }

    /**
     * unbinds a reference from this component
     */
    public void unBindReference(ServiceReference reference) {
      boundedReferences.remove(reference);
    }
  }

  /**
   * This class is used by the SCR to register servicefactories in advance for
   * any component declaration declaring the attribute servicefactory. This
   * class should not be instaniated if the declaration isn't enable and
   * satisfied.
   *
   * @author Magnus Klack
   */
  private class CustomComponentServiceFactory implements ServiceFactory,
                                                         DeclarativeComponent {
    private ComponentDeclaration componentDeclaration;
    private String[] interfaceNames;
    private Hashtable configurations = new Hashtable();
    private long componentID;
    private ServiceReference serviceReference;
    private List componentContexts = new ArrayList();
    private ServiceRegistration serviceRegistration;
    private List boundedReferences = new ArrayList();

    /**
     * Constructor for the custom service factory used by the
     * registerCustomServiceFactory method
     *
     * @param serviceInterface the string representation of the interface
     * @param declaration      the componentDeclaration
     */
    public CustomComponentServiceFactory(String[] serviceInterfaces,
                                         ComponentDeclaration declaration) {
      componentCounter++;
      componentID = componentCounter;
      componentDeclaration = declaration;
      interfaceNames = serviceInterfaces;
    }

    /**
     * This will be called when a new service is required from an external
     * bundle
     *
     * @param bundle       the requesting bundle
     * @param registration the serviceRegistration
     */
    public synchronized Object getService(Bundle bundle, ServiceRegistration registration) {
      ComponentActivator.debug("getService() is called in CustomComponentServiceFactory");

      ComponentContext componentContext = (ComponentContext) configurations.get(bundle);
      if (componentContext == null) {
        try {

          Dictionary properties = componentDeclaration.getDeclaredProperties();
          properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName());
          properties.put(ComponentConstants.COMPONENT_ID, new Long(componentCounter));
          properties.put(ComponentConstants.SERVICE_COMPONENT, componentDeclaration.getXmlFile());

          componentContext = createComponentContext(componentDeclaration,
                                                    componentID,
                                                    serviceRegistration,
                                                    properties,
                                                    bundle,
                                                    componentDeclaration.getDeclaringBundle());
        } catch (ComponentException e) {
          ComponentActivator.error("error when creating component context in CustomComponentServiceFactory", e);
          return null;
        } 

        try {
          invoke(this, componentContext.getComponentInstance(), true);
        } catch (ComponentException e) {
          ComponentActivator.error("error when binding references in CustomComponentServiceFactory", e);
        }

        try {
          activateContext(componentContext);
          componentContexts.add(componentContext);
        } catch (ComponentException e) {
          ComponentActivator.error("error when activating instance in CustomComponentServiceFactory", e);
        }

        configurations.put(bundle, componentContext);
      }
      
      return componentContext.getComponentInstance().getInstance();
    }

    /**
     * This method will be called when a bundle no longer needs a service
     * produced by this factory class
     *
     * @param bundle       the bundle requesting this method
     * @param registration the service registration
     * @param service      the service object
     */
    public synchronized void ungetService(Bundle bundle,
                                          ServiceRegistration registration,
                                          Object service) {
      ComponentActivator.debug("ungetService() is called in CustomServiceFactory");
      configurations.remove(bundle);
    }

    /**
     * sets the service reference to this configuration
     *
     * @param reference the service reference
     */
    public void setServiceReference(ServiceReference reference) {
      serviceReference = reference;
    }

    /**
     * returns the service reference used by this component configuration
     *
     * @return ServiceReference the service reference used by this instance
     */
    public ServiceReference getServiceReference() {
      return serviceReference;
    }

    /**
     * sets the service registration of this object
     *
     * @param registration the service registration
     */
    public void setServiceRegistration(ServiceRegistration registration) {
      serviceRegistration = registration;
    }

    /**
     * returns the service registration used by this object
     *
     * @return ServiceRegistration the service registration this bundle uses
     */
    public ServiceRegistration getServiceRegistration() {
      return serviceRegistration;
    }

    /**
     * this method returns the component declaration for the service
     *
     * @return the component declaration
     */
    public ComponentDeclaration getComponentDeclaration() {
      return componentDeclaration;
    }

    /**
     * this method returns all created componentContexts created by this
     * service factory
     *
     * @return List containing all componentContexts
     */
    public List getComponentContexts() {
      return componentContexts;
    }

    /**
     * not used here
     */
    public ComponentContext getComponentContext() {
      return null;
    }

    /**
     * binds a reference/dependency to this component
     */
    public void binReference(ServiceReference reference) {
      boundedReferences.add(reference);
    }

    /**
     * checks if this component is bounded to a specific reference
     *
     * @return boolean true if the component is bounded to the reference
     *         otherwise false
     */
    public boolean isBoundedTo(ServiceReference reference) {
      return boundedReferences.contains(reference);
    }

    /**
     * returns all bounded references
     *
     * @return List with all boundend references
     */
    public List getAllBoundedReferences() {
      return boundedReferences;
    }

    /**
     * binds a reference to this component
     *
     * @param ServiceReference
     *            the reference to be bounded
     */
    public void bindReference(ServiceReference reference) {
      boundedReferences.add(reference);
    }

    /**
     * unbinds a reference from this component
     */
    public void unBindReference(ServiceReference reference) {
      boundedReferences.remove(reference);
    }
  }

  /**
   * This class creates new components when newInstance is called from an
   * external source. It will also register services in advance for the
   * declared component
   *
   * @author Magnus Klack
   */
  private class CustomComponentFactory implements ComponentFactory,
                                                  DeclarativeComponent {
    private ComponentDeclaration componentDeclaration;
    private ServiceReference serviceReference;
    private ServiceRegistration serviceRegistration;
    private List boundedReferences = new ArrayList();

    /**
     * constructor for CustomComponentFactory
     *
     * @param declaration the declaration
     */
    public CustomComponentFactory(ComponentDeclaration declaration) {
      componentDeclaration = declaration;
      if (componentDeclaration.getXmlFile() == null) {
        ComponentActivator.error("XML file is null in component declaration");
      }
    }

    /**
     * this method will create a new Component of the type which this
     * instance's component declaration declares. It can be an delayed
     * component or an immediate component
     *
     * @param Dictionary the properties for the new component
     */

    public ComponentInstance newInstance(Dictionary properties) throws ComponentException {
      ComponentActivator.debug("newInstance() is called in CustomComponentFactory");

      if (!isSatisfied(componentDeclaration)) {
        ComponentActivator.error("component factory is not satisfied");
        throw new ComponentException("Component factory is not satisfied");
      }

      componentCounter++;

      try {
        if (componentDeclaration.getServiceInfo().size() > 0) {

          // get all services
          Iterator iteratorServices = componentDeclaration.getServiceInfo().iterator();
          ArrayList vectorInterfaces = new ArrayList();
          while (iteratorServices.hasNext()) {
            ComponentServiceInfo serviceInfo = (ComponentServiceInfo) iteratorServices.next();
            ArrayList interfaces = serviceInfo.getComponentInterfaces();
            vectorInterfaces.addAll(interfaces);
          }
          String[] allInterfaces = (String[]) vectorInterfaces.toArray(new String[vectorInterfaces.size()]);

          ComponentActivator.debug("register component factory service "
                                   + "for new component instance with name"
                                   + componentDeclaration.getComponentName()
                                   + " the service is:" + allInterfaces);

          ComponentDeclaration newDeclaration = (ComponentDeclaration) componentDeclaration.getClone();

          newDeclaration.setFactory(null); // this is not a factory

          CustomDelayedService delayedService = registerDelayedComponent(allInterfaces, 
									 newDeclaration, 
									 properties);
          // Activate by getting the service:
          Object obj = delayedService.getService(componentDeclaration.getDeclaringBundle(),
                                                 delayedService.getServiceRegistration());
          delayedService.ungetService(componentDeclaration.getDeclaringBundle(),
                                      delayedService.getServiceRegistration(),
                                      obj);
          activeComponents.add(delayedService);
          return delayedService.getComponentContext().getComponentInstance();

        } else {
	  ImmediateComponent immediateComponent = new ImmediateComponent(componentDeclaration, properties);
          activeComponents.add(immediateComponent);
          return immediateComponent.getComponentContext().getComponentInstance();
        }

      } catch (ComponentException e) {
        ComponentActivator.error("error when activating instance in CustomComponentFactory", e);
      } catch (Exception e) {
        ComponentActivator.error("error when register service in component factory", e);
        throw new ComponentException("error when register service in component factory:", e.getCause());
      }

      return null;
    }

    /**
     * @param reference
     */
    public void setServiceReference(ServiceReference reference) {
      serviceReference = reference;
    }

    /**
     * @return the service reference of this component factory
     */
    public ServiceReference getServiceReference() {
      return serviceReference;
    }

    /**
     * Sets the service registration for this component factory
     *
     * @param registration
     */
    public void setServiceRegistration(ServiceRegistration registration) {
      serviceRegistration = registration;
    }

    /**
     * return the service registration
     *
     * @return ServiceRegistration used by this object
     */
    public ServiceRegistration getServiceRegistration() {
      return serviceRegistration;
    }

    /**
     * this return the component delcaration
     *
     * @return ComponentDeclaration
     */
    public ComponentDeclaration getComponentDeclaration() {
      return componentDeclaration;
    }

    /**
     * not used here
     */
    public List getComponentContexts() {
      return null;
    }

    /**
     * not used here
     */
    public ComponentContext getComponentContext() {
      return null;
    }

    /**
     * binds a reference/dependency to this component
     */
    public void binReference(ServiceReference reference) {
      boundedReferences.add(reference);
    }

    /**
     * checks if this component is bounded to a specific reference
     *
     * @return boolean true if the component is bounded to the reference
     *         otherwise false
     */
    public boolean isBoundedTo(ServiceReference reference) {
      return boundedReferences.contains(reference);
    }

    /**
     * returns all bounded references
     *
     * @return List with all boundend references
     */
    public List getAllBoundedReferences() {
      return boundedReferences;
    }

    /**
     * binds a reference to this component
     *
     * @param ServiceReference the reference to be bounded
     */
    public void bindReference(ServiceReference reference) {
      boundedReferences.add(reference);
    }

    /**
     * unbinds a reference from this component
     */
    public void unBindReference(ServiceReference reference) {
      boundedReferences.remove(reference);
    }
  }

  /*
    This class awaits configuration event. Whenever it receives 
    one of a currently registered component i will attempt to 
    restart it. Confirming to 112.7 (r4-cmpn).

    One could probably implement this as a component.
   */

  private class ComponentConfigurationListener 
    implements ConfigurationListener {
    
    public void configurationEvent(ConfigurationEvent event) {
      
      for (int i = 0; i < activeComponents.size(); i++) {
	DeclarativeComponent component = (DeclarativeComponent) activeComponents.get(i);
	ComponentDeclaration decl = component.getComponentDeclaration();
	
	if (decl.getComponentName().equals(event.getPid())) {
	  disableComponent(component, 
			   decl.getDeclaringBundle(), 
			   false);

	  //MO: shouldn't this be like this? With it does not pass the tests.
	  // if (event.getType() != ConfigurationEvent.CM_DELETED) {
	  evaluate(component.getComponentDeclaration(), false);
	    //}

	}
      }
    }
  }
}
 
