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
