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

package org.knopflerfish.bundle.permissionadmin_test;

import java.util.*;
import java.io.*;
import java.math.*;
import java.net.*;
import java.lang.reflect.*;
import java.security.*;

import org.osgi.framework.*;

import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.*;

import junit.framework.*;

public class PermissionAdminTestSuite extends TestSuite  {
  BundleContext bc;

  // Permission test bundles
  Bundle buP;

 // Permission test bundles
  Bundle buU;
  Bundle buV;
  Bundle buW;
  Bundle buW1;
  Bundle buX;
  Bundle buY;
  Bundle buZ;



  // the three event listeners
  FrameworkListener fListen;
  BundleListener    bListen;
  ServiceListener   sListen;

  Properties props = System.getProperties();
  String     lineseparator = props.getProperty("line.separator");

  String     test_url_base;

  Vector     events    = new Vector();  // vector for events from test bundles
  Vector     expevents = new Vector();	// comparision vector

  PrintStream out = System.out;

  public PermissionAdminTestSuite (BundleContext bc) {
    super ("PermissionAdminTestSuite");

    this.bc = bc;

    test_url_base = "bundle://" + bc.getBundle().getBundleId() + "/";

    addTest(new Setup());
    addTest(new Frame090a());
    addTest(new Frame095a());
    addTest(new Frame0100a());
    addTest(new Frame0105a());
    addTest(new Frame0205a());
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

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
	buP,
	buU ,
	buV ,
	buW ,
	buW1 ,
	buX ,
	buY ,
	buZ ,
      };
      for(int i = 0; i < bundles.length; i++) {
	try {  bundles[i].uninstall();  } 
	catch (Exception ignored) { }      
      }

      // Permission test bundles
      buP = null;
      buU = null;
      buV = null;
      buW = null;
      buW1 = null;
      buX = null;
      buY = null;
      buZ = null;


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

  // Also install all possible listeners
  class Setup extends FWTestCase {
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

  // 18. Install and start testbundle U, to check that it is unable to register its own service  
  //     to see that the Permissions work.
  //     The testbundle U has no permission to register its service and should 
  //     not be registered

  public final static String USAGE_FRAME090A = "";
  public final static String [] HELP_FRAME090A =  {
    "Install and start bundleU_test, to check that it is unable to register its own service",
    "to see that the Permissions work.",
    "The testbundle U has no permission to register its service and should",
    "not be registered."
  };

  class Frame090a extends FWTestCase {
    public void runTest() throws Throwable {
      buU = null;
      boolean teststatus = true;
      clearEvents();
      try {
	buU = Util.installBundle(bc, "bundleU_test-1.0.0.jar");
	buU.start();
	teststatus = true;
      }
      catch (BundleException bexcA) {
	fail("framework test bundle "+ bexcA +" :FRAME090A:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("framework test bundle "+ secA +" :FRAME090A:FAIL");
	teststatus = false;
      }
      
      // Check that a service reference does not exist
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleU_test.BundleU");
      if (sr1 != null) {
	fail("framework test bundle, found unexpected service from test bundle U found :FRAME090A:FAIL");
	teststatus = false;
      }
      
      // check the listeners for events, expect none 
      boolean lStat = checkListenerEvents(out, false , 0, true , BundleEvent.STARTED, false, ServiceEvent.REGISTERED, buU, sr1);
      
      // get the permissions of bundle buU and check if they are as expected
      ServicePermission get = new ServicePermission("*", ServicePermission.GET);
      ServicePermission register = new ServicePermission("*", ServicePermission.REGISTER);
      
      boolean p1 = buU.hasPermission(get);
      boolean p2 = buU.hasPermission(register);
      // out.println("framework test bundle : p1, p2" + p1 + ",  "+  p2);
      
      if (!(p1 == true && p2 == false)) {
	teststatus = false;
	out.println("framework test bundle permissions of test bundleU not as expected");
	fail("framework test bundle: GET is " + p1 +" should be true, REGISTER is " + p2 + ",  should be false");
      }
      
      if (teststatus == true && buU.getState() == Bundle.ACTIVE && lStat == true) {
	out.println("### framework test bundle :FRAME090A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME090A:FAIL");
      }
    }
  }

  // 19. Install and start testbundle V, to check that it is:
  //     able to register its own service
  //     unable to get the log service,
  //     able to get the test service
  //    
  public final static String USAGE_FRAME095A = "";
  public final static String [] HELP_FRAME095A =  {
    "Install and start bundleV_test, to check that it is:",
    "able to register its own service",
    "unable to get the log service",
    "able to get the test service"
  };

