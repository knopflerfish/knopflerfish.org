package com.isv.process.myvalue.impl; 
import org.osgi.service.component.ComponentContext; 
import com.isv.process.myvalue.*; 
import com.isv.service.stockquote.*; 
import com.isv.service.customerinfo.*; 
public class MyValueImpl implements MyValue { 
    private ComponentContext context; 
    
    // component activator 
    protected void activate(ComponentContext context) { 
	// prepare to be useable as a service 
	this.context = context; 
    } 

    public float getMyValue(String customerID) throws MyValueException {
	// variables Customer 
	customer = null; 	
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