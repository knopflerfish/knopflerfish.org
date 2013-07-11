/*
 * Copyright (c) 2010-2011, KNOPFLERFISH project
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

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.security.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

import junit.framework.*;

import org.knopflerfish.service.framework_test.FrameworkTest;


public class FragmentTestSuite extends TestSuite implements FrameworkTest {
  BundleContext bc;
  Bundle bu;

  // PackageAdmin
  ServiceReference paSR = null;
  PackageAdmin pa = null;

  // Test target bundles
  Bundle buA;
  Bundle buB;
  Bundle buC;
  Bundle buD;
  Bundle buE;
  Bundle buF;
  Bundle buG;

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
    addTest(new Frame550a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame560a());
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
      paSR = bc.getServiceReference(PackageAdmin.class.getName());
      pa = (PackageAdmin)bc.getService(paSR);
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
      buF = null;
      buG = null;

      if (pa != null) {
        bc.ungetService(paSR);
        pa = null;
      }
    }
  }


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
         but the intention seems to be 2 and no fragment */
      assertTrue("Fragment should NOT be resolved", buB.getState() == Bundle.INSTALLED);
      assertTrue("Exporter 1.0 should NOT be resolved", buC.getState() == Bundle.INSTALLED);
      assertTrue("Exporter 2.0 should be resolved", buD.getState() == Bundle.RESOLVED);
      
      out.println("### framework test bundle :FRAME520A:PASS");
    }

  }


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


  public final static String [] HELP_FRAME550A =  {
    "Check that we handle dynamic attach of fragments"
  };

  class Frame550a extends FWTestCase {

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
             +"(" + bexcR.getNestedException() + ") :FRAME550A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME550A:FAIL");
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
      } catch (IOException ioe) {
      }

      buC = Util.installBundle(bc, "fb_C_api-1.0.0.jar");
      assertNotNull(buC);
      try {
        assertTrue("Fragment 1.0 should be resolved", pa.resolveBundles(new Bundle [] {buB}));
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME550A:FAIL");
      }

      fragData = buF.getResource("/test_fb/fragment/data2");
      assertNotNull(fragData);
      try {
        InputStream is = fragData.openStream();
        byte [] buf = new byte[1];
        assertTrue("Host should be able to read Fragment B data", is.read(buf) == 1);
        assertEquals("Host should get Fragment B data", "B", new String(buf));
        is.close();
      } catch (IOException ioe) {
      }

      fragData = buF.getResource("/test_fb/fragment/data");
      assertNotNull(fragData);
      try {
        InputStream is = fragData.openStream();
        byte [] buf = new byte[1];
        assertTrue("Host should still be able to read Fragment G data", is.read(buf) == 1);
        assertEquals("Host should still get Fragment G data", "G", new String(buf));
        is.close();
      } catch (IOException ioe) {
      }

      out.println("### framework test bundle :FRAME550A:PASS");
    }

  }


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
      } catch (InterruptedException _) {
      }

      assertEquals("Class should come from Host after refresh", "HOST", checkApi(1));

      out.println("### framework test bundle :FRAME560A:PASS");
    }

  }


  //
  // Help methods
  //

  /**
   */
  private String checkApi(int version)
  {
    ServiceReference sr = null;
    Object fa = null;

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
    Class c = fa.getClass();
    Class [] ci = c.getInterfaces();
    if (ci.length != 1) {
      return "Doesn't have a FragApi interface";
    }
    try {
      Field ver = ci[0].getDeclaredField("VERSION");
      int fv = ver.getInt(fa);
      if (fv != version) {
        return "Wrong FragApi version, was " + fv + " expected " + version;
      }
    } catch (IllegalAccessException _) {
      return "Failed to find access FragApi version";
    } catch (NoSuchFieldException _) {
      return "Failed to find FragApi version";
    }

    // Get source
    String res;
    try {
      Method where = c.getDeclaredMethod("where", new Class [] {});
      if (where.getReturnType() != String.class) {
        return "Method where doesn't return String";
      }
      res = (String)where.invoke(fa, null);
    } catch (NoSuchMethodException _) {
      return "Failed to find where method";
    } catch (IllegalAccessException _) {
      return "Failed to find access where method";
    } catch (InvocationTargetException _) {
      return "Failed to invoke where method";
    }

    bc.ungetService(sr);

    return res;
  }


}
