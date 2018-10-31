/*
 * Copyright (c) 2003-2016, KNOPFLERFISH project
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;


/**
 * This class contains references to all common data structures
 * inside the framework.
 *
 * @author Jan Stein, Erik Wistrand, Philippe Laporte,
 *         Mats-Ola Persson, Gunnar Ekolin
 */
public class FrameworkContext  {

  private static final String CONDITIONAL_PERMISSION_SECURITY_MANAGER = "org.knopflerfish.framework.permissions.ConditionalPermissionSecurityManager";

  private static final String KF_SECURITY_MANAGER = "org.knopflerfish.framework.permissions.KFSecurityManager";

  private static final String SECURE_PERMISSON_OPS = "org.knopflerfish.framework.SecurePermissionOps";

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
  ServiceHooks serviceHooks;

  /**
   * All bundle hooks.
   */
  BundleHooks bundleHooks;

  /**
   * All resolver hooks.
   */
  ResolverHooks resolverHooks;

  /**
   * All weaving hooks.
   */
  WeavingHooks weavingHooks;


  /**
   * All capabilities, exported and imported packages in this framework.
   */
  Resolver resolver;

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
  List<Validator> validator = null;

  /**
   * Private Bundle Data Storage
   */
  FileTree dataStorage /*= null*/;

  /**
   * The start level controller.
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
  ArrayList<String> bootDelegationPatterns;
  boolean bootDelegationUsed /*= false*/;

  /**
   * The parent class loader for bundle classloaders.
   */
  ClassLoader parentClassLoader;

  /**
   * Is init in progress.
   */
  boolean isInit = false;

  /**
   * Is this first init.
   */
  boolean firstInit = true;

  /**
   * Framework id.
   */
  final int id;

  /**
   * Framework init count.
   */
  int initCount = 0;

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
  LinkedList<BundleThread> bundleThreads;

  /**
   * Package admin handle
   */
  PackageAdminImpl packageAdmin = null;

  /**
   * Mode for BSN collision checks.
   */
  String bsnversionMode;

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


  static void setupURLStreamHandleFactory() {
    ServiceURLStreamHandlerFactory res = new ServiceURLStreamHandlerFactory();
    try {
      URL.setURLStreamHandlerFactory(res);
      systemUrlStreamHandlerFactory = res;
    } catch (final Throwable e) {
      System.err.println("Cannot set global URL handlers, "
         +"continuing without OSGi service URL handler (" +e +")");
    }
  }


