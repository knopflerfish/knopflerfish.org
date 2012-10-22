/*
 * Copyright (c) 2003-2012, KNOPFLERFISH project
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.startlevel.StartLevel;

public class ManifestHTMLDisplayer extends DefaultSwingBundleDisplayer
                                   implements JHTMLBundleLinkHandler {

  public ManifestHTMLDisplayer(BundleContext bc) {
    super(bc, "Manifest", "Shows bundle manifest", true);
  }

  //-------------------------------- Resource URL ------------------------------
  /**
   * Helper class that handles links to bundle resources.
   */
  public static class ResourceUrl {
    public static final String URL_RESOURCE_PREFIX = "http://desktop/resource/";

    /** Bundle Id of the bundle that owns the resource. */
    private long bid;

    /** Path within the bundle to the resource. */
    private String path;

    public ResourceUrl(URL url) {
      if(!isResourceLink(url)) {
        throw new RuntimeException("URL '" + url + "' does not start with " +
                                   URL_RESOURCE_PREFIX);
      }
      final String urlS = url.toString();
      int start = URL_RESOURCE_PREFIX.length();
      int end = urlS.indexOf('/', start+1);
      if (end<start+1) {
        throw new RuntimeException("Invalid bundle resource URL '" + url
                                   + "' bundle id is missing " + urlS);
      }
      bid = Long.parseLong(urlS.substring(start,end));

      start = end;
      path = urlS.substring(start);
    }

    public ResourceUrl(final Bundle bundle, final String path) {
      bid = bundle.getBundleId();
      this.path = path;
    }

    public static boolean isResourceLink(URL url) {
      return url.toString().startsWith(URL_RESOURCE_PREFIX);
    }

    public long getBid() {
      return bid;
    }

    public String getPath() {
      return path;
    }

    public void resourceLink(final StringBuffer sb) {
      resourceLink(sb, path);
    }

    public void resourceLink(final StringBuffer sb,
                             final String label) {
      sb.append("<a href=\"");
      sb.append(URL_RESOURCE_PREFIX);
      sb.append(bid);
      sb.append("/");
      sb.append(path);
      sb.append("\">");
      sb.append(label);
      sb.append("</a>");

    }
  }
  //-------------------------------- Resource URL ------------------------------

  public JComponent newJComponent() {
    return new JHTML(this);
  }

  public void valueChanged(long  bid) {
    final Bundle[] bl = Activator.desktop.getSelectedBundles();

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      final JHTML comp = (JHTML)it.next();
      comp.valueChanged(bl);
    }
  }

  class JHTML extends JHTMLBundle {
    private static final long serialVersionUID = 1L;

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
      appendRow(sb, "Last modified",
                "" + new SimpleDateFormat().format(new Date(b.getLastModified())));

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
            final StringBuffer sb2 = new StringBuffer(30);
            new ResourceUrl(b, value).resourceLink(sb2);
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

  public boolean canRenderUrl(final URL url) {
    return ResourceUrl.isResourceLink(url);
  }

  public void renderUrl(final URL url, final StringBuffer sb) {
    final ResourceUrl resUrl = new ResourceUrl(url);

    appendResourceHTML(sb, resUrl);
  }

  void appendResourceHTML(final StringBuffer sb, final ResourceUrl resUrl) {
    final Bundle bundle = Activator.getTargetBC_getBundle(resUrl.getBid());
    final URL url = bundle.getResource(resUrl.getPath());

    sb.append("<html>");
    sb.append("<table border=0>");

    sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
    JHTMLBundle.startFont(sb, "-1");
    sb.append("#" + bundle.getBundleId() + " " + resUrl.getPath());
    JHTMLBundle.stopFont(sb);
    sb.append("</td>\n");
    sb.append("</tr>\n");

    sb.append("<tr>");
    sb.append("<td>");
    sb.append("<pre>");
    JHTMLBundle.startFont(sb, "-1");
    try {
      byte[] bytes = Util.readStream(url.openStream());
      String value = new String(bytes);
      value = Strings.replace(value, "<", "&lt;");
      value = Strings.replace(value, ">", "&gt;");

      sb.append(value);
    } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      sb.append(sw.toString());
    }
    JHTMLBundle.stopFont(sb);
    sb.append("</pre>");
    sb.append("</td>");
    sb.append("</tr>");

    sb.append("</table>");
    sb.append("</html>");
  }


}
