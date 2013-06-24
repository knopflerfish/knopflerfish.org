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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWiring;

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
   * State of bundle.
   */
  volatile int state;

  /**
   * Bundle generation data.
   */
  protected final Vector<BundleGeneration> generations;

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
   * Stores the default locale entries when uninstalled.
   */
  volatile private HeaderDictionary cachedHeaders = null;

  /**
   * Type of operation in progress. Blocks bundle calls during activator and
   * listener calls
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

  /** Remember if bundle was started */
  private boolean wasStarted;

  /** start/stop time-out/uninstall flag, see BundleThread */
  volatile Boolean aborted;

  /** current bundle thread */
  private BundleThread bundleThread;
  
  /**
   * Construct a new Bundle empty.
   *
   * Only called for system bundle
   *
   * @param fw Framework for this bundle.
   */
  BundleImpl(FrameworkContext fw) {
    fwCtx = fw;
    secure = fwCtx.perm;
    id = 0;
    generations = new Vector<BundleGeneration>(1);
    location = Constants.SYSTEM_BUNDLE_LOCATION;
    state = INSTALLED;
    bundleDir = fwCtx.getDataStorage(id);
  }


  /**
   * Construct a new Bundle based on a BundleArchive.
   *
   * @param fw FrameworkContext for this bundle.
   * @param ba Bundle archive with holding the contents of the bundle.
   * @param checkContext AccessConrolContext to do permission checks against.
   * @throws BundleException If we have duplicate symbolicname and version.
   * @exception IOException If we fail to read and store our JAR bundle or if
   *              the input data is corrupted.
   * @exception SecurityException If we don't have permission to install
   *              extension.
   * @exception IllegalArgumentException Faulty manifest for bundle
   */
  BundleImpl(FrameworkContext fw, BundleArchive ba, Object checkContext) throws BundleException {
    fwCtx = fw;
    secure = fwCtx.perm;
    id = ba.getBundleId();
    location = ba.getBundleLocation();
    state = INSTALLED;
    generations = new Vector<BundleGeneration>(2);
    final BundleGeneration gen = new BundleGeneration(this, ba, null);
    generations.add(gen);
    gen.checkPermissions(checkContext);
    doExportImport();
    bundleDir = fwCtx.getDataStorage(id);

    // Activate extension as soon as they are installed so that
    // they get added in bundle id order.
    if (gen.isExtension() && attachToFragmentHost(fwCtx.systemBundle.current())) {
      gen.setWired();
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

    synchronized (fwCtx.resolver) {
      final BundleGeneration current = current();
      if (current.isFragment()) {
        throw new BundleException("Bundle#" + id +
                                  ", cannot start a fragment bundle",
                                  BundleException.INVALID_OPERATION);
      }
      if (state == UNINSTALLED) {
        throw new IllegalStateException("Bundle is uninstalled");
      }
      fwCtx.resolverHooks.checkResolveBlocked();

      // The value -1 is used by this implementation to indicate a bundle
      // that has not been started, thus ensure that options is != -1.
      options &= 0xFF;

      if (fwCtx.startLevelController != null) {
        if (getStartLevel() > fwCtx.startLevelController.getStartLevel()) {
          if ((options & START_TRANSIENT) != 0) {
            throw new BundleException("Bundle#" + id +
                                      ", can not transiently activate bundle with start level "
                                      + getStartLevel() + " when running on start level "
                                      + fwCtx.startLevelController.getStartLevel(),
                                      BundleException.START_TRANSIENT_ERROR);
          } else {
            setAutostartSetting(options);
            return;
          }
        }
      }

      // Initialize the activation; checks initialization of lazy
      // activation.

      // 1: If an operation is in progress, wait a little
      waitOnOperation(fwCtx.resolver, "Bundle.start", false);

      // 2: start() is idempotent, i.e., nothing to do when already started
      if (state == ACTIVE) {
        return;
      }

      // 3: Record non-transient start requests.
      if ((options & START_TRANSIENT) == 0) {
        setAutostartSetting(options);
      }

      // 5: Lazy?
      if ((options & START_ACTIVATION_POLICY) != 0 && current.lazyActivation) {
        // 4: Resolve bundle (if needed)
        if (INSTALLED == getUpdatedState(new BundleImpl [] { this })) {
          throw resolveFailException;
        }
        if (STARTING == state)
          return;
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
    synchronized (fwCtx.resolver) {
      operation = IDLE;
      fwCtx.resolver.notifyAll();
    }
  }


  // Performs the actual activation.
  void finalizeActivation() throws BundleException {
    synchronized (fwCtx.resolver) {
      // 4: Resolve bundle (if needed)
      switch (getUpdatedState(new BundleImpl [] { this })) {
      case INSTALLED:
        throw resolveFailException;
      case STARTING:
        if (operation == ACTIVATING) {
          // finalization already in progress.
          return;
        }
        // Lazy activation; fall through to RESOLVED.
      case RESOLVED:
        // 6:
        state = STARTING;
        operation = ACTIVATING;
        if (fwCtx.debug.lazy_activation) {
          fwCtx.debug.println("activating #" + getBundleId());
        }
        // 7:
        if (null == bundleContext) {
          bundleContext = new BundleContextImpl(this);
        }
        final BundleException e = bundleThread().callStart0(this);
        operation = IDLE;
        fwCtx.resolver.notifyAll();
        if (e != null) {
          throw e;
        }
        break;
      case ACTIVE:
        break;
      case STOPPING:
        // This happens if call start from inside the BundleActivator.stop
        // method.
        // Don't allow it.
        throw new BundleException("Bundle#" + id +
                                  ", start called from BundleActivator.stop",
                                  BundleException.ACTIVATOR_ERROR);
      case UNINSTALLED:
        throw new IllegalStateException("Bundle is in UNINSTALLED state");
      }
    }
  }


  /**
   * Start code that is executed in the bundleThread without holding the
   * packages lock.
   */
  BundleException start0() {
    final BundleArchive archive = current().archive;
    final String ba = archive.getAttribute(Constants.BUNDLE_ACTIVATOR);
    boolean bStarted = false;
    // res is used to signal that start did not complete in a normal way
    BundleException res = null;

    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTING, this));

    // If SETCONTEXTCLASSLOADER, set the thread's context
    // class loader to the bundle class loader. This
    // is useful for debugging external libs using
    // the context class loader.
    ClassLoader oldLoader = null;
    if (fwCtx.props.SETCONTEXTCLASSLOADER) {
      oldLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(getClassLoader());
    }

    int error_type = BundleException.MANIFEST_ERROR;
    try {
      if (ba != null) {
        @SuppressWarnings("unchecked")
        final
		Class<BundleActivator> c =
            (Class<BundleActivator>) getClassLoader().loadClass(ba.trim());
        error_type = BundleException.ACTIVATOR_ERROR;
        bactivator = c.newInstance();

        bactivator.start(bundleContext);
        bStarted = true;
      } else {
        final String locations = fwCtx.props.getProperty(FWProps.MAIN_CLASS_ACTIVATION_PROP);
        if (locations.length() > 0) {
          final String mc = archive.getAttribute("Main-Class");

          if (mc != null) {
            final String[] locs = Util.splitwords(locations, ",");
            for (final String loc : locs) {
              if (loc.equals(location)) {
                if (fwCtx.debug.resolver) {
                  fwCtx.debug.println("starting main class " + mc);
                }
                error_type = BundleException.ACTIVATOR_ERROR;
                final Class<?> mainClass = getClassLoader().loadClass(mc.trim());
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

    } catch (Throwable t) {
      res = new BundleException("Bundle#" + id + " start failed", error_type, t);
    }
    
    // activator.start() done
    // - normal -> state = active, started event
    // - exception from start() -> res = ex, start-failed clean-up
    // - unexpected state change (uninstall, refresh?):
    //   -> res = new exception
    // - start time-out -> res = new exception (not used?)    

    // if start was aborted (uninstall or timeout), make sure
    // finalizeActivation() has finished before checking aborted/state
    synchronized (fwCtx.resolver) {
      if (!isBundleThread(Thread.currentThread())) {
        // newer BundleThread instance has been active for this BundleImpl,
        // end thread execution 
        throw new RuntimeException("Aborted bundle thread ending execution");
      }
      
      Exception cause = null;
      if (aborted == Boolean.TRUE) {
        if (UNINSTALLED == state) {
          cause = new Exception("Bundle uninstalled during start()");
        } else {
          cause = new Exception("Bundle activator start() time-out");
        }
      } else {
        aborted = Boolean.FALSE; // signal to other thread that BundleThread
                                 // concludes start/stop
        if (STARTING != state) {
          cause = new Exception("Bundle changed state because of refresh during start()");
        }
      }
      if (cause != null) {
        res = new BundleException("Bundle#" + id + " start failed",
                                BundleException.STATECHANGE_ERROR, cause);
      }
    }
    
    if (fwCtx.debug.lazy_activation) {
      fwCtx.debug.println("activating #" + getBundleId() + " completed.");
    }
    if (fwCtx.props.SETCONTEXTCLASSLOADER) {
      Thread.currentThread().setContextClassLoader(oldLoader);
    }

    if (res == null) {
      // 10:
      state = ACTIVE;
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTED, this));
    } else if (operation == ACTIVATING) {
      // 8:
      startFailed();
    }
    return res;
  }

  void startFailed()
  {
    // 8:
    state = STOPPING;
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));
    removeBundleResources();
    bundleContext.invalidate();
    bundleContext = null;
    state = RESOLVED;
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPED, this));
  }

  /**
   * Stop this bundle.
   *
   * @see org.osgi.framework.Bundle#stop
   */
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

    synchronized (fwCtx.resolver) {
      if (current().isFragment()) {
        throw new BundleException("Bundle#" + id + ", can not stop a fragment",
                                  BundleException.INVALID_OPERATION);
      }

      // 1:
      if (state == UNINSTALLED) {
        throw new IllegalStateException("Bundle is uninstalled");
      }

      // 2: If an operation is in progress, wait a little
      waitOnOperation(fwCtx.resolver, "Bundle.stop", false);

      // 3:
      if ((options & STOP_TRANSIENT) == 0) {
        setAutostartSetting(-1);
      }
      switch (state) {
      case INSTALLED:
      case RESOLVED:
      case STOPPING:
      case UNINSTALLED:
        // 4:
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
    // 5:
    state = STOPPING;
    operation = DEACTIVATING;
    // 6-13:
    final Exception savedException = bundleThread().callStop1(this);
    if (state != UNINSTALLED) {
      state = RESOLVED;
      bundleThread().bundleChanged(new BundleEvent(BundleEvent.STOPPED, this));
      fwCtx.resolver.notifyAll();
      operation = IDLE;
    }
    return savedException;
  }


  /**
   * Stop code that is executed in the bundleThread without holding the packages
   * lock.
   */
  Exception stop1() {
    BundleException res = null;

    // 6:
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));

    // 7:
    if (wasStarted && bactivator != null) {
      try {
        bactivator.stop(bundleContext);
      } catch (final Throwable e) {
        res = new BundleException("Bundle#" + id + ", BundleActivator.stop() failed",
                                  BundleException.ACTIVATOR_ERROR, e);
      }

      // if stop was aborted (uninstall or timeout), make sure
      // finalizeActivation() has finished before checking aborted/state
      synchronized (fwCtx.resolver) {
        Exception cause = null;
        if (aborted == Boolean.TRUE) {
          if (UNINSTALLED == state) {
            cause = new Exception("Bundle uninstalled during stop()");
          } else {
            cause = new Exception("Bundle activator stop() time-out");
          }
        } else {
          aborted = Boolean.FALSE; // signal to other thread that BundleThread
                                   // concludes stop
          if (STOPPING != state) {
            cause = new Exception("Bundle changed state because of refresh during stop()");
          }
        }
        if (cause != null) {
          res = new BundleException("Bundle stop failed",
                                  BundleException.STATECHANGE_ERROR, cause);
        }
      }
      bactivator = null;
    }

    if (operation == DEACTIVATING) {
      stop2();
    }

    return res;
  }

  void stop2()
  {
    // Call hooks after we've called Activator.stop(), but before we've
    // cleared all resources
    if (null != bundleContext) {
      fwCtx.listeners.serviceListeners.hooksBundleStopped(bundleContext);
      // 8-10:
      removeBundleResources();
      bundleContext.invalidate();
      bundleContext = null;
    }
  }


  /**
   * Wait for an ongoing operation to finish.
   *
   * @param lock Object used for locking.
   * @param src Caller to include in exception message.
   * @param longWait True, if we should wait extra long before aborting.
   * @throws BundleException if the ongoing (de-)activation does not finish
   *           within reasonable time.
   */
  void waitOnOperation(Object lock, final String src, boolean longWait) throws BundleException {
    if (operation != IDLE) {
      long left = longWait ? 20000 : 500;
      final long waitUntil = Util.timeMillis() + left;
      do {
        try {
          lock.wait(left);
          if (operation == IDLE) {
            return;
          }
        } catch (final InterruptedException _ie) {
        }
        left = waitUntil - Util.timeMillis();
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
      throw new BundleException(src + " called during " + op + " of Bundle#"
                                + id, BundleException.STATECHANGE_ERROR);
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
      if (current().isExtension()) {
        secure.checkExtensionLifecycleAdminPerm(this);
      }
      synchronized (fwCtx.resolver) {
        final boolean wasActive = state == ACTIVE;

        switch (getState()) {
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
        } catch (final IOException ignore) {
        }
      }

    }
  }


  void update0(InputStream in, boolean wasActive, Object checkContext) throws BundleException {
    final boolean wasResolved = state == RESOLVED;
    final BundleGeneration current = current();
    final Fragment oldFragment = current.fragment;
    final int oldStartLevel = getStartLevel();
    BundleArchive newArchive = null;
    BundleGeneration newGeneration = null;

    operation = UPDATING;
    try {
      // New bundle as stream supplied?
      InputStream bin;
      final BundleArchive archive = current.archive;
      if (in == null) {
        // Try Bundle-UpdateLocation
        String update = archive != null ? archive
            .getAttribute(Constants.BUNDLE_UPDATELOCATION) : null;
        if (update == null) {
          // Take original location
          update = location;
        }
        final URL url = new URL(update);

        // Handle case where bundle location is a reference and/or a directory
        // URL. In these cases, send a NULL input stream to the archive
        // indicating that is should re-use the old bundle location
        String fname = url.getFile(); // if reference URL, the getFile() result
                                      // may be a file: string
        if (fname.startsWith("file:")) {
          fname = fname.substring(5);
        }
        final File file = new File(fname);
        if (file.isDirectory()) {
          bin = null;
        } else {
          bin = url.openStream();
        }
      } else {
        bin = in;
      }

      newArchive = fwCtx.storage.updateBundleArchive(archive, bin);
      newGeneration = new BundleGeneration(this, newArchive, current);
      newGeneration.checkPermissions(checkContext);
      newArchive.setStartLevel(oldStartLevel);
      fwCtx.storage.replaceBundleArchive(archive, newGeneration.archive);
    } catch (final Exception e) {
      if (newArchive != null) {
        newArchive.purge();
      }
      operation = IDLE;
      if (wasActive) {
        try {
          start();
        } catch (final BundleException be) {
          fwCtx.frameworkError(this, be);
        }
      }
      if (e instanceof BundleException) {
        throw (BundleException)e;
      } else {
        throw new BundleException("Failed to get update Bundle#" + id , BundleException.UNSPECIFIED, e);
      }
    }

    boolean saveZombie;
    if (oldFragment != null) {
      saveZombie = oldFragment.hasHosts();
      if (saveZombie) {
        if (oldFragment.extension != null) {
          if (oldFragment.extension.equals(Constants.EXTENSION_BOOTCLASSPATH)) {
            fwCtx.systemBundle.bootClassPathHasChanged = true;
          }
        } else {
          for (final BundleGeneration bundleGeneration : oldFragment.getHosts()) {
            bundleGeneration.bpkgs.fragmentIsZombie(this);
          }
        }
      }
    } else {
      // Remove this bundle's packages
      saveZombie = !current.unregisterPackages(false);

      // Loose old bundle if no exporting packages left
      if (!saveZombie) {
        current.closeClassLoader();
      }
    }

    // Activate new bundle generation
    final BundleGeneration oldGen = current;
    state = INSTALLED;
    if (saveZombie) {
      generations.add(0, newGeneration);
      fwCtx.bundles.addZombie(this);
    } else {
      generations.set(0, newGeneration);
    }
    doExportImport();

    // Purge old archive
    if (!saveZombie) {
      oldGen.purge(false);
    }

    // Broadcast events
    if (wasResolved) {
      bundleThread().bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
    }
    bundleThread().bundleChanged(new BundleEvent(BundleEvent.UPDATED, this));
    operation = IDLE;

    // Restart bundles previously stopped in the operation
    if (wasActive) {
      try {
        start();
      } catch (final BundleException be) {
        fwCtx.frameworkError(this, be);
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
    if (current().isExtension()) {
      secure.checkExtensionLifecycleAdminPerm(this);
    }
    secure.callUninstall0(this);
  }


  void uninstall0() {
    synchronized (fwCtx.resolver) {
      final BundleGeneration current = current();
      if (!current.isUninstalled()) {
        try {
          current.archive.setStartLevel(-2); // Mark as uninstalled
        } catch (final Exception ignored) {
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
          waitOnOperation(fwCtx.resolver, "Bundle.uninstall", true);
          exception = (state & (ACTIVE | STARTING)) != 0 ? stop0() : null;
        } catch (final Exception se) {
          // Force to install
          setStateInstalled(false);
          fwCtx.resolver.notifyAll();
          exception = se;
        }
        operation = UNINSTALLING;
        if (exception != null) {
          fwCtx.frameworkError(this, exception);
        }
        // Fall through
      case RESOLVED:
      case INSTALLED:
        fwCtx.bundles.remove(location);
        if (operation != UNINSTALLING) {
          try {
            waitOnOperation(fwCtx.resolver, "Bundle.uninstall", true);
            operation = UNINSTALLING;
          } catch (final BundleException be) {
            // Make sure that bundleContext is invalid
            if (bundleContext != null) {
              bundleContext.invalidate();
              bundleContext = null;
            }
            operation = UNINSTALLING;
            fwCtx.frameworkError(this, be);
          }
        }
        if (state == UNINSTALLED) {
          operation = IDLE;
          throw new IllegalStateException("Bundle is in UNINSTALLED state");
        }
        boolean saveZombie = false;
        if (current.isFragment()) {
          if (isAttached()) {
            if (current.isExtension()) {
              if (current.isBootClassPathExtension()) {
                fwCtx.systemBundle.bootClassPathHasChanged = true;
              }
            } else {
              for (final BundleGeneration hbg : current.getHosts()) {
                if (hbg.bpkgs != null) {
                  hbg.bpkgs.fragmentIsZombie(this);
                }
              }
            }
            // Fragment in use, save as zombie generation
            saveZombie = true;
          } else {
            doPurge = true;
          }
        } else { // Non-fragment bundle
          // Try to unregister this bundle's packages
          final boolean pkgsUnregistered = current.unregisterPackages(false);

          if (pkgsUnregistered) {
            // No exports in use, clean up.
            current.closeClassLoader();
            doPurge = true;
          } else {
            // Exports are in use, save as zombie generation
            saveZombie = true;
          }
        }

        state = INSTALLED;
        bundleThread().bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
        cachedHeaders = current.getHeaders0(null);
        bactivator = null;
        state = UNINSTALLED;
        // Purge old archive
        final BundleGeneration oldGen = current;
        if (saveZombie) {
          generations.add(0, new BundleGeneration(oldGen));
          fwCtx.bundles.addZombie(this);
        } else {
          generations.set(0, new BundleGeneration(oldGen));
        }
        if (doPurge) {
          oldGen.purge(false);
        }
        operation = IDLE;
        if (bundleDir != null) {
          if (bundleDir.exists() && !bundleDir.delete()) {
            fwCtx.frameworkError(this,
                new IOException("Failed to delete bundle data"));
          }
          bundleDir = null;
        }
        // id, location and headers survives after uninstall.

        // There might be bundle threads that are running start or stop
        // operation. This will wake them and give them an chance to terminate.
        fwCtx.resolver.notifyAll();
        break;
      }
    }
    fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, this));
  }


  /**
   * Get header data. This is all entries in the bundles MANIFEST file.
   *
   * @see org.osgi.framework.Bundle#getHeaders
   */
  public Dictionary<String, String> getHeaders() {
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
  public ServiceReference<?>[] getRegisteredServices() {
    checkUninstalled();
    final Set<ServiceRegistrationImpl<?>> sr = fwCtx.services.getRegisteredByBundle(this);
    secure.filterGetServicePermission(sr);
    if (sr.size() > 0) {
      final ServiceReference<?>[] res = new ServiceReference[sr.size()];
      int pos = 0;
      for (final ServiceRegistrationImpl<?> serviceRegistrationImpl : sr) {
        res[pos++] = serviceRegistrationImpl.getReference();
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
  public ServiceReference<?>[] getServicesInUse() {
    checkUninstalled();
    final Set<ServiceRegistrationImpl<?>> sr = fwCtx.services.getUsedByBundle(this);
    secure.filterGetServicePermission(sr);
    if (sr.size() > 0) {
      final ServiceReference<?>[] res = new ServiceReference<?>[sr.size()];
      int pos = 0;
      for (final ServiceRegistrationImpl<?> serviceRegistrationImpl : sr) {
        res[pos++] = serviceRegistrationImpl.getReference();
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
    final BundleGeneration fix = current();
    checkUninstalled();
    if (permission instanceof Permission) {
      if (secure.checkPermissions()) {
        // get the current status from permission admin
        final PermissionCollection pc = fix.getProtectionDomain().getPermissions();
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
    // NYI, sync BundleGeneration
    if (secure.okResourceAdminPerm(this) && !current().isFragment()) {
      if (getUpdatedState(new BundleImpl [] { this }) != INSTALLED) {
        final ClassLoader cl0 = getClassLoader();
        if (cl0 != null) {
          return cl0.getResource(name);
        }
      } else {
        final Vector<URL> uv = secure.getBundleClassPathEntries(current(), name, true);
        if (uv != null) {
          return uv.firstElement();
        }
      }
    }
    return null;
  }


  /**
   * @see org.osgi.framework.Bundle#getSymbolicName()
   */
  public String getSymbolicName() {
    return current().symbolicName;
  }


  /**
   *
   * @see org.osgi.framework.Bundle#getLastModified()
   */
  public long getLastModified() {
    return current().timeStamp;
  }


  /**
   *
   * @see org.osgi.framework.Bundle#getSignerCertificates()
   */
  @SuppressWarnings("unchecked")
  public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
    boolean onlyTrusted;
    if (signersType == SIGNERS_ALL) {
      onlyTrusted = false;
    } else if (signersType == SIGNERS_TRUSTED) {
      onlyTrusted = true;
    } else {
      throw new IllegalArgumentException("signersType not SIGNER_ALL or SIGNERS_TRUSTED");
    }
    final BundleArchive fix = current().archive;
    if (fix != null) {
      final List<List<X509Certificate>> cs = fix.getCertificateChains(onlyTrusted);
      if (cs != null) {
        final Map<X509Certificate, List<X509Certificate>> res = new HashMap<X509Certificate, List<X509Certificate>>();
        for (final List<X509Certificate> chain : cs) {
          final List<X509Certificate> copy = new ArrayList<X509Certificate>(chain);
          res.put(chain.get(0), copy);
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
    return current().version;
  }


  public <A> A adapt(Class<A> type) {
    secure.checkAdaptPerm(this, type);
    return adaptSecure(type);
  }


  public File getDataFile(String filename) {
    return bundleContext.getDataFile(filename);
  }


  public int compareTo(Bundle bundle) {
    return new Long(getBundleId()).compareTo(new Long(bundle.getBundleId()));
  }

  //
  // Package methods
  //

  /**
   * Get updated bundle state. That means check if an installed bundle has been
   * resolved.
   *
   * @return Bundles state
   */
  int getUpdatedState(BundleImpl [] triggers) {
    if (state == INSTALLED) {
      try {
        synchronized (fwCtx.resolver) {
          waitOnOperation(fwCtx.resolver, "Bundle.resolve", true);
          if (state == INSTALLED) {
            final BundleGeneration current = current();
            if (triggers != null) {
              fwCtx.resolverHooks.beginResolve(triggers);
            }
            @SuppressWarnings("deprecation")
            final
            String ee = current.archive.getAttribute(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
            if (ee != null) {
              if (fwCtx.debug.resolver) {
                fwCtx.debug.println("bundle #" + current.archive.getBundleId() + " has EE=" + ee);
              }
              if (!fwCtx.isValidEE(ee)) {
                throw new BundleException("Bundle#" + id +
                                          ", unable to resolve: Execution environment '"
                                          + ee + "' is not supported",
                                          BundleException.RESOLVE_ERROR);
              }
            }
            if (current.isFragment()) {
              final List<BundleGeneration> hosts = current.fragment.targets();
              if (!hosts.isEmpty()) {
                for (final BundleGeneration host : hosts) {
                  if (!host.bpkgs.isActive()) {
                    // Try resolve our host
                    // NYI! Detect circular attach
                    host.bundle.getUpdatedState(null);
                  } else {
                    if (!current.fragment.isHost(host)) {
                      attachToFragmentHost(host);
                    }
                  }
                }
                if (state == INSTALLED && current.fragment.hasHosts()) {
                  current.setWired();
                  state = RESOLVED;
                  operation = RESOLVING;
                  bundleThread().bundleChanged(new BundleEvent(BundleEvent.RESOLVED, this));
                  operation = IDLE;
                }
              }
            } else {
              if (current.resolvePackages()) {
                current.setWired();
                state = RESOLVED;
                operation = RESOLVING;
                current.updateStateFragments();
                bundleThread().bundleChanged(new BundleEvent(BundleEvent.RESOLVED, this));
                operation = IDLE;
              } else {
                String reason = current.bpkgs.getResolveFailReason();
                throw new BundleException("Bundle#" + id + ", unable to resolve: "
                    + reason,
                    reason == Resolver.RESOLVER_HOOK_VETO ?
                                                           BundleException.REJECTED_BY_HOOK :
                                                             BundleException.RESOLVE_ERROR);
              }
            }
            if (triggers != null && triggers.length == 1) {
              fwCtx.resolverHooks.endResolve(triggers);
            }
          }
        }
      } catch (final BundleException be) {
        if (triggers != null && triggers.length == 1) {
          fwCtx.resolverHooks.endResolve(triggers);
        }
        resolveFailException = be;
        fwCtx.frameworkError(this, be);
      }
    }
    return state;
  }


  /**
   * Attach a fragment to host bundle
   * @throws BundleException 
   */
  boolean attachToFragmentHost(BundleGeneration host) throws BundleException {
    final BundleGeneration fix = current();
    if (fix.isFragment() && secure.okFragmentBundlePerm(this)) {
      try {
        if (fix.isExtension()) {
          fwCtx.systemBundle.attachExtension(fix);
        } else {
          host.attachFragment(fix);
        }
        fix.fragment.addHost(host);
        return true;
      } catch (final BundleException be) {
        throw be;
      } catch (final Exception e) {
        fwCtx.frameworkWarning(this, e);
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
   * Get class loader for this bundle. Create the classloader if we haven't done
   * this previously. This method can only be called when the bundle is
   * resolved.
   *
   * @return Bundles classloader.
   */
  ClassLoader getClassLoader() {
    return current().getClassLoader();
  }

  /**
   * Set state to INSTALLED and throw away our classloader. Reset all package
   * registration. We assume that the bundle is resolved when entering this
   * method.
   */
  void setStateInstalled(boolean sendEvent) {
    synchronized (fwCtx.resolver) {
      // Make sure that bundleContext is invalid
      if (bundleContext != null) {
        bundleContext.invalidate();
        bundleContext = null;
      }
      final BundleGeneration current = current();
      if (current.isFragment()) {
        current.fragment.removeHost(null);
      } else {
        current.closeClassLoader();
        current.unregisterPackages(true);
        current.bpkgs.registerPackages();
      }
      current.clearWiring();
      state = INSTALLED;
      if (sendEvent) {
        operation = UNRESOLVING;
        bundleThread().bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
      }
      operation = IDLE;
    }
  }


  /**
   * Purge any old files and data associated with this bundle.
   */
  void purge() {
    fwCtx.bundles.removeZombie(this);
    Vector<BundleGeneration> old;
    synchronized (generations) {
      final List<BundleGeneration> sub = generations.subList(1, generations.size());
      old = new Vector<BundleGeneration>(sub);
      sub.clear();
    }
    for (final BundleGeneration bg : old) {
      bg.purge(true);
    }
  }


  /**
   * Get bundle archive.
   *
   * @return BundleArchive object.
   */
  BundleArchive getBundleArchive(long generation) {
    synchronized (generations) {
      for (final BundleGeneration bg : generations) {
        if (bg.generation == generation) {
          return bg.archive;
        }
      }
    }
    return null;
  }


  /**
   * Get exported packages. Note! Can be called without packages lock held.
   *
   * @return Iterator of all exported packages as ExportPkg.
   */
  @SuppressWarnings("unchecked")
  Iterator<ExportPkg> getExports() {
    synchronized (generations) {
      HashSet<ExportPkg> res;
      if (generations.size() > 1) {
        res = new HashSet<ExportPkg>();
        for (final BundleGeneration bg : generations) {
          final BundlePackages bp = bg.bpkgs;
          if (bp != null) {
            for (final Iterator<ExportPkg> j = bp.getExports(); j.hasNext();) {
              res.add(j.next());
            }
          }
        }
        return res.iterator();
      } else {
        final BundlePackages bp = generations.get(0).bpkgs;
        if (bp != null) {
          return bp.getExports();
        } else {
          return Collections.EMPTY_LIST.iterator();
        }
      }
    }
  }


  /**
   * Get Hosts for this bundle packages.
   *
   * @return Vector of all host bundles BundleGeneration.
   */
  Vector<BundleGeneration> getHosts(final boolean zombieHosts) {
    Vector<BundleGeneration> res = null;
    if (zombieHosts) {
      synchronized (generations) {
        for (final BundleGeneration bg : generations) {
          final Vector<BundleGeneration> h = bg.getHosts();
          if (h != null) {
            if (res != null) {
              res.addAll(h);
            } else {
              res = h;
            }
          }
        }
      }
    } else {
      res = current().getHosts();
    }
    return res;
  }


  /**
   * Get a list of all BundlePackages that require the exported packages that
   * comes from this bundle.
   *
   * @return List of all requiring bundles as BundlePackages.
   */
  @SuppressWarnings("unchecked")
  List<BundlePackages> getRequiredBy() {
    List<BundlePackages> res = null;
    synchronized (generations) {
      for (final BundleGeneration bg : generations) {
        final BundlePackages bp = bg.bpkgs;
        if (bp != null) {
          final List<BundlePackages> rb = bp.getRequiredBy();
          if (res != null) {
            res.addAll(rb);
          } else {
            res = rb;
          }
        }
      }
    }
    return res != null ? res : Collections.EMPTY_LIST;
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
   * Internal version; only to be used from inside PriviledgedActions running on
   * the framework's security context.
   *
   * @param setting The autostart setting to store.
   */
  void setAutostartSetting0(int setting) {
    try {
      final BundleArchive ba = current().archive;
      if (null != ba) {
        ba.setAutostartSetting(setting);
      }
    } catch (final IOException e) {
      fwCtx.frameworkError(this, e);
    }
  }


  /**
   * Get the autostart setting from the bundle storage.
   *
   * @return the current autostart setting, "-1" if bundle not started.
   */
  int getAutostartSetting() {
    final BundleArchive ba = current().archive;
    return ba != null ? ba.getAutostartSetting() : -1;
  }


  // Start level related

  /**
   *
   */
  int getStartLevel() {
    final BundleArchive ba = current().archive;
    if (ba != null) {
      return ba.getStartLevel();
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
    final BundleArchive ba = current().archive;
    if (ba != null) {
      try {
        ba.setStartLevel(n);
      } catch (final Exception e) {
        fwCtx.frameworkError(this, new BundleException(
            "Failed to set start level on #" + id, e));
      }
    }
  }


  // Misc other

  /**
   * Return a string representing this bundle. Only return identifier, since it
   * requires AdminPermisson to get the location.
   *
   * @return a String representing this bundle.
   */
  public String toString() {
    return toString(0); // 0 is lowest detail
  }


  String toString(int detail) {
    final StringBuffer sb = new StringBuffer();

    sb.append("BundleImpl[");
    sb.append("id=" + getBundleId());
    if (detail > 0) {
      sb.append(", state=" + getState());
    }

    if (detail > 1) {
      sb.append(", startlevel=" + getStartLevel());
    }

    if (detail > 3) {
      try {
        sb.append(", autostart setting=");
        sb.append(getAutostartSetting());
      } catch (final Exception e) {
        sb.append(e.toString());
      }
    }
    if (detail > 4) {
      sb.append(", loc=" + location);
    }

    if (detail > 4) {
      sb.append(", symName=" + getSymbolicName());
    }

    sb.append("]");

    return sb.toString();
  }


  /**
   * Get bundle data. Get resources from bundle or fragment jars.
   *
   * @see org.osgi.framework.Bundle#findEntries
   */
  public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
    if (secure.okResourceAdminPerm(this)) {
      // Try to resolve, so that fragments attach.
      getUpdatedState(new BundleImpl [] { this });
      final Vector<URL> res = secure.callFindEntries(current(), path, filePattern, recurse);
      if (!res.isEmpty()) {
        return res.elements();
      }
    }
    return null;
  }


  /**
   *
   */
  public URL getEntry(String name) {
    if (secure.okResourceAdminPerm(this)) {
      checkUninstalled();
      try {
        final BundleGeneration fix = current();
        if ("/".equals(name)) {
          return fix.getURL(0, "/");
        }
        final InputStream is = secure.callGetBundleResourceStream(fix.archive, name, 0);
        if (is != null) {
          is.close();
          return fix.getURL(0, name);
        }
      } catch (final IOException _ignore) {
      }
    }
    return null;
  }


  /**
   *
   */
  public Enumeration<String> getEntryPaths(String path) {
    if (secure.okResourceAdminPerm(this)) {
      checkUninstalled();
      return secure.callFindResourcesPath(current().archive, path);
    } else {
      return null;
    }
  }


  /**
   * @see org.osgi.framework.Bundle#getHeaders(String locale)
   */
  public Dictionary<String, String> getHeaders(String locale) {
    secure.checkMetadataAdminPerm(this);
    Dictionary<String, String> res = secure.callGetHeaders0(current(), locale);
    if (res == null && cachedHeaders != null) {
      res = cachedHeaders.cloneHD();
      // If we went to uninstalled, then use saved value.
      // Otherwise try again, NYI make sure we don't inf-loop.
      if (cachedHeaders == null) {
        return getHeaders(locale);
      }
    }
    return res;
  }


  /**
   *
   * @see org.osgi.framework.Bundle#getResources(String name)
   */
  public Enumeration<URL> getResources(String name) throws IOException {
    checkUninstalled();
    // NYI! Fix BundleGeneration
    final BundleGeneration current = current();
    if (secure.okResourceAdminPerm(this) && !current.isFragment()) {
      Enumeration<URL> e = null;
      if (getUpdatedState(new BundleImpl [] { this }) != INSTALLED) {
        if (this instanceof SystemBundle) {
          e = getClassLoader().getResources(name);
        } else {
          final BundleClassLoader cl0 = (BundleClassLoader)current.getClassLoader();
          if (cl0 != null) {
            e = cl0.getResourcesOSGi(name);
          }
        }
      } else {
        final Vector<URL> uv = secure.getBundleClassPathEntries(current, name, false);
        if (uv != null) {
          e = uv.elements();
        }
      }
      if (e != null && e.hasMoreElements()) {
        return e;
      }
    }
    return null;
  }


  /**
   *
   * @see org.osgi.framework.Bundle#loadClass()
   */
  public Class<?> loadClass(final String name) throws ClassNotFoundException {
    if (secure.okClassAdminPerm(this)) {
      checkUninstalled();
      final BundleGeneration current = current();
      if (current.isFragment()) {
        throw new ClassNotFoundException("Can not load classes from fragment/extension bundles");
      }
      // NYI! Fix BundleGeneration
      if (getUpdatedState(new BundleImpl [] { this }) == INSTALLED) {
        throw new ClassNotFoundException(resolveFailException.getMessage());
      }

      ClassLoader cl;
      if (this instanceof SystemBundle) {
        cl = ((SystemBundle)this).getClassLoader();
      } else {
        cl = current.getClassLoader();
        if (cl == null) {
          throw new IllegalStateException("state is uninstalled?");
        }
      }
      return cl.loadClass(name);
    } else {
      throw new ClassNotFoundException("No AdminPermission to get class: " + name);
    }
  }

  /**
   * Get the current generation.
   *
   */
  BundleGeneration current() {
    return generations.get(0);
  }

  /**
   * Checks if this bundle is an extension bundle that is updated/uninstalled
   * and needs to be restarted.
   */
  boolean extensionNeedsRestart() {
    return current().isExtension() && (state & (INSTALLED | UNINSTALLED)) != 0;
  }


  /**
   * Checks if this bundle is attached to a fragment host.
   */
  boolean isAttached() {
    final BundleGeneration fix = current();
    return fix.fragment != null && fix.fragment.hasHosts();
  }


  /**
   * Returns the name of the bundle's fragment host. Returns null if this is not
   * a fragment.
   */
  String getFragmentHostName() {
    final BundleGeneration fix = current();
    if (fix.isFragment()) {
      return fix.fragment.hostName;
    } else {
      return null;
    }
  }


  // Lazy bundles in state STARTING must not be actiavted during shutdown
  boolean triggersActivationPkg(String pkg) {
    return Bundle.STOPPING != fwCtx.systemBundle.getState() && state == Bundle.STARTING
        && operation != ACTIVATING && current().isPkgActivationTrigger(pkg);
  }


  // Lazy bundles in state STARTING must not be actiavted during shutdown
  boolean triggersActivationCls(String name) {
    if (Bundle.STOPPING != fwCtx.systemBundle.getState() && state == Bundle.STARTING
        && operation != ACTIVATING) {
      String pkg = "";
      final int pos = name.lastIndexOf('.');
      if (pos != -1) {
        pkg = name.substring(0, pos);
      }
      return current().isPkgActivationTrigger(pkg);
    }
    return false;
  }


  /**
   *
   */
  BundleThread bundleThread() {
    synchronized (fwCtx.bundleThreads) {
      if (fwCtx.bundleThreads.isEmpty()) {
        bundleThread = secure.createBundleThread(fwCtx);
      } else {
        bundleThread = (BundleThread)fwCtx.bundleThreads.removeFirst();
      }
      return bundleThread;
    }
  }


  @SuppressWarnings("unchecked")
  <A> A adaptSecure(Class<A> type) {
    Object res = null;
    if (BundleRevision.class.equals(type)) {
      res = current().bundleRevision;
    } else if (BundleRevisions.class.equals(type)) {
      res = new BundleRevisionsImpl(generations);
    } else if (BundleWiring.class.equals(type)) {
      BundleRevision bundleRevision = current().bundleRevision;
      if (bundleRevision != null) {
        res = bundleRevision.getWiring();
      }
    } else if (fwCtx.startLevelController != null &&
	           BundleStartLevel.class.equals(type)) {
      res = fwCtx.startLevelController.bundleStartLevel(this);
    }
    return (A) res;
  }


  boolean usesBundleGeneration(BundleGeneration check) {
    return generations.contains(check);
  }


  boolean hasZombies() {
    return generations.size() > 1;
  }

  boolean isBundleThread(Thread t) {
    return bundleThread == t;
  }


  void resetBundleThread() {
    bundleThread = null;
  }


  //
  // Private methods
  //

  /**
   * Register all our import and export packages.
   *
   */
  private void doExportImport() {
    final BundleGeneration current = current();
    if (!current.isFragment()) {
      // fragments don't export anything themselves.
      current.bpkgs.registerPackages();
    }
  }


  /**
   * Remove a bundles all registered listeners, registered services and used
   * services.
   *
   */
  private void removeBundleResources() {
    fwCtx.listeners.removeAllListeners(bundleContext);
    final Set<ServiceRegistrationImpl<?>> srs = fwCtx.services.getRegisteredByBundle(this);
    for (final ServiceRegistrationImpl<?> serviceRegistrationImpl : srs) {
      try {
        serviceRegistrationImpl.unregister();
      } catch (final IllegalStateException ignore) {
        // Someone has unregistered the service after stop completed.
        // This should not occur, but we don't want get stuck in
        // an illegal state so we catch it.
      }
    }
    final Set<ServiceRegistrationImpl<?>> s = fwCtx.services.getUsedByBundle(this);
    for (final ServiceRegistrationImpl<?> sri : s) {
      sri.ungetService(this, false);
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
   * State is resolved, allowing bundle package access.
   * <p>
   * State is
   * <tt>Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING</tt>
   * </p>
   */
  boolean isResolved() {
    return (state & (RESOLVED | STARTING | ACTIVE | STOPPING)) != 0;
  }

}
