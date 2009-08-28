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

package org.knopflerfish.bundle.httproot;

import java.util.*;
import java.net.URL;
import java.io.InputStream;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;
import org.knopflerfish.service.log.LogRef;

public class Activator implements BundleActivator {

  // This is my world
  static BundleContext bc;
  static LogRef        log;

  static final String  RES_ALIAS     = "/";     // the http server root
  static final String  RES_DIR       = "/www";  // bundle resource directory 

  static final String  SERVLET_ALIAS = "/servlet/knopflerfish-info"; // a small servlet

  Hashtable registrations = new Hashtable();

  public void start(BundleContext bc) throws BundleException {

    this.bc  = bc;
    this.log = new LogRef(bc);

    ServiceListener listener = new ServiceListener() {
	public void serviceChanged(ServiceEvent ev) {
	  ServiceReference sr = ev.getServiceReference();

	  switch(ev.getType()) {
	  case ServiceEvent.REGISTERED:
	    setRoot(sr);
	    break;
	  case ServiceEvent.UNREGISTERING:
	    unsetRoot(sr);
	    break;
	  }
	}
      };
    
    String filter = "(objectclass=" + HttpService.class.getName() + ")";

    try {
      bc.addServiceListener(listener, filter);

      ServiceReference[] srl = bc.getServiceReferences(null, filter);
      for(int i = 0; srl != null && i < srl.length; i++) {
	listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED,
						 srl[i]));
      }
    } catch (Exception e) {
      log.error("Failed to set up listener for http service", e);
    }
  }
  
  public void stop(BundleContext bc) throws BundleException {
  }

  void setRoot(ServiceReference sr) {

    if(registrations.containsKey(sr)) {
      return; // already done
    }

    log.info("set root for " + sr);

    HttpService http = (HttpService)bc.getService(sr);

    HttpContext context = new HttpContext() {
	public boolean handleSecurity(HttpServletRequest  request,
				      HttpServletResponse response) 
	  throws java.io.IOException {
	  return true;
	}
	
	public URL getResource(String name) {

	  // when registering the root, it seems
	  // like we get no separator before the file.
	  // Is that a bug?? Code below is a workaround	  
	  if(name.startsWith(RES_DIR) && 
	     name.length() > RES_DIR.length() &&
	     '/' != name.charAt(RES_DIR.length())) {
	    name = RES_DIR + "/" + name.substring(RES_DIR.length());
	  }

	  // default to index.html
	  if(name.equals(RES_DIR)) {
	    name = "/www/index.html";
	  }

	  // and send the plain file
	  URL url = getClass().getResource(name);

	  return url;
	}
	
	public String getMimeType(String reqEntry) {
	  return null; // server decides type
	}
      };
    
    try {
      http.registerResources(RES_ALIAS, RES_DIR, context);
      http.registerServlet(SERVLET_ALIAS, new InfoServlet(sr), new Hashtable(), context);

      registrations.put(sr, context);
    } catch (Exception e) {
      log.error("Failed to register resource", e);
    }
  } 

  void unsetRoot(ServiceReference sr) {
    if(!registrations.containsKey(sr)) {
      return; // nothing to do
    }

    log.info("unset root for " + sr);
    
    HttpService http = (HttpService)bc.getService(sr);
    
    if(http != null) {
      http.unregister(RES_ALIAS);
      http.unregister(SERVLET_ALIAS);
      bc.ungetService(sr);
    }
    registrations.remove(sr);
  }

}
