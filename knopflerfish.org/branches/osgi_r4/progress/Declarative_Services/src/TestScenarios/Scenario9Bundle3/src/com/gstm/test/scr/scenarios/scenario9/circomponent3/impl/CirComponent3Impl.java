/*
 * @(#)CirComponent3Impl.Java        1.0 2005/08/24
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
package com.gstm.test.scr.scenarios.scenario9.circomponent3.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario9.circomponent3.CirComponent3;
import com.gstm.test.scr.scenarios.scenario9.circomponent4.CirComponent4;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 */
public class CirComponent3Impl implements CirComponent3 {

	private ComponentContext context;

	WhiteboardImpl board = WhiteboardImpl.INSTANCE;

	CirComponent4 provider = null;

	public int getValue() {
		return 3;
	}

	public void activate(ComponentContext context) {
		this.context = context;

			provider = (CirComponent4) context.locateService("message");
			if(provider == null){
				System.out.println("No provider for CirComponent3 pressent yet.....");
			}
			
		System.out.println("Provider:" + provider);

		if (provider != null) {
			int serviceValue = provider.getValue();
			System.out
					.println("CirComponent3 getting values from CirComponent4:"
							+ serviceValue);
			/* Notifying that the component has received the service */
			board
					.setValue(
							"com.gstm.test.scr.scenarios.scenario9.circomponent3.CirComponent3",
							new Integer(serviceValue));
		}
	}
}
