/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario3.userlook.impl;

import org.osgi.service.component.ComponentContext;

import com.gstm.test.scr.scenarios.scenario3.provider1.Provider1;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class UserLookImpl {
    private ComponentContext context; 
    WhiteboardImpl board = WhiteboardImpl.INSTANCE;
    Provider1 provider = null;
    
    // component activator 
    public void activate(ComponentContext context) { 
    	System.out.println("||||||||||||||||The activate has been executed||||||||||||||||");
    	this.context = context;
		
		provider = (Provider1) context.locateService("message");
		System.out.println("Provider:" + provider);
		if(provider!=null){
			int serviceValue = provider.getValue();
			System.out.println("UserLookImpl - The provider has returned the value: " + serviceValue);
			board.setValue("com.gstm.test.scr.scenarios.scenario3.userlook.impl.UserLookImpl", new Integer(serviceValue));
		}
    }
    
    public UserLookImpl(){

    }
}
