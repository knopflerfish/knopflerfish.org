/*
 * Copyright (c) 2004,2006 KNOPFLERFISH project
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

public class PackageAdminTestSuite extends TestSuite implements FrameworkTest {
  BundleContext bc;
  Bundle bu;
  Bundle buA;
  Bundle buB;

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
  ServiceListener sListen;

  Properties props = System.getProperties();
  String lineseparator = props.getProperty("line.separator");
  Vector events = new Vector();			// vector for events from test bundles
  Vector expevents = new Vector();		// comparision vector


  String packServiceName = "org.osgi.service.packageadmin.PackageAdmin";

  PrintStream out = System.out;

  public PackageAdminTestSuite (BundleContext bc) {
    super("PackageAdminTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new Frame0187a());
    addTest(new Frame0200a());
    addTest(new Frame215a());
    addTest(new Frame220b());
    addTest(new Cleanup());
  }
 

  class FWTestCase extends TestCase {
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

  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
      PackageAdmin pa = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
      if (pa == null) {
        fail("Failed to get PackageAdmin service");
      }

      try {
        pa.refreshPackages(null);
      } catch (Exception e) {
        fail("Failed to refresh packages");
      }
             

      buA = Util.installBundle(bc, "bundleA_test-1.0.0.jar");
      buB = Util.installBundle(bc, "bundleB_test-1.0.0.jar");
      assertNotNull(buA);
      assertNotNull(buB);
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
	buA,
	buB,
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

  public final static String [] HELP_FRAME187A =  {
    "Install resimp_test and resexp_test",
    "and do <count> updates refreshes",
    "<count> defaults to 1"
  };
  
  class Frame0187a extends FWTestCase {
    public void runTest() throws Throwable {
      Hashtable opts = new Hashtable();
      boolean teststatus = true;
      String cntStr = (String) opts.get("count");
      int cnt = 1;
      try {
	cnt = Integer.parseInt(cntStr);
      }
      catch (NumberFormatException nfe) {
	cnt = 1;
	out.println ("Param: " + cntStr);
	out.println("Count defaulted to: " + String.valueOf(cnt) + " :FRAME187A");
      }

      buRimp = null;
      buRexp = null;


      // Clear any old events
      events.removeAllElements();
      expevents.removeAllElements();

      // Register and start the two test bundles 
      try {
	buRexp = Util.installBundle(bc, "resexp_test-1.0.0.jar");
	buRimp = Util.installBundle(bc, "resimp_test-1.0.0.jar");
	buRexp.start();
	buRimp.start();
      }
      catch (BundleException bexcR) {
	teststatus = false;
	fail("framework test bundle "+ bexcR +" :FRAME187A:FAIL");
      }
      catch (SecurityException secR) {
	teststatus = false;
	fail("framework test bundle "+ secR +" :FRAME187A:FAIL");
      }

      // Get packageadmin service


      PackageAdmin packService = null;

      try {
	packService = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
	if (packService == null) {
	  teststatus  = false;
	  fail("Got null service " + packServiceName + " in FRAME187A:FAIL");
	} 
      }
      catch (Exception ex) {
	teststatus  = false;
	fail("Got exception: " + ex + " in FRAME187A:FAIL");
      }

      // Set the bundle to refresh
      Bundle [] buRefresh = {buRexp};

      for (int i = 0; i < cnt ; i++) {
	// Update the buRexp bundle
	try {
	  buRexp.update(bc.getBundle().getResource("resexp_test-1.0.0.jar").openStream());;
	}
	catch (BundleException bex) {
	  teststatus  = false;
	  fail("Got exception: " + bex.getNestedException() + " in FRAME187A:FAIL");
	}
	catch (Exception e) {
	  teststatus  = false;
	  e.printStackTrace();
	  fail("Got exception " + e + " + in FRAME187A:FAIL");
	}

	// Refresh packages 
	packService.refreshPackages(buRefresh);

	// sleep to give the packService.refreshPackages thread time to do its task
	try {
	  Thread.sleep(300);
	}
	catch (Exception ex) {
	  out.println("### framework test bundle :FRAME187A exception");
	  ex.printStackTrace(out);
	}
      }

      // If somthing broke, there might be events to check

      // expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleP_test.BundP", "constructor, bundleP_test File reference: true", 1));
      boolean event = checkEvents(out, expevents, events);
      if (event != true) {
	fail("Mismatch between expected and real events FRAME187A:FAIL");
	teststatus = false;
      }

      if (teststatus == true) {
	out.println("### framework test bundle :FRAME187A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME187A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAME200A =  {
    "Try to get a package admin service",
    "do simple introspection checks of this test bundle"
  };
  
  class Frame0200a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean pass = true;
      String packServiceName = "org.osgi.service.packageadmin.PackageAdmin";
      String expectedExportedPackageName = "org.knopflerfish.service.framework_test";
      String expectedVersion = "1.0"; // also accept "1.0.0"
      PackageAdmin packService = null;
      Bundle fw_test = bc.getBundle();	// this bundle
      
      try {
	packService = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
	if (packService == null) {
	  pass  = false;
	  fail("Got null service " + packServiceName + " in FRAME200A:FAIL");
	} else {
	  ExportedPackage exp1 = packService.getExportedPackage("org.knopflerfish.service.framework_test");
	  if (exp1 != null) {
	    if (!exp1.getName().equals(expectedExportedPackageName)) {
	      pass  = false;
	      fail("Got exported package " + exp1.getName() + " expected " + expectedExportedPackageName + " in FRAME200A:FAIL");
	    }
            
	  } else {
	    pass  = false;
	    fail("Got null exported package in FRAME200A:FAIL");
	  }
	  // Now try the other method, get an array of exported packages,
	  // with one expected package 
	  
	  ExportedPackage [] exp2 = packService.getExportedPackages(fw_test);
	  
	  if (exp2 != null) {
	    if (exp2.length != 1) {
	      pass  = false;
	      for (int i = 0; i < exp2.length ; i++ ) {
		fail("Got exported package at " + i + " " + exp2[i].getName() + " in FRAME200A:FAIL");
	      }
	    } else {
	      if (!exp2[0].getName().equals(expectedExportedPackageName)) {
		pass  = false;
		fail("Got exported package " + exp2[0].getName() + " expected " + expectedExportedPackageName + " in FRAME200A:FAIL");
	      }
	    }
	  } else {
	    pass  = false;
	    fail("Got null exported package array in FRAME200A:FAIL");
	  }
	  
	  
	  // Test the methods of the ExportedPackage interface
	  
	  Bundle b1 = exp1.getExportingBundle();
	  if (b1 != fw_test) {
	    pass = false; 
	    fail("Got bundle reference mismatch from getExportingBundle() method in FRAME200A:FAIL");
	  }
	  
	  String version = exp1.getSpecificationVersion();
	  if (!(expectedVersion.equals(version) ||
	      (expectedVersion + ".0").equals(version))) {
	    pass = false; 
	    fail("Expected version: " + expectedVersion + " got: " + version + "in FRAME200A:FAIL");
	  }
	  
	  // Expect no importers of this package
	  
	  Bundle [] b2 = exp1.getImportingBundles();
	  
	  if (b2.length != 0) {
	    pass = false;
	    for (int i = 0; i < b2.length; i++) {
	      out.println("Package importing bundles: " + b2[i].getLocation());
	    }
	    fail("Got more then expected importing bundles, FRAME200A:FAIL");
	  }
	  
	  // no removal should be pending
	  boolean pending = exp1.isRemovalPending();
	  
	  if (pending == true) {
	    pass = false;
	    fail("Got pending removal: " + pending + " expected false, FRAME200A:FAIL");
	  } 
	  
	  // Exercise refreshPackages using bundleA_test and bundleB_test
	  
	  Bundle [] buRefresh = {buA, buB};
	  packService.refreshPackages(buRefresh); 
	  
	}
      }
      catch (Throwable tt) {
	fail("Failed to get " + packServiceName + " service, exception " + tt + " ,FRAME200A:FAIL");
	pass  = false;
      }
      
      
      if (pass == true) {
	out.println("### framework test bundle :FRAME200A:PASS");
      } else {
	fail("### framework test bundle :FRAME200A:FAIL");
      }
    }
  }

  // 215A. More advanced test of the packageAdmin interface
  //       Check of version selection at refresh.
  
  public final static String USAGE_FRAME215A = "";
  public final static String [] HELP_FRAME215A =  {
    "Use the framework refresh mechanism to see that version handling of",
    "exported/imported packages work, in the case that the new version of",
    "the package is exported by another bundle than the previous version."
  };
  
  class Frame215a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String packServiceName = "org.osgi.service.packageadmin.PackageAdmin";
      String packageName = "org.knopflerfish.service.bundleP1_test";
      String expectedVersion = "1.0";
      PackageAdmin packService = null;
      Bundle fw_test = bc.getBundle();    // this bundle
      
      
      packService = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
      if (packService == null) {
	teststatus  = false;
	fail("Got null service " + packServiceName + " in FRAME215A:FAIL");
      }
      
      try {
	buP1 = null;
	buP2 = null;
	buP3 = null;
	
	long bidP1 = 0;
	long bidP2 = 0;
	long bidP3 = 0;
	
	// Install test bundles P1 and P2, start the later
	// Check that it imports from P1
	// out.println("bundleP1_test-1.0.0.jar");
	
	try {
	  buP1 = Util.installBundle(bc, "bundleP1_test-1.0.0.jar");
	  buP2 = Util.installBundle(bc, "bundleP2_test-1.0.0.jar");
	  // buP3 = Util.installBundle(bc, "bundleP3_test-1.0.0.jar");
	  buP2.start();
	  
	  bidP1 = buP1.getBundleId();
	  bidP2 = buP2.getBundleId();
	  teststatus = true;
	}
	catch (BundleException bexcA) {
	  fail("framework test bundle "+ bexcA +" :FRAME215A:FAIL");
	  teststatus = false;
	}
	catch (SecurityException secA) {
	  fail("framework test bundle "+ secA +" :FRAME215A:FAIL");
	  teststatus = false;
	}
	
	// Check the exported packages
	// In this case the P2 test bundle should import from P1, with version 1.0
	
	boolean importState = checkExportVersion (out, buP1, buP2, packageName, "1.0.0");
	if (importState != true) {
	  teststatus = false;
	  fail("framework test bundle , P1 to P2 export not as expected :FRAME215A:FAIL");
	}
	
	// Now load bundle P3,that exports the same package as P1 but with version 3
	
	try {
	buP3 = Util.installBundle(bc, "bundleP3_test-1.0.0.jar");
	bidP3 = buP3.getBundleId();
	}
	catch (BundleException bexcA) {
	  fail("framework test bundle "+ bexcA +" :FRAME215A:FAIL");
	  teststatus = false;
	}
	catch (SecurityException secA) {
	  fail("framework test bundle "+ secA +" :FRAME215A:FAIL");
	  teststatus = false;
	}
	
	importState = checkExportVersion (out, buP1, buP2, packageName, "1.0.0");
	if (importState != true) {
	  teststatus = false;
	  fail("framework test bundle , P1 to P2 export not as expected after P3 installation :FRAME215A:FAIL");
	}
	
	// Exercise refreshPackages on bundle P3
	// In this case nothing should happen, as that packet version 
	// exported by P3 is not used anywhere
	
	Bundle [] buRefresh = {buP3};
	packService.refreshPackages(buRefresh);
	
	// Give refresh thread time to run 
	try {
	  Thread.sleep(2000);
	}
	catch (InterruptedException ix) {}
	
	importState = checkExportVersion (out, buP1, buP2, packageName, "1.0.0");
	if (importState != true) {
	  teststatus = false;
	  fail("framework test bundle , P1 to P2 export not as expected after P3 installation and refresh of P3 :FRAME215A:FAIL");
	}
	
	// Exercise refreshPackages on bundle P1
	// In this case the P2 bundle should change to import from P3
	// as that bundle is has a higher version number in its exported 
	// package.
	
	Bundle [] buRefresh1 = {buP1};
	packService.refreshPackages(buRefresh1);
	
	// Give refresh thread time to run 
	try {
	  Thread.sleep(1000);
	}
	catch (InterruptedException ix) {}
	
	// Test if the exported package version to bundle P2 now is bundle P3 with version 3.0
	importState = checkExportVersion (out, buP3, buP2, packageName, "3.0.0");
	
	if (importState != true) {
	  teststatus = false;
	  fail("framework test bundle , P1 to P2 export not as expected after P3 installation and refresh of P1 :FRAME215A:FAIL");
	}
	// Test if status of P3 is in the expected RESOLVED state now.
	
	if (buP3.getState() != Bundle.RESOLVED) {
	  fail("### framework test bundle bundleP3_test status was " + getStateString(buP3.getState())
	       + " should be " + getStateString(Bundle.RESOLVED) + " :FRAME215A:");
	  teststatus = false;
	}

	if (teststatus == true) {
	  out.println("### framework test bundle :FRAME215A:PASS");
	} else {
	  fail("### framework test bundle :FRAME215A:FAIL");
	}
      } finally {
	// Uninstall and refresh as a preparation for next test case
	try {
	  if (buP3 != null)
	    buP3.uninstall();
	  if (buP2 != null)
	    buP2.uninstall();
	  if (buP1 != null)
	    buP1.uninstall();
	}
	catch (BundleException bexcA) {
	  out.println("framework test bundle "+ bexcA +" :FRAME215A:FAIL");
	  teststatus = false;
	}
	Bundle [] buRefresh2 = {buP1, buP2, buP3};
	packService.refreshPackages(buRefresh2);
	try {
	  Thread.sleep(1000);
	}
	catch (InterruptedException ix) {}
	
      }
    }
  }
  

  public final static String [] HELP_FRAME220B =  {
    "Use the framework package, version management mechanism to see that version handling of",
    "exported/imported packages work, in the case that a version of a package",
    "is already exported and that a new bundle that exports a newer package version.",
    "Check that the new bundle gets a copy of the new package.",
  };
  
  class Frame220b extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      // String packServiceName = "org.osgi.service.packageadmin.PackageAdmin";
      String packageName = "org.knopflerfish.service.bundleP1_test";
      String expectedVersion = "3.0.0";
      // PackageAdmin packService = null;
      // Bundle fw_test = bc.getBundle();    // this bundle
      
      buP1 = null;
      buP3 = null;
      
      long bidP1 = 0;
      long bidP3 = 0;
      
      // Install test bundle P1 and start it
      
      try {
	buP1 = Util.installBundle(bc, "bundleP1_test-1.0.0.jar");
	buP1.start();
	bidP1 = buP1.getBundleId();
	teststatus = true;
      }
      catch (BundleException bexcA) {
	fail("framework test bundle "+ bexcA +" :FRAME220B:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("framework test bundle "+ secA +" :FRAME220B:FAIL");
	teststatus = false;
      }
      
      // Install test bundle P3 and start it.
      
      try {
	buP3 = Util.installBundle(bc, "bundleP3_test-1.0.0.jar");
	buP3.start();
	bidP3 = buP3.getBundleId();
      }
      catch (BundleException bexcA) {
	out.println("framework test bundle "+ bexcA +" :FRAME220B:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("framework test bundle "+ secA +" :FRAME220B:FAIL");
	teststatus = false;
      }
      
      // Check that we have the new version
      if (!checkExportVersion(out, buP3, null, packageName, expectedVersion)) {
	fail("framework test bundle , we didn't get expected export import wire:FRAME220B:FAIL");
	teststatus = false;
      }
      
      if (teststatus == true) {
	out.println("### framework test bundle :FRAME220B:PASS");
      } else {
	fail("### framework test bundle :FRAME220B:FAIL");
      }
    }
  }
  
  // Check of exporting and importing bundles and versions
  
  private boolean checkExportVersion (Object _out, Bundle exporter, Bundle importer, String packName, String version) {
    String packServiceName = "org.osgi.service.packageadmin.PackageAdmin";
    PackageAdmin packService = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
    boolean teststatus = false;
    if (packService == null) {
      out.println("Got null service " + packServiceName + " in FRAME215A:FAIL");
    } else {
      // Now get the array of exported packages from exporting bundle,
      // with one expected package
      long expId = exporter.getBundleId();
      
      ExportedPackage [] exp2 = packService.getExportedPackages(exporter);

      // For all exported packages in exporter bundle, (with the specified version)
      // look for if they are imported by the importer bundle
      //
      if (exp2 != null) {
	for (int i = 0; i < exp2.length ; i++ ) {
	  //out.println("Got exported package " + exp2[i].getName() + " spev ver. " + exp2[i].getSpecificationVersion() + " in FRAME215A");
	  if (version.equals(exp2[i].getSpecificationVersion()) && packName.equals(exp2[i].getName())) {
	    Bundle [] ib = exp2[i].getImportingBundles();
            if (ib != null) {
              if (importer == null) {
                // Except no importers
                if (ib.length == 0) {
                  teststatus = true;
                }
              } else {
                long impId = importer.getBundleId();
                for (int j = 0; j < ib.length ; j++ ) {
                  //out.println("   Importing bundle: " + ib[j].getBundleId());
                  if (ib[j].getBundleId() ==  impId) {
                    // out.println ("MATCH p2 p2 hurrah");
                    teststatus = true;
                  }
                }
	      }
	    }
	  }
	}
      } else {
	// out.println("Got null exported package array from bundle " + exporter.getBundleId()  +" in FRAME215A");
      }
    }
    return teststatus;
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

  // General printout of PermissionInfo

  private void printPermission(Object _out, PermissionInfo pi) {
    StringBuffer sb1 = new StringBuffer();

    sb1.append("ENCODED: ");
    if (pi.getEncoded() != null) { sb1.append(pi.getEncoded() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("ACTIONS: ");
    if (pi.getActions() != null) { sb1.append(pi.getActions() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("NAME: ");
    if (pi.getName() != null) { sb1.append(pi.getName() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("TYPE: ");
    if (pi.getType() != null) { sb1.append(pi.getType() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("STRING: ");
    if (pi.toString() != null) { sb1.append(pi.toString() + lineseparator); } else { sb1.append("null" + lineseparator); }

    out.println(sb1.toString());
  }

  // Condensed printout of PermissionInfo

  private void printPermissionShort (Object _out, PermissionInfo pi) {
    StringBuffer sb1 = new StringBuffer();

    sb1.append("  ENCODED: ");
    if (pi.getEncoded() != null) { sb1.append(pi.getEncoded()); } else { sb1.append("null"); }

    out.println(sb1.toString());
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
    boolean listenState = true;	// assume everything will work

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

  private void clearEvents() {
    fListen.clearEvent();
    bListen.clearEvent();
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
	//TODO use constants instead  
    switch (bundleState) {
    case 0x01: return "UNINSTALLED";
    case 0x02: return "INSTALLED";
    case 0x04: return "RESOLVED";
    case 0x08: return "STARTING";
    case 0x10: return "STOPPING";
    case 0x20: return "ACTIVE";

    default: return "Unknown state";

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

}
