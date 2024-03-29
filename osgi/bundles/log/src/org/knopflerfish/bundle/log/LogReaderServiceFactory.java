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
import org.osgi.service.log.LogReaderService;

/**
 * A LogReaderServiceFactory implements the log functionality.  I.e.,
 * it keeps log entries in memory and initiates callbacks to
 * LogListeners. It does not keep track of subscribers, this is
 * delegated to LogReaderServiceImpl.
 *
 * It uses an instance of FileLog to write log entries to file if
 * that configuration option has been enabled. It also prints log
 * entries on System.out if desired.
 */
public final class LogReaderServiceFactory
  implements ServiceFactory<LogReaderService>
{
  /** Handle to the framework. */
  final BundleContext bc;

  /**
   * A FileLog that writes log entries to file. (Accessed from the
   * LogConfigCommandGroup).
   */
  FileLog fileLog;

  /**
   * The logReaderServicies table maps LogReaderServiceImpl object
   * to the bundle that owns the instance. The key is an instance of
   * LogReaderServiceImpl and the value the Bundle object for the
   * bundle that uses the service.
   */
  final Hashtable<LogReaderServiceImpl, Bundle> logReaderServicies = new Hashtable<LogReaderServiceImpl, Bundle>();

  final LogConfigImpl configuration;

  /**
   * The constructor fetches the destination(s) of the log entries
   * from the system properties.
   *
   * @param bc Our handle to the framework.
   * @param lc The log configuration to use.
   */
  public LogReaderServiceFactory(final BundleContext bc,
                                 final LogConfigImpl lc)
  {
    this.bc = bc;
    this.configuration = lc;

    history = new LogEntry[configuration.getMemorySize()];
    historyInsertionPoint = 0;

    LogEntryImpl.setTimestampPattern(configuration.getTimestampPattern());

    try {
      if (configuration.getFile()) {
        fileLog = new FileLog(bc, configuration);
      }
    } catch (Exception e) {
    }

    configuration.init(this);
  }

  /**
   * The stop method is called by the bundle activator when the log
   * bundle is stopped. If a <code>fileLog</code> is active shut it
   * down.
   */
  void stop() {
    synchronized (this) {
      if (fileLog != null) {
        fileLog.stop();
        fileLog = null;
      }
    }
    configuration.stop();
  }

  /* Methods called when configuration is changed. */

  /**
   * Reset number of log entries that are kept in memory.
   *
   * @param size the new maximum number of log entries in memory.
   * @param memorySize the current maximum number of in memory log entries.
   */
  synchronized void resetMemorySize(int size, int memorySize) {
    if (size <= 0) {
      size = 1;
    }
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

  // If the file log is active, restart it since the log directory has changed.
  private synchronized void resetFile() {
    if (fileLog != null) {
      fileLog.stop();
      fileLog = null;
      fileLog = new FileLog(bc, configuration);
    }
  }

  // Stop or start file log
  private synchronized void resetFile(Boolean newValue, Boolean oldValue) {
    if (newValue && fileLog == null) {
      fileLog = new FileLog(bc, configuration);
      if (oldValue == null) {
        synchronized (history) {
          fileLog.saveMemEntries(new ArrayEnumeration<LogEntry>(history,
                                                      historyInsertionPoint));
        }
      }
    } else if (!newValue && fileLog != null) {
      fileLog.stop();
      fileLog = null;
    }
  }

  /* Method called by the LogConfig to indicate chage of properties. */
  void configChange(final String propName,
                    final Object oldValue,
                    final Object newValue) {
    if (propName.equals(LogConfigImpl.MEM)) {
      resetMemorySize((Integer) newValue,
              (Integer) oldValue);
    } else if (propName.equals(LogConfigImpl.DIR)) {
      resetFile();
    } else if (propName.equals(LogConfigImpl.FILE)) {
      resetFile((Boolean) newValue, (Boolean) oldValue);
    } else if (propName.equals(LogConfigImpl.GEN) && fileLog != null) {
      synchronized (fileLog) {
        fileLog.resetGenerations((Integer) newValue,
                (Integer) oldValue);
      }
    } else if (propName.equals(LogConfigImpl.TIMESTAMP_PATTERN)) {
      LogEntryImpl.setTimestampPattern((String) newValue);
    }
  }

  /**
   * Each Bundle gets its own LogReaderServiceImpl, and the
   * LogReaderServiceFactory keeps track of the created LogReaderServices.
   */
  public LogReaderService getService(Bundle bc,
                                     ServiceRegistration<LogReaderService> sd)
  {
    LogReaderServiceImpl lrsi = new LogReaderServiceImpl(this);
    logReaderServicies.put(lrsi, bc);
    return lrsi;
  }

  public void ungetService(Bundle bc,
                           ServiceRegistration<LogReaderService> sd,
                           LogReaderService s)
  {
    logReaderServicies.remove(s);
  }

  /*
   * This is the code that implements the log functionality! It keeps a short
   * history of the log in memory, this is the part that is returned when
   * getLog is called.
   */

  /** The history list (an array used as a circular list). */
  private LogEntry[] history;

  /**
   * The index in <code>history</code> where the next entry shall be *
   * inserted.
   */
  private int historyInsertionPoint = 0;

  /*
   * Return an enumeration of the historyLength last entries in the log.
   */
  public Enumeration<LogEntry> getLog() {
    return new ArrayEnumeration<LogEntry>(history, historyInsertionPoint);
  }

  /**
   * Returns the filter level for a specific bundle.
   *
   * @param bundle the bundle to get the filter level for.
   * @return log filter level for the specified bundle.
   */
  protected int getLogLevel(final Bundle bundle) {
    // The synchronized block below is needed to work around a bug in
    // JDK1.2.2 on Linux (with green threads).
    synchronized (configuration) {
      Integer res = AccessController
        .doPrivileged(new PrivilegedAction<Integer>() {
            public Integer run() {
              return getFilterLevel(bundle);
            }
          });
      return res;
    }
  }

  /**
   * A new log entry has arrived. If its numeric level is less than or
   * equal to the filter level then output it and store it in the
   * memory log. All LogReaderServiceImpls are notified for all new
   * logEntries, i.e., the logFilter is not used for this. The
   * LogReaderService will notify the LogListeners.
   *
   * @param le The new LogEntry
   */
  protected void log(final LogEntryImpl le) {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
          synchronized(LogReaderServiceFactory.this) {
            if (le.getLevel() <= getFilterLevel(le.getBundle())) {
              if (fileLog != null) {
                fileLog.logged(le);
              }
              if (configuration.getOut()) {
                System.out.println(le);
              }
              history[historyInsertionPoint] = le;
              historyInsertionPoint++;
              if (historyInsertionPoint == history.length) {
                historyInsertionPoint = 0;
              }
            }
          }
          for (Enumeration<LogReaderServiceImpl> e = logReaderServicies.keys();
               e.hasMoreElements();) {
            try {
              e.nextElement().callback(le);
            } catch (Exception ce) {
              // TBD Log error?
            }
          }
          return null;}
      });
  }

  /**
   * Get current filter level given a bundle. *
   *
   * @param b the bundle to get the filter level for.
   * @return filter level.
   */
  private int getFilterLevel(Bundle b) {
    return (b != null)
      ? configuration.getLevel(b)
      : configuration.getFilter();
  }
}

/*
 * Auxiliary class used to create a Enumeration from an array. The array is
 * cloned to keep the array consistent even if the contents of the original array
 * is changed.
 *
 * The elements in the array are not cloned, but they are not changed in the log
 * anyway.
 */

class ArrayEnumeration<E> implements Enumeration<E> {

  private E[] array;

  private int pos;

  private int endPos;

  private boolean more = true;

  public ArrayEnumeration(E[] a, int start) {
    this.array = (E[]) a.clone();
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

  public E nextElement() {
    if (more) {
      E o = array[pos];
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
