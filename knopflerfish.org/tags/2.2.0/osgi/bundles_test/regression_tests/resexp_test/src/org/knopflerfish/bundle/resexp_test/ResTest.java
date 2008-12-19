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

package org.knopflerfish.bundle.resexp_test;

import java.util.*;
import java.lang.reflect.*;
import java.net.URL;
import java.io.*;

import org.osgi.framework.*;

public class ResTest {
  static BundleContext bc = null;

  public void test() {
    String path = "/misc/test.txt";
    
    InputStream in = null;
    String expStr = "This is a text file.";
    String s1 = null;
    URL url = null;
    
    // log("test resource path=" + path);

    if(bc != null) {
      try {
	url = bc.getBundle().getResource(path);
	in = url.openStream();
	s1 = getString(in);
        if (!expStr.equals(s1)) {
	  log("bc.getBundle().getResource");    
	  log(" URL=" + url);
          log(" got '" + s1 + "'" + " expected " + expStr);
        }
	in.close();
      } catch (Exception e) {
	log("bc.getBundle().getResource");    
	log(" URL=" + url);
        log(" got '" + s1 + "'" + " expected " + expStr);
        // e.printStackTrace();
        putStackTrace(e);
      }
    } else {
      log("No BC set");
    }
    s1 = null;
    
    try {
      url = getClass().getResource(path);
      in = url.openStream();
      s1 = getString(in);
      if (!expStr.equals(s1)) {
        log("getClass().getResource");    
        log(" URL=" + url);
        log(" got '" + s1 + "'" + " expected " + expStr);
      }
      in.close();
    } catch (Exception e) {
      log("getClass().getResource");    
      log(" URL=" + url);
      log(" got '" + s1 + "'" + " expected " + expStr);
      // e.printStackTrace();
      putStackTrace(e);
    }
    
    s1 = null;
    try {
      in = getClass().getResourceAsStream(path);
      s1 = getString(in);
      if (!expStr.equals(s1)) { 
        log("getClass().getResourceAsStream:");    
        log(" got '" + s1 + "'" + " expected " + expStr);
      }
      in.close();
    } catch (Exception e) {
      // e.printStackTrace();
      log("getClass().getResourceAsStream:");
      log(" got '" + s1 + "'" + " expected " + expStr);
      putStackTrace(e);
    }
    
    s1 = null;
    try {
      url = getClass().getClassLoader().getResource(path);
      in = url.openStream();
      s1 = getString(in);
      if (!expStr.equals(s1)) { 
        log("ClassLoader.getResource:");    
        log(" URL=" + url);
        log(" got '" + s1 + "'" + " expected " + expStr);
      }
      in.close();
    } catch (Exception e) {
      log("ClassLoader.getResource:");    
      log(" URL=" + url);
      log(" got '" + s1 + "'" + " expected " + expStr);
      // e.printStackTrace();
      putStackTrace(e);
    }
    
    s1 = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream(path);
      s1 = getString(in);
      if (!expStr.equals(s1)) { 
        log("ClassLoader.getResourceAsStream:");    
        log(" got '" + s1 + "'" + " expected " + expStr);
      }
      in.close();
    } catch (Exception e) {
      log("ClassLoader.getResourceAsStream:");    
      log(" got '" + s1 + "'" + " expected " + expStr);
      // e.printStackTrace();
      putStackTrace(e);
    }
    
  }
  
  String getString(InputStream in) throws Exception {
    BufferedInputStream bin = new BufferedInputStream(in);
    
    byte [] buf = new byte[1024];
    int n;
    StringBuffer sb = new StringBuffer();

    while(-1 != (n = bin.read(buf))) {
      String s = new String(buf, 0, n);
      sb.append(s);
    }
    return sb.toString();
  }

  void putStackTrace(Exception e) {
    // e.printStackTrace();
    putValue(" Exception message " + e.getMessage(), 1);
    CharArrayWriter cw = new CharArrayWriter();
    PrintWriter pw = new PrintWriter(cw);
    e.printStackTrace(pw);
    putValue(cw.toString(),2);
    pw.close();
  }
  
  static public void log(String s) {
    putValue(s, 0);
    // System.out.println("resexport: " + s);
  }

  static public void setBC(BundleContext _bc) {
    bc = _bc;
    // log("setBC " + _bc);
  }

  private static void putValue (String methodName, int value) {
    ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.framework_test.FrameworkTest");

    Integer i1 = new Integer(value);
    Method m;
    Class c, parameters[];

    Object obj1 = bc.getService(sr);
    // System.out.println("servref  = "+ sr);
    // System.out.println("object = "+ obj1);

    Object[] arguments = new Object[3];
    arguments[0] = "org.knopflerfish.bundle.resexp_test";
    arguments[1] = methodName;
    arguments[2] = i1;

    c = obj1.getClass();
    parameters = new Class[3];
    parameters[0] = arguments[0].getClass();
    parameters[1] = arguments[1].getClass();
    parameters[2] = arguments[2].getClass();
    try {
      m = c.getMethod("putEvent", parameters);
      m.invoke(obj1, arguments);
    }
    catch (IllegalAccessException ia) {
      System.out.println("Framework test IllegalAccessException" +  ia);
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
    }
  }
}
