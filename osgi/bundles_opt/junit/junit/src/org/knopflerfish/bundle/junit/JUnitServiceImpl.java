/*
 * Copyright (c) 2004-2022 KNOPFLERFISH project
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

package org.knopflerfish.bundle.junit;

import java.util.*;
import java.io.*;

import org.knopflerfish.service.junit.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

import junit.framework.*;

import java.lang.reflect.*;

public class JUnitServiceImpl implements JUnitService {

  private HashMap<String,JUnitResult> collectedResults = new HashMap<String,JUnitResult>();
  private HashMap<String,Vector<JUnitResult>> referencedResults = new HashMap<String,Vector<JUnitResult>>();
  
  private LinkedHashMap<String,TestSuite> subsuites = new LinkedHashMap<String,TestSuite>();
  
  JUnitServiceImpl() {
  }

  public void runTest(PrintWriter out, TestSuite suite) throws IOException {
    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      runTestXML(out, suite);

      out.flush ();
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }

  public TestResult runTestSuite(PrintWriter out, TestSuite suite) throws IOException {
    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
    TestResult testResult;
    
    try {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      testResult = runTestXML(out, suite);
      out.flush ();
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
    return testResult;
  }
  
  public TestResult runTestXML(PrintWriter out, TestSuite suite) throws IOException {

    TestResult testResult = null;
    
    out.println("<?xml version=\"1.0\"?>");
    out.println("<?xml-stylesheet type=\"text/xsl\" href=\"junit_style.xsl\"?>");
    out.print("<junit");
    out.print(" date=\"" + (new Date()) + "\"");
    out.println(">");

    dumpSystemProps(out);

    try {
      testResult = runTestCase(out, suite);
    } catch (Exception e) {
      out.println(" <fail " +
                  "  message=\"" + e.getMessage() + "\"" +
                  "  exception=\"" + e.getClass().getName() + "\">");
      toXMLException(e, out);
      out.println(" </fail>");
    }

    out.println("</junit>");
    return testResult;
  }

  void dumpSystemProps(PrintWriter out) throws IOException {

    dumpProps(out, System.getProperties(), "System.properties");

    Hashtable props = new Hashtable();
    copyBCProp(props, Constants.FRAMEWORK_VENDOR);
    copyBCProp(props, Constants.FRAMEWORK_VERSION);
    copyBCProp(props, Constants.FRAMEWORK_OS_NAME);
    copyBCProp(props, Constants.FRAMEWORK_PROCESSOR);
    copyBCProp(props, Constants.FRAMEWORK_OS_VERSION);
    copyBCProp(props, Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

    dumpProps(out, props, "Framework.properties");
  }

  void copyBCProp(Hashtable props, String name) {
    String s = Activator.bc.getProperty(name);
    if(s != null) {
      props.put(name, s);
    } else {
      props.put(name, "[null]");
    }
  }

  void dumpProps(PrintWriter out,
                 Dictionary props,
                 String name) throws IOException {
    out.println("<properties name=\"" + name + "\">");
    try {
      for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
        String key = (String)e.nextElement();
        Object val = props.get(key);
        out.print(" <value key=\"" + key + "\">");
        out.print("<![CDATA[" + val + "]]>");
        out.println("</value>");
      }
    } catch (Exception e) {
      out.println("<!--");
      e.printStackTrace(out);
      out.println("-->");
    }
    out.println("</properties>");
  }

  protected TestResult runTestCase(PrintWriter out,
                                   TestSuite suite)  throws Exception {

    out.println(" <testcase id=\"" + suite.getName() + "\">");

    String desc   = getBeanString(suite, "getDescription", "");
    out.print("  <description>");
    out.print("<![CDATA[" + desc + "]]>");
    out.println("</description>");

    String docurl = getBeanString(suite, "getDocURL", "");
    out.print("  <docurl>");
    out.print("<![CDATA[" + docurl + "]]>");
    out.println("</docurl>");

    ServiceTracker testListenerTracker = null;
    final TestResult tr = new TestResult();
    
    try {
      System.out.println("run test on " + suite);

     
      testListenerTracker = new ServiceTracker(Activator.bc,
                                               TestListener.class.getName(),
                                               null) {
          public Object addingService(ServiceReference reference)
          {
            TestListener tl = (TestListener) Activator.bc.getService(reference);
            tr.addListener(tl);
            return tl;
          }
          public void removedService(ServiceReference reference, Object service)
          {
            TestListener tl = (TestListener) service;
            tr.removeListener(tl);
          }
        };
      testListenerTracker.open();

      long start = System.currentTimeMillis();
      suite.run(tr);
      long stop  = System.currentTimeMillis();
      
      // Clear tables
      collectedResults.clear();
      referencedResults.clear();
      subsuites.clear();
      
      collectRegisteredTestReults();
      toXMLSuite(suite, tr, stop - start, out, 2, true, true);
      
      TestSuite[] suites = subsuites.values().toArray(new TestSuite[0]);
      for (int i = 0; i < suites.length; i++) {
        collectReferencedSuites(suites[i]);
      }
//      for (Iterator<JUnitResult> it = collectedResults.values().iterator(); it.hasNext(); ) {
//        toXMLSuite(it.next().getTestSuite(), tr, 0, out, 2, false, true);
//      }
      for (Iterator<TestSuite> it = subsuites.values().iterator(); it.hasNext(); ) {
        toXMLSuite(it.next(), tr, 0, out, 2, false, false);
      }
      toXMLResult(tr, out);
      testListenerTracker.close();
    } finally {
      out.println(" </testcase>");
      if (null!=testListenerTracker){
        testListenerTracker.close();
      }
    }
    return tr;
  }

  private void collectRegisteredTestReults()
  {
    Activator.log.info("Collecting Registered Test Results");
    Collection<ServiceReference<JUnitResult>> resultServices;
    try {
      resultServices = Activator.bc.getServiceReferences(JUnitResult.class, 
                                                         null);
    
      for (Iterator<ServiceReference<JUnitResult>> it = resultServices.iterator(); it.hasNext(); ) {
        JUnitResult res = Activator.bc.getService(it.next());
        collectedResults.put(res.getTestSuite().getName(), res);
        
        Vector<JUnitResult> v = referencedResults.get(getTestName(res.getParentTest()));
        if (v == null) {
          v = new Vector<JUnitResult>();
          v.add(res);
          referencedResults.put(getTestName(res.getParentTest()), v);
        }
        else {
          v.add(res);
        }
        // referencedResults.put(getTestName(res.getParentTest()), res);
        Activator.log.info("Collected test suite: " + res.getTestSuite().getName());
      }
    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static String getTestName(Test test)  {
    if (test instanceof TestSuite)
      return ((TestSuite)test).getName();
    else if (test instanceof TestCase)
      return ((TestCase)test).getName();
    else
      throw new IllegalArgumentException("Unexpected Test type");
  }

  public TestSuite getTestSuite(final String id,
                                final String subid) {
    Object obj = null;

    try {
      ServiceReference[] srl =
        Activator.bc.getServiceReferences((String)null, "(service.pid=" + id + ")");

      if(srl == null || srl.length == 0) {
        obj = new TestCase("No id=" + id) {
            public void runTest() {
              throw new IllegalArgumentException("No test with id=" + id);
            }
          };
      }
      if(srl != null && srl.length != 1) {
        obj = new TestCase("Multiple id=" + id) {
            public void runTest() {
              throw new IllegalArgumentException("More than one test with id=" + id);
            }
          };
      }
      if(obj == null) {
        obj = Activator.bc.getService(srl[0]);
      }
    } catch (Exception e) {
      obj = new TestCase("Bad filter syntax id=" + id) {
          public void runTest() {
            throw new IllegalArgumentException("Bad syntax id=" + id);
          }
        };
    }

    if(!(obj instanceof Test)) {
      final Object oldObj = obj;
      obj = new TestCase("ClassCastException") {
          public void runTest() {
            throw new ClassCastException("Service implements " +
                                         oldObj.getClass().getName() +
                                         " instead of " +
                                         Test.class.getName());
          }
        };
    }

    Test test = (Test)obj;

    TestSuite suite;

    if(subid != null && !"".equals(subid)) {
      Test subtest = findTest(test, subid);
      if(subtest != null) {
        test = subtest;
      } else {
        test = new TestCase("IllegalArgumentException") {
            public void runTest() {
              throw new ClassCastException("subtest " + subid + " not found");
            }
          };
      }
    }

    if(test instanceof TestSuite) {
      suite = (TestSuite)test;
    } else {
      suite = new TestSuite(id);
      suite.addTest(test);
    }


    return suite;
  }

  protected Test findTest(Test test, String id) {
    if(test instanceof TestSuite) {
      TestSuite ts = (TestSuite)test;
      if(id.equals(ts.getName()) || id.equals(ts.getClass().getName())) {
        return ts;
      }
      for(int i = 0; i < ts.testCount(); i++) {
        Test child = ts.testAt(i);
        Test r = findTest(child, id);
        if(r != null) {
          return r;
        }
      }
    }
    if(test instanceof TestCase) {
      TestCase tc = (TestCase)test;
      if(id.equals(tc.getName())) {
        return tc;
      }
    }
    if(id.equals(test.getClass().getName())) {
      return test;
    }
    return null;
  }

  protected static String indent(int n) {
    StringBuilder sb = new StringBuilder();
    while(n --> 0) {
      sb.append(" ");
    }
    return sb.toString();
  }

  protected void toXMLSuite(TestSuite suite,
                            TestResult tr,
                            long time,
                            PrintWriter out,
                            int n,
                            boolean collect,
                            boolean recursive) throws IOException {
    out.print(indent(n) + "  <suite class = \"" +
              suite.getClass().getName() + "\"");
    out.print(" name  = \"" + suite.getName() + "\"");
    out.print(" time  = \"" + time + "\"");
    out.println(">");

    for(int i = 0; i < suite.testCount(); i++) {
      Test test = suite.testAt(i);
      String clazz = test.getClass().getName();
      String name  = null;
      if(test instanceof TestCase) {
        name = ((TestCase)test).getName();
      }
      if (test instanceof TestSuite) {
        name = ((TestSuite)test).getName();
      }
      if(name == null) {
        name = clazz;
      }
      if(test instanceof TestSuite) {
        out.print(indent(n) + "   <suitecase class = \"" + clazz + "\"");
        out.print(" name  = \"" + name + "\"");
        out.print(" status = \"" + getTestCaseStatus(tr, test) + "\"");
        out.println(">");
        String desc   = getBeanString(test, "getDescription", "");
        out.print("  <description>");
        out.print("<![CDATA[" + desc + "]]>");
        out.println("</description>");
        
        out.println("</suitecase>");
        
//        if (collect)
//          subsuites.add((TestSuite) test);
        if (recursive)
          toXMLSuite((TestSuite)test, tr, 0, out, n + 1, collect, recursive);
      } else {
        Vector<JUnitResult> linkedTest = referencedResults.get(name);
        out.print(indent(n) + "   <case class = \"" + clazz + "\"");
        out.print(" name  = \"" + name + "\"");
        out.print(" status = \"" + getTestCaseStatus(tr, test) + "\"");
        
        out.println(">");
        if (linkedTest != null) {
          for (Iterator<JUnitResult> it = linkedTest.iterator(); it.hasNext(); ) {
            JUnitResult jres = it.next();
            out.print(indent(n) + "   <ref refname = \"" + jres.getTestSuite().getName() +  "\" />"); 
            if (collect) {
              if (!subsuites.containsKey(jres.getTestSuite().getName()))
              subsuites.put(jres.getTestSuite().getName(), jres.getTestSuite());
            }
          }
          // out.print(" ref = \"" + linkedTest.getTestSuite().getName() + "\"");
        }
        String desc   = getBeanString(test, "getDescription", "");
        out.print("  <description>");
        out.print("<![CDATA[" + desc + "]]>");
        out.println("</description>");
        
        out.println("</case>");
      }
    }
    out.println(indent(n) + "  </suite>");
  }

  private void collectReferencedSuites(TestSuite suite) {
    for(int i = 0; i < suite.testCount(); i++) {
      Test test = suite.testAt(i);
      if (test instanceof TestSuite) {
        TestSuite ts = (TestSuite)test;
        subsuites.put(ts.getName(), ts);
        collectReferencedSuites(ts);
      }
      else if (test instanceof TestCase) {
        TestCase tc = (TestCase)test;
        Vector<JUnitResult> linkedTest = referencedResults.get(getTestName(test));
        if (linkedTest != null) {
          for (Iterator<JUnitResult> it = linkedTest.iterator(); it.hasNext(); ) {
            JUnitResult jres = it.next();
            subsuites.put(jres.getTestSuite().getName(), jres.getTestSuite());
          }
        }
      }
    }
  }

  String getTestCaseStatus(TestResult tr, Test test) {
    for(Enumeration e = tr.failures(); e.hasMoreElements(); ) {
      TestFailure tf = (TestFailure)e.nextElement();
      if(tf.failedTest() == test) {
        return "failed";
      }
    }
    for(Enumeration e = tr.errors(); e.hasMoreElements(); ) {
      TestFailure tf = (TestFailure)e.nextElement();
      if(tf.failedTest() == test) {
        return "error";
      }
    }
    return "passed";
  }


  protected void toXMLResult(TestResult tr, PrintWriter out) throws IOException {
    out.println("  <testresult wasSuccessful = \"" + tr.wasSuccessful() + "\"");
    out.println("              runCount      = \"" + tr.runCount() + "\"");
    out.println("              failureCount  = \"" + tr.failureCount() + "\"");
    out.println("              errorCount    = \"" + tr.errorCount() + "\"");
    out.println("  >");
    if(tr.failureCount() > 0 ) {
      out.println("   <failures>");
      for(Enumeration e = tr.failures(); e.hasMoreElements(); ) {
        TestFailure tf = (TestFailure)e.nextElement();
        toXMLFailure(tf, out);
      }
      out.println("   </failures>");
    }
    if(tr.errorCount() > 0 ) {
      out.println("   <errors>");
      for(Enumeration e = tr.errors(); e.hasMoreElements(); ) {

        TestFailure tf = (TestFailure)e.nextElement();
        toXMLFailure(tf, out);
      }
      out.println("   </errors>");
    }
    out.println("  </testresult>");
  }

  void toXMLFailure(TestFailure tf, PrintWriter out) throws IOException {
    out.println("    <failure");

    out.println("      exceptionMessage=\"" + escape(tf.exceptionMessage()) + "\"");
    Test failedTest = tf.failedTest();
    if(failedTest instanceof TestCase) {
      TestCase tc = (TestCase)failedTest;
      String name = tc.getName();
      if(name == null) {
        name = tc.getClass().getName();
      }

      out.println("      failedTestCaseName=\"" + escape(name) + "\"");
      out.println("      failedTestCaseClass=\"" + tc.getClass().getName() + "\"");
    } else {
      out.println("      failedTest=\"" + tf.failedTest() + "\"");
    }
    out.println("    >");
    out.print("     <trace><![CDATA[");
    out.print(tf.trace());
    out.println("]]></trace>");

    out.println("    </failure>");
  }

  protected String escape(String s) {
    if(s == null) {
      return null;
    }
    s = s
      .replace('<', '[')
      .replace('>', ']')
      .replace('\"', '\'');
    s = replace(s, "&", "&amp;");

    return s;
  }

  protected void toXMLError(AssertionFailedError af, PrintWriter out) throws IOException {
    out.println("    <error ");
    out.println("      exceptionMessage=\"" + escape(af.getMessage()) + "\"");
    out.println("    >");
    toXMLException(af, out);
    out.println("    </failure>");
  }

  protected void toXMLException(Throwable t, PrintWriter out) throws IOException {
    out.print("     <exception class=\"" + t.getClass().getName() + "\">");
    out.print("<![CDATA[");
    t.printStackTrace(out);
    out.println("]]></exception>");
  }

  static String getBeanString(Object target,
                              String methodName,
                              String defVal) {
    String val = defVal;
    try {
      Method m = target.getClass().getMethod(methodName,
                                             new Class[] { });
      val = (String) m.invoke(target, (Object[]) null);
    } catch (Exception e) {
      return defVal;
    }
    return val;
  }

  /**
   * Replace all occurances of a substring with another string.
   *
   * <p>
   * The returned string will shrink or grow as necessary, depending on
   * the lengths of <tt>v1</tt> and <tt>v2</tt>.
   * </p>
   *
   * <p>
   * Implementation note: This method avoids using the standard String
   * manipulation methods to increase execution speed.
   * Using the <tt>replace</tt> method does however
   * include two <tt>new</tt> operations in the case when matches are found.
   * </p>
   *
   *
   * @param s  Source string.
   * @param v1 String to be replaced with <code>v2</code>.
   * @param v2 String replacing <code>v1</code>.
   * @return Modified string. If any of the input strings are <tt>null</tt>,
   *         the source string <tt>s</tt> will be returned unmodified.
   *         If <tt>v1.length == 0</tt>, <tt>v1.equals(v2)</tt> or
   *         no occurances of <tt>v1</tt> is found, also
   *         return <tt>s</tt> unmodified.
   */
  public static String replace(final String s,
                               final String v1,
                               final String v2) {

    // return quick when nothing to do
    if(s == null
       || v1 == null
       || v2 == null
       || v1.length() == 0
       || v1.equals(v2)) {
      return s;
    }

    int ix       = 0;
    int v1Len    = v1.length();
    int n        = 0;

    // count number of occurances to be able to correctly size
    // the resulting output char array
    while(-1 != (ix = s.indexOf(v1, ix))) {
      n++;
      ix += v1Len;
    }

    // No occurances at all, just return source string
    if(n == 0) {
      return s;
    }

    // Set up an output char array of correct size
    int     start  = 0;
    int     v2Len  = v2.length();
    char[]  r      = new char[s.length() + n * (v2Len - v1Len)];
    int     rPos   = 0;

    // for each occurance, copy v2 where v1 used to be
    while(-1 != (ix = s.indexOf(v1, start))) {
      while(start < ix) r[rPos++] = s.charAt(start++);
      for(int j = 0; j < v2Len; j++) {
        r[rPos++] = v2.charAt(j);
      }
      start += v1Len;
    }

    // ...and add all remaining chars
    ix = s.length();
    while(start < ix) r[rPos++] = s.charAt(start++);

    // ..ouch. this hurts.
    return new String(r);
  }

}
