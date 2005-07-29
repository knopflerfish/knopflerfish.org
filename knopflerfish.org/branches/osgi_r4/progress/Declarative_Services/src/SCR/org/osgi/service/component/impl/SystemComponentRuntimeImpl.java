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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

import com.sun.jndi.toolkit.ctx.ComponentContext;

/**
 * This class is the implementation of the declarative service feature. It will
 * locate and bind diffrent types of declared components on demand. It will also
 * listen to BundleEvents rasied within the framework and
 */
public class SystemComponentRuntimeImpl implements BundleListener {
	/* variable holding the bundlecontext */
	private BundleContext bundleContext;

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
	 * 
	 * @throws IOException
	 */
	public synchronized void bundleChanged(BundleEvent event) {

		customParser = new CustomParser();
		ComponentDeclaration compDec = customParser.readXML(event);
	}
	
	
	
	
	/**
	 * this method creates a ComponentContext 
	 * 
	 * @param componentDeclaration the ComponentDeclaration object
	 * @return ComponentContext Object if succeed
	 */
	private ComponentContext createComponentContext(ComponentDeclaration componentDeclaration){
		return null;
	}
	
	/**
	 * this function will use a ComponentDeclaration to create a new ComponentInstance object
	 * 
	 * @param componentDeclaration the component declaration
	 * @return ComponentInstance object
	 */
	private ComponentInstance createComponentInstance(ComponentDeclaration componentDeclaration){
		return null;
	}
	
	
	/**
	 * this method will create a new component factory and return the instance of it
	 * 
	 * @param componentDeclaration the component declaration
	 * @return the componentFactory object if succeed null else
	 */
	private ComponentFactory createComponentFactory(ComponentDeclaration componentDeclaration){
		return null;
	}
	
	
	
	/**
	 * this method will track the dependencies and give them to the declarative
	 * service which this class controlls. If the component doesn't declare
	 * a bind method the method will try to call a pressumed activate() method
	 * with the configurator as context. Dependencies are kept in the class
	 * ComponentReferenceInfo. 
	 * 
	 * @author magnus klack
	 */
	private void trackDependencies(ComponentDeclaration componentDeclaration,ComponentInstance componentInstance) {
		ArrayList referenceInfo = componentDeclaration.getReferenceInfo(); 
		
		/* create an iterator */
		Iterator it = referenceInfo.iterator();

		/* iterate while still has next */
		while (it.hasNext()) {
			
			/* create the a temporary object */
			ComponentReferenceInfo componentRef = (ComponentReferenceInfo) it
					.next();

			if (componentRef.getInterfaceType() != null) {
				/* get the interface */
				String interfaceName = componentRef.getInterfaceType();
				/* get the reference */
				String targetFilter = componentRef.getTarget();

				/* assign a component class object */
				Object componentObject = componentInstance.getInstance();

				/*
				 * create a string representing the method which should be
				 * called
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
				
				/* check if methodName equals activate 
				 * if so it is an locate delcared component
				 */
				if (methodName.equals("activate")) {
					try{
						/* get the argument classes */
						Class partypes[] = new Class[1];

						/* get the argument classes */
						partypes[0] = this.getClass();

						/* get the method */
						Method method = componentObject.getClass().getMethod(
								methodName, partypes);

						/* create the arglist */
						Object arglist[] = new Object[1];

						/* add the argument variable to the arglist */
						arglist[0] = this;

						/* invoke method att the component class */
						method.invoke(componentObject, arglist);

					} catch (NoSuchMethodException e) {
						System.err
								.println("Error calling \"activate() method\" No Method Found:"
										+ e);

					} catch (IllegalAccessException e) {
						System.err.println("Error invoking Class:"
								+ componentObject + " Class Cannot be Accessed:"
								+ e);

					} catch (InvocationTargetException e) {
						System.err.println("Error Invoking Target in:"
								+ componentObject + ":" + e);

					}

				} else {

					try {
						/* get the bundle context */
						ServiceReference serviceReference = bundleContext
								.getServiceReference(interfaceName);

						/* get the service */
						Object argument = bundleContext
								.getService(serviceReference);

						/* create an array to store the arguments in */
						Class partypes[] = new Class[1];

						/* get the argument classes */
						partypes[0] = Class.forName(interfaceName);

						/* get the method */
						Method method = componentObject.getClass().getMethod(
								methodName, partypes);

						/* create the arglist */
						Object arglist[] = new Object[1];

						/* add the argument variable to the arglist */
						arglist[0] = argument;

						/* invoke method att the component class */
						method.invoke(componentObject, arglist);

					} catch (ClassNotFoundException e) {
						System.err
								.println("Error when trying to locate the class interfaceClass:"
										+ interfaceName);

					} catch (NoSuchMethodException e) {
						System.err.println("Error calling \"" + methodName
								+ " method\" No Method Found:" + e);

					} catch (IllegalAccessException e) {
						System.err.println("Error invoking Class:"
								+ componentObject + " Class Cannot be Accessed:"
								+ e);

					} catch (InvocationTargetException e) {
						System.err.println("Error Invoking Target in:"
								+ componentObject + ":" + e);
					

					}
				
				}// end if (methodName.equals("activate")) 

			}else{
				System.err.println("dependency interface not declared");
			
			}//if (componentRef.getInterfaceType() != null)

		}//end while

	}

	
	
	

}
