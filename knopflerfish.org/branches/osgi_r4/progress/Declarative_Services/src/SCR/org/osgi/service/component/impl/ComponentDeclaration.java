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

import org.osgi.framework.Bundle;

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
public class ComponentDeclaration {
	/** The name of the component */
	private String componentName;
	/** to use autoenable on the component or not */
	private boolean autoEnable;
	/** the factory to use */
	private String factory;
	/** to use a factory with the component or not */
	private boolean serviceFactory;
	/** variable holding the declaring bundle */	
	private Bundle declaringBundle;
	
	/**
	 * the class that implements the interfaces that is listed in
	 * serviceInfo
	 */
	private String implementation;
	
	/** A list containing all properties for the component */
	private ArrayList propertiesInfo;
	/** A list containing all property for the component */
	private ArrayList propertyInfo;
	/** A list containing all services for the component */
	private ArrayList serviceInfo;
	/** A list containing all references for the component */
	private ArrayList referenceInfo;
	/** A list containing all interfaces for the component */
	private ArrayList provideInfo;
	
	/* The constructor */
	ComponentDeclaration(){
		propertiesInfo = new ArrayList();
		propertyInfo = new ArrayList();
		serviceInfo = new ArrayList();
		referenceInfo = new ArrayList();
		provideInfo = new ArrayList();
	}
	
	
	public Object getClone()  {
		
			ComponentDeclaration clone = new ComponentDeclaration();
			clone.setAutoEnable(isAutoEnable());
			clone.setComponentName(getComponentName());
			clone.setDeclaraingBundle(getDeclaringBundle());
			clone.setFactory(getFactory());
			clone.setImplementation(getImplementation());
			clone.setPropertiesInfo(getPropertiesInfo());
			clone.setReferenceInfo(getReferenceInfo());
			clone.setServiceFactory(isServiceFactory());
			clone.setServiceInfo(getServiceInfo());
			
			return clone;
		
	}
	
	/**
	 * sets the bundle which declares the component
	 * 
	 * @param bundle the declaring bundle
	 */
	public void setDeclaraingBundle(Bundle bundle){
		declaringBundle=bundle;
	}
	
	/**
	 * returns the declaring bundle,i.e,
	 * the bundle holding this component.
	 * 
	 * @return bundle if set else null
	 */
	public Bundle getDeclaringBundle(){
		return declaringBundle;
	}
	
	/**
	 * @return Returns the autoEnable.
	 */
	public boolean isAutoEnable() {
		return autoEnable;
	}
	/**
	 * @return Returns the serviceFactory.
	 */
	public boolean isServiceFactory() {
		return serviceFactory;
	}
	/**
	 * @param serviceFactory The serviceFactory to set.
	 */
	public void setServiceFactory(boolean serviceFactory) {
		this.serviceFactory = serviceFactory;
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
	public String getFactory() {
		return factory;
	}
	/**
	 * @param factory The factory to set.
	 */
	public void setFactory(String factory) {
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
	
	/**
	 * @param ComponentPropertiesInfo
	 *            The ComponentPropertiesInfo to add.
	 */
	public void addPropertiesInfo(ComponentPropertiesInfo propsInfo){
		propertiesInfo.add(propsInfo);
	}

	/**
	 * @param ComponentPropertyInfo
	 *            The ComponentPropertyInfo to add.
	 */
	public void addPropertyInfo(ComponentPropertyInfo propInfo){
		propertyInfo.add(propInfo);
	}
	
	/**
	 * @param ComponentServiceInfo
	 *            The ComponentServiceInfo to add.
	 */
	public void addServiceInfo(ComponentServiceInfo servInfo){
		serviceInfo.add(servInfo);
	}
	
	/**
	 * @param ComponentReferenceInfo
	 *            The ComponentReferenceInfo to add.
	 */
	public void addReferenceInfo(ComponentReferenceInfo refInfo){
		referenceInfo.add(refInfo);
	}
}
