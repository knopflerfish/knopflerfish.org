/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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

public class FragmentTestSuite extends TestSuite implements FrameworkTest {
  BundleContext bc;
  Bundle bu;

  // Test target bundles
  Bundle buA;
  Bundle buB;
  Bundle buC;
  Bundle buD;
  Bundle buE;

  PrintStream out = System.out;

  static final String buB_package = "test_rb.B";

  public FragmentTestSuite (BundleContext bc) {
    super("FragmentTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new Frame500a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame510a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame520a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame530a());
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

      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
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
        buE,
      };
      for(int i = 0; i < bundles.length; i++) {
        if (bundles[i] != null) {
          try {
            bundles[i].uninstall();
          } catch (Exception ignored) { }
        }
      }

      buA = null;
      buB = null;
      buC = null;
      buD = null;
      buE = null;
    }
  }


  public final static String [] HELP_FRAME500A =  {
    "Check that we handle fragments with stricter but overlaping import requirements"
  };

  class Frame500a extends FWTestCase {

    public void runTest() throws Throwable {
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME500A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME500A:FAIL");
      }

      assertTrue("Fragment should be resolved", buB.getState() == Bundle.RESOLVED);
      assertTrue("Exporter should be resolved", buC.getState() == Bundle.RESOLVED);
      
      out.println("### framework test bundle :FRAME500A:PASS");
    }
  }

  public final static String [] HELP_FRAME510A =  {
    "Split packages via require bundle directive."
  };

  class Frame510a extends FWTestCase {

    public void runTest() throws Throwable {
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buD = Util.installBundle(bc, "fb_D_api-2.0.0.jar");
      assertNotNull(buD);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME510A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME510A:FAIL");
      }

      assertTrue("Fragment should not be resolved", buB.getState() == Bundle.INSTALLED);
      assertTrue("Exporter should be resolved", buD.getState() == Bundle.RESOLVED);
      
      out.println("### framework test bundle :FRAME510A:PASS");
    }

  }


  public final static String [] HELP_FRAME520A =  {
    "Refresh of required bundle."
  };

  class Frame520a extends FWTestCase {

    public void runTest() throws Throwable {
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      buD = Util.installBundle(bc, "fb_D_api-2.0.0.jar");
      assertNotNull(buC);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME520A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME520A:FAIL");
      }

      /* It is not specificly specified if we should api 1 or 2,
         but the intention seems to be 2 and no fragment */
      assertTrue("Fragment should NOT be resolved", buB.getState() == Bundle.INSTALLED);
      assertTrue("Exporter 1.0 should NOT be resolved", buC.getState() == Bundle.INSTALLED);
      assertTrue("Exporter 2.0 should be resolved", buD.getState() == Bundle.RESOLVED);
      
      out.println("### framework test bundle :FRAME520A:PASS");
    }

  }


  public final static String [] HELP_FRAME530A =  {
    "Refresh of required bundle."
  };

  class Frame530a extends FWTestCase {

    public void runTest() throws Throwable {
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      buE = Util.installBundle(bc, "fb_E-2.0.0.jar");
      assertNotNull(buC);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME530A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME530A:FAIL");
      }

      /* It is not specificly specified if we should api 1 or 2,
         but the intention seems to be 2 and no fragment */
      assertTrue("Fragment 1.0 should NOT be resolved", buB.getState() == Bundle.INSTALLED);
      assertTrue("Exporter 1.0 should be resolved", buC.getState() == Bundle.RESOLVED);
      assertTrue("Fragment 2.0 should be resolved", buE.getState() == Bundle.RESOLVED);
      
      out.println("### framework test bundle :FRAME530A:PASS");
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
