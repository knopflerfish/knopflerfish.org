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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

/**
 * @author Martin Berg
 * 
 * This class is used to store information of a specific component It has four
 * helper classes that supply additional information these are:
 * PropertiesInformation PropertyInformation ServiceInformation
 * ReferenceInformation
 */
public class ComponentContextImpl implements ComponentContext{
	/* the bundle context */
	private BundleContext bundleContext;
	/* the component instance */
	private ComponentInstance componentInstance;
	
	
	/* The constructor */
	public ComponentContextImpl(BundleContext context) {
		/* assign the bundle context */
		bundleContext = context;

	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#getProperties()
	 */
	public Dictionary getProperties() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#enableComponent(java.lang.String)
	 */
	public void enableComponent(String name) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#disableComponent(java.lang.String)
	 */
	public void disableComponent(String name) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#getServiceReference()
	 */
	public ServiceReference getServiceReference() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#getComponentInstance()
	 */
	public ComponentInstance getComponentInstance() {
		return componentInstance;

	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#locateService(java.lang.String)
	 */
	public Object locateService(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#locateServices(java.lang.String)
	 */
	public Object[] locateServices(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#getBundleContext()
	 */
	public BundleContext getBundleContext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentContext#getUsingBundle()
	 */
	public Bundle getUsingBundle() {
		// TODO Auto-generated method stub
		return null;
	}


}