/*
 * @(#)ComponentStorage.java        1.0 2005/08/03
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
 * @author Martin
 * 
 * This class stores both satisfied and unsatisfied components using
 * ComponentContainers
 */
public class ComponentStorage {
	/* a list containing all satisfied components */
	private ArrayList satisfiedComponents;

	/* a list containing all unsatisfied components */
	private ArrayList unsatisfiedComponents;

	/**
	 * @return Returns every components, both satisfied and unsatisfied
	 */
	protected ArrayList getComponents() {
		ArrayList allComponents = new ArrayList();
		for (int i = 0; i < satisfiedComponents.size(); i++) {
			allComponents.add(satisfiedComponents.get(i));
		}
		for (int i = 0; i < unsatisfiedComponents.size(); i++) {
			allComponents.add(unsatisfiedComponents.get(i));
		}
		return allComponents;
	}

	/**
	 * @return Returns every satisfied component
	 */
	protected ArrayList getSatisfiedComponents() {
		return satisfiedComponents;
	}

	/**
	 * @return Returns the ComponentContainer containing the specified name or
	 *         null if non found
	 */
	protected ComponentContainer getSatisfiedComponent(String name) {
		for (int i = 0; i < satisfiedComponents.size(); i++) {
			ComponentContainer compCont = (ComponentContainer) satisfiedComponents
					.get(i);
			/*
			 * chacks if the name in the componetDeclaration equals the suplied
			 * name
			 */
			if (compCont.getComponentDeclaration().getComponentName().equals(
					name)) {
				return compCont;
			}
		}
		return null;
	}

	/**
	 * @return Returns every unsatisfied Components
	 */
	protected ArrayList getUnsatisfiedComponents() {
		return unsatisfiedComponents;
	}

	/**
	 * @return Returns the ComponentContainer containing the specified name or
	 *         null if non found
	 */
	protected ComponentContainer getUnsatisfiedComponent(String name) {
		for (int i = 0; i < unsatisfiedComponents.size(); i++) {
			ComponentContainer compCont = (ComponentContainer) unsatisfiedComponents
					.get(i);
			/*
			 * chacks if the name in the componetDeclaration equals the suplied
			 * name
			 */
			if (compCont.getComponentDeclaration().getComponentName().equals(
					name)) {
				return compCont;
			}
		}
		return null;
	}

	/**
	 * @param name
	 *            the name of the component to locate
	 * @return Returns the component that matches the name
	 */
	protected ComponentContainer getComponent(String name) {
		ComponentContainer unsatisfiedCompCont = getUnsatisfiedComponent(name);
		ComponentContainer satisfiedCompCont = getSatisfiedComponent(name);

		if ((unsatisfiedCompCont == null) && (satisfiedCompCont == null)) {
			return null;
		} else if ((unsatisfiedCompCont != null) && (satisfiedCompCont == null)) {
			return unsatisfiedCompCont;
		} else {
			return satisfiedCompCont;
		}
	}

	/**
	 * Deletes the ComponentContainer, ComponentDeclaration and all its
	 * ComponentContexts where the parameter name matches the name in the
	 * ComponentDeclaration
	 * 
	 * @param String
	 *            name, the name of the component to delete.
	 *  
	 */
	protected void deleteComponent(String name) {
		/* Get the desired componentContainer */
		ComponentContainer compCont = getComponent(name);

		/* Empty the componentContexts list */
		compCont.emptyComponentContexts();
	}

	/**
	 * Deletes the ComponentContainer, ComponentDeclaration and all its
	 * ComponentContexts where the parameter name matches the
	 * ComponentDeclaration in the ComponentDeclaration
	 * 
	 * @param A
	 *            componentDeclaration to be deleted
	 */
	protected void deleteComponent(ComponentDeclaration compDec) {
		/* Get the desired componentContainer */
		ComponentContainer compCont = getComponent(compDec.getComponentName());

		/* Empty the componentContexts list */
		compCont.emptyComponentContexts();
	}

	/**
	 * @param the
	 *            componentContainer to set to satisfied
	 */
	protected void setSatisfied(ComponentContainer compCont) {
		satisfiedComponents.add(compCont);
	}

	/**
	 * @param the
	 *            componentContainer to set to unsatisfied
	 */
	protected void setUnsatisfied(ComponentContainer compCont) {
		unsatisfiedComponents.add(compCont);
	}
}
