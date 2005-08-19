/*
 * @(#)MyValueLocateImpl.java        1.0 2005/06/28
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

package com.isv.process.myvalue.impl; 


import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.service.component.ComponentContext; 
import com.isv.process.myvalue.*; 
import com.isv.service.stockquote.*; 
import com.isv.service.customerinfo.*;

public class MyValueLocateImpl implements MyValueLocate { 
    private ComponentContext context; 
    private StockQuote sQuote;
    // component activator 
    protected void activate(ComponentContext context) { 
	// prepare to be useable as a service 
	this.context = context; 
	
	System.out.println("\nMyValueLocate was activated:");
	Dictionary properties = context.getProperties();
	Enumeration keys = properties.keys();
	
	for(;keys.hasMoreElements();){
		String key=(String)keys.nextElement();
		System.out.println(key +":" + properties.get(key));
	}
	
	
	System.out.println("\n");
	
	
	
	
    } 

    public float getMyValue(String customerID) throws MyValueException {
	// variables Customer 
	Customer customer = null; 	
	float quote = 0; 	
	float value = 0; 

	// invoke 
	CustomerInfo cInfo = (CustomerInfo)context.locateService("customerInfo"); 
	customer = cInfo.getCustomer(customerID); 
	if (customer.getErrorMsg().equals("")) { 
	    // invoke StockQuote 
	    sQuote = (StockQuote)context.locateService("stockQuote"); 
	    quote = sQuote.getQuote(customer.getSymbol()); 

	    // assign 
	    value = quote * customer.getNumShares(); 
	} else { 
	    // throw 
	    throw new MyValueException(customer.getErrorMsg()); 
	} 
	
	// reply 
	return value; 
    } 
    // component dectivator 
    protected void deactivate(ComponentContext context) 
    { 
	// clean up; we are no longer a useable service 
	this.context = null; 
    } 
}