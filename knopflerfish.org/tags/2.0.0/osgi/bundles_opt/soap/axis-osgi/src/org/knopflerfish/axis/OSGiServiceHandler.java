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
package org.knopflerfish.axis;

import java.io.ByteArrayInputStream;

import java.util.Hashtable;

import javax.xml.namespace.QName;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.WSDDEngineConfiguration;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDDocument;
import org.apache.axis.deployment.wsdd.WSDDService;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.enum.Scope;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.utils.XMLUtils;

import org.apache.commons.logging.Log;

import org.osgi.framework.*;


/** An <code>OSGiServiceHandler</code> attempts to find the referenced service
 *  in the OSGi registry, and dynamically define a corresponding SOAP service.
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class OSGiServiceHandler
   extends BasicHandler {
   protected static Log log = LogFactory.getLog(OSGiServiceHandler.class.getName());

   public void generateWSDL(MessageContext msgContext)
                     throws AxisFault {
      invoke(msgContext);
   }

   public void invoke(MessageContext msgContext)
               throws AxisFault {
      log.debug("Enter: OSGiServiceHandler::invoke");

      // If there's already a targetService then just return.
      if (msgContext.getService() != null)
         return;
      String path = (String) msgContext.getProperty(
                           HTTPConstants.MC_HTTP_SERVLETPATHINFO);
      int osgiSRindex = path.indexOf("osgi-sr/");

      // If there's no "osgi-sr" in the path then just return.
      if (osgiSRindex < 0)
         return;
      try {
         String filter = path.substring(osgiSRindex + 8);
         String serviceName = "OSGi-SR:" + filter;
         WSDDEngineConfiguration config = (WSDDEngineConfiguration) msgContext.getAxisEngine()
          .getConfig();
         WSDDDeployment deployment = config.getDeployment();
         WSDDService ws = findService(deployment, serviceName);
         SOAPService service = null;

         if (ws == null) {
            BundleContext bContext = org.knopflerfish.bundle.axis.Activator.axisBundle;
            Object serviceObject = null;
            String sfilter = "(" + filter + ")";
            ServiceReference[] srs = bContext.getServiceReferences(null, 
                                                                   sfilter);

            serviceObject = bContext.getService(srs[0]);
            String addDoc = deploymentWSDD(serviceWSDD(serviceName, 
                                                       serviceObject.getClass().getName()));
            WSDDDocument doc = new WSDDDocument(XMLUtils.newDocument(new ByteArrayInputStream(addDoc.getBytes())));

            doc.deploy(deployment);
            msgContext.getAxisEngine().refreshGlobalOptions();
            deployment = config.getDeployment();
            ws = findService(deployment, serviceName);
            service = deployment.getService(ws.getQName());
//            service.setOption("scope", Scope.APPLICATION_STR);
            msgContext.getAxisEngine().getApplicationSession().set(serviceName, 
                                                                   serviceObject);
         }
         service = deployment.getService(ws.getQName());
         msgContext.setService(service);
      } catch (Exception e) {
         e.printStackTrace();
         throw AxisFault.makeFault(e);
      }
      log.debug("Exit: OSGiServiceHandler::invoke");
   }

   private String deploymentWSDD(String body) {
      return "<deployment" + " xmlns=\"http://xml.apache.org/axis/wsdd/\"" + 
             " xmlns:java=\"http://xml.apache.org/axis/wsdd/providers/java\">" + 
             body + "</deployment>";
   }

   private WSDDService findService(WSDDDeployment deployment, String name) {
      WSDDService[] ws = deployment.getServices();

      for (int i = 0; i < ws.length; i++) {
         WSDDService s = ws[i];

         if (ws[i].getServiceDesc().getName().equals(name))
            return ws[i];
      }
      return null;
   }

   private String serviceWSDD(String serviceName, String className) {
      return "<service name=\"" + serviceName + "\" provider=\"java:RPC\">" + 
             "<parameter name=\"allowedMethods\" value=\"*\"/>" + 
             "<parameter name=\"className\" value=\"" + className + "\"/>" + 
             "<parameter name=\"scope\" value=\"Application\"/>" + 
             "</service>";
   }
}
