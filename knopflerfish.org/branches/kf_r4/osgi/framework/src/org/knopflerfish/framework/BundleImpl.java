/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

import org.osgi.framework.*;
import org.osgi.framework.AdminPermission;


/**
 * Implementation of the Bundle object.
 *
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 * @author Philippe Laporte
 */

class BundleImpl implements Bundle {

  /**
   * Framework for bundle.
   */
  final Framework framework;

  /**
   * Bundle identifier.
   */
  final long id;

  /**
   * Bundle location identifier.
   */
  final String location;

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
   * Classloader for bundle.
   */
  private BundleClassLoader classLoader = null;

  /**
   * Zombie classloaders for bundle.
   */
  private Map /* String -> BundleClassLoader */ oldClassLoaders = null;
    
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
   * List of all nativeLibs that can be used in this bundle.
   * 
   * pl: unused
   */
  //protected List nativeLibs = null;

  protected long lastModified;

  /**
   * Set to true of bundle.start() has been called but
   * current start levels was too low to actually start the bundle.
   */
  boolean bDelayedStart = false;

  private AdminPermission CLASS_ADMIN_PERM;
  private AdminPermission EXECUTE_ADMIN_PERM;
  private AdminPermission EXTENSIONLIFECYCLE_ADMIN_PERM;
  private AdminPermission LIFECYCLE_ADMIN_PERM;
  private AdminPermission METADATA_ADMIN_PERM;
  private AdminPermission RESOURCE_ADMIN_PERM;
  
  private void initPerms(){
	 if(System.getSecurityManager() != null){
		 CLASS_ADMIN_PERM = new AdminPermission(this, AdminPermission.CLASS);
		 EXECUTE_ADMIN_PERM = new AdminPermission(this, AdminPermission.EXECUTE);
		 EXTENSIONLIFECYCLE_ADMIN_PERM = new AdminPermission(this, AdminPermission.EXTENSIONLIFECYCLE);
		 LIFECYCLE_ADMIN_PERM = new AdminPermission(this, AdminPermission.LIFECYCLE);
		 METADATA_ADMIN_PERM = new AdminPermission(this, AdminPermission.METADATA);
		 RESOURCE_ADMIN_PERM = new AdminPermission(this, AdminPermission.RESOURCE);
	 }
  }
  
  private void checkClassAdminPerm(){
	  if(CLASS_ADMIN_PERM != null){
		  AccessController.checkPermission(CLASS_ADMIN_PERM);
	  }
  }
  
  void checkExecuteAdminPerm(){
	  if(EXECUTE_ADMIN_PERM != null){
		  AccessController.checkPermission(EXECUTE_ADMIN_PERM);
	  }
  }
  
  private void checkExtensionLifecycleAdminPerm(){
	  if(EXTENSIONLIFECYCLE_ADMIN_PERM != null){
		  AccessController.checkPermission(EXTENSIONLIFECYCLE_ADMIN_PERM);
	  }
  }
  
  void checkLifecycleAdminPerm(){
	  if(LIFECYCLE_ADMIN_PERM != null){
		  AccessController.checkPermission(LIFECYCLE_ADMIN_PERM);
	  }
  }
  
  void checkMetadataAdminPerm(){
	  if(METADATA_ADMIN_PERM != null){
		  AccessController.checkPermission(METADATA_ADMIN_PERM);
	  }
  }
  
  private void checkResourceAdminPerm(){
	  if(RESOURCE_ADMIN_PERM != null){
		  AccessController.checkPermission(RESOURCE_ADMIN_PERM);
	  }
  }
  
