/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.util;

import com.gstm.test.scr.scenarios.util.impl.WhiteboardImpl;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Whiteboard {
    public WhiteboardImpl getInstance();
	public void setValue(String key, Object value);
	public Object getValue(String key);
}
