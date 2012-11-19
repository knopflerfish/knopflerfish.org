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

package org.knopflerfish.bundle.bundleP_test;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import org.knopflerfish.service.bundleP_test.*;
import org.osgi.framework.*;

/*
   This bundle is used to check parts of the functionality of the framework
   It registers itself and its service.
   It reports if succeded to register itself.
*/

public class BundP implements BundleP {
  BundleContext bc;
  String []serviceDescription = {"org.knopflerfish.service.bundleP_test.BundleP"};
  ServiceRegistration sreg;

  public BundP (BundleContext bc) {
    this.bc = bc;
    sreg = bc.registerService(serviceDescription, this, null);
   
    if (sreg != null) {
       putValue("constructor, bundleP_test Service reference != null",0);
    }
    else {
       putValue("constructor, bundleP_test Service reference == null",0);
    }  
    // try to get a file handle and try if the file exist
    File f1 = new File("/tmp/testfile4");
    boolean exi = false;
    if (f1 != null) {
       exi = f1.exists();
       putValue("constructor, bundleP_test File reference: " + exi ,1);
    } else {
       putValue("constructor, bundleP_test File reference: " + exi ,1);
    }
  }

  public void unreg() {
    
  }

  private void putValue (String methodName, int value) {
    ServiceReference sr = bc.getServiceReference("org.knopflerfish.service.framework_test.FrameworkTest");

    Integer i1 = new Integer(value);
    Method m;
    Class c, parameters[];

    Object obj1 = bc.getService(sr);

    Object[] arguments = new Object[3];
    arguments[0] = "org.knopflerfish.bundle.bundleP_test.BundP";
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
