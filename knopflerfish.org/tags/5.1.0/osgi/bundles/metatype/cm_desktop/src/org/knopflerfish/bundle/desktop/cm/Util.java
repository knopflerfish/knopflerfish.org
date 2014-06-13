/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;


public class Util
{
  /**
   * Default whitespace string for splitwords(). Value is <tt>" \t\n\r"</tt>)
   */
  protected static String WHITESPACE = " \t\n\r";

  /**
   * Default citation char for splitwords(). Value is <tt>'"'</tt>
   */
  protected static char CITCHAR = '"';

  public static String getBundleName(Bundle b)
  {
    String s = getHeader(b, "Bundle-Name", "");
    if (s == null || "".equals(s) || s.startsWith("%")) {
      s = shortLocation(b.getLocation());
    }

    return s;
  }

  public static Comparator<Bundle> bundleNameComparator = new Comparator<Bundle>() {
    @Override
    public int compare(Bundle b1, Bundle b2) {
      if(b1 == b2) {
        return 0;
      }
      if(b1 == null) {
        return -1;
      }
      if(b2 == null) {
        return 1;
      }

      return
        getBundleName(b1).compareToIgnoreCase(getBundleName(b2));
    }
  };

  static public void openExternalURL(URL url)
      throws IOException
  {
    if (Util.isWindows()) {
      // Yes, this only works on windows
      final String systemBrowser = "explorer.exe";
      final Runtime rt = Runtime.getRuntime();
      final Process proc =
        rt.exec(new String[] { systemBrowser, "\"" + url.toString() + "\"", });
      new StreamGobbler(proc.getErrorStream());
      new StreamGobbler(proc.getInputStream());
    } else if (Util.isMacOSX()) {
      // Yes, this only works on Mac OS X
      final Runtime rt = Runtime.getRuntime();
      final Process proc =
        rt.exec(new String[] { "/usr/bin/open", url.toString(), });
      new StreamGobbler(proc.getErrorStream());
      new StreamGobbler(proc.getInputStream());
    } else {
      throw new IOException(
                            "Only windows and Mac OS X browsers are yet supported");
    }
  }

  public static boolean isWindows()
  {
    final String os = Activator.bc.getProperty("os.name");
    if (os != null) {
      return -1 != os.toLowerCase().indexOf("win");
    }
    return false;
  }

  public static boolean isMacOSX()
  {
    final String os = Activator.bc.getProperty("os.name");
    return "Mac OS X".equals(os);
  }

  /** A thread that empties an input stream without complaining. */
  static class StreamGobbler
    extends Thread
  {
    InputStream is;

    StreamGobbler(InputStream is)
    {
      this.is = is;
      start();
    }

    @Override
    public void run()
    {
      final BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line = "";
      try {
        while (null != line) {
          line = br.readLine();
          // System.out.println(line);
        }
      } catch (final IOException _ioe) {
      }
    }
  }

  public static Object toArray(Vector<Object> v, Class<?> clazz)
  {
    if (v == null) {
      return null;
    }
    final Object array = Array.newInstance(clazz, v != null ? v.size() : 0);

    for (int i = 0; i < v.size(); i++) {
      Array.set(array, i, v.elementAt(i));
    }
    return array;
  }

  /**
   * Convert an object for which obj.getClass().isArray() == true to a vector of
   * boxed values.
   */
  public static Vector<Object> toVector(Object array)
  {
    if (array == null) {
      return null;
    }
    final Vector<Object> v = new Vector<Object>();
    for (int i = 0; array != null && i < Array.getLength(array); i++) {
      v.addElement(Array.get(array, i));
    }
    return v;
  }

  public static String shortLocation(String s)
  {
    int ix = s.lastIndexOf("/");

    // handle eclipse extended location directory syntax
    if (s.endsWith("/")) {
      ix = s.lastIndexOf("/", ix - 1);
    }

    if (ix == -1) {
      ix = s.lastIndexOf("\\");
    }
    if (ix != -1) {
      return s.substring(ix + 1);
    }
    return s;
  }

  public static String getHeader(Bundle b, String name)
  {
    return getHeader(b, name, null);
  }

  public static String getHeader(Bundle b, String name, String def)
  {
    final String s = b != null ? (String) b.getHeaders().get(name) : def;

    return s;
  }

  public static void startFont(StringBuffer sb)
  {
    startFont(sb, "-2");
  }

  public static void stopFont(StringBuffer sb)
  {
    sb.append("</font>");
  }

  public static void startFont(StringBuffer sb, String size)
  {
    sb.append("<font size=\"" + size
              + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">");
  }

  public static final String URL_CM_HOST = "desktop";
  public static final String URL_CM_PATH_PREFIX = "/cm";
  public static final String URL_CM = "http://" +URL_CM_HOST +URL_CM_PATH_PREFIX;
  /** Name of mandatory parameter for URL_CM URLs. */
  public static final String URL_CM_CMD = "cmd";
  /** Value of mandatory URL_CMD parameter for URL_CM URLs. */
  public static final String URL_CM_CMD_IMPORT = "Import...";


  public static final String URL_BUNDLE_PREFIX =
    "http://127.0.0.1/desktop/bid/";
  public static final String URL_SERVICE_PREFIX =
      "http://127.0.0.1/desktop/sid/";

  public static void bundleLink(StringBuffer sb, Bundle b)
  {
    sb.append("<a href=\"" + URL_BUNDLE_PREFIX + b.getBundleId() + "\">");
    sb.append(Util.getBundleName(b));
    sb.append("</a>");
  }

  public static void serviceLink(StringBuffer sb,
                                 ServiceReference<?> sr,
                                 String txt)
  {
    sb.append("<a href=\"" + URL_SERVICE_PREFIX
              + sr.getProperty(Constants.SERVICE_ID) + "\">");
    sb.append(txt);
    sb.append("</a>");
  }

  public static boolean isBundleLink(URL url)
  {
    return url.toString().startsWith(URL_BUNDLE_PREFIX);
  }

  public static boolean isServiceLink(URL url)
  {
    return url.toString().startsWith(URL_SERVICE_PREFIX);
  }

  public static boolean isImportLink(URL url)
  {
    return URL_CM_HOST.equals(url.getHost())
        && url.getPath().startsWith(URL_CM_PATH_PREFIX)
        && paramsFromURL(url).get(URL_CM_CMD).equals(URL_CM_CMD_IMPORT);
  }

  public static long bidFromURL(URL url)
  {
    if (!isBundleLink(url)) {
      throw new RuntimeException("URL '" + url + "' does not start with "
                                 + URL_BUNDLE_PREFIX);
    }
    return Long.parseLong(url.toString().substring(URL_BUNDLE_PREFIX.length()));
  }


  public static Map<String, String> paramsFromURL(final URL url)
  {
    final Map<String, String> res = new HashMap<String, String>();
    final String query = url.getQuery();
    if (null != query) {
      final StringTokenizer st = new StringTokenizer(query, "&");
      while (st.hasMoreTokens()) {
        final String tok = st.nextToken();
        final int delimPos = tok.indexOf('=');
        final String key = tok.substring(0, delimPos).trim();
        final String value = tok.substring(delimPos + 1).trim();

        res.put(key, value);
      }
    }
    return res;
  }


}
