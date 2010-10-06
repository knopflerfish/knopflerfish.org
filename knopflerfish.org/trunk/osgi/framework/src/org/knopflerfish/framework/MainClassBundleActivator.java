/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.lang.reflect.*;

import org.osgi.framework.*;


/**
 * BundleActivator implementation that can handle a jar file
 * with just a Main-class attribute.
 *
 * <p>
 * When the <tt>start</tt> method is called, a new thread is started for
 * the bundle and the static <tt>main</tt> method is called with zero
 * arguments.
 * </p>
 *
 * <p>
 * When the <tt>stop</tt> method is called, any static method named "stop"
 * is called.
 * </p>
 */
public class MainClassBundleActivator implements BundleActivator, Runnable {

  Method startMethod = null;
  Method stopMethod  = null;

  Thread runner      = null;

  String[] argv = new String[] { };

  public MainClassBundleActivator(Class clazz) throws Exception {
    startMethod = clazz.getMethod("main", new Class[] { argv.getClass() });

    // Check for optional stop method
    try {
      stopMethod  = clazz.getMethod("stop", new Class[] { });
    } catch (Exception ignored) {
    }
  }

  public void start(BundleContext bc) throws BundleException {

    try {
      BundleImpl b = (BundleImpl)bc.getBundle();
      runner = new Thread(b.fwCtx.threadGroup,
                          "start thread for executable jar file, bundle id="
                          + b.getBundleId());
      runner.start();
    } catch (Exception e) {
      throw new BundleException("Failed to start main class",
                                BundleException.UNSPECIFIED, e);
    }
  }

  public void stop(BundleContext bc) throws BundleException {
    if(stopMethod != null) {
      try {
        stopMethod.invoke(null, new Object[] { } );
      } catch (Exception e) {
        throw new BundleException("Failed to stop main class",
                                  BundleException.UNSPECIFIED, e);
      }
    }
  }

  public void run() {
    try {
      startMethod.invoke(null, new Object[] { argv } );
    } catch (Exception e) {
      System.err.println("Failed to start executable jar file: " + e);
    }
  }
}
