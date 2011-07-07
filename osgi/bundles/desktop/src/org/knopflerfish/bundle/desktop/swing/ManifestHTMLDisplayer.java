/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.startlevel.StartLevel;

public class ManifestHTMLDisplayer extends DefaultSwingBundleDisplayer {

  public ManifestHTMLDisplayer(BundleContext bc) {
    super(bc, "Manifest", "Shows bundle manifest", true);
  }

  public JComponent newJComponent() {
    return new JHTML(this);
  }

  public void valueChanged(long  bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JHTML comp = (JHTML)it.next();
      comp.valueChanged(bl);
    }
  }

  class JHTML extends JHTMLBundle {
    JHTML(DefaultSwingBundleDisplayer displayer) {
      super(displayer);
    }

    public StringBuffer  bundleInfo(Bundle b) {
      StringBuffer sb = new StringBuffer();

      Dictionary headers = b.getHeaders();

      sb.append("<table border=0 cellspacing=1 cellpadding=0>\n");
      appendRow(sb, "Location", "" + b.getLocation());
      appendRow(sb, "State",    Util.stateName(b.getState()));
      if (b.getSymbolicName() != null) {
        appendRow(sb, "Symbolic name", b.getSymbolicName());
      }
      appendRow(sb, "Last modified", "" + new SimpleDateFormat().format(new Date(b.getLastModified())));

      StartLevel sls = (StartLevel)Activator.desktop.slTracker.getService();
      if(sls != null) {
        String level = "";
        try {
          level = Integer.toString(sls.getBundleStartLevel(b));
        } catch (IllegalArgumentException e) {
          level = "not managed";
        }
        appendRow(sb, "Start level", level);
      }

      // Spacer for better layout (and separation of non-manifest data):
      appendRow(sb, "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", "");

      List headerKeys = new ArrayList(headers.size());
      for(Enumeration e = headers.keys(); e.hasMoreElements(); ) {
        headerKeys.add(e.nextElement());
      }
      Collections.sort(headerKeys);
      for(Iterator it = headerKeys.iterator(); it.hasNext(); ) {
        String  key   = (String)it.next();
        String  value = (String)headers.get(key);
        if(value != null && !"".equals(value)) {
          value = Strings.replace(value, "<", "&lt;");
          value = Strings.replace(value, ">", "&gt;");
          if("Import-Package".equals(key) ||
             "Export-Service".equals(key) ||
             "Bundle-Classpath".equals(key) ||
             "Classpath".equals(key) ||
             "Import-Service".equals(key) ||
             "Export-Package".equals(key)) {
            value = Strings.replaceWordSep(value,",", "<br>", '"');
          } else if("Service-Component".equals(key)) {
            StringBuffer sb2 = new StringBuffer();
            Util.resourceLink(sb2, value);
            value = sb2.toString();
          } else {
            if(value.startsWith("http:") ||
               value.startsWith("https:") ||
               value.startsWith("ftp:") ||
               value.startsWith("file:")) {
              value = "<a href=\"" + value + "\">" + value + "</a>";
            }
          }
          appendRow(sb, key, value);
        }
      }
      sb.append("</table>");
      return sb;
    }
  }

}
