/*
 * Copyright (c) 2006-2016, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


public class Activator implements BundleActivator
{
  private static ServiceTracker<LogService,LogService> logTracker;

  private SCR scr;


  /**
   * Initialize log and start SCR.
   */
  public void start(BundleContext bc) throws Exception {
    logTracker = new ServiceTracker<LogService, LogService>(bc,
                                                            LogService.class.getName(),
                                                            null);
    logTracker.open();
    scr = new SCR(bc);
    scr.start();
  }


  /**
   * Stop SCR
   */
  public void stop(BundleContext bc) {
    if (scr != null) {
      scr.stop();
    }
  }

  //
  // Log utility methods for component bundle
  //

  /**
   * Log info message via specified BundleContext
   */
  static void logInfo(BundleContext bc, String msg) {
    logBC(bc, LogService.LOG_INFO, msg, null);
  }


  /**
   * Log error message on specified bundle
   */
  static void logError(Bundle b, String msg, Throwable t) {
    logError(b.getBundleContext(), msg, t);
  }


  /**
   * Log error message via specified BundleContext
   */
  static void logError(BundleContext bc, String msg, Throwable t) {
    logBC(bc, LogService.LOG_ERROR, msg, t);
  }


  /**
   * Log message via specified BundleContext
   */
  static void logBC(BundleContext bc, int level, String msg, Throwable t) {
    try {
      ServiceReference<LogService> sr = bc.getServiceReference(LogService.class);
      if (sr != null) {
        LogService log = bc.getService(sr);
        if (log != null) {
          log.log(level, msg, t);
          bc.ungetService(sr);
        }
      }
    } catch (IllegalStateException ise) {
      log(level, "Logging message for " + bc.getBundle() 
          + " since it was inactive: " + msg, t);
    }
  }

  //
  // Log utility methods for SCR bundle
  //

  public static void logDebug(String message) {
    log(LogService.LOG_DEBUG, message, null);
  }

  public static void logInfo(String message) {
    log(LogService.LOG_INFO, message, null);
  }

  public static void logWarning(String message) {
    log(LogService.LOG_WARNING, message, null);
  }

  public static void logError(String message) {
    log(LogService.LOG_ERROR, message, null);
  }

  public static void logError(String message, Throwable t) {
    log(LogService.LOG_ERROR, message, t);
  }

  public static void log(int level, String message, Throwable t) {
    if (logTracker == null)
      return;
    LogService log = logTracker.getService();
    if (log == null)
      return;
    if (t == null) {
      log.log(level, message);
    } else {
      log.log(level, message, t);
    }
  }

  /**
   * Return info about ServiceReference
   */
  public static String srInfo(ServiceReference<?> sr) {
    StringBuffer sb = new StringBuffer();
    sb.append("ServiceReference(id=");
    sb.append(sr.getProperty(Constants.SERVICE_ID));
    sb.append(", [");
    String [] oc = (String [])sr.getProperty(Constants.OBJECTCLASS);
    for (int i = 0; i < oc.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(oc[i]);
    }
    sb.append("])");
    return sb.toString();
  }

}
