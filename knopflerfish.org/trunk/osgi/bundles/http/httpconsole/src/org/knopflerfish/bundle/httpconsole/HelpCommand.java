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
	
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

import org.osgi.framework.*;

public class HelpCommand extends IconCommand {
  ConsoleServlet console;
  
  HelpCommand(ConsoleServlet console) {
    super("cmd_help", 
	  "Help",
	  "Show help",
	  Activator.RES_ALIAS + "/help.gif");
    this.console = console;

    displayFlags = DISPLAY_COMPACTLIST;
  }

  public StringBuffer run(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();

    sb.append("<div class=\"shadow\">Console help</div>\n");

    sb.append("<p>Check one or more bundles from the bundle list, then use the toolbar action buttons to start, stop uninstall or update.</p>\n");

    sb.append("<div class=\"shadow\">Toolbar</div>\n");


    for(int i = 0; i < console.commands.length; i++) {
      if(console.commands[i] instanceof IconCommand) {
	sb.append(" <div>\n");
	StringWriter sw  = new StringWriter();
	
	sb.append(" <span>\n");
	try {
	  console.commands[i].toHTML(request, new PrintWriter(sw));
	  sb.append(sw.toString());
	} catch (Exception e) {
	  sb.append(e.toString());
	}
	sb.append(" </span>\n");
	sb.append(" <span style=\"vertical-align:top;\">\n");
	sb.append(console.commands[i].getDescription());
	sb.append(" </span>\n");
	sb.append("</div>\n");
      }
    }

    //    sb.append("</table>\n");

    sb.append("<div class=\"shadow\">Bundle icons</div>");

    sb.append("<table>");

    sb.append("<tr>");
    sb.append("<td><img src=\"" + Util.BUNDLE_IMAGE + "\">");
    sb.append("</td>");
    sb.append("<td>Bundle with activator</td>");
    sb.append("</tr>");

    sb.append("<tr>");
    sb.append("<td><img src=\"" + Util.BUNDLE_ACTIVE_IMAGE + "\">");
    sb.append("</td>");
    sb.append("<td>Started bundle with activator</td>");
    sb.append("</tr>");

    sb.append("<tr>");
    sb.append("<td><img src=\"" + Util.LIB_IMAGE + "\">");
    sb.append("</td>");
    sb.append("<td>Bundle without activator</td>");
    sb.append("</tr>");
    sb.append("<tr>");
    sb.append("<td><img src=\"" + Util.LIB_ACTIVE_IMAGE + "\">");
    sb.append("</td>");
    sb.append("<td>Started bundle without activator</td>");
    sb.append("</tr>");

    sb.append("</table>");


    return sb;
  }
}
