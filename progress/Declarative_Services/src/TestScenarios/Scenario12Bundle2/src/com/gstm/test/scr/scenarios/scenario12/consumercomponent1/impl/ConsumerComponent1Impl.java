/*
 * Created on Aug 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario12.consumercomponent1.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario12.consumercomponent1.ConsumerComponent1;
import com.gstm.test.scr.scenarios.scenario12.serviceprovider1.ServiceProvider1;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 */
public class ConsumerComponent1Impl implements ConsumerComponent1 {

	private ComponentContext context;

	WhiteboardImpl board = WhiteboardImpl.INSTANCE;

	ServiceProvider1 provider = null;

	public void activate(ComponentContext context) {
		this.context = context;

		try {
			provider = (ServiceProvider1) context.locateService("getNumber");
		} catch (ClassCastException e) {
			System.err.println("error due to:\n" + e);
		}

		System.out.println("Provider:" + provider);

		if (provider != null) {
			int serviceValue = provider.getValue();
			System.out
					.println("ConsumerComponent1 getting values from ServiceProvider1:"
							+ serviceValue);
			/* Notifying that the component has received the service */
			board
					.setValue(
							"com.gstm.test.scr.scenarios.scenario12.comsumercomponent1.ConsumerComponent1",
							new Integer(serviceValue));
		}
	}

	public void deactivate(ComponentContext context) {
		System.out.println("ConsumerComponent1 is deactivated");
		this.context = null;
		
		/* Notifying that the component has been deactivated */
		board
				.setValue(
						"com.gstm.test.scr.scenarios.scenario12.comsumercomponent1.ConsumerComponent1",
						new Integer(2));
	}
}
