/*
 * Copyright (c) 2009-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.preferences_test;

import java.io.*;

import org.osgi.framework.*;
import org.osgi.service.prefs.*;

import junit.framework.*;

public class PreferencesTestSuite
  extends TestSuite
{
  Bundle bu;
  BundleContext bc;
  ServiceReference prefsSR;
  PreferencesService prefs;

  final PrintStream out = System.out;

  public PreferencesTestSuite(BundleContext bc) {
    super ("PreferencesTestSuite");

    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new Preferences0100a());
    //addTest(new Preferences0120a());
    addTest(new Cleanup());
  }


  // Get the Preferences Service.
  class Setup extends FWTestCase {
    public void runTest()
      throws Throwable
    {
      assertNotNull("No bundle context...", bc);

      prefsSR = bc.getServiceReference(PreferencesService.class.getName());
      assertNotNull("No preferences service reference available", prefsSR);

      prefs = (PreferencesService) bc.getService(prefsSR);
      assertNotNull("No preferences service reference available", prefs);
    }
  }

  // Unget the Preferences Service.
  class Cleanup extends FWTestCase {
    public void runTest()
      throws Throwable
    {
      prefs = null;
      bc.ungetService(prefsSR);
      prefsSR = null;
    }
  }

  public class Preferences0100a extends FWTestCase {
    public String getDescription() {
      // Test case for bug fixed in 2.0.4
      return "Test that a deleted preferences node can be re-used"
        +" after a flush.";
    }

    public void runTest()
      throws Throwable
    {
      assertNotNull(prefs);

      final Preferences p = prefs.getUserPreferences("parent");
      assertNotNull("User prefs tree 'parent'", p);

      Preferences p2 = p.node("child");
      assertNotNull("/parent/child", p);

      p2.put("key", "data");
      p2.flush();

      p2.removeNode();
      p.flush();

      p2 = p.node("child");
      p2.put("newkey", "newdata");

      p.removeNode();
      try {
        p.flush();
      } catch (IllegalStateException ise) {
        // Expected exception in some implementations...
      }
    }
  }


}
