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
import java.net.*;
import java.security.*;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.lang.reflect.Constructor;

import org.osgi.framework.*;
import org.osgi.framework.launch.Framework;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class contains references to all common data structures
 * inside the framework.
 *
 * @author Jan Stein, Erik Wistrand, Philippe Laporte,
 *         Mats-Ola Persson, Gunnar Ekolin
 */
public class FrameworkContext  {

  /**
   * Specification version for this framework.
   */
  static final String SPEC_VERSION = "1.5";

  /**
   * Specification version for org.osgi.framework.launch
   */
  static final String LAUNCH_VERSION = "1.0";

  /**
   * Specification version for org.osgi.framework.hooks.service
   */
  static final String HOOKS_VERSION = "1.0";

  /**
   * Boolean indicating if the framework has been initialized or not.
   */
  boolean initialized = false;

  /**
   * Boolean indicating that framework is running.
   */
  volatile boolean active;

  /**
   * Set during shutdown process.
   */
  volatile boolean shuttingdown /*= false*/;

  /** Monitot to wait for start completion on when shutting down. */
  private Object startStopLock = new Object();

  /**
   * All bundle in this framework.
   */
  public Bundles bundles;

  /**
   * All listeners in this framework.
   */
  Listeners listeners;

  /**
   * All exported and imported packages in this framework.
   */
  Packages packages;

  /**
   * All registered services in this framework.
   */
  Services services;

  /**
   * PermissionOps handle.
   */
  PermissionOps perm;


  /**
   * System bundle
   */
  SystemBundle systemBundle;

  BundleContextImpl systemBC;

  /**
   * Bundle Storage
   */
  BundleStorage storage;

  /**
   * Private Bundle Data Storage
   */
  FileTree dataStorage /*= null*/;

  /**
   * The start level controler.
   */
  StartLevelController startLevelController;


  /**
   * Factory for handling service-based URLs
   */
  ServiceURLStreamHandlerFactory urlStreamHandlerFactory;

  /**
   * Factory for handling service-based URL contents
   */
  ServiceContentHandlerFactory   contentHandlerFactory;


  /**
   * The file where we store the class path
   */
  private final static String CLASSPATH_DIR = "classpath";
  private final static String BOOT_CLASSPATH_FILE = "boot";
  private final static String FRAMEWORK_CLASSPATH_FILE = "framework";

  /** Cached value of
   * props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)
   * Used and updated by isValidEE()
   */
  private Set    eeCacheSet = new HashSet();
  private String eeCache = null;

  final static int EXIT_CODE_NORMAL  = 0;
  final static int EXIT_CODE_RESTART = 200;

  public FWProps props;

  /**
   * Contruct a framework context
   *
   */
  public FrameworkContext(Map initProps, FrameworkContext parent)  {
    props        = new FWProps(initProps, parent);
    systemBundle = new SystemBundle(this);

    log("created");
  }


  // Initialize the framework, see spec v4.2 sec 4.2.4
  void init()
  {
    if (initialized) return;

    log("initializing");

    if (Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
        .equals(props.getProperty(Constants.FRAMEWORK_STORAGE_CLEAN))) {
      deleteFWDir();
      // Must remove the storage clean property since it should not be
      // used more than once!
      props.removeProperty(Constants.FRAMEWORK_STORAGE_CLEAN);
    }
    props.save();

    buildBootDelegationPatterns();

    ProtectionDomain pd = null;
    if (System.getSecurityManager() != null) {
      try {
        pd = getClass().getProtectionDomain();
      } catch (Throwable t) {
        if(props.debug.classLoader) {
          props.debug.println("Failed to get protection domain: " + t);
        }
      }
      perm = new SecurePermissionOps(this);
    } else {
      perm = new PermissionOps();
    }
    systemBundle.setPermissionOps(perm);

    boolean createHandlers = null==urlStreamHandlerFactory;
    if (createHandlers) {
      // Set up URL handlers before creating the storage
      // implementation, with the exception of the bundle: URL
      // handler, since this requires an intialized framework to work
      urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
      contentHandlerFactory   = new ServiceContentHandlerFactory(this);
    }

    urlStreamHandlerFactory
      .setURLStreamHandler(ReferenceURLStreamHandler.PROTOCOL,
                           new ReferenceURLStreamHandler());

    // Install newly created service based URL stream handler. This
    // can be turned off if there is need
    if(createHandlers && props.REGISTERSERVICEURLHANDLER) {
      try {
        URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

        URLConnection.setContentHandlerFactory(contentHandlerFactory);
      } catch (Throwable e) {
        props.debug.println("Cannot set global URL handlers, "
                            +"continuing without OSGi service URL handler ("
                            + e + ")");
        e.printStackTrace();
      }
    }


    try {
      Class storageImpl = Class.forName(props.whichStorageImpl);

      Constructor cons
        = storageImpl.getConstructor(new Class[] { FrameworkContext.class });
      storage
        = (BundleStorage) cons.newInstance(new Object[] { this });
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize storage "
                                 +props.whichStorageImpl +": ", e);
    }
    dataStorage       = Util.getFileStorage(this, "data");
    packages          = new Packages(this);

