/*
 * @(#)ComponentReferenceInfo.java        1.0 2005/07/28
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

/**
 * @author Administrator
 * 
 * This class is a helper classes that supply additional information to the
 * component class
 */
public class ComponentReferenceInfo {

	/* component reference name */
	String referenceName;

	/* Java interface */
	String interfaceType;

	/* The number of service wich will bind up to this component */
	String cardinality;

	/*
	 * Indicate when this service may be activated and deactivated, (either
	 * static or dynamic)
	 */
	String policy;

	/* Selection filter to be used when selecting a desired service */
	String target;

	/* The name of the component method to call when to bind */
	String bind;

	/* The name of the component method to call when to unbind */
	String unbind;

	/**
	 * @return Returns the bind.
	 */
	public String getBind() {
		return bind;
	}

	/**
	 * @param bind
	 *            The bind to set.
	 */
	public void setBind(String bind) {
		this.bind = bind;
	}

	/**
	 * @return Returns the cardinality.
	 */
	public String getCardinality() {
		return cardinality;
	}

	/**
	 * @param cardinality
	 *            The cardinality to set.
	 */
	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}

	/**
	 * @return Returns the interfaceType.
	 */
	public String getInterfaceType() {
		return interfaceType;
	}

	/**
	 * @param interfaceType
	 *            The interfaceType to set.
	 */
	public void setInterfaceType(String interfaceType) {
		this.interfaceType = interfaceType;
	}

	/**
	 * @return Returns the policy.
	 */
	public String getPolicy() {
		return policy;
	}

	/**
	 * @param policy
	 *            The policy to set.
	 */
	public void setPolicy(String policy) {
		this.policy = policy;
	}

	/**
	 * @return Returns the referenceName.
	 */
	public String getReferenceName() {
		return referenceName;
	}

	/**
	 * @param referenceName
	 *            The referenceName to set.
	 */
	public void setReferenceName(String referenceName) {
		this.referenceName = referenceName;
	}

	/**
	 * @return Returns the target.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target
	 *            The target to set.
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * @return Returns the unbind.
	 */
	public String getUnbind() {
		return unbind;
	}

	/**
	 * @param unbind
	 *            The unbind to set.
	 */
	public void setUnbind(String unbind) {
		this.unbind = unbind;
	}
}