  class Frame095a extends FWTestCase {
    public void runTest() throws Throwable {
      buV = null;
      boolean teststatus = true;
      try {
	buV = Util.installBundle(bc, "bundleV_test-1.0.0.jar");
	buV.start();
	teststatus = true;
      }
      catch (BundleException bexcA) {
	bexcA.printStackTrace();
	fail("framework test bundle "+ bexcA +" :FRAME095A:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	secA.printStackTrace();
	fail("framework test bundle "+ secA +" :FRAME095A:FAIL");
	teststatus = false;
      }
      
      // Check that a service reference does exist
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleV_test.BundleV");
      if (sr1 == null) {
	fail("framework test bundle, did not found expected service from test bundle V found :FRAME095:FAIL");
	teststatus = false;
      }
      
      // check the listeners for events, expect registration
      boolean lStat = checkListenerEvents(out, false , 0, true , BundleEvent.STARTED, true, ServiceEvent.REGISTERED, buV, sr1);
      
      // get the permissions of bundle buV and check if they are as expected
      ServicePermission get = new ServicePermission("org.knopflerfish.service.framework_test.FrameworkTest", ServicePermission.GET);
      ServicePermission register = new ServicePermission("*", ServicePermission.REGISTER);
      
      boolean p1 = buV.hasPermission(get);
      boolean p2 = buV.hasPermission(register);
      // out.println("framework test bundle : p1, p2" + p1 + ",  "+  p2);
      if (!(get.getActions().equals("get") && register.getActions().equals("register"))) {
	teststatus = false;
	out.println("framework test bundle get getActions is: " + get.getActions() + " should be get");
	fail("framework test bundle register getActions is: " + register.getActions() + " should be register");
      }
      
      // now give the bundleV a servicereference via reflection
      ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.bundleV_test.BundleV");
      Object srl = bc.getServiceReference("org.osgi.service.log.LogService");
      
      Method m;
      Class c, parameters[];
      
      Object obj1 = bc.getService(sr);
      // out.println("servref  = "+ sr);
      // out.println("object = "+ obj1);
      
      Object[] arguments = new Object[1];
      arguments[0] = srl;
      
      c = obj1.getClass();
      parameters = new Class[1];
      //
      Method [] mxx = c.getDeclaredMethods();
      for (int y=0;y <mxx.length; y++) {
	// out.println("Methods in obj1 " + mxx[y].getName());
	if (mxx[y].getName().endsWith("tryService")) {
	  // out.println("Methods in obj1 " + mxx[y].getName());
	  Class [] cxx = mxx[y].getParameterTypes();
	  for (int z=0; z < cxx.length; z++) {
	    // out.println("Parameter Classes in obj1 " + cxx[z].getName());
	    if (cxx[z].getName().endsWith("org.osgi.framework.ServiceReference")) {
	      // out.println("Selected Parameter Classes in obj1 " + cxx[z].toString());
	      parameters[0] = cxx[z]; 
	    }
	  }
	}
      }
      //
      // parameters[0] = arguments[0].getClass();
      
      // out.println("Parameters [0] " + parameters[0].toString());
      
      try {
	m = c.getMethod("tryService", parameters);
	m.invoke(obj1, arguments);
      }
      catch (IllegalAccessException ia) {
	out.println("Framework test IllegaleAccessException" +  ia);
      }
      catch (InvocationTargetException ita) {
	out.println("Framework test InvocationTargetException" +  ita);
	out.println("Framework test nested InvocationTargetException" +  ita.getTargetException() );
      }
      catch (NoSuchMethodException nme) {
	out.println("Framework test NoSuchMethodException " +  nme);
	nme.printStackTrace();
      }
      catch (Throwable thr) {
	out.println("Unexpected " +  thr);
      }
      
      if (!(p1 == true && p2 == true)) {
	teststatus = false;
	out.println("framework test bundle permissions of test bundleV not as expected");
	fail("framework test bundle: GET is " + p1 +" should be true, REGISTER is " + p2 + ",  should be true");
      }
      /* 
	 for (int i = 0; i< events.size() ; i++) {
	 devEvent dee = (devEvent) events.elementAt(i);
	 out.print("Bundle " + dee.getDevice());
	 out.print(" Method " + dee.getMethod());
	 out.println(" Value " + dee.getValue());
	 }
      */
      
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleV_test.BundV", "constructor, Service reference == null", 0));
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleV_test.BundV", "tryService SecurityException", 1));
      
      // Now check that the events from bundleV_test match the expected sequence of events
      // their metods called the correct number of times and in the right sequence
      
      if (events.size() != expevents.size()) {
	teststatus = false;
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
	    teststatus = false;
	  }
	}
	
      }
      
