/*
 * Created on Mon Aug 15 12:29:17 CEST 2005
 */
package com.gstm.test.scr.scenarios.scenario1.bundle2;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

import com.gstm.test.scr.scenarios.scenario1.bundle1.Scenario1;

public class Activator implements BundleActivator {
  
  public void start(BundleContext context) throws Exception {
  	System.out.println("Starting Scenario1Bundle2");
//  	ServiceReference servicerReference = context.getServiceReference(ComponentFactory.class.getName());
  	ServiceReference servicerReference = context.getServiceReference("org.osgi.service.component.ComponentFactory");
  	ComponentFactory factory = (ComponentFactory) context.getService(servicerReference);
  	
  	ComponentInstance componentInstance = factory.newInstance(new Hashtable());
	Scenario1 service = (Scenario1) componentInstance.getInstance();
  	System.out.println("Scenario1Bundle2 has received the value:" + service.getValue());
  }

  public void stop(BundleContext context) throws Exception {
  }
}