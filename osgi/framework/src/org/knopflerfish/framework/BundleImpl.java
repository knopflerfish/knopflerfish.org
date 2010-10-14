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

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

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
   * Bundle identifier.
   */
  final long id;

  /**
   * Bundle location identifier.
   */
  final String location;

  /**
   * Handle to secure operations.
   */
  PermissionOps secure;

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
  volatile int state;

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
   * Bundle protect domain. Will allways be <tt>null</tt> for the
   * system bundle, methods requireing access to it must be overridden
   * in the SystemBundle class.
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
  ArrayList /* BundleImpl */ fragments = null;

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


  /**
   * Type of operation in progress. Blocks bundle calls
   * during activator and listener calls
   */
  volatile protected int operation;
  final static int IDLE = 0;
  final static int ACTIVATING = 1;
  final static int DEACTIVATING = 2;
  final static int RESOLVING = 3;
  final static int UNINSTALLING = 4;
  final static int UNRESOLVING = 5;
  final static int UPDATING = 6;

  /** Saved exception of resolve failure. */
  private BundleException resolveFailException;

  /** Rember if bundle was started */
  private boolean wasStarted;
  

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
             String sym,
             Version ver)
  {
    this.fwCtx = fw;
    this.secure = fwCtx.perm;
    this.id = id;
    this.location = loc;
    this.protectionDomain = null;
    this.symbolicName = sym;
    this.singleton = false;
    this.version = ver;
    this.v2Manifest = true;
    this.state = INSTALLED;
    this.attachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
    modified();
  }


  /**
   * Construct a new Bundle based on a BundleArchive.
   *
   * @param fw FrameworkContext for this bundle.
   * @param ba Bundle archive with holding the contents of the bundle.
   * @param checkContext AccessConrolContext to do permission checks against.
   * @exception IOException If we fail to read and store our JAR bundle or if
   *            the input data is corrupted.
   * @exception SecurityException If we don't have permission to
   *            install extension.
   * @exception IllegalArgumentException Faulty manifest for bundle
   */
  BundleImpl(FrameworkContext fw, BundleArchive ba, Object checkContext) {
    fwCtx = fw;
    secure = fwCtx.perm;
    id = ba.getBundleId();
    location = ba.getBundleLocation();
    archive = ba;
    state = INSTALLED;
    checkCertificates(ba);
    protectionDomain = secure.getProtectionDomain(this);
    try {
      secure.checkLifecycleAdminPerm(this, checkContext);
      checkManifestHeaders(checkContext);
      bpkgs = new BundlePackages(this,
                                 generation++,
                                 archive.getAttribute(Constants.EXPORT_PACKAGE),
                                 archive.getAttribute(Constants.IMPORT_PACKAGE),
                                 archive.getAttribute(Constants.DYNAMICIMPORT_PACKAGE),
                                 archive.getAttribute(Constants.REQUIRE_BUNDLE));
    } catch (RuntimeException re) {
      secure.purge(this, protectionDomain);
      throw re;
    }

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
      fwCtx.debug.println("Failed to set start level on #" + id + ": " + e);
    }

    lastModified = archive.getLastModified();
    if (lastModified == 0) {
      modified();
    }
    
    // Activate extension as soon as they are installed so that
    // they get added in bundle id order.
    if (isExtension() && resolveFragment(fwCtx.systemBundle)) {
      state = RESOLVED;
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


  public void start() throws BundleException {
    start(0);
  }


  /**
   * Start this bundle.
   *
   * @see org.osgi.framework.Bundle#start
   */
  public void start(int options) throws BundleException {
    secure.checkExecuteAdminPerm(this);

    synchronized (fwCtx.packages) {
      if (isFragment()) {
        throw new BundleException("Cannot start a fragment bundle",
                                  BundleException.INVALID_OPERATION);
      }

      if (state == UNINSTALLED) {
        throw new IllegalStateException("Bundle is uninstalled");
      }

      // The value -1 is used by this implemtation to indicate a bundle
      // that has not been started, thus ensure that options is != -1.
      options &= 0xFF;

      if (fwCtx.startLevelController != null) {
        if (getStartLevel() > fwCtx.startLevelController.getStartLevel()) {
          if ((options & START_TRANSIENT) != 0) {
            throw new BundleException
              ("Can not transiently activate bundle with start level "
               +getStartLevel() +" when running on start level "
               +fwCtx.startLevelController.getStartLevel(),
               BundleException.START_TRANSIENT_ERROR);
          } else {
            setAutostartSetting(options);
            return;
          }
        }
      }

      // Initialize the activation; checks initialization of lazy
      // activation.

      //1: If an operation is in progress, wait a little
      waitOnOperation(fwCtx.packages, "Bundle.start", false);

      //2: start() is idempotent, i.e., nothing to do when already started
      if (state == ACTIVE) {
        return;
      }

      //3: Record non-transient start requests.
      if ((options & START_TRANSIENT) == 0) {
        setAutostartSetting(options);
      }

      //5: Lazy?
      if ((options & START_ACTIVATION_POLICY) != 0 && lazyActivation ) {
        //4: Resolve bundle (if needed)
        if (INSTALLED == getUpdatedState()) {
          throw resolveFailException;
        }
        if (STARTING == state) return;
        state = STARTING;
        bundleContext = new BundleContextImpl(this);
        operation = ACTIVATING;
      } else {
        secure.callFinalizeActivation(this);
        return;
      }
    }
    // Last step of lazy activation
    secure.callBundleChanged(fwCtx, new BundleEvent(BundleEvent.LAZY_ACTIVATION, this));
    synchronized (fwCtx.packages) {
      operation = IDLE;
      fwCtx.packages.notifyAll();
    }
  }

  // Performs the actual activation.
  void finalizeActivation()
    throws BundleException
  {
    synchronized (fwCtx.packages) {
      //4: Resolve bundle (if needed)
      switch (getUpdatedState()) {
      case INSTALLED:
        throw resolveFailException;
      case STARTING:
        if (operation == ACTIVATING) {
          // finalization already in progress.
          return;
        }
        // Lazy activation; fall through to RESOLVED.
      case RESOLVED:
        //6:
        state = STARTING;
        operation = ACTIVATING;
        if (fwCtx.debug.lazy_activation) {
          fwCtx.debug.println("activating #" +getBundleId());
        }
        //7:
        if (null==bundleContext) {
          bundleContext = new BundleContextImpl(this);
        }
        BundleException e = bundleThread().callStart0(this);
        operation = IDLE;
        fwCtx.packages.notifyAll();
        if (e != null) {
          throw e;
        }
        break;
      case ACTIVE:
        break;
      case STOPPING:
        // This happens if call start from inside the BundleActivator.stop method.
        // Don't allow it.
        throw new BundleException("start called from BundleActivator.stop",
                                  BundleException.ACTIVATOR_ERROR);
      case UNINSTALLED:
        throw new IllegalStateException("Bundle is in UNINSTALLED state");
      }
    }
  }


  /**
   * Start code that is executed in the bundleThread without holding
   * the packages lock.
   */
  BundleException start0() {
    final String ba = archive.getAttribute(Constants.BUNDLE_ACTIVATOR);
    boolean bStarted = false;
    BundleException res = null;

    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTING, this));

    // If SETCONTEXTCLASSLOADER, set the thread's context
    // class loader to the bundle class loader. This
    // is useful for debugging external libs using
    // the context class loader.
    ClassLoader oldLoader = null;
    if (fwCtx.props.SETCONTEXTCLASSLOADER) {
      oldLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(classLoader);
    }

    int error_type = BundleException.MANIFEST_ERROR;
    try {
      if (ba != null) {
        Class c = classLoader.loadClass(ba.trim());
        error_type = BundleException.ACTIVATOR_ERROR;
        bactivator = (BundleActivator)c.newInstance();

        bactivator.start(bundleContext);
        bStarted = true;
      } else {
        String locations = fwCtx.props.getProperty(FWProps.MAIN_CLASS_ACTIVATION_PROP);
        if (locations.length() > 0) {
          final String mc = archive.getAttribute("Main-Class");

          if (mc != null) {
            String[] locs = Util.splitwords(locations, ",");
            for (int i = 0; i < locs.length; i++) {
              if (locs[i].equals(location)) {
                if(fwCtx.debug.packages) {
                  fwCtx.debug.println("starting main class " + mc);
                }
                error_type = BundleException.ACTIVATOR_ERROR;
                Class mainClass = classLoader.loadClass(mc.trim());
                bactivator = new MainClassBundleActivator(mainClass);
                bactivator.start(bundleContext);
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

      if (STARTING != state) {
        error_type = BundleException.STATECHANGE_ERROR;
        if (UNINSTALLED == state) {
          throw new Exception("Bundle uninstalled during start()");
        } else {
          throw new Exception("Bundle changed state because of refresh during start()");
        }
      }
      state = ACTIVE;
    } catch (Throwable t) {
      res = new BundleException("Bundle start failed", error_type, t);
    }
    if (fwCtx.debug.lazy_activation) {
      fwCtx.debug.println("activating #" + getBundleId() + " completed.");
    }
    if (fwCtx.props.SETCONTEXTCLASSLOADER) {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }
    if (res == null) {
      //10:
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTED, this));
    } else if (operation == ACTIVATING) {
      //8:
      state = STOPPING;
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));
      removeBundleResources();
      bundleContext.invalidate();
      bundleContext = null;
      state = RESOLVED;
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPED, this));
    }
    return res;
  }


  public void stop() throws BundleException {
    stop(0);
  }

  /**
   * Stop this bundle.
   *
   * @see org.osgi.framework.Bundle#stop
   */
  public void stop(int options) throws BundleException {
    Exception savedException = null;

    secure.checkExecuteAdminPerm(this);

    synchronized (fwCtx.packages) {
      if (isFragment()) {
        throw new BundleException("Cannot stop a fragment bundle",
                                  BundleException.INVALID_OPERATION);
      }

      //1:
      if (state == UNINSTALLED) {
        throw new IllegalStateException("Bundle is uninstalled");
      }


      //2: If an operation is in progress, wait a little
      waitOnOperation(fwCtx.packages, "Bundle.stop", false);

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
        savedException = stop0();
        break;
      }
    }
    if (savedException != null) {
      if (savedException instanceof BundleException) {
        throw (BundleException)savedException;
      } else {
        throw (RuntimeException)savedException;
      }
    }
  }


  Exception stop0() {
    wasStarted = state == ACTIVE;
    //5:
    state = STOPPING;
    operation = DEACTIVATING;
    //6-13:
    final Exception savedException = 
      bundleThread().callStop1(this);
    if (state != UNINSTALLED) {
      state = RESOLVED;
      bundleThread().bundleChanged(new BundleEvent(BundleEvent.STOPPED, this));
      fwCtx.packages.notifyAll();
      operation = IDLE;
    }
    return savedException;
  }


  /**
   * Stop code that is executed in the bundleThread without holding
   * the packages lock.
   */
  Exception stop1() {
    BundleException res = null;

    //6:
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));

    //7:
    if (wasStarted && bactivator != null) {
      try {
        bactivator.stop(bundleContext);
        if (state != STOPPING) {
          if (state == UNINSTALLED) {
            return new IllegalStateException("Bundle is uninstalled");
          } else {
            return new IllegalStateException("Bundle changed state because of refresh during stop");
          }
        }
      } catch (Throwable e) {
        res = new BundleException("Bundle.stop: BundleActivator stop failed",
                                  BundleException.ACTIVATOR_ERROR, e);
      }
      bactivator = null;
    }

    if (operation == DEACTIVATING) {
      // Call hooks after we've called Activator.stop(), but before we've cleared all resources
      if (null != bundleContext) {
        fwCtx.listeners.serviceListeners.hooksBundleStopped(bundleContext);
        //8-10:
        removeBundleResources();
        bundleContext.invalidate();
        bundleContext = null;
      }
    }

    return res;
  }


  /**
   * Wait for an ongoing operation to finish.
   *
   * @param lock Object used for locking.
   * @param src Caller to include in exception message.
   * @param longWait True, if we should wait extra long before aborting.
   * @throws BundleException if the ongoing (de-)activation does not
   * finish within reasonable time.
   */
  void waitOnOperation(Object lock, final String src, boolean longWait)
    throws BundleException
  {
    if (operation != IDLE) {
      long left = longWait ? 2000 : 200;
      long waitUntil = System.currentTimeMillis() + left;
      do {
        try {
          lock.wait(left);
          if (operation == IDLE) {
            return;
          }
        } catch (InterruptedException _ie) { }
        left = waitUntil - System.currentTimeMillis();
      } while (left > 0);
      String op;
      switch (operation) {
      case IDLE:
        // Should not happen!
        return;
      case ACTIVATING:
        op = "start";
        break;
      case DEACTIVATING:
        op = "stop";
        break;
      case RESOLVING:
        op = "resolve";
        break;
      case UNINSTALLING:
        op = "uninstall";
        break;
      case UNRESOLVING:
        op = "unresolve";
        break;
      case UPDATING:
        op = "update";
        break;
      default:
        op = "unknown operation";
        break;
      }
      throw new BundleException(src + " called during " + op + " of Bundle",
                                BundleException.STATECHANGE_ERROR);
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
  public void update(final InputStream in) throws BundleException {
    try {
      secure.checkLifecycleAdminPerm(this);
      if (isExtension()) {
        secure.checkExtensionLifecycleAdminPerm(this);
      }
      synchronized (fwCtx.packages) {
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
      }
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) {}
      }

    }
  }


  void update0(InputStream in, boolean wasActive, Object checkContext) throws BundleException {
    final boolean wasResolved = state == RESOLVED;
    final Fragment oldFragment = fragment;
    final int oldStartLevel = getStartLevel();
    BundleArchive newArchive = null;
    BundlePackages newBpkgs = null;

    operation = UPDATING;
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
      checkCertificates(newArchive);
      checkManifestHeaders(checkContext);
      // Increment generation when we have decide to commit this version
      newBpkgs = new BundlePackages(this,
                                    generation,
                                    newArchive.getAttribute(Constants.EXPORT_PACKAGE),
                                    newArchive.getAttribute(Constants.IMPORT_PACKAGE),
                                    newArchive.getAttribute(Constants.DYNAMICIMPORT_PACKAGE),
                                    newArchive.getAttribute(Constants.REQUIRE_BUNDLE));
      newArchive.setStartLevel(oldStartLevel);
      fwCtx.storage.replaceBundleArchive(archive, newArchive);
    } catch (Exception e) {
      if (newArchive != null) {
        newArchive.purge();
      }
      operation = IDLE;
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
        throw new BundleException("Failed to get update bundle",
                                  BundleException.UNSPECIFIED, e);
      }
    }

    boolean purgeOld;

    if (oldFragment != null) {
      if (oldFragment.hasHosts()) {
        if (oldFragment.extension != null) {
          if (oldFragment.extension.equals(Constants.EXTENSION_BOOTCLASSPATH)) {
            fwCtx.systemBundle.bootClassPathHasChanged = true;
          }
        } else {
          for (Iterator i = oldFragment.getHosts(); i.hasNext(); ) {
            ((BundleImpl)i.next()).bpkgs.fragmentIsZombie(this);
          }
        }
        oldFragment.removeHost(null);
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
        detachFragments();
      }
    }

    // Activate new bundle
    BundleArchive oldArchive = archive;
    archive = newArchive;
    cachedRawHeaders = null;
    state = INSTALLED;
    ProtectionDomain oldProtectionDomain = protectionDomain;
    protectionDomain = secure.getProtectionDomain(this);
    bpkgs = newBpkgs;
    doExportImport();

    // Purge old archive
    if (purgeOld) {
      secure.purge(this, oldProtectionDomain);
      if (null != oldArchive) {
        oldArchive.purge();
      }
    }

    // Broadcast events
    if (wasResolved) {
      bundleThread().bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
                                                       
    }
    bundleThread().bundleChanged(new BundleEvent(BundleEvent.UPDATED, this));
    operation = IDLE;
    //only when complete success
    modified();

    // Restart bundles previously stopped in the operation
    if (wasActive) {
      try {
        start();
      } catch (BundleException be) {
        fwCtx.listeners.frameworkError(this, be);
      }
    }
  }


  /**
   * Uninstall this bundle.
   *
   * @see org.osgi.framework.Bundle#uninstall
   */
  public void uninstall() throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    if (isExtension()) {
      secure.checkExtensionLifecycleAdminPerm(this);
    }
    secure.callUninstall0(this);
  }

  void uninstall0() {
    synchronized (fwCtx.packages) {
      if (null!=archive) {
        try {
          archive.setStartLevel(-2); // Mark as uninstalled
        } catch (Exception ignored) {
        }
      }

      boolean doPurge = false;
      switch (state) {
      case UNINSTALLED:
        throw new IllegalStateException("Bundle is in UNINSTALLED state");

      case STARTING: // Lazy start
      case ACTIVE:
      case STOPPING:
        Exception exception;
        try {
          waitOnOperation(fwCtx.packages, "Bundle.uninstall", true);
          exception = (state & (ACTIVE|STARTING)) != 0 ? stop0() : null;
        } catch (Exception se) {
          // Force to install
          setStateInstalled(false);
          fwCtx.packages.notifyAll();
          exception = se;
        }
        operation = UNINSTALLING;
        if (exception != null) {
          fwCtx.listeners.frameworkError(this, exception);
        }
        // Fall through
      case RESOLVED:
      case INSTALLED:
        fwCtx.bundles.remove(location);
        if (operation != UNINSTALLING) {
          try {
            waitOnOperation(fwCtx.packages, "Bundle.uninstall", true);
            operation = UNINSTALLING;
          } catch (BundleException be) {
            // Make sure that bundleContext is invalid
            if (bundleContext != null) {
              bundleContext.invalidate();
              bundleContext = null;
            }
            operation = UNINSTALLING;
            fwCtx.listeners.frameworkError(this, be);
          }
        }
        if (isFragment()) {
          if (isAttached()) {
            if (isExtension()) {
              if (isBootClassPathExtension()) {
                fwCtx.systemBundle.bootClassPathHasChanged = true;
              }
            } else {
              for (Iterator i = fragment.getHosts(); i.hasNext(); ) {
                BundleImpl hb = (BundleImpl)i.next();
                if (hb.bpkgs != null) {
                  hb.bpkgs.fragmentIsZombie(this);
                }
              }
            }
            fragment.removeHost(null);
          } else {
            secure.purge(this, protectionDomain);
            doPurge = true;
          }
        } else { // Non-fragment bundle
          // Try to unregister this bundle's packages
          boolean pkgsUnregistered = bpkgs.unregisterPackages(false);

          if (pkgsUnregistered) {
            // No exports in use, clean up.
            if (classLoader != null) {
              if(classLoader instanceof BundleClassLoader) {
                ((BundleClassLoader)classLoader).purge(false);
              }
              classLoader = null;
            } else {
              secure.purge(this, protectionDomain);
            }
            doPurge = true;
          } else {
            // Exports are in use, save as zombie packages
            saveZombiePackages();
          }

          if (isFragmentHost()) {
            detachFragments();
          }
        }

        state = INSTALLED; 
        cachedHeaders = getHeaders0(null);
        bpkgs = null;
        bactivator = null;
        bundleThread().bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
        // Purge old archive
        if (doPurge) {
          archive.purge();
          archive = null;
        }
        if (bundleDir != null) {
          if (bundleDir.exists() && !bundleDir.delete()) {
            fwCtx.listeners.frameworkError(this,
                                           new IOException("Failed to delete bundle data"));
          }
          bundleDir = null;
        }
        modified();
        // id, location and headers survives after uninstall.
        state = UNINSTALLED;  
        operation = IDLE;
        break;
      }
    }
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, this));
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
    checkUninstalled();
    Set sr = fwCtx.services.getRegisteredByBundle(this);
    secure.filterGetServicePermission(sr);
    if (sr.size() > 0) {
      ServiceReference[] res = new ServiceReference[sr.size()];
      int pos = 0;
      for (Iterator i = sr.iterator(); i.hasNext(); ) {
        res[pos++] = ((ServiceRegistration)i.next()).getReference();
      }
      return res;
    }
    return null;
  }


  /**
   * Get services that this bundle uses.
   *
   * @see org.osgi.framework.Bundle#getServicesInUse
   */
  public ServiceReference[] getServicesInUse() {
    checkUninstalled();
    Set sr = fwCtx.services.getUsedByBundle(this);
    secure.filterGetServicePermission(sr);
    if (sr.size() > 0) {
      ServiceReference[] res = new ServiceReference[sr.size()];
      int pos = 0;
      for (Iterator i = sr.iterator(); i.hasNext(); ) {
        res[pos++] = ((ServiceRegistration)i.next()).getReference();
      }
      return res;
    }
    return null;
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
   * Returns this bundle's BundleContext.
   *
   * @see org.osgi.framework.Bundle#getBundleContext
   * @since org.osgi.framework 1.4
   */
  public BundleContext getBundleContext() {
    secure.checkContextAdminPerm(this);
    return bundleContext;
  }


  /**
   * @see org.osgi.framework.Bundle#getResource(String name)
   */
  public URL getResource(String name) {
    checkUninstalled();
    if (secure.okResourceAdminPerm(this) && !isFragment()) {
      if (getUpdatedState() != INSTALLED) {
        ClassLoader cl0 = classLoader;
        if (cl0 != null) {
          return cl0.getResource(name);
        }
      }
      // NYI! We should search jar if bundle is unresolved.
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
   *
   * @see org.osgi.framework.Bundle#getLastModified()
   */
  public long getLastModified() {
    return lastModified;
  }


  /**
   *
   * @see org.osgi.framework.Bundle#getSignerCertificates()
   */
  public Map/* X509Certificate -> List(X509Certificate) */getSignerCertificates(int signersType) {
    boolean onlyTrusted;
    if (signersType == SIGNERS_ALL) {
      onlyTrusted = false;
    } else if (signersType == SIGNERS_TRUSTED) {
      onlyTrusted = true;
    } else {
      throw new IllegalArgumentException("signersType not SIGNER_ALL or SIGNERS_TRUSTED");
    }

    if (archive != null) {
      List cs = archive.getCertificateChains(onlyTrusted);
      if (cs != null) {
        Map res = new HashMap();
        for (Iterator i = cs.iterator(); i.hasNext(); ) {
          ArrayList chain = (ArrayList)i.next();
          res.put(chain.get(0), chain.clone());
        }
        return res;
      }
    }
    return Collections.EMPTY_MAP;
  }


  /**
   *
   * @see org.osgi.framework.Bundle#getVersion()
   */
  public Version getVersion() {
    return version;
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
      try {
        synchronized (fwCtx.packages) {
          waitOnOperation(fwCtx.packages, "Bundle.resolve", false);
          if (state == INSTALLED) {
          // NYI! check EE for fragments
            String ee = archive.getAttribute(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
            if (ee != null) {
              if (fwCtx.debug.packages) {
                fwCtx.debug.println("bundle #" + archive.getBundleId() + " has EE=" + ee);
              }
              if (!fwCtx.isValidEE(ee)) {
                throw new BundleException("Unable to resolve bundle: Execution environment '" +
                                          ee + "' is not supported",
                                          BundleException.RESOLVE_ERROR);
              }
            }
            if (isFragment()) {
              List /* BundleImpl */ hosts = fragment.targets();
              if (hosts != null) {
                for (Iterator i = hosts.iterator(); i.hasNext(); ) {
                  BundleImpl host = (BundleImpl)i.next();
                  if (host.state == INSTALLED) {
                    // Try resolve our host
                    // NYI! Detect circular attach
                    host.getUpdatedState();
                  } else if (!fragment.isHost(host)) {
                    // NYI! dynamic attach?
                  }
                }
                if (state == INSTALLED && fragment.hasHosts()) {
                  state = RESOLVED;
                  operation = RESOLVING;
                  bundleThread().bundleChanged(new BundleEvent(BundleEvent.RESOLVED, this));
                  operation = IDLE;
                }
              }
            } else {
              // TODO, should we do this as a part of package resolving.
              attachFragments();
              if (bpkgs.resolvePackages()) {
                classLoader =  (ClassLoader)
                  secure.newBundleClassLoader(bpkgs, archive, fragments, protectionDomain);
                resolveFailException = null;
                state = RESOLVED;
                operation = RESOLVING;
                if (fragments != null) {
                  for (Iterator i = fragments.iterator(); i.hasNext(); ) {
                    BundleImpl fb = (BundleImpl)i.next();
                    fb.getUpdatedState();
                  }
                }
                bundleThread().bundleChanged(new BundleEvent(BundleEvent.RESOLVED, this));
                operation = IDLE;
              } else {
                throw new BundleException("Unable to resolve bundle: " +
                                          bpkgs.getResolveFailReason(),
                                          BundleException.RESOLVE_ERROR);
              }
            }
          }
        }
      } catch (BundleException be) {
        resolveFailException = be;
        detachFragments();
        fwCtx.listeners.frameworkError(this, be);
      }
    }
    return state;
  }


  /**
   * Resolve fragment
   */
  boolean resolveFragment(BundleImpl host) {
    if (isFragment() && secure.okFragmentBundlePerm(this)) {
      try {
        host.attachFragment(this);
        fragment.addHost(host);
        return true;
      } catch (Exception _ignore) { 
        // TODO, Log this?
      }
    }
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
    return classLoader;
  }


  /**
   * Get class loader for this bundle.
   * Create the classloader if we haven't done this previously.
   * This method can only be called when the bundle is resolved.
   *
   * @return Bundles classloader.
   */
  void setClassLoader(ClassLoader cl) {
    classLoader = cl;
  }


  /**
   * Set state to INSTALLED and throw away our classloader.
   * Reset all package registration.
   * We assume that the bundle is resolved when entering this method.
   */
  void setStateInstalled(boolean sendEvent) {
    synchronized (fwCtx.packages) {
      // Make sure that bundleContext is invalid
      if (bundleContext != null) {
        bundleContext.invalidate();
        bundleContext = null;
      }
      if (isFragment()) {
        classLoader = null;
        fragment.removeHost(null);
      } else {
        if (classLoader != null) {
          if (classLoader instanceof BundleClassLoader) {
            ((BundleClassLoader)classLoader).close();
          }
          classLoader = null;
        }
        bpkgs.unregisterPackages(true);
        if (isFragmentHost()) {
          detachFragments();
        }
        bpkgs.registerPackages();
      }

      state = INSTALLED;
      if (sendEvent) {
        operation = UNRESOLVING;
        bundleThread().bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
      }
      operation = IDLE;
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
      return classLoader;
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
          ((BundleClassLoader)obj).purge(true);
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
        return ((BundleClassLoader)classLoader).getBundleArchive(frag);
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
  URL getURL(long gen, long fragId, int subId, String path) {
    try {
      StringBuffer u = new StringBuffer(BundleURLStreamHandler.PROTOCOL);
      u.append("://");
      u.append(id);
      if (gen != -1) {
        u.append('.').append(gen);
      }
      if (fragId != -1 && fragId != id) {
        u.append('_').append(fragId);
      }
      if (subId >= 0) {
        u.append(':').append(subId);
      }
      if (!path.startsWith("/")) {
        u.append('/');
      }
      u.append(path);
      return secure.getBundleURL(fwCtx, u.toString());
    } catch (MalformedURLException e) {
      return null;
    }
  }


  //
  // Private methods
  //

  /**
   * Check bundle certificates
   */
  private void checkCertificates(BundleArchive ba) {
    ArrayList cs = ba.getCertificateChains(false);
    if (cs != null) {
      if (fwCtx.validator != null) {
        if (fwCtx.debug.certificates) {
          fwCtx.debug.println("Validate certs for bundle #" + ba.getBundleId());
        }
        cs = (ArrayList)cs.clone();
        for (Iterator vi = fwCtx.validator.iterator(); !cs.isEmpty() && vi.hasNext();) {
          Validator v = (Validator)vi.next();
          for (Iterator ci = cs.iterator(); ci.hasNext();) {
            List c = (List)ci.next();
            if (v.validateCertificateChain(c)) {
              ba.trustCertificateChain(c);
              ci.remove();
              if (fwCtx.debug.certificates) {
                fwCtx.debug.println("Validated cert: " + c.get(0));
              }
            } else {
              if (fwCtx.debug.certificates) {
                fwCtx.debug.println("Failed to validate cert: " + c.get(0));
              }
            }
          }
        }
        if (cs.isEmpty()) {
          // Ok, bundle is signed and validated!
          return;
        }
      }
    }
    if (fwCtx.props.getBooleanProperty(FWProps.ALL_SIGNED_PROP)) {
      throw new IllegalArgumentException("All installed bundles must be signed!");
    }
  }


  /**
   * Check manifest and cache certain manifest headers as variables.
   */
  private void checkManifestHeaders(Object checkContext) {
    boolean newV2Manifest;
    String newSymbolicName;
    Version newVersion;
    String newAttachPolicy;
    boolean newSingleton;
    Fragment newFragment;
    boolean newLazyActivation;
    HashSet newLazyIncludes;
    HashSet newLazyExcludes;
    // TBD, v2Manifest unnecessary to cache?
    String mv = archive.getAttribute(Constants.BUNDLE_MANIFESTVERSION);
    newV2Manifest = mv != null && mv.trim().equals("2");
    Iterator i = Util.parseEntries(Constants.BUNDLE_SYMBOLICNAME,
                                   archive.getAttribute(Constants.BUNDLE_SYMBOLICNAME),
                                   true, true, true);
    Map e = null;
    if (i.hasNext()) {
      e = (Map)i.next();
      newSymbolicName = (String)e.get("$key");
    } else {
      if (newV2Manifest) {
        throw new IllegalArgumentException("Bundle has no symbolic name, location=" +
                                           location);
      } else {
        newSymbolicName = null;
      }
    }
    String mbv = archive.getAttribute(Constants.BUNDLE_VERSION);
    if (mbv != null) {
      try {
        newVersion = new Version(mbv);
      } catch (Throwable ee) {
        if (newV2Manifest) {
          throw new IllegalArgumentException("Bundle does not specify a valid " +
              Constants.BUNDLE_VERSION + " header. Got exception: " + ee.getMessage());
        } else {
          newVersion = Version.emptyVersion;
        }
      }

    } else {
      newVersion = Version.emptyVersion;
    }

    newAttachPolicy = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
    if (e != null) {
      newSingleton = "true".equals((String)e.get(Constants.SINGLETON_DIRECTIVE));
      BundleImpl snb = fwCtx.bundles.getBundle(newSymbolicName, newVersion);
      String tmp = (String)e.get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
      newAttachPolicy = tmp == null ? Constants.FRAGMENT_ATTACHMENT_ALWAYS : tmp;
      // TBD! Should we allow update to same version?
      if (snb != null && snb != this) {
        throw new IllegalArgumentException("Bundle with same symbolic name and version " +
                                           "is already installed (" + newSymbolicName + ", " +
                                           newVersion);
      }
    } else {
      newSingleton = false;
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

        secure.checkExtensionLifecycleAdminPerm(this, checkContext);
        if (!secure.okAllPerm(this)) {
          throw new IllegalArgumentException("An extension bundle must have AllPermission");
        }

        if (!fwCtx.props.getBooleanProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION) &&
            Constants.EXTENSION_FRAMEWORK.equals(extension)) {
          throw new UnsupportedOperationException("Framework extension bundles are not supported "
                                                  + "by this framework. Consult the documentation");
        }
        if (!fwCtx.props.getBooleanProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION) &&
            Constants.EXTENSION_BOOTCLASSPATH.equals(extension)) {
          throw new UnsupportedOperationException("Bootclasspath extension bundles are not supported "
                                                  + "by this framework. Consult the documentation");
        }
      } else {
        if (extension != null) {
          throw new IllegalArgumentException("Did not recognize directive " +
                                             Constants.EXTENSION_DIRECTIVE
                                             + ":=" + extension + "." );
        }
      }

      newFragment = new Fragment(key,
                                 extension,
                                 (String)e.get(Constants.BUNDLE_VERSION_ATTRIBUTE));
    } else {
      newFragment = null;
    }

    i = Util.parseEntries(Constants.BUNDLE_ACTIVATIONPOLICY,
                          archive.getAttribute(Constants.BUNDLE_ACTIVATIONPOLICY),
                          true, true, true);
    newLazyIncludes = null;
    newLazyExcludes = null;
    if (i.hasNext()) {
      e = (Map)i.next();
      newLazyActivation = Constants.ACTIVATION_LAZY.equals(e.get("$key"));
      if (newLazyActivation) {
        if (e.containsKey(Constants.INCLUDE_DIRECTIVE)) {
          newLazyIncludes = 
            Util.parseEnumeration(Constants.INCLUDE_DIRECTIVE,
                                  (String) e.get(Constants.INCLUDE_DIRECTIVE));
        }

        if (e.containsKey(Constants.EXCLUDE_DIRECTIVE)) {
          newLazyExcludes =
            Util.parseEnumeration(Constants.EXCLUDE_DIRECTIVE,
                                  (String) e.get(Constants.EXCLUDE_DIRECTIVE));

          if (newLazyIncludes != null) {
            for (Iterator excsIter = newLazyExcludes.iterator(); excsIter.hasNext();) {
              String entry = (String)excsIter.next();
              if (newLazyIncludes.contains(entry)) {
                throw new IllegalArgumentException
                  ("Conflicting " +Constants.INCLUDE_DIRECTIVE
                   +"/" +Constants.EXCLUDE_DIRECTIVE
                   +" entries in " +Constants.BUNDLE_ACTIVATIONPOLICY+": '"
                   +entry +"' both included and excluded");
              }
            }
          }
        }
      }
    } else {
      newLazyActivation = false;
    }
    v2Manifest = newV2Manifest;
    symbolicName = newSymbolicName;
    version = newVersion;
    attachPolicy = newAttachPolicy;
    singleton = newSingleton;
    fragment = newFragment;
    lazyActivation = newLazyActivation;
    lazyIncludes = newLazyIncludes;
    lazyExcludes = newLazyExcludes;
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
   * Register all our import and export packages.
   *
   */
  private void doExportImport() {
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
    fwCtx.listeners.removeAllListeners(bundleContext);
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
      ServiceReferenceImpl sri = ((ServiceRegistrationImpl)i.next()).reference;
      if (sri != null) {
        sri.ungetService(this, false);
      }
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
    oldClassLoaders.put(bpkgs, classLoader);
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

    if (archive != null) {
      try {
        archive.setStartLevel(n);
      } catch (Exception e) {
        fwCtx.listeners.frameworkError(this,
            new BundleException("Failed to set start level on #" + id, e));
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
        InputStream is = secure.callGetBundleResourceStream(archive, name, 0);
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
    // TBD, should we do like this and allow mixed locales?
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
    if (!locale.equals("")) {
      locale = "_" + locale;
    }
    BundleClassLoader cl = null;
    if ((state & RESOLVED_FLAGS) != 0) {
      // NYI, don't use classLoader here since it confuses the reader
      if (isFragment()) {
        Iterator i = fragment.getHosts();
        BundleImpl best = null;
        while (i.hasNext()) {
          BundleImpl b = (BundleImpl)i.next();
          if (best == null || b.version.compareTo(best.version) > 0) {
            best = b;
          }
        }
        if (best == fwCtx.systemBundle) {
          best.readLocalization(locale, localization_entries, baseName);
          return;
        }
        cl = (BundleClassLoader)best.getClassLoader();
      } else {
        cl = (BundleClassLoader)classLoader;
      }
    }
    while (true) {
      String l = baseName + locale + ".properties";
      Hashtable res;
      if (cl != null) {
        res = cl.getLocalizationEntries(l);
      } else {
        res = archive.getLocalizationEntries(l);
      }
      if (res != null) {
        localization_entries.putAll(res);
        break;
      }
      int pos = locale.lastIndexOf('_');
      if (pos == -1) {
        break;
      }
      locale = locale.substring(0, pos);
    }
  }


  /**
   * @see org.osgi.framework.Bundle#getHeaders(String locale)
   */
  public Dictionary getHeaders(String locale) {
    secure.checkMetadataAdminPerm(this);
    return secure.callGetHeaders0(this, locale);
  }

  HeaderDictionary getHeaders0(String locale) {
    synchronized (fwCtx.packages) {
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
      return localize(getLocaleDictionary(locale, base));
    }
  }


  /**
   *
   */
  private void modified() {
    lastModified = System.currentTimeMillis();
    //TODO make sure it is persistent
    if (archive != null) {
      try {
        archive.setLastModified(lastModified);
      } catch(IOException e) {
        // NYI! Log this
      }
    }
  }
  /**
   *
   * @see org.osgi.framework.Bundle#getResources(String name)
   */
  public Enumeration getResources(String name) throws IOException {
    checkUninstalled();
    if (secure.okResourceAdminPerm(this) && !isFragment()) {
      if (getUpdatedState() != INSTALLED) {
        ClassLoader cl0 = classLoader;
        if (cl0 != null) {
          Enumeration e = cl0 instanceof BundleClassLoader
            ? ((BundleClassLoader) cl0).getResourcesOSGi(name)
            : cl0.getResources(name);
          return e != null && e.hasMoreElements() ? e : null;
        }
      }
      // NYI! We should search jar if bundle is unresolved.
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
      if (isFragment()) {
        throw new ClassNotFoundException("Can not load classes from fragment/extension bundles");
      }
      if (getUpdatedState() == INSTALLED) {
        throw new ClassNotFoundException(resolveFailException.getMessage());
      }

      ClassLoader cl = classLoader;
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
    return isExtension() && (state & (INSTALLED|UNINSTALLED)) != 0;
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
    return isFragment() && fragment.hasHosts();
  }


  /**
   * Returns the name of the bundle's fragment host.
   * Returns null if this is not a fragment.
   */
  String getFragmentHostName() {
    if (isFragment()) {
      return fragment.hostName;
    } else {
      return null;
    }
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
          fb.resolveFragment(this);
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

    if(fwCtx.debug.packages) {
      fwCtx.debug.println("Fragment(id=" +fragmentBundle.getBundleId()
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
  private void detachFragments() {
    if (fragments != null) {
      while (fragments.size() > 0) {
        detachFragment((BundleImpl)fragments.get(0));
      }
    }
  }


  /**
   * Detach fragment from this bundle.
   */
  private void detachFragment(BundleImpl fb) {
    // NYI! extensions
    if (fragments.remove(fb)) {
      bpkgs.detachFragment(fb);
      if(fwCtx.debug.packages) {
        fwCtx.debug.println("Fragment(id=" +fb.getBundleId()
                            +") detached from host(id=" +bpkgs.bundle.id
                            +",gen=" +bpkgs.generation +")");
      }
      if (fb.state != UNINSTALLED) {
        fb.fragment.removeHost(this);
        if (!fb.fragment.hasHosts()) {
          fb.setStateInstalled(true);
        }
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

  // Lazy bundles in state STARTING must not be actiavted during shutdown
  boolean triggersActivationPkg(String pkg) {
    return Bundle.STOPPING!= fwCtx.systemBundle.getState()
      && state == Bundle.STARTING
      && operation != ACTIVATING
      && lazyActivation
      && isPkgActivationTrigger(pkg);
  }

  // Lazy bundles in state STARTING must not be actiavted during shutdown
  boolean triggersActivationCls(String name) {
    if (Bundle.STOPPING!= fwCtx.systemBundle.getState()
        && state == Bundle.STARTING
        && operation != ACTIVATING && lazyActivation) {
      String pkg = "";
      int pos = name.lastIndexOf('.');
      if (pos != -1) {
        pkg = name.substring(0, pos);
      }
      return isPkgActivationTrigger(pkg);
    }
    return false;
  }


  BundleThread bundleThread() {
    synchronized (fwCtx.bundleThreads) {
      if (fwCtx.bundleThreads.isEmpty()) {
        return secure.createBundleThread(fwCtx);
      } else {
        return (BundleThread)fwCtx.bundleThreads.removeFirst();
      }
    }
  }

  /**
   * TBD refactor
   */
  class Fragment {
    final String hostName;
    final String extension;
    final VersionRange versionRange;
    private List /* BundleImpl */ hosts = new ArrayList(2);

    Fragment(String hostName, String extension, String range) {
      this.hostName = hostName;
      this.extension = extension;
      this.versionRange = range == null ?
        VersionRange.defaultVersionRange :
        new VersionRange(range);
    }


    void addHost(BundleImpl host) {
      hosts.add(host);
    }


    void removeHost(BundleImpl host) {
      if (host == null) {
        hosts.clear();
      } else {
        hosts.remove(host);
      }
    }


    boolean isHost(BundleImpl host) {
      return hosts.contains(host);
    }


    Iterator getHosts() {
      return hosts.iterator();
    }


    boolean hasHosts() {
      return !hosts.isEmpty();
    }


    Bundle [] hostsArray() {
      return (Bundle [])hosts.toArray(new Bundle [hosts.size()]);
    }


    boolean isTarget(BundleImpl b) {
      return hostName.equals(b.symbolicName) && versionRange.withinRange(b.version);
    }


    List /* BundleImpl */ targets() {
      List bundles = fwCtx.bundles.getBundles(hostName, versionRange);
      for (Iterator iter = bundles.iterator(); iter.hasNext(); ) {
        BundleImpl t = (BundleImpl)iter.next();

        if (t.state == UNINSTALLED ||
            t.attachPolicy.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
          iter.remove();
        }
      }

      if (bundles.isEmpty()) {
        return null;
      }
      return bundles;
    }
  }//class Fragment

}//class BundleImpl
