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

import java.util.Date;
import java.text.SimpleDateFormat;

import org.knopflerfish.service.log.LogUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

/**
 * This class implements the LogEntry interface defined by OSGi.
 *
 */
public final class LogEntryImpl implements LogEntry {

  // The SimpleDateFormat class is not thread safe; wrap access to it
  // in synchronized methods.
  private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat();

  static void setTimestampPattern(final String pattern)
  {
    synchronized(simpleDateFormat) {
      try {
        simpleDateFormat.applyPattern(pattern);
      } catch (Throwable t) {
      }
    }
  }

  static String formatTimestamp(final Date date)
  {
    synchronized(simpleDateFormat) {
      return simpleDateFormat.format(date);
    }
  }

  // Log entry data.
  private final Bundle bundle;
  private final ServiceReference<?> serviceReference;
  private final int level;
  private final String message;
  private final Throwable throwable;
  private final long timestamp;

  private final String threadInfo;
  private StackTraceElement location;

  //TODO: assign proper values
  private final LogLevel logLevel = LogLevel.DEBUG;
  private final String loggerName = "";
  private final long sequence = 0;

  public LogEntryImpl(final Bundle bundle, final int level, final String message) {
    this(bundle, null, level, message, null);
  }

  public LogEntryImpl(final Bundle bundle, final int level, final String message,
                      final Throwable throwable) {
    this(bundle, null, level, message, throwable);
  }

  public LogEntryImpl(final Bundle bundle, final ServiceReference<?> serviceReference, final int level,
                      final String message) {
    this(bundle, serviceReference, level, message, null);
  }

  public LogEntryImpl(final Bundle bundle, final ServiceReference<?> serviceReference, final int level,
                      final String message,  final Throwable throwable) {
    this.bundle = bundle;
    this.serviceReference = serviceReference;
    this.level = level;
    this.message = message;
    this.throwable = throwable;
    this.timestamp = System.currentTimeMillis();
    this.threadInfo = Thread.currentThread().getName();
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    this.location = stackTrace.length == 0 ? null : stackTrace[0];
  }

  /**
   * Returns a string representing this log entry.
   * The format is:
   *
   * <pre>
   *  level    YYYYMMDD HH:MM:ss bid#NR - [Service] - Message (Exception)
   * </pre>
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(100);
    sb.append(LogUtil.fromLevel(level, 8));
    sb.append(" ");
    sb.append(formatTimestamp(new Date(timestamp)));
    sb.append(" ");
    sb.append("bid#");
    // Reserve 8 chars for the bundle id, pad with spaces if needed
    final int bidEndPos = sb.length() + 8;
    if (bundle != null) {
      sb.append(bundle.getBundleId());
    }
    if (sb.length() < bidEndPos) {
      sb.append("        ");
      sb.setLength(bidEndPos);
    }
    sb.append(" - ");
    if (serviceReference != null) {
      sb.append("[");
      sb.append(serviceReference.getProperty(Constants.SERVICE_ID).toString());
      sb.append(";");
      String[] clazzes = (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
      for (int i = 0; i < clazzes.length; i++) {
        if (i > 0)
          sb.append(",");
        sb.append(clazzes[i]);
      }
      sb.append("] ");
    }
    sb.append(message);
    if (throwable != null) {
      sb.append(" (");
      sb.append(throwable.toString());
      sb.append(")");
    }

    return sb.toString();
  }

  @Override
  public Bundle getBundle() {
    return bundle;
  }

  @Override
  public ServiceReference<?> getServiceReference() {
    return serviceReference;
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Throwable getException() {
    return throwable;
  }

  @Override
  public long getTime() {
    return timestamp;
  }

  @Override
  public LogLevel getLogLevel() {
	  return logLevel;
  }

  @Override
  public String getLoggerName() {
	  return loggerName;
  }

  @Override
  public long getSequence() {
	  return sequence;
  }

  @Override
  public String getThreadInfo() {
	  return threadInfo;
  }

  @Override
  public StackTraceElement getLocation() {
	  return location;
  }

}
