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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;

/**
 * * A LogReaderServiceFactory implements the log functionality. * * I.e., it
 * keeps log entries in memory and initiates callbacks to * LogListeners. It
 * does not keep track of subscribers, this is * delegated to
 * LogReaderServiceImpl. * * It uses an instance of FileLog to write log entryes
 * to file if * that configuration option has been enabled. It also prints log *
 * entries on System.out if desired. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.3 $ *
 */
public final class LogReaderServiceFactory implements ServiceFactory {
    /** Handle to the framework. */
    BundleContext bc;

    /**
     * A FileLog that writes log entries to file. * (Accessed from the
     * LogConfigCommandGroup).
     */
    FileLog fileLog;

    /**
     * The logReaderServicies table maps LogReaderServiceImpl object to * the
     * bundle that owns the instance. * * The key is an instance of
     * LogReaderServiceImpl and the value the * Bundle object for the bundle
     * that uses the service.
     */
    Hashtable logReaderServicies = new Hashtable();

    LogConfigImpl configuration;

    /**
     * * The constructor fetches the destination(s) of the log entries * from
     * the system properties.
     */
    public LogReaderServiceFactory(BundleContext bc, LogConfigImpl lc) {
        this.bc = bc;
        configuration = lc;
        history = new LogEntry[configuration.getMemorySize()];
        historyInsertionPoint = 0;
        try {
            if (configuration.getFile()) {
                fileLog = new FileLog(bc, configuration);
            }
        } catch (Exception e) {
        }
        configuration.init(this);
    }

    /**
     * * The stop method is called by the bundle activator when the log * bundle
     * is stoped. If a <code>fileLog</code> is active terminate * it.
     */
    void stop() {
        if (fileLog != null) {
            synchronized (fileLog) {
                fileLog.stop();
                fileLog = null;
            }
        }
        configuration.stop();
    }

    /* Methods called when configuration is changed. */

    /**
     * * Reset number of log entries that are kept in memory. *
     * 
     * @param size
     *            the new maximum number of log entries in memory.
     */
    void resetMemorySize(int size, int memorySize) {
        if (size <= 0) {
            size = 1;
        }
        synchronized (history) {
            LogEntry[] new_history = new LogEntry[size];
            int leftSize = historyInsertionPoint;
            // Copy all entries to the left of the insertion point in
            // history to end of the new_history.
            if (leftSize > 0) {
                // There are entries to the left
                if (leftSize > size) {
                    // To many entries; ignore oldest (leftmost)
                    System.arraycopy(history, leftSize - size + 1, new_history,
                            1, size - 1);
                } else {
                    // Copy all entries to the left
                    System.arraycopy(history, 0, new_history, size - leftSize,
                            leftSize);
                    // Are there more entries to the right?
                    int remaindingSize = size - leftSize;
                    int remaindingEntries = memorySize - leftSize;
                    if (remaindingSize > remaindingEntries) {
                        // Copy all entries to the right
                        System.arraycopy(history, leftSize, new_history,
                                remaindingSize - remaindingEntries,
                                remaindingEntries);
                    } else {
                        // Too many entries; ignore oldest (leftmost)
                        System.arraycopy(history, memorySize - remaindingSize,
                                new_history, 0, remaindingSize);
                    }
                }
            } else {
                // Copy the last size entries from history
                int s = (size > memorySize) ? memorySize : size;
                int fromPos = (size > memorySize) ? 0 : memorySize - s;
                System.arraycopy(history, fromPos, new_history, size - s, s);
            }
            history = new_history;
            historyInsertionPoint = 0;
        }
    }

    private void resetFile(Boolean newValue, Boolean oldValue) {
        if (newValue.booleanValue() && fileLog == null) {
            fileLog = new FileLog(bc, configuration);
            if (oldValue == null) {
                synchronized (fileLog) {
                    synchronized (history) {
                        fileLog.saveMemEntries(new ArrayEnumeration(history,
                                historyInsertionPoint));
                    }
                }
            }
        } else if (!(newValue.booleanValue()) && (fileLog != null)) {
            synchronized (fileLog) {
                fileLog.stop();
                fileLog = null;
            }
        }
    }

