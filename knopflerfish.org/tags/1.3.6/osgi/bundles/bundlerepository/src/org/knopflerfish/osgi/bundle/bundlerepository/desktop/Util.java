/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.osgi.bundle.bundlerepository.desktop;

import org.osgi.framework.*;
import java.util.*;
import org.ungoverned.osgi.service.bundlerepository.*;

import java.net.*;
import java.io.*;

import java.lang.reflect.Array;

/**
 * Misc static utility methods
 */
public class Util {

  /**
   * Check if a bundle is installed from repo
   *
   */
  public static boolean isInRepo(Bundle b, String updateURL) {
    String bundleUpLoc = (String)b.getHeaders().get(BundleRecord.BUNDLE_UPDATELOCATION);
    boolean bIsRepoBundle = false;
    
    if(bundleUpLoc != null) {
      bIsRepoBundle = updateURL.equals(bundleUpLoc);
    } else {
      bIsRepoBundle = updateURL.equals(b.getLocation());
    }

    return bIsRepoBundle;
  }


  /**
   * Check if an installed bundle corresponds to an OBR BubdleRecord.
   *
   * <p>
   * Equality is tested on either equal bundle locations, or
   * if bundle name and bundle version are equal.
   * </p>
   *
   */
  static boolean bundleEqual(Bundle b, BundleRecord br) {
    String loc = (String)br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
    if(loc.equals(b.getLocation())) {
      return true;
    }

    // Use UUID if present
    String uuid_br = (String)br.getAttribute("Bundle-UUID");
    String uuid_b  = (String)b.getHeaders().get("Bundle-UUID");
    if(uuid_b != null && uuid_b.equals(uuid_br)) {
      return true;
    }
    
    if(bundleAttrEqual(b, br, "Bundle-Name") &&
       bundleAttrEqual(b, br, "Bundle-Version")) {
      return true;
    }
    return false;
  }

  /**
   * Compare attribute values in a bundle and a bundle record.
   *
   * An attribute value is equal either if its reference equal, or
   * if <tt>equals</tt> on the bundle's attribute value is <tt>true</tt>
   */
  static boolean bundleAttrEqual(Bundle b, BundleRecord br, String attr) {
    String val_br = (String)br.getAttribute(attr);
    String val_b  = (String)b.getHeaders().get(attr);
    return 
      (val_br == val_b) ||
      (val_b != null && val_b.equals(val_br));
  }


  /**
   * Get a string value from a BundleRecord, with default value
   *
   * @param name attribute to get
   * @param def value to return if attribute value is <tt>null</tt> or
   *            the empty string.
   * @return value if the specified attribute or <tt>def</tt> if
   *         the value is <tt>null</tt> or the empty string.
   */
  public static String getAttribute(BundleRecord br, String name, String def) {
    Object obj = br.getAttribute(name);
    String s = def;
    if(obj instanceof String) {
      s = (String)obj;
      if(s == null || "".equals(s)) {
        s = def;
      }
    } else if(obj instanceof java.util.List) {
      java.util.List list = (java.util.List)obj;
      if(list.size() == 1) {
        return list.get(0).toString();
      } else if(list.size() > 1) {
        System.out.println(name + " has more than one entry " + list + ", using the first");
        return list.get(0).toString();
      } else {
        return def;
      }
    } else {
      System.out.println(name + ", type=" + obj.getClass().getName() + " " + obj);
    }
    return s;
  }
  
  
  /**
   * Generic Object to HTML string conversion method.
   */
  public static String toHTML(Object obj) {
    if(obj == null) {
      return "null";
    }
    if(obj instanceof String) {
      String s = (String)obj;
      try {
	URL url = new URL(s);
	return "<a href=\"" + s + "\">" + s + "</a>";
      } catch (Exception e) {
	
      }
      return s;
    } else if(obj.getClass().isArray()) {
      StringBuffer sb = new StringBuffer();
      int len = Array.getLength(obj);

      for(int i = 0; i < len; i++) {
	sb.append(toHTML(Array.get(obj, i)));
	if(i < len - 1) {
	  sb.append("<br>\n");
	}
      }
      return sb.toString();
    } else {
      return obj.toString();
    }
  }
  
  /**
   * Get Name of BundleRecord by first trying "Bundle-Name" attribute, 
   * then last part of "Bundle-UpdateLocation"
   */
  public static String getBRName(BundleRecord br) {
    String s = getAttribute(br, BundleRecord.BUNDLE_NAME, null);
    if(s == null) {
      s = getAttribute(br, BundleRecord.BUNDLE_UPDATELOCATION, "no name");
      int ix = s.lastIndexOf('/');
      if(ix != -1) {
	s = s.substring(ix + 1);
      }
    }
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

  public static void openExternalURL(URL url) throws IOException {
    if(isWindows()) {
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
}
