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

package org.knopflerfish.bundle.httpconsole;

import java.util.*;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;

public class Activator implements BundleActivator {

  // This is my world
  static BundleContext bc;

  static final String  RES_DIR       = "/www";   
  static String  SERVLET_ALIAS       = "/servlet/console";
  static final String  RES_ALIAS     = "/console/resources";  

  String filter = "(objectclass=" + HttpService.class.getName() + ")";

  Hashtable registrations = new Hashtable();


  static LogRef log;
  public void start(BundleContext bc) throws BundleException {
    this.bc  = bc;
    this.log = new LogRef();

    String alias = 
      System.getProperty("org.knopflerfish.httpconsole.alias");
    if(alias != null && !"".equals(alias)) {
      SERVLET_ALIAS = alias.trim();
    }

    String fs = 
      System.getProperty("org.knopflerfish.httpconsole.filter");
    if(fs != null && !"".equals(fs)) {
      // Just do a quick syntax check
      try {
	bc.createFilter(fs);
	filter = fs;
      } catch (Exception e) {
	log.warn("Failed to use custom filter", e);
      }
    }

    ServiceListener listener = new ServiceListener() {
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



  void setServlet(ServiceReference sr) {

    if(registrations.containsKey(sr)) {
      return; // already done
    }

    log.info("set httconsole servlet for HttpService #" + 
	     sr.getProperty("service.id") + " at " + SERVLET_ALIAS);

    HttpService http = (HttpService)bc.getService(sr);

    HttpContext context = new HttpContext() {
	public URL getResource(String name) {

	  // and send the plain file
	  URL url = getClass().getResource(name);

	  return url;
	}
	
	public String getMimeType(String reqEntry) {
	  return null; // server decides type
	}

	public boolean handleSecurity( HttpServletRequest request,
				       HttpServletResponse response )
	  throws IOException 
	{
	  // Security is handled by server
	  return true;
	}
      };
    
    try {
      http.registerResources(RES_ALIAS, RES_DIR, context);
      http.registerServlet(SERVLET_ALIAS, 
			   new ConsoleServlet(sr), 
			   new Hashtable(), context);

      registrations.put(sr, context);
    } catch (Exception e) {
      log.error("Failed to register resource", e);
    }
  } 

  void unsetServlet(ServiceReference sr) {
    if(!registrations.containsKey(sr)) {
      return; // nothing to do
    }

    log.info("unset httpconsole servlet for " + sr);
    
    HttpService http = (HttpService)bc.getService(sr);
    
    if(http != null) {
      http.unregister(RES_ALIAS);
      http.unregister(SERVLET_ALIAS);
      bc.ungetService(sr);
    }
    registrations.remove(sr);
  }

  class LogRef {
    void info(String msg) {
      System.out.println("INFO: " + msg);
    }
    
    void error(String msg, Throwable t) {
      System.out.println("ERROR: " + msg);
      if(t != null) {
	t.printStackTrace();
      }
    }
    void warn(String msg) {
      warn(msg, null);
    }

    void warn(String msg, Throwable t) {
      System.out.println("WARN: " + msg);
      if(t != null) {
	t.printStackTrace();
      }
    }
  }

}
