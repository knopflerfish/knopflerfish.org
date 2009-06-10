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

package org.knopflerfish.framework;

import java.io.*;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.knopflerfish.framework.permissions.PermissionAdminImpl;
import org.knopflerfish.framework.permissions.ConditionalPermissionAdminImpl;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.framework.launch.Framework;

/**
 * Implementation of the System Bundle object.
 *
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */
public class SystemBundle extends BundleImpl implements Framework {


  /**
   * Property name pointing to file listing of system-exported packages
   */
  private final static String SYSPKG_FILE
    = Constants.FRAMEWORK_SYSTEMPACKAGES + ".file";


  /**
   * Name of system property for exporting all packages for according
   * to version of the running JRE.
   */
  private final static String EXPORT_ALL_CURRENT =
    "org.knopflerfish.framework.system.export.all";

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
   * Name of system property for exporting all J2SE 1.6 packages.
   */
  private final static String EXPORT16 =
    "org.knopflerfish.framework.system.export.all_16";

  /**
   * Export-Package string for system packages
   */
  private String exportPackageString;

  /**
   * The event to return to callers waiting in Framework.waitForStop()
   * when the framework has been stopped.
   */
  FrameworkEvent stopEvent;


  /**
   * Construct the System Bundle handle.
   *
   */
  SystemBundle(FrameworkContext fw)
  {
    super(fw,
          0,
          Constants.SYSTEM_BUNDLE_LOCATION,
          Constants.SYSTEM_BUNDLE_SYMBOLICNAME,
          new Version(Main.readVersion()));
  }

  public void init() throws BundleException {
    if (fwCtx.initialized) return; // Already done.

    state = STARTING;
    stopEvent = null;

    fwCtx.init();

    StringBuffer sp = new StringBuffer
      (fwCtx.props.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, ""));
    if (sp.length() > 0) {
      sp.append(",");
    }

    if ("true".equals(fwCtx.props.getProperty(EXPORT_ALL_CURRENT, "").trim())) {
      String jv = fwCtx.props.getProperty("java.version", null);
      if (null!=jv) { // Extract <M>.<N> part of the version string
        int end = jv.indexOf('.');
        if (end>-1) {
          end = jv.indexOf('.',end+1);
        }
        if (end>-1) {
          addSysPackagesFromFile(sp, "packages"+ jv.substring(0,end)+".txt");
        }
      }
    } else {

      if("true".equals(fwCtx.props.getProperty(EXPORT13, "").trim())) {
        addSysPackagesFromFile(sp, "packages1.3.txt");
      }

      if("true".equals(fwCtx.props.getProperty(EXPORT14, "").trim())) {
        addSysPackagesFromFile(sp, "packages1.4.txt");
      }

      if("true".equals(fwCtx.props.getProperty(EXPORT15, "").trim())) {
        addSysPackagesFromFile(sp, "packages1.5.txt");
      }

      if("true".equals(fwCtx.props.getProperty(EXPORT16, "").trim())) {
        addSysPackagesFromFile(sp, "packages1.6.txt");
      }

    }

