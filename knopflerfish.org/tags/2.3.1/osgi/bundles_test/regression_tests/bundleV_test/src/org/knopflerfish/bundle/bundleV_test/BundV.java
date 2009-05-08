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

package org.knopflerfish.bundle.bundleV_test;

import java.util.*;
import java.lang.reflect.*;
import java.security.*;
import org.knopflerfish.service.bundleV_test.*;
import org.osgi.framework.*;

/*
   This bundle is used to check parts of the functionality of the framework
   It registers itself and its service and tries to 
   get a log service, which should fail as it does not have that permission
*/

public class BundV implements BundleV {
  BundleContext bc;
  String []serviceDescription = {"org.knopflerfish.service.bundleV_test.BundleV"};
  ServiceRegistration sreg;
  String LogServiceName = "org.osgi.service.log.LogService";
  ServiceReference srLog = null;
  Object log = null;
  ServiceReference srLog1 = null;
  Object logserv = null;

  public BundV (BundleContext bc) {
    // boolean cat = false;
    this.bc = bc;
    try {
      sreg = bc.registerService(serviceDescription, this, null);
    }

    catch (AccessControlException ace) {
      System.out.println ("Exception " + ace + " in BundleV start"); 
    }
    catch (RuntimeException ru) {
      System.out.println ("Exception " + ru + " in BundleV start"); 
      ru.printStackTrace();
    }

    // Now try to get a service reference to the log service, which should 
    // result in a null as we aint allowed to get it.

    try {
      srLog = bc.getServiceReference( LogServiceName);
    }
/* 
    catch (SecurityException sec) {
      System.out.println ("SecurityException " + sec + " in BundleV start"); 
      putValue("SecurityException",1);
      cat = true;
    }
    catch (IllegalStateException ise) {
      System.out.println ("IllegalStateException " + ise + " in BundleV start"); 
      putValue("IllegalStateException",1);
      cat = true;
    }
*/
    catch (Exception ex) {
      putValue("constructor, unexpected Exception",1);
      // System.out.println ("Exception " + ex + " in BundleV start"); 
      // cat = true;
    }

    if (srLog != null) {
       putValue("constructor, Service reference != null",0);
    }
    else {
       putValue("constructor, Service reference == null",0);
    }  

  }

  // An entrypoint where to send a valid service reference to the Log
  // which when used to get the service should generate an exception
  // 
  public void tryService (ServiceReference serv) {
    // System.out.println ("tryService" + serv);
    srLog1 = serv;
    try {
      Object log = bc.getService(srLog1);
      System.out.println ("in bundleV tryService log = " + log);
    }
    catch (SecurityException sec) {
      putValue("tryService SecurityException",1);
      // System.out.println ("SecurityException " + sec + " in BundleV tryService");
    }
    catch (IllegalStateException ise) {
      putValue("tryService IllegalStateException",2);
      // System.out.println ("IllegalStateException " + ise + " in BundleV tryService");
    }
    catch (Exception ex) {
      putValue("tryService Exception",3);
      // System.out.println ("Exception " + ex + " in BundleV tryService");
    }
  }

  private void putValue (String methodName, int value) {
    ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.framework_test.FrameworkTest");

    Integer i1 = new Integer(value);
    Method m;
    Class c, parameters[];

    Object obj1 = bc.getService(sr);
    // System.out.println("servref  = "+ sr);
    // System.out.println("object = "+ obj1);

    Object[] arguments = new Object[3];
    arguments[0] = "org.knopflerfish.bundle.bundleV_test.BundV";
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
}
