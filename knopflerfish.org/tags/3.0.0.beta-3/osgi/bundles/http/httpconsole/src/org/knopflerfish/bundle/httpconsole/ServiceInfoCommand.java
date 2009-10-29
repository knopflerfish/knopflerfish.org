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
import java.util.*;

import org.osgi.framework.*;

public class ServiceInfoCommand extends IconCommand {
  public ServiceInfoCommand() {
    super("cmd_serviceinfo", 
	  "Service Info",
	  "Show service info",
	  null);
  }

  public StringBuffer run(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();

    String serviceID = request.getParameter("service.id");

    if(serviceID == null || "".equals(serviceID)) {
      sb.append("<div class=\"shadow\">No service selected</div>");
      return sb;
    }

    String filter = "(service.id=" + serviceID + ")";
    try {
      ServiceReference[] srl = Activator.bc.getServiceReferences(null, filter);

      for(int i = 0; srl != null && i < srl.length; i++) {
	printServiceInfo(sb, srl[i]);
      }

    } catch (Exception e) {
      sb.append(Util.toHTML(e));
    }

    return sb;
  }

  void printServiceInfo(StringBuffer sb, ServiceReference sr) {
    sb.append("<div class=\"shadow\">Service #" + 
	      sr.getProperty("service.id") + 
	      "</div>");

    String[] keys = sr.getPropertyKeys();
    
    sb.append("<table>\n");
    for(int i = 0; keys != null && i < keys.length; i++) {
      sb.append(" <tr>\n");
      sb.append("  <td>");
      sb.append(keys[i]);
      sb.append("</td>\n");

      sb.append("<td>\n");
      try {
	Object val = sr.getProperty(keys[i]);
	StringWriter sw = new StringWriter();
	Util.printObject(new PrintWriter(sw), val);
	sb.append(sw.toString());
      } catch (Exception e) {
	sb.append(Util.toHTML(e));
      }
      sb.append("</td>\n");
      sb.append(" </tr>\n");
    }
    sb.append("</table>\n");
  }

  public void toHTML(HttpServletRequest request, PrintWriter out) throws IOException {
  }

  public boolean      isTrigger(HttpServletRequest request) {
    return null != request.getParameter("service.id");
  }
}
