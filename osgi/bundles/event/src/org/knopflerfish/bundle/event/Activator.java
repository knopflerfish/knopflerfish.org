/*
 * Copyright (c) 2005-2010, KNOPFLERFISH project
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

package org.knopflerfish.bundle.event;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;

/**
 * The Activator class is the startup class for the EventHandlerService.
 *
 * @author Magnus Klack
 */
public class Activator implements BundleActivator {
  private static final String TIMEOUT_PROP
    = "org.knopflerfish.eventadmin.timeout";
  private static final String TIMEWARNING_PROP
    = "org.knopflerfish.eventadmin.timewarning";
  private static final String QUEUE_HANDLER_MULTIPLE_PROP
    = "org.knopflerfish.eventadmin.queuehandler.multiple";
  private static final String QUEUE_HANDLER_TIMEOUT_PROP
    = "org.knopflerfish.eventadmin.queuehandler.timeout";

  static BundleContext bundleContext;
  static LogRef log;
  static ServiceRegistration reg;
  static EventAdminService eventAdmin;
  static EventHandlerTracker handlerTracker;
  static long timeout = 0;
  static boolean useMultipleQueueHandlers = true;
  static long    queueHandlerTimeout = 1100;
  static long timeWarning = 0;

  public void start(BundleContext context) throws Exception
  {
    Activator.bundleContext = context;

    /* Tries to get the timeout property from the system */
    try {
      final String timeoutS = Activator.bundleContext.getProperty(TIMEOUT_PROP);
      if (null != timeoutS && 0 < timeoutS.length()) {
        timeout = Long.parseLong(timeoutS);
      }
    } catch (NumberFormatException ignore) {
    }

    try {
      final String timeoutS
        = Activator.bundleContext.getProperty(QUEUE_HANDLER_TIMEOUT_PROP);
      if (null != timeoutS && 0 < timeoutS.length()) {
        queueHandlerTimeout = Long.parseLong(timeoutS);
      }
    } catch (NumberFormatException ignore) {
    }

    final String qhm
      = Activator.bundleContext.getProperty(QUEUE_HANDLER_MULTIPLE_PROP);
    if (null!=qhm) {
      useMultipleQueueHandlers = !"false".equalsIgnoreCase(qhm);
    }

    try {
      String timeWarningS = Activator.bundleContext
          .getProperty(TIMEWARNING_PROP);
      if (null != timeWarningS && 0 < timeWarningS.length()) {
        timeWarning = Long.parseLong(timeWarningS);
      }
    } catch (NumberFormatException ignore) {
      timeWarning = 0;
    }

    log = new LogRef(context);

    handlerTracker = new EventHandlerTracker(context);
    handlerTracker.open();

    eventAdmin = new EventAdminService();
    reg = bundleContext.registerService(EventAdmin.class.getName(), eventAdmin,
        null);
  }

  public void stop(BundleContext context) throws Exception
  {
    reg.unregister();

    eventAdmin.stop();
    eventAdmin = null;

    handlerTracker.close();
    handlerTracker = null;

    //InternalAdminEvent.close();
  }
}
