/*
 * Created on Aug 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario11.impl;

/**
 * @author Martin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IllegalXMLException extends Exception {

	String error;
	Throwable cause;
	
	public IllegalXMLException(String e){
		this.error = e;
	}
	
	public IllegalXMLException(String error, Throwable cause){
		this.error = error;
		this.cause=cause;
	}
	
	public Throwable getCause(){
		return cause;
	}
	
	public String getMessage(){
		return error;
	}
}