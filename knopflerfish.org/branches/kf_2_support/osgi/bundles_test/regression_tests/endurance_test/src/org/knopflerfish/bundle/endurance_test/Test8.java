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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.bundleEnd7_test.*;
import org.knopflerfish.service.bundleEnd101_test.*;
import org.knopflerfish.service.bundleEnd102_test.*;

class Test8 implements EnduranceTest {
  private Bundle b1;
  private Bundle b2;

  private ServiceTracker tracker1;
  private ServiceTracker tracker2;
  private ServiceTracker tracker;
  private BundleContext bc;
  
  Test8(BundleContext bc) {
    this.bc = bc;
  }
  
  public void prepare() {
    try {
      b1 = Util.installBundle(bc, "bundleEnd101_test-1.0.0.jar");
      b2 = Util.installBundle(bc, "bundleEnd102_test-1.0.0.jar");
      b1.start();
      b2.start();
      
      tracker1 = new ServiceTracker(bc, Control101.class.getName(), null);
      tracker2 = new ServiceTracker(bc, Control102.class.getName(), null);
      tracker1.open();
      tracker2.open();
      
      tracker = new ServiceTracker(bc, Control.class.getName(), null); 
      tracker.open();
      
    } catch (BundleException e) {
      
      e.printStackTrace();
    }
  }

  public boolean runTest() {
    Control c = (Control)tracker.getService();
    c.registerFactory();
    Control101 c101 = (Control101)tracker1.getService();
    Control102 c102 = (Control102)tracker2.getService();
    c101.getPhonyFactoryService();
    c102.getPhonyFactoryService();
    c.unregisterFactory();
    return true;
  }

  public void cleanup() {
     tracker1.close();
     tracker2.close();
     
     try {
      b1.uninstall();
      b2.uninstall();
      tracker.getServiceReference().getBundle().uninstall();
       
    } catch (BundleException e) {
      e.printStackTrace();
    }
    tracker.close();
  }

  public int getNoRuns() {
    return 1000;
  }

  public String testName() {
    return "Servicefactory tests";
  }
}
