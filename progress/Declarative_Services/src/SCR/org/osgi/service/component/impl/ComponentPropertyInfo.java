/*
 * @(#)ComponentPropertyInfo.java        1.0 2005/07/28
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
 * @author Administrator
 * 
 * This class is a helper classes that supply additional information to the
 * component class
 */
public class ComponentPropertyInfo {
	/* Property name */
	private String name;

	/* Property value */
	private ArrayList value;

	/* Property type */
	private String type;

	ComponentPropertyInfo(){
		value = new ArrayList();
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return Returns the value.
	 */
	public ArrayList getValues() {
		return value;
	}

	/**
	 * @param value
	 *            The value to set.
	 */
	public void setValue(String value) {
		this.value.add(value);
	}
}
