/*
 * @(#)Provider1Impl.Java        1.0 2005/08/16
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
package com.gstm.test.scr.scenarios.scenario3.provider1.impl;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import com.gstm.test.scr.scenarios.scenario3.provider1.Provider1;

/**
 * @author Martin
 */
public class Provider1Impl implements Provider1, ManagedService {
	
	/* return message */
	int msg = 0;
	
	public int getValue(){
		return msg;
	}

	public void updated(Dictionary properties) throws ConfigurationException {
		msg = ((Integer)properties.get("message")).intValue();
	}
}
