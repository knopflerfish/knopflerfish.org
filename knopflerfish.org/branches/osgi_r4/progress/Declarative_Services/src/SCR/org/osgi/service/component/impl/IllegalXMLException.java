/*
 * Created on Aug 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.osgi.service.component.impl;

/**
 * @author Martin
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IllegalXMLException extends Exception {

	String error;
	
	IllegalXMLException(String error){
		this.error = error;
	}
}
