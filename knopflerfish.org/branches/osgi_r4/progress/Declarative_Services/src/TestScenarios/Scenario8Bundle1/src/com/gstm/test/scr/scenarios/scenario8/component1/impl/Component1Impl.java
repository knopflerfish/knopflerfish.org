/*
 * @(#)Component1Impl.Java        1.0 2005/08/22
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
package com.gstm.test.scr.scenarios.scenario8.component1.impl;

import org.osgi.service.component.ComponentContext;


import com.gstm.test.scr.scenarios.scenario8.faultyprovider.FaultyProvider;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 */
public class Component1Impl  {
    private ComponentContext context; 
    WhiteboardImpl board = WhiteboardImpl.INSTANCE;
    FaultyProvider provider = null;
    
    public Component1Impl(){
    	/* Notifying that the component has been instanciated */
    	board.setValue("com.gstm.test.scr.scenarios.scenario8.component1.Component1", new Integer(10));
    }
    
    // component activator 
    public void activate(ComponentContext context) { 
    	this.context = context;
    	
    	try{
    		provider = (FaultyProvider) context.locateService("message");
   		}catch(ClassCastException e){
   			System.err.println("error due to:\n" + e);
   		}
		
		System.out.println("Provider:" + provider);
		
		if(provider!=null){
			int serviceValue = provider.getValue();
			System.out.println("Component1 getting values from faultyProvider:" + serviceValue);
			/* Notifying that the component has received the service */
			board.setValue("com.gstm.test.scr.scenarios.scenario8.component1.Component1", new Integer(serviceValue));
		}
    }
}