  /**
   * Contruct a framework context
   *
   */
  FrameworkContext(Map<String, String> initProps)  {
    synchronized (globalFwLock) {
      id = globalId++;
    }
    threadGroup = new ThreadGroup("FW#" + id);
    props = new FWProps(initProps, this);
    perm = new PermissionOps();
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
      final int pos = clazz.lastIndexOf('.');
      if (pos != -1) {
        final Pkg p = resolver.getPkg(clazz.substring(0, pos));
        if (p != null) {
          final ExportPkg ep = p.getBestProvider();
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
  void init(FrameworkListener... initListeners)
  {
    log("initializing");
    initCount++;
    isInit = true;

    if (firstInit && Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
        .equals(props.getProperty(Constants.FRAMEWORK_STORAGE_CLEAN))) {
      deleteFWDir();
      firstInit = false;
    }

    buildBootDelegationPatterns();
    selectBootDelegationParentClassLoader();

    if (setSecurityManager()) {
      perm = (PermissionOps) doNew(SECURE_PERMISSON_OPS);
      systemBundle.secure = perm;
    }
    perm.init();

    listeners = new Listeners(this, perm, initListeners);

    final String v = props.getProperty(FWProps.VALIDATOR_PROP);
    if (!v.equalsIgnoreCase("none") && !v.equalsIgnoreCase("null")) {
      validator = new ArrayList<Validator>();
      for (int start = 0; start < v.length(); ) {
        int end = v.indexOf(',', start);
        if (end == -1) {
          end = v.length();
        }
        final String vs = "org.knopflerfish.framework.validator." + v.substring(start, end).trim();
        validator.add((Validator) doNew(vs));
        start = end + 1;
      }
    }

    if (null == urlStreamHandlerFactory) {
      // Set up URL handlers before creating the storage
      // implementation, with the exception of the bundle: URL
      // handler, since this requires an intialized framework to work
      if (props.REGISTERSERVICEURLHANDLER) {
        // Check if we already have registered one
        if (systemUrlStreamHandlerFactory == null) {
          setupURLStreamHandleFactory();
        }
        urlStreamHandlerFactory = systemUrlStreamHandlerFactory;
        if (systemContentHandlerFactory != null) {
          contentHandlerFactory   = systemContentHandlerFactory;
        } else {
          contentHandlerFactory   = new ServiceContentHandlerFactory(this);
          try {
            URLConnection.setContentHandlerFactory(contentHandlerFactory);
            systemContentHandlerFactory   = contentHandlerFactory;
          } catch (final Throwable e) {
            debug.printStackTrace
              ("Cannot set global content handlers, "
               +"continuing without OSGi service content handler (" +e +")", e);
            frameworkError(systemBundle, e);
          }
        }
      } else {
          if (systemUrlStreamHandlerFactory != null) {
            urlStreamHandlerFactory = systemUrlStreamHandlerFactory;
          }
          else {
              urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory();
          }
          contentHandlerFactory   = new ServiceContentHandlerFactory(this);
      }
    }

    props.props.put(Constants.FRAMEWORK_UUID, getUUID());

    String bsnProp = props.getProperty(Constants.FRAMEWORK_BSNVERSION).trim().toLowerCase();

    if (bsnProp.equals(Constants.FRAMEWORK_BSNVERSION_MANAGED) ||
        bsnProp.equals(Constants.FRAMEWORK_BSNVERSION_MULTIPLE) ||
        bsnProp.equals(Constants.FRAMEWORK_BSNVERSION_SINGLE)) {
      bsnversionMode = bsnProp;
    } else {
      bsnversionMode = Constants.FRAMEWORK_BSNVERSION_MANAGED;
      debug.println("Unknown property value: " + Constants.FRAMEWORK_BSNVERSION + " = " + bsnProp);
    }

    final String storageClass = "org.knopflerfish.framework.bundlestorage." +
      props.getProperty(FWProps.BUNDLESTORAGE_PROP) + ".BundleStorageImpl";
    try {
      @SuppressWarnings("unchecked")
      final
      Class<? extends BundleStorage> storageImpl =
          (Class<? extends BundleStorage>) Class.forName(storageClass);

      final Constructor<? extends BundleStorage> cons =
          storageImpl.getConstructor(new Class[] { FrameworkContext.class });
      storage = cons.newInstance(new Object[] { this });
    } catch (final Exception e) {
      Throwable cause = e;
      if (e instanceof InvocationTargetException) {
        // Use the nested exception as cause in this case.
        cause = ((InvocationTargetException)e).getTargetException();
      }
      RuntimeException re = new RuntimeException("Failed to initialize storage "
                                                 + storageClass, cause);
      frameworkError(systemBundle, re);
      throw re;
    }
    if (props.getBooleanProperty(FWProps.READ_ONLY_PROP)) {
      dataStorage = null;
    } else {
      dataStorage = Util.getFileStorage(this, "data");
    }

    BundleThread.checkWarnStopActionNotSupported(this);
    bundleThreads = new LinkedList<BundleThread>();

    resolver  = new Resolver(this);
    services  = new Services(this, perm);

    // Add this framework to the bundle URL handle
    urlStreamHandlerFactory.addFramework(this);

    serviceHooks = new ServiceHooks(this);
    bundleHooks = new BundleHooks(this);
    resolverHooks = new ResolverHooks(this);
    weavingHooks = new WeavingHooks(this);

    systemBundle.initSystemBundle();

    bundles = new Bundles(this);

    serviceHooks.open();
    resolverHooks.open();
    weavingHooks.open();

    perm.registerService();

    packageAdmin = new PackageAdminImpl(this);
    @SuppressWarnings("deprecation")
    final
    String[] classes = new String [] { org.osgi.service.packageadmin.PackageAdmin.class.getName() };
    services.register(systemBundle,
                      classes,
                      packageAdmin,
                      null);
    registerStartLevel();
    bundles.load();
    systemBundle.extensionCallStart(null);
    listeners.initDone();
    isInit = false;
    log("inited");

    if (debug.framework) {
      log("Installed bundles:");
      // Use the ordering in the bundle storage to get a sorted list of bundles.
      final BundleArchive [] allBAs = storage.getAllBundleArchives();
      for (final BundleArchive ba : allBAs) {
        final Bundle b = bundles.getBundle(ba.getBundleLocation());
        log(" #" +b.getBundleId() +" " +b.getSymbolicName() +":"
            +b.getVersion() +" location:" +b.getLocation());
      }
    }
  }


  private Object doNew(final String clazz) {
    try {
      final Class<?> n = (Class<?>) Class.forName(clazz);
      final Constructor<?> nc = n.getConstructor(new Class[] { FrameworkContext.class });
      return nc.newInstance(new Object[] { this });
    } catch (final InvocationTargetException ite) {
      throw new RuntimeException(clazz + ", constructor failed with, " + ite.getTargetException(), ite);
    } catch (final NoSuchMethodException e) {
      // If no validator, probably stripped framework
      throw new RuntimeException(clazz + ", found no such class", e);
    } catch (final NoClassDefFoundError ncdfe) {
      // Validator uses class not supported by JVM ignore
      throw new RuntimeException(clazz + ", class not supported by JVM", ncdfe);
    } catch (final Exception e) {
      throw new RuntimeException(clazz + ", constructor failed", e);
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

    resolverHooks = null;

    if(props.REGISTERSERVICEURLHANDLER) {
      urlStreamHandlerFactory.removeFramework(this);
      // Since handlers can only be registered once, keep them in this
      // case.
    } else {
      urlStreamHandlerFactory = null;
      contentHandlerFactory   = null;
    }

    bundles.clear();
    bundles = null;

    services.clear();
    services = null;

    listeners.clear();
    listeners = null;

    resolver.clear();
    resolver = null;

    synchronized (bundleThreads) {
      while (!bundleThreads.isEmpty()) {
        bundleThreads.remove(0).quit();
      }
    }
    bundleThreads = null;

    dataStorage = null;

    storage.close();
    storage = null;

    perm = new PermissionOps();

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
          current = (SecurityManager) doNew(KF_SECURITY_MANAGER);
          System.setSecurityManager(current);
          smUse = 1;
        } else {
          Class<?> cpsmc;
          try {
            cpsmc = Class.forName(CONDITIONAL_PERMISSION_SECURITY_MANAGER);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException("Missing class", e); 
          }
          if (cpsmc.isInstance(current)) {
            if (smUse == 0) {
              smUse = 2;
            } else {
              smUse++;
            }
          } else {
            throw new SecurityException("Incompatible security manager installed");
          }
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
    final String d = Util.getFrameworkDir(this);

    final FileTree dir = (d != null) ? new FileTree(d) : null;
    if (dir != null) {
      if(dir.exists()) {
        log("deleting old framework directory.");
        final boolean bOK = dir.delete();
        if(!bOK) {
          debug.println("Failed to remove existing fwdir "
                              +dir.getAbsolutePath());
        }
      }
    }
  }


  private String getUUID() {
    // We use a "pseudo" random UUID (Version 4).
    final String sid = Integer.toHexString(id * 65536 + initCount);
    final String baseUUID = "4e524769-3136-4b46-8000-00000000";
    return baseUUID.substring(0, baseUUID.length() - sid.length()) + sid;
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

      @SuppressWarnings("deprecation")
      final
      String [] clsName = new String [] { org.osgi.service.startlevel.StartLevel.class.getName() };
      services.register(systemBundle,
                        clsName,
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
    if (b == null || !(b instanceof BundleImpl) || this != ((BundleImpl)b).fwCtx) {
      throw new IllegalArgumentException("Bundle does not belong to this framework: " + b);
    }
  }


  /**
   * Parse boot-delegation pattern property.
   *
   */
  void buildBootDelegationPatterns() {
    final String bootDelegationString
      = props.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);

    bootDelegationUsed = bootDelegationString.length() > 0;
    bootDelegationPatterns = new ArrayList<String>(1);

    if (bootDelegationUsed) {
      try {
        for (final HeaderEntry he : Util
            .parseManifestHeader(Constants.FRAMEWORK_BOOTDELEGATION,
                                 bootDelegationString, true, true, false)) {
          final String key = he.getKey();
          if (key.equals("*")) {
            bootDelegationPatterns = null;
            //in case funny person puts a * amongst other things
            break;
          }
          else if (key.endsWith(".*")) {
            bootDelegationPatterns.add(key.substring(0, key.length() - 1));
          }
          else if (key.endsWith(".")) {
            frameworkError(systemBundle, new IllegalArgumentException
                           (Constants.FRAMEWORK_BOOTDELEGATION
                            +" entry ends with '.': " +key));
          }
          else if (key.indexOf("*") != - 1) {
            frameworkError(systemBundle, new IllegalArgumentException
                           (Constants.FRAMEWORK_BOOTDELEGATION
                            +" entry contains a '*': " + key));
          }
          else {
            bootDelegationPatterns.add(key);
          }
        }
      }
      catch (final IllegalArgumentException e) {
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
      for (final String ps : bootDelegationPatterns) {
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
   * Convenience method for throwing framework error event.
   *
   * @param b Bundle which caused the error.
   * @param t Throwable generated.
   */
  public void frameworkError(Bundle b, Throwable t, FrameworkListener... oneTimeListeners) {
    listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, b, t), oneTimeListeners);
  }


  /**
   * Convenience method for throwing framework error event.
   *
   * @param bc BundleContext for bundle which caused the error.
   * @param t Throwable generated.
   */
  public void frameworkError(BundleContextImpl bc, Throwable t, FrameworkListener... oneTimeListeners) {
    listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bc.bundle, t), oneTimeListeners);
  }


  /**
   * Convenience method for throwing framework info event.
   *
   * @param b Bundle which caused the throwable.
   * @param t Throwable generated.
   */
  public void frameworkInfo(Bundle b, Throwable t, FrameworkListener... oneTimeListeners) {
    listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.INFO, b, t), oneTimeListeners);
  }


  /**
   * Convenience method for throwing framework warning event.
   *
   * @param b Bundle which caused the throwable.
   * @param t Throwable generated.
   */
  public void frameworkWarning(Bundle b, Throwable t, FrameworkListener... oneTimeListeners) {
    listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.WARNING, b, t), oneTimeListeners);
  }


  /**
   * Convenience method for throwing framework warning event.
   *
   * @param b Bundle which caused the throwable.
   * @param t Throwable generated.
   */
  public void frameworkWarning(BundleGeneration bg, Throwable t, FrameworkListener... oneTimeListeners) {
    frameworkWarning(bg.bundle, t, oneTimeListeners);
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
