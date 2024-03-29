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

import org.osgi.framework.*;

public class InstallFileCommand2 implements Command {
  String redir = null;
  String msg   = null;

  public int getDisplayFlags() {
    return DISPLAY_COMPACTLIST;
  }

  public StringBuilder run(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();

    sb.append("<div class=\"shadow\">").append(getName()).append("</div>");

    try {
      StringBuilder filename = new StringBuilder();
      byte[] bytes = Util.loadFormData(request, getId(), filename);
      if(bytes == null || bytes.length == 0) {
        sb.append("No file, or empty files selected");
      } else {
        InputStream in = new ByteArrayInputStream(bytes);
        String loc = in.toString() + "." + filename;
        Bundle b = Activator.bc.installBundle(loc, in);
        if(b != null) {
          sb.append("Installed bundle of size ").append(bytes.length).append(" bytes<br/>");
        } else {
          sb.append("Failed to install bundle of size ").append(bytes.length).append(" bytes<br/>");
        }
      }
    } catch (BundleException be){
      if (be.getCause() instanceof java.util.zip.ZipException) {
        sb.append("The slected file was not a valid jar file.");
      } else {
        sb.append(Util.toHTML(be));
      }
    } catch (Exception e) {
      sb.append(Util.toHTML(e));
    }

    redir = Activator.SERVLET_ALIAS;
    msg   = sb.toString();

    return sb;
  }

  public void toHTML(HttpServletRequest request, PrintWriter out) {
    out.println("<div class=\"shadow\">" + getName() + "</div>");

    out.print("<input alt=\"File\"" +
              " type=\"file\"" +
              " name=\"" + getId() + "_file\">");
    out.print("<br/>");
    out.print(" <input " +
              " type=\"submit\"" +
              " title=\"Install bundle from file\"" +
              " name=\"" + getId() + "\"" +
              " value=\"" + "Install" + "\"" +
              ">");
  }

  public String getId() {
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
    return s != null && s.startsWith("multipart/form-data");
  }
}
