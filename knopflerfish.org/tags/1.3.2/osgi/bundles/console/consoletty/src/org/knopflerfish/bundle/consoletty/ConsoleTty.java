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

package org.knopflerfish.bundle.consoletty;

import java.util.*;
import java.io.*;

import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.knopflerfish.service.console.*;
import org.osgi.service.cm.*;

//  ********************     ConsoleTty     ********************
/**
 ** Bundle activator implementation.
 **
 ** @author Jan Stein
 ** @version $Revision: 1.1.1.1 $
 */
public class ConsoleTty implements BundleActivator, ManagedService {

  final static private String logServiceName = LogService.class.getName();
  final static private String consoleServiceName = ConsoleService.class.getName();

  final static private String NONBLOCKING = "nonblocking";
  private boolean nonblocking = false;

  private ServiceReference srefConsole;
  private ConsoleService sConsole = null;
  private Session consoleSession = null;
  private ServiceRegistration configSR;
  private BundleContext  bc;

  /*---------------------------------------------------------------------------*
   *			  BundleActivator implementation
   *---------------------------------------------------------------------------*/
  
  //  ====================    start      ====================
  /**
   ** Called by the framework when this bundle is started.
   **
   ** @param bc Bundle context.
   ** @exception BundleException shold be thrown when implemented
   */
  public void start(BundleContext bc) throws Exception {
    this.bc = bc;
    
    log(LogService.LOG_INFO, "Starting");

    // Get config
    Hashtable p = new Hashtable();
    p.put(Constants.SERVICE_PID,getClass().getName());
    configSR = bc.registerService(ManagedService.class.getName(), this, p);

    // Get console service
    srefConsole = bc.getServiceReference(consoleServiceName);

    if (srefConsole == null) {
      log(LogService.LOG_ERROR, "Failed to get ConsoleService reference, can not continue");
      return;
    }

    sConsole = (ConsoleService) bc.getService(srefConsole);
    if (sConsole == null) {
      log(LogService.LOG_ERROR, "Failed to get ConsoleService, can not continue");
      return;
    }
    try {
      PrintStream out = null;
      try {
	ServiceReference[] srl = 
	  bc.getServiceReferences(PrintStream.class.getName(),
				 "(service.pid=java.lang.System.out)");
	if(srl != null && srl.length == 1) {
	  out = (PrintStream)bc.getService(srl[0]);
	}
      } catch (Exception e) { 
	e.printStackTrace(System.out);
      }
      if(out == null) {
	out = System.out;
      }

      consoleSession = sConsole.runSession("console tty",
					   new InputStreamReader(new SystemIn(bc)),
					   new PrintWriter(out));
    } catch (IOException ioe) {
      log(LogService.LOG_ERROR, "Failed to start console session, can not continue");
    }
  }

  //  ====================    stop      ====================
  /**
   ** Called by the framework when this bundle is stopped.
   **
   ** @param bc Bundle context.
   */
  public synchronized void stop(BundleContext bc) {
    log(LogService.LOG_INFO, "Stopping");
    try {
      if (consoleSession != null) {
	consoleSession.close();
      }
    } catch (Exception e) {
      log(LogService.LOG_ERROR, "Failed to close session", e);
    }
  }

  /*---------------------------------------------------------------------------*
   *			  ManagedService implementation
   *---------------------------------------------------------------------------*/
  
  //  ====================    updated     ====================
  /**
   ** Called by CM when it got this bundles configuration.
   **
   ** @param bc Bundle context.
   */
  public synchronized void updated(Dictionary cfg) 
    throws ConfigurationException, IllegalArgumentException {
    if (cfg != null) {
      Boolean b = (Boolean)cfg.get(NONBLOCKING);
      if (b != null) {
	nonblocking = b.booleanValue();
      }
    } else {
      nonblocking = false;
    }
  }

  /*---------------------------------------------------------------------------*
   *			  Protected utility methods
   *---------------------------------------------------------------------------*/

  //  ====================    log      ====================
  /**
   ** Utility method used for logging.
   **
   ** @param level Log level
   ** @param msg   Log message
   */
  public void log(int level, String msg) {
    log(level, msg, null);
  }
  public void log(int level, String msg, Exception e) {
    ServiceReference srLog = bc.getServiceReference(logServiceName);
    if (srLog != null) {  
      LogService sLog = (LogService)bc.getService(srLog);
      if (sLog != null) {
	if(e == null) {
	  sLog.log(level, msg);
	} else {
	  sLog.log(level, msg, e);
	}
      }
     bc.ungetService(srLog);
    }
  }

  /* ------------------------------------------------------------
     Help class for VMs that when blocking in read() om System.in
     block other threads too. 
     ------------------------------------------------------------*/
  class SystemIn extends InputStream {

    InputStream in; 

    SystemIn(BundleContext bc) {
      try {
	ServiceReference[] srl = 
	  bc.getServiceReferences(InputStream.class.getName(),
				 "(service.pid=java.lang.System.in)");
	if(srl != null && srl.length == 1) {
	  in = (InputStream)bc.getService(srl[0]);
	}
      } catch (Exception e) { 
	e.printStackTrace(System.err);
      }
      if(in == null) {
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
	return (int) b[0];
      } else {
	return -1;
      }
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
	  } catch (InterruptedException e) {}
	  nap = 200;
	}
      }
      return in.read(buf, off, len);
    } 
  }

}

