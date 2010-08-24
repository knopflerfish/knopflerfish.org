/*
 * Copyright (c) 2004-2010, KNOPFLERFISH project
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

package org.knopflerfish.bundle.trayicons.framework;

import java.awt.EventQueue;
import java.awt.event.*;

import org.osgi.framework.*;
import org.osgi.service.startlevel.*;
import org.osgi.util.tracker.*;

import org.knopflerfish.service.log.LogRef;

public class Activator
  implements BundleActivator
{
  public static BundleContext bc;
  public static LogRef        log;

  private FrameworkTrayIcon tray_icon = null;

  public void start(BundleContext _bc) {
    this.bc  = _bc;
    this.log = new LogRef(bc);

    EventQueue.invokeLater(new Runnable() {
      public void run()
      {
        try {
          tray_icon = FrameworkTrayIcon.getFrameworkTrayIcon();
          tray_icon.show();
        }
        catch (Throwable t) {
          log.error("SystemTray is not supported on this platform", t);
        }
      }
    });
  }

  public void stop(BundleContext bc) {
    if (tray_icon != null) {
      // Must not terminate untill the tray icon has been removed!
      try {
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
            tray_icon.close();
            tray_icon = null;
          }
        });
      } catch (Exception e) {
        log.error("Failed to close the tray icon: "+e, e);
      }
    }
    log.close();

    log = null;
    this.bc  = null;
  }

}
