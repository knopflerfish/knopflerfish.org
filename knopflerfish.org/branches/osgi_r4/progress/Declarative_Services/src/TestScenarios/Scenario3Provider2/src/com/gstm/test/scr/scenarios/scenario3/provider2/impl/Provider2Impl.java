/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario3.provider2.impl;

import com.gstm.test.scr.scenarios.scenario3.provider1.Provider1;

/**
 * @author Martin
 */
public class Provider2Impl implements Provider1 {
	
	/* return message */
	int msg = 2;
	
	public int getValue(){
		return msg;
	}
}
