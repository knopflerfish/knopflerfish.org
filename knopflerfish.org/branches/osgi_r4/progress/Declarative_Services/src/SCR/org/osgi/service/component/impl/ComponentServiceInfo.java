/*
 * @(#)ComponentServiceInfo.java        1.0 2005/07/28
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

import java.util.ArrayList;

/**
 * @author Martin Berg 
 * 
 * This class is a helper classes to the ComponentController and
 * that supply additional information of a component
 */
public class ComponentServiceInfo {

	/* to use a factory with the component or not */
	private boolean serviceFactory;
	/* A list containing all interfaces that the component supplys */
	private ArrayList interfaces;

	ComponentServiceInfo(){
		interfaces = new ArrayList();
	}
	/* insert a componentInterface in the list */
	public void instertInterface(String componentInterface) {
		interfaces.add(componentInterface);
	}
	/**
	 * @return Returns the serviceFactory.
	 */
	public boolean isServiceFactory() {
		return serviceFactory;
	}
	/**
	 * @return Returns the componentInterfaces.
	 */
	public ArrayList getComponentInterfaces() {
		return interfaces;
	}
	/**
	 * @param serviceFactory The serviceFactory to set.
	 */
	public void setServiceFactory(boolean serviceFactory) {
		this.serviceFactory = serviceFactory;
	}
}
