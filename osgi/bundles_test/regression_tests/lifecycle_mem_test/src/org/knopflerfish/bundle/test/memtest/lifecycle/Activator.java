/*
 * Copyright (c) 2005-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.test.memtest.lifecycle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Bundle;

public class Activator implements BundleActivator, Runnable {

  // private fields

  private static final String TEST_BUNDLE_LOCATION_KEY = "org.knopflerfish.bundle.test.memtest.lifecycle.bundle";
  private static final String TEST_BUNDLE_LOCATION = System.getProperty(TEST_BUNDLE_LOCATION_KEY);

  private BundleContext context;
  private Thread thread;


  // implements BundleActivator

  public void start(BundleContext context) {
    this.context = context;

    thread = new Thread(this);
    thread.start();
  }

  public void stop(BundleContext context) {
    thread = null;
  }


  // implements Runnable

  public void run() {
    Thread currentThread = Thread.currentThread();
    try {
      while (currentThread == thread) {
        try {
          Bundle bundle = context.installBundle(TEST_BUNDLE_LOCATION);
          Thread.sleep(100);
          bundle.start();
          Thread.sleep(100);
          bundle.stop();
          Thread.sleep(100);
          bundle.uninstall();
          Thread.sleep(100);
        } catch (InterruptedException ignore) { }
      }
    } catch (BundleException be) {
      be.printStackTrace(System.err);
    }
  }

} // Activator
