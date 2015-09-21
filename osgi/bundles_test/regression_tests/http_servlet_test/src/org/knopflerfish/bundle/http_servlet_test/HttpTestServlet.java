/*
 * Copyright (c) 2015, KNOPFLERFISH project
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

package org.knopflerfish.bundle.http_servlet_test;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class HttpTestServlet extends HttpServlet {
  boolean destroyCalled = false;
  HttpServletTestSuite servletTestSuite;

  // Static variable to assert against
  public static HttpServletRequest activeRequest = null;
  public static HttpServletResponse activeResponse;
  
  public HttpTestServlet() {  }
  
  public HttpTestServlet(HttpServletTestSuite suite) {
    servletTestSuite = suite;
    
    
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException,IOException
  {
    activeRequest = request;
    activeResponse = response;

    servletTestSuite.runTestSuite(servletTestSuite.getRequestTestSuite, null);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException,IOException
    {
      
      activeRequest = request;
      activeResponse = response;

      servletTestSuite.runTestSuite(servletTestSuite.postRequestTestSuite, null);
    }

  @Override
  public void destroy()
  {
    destroyCalled = true;
    log("HttpTestServlet.destroy() called");
    super.destroy();
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

}
