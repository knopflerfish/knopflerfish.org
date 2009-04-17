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

package org.knopflerfish.bundle.http_test;

import org.knopflerfish.service.http_test.*;

import java.io.*;
import java.util.*;
import org.osgi.framework.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.http.*;
import org.osgi.service.http.HttpService;

import javax.servlet.*;
import javax.servlet.http.*;

import junit.framework.*;

public class HttpTestSuite extends TestSuite  {
  BundleContext bc;
  Bundle bu;
  String HttpServiceClass = "org.osgi.service.http.HttpService";
  
  String http     = "http://";
  String hostname = "localhost";
  String port;
  Object obj;

  ServiceReference httpSR = null;

  PrintStream out = System.out;

  static HttpService httpService;
  /* common resource/servlet references */

  
  public HttpTestSuite (BundleContext bc) {
    super ("HttpTestSuite");

    try {
      this.bc = bc;
      bu = bc.getBundle();
      
      
      /*
	This bundle is used to control the functionality of the http service API.
	The test script calls these console commands and may use their printouts.
	
      */
      
      addTest(new Setup());
      addTest(new Http005a());
      addTest(new Http006a());
      addTest(new Http010a());
      addTest(new Http015a());
      addTest(new Http020a());
      addTest(new Http025a());
      addTest(new Http030a());
      addTest(new Http035a());
      addTest(new Http040a());
      addTest(new Http045a());
      addTest(new Http050a());
      addTest(new Http055a());
      addTest(new Http060a());
      addTest(new Http065a());
      addTest(new Http085a());
      addTest(new Http090a());
      addTest(new Http105a());
      addTest(new Cleanup());
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
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

  // Also install all possible listeners
  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
      httpSR = bc.getServiceReference(HttpServiceClass);
      assertNotNull("Setup: no http service ref available", httpSR);
      httpService = (HttpService) bc.getService(httpSR);
      
      assertNotNull("Setup: no http service object available", httpService);
      System.out.println("HttpService.class.getName():" + HttpService.class.getName());
      
      // Now let's get on with the port...
      obj = httpSR.getProperty("port");
      if (obj != null) {
	port = obj.toString();
      } else {
	System.out.println("Ooops - failed to find the port property!!!");
	
	// Dump the properties as known by the http service
	String[] keys = httpSR.getPropertyKeys();
	System.out.println("--- Propery keys ---");
	for (int i=0; i<keys.length; i++) {
	  System.out.println(i + ": " + keys[i] + " --> "+ httpSR.getProperty(keys[i]));
	}

      }
    }
  }

  public final static String [] HELP_HTTP006A = {
    "Test http server properties",
  };
  
  class Http006a extends FWTestCase {
    public void runTest() throws Throwable {
      Object obj = httpSR.getProperty("port");

      assertNotNull("No 'port' property set on http server", obj);
      
      assertTrue("Port property must be integer", obj instanceof Integer);
    }
  }

  class Http005a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      
      HttpTestContext hc1 = new HttpTestContext(resourceName);
      try {
	httpService.registerResources(alias, internalName, hc1);
      }
      catch (NamespaceException ne) {
	teststatus = false;
	fail("HTTP Exception " + ne);
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }

  // 2. Unregister the test page from the server
  public final static String USAGE_HTTP010A = "";
  public final static String [] HELP_HTTP010A = {
    "Unregister the test page registerd in http005a,  respond with its URI"
  };

  class Http010a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index.html";
      try {
	httpService.unregister(alias);
      }
      catch (IllegalArgumentException ne) {
	fail("HTTP Exception at unregistering  " + ne);
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }

  // 3. Register a test servlet on the server, respond with its URI

  public final static String USAGE_HTTP015A = "";
  public final static String [] HELP_HTTP015A = {
    "Register a test servlet on the server, respond with its URI"
  };

  class Http015a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index2.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();
      
      HttpTestContext hc2 = new HttpTestContext(resourceName);
      
