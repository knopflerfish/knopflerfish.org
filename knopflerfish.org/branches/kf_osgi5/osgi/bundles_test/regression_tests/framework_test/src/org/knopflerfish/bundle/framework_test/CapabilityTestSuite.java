/* Copyright (c) 2013-2013, KNOPFLERFISH project
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
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.packageadmin.*;

import junit.framework.*;

import org.knopflerfish.service.framework_test.FrameworkTest;

public class CapabilityTestSuite extends TestSuite implements FrameworkTest
{
  BundleContext bc;
  FrameworkWiring fw;

  // Test target bundles
  Bundle buCU;
  Bundle buCU1;
  Bundle buCUC1;
  Bundle buCUC2;
  Bundle buCUP1;
  Bundle buCUP2;

  PrintStream out = System.out;


  public CapabilityTestSuite (BundleContext bc) {
    super("CapabiltyTestSuite");
    this.bc = bc;

    addTest(new Setup());
    addTest(new Frame600a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame610a());
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
      fw = bc.getBundle(0).adapt(FrameworkWiring.class);
      FrameworkListener fl = new FrameworkListener() {
        @Override
        public void frameworkEvent(FrameworkEvent event) {
          synchronized (this) {
            notifyAll();
          }
        }
      };
      try {
        synchronized (fl) {
          fw.refreshBundles(null, fl);
          fl.wait(3000);
        }
        buCUC1 = Util.installBundle(bc, "bundleCUC1_test-1.0.0.jar");
        assertNotNull(buCUC1);
        buCUC2 = Util.installBundle(bc, "bundleCUC2_test-2.0.0.jar");
        assertNotNull(buCUC2);
        buCUP1 = Util.installBundle(bc, "bundleCUP1_test-1.0.0.jar");
        assertNotNull(buCUP1);
        buCUP2 = Util.installBundle(bc, "bundleCUP2_test-2.0.0.jar");
        assertNotNull(buCUP2);
      } catch (Exception e) {
        fail("Failed to refresh packages: " + e);
      }
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      // Uninstalls the test target bundles

      Bundle[] bundles = new Bundle[] {
        buCUC1,
        buCUC2,
        buCUP1,
        buCUP2,
        buCU,
      };
      for(int i = 0; i < bundles.length; i++) {
        if (bundles[i] != null) {
          try {
            bundles[i].uninstall();
          } catch (Exception ignored) { }
        }
      }

      fw = null;
      buCU = null;
      buCUC1 = null;
      buCUC2 = null;
      buCUP1 = null;
      buCUP2 = null;
    }
  }


  public final static String [] HELP_FRAME600A =  {
    "Check that we care about uses on Require-Capablity"
  };

  class Frame600a extends FWTestCase {

    public void runTest() throws Throwable {
      buCU = Util.installBundle(bc, "bundleCU_test-1.0.0.jar");
      assertNotNull(buCU);
      try {
        buCU.start();
      } catch (BundleException bexcR) {
        fail("framework test failed to resolve :FRAME600A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME600A:FAIL");
      }

      assertTrue("CUC2 should be resolved", buCUC2.getState() == Bundle.RESOLVED);
      assertTrue("CUC1 should not be resolved", buCUC1.getState() == Bundle.INSTALLED);
      assertTrue("CUP2 should be resolved", buCUP2.getState() == Bundle.RESOLVED);
      assertTrue("CUP1 should not be resolved", buCUP1.getState() == Bundle.INSTALLED);
      
      out.println("### framework test bundle :FRAME600A:PASS");
    }
  }


  public final static String [] HELP_FRAME610A =  {
    "Check that we care about uses on Require-Capablity"
  };

  class Frame610a extends FWTestCase {

    public void runTest() throws Throwable {
      buCU1 = Util.installBundle(bc, "bundleCU1_test-1.0.0.jar");
      assertNotNull(buCU1);
      // NOTE! Currently KF will not resolve the same package with
      // different version within the same resolve operation.
      // Therefore we resolve each package seperatly before trying
      // to resolve the bundle that uses both.
      try {
        buCUC1.start();
      } catch (BundleException bexcR) {
        fail("framework test failed to resolve:FRAME610A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME610A:FAIL");
      }     
      try {
        buCUC2.start();
      } catch (BundleException bexcR) {
        fail("framework test failed to resolve:FRAME610A:FAIL");
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME610A:FAIL");
      }     
      try {
        buCU1.start();
        fail("framework test bundle should not resolve :FRAME610A:FAIL");
      } catch (BundleException bexcR) {
        // Should fail to resolve
        bexcR.printStackTrace(out);
      } catch (SecurityException secR) {
        fail("framework test bundle "+ secR +" :FRAME610A:FAIL");
      }
      
      out.println("### framework test bundle :FRAME610A:PASS");
    }
  }

}
