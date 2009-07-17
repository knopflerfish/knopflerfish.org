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

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.osgi.framework.ServiceReference;

import org.knopflerfish.service.log.LogRef;

import org.codehaus.jam.JMethod;
import org.apache.axis2.transport.http.*;
import org.apache.axis2.deployment.*;
import org.apache.axis2.context.*;
import org.apache.axis2.Constants;

import org.apache.axis2.engine.AxisConfigurator;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.description.AxisService;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.util.Loader;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.repository.util.ArchiveReader;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisConfigurator;
import org.apache.axis2.transport.http.HTTPConstants;

import javax.servlet.ServletConfig;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class Axis2AdminServlet extends HttpServlet {

  Axis2Servlet axisServlet;

  public Axis2AdminServlet(Axis2Servlet axisServlet) {
    super();
    this.axisServlet = axisServlet;
  }

  protected void doPost(HttpServletRequest req,
                       HttpServletResponse resp)     throws ServletException, IOException {
    doGet(req, resp);
  }

  protected void doGet(HttpServletRequest req,
                       HttpServletResponse resp)
    throws ServletException, IOException
  {
    PrintWriter out = resp.getWriter();
    
    resp.setContentType("text/html");

    out.println("<html>");
    out.println("<head>");
    out.println(" <title>Axis2Admin</title>");
    out.println("<head>");
    out.println("<body>");

    AxisConfiguration axisConfig = axisServlet.getAxisConfigurator().getAxisConfiguration();
    Map services = axisConfig.getServices();

    out.println("<h2>Registered services</h2>");
    out.println("<dl>");
    for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
      String      name = (String)it.next();
      AxisService as   = (AxisService)services.get(name);

      out.print("<dt>");
      out.println(name);
      out.println("" + wsdlLink(as));
      out.println("</dt>");
      out.print("<dd>");
      if(as instanceof OSGiAxisService) {
        SOAPService ss = ((OSGiAxisService)as).soapService;
        out.println("<ul>");
        out.println(" <li>bundle " + 
                    "#" + ss.sr.getBundle().getBundleId() + 
                    ", " + ss.sr.getBundle().getLocation() + 
                    "</li>");
        out.println(" <li>service #" + ss.sr.getProperty(org.osgi.framework.Constants.SERVICE_ID) + ", " + ss.serviceClass);
        if(ss.schemaGenerator != null) {
          out.println("<ul>");
          JMethod[] methods = ss.schemaGenerator.getMethods();
          for(int i = 0; methods != null && i < methods.length; i++) {
            out.println("<li>" + methods[i].getQualifiedName() + "</li>");
          }
          out.println("</ul>");
        }
        out.println("</li>");
        out.println("</ul>");
      } else {
        out.println(as.getClass().getName() + " " + as);
      }
      out.println("</dd>");
    }
    out.println("</dl>");

    out.println("</body>");
    out.println("</html>");
    out.flush();
  }

  String wsdlLink(AxisService as) {
    return "<a href=\"" + 
      Axis2AdminImpl.AXIS2_SERVLET_ALIAS + "/services/" + as.getName() + "?wsdl" +
      "\">wsdl</a>";
  }

}
