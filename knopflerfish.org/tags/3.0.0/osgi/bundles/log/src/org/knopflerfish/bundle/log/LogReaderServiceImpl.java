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

import java.util.Enumeration;
import java.util.Vector;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * * A LogReaderServiceImpl keeps track of the log subscribes from one *
 * BundleContext. It also contains a reference to the * LogReaderServiceFactory,
 * where the log functionality is * implemented. * * The log function in
 * registered LogListeners are called when the * LogReaderServiceFactory calls
 * the callback method.
 */
public class LogReaderServiceImpl implements LogReaderService {

    /** The log reader service factory that implements the log functionlity. */
    LogReaderServiceFactory lrsf;

    /**
     * * A Vector with LogListener objects.
     */
    Vector listeners = new Vector(2);

    /**
     * * The constructor saves the LogReaderServiceFactory. *
     * 
     * @param lrsf
     *            the log reader service factory that implements the * log
     *            functionlity.
     */
    LogReaderServiceImpl(LogReaderServiceFactory lrsf) {
        this.lrsf = lrsf;
    }

    /**
     * * Subscribe method. *
     * 
     * @param l
     *            A log listener to be notify when new log entries arrives.
     */
    public synchronized void addLogListener(LogListener l) {
        if (l == null)
            throw new IllegalArgumentException("LogListener can not be null");
        if (!listeners.contains(l)) {
            listeners.addElement(l);
        }
    }

    /**
     * * Unsubscribe method. * LogListeners are removed when number of
     * subscriptions are 0. *
     * 
     * @param l
     *            A log listener to be removed.
     */
    public synchronized void removeLogListener(LogListener l) {
        if (l == null)
            throw new IllegalArgumentException("LogListener can not be null");
        listeners.removeElement(l);
    }

    /**
     * * Bridge to LogReaderServiceFactory for fetching the log.
     */
    public Enumeration getLog() {
        return lrsf.getLog();
    }

    /**
     * * Used by LogReaderServiceFactory for every new log entry. * * Note that
     * the callback operation is not disturbed by changes in * the listener set
     * since such changes will result in that * <code>listerners</code> refers
     * to a new object, but the callback * operation will continue its
     * enumeration of the old listeners * object. * *
     * 
     * @param le
     *            A log entry to send to all listeners.
     */
    public void callback(LogEntry le) {
        Enumeration i = listeners.elements();

        while (i.hasMoreElements()) {
            try {
                ((LogListener) i.nextElement()).logged(le);
            } catch (Exception exc) {
            }
        }
    }

}