  /**
   * Construct a new Bundle empty.
   *
   * Only called for system bundle
   *
   * @param fw Framework for this bundle.
   */
  BundleImpl(Framework fw, long id, String loc) {
    this.framework = fw;
    this.id = id;
    this.location = loc;
    initPerms();
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
    id = ba.getBundleId();
    location = ba.getBundleLocation();
    archive = ba;

    state = INSTALLED;
    doExportImport();
    FileTree dataRoot = fw.getDataStorage();
    if (dataRoot != null) {
      bundleDir = new FileTree(dataRoot, Long.toString(id));
    }
    /* permissions are dynamic! 
    ProtectionDomain pd = null;
    if (fw.permissions != null) {
      try {
        URLStreamHandler handler 
          = bpkgs.bundle.framework.bundleURLStreamhandler;
        
	URL bundleUrl = new URL(BundleURLStreamHandler.PROTOCOL, 
                                Long.toString(id), 
                                -1,
                                "",
                                handler);
	 
	PermissionCollection pc = fw.permissions.getPermissionCollection(this);
	pd = new ProtectionDomain(new CodeSource(bundleUrl, null), pc);
      } catch (MalformedURLException e) { 
	e.printStackTrace();
      }
    }
    //protectionDomain = pd;
    */
    int oldStartLevel = archive.getStartLevel();

    try {
      if(framework.startLevelService == null) {
	archive.setStartLevel(0);
      } else {
	if(oldStartLevel == -1) {
	  archive.setStartLevel(framework.startLevelService.getInitialBundleStartLevel());
	} else {
	} 
      }
    } catch (Exception e) {
      Debug.println("Failed to set start level on #" + getBundleId() + ": " + e);
    }
    initPerms();
    lastModified = archive.getLastModified();
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
	checkExecuteAdminPerm();

    int updState = getUpdatedState();
    
    setPersistent(true);
    
    if(framework.startLevelService != null) {
      if(getStartLevel() > framework.startLevelService.getStartLevel()) {
    	  bDelayedStart = true;
    	  return;
      }
    }


    switch (updState) {
    case INSTALLED:
      throw new BundleException("Failed, " + bpkgs.getResolveFailReason());
    case RESOLVED:
      if (framework.active) {
	state = STARTING;
	framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTING, this));
	bundleContext = new BundleContextImpl(this);
	try {
	  AccessController.doPrivileged(new PrivilegedExceptionAction() {
	      public Object run() throws BundleException {
		final String ba = archive.getAttribute(Constants.BUNDLE_ACTIVATOR);
		boolean bStarted = false;

		ClassLoader oldLoader = null;

		if(Framework.SETCONTEXTCLASSLOADER) {
		  oldLoader = Thread.currentThread().getContextClassLoader();
		}

		try {

		  // If SETCONTEXTCLASSLOADER, set the thread's context
		  // class loader to the bundle class loader. This
		  // is useful for debugging external libs using
		  // the context class loader.
		  if(Framework.SETCONTEXTCLASSLOADER) {
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
		  
		  if(!bStarted) {
		    // Even bundles without an activator are marked as 
		    // ACTIVE.
		    
		    // Should we possible log an information message to 
		    // make sure users are aware of the missing activator?
		  }
		  
		  state = ACTIVE;
		  startOnLaunch(true);

		} catch (Throwable t) {
		  throw new BundleException("BundleActivator start failed", t);
		} finally {
		  if(Framework.SETCONTEXTCLASSLOADER) {
		    Thread.currentThread().setContextClassLoader(oldLoader);
		  }
		}
		return null;
	      }
	    });
	} catch (PrivilegedActionException e) {
	  removeBundleResources();
	  bundleContext.invalidate();
	  bundleContext = null;
	  state = RESOLVED;
	  throw (BundleException) e.getException();
	}
	framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STARTED, this));
	break;
      } else {
	startOnLaunch(true);
      }
    case ACTIVE:
      return;
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
	checkExecuteAdminPerm();

    bDelayedStart = false;
    
    setPersistent(false);

    if(framework.startLevelService != null) {
      if(getStartLevel() <= framework.startLevelService.getStartLevel()) {
	if(state == ACTIVE) {
	  bDelayedStart = true;
	}
      }
    }

    switch (state) {
    case INSTALLED:
    case RESOLVED:
      // We don't want this bundle to start on launch after it has been
      // stopped. (Don't apply during shutdown
      if (allowSetStartOnLaunchFalse()) {
	AccessController.doPrivileged(new PrivilegedAction() {
	    public Object run() {
	      startOnLaunch(false);
	      return null;
	    }
	  });
      }
      break;
    case ACTIVE:
      state = STOPPING;
      framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));
      
      Throwable savedException =
	(Throwable) AccessController.doPrivileged(new PrivilegedAction() {
	  public Object run() {
	    Throwable res = null;
	    if (allowSetStartOnLaunchFalse()) {
	      startOnLaunch(false);
	    }
	    if (bactivator != null) {
	      try {
		bactivator.stop(bundleContext);
	      } catch (Throwable e) {
		res = e;
	      }
	      bactivator = null;
	    }

	    bundleContext.invalidate();
	    bundleContext = null;
	    removeBundleResources();
	    state = RESOLVED;
	    return res;
	  }
	});

      framework.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPED, this));

      if (savedException != null) {
	throw new BundleException("Bundle.stop: BundleActivator stop failed",
				  savedException);
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
      checkLifecycleAdminPerm();
      final boolean wasActive = state == ACTIVE;
      
      switch (getUpdatedState()) {
      case ACTIVE:
	stop();
	// Fall through   
      case RESOLVED:
    	  framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UNRESOLVED, this));
      case INSTALLED:  
	// Load new bundle
	try {
	  final BundleImpl thisBundle = this;
	  final int oldStartLevel = getStartLevel();
	  AccessController.doPrivileged(new PrivilegedExceptionAction() {
	      public Object run() throws BundleException {
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
		  newArchive = framework.storage.replaceBundleJar(archive, bin);
		  newArchive.setStartLevel(oldStartLevel);
		} catch (Exception e) {
		  if (newArchive != null) {
		    newArchive.purge();
		  }
		
		  if (wasActive) {
		    try {
		      start();
		    } catch (BundleException be) {
		      framework.listeners.frameworkError(thisBundle, be);
		    }
		  }
		  throw new BundleException("Failed to get update bundle", e);
		}

		// Remove this bundle's packages
		boolean allRemoved = bpkgs.unregisterPackages(false);

		// Loose old bundle if no exporting packages left
		if (classLoader != null) {
		  if (allRemoved) {
		    classLoader.purge();
		  } else {
		    saveZombiePackages();
		  }
		  classLoader = null;
		}


		// Activate new bundle
		state = INSTALLED;
		BundleArchive oldArchive = archive;
		archive = newArchive;
		doExportImport();

		// Purge old archive
		if (allRemoved) {
		  oldArchive.purge();
		}

		checkEE(newArchive);
      
		// Broadcast updated event
		framework.listeners.bundleChanged(new BundleEvent(BundleEvent.UPDATED, thisBundle));

		// Restart bundles previously stopped in the operation
		if (wasActive) {
		  try {
		    thisBundle.start();
		  } catch (BundleException be) {
		    framework.listeners.frameworkError(thisBundle, be);
		  }
		}
		return null;
	      }
	    });
	} catch (PrivilegedActionException e) {
	  throw (BundleException) e.getException();
	}
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
    //only when complete success
    modified();
  }


  void checkEE(BundleArchive ba) throws BundleException {
    String ee = ba.getAttribute(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
    if(ee != null) {
      if(Debug.packages) {
	Debug.println("bundle #" + ba.getBundleId() + " has EE=" + ee);
      }
      if(!framework.isValidEE(ee)) {
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
	checkLifecycleAdminPerm();

    try {
      archive.setStartLevel(-2); // Mark as uninstalled
    } catch (Exception ignored) {   }

    bDelayedStart = false;
    
    switch (state) {
    case ACTIVE:
      try {
	stop();
      } catch (BundleException be) {
	framework.listeners.frameworkError(this, be);
      }
      // Fall through
    case INSTALLED:
    case RESOLVED:
      framework.bundles.remove(location);
      if (bpkgs.unregisterPackages(false)) {
	if (classLoader != null) {
	  AccessController.doPrivileged(new PrivilegedAction() {
	      public Object run() {
		classLoader.purge();
		classLoader = null;
		return null;
	      }
	    });
	}
	archive.close();
      } else {
	saveZombiePackages();
	classLoader = null;
      }
      bpkgs = null;
      bactivator = null;
      if (bundleDir != null) {
	AccessController.doPrivileged(new PrivilegedAction() {
	  public Object run() {
	    if(!bundleDir.delete()) {
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
	    return null;
	  }
	});
      }
    
      // id, location and headers survices after uninstall.
      state = UNINSTALLED;
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
    //only when complete success
    modified();
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
    checkMetadataAdminPerm();
    return location;
  }


  /**
   * Get services that this bundle has registrated.
   *
   * @see org.osgi.framework.Bundle#getRegisteredServices
   */
  public ServiceReference[] getRegisteredServices() {
    Set sr = framework.services.getRegisteredByBundle(this);
    if (framework.permissions != null) {
      filterGetServicePermission(sr);
    }
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
    if (framework.permissions != null) {
      filterGetServicePermission(sr);
    }
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
	if(state == UNINSTALLED){
		throw new IllegalStateException("bundle is uninstalled");
    }  
    if (permission instanceof Permission) {
    	if (framework.permissions != null) {
            //get the current status from permission admin
    		PermissionCollection pc = framework.permissions.getPermissionCollection(this);
    	    return pc != null ? pc.implies((Permission)permission) : false;
    	} 
    	else {
    		return true;
    	}
    } 
    else {
      return false;
    }
  }


  /**
   * @see org.osgi.framework.Bundle#getResource(String name)
   */
  public URL getResource(String name) {
    if (state == UNINSTALLED) {
      throw new IllegalStateException("Bundle is in UNINSTALLED state");
    }
    //  TODO check not fragment
    try {
    	checkResourceAdminPerm();
    	BundleClassLoader cl = (BundleClassLoader)getClassLoader();
    	if (cl != null) {
    		return cl.getBundleResource(name);
    	}
    } catch (SecurityException e) {
    	//return null; done below
    }
    return null;
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
			  if (state == INSTALLED && bpkgs.resolvePackages()) {
				  state = RESOLVED;
				  framework.listeners.bundleChanged(new BundleEvent(BundleEvent.RESOLVED, this));
			  }
		  }
	  }
	  return state;
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
   */
  ClassLoader getClassLoader() {
    if (classLoader == null) {
      synchronized (this) {
	if (classLoader == null) {
	  classLoader = (BundleClassLoader)
	    AccessController.doPrivileged(new PrivilegedAction() {
		public Object run() {
		  return new BundleClassLoader(bpkgs, archive);
		}
	      });
	}
      }
    }
    return classLoader;
  }


  /**
   * Set state to INSTALLED and throw away our classloader.
   * Reset all package registration.
   *
   */
  synchronized void setStateInstalled() {
    if (classLoader != null) {
      classLoader.close();
      classLoader = null;
    }
    bpkgs.unregisterPackages(true);
    bpkgs.registerPackages();
    state = INSTALLED;
  }


  /**
   * Get a Classloader object that we export from our bundle.
   *
   * @param pkg Name of java package to get from.
   * @return BundleClassLoader object for specified pkg, null if no classloader.
   */
  BundleClassLoader getExporterClassLoader(String pkg) {
      BundleClassLoader cl;
      if (oldClassLoaders != null) {
    	  cl = (BundleClassLoader)oldClassLoaders.get(pkg);
    	  if (cl != null) {
    		  return cl;
    	  }
      }
      return (BundleClassLoader)getClassLoader();
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
    }
    oldClassLoaders = null;
  }


  /**
   * Get bundle archive.
   *
   * @return BundleArchive object.
   */
  BundleArchive getBundleArchive() {
    return archive;
  }


  /**
   * Get exported packages.
   *
   * @return Iterator of all exported packages as PkgEntry.
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
   * @return Iterator of all imported packages as PkgEntry.
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

  //
  // Private methods
  //

  /**
   * Save the start on launch flag to the persistent bundle storage.
   *
   * @param value Boolean state for start on launch flag.
   */
  private void startOnLaunch(boolean value) { 
    try {
      archive.setStartOnLaunchFlag(value);
    } catch (IOException e) {
      framework.listeners.frameworkError(this, e);
    }
  }
  
  void setPersistent(final boolean value) { 
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
	  public Object run() throws Exception {
	    archive.setPersistent(value);
	    return null;
	  }
	});
    } catch (Exception e) {
      framework.listeners.frameworkError(this, e);
    }
  }
  
  

  /**
   * Filter out all services that we don't have permission to get.
   *
   * @param srs Set of ServiceRegistrationImpls to check.
   */
  private void filterGetServicePermission(Set srs) { 
    AccessControlContext acc = AccessController.getContext();
    for (Iterator i = srs.iterator(); i.hasNext();) {
      ServiceRegistrationImpl sr = (ServiceRegistrationImpl)i.next();;
      String[] classes = (String[])sr.properties.get(Constants.OBJECTCLASS);
      boolean perm = false;
      for (int n = 0; n < classes.length; n++) {
	try { 
	  acc.checkPermission(new ServicePermission(classes[n], ServicePermission.GET));
	  perm = true;
	  break;
	} catch (AccessControlException ignore) { }
      }
      if (!perm) {
	i.remove();
      }
    }
  }


  /**
   * Look at our manifest and register all our import and export
   * packages.
   *
   */
  private void doExportImport() {
    bpkgs = new BundlePackages(this,
			       archive.getAttribute(Constants.EXPORT_PACKAGE),
			       archive.getAttribute(Constants.IMPORT_PACKAGE),
			       archive.getAttribute(Constants.DYNAMICIMPORT_PACKAGE));
    bpkgs.registerPackages();
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
    Iterator i = bpkgs.getExports();
    while (i.hasNext()) {
      PkgEntry pkg = (PkgEntry)i.next();
      oldClassLoaders.put(pkg.name, getClassLoader());
    }
  }


 // Start level related

  boolean isPersistent() {
    boolean b = archive.isPersistent();
    
    // yup.
    b |= bDelayedStart;

    return b;
  }
 
  int getStartLevel() {
    if(archive != null) {
      return archive.getStartLevel();
    } else {
      return 0;
    }
  }

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

    sb.append("]");

    return sb.toString();
  }

