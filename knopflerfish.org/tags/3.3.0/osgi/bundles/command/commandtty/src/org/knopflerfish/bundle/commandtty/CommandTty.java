/*
 * Copyright (c) 2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.commandtty;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.command.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class CommandTty implements 
                          BundleActivator,
                          ManagedService,
                          ServiceTrackerCustomizer {
  
  final static private String NONBLOCKING = "nonblocking";
  
  boolean nonblocking = false;
  
  private ServiceTracker cmdProcTracker;  
  private ServiceTracker logTracker;  
  private CommandSession commandSession = null;
  
  private BundleContext bc;  

  InputStream inStream;
  PrintStream outStream;
  PrintStream errStream;

  ReadThread  readThread;
  public void start(BundleContext bc) throws Exception {
    this.bc = bc;
    
    log(LogService.LOG_INFO, "Starting");
    
    // Get config
    Hashtable p = new Hashtable();
    p.put(Constants.SERVICE_PID, getClass().getName());
    bc.registerService(ManagedService.class.getName(), this, p);
    
    inStream  = new SystemIn(bc);
    outStream = System.out;
    errStream = System.err;
    
    cmdProcTracker = new ServiceTracker(bc, CommandProcessor.class.getName(), this);
    cmdProcTracker.open();

    logTracker = new ServiceTracker(bc, LogService.class.getName(), null);
    logTracker.open();

  }
  

  public synchronized void stop(BundleContext bc) {
    log(LogService.LOG_INFO, "Stopping");
    cmdProcTracker.close();
    closeSession();
  }
  
  /*---------------------------------------------------------------------------*
   *			  ManagedService implementation
   *---------------------------------------------------------------------------*/
  
  public synchronized void updated(Dictionary cfg)
    throws IllegalArgumentException {
    if (cfg != null) {
      Boolean b = (Boolean) cfg.get(NONBLOCKING);
      if (b != null) {
        nonblocking = b.booleanValue();
      }
    } else {
      nonblocking = false;
    }
  }
  
  public Object addingService(ServiceReference reference) {
    CommandProcessor cmdProcessor = (CommandProcessor) bc.getService(reference);
    try {
      closeSession();
      commandSession = cmdProcessor.createSession(inStream, outStream, errStream);
      readThread = new ReadThread(inStream, commandSession);
      readThread.start();
    } catch (Exception ioe) {
      log(LogService.LOG_ERROR,
          "Failed to start command session, can not continue");
    }
    return cmdProcessor;
  }
  
  public void modifiedService(ServiceReference reference, Object service) {
  }
  
  public void removedService(ServiceReference reference, Object service) {
    closeSession();
  }

  void closeSession() {
    if (commandSession != null) {
      if(readThread != null) {
        readThread.stop();
        readThread = null;
      }
      commandSession.close();
      commandSession = null;
    }
  }
  
  public void log(int level, String msg) {
    log(level, msg, null);
  }
  
  public void log(int level, String msg, Exception e) {
    if(logTracker != null) {
      LogService sLog = (LogService)logTracker.getService();
      if (sLog != null) {
        if (e == null) {
          sLog.log(level, msg);
        } else {
          sLog.log(level, msg, e);
        }
        return;
      }
    }
    System.out.println("LOG " + level + ": " + msg);
    if(e != null) {
      e.printStackTrace();
    }
  }
  
  /**
   * Help class for VMs that when blocking in read() om System.in 
   * block other threads too.
   */
  class SystemIn extends InputStream {
    InputStream in;    
    SystemIn(BundleContext bc) {
      try {
        ServiceReference[] srl = bc.getServiceReferences(
                                                         InputStream.class.getName(),
                                                         "(service.pid=java.lang.System.in)");
        if (srl != null && srl.length == 1) {
          in = (InputStream) bc.getService(srl[0]);
        }
      } catch (Exception e) {
        e.printStackTrace(System.err);
      }
      if (in == null) {
        in = System.in;
      }
    }
    
    public void close() throws IOException {
      in.close();
    }
    
    public int available() throws IOException {
      return in.available();
    }
    
    public int read() throws IOException {
      byte[] b = new byte[1];
      if (read(b) == 1) {
        return b[0];
      }
      return -1;
    }
    
    public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
    }
    
    public int read(byte[] buf, int off, int len) throws IOException {
      if (nonblocking) {
        int nap = 50;
        while (in.available() == 0) {
          try {
            Thread.sleep(nap);
          } catch (InterruptedException e) {
          }
          nap = 200;
        }
      }
      return in.read(buf, off, len);
    }
  }  
  
  public class ReadThread extends Thread {    
    CommandSession    session;
    InputStream       in;
    BufferedReader    reader;
    InputStreamReader isReader;
    boolean bOpen;

    public ReadThread(InputStream in, CommandSession session) {
      super("Reader thread " + session.getClass().getName() + "@" + session.hashCode());
      this.in      = in;
      this.session = session;
      
      isReader = new InputStreamReader(in);
      reader   = new BufferedReader(isReader);

      bOpen = true;
    }
    
    public void run() {
      System.out.println("reading...\n");
      while (bOpen) {
        try {
          outStream.print("> ");
          String line = reader.readLine();
          if(bOpen) {
            log(LogService.LOG_INFO, "exec '" + line + "'");
            try {
              session.execute(line);
            } catch (Exception e) {
              log(LogService.LOG_ERROR, "Failed to exec " + line, e);
            }
          } else {
            // closed
          }
        } catch (InterruptedIOException ignore) {
          // If we are interrupted, we check bOpen flag and try to read
          // again.
          continue;
        } catch (IOException ignore) {
          bOpen = false;
        }
      }
    }
    
    public void close() {
      bOpen = false;
      this.interrupt();
    }
  }
}
