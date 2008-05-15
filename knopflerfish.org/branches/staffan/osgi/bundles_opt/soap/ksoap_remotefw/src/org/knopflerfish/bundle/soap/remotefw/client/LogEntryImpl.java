/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.ArrayList;
import java.util.Enumeration;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

public class LogEntryImpl implements LogEntry {

  RemoteFW fw;
  Bundle bundle;
  int level;
  Throwable exception;
  String message;
  long time;

  LogEntryImpl(RemoteFW fw,
               Bundle bundle,
               int level,
               String message,
               long time,
               Throwable exception) {
    this.fw  = fw;
    this.bundle = bundle;
    this.level = level;
    this.message = message;
    this.time = time;
    this.exception = exception;
  }

  LogEntryImpl(RemoteFW fw, Bundle bundle) {
    this.fw  = fw;
    this.bundle = bundle;
  }

  public Bundle getBundle() {
    return bundle;
  }

  public Throwable getException() {
    return exception;
  }

  public int getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public ServiceReference getServiceReference() {
    return null;
    //TODO?
  }

  public long getTime() {
    return time;
  }

}
