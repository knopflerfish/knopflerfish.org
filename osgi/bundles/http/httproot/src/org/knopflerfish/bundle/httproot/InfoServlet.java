/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;

public class InfoServlet extends HttpServlet {

  private static final long serialVersionUID = 6985595442868609560L;

  private final ServiceReference httpSR;

  public InfoServlet(final ServiceReference httpSR) {
    this.httpSR = httpSR;
  }

  public void doPost(HttpServletRequest  request,
                     HttpServletResponse response)
    throws ServletException,
           IOException
  {
    // Handle just as GET
    doGet(request, response);
  }

  public void doGet(HttpServletRequest  request,
                    HttpServletResponse response)
    throws ServletException, IOException {

    PrintWriter out = response.getWriter();

    response.setContentType("text/html");

    printHeader(out);
    printMain(out);
    printFooter(out);

    try {
    } catch (Exception e) {
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");
    }

  }

  void printMain(PrintWriter out) throws IOException {
    final String[] keys = httpSR.getPropertyKeys();
    Arrays.sort(keys);

    out.println("<h2>Web server properties</h2>");

    out.println("<table>");
    for(int i = 0; i < keys.length; i++) {
      out.println("<tr>");
      out.println("<td>" + keys[i] + "</td>");

      out.println("<td>");
      printObject(out, httpSR.getProperty(keys[i]));
      out.println("<td>");

      out.println("</tr>");
    }
    out.println("</table>");
  }

  void printObject(PrintWriter out, Object val) throws IOException {
    if(val == null) {
      out.println("null");
    } else if(val.getClass().isArray()) {
      printArray(out, (Object[])val);
    } else if(val instanceof Vector) {
      printVector(out, (Vector)val);
    } else if(val instanceof Map) {
      printMap(out, (Map)val);
    } else if(val instanceof Set) {
      printSet(out, (Set)val);
    } else if(val instanceof Dictionary) {
      printDictionary(out, (Dictionary)val);
    } else {
      out.print(val);
      //      out.print(" (" + val.getClass().getName() + ")");
    }
  }

  void printDictionary(PrintWriter out, Dictionary d) throws IOException {
    // Sort keys using a sorted set.
    final TreeSet keys = new TreeSet();
    for(Enumeration e = d.keys(); e.hasMoreElements();) {
      final String key = (String) e.nextElement();
      keys.add(key);
    }

    out.println("<table>");
    for(Iterator it = keys.iterator(); it.hasNext();) {
      final String key = (String) it.next();
      final Object val = d.get(key);

      out.println("<tr>");

      out.println("<td>");
      printObject(out, key);
      out.println("</td>");

      out.println("<td>");
      printObject(out, val);
      out.println("</td>");

      out.println("</tr>");
    }
    out.println("</table>");
  }

  void printMap(PrintWriter out, Map m) throws IOException {
    // Sorted map
    final TreeMap ms = new TreeMap(m);

    out.println("<table>");
    for(Iterator it = ms.keySet().iterator(); it.hasNext();) {
      final Object key = it.next();
      final Object val = ms.get(key);

      out.println("<tr>");

      out.println("<td>");
      printObject(out, key);
      out.println("</td>");

      out.println("<td>");
      printObject(out, val);
      out.println("</td>");

      out.println("</tr>");
    }
    out.println("</table>");
  }

  void printArray(PrintWriter out, Object[] a) throws IOException {
    for(int i = 0; i < a.length; i++) {
      printObject(out, a[i]);
      if(i < a.length - 1) {
        out.println("<br>");
      }
    }
  }

  void printSet(PrintWriter out, Set a) throws IOException {
    for(Iterator it = a.iterator(); it.hasNext();) {
      printObject(out, it.next());
      if(it.hasNext()) {
        out.println("<br>");
      }
    }
  }

  void printVector(PrintWriter out, Vector a) throws IOException {
    for(int i = 0; i < a.size(); i++) {
      printObject(out, a.elementAt(i));
      if(i < a.size() - 1) {
        out.println("<br>");
      }
    }
  }

  void printHeader(PrintWriter out) throws IOException {
    out.println("<html>");
    out.println("<head>");
    out.println(" <meta http-equiv=\"CACHE-CONTROL\" content=\"NO-CACHE\"/>");
    out.println(" <title>Knopflerfish OSGi Http Service Info</title>");
    out.println(" <link href=\"/knopflerfish.css\" rel=\"stylesheet\" type=\"text/css\">");
    out.println(" <link rel=\"shortcut icon\" href=\"/images/favicon.png\"/>");
    out.println("</head>");
    out.println("<body>");
    out.println(" <div id=\"main\">");
    out.println("  <div id=\"header\">");
    out.println("   <div id=\"header_logo\">");
    out.println("    <a href=\"/\"><img src=\"/images/kf300_black.png\" border=0 alt=\"knopflerfish logo\"></a>");
    out.println("   </div>");
    out.println("   <div id=\"header_menu\">");
    out.println("    <a class=\"button_closed\" href=\"/index.html\">Home</a>");
    out.println("    <a class=\"button_closed\" href=\"/docs/\">Documentation</a>");
    out.println("    <a class=\"button_open\" href=\"/servlet/knopflerfish-info\">Http-Server-Info</a>");
    out.println("    <a class=\"button_closed\" href=\"http://www.knopflerfish.org/\">www.knopflerfish.org</a>");
    out.println("   </div>");
    out.println("  </div>");
    out.println(" <div id=\"mainblock\">");
  }

  void printFooter(PrintWriter out) throws IOException {
    out.println("  </div>");
    out.println("  <div id=\"footer\">");
    out.println("   <div id=\"copyright\">");
    out.println("    Copyright &#169; 2003-2011 The Knopflerfish Project. All rights reserved.");
    out.println("   </div>");
    out.println("  </div>");
    out.println(" </div>");
    out.println("</body>");
    out.println("</html>");
  }
}
