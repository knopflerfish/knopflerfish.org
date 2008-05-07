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

      // Check that bCc have registered 3 service instanciating
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
