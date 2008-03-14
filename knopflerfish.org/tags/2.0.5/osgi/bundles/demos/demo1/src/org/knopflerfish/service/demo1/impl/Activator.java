/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.service.demo1.impl;

import java.util.Hashtable;
import org.osgi.framework.*;
import org.knopflerfish.service.demo1.*;


/**
 * Activator which creates and registers a Demo1 service.
 */
public class Activator implements BundleActivator {
  
  private Demo1Impl demo1;
  
  public void start(BundleContext bc) {
    System.out.println("start " + getClass().getName());

    demo1 = new Demo1Impl();

    bc.registerService(Demo1.class.getName(), 
		       demo1, 
		       new Hashtable());

    // Create a service factory for DemoFactory1 implementations
    ServiceFactory factory = new ServiceFactory() {
	Hashtable services = new Hashtable();

	// Will get called when a bundle request a service
	public Object getService(Bundle b, 
				 ServiceRegistration reg) {
	  System.out.println("get from " + b.getBundleId());

	  
	  // Create when necessary
	  DemoFactory1 impl = (DemoFactory1)services.get(b);
	  if(impl == null) {
	    impl = new DemoFactory1Impl(b);
	    services.put(b, impl);
	  }
	  return impl;
	  
	}

	// will get called when a bundle ungets a service or stops
	public void ungetService(Bundle b, 
				 ServiceRegistration reg,
				 Object service) {
	  System.out.println("unget from " + b.getBundleId());
	  services.remove(b);
	}
      };
    
    // Note how factory only implements ServiceFactory, 
    // but we still register as DemoFactory1 service
    bc.registerService(DemoFactory1.class.getName(), 
		       factory, 
		       new Hashtable());
    
  }

  public void stop(BundleContext bc) {
    System.out.println("stop " + getClass().getName());

    demo1 = null;
  }
}
