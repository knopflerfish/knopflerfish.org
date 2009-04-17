/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

import java.util.*;
import java.io.*;

import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.knopflerfish.service.console.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ConsoleSwing {


  final static private String logServiceName     = LogService.class.getName();
  final static private String consoleServiceName = ConsoleService.class.getName();

  static ConsoleSwing theConsoleSwing;

  static BundleContext  bc     = null;
  static Config         config = new Config();
  public static SwingIO swingIO  = null;

  ServiceReference      srefConsole    = null;
  ConsoleService        sConsole       = null;
  Session               consoleSession = null;

  
  public ConsoleSwing(BundleContext bc)  {
    this.bc              = bc;
    this.theConsoleSwing = this;
  }

  public void start() {
    
    log(LogService.LOG_INFO, "Starting");

    // Get console service
    srefConsole = bc.getServiceReference(consoleServiceName);
    if(srefConsole == null) {
      log(LogService.LOG_ERROR, "No console service");
      return;
    }

    sConsole    = (ConsoleService) bc.getService(srefConsole);
    if (sConsole == null) {
      log(LogService.LOG_ERROR, "Failed to get ConsoleService, can not continue");
    }
    
    /*
    new Thread("console swing init thread") 
    {
      { this.start(); }
      public void run() {
    */
    
    swingIO = new SwingIO();
    swingIO.start();
    
    if(sConsole != null) {
      try {
	consoleSession = 
	  sConsole.runSession("ConsoleSwing",
			      swingIO.in,
			      new PrintWriter(swingIO.out));
      } catch (IOException ioe) {
	log(LogService.LOG_ERROR, "Failed to start console session");
      }
    } else {
      log(LogService.LOG_WARNING, "No console service - skipping session");
    }
    /*
  }
    };
    */
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

