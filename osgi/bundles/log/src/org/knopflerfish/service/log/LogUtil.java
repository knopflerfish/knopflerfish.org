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

package org.knopflerfish.service.log;

import org.osgi.service.log.LogLevel;

/**
 * Utility class for the LogService interface.
 *
 * It exports methods that translates between the numeric values of
 * the severity level constants and human readable strings.
 */
public class LogUtil {

  /**
   * Converts from a numeric log severity level to a string.
   *
   * @param level
   *            is the log severity level.
   * @return string representation of a numeric log level.
   * @deprecated Replaced by {@link #fromLogLevel(LogLevel)}.
   */
  @Deprecated
  public static String fromLevel(int level) {
    return fromLevel(level, 0);
  }

  /**
   * Converts from a numeric log severity level to a left justified
   * string of at least the given length.
   *
   * @param level
   *            is the log severity level.
   * @param length
   *            the minimum length of the resulting string.
   * @return left justified string representation of a numeric log level.
   * @deprecated Replaced by {@link #fromLogLevel(LogLevel, int)}.
   */
  @Deprecated
  public static String fromLevel(int level, int length) {
    return fromLogLevel(ordinalToLogLevel(level), length);
  }

  /**
   * Converts from a log severity level to a string.
   *
   * @param logLevel
   *            is the log severity level.
   * @return string representation of a log level.
   */
  public static String fromLogLevel(LogLevel logLevel) {
    return fromLogLevel(logLevel, 0);
  }

  /**
   * Converts from a log severity level to a left justified
   * string of at least the given length.
   *
   * @param logLevel
   *            is the log severity level.
   * @param length
   *            the minimum length of the resulting string.
   * @return left justified string representation of a log level.
   */
  public static String fromLogLevel(LogLevel logLevel, int length) {
    final StringBuilder sb = new StringBuilder(Math.max(length, 7));
    sb.append(toString(logLevel));
    while (sb.length() < length) {
      sb.append(" ");
    }
    return sb.toString();
  }

  private static String toString(LogLevel logLevel) {
    switch (logLevel) {
      case AUDIT:
        return "audit";
      case INFO:
        return "info";
      case DEBUG:
        return "debug";
      case WARN:
        return "Warning";
      case ERROR:
        return "ERROR";
      case TRACE:
        return "trace";
      default:
        return "[" + logLevel + "]";
    }
  }

  public static LogLevel ordinalToLogLevel(int ordinal) {
    final LogLevel[] logLevels = LogLevel.values();
    if (ordinal < 0 || ordinal >= logLevels.length) {
      throw new IllegalArgumentException("Unknown log level: " + ordinal);
    }
    return logLevels[ordinal];
  }

  /**
   * Converts a string representing a log severity level to an int.
   *
   * @param level
   *            The string to convert.
   * @param def
   *            Default value to use if the string is not recognized
   *            as a log level.
   * @return the log level, or the default value if the string can not
   *         be recognized.
   * @deprecated Replaced by {@link #toLogLevel(String, LogLevel)}.
   */
  @Deprecated
  public static int toLevel(String level, int def) {
    final LogLevel logLevel = toLogLevel(level);
    if (logLevel == null) {
      return def;
    }
    return logLevel.ordinal();
  }

  /**
   * Converts a string representing a log severity level to a LogLevel.
   *
   * @param level
   *            The string to convert.
   * @param def
   *            Default value to use if the string is not recognized
   *            as a log level.
   * @return the log level, or the default value if the string can not
   *         be recognized.
   */
  public static LogLevel toLogLevel(String level, LogLevel def) {
    LogLevel logLevel = toLogLevel(level);
    if (logLevel == null) {
      return def;
    }
    return logLevel;
  }

  private static LogLevel toLogLevel(String level) {
    if (level == null) {
      return null;
    }

    level = level.trim();
    if (level.equalsIgnoreCase("INFO")) {
      return LogLevel.INFO;
    } else if (level.equalsIgnoreCase("TRACE")) {
      return LogLevel.TRACE;
    } else if (level.equalsIgnoreCase("DEBUG")) {
      return LogLevel.DEBUG;
    } else if (level.equalsIgnoreCase("WARNING")) {
      return LogLevel.WARN;
    } else if (level.equalsIgnoreCase("ERROR")) {
      return LogLevel.ERROR;
    } else if (level.equalsIgnoreCase("AUDIT")) {
      return LogLevel.AUDIT;
    }
    return null;
  }

}
