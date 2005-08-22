/*
 * @(#)UserEventImpl.Java        1.0 2005/08/16
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
package com.gstm.test.scr.scenarios.scenario3.userevent.impl;

import com.gstm.test.scr.scenarios.scenario3.provider1.Provider1;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin Berg
 * 
 */
public class UserEventImpl {
	private Provider1 provider = null;
	WhiteboardImpl board = WhiteboardImpl.INSTANCE;
	
	public UserEventImpl(){

	}
	
	/* bind the service reference*/
	protected void bindProvider1(Provider1 provider){
		this.provider = provider;
		int serviceValue = provider.getValue();
		System.out.println("UserEventImpl - The provider has returned the value: " + serviceValue);
		board.setValue("com.gstm.test.scr.scenarios.scenario3.userevent.impl.UserEventImpl", new Integer(serviceValue));
	}
	
	/* unbind the service reference*/
	protected void unbindProvider1(Provider1 provider){
		this.provider = null;
	}
}
