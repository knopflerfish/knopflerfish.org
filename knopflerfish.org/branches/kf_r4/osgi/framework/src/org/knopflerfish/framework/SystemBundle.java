/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.io.*;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.knopflerfish.framework.permissions.PermissionAdminImpl;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.startlevel.StartLevel;

/**
 * Implementation of the System Bundle object.
 *
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */
public class SystemBundle extends BundleImpl {


  /**
   * Property name pointing to file listing of system-exported packages
   */
  private final static String SYSPKG_FILE = Constants.FRAMEWORK_SYSTEMPACKAGES + ".file";

  /**
   * Name of system property for exporting all J2SE 1.3 packages.
   */
  private final static String EXPORT13 =
    "org.knopflerfish.framework.system.export.all_13";

  /**
   * Name of system property for exporting all J2SE 1.4 packages.
   */
  private final static String EXPORT14 =
    "org.knopflerfish.framework.system.export.all_14";

  /**
   * Name of system property for exporting all J2SE 1.5 packages.
   */
  private final static String EXPORT15 =
    "org.knopflerfish.framework.system.export.all_15";

  /**
   * Export-Package string for system packages
   */
  private final String exportPackageString;


  /**
   * Construct the System Bundle handle.
   *
   */
  SystemBundle(Framework fw, ProtectionDomain pd) {
    super(fw, 0, Constants.SYSTEM_BUNDLE_LOCATION, pd,
          Constants.SYSTEM_BUNDLE_SYMBOLICNAME, new Version(Main.readVersion()));
    state = STARTING;
    StringBuffer sp = new StringBuffer(System.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, ""));
    if (sp.length() > 0) {
      sp.append(",");
    }

    if("true".equals(System.getProperty(EXPORT13, "").trim())) {
      addSysPackagesFromFile(sp, "packages1.3.txt");
    }

    if("true".equals(System.getProperty(EXPORT14, "").trim())) {
      addSysPackagesFromFile(sp, "packages1.4.txt");
    }

    if("true".equals(System.getProperty(EXPORT15, "").trim())) {
      addSysPackagesFromFile(sp, "packages1.5.txt");
    }

    addSysPackagesFromFile(sp, System.getProperty(SYSPKG_FILE, null));

    addSystemPackages(sp);

