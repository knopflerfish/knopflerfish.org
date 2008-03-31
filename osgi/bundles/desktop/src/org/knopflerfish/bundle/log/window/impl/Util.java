/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

import java.util.*;
import java.text.*;
import java.io.*;
import java.net.URL;

import org.osgi.framework.*;
import org.osgi.service.log.*;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.Text;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;


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
    int ix = s.lastIndexOf("/");
    if(ix == -1) ix = s.lastIndexOf("\\");
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

  public static String getHeader(Bundle b, String name) {
    return getHeader(b, name, null);
  }

  public static String getHeader(Bundle b, String name, String def) {
    String s = b != null
      ? (String)b.getHeaders().get(name)
      : def;

    return s;
  }


  public static String toHTML(ExtLogEntry e) {
    StringBuffer sb = new StringBuffer();

    sb.append("<html>");

    sb.append("<table border=0 width=\"100%\">");
    sb.append("<tr bgcolor=\"#eeeeee\">");

    sb.append("<td width=50 valign=top align=left bgcolor=\"#eeeeee\">" + 
	      fontify(e.getId() + 
		      ", "+ shortName(e.getBundle())) + "</td>")
      ;
    
    sb.append("<td  valign=top  align=left bgcolor=\"#eeeeee\">" + 
	      fontify(tf.format(new Date(e.getTime()))) + "</td>")
;

    sb.append("<td valign=top align=right bgcolor=\"#eeeeee\">" + 
	      fontify(levelString(e.getLevel())) + "</td>");


    sb.append("</tr>");

    sb.append("<tr>");
    sb.append("<td width=\"100%\" colspan=3>");
    sb.append(fontify(e.getMessage()));
    sb.append("</td>");
    sb.append("</tr>");
    
    Throwable t = e.getException();
    if(t != null) {
      sb.append("<tr bgcolor=\"#eeeeee\">");
      
      sb.append("<td colspan=3 align=left bgcolor=\"#eeeeee\">" + 
		fontify("Exception"));
      sb.append("</td>");
      sb.append("</tr>");

      sb.append("<tr>");
      sb.append("<td colspan=3>");
      
      StringWriter w = new StringWriter();
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

  public static String toString(LogEntry e) {
    String s = 
      "Time: "      + tf.format(new Date(e.getTime())) + "\n" + 
      "Level: "     + levelString(e.getLevel()) + "\n" + 
      "Message: "   + e.getMessage() + "\n";
    Throwable t = e.getException();
    if(t != null) {
      StringWriter w = new StringWriter();
      t.printStackTrace(new PrintWriter(w));
      s = s + w.toString();
    } 

    return s;
  }


  static SimpleDateFormat tf = new SimpleDateFormat("MMM dd HH:mm:ss ");

  static String[] levels = {
    "error",
    "warning",
    "info", 
    "debug",
  };

  public static String levelString(int n) {
    try {
      return levels[n-1];
    } catch (Exception e) {
      return "Unknown:" + n;
    }
  }

  public static StringBuffer pad(StringBuffer sb, int n) {
    while(sb.length() < n) sb.append(" ");
    return sb;
  }

}





