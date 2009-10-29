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

public class InstallFileCommand extends IconCommand {

  InstallFileCommand2 installFile2 = new InstallFileCommand2();

  InstallFileCommand() {
    super("cmd_installfile",
	  "Install file",
	  "Install bundle from file",
	  Activator.RES_ALIAS + "/open.gif");
  }

  public int getDisplayFlags() {
    return installFile2.getDisplayFlags();
  }
  
  public StringBuffer run(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();

    if(installFile2.isTrigger(request)) {
      return installFile2.run(request);
    } else {
      StringWriter sw = new StringWriter();
      try {
	installFile2.toHTML(request, new PrintWriter(sw));
	sb.append(sw.toString());
      } catch (Exception e) {
	sb.append(Util.toHTML(e));
      }
    }

    return sb;
  }

  public void toHTML(HttpServletRequest request, PrintWriter out) throws IOException {
    out.println(" <input alt=\"" + getDescription() + "\"" + 
		" type=\"image\"" + 
		" class=\"iconcmd\"" + 
		" name=\"" + getId() + "\"" + 
		" src=\"" + getIcon() + "\">");
  }
  

  public boolean isTrigger(HttpServletRequest request) {
    return 
      null != request.getParameter(getId() + ".x")
      || installFile2.isTrigger(request)
      ;
  }
}
