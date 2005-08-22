/*
 * @(#)UserLookImpl.Java        1.0 2005/08/16
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
package com.gstm.test.scr.scenarios.scenario3.userlook.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario3.provider1.Provider1;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin Berg
 * 
 * A bundle with one component used in Scenario 3
 */
public class UserLookImpl {
    private ComponentContext context; 
    WhiteboardImpl board = WhiteboardImpl.INSTANCE;
    Provider1 provider = null;
    
    // component activator 
    public void activate(ComponentContext context) { 
    	this.context = context;
		
    	provider = (Provider1) context.locateService("message");
		
		System.out.println("Provider:" + provider);
		
		if(provider!=null){
			int serviceValue = provider.getValue();
			System.out.println("UserLookImpl - The provider has returned the value: " + serviceValue);
			board.setValue("com.gstm.test.scr.scenarios.scenario3.userlook.impl.UserLookImpl", new Integer(serviceValue));
		}
    }
}
