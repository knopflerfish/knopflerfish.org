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

public class InstallFileCommand2 implements Command {
  String redir = null;
  String msg   = null;
  
  public int getDisplayFlags() {
    return DISPLAY_COMPACTLIST;
  }

  public StringBuffer run(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();

    sb.append("<div class=\"shadow\">" + getName() + "</div>");
    
    try {
      StringBuffer filename = new StringBuffer();
      byte[] bytes = Util.loadFormData(request, getId(), filename);
      if(bytes == null || bytes.length == 0) {
	sb.append("No file, or empty files selected");
      } else {
	InputStream in = new ByteArrayInputStream(bytes);
	String loc = in.toString() + "." + filename;
	Bundle b = Activator.bc.installBundle(loc, in);
	if(b != null) {
	  sb.append("Installed bundle of size " + 
		    bytes.length + " bytes<br/>");
	} else {
	  sb.append("Failed to install bundle of size " + bytes.length + " bytes<br/>");
	}
      }
    } catch (Exception e) {
      sb.append(Util.toHTML(e));
    }

    redir = Activator.SERVLET_ALIAS;
    msg   = sb.toString();

    return sb;
  }

  public void toHTML(HttpServletRequest request, PrintWriter out) throws IOException {
    out.println("<div class=\"shadow\">" + getName() + "</div>");

    out.print("<input alt=\"File\"" + 
	      " type=\"file\"" + 
	      " name=\"" + getId() + "_file\">");
    out.print("<br/>");
    out.print(" <input " + 
	      " type=\"submit\"" + 
	      " name=\"" + getId() + "\"" + 
	      " value=\"" + "Install" + "\"" + 
	      ">");    
  }
  
  public String       getId() {
    return "cmd_installfile2";
  }

  public String getName() {
    return "Install from file";
  }

  public String getIcon() {
    return null;
  }

  public String getDescription() {
    return "Install bundle from file";
  }


  public boolean isTrigger(HttpServletRequest request) {

    String s = request.getHeader("content-type");
    if(s == null) { s = ""; }
    

    boolean b = 
      s.startsWith("multipart/form-data");
    
    return b;
  }
}
