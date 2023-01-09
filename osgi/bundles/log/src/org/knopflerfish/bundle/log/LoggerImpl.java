/*
 * Copyright (c) 2022, KNOPFLERFISH project
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

import org.osgi.framework.BundleContext;
import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LoggerConsumer;

import org.knopflerfish.service.log.LogRef;

public class LoggerImpl implements FormatterLogger {

  private final String name;
  private final LogRef logRef;

  LoggerImpl(String name, BundleContext bc) {
    this.name = name;
    logRef = new LogRef(bc);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isTraceEnabled() {
    return isEnabled(LogLevel.TRACE);
  }

  @Override
  public void trace(String message) {
    logRef.trace(message);
  }

  @Override
  public void trace(String format, Object arg) {
    logRef.trace(String.format(format, arg));
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    logRef.trace(String.format(format, arg1, arg2));
  }

  @Override
  public void trace(String format, Object... arguments) {
    logRef.trace(String.format(format, arguments));
  }

  @Override
  public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
    consumer.accept(this);
  }

  @Override
  public boolean isDebugEnabled() {
    return isEnabled(LogLevel.DEBUG);
  }

  @Override
  public void debug(String message) {
    logRef.debug(message);
  }

  @Override
  public void debug(String format, Object arg) {
    logRef.debug(String.format(format, arg));
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    logRef.debug(String.format(format, arg1, arg2));
  }

  @Override
  public void debug(String format, Object... arguments) {
    logRef.debug(String.format(format, arguments));
  }

  @Override
  public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
    consumer.accept(this);
  }

  @Override
  public boolean isInfoEnabled() {
    return isEnabled(LogLevel.INFO);
  }

  @Override
  public void info(String message) {
    logRef.info(message);
  }

  @Override
  public void info(String format, Object arg) {
    logRef.info(String.format(format, arg));
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    logRef.info(String.format(format, arg1, arg2));
  }

  @Override
  public void info(String format, Object... arguments) {
    logRef.info(String.format(format, arguments));
  }

  @Override
  public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
    consumer.accept(this);
  }

  @Override
  public boolean isWarnEnabled() {
    return isEnabled(LogLevel.WARN);
  }

  @Override
  public void warn(String message) {
    logRef.warn(message);
  }

  @Override
  public void warn(String format, Object arg) {
    logRef.warn(String.format(format, arg));
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    logRef.warn(String.format(format, arg1, arg2));
  }

  @Override
  public void warn(String format, Object... arguments) {
    logRef.warn(String.format(format, arguments));
  }

  @Override
  public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
    consumer.accept(this);
  }

  @Override
  public boolean isErrorEnabled() {
    return isEnabled(LogLevel.ERROR);
  }

  @Override
  public void error(String message) {
    logRef.error(message);
  }

  @Override
  public void error(String format, Object arg) {
    logRef.error(String.format(format, arg));
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    logRef.error(String.format(format, arg1, arg2));
  }

  @Override
  public void error(String format, Object... arguments) {
    logRef.error(String.format(format, arguments));
  }

  @Override
  public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
    consumer.accept(this);
  }

  @Override
  public void audit(String message) {
    logRef.audit(message);
  }

  @Override
  public void audit(String format, Object arg) {
    logRef.audit(String.format(format, arg));
  }

  @Override
  public void audit(String format, Object arg1, Object arg2) {
    logRef.audit(String.format(format, arg1, arg2));
  }

  @Override
  public void audit(String format, Object... arguments) {
    logRef.audit(String.format(format, arguments));
  }

  private boolean isEnabled(LogLevel logLevel) {
    return logRef.getCurrentLogLevel().implies(logLevel);
  }

}
