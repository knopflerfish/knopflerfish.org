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

class Test10 implements EnduranceTest {
  private BundleContext bc;
  private int loops;
  private String[] locales;
  private int noRuns;
  private String desc;
  
  Test10(BundleContext bc, int noGets, int noRuns, String[] locales, String desc) {
    this.bc = bc;
    this.loops = noGets;
    this.noRuns = noRuns;
    this.locales = locales;
    this.desc = desc;
  }
  
  public void prepare() {

  }

  public boolean runTest() {
    try {
      Bundle fragment = Util.installBundle(bc, "bundleEnd152_test-1.0.0.jar");
      Bundle bundle = Util.installBundle(bc, "bundleEnd151_test-1.0.0.jar");
      bundle.start();

      for (int i = 0; i < loops; i++) {   
        for (int o = 0; o < locales.length; o++) {
          bundle.getHeaders(locales[o]);
        }
      }
      
      bundle.uninstall();
      fragment.uninstall();
    } catch (BundleException e) {
      e.printStackTrace();
    } 
    return true;
  }

  public void cleanup() {
  }

  public int getNoRuns() {
    return noRuns;
  }

  public String testName() {
    return desc;
  }

}
