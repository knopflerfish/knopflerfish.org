/*
 * @(#)Component1Impl.Java        1.0 2005/08/23
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
package com.gstm.test.scr.scenarios.scenario8.faultyprovider.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario8.faultyprovider.FaultyProvider;

/**
 * @author Martin
 */
public class FaultyProviderImpl implements FaultyProvider {

	ComponentContext context;
	
    public void activate(ComponentContext context) { 
    	
		System.out.println("Scenario 8 FaultyProvider: Waiting in activate......");
		synchronized (this){
			while(true){
    			/* hanging around */
    		}
		}
//		synchronized (this) {
//			try {
//				System.out.println("Scenario 8 FaultyProvider: Waiting in activate......");
//				wait(180000);
//			} catch (Exception e) {
//				System.out.println("Scenario 8 FaultyProvider: Couldn't wait:" + e);
//			}
//		}
    	
//    	this.context = context;
    }
	
	public int getValue() {
		return 15;
	}

}
