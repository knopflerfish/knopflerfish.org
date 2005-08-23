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
package com.gstm.test.scr.scenarios.scenario6.component1.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario6.component1.Component1;
import com.gstm.test.scr.scenarios.scenario6.factory.CounterFactory;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 */
public class Component1Impl implements Component1 {
    private ComponentContext context; 
    WhiteboardImpl board = WhiteboardImpl.INSTANCE;
    CounterFactory provider = null;
    
    // component activator 
    public void activate(ComponentContext context) { 
    	this.context = context;
		
		try{
			provider = (CounterFactory) context.locateService("counter");
   		}catch(ClassCastException e){
   			System.err.println("error due to:\n" + e);
   		}
		
		System.out.println("Provider:" + provider);
		
		if(provider!=null){
			System.out.print("Component1 getting values from counterFactory:");
			for(int i=0 ; i<50 ; i++){
				int serviceValue = provider.getValue();
				if(i == 0){
					System.out.print(serviceValue);
				} else{
					System.out.print(", " + serviceValue);
				}
				board.setValue("com.gstm.test.scr.scenarios.scenario6.component1.Component1", new Integer(serviceValue));
			}
			/* New line */
			System.out.println("");
		}
    }
}