    addSysPackagesFromFile(sp, fwCtx.props.getProperty(SYSPKG_FILE, null));
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
          "=" + FrameworkContext.SPEC_VERSION);

    sp.append(",org.osgi.framework.launch;" + Constants.VERSION_ATTRIBUTE + "=" + FrameworkContext.LAUNCH_VERSION);
    sp.append(",org.osgi.framework.hooks.service;" + Constants.VERSION_ATTRIBUTE + "=" + FrameworkContext.HOOKS_VERSION);

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

    // Set up conditionalpermissionadmin package
    name = ConditionalPermissionAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  ConditionalPermissionAdminImpl.SPEC_VERSION);

    // Set up startlevel package
    name = StartLevel.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  StartLevelController.SPEC_VERSION);

    // Set up tracker package
    name = ServiceTracker.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  "1.4");

    // Set up URL package
    name = org.osgi.service.url.URLStreamHandlerService.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE +
          "=" +  "1.0");
  }


  /**
   * Read a file with package names and add them to a stringbuffer.
   * The file is searched for in the current working directory, then
   * on the class path.
   * @param sp Buffer to append the exports to. Same format as the
   *           Export-Package manifest header.
   * @param sysPkgFile Name of the file to load packages to be
   *           exported from.
   */
  void addSysPackagesFromFile(StringBuffer sp, String sysPkgFile) {
    if (null==sysPkgFile || 0==sysPkgFile.length() ) return;

    if(fwCtx.props.debug.packages) {
      fwCtx.props.debug.println("Will add system packages from file " + sysPkgFile);
    }

    URL  url = null;
    File f = new File(sysPkgFile);

    if(!f.exists() || !f.isFile()) {
      url = SystemBundle.class.getResource(sysPkgFile);
      if (null==url) {
        url = SystemBundle.class.getResource("/" +sysPkgFile);
      }
      if (null==url) {
        fwCtx.props.debug.println("Could not add system bundle package exports from '"
                      + sysPkgFile +"', file not found.");
      }
    }
    BufferedReader in = null;
    try {
      Reader reader = null;
      String source = null;

      if (null==url) {
        reader = new FileReader(f);
        source = f.getAbsolutePath().toString();
      } else {
        reader = new InputStreamReader(url.openStream());
        source = url.toString();
      }
      in = new BufferedReader(reader);
      if(fwCtx.props.debug.packages) {
        fwCtx.props.debug.println("\treading from " +source);
      }

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
      throw new IllegalArgumentException("Failed to read " +sysPkgFile +": " +e);
    } finally {
      try {   in.close();  } catch (Exception ignored) { }
    }
  }


  public boolean hasPermission(Object permission) {
    // we have them all
    return true;
  }


  //
  // Bundle interface overrides
  //

  synchronized public void start(int options) throws BundleException {
    if (null==secure) init();
    secure.checkExecuteAdminPerm(this);
    fwCtx.launch();
  }


  Object waitForStopLock = new Object();
  public FrameworkEvent waitForStop(long timeout)
    throws InterruptedException
  {
    // Already stopped?
    if (RESOLVED==state) return stopEvent;

    synchronized (waitForStopLock) {
      waitForStopLock.wait(timeout);
    }
    return RESOLVED==state ? stopEvent
      : new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this, null);
  }


  public void stop(int options) throws BundleException {
    secure.checkExecuteAdminPerm(this);

    if (STARTING==state || ACTIVE==state) {
      // There is some work to do.
      if (null==stopEvent) {
        stopEvent = new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
      }
      fwCtx.shutdown();
    }
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
    headers.put(Constants.BUNDLE_SYMBOLICNAME,
                Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
    headers.put(Constants.BUNDLE_NAME, Constants.SYSTEM_BUNDLE_LOCATION);
    headers.put(Constants.EXPORT_PACKAGE, exportPackageString);
    headers.put(Constants.BUNDLE_VERSION, Main.readRelease());

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
    this.bundleContext = bc;
  }

  void setPermissionOps(PermissionOps permissionOps) {
    this.secure = permissionOps;
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
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));
  }

  /**
   * Shutting down is done.
   */
  void systemShuttingdownDone() {
    state = RESOLVED;
    synchronized(waitForStopLock) {
      waitForStopLock.notifyAll();
    }
  }


  /**
   * Checks whether a path is included in the path.
   */
  private boolean isInClassPath(BundleImpl extension) {
    String cps = extension.isBootClassPathExtension() ?
      "sun.boot.class.path" : "java.class.path";
    String cp = fwCtx.props.getProperty(cps);
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
   * @param baseName the basename for localization properties,
   *        <code>null</code> will choose OSGi default
   */
  protected void readLocalization(String locale,
                                  Hashtable localization_entries,
                                  String baseName) {
    String[] parts = Util.splitwords(locale, "_");
    String tmploc;
    int o = 0;

    if (baseName == null) {
      baseName = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
    }
    if ("".equals(parts[0])) {
      tmploc = baseName;
    } else {
      tmploc = baseName + "_" + parts[0];
    }
    do {
      if (fragments != null) {
        for (int i = fragments.size() - 1; i >= 0; i--) {
          BundleImpl b = (BundleImpl)fragments.get(i);
          Hashtable tmp = b.archive.getLocalizationEntries(tmploc + ".properties");
          if (tmp != null) {
            localization_entries.putAll(tmp);
          }
        }
      }
      // NYI! read localization from framework.
      // There is no need for this now since it isn't used.

      if (++o >= parts.length) {
        break;
      }
      tmploc = tmploc + "_" + parts[o];

    } while (true);
  }
}
