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

package org.knopflerfish.bundle.httptest;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

class HttpTestServlet extends HttpServlet implements SingleThreadModel {
  final static String SERVLET_INFO = "HttpTestServlet";
  PrintWriter out;
  final String[] stdhdrs = new String[] {"Value", "Got", "Expected", "Status"};

  HttpTest httpTest;
  
  public HttpTestServlet(HttpTest httpTest) {
    this.httpTest = httpTest;
  }
  

  //----------------------------------------------------------------------------
  //	Servlet overrides
  //----------------------------------------------------------------------------
  public void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException,IOException 
  {
    doGet(request, response);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException,IOException 
  {
    try {
      // Get stream to write output to
      out = response.getWriter();
      
      response.setContentType("text/html");

      out.println("<html><body BGCOLOR=\"" + httpTest.bgcolor + "\">");
      
      // Tests to perform 
      checkHttpServlet();
      checkParams(request);
      checkHttpRequest(request);
      checkServletConfig(getServletConfig());
      checkServletContext(getServletContext());
      out.println("</body></html>");
      out.flush();
      
    } 
    catch (Exception e) {
      e.printStackTrace(System.out);
      throw new ServletException("Failed to fulfill request", e);
    }
  }

  public java.lang.String getServletInfo() {
    return SERVLET_INFO;
  }

  //----------------------------------------------------------------------------
  //	Test methods
  //----------------------------------------------------------------------------

  // Check servlet itself
  private void checkHttpServlet() {
    System.err.println("HttpServlet");
    tableStart(stdhdrs);
    printHdr(3, "HttpServlet");
    printRes("initParam->in1", getInitParameter("in1"), "inval1");
    printRes("initParam->in2", getInitParameter("in2"), "inval2");
    tableEnd();
  }
  
  // Check parameters
  private void checkParams(HttpServletRequest request) {
    System.err.println("checkParams");
    printHdr(3, "Servlet parameters");
    tableStart(stdhdrs);
    String arg1 = request.getParameter("arg1");
    String arg2 = request.getParameter("arg2");
    printRes("arg1", arg1, "apa");
    printRes("arg2", arg2, "bepa");
    tableEnd();
  }

  // Check HttpRequest
  private void checkHttpRequest(HttpServletRequest request) {
    System.err.println("checkHttpRequest()");
    printHdr(3, "HttpServletRequest");
    tableStart(stdhdrs);
    printRes("getCharacterEncoding()", request.getCharacterEncoding(), null);
    printRes("getContentLength()", String.valueOf(request.getContentLength()), "-1");
    printRes("getContentType()", request.getContentType(), null);
    printRes("getProtocol()", request.getProtocol(), "HTTP/1.0");
    printRes("getRemoteAddr()", request.getRemoteAddr());
    printRes("getRemoteHost()", request.getRemoteHost());
    printRes("getScheme()", request.getScheme(), "http");
    printRes("getServerName()", request.getServerName());
    printRes("getServerPort()", String.valueOf(request.getServerPort()));

    // attributes
    printRes("getAttribute(\"attr1\")", request.getAttribute("attr1"), null);
    printRes("setAttribute(\"attr1\", \"attr1_value\")", "");
    request.setAttribute("attr1", "attr1_value");
    printRes("getAttribute(\"attr1\")", request.getAttribute("attr1"), "attr1_value");
    try {
      request.setAttribute("attr1", "attr1_value2");
    }
    catch (IllegalStateException ise) { 
      printRes("setAttribute(\"attr1\", \"attr1_value2\")", ise.getClass().getName(), ise.getClass().getName());
    }
    catch (Exception e) {
      printRes("setAttribute(\"attr1\", \"attr1_value2\")", e, new IllegalStateException());
    }
    
    // HttpServletRequest
    printRes("getAuthType()", request.getAuthType());
    printRes("getMethod()", request.getMethod());
    printRes("getRequestURI()", request.getRequestURI());
    printRes("getServletPath()", request.getServletPath());
    
    tableEnd();
  }

  // Check ServletConfig
  private void checkServletConfig(ServletConfig sc) {
    System.err.println("checkServletConfig()");
    printHdr(3, "ServletConfig");
    tableStart(stdhdrs);
    printRes("initParam->in1", sc.getInitParameter("in1"), "inval1");
    printRes("initParam->in2", sc.getInitParameter("in2"), "inval2");
    tableEnd();
  }
  
  // Check ServletContext
  private void checkServletContext(ServletContext sc) {
    System.err.println("checkServletContext()");
    printHdr(3, "ServletContext");
    tableStart(stdhdrs);
    printRes("getMajorVersion()", String.valueOf(sc.getMajorVersion()), "2");
    printRes("getMinorVersion()", String.valueOf(sc.getMinorVersion()), "1");
    printRes("getServerInfo()", sc.getServerInfo());
    
    // attributes
    printRes("getAttribute(attr1)", sc.getAttribute("attr1"), null);
    sc.setAttribute("attr1", "attr1_value");
    printRes("getAttribute(attr1)", sc.getAttribute("attr1"), "attr1_value");
    sc.setAttribute("attr1", "attr1_value2");
    printRes("getAttribute(attr1)", sc.getAttribute("attr1"), "attr1_value2");
    sc.removeAttribute("attr1");
    printRes("getAttribute(attr1)", sc.getAttribute("attr1"), null);
    
    tableEnd();
  }
  
  private void printRes(String value, Object checked, Object expected) {
    boolean stat = (checked == null) ? (expected == null) : (checked.equals(expected));
    out.println("<tr>" + 
		"<td>" + value		+ "</td>" + 
		"<td>" + checked	+ "</td>" + 
		"<td>" + expected	+ "</td>" + 
		((stat) ? "<td>OK</td>" : "<td BGCOLOR=\"#ff0000\">FAILED</td>") + 
		"</tr>");
  }
  
  private void printRes(String value, String checked) {
    out.println("<tr>" + 
		"<td>" + value		+ "</td>" + 
		"<td>" + checked	+ "</td>" + 
		"<td>" + "&nbsp;"	+ "</td>" + 
		"<td>" + "&nbsp;"	+ "</td>" + 
		"</tr>");
  }    

  private void tableStart(String[] hdrs) {
    out.println("<table BORDER=1 BGCOLOR=\"#fafafa\"><tr>");
    for (int i = 0; i < hdrs.length; i++) {
      out.println("<th>" + hdrs[i] + "</th>");
    }
    out.println("<tr>");
  }

  private void tableEnd() {
    out.println("</table>");
  }

  private void printHdr(int l, String hdr) {
    out.println("<h" + l + ">" + hdr + "</h" + l + ">");
  }
}
