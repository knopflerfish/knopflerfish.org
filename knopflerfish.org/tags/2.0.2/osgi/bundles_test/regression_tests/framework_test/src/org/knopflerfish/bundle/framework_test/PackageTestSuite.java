/*
 * Copyright (c) 2004-2005, KNOPFLERFISH project
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

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;
import org.knopflerfish.service.framework_test.*;

import org.osgi.service.packageadmin.*;

import junit.framework.*;

public class PackageTestSuite extends TestSuite implements FrameworkTest {
  BundleContext bc;
  Bundle bu;

  // Sample export bundle
  Bundle buA;
  Bundle buA1;

  // Package version test bundles
  Bundle buPT1;
  Bundle buPT2;
  Bundle buPT3;

  // the three event listeners
  FrameworkListener fListen;
  BundleListener bListen;
  ServiceListener sListen;

  Properties props = System.getProperties();
  String lineseparator = props.getProperty("line.separator");
  Vector events = new Vector();			// vector for events from test bundles
  Vector expevents = new Vector();		// comparision vector

  PrintStream out = System.out;

  final String packServiceName = "org.osgi.service.packageadmin.PackageAdmin";
  final String buA_package = "org.knopflerfish.service.bundleA_test";

  public PackageTestSuite (BundleContext bc) {
    super("PackageTestSuite");
    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new Frame300a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame305a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame310a());
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
      PackageAdmin pa = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
      if (pa == null) {
        fail("Failed to get PackageAdmin service");
      }

      try {
        pa.refreshPackages(null);
      } catch (Exception e) {
        fail("Failed to refresh packages");
      }

      buA = Util.installBundle(bc, "bundleA_test-1.0.0.jar");
      assertNotNull(buA);
      buA1 = Util.installBundle(bc, "bundleA1_test-1.0.1.jar");
      assertNotNull(buA1);
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
	buA ,
	buA1,
	buPT1,
	buPT2,
	buPT3
      };
      for(int i = 0; i < bundles.length; i++) {
	try {  bundles[i].uninstall();  } 
	catch (Exception ignored) { }      
      }

      // Package version test bundles
      buA = null;
      buA1 = null;
      buPT1 = null;
      buPT2 = null;
      buPT3 = null;
    }
  }


  public final static String [] HELP_FRAME300A =  {
    "Install bundlePT1 and see that it can import from bundleA_test"
  };
  
  class Frame300a extends FWTestCase {

    public void runTest() throws Throwable {
      boolean teststatus = true;

      // Register and start the two test bundles 
      try {
	buPT1 = Util.installBundle(bc, "bundlePT1_test-1.0.0.jar");
	buPT1.start();
      }
      catch (BundleException bexcR) {
	teststatus = false;
	fail("framework test bundle "+ bexcR +"(" + bexcR.getNestedException() + ") :FRAME300A:FAIL");
      }
      catch (SecurityException secR) {
	teststatus = false;
	fail("framework test bundle "+ secR +" :FRAME300A:FAIL");
      }

      String ciStr = checkImport(bc, buA_package, new Version(1,5,2), buPT1);
      if ( ciStr != null ) {
	  teststatus = false;
	  fail(ciStr +  ":FRAME300A:FAIL");
      }

      if (teststatus == true) {
	out.println("### framework test bundle :FRAME300A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME300A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAME305A =  {
    "Install bundlePT2 and see that it can import from bundleA_test"
  };
  
  class Frame305a extends FWTestCase {

    public void runTest() throws Throwable {
      boolean teststatus = true;

      // Register and start the two test bundles 
      try {
	buPT2 = Util.installBundle(bc, "bundlePT2_test-1.0.0.jar");
	buPT2.start();
      }
      catch (BundleException bexcR) {
	teststatus = false;
	fail("framework test bundle "+ bexcR +"(" + bexcR.getNestedException() + ") :FRAME305A:FAIL");
      }
      catch (SecurityException secR) {
	teststatus = false;
	fail("framework test bundle "+ secR +" :FRAME305A:FAIL");
      }

      String ciStr = checkImport(bc, buA_package, new Version(1,0,0), buPT2);
      if ( ciStr != null ) {
	  teststatus = false;
	  fail(ciStr +  ":FRAME305A:FAIL");
      }

      if (teststatus == true) {
	out.println("### framework test bundle :FRAME305A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME305A:FAIL");
      }
    }
  }


  public final static String [] HELP_FRAME310A =  {
    "Install bundlePT3 and see that it can import from bundleA_test"
  };
  
  class Frame310a extends FWTestCase {

    public void runTest() throws Throwable {
      boolean teststatus = true;

      try {
	buPT3 = Util.installBundle(bc, "bundlePT3_test-1.0.0.jar");
      }
      catch (BundleException bexcR) {
	teststatus = false;
	fail("framework test bundle "+ bexcR +"(" + bexcR.getNestedException() + ") :FRAME310A:FAIL");
      }
      catch (SecurityException secR) {
	teststatus = false;
	fail("framework test bundle "+ secR +" :FRAME310A:FAIL");
      }
      try {
	buPT3.start();
	fail("framework test bundle start did start, but it should fail :FRAME310A:FAIL");
      }
      catch (BundleException bexcR) {
	// start should fail with BundleException
      }
      catch (SecurityException secR) {
	teststatus = false;
	fail("framework test bundle "+ secR +" :FRAME310A:FAIL");
      }

      if (teststatus == true) {
	out.println("### framework test bundle :FRAME310A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME310A:FAIL");
      }
    }
  }


  //
  // Help methods
  //

  private String checkImport(BundleContext bc, String pkg, Version ver, Bundle b)
  {
      PackageAdmin packService = null;

      try {
	packService = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
	if (packService == null) {
	  return "Got null service " + packServiceName;
	} 
      }
      catch (Exception ex) {
	return "Got exception: " + ex;
      }

      ExportedPackage exp = packService.getExportedPackage(buA_package);

      if (exp == null) {
	return "Imported wrong package, found no version";
      } else if (ver.compareTo(exp.getVersion()) != 0) {
	return "Imported wrong package, found " + exp;
      }
      return null;
  }


  private void refreshPackages(BundleContext bc, Bundle [] bs)
  {
      PackageAdmin packService = null;

      try {
	packService = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
	if (packService == null) {
	  return;
	} 
      }
      catch (Exception ex) {
	return;
      }

      packService.refreshPackages(bs);

      try {
	Thread.sleep(900);
      }
      catch (Exception ex) {
	out.println("### framework test bundle :FRAME187A exception");
	ex.printStackTrace(out);
      }

  }

}
