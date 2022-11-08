/*
 * Copyright (c) 2004-2022, KNOPFLERFISH project
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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.knopflerfish.service.log.LogRef;

public class Activator
  implements BundleActivator
{
  public static BundleContext bc;
  public static LogRef log;

  private FrameworkTrayIcon tray_icon = null;

  public void start(BundleContext _bc)
  {
    Activator.bc = _bc;
    Activator.log = new LogRef(bc);

    EventQueue.invokeLater(() -> {
      try {
        tray_icon = FrameworkTrayIcon.getFrameworkTrayIcon();
        //noinspection ConstantConditions
        tray_icon.show();
      } catch (final Throwable t) {
        log.error("SystemTray is not supported on this platform", t);
      }
    });
  }

  public void stop(BundleContext bc)
  {
    if (tray_icon != null) {
      // Must not terminate until the tray icon has been removed!
      try {
        EventQueue.invokeAndWait(() -> {
          tray_icon.close();
          tray_icon = null;
        });
      } catch (final Exception e) {
        log.error("Failed to close the tray icon: " + e, e);
      }
    }
    log.close();

    log = null;
    Activator.bc = null;
  }

}
