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

package org.knopflerfish.bundle.console;

import java.util.ArrayList;

import org.knopflerfish.service.console.ConsoleService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

// ******************** Console ********************
/**
 * Bundle activator implementation.
 *
 * @author Anders Rimen
 */
public class Activator
  implements BundleActivator
{
    BundleContext bc;

    ServiceRegistration<ConsoleService> consoleReg;

    // The set of active sessions that must be closed when this
    // bundle is stopped.
    static final ArrayList<SessionImpl> sessions = new ArrayList<>();

    /**
     * Called by the framework when this bundle is started.
     *
     * @param bc Bundle context.
     */
    public void start(BundleContext bc) {
        this.bc = bc;

        log(LogService.LOG_INFO, "Starting version "
                + bc.getBundle().getHeaders().get("Bundle-Version"));

        consoleReg = bc.registerService(ConsoleService.class,
                new ConsoleServiceImpl(bc), null);
    }

    /**
     * * Called by the framework when this bundle is stopped. * *
     *
     * @param bc
     *            Bundle context.
     */
    public void stop(BundleContext bc) {
        log(LogService.LOG_INFO, "Stopping");
        ArrayList<SessionImpl> currentSessions = new ArrayList<>(sessions);
        for (SessionImpl s : currentSessions) {
            s.bundleStopped();
        }
    }

    /**
     * Utility method used for logging.
     *
     * @param level Log level
     * @param msg Log message
     */
    @SuppressWarnings("SameParameterValue")
    void log(int level, String msg) {
        ServiceReference<LogService> srLog = bc.getServiceReference(LogService.class);
        if (srLog != null) {
            LogService sLog = bc.getService(srLog);
            if (sLog != null) {
                sLog.log(level, msg);
            }
            bc.ungetService(srLog);
        }
    }
}
