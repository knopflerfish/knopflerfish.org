/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import org.osgi.framework.BundleException;


/**
 * Static variables that controls debugging of the framework code.
 *
 * @author Jan Stein
 */
public class Debug {

  /**
   * Report whenever the bundle classloader does something.
   */
  static boolean classLoader = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.classloader"));


  /**
   * Report event handling events.
   */
  static boolean errors = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.errors"));


  /**
   * Report package handling events.
   */
  static boolean packages = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.packages"));

  /**
   * Report startlevel.
   */
  static boolean startlevel = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.startlevel"));

  /**
   * Report url
   */
  static boolean url = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.url"));

  /**
   * Report LDAP handling
   */
  static boolean ldap = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.ldap"));

  /**
   * Report Class patching handling
   */
  static boolean patch = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.patch"));

  /**
   * Report Automanifest handling
   */
  static boolean automanifest = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.automanifest"));



  /**
   * Common println method for debug messages.
   */
  static void println(String str) {
    System.out.println("## DEBUG: " + str);
  }

  /**
   * Common printStackTrace method for debug messages.
   */
  static void printStackTrace(String str, Throwable t) {
    System.out.println("## DEBUG: " + str);
    t.printStackTrace();
    if (t instanceof BundleException) {
      Throwable n = ((BundleException)t).getNestedException();
      if (n != null) {
	System.out.println("Nested bundle exception:");
	n.printStackTrace();
      }
    }
  }

}