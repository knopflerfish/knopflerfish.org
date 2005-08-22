/*
 * Created on Aug 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.scenario6.factory.impl;

import com.gstm.test.scr.scenarios.scenario6.factory.CounterFactory;

/**
 * @author Martin
 * 
 */
public class CounterFactoryImpl implements CounterFactory {
	private int value = 0;
	
	public int getValue(){
		value= value + 1;
		return value;
	}
}
