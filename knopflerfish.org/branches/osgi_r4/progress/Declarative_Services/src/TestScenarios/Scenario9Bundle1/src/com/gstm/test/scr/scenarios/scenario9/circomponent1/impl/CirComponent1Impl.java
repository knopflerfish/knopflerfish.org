/*
 * @(#)CirComponent1Impl.Java        1.0 2005/08/24
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
package com.gstm.test.scr.scenarios.scenario9.circomponent1.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario9.circomponent2.CirComponent2;
import com.gstm.test.scr.scenarios.scenario9.circomponent1.CirComponent1;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 */
public class CirComponent1Impl implements CirComponent1 {

	private ComponentContext context;

	WhiteboardImpl board = WhiteboardImpl.INSTANCE;

	CirComponent2 provider = null;

	public int getValue() {
		return 1;
	}

	public void activate(ComponentContext context) {
		this.context = context;

		try {
			provider = (CirComponent2) context.locateService("message");
		} catch (ClassCastException e) {
			System.err.println("error due to:\n" + e);
		}

		System.out.println("Provider:" + provider);

		if (provider != null) {
			int serviceValue = provider.getValue();
			System.out
					.println("CirComponent1 getting values from CirComponent2:"
							+ serviceValue);
			/* Notifying that the component has received the service */
			board
					.setValue(
							"com.gstm.test.scr.scenarios.scenario9.circomponent1.CirComponent1",
							new Integer(serviceValue));
		}
	}
}
