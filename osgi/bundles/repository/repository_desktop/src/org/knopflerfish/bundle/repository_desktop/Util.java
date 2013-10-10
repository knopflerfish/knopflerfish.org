/*
 * Copyright (c) 2004-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repository_desktop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

/**
 * Misc static utility methods
 */
public class Util {

  /**
   * Check if a bundle object corresponds to a resource.
   *
   * <p>
   * Equality is tested on
   * <ol>
   *   <li> location,
   *   <li> bundle symbolic name and version.
   * </ol>
   * </p>
   *
   * @param bundle The bundle to test.
   * @param resource The resource to compare the bundle to.
   */
  static boolean bundleEqual(Bundle bundle, Resource resource) {
    if (bundle.getLocation().equals(Util.getLocation(resource))) {
      return true;
    }

    final String bsn = getResourceName(resource);
    if (bsn.equals(bundle.getSymbolicName())) {
      final Version version = getResourceVersion(resource);
      return bundle.getVersion().equals(version);
    }
    return false;
  }

  /**
   * Get the name of repository resource from the identity name space."
   */
  public static String getResourceName(Resource resource)
  {
    final List<Capability> idCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
    if (idCaps.size() != 1) {
      Activator.log.error("Found " + idCaps.size() +" identity capabilites expected one: " +idCaps);
      return resource.toString();
    }
    final Capability idCap = idCaps.get(0);
    return idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE).toString();
  }


  /**
   * Get version of repository resource form the identity name space.
   */
  public static Version getResourceVersion(Resource resource)
  {
    final List<Capability> idCaps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
    if (idCaps.size() != 1) {
      Activator.log.error("Found " + idCaps.size() +" identity capabilites expected one: " +idCaps);
      return Version.emptyVersion;
    }
    final Capability idCap = idCaps.get(0);
    return (Version) idCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
  }

  /**
   * Get the MIME-type of a repository resource.
   *
   * @param resource
   *          The resource to get the MIME type for.
   *
   * @return Resource MIME type or {@code null} if no MIME-type available.
   */
  public static String getResourceMime(Resource resource)
  {
    String res = null;
    for (final Capability cap : resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)) {
      final Map<String, Object> attrs = cap.getAttributes();
      final String mime = (String) attrs.get(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE);
      if (mime != null) {
        res = mime;
        break;
      }
    }
    return res;
  }

  /**
   * Get the URL of a repository resource.
   *
   * @param resource
   *          The resource to get the URL for.
   *
   * @return Resource download URL or {@code null} if no URL available.
   */
  public static URL getResourceUrl(Resource resource)
  {
    URL res = null;
    for (final Capability cap : resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)) {
      final Map<String, Object> attrs = cap.getAttributes();
      final String url = (String) attrs.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
      if (url != null) {
        try {
          res = new URL(url);
          break;
        } catch (final Exception e) {
          Activator.log.warn("Failed to parse reosurce URL '" + url + "' for "
                             + resource, e);
        }
      }
    }
    return res;
  }

  /**
   * Get the size in bytes of a repository resource.
   *
   * @param resource
   *          The resource to get the URL for.
   *
   * @return Resource size in byte or {@code -1} if no size available.
   */
  public static long getResourceSize(Resource resource)
  {
    long res = -1;
    for (final Capability cap : resource
        .getCapabilities(ContentNamespace.CONTENT_NAMESPACE)) {
      final Map<String, Object> attrs = cap.getAttributes();
      final Long size = (Long) attrs.get(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE);
      if (size != null) {
        res = size.longValue();
      }
    }
    return res;
  }

  static final Set<String> supportedMimeTypes = new TreeSet<String>();
  static {
    supportedMimeTypes.add(BundleRequirement.MIME_BUNDLE);
    supportedMimeTypes.add(BundleRequirement.MIME_BUNDLE_ALT);
  }

  /**
   * Get the location to use when installing this resource.
   *
   * If available, the resource URL will be used as the location. Otherwise we
   * simply use the hash code of the resource.
   *
   * @param resource
   *          the resource to determine the installation location for.
   * @return location to use when installing this resource.
   */
  public static String getLocation(Resource resource)
  {
    for (final Capability cap : resource
        .getCapabilities(ContentNamespace.CONTENT_NAMESPACE)) {
      final Map<String, Object> attrs = cap.getAttributes();
      if (supportedMimeTypes.contains(attrs
          .get(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE))) {
        final String url =
          (String) attrs.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        if (url != null) {
          return url;
        }
      }
    }
    // All resources this class works with are supposed to be bundles with a
    // valid download URL in the osgi.content name space. If not then simply
    // return something unique for the resource.
    return String.valueOf(resource.hashCode());
  }

  /**
   * Generic Object to HTML string conversion method.
   */
  public static String toHTML(Object obj) {
    if(obj == null) {
      return "null";
    }
    if(obj instanceof String) {
      final String s = (String) obj;
      try {
        new URL(s);
        return "<a href=\"" + s + "\">" + s + "</a>";
      } catch (final Exception e) {
      }
      return s;
    } else if(obj.getClass().isArray()) {
      final StringBuffer sb = new StringBuffer();
      final int len = Array.getLength(obj);

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

  public static void startFont(StringBuffer sb) {
    startFont(sb, "-2");
  }

  public static void startFont(StringBuffer sb, String size) {
    sb.append("<font size=\"" + size + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">");
  }

  public static void stopFont(StringBuffer sb) {
    sb.append("</font>");
  }

  public static void openExternalURL(URL url) throws IOException {
    if(Util.isWindows()) {
      // Yes, this only works on windows
      final String systemBrowser = "explorer.exe";
      final Runtime rt = Runtime.getRuntime();
      final Process proc = rt.exec(new String[] {
        systemBrowser,
        "\"" + url.toString() + "\"",
      });
      new StreamGobbler(proc.getErrorStream());
      new StreamGobbler(proc.getInputStream());
    } else if (Util.isMacOSX()) {
      // Yes, this only works on Mac OS X
      final Runtime rt = Runtime.getRuntime();
      final Process proc = rt.exec(new String[] {
        "/usr/bin/open",
        url.toString(),
      });
      new StreamGobbler(proc.getErrorStream());
      new StreamGobbler(proc.getInputStream());
    } else {
      throw new IOException
        ("Only windows and Mac OS X browsers are yet supported");
    }
  }

  public static boolean isWindows() {
    final String os = System.getProperty("os.name");
    if(os != null) {
      return -1 != os.toLowerCase().indexOf("win");
    }
    return false;
  }


  public static boolean isMacOSX() {
    final String os = System.getProperty("os.name");
    return "Mac OS X".equals(os);
  }

  /** A thread that empties an input stream without complaining.*/
  static class StreamGobbler extends Thread
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
        while (null!=line) {
          line = br.readLine();
          //System.out.println(line);
        }
      } catch (final IOException _ioe) {
      }
    }
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


}
