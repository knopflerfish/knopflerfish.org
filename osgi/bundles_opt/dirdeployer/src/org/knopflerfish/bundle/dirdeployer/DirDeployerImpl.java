/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.dirdeployer;

import java.util.*;
import java.io.*;
import java.net.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.startlevel.*;
import org.osgi.service.log.LogService;

import org.knopflerfish.service.dirdeployer.*;

/**
 * Implementatation of <tt>DirDeployerService</tt> which
 * scans a set of directories regularely on a thread.
 *
 */
class DirDeployerImpl implements DirDeployerService {

  // File -> DeployedFile
  Hashtable      deployedFiles = new Hashtable();

  Thread         runner = null;    // scan thread
  boolean        bRun   = false;   // flag for stopping scan thread

  
  ServiceTracker logTracker;
  Config         config;
  
  public DirDeployerImpl() {

    // create and register the configuration class
    config = new Config();

    logTracker = new ServiceTracker(Activator.bc,
				    LogService.class.getName(),
				    null);
    logTracker.open();
  }

  /**
   * Start the scanner thread if not already started. Also register
   * the config object.
   */
  void start() {
    config.register();

    if(runner != null) {
      return;
    }
    
    runner = new Thread("Directory deployer") {
	public void run() {
	  log("started scan");
	  while(bRun) {
	    try {
	      synchronized(config) {
		long now = System.currentTimeMillis();
		doScan();
		long time = System.currentTimeMillis() - now;

		// make sure we don't spend all our time in scanning dirs
		if(time > config.interval) {
		  config.interval = Math.max(100, time * 2);
		  log("increased interval to " + config.interval);
		}
	      }

	      Thread.sleep(Math.max(100, config.interval));
	    } catch (Exception e) {
	      log("scan failed", e);
	    }
	  }
	  log("stopped scan");

	  if(config.bUninstallOnStop) {
	    uninstallAll();
	  }
	}
      };
    bRun = true;
    runner.start();
  }
  

  /**
   * Stop the scanner thread if not already stopped. Also unregister the
   * config object.
   */
  void stop() {
    config.unregister();
    
    if(runner == null) {
      return;
    }

    try {
      bRun = false;
      runner.join(config.interval * 2);
    } catch (Exception ignored) {
    }
    runner = null;
  }

  /**
   * Scan for new, updated and lost files.
   */
  void doScan() {

    synchronized(config) {
      for(int i = 0; i < config.dirs.length; i++) {
	scanForNewFiles(config.dirs[i]);
      }
    }
    
    scanForLostFiles();
    startDelayed();
  }

  /**
   * Scan a directory for new/updated files.
   *
   * <p>
   * Check if any new files have appeared or
   * if any already deployed files has been replaced with newer
   * files.
   * </p>
   *
   * <p>
   * New files are installed (and marked for delayed start if they have an
   * activator). Files newer than a previously deployed bundle are updated.
   * </p>
   *
   * <p>
   * Files that have the same location as an already installed
   * bundle is not installed again, instead, the installed bundle is
   * re-used in the created DeployedFile instance.
   * </p>
   */
  void scanForNewFiles(File dir) {
    synchronized(deployedFiles) {
      //      log("scanForNewFiles " + dir);

      if(dir != null && dir.exists() && dir.isDirectory()) {
	String[] files = dir.list();
	
	for(int i = 0; i < files.length; i++) {
	  try {
	    File f = new File(dir, files[i]);
	    if(isBundleFile(f)) {
	      DeployedFile dp = (DeployedFile)deployedFiles.get(f);
	      if(dp != null) {
		if(dp.needUpdate()) {
		  dp.update();
		}
	      } else {
		dp = new DeployedFile(f);
		
		deployedFiles.put(f, dp);
		dp.install(); // will install or re-use as appropiate
		
		// Mark to be started later on
		// this is to allow better package resolving
		if(null != dp.bundle.getHeaders().get("Bundle-Activator")) {
		  dp.bDelayedStart = true;
		}
	      }
	    }
	  } catch (Exception e) {
	    log("scan failed", e);
	  }
	}
      }
    }
  }
  
  
  /**
   * Check if any files has been removed from any of the scanned
   * dirs, If so, uninstall
   * them and remove from deployed map.
   */
  void scanForLostFiles() {
    synchronized(deployedFiles) {
      Vector removed = new Vector();
      
      // check, uninstall and copy to removed vector as
      // necessary
      for(Enumeration e = deployedFiles.keys(); e.hasMoreElements(); ) {
	File f = (File)e.nextElement();
	if(!f.exists() || !isInScannedDirs(f)) {
	  try {
	    DeployedFile dp = (DeployedFile)deployedFiles.get(f);
	    dp.uninstall();
	    removed.addElement(f);
	  } catch (Exception ex) {
	    ex.printStackTrace();
	  }
	}
      }
      
      // ...then remove from map
      for(int i = 0; i < removed.size(); i++) {
	File f = (File)removed.elementAt(i);
	deployedFiles.remove(f);
      }
    }
  }


