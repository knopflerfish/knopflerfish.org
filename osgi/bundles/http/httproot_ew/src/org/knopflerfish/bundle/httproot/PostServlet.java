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

package org.knopflerfish.bundle.httproot;

import java.util.*;
import java.net.URL;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;
import org.knopflerfish.service.log.LogRef;

public class PostServlet extends HttpServlet {

  ServiceReference httpSR;

  public PostServlet(ServiceReference httpSR) {
    this.httpSR = httpSR;
  }

  public void doPost(HttpServletRequest  request, 
		     HttpServletResponse response) 
    throws ServletException,
	   IOException 
  {
    System.out.println("client 1");
    PrintWriter out = response.getWriter();
    
    System.out.println("client 2");
    response.setContentType("text/plain");
    System.out.println("client 3");
    out.println("POST ok");
    System.out.println("client 4");

    for(Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
      String key  = (String)e.nextElement();
      String val  = request.getParameter(key);

      out.println("servlet, param: " + key + ": " + val);
    }

    for(Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
      String key  = (String)e.nextElement();
      String val  = request.getHeader(key);

      out.println("servlet, header: " + key + ": " + val);
    }

    System.out.println("servlet: read input");
    InputStream is = request.getInputStream();
    
    System.out.println("servlet: is=" + is);
    int n = 0;
    int total = 0;
    byte[] buf = new byte[1024];
    
    
    while(-1 != (n = is.read(buf))) {
      System.out.println("servlet got " + n + " bytes: " + 
			 new String(buf, 0, n));
      total += n;
    }

    System.out.println("servlet: read total=" + n);
    

  }
}
