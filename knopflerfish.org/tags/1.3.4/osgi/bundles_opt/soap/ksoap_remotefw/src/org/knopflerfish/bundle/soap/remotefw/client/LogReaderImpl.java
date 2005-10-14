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
import java.util.Iterator;
import java.util.Vector;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

public class LogReaderImpl implements LogReaderService {

  boolean bDebug = "true".equals(System.getProperty("org.knopflerfish.soap.remotefw.client.debug", "false"));
  long    delay  = 3 * 1000;
  Thread  runner = null;
  boolean bRun   = false;
  boolean bInEvent = false;
  Object eventLock = new Object();

  RemoteFW fw;

  ArrayList logListeners = new ArrayList();
  //TODO: poll and call

  LogReaderImpl(RemoteFW fw) {
    this.fw  = fw;

    try {
      delay = Long.parseLong(System.getProperty("org.knopflerfish.soap.remotefw.client.eventinterval", Long.toString(delay)));
    } catch (Exception e) {
    }
    start();
  }

  void start() {
    if(runner == null) {
      runner = new Thread() {
          public void run() {
            while(bRun) {
              try {
                if(!bInEvent) {
                  if(bDebug) {
                    System.out.println("doEvents");
                  }
                  doEvents();
                }
                if(bDebug) {
                  System.out.println("sleep " + delay);
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
              try {
                Thread.sleep(delay);
              } catch (InterruptedException ignore) {}
            }
          }
        };
      bRun = true;
      runner.start();
    }
  }

  void stop() {
    if(runner != null) {
      bRun = false;
      try {
        runner.wait(delay * 2);
      } catch (Exception ignore) {
      }
      runner = null;
    }
  }

  void doEvents() {
    synchronized(eventLock) {
      try {
        if(bInEvent) {
          Exception e = new RuntimeException("already in doEvents");
          e.printStackTrace();
          return;
        }
        bInEvent = true;
        if(bDebug) {
          System.out.println("doLogEvents ");
        }

        Vector vector = fw.getLog();
        for (Iterator events = vector.iterator(); events.hasNext();) {
          LogEntry entry = (LogEntry) events.next();
          for (Iterator listeners = logListeners.iterator(); listeners.hasNext();) {
            LogListener listener = (LogListener) listeners.next();
            listener.logged(entry);
          }
        }

        if(bDebug) {
          System.out.println("done doLogEvents ");
        }
      } finally {
        bInEvent = false;
      }
    }
  }

  public void addLogListener(LogListener listener) {
    logListeners.add(listener);
  }

  public Enumeration getLog() {
    return fw.getFullLog().elements();
  }

  public void removeLogListener(LogListener listener) {
    logListeners.remove(listener);
  }

}