public Enumeration findEntries(String path, String filePattern, boolean recurse) { 
	try{
		checkResourceAdminPerm();
	}
	catch(AccessControlException e){
		return null;
	}
	if(this.state == INSTALLED){
		getUpdatedState(); 
	}
    //TODO
	//look in fragments etc
	return null;
}

public URL getEntry(String name) {
	try{
		checkResourceAdminPerm();
	}
	catch(AccessControlException e){
		return null;
	}
	if(state == UNINSTALLED){
		throw new IllegalStateException("state is uninstalled");
	}
	//not REALLY using class loader, just don't want to duplicate functionality
	BundleClassLoader cl = (BundleClassLoader) getClassLoader();
    if (cl != null) {
    	Enumeration e =  cl.findBundleResources(name);
    	if (e != null && e.hasMoreElements()) {
    		return (URL)e.nextElement();
    	}
    }
	return null;
}

//TODO this is not complete
public Enumeration getEntryPaths(String path) {
	try{
		checkResourceAdminPerm();
	}
	catch(AccessControlException e){
		return null;
	}
	if(state == UNINSTALLED){
		throw new IllegalStateException("state is uninstalled");
	}
    //not REALLY using class loader
	BundleClassLoader cl = (BundleClassLoader) getClassLoader();
    if (cl != null) {
    	return cl.findBundleResourcesPath(path);
    }
	return null;
}

