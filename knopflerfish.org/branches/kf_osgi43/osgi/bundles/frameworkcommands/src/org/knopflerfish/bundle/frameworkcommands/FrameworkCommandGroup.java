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

package org.knopflerfish.bundle.frameworkcommands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;


/**
 * Console commands for interaction with the framework and its
 * standard services.
 *
 * @author Makewave AB
 */

public class FrameworkCommandGroup
  extends CommandGroupAdapter
{
  final BundleContext bc;

  private final PackageAdmin packageAdmin;
  private final PermissionAdminHelper permissionAdminHelper;

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
  private final List<URL> baseURLs = new ArrayList<URL>();


  FrameworkCommandGroup(BundleContext bc) {
    super("framework", "Framework commands");
    this.bc = bc;

    // all of these services are framework singleton internal services
    // thus, we take a shortcut and skip the service tracking

    final ServiceReference<?> sr = bc.getServiceReference(PackageAdmin.class
                                                 .getName());
    packageAdmin = null==sr ? null : (PackageAdmin) bc.getService(sr);

    permissionAdminHelper = initPermissionAdminHelper();

    try {
      setupJars();
    } catch (final MalformedURLException mfe) {
    }
  }

  private PermissionAdminHelper initPermissionAdminHelper() {
    // Try to see if we can create the PermissionAdminHelper object.
    try {
      return new PermissionAdminHelperImpl(bc);
    } catch (final Exception ex) {
      //log.error("Failed to create permissionAdminHelper: " + ex, ex);
    } catch (final LinkageError ce) {
      //log.info("There is no PermissionAdmin service available.", ce);
    }
    return null;
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

    for (final String prefixe : prefixes) {
      try {
        baseURLs.add(new URL(prefixe));
      } catch (final MalformedURLException mfe) {
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
      for (final URL baseURL : baseURLs) {
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
              if (is != null) {
                is.close();
              }
            }
          }
          location = url.toString();
          break; // Found.
        } catch (final Exception _e) {
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

  public int cmdAddpermission(Dictionary<String,?> opts, Reader in, PrintWriter out,
                              Session session) {
    if (permissionAdminHelper == null) {
      out.println("Permission Admin service is not available");
      return 1;
    } else {
      return permissionAdminHelper.cmdAddpermission(opts, in, out, session);
    }
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

  public int cmdBundles(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null,
                            opts.get("-s") != null,
                            opts.get("-t") != null);
    final boolean verbose = (opts.get("-l") != null);
    final boolean oneColumn = (opts.get("-1") != null);

    final Vector<Bundle> tmp = new Vector<Bundle>();
    for (final Bundle element : b) {
      if (element != null) {
        tmp.add(element);
      }
    }

    if (tmp.size() == 0) {
      out.println("ERROR! No matching bundle");
      return 1;
    } else {
      printBundles(out, tmp.toArray(new Bundle[tmp.size()]), verbose, oneColumn);
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
      final SimpleDateFormat dateFormat = new SimpleDateFormat();
      for (int i = 0; i < b.length; i++) { // Or just look at the first one...
        lastModified[i] = dateFormat.format(new Date(b[i].getLastModified()));
        if (lastModified[i].length() > longestLM) {
          longestLM = lastModified[i].length();
        }
      }
      String lmHeader = "modified";
      if (longestLM > lmHeader.length()) {
        final String blank = "                                    ";
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
        level = String.valueOf(b[i].adapt(BundleStartLevel.class).getStartLevel());
        if (level.length() < 2) {
          level = " " + level;
        }
      } catch (final Exception e) {
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
        final String s = Util.showId(b[i]) + showState(b[i])
          + Util.shortName(b[i]) + getBundleSpeciality(b[i]);
        if ((i & 1) == 0 && !oneColumn) {
          out.print(s);
          final int l = 40 - s.length();
          if (l > 0) {
            final String blank = "                                    ";
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
    final StringBuffer sb = new StringBuffer();
    final Bundle[] fragments = packageAdmin.getFragments(bundle);
    if (fragments != null && fragments.length > 0) {
      sb.append("h:"); // host
      for (int i=0; i<fragments.length;i++){
        if (i>0) {
          sb.append(",");
        }
        sb.append(fragments[i].getBundleId());
      }
    }
    final Bundle[] hosts = packageAdmin.getHosts(bundle);
    if (hosts != null && hosts.length > 0) {
      sb.append("f:"); // fragment
      for (int i=0; i<hosts.length;i++){
        if (i>0) {
          sb.append(",");
        }
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

  public int cmdCall(final Dictionary<String,?> opts,
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
    final ServiceReference<?> sr = AccessController
      .doPrivileged(new PrivilegedAction<ServiceReference<?>>() {
          public ServiceReference<?> run() {
            ServiceReference<?> res = null;
            if (null==filter) {
              res = bc.getServiceReference(si);
              if (null==res) {
                out.println("No service with interface '" + si +"'.");
              }
            } else {
              try {
                final ServiceReference<?>[] srs = bc.getServiceReferences(si,filter);
                if (null==srs) {
                  out.println("No service that matches the filter '"
                              +filter +"'.");
                } else if (1==srs.length) {
                  res = srs[0];
                } else {
                  out.println("Multiple service matches the filter '"
                              +filter +"' please narrow it down.");
                }
              } catch (final InvalidSyntaxException ise) {
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
    final Object s = AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
          return bc.getService(sr);
        }
      });
    if (s == null) {
      out.println("No such service: " + si);
      return 1;
    }

    final String method = (String) opts.get("method");
    Class<?>[] parameterTypes = null;
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
        final int ix = val.indexOf("::");
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
    } catch (final InvocationTargetException e) {
      out.println("Exception thrown by call");
      e.getTargetException().printStackTrace(out);
    } catch (final IllegalAccessException e) {
      out.println("Call method not accessible (must be public)");
    } catch (final NullPointerException e) {
      out.println("Internal error: " + e);
    } catch (final IllegalArgumentException e) {
      out.println("Internal error: " + e);
    } catch (final NoSuchMethodException e) {
      out.println("No method '" + method + "' with matching arguments: "
                  + e);
    }

    bc.ungetService(sr);
    return res;
  }

  Class<?> getClass(String className) {
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
    } catch (final Exception e) {
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
  Class<?> findClass(final String type, final Class<?> clazz)
  {
    if (type.equals(clazz.getName())) {
      return clazz;
    }

    final Class<?>[] clazzes = clazz.getInterfaces();
    for (final Class<?> clazze : clazzes) {
      if (type.equals(clazze.getName())) {
        return clazze;
      }
    }
    return findClass(type, clazz.getSuperclass());
  }

  Method findMethod(final String si,
                    final Class<?> clazz,
                    final String name,
                    final int nArgs)
  {
    Class<?> ifClass = findClass(si, clazz);
    // Fallback to use the original class if the one given by si was not found.
    if (null==ifClass) {
      ifClass = clazz;
    }

    final Method[] methods = ifClass.getMethods();
    final Vector<Method> v = new Vector<Method>();
    for (final Method method : methods) {
      if (method.getName().equals(name)
          && method.getParameterTypes().length == nArgs) {
        v.addElement(method);
      }
    }
    if (v.size() == 1) {
      return v.elementAt(0);
    }
    return null;
  }

  Object makeObject(String val, String className) {
    try {
      final Class<?> clazz = getClass(className);

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

      final Constructor<?> cons = clazz
        .getConstructor(new Class[] { String.class });
      final Object r = cons.newInstance(new Object[] { val });
      return r;
    } catch (final Exception e) {
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

  public int cmdCertificates(Dictionary<String,?> opts, Reader in, PrintWriter out,
                              Session session) {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null);
    boolean match = false;
    for (final Bundle element : b) {
      if (element != null) {
        try {
          boolean found = false;
          final Method m = element.getClass().getMethod("getCertificates",
                                               (Class[]) null);
          final Certificate [] cs = (Certificate []) m.invoke(element,
                                                        (Object[]) null);
          out.println("Bundle: " + showBundle(element));
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
        } catch (final Exception e) {
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

  public int cmdClosure(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {

    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }

    final String bname = (String) opts.get("bundle");
    Bundle[] bl = getBundles(new String[] { bname }, true);
    final Bundle bundle = bl[0];
    if (bundle == null) {
      out.println("ERROR! No matching bundle for '" + bname + "'");
      return 1;
    }

    bl = getBundles(null, false, false, false);

    // Package

    final Vector<Bundle> pkgClosure = new Vector<Bundle>();
    // This is O(n2) at least, possibly O(n3). Should be improved
    for(int b = 0; b < bl.length; b++) {
      final ExportedPackage[] pkgs = packageAdmin.getExportedPackages(bl[b]);
      if (pkgs == null) {
        continue;
      }
      for (final ExportedPackage pkg : pkgs) {
        final Bundle[] bl2 = pkg.getImportingBundles();
        if (bl2 == null) {
          continue;
        }
        for (final Bundle element : bl2) {
          if(element.getBundleId() == bundle.getBundleId() && !pkgClosure.contains(bl[b])) {
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
      final Bundle[] bundles = pkgClosure.toArray(new Bundle[pkgClosure.size()]);
      printBundles(out, bundles, false, true);
    }

    // Service

    final Vector<Bundle> serviceClosure = new Vector<Bundle>();
    final ServiceReference<?>[] srl = bundle.getServicesInUse();
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
      final Bundle[] bundles = serviceClosure.toArray(new Bundle[serviceClosure.size()]);
      printBundles(out, bundles, false, true);
    }

    // Fragment

    final Bundle[] fragmentBundles = packageAdmin.getFragments(bundle);
    if (fragmentBundles == null) {
      out.println("No fragments");
    } else {
      out.println("Fragments:");
      printBundles(out, fragmentBundles, false, true);
    }

    // Host

    final Bundle[] hostBundles = packageAdmin.getHosts(bundle);
    if (hostBundles == null) {
      out.println("No hosts");
    } else {
      out.println("Hosts:");
      printBundles(out, hostBundles, false, true);
    }

    // Required

    final Vector<Bundle> required = new Vector<Bundle>();
    final Vector<Bundle> requiredBy = new Vector<Bundle>();

    try { // untested code
      final RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(null);
      if (requiredBundles != null) {
        for (final RequiredBundle requiredBundle : requiredBundles) {
          final Bundle[] requiringBundles = requiredBundle.getRequiringBundles();
          if (requiringBundles == null) {
            continue;
          }
          if (requiredBundle.getBundle().equals(bundle)) {
            for (final Bundle requiringBundle : requiringBundles) {
              requiredBy.add(requiringBundle);
            }
          } else {
            for (final Bundle requiringBundle : requiringBundles) {
              if (requiringBundle.equals(bundle)) {
                required.add(requiredBundle.getBundle());
              }
            }
          }
        }
      }
    } catch (final Throwable ignored) {}
    if (required.size() == 0) {
      out.println("No required bundles");
    } else {
      out.println("Required bundles:");
      final Bundle[] bundles = required.toArray(new Bundle[required.size()]);
      printBundles(out, bundles, false, true);
    }
    if (requiredBy.size() == 0) {
      out.println("No requiring bundles");
    } else {
      out.println("Requiring bundles:");
      final Bundle[] bundles = requiredBy.toArray(new Bundle[requiredBy.size()]);
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

  public int cmdCondpermission(Dictionary<String,?> opts, Reader in, PrintWriter out,
                               Session session) {
    if (permissionAdminHelper == null) {
      out.println("Conditional Permission Admin service is not available");
      return 1;
    } else {
      return permissionAdminHelper.cmdCondpermission(opts, in, out, session);
    }
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

  public int cmdDeletepermission(Dictionary<String,?> opts, Reader in, PrintWriter out,
                                 Session session) {
    if (permissionAdminHelper == null) {
      out.println("Permission Admin service is not available");
      return 1;
    } else {
      return permissionAdminHelper.cmdDeletepermission(opts, in, out, session);
    }
  }

  //
  // Findbundles command
  //

  public final static String USAGE_FINDBUNDLES = "<symbolic name>";

  public final static String[] HELP_FINDBUNDLES = new String[] {
    "Find bundles with a given symbolic name",
    "<symbolic name>  Symbolic name" };

  public int cmdFindbundles(Dictionary<String,?> opts, Reader in, PrintWriter out,
                            Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    final String symbolicName = (String) opts.get("symbolic name");
    final Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
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

  public int cmdHeaders(Dictionary<String, ?> opts,
                        Reader in,
                        PrintWriter out,
                        Session session)
  {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null);
    final String locale = (String) opts.get("-l");
    boolean found = false;
    for (final Bundle element : b) {
      if (element != null) {
        out.println("Bundle: " + showBundle(element));
        final Dictionary<String, String> d = (locale == null ? element.getHeaders()
            : element.getHeaders(locale));
        for (final Enumeration<String> e = d.keys(); e.hasMoreElements();) {
          final String key = e.nextElement();
          out.println("  " + key + " = " + Util.showObject(d.get(key)));
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

  public int cmdInstall(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    final String[] loc = (String[]) opts.get("location");
    String url = null;
    try {
      for (final String element : loc) {
        url = completeLocation(element);
        final Bundle b = bc.installBundle(url);
        out.println("Installed: " + showBundle(b));
        if (opts.get("-s") != null) {
          b.start(Bundle.START_ACTIVATION_POLICY);
          out.println("Started: " + showBundle(b));
        }
      }
    } catch (final BundleException e) {
      Throwable t = e;
      while (t instanceof BundleException
             && ((BundleException) t).getNestedException() != null) {
        t = ((BundleException) t).getNestedException();
      }
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

  public int cmdMeminfo(Dictionary<String,?> opts, Reader in, PrintWriter out,
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
    final Runtime r = Runtime.getRuntime();
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

  public int cmdPackage(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    final boolean verbose = opts.get("-l") != null;
    ExportedPackage[] epkgs;
    final String[] selection = (String[]) opts.get("selection");
    if (opts.get("-b") != null) {
      final Bundle[] b = getBundles(selection, false);
      epkgs = new ExportedPackage[0];
      for (final Bundle element : b) {
        if (element != null) {
          final ExportedPackage[] e = packageAdmin
            .getExportedPackages(element);
          if (e != null) {
            if (verbose) {
              final ExportedPackage[] ne = new ExportedPackage[e.length
                                                         + epkgs.length];
              System.arraycopy(epkgs, 0, ne, 0, epkgs.length);
              System.arraycopy(e, 0, ne, epkgs.length, e.length);
              epkgs = ne;
            } else {
              out.println("Exported by " + showBundle(element));
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
        final Bundle b = epkgs[i].getExportingBundle();
        if (verbose) {
          out.println();
          out.println("   specification version: "
                      + epkgs[i].getVersion());
          out.println("   removal pending: "
                      + epkgs[i].isRemovalPending());
          out.println("   exporting bundle: " + showBundle(b));
          final Bundle[] ib = epkgs[i].getImportingBundles();
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
  // Capability command
  //

  public final static String USAGE_CAPABILITY = "[-d] [-i] [-l] [-r] [-p] [<bundle>] ...";

  public final static String[] HELP_CAPABILITY = new String[] {
    "Show information about active capablities in the current wiring for a bundle",
    "-d         Show declared capabilities",
    "-i         Sort on bundle id",
    "-l         Long format show all details",
    "-r         Only show required capabilites",
    "-p         Only provided capabilites",
    "<bundle>   The selected bundle" };

  public int cmdCapability(Dictionary<String, ?> opts,
                           Reader in,
                           PrintWriter out,
                           Session session)
  {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null);
    final boolean doRequirements = opts.get("-r")!=null;
    final boolean doProvides = opts.get("-p")!=null;
    final boolean doDetailed = opts.get("-l")!=null;
    final boolean doDeclared = opts.get("-d")!=null;

    boolean found = false;
    for (final Bundle element : b) {
      if (element != null) {
        out.println("Bundle: " + showBundle(element));
        found = true;
        final BundleRevision rev = element.adapt(BundleRevision.class);
        final BundleWiring wiring = rev.getWiring();
        if (!doDeclared && wiring == null) {
          out.println("  Bundle is unresolved, only declared capabilites are available.");
        } else {
          final String prefix = doDeclared ? "  Declared " : "  ";

          if (doRequirements || !doProvides) {
            out.print(prefix);
            out.println("Requirements: ");
            final List<BundleRequirement> reqs = doDeclared
              ? rev.getDeclaredRequirements(null)
              : rev.getWiring().getRequirements(null);
            for (final BundleRequirement req : reqs) {
              out.print("    ");
              out.print(req.getNamespace());
              out.print("  ");
              if (!doDetailed) {
                final String f = req.getDirectives().get(Constants.FILTER_DIRECTIVE);
                out.println(f != null ? f : "NO FILTER");
              } else {
                out.println(req.getDirectives());
              }
            }
          }

          if (doProvides || !doRequirements) {
            out.print(prefix);
            out.println("Capabilites: ");
            final List<BundleCapability> caps = doDeclared
              ? rev.getDeclaredCapabilities(null)
              : rev.getWiring().getCapabilities(null);
            for (final BundleCapability bc : caps) {
              out.print("    ");
              out.print(bc.getNamespace());
              out.print("  ");
              out.print(bc.getAttributes());
              if (doDetailed) {
                out.print(" ");
                out.print(bc.getDirectives());
              }
              out.println();
            }
          }
        }
      }
    }
    if (!found) {
      out.println("ERROR! No matching bundle");
      return 1;
    }
    return 0;
  }

  //
  // Wiring command
  //

  public final static String USAGE_WIRING = "[-i] [-l] [-r] [-p] [<bundle>] ...";

  public final static String[] HELP_WIRING = new String[] {
    "Show information about active wires in the current wiring for a bundle",
    "-i         Sort on bundle id",
    "-l         Long format show all details",
    "-r         Only show wires for required capabilites",
    "-p         Only show wires for provided capabilites",
    "<bundle>   The selected bundle" };

  public int cmdWiring(Dictionary<String, ?> opts,
                       Reader in,
                       PrintWriter out,
                       Session session)
  {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"),
                            opts.get("-i") != null);
    final boolean doRequirements = opts.get("-r")!=null;
    final boolean doProvides = opts.get("-p")!=null;
    final boolean doDetailed = opts.get("-l")!=null;

    boolean found = false;
    for (final Bundle element : b) {
      if (element != null) {
        out.println("Bundle: " + showBundle(element));
        found = true;
        final BundleRevision rev = element.adapt(BundleRevision.class);
        final BundleWiring wiring = rev.getWiring();
        if (wiring == null) {
          out.println("  Bundle is unresolved, no wires are defined.");
        } else {

          if (doRequirements || !doProvides) {
            out.println("  Required Wires: ");
            BundleRequirement prevReq = null;
            for (final BundleWire w : wiring.getRequiredWires(null)) {
              final BundleRequirement req = w.getRequirement();
              final BundleCapability cap = w.getCapability();
              if (!req.equals(prevReq)) {
                out.print("    ");
                out.print(req.getNamespace());
                out.print("  ");
                if (!doDetailed) {
                  final String f = req.getDirectives()
                      .get(Constants.FILTER_DIRECTIVE);
                  out.println(f != null ? f : "NO FILTER");
                } else {
                  out.println(req.getDirectives());
                }
              }
              final BundleRevision provider = w.getProviderWiring().getRevision();
              out.print("      ");
              out.print(showBundle(provider.getBundle()));
              out.print(" - ");
              out.println(cap.getAttributes());
              prevReq = req;
            }
          }

          if (doProvides || !doRequirements) {
            out.println("  Provided Wires: ");
            BundleCapability  prevCap = null;
            for (final BundleWire w : wiring.getProvidedWires(null)) {
              final BundleRequirement req = w.getRequirement();
              final BundleCapability  cap = w.getCapability();

              if (!cap.equals(prevCap)) {
                out.print("    ");
                out.print(cap.getNamespace());
                out.print("  ");
                out.print(cap.getAttributes());
                if (doDetailed) {
                  out.print(" ");
                  out.print(cap.getDirectives());
                }
                out.println();
              }
              final BundleRevision requierer = w.getRequirerWiring().getRevision();
              out.print("      ");
              out.print(showBundle(requierer.getBundle()));
              out.print(" - ");
              out.print(req.getDirectives().get(Constants.FILTER_DIRECTIVE));
              out.println();
              prevCap = cap;
            }
          }
        }
      }
    }
    if (!found) {
      out.println("ERROR! No matching bundle");
      return 1;
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

  public int cmdPermissions(Dictionary<String,?> opts, Reader in, PrintWriter out,
                            Session session) {
    if (permissionAdminHelper == null) {
      out.println("Permission Admin service is not available");
      return 1;
    } else {
      return permissionAdminHelper.cmdPermissions(opts, in, out, session);
    }
  }

  //
  // Refresh command
  //

  public final static String USAGE_REFRESH = "[<bundle>] ...";

  public final static String[] HELP_REFRESH = new String[] {
    "Refresh all exported java packages belong to specified bundle",
    "If no bundle is specified refresh all bundles",
    "<bundle> Name or id of bundle" };

  public int cmdRefresh(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    final String[] bs = (String[]) opts.get("bundle");
    if (bs != null) {
      Bundle[] b = getBundles(bs, true);
      for (int i = 0; i < b.length; i++) {
        if (b[i] == null) {
          final Bundle[] nb = new Bundle[i];
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

  public int cmdResolve(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    if (packageAdmin == null) {
      out.println("Package Admin service is not available");
      return 1;
    }
    final String[] bs = (String[]) opts.get("bundle");
    Bundle[] b = null;
    if (bs != null) {
      b = getBundles(bs, true);
      for (int i = 0; i < b.length; i++) {
        if (b[i] == null) {
          final Bundle[] nb = new Bundle[i];
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

  public int cmdServices(final Dictionary<String,?> opts, Reader in,
                         final PrintWriter out, Session session) {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"), opts
                                  .get("-i") != null);
    final Integer res = AccessController.doPrivileged(new PrivilegedAction<Integer>()
      {
        public Integer run() {
          final boolean useDefaultOper = opts.get("-r") == null
            && opts.get("-u") == null;
          ServiceReference<?>[] fs = null;
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
              fs = bc.getServiceReferences((String)null, filter);
              if (null==fs) {
                out.println("No services matching '"+filter +"'.");
                return new Integer(0);
              }
            } catch (final InvalidSyntaxException ise) {
              out.println("Invalid filter '" +filter +"' found: "
                          +ise.getMessage());
              return new Integer(1);
            }
          }

          for (final Bundle element : b) {
            if (element != null) {
              final String heading = "Bundle: " + showBundle(element);
              boolean headingPrinted = false;
              if (opts.get("-r") != null || useDefaultOper) {
                headingPrinted =
                  showServices(getServicesRegisteredBy(element,fs),
                               out,
                               heading,
                               headingPrinted,
                               "  registered:",
                               opts.get("-l") != null);
              }
              if (opts.get("-u") != null) {
                headingPrinted =
                  showServices(getServicesUsedBy(element,fs),
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

  ServiceReference<?>[] getServicesRegisteredBy(Bundle b,
                                             ServiceReference<?>[] services)
  {
    if (services==null) {
      return b.getRegisteredServices();
    }
    // Filter the given services on registered by.
    final long bid = b.getBundleId();
    int count = 0;
    for (final ServiceReference<?> service : services) {
      if (bid==service.getBundle().getBundleId()) {
        count++;
      }
    }
    if (0==count) {
      return null;
    }
    final ServiceReference<?>[] res = new ServiceReference<?>[count];
    int ix = 0;
    for (final ServiceReference<?> service : services) {
      if (bid==service.getBundle().getBundleId()) {
        res[ix++] = service;
      }
    }
    return res;
  }

  ServiceReference<?>[] getServicesUsedBy(Bundle b,
                                       ServiceReference<?>[] services)
  {
    if (null==services) {
      return b.getServicesInUse();
    }
    // Filter the given services on using bundle.
    final long bid = b.getBundleId();
    int count = 0;
    for (final ServiceReference<?> service : services) {
      final Bundle[] usingBundles = service.getUsingBundles();
      for (int k=0; usingBundles!=null && k<usingBundles.length; k++) {
        if (bid==usingBundles[k].getBundleId()) {
          count++;
          break;
        }
      }
    }
    if (0==count) {
      return null;
    }
    final ServiceReference<?>[] res = new ServiceReference<?>[count];
    int ix = 0;
    for (final ServiceReference<?> service : services) {
      final Bundle[] usingBundles = service.getUsingBundles();
      for (int k=0; usingBundles!=null && k<usingBundles.length; k++) {
        if (bid==usingBundles[k].getBundleId()) {
          res[ix++] = service;
          break;
        }
      }
    }
    return res;
  }

  boolean showServices(final ServiceReference<?>[] services,
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
      for (final ServiceReference<?> service : services) {
        if (null!=service) {
          if (detailed) {
            out.print("\n    ");
            showLongService(service, "    ", out);
          } else {
            out.print(" "+ Util.showServiceClasses(service));
          }
        }
      }
      out.println("");
      return true;
    }
    return false;
  }

  void showLongService(ServiceReference<?> s, String pad, PrintWriter out) {
    out.print(Util.showServiceClasses(s));
    final String[] k = s.getPropertyKeys();
    for (final String element : k) {
      out.print("\n  " + pad + element + " = "
                + Util.showObject(s.getProperty(element)));
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

  public int cmdSetcondpermission(Dictionary<String,?> opts, Reader in, PrintWriter out,
                                  Session session) {
    if (permissionAdminHelper == null) {
      out.println("Conditional Permission Admin service is not available");
      return 1;
    } else {
      return permissionAdminHelper.cmdSetcondpermission(opts, in, out, session);
    }
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

  public int cmdStart(Dictionary<String,?> opts, Reader in, PrintWriter out,
                      Session session) {
    int startOptions = 0;
    if (opts.get("-t") != null) {
      startOptions |= Bundle.START_TRANSIENT;
    }
    if (opts.get("-e") == null) {
      startOptions |= Bundle.START_ACTIVATION_POLICY;
    }

    final Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (final Bundle element : b) {
      if (element != null) {
        try {
          element.start(startOptions);
          out.println("Started: " + showBundle(element));
        } catch (final BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null) {
            t = ((BundleException) t).getNestedException();
          }
          out.println("Couldn't start bundle: " + showBundle(element)
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

  public int cmdStop(Dictionary<String,?> opts, Reader in, PrintWriter out,
                     Session session) {
    int stopOptions = 0;
    if (opts.get("-t") != null) {
      stopOptions |= Bundle.STOP_TRANSIENT;
    }

    final Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (int i = b.length - 1; i >= 0; i--) {
      if (b[i] != null) {
        try {
          b[i].stop(stopOptions);
          out.println("Stopped: " + showBundle(b[i]));
        } catch (final BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null) {
            t = ((BundleException) t).getNestedException();
          }
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

  public int cmdShowstate(Dictionary<String,?> opts, Reader in, PrintWriter out,
                          Session session) {
    final String[] pids = (String[]) opts.get("pid");
    try {
      if (pids != null && pids.length > 0) {
        for (final String pid : pids) {
          showstate(out, bc.getServiceReferences((String)null, "(service.id="
                                                 + pid + ")"));
        }
      } else {
        showstate(out, bc.getServiceReferences((String)null, "(state=*)"));
      }
    } catch (final Exception e) {
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

  public int cmdShutdown(Dictionary<String,?> opts, Reader in, PrintWriter out,
                         Session session) {

    final boolean restart = opts.get("-r") != null;

      final Bundle sysBundle = bc.getBundle(0);
      if (restart) {
        try {
          sysBundle.update(); // restart the framework
        } catch (final Exception e) {
          out.println("Failed to restart the framework " + e);
          return 1;
        }
      } else {
        try {
          sysBundle.stop(); // shut down the framework
        } catch (final Exception e) {
          out.println("Failed to stop using system bundle " + e);
          try {
            System.exit(0);
          } catch (final Exception e2) {
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

  public final static String USAGE_THREADS = "[-a] [-s] [<name>] ...";

  public final static String[] HELP_THREADS = new String[] {
    "Display threads within this framework",
    "-a     List all threads in this JVM",
    "-s     Display stack trace for thread",
    "<name> Names of specific threads, can be a wildcard * at the end" };

  public int cmdThreads(Dictionary<String,?> opts, Reader in, PrintWriter out,
                         Session session) {
    final String [] threadNames = (String [])opts.get("name");
    final boolean showAll = opts.get("-a") != null;
    final boolean showStack = opts.get("-s") != null;
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
      final int acount = tg.activeCount() + 5;
      threads = new Thread[acount];
      count = tg.enumerate(threads);
      if (count < acount) {
        break;
      }
    }
    int groupCols = tg.getName().length();
    boolean sameGroup = true;
    for (int i = 0; i < count; i++) {
      final ThreadGroup itg = threads[i].getThreadGroup();
      if (!tg.equals(itg)) {
        final int cols = itg.getName().length();
        if (groupCols < cols) {
          groupCols = cols;
        }
        sameGroup = false;
      }
    }
    out.print("Pri ");
    if (!sameGroup) {
      final String glabel = "Group                              ";
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
        final StringBuffer sb = new StringBuffer();
        if (threadNames != null) {
          boolean match = false;
          for (final String threadName : threadNames) {
            final String name = threads[i].getName();
            final int last = threadName.length() - 1;
            if (threadName.indexOf('*') == last && last >= 0) {
              if (name.startsWith(threadName.substring(0, last))) {
                match = true;
                break;
              }
            } else if (name.equals(threadName)) {
              match = true;
              break;
            }
          }
          if (!match) {
            continue;
          }
        }
        final int p = threads[i].getPriority();
        if (p < 10) {
          sb.append(' ');
        }
        sb.append(p);
        do {
          sb.append(' ');
        } while (sb.length() < 4);
        if (!sameGroup) {
          final String g = threads[i].getThreadGroup().getName();
          sb.append(g);
          int l = g.length();
          do {
            sb.append(' ');
            l++;
          } while (l < groupCols);
        }
        sb.append(threads[i].getName());
        out.println(sb.toString());
        if (showStack) {
          out.println(printStackTrace(threads[i]));
        }
      } catch (final NullPointerException _ignore) {
        // Handle disappering thread
      }
    }
    return 0;
  }

  private String printStackTrace(Thread t) {
    try {
      final Method m = t.getClass().getMethod("getStackTrace", (Class[]) null);
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      final StackTraceElement [] st = (StackTraceElement [])
        m.invoke(t, (Object[]) null);
      for (final StackTraceElement element : st) {
        pw.println(" >  " + element);
      }
      return sw.toString();
    } catch (final IllegalAccessException _ia) {
      return " ** Failed access StackTrace.";
    } catch (final InvocationTargetException _it) {
      return " ** Failed to get StackTrace.";
    } catch (final NoSuchMethodException _nsm) {
      return " ** java.lang.Thread.getStackTrace() not available.";
    }
  }


  //
  // Uninstall command
  //

  public final static String USAGE_UNINSTALL = "<bundle> ...";

  public final static String[] HELP_UNINSTALL = new String[] {
    "Uninstall one or more bundles", "<bundle> Name or id of bundle" };

  public int cmdUninstall(Dictionary<String,?> opts, Reader in, PrintWriter out,
                          Session session) {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (final Bundle element : b) {
      if (element != null) {
        try {
          element.uninstall();
          out.println("Uninstalled: " + showBundle(element));
        } catch (final BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null) {
            t = ((BundleException) t).getNestedException();
          }
          out.println("Couldn't uninstall: " + showBundle(element)
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

  public int cmdUpdate(Dictionary<String,?> opts, Reader in, PrintWriter out,
                       Session session) {
    final Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
    boolean found = false;
    for (int i = b.length - 1; i >= 0; i--) {
      if (b[i] != null) {
        try {
          b[i].update();
          out.println("Updated: " + showBundle(b[i]));
        } catch (final BundleException e) {
          Throwable t = e;
          while (t instanceof BundleException
                 && ((BundleException) t).getNestedException() != null) {
            t = ((BundleException) t).getNestedException();
          }
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

  public int cmdFromupdate(Dictionary<String,?> opts, Reader in, PrintWriter out,
                           Session session) {
    final String bname = (String) opts.get("bundle");
    final Bundle[] bl = getBundles(new String[] { bname }, true);
    final String fromURL = completeLocation((String) opts.get("url"));

    final Bundle b = bl[0];
    if (b == null) {
      out.println("ERROR! No matching bundle for '" + bname + "'");
      return 1;
    }

    try {
      final URL url = new URL(fromURL);
      final URLConnection conn = url.openConnection();
      final InputStream inStream = conn.getInputStream();
      b.update(inStream);
      out.println("Updated: " + showBundle(b));
    } catch (final BundleException e) {
      Throwable t = e;
      while (t instanceof BundleException
             && ((BundleException) t).getNestedException() != null) {
        t = ((BundleException) t).getNestedException();
      }
      out.println("Couldn't update: " + showBundle(b) + " (due to: " + t
                  + ")");
    } catch (final Exception e) {
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

  public int cmdFrominstall(Dictionary<String,?> opts, Reader in, PrintWriter out,
                            Session session) {
    final String fromURL = completeLocation( (String) opts.get("url"));
    String loc = (String) opts.get("location");

    if (loc == null) {
      loc = fromURL;
    }

    try {
      final URL url = new URL(fromURL);
      final URLConnection conn = url.openConnection();
      final InputStream inStream = conn.getInputStream();
      final Bundle b = bc.installBundle(loc, inStream);
      out.println("Installed: " + showBundle(b));
    } catch (final BundleException e) {
      Throwable t = e;
      while (t instanceof BundleException
             && ((BundleException) t).getNestedException() != null) {
        t = ((BundleException) t).getNestedException();
      }
      out.println("Couldn't install: url=" + fromURL + ", location="
                  + loc + " (due to: " + t + ")");
    } catch (final Exception e) {
      out.println("Couldn't install: url=" + fromURL + ", location="
                  + loc + " (due to: " + e + ")");
    }

    return 0;
  }

  //
  // Private methods
  //

  private void showstate(PrintWriter out, ServiceReference<?>[] srs) {
    if (srs != null) {
      for (final ServiceReference<?> sr : srs) {
        final Object state = sr.getProperty("state");
        if (state != null) {
          out.println("State for " + sr.getProperty("service.id")
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
    final Bundle[] b = bc.getBundles();
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

    for (final int l = x; x > 0;) {
      x = 0;
      int p = Integer.MAX_VALUE;
      p = b[0].adapt(BundleStartLevel.class).getStartLevel();
      for (int i = 1; i < l; i++) {
        int n = Integer.MAX_VALUE;
        n = b[i].adapt(BundleStartLevel.class).getStartLevel();
        if (p > n) {
          x = i - 1;
          final Bundle t = b[x];
          b[x] = b[i];
          b[i] = t;
        } else {
          p = n;
        }
      }
    }
  }

  public String showState(Bundle bundle) {
    final StringBuffer sb = new StringBuffer();

    try {
      final StringBuffer s = new StringBuffer
        (String.valueOf(bundle.adapt(BundleStartLevel.class).getStartLevel()));
      while (s.length() < 2) {
        s.insert(0, " ");
      }
      sb.append(s.toString());
    } catch (final Exception ignored) {
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

  //
  // Set start level command
  //
  public final static String USAGE_STARTLEVEL = "[<level>]";

  public final static String[] HELP_STARTLEVEL = new String[] {
    "Shows or sets the global startlevel", "[<level>] new start level",
    "          if no <level> is provided, show current level", };

  public int cmdStartlevel(Dictionary<String, ?> opts,
                           Reader in,
                           PrintWriter out,
                           Session session)
  {
    final FrameworkStartLevel fsl = bc.getBundle(0)
        .adapt(FrameworkStartLevel.class);
    final String levelStr = (String) opts.get("level");

    try {
      if (levelStr != null) {
        final int level = Integer.parseInt(levelStr);
        fsl.setStartLevel(level);
      } else {
        out.println("current start level:        " + fsl.getStartLevel());
        out.println("initial bundle start level: "
                    + fsl.getInitialBundleStartLevel());
      }
      return 0;
    } catch (final Exception e) {
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

  public int cmdCd(Dictionary<String,?> opts, Reader in, PrintWriter out,
                   Session session) {

    final String[] baseURLsArg = (String[]) opts.get("base URL");
    final boolean append = opts.get("-a") != null;

    try {
      if (opts.get("-reset") != null) {
        setupJars();
      }
      if (baseURLsArg == null) {
        for (final URL url : baseURLs) {
          out.println(" " + url);
        }
      } else {
        setupJars(baseURLsArg, append);
      }
      return 0;
    } catch (final Exception e) {
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

  public int cmdBundlelevel(Dictionary<String, ?> opts,
                            Reader in,
                            PrintWriter out,
                            Session session)
  {
    final FrameworkStartLevel fsl = bc.getBundle(0)
        .adapt(FrameworkStartLevel.class);

    int level = -1;
    try {
      level = Integer.parseInt((String) opts.get("level"));
      final String[] bls = (String[]) opts.get("bundle");
      final Bundle[] bl = getBundles(bls, false, false);

      if (bls == null || bls.length == 0) {
        fsl.setInitialBundleStartLevel(level);
        out.println("initial bundle start level set to " + level);
      } else {
        for (int i = 0; i < bl.length; i++) {
          if (bl[i] != null) {
            System.out.println("set " + i + " " + bl[i] + " " + level);
            bl[i].adapt(BundleStartLevel.class).setStartLevel(level);
          }
        }
      }
      return 0;
    } catch (final Exception e) {
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

  public int cmdProperty(Dictionary<String,?> opts, Reader in, PrintWriter out,
                         Session session)
  {
    final boolean sysProps = opts.get("-s") != null;
    final boolean includeFwPros = opts.get("-f") != null;
    final String[] propNamesA = (String[]) opts.get("property");

    try {
      final Set<String> propNames = new TreeSet<String>();
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

      for (final String key : propNames) {
        final String val = sysProps
          ? (String) System.getProperty(key)
          : (String) bc.getProperty(key);
        if (null!=val) {
          out.println("  " + key + " : " + val);
        }
      }

      return 0;
    } catch (final Exception e) {
      out.println("Failed to print props values: "+e);
      e.printStackTrace(out);
      return -1;
    }
  }
  // The key under which the KF-framework keeps a comma-separated list
  // of all framework property keys.
  public static final String fwPropKeysKey
    = "org.knopflerfish.framework.bundleprops.keys";
  public static final Set<String> FW_PROP_NAMES = new HashSet<String>() {
    private static final long serialVersionUID = 1L;
  {
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
  private Set<String> getAllFrameworkPropKeys()
  {
    final HashSet<String> res = new HashSet<String>();

    // Keys of properties mentioned in the OSGi specification.
    res.addAll(FW_PROP_NAMES);

    // All available keys from a property maintained by the
    // Knopflerfish Framework implementation for this purpose.
    final String fwPropKeys = bc.getProperty(fwPropKeysKey);
    if (null!=fwPropKeys) {
      final StringTokenizer st = new StringTokenizer(fwPropKeys,",");
      while (st.hasMoreTokens()) {
        final String key = st.nextToken().trim();
        res.add(key);
      }
    }
    return res;
  }
  // The set of keys for all System properties
  private Set<String> getAllSystemPropKeys()
  {
    final HashSet<String> res = new HashSet<String>();

    final Properties properties = System.getProperties();
    for (final Enumeration<?> pke = properties.propertyNames(); pke.hasMoreElements();){
      final String key = (String) pke.nextElement();
      res.add(key);
    }
    return res;
  }


}
