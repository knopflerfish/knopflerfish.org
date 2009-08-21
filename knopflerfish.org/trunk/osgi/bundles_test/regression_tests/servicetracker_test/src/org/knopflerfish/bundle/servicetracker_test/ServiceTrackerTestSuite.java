/*
 * Copyright (c) 2004-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.servicetracker_test;

import java.util.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;
import junit.framework.*;
import org.osgi.util.tracker.*;


public class ServiceTrackerTestSuite extends TestSuite {
  BundleContext bc;
  Bundle bu;

  // Service tracker test bundles
  Bundle buS;

  ServiceTracker st1 = null;

  PrintStream out = System.out;

  public ServiceTrackerTestSuite(BundleContext bc) {
    super ("ServiceTrackerTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new St010a());
    addTest(new Cleanup());
  }

  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }

  public final static String USAGE_ST010A = "";
  public final static String [] HELP_ST010A =  {
    "Test of Service Tracker class"
  };

  class St010a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean pass = true;

      // load and start bundleS_test, that may be prodded to
      // register/unregister some services. At start it registers one
      // service, com.gatespace.service.bundleS_test.BundleS

      buS = null;

      boolean teststatus = true;
      try {
        buS = Util.installBundle(bc, "bundleS_test-1.0.0.jar");
        buS.start();
        teststatus = true;
        out.println("started service bundle");
      }
      catch (BundleException bexcS) {
        teststatus = false;
        fail("Frame test bundle "+ bexcS +" :SERVICETRACKER010A:FAIL");
      }
      catch (SecurityException secS) {
        teststatus = false;
        fail("Frame test bundle "+ secS +" :SERVICETRACKER010A:FAIL");
      }

      // 1. Create a ServiceTracker with ServiceTrackerCustomizer == null

      String s1 = "org.knopflerfish.service.bundleS_test.BundleS";
      ServiceReference servref = bc.getServiceReference(s1+"0");

      assertNotNull("Must have a registered service of class " + s1+"0",
                    servref);

      st1 = new ServiceTracker(bc, servref, null);

      // 2. Check the size method with an unopened service tracker

      if (st1.size() != 0) {
        pass = false;
        fail("size method returned " + st1.size() +" , expected 0 :SERVICETRACKER010A:FAIL");
      }

      // 3. Open the service tracker and see what it finds,
      // expect to find one instance of the implementation,
      // "org.knopflerfish.bundle.bundleS_test.BundS"

      st1.open();
      String expName  = "org.knopflerfish.bundle.bundleS_test.BundS";
      ServiceReference sa2[] = st1.getServiceReferences();

      if (sa2.length == 1) {
        String name = bc.getService(sa2[0]).getClass().getName();
        if (!name.equals(expName)) {
          pass = false;
          fail("Service referenced class name: got " + name + " expected " + expName + " :SERVICETRACKER010A:FAIL");
        }
        // fail("Class name: " + name);
      } else {
        fail("Got " + sa2.length + " service references, expected 1 :SERVICETRACKER010A:FAIL");
        pass = false;
      }

      // 4. Check the size method, now when the servicetracker is open
      if (st1.size() != 1) {
        pass = false;
        fail("size method returned " + st1.size() +" , expected 1 :SERVICETRACKER010A:FAIL");
      }

      // 5. Close this service tracker
      st1.close();

      // 6. Check the size method, now when the servicetracker is closed
      if (st1.size() != 0) {
        pass = false;
        fail("size method returned " + st1.size() +" , expected 0, after service tracker close() :SERVICETRACKER010A:FAIL");
      }

      // 7. Check if we still track anything , we should get null
      sa2 = st1.getServiceReferences();
      if (sa2 != null) {
        fail("Service references still tracked: got " + sa2 + " expected null :SERVICETRACKER010A:FAIL");
      }

      // 8. A new Servicetracker, this time with a filter for the object
      String fs = "(" + Constants.OBJECTCLASS + "=" + s1 + "*" + ")";
      Filter f1 = null;
      try {
        f1 = bc.createFilter(fs);
      }
      catch (InvalidSyntaxException ise ) {
        pass = false;
        fail("Illegal filter syntax, Frame test bundle :SERVICETRACKER010A:FAIL");
      }

      st1 = new ServiceTracker(bc, f1, null);
      // add a service
      serviceControl (servref, "1", "register",  "7");

      // 9. Open the service tracker and see what it finds,
      // expect to find two instances of references to
      // "org.knopflerfish.bundle.bundleS_test.BundS"
      // i.e. they refer to the same piece of code

      st1.open();
      sa2 = st1.getServiceReferences();

      if (sa2.length == 2) {
        for (int i = 0;  i < sa2.length ; i++) {
          String name = bc.getService(sa2[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        fail("Got " + sa2.length + " service references, expected 2 :SERVICETRACKER010A:FAIL");
        pass = false;
      }

      // 10. Get bundleS to register one more service and see if it appears
      serviceControl (servref, "2", "register", "1");

      sa2 = st1.getServiceReferences();

      if (sa2.length == 3) {
        for (int i = 0;  i < sa2.length ; i++) {
          String name = bc.getService(sa2[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        fail("Got " + sa2.length + " service references, expected 3 :SERVICETRACKER010A:FAIL");
        pass = false;
      }

      // 11. Get bundleS to register one more service and see if it appears
      serviceControl (servref, "3", "register", "2");

      sa2 = st1.getServiceReferences();

      if (sa2.length == 4) {
        for (int i = 0;  i < sa2.length ; i++) {
          String name = bc.getService(sa2[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        fail("Got " + sa2.length + " service references, expected 4 :SERVICETRACKER010A:FAIL");
        pass = false;
      }

      // 12. Get bundleS to unregister one service and see if it disappears
      serviceControl (servref, "3", "unregister", "0");

      sa2 = st1.getServiceReferences();

      if (sa2.length == 3) {
        for (int i = 0;  i < sa2.length ; i++) {
          String name = bc.getService(sa2[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " after service unregistration :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        fail("Got " + sa2.length + " service references, expected 3 :SERVICETRACKER010A:FAIL");
        pass = false;
      }

      // 13. Get the highest ranking service reference, it should have ranking 7

      ServiceReference h1 = st1.getServiceReference();
      Integer rank = (Integer) h1.getProperty(Constants.SERVICE_RANKING);
      if (rank.intValue() != 7) {
        fail("Service rank was: " + rank.toString() + " expected 7 :SERVICETRACKER010A:FAIL");
        pass = false;
      }

      // 14. Get the service of the highest ranked service reference

      Object o1 = st1.getService(h1);
      if (o1 == null ) {
        pass = false;
        fail("Attempt to get service object " + o1 + " failed  :SERVICETRACKER010A:FAIL");
      }

      // 14a Get the highest ranked service, directly this time

      Object o3 = st1.getService();
      if (o3 == null ) {
        pass = false;
        fail("Attempt to get service object " + o3 + " failed  :SERVICETRACKER010A:FAIL");
      } else {
        if (o1 != o3) {
          pass = false;
          fail("The two methods to get the highest ranked service differ:" + o1 + o3 + "  :SERVICETRACKER010A:FAIL");
        }
      }

      // 15. Now release the tracking of that service and then try to get it
      //     from the servicetracker, which should yield a null object

      serviceControl (servref, "1", "unregister",  "7");

      Object o2 = st1.getService(h1);
      if (o2 != null ) {
        pass = false;
        fail("Attempt to get service object " + o2 + " succeded unexpectedly  :SERVICETRACKER010A:FAIL");
      }

      // 16. Get all service objects this tracker tracks, it should be 2

      Object [] ts1 = st1.getServices();
      if (ts1 != null) {
        if (ts1.length != 2) {
          pass = false;
          fail("Expected 2 objects, got: " + ts1.length + " :SERVICETRACKER010A:FAIL" );
        }
      } else {
        pass = false;
        fail("No arry of tracked services found :SERVICETRACKER010A:FAIL" );
      }

      // 17. Test the remove method.
      //     First register another service, then remove it being tracked

      serviceControl (servref, "1", "register",  "7");
      h1 = st1.getServiceReference();

      ServiceReference [] sa3 = st1.getServiceReferences();

      if (sa3.length != 3) {
        for (int i = 0;  i < sa3.length ; i++) {
          String name = bc.getService(sa3[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " after service unregistration :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        // out.println("Got " + sa3.length + " service references, expected 3 :SERVICETRACKER010A:FAIL");
      }

      st1.remove(h1);           // remove tracking on one servref

      sa2 = st1.getServiceReferences();

      if (sa2.length != 2) {
        for (int i = 0;  i < sa2.length ; i++) {
          String name = bc.getService(sa2[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " after service unregistration :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        // out.println("Got " + sa2.length + " service references, expected 2 :SERVICETRACKER010A:FAIL");
      }


      // 18. Test the addingService method,add a service reference

      st1.addingService(h1);

      sa3 = st1.getServiceReferences();

      if (sa3.length != 3) {
        for (int i = 0;  i < sa3.length ; i++) {
          String name = bc.getService(sa3[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " after service unregistration :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        // out.println("Got " + sa3.length + " service references, expected 3 :SERVICETRACKER010A:FAIL");
      }

      // 19. Test the removedService method, remove a service reference

      ServiceReference sr3 = st1.getServiceReference();
      Object s5 = st1.getService(sr3);

      st1.removedService(sr3, s5);

      sa3 = st1.getServiceReferences();

      if (sa3.length != 3) {
        for (int i = 0;  i < sa3.length ; i++) {
          String name = bc.getService(sa3[i]).getClass().getName();
          if (!name.equals(expName)) {
            pass = false;
            fail("Service referenced class name: got " + name + " expected " + expName + " after call of removedService() :SERVICETRACKER010A:FAIL");
          }
        }
      } else {
        // out.println("Got " + sa3.length + " service references, expected 3 in after call of removedService() :SERVICETRACKER010A:FAIL");
      }

      // 20. Test the waitForService method

      Object o9 = null;
      try {
        o9 = st1.waitForService(50);
      }
      catch (InterruptedException ie) {
        pass = false;
        fail("Got unexpected " + ie + " in waitForService method :SERVICETRACKER010A:FAIL");
      }
      if (o9 == null) {
        pass = false;
        fail("Got null object from waitForService method :SERVICETRACKER010A:FAIL");
      }

      if (pass == true) {
        out.println("### Frame test bundle :ST010A:PASS");
      } else {
        fail("### Frame test bundle :ST010A:FAIL");
      }
    }
  }


  // General status check functions


  // to access test service methods via reflection
  // service registration control

  private void serviceControl(ServiceReference sr, String service, String operation, String rank) {
    Method m;
    Class c, parameters[];

    Object obj1 = bc.getService(sr);
    // System.out.println("servref  = "+ sr);
    // System.out.println("object = "+ obj1);

    Object[] arguments = new Object[3];
    arguments[0] = service;     // the service to manipulate
    arguments[1] = operation;   // the operation to do
    arguments[2] = rank;        // the rank of the service

    c = obj1.getClass();
    parameters = new Class[3];
    parameters[0] = arguments[0].getClass();
    parameters[1] = arguments[1].getClass();
    parameters[2] = arguments[2].getClass();

    // System.out.println("Parameters [0] " + parameters[0].toString());

    try {
      m = c.getMethod("controlService", parameters);
      m.invoke(obj1, arguments);
    }
    catch (IllegalAccessException ia) {
      System.out.println("Framework test IllegaleAccessException" +  ia);
    }
    catch (InvocationTargetException ita) {
      System.out.println("Framework test InvocationTargetException" +  ita);
      System.out.println("Framework test nested InvocationTargetException" +  ita.getTargetException() );
    }
    catch (NoSuchMethodException nme) {
      System.out.println("Framework test NoSuchMethodException " +  nme);
      nme.printStackTrace();
    }
    catch (Throwable thr) {
      System.out.println("Unexpected " +  thr);
      thr.printStackTrace();
    }
  }

}
