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

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.SessionListener;

import org.knopflerfish.service.soap.remotefw.RemoteFW;

public class ConsoleSessionImpl implements Session {

  RemoteFW fw;
  String name;
  Reader in;
  PrintWriter out;
  char escapeChar;
  String interruptString;
  ConsoleReaderWriter rw;

  public ConsoleSessionImpl(RemoteFW fw, String name, Reader in, PrintWriter out) {
    this.fw  = fw;
    this.name = name;
    this.in = in;
    this.out = out;
    fw.createSession(name);
    rw = new ConsoleReaderWriter(fw, in, out);
  }

  public void abortCommand() {
    fw.abortCommand();
  }

  public void addSessionListener(SessionListener l) {
    //TODO?
  }

  public void close() {
    rw.stop();
    fw.closeSession();
  }

  public char getEscapeChar() {
    return escapeChar;
  }

  public String getInterruptString() {
    return interruptString;
  }

  public String getName() {
    return name;
  }

  public Dictionary getProperties() {
    return null;
    //TODO?
  }

  public void removeSessionListener(SessionListener l) {
    //TODO?
  }

  public void setEscapeChar(char escapeChar) {
    fw.setEscapeChar(escapeChar);
    this.escapeChar = escapeChar;
  }

  public void setInterruptString(String interruptString) {
    fw.setInterruptString(interruptString);
    this.interruptString = interruptString;
  }

}
