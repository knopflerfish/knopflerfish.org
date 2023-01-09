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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.Logger;

import static org.osgi.service.log.LogLevel.AUDIT;
import static org.osgi.service.log.LogLevel.DEBUG;
import static org.osgi.service.log.LogLevel.ERROR;
import static org.osgi.service.log.LogLevel.INFO;
import static org.osgi.service.log.LogLevel.TRACE;
import static org.osgi.service.log.LogLevel.WARN;

/**
 * LogRef is an utility class that simplifies the use of the LogService.
 * 
 * <P>
 * LogRef let you use the log without worrying about getting new
 * service objects when the log service is restarted. It also supplies methods
 * with short names that does logging with all the different LogService severity
 * types.
 * </P>
 * <P>
 * To use the LogRef you need to import
 * <code>org.knopflerfish.service.log.LogRef</code> and instantiate LogRef with
 * your bundle context as parameter. The bundle context is used for getting the
 * LogService and adding a service listener.
 * </P>
 * 
 * <H2>Example usage</H2>
 * 
 * The <code>if</code> statement that protects each call to the
 * <code>LogRef</code> instance below is there to save the effort required for
 * creating the message string object in cases where the log will throw away the
 * log entry due to its unimportance. The user must have this <code>if</code>
 * -test in his code since that is the only way to avoid constructing the string
 * object. Placing it in the wrapper (LogRef) will not help due to the design of
 * the Java programming language.
 * 
 * <PRE>
 * package org.knopflerfish.example.hello;
 * 
 * import org.osgi.framework.*;
 * import org.knopflerfish.service.log.LogRef;
 * 
 * public class Hello
 *   implements BundleActivator
 * {
 *   LogRef log;
 * 
 *   public void start(BundleContext bundleContext)
 *   {
 *     log = new LogRef(bundleContext);
 *     if (log.doInfo())
 *       log.info(&quot;Hello started.&quot;);
 *   }
 * 
 *   public void stop(BundleContext bundleContext)
 *   {
 *     if (log.doDebug())
 *       log.debug(&quot;Hello stopped.&quot;);
 *   }
 * }
 * </PRE>
 * 
 * @author Gatespace AB
 * 
 * @see org.osgi.service.log.LogService
 * @see org.knopflerfish.service.log.LogService
 */

