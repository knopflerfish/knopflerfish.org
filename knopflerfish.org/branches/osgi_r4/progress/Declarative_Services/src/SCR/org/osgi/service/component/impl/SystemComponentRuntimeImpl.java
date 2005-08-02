/*
 * @(#)SystemComponentRuntimeImpl.java        1.0 2005/06/28
 *
 * Copyright (c) 2003-2005 Gatespace telematics AB
 * Otterhallegatan 2, 41670,Gothenburgh, Sweden.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of 
 * Gatespace telematics AB. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Gatespace telematics AB.
 */

package org.osgi.service.component.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * This class is the implementation of the declarative service feature. It will
 * locate and bind diffrent types of declared components on demand. It will also
 * listen to BundleEvents rasied within the framework and
 */
public class SystemComponentRuntimeImpl implements BundleListener {
	/** variable holding the bundlecontext */
	private BundleContext bundleContext;

	/** variable holding the custom parser object */
	private CustomParser customParser;

	public SystemComponentRuntimeImpl(BundleContext context) {
		/* assign the bundlecontext */
		bundleContext = context;
		/* add this as a bundle listener */
		bundleContext.addBundleListener(this);
	}

	/**
	 * Listen for BundleEvents from the framework and
	 * creates a ComponentDeclaration 
	 * @throws BundleException
	 * 
	 * @throws IOException
	 */
	public void bundleChanged(BundleEvent event) {
		System.out.println("******************* INCOMING EVENT *****************************");
		if(event.getBundle().getHeaders().get("Service-Component") !=null){
			
			System.out.println("******************* Parsing Started *****************************");
			customParser = new CustomParser();
			
		
			ComponentDeclaration componentDeclaration = customParser.readXML(event);
			
		
			if(componentDeclaration.isAutoEnable()){
				
				ComponentContext componentContext = createComponentContext(componentDeclaration);
				registerComponentServices(componentDeclaration,componentContext);
				this.trackReferences(componentDeclaration,componentContext.getComponentInstance());
				
			}
		
			
		}

	}

