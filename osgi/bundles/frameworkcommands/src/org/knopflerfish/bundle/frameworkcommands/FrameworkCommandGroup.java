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
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.service.startlevel.StartLevel;

// ******************** FrameworkCommandGroup ********************
/**
 * * Interface for commands to be handled by the console. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.1.1.1 $
 */

public class FrameworkCommandGroup extends CommandGroupAdapter {

    BundleContext bc;

    private PackageAdmin packageAdmin = null;

    private PermissionAdmin permissionAdmin = null;

    private StartLevel startLevel = null;

    /**
     * * The default directories for bundle jar files. *
     * <p> * The system property <code>org.knopflerfish.gosg.jars</code> holds
     * a * semicolon separated path of URLs that is used to complete the *
     * location when it is given as a partial URL. *
     * </p>
     */
    private String[] bundleDirs = null;

    FrameworkCommandGroup(BundleContext bc) {
        super("framework", "Framework commands");
        this.bc = bc;

        // all of these services are framework singleton internal services
        // thus, we take a shortcut and skip the service tracking

        ServiceReference sr = bc.getServiceReference(PackageAdmin.class
                .getName());
        if (sr != null) {
            packageAdmin = (PackageAdmin) bc.getService(sr);
        }

        sr = bc.getServiceReference(PermissionAdmin.class.getName());
        if (sr != null) {
            permissionAdmin = (PermissionAdmin) bc.getService(sr);
        }

        sr = bc.getServiceReference(StartLevel.class.getName());
        if (sr != null) {
            startLevel = (StartLevel) bc.getService(sr);
        }

        setupJars();
    }

    void setupJars() {
        String jars = System.getProperty("org.knopflerfish.gosg.jars",
                "file:./");
        StringTokenizer st = new StringTokenizer(jars, ";");
        bundleDirs = new String[st.countTokens()];
        for (int i = 0; i < bundleDirs.length; i++) {
            String path = st.nextToken();
            try {
                bundleDirs[i] = new URL(path).toString();
            } catch (Exception e) {
                bundleDirs[i] = path;
            }
        }
    }

