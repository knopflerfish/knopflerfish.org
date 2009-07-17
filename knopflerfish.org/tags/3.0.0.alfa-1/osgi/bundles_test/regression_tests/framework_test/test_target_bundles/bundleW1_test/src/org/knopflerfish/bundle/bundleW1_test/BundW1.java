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

package org.knopflerfish.bundle.bundleW1_test;

import java.util.*;
import java.lang.reflect.*;
import java.security.*;
import org.knopflerfish.service.bundleW1_test.*;
import org.osgi.framework.*;

/*
   This bundle is used to check parts of the functionality of the framework
   It registers itself and its service and tries to 
   get a log service, which should fail as it does not have that permission
*/

public class BundW1 implements BundleW1 {
  BundleContext bc;
  String base_url;

  String []serviceDescription = {"org.knopflerfish.service.bundleW1_test.BundleW1"};
  ServiceRegistration sreg;
  String LogServiceName = "org.osgi.service.log.LogService";
  ServiceReference srLog = null;
  Object log = null;
  ServiceReference srLog1 = null;
  Object logserv = null;

  public BundW1 (BundleContext bc) {
    this.bc = bc;
    try {
      sreg = bc.registerService(serviceDescription, this, null);
    }

    catch (AccessControlException ace) {
      System.out.println ("Exception " + ace + " in BundleW1 start"); 
    }
    catch (RuntimeException ru) {
      System.out.println ("Exception " + ru + " in BundleW1 start"); 
      ru.printStackTrace();
    }

    base_url = getUrl();
  }

  // An entrypoint where to send a command to load a specified bundle
  // which then might cause an exception depending on package 
  // import priviliges of this bundle
  // 
  public void tryPackage (String bundle) {
    // System.out.println ("tryPackage " + bundle);
    Bundle buA = null;
    try {
      buA = bc.installBundle (base_url +bundle+".jar");
      buA.start();
      putValue("tryPackage succeded with bundle " + bundle ,3);
    }
    catch (BundleException bexcA) {
      putValue("tryPackage got BundleException with bundle " + bundle ,3);
      putValue("tryPackage got nested Exception " + bexcA.getNestedException().toString() + " " + bundle ,3);
      // System.out.println("### Frame test bundleW1 "+ bexcA );
      // System.out.println("### Frame test bundleW1 "+ bexcA.getNestedException() );
    }
    catch (SecurityException secA) {
      putValue("tryPackage got SecurityException with bundle " + bundle ,3);
      // System.out.println("### Frame test bundleW1 "+ secA );
    }
  }

  private void putValue (String methodName, int value) {
    ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.framework_test.FrameworkTest");

    Integer i1 = new Integer(value);
    Method m;
    Class c, parameters[];

    Object obj1 = bc.getService(sr);

    Object[] arguments = new Object[3];
    arguments[0] = "org.knopflerfish.bundle.bundleW1_test.BundW1";
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
      System.out.println("Framework test IllegaleAccessException" +  ia);
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

  // get the base url from the main test program
  private String getUrl () {
    ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.framework_test.FrameworkTest");
    String url = null;

    Method m;
    Class c, parameters[];

    Object devA = bc.getService(sr);

    Object[] arguments = new Object[0];

    c = devA.getClass();
    parameters = new Class[0];
    try {
      m = c.getMethod("getBaseURL", parameters);
      url = (String) m.invoke(devA, arguments);
      // System.out.println("URL via reflection =" + url);
    }
    catch (IllegalAccessException ia) {
      System.out.println("Device test IllegaleAccessException" +  ia);
    }
    catch (InvocationTargetException ita) {
      System.out.println("Device test InvocationTargetException" +  ita);
      System.out.println("Device test nested InvocationTargetException" +  ita.getTargetException() );
    }
    catch (NoSuchMethodException nme) {
      System.out.println("Device test NoSuchMethodException " +  nme);
    }
    catch (Throwable thr) {
      System.out.println("Unexpected " +  thr);
    }
    return url;
  }

}
