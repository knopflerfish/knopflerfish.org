/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.log;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * * Catch all framework generated events and add them to the log. *
 * <p> * Mapping from event to log entry is according to the proposal *
 * <b>Mapping Rules from Framework-Generated Events to Log Entries * (Draft 1)</b>
 * to CPEG by Jan Luehe (Sun) 2000-08-29. * The severity level of service events
 * for modified services has * been changed to debug since such event may be
 * very frequent. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.1.1.1 $
 */
public class LogFrameworkListener implements FrameworkListener, BundleListener,
        ServiceListener {

    private final LogReaderServiceFactory lrsf;

    public LogFrameworkListener(LogReaderServiceFactory lrsf) {
        this.lrsf = lrsf;
    }

    /**
     * * The framework event callback method inserts all framework events * into
     * the log. * * Events of type <code>error</code> are logged at the error
     * level * other event types are logged on the info level. *
     * <p>
     * FrameworkListener callback. *
     * 
     * @param fe
     *            the framework event that has occured.
     */
    public void frameworkEvent(FrameworkEvent fe) {
        int level = LogService.LOG_INFO;
        String msg = null;
        Throwable thr = null;
        switch (fe.getType()) {
        case FrameworkEvent.ERROR:
            msg = "FrameworkEvent ERROR";
            level = LogService.LOG_ERROR;
            thr = fe.getThrowable();
            break;
        case FrameworkEvent.STARTED:
            msg = "FrameworkEvent STARTED";
            level = LogService.LOG_INFO;
            break;
        case FrameworkEvent.STARTLEVEL_CHANGED:
            msg = "FrameworkEvent STARTLEVEL_CHANGED";
            level = LogService.LOG_INFO;
            break;
        case FrameworkEvent.PACKAGES_REFRESHED:
            msg = "FrameworkEvent PACKAGES_REFRESHED";
            level = LogService.LOG_INFO;
            break;
        default:
            msg = "FrameworkEvent <" + fe.getType() + ">";
            level = LogService.LOG_WARNING;
            break;
        }
        lrsf.log(new LogEntryImpl(fe.getBundle(), level, msg, thr));
    }

    /**
     * * The bundle event callback method inserts all bundle events * into the
     * log. * * Events are all assinged the log level info, *
     * 
     * @param be
     *            the bundle event that has occured.
     */
    public void bundleChanged(BundleEvent be) {
        String msg = null;
        switch (be.getType()) {
        case BundleEvent.INSTALLED:
            msg = "BundleEvent INSTALLED";
            break;
        case BundleEvent.STARTED:
            msg = "BundleEvent STARTED";
            break;
        case BundleEvent.STOPPED:
            msg = "BundleEvent STOPPED";
            break;
        case BundleEvent.UNINSTALLED:
            msg = "BundleEvent UNINSTALLED";
            break;
        case BundleEvent.UPDATED:
            msg = "BundleEvent UPDATED";
            break;
        }
        lrsf.log(new LogEntryImpl(be.getBundle(), LogService.LOG_INFO, msg));
    }

    /**
     * * The service event callback method inserts all service events * into the
     * log. * * Event of types REGISTERED, UNREGISTERED are assinged the log
     * level info. Events of type MODIFIED are assigned the log level DEBUG. *
     * 
     * @param se
     *            the service event that has occured.
     */
    public void serviceChanged(ServiceEvent se) {
        ServiceReference sr = se.getServiceReference();
        Bundle bundle = sr.getBundle();
        String msg = null;
        int level = LogService.LOG_INFO;
        switch (se.getType()) {
        case ServiceEvent.REGISTERED:
            msg = "ServiceEvent REGISTERED";
            break;
        case ServiceEvent.UNREGISTERING:
            msg = "ServiceEvent UNREGISTERING";
            break;
        case ServiceEvent.MODIFIED:
            msg = "ServiceEvent MODIFIED";
            level = LogService.LOG_DEBUG;
            break;
        }
        lrsf.log(new LogEntryImpl(bundle, sr, level, msg));
    }

}// end of class LogFrameworkListener
