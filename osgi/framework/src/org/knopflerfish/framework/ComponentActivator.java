/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

import java.net.URL;
import java.util.Dictionary;
import java.util.Vector;
import java.util.ArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentConstants;

public class ComponentActivator implements BundleActivator {

  ComponentRuntimeImpl systemComponentRuntime;
  BundleContext bundleContext;


  /**
   * this is the main entry for the SCR it will check if any bundles declares
   * declarative components and create a ComponentDeclaration of the declaring
   * XML file. Having all already ACTIVE bundles with components the method will
   * create an instance of ComponentRuntimeImpl and pass the bundle context
   * and a Vector containing already active components.
   */
  public void start(BundleContext context) throws Exception {
    bundleContext = context;
    systemComponentRuntime = new ComponentRuntimeImpl(context);
  }

  /**
   * Stop the SCR
   */
  public void stop(BundleContext context) throws Exception {
    systemComponentRuntime.shutdown();
    systemComponentRuntime=null;
  }


  static boolean debug = "true".equalsIgnoreCase(System.getProperty("org.knopflerfish.framework.debug.component"));

  public static void debug(String msg) {
    debug(msg, null);
  }

  public static void debug(String msg, Throwable e) {
    if (!debug) return;
    error(msg, e);
  }

  public static void error(String msg) {
    error(msg, null);
  }

  public static void error(Throwable e) {
    error(null, e);
  }

  public static void error(String msg, Throwable e) {
    System.out.println("## DEBUG (component): " + (msg == null ? "" : msg));
    if (e == null) return;
    e.printStackTrace();
    if (e instanceof BundleException) {
      Throwable n = ((BundleException) e).getNestedException();
      if (n != null) {
        System.out.println("Nested bundle exception:");
        n.printStackTrace();
      }
    }
    try {
      if (e.getCause() != null) {
        System.out.println("Caused by:");
        e.getCause().printStackTrace();
      }
    } catch (Throwable ignore) {}
  }
}
