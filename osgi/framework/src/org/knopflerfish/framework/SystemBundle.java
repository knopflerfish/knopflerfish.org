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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

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
   * Export-Package string for system packages
   */
  private final String exportPackageString;

  /**
   * The file where we store the class path
   */
  private final static String CLASSPATH_FILE = "system.classpath";


  /**
   * All current extensions
   */
  private ArrayList extensions = new ArrayList();



  /**
   * Construct the System Bundle handle.
   *
   */
  SystemBundle(Framework fw) {
    super(fw, 0, Constants.SYSTEM_BUNDLE_LOCATION, Constants.SYSTEM_BUNDLE_SYMBOLICNAME,
	  new Version(Main.readVersion()));
    state = STARTING;
    StringBuffer sp = new StringBuffer(System.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, ""));

    if (sp.length() > 0) {
      sp.append(",");
    }

    if("true".equals(System.getProperty(EXPORT13, "").trim())) {
      addSysPackagesFromFile(sp, "packages1.3.txt");
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
          "=" +  "1.3");

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
    checkExecuteAdminPerm();
  }


  /**
   * Stop this bundle.
   *
   * @see org.osgi.framework.Bundle#stop
   */
  public void stop() throws BundleException {
    stop(0);    
  }


  synchronized public void stop(int exitcode) throws BundleException
  {
    checkExecuteAdminPerm();

    StringBuffer classpath = new StringBuffer();
    for (Iterator iter = extensions.iterator(); iter.hasNext(); ) {
      BundleImpl eb = (BundleImpl)iter.next();

      // write pending updates.. TODO: refactor.
      if (eb.fragment.pendingUpdate != null) {
        try {
          framework.storage.replaceBundleArchive(eb.archive, eb.fragment.pendingUpdate);
          // framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UPDATED, eb));
        } catch (Exception _e) { /* TBD! What to do */ }
        eb.archive = eb.fragment.pendingUpdate;
        eb.fragment.pendingUpdate = null;
      }
      // end refactor

      String path = eb.archive.getJarLocation();
      
      classpath.append(path);
      if (iter.hasNext()) {
        classpath.append(":");
      }
    }

    classpath.append("\n");

    try {
      FileTree storage = Util.getFileStorage("classpath");
      Util.putContent(new File(storage, CLASSPATH_FILE), classpath.toString(), false);
    } catch (IOException e) {
      throw new BundleException("Could not save classpath " + e);
    } finally {
      // probably need to add security here.
      if (Main.bootMgr != 0) {
        try {
          framework.stopBundle(Main.bootMgr);
        } catch (BundleException e) {
          System.err.println("Stop of BootManager failed, " +
                             e.getNestedException());
        }
      }
      Main.shutdown(exitcode);
    }
  }


  /**
   * Update this bundle.
   *
   * @see org.osgi.framework.Bundle#update
   */
  synchronized public void update(InputStream in) throws BundleException {
    checkLifecycleAdminPerm();
    if(Framework.R3_TESTCOMPLIANT || "true".equals(System.getProperty("org.knopflerfish.framework.restart.allow", "true"))) {
      Main.restart();
    } else {
      Main.shutdown(2);
    }
  }


  /**
   * Uninstall this bundle.
   *
   * @see org.osgi.framework.Bundle#uninstall
   */
  synchronized public void uninstall() throws BundleException {
    checkLifecycleAdminPerm();
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
    checkMetadataAdminPerm();
    Hashtable headers = new Hashtable();
    headers.put(Constants.BUNDLE_NAME, Constants.SYSTEM_BUNDLE_LOCATION);
    headers.put(Constants.EXPORT_PACKAGE, exportPackageString);
    return headers;
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
  private boolean isInClassPath(String path) {
    String cp = System.getProperty("java.class.path");
    String[] cps = cp.split(":");
    
    for (int i = 0; i < cps.length; i++) {
      if (cps[i].equals(path)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Adds an bundle as an extension that will be included
   * in the boot class path on restart.
   */
  void addExtension(BundleImpl extension) {

    if (!Framework.SUPPORTS_EXTENSION_BUNDLES) {
      if (Framework.bIsMemoryStorage) {
        throw new UnsupportedOperationException("Extension bundles are not supported in memory storage mode.");
      } else if (!Framework.EXIT_ON_SHUTDOWN) {
        throw new UnsupportedOperationException("Extension bundles require that the property " +
                                                Main.EXITONSHUTDOWN_PROP + " is set to \"true\"");
        
      } else if (!Framework.USING_WRAPPER_SCRIPT) {
        throw new UnsupportedOperationException("Extension bundles require the use of a wrapper script. " +
                                                "Consult the documentation");
        
      } else {
        throw new UnsupportedOperationException("Extension bundles are not supported.");
      }
    }  
    

    int i = 0;
    for (int n = extensions.size(); i < n; i++) {
      BundleImpl tmp = (BundleImpl)extensions.get(i);
      if (tmp.id > extension.id) {
        break;
      }
    }
        
    extensions.add(i, extension);
    if (isInClassPath(extension.archive.getJarLocation())) {
      extension.state = RESOLVED;
      framework.listeners.bundleChanged(new BundleEvent(BundleEvent.RESOLVED, extension));
    }
  }

  /**
   * Removes an extension. This bundle will not be in the
   * framework's class path on next start up.
   */
  void removeExtension(BundleImpl extension) {
    extensions.remove(extension);
  }

}
