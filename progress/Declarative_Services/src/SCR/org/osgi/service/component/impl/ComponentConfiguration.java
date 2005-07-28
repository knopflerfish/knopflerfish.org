/*
 * @(#)ComponentController.java        1.0 2005/07/28
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
 * This class is used to store information of a specific component
 * It has four helper classes that supply additional information
 * these are:
 * PropertiesInformation
 * PropertyInformation
 * ServiceInformation
 * ReferenceInformation
 */
public class ComponentController {
	/* The name of the component */
	private String componentName;
	/* to use autoenable on the component or not */
	private boolean autoEnable;
	/* to use a factory with the component or not */
	private boolean factory;
	/*
	 * the class that implements the interfaces that is listed in
	 * serviceInfo
	 */
	private String implementation;
	
	/* A list containing all properties for the component */
	private ArrayList propertiesInfo;
	/* A list containing all property for the component */
	private ArrayList propertyInfo;
	/* A list containing all services for the component */
	private ArrayList serviceInfo;
	/* A list containing all references for the component */
	private ArrayList referenceInfo;
	
	/* The constructor */
	ComponentController(){
		propertiesInfo = new ArrayList();
		propertyInfo = new ArrayList();
		serviceInfo = new ArrayList();
		referenceInfo = new ArrayList();			
	}
		
	/**
	 * @return Returns the autoEnable.
	 */
	public boolean isAutoEnable() {
		return autoEnable;
	}
	/**
	 * @param autoEnable
	 *            The autoEnable to set.
	 */
	public void setAutoEnable(boolean autoEnable) {
		this.autoEnable = autoEnable;
	}
	/**
	 * @return Returns the componentName.
	 */
	public String getComponentName() {
		return componentName;
	}
	/**
	 * @param componentName
	 *            The componentName to set.
	 */
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}
	/**
	 * @return Returns the factory.
	 */
	public boolean isFactory() {
		return factory;
	}
	/**
	 * @param factory
	 *            The factory to set.
	 */
	public void setFactory(boolean factory) {
		this.factory = factory;
	}
	/**
	 * @return Returns the implementation.
	 */
	public String getImplementation() {
		return implementation;
	}
	/**
	 * @param implementation
	 *            The implementation to set.
	 */
	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}
	/**
	 * @return Returns the propertiesInfo.
	 */
	public ArrayList getPropertiesInfo() {
		return propertiesInfo;
	}
	/**
	 * @param propertiesInfo
	 *            The propertiesInfo to set.
	 */
	public void setPropertiesInfo(ArrayList propertiesInfo) {
		this.propertiesInfo = propertiesInfo;
	}
	/**
	 * @return Returns the propertyInfo.
	 */
	public ArrayList getPropertyInfo() {
		return propertyInfo;
	}
	/**
	 * @param propertyInfo
	 *            The propertyInfo to set.
	 */
	public void setPropertyInfo(ArrayList propertyInfo) {
		this.propertyInfo = propertyInfo;
	}
	/**
	 * @return Returns the referenceInfo.
	 */
	public ArrayList getReferenceInfo() {
		return referenceInfo;
	}
	/**
	 * @param referenceInfo
	 *            The referenceInfo to set.
	 */
	public void setReferenceInfo(ArrayList referenceInfo) {
		this.referenceInfo = referenceInfo;
	}
	/**
	 * @return Returns the serviceInfo.
	 */
	public ArrayList getServiceInfo() {
		return serviceInfo;
	}
	/**
	 * @param serviceInfo
	 *            The serviceInfo to set.
	 */
	public void setServiceInfo(ArrayList serviceInfo) {
		this.serviceInfo = serviceInfo;
	}
	
	/**
	 * @return Returns an instance of PropertiesInformation.
	 */
	public ComponentPropertiesInfo getNewPropertiesIntance(){ 
		ComponentPropertiesInfo props = new ComponentPropertiesInfo();
		return props;
	}
	
	/**
	 * @return Returns an instance of PropertyInformation.
	 */
	public ComponentPropertyInfo getNewPropertyIntance(){ 
		ComponentPropertyInfo prop = new ComponentPropertyInfo();
		return prop;
	}
	
	/**
	 * @return Returns an instance of ServiceInformation.
	 */
	public ComponentServiceInfo getNewServiceIntance(){ 
		ComponentServiceInfo serv = new ComponentServiceInfo();
		return serv;
	}
		/**
	 * @return Returns an instance of ReferenceInformation.
	 */
	public ComponentReferenceInfo getNewReferenceIntance(){ 
		ComponentReferenceInfo ref = new ComponentReferenceInfo();
		return ref;
	}
}
