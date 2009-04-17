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

import org.knopflerfish.service.log.LogService;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * * The LogServiceImpl is repsonsible for creating a LogEntry with the * log
 * data it receives, and forward it to the * LogReaderServiceFactory that
 * implements the log functionality. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.1.1.1 $
 */
public class LogServiceImpl implements LogService {

    /** The bundle that owns this service object. */
    private Bundle bundle;

    /** The log reader service factory to forward requests to. */
    private LogReaderServiceFactory lrsf;

    /**
     * * The constructor saves the Bundle and the LogReaderServiceFactory. * *
     * 
     * @param bundle
     *            The bundle that requested this service. *
     * @param lrsf
     *            The LogReaderServiceFactory that implements the * log
     *            functionality.
     */
    public LogServiceImpl(Bundle bundle, LogReaderServiceFactory lrsf) {
        this.bundle = bundle;
        this.lrsf = lrsf;
    }

    /**
     * * Create LogEntry object and send it to <code>lrsf</code>. *
     * 
     * @param level
     *            The severity level of the entry to create. *
     * @param msg
     *            The message of the entry to create.
     */
    public void log(int level, String msg) {
        lrsf.log(new LogEntryImpl(bundle, level, msg));
    }

    /**
     * * Create LogEntry object and send it to <code>lrsf</code>. *
     * 
     * @param level
     *            The severity level of the entry to create. *
     * @param msg
     *            The message of the entry to create. *
     * @param t
     *            A Throwable that goes with the entry.
     */
    public void log(int level, String msg, Throwable t) {
        lrsf.log(new LogEntryImpl(bundle, level, msg, t));
    }

    /**
     * * Create LogEntry object and send it to <code>lrsf</code>. *
     * 
     * @param sref
     *            A ServiceReference for the service that wants this entry. *
     * @param level
     *            The severity level of the entry to create. *
     * @param msg
     *            The message of the entry to create.
     */
    public void log(ServiceReference sref, int level, String msg) {
        lrsf.log(new LogEntryImpl(bundle, sref, level, msg));
    }

    /**
     * * Create LogEntry object and send it to <code>lrsf</code>. *
     * 
     * @param sref
     *            A ServiceReference for the service that wants this entry. *
     * @param level
     *            The severity level of the entry to create. *
     * @param msg
     *            The message of the entry to create. *
     * @param t
     *            A Throwable that goes with the entry.
     */
    public void log(ServiceReference sref, int level, String msg, Throwable t) {
        lrsf.log(new LogEntryImpl(bundle, sref, level, msg, t));
    }

    /**
     * * Get the current log level. The log will discard log entires * with a
     * level that is less severe than the current level. * I.e. entries with a
     * level that is numerically larger than the * value returned by this
     * method. * * E.g. If the current log level is LOG_WARNING then the log
     * will * discard all log entries with level LOG_INFO and LOG_DEBUG. I.e. *
     * there is no need for a bundle to try to send such log entries * to the
     * log. The bundle may actually save a number of CPU-cycles * by getting the
     * log level and do nothing if the intended log * entry is less severe than
     * the current log level. * *
     * 
     * @return the lowest severity level that is accepted into the * log.
     */
    public int getLogLevel() {
        return lrsf.getLogLevel(bundle);
    }

}
