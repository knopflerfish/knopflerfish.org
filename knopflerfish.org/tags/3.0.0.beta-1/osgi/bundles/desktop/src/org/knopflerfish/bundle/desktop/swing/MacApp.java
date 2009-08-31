/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationListener;
import com.apple.eawt.ApplicationEvent;

public class MacApp {
  Desktop desktop;
  Application app;
  ApplicationListener quitListener = new ApplicationListener() {
      public void handleAbout(ApplicationEvent event) {
        desktop.showVersion();
        event.setHandled(true);
      }

      public void handleOpenApplication(ApplicationEvent event) {
      }

      public void handleReOpenApplication(ApplicationEvent event) {
      }

      public void handleOpenFile(ApplicationEvent event) {
      }

      public void handlePreferences(ApplicationEvent event) {
      }

      public void handlePrintFile(ApplicationEvent event) {
      }

      public void handleQuit(ApplicationEvent event) {
        desktop.stopFramework();
        event.setHandled(true);
      }
    };

  public MacApp(Desktop _desktop) {
    this.desktop = _desktop;
    app = new Application();

    app.addApplicationListener(quitListener);
    //System.out.println("Mac Application listener installed.");
  }

  public void stop()
  {
    if (null!=app) app.removeApplicationListener(quitListener);
    //System.out.println("Mac Application listener removed.");
  }

}