      events.removeAllElements();
      expevents.removeAllElements();
      
      if (teststatus == true && buV.getState() == Bundle.ACTIVE && lStat == true) {
	out.println("### framework test bundle :FRAME095A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME095A:FAIL");
      }
    }
  }

  // 20. Install and start testbundle W, and order it to install test bundles
  //     bundleX_test, which should be ok 
  //     bundleY_test, which should fail 
  //
  public final static String USAGE_FRAME100A = "";
  public final static String [] HELP_FRAME100A =  {
    "Install and start bundleW_test, to check that it is:",
    "able to install bundleX_test, which should be ok",
    "able to install bundleY_test, which should be fail"
  };

  class Frame0100a extends FWTestCase {
    public void runTest() throws Throwable {
      buW = null;
      boolean teststatus = true;
      try {
	buW = Util.installBundle(bc, "bundleW_test-1.0.0.jar");
	buW.start();
	teststatus = true;
      }
      catch (BundleException bexcA) {
	fail("framework test bundle "+ bexcA +" :FRAME100A:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("framework test bundle "+ secA +" :FRAME100A:FAIL");
	teststatus = false;
      }
      
      // Check that a service reference does exist
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleW_test.BundleW");
      if (sr1 == null) {
	fail("framework test bundle, did not found expected service from test bundle W found :FRAME100A:FAIL");
	teststatus = false;
      }
      
      // check the listeners for events, expect registration
      boolean lStat = checkListenerEvents(out, false , 0, true , BundleEvent.STARTED, true, ServiceEvent.REGISTERED, buW, sr1);
      
      // now give the bundleW a jar file to load, via reflection
      ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.bundleW_test.BundleW");
      bundleLoad (out, sr, "bundleX_test");
      bundleLoad (out, sr, "bundleY_test");
      bundleLoad (out, sr, "bundleZ_test");
      /* 
      // list all events 
      for (int i = 0; i< events.size() ; i++) {
      devEvent dee = (devEvent) events.elementAt(i);
      out.print("Bundle " + dee.getDevice());
      out.print(" Method " + dee.getMethod());
      out.println(" Value " + dee.getValue());
      }
      */
      
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleX_test.BundX", "constructor, Service reference != null", 0));
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleW_test.BundW", "tryPackage succeded with bundle bundleX_test", 3));
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleW_test.BundW", "tryPackage got BundleException with bundle bundleY_test: Bundle.start: Failed, missing export permission for package(s): org.knopflerfish.service.bundleY_test", 3));
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleW_test.BundW", "tryPackage got BundleException with bundle bundleZ_test: Bundle.start: Failed, missing export permission for package(s): org.knopflerfish.service.bundleZ_test", 3));
      
      
      // Now check that the events from bundleV_test match the expected sequence of events
      // their metods called the correct number of times and in the right sequence
      
      if (events.size() != expevents.size()) {
	teststatus = false;
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
	    teststatus = false;
	  }
	}
      }
      
      events.removeAllElements();
      expevents.removeAllElements();
      
      if (teststatus == true && buW.getState() == Bundle.ACTIVE && lStat == true) {
	out.println("### framework test bundle :FRAME100A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME100A:FAIL");
      }
    }
  }

  // 21. Install testbundle W1, which should be possible to install and start
  //     However, when it is ordered to install and start other bundles
  //     that should cause SecurityExceptions in W1
  //

  public final static String USAGE_FRAME105A = "";
  public final static String [] HELP_FRAME105A =  {
    "Install and start bundleW1_test, to check that it is:",
    "possible to install and start.",
    "However when ordered to install and start other bundles",
    "that should cause SecurityExceptions in W1"
  };

