/*
 * Copyright (c) 2004-2008, KNOPFLERFISH project
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
import java.net.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;
import org.knopflerfish.service.framework_test.*;

import junit.framework.*;

public class FrameworkTestSuite extends TestSuite implements FrameworkTest {
  BundleContext bc;
  Bundle bu;
  Bundle buA;
  Bundle buB;
  Bundle buC;
  Bundle buD;
  Bundle buD1;
  Bundle buE;
  Bundle buF;
  Bundle buH;
  Bundle buJ;
  // Bundle for resource reading integrity check
  Bundle buR2;
  Bundle buR3;
  Bundle buR4;
  Bundle buR5;
  Bundle buR6;

  // Bundles for resource reading integrity check
  Bundle buRimp;
  Bundle buRexp;
  // Package version test bundles
  Bundle buP1;
  Bundle buP2;
  Bundle buP3;

  // the three event listeners
  FrameworkListener fListen;
  BundleListener bListen;
  SyncBundleListener syncBListen;
  ServiceListener sListen;

  Properties props = System.getProperties();
  String lineseparator = props.getProperty("line.separator");
  String test_url_base;
  Vector events = new Vector();                 // vector for events from test bundles
  Vector expevents = new Vector();              // comparision vector


  PrintStream out = System.out;

  long eventDelay = 500;

  public FrameworkTestSuite (BundleContext bc) {
    super("FrameworkTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();
    test_url_base = "bundle://" + bc.getBundle().getBundleId() + "/";

    try {
      eventDelay = Long.getLong("org.knopflerfish.framework_tests.eventdelay",
                                new Long(eventDelay)).longValue();
    } catch (Exception e) {
      e.printStackTrace();
    }

    addTest(new Setup());
    addTest(new Frame005a());
    addTest(new Frame007a());
    addTest(new Frame010a());
    addTest(new Frame018a());
    addTest(new Frame019a());
    addTest(new Frame020a());
    addTest(new Frame025a());
    addTest(new Frame030a());
    addTest(new Frame035a());
    addTest(new Frame038a());
    //    addTest(new Frame040a()); skipped since not a valid test?
    addTest(new Frame041a());
    addTest(new Frame045a());
    addTest(new Frame050a());
    addTest(new Frame055a());
    addTest(new Frame060a());
    addTest(new Frame065a());
    addTest(new Frame068a());
    addTest(new Frame069a());
    addTest(new Frame070a());
    addTest(new Frame075a());
    addTest(new Frame080a());
    addTest(new Frame085a());
    addTest(new Frame110a());
    addTest(new Frame115a());
    //don't fix up security for now
    //addTest(new Frame120a());
    addTest(new Frame125a());
    addTest(new Frame130a());
    addTest(new Frame160a());
    addTest(new Frame161a());
    addTest(new Frame162a());
    addTest(new Frame163a());
    addTest(new Frame170a());
    addTest(new Frame175a());
    addTest(new Frame180a());
    addTest(new Frame181a());
    addTest(new Frame185a());
    addTest(new Frame186a());
    addTest(new Frame190a());
    addTest(new Frame210a());
    addTest(new Frame211a());

    addTest(new Cleanup());
  }


  public String getDescription() {
    return "Tests core functionality in the framework";
  }

  public String getDocURL() {
    return  "https://www.knopflerfish.org/svn/knopflerfish.org/trunk/osgi/bundles_test/regression_tests/framework_test/readme.txt";
  }

  public final static String [] HELP_FRAME005A =  {
    "Verify information from the getHeaders() method",
  };

  class Frame005a extends FWTestCase {

    public void runTest() throws Throwable {
      Dictionary ai = bu.getHeaders();

      // check expected headers

      String k =  "Bundle-ContactAddress";
      String info = (String) ai.get(k);
      assertEquals("bad Bundle-ContactAddress", "http://www.knopflerfish.org", info);

      k =  "Bundle-Description";
      info = (String) ai.get(k);
      assertEquals("bad Bundle-Description", "Test bundle for framework", info);

      k =  "Bundle-DocURL";
      info = (String) ai.get(k);
      assertEquals("bad Bundle-DocURL", "http://www.knopflerfish.org", info);

      k =  "Bundle-Name";
      info = (String) ai.get(k);
      assertEquals("bad Bundle-Name", "framework_test", info);

      k =  "Bundle-Vendor";
      info = (String) ai.get(k);
      assertEquals("bad Bundle-Vendor", "Knopflerfish/Gatespace Telematics", info);

      k =  "Bundle-Version";
      info = (String) ai.get(k);
      assertEquals("bad Bundle-Version", "1.0.0", info);

      k =  "Bundle-ManifestVersion";
      info = (String) ai.get(k);
      assertEquals("bad " + k, "2", info);


      String version = props.getProperty("java.version");
      String vendor = props.getProperty("java.vendor");
      out.println("framework test bundle, Java version " + version);
      out.println("framework test bundle, Java vendor " + vendor);

      out.println("### framework test bundle :FRAME005A:PASS");
    }
  }

  public final static String [] HELP_FRAME007A =  {
    "Extract all information from the getProperty in the BundleContext interface "
  };

  class Frame007a extends FWTestCase {
    public void runTest() throws Throwable {
      String[] NNList = new String[] {
        Constants.FRAMEWORK_OS_VERSION,
        Constants.FRAMEWORK_OS_NAME,
        Constants.FRAMEWORK_PROCESSOR,
        Constants.FRAMEWORK_VERSION,
        Constants.FRAMEWORK_VENDOR,
        Constants.FRAMEWORK_LANGUAGE,
      };

      for(int i = 0; i < NNList.length; i++) {
        String k = NNList[i];
        String v = bc.getProperty(k);
        if(v == null) {
          fail("'" + k + "' not set");
        }
      }
      
      String[] TFList = new String[] {
        Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE,
        Constants.SUPPORTS_FRAMEWORK_FRAGMENT,
        Constants.SUPPORTS_FRAMEWORK_EXTENSION,
        Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION,
      };
      
      for( int i = 0; i < TFList.length; i++) {
        String k = TFList[i];
        String v = bc.getProperty(k);
        if(v == null) {
          fail("'" + k + "' not set");
        } 
        if(!("true".equals(v) || "false".equals(v))) {
          fail("'" + k + "' is '" + v + "', expected 'true' or 'false'");
        }
      }
    }
  }

  public final static String [] HELP_FRAME010A =  {
    "Get context id, location and status of the bundle"
  };


  class Frame010a extends FWTestCase {
    public void runTest() throws Throwable {

      long contextid = bu.getBundleId();
      out.println("CONTEXT ID: " + contextid);

      String location = bu.getLocation();
      out.println("LOCATION: " + location);

      int bunstate = bu.getState();
      out.println("BCACTIVE: " + bunstate);
    }
  }


  static int nRunCount = 0;

  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
      if(nRunCount > 0) {
        fail("The FrameworkTestSuite CANNOT be run reliably more than once. Other test results in this suite are/may not be valid. Restart framework to retest  :Cleanup:FAIL");
      }
      nRunCount++;
      fListen = new FrameworkListener();
      try {
        bc.addFrameworkListener(fListen);
      } catch (IllegalStateException ise) {
        fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }

      bListen = new BundleListener();
      try {
        bc.addBundleListener(bListen);
      } catch (IllegalStateException ise) {
        fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }

      syncBListen = new SyncBundleListener();
      try {
        bc.addBundleListener(syncBListen);
      } catch (IllegalStateException ise) {
        fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }

      sListen = new ServiceListener();
      try {
        bc.addServiceListener(sListen);
      } catch (IllegalStateException ise) {
        fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }

      Locale.setDefault(Locale.CANADA_FRENCH);

      out.println("### framework test bundle :SETUP:PASS");
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
        buA ,
        buB ,
        buC,
        buD ,
        buD1 ,
        buE ,
        buH ,
        buJ ,
        buR2 ,
        buR3 ,
        buR4 ,
        buR5 ,
        buR6 ,
        buRimp ,
        buRexp ,
        buP1 ,
        buP2 ,
        buP3 ,
      };
      for(int i = 0; i < bundles.length; i++) {
        try {  bundles[i].uninstall();  }
        catch (Exception ignored) { }
      }


      buA = null;
      buB = null;
      buC = null;
      buD = null;
      buD1 = null;
      buE = null;
      buF = null;
      buH = null;
      buJ = null;
      // for resource reading integrity check
      buR2 = null;
      buR3 = null;
      buR4 = null;
      buR5 = null;
      buR6 = null;

      // Bundles for resource reading integrity check
      buRimp = null;
      buRexp = null;
      // Package version test bundles
      buP1 = null;
      buP2 = null;
      buP3 = null;



      try   { bc.removeFrameworkListener(fListen); }
      catch (Exception ignored) { }
      fListen = null;

      try   { bc.removeServiceListener(sListen); }
      catch (Exception ignored) { }
      sListen = null;

      try   { bc.removeBundleListener(bListen); }
      catch (Exception ignored) { }
      bListen = null;

    }
  }

  public final static String [] HELP_FRAME018A = {
    "Test result of getService(null). Should throw NPE",
  };

  class Frame018a extends FWTestCase {

    public void runTest() throws Throwable {
      try {
        Object obj = null;
        obj = bc.getService(null);
        fail("### FRAME018A:FAIL Got service object=" + obj + ", excpected NullPointerException");
      } catch (NullPointerException e) {
        out.println("### FRAME018A:PASS: got NPE=" + e);
      } catch (RuntimeException e) {
        fail("### FRAME018A:FAIL: got RTE=" + e);
      } catch (Throwable e) {
        fail("### FRAME018A:FAIL Got " + e + ", expected NullPointerException");
      }
    }
  }

  public final static String [] HELP_FRAME019A = {
    "Try bundle:// syntax, if present in FW, by installing bundleA_test",
    "This test is also valid if ",
    "new URL(bundle://) throws MalformedURLException",
  };

  class Frame019a extends FWTestCase {

    public void runTest() throws Throwable {
      Bundle bA = null;
      try {
        try {
          URL url = new URL("bundle://" + bc.getBundle().getBundleId() + "/" +
                            "bundleA_test-1.0.0.jar");

          // if the URL can be created, it should be possible to install
          // from the URL string representation
          bA = bc.installBundle(url.toString());

          assertNotNull("Bundle should be possible to install from " + url, bA);
          try {
            bA.start();
          } catch (Exception e) {
            fail(url + " couldn't be started, FRAME019A:FAIL");
          }

          assertEquals("Bundle should be in ACTIVE state",
                       Bundle.ACTIVE, bA.getState());

          out.println("### FRAME019A: PASSED, bundle URL " + url);

          // finally block will uninstall bundle and clean up events
        } catch (MalformedURLException e) {
          out.println("### FRAME019A: PASSED, bundle: URL not supported: " + e);
        }
      } finally {
        try {
          if(bA != null) {
            bA.uninstall();
          }
        } catch (Exception e) {
        }
        clearEvents();
      }
    }
  }

  public final static String [] HELP_FRAME020A =  {
    "Load bundleA_test and check that it exists and that its expected service does not exist",
    "Also check that the expected events in the framework occurs"
  };

  class Frame020a extends FWTestCase {
    public void runTest() throws Throwable {
      buA = null;
      boolean teststatus = true;
      try {
        buA = Util.installBundle(bc, "bundleA_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        fail("framework test bundle "+ bexcA +" :FRAME020A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME020A:FAIL");
        teststatus = false;
      }

      //Localization tests
      Dictionary dict = buA.getHeaders();
      if(!dict.get(Constants.BUNDLE_SYMBOLICNAME).equals("org.knopflerfish.bundle.bundleA_test")){
          fail("framework test bundle, " +  Constants.BUNDLE_SYMBOLICNAME + " header does not have right value:FRAME020A:FAIL");
      }


      // Check that no service reference exist yet.
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleA_test.BundleA");
      if (sr1 != null) {
        fail("framework test bundle, service from test bundle A unexpectedly found :FRAME020A:FAIL");
        teststatus = false;
      }

      // check the listeners for events, expect only a bundle event, of type installation
      boolean lStat = checkListenerEvents(out, false , 0, true , BundleEvent.INSTALLED, false, 0, buA, sr1);

      if (teststatus == true && buA.getState() == Bundle.INSTALLED && lStat == true) {
        out.println("### framework test bundle :FRAME020A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME020A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAME025A =  {
    "Start bundleA_test and check that it gets state ACTIVE",
    "and that the service it registers exist"
  };

  class Frame025a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean ungetStat = false;
      try {
        buA.start();
        teststatus = true;
      }
      catch (BundleException bexcA) {
        fail("framework test bundle "+ bexcA +" :FRAME025A:FAIL");
        teststatus = false;
        bexcA.printStackTrace();
      }
      catch (IllegalStateException ise) {
        fail("framework test bundle "+ ise +" :FRAME025A:FAIL");
        teststatus = false;
        ise.printStackTrace();
      }
      catch (SecurityException sec) {
        fail("framework test bundle "+ sec +" :FRAME025A:FAIL");
        teststatus = false;
        sec.printStackTrace();
      }

      // Check if testbundleA registered the expected service
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleA_test.BundleA");
      if (sr1 == null) {
        fail("framework test bundle, expected service not found :FRAME025A:FAIL");
        teststatus = false;
      }
      else {
        try {
          Object o1 = bc.getService(sr1);
          if (o1 == null) {
            fail("framework test bundle, no service object found :FRAME025A:FAIL");
            teststatus = false;
          }

          try {
            ungetStat = bc.ungetService(sr1);
          }
          catch (IllegalStateException ise) {
            fail("framework test bundle, ungetService exception " + ise + ":FRAME025A:FAIL");
          }
        }
        catch (SecurityException sek) {
          fail("framework test bundle, getService " + sek + ":FRAME025A:FAIL");
          teststatus = false;
        }
      }
      // check the listeners for events, expect a bundle event of started and a service event of type registered
      boolean lStat = checkListenerEvents( out, false, 0, true, BundleEvent.STARTED, true, ServiceEvent.REGISTERED, buA, sr1);

      assertTrue("BundleA should be ACTIVE", buA.getState() == Bundle.ACTIVE);
      assertTrue("Service unget should be true", ungetStat == true); // Changed according to KF bug #1780141
      assertTrue("Unexpected events", lStat == true);

      if (teststatus == true && buA.getState() == Bundle.ACTIVE && ungetStat == true && lStat == true ) {
        out.println("### framework test bundle :FRAME025A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME025A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME030A =  {
    "Stop bundleA_test and check that it gets state RESOLVED"
  };

  class Frame030a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleA_test.BundleA");
      boolean lStatSync = false;

      try {
        buA.stop();
        lStatSync = checkSyncListenerEvents(out, true, BundleEvent.STOPPING, buA, null);
        teststatus = true;
      }
      catch (IllegalStateException ise ) {
        fail("framework test bundle, getService " + ise + ":FRAME030A:FAIL");
        teststatus = false;
      }
      catch (BundleException be ) {
        fail("framework test bundle, getService " + be + ":FRAME030A:FAIL");
        teststatus = false;
      }

      boolean lStat = checkListenerEvents(out, false, 0, true, BundleEvent.STOPPED, true, ServiceEvent.UNREGISTERING, buA, sr1);


      if (teststatus == true && buA.getState() == Bundle.RESOLVED && lStat == true && lStatSync == true) {
        out.println("### framework test bundle :FRAME030A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME030A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME035A =  {
    "Stop bundleA_test and check that it gets state UNINSTALLED"
  };

  class Frame035a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleA_test.BundleA");

      try {
        buA.uninstall();
      }
      catch (IllegalStateException ise ) {
        fail("framework test bundle, getService " + ise + ":FRAME035A:FAIL");
        teststatus = false;
      }
      catch (BundleException be ) {
        fail("framework test bundle, getService " + be + ":FRAME035A:FAIL");
        teststatus = false;
      }

      boolean lStat = checkListenerEvents(out, false, 0, true, BundleEvent.UNINSTALLED, false, 0, buA, sr1);

      if (teststatus == true && buA.getState() == Bundle.UNINSTALLED && lStat == true) {
        out.println("### framework test bundle :FRAME035A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME035A:Fail");
      }

    }
  }

  public final static String [] HELP_FRAME038A =  {
    "Install a non existent file, check that the right exception is thrown"
  };

  class Frame038a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean exception;
      buD = null;
      try {
        buD = Util.installBundle(bc, "nonexisting_bundle_file.jar");
        exception = false;
      }
      catch (BundleException bexcA) {
        // out.println("framework test bundle "+ bexcA +" :FRAME038A:FAIL");
        exception = true;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME038A:FAIL");
        teststatus = false;
        exception = true;
      }



      if (exception == false) {
        teststatus = false;
      }

      // check the listeners for events, expect nothing
      boolean lStat = checkListenerEvents(out, false,0, false,0, false,0, buD, null);

      if (teststatus == true && buD == null && lStat == true) {
        out.println("### framework test bundle :FRAME038A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME038A:FAIL");
      }
    }
  }

  // 8. Install testbundle D, check that an BundleException is thrown
  //    as this bundle has no manifest file in its jar file
  //    (hmmm...I don't think this is a correct test. /EW)
  class Frame040a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean exception;
      buD = null;
      try {
        buD = Util.installBundle(bc, "bundleD_test-1.0.0.jar");
        exception = false;
      }
      catch (BundleException bexcA) {
        System.out.println("framework test bundle "+ bexcA +" :FRAME040A:FAIL");
        // This exception is expected
        exception = true;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME040A:FAIL");
        teststatus = false;
        exception = true;
      }

      // Check that no service reference exist.
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleD_test.BundleD");
      if (sr1 != null) {
        fail("framework test bundle, service from test bundle D unexpectedly found :FRAME040A:FAIL");
        teststatus = false;
      }

      if (exception == false) {
        teststatus = false;
      }

      // check the listeners for events, expect only a bundle event, of type installation
      boolean lStat = checkListenerEvents(out, false, 0, false , 0, false, 0, buD, null);

      out.println("FRAME040A: lStat=" + lStat);
      if (teststatus == true && buD == null && lStat == true) {
        out.println("### framework test bundle :FRAME040A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME040A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME041A =  {
    "Install bundleD1_test, which has a broken manifest file,",
    "an empty import statement and check",
    "that the expected exceptions are thrown"
  };

  class Frame041a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean exception;
      buD1 = null;
      try {
        buD1 = Util.installBundle(bc, "bundleD1_test-1.0.0.jar");
        exception = false;
      }
      catch (BundleException bexcA) {
        // System.out.println("framework test bundle "+ bexcA +" :FRAME041A");
        // Throwable tex = bexcA.getNestedException();
        // if (tex != null) {
        //   System.out.println("framework test bundle, nested exception "+ tex +" :FRAME041A");
        // }
        // This exception is expected
        exception = true;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME041A:FAIL");
        teststatus = false;
        exception = true;
      }

      // Check that no service reference exist.
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleD1_test.BundleD1");
      if (sr1 != null) {
        fail("framework test bundle, service from test bundle D1 unexpectedly found :FRAME041A:FAIL");
        teststatus = false;
      }

      if (exception == false) {
        teststatus = false;
      }

      // check the listeners for events, expect only a bundle event, of type installation
      boolean lStat = checkListenerEvents(out, false, 0, false , 0, false, 0, buD, null);

      if (teststatus == true && buD == null && lStat == true) {
        out.println("### framework test bundle :FRAME041A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME041A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAME045A =  {
    "Add a service listener with a broken LDAP filter to get an exception"
  };

  class Frame045a extends FWTestCase {
    public void runTest() throws Throwable {
      ServiceListener sListen1 = new ServiceListener();
      String brokenFilter = "A broken LDAP filter";

      try {
        bc.addServiceListener(sListen1, brokenFilter);
        out.println("Frame045a: Added LDAP filter");
      }
      catch (InvalidSyntaxException ise) {
        assertEquals("InvalidSyntaxException.getFilter should be same as input string", brokenFilter, ise.getFilter());
      } catch (Exception e) {
        fail("framework test bundle, wroing exception on broken LDAP filter, FREME045A:FAIL " + e);
      }

      out.println("### framework test bundle :FRAME045A:PASS");
    }
  }

  public final static String [] HELP_FRAME050A =  {
    "Loads and starts bundleB_test, checks that it gets the state ACTIVE.",
    "Checks that it implements the Configurable interface."
  };

  class Frame050a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      try {
        buB = Util.installBundle(bc, "bundleB_test-1.0.0.jar");
        buB.start();
        teststatus = true;
      }
      catch (BundleException bexcB) {
        fail("framework test bundle "+ bexcB +" :FRAME050A:FAIL");
        teststatus = false;
        bexcB.printStackTrace();
      }
      catch (SecurityException secB) {
        fail("framework test bundle "+ secB +" :FRAME050A:FAIL");
        teststatus = false;
        secB.printStackTrace();
      }
      catch (IllegalStateException ise) {
        fail("framework test bundle "+ ise +" :FRAME050A:FAIL");
        teststatus = false;
        ise.printStackTrace();
      }



      // Check if testbundleB registered the expected service
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleB_test.BundleB");
      if (sr1 == null) {
        fail("framework test bundle, expected service not found :FRAME050A:FAIL");
        teststatus = false;
      } else {
        Object o1 = bc.getService(sr1);
        // out.println("o1 = " + o1);

        if (!(o1 instanceof Configurable)) {
          fail("framework test bundle, service does not support Configurable :FRAME050A:FAIL");
          teststatus = false;
        }
        else {
          // out.println("framework test bundle got service ref");
          Configurable c1 = (Configurable) o1;
          // out.println("c1 = " + c1);
          Object o2 = c1.getConfigurationObject();
          if (o2 != c1) {
            teststatus = false;
            fail("framework test bundle, configuration object is not the same as service object :FRAME050A:FAIL");
          }
          // out.println("o2 = " + o2 + " bundle = " + buB);
          // out.println("bxx " + sr1.getBundle());
        }
      }
      // Check that the dictionary from the bundle seems to be ok, keys[1-4], value[1-4]
      String keys [] = sr1.getPropertyKeys();
      for (int k=0; k< keys.length; k++) {
        if (keys[k].equals("key"+k)) {
          if (!(sr1.getProperty(keys[k]).equals("value"+k))) {
            teststatus = false;
            fail("framework test bundle, key/value mismatch in propety list :FRAME050A:FAIL");
          }
        }
      }

      if (teststatus == true && buB.getState() == Bundle.ACTIVE) {
        out.println("### framework test bundle :FRAME050A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME050A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME055A =  {
    "Load and start bundleC_test, checks that it gets the state ACTIVE.",
    "Checks that it is available under more than one name.",
    "Then stop the bundle, check that no exception is thrown",
    "as the bundle unregisters itself in its stop method."
  };

  class Frame055a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      try {
        buC = Util.installBundle(bc, "bundleC_test-1.0.0.jar");
        buC.start();
        teststatus = true;
      }
      catch (BundleException bexcB) {
        teststatus = false;
        bexcB.printStackTrace();
        fail("framework test bundle "+ bexcB +" :FRAME055A:FAIL");
      }
      catch (SecurityException secB) {
        teststatus = false;
        secB.printStackTrace();
        fail("framework test bundle "+ secB +" :FRAME055A:FAIL");
      }
      catch (IllegalStateException ise) {
        teststatus = false;
        ise.printStackTrace();
        fail("framework test bundle "+ ise +" :FRAME055A:FAIL");
      }


      Dictionary dict = buC.getHeaders();
      if(!dict.get(Constants.BUNDLE_SYMBOLICNAME).equals("org.knopflerfish.bundle.bundleC_test")){
          fail("framework test bundle, " +  Constants.BUNDLE_SYMBOLICNAME + " header does not have right value:FRAME055A:FAIL");
      }


      // Check if testbundleC registered the expected service
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleC_test.BundleC");
      if (sr1 == null) {
        teststatus = false;
        fail("framework test bundle, expected service not found :FRAME055A:FAIL");
      }
      else {
        // get objectClass service name array
        int hits = 0;
        String [] packnames = (String[]) sr1.getProperty("objectClass");
        for (int j = 0; j< packnames.length; j++) {
          if (packnames[j].equals("org.knopflerfish.service.bundleC_test.BundleC")) {
            hits++;
          }
          if (packnames[j].equals("java.lang.Object")) {
            hits++;
          }
        }
        if (hits !=2) {
          teststatus = false;
          fail("framework test bundle, expected service not registered under the two expected names :FRAME055A:FAIL");
        }
      }

      // Check if testbundleC registered the expected service with java.lang.Object as well

      ServiceReference sref [] = null;
      try {
        sref = bc.getServiceReferences("java.lang.Object",null);
        if (sref == null) {
          fail("framework test bundle, expected service not found :FRAME055A:FAIL");
          teststatus = false;
        }
        else {
          // get objectClass service name array
          int hits = 0;
          String [] packnames = (String[]) sr1.getProperty("objectClass");
          for (int j = 0; j< packnames.length; j++) {
            if (packnames[j].equals("org.knopflerfish.service.bundleC_test.BundleC")) {
              hits++;
            }
            if (packnames[j].equals("java.lang.Object")) {
              hits++;
            }
          }
          if (hits !=2) {
            teststatus = false;
            fail("framework test bundle, expected service not registered under the two expected names :FRAME055A:FAIL");
          }
        }
      }
      catch (InvalidSyntaxException ise) {
        fail("framework test bundle, invalid syntax in LDAP filter :" + ise + " :FRAME055A:FAIL");
        teststatus = false;
      }

      // 11a. check the getProperty after registration, something should come back
      // Check that both keys in the service have their expected values

      boolean h1 = false;
      boolean h2 = false;
      if (sref != null) {
        for (int i = 0; i< sref.length; i++) {
          String sn1[] = (String[]) sref[i].getProperty("objectClass");
          for (int j = 0; j < sn1.length; j++) {
            if (sn1[j].equals("org.knopflerfish.service.bundleC_test.BundleC")) {
              String keys[] = sref[i].getPropertyKeys();
              if (keys != null) {
                for (int k = 0; k< keys.length; k++) {
                  try {
                    String s1 = (String) sref[i].getProperty(keys[k]);
                    if (s1.equals("value1")) {h1 = true;}
                    if (s1.equals("value2")) {h2 = true;}
                  }
                  catch (Exception e1) {
                    // out.println("framework test bundle exception " + e1 );
                  }
                }
              }
            }
          }
        }
      }

      if (! (h1 == true && h2 == true)) {
        teststatus = false;
        fail("framework test bundle, expected property values from registered bundleC_test not found :FRAME055A:FAIL");
      }

      try {
        buC.stop();
      }
      catch (BundleException bexp) {
        teststatus = false;
        fail("framework test bundle, exception in stop method :" + bexp + " :FRAME055A:FAIL");
      }
      catch (Throwable thr) {
        teststatus = false;
        fail("framework test bundle, exception in stop method :" + thr + " :FRAME055A:FAIL");
      }

      // 11a. check the getProperty after unregistration, something should come back
      // Check that both keys i the service still have their expected values

      h1 = false;
      h2 = false;

      if (sref != null) {
        for (int i = 0; i< sref.length; i++) {
          String sn1[] = (String[]) sref[i].getProperty("objectClass");
          if (sn1 != null) {
            for (int j = 0; j < sn1.length; j++) {
              if (sn1[j].equals("org.knopflerfish.service.bundleC_test.BundleC")) {
                String keys[] = sref[i].getPropertyKeys();
                if (keys != null) {
                  for (int k = 0; k< keys.length; k++) {
                    try {
                      String s1 = (String) sref[i].getProperty(keys[k]);
                      if (s1.equals("value1")) {h1 = true;}
                      if (s1.equals("value2")) {h2 = true;}
                    }
                    catch (Exception e1) {
                      // out.println("framework test bundle exception " + e1 );
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (!(h1 == true && h2 == true)) {
        teststatus = false;
        fail("framework test bundle, expected property values from unregistered bundleC_test not found :FRAME055A:FAIL");
      }

      out.println("framework test bundle, buC.getState() = " + buC.getState());
      if (teststatus == true && buC.getState() == Bundle.RESOLVED) {
        out.println("### framework test bundle :FRAME055A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME055A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME060A =  {
    "Gets the configurable object from testbundle B,",
    "update its properties and check that a ServiceEvent occurs.",
    "Also get the ServiceRegistration object from bundle",
    "and check that the bundle is the same and that",
    "unregistration causes a ServiceEvent.UNREGISTERING."
  };

  class Frame060a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean lStat = false;
      boolean lStat2 = false;

      ServiceRegistration servRegB = null;
      Method m;
      Class c, parameters[];
      ServiceRegistration ServReg;
      // clear the listeners
      clearEvents();    // get rid of all prevoius events


      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleB_test.BundleB");
      if (sr1 == null) {
        fail("framework test bundle, expected service not found :FRAME060A:FAIL");
        teststatus = false;
      }
      else {
        Object o1 = bc.getService(sr1);
        // out.println("o1 = " + o1);

        if (!(o1 instanceof Configurable)) {
          fail("framework test bundle, service does not support Configurable :FRAME060A:FAIL");
          teststatus = false;
        }
        else {
          Hashtable h1 = new  Hashtable();
          h1.put ("key1","value7");
          h1.put ("key2","value8");

          // now for some reflection exercises

          Object[] arguments = new Object[1];
          c = o1.getClass();
          parameters = new Class[1];
          parameters[0] = h1.getClass(); //  new Class[0];
          arguments[0] = h1;
          try {
            m = c.getMethod("setServReg", parameters);
            servRegB = (ServiceRegistration) m.invoke(o1, arguments);
            // System.out.println("servRegB= " + servRegB);
          }
          catch (IllegalAccessException ia) {
            out.println("Frame test IllegaleAccessException" +  ia);
          }
          catch (InvocationTargetException ita) {
            out.println("Frame test InvocationTargetException" +  ita);
            out.println("Frame test nested InvocationTargetException" +  ita.getTargetException() );

          }
          catch (NoSuchMethodException nme) {
            out.println("Frame test NoSuchMethodException" +  nme);
          }

          // Check that the dictionary from the bundle seems to be ok, keys[1-2], value[7-8]

          String keys [] = sr1.getPropertyKeys();
          for (int k=0; k< keys.length; k++) {
            // out.println("key=" + keys[k] +" val= " + sr1.getProperty(keys[k]));
            int l = k + 6;
            if (keys[k].equals("key"+l)) {
              if (!(sr1.getProperty(keys[k]).equals("value"+k))) {
                teststatus = false;
                fail("framework test bundle, key/value mismatch in propety list :FRAME060A:FAIL");
              }
            }
          }
          // check the listeners for events, in this case service event MODIFIED
          lStat = checkListenerEvents(out, false,0,  false,0,  true,ServiceEvent.MODIFIED,  buB, sr1);
          clearEvents();

          // now to get the service reference as well for some manipulation
          arguments = new Object [0];
          parameters = new Class [0];
          try {
            m = c.getMethod("getServReg", parameters);
            servRegB = (ServiceRegistration) m.invoke(o1, arguments);
            ServiceReference sri = servRegB.getReference();
            if (sri.getBundle() != buB) {
              teststatus = false;
              fail("framework test bundle, bundle not as expected :FRAME060A:FAIL");
            }
            else {
              servRegB.unregister();
              out.println("servRegB= " + servRegB);
            }
          }
          catch (IllegalAccessException ia) {
            out.println("Frame test IllegaleAccessException" +  ia);
          }
          catch (InvocationTargetException ita) {
            out.println("Frame test InvocationTargetException" +  ita);
          }
          catch (NoSuchMethodException nme) {
            out.println("Frame test NoSuchMethodException" +  nme);
          }

          lStat2 = checkListenerEvents(out, false,0,  false,0,  true,ServiceEvent.UNREGISTERING,  buB, sr1);

        }
      }

      if (teststatus == true && buB.getState() == Bundle.ACTIVE && lStat == true && lStat2 == true) {
        out.println("### framework test bundle :FRAME060A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME060A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME065A =  {
    "Load and try to start bundleE_test, ",
    "It should be possible to load , but should not be possible to start",
    "as the start method in the manifest is not available in the bundle."
  };

  class Frame065a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean catchstatus = true;
      buE = null;
      try {
        buE = Util.installBundle(bc, "bundleE_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        teststatus = false;
        fail("framework test bundle "+ bexcA +" :FRAME065A:FAIL");
      }
      catch (SecurityException secA) {
        teststatus = false;
        fail("framework test bundle "+ secA +" :FRAME065A:FAIL");
      }
      clearEvents();

      // now try and start it, which should generate a BundleException
      try {
        buE.start();
        catchstatus = false;
      }
      catch (BundleException bexcA) {
        // the nested exception should be a ClassNotFoundException, check that
        Throwable t1 = bexcA.getNestedException();
        if (t1 instanceof ClassNotFoundException) {
          catchstatus = true;
        }
        else {
          catchstatus = false;
          bexcA.printStackTrace();
          fail("framework test bundle, unexpected nested exception "+ t1 +" :FRAME065A:FAIL");
        }

        //System.out.println("framework test bundle "+ bexcA +" :FRAME065A:FAIL");
      }
      catch (IllegalStateException ise) {
        teststatus = false;
        ise.printStackTrace();
        fail("framework test bundle "+ ise +" :FRAME065A:FAIL");
      }
      catch (SecurityException sec) {
        teststatus = false;
        sec.printStackTrace();
        fail("framework test bundle "+ sec +" :FRAME065A:FAIL");
      }
      // check the events, only BundleEvent.RESOLVED and BundleEvent.STARTING should have happened
      boolean lStat = checkListenerEvents(out, false, 0, true, BundleEvent.RESOLVED, false, 0, buE, null);
      boolean lStatSync = checkSyncListenerEvents(out, true, BundleEvent.STARTING, buE, null);

      if (catchstatus == false) {
        teststatus = false;
      }


      Dictionary dict = buE.getHeaders();
      if(!dict.get(Constants.BUNDLE_SYMBOLICNAME).equals("org.knopflerfish.bundle.bundleE_test")){
          fail("framework test bundle, " +  Constants.BUNDLE_SYMBOLICNAME + " header does not have right value:FRAME065A:FAIL");
      }

      if (teststatus == true && lStat == true && lStatSync == true) {
        out.println("### framework test bundle :FRAME065A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME065A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME068A =  {
    "Tests accessing multiple resources inside the test bundle itself",
    "using ClassLoader.getResource"
  };


  class Frame068a extends FWTestCase {
    public void runTest() throws Throwable {
      int n;

      // first check that correct number of files exists

      // the fw_test_multi.txt resources are present in
      // res1.jar, subdir/res1.jar, res2.jar and in the top bundle
      n = countResources("/fw_test_multi.txt");
      assertEquals("Multiple resources should be reflected by CL.getResources() > 1", 4, n);



      //bundle.loadClass test
      boolean cauchtException = false;
      try{
          bc.getBundle().loadClass("org.knopflerfish.bundle.io.Activato");
      }
      catch(ClassNotFoundException e){
          cauchtException = true;
      }

      if(!cauchtException){
          fail("bundle.loadclass failed to generate exception for non-existent class");
      }

      try{
          bc.getBundle().loadClass("org.knopflerfish.bundle.io.Activator");
      }
      catch(ClassNotFoundException e){
          fail("bundle.loadclass failed");
      }

      try{
          bc.getBundle().loadClass("org.osgi.service.io.ConnectionFactory");
      }
      catch(ClassNotFoundException e){
          fail("bundle.loadclass failed");
      }
      //existing directory
      Enumeration enume = bc.getBundle().getEntryPaths("/");
      if(enume == null ){
          fail("GetEntryPaths did not retrieve the correct number of elements, "
               +"/");
      }
      out.println("bc.getBundle().getEntryPaths(\"/\")");
      int i = 0;
      while(enume.hasMoreElements()){
          i++;
          out.println(enume.nextElement());
      }
      // This test needs to be updated every time a the
      // framework_tests bundle is changed in such a way that new
      // files or directories are added or removed to / from the top
      // level of the bundle jar-file.
      if(i != 43){
          fail("GetEntryPaths did not retrieve the correct number of elements, "
               +"43 != "+ i);
      }

      //another existing directory
      out.println("getEntryPaths(\"/org/knopflerfish/bundle/framework_test\")");
      enume = bc.getBundle()
        .getEntryPaths("/org/knopflerfish/bundle/framework_test");
      if(enume == null ){
          fail("GetEntryPaths did not retrieve the correct number of elements, "
               +"framework_test");
      }
      i = 0;
      while(enume.hasMoreElements()){
          i++;
          out.println(enume.nextElement());
      }
      // This test needs to be updated every time a the
      // FrameworkTestSuite is changed in such a way that new files
      // or directories are added or removed to/from the sub-dir
      // "org/knopflerfish/bundle/framework_test" of the jar-file.
      if(i!=111){
          fail("GetEntryPaths did not retrieve the correct number of elements, "
               +"111 != " + i);
      }


      //existing file, non-directory, ending with slash
      enume = bc.getBundle().getEntryPaths("/bundleA_test-1.0.0.jar/");
      if(enume != null ){
          fail("GetEntryPaths did not retrieve the correct number of elements");
      }

      //existing file, non-directory
      enume = bc.getBundle().getEntryPaths("/bundleA_test-1.0.0.jar");
      if(enume != null ){
          fail("GetEntryPaths did not retrieve the correct number of elements");
      }

      //non-existing file
      enume = bc.getBundle().getEntryPaths("/e");
      if(enume != null){
          fail("GetEntryPaths did not retrieve the correct number of elements");
      }

      //empty dir
      enume = bc.getBundle().getEntryPaths("/emptySubDir");
      if(enume != null){
          fail("GetEntryPaths did not retrieve the correct number of elements");
      }

      //dir with only one entry
      enume = bc.getBundle().getEntryPaths("/org/knopflerfish/bundle");
      if(enume == null ){
          fail("GetEntryPaths did not retrieve the correct number of elements");
      }
      i = 0;
      while(enume.hasMoreElements()){
          i++;
          enume.nextElement();
      }
      if(i != 1){
          fail("GetEntryPaths did not retrieve the correct number of elements");
      }




      //TODO more, extensive loadClass tests


      // the fw_test_single.txt resource is present just
      // res2.jar
      n = countResources("/fw_test_single.txt");
      assertEquals("Single resources should be reflected by CL.getResources() == 1", 1, n);

      // getEntry test
      URL url = bc.getBundle().getEntry("/fw_test_multi.txt");
      assertURLExists(url);

      // the fw_test_nonexistent.txt is not present at all
      n = countResources("/fw_test_nonexistent.txt");
      assertEquals("Multiple nonexistent resources should be reflected by CL.getResources() == 0", 0, n);

      // Try to get the top level URL of the bundle
      url = bc.getBundle().getResource("/");
      out.println("bc.getBundle().getResource(\"/\") -> " +url);
      assertNotNull("bc.getBundle().getResource(\"/\")",url);
      // Check that we can build a usable URL from root URL.
      url = new URL(url,"META-INF/MANIFEST.MF");
      assertURLExists(url);

      // Try to get the top level URLs of the bundle and check them
      {
        out.println("bc.getBundle().getResources(\"/\") -> ");
        Enumeration e = bc.getBundle().getResources("/");
        assertNotNull("bc.getBundle().getResources(\"/\")", e);
        while(e.hasMoreElements()) {
          url = (URL)e.nextElement();
          out.println("\t" +url);
          assertNotNull("Bundle root URL", url);
        }
      }


      out.println("### framework test bundle :FRAME068A:PASS");
    }


    int countResources(String name) throws Exception {
      return countResources(name, false);
    }
    int countResources(String name, boolean verbose ) throws Exception {
      Bundle bundle = bc.getBundle();
      int n = 0;
      Enumeration e = bundle.getResources(name);
      if (verbose) {
        out.println("bc.getBundle().getResources(\"" + name +"\") -> ");
      }
      if(e == null) return 0;
      while(e.hasMoreElements()) {
        URL url = (URL)e.nextElement();
        if (verbose) { out.println("\t" +url); }
        assertURLExists(url);
        n++;
      }
      return n;
    }

    void testRes(String name, int count) throws Exception {
      out.println("testRes(" + name + ")");
      ClassLoader cl = getClass().getClassLoader();
      URL url1 = cl.getResource(name);
      out.println(" ClassLoader url = " + url1);
      assertURLExists(url1);

      URL url2 = bc.getBundle().getResource(name);
      out.println(" bundle url      = " + url1);
      assertURLExists(url2);

      int n = 1;
      for(Enumeration e = cl.getResources(name); e.hasMoreElements(); ) {
        URL url = (URL)e.nextElement();
        out.println("  " + n + "              = " + url);
        assertURLExists(url);
        n++;
      }

    }

    void assertURLExists(URL url) throws Exception {
      InputStream is = null;

      try {
        is = url.openStream();
        assertNotNull("URL " + url + " should give a non-null stream", is);
        return;
      } finally {
        try { is.close(); } catch (Exception ignored) {  }
      }
    }
  }

  public final static String [] HELP_FRAME069A =  {
    "Tests contents of multiple resources inside the test bundle itself",
    "using ClassLoader.getResource"
  };

  class Frame069a extends FWTestCase {
    public void runTest() throws Throwable {
      Hashtable texts = new Hashtable();
      texts.put("This is a resource in the bundle's res2.jar internal jar file",
                Boolean.FALSE);
      texts.put("This is a resource in the bundle's res1.jar internal jar file",
                Boolean.FALSE);
      texts.put("This is a resource in the bundle's main package",
                Boolean.FALSE);

      verifyContent("/fw_test_multi.txt", texts);

      texts = new Hashtable();
      texts.put("This is a single resource in the bundle's res2.jar internal jar file.",
                Boolean.FALSE);

      verifyContent("/fw_test_single.txt", texts);

      out.println("### framework test bundle :FRAME069A:PASS");
    }

    void verifyContent(String name, Hashtable texts) throws Exception {
      ClassLoader cl = getClass().getClassLoader();
      for(Enumeration e = cl.getResources(name);
          e.hasMoreElements();) {
        URL url = (URL)e.nextElement();
        out.println("Loading text from "+url);
        String s = new String(Util.loadURL(url));
        if(!texts.containsKey(s)) {
          fail("Checking resource name '" + name + "', found unexpected content '" + s + "' in " + url);
        }
        texts.put(s, Boolean.TRUE);
      }
      for(Enumeration e = texts.keys(); e.hasMoreElements();) {
        String  s = (String)e.nextElement();
        Boolean b = (Boolean)texts.get(s);
        if(!b.booleanValue()) {
          fail("Checking resource name '" + name + "', did not find content '" + s + "'");
        }
      }
    }

 }

  public final static String [] HELP_FRAME070A =  {
    "Reinstalls and the updates testbundle_A.",
    "The version is checked to see if an update has been made."
  };

  class Frame070a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean catchstatus = true;
      String jarA = "bundleA_test-1.0.0.jar";
      String jarA1 = "bundleA1_test-1.0.1.jar";
      InputStream fis;
      String versionA;
      String versionA1;

      buA = null;

      try {
        buA = Util.installBundle(bc, jarA);
        teststatus = true;
      }
      catch (BundleException bexcA) {
        fail("framework test bundle "+ bexcA +" :FRAME070A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME070A:FAIL");
        teststatus = false;
      }

      Dictionary ai = buA.getHeaders();

      if(false) {
        // debugging
        for (Enumeration e = ai.keys(); e.hasMoreElements();) {
          Object key = e.nextElement();
          Object value = ai.get(key);
          String s =  key.toString();
          String v =  value.toString();
          out.println("A: Manifest info: " + s + ", " + v);
        }
      }

      versionA = (String) ai.get("Bundle-Version");

      clearEvents();
      out.println("Before version = " + versionA);

      try {
        URL urk = bc.getBundle().getResource(jarA1);
        out.println("update from " + urk);
        // URLConnection url1 = URLConnection (urk);
        fis = urk.openStream();
        if(fis == null) {
          fail("No data at " + urk + ":FRAME070A:FAIL");
        }


        try {

      //                TODO rework, does not always work

          long lastModified = buA.getLastModified();

          buA.update(fis);
          /*
          if(buA.getLastModified() <= lastModified){
                  fail("framework test bundle, update does not change lastModified value :FRAME070A:FAIL");
          }*/
        }
        catch (BundleException be ) {
          teststatus = false;
          fail("framework test bundle, update without new bundle source :FRAME070A:FAIL");
        }
      }
      catch (MalformedURLException murk) {
        teststatus = false;
        fail("framework test bundle, update file not found " + murk+ " :FRAME070A:FAIL");
      }
      catch (FileNotFoundException fnf) {
        teststatus = false;
        fail("framework test bundle, update file not found " + fnf+ " :FRAME070A:FAIL");
      }
      catch (IOException ioe) {
        teststatus = false;
        fail("framework test bundle, update file not found " + ioe+ " :FRAME070A:FAIL");
      }

      Dictionary a1i = buA.getHeaders();

      if(false) {
        // debugging
        for (Enumeration e = a1i.keys(); e.hasMoreElements();) {
          Object key = e.nextElement();
          Object value = a1i.get(key);
          String s =  key.toString();
          String v =  value.toString();
          out.println("A1: Manifest info: " + s + ", " + v);
        }
      }

      a1i = buA.getHeaders();
      versionA1 = (String) a1i.get("Bundle-Version");

      out.println("After  version = " + versionA1);

      // check the events, none should have happened
      boolean lStat = checkListenerEvents(out, false, 0, true ,BundleEvent.UPDATED , false, 0, buA, null);

      if (versionA1.equals(versionA)) {
        teststatus = false;
        fail("framework test bundle, update of bundle failed, version info unchanged :FRAME070A:Fail");
      }


      if (teststatus == true ) {
        out.println("### framework test bundle :FRAME070A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME070A:FAIL");
      }
    }
  }

  // 15. Uninstall a the testbundle B and then try to start and stop it
  // In both cases exceptions should be thrown.

  public final static String [] HELP_FRAME075A =  {
    "Uninstall bundleB_test and the try to start and stop it.",
    "In both cases exceptions should be thrown."
  };

  class Frame075a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean exep1 = false;
      boolean exep2 = false;

      try {
        buB.uninstall();
      }
      catch (BundleException be) {
        teststatus = false;
        fail("framework test bundle, uninstall of bundleB failed:" + be +" :FRAME075A:FAIL");
      }

      catch (Exception e) {
        fail("framework test bundle, got unexpected exception " + e + " :FRAME075A:FAIL");
        e.printStackTrace();
      }

      try {
        buB.start();
      }
      catch (BundleException be) {
        teststatus =  false;
        fail("framework test bundle, got unexpected exception " + be + "at start :FRAME075A:FAIL");
      }
      catch (IllegalStateException ise) {
        exep1 = true;
        // out.println("Got expected exception" + ise);
      }
      catch (SecurityException sec) {
        teststatus = false;
        fail("framework test bundle, got unexpected exception " + sec + " :FRAME075A:FAIL");
      }

      catch (Exception e) {
        fail("framework test bundle, got unexpected exception " + e + " :FRAME075A:FAIL");
        e.printStackTrace();
      }

      try {
        buB.stop();
      }
      catch (BundleException be) {
        teststatus =  false;
        fail("framework test bundle, got unexpected exception " + be + "at stop :FRAME075A:FAIL");
      }
      catch (IllegalStateException ise) {
        exep2 = true;
        // out.println("Got expected exception" + ise);
      }
      catch (SecurityException sec) {
        teststatus = false;
        fail("framework test bundle, got unexpected exception " + sec + " :FRAME075A:FAIL");
      }

      catch (Exception e) {
        fail("framework test bundle, got unexpected exception " + e + " :FRAME075A:FAIL");
        e.printStackTrace();
      }

      // System.err.println("teststatus=" + teststatus + " exep1= " + exep1 + " exep2= " + exep2);
      teststatus = teststatus && exep1 && exep2;

      if (teststatus == true ) {
        out.println("### framework test bundle :FRAME075A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME075A:FAIL");
      }
    }
  }

  // 16. Install and start testbundle F and then try to and stop it
  // In this case a bundeException is expected

  public final static String [] HELP_FRAME080A =  {
    "Installs and starts bundleF_test and then try to and stop it.",
    "A BundleException is expected."
  };

  class Frame080a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean catchstatus = true;
      buF = null;
      try {
        buF = Util.installBundle(bc, "bundleF_test-1.0.0.jar");
      }
      catch (BundleException bexcA) {
        fail("framework test bundle "+ bexcA +" :FRAME080A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME080A:FAIL");
        teststatus = false;
      }

      Dictionary dict = buF.getHeaders("fr_CA");
      if(!dict.get(Constants.BUNDLE_SYMBOLICNAME).equals("org.knopflerfish.bundle.bundleF_test")){
          fail("framework test bundle, " +  Constants.BUNDLE_SYMBOLICNAME + " header does not have correct value:FRAME080A:FAIL");
      }
      if(!dict.get(Constants.BUNDLE_DESCRIPTION).equals("Test")){
          fail("framework test bundle, " +  Constants.BUNDLE_DESCRIPTION + " header does not have correct localized value:FRAME080A:FAIL");
      }

      dict = buF.getHeaders("fr");
      if(!dict.get(Constants.BUNDLE_DESCRIPTION).equals("Tezt")){
          fail("framework test bundle, " +  Constants.BUNDLE_DESCRIPTION + " header does not have correct localized value:FRAME080A:FAIL");
      }



      // now start it
      try {
        buF.start();
      }
      catch (BundleException bexcA) {
        fail("framework test bundle, unexpected exception "+ bexcA +" :FRAME080A:FAIL");
        teststatus = false;
        bexcA.printStackTrace();
      }
      catch (IllegalStateException ise) {
        fail("framework test bundle "+ ise +" :FRAME080A:FAIL");
        teststatus = false;
        ise.printStackTrace();
      }
      catch (SecurityException sec) {
        fail("framework test bundle "+ sec +" :FRAME080A:FAIL");
        teststatus = false;
        sec.printStackTrace();
      }
      // now for the test of a stop that should casue an exception

      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleF_test.BundleF");

      clearEvents ();
      try {
        buF.stop();
      }
      catch (BundleException be) {
        Throwable t1 = be.getNestedException();
        if (t1.getMessage().equals("BundleF stop")) {
          catchstatus = true;
          // out.println("Got expected exception" + be);
        }
        else {
          catchstatus = false;
        }
      }
      catch (IllegalStateException ise) {
        teststatus =  false;
        fail("framework test bundle, got unexpected exception " + ise + "at stop :FRAME080A:FAIL");
      }
      catch (SecurityException sec) {
        teststatus = false;
        fail("framework test bundle, got unexpected exception " + sec + " :FRAME080A:FAIL");
      }

      catch (Exception e) {
        fail("framework test bundle, got unexpected exception " + e + " :FRAME080A:FAIL");
        e.printStackTrace();
      }

      // check the events,
      boolean lStat = checkListenerEvents(out, false, 0, true ,BundleEvent.STOPPED , true, ServiceEvent.UNREGISTERING, buF, sr1);
      // out.println("lStat = "+ lStat);

      if (catchstatus == false) {
        teststatus = false;
      }

      if (teststatus == true && lStat == true ) {
        out.println("### framework test bundle :FRAME080A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME080A:FAIL");
      }
    }
  }

  // 17. Install and start testbundle H, a service factory and test that the methods
  //     in that interface works.

  public final static String [] HELP_FRAME085A =  {
    "Installs and starts bundleH_test, a service factory",
    "and tests that the methods in that API works."
  };

  class Frame085a extends FWTestCase {
    public void runTest() throws Throwable {
      buH = null;
      boolean teststatus = true;
      try {
        buH = Util.installBundle(bc, "bundleH_test-1.0.0.jar");
        buH.start();
        teststatus = true;
      }
      catch (BundleException bexcA) {
        fail("framework test bundle "+ bexcA +" :FRAME085A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME085A:FAIL");
        teststatus = false;
      }

      Dictionary dict = buH.getHeaders("en_US");

      if(!dict.get(Constants.BUNDLE_SYMBOLICNAME).equals("org.knopflerfish.bundle.bundleH_test")){
          fail("framework test bundle, " +  Constants.BUNDLE_SYMBOLICNAME + " header does not have rightt value:FRAME085A:FAIL");
      }

      if(!dict.get(Constants.BUNDLE_DESCRIPTION).equals("Test bundle for framework, bundleH_test")){
          fail("framework test bundle, " +  Constants.BUNDLE_DESCRIPTION + " header does not have rightt value:FRAME085A:FAIL");
      }

      if(!dict.get(Constants.BUNDLE_NAME).equals("bundle_H")){
          fail("framework test bundle, " +  Constants.BUNDLE_NAME + " header does not have rightt value:FRAME085A:FAIL");
      }

      if(!dict.get(Constants.BUNDLE_VERSION).equals("2.0.0")){
          fail("framework test bundle, " +  Constants.BUNDLE_VERSION + " header does not have right value:FRAME085A:FAIL");
      }


      // Check that a service reference exist
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleH_test.BundleH");
      if (sr1 == null) {
        fail("framework test bundle, no service from test bundle H found :FRAME085A:FAIL");
        teststatus = false;
      }

      // check the listeners for events, expect only a bundle event, of type installation
      boolean lStat = checkListenerEvents(out, false , 0, true , BundleEvent.STARTED, true, ServiceEvent.REGISTERED, buH, sr1);

      // Object sf = bc.getService(sr1);
      // System.out.println("SERVICE = "+ sf);
      // Object sf1 = bc.getService(sr1);
      // System.out.println("SERVICE = "+ sf1);

      try {
        buH.stop();
      }
      catch (BundleException bexp) {
        out.println("framework test bundle, exception in stop method :" + bexp + " FRAME085A");
        teststatus = false;
      }
      catch (Throwable thr) {
        fail("framework test bundle, exception in stop method :" + thr + " :FRAME085A:Fail");
        teststatus = false;
      }

      if (teststatus == true && buH.getState() == Bundle.RESOLVED && lStat == true) {
        out.println("### framework test bundle :FRAME085A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME085A:FAIL");
      }
    }
  }


  // 22. Install testbundle J, which should throw an exception at start
  //     then check if the framework removes all traces of the bundle
  //     as it registers one service itself before the bundle exception is thrown

  public final static String [] HELP_FRAME110A =  {
    "Install and start bundleJ_test, which should throw an exception at start.",
    "then check if the framework removes all traces of the bundle",
    "as it registers one service (itself) before the bundle exception is thrown"
  };

  class Frame110a extends FWTestCase {
    public void runTest() throws Throwable {
      clearEvents();
      buJ = null;
      boolean lStat1 = false;

      boolean teststatus = true;
      boolean bex = false;
      try {
        buJ = Util.installBundle(bc, "bundleJ_test-1.0.0.jar");
        lStat1 = checkListenerEvents(out, false, 0, true, BundleEvent.INSTALLED, false, 0, buJ, null);
        buJ.start();
        teststatus = false;
      }
      catch (BundleException bexcA) {
        out.println("framework test bundle "+ bexcA +" :FRAME110A:expexted");
        bex = true;
      }
      catch (SecurityException secA) {
        teststatus = false;
        fail("framework test bundle "+ secA +" :FRAME110A:FAIL");
      }

      if (bex != true ) {
        teststatus = false;
        fail("framework test bundle, expected bundle exception missing :FRAME110A");
      }

      if(buJ == null) {
        fail("No installed bundle: :FRAME110A:FAIL");
      }

      // check the listeners for events, expect only a bundle event, of type installation
      boolean lStat2 = checkListenerEvents(out, false , 0, true , BundleEvent.RESOLVED, true, ServiceEvent.UNREGISTERING, buJ, null);

      if (teststatus == true && buJ.getState() == Bundle.RESOLVED && lStat1 == true && lStat2 == true) {
        out.println("### framework test bundle :FRAME110A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME110A:FAIL");
      }

      // Check that no service reference exist from the crashed bundle J.
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleJ_test.BundleJ");
      if (sr1 != null) {
        fail("framework test bundle, service from test bundle J unexpectedly found :FRAME110A:FAIL");
      }

    }
  }

  public final static String [] HELP_FRAME115A =  {
    "Test getDataFile() method."
  };

  class Frame115a extends FWTestCase {
    public void runTest() throws Throwable {

      boolean teststatus = true;
      String filename = "testfile_1";
      byte [] testdata = {1,2,3,4,5};
      File testFile = bc.getDataFile(filename);
      if (testFile != null ) {
        try {
          FileOutputStream fout = new FileOutputStream (testFile);
          fout.write(testdata);
          fout.close();
        }
        catch (IOException ioe) {
          teststatus = false;
          fail("framework test bundle, I/O error on write in FRAME115A " + ioe);
        }

        try {
          FileInputStream fin = new FileInputStream (testFile);
          byte [] indata = new byte [5];
          int incount;
          incount = fin.read(indata);
          fin.close();
          if (incount == 5) {
            for (int i = 0; i< incount; i++ ) {
              if (indata[i] != testdata[i]) {
                teststatus = false;
                fail("framework test bundle FRAME115A, is " + indata [i] + ", should be " + testdata [i]);
              }
            }
          }
          else {
            teststatus = false;
            fail("framework test bundle, I/O data error in FRAME115A");
            out.println("Should be 5 bytes, was " + incount );
          }
        }
        catch (IOException ioe) {
          teststatus = false;
          fail("framework test bundle, I/O error on read in FRAME115A " + ioe);
        }

      }
      else {
        // nothing to test
        fail("framework test bundle, no persistent data storage FRAME115A");
        teststatus = true;
      }

      // Remove testfile_1

      testFile.delete();

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME115A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME115A:FAIL");
      }
    }
  }

  // 24. Test of the AdminPermission class

  public final static String [] HELP_FRAME120A =  {
    "Test of the AdminPermission class"
  };

  class Frame120a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String s1 = null;
      String s2 = null;
      AdminPermission ap1 = new AdminPermission();
      AdminPermission ap2 = null;
      SocketPermission sp1 = new SocketPermission("localhost:6666","listen"); // to test of cats among hermelins
      Object testObject = new Object();

      // constructor and getName method check
      if (!ap1.getName().equals("*")) {
        out.println("framework test bundle, Name of AdminPermission object is " + ap1.getName() + " in FRAME120A");
        fail("framework test bundle, Name of AdminPermission object should be: AdminPermission");
        teststatus = false;
      }

      //this is no longer valid!
      try {
        ap2 = new AdminPermission(s1,s2);
      }
      catch (Exception e) {
        fail("framework test bundle, constructor with two null strings failed in FRAME120A");
        teststatus = false;
      }

      if (ap2 != null && !ap2.getName().equals("AdminPermission")) {
        out.println("framework test bundle, Name of AdminPermission object is " + ap2.getName() + " in FRAME120A");
        out.println("framework test bundle, Name of AdminPermission object should be: AdminPermission");
        teststatus = false;
      }

      // implies method check

      AdminPermission ap3 = new AdminPermission();
      if (!ap1.implies(ap3)) {
        out.println("framework test bundle, implies method failed, returned "+ ap1.implies(ap3) + " should have been true,  FRAME120A");
        teststatus = false;
      }

      if (ap1.implies(sp1)) {
        out.println("framework test bundle, implies method failed, returned "+ ap1.implies(sp1) + " should have been false,  FRAME120A");
        teststatus = false;
      }

      // equals method check

      if (!ap1.equals(ap2)) {
        out.println("framework test bundle, equals method failed, returned "+ ap1.equals(ap2) + " should have been true,  FRAME120A");
        teststatus = false;
      }

      if (ap1.equals(sp1)) {
        out.println("framework test bundle, equals method failed, returned "+ ap1.equals(sp1) + " should have been false,  FRAME120A");
        teststatus = false;
      }

      // newPermissionCollection method check, also check the implemented
      // abstract methods of the PermissionCollection

      PermissionCollection pc1 = ap1.newPermissionCollection();

      if (pc1 != null) {
        pc1.add (ap1);
        boolean trig = false;
        try { // add a permission that is not an AdminPermission
          pc1.add (sp1);
          trig = true;
        }
        catch (RuntimeException ex1) {
          trig = false;
        }
        if (trig == true) {
          out.println("framework test bundle, add method on PermissionCollection failed, FRAME120A");
          out.println("permission with type different from AdminPermission succeded unexpectedly FRAME120A");
          teststatus = false;
        }

        pc1.add (ap2);

        if (!pc1.implies(ap3)) {
          out.println("framework test bundle, implies method on PermissionCollection failed, FRAME120A");
          teststatus = false;
        }
        boolean hit1 = false;
        int count = 0;

        /* The enumeration of this kind of permission is a bit weird
           as it actually returns a new AdminPermission if an
           element has been added, thus the test becomes a bit odd.

           This comes from looking at the source code, i.e a bit of
           peeking behind the screens but in this case it was
           necessary as the functionality is not possible to
           understand otherwise.
        */
        for (Enumeration e = pc1.elements(); e.hasMoreElements(); ) {
          AdminPermission ap4 = (AdminPermission) e.nextElement();
          // out.println("DEBUG framework test bundle, got AdminPermission " + ap4 +" FRAME120A");
          count++;
          if (ap4 != null) { hit1 = true;}
        }
        if (hit1 != true || count != 1) {
          teststatus = false;
          out.println("framework test bundle, elements method on PermissionCollection failed, FRAME120A");
          if (hit1 != true) {
            out.println("framework test bundle, no AdminPermission retrieved, FRAME120A");
          }

          if (count != 1) {
            out.println("framework test bundle, number of entered objects: 1, number retrieved: " + count + " , FRAME120A");
          }
        }
        if (pc1.isReadOnly() == true) {
          teststatus = false;
          out.println("framework test bundle, isReadOnly method on PermissionCollection is: "+pc1.isReadOnly() +" should be false, FRAME120A");
        }

        pc1.setReadOnly();
        if (pc1.isReadOnly() == false) {
          teststatus = false;
          out.println("framework test bundle, isReadOnly method on PermissionCollection is: "+pc1.isReadOnly() +" should be true, FRAME120A");
        }


      } else {
        out.println("framework test bundle, newPermissionCollection method failed, returned null, FRAME120A");
        teststatus = false;
      }

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME120A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME120A:FAIL");
      }
    }
  }
  // 25. Test of the PackagePermission class

  public final static String [] HELP_FRAME125A =  {
    "Test of the PackagePermission class"
  };

  class Frame125a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String validName = "valid.name.test";
      String validName1 = "valid.name.test1";
      String validName2 = "valid.name.test2";
      String invalidName = null;
      String validAction = PackagePermission.EXPORT+","+PackagePermission.IMPORT;
      String invalidAction = "apa";

      PackagePermission pp1 = null;
      PackagePermission pp2 = null;
      PackagePermission pp3 = null;
      PackagePermission pp4 = null;

      // constructor check

      try {
        pp1 = new PackagePermission(validName,validAction);
      }
      catch (RuntimeException re) {
        out.println("framework test bundle, PackagePermission constructor("+ validName +"," + validAction + ") failed, in FRAME125A");
        teststatus = false;
      }

      try {
        pp1 = new PackagePermission(invalidName,validAction);
        out.println("framework test bundle, PackagePermission constructor("+ invalidName +"," + validAction + ") succeded unexpected, in FRAME125A");
        teststatus = false;
      }
      catch (RuntimeException re) { }

      try {
        pp1 = new PackagePermission(validName,invalidAction);
        out.println("framework test bundle, PackagePermission constructor("+ validName +"," + invalidAction + ") succeded unexpected, in FRAME125A");
        teststatus = false;
      }
      catch (RuntimeException re) { }

      try {
        pp1 = new PackagePermission(invalidName,invalidAction);
        out.println("framework test bundle, PackagePermission constructor("+ invalidName +"," + invalidAction + ") succeded unexpected, in FRAME125A");
        teststatus = false;
      }
      catch (RuntimeException re) { }

      // equals test
      pp1 = new PackagePermission(validName,validAction);
      pp2 = new PackagePermission(validName,validAction);

      if (!pp1.equals(pp2)) {
        out.println("framework test bundle, PackagePermission equals method failed for identical objects, in FRAME125A");
        teststatus = false;
      }

      pp3 = new PackagePermission(validName,PackagePermission.IMPORT);
      if (pp1.equals(pp3)) {
        out.println("framework test bundle, PackagePermission equals method failed for non identical objects, in FRAME125A");
        teststatus = false;
      }

      pp3 = new PackagePermission(validName2,validAction);
      if (pp1.equals(pp3)) {
        out.println("framework test bundle, PackagePermission equals method failed for non identical objects, in FRAME125A");
        teststatus = false;
      }

      // getActions test
      pp1 = new PackagePermission(validName,PackagePermission.IMPORT);
      pp2 = new PackagePermission(validName,PackagePermission.EXPORT);
      pp3 = new PackagePermission(validName,PackagePermission.IMPORT+","+PackagePermission.EXPORT);
      pp4 = new PackagePermission(validName,PackagePermission.EXPORT+","+PackagePermission.IMPORT);

      if (!pp1.getActions().equals(PackagePermission.IMPORT)) {
        out.println("framework test bundle, PackagePermission getActions method failed in FRAME125A");
        out.println("framework test bundle, expected: "+PackagePermission.IMPORT);
        out.println("framework test bundle, got:" + pp1.getActions());
        teststatus = false;
      }

      if (!pp2.getActions().equals(PackagePermission.EXPORT+","+PackagePermission.IMPORT)) {
        out.println("framework test bundle, PackagePermission getActions method failed in FRAME125A");
        out.println("framework test bundle, expected: "+PackagePermission.EXPORT);
        out.println("framework test bundle, got:" + pp2.getActions());
        teststatus = false;
      }

      if (!pp3.getActions().equals(PackagePermission.EXPORT+","+PackagePermission.IMPORT)) {
        out.println("framework test bundle, PackagePermission getActions method failed in FRAME125A");
        out.println("framework test bundle, expected: "+PackagePermission.EXPORT +","+PackagePermission.IMPORT);
        out.println("framework test bundle, got:" + pp3.getActions());
        teststatus = false;
      }

      if (!pp4.getActions().equals(PackagePermission.EXPORT+","+PackagePermission.IMPORT)) {
        out.println("framework test bundle, PackagePermission getActions method failed in FRAME125A");
        out.println("framework test bundle, expected: "+PackagePermission.EXPORT +","+PackagePermission.IMPORT);
        out.println("framework test bundle, got:" + pp4.getActions());
        teststatus = false;
      }

      // implies test
      boolean impstatus = true;
      pp1 = new PackagePermission(validName,PackagePermission.IMPORT);
      pp2 = new PackagePermission(validName,PackagePermission.EXPORT);

      impstatus = impstatus && implyCheck (out, true,  pp2, pp1); // export implies import
      impstatus = impstatus && implyCheck (out, false, pp1, pp2); // import does not imply export

      pp1 = new PackagePermission("test1.*",PackagePermission.EXPORT);
      pp2 = new PackagePermission("test2.*",PackagePermission.EXPORT);

      impstatus = impstatus && implyCheck (out, false, pp2, pp1); // different packet names, implication = false
      impstatus = impstatus && implyCheck (out, false, pp1, pp2); // different packet names, implication = false

      pp1 = new PackagePermission("test1.*",PackagePermission.EXPORT);
      pp2 = new PackagePermission("test1.a",  PackagePermission.EXPORT);

      impstatus = impstatus && implyCheck (out, false, pp2, pp1); // test1.a does not imply test1.*, implication = false
      impstatus = impstatus && implyCheck (out, true,  pp1, pp2); // test1.* implies test1.a, implication = true

      pp1 = new PackagePermission("test1.*",PackagePermission.EXPORT);
      pp2 = new PackagePermission("test1.a",PackagePermission.IMPORT);
      pp3 = new PackagePermission("test1.*",PackagePermission.IMPORT);
      pp4 = new PackagePermission("test1.a",PackagePermission.EXPORT);

      impstatus = impstatus && implyCheck (out, true,  pp1, pp1); // test1.* & export implies        test1.* & export, implication = true
      impstatus = impstatus && implyCheck (out, true,  pp1, pp2); // test1.* & export implies        test1.a & import, implication = true
      impstatus = impstatus && implyCheck (out, true,  pp1, pp3); // test1.* & export implies        test1.* & import, implication = true
      impstatus = impstatus && implyCheck (out, true,  pp1, pp4); // test1.* & export implies        test1.a & export, implication = true

      impstatus = impstatus && implyCheck (out, false, pp2, pp1); // test1.a & import does not imply test1.* & export, implication = false
      impstatus = impstatus && implyCheck (out, true,  pp2, pp2); // test1.a & import implies        test1.a & import, implication = true
      impstatus = impstatus && implyCheck (out, false, pp2, pp3); // test1.a & import does not imply test1.* & import, implication = false
      impstatus = impstatus && implyCheck (out, false, pp2, pp4); // test1.a & import does not imply test1.a & export, implication = false

      impstatus = impstatus && implyCheck (out, false, pp3, pp1); // test1.* & import does not imply test1.* & export, implication = false
      impstatus = impstatus && implyCheck (out, true,  pp3, pp2); // test1.* & import implies        test1.a & import, implication = true
      impstatus = impstatus && implyCheck (out, true,  pp3, pp3); // test1.* & import implies        test1.* & import, implication = true
      impstatus = impstatus && implyCheck (out, false, pp3, pp4); // test1.* & import does not imply test1.a & export, implication = false

      impstatus = impstatus && implyCheck (out, false, pp4, pp1); // test1.a & export does not imply test1.* & export, implication = false
      impstatus = impstatus && implyCheck (out, true,  pp4, pp2); // test1.a & export implies        test1.a & import, implication = true
      impstatus = impstatus && implyCheck (out, false, pp4, pp3); // test1.a & export does not imply test1.* & import, implication = false
      impstatus = impstatus && implyCheck (out, true,  pp4, pp4); // test1.a & export implies        test1.a & export, implication = true

      // newPermissionCollection tests

      PackagePermission pp5 = new PackagePermission("test1.*",PackagePermission.EXPORT);
      PackagePermission pp6 = new PackagePermission("test1.a",PackagePermission.IMPORT);
      PackagePermission pp7 = new PackagePermission("test2.*",PackagePermission.IMPORT);
      PackagePermission pp8 = new PackagePermission("test2.a",PackagePermission.EXPORT);
      PackagePermission pp9 = new PackagePermission("test3.a",PackagePermission.EXPORT);

      PermissionCollection pc1 = pp5.newPermissionCollection();
      if (pc1 != null) {
        int count = 0;
        boolean b1 = false;
        boolean b2 = false;
        boolean b3 = false;
        boolean b4 = false;

        try {
          pc1.add(pp5);
          pc1.add(pp6);
          pc1.add(pp7);
          pc1.add(pp8);

          for (Enumeration e = pc1.elements(); e.hasMoreElements(); ) {
            PackagePermission ptmp = (PackagePermission) e.nextElement();
            // out.println("DEBUG framework test bundle, got AdminPermission " + ptmp +" FRAME125A");
            count++;
            if (ptmp == pp5) { b1 = true;}
            if (ptmp == pp6) { b2 = true;}
            if (ptmp == pp7) { b3 = true;}
            if (ptmp == pp8) { b4 = true;}
          }
          if (count != 4 || b1 != true || b2 != true || b3 != true || b4 != true) {
            teststatus = false;
            out.println("framework test bundle, elements method on PermissionCollection failed, FRAME125A");

            if (count != 4) {
              out.println("framework test bundle, number of entered PackagePermissions: 4, number retrieved: " + count + " , FRAME125A");
            }
          }
          boolean ipcstat = true;
          ipcstat = ipcstat && implyCheck (out, true,  pc1, pp5); // test1.* & export implies        test1.* & export, implication = true
          ipcstat = ipcstat && implyCheck (out, false, pc1, pp9); // test1.* & export does not imply test3.a & export, implication = false

          if (ipcstat != true) {
            teststatus = false;
          }

        }
        catch (Throwable ex) {
          out.println("DEBUG framework test bundle, Exception " + ex);
          ex.printStackTrace();
          ex.printStackTrace(out);
        }

      } else {
        // teststatus = false;
        out.println("framework test bundle, newPermissionsCollection method on PackagePermission returned null,FRAME125A");
      }

      if (teststatus == true && impstatus == true) {
        out.println("### framework test bundle :FRAME125A:PASS");
      }
      else {
        fail("### framework test bundle :FRAME125A:FAIL");
      }
    }
  }

  // 26. Test of the ServicePermission class

  public final static String [] HELP_FRAME130A =  {
    "Test of the ServicePermission class"
  };

  class Frame130a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String validClass = "valid.class.name";
      String validClass1 = "valid.class.name.1";
      String validClass2 = "valid.class.name.2";
      String invalidClass = null;
      String validAction = ServicePermission.GET+","+ServicePermission.REGISTER;
      String invalidAction = "skunk";

      ServicePermission sp1 = null;
      ServicePermission sp2 = null;
      ServicePermission sp3 = null;
      ServicePermission sp4 = null;
      ServicePermission sp5 = null;
      ServicePermission sp6 = null;
      ServicePermission sp7 = null;
      ServicePermission sp8 = null;

      // constructor check

      try {
        sp1 = new ServicePermission (validClass,validAction);
      }
      catch (RuntimeException re) {
        out.println("framework test bundle, ServicePermission constructor("+ validClass +"," + validAction + ") failed, in FRAME130A");
        teststatus = false;
      }

      try {
        sp1 = new ServicePermission(invalidClass,validAction);
        out.println("framework test bundle, ServicePermission constructor("+ invalidClass +"," + validAction + ") succeded unexpected, in FRAME130A");
        teststatus = false;
      }
      catch (RuntimeException re) { }

      try {
        sp1 = new ServicePermission(validClass,invalidAction);
        out.println("framework test bundle, ServicePermission constructor("+ validClass +"," + invalidAction + ") succeded unexpected, in FRAME130A");
        teststatus = false;
      }
      catch (RuntimeException re) { }

      try {
        sp1 = new ServicePermission(invalidClass,invalidAction);
        out.println("framework test bundle, ServicePermission constructor("+ invalidClass +"," + invalidAction + ") succeded unexpected, in FRAME130A");
        teststatus = false;
      }
      catch (RuntimeException re) { }

      // equals test
      sp1 = new ServicePermission(validClass,validAction);
      sp2 = new ServicePermission(validClass,validAction);

      if (!sp1.equals(sp2)) {
        out.println("framework test bundle, ServicePermission equals method failed for identical objects, in FRAME130A");
        teststatus = false;
      }

      sp3 = new ServicePermission(validClass,ServicePermission.GET);
      if (sp1.equals(sp3)) {
        out.println("framework test bundle, ServicePermission equals method failed for non identical objects, in FRAME130A");
        teststatus = false;
      }

      sp3 = new ServicePermission(validClass2,validAction);
      if (sp1.equals(sp3)) {
        out.println("framework test bundle, ServicePermission equals method failed for non identical objects, in FRAME130A");
        teststatus = false;
      }

      // getActions test

      sp1 = new ServicePermission(validClass,ServicePermission.GET);
      sp2 = new ServicePermission(validClass,ServicePermission.REGISTER);
      sp3 = new ServicePermission(validClass,ServicePermission.GET+","+ServicePermission.REGISTER);
      sp4 = new ServicePermission(validClass,ServicePermission.REGISTER+","+ServicePermission.GET);

      if (!sp1.getActions().equals(ServicePermission.GET)) {
        out.println("framework test bundle, ServicePermission getActions method failed in FRAME130A");
        out.println("framework test bundle, expected: "+ServicePermission.GET);
        out.println("framework test bundle, got: " + sp1.getActions());
        teststatus = false;
      }

      if (!sp2.getActions().equals(ServicePermission.REGISTER)) {
        out.println("framework test bundle, ServicePermission getActions method failed in FRAME130A");
        out.println("framework test bundle, expected: "+ServicePermission.REGISTER);
        out.println("framework test bundle, got: " + sp2.getActions());
        teststatus = false;
      }

      if (!sp3.getActions().equals(ServicePermission.GET+","+ServicePermission.REGISTER)) {
        out.println("framework test bundle, ServicePermission getActions method failed in FRAME130A");
        out.println("framework test bundle, expected: "+ServicePermission.GET +","+ServicePermission.REGISTER);
        out.println("framework test bundle, got: " + sp3.getActions());
        teststatus = false;
      }

      if (!sp4.getActions().equals(ServicePermission.GET+","+ServicePermission.REGISTER)) {
        out.println("framework test bundle, ServicePermission getActions method failed in FRAME130A");
        out.println("framework test bundle, expected: "+ServicePermission.GET +","+ServicePermission.REGISTER);
        out.println("framework test bundle, got: " + sp4.getActions());
        teststatus = false;
      }

      // implies test

      boolean impstatus = true;
      sp1 = new ServicePermission(validClass,ServicePermission.GET);
      sp2 = new ServicePermission(validClass,ServicePermission.REGISTER);

      impstatus = impstatus && implyCheck (out, false, sp2, sp1); // get does not imply register
      impstatus = impstatus && implyCheck (out, false, sp1, sp2); // register does not imply get

      sp1 = new ServicePermission("validClass1.*", ServicePermission.REGISTER+","+ServicePermission.GET);
      sp2 = new ServicePermission("validClass2.*", ServicePermission.REGISTER+","+ServicePermission.GET);

      impstatus = impstatus && implyCheck (out, false, sp2, sp1); // different class names, implication = false
      impstatus = impstatus && implyCheck (out, false, sp1, sp2); // different class names, implication = false

      sp1 = new ServicePermission("validClass1.*", ServicePermission.REGISTER+","+ServicePermission.GET);
      sp2 = new ServicePermission("validClass1.a", ServicePermission.REGISTER+","+ServicePermission.GET);

      impstatus = impstatus && implyCheck (out, false, sp2, sp1); // validClass1.a does not imply validClass1.*, implication = false
      impstatus = impstatus && implyCheck (out, true,  sp1, sp2); // validClass1.* implies validClass1.a, implication = true

      sp1 = new ServicePermission("test1.*",ServicePermission.REGISTER);
      sp2 = new ServicePermission("test1.*",ServicePermission.GET);
      sp3 = new ServicePermission("test1.*",ServicePermission.REGISTER+","+ServicePermission.GET);
      sp4 = new ServicePermission("test1.a",ServicePermission.REGISTER);
      sp5 = new ServicePermission("test1.a",ServicePermission.GET);
      sp6 = new ServicePermission("test1.a",ServicePermission.REGISTER+","+ServicePermission.GET);

      impstatus = impstatus && implyCheck (out, true,  sp1, sp1); // test1.* & register implies      test1.* & register,
      impstatus = impstatus && implyCheck (out, false, sp1, sp2); // test1.* & register implies not  test1.* & get,
      impstatus = impstatus && implyCheck (out, false, sp1, sp3); // test1.* & register implies not  test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, true,  sp1, sp4); // test1.* & register implies      test1.a & register,
      impstatus = impstatus && implyCheck (out, false, sp1, sp5); // test1.* & register implies not  test1.a & get,
      impstatus = impstatus && implyCheck (out, false, sp1, sp6); // test1.* & register implies not  test1.a & reg & g,

      impstatus = impstatus && implyCheck (out, false, sp2, sp1); // test1.* & get      implies not  test1.* & register,
      impstatus = impstatus && implyCheck (out, true,  sp2, sp2); // test1.* & get      implies      test1.* & get,
      impstatus = impstatus && implyCheck (out, false, sp2, sp3); // test1.* & get      implies not  test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, false, sp2, sp4); // test1.* & get      implies      test1.a & register,
      impstatus = impstatus && implyCheck (out, true,  sp2, sp5); // test1.* & get      implies      test1.a & get,
      impstatus = impstatus && implyCheck (out, false, sp2, sp6); // test1.* & get      implies not  test1.a & reg & g,

      impstatus = impstatus && implyCheck (out, true, sp3, sp1); // test1.* & get&reg  implies      test1.* & register,
      impstatus = impstatus && implyCheck (out, true, sp3, sp2); // test1.* & get&reg  implies      test1.* & get,
      impstatus = impstatus && implyCheck (out, true, sp3, sp3); // test1.* & get&reg  implies      test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, true, sp3, sp4); // test1.* & get&reg  implies      test1.a & register,
      impstatus = impstatus && implyCheck (out, true, sp3, sp5); // test1.* & get&reg  implies      test1.a & get,
      impstatus = impstatus && implyCheck (out, true, sp3, sp6); // test1.* & get&reg  implies      test1.a & reg & g,

      impstatus = impstatus && implyCheck (out, false, sp4, sp1); // test1.a & reg  implies not   test1.* & register,
      impstatus = impstatus && implyCheck (out, false, sp4, sp2); // test1.a & reg  implies not   test1.* & get,
      impstatus = impstatus && implyCheck (out, false, sp4, sp3); // test1.a & reg  implies not   test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, true,  sp4, sp4); // test1.a & reg  implies       test1.a & register,
      impstatus = impstatus && implyCheck (out, false, sp4, sp5); // test1.a & reg  implies not   test1.a & get,
      impstatus = impstatus && implyCheck (out, false, sp4, sp6); // test1.a & reg  implies not   test1.a & reg & g,

      impstatus = impstatus && implyCheck (out, false, sp5, sp1); // test1.a & get  implies not   test1.* & register,
      impstatus = impstatus && implyCheck (out, false, sp5, sp2); // test1.a & get  implies not   test1.* & get,
      impstatus = impstatus && implyCheck (out, false, sp5, sp3); // test1.a & get  implies not   test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, false, sp5, sp4); // test1.a & get  implies not   test1.a & register,
      impstatus = impstatus && implyCheck (out, true,  sp5, sp5); // test1.a & get  implies       test1.a & get,
      impstatus = impstatus && implyCheck (out, false, sp5, sp6); // test1.a & get  implies not   test1.a & reg & g,

      impstatus = impstatus && implyCheck (out, false, sp6, sp1); // test1.a & get & reg implies not   test1.* & register,
      impstatus = impstatus && implyCheck (out, false, sp6, sp2); // test1.a & get & reg implies not   test1.* & get,
      impstatus = impstatus && implyCheck (out, false, sp6, sp3); // test1.a & get & reg implies not   test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, true,  sp6, sp4); // test1.a & get & reg implies       test1.a & register,
      impstatus = impstatus && implyCheck (out, true,  sp6, sp5); // test1.a & get & reg implies       test1.a & get,
      impstatus = impstatus && implyCheck (out, true,  sp6, sp6); // test1.a & get & reg implies       test1.a & reg & g,

      sp7 = new ServicePermission("test2.a",ServicePermission.REGISTER+","+ServicePermission.GET);
      sp8 = new ServicePermission("*",ServicePermission.REGISTER+","+ServicePermission.GET);

      impstatus = impstatus && implyCheck (out, false, sp7, sp1); // test2.a & get & reg implies not   test1.* & register,
      impstatus = impstatus && implyCheck (out, false, sp7, sp2); // test2.a & get & reg implies not   test1.* & get,
      impstatus = impstatus && implyCheck (out, false, sp7, sp3); // test2.a & get & reg implies not   test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, false, sp7, sp4); // test2.a & get & reg implies not   test1.a & register,
      impstatus = impstatus && implyCheck (out, false, sp7, sp5); // test2.a & get & reg implies not   test1.a & get,
      impstatus = impstatus && implyCheck (out, false, sp7, sp6); // test2.a & get & reg implies not   test1.a & reg & g,

      impstatus = impstatus && implyCheck (out, true,  sp8, sp1); // * & get & reg implies       test1.* & register,
      impstatus = impstatus && implyCheck (out, true,  sp8, sp2); // * & get & reg implies       test1.* & get,
      impstatus = impstatus && implyCheck (out, true,  sp8, sp3); // * & get & reg implies       test1.* & reg & get,
      impstatus = impstatus && implyCheck (out, true,  sp8, sp4); // * & get & reg implies       test1.a & register,
      impstatus = impstatus && implyCheck (out, true,  sp8, sp5); // * & get & reg implies       test1.a & get,
      impstatus = impstatus && implyCheck (out, true,  sp8, sp6); // * & get & reg implies       test1.a & reg & g,

      PermissionCollection pc1 = sp1.newPermissionCollection();
      if (pc1 != null) {
        int count = 0;
        boolean b1 = false;
        boolean b2 = false;
        boolean b3 = false;
        boolean b4 = false;

        try {
          pc1.add(sp1);
          pc1.add(sp2);
          pc1.add(sp3);
          pc1.add(sp4);

          // the combination of these four servicepermissions should create
          // a servicecollection that implies the following

          boolean ipcstat = true;
          ipcstat = ipcstat && implyCheck (out, true,  pc1, sp1); // permission is in collection
          ipcstat = ipcstat && implyCheck (out, true,  pc1, sp2); // permission is in collection
          ipcstat = ipcstat && implyCheck (out, true,  pc1, sp3); // permission is in collection
          ipcstat = ipcstat && implyCheck (out, true,  pc1, sp4); // permission is in collection
          ipcstat = ipcstat && implyCheck (out, true,  pc1, sp5); // permission is in collection
          ipcstat = ipcstat && implyCheck (out, false, pc1, sp7); // permission is not in collection

          if (ipcstat != true) {
            teststatus = false;
          }

        }
        catch (Throwable ex) {
          out.println("DEBUG framework test bundle, Exception " + ex);
          ex.printStackTrace();
          ex.printStackTrace(out);
        }
      } else {
        // teststatus = false;
        out.println("framework test bundle, newPermissionsCollection method on ServicePermission returned null,FRAME130A");
      }

      if (teststatus == true && impstatus == true) {
        out.println("### framework test bundle :FRAME130A:PASS");
      }
      else {
        out.println("### framework test bundle :FRAME130A:FAIL");
      }
    }
  }




  public final static String [] HELP_FRAME160A =  {
    "Test bundle resource retrieval."
  };

  class Frame160a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean pass = true;

      Bundle buR = null;
      Bundle buR1 = null;
      try {
        try {
          buR = Util.installBundle(bc, "bundleR_test-1.0.0.jar");
        } catch (BundleException e) {
          out.println("Failed install R: " + e.getNestedException() + ", in FRAME160A");
          pass = false;
        }

        try {
          buR1 = Util.installBundle(bc, "bundleR1_test-1.0.0.jar");
        } catch (BundleException e) {
          pass = false;
          fail("Failed install R1: " + e.getNestedException() + ", in FRAME160A:FAIL");
        }

        try {
          buR.start();
        } catch (BundleException e) {
          e.getNestedException().printStackTrace(out);
          pass = false;
          fail("Failed start of R in FRAME160A:FAIL");
        }

        if (pass == true) {
          out.println("### framework test bundle :FRAME160A:PASS");
        } else {
          fail("### framework test bundle :FRAME160A:FAIL");
        }
      } finally {

        if (buR != null) {
          try {
            buR.uninstall();
          } catch (BundleException ignore) { }
        }
        if (buR1 != null) {
          try {
            buR1.uninstall();
          } catch (BundleException ignore) { }
        }
      }
    }
  }

  public final static String [] HELP_FRAME161A =  {
    "Test bundle resource retrieval from boot class path; "
    +" a resource in-side the java package."
  };

  class Frame161a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean pass = true;

      String resourceName = "java/lang/Thread.class";

      URL url1 = bc.getBundle().getResource(resourceName);
      URL url2 = this.getClass().getClassLoader().getResource(resourceName);

      //System.out.println("URL from bundle.getResource() = "+url1);
      //System.out.println("URL from classLoader.getResource() = "+url2);

      // Bundle.getResource() as well as ClassLoader.getResource()
      // should return resources according to the class space
      // (classpath), i.e., delegate to parent class loader before
      // searching its own paths.
      assertNotNull("bundle.getResource(\"" +resourceName+"\")" , url1);
      assertNotNull("bundleClassLoader.getResource(\""+resourceName+"\")",url2);
      assertEquals("Same URL returned from booth classloader and bundle",
                   url1,url2);
      if (pass == true) {
        out.println("### framework test bundle :FRAME161A:PASS");
      } else {
        fail("### framework test bundle :FRAME161A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME162A =  {
    "Test bundle resource retrieval from boot class path; "
    +" a resource outside the java package."
  };

  class Frame162a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean pass = true;

      // The Any class have been present since 1.2
      String resourceName = "org/omg/CORBA/Any.class";

      URL url1 = bc.getBundle().getResource(resourceName);
      URL url2 = this.getClass().getClassLoader().getResource(resourceName);

      //System.out.println("URL from bundle.getResource() = "+url1);
      //System.out.println("URL from classLoader.getResource() = "+url2);

      // Bundle.getResource() and BundleClassLoader.getResource()
      // should both return resources according to the class space
      // (classpath), i.e., don't delgate to parent in this case since
      // the resource does not belong to the java-package.
      assertNull("bundle.getResource(\"" +resourceName+"\")" , url1);
      assertNull("bundleClassLoader.getResource(\""+resourceName+"\")",url2);

      if (pass == true) {
        out.println("### framework test bundle :FRAME162A:PASS");
      } else {
        fail("### framework test bundle :FRAME162A:FAIL");
      }
    }
  }

  public final static String [] HELP_FRAME163A =  {
    "Test bundle resource retrieval from boot class path; "
    +" a resource via boot delegation."
  };

  class Frame163a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean pass = true;

      // The Context class have been present in Java SE since 1.3
      String resourceName = "javax/naming/Context.class";

      URL url1 = bc.getBundle().getResource(resourceName);
      URL url2 = this.getClass().getClassLoader().getResource(resourceName);

      //System.out.println("URL from bundle.getResource() = "+url1);
      //System.out.println("URL from classLoader.getResource() = "+url2);

      // Bundle.getResource() as well as ClassLoader.getResource()
      // should return resources according to the class space
      // (classpath), i.e., delegate to parent class loader before
      // searching its own paths.
      assertNotNull("bundle.getResource(\"" +resourceName+"\")" , url1);
      assertNotNull("bundleClassLoader.getResource(\""+resourceName+"\")",url2);
      assertEquals("Same URL returned from booth classloader and bundle",
                   url1,url2);

      if (pass == true) {
        out.println("### framework test bundle :FRAME163A:PASS");
      } else {
        fail("### framework test bundle :FRAME163A:FAIL");
      }
    }
  }

  boolean callReturned;
  Integer doneSyncListener;
  String errorFrame170;

  public final static String [] HELP_FRAME170A =  {
    "Test of ServiceReference.getUsingBundles() and SynchronousBundleListener."
  };

  class Frame170a extends FWTestCase {
    public void runTest() throws Throwable {
      ServiceRegistration tsr = null;
      Bundle buQ = null;
      Bundle buQ1 = null;
      try {
        boolean pass = true;

        // Make sure that there are no pending BundleListener calls.
        try {
          Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        BundleListener bl1 = new BundleListener() {
            public void bundleChanged(BundleEvent be) {
              if (doneSyncListener.intValue() == 0) {
                errorFrame170 = "Called BundleListener before SBL";
              }
              synchronized (doneSyncListener) {
                doneSyncListener = new Integer(doneSyncListener.intValue() + 1);
              }
            }
          };
        bc.addBundleListener(bl1);
        SynchronousBundleListener bl2 = new SynchronousBundleListener() {
            public void bundleChanged(BundleEvent be) {
              if (callReturned) {
                errorFrame170 = "Returned from bundle operation before SBL was done";
              } else {
                try {
                  Thread.sleep(1000);
                } catch (InterruptedException ignore) {}
                if (callReturned) {
                  errorFrame170 = "Returned from bundle operation before SBL was done";
                }
              }
              synchronized (doneSyncListener) {
                doneSyncListener = new Integer(doneSyncListener.intValue() + 1);
              }
            }
          };
        bc.addBundleListener(bl2);
        BundleListener bl3 = new BundleListener() {
            public void bundleChanged(BundleEvent be) {
              if (doneSyncListener.intValue() == 0) {
                errorFrame170 = "Called BundleListener before SBL";
              }
              synchronized (doneSyncListener) {
                doneSyncListener = new Integer(doneSyncListener.intValue() + 1);
              }
            }
          };
        bc.addBundleListener(bl3);

        doneSyncListener = new Integer(0);
        callReturned = false;
        errorFrame170 = null;
        try {
          buQ = Util.installBundle(bc, "bundleQ_test-1.0.0.jar");
          callReturned = true;
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignore) {}
          if (errorFrame170 != null) {
            out.println(errorFrame170 + ", in FRAME170A");
            pass = false;
          }
          if (doneSyncListener.intValue() != 3) {
            out.println("Failed to call all bundleListeners (only " + doneSyncListener +
                        "), in FRAME170A");
            pass = false;
          }
        } catch (BundleException e) {
          out.println("Failed install Q: " + e.getNestedException() + ", in FRAME170A");
          pass = false;
        }
        bc.removeBundleListener(bl1);
        bc.removeBundleListener(bl2);
        bc.removeBundleListener(bl3);
        try {
          buQ1 = bc.installBundle("Q1", bc.getBundle().getResource("bundleQ_test-1.0.0.jar").openStream());
        } catch (BundleException e) {
          pass = false;
          fail("Failed install Q1: " + e.getNestedException() + ", in FRAME170A:FAIL");
        } catch (IOException e) {
          pass = false;
          fail("Failed to open Q1 url: " + e + ", in FRAME170A:FAIL");
        }

        Hashtable props = new Hashtable();
        props.put("bundleQ", "secret");

        try {
          tsr = bc.registerService ("java.lang.Object", this, props);
        } catch (Exception e) {
          fail("Failed to register service in FRAME170A:FAIL");
        }

        if (tsr.getReference().getUsingBundles() != null) {
          pass = false;
          String ids = "" + tsr.getReference().getUsingBundles()[0].getBundleId();
          for (int i=1; i<tsr.getReference().getUsingBundles().length; i++) {
            ids += "," + tsr.getReference().getUsingBundles()[i].getBundleId();
          }
          fail("Unknown bundle (" + ids + ") using service in FRAME170A:FAIL");
        }
        try {
          buQ.start();
        } catch (BundleException e) {
          e.getNestedException().printStackTrace(out);
          pass = false;
          fail("Failed start of Q in FRAME170A:FAIL");
        }
        Bundle[] bs = tsr.getReference().getUsingBundles();
        if (bs.length != 1) {
          pass = false;
          fail("Wrong number (" + bs.length +
               " not 1) of bundles using service in FRAME170A:FAIL");
        } else if (bs[0] != buQ) {
          pass = false;
          fail("Unknown bundle using service instead of bundle Q in FRAME170A:FAIL");
        }
        try {
          buQ1.start();
        } catch (BundleException e) {
          e.getNestedException().printStackTrace(out);
          pass = false;
          fail("Failed start of Q1 in FRAME170A:FAIL");
        }
        bs = tsr.getReference().getUsingBundles();
        if (bs.length != 2) {
          pass = false;
          fail("Wrong number (" + bs.length +
               " not 2) of bundles using service in FRAME170A:FAIL");
        } else if ((bs[0] != buQ || bs[1] != buQ1) &&
                   (bs[1] != buQ || bs[0] != buQ1)) {
          pass = false;
          fail("Unknown bundle using service instead of bundle Q and Q1 in FRAME170A:FAIL");
        }
        try {
          buQ.stop();
        } catch (BundleException e) {
          e.getNestedException().printStackTrace(out);
          pass = false;
          fail("Failed stop of Q in FRAME170A:FAIL");
        }
        bs = tsr.getReference().getUsingBundles();
        if (bs.length != 1) {
          pass = false;
          fail("After stop wrong number (" + bs.length +
               " not 1) of bundles using service in FRAME170A:FAIL");
        } else if (bs[0] != buQ1) {
          pass = false;
          fail("Unknown bundle using service instead of bundle Q1 in FRAME170A:FAIL");
        }

        // Check that we haven't called any bundle listeners
        if (doneSyncListener.intValue() != 3) {
          pass = false;
          fail("Called bundle listeners after removal (" + doneSyncListener +
               "), in FRAME170A:FAIL");
        }

        if (pass == true) {
          out.println("### framework test bundle :FRAME170A:PASS");
        } else {
          fail("### framework test bundle :FRAME170A:FAIL");
        }
      } finally {
        // clean up
        if (tsr != null) {
          tsr.unregister();
        }
        try {
          buQ.uninstall();
        } catch (BundleException ignore) { }
        try {
          buQ1.uninstall();
        } catch (BundleException ignore) { }
      }

    }
  }

  // 175A. Resource integrity when reading different resources of a bundle

  public final static String [] HELP_FRAME175A =  {
    "Check of resource integrity when using intermixed reading of differenent resources from bundleR2_test."
  };

  class Frame175a extends FWTestCase {
    public void runTest() throws Throwable {
      buR2 = null;
      boolean teststatus = true;
      try {
        buR2 = Util.installBundle(bc, "bundleR2_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        teststatus = false;
        fail("framework test bundle "+ bexcA +" :FRAME175A:FAIL");
      }
      catch (SecurityException secA) {
        teststatus = false;
        fail("framework test bundle "+ secA +" :FRAME175A:FAIL");
      }
      // Now read resources A and B intermixed
      // A is 50 A:s , B is 50 B:s
      int a_cnt1 = 0;
      int a_cnt2 = 0;
      int b_cnt = 0;
      byte [] a1 = new byte[50];
      byte [] b1 = new byte[50];
      String A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
      String B = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

      InputStream is1 = null ;
      InputStream is2 = null ;

      try {
        URL u1 = buR2.getResource("org/knopflerfish/bundle/bundleR2_test/A");
        is1 = u1.openStream();
        a_cnt1 = is1.read(a1, 0, 10);

        URL u2 = buR2.getResource("org/knopflerfish/bundle/bundleR2_test/B");
        is2 = u2.openStream();
        b_cnt = is2.read(b1, 0, 50);

        // continue reading from is1 now, what do we get ??

        a_cnt2 = is1.read(a1, a_cnt1, 50-a_cnt1);
      }
      catch (Throwable tt) {
        tt.printStackTrace();
        teststatus  = false;
        fail("Failed to read resource" + " ,FRAME175A:FAIL");
      }
      finally {
        try {
          is1.close();
          is2.close();
        }
        catch (Throwable tt) {
          out.println("Failed to read close input streams" + " ,FRAME175A:FAIL");
          tt.printStackTrace();
          teststatus  = false;
        }
      }

      if (A.compareTo(new String(a1)) != 0) {
        teststatus = false;
        fail("framework test bundle expected: " + A  + "\n got: " + new String(a1) +" :FRAME175A:FAIL");
      }
      if (B.compareTo(new String(b1)) != 0) {
        teststatus = false;
        fail("framework test bundle expected: " + B  + "\n got: " + new String(b1) +" :FRAME175A:FAIL");
      }

      // check the resulting events

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME175A:PASS");
      } else {
        fail("### framework test bundle :FRAME175A:FAIL");
      }
    }
  }


  // 180. Resource integrity when reading different resources of a bundle
  //      on the top level in the namespace of the bundle.

  public final static String [] HELP_FRAME180A =  {
    "Check of resource on top of bundle name space from bundleR3_test."
  };

  class Frame180a extends FWTestCase {
    public void runTest() throws Throwable {
      buR3 = null;
      boolean teststatus = true;
      try {
        buR3 = Util.installBundle(bc, "bundleR3_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        out.println("framework test bundle "+ bexcA +" :FRAME180A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        out.println("framework test bundle "+ secA +" :FRAME180A:FAIL");
        teststatus = false;
      }
      // Now read resources A
      // A is 50 A:s
      int a_cnt1 = 0;
      int b_cnt = 0;
      byte [] a1 = new byte[50];
      String A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

      InputStream is1 = null ;

      try {
        URL u1 = buR3.getResource("/A");
        is1 = u1.openStream();
        a_cnt1 = is1.read(a1);
      }
      catch (Throwable tt) {
        out.println("Failed to read resource" + " ,FRAME180A:FAIL");
        tt.printStackTrace();
        teststatus  = false;
      }
      finally {
        try {
          if (is1 != null) {
            is1.close();
          }
        }
        catch (Throwable tt) {
          out.println("Failed to close input streams" + " ,FRAME180A:FAIL");
          tt.printStackTrace();
          teststatus  = false;
        }
      }

      if (A.compareTo(new String(a1)) != 0) {
        teststatus = false;
        out.println("framework test bundle expected: " + A  + "\n got: " + xlateData(a1) +" :FRAME180A:FAIL");
      }

      // check the resulting events

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME180A:PASS");
      } else {
        fail("### framework test bundle :FRAME180A:FAIL");
      }
    }
  }

  // 181. Resource integrity when reading different resources of a bundle
  //      on the top level in the namespace of the bundle. (180 without leading / in resource name)

  public final static String [] HELP_FRAME181A =  {
    "Check of resource on top of bundle name space from bundleR3_test."
  };

  class Frame181a extends FWTestCase {
    public void runTest() throws Throwable {
      buR3 = null;
      boolean teststatus = true;
      try {
        buR3 = Util.installBundle(bc, "bundleR3_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        out.println("framework test bundle "+ bexcA +" :FRAME181A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        out.println("framework test bundle "+ secA +" :FRAME181A:FAIL");
        teststatus = false;
      }
      // Now read resources A
      // A is 50 A:s
      int a_cnt1 = 0;
      int b_cnt = 0;
      byte [] a1 = new byte[50];
      String A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

      InputStream is1 = null ;

      try {
        URL u1 = buR3.getResource("A");
        is1 = u1.openStream();
        a_cnt1 = is1.read(a1);
      }
      catch (Throwable tt) {
        out.println("Failed to read resource" + " ,FRAME181A:FAIL");
        tt.printStackTrace();
        teststatus  = false;
      }
      finally {
        try {
          if (is1 != null) {
            is1.close();
          }
        }
        catch (Throwable tt) {
          out.println("Failed to close input streams" + " ,FRAME181A:FAIL");
          tt.printStackTrace();
          teststatus  = false;
        }
      }

      if (A.compareTo(new String(a1)) != 0) {
        teststatus = false;
        out.println("framework test bundle expected: " + A  + "\n got: " + xlateData(a1) +" :FRAME181A:FAIL");
      }

      // check the resulting events

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME181A:PASS");
      } else {
        fail("### framework test bundle :FRAME181A:FAIL");
      }
    }
  }

  // 185. Resource integrity when reading different resources of a bundle
  //      on the top level in the namespace of the bundle.

  public final static String [] HELP_FRAME185A =  {
    "Check of resource on top of bundle name space from bundleR4_test,",
    "that has an unresolvable package imported"
  };

  class Frame185a extends FWTestCase {
    public void runTest() throws Throwable {
      buR4 = null;
      boolean teststatus = true;
      try {
        buR4 = Util.installBundle(bc, "bundleR4_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        out.println("framework test bundle "+ bexcA +" :FRAME185A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        out.println("framework test bundle "+ secA +" :FRAME185A:FAIL");
        teststatus = false;
      }
      // Now read resources A
      // A is 50 A:s
      int a_cnt1 = 0;
      int b_cnt = 0;
      byte [] a1 = new byte[50];
      String A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

      InputStream is1 = null ;

      try {
        URL u1 = buR4.getEntry("/A");
        is1 = u1.openStream();
        a_cnt1 = is1.read(a1);
      }
      catch (Throwable tt) {
        out.println("Failed to read resource" + " ,FRAME185A:FAIL");
        tt.printStackTrace();
        teststatus  = false;
      }
      finally {
        try {
          if (is1 != null) {
            is1.close();
          }
        }
        catch (Throwable tt) {
          tt.printStackTrace();
          teststatus  = false;
          fail("Failed to close input streams" + " ,FRAME185A:FAIL");
        }
      }

      if (A.compareTo(new String(a1)) != 0) {
        teststatus = false;
        fail("framework test bundle expected: " + A  + "\n got: " + xlateData(a1) +" :FRAME185A:FAIL");
      }

      // check the resulting events

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME185A:PASS");
      } else {
        fail("### framework test bundle :FRAME185A:FAIL");
      }
    }
  }

  // 186. Resource integrity when reading different resources of a bundle
  //      on the top level in the namespace of the bundle. (185 without leading / in resource name)

  public final static String [] HELP_FRAME186A =  {
    "Check of resource on top of bundle name space from bundleR4_test,",
    "that has an unresolvable package imported"
  };

  class Frame186a extends FWTestCase {
    public void runTest() throws Throwable {
      buR4 = null;
      boolean teststatus = true;
      try {
        buR4 = Util.installBundle(bc, "bundleR4_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        teststatus = false;
        fail("framework test bundle "+ bexcA +" :FRAME186A:FAIL");
      }
      catch (SecurityException secA) {
        teststatus = false;
        fail("framework test bundle "+ secA +" :FRAME186A:FAIL");
      }
      // Now read resources A
      // A is 50 A:s
      int a_cnt1 = 0;
      int b_cnt = 0;
      byte [] a1 = new byte[50];
      String A = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

      InputStream is1 = null ;

      try {
        URL u1 = buR4.getEntry("A");
        is1 = u1.openStream();
        a_cnt1 = is1.read(a1);
      }
      catch (Throwable tt) {
        fail("Failed to read resource" + " ,FRAME186A:FAIL");
        tt.printStackTrace();
        teststatus  = false;
      }
      finally {
        try {
          if (is1 != null) {
            is1.close();
          }
        }
        catch (Throwable tt) {
          tt.printStackTrace();
          teststatus  = false;
          fail("Failed to close input streams" + " ,FRAME186A:FAIL");
        }
      }

      if (A.compareTo(new String(a1)) != 0) {
        teststatus = false;
        fail("framework test bundle expected: " + A  + "\n got: " + xlateData(a1) +" :FRAME186A:FAIL");
      }

      // check the resulting events

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME186A:PASS");
      } else {
        fail("### framework test bundle :FRAME186A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAME190A =  {
    "Check of resource access inside bundle name space from bundleR5_test and",
    "bundleR6_test, that bundleR5_test exports a resource that is accessed via ",
    "the bundle context of bundleR6_test"
  };

  class Frame190a extends FWTestCase {
    public void runTest() throws Throwable {
      buR5 = null;
      boolean teststatus = true;
      try {
        buR5 = Util.installBundle(bc, "bundleR5_test-1.0.0.jar");
        teststatus = true;
      }
      catch (BundleException bexcA) {
        fail("framework test bundle "+ bexcA +" :FRAME190A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME190A:FAIL");
        teststatus = false;
      }

      buR6 = null;
      try {
        buR6 = Util.installBundle(bc, "bundleR6_test-1.0.0.jar");
        teststatus = teststatus && true;
      }
      catch (BundleException bexcA) {
        fail("framework test bundle "+ bexcA +" :FRAME190A:FAIL");
        teststatus = false;
      }
      catch (SecurityException secA) {
        fail("framework test bundle "+ secA +" :FRAME190A:FAIL");
        teststatus = false;
      }

      // out.println("Bundle R5 state: " + buR5.getState());
      // out.println("Bundle R6 state: " + buR6.getState());
      // Now try to access resource in R5, which should give null
      // as bundle R5 is not resolved yet
      //
      int a_cnt1 = 0;
      int b_cnt = 0;
      byte [] a1 = new byte[50];
      String A = "R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5R5";

      InputStream is1 = null ;

      // this part removed since the spec is really vague about
      // resolving inside of getResource()
      if(false) {
        // if buR6 not has been automatically resolved, verify that
        // getResource doesn't do it. (yes there might be a timing p
        // problem here)
        if(buR6.getState() == Bundle.INSTALLED) {
          URL u1 = null;;
          try {
            u1 = buR6.getResource("/org/knopflerfish/bundle/bundleR5_test/R5");
          }
          catch (Throwable tt) {
            fail("Failed to read resource" + " ,FRAME190A:FAIL");
            tt.printStackTrace();
            teststatus  = false;
          }
          if (u1 != null) {
            teststatus  = false;
            fail("Unexpected access to resource in bundle R5 " + " ,FRAME190A:FAIL");
          }
        }
      }

      // Start R6, so that it defintely gets state resolved
      try {
        buR6.start();
      }
      catch (BundleException be) {
        fail("Failed to start bundle R6 " + be.toString() +  " ,FRAME190A:FAIL");
        teststatus  = false;
      }

      // out.println("Bundle R5 state: " + buR5.getState());
      // out.println("Bundle R6 state: " + buR6.getState());

      try {
        URL u2 = buR6.getResource("/org/knopflerfish/bundle/bundleR5_test/R5");
        is1 = u2.openStream();
        a_cnt1 = is1.read(a1);
      }
      catch (Throwable tt) {
        fail("Failed to read resource '/org/knopflerfish/bundle/bundleR5_test/R5' via bundleR6_test" + " ,FRAME190A:FAIL");
        tt.printStackTrace();
        teststatus  = false;
      }
      finally {
        try {
          if (is1 != null) {
            is1.close();
          }
        }
        catch (Throwable tt) {
          fail("Failed to close input streams" + " ,FRAME190A:FAIL");
          tt.printStackTrace();
          teststatus  = false;
        }
      }

      if (A.compareTo(new String(a1)) != 0) {
        teststatus = false;
        fail("framework test bundle expected: " + A  + "\n got: " + xlateData(a1) +" :FRAME190A:FAIL");
      }


      // check the resulting events

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME190A:PASS");
      } else {
        fail("### framework test bundle :FRAME190A:FAIL");
      }
    }
  }





  // 210. Check that no deadlock is created when
  //      using synchronous event handling, by
  //      creating threads that register and unregister
  //      services in a syncronous way

  public final static String [] HELP_FRAME210A =  {
    "Deadlock test when using synchronous serviceChange listener and updating different threads."
  };

  class Frame210a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;

      RegServThread rst = null;
      RegListenThread rlt = null;

      try {
        rst = new RegServThread (bc,out);
        rlt = new RegListenThread (bc,out);
      }
      catch (Exception ex) {
        teststatus = false;
        fail("### framework test bundle :FRAME210A exception");
        ex.printStackTrace(out);
      }

      // Start the service registering thread
      int ID = 1;
      Thread t1 = new Thread(rst, "thread id= " + ID + " ");
      t1.start();
      // System.out.println("Start of thread " + String.valueOf(ID));

      // Start the listener thread
      ID = 2;
      Thread t2 = new Thread(rlt, "thread id= " + ID + " ");
      t2.start();
      // System.out.println("Start of thread " + String.valueOf(ID));

      // sleep to give the threads t1 and t2 time to create a possible deadlock.
      try {
        Thread.sleep(1500);
      }
      catch (Exception ex) {
        out.println("### framework test bundle :FRAME210A exception");
        ex.printStackTrace(out);
      }

      // Ask the listener thread if it succeded to make a synchroized serviceChange

      if (rlt.getStatus() != true) {
        teststatus = false;
        fail("### framework test bundle :FRAME210A failed to execute sychnronized serviceChanged()");
      }

      // Ask the registering thread if it succeded to make a service update

      if (rst.getStatus() != true) {
        teststatus = false;
        fail("### framework test bundle :FRAME210A failed to execute sychnronized service update");
      }

      // Stop the threads

      rst.stop();
      rlt.stop();

      if (teststatus == true) {
        out.println("### framework test bundle :FRAME210A:PASS");
      } else {
        fail("### framework test bundle :FRAME210A:FAIL");
      }
    }
  }

  class Frame211a extends FWTestCase {
            public void runTest() throws Throwable {

            //existing directory
                Enumeration enume = buF.getEntryPaths("/");
                if(enume == null ){
                          fail("GetEntryPaths did not retrieve the correct number of elements");
                    }
                int i = 0;
                while(enume.hasMoreElements()){
                  i++;
                  enume.nextElement();
                }
                if(i != 3 && i != 2){ //manifest gets skipped
                  fail("GetEntryPaths did not retrieve the correct number of elements");
                }

                //another existing directory
                enume = buF.getEntryPaths("/org/knopflerfish/bundle/");
                if(enume == null ){
                  fail("GetEntryPaths did not retrieve the correct number of elements");
                }
                i = 0;
                while(enume.hasMoreElements()){
                  i++;
                  enume.nextElement();
                }
                if(i != 1 ){
                  fail("GetEntryPaths did not retrieve the correct number of elements");
                }

                //existing file, non-directory, ending with slash
                enume = buF.getEntryPaths("/BundF.class/");
                if(enume != null ){
                  fail("GetEntryPaths did not retrieve the correct number of elements");
                }

            //existing file, non-directory
                enume = buF.getEntryPaths("/BundF.class");
                if(enume != null ){
                  fail("GetEntryPaths did not retrieve the correct number of elements");
                }

                //non-existing file
                enume = buF.getEntryPaths("/e");
                if(enume != null){
                  fail("GetEntryPaths did not retrieve the correct number of elements");
                }

                //dir with only one entry
                enume = buF.getEntryPaths("/OSGI-INF");
                if(enume == null ){
                        fail("GetEntryPaths did not retrieve the correct number of elements");
                }
                i = 0;
                while(enume.hasMoreElements()){
                  i++;
                  enume.nextElement();
                }
                if(i != 1){
                  fail("GetEntryPaths did not retrieve the correct number of elements");
                }


                if (buF != null) {
                        try {
                            buF.uninstall();
                        }
                        catch (BundleException ignore) {
                        }
                }

                Dictionary dict = buF.getHeaders();
                if(!dict.get(Constants.BUNDLE_SYMBOLICNAME).equals("org.knopflerfish.bundle.bundleF_test")){
                  fail("framework test bundle, " +  Constants.BUNDLE_SYMBOLICNAME + " header does not have right value:FRAME211A:FAIL");
                }



                dict = buF.getHeaders("");
                if(!dict.get(Constants.BUNDLE_DESCRIPTION).equals("%description")){
                  fail("framework test bundle, " +  Constants.BUNDLE_DESCRIPTION + " header does not have raw value, " + dict.get(Constants.BUNDLE_DESCRIPTION) + ":FRAME211A:FAIL");
                }



            }
  }

  // General status check functions
  // prevent control characters to be printed
  private String xlateData(byte [] b1) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < b1.length ; i++) {
      if (-128 <= b1[i] && b1[i] < 0) {
        sb.append(new String(b1, i, 1));
      }
      if (0 <= b1[i] && b1[i] < 32) {
        sb.append("^");
        sb.append(String.valueOf(b1[i]));
      } else {
        if (32 <= b1[i] && b1[i] < 127) {
          sb.append(new String(b1, i, 1));
        }
      }
    }
    return sb.toString();
  }

  // Check that the expected implications occur
  public boolean implyCheck (Object _out, boolean expected, Permission p1, Permission p2) {
    boolean result = true;
    if (p1.implies(p2) == expected) {
      result = true;
    } else {
      out.println("framework test bundle, ...Permission implies method failed");
      out.println("Permission p1: " + p1.toString());
      out.println("Permission p2: " + p2.toString());
      result = false;
    }
    // out.println("DEBUG implies method in FRAME125A");
    // out.println("DEBUG p1: " + p1.toString());
    // out.println("DEBUG p2: " + p2.toString());
    return result;
  }

  public boolean implyCheck (Object _out, boolean expected, PermissionCollection p1, Permission p2) {
    boolean result = true;
    if (p1.implies(p2) == expected) {
      result = true;
    } else {
      out.println("framework test bundle, ...Permission implies method failed");
      out.println("Permission p1: " + p1.toString());
      out.println("Permission p2: " + p2.toString());
      result = false;
    }
    return result;
  }


  /* Interface implementations for this class */

  public java.lang.Object getConfigurationObject() {
    return this;
  }


  // Check that the expected events has reached the listeners and reset the events in the listeners
  private boolean checkListenerEvents(Object _out, boolean fwexp, int fwtype, boolean buexp, int butype, boolean sexp, int stype, Bundle bunX, ServiceReference servX ) {
    boolean listenState = true; // assume everything will work

    // Sleep a while to allow events to arrive
    try {
      Thread.sleep(eventDelay);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    if (fwexp == true) {
      if (fListen.getEvent() != null) {
        if (fListen.getEvent().getType() != fwtype || fListen.getEvent().getBundle() != bunX) {
          System.out.println("framework test bundle, wrong type of framework event/bundle : " +  fListen.getEvent().getType());
          System.out.println("framework test bundle, event was from bundle: " + fListen.getEvent().getBundle());
          Throwable th1 = fListen.getEvent().getThrowable();
          if (th1 != null) {
            System.out.println("framework test bundle, exception was: " + th1);
          }
          listenState = false;
        }
      }
      else {
        System.out.println("framework test bundle, missing framework event");
        listenState = false;
      }
    }
    else {
      if (fListen.getEvent() != null) {
        listenState = false;
        System.out.println("framework test bundle, unexpected framework event: " +  fListen.getEvent().getType());
        System.out.println("framework test bundle, event was from bundle: " + fListen.getEvent().getBundle());
        Throwable th1 = fListen.getEvent().getThrowable();
        if (th1 != null) {
          System.out.println("framework test bundle, exception was: " + th1);
        }
      }
    }

    if (buexp == true) {
      if (bListen.getEvent() != null) {
        if (bListen.getEvent().getType() != butype || bListen.getEvent().getBundle() != bunX) {
          System.out.println("framework test bundle, wrong type of bundle event/bundle: " +  bListen.getEvent().getType());
          System.out.println("framework test bundle, event was from bundle: " + bListen.getEvent().getBundle().getLocation());
          listenState = false;
        }
      }
      else {
        System.out.println("framework test bundle, missing bundle event");
        listenState = false;
      }
    }
    else {
      if (bListen.getEvent() != null) {
        listenState = false;
        System.out.println("framework test bundle, unexpected bundle event: " +  bListen.getEvent().getType());
        System.out.println("framework test bundle, event was from bundle: " + bListen.getEvent().getBundle());
      }
    }


    if (sexp == true) {
      if (sListen.getEvent() != null) {
        if (servX != null) {
          if (sListen.getEvent().getType() != stype || servX != sListen.getEvent().getServiceReference() ) {
            System.out.println("framework test bundle, wrong type of service event: " +  sListen.getEvent().getType());
            listenState = false;
          }
        }
        else { // ignore from which service reference the event came
          if (sListen.getEvent().getType() != stype ) {
            System.out.println("framework test bundle, wrong type of service event: " +  sListen.getEvent().getType());
            listenState = false;
          }
        }
      }
      else {
        System.out.println("framework test bundle, missing service event");
        listenState = false;
      }
    }
    else {
      if (sListen.getEvent() != null) {
        listenState = false;
        System.out.println("framework test bundle, unexpected service event: " +  sListen.getEvent().getType());
      }
    }

    fListen.clearEvent();
    bListen.clearEvent();
    sListen.clearEvent();
    return listenState;
  }

  // Check that the expected events has reached the listeners and reset the events in the listeners
  private boolean checkSyncListenerEvents(Object _out, boolean buexp, int butype, Bundle bunX, ServiceReference servX ) {
    boolean listenState = true; // assume everything will work

    // Sleep a while to allow events to arrive
    try {
      Thread.sleep(eventDelay);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    if (buexp == true) {
      if (syncBListen.getEvent() != null) {
        if (syncBListen.getEvent().getType() != butype || syncBListen.getEvent().getBundle() != bunX) {
          System.out.println("framework test bundle, wrong type of sync bundle event/bundle: " +  syncBListen.getEvent().getType());
          System.out.println("framework test bundle, event was from bundle: " + syncBListen.getEvent().getBundle().getLocation());
          listenState = false;
        }
      }
      else {
        System.out.println("framework test bundle, missing sync bundle event");
        listenState = false;
      }
    }
    else {
      if (syncBListen.getEvent() != null) {
        listenState = false;
        System.out.println("framework test bundle, unexpected sync bundle event: " +  syncBListen.getEvent().getType());
        System.out.println("framework test bundle, event was from bundle: " + syncBListen.getEvent().getBundle());
      }
    }

    syncBListen.clearEvent();
    return listenState;
  }

  private void clearEvents() {
    fListen.clearEvent();
    bListen.clearEvent();
    syncBListen.clearEvent();
    sListen.clearEvent();
  }
  // Get the bundle that caused the event
  private Bundle getFEBundle () {
    if (fListen.getEvent() != null) {
      return fListen.getEvent().getBundle();
    }
    else {
      return null;
    }
  }

  private Bundle getBEBundle () {
    if (bListen.getEvent() != null) {
      return bListen.getEvent().getBundle();
    }
    else {
      return null;
    }
  }

  // So that other bundles in the test may get the base url
  public String getBaseURL() {
    return test_url_base;
  }

  // to access test service methods via reflection
  private void bundleLoad (Object _out, ServiceReference sr, String bundle) {
    Method m;
    Class c, parameters[];

    Object obj1 = bc.getService(sr);
    // System.out.println("servref  = "+ sr);
    // System.out.println("object = "+ obj1);

    Object[] arguments = new Object[1];
    arguments[0] = bundle;              // the bundle to load packages from

    c = obj1.getClass();
    parameters = new Class[1];
    parameters[0] = arguments[0].getClass();

    // System.out.println("Parameters [0] " + parameters[0].toString());

    try {
      m = c.getMethod("tryPackage", parameters);
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
  public synchronized void putEvent (String device, String method, Integer value) {
    // System.out.println("putEvent" + device + " " + method + " " + value);
    events.addElement(new devEvent(device, method, value));
  }

  class devEvent {
    String dev;
    String met;
    int val;

    public devEvent (String dev, String met , Integer val) {
      this.dev = dev;
      this.met = met;
      this.val = val.intValue();
    }

    public devEvent (String dev, String met , int val) {
      this.dev = dev;
      this.met = met;
      this.val = val;
    }

    public String getDevice() {
      return dev;
    }

    public String getMethod() {
      return met;
    }

    public int getValue() {
      return val;
    }

  }

  private boolean checkEvents(Object _out, Vector expevents, Vector events) {
    boolean state = true;
    if (events.size() != expevents.size()) {
      state = false;
      out.println("Real events");
      for (int i = 0; i< events.size() ; i++) {
        devEvent dee = (devEvent) events.elementAt(i);
        out.print("Bundle " + dee.getDevice());
        out.print(" Method " + dee.getMethod());
        out.println(" Value " + dee.getValue());
      }
      out.println("Expected events");
      for (int i = 0; i< expevents.size() ; i++) {
        devEvent dee = (devEvent) expevents.elementAt(i);
        out.print("Bundle " + dee.getDevice());
        out.print(" Method " + dee.getMethod());
        out.println(" Value " + dee.getValue());
      }
    }
    else {
      for (int i = 0; i< events.size() ; i++) {
        devEvent dee = (devEvent) events.elementAt(i);
        devEvent exp = (devEvent) expevents.elementAt(i);
        if (!(dee.getDevice().equals(exp.getDevice()) && dee.getMethod().equals(exp.getMethod()) && dee.getValue() == exp.getValue())) {
          out.println("Event no = " + i);
          if (!(dee.getDevice().equals(exp.getDevice()))) {
            out.println ("Bundle is " + dee.getDevice() +  " should be " + exp.getDevice());
          }
          if (!(dee.getMethod().equals(exp.getMethod()))) {
            out.println ("Method is " + dee.getMethod() +  " should be " + exp.getMethod());
          }
          if (!(dee.getValue() == exp.getValue())) {
            out.println ("Value is " + dee.getValue() +  " should be " + exp.getValue());
          }
          state = false;
        }
      }
    }
    return state;
  }

  private String getStateString(int bundleState) {
    switch (bundleState) {
    case 0x01: return "UNINSTALLED";
    case 0x02: return "INSTALLED";
    case 0x04: return "RESOLVED";
    case 0x08: return "STARTING";
    case 0x10: return "STOPPING";
    case 0x20: return "ACTIVE";

    default: return "Unknow state";

    }
  }

  class FrameworkListener implements org.osgi.framework.FrameworkListener {
    FrameworkEvent fwe;
    public void frameworkEvent(FrameworkEvent evt) {
      this.fwe = evt;
      // System.out.println("FrameworkEvent: "+ evt.getType());
    }
    public FrameworkEvent getEvent() {
      return fwe;
    }
    public void clearEvent() {
      fwe = null;
    }
  }

  class ServiceListener implements org.osgi.framework.ServiceListener {
    ServiceEvent serve = null;
    public void serviceChanged(ServiceEvent evt) {
      this.serve = evt;
      // System.out.println("ServiceEvent: " + evt.getType());
    }
    public ServiceEvent getEvent() {
      return serve;
    }
    public void clearEvent() {
      serve = null;
    }

  }

  class BundleListener implements org.osgi.framework.BundleListener {
    BundleEvent bunEvent = null;

    public void bundleChanged (BundleEvent evt) {
      this.bunEvent = evt;
      // System.out.println("BundleEvent: "+ evt.getType());
    }
    public BundleEvent getEvent() {
      return bunEvent;
    }
    public void clearEvent() {
      bunEvent = null;
    }
  }

  class SyncBundleListener implements SynchronousBundleListener {
            BundleEvent bunEvent = null;

            public void bundleChanged (BundleEvent evt) {
              if(evt.getType() == BundleEvent.STARTING || evt.getType() == BundleEvent.STOPPING)
                  this.bunEvent = evt;
              // System.out.println("BundleEvent: "+ evt.getType());
            }
            public BundleEvent getEvent() {
              return bunEvent;
            }
            public void clearEvent() {
              bunEvent = null;
            }
  }

}
