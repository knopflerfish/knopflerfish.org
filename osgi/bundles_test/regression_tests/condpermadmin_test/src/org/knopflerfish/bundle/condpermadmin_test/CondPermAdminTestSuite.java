/*
 * Copyright (c) 2009-2022, KNOPFLERFISH project
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

import java.io.PrintStream;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

import static org.osgi.service.condpermadmin.ConditionalPermissionInfo.ALLOW;

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

  String     test_url_base;

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
      addTest(new Condperm110a());
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
      ServiceReference<?> serviceRef = bc.getServiceReference(serviceName);
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
          for (Bundle b : bs) {
            paService.setPermissions(b.getLocation(), null);
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
    public void runTest() {
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
        PermissionInfo[] pa = new PermissionInfo[]
          { new PermissionInfo("(java.security.AllPermission)") };
        Bundle [] bs = bc.getBundles();
        for (Bundle b : bs) {
          paService.setPermissions(b.getLocation(), pa);
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

  @SuppressWarnings("unused")
  public final static String USAGE_CONDPERM100A = "";

  @SuppressWarnings("unused")
  public final static String [] HELP_CONDPERM100A =  {
    "Tests of ConditionalPermissionAdmin.getAccessControlContext",
    "Test different patterns and combinatitions."
  };

  class Condperm100a extends FWTestCase {
    public void runTest() {
      ConditionInfo ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"*, c=SE"});
      ConditionInfo ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"*, o=big, c=*"});
      ConditionInfo ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"cn=hello, o=SMALL, c=FR"});
      ConditionInfo cibl = new ConditionInfo(BUNDLE_LOCATION_CONDITION, new String[] {"http:*"});

      PermissionInfo pi1 = new PermissionInfo("java.util.PropertyPermission", "org.knopflerfish.*", "read");
      PermissionInfo pi2 = new PermissionInfo("java.util.PropertyPermission", "org.osgi.*", "read");
      PermissionInfo pi3 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "import");
      PermissionInfo pi4 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "export");

      Permission p1 = new PropertyPermission("org.knopflerfish.*", "read");
      Permission p2 = new PropertyPermission("org.osgi.*", "read");

      cpaService.setConditionalPermissionInfo("CPATEST100_1", new ConditionInfo[] {ci1}, new PermissionInfo[] {pi1});
      cpaService.setConditionalPermissionInfo("CPATEST100_2", new ConditionInfo[] {ci2}, new PermissionInfo[] {pi2});
      cpaService.setConditionalPermissionInfo("CPATEST100_3", new ConditionInfo[] {ci3}, new PermissionInfo[] {pi3});
      cpaService.setConditionalPermissionInfo("CPATEST100_4", new ConditionInfo[] {ci1, cibl}, new PermissionInfo[] {pi4});
      AccessControlContext acc = cpaService.getAccessControlContext(new String[]{"cn=X, o=small, c=SE"});
      try {
        acc.checkPermission(p1);
      } catch (Throwable t) {
        t.printStackTrace();
        fail("Permission check of " + p1 + " failed, threw " + t + " :CONDPERM100A:FAIL");
      }
      try {
        acc.checkPermission(p2);
        fail("Permission check of " + p2 + " passed :CONDPERM100A:FAIL");
      } catch (AccessControlException _ignore) {
        // Expected
      } catch (Throwable t) {
        fail("Permission check of " + p2 + " throw "+ t + " :CONDPERM100A:FAIL");
      }

      cpaService.getConditionalPermissionInfo("CPATEST100_1").delete();
      cpaService.getConditionalPermissionInfo("CPATEST100_2").delete();
      cpaService.getConditionalPermissionInfo("CPATEST100_3").delete();
      cpaService.getConditionalPermissionInfo("CPATEST100_4").delete();

      out.println("### framework test bundle :CONDPERM100A:PASS");
    }
  }

  @SuppressWarnings("unused")
  public final static String USAGE_CONDPERM110A = "";

  @SuppressWarnings("unused")
  public final static String [] HELP_CONDPERM110A =  {
    "Tests of ConditionalPermissionAdmin.getAccessControlContext using new ConditionalPermissionUpdate",
    "Test different patterns and combinatitions."
  };

  class Condperm110a extends FWTestCase {
    public void runTest() {
      ConditionInfo ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"*, c=SE"});
      ConditionInfo ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"*, o=big, c=*"});
      ConditionInfo ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[] {"cn=hello, o=SMALL, c=FR"});
      ConditionInfo cibl = new ConditionInfo(BUNDLE_LOCATION_CONDITION, new String[] {"http:*"});

      PermissionInfo pi1 = new PermissionInfo("java.util.PropertyPermission", "org.knopflerfish.*", "read");
      PermissionInfo pi2 = new PermissionInfo("java.util.PropertyPermission", "org.osgi.*", "read");
      PermissionInfo pi3 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "import");
      PermissionInfo pi4 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "export");

      Permission p1 = new PropertyPermission("org.knopflerfish.*", "read");
      Permission p2 = new PropertyPermission("org.osgi.*", "read");

      ConditionalPermissionUpdate update = cpaService.newConditionalPermissionUpdate();
      List<ConditionalPermissionInfo> cpis = update.getConditionalPermissionInfos();
      assertEquals("Empty conditional permission list", 0, cpis.size());
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci1}, new PermissionInfo[] {pi1}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci2}, new PermissionInfo[] {pi2}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci3}, new PermissionInfo[] {pi3}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci1, cibl}, new PermissionInfo[] {pi4}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci3, cibl}, new PermissionInfo[] {pi4}, ALLOW));
      assertTrue("First commit", update.commit());
      AccessControlContext acc = cpaService.getAccessControlContext(new String[]{"cn=X, o=small, c=SE"});
      try {
        acc.checkPermission(p1);
      } catch (Throwable t) {
        t.printStackTrace();
        fail("Permission check of " + p1 + " failed, threw " + t + " :CONDPERM110A:FAIL");
      }
      try {
        acc.checkPermission(p2);
        fail("Permission check of " + p2 + " passed :CONDPERM110A:FAIL");
      } catch (AccessControlException _ignore) {
        // Expected
      } catch (Throwable t) {
        fail("Permission check of " + p2 + " throw "+ t + " :CONDPERM110A:FAIL");
      }

      update = cpaService.newConditionalPermissionUpdate();
      cpis = update.getConditionalPermissionInfos();
      assertEquals("Full conditional permission list", 5, cpis.size());
      cpis.add(0,cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci1}, new PermissionInfo[] {pi1}, ALLOW));
      cpis.set(2, cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci2}, new PermissionInfo[] {pi2}, ALLOW));
//      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci3}, new PermissionInfo[] {pi3}, ConditionalPermissionInfo.ALLOW));
      cpis.set(4,cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci1, cibl}, new PermissionInfo[] {pi4}, ALLOW));
//      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci3, cibl}, new PermissionInfo[] {pi4}, ConditionalPermissionInfo.ALLOW));
      assertTrue("Second commit", update.commit());
      update = cpaService.newConditionalPermissionUpdate();
      cpis = update.getConditionalPermissionInfos();
      assertEquals("Full conditional permission list", 6, cpis.size());
      cpis.remove(1);
      assertTrue("Third commit", update.commit());
      acc = cpaService.getAccessControlContext(new String [] { "cn=X, o=small, c=SE" });
      try {
        acc.checkPermission(p1);
      } catch (Throwable t) {
        t.printStackTrace();
        fail("Permission check of " + p1 + " failed, threw " + t + " :CONDPERM110A:FAIL");
      }
      try {
        acc.checkPermission(p2);
        fail("Permission check of " + p2 + " passed :CONDPERM110A:FAIL");
      } catch (AccessControlException _ignore) {
        // Expected
      } catch (Throwable t) {
        fail("Permission check of " + p2 + " throw "+ t + " :CONDPERM110A:FAIL");
      }

      update = cpaService.newConditionalPermissionUpdate();
      cpis = update.getConditionalPermissionInfos();
      assertEquals("Full conditional permission list", 5, cpis.size());
      cpis.clear();
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci1}, new PermissionInfo[] {pi1}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci2}, new PermissionInfo[] {pi2}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci3}, new PermissionInfo[] {pi3}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci1, cibl}, new PermissionInfo[] {pi4}, ALLOW));
      cpis.add(cpaService.newConditionalPermissionInfo(null, new ConditionInfo[] {ci3, cibl}, new PermissionInfo[] {pi4}, ALLOW));
      assertTrue("Fourth commit", update.commit());
      acc = cpaService.getAccessControlContext(new String [] { "cn=X, o=small, c=SE" });
      try {
        acc.checkPermission(p1);
      } catch (Throwable t) {
        t.printStackTrace();
        fail("Permission check of " + p1 + " failed, threw " + t + " :CONDPERM110A:FAIL");
      }
      try {
        acc.checkPermission(p2);
        fail("Permission check of " + p2 + " passed :CONDPERM110A:FAIL");
      } catch (AccessControlException _ignore) {
        // Expected
      } catch (Throwable t) {
        fail("Permission check of " + p2 + " throw "+ t + " :CONDPERM110A:FAIL");
      }

      update = cpaService.newConditionalPermissionUpdate();
      cpis = update.getConditionalPermissionInfos();
      assertEquals("Full conditional permission list", 5, cpis.size());
      cpis.clear();
      assertTrue("Third commit", update.commit());

      out.println("### framework test bundle :CONDPERM110A:PASS");
    }
  }

  // 200-series tests BundleSignerCondition.

  @SuppressWarnings("unused")
  public final static String USAGE_CONDPERM200A = "";

  @SuppressWarnings("unused")
  public final static String [] HELP_CONDPERM200A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test different patterns and combinatitions."
  };

  class Condperm200a extends FWTestCase {
    public void runTest() throws Throwable {
      ConditionInfo ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
          new String[] {"*, c=*"});
      ConditionInfo ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
          new String[] {"*, c=KF"});
      ConditionInfo ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
          new String[] {"*, c=SE"});

      PermissionInfo pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
          "org.osgi.framework", "import");
      PermissionInfo pi2 = new PermissionInfo("org.osgi.framework.PackagePermission",
          P1_SERVICE_NAME, "import");
      PermissionInfo pi3 = new PermissionInfo("org.osgi.framework.PackagePermission",
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
      } catch (Exception e) {
        fail("install framework test bundle "+ e +" :CONDPERM200A:FAIL");
      }

      try {
        buP2.start();
        fail("started test bundle P2! :CONDPERM200A:FAIL");
      } catch (BundleException ignored) {
        // Expected
      } catch (Exception e) {
        fail("framework test bundle start "+ e +" :CONDPERM200A:FAIL");
      }

      // Grant P1 export permissions and see that P2 starts.
      cpaService.setConditionalPermissionInfo("CPATEST200_3",
                                              new ConditionInfo[] {ci2},
                                              new PermissionInfo[] {pi3});

      try {
        buP2.start();
      } catch (Exception e) {
        fail("framework test bundle "+ e +" :CONDPERM200A:FAIL");
      }

      buP2.uninstall();
      buP2 = null;
      buP1.uninstall();
      buP1 = null;

      cpaService.getConditionalPermissionInfo("CPATEST200_1").delete();
      cpaService.getConditionalPermissionInfo("CPATEST200_2").delete();
      cpaService.getConditionalPermissionInfo("CPATEST200_3").delete();

      out.println("### framework test bundle :CONDPERM200A:PASS");
    }
  }

  @SuppressWarnings("unused")
  public final static String USAGE_CONDPERM210A = "";

  @SuppressWarnings("unused")
  public final static String [] HELP_CONDPERM210A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test different patterns and combinatitions."
  };

  class Condperm210a extends FWTestCase {
    public void runTest() throws Throwable {
      ConditionInfo ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
          new String[] {"*, c=KF"});
      ConditionInfo ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
          new String[] {"*, c=SE"});
      ConditionInfo ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
          new String[] {"*, st=*, c=*"});
      ConditionInfo ci4 = new ConditionInfo(BUNDLE_SIGNER_CONDITION,
          new String[] {"cn=test dude, ou=*, o=*, l=*, c=se ; -"});

      PermissionInfo pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
          "org.osgi.framework", "import");
      PermissionInfo pi2 = new PermissionInfo("org.osgi.framework.PackagePermission",
          P1_SERVICE_NAME, "import");
      PermissionInfo pi3 = new PermissionInfo("org.osgi.framework.PackagePermission",
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
      } catch (Exception e) {
        fail("install framework test bundle "+ e +" :CONDPERM210A:FAIL");
      }

      try {
        buP2.start();
      } catch (Exception e) {
        fail("start P2 framework test bundle "+ e +" :CONDPERM210A:FAIL");
      }

      try {
        buP3.start();
      } catch (Exception e) {
        fail("start P3 framework test bundle "+ e +" :CONDPERM210A:FAIL");
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
      for (Bundle b : bs) {
        if (b == buP2) {
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

      out.println("### framework test bundle :CONDPERM210A:PASS");
    }
  }

  @SuppressWarnings("unused")
  public final static String USAGE_CONDPERM220A = "";

  @SuppressWarnings("unused")
  public final static String [] HELP_CONDPERM220A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test certificate chains."
  };

  class Condperm220a extends FWTestCase {
    public void runTest() throws Throwable {
      ConditionInfo ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"*; *, cn=CA Dude, ou=Test, o=*, l=*, c=se"});
      ConditionInfo ci2 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"CN=TEST DUDE,ou=*,o=*,l=*,c=SE; *, o=Knopflerfish, l=Gbg, C=SE"});
      ConditionInfo ci3 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"CN=Goalie Dude, ou=test, o=*, l=*, c=SE;*, l=Trosa, C=SE; *, l=GBG, C=SE"});
      ConditionInfo ci4 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]
          {"-; cn=CA Dude, ou=Test, o=Knopflerfish, l=GBG, C=SE"});

      PermissionInfo pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
          "org.osgi.framework", "import");
      PermissionInfo pi2 = new PermissionInfo("org.osgi.framework.PackagePermission",
          P1_SERVICE_NAME, "import,export");
      PermissionInfo pi3 = new PermissionInfo("org.osgi.framework.PackagePermission",
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
      } catch (Exception e) {
        e.printStackTrace();
        fail("install framework test bundle "+ e +" :CONDPERM220A:FAIL");
      }

      try {
        buP3.start();
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle "+ e +" :CONDPERM220A:FAIL");
      }

      try {
        buP4.start();
        fail("started test bundleP4! :CONDPERM220A:FAIL");
      } catch (BundleException e) {
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle start "+ e +" :CONDPERM220A:FAIL");
      }

      try {
        buP3.uninstall();
        buP3 = null;
        buP3 = Util.installBundle(bc, "bundleP3_test-1.0.0.jar");
      } catch (Exception e) {
        e.printStackTrace();
        fail("install framework test bundle 2 "+ e +" :CONDPERM220A:FAIL");
      }

      // Update import permissions and see that P3 & P4 starts.
      cpaService.setConditionalPermissionInfo("CPATEST220_1",
                                              new ConditionInfo[] {ci4},
                                              new PermissionInfo[] {pi1});
      try {
        buP3.start();
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle 2 "+ e +" :CONDPERM220A:FAIL");
      }

      try {
        buP4.start();
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle 3 "+ e +" :CONDPERM220A:FAIL");
      }

      buP3.uninstall();
      buP3 = null;
      buP4.uninstall();
      buP4 = null;

      cpaService.getConditionalPermissionInfo("CPATEST220_1").delete();
      cpaService.getConditionalPermissionInfo("CPATEST220_2").delete();
      cpaService.getConditionalPermissionInfo("CPATEST220_3").delete();

      out.println("### framework test bundle :CONDPERM220A:PASS");
    }
  }

  @SuppressWarnings("unused")
  public final static String USAGE_CONDPERM230A = "";

  @SuppressWarnings("unused")
  public final static String [] HELP_CONDPERM230A =  {
    "Tests of BundleSignerCondition matching bundles.",
    "Test that unvalid certificate are rejected."
  };

  class Condperm230a extends FWTestCase {
    public void runTest() throws Throwable {
      ConditionInfo ci1 = new ConditionInfo(BUNDLE_SIGNER_CONDITION, new String[]{"*, c=*"});

      PermissionInfo pi1 = new PermissionInfo("org.osgi.framework.PackagePermission",
          "org.osgi.framework", "import");

      cpaService.setConditionalPermissionInfo("CPATEST230_1",
                                              new ConditionInfo[] {ci1},
                                              new PermissionInfo[] {pi1});

      // Install test bundle P5
      try {
        buP5 = Util.installBundle(bc, "bundleP5_test-1.0.0.jar");
      } catch (Exception e) {
        fail("install framework test bundle "+ e +" :CONDPERM230A:FAIL");
      }

      try {
        buP5.start();
        fail("started test bundleP5! :CONDPERM230A:FAIL");
      } catch (BundleException ignored) {
        // Expected
      }

      buP5.uninstall();
      buP5 = null;

      cpaService.getConditionalPermissionInfo("CPATEST230_1").delete();

      out.println("### framework test bundle :CONDPERM230A:PASS");
    }
  }


  static class FrameworkListener implements org.osgi.framework.FrameworkListener {
    ArrayList<FrameworkEvent> fe = new ArrayList<>();

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
          return fe.remove(0);
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
