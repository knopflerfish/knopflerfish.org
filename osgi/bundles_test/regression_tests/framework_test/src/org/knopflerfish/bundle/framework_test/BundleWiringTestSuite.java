/* Copyright (c) 2014-2014, KNOPFLERFISH project
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
import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.knopflerfish.service.framework_test.FrameworkTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

public class BundleWiringTestSuite extends TestSuite implements FrameworkTest
{
  BundleContext bc;

  // Test target bundles
  Bundle buB;

  PrintStream out = System.out;


  public BundleWiringTestSuite (BundleContext bc) {
    super("BundleWiringTestSuite");
    this.bc = bc;

    addTest(new Setup());
    addTest(new Frame700a());
    addTest(new Cleanup());
  }

  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      // Uninstalls the test target bundles

      Bundle[] bundles = new Bundle[] {
        buB,
      };
      for(int i = 0; i < bundles.length; i++) {
        if (bundles[i] != null) {
          try {
            bundles[i].uninstall();
          } catch (Exception ignored) { }
        }
      }
      buB = null;
    }
  }


  public final static String [] HELP_FRAME700A =  {
    "Check that listResources work correctly"
  };

  class Frame700a extends FWTestCase {

    public void runTest() throws Throwable {
      buB = Util.installBundle(bc, "bundleB_test-1.0.0.jar");
      assertNotNull(buB);
      FrameworkWiring fw = bc.getBundle(0).adapt(FrameworkWiring.class);
      assertNotNull(fw);
      fw.resolveBundles(Collections.singleton(buB));
      BundleWiring wiring = buB.adapt(BundleWiring.class);
      assertNotNull(wiring);
      Collection resources = wiring.listResources("/", "*.class", 0);
      assertEquals("Class files in top of bundleB_test", 0, resources.size());
      resources = wiring.listResources("/org/knopflerfish/bundle", "*.class",
          BundleWiring.FINDENTRIES_RECURSE);
      assertEquals("Class files in bundleB_test", 2, resources.size());
      resources = wiring.listResources("/", "*.class",
          BundleWiring.FINDENTRIES_RECURSE + BundleWiring.LISTRESOURCES_LOCAL);
      assertEquals("Class files in bundleB_test", 3, resources.size());
      resources = wiring.listResources("org/", null,
          BundleWiring.FINDENTRIES_RECURSE + BundleWiring.LISTRESOURCES_LOCAL);
      assertEquals("All entires in bundleB_test org", 8, resources.size());
      out.println("### framework test bundle :FRAME700A:PASS");
    }
  }

}
