package org.knopflerfish.bundle.perf.servicereg;

import org.osgi.framework.*;

import java.util.*;
import org.knopflerfish.service.perf.servicereg.*;

import junit.framework.*;

public class PerformanceRegistryTestSuite extends TestSuite {
  BundleContext bc;

  int nListeners = 100;
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


  public class Setup extends TestCase {
    public String getName() {
      return "Setup";
    }

    public String getDescription() {
      return "Initialize event counters";
    }

    public void runTest() throws Throwable {
      nRegistered    = 0;
      nUnregistering = 0;
      nModified      = 0;
    }
  }
  
  public class Cleanup extends TestCase {
    public String getName() {
      return "Cleanup";
    }

    public String getDescription() {
      return "Remove all service listeners";
    }


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

  public class AddListeners extends TestCase {

    public String getName() {
      return "AddListeners";
    }

    public String getDescription() {
      return "Add " + nListeners + " service listeners with a filter";
    }
    
    public void runTest() throws Throwable {
      addListeners(nListeners);
    }
  }

  public class RegisterServices extends TestCase {
    public String getName() {
      return "ModifyServices";
    }

    public String getDescription() {
      return "Register services, and check that we get #of services (" + 
	nServices + ") * #of listeners (" + nListeners + ")  REGISTERED events (" + time + "ms)";
    }
    
    long time = 0;
    public void runTest() throws Throwable {
      long start = System.currentTimeMillis();
      registerServices(nServices);
      time  = System.currentTimeMillis() - start;
      log("register took "  + time + "ms");
      assertEquals("# REGISTERED events must be same as # of registered services  * # of listeners",
		   nServices * listeners.size(), nRegistered);
    }
  }

  public class ModifyServices extends TestCase {
    public String getName() {
      return "ModifyServices";
    }

    public String getDescription() {
      return "Modify all services, and check that we get #of services (" + 
	nServices + ") * #of listeners (" + nListeners + ")  MODIFIED " + 
	" events (" + time + "ms)";
    }
    
    long time = 0;
    public void runTest() throws Throwable {
      long start = System.currentTimeMillis();
      modifyServices();
      time = System.currentTimeMillis() - start;
      log("modify took "  + time + "ms");
      assertEquals("# MODIFIED events must be same as # of modified services  * # of listeners",
		   nServices * listeners.size(), nModified);
    }
  }

  public class UnregisterServices extends TestCase {
    public String getName() {
      return "UnregisterServices";
    }

    public String getDescription() {
      return "Unregister all services, and check that we get #of services (" + 
	nServices + ") * #of listeners (" + nListeners + ")  UNREGISTERING events (" + time + "ms)";
    }

    long time = 0;
    public void runTest() throws Throwable {
      long start = System.currentTimeMillis();
      unregisterServices();
      time = System.currentTimeMillis() - start;
      log("unregister took "  + time + "ms");
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

