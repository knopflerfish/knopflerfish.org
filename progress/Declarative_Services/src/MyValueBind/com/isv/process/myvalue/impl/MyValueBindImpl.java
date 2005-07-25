package com.isv.process.myvalue.impl; 
import com.isv.process.myvalue.MyValueBind;
import com.isv.process.myvalue.MyValueException;
import com.isv.service.customerinfo.Customer;
import com.isv.service.customerinfo.CustomerInfo;
import com.isv.service.stockquote.StockQuote;

/**
 * This class is the MyValue Implementation using the
 * Bind method
 * @author magnus
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MyValueBindImpl implements MyValueBind { 
	private CustomerInfo cInfo; 
    private StockQuote sQuote; 
    
    	protected void bindCustomerInfo(CustomerInfo cInfo) { 
    		this.cInfo = cInfo; 
    	} 

        protected void unbindCustomerInfo(CustomerInfo cInfo) { 
    	this.cInfo = null; 
        } 

        protected void bindStockQuote(StockQuote sQuote) { 
    	this. sQuote = sQuote; 
        } 

        protected void unbindStockQuote(StockQuote sQuote) { 	
    	this. sQuote = null; 
        } 

        public float getMyValue(String customerID) throws MyValueException { 
    	// variables 
    	Customer customer = null; 
    	float quote = 0; 
    	float value = 0; 

    	// invoke 
    	customer = cInfo.getCustomer(customerID); 

    	if (customer.getErrorMsg().equals("")) { 
    	    // invoke 
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
}