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

package org.knopflerfish.bundle.log.window.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

/**
 * LogEntry implementation with an extra ID field.
 */
public class ExtLogEntry implements LogEntry {
  LogEntry entry;
  long     id;

  public ExtLogEntry(LogEntry entry, long id) {
    this.entry = entry;
    this.id    = id;
  }

  public long getId() {
    return id;
  }

  public Bundle getBundle() {
    return entry.getBundle();
  }

  public ServiceReference<?> getServiceReference() {
    return entry.getServiceReference();
  }

  @Deprecated
  public int getLevel() {
    return entry.getLevel();
  }

  public String getMessage() {
    return entry.getMessage();
  }

  
  public Throwable getException() {
    return entry.getException();
  }
  
  public long getTime() {
    return entry.getTime();
  }

  @Override
  public LogLevel getLogLevel() {
	  return entry.getLogLevel();
  }

  @Override
  public String getLoggerName() {
	  return entry.getLoggerName();
  }

  @Override
  public long getSequence() {
	  return entry.getSequence();
  }

  @Override
  public String getThreadInfo() {
	  return entry.getThreadInfo();
  }

  @Override
  public StackTraceElement getLocation() {
	  return entry.getLocation();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getId());
    sb.append(": ").append(new Date(getTime()));
    sb.append("  ").append(getLogLevel());

    sb.append(" #").append(getBundle().getBundleId());
    sb.append(" ").append(Util.getBundleName(getBundle()));

    sb.append(" -  ").append(getMessage());
    if(getException() != null) {
      StringWriter w = new StringWriter();
      getException().printStackTrace(new PrintWriter(w));
      
      sb.append(", ");
      sb.append(w.toString());
    }

    return sb.toString();
  }

}
