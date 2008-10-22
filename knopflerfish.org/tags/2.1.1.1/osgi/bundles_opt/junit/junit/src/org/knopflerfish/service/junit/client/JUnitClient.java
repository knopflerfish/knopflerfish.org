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

package org.knopflerfish.service.junit.client;

import junit.framework.*;
import java.util.*;
import java.net.*;
import java.io.*;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import java.lang.reflect.*;

public class JUnitClient extends TestSuite {

  static final String TAG_JUNIT      = "junit";
  static final String TAG_TESTCASE   = "testcase";
  static final String TAG_TESTRESULT = "testresult";
  static final String TAG_FAILURES   = "failures";
  static final String TAG_ERRORS     = "errors";
  static final String TAG_FAILURE    = "failure";
  static final String TAG_TRACE      = "trace";
  static final String TAG_CASE       = "case";
  static final String TAG_SUITE      = "suite";

  static final String ATTR_EXCEPTIONMESSAGE    = "exceptionMessage";
  static final String ATTR_FAILEDTESTCASENAME  = "failedTestCaseName";
  static final String ATTR_FAILEDTESTCASECLASS = "failedTestCaseClass";

  static final String ATTR_NAME          = "name";
  static final String ATTR_CLASS         = "class";


  URL targetURL = null;

  JUnitClient() {
  }

  /**
   * Construct a test suite from the URL specified by the
   * system property <code>suite.url</code>
   *
   * <p>
   * If loading fails in any way, wrap this in a failed test
   * case, added to the suite.
   * </p>
   */
  public static Test suite() throws Exception {

    URL url = new URL(System.getProperty("suite.url"));

    JUnitClient suite = new JUnitClient();
    suite.load(url);

    return suite;
  }

  /**
   * Read tests and results from the specified URL.
   */
  public void load(URL url) {
    this.targetURL = url;

    InputStream in = null;

    try {
      in               = targetURL.openStream();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
      DocumentBuilder        builder = factory.newDocumentBuilder();

      Document doc = builder.parse(in);

      parseDoc(doc.getDocumentElement());
    } catch (Exception e) {

      // If document loading failed by any reason, present this
      // as a failed test case.

      final StringWriter w = new StringWriter();
      e.printStackTrace(new PrintWriter(w));
      
      // This test will always fail with a trace from the
      // caught load exception.
      Test test = new TestCase(e.getClass().getName()) {
	  public void runTest() {
	    throw new RemoteAssertionFailedError(getName(),
						 "targetURL=" + targetURL + 
						 "\n" + 
						 w.toString());
	  }
	};

      // ...a nice way of logging failures
      addTest(test);

    } finally {
      try { in.close(); } catch (Exception ignored) { }
    }
  }


  void parseDoc(Element el) {
    if(TAG_JUNIT.equals(el.getTagName())) {
      parseJUnit(el);
    } else {
      NodeList nl = el.getChildNodes();
      for(int i = 0; i < nl.getLength(); i++) {
	Node child = nl.item(i);
	if(child instanceof Element) {
	  Element childEl = (Element)child;
	  if(TAG_JUNIT.equals(childEl.getTagName())) {
	    parseJUnit(childEl);
	  }
	}
      }
    }
  }

