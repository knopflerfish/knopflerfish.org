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

package org.knopflerfish.bundle.junit;

import junit.framework.*;
import org.osgi.framework.*;
import java.util.*;
import org.osgi.service.http.*;

public class HttpExporter {
 static final String SERVLET_ALIAS = "/junit";

  ServiceListener httpListener = null;

  public HttpExporter(BundleContext dummy) {
  }

  public void open() {
    openHTTP();
  }

  public void openHTTP() {
    httpListener = new ServiceListener() {
	public void serviceChanged(ServiceEvent ev) {
	  ServiceReference sr = ev.getServiceReference();
	  
	  switch(ev.getType()) {
	  case ServiceEvent.REGISTERED:
	    setServlet(sr);
	    break;
	  case ServiceEvent.UNREGISTERING:
	    unsetServlet(sr);
	    break;
	  }
	}
      };
    
    String filter = "(objectclass=" + HttpService.class.getName() + ")";

    try {
      Activator.bc.addServiceListener(httpListener, filter);
      
      ServiceReference[] srl = Activator.bc.getServiceReferences(null, filter);
      for(int i = 0; srl != null && i < srl.length; i++) {
	httpListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED,
						     srl[i]));
      }
    } catch (Exception e) {
      Activator.log.error("Failed to set up listener for http service", e);
    }
  }

  Hashtable registrations = new Hashtable();

  void unsetServlet(ServiceReference sr) {
    registrations.remove(sr);
    HttpService http = (HttpService)Activator.bc.getService(sr);
    if(http != null) {
      try {
	http.unregister(SERVLET_ALIAS);
	
      } catch (Exception e) {
	Activator.log.error("Failed to unregister junit servlet", e);
      }
    }
  }

  void setServlet(ServiceReference sr) {
    if(registrations.containsKey(sr)) {
      return; // already done
    }


    HttpService http = (HttpService)Activator.bc.getService(sr);

    try {
      JUnitServlet servlet = new JUnitServlet();
      http.registerServlet(SERVLET_ALIAS, 
			   servlet,
			   new Hashtable(), 
			   null);
      registrations.put(sr, servlet);

      Activator.log.info("registered junit servlet at " + 
			 SERVLET_ALIAS + ", port=" + sr.getProperty("port"));
    } catch (Exception e) {
      Activator.log.error("Failed to register junit servlet", e);
    }
  }
}
