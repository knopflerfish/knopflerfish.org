/*
 * @(#)Provider2Impl.Java        1.0 2005/08/16
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
package com.gstm.test.scr.scenarios.scenario4.immediatecomponent1.impl;

import org.osgi.service.component.ComponentContext;

/**
 * @author Martin
 */
public class ImmediateComponent1Impl {

	private ComponentContext context; 
	
	 public void activate(ComponentContext context) { 
    	this.context = context;
    	
    	System.out.println("Starting ImmediateComponent1");
    	
    	activateComponents();
	 }
	 
	 private void activateComponents(){
	 	/* Starting DealyedComponent1 */
	 	System.out.println("<<<<<<<<<< Enabeling DealyedComponent1 >>>>>>>>>>>>>>");
	 	context.enableComponent("com.gstm.test.scr.scenarios.scenario4.delayedcomponent1.DelayedComponent1");
	 	
	 	/* Waiting a coouple of seconds */
	 	synchronized (this) {
	 		try {
				System.out.println("ImmediateComponent1: waiting a litle.....");
				wait(2000);
			} catch (Exception e) {
				System.err.println("Couldn't wait:" + e);
			}
	 	}
	 	
		/* Starting ImmediateComponent2 and ImmediateComponent3 */ 
	 	System.out.println("<<<<<<<<<< Enabeling ImmediateComponent2 and ImmediateComponent3 >>>>>>>>>>>>>>");
	 	context.enableComponent(null);
//	 	System.out.println("<<<<<<<<<< Enabeling ImmediateComponent2 >>>>>>>>>>>>>>");
//	 	context.enableComponent("com.gstm.test.scr.scenarios.scenario4.immediatecomponent2.ImmediateComponent2");
//	 	System.out.println("<<<<<<<<<< Enabeling ImmediateComponent3 >>>>>>>>>>>>>>");
//	 	context.enableComponent("com.gstm.test.scr.scenarios.scenario4.immediatecomponent3.ImmediateComponent3");
	 	
	 }
}
