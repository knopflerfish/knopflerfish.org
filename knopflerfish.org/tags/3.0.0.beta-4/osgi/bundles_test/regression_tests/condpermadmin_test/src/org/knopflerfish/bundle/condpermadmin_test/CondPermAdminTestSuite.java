/*
 * Copyright (c) 2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.condpermadmin_test;

import java.util.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;

import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.*;
import org.osgi.service.condpermadmin.*;

import junit.framework.*;

public class CondPermAdminTestSuite extends TestSuite  {

  final static String BUNDLE_LOCATION_CONDITION =
    "org.osgi.service.condpermadmin.BundleLocationCondition";
  final static String BUNDLE_SIGNER_CONDITION =
    "org.osgi.service.condpermadmin.BundleSignerCondition";
  final static String CPA_SERVICE_NAME =
    "org.osgi.service.condpermadmin.ConditionalPermissionAdmin";
  final static String PA_SERVICE_NAME =
    "org.osgi.service.permissionadmin.PermissionAdmin";
  final static String PACK_SERVICE_NAME =
    "org.osgi.service.packageadmin.PackageAdmin";
  final static String P1_SERVICE_NAME =
    "org.knopflerfish.service.bundleP1_test";


  BundleContext bc;

  FrameworkListener fListen;

  PermissionAdmin paService = null;
  ConditionalPermissionAdmin cpaService = null;
  PackageAdmin packService = null;

  Properties props = System.getProperties();
  String     lineseparator = props.getProperty("line.separator");

  String     test_url_base;

  Vector     events    = new Vector();  // vector for events from test bundles
  Vector     expevents = new Vector();	// comparision vector

  PrintStream out = System.out;

  // Package version test bundles
  Bundle buP1 = null;
  Bundle buP2 = null;
  Bundle buP3 = null;
  Bundle buP4 = null;
  Bundle buP5 = null;


  public CondPermAdminTestSuite (BundleContext bc) {
    super ("ConditionalPermissionAdminTestSuite");

    this.bc = bc;

    test_url_base = "bundle://" + bc.getBundle().getBundleId() + "/";
    // No need to test if we do not have CPA.
    if (bc.getServiceReference(CPA_SERVICE_NAME) != null) {
      addTest(new Setup());
      addTest(new Condperm100a());
      addTest(new Condperm200a());
      addTest(new Condperm210a());
      addTest(new Condperm220a());
      addTest(new Condperm230a());
      addTest(new Cleanup());
    } else {
      System.out.println("CondPermAdminTestSuite - Skip tests! No " + CPA_SERVICE_NAME);
    }
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


    /**
     * Get named service
     *
     * @param serviceName Name of service to be fetched.
     * @param entity calling this method, used for error messages
     * @return the requested service
     */
    public Object getService(String serviceName, String entity) {
      ServiceReference serviceRef = bc.getServiceReference(serviceName);
      if (serviceRef == null) {
	fail("Got null service reference, " + serviceName + ":" + entity + ":FAIL");
      }
      Object service = bc.getService(serviceRef);
      if (service == null) {
	fail("Got null service, " + serviceName + ":" + entity + ":FAIL");
      }
      return service;
    }
  }


  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {

      if (buP1 != null) {
	buP1.uninstall();
	buP1 = null;
      }
      if (buP2 != null) {
	buP2.uninstall();
	buP2 = null;
      }
      if (buP3 != null) {
	buP3.uninstall();
	buP3 = null;
      }
      if (buP4 != null) {
	buP4.uninstall();
	buP4 = null;
      }
      if (buP5 != null) {
	buP5.uninstall();
	buP5 = null;
      }

      try   { bc.removeFrameworkListener(fListen); } 
      catch (Exception ignored) { }
      fListen = null;

      if (paService != null) {
	try {
	  Bundle [] bs = bc.getBundles();
	  for (int i = 0; i < bs.length; i++) {
	    paService.setPermissions(bs[i].getLocation(), null);
	  }
	  
	} catch (Throwable tt) {
          tt.printStackTrace();
	  fail("Failed to cleanup initial permissions :CLEANUP:FAIL");
	}
      }
      
    }
  }

  // Also install all possible listeners
  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
      fListen = new FrameworkListener();
      try {
	bc.addFrameworkListener(fListen);
      } catch (IllegalStateException ise) {
	fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }
      // Give all existing bundles permissions
      // Use PermissionAdmin, change to ConditionalPermissionAdmin later!?
      paService = (PermissionAdmin) getService(PA_SERVICE_NAME, "SETUP");

      try {
	PermissionInfo pa[] = new PermissionInfo[]
	  { new PermissionInfo("(java.security.AllPermission)") };
	Bundle [] bs = bc.getBundles();
	for (int i = 0; i < bs.length; i++) {
	  paService.setPermissions(bs[i].getLocation(), pa);
	}

      } catch (Throwable tt) {
	fail("Failed to setup initial permissions :SETUP:FAIL");
      }

      cpaService = (ConditionalPermissionAdmin)getService(CPA_SERVICE_NAME, "SETUP");

      packService = (PackageAdmin)getService(PACK_SERVICE_NAME, "SETUP");

      out.println("### framework test bundle :SETUP:PASS");
    }
  }

  // 100-series tests CPA Service.

  public final static String USAGE_CONDPERM100A = "";
  public final static String [] HELP_CONDPERM100A =  {
    "Tests of ConditionalPermissionAdmin.getAccessControlContext",
    "Test different patterns and combinatitions."
  };

  class Condperm100a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      ConditionInfo ci1, ci2, ci3, cibl;
      PermissionInfo pi1, pi2, pi3, pi4, pi5;
      Permission p1, p2, p3, p4, p5;
      AccessControlContext acc;

      ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"*, c=SE"});
      ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"*, o=big, c=*"});
      ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"cn=hello, o=SMALL, c=FR"});
      cibl = new ConditionInfo(BUNDLE_LOCATION_CONDITION, new String[] {"http:*"});
      
      pi1 = new PermissionInfo("java.util.PropertyPermission", "org.knopflerfish.*", "read");
      pi2 = new PermissionInfo("java.util.PropertyPermission", "org.osgi.*", "read");
      pi3 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "import");
      pi4 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "export");
      pi5 = new PermissionInfo("org.osgi.framework.ServicePermission", "*", "get, register");

      p1 = new PropertyPermission("org.knopflerfish.*", "read");
      p2 = new PropertyPermission("org.osgi.*", "read");
      p3 = new PackagePermission("org.osgi.framework", "import");
      p4 = new PackagePermission("org.osgi.framework", "export");
      p5 = new ServicePermission("*", ServicePermission.GET);

      cpaService.setConditionalPermissionInfo("CPATEST100_1", new ConditionInfo[] {ci1}, new PermissionInfo[] {pi1});
      cpaService.setConditionalPermissionInfo("CPATEST100_2", new ConditionInfo[] {ci2}, new PermissionInfo[] {pi2});
      cpaService.setConditionalPermissionInfo("CPATEST100_3", new ConditionInfo[] {ci3}, new PermissionInfo[] {pi3});
      cpaService.setConditionalPermissionInfo("CPATEST100_4", new ConditionInfo[] {ci1, cibl}, new PermissionInfo[] {pi4});
      acc = cpaService.getAccessControlContext(new String [] { "cn=X, o=small, c=SE" });
      try {
	acc.checkPermission(p1);
      } catch (Throwable t) {
	fail("Permission check of " + p1 + " failed, threw " + t + " :CONDPERM100A:FAIL");
	teststatus  = false;
      }
      try {
	acc.checkPermission(p2);
	fail("Permission check of " + p2 + " passed :CONDPERM100A:FAIL");
	teststatus  = false;
      } catch (AccessControlException _ignore) {
	// Expected
      } catch (Throwable t) {
	fail("Permission check of " + p2 + " throw "+ t + " :CONDPERM100A:FAIL");
	teststatus  = false;
      }
      
      cpaService.getConditionalPermissionInfo("CPATEST100_1").delete();
      cpaService.getConditionalPermissionInfo("CPATEST100_2").delete();
      cpaService.getConditionalPermissionInfo("CPATEST100_3").delete();
      cpaService.getConditionalPermissionInfo("CPATEST100_4").delete();

      if (teststatus == true) {
	out.println("### framework test bundle :CONDPERM100A:PASS");
      } else {
	fail("### framework test bundle :CONDPERM100A:FAIL");
      }
    }
  }

  // 200-series tests BundleSignerCondition.

  public final static String USAGE_CONDPERM200A = "";
  public final static String [] HELP_CONDPERM200A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test different patterns and combinatitions."
  };

  class Condperm200a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      ConditionInfo ci1, ci2, ci3;
      PermissionInfo pi1, pi2, pi3, pi4, pi5;
      AccessControlContext acc;

      ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
			      new String[] {"*, c=*"});
      ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
			      new String[] {"*, c=KF"});
      ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
			      new String[] {"*, c=SE"});
      
      pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       "org.osgi.framework", "import");
      pi2 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       P1_SERVICE_NAME, "import");
      pi3 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       P1_SERVICE_NAME, "export");

      cpaService.setConditionalPermissionInfo("CPATEST200_1",
					      new ConditionInfo[] {ci1},
					      new PermissionInfo[] {pi1});
      cpaService.setConditionalPermissionInfo("CPATEST200_2",
					      new ConditionInfo[] {ci3},
					      new PermissionInfo[] {pi2});
	
      // Install test bundles P1 and P2, start the later
      // Check that it fails, because it can not imports from P1,
      // because P1 does not have export permissions.
	
      try {
	buP1 = Util.installBundle(bc, "bundleP1_test-1.0.0.jar");
	buP2 = Util.installBundle(bc, "bundleP2_test-1.0.0.jar");
      }	catch (Exception e) {
	fail("install framework test bundle "+ e +" :CONDPERM200A:FAIL");
	teststatus = false;
      }

      try {
	buP2.start();
	fail("started test bundle P2! :CONDPERM200A:FAIL");
	teststatus = false;
      }	catch (BundleException e) {
	teststatus = true;
      }	catch (Exception e) {
	fail("framework test bundle start "+ e +" :CONDPERM200A:FAIL");
	teststatus = false;
      }
	
      // Grant P1 export permissions and see that P2 starts.
      cpaService.setConditionalPermissionInfo("CPATEST200_3",
					      new ConditionInfo[] {ci2},
					      new PermissionInfo[] {pi3});

      try {
	buP2.start();
	teststatus = true;
      }	catch (Exception e) {
	fail("framework test bundle "+ e +" :CONDPERM200A:FAIL");
	teststatus = false;
      }

      buP2.uninstall();
      buP2 = null;
      buP1.uninstall();
      buP1 = null;

      cpaService.getConditionalPermissionInfo("CPATEST200_1").delete();
      cpaService.getConditionalPermissionInfo("CPATEST200_2").delete();
      cpaService.getConditionalPermissionInfo("CPATEST200_3").delete();

      if (teststatus == true) {
	out.println("### framework test bundle :CONDPERM200A:PASS");
      } else {
	fail("### framework test bundle :CONDPERM200A:FAIL");
      }
    }
  }


  public final static String USAGE_CONDPERM210A = "";
  public final static String [] HELP_CONDPERM210A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test different patterns and combinatitions."
  };

  class Condperm210a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      ConditionInfo ci1, ci2, ci3, ci4;
      PermissionInfo pi1, pi2, pi3;
      AccessControlContext acc;

      ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
			      new String[] {"*, c=KF"});
      ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
			      new String[] {"*, c=SE"});
      ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
			      new String[] {"*, st=*, c=*"});
      ci4 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
			      new String[] {"cn=test dude, ou=*, o=*, l=*, c=se ; -"});
      
      pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       "org.osgi.framework", "import");
      pi2 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       P1_SERVICE_NAME, "import");
      pi3 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       P1_SERVICE_NAME, "export");

      cpaService.setConditionalPermissionInfo("CPATEST210_1",
					      new ConditionInfo[] {},
					      new PermissionInfo[] {pi1, pi2});
      cpaService.setConditionalPermissionInfo("CPATEST210_2",
					      new ConditionInfo[] {ci1, ci3},
					      new PermissionInfo[] {pi3});
      cpaService.setConditionalPermissionInfo("CPATEST210_3",
					      new ConditionInfo[] {ci2, ci3},
					      new PermissionInfo[] {pi3});
	
      // Install test bundles P1,P2 and P3, start the later
      // Check that it is ok and that it imports from P1
      // because P3 does not have export permissions.
	
      try {
	buP1 = Util.installBundle(bc, "bundleP1_test-1.0.0.jar");
	buP2 = Util.installBundle(bc, "bundleP2_test-1.0.0.jar");
	buP3 = Util.installBundle(bc, "bundleP3_test-1.0.0.jar");
      }	catch (Exception e) {
	fail("install framework test bundle "+ e +" :CONDPERM210A:FAIL");
	teststatus = false;
      }

      try {
	buP2.start();
      }	catch (Exception e) {
	fail("start P2 framework test bundle "+ e +" :CONDPERM210A:FAIL");
	teststatus = false;
      }

      try {
	buP3.start();
      }	catch (Exception e) {
	fail("start P3 framework test bundle "+ e +" :CONDPERM210A:FAIL");
	teststatus = false;
      }

      ExportedPackage ep = packService.getExportedPackage(P1_SERVICE_NAME);
      if (ep == null) {
        fail("P1 package not exported:CONDPERM210A:FAIL");
      }
      if (ep.getExportingBundle() != buP1) {
        fail("P1 not exporting bundle, " + ep.getExportingBundle() + " != " +
             buP1 +" :CONDPERM210A:FAIL");
      }
      Bundle [] bs = ep.getImportingBundles();
      boolean correctImport = false;
      for (int i = 0; i < bs.length; i++) {
        if (bs[i] == buP2) {
          correctImport = true;
          break;
        }
      }
      if (!correctImport) {
        fail("P2 not importing bundle:CONDPERM210A:FAIL");
      }

      cpaService.setConditionalPermissionInfo("CPATEST210_4",
					      new ConditionInfo[] {ci4},
					      new PermissionInfo[] {pi3});

      packService.refreshPackages(new Bundle []  {buP1, buP2, buP3});
      if (!fListen.waitFor(FrameworkEvent.PACKAGES_REFRESHED)) {
        fail("Wait for PACKAGES_REFRESHED:CONDPERM210A:FAIL");
      }

      ep = packService.getExportedPackage(P1_SERVICE_NAME);
      if (ep == null) {
        fail("P1 package not exported again:CONDPERM210A:FAIL");
      }
      if (ep.getExportingBundle() != buP3) {
        fail("P3 not exporting bundle, " + ep.getExportingBundle() + " != " +
             buP3 + " :CONDPERM210A:FAIL");
      }

      buP1.uninstall();
      buP1 = null;
      buP2.uninstall();
      buP2 = null;
      buP3.uninstall();
      buP3 = null;

      cpaService.getConditionalPermissionInfo("CPATEST210_1").delete();
      cpaService.getConditionalPermissionInfo("CPATEST210_2").delete();
      cpaService.getConditionalPermissionInfo("CPATEST210_3").delete();
      cpaService.getConditionalPermissionInfo("CPATEST210_4").delete();

      if (teststatus == true) {
	out.println("### framework test bundle :CONDPERM210A:PASS");
      } else {
	fail("### framework test bundle :CONDPERM210A:FAIL");
      }
    }
  }


  public final static String USAGE_CONDPERM220A = "";
  public final static String [] HELP_CONDPERM220A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test certificate chains."
  };

  class Condperm220a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      ConditionInfo ci1, ci2, ci3, ci4;
      PermissionInfo pi1, pi2, pi3;
      AccessControlContext acc;

      ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"* ; *, cn=CA Dude, ou=Test, o=*, l=*, c=se"});
      ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"CN=TEST DUDE,ou=*,o=*,l=*,c=SE ; *, o=Knopflerfish, l=Gbg, C=SE"});
      ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"CN=Goalie Dude, ou=test, o=*, l=*, c=SE;*, l=Trosa, C=SE ; *, l=GBG, C=SE"});
      ci4 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"- ; cn=CA Dude, ou=Test, o=Knopflerfish, l=GBG, C=SE"});
      
      pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       "org.osgi.framework", "import");
      pi2 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       P1_SERVICE_NAME, "import,export");
      pi3 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       P1_SERVICE_NAME, "import");

      cpaService.setConditionalPermissionInfo("CPATEST220_1",
					      new ConditionInfo[] {ci1},
					      new PermissionInfo[] {pi1});
      cpaService.setConditionalPermissionInfo("CPATEST220_2",
					      new ConditionInfo[] {ci2},
					      new PermissionInfo[] {pi2});
      cpaService.setConditionalPermissionInfo("CPATEST220_3",
					      new ConditionInfo[] {ci3},
					      new PermissionInfo[] {pi3});
	
      // Install test bundles P3 and P4
      try {
	buP3 = Util.installBundle(bc, "bundleP3_test-1.0.0.jar");
	buP4 = Util.installBundle(bc, "bundleP4_test-1.0.0.jar");
      }	catch (Exception e) {
	fail("install framework test bundle "+ e +" :CONDPERM220A:FAIL");
	teststatus = false;
      }

      try {
	buP3.start();
	teststatus = true;
      }	catch (Exception e) {
	fail("framework test bundle "+ e +" :CONDPERM220A:FAIL");
	teststatus = false;
      }

      try {
	buP4.start();
	fail("started test bundleP4! :CONDPERM220A:FAIL");
	teststatus = false;
      }	catch (BundleException e) {
	teststatus = true;
      }	catch (Exception e) {
	fail("framework test bundle start "+ e +" :CONDPERM220A:FAIL");
	teststatus = false;
      }

      try {
        buP3.uninstall();
        buP3 = null;
	buP3 = Util.installBundle(bc, "bundleP3_test-1.0.0.jar");
      }	catch (Exception e) {
	fail("install framework test bundle 2 "+ e +" :CONDPERM220A:FAIL");
	teststatus = false;
      }

      // Update import permissions and see that P3 & P4 starts.
      cpaService.setConditionalPermissionInfo("CPATEST220_1",
					      new ConditionInfo[] {ci4},
					      new PermissionInfo[] {pi1});
      try {
	buP3.start();
	teststatus = true;
      }	catch (Exception e) {
	fail("framework test bundle 2 "+ e +" :CONDPERM220A:FAIL");
	teststatus = false;
      }

      try {
	buP4.start();
	teststatus = true;
      }	catch (Exception e) {
	fail("framework test bundle 3 "+ e +" :CONDPERM220A:FAIL");
	teststatus = false;
      }

      buP3.uninstall();
      buP3 = null;
      buP4.uninstall();
      buP4 = null;

      cpaService.getConditionalPermissionInfo("CPATEST220_1").delete();
      cpaService.getConditionalPermissionInfo("CPATEST220_2").delete();
      cpaService.getConditionalPermissionInfo("CPATEST220_3").delete();

      if (teststatus == true) {
	out.println("### framework test bundle :CONDPERM220A:PASS");
      } else {
	fail("### framework test bundle :CONDPERM220A:FAIL");
      }
    }
  }


  public final static String USAGE_CONDPERM230A = "";
  public final static String [] HELP_CONDPERM230A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test that unvalid certificate are rejected."
  };

  class Condperm230a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      ConditionInfo ci1, ci2, ci3, ci4;
      PermissionInfo pi1, pi2, pi3;
      AccessControlContext acc;

      ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"*, c=*"});
      
      pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
			       "org.osgi.framework", "import");

      cpaService.setConditionalPermissionInfo("CPATEST230_1",
					      new ConditionInfo[] {ci1},
					      new PermissionInfo[] {pi1});
	
      // Install test bundle P5
      try {
	buP5 = Util.installBundle(bc, "bundleP5_test-1.0.0.jar");
      }	catch (Exception e) {
	fail("install framework test bundle "+ e +" :CONDPERM230A:FAIL");
	teststatus = false;
      }

      try {
	buP5.start();
	fail("started test bundleP5! :CONDPERM230A:FAIL");
	teststatus = false;
      }	catch (BundleException e) {
	teststatus = true;
      }

      buP5.uninstall();
      buP5 = null;

      cpaService.getConditionalPermissionInfo("CPATEST230_1").delete();

      if (teststatus == true) {
	out.println("### framework test bundle :CONDPERM230A:PASS");
      } else {
	fail("### framework test bundle :CONDPERM230A:FAIL");
      }
    }
  }


  class FrameworkListener implements org.osgi.framework.FrameworkListener {
    ArrayList fe = new ArrayList();

    synchronized public void frameworkEvent(FrameworkEvent evt) {
      fe.add(evt);
      notify();
    }

    synchronized public FrameworkEvent getEvent() {
      for (int i = 0; i < 10; i++) {
        if (fe.isEmpty()) {
          try {
            wait(2000);
          } catch (InterruptedException ignore) { }
        } else {
          return (FrameworkEvent)fe.remove(0);
        }
      }
      return null;
    }

    public boolean waitFor(int type) {
      FrameworkEvent e;
      while ((e = getEvent()) != null) {
        if (e.getType() == type) {
          return true;
        }
      }
      return false;
    }

    synchronized public void clearEvent() {
      fe.clear();
    }
  }
}
