/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario3.provider1.impl;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import com.gstm.test.scr.scenarios.scenario3.provider1.Provider1;

/**
 * @author Martin
 */
public class Provider1Impl implements Provider1, ManagedService {
	
	/* return message */
	int msg = 0;
	
	public int getValue(){
		return msg;
	}

	public void updated(Dictionary properties) throws ConfigurationException {
		msg = ((Integer)properties.get("message")).intValue();
	}
}
