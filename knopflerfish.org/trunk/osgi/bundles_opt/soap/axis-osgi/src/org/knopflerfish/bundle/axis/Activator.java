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
package org.knopflerfish.bundle.axis;

import java.io.InputStream;

import java.net.URL;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.server.AxisServer;
import org.apache.axis.utils.XMLUtils;
//import org.apache.axis.WSDDEngineConfiguration;


import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.axis.ObjectSOAPService;

import org.knopflerfish.util.servlet.ServletDescriptor;
import org.knopflerfish.util.servlet.WebApp;
import org.knopflerfish.util.servlet.WebAppDescriptor;

import org.osgi.framework.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;


/** The <code>Activator</code> is the activator for the Axis OSGi bundle.
 *  Further it handles service registration events for SOAP services.
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class Activator
   implements BundleActivator, ServiceListener {
   public static BundleContext axisBundle = null;
   public static LogRef log = null;
   private static AxisServer axisServer = null;
   private WebApp webApp = null;

   public static AxisServer getAxisServer() {
      return axisServer;
   }

   public void start(BundleContext bc)
              throws BundleException {
      try {
         log = new LogRef(bc, true);
         axisBundle = bc;
         URL url = this.getClass().getResource("/axis/server-config.wsdd");
         InputStream is = url.openStream();
         EngineConfiguration fromBundleResource = new FileProvider(is);

         log.info("Configuration file read.");
         axisServer = new AxisServer(fromBundleResource);
         log.info("Axis server started.");
         webApp = new WebApp(getWebAppDescriptor());
         webApp.start(bc);
         log.info("Web application started.");
	 axisBundle.addServiceListener(this);   

	 // Make sure we get services already registered
	 ServiceReference[] srl = axisBundle.getServiceReferences(null, null);
	 for(int i = 0; srl != null && i < srl.length; i++) {
	   serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
	 }

      } catch (Exception e) {
         log.error("Exception when starting bundle", e);
         throw new BundleException("Failed to start server");
      }
   }

   public void stop(BundleContext bc)
             throws BundleException {
      try {
	 axisBundle.removeServiceListener(this);   
         webApp.stop(bc);
         webApp = null;
         axisBundle = null;
         axisServer = null;
         log.close();
         log = null;
      } catch (Exception e) {
         log.error("Exception when stopping bundle", e);
         throw new BundleException("Failed to stop server", e);
      }
   }

   private WebAppDescriptor getWebAppDescriptor() {
      WebAppDescriptor wad = new WebAppDescriptor();

      wad.servlet = new ServletDescriptor[1];
      wad.context = "/axis";
      wad.servlet[0] = new ServletDescriptor("/services", 
                                             new ServicesServlet());
      return wad;
   }
   
  
  Map exportedServices = new HashMap();
   
  public void serviceChanged(ServiceEvent event) {
    try {
      switch(event.getType()) {
      case ServiceEvent.REGISTERED:
	{
	  ServiceReference sr = event.getServiceReference();
	  String serviceName = (String) sr.getProperty("SOAP_SERVICE_NAME");
	  if (serviceName != null) {
	    log.info("ServicesServlet:: added service "+serviceName);
	    
	    Object serviceObj = axisBundle.getService(sr);
	    
	    getAxisServer().getClassCache()
	      .registerClass(serviceObj.getClass().getName(),
			     serviceObj.getClass());
	    
	    ObjectSOAPService soapService = 
	      new ObjectSOAPService(axisServer,serviceName,serviceObj);
	    
	    soapService.deploy();
	    
	    exportedServices.put(sr, soapService);
	  }
	} 
	break;
	case ServiceEvent.UNREGISTERING:
	  {
	    ServiceReference sr = event.getServiceReference();
	    String serviceName  = (String) sr.getProperty("SOAP_SERVICE_NAME");
	    if (serviceName != null) {
	      
	      ObjectSOAPService soapService 
		= (ObjectSOAPService)exportedServices.get(sr);
	      if(soapService != null) {
		Object serviceObj = soapService.getServiceObject();
		
		getAxisServer().getClassCache()
		  .deregisterClass(soapService.getClass().getName());
		
		soapService.undeploy();
		log.info("ServicesServlet:: removed service "+serviceName);
	      }
	      
	      //   (new ObjectSOAPService(axisServer,serviceName,null)).undeploy();
	    }
	  }
	  break;
      }
    } catch (Exception e) {
      log.error("ServicesServlet::serviceChanged() Exception", e);
    }
  }
}
