/*
 * Copyright (c) 2007-2009 KNOPFLERFISH project
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

package org.knopflerfish.bundle.framework_test;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;
import org.knopflerfish.service.framework_test.*;

import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.*;

import junit.framework.*;

public class ServiceListenerTestSuite
  extends TestSuite
  implements FrameworkTest
{
  BundleContext bc;
  Bundle bu;

  // A bundle that publishes one service, used to trigger the service
  // listener events.
  Bundle buA;
  Bundle buA2;
  Bundle buSL1;
  Bundle buSL2;
  Bundle buSL3;
  Bundle buSL4;

  PrintStream out = System.out;
  PrintStream err = System.err;

  final static String TRUE   = "true";
  final static String FALSE  = "false";

  // If set to true, then during the UNREGISTERING event the Listener
  // can use the ServiceReference to receive an instance of the service.
  final static boolean UNREGISTERSERVICE_VALID_DURING_UNREGISTERING
    = TRUE.equals(System.getProperty
           ("org.knopflerfish.servicereference.valid.during.unregistering",
            TRUE));


  public ServiceListenerTestSuite (BundleContext bc)
  {
    super("ServiceListenerTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new FrameSL05a());
    addTest(new FrameSL10a());
    addTest(new FrameSL15a());
    addTest(new FrameSL20a());
    addTest(new FrameSL25a());
    addTest(new Cleanup());
  }


  class FWTestCase
    extends TestCase
  {
    public String getName() {
      String name = getClass().getName();
      int ix = name.lastIndexOf("$");
      if(ix == -1) {
        ix = name.lastIndexOf(".");
      }
      if(ix != -1) {
        name = name.substring(ix + 1);
      }
      return name;
    }
  }

  /**
   * Install test target bundles.
   */
  class Setup
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "bundleA_test-1.0.0.jar");
      assertNotNull(buA);
      buA2 = Util.installBundle(bc, "bundleA2_test-1.0.0.jar");
      assertNotNull(buA2);
      buSL1 = Util.installBundle(bc, "bundleSL1-1.0.0.jar");
      assertNotNull(buSL1);
      buSL2 = Util.installBundle(bc, "bundleSL2-1.0.0.jar");
      assertNotNull(buSL2);
      buSL3 = Util.installBundle(bc, "bundleSL3-1.0.0.jar");
      assertNotNull(buSL3);
      buSL4 = Util.installBundle(bc, "bundleSL4-1.0.0.jar");
      assertNotNull(buSL4);
      out.println("org.knopflerfish.servicereference.valid.during.unregistering"
                  +" is " +UNREGISTERSERVICE_VALID_DURING_UNREGISTERING);
      out.println("### ServiceListenerTestSuite :SETUP:PASS");
    }
  }

  /**
   * Uninstall test target bundles.
   */
  class Cleanup
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
        buA,
        buA2,
        buSL1,
        buSL2,
        buSL3,
        buSL4,
      };
      for(int i = 0; i < bundles.length; i++) {
        try {  bundles[i].uninstall();  }
        catch (Exception ignored) { }
      }

      buA = null;
      buA2 = null;
      buSL1 = null;
      buSL2 = null;
      buSL3 = null;
      buSL4 = null;
    }
  }

  public final static String [] HELP_FRAMESL05A =  {
    "Checks that the correct service events",
    "are sent to a registered service listener.",
    "Case where the bundle does not unregisters its",
    "service in the stop()-method."
  };

  class FrameSL05a
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      int cnt = 1;

      teststatus = runStartStopTest( "FRAMEsl05A", cnt, buA,
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     },
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     } );

      if (teststatus == true) {
        out.println("### ServiceListenerTestsuite :FRAMEsl05A:PASS");
      }
      else {
        fail("### ServiceListenerTestsuite :FRAMEsl05A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAMESL10A =  {
    "Checks that the correct service events",
    "are sent to a registered service listener.",
    "Case where the bundle unregisters its service",
    "in the stop()-method."
  };


  class FrameSL10a
    extends FWTestCase
  {
    public void runTest() throws Throwable {
      int cnt = 1;

      assertTrue(runStartStopTest( "FRAMEsl10A", cnt, buA2,
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     },
                                     new int[]{
                                       ServiceEvent.REGISTERED,
                                       ServiceEvent.UNREGISTERING,
                                     }));
      out.println("### ServiceListenerTestsuite :FRAMEsl10A:PASS");
    }
  }



  public final static String [] HELP_FRAMESL15A =  {
    "Checks that the correct service events",
    "are sent to a registered service listener.",
    "This case checks ServiceReference.isAssignableTo(..)",
    "after that the package providing bundle have been updated.",
    "buSL2 should not be informed about the FooService when it",
    "restarts after the update since classes are incompatible."
  };


  class FrameSL15a
    extends FWTestCase
  {
    public void runTest() throws Throwable {

      ServiceListener sListen = new ServiceListener();
      try {
        bc.addServiceListener(sListen);
      } catch (IllegalStateException ise) {
        err.println("service listener registration failed "+ ise);
        ise.printStackTrace(err);
        fail("service listener registration failed "+ ise);
      }
      AllServiceListener asListen = new AllServiceListener();
      try {
        bc.addServiceListener(asListen);
      } catch (IllegalStateException ise) {
        err.println("all service listener registration failed "+ ise);
        ise.printStackTrace(err);
        fail("all service listener registration failed "+ ise);
      }

      int[] expectedServiceEventTypes = new int[]{
        // Startup
        ServiceEvent.REGISTERED,      // Activator at start of buSL1
        ServiceEvent.REGISTERED,      // FooService at start of buSL2

        // Update buSL1
        ServiceEvent.UNREGISTERING,   // Activator at update of buSL1
        ServiceEvent.REGISTERED,      // Activator at start of buSL1 after upg

        // Stop/Start buSL2
        ServiceEvent.UNREGISTERING,   // FooService at first stop of buSL2
        ServiceEvent.REGISTERED,      // FooService at re-start of buSL2

        // refreshPackage([buSL1])
        ServiceEvent.UNREGISTERING,   // Activator at refresh
        ServiceEvent.UNREGISTERING,   // FooService at refresh
        ServiceEvent.REGISTERED,      // Activator at re-start after refresh
        ServiceEvent.REGISTERED,      // FooService at re-start after refresh

        // Shutdown
        ServiceEvent.UNREGISTERING,   // Activator at stop of buSL1
        ServiceEvent.UNREGISTERING,   // FooService at stop of buSL2
      };


      // Start buSL1 to ensure that the Service package is available.
      try {
        out.println("Starting buSL1: " +buSL1);
        buSL1.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }


      // Start buSL2 that will import the serivce package and publish FooService
      try {
        out.println("Starting buSL2: " +buSL2);
        buSL2.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }

      // Check that buSL1 has been notified about the FooService.
      out.println("Check that FooService is added to service tracker in buSL1");
      ServiceReference buSL1SR
        = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator");
      assertNotNull("No activator service reference.", buSL1SR);
      Object buSL1Activator = bc.getService(buSL1SR);
      assertNotNull("No activator service.", buSL1Activator);
      Field serviceAddedField
        = buSL1Activator.getClass().getField("serviceAdded");
      assertTrue("", serviceAddedField.getBoolean(buSL1Activator));
      out.println("buSL1Activator.serviceAdded is true");
      bc.ungetService(buSL1SR);
      buSL1Activator = null;
      buSL1SR = null;

      // Update buSL1
      try {
        Util.updateBundle(bc, buSL1, "bundleSL1-1.0.0.jar");
      } catch (BundleException bex) {
        err.println("Failed to update bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to update bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to update bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to update bundle, got exception " + e);
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }
      out.println("buSL1 updated.");


      // Check that the updated buSL1 has NOT been notified about the
      // FooService (packages are incompatible).
      // Check that buSL1 has been notified about the FooService.
      out.println("Check that FooService is not added to service tracker "
                  +"in buSL1 after update.");
      buSL1SR = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator");
      assertNotNull("No activator service reference.", buSL1SR);
      buSL1Activator = bc.getService(buSL1SR);
      assertNotNull("No activator service.", buSL1Activator);
      serviceAddedField = buSL1Activator.getClass().getField("serviceAdded");
      assertFalse("", serviceAddedField.getBoolean(buSL1Activator));
      out.println("buSL1Activator.serviceAdded is false");
      bc.ungetService(buSL1SR);
      buSL1Activator = null;
      buSL1SR = null;


      // Stop buSL2
      try {
        out.println("Stop buSL2: " +buSL2);
        buSL2.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }

      // Start buSL2
      try {
        out.println("Starting buSL2: " +buSL2);
        buSL2.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }

      // Check that the updated buSL1 has NOT been notified about the
      // FooService (packages are still incompatible).
      out.println("Check that FooService is not added to service tracker "
                  +"in buSL1 after update and restart of buSL2.");
      buSL1SR = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator");
      assertNotNull("No activator service reference.", buSL1SR);
      buSL1Activator = bc.getService(buSL1SR);
      assertNotNull("No activator service.", buSL1Activator);
      serviceAddedField = buSL1Activator.getClass().getField("serviceAdded");
      assertFalse("", serviceAddedField.getBoolean(buSL1Activator));
      out.println("buSL1Activator.serviceAdded is false");
      bc.ungetService(buSL1SR);
      buSL1Activator = null;
      buSL1SR = null;

      // Refresh packages
      assertNull(Util.refreshPackages(bc,new Bundle[]{buSL1}));

      // Check that buSL1 has been notified about the FooService.
      out.println("Check that FooService is added to service tracker "
                  +"in buSL1 after package refresh.");
      buSL1SR = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator");
      assertNotNull("No activator service reference.", buSL1SR);
      buSL1Activator = bc.getService(buSL1SR);
      assertNotNull("No activator service.", buSL1Activator);
      serviceAddedField = buSL1Activator.getClass().getField("serviceAdded");
      assertTrue("", serviceAddedField.getBoolean(buSL1Activator));
      out.println("buSL1Activator.serviceAdded is true");
      bc.ungetService(buSL1SR);
      buSL1Activator = null;
      buSL1SR = null;

      // Stop buSL1
      try {
        out.println("Stop buSL1: " +buSL1);
        buSL1.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }

      // Stop buSL2
      try {
        out.println("Stop buSL2: " +buSL2);
        buSL2.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }


      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }

      // Check service events seen by this class (no connection to the
      // service package so we should see all events for the FooService)
      out.println("Checking ServiceEvents(ServiceListener):");
      if (!sListen.checkEvents(expectedServiceEventTypes)) {
        err.println("Service listener event notification error"
                    +":FRAMEsl15A:FAIL");
        fail("Service listener event notification error");
      }

      out.println("Checking ServiceEvents(AllServiceListener):");
      if (!asListen.checkEvents(expectedServiceEventTypes)) {
        err.println("All service listener event notification error :"
                    +":FRAMEsl15A:FAIL");
        fail("Service listener event notification error");
      }

      assertTrue(sListen.teststatus);
      try {
        bc.removeServiceListener(sListen);
        sListen.clearEvents();
      } catch (IllegalStateException ise) {
        fail("service listener removal failed "+ ise);
      }

      assertTrue(asListen.teststatus);
      try {
        bc.removeServiceListener(asListen);
        asListen.clearEvents();
      } catch (IllegalStateException ise) {
        fail("all service listener removal failed "+ ise);
      }
    }
  }


  public final static String [] HELP_FRAMESL20A =  {
    "Checks that the correct service events",
    "are sent to a registered service listener.",
    "This case checks ServiceReference.isAssignableTo(..)",
    "works for bundles that uses the service and gets the",
    "service package via require bundle."
  };


  class FrameSL20a
    extends FWTestCase
  {
    public void runTest() throws Throwable {

      ServiceListener sListen = new ServiceListener(false);
      try {
        bc.addServiceListener(sListen);
      } catch (IllegalStateException ise) {
        err.println("service listener registration failed "+ ise);
        ise.printStackTrace(err);
        fail("service listener registration failed "+ ise);
      }
      AllServiceListener asListen = new AllServiceListener(false);
      try {
        bc.addServiceListener(asListen);
      } catch (IllegalStateException ise) {
        err.println("all service listener registration failed "+ ise);
        ise.printStackTrace(err);
        fail("all service listener registration failed "+ ise);
      }

      int[] expectedServiceEventTypes = new int[]{
        // Startup
        ServiceEvent.REGISTERED,      // Activator at start of buSL1
        ServiceEvent.REGISTERED,      // FooService at start of buSL2
        ServiceEvent.REGISTERED,      // Activator at start of buSL3

        // Stop buSL2
        ServiceEvent.UNREGISTERING,   // FooService at first stop of buSL2

        // Shutdown
        ServiceEvent.UNREGISTERING,   // Activator at stop of buSL1
        ServiceEvent.UNREGISTERING,   // Activator at stop of buSL3
      };


      // Start buSL1 to ensure that the Service package is available.
      try {
        out.println("Starting buSL1: " +buSL1);
        buSL1.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }


      // Start buSL2 that will import the serivce package and publish FooService
      try {
        out.println("Starting buSL2: " +buSL2);
        buSL2.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }

      // Start buSL3 that will import the serivce package and get the service
      try {
        out.println("Starting buSL3: " +buSL3);
        buSL3.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }

      // Check that buSL1 has been notified about the FooService.
      out.println("Check that FooService is added to service tracker in buSL1");
      ServiceReference buSL1SR
        = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator");
      assertNotNull("No activator service reference.", buSL1SR);
      Object buSL1Activator = bc.getService(buSL1SR);
      assertNotNull("No activator service.", buSL1Activator);
      Field serviceAddedField
        = buSL1Activator.getClass().getField("serviceAdded");
      assertTrue("bundleSL1 not notified about presence FooService",
                 serviceAddedField.getBoolean(buSL1Activator));
      out.println("buSL1Activator.serviceAdded is true");
      bc.ungetService(buSL1SR);
      buSL1Activator = null;
      buSL1SR = null;

      // Check that buSL3 has been notified about the FooService.
      out.println("Check that FooService is added to service tracker in buSL3");
      ServiceReference buSL3SR
        = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator3");
      assertNotNull("No activator service reference.", buSL3SR);
      Object buSL3Activator = bc.getService(buSL3SR);
      assertNotNull("No activator service.", buSL3Activator);
      Field serviceAddedField3
        = buSL3Activator.getClass().getField("serviceAdded");
      assertTrue("bundleSL3 not notified about presence FooService",
                 serviceAddedField3.getBoolean(buSL3Activator));
      out.println("buSL3Activator.serviceAdded is true");
      bc.ungetService(buSL3SR);
      buSL3Activator = null;
      buSL3SR = null;

      // Stop the service provider: buSL2
      try {
        out.println("Stop buSL2: " +buSL2);
        buSL2.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }


      // Check that buSL3 has been notified about the removal of FooService.
      out.println("Check that FooService is removed from service tracker in buSL3");
      buSL3SR
        = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator3");
      assertNotNull("No activator service reference.", buSL3SR);
      buSL3Activator = bc.getService(buSL3SR);
      assertNotNull("No activator service.", buSL3Activator);
      Field serviceRemovedField3
        = buSL3Activator.getClass().getField("serviceRemoved");
      assertTrue("bundleSL3 not notified about removal of FooService",
                 serviceRemovedField3.getBoolean(buSL3Activator));
      out.println("buSL3Activator.serviceRemoved is true");
      bc.ungetService(buSL3SR);
      buSL3Activator = null;
      buSL3SR = null;


      // Stop buSL1
      try {
        out.println("Stop buSL1: " +buSL1);
        buSL1.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }


      // Stop buSL3
      try {
        out.println("Stop buSL3: " +buSL3);
        buSL3.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }


      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }

      // Check service events seen by this class (no connection to the
      // service package so we should see all events for the FooService)
      out.println("Checking ServiceEvents(ServiceListener):");
      if (!sListen.checkEvents(expectedServiceEventTypes)) {
        err.println("Service listener event notification error"
                    +":FRAMEsl20A:FAIL");
        fail("Service listener event notification error");
      }

      out.println("Checking ServiceEvents(AllServiceListener):");
      if (!asListen.checkEvents(expectedServiceEventTypes)) {
        err.println("All service listener event notification error :"
                    +":FRAMEsl20A:FAIL");
        fail("Service listener event notification error");
      }

      assertTrue("Service listener checks", sListen.teststatus);
      try {
        bc.removeServiceListener(sListen);
        sListen.clearEvents();
      } catch (IllegalStateException ise) {
        fail("service listener removal failed "+ ise);
      }

      assertTrue("All-service listener checks", asListen.teststatus);
      try {
        bc.removeServiceListener(asListen);
        asListen.clearEvents();
      } catch (IllegalStateException ise) {
        fail("all service listener removal failed "+ ise);
      }
    }
  }



  public final static String [] HELP_FRAMESL25A =  {
    "Checks that the correct service events",
    "are sent to a registered service listener.",
    "This case checks ServiceReference.isAssignableTo(..)",
    "works when the service provider bundle",
    "gets the service package via require bundle."
  };


  class FrameSL25a
    extends FWTestCase
  {
    public void runTest() throws Throwable {

      ServiceListener sListen = new ServiceListener(false);
      try {
        bc.addServiceListener(sListen);
      } catch (IllegalStateException ise) {
        err.println("service listener registration failed "+ ise);
        ise.printStackTrace(err);

        fail("service listener registration failed "+ ise);
      }
      AllServiceListener asListen = new AllServiceListener(false);
      try {
        bc.addServiceListener(asListen);
      } catch (IllegalStateException ise) {
        err.println("all service listener registration failed "+ ise);
        ise.printStackTrace(err);
        fail("all service listener registration failed "+ ise);
      }

      int[] expectedServiceEventTypes = new int[]{
        // Startup
        ServiceEvent.REGISTERED,      // Activator at start of buSL1
        ServiceEvent.REGISTERED,      // FooService at start of buSL4
        ServiceEvent.REGISTERED,      // Activator at start of buSL3

        // Stop buSL4
        ServiceEvent.UNREGISTERING,   // FooService at first stop of buSL4

        // Shutdown
        ServiceEvent.UNREGISTERING,   // Activator at stop of buSL1
        ServiceEvent.UNREGISTERING,   // Activator at stop of buSL3
      };


      // Start buSL1 to ensure that the Service package is available.
      try {
        out.println("Starting buSL1: " +buSL1);
        buSL1.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }


      // Start buSL4 that will require the serivce package and publish
      // FooService
      try {
        out.println("Starting buSL4: " +buSL4);
        buSL4.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }

      // Start buSL3 that will require the serivce package and get the service
      try {
        out.println("Starting buSL3: " +buSL3);
        buSL3.start();
      } catch (BundleException bex) {
        err.println("Failed to start bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to start bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to start bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to start bundle, got exception " + e);
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }

      // Check that buSL3 has been notified about the FooService.
      out.println("Check that FooService is added to service tracker in buSL3");
      ServiceReference buSL3SR
        = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator3");
      assertNotNull("No activator service reference.", buSL3SR);
      Object buSL3Activator = bc.getService(buSL3SR);
      assertNotNull("No activator service.", buSL3Activator);
      Field serviceAddedField3
        = buSL3Activator.getClass().getField("serviceAdded");
      assertTrue("bundleSL3 not notified about presence FooService",
                 serviceAddedField3.getBoolean(buSL3Activator));
      out.println("buSL3Activator.serviceAdded is true");
      bc.ungetService(buSL3SR);
      buSL3Activator = null;
      buSL3SR = null;

      // Check that buSL1 has been notified about the FooService.
      out.println("Check that FooService is added to service tracker in buSL1");
      ServiceReference buSL1SR
        = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator");
      assertNotNull("No activator service reference.", buSL1SR);
      Object buSL1Activator = bc.getService(buSL1SR);
      assertNotNull("No activator service.", buSL1Activator);
      Field serviceAddedField
        = buSL1Activator.getClass().getField("serviceAdded");
      assertTrue("bundleSL1 not notified about presence FooService",
                 serviceAddedField.getBoolean(buSL1Activator));
      out.println("buSL1Activator.serviceAdded is true");
      bc.ungetService(buSL1SR);
      buSL1Activator = null;
      buSL1SR = null;

      // Stop the service provider: buSL4
      try {
        out.println("Stop buSL4: " +buSL4);
        buSL4.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }


      // Check that buSL3 has been notified about the removal of FooService.
      out.println("Check that FooService is removed from service "
                  +"tracker in buSL3");
      buSL3SR
        = bc.getServiceReference("org.knopflerfish.bundle.foo.Activator3");
      assertNotNull("No activator service reference.", buSL3SR);
      buSL3Activator = bc.getService(buSL3SR);
      assertNotNull("No activator service.", buSL3Activator);
      Field serviceRemovedField3
        = buSL3Activator.getClass().getField("serviceRemoved");
      assertTrue("bundleSL3 not notified about removal of FooService",
                 serviceRemovedField3.getBoolean(buSL3Activator));
      out.println("buSL3Activator.serviceRemoved is true");
      bc.ungetService(buSL3SR);
      buSL3Activator = null;
      buSL3SR = null;


      // Stop buSL1
      try {
        out.println("Stop buSL1: " +buSL1);
        buSL1.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }

      // Stop buSL3
      try {
        out.println("Stop buSL3: " +buSL3);
        buSL3.stop();
      } catch (BundleException bex) {
        err.println("Failed to stop bundle, got exception: "
                    +bex.getNestedException());
        bex.printStackTrace(err);
        fail("Failed to stop bundle, got exception: "
             + bex.getNestedException());
      } catch (Exception e) {
        err.println("Failed to stop bundle, got exception " +e);
        e.printStackTrace(err);
        fail("Failed to stop bundle, got exception " + e);
      }


      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception e) {
        err.println("Unexpected excpetion during sleep:" +e);
        e.printStackTrace(err);
      }

      // Check service events seen by this class (no connection to the
      // service package so we should see all events for the FooService)
      out.println("Checking ServiceEvents(ServiceListener):");
      if (!sListen.checkEvents(expectedServiceEventTypes)) {
        err.println("Service listener event notification error"
                    +":FRAMEsl25A:FAIL");
        fail("Service listener event notification error");
      }

      out.println("Checking ServiceEvents(AllServiceListener):");
      if (!asListen.checkEvents(expectedServiceEventTypes)) {
        err.println("All service listener event notification error :"
                    +":FRAMEsl25A:FAIL");
        fail("Service listener event notification error");
      }

      assertTrue("Service listener checks", sListen.teststatus);
      try {
        bc.removeServiceListener(sListen);
        sListen.clearEvents();
      } catch (IllegalStateException ise) {
        fail("service listener removal failed "+ ise);
      }

      assertTrue("All-service listener checks", asListen.teststatus);
      try {
        bc.removeServiceListener(asListen);
        asListen.clearEvents();
      } catch (IllegalStateException ise) {
        fail("all service listener removal failed "+ ise);
      }
    }
  }



  boolean runStartStopTest( String tcName,
                            int cnt,
                            Bundle targetBundle,
                            int[] eventsA,
                            int[] events)
  {
    boolean teststatus = true;

    for (int i = 0; i < cnt && teststatus; i++) {
      ServiceListener sListen = new ServiceListener();
      try {
        bc.addServiceListener(sListen);
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("service listener registration failed "+ ise
                    + " :" +tcName +":FAIL");
      }
      AllServiceListener asListen = new AllServiceListener();
      try {
        bc.addServiceListener(asListen);
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("all service listener registration failed "+ ise
                    + " :" +tcName +":FAIL");
      }

      // Start the test target to get a service published.
      try {
        out.println("Starting targetBundle: "+targetBundle);
        targetBundle.start();
      } catch (BundleException bex) {
        teststatus  = false;
        err.println("Failed to start bundle, got exception: "
                    + bex.getNestedException()
                    + " in " +tcName +":FAIL");
      } catch (Exception e) {
        teststatus  = false;
        e.printStackTrace(err);
        err.println("Failed to start bundle, got exception "
                    + e + " + in " +tcName +":FAIL");
      }

      // sleep to stabelize state.
      try {
        Thread.sleep(300);
      } catch (Exception ex) {
        out.println("### framework test bundle :" +tcName +" exception");
        ex.printStackTrace(out);
      }

      // Stop the test target to get a service unpublished.
      try {
        targetBundle.stop();
      } catch (BundleException bex) {
        teststatus  = false;
        err.println("Failed to stop bundle, got exception: "
                    + bex.getNestedException()
                    + " in " +tcName +":FAIL");
      } catch (Exception e) {
        teststatus  = false;
        e.printStackTrace(err);
        err.println("Failed to stop bundle, got exception "
                    + e + " + in " +tcName +":FAIL");
      }

      if ( teststatus && !sListen.checkEvents(events)) {
        teststatus  = false;
        err.println("Service listener event notification error :"
                    +tcName +":FAIL");
      }

      if ( teststatus && !asListen.checkEvents(eventsA)) {
        teststatus  = false;
        err.println("All service listener event notification error :"
                    +tcName +":FAIL");
      }

      try {
        bc.removeServiceListener(sListen);
        teststatus &= sListen.teststatus;
        sListen.clearEvents();
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("service listener removal failed "+ ise
                    + " :" +tcName +":FAIL");
      }
      try {
        bc.removeServiceListener(asListen);
        teststatus &= asListen.teststatus;
        asListen.clearEvents();
      } catch (IllegalStateException ise) {
        teststatus  = false;
        err.println("all service listener removal failed "+ ise
                    + " :" +tcName +":FAIL");
      }
    }
    return teststatus;
  }


  class ServiceListener
    implements org.osgi.framework.ServiceListener
  {
    final boolean checkUsingBundles;
    final ArrayList events = new ArrayList(10);

    boolean teststatus = true;

    public ServiceListener()
    {
      this(true);
    }
    public ServiceListener(boolean checkUsingBundles)
    {
      this.checkUsingBundles = checkUsingBundles;
    }


    public void serviceChanged(ServiceEvent evt) {
      events.add(evt);
      out.println("ServiceEvent: " +toString(evt) );
      if (ServiceEvent.UNREGISTERING==evt.getType()) {
        ServiceReference sr = evt.getServiceReference();

        // Validate that no bundle is marked as using the service
        Bundle[] usingBundles = sr.getUsingBundles();
        if (checkUsingBundles && null!=usingBundles) {
          teststatus = false;
          printUsingBundles(sr, "*** Using bundles (unreg) should be null "
                            +"but is: ");
        }

        // Check if the service can be fetched
        Object service = bc.getService(sr);
        usingBundles = sr.getUsingBundles();
        if (UNREGISTERSERVICE_VALID_DURING_UNREGISTERING) {
          // In this mode the service shall be obtainable during
          // unregistration.
          if (null==service) {
            teststatus = false;
            out.print("*** Service should be available to ServiceListener "
                      +"while handling unregistering event.");
          }
          out.println("Service (unreg): " +service);
          if (checkUsingBundles && usingBundles.length!=1) {
            teststatus = false;
            printUsingBundles(sr,
                              "*** One using bundle expected "
                              +"(unreg, after getService), found: ");
          } else {
            printUsingBundles(sr, "Using bundles (unreg, after getService): ");
          }
        } else {
          // In this mode the service shall NOT be obtainable during
          // unregistration.
          if (null!=service) {
            teststatus = false;
            out.print("*** Service should not be available to ServiceListener "
                      +"while handling unregistering event.");
          }
          if (checkUsingBundles && null!=usingBundles) {
            teststatus = false;
            printUsingBundles(sr,
                              "*** Using bundles (unreg, after getService), "
                              +"should be null but is: ");
          } else {
            printUsingBundles(sr,
                              "Using bundles (unreg, after getService): null");
          }
        }
        bc.ungetService(sr);

        // Check that the UNREGISTERING service can not be looked up
        // using the service registry.
        try {
          Long sid = (Long)sr.getProperty(Constants.SERVICE_ID);
          String sidFilter = "(" +Constants.SERVICE_ID +"=" +sid +")";
          ServiceReference[] srs = bc.getServiceReferences(null, sidFilter);
          if (null==srs || 0==srs.length) {
            out.println("ServiceReference for UNREGISTERING service is not"
                        +" found in the service registry; ok.");
          } else {
            teststatus = false;
            out.println("*** ServiceReference for UNREGISTERING"
                        +" service, "
                        + sr
                        +", not found in the service registry; fail.");
            out.print("Found the following Service references: ");
            for (int i=0; null!=srs && i<srs.length; i++) {
              if (i>0) out.print(", ");
              out.print(srs[i]);
            }
            out.println();
          }
        } catch (Throwable t) {
          teststatus = false;
          out.println("*** Unexpected excpetion when trying to lookup a"
                      +" service while it is in state UNREGISTERING; "
                      +t);
          t.printStackTrace(out);
        }
      }
    }

    void clearEvents()
    {
      events.clear();
    }

    boolean checkEvents(int[] eventTypes)
    {
      if (events.size() != eventTypes.length) {
        dumpEvents(eventTypes);
        return false;
      }

      for (int i=0; i<eventTypes.length; i++) {
        ServiceEvent evt = (ServiceEvent) events.get(i);
        if (eventTypes[i] != evt.getType() ) {
          dumpEvents(eventTypes);
          return false;
        }
      }
      return true;
    }

    private void printUsingBundles(ServiceReference sr, String caption)
    {
      Bundle[] usingBundles = sr.getUsingBundles();

      out.print(caption!=null ? caption : "Using bundles: ");
      for (int i=0; usingBundles!=null && i<usingBundles.length; i++) {
        if (i>0) out.print(", ");
        out.print(usingBundles[i]);
      }
      out.println();
    }

    String toStringEventType(int eventType)
    {
      String res = String.valueOf(eventType);

      switch (eventType) {
      case ServiceEvent.REGISTERED:    res +=" (REGISTERED)    "; break;
      case ServiceEvent.MODIFIED:      res +=" (MODIFIED)      "; break;
      case ServiceEvent.UNREGISTERING: res +=" (UNREGISTERING) "; break;
      default:                         res +=" (?)             ";
      }
      return res;
    }

    String toString(ServiceEvent evt)
    {
      if (null==evt) return " - NONE - ";

      ServiceReference sr = evt.getServiceReference();
      String[] objectClasses = (String[]) sr.getProperty(Constants.OBJECTCLASS);
      String res = toStringEventType(evt.getType());

      for (int i=0; i<objectClasses.length; i++){
        if (i>0) res += ", ";
        res += objectClasses[i];
      }
      return res;
    }

    // Print expected and actual service events.
    void dumpEvents(int[] eventTypes)
    {
      int max = events.size()> eventTypes.length
        ? events.size() : eventTypes.length;
      out.println("Expected event type --  Actual event");
      for (int i=0; i<max; i++) {
        ServiceEvent evt
          = i<events.size() ? (ServiceEvent) events.get(i) : null;
        out.println(" "
                    +(i<eventTypes.length
                      ? toStringEventType(eventTypes[i]) : "- NONE - ")
                    +" -- "
                    +toString(evt) );
      }
    }

  } // end of class ServiceListener

  // A servicelistener that will be notified about all Services, not
  // only assignable ones.
  class AllServiceListener
    extends ServiceListener
    implements org.osgi.framework.AllServiceListener
  {

    public AllServiceListener()
    {
      super(true);
    }
    public AllServiceListener(boolean checkUsingBundles)
    {
      super(checkUsingBundles);
    }

  }
}
