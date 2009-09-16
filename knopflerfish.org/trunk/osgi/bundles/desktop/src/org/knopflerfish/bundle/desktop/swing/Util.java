/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.*;
import org.osgi.service.packageadmin.*;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import javax.swing.*;

import org.knopflerfish.util.Text;

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

  public static final String URL_BUNDLE_PREFIX = "http://desktop/bid/";
  public static final String URL_SERVICE_PREFIX = "http://desktop/sid/";
  public static final String URL_RESOURCE_PREFIX = "http://desktop/resource/";

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

  public static void resourceLink(StringBuffer sb,
                                  String path) {
    sb.append("<a href=\"" + URL_RESOURCE_PREFIX + path + "\">");
    sb.append(path);
    sb.append("</a>");
  }

  public static boolean isBundleLink(URL url) {
    return url.toString().startsWith(URL_BUNDLE_PREFIX);
  }

  public static boolean isServiceLink(URL url) {
    return url.toString().startsWith(URL_SERVICE_PREFIX);
  }

  public static boolean isResourceLink(URL url) {
    return url.toString().startsWith(URL_RESOURCE_PREFIX);
  }

  public static String resourcePathFromURL(URL url) {
    if(!isResourceLink(url)) {
      throw new RuntimeException("URL '" + url + "' does not start with " +
                                 URL_RESOURCE_PREFIX);
    }
    return url.toString().substring(URL_RESOURCE_PREFIX.length());
  }

  public static long bidFromURL(URL url) {
    if(!isBundleLink(url)) {
      throw new RuntimeException("URL '" + url + "' does not start with " +
                                 URL_BUNDLE_PREFIX);
    }
    return Long.parseLong(url.toString().substring(URL_BUNDLE_PREFIX.length()));
  }

  public static long sidFromURL(URL url) {
    if(!isServiceLink(url)) {
      throw new RuntimeException("URL '" + url + "' does not start with " +
                                 URL_SERVICE_PREFIX);
    }
    return Long.parseLong(url.toString().substring(URL_SERVICE_PREFIX.length()));
  }

  public static String serviceEventName(int type) {
    switch(type) {
    case ServiceEvent.REGISTERED:    return "registered";
    case ServiceEvent.UNREGISTERING: return "unregistering";
    case ServiceEvent.MODIFIED:      return "modified";
    default:                      return "<" + type + ">";
    }
  }

  public static String bundleEventName(int type) {
    switch(type) {
    case BundleEvent.INSTALLED:   return "installed";
    case BundleEvent.STARTED:     return "started";
    case BundleEvent.STOPPED:     return "stopped";
    case BundleEvent.UNINSTALLED: return "uninstalled";
    case BundleEvent.UPDATED:     return "updated";
    default:                      return "<" + type + ">";
    }
  }

  public static Object getProp(ServiceReference sr,
                               String key,
                               Object def) {
    Object obj = sr.getProperty(key);
    return obj != null ? obj : def;
  }

  public static String getStringProp(ServiceReference sr,
                                     String key,
                                     String def) {
    return (String)getProp(sr, key, def);
  }

  public static boolean getBooleanProp(ServiceReference sr,
                                       String key,
                                       boolean def) {
    return ((Boolean)getProp(sr, key, def ? Boolean.TRUE : Boolean.FALSE))
      .booleanValue();
  }

  public static String stateName(int state) {
    switch(state) {
    case Bundle.ACTIVE:
      return "active";
    case Bundle.INSTALLED:
      return "installed";
    case Bundle.UNINSTALLED:
      return "uninstalled";
    case Bundle.RESOLVED:
      return "resolved";
    case Bundle.STARTING:
      return "starting";
    case Bundle.STOPPING:
      return "stopping";
    default:
      return "unknown " + state;
    }
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

  public static String getBundleName(Bundle b) {
    if(b == null) {
      return "null";
    }
    String s = getHeader(b, "Bundle-Name", "");
    if(s == null || "".equals(s) || s.startsWith("%")) {
      String loc = b.getLocation();
      if (loc != null) {
        s = shortLocation(b.getLocation());
      }
    }

    return s;
  }

  public static Bundle findBundleByHeader(BundleContext bc, String headerName, String headerValue) {
    if(headerName == null) {
      throw new NullPointerException("headerName cannot be null");
    }
    if(headerValue == null) {
      throw new NullPointerException("headerValue cannot be null");
    }
    Bundle[] bl = bc.getBundles();
    for(int i = 0; bl != null && i < bl.length; i++) {
      String v = getHeader(bl[i], headerName);
      if(headerValue.equals(v)) {
        return bl[i];
      }
    }
    return null;
  }

  public static boolean doAutostart() {
    return "true".equals(Util.getProperty("org.knopflerfish.desktop.autostart", "false"));
  }

  public static boolean canBeStarted(Bundle b) {
    return hasActivator(b) || hasMainClass(b) || hasComponent(b);
  }

  public static boolean hasActivator(Bundle b) {
    return null != getHeader(b, "Bundle-Activator");
  }

  public static boolean hasFragment(Bundle b) {
    return null != getHeader(b, "Fragment-Host");
  }

  public static boolean hasComponent(Bundle b) {
    return null != getHeader(b, "Service-Component");
  }

  public static boolean hasMainClass(Bundle b) {
    return null != getHeader(b, "Main-class");
  }

  static public String bundleInfo(Bundle b) {
    StringBuffer sb = new StringBuffer();

    sb.append("<html>");
    sb.append(" Id: "       + b.getBundleId() + "<br>");
    sb.append(" Name: "     + Util.getBundleName(b) + "<br>");
    sb.append(" State: "    + Util.stateName(b.getState()) + "<br>");

    StartLevel sls = (StartLevel)Activator.desktop.slTracker.getService();
    if(sls != null) {
      sb.append(" Start level: ");
      try {
        sb.append(sls.getBundleStartLevel(b));
      } catch (IllegalArgumentException e) {
        sb.append("not managed");
      }
      sb.append("<br>");
    }

    sb.append("</html>");
    return sb.toString();
  }

  // Bundle -> Icon
  static Map iconMap = new HashMap();

  public static Icon getBundleIcon(Bundle b) {
    synchronized(iconMap) {
      Class clazz = Util.class;
      Icon icon = (Icon)iconMap.get(b);
      if(icon != null) {
        return icon;
      }

      URL appURL = null;
      String iconName = (String)b.getHeaders().get("Application-Icon");
      if(iconName == null) {
        iconName = "";
      }
      iconName = iconName.trim();

      if(iconName != null && !"".equals(iconName)) {
        try {
          appURL = b.getResource(iconName);
        } catch (Exception e) {
          Activator.log.error("Failed to load icon", e);
        }
      }

      try {
        if(Util.hasMainClass(b)) {
          icon = new BundleImageIcon(b,
                                     appURL != null ? appURL : clazz.getResource("/jarexec.gif"));
        } else if(Util.hasFragment(b)) {
          icon = new BundleImageIcon(b,
                                     appURL != null ? appURL : clazz.getResource("/frag.gif"));
        } else if(Util.hasComponent(b)) {
          icon = new BundleImageIcon(b,
                                     appURL != null ? appURL : clazz.getResource("/component.png"));
        } else if(Util.hasActivator(b)) {
          icon = new BundleImageIcon(b,
                                     appURL != null ? appURL : clazz.getResource("/bundle.png"));
        } else {
          icon = new BundleImageIcon(b,
                                     appURL != null ? appURL : clazz.getResource("/lib.png"));
        }
      } catch (Exception e) {
        Activator.log.error("Failed to load icon, appURL=" + appURL);
        icon = new BundleImageIcon(b, clazz.getResource("/bundle.png"));
      }
      iconMap.put(b, icon);
      return icon;
    }
  }

  public static Comparator bundleIdComparator = new BundleIdComparator();

  public static class BundleIdComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      Bundle b1 = (Bundle)o1;
      Bundle b2 = (Bundle)o2;

      return (int)(b1.getBundleId() - b2.getBundleId());
    }

    public boolean equals(Object obj) {
      return obj.getClass().equals(BundleIdComparator.class);
    }
  }


