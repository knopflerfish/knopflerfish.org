/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Locale;


import org.osgi.framework.*;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


/**
 * This class contains references to all common data structures
 * inside the framework.
 *
 * @author Jan Stein, Erik Wistrand
 */
public class Framework {

  /**
   * Specification version for this framework.
   */
  static final String SPEC_VERSION = "1.2";

  /**
   * AdminPermission used for permission check.
   */
  final static AdminPermission ADMIN_PERMISSION = new AdminPermission();

  /**
   * Boolean indicating that framework is running.
   */
  boolean active;

  /**
   * Set during shutdown process.
   */
  boolean shuttingdown = false;

  /**
   * All bundle in this framework.
   */
  protected Bundles bundles;

  /**
   * All listeners in this framework.
   */
  Listeners listeners = new Listeners();

  /**
   * All exported and imported packages in this framework.
   */
  Packages packages;

  /**
   * All registered services in this framework.
   */
  Services services = new Services();

  /**
   * PermissionAdmin service
   */
  PermissionAdminImpl permissions = null;

  /**
   * indicates that we use security
   */
  boolean bPermissions = false;

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
  FileTree dataStorage = null;

  /**
   * Main handle so that main does get GCed.
   */
  Object mainHandle;

  /**
   * The start level service.
   */
  StartLevelImpl                 startLevelService;

  /**
   * Factory for handling service-based URLs
   */
  ServiceURLStreamHandlerFactory urlStreamHandlerFactory;

  /**
   * Factory for handling service-based URL contents
   */
  ServiceContentHandlerFactory   contentHandlerFactory;

  /**
   * Magic handler for bundle: URLs
   */
  URLStreamHandler bundleURLStreamhandler;

  /**
   * Property constants for the framework.
   */
  final static String osArch    = System.getProperty("os.arch");
  final static String osName    = System.getProperty("os.name");
  final static String osVersion = System.getProperty("os.version");

  // Some tests conflicts with the R3 spec. If testcompliant=true
  // prefer the tests, not the spec
  public final static boolean R3_TESTCOMPLIANT =
    "true".equals(System.getProperty("org.knopflerfish.osgi.r3.testcompliant",
                                     "false"));

  // If set to true, set the bundle startup thread's context class
  // loader to the bundle class loader. This is useful for tests
  // but shouldn't really be used in production.
  final static boolean SETCONTEXTCLASSLOADER =
    "true".equals(System.getProperty("org.knopflerfish.osgi.setcontextclassloader", "false"));

  final static boolean REGISTERBUNDLEURLHANDLER =
    "true".equals(System.getProperty("org.knopflerfish.osgi.registerbundleurlhandler", "false"));

  final static boolean REGISTERSERVICEURLHANDLER =
    "true".equals(System.getProperty("org.knopflerfish.osgi.registerserviceurlhandler", "true"));


  // Accepted execution environments.
  static String defaultEE = "CDC-1.0/Foundation-1.0,OSGi/Minimum-1.0";

  static boolean bIsMemoryStorage = false;

