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

package org.knopflerfish.bundle.commons.logging;

import org.osgi.framework.*;
import org.knopflerfish.service.log.LogRef;

import org.apache.commons.logging.*;

public class LogOSGI implements  Log {

  String name;

  public LogOSGI(String name) {
    this.name = name;
  }

  public boolean isDebugEnabled() {
    return Activator.log.doDebug();
  }

  public boolean isErrorEnabled() {
    return Activator.log.doError();
  }

  public boolean isFatalEnabled() {
    return Activator.log.doError();
  }


  public boolean isInfoEnabled() {
    return Activator.log.doInfo();
  }

  public boolean isTraceEnabled() {
    return Activator.log.doDebug();
  }
  
  public boolean isWarnEnabled() {
    return Activator.log.doWarn();
  }
  
  public void trace(Object message) {
    Activator.log.debug(name + "/trace: " + message);
  }
  

  public void trace(Object message, Throwable t) {
    Activator.log.debug(name + "/trace: " + message, t);
  }


  public void debug(Object message) {
    Activator.log.debug(name + ": " + message);
  }

  public void debug(Object message, Throwable t) {
    Activator.log.debug(name + ":" + message, t);
  }

  public void info(Object message) {
    Activator.log.info(name + ": " + message);
  }


  public void info(Object message, Throwable t) {
    Activator.log.info(name + ": "+ message, t);
  }

  public void warn(Object message) {
    Activator.log.warn(name + ": " + message);
  }


  public void warn(Object message, Throwable t) {
    Activator.log.warn(name + ": " + message, t);
  }

  public void error(Object message) {
    Activator.log.error(name + ": " + message);
  }

  public void error(Object message, Throwable t) {
    Activator.log.error(name + ": " + message, t);
  }

  public void fatal(Object message) {
    Activator.log.error(name + "/fatal: " + message);
  }

  public void fatal(Object message, Throwable t) {
    Activator.log.error(name + "/fatal: " + message, t);
  }
}