    /* Method called by the LogConfig to indicate chage of properties. */
    void configChange(String propName, Object oldValue, Object newValue) {
        String name = propName;
        if (name.equals(LogConfigImpl.MEM)) {
            resetMemorySize(((Integer) newValue).intValue(),
                    ((Integer) oldValue).intValue());
        } else if (name.equals(LogConfigImpl.FILE)) {
            resetFile((Boolean) newValue, (Boolean) oldValue);
        } else if (name.equals(LogConfigImpl.GEN) && fileLog != null) {
            synchronized (fileLog) {
                fileLog.resetGenerations(((Integer) newValue).intValue(),
                        ((Integer) oldValue).intValue());
            }
        }
    }

    /**
     * Each Bundle gets its own LogReaderServiceImpl, and the
     * LogReaderServiceFactory keeps track of the created LogReaderServices.
     */
    public Object getService(Bundle bc, ServiceRegistration sd) {
        LogReaderServiceImpl lrsi = new LogReaderServiceImpl(this);
        logReaderServicies.put(lrsi, bc);
        return lrsi;
    }

    public void ungetService(Bundle bc, ServiceRegistration sd, Object s) {
        logReaderServicies.remove(s);
    }

    /*
     * This is the code that implements the log functionality! It keeps a short
     * history of the log in memory, this is the part that is returned when
     * getLog is called.
     */

    /** The history list (an array used as a circular list). */
    LogEntry[] history;

    /**
     * The index in <code>history</code> where the next entry shall be *
     * inserted.
     */
    int historyInsertionPoint = 0;

    /*
     * Return an enumeration of the historyLength last entries in the log.
     */
    public Enumeration getLog() {
        return new ArrayEnumeration(history, historyInsertionPoint);
    }

    /**
     * * Returns the filter level for a specific bundle. *
     * 
     * @param bundel
     *            the bundle to get the filter level for.
     */
    public int getLogLevel(final Bundle bundle) {
        // The synchronized block below is needed to work around a bug in
        // JDK1.2.2 on Linux (with green threads).
        synchronized (configuration) {
            Integer res = (Integer) AccessController
                    .doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            return new Integer(getFilterLevel(bundle));
                        }
                    });
            return res.intValue();
        }
    }

    /**
     * * A new log entry has arrived. If its numeric level is less than * or
     * equal to the filter level then output it and store it in the * memory
     * log. All LogReaderServiceImpl is notified for all new * logEntries, i.e.,
     * the logFilter is not used for this. The * LogReaderService will notify
     * the LogListeners. *
     * 
     * @param le
     *            The new LogEntry
     */
    protected synchronized void log(final LogEntryImpl le) {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (le.getLevel() <= getFilterLevel(le.getBundle())) {
                    if (fileLog != null) {
                        synchronized (fileLog) {
                            fileLog.logged(le);
                        }
                    }
                    if (configuration.getOut()) {
                        System.out.println(le);
                    }
                    synchronized (history) {
                        history[historyInsertionPoint] = le;
                        historyInsertionPoint++;
                        if (historyInsertionPoint == history.length) {
                            historyInsertionPoint = 0;
                        }
                    }
                }
                for (Enumeration e = logReaderServicies.keys(); e
                        .hasMoreElements();) {
                    try {
                        ((LogReaderServiceImpl) e.nextElement()).callback(le);
                    } catch (Exception ce) {
                        // TBD Log error?
                    }
                }
                return null;
            }
        });
    }

    /**
     * Get current filter level given a bundle. *
     * 
     * @return filter level.
     */
    int getFilterLevel(Bundle b) {
        return (b != null) ? configuration.getLevel(b) : configuration
                .getFilter();
    }
}

/*
 * Auxiliary class used to create a Enumeration from an array. The array is
 * cloned to keep the array consistent even if the contents of the orginal array
 * is changed.
 * 
 * The elements in the array are not cloned, but they are not changed in the log
 * anyway.
 */

class ArrayEnumeration implements Enumeration {

    private Object[] array;

    private int pos;

    private int endPos;

    private boolean more = true;

    public ArrayEnumeration(Object[] a, int start) {
        this.array = (Object[]) a.clone();
        endPos = start;
        pos = start;
        nextPos();
    }

    /**
     * Decrement i until array[i] is non-null or there are no more * elements.
     * Sets <code>more</code> to false when the last element * has been
     * reached.
     */
    private void nextPos() {
        do {
            pos = (pos == 0) ? array.length - 1 : pos - 1;
        } while (array[pos] == null && pos != endPos);
        more = array[pos] != null;
    }

    public boolean hasMoreElements() {
        return more;
    }

    public Object nextElement() {
        if (more) {
            Object o = array[pos];
            if (pos == endPos) {
                more = false;
            } else {
                nextPos();
            }
            return o;
        }
        throw new NoSuchElementException();
    }

}
