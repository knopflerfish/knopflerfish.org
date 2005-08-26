/*
 * @(#)ComponentContextImpl.java        1.0 2005/07/28
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
 * @author Magnus Klack
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
	private SystemComponentRuntimeImpl systemComponentRuntime;
	
	/** variable holding the bundle using this context */
	private Bundle usingBundle;
	
	/** 
	 *  variable holding the requesting bundle the same as using bundle
	 *  but this variable is never null 
	 */
	private Bundle requestBundle;
	
	/* The constructor */
	public ComponentContextImpl(ComponentInstance component,BundleContext context,Dictionary props,
			ServiceRegistration registration,SystemComponentRuntimeImpl scr,
			Bundle useBundle,Bundle reqBundle) {
		
		/* assign the bundle context */
		bundleContext = context;
		/* assign the properties */
		properties = props;
		/* assign the component instance */
		componentInstance =component;
		/* assign the service reference */
		serviceRegistration = registration;
		/* assign the SCR */
		systemComponentRuntime = scr;
		/* assign the using bundle */
		usingBundle = useBundle;
		/* assign the declaring bundle */
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
		try{
			
			/* create a process */
			EnableProcess enableProcess = new EnableProcess(name);
			/* start the process */
			enableProcess.start();
			/* tell the SCR to enable component with the given name */
			//systemComponentRuntime.enableComponent(name,requestBundle);
			
		}catch(ComponentException e){
			System.err.println(e);
		}
		
	}

	/**
	 * This is used by a component to disable another component in the same bundle
	 * or the same component.
	 * 
	 * @param name the name of the component
	 */
	public void disableComponent(String name) {
		
		try{
			/* create new process which will stop the component */
			StopProcess stopper = new StopProcess(name);
			/* run the process */
			stopper.run();
		}catch(ComponentException e){
			/* print the error */
			System.err.println("error then stopping component " + name
					+" due to:" + e);
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
		
		if(serviceName!=null){
			try{
				return systemComponentRuntime.locateService(name,serviceName);
			
			}catch(ComponentException e){
				System.err.println(e);
				return null;
			}	
			
		}else{
			System.err.println("no component.name specified in the context");
			return null;
		}
		
	}

	/**
	 * This is used 
	 */
	public Object[] locateServices(String name) {
		String serviceName =(String) properties.get(ComponentConstants.COMPONENT_NAME);
		
		if(serviceName!=null){
			try{
				return systemComponentRuntime.locateServices(name,serviceName);
			
			}catch(ComponentException e){
				System.err.println(e);
				return null;
			}	
			
		}else{
			System.err.println("no component.name specified in the context");
			return null;
		}
	}

	/**
	 * 
	 */
	public BundleContext getBundleContext() {
		return requestBundle.getBundleContext();
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
	private class EnableProcess extends Thread{
		private String componentName;
		/**
		 * @param name
		 */
		public EnableProcess(String name) {
			componentName=name;
		}
		
		public void run(){
			/* tell the SCR to enable component with the given name */
			systemComponentRuntime.enableComponent(componentName,requestBundle);
		
		}
		
	}
	/**
	 * This class will stop a specific component
	 * 
	 * @author Magnus Klack
	 */
	private class StopProcess extends Thread{
		/** the component to be stopped */
		private String componentName;
		
		public StopProcess(String name){
			componentName=name;
		}
		
		public void run(){
			try{
				/* tell the SCR to disable the component */
			    systemComponentRuntime.disableComponent(componentName,requestBundle,false);
			}catch(Exception e){
				throw new ComponentException(e.getMessage(),e.getCause());
			}
		}
		
	}

}