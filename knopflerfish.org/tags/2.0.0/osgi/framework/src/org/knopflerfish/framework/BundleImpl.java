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
import java.net.*;
import java.security.*;

import java.util.Enumeration;
import java.util.Set;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.osgi.framework.*;


/**
 * Implementation of the Bundle object.
 *
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */

class BundleImpl implements Bundle {

  /**
   * Union of flags allowing bundle package access.
   * <p>
   * Value is <tt>Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING</tt>
   * </p>
   */
  static int RESOLVED_FLAGS = RESOLVED | STARTING | ACTIVE | STOPPING;

  /**
   * Framework for bundle.
   */
  final Framework framework;

  /**
   * Handle to secure operations.
   */
  final PermissionOps secure;

  /**
   * Bundle identifier.
   */
  final long id;

  /**
   * Bundle location identifier.
   */
  final String location;

  /**
   * Does bundle have a version 2 manifest.
   */
  boolean v2Manifest;

  /**
   * Bundle symbolic name.
   */
  String symbolicName;

  /**
   * Bundle is a singleton.
   */
  boolean singleton;

  /**
   * Bundle version.
   */
  Version version;

  /**
   * State of bundle.
   */
  int state;

  /**
   * Packages that the bundle wants to export and import.
   */
  BundlePackages bpkgs;

  /**
   * Bundle JAR data.
   */
  BundleArchive archive;

  /**
   * Generation of BundlePackages.
   */
  int generation = 0;

  /**
   * Bundle protect domain.
   */
  private ProtectionDomain protectionDomain;

  /**
   * Classloader for bundle.
   */
  private ClassLoader classLoader = null;

  /**
   * Zombie packages for bundle.
   */
  private HashMap /* BundlePackages -> BundleClassLoader */ oldClassLoaders = null;

  /**
   * Directory for bundle data.
   */
  protected FileTree bundleDir = null;

  /**
   * BundleContext for bundle.
   */
  protected BundleContextImpl bundleContext = null;

  /**
   * BundleActivator for bundle.
   */
  protected BundleActivator bactivator = null;

  /**
   * Time when bundle was last modified.
   *
   */
  protected long lastModified;

  /**
   * Set to true of bundle.start() has been called but
   * current start levels was too low to actually start the bundle.
   */
  boolean bDelayedStart = false;

  /**
   * All fragment bundles this bundle hosts.
   */
  ArrayList fragments = null;
 
  /**
   * This bundle's fragment attach policy.
   */
  String attachPolicy;
  
  /**
   * Fragment description. This is null when the bundle isn't
   * a fragment bundle.
   */
  Fragment fragment = null;

  /**
   * Stores the default locale entries when uninstalled.
   */
  private HeaderDictionary cachedHeaders = null;
  
  /**
   * Stores the raw manifest headers.
   */
  private HeaderDictionary cachedRawHeaders = null;

  
  /**
   * Construct a new Bundle empty.
   *
   * Only called for system bundle
   *
   * @param fw Framework for this bundle.
   */
  BundleImpl(Framework fw, long id, String loc, ProtectionDomain pd, String sym, Version ver) {
    this.framework = fw;
    this.secure = fw.perm;
    this.id = id;
    this.location = loc;
    this.protectionDomain = pd;
    this.symbolicName = sym;
    this.singleton = false;
    this.version = ver;
    this.v2Manifest = true;
    this.attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
    modified();
  }

  /**
   * Construct a new Bundle based on a BundleArchive.
   *
   * @param bundlesDir Directory where to store the bundles all persistent data.
   * @param fw Framework for this bundle.
   * @param loc Location for new bundle.
   * @param in Bundle JAR as an inputstream.
   * @exception IOException If we fail to read and store our JAR bundle or if
   *            the input data is corrupted.
   * @exception SecurityException If we don't have permission to import and export
   *            bundle packages.
   */
  BundleImpl(Framework fw, BundleArchive ba) {
    framework = fw;
    secure = fw.perm;
    id = ba.getBundleId();
    location = ba.getBundleLocation();
    archive = ba;
    state = INSTALLED;
    checkManifestHeaders();
    protectionDomain = secure.getProtectionDomain(this);
    doExportImport();
    bundleDir = fw.getDataStorage(id);

    int oldStartLevel = archive.getStartLevel();
    try {
      if (framework.startLevelService == null) {
        archive.setStartLevel(0);
      } else {
        if (oldStartLevel == -1) {
          archive.setStartLevel(framework.startLevelService.getInitialBundleStartLevel());
        }
      }
    } catch (Exception e) {
      Debug.println("Failed to set start level on #" + id + ": " + e);
    }

    // Activate extension as soon as they are installed so that
    // they get added in bundle id order.
    if (isExtension() && resolveFragment(framework.systemBundle)) {
      state = RESOLVED;
    }

    lastModified = archive.getLastModified();
    if (lastModified == 0) {
      modified();
    }
  }


  //
  // Bundle interface
  //

  /**
   * Get bundle state.
   *
   * @see org.osgi.framework.Bundle#getState
   */
  public int getState() {
    return state;
  }


