package org.knopflerfish.bundle.perf.servicereg;

import org.osgi.framework.*;

import java.util.*;
import org.knopflerfish.service.perf.servicereg.*;

import junit.framework.*;

public class PerformanceRegistryTestSuite extends TestSuite {
  BundleContext bc;

  int nListeners = 500;
  int nServices  = 1000;

  public PerformanceRegistryTestSuite(BundleContext bc) {
    super("PerformanceRegistryTestSuite");
    this.bc = bc;

    try {
      nListeners = Integer.parseInt(System.getProperty("org.knopflerfish.bundle.perf.servicereg.nlisteners", "" + nListeners));
    } catch (Exception ignored) {
    }

    try {
      nServices = Integer.parseInt(System.getProperty("org.knopflerfish.bundle.perf.servicereg.nservices", "" + nServices));
    } catch (Exception ignored) {
    }

    addTest(new Setup());
    addTest(new AddListeners());
    addTest(new RegisterServices());
    addTest(new ModifyServices());
    addTest(new UnregisterServices());
    addTest(new Cleanup());
  }

  int nRegistered    = 0;
  int nUnregistering = 0;
  int nModified      = 0;


  class Setup extends TestCase {
    public void runTest() throws Throwable {
      nRegistered    = 0;
      nUnregistering = 0;
      nModified      = 0;
    }
  }
  
  class Cleanup extends TestCase {
    public void runTest() throws Throwable {
      for(int i = 0; i < listeners.size(); i++) {
	try {
	  ServiceListener l = (ServiceListener)listeners.elementAt(i);
	  bc.removeServiceListener(l);
	} catch (Exception e) {
	  e.printStackTrace();
	}
      }
    }
  }

  class AddListeners extends TestCase {
    public void runTest() throws Throwable {
      addListeners(nListeners);
    }
  }

  class RegisterServices extends TestCase {
    public void runTest() throws Throwable {
      long start = System.currentTimeMillis();
      registerServices(nServices);
      long stop  = System.currentTimeMillis();
      log("register took "  + (stop - start) + "ms");
      assertEquals("# REGISTERED events must be same as # of registered services  * # of listeners",
		   nServices * listeners.size(), nRegistered);
    }
  }

  class ModifyServices extends TestCase {
    public void runTest() throws Throwable {
      long start = System.currentTimeMillis();
      modifyServices();
      long stop  = System.currentTimeMillis();
      log("modify took "  + (stop - start) + "ms");
      assertEquals("# MODIFIED events must be same as # of modified services  * # of listeners",
		   nServices * listeners.size(), nModified);
    }
  }

  class UnregisterServices extends TestCase {
    public void runTest() throws Throwable {
      long start = System.currentTimeMillis();
      unregisterServices();
      long stop  = System.currentTimeMillis();
      log("unregister took "  + (stop - start) + "ms");
      assertEquals("# UNREGISTERING events must be same as # of (un)registered services * # of listeners",
		   nServices * listeners.size(), nUnregistering);

    }
  }


  void addListeners(int n) {
    log("adding " + n + " service listeners");
    for(int i = 0; i < n; i++) {
      ServiceListener l = new ServiceListener() {
	  public void serviceChanged(ServiceEvent ev) {
	    switch(ev.getType()) {
	    case ServiceEvent.REGISTERED:
	      nRegistered++;
	      break;
	    case ServiceEvent.UNREGISTERING:
	      nUnregistering++;
	      break;
	    case ServiceEvent.MODIFIED:
	      nModified++;
	      break;
	    }
	  }
	};
      
      try {
	listeners.addElement(l);
	bc.addServiceListener(l, "(perf.service.value>=0)");
      } catch (Exception e) {
	e.printStackTrace();
      }
    }
    log("listener count=" + listeners.size());
  }

  Vector regs = new Vector();
  Vector listeners = new Vector();

  void unregisterServices() {
    log("unregistering " + regs.size() + " services, listener count=" + listeners.size());
    for(int i = 0; i < regs.size(); i++) {
      ServiceRegistration reg = (ServiceRegistration)regs.elementAt(i);
      reg.unregister();
    }
    regs = new Vector();
  }

  void modifyServices() {
    log("modifying " + regs.size() + " services, listener count=" + listeners.size());

    for(int i = 0; i < regs.size(); i++) {
      ServiceRegistration reg = (ServiceRegistration)regs.elementAt(i);
      Hashtable props = new Hashtable();
      props.put("perf.service.value", new Integer(i * 2));
      
      reg.setProperties(props);
    }
  }

  void registerServices(int n) {

    log("registering " + n + " services, listener count=" + listeners.size());

    for(int i = 0; i < n; i++) {
      Hashtable props = new Hashtable();
      props.put("service.pid", "my.service." + i);
      props.put("perf.service.value", "" + new Integer(i+1));
      
      Object service = new TestService() { };
      ServiceRegistration reg = 
	bc.registerService(TestService.class.getName(), service, props);
      regs.addElement(reg);
    }
  }

  static void log(String msg) {
    System.out.println("registry_perf: " + msg);
  }
}

