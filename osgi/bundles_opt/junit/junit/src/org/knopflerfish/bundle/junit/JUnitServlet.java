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
import java.net.URL;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;
import org.knopflerfish.service.log.LogRef;
import junit.framework.*;

public class JUnitServlet extends HttpServlet {


  JUnitServlet() {
  }

  public void doGet(HttpServletRequest  request, 
		     HttpServletResponse response) 
    throws ServletException,
	   IOException 
  {
    doPost(request, response);
  }

  public void doPost(HttpServletRequest  request, 
		     HttpServletResponse response) 
    throws ServletException,
	   IOException 
  {
    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

      PrintWriter out = response.getWriter();
      response.setContentType ("text/xml");

      String cmd = request.getParameter("cmd");

      if(cmd == null) {
	cmd = "run";
      }

      handleCommand(request, response, cmd, out);

      out.flush ();
    } catch (RuntimeException e) {
      Activator.log.error("servlet failed", e);
      throw e;
    } catch (IOException e) {
      Activator.log.error("servlet failed", e);
      throw e;
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
  }

  void handleCommand(HttpServletRequest  request, 
		     HttpServletResponse response,
		     String cmd,
		     PrintWriter out) throws 
		       ServletException,
		       IOException {
    out.println("<?xml version=\"1.0\"?>");
    out.println("<junit>");
    try {
      if("run".equals(cmd)) {
	runTest(request.getParameter("id"), out);
      } else {
	throw new IllegalArgumentException("Unknown command='" + cmd + "'");
      }
    } catch (Exception e) {
      out.println(" <fail " + 
		  "  message=\"" + e.getMessage() + "\"" + 
		  "  exception=\"" + e.getClass().getName() + "\">");
      dumpException(e, out);
      out.println(" </fail>");
    }

    out.println("</junit>");
  }

  void runTest(final String id, PrintWriter out) throws Exception {
    out.println(" <testcase id=\"" + id + "\">");

    try {
      Object obj = null;

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
      if(test instanceof TestSuite) {
	suite = (TestSuite)test;
      } else {
	suite = new TestSuite();
	suite.addTest(test);
      }
      
      
      dumpSuite(suite, out, 2);
      
      TestResult tr = new TestResult();
      
      System.out.println("run test on " + suite);
      suite.run(tr);
    
      dumpResult(tr, out);
    } finally {
      out.println(" </testcase>");
    }
  }

  static String indent(int n) {
    StringBuffer sb = new StringBuffer();
    while(n --> 0) {
      sb.append(" ");
    }
    return sb.toString();
  }

  void dumpSuite(TestSuite suite, 
		 PrintWriter out, 
		 int n) throws IOException {
    out.print(indent(n) + "  <suite class = \"" + 
		suite.getClass().getName() + "\"");
    out.println(" name  = \"" + suite.getName() + "\">");

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
	dumpSuite((TestSuite)test, out, n + 1);
      } else {
	out.print(indent(n) + "   <case class = \"" + clazz + "\"");
	out.println(" name  = \"" + name  + "\"/>");
      }
    }
    out.println(indent(n) + "  </suite>");
  }

  void dumpResult(TestResult tr, PrintWriter out) throws IOException {
    out.println("  <testresult wasSuccessful = \"" + tr.wasSuccessful() + "\"");
    out.println("              runCount      = \"" + tr.runCount() + "\"");
    out.println("              failureCount  = \"" + tr.failureCount() + "\"");
    out.println("              errorCount    = \"" + tr.errorCount() + "\"");
    out.println("  >");
    if(tr.failureCount() > 0 ) {
      out.println("   <failures>");
      for(Enumeration e = tr.failures(); e.hasMoreElements(); ) {
	TestFailure tf = (TestFailure)e.nextElement();
	dumpFailure(tf, out);
      }
      out.println("   </failures>");
    }
    if(tr.errorCount() > 0 ) {
      out.println("   <errors>");
      for(Enumeration e = tr.errors(); e.hasMoreElements(); ) {
	
	TestFailure tf = (TestFailure)e.nextElement();
	dumpFailure(tf, out);
      }
      out.println("   </errors>");
    }
    out.println("  </testresult>");
  }

  void dumpFailure(TestFailure tf, PrintWriter out) throws IOException {
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

  String escape(String s) {
    return s == null 
      ? s 
      : (s.replace('<', '[').replace('>', ']'));
  }

  void dumpError(AssertionFailedError af, PrintWriter out) throws IOException {
    out.println("    <error ");
    out.println("      exceptionMessage=\"" + af.getMessage() + "\"");
    out.println("    >");
    dumpException(af, out);
    out.println("    </failure>");
  }

  void dumpException(Throwable t, PrintWriter out) throws IOException {
    out.print("     <exception class=\"" + t.getClass().getName() + "\">");
    out.print("<![CDATA[");
    t.printStackTrace(out);
    out.println("]]></exception>");
  }
}
