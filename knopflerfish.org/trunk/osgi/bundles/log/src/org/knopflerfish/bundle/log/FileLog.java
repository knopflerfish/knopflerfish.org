/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

/**
 * A FileLog saves log entries to a circular set of files. The files
 * are named <code>gosglog</code> followed by an integer. The file
 * <code>gosglog0</code> is the current log file,
 * <code>gosglog1</code> is the one before and so on.
 *
 */
public final class FileLog implements LogListener {
  /** Date formater used to time stamp log files. */
  private static final SimpleDateFormat SDF
    = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

  /** Base name of log files. */
  private static final String LOGBASE = "gosglog";

  // ********** Configuration data **********
  /** Size of curent log. */
  private int logSize;

  /** Directory where the log files are stored. */
  private File logdir = null;

  // ********** Instance data **********
  /**
   * PrintWriter for current log file. Is <code>null</code> if
   * opening of the log file failed.
   */
  PrintWriter log;

  /** Handle to the configuration object */
  private final LogConfigImpl configuration;

  /**
   * The constructor creates a file log object base on configuration
   * data.  It will initialize a new empty log file.
   *
   * @param bc handle to the framework.
   * @param lc the log configuration to use.
   */
  public FileLog(final BundleContext bc, final LogConfigImpl lc) {
    configuration = lc;
    logdir = configuration.getDir();

    // Start by removing old log files, and rename previous log files
    // to old log files.
    savePreviousLog();
    // Open a new log, if possible
    openLog();
  }

  /**
   * * The stop method is called by the log reader service factory when * the
   * log bundle is stoped. Flush data and close the current log file.
   */
  synchronized void stop() {
    if (log != null) {
      log.flush();
      log.close();
      log = null;
    }
  }

  /**
   * Set the number of log file generations.
   *
   * @param oldGen
   *            the old generation count value.
   * @param gen
   *            the new generation count value.
   */
  synchronized void resetGenerations(int gen, int oldGen) {
    if (gen < 1) {
      gen = 1;
    }
    if (gen != oldGen) {
      if (oldGen > gen) { // Remove exessive files
        trimGenerations(gen);
      }
    }
  }

  /**
   * Open and initialize a new log file. Make sure that
   * <code>saveOldGen()</code> or <code>savePreviousLog()</code> has
   * been called before this method is called. I.e. make sure that
   * the file <code>LOGBASE0</code> in <code>logdir</code> is
   * available for writing.
   */
  private void openLog() {
    if (logdir == null)
      return;

    final File logfile = new File(logdir, LOGBASE + "0");
    try {
      final FileOutputStream fos = new FileOutputStream(logfile);
      final OutputStreamWriter osw = new OutputStreamWriter(fos);
      final BufferedWriter bw = new BufferedWriter(osw);
      log = new PrintWriter(bw);
      final String s = "Log started " + SDF.format(new Date());
      logSize = s.length();
      log.println(s);
      log.flush();
    } catch (IOException e) {
      System.err.println("Failed to open logfile " + logfile
                         + " due to: " + e.getMessage());
      log = null;
    }
  }

  /**
   * Rename log files one step.
   */
  private void saveOldGen() {
    if (logdir == null)
      return;
    for (int i = configuration.getMaxGen() - 1; i > 0; i--) {
      final File dst = new File(logdir, LOGBASE + i);
      final File src = new File(logdir, LOGBASE + (i - 1));
      if (dst.exists()) {
        dst.delete();
      }
      src.renameTo(dst);
    }
  }

  /**
   * Copies previous logs (assuming they have the same file name as
   * the new log) to filename".old"
   */
  private void savePreviousLog() {
    if (logdir == null)
      return;

    // Delete old logs
    boolean done = false;
    for (int i = 0; !done; i++) {
      final File src = new File(logdir, LOGBASE + i + ".old");
      if (src.exists()) {
        src.delete();
      } else
        done = true;
    }

    // Move current logs to old logs
    done = false;
    for (int i = 0; !done; i++) {
      final File src = new File(logdir, LOGBASE + i);
      if (src.exists()) {
        File dst = new File(logdir, LOGBASE + i + ".old");
        if (dst.exists()) {
          dst.delete();
        }
        src.renameTo(dst);
      } else
        done = true;
    }
  }

  /**
   * Delete all log files above given generation
   *
   * @param gen
   *            is the first file to remove
   */
  private void trimGenerations(int gen) {
    if (logdir == null)
      return;

    // Delete old logs
    boolean done = false;
    for (int i = gen; !done; i++) {
      final File src = new File(logdir, LOGBASE + i);
      if (src.exists()) {
        src.delete();
      } else
        done = true;
    }
  }

  /**
   * A new log entry has arrived write it to the log file.
   *
   * @param le
   *            The new LogEntry
   */
  public synchronized void logged(LogEntry le) {
    if (log != null) {
      String s = le.toString();
      if (logSize + s.length() > configuration.getFileSize()) {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              log.close();
              saveOldGen();
              openLog();
              return null;
            }
          });
      }
      logSize += s.length();
      log.println(s);
      if (configuration.getFlush()) {
        log.flush();
      }
    }
  }

  /**
   * Called when a valid configuration has been received for the
   * first time if the file writing is enabled in this
   * configuration.
   *
   * @param logEntries
   *            The LogEntries which have been written to memory before a
   *            configuration has been received.
   */
  synchronized void saveMemEntries(Enumeration logEntries) {
    // The log entires in the enumeration are in the wrong order,
    // i.e., latest first and oldest last thus we must reverse them.
    List entries = new ArrayList();
    while (logEntries.hasMoreElements()) {
      entries.add(logEntries.nextElement());
    }
    for (int i=entries.size()-1; i>-1; --i) {
      logged((LogEntry) (entries.get(i)));
    }
    entries.clear();
  }

}// end of class FileLog
