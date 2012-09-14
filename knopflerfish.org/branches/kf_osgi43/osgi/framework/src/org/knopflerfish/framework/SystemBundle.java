/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

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
   * The file where we store the class path
   */
  private final static String BOOT_CLASSPATH_FILE = "boot_cp";

  /**
   * Export-Package string for system packages
   */
  private String exportPackageString;

  /**
   * The event to return to callers waiting in Framework.waitForStop() when the
   * framework has been stopped.
   */
  volatile private FrameworkEvent stopEvent = null;

  /**
   * The thread that performs shutdown of this framework instance.
   */
  private Thread shutdownThread = null;

  /**
   * Lock object
   */
  private Object lock = new Object();

  /**
   * Marker that we need to restart JVM.
   */
  boolean bootClassPathHasChanged;


  /**
   * Construct the System Bundle handle.
   * 
   */
  SystemBundle(FrameworkContext fw) {
    super(fw);
  }


  /**
   * Initialize this framework.
   * 
   * @see org.osgi.framework.Framework#init
   */
  public void init() throws BundleException {
    secure.checkExecuteAdminPerm(this);

    synchronized (lock) {
      waitOnOperation(lock, "Framework.init", true);

      switch (state) {
      case INSTALLED:
      case RESOLVED:
        break;
      case STARTING:
      case ACTIVE:
        return;
      default:
        throw new IllegalStateException("INTERNAL ERROR, Illegal state, " + state);
      }
      doInit();
    }
  }


  /**
   * Start this framework.
   * 
   * @see org.osgi.framework.Framework#start
   */
  public void start(int options) throws BundleException {
    List bundlesToStart = null;
    synchronized (lock) {
      waitOnOperation(lock, "Framework.start", true);

      switch (state) {
      case INSTALLED:
      case RESOLVED:
        doInit();
        // Fall through
      case STARTING:
        operation = ACTIVATING;
        break;
      case ACTIVE:
        return;
      default:
        throw new IllegalStateException("INTERNAL ERROR, Illegal state, " + state);
      }
      if (fwCtx.startLevelController == null) {
        bundlesToStart = fwCtx.storage.getStartOnLaunchBundles();
      }
    }

    if (fwCtx.startLevelController != null) {
      // start level open is delayed to this point to
      // correctly work at restart
      fwCtx.startLevelController.open();
    } else {
      // Start bundles according to their autostart setting.
      final Iterator i = bundlesToStart.iterator();
      while (i.hasNext()) {
        final BundleImpl b = (BundleImpl)fwCtx.bundles.getBundle((String)i.next());
        try {
          final int autostartSetting = b.gen.archive.getAutostartSetting();
          // Launch must not change the autostart setting of a bundle
          int option = Bundle.START_TRANSIENT;
          if (Bundle.START_ACTIVATION_POLICY == autostartSetting) {
            // Transient start according to the bundles activation policy.
            option |= Bundle.START_ACTIVATION_POLICY;
          }
          b.start(option);
        } catch (BundleException be) {
          fwCtx.listeners.frameworkError(b, be);
        }
      }
    }
    synchronized (lock) {
      state = ACTIVE;
      operation = IDLE;
      lock.notifyAll();
      fwCtx.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, this, null));
    }
  }


  /**
   *
   */
  public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
    synchronized (lock) {
      // Already stopped?
      if (((INSTALLED | RESOLVED) & state) == 0) {
        stopEvent = null;
        while (true) {
          long st = System.currentTimeMillis();
          try {
            lock.wait(timeout);
            if (stopEvent != null) {
              break;
            }
          } catch (InterruptedException _) {
          }
          if (timeout > 0) {
            timeout = timeout - (System.currentTimeMillis() - st);
            if (timeout <= 0) {
              break;
            }
          }
        }
        if (stopEvent == null) {
          return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this, null);
        }
      } else if (stopEvent == null) {
        // Return this if stop or update have not been called and framework is
        // stopped.
        stopEvent = new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
      }
      return stopEvent;
    }
  }


  /**
   * Stop this framework.
   * 
   * @see org.osgi.framework.Framework#stop
   */
  public void stop(int options) throws BundleException {
    secure.checkExecuteAdminPerm(this);
    secure.callShutdown(this, false);
  }


  /**
   * Restart this framework.
   * 
   * @see org.osgi.framework.Framework#update
   */
  public void update(InputStream in) throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    if (in != null) {
      try {
        in.close();
      } catch (IOException ignore) {
      }
    }
    secure.callShutdown(this, true);
  }


  /**
   * Uninstall of framework are not allowed.
   * 
   * @see org.osgi.framework.Framework#uninstall
   */
  public void uninstall() throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    throw new BundleException("uninstall of System bundle is not allowed",
        BundleException.INVALID_OPERATION);
  }


  /**
   * The system has all the permissions.
   * 
   * @see org.osgi.framework.Bundle#hasPermission
   */
  public boolean hasPermission(Object permission) {
    return true;
  }


  /**
   * Get header data.
   * 
   * @see org.osgi.framework.Bundle#getHeaders
   */
  public Dictionary getHeaders() {
    return getHeaders(null);
  }


  /**
   * Get header data.
   * 
   * @see org.osgi.framework.Bundle#getHeaders
   */
  public Dictionary getHeaders(String locale) {
    secure.checkMetadataAdminPerm(this);
    Hashtable headers = new Hashtable();
    headers.put(Constants.BUNDLE_SYMBOLICNAME, getSymbolicName());
    headers.put(Constants.BUNDLE_NAME, location);
    headers.put(Constants.EXPORT_PACKAGE, exportPackageString);
    headers.put(Constants.BUNDLE_VERSION, Main.readRelease());
    headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
    headers.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
        fwCtx.props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
    headers.put("Bundle-Icon", "icon.png;size=32,icon64.png;size=64");
    return headers;
  }


  /**
   * Get bundle data. Get resources from bundle or fragment jars.
   * 
   * @see org.osgi.framework.Bundle#findEntries
   */
  public Enumeration findEntries(String path, String filePattern, boolean recurse) {
    // TBD, What should system bundle return?
    return null;
  }


  /**
   *
   */
  public URL getEntry(String name) {
    if (secure.okResourceAdminPerm(this)) {
      return getClass().getResource(name);
    }
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
   * 
   * @return System Bundle classloader.
   */
  ClassLoader getClassLoader() {
    return getClass().getClassLoader();
  }


  /**
   * Set system bundle state to stopping
   */
  void systemShuttingdown(final boolean restart) throws BundleException {
  }


  /**
   * Shutting down is done.
   */
  void systemShuttingdownDone(final FrameworkEvent fe) {
    synchronized (lock) {
      if (state != INSTALLED) {
        state = RESOLVED;
        operation = IDLE;
        lock.notifyAll();
      }
      stopEvent = fe;
    }
  }


  /**
   * Adds an bundle as an extension that will be included in the boot class path
   * on restart.
   */
  void attachExtension(BundleGeneration extension) {
    if (extension.isBootClassPathExtension()) {
      // if we attach during startup, we assume that bundle is in BCP.
      if (getClassLoader() == null) {
        gen.attachFragment(extension);
      } else {
        throw new UnsupportedOperationException(
            "Bootclasspath extension can not be dynamicly activated");
      }
    } else {
      try {
        addClassPathURL(new URL("file:" + extension.archive.getJarLocation()));
        gen.attachFragment(extension);
      } catch (Exception e) {
        throw new UnsupportedOperationException(
            "Framework extension could not be dynamicly activated, " + e);
      }
    }
  }


  /**
   * Reads all localization entries that affects this bundle (including its
   * host/fragments)
   * 
   * @param locale locale == "" the bundle.properties will be read o/w it will
   *          read the files as described in the spec.
   * @param localization_entries will append the new entries to this dictionary
   * @param baseName the basename for localization properties, <code>null</code>
   *          will choose OSGi default
   */
  void readLocalization(String locale, Hashtable localization_entries, String baseName) {
    if (gen.fragments == null) {
      // NYI! read localization from framework.
      // There is no need for this now since it isn't used.
      return;
    }
    if (baseName == null) {
      baseName = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
    }
    if (!locale.equals("")) {
      locale = "_" + locale;
    }
    while (true) {
      String l = baseName + locale + ".properties";
      for (int i = gen.fragments.size() - 1; i >= 0; i--) {
        BundleGeneration bg = (BundleGeneration)gen.fragments.get(i);
        Hashtable tmp = bg.archive.getLocalizationEntries(l);
        if (tmp != null) {
          localization_entries.putAll(tmp);
          return;
        }
      }
      int pos = locale.lastIndexOf('_');
      if (pos == -1) {
        break;
      }
      locale = locale.substring(0, pos);
    }
  }


  /**
   *
   */
  void initSystemBundle() {
    bundleContext = new BundleContextImpl(this);
    StringBuffer sp = new StringBuffer(
        fwCtx.props.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES));
    if (sp.length() == 0) {
      // Try the system packages file
      addSysPackagesFromFile(sp, fwCtx.props.getProperty(FWProps.SYSTEM_PACKAGES_FILE_PROP));
      if (sp.length() == 0) {
        // Try the system packages base property.
        sp.append(fwCtx.props.getProperty(FWProps.SYSTEM_PACKAGES_BASE_PROP));

        if (sp.length() == 0) {
          // use default set of packages.
          String jver = fwCtx.props.getProperty(FWProps.SYSTEM_PACKAGES_VERSION_PROP);

          if (jver == null) {
            jver = Integer.toString(FWProps.javaVersionMajor) + "." + FWProps.javaVersionMinor;
          }
          try {
            addSysPackagesFromFile(sp, "packages" + jver + ".txt");
          } catch (IllegalArgumentException iae) {
            if (fwCtx.debug.framework) {
              fwCtx.debug.println("No built in list of Java packages to be exported "
                  + "by the system bundle for JRE with version '" + jver
                  + "', using the list for 1.6.");
            }
            addSysPackagesFromFile(sp, "packages1.6.txt");
          }
        }
        addSystemPackages(sp);
      }
    }
    final String extraPkgs = fwCtx.props.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
    if (extraPkgs.length() > 0) {
      sp.append(",").append(extraPkgs);
    }
    exportPackageString = sp.toString();
    gen = new BundleGeneration(this, exportPackageString);
    gen.bpkgs.registerPackages();
    gen.bpkgs.resolvePackages();
  }


  /**
   *
   */
  void uninitSystemBundle() {
    bundleContext.invalidate();
    bundleContext = null;
    if (!bootClassPathHasChanged) {
      for (Iterator i = fwCtx.bundles.getFragmentBundles(this).iterator(); i.hasNext();) {
        BundleGeneration bg = (BundleGeneration)i.next();
        if (bg.isBootClassPathExtension() && bg.bundle.extensionNeedsRestart()) {
          bootClassPathHasChanged = true;
          break;
        }
      }
    }
  }


  //
  // Private methods
  //

  /**
   *
   */
  private void doInit() throws BundleException {
    state = STARTING;
    bootClassPathHasChanged = false;
    fwCtx.init();
  }


  /**
   * Add all built-in system packages to a stringbuffer.
   */
  private void addSystemPackages(StringBuffer sp) {
    if (sp.length() > 0 && ',' != sp.charAt(sp.length() - 1)) {
      sp.append(",");
    }
    // Set up org.osgi.framework package
    String name = Bundle.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append(name + ";" + Constants.VERSION_ATTRIBUTE + "=" + FrameworkContext.SPEC_VERSION);

    sp.append(",org.osgi.framework.launch;" + Constants.VERSION_ATTRIBUTE + "="
        + FrameworkContext.LAUNCH_VERSION);
    sp.append(",org.osgi.framework.hooks.service;" + Constants.VERSION_ATTRIBUTE + "="
        + FrameworkContext.HOOKS_VERSION);

    // Set up packageadmin package
    name = PackageAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE + "="
        + PackageAdminImpl.SPEC_VERSION);

    // Set up permissionadmin package
    name = PermissionAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE + "="
        + PermissionAdminImpl.SPEC_VERSION);

    // Set up conditionalpermissionadmin package
    name = ConditionalPermissionAdmin.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE + "="
        + ConditionalPermissionAdminImpl.SPEC_VERSION);

    // Set up startlevel package
    name = StartLevel.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE + "="
        + StartLevelController.SPEC_VERSION);

    // Set up tracker package
    name = ServiceTracker.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE + "=" + "1.4");

    // Set up URL package
    name = org.osgi.service.url.URLStreamHandlerService.class.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    sp.append("," + name + ";" + Constants.VERSION_ATTRIBUTE + "=" + "1.0");
  }


  /**
   * Read a file with package names and add them to a stringbuffer. The file is
   * searched for in the current working directory, then on the class path.
   * 
   * @param sp Buffer to append the exports to. Same format as the
   *          Export-Package manifest header.
   * @param sysPkgFile Name of the file to load packages to be exported from.
   */
  private void addSysPackagesFromFile(StringBuffer sp, String sysPkgFile) {
    if (null == sysPkgFile || 0 == sysPkgFile.length())
      return;

    if (fwCtx.debug.packages) {
      fwCtx.debug.println("Will add system packages from file " + sysPkgFile);
    }

    URL url = null;
    File f = new File(new File(sysPkgFile).getAbsolutePath());

    if (!f.exists() || !f.isFile()) {
      url = SystemBundle.class.getResource(sysPkgFile);
      if (null == url) {
        url = SystemBundle.class.getResource("/" + sysPkgFile);
      }
      if (null == url) {
        if (fwCtx.debug.packages) {
          fwCtx.debug.println("Could not add system bundle package exports from '" + sysPkgFile
              + "', file not found.");
        }
      }
    }
    BufferedReader in = null;
    try {
      Reader reader = null;
      String source = null;

      if (null == url) {
        reader = new FileReader(f);
        source = f.getAbsolutePath().toString();
      } else {
        reader = new InputStreamReader(url.openStream());
        source = url.toString();
      }
      in = new BufferedReader(reader);
      if (fwCtx.debug.packages) {
        fwCtx.debug.println("\treading from " + source);
      }

      String line;
      for (line = in.readLine(); line != null; line = in.readLine()) {
        line = line.trim();
        if (line.length() > 0 && !line.startsWith("#")) {
          sp.append(line);
          sp.append(",");
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read " + sysPkgFile + ": " + e);
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {
      }
    }
  }


  /**
   * This method start a thread that stop this Framework, stopping all started
   * bundles.
   * 
   * <p>
   * If the framework is not started, this method does nothing. If the framework
   * is started, this method will:
   * <ol>
   * <li>Set the state of the FrameworkContext to <i>inactive</i>.</li>
   * <li>Suspended all started bundles as described in the
   * {@link Bundle#stop(int)} method except that the persistent state of the
   * bundle will continue to be started. Reports any exceptions that occur
   * during stopping using <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol>
   * </p>
   * 
   */
  void shutdown(final boolean restart) {
    synchronized (lock) {
      boolean wasActive = false;
      switch (state) {
      case Bundle.INSTALLED:
      case Bundle.RESOLVED:
        shutdownDone(false);
        break;
      case Bundle.ACTIVE:
        wasActive = true;
        // Fall through
      case Bundle.STARTING:
        if (shutdownThread == null) {
          try {
            final boolean wa = wasActive;
            shutdownThread = new Thread(fwCtx.threadGroup, "Framework shutdown") {
              public void run() {
                shutdown0(restart, wa);
              }
            };
            shutdownThread.setDaemon(false);
            shutdownThread.start();
          } catch (Exception e) {
            systemShuttingdownDone(new FrameworkEvent(FrameworkEvent.ERROR, this, e));
          }
        }
        break;
      case Bundle.STOPPING:
        // Shutdown already inprogress
        break;
      }
    }
  }


  /**
   * Stop this FrameworkContext, suspending all started contexts. This method
   * suspends all started contexts so that they can be automatically restarted
   * when this FrameworkContext is next launched.
   * 
   * <p>
   * If the framework is not started, this method does nothing. If the framework
   * is started, this method will:
   * <ol>
   * <li>Set the state of the FrameworkContext to <i>inactive</i>.</li>
   * <li>Stop all started bundles as described in the {@link Bundle#stop(int)}
   * method except that the persistent state of the bundle will continue to be
   * started. Reports any exceptions that occur during stopping using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol>
   * </p>
   * 
   */
  private void shutdown0(final boolean restart, final boolean wasActive) {
    try {
      synchronized (lock) {
        waitOnOperation(lock, "Framework." + (restart ? "update" : "stop"), true);
        operation = DEACTIVATING;
        state = STOPPING;
      }
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));
      if (wasActive) {
        stopAllBundles();
        saveClasspaths();
      }
      synchronized (lock) {
        fwCtx.uninit();
        shutdownThread = null;
        shutdownDone(restart);
      }
      if (restart) {
        if (wasActive) {
          start();
        } else {
          init();
        }
      }
    } catch (Exception e) {
      shutdownThread = null;
      systemShuttingdownDone(new FrameworkEvent(FrameworkEvent.ERROR, this, e));
    }

  }


  /**
   * Tell system bundle shutdown finished.
   */
  private void shutdownDone(boolean restart) {
    int t;
    if (bootClassPathHasChanged) {
      t = FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED;
    } else {
      t = restart ? FrameworkEvent.STOPPED_UPDATE : FrameworkEvent.STOPPED;
    }
    systemShuttingdownDone(new FrameworkEvent(t, this, null));
  }


  /**
   * Stop and unresolve all bundles.
   */
  private void stopAllBundles() {
    if (fwCtx.startLevelController != null) {
      fwCtx.startLevelController.shutdown();
    }

    // Stop all active bundles, in reverse bundle ID order
    // The list will be empty when the start level service is in use.
    final List activeBundles = fwCtx.bundles.getActiveBundles();
    for (int i = activeBundles.size() - 1; i >= 0; i--) {
      final BundleImpl b = (BundleImpl)activeBundles.get(i);
      try {
        if (((Bundle.ACTIVE | Bundle.STARTING) & b.getState()) != 0) {
          // Stop bundle without changing its autostart setting.
          b.stop(Bundle.STOP_TRANSIENT);
        }
      } catch (BundleException be) {
        fwCtx.listeners.frameworkError(b, be);
      }
    }

    final List allBundles = fwCtx.bundles.getBundles();

    // Set state to INSTALLED and purge any unrefreshed bundles
    for (Iterator i = allBundles.iterator(); i.hasNext();) {
      final BundleImpl b = (BundleImpl)i.next();
      if (b.getBundleId() != 0) {
        b.setStateInstalled(false);
        b.purge();
      }
    }
  }


  private void saveClasspaths() {
    StringBuffer bootClasspath = new StringBuffer();
    for (Iterator i = fwCtx.bundles.getFragmentBundles(this).iterator(); i.hasNext();) {
      BundleGeneration ebg = (BundleGeneration)i.next();
      String path = ebg.archive.getJarLocation();
      if (ebg.isBootClassPathExtension()) {
        if (bootClasspath.length() > 0) {
          bootClasspath.append(File.pathSeparator);
        }
        bootClasspath.append(path);
      }
    }

    // Post processing to handle boot class extension
    try {
      File bcpf = new File(Util.getFrameworkDir(fwCtx), BOOT_CLASSPATH_FILE);
      if (bootClasspath.length() > 0) {
        saveStringBuffer(bcpf, bootClasspath);
      } else {
        bcpf.delete();
      }
    } catch (IOException e) {
      if (fwCtx.debug.errors) {
        fwCtx.debug.println("Could not save classpath " + e);
      }
    }
  }


  private void saveStringBuffer(File f, StringBuffer content) throws IOException {
    PrintStream out = null;
    try {
      out = new PrintStream(new FileOutputStream(f));
      out.println(content.toString());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }


  private void addClassPathURL(URL url) throws Exception {
    ClassLoader cl = getClassLoader();
    Method m = null;
    Class c = cl.getClass();
    while (true) {
      try {
        m = c.getDeclaredMethod("addURL", new Class[] { URL.class });
        break;
      } catch (NoSuchMethodException e) {
        c = c.getSuperclass();
        if (c == null) {
          throw e;
        }
      }
    }
    m.setAccessible(true);
    m.invoke(cl, new Object[] { url });
  }

}
