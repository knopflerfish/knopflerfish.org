/*
 * Copyright (c) 2004-2010, KNOPFLERFISH project
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
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.log.LogService;

import org.knopflerfish.service.dirdeployer.*;

/**
 * Implementation of <tt>DirDeployerService</tt> which
 * scans a set of directories regularly on a thread.
 *
 */
class DirDeployerImpl implements DirDeployerService {

  /**
   * The prefix used for the bundle location of all bundles installed
   * by the dir deployer. Must not be "file:" or some other known
   * URL-protocol since it is used to identify bundles installed by
   * the dir deployer so that bundles removed from any of the scanned
   * directories while the framework is stopped can be detected and
   * un-installed on the next start-up.
   */
  final static String LOCATION_PREFIX = "dirdeployer:";


  // File -> DeployedFile
  final HashMap  deployedFiles = new HashMap();

  Thread         runner = null;    // scan thread
  boolean        bRun   = false;   // flag for stopping scan thread
  // List with bundles that has been updated, if non-empty a package
  // admin refresh call is needed.
  final List updatedBundles = new ArrayList();

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

          if(config.uninstallOnStop) {
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
    scanForStrayBundles();
    refreshPackages();
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
   * New files are installed (and marked for delayed start if they are
   * not fragment bundles). Files newer than a previously deployed
   * bundle are updated.
   * </p>
   *
   * <p>
   * Files that have the same location as an already installed bundle
   * is not installed again, instead, the installed bundle is re-used
   * in the created DeployedFile instance.
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
              DeployedFile dp = (DeployedFile) deployedFiles.get(f);
              if(dp != null) {
                dp.updateIfNeeded();
              } else {
                dp = new DeployedFile(f);

                deployedFiles.put(f, dp);
                dp.installIfNeeded();
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
      // check, uninstall and copy to removed vector as
      // necessary
      for (Iterator it = deployedFiles.entrySet().iterator(); it.hasNext();) {
        final Map.Entry entry = (Map.Entry) it.next();
        final File f = (File) entry.getKey();
        if(!f.exists() || !isInScannedDirs(f)) {
          try {
            DeployedFile dp = (DeployedFile) entry.getValue();
            log("uninstalling since file is removed "+dp);
            dp.uninstall();
            it.remove();
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }


  /**
   * Check if any of the currently installed bundles that has been
   * installed by the dirdeployer does not correspond to a file in the
   * scanned directories. If so uninstall the bundle.
   */
  void scanForStrayBundles() {
    final Bundle[] bl = Activator.bc.getBundles();
    for(int i = 0; i < bl.length; i++) {
      final Bundle bundle = bl[i];
      final String location = bundle.getLocation();
      if(location.startsWith(LOCATION_PREFIX)) {
        try {
          final String filePath = location.substring(LOCATION_PREFIX.length());
          final File f = new File(filePath);
          final DeployedFile dp = (DeployedFile) deployedFiles.get(f);
          if (null==dp) {
            // Found a stray bundle; uninstall it.
            log("uninstalling stray bundle not in deploy dirs, "+f);
            bundle.uninstall();
          }
        } catch (Exception ex) {
          log("Exception during scan for stray bundles: bundle "
              +bundle.getBundleId() +" with location '" +location +"'; " +ex,
              ex);
        }
      }
    }
  }


  /**
   * Call <tt>PackageAdmin.refreshPackages()</tt> when needed.
   */
  void refreshPackages()
  {
    if (0<updatedBundles.size()) {
      synchronized(updatedBundles) {
        log("Bundles has been updated; refreshing "+updatedBundles);

        final PackageAdmin pkgAdmin = (PackageAdmin)
          Activator.packageAdminTracker.getService();

        final Bundle[] bundles = (Bundle[])
          updatedBundles.toArray(new Bundle[updatedBundles.size()]);
        pkgAdmin.refreshPackages(bundles);
        // Refresh request sent, clear before reliasing monitor.
        updatedBundles.clear();
        // Wait for the package refresh to finish.
        try {
          updatedBundles.wait();
          log("refresh completed");
        } catch (InterruptedException ie) {
          log("refresh interrupted", ie);
        }
      }
    }
  }
  // Called by the activator when a PACKAGES_REFRESHED event is recieved.
  void refreshPackagesDone()
  {
    synchronized(updatedBundles) {
      updatedBundles.notifyAll();
    }
  }

  /**
   * Start bundles marked that shall be started but is not yet.
   */
  void startDelayed() {
    for (Iterator it = deployedFiles.values().iterator(); it.hasNext();) {
      final DeployedFile dp = (DeployedFile) it.next();
      try {
        dp.start();
      } catch (Exception ex) {
        log("Failed to start "+dp +"; "+ex, ex);
      }
    }
  }

  /**
   * Uninstall all deployed bundles and clear deploy map
   */
  void uninstallAll() {
    synchronized(deployedFiles) {
      for (Iterator it = deployedFiles.values().iterator(); it.hasNext();) {
        final DeployedFile dp = (DeployedFile) it.next();
        try {
          log("uninstall "+dp);
          dp.uninstall();
        } catch (Exception ex) {
          log("Failed to uinstall "+dp +"; "+ex, ex);
        }
        it.remove();
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
    final String  location;
    final File    file;
    Bundle  bundle;
    long    lastUpdate     = -1;
    boolean start = false;

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
      location  = LOCATION_PREFIX + file.getPath();
    }

    /**
     * Check if deployed file is installed, if not install it.
     *
     * <p>
     * If the location for this DeployFile already is installed
     * in the framework, re-use the installed Bundle instance,
     * otherwise install using <tt>BundleContext.installBundle</tt>
     */
    public void installIfNeeded() throws BundleException {
      final Bundle[] bl = Activator.bc.getBundles();
      for(int i = 0; i < bl.length; i++) {
        if(location.equals(bl[i].getLocation())) {
          bundle = bl[i];
          log("already installed " + this);
        }
      }
      if(bundle == null) {
        InputStream is = null;
        try {
          is = new FileInputStream(file);
          bundle = Activator.bc.installBundle(location, is);

          // Set bundle start level if possible
          StartLevel sl = (StartLevel)
            Activator.startLevelTracker.getService();
          sl.setBundleStartLevel(bundle, config.startLevel);

          log("installed " + this);
        } catch (Exception ioe) {
          log("Failed to install " + this +"; "+ioe, ioe);
        } finally {
          if (null!=is) try { is.close(); } catch (IOException ioe) {}
        }
      }

      // Check if this bundle shall be started or not.
      if (null!=bundle) {
        start = null==bundle.getHeaders().get(Constants.FRAGMENT_HOST);
      }
    }

    public void start() throws BundleException {
      if (null!=bundle) {
        final int state = bundle.getState();
        if (start && (Bundle.INSTALLED==state || Bundle.RESOLVED==state)) {
          log("starting " + this);
          bundle.start();
        }
      }
    }

    public void updateIfNeeded() throws BundleException {
      if (needUpdate()) {
        log("update " + this);
        InputStream is = null;
        try {
          is = new FileInputStream(file);
          bundle.update(is);
          synchronized(updatedBundles) {
            updatedBundles.add(bundle);
          }

          // Check if the updated bundle shall be started or not.
          start = null==bundle.getHeaders().get(Constants.FRAGMENT_HOST);
        } catch (IOException ioe) {
        } finally {
          if (null!=is) try { is.close(); } catch (IOException ioe) {}
        }
      }
    }

    public void uninstall() throws BundleException {
      if (bundle!=null) {
        if (Bundle.UNINSTALLED!=bundle.getState()) {
          log("uninstall " + this);
          bundle.uninstall();
        }
        bundle = null;
      }
    }

    /**
     * A deployed bundle should be updated if the bundle file
     * is newer than tha latest update time.
     */
    public boolean needUpdate() {
      return file.lastModified() > (bundle==null ? -1
                                    : bundle.getLastModified());
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
