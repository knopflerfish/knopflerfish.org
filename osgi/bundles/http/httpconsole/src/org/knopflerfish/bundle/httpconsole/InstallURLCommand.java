/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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
	
import javax.servlet.http.*;
import java.io.*;

public class InstallURLCommand extends IconCommand {

  InstallURLCommand() {
    super("cmd_installurl",
	  "Install from URL",
	  "Install bundle from URL",
	  Activator.RES_ALIAS + "/openurl.png");
    displayFlags = DISPLAY_COMPACTLIST;
  }
  
  public StringBuilder run(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
   
    String url = request.getParameter(getId() + "_url");

    sb.append("<div class=\"shadow\">").append(getName()).append("</div>");
    
    if(!(url == null || "".equals(url))) {
      try {
        Activator.bc.installBundle(url);
        sb.append("installed ").append(url).append("<br/>");
      } catch (Exception e) {
        sb.append(Util.toHTML(e));
      }
    } else {
      sb.append("No URL entered");
    }

    return sb;
  }

  public void toHTML(HttpServletRequest request, PrintWriter out) {
    out.println("<div class=\"shadow\">" + getName() + "</div>");
    out.print("<input alt=\"URL\"" + 
		" type=\"text\"" + 
		" name=\"" + getId() + "_url\">");
    out.print(" URL<br/>");
    out.print(" <input " + 
		" type=\"submit\"" + 
		" name=\"" + getId() + "\"" + 
		" value=\"" +"Install" + "\"" + 
		"\">");

  }
  
  public boolean isTrigger(HttpServletRequest request) {
    return null != request.getParameter(getId());
  }
}
