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
package org.knopflerfish.bundle.axis;

import java.net.URL;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.axis.AxisFault;
import org.apache.axis.WSDDEngineConfiguration;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDDocument;
import org.apache.axis.server.AxisServer;
import org.apache.axis.transport.http.AxisServlet;
import org.apache.axis.utils.XMLUtils;

import org.knopflerfish.util.servlet.WebApp;

import org.osgi.framework.*;


/** The <code>ServiceServlet</code> extends the AxisServlet to enable it to work
 *  in an OSGi environment. Further it monitors service registrations in order
 *  to automate Axis service registrations.
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class ServicesServlet
   extends AxisServlet
   implements ServiceListener {
   public AxisServer getEngine()
                        throws AxisFault {
      return Activator.getAxisServer();
   }

   public void serviceChanged(ServiceEvent event) {
      try {
         if (event.getType() == ServiceEvent.REGISTERED) {
            ServiceReference sr = event.getServiceReference();

            Activator.log.info("ServicesServlet:: added service");
            URL url = (URL) sr.getProperty("AXIS_DEPLOY");

            if (url != null) {
               deployWSDD(url.openStream());
            }
         }
         if (event.getType() == ServiceEvent.UNREGISTERING) {
            ServiceReference sr = event.getServiceReference();

            Activator.log.info("ServicesServlet:: removed service");
            URL url = (URL) sr.getProperty("AXIS_UNDEPLOY");

            if (url != null) {
               deployWSDD(url.openStream());
            }
         }
      } catch (Exception e) {
         Activator.log.error("ServicesServlet::serviceChanged() Exception", e);
      }
   }

   protected String getWebappBase(HttpServletRequest request) {
      StringBuffer baseURL = new StringBuffer(128);

      baseURL.append(request.getScheme());
      baseURL.append("://");
      baseURL.append(request.getServerName());
      if (request.getServerPort() != 80) {
         baseURL.append(":");
         baseURL.append(request.getServerPort());
      }
      baseURL.append(request.getContextPath());
      baseURL.append(WebApp.webAppDescriptor.context);
      return baseURL.toString();
   }

   private void deployWSDD(java.io.InputStream stream) {
      try {
         Activator.log.info("ServicesServlet:: deployWSDD");
         WSDDDocument doc = new WSDDDocument(XMLUtils.newDocument(stream));

         doc.deploy(((WSDDEngineConfiguration) getEngine().getConfig()).getDeployment());
         getEngine().refreshGlobalOptions();
      } catch (Exception e) {
         Activator.log.error("ServicesServlet::deployWSDD() Exception", e);
      }
   }
}
