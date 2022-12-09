/*
 * Copyright (c) 2012-2022, KNOPFLERFISH project
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

package org.knopflerfish.example.rxtx_echo;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

  private static ServiceTracker logTracker = null;

  private SerialPortDevice serial;

  //
  // BundleActivator implementation
  //

  public void start(BundleContext bc) {
    logTracker = new ServiceTracker(bc, LogService.class.getName(), null);
    logTracker.open();

    // Create serial port reader
    serial = new SerialPortDevice(this, true);
    String device = bc.getProperty("org.knopflerfish.example.rxtx_echo.device");
    if (device == null) {
      device = "S0";
    }
    serial.open(device);
  }    


  public void stop(BundleContext context) {
    logTracker.close();
    serial.close();
  }

  //
  // SerialPortDevice callback method
  //

  /**
   * Called by SerialPortDevice when a device is connected or disconnected (CTS state).
   */
  public void gotDev(boolean connected, String dev) {
    if (connected) {
      logInfo("Connected to: " + dev);
    } else {
      logInfo("Disconnected from: " + dev);
    }
  }


  /**
   * Called by SerialPortDevice when a new message is read.
   */
  public void gotMsg(String msg) {
    serial.writeString("\r\nECHO: " + msg + "\r\n");
    logDebug("Wrote echo, " + msg);
  }

  /*****************************************************************************
   * Log utility methods
   ****************************************************************************/

  public static void logDebug(String message)
  {
    logDebug(message, null);
  }

  public static void logDebug(String message, Throwable t)
  {
    log(LogService.LOG_DEBUG, message, t);
  }

  public static void logInfo(String message)
  {
    log(LogService.LOG_INFO, message, null);
  }

  public static void logWarning(String message)
  {
    logWarning(message, null);
  }

  public static void logWarning(String message, Throwable t)
  {
    log(LogService.LOG_WARNING, message, t);
  }

  public static void logError(String message, Throwable t)
  {
    log(LogService.LOG_ERROR, message, t);
  }

  public static void log(int level, String message, Throwable t)
  {
    if (logTracker == null)
      return;
    LogService log = (LogService) logTracker.getService();
    if (log == null)
      return;
    if (t == null) {
      log.log(level, message);
    } else {
      log.log(level, message, t);
    }
  }

}
