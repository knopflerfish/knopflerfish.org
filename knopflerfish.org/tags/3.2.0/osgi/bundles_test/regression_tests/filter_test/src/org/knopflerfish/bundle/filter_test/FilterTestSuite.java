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

package org.knopflerfish.bundle.filter_test;

import java.util.*;
import java.io.*;
import java.math.*;
import org.osgi.framework.*;
import junit.framework.*;

public class FilterTestSuite extends TestSuite {
  BundleContext bc;

  PrintStream out = System.out;
  Bundle      bu  = null;

  public FilterTestSuite(BundleContext bc) {
    super ("FilterTestSuite");

    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new Frame0140a());
    addTest(new Frame0150a());
    addTest(new Cleanup());
  }
 

  // Also install all possible listeners
  class Setup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
    }
  }

  public class Frame0140a extends FWTestCase {
    public String getDescription() {
      return "Test the LDAP filter when getting service refences";
    }

    public void runTest() throws Throwable {
      ServiceRegistration tsr = null;

      try {
	// register a service with known properties
	Hashtable props = new Hashtable();
	props.put("String", "test");
	props.put("SpaceString", "test string with spaces");
	props.put("Boolean", new Boolean(true));
	props.put("Byte", new Byte((byte)16));
	props.put("Character", new Character('X'));
	props.put("Short", new Integer(10));
	props.put("Integer", new Integer(10));
	props.put("Long", new Long(-9876543210L));
	props.put("Float", new Float(1.23));
	props.put("Double", new Double(-3.21));
	//	props.put("BigDecimal", new BigDecimal("9876543210"));
	props.put("BigInteger", new BigInteger("-98765432109876543210"));
	props.put("int_array", new int [] { 10, 20, 30 });
	Vector v = new Vector();
	v.addElement("apa");
	v.addElement("bepa");
	v.addElement("cepa");
	props.put("Vector", v);

	try {
	  tsr = bc.registerService ("java.lang.Object", this, props);
	  /*
	  System.out.println("registered with props=" + props);

	  ServiceReference sr = tsr.getReference();
	  String [] keys = sr.getPropertyKeys();
	  for(int i = 0; i < keys.length; i++) {
	    Object val = sr.getProperty(keys[i]);
	    out.println(keys[i] + " " + 
			(val == null ? "null" : val.getClass().getName()) 
			+ " = " + val);
	  }
	  */
	} catch (Exception e) {
	  fail("Failed to register service in FRAME140A");
	}
	
	// Match these in the registered service. They should
	//  a) be syntactically correct
	//  b) match the registerd service
	String [] matchTests = new String [] {
	// Ignore case tests
	  "(string=test)",
	  "(STRING=test)",
	  "(StRiNg=test)",
	  // Equal tests
	  "(String=test)",
	  "(SpaceString=test string with spaces)",
	  "(Boolean=true)",
	  "(Byte=16)",
	  "(Character=X)",
	  "(Short=10)",
	  "(Integer=10)",
	  "(Long=-9876543210)",
	  "(Float=1.23)",
	  "(Double=-3.21)",
	  //	  "(BigDecimal=9876543210)",
	  "(BigInteger=-98765432109876543210)",
	  "(int_array=10)",
	  "(int_array=20)",
	  "(int_array=30)",
	  "(Vector=apa)",
	  "(Vector=bepa)",
	  "(Vector=cepa)",
	  // Approx Equal tests
	  "(String~=test)",
	  "(Boolean~=true)",
	  "(Byte~=16)",
	  "(Character~=X)",
	  "(Short~=10)",
	  "(Integer~=10)",
	  "(Long~=-9876543210)",
	  "(Float~=1.23)",
	  "(Double~=-3.21)",
	  //	  "(BigDecimal~=9876543210)",
	  "(BigInteger~=-98765432109876543210)",
	  // Less Equal tests
	  "(String<=test)",
	  "(Byte<=16)",
	  "(Character<=X)",
	  "(Short<=10)",
	  "(Integer<=10)",
	  "(Long<=-9876543210)",
	  "(Float<=1.23)",
	  "(Double<=-3.21)",
	  //	  "(BigDecimal<=9876543210)",
	  "(BigInteger<=-98765432109876543210)",
	  "(String<=u)",
	  "(Byte<=21)",
	  "(Character<=Y)",
	  "(Short<=12)",
	  "(Integer<=100)",
	  "(Long<=9876543210)",
	  "(Float<=2)",
	  "(Double<=-1)",
	  //	  "(BigDecimal<=19876543210)",
	  "(BigInteger<=-98765432109876543200)",
	  // Greater Equal tests
	  "(String>=test)",
	  "(Byte>=16)",
	  "(Character>=X)",
	  "(Short>=10)",
	  "(Integer>=10)",
	  "(Long>=-9876543210)",
	  "(Float>=1.23)",
	  "(Double>=-3.21)",
	  //	  "(BigDecimal>=9876543210)",
	  "(BigInteger>=-98765432109876543210)",
	  "(String>=sestur)",
	  "(Byte>=1)",
	  "(Character>=A)",
	  "(Short>=-20)",
	  "(Integer>=0)",
	  "(Long>=-19876543210)",
	  "(Float>=-21.23)",
	  "(Double>=-3.211)",
	  //	  "(BigDecimal>=-98763091271309543210)",
	  "(BigInteger>=-9871290865432109876543210)",
	  // Present tests
	  "(String=*)",
	  "(Boolean=*)",
	  "(Integer=*)",
	  //	  "(BigDecimal=*)",
	  "(int_array=*)",
	  "(Vector=*)",
	  // Not tests
	  "(!(No=*))",
	  // And tests
	  "(&(String=*))",
	  "(&(String=*)(Integer>=0))",
	  "(&(String=*)(Integer>=0)(Boolean=true))",
	  // Or tests
	  "(|(String=*))",
	  "(|(String=*)(Integer>=0))",
	  "(|(Noway=*)(Integer>=0))",
	  "(|(Integer>=0)(Noway=*))",
	  "(|(Noway=*)(Noway=*)(String=*))",
	  // Substring tests
	  "(String=t*)",
	  "(String=*t)",
	  "(SpaceString=test string * spaces)",
	  "(SpaceString=* string * spaces)",
	  "(SpaceString=* *)",
	  // Multiple tests
	  "(&(|(Noway=*)(!(Noway=*)))(String=*))"
	};

	// do the matching
	for (int i = 0; i < matchTests.length; i++) {
	  ServiceReference [] sr = null;
	  try {
	    out.println("test LDAP matchTest[" + i + "]=" + matchTests[i]);
	    sr = bc.getServiceReferences("java.lang.Object", matchTests[i]);
	  } catch (InvalidSyntaxException ise) {
	    ise.printStackTrace();
	    fail("Failed LDAP filter: \"" + matchTests[i] + "\" in FRAME140A, Exception: " + ise);
	  }
	  boolean bFound = false;
	  if (sr != null) {
	    for (int j = 0; j < sr.length; j++) {
	      if (sr[j].getBundle() == bu) {
		bFound = true;
	      }
	    }
	  }
	  if(!bFound) {
	    out.println("Failed to get service match on \"" + matchTests[i] + "\", sr=" + sr + ", in FRAME140A");
	    fail("Failed to get service match on \"" + matchTests[i] + "\", sr=" + sr + ", in FRAME140A");
	  }
	}
	
	// NoMatch tests. None of these should match
	String [] noMatchTests = new String [] {
	  // Equal tests
	  "(String=notest)",
	  "(SpaceString=teststringwithspaces)",
	  "(Boolean=false)",
	  "(Byte=1)",
	  "(Character=Y)",
	  "(Short=0)",
	  "(Integer=-10)",
	  "(Long=-90)",
	  "(Float=1.231)",
	  "(Double=3.21)",
	  //	  "(BigDecimal=10)",
	  "(BigInteger=-8765432109876543210)",
	  "(int_array=15)",
	  "(Vector=depa)",
	  // Approx Equal tests, Impl. dependent?!
	  
	  // Less Equal tests
	  "(String<=rest)",
	  "(Byte<=15)",
	  "(Character<=W)",
	  "(Short<=9)",
	  "(Integer<=9)",
	  "(Long<=-9876543211)",
	  "(Float<=1.2)",
	  "(Double<=-3.2100000001)",
	  //	  "(BigDecimal<=9876543209)",
	  "(BigInteger<=-98765432109876543211)",
	  // Greater Equal tests
	  "(String>=tests)",
	  //triggering an exception on purpose
	  "(Byte>=160)",
	  "(Character>=Y)",
	  "(Short>=11)",
	  "(Integer>=11)",
	  "(Long>=0)",
	  "(Float>=3)",
	  "(Double>=0.21)",
	  //	  "(BigDecimal>=9876543211)",
	  "(BigInteger>=-98765432109876543209)",
	  // Present tests
	  "(No=*)",
	  // Not tests
	  "(!(String=*))",
	  // And tests
	  "(&(No=*))",
	  "(&(No=*)(Integer>=0))",
	  "(&(String=*)(Integer<=0))",
	  "(&(String=*)(Integer>=0)(Boolean=false))",
	  // Or tests
	  "(|(No=*))",
	  "(|(No=*)(Integer<=0))",
	  "(|(Noway=*)(No=*)(Nono=*))",
	  // Substring tests
	  "(String=e*)",
	  "(String=*s)",
	  "(SpaceString=test string * )",
	  "(SpaceString=* String * spaces)",
	  "(SpaceString=*X*)",
	  // Multiple tests
	  "(&(|(Noway=*)(!(String=*)))(String=*))"
	};
	for (int i = 0; i < noMatchTests.length; i++) {
	  ServiceReference [] sr = null;
	  try {
	    sr = bc.getServiceReferences("java.lang.Object", noMatchTests[i]);
	  } catch (InvalidSyntaxException ise) {
	    ise.printStackTrace();
	    fail("Failed LDAP filter: \"" + noMatchTests[i] + "\" in FRAME140A, Exception: " + ise);
	  }
	  if (sr != null) {
	    for (int j = 0; j < sr.length; j++) {
	      if (sr[j].getBundle() == bu) {
		fail("Failed LDAP filter test: \"" + noMatchTests[i] + "\" in FRAME140A, should not match");
	      }
	    }
	  }
	}
	
	out.println("### framework test bundle :FRAME140A:PASS");

      } finally {
	try {
	  tsr.unregister();
	} catch (Exception ignored) {}
      }
    }
  }


  public class Frame0150a extends FWTestCase {
    public String getDescription() {
      return "Test the Framework Filter class";
    }

    public void runTest() throws Throwable {
      ServiceRegistration tsr = null;

      try {

	Hashtable props1 = new Hashtable();
	props1.put("String", "123");
	Hashtable props2 = new Hashtable();
	props2.put("sTRING", "123");
	Hashtable props3 = new Hashtable();
	Hashtable props4 = new Hashtable();
	props4.put("String", "String");
	Hashtable props5 = new Hashtable();
	props5.put("STRING", "String");

	try {
	  tsr = bc.registerService ("java.lang.Object", this, props1);
	} catch (Exception e) {
	  fail("Failed to register service in FRAME150A");
	}
	boolean shouldFail = false;
	try {
	  Filter f1 = bc.createFilter("(  String=123)");
	  Filter f2 = bc.createFilter("(String	=123)");
	  Filter f3 = bc.createFilter("(String=*)");
	  Filter f4 = bc.createFilter("(String=String)");
	  
	  if (!f1.match(tsr.getReference())) {
	    fail("Failed to match against service reference in FRAME150A");
	  }
	  if (!f2.match(props2)) {
	    fail("Failed to match against dictionary in FRAME150A");
	  }
	  if (f3.match(props3)) {
	    fail("Illegal matched filter against empty dictionary in FRAME150A");
	  }
	  if (!f4.matchCase(props4)) {
		  fail("Failed to match against dictionary in FRAME150A");
	  }
	  if (f4.matchCase(props5)) {
		  fail("Illegal matched filter against dictionary in FRAME150A");
	  }
	  if (!f1.equals(f2)) {
	    fail("Failed to compare to equal filters in FRAME150A");
	  }
	  if (f3.equals(f2)) {
	    fail("Failed to compare to different filters in FRAME150A");
	  }
	  if (f1.hashCode() != f2.hashCode()) {
	    fail("Failed to compare hashCode for equal filters in FRAME150A");
	  }
	  if (!"(string=123)".equalsIgnoreCase(f1.toString()) ||
	      !"(string=123)".equalsIgnoreCase(f2.toString())) {
	    fail("Filter.toString() did not produce expected result in FRAME150A");
	  }
	  try {
	    props1.putAll(props2);
	    f1.match(props1);
	    fail("Filter.match() did not throw expected exception in FRAME150A");
	  } catch (IllegalArgumentException iae) {
	    // ok
	  }
	  shouldFail = true;
	  bc.createFilter("(what?)");
	  fail("BundleContext.createFilter() did not throw expected exception in FRAME150A");
	} catch (InvalidSyntaxException ise) {
	  if (!shouldFail) {
	    ise.printStackTrace(out);
	    fail("BundleContext.createFilter() throw unexpected exception in FRAME150A");
	  }
	}
	
	out.println("### framework test bundle :FRAME150A:PASS");
      } finally {
	try {
	  tsr.unregister();
	} catch (Exception ignored) { }
      }
    }
  }
}