  void parseJUnit(Element el) {
    assertTagName(el, TAG_JUNIT);

    NodeList nl = el.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if(child instanceof Element) {
	Element childEl = (Element)child;
	if(TAG_TESTCASE.equals(childEl.getTagName())) {
	  parseTestCase(childEl);
	}
      }
    }

  }

  void parseTestCase(Element el) {
    assertTagName(el, TAG_TESTCASE);

    NodeList nl = el.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if(child instanceof Element) {
	Element childEl = (Element)child;
	if(TAG_TESTRESULT.equals(childEl.getTagName())) {
	  parseTestResult(childEl);
	}
	if(TAG_SUITE.equals(childEl.getTagName())) {
	  parseSuite(childEl);
	}
      }
    }

  }

 void parseSuite(Element el) {
    assertTagName(el, TAG_SUITE);

    NodeList nl = el.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if(child instanceof Element) {
	Element childEl = (Element)child;
	if(TAG_SUITE.equals(childEl.getTagName())) {
	  parseSuite(childEl);
	}
	if(TAG_CASE.equals(childEl.getTagName())) {
	  
	  RemoteTest test = new RemoteTest(childEl.getAttribute(ATTR_CLASS),
					   childEl.getAttribute(ATTR_NAME),
					   "", 
					   "");
	  addTest(test);
	}
      }
    }
  }

  void parseTestResult(Element el) {
    assertTagName(el, TAG_TESTRESULT);


    NodeList nl = el.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if(child instanceof Element) {
	Element childEl = (Element)child;
	if(TAG_FAILURES.equals(childEl.getTagName())) {
	  parseFailures(childEl);
	} else if(TAG_ERRORS.equals(childEl.getTagName())) {
	  parseErrors(childEl);
	}
      }
    }
  }
  
  void parseFailures(Element el) {
    assertTagName(el, TAG_FAILURES);

    NodeList nl = el.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if(child instanceof Element) {
	Element childEl = (Element)child;
	if(TAG_FAILURE.equals(childEl.getTagName())) {
	  parseFailure(childEl, false);
	}
      }
    }
  }

  void parseErrors(Element el) {
    assertTagName(el, TAG_ERRORS);

    NodeList nl = el.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if(child instanceof Element) {
	Element childEl = (Element)child;
	if(TAG_FAILURE.equals(childEl.getTagName())) {
	  parseFailure(childEl, true);
	}
      }
    }
  }

  void parseFailure(Element el, boolean bError) {
    assertTagName(el, TAG_FAILURE);

    String exceptionMessage    = el.getAttribute(ATTR_EXCEPTIONMESSAGE);
    String failedTestCaseName  = el.getAttribute(ATTR_FAILEDTESTCASENAME);
    String failedTestCaseClass = el.getAttribute(ATTR_FAILEDTESTCASECLASS);
    String trace               = "";


    NodeList nl = el.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++) {
      Node child = nl.item(i);
      if(child instanceof Element) {
	Element childEl = (Element)child;
	if(TAG_TRACE.equals(childEl.getTagName())) {
	  Text text = (Text)childEl.getFirstChild();
	  trace = text.getData();
	}
      }
    }


    RemoteTest test = null;
    for(int i = 0; i < testCount(); i++) {
      RemoteTest rt = (RemoteTest)testAt(i);
      if(rt.name.equals(failedTestCaseName) && 
	 rt.className.equals(failedTestCaseClass)) {
	test = rt;
      }
    }

    if(test == null) {
      throw new IllegalArgumentException("Unknown test " + failedTestCaseClass + ", " + failedTestCaseName);
    }

    test.exceptionMessage = exceptionMessage;
    test.trace            = trace;
    test.bFailure         = !bError;
    test.bError           = bError;

    if(bError) {
      test.error        = new RemoteError(exceptionMessage, trace);
    } else {
      test.error        = new RemoteAssertionFailedError(exceptionMessage,
							 trace);
    }
  }


  static void assertTagName(Element el, String name) {
    if(!name.equals(el.getTagName())) {
      throw new IllegalArgumentException("expected '" + name + "', found " + 
					 " '" + el.getTagName() + "'");
    }
  }


  static int getInt(Element el, String attr, int def) {
    String s = el.getAttribute(attr);
    int v = def;
    if(s != null) {
      try {
	v = Integer.parseInt(s);
      } catch (Exception e) {
      }
    }
    return v;
  }

  public String toString() {
    return "JUnitClient[" + targetURL + "]";
  }
}

class RemoteTest extends TestCase {
  String               name;
  String               className;
  String               exceptionMessage;
  String               trace;
  boolean              bFailure = false;
  boolean              bError   = false;
  Error                error    = null; 

  RemoteTest(String className, 
	     String name, 
	     String exceptionMessage,
	     String trace) {
    super(name);
    this.name             = name;
    this.className        = className;
    this.exceptionMessage = exceptionMessage;
    this.trace            = trace;
  }
  
  public String toString() {
    return name + "(" + className + ")";
  }


  public int hashCode() {
    return toString().hashCode();
  }
  
  public boolean equals(Object other) {
    if(other == null || !(other instanceof RemoteTest)) {
      return false;
    }
    return toString().equals(other.toString());
  }
  
  public int countTestCases() {
    return 1;
  }

  public void runTest() {
    if(bFailure || bError) {
      throw error;
    }
  }
}

class RemoteAssertionFailedError extends AssertionFailedError {
  String trace;
  
  RemoteAssertionFailedError(String name, String trace) {
    super(name);
    this.trace = trace;
  }
  
  public void printStackTrace(PrintStream s) {
    s.print(trace);
  }

  public void printStackTrace(PrintWriter s) {
    s.print(trace);
  }  
}

class RemoteError extends Error {
  String trace;
  
  RemoteError(String name, String trace) {
    super(name);
    this.trace = trace;
  }
  
  public void printStackTrace(PrintStream s) {
    s.print(trace);
  }

  public void printStackTrace(PrintWriter s) {
    s.print(trace);
  }  
}
