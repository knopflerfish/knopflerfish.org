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

  Command[]        commands;

  // the fime install command is special
  // since it requires a multipart/form-data
  // form encoding. 
  InstallFileCommand installFileCommand;

  public ConsoleServlet(ServiceReference httpSR) {
    this.httpSR = httpSR;

    commands = new Command[] {
      new ReloadCommand(),
      installFileCommand = new InstallFileCommand(),
      new IconDialogCommand(new InstallURLCommand()),
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

    StringBuffer sb = new StringBuffer();

    try {
      handleCommands(request, sb);
    } catch (Exception e) {
      sb.append(Util.toHTML(e));
    }

    if(installFileCommand.installFile2.redir != null) {
      String base  = "http://" + request.getServerName();
      int    port = request.getServerPort();
      
      if(port != 80) base = base + ":" + port;
      
      String url = base + installFileCommand.installFile2.redir;
      installFileCommand.installFile2.redir = null;
      response.sendRedirect(url);
      return;
    }

    if(installFileCommand.installFile2.msg != null) {
      sb.append(installFileCommand.installFile2.msg);
      installFileCommand.installFile2.msg = null;
    }

    response.setContentType("text/html");

    if(false) {
      for(Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
	String key = (String)e.nextElement();
	String val = request.getHeader(key);
	System.out.println("header: " + key + "=" + val);
      }
      
      for(Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
	String key = (String)e.nextElement();
	String val = request.getParameter(key);
	System.out.println("param: " + key + "=" + val);
      }
    }

    printHeader(out);


    Util.formStart(out, installFileCommand.isTrigger(request));
        
    try {
      out.println("<table width=\"100%\" class=\"maintable\">");


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
      } else {
	out.println("<div class=\"shadow\">&nbsp;</div>");
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

    Util.formStop(out);
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
