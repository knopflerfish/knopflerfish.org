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

package org.knopflerfish.bundle.desktop.event;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.Iterator;

import org.knopflerfish.util.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import java.lang.reflect.Array;


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
    if(b == null) {
      return "";
    }

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

  public static int getId(Event e) {
    return e.hashCode();
  }

  public static Bundle getBundle(Event e) {
    Bundle b = (Bundle)e.getProperty("bundle");
    return b;
  }

  public static Throwable getException(Event e) {
    return (Throwable)e.getProperty("exception");
  }

  public static ServiceReference getServiceReference(Event e) {
    return (ServiceReference)e.getProperty("service");
  }

  public static long getTime(Event e) {
    return getNumber(e, "timestamp", new Long(0)).longValue();
  }

  public static long getBundleId(Event e) {
    return getNumber(e, "bundle.id", new Long(-1)).longValue();
  }
  
  public static int getLevel(Event e) {
    return getNumber(e, "log.level", new Integer(0)).intValue();
  }

  public static String getMessage(Event e) {
    return getString(e, "message", "");
  }

  public static Number getNumber(Event e, String key, Number def) {
    Object v = e.getProperty(key);
    if(!(v instanceof Number)) {
      return def;
    }

    return (Number)v;
  }


  public static String getString(Event e, String key, String def) {
    Object v = e.getProperty(key);
    return v == null ? def : v.toString();
  }



  static Set SKIP_KEYS = new HashSet() {{
    add("log.entry");
    add("event");
    add(EventConstants.BUNDLE);
    add(EventConstants.EXCEPTION);
    add(EventConstants.SERVICE);
  }};

  public static String objToHTML(Object obj) {
    if(obj == null) {
      return "";
    } else if(obj instanceof Map) {
      return mapToHTML(obj);
    } else if(obj.getClass().isArray()) {
      return arrayToHTML(obj);
    } else {
      return fontify(obj.toString());
    }
  }

  public static String mapToHTML(Object obj) {
    if(obj == null) {
      return "null";
    }
    StringBuffer sb = new StringBuffer();
    Map map = (Map)obj;
    sb.append("<table>");
    for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
      Object key = it.next();
      Object val = map.get(key);
      sb.append("<tr>");
      sb.append("<td>");
      sb.append(fontify(key.toString()));
      sb.append("</td>");
      sb.append("<td>");
      sb.append(fontify(val.toString()));
      sb.append("</td>");
      sb.append("</tr>");
    }
    sb.append("</table>");
    return sb.toString();
  }

  public static String arrayToHTML(Object obj) {
    if(obj == null) {
      return "null";
    }
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for(int i = 0; i < Array.getLength(obj); i++) {
      if(i > 0) {
        sb.append(", ");
      }
      Object val = Array.get(obj, i);
      sb.append(objToHTML(val));
    }
    sb.append("]");
    return sb.toString();
  }

  public static String toHTML(Event e) {
    StringBuffer sb = new StringBuffer();

    String[] names = e.getPropertyNames();

    Set keys = new TreeSet();
    for(int i = 0; i < names.length; i++) {
      // if(!SKIP_KEYS.contains(names[i])) 
        {
          keys.add(names[i]);
        }
    }

    sb.append("<html>");

    sb.append("<table border=0 width=\"100%\">\n");

    sb.append("<tr bgcolor=\"#eeeeee\">");
    sb.append("<td  valign=top  align=left bgcolor=\"#eeeeee\">" + 
	      fontify(tf.format(new Date(getTime(e)))) + "</td>");
    sb.append("<td  valign=top  align=right bgcolor=\"#eeeeee\">" + 
	      fontify(e.getTopic()) + "</td>");    
    sb.append("</tr>");

    sb.append("<tr bgcolor=\"#eeeeee\">");
        sb.append("<td  valign=top  colspan=2 align=left bgcolor=\"#eeeeee\">" + 
	      fontify(getMessage(e)) + "</td>");
    sb.append("</tr>");
    
    Throwable t = getException(e);

    if(t != null) {
      StringWriter w = new StringWriter();
      t.printStackTrace(new PrintWriter(w));
      sb.append("<td colspan=2>");
      sb.append("<pre>");
      sb.append(w.toString());
      sb.append("</pre>");
      sb.append("</td>");
    } 

    for(Iterator it = keys.iterator(); it.hasNext();) {
      String key = (String)it.next();
      sb.append("<tr>");
      
      sb.append("<td>");
      sb.append(fontify(key));
      sb.append("</td>");

      sb.append("<td>");
      Object val = e.getProperty(key);
      if(SKIP_KEYS.contains(key)) {
        sb.append(fontify("<i>" + val.getClass().getName() + "@" + val.hashCode() + "</i>"));
      } else {
        sb.append(objToHTML(val));
      }
      sb.append("</td>");
      
      sb.append("</tr>");
    }


    sb.append("</tr>\n");

    sb.append("</table>\n");
    sb.append("</html>");

    return sb.toString();
  }

    
  public static String toHTMLLogEntry(Event e) {

    StringBuffer sb = new StringBuffer();

    sb.append("<html>");

    sb.append("<table border=0 width=\"100%\">");
    sb.append("<tr bgcolor=\"#eeeeee\">");

    sb.append("<td  valign=top  align=left bgcolor=\"#eeeeee\">" + 
	      fontify(tf.format(new Date(getTime(e)))) + "</td>");

    sb.append("<td colspan=2 valign=top align=right bgcolor=\"#eeeeee\">" + 
	      fontify(levelString(getLevel(e))) + "</td>");


    sb.append("</tr>");

    sb.append("<tr>");
    sb.append("<td width=\"100%\" colspan=3>");
    sb.append(fontify(getMessage(e)));
    sb.append("</td>");
    sb.append("</tr>");
    
    Throwable t = getException(e);
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
    if(s == null) {
      return "";
    }
    return "<font size=\"" + size + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + s + "</font>";
  }
  
  public static String toString(Event e) {
    String topic = e.getTopic();
    if(topic.startsWith("org/osgi/service/log/LogEntry")) {
      return toStringLogEntry(e);
    } else {
      return e.toString();
    }    
  }

  public static String toStringLogEntry(Event e) {
    String s = 
      "Time: "      + tf.format(new Date(getTime(e))) + "\n" + 
      "Level: "     + levelString(getLevel(e)) + "\n" + 
      "Message: "   + getMessage(e) + "\n";
    Throwable t = getException(e);
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





