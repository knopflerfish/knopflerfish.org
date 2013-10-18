/*
 * Copyright (c) 2004-2008, KNOPFLERFISH project
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

public class RestartTestSuite extends TestSuite {
  BundleContext bc;

  PrintStream out = System.out;

  State state = null;
  StartLevel sl;

  public RestartTestSuite (BundleContext bc) {
    super("RestartTestSuite");
    this.bc = bc;

    addTest(new Setup());
    addTest(new Restart050());
    addTest(new Cleanup());
  }


  class Restart050 extends FWTestCase {
    public void runTest() throws Throwable {
      Bundle[] bl = bc.getBundles();
      for(int i = 0; i < bl.length; i++) {
        out.println(i);
        out.println(i + ", #" + bl[i].getBundleId() +
                    ", " + bl[i].getHeaders().get("Bundle-UUID") +
                    ", state=" + bl[i].getState() +
                    ", level=" + sl.getBundleStartLevel(bl[i]));
      }

      state.assertBundles(bl);
    }
  }

  class Setup extends FWTestCase {
    public void runTest() throws Throwable {

      File f = bc.getDataFile(Activator.STATE_FILENAME);

      assertTrue(Activator.STATE_FILENAME + " must exists", f.exists());

      ServiceReference sr = bc.getServiceReference(StartLevel.class.getName());
      assertNotNull("StartLevel references cannot be null", sr);

      sl = (StartLevel)bc.getService(sr);

      assertNotNull("StartLevel service cannot be null", sr);

      state = new State(sl);
      state.load(new FileInputStream(f));
      out.println("loaded state=" + state);

      out.println("PASSED: Setup, sl=" + sl.getStartLevel());
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }
}
