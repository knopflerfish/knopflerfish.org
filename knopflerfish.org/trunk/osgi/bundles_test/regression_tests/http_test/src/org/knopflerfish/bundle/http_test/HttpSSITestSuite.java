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
import org.knopflerfish.service.console.*;
import org.osgi.service.http.HttpService;

import javax.servlet.*;
import javax.servlet.http.*;

import junit.framework.*;

public class HttpSSITestSuite extends TestSuite  {
  BundleContext bc;
  Bundle        bu;
  String HttpServiceClass = "org.osgi.service.http.HttpService";
  
  String http     = "http://";
  String hostname = "localhost";
  String port;
  Object obj;

  PrintStream out = System.out;

  static HttpService httpService;
  
  public HttpSSITestSuite (BundleContext bc) {
    super ("HttpSSITestSuite");

    try {
	this.bc = bc;
	bu = bc.getBundle();

	// Get the used port (may vary!) from the http server
	httpService = (HttpService) bc.getService(bc.getServiceReference(HttpServiceClass));
	ServiceReference ref = bc.getServiceReference(HttpService.class.getName());
	System.out.println("HttpService.class.getName():" + HttpService.class.getName());
	
	// Now let's get on with the port...
	obj = ref.getProperty("port");
	if (obj != null) {
	    port = obj.toString();
	} else {
	    System.out.println("Ooops - failed to find the port property!!!");

	    // Dump the properties as known by the http service
	    String[] keys = ref.getPropertyKeys();
	    System.out.println("--- Propery keys ---");
	    for (int i=0; i<keys.length; i++) {
		System.out.println(i + ": " + keys[i] + " --> "+ ref.getProperty(keys[i]));
	    }
	    System.out.println("--------------------");
	}

	/*
	  This bundle is used to control the functionality of the http service API.
	  The test script calls these console commands and may use their printouts.

	*/

	addTest(new Setup());
	addTest(new Http070a());
	addTest(new Http075a());
	addTest(new Http080a());
	addTest(new Http095a());
	addTest(new Http100a());
	addTest(new Http110a());
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
    }
  }


  // 14. register three resource that makes server side include (SSI)
  // ssitest1.shtml includes ssitest2.shtml 
  // ssitest2.shtml includes ssitest3.shtml 
  //
  public final static String USAGE_HTTP070A = "";
  public final static String [] HELP_HTTP070A = {
    "Register three resources that makes server side includes (SSI).",
    "ssitest1.shtml includes ssitest2.shtml",
    "ssitest2.shtml includes ssitest3.shtml"
  };

  class Http070a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/ssitest";
      String resourceName = "/http_test/SSI";
      
      HttpTest3Context hc1 = new HttpTest3Context();
      try {
	httpService.registerResources(alias, resourceName, hc1);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }

  // 15. register a resource that makes include of a gif picture
  //     and the gif picture itself
  //
  public final static String USAGE_HTTP075A = "";
  public final static String [] HELP_HTTP075A = {
    "Register a resource that includes a .gif picture and ",
    "the .gif picture itself"
  };

  class Http075a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/ssi4.html";
      String resourceName = "/http_test/SSI/ssi4.html";
      String internalName = "s9";
      
      HttpTestContext hc9 = new HttpTestContext(resourceName);
      try {
	httpService.registerResources(alias, internalName, hc9);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
      
      /*                                             */ 
      
      alias = "/splash.gif";
      resourceName = "/http_test/SSI/splash.gif";
      internalName = "s10";
      
      HttpTestContext hc10 = new HttpTestContext(resourceName);
      try {
	httpService.registerResources(alias, internalName, hc10);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }


  // 16. register two resources that makes server side include (SSI)
  // reqursively to each other
  //
  // ssitestA.shtml includes ssitestB.shtml
  // ssitestB.shtml includes ssitestA.shtml
  //
  public final static String USAGE_HTTP080A = "";
  public final static String [] HELP_HTTP080A = {
    "Register three resources that makes server side includes (SSI).",
    "reqursively of eachother.",
    "ssitestA.shtml includes ssitestB.shtml",
    "ssitestB.shtml includes ssitestA.shtml"
  };

  class Http080a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/ssiloop";
      String resourceName = "/http_test/SSI-loop";
      String internalName = "sA";
      
      HttpTest3Context hcA = new HttpTest3Context();
      try {
	httpService.registerResources(alias, resourceName, hcA);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }


  // 19. Register a test page with SSI of another .html page on the server, respond with its URI

  public final static String USAGE_HTTP095A = "";
  public final static String [] HELP_HTTP095A = {
    "Register a test page (from inside the http_test.jar) on the server, respond with its URI"
  };

  class Http095a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/reg_file";
      String resourceName = "/http_test/SSI-file";
      String internalName = "/http_test/SSI-file";
      
      HttpTest3Context hc23 = new HttpTest3Context();
      try {
	httpService.registerResources(alias, resourceName, hc23);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }


  // 20. Register a test page with SSI of another, non existing .html page on the server,
  //  respond with its URI

  public final static String USAGE_HTTP100A = "";
  public final static String [] HELP_HTTP100A = {
    "Register a test page (from inside the http_test.jar) on the server, respond with its URI",
    "The page refers to a non existent page"
  };

  class Http100a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/reg_no_file";
      String resourceName = "/http_test/SSI-file";
      
      HttpTest3Context hc24 = new HttpTest3Context();
      try {
	httpService.registerResources(alias, resourceName, hc24);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }

  // 22. Register a test page with all supported SSI commands, respond with its URI

  public final static String USAGE_HTTP110A = "";
  public final static String [] HELP_HTTP110A = {
    "Register a test page with all supported SSI commands, respond with its URI"
  };

  class Http110a extends FWTestCase {
    public void runTest() throws Throwable {
      boolean teststatus = true;
      String alias = "/ssi_commands";
      String resourceName = "/http_test/SSI-commands";
      
      HttpTest3Context hc24 = new HttpTest3Context();
      try {
	httpService.registerResources(alias, resourceName, hc24);
      }
      catch (NamespaceException ne) {
	out.println("HTTP Exception " + ne);
	teststatus = false;
      }
      String uri = "URI: " + ":"+port+alias;  // create the probable URI
      out.println (uri);
    }
  }



  // 995. Cleanup, ie unregister everything
  public final static String USAGE_HTTP995A = "";
  public final static String [] HELP_HTTP995A = {
    "Cleanup after test, unregister everything.",
  };

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      String aliaslist [] = new String [] { 
	"/ssi4.html",
	"/ssi_commands",
	// "/file"
      };
      
      for (int i = 0; i< aliaslist.length; i++) {
	try {
	  httpService.unregister(aliaslist[i]);
	}
	catch (IllegalArgumentException ne) {
	  fail("HTTP Exception at unregistering  " + ne);
	}
      }
    }
  }
}
