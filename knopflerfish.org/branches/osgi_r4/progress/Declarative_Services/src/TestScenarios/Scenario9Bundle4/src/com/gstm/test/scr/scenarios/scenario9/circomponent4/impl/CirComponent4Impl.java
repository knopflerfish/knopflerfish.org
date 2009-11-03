/*
 * @(#)CirComponent4Impl.Java        1.0 2005/08/24
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
package com.gstm.test.scr.scenarios.scenario9.circomponent4.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario9.circomponent3.CirComponent3;
import com.gstm.test.scr.scenarios.scenario9.circomponent4.CirComponent4;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 */
public class CirComponent4Impl implements CirComponent4 {

	private ComponentContext context;

	WhiteboardImpl board = WhiteboardImpl.INSTANCE;

	CirComponent3 provider = null;

	public int getValue() {
		return 4;
	}

	public void activate(ComponentContext context) {
		this.context = context;

		try {
			provider = (CirComponent3) context.locateService("message");
		} catch (ClassCastException e) {
			System.err.println("error due to:\n" + e);
		}

		System.out.println("Provider:" + provider);

		if (provider != null) {
			int serviceValue = provider.getValue();
			System.out
					.println("CirComponent4 getting values from CirComponent3:"
							+ serviceValue);
			/* Notifying that the component has received the service */
			board
					.setValue(
							"com.gstm.test.scr.scenarios.scenario9.circomponent4.CirComponent4",
							new Integer(serviceValue));
		}
	}

}