    exportPackageString = sp.toString();
    bpkgs = new BundlePackages(this, 0, exportPackageString, null, null, null);
    bpkgs.registerPackages();
    bpkgs.resolvePackages();
  }


  /**
   * Add all built-in system packages to a stringbuffer.
   */
  void addSystemPackages(StringBuffer sp) {
    // Set up org.osgi.framework package
    String name = Bundle.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append(name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" + Framework.SPEC_VERSION);

    // Set up packageadmin package
    name = PackageAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  PackageAdminImpl.SPEC_VERSION);

    // Set up permissionadmin package
    name = PermissionAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  PermissionAdminImpl.SPEC_VERSION);

    // Set up startlevel package
    name = StartLevel.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  StartLevelImpl.SPEC_VERSION);

    // Set up tracker package
    name = ServiceTracker.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  "1.3.1");

    // Set up URL package
    name = org.osgi.service.url.URLStreamHandlerService.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  "1.0");
  }


  /**
   * Read a file with package names and add them to a stringbuffer.
   */
  void addSysPackagesFromFile(StringBuffer sp, String sysPkgFile) {

    if(sysPkgFile != null) {
      File f = new File(sysPkgFile);
      if(!f.exists() || !f.isFile()) {
    throw new RuntimeException("System package file '" + sysPkgFile +
                   "' does not exists");
      } else {
    if(Debug.packages) {
      Debug.println("adding system packages from file " + sysPkgFile);
    }
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(sysPkgFile));
      String line;
      for(line = in.readLine(); line != null;
          line = in.readLine()) {
        line = line.trim();
        if(line.length() > 0 && !line.startsWith("#")) {
          sp.append(line);
          sp.append(",");
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read " + sysPkgFile + ": " + e);
    } finally {
      try {   in.close();  } catch (Exception ignored) { }
    }
      }
    }
  }


  public boolean hasPermission(Object permission) {
    // we have them all
    return true;
  }

  //
  // Bundle interface
  //

  /**
   * Start this bundle.
   *
   * @see org.osgi.framework.Bundle#start
   */
  synchronized public void start() throws BundleException
  {
    secure.checkExecuteAdminPerm(this);
  }


  /**
   * Stop this bundle.
   *
   * @see org.osgi.framework.Bundle#stop
   */
  public void stop() throws BundleException {
    stop(0);    
  }


  synchronized public void stop(int exitcode) throws BundleException {
    secure.checkExecuteAdminPerm(this);
    secure.callMainShutdown(exitcode);
  }


  /**
   * Update this bundle.
   *
   * @see org.osgi.framework.Bundle#update
   */
  synchronized public void update(InputStream in) throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    secure.callMainRestart();
  }


  /**
   * Uninstall this bundle.
   *
   * @see org.osgi.framework.Bundle#uninstall
   */
  synchronized public void uninstall() throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    throw new BundleException("uninstall of System bundle is not allowed");
  }


  /**
   * Get header data. Simulate EXPORT-PACKAGE.
   *
   * @see org.osgi.framework.Bundle#getHeaders
   */
  public Dictionary getHeaders() {
    return getHeaders(null);
  }

  public Dictionary getHeaders(String locale) {
    secure.checkMetadataAdminPerm(this);
    Hashtable headers = new Hashtable();
    headers.put(Constants.BUNDLE_NAME, Constants.SYSTEM_BUNDLE_LOCATION);
    headers.put(Constants.EXPORT_PACKAGE, exportPackageString);
    return headers;
  }


  /**
   * Get bundle data. Get resources from bundle or fragment jars.
   *
   * @see org.osgi.framework.Bundle#findEntries
   */
  public Enumeration findEntries(String path, String filePattern, boolean recurse) {
    return null;
  }


  /**
   *
   */
  public URL getEntry(String name) {
    return null;
  }


  /**
   *
   */
  public Enumeration getEntryPaths(String path) {
    return null;
  }


  //
  // Package method
  //

  /**
   * Get class loader for this bundle.
   */
  ClassLoader getClassLoader() {
    return getClass().getClassLoader();
  }

  void setBundleContext(BundleContextImpl bc) {
    bundleContext = bc;
  }

  /**
   * Set system bundle state to active
   */
  void systemActive() {
    state = ACTIVE;
  }


  /**
   * Set system bundle state to stopping
   */
  void systemShuttingdown() {
    state = STOPPING;
  }


  /**
   * Checks whether a path is included in the path.
   */
  private boolean isInClassPath(BundleImpl extension) {
    String cps = extension.isBootClassPathExtension() ? 
      "sun.boot.class.path" : "java.class.path";
    String cp = System.getProperty(cps);
    String[] scp = Util.splitwords(cp, ":");
    String path = extension.archive.getJarLocation();

    for (int i = 0; i < scp.length; i++) {
      if (scp[i].equals(path)) {
        return true;
      }
    }

    return false;
  }


  /**
   * Adds an bundle as an extension that will be included
   * in the boot class path on restart.
   */
  void attachFragment(BundleImpl extension) {
    // NYI! Plugin VM specific functionality, dynamic classpath additions
    if (isInClassPath(extension)) {
      super.attachFragment(extension);
    } else {
      throw new UnsupportedOperationException("Extension can not be dynamicly activated");
    }
  }


  /**
   * Reads all localization entries that affects this bundle 
   * (including its host/fragments)
   * @param locale locale == "" the bundle.properties will be read
   *               o/w it will read the files as described in the spec.
   * @param localization_entries will append the new entries to this dictionary
   */
  protected void readLocalization(String locale, Hashtable localization_entries) {

    String[] parts = Util.splitwords(locale, "_");
    String tmploc = parts[0];
    int o = 0;
    
    do {
      if (fragments != null) {
        for (int i = fragments.size() - 1; i >= 0; i--) {
          BundleImpl b = (BundleImpl)fragments.get(i);
          // NYI! Get correct archive
          Hashtable tmp = b.archive.getLocalizationEntries(tmploc);
          if (tmp != null) {
            localization_entries.putAll(tmp);
          }
        }
      }
      // NYI! read localization from framework?
      
      if (++o >= parts.length) {
        break;
      }
      tmploc = tmploc + "_" + parts[o];
      
    } while (true);
  }
}
