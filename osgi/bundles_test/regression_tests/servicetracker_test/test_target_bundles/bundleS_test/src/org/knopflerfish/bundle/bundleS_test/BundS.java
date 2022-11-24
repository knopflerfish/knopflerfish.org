/*
 * Copyright (c) 2004-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.bundleS_test;

import java.io.PrintWriter;
import java.security.AccessControlException;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.knopflerfish.service.bundleS_test.BundleS0;
import org.knopflerfish.service.bundleS_test.BundleS1;
import org.knopflerfish.service.bundleS_test.BundleS2;
import org.knopflerfish.service.bundleS_test.BundleS3;

public class BundS implements BundleS0, BundleS1, BundleS2, BundleS3 {
  BundleContext bc;
  final static String CONSOLE_SERVICE = "org.knopflerfish.service.console.CommandGroup";
  final static String SERVICE = "org.knopflerfish.service.bundleS_test.BundleS";
  final static String SERVICE0 = "org.knopflerfish.service.bundleS_test.BundleS0";
  final static String SERVICE1 = "org.knopflerfish.service.bundleS_test.BundleS1";
  final static String SERVICE2 = "org.knopflerfish.service.bundleS_test.BundleS2";
  final static String SERVICE3 = "org.knopflerfish.service.bundleS_test.BundleS3";

  String[] serviceClasses = { SERVICE0 };

  ServiceRegistration<?>[] servregs = new ServiceRegistration[4];

  ServiceRegistration<?> sreg;

  public BundS (BundleContext bc) {
    this.bc = bc;
    Hashtable<String, Object> props = new Hashtable<>();
    try {
      sreg = bc.registerService(serviceClasses, this, props);
    }

    catch (AccessControlException ace) {
      System.out.println ("Exception " + ace + " in BundleS start"); 
    }
    catch (RuntimeException ru) {
      System.out.println ("Exception " + ru + " in BundleS start"); 
      ru.printStackTrace();
    }
  }

  // An entrypoint where to send a command to register/unregister 
  // a specific service (via reflection from the FrameworkTest ?)
  //
  public void controlService (String service, String operation, String rank) {
    execReg (service, operation, rank);
  }


// Common help routine 

  private boolean execReg (String serviceId, String operation, String ranking) {
    boolean status = false;
    String servicename;
    int offset = Integer.parseInt(serviceId);
    if ( 0 <= offset && offset <= 3) {
      if (operation.equals("register")) {
        if (servregs[offset] == null) {
          servicename = SERVICE + offset;
          String []serviceDescription1 = { servicename };
          Hashtable<String, Object> d1 = new Hashtable<>();
          Integer rank = new Integer(ranking);
          d1.put(Constants.SERVICE_RANKING, rank);
          servregs[offset] = bc.registerService(serviceDescription1, this, d1);
          status = true;
          // System.out.println("BUNDLES: Registered service " + serviceId);
        }
      }
      if (operation.equals("unregister")) {
        if (servregs[offset] != null) {
          ServiceRegistration<?> sr1 = servregs[offset];
          sr1.unregister();
          servregs[offset] = null;
          status = true;
        }
      }
   }
   return status;
 }


 /*
   Utility printout for standard response
  */

  private void printResult (boolean status, PrintWriter out, String s1) {
    if (status) {
      out.println(s1 + ": PASS");
    }
    else {
      out.println(s1 + ": FAIL");
    }
  }


}
