/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.boing;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.knopflerfish.service.log.LogRef;

public class Activator implements BundleActivator {

  static public LogRef log;
  static public BundleContext bc;

  private BoingDisplayer disp;
  private BoingDisplayer disp2;

  public void start(BundleContext bc) {
    Activator.bc = bc;
    Activator.log = new LogRef(Activator.bc);

    // bundle displayers
    disp = new BoingDisplayer(Activator.bc, true);
    disp.open();
    disp.register();

    // bundle displayers
    disp2 = new BoingDisplayer(Activator.bc, false, true, false, 120, 120);
    disp2.open();
    disp2.register();
  }

  public void stop(BundleContext bc) {
    try {
      if (log != null) {
        log = null;
      }

      disp.close();
      disp2.close();

      Activator.bc = null;
    } catch (Exception e) {
      log.warn("Exception in stop", e);
    }
  }

}
