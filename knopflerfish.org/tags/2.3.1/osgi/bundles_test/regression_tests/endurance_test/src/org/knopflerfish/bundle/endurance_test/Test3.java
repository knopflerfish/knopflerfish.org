/*
 * Copyright (c) 2006, KNOPFLERFISH project
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
package org.knopflerfish.bundle.endurance_test;

import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.*;

import org.knopflerfish.service.bundleEnd7_test.*;

class Test3 implements EnduranceTest {
  private BundleContext bc;
  private Bundle bundle;
  private ServiceTracker tracker;
  
  
  Test3(BundleContext bc) {
    this.bc = bc;
  }

  public void prepare() {
    try {
      bundle = Util.installBundle(bc, "bundleEnd7_test-1.0.0.jar");
      bundle.start();
      tracker = new ServiceTracker(bc, Control.class.getName(), null);
      tracker.open();
    } catch (Exception e) { 
      e.printStackTrace();
    }
  }

  public boolean runTest() {
    Control control = (Control)tracker.getService();
    control.register();
    ServiceReference ref = bc.getServiceReference(PhonyService7.class.getName());
    bc.getService(ref);
    bc.ungetService(ref);
    control.unregister();
    
    return true;
  }

  public void cleanup() {
      tracker.close();
      // is cleaned up in 4..
  }

  public int getNoRuns() {
    return 1000;
  }

  public String testName() {
    return "Register/getServiceReference/getService/ungetService/unregister";
  }

}