  class Frame0105a extends FWTestCase {
    public void runTest() throws Throwable {
      buW1 = null;
      boolean teststatus = true;
      clearEvents();
      try {
	buW1 = Util.installBundle(bc, "bundleW1_test-1.0.0.jar");
	teststatus = true;
      }
      catch (BundleException bexcA) {
	fail("framework test bundleW1 install "+ bexcA +" :FRAME105A:FAIL");
	fail("framework test bundleW1 install "+ bexcA.getNestedException() +" :FRAME105A:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("framework test bundleW1  install "+ secA +" :FRAME105A:FAIL");
	teststatus = false;
      }
      
      // check the listeners for events, expect registration
      boolean lStat = checkListenerEvents(out, false , 0, true , BundleEvent.INSTALLED, false, ServiceEvent.REGISTERED, buW1, null);
      
      try {
	buW1.start();
      }
      catch (BundleException bexcA) {
	fail("framework test bundleW1 start "+ bexcA +" :FRAME105A:FAIL");
	fail("framework test bundleW1 start "+ bexcA.getNestedException() +" :FRAME105A:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("framework test bundleW1 start "+ secA +" :FRAME105A:FAIL");
	teststatus = false;
      }
      
      // Check that a service reference does exist
      ServiceReference sr1 = bc.getServiceReference("org.knopflerfish.service.bundleW1_test.BundleW1");
      if (sr1 == null) {
	fail("framework test bundleW1, unexpected service from test bundle W1 found :FRAME105A:FAIL");
	teststatus = false;
      }
      boolean lStat2 = checkListenerEvents(out, false , 0, true , BundleEvent.STARTED, true, ServiceEvent.REGISTERED, buW1, sr1);
      
      // now give the bundleW1 jar files to load, via reflection
      ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.bundleW1_test.BundleW1");
      if (sr != null ) {
	bundleLoad (out, sr, "bundleX_test");
	bundleLoad (out, sr, "bundleY_test");
	bundleLoad (out, sr, "bundleZ_test");
      }
      
      
      /* 
      // list all events
      for (int i = 0; i< events.size() ; i++) {
      devEvent dee = (devEvent) events.elementAt(i);
      out.print("Bundle " + dee.getDevice());
      out.print(" Method " + dee.getMethod());
      out.println(" Value " + dee.getValue());
      }
      */
      
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleW1_test.BundW1", "tryPackage got SecurityException with bundle bundleX_test", 3));
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleW1_test.BundW1", "tryPackage got SecurityException with bundle bundleY_test", 3));
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleW1_test.BundW1", "tryPackage got SecurityException with bundle bundleZ_test", 3));
      
      
      // Now check that the events match the expected sequence of events
      // their metods called the correct number of times and in the right sequence
      
      if (events.size() != expevents.size()) {
	teststatus = false;
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
	    teststatus = false;
	  }
	}
      }
      
      events.removeAllElements();
      expevents.removeAllElements();
      
      if (teststatus == true && buW1.getState() == Bundle.ACTIVE && lStat == true && lStat2 == true) {
	out.println("### framework test bundle :FRAME105A:PASS");
      }
      else {
	fail("### framework test bundle :FRAME105A:FAIL");
      }
    }
  }
  
  // 205A. Simple test of the PermissionAdmin interface
  
  public final static String USAGE_FRAME205A = "";
  public final static String [] HELP_FRAME205A =  {
    "Try to get a permission admin service",
    "do simple introspection checks of this test bundle"
  };
  
  class Frame0205a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String permissionServiceName = "org.osgi.service.permissionadmin.PermissionAdmin";
      String expectedExportedPackageName = "org.knopflerfish.service.framework_test";
      String expectedVersion = "1.0";
      PermissionAdmin permissionService = null;
      Bundle fw_test = bc.getBundle();    // this bundle
      
      // Try to create an instace of class PermissionInfo and test its methods
      
      String pType = "org.knopflerfish.bundle.framework_test.PermissionTest";
      String pName = "ppp";
      String pAction = "A,B";
      String pEncoding = "(" + pType + " \"" + pName + "\"" + " \"" + pAction + "\")";
      
