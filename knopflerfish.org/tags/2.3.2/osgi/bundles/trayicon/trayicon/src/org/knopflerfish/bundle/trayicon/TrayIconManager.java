/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.trayicon;

import org.osgi.framework.*;
import com.jeans.trayicon.*;
import java.util.*;
import javax.swing.*;
import org.knopflerfish.service.trayicon.*;


public class TrayIconManager implements ServiceListener {

  // ServiceReference -> TrayIconWrapper
  Map trayIcons = new HashMap();
  
  public TrayIconManager() {
    
  }
  
  public void serviceChanged(ServiceEvent ev) {
    ServiceReference sr = ev.getServiceReference();

    try {
      switch(ev.getType()) {
      case ServiceEvent.REGISTERED:
	synchronized(trayIcons) {
	  if(!trayIcons.containsKey(sr)) {
	    TrayIcon trayIcon = (TrayIcon)Activator.bc.getService(sr);
	    for(Iterator it = trayIcons.keySet().iterator(); it.hasNext();) {
	      ServiceReference sr2 = (ServiceReference)it.next();
	      TrayIconWrapper wrapper = (TrayIconWrapper)trayIcons.get(sr2);
	      if(wrapper.trayIcon.getId().equals(trayIcon.getId())) {
		Activator.log.error("Duplicate trayicon id=" + trayIcon.getId());
		return;
	      }
	    }
	    TrayIconWrapper wrapper = new TrayIconWrapper(trayIcon);
	    wrapper.open();
	    trayIcons.put(sr, wrapper);
	}
	}
	break;
      case ServiceEvent.UNREGISTERING:
	synchronized(trayIcons) {
	  TrayIconWrapper wrapper = (TrayIconWrapper)trayIcons.get(sr);
	  Activator.log.info("unregister " + wrapper);
	  if(wrapper != null) {
	    wrapper.close();
	    trayIcons.remove(sr);
	    Activator.bc.ungetService(sr);
	  }
	}
	break;
      case ServiceEvent.MODIFIED:
	break;
      }
    } catch (Exception e) {
      Activator.log.error("Failed to handle service event", e);
    }
  }
  
  public void open() {
    //    WindowsTrayIcon.keepAlive();

    try {
      String systemLF = UIManager.getSystemLookAndFeelClassName();
      UIManager.setLookAndFeel(systemLF);
    } catch (Exception e) {
      Activator.log.info("Failed to set system L&F", e);
    }

    synchronized(trayIcons) {
      String filter = 
	"(objectclass=" + TrayIcon.class.getName() + ")";
      
      try {
	ServiceReference[] srl = 
	  Activator.bc.getServiceReferences(null, filter);
	
	for(int i = 0; srl != null && i <srl.length; i++) {
	  serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
	}
	
	Activator.bc.addServiceListener(this, filter);
      } catch (Exception e) {
	Activator.log.error("Failed to setup TrayIconManager", e);
      }
    }
  }

  public void close() {
    Activator.log.info("manager.close()");
    synchronized(trayIcons) {
      for(Iterator it = trayIcons.keySet().iterator(); it.hasNext();) {
	ServiceReference sr = (ServiceReference)it.next();
	TrayIconWrapper wrapper = (TrayIconWrapper)trayIcons.get(sr);
	Activator.log.info(" call wrapper.close()");
	wrapper.close();
	Activator.log.info(" called wrapper.close()");
      }
      trayIcons.clear();
      Activator.log.info(" call cleanUp");
      WindowsTrayIcon.cleanUp();
      logErr();

      Activator.log.info(" called cleanUp");
    }
  }

  void logErr() {
    logErr("tray log");
  }
  void logErr(String msg) {
    int lastErr = WindowsTrayIcon.getLastError();
    if(lastErr != WindowsTrayIcon.NOERR) {
      Exception e = new RuntimeException("WindowsTrayIcon error: " + errString(lastErr));
      
      Activator.log.error(msg, e);
    } else {
      if(Activator.log.doDebug()) {
	Activator.log.debug(msg + ", lastErr=" + lastErr + 
			    "(" + errString(lastErr) + ")");
      }
    }
  }
  
  static String errString(int n) {
    switch(n) {
    case WindowsTrayIcon.NOERR:          return "none";
    case WindowsTrayIcon.ERRTHREAD:      return "ERRTHREAD";
    case WindowsTrayIcon.JNIERR:         return "JNIERR";
    case WindowsTrayIcon.METHODID:       return "METHODID";
    case WindowsTrayIcon.NOLISTENER:     return "NOLISTENER";
    case WindowsTrayIcon.NOTENOUGHMEM:   return "JNOTENOUGHMEM";
    case WindowsTrayIcon.NOTIFYPROCERR:  return "NOTIFYPROCERR";
    case WindowsTrayIcon.NOVMS:          return "NOVMS";
    case WindowsTrayIcon.TOOMANYICONS:   return "TOOMANYICONS";
    case WindowsTrayIcon.WRONGICONID:    return "WRONGICONID";
    default: return "#" + n; 
    }
  }
}
