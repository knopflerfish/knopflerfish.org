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
    
    // component activator 
    protected void activate(ComponentContext context) { 
		this.context = context; 
    }
    
    public UserLookImpl(){
    	new ValueGetter();
    }
    
    private ComponentContext getContext(){
    	return context;
    }
	
    /* A class used to accuire the value in the service when the service has been bound */
	class ValueGetter extends Thread{
		ComponentContext context;
		
		/* Shared Dictonary, store events */
		WhiteboardImpl board = WhiteboardImpl.INSTANCE;
		//Whiteboard board = WhiteboardImpl.getInstance();
		
		boolean searchingProvider = true; 
		
		ValueGetter(){
			while(searchingProvider){
				context = getContext();
		
				if(context == null){
					/* no service bound waiting a litle */
				} else {
					Provider1 provider = (Provider1) context.locateService("message");
					
					int serviceValue = provider.getValue();
					System.out.println("UserLookImpl - The provider has returned the value: " + serviceValue);
					board.setValue("com.gstm.test.scr.scenarios.scenario3.UserLookImpl", new Integer(serviceValue));
					searchingProvider = false;
				}
				
				/* Let the service get avalible */
				try {
					wait(40);
				} catch (Exception e) {
					System.out.println("Couldn't wait:" + e);
				}
			}
		}
	}
}
