/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import org.knopflerfish.framework.Util.HeaderEntry;
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
  static final String SPEC_VERSION = "1.6";

  /**
   * Specification version for org.osgi.framework.launch
   */
  static final String LAUNCH_VERSION = "1.0";

  /**
   * Specification version for org.osgi.framework.hooks.service
   */
  static final String HOOKS_VERSION = "1.1";

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
   * All bundle in this framework.
   */
  boolean firstInit = true;

  /**
   * Cached value of
   * props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)
   * Used and updated by isValidEE()
   */
  private final Set<String>    eeCacheSet = new HashSet<String>();
  private String eeCache = null;

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


  boolean bsnversionSingle;


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
  void init()
  {
    log("initializing");
    initCount++;

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

    final String v = props.getProperty(FWProps.VALIDATOR_PROP);
    if (!v.equalsIgnoreCase("none") && !v.equalsIgnoreCase("null")) {
      validator = new ArrayList<Validator>();
      for (int start = 0; start < v.length(); ) {
        int end = v.indexOf(',', start);
        if (end == -1) {
          end = v.length();
        }
        final String vs = "org.knopflerfish.framework.validator." + v.substring(start, end).trim();
        try {
          @SuppressWarnings("unchecked")
          final
          Class<? extends Validator> vi = (Class<? extends Validator>) Class.forName(vs);
          final Constructor<? extends Validator> vc =
              vi.getConstructor(new Class[] { FrameworkContext.class });
          validator.add(vc.newInstance(new Object[] { this }));
        } catch (final InvocationTargetException ite) {
          // NYI, log error from validator
          System.err.println("Construct of " + vs + " failed: " + ite.getTargetException());
        } catch (final NoSuchMethodException e) {
          // If no validator, probably stripped framework
          throw new RuntimeException(vs + ", found no such Validator", e);
        } catch (final NoClassDefFoundError ncdfe) {
          // Validator uses class not supported by JVM ignore
          throw new RuntimeException(vs + ", Validator not supported by JVM", ncdfe);
        } catch (final Exception e) {
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
        } else {
          urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
          contentHandlerFactory   = new ServiceContentHandlerFactory(this);
          try {
            URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);
            URLConnection.setContentHandlerFactory(contentHandlerFactory);
            systemContentHandlerFactory   = contentHandlerFactory;
            systemUrlStreamHandlerFactory = urlStreamHandlerFactory;
          } catch (final Throwable e) {
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

    props.props.put(Constants.FRAMEWORK_UUID, getUUID());

    bsnversionSingle = Constants.FRAMEWORK_BSNVERSION_SINGLE
        .equals(props.getProperty(Constants.FRAMEWORK_BSNVERSION));

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
      throw new RuntimeException("Failed to initialize storage "
                                 + storageClass, cause);
    }
    dataStorage = Util.getFileStorage(this, "data");

    BundleThread.checkWarnStopActionNotSupported(this);
    bundleThreads = new LinkedList<BundleThread>();

    resolver  = new Resolver(this);
    listeners = new Listeners(this, perm);
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

    log("inited");

    log("Installed bundles:");
    // Use the ordering in the bundle storage to get a sorted list of bundles.
    final BundleArchive [] allBAs = storage.getAllBundleArchives();
    for (final BundleArchive ba : allBAs) {
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

      // restoreState just reads from persistent storage
      // open() needs to be called to actually do the work
      // This is done after framework has been launched.
      startLevelController.restoreState();

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
   * Check if an execution environment string is accepted
   *
   */
  boolean isValidEE(String ee) {
    ee = ee.trim();
    if(ee == null || "".equals(ee)) {
      return true;
    }

    @SuppressWarnings("deprecation")
    final String fwEE = props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

    if(fwEE == null) {
      // If EE is not set, allow everything
      return true;
    } else if (!fwEE.equals(eeCache)) {
      eeCacheSet.clear();

      final String[] l = Util.splitwords(fwEE, ",");
      for (final String element : l) {
        eeCacheSet.add(element);
      }
      eeCache = fwEE;
    }

    final String[] eel   = Util.splitwords(ee, ",");
    for (final String element : eel) {
      if(eeCacheSet.contains(element)) {
        return true;
      }
    }
    return false;
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
   * The list of active {@link ExtensionContext} instances for attached
   * extensions with an ExtensionActivator.
   */
  private final List<ExtensionContext> extCtxs = new ArrayList<ExtensionContext>();

  /**
   * Create a new {@link ExtensionContext} instances for the specified
   * extension. This will create an instance of the extension
   * activator class and call its activate-method of.
   *
   * @param extension the extension bundle to activate.
   */
  void activateExtension(final BundleGeneration extension) {
    extCtxs.add( new ExtensionContext(this, extension) );
  }

  /**
   * Inform all active extension contexts about a newly created bundle
   * class loader.
   *
   * @param bcl the new bundle class loader to inform about.
   */
  void bundleClassLoaderCreated(final BundleClassLoader bcl) {
    for (final ExtensionContext extCtx : extCtxs) {
      extCtx.bundleClassLoaderCreated(bcl);
    }
  }

  /**
   * Inform all active extension contexts about a closed down bundle
   * class loader.
   *
   * @param bcl the closed down bundle class loader to inform about.
   */
  void bundleClassLoaderClosed(final BundleClassLoader bcl) {
    for (final ExtensionContext extCtx : extCtxs) {
      extCtx.bundleClassLoaderClosed(bcl);
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
