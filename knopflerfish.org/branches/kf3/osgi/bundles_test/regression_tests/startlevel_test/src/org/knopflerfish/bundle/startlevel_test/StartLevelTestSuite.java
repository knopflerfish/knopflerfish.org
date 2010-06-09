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

package org.knopflerfish.bundle.startlevel_test;

import java.util.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;

import junit.framework.*;
import org.osgi.service.packageadmin.*;
import org.osgi.service.startlevel.*;

public class StartLevelTestSuite extends TestSuite {

  BundleContext bc;

  Bundle buA = null;
  Bundle buB = null;
  Bundle buC = null;
  Bundle buD = null;

  PrintStream out = System.out;

  SyncBundleListener syncBListen;

  PackageAdmin pa;
  StartLevel sl;

  int baseLevel;


  public StartLevelTestSuite (BundleContext bc) {
    super("StartLevelTestSuite");
    this.bc = bc;

    addTest(new Setup());
    addTest(new StartLevel100a());
    addTest(new Cleanup());
  }


  public final static String [] HELP_STARTLVL100A =  {
    "Install bundleA_test, bundleB_test, bundleC_test and bundleD_test",
    "Start with help start level service and check correct order",
    "Update and refresh and check correct stop/start order",
  };
  
  class StartLevel100a extends FWTestCase {

    public void runTest() throws Throwable {
      boolean pass = true;

      out.println("### framework test bundle :STARTLVL100A start");

      try {
        buA = Util.installBundle(bc, "bundleSLA_test-1.0.0.jar");
        sl.setBundleStartLevel(buA, baseLevel + 10);
      } catch (Exception e) {
        out.println("Unexpected exception: "+e);
        e.printStackTrace();
        fail("framework test bundle "+ e +" :STARTLVL100A:FAIL");
      }

      buB = null;
      try {
        buB = Util.installBundle(bc, "bundleSLB_test-1.0.0.jar");
        sl.setBundleStartLevel(buB, baseLevel + 30);
      } catch (Exception e) {
        out.println("Unexpected exception: "+e);
        e.printStackTrace();
        fail("framework test bundle "+ e +" :STARTLVL100A:FAIL");
      }

      buC = null;
      try {
        buC = Util.installBundle(bc, "bundleSLC_test_api-1.0.0.jar");
        sl.setBundleStartLevel(buC, baseLevel + 20);
      } catch (Exception e) {
        out.println("Unexpected exception: "+e);
        e.printStackTrace();
        fail("framework test bundle "+ e +" :STARTLVL100A:FAIL");
      }

      try {
        buA.start();
        assertTrue("BundleA should not be ACTIVE", buA.getState() != Bundle.ACTIVE);
      } catch (Exception e) {
        out.println("Unexpected exception: "+e);
        e.printStackTrace();
        fail("framework test bundle "+ e +" :STARTLVL100A:FAIL");
      }

      try {
        buB.start();
        assertTrue("BundleB should not be ACTIVE", buB.getState() != Bundle.ACTIVE);
      } catch (Exception e) {
        out.println("Unexpected exception: "+e);
        e.printStackTrace();
        fail("framework test bundle "+ e +" :STARTLVL100A:FAIL");
      }

      try {
        buC.start();
        assertTrue("BundleC should not be ACTIVE", buC.getState() != Bundle.ACTIVE);
      } catch (Exception e) {
        out.println("Unexpected exception: "+e);
        e.printStackTrace();
        fail("framework test bundle "+ e +" :STARTLVL100A:FAIL");
      }

      syncBListen.clearEvents();

      sl.setStartLevel(baseLevel + 30);

      pass = syncBListen.checkEvents(new BundleEvent [] {
          new BundleEvent(BundleEvent.STARTED, buA),
          new BundleEvent(BundleEvent.STARTED, buC),
          new BundleEvent(BundleEvent.STARTED, buB)
        });
      assertTrue("Bundle A, C, B should start", pass);
      
      buD = null;
      try {
        buD = Util.installBundle(bc, "bundleSLD_test-1.0.0.jar");
        sl.setBundleStartLevel(buD, baseLevel + 15);
        buD.start();
        assertTrue("BundleD should be ACTIVE", buD.getState() == Bundle.ACTIVE);
      } catch (Exception e) {
        out.println("Unexpected exception: "+e);
        e.printStackTrace();
        fail("start level test bundle "+ e +" :STARTLVL100A:FAIL");
      }

      syncBListen.clearEvents();

      Util.updateBundle(bc, buC, "bundleSLC_test_api-1.0.0.jar");

      // Check BundleEvent stop/start C
      pass = syncBListen.checkEvents(new BundleEvent [] {
          new BundleEvent(BundleEvent.STOPPED, buC),
          new BundleEvent(BundleEvent.STARTED, buC),
        });
      assertTrue("Bundle C should stop and start", pass);

      syncBListen.clearEvents();

      pa.refreshPackages(new Bundle [] {buA, buB, buC, buD});

      // Check BundleEvent stop order B, C, D, A
      // Check BundleEvent start order A, D, C, B
      pass = syncBListen.checkEvents(new BundleEvent [] {
          new BundleEvent(BundleEvent.STOPPED, buB),
          new BundleEvent(BundleEvent.STOPPED, buC),
          new BundleEvent(BundleEvent.STOPPED, buD),
          new BundleEvent(BundleEvent.STOPPED, buA),
          new BundleEvent(BundleEvent.STARTED, buA),
          new BundleEvent(BundleEvent.STARTED, buD),
          new BundleEvent(BundleEvent.STARTED, buC),
          new BundleEvent(BundleEvent.STARTED, buB)
        });
      assertTrue("Bundle B, C, D, A should stop and start in reverse", pass);

      buA.uninstall();
      buA = null;
      buB.uninstall();
      buB = null;
      buC.uninstall();
      buC = null;
      buD.uninstall();
      buD = null;

      out.println("### start level test bundle :STARTLVL100A:PASS");
    }

  }

