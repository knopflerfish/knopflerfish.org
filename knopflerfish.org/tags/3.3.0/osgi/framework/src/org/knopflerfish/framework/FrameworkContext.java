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

package org.knopflerfish.framework;

import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

// NYI, make these imports dynamic!
import org.knopflerfish.framework.permissions.ConditionalPermissionSecurityManager;
import org.knopflerfish.framework.permissions.KFSecurityManager;


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
   * Debug handle.
   */
  public Debug debug;

  /**
   * All bundle in this framework.
   */
  Bundles bundles;

  /**
   * All listeners in this framework.
   */
  Listeners listeners;

  /**
   * All service hooks.
   */
  Hooks hooks;

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

  /**
   * Bundle Storage
   */
  BundleStorage storage;

  /**
   * Bundle Validator
   */
  List /* Validator */ validator = null;

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
  ServiceContentHandlerFactory contentHandlerFactory;

  /**
   * Patterns from the boot delegation property.
   */
  ArrayList /* String */ bootDelegationPatterns;
  boolean bootDelegationUsed /*= false*/;

  /**
   * The parent class loader for bundle classloaders.
   */
  ClassLoader parentClassLoader;

  /**
   * All bundle in this framework.
   */
  boolean firstInit = true;

  /**
   * Cached value of
   * props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)
   * Used and updated by isValidEE()
   */
  private Set    eeCacheSet = new HashSet();
  private String eeCache = null;

  /**
   * Framework id.
   */
  final int id;

  /**
   * Framework properties.
   */
  public FWProps props;

  /**
   * Framework Thread Group.
   */
  public ThreadGroup threadGroup;

  /**
   * Threads for running listeners and activators
   */
  LinkedList bundleThreads;

  /**
   * Factory for handling service-based URLs
   */
  volatile static ServiceURLStreamHandlerFactory systemUrlStreamHandlerFactory;

  /**
   * Factory for handling service-based URL contents
   */
  volatile static ServiceContentHandlerFactory   systemContentHandlerFactory;

  /**
   * JVM global lock.
   */
  static Object globalFwLock = new Object();

  /**
   * Id to use for next instance of KF framework.
   */
  static int globalId = 0;

  /**
   * Reference counter for security manager.
   */
  static int smUse = 0;


  /**
   * Contruct a framework context
   *
   */
  FrameworkContext(Map initProps)  {
    synchronized (globalFwLock) {
      id = globalId++;
    }
    threadGroup = new ThreadGroup("FW#" + id);
    props = new FWProps(initProps, this);
    perm = new SecurePermissionOps(this);
    systemBundle = new SystemBundle(this);
    log("created");
  }


  /**
   * Public method used by permissionshandling for fetching
   * the class loader used for named class.
   *
   */
  public ClassLoader getClassLoader(String clazz) {
    if (clazz != null) {
      int pos = clazz.lastIndexOf('.');
      if (pos != -1) {
        Pkg p = packages.getPkg(clazz.substring(0, pos));
        if (p != null) {
          ExportPkg ep = p.getBestProvider();
          if (ep != null) {
            return ep.bpkgs.bg.getClassLoader();
          }
        }
      }
    }
    return systemBundle.getClassLoader();
  }


  /**
   * Initialize the framework, see spec v4.2 sec 4.2.4
   *
   */
  void init()
  {
    log("initializing");

    if (firstInit && Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
        .equals(props.getProperty(Constants.FRAMEWORK_STORAGE_CLEAN))) {
      deleteFWDir();
      firstInit = false;
    }

    buildBootDelegationPatterns();
    selectBootDelegationParentClassLoader();

    if (setSecurityManager()) {
      systemBundle.secure = perm;
    } else {
      perm = new PermissionOps();
    }
    perm.init();

    String v = props.getProperty(FWProps.VALIDATOR_PROP);
    if (!v.equalsIgnoreCase("none") && !v.equalsIgnoreCase("null")) {
      validator = new ArrayList();
      for (int start = 0; start < v.length(); ) {
        int end = v.indexOf(',', start);
        if (end == -1) {
          end = v.length();
        }
        String vs = "org.knopflerfish.framework.validator." + v.substring(start, end).trim();
        try {
          Class vi = Class.forName(vs);
          Constructor vc = vi.getConstructor(new Class[] { FrameworkContext.class });
          validator.add((Validator)vc.newInstance(new Object[] { this }));
        } catch (InvocationTargetException ite) {
          // NYI, log error from validator
          System.err.println("Construct of " + vs + " failed: " + ite.getTargetException());
        } catch (NoSuchMethodException e) {
          // If no validator, probably stripped framework
          throw new RuntimeException(vs + ", found no such Validator", e);
        } catch (NoClassDefFoundError ncdfe) {
          // Validator uses class not supported by JVM ignore
          throw new RuntimeException(vs + ", Validator not supported by JVM", ncdfe);
        } catch (Exception e) {
          throw new RuntimeException(vs + ", failed to construct Validator", e);
        }
        start = end + 1;
      }
    }

    if (null == urlStreamHandlerFactory) {
      // Set up URL handlers before creating the storage
      // implementation, with the exception of the bundle: URL
      // handler, since this requires an intialized framework to work
      if (props.REGISTERSERVICEURLHANDLER) {
        // Check if we already have registered one
        if (systemUrlStreamHandlerFactory != null) {
          urlStreamHandlerFactory = systemUrlStreamHandlerFactory;
          contentHandlerFactory   = systemContentHandlerFactory;
          urlStreamHandlerFactory.addFramework(this);
        } else {
          urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
          contentHandlerFactory   = new ServiceContentHandlerFactory(this);
          try {
            URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);
            URLConnection.setContentHandlerFactory(contentHandlerFactory);
            systemContentHandlerFactory   = contentHandlerFactory;
            systemUrlStreamHandlerFactory = urlStreamHandlerFactory;
          } catch (Throwable e) {
            debug.printStackTrace
              ("Cannot set global URL handlers, "
               +"continuing without OSGi service URL handler (" +e +")", e);
          }
        }
      } else {
        urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
        contentHandlerFactory   = new ServiceContentHandlerFactory(this);
      }
    }

    String storageClass = "org.knopflerfish.framework.bundlestorage." +
      props.getProperty(FWProps.BUNDLESTORAGE_PROP) + ".BundleStorageImpl";
    try {
      Class storageImpl = Class.forName(storageClass);

      Constructor cons = storageImpl.getConstructor(new Class[] { FrameworkContext.class });
      storage = (BundleStorage) cons.newInstance(new Object[] { this });
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize storage "
                                 + storageClass, e);
    }
    dataStorage = Util.getFileStorage(this, "data");

    bundleThreads = new LinkedList();

    packages  = new Packages(this);
    listeners = new Listeners(this, perm);
    services  = new Services(this, perm);

    // Add this framework to the bundle URL handle
    urlStreamHandlerFactory.addFramework(this);

    systemBundle.initSystemBundle();

    bundles           = new Bundles(this);

    hooks             = new Hooks(this);    
    hooks.open();

    perm.registerService();

    String[] classes = new String [] { PackageAdmin.class.getName() };
    services.register(systemBundle,
                      classes,
                      new PackageAdminImpl(this),
                      null);

    registerStartLevel();

    bundles.load();

    log("inited");

    log("Installed bundles:");
    // Use the ordering in the bundle storage to get a sorted list of bundles.
    final BundleArchive [] allBAs = storage.getAllBundleArchives();
    for (int i = 0; i<allBAs.length; i++) {
      final BundleArchive ba = allBAs[i];
      final Bundle b = bundles.getBundle(ba.getBundleLocation());
      log(" #" +b.getBundleId() +" " +b.getSymbolicName() +":"
          +b.getVersion() +" location:" +b.getLocation());
    }
  }


  /**
   * Undo as much as possible of what init() does.
   *
   */
  void uninit()
  {
    log("uninit");
    startLevelController = null;

    systemBundle.uninitSystemBundle();

    bundles.clear();
    bundles = null;

    services.clear();
    services = null;

    listeners.clear();
    listeners = null;

    packages.clear();
    packages = null;

    synchronized (bundleThreads) {
      while (!bundleThreads.isEmpty()) {
        ((BundleThread)bundleThreads.remove(0)).quit();
      }
    }
    bundleThreads = null;

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

    perm = new SecurePermissionOps(this);

    synchronized (globalFwLock) {
      if (--smUse == 0) {
        System.setSecurityManager(null);
      }
    }

    parentClassLoader = null;
    bootDelegationPatterns = null;
  }


  /**
   * Check and/or set security manager.
   *
   */
  private boolean setSecurityManager() {
    synchronized (globalFwLock) {
      SecurityManager current = System.getSecurityManager();
      final String osgiSecurity = props.getProperty(Constants.FRAMEWORK_SECURITY);

      if (osgiSecurity.length() > 0) {
        if (!Constants.FRAMEWORK_SECURITY_OSGI.equals(osgiSecurity)) {
          throw new SecurityException("Unknown OSGi security, " + osgiSecurity);
        }
        if (current == null) {
          final String POLICY_PROPERTY = "java.security.policy";
          final String defaultPolicy
            = this.getClass().getResource("/framework.policy").toString();
          final String policy = System.getProperty(POLICY_PROPERTY, defaultPolicy);
          if (debug.framework) {
            debug.println("Installing OSGi security manager, policy="+policy);
          }
          System.setProperty(POLICY_PROPERTY, policy);
          // Make sure policy is updated, required for some JVMs.
          java.security.Policy.getPolicy().refresh();
          current = new KFSecurityManager(debug);
          System.setSecurityManager(current);
          smUse = 1;
        } else if (current instanceof ConditionalPermissionSecurityManager) {
          if (smUse == 0) {
            smUse = 2;
          } else {
            smUse++;
          }
        } else if (!(current instanceof ConditionalPermissionSecurityManager)) {
          throw new SecurityException("Incompatible security manager installed");
        }
      }
      return current != null;
    }
  }


  /**
   * Delete framework directory if it exists.
   *
   */
  private void deleteFWDir() {
    String d = Util.getFrameworkDir(this);

    FileTree dir = (d != null) ? new FileTree(d) : null;
    if (dir != null) {
      if(dir.exists()) {
        log("deleting old framework directory.");
        boolean bOK = dir.delete();
        if(!bOK) {
          debug.println("Failed to remove existing fwdir "
                              +dir.getAbsolutePath());
        }
      }
    }
  }


  /**
   * Setup start level service, if enabled.
   *
   */
  private void registerStartLevel() {
    if (props.getBooleanProperty(FWProps.STARTLEVEL_USE_PROP)) {
      if (debug.startlevel) {
        debug.println("[using startlevel service]");
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
   * Get private bundle data storage file handle.
   *
   */
  public FileTree getDataStorage(long id) {
    if (dataStorage != null) {
      return new FileTree(dataStorage, Long.toString(id));
    }
    return null;
  }


  /**
   * Check that bundle belongs to this framework instance.
   *
   */
  void checkOurBundle(Bundle b) {
    if (this != ((BundleImpl)b).fwCtx) {
      throw new IllegalArgumentException("Bundle does not belong to this framework: " + b);
    }
  }


  /**
   * Check if an execution environment string is accepted
   *
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
   * Parse bootdelegation pattern property.
   *
   */
  void buildBootDelegationPatterns() {
    final String bootDelegationString
      = props.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);

    bootDelegationUsed = bootDelegationString.length() > 0;
    bootDelegationPatterns = new ArrayList(1);

    if (bootDelegationUsed) {
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
            listeners.frameworkError(systemBundle, new IllegalArgumentException
                                     (Constants.FRAMEWORK_BOOTDELEGATION
                                      +" entry ends with '.': " +key));
          }
          else if (key.indexOf("*") != - 1) {
            listeners.frameworkError(systemBundle, new IllegalArgumentException
                                     (Constants.FRAMEWORK_BOOTDELEGATION
                                      +" entry contains a '*': " + key));
          }
          else {
            bootDelegationPatterns.add(key);
          }
        }
      }
      catch (IllegalArgumentException e) {
        debug.printStackTrace("Failed to parse " +
                              Constants.FRAMEWORK_BOOTDELEGATION, e);
      }
    }
  }


  /**
   * Check if named resource is matched by the bootdelegation pattern
   *
   */
  boolean isBootDelegatedResource(String name) {
    // Convert resource name to class name format, preserving the
    // package part of the path/name.
    final int pos = name.lastIndexOf('/');
    return pos != -1
      ? isBootDelegated(name.substring(0,pos).replace('/','.')+".X")
      : false;
  }


  /**
   * Check if named class is matched by the bootdelegation pattern
   *
   */
  boolean isBootDelegated(String className){
    if(!bootDelegationUsed){
      return false;
    }
    if (bootDelegationPatterns == null) {
      return true;
    }
    final int pos = className.lastIndexOf('.');
    if (pos != -1) {
      final String classPackage = className.substring(0, pos);
      for (Iterator i = bootDelegationPatterns.iterator(); i.hasNext(); ) {
        String ps = (String)i.next();
        if ((ps.endsWith(".") &&
             classPackage.regionMatches(0, ps, 0, ps.length() - 1)) ||
            classPackage.equals(ps)) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Select the parent class loader,to be used by bundle classloaders.
   *
   */
  private void selectBootDelegationParentClassLoader() {
    final String s = props.getProperty(Constants.FRAMEWORK_BUNDLE_PARENT);
    if (Constants.FRAMEWORK_BUNDLE_PARENT_EXT.equals(s)) {
      parentClassLoader = ClassLoader.getSystemClassLoader();
      if (parentClassLoader != null) {
        parentClassLoader = parentClassLoader.getParent();
      }
    } else if (Constants.FRAMEWORK_BUNDLE_PARENT_APP.equals(s)) {
      parentClassLoader = ClassLoader.getSystemClassLoader();
    } else if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equals(s)) {
      parentClassLoader = this.getClass().getClassLoader();
    } else {
      parentClassLoader = Object.class.getClassLoader();
    }
    // If bootclassloader, wrap it
    if (parentClassLoader == null) {
      parentClassLoader = new BCLoader();
    }
  }


  /**
   * Log message for debugging framework
   *
   */
  void log(String msg) {
    if (debug.framework) {
      debug.println("Framework instance " +hashCode() +": " +msg);
    }
  }


  /**
   * Log message for debugging framework
   *
   */
  void log(String msg, Throwable t) {
    if (debug.framework) {
      debug.printStackTrace("Framework instance " +hashCode() +": "
                                  +msg, t);
    }
  }


  /**
   * Wrapper class for BootClassLoader.
   *
   */
  private static class BCLoader extends ClassLoader {
    protected BCLoader() {
      super(null);
    }
  }
}
