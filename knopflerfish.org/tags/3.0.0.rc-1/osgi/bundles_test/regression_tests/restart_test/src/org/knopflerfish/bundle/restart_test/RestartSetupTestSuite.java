/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.restart_test;

import java.util.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;

import junit.framework.*;
import org.osgi.service.startlevel.*;

public class RestartSetupTestSuite extends TestSuite {
  BundleContext bc;

  PrintStream out = System.out;

  State state;
  StartLevel sl;

  public RestartSetupTestSuite (BundleContext bc) {
    super("RestartSetupTestSuite");
    this.bc = bc;

    addTest(new Setup());
    addTest(new Restart001());
    addTest(new Cleanup());
  }
 

  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
      File f = bc.getDataFile(Activator.STATE_FILENAME);
      assertFalse(Activator.STATE_FILENAME + " shouldn't exists", f.exists());

      ServiceReference sr = bc.getServiceReference(StartLevel.class.getName());
      assertNotNull("StartLevel references cannot be null", sr);

      sl = (StartLevel)bc.getService(sr);

      assertNotNull("StartLevel service cannot be null", sr);

      state = new State(sl);
      
      int level = sl.getStartLevel();
      assertEquals("Startlevel must be same as in setup run", 20, level);
    }
  }

  void sleep() throws Exception {
    Thread.sleep(1000);
  }

  class Restart001 extends FWTestCase {
    public void runTest() throws Throwable {

      sl.setInitialBundleStartLevel(5);

      // install and start a bundle
      Bundle buA = Util.installBundle(bc, "bundleA_test-1.0.0.jar");
      buA.start();
      sleep();
      state.addBundle(buA);

      // just install a bundle
      Bundle buB = Util.installBundle(bc, "bundleB_test-1.0.0.jar");
      sleep();
      state.addBundle(buB);

      // install and start a bundle, then uninstall it
      Bundle buC = Util.installBundle(bc, "bundleC_test-1.0.0.jar");
      buC.start();

      buC.uninstall();
      sleep();
      state.addBundle(buC);

      sl.setInitialBundleStartLevel(7);

      // install and start a bundle that will fail in the stop method
      Bundle buF = Util.installBundle(bc, "bundleF_test-1.0.0.jar");
      buF.start();
      sleep();
      state.addBundle(buF);

    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      File f = bc.getDataFile(Activator.STATE_FILENAME);

      assertFalse(Activator.STATE_FILENAME + " shouldn't exists", f.exists());

      FileOutputStream fout = null;
      try {
	fout = new FileOutputStream(f);
	state.save(fout, "state");
	out.println("saved state=" + state);
      } catch (Exception e) {
	fail("Failed to mark as started: " + e);
      } finally {
	try {  fout.close(); } catch (Exception ignored) { }
      }
    }
  }
}