  class Setup extends FWTestCase {

    public void runTest() throws Throwable {
      syncBListen = new SyncBundleListener();
      try {
        bc.addBundleListener(syncBListen);
      } catch (IllegalStateException ise) {
        fail("start level test bundle "+ ise + " :SETUP:FAIL");
      }

      ServiceReference sr = bc.getServiceReference(PackageAdmin.class.getName());
      assertNotNull("PackageAdmin reference cannot be null", sr);
      pa = (PackageAdmin) bc.getService(bc.getServiceReference(PackageAdmin.class.getName()));
      assertNotNull("PackageAdmin service cannot be null", pa);

      sr = bc.getServiceReference(StartLevel.class.getName());
      assertNotNull("StartLevel reference cannot be null", sr);

      sl = (StartLevel)bc.getService(sr);
      assertNotNull("StartLevel service cannot be null", sl);

      baseLevel = sl.getStartLevel();

      out.println("PASSED: Setup, sl=" + baseLevel);
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      if (buA != null) {
        buA.uninstall();
        buA = null;
      }
      if (buB != null) {
        buB.uninstall();
        buB = null;
      }
      if (buC != null) {
        buC.uninstall();
        buC = null;
      }
      if (buD != null) {
        buD.uninstall();
        buD = null;
      }
    }
  }

  class SyncBundleListener implements SynchronousBundleListener {
    Vector/*<BundleEvent>*/ events = new Vector();

    public void bundleChanged (BundleEvent evt) {
      if (evt.getType() == BundleEvent.STARTED
          || evt.getType() == BundleEvent.STOPPED) {
        events.addElement(evt);
      }
    }


    public void clearEvents() {
      events.clear();
    }


    public boolean checkEvents(BundleEvent [] expevents) {
      boolean res = true;
      for (int i = 0; i < 20; i++) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignore) { }
        if (events.size() == expevents.length) {
          break;
        }
      }
      if (events.size() == expevents.length) {
        for (int i = 0; i< events.size() ; i++) {
          BundleEvent be = (BundleEvent) events.elementAt(i);
          if (!(be.getBundle().equals(expevents[i].getBundle())
                && be.getType() == expevents[i].getType())) {
            res = false;
	  }
	}
      } else {
        res = false;
      }
      if (!res) {
        out.println("Real events");
        for (int i = 0; i < events.size() ; i++) {
          BundleEvent be = (BundleEvent) events.elementAt(i);
          out.println("Event " + be.getBundle() + ", Type " + be.getType());
        }
        out.println("Expected events");
        for (int i = 0; i < expevents.length ; i++) {
          out.println("Event " + expevents[i].getBundle() + ", Type " + expevents[i].getType());
        }
      }
      return res;
    }

  }

}
