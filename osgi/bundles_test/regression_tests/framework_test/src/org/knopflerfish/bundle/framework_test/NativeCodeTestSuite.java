/*
 * Copyright (c) 2004-2014, KNOPFLERFISH project
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

package org.knopflerfish.bundle.framework_test;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class NativeCodeTestSuite extends TestSuite {
  BundleContext bc;
  Bundle        buN;

  String     test_url_base;

  PrintStream out = System.out;

  public NativeCodeTestSuite(BundleContext bc) {
    super ("NativeCodeTestSuite");

    this.bc = bc;
    test_url_base = "bundle://" + bc.getBundle().getBundleId() + "/";

    addTest(new Setup());
    // addTest(new Frame0135a());
    addTest(new Frame0137a());
    addTest(new Frame0138a());
    addTest(new Frame0139a());
    addTest(new Cleanup());
  }


  // Also install all possible listeners
  public class Setup extends FWTestCase {

    public String getDescription() {
      return "This does some error handling tests of bundles with native code";
    }

    public void runTest() throws Throwable {
      out.println("### framework test bundle :SETUP:PASS");
    }
  }

  class Cleanup extends FWTestCase {
    public void runTest() throws Throwable {
      Bundle[] bundles = new Bundle[] {
          buN ,
      };
      for(int i = 0; i < bundles.length; i++) {
        try {  bundles[i].uninstall();  } 
        catch (Exception ignored) { }      
      }

      buN = null;
    }
  }


  // 27. Install testbundle N (with native code )
  //     and call its test method, which should return Hello world
  //     The name of the test bundle .jar file to load is made from
  //     a concatenation of the strings
  //     bundleN-<processor>-<osname>[-<osversion>][-<language>]_test.jar

  class Frame0135a extends FWTestCase {
    public void runTest() throws Throwable {
      Dictionary opts = new Hashtable();
      String processor = (String) opts.get("processor");
      String osname    = (String) opts.get("osname");
      String osversion = (String) opts.get("osversion");
      String language  = (String) opts.get("language");

      // at present only the processor and osname are used
      StringBuilder b1 = new StringBuilder(test_url_base+"bundleN-"+ processor + "-" + osname);
      //
      if (osversion != null) {
        b1.append("-").append(osversion);
      }
      if (language != null) {
        b1.append("-").append(language);
      }
      b1.append("_test_all-1.0.0.jar");
      String jarName = b1.toString();

      // out.println("NATIVE " + jarName);
      // out.flush();
      boolean teststatus = true;

      try {
        buN = Util.installBundle (bc, jarName);
        buN.start();
      }
      catch (BundleException bex) {
        out.println("framework test bundle "+ bex +" :FRAME135:FAIL");
        Throwable tx = bex.getNestedException();
        if (tx != null) {
          out.println("framework test bundle, nested exception "+ tx +" :FRAME135:FAIL");
        }
        teststatus = false;
      }
      catch (SecurityException sec) {
        out.println("framework test bundle "+ sec +" :FRAME135A:FAIL");
        teststatus = false;
      }

      if (teststatus == true) {
        // Get the service reference and a service from the native bundle
        ServiceReference srnative = bc.getServiceReference("org.knopflerfish.service.nativetest.NativeTest");
        if (srnative != null) {
          @SuppressWarnings("unchecked")
          Object o = bc.getService(srnative);
          if (o != null) { 
            // now for some reflection exercises
            String expectedString = "Hello world";
            String nativeString = null;

            Method m;
            Class<? extends Object> c;
            Class parameters[];

            // out.println("servref  = "+ sr);
            // out.println("object = "+ obj1);

            Object[] arguments = new Object[0];
            parameters = new Class[0];
            c = o.getClass();

            try {
              m = c.getMethod("getString", parameters);
              nativeString = (String) m.invoke(o, arguments);
              if (!expectedString.equals(nativeString)) {
                out.println("Frame test native bundle method failed, expected: " + expectedString + " got: " + nativeString + ":FRAME135A:FAIL");
                teststatus = false;
              }
            }
            catch (IllegalAccessException ia) {
              out.println("Frame test IllegaleAccessException" +  ia + ":FRAME135A:FAIL");
              teststatus = false;
            }
            catch (InvocationTargetException ita) {
              out.println("Frame test InvocationTargetException" +  ita);
              out.println("Frame test nested InvocationTargetException" +  ita.getTargetException()  + ":FRAME135A:FAIL");
              teststatus = false;
            }
            catch (NoSuchMethodException nme) {
              out.println("Frame test NoSuchMethodException" +  nme + ":FRAME135A:FAIL");
              teststatus = false;
            }
          } else {
            out.println("framework test bundle, failed to get service for org.knopflerfish.service.nativetest.NativeTest :FRAME135A:FAIL");
            teststatus = false;
          }
        } else {
          out.println("framework test bundle, failed to get service reference for org.knopflerfish.service.nativetest.NativeTest :FRAME135A:FAIL");
          teststatus = false;
        }
      }

      if (teststatus == true && buN.getState() == Bundle.ACTIVE) {
        out.println("### framework test bundle :FRAME135A:PASS");
      }
      else {
        out.println("### framework test bundle :FRAME135A:FAIL");
      }
    }
  }


  // Install testbundle N1 & N2 (with faulty native code headers)
  //

  class Frame0137a extends FWTestCase {
    public void runTest() throws Throwable {

      try {
        buN = Util.installBundle (bc, "bundleN1_test-1.0.0.jar");
        buN.start();
        fail("framework faulty native test bundle N1 should not resolve :FRAME137:FAIL");
      } catch (BundleException bex) {
        // Expected bundle exception
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle N1, unexpected "+ e +" :FRAME137A:FAIL");
      }

      try {
        buN = Util.installBundle (bc, "bundleN2_test-1.0.0.jar");
        buN.start();
        fail("framework faulty native test bundle N2 should not resolve :FRAME137:FAIL");
      } catch (BundleException bex) {
        // Expected bundle exception
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle N2, unexpected "+ e +" :FRAME137A:FAIL");
      }

      out.println("### framework test bundle :FRAME137A:PASS");
    }
  }


  // Install testbundle N3 and fragments N4+N5 and check that only N5 fragment is attached.
  //

  class Frame0138a extends FWTestCase {
    public void runTest() throws Throwable {
    
      Bundle buN4 = null;
      Bundle buN5 = null;
      try {
        buN = Util.installBundle (bc, "bundleN3_test-1.0.0.jar");
        buN4 = Util.installBundle (bc, "bundleN4_test-1.0.0.jar");
        buN5 = Util.installBundle (bc, "bundleN5_test-1.0.0.jar");
        buN.start();
        assertEquals("Unmatched fragment not resolved", Bundle.INSTALLED, buN4.getState());
        assertEquals("Matched fragment resolved", Bundle.RESOLVED, buN5.getState());
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle N3, unexpected "+ e +" :FRAME138A:FAIL");
      } finally {
        if (buN4 != null) {
          buN4.uninstall();
        }
        if (buN5 != null) {
          buN5.uninstall();
        }
      }

      out.println("### framework test bundle :FRAME138A:PASS");
    }
  }


  // Install testbundle N3 with arm processor and match with arm_le framework
  //

  class Frame0139a extends FWTestCase {
    public void runTest() throws Throwable {

      try {
        buN = Util.installBundle (bc, "bundleN3_test-1.0.0.jar");
        buN.start();
      } catch (Exception e) {
        e.printStackTrace();
        fail("framework test bundle N3, unexpected "+ e +" :FRAME139A:FAIL");
      }

      out.println("### framework test bundle :FRAME139A:PASS");
    }
  }

}
