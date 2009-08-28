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

package org.knopflerfish.bundle.desktop.cm;

import java.net.URL;
import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import org.osgi.framework.*;
import java.lang.reflect.Array;


public class Util {
  /** 
   * Default whitespace string for splitwords().
   * Value is <tt>" \t\n\r"</tt>)
   */
  protected static String  WHITESPACE = " \t\n\r";

  /**
   * Default citation char for splitwords().
   * Value is <tt>'"'</tt>
   */
  protected static char   CITCHAR    = '"';
  
  
  
  /**
   * Split a string into words separated by whitespace.
   * <p>
   * Citation chars may be used to group words with embedded
   * whitespace.
   * </p>
   *
   * @param s          String to split.
   * @param whiteSpace whitespace to use for splitting. Any of the
   *                   characters in the whiteSpace string are considered
   *                   whitespace between words and will be removed
   *                   from the result. If no words are found, return an
   *                   array of length zero.
   * @param citChar    Citation character used for grouping words with 
   *                   embedded whitespace. Typically '"'
   */
  public static String [] splitwords(String s, 
				     String whiteSpace,
				     char   citChar,
				     Hashtable keepWhite) {
    boolean       bCit  = false;        // true when inside citation chars.
    Vector        v     = new Vector(); // (String) individual words after splitting
    StringBuffer  buf   = null; 
    int           i     = 0; 
    
    while(i < s.length()) {
      char c = s.charAt(i);

      if(bCit || whiteSpace.indexOf(c) == -1) {
	// Build up word until we breaks on either a citation char or whitespace
	if(c == citChar) {
	  bCit = !bCit;
	} else {
	  if(buf == null) {
	    buf = new StringBuffer();
	  }
	  buf.append(c);
	}
	i++;
      } else {	
	// found whitespace or end of citation, append word if we have one
	String w = "" + c;
	//	System.out.println("white=" + w);
	if(whiteSpace.indexOf(c) != -1 && keepWhite.containsKey(w)) {
	  v.addElement(w);
	}
	if(buf != null) {
	  v.addElement(buf.toString());
	  buf = null;
	}

	// and skip whitespace so we start clean on a word or citation char
	while((i < s.length()) && (-1 != whiteSpace.indexOf(s.charAt(i)))) {
	  i++;
	}
      }
    }

    // Add possible remaining word
    if(buf != null) {
      v.addElement(buf.toString());
    }
    
    // Copy back into an array
    String [] r = new String[v.size()];
    v.copyInto(r);
    
    return r;
  }

  public static String getBundleName(Bundle b) {
    String s = getHeader(b, "Bundle-Name", "");
    if(s == null || "".equals(s) || s.startsWith("%")) {
      s = shortLocation(b.getLocation());
    }

    return s;
  }

  static public void openExternalURL(URL url) throws IOException {
    if(Util.isWindows()) {
      // Yes, this only works on windows
      String systemBrowser = "explorer.exe";
      Runtime rt = Runtime.getRuntime();
      Process proc = rt.exec(new String[] {
	systemBrowser, 
	"\"" + url.toString() + "\"",
      });
    } else {
      throw new IOException("Only windows browsers are yet supported");
    }
  }
  
  public static boolean isWindows() {
    String os = System.getProperty("os.name");
    if(os != null) {
      return -1 != os.toLowerCase().indexOf("win");
    }
    return false;
  }


  public static Object toArray(Vector v, Class clazz) {
    if(v == null) {
      return null;
    }
    Object array = (Object)Array.newInstance(clazz, 
						 v != null ? v.size() : 0);

    for(int i = 0; i < v.size(); i++) {
      Array.set(array, i, v.elementAt(i));
    }
    return array;
  }

  /**
   * Convert an object for which obj.getClass().isArray() == true
   * to a vector of boxed values.
   */
  public static Vector toVector(Object array) {
    if(array == null) {
      return null;
    }
    Vector v = new Vector();
    for(int i = 0; array != null && i < Array.getLength(array); i++) {
      v.addElement(Array.get(array, i));
    }
    return v;
  }

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

  public static String getHeader(Bundle b, String name) {
    return getHeader(b, name, null);
  }

  public static String getHeader(Bundle b, String name, String def) {
    String s = b != null
      ? (String)b.getHeaders().get(name)
      : def;

    return s;
  }

  public static void startFont(StringBuffer sb) {
    startFont(sb, "-2");
  }
  
  public static void stopFont(StringBuffer sb) {
    sb.append("</font>");
  }
  
  public static void startFont(StringBuffer sb, String size) {
    sb.append("<font size=\"" + size + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">");
  }

  public static final String URL_BUNDLE_PREFIX = "http://127.0.0.1/desktop/bid/";
  public static final String URL_SERVICE_PREFIX = "http://127.0.0.1/desktop/sid/";

  public static void bundleLink(StringBuffer sb, Bundle b) {
    sb.append("<a href=\"" + URL_BUNDLE_PREFIX + b.getBundleId() + "\">");
    sb.append(Util.getBundleName(b));
    sb.append("</a>");
  }

  public static void serviceLink(StringBuffer sb, 
				 ServiceReference sr,
				 String txt) {
    sb.append("<a href=\"" + URL_SERVICE_PREFIX + 
	      sr.getProperty(Constants.SERVICE_ID) + "\">");
    sb.append(txt);
    sb.append("</a>");
  }

  public static boolean isBundleLink(URL url) {
    return url.toString().startsWith(URL_BUNDLE_PREFIX);
  }

  public static boolean isServiceLink(URL url) {
    return url.toString().startsWith(URL_SERVICE_PREFIX);
  }

  public static long bidFromURL(URL url) {
    if(!isBundleLink(url)) {
      throw new RuntimeException("URL '" + url + "' does not start with " + 
				 URL_BUNDLE_PREFIX);
    }
    return Long.parseLong(url.toString().substring(URL_BUNDLE_PREFIX.length()));
  }
  
}

