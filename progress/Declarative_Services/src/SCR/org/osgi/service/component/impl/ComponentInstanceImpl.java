/*
 * Created on Jul 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.osgi.service.component.impl;

import org.osgi.service.component.ComponentInstance;

/**
 * @author magnus
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ComponentInstanceImpl implements ComponentInstance{
	private Object instanceElement;
	
	public ComponentInstanceImpl(Object element){
		instanceElement = element;
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentInstance#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub	
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.component.ComponentInstance#getInstance()
	 */
	public Object getInstance() {
		// TODO Auto-generated method stub
		return instanceElement;
	}

}
