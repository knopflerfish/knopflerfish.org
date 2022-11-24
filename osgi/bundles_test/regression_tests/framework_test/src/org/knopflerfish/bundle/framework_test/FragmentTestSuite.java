/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.knopflerfish.service.framework_test.FrameworkTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;


public class FragmentTestSuite extends TestSuite implements FrameworkTest {
  BundleContext bc;
  Bundle bu;

  // PackageAdmin
  ServiceReference<PackageAdmin> paSR = null;
  PackageAdmin pa = null;

  // Test target bundles
  Bundle buA;
  Bundle buB;
  Bundle buC;
  Bundle buD;
  Bundle buE;
  Bundle buF;
  Bundle buG;
  Bundle buH;
  Bundle buI;
  Bundle buJ;
  Bundle buK;

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
    addTest(new Setup());
    addTest(new Frame540a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame550b());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame555a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame560a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame570a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame580a());
    addTest(new Cleanup());
  }


  static class FWTestCase extends TestCase {

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
      paSR = bc.getServiceReference(PackageAdmin.class);
      pa = bc.getService(paSR);
      if (pa == null) {
        fail("Failed to get PackageAdmin service");
      }

      try {
        pa.refreshPackages(null);
        Thread.sleep(1000);
      } catch (Exception e) {
        fail("Failed to refresh packages");
      }
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
        buF,
        buG,
        buH,
        buI,
        buJ,
        buK
      };
      for (Bundle bundle : bundles) {
        if (bundle != null) {
          try {
            bundle.uninstall();
          } catch (Exception ignored) {
          }
        }
      }

      buA = null;
      buB = null;
      buC = null;
      buD = null;
      buE = null;
      buF = null;
      buG = null;
      buH = null;
      buI = null;
      buJ = null;
      buK = null;

      if (pa != null) {
        bc.ungetService(paSR);
        pa = null;
      }
    }
  }


  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME500A =  {
    "Check that we handle fragments with stricter but overlaping import requirements"
  };

  class Frame500a extends FWTestCase {

    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
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

  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME510A =  {
    "Check that we handle fragments with stricter but overlaping import requirements"
  };

  class Frame510a extends FWTestCase {

    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
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


  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME520A =  {
    "Check that we handle fragments with stricter but overlaping import requirements"
  };

  class Frame520a extends FWTestCase {

    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      buD = Util.installBundle(bc, "fb_D_api-2.0.0.jar");
      assertNotNull(buD);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME520A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME520A:FAIL");
      }

      /* It is not specificly specified if we should api 1 or 2,
         but since it says that we should attach if we can resolve, we go with 1 */
      assertTrue("Fragment should be resolved", buB.getState() == Bundle.RESOLVED);
      assertTrue("Exporter 1.0 should be resolved", buC.getState() == Bundle.RESOLVED);
      assertTrue("Exporter 2.0 should NOT be resolved", buD.getState() == Bundle.INSTALLED);

      out.println("### framework test bundle :FRAME520A:PASS");
    }

  }


  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME530A =  {
    "Check that we handle fragments with stricter but overlaping import requirements"
  };

  class Frame530a extends FWTestCase {

    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      buE = Util.installBundle(bc, "fb_E-2.0.0.jar");
      assertNotNull(buE);
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


  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME540A =  {
    "Check that we handle dynamic attach of fragments"
  };

  class Frame540a extends FWTestCase {

    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME540A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME540A:FAIL");
      }

      assertTrue("Exporter 1.0 should be resolved", buC.getState() == Bundle.RESOLVED);

      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      try {
        assertTrue("Fragment 1.0 should be resolved", pa.resolveBundles(new Bundle [] {buB}));
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME540A:FAIL");
      }

      out.println("### framework test bundle :FRAME540A:PASS");
    }

  }


  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME550B =  {
    "Check that we handle dynamic attach of fragments"
  };

  class Frame550b extends FWTestCase {

    public void runTest() throws Throwable {
      buF = Util.installBundle(bc, "fb_F-2.0.0.jar");
      assertNotNull(buF);
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buG = Util.installBundle(bc, "fb_G-1.0.0.jar");
      assertNotNull(buG);
      try {
        buF.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME550B:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME550B:FAIL");
      }

      assertTrue("Fragment 1.0 should NOT be resolved", buB.getState() == Bundle.INSTALLED);
      assertTrue("Fragment G should be resolved", buG.getState() == Bundle.RESOLVED);

      URL fragData = buF.getResource("/test_fb/fragment/data");
      assertNotNull(fragData);
      try {
        InputStream is = fragData.openStream();
        byte [] buf = new byte[1];
        assertTrue("Host should be able to read Fragment G data", is.read(buf) == 1);
        assertEquals("Host should get Fragment G data", "G", new String(buf));
        is.close();
      } catch (IOException ignored) {
      }

      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      try {
        assertFalse("Fragment 1.0 should not be resolved", pa.resolveBundles(new Bundle [] {buB}));
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME550B:FAIL");
      }

      out.println("### framework test bundle :FRAME550B:PASS");
    }

  }


  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME555A =  {
    "Check that we attach of fragments with external imports"
  };

  class Frame555a extends FWTestCase {

    public void runTest() throws Throwable {
      buK = Util.installBundle(bc, "fb_K-1.0.0.jar");
      assertNotNull(buK);
      buJ = Util.installBundle(bc, "fb_J_api-1.0.0.jar");
      assertNotNull(buJ);
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME555A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME555A:FAIL");
      }
      assertTrue("Fragment K should be resolved", buK.getState() == Bundle.RESOLVED);
      assertTrue("Fragment J should be resolved", buJ.getState() == Bundle.RESOLVED);

      out.println("### framework test bundle :FRAME555A:PASS");
    }

  }


  @SuppressWarnings("unused")
  public final static String [] HELP_FRAME560A =  {
    "Check that we handle dynamic attach of fragments"
  };

  class Frame560a extends FWTestCase {

    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME560A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME560A:FAIL");
      }

      assertTrue("Fragment 1.0 should be resolved", buB.getState() == Bundle.RESOLVED);

      buB.uninstall();

      assertEquals("Class should come from Fragment 1.0", "FRAG", checkApi(1));

      pa.refreshPackages(new Bundle [] {buB});
      // NYI wait for refreshed event
      try {
        Thread.sleep(5000);
      } catch (InterruptedException ignored) {
      }

      assertEquals("Class should come from Host after refresh", "HOST", checkApi(1));

      out.println("### framework test bundle :FRAME560A:PASS");
    }
  }

  class Frame570a extends FWTestCase {

    public void runTest() throws Throwable {
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      buH = Util.installBundle(bc, "fb_H-1.0.0.jar");
      assertNotNull(buH);
      try {
        buA.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME570A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME570A:FAIL");
      }

      assertTrue("Fragment 1.0 should be resolved", buB.getState() == Bundle.RESOLVED);
      assertTrue("Provider of optional package should be resolved", buH.getState() == Bundle.RESOLVED);
      try {
        buA.loadClass("test_fb.H.LibObject");
      } catch (ClassNotFoundException cnfe) {
        fail("Fragment host should get fragment imported class "+ cnfe +" :FRAME570A:FAIL");
      }
      out.println("### framework test bundle :FRAME570A:PASS");
    }

  }

  class Frame580a extends FWTestCase {

    public void runTest() throws Throwable {
      buB = Util.installBundle(bc, "fb_B-1.0.0.jar");
      assertNotNull(buB);
      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      buI = Util.installBundle(bc, "fb_I-1.0.0.jar");
      assertNotNull(buI);
      assertFalse("Should not resolve since fragment host is missing",
                  pa.resolveBundles(new Bundle [] {buI}));
      buA = Util.installBundle(bc, "fb_A-1.0.0.jar");
      assertNotNull(buA);
      try {
        buI.start();
      } catch (BundleException bexcR) {
        fail("framework test bundle "+ bexcR
             +"(" + bexcR.getNestedException() + ") :FRAME580A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME580A:FAIL");
      }
      out.println("### framework test bundle :FRAME580A:PASS");
    }

  }

  //
  // Help methods
  //

  @SuppressWarnings("SameParameterValue")
  private String checkApi(int version)
  {
    ServiceReference<?> sr;
    Object fa;

    try {
      sr = bc.getServiceReference("test_fapi.FragApi");
      fa = bc.getService(sr);
      if (fa == null) {
        return "No FragApi service available";
      }
    } catch (Exception ex) {
      return "Got exception: " + ex;
    }

    // Check api version
    Class<?> c = fa.getClass();
    Class<?>[] ci = c.getInterfaces();
    if (ci.length != 1) {
      return "Doesn't have a FragApi interface";
    }
    try {
      Field ver = ci[0].getDeclaredField("VERSION");
      int fv = ver.getInt(fa);
      if (fv != version) {
        return "Wrong FragApi version, was " + fv + " expected " + version;
      }
    } catch (IllegalAccessException _ignore) {
      return "Failed to find access FragApi version";
    } catch (NoSuchFieldException _ignore) {
      return "Failed to find FragApi version";
    }

    // Get source
    String res;
    try {
      Method where = c.getDeclaredMethod("where");
      if (where.getReturnType() != String.class) {
        return "Method where doesn't return String";
      }
      res = (String)where.invoke(fa, (Object[])null);
    } catch (NoSuchMethodException _ignore) {
      return "Failed to find where method";
    } catch (IllegalAccessException _ignore) {
      return "Failed to find access where method";
    } catch (InvocationTargetException _ignore) {
      return "Failed to invoke where method";
    }

    bc.ungetService(sr);

    return res;
  }


}
