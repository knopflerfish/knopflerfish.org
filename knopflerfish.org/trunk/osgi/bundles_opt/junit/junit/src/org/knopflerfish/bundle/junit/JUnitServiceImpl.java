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

package org.knopflerfish.bundle.junit;

import java.util.*;
import java.io.*;

import org.knopflerfish.service.junit.*;
import org.osgi.framework.*;
import junit.framework.*;
import java.lang.reflect.*;

public class JUnitServiceImpl implements JUnitService {

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

  public void runTestXML(PrintWriter out, TestSuite suite) throws IOException {


    out.println("<?xml version=\"1.0\"?>");
    out.println("<?xml-stylesheet type=\"text/xsl\" href=\"junit_style.xsl\"?>");
    out.print("<junit");
    out.print(" date=\"" + (new Date()) + "\"");
    out.println(">");

    dumpSystemProps(out);

    try {
      runTestCase(out, suite);
    } catch (Exception e) {
      out.println(" <fail " + 
		  "  message=\"" + e.getMessage() + "\"" + 
		  "  exception=\"" + e.getClass().getName() + "\">");
      toXMLException(e, out);
      out.println(" </fail>");
    }

    out.println("</junit>");
  }

  void dumpSystemProps(PrintWriter out) throws IOException {
    out.println("<properties name=\"System.properties\">");
    try {
      Properties props = System.getProperties();
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

  protected void runTestCase(PrintWriter out, 
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
    
    try {

      System.out.println("run test on " + suite);

      TestResult tr = new TestResult();

      long start = System.currentTimeMillis();
      suite.run(tr);
      long stop  = System.currentTimeMillis();

      toXMLSuite(suite, tr, stop - start, out, 2);
      toXMLResult(tr, out);
    } finally {
      out.println(" </testcase>");
    }
  }

  public TestSuite getTestSuite(final String id, 
				final String subid) {
    Object obj = null;
    
    try {
      ServiceReference[] srl = 
	Activator.bc.getServiceReferences(null, "(service.pid=" + id + ")");

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
    StringBuffer sb = new StringBuffer();
    while(n --> 0) {
      sb.append(" ");
    }
    return sb.toString();
  }

  protected void toXMLSuite(TestSuite suite, 
			    TestResult tr,
			    long time,
			    PrintWriter out, 
			    int n) throws IOException {
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
      if(name == null) {
	name = clazz;
      }
      if(test instanceof TestSuite) {
	toXMLSuite((TestSuite)test, tr, 0, out, n + 1);
      } else {
	out.print(indent(n) + "   <case class = \"" + clazz + "\"");
	out.print(" name  = \"" + name + "\"");
	out.print(" status = \"" + getTestCaseStatus(tr, test) + "\"");
	out.println(">");

	String desc   = getBeanString(test, "getDescription", "");
	out.print("  <description>");
	out.print("<![CDATA[" + desc + "]]>");
	out.println("</description>");
	
	out.println("</case>");
      }
    }
    out.println(indent(n) + "  </suite>");
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
    return 
      s
      .replace('<', '[').replace('>', ']')
      .replace('\"', '\'');
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
      val = (String)m.invoke(target, null);
    } catch (Exception e) {
      return defVal;
    }
    return val;
  }

}
