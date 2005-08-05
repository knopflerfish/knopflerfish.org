/*
 * @(#)ComponentContainer.java        1.0 2005/08/03
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

import org.osgi.service.component.ComponentContext;

/**
 * @author Martin
 *
 * ComponentContainer stores a component, a component
 * consists of two major parts:
 * - 1 ComponentDeclaration instance (always mandatory)
 * - 0 to N ComponentContext instances (mandatory only if Component is satisfied)
 */
public class ComponentContainer {

	private ComponentDeclaration componentDeclaration;
	private ArrayList componentContexts;

	protected ComponentContainer(ComponentDeclaration componentDeclaration){
		this.componentDeclaration = componentDeclaration;
		componentContexts = new ArrayList();
	}
	
	/**
	 * @return Returns the componentContexts.
	 */
	protected ArrayList getComponentContexts() {
		return componentContexts;
	}
	/**
	 * @param componentContext The componentContext to insert.
	 */
	protected void insertComponentContext(ComponentContext componentContext) {
		componentContexts.add(componentContext);
	}
	/**
	 * @return Returns the componentDeclaration.
	 */
	protected ComponentDeclaration getComponentDeclaration() {
		return componentDeclaration;
	}
	
	/**
	 * Empty the componentContexts list
	 */
	protected void emptyComponentContexts(){
		componentContexts.clear();
	}
	/**
	 * Empty the componentDeclaration
	 */
	protected void removeComponentDeclaration(){
		componentDeclaration = null;
	}
}