  /**
   * Start this bundle.
   *
   * @see org.osgi.framework.Bundle#start
   */
  synchronized public void start() throws BundleException {
    secure.checkExecuteAdminPerm(this);

    if (isFragment()) {
      throw new BundleException("Cannot start a fragment bundle");
    }

    if (framework.startLevelService != null) {
      if (state != UNINSTALLED &&
         getStartLevel() > framework.startLevelService.getStartLevel()) {
        secure.callSetPersistent(this, true);
        bDelayedStart = true;
        return;
      }
    }

    switch (getUpdatedState()) {
    case INSTALLED:
      throw new BundleException("Failed, " + bpkgs.getResolveFailReason());
    case RESOLVED:
      if (framework.active) {
        state = STARTING;
        framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTING, this));
        bundleContext = new BundleContextImpl(this);
        try {
          secure.callStart0(this);
        } catch (BundleException e) {
          removeBundleResources();
          bundleContext.invalidate();
          bundleContext = null;
          state = RESOLVED;
          throw e;
        }
        framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTED, this));
      } else {
        secure.callSetPersistent(this, true);
        startOnLaunch(true);
      }
      break;
    case ACTIVE:
      break;
    case STARTING:
      // This happens if call start from inside the BundleActivator.start method.
      // Don't allow it.
      throw new BundleException("called from BundleActivator.start");
    case STOPPING:
      // This happens if call start from inside the BundleActivator.stop method.
      // Don't allow it.
      throw new BundleException("called from BundleActivator.stop");
    case UNINSTALLED:
      throw new IllegalStateException("Bundle is in UNINSTALLED state");
    }
  }


  void start0() throws BundleException {
    final String ba = archive.getAttribute(Constants.BUNDLE_ACTIVATOR);
    boolean bStarted = false;

    ClassLoader oldLoader = null;

    if (Framework.SETCONTEXTCLASSLOADER) {
      oldLoader = Thread.currentThread().getContextClassLoader();
    }

    try {
      // If SETCONTEXTCLASSLOADER, set the thread's context
      // class loader to the bundle class loader. This
      // is useful for debugging external libs using
      // the context class loader.
      if (Framework.SETCONTEXTCLASSLOADER) {
        Thread.currentThread().setContextClassLoader(getClassLoader());
      }

      if (ba != null) {
        Class c = getClassLoader().loadClass(ba.trim());
        bactivator = (BundleActivator)c.newInstance();

        bactivator.start(bundleContext);
        bStarted = true;
      } else {
        // Check if we have a standard Main-class attribute as
        // in normal executable jar files. This is a slight
        // extension to the OSGi spec.
        final String mc = archive.getAttribute("Main-class");

        if (mc != null) {
          if(Debug.packages) {
            Debug.println("starting main class " + mc);
          }
          Class mainClass = getClassLoader().loadClass(mc.trim());

          bactivator = MainClassBundleActivator.create(getClassLoader(), mainClass);
          bactivator.start(bundleContext);
          bStarted = true;
        }
      }

      if (!bStarted) {
        // Even bundles without an activator are marked as
        // ACTIVE.
        // Should we possible log an information message to
        // make sure users are aware of the missing activator?
      }

      state = ACTIVE;
      setPersistent(true);
      startOnLaunch(true);
    } catch (Throwable t) {
      throw new BundleException("BundleActivator start failed", t);
    } finally {
      if (Framework.SETCONTEXTCLASSLOADER) {
        Thread.currentThread().setContextClassLoader(oldLoader);
      }
    }
  }


  /**
   * Check if setStartOnLaunch(false) is allowed.
   */
  boolean allowSetStartOnLaunchFalse() {
    /* boolean bCompat =
       framework.startLevelService == null ||
       framework.startLevelService.bCompat;
    */
    return
      // never never touch on FW shutdown
      !framework.shuttingdown && !archive.isPersistent();

    /*
      &&

      // allow touch if in startlevel compatibility mode
      (bCompat ||
      // ...also allow touch if not marked as persistant startlevel active
      !isPersistent());
    */
  }

  /**
   * Stop this bundle.
   *
   * @see org.osgi.framework.Bundle#stop
   */
  synchronized public void stop() throws BundleException {
    secure.checkExecuteAdminPerm(this);

    if (isFragment()) {
      throw new BundleException("Cannot stop a fragment bundle");
    }

    bDelayedStart = false;

    switch (state) {
    case INSTALLED:
    case RESOLVED:
      secure.callSetPersistent(this, false);
      // We don't want this bundle to start on launch after it has been
      // stopped. (Don't apply during shutdown
      if (allowSetStartOnLaunchFalse()) {
        secure.callStartOnLaunch(this, false);
      }
      break;
    case ACTIVE:
      BundleException savedException = secure.callStop0(this, true);
      if (savedException != null) {
        throw savedException;
      }
      break;
    case STARTING:
      // This happens if we call stop from inside the BundleActivator.start method.
      // We don't allow it.
      throw new BundleException("Bundle.start called from BundleActivator.stop");
    case STOPPING:
      // This happens if we call stop from inside the BundleActivator.stop method.
      // We don't allow it.
      throw new BundleException("Bundle.stop called from BundleActivator.stop");
    case UNINSTALLED:
      throw new IllegalStateException("Bundle.stop: Bundle is in UNINSTALLED state");
    }
  }

  synchronized BundleException stop0(boolean resetPersistent) {
    BundleException res = null;

    state = STOPPING;
    framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));

    if (resetPersistent) {
      setPersistent(false);
    }

    if (allowSetStartOnLaunchFalse()) {
      startOnLaunch(false);
    }
    if (bactivator != null) {
      try {
        bactivator.stop(bundleContext);
      } catch (Throwable e) {
        res = new BundleException("Bundle.stop: BundleActivator stop failed", e);
      }
      bactivator = null;
    }

    bundleContext.invalidate();
    bundleContext = null;
    removeBundleResources();
    state = RESOLVED;
    framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPED, this));
    return res;
  }


  /**
   * Update this bundle.
   *
   * @see org.osgi.framework.Bundle#update
   */
  public void update() throws BundleException {
    update(null);
  }


  /**
   * Update this bundle.
   *
   * @see org.osgi.framework.Bundle#update
   */
  synchronized public void update(final InputStream in) throws BundleException {
    try {
      secure.checkLifecycleAdminPerm(this);
      if (isExtension()) {
        secure.checkExtensionLifecycleAdminPerm(this);
      }
      final boolean wasActive = state == ACTIVE;

      switch (getUpdatedState()) {
      case ACTIVE:
        stop();
        // Fall through
      case RESOLVED:
      case INSTALLED:
        // Load new bundle
        secure.callUpdate0(this, in, wasActive);
        break;
      case STARTING:
        // Wait for RUNNING state, this doesn't happen now
        // since we are synchronized.
        throw new IllegalStateException("Bundle is in STARTING state");
      case STOPPING:
        // Wait for RESOLVED state, this doesn't happen now
        // since we are synchronized.
        throw new IllegalStateException("Bundle is in STOPPING state");
      case UNINSTALLED:
        throw new IllegalStateException("Bundle is in UNINSTALLED state");
      }
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) {}
      }

    }
  }


  void update0(InputStream in, boolean wasActive) throws BundleException {
    final boolean wasResolved = state == RESOLVED;
    final int oldStartLevel = getStartLevel();
    BundleArchive newArchive = null;
    //HeaderDictionary newHeaders;
    try {
      // New bundle as stream supplied?
      InputStream bin;
      if (in == null) {
        // Try Bundle-UpdateLocation
        String update = archive.getAttribute(Constants.BUNDLE_UPDATELOCATION);
        if (update == null) {
          // Take original location
          update = location;
        }
        bin = (new URL(update)).openStream();
      } else {
        bin = in;
      }

      newArchive = framework.storage.updateBundleArchive(archive, bin);
      checkEE(newArchive);
      checkManifestHeaders();
      newArchive.setStartLevel(oldStartLevel);
      framework.storage.replaceBundleArchive(archive, newArchive);
    } catch (Exception e) {
      if (newArchive != null) {
        newArchive.purge();
      }
                  
      if (wasActive) {
        try {
          start();
        } catch (BundleException be) {
          framework.listeners.frameworkError(this, be);
        }
      }
      if (e instanceof BundleException) {
        throw (BundleException)e;
      } else {
        throw new BundleException("Failed to get update bundle", e);
      }
    }

    boolean purgeOld;

    if (isFragment()) {
      if (isAttached()) {
        fragment.setHost(null);
        purgeOld = false;
      } else {
        purgeOld = true;
      }
    } else {
      // Remove this bundle's packages
      boolean allRemoved = bpkgs.unregisterPackages(false);

      // Loose old bundle if no exporting packages left
      if (allRemoved) {
        if (classLoader != null) {
          ((BundleClassLoader)classLoader).close();
          classLoader = null;
        }
        purgeOld = true;
      } else {
        saveZombiePackages();
        purgeOld = false;
      }
    }

    // Activate new bundle
    BundleArchive oldArchive = archive;
    archive = newArchive;
    cachedRawHeaders = null;
    state = INSTALLED;
    ProtectionDomain oldProtectionDomain = protectionDomain;
    protectionDomain = secure.getProtectionDomain(this);
    doExportImport();

    // Purge old archive
    if (purgeOld) {
      secure.purge(this, oldProtectionDomain);
      oldArchive.purge();
    }

    // Broadcast updated event
    if (wasResolved) {
      framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED,
                                                        this));
    }
    framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UPDATED,
                                                      this));

    // Restart bundles previously stopped in the operation
    if (wasActive) {
      try {
        start();
      } catch (BundleException be) {
        framework.listeners.frameworkError(this, be);
      }
    }
    //only when complete success
    modified();
  }


  void checkEE(BundleArchive ba) throws BundleException {
    String ee = ba.getAttribute(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
    if (ee != null) {
      if (Debug.packages) {
        Debug.println("bundle #" + ba.getBundleId() + " has EE=" + ee);
      }
      if (!framework.isValidEE(ee)) {
        throw new BundleException("Execution environment '" + ee + "' is not supported");
      }
    }
  }


  /**
   * Uninstall this bundle.
   *
   * @see org.osgi.framework.Bundle#uninstall
   */
  synchronized public void uninstall() throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    if (isExtension()) {
      secure.checkExtensionLifecycleAdminPerm(this);
    }
    secure.callUninstall0(this);
  }

  void uninstall0() {
    boolean wasResolved = false;

    try {
      archive.setStartLevel(-2); // Mark as uninstalled
    } catch (Exception ignored) {   }

    cachedHeaders = getHeaders0(null);

    bDelayedStart = false;
  
    switch (state) {
    case ACTIVE:
      try {
        stop();
      } catch (BundleException be) {
        framework.listeners.frameworkError(this, be);
      }
      // Fall through
    case RESOLVED:
      wasResolved = true;
      // Fall through
    case INSTALLED:

      framework.bundles.remove(location);

      if (isFragment()) {
        if (isAttached()) {
          classLoader = null;
          fragment.setHost(null);
        } else {
          secure.purge(this, protectionDomain);
          archive.purge();
        }
      } else {
        if (bpkgs.unregisterPackages(false)) {
          if (classLoader != null) {
            ((BundleClassLoader)classLoader).purge();
            classLoader = null;
          } else {
            secure.purge(this, protectionDomain);
            archive.purge();
          }
        } else {
          saveZombiePackages();
          classLoader = null;
        }
        if (isFragmentHost()) {
          detachFragments(true);
        }
      }

      bpkgs = null;
      bactivator = null;
      if (bundleDir != null) {
        if (!bundleDir.delete()) {
          // Bundle dir is not deleted completely, make sure we mark
          // it as uninstalled for next framework restart
          try {
            archive.setStartLevel(-2); // Mark as uninstalled
          } catch (Exception e) {
            Debug.println("Failed to mark bundle " + id +
                          " as uninstalled, " + bundleDir +
                          " must be deleted manually: " + e);
          }
        }
        bundleDir = null;
      }

      // id, location and headers survices after uninstall.
      state = UNINSTALLED;
      modified();

      if (wasResolved) {
        framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
      }
      framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, this));
      break;
    case STARTING:
      // Wait for RUNNING state, this doesn't happen now
      // since we are synchronized.
      throw new IllegalStateException("Bundle is in STARTING state");
    case STOPPING:
      // Wait for RESOLVED state, this doesn't happen now
      // since we are synchronized.
      throw new IllegalStateException("Bundle is in STOPPING state");
    case UNINSTALLED:
      throw new IllegalStateException("Bundle is in UNINSTALLED state");
    }
  }


  /**
   * Get header data. This is all entries in the bundles
   * MANIFEST file.
   *
   * @see org.osgi.framework.Bundle#getHeaders
   */
  public Dictionary getHeaders() {
    return getHeaders(null);
  }


  /**
   * Get bundle identifier.
   *
   * @see org.osgi.framework.Bundle#getBundleId
   */
  public long getBundleId() {
    return id;
  }


  /**
   * Get bundle location.
   *
   * @see org.osgi.framework.Bundle#getLocation
   */
  public String getLocation() {
    secure.checkMetadataAdminPerm(this);
    return location;
  }


  /**
   * Get services that this bundle has registrated.
   *
   * @see org.osgi.framework.Bundle#getRegisteredServices
   */
  public ServiceReference[] getRegisteredServices() {
    Set sr = framework.services.getRegisteredByBundle(this);
    secure.filterGetServicePermission(sr);
    ServiceReference[] res = new ServiceReference[sr.size()];
    int pos = 0;
    for (Iterator i = sr.iterator(); i.hasNext(); ) {
      res[pos++] = ((ServiceRegistration)i.next()).getReference();
    }
    return res;
  }


  /**
   * Get services that this bundle uses.
   *
   * @see org.osgi.framework.Bundle#getServicesInUse
   */
  public ServiceReference[] getServicesInUse() {
    Set sr = framework.services.getUsedByBundle(this);
    secure.filterGetServicePermission(sr);
    ServiceReference[] res = new ServiceReference[sr.size()];
    int pos = 0;
    for (Iterator i = sr.iterator(); i.hasNext(); ) {
      res[pos++] = ((ServiceRegistration)i.next()).getReference();
    }
    return res;
  }


  /**
   * Determine whether the bundle has the requested permission.
   *
   * @see org.osgi.framework.Bundle#hasPermission
   */
  public boolean hasPermission(Object permission) {
    checkUninstalled();
    if (permission instanceof Permission) {
      if (secure.checkPermissions()) {
        //get the current status from permission admin
	PermissionCollection pc = protectionDomain.getPermissions();
        return pc != null ? pc.implies((Permission)permission) : false;
      } else {
        return true;
      }
    } else {
      return false;
    }
  }


  /**
   * @see org.osgi.framework.Bundle#getResource(String name)
   */
  public URL getResource(String name) {
    // ResourceAdminPermission checked in the classloader.
    checkUninstalled();
    if (isFragment()) {
      return null;
    }
    if (state == INSTALLED && !secure.okResourceAdminPerm(this)) {
      // We don't want to create a classloader unless we have permission to.
      return null;
    }
    if (getUpdatedState() != INSTALLED) {
      BundleClassLoader cl = (BundleClassLoader)getClassLoader();
      if (cl != null) {
        Enumeration res = cl.getBundleResources(name, true);
        if (res != null) {
          return (URL)res.nextElement();
        }
      }
    }
    return null;
  }


  /**
   * @see org.osgi.framework.Bundle#getSymbolicName()
   */
  public String getSymbolicName() {
    return symbolicName;
  }


  //
  // Package methods
  //

  /**
   * Get updated bundle state. That means check if an installed
   * bundle has been resolved.
   *
   * @return Bundles state
   */
  int getUpdatedState() {
    if (state == INSTALLED) {
      synchronized (this) {
        if (state == INSTALLED) {
          if (isFragment()) {
            BundleImpl host = getFragmentHost();
            if (host != null) {
              if (host.state == INSTALLED) {
                // Try resolve our host
                host.getUpdatedState();
              } else {
                // NYI! dynamic attach?
              }
            }
          } else {
            // TODO, should we do this as a part of package resolving.
            attachFragments();
            if (bpkgs.resolvePackages()) {
              if (fragments != null) {
                for (Iterator i = fragments.iterator(); i.hasNext(); ) {
                  BundleImpl b = (BundleImpl)i.next();
                  b.state = RESOLVED;
                }
              }
              state = RESOLVED;
              if (fragments != null) {
                for (Iterator i = fragments.iterator(); i.hasNext(); ) {
                  BundleImpl b = (BundleImpl)i.next();
                  framework.listeners.bundleChanged(new BundleEvent(BundleEvent.RESOLVED, b));
                }
              }
              framework.listeners.bundleChanged(new BundleEvent(BundleEvent.RESOLVED, this));

              if (id != 0) { // this is not applicable to system bundle.
                List fe = archive.getFailedClassPathEntries();
                if (fe != null) {
                  for (Iterator i = fe.iterator(); i.hasNext(); ) {
                    Exception e = new IOException("Failed to classpath entry: " + i.next());
                    framework.listeners.frameworkInfo(this, e);
                  }
                }
              }
            } else {
              detachFragments(false);
            }
          } 
        }
      }
    }
    return state;
  }


  /**
   * Resolve fragment
   */
  boolean resolveFragment(BundleImpl host) {
    if (host == getFragmentHost() && secure.okFragmentBundlePerm(this)) {
      try {
        host.attachFragment(this);
        fragment.setHost(host);
        return true;
      } catch (Exception _ignore) { }
    }
    // TODO, Log this?
    return false;
  }


  /**
   * Get root for persistent storage area for this bundle.
   *
   * @return A File object representing the data root.
   */
  File getDataRoot() {
    return bundleDir;
  }


  /**
   * Get class loader for this bundle.
   * Create the classloader if we haven't done this previously.
   * This method can only be called when the bundle is resolved.
   *
   * @return Bundles classloader.
   */
  ClassLoader getClassLoader() {
    if (classLoader == null) {
      synchronized (this) {
        if (classLoader == null && (state & RESOLVED_FLAGS) != 0) {
          classLoader = secure.callGetClassLoader0(this);
        }
      }
    }
    return classLoader;
  }


  ClassLoader getClassLoader0() {
    if (isFragment()) {
      if (isAttached()) {
        if (isBootClassPathExtension()) {
          ClassLoader root = ClassLoader.getSystemClassLoader();
          while (root.getParent() != null) {
            root = root.getParent();
          }
          return root;
        } else {
          return getFragmentHost().getClassLoader();
        }
      }
      return null;
    } else {
      ArrayList frags;
      if (isFragmentHost()) {
        frags = new ArrayList();
        for (Iterator i = fragments.iterator(); i.hasNext(); ) {
          frags.add(((BundleImpl)i.next()).archive);
        }
      } else {
        frags = null;
      }
      return new BundleClassLoader(bpkgs, archive, frags, protectionDomain, secure);
    }
  }


  /**
   * Set state to INSTALLED and throw away our classloader.
   * Reset all package registration.
   * We assume that the bundle is resolved when entering this method.
   */
  synchronized void setStateInstalled(boolean sendEvent) {
    if (isFragment()) {
      classLoader = null;
      fragment.setHost(null);
    } else {
      if (classLoader != null) {
        ((BundleClassLoader)classLoader).close();
        classLoader = null;
      }
      bpkgs.unregisterPackages(true);
      if (isFragmentHost()) {
        detachFragments(true);
      }
      bpkgs.registerPackages();
    }
    
    state = INSTALLED;
    if (sendEvent) {
      framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
    }
  }


  /**
   * Get the BundleClassLoader object for one of our BundlePackages.
   *
   * @param ebpkgs BundlePackages to get class loader for.
   * @return BundleClassLoader object for specified pkg, null if no classloader.
   */
  ClassLoader getClassLoader(BundlePackages ebpkgs) {
    if (bpkgs == ebpkgs) {
      return getClassLoader();
    } else if (oldClassLoaders != null) {
      return (ClassLoader)oldClassLoaders.get(ebpkgs);
    }
    return null;
  }


  /**
   * Purge any old files associated with this bundle.
   *
   */
  void purge() {
    if (state == UNINSTALLED) {
      framework.bundles.remove(location);
    }
    if (oldClassLoaders != null) {
      for (Iterator i = oldClassLoaders.values().iterator(); i.hasNext();) {
        ((BundleClassLoader)i.next()).purge();
      }
      oldClassLoaders = null;
    }
  }


  /**
   * Get bundle archive.
   *
   * @return BundleArchive object.
   */
  BundleArchive getBundleArchive(long gen, long frag) {
    // TODO, maybe we should always specify generation and return null if they don't match
    if (gen == -1 || (bpkgs != null && bpkgs.generation == gen)) {
      if (frag == -1) {
        return archive;
      } else {
        return ((BundleClassLoader)getClassLoader()).getBundleArchive(frag);
      }
    } else {
      for (Iterator i = oldClassLoaders.values().iterator(); i.hasNext();) {
        BundleClassLoader cl = (BundleClassLoader)i.next();
        if (cl.getBpkgs().generation == gen) {
          return cl.getBundleArchive(frag);
        }
      }
      return null;
    }
  }


  /**
   * Get exported packages.
   *
   * @return Iterator of all exported packages as ExportPkg.
   */
  Iterator getExports() {
    if (oldClassLoaders != null) {
      HashSet res = new HashSet();
      for (Iterator i = oldClassLoaders.values().iterator(); i.hasNext();) {
        for (Iterator j = ((BundleClassLoader)i.next()).getBpkgs().getExports(); j.hasNext();) {
          res.add(j.next());
        }
      }
      if (bpkgs != null) {
        for (Iterator i = bpkgs.getExports(); i.hasNext();) {
          res.add(i.next());
        }
      }
      return res.iterator();
    } else if (bpkgs != null) {
      return bpkgs.getExports();
    } else {
      return (new ArrayList(0)).iterator();
    }
  }


  /**
   * Get imported packages.
   *
   * @return Iterator of all imported packages as ImportPkg.
   */
  Iterator getImports() {
    if (oldClassLoaders != null) {
      HashSet res = new HashSet();
      for (Iterator i = oldClassLoaders.values().iterator(); i.hasNext();) {
        for (Iterator j = ((BundleClassLoader)i.next()).getBpkgs().getImports(); j.hasNext();) {
          res.add(j.next());
        }
      }
      if (bpkgs != null) {
        for (Iterator i = bpkgs.getImports(); i.hasNext();) {
          res.add(i.next());
        }
      }
      return res.iterator();
    } else if (bpkgs != null) {
      return bpkgs.getImports();
    } else {
      return (new ArrayList(0)).iterator();
    }
  }


  /**
   * Construct URL to bundle resource
   */
  URL getURL(long gen, long frag, int bcpElem, String path) {
    try {
      StringBuffer u = new StringBuffer(BundleURLStreamHandler.PROTOCOL);
      u.append("://");
      u.append(id);
      if (gen != -1) {
        u.append('.').append(gen);
      }
      if (frag != -1 && frag != id) {
        u.append('_').append(frag);
      }
      if (bcpElem >= 0) {
        u.append(':').append(bcpElem);
      }
      if (!path.startsWith("/")) {
        u.append('/');
      }
      u.append(path);
      return new URL(u.toString());
    } catch (MalformedURLException e) {
      return null;
    }
  }


  //
  // Private methods
  //

  /**
   * Cache certain manifest headers as variables.
   */
  private void checkManifestHeaders() {
    // TBD, v2Manifest unnecessary to cache?
    v2Manifest = "2".equals(archive.getAttribute(Constants.BUNDLE_MANIFESTVERSION));
    Iterator i = Util.parseEntries(Constants.BUNDLE_SYMBOLICNAME,
                                   archive.getAttribute(Constants.BUNDLE_SYMBOLICNAME),
                                   true, true, true);
    Map e = null;
    if (i.hasNext()) {
      e = (Map)i.next();
      symbolicName = (String)e.get("key");
    } else {
      if (v2Manifest) {
        throw new IllegalArgumentException("Bundle has no symbolic name, location=" +
                                           location);
      } else {
        symbolicName = null;
      }
    }
    String mbv = archive.getAttribute(Constants.BUNDLE_VERSION);
    if (mbv != null) {
      version = new Version(mbv);
    } else {
      version = Version.emptyVersion;
    }

    attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
    if (e != null) {
      singleton = "true".equals((String)e.get(Constants.SINGLETON_DIRECTIVE));
      BundleImpl snb = framework.bundles.getBundle(symbolicName, version);
      String tmp = (String)e.get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
      attachPolicy = tmp == null ? Constants.FRAGMENT_ATTACHMENT_ALWAYS : tmp;
      // TBD! Should we allow update to same version?
      if (snb != null && snb != this) {
        throw new IllegalArgumentException("Bundle with same symbolic name and version " +
                                           "is already installed (" + symbolicName + ", " +
                                           version);
      }
    } else {
      singleton = false;
    }

    i = Util.parseEntries(Constants.FRAGMENT_HOST,
                          archive.getAttribute(Constants.FRAGMENT_HOST),
                          true, true, true);
    
    if (i.hasNext()) {
      
      if (archive.getAttribute(Constants.BUNDLE_ACTIVATOR) != null) {
        throw new IllegalArgumentException("A fragment bundle can not have a Bundle-Activator.");
      }
      
      e = (Map)i.next();
      String extension = (String)e.get(Constants.EXTENSION_DIRECTIVE);
      String key = (String)e.get("key");
      
      if (Constants.EXTENSION_FRAMEWORK.equals(extension) ||
          Constants.EXTENSION_BOOTCLASSPATH.equals(extension)) {
        
        // an extension bundle must target the system bundle.  
        if (!Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(key) && 
            !"org.knopflerfish.framework".equals(key)) {
          throw new IllegalArgumentException("An extension bundle must target " +
                                             "the system bundle(=" +
                                             Constants.SYSTEM_BUNDLE_SYMBOLICNAME + ")");
        }
        
        if (archive.getAttribute(Constants.IMPORT_PACKAGE) != null ||
            archive.getAttribute(Constants.REQUIRE_BUNDLE) != null ||
            archive.getAttribute(Constants.BUNDLE_NATIVECODE) != null ||
            archive.getAttribute(Constants.DYNAMICIMPORT_PACKAGE) != null ||
            archive.getAttribute(Constants.BUNDLE_ACTIVATOR) != null) {
          throw new IllegalArgumentException("An extension bundle cannot specify: " +
                                             Constants.IMPORT_PACKAGE + ", " +
                                             Constants.REQUIRE_BUNDLE + ", " +
                                             Constants.BUNDLE_NATIVECODE + ", " +
                                             Constants.DYNAMICIMPORT_PACKAGE + " or " +
                                             Constants.BUNDLE_ACTIVATOR);
        }
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
      } else {
        if (extension != null) {
          throw new IllegalArgumentException("Did not recognize directive " + 
                                             Constants.EXTENSION_DIRECTIVE
                                             + ":=" + extension + "." );
        }
      }
      
      if (fragment == null) {
        fragment = new Fragment(key,
                                extension,
                                (String)e.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
      }
    }
  }


  /**
   * Save the start on launch flag to the persistent bundle storage.
   *
   * @param value Boolean state for start on launch flag.
   */
  void startOnLaunch(boolean value) {
    try {
      archive.setStartOnLaunchFlag(value);
    } catch (IOException e) {
      framework.listeners.frameworkError(this, e);
    }
  }


  /**
   *
   */
  void setPersistent(final boolean value) {
    try {
      archive.setPersistent(value);
    } catch (Exception e) {
      framework.listeners.frameworkError(this, e);
    }
  }


  /**
   * Look at our manifest and register all our import and export
   * packages.
   *
   */
  void doExportImport() {
    bpkgs = new BundlePackages(this,
                               generation++,
                               archive.getAttribute(Constants.EXPORT_PACKAGE),
                               archive.getAttribute(Constants.IMPORT_PACKAGE),
                               archive.getAttribute(Constants.DYNAMICIMPORT_PACKAGE),
                               archive.getAttribute(Constants.REQUIRE_BUNDLE));
    if (!isFragment()) {
      // fragments don't export anything themselves.
      bpkgs.registerPackages();
    }
  }


  /**
   * Remove a bundles all registered listeners, registered services and
   * used services.
   *
   */
  private void removeBundleResources() {
    framework.listeners.removeAllListeners(this);
    Set srs = framework.services.getRegisteredByBundle(this);
    for (Iterator i = srs.iterator(); i.hasNext();) {
      try {
        ((ServiceRegistration)i.next()).unregister();
      } catch (IllegalStateException ignore) {
        // Someone has unregistered the service after stop completed.
        // This should not occur, but we don't want get stuck in
        // an illegal state so we catch it.
      }
    }
    Set s = framework.services.getUsedByBundle(this);
    for (Iterator i = s.iterator(); i.hasNext(); ) {
      ((ServiceRegistrationImpl) i.next()).reference.ungetService(this, false);
    }
  }


  /**
   * Save classloader for active package exports.
   *
   */
  private void saveZombiePackages() {
    if (oldClassLoaders == null) {
      oldClassLoaders = new HashMap();
    }
    oldClassLoaders.put(bpkgs, getClassLoader());
    classLoader = null;
  }


  // Start level related

  /**
   *
   */
  boolean isPersistent() {
    return bDelayedStart || archive.isPersistent();
  }


  /**
   *
   */
  int getStartLevel() {
    if (archive != null) {
      return archive.getStartLevel();
    } else {
      return 0;
    }
  }


  /**
   *
   */
  void setStartLevel(int n) {
    // as soon as anoyone sets the start level explicitly
    // the level becomes persistent

    if(archive != null) {
      try {
        archive.setStartLevel(n);
      } catch (Exception e) {
        Debug.println("Failed to set start level on #" + getBundleId());
      }
    }
  }

  // Misc other

  /**
   * Return a string representing this bundle. Only return
   * identifier, since it requires AdminPermisson to get
   * the location.
   *
   * @return a String representing this bundle.
   */
  public String toString() {
    return toString(0); // 0 is lowest detail
  }

  String toString(int detail) {
    StringBuffer sb = new StringBuffer();

    sb.append("BundleImpl[");
    sb.append("id=" + getBundleId());
    if(detail > 0) {
      sb.append(", state=" + getState());
    }

    if(detail > 1) {
      sb.append(", startlevel=" + getStartLevel());
    }

    if(detail > 3) {
      sb.append(", bDelayedStart=" + bDelayedStart);
    }

    if(detail > 4) {
      try {
        sb.append(", bPersistant=" + isPersistent());
      }  catch (Exception e) {
        sb.append(", bPersistant=" + e);
      }
    }
    if(detail > 4) {
      sb.append(", loc=" + location);
    }

    if(detail > 4) {
      sb.append(", symName=" + symbolicName);
    }

    sb.append("]");

    return sb.toString();
  }


  /**
   * Get bundle data. Get resources from bundle or fragment jars.
   *
   * @see org.osgi.framework.Bundle#findEntries
   */
  public Enumeration findEntries(String path, String filePattern, boolean recurse) {
    if (secure.okResourceAdminPerm(this)) {
      if (state == INSTALLED) {
        // We need to resolve if there are fragments involved
        if (!framework.bundles.getFragmentBundles(this).isEmpty()) {
          getUpdatedState();
        }
      }
      return secure.callFindEntries0(this, path, filePattern, recurse);
    } else {
      return null;
    }
  }


  /**
   *
   */
  Enumeration findEntries0(String path, String filePattern, boolean recurse) {
    Vector res = new Vector();
      if (isFragmentHost()) {
        for (Iterator i = fragments.iterator(); i.hasNext(); ) {
          BundleImpl fb = (BundleImpl)i.next();
          fb.addResourceEntries(res, path, filePattern, recurse);
        }
      }
      addResourceEntries(res, path, filePattern, recurse);
      return res.size() != 0 ? res.elements() : null;
  }


  /**
   *
   */
  void addResourceEntries(Vector res, String path, String pattern, boolean recurse) {
    Enumeration e = archive.findResourcesPath(path);
    if (e != null) {
      while (e.hasMoreElements()) {
        String fp = (String)e.nextElement();
        if (fp.endsWith("/")) {
          if (recurse) {
            addResourceEntries(res, fp, pattern, recurse);
          }
        } else {
          int l = fp.lastIndexOf('/');
          if (pattern == null || Util.filterMatch(pattern, fp.substring(l + 1))) {
            URL url = getURL(-1, -1, -1, fp);
            if (url != null) {
              res.add(url);
            }
          }
        }
      }
    }
  }


  /**
   *
   */
  public URL getEntry(String name) {
    if (secure.okResourceAdminPerm(this)) {
      checkUninstalled();
      try {
        InputStream is = secure.callGetInputStream(archive, name, 0);
        if (is != null) {
          is.close();
          return getURL(-1, -1, -1, name);
        }
      } catch (IOException _ignore) { }
    }
    return null;
  }


  /**
   *
   */
  public Enumeration getEntryPaths(String path) {
    if (secure.okResourceAdminPerm(this)) {
      checkUninstalled();
      return secure.callFindResourcesPath(archive, path);
    } else {
      return null;
    }
  }


  /**
   * Get locale dictionary for this bundle.
   */
  private Dictionary getLocaleDictionary(String locale, String baseName) {
    String defaultLocale = Locale.getDefault().toString();

    if (locale == null) {
      locale = defaultLocale;
    } else if (locale.equals("")) {
      return null;
    } 

    Hashtable localization_entries = new Hashtable();
    readLocalization("", localization_entries, baseName);
    readLocalization(Locale.getDefault().toString(), localization_entries, baseName);
    if (!locale.equals(defaultLocale)) {
      readLocalization(locale, localization_entries, baseName);
    } 
    
    return localization_entries;
  }


  /**
   * "Localizes" this bundle's headers
   * @param localization_entries A mapping of localization variables to values. 
   * @returns the updated dictionary.
   */
  private HeaderDictionary localize(Dictionary localization_entries) {
    HeaderDictionary localized = (HeaderDictionary)cachedRawHeaders.clone();
    
    if (localization_entries != null) {
      for (Enumeration e = localized.keys();
           e.hasMoreElements(); ) {
        String key = (String)e.nextElement();
        String unlocalizedEntry = (String)localized.get(key);

        if (unlocalizedEntry.startsWith("%")) {
          String k = unlocalizedEntry.substring(1);
          String val = (String)localization_entries.get(k);

          if (val == null) {
            localized.put(key, k);
          } else {
            localized.put(key, val);
          }
        }
      }
    }
    return localized;
  }

  /**
   * Reads all localization entries that affects this bundle 
   * (including its host/fragments)
   * @param locale locale == "" the bundle.properties will be read
   *               o/w it will read the files as described in the spec.
   * @param localization_entries will append the new entries to this dictionary
   */
  protected void readLocalization(String locale, 
                                  Hashtable localization_entries,
                                  String baseName) {
    if (baseName == null) {
      baseName = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
    }
    int o = 0;
    String[] parts = Util.splitwords(locale, "_");
    String tmploc;
    if ("".equals(parts[0])) {
      tmploc = baseName;
    } else {
      tmploc = baseName + "_" + parts[0];
    }
    do {
      Hashtable tmp;
      if ((state & RESOLVED_FLAGS) != 0) {
        tmp = ((BundleClassLoader)getClassLoader()).getLocalizationEntries(tmploc +
                                                                           ".properties");
      } else if (archive != null) { // archive == null if this == systemBundle
        tmp = archive.getLocalizationEntries(tmploc  + ".properties");
      } else {
        // No where to search, return.
        return;
      }
      if (tmp != null) {
        localization_entries.putAll(tmp);
      }
      
      if (++o >= parts.length) {
        break;
      }
      tmploc = tmploc + "_" + parts[o];
      
    } while (true);
  }


  /**
   * @see org.osgi.framework.Bundle#getHeaders(String locale)
   */
  public Dictionary getHeaders(String locale) {
    secure.checkMetadataAdminPerm(this);
    return secure.callGetHeaders0(this, locale);
  }

  HeaderDictionary getHeaders0(String locale) {
    if (cachedRawHeaders == null) {
      cachedRawHeaders = archive.getUnlocalizedAttributes();
    }

    if ("".equals(locale)) {
      return (HeaderDictionary)cachedRawHeaders.clone();
    }
    
    if (state == UNINSTALLED) {
      return (HeaderDictionary)cachedHeaders.clone();
    } 

    String base = (String)cachedRawHeaders.get(Constants.BUNDLE_LOCALIZATION);
    Dictionary d;
    if (isFragment() && fragment.host != null) {
      d = fragment.host.getLocaleDictionary(locale, base);
    } else {
      d = getLocaleDictionary(locale, base);
    }
    return localize(d);
  }


  /**
   *
   */
  private void modified(){
    lastModified = System.currentTimeMillis();
    //TODO make sure it is persistent
    if(archive != null){
      try{
        archive.setLastModified(lastModified);
      }
      catch(IOException e){}
    }
  }

  /**
   *
   * @see org.osgi.framework.Bundle#getLastModified()
   */
  public long getLastModified() {
    return lastModified;
  }


  /**
   * @see org.osgi.framework.Bundle#getResources(String name)
   */
  public Enumeration getResources(String name) throws IOException {
    // ResourceAdminPermission checked in the classloader.
    checkUninstalled();
    if (isFragment()) {
      return null;
    }
    if (state == INSTALLED && !secure.okResourceAdminPerm(this)) {
      // We don't want to create a classloader unless we have permission to.
      return null;
    }
    if (getUpdatedState() != INSTALLED) {
      BundleClassLoader cl = (BundleClassLoader)getClassLoader();
      if (cl != null) {
        return cl.getBundleResources(name, false);
      }
    }
    return null;
  }


  /**
   *
   * @see org.osgi.framework.Bundle#loadClass()
   */
  public Class loadClass(final String name) throws ClassNotFoundException {
    if (secure.okClassAdminPerm(this)) {
      checkUninstalled();
      if (isFragment() && !isExtension()) {
        throw new ClassNotFoundException("Can not load classes from fragment bundles");
      }
      if (getUpdatedState() == INSTALLED) {
        framework.listeners.frameworkError(this, new BundleException("Unable to resolve bundle: " + bpkgs.getResolveFailReason()));
        throw new ClassNotFoundException("Unable to resolve bundle");
      }

      ClassLoader cl = getClassLoader();
      if (cl == null) {
        throw new IllegalStateException("state is uninstalled?");
      }
      return cl.loadClass(name);
    } else {
      throw new ClassNotFoundException("No AdminPermission to get class: " + name);
    }
  }


  /**
   * Checks if this bundle is a fragment
   */
  boolean isFragment() {
    return fragment != null;
  }


  /**
   * Checks if this bundle is an extension bundle
   */
  boolean isExtension() {
    return isFragment() &&
      fragment.extension != null;      
  }

  /**
   * Checks if this bundle is an extension bundle that
   * is updated/uninstalled and needs to be restarted.
   */
  boolean extensionNeedsRestart() {
    return isExtension() &&
      (state & (INSTALLED|UNINSTALLED)) != 0;
    // &&
//      framework.systemBundle.fragments != null &&
//      framework.systemBundle.fragments.contains(this);
  }

  /**
   * Checks if this bundle is a boot class path extension bundle
   */
  boolean isBootClassPathExtension() {
    return isExtension() &&
      fragment.extension.equals(Constants.EXTENSION_BOOTCLASSPATH);
  }

  /**
   * Checks if this bundle is a framework extension bundle
   */
  boolean isFrameworkExtension() {
    return isExtension() &&
      fragment.extension.equals(Constants.EXTENSION_FRAMEWORK);
  }


  /**
   * Checks if this bundle is attached to a fragment host.
   */
  boolean isAttached() {
    return isFragment() &&
      fragment.host != null;
  }


  /**
   * Returns the name of the bundle's fragment host.
   * Returns null if this is not a fragment.
   */
  String getFragmentHostName() {
    if (isFragment()) {
      return fragment.name;
    } else {
      return null;
    }
  }


  /**
   * Returns the attached fragment host OR 
   * the most suitable.
   */
  BundleImpl getFragmentHost() {
    return isFragment() ? fragment.targets() : null;
  }


  /**
   * Determines whether this bundle is a fragment host 
   * or not.
   */
  boolean isFragmentHost() {
    return fragments != null && fragments.size() > 0;
  }


  /**
   * Attaches all relevant fragments to this bundle.
   */
  void attachFragments() {
    if (!attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
      List hosting = framework.bundles.getFragmentBundles(this);
      if (hosting.size() > 0 && secure.okHostBundlePerm(this)) {
        // retrieve all fragments this bundle host
        for (Iterator iter = hosting.iterator(); iter.hasNext(); ) {
          BundleImpl fb = (BundleImpl)iter.next();
          if (fb.state == INSTALLED) {
            fb.resolveFragment(this);
          }
        }
      }
    }
  }


  /**
   * Attaches a fragment to this bundle.
   */
  void attachFragment(BundleImpl fragmentBundle) {
    checkUninstalled();
    if (attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
      throw new IllegalStateException("Bundle does not allow fragments to attach");
    }
    if (attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_RESOLVETIME) &&
        (state & RESOLVED_FLAGS) != 0) {
      throw new IllegalStateException("Bundle does not allow fragments to attach dynamicly");
    }

    String failReason = bpkgs.attachFragment(fragmentBundle.bpkgs);
    if (failReason != null) {
      throw new IllegalStateException(failReason);
    }

    if (fragments == null) {
      fragments = new ArrayList();
    }
    int i = 0;
    for (; i < fragments.size(); i++) {
      BundleImpl b = (BundleImpl)fragments.get(i);
      if (b.id > fragmentBundle.id) {
        break;
      }
    }
    fragments.add(i, fragmentBundle);
  }


  /**
   * Returns a iterator over all attached fragments.
   */
  Iterator getFragments() {
    return fragments == null ? 
      new ArrayList(0).iterator() : fragments.iterator();
  }


  /**
   * Detach all fragments from this bundle.
   */
  private void detachFragments(boolean sendEvent) {
    if (fragments != null) {
      while (fragments.size() > 0) {
        detachFragment((BundleImpl)fragments.get(0), sendEvent);
      }
    }
  }


  /**
   * Detach fragment from this bundle.
   */
  private void detachFragment(BundleImpl fb, boolean sendEvent) {
    // NYI! extensions
    if (fragments.remove(fb)) {
      // NYI purge control
      bpkgs.detachFragment(fb);
      if (fb.state != UNINSTALLED) {
        fb.setStateInstalled(sendEvent);
      }
    }
  }

  /**
   * Check if bundle is in state UNINSTALLED. If so, throw exception.
   */
  private void checkUninstalled() {
    if (state == UNINSTALLED) {
      throw new IllegalStateException("Bundle is in UNINSTALLED state");
    }
  }


  /**
   */  
  class Fragment {
    final String name;
    final String extension;
    final VersionRange versionRange;
    BundleImpl host = null;
    
    Fragment(String name, String extension, String range) {
      this.name = name;
      this.extension = extension;
      this.versionRange = range == null ?
        VersionRange.defaultVersionRange :     
        new VersionRange(range);
    }
    
    void setHost(BundleImpl host) {
      this.host = host;
    }

    BundleImpl targets() {
      if (host != null) {
        return host;
      }

      List bundles = framework.bundles.getBundles(name, versionRange);

      if (bundles.isEmpty()) {
        return null;
      }

      BundleImpl best = null;

      for (Iterator iter = bundles.iterator(); iter.hasNext(); ) {
        BundleImpl challenger = (BundleImpl)iter.next();

        if (challenger.state != UNINSTALLED &&
            !challenger.attachPolicy.
            equals(Constants.FRAGMENT_ATTACHMENT_NEVER) &&
            (best == null ||
             challenger.version.compareTo(best.version) > 0)) {

          best = challenger;
        }
      }

      return best;
    }
  }

}//class
