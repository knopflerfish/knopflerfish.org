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

import java.util.Hashtable;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;

/**
 * <p>
 * Wrapper class which listens for all framework services of
 * class HttpServlet or HttpContext. Each such service is picked
 * up and installed into actual running HttpServices
 * </p>
 * <p>
 * <p>
 * The alias used for the servlet/resource is taken from the 
 * PROP_ALIAS service property.
 * </p>
 * <p>
 * The resource dir used for contexts is taken from the 
 * PROP_DIR service property.
 * </p>
 */
public class HttpWrapper {

  /**
   * Service property name for HttpServlet and HttpContexts alias
   *
   * <p>
   * Value is <tt>httpwrapper.resource.alias</tt>
   * </p>
   */
  public static String PROP_ALIAS = "httpwrapper.resource.alias";

  /**
   * Service property name for HttpContext resource dir
   *
   * <p>
   * Value is <tt>httpwrapper.resource.dir</tt>
   * </p>
   */
  public static String PROP_DIR   = "httpwrapper.resource.dir";

  BundleContext  bc;

  HttpServlet servlet;
  HttpContext context;

  HttpWrapper(BundleContext bc, HttpServlet servlet, HttpContext context) {
    this.bc = bc;
    this.servlet = servlet;
    this.context = context;
  }
  
  void open() {
    ServiceListener sl = new ServiceListener() {
    	public void serviceChanged(ServiceEvent ev) {
    	  ServiceReference sr = ev.getServiceReference();
    	  switch(ev.getType()) {
    	  case ServiceEvent.REGISTERED:
    	    {
      	    try {
              register(sr);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    };
    
    String filter = "(objectclass=" + HttpService.class.getName() + ")";
    
    try {
      bc.addServiceListener(sl, filter);
      ServiceReference[] srl = bc.getServiceReferences(null, filter);
      for(int i = 0; srl != null && i < srl.length; i++) {
        register(srl[i]);
      }
    } catch (InvalidSyntaxException e) { 
      e.printStackTrace(); 
    }
  }
  
  void register(ServiceReference sr) {
    HttpService http = (HttpService) bc.getService(sr);
    if (http == null) {
      Activator.log.warn("http resource is null");
      return;
    }
    
    try {
      Hashtable props = new Hashtable();
      http.registerServlet(Activator.SERVLET_ALIAS, servlet, props, null);
      http.registerResources(Activator.RES_ALIAS, Activator.RES_DIR, context);
    } catch (Exception e) {
      Activator.log.error("Failed to register resource", e);
    }
  }

}
