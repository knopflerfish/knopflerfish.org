package com.isv.service.customerinfo.impl;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.isv.service.customerinfo.Customer;
import com.isv.service.customerinfo.CustomerInfo;


public class CustomerInfoImpl implements CustomerInfo {
	private ComponentContext componentContext;
	
	public void activate(ComponentContext context){
		
		componentContext = context;
		
		System.out.println("\n****************** Customer info was activated with data below ***********");
		System.out.println("Component name:" + context.getProperties().get(ComponentConstants.COMPONENT_NAME));
		System.out.println("Component id:" + context.getProperties().get(ComponentConstants.COMPONENT_ID));
		System.out.println("Component Service reference:" + context.getServiceReference());
		System.out.println("\n");
		
		
		
		
		
	}
	
	public Customer getCustomer(String customerID) {
		// here is where the interaction with business logic would go 
		Customer cust = new Customer();
		cust.setCustNo(customerID);
		cust.setFirstName("Victor");
		cust.setLastName("Hugo");
		cust.setSymbol("IBM");
		cust.setNumShares(100);
		cust.setPostalCode("10589");
		cust.setErrorMsg("");
		
		
		
		return cust;
		
	
	} 
}