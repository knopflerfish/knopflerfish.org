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

package org.knopflerfish.bundle.log.window.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;

import org.knopflerfish.util.Text;
import org.osgi.service.log.LogLevel;


public class Util {

 public static String shortLocation(String s) {
    int ix = s.lastIndexOf("/");

    // handle eclipse extended location directory syntax
    if(s.endsWith("/")) {
      ix = s.lastIndexOf("/", ix - 1);
    }

    if(ix == -1) {
      ix = s.lastIndexOf("\\");
    }
    if(ix != -1) {
      return s.substring(ix + 1);
    }
    return s;
  }

  public static String shortName(Bundle b) {
    String s = b.getLocation();
    if (null==s) {
      // Remote, uninstalled bundle may give null location.
      return String.valueOf(b.getBundleId());
    }
    int ix = s.lastIndexOf("/");
    if(ix == -1) {
      ix = s.lastIndexOf("\\");
    }
    if(ix != -1) {
      s = s.substring(ix + 1);
    }
    if(s.endsWith(".jar")) {
      s = s.substring(0, s.length() - 4);
    }
    return s;
  }

  public static String getBundleName(Bundle b) {
    String s = getHeader(b, "Bundle-Name", "");
    if(s == null || "".equals(s) || s.startsWith("%")) {
      s = shortLocation(b.getLocation());
    }

    return s;
  }

  public static String getHeader(Bundle b, String name, String def) {
    return b != null
      ? b.getHeaders().get(name)
      : def;
  }


  public static String toHTML(ExtLogEntry e) {
    final StringBuilder sb = new StringBuilder();

    sb.append("<html>");

    sb.append("<table border=0 width=\"100%\">");
    sb.append("<tr bgcolor=\"#eeeeee\">");

    sb.append("<td width=50 valign=top align=left bgcolor=\"#eeeeee\">")
        .append(fontify(e.getId() + ", " + shortName(e.getBundle())))
        .append("</td>");

    sb.append("<td  valign=top  align=left bgcolor=\"#eeeeee\">")
        .append(fontify(tf.format(new Date(e.getTime()))))
        .append("</td>");

    sb.append("<td valign=top align=right bgcolor=\"#eeeeee\">")
        .append(fontify(levelString(e.getLogLevel())))
        .append("</td>");

    sb.append("</tr>");

    sb.append("<tr>");
    sb.append("<td width=\"100%\" colspan=3>");
    sb.append(fontify(quoteHtml(e.getMessage())));
    sb.append("</td>");
    sb.append("</tr>");

    final ServiceReference<?> sr = e.getServiceReference();
    if (null!=sr) {
      sb.append("<tr bgcolor=\"#eeeeee\">");
      sb.append("<td width=\"100%\" colspan=\"3\">");
      sb.append(fontify("Service Properties"));
      sb.append("</td>");
      sb.append("</tr>");
      final String[] propKeys = sr.getPropertyKeys();
      for (final String propKey : propKeys) {
        // Reuse service reference properties presentation form the
        // services tab.
        final StringWriter sw = new StringWriter();
        final PrintWriter  pr = new PrintWriter(sw);
        try {
          org.knopflerfish.bundle.desktop.swing.Util
            .printObject(pr, sr.getProperty(propKey));
        } catch (final IOException ignored) {
        }

        sb.append("<tr>");
        sb.append("<td valign=top align=left>")
            .append(fontify(propKey))
            .append("</td>");
        sb.append("<td valign=top align=left colspan=\"2\">")
            .append(fontify(sw.toString()))
            .append("</td>");
        sb.append("</tr>");
      }
    }

    final Throwable t = e.getException();
    if(t != null) {
      sb.append("<tr bgcolor=\"#eeeeee\">");

      sb.append("<td colspan=3 align=left bgcolor=\"#eeeeee\">")
          .append(fontify("Exception"));
      sb.append("</td>");
      sb.append("</tr>");

      sb.append("<tr>");
      sb.append("<td colspan=3>");

      final StringWriter w = new StringWriter();
      t.printStackTrace(new PrintWriter(w));
      sb.append(fontify(Text.replace(w.toString(), "\n", "<br>")));
      sb.append("</td>");
      sb.append("</tr>");
    }

    sb.append("</table>");

    sb.append("</font>\n");
    sb.append("</html>");

    return sb.toString();
  }

  static public String fontify(String s) {
    return fontify(s, "-2");
  }

  static public String fontify(String s, String size) {
    return "<font size=\"" + size + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + s + "</font>";
  }

  static public String quoteHtml(final String s) {
    String res = s.replace("&", "&amp;");
    res = res.replace("<", "&lt;");
    res = res.replace(">", "&gt;");
    res = res.replace("\n", "<br>");

    return res;
  }

  public static String toString(LogEntry e) {
    String s =
      "Time: "      + tf.format(new Date(e.getTime())) + "\n" +
      "Level: "     + e.getLogLevel() + "\n" +
      "Message: "   + e.getMessage() + "\n";
    final Throwable t = e.getException();
    if(t != null) {
      final StringWriter w = new StringWriter();
      t.printStackTrace(new PrintWriter(w));
      s = s + w.toString();
    }

    return s;
  }


  static SimpleDateFormat tf = new SimpleDateFormat("MMM dd HH:mm:ss ");

  public static String levelString(LogLevel logLevel) {
    return logLevel.name().toLowerCase();
  }

}
