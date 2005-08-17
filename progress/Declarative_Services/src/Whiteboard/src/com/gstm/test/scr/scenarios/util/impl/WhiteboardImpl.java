/*
 * Created on Aug 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.gstm.test.scr.scenarios.util.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


public class WhiteboardImpl implements BundleActivator{
	 Dictionary d = new Hashtable();
	 EventAdmin eventAdmin;
	 int i = 0;

	public static WhiteboardImpl INSTANCE;
	
    private WhiteboardImpl(EventAdmin ea) {
    	eventAdmin = ea;
	}
    public WhiteboardImpl()
    {
    }
    
    public WhiteboardImpl getInstance(){
    	return INSTANCE;
    }
    
	public void setValue(String key, Object value){
		Dictionary props = new Hashtable();
		i++;
		System.out.println("Whiteboard contains: " + i);
		props.put(key, value);
		d.put(key, value);
		Event evt = new Event("com/gstm/test/scr/scenarios/util/Whiteboard", props);
		eventAdmin.postEvent(evt);
	}
	
	public Object getValue(String key){
		return d.get(key);// can this generate an exception?
	}
	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		EventAdmin ea = null;
		ServiceReference serviceReference = context.getServiceReference(EventAdmin.class.getName());
		if(serviceReference != null){
			ea = (EventAdmin) context.getService(serviceReference);
		}
		if(ea != null){
			INSTANCE = new WhiteboardImpl(ea);
		}
	}
	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		eventAdmin = null;
		INSTANCE = null;
		context = null;
		d = null;
	}

}
