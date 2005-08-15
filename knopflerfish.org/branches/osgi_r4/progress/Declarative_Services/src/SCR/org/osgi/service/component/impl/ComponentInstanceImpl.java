/*
 * @(#)ComponentInstanceImpl.java        1.0 2005/07/28
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

import org.osgi.service.component.ComponentInstance;

/**
 * class representing the componentInstance used to wrap a component object
 * into a common/known variable type
 * 
 * @author Magnus Klack
 */
public class ComponentInstanceImpl implements ComponentInstance{
	/** variable holding the component instance element */
	private Object instanceElement;
	/** variable holding the SystemComponentRuntime */
	private SystemComponentRuntimeImpl systemComponentRuntime;
	
	public ComponentInstanceImpl(Object element,SystemComponentRuntimeImpl scr){
		/* assign the component instance element */
		instanceElement = element;
		/* assign the SCR */
		systemComponentRuntime=scr;
	}
	
	/**
	 * this method will dispose the componentInstance 
	 */
	public void dispose() {
		/* tell SCR to dispose the component with this instanceElement */
		systemComponentRuntime.disposeComponent(instanceElement);	
	}

	/**
	 * returns the component instance element
	 *  
	 * @return Object the component instance element 
	 */
	public Object getInstance() {
		/* just return the element */
		return instanceElement;
	}

}