// StringBuffer (red.green.blue) -> Color
  static Hashtable colors = new Hashtable();

  static int maxK = 256;

  public static Color rgbInterpolate(Color c1, Color c2, double k) {
    int K = (int)(maxK * k);

    if(c1 == null || c2 == null) {
      return Color.gray;
    }

    if(k <= 0.0) return c1;
    if(k >= 1.0) return c2;

    int r1 = c1.getRed();
    int g1 = c1.getGreen();
    int b1 = c1.getBlue();
    int r2 = c2.getRed();
    int g2 = c2.getGreen();
    int b2 = c2.getBlue();

    int r = (int)(r1 + (double)K * (r2 - r1) / maxK);
    int g = (int)(g1 + (double)K * (g2 - g1) / maxK);
    int b = (int)(b1 + (double)K * (b2 - b1) / maxK);

    Integer key = new Integer((r << 16) | (g << 8) | g);

    Color c = (Color)colors.get(key);
    if(c == null) {
      c = new Color(r, g, b);
      colors.put(key, c);
    }
    return c;
  }

  static Color rgbInterpolate2(Color c1, Color c2, double k) {

    if(c1 == null || c2 == null) {
      return Color.gray;
    }

    if(k == 0.0) return c1;
    if(k == 1.0) return c2;

    int r1 = c1.getRed();
    int g1 = c1.getGreen();
    int b1 = c1.getBlue();
    int r2 = c2.getRed();
    int g2 = c2.getGreen();
    int b2 = c2.getBlue();

    int r = (int)(r1 + (double)(r2 - r1));
    int g = (int)(g1 + (double)(g2 - g1));
    int b = (int)(b1 + (double)(b2 - b1));

    Color c = new Color(r, g, b);
    return c;
  }

    public static byte[] readStream(InputStream is) throws IOException {
      try {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedInputStream   bin = new BufferedInputStream(is);
        byte[] buf = new byte[1024 * 10];
        int len;
        while(-1 != (len = bin.read(buf))) {
          out.write(buf, 0, len);
        }
        return out.toByteArray();
      } finally {
        try { is.close(); } catch (Exception ignored) { }
      }
    }


    /**
     * Get transitive closure of a target bundle
     * by searching for all exporters to the target.
     *
     * @param pkgAdmin   PackageAdmin service used for export search
     * @param allBundles list of all bundles in fw
     * @param target     Target bundle to calculate closure for
     * @param handled    Set of already scanned bundles. Should be
     *                   null or empty set on top level call
     * @return           Set of <tt>Bundle</tt>
     */
    static public Set getPackageClosure(PackageManager pm,
                                        // PackageAdmin pkgAdmin,
                                        // Bundle[]     allBundles,
                                        Bundle       target,
                                        Set          handled) {

      /*
      if(pkgAdmin == null) {
        throw new IllegalArgumentException("pkgAdmin argument cannot be null");
      }
      */

      if(handled == null) {
        handled = new HashSet();
      }

      Set closure = new TreeSet(Util.bundleIdComparator);

      Collection importedPkgs = pm.getImportedPackages(target);

      for(Iterator it = importedPkgs.iterator(); it.hasNext();) {
        ExportedPackage pkg = (ExportedPackage)it.next();

        Bundle exporter = pkg.getExportingBundle();
        closure.add(exporter);

        // Then, get closure from the exporter, if not already
        // handled. Add that closure set to the target closure.
        if(!handled.contains(exporter)) {
          handled.add(exporter);

          // call recursivly with exporter as target
          Set trans = getPackageClosure(pm, exporter, handled);
          closure.addAll(trans);
        }
      }
      /*
      // This is O(n2) at least, possibly O(n3). Should be improved
      for(int i = 0; i < allBundles.length; i++) {
        ExportedPackage[] pkgs = pkgAdmin.getExportedPackages(allBundles[i]);

        for(int j = 0; pkgs != null && j < pkgs.length; j++) {
          Bundle[] bl2 = pkgs[j].getImportingBundles();

          for(int k = 0; bl2 != null && k < bl2.length;  k++) {
            if(bl2[k].getBundleId() == target.getBundleId()) {

              // found an exporter to target - add it to closure
              closure.add(allBundles[i]);

              // Then, get closure from the exporter, if not already
              // handled. Add that closure set to the target closure.
              if(!handled.contains(allBundles[i])) {
                handled.add(allBundles[i]);

                // call recursivley with exporter as target
                Set trans =
                  getPackageClosure(pkgAdmin, allBundles, allBundles[i], handled);
                closure.addAll(trans);
              }
            }
          }
        }
      }
      */
      return closure;
    }

    /**
     * Get transitive closure of bundles a target bundle depends
     * on via services.
     *
     * @param target target bundle to get closure for
     * @param handles set of already handled bundles. Should be null or
     *                empty set on top level call.
     * @return        Set of <tt>Bundle</tt>
     */
    static public Set getServiceClosure(Bundle       target,
                                        Set          handled) {

      if(handled == null) {
        handled = new HashSet();
      }

      Set closure = new TreeSet(Util.bundleIdComparator);

      ServiceReference[] srl = target.getServicesInUse();

      for(int i = 0; srl != null && i < srl.length; i++) {
        Bundle b = srl[i].getBundle();
        closure.add(b);

        if(!handled.contains(b)) {
          handled.add(b);

          Set trans = getServiceClosure(b, handled);
          closure.addAll(trans);
        }
      }

      return closure;
    }

  static String[] STD_PROPS = {
    "org.knopflerfish.verbosity=0",
    "org.knopflerfish.gosg.jars",
    "org.knopflerfish.framework.debug.packages=false",
    "org.knopflerfish.framework.debug.errors=false",
    "org.knopflerfish.framework.debug.classloader=false",
    "org.knopflerfish.framework.debug.startlevel=false",
    "org.knopflerfish.framework.debug.ldap=false",
    "org.osgi.framework.system.packages=",
    "org.knopflerfish.http.dnslookup=false",
    "org.knopflerfish.startlevel.use=true",
    "org.knopflerfish.log.out=false",
    "org.knopflerfish.log.level=info",
  };

  public static StringBuffer getXARGS(Bundle target,
                                      Set pkgClosure,
                                      Set serviceClosure) {

    StringBuffer sb = new StringBuffer();

    String jarBase = Util.getProperty("org.knopflerfish.gosg.jars", "");

    Set all = new TreeSet(Util.bundleIdComparator);
    all.addAll(pkgClosure);
    all.addAll(serviceClosure);

    all.remove(Activator.getTargetBC().getBundle(0));
    if(target != null) {
      all.add(target);
    }
    for(int i = 0; i < STD_PROPS.length; i++) {
      String[] w = Text.splitwords(STD_PROPS[i], "=", '\"');
      String def = null;
      if(w.length == 2) {
        def = w[1];
      }
      String val = Util.getProperty(w[0],null);
      if(null != val && !val.equals(def)) {
        sb.append("-D" + w[0] + "=" + val);
        sb.append("\n");
      }
    }

    StartLevel sl = (StartLevel)Activator.desktop.slTracker.getService();

    int levelMax = -1;

    int n = 0;
    int lastLevel = -1;
    for(Iterator it = all.iterator(); it.hasNext(); ) {
      Bundle b = (Bundle)it.next();
      int level = -1;
      try {
        level = sl.getBundleStartLevel(b);
      } catch (Exception ignored) {
      }

      levelMax = Math.max(level, levelMax);
      if(level != -1 && level != lastLevel) {
        sb.append("-initlevel " + level + "\n");

        lastLevel = level;
      }
      sb.append("-install " +
                Text.replace(b.getLocation(), jarBase, "") +
                "\n");

      n++;
    }

    sb.append("-launch\n");

    n = 0;
    for(Iterator it = all.iterator(); it.hasNext(); ) {
      Bundle b = (Bundle)it.next();
      n++;
      if(b.getState() == Bundle.ACTIVE) {
        sb.append("-start " + n + "\n");
      }
    }

    if(levelMax != -1) {
      sb.append("-startlevel " + levelMax);
    }

    return sb;
  }

  public static final String[] FWPROPS = new String[] {
    Constants.FRAMEWORK_VENDOR,
    Constants.FRAMEWORK_VERSION,
    Constants.FRAMEWORK_LANGUAGE,
    Constants.FRAMEWORK_OS_NAME ,
    Constants.FRAMEWORK_OS_VERSION,
    Constants.FRAMEWORK_PROCESSOR,
    Constants.FRAMEWORK_EXECUTIONENVIRONMENT,
  };

  static public String getSystemInfo() {
    StringBuffer sb = new StringBuffer();
    try {

      Map props = new TreeMap(Activator.getSystemProperties());

      sb.append("<table>\n");

      sb.append(" <tr><td colspan=2 bgcolor=\"#eeeeee\">");
      sb.append(fontify("Framework properties", -1));

      String spid = (String)props.get("org.osgi.provisioning.spid");
      if(spid != null && !"".equals(spid)) {
        sb.append(fontify(" (" + spid + ")", -1));
      }

      sb.append("</td>\n");
      sb.append(" </tr>\n");


      for(int i = 0; i < FWPROPS.length; i++) {
        sb.append(" <tr>\n");
        sb.append("  <td valign=\"top\">");
        sb.append(fontify(FWPROPS[i]));
        sb.append("</td>\n");
        sb.append("  <td valign=\"top\">");
        sb.append(fontify(Activator.getTargetBC().getProperty(FWPROPS[i])));
        sb.append("</td>\n");
        sb.append(" </tr>\n");
      }

      sb.append("<tr><td colspan=2 bgcolor=\"#eeeeee\">");
      sb.append(fontify("System properties", -1));
      sb.append("</td>\n");
      sb.append("</tr>\n");


      for(Iterator it = props.keySet().iterator(); it.hasNext();) {
        String key = (String)it.next();
        String val = (String)props.get(key);
        sb.append(" <tr>\n");
        sb.append("  <td valign=\"top\">");
        sb.append(fontify(key));
        sb.append("</td>\n");
        sb.append("  <td valign=\"top\">");
        sb.append(fontify(val));
        sb.append("</td>\n");
        sb.append("</tr>\n");
      }

    } catch (Exception e) {
      sb.append("<tr><td colspan=2>" +
                fontify("Failed to get system props: " + e) +
                "</td></tr>");

    }
    sb.append("</table>");

    return sb.toString();
  }

  static public String fontify(Object o) {
    return fontify(o, -2);
  }

  public static String fontify(Object o, int size) {
    return "<font size=\"" + size + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">" + o + "</font>";
  }


  static public void printObject(PrintWriter out, Object val) throws IOException {
    if(val == null) {
      out.println("null");
    } else if(val.getClass().isArray()) {
      printArray(out, val);
    } else if(val instanceof Vector) {
      printVector(out, (Vector)val);
    } else if(val instanceof Map) {
      printMap(out, (Map)val);
    } else if(val instanceof Set) {
      printSet(out, (Set)val);
    } else if(val instanceof Dictionary) {
      printDictionary(out, (Dictionary)val);
    } else {
      out.print(Util.fontify(val));
      //      out.print(" (" + val.getClass().getName() + ")");
    }
  }

  static public void printDictionary(PrintWriter out, Dictionary d) throws IOException {

    out.println("<table border=0>");
    for(Enumeration e = d.keys(); e.hasMoreElements();) {
      Object key = e.nextElement();
      Object val = d.get(key);
      out.println("<tr>");

      out.println("<td valign=top>");
      printObject(out, key);
      out.println("</td>");

      out.println("<td valign=top>");
      printObject(out, val);
      out.println("</td>");

      out.println("</tr>");
    }
    out.println("</table>");
  }

  static public void printMap(PrintWriter out, Map m) throws IOException {

    out.println("<table border=0>");
    for(Iterator it = m.keySet().iterator(); it.hasNext();) {
      Object key = it.next();
      Object val = m.get(key);

      out.println("<tr>");

      out.println("<td valign=top>");
      printObject(out, key);
      out.println("</td>");

      out.println("<td valign=top>");
      printObject(out, val);
      out.println("</td>");

      out.println("</tr>");
    }
    out.println("</table>");
  }

  static public void printArray(PrintWriter out, Object a) throws IOException {
    int length = Array.getLength(a);
    for(int i = 0; i < length; i++) {
      printObject(out, Array.get(a,i));
      if(i < length - 1) {
        out.println("<br>");
      }
    }
  }

  static public void printSet(PrintWriter out, Set a) throws IOException {
    for(Iterator it = a.iterator(); it.hasNext();) {
      printObject(out, it.next());
      if(it.hasNext()) {
        out.println("<br>");
      }
    }
  }

  static public void printVector(PrintWriter out, Vector a) throws IOException {
    for(int i = 0; i < a.size(); i++) {
      printObject(out, a.elementAt(i));
      if(i < a.size() - 1) {
        out.println("<br>");
      }
    }
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
      new StreamGobbler(proc.getErrorStream());
      new StreamGobbler(proc.getInputStream());
    } else if (Util.isMacOSX()) {
      // Yes, this only works on Mac OS X
      Runtime rt = Runtime.getRuntime();
      Process proc = rt.exec(new String[] {
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
    String os = Util.getProperty("os.name", null);
    if(os != null) {
      return -1 != os.toLowerCase().indexOf("win");
    }
    return false;
  }

  public static boolean isMacOSX() {
    String os = Util.getProperty("os.name", null);
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

    public void run()
    {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line = "";
      try {
        while (null!=line) {
          line = br.readLine();
        }
      } catch (IOException _ioe) {
      }
    }
  }

  public static String getProperty(String key, String def)
  {
    String sValue = Activator.getBC().getProperty(key);
    if (null!=sValue && 0<sValue.length()) {
      return sValue;
    }
    return def;
  }

  public static int getIntProperty(String key, int def)
  {
    String sValue = Activator.getBC().getProperty(key);
    if (null!=sValue && 0<sValue.length()) {
      try {
        return Integer.parseInt(sValue);
      } catch (Exception _e) {
      }
    }
    return def;
  }


  public static boolean getBooleanProperty(String key, boolean def)
  {
    String sValue = Activator.getBC().getProperty(key);
    if (null!=sValue && 0<sValue.length()) {
      return "true".equals(sValue);
    }
    return def;
  }


  /**
   * Try to get the BundleContext from a Bundle instance using
   * various known backdoors (we don't really rely on R4.1 yet)
   */
  public static BundleContext getBundleContext(Bundle b) {
    Class clazz = b.getClass();
    try {
      // getBundleContext() is an R4.1 method, but try to grab it
      // using reflection and punch a hole in the method modifiers.
      // Should work on recent KF and recent Felix.
      Method m =  clazz.getMethod("getBundleContext", new Class[] { });

      m.setAccessible(true);
      return (BundleContext)m.invoke(b, new Object[] { });
    } catch (Exception e) {
      Activator.log.debug("Failed to call Bundle.getBundleContext()", e);

      // Try some known private fields.
      String[] fieldNames = new String[] {
        "bundleContext", // available in KF
        "context",       // available in Equinox and Concierge
      };
      for(int i = 0; i < fieldNames.length; i++) {
        try {
          Activator.log.debug("Try field " + clazz.getName() + "." + fieldNames[i]);

          Field field = clazz.getDeclaredField(fieldNames[i]);
          field.setAccessible(true);
          return (BundleContext)field.get(b);
        } catch (Exception e2) {
          Activator.log.info("Failed: field " + clazz.getName() + "." + fieldNames[i], e2);
        }
      }
    }
    Activator.log.warn("Failed to get BundleContext from " + clazz.getName());
    return null;
  }

  public static String getServiceInfo(ServiceReference sr) {
    StringBuffer sb = new StringBuffer();

    sb.append(sr.getProperty("service.id") + ": " + getClassNames(sr));
    sb.append("\n");
    sb.append("from #" + sr.getBundle().getBundleId());
    sb.append(" " + Util.getBundleName(sr.getBundle()));



    Bundle[] bl = sr.getUsingBundles();
    if(bl != null) {
      sb.append("\nto ");
      for(int i = 0; i < bl.length; i++) {
        sb.append("#" + bl[i].getBundleId());
        sb.append(" " + Util.getBundleName(bl[i]));
        if(i < bl.length -1) {
          sb.append("\n");
        }
      }
    }
    return sb.toString();
  }


  public static String getClassNames(ServiceReference sr) {
    return getClassNames(sr, "\n");
  }

  public static String getClassNames(ServiceReference sr, String sep) {

    StringBuffer sb = new StringBuffer();
    String sa[] = (String[])sr.getProperty("objectClass");
    for(int j = 0; j < sa.length; j++) {
      sb.append(sa[j]);
      if(j < sa.length - 1) {
        sb.append(sep);
      }
    }
    return sb.toString();
  }

  static public void setAntialias(Graphics g, boolean b) {
    Graphics2D g2 = (Graphics2D)g;
    if(b) {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
    } else {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_OFF);
    }
  }

}