/**
 * @see org.osgi.framework.Bundle#getHeaders(String locale)
 */
public Dictionary getHeaders(String locale) {
	//TODO look in fragments
	checkMetadataAdminPerm();
	return archive.getAttributes(locale, state);
}

private void modified(){
	lastModified = System.currentTimeMillis();
	//make it persistent
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
	if (state == UNINSTALLED) {
		throw new IllegalStateException("Bundle is in UNINSTALLED state");
	}
    //	TODO check not fragment
	try {
		checkResourceAdminPerm();
	    BundleClassLoader cl = (BundleClassLoader)getClassLoader();
	    if (cl != null) {
	    	return cl.getBundleResources(name);
	    }
	} catch (SecurityException e) { 
		//return null; done below
	}
	return null;
}

/**
 * @see org.osgi.framework.Bundle#getSymbolicName()
 */
public String getSymbolicName() {
	if(id != 0){
		return archive.getAttribute(Constants.BUNDLE_SYMBOLICNAME);
	}
	else{
		return Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
	}
	
}

public Class loadClass(final String name) throws ClassNotFoundException {
	try{
		checkClassAdminPerm();
	}
	catch(AccessControlException e){
		throw new ClassNotFoundException(e.getMessage(), e);
	}
	if(this.state == UNINSTALLED){
		throw new IllegalStateException("state is uninstalled");
	}
	if(this.state == INSTALLED){
		if(getUpdatedState() != RESOLVED){
		    framework.listeners.frameworkError(this, new BundleException("Unable to resolve bundle: " + bpkgs.getResolveFailReason()));
			throw new ClassNotFoundException("Unable to resolve bundle");
		}
	}
	//TODO check not fragment
	/*Class clazz = null;
	try{
		clazz = (Class) AccessController.doPrivileged(new PrivilegedExceptionAction(){
	
			public Object run() throws ClassNotFoundException{
	*/			
				    BundleClassLoader cl = (BundleClassLoader) getClassLoader();
				
					if (cl != null) {
						return cl.loadClass(name);
					}
			        return null;    
	/*		}
	}	
	);
	}
	catch(PrivilegedActionException e){
		Exception ee;
		if((ee = e.getException()) instanceof ClassNotFoundException){
			throw (ClassNotFoundException) ee;
		}
	}
	return clazz;*/
}//method 
}//class
