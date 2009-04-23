/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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

package org.knopflerfish.framework.permissions;

import java.security.AccessController;
import java.security.PrivilegedAction;
import org.osgi.framework.BundleException;
import org.knopflerfish.framework.FrameworkImpl;

/**
 * Static variables that controls debugging of the framework code.
 *
 * @author Jan Stein
 */
public class Debug {

  /**
   * Controls if we should configure conditional permission so that it passes tck-4.0.1.
   */
  final static boolean tck401compat = new Boolean(System.getProperty("org.knopflerfish.framework.tck401compat", "false")).booleanValue();

  /**
   * Report Permission handling
   */
  final static boolean permissions = new Boolean(System.getProperty("org.knopflerfish.framework.debug.permissions", "false")).booleanValue();


  /**
   * Thread local storage to prevent recursive debug message
   * in permission checks
   */
  private static ThreadLocal insideDebug = new ThreadLocal() {
      protected synchronized Object initialValue() {
        return new Boolean(false);
      }
    };


  /**
   * Are we already inside a debug print?
   */
  private static void inside(boolean b) {
    insideDebug.set(new Boolean(b));
  }

  
  /**
   * Are we already inside a debug print?
   */
  private static boolean isInside() {
    return ((Boolean) (insideDebug.get())).booleanValue();
  }

  
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
    if (!isInside()) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            inside(true);
            println0(str);
            inside(false);
            return null;
          }
        });
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
    if (!isInside()) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            inside(true);
            printStackTrace0(str,t);
            inside(false);
            return null;
          }
        });
    }
  }

}
