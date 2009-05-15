/*
 * Copyright (c) 2004-2009, KNOPFLERFISH project
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
import java.net.*;
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
       * This bundle is used to control the functionality of the http
       * service API.  The test script calls these console commands
       * and may use their printouts.
       */

      addTest(new Setup());
      addTest(new Http005a());
      addTest(new Http006a());
      addTest(new Http010a());
      addTest(new Http015a());
      addTest(new Http015b());
      addTest(new Http020a());
      addTest(new Http025a());
      addTest(new Http029a());
      addTest(new Http030a());
      addTest(new Http035a());
      addTest(new Http040a());
      addTest(new Http045a());
      addTest(new Http050a());
      addTest(new Http055a());
      addTest(new Http060a());
      addTest(new Http065a());
      addTest(new Http085a());
      addTest(new Cleanup());
      addTest(new Http100a());
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }

  URL getUrl(String alias)
    throws MalformedURLException
  {
    return new URL("http://" + hostname + ":" + port + alias);
  }

  void checkAlias(PrintStream out, String alias)
    throws Exception
  {
    checkAlias(out, alias, true);
  }

  void checkAlias(PrintStream out, String alias, boolean bRead)
    throws Exception
  {
    URL url = getUrl(alias);
    out.println("checkAlias " + alias + ", " + url);

    if(bRead) {
      boolean bOK = false;
      InputStream is = null;
      try {
        is = url.openStream();
        byte[] buf = new byte[1024];
        int n;
        int total = 0;
        while(-1 != (n = is.read(buf))) {
          total += n;
        }
        out.println(" -> " + total + " bytes");
        bOK = total > 0;
      } catch (Exception e) {
        out.println(" -> " + e);
        throw e;
      } finally {
        try { is.close(); } catch (Exception ignored) { }
      }
      if(!bOK) {
        throw new RuntimeException("No data from alias=" + alias
                                   + ", url=" + url);
      }
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

  // Fetch the http service
  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:SETUP starting");
      httpSR = bc.getServiceReference(HttpServiceClass);
      assertNotNull("Setup: no http service ref available", httpSR);
      httpService = (HttpService) bc.getService(httpSR);
      assertNotNull("Setup: no http service object available", httpService);

      Object hostObj = httpSR.getProperty("host");
      if(hostObj != null) {
        String s = hostObj.toString();
        if(s.length() > 0) {
          hostname = s;
        }
      }

      // Now let's get on with the port...
      obj = httpSR.getProperty("port.http");
      if(obj == null) {
        obj = httpSR.getProperty("openPort");
      }
      if (obj != null) {
        port = obj.toString();
      } else {
        out.println("Ooops - failed to find the port property!!!");

        // Dump the properties as known by the http service
        String[] keys = httpSR.getPropertyKeys();
        System.out.println("--- Propetry keys ---");
        for (int i=0; i<keys.length; i++) {
          out.println(i +": " +keys[i] +" --> " +httpSR.getProperty(keys[i]));
        }
      }
      out.println("HttpTestSuite:SETUP using service with URL=" +getUrl("/"));
    }
  }

  public final static String [] HELP_HTTP006A = {
    "Test http server properties",
  };

  class Http006a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP006A starting");
      assertNotNull("Setup: no http service ref available", httpSR);

      Object obj = httpSR.getProperty("openPort");

      assertNotNull("No 'port' property set on http server registration", obj);
      assertTrue("Port property must be integer", obj instanceof Integer);
    }
  }


  public final static String [] HELP_HTTP005A = {
    "Test http server service registration",
  };

  class Http005a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP005A starting");
      assertNotNull("Setup: no http service object available", httpService);

      String alias = "/index.html";
      String resourceName = "/http_test/index.html";

      HttpTestContext hc1 = new HttpTestContext("005A");
      try {
        httpService.registerResources(alias, resourceName, hc1);
      }
      catch (NamespaceException ne) {
        fail("HTTP Exception " + ne);
      }
      checkAlias (out, alias);
    }
  }

  // 2. Unregister the test page from the server
  public final static String USAGE_HTTP010A = "";
  public final static String [] HELP_HTTP010A = {
    "Unregister the test page registerd in http005a,  respond with its URI"
  };

  class Http010a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP010A starting");
      assertNotNull("Setup: no http service object available", httpService);

      String alias = "/index.html";
      try {
        httpService.unregister(alias);
      }
      catch (IllegalArgumentException ne) {
        fail("HTTP Exception at unregistering  " + ne);
      }
      checkAlias (out, alias, false);
    }
  }

  // 3. Register a test servlet on the server, respond with its URI

  public final static String USAGE_HTTP015A = "";
  public final static String [] HELP_HTTP015A = {
    "Register a test servlet on the server, respond with its URI"
  };

  class Http015a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP015A starting");
      assertNotNull("Setup: no http service object available", httpService);

      String alias = "/index2.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();

      HttpTestContext hc2 = new HttpTestContext("015A");

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
      checkAlias (out, alias);
    }
  }

  public final static String USAGE_HTTP015B = "";
  public final static String [] HELP_HTTP015B = {
    "Register a test root servlet on the server, respond with its URI"
  };

  class Http015b extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP015B starting");
      assertNotNull("Setup: no http service object available", httpService);

      String alias = "/";
      String resourceName = "/http_test";
      Dictionary d1 = new Hashtable();

      HttpTestContext hc = new HttpTestContext("015B");

      Servlet serv1 = new HttpTestServlet();
      try {
        out.println("### 15ba");
        httpService.registerServlet(alias, serv1, d1, hc);
        checkAlias (out, alias);
      } catch (Exception ne) {
        fail("Exception " + ne);
      } finally {
        try {
          httpService.unregister(alias);
        } catch (Exception ignored) {
        }
      }

    }
  }

  // 4. Unregister the test servlet from the server

  public final static String USAGE_HTTP020A = "";
  public final static String [] HELP_HTTP020A = {
    "Unregister the test servlet registerd in http015a,  respond with its URI"
  };

  class Http020a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP020A starting");
      assertNotNull("Setup: no http service object available", httpService);

      String alias = "/index2.html";
      try {
        httpService.unregister(alias);
      }
      catch (IllegalArgumentException ne) {
        fail("HTTP Exception at unregistering  " + ne);
      }
      checkAlias (out, alias, false);
    }
  }

  // 5. Try to register a resource with a few cases of broken names
  //    and broken aliases.
  //    This should generate exceptions, which are taken as an indication
  //    that the test worked

  public final static String USAGE_HTTP025A = "";
  public final static String [] HELP_HTTP025A = {
    "Register a resources with broken names and aliases.",
    "This should generate exceptions."
  };

  class Http025a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP025A starting");
      assertNotNull("Setup: no http service object available", httpService);

      String alias = "/index.html";
      String ba1 = "index.html";
      String ba2 = "index.html/";
      String internalName = "n1";
      String bin1 = "n1/";

      String resourceName = "/http_test/index.html";
      HttpTestContext hc1 = new HttpTestContext("025A");

      try {
        httpService.registerResources(ba1, internalName, hc1);
        fail("register should not succeed");
      } catch (Exception e) {
        // expected
      }

      try {
        httpService.registerResources(ba2, internalName, hc1);
        fail("register should not succeed");
      }  catch (Exception ne) {
        // expected
      }


      try {
        httpService.registerResources(alias, bin1, hc1);
        fail("register should not succeed");
      }  catch (Exception ne) {
        // expected
      }

    }
  }

  public final static String USAGE_HTTP029A = "";
  public final static String [] HELP_HTTP029A = {
    "Register a as root and test a few resources"
  };

  class Http029a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP029A starting");
      assertNotNull("Setup: no http service object available", httpService);
      String alias = "/";

      HttpTestContext hc = new HttpTestContext("029A");
      try {
        out.println("### 29a");
        httpService.registerResources(alias, "/http_test", hc);
        checkAlias (out, "/index.html");
        checkAlias (out, "/A/index.html");
      } finally {
        try {
          httpService.unregister(alias);
        } catch (Exception ignored) {
        }
      }
    }
  }


  public final static String USAGE_HTTP030A = "";
  public final static String [] HELP_HTTP030A = {
    "Register a test page on the server,",
    "Respond with its URI"
  };

  class Http030a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP030A starting");
      assertNotNull("Setup: no http service object available", httpService);
      boolean teststatus = true;
      String alias = "/index.html";
      String resourceName = "/http_test/index.html";

      HttpTestContext hc1 = new HttpTestContext("030A");
      try {
        httpService.registerResources(alias, resourceName, hc1);
      }
      catch (NamespaceException ne) {
        fail("HTTP Exception " + ne);
      }
      checkAlias (out, alias);
    }
  }

  // 7. Register the same test page on the server, expect an exception
  public final static String USAGE_HTTP035A = "";
  public final static String [] HELP_HTTP035A = {
    "Register the same page as in http030a, expect an exception."
  };

  class Http035a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP035A starting");
      assertNotNull("Setup: no http service object available", httpService);
      String alias = "/index.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";

      HttpTestContext hc1 = new HttpTestContext("035A");
      try {
        httpService.registerResources(alias, internalName, hc1);
        fail("register should not succeed");
      } catch (NamespaceException ne) {
        // expected
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
      out.println("HttpTestSuite:HTTP040A starting");
      assertNotNull("Setup: no http service object available", httpService);
      boolean teststatus = true;
      boolean except1 = true;
      boolean except2 = true;
      boolean except3 = true;
      String ba1 = "index.html";
      String ba2 = "index.html/";
      Dictionary d1 = new Hashtable();

      String resourceName = "/http_test/index.html";
      HttpTestContext hc1 = new HttpTestContext("040A");
      Servlet serv1 = new HttpTestServlet();

      try {
        httpService.registerServlet(ba1, serv1, d1, hc1);
        fail("register should not succeed");
      } catch (NamespaceException ne) {
      } catch (ServletException se) {
      } catch (IllegalArgumentException ne) {
      }


      try {
        httpService.registerServlet(ba2, serv1, d1, hc1);
        fail("register should not succeed");
      } catch (NamespaceException ne) {
      } catch (ServletException se) {
      } catch (IllegalArgumentException ne) {
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
      out.println("HttpTestSuite:HTTP045A starting");
      assertNotNull("Setup: no http service object available", httpService);
      boolean teststatus = true;
      String alias = "/index2.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();

      HttpTestContext hc2 = new HttpTestContext("045A");

      Servlet serv1 = new HttpTestServlet();
      try {
        httpService.registerServlet(alias, serv1, d1, hc2);
        checkAlias (out, alias);
      }
      catch (Exception ne) {
        fail("HTTP Exception " + ne);
      }

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
      out.println("HttpTestSuite:HTTP050A starting");
      assertNotNull("Setup: no http service object available", httpService);
      boolean teststatus = true;
      boolean except1 = true;
      String alias = "/index2.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();

      HttpTestContext hc2 = new HttpTestContext("050A");

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
        fail("HTTP ServletException " + se);
      }
      if (except1 == false) {
        fail("HTTP Missing NamespaceIllegalArgumentException in HTTP050A");
      }
    }
  }

  // 11. Register a new test servlet with permission that are false
  public final static String USAGE_HTTP055A = "";
  public final static String [] HELP_HTTP055A = {
    "Register a servlet with permission that are false."
  };

  class Http055a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP055A starting");
      assertNotNull("Setup: no http service object available", httpService);
      boolean teststatus = true;
      String alias = "/index3.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n1";
      Dictionary d1 = new Hashtable();

      HttpTestContext hc3 = new HttpTestContext("055A");
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
      if(teststatus == false) {
        fail("Failed to register servlet");
      }

      try {
        checkAlias (out, alias, true);
        fail("Servlet should not return any result");
      } catch (Exception expected_failure) {
        // this is success
      }
    }
  }

  // 12. Register two resources that partly shadow each other
  public final static String USAGE_HTTP060A = "";
  public final static String [] HELP_HTTP060A = {
    "Register two resource that partly shadow each other."
  };

  class Http060a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP060A starting");
      assertNotNull("Setup: no http service object available", httpService);
      boolean teststatus = true;
      String alias3 = "/A/index.html";
      String resourceName3 = "/http_test/A/index.html";

      out.println("### 060A-1");
      HttpTestContext hc1 = new HttpTestContext("060A-1");
      try {
        out.println("### 060A-1 a");
        httpService.registerResources(alias3, resourceName3, hc1);
        out.println("### 060A-1 b");
        checkAlias (out, alias3);
        out.println("### 060A-1 c");
      }
      catch (NamespaceException ne) {
        fail("HTTP Exception " + ne);
      }

      out.println("## 060A-2");

      String alias4 = "/A/B/index.html";
      String resourceName4 = "/http_test/B/index.html";

      HttpTestContext hc4 = new HttpTestContext("060A-2");
      try {
        httpService.registerResources(alias4, resourceName4, hc4);
        checkAlias (out, alias4);
      }
      catch (NamespaceException ne) {
        fail("HTTP Exception " + ne);
      }

      out.println("## 060A-3");

      String alias5 = "/A/B/C/index.html";
      String resourceName5 = "/http_test/C/index.html";

      HttpTestContext hc5 = new HttpTestContext("060A-3");
      try {
        httpService.registerResources(alias5, resourceName5, hc5);
        checkAlias (out, alias5);
      }
      catch (NamespaceException ne) {
        fail("HTTP Exception " + ne);
      }

      out.println("## 060A-4");
      String alias6 = "/A";
      String resourceName6 = "/http_test/D/index.html";

      HttpTestContext hc6 = new HttpTestContext("060A-4");
      try {
        httpService.registerResources(alias6, resourceName6, hc6);
        checkAlias (out, alias6);
      }
      catch (NamespaceException ne) {
        fail("HTTP Exception " + ne);
      }
    }
  }

  // 13. register a bad servlet that throws ServletException

  public final static String USAGE_HTTP065A = "";
  public final static String [] HELP_HTTP065A = {
    "Register a bad servlet, that throws ServletException."
  };

  class Http065a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP065A starting");
      assertNotNull("Setup: no http service object available", httpService);
      boolean teststatus = true;
      boolean expect1 = true;
      String alias = "/index6.html";
      String resourceName = "/http_test/index.html";
      String internalName = "n6";
      Dictionary d1 = new Hashtable();

      HttpTestContext hc2 = new HttpTestContext("065A");

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
      out.println("HttpTestSuite:HTTP085A starting");
      assertNotNull("Setup: no http service object available", httpService);
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
        fail("Failed to register resource: " + ne);
        teststatus = false;
      }

      try {
        checkAlias (out, alias);
        fail("No data should be returned from HttpTestNullContext");
      } catch (Exception expected_failure) {
        // this is ok
      }
    }
  }




  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:CLEANUP starting");
      assertNotNull("Setup: no http service ref available", httpSR);
      assertNotNull("Setup: no http service object available", httpService);
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

  // This test case should be executed after cleanup since it
  // will restart the HttpService and thus makes the httpSR and
  // httpService variables chared by the other test cases void.
  public final static String USAGE_HTTP100A = "";
  public final static String [] HELP_HTTP100A = {
    "Check that Servlet.destroy is called when the HttpService stops."
  };

  class Http100a extends FWTestCase {
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:HTTP100A starting");
      httpSR = bc.getServiceReference(HttpServiceClass);
      assertNotNull("Setup: no http service ref available", httpSR);
      httpService = (HttpService) bc.getService(httpSR);
      assertNotNull("Setup: no http service object available", httpService);

      String alias = "/index100.html";
      Dictionary d1 = new Hashtable();

      HttpTestContext hc = new HttpTestContext("100A");

      HttpTestServlet serv1 = new HttpTestServlet();
      try {
        httpService.registerServlet(alias, serv1, d1, hc);
      } catch (NamespaceException ne) {
        out.println("Unexpected namespace exception: "+ne);
        ne.printStackTrace();
        fail("Unexpected namespace exception: "+ne);
      } catch (ServletException se) {
        out.println("Unexpected servlet exception: "+se);
        se.printStackTrace();
        fail("Unexpected servlet exception: "+se);
      }
      Bundle httpBundle = httpSR.getBundle();
      try {
        httpBundle.stop();
      } catch (Exception e) {
        out.println("Failed to stop HttpService providing bundle: "+e);
        e.printStackTrace();
        fail("Failed to stop HttpService providing bundle: "+e);
      }

      assertTrue("Servlet.destroy() should be called when HttpService "
                 +"is stopped", serv1.destroyCalled);

      try {
        httpBundle.start();
      } catch (Exception e) {
        out.println("Failed to restart HttpService providing bundle: "+e);
        e.printStackTrace();
        fail("Failed to restart HttpService providing bundle: "+e);
      }
    }
  }


}
