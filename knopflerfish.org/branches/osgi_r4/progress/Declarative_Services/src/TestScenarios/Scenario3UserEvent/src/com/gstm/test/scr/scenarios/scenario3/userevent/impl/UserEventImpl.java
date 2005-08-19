/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario3.userevent.impl;

import com.gstm.test.scr.scenarios.scenario3.provider1.Provider1;
import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Martin
 * 
 * <?xml version="1.0" encoding="UTF-8"?>
 * <scr:component 
 * 		name="com.gstm.test.scr.scenarios.scenario3.UserEvent" 
 * 		enabled="true" 
 * 		xmlns:scr="http://www.osgi.org/xmlns/scr/v1.0.0">
 * 
 * 	<implementation 
 * 		class="com.gstm.test.scr.scenarios.scenario3.impl.UserEventImpl" />
 * 
 * 	<reference 
 * 		name="message" 
 * 		interface="com.gstm.test.scr.scenarios.scenario3.Provider1" 
 * 		cardinality="1..n" 
 * 		policy="static" 
 * 		target="(component.name=com.gstm.test.scr.scenarios.scenario3.Provider1)" 
 * 		bind="bindProvider" 
 * 		unbind="unbindProvider" 
 * 	/>
</scr:component>
 * 
 */
public class UserEventImpl {
	private Provider1 provider = null;
	WhiteboardImpl board = WhiteboardImpl.INSTANCE;
	
	public UserEventImpl(){

	}
	
	/* bind the service reference*/
	protected void bindProvider1(Provider1 provider){
		this.provider = provider;
		System.out.println("||||||||||||||||The binding has been executed||||||||||||||||");
		int serviceValue = provider.getValue();
		System.out.println("UserEventImpl - The provider has returned the value: " + serviceValue);
		board.setValue("com.gstm.test.scr.scenarios.scenario3.userevent.impl.UserEventImpl", new Integer(serviceValue));
	}
	
	/* unbind the service reference*/
	protected void unbindProvider1(Provider1 provider){
		this.provider = null;
		System.out.println("The unbinding has been executed");
	}
}
