/*
 * Copyright (c) 2015, KNOPFLERFISH project
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

package org.knopflerfish.bundle.http_servlet_test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.knopflerfish.service.http_servlet_test.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;

import javax.servlet.*;

import junit.framework.*;

import org.knopflerfish.service.junit.*;

public class HttpServletTestSuite extends TestSuite  {
  public static final String TEST_SERVLET_ALIAS = "/testservlet";
  
  BundleContext bc;
  Bundle bu;
  String HttpServiceClass = "org.osgi.service.http.HttpService";

  String http     = "http://";
  String hostname = "localhost";
  public static int port;
  Object obj;

  ServiceReference httpSR = null;

  PrintStream out = System.out;

  public Servlet testServlet;

  public HttpContext testContext;

  private FWTestCase activeTestCase;

  public TestResult testResult;

  public TestSuite getRequestTestSuite;
  public TestSuite postRequestTestSuite;

  public TestSuite runThisSuite;

  private TestSuite getResponseTests;

  private TestSuite postResponseTests;

  private HttpClient httpclient;

  private static HttpMethod currentHttpMethod;

  // private TestSuite responseTests;
  
  static HttpService httpService;
  /* common resource/servlet references */

  // private static HttpURLConnection currentRequestConnection;

  public HttpServletTestSuite (BundleContext bc) {
    super ("HttpServletTestSuite");

    try {
      this.bc = bc;
      bu = bc.getBundle();

      // Tests the Servlet API on an HTTP GET request, including testing
      // the response received
      getRequestTestSuite = new TestSuite("GetRequestTestSuite");
      getRequestTestSuite.addTestSuite(HttpServletRequestTestCase.class);
      getRequestTestSuite.addTestSuite(HttpServletResponseTestsDefaultValues.class);
      getRequestTestSuite.addTestSuite(HttpServletResponseTests.class);
      getRequestTestSuite.addTestSuite(SendGetResponseTests.class);
      
   // Tests the Servlet API on an HTTP POST request, including testing
      // the response received
      postRequestTestSuite = new TestSuite("PostRequestTestSuite");
      postRequestTestSuite.addTestSuite(HttpServletRequestTestCase.class);
      postRequestTestSuite.addTestSuite(HttpServletResponseTestsDefaultValues.class);
      postRequestTestSuite.addTestSuite(HttpServletResponseTests.class);
      postRequestTestSuite.addTestSuite(PostRequestTests.class);
      postRequestTestSuite.addTestSuite(SendPostResponseTests.class);
      
      getResponseTests = new TestSuite(GetResponseTests.class);
      postResponseTests = new TestSuite(PostResponseTests.class);
  
      
      /*
       * This bundle is used to test the Servlet API primarily.
       * The test cases below act as proxies and trigger off the "real" tests
       * carried out by the test suites defined above. In short, the tests work like this:
       * 1. The proxy test cases below are called
       * 2. They will use a normal HttpURLConnection and make requests to the HTTP server,
       *    e.g. GET and POST.
       * 3. When requests are received by the test servlet tests are invoked dynamically and test results
       *    are reported back and merged into the proxy's TestResult
       * 4. Additional tests may be invoked upon receiving the Response. They are reported back in
       *    a similar manner as above.
       * 5. This use of JUnit can be regarded as unorthodox
       *  
       */

      addTest(new Setup());
      addTest(new GetRequestSuiteProxy(5));
      addTest(new PostRequestSuiteProxy());
      addTest(new Cleanup());
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
    boolean passed = true;
    Throwable failure = null;
    protected String failMessage = null;
    
    public FWTestCase() {
      ;
    }
    @Override
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
    
    @Override
    public void run(final TestResult result) {
      log("run() called, assigning testResult: " + result);
      log("setting activeTestCase: " + this.getName()); 
      
      testResult = result;
      activeTestCase = this;
      super.run(result);
      testResult = null;
      activeTestCase = null;
      log("run() completed: " + this.getName()); 
    }
    
    public void setFailure(Throwable t) {
      passed = false;
      failure = t;
    }
    
    public boolean hasPassed() {
      return passed;
    }
    
    public void reportResult(TestResult result) {
       if (!result.wasSuccessful()) {
         passed = false;
         // Merge TestResult
         System.out.println("The test failed, merging failures");
         for (Enumeration e = result.errors(); e.hasMoreElements(); ) {
           TestFailure failure = (TestFailure)e.nextElement();
           testResult.addError(failure.failedTest(), failure.thrownException());
         }
         for (Enumeration e = result.failures(); e.hasMoreElements(); ) {
           TestFailure failure = (TestFailure)e.nextElement();
           testResult.addFailure(failure.failedTest(), (AssertionFailedError) failure.thrownException());
         }
         failMessage = "Composite Test Failed. Reporting back via test case proxy";
       }
    }
  }

  
  // Fetch the http service
  class Setup extends FWTestCase {
    
    @Override
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
      if (obj != null && obj instanceof Integer) {
        port = ((Integer)obj).intValue();
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
      
      testServlet = new HttpTestServlet(HttpServletTestSuite.this);
      testContext = new HttpTestContext("HttpTestContext");
      
      httpService.registerServlet(TEST_SERVLET_ALIAS, testServlet, null, testContext);
      
    }
  }

  class GetRequestSuiteProxy extends FWTestCase {
    private int num_calls = 1;
    
    GetRequestSuiteProxy(int count) {
      super();
      num_calls = count;
    }
    
    @Override
    public void runTest() throws Throwable {
      out.println("HttpTestSuite - starting: " + getName());
     httpclient = new HttpClient();
      
      URL url = getUrl(TEST_SERVLET_ALIAS);
      // Set expected values
      TestData.isSecure = false;
      TestData.method = "GET";
      TestData.queryString = null;
      TestData.requestURL = url.toString();
      
      runThisSuite = getRequestTestSuite;

      currentHttpMethod = new GetMethod(url.toString());
      
      for (int i = 0; i < num_calls; i++) {
        httpclient.executeMethod(currentHttpMethod);
        HttpServletTestSuite.this.runTestSuite(getResponseTests, null);
      }
      
      currentHttpMethod.releaseConnection();
      currentHttpMethod = null;
      
      // read and check result
      if (!passed) {
        fail(failMessage);
      }
    }
  }
  
  class PostRequestSuiteProxy extends FWTestCase {
    
    @Override
    public void runTest() throws Throwable {
      out.println("HttpTestSuite - starting: " + getName());
      
      URL url = getUrl(TEST_SERVLET_ALIAS);
      // Set expected values
      TestData.isSecure = false;
      TestData.method = "POST";
      TestData.queryString = null;
      TestData.requestURL = url.toString();
      runThisSuite = postRequestTestSuite;
      
//      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//      conn.setDoOutput(true);
//      conn.setRequestMethod("POST");
//      conn.setRequestProperty("Content-Type", "text/plain");
      
      // byte[] postbuf = TestData.postRequestBody.getBytes("UTF-8");
      RequestEntity requestEntity = new StringRequestEntity(TestData.postRequestBody,
                                                            "text/plain",
                                                            "UTF-8");
      
      // conn.setRequestProperty("Content-Length", String.valueOf(postbuf.length));
      // currentHttpMethod = 
      PostMethod postMethod = new PostMethod(url.toString());
      currentHttpMethod = postMethod;
      postMethod.setRequestEntity(requestEntity);
      
//      currentHttpMethod.setRe
//      conn.connect();
//      OutputStream os = conn.getOutputStream();
//      os.write(postbuf, 0, postbuf.length);
//      os.close();
//      currentRequestConnection = conn;

      httpclient.executeMethod(currentHttpMethod);
      HttpServletTestSuite.this.runTestSuite(postResponseTests, null);
      
      // conn.disconnect();
      
      // OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
      // osw.write(TestData.postRequestBody);
      
      // read and check result
      currentHttpMethod.releaseConnection();
      currentHttpMethod = null;
      
      if (!passed) {
        fail(failMessage);
      }
      
    }
  }

  class Cleanup extends FWTestCase {

    @Override
    public void runTest() throws Throwable {
      out.println("HttpTestSuite:CLEANUP starting");
      assertNotNull("Setup: no http service ref available", httpSR);
      assertNotNull("Setup: no http service object available", httpService);
      try {
        httpService.unregister(TEST_SERVLET_ALIAS);
      }
      catch (IllegalArgumentException ne) {
        fail("Cleanup: HTTP Exception at unregistering  " + ne);
      }
      try {
        bc.ungetService(httpSR);
      } catch (Exception ignored) { }
    }
  }


  private void setTestCase(FWTestCase testCase)
  {
    activeTestCase = testCase;
  }

  public void setFailure(Throwable t) {
    activeTestCase.setFailure(t);
  }
  
  public TestCase getTestCase() {
    return activeTestCase;
  }

  public TestResult getTestResult()  {
    return testResult;
  }

  public void reportResult(TestSuite testSuite, TestResult result)
  {
    reportResult(testSuite, result, activeTestCase);
  }

  public void reportResult(TestSuite testSuite, TestResult result, Test parent) {
    activeTestCase.reportResult(result);
    RemoteTestResult testRes = new RemoteTestResult(testSuite, result, parent);
    ServiceRegistration sr = bc.registerService(JUnitResult.class.getName(), testRes, null);
  }
  
  public void runTestSuite(TestSuite suite, TestSuite parent) {
    System.out.println("Running the test suite: " + suite.getName());
    TestResult result = new TestResult();
    
    // System.out.println("Running Test Suite");
    suite.run(result);
    System.out.println("Report Result for suite: " + suite.getName() + " test runs: " + result.runCount() + " successful: " + result.wasSuccessful());
    if (parent != null)
      reportResult(suite, result, parent);
    else
      reportResult(suite, result);
  }

  class RemoteTestResult implements JUnitResult {
    private TestSuite testSuite;
    private Test parentTest;
    private TestResult result;
    
    RemoteTestResult(TestSuite testSuite, TestResult result, Test parentTest) {
      this.testSuite = testSuite;
      this.result = result;
      this.parentTest = parentTest;
    }

    @Override
    public Test getParentTest() {
      return parentTest;
    }

    @Override
    public TestResult getResult() {
      return result;
    }

    @Override
    public TestSuite getTestSuite()  {
      return testSuite;
    }

  }
  
  static void log(String s) {
    System.out.println("HttpServletTestSuite: " + s);
  }

//  public static HttpURLConnection getRequestConnection()  {
//    return currentRequestConnection;
//  }

  public static HttpMethod getCurrentHttpMethod() {
    return currentHttpMethod;
  }

}
