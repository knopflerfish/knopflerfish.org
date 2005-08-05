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

	protected ComponentStorage(){
		satisfiedComponents = new ArrayList();
		unsatisfiedComponents = new ArrayList();
	}
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
		//ComponentContainer compCont = getComponent(name);
		
		ComponentContainer compCont;
		
		/* Check the unstaisfied list */
		for (int i = 0; i < unsatisfiedComponents.size(); i++) {
			compCont = (ComponentContainer) unsatisfiedComponents
					.get(i);
			/*
			 * checks if the name in the componetDeclaration equals the suplied
			 * name
			 */
			if (compCont.getComponentDeclaration().getComponentName().equals(
					name)) {
				unsatisfiedComponents.remove(i);
				/* Empty the componentContexts list */
				compCont.emptyComponentContexts();
				/* Remove the ComponentDeclaration */
				compCont.removeComponentDeclaration();
			}
		}
		
		/* Check the satisfied list */
		for (int i = 0; i < satisfiedComponents.size(); i++) {
			compCont = (ComponentContainer) satisfiedComponents
					.get(i);
			/*
			 * checks if the name in the componetDeclaration equals the suplied
			 * name
			 */
			if (compCont.getComponentDeclaration().getComponentName().equals(
					name)) {
				satisfiedComponents.remove(i);
				/* Empty the componentContexts list */
				compCont.emptyComponentContexts();
				/* Remove the ComponentDeclaration */
				compCont.removeComponentDeclaration();
			}
		}
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
	 * This method checks if the ComponentDeclaration already is pressent in the unsatisfied
	 * list and if so moves it to the satisfied list. It also checks if the supplied 
	 * ComponentContainer (or another ComponentContainer that has the same ComponentDelaration 
	 * i.e they are equal) already exists in the satisfied list. If so it checks if all supplied
	 * ComponentContexts already is pressent in the ComponentContainer, if one or more isn't the 
	 * they are added to the ComponentContainer already existing in the satisfied list. If no 
	 * there is no match the supplied ComponentContainer is added to the satisfied list.
	 *  
	 * @param the
	 *            componentContainer to set to satisfied
	 */
	protected void setSatisfied(ComponentContainer incommingCompContainer) {
		/* boolean showing if the incomming container equals a stored container */
		boolean foundSatisfiedComponentContainer = false;
		boolean foundUnsatisfiedComponentContainer = false;
		
		/* boolean showing if the component declaration in the incomming container equals a stored component declaration */
		boolean foundComponentDeclaration = false;
		
		/* check if the incomming container is already stored in the storage array for unsatisfied components */
		for (int i = 0 ; i < unsatisfiedComponents.size() ; i++){
			ComponentContainer storedCompContainer = (ComponentContainer) unsatisfiedComponents.get(i);
			/* if the component Declaration is the same a match is found */
			if(storedCompContainer.getComponentDeclaration().equals(incommingCompContainer.getComponentDeclaration())){
				foundUnsatisfiedComponentContainer = true;
				/* Move the component to the satisfied array */
				satisfiedComponents.add(storedCompContainer);
				unsatisfiedComponents.remove(i);
				/* move pointer to previus possition */
				i--;
			}
		}
		
		/* If no ComponentContainer was found in the unsatisfied array, check if an equal container already exists in the satisfied array */
		if(foundUnsatisfiedComponentContainer == false){
			if(satisfiedComponents.contains(incommingCompContainer)){
				/* The componentContainer already exists in satisfied */
				foundSatisfiedComponentContainer = true;
			}
		}
				
		/* The Container was not the same but the contents can all the same be equal */
		if(foundSatisfiedComponentContainer == false){
			for (int i = 0 ; i < satisfiedComponents.size() ; i++){
				ComponentContainer storedCompContainer = (ComponentContainer) satisfiedComponents.get(i);
				/* if the component Declaration is the same a match is found */
				if(storedCompContainer.getComponentDeclaration().equals(incommingCompContainer.getComponentDeclaration())){
					foundComponentDeclaration = true;
					/* Needs to check if the incomming component context already exist in the stored componentContainer */
					ArrayList incommingCompontexts = incommingCompContainer.getComponentContexts();
					for(int j=0 ; j < incommingCompontexts.size() ; j++){
						if (storedCompContainer.getComponentContexts().contains(incommingCompontexts.get(j))){
							// do nothing they are equal
						}else{
							/* Add the component context that was missing */
							storedCompContainer.getComponentContexts().add(incommingCompontexts.get(j));
						}
					}
				}
			}
		}

		/* No equal container or part of the container was found so add the incomming container */
		if(foundComponentDeclaration == false){
			insertSatisfied(incommingCompContainer);
		}	
	}
	
	/**
	 * This method checks if the ComponentDeclaration already is pressent in the satisfied
	 * list and if so moves it to the unsatisfied list. It also checks if the supplied 
	 * ComponentContainer (or another ComponentContainer that has the same ComponentDelaration 
	 * i.e they are equal) already exists in the unsatisfied list. If so it checks if all supplied
	 * ComponentContexts already is pressent in the ComponentContainer, if one or more isn't the 
	 * they are added to the ComponentContainer already existing in the unsatisfied list. If no 
	 * there is no match the supplied ComponentContainer is added to the unsatisfied list.
	 * 
	 * @param the
	 *            componentContainer to set to unsatisfied
	 */
	protected void setUnsatisfied(ComponentContainer incommingCompContainer) {
		/* boolean showing if the incomming container equals a stored container */
		boolean foundSatisfiedComponentContainer = false;
		boolean foundUnsatisfiedComponentContainer = false;
		
		/* boolean showing if the component declaration in the incomming container equals a stored component declaration */
		boolean foundComponentDeclaration = false;
		
		/* check if the incomming container is already stored in the storage array for unsatisfied components */
		for (int i = 0 ; i < satisfiedComponents.size() ; i++){
			ComponentContainer storedCompContainer = (ComponentContainer) satisfiedComponents.get(i);
			/* if the component Declaration is the same a match is found */
			if(storedCompContainer.getComponentDeclaration().equals(incommingCompContainer.getComponentDeclaration())){
				foundSatisfiedComponentContainer = true;
				/* Move the component to the satisfied array */
				unsatisfiedComponents.add(storedCompContainer);
				satisfiedComponents.remove(i);
				/* move pointer to previus possition */
				i--;
			}
		}
		
		/* If no ComponentContainer was found in the unsatisfied array, check if an equal container already exists in the satisfied array */
		if(foundSatisfiedComponentContainer == false){
			if(unsatisfiedComponents.contains(incommingCompContainer)){
				/* The componentContainer already exists in satisfied */
				foundUnsatisfiedComponentContainer = true;
			}
		}
				
		/* The Container was not the same but the contents can all the same be equal */
		if(foundUnsatisfiedComponentContainer == false){
			for (int i = 0 ; i < unsatisfiedComponents.size() ; i++){
				ComponentContainer storedCompContainer = (ComponentContainer) unsatisfiedComponents.get(i);
				/* if the component Declaration is the same a match is found */
				if(storedCompContainer.getComponentDeclaration().equals(incommingCompContainer.getComponentDeclaration())){
					foundComponentDeclaration = true;
					/* Needs to check if the incomming component context already exist in the stored componentContainer */
					ArrayList incommingCompontexts = incommingCompContainer.getComponentContexts();
					for(int j=0 ; j < incommingCompontexts.size() ; j++){
						if (storedCompContainer.getComponentContexts().contains(incommingCompontexts.get(j))){
							// do nothing they are equal
						}else{
							/* Add the component context that was missing */
							storedCompContainer.getComponentContexts().add(incommingCompontexts.get(j));
						}
					}
				}
			}
		}

		/* No equal container or part of the container was found so add the incomming container */
		if(foundComponentDeclaration == false){
			insertUnsatisfied(incommingCompContainer);
		}
	}

	private void insertSatisfied(ComponentContainer incommingCompContainer){
		satisfiedComponents.add(incommingCompContainer);
	}

	private void insertUnsatisfied(ComponentContainer incommingCompContainer){
		unsatisfiedComponents.add(incommingCompContainer);
	}
}