      try {
	PermissionInfo pinfo1 = new PermissionInfo(pType, pName, pAction);
	PermissionInfo pinfo2 = new PermissionInfo(pEncoding);
	// Do the object behave as expected ?
	if (!pinfo1.equals(pinfo2)) {
	  fail("equals method or constructor failed" + " FRAME205A:FAIL");
	  teststatus  = false;
	}
	
	String actions = pinfo1.getActions();
	if (!actions.equals(pAction)) {
	  fail("getActions method got: " + actions + " expected " + pAction +" FRAME205A:FAIL");
	  teststatus  = false;
	}
	
	String encoding = pinfo1.getEncoded();
	if (!encoding.equals(pEncoding)) {
	  fail("getEncoded method got: " + encoding + " expected " + pEncoding +" FRAME205A:FAIL");
	  teststatus  = false;
	}
	
	String name = pinfo1.getName();
	if (!name.equals(pName)) {
	  fail("getName method got: " + name + " expected " + pName +" FRAME205A:FAIL");
	  teststatus  = false;
	}
	
	String type = pinfo1.getType();
	if (!type.equals(pType)) {
	  out.println("getType method got: " + type);
	  out.println("getType method expected: " + pType);
	  fail("getType method got: " + type + " expected " + pType +" FRAME205A:FAIL");
	  teststatus  = false;
	}
	
	int hcode1 = pinfo1.hashCode();
	int hcode2 = pinfo1.hashCode();
	
	if (hcode1 != hcode2) {
	  fail("hashCode method got: " + String.valueOf(hcode1) + " expected " + String.valueOf(hcode2) +" FRAME205A:FAIL");
	  teststatus  = false;
	}
	
	String str = pinfo1.toString();
	if (str == null) {
	  fail("toString method returned null, FRAME205A:FAIL");
	  teststatus  = false;
	}
	
      }
      
      catch (Throwable tt) {
	fail("Failed to create permission class instance" + " ,FRAME205A:FAIL");
	teststatus  = false;
      }
      
      // Get a service
      
      
      try {
	permissionService = (PermissionAdmin) bc.getService(bc.getServiceReference(permissionServiceName));
	if (permissionService == null) {
	  teststatus  = false;
	  fail("Got null service " + permissionServiceName + " in FRAME205A:FAIL");
	} else {
	  // Examine the default permissions 
	  PermissionInfo [] pinfo = permissionService.getDefaultPermissions();
	  if (pinfo == null) {
	    fail("No default permission set, got null, FRAME205A:FAIL");
	  } else {
	    for (int i = 0 ; i < pinfo.length; i++) {
	      printPermission(out, pinfo[i]);
	    }
	  }
	  // out.println("Got service " + permissionServiceName + " in FRAME205A");
	}
	
      }
      catch (Throwable tt) {
	fail("Failed to get " + permissionServiceName + " service, exception " + tt + " ,FRAME205A:FAIL");
	teststatus  = false;
      }
      
      
      // Print out bundles and Get the location of all bundles with any permission set
      
      /* 
	 String blocs [] =  permissionService.getLocations();
	 if (blocs != null) {
	 for (int i = 0 ; i < blocs.length; i++) {
	 out.println("Bundle " + blocs[i] );
	 PermissionInfo pinfo [] = permissionService.getPermissions(blocs[i]);
	 if (pinfo != null) {
	 for (int j = 0 ; j < pinfo.length; j++) {
	 printPermissionShort(out, pinfo[j]);
	 }
	 }
	 }
	 }
      */
      
      // create a test file for later use by permission tests
      
      String filename = "/tmp/testfile4";
      byte [] testdata = {1,2,3,4,5};
      File testFile = new File (filename);
      if (testFile != null ) {
	try {
	  FileOutputStream fout = new FileOutputStream (testFile);
	  fout.write(testdata);
	  fout.close();
	}
	catch (IOException ioe) {
	  teststatus = false;
	  fail("framework test bundle, I/O error on write " + ioe + " in FRAME205A:FAIL");
	}
      }
      
      
      // set up permissions (to not yet loaded bundle bundleP_test) allow it to succeed
      // in getting file permission
      
