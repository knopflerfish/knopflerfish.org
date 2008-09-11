/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import java.security.AccessController;
import java.security.PrivilegedAction;
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
  static boolean classLoader = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.classloader"));


  /**
   * Report event handling events.
   */
  static boolean errors = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.errors"));


  /**
   * Report package handling events.
   */
  static boolean packages = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.packages"));

  /**
   * Report startlevel.
   */
  static boolean startlevel = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.startlevel"));

  /**
   * Report url
   */
  static boolean url = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.url"));

  /**
   * Report LDAP handling
   */
  static boolean ldap = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.ldap"));

  /**
   * When security is enabled, print information about service
   * reference lookups that are rejected due to missing permissions
   * for calling bundle.
   */
  static boolean service_reference = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.service_reference"));

  /**
   * When security is enabled, print information about resource
   * lookups that are rejected due to missing permissions for the
   * calling bundle.
   */
  static boolean bundle_resource = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.bundle_resource"));

  /**
   * When security is enabled, print information about context
   * lookups that are rejected due to missing permissions for the
   * calling bundle.
   */
  static boolean bundle_context = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.bundle_context"));

  /**
   * Report Class patching handling
   */
  static boolean patch = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.patch"));

  /**
   * Report Automanifest handling
   */
  static boolean automanifest = "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.automanifest"));



  /**
   * When security is enabled, use a doPrivileged() around
   * the actual call to System.out.println() to allow for PrintStream
   * implementations that does not handle the case with limited
   * priviledges themselfs.
   */
  static boolean use_do_privilege
    = System.getSecurityManager() != null
    && "true".equalsIgnoreCase(Framework.getProperty("org.knopflerfish.framework.debug.print_with_do_privileged","true"));


  /**
   * The actual println implementation.
   *
   * @param str the message to print.
   */
  private static void println0(final String str) {
    System.out.println("## DEBUG: " + str);
  }

  /**
   * Common println method for debug messages.
   *
   * @param str the message to print.
   */
  static void println(final String str) {
    if(use_do_privilege) {
      // The call to this method can be made from a the framework on
      // behalf of a bundle that have no permissions at all assinged
      // to it.
      //
      // Use doPrivileged() here to protect the Framework from
      // PrintStream implementations that does not wrapp calls needing
      // premissions in their own doPrivileged().
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            println0(str);
            return null;
          }
        });
    } else {
      println0(str);
    }
  }

  /**
   * The actual printStackTrace() implementation.
   *
   * @param str the message to print.
   * @param t   the throwable to print a stack trace for.
   */
  private static void printStackTrace0(final String str, final Throwable t) {
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

  /**
   * Common printStackTrace method for debug messages.
   *
   * @param str the message to print.
   * @param t   the throwable to print a stack trace for.
   */
  static void printStackTrace(final String str, final Throwable t) {
    if(use_do_privilege) {
      // The call to this method can be made from a the framework on
      // behalf of a bundle that have no permissions at all assinged
      // to it.
      //
      // Use doPrivileged() here to protect the Framework from
      // PrintStream implementations that does not wrapp calls needing
      // premissions in their own doPrivileged().
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            printStackTrace0(str,t);
            return null;
          }
        });
    } else {
      printStackTrace0(str,t);
    }
  }

}
