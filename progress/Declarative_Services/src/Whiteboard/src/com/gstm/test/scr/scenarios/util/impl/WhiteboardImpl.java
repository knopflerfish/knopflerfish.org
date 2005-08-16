/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.util.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import com.gstm.test.scr.scenarios.util.Whiteboard;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WhiteboardImpl implements Whiteboard{
	 Dictionary d = new Hashtable();

	public final static WhiteboardImpl INSTANCE = new WhiteboardImpl();
    private WhiteboardImpl() {
          // Exists only to defeat instantiation.
	}
    public static Whiteboard getInstance(){
    	return INSTANCE;
    }
	public void setValue(String key, Object value){
		d.put(key, value);
	}
	public Object getValue(String key){
		return d.get(key);// can this generate an exception?
	}

}
