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

public class StatusCommand implements Command {
  public StringBuffer run(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();
   
    return sb;
  }

  public int getDisplayFlags() {
    return 0;
  }

  public void toHTML(HttpServletRequest request, PrintWriter out) throws IOException {
    Bundle[] bl = new Bundle[0];

    try {
      Activator.bc.getBundles();
    } catch (IllegalStateException e) {
      out.println("HTTP console updated itself, please " + 
		  "<a href=\"" + Activator.SERVLET_ALIAS + "\">reload</a>");
      return;
    }
    int nActive = 0;
    int nTotal  = bl.length;
    
    for(int i = 0; i < bl.length; i++) {
      Bundle b = bl[i];
      if(b.getState() == Bundle.ACTIVE) {
	nActive++;
      }
    }
    
    out.print(" <nobr style=\"vertical-align:top;\">");
    out.print(Activator.bc.getProperty(Constants.FRAMEWORK_VENDOR));
    out.print(" on ");
    out.print(Activator.bc.getProperty(Constants.FRAMEWORK_OS_NAME));
    out.print("/");
    out.print(Activator.bc.getProperty(Constants.FRAMEWORK_OS_VERSION));
    out.print("/");
    out.print(Activator.bc.getProperty("java.vm.name"));
    out.print("</nobr>");
  }
  
  public String       getId() {
    return "cmd_status";
  }

  public String       getName() {
    return "Status";
  }

  public String getIcon() {
    return null;
  }

  public String getDescription() {
    return "Fframework status";
  }

  public boolean isTrigger(HttpServletRequest request) {
    return false;
  }
}
