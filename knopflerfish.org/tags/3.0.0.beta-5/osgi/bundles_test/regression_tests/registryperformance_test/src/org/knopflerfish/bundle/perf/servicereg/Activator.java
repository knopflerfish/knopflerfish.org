package org.knopflerfish.bundle.perf.servicereg;

import org.osgi.framework.*;

import java.util.*;
import org.knopflerfish.service.perf.servicereg.*;

import junit.framework.*;

public class Activator implements BundleActivator {

  static BundleContext bc;
  public void start(BundleContext bc) {
    {
      this.bc = bc;
      TestSuite suite = new PerformanceRegistryTestSuite(bc);
      Hashtable props = new Hashtable();
      props.put("service.pid", suite.getName());
      ServiceRegistration sr 
	= bc.registerService(TestSuite.class.getName(), suite, props);
    }
  }
      
  public void stop(BundleContext bc) {
    this.bc = null;
  }
}