      Servlet serv1 = new HttpTestServlet();
      try {
	httpService.registerServlet(alias, serv1, d1, hc2);
      }
      catch (NamespaceException ne) {
	fail("HTTP Exception " + ne);
      }
      catch (ServletException se) {
	fail("HTTP Exception " + se);
      }
      String uri = "URI: " + ":"+port+alias;  // create the URI
      out.println (uri);
    }
  }
  
  // 4. Unregister the test servlet from the server

  public final static String USAGE_HTTP020A = "";
  public final static String [] HELP_HTTP020A = {
    "Unregister the test servlet registerd in http015a,  respond with its URI"
  };

  class Http020a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index2.html";
      try {
	httpService.unregister(alias);
      }
      catch (IllegalArgumentException ne) {
	fail("HTTP Exception at unregistering  " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the URI
      out.println (uri);
    }
  }

  // 5. Try to register a resource with a few cases of broken names
  //    and broken aliases.
  //    This should generate exceptions, which are taken as an indication 
  //    that the test worked 

  public final static String USAGE_HTTP025A = "";
  public final static String [] HELP_HTTP025A = {
    "Register a resource with a some broken names and aliases.",
    "This should generate exceptions."
  };

  class Http025a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean except1 = true;
      boolean except2 = true;
      boolean except3 = true;
      String alias = "/index.html";
      String ba1 = "index.html";
      String ba2 = "index.html/";
      String internalName = "n1";
      String bin1 = "n1/";
      
      String resourceName = "/http_test/index.html";
      HttpTestContext hc1 = new HttpTestContext(resourceName);
      
      try {
	httpService.registerResources(ba1, internalName, hc1);
	except1 = false;
      }
      catch (NamespaceException ne) {
	fail("HTTP NameSpaceException " + ne + "in HTTP025A");
	teststatus = false;
      }
      catch (IllegalArgumentException ne) {
	except1 = true;
      }
      
      if (except1 == false) {
	teststatus = false;
	fail("HTTP Missing IllgalArgumentException in HTTP025A");
      }
      
      try {
	httpService.registerResources(ba2, internalName, hc1);
	except2 = false;
      }
      catch (NamespaceException ne) {
	fail("HTTP NameSpaceException " + ne + "in HTTP025A");
	teststatus = false;
      }
      catch (IllegalArgumentException ne) {
	except2 = true;
      }
      
      if (except2 == false) {
	teststatus = false;
	fail("HTTP Missing IllgalArgumentException in HTTP025A");
      }
      
      try {
	httpService.registerResources(alias, bin1, hc1);
	except3 = false;
      }
      catch (NamespaceException ne) {
	fail("HTTP NameSpaceException " + ne + "in HTTP025A");
	teststatus = false;
      }
      catch (IllegalArgumentException ne) {
	except3 = true;
      }
      
      if (except3 == false) {
	teststatus = false;
	fail("HTTP Missing IllgalArgumentException in HTTP025A");
      }
      
      if (teststatus == true ) {
	out.println("### Log test bundle :HTTP025A: PASS");
      }
      else {
	fail("### Log test bundle :HTTP025A: FAIL");
      }
    }
  }
  
  // 6. Register a test page on the server

  public final static String USAGE_HTTP030A = "";
  public final static String [] HELP_HTTP030A = {
    "Register a test page on the server,",
    "Respond with its URI"
  };

  class Http030a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      
      HttpTestContext hc1 = new HttpTestContext(resourceName);
      try {
	httpService.registerResources(alias, internalName, hc1);
      }
      catch (NamespaceException ne) {
	fail("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }

  // 7. Register the same test page on the server, expect an exception
  public final static String USAGE_HTTP035A = "";
  public final static String [] HELP_HTTP035A = {
    "Register the same page as in http030a, expect an exception."
  };
  
  class Http035a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean exception = false;
      String alias = "/index.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      
      HttpTestContext hc1 = new HttpTestContext(resourceName);
      try {
	httpService.registerResources(alias, internalName, hc1);
	exception = false;
	teststatus = false;
      }
      catch (NamespaceException ne) {
	exception = true;
      }
      if (exception == false) {
	teststatus = false;
	fail("HTTP Missing NameSpaceException in HTTP035A");
      }
      
      if (teststatus == true ) {
	out.println("### Log test bundle :HTTP035A: PASS");
      }
      else {
	fail("### Log test bundle :HTTP035A: FAIL");
      }
    }
  }

  // 8. Try to register a servlet with a few cases of broken names
  //    and broken aliases.
  //    This should generate exceptions, which are taken as an indication
  //    that the test worked

  public final static String USAGE_HTTP040A = "";
  public final static String [] HELP_HTTP040A = {
    "Register a servlet with a some broken names and aliases.",
    "This should generate exceptions."
  };

  class Http040a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean except1 = true;
      boolean except2 = true;
      boolean except3 = true;
      String ba1 = "index.html";
      String ba2 = "index.html/";
      Dictionary d1 = new Hashtable();
      
      String resourceName = "/http_test/index.html";
      HttpTestContext hc1 = new HttpTestContext(resourceName);
      Servlet serv1 = new HttpTestServlet();
      
      try {
	httpService.registerServlet(ba1, serv1, d1, hc1);
	except1 = false;
	teststatus = false;
      }
      catch (NamespaceException ne) {
	out.println("HTTP NameSpaceException " + ne + "in HTTP040A");
	except1 = false;
      }
      catch (ServletException se) {
	out.println("HTTP ServletException " + se + "in HTTP040A");
	except1 = false;
      }
      catch (IllegalArgumentException ne) {
	except1 = true;
      }
      if (except1 == false) {
	teststatus = false;
	fail("HTTP Missing IllegalArgumentException in HTTP040A");
      }
      
      try {
	httpService.registerServlet(ba2, serv1, d1, hc1);
	except2 = false;
	teststatus = false;
      }
      catch (NamespaceException ne) {
	out.println("HTTP NameSpaceException " + ne + "in HTTP040A");
	except2 = false;
      }
      catch (ServletException se) {
	out.println("HTTP ServletException " + se + "in HTTP040A");
	except2 = false;
      }
      catch (IllegalArgumentException ne) {
	except2 = true;
      }
      if (except2 == false) {
	teststatus = false;
	out.println("HTTP Missing IllegalArgumentException in HTTP040A");
      }
      
      if (teststatus == true ) {
	out.println("### Log test bundle :HTTP040A: PASS");
      }
      else {
	fail("### Log test bundle :HTTP040A: FAIL");
      }
    }
  }
  
  // 9. Register a test servlet on the server
  public final static String USAGE_HTTP045A = "";
  public final static String [] HELP_HTTP045A = {
    "Register a servlet on the server.",
    "Respond with its URI."
  };

  class Http045a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index2.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();
      
      HttpTestContext hc2 = new HttpTestContext(resourceName);
      
      Servlet serv1 = new HttpTestServlet();
      try {
	httpService.registerServlet(alias, serv1, d1, hc2);
      }
      catch (NamespaceException ne) {
	out.println("HTTP NameSpaceException " + ne + "in HTTP045A");
	teststatus = false;
      }
      catch (ServletException se) {
	out.println("HTTP ServletException " + se + "in HTTP045A");
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }
    
    // 10. Register the same test servlet on the server and see that 
    //    a namespace exception is thrown.
    
  public final static String USAGE_HTTP050A = "";
  public final static String [] HELP_HTTP050A = {
    "Register the same servlet as in http045..",
    "This should generate a namespace exception."
  };

  class Http050a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean except1 = true;
      String alias = "/index2.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();
      
      HttpTestContext hc2 = new HttpTestContext(resourceName);
      
      Servlet serv1 = new HttpTestServlet();
      try {
	httpService.registerServlet(alias, serv1, d1, hc2);
	teststatus = false;
	except1 = false;
      }
      catch (NamespaceException ne) {
	except1 = true;
      }
      catch (ServletException se) {
	out.println("HTTP ServletException " + se + "in HTTP050A");
	teststatus = false;
      }
      if (except1 == false) {
	out.println("HTTP Missing NamespaceIllegalArgumentException in HTTP050A");
	teststatus = false;
      }
      
      if (teststatus == true ) {
	out.println("### Log test bundle :HTTP050A: PASS");
      }
      else {
	out.println("### Log test bundle :HTTP050A: FAIL");
      }
    }
  }

  // 11. Register a new test servlet with permission that are false
  public final static String USAGE_HTTP055A = "";
  public final static String [] HELP_HTTP055A = {
    "Register a servlet with persmission that are false."
  };

  class Http055a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index3.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();
      
      HttpTestContext hc3 = new HttpTestContext(resourceName);
      hc3.setSecurity(false);
      
      Servlet serv1 = new HttpTestServlet();
      try {
	httpService.registerServlet(alias, serv1, d1, hc3);
      }
      catch (NamespaceException ne) {
	out.println("HTTP NameSpaceException " + ne + "in HTTP055A");
	teststatus = false;
      }
      catch (ServletException se) {
	out.println("HTTP ServletException " + se + "in HTTP055A");
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }

  // 12. Register two resources that partly shadow each other
  public final static String USAGE_HTTP060A = "";
  public final static String [] HELP_HTTP060A = {
    "Register two resource that partly shadow each other."
  };

  class Http060a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias3 = "/A/index.html";
      String resourceName3 = "/http_test/A/index.html";
      String internalName3 = "nameA";
      
      HttpTestContext hc1 = new HttpTestContext(resourceName3);
      try {
	httpService.registerResources(alias3, internalName3, hc1);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias3;  // create the probable URI
      out.println (uri);
      
      String alias4 = "/A/B/index.html";
      String resourceName4 = "/http_test/B/index.html";
      String internalName4 = "name4";
      
      HttpTestContext hc4 = new HttpTestContext(resourceName4);
      try {
	httpService.registerResources(alias4, internalName4, hc4);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      uri = "URI: " + ":"+port+alias4;  // create the probable URI
      out.println (uri);
      
      String alias5 = "/A/B/C/index.html";
      String resourceName5 = "/http_test/C/index.html";
      String internalName5 = "name5";
      
      HttpTestContext hc5 = new HttpTestContext(resourceName5);
      try {
	httpService.registerResources(alias5, internalName5, hc5);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      uri = "URI: " + ":"+port+alias5;  // create the probable URI
      out.println (uri);
      
      String alias6 = "/A";
      String resourceName6 = "/http_test/D/index.html";
      String internalName6 = "name6";
      
      HttpTestContext hc6 = new HttpTestContext(resourceName6);
      try {
	httpService.registerResources(alias6, internalName6, hc6);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      uri = "URI: " + ":"+port+alias6;  // create the probable URI
      out.println (uri);
    }
  }

  // 13. register a bad servlet that throws ServletException

  public final static String USAGE_HTTP065A = "";
  public final static String [] HELP_HTTP065A = {
    "Register a bad servlet, that throws ServletException."
  };

  class Http065a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      boolean expect1 = true;
      String alias = "/index6.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n6";
      Dictionary d1 = new Hashtable();
      
      HttpTestContext hc2 = new HttpTestContext(resourceName);
      
      Servlet serv1 = new HttpBadTestServlet();
      try {
	httpService.registerServlet(alias, serv1, d1, hc2);
	teststatus = false;
	expect1 = false;
      }
      catch (NamespaceException ne) {
	out.println("HTTP NamespaceException " + ne + "in HTTP065A");
	expect1 = true;
      }
      catch (ServletException se) {
	expect1 = true;
      }
      if (expect1 == false) {
	out.println("HTTP Missing NamespaceIllegalArgumentException in HTTP065A");
	teststatus = false;
      }
      
      if (teststatus == true ) {
	out.println("### Http test bundle :HTTP065A: PASS");
      }
      else {
	out.println("### Http test bundle :HTTP065A: FAIL");
      }
    }
  }


  // 17. Register test page on the server, respond with its URI
  //     This test page has a http context that always returns null
  //     when asked for a resource, to test that the http server 
  //     acts accordingly.

  public final static String USAGE_HTTP085A = "";
  public final static String [] HELP_HTTP085A = {
    "Register a test page (from inside the http_test.jar) on the server, respond with its URI",
    "The httpcontext of this page is special as it always responds",
    "with null when asked to provide a resource, i.e the server actions",
    "when this happens is checked."
  };

  class Http085a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/tom";
      String resourceName = "/http_test/externalfile/tom.html";
      String internalName = "n17";
      
      HttpTestNullContext hc11 = new HttpTestNullContext(resourceName);
      try {
	httpService.registerResources(alias, internalName, hc11);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }


  // 18. Register a test page on the server, respond with its URI

  public final static String USAGE_HTTP090A = "";
  public final static String [] HELP_HTTP090A = {
    "Register a test page (from inside the http_test.jar) on the server, respond with its URI"
  };

  class Http090a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/file";
      String resourceName = "/http_test/externalfile";
      String internalName = "n18";
      
      HttpTest2Context hc22 = new HttpTest2Context(resourceName);
      try {
	httpService.registerResources(alias, internalName, hc22);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }

  // 21. Test the dynamic port allocation (get port numbers from both server and system)

  public final static String USAGE_HTTP105A = "";
  public final static String [] HELP_HTTP105A = {
    "Gets used port number from both http server and system properties"
  };

  class Http105a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/index.html";
      String systemPort = (String)System.getProperty("org.osgi.service.http.port");
      
      out.println("Http server system port: " + systemPort);
      out.println("Http server actual port: " + port);
      
      // Reused some stuff from test 005 - get something to look at.
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }


  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      try {
	String aliaslist [] = new String [] { 
	  "/index.html",
	  "/index2.html",
	  "/index3.html",
	  "/A/index.html",
	  "/A/B/index.html",
	  "/A/B/C/index.html",
	  "/A",
	  "/tom" // ,
	  // "/file"
	};
	
	for (int i = 0; i< aliaslist.length; i++) {
	  try {
	    httpService.unregister(aliaslist[i]);
	  }
	  catch (IllegalArgumentException ne) {
	    fail("Cleanup: HTTP Exception at unregistering  " + ne);
	  }
	}
      } finally {

	try {
	  bc.ungetService(httpSR);
	} catch (Exception ignored) {
	}
      }
    }
  }
}
