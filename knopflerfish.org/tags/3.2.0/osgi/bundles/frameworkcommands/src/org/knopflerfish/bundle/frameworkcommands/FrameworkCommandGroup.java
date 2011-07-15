/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

package org.knopflerfish.bundle.frameworkcommands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.startlevel.StartLevel;

// ******************** FrameworkCommandGroup ********************

/**
 * Console commands for interaction with the framework.
 *
 * @author Gatespace AB
 */

public class FrameworkCommandGroup
  extends CommandGroupAdapter
{
  final BundleContext bc;

  private final PackageAdmin packageAdmin;
  private final PermissionAdmin permissionAdmin;
  private final ConditionalPermissionAdmin condPermAdmin;
  private final StartLevel startLevel;

  /**
   * The default directories for bundle jar files.
   *
   * <p>
   *
   * The framework property <code>org.knopflerfish.gosg.jars</code>
   * holds a semicolon separated path of URLs that is used to complete
   * the location when it is given as a partial URL.
   *
   * </p>
   */
  private List/*<URL>*/ baseURLs = new ArrayList();


  FrameworkCommandGroup(BundleContext bc) {
    super("framework", "Framework commands");
    this.bc = bc;

    // all of these services are framework singleton internal services
    // thus, we take a shortcut and skip the service tracking

    ServiceReference sr = bc.getServiceReference(PackageAdmin.class
                                                 .getName());
    packageAdmin = null==sr ? null : (PackageAdmin) bc.getService(sr);

    sr = bc.getServiceReference(PermissionAdmin.class.getName());
    permissionAdmin = null==sr ? null : (PermissionAdmin) bc.getService(sr);

    sr = bc.getServiceReference(ConditionalPermissionAdmin.class.getName());
    condPermAdmin = null==sr ? null
      : (ConditionalPermissionAdmin) bc.getService(sr);

    sr = bc.getServiceReference(StartLevel.class.getName());
    startLevel = null==sr ? null : (StartLevel) bc.getService(sr);

    try {
      setupJars();
    } catch (MalformedURLException mfe) {
    }
  }

  void setupJars()
    throws MalformedURLException
  {
    String jars = bc.getProperty("org.knopflerfish.gosg.jars");
    jars = null==jars || jars.length()==0 ? "file:jars/" : jars;

    final StringTokenizer st = new StringTokenizer(jars, ";");
    final String[] prefixes = new String[st.countTokens()];

    for (int i=0; st.hasMoreTokens(); i++) {
      prefixes[i] = st.nextToken();
    }

    setupJars(prefixes, false);
  }

  void setupJars(final String[] prefixes, boolean append)
    throws MalformedURLException
  {
    MalformedURLException firstMFE = null;
    if (!append) {
      baseURLs.clear();
    }

    for (int i=0; i<prefixes.length; i++) {
      try {
        baseURLs.add(new URL(prefixes[i]));
      } catch (MalformedURLException mfe) {
        if (null==firstMFE){
          firstMFE = mfe;
        }
      }
    }
    if (null!=firstMFE) {
      throw firstMFE;
    }
  }

  /**
   * Completes a partial bundle location using the bundles dir path.
   * The result is the first combination of a directory URL (as
   * returned by <code>getBundleDirs()</code>) and the specified
   * location that results in a valid URL with accessible data.
   *
   * @param location the bundle location to complete
   */
  public String completeLocation(String location) {
    final int ic = location.indexOf(":");
    if (ic < 2 || ic > location.indexOf("/")) {
      // URL wihtout protocol complete it.
      for (Iterator it=baseURLs.iterator(); it.hasNext(); ) {
        final URL baseURL = (URL) it.next();

        try {
          final URL url = new URL(baseURL, location);
          if ("file".equals(url.getProtocol())) {
            final File f = new File(url.getFile());
            if (!f.exists() || !f.canRead()) {
              continue; // Noope; try next.
            }
          } else if ("http".equals(url.getProtocol())) {
            final HttpURLConnection uc = (HttpURLConnection)
              url.openConnection();
            uc.connect();
            final int rc = uc.getResponseCode();
            uc.disconnect();
            if (rc != HttpURLConnection.HTTP_OK) {
              continue; // Noope; try next.
            }
          } else {
            // Generic case; Check if we can read data from this URL
            InputStream is = null;
            try {
              is = url.openStream();
            } finally {
              if (is != null)
                is.close();
            }
          }
          location = url.toString();
          break; // Found.
        } catch (Exception _e) {
        }
      }
    }
    return location;
  }

  //
  // Addpermission command
  //

  public final static String USAGE_ADDPERMISSION
    = "-b #bundle# | -d | -l #location# <type> [<name> [<actions>]]";

  public final static String[] HELP_ADDPERMISSION = new String[] {
    "Add permissions to bundle",
    "-d            Add default permissions",
    "-b #bundle#   Add permission for bundle name or id",
    "-l #location# Add permission for location",
    "<type>        Permission type", "<name>        Permission name",
    "<actions>     Permission actions" };

  public int cmdAddpermission(Dictionary opts, Reader in, PrintWriter out,
                              Session session) {
    if (permissionAdmin == null) {
      out.println("Permission Admin service is not available");
      return 1;
    }
    String loc = null;
    PermissionInfo[] pi;
    String selection = (String) opts.get("-b");
    if (selection != null) {
      Bundle[] b = bc.getBundles();
      Util.selectBundles(b, new String[] { selection });
      for (int i = 0; i < b.length; i++) {
        if (b[i] != null) {
          if (loc == null) {
            loc = b[i].getLocation();
          } else {
            out.println("ERROR! Multiple bundles selected");
            return 1;
          }
        }
      }
      if (loc == null) {
        out.println("ERROR! No matching bundle");
        return 1;
      }
      pi = permissionAdmin.getPermissions(loc);
    } else if (opts.get("-d") != null) {
      pi = permissionAdmin.getDefaultPermissions();
    } else {
      loc = (String) opts.get("-l");
      pi = permissionAdmin.getPermissions(loc);
    }
    PermissionInfo pia;
    try {
      pia = new PermissionInfo((String)opts.get("type"),
                               (String)opts.get("name"),
                               (String)opts.get("actions"));
    } catch (IllegalArgumentException e) {
      out.println("ERROR! " + e.getMessage());
      out.println("PermissionInfo type = " + opts.get("type"));
      out.println("PermissionInfo name = " + opts.get("name"));
      out.println("PermissionInfo actions = " + opts.get("actions"));
      return 1;
    }
    if (pi != null) {
      PermissionInfo[] npi = new PermissionInfo[pi.length + 1];
      System.arraycopy(pi, 0, npi, 0, pi.length);
      pi = npi;
    } else {
      pi = new PermissionInfo[1];
    }
    pi[pi.length - 1] = pia;
    if (loc != null) {
      permissionAdmin.setPermissions(loc, pi);
    } else {
      permissionAdmin.setDefaultPermissions(pi);
    }
    return 0;
  }

  //
  // Bundles command
  //

  public final static String USAGE_BUNDLES
    = "[-1] [-i] [-l] [-s] [-t] [<bundle>] ...";

  public final static String[] HELP_BUNDLES = new String[] {
    "List bundles",
    "-1       One column output",
    "-i       Sort on bundle id",
    "-s       Sort on bundle start level",
    "-t       Sort on last modified time",
    "-l       Verbose output",
    "<bundle> Name or id of bundle"
  };

  public int cmdBundles(Dictionary opts, Reader in, PrintWriter out,
                        Session session) {
    Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null,
                            opts.get("-s") != null,
                            opts.get("-t") != null);
    boolean verbose = (opts.get("-l") != null);
    boolean oneColumn = (opts.get("-1") != null);

    Vector tmp = new Vector();
    for (int i = 0; i < b.length; i++) {
      if (b[i] != null) {
        tmp.add(b[i]);
      }
    }

    if (tmp.size() == 0) {
      out.println("ERROR! No matching bundle");
      return 1;
    } else {
      printBundles(out, (Bundle[])tmp.toArray(new Bundle[tmp.size()]), verbose, oneColumn);
      return 0;
    }
  }

  private void printBundles(PrintWriter out, Bundle[] b, boolean verbose, boolean oneColumn) {
    boolean needNl = false;
    // .println("12 5/active CM Commands 2 1/active CM Service");
    String[] lastModified = null;
    if (verbose) {
      lastModified = new String[b.length];
      int longestLM = 0;
      SimpleDateFormat dateFormat = new SimpleDateFormat();
      for (int i = 0; i < b.length; i++) { // Or just look at the first one...
        lastModified[i] = dateFormat.format(new Date(b[i].getLastModified()));
        if (lastModified[i].length() > longestLM) {
          longestLM = lastModified[i].length();
        }
      }
      String lmHeader = "modified";
      if (longestLM > lmHeader.length()) {
        String blank = "                                    ";
        lmHeader += blank.substring(blank.length() - (longestLM - lmHeader.length()));
      }
      out.println("   id  level/state  " + lmHeader + "  location");
      out.println("   ----------------------------------------------");
    } else {
      out.println("   id  level/state name");
      out.println("   --------------------");
    }
    for (int i = 0; i < b.length; i++) {
      String level = null;
      try {
        level = "" + startLevel.getBundleStartLevel(b[i]);
        if (level.length() < 2) {
          level = " " + level;
        }
      } catch (Exception e) {
        // no start level set.
      }

      if (b[i] == null) {
        break;
      }
      if (verbose) {
        out.println(Util.showId(b[i]) + showState(b[i])
                    + " " + lastModified[i]
                    + "  " + b[i].getLocation()
                    + getBundleSpeciality(b[i]));
      } else {
        String s = Util.showId(b[i]) + showState(b[i])
          + Util.shortName(b[i]) + getBundleSpeciality(b[i]);
        if ((i & 1) == 0 && !oneColumn) {
          out.print(s);
          int l = 40 - s.length();
          if (l > 0) {
            String blank = "                                    ";
            out.print(blank.substring(blank.length() - l));
          }
          needNl = true;
        } else {
          out.println(s);
          needNl = false;
        }
      }
    }
    if (needNl) {
      out.println("");
    }
  }

  private String getBundleSpeciality(Bundle bundle) {
    if (packageAdmin == null) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    Bundle[] fragments = packageAdmin.getFragments(bundle);
    if (fragments != null && fragments.length > 0) {
      sb.append("h:"); // host
      for (int i=0; i<fragments.length;i++){
        if (i>0) sb.append(",");
        sb.append(fragments[i].getBundleId());
      }
    }
    Bundle[] hosts = packageAdmin.getHosts(bundle);
    if (hosts != null && hosts.length > 0) {
      sb.append("f:"); // fragment
      for (int i=0; i<hosts.length;i++){
        if (i>0) sb.append(",");
        sb.append(hosts[i].getBundleId());
      }
    }
    return sb.length()>0 ? (" (" +sb.toString() + ")" ) : "";
  }


  //
  // Call command
  //

  public final static String USAGE_CALL
    = "[-f #filter#] <interface> <method> [<args>] ...";

  public final static String[] HELP_CALL = new String[] {
    "Call a method with zero or more java.lang.String",
    "arguments in a registered service.",
    "-f #filter# Filter to select service, the condition",
    "            '(objectClass=<interface>) is allways added to the filter",
    "<interface> Service interface",
    "<method>    Method in service to call",
    "<args>      Arguments to method. If arguments",
    "            are on the form \"value::type\", the value",
    "            will be attempted to created as the",
    "            specified type", };

  public int cmdCall(final Dictionary opts,
                     final Reader in,
                     final PrintWriter out,
                     final Session session)
  {
    int res = 1;
    final String si = (String) opts.get("interface");
    final String filterArg = (String) opts.get("-f");
    final String filter = null==filterArg
      ? null
      : "(&(objectClass=" +si +")" +filterArg +")";
    final ServiceReference sr = (ServiceReference) AccessController
      .doPrivileged(new PrivilegedAction() {
          public Object run() {
            ServiceReference res = null;
            if (null==filter) {
              res = bc.getServiceReference(si);
              if (null==res) {
                out.println("No service with interface '" + si +"'.");
              }
            } else {
              try {
                ServiceReference[] srs = bc.getServiceReferences(si,filter);
                if (null==srs) {
                  out.println("No service that matches the filter '"
                              +filter +"'.");
                } else if (1==srs.length) {
                  res = srs[0];
                } else {
                  out.println("Multiple service matches the filter '"
                              +filter +"' please narrow it down.");
                }
              } catch (InvalidSyntaxException ise) {
                out.println("Invalid filter '" +filter +"': "
                            +ise.getMessage());
              }
            }
            return res;
          }
        });
    if (sr == null) {
      return 1;
    }
    Object s = AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return bc.getService(sr);
        }
      });
    if (s == null) {
      out.println("No such service: " + si);
      return 1;
    }

    String method = (String) opts.get("method");
    Class[] parameterTypes = null;
    Object[] methodArgs = null;
    String[] args = (String[]) opts.get("args");
    if (args == null) {
      args = new String[0];
    }

    methodArgs = new Object[args.length];

    try {
      Method m = findMethod(si, s.getClass(), method, args.length);

      if (m != null) {
        parameterTypes = m.getParameterTypes();
      } else {
        parameterTypes = new Class[args.length];
      }
      for (int i = 0; i < args.length; i++) {
        String val = args[i];
        String className = String.class.getName();
        int ix = val.indexOf("::");
        if (ix != -1) {
          className = val.substring(ix + 2);
          val = val.substring(0, ix);
        }
        if (m == null) {
          methodArgs[i] = makeObject(val, className);
          parameterTypes[i] = getClass(className);
        } else {
          methodArgs[i] = makeObject(val, parameterTypes[i].getName());
        }
      }

      if (m == null) {
        m = s.getClass().getMethod(method, parameterTypes);
      }

      out.println("Result: " + Util.showObject(m.invoke(s, methodArgs)));
      res = 0;
    } catch (InvocationTargetException e) {
      out.println("Exception thrown by call");
      e.getTargetException().printStackTrace(out);
    } catch (IllegalAccessException e) {
      out.println("Call method not accessible (must be public)");
    } catch (NullPointerException e) {
      out.println("Internal error: " + e);
    } catch (IllegalArgumentException e) {
      out.println("Internal error: " + e);
    } catch (NoSuchMethodException e) {
      out.println("No method '" + method + "' with matching arguments: "
                  + e);
    }

    bc.ungetService(sr);
    return res;
  }

  Class getClass(String className) {
    try {
      if ("int".equals(className)) {
        return Integer.TYPE;
      } else if ("boolean".equals(className)) {
        return Boolean.TYPE;
      } else if ("long".equals(className)) {
        return Long.TYPE;
      } else if ("string".equals(className)) {
        return String.class;
      }

      if (-1 == className.indexOf(".")) {
        className = "java.lang." + className;
      }
      return Class.forName(className);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unknown class " + className);
    }
  }

  /**
   * Find the class object for the named interface/class that the
   * specified class implements.
   * @param type  The full name of the class / interface that we are
   *              looking for.
   * @param clazz The class to investigate.
   * @return class object for the specified type.
   */
  Class findClass(final String type, final Class clazz)
  {
    if (type.equals(clazz.getName())) return clazz;

    Class[] clazzes = clazz.getInterfaces();
    for (int i=0; i<clazzes.length; i++) {
      if (type.equals(clazzes[i].getName())) return clazzes[i];
    }
    return findClass(type, clazz.getSuperclass());
  }

  Method findMethod(final String si,
                    final Class clazz,
                    final String name,
                    final int nArgs)
  {
    Class ifClass = findClass(si, clazz);
    // Fallback to use the original class if the one given by si was not found.
    if (null==ifClass) ifClass = clazz;

    Method[] methods = ifClass.getDeclaredMethods();
    Vector v = new Vector();
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getName().equals(name)
          && methods[i].getParameterTypes().length == nArgs) {
        v.addElement(methods[i]);
      }
    }
    if (v.size() == 1) {
      return (Method) v.elementAt(0);
    }
    return null;
  }

  Object makeObject(String val, String className) {
    try {
      Class clazz = getClass(className);

      if (clazz == Integer.TYPE) {
        return new Integer(val);
      }

      if (clazz == Long.TYPE) {
        return new Long(val);
      }

      if (clazz == Boolean.TYPE) {
        return "true".equals(val) ? Boolean.TRUE : Boolean.FALSE;
      }

      if (clazz == String.class) {
        return val;
      }

      Constructor cons = clazz
        .getConstructor(new Class[] { String.class });
      Object r = cons.newInstance(new Object[] { val });
      return r;
    } catch (Exception e) {
      throw new IllegalArgumentException("makeObject(" + val + ", "
                                         + className + "): " + e);
    }
  }

  //
  // Certificates command
  //

  public final static String USAGE_CERTIFICATES
    = "[ -i ] <bundle> ...";

  public final static String[] HELP_CERTIFICATES = new String[] {
    "List certificates for bundles",
    "-i           Sort on bundle id",
    "<bundle>     Name or id of bundle" };

  public int cmdCertificates(Dictionary opts, Reader in, PrintWriter out,
                              Session session) {
    Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null);
    boolean match = false;
    for (int i = 0; i < b.length; i++) {
      if (b[i] != null) {
        try {
          boolean found = false;
          Method m = b[i].getClass().getMethod("getCertificates", null);
          Certificate [] cs = (Certificate [])m.invoke(b[i], null);
          out.println("Bundle: " + showBundle(b[i]));
          if (cs != null) {
            for (int j = 0; j < cs.length; j++) {
              out.println("Certificate " + j + ":");
              out.println(cs[j].toString());
              found = true;
            }
          }
          if (!found) {
            out.println("  Not a signed bundle.");
          }
        } catch (Exception e) {
          out.println("This command only works on a Knopflefish framework");
          return 1;
        }
        match = true;
      }
    }
    if (!match) {
      out.println("ERROR! No matching bundle");
      return 1;
    }
    return 0;
  }


  //
  // Closure command
  //

  public final static String USAGE_CLOSURE = "<bundle>";

  public final static String[] HELP_CLOSURE = new String[] {
    "Display the closure for a bundle",
    "<bundle> - Name or id of bundle" };

  public int cmdClosure(Dictionary opts, Reader in, PrintWriter out,
                        Session session) {

    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }

    String bname = (String) opts.get("bundle");
    Bundle[] bl = getBundles(new String[] { bname }, true);
    Bundle bundle = bl[0];
    if (bundle == null) {
      out.println("ERROR! No matching bundle for '" + bname + "'");
      return 1;
    }

    bl = getBundles(null, false, false, false);

    // Package

    Vector pkgClosure = new Vector();
    // This is O(n2) at least, possibly O(n3). Should be improved
    for(int b = 0; b < bl.length; b++) {
      ExportedPackage[] pkgs = packageAdmin.getExportedPackages(bl[b]);
      if (pkgs == null) continue;
      for(int p = 0; p < pkgs.length; p++) {
        Bundle[] bl2 = pkgs[p].getImportingBundles();
        if (bl2 == null) continue;
        for(int ib = 0; ib < bl2.length;  ib++) {
          if(bl2[ib].getBundleId() == bundle.getBundleId() && !pkgClosure.contains(bl[b])) {
            pkgClosure.add(bl[b]);
          }
        }
      }
    }
    pkgClosure.remove(bundle);
    if (pkgClosure.size() == 0) {
      out.println("No package dependencies");
    } else {
      out.println("Static dependencies via packages:");
      Bundle[] bundles = (Bundle[]) pkgClosure.toArray(new Bundle[pkgClosure.size()]);
      printBundles(out, bundles, false, true);
    }

    // Service

    Vector serviceClosure = new Vector();
    ServiceReference[] srl = bundle.getServicesInUse();
    for (int i = 0; srl != null && i < srl.length; i++) {
      if (!serviceClosure.contains(srl[i].getBundle())) {
        serviceClosure.add(srl[i].getBundle());
      }
    }
    serviceClosure.remove(bundle);
    if(serviceClosure.size() == 0) {
      out.println("No service dependencies");
    } else {
      out.println("Runtime dependencies via services:");
      Bundle[] bundles = (Bundle[]) serviceClosure.toArray(new Bundle[serviceClosure.size()]);
      printBundles(out, bundles, false, true);
    }

    // Fragment

    Bundle[] fragmentBundles = packageAdmin.getFragments(bundle);
    if (fragmentBundles == null) {
      out.println("No fragments");
    } else {
      out.println("Fragments:");
      printBundles(out, fragmentBundles, false, true);
    }

    // Host

    Bundle[] hostBundles = packageAdmin.getHosts(bundle);
    if (hostBundles == null) {
      out.println("No hosts");
    } else {
      out.println("Hosts:");
      printBundles(out, hostBundles, false, true);
    }

    // Required

    Vector required = new Vector();
    Vector requiredBy = new Vector();

    try { // untested code
      RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(null);
      if (requiredBundles != null) {
        for (int reqd = 0; reqd < requiredBundles.length; reqd++) {
          Bundle[] requiringBundles = requiredBundles[reqd].getRequiringBundles();
          if (requiringBundles == null) continue;
          if (requiredBundles[reqd].getBundle().equals(bundle)) {
            for (int ring = 0; ring < requiringBundles.length; ring++) {
              requiredBy.add(requiringBundles[ring]);
            }
          } else {
            for (int ring = 0; ring < requiringBundles.length; ring++) {
              if (requiringBundles[ring].equals(bundle)) {
                required.add(requiredBundles[reqd].getBundle());
              }
            }
          }
        }
      }
    } catch (Throwable ignored) {}
    if (required.size() == 0) {
      out.println("No required bundles");
    } else {
      out.println("Required bundles:");
      Bundle[] bundles = (Bundle[]) required.toArray(new Bundle[required.size()]);
      printBundles(out, bundles, false, true);
    }
    if (requiredBy.size() == 0) {
      out.println("No requiring bundles");
    } else {
      out.println("Requiring bundles:");
      Bundle[] bundles = (Bundle[]) requiredBy.toArray(new Bundle[requiredBy.size()]);
      printBundles(out, bundles, false, true);
    }

    return 0;
  }


  //
  // CondPermission command
  //

  public final static String USAGE_CONDPERMISSION
    = "[<name>] ...";

  public final static String[] HELP_CONDPERMISSION = new String[] {
    "Get conditional permissions",
    "<name>               Name of conditional permission" };

  public int cmdCondpermission(Dictionary opts, Reader in, PrintWriter out,
                                  Session session) {
    if (condPermAdmin == null) {
      out.println("Conditional Permission Admin service is not available");
      return 1;
    }
    String [] names = (String []) opts.get("name");
    Enumeration e;
    if (names != null) {
      Vector cpis = new Vector();
      for (int i = 0; i < names.length; i++ ) {
        ConditionalPermissionInfo cpi = condPermAdmin.getConditionalPermissionInfo(names[i]);
        if (cpi != null) {
          cpis.addElement(cpi);
        } else {
          out.println("Didn't find ConditionalPermissionInfo named: " + names[i]);
        }
      }
      e = cpis.elements();
    } else {
      e = condPermAdmin.getConditionalPermissionInfos();
    }
    while (e.hasMoreElements()) {
      // NYI! pretty print
      out.println(e.nextElement().toString());
    }
    return 0;
  }


  //
  // Deletepermission command
  //

  public final static String USAGE_DELETEPERMISSION
    = "[-r] -b #bundle# | -d | -l #location# <type> <name> <actions>";

  public final static String[] HELP_DELETEPERMISSION = new String[] {
    "Delete permissions from a bundle",
    "-b #bundle#   Delete permission for bundle name or id",
    "-d            Delete default permissions",
    "-l #location# Delete permission for location",
    "-r            Remove entry if empty",
    "<type>        Permission type (*, match all)",
    "<name>        Permission name (*, match all)",
    "<actions>     Permission actions (*, match all)" };

  public int cmdDeletepermission(Dictionary opts, Reader in, PrintWriter out,
                                 Session session) {
    if (permissionAdmin == null) {
      out.println("Permission Admin service is not available");
      return 1;
    }
    String loc = null;
    PermissionInfo[] pi;
    String selection = (String) opts.get("-b");
    if (selection != null) {
      Bundle[] b = bc.getBundles();
      Util.selectBundles(b, new String[] { selection });
      for (int i = 0; i < b.length; i++) {
        if (b[i] != null) {
          if (loc == null) {
            loc = b[i].getLocation();
          } else {
            out.println("ERROR! Multiple bundles selected");
            return 1;
          }
        }
      }
      if (loc == null) {
        out.println("ERROR! No matching bundle");
        return 1;
      }
      pi = permissionAdmin.getPermissions(loc);
    } else if (opts.get("-d") != null) {
      pi = permissionAdmin.getDefaultPermissions();
    } else {
      loc = (String) opts.get("-l");
      pi = permissionAdmin.getPermissions(loc);
    }
    if (pi != null) {
      String type = (String) opts.get("type");
      String name = (String) opts.get("name");
      String actions = (String) opts.get("actions");
      int size = 0;
      for (int i = 0; i < pi.length; i++) {
        if (("*".equals(type) || pi[i].getType().equals(type))
            && ("*".equals(name) || pi[i].getName().equals(name))
            && ("*".equals(actions) || pi[i].getActions().equals(
                                                                 actions))) {
          pi[i] = null;
        } else {
          size++;
        }
      }
      if (size == 0) {
        if (opts.get("-r") != null) {
          pi = null;
        } else {
          pi = new PermissionInfo[0];
        }
      } else {
        PermissionInfo[] npi = new PermissionInfo[size];
        for (int i = pi.length - 1; i >= 0; i--) {
          if (pi[i] != null) {
            npi[--size] = pi[i];
          }
        }
        pi = npi;
      }
      if (loc != null) {
        permissionAdmin.setPermissions(loc, pi);
      } else {
        permissionAdmin.setDefaultPermissions(pi);
      }
    }
    return 0;
  }

  //
  // Findbundles command
  //

  public final static String USAGE_FINDBUNDLES = "<symbolic name>";

  public final static String[] HELP_FINDBUNDLES = new String[] {
    "Find bundles with a given symbolic name",
    "<symbolic name>  Symbolic name" };

  public int cmdFindbundles(Dictionary opts, Reader in, PrintWriter out,
                            Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    String symbolicName = (String) opts.get("symbolic name");
    Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
    if (bundles == null) {
      out.println("No bundles found.");
    } else {
      printBundles(out, bundles, true, true);
    }
    return 0;
  }

  //
  // Headers command
  //

  public final static String USAGE_HEADERS = "[-i] [-l #locale#] <bundle> ...";

  public final static String[] HELP_HEADERS = new String[] {
    "Show bundle header values",
    "-i           Sort on bundle id",
    "-l #locale#  Get localized headers for a given locale",
    "<bundle>     Name or id of bundle" };

  public int cmdHeaders(Dictionary opts, Reader in, PrintWriter out,
                        Session session) {
    Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null);
    String locale = (String) opts.get("-l");
    boolean found = false;
    for (int i = 0; i < b.length; i++) {
      if (b[i] != null) {
        out.println("Bundle: " + showBundle(b[i]));
        Dictionary d = (locale == null ? b[i].getHeaders()
                        : b[i].getHeaders(locale));
        for (Enumeration e = d.keys(); e.hasMoreElements();) {
          String key = (String) e.nextElement();
          out.println("  " + key + " = "
                      + Util.showObject(d.get(key)));
        }
        found = true;
      }
    }
    if (!found) {
      out.println("ERROR! No matching bundle");
      return 1;
    }
    return 0;
  }

  //
  // Install command
  //

  public final static String USAGE_INSTALL = "[-s] <location> ...";
  public final static String[] HELP_INSTALL = new String[] {
    "Install one or more bundles",
    "-s         Persistently start bundle(s) according to activation policy",
    "<location> Location of bundle archive (URL).",
    "Note: The base URLs used to complete partial URLs may be set using the cd command"
  };

  public int cmdInstall(Dictionary opts, Reader in, PrintWriter out,
                        Session session) {
    String[] loc = (String[]) opts.get("location");
    String url = null;
    try {
      for (int i = 0; i < loc.length; i++) {
        url = completeLocation(loc[i]);
        Bundle b = bc.installBundle(url);
        out.println("Installed: " + showBundle(b));
        if (opts.get("-s") != null) {
          b.start(Bundle.START_ACTIVATION_POLICY);
          out.println("Started: " + showBundle(b));
        }
      }
    } catch (BundleException e) {
      Throwable t = e;
      while (t instanceof BundleException
             && ((BundleException) t).getNestedException() != null)
        t = ((BundleException) t).getNestedException();
      if (t instanceof FileNotFoundException) {
        out.println("Couldn't install/start bundle: URL not found: "
                    + url);
      } else {
        out.println("Couldn't install/start bundle: " + url
                    + " (due to: " + t + ")");
        t.printStackTrace(out);
      }
      return 1;
    }
    return 0;
  }

  
  //
  // Meminfo command
  //
  public final static String USAGE_MEMINFO = "[-gc] [-b | -m]";

  public final static String[] HELP_MEMINFO = new String[] {
    "Display java memory information, in kilobytes",
    "-gc  Run garbage collector first",
    "-b   Display using bytes",
    "-m   Display using megabytes" };

  public int cmdMeminfo(Dictionary opts, Reader in, PrintWriter out,
                         Session session) {
    if (opts.get("-gc") != null) {
      System.gc();
    }
    
    int d = 1024;
    String unit = "kB";
    if (opts.get("-b") != null) {
      d = 1;
      unit = "bytes";
    } else if (opts.get("-m") != null) {
      d = d * 1024;
      unit = "MB";
    }
    Runtime r = Runtime.getRuntime();
    out.println("Total: " + (r.totalMemory() + d/2) / d
        + "  Free: " + (r.freeMemory() + d/2) / d
        + "  Max: " + (r.maxMemory() + d/2) / d + "  (" + unit + ")");
    return 0;
  }

  
  //
  // Package command
  //

  public final static String USAGE_PACKAGE = "[-l] -b | -p [<selection>] ...";

  public final static String[] HELP_PACKAGE = new String[] {
    "Show java package information",
    "If no package or bundle is specified show all packages",
    "-l         Verbose output",
    "-b         Only look at selected bundles",
    "-p         Only look at selected packages",
    "<selection>  Package or bundle" };

  public int cmdPackage(Dictionary opts, Reader in, PrintWriter out,
                        Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    boolean verbose = opts.get("-l") != null;
    ExportedPackage[] epkgs;
    String[] selection = (String[]) opts.get("selection");
    if (opts.get("-b") != null) {
      Bundle[] b = getBundles(selection, false);
      epkgs = new ExportedPackage[0];
      for (int i = 0; i < b.length; i++) {
        if (b[i] != null) {
          ExportedPackage[] e = packageAdmin
            .getExportedPackages(b[i]);
          if (e != null) {
            if (verbose) {
              ExportedPackage[] ne = new ExportedPackage[e.length
                                                         + epkgs.length];
              System.arraycopy(epkgs, 0, ne, 0, epkgs.length);
              System.arraycopy(e, 0, ne, epkgs.length, e.length);
              epkgs = ne;
            } else {
              out.println("Exported by " + showBundle(b[i]));
              out.println("   Package: " + e[0].getName());
              for (int j = 1; j < e.length; j++) {
                out.println("            " + e[j].getName());
              }
            }
          }
        }
      }
      if (!verbose) {
        return 0;
      }
    } else {
      if (selection != null) {
        epkgs = new ExportedPackage[selection.length];
        for (int i = 0; i < selection.length; i++) {
          epkgs[i] = packageAdmin.getExportedPackage(selection[i]);
        }
      } else {
        epkgs = packageAdmin.getExportedPackages((Bundle)null);
        // TODO: We should sort here
      }
    }
    for (int i = 0; i < epkgs.length; i++) {
      if (epkgs[i] != null) {
        out.print("Package: " + epkgs[i].getName());
        Bundle b = epkgs[i].getExportingBundle();
        if (verbose) {
          out.println();
          out.println("   specification version: "
                      + epkgs[i].getVersion());
          out.println("   removal pending: "
                      + epkgs[i].isRemovalPending());
          out.println("   exporting bundle: " + showBundle(b));
          Bundle[] ib = epkgs[i].getImportingBundles();
          if (ib != null && ib.length > 0) {
            out.println("   importing bundle: "
                        + showBundle(ib[0]));
            for (int j = 1; j < ib.length; j++) {
              out.println("                     "
                          + showBundle(ib[j]));
            }
          }
          out.println();
        } else {
          out.println(" exported by " + showBundle(b));
        }
      } else {
        if (verbose) {
          out.println("Package not found: " + selection[i]);
          out.println();
        }
      }
    }
    return 0;
  }

  //
  // Permissions command
  //

  public final static String USAGE_PERMISSIONS = "[-d] [<selection>] ...";

  public final static String[] HELP_PERMISSIONS = new String[] {
    "Show permission information",
    "If no parameters is given show all entries",
    "-d           Show default permissions",
    "<selection>  Name or id of bundle or an unknown location" };

  public int cmdPermissions(Dictionary opts, Reader in, PrintWriter out,
                            Session session) {
    if (permissionAdmin == null) {
      out.println("Permission Admin service is not available");
      return 1;
    }
    String[] loclist = permissionAdmin.getLocations();
    String[] selection = (String[]) opts.get("selection");
    if (loclist != null && selection != null) {
      Bundle[] b = bc.getBundles();
      Util.selectBundles(b, selection);
      lloop: for (int i = 0; i < loclist.length; i++) {
        for (int j = 0; j < selection.length; j++) {
          if (loclist[i].equals(selection[j])) {
            continue lloop;
          }
        }
        for (int j = 0; j < b.length; j++) {
          if (b[j] != null && loclist[i].equals(b[j].getLocation())) {
            continue lloop;
          }
        }
        loclist[i] = null;
      }
    }

    if (opts.get("-d") != null) {
      out.println("Default permissions");
      showPerms(out, permissionAdmin.getDefaultPermissions());
    }

    if (loclist != null) {
      Bundle[] b = bc.getBundles();
      for (int i = 0; i < loclist.length; i++) {
        if (loclist[i] != null) {
          int j = b.length;
          while (--j >= 0) {
            if (loclist[i].equals(b[j].getLocation())) {
              break;
            }
          }
          out.println("Location: "
                      + loclist[i]
                      + (j >= 0 ? " (Bundle #" + b[j].getBundleId() + ")"
                         : ""));
          showPerms(out, permissionAdmin.getPermissions(loclist[i]));
        }
      }
    }
    return 0;
  }

  //
  // Refresh command
  //

  public final static String USAGE_REFRESH = "[<bundle>] ...";

  public final static String[] HELP_REFRESH = new String[] {
    "Refresh all exported java packages belong to specified bundle",
    "If no bundle is specified refresh all bundles",
    "<bundle> Name or id of bundle" };

  public int cmdRefresh(Dictionary opts, Reader in, PrintWriter out,
                        Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    String[] bs = (String[]) opts.get("bundle");
    if (bs != null) {
      Bundle[] b = getBundles(bs, true);
      for (int i = 0; i < b.length; i++) {
        if (b[i] == null) {
          Bundle[] nb = new Bundle[i];
          System.arraycopy(b, 0, nb, 0, nb.length);
          b = nb;
          break;
        }
      }
      if (b.length == 0) {
        out.println("ERROR! No matching bundle");
        return 1;
      }
      packageAdmin.refreshPackages(b);
    } else {
      packageAdmin.refreshPackages(null);
    }
    return 0;
  }

  //
  // Resolve command
  //

  public final static String USAGE_RESOLVE = "[<bundle>] ...";

  public final static String[] HELP_RESOLVE = new String[] {
    "Resolve one or more bundles",
    "If no bundle is specified resolve all bundles",
    "<bundle> Name or id of bundle" };

  public int cmdResolve(Dictionary opts, Reader in, PrintWriter out,
                        Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    String[] bs = (String[]) opts.get("bundle");
    Bundle[] b = null;
    if (bs != null) {
      b = getBundles(bs, true);
      for (int i = 0; i < b.length; i++) {
        if (b[i] == null) {
          Bundle[] nb = new Bundle[i];
          System.arraycopy(b, 0, nb, 0, nb.length);
          b = nb;
          break;
        }
      }
      if (b.length == 0) {
        out.println("ERROR! No matching bundle");
        return 1;
      }
    }
    packageAdmin.resolveBundles(b);
    return 0;
  }

  //
  // Services command
  //

  public final static String USAGE_SERVICES
    = "[-i] [-l] [-sid #id#] [-f #filter#] [-r] [-u] [<bundle>] ...";

  public final static String[] HELP_SERVICES = new String[] {
    "List registered services",
    "-i          Sort on bundle id",
    "-l          Verbose output",
    "-r          Show services registered by named bundles (default)",
    "-sid #id#   Show the service with the specified service id",
    "-u          Show services used by named bundles",
    "-f #filter# Show all services that matches the specified filter.",
    "<bundle>    Name or id of bundle" };

  public int cmdServices(final Dictionary opts, Reader in,
                         final PrintWriter out, Session session) {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"), opts
                                  .get("-i") != null);
    Integer res = (Integer) AccessController.doPrivileged(new PrivilegedAction()
      {
        public Object run() {
          boolean useDefaultOper = opts.get("-r") == null
            && opts.get("-u") == null;
          ServiceReference[] fs = null;
          String filter = null;
          if (opts.get("-f") != null) {
            filter = (String) opts.get("-f");
            if ('(' != filter.charAt(0)) {
              filter = "(" +filter +")";
            }
          }
          if (opts.get("-sid") != null) {
            if (filter!=null) {
              filter = "(&(service.id=" +opts.get("-sid") +")" +filter +")";
            } else {
              filter = "(service.id=" +opts.get("-sid") +")";
            }
          }
          if (filter != null) {
            try {
              fs = bc.getServiceReferences(null, filter);
              if (null==fs) {
                out.println("No services matching '"+filter +"'.");
                return new Integer(0);
              }
            } catch (InvalidSyntaxException ise) {
              out.println("Invalid filter '" +filter +"' found: "
                          +ise.getMessage());
              return new Integer(1);
            }
          }

          for (int i = 0; i < b.length; i++) {
            if (b[i] != null) {
              final String heading = "Bundle: " + showBundle(b[i]);
              boolean headingPrinted = false;
              if (opts.get("-r") != null || useDefaultOper) {
                headingPrinted =
                  showServices(getServicesRegisteredBy(b[i],fs),
                               out,
                               heading,
                               headingPrinted,
                               "  registered:",
                               opts.get("-l") != null);
              }
              if (opts.get("-u") != null) {
                headingPrinted =
                  showServices(getServicesUsedBy(b[i],fs),
                               out,
                               heading,
                               headingPrinted,
                               "  uses:",
                               opts.get("-l") != null);
              }
            }
          }
          return new Integer(0);
        }
      });
    return res.intValue();
  }

  ServiceReference[] getServicesRegisteredBy(Bundle b,
                                             ServiceReference[] services)
  {
    if (services==null) {
      return b.getRegisteredServices();
    }
    // Filter the given services on registered by.
    long bid = b.getBundleId();
    int count = 0;
    for (int j = 0; j<services.length; j++) {
      if (bid==services[j].getBundle().getBundleId()) {
        count++;
      }
    }
    if (0==count) return null;
    ServiceReference[] res = new ServiceReference[count];
    int ix = 0;
    for (int j = 0; j<services.length; j++) {
      if (bid==services[j].getBundle().getBundleId()) {
        res[ix++] = services[j];
      }
    }
    return res;
  }

  ServiceReference[] getServicesUsedBy(Bundle b,
                                       ServiceReference[] services)
  {
    if (null==services) {
      return b.getServicesInUse();
    }
    // Filter the given services on using bundle.
    long bid = b.getBundleId();
    int count = 0;
    for (int j = 0; j<services.length; j++) {
      Bundle[] usingBundles = services[j].getUsingBundles();
      for (int k=0; usingBundles!=null && k<usingBundles.length; k++) {
        if (bid==usingBundles[k].getBundleId()) {
          count++;
          break;
        }
      }
    }
    if (0==count) return null;
    ServiceReference[] res = new ServiceReference[count];
    int ix = 0;
    for (int j = 0; j<services.length; j++) {
      Bundle[] usingBundles = services[j].getUsingBundles();
      for (int k=0; usingBundles!=null && k<usingBundles.length; k++) {
        if (bid==usingBundles[k].getBundleId()) {
          res[ix++] = services[j];
          break;
        }
      }
    }
    return res;
  }

  boolean showServices(final ServiceReference[] services,
                       final PrintWriter out,
                       final String heading,
                       final boolean headinPrinted,
                       final String title,
                       final boolean detailed)
  {
    if (services != null && services.length > 0) {
      if (!headinPrinted) {
        out.println(heading);
      }
      out.print(title);
      for (int j = 0; j<services.length; j++) {
        if (null!=services[j]) {
          if (detailed) {
            out.print("\n    ");
            showLongService(services[j], "    ", out);
          } else {
            out.print(" "+ Util.showServiceClasses(services[j]));
          }
        }
      }
      out.println("");
      return true;
    }
    return false;
  }

  void showLongService(ServiceReference s, String pad, PrintWriter out) {
    out.print(Util.showServiceClasses(s));
    String[] k = s.getPropertyKeys();
    for (int i = 0; i < k.length; i++) {
      out.print("\n  " + pad + k[i] + " = "
                + Util.showObject(s.getProperty(k[i])));
    }
  }


  //
  // SetCondPermission command
  //

  public final static String USAGE_SETCONDPERMISSION
    = "[-name #name] <conditional_permission_info>...";

  public final static String[] HELP_SETCONDPERMISSION = new String[] {
    "Set conditional permission",
    "-name #name                     Name of conditional permission",
    "<conditional_permission_info>   ConditionalPermissionInfo string",
    "",
    "Example that grants all bundles installed with a file-url all permissions:",
    "> setcondpermission '[org.osgi.service.condpermadmin.BundleLocationCondition \"file:*\"]' (java.security.AllPermission)" };

  public int cmdSetcondpermission(Dictionary opts, Reader in, PrintWriter out,
                                  Session session) {
    if (condPermAdmin == null) {
      out.println("Conditional Permission Admin service is not available");
      return 1;
    }
    String loc = null;
    Vector /* ConditionInfo */ cis = new Vector();
    Vector /* PermissionInfo */ pis = new Vector();
    String name = (String) opts.get("-name");
    String [] cpis = (String []) opts.get("conditional_permission_info");
    String endChar = null;
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < cpis.length; ) {
      String cpi = cpis[i];
      if (endChar != null) {
        buf.append(cpi);
        i++;
        if (cpi.endsWith(endChar)) {
          try {
            if (endChar == "]") {
              cis.addElement(new ConditionInfo(buf.toString()));
            } else {
              pis.addElement(new PermissionInfo(buf.toString()));
            }
          } catch (IllegalArgumentException e) {
            out.println("ERROR! Failed to instanciate: " + buf.toString()
                + " " + e.getMessage());
            return 1;
          }
          endChar = null;
          buf.setLength(0);
        } else {
          buf.append(' ');
        }
      } else if (cpi.startsWith("[")) {
        endChar = "]";
      } else if (cpi.startsWith("(")) {
        endChar = ")";
      } else {
        out.println("ERROR! Expected start char '(' or '[', got: " + cpi);
        return 1;
      }
    }
    ConditionInfo [] cia = (ConditionInfo []) cis.toArray(new ConditionInfo [cis.size()]);
    PermissionInfo [] pia = (PermissionInfo []) pis.toArray(new PermissionInfo [pis.size()]);
    condPermAdmin.setConditionalPermissionInfo(name, cia, pia);
    return 0;
  }

  //
  // Start command
  //

  public final static String USAGE_START = "[-t] [-e] <bundle> ...";
  public final static String[] HELP_START = new String[] {
    "Persistently start one or more bundles according to their ",
    "activation policy.",
    "-t       Perform a transient start. I.e., non-persisten start.",
    "-e       Eagerly start the bundles, ignoring their activation policy.",
    "<bundle> Name or id of bundle"
  };

  public int cmdStart(Dictionary opts, Reader in, PrintWriter out,
                      Session session) {
    int startOptions = 0;
    if (opts.get("-t") != null) {
      startOptions |= Bundle.START_TRANSIENT;
    }
    if (opts.get("-e") == null) {
      startOptions |= Bundle.START_ACTIVATION_POLICY;
    }

    Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (int i = 0; i < b.length; i++) {
      if (b[i] != null) {
        try {
          b[i].start(startOptions);
          out.println("Started: " + showBundle(b[i]));
        } catch (BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null)
            t = ((BundleException) t).getNestedException();
          out.println("Couldn't start bundle: " + showBundle(b[i])
                      + " (due to: " + t + ")");
          t.printStackTrace(out);
        }
        found = true;
      }
    }
    if (!found) {
      out.println("ERROR! No matching bundle");
      return 1;
    }
    return 0;
  }

  //
  // Stop command
  //
  public final static String USAGE_STOP = "[-t] <bundle> ...";
  public final static String[] HELP_STOP = new String[] {
    "Persitently stop one or more bundles",
    "-t       Perform a transient stop. I.e., non-persisten stop.",
    "<bundle> Name or id of bundle"
  };

  public int cmdStop(Dictionary opts, Reader in, PrintWriter out,
                     Session session) {
    int stopOptions = 0;
    if (opts.get("-t") != null) {
      stopOptions |= Bundle.STOP_TRANSIENT;
    }

    Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (int i = b.length - 1; i >= 0; i--) {
      if (b[i] != null) {
        try {
          b[i].stop(stopOptions);
          out.println("Stopped: " + showBundle(b[i]));
        } catch (BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null)
            t = ((BundleException) t).getNestedException();
          out.println("Couldn't stop bundle: " + showBundle(b[i])
                      + " (due to: " + t + ")");
        }
        found = true;
      }
    }
    if (!found) {
      out.println("ERROR! No matching bundle");
      return 1;
    }
    return 0;
  }

  //
  // Showstate command
  //

  public final static String USAGE_SHOWSTATE = "[<pid>] ...";

  public final static String[] HELP_SHOWSTATE = new String[] {
    "Show the state of a service, if the service provides state information",
    "<pid>     The service pid(s) of interest" };

  public int cmdShowstate(Dictionary opts, Reader in, PrintWriter out,
                          Session session) {
    String[] pids = (String[]) opts.get("pid");
    try {
      if (pids != null && pids.length > 0) {
        for (int i = 0; i < pids.length; i++) {
          showstate(out, bc.getServiceReferences(null, "(service.id="
                                                 + pids[i] + ")"));
        }
      } else
        showstate(out, bc.getServiceReferences(null, "(state=*)"));
    } catch (Exception e) {
      out.println("Error: " + e);
    }
    return 0;
  }

  //
  // Shutdown command
  //

  public final static String USAGE_SHUTDOWN = "[-r]";

  public final static String[] HELP_SHUTDOWN = new String[] {
    "Shutdown framework", "-r Restart framework" };

  public int cmdShutdown(Dictionary opts, Reader in, PrintWriter out,
                         Session session) {

    boolean restart = opts.get("-r") != null;

      Bundle sysBundle = bc.getBundle(0);
      if (restart) {
        try {
          sysBundle.update(); // restart the framework
        } catch (Exception e) {
          out.println("Failed to restart the framework " + e);
          return 1;
        }
      } else {
        try {
          sysBundle.stop(); // shut down the framework
        } catch (Exception e) {
          out.println("Failed to stop using system bundle " + e);
          try {
            System.exit(0);
          } catch (Exception e2) {
            out.println("Failed to exit using system exit " + e2);
            return 1;
          }
        }
      }
    return 0;
  }

  //
  // Threads command
  //

  public final static String USAGE_THREADS = "[-a]";

  public final static String[] HELP_THREADS = new String[] {
    "Display threads within this framework", "-a  List all threads in this JVM" };

  public int cmdThreads(Dictionary opts, Reader in, PrintWriter out,
                         Session session) {
    final boolean showAll = opts.get("-a") != null;
    ThreadGroup tg = Thread.currentThread().getThreadGroup();

    for (ThreadGroup ctg = tg; ctg != null; ctg = ctg.getParent()) {
      if (showAll) {
        tg = ctg;
      } else if (ctg.getName().startsWith("FW#")) {
        tg = ctg;
        break;
      }
    }
    
    Thread [] threads;
    int count;
    while (true) {
      int acount = tg.activeCount() + 5;
      threads = new Thread[acount];
      count = tg.enumerate(threads);
      if (count < acount) {
        break;
      }
    }
    int groupCols = tg.getName().length();
    boolean sameGroup = true;
    for (int i = 0; i < count; i++) {
      ThreadGroup itg = threads[i].getThreadGroup();
      if (!tg.equals(itg)) {
        int cols = itg.getName().length();
        if (groupCols < cols) {
          groupCols = cols;
        }
        sameGroup = false;
      }
    }
    out.print("Pri ");
    if (!sameGroup) {
      String glabel = "Group                              ";
      if (groupCols < 4) {
        groupCols = 4;
      }
      if (++groupCols > glabel.length()) {
        groupCols = glabel.length();
        out.print(glabel);
      } else {
        out.print(glabel.substring(0, groupCols));
      }
    }
    out.println("Name");
    for (int i = 0; i < count; i++) {
      try {
        StringBuffer sb = new StringBuffer();
        int p = threads[i].getPriority();
        if (p < 10) {
          sb.append(' ');
        }
        sb.append(p);
        do {
          sb.append(' ');
        } while (sb.length() < 4);
        if (!sameGroup) {
          String g = threads[i].getThreadGroup().getName();
          sb.append(g);
          int l = g.length();
          do {
            sb.append(' ');
            l++;
          } while (l < groupCols);
        }
        sb.append(threads[i].getName());
        out.println(sb.toString());
      } catch (NullPointerException _ignore) {
        // Handle disappering thread
      }
    }
    return 0;
  }


  //
  // Uninstall command
  //

  public final static String USAGE_UNINSTALL = "<bundle> ...";

  public final static String[] HELP_UNINSTALL = new String[] {
    "Uninstall one or more bundles", "<bundle> Name or id of bundle" };

  public int cmdUninstall(Dictionary opts, Reader in, PrintWriter out,
                          Session session) {
    Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (int i = 0; i < b.length; i++) {
      if (b[i] != null) {
        try {
          b[i].uninstall();
          out.println("Uninstalled: " + showBundle(b[i]));
        } catch (BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null)
            t = ((BundleException) t).getNestedException();
          out.println("Couldn't uninstall: " + showBundle(b[i])
                      + " (due to: " + t + ")");
        }
        found = true;
      }
    }
    if (!found) {
      out.println("ERROR! No matching bundle");
      return 1;
    }
    return 0;
  }

  //
  // Update command
  //

  public final static String USAGE_UPDATE = "<bundle> ...";

  public final static String[] HELP_UPDATE = new String[] {
    "Update one or more bundles",
    "<bundle> Name or id of bundle",
    "Note: Use refresh command to force the framework to do a package update",
    "of exported packages used by running bundles." };

  public int cmdUpdate(Dictionary opts, Reader in, PrintWriter out,
                       Session session) {
    Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (int i = b.length - 1; i >= 0; i--) {
      if (b[i] != null) {
        try {
          b[i].update();
          out.println("Updated: " + showBundle(b[i]));
        } catch (BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null)
            t = ((BundleException) t).getNestedException();
          out.println("Couldn't update: " + showBundle(b[i])
                      + " (due to: " + t + ")");
        }
        found = true;
      }
    }
    if (!found) {
      out.println("ERROR! No matching bundle");
      return 1;
    }
    out
      .println("Note: Use refresh command to update exported packages in running bundles");
    return 0;
  }

  public final static String USAGE_FROMUPDATE = "<bundle> <url>";

  public final static String[] HELP_FROMUPDATE = new String[] {
    "Update a bundle from a specific URL",
    "<bundle> - Name or id of bundle",
    "<url>    - URL to update from",
    "Note 1: Use refresh command to force the framework to do a package update",
    "of exported packages used by running bundles.",
    "Note 2: The base URLs used to complete partial URLs may be set using the cd command"
  };

  public int cmdFromupdate(Dictionary opts, Reader in, PrintWriter out,
                           Session session) {
    String bname = (String) opts.get("bundle");
    Bundle[] bl = getBundles(new String[] { bname }, true);
    String fromURL = completeLocation((String) opts.get("url"));

    Bundle b = bl[0];
    if (b == null) {
      out.println("ERROR! No matching bundle for '" + bname + "'");
      return 1;
    }

    try {
      URL url = new URL(fromURL);
      URLConnection conn = url.openConnection();
      InputStream inStream = conn.getInputStream();
      b.update(inStream);
      out.println("Updated: " + showBundle(b));
    } catch (BundleException e) {
      Throwable t = e;
      while (t instanceof BundleException
             && ((BundleException) t).getNestedException() != null)
        t = ((BundleException) t).getNestedException();
      out.println("Couldn't update: " + showBundle(b) + " (due to: " + t
                  + ")");
    } catch (Exception e) {
      out.println("Couldn't update: " + showBundle(b) + " (due to: " + e
                  + ")");
    }

    out.println("Note: Use refresh command to update exported packages "
                +"in running bundles");
    return 0;
  }

  public final static String USAGE_FROMINSTALL = "<url> [<location>]";

  public final static String[] HELP_FROMINSTALL = new String[] {
    "Install a bundle with a specific location from an URL",
    "<url>      - URL to bundle jar file",
    "<location> - Optional location string to use for installation",
    "Note: The base URLs used to complete partial URLs may be set using the cd command"
  };

  public int cmdFrominstall(Dictionary opts, Reader in, PrintWriter out,
                            Session session) {
    final String fromURL = completeLocation( (String) opts.get("url"));
    String loc = (String) opts.get("location");

    if (loc == null) {
      loc = fromURL;
    }

    try {
      URL url = new URL(fromURL);
      URLConnection conn = url.openConnection();
      InputStream inStream = conn.getInputStream();
      Bundle b = bc.installBundle(loc, inStream);
      out.println("Installed: " + showBundle(b));
    } catch (BundleException e) {
      Throwable t = e;
      while (t instanceof BundleException
             && ((BundleException) t).getNestedException() != null)
        t = ((BundleException) t).getNestedException();
      out.println("Couldn't install: url=" + fromURL + ", location="
                  + loc + " (due to: " + t + ")");
    } catch (Exception e) {
      out.println("Couldn't install: url=" + fromURL + ", location="
                  + loc + " (due to: " + e + ")");
    }

    return 0;
  }

  //
  // Private methods
  //

  private void showstate(PrintWriter out, ServiceReference[] srs) {
    if (srs != null) {
      for (int i = 0; i < srs.length; i++) {
        Object state = srs[i].getProperty("state");
        if (state != null) {
          out.println("State for " + srs[i].getProperty("service.id")
                      + ":");
          out.println(state.toString());
        }
      }
    }
  }

  private Bundle[] getBundles(String[] selection, boolean sortNumeric) {
    return getBundles(selection, sortNumeric, false, false);
  }

  private Bundle[] getBundles(String[] selection,
                              boolean sortNumeric,
                              boolean sortStartLevel) {
    return getBundles(selection, sortNumeric, sortStartLevel, false);
  }

  private Bundle[] getBundles(String[] selection,
                              boolean sortNumeric,
                              boolean sortStartLevel,
                              boolean sortTime) {
    Bundle[] b = bc.getBundles();
    Util.selectBundles(b, selection);
    if (sortNumeric) {
      Util.sortBundlesId(b);
    } else {
      Util.sortBundles(b, false);
    }
    if (sortStartLevel) {
      sortBundlesStartLevel(b);
    }
    if (sortTime) {
      Util.sortBundlesTime(b);
    }

    return b;
  }

  /**
   * Sort an array of bundle objects based on their start level All entries
   * with no start level is placed at the end of the array.
   *
   * @param b
   *            array of bundles to be sorted, modified with result
   */
  protected void sortBundlesStartLevel(Bundle[] b) {
    int x = b.length;

    for (int l = x; x > 0;) {
      x = 0;
      int p = Integer.MAX_VALUE;
      try {
        p = startLevel.getBundleStartLevel(b[0]);
      } catch (Exception ignored) {
      }
      for (int i = 1; i < l; i++) {
        int n = Integer.MAX_VALUE;
        try {
          n = startLevel.getBundleStartLevel(b[i]);
        } catch (Exception ignored) {
        }
        if (p > n) {
          x = i - 1;
          Bundle t = b[x];
          b[x] = b[i];
          b[i] = t;
        } else {
          p = n;
        }
      }
    }
  }

  public String showState(Bundle bundle) {
    StringBuffer sb = new StringBuffer();

    try {
      StringBuffer s = new StringBuffer
        (Integer.toString(startLevel.getBundleStartLevel(bundle)));
      while (s.length() < 2) {
        s.insert(0, " ");
      }
      sb.append(s.toString());
    } catch (Exception ignored) {
      sb.append("--");
    }

    sb.append("/");

    switch (bundle.getState()) {
    case Bundle.INSTALLED:
      sb.append("installed");
      break;
    case Bundle.RESOLVED:
      sb.append("resolved");
      break;
    case Bundle.STARTING:
      sb.append("starting");
      break;
    case Bundle.ACTIVE:
      sb.append("active");
      break;
    case Bundle.STOPPING:
      sb.append("stopping");
      break;
    case Bundle.UNINSTALLED:
      sb.append("uninstalled");
      break;
    default:
      sb.append("ILLEGAL <" + bundle.getState() + "> ");
      break;
    }
    while (sb.length() < 13) {
      sb.append(" ");
    }

    return sb.toString();
  }

  String showBundle(Bundle b) {
    return Util.shortName(b) + " (#" + b.getBundleId() + ")";
  }

  private void showPerms(PrintWriter out, PermissionInfo[] pi) {
    final String shift = "    ";
    if (pi == null) {
      out.println(shift + "DEFAULT");
    } else if (pi.length == 0) {
      out.println(shift + "NONE");
    } else {
      for (int i = 0; i < pi.length; i++) {
        out.println(shift + pi[i]);
      }
    }
  }

  //
  // Set start level command
  //
  public final static String USAGE_STARTLEVEL = "[<level>]";

  public final static String[] HELP_STARTLEVEL = new String[] {
    "Shows or sets the global startlevel", "[<level>] new start level",
    "          if no <level> is provided, show current level", };

  public int cmdStartlevel(Dictionary opts, Reader in, PrintWriter out,
                           Session session) {

    String levelStr = (String) opts.get("level");

    try {
      if (levelStr != null) {
        int level = Integer.parseInt(levelStr);
        startLevel.setStartLevel(level);
      } else {
        out.println("current start level:        "
                    + startLevel.getStartLevel());
        out.println("initial bundle start level: "
                    + startLevel.getInitialBundleStartLevel());
      }
      return 0;
    } catch (Exception e) {
      out.println("Failed to show/set startlevel=" + levelStr);
      e.printStackTrace(out);
      return -1;
    }
  }

  //
  // CD command
  //
  public final static String USAGE_CD = "[-reset] [-a] [<base URL>] ...";

  public final static String[] HELP_CD = new String[] {
    "Shows or sets the base URLs used to complete bundle location",
    "when installing bundles.",
    "[-reset]       reset the base URL list to the startup value.",
    "[-a]           append given base URLs to the current list.",
    "[<base URL>] ... new list of base URLs to be used.", };

  public int cmdCd(Dictionary opts, Reader in, PrintWriter out,
                   Session session) {

    final String[] baseURLsArg = (String[]) opts.get("base URL");
    final boolean append = opts.get("-a") != null;

    try {
      if (opts.get("-reset") != null) {
        setupJars();
      }
      if (baseURLsArg == null) {
        for (Iterator it = baseURLs.iterator(); it.hasNext(); ) {
          out.println(" " + it.next());
        }
      } else {
        setupJars(baseURLsArg, append);
      }
      return 0;
    } catch (Exception e) {
      out.println("Failed to cd: "+e);
      e.printStackTrace(out);
      return -1;
    }
  }

  //
  // Set bundle start level
  //
  public final static String USAGE_BUNDLELEVEL = "<level> [<bundle>] ...";

  public final static String[] HELP_BUNDLELEVEL = new String[] {
    "Set startlevel(s) for bundles", "<level>   new start level",
    "<bundle>  Name or id of bundles",
    "          If bundle list is empty, set initial",
    "          start level for new bundles", };

  public int cmdBundlelevel(Dictionary opts, Reader in, PrintWriter out,
                            Session session) {
    int level = -1;
    try {
      level = Integer.parseInt((String) opts.get("level"));
      String[] bls = (String[]) opts.get("bundle");
      Bundle[] bl = getBundles(bls, false, false);

      if (bls == null || bls.length == 0) {
        startLevel.setInitialBundleStartLevel(level);
        out.println("initial bundle start level set to " + level);
      } else {
        for (int i = 0; i < bl.length; i++) {
          if (bl[i] != null) {
            System.out.println("set " + i + " " + bl[i] + " "
                               + level);
            startLevel.setBundleStartLevel(bl[i], level);
          }
        }
      }
      return 0;
    } catch (Exception e) {
      out.println("Failed to set bundle startlevel=" + level);
      e.printStackTrace(out);
      return -1;
    }
  }

  //
  // Property command
  //
  public final static String USAGE_PROPERTY = "[-s] [-f] [<property>] ...";

  public final static String[] HELP_PROPERTY = new String[] {
    "Lists Framework and System properties with values.",
    "If no property name is specified all properties will be listed.",
    "[-f]             Only list framework properties, i.e., those mentioned in",
    "                 the OSGi specification and other properties that are",
    "                 known to be present in the frameworks property map.",
    "[-s]             Show the value returned by System.getProperty().",
    "                 The default is to show the value returned by",
    "                 BundleContext.getProperty().",
    "[<property>] ... Property keys to include in the list.", };

  public int cmdProperty(Dictionary opts, Reader in, PrintWriter out,
                         Session session)
  {
    final boolean sysProps = opts.get("-s") != null;
    final boolean includeFwPros = opts.get("-f") != null;
    final String[] propNamesA = (String[]) opts.get("property");

    try {
      final Set propNames = new TreeSet();
      if (includeFwPros) {
        // -f
        propNames.addAll(getAllFrameworkPropKeys());
      } else if (null!=propNamesA) {
        // List specified props
        propNames.addAll(Arrays.asList(propNamesA));
      } else {
        // List all props
        propNames.addAll(getAllFrameworkPropKeys());
        propNames.addAll(getAllSystemPropKeys());
      }

      for(Iterator it = propNames.iterator(); it.hasNext();) {
        final String key = (String) it.next();
        final String val = sysProps
          ? (String) System.getProperty(key)
          : (String) bc.getProperty(key);
        if (null!=val) {
          out.println("  " + key + " : " + val);
        }
      }

      return 0;
    } catch (Exception e) {
      out.println("Failed to print props values: "+e);
      e.printStackTrace(out);
      return -1;
    }
  }
  // The key under which the KF-framework keeps a comma-separated list
  // of all framework property keys.
  public static final String fwPropKeysKey
    = "org.knopflerfish.framework.bundleprops.keys";
  public static final Set FW_PROP_NAMES = new HashSet() {{
    add(Constants.FRAMEWORK_VENDOR);
    add(Constants.FRAMEWORK_VERSION);
    add(Constants.FRAMEWORK_LANGUAGE);
    add(Constants.FRAMEWORK_OS_NAME);
    add(Constants.FRAMEWORK_OS_VERSION);
    add(Constants.FRAMEWORK_PROCESSOR);
    add(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
    add(Constants.FRAMEWORK_BOOTDELEGATION);
    add(Constants.FRAMEWORK_STORAGE);
    add(Constants.FRAMEWORK_STORAGE_CLEAN);
    add(Constants.FRAMEWORK_TRUST_REPOSITORIES);
    add(Constants.FRAMEWORK_EXECPERMISSION);
    add(Constants.FRAMEWORK_LIBRARY_EXTENSIONS);
    add(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
    add(Constants.FRAMEWORK_BUNDLE_PARENT);
    add(Constants.FRAMEWORK_WINDOWSYSTEM);
    add(Constants.FRAMEWORK_SECURITY);
    add(Constants.SUPPORTS_FRAMEWORK_EXTENSION);
    add(Constants.SUPPORTS_FRAMEWORK_FRAGMENT);
    add(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE);
  }};
  // The set of keys for all Framework properties
  private Set getAllFrameworkPropKeys()
  {
    final HashSet res = new HashSet();

    // Keys of properites mentioned in the OSGi specification.
    res.addAll(FW_PROP_NAMES);

    // All available keys from a property mainained by the
    // Knopflerfish Framewwork implementation for this purpose.
    final String fwPropKeys = bc.getProperty(fwPropKeysKey);
    if (null!=fwPropKeys) {
      final StringTokenizer st = new StringTokenizer(fwPropKeys,",");
      while (st.hasMoreTokens()) {
        final String key = ((String) st.nextToken()).trim();
        res.add(key);
      }
    }
    return res;
  }
  // The set of keys for all System properties
  private Set getAllSystemPropKeys()
  {
    final HashSet res = new HashSet();

    final Properties properties = System.getProperties();
    for (Enumeration pke = properties.propertyNames(); pke.hasMoreElements();){
      final String key = (String) pke.nextElement();
      res.add(key);
    }
    return res;
  }


}