    listeners         = new Listeners(this, perm);
    services          = new Services(this, perm);

    systemBC          = new BundleContextImpl(systemBundle);
    systemBundle.setBundleContext(systemBC);

    bundles           = new Bundles(this);

    perm.registerService();

    String[] classes = new String [] { PackageAdmin.class.getName() };
    services.register(systemBundle,
                      classes,
                      new PackageAdminImpl(this),
                      null);

    // ...and create the bundle URL handle, now that we have the set of bundles
    urlStreamHandlerFactory
      .setURLStreamHandler(BundleURLStreamHandler.PROTOCOL,
                           new BundleURLStreamHandler(bundles, perm));

    registerStartLevel();

    bundles.load();

    initialized = true;
    log("inited");

    log("Installed bundles:");
    final List allBundles = bundles.getBundles();
    for (Iterator i = allBundles.iterator(); i.hasNext(); ) {
      final BundleImpl b = (BundleImpl) i.next();
      log(" #" +b.getBundleId() +" " +b.getSymbolicName() +":"
          +b.getVersion() +" location:" +b.getLocation());
    }
  }

  /**
   * Undo as much as possible of what init() does.
   */
  private void uninit()
  {
    initialized = false;

    startLevelController = null;

    bundles.clear();
    bundles = null;

    systemBC.invalidate();
    systemBC = null;
    systemBundle.setBundleContext(systemBC);

    services.clear();
    services = null;

    listeners.clear();
    listeners = null;

    packages.clear();
    packages = null;

    dataStorage = null;

    storage.close();
    storage = null;

    if(props.REGISTERSERVICEURLHANDLER) {
      // Since handlers can only be registered once, keep them in this
      // case.
    } else {
      urlStreamHandlerFactory = null;
      contentHandlerFactory   = null;
    }

    perm = null;
    systemBundle.setPermissionOps(perm);
    bootDelegationPatterns.clear();
  }

  private void deleteFWDir() {
    String d = Util.getFrameworkDir(this);

    FileTree dir = (d != null) ? new FileTree(d) : null;
    if (dir != null) {
      if(dir.exists()) {
        log("deleting old framework directory.");
        boolean bOK = dir.delete();
        if(!bOK) {
          props.debug.println("Failed to remove existing fwdir "
                              +dir.getAbsolutePath());
        }
      }
    }
  }


  private final String USESTARTLEVEL_PROP = "org.knopflerfish.startlevel.use";

  private void registerStartLevel() {
    String useStartLevel = props.getProperty(USESTARTLEVEL_PROP, FWProps.TRUE);

    if(FWProps.TRUE.equals(useStartLevel)) {
      if(props.debug.startlevel) {
        props.debug.println("[using startlevel service]");
      }
      startLevelController = new StartLevelController(this);

      // restoreState just reads from persistent storage
      // open() needs to be called to actually do the work
      // This is done after framework has been launched.
      startLevelController.restoreState();

      services.register(systemBundle,
                        new String [] { StartLevel.class.getName() },
                        startLevelController,
                        null);
    }
  }


  /**
   * Start this FrameworkContext.
   * This method starts all the bundles that were started at
   * the time of the last shutdown.
   *
   * <p>If the FrameworkContext is already started, this method does nothing.
   * If the FrameworkContext is not started, this method will:
   * <ol>
   * <li>Enable event handling. At this point, events can be delivered to
   * listeners.</li>
   * <li>Attempt to start all bundles marked for starting as described in the
   * {@link Bundle#start(int)} method.
   * Reports any exceptions that occur during startup using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Set the state of the FrameworkContext to <i>active</i>.</li>
   * <li>Broadcasting a <code>FrameworkEvent</code> through the
   * <code>FrameworkListener.frameworkStarted</code> method.</li>
   * </ol></p>
   *
   * <p>If this FrameworkContext is not launched, it can still install,
   * uninstall, start and stop bundles.  (It does these tasks without
   * broadcasting events, however.)  Using FrameworkContext without launching
   * it allows for off-line debugging of the FrameworkContext.</p>
   *
   * @param startBundle If it is specified with a value larger than 0,
   *                    then the bundle with that id is started.
   *                    Otherwise start all suspended bundles.
   */
  public void launch(long startBundle) throws BundleException {
    if (!active) {
      synchronized(startStopLock) {
        log("starting");

        active = true;
        if (startBundle > 0) {
          startBundle(startBundle, 0);
        } else if (startLevelController != null) {
          // start level open is delayed to this point to
          // correctly work at restart
          startLevelController.open();
        } else {
          // Start bundles according to their autostart setting.
          final Iterator i = storage.getStartOnLaunchBundles().iterator();
          while (i.hasNext()) {
            final BundleImpl b = (BundleImpl)
              bundles.getBundle((String)i.next());
            try {
              final int autostartSetting = b.archive.getAutostartSetting();
              // Launch must not change the autostart setting of a bundle
              int option = Bundle.START_TRANSIENT;
              if (Bundle.START_ACTIVATION_POLICY == autostartSetting) {
                // Transient start according to the bundles activation policy.
                option |= Bundle.START_ACTIVATION_POLICY;
              }
              b.start(option);
            } catch (BundleException be) {
              listeners.frameworkError(b, be);
            }
          }
        }

        systemBundle.systemActive();
        log("started");
        listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED,
                                                    systemBundle, null));
      }
    }
  }


  /**
   * The thread that performs shutdown of this framework instance.
   */
  private Thread shutdownThread = null;

  /**
   * Stop this FrameworkContext, suspending all started contexts.
   * This method suspends all started contexts so that they can be
   * automatically restarted when this FrameworkContext is next launched.
   *
   * <p>If the framework is not started, this method does nothing.
   * If the framework is started, this method will:
   * <ol>
   * <li>Set the state of the FrameworkContext to <i>inactive</i>.</li>
   * <li>Suspended all started bundles as described in the
   * {@link Bundle#stop(int)} method except that the persistent
   * state of the bundle will continue to be started.
   * Reports any exceptions that occur during stopping using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol></p>
   *
   */
  public void shutdown() {
    if (null!=shutdownThread && shutdownThread.isAlive()) {
      log("stopping already in progress, ignoreing this request");
      return;
    }

    if (!active) {
      log("stop ignored, framework not started.");
      return;
    }

    log("stopping");
    Thread shutdownThread = new Thread("Framework shutdown")
      {
        public void run()
        {
          shutdown0();
          log("stopped");
        }
      };
    shutdownThread.setDaemon(false);
    shutdownThread.start();
  }


  /**
   * Stop this FrameworkContext, suspending all started contexts.
   * This method suspends all started contexts so that they can be
   * automatically restarted when this FrameworkContext is next launched.
   *
   * <p>If the framework is not started, this method does nothing.
   * If the framework is started, this method will:
   * <ol>
   * <li>Set the state of the FrameworkContext to <i>inactive</i>.</li>
   * <li>Stop all started bundles as described in the
   * {@link Bundle#stop(int)} method except that the persistent
   * state of the bundle will continue to be started.
   * Reports any exceptions that occur during stopping using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol>
   * </p>
   *
   */
  private void shutdown0()
  {
    try {
      //If starting wait for it to complete
      synchronized(startStopLock) {
        active = false;
        shuttingdown = true;
        systemBundle.systemShuttingdown();

        saveClasspaths();
        stopAllBundles();
        uninit();

        shuttingdown = false;
      }
    } catch (Exception e) {
      log("error during stop", e);
      systemBundle.stopEvent
        = new FrameworkEvent(FrameworkEvent.ERROR, systemBundle, e);
    }
    systemBundle.systemShuttingdownDone();
    shutdownThread = null;
  }


  /**
   * Stop and unresolve all bundles.
   */
  private void stopAllBundles()
  {
    log("stopping bundles");

    if (startLevelController != null) {
      startLevelController.shutdown();
    }

    // Stop all active bundles, in reverse bundle ID order
    // The list will be empty when the start level service is in use.
    final List activeBundles = bundles.getActiveBundles();
    for (int i = activeBundles.size()-1; i >= 0; i--) {
      final BundleImpl b = (BundleImpl) activeBundles.get(i);
      try {
        synchronized (b) {
          if ( ((Bundle.ACTIVE|Bundle.STARTING) & b.getState()) != 0) {
            // Stop bundle without changing its autostart setting.
            b.stop(Bundle.STOP_TRANSIENT);
          }
        }
      } catch (BundleException be) {
        listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR,
                                                    b, be));
      }
    }

    final List allBundles = bundles.getBundles();

    // Set state to INSTALLED and purge any unrefreshed bundles
    for (Iterator i = allBundles.iterator(); i.hasNext(); ) {
      final BundleImpl b = (BundleImpl) i.next();
      b.setStateInstalled(false);
      b.purge();
    }
    log("bundles stopped");
  }


  public void saveClasspaths()
  {
    StringBuffer bootClasspath = new StringBuffer();
    StringBuffer frameworkClasspath = new StringBuffer();
    for (Iterator i = bundles.getFragmentBundles(systemBundle).iterator();
         i.hasNext(); ) {
      BundleImpl eb = (BundleImpl)i.next();
      String path = eb.archive.getJarLocation();
      StringBuffer sb = eb.isBootClassPathExtension()
        ? bootClasspath : frameworkClasspath;
      if (sb.length()>0) {
        sb.append(File.pathSeparator);
      }
      sb.append(path);
    }

    // Post processing to handle boot class extension
    try {
      FileTree storage = Util.getFileStorage(this, CLASSPATH_DIR);
      File bcpf = new File(storage, BOOT_CLASSPATH_FILE);
      File fcpf = new File(storage, FRAMEWORK_CLASSPATH_FILE);
      if (bootClasspath.length() > 0) {
        saveStringBuffer(bcpf, bootClasspath);
      } else {
        bcpf.delete();
      }
      if (frameworkClasspath.length() > 0) {
        saveStringBuffer(fcpf, frameworkClasspath);
      } else {
        fcpf.delete();
      }
    } catch (IOException e) {
      System.err.println("Could not save classpath " + e);
    }
    log("boot classpath handling done");
  }


  private void saveStringBuffer(File f, StringBuffer content)
    throws IOException
  {
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


  /**
   * Install a bundle from the given location.
   *
   * @param location The location identifier of the bundle to install.
   * @param in The InputStream from which the bundle will be read.
   * @return The BundleImpl object of the installed bundle.
   * @exception BundleException If the install failed.
   */
  public long installBundle(String location, InputStream in) throws BundleException {
    return bundles.install(location, in).id;
  }


  /**
   * Start a bundle.
   *
   * @param id Id of bundle to start.
   * @param options The start options to use when starting the bundle.
   * @exception BundleException If start failed.
   */
  public void startBundle(long id, int options)
    throws BundleException
  {
    final Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.start(options);
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Stop a bundle.
   *
   * @param id Id of bundle to stop.
   * @param options The stop options to use when stopping the bundle.
   * @exception BundleException If stop failed.
   */
  public void stopBundle(long id, int options) throws BundleException {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.stop(options);
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Uninstall a bundle.
   *
   * @param id Id of bundle to stop.
   * @exception BundleException If uninstall failed.
   */
  public void uninstallBundle(long id) throws BundleException {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.uninstall();
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Update a bundle.
   *
   * @param id Id of bundle to update.
   * @exception BundleException If update failed.
   */
  public void updateBundle(long id) throws BundleException {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.update();
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Retrieve location of the bundle that has the given
   * unique identifier.
   *
   * @param id The identifier of the bundle to retrieve.
   * @return A location as a string, or <code>null</code>
   * if the identifier doesn't match any installed bundle.
   */
  public String getBundleLocation(long id) {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      return b.getLocation();
    } else {
      return null;
    }
  }

  /**
   * Retrieve bundle id of the bundle that has the given
   * unique location.
   *
   * @param location The location of the bundle to retrieve.
   * @return The unique identifier of the bundle, or <code>-1</code>
   * if the location doesn't match any installed bundle.
   */
  public long getBundleId(String location) {
    Bundle b = bundles.getBundle(location);
    if (b != null) {
      return b.getBundleId();
    } else {
      return -1;
    }
  }

  /**
   * Get private bundle data storage file handle.
   */
  public FileTree getDataStorage(long id) {
        if (dataStorage != null) {
          return new FileTree(dataStorage, Long.toString(id));
        }
        return null;
  }

  /**
   * Check if an execution environment string is accepted
   */
  boolean isValidEE(String ee) {
    ee = ee.trim();
    if(ee == null || "".equals(ee)) {
      return true;
    }

    String fwEE = props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

    if(fwEE == null) {
      // If EE is not set, allow everything
      return true;
    } else if (!fwEE.equals(eeCache)) {
      eeCacheSet.clear();

      String[] l = Util.splitwords(fwEE, ",");
      for(int i = 0 ; i < l.length; i++) {
        eeCacheSet.add(l[i]);
      }
      eeCache = fwEE;
    }

    String[] eel   = Util.splitwords(ee, ",");
    for(int i = 0 ; i < eel.length; i++) {
      if(eeCacheSet.contains(eel[i])) {
        return true;
      }
    }
    return false;
  }


  /**
   * Get the bundle context used by the system bundle.
   */
  public BundleContext getSystemBundleContext() {
    return systemBC;
  }

  ArrayList /* String */ bootDelegationPatterns = new ArrayList(1);
  boolean bootDelegationUsed /*= false*/;

  void buildBootDelegationPatterns() {
    String bootDelegationString = props.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
    bootDelegationUsed = (bootDelegationString != null);

    try {
      Iterator i = Util.parseEntries(Constants.FRAMEWORK_BOOTDELEGATION,
                                     bootDelegationString,
                                     true, true, false);

      while (i.hasNext()) {
        Map e = (Map)i.next();
        String key = (String)e.get("$key");
        if (key.equals("*")) {
          bootDelegationPatterns = null;
          //in case funny person puts a * amongst other things
          break;
        }
        else if (key.endsWith(".*")) {
          bootDelegationPatterns.add(key.substring(0, key.length() - 1));
        }
        else if (key.endsWith(".")) {
          listeners.frameworkError(systemBundle, new IllegalArgumentException(
                                                                                                            Constants.FRAMEWORK_BOOTDELEGATION + " entry ends with '.': " + key));
        }
        else if (key.indexOf("*") != - 1) {
          listeners.frameworkError(systemBundle, new IllegalArgumentException(
                                                                                                            Constants.FRAMEWORK_BOOTDELEGATION + " entry contains a '*': " + key));
        }
        else {
          bootDelegationPatterns.add(key);
        }
      }
    }
    catch (IllegalArgumentException e) {
      listeners.frameworkError(systemBundle, e);
    }
  }

  boolean isBootDelegatedResource(String name) {
    // Convert resource name to class name format, preserving the
    // package part of the path/name.
    int pos = name.lastIndexOf('/');
    return pos != -1
      ? isBootDelegated(name.substring(0,pos).replace('/','.')+".X")
      : false;
  }

  boolean isBootDelegated(String className){
    if(!bootDelegationUsed){
      return false;
    }
    int pos = className.lastIndexOf('.');
    if (pos != -1) {
      String classPackage = className.substring(0, pos);
      if (bootDelegationPatterns == null) {
        return true;
      }
      else {
        for (Iterator i = bootDelegationPatterns.iterator(); i.hasNext(); ) {
          String ps = (String)i.next();
          if ((ps.endsWith(".") &&
               classPackage.regionMatches(0, ps, 0, ps.length() - 1)) ||
               classPackage.equals(ps)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  void log(String msg)
  {
    if (props.debug.framework) {
      props.debug.println("Framework instance " +hashCode() +": " +msg);
    }
  }

  void log(String msg, Throwable t)
  {
    if (props.debug.framework) {
      props.debug.printStackTrace("Framework instance " +hashCode() +": "
                                  +msg, t);
    }
  }

}
