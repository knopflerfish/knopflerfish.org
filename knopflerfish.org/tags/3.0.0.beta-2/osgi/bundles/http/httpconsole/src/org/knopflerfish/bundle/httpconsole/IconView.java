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

public class IconView implements BundleView {

  ConsoleServlet console;

  public IconView(ConsoleServlet console) {
    this.console = console;
  }

  public StringBuffer run(HttpServletRequest  request) {
    StringBuffer sb = new StringBuffer();

    return sb;
  }

  public String       getId() {
    return "iconview";
  }

  public String       getName() {
    return "Icon View";
  }


  public void toHTML(HttpServletRequest request, 
		     PrintWriter out,
		     int displayFlags) throws IOException  {

    Bundle[] bundles = Util.getSortedBundles(Activator.bc);

    int nActive = 0;
    int nTotal  = bundles.length;
    
    for(int i = 0; i < bundles.length; i++) {
      if(bundles[i].getState() == Bundle.ACTIVE) {
	nActive++;
      }
    }

    String uapixels = request.getHeader("ua-pixels");

    int width  = Integer.MAX_VALUE;
    int height = Integer.MAX_VALUE;
    if(uapixels != null) {
      int ix = uapixels.indexOf("x");
      if(ix != -1) {
	try {
	  width  = Integer.parseInt(uapixels.substring(0, ix));
	  height = Integer.parseInt(uapixels.substring(ix + 1));
	} catch (Exception ignored) {
	  ignored.printStackTrace();
	}
      }
    }

    boolean bCompact = 
      (0 != (displayFlags & Command.DISPLAY_COMPACTLIST));
      //      (height < 400) && 
      //      (request.getParameter("cmd_info.x") != null);

    out.print("<div class=\"shadow\">");
    out.print(nTotal + " bundles, " + nActive + " active");
    
    /*
    if(console.login.bRequireLogin) {
      out.print(", <a class=\"std\" href=\"" + Activator.SERVLET_ALIAS + "?" + 
		Login.LOGOUT_CMD + "=true" + "\">" + 
		"logout</a>");
    }
    */
    out.println("</div>");

    out.println("<table width=\"100%\" class=\"bundletable\">");
    for(int i = 0; i < bundles.length; i++) {
      String oddeven = ((i & 1) == 0) ? "even" : "odd";

      String img = Util.getBundleImage(bundles[i]);

      
      out.print  (" <tr class=\"row_" + oddeven + "\">");
      out.print  ("  <td class=\"row_" + oddeven + "\">");
      out.print  ("   <input type=\"checkbox\"" + 
		  " name=\"" + Util.BUNDLE_ID_PREFIX + 
		  bundles[i].getBundleId() + "\"");
      if(null != request.getParameter(Util.BUNDLE_ID_PREFIX + bundles[i].getBundleId())) {
	out.print( " checked ");
      }
      out.print  ("/>");
      out.println("  </td>");
      out.print  ("  <td class=\"row_" + oddeven + "\">");
      out.print(Util.infoLink(bundles[i]));
      out.print  ("<img border=\"0\" src=\"" + img + "\"/>");
      out.print  ("</a>");
      out.println("  </td>");
      out.print  ("  <td class=\"row_" + oddeven + "\">");
      out.print  (     "<div class=\"bundlename\">" + 
		       Util.getName(bundles[i]) + 
		       "</div>");
      if(!bCompact) {
	out.print  (     "<div class=\"bundledescription\">" + 
			 Util.getDescription(bundles[i]) + 
			 " (" + 
			 "#" + bundles[i].getBundleId() + 
			 ", " + Util.getStateString(bundles[i].getState()) +
			 ")" +
			 "</div>");
      }
      out.println("  </td>");
      out.println(" </tr>");
    }
    out.println("</table>");
  }
}
