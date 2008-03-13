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

import org.apache.axis2.transport.http.*;
import org.apache.axis2.deployment.*;
import org.apache.axis2.context.*;
import org.apache.axis2.Constants;

import org.apache.axis2.engine.AxisConfigurator;
import org.apache.axis2.engine.AxisConfiguration;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class Axis2Servlet extends AxisServlet {
  protected OSGiConfigurator configurator;

  public Axis2Servlet() {
    super();
  }

  public AxisConfigurator getAxisConfigurator() {
    return configurator;
  }

  protected void service(HttpServletRequest req,
                         HttpServletResponse resp)
    throws ServletException, IOException
  {
    ClassLoader t = null;
    try {
      t = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      HttpServletRequest wReq = new HttpServletRequestWrapper(req) {
          public String getContextPath() {
            return Axis2AdminImpl.AXIS2_SERVLET_ALIAS;
          }
        };
      HttpServletResponse wResp = new HttpServletResponseWrapper(resp) {
          // Axis sometimes set the content type twice, with the wrong
          // type the second time...
          public void setContentType(String contentType) {
            if (!isCommitted() && (null==getContentType())) {
              super.setContentType(contentType);
            } else {
              if(Activator.log.doDebug()) {
                Activator.log.debug("** setContentType("+contentType+") ignored!");
              }
            }
          }
        };
      super.service(wReq, wResp);
      if(Activator.log.doDebug()) {        
        Activator.log.debug("Request.contentType is: "+req.getContentType());
        Activator.log.debug("Request.characterEncoding is: "+req.getCharacterEncoding());
        Activator.log.debug("Response.contentType is: "+resp.getContentType());
      }
    } finally {
      Thread.currentThread().setContextClassLoader(t);
    }
  }


  protected ConfigurationContext initConfigContext(ServletConfig config)
    throws ServletException
  {
    try {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

      configurator = new OSGiConfigurator(config);

      ConfigurationContext configContext =
        ConfigurationContextFactory.createConfigurationContext(configurator);

      configContext.setProperty(Constants.CONTAINER_MANAGED,
                                Constants.VALUE_TRUE);
      return configContext;
    } catch (Exception e) {
      Activator.log.warn("Failed to init axis context", e);
      throw new ServletException(e);
    } finally {
    }
  }

}
