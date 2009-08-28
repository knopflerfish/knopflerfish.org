/*
 * Copyright (c) 2004, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.service.demo1client.impl;

import org.osgi.framework.*;
import org.knopflerfish.service.demo1.*;


/**
 * Activator which registers a listener for, and test, any Demo1 service.
 */
public class Activator implements BundleActivator {
  
  static BundleContext bc;

  ServiceListener listener;

  public void start(BundleContext bc) {
    System.out.println("start " + getClass().getName());

    this.bc = bc;

    
    // create the listener as a new anonymous implementation
    // of org.osgi.framework.ServiceListener
    listener = 
      new ServiceListener() {
	  public void serviceChanged(ServiceEvent ev) {

	    ServiceReference sr = ev.getServiceReference();

	    // just print some info and call testService() on
	    // all registered Demo1 services
	    switch(ev.getType()) {
	    case ServiceEvent.REGISTERED: 
	      System.out.println("Got demo1 service");
	      testService(sr);
	      break;
	    case ServiceEvent.UNREGISTERING: 
	      System.out.println("Lost demo1 service");
	      break;
	    case ServiceEvent.MODIFIED: 
	      System.out.println("Modified demo1 service");
	      break;
	    default:
	      break;
	    }
	    
	  }
	};

    try {

      // Filter that matches all Demo1 services
      String filter = "(objectclass=" + Demo1.class.getName() + ")";
      
      // Fetch all registered Demo1 services
      // and test them manually
      ServiceReference[] srl = 
	bc.getServiceReferences(null, filter);
   
      for(int i = 0; srl != null && i < srl.length; i++) {
	testService(srl[i]);
      }

      // ...and catch all newly registed ones too.
      bc.addServiceListener(listener, filter);
      
    } catch (Exception e) {
      // sounds unlikely, but filter syntax errors are easy to write.
      e.printStackTrace();
    }


    testServiceFactory();
  }

  void  testServiceFactory() {
    // Try to get a reference to the service produced by a factory
    ServiceReference factorySR = bc.getServiceReference(DemoFactory1.class.getName());
    if(factorySR != null) {
      DemoFactory1 df = (DemoFactory1)bc.getService(factorySR);
      if(df != null) {
	// Different bundles will get different printouts
	df.hello();
      }
    }
  }

  
  void testService(ServiceReference sr) {
    Demo1 demo1 = (Demo1)bc.getService(sr);
    
    int r = demo1.add(7, 11);

    System.out.println("Testing " + demo1 + ", result=" + r);

    // ..return the object to be nice
    bc.ungetService(sr);
  }

  public void stop(BundleContext bc) {
    System.out.println("stop " + getClass().getName());

    this.bc = null;
  }
}
