/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing.console;

import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConsoleSwing
  implements org.osgi.util.tracker.ServiceTrackerCustomizer
{
  final static private String logServiceName     = LogService.class.getName();
  final static private String consoleServiceName = ConsoleService.class.getName();

  static ConsoleSwing theConsoleSwing;

  static BundleContext  bc     = null;
  static Config         config = new Config();
  public static SwingIO swingIO  = null;

  ServiceTracker        consoleTracker;
  ConsoleService        consoleService = null;
  Session               consoleSession = null;


  public ConsoleSwing(BundleContext bc)  {
    this.bc              = bc;
    this.theConsoleSwing = this;
    // Set up service tracker for the console service.
    consoleTracker = new ServiceTracker(bc, consoleServiceName, this);
  }

  public void start() {

    log(LogService.LOG_INFO, "Starting");

    swingIO = new SwingIO();
    swingIO.start();
    swingIO.disableInput("No Console Service Available.");
    consoleTracker.open();
  }

  public void clearConsole() {
    if(swingIO != null) {
      swingIO.clear();
    }
  }

  void close() {
    closeSession();
    closeSwing();
  }

  JComponent fallBack = null;

  public JComponent getJComponent() {
    if(swingIO != null) {
      return swingIO;
    } else {
      if(fallBack == null) {
        fallBack = new JLabel("No console available");
      }
      return fallBack;
    }
  }


  static void reinit() {
    if(swingIO != null) {
      swingIO.reinit();
    }
  }

  public synchronized void stop() {
    log(LogService.LOG_INFO, "Stopping");
    consoleTracker.close();
    close();
    this.bc           = null;
  }


  void closeSwing() {
    if(swingIO != null) {
      swingIO.stop();
      swingIO = null;
    }
    System.out.println("Swing console closed");
  }

  void closeSession() {
    if (consoleSession != null) {
      try {

        consoleSession.close();
        consoleSession = null;
      } catch(Exception e) {
        log(LogService.LOG_WARNING, "session close failed", e);
      }
    }
  }


  /*------------------------------------------------------------------------*
   *			  ServiceTrackerCustomizer implementation
   *------------------------------------------------------------------------*/

  public Object addingService(ServiceReference reference) {
    if (null==consoleService) {
      log(LogService.LOG_INFO, "New console service selected.");

      consoleService  = (ConsoleService) bc.getService(reference);

      if (swingIO==null) swingIO = new SwingIO();

      if(consoleService != null) {
        try {
          swingIO.start();
          consoleSession =
            consoleService.runSession("ConsoleSwing",
                                      swingIO.in,
                                      new PrintWriter(swingIO.out));
        } catch (IOException ioe) {
          swingIO.disableInput("No Console Service Available.");
          log(LogService.LOG_ERROR, "Failed to start console session");
        }
      } else {
        swingIO.disableInput("No Console Service Available.");
        log(LogService.LOG_WARNING, "No console service - skipping session");
      }
      return consoleService;
    } else {
      return null;
    }
  }

  public void modifiedService(ServiceReference reference, Object service) {
  }

  public void removedService(ServiceReference reference, Object service) {
    if (consoleService == service) {
      if (null!=consoleSession) {
        log(LogService.LOG_INFO, "Console service closed.");
        closeSession();
        swingIO.disableInput("No Console Service Available.");
      }
      consoleService = null;
    }
  }


  public static void log(int level, String msg) {
    log(level, msg, null);
  }

  public static void log(int level, String msg, Exception e) {
    ServiceReference srLog = bc.getServiceReference(logServiceName);
    if (srLog != null) {
      LogService sLog = (LogService)bc.getService(srLog);
      if (sLog != null) {
        if(e != null) {
          sLog.log(level, msg, e);
        } else {
          sLog.log(level, msg);
        }
      }
      bc.ungetService(srLog);
    }
  }
}
