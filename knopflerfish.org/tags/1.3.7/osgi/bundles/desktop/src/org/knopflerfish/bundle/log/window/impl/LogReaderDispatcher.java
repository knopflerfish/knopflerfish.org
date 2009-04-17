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

package org.knopflerfish.bundle.log.window.impl;

import java.util.*;
import java.text.*;

import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.knopflerfish.service.log.LogRef;

import javax.swing.*;

/**
 * Class that listens for all log entries and dispatches them to 
 * a LogTableModel.
 */
public class LogReaderDispatcher 
  implements 
    LogListener, 
    ServiceListener
{

  BundleContext bc;
  LogTableModel model;
  
  public LogReaderDispatcher(BundleContext bc,
			     LogTableModel model) {
    this.bc    = bc;
    this.model = model;
  }

  public void open() {
    String filter = "(objectclass=" + LogReaderService.class.getName() + ")";

    try {
      ServiceReference [] srl = bc.getServiceReferences(null, filter);
      for(int i = 0; srl != null && i < srl.length; i++) {

	serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
      }
      bc.addServiceListener(this, filter);
    } catch (Exception e) {
      e.printStackTrace();
    }    
  }

  Hashtable logReaders = new Hashtable();

  public void clearAll() {
    model.clear();
  }

  public void getAll() {
    for(Enumeration e = logReaders.keys(); e.hasMoreElements(); ) {
      ServiceReference sr = (ServiceReference)e.nextElement();
      LogReaderService lr = (LogReaderService)logReaders.get(sr);
      
      for(Enumeration e2 = lr.getLog(); e2.hasMoreElements(); ) {
	LogEntry entry = (LogEntry)e2.nextElement();
	logged(entry);
      }
    }
  }
  
  public void close() {
    bc.removeServiceListener(this);

    for(Enumeration e = logReaders.keys(); e.hasMoreElements(); ) {
      ServiceReference sr = (ServiceReference)e.nextElement();
      serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
    }
    logReaders.clear();
  }

  public void serviceChanged(ServiceEvent ev) {
    ServiceReference sr = ev.getServiceReference();

    
    LogReaderService lr = 
      logReaders.containsKey(sr) 
      ? (LogReaderService)logReaders.get(sr)
      : (LogReaderService)bc.getService(sr);
    
    switch(ev.getType()) {
    case ServiceEvent.REGISTERED:
      lr.addLogListener(this);
      logReaders.put(sr, lr);
      break;
    case ServiceEvent.MODIFIED:
      break;
    case ServiceEvent.UNREGISTERING:
      lr.removeLogListener(this);
      logReaders.remove(sr);
      bc.ungetService(sr);
      break;
    }
  }

  static long idCount = 0;
  
  /**
   * Listener method called for each LogEntry created.
   * As with all event listeners, this method should return to its
   * caller as soon
   * as possible.
   *
   * @param entry A <code>LogEntry</code> object containing log information.
   * @see LogEntry
   */
  public void logged(LogEntry entry) {
    final ExtLogEntry extEntry = new ExtLogEntry(entry, idCount++);
    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  try {
	    model.logged(extEntry);
	  } catch (Exception e) {
	    e.printStackTrace();
	  }
	}
      });
  }
}





