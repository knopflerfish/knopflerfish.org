/*
 * Copyright (c) 2004-2010, KNOPFLERFISH project
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
import java.math.*;
import java.net.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;
import org.knopflerfish.bundle.framework_test.FrameworkTestSuite.Frame069a;
import org.knopflerfish.service.framework_test.*;

import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.*;

import junit.framework.*;

public class NativeCodeTestSuite extends TestSuite {
  BundleContext bc;
  Bundle        buN;

  // the three event listeners
  FrameworkListener fListen;
  BundleListener bListen;
  ServiceListener sListen;

  Properties props         = System.getProperties();
  String     lineseparator = props.getProperty("line.separator");
  String     test_url_base;
  Vector     events        = new Vector(); // vector for events from test bundles
  Vector     expevents     = new Vector(); // comparision vector


  PrintStream out = System.out;

  public NativeCodeTestSuite(BundleContext bc) {
    super ("NativeCodeTestSuite");

    this.bc = bc;
    test_url_base = "bundle://" + bc.getBundle().getBundleId() + "/";

    addTest(new Setup());
   // addTest(new Frame0135a());
    addTest(new Frame0137a());
    addTest(new Frame0139a());
    addTest(new Cleanup());
  }
 

  // Also install all possible listeners
  public class Setup extends FWTestCase {

    public String getDescription() {
      return "This does some error handling tests of bundles with native code";
    }

    public void runTest() throws Throwable {
      fListen = new FrameworkListener();
      try {
	bc.addFrameworkListener(fListen);
      } catch (IllegalStateException ise) {
	fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }
      
      bListen = new BundleListener();
      try {
	bc.addBundleListener(bListen);
      } catch (IllegalStateException ise) {
	fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }
      
      sListen = new ServiceListener();
      try {
	bc.addServiceListener(sListen);
      } catch (IllegalStateException ise) {
	fail("framework test bundle "+ ise + " :SETUP:FAIL");
      }
      
      out.println("### framework test bundle :SETUP:PASS");
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
	buN ,
      };
      for(int i = 0; i < bundles.length; i++) {
	try {  bundles[i].uninstall();  } 
	catch (Exception ignored) { }      
      }

      buN = null;

      try   { bc.removeFrameworkListener(fListen); } 
      catch (Exception ignored) { }
      fListen = null;

      try   { bc.removeServiceListener(sListen); } 
      catch (Exception ignored) { }
      sListen = null;

      try   { bc.removeBundleListener(bListen); } 
      catch (Exception ignored) { }
      bListen = null;
      
    }
  }


  // 27. Install testbundle N (with native code )
  //     and call its test method, which should return Hello world
  //     The name of the test bundle .jar file to load is made from
  //     a concatenation of the strings
  //     bundleN-<processor>-<osname>[-<osversion>][-<language>]_test.jar

  class Frame0135a extends FWTestCase {
    public void runTest() throws Throwable {
      Dictionary opts = new Hashtable();
      // clearEvents();
      // buN = null;
      String processor = (String) opts.get("processor");
      String osname    = (String) opts.get("osname");
      String osversion = (String) opts.get("osversion");
      String language  = (String) opts.get("language");

      // at present only the processor and osname are used
      StringBuffer b1 = new StringBuffer(test_url_base+"bundleN-"+ processor + "-" + osname);
      //
      if (osversion != null) {
	b1.append("-"+osversion);
      }
      if (language != null) {
	b1.append("-"+language);
      }
      b1.append("_test_all-1.0.0.jar");
      String jarName = b1.toString();

      // out.println("NATIVE " + jarName);
      // out.flush();
      boolean teststatus = true;
    
      try {
	buN = Util.installBundle (bc, jarName);
	buN.start();
      }
      catch (BundleException bex) {
	out.println("framework test bundle "+ bex +" :FRAME135:FAIL");
	Throwable tx = bex.getNestedException();
	if (tx != null) {
	  out.println("framework test bundle, nested exception "+ tx +" :FRAME135:FAIL");
	}
	teststatus = false;
      }
      catch (SecurityException sec) {
	out.println("framework test bundle "+ sec +" :FRAME135A:FAIL");
	teststatus = false;
      }

      if (teststatus == true) {
	// Get the service reference and a service from the native bundle
	ServiceReference srnative = bc.getServiceReference("org.knopflerfish.service.nativetest.NativeTest");
	if (srnative != null) {
	  Object o = bc.getService(srnative);
	  if (o != null) { 
	    // now for some reflection exercises
	    String expectedString = "Hello world";
	    String nativeString = null;
  
	    Method m;
	    Class c;
	    Class parameters[];

	    // out.println("servref  = "+ sr);
	    // out.println("object = "+ obj1);

	    Object[] arguments = new Object[0];
	    parameters = new Class[0];
	    c = o.getClass();
  
	    try {
	      m = c.getMethod("getString", parameters);
	      nativeString = (String) m.invoke(o, arguments);
	      if (!expectedString.equals(nativeString)) {
		out.println("Frame test native bundle method failed, expected: " + expectedString + " got: " + nativeString + ":FRAME135A:FAIL");
		teststatus = false;
	      }
	    }
	    catch (IllegalAccessException ia) {
	      out.println("Frame test IllegaleAccessException" +  ia + ":FRAME135A:FAIL");
	      teststatus = false;
	    }
	    catch (InvocationTargetException ita) {
	      out.println("Frame test InvocationTargetException" +  ita);
	      out.println("Frame test nested InvocationTargetException" +  ita.getTargetException()  + ":FRAME135A:FAIL");
	      teststatus = false;
	    }
	    catch (NoSuchMethodException nme) {
	      out.println("Frame test NoSuchMethodException" +  nme + ":FRAME135A:FAIL");
	      teststatus = false;
	    }
	  } else {
	    out.println("framework test bundle, failed to get service for org.knopflerfish.service.nativetest.NativeTest :FRAME135A:FAIL");
	    teststatus = false;
	  }
	} else {
	  out.println("framework test bundle, failed to get service reference for org.knopflerfish.service.nativetest.NativeTest :FRAME135A:FAIL");
	  teststatus = false;
	}
      }

      if (teststatus == true && buN.getState() == Bundle.ACTIVE) {
	out.println("### framework test bundle :FRAME135A:PASS");
      }
      else {
	out.println("### framework test bundle :FRAME135A:FAIL");
      }
    }
  }


  // Install testbundle N1 & N2 (with faulty native code headers)
  //

  class Frame0137a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
    
      try {
	buN = Util.installBundle (bc, "bundleN1_test-1.0.0.jar");
	buN.start();
        fail("framework faulty native test bundle N1 should not resolve :FRAME137:FAIL");
      } catch (BundleException bex) {
        // Expected bundle exception
      } catch (Exception e) {
        e.printStackTrace();
	fail("framework test bundle N1, unexpected "+ e +" :FRAME137A:FAIL");
      }

      try {
	buN = Util.installBundle (bc, "bundleN2_test-1.0.0.jar");
	buN.start();
        fail("framework faulty native test bundle N2 should not resolve :FRAME137:FAIL");
      } catch (BundleException bex) {
        // Expected bundle exception
      } catch (Exception e) {
        e.printStackTrace();
	fail("framework test bundle N2, unexpected "+ e +" :FRAME137A:FAIL");
      }

      out.println("### framework test bundle :FRAME137A:PASS");
    }
  }


  
  // Install testbundle N3 with arm processor and match with arm_le framework
  //

  class Frame0139a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
    
      try {
	buN = Util.installBundle (bc, "bundleN3_test-1.0.0.jar");
	buN.start();
      } catch (Exception e) {
        e.printStackTrace();
	fail("framework test bundle N3, unexpected "+ e +" :FRAME1370:FAIL");
      }

      out.println("### framework test bundle :FRAME137A:PASS");
    }
  }


  
  // Check of exporting and importing bundles and versions
  
  private boolean checkExportVersion (Object _out, Bundle exporter, Bundle importer, String packName, String version) {
    String packServiceName = "org.osgi.service.packageadmin.PackageAdmin";
    PackageAdmin packService = (PackageAdmin) bc.getService(bc.getServiceReference(packServiceName));
    boolean teststatus = true;
    if (packService == null) {
      teststatus  = false;
      out.println("Got null service " + packServiceName + " in FRAME215A:FAIL");
    } else {
      // Now get the array of exported packages from exporting bundle,
      // with one expected package
      long expId = exporter.getBundleId();
      long impId = importer.getBundleId();
      
      ExportedPackage [] exp2 = packService.getExportedPackages(exporter);

      // For all exported packages in exporter bundle, (with the specified version)
      // look for if they are imported by the importer bundle
      //
      if (exp2 != null) {
	for (int i = 0; i < exp2.length ; i++ ) {
	  // out.println("Got exported package " + exp2[i].getName() + " spev ver. " + exp2[i].getSpecificationVersion() + " in FRAME215A");
	  if (version.equals(exp2[i].getSpecificationVersion()) && packName.equals(exp2[i].getName())) {
	    Bundle [] ib = exp2[i].getImportingBundles();
	    for (int j = 0; j < ib.length ; j++ ) {
	      // out.println("   Importing bundle: " + ib[j].getBundleId());
	      if (ib[j].getBundleId() ==  impId) {
		// out.println ("MATCH p2 p2 hurrah");
		teststatus = true;
	      }
	    }
	  }
	}
      } else {
	teststatus  = false;
	// out.println("Got null exported package array from bundle " + exporter.getBundleId()  +" in FRAME215A");
      }
    }
    return teststatus;
  }

  // General status check functions
  // prevent control characters to be printed
  private String xlateData(byte [] b1) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < b1.length ; i++) {
      if (-128 <= b1[i] && b1[i] < 0) {
	sb.append(new String(b1, i, 1));
      }
      if (0 <= b1[i] && b1[i] < 32) {
	sb.append("^");
	sb.append(String.valueOf(b1[i]));
      } else {
	if (32 <= b1[i] && b1[i] < 127) {
	  sb.append(new String(b1, i, 1));
	}
      }  
    }
    return sb.toString();
  }

  // General printout of PermissionInfo

  private void printPermission(Object _out, PermissionInfo pi) {
    StringBuffer sb1 = new StringBuffer();

    sb1.append("ENCODED: ");
    if (pi.getEncoded() != null) { sb1.append(pi.getEncoded() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("ACTIONS: ");
    if (pi.getActions() != null) { sb1.append(pi.getActions() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("NAME: ");
    if (pi.getName() != null) { sb1.append(pi.getName() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("TYPE: ");
    if (pi.getType() != null) { sb1.append(pi.getType() + lineseparator); } else { sb1.append("null" + lineseparator); }

    sb1.append("STRING: ");
    if (pi.toString() != null) { sb1.append(pi.toString() + lineseparator); } else { sb1.append("null" + lineseparator); }

    out.println(sb1.toString());
  }

  // Condensed printout of PermissionInfo

  private void printPermissionShort (Object _out, PermissionInfo pi) {
    StringBuffer sb1 = new StringBuffer();

    sb1.append("  ENCODED: ");
    if (pi.getEncoded() != null) { sb1.append(pi.getEncoded()); } else { sb1.append("null"); }

    out.println(sb1.toString());
  }

  // Check that the expected implications occur 
  public boolean implyCheck (Object _out, boolean expected, Permission p1, Permission p2) {
    boolean result = true;
    if (p1.implies(p2) == expected) {
      result = true;
    } else {
      out.println("framework test bundle, ...Permission implies method failed");
      out.println("Permission p1: " + p1.toString());
      out.println("Permission p2: " + p2.toString());
      result = false;
    }
    // out.println("DEBUG implies method in FRAME125A");
    // out.println("DEBUG p1: " + p1.toString());
    // out.println("DEBUG p2: " + p2.toString());
    return result;
  }

  public boolean implyCheck (Object _out, boolean expected, PermissionCollection p1, Permission p2) {
    boolean result = true;
    if (p1.implies(p2) == expected) {
      result = true;
    } else {
      out.println("framework test bundle, ...Permission implies method failed");
      out.println("Permission p1: " + p1.toString());
      out.println("Permission p2: " + p2.toString());
      result = false;
    }
    return result;
  }

  /* Interface implementations for this class */

  public java.lang.Object getConfigurationObject() {
    return this;
  }


  // Check that the expected events has reached the listeners and reset the events in the listeners
  private boolean checkListenerEvents(Object _out, boolean fwexp, int fwtype, boolean buexp, int butype, boolean sexp, int stype, Bundle bunX, ServiceReference servX ) {
    boolean listenState = true;	// assume everything will work

    if (fwexp == true) {
      if (fListen.getEvent() != null) {
	if (fListen.getEvent().getType() != fwtype || fListen.getEvent().getBundle() != bunX) {
	  System.out.println("framework test bundle, wrong type of framework event/bundle : " +  fListen.getEvent().getType());
	  System.out.println("framework test bundle, event was from bundle: " + fListen.getEvent().getBundle());
	  Throwable th1 = fListen.getEvent().getThrowable();
	  if (th1 != null) {
	    System.out.println("framework test bundle, exception was: " + th1);
	  }
	  listenState = false;
	}
      }
      else {
	System.out.println("framework test bundle, missing framework event");
	listenState = false;
      }
    }
    else {
      if (fListen.getEvent() != null) {
	listenState = false;
	System.out.println("framework test bundle, unexpected framework event: " +  fListen.getEvent().getType());
	System.out.println("framework test bundle, event was from bundle: " + fListen.getEvent().getBundle());
	Throwable th1 = fListen.getEvent().getThrowable();
	if (th1 != null) {
	  System.out.println("framework test bundle, exception was: " + th1);
	}
      }
    }

    if (buexp == true) {
      if (bListen.getEvent() != null) {
	if (bListen.getEvent().getType() != butype || bListen.getEvent().getBundle() != bunX) {
	  System.out.println("framework test bundle, wrong type of bundle event/bundle: " +  bListen.getEvent().getType());
	  System.out.println("framework test bundle, event was from bundle: " + bListen.getEvent().getBundle().getLocation());
	  listenState = false;
	}
      }
      else {
	System.out.println("framework test bundle, missing bundle event");
	listenState = false;
      }
    }
    else {
      if (bListen.getEvent() != null) {
	listenState = false;
	System.out.println("framework test bundle, unexpected bundle event: " +  bListen.getEvent().getType());
	System.out.println("framework test bundle, event was from bundle: " + bListen.getEvent().getBundle());
      }
    }


    if (sexp == true) {
      if (sListen.getEvent() != null) {
	if (servX != null) {
	  if (sListen.getEvent().getType() != stype || servX != sListen.getEvent().getServiceReference() ) {
	    System.out.println("framework test bundle, wrong type of service event: " +  sListen.getEvent().getType());
	    listenState = false;
	  }
	}
	else { // ignore from which service reference the event came
	  if (sListen.getEvent().getType() != stype ) {
	    System.out.println("framework test bundle, wrong type of service event: " +  sListen.getEvent().getType());
	    listenState = false;
	  }
	}
      }
      else {
	System.out.println("framework test bundle, missing service event");
	listenState = false;
      }
    }
    else {
      if (sListen.getEvent() != null) {
	listenState = false;
	System.out.println("framework test bundle, unexpected service event: " +  sListen.getEvent().getType());
      }
    }

    fListen.clearEvent();
    bListen.clearEvent();
    sListen.clearEvent();
    return listenState;
  }

  private void clearEvents() {
    fListen.clearEvent();
    bListen.clearEvent();
    sListen.clearEvent();
  }
  // Get the bundle that caused the event 
  private Bundle getFEBundle () {
    if (fListen.getEvent() != null) {
      return fListen.getEvent().getBundle();
    }
    else {
      return null;
    }
  }

  private Bundle getBEBundle () {
    if (bListen.getEvent() != null) {
      return bListen.getEvent().getBundle();
    }
    else {
      return null;
    }
  }

  // So that other bundles in the test may get the base url
  public String getBaseURL() {
    return test_url_base;
  }

  // to access test service methods via reflection 
  private void bundleLoad (Object _out, ServiceReference sr, String bundle) {
    Method m;
    Class c, parameters[];

    Object obj1 = bc.getService(sr);
    // System.out.println("servref  = "+ sr);
    // System.out.println("object = "+ obj1);

    Object[] arguments = new Object[1];
    arguments[0] = bundle;              // the bundle to load packages from

    c = obj1.getClass();
    parameters = new Class[1];
    parameters[0] = arguments[0].getClass();

    // System.out.println("Parameters [0] " + parameters[0].toString());

    try {
      m = c.getMethod("tryPackage", parameters);
      m.invoke(obj1, arguments);
    }
    catch (IllegalAccessException ia) {
      System.out.println("Framework test IllegaleAccessException" +  ia);
    }
    catch (InvocationTargetException ita) {
      System.out.println("Framework test InvocationTargetException" +  ita);
      System.out.println("Framework test nested InvocationTargetException" +  ita.getTargetException() );
    }
    catch (NoSuchMethodException nme) {
      System.out.println("Framework test NoSuchMethodException " +  nme);
      nme.printStackTrace();
    }
    catch (Throwable thr) {
      System.out.println("Unexpected " +  thr);
      thr.printStackTrace();
    }
  }
  public synchronized void putEvent (String device, String method, Integer value) {
    // System.out.println("putEvent" + device + " " + method + " " + value);
    events.addElement(new devEvent(device, method, value));
  }

  class devEvent {
    String dev;
    String met;
    int val;

    public devEvent (String dev, String met , Integer val) {
      this.dev = dev;
      this.met = met;
      this.val = val.intValue();
    }

    public devEvent (String dev, String met , int val) {
      this.dev = dev;
      this.met = met;
      this.val = val;
    }

    public String getDevice() {
      return dev;
    }

    public String getMethod() {
      return met;
    }

    public int getValue() {
      return val;
    }

  }

  private boolean checkEvents(Object _out, Vector expevents, Vector events) {
    boolean state = true;
    if (events.size() != expevents.size()) {
      state = false;
      out.println("Real events");
      for (int i = 0; i< events.size() ; i++) {
	devEvent dee = (devEvent) events.elementAt(i);
	out.print("Bundle " + dee.getDevice());
	out.print(" Method " + dee.getMethod());
	out.println(" Value " + dee.getValue());
      }
      out.println("Expected events");
      for (int i = 0; i< expevents.size() ; i++) {
	devEvent dee = (devEvent) expevents.elementAt(i);
	out.print("Bundle " + dee.getDevice());
	out.print(" Method " + dee.getMethod());
	out.println(" Value " + dee.getValue());
      }
    }
    else {
      for (int i = 0; i< events.size() ; i++) {
	devEvent dee = (devEvent) events.elementAt(i);
	devEvent exp = (devEvent) expevents.elementAt(i);
	if (!(dee.getDevice().equals(exp.getDevice()) && dee.getMethod().equals(exp.getMethod()) && dee.getValue() == exp.getValue())) {
	  out.println("Event no = " + i);
	  if (!(dee.getDevice().equals(exp.getDevice()))) {
	    out.println ("Bundle is " + dee.getDevice() +  " should be " + exp.getDevice());
	  }
	  if (!(dee.getMethod().equals(exp.getMethod()))) {
	    out.println ("Method is " + dee.getMethod() +  " should be " + exp.getMethod());
	  }
	  if (!(dee.getValue() == exp.getValue())) {
	    out.println ("Value is " + dee.getValue() +  " should be " + exp.getValue());
	  }
	  state = false;
	}
      }
    }
    return state;
  }

  private String getStateString(int bundleState) {
    switch (bundleState) {
    case 0x01: return "UNINSTALLED";
    case 0x02: return "INSTALLED";
    case 0x04: return "RESOLVED";
    case 0x08: return "STARTING";
    case 0x10: return "STOPPING";
    case 0x20: return "ACTIVE";

    default: return "Unknow state";

    }
  }

  class FrameworkListener implements org.osgi.framework.FrameworkListener {
    FrameworkEvent fwe;
    public void frameworkEvent(FrameworkEvent evt) {
      this.fwe = evt;
      // System.out.println("FrameworkEvent: "+ evt.getType());
    }
    public FrameworkEvent getEvent() {
      return fwe;
    }
    public void clearEvent() {
      fwe = null;
    }
  }
  
  class ServiceListener implements org.osgi.framework.ServiceListener {
    ServiceEvent serve = null;
    public void serviceChanged(ServiceEvent evt) {
      this.serve = evt;
      // System.out.println("ServiceEvent: " + evt.getType());
    }
    public ServiceEvent getEvent() {
      return serve;
    }
    public void clearEvent() {
      serve = null;
    }
    
  }
  
  class BundleListener implements org.osgi.framework.BundleListener {
    BundleEvent bunEvent = null;
    
    public void bundleChanged (BundleEvent evt) {
      this.bunEvent = evt;
      // System.out.println("BundleEvent: "+ evt.getType());
    }
    public BundleEvent getEvent() {
      return bunEvent;
    }
    public void clearEvent() {
      bunEvent = null;
    }
  }

}