  static int javaVersionMajor = -1;
  static int javaVersionMinor = -1;
  static int javaVersionMicro = -1;
  static {
    String javaVersion = System.getProperty("java.version");
    // Value is on the form M.N.U_P[-xxx] where M,N,U,P are decimal integers
    if (null!=javaVersion) {
      int startPos = 0;
      int endPos   = 0;
      int max      = javaVersion.length();
      while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))) {
        endPos++;
      }
      if (startPos<endPos) {
        try {
          javaVersionMajor
            = Integer.parseInt(javaVersion.substring(startPos,endPos));
          startPos = endPos +1;
          endPos   = startPos;
          while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))) {
            endPos++;
          }
          if (startPos<endPos) {
            javaVersionMinor
              = Integer.parseInt(javaVersion.substring(startPos,endPos));
            startPos = endPos +1;
            endPos   = startPos;
            while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))){
              endPos++;
            }
            if (startPos<endPos) {
              javaVersionMicro
                = Integer.parseInt(javaVersion.substring(startPos,endPos));
            }
          }
        } catch (NumberFormatException _nfe) {
        }
      }
    }
  }

  /**
   * Is it safe to use double-checked locking or not.
   * It is safe if JSR 133 is included in the running JRE.
   * I.e., for Java SE if version is 1.5 or higher.
   */
  final static boolean isDoubleCheckedLockingSafe
    = "true".equals(System.getProperty
                    ("org.knopflerfish.framework.is_doublechecked_locking_safe",
                     (javaVersionMajor>=1 && javaVersionMinor>=5
                      ? "true" : "false")));

  /**
   * Contruct a framework.
   *
   */
  public Framework(Object m) throws Exception {
    // guard this for profiles without System.setProperty
    try {
      System.setProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, defaultEE);
    } catch (Throwable e) {
      if(Debug.packages) {
        Debug.println("Failed to set execution environment: " + e);
      }
    }

    String whichStorageImpl = "org.knopflerfish.framework.bundlestorage." +
      System.getProperty("org.knopflerfish.framework.bundlestorage", "file") +
      ".BundleStorageImpl";

    bIsMemoryStorage = whichStorageImpl.equals("org.knopflerfish.framework.bundlestorage.memory.BundleStorageImpl");

    // We just happens to know that the memory storage impl isn't R3
    // compatible. And it never will be since it isn't persistent
    // by design.
    if(R3_TESTCOMPLIANT && bIsMemoryStorage) {
      throw new RuntimeException("Memory bundle storage is not compatible " +
                                 "with R3 complicance");
    }

    Class storageImpl = Class.forName(whichStorageImpl);
    storage           = (BundleStorage)storageImpl.newInstance();

    dataStorage       = Util.getFileStorage("data");
    packages          = new Packages(this);

    // guard this for profiles without Class.getProtectionDomain
    ProtectionDomain pd = null;
    if(System.getSecurityManager() != null) {
      try {
        pd = getClass().getProtectionDomain();
      } catch (Throwable t) {
        if(Debug.classLoader) {
          Debug.println("Failed to get protection domain: " + t);
        }
      }
    }
    systemBundle      = new SystemBundle(this, pd);

    systemBC          = new BundleContextImpl(systemBundle);
    bundles           = new Bundles(this);

    systemBundle.setBundleContext(systemBC);



    if (System.getSecurityManager() != null) {
      bPermissions = true;
      permissions       = new PermissionAdminImpl(this);
      String [] classes = new String [] { PermissionAdmin.class.getName() };
      services.register(systemBundle,
                        classes,
                        permissions,
                        null);

      Policy.setPolicy(new FrameworkPolicy(Policy.getPolicy(),permissions));
    }

    String[] classes = new String [] { PackageAdmin.class.getName() };
    services.register(systemBundle,
                      classes,
                      new PackageAdminImpl(this),
                      null);


    String useStartLevel =
      System.getProperty("org.knopflerfish.startlevel.use", "true");

    if("true".equals(useStartLevel)) {
      if(Debug.startlevel) {
        Debug.println("[using startlevel service]");
      }
      startLevelService = new StartLevelImpl(this);

      // restoreState just reads from persistant storage
      // open() needs to be called to actually do the work
      // This is done after framework has been launched.
      startLevelService.restoreState();

      services.register(systemBundle,
                        new String [] { StartLevel.class.getName() },
                        startLevelService,
                        null);
    }


    mainHandle = m;

    urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
    contentHandlerFactory   = new ServiceContentHandlerFactory(this);
    bundleURLStreamhandler  = new BundleURLStreamHandler(bundles);

    // Only register bundle: URLs publicly if explicitly told so
    // Note: registering bundle: URLs exports way to much.
    if(REGISTERBUNDLEURLHANDLER) {
      urlStreamHandlerFactory
        .setURLStreamHandler(BundleURLStreamHandler.PROTOCOL,
                             bundleURLStreamhandler);
    }

    urlStreamHandlerFactory
      .setURLStreamHandler(ReferenceURLStreamHandler.PROTOCOL,
                           new ReferenceURLStreamHandler());

    // Install service based URL stream handler. This can be turned
    // off if there is need
    if(REGISTERSERVICEURLHANDLER) {
      try {
        URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

        URLConnection.setContentHandlerFactory(contentHandlerFactory);
      } catch (Throwable e) {
        Debug.println("Cannot set global URL handlers, continuing without OSGi service URL handler (" + e + ")");
        e.printStackTrace();
      }
    }
    bundles.load();
  }


  /**
   * Start this Framework.
   * This method starts all the bundles that were started at
   * the time of the last shutdown.
   *
   * <p>If the Framework is already started, this method does nothing.
   * If the Framework is not started, this method will:
   * <ol>
   * <li>Enable event handling. At this point, events can be delivered to
   * listeners.</li>
   * <li>Attempt to start all bundles marked for starting as described in the
   * {@link Bundle#start} method.
   * Reports any exceptions that occur during startup using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Set the state of the Framework to <i>active</i>.</li>
   * <li>Broadcasting a <code>FrameworkEvent</code> through the
   * <code>FrameworkListener.frameworkStarted</code> method.</li>
   * </ol></p>
   *
   * <p>If this Framework is not launched, it can still install,
   * uninstall, start and stop bundles.  (It does these tasks without
   * broadcasting events, however.)  Using Framework without launching
   * it allows for off-line debugging of the Framework.</p>
   *
   * @param startBundle If it is specified with a value larger than 0,
   *                    then the bundle with that id is started.
   *                    Otherwise start all suspended bundles.
   */
  public void launch(long startBundle) throws BundleException {
    if (!active) {
      active = true;
      if (startBundle > 0) {
        startBundle(startBundle);
      } else {
        for (Iterator i = storage.getStartOnLaunchBundles().iterator(); i.hasNext(); ) {
          Bundle b = bundles.getBundle((String)i.next());
          try {
            b.start();
          } catch (BundleException be) {
            listeners.frameworkError(b, be);
          }
        }
      }
      systemBundle.systemActive();

      // start level open is delayed to this point to
      // correctly work at restart
      if(startLevelService != null) {
        startLevelService.open();
      }

      listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, systemBundle, null));
    }
  }


  /**
   * Stop this Framework, suspending all started contexts.
   * This method suspends all started contexts so that they can be
   * automatically restarted when this Framework is next launched.
   *
   * <p>If the framework is not started, this method does nothing.
   * If the framework is started, this method will:
   * <ol>
   * <li>Set the state of the Framework to <i>inactive</i>.</li>
   * <li>Suspended all started bundles as described in the
   * {@link Bundle#stop} method except that the persistent
   * state of the bundle will continue to be started.
   * Reports any exceptions that occur during stopping using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol></p>
   *
   */
  public void shutdown() {
    if (active) {
      // No shuttingdown event specified
      // listeners.frameworkChanged(new FrameworkEvent(FrameworkEvent.SHUTTING_DOWN));
      active = false;
      List slist = storage.getStartOnLaunchBundles();
      shuttingdown = true;
      if(startLevelService != null) {
        startLevelService.shutdown();
      }
      systemBundle.systemShuttingdown();
      try {
        systemBundle.stop();
      } catch (BundleException ignore) {}
      // Stop bundles, in reverse start order
      for (int i = slist.size()-1; i >= 0; i--) {
        Bundle b = bundles.getBundle((String)slist.get(i));
        try {
          if(b != null) {
            synchronized (b) {
              if (b.getState() == Bundle.ACTIVE) {
                b.stop();
              }
            }
          }
        } catch (BundleException be) {
          listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, b, be));
        }
      }
      shuttingdown = false;
      // Purge any unrefreshed bundles
      BundleImpl [] all = bundles.getBundles();
      for (int i = 0; i < all.length; i++) {
        all[i].purge();
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
   * @exception BundleException If start failed.
   */
  public void startBundle(long id) throws BundleException {
    BundleImpl b = bundles.getBundle(id);
    if (b != null) {
      b.start();
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Stop a bundle.
   *
   * @param id Id of bundle to stop.
   * @exception BundleException If stop failed.
   */
  public void stopBundle(long id) throws BundleException {
    BundleImpl b = bundles.getBundle(id);
    if (b != null) {
      b.stop();
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
    BundleImpl b = bundles.getBundle(id);
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
    BundleImpl b = bundles.getBundle(id);
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
    BundleImpl b = bundles.getBundle(id);
    if (b != null) {
      return b.location;
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
    BundleImpl b = bundles.getBundle(location);
    if (b != null) {
      return b.id;
    } else {
      return -1;
    }
  }

  /**
   * Check that we have admin permission.
   *
   * @exception SecurityException if we don't have admin permission.
   */
  void checkAdminPermission() {
    if (bPermissions) {
      SecurityManager sm = System.getSecurityManager();
      if (null!=sm) {
        sm.checkPermission(ADMIN_PERMISSION);
      }
    }
  }

  /**
   * Get private bundle data storage file handle.
   */
  FileTree getDataStorage() {
    return dataStorage;
  }

  /**
   * Check if an execution environment string is accepted
   */
  boolean isValidEE(String ee) {

    if(ee == null || "".equals(ee)) {
      return true;
    }

    String fwEE = System.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

    if(fwEE == null) {
      eeCacheSet.clear();
    } else {
      if(!fwEE.equals(eeCache)) {
        eeCacheSet.clear();

        String[] l = Util.splitwords(fwEE, ",", '\"');
        for(int i = 0 ; i < l.length; i++) {
          eeCacheSet.add(l[i]);
        }
      }
    }
    eeCache = fwEE;

    String[] eel   = Util.splitwords(ee, ",", '\"');

    for(int i = 0 ; i < eel.length; i++) {
      if(eeCacheSet.contains(eel[i])) {
        return true;
      }
    }

    return false;
  }


  // Cached value of
  // System.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)
  // Used and updated by isValidEE()
  Set    eeCacheSet = new HashSet();
  String eeCache = null;

  //
  // Static package methods
  //

  /**
   * Retrieve the value of the named framework property.
   *
   */
  public static String getProperty(String key) {
    if (Constants.FRAMEWORK_VERSION.equals(key)) {
      // The version of the framework.
      return SPEC_VERSION;
    } else if (Constants.FRAMEWORK_VENDOR.equals(key)) {
      // The vendor of this framework implementation.
      return "Knopflerfish";
    } else if (Constants.FRAMEWORK_LANGUAGE.equals(key)) {
      // The language being used. See ISO 639 for possible values.
      return Locale.getDefault().getLanguage();
    } else if (Constants.FRAMEWORK_OS_NAME.equals(key)) {
      // The name of the operating system of the hosting computer.
      return osName;
    } else if (Constants.FRAMEWORK_OS_VERSION.equals(key)) {
      // The version number of the operating system of the hosting computer.
      return osVersion;
    } else if (Constants.FRAMEWORK_PROCESSOR.equals(key)) {
      // The name of the processor of the hosting computer.
      return osArch;
    } else if (Constants.FRAMEWORK_EXECUTIONENVIRONMENT.equals(key)) {
      // The name of the fw execution environment
      return System.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
    } else {
      return System.getProperty(key);
    }
  }

  /**
   * Get the bundle context used by the system bundle.
   */
  public BundleContext getSystemBundleContext() {
    return (BundleContext)
      AccessController.doPrivileged(new  PrivilegedAction() {
          public Object run() {
            return systemBC;
          }});
  }
}
