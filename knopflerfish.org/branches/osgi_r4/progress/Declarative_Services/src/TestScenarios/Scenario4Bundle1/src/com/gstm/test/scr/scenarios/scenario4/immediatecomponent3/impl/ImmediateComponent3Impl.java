/*
 * @(#)ImmediateComponent2Impl.Java        1.0 2005/08/16
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
package com.gstm.test.scr.scenarios.scenario4.immediatecomponent3.impl;

import com.gstm.test.scr.scenarios.scenario4.delayedcomponent1.DelayedComponent1;
import com.gstm.test.scr.scenarios.scenario4.immediatecomponent2.ImmediateComponent2;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 */
public class ImmediateComponent3Impl implements ImmediateComponent2{
	
	private DelayedComponent1 provider = null;
	WhiteboardImpl board = WhiteboardImpl.INSTANCE;
	
	/* bind the service reference*/
	protected void bindDelayedComponent1(DelayedComponent1 provider){
		this.provider = provider;
		int serviceValue = provider.getValue();
		System.out.println("ImmediateComponent3Impl - The provider has returned the value: " + serviceValue);
		board.setValue("com.gstm.test.scr.scenarios.scenario4.immediatecomponent3.ImmediateComponent3", new Integer(serviceValue));
	}
	
	/* unbind the service reference*/
	protected void unbindDelayedComponent1(DelayedComponent1 provider){
		this.provider = null;
	}
}
