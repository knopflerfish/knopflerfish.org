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

package org.knopflerfish.bundle.httpconsole;

import java.util.*;
import java.net.URL;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;

public class ConsoleServlet extends HttpServlet {

  ServiceReference httpSR;

  Command[] commands;

  public ConsoleServlet(ServiceReference httpSR) {
    this.httpSR = httpSR;

    commands = new Command[] {
      new ReloadCommand(),
      new InstallCommand(),
      new StartCommand(),
      new StopCommand(),
      new UpdateCommand(),
      new UninstallCommand(),
      new InfoCommand(),
      new HelpCommand(this),
      new StatusCommand(),
    };

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


    out.println("<form " + 
		" action=\"" + Activator.SERVLET_ALIAS + "\"" + 
		" method=\"GET\"" + 
		">");
        
    StringBuffer sb = new StringBuffer();
    try {
      out.println("<table width=\"100%\" class=\"maintable\">");
      handleCommands(request, sb);

      out.println("<tr class=\"toolview\">");
      out.println("<td colspan=\"2\" class=\"toolview\">");
      printToolbar(request, out);
      out.println("</td>");
      out.println("</tr>");

      out.println("<tr>");
      out.println("<td class=\"mainview\">");
      printMain(request, out);
      out.println("</td>");

      out.println("<td width=\"30%\" class=\"maininfo\">");
      if(sb.length() > 0) {
	out.println("<div class=\"cmdresult\">");
	out.println(sb.toString());
	out.println("</div>");
      }
      out.println("</td>");      
      out.println("</tr>");

      out.println("<tr class=\"toolview\">");
      out.println("<td colspan=\"2\" class=\"toolview\">");
      printToolbar(request, out);
      out.println("</td>");
      out.println("</tr>");

      out.println("<table>");
    } catch (Exception e) {
      out.println(Util.toHTML(e));
    }

    out.println("<form>");
    printFooter(out);

    try {
    } catch (Exception e) {
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");      
    }

  }

  BundleView iconView = new IconView();

  void handleCommands(HttpServletRequest request, 
		      StringBuffer       sb) throws Exception {
    for(int i = 0; i < commands.length; i++) {
      if(commands[i].isTrigger(request)) {
	StringBuffer s = commands[i].run(request);
	sb.append(s.toString());
      }
    }
  }

  void printMain(HttpServletRequest request, PrintWriter out) throws IOException {
    
    iconView.toHTML(request,out);
    
  }


  void printToolbar(HttpServletRequest request, PrintWriter out) throws IOException {
    
    out.println("<div class=\"toolbar\">");
    
    for(int i = 0; i < commands.length; i++) {
      commands[i].toHTML(request, out);
    }
    
    out.println("</div>");
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

    out.println("<table>");
    for(Enumeration e = d.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      Object val = d.get(key);
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

    out.println("<table>");
    for(Iterator it = m.keySet().iterator(); it.hasNext();) {
      Object key = it.next();
      Object val = m.get(key);

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
    out.println("<title>Knopflerfish OSGi console</title>");
    
    out.println("<LINK href=\"" + Activator.RES_ALIAS + "/console.css\" rel=\"stylesheet\" type=\"text/css\">");
    out.println("</head>");
    out.println("<body>");
    
  }

  void printFooter(PrintWriter out) throws IOException {
    out.println("</body>");
    out.println("</html>");  
  }
}
