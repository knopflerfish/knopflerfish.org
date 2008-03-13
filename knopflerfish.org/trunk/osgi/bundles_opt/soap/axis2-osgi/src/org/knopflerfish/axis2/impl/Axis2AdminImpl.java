/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above/*
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
package org.knopflerfish.axis2.impl;


import java.util.*;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.AxisService;

import org.knopflerfish.service.axis2.Axis2Admin;
import org.knopflerfish.service.log.LogRef;


public class Axis2AdminImpl implements Axis2Admin {
  LogRef        log;
  BundleContext bc;

  // servlet aliases. this is where the axis2 and axis2admin servlets
  // are registered in the HTTP server
  static final String  AXIS2_SERVLET_ALIAS      = "/axis2";
  static final String  AXIS2ADMIN_SERVLET_ALIAS = "/axis2admin";

  // Map for keeping track of services to be deployed as SOAP services
  // ServiceReference -> SOAPService
  protected Map soapServices = new HashMap();

  // Map for keeping track of axis2 servlet in http servers
  // ServiceReference -> Object[] { HttpContext, Axis2Servlet, Axis2AdminServlet }
  Hashtable servletRegistrations = new Hashtable();

  public Axis2AdminImpl(BundleContext bc) {
    this.bc = bc;
    log = Activator.log;
  }

  void init() throws Exception {
    setupSOAPServiceListener();
    setupHttpServiceListener();
  }

  public void cleanup() {

    undeploySOAPServices(null);

    bc.removeServiceListener(soapServiceListener);
    bc.removeServiceListener(httpServiceListener);

    log.close();
    log = null;
    bc  = null;
  }


  public String[] getPublishedServiceNames() {
    synchronized(soapServices) {
      Set names = new TreeSet();
      for(Iterator it = soapServices.keySet().iterator(); it.hasNext();) {
        ServiceReference sr          = (ServiceReference)it.next();
        SOAPService      soapService = (SOAPService)soapServices.get(sr);
        if(soapService.getDeployCount() > 0) {
          names.add(soapService.serviceName);
        }
      }
      String[] r = new String[names.size()];
      names.toArray(r);
      return r;
    }
  }

  void setupSOAPServiceListener() throws Exception {
    log.debug("Activator.setupSOAPServiceListener()");

    // yes, listen for ALL services.
    bc.addServiceListener(soapServiceListener);

    // Make sure we get services already registered
    handleRegisteredSOAPServices();

    log.debug("Activator.setupAxis() done");
  }

  /**
   * Get all registered SOAP services and deploy them in axis servlets
   */
  void handleRegisteredSOAPServices() throws Exception {
    ServiceReference[] srl = bc.getServiceReferences(null, null);
    for(int i = 0; srl != null && i < srl.length; i++) {
      soapServiceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
    }
  }


  void setupHttpServiceListener() throws Exception {
    String filter = "(objectclass=" + HttpService.class.getName() + ")";
    
    Activator.bc.addServiceListener(httpServiceListener, filter);
    
    ServiceReference[] srl = Activator.bc.getServiceReferences(null, filter);
    for(int i = 0; srl != null && i < srl.length; i++) {
        httpServiceListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED,
                                                            srl[i]));
    }
  }


  // Service listener for http services. 
  // Called only for http services
  protected ServiceListener httpServiceListener = new ServiceListener() {
      public void serviceChanged(ServiceEvent ev) {
        ServiceReference sr = ev.getServiceReference();
        
        switch(ev.getType()) {
        case ServiceEvent.REGISTERED:
          setupAxisServlet(sr);
          break;
        case ServiceEvent.UNREGISTERING:
          removeAxisServlet(sr);
          break;
        }
      }
    };


  // Service listener for possible SOAP services. 
  // Called for ALL services
  protected ServiceListener soapServiceListener = new ServiceListener() {
      public void serviceChanged(ServiceEvent event) {
        ServiceReference sr = event.getServiceReference();
        
        try {
          switch(event.getType()) {
          case ServiceEvent.REGISTERED:
            if (isSOAPService(sr)) {
              deploySOAPServiceRegistration(sr);
            }
            break;
          case ServiceEvent.MODIFIED: // modification is handled by undeploy + deploy
            if (soapServices.containsKey(sr)) {
              undeploySOAPServiceRegistration(sr, null);
            }
            if (isSOAPService(sr)) { // perhaps it's no longer an exported SOAP service
              deploySOAPServiceRegistration(sr);
            }
            break;
          case ServiceEvent.UNREGISTERING:
            if (soapServices.containsKey(sr)) {
              undeploySOAPServiceRegistration(sr, null);
            }
            break;
          }
        } catch (Exception e) {
          log.error("soapServiceListener", e);
        }
      }
    };

  
  // use the same httpcontext for all servlets
  HttpContext myHttpContext = new HttpContext() {
      public boolean handleSecurity(HttpServletRequest  request,
                                    HttpServletResponse response)
        throws java.io.IOException {
        return true;
      }
      
      public URL getResource(String name) {
        return null;
      }
      
      public String getMimeType(String reqEntry) {
        return null; // server decides type
      }
    };
  

  /**
   * Setup Axis2Servlet at the http service referenced by sr
   */
  void setupAxisServlet(ServiceReference httpSR) {

    synchronized(servletRegistrations) {
      if(servletRegistrations.containsKey(httpSR)) {
        return; // already done
      }
      
      log.debug("set axis servlet for " + httpSR);
      
      HttpService http = (HttpService)Activator.bc.getService(httpSR);
      
      try {
        Axis2Servlet      axisServlet  = new Axis2Servlet();
        Axis2AdminServlet adminServlet = new Axis2AdminServlet(axisServlet);

        http.registerServlet(AXIS2_SERVLET_ALIAS, axisServlet, new Hashtable(), myHttpContext);
        http.registerServlet(AXIS2ADMIN_SERVLET_ALIAS, adminServlet, new Hashtable(), myHttpContext);
        
        log.info("registered axis servlet at " + AXIS2_SERVLET_ALIAS);
        log.info("registered axis admin servlet at " + AXIS2ADMIN_SERVLET_ALIAS);
        
        servletRegistrations.put(httpSR, new Object[] { 
          myHttpContext, 
          axisServlet, 
          adminServlet,
        });

        handleRegisteredSOAPServices();
       
      } catch (Exception e) {
        log.error("Failed to setup Axis2Servlet", e);
      }
    }
  }

  /**
   * Remove Axis2Servlet at the http service referenced by httpSR
   */
  void removeAxisServlet(ServiceReference httpSR) {
    synchronized(servletRegistrations) {
      if(!servletRegistrations.containsKey(httpSR)) {
        return; // nothing to do
      }
      
      log.debug("remove axis " + httpSR);

      try {
        Object[]          v          = (Object[])servletRegistrations.get(httpSR);
        Axis2Servlet      servlet    = (Axis2Servlet)v[1];        
        AxisConfiguration axisConfig = servlet.getAxisConfigurator().getAxisConfiguration();
        
        for(Iterator it = soapServices.keySet().iterator(); it.hasNext(); ) {            
          ServiceReference soapSR      = (ServiceReference)it.next();
          SOAPService      soapService = (SOAPService)soapServices.get(soapSR);
          soapService.undeploy(axisConfig);
        }
      } catch (Exception e) {
        log.warn("Failed to undeploy", e);
      }
      
      HttpService http = (HttpService)Activator.bc.getService(httpSR);
      
      if(http != null) {
        http.unregister(AXIS2_SERVLET_ALIAS);
        http.unregister(AXIS2ADMIN_SERVLET_ALIAS);
        bc.ungetService(httpSR);
      }
      servletRegistrations.remove(httpSR);
    }
  }


  boolean isSOAPService(ServiceReference sr) {    
    Object serviceName = sr.getProperty(Axis2Admin.SOAP_SERVICE_NAME);
    return 
      (serviceName != null) &&
      (serviceName instanceof String);
  }

  /**
   * Undeploy all SOAP services present in the soapServices map
   *
   * @param httpSR if null, remove from all http service, otherwise
   *               specifies the http server to remove the soap service from
   */
  void undeploySOAPServices(ServiceReference httpSR) {
    synchronized(soapServices) {
      for(Iterator it = soapServices.keySet().iterator(); it.hasNext();) {
        ServiceReference sr = (ServiceReference)it.next();
        SOAPService  soapService = (SOAPService)soapServices.get(sr);
        undeploySOAPService(soapService, httpSR);
      }
      soapServices.clear();
    }
  }
  
  /**
   * Undeploy a single SOAPService from one or all http servers
   *
   * @param httpSR if null, remove from all http service, otherwise
   *               specifies the http server to remove the soap service from
   */
  public void undeploySOAPService(SOAPService soapService, 
                                  ServiceReference httpSR) {
    log.debug("undeploySOAPService " + soapService);
    
    synchronized(servletRegistrations) {
      for(Iterator it = servletRegistrations.keySet().iterator(); it.hasNext(); ) {
        try {
          ServiceReference sr      = (ServiceReference)it.next();
          if(httpSR == null || httpSR.equals(sr)) {
            Object[]         v       = (Object[])servletRegistrations.get(sr);
            Axis2Servlet     servlet = (Axis2Servlet)v[1];

            AxisConfiguration axisConfig = servlet.getAxisConfigurator().getAxisConfiguration();
            
            soapService.undeploy(axisConfig);
          }
        } catch (Exception e) {
          log.warn("Failed to undeploy service " + soapService, e);
        }
      }
    }
  }

  void undeploySOAPServiceRegistration(ServiceReference sr,
                                       ServiceReference httpSR) {
    SOAPService  soapService = (SOAPService)soapServices.get(sr);
    String       serviceName = (String) sr.getProperty(Axis2Admin.SOAP_SERVICE_NAME);

    log.debug("undeploySOAPServiceRegistration " + serviceName);

    soapServices.remove(sr);
    undeploySOAPService(soapService, httpSR);
  }

  void deploySOAPServiceRegistration(ServiceReference sr) {
    String           serviceName    = (String)   sr.getProperty(Axis2Admin.SOAP_SERVICE_NAME);

    log.debug("deploySOAPServiceRegistration " +serviceName);
    
    // throws exception if name is invalid
    assertServiceName(serviceName);
    
    synchronized(soapServices) {
      SOAPService soapService = (SOAPService)soapServices.get(sr);
      if(soapService == null) {
        soapService = new SOAPService(sr);
        log.debug("created new " + soapService);
      } else {
        log.debug("reused " + soapService);
      }

      int count = deploySOAPService(soapService);
      
      log.debug("deployed in " + count  + " servlets");
      soapServices.put(sr, soapService);
    }
  }


  /**
   * Deploy a SOAPService in all found axis servlets.
   *
   * @return number of successful deployments
   */
  int  deploySOAPService(SOAPService soapService) {
    log.debug("deploySOAPService " +  soapService);

    int count = 0;
    synchronized(servletRegistrations) {
      
      for(Iterator it = servletRegistrations.keySet().iterator(); it.hasNext(); ) {
        try {
          ServiceReference sr      = (ServiceReference)it.next();
          Object[]         v       = (Object[])servletRegistrations.get(sr);
          Axis2Servlet     servlet = (Axis2Servlet)v[1];
          
          AxisConfiguration axisConfig = servlet.getAxisConfigurator().getAxisConfiguration();
                    
          soapService.deploy(axisConfig);
          count++;
        } catch (Exception e) {
          log.warn("Failed to deploy service " + soapService, e);
        }
      }
    }
    return count;
  }

  
  // Some chars we don't allow in soap service names
  static final String ILLEGAL_SERVICE_CHARS = "\r\n\t \\\"'!#%&/()=+{}[]?<>;";

  /**
   * Check if service name is OK for publishing as SOAP service.
   *
   * This incluced checking for previous registrations at the same name.
   *
   * @throws IllegalArgumentException if name is not valid
   */
  void assertServiceName(String serviceName) {
    if(serviceName == null) {
      throw new IllegalArgumentException("Service name cannot be null");
    }
    if("".equals(serviceName)) {
      throw new IllegalArgumentException("Service name cannot be empty string");
    }

    for(int i = 0; i < serviceName.length(); i++) {
      if(-1 != ILLEGAL_SERVICE_CHARS.indexOf(serviceName.charAt(i))) {
        throw new IllegalArgumentException("Service name " + 
                                           "'" + serviceName + "'" + 
                                           " cannot contain whitespace or special characters");
      }
    }

    synchronized(soapServices) {
      for(Iterator it = soapServices.keySet().iterator(); it.hasNext();) {
        ServiceReference sr          = (ServiceReference)it.next();
        SOAPService      soapService = (SOAPService)soapServices.get(sr);
        String           name        = (String)sr.getProperty(Axis2Admin.SOAP_SERVICE_NAME);
        if(soapService.getDeployCount() > 0 && name.equals(serviceName)) {
          throw new IllegalArgumentException("Service '" + name + "' is already deployed");
        }
      }
    }
  }
}
