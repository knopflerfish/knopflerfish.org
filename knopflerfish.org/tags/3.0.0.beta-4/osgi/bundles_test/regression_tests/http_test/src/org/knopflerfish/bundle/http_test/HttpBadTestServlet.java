/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.http_test;

import org.knopflerfish.service.http_test.*;

import java.io.*;
import java.util.*;
import org.osgi.framework.*;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.http.*;
import org.osgi.service.http.HttpService;


import javax.servlet.*;
import javax.servlet.http.*;

class HttpBadTestServlet extends HttpServlet {
  public HttpBadTestServlet() {
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException,IOException 
  {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    out.println("<HTML>");
    out.println("<HEAD><TITLE>" + " HttpTestServlet" +  "</TITLE></HEAD>");
    out.println("<BODY>");
    out.println("Test servlet 1 for http_test test bundle <br>");
    out.println("The servlet's URI is: " + request.getRequestURI() + "<br>");
    out.println("The servlet's path is: " + request.getServletPath() + "<br>");
    out.println("The servlet's pathinfo is: " + request.getPathInfo());
    out.println("</BODY>");
    out.println("</HTML>");
  }
  
  void checkRequest(HttpServletRequest request) throws ServletException {
    ServletConfig config = getServletConfig();
    if (config == null) {
      // log.log(LogService.LOG_ERROR, "Failed to get config object");
      throw new ServletException("Failed to get ServletConfig");
    }

    ServletContext context = config.getServletContext();
    if (context == null) {
      // log.log(LogService.LOG_ERROR, "Failed to get context object");
      throw new ServletException("Failed to get ServletContext");
    }
  }    
  public void init () throws ServletException {
    throw new ServletException("Bad servlet, init fails on purpose");

  }
  public void init (ServletConfig config) throws ServletException {
    throw new ServletException("Bad servlet, init fails on purpose");
  }

}    
