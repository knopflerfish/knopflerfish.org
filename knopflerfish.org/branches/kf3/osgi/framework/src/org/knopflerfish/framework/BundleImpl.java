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
import java.security.cert.Certificate;
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

public class BundleImpl implements Bundle {

  /**
   * Union of flags allowing bundle package access.
   * <p>
   * Value is <tt>Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING</tt>
   * </p>
   */
  final static int RESOLVED_FLAGS = RESOLVED | STARTING | ACTIVE | STOPPING;

  /**
   * Framework for bundle.
   */
  final FrameworkContext fwCtx;

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
  private volatile ClassLoader classLoader = null;

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
   * True when this bundle has its activation policy
   * set to "lazy"
   */
  boolean lazyActivation = false;
  private HashSet lazyIncludes;
  private HashSet lazyExcludes;


  /** True while the activator's start method is running. */
  private boolean activating;
  /** True while the activator's stop method is running. */
  private boolean deactivating;


  /**
   * Construct a new Bundle empty.
   *
   * Only called for system bundle
   *
   * @param fw Framework for this bundle.
   */
  BundleImpl(FrameworkContext fw,
             long id,
             String loc,
             ProtectionDomain pd,
             String sym,
             Version ver)
  {
    this.fwCtx = fw;
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
   * @param fw FrameworkContext for this bundle.
   * @param ba Bundle archive with holding the contents of the bundle.
   * @exception IOException If we fail to read and store our JAR bundle or if
   *            the input data is corrupted.
   * @exception SecurityException If we don't have permission to
   *            import and export bundle packages.
   */
  BundleImpl(FrameworkContext fw, BundleArchive ba) {
    fwCtx = fw;
    secure = fwCtx.perm;
    id = ba.getBundleId();
    location = ba.getBundleLocation();
    archive = ba;
    state = INSTALLED;
    checkManifestHeaders();
    protectionDomain = secure.getProtectionDomain(this);
    doExportImport();
    bundleDir = fwCtx.getDataStorage(id);

    int oldStartLevel = archive.getStartLevel();
    try {
      if (fwCtx.startLevelController == null) {
        archive.setStartLevel(0);
      } else {
        if (oldStartLevel == -1) {
          archive.setStartLevel(fwCtx.startLevelController.getInitialBundleStartLevel());
        }
      }
    } catch (Exception e) {
      fwCtx.props.debug.println("Failed to set start level on #" + id + ": " + e);
    }

    // Activate extension as soon as they are installed so that
    // they get added in bundle id order.
    if (isExtension() && resolveFragment(fwCtx.systemBundle)) {
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

  synchronized public void start() throws BundleException {
    start(0);
  }


  /**
   * Start this bundle.
   *
   * @see org.osgi.framework.Bundle#start
   */
  synchronized public void start(int options) throws BundleException {
    secure.checkExecuteAdminPerm(this);

    if (isFragment()) {
      throw new BundleException("Cannot start a fragment bundle");
    }

    if (state == UNINSTALLED) {
      throw new IllegalStateException("Bundle is uninstalled");
    }

    // The value -1 is used by this implemtation to indicate a bundle
    // that has not been started, thus ensure that options is != -1.
    options &= 0xFF;

    if (fwCtx.startLevelController != null) {
      if (state != UNINSTALLED &&
         getStartLevel() > fwCtx.startLevelController.getStartLevel()) {
        if ((options & START_TRANSIENT) != 0) {
          throw new BundleException
            ("Can not transiently activate bundle with start level "
             +getStartLevel() +" when running on start level "
             +fwCtx.startLevelController.getStartLevel());
        } else {
          setAutostartSetting(options);
          return;
        }
      }
    }

    // Initialize the activation; checks initialization of lazy
    // activation.

    //1: If activating or deactivating, wait a litle
    waitOnActivation("BundleActivator.start");

    //2: start() is idempotent, i.e., nothing to do when already started
    if (state == ACTIVE) {
      return;
    }

    //3: Record non-transient start requests.
    if ((options & START_TRANSIENT) == 0) {
      setAutostartSetting(options);
    }

    //4: Resolve bundle (if needed)
    if (INSTALLED == getUpdatedState()) {
      throw new BundleException("Failed, " + bpkgs.getResolveFailReason());
    }

    //5: Lazy?
    if ((options & START_ACTIVATION_POLICY) != 0 && lazyActivation ) {
      if (STARTING == state) return;
      state = STARTING;
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.LAZY_ACTIVATION,
                                                    this));
      bundleContext = new BundleContextImpl(this);
    } else {
      finalizeActivation();
    }
  }

  // Performs the actual activation.
  synchronized void finalizeActivation()
    throws BundleException
  {
    switch (getUpdatedState()) {
    case INSTALLED:
      throw new BundleException("Failed, " + bpkgs.getResolveFailReason());
    case STARTING:
      if (activating) return; // finalization already in progress.
      // Lazy activation; fall through to RESOLVED.
    case RESOLVED:
      //6:
      state = STARTING;
      //7:
      if (fwCtx.active) {
        fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTING,
                                                      this));
      }

      if (null==bundleContext) {
        bundleContext = new BundleContextImpl(this);
      }

      try {
        secure.callStart0(this);
      } catch (BundleException e) {
        //8:
        state = STOPPING;
        if (fwCtx.active) {
          fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING,
                                                        this));
        }
        removeBundleResources();
        bundleContext.invalidate();
        bundleContext = null;
        state = RESOLVED;
        if (fwCtx.active) {
          fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPED,
                                                        this));
        }
        throw e;
      }
      //10:
      if (fwCtx.active) {
        fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTED,
                                                      this));
      }
      break;
    case ACTIVE:
      break;
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

    if (fwCtx.props.SETCONTEXTCLASSLOADER) {
      oldLoader = Thread.currentThread().getContextClassLoader();
    }

    try {
      // If SETCONTEXTCLASSLOADER, set the thread's context
      // class loader to the bundle class loader. This
      // is useful for debugging external libs using
      // the context class loader.
      if (fwCtx.props.SETCONTEXTCLASSLOADER) {
        Thread.currentThread().setContextClassLoader(getClassLoader());
      }

      if (ba != null) {
        Class c = getClassLoader().loadClass(ba.trim());
        bactivator = (BundleActivator)c.newInstance();

        activating = true;
        bactivator.start(bundleContext);
        activating = false;
        bStarted = true;
      } else {
        // If the Main-Class manifest attribute is set and this
        // bundles location is present in the value (comma separated
        // list) of the (System) property named
        // org.knopflerfish.framework.main.class.activation then setup
        // up a bundle activator that calls the main-method of the
        // Main-Class when the bundle is started, and if the
        // Main-Class contains a method named stop() call that
        // method when the bundle is stopped.
        String locations = fwCtx.props.getProperty
          ("org.knopflerfish.framework.main.class.activation");
        if (locations != null) {
          final String mc = archive.getAttribute("Main-Class");

          if (mc != null) {
            String[] locs = Util.splitwords(locations, ",");
            for (int i = 0; i < locs.length; i++) {
              if (locs[i].equals(location)) {
                if(fwCtx.props.debug.packages) {
                  fwCtx.props.debug.println("starting main class " + mc);
                }
                Class mainClass = getClassLoader().loadClass(mc.trim());
                bactivator = new MainClassBundleActivator(mainClass);
                activating = true;
                bactivator.start(bundleContext);
                activating = false;
                bStarted = true;
                break;
              }
            }
          }
        }
      }

      if (!bStarted) {
        // Even bundles without an activator are marked as
        // ACTIVE.
        // Should we possible log an information message to
        // make sure users are aware of the missing activator?
      }

      if (UNINSTALLED==state) {
        throw new Exception("Bundle uninstalled during start()");
      }
      state = ACTIVE;
    } catch (Throwable t) {
      throw new BundleException("BundleActivator start failed", t);
    } finally {
      activating = false;
      if (fwCtx.props.SETCONTEXTCLASSLOADER) {
        Thread.currentThread().setContextClassLoader(oldLoader);
      }
    }
  }


  synchronized public void stop() throws BundleException {
    stop(0);
  }

  /**
   * Stop this bundle.
   *
   * @see org.osgi.framework.Bundle#stop
   */
  synchronized public void stop(int options) throws BundleException {
    secure.checkExecuteAdminPerm(this);

    if (isFragment()) {
      throw new BundleException("Cannot stop a fragment bundle");
    }

    //1:
    if (state == UNINSTALLED) {
      throw new IllegalStateException("Bundle is uninstalled");
    }


    //2: If activating or deactivating, wait a litle
    waitOnActivation("BundleActivator.stop");

    //3:
    if ((options & STOP_TRANSIENT) == 0) {
      setAutostartSetting(-1);
    }

    switch (state) {
    case INSTALLED:
    case RESOLVED:
    case STOPPING:
    case UNINSTALLED:
      //4:
      return;

    case ACTIVE:
    case STARTING: // Lazy start...
      //5-13:
      final BundleException savedException = secure.callStop0(this);
      if (savedException != null) {
        throw savedException;
      }
      break;
    }
  }

  synchronized BundleException stop0() {
    final boolean wasStarted = ACTIVE == state;
    BundleException res = null;

    //5:
    state = STOPPING;
    //6:
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));

    //7:
    if (wasStarted && bactivator != null) {
      deactivating = true;
      try {
        bactivator.stop(bundleContext);
      } catch (Throwable e) {
        res = new BundleException("Bundle.stop: BundleActivator stop failed",
                                  e);
      } finally {
        deactivating = false;
        bactivator = null;
      }
    }
    if (null!=bundleContext) bundleContext.invalidate();
    bundleContext = null;
    //8-10:
    removeBundleResources();
    if (UNINSTALLED==state) {
      //11:
      res = new BundleException("Bundle uninstalled during stop()");
    } else {
      //12:
      state = RESOLVED;
      //13:
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPED, this));
    }
    return res;
  }


  /**
   * Release monitors and wait a litle for an ongoing activation /
   * de-activation to finish.
   *
   * @param src Caller to inlcude in exception message.
   * @throws BundleException if the ongoing (de-)activation does not
   * finish within reasonable time.
   */
  private synchronized void waitOnActivation(final String src)
    throws BundleException
  {
    int k = 0;
    while ((activating || deactivating) && k<10) {
      try {
        this.wait(10L);
      } catch (InterruptedException _ie) {
      }
      k++;
    }
    if (activating) {
      throw new BundleException(src +" called from BundleActivator.start");
    }

    if (deactivating) {
      throw new BundleException(src +" called from BundleActivator.stop");
    }
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
        stop(STOP_TRANSIENT);
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
        String update = archive!=null
          ? archive.getAttribute(Constants.BUNDLE_UPDATELOCATION)
          : null;
        if (update == null) {
          // Take original location
          update = location;
        }
        URL url = new URL(update);

        // Handle case where bundle location is a reference and/or a directory
        // URL. In these cases, send a NULL input stream to the archive
        // indicating that is should re-use the old bundle location
        String fname = url.getFile(); // if reference URL, the getFile() result may be a file: string
        if(fname.startsWith("file:")) {
          fname = fname.substring(5);
        }
        File file = new File(fname);
        if(file.isDirectory()) {
          bin = null;
        } else {
          bin = url.openStream();
        }
      } else {
        bin = in;
      }

      newArchive = fwCtx.storage.updateBundleArchive(archive, bin);
      checkEE(newArchive);
      checkManifestHeaders();
      newArchive.setStartLevel(oldStartLevel);
      fwCtx.storage.replaceBundleArchive(archive, newArchive);
    } catch (Exception e) {
      if (newArchive != null) {
        newArchive.purge();
      }

      if (wasActive) {
        try {
          start();
        } catch (BundleException be) {
          fwCtx.listeners.frameworkError(this, be);
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
        BundleImpl host = getFragmentHost();
        host.bpkgs.fragmentIsZombie(this);
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
          if(classLoader instanceof BundleClassLoader) {
            ((BundleClassLoader)classLoader).close();
          }
          classLoader = null;
        }
        purgeOld = true;
      } else {
        saveZombiePackages();
        purgeOld = false;
      }

      if (isFragmentHost()) {
        detachFragments(true);
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
      if (null!=oldArchive) oldArchive.purge();
    }

    // Broadcast updated event
    if (wasResolved) {
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED,
                                                        this));
    }
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.UPDATED,
                                                      this));

    // Restart bundles previously stopped in the operation
    if (wasActive) {
      try {
        start();
      } catch (BundleException be) {
        fwCtx.listeners.frameworkError(this, be);
      }
    }
    //only when complete success
    modified();
  }


  void checkEE(BundleArchive ba) throws BundleException {
    String ee = ba.getAttribute(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
    if (ee != null) {
      if (fwCtx.props.debug.packages) {
        fwCtx.props.debug.println("bundle #" + ba.getBundleId() + " has EE=" + ee);
      }
      if (!fwCtx.isValidEE(ee)) {
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

    if (null!=archive) {
      try {
        archive.setStartLevel(-2); // Mark as uninstalled
      } catch (Exception ignored) {
      }
    }

    cachedHeaders = getHeaders0(null);

    switch (state) {
    case UNINSTALLED:
      throw new IllegalStateException("Bundle is in UNINSTALLED state");

    case STOPPING:
      // Wait for RESOLVED state, this doesn't happen now
      // since we are synchronized.
      throw new IllegalStateException("Bundle is in STOPPING state");

    case STARTING: // Lazy start
    case ACTIVE:
      try {
        stop();
      } catch (Throwable t) {
        fwCtx.listeners.frameworkError(this, t);
      }
      // Fall through
    case RESOLVED:
      wasResolved = true;
      // Fall through
    case INSTALLED:

      fwCtx.bundles.remove(location);

      if (isFragment()) {
        if (isAttached()) {
          BundleImpl host = getFragmentHost();
          host.bpkgs.fragmentIsZombie(this);
          fragment.setHost(null);
          classLoader = null;
        } else {
          secure.purge(this, protectionDomain);
          if (null!=archive) archive.purge();
        }
      } else { // Non-fragment bundle
        // Try to unregister this bundle's packages
        boolean pkgsUnregistered = bpkgs.unregisterPackages(false);

        if (pkgsUnregistered) {
          // No exports in use, clean up.
          if (classLoader != null) {
            if(classLoader instanceof BundleClassLoader) {
              ((BundleClassLoader)classLoader).purge();
            }
            classLoader = null;
          } else {
            secure.purge(this, protectionDomain);
            if (null!=archive) archive.purge();
          }
        } else {
          // Exports are in use, save as zombie packages
          saveZombiePackages();
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
          if (null!=archive) {
            try {
              archive.setStartLevel(-2); // Mark as uninstalled
            } catch (Exception e) {
              fwCtx.props.debug.println("Failed to mark bundle " + id +
                                        " as uninstalled, " + bundleDir +
                                        " must be deleted manually: " + e);
            }
          }
        }
        bundleDir = null;
      }

      // id, location and headers survives after uninstall.
      state = UNINSTALLED;
      modified();

      if (wasResolved) {
        fwCtx.listeners
          .bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
      }
      fwCtx.listeners
        .bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, this));
      break;
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
   * @see org.osgi.fwCtx.Bundle#getBundleId
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
    Set sr = fwCtx.services.getRegisteredByBundle(this);
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
    Set sr = fwCtx.services.getUsedByBundle(this);
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
   * Returns this bundle's BundleContext. This method will be
   * introduced in OSGi R4.1 but included here as a migration step.
   *
   * @see org.osgi.framework.Bundle#getBundleContext
   * @since 1.4
   */
  public BundleContext getBundleContext() {
    secure.checkContextAdminPerm(this);
    return secure.callGetBundleContext0(this);
  }

  BundleContext getBundleContext0() {
    return bundleContext;
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
      ClassLoader cl0 = getClassLoader();
      if (cl0 != null) {
        return cl0.getResource(name);
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


  /**
   * Get protection domain for bundle. Used by BundleSignerCondition.
   */
  public Certificate [] getCertificates() {
    // If we have protection domain priviledges then we can get certs.
    secure.checkGetProtectionDomain();
    return archive!=null ? archive.getCertificates() : null;
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
                  fwCtx.listeners
                    .bundleChanged(new BundleEvent(BundleEvent.RESOLVED, b));
                }
              }
              fwCtx.listeners
                .bundleChanged(new BundleEvent(BundleEvent.RESOLVED, this));

              // This is not applicable to system bundle.
              if (id!=0 && null!=archive) {
                List fe = archive.getFailedClassPathEntries();
                if (fe != null) {
                  for (Iterator i = fe.iterator(); i.hasNext(); ) {
                    Exception e = new IOException
                      ("Failed to classpath entry: " + i.next());
                    fwCtx.listeners.frameworkInfo(this, e);
                  }
                }
              }
            } else {
              fwCtx.listeners.frameworkError
                (this,
                 new BundleException("Unable to resolve bundle: "
                                     + bpkgs.getResolveFailReason()));
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
    ClassLoader loader = classLoader;
    if (loader == null) {
      synchronized(this) {
        if (classLoader == null && (state & RESOLVED_FLAGS) != 0) {
          classLoader = secure.callGetClassLoader0(this);
        }
        loader = classLoader;
      }
    }
    return loader;
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
        if(classLoader instanceof BundleClassLoader) {
          ((BundleClassLoader)classLoader).close();
        }
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
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
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
      fwCtx.bundles.remove(location);
    }
    if (oldClassLoaders != null) {
      for (Iterator i = oldClassLoaders.values().iterator(); i.hasNext();) {
        Object obj = i.next();
        if(obj instanceof BundleClassLoader) {
          ((BundleClassLoader)obj).purge();
        }
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
        Object obj = i.next();
        if(obj instanceof BundleClassLoader) {
          for (Iterator j = ((BundleClassLoader)obj).getBpkgs().getExports(); j.hasNext();) {
            res.add(j.next());
          }
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
        Object obj = i.next();
        if(obj instanceof BundleClassLoader) {
          for (Iterator j = ((BundleClassLoader)obj).getBpkgs().getImports(); j.hasNext();) {
            res.add(j.next());
          }
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
   * Get a list of all BundlePackages that require the exported
   * packages that comes from this bundle.
   *
   * @return List of all requiring bundles as BundlePackages.
   */
  List getRequiredBy() {
    if (oldClassLoaders != null) {
      ArrayList res = new ArrayList();
      for (Iterator i = oldClassLoaders.values().iterator(); i.hasNext();) {
        Object obj = i.next();
        if(obj instanceof BundleClassLoader) {
          res.addAll(((BundleClassLoader) obj).getBpkgs().getRequiredBy());
        }
      }
      if (bpkgs != null) {
        res.addAll(bpkgs.getRequiredBy());
      }
      return res;
    } else if (bpkgs != null) {
      return bpkgs.getRequiredBy();
    } else {
      return new ArrayList(0);
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
      return secure.getBundleURL(this, u.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
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
    if (null==archive) return; // System bundle; nothing to check.

    // TBD, v2Manifest unnecessary to cache?
    v2Manifest = "2".equals(archive.getAttribute(Constants.BUNDLE_MANIFESTVERSION));
    Iterator i = Util.parseEntries(Constants.BUNDLE_SYMBOLICNAME,
                                   archive.getAttribute(Constants.BUNDLE_SYMBOLICNAME),
                                   true, true, true);
    Map e = null;
    if (i.hasNext()) {
      e = (Map)i.next();
      symbolicName = (String)e.get("$key");
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
      try {
        version = new Version(mbv);
      } catch (Throwable ee) {
        if (v2Manifest) {
          throw new IllegalArgumentException("Bundle does not specify a valid " +
              Constants.BUNDLE_VERSION + " header. Got exception: " + ee.getMessage());
        } else {
          version = Version.emptyVersion;
        }
      }

    } else {
      version = Version.emptyVersion;
    }

    attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
    if (e != null) {
      singleton = "true".equals((String)e.get(Constants.SINGLETON_DIRECTIVE));
      BundleImpl snb = fwCtx.bundles.getBundle(symbolicName, version);
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
      String key = (String)e.get("$key");

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
        if (!fwCtx.props.SUPPORTS_EXTENSION_BUNDLES) {
          if (fwCtx.props.bIsMemoryStorage) {
            throw new UnsupportedOperationException("Extension bundles are not supported in memory storage mode.");
          } else if (!fwCtx.props.EXIT_ON_SHUTDOWN) {
            throw new UnsupportedOperationException("Extension bundles require that the property " +
                                                    Main.EXITONSHUTDOWN_PROP + " is set to \"true\"");
          } else if (!fwCtx.props.USING_WRAPPER_SCRIPT) {
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

    i = Util.parseEntries(Constants.BUNDLE_ACTIVATIONPOLICY,
                          archive.getAttribute(Constants.BUNDLE_ACTIVATIONPOLICY),
                          true, true, true);
    if (i.hasNext()) {
      e = (Map)i.next();
      lazyActivation = Constants.ACTIVATION_LAZY.equals(e.get("$key"));

      if (lazyActivation) {
        if (e.containsKey(Constants.INCLUDE_DIRECTIVE)) {
          final ArrayList incs =
            Util.parseEnumeration(Constants.INCLUDE_DIRECTIVE,
                                  (String) e.get(Constants.INCLUDE_DIRECTIVE));
          lazyIncludes = new HashSet();
          lazyIncludes.addAll(incs);
        }

        if (e.containsKey(Constants.EXCLUDE_DIRECTIVE)) {
          final ArrayList excs =
            Util.parseEnumeration(Constants.EXCLUDE_DIRECTIVE,
                                  (String) e.get(Constants.EXCLUDE_DIRECTIVE));
          lazyExcludes = new HashSet();

          for (Iterator excsIter = excs.iterator(); excsIter.hasNext();) {
            String entry = (String)excsIter.next();
            if (lazyIncludes != null && lazyIncludes.contains(entry)) {
              throw new IllegalArgumentException
                ("Conflicting " +Constants.INCLUDE_DIRECTIVE
                 +"/" +Constants.EXCLUDE_DIRECTIVE
                 +" entries in " +Constants.BUNDLE_ACTIVATIONPOLICY+": '"
                 +entry +"' both included and excluded");
            }
            lazyExcludes.add(entry);
          }
        }
      }
    }

  }



  /**
   * Save the autostart setting to the persistent bundle storage.
   *
   * @param setting The autostart options to save.
   */
  void setAutostartSetting(int setting) {
    secure.callSetAutostartSetting(this, setting);
  }

  /**
   * Internal version; only to be used from inside PriviledgedActions
   * running on the framework's security context.
   *
   * @param setting The autostart setting to store.
   */
  void setAutostartSetting0(int setting) {
    try {
      if (null!=archive) {
        archive.setAutostartSetting(setting);
      }
    } catch (IOException e) {
      fwCtx.listeners.frameworkError(this, e);
    }
  }


  /**
   * Get the autostart setting from the bundle storage.
   *
   * @return the current autostart setting, "-1" if bundle not
   * started.
   */
  int getAutostartSetting() {
    return archive!=null ? archive.getAutostartSetting() : -1;
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
    fwCtx.listeners.removeAllListeners(this);
    Set srs = fwCtx.services.getRegisteredByBundle(this);
    for (Iterator i = srs.iterator(); i.hasNext();) {
      try {
        ((ServiceRegistration)i.next()).unregister();
      } catch (IllegalStateException ignore) {
        // Someone has unregistered the service after stop completed.
        // This should not occur, but we don't want get stuck in
        // an illegal state so we catch it.
      }
    }
    Set s = fwCtx.services.getUsedByBundle(this);
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
        fwCtx.props.debug.println("Failed to set start level on #"
                                  + getBundleId());
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
      try {
        sb.append(", autostart setting=");
        sb.append(getAutostartSetting());
      }  catch (Exception e) {
        sb.append(e.toString());
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
  public Enumeration findEntries(String path,
                                 String filePattern,
                                 boolean recurse)
  {
    if (secure.okResourceAdminPerm(this)) {
      if (state == INSTALLED) {
        // We need to resolve if there are fragments involved
        if (!fwCtx.bundles.getFragmentBundles(this).isEmpty()) {
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
  Enumeration findEntries0(String path,
                           String filePattern,
                           boolean recurse)
  {
        Vector res = new Vector();
        addResourceEntries(res, path, filePattern, recurse);
        if (isFragmentHost()) {
          for (Iterator i = fragments.iterator(); i.hasNext(); ) {
            BundleImpl fb = (BundleImpl)i.next();
            fb.addResourceEntries(res, path, filePattern, recurse);
          }
        }
        return res.size() != 0 ? res.elements() : null;
  }


  /**
   *
   */
  void addResourceEntries(Vector res,
                          String path,
                          String pattern,
                          boolean recurse)
  {
    Enumeration e = archive.findResourcesPath(path);
    if (e != null) {
      while (e.hasMoreElements()) {
        String fp = (String)e.nextElement();
        boolean isDirectory = fp.endsWith("/");
        int searchBackwardFrom = fp.length() - 1;
        if(isDirectory) {
                // Skip last / in case of directories
                searchBackwardFrom = searchBackwardFrom - 1;
        }
        int l = fp.lastIndexOf('/', searchBackwardFrom);
        String lastComponentOfPath = fp.substring(l + 1, searchBackwardFrom + 1);
        if (pattern == null || Util.filterMatch(pattern, lastComponentOfPath)) {
                URL url = getURL(-1, -1, -1, fp);
                if (url != null) {
                  res.add(url);
                }
        }
        if (isDirectory && recurse) {
            addResourceEntries(res, fp, pattern, recurse);
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
        if ("/".equals(name)) {
          return getURL(-1, -1, -1, "/");
        }
        InputStream is = secure.callGetInputStream(archive, name, -1);
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
   * @param baseName the basename for localization properties,
   *        <code>null</code> will choose OSGi default
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
      } else {
        tmp = archive.getLocalizationEntries(tmploc  + ".properties");
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


  public Map/* <X509Certificate, List<X509Certificate>> */getSignerCertificates(int signersType) {
    throw new RuntimeException("NYI");
  }

  public Version getVersion() {
    return version;
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
      ClassLoader cl0 = getClassLoader();
      if (cl0 != null) {
        Enumeration e = cl0 instanceof BundleClassLoader
          ? ((BundleClassLoader) cl0).getResourcesOSGi(name)
          : cl0.getResources(name);;
        return e != null && e.hasMoreElements() ? e : null;
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
        fwCtx.listeners.frameworkError(this, new BundleException("Unable to resolve bundle: " + bpkgs.getResolveFailReason()));
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
//      fwCtx.systemBundle.fragments != null &&
//      fwCtx.systemBundle.fragments.contains(this);
  }

  /**
   * Checks if this bundle is a boot class path extension bundle
   */
  boolean isBootClassPathExtension() {
    return isExtension() &&
      fragment.extension.equals(Constants.EXTENSION_BOOTCLASSPATH);
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
      List hosting = fwCtx.bundles.getFragmentBundles(this);
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

    if(fwCtx.props.debug.packages) {
      fwCtx.props.debug.println("Fragment(id=" +fragmentBundle.getBundleId()
                    +") attached to host(id=" +bpkgs.bundle.id
                    +",gen=" +bpkgs.generation +")");


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
   * Detach all fragments from this bundle and its bundle packages.
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
      bpkgs.detachFragment(fb);
      if(fwCtx.props.debug.packages) {
        fwCtx.props.debug.println("Fragment(id=" +fb.getBundleId()
                      +") detached from host(id=" +bpkgs.bundle.id
                      +",gen=" +bpkgs.generation +")");
      }
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
   * @param packageName
   * @return true if this package name should trigger
   * activation of a lazyBundle
   */
  private boolean isPkgActivationTrigger(String packageName) {
    return (lazyIncludes == null && lazyExcludes == null) ||
      (lazyIncludes != null && lazyIncludes.contains(packageName)) ||
      (lazyExcludes != null && !lazyExcludes.contains(packageName));
  }

  boolean triggersActivationPkg(String pkg) {
    return state == Bundle.STARTING && lazyActivation
      && isPkgActivationTrigger(pkg);
  }

  boolean triggersActivationCls(String name) {
    if (state == Bundle.STARTING && lazyActivation) {
      String pkg = "";
      int pos = name.lastIndexOf('.');
      if (pos != -1) {
        pkg = name.substring(0, pos);
      }
      return isPkgActivationTrigger(pkg);
    }
    return false;
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

      List bundles = fwCtx.bundles.getBundles(name, versionRange);

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
  }//class Fragment

}//class BundleImpl