	/**
	 * this method registers services declared in the componentDeclaration
	 * 
	 * @param componentDeclaration the component declaration
	 * @param componentContext the componentContext
	 */
	private void registerComponentServices(
			ComponentDeclaration componentDeclaration,
			ComponentContext componentContext) {
	
		/* get all the service */
		ArrayList services = componentDeclaration.getServiceInfo();
		/* create an iterator to iterate through the services */
		Iterator iteratorServices = services.iterator();
		
		
		/* iterate */
		while(iteratorServices.hasNext()){
			/* create a service info */
			ComponentServiceInfo serviceInfo = (ComponentServiceInfo)iteratorServices.next();
			/* get all the interfaces */
			ArrayList interfaces = serviceInfo.getComponentInterfaces();
			/* create an iterator variable */
			Iterator iteratorInterfaces = interfaces.iterator();
			
			/* iterate throgh all the services */
			while(iteratorInterfaces.hasNext()){
				
				/* create a temporary string variable */
				String serviceInterface = (String)iteratorInterfaces.next();
				 /* create the hashtable */
		        Hashtable propsTable = new Hashtable();
		        
		        /* TODO check if more properties should be added 
		        *  here. That might require some changes
		        */
		        
		        /* add the Constant indicating the service_PID */
		        propsTable.put(Constants.SERVICE_PID,serviceInterface);
		        /* add the Constant variable and the id to the Hashtable */
		        propsTable.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName());
		        /* print that we register a service */
		        System.out.println("SCR registers service for interface:"+ serviceInterface );

		        /* register the service to the framework use the componentInstance */
		        try{       	 		        
		        	 bundleContext.registerService(serviceInterface
			        		,componentContext.getComponentInstance().getInstance(),
			                propsTable);
		        
		        }catch(Exception e){
		        	System.err.println("Error registering service:" + e);
		        }
		        
		        
			}
        
		}
		

	}

	/**
	 * this method creates a ComponentContext, it will check if the 
	 * component is autoenable if all variables will be passed to the 
	 * constuctor else only selective variables. 
	 * 
	 * @param componentDeclaration the ComponentDeclaration object
	 * @return ComponentContext Object if succeed
	 */
	private ComponentContext createComponentContext(
			ComponentDeclaration componentDeclaration) {
		try {
			
			Dictionary props = new Hashtable();
			/* TODO
			 * Create properties here pick from component declaration
			 */
			ComponentContextImpl newContext = new ComponentContextImpl(
					createComponentInstance(componentDeclaration),
					bundleContext, props, null);

			return newContext;

		} catch (Exception e) {
			System.err.println("Error in createComponentContext():" + e);

		}

		return null;
	}
 
	/**
	 * this function will use a ComponentDeclaration to create a new ComponentInstance object
	 * 
	 * @param componentDeclaration the component declaration
	 * @return ComponentInstance object
	 */
	private ComponentInstance createComponentInstance(
			ComponentDeclaration componentDeclaration) {
			
			/* get assign null to the component name */
			String componentName = null;
			/* get the name of the components implementation */
			componentName = componentDeclaration.getImplementation();
			/* create a new factory object */
			ComponentCreator creator = new ComponentCreator(componentName);
			/* create a new instance */
		    Object element = creator.newInstance();
			
		    /* return the component instance */
		    return new ComponentInstanceImpl(element);
	}

	/**
	 * this method will create a new component factory and return the instance of it
	 * 
	 * @param componentDeclaration the component declaration
	 * @return the componentFactory object if succeed null else
	 * 
	 * @author Magnus Klack
	 */
	private ComponentFactory createComponentFactory(
			ComponentDeclaration componentDeclaration) {
		return null;
	}

	/**
	 * this method will track the dependencies and give them to the declarative
	 * service which this class controlls. If the component doesn't declare
	 * a bind method the method will try to call a pressumed activate() method
	 * with the configurator as context. Dependencies are kept in the class
	 * ComponentReferenceInfo. 
	 * @param componentDeclaration the component declaration
	 * @param componentInstance the componentInstance
	 * 
	 * @author Magnus Klack
	 */
	private void trackReferences(ComponentDeclaration componentDeclaration,
			ComponentInstance componentInstance) {

		System.out
				.println("**************** Tracking references *********************");
		/* create an iterator */
		Iterator it = null;
		/* get the reference info array */
		ArrayList referenceInfo = componentDeclaration.getReferenceInfo();
		/* create the iterator */
		it = referenceInfo.iterator();

		/* iterate while still has next */
		while (it.hasNext()) {

			/* create the a temporary object */
			ComponentReferenceInfo componentRef = (ComponentReferenceInfo) it
					.next();

			if (componentRef.getInterfaceType() != null) {
				/* get the interface */
				String interfaceName = componentRef.getInterfaceType();
				/* print the service interface name */
				System.out.println("service interface name is:" + interfaceName);
				/* get the reference */
				String targetFilter = componentRef.getTarget();
				/* assign a component class object */
				Object componentObject = componentInstance.getInstance();

				/* create a string representing the method which should be
				 * called, i.e, the bind method
				 */
				String methodName = "";

				/* check if a bind method is declared */
				if (componentRef.getBind() != null) {
					/* assign the method name */
					methodName = componentRef.getBind();
				} else {
					/* else set activate as default */
					methodName = "activate";
				}
				
				System.out.println("The bind-method is:" + methodName);
				
				/*
				 * check if methodName equals activate if so it is an locate
				 * delcared component
				 */
				if (methodName.equals("activate")) {
					
					/*TODO enter the activate method here 
					 * 
					 */
					
				} else {

					try {
						
						/* get the service reference */
						ServiceReference serviceReference = bundleContext.getServiceReference(interfaceName);
						/* get the service */
						Object reference = bundleContext.getService(serviceReference);
												
						try{
							/* print that we try to invoke the method */
							System.out.println("Trying to invoke " + methodName + " in class:" + componentDeclaration.getImplementation());
							/* get the method */
							Method method =componentObject.getClass().getDeclaredMethod(methodName, 
									new Class[]{Class.forName(interfaceName)});
							
							/* set this as accessible */
							method.setAccessible(true);
							/* invoke the method */
							method.invoke(componentObject,new Object[]{reference});
							
						}catch(NoSuchMethodException e){
							System.err.println("ERROR GETTING METHOD:"  +e);
						}catch(IllegalAccessException e){
							System.err.println("ERROR GETTING METHOD:" +e);
						}catch(Exception e){
							System.err.println("ERROR GETTING METHOD:" +e);
						}
										
					} catch (NullPointerException e) {
						System.err.println("Error in trackReferences" + e);
					} catch(Exception e){
						System.err.println("Error in trackReferences" + e);
					}

				}// end if (methodName.equals("activate"))

			} else {
				System.err.println("dependency interface not declared");

			}//if (componentRef.getInterfaceType() != null)

		}//end while

	}
	
	/**
	 * This class will create new instances of a given class
	 * it will use the TracingIH class to trace and return an instance 
	 * of the class.
	 * 
	 * @author Magnus Klack
	 *
	 */
	private class ComponentCreator{
		/* a Class representing the component implementation */
		private Class componentClass;
		
		/**
		 * Constructor creates a class 
		 * @param className the class name
		 */
		public ComponentCreator(String className){
			try{
				/* create the class object */
				componentClass=Class.forName(className);
			}catch(ClassNotFoundException e){
				/* print the error */
				System.err.println("Error in ComponentCreator:"+e);
			
			}
			
		}
		
		/**
		 * Creates a new instance of the object 
		 * uses Proxy technique to trace object
		 * @return
		 */
		public Object newInstance() {
			try {
				
				/* creates the class instance */
				Object returnObject = componentClass
						.newInstance();
				
				/* create the return object with tracing feature will give the object as proxy */
				//returnObject=TracingIH.createProxy(
				//		returnObject, new PrintWriter(System.out));
				
				System.out.println("Creating instance:" + returnObject);
				/* return the object */
				return returnObject;
				
			} catch (IllegalAccessException e) {
				System.err
						.println("Error in ComponentCreator  Can't access constructor of:"
								+ componentClass + "\n" + e);
			} catch (InstantiationException e) {
				System.err
						.println("Error in ComponentCreator  Can't create new instance of:"
								+ componentClass + " \n" + e);
			} catch (Exception e) {
				System.err.println("Error in ComponentCreator:");
				e.printStackTrace();
			}
			
			return null;
		}
		
	
	}//end ComponentCreator
	

}


