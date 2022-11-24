/*
 * Copyright (c) 2004-2022, KNOPFLERFISH project
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

    @SuppressWarnings("unused")
    public String getDescription() {
      return "Initialize event counters";
    }

    public void runTest() {
      nRegistered    = 0;
      nUnregistering = 0;
      nModified      = 0;
    }
  }
  
  public class Cleanup extends TestCase {
    public String getName() {
      return "Cleanup";
    }

    @SuppressWarnings("unused")
    public String getDescription() {
      return "Remove all service listeners";
    }


    public void runTest() {
      for(int i = 0; i < listeners.size(); i++) {
	try {
	  ServiceListener l = listeners.elementAt(i);
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

    @SuppressWarnings("unused")
    public String getDescription() {
      return "Add " + nListeners + " service listeners with a filter";
    }
    
    public void runTest() {
      addListeners(nListeners);
    }
  }

  public class RegisterServices extends TestCase {
    public String getName() {
      return "ModifyServices";
    }

    @SuppressWarnings("unused")
    public String getDescription() {
      return "Register services, and check that we get #of services (" + 
	nServices + ") * #of listeners (" + nListeners + ")  REGISTERED events (" + time + "ms)";
    }
    
    long time = 0;
    public void runTest() {
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

    @SuppressWarnings("unused")
    public String getDescription() {
      return "Modify all services, and check that we get #of services (" + 
	nServices + ") * #of listeners (" + nListeners + ")  MODIFIED " + 
	" events (" + time + "ms)";
    }
    
    long time = 0;
    public void runTest() {
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

    @SuppressWarnings("unused")
    public String getDescription() {
      return "Unregister all services, and check that we get #of services (" + 
	nServices + ") * #of listeners (" + nListeners + ")  UNREGISTERING events (" + time + "ms)";
    }

    long time = 0;
    public void runTest() {
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
      ServiceListener l = ev -> {
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

  Vector<ServiceRegistration<TestService>> regs = new Vector<>();
  Vector<ServiceListener> listeners = new Vector<>();

  void unregisterServices() {
    log("unregistering " + regs.size() + " services, listener count=" + listeners.size());
    for(int i = 0; i < regs.size(); i++) {
      ServiceRegistration<TestService> reg = regs.elementAt(i);
      reg.unregister();
    }
    regs = new Vector<>();
  }

  void modifyServices() {
    log("modifying " + regs.size() + " services, listener count=" + listeners.size());

    for(int i = 0; i < regs.size(); i++) {
      ServiceRegistration<TestService> reg = regs.elementAt(i);
      Hashtable<String, Object> props = new Hashtable<>();
      props.put("perf.service.value", i * 2);
      
      reg.setProperties(props);
    }
  }

  void registerServices(int n) {

    log("registering " + n + " services, listener count=" + listeners.size());

    for(int i = 0; i < n; i++) {
      Hashtable<String, Object> props = new Hashtable<>();
      props.put("service.pid", "my.service." + i);
      props.put("perf.service.value", "" + (i + 1));

      TestService service = new TestService() { };
      ServiceRegistration<TestService> reg =
	bc.registerService(TestService.class, service, props);
      regs.addElement(reg);
    }
  }

  static void log(String msg) {
    System.out.println("registry_perf: " + msg);
  }
}