    /**
     * Completes a partial bundle location using the bundles dir path. * The
     * result is the first combination of a directory URL (as * returned by
     * <code>getBundleDirs()</code>) and the specified * location that
     * results in a valid URL with accessible data.
     */
    public String completeLocation(String location) {
        int ic = location.indexOf(":");
        if (ic < 2 || ic > location.indexOf("/")) {
            // URL wihtout protocol complete it.
            String[] paths = bundleDirs;
            for (int i = 0; i < paths.length; i++) {
                try {
                    URL url = new URL(new URL(paths[i]), location);
                    if ("file".equals(url.getProtocol())) {
                        File f = new File(url.getFile());
                        if (!f.exists() || !f.canRead()) {
                            continue; // Noope; try next.
                        }
                    } else if ("http".equals(url.getProtocol())) {
                        HttpURLConnection uc = (HttpURLConnection) url
                                .openConnection();
                        uc.connect();
                        int rc = uc.getResponseCode();
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

    public final static String USAGE_ADDPERMISSION = "-b #bundle# | -d | -l #location# <type> <name> <actions>";

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
        String perm = "(" + opts.get("type") + " \"" + opts.get("name")
                + "\" \"" + opts.get("actions") + "\")";
        PermissionInfo pia;
        try {
            pia = new PermissionInfo(perm);
        } catch (IllegalArgumentException e) {
            out.println("ERROR! " + e.getMessage());
            out.println("PermissionInfo string = " + perm);
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

    public final static String USAGE_BUNDLES = "[-1] [-i] [-l] [-s] [<bundle>] ...";

    public final static String[] HELP_BUNDLES = new String[] { "List bundles",
            "-1       One column output", "-i       Sort on bundle id",
            "-s       Sort on bundle start level", "-l       Verbose output",
            "<bundle> Name or id of bundle" };

    public int cmdBundles(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        Bundle[] b = getBundles((String[]) opts.get("bundle"),
                opts.get("-i") != null, opts.get("-s") != null);
        boolean needNl = false;
        // .println("12 5/active CM Commands 2 1/active CM Service");
        if (opts.get("-l") != null) {
            out.println("   id  level/state location");
        } else {
            out.println("   id  level/state name");
        }
        out.println("   --------------------");
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
            if (opts.get("-l") != null) {
                out.println(Util.showId(b[i]) + showState(b[i])
                        + b[i].getLocation());
            } else {
                if ((i & 1) == 0 && opts.get("-1") == null) {
                    String s = Util.showId(b[i]) + showState(b[i])
                            + Util.shortName(b[i]);
                    out.print(s);
                    int l = 40 - s.length();
                    if (l > 0) {
                        String blank = "                                    ";
                        out.print(blank.substring(blank.length() - l));
                    }
                    needNl = true;
                } else {
                    out.println(Util.showId(b[i]) + showState(b[i])
                            + Util.shortName(b[i]));
                    needNl = false;
                }
            }
        }
        if (needNl) {
            out.println("");
        }
        return 0;
    }

    //
    // Call command
    //

    public final static String USAGE_CALL = "<interface> <method> [<args>] ...";

    public final static String[] HELP_CALL = new String[] {
            "Call a method with zero or more java.lang.String",
            "arguments in a registered service.",
            "<interface> Service interface",
            "<method>    Method in service to call",
            "<args>      Arguments to method. If arguments",
            "            are on the form \"value::type\", the value",
            "            will be attempted to created as the",
            "            specified type", };

    public int cmdCall(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int res = 1;
        final String si = (String) opts.get("interface");
        final ServiceReference sr = (ServiceReference) AccessController
                .doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return bc.getServiceReference(si);
                    }
                });
        if (sr == null) {
            out.println("No such service reference class: " + si);
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
            Method m = findMethod(s.getClass(), method, args.length);

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

    Method findMethod(Class clazz, String name, int nArgs) {
        Method[] methods = clazz.getDeclaredMethods();
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
    // Deletepermission command
    //

    public final static String USAGE_DELETEPERMISSION = "[-r] -b #bundle# | -d | -l #location# <type> <name> <actions>";

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
    // Headers command
    //

    public final static String USAGE_HEADERS = "[-i] <bundle> ...";

    public final static String[] HELP_HEADERS = new String[] {
            "Show bundle header values", "-i       Sort on bundle id",
            "<bundle> Name or id of bundle" };

    public int cmdHeaders(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        Bundle[] b = getBundles((String[]) opts.get("bundle"),
                opts.get("-i") != null);
        boolean found = false;
        for (int i = 0; i < b.length; i++) {
            if (b[i] != null) {
                out.println("Bundle: " + showBundle(b[i]));
                Dictionary d = b[i].getHeaders();
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
            "Install one or more bundles", "-s         Start bundle(s)",
            "<location> Name or id of bundle" };

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
                    b.start();
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
                epkgs = packageAdmin.getExportedPackages(null);
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
                            + epkgs[i].getSpecificationVersion());
                    out.println("   removal pending: "
                            + epkgs[i].isRemovalPending());
                    out.println("   exporting bundle: " + showBundle(b));
                    Bundle[] ib = epkgs[i].getImportingBundles();
                    if (ib.length > 0) {
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
    // Services command
    //

    public final static String USAGE_SERVICES = "[-i] [-l] [-r] [-u] [<bundle>] ...";

    public final static String[] HELP_SERVICES = new String[] {
            "List registered services", "-i       Sort on bundle id",
            "-l       Verbose output",
            "-r       Show services registered by named bundles (default)",
            "-u       Show services used by named bundles",
            "<bundle> Name or id of bundle" };

    public int cmdServices(final Dictionary opts, Reader in,
            final PrintWriter out, Session session) {
        final Bundle[] b = getBundles((String[]) opts.get("bundle"), opts
                .get("-i") != null);
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                for (int i = 0; i < b.length; i++) {
                    if (b[i] != null) {
                        out.println("Bundle: " + showBundle(b[i]));
                        if (opts.get("-r") != null || opts.get("-u") == null) {
                            ServiceReference[] s = b[i].getRegisteredServices();
                            if (s != null && s.length > 0) {
                                out.print("  registered:");
                                for (int j = 0; j < s.length; j++) {
                                    if (opts.get("-l") != null) {
                                        out.print("\n    ");
                                        showLongService(s[j], "    ", out);
                                    } else {
                                        out.print(" "+ Util.showServiceClasses(s[j]));
                                    }
                                }
                                out.println("");
                            }
                        }
                        if (opts.get("-u") != null) {
                            ServiceReference[] s = b[i].getServicesInUse();
                            if (s != null && s.length > 0) {
                                out.print("  uses:");
                                for (int j = 0; j < s.length; j++) {
                                    if (opts.get("-l") != null) {
                                        out.print("\n    ");
                                        showLongService(s[j], "    ", out);
                                    } else {
                                        out.print(" "+ Util.showServiceClasses(s[j]));
                                    }
                                }
                            }
                            out.println("");
                        }
                    }
                }
                return null;
            }
        });
        return 0;
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
    // Start command
    //

    public final static String USAGE_START = "<bundle> ...";

    public final static String[] HELP_START = new String[] {
            "Start one or more bundles", "<bundle> Name or id of bundle" };

    public int cmdStart(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
        boolean found = false;
        for (int i = 0; i < b.length; i++) {
            if (b[i] != null) {
                try {
                    b[i].start();
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

    public final static String USAGE_STOP = "<bundle> ...";

    public final static String[] HELP_STOP = new String[] {
            "Stop one or more bundles", "<bundle> Name or id of bundle" };

    public int cmdStop(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        Bundle[] b = getBundles((String[]) opts.get("bundle"), true);
        boolean found = false;
        for (int i = b.length - 1; i >= 0; i--) {
            if (b[i] != null) {
                try {
                    b[i].stop();
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

    public final static String USAGE_SHUTDOWN = "[<exit code>]";

    public final static String[] HELP_SHUTDOWN = new String[] {
            "Shutdown framework", "<exit code> Exit code for JVM" };

    public int cmdShutdown(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        
        String c = (String) opts.get("exit code"); 
        int ec = 0; 
        if (c != null) {
        	try { 
        		ec = Integer.parseInt(c); 
        	} 
        	catch (NumberFormatException e) {
        		out.println("Illegal exit code: must be an integer"); 
        		return 1; 
        	} 
        }
         
        try {
            Bundle sysBundle = bc.getBundle(0);
            sysBundle.stop();
        } catch (Exception e) {
            out.println("Failed to stop using system bundle");
            try {
                System.exit(0);
            } catch (Exception e2) {
                out.println("Failed to exit using system exit " + e2);
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
            "Note: Use refresh command to force the framework to do a package update",
            "of exported packages used by running bundles." };

    public int cmdFromupdate(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        String bname = (String) opts.get("bundle");
        Bundle[] bl = getBundles(new String[] { bname }, true);
        String fromURL = (String) opts.get("url");

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

        out
                .println("Note: Use refresh command to update exported packages in running bundles");
        return 0;
    }

    public final static String USAGE_FROMINSTALL = "<url> [<location>]";

    public final static String[] HELP_FROMINSTALL = new String[] {
            "Install a bundle with a specific location from an URL",
            "<url>      - URL to bundle jar file",
            "<location> - Optional location string to use for installation", };

    public int cmdFrominstall(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        String fromURL = (String) opts.get("url");
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
        return getBundles(selection, sortNumeric, false);
    }

    private Bundle[] getBundles(String[] selection, boolean sortNumeric,
            boolean sortStartLevel) {
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
            StringBuffer s = new StringBuffer(Integer.toString(startLevel
                    .getBundleStartLevel(bundle)));
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
    public final static String USAGE_CD = "[-reset] [<prefix>] ...";

    public final static String[] HELP_CD = new String[] {
            "Shows or sets URL prefix",
            "[-reset]       reset prefix list to value startup value",
            "[<prefix>] ... list of URL prefixes for install command", };

    public int cmdCd(Dictionary opts, Reader in, PrintWriter out,
            Session session) {

        String[] prefixes = (String[]) opts.get("prefix");

        try {
            if (opts.get("-reset") != null) {
                setupJars();
            }
            if (prefixes == null) {
                for (int i = 0; i < bundleDirs.length; i++) {
                    out.println(" " + bundleDirs[i]);
                }
            } else {
                bundleDirs = prefixes;
            }
            return 0;
        } catch (Exception e) {
            out.println("Failed to cd");
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

}