public class LogRef
  implements ServiceListener, LogService
{
  // Class name of the OSGI log service
  private final static String LOG_CLASS_OSGI = org.osgi.service.log.LogService.class
      .getName();

  // Class name of Knopflerfish extended log service
  private final static String LOG_CLASS_KF = org.knopflerfish.service.log.LogService.class
      .getName();

  private final static String logServiceFilter = "(|" + "(objectClass="
                                                 + LOG_CLASS_KF
                                                 + ")(objectClass="
                                                 + LOG_CLASS_OSGI + "))";

  // Date formatter used then sending entries to System.out
  private static SimpleDateFormat simpleDateFormat = null;

  // Handle to the framework
  private BundleContext bc;

  // Logger name (TODO)
  private String name;

  // Service reference for the current log service
  private ServiceReference<?> logSR;

  // The current log service
  private org.osgi.service.log.LogService log;

  // If true and no log service, print on System.out
  private boolean useOut;

  // The id of the calling bundle
  private long bundleId;

  // If true warn about using closed LogRef object
  private boolean doWarnIfClosed;

  /**
   * Create new LogRef object for a given bundle.
   * 
   * @param bc
   *          the bundle context of the bundle that this log ref instance
   *          belongs too.
   * @param out
   *          If true print messages on <code>System.out</code> when there is no
   *          log service.
   */
  public LogRef(BundleContext bc, boolean out)
  {
    init(bc, out, LogRef.class.getName());
  }

  /**
   * Create new LogRef object for a given bundle.
   * 
   * <p>
   * If the system property <tt>org.knopflerfish.log.out</tt> equals "true",
   * system.out will be used as fallback if no log service is found.
   * </p>
   * 
   * @param bc
   *          the bundle context of the bundle that this log ref instance
   *          belongs too.
   */
  public LogRef(BundleContext bc)
  {
    boolean b = false;

    try {
      b = "true".equals(bc.getProperty("org.knopflerfish.log.out"));
    } catch (Throwable t) {
      System.err.println("get system property failed: " + t);
      t.printStackTrace();
    }

    init(bc, b, LogRef.class.getName());
  }

  /**
   * Create new LogRef object for a given bundle.
   *
   * <p>
   * If the system property <tt>org.knopflerfish.log.out</tt> equals "true",
   * system.out will be used as fallback if no log service is found.
   * </p>
   *
   * @param bc
   *          the bundle context of the bundle that this log ref instance
   *          belongs too.
   *
   * @param name
   *          the name of the logger
   */
  public LogRef(BundleContext bc, String name)
  {
    boolean b = false;

    try {
      b = "true".equals(bc.getProperty("org.knopflerfish.log.out"));
    } catch (Throwable t) {
      System.err.println("get system property failed: " + t);
      t.printStackTrace();
    }

    init(bc, b, name);
  }

  private void init(BundleContext bc, boolean out, String name)
  {
    this.bc = bc;
    this.name = name;
    useOut = out;
    bundleId = bc.getBundle().getBundleId();
    try {
      bc.addServiceListener(this, logServiceFilter);
    } catch (InvalidSyntaxException e) {
      error("Failed to register log service listener (filter="
            + logServiceFilter + ")", e);
    }
  }

  /**
   * Service listener entry point. Releases the log service object if one has
   * been fetched.
   * 
   * @param evt
   *          Service event
   */
  public void serviceChanged(ServiceEvent evt)
  {
    if (evt.getServiceReference() == logSR
        && evt.getType() == ServiceEvent.UNREGISTERING) {
      ungetLogService();
    }
  }

  /**
   * Unget the log service. Note that this method is synchronized on the same
   * object as the internal method that calls the actual log service. This
   * ensures that the log service is not removed by this method while a log
   * message is generated.
   */
  private synchronized void ungetLogService()
  {
    doWarnIfClosed = doDebug();
    if (log != null) {
      bc.ungetService(logSR);
      logSR = null;
      log = null;
    }
  }

  /**
   * Close this LogRef object. Ungets the log service if active.
   */
  public void close()
  {
    ungetLogService();
    bc.removeServiceListener(this);
    bc = null;
  }

  /**
   * Sends a message to the log if possible.
   * 
   * @param msg
   *          Human readable string describing the condition.
   * @param level
   *          The severity of the message (Should be one of the four predefined
   *          severities).
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param e
   *          The exception that reflects the condition.
   * @deprecated Replaced by {@link #doLog(String, LogLevel, ServiceReference, Throwable)}.
   */
  @Deprecated
  protected synchronized void doLog(String msg,
                                    int level,
                                    ServiceReference<?> sr,
                                    Throwable e)
  {
    doLog(msg, LogUtil.ordinalToLogLevel(level), sr, e);
  }

  /**
   * Sends a message to the log if possible.
   *
   * @param msg
   *          Human readable string describing the condition.
   * @param level
   *          The severity of the message.
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param e
   *          The exception that reflects the condition.
   */
  protected synchronized void doLog(String msg,
                                    LogLevel level,
                                    ServiceReference<?> sr,
                                    Throwable e)
  {
    refreshService();
    if (log != null) {
      doLog(getLogger(), level, msg, e);
    } else {
      doLogToSystemOut(msg, level, sr, e);
    }
  }

  private void refreshService() {
    if (bc != null && log == null) {
      try {
        logSR = bc
            .getServiceReference(LogService.class);
        if (logSR == null) {
          // No service implementing the Knopflerfish
          // extended log, try to look for a standard OSGi
          // log service.
          logSR = bc.getServiceReference(org.osgi.service.log.LogService.class);
        }
        if (logSR != null) {
          log = (org.osgi.service.log.LogService) bc.getService(logSR);
        }
        if (log == null) {
          // Failed to get log service clear the service reference.
          logSR = null;
        }
      } catch (IllegalStateException ise) {
        // Bundle not active, can not fetch a log service.
        bc = null;
        log = null;
        logSR = null;
      }
    }
  }

  private Logger getLogger() {
    if (bc == null) {
      return log.getLogger(name, Logger.class);
    }
    return log.getLogger(bc.getBundle(), name, Logger.class);
  }

  private void doLogToSystemOut(String msg, LogLevel level, ServiceReference<?> sr, Throwable e) {
    if (useOut || doWarnIfClosed) {
      if (bc == null) {
        System.err.println("WARNING! Bundle #" + bundleId
                           + " called closed LogRef object");
      }
      // No log service and request for messages on System.out
      System.out.print(LogUtil.fromLogLevel(level, 8));
      System.out.print(" ");
      if (simpleDateFormat == null) {
        simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
      }
      System.out.print(simpleDateFormat.format(new Date()));
      System.out.print(" ");
      System.out.print(getBundleName());
      System.out.print(" - ");
      if (sr != null) {
        System.out.print("[");
        System.out.print(sr);
        System.out.print("] ");
      }
      System.out.print(msg);
      if (e != null) {
        System.out.print(" (");
        System.out.print(e.toString());
        System.out.print(")");

        System.out.println();
        e.printStackTrace();
      }
      System.out.println();
    }
  }

  private void doLog(Logger logger, LogLevel level, String msg, Throwable e) {
    switch (level) {
      case AUDIT:
        logger.audit(msg, e);
        break;
      case ERROR:
        if (logger.isErrorEnabled()) {
          logger.error(msg, e);
        }
        break;
      case WARN:
        if (logger.isWarnEnabled()) {
          logger.warn(msg, e);
        }
        break;
      case INFO:
        if (logger.isInfoEnabled()) {
          logger.info(msg, e);
        }
        break;
      case DEBUG:
        if (logger.isDebugEnabled()) {
          logger.debug(msg, e);
        }
        break;
      case TRACE:
        if (logger.isTraceEnabled()) {
          logger.trace(msg, e);
        }
        break;
    }
  }

  /**
   * Returns the current log level. There is no use to generate log entries with
   * a severity level less than this value since such entries will be thrown
   * away by the log.
   * 
   * @return the current severity log level for this bundle.
   * @deprecated Replaced by {@link LogRef#getCurrentLogLevel()}.
   */
  @Deprecated
  @Override
  public int getLogLevel()
  {
    return getCurrentLogLevel().ordinal();
  }

  /**
   * Returns the current log level. There is no use to generate log entries with
   * a severity level less than this value since such entries will be thrown
   * away by the log.
   *
   * @return the current severity log level for this bundle.
   */
  @Override
  public LogLevel getCurrentLogLevel()
  {
    if (log != null && (log instanceof LogService)) {
      return ((LogService) log).getCurrentLogLevel();
    }
    return DEBUG;
  }

  /**
   * Returns true if messages with severity debug or higher are saved by the
   * log.
   * 
   * @return <code>true</code> if messages with severity DEBUG or higher are
   *         included in the log, otherwise <code>false</code>.
   */
  public boolean doDebug()
  {
    return doLogLevel(DEBUG);
  }

  /**
   * Returns true if messages with severity warning or higher are saved by the
   * log.
   * 
   * @return <code>true</code> if messages with severity WARN or higher
   *         are included in the log, otherwise <code>false</code>.
   */
  public boolean doWarn()
  {
    return doLogLevel(WARN);
  }

  /**
   * Returns true if messages with severity info or higher are saved by the log.
   * 
   * @return <code>true</code> if messages with severity INFO or higher are
   *         included in the log, otherwise <code>false</code>.
   */
  public boolean doInfo()
  {
    return doLogLevel(INFO);
  }

  /**
   * Returns true if messages with severity error or higher are saved by the
   * log.
   * 
   * @return <code>true</code> if messages with severity ERROR or higher are
   *         included in the log, otherwise <code>false</code>.
   */
  public boolean doError()
  {
    return doLogLevel(ERROR);
  }

  private boolean doLogLevel(LogLevel logLevel) {
    return getCurrentLogLevel().implies(logLevel);
  }

  /**
   * Log a trace level message
   * 
   * @param msg
   *          Log message.
   */
  public void trace(String msg)
  {
    doLog(msg, TRACE, null, null);
  }

  /**
   * Log a debug level message
   *
   * @param msg
   *          Log message.
   */
  public void debug(String msg)
  {
    doLog(msg, DEBUG, null, null);
  }

  /**
   * Log a debug level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   */
  public void debug(String msg, ServiceReference<?> sr)
  {
    doLog(msg, DEBUG, sr, null);
  }

  /**
   * Log a debug level message.
   * 
   * @param msg
   *          Log message
   * @param e
   *          The exception that reflects the condition.
   */
  public void debug(String msg, Throwable e)
  {
    doLog(msg, DEBUG, null, e);
  }

  /**
   * Log a debug level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param e
   *          The exception that reflects the condition.
   */
  public void debug(String msg, ServiceReference<?> sr, Throwable e)
  {
    doLog(msg, DEBUG, sr, e);
  }

  /**
   * Log an info level message.
   * 
   * @param msg
   *          Log message
   */
  public void info(String msg)
  {
    doLog(msg, INFO, null, null);
  }

  /**
   * Log an info level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   */
  public void info(String msg, ServiceReference<?> sr)
  {
    doLog(msg, INFO, sr, null);
  }

  /**
   * Log an info level message.
   * 
   * @param msg
   *          Log message
   * @param e
   *          The exception that reflects the condition.
   */
  public void info(String msg, Throwable e)
  {
    doLog(msg, INFO, null, e);
  }

  /**
   * Log an info level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param e
   *          The exception that reflects the condition.
   */
  public void info(String msg, ServiceReference<?> sr, Throwable e)
  {
    doLog(msg, INFO, sr, e);
  }

  /**
   * Log a warning level message.
   * 
   * @param msg
   *          Log message
   */
  public void warn(String msg)
  {
    doLog(msg, WARN, null, null);
  }

  /**
   * Log a warning level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   */
  public void warn(String msg, ServiceReference<?> sr)
  {
    doLog(msg, WARN, sr, null);
  }

  /**
   * Log a warning level message.
   * 
   * @param msg
   *          Log message
   * @param e
   *          The exception that reflects the condition.
   */
  public void warn(String msg, Throwable e)
  {
    doLog(msg, WARN, null, e);
  }

  /**
   * Log a warning level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param e
   *          The exception that reflects the condition.
   */
  public void warn(String msg, ServiceReference<?> sr, Throwable e)
  {
    doLog(msg, WARN, sr, e);
  }

  /**
   * Log an error level message.
   * 
   * @param msg
   *          Log message
   */
  public void error(String msg)
  {
    doLog(msg, ERROR, null, null);
  }

  /**
   * Log an error level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   */
  public void error(String msg, ServiceReference<?> sr)
  {
    doLog(msg, ERROR, sr, null);
  }

  /**
   * Log an error level message.
   * 
   * @param msg
   *          Log message
   * @param e
   *          The exception that reflects the condition.
   */
  public void error(String msg, Throwable e)
  {
    doLog(msg, ERROR, null, e);
  }

  /**
   * Log an error level message.
   * 
   * @param msg
   *          Log message
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param e
   *          The exception that reflects the condition.
   */
  public void error(String msg, ServiceReference<?> sr, Throwable e)
  {
    doLog(msg, ERROR, sr, e);
  }

  /**
   * Log an audit level message
   *
   * @param msg
   *          Log message.
   */
  public void audit(String msg)
  {
    doLog(msg, AUDIT, null, null);
  }

  /**
   * Log a message. The ServiceDescription field and the Throwable field of the
   * LogEntry will be set to null.
   * 
   * @param level
   *          The severity of the message.
   * @param message
   *          Human readable string describing the condition.
   */
  public void log(LogLevel level, String message)
  {
    doLog(message, level, null, null);
  }

  /**
   * @deprecated Replaced by {@link #log(LogLevel, String)}.
   */
  @Deprecated
  @Override
  public void log(int level, String message)
  {
    doLog(message, level, null, null);
  }

  /**
   * Log a message with an exception. The ServiceDescription field of the
   * LogEntry will be set to null.
   * 
   * @param level
   *          The severity of the message.
   * @param message
   *          Human readable string describing the condition.
   * @param exception
   *          The exception that reflects the condition.
   */
  public void log(LogLevel level, String message, Throwable exception)
  {
    doLog(message, level, null, exception);
  }

  /**
   * @deprecated Replaced by {@link #log(LogLevel, String)}.
   */
  @Deprecated
  @Override
  public void log(int level, String message, Throwable exception)
  {
    doLog(message, level, null, exception);
  }

  /**
   * Log a message associated with a specific Service. The Throwable field of
   * the LogEntry will be set to null.
   * 
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param level
   *          The severity of the message.
   * @param message
   *          Human readable string describing the condition.
   */
  public void log(ServiceReference<?> sr,
                  LogLevel level,
                  String message)
  {
    doLog(message, level, sr, null);
  }

  /**
   * @deprecated Replaced by {@link #log(ServiceReference, LogLevel, String)}.
   */
  @Deprecated
  @Override
  public void log(ServiceReference<?> sr,
                  int level,
                  String message)
  {
    doLog(message, level, sr, null);
  }

  /**
   * Log a message with an exception associated with a specific Service.
   * 
   * @param sr
   *          The <code>ServiceReference</code> of the service that this message
   *          is associated with.
   * @param level
   *          The severity of the message.
   * @param message
   *          Human readable string describing the condition.
   * @param exception
   *          The exception that reflects the condition.
   */
  public void log(ServiceReference<?> sr,
                  LogLevel level,
                  String message,
                  Throwable exception)
  {
    doLog(message, level, sr, exception);
  }

  /**
   * @deprecated Replaced by {@link #log(ServiceReference, LogLevel, String, Throwable)}.
   */
  @Deprecated
  @Override
  public void log(ServiceReference<?> sr,
                  int level,
                  String message,
                  Throwable exception)
  {
    doLog(message, level, sr, exception);
  }

  @Override
  public Logger getLogger(String name) {
	  return log.getLogger(name);
  }

  @Override
  public Logger getLogger(Class<?> clazz) {
	  return log.getLogger(clazz);
  }

  @Override
  public <L extends Logger> L getLogger(String name, Class<L> loggerType) {
	  return log.getLogger(name, loggerType);
  }

  @Override
  public <L extends Logger> L getLogger(Class<?> clazz, Class<L> loggerType) {
	  return log.getLogger(clazz, loggerType);
  }

  @Override
  public <L extends Logger> L getLogger(Bundle bundle, String name, Class<L> loggerType) {
	  return log.getLogger(bundle, name, loggerType);
  }

  /**
   * Returns a human readable name for the bundle that <code>bc</code>
   * represents.
   * 
   * @return Name of the bundle that uses this wrapper (at least 12 characters).
   */
  private String getBundleName()
  {
    // We can't get bundle-name since it requires AdminPermission.
    return String.format("bid#%1$-8s", bundleId);
  }

}
