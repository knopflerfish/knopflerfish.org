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

    // Wrap the Http service into something more
    // white-board-like
    HttpWrapper wrapper = new HttpWrapper(bc);
    wrapper.open();

    // By registering the servlet and the context
    // they will be picked up by the wrapper and installed
    // in the http service
    Hashtable props1 = new Hashtable();
    props1.put(HttpWrapper.PROP_ALIAS, SERVLET_ALIAS);
    bc.registerService(HttpServlet.class.getName(), servlet, props1);

    Hashtable props2 = new Hashtable();
    props2.put(HttpWrapper.PROP_ALIAS, RES_ALIAS);
    props2.put(HttpWrapper.PROP_DIR,   RES_DIR);
    bc.registerService(HttpContext.class.getName(), context, props2);
  }

  public void stop(BundleContext bc) throws BundleException {
    // all is done by FW
  }

  // These are registered as-in into the framework.
  // The HttpWrapper will pick them up and handle the gory
  // work of registering them into all actual http services
  HttpServlet servlet = new ConsoleServlet();

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
