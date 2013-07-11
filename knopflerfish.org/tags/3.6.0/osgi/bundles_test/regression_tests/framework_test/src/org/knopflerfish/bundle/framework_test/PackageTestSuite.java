/*
 * Copyright (c) 2004-2011, KNOPFLERFISH project
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
  Bundle buI;

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
    addTest(new Setup());
    addTest(new Frame320a());
    addTest(new Cleanup());
    addTest(new Setup());
    addTest(new Frame325a());
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
      refreshPackages(bc, null);

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
	buI,
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
      buI = null;
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
      // Register and start the two test bundles 
      try {
	buPT1 = Util.installBundle(bc, "bundlePT1_test-1.0.0.jar");
	buPT1.start();
      }
      catch (BundleException bexcR) {
	fail("framework test bundle "+ bexcR +"(" + bexcR.getNestedException() + ") :FRAME300A:FAIL");
      }
      catch (SecurityException secR) {
	fail("framework test bundle "+ secR +" :FRAME300A:FAIL");
      }

      String ciStr = checkImport(bc, buA_package, new Version(1,5,2),
                                 new Bundle[] {buPT1});
      if ( ciStr != null ) {
	  fail(ciStr +  ":FRAME300A:FAIL");
      }

      out.println("### framework test bundle :FRAME300A:PASS");
    }
  }


  public final static String [] HELP_FRAME305A =  {
    "Install bundlePT2 and see that it can import from bundleA_test"
  };
  
  class Frame305a extends FWTestCase {

    public void runTest() throws Throwable {
      try {
	buPT2 = Util.installBundle(bc, "bundlePT2_test-1.0.0.jar");
	buPT2.start();
      }
      catch (BundleException bexcR) {
	fail("framework test bundle "+ bexcR +"(" + bexcR.getNestedException() + ") :FRAME305A:FAIL");
      }
      catch (SecurityException secR) {
	fail("framework test bundle "+ secR +" :FRAME305A:FAIL");
      }

      String ciStr = checkImport(bc, buA_package, new Version(1,0,0),
                                 new Bundle[] {buPT2});
      if ( ciStr != null ) {
	  fail(ciStr +  ":FRAME305A:FAIL");
      }

      out.println("### framework test bundle :FRAME305A:PASS");
    }
  }


  public final static String [] HELP_FRAME310A =  {
    "Install bundlePT3 and see that it can import from bundleA_test"
  };
  
  class Frame310a extends FWTestCase {

    public void runTest() throws Throwable {
      try {
	buPT3 = Util.installBundle(bc, "bundlePT3_test-1.0.0.jar");
      }
      catch (BundleException bexcR) {
	fail("framework test bundle "+ bexcR +"(" + bexcR.getNestedException() + ") :FRAME310A:FAIL");
      }
      catch (SecurityException secR) {
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
	fail("framework test bundle "+ secR +" :FRAME310A:FAIL");
      }

      out.println("### framework test bundle :FRAME310A:PASS");
    }
  }


  public final static String [] HELP_FRAME320A =  {
    "Optional import test. BundleA exports package A version 1.0 and B",
    "BundleA1 exports package A version 1.5.2.",
    "Start bundleI that imports package A 1.5 or better optionaly and B",
    "any version. BundleI should import package A from A1 and package B",
    " from A and BundleA should import package A from A1."
  };

  class Frame320a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("### framework test bundle :FRAME320A start");

      try {
        buI = Util.installBundle(bc, "bundleI_test-1.0.0.jar");
        assertTrue("BundleI should be INSTALLED",
                   buI.getState() == Bundle.INSTALLED);
      } catch (BundleException bexcA) {
        out.println("Unexpected bundle exception: "+bexcA);
        bexcA.printStackTrace();
        fail("framework test bundle "+ bexcA +" :FRAME320A:FAIL");
      } catch (SecurityException secA) {
        out.println("Unexpected security exception: "+secA);
        secA.printStackTrace();
        fail("framework test bundle "+ secA +" :FRAME320A:FAIL");
      }

      // Start I
      try {
        buI.start();
        assertTrue("BundleI should be ACTIVE",
                   buI.getState() == Bundle.ACTIVE);
      } catch (BundleException bexcA) {
        out.println("Unexpected bundle exception: "+bexcA);
        bexcA.printStackTrace();
        fail("framework test bundle "+ bexcA +" :FRAME320A:FAIL");
      } catch (IllegalStateException ise) {
        out.println("Unexpected illegal state exception: "+ise);
        ise.printStackTrace();
        fail("framework test bundle "+ ise +" :FRAME320A:FAIL");
      } catch (SecurityException sec) {
        out.println("Unexpected security exception: "+sec);
        sec.printStackTrace();
        fail("framework test bundle "+ sec +" :FRAME320A:FAIL");
      }
      assertTrue("BundleA should be RESOLVED",
                 buA.getState() == Bundle.RESOLVED);
      assertTrue("BundleA1 should be RESOLVED",
                 buA1.getState() == Bundle.RESOLVED);
      String ciStr = checkImport(bc, buA_package, new Version(1,5,2),
                                 new Bundle[] {buI, buA});
      if ( ciStr != null ) {
	  fail(ciStr +  ":FRAME320A:FAIL");
      }

      out.println("### framework test bundle :FRAME320A:PASS");
    }
  }


  public final static String [] HELP_FRAME325A =  {
    "Optional import test. BundleA exports package version 1.0",
    "then start bundleI that imports 1.5 or better optionally. Check that",
    "bundle start fails, meaning it did not get the package. But the bundle",
    "resolves, meaning that the import was optional."
  };

  class Frame325a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("### framework test bundle :FRAME325A start");

      try {
        buI = Util.installBundle(bc, "bundleI_test-1.0.0.jar");
        assertTrue("BundleI should be INSTALLED",
                   buI.getState() == Bundle.INSTALLED);
      } catch (BundleException bexcA) {
        out.println("Unexpected bundle exception: "+bexcA);
        bexcA.printStackTrace();
        fail("framework test bundle "+ bexcA +" :FRAME325A:FAIL");
      } catch (SecurityException secA) {
        out.println("Unexpected security exception: "+secA);
        secA.printStackTrace();
        fail("framework test bundle "+ secA +" :FRAME325A:FAIL");
      }

      // Uninstall exporter of package version 1.5.2
      buA1.uninstall();

      // Start I
      try {
        buI.start();
        fail("framework test bundleI start should fail :FRAME325A:FAIL");
      } catch (BundleException bexcA) {
        System.out.println("STATE I = " + buI.getState() + " should " + Bundle.RESOLVED);
        assertTrue("BundleI should be RESOLVED",
                   buI.getState() == Bundle.RESOLVED);
      } catch (IllegalStateException ise) {
        out.println("Unexpected illegal state exception: "+ise);
        ise.printStackTrace();
        fail("framework test bundle "+ ise +" :FRAME325A:FAIL");
      } catch (SecurityException sec) {
        out.println("Unexpected security exception: "+sec);
        sec.printStackTrace();
        fail("framework test bundle "+ sec +" :FRAME325A:FAIL");
      }
      assertTrue("BundleA should be RESOLVED",
                 buA.getState() == Bundle.RESOLVED);
      String ciStr = checkImport(bc, buA_package, new Version(1,0,0),
                                 new Bundle[] {});
      if ( ciStr != null ) {
	  fail(ciStr +  ":FRAME325A:FAIL");
      }

      out.println("### framework test bundle :FRAME325A:PASS");
    }
  }


  //
  // Help methods
  //

  private String checkImport(BundleContext bc, String pkg, Version ver, Bundle[] eibs)
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
      Bundle[] ibs = exp.getImportingBundles();
      for (int i = 0; i < eibs.length; i++) {
        for (int j = 0; j < ibs.length; j++) {
          if (eibs[i] == ibs[j]) {
            eibs[i] = null;
            ibs[j] = null;
          } 
        }
      }
      for (int i = 0; i < eibs.length; i++) {
        if (eibs[i] != null) {
          return "Missing importer: " + eibs[i].getLocation();
        }
      }
      for (int j = 0; j < ibs.length; j++) {
        if (ibs[j] != null) {
          return "Unexpected importer: " + ibs[j].getLocation();
        }
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