      PermissionInfo pi1 = new PermissionInfo("java.util.PropertyPermission", "org.knopflerfish.*", "read");
      PermissionInfo pi2 = new PermissionInfo("java.util.PropertyPermission", "org.osgi.*", "read");
      PermissionInfo pi3 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "import");
      PermissionInfo pi4 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.knopflerfish.service.bundleP_test", "import,export");
      PermissionInfo pi5 = new PermissionInfo("org.osgi.framework.ServicePermission", "*", "get,register");
      PermissionInfo pi6 = new PermissionInfo("java.io.FilePermission", filename, "read");
      
      PermissionInfo pa[] = new PermissionInfo[] {pi1, pi2, pi3, pi4, pi5 ,pi6};
      
      try {
	permissionService.setPermissions(test_url_base+"bundleP_test-1.0.0.jar", pa);
      }
      
      catch (SecurityException sex) {
	fail("Failed to set permissions for bundleP_test, got exception " + sex + " ,FRAME205A:FAIL");
	teststatus  = false;
      }
      
      events.removeAllElements();
      expevents.removeAllElements();
      
      // register and start bundleP_test
      buP = null;
      out.println(test_url_base+"bundleP_test-1.0.0.jar");
      
      try {
	buP = bc.installBundle (test_url_base+"bundleP_test-1.0.0.jar");
	buP.start();
      }
      catch (BundleException bexcA) {
	fail("Start of bundleP_test exception: "+ bexcA +" :FRAME0205A:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("Start of bundleP_test exception: "+ secA +" :FRAME0205A:FAIL");
	teststatus = false;
      }
      
      // check that the events from bundleP_test match the expected sequence of events
      // their metods called the correct number of times and in the right sequence
      
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleP_test.BundP", "constructor, bundleP_test Service reference != null", 0));
      expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleP_test.BundP", "constructor, bundleP_test File reference: true", 1));
      boolean event = checkEvents(out, expevents, events);
      if (event != true) {
	fail("Mismatch between expected and real events FRAME205A:FAIL");
	teststatus = false;
      }
      
      events.removeAllElements();
      expevents.removeAllElements();
      
      // stop the bundle
      try {
	buP.stop();
      }
      catch (BundleException bexcA) {
	fail("Stop of bundleP_test exception: "+ bexcA +" :FRAME0205A:FAIL");
	teststatus = false;
      }
      catch (SecurityException secA) {
	fail("Stop of bundleP_test exception: "+ secA +" :FRAME0205A:FAIL");
	teststatus = false;
      }
      
      // change its permisssions, so that it should not succeed to register itself
      pi1 = new PermissionInfo("java.util.PropertyPermission", "org.knopflerfish.*", "read");
      pi2 = new PermissionInfo("java.util.PropertyPermission", "org.osgi.*", "read");
      pi3 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.osgi.framework", "import");
      pi4 = new PermissionInfo("org.osgi.framework.PackagePermission", "org.knopflerfish.service.bundleP_test", "import,export");
      pi5 = new PermissionInfo("org.osgi.framework.ServicePermission", "*", "get, register");
      pi6 = new PermissionInfo("java.io.FilePermission", "", "");
      
      pa = new PermissionInfo[] {pi1, pi2, pi3, pi4, pi5, pi6 };
      
      // expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleP_test.BundP", "constructor, Service reference == null", 0));
      // expevents.addElement(new devEvent("org.knopflerfish.bundle.bundleP_test.BundP", "tryService SecurityException", 1));
      
      try {
	permissionService.setPermissions(test_url_base+"bundleP_test-1.0.0.jar", pa);
      }
      catch (SecurityException sex) {
	fail("Failed to set permissions for bundleP_test, got exception " + sex + " ,FRAME205A:FAIL");
	teststatus  = false;
      }
      
      
      // start the bundle again, expect a nested bundle exception
      // the nested exception should be a file permission exception
      
      try {
	buP.start();
	teststatus = false;
      }
      catch (BundleException bexcA) {
	Throwable tex = bexcA.getNestedException();
	if (tex != null) {
	  String xp = tex.getClass().getName();
	  String lookFor = "access denied (java.io.FilePermission";
	  String fromException = tex.getMessage();
	  // out.println("String from exception " + fromException);
	  // out.println("String to look for " + lookFor);
	  // out.println("Class name " + xp);
	  // out.println("Index of searched string in fromException.indexOf(lookFor) " + fromException.indexOf(lookFor) );
	  
	  if (fromException.toLowerCase().indexOf(lookFor.toLowerCase()) != 0) {
	    fail("Expected string " + lookFor + " got " +  fromException + " :FRAME0205A:FAIL");
	    teststatus = false;
	  }
	  if (!"java.security.AccessControlException".equals(xp)) {
	    fail("Expected exception " + "java.security.AccessControlException" + " got " + xp + " :FRAME0205A:FAIL");
	    teststatus = false;
	  }
	} else {
	  fail("No expected nested exception found :FRAME0205A:FAIL");
	  teststatus = false;
	}
      }
      catch (SecurityException secA) {
	fail("Unexpected exception" + secA + " :FRAME0205A:FAIL");
	teststatus = false;
      }
      
      // check the resulting events
      // Remove testfile4
      
      testFile.delete();
      
      if (teststatus == true) {
	out.println("### framework test bundle :FRAME205A:PASS");
      } else {
	fail("### framework test bundle :FRAME205A:FAIL");
      }
    }
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


  // So that other bundles in the test may get the base url

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
