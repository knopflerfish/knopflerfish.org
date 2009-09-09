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
   * All bundle in this framework.
   */
  public Bundles bundles;

  /**
   * All listeners in this framework.
   */
  Listeners listeners;

  /**
   * All service hooks
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

  BundleContextImpl systemBC;

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
  ServiceContentHandlerFactory   contentHandlerFactory;

  /**
   * Factory for handling service-based URLs
   */
  static ServiceURLStreamHandlerFactory systemUrlStreamHandlerFactory;

  /**
   * Factory for handling service-based URL contents
   */
  static ServiceContentHandlerFactory   systemContentHandlerFactory;


  /**
   * Cached value of
   * props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)
   * Used and updated by isValidEE()
   */
  private Set    eeCacheSet = new HashSet();
  private String eeCache = null;

  final private int id;

  public FWProps props;

  static int globalId = 1;

  /**
   * Contruct a framework context
   *
   */
  public FrameworkContext(Map initProps, FrameworkContext parent)  {
    props        = new FWProps(initProps, parent);
    if (setSecurityManager()) {
      perm = new SecurePermissionOps(this);
    } else {
      perm = new PermissionOps();
    }
    systemBundle = new SystemBundle(this);

    id = globalId++;

    log("created");
  }


  // Initialize the framework, see spec v4.2 sec 4.2.4
  void init()
  {
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
    selectBootDelegationParentClassLoader();

    perm.init();

    String v = props.getProperty("org.knopflerfish.framework.validator");
    ProtectionDomain pd = null;
    if (System.getSecurityManager() != null) {
      try {
        pd = getClass().getProtectionDomain();
      } catch (Throwable t) {
        if(props.debug.classLoader) {
          props.debug.println("Failed to get protection domain: " + t);
        }
      }
      if (v == null) {
        v = "JKSValidator";
      }
    }
    if (v != null && !v.equalsIgnoreCase("none") && !v.equalsIgnoreCase("null")) {
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
        } catch (Exception e) {
          throw new RuntimeException(vs + " is not a Validator", e);
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
            systemUrlStreamHandlerFactory = urlStreamHandlerFactory;
            systemContentHandlerFactory   = contentHandlerFactory;
          } catch (Throwable e) {
            props.debug.printStackTrace
              ("Cannot set global URL handlers, "
               +"continuing without OSGi service URL handler (" +e +")", e);
          }
        }
      } else {
        urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
        contentHandlerFactory   = new ServiceContentHandlerFactory(this);
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
    storage.setCheckSigned(validator != null);
    dataStorage       = Util.getFileStorage(this, "data");
    packages          = new Packages(this);

    listeners         = new Listeners(this, perm);
    services          = new Services(this, perm);

    systemBC          = new BundleContextImpl(systemBundle);
    systemBundle.setBundleContext(systemBC);

    bundles           = new Bundles(this);

    hooks             = new Hooks(this);    
    hooks.open();

    perm.registerService();

    String[] classes = new String [] { PackageAdmin.class.getName() };
    services.register(systemBundle,
                      classes,
                      new PackageAdminImpl(this),
                      null);

    // Add this framework to the bundle URL handle, now that we have the set of bundles
    urlStreamHandlerFactory.addFramework(this);

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
   */
  void uninit()
  {
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

    parentClassLoader = null;
    bootDelegationPatterns = null;
  }


  /**
   *
   */
  int getId() {
    return id;
  }


  private static final String POLICY_PROPERTY = "java.security.policy";

  /**
   *
   */
  private boolean setSecurityManager() {
    final String osgiSecurity = props.getProperty(Constants.FRAMEWORK_SECURITY);
    final boolean useOSGiSecurityManager
      = Constants.FRAMEWORK_SECURITY_OSGI.equals(osgiSecurity);

    if (useOSGiSecurityManager && null!=System.getSecurityManager()) {
      // NYI! Check if we have expected security manager
      throw new SecurityException
        ("Can not install OSGi security manager, another security manager "
         +"is already installed.");
    } else if (useOSGiSecurityManager) {
      final String defaultPolicy
        = this.getClass().getResource("/framework.policy").toString();
      final String policy
        = props.getProperty(POLICY_PROPERTY, defaultPolicy);
      if (props.debug.framework) {
        props.debug.println("Installing OSGi security manager, policy="+policy);
      }
      System.setProperty(POLICY_PROPERTY, policy);
      System.setSecurityManager(new KFSecurityManager());
      return true;
    } else {
      try {
        final String manager = props.getProperty("java.security.manager");
        final String policy  = props.getProperty(POLICY_PROPERTY);

        if(manager != null) {
          if(System.getSecurityManager() == null) {
            if (props.debug.framework) {
              props.debug.println("Installing security manager=" + manager +
                                  ", policy=" + policy);
            }
            // If policy was given as a framework property export it
            // as a system property.
            System.setProperty(POLICY_PROPERTY, policy);
            SecurityManager sm = null;
            if("".equals(manager)) {
              sm = new SecurityManager();
            } else {
              Class       clazz = Class.forName(manager);
              Constructor cons  = clazz.getConstructor(new Class[0]);

              sm = (SecurityManager)cons.newInstance(new Object[0]);
            }
            System.setSecurityManager(sm);
            return true;
          }
        }
      } catch (Exception e) {
        throw new IllegalArgumentException
          ("Failed to install security manager." + e);
      }
    }
    return false;
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

  ArrayList /* String */ bootDelegationPatterns;
  boolean bootDelegationUsed /*= false*/;

  void buildBootDelegationPatterns() {
    final String bootDelegationString
      = props.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);

    bootDelegationUsed = (bootDelegationString != null);
    bootDelegationPatterns = new ArrayList(1);

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
      listeners.frameworkError(systemBundle, e);
    }
  }

  boolean isBootDelegatedResource(String name) {
    // Convert resource name to class name format, preserving the
    // package part of the path/name.
    final int pos = name.lastIndexOf('/');
    return pos != -1
      ? isBootDelegated(name.substring(0,pos).replace('/','.')+".X")
      : false;
  }

  boolean isBootDelegated(String className){
    if(!bootDelegationUsed){
      return false;
    }
    final int pos = className.lastIndexOf('.');
    if (pos != -1) {
      if (bootDelegationPatterns == null) {
        return true;
      }
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

  /** The parent class loader for to be used by bundle classloaders. */
  ClassLoader parentClassLoader;
  void selectBootDelegationParentClassLoader() {
    final ArrayList cls = new ArrayList();
    ClassLoader cl = this.getClass().getClassLoader();
    cls.add(cl);
    while (null!=cl.getParent()) {
      cl = cl.getParent();
      cls.add(cl);
    }

    final String s = props.getProperty(Constants.FRAMEWORK_BUNDLE_PARENT);
    if (Constants.FRAMEWORK_BUNDLE_PARENT_EXT.equals(s)) {
      // Is this what the OSGi spec means by the "extension" loader?
      parentClassLoader = cls.size()>=1 ?
        (ClassLoader) cls.get(1) : (ClassLoader) cls.get(0);
    } else if (Constants.FRAMEWORK_BUNDLE_PARENT_APP.equals(s)) {
      // Is this what the OSGi spec means by the "application" loader?
      parentClassLoader = ClassLoader.getSystemClassLoader();
    } else if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equals(s)) {
      parentClassLoader = (ClassLoader) cls.get(0);
    } else { // Default: Constants.FRAMEWORK_BUNDLE_PARENT_BOOT
      parentClassLoader = (ClassLoader) cls.get(cls.size()-1);
    }
    cls.clear();
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