  /**
   * Start bundles marked for delayed start.
   */
  void startDelayed() {
    for(Enumeration e = deployedFiles.keys(); e.hasMoreElements(); ) {
      File f = (File)e.nextElement();
      try {
	DeployedFile dp = (DeployedFile)deployedFiles.get(f);
	if(dp.bDelayedStart) {
	  dp.start();
	}
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
  }
  
  /**
   * Uninstall all deployed bundles and clear deploy map
   */
  void uninstallAll() {
    synchronized(deployedFiles) {
      for(Enumeration e = deployedFiles.keys(); e.hasMoreElements(); ) {
	File f = (File)e.nextElement();
	try {
	  DeployedFile dp = (DeployedFile)deployedFiles.get(f);
	  dp.uninstall();
	} catch (Exception ex) {
	  ex.printStackTrace();
	}
      }
      deployedFiles.clear();
    }
  }

  /**
   * Same as <tt>log(msg, null)</tt>
   */
  void log(String s) {
    log(s, null);
  }

  /**
   * Log msg to log service if available and to stdout if
   * not available.
   */
  void log(String msg, Throwable t) {
    int level = t == null ? LogService.LOG_INFO : LogService.LOG_WARNING;

    LogService log = (LogService)logTracker.getService();
    if(log == null) {
      System.out.println("[dirdeployer " + level + "] " + msg);
      if(t != null) {
	t.printStackTrace();
      }
    } else {
      log.log(level, msg, t);
    }
  }


  /**
   * Check if a file is in one of the scanned dirs
   */
  boolean isInScannedDirs(File f) {
    synchronized(config) {
      for(int i = 0; i < config.dirs.length; i++) {
	if(config.dirs[i].equals(new File(f.getParent()))) {
	  return true;
	}
      }
      return false;
    }
  }

  /**
   * Check if a file seems to be a bundle jar file.
   */
  static boolean isBundleFile(File f) {
    return f.toString().toLowerCase().endsWith(".jar");
  }

  /**
   * Utility class to store info about a deployed bundle.
   */
  class DeployedFile {
    String  location;
    File    file;
    Bundle  bundle;
    long    lastUpdate     = -1;
    boolean bDelayedStart = false;

    /**
     * Create a deployedfile instance from a specified file.
     *
     * @throws RuntimeException if the specified does not exists
     */
    public DeployedFile(File f) {
      if(!f.exists()) {
	throw new RuntimeException("No file " + f);
      }
      this.file = f;

      // location URL to be used for for installing bundle
      location  = "file:" + file.getAbsolutePath();
    }
    
    /**
     * Install into framework.
     *
     * <p>
     * If the location for this DeployFile already is installed
     * in the framework, re-use the installed Bundle instance, 
     * otherwise install using <tt>BundleContext.installBundle</tt>
     */
    public void install() throws BundleException {
      ServiceReference startLevelSR = null;
      try {
	Bundle[] bl = Activator.bc.getBundles();
	for(int i = 0; i < bl.length; i++) {
	  if(location.equals(bl[i].getLocation())) {
	    bundle = bl[i];
	    log("already installed " + this);
	  }
	}
	if(bundle == null) {
	  bundle = Activator.bc.installBundle(location);

	  // Set bundle start level if possible
	  try {
	    startLevelSR = Activator.bc.getServiceReference(StartLevel.class.getName());
	    StartLevel sl = (StartLevel)Activator.bc.getService(startLevelSR);
	    sl.setBundleStartLevel(bundle, config.startLevel);
	  } catch (Exception e) {
	    log("Failed to set start level for " + this, e);
	  }
	  log("installed " + this);
	}
	lastUpdate = System.currentTimeMillis();
      } finally {
	if(startLevelSR != null) {
	  Activator.bc.ungetService(startLevelSR);
	}
      }
    }
    
    public void start() throws BundleException {
      log("start " + this);
      bDelayedStart = false;  // delayed start is only valid once
      bundle.start();
    }
    
    public void update() throws BundleException {
      log("update " + this);
      bundle.update();
      lastUpdate = System.currentTimeMillis();
    }

    public void uninstall() throws BundleException {
      log("uninstall " + this);
      bundle.uninstall();
      bundle = null;
      lastUpdate = -1;
    }

    /**
     * A deployed bundle should be updated if the bundle file
     * is newer than tha latest update time.
     */
    public boolean needUpdate() {
      return file.lastModified() > lastUpdate;
    }

    
    public String toString() {
      return 
	"DeployedFile[" + 
	"location=" + location + 
	", bundle=" + bundle + 
	"]";
    }
  }
}

