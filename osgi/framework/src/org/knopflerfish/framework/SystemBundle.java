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

package org.knopflerfish.framework;

import java.io.*;
import java.util.Dictionary;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.startlevel.StartLevel;

import java.security.ProtectionDomain;

/**
 * Implementation of the System Bundle object.
 *
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 */
public class SystemBundle extends BundleImpl {

  /**
   * Property name for telling which packages framework exports.
   */
  private final static String SYSPKG = "org.osgi.framework.system.packages";

  /**
   * Property name pointing to file of system packages
   */
  private final static String SYSPKG_FILE = "org.osgi.framework.system.packages.file";

  /**
   * Name of system property for exporting all J2SE 1.3 packages.
   */
  private final static String EXPORT13 =
    "org.knopflerfish.framework.system.export.all_13";

  private HeaderDictionary headers = null;

  boolean restarting = false;

  /**
   * Construct a the System Bundle handle.
   *
   * @param bundlesDir Directory where to store the bundles all persistent data.
   * @param fw Framework for this bundle.
   * @param loc Location for new bundle.
   * @param in Bundle JAR as an inputstream.
   * @exception SecurityException If we don't have permission to import and export
   *            bundle packages.
   */
  SystemBundle(Framework fw, ProtectionDomain pd) {
    super(fw, 0, Constants.SYSTEM_BUNDLE_LOCATION, pd);
    state = STARTING;

    StringBuffer sp = new StringBuffer(System.getProperty(SYSPKG, ""));

    if (sp.length() > 0) {
      sp.append(",");
    }

    if("true".equals(System.getProperty(EXPORT13, "").trim())) {
      addSysPackagesFromFile(sp, "packages1.3.txt");
    }

    addSysPackagesFromFile(sp, System.getProperty(SYSPKG_FILE, null));

    addSystemPackages(sp);

    bpkgs = new BundlePackages(this, sp.toString(), null, null);
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
    sp.append(name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION +
              "=" + Framework.SPEC_VERSION);

    // Set up packageadmin package
    name = PackageAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION +
              "=" +  PackageAdminImpl.SPEC_VERSION);

    // Set up permissionadmin package
    name = PermissionAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION +
              "=" +  PermissionAdminImpl.SPEC_VERSION);

    // Set up startlevel package
    name = StartLevel.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION +
              "=" +  StartLevelImpl.SPEC_VERSION);

    // Set up tracker package
    name = ServiceTracker.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION +
              "=" +  "1.2");

    // Set up URL package
    name = org.osgi.service.url.URLStreamHandlerService.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.PACKAGE_SPECIFICATION_VERSION +
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
  public void start() throws BundleException {
    framework.checkAdminPermission();
  }


  /**
   * Stop this bundle.
   *
   * @see org.osgi.framework.Bundle#stop
   */
  synchronized public void stop() throws BundleException {
    framework.checkAdminPermission();
    if (restarting)
      return;
    Main.shutdown(0);
  }


  /**
   * Update this bundle.
   *
   * @see org.osgi.framework.Bundle#update
   */
  synchronized public void update(InputStream in) throws BundleException {
    framework.checkAdminPermission();
    if(Framework.R3_TESTCOMPLIANT || "true".equals(System.getProperty("org.knopflerfish.framework.restart.allow", "true"))) {
      restarting = true;
      try {
        Main.restart();
      } finally {
        restarting = false;
      }
    } else {
      Main.shutdown(2);
    }
  }


  /**
   * Uninstall this bundle.
   *
   * @see org.osgi.framework.Bundle#uninstall
   */
  public void uninstall() throws BundleException {
    framework.checkAdminPermission();
    throw new BundleException("uninstall of System bundle is not allowed");
  }


  /**
   * Get header data. Simulate EXPORT-PACKAGE.
   *
   * @see org.osgi.framework.Bundle#getHeaders
   */
  public Dictionary getHeaders() {
    framework.checkAdminPermission();
    if (headers == null) {
      headers = new HeaderDictionary();
      headers.put(Constants.BUNDLE_NAME, Constants.SYSTEM_BUNDLE_LOCATION);
      headers.put(Constants.EXPORT_PACKAGE, framework.packages.systemPackages());
    }
    return new HeaderDictionary(headers);
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

}
