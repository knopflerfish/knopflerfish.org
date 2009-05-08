/*
 * Copyright (c) 2008-2008, KNOPFLERFISH project
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

import java.io.PrintStream;
import java.util.*;
import java.security.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

import junit.framework.*;

import org.knopflerfish.service.framework_test.FrameworkTest;

public class RequireBundleTestSuite extends TestSuite implements FrameworkTest {
  BundleContext bc;
  Bundle bu;

  // Test target bundles
  Bundle buA;
  Bundle buB;
  Bundle buC;
  Bundle buD;
  Bundle buCc;

  PrintStream out = System.out;

  static final String buB_package = "test_rb.B";

  public RequireBundleTestSuite (BundleContext bc) {
    super("RequireBundleTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new Frame400a());
    addTest(new Frame410a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame420a());
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
      // Looks up the package admin service and installs the test target bundles
      ServiceReference paSR = bc.getServiceReference
        (org.osgi.service.packageadmin.PackageAdmin.class.getName());
      PackageAdmin pa = (PackageAdmin)bc.getService(paSR);
      if (pa == null) {
        fail("Failed to get PackageAdmin service");
      }

      try {
        pa.refreshPackages(null);
	Thread.sleep(1000);
      } catch (Exception e) {
        fail("Failed to refresh packages");
      }
      bc.ungetService(paSR);

      buA = Util.installBundle(bc, "rb_A-0.1.0.jar");
      assertNotNull(buA);
      buB = Util.installBundle(bc, "rb_B_api-0.1.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "rb_C_api-0.1.0.jar");
      assertNotNull(buC);
      buD = Util.installBundle(bc, "rb_D_api-0.1.0.jar");
      assertNotNull(buD);
      buCc = Util.installBundle(bc, "rb_C-0.1.0.jar");
      assertNotNull(buCc);
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      // Uninstalls the test target bundles

      Bundle[] bundles = new Bundle[] {
        buA,
        buB,
        buC,
        buD,
        buCc
      };
      for(int i = 0; i < bundles.length; i++) {
        try {  bundles[i].uninstall();  }
        catch (Exception ignored) { }
      }

      buA = null;
      buB = null;
      buC = null;
      buD = null;
      buCc = null;
    }
  }


  public final static String [] HELP_FRAME400A =  {
    "Check that the require bundle directives visibility and resolution works"
  };

  class Frame400a extends FWTestCase {

    public void runTest() throws Throwable {
      // Start buA to resolve it
      try {
        buA.start();
      }
      catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME400A:FAIL");
      }
      catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME400A:FAIL");
      }

      String ceStr = checkExports(bc, buA, new String[]{"test_rb.B"});
      if ( ceStr != null ) {
          fail(ceStr +  ":FRAME400A:FAIL");
      }
      out.println("### framework test bundle :FRAME400A:PASS");
    }
  }

  public final static String [] HELP_FRAME410A =  {
    "Split packages via require bundle directive."
  };

  class Frame410a extends FWTestCase {

    public void runTest() throws Throwable {
      // Start buA to resolve it
      try {
        buCc.start();
      }
      catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME410A:FAIL");
      }
      catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME410A:FAIL");
      }

      String ceStr = checkExports(bc, buC, new String[]{"test_rb.C"});
      if ( ceStr != null ) {
          fail(ceStr +  ":FRAME410A:FAIL");
      }

      // Check that bC has registered 3 services instanciating
      // classes via both the require-bundle packages and the imported
      // package from bC that is split over bC and bD.
      Map expected = new HashMap();
      expected.put("C.C","Class test_rb.C.C from bundle C.");
      expected.put("C.D","Class test_rb.C.D from bundle D.");
      expected.put("D.D","Class test_rb.D.D from bundle D.");
      try {
        ServiceReference[] srs
          = bc.getServiceReferences(Object.class.getName(),
                                    "(test_rb=*)");
        assertEquals("Found correct number of services",3,srs.length);
        for (int i=0; i<srs.length; i++) {
          ServiceReference sr = srs[i];
          String name   = (String) sr.getProperty("test_rb");
          String value  = (String) sr.getProperty("toString");
          String answer = (String) expected.remove(name);
          String msg    = "Value of toString for "+name
            +" expected '" +answer +"' was '" +value +"'.";
          assertEquals(msg, answer, value);
        }
        out.println("Unused expected keys: " +expected);
        assertEquals("All expected keys not found.", 0, expected.size());
      } catch (Exception e) {
        fail("framework test bundle "+ e +" :FRAME410A:FAIL");
      }
      out.println("### framework test bundle :FRAME410A:PASS");
    }
  }


  public final static String [] HELP_FRAME420A =  {
    "Refresh of required bundle."
  };

  class Frame420a extends FWTestCase {

    public void runTest() throws Throwable {
      // Start buA to resolve it
      try {
        buCc.start();
      }
      catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME420A:FAIL");
      }
      catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME420A:FAIL");
      }

      String ceStr = checkExports(bc, buC, new String[]{"test_rb.C"});
      if ( ceStr != null ) {
          fail(ceStr +  ":FRAME420A:FAIL");
      }

      try {
        ServiceReference paSR = bc.getServiceReference
          (org.osgi.service.packageadmin.PackageAdmin.class.getName());
        PackageAdmin pa = (PackageAdmin)bc.getService(paSR);
        if (pa == null) {
          fail("Failed to get PackageAdmin service");
        }
        // buD shall export 2 packages:
        ExportedPackage[] epkgs = pa.getExportedPackages(buD);
        assertNotNull("Packages shall be exported from D",epkgs);
        assertEquals("Number of packages exported from D",2,epkgs.length);
        int normalPkgs = 0;
        int pendingRemovalPkgs = 0;
        for(int i=0; i<epkgs.length; i++) {
          out.println("epkgs["+i+"] initially: "+epkgs[i]);
          if (epkgs[i].isRemovalPending()) {
            pendingRemovalPkgs++;
          } else {
            normalPkgs++;
          }
        }
        assertEquals("Pkgs pending removal",0,pendingRemovalPkgs);
        assertEquals("Pkgs exported",2,normalPkgs);
        // buD shall be required by 2 bundles (buC and buCc).
        RequiredBundle[] rbs = pa.getRequiredBundles(buD.getSymbolicName());
        assertEquals("D is a RequiredBundle",1,rbs.length);
        assertTrue("RequiredBundle removal pending",!rbs[0].isRemovalPending());
        Bundle[] dUsers = rbs[0].getRequiringBundles();
        StringBuffer sb = new StringBuffer(buD +" is required by ");
        for (int i=0; i<dUsers.length; i++) {
          sb.append(" ").append(dUsers[i].toString());
        }
        out.println(sb.toString());
        assertEquals("Number of bundles requiring D",2,dUsers.length);
        bc.ungetService(paSR);
        pa = null;

        // Update the required bundle, D, check that there are two
        // packages exported marked as pending removal.
        out.println("Updating " +buD);
        Util.updateBundle(bc, buD, "rb_D_api-0.1.0.jar");
        // Must always fetch a new PackageAdmin after an update!
        pa = (PackageAdmin)bc.getService(paSR);
        if (pa == null) {
          fail("Failed to get PackageAdmin service");
        }
        epkgs = pa.getExportedPackages(buD);
        assertNotNull("Packages shall be exported from D",epkgs);
        assertEquals("Number of packages exported from D",2,epkgs.length);
        normalPkgs = 0;
        pendingRemovalPkgs = 0;
        for(int i=0; i<epkgs.length; i++) {
          out.println("epkgs["+i+"] after update: "+epkgs[i]);
          if (epkgs[i].isRemovalPending()) {
            pendingRemovalPkgs++;
          } else {
            normalPkgs++;
          }
        }
        assertEquals("Pkgs pending removal",2,pendingRemovalPkgs);
        assertEquals("Pkgs exported",0,normalPkgs);

        // buD is still required by 2 bundles (buC and buCc).
        rbs = pa.getRequiredBundles(buD.getSymbolicName());
        assertEquals("D is a RequiredBundle after update",1,rbs.length);
        assertTrue("RequiredBundle removal not pending after update",
                   !rbs[0].isRemovalPending());
        dUsers = rbs[0].getRequiringBundles();
        sb = new StringBuffer(buD +" is required by ");
        for (int i=0; i<dUsers.length; i++) {
          sb.append(" ").append(dUsers[i].toString());
        }
        out.println(sb.toString());
        assertEquals("Number of bundles requiring D after update",
                     2,dUsers.length);

        // Refresh and wait
        out.println("Calling PackageAdmin.refresh().");
        pa.refreshPackages(null);
        bc.ungetService(paSR);
        pa = null;
        // Note: PackageAdmin.refresh() will return immediately, i.e.,
        // before the refresh operation is completed. Thus a litle
        // sleep here since there is no explicit event signaling the
        // completion of the refresh operation.
        Thread.currentThread().sleep(1000);

        // Check that the old packages are gone.
        // Must allways fetch a new PackageAdmin after a refresh!
        pa = (PackageAdmin)bc.getService(paSR);
        if (pa == null) {
          fail("Failed to get PackageAdmin service");
        }

        epkgs = pa.getExportedPackages(buD);
        assertNotNull("Packages shall be exported from D",epkgs);
        normalPkgs = 0;
        pendingRemovalPkgs = 0;
        for(int i=0; i<epkgs.length; i++) {
          out.println("epkgs["+i+"] after refresh: "+epkgs[i]);
          if (epkgs[i].isRemovalPending()) {
            pendingRemovalPkgs++;
          } else {
            normalPkgs++;
          }
        }
        assertEquals("Number of packages exported from D",2,epkgs.length);
        assertEquals("Pkgs pending removal",0,pendingRemovalPkgs);
        assertEquals("Pkgs exported",2,normalPkgs);

        // buD is required by 2 bundles (buC and buCc).
        rbs = pa.getRequiredBundles(buD.getSymbolicName());
        assertEquals("D is a RequiredBundle after refresh",1,rbs.length);
        assertTrue("RequiredBundle removal not pending after refresh",
                   !rbs[0].isRemovalPending());
        dUsers = rbs[0].getRequiringBundles();
        sb = new StringBuffer(buD +" is required by ");
        for (int i=0; i<dUsers.length; i++) {
          sb.append(" ").append(dUsers[i].toString());
        }
        out.println(sb.toString());
        assertEquals("Number of bundles requiring D after refresh",
                     2,dUsers.length);
        bc.ungetService(paSR);
        pa = null;

      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle "+ e +" :FRAME420A:FAIL");
      }
      out.println("### framework test bundle :FRAME420A:PASS");
    }
  }


  //
  // Help methods
  //

  /**
   * Get the packages exported by the specified bundle and check the
   * they match with the given array of package names that are
   * expected to be exported by this bundle.
   * @param bc      Our bundle context.
   * @param b       Exporting bundle.
   * @param expPkgs Expected exports.
   */
  private String checkExports(BundleContext bc, Bundle b, String[] expPkgs)
  {
    ServiceReference paSR = null;
    PackageAdmin pa = null;

    try {
      paSR = bc.getServiceReference
        (org.osgi.service.packageadmin.PackageAdmin.class.getName());
      pa = (PackageAdmin) bc.getService(paSR);
      if (pa == null) {
        return "No package-amdin service available";
      }
    } catch (Exception ex) {
      return "Got exception: " + ex;
    }

    ExportedPackage[] bExpPkgs = pa.getExportedPackages(b);
    bc.ungetService(paSR);

    if (bExpPkgs==null && expPkgs.length>0) {
      return "No packages exported from bundle " +b.getBundleId();
    } else if (bExpPkgs!=null) {
      Set expPkgSet = new HashSet(Arrays.asList(expPkgs));
      out.println("Expected exports: " +expPkgSet);
      out.println("Actual exports:   " +Arrays.asList(bExpPkgs));
      for (int i=0; i<bExpPkgs.length; i++) {
        ExportedPackage epkg = bExpPkgs[i];
        if (!expPkgSet.remove(epkg.getName())) {
          return "Unexpected exported package: "+epkg.getName();
        }
      }
      return expPkgSet.size()>0
        ? ("Missing exports: " + expPkgSet)
        : (String) null;
    }
    return null;
  }


}
