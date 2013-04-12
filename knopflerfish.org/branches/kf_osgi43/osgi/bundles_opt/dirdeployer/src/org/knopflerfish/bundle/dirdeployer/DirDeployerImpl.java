/*
 * Copyright (c) 2004-2013, KNOPFLERFISH project
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.dirdeployer.DirDeployerService;

/**
 * Implementation of <tt>DirDeployerService</tt> which scans a set of
 * directories regularly on a thread.
 *
 */
class DirDeployerImpl
  implements DirDeployerService, FrameworkListener
{

  /**
   * The prefix used for the bundle location of all bundles installed by the dir
   * deployer. Must not be "file:" or some other known URL-protocol since it is
   * used to identify bundles installed by the dir deployer so that bundles
   * removed from any of the scanned directories while the framework is stopped
   * can be detected and un-installed on the next start-up.
   */
  final static String LOCATION_PREFIX = "dirdeployer:";

  final HashMap<File, DeployedFile> deployedFiles =
    new HashMap<File, DeployedFile>();

  Thread runner = null; // scan thread
  boolean bRun = false; // flag for stopping scan thread

  // List with bundles that has been updated, if non-empty a package
  // admin refresh call is needed.
  final List<Bundle> updatedBundles = new ArrayList<Bundle>();

  ServiceTracker<LogService,LogService> logTracker;
  Config config;

  public DirDeployerImpl()
  {

    // create and register the configuration class
    config = new Config();

    logTracker =
      new ServiceTracker<LogService, LogService>(Activator.bc,
                                                 LogService.class, null);
    logTracker.open();
  }

  /**
   * Start the scanner thread if not already started. Also register the config
   * object.
   */
  void start()
  {
    config.register();

    if (runner != null) {
      return;
    }

    runner = new Thread("Directory deployer") {
      @Override
      public void run()
      {
        log("started scan");
        while (bRun) {
          try {
            doScan();
            Thread.sleep(Math.max(100, config.interval));
          } catch (final Exception e) {
            log("scan failed", e);
          }
        }
        log("stopped scan");

        if (config.uninstallOnStop) {
          uninstallAll();
        }
      }
    };
    bRun = true;
    runner.start();
  }

  /**
   * Stop the scanner thread if not already stopped. Also unregister the config
   * object.
   */
  void stop()
  {
    config.unregister();

    if (runner == null) {
      return;
    }

    try {
      bRun = false;
      runner.join(config.interval * 2);
    } catch (final Exception ignored) {
    }
    runner = null;
  }

  /**
   * Scan for new, updated and lost files.
   */
  void doScan()
  {

    synchronized (config) {
      for (final File dir : config.dirs) {
        scanForNewFiles(dir);
      }
    }

    scanForLostFiles();
    scanForStrayBundles();
    refreshBundles();
    startDelayed();
  }

  /**
   * Scan a directory for new/updated files.
   *
   * <p>
   * Check if any new files have appeared or if any already deployed files has
   * been replaced with newer files.
   * </p>
   *
   * <p>
   * New files are installed (and marked for delayed start if they are not
   * fragment bundles). Files newer than a previously deployed bundle are
   * updated.
   * </p>
   *
   * <p>
   * Files that have the same location as an already installed bundle is not
   * installed again, instead, the installed bundle is re-used in the created
   * DeployedFile instance.
   * </p>
   */
  void scanForNewFiles(File dir)
  {
    synchronized (deployedFiles) {
      // log("scanForNewFiles " + dir);

      if (dir != null && dir.exists() && dir.isDirectory()) {
        final String[] files = dir.list();

        for (final String file : files) {
          try {
            final File f = new File(dir, file);
            if (isBundleFile(f)) {
              DeployedFile dp = deployedFiles.get(f);
              if (dp != null) {
                dp.updateIfNeeded();
              } else {
                dp = new DeployedFile(f);

                deployedFiles.put(f, dp);
                dp.installIfNeeded();
              }
            }
          } catch (final Exception e) {
            log("scan failed", e);
          }
        }
      }
    }
  }

  /**
   * Check if any files has been removed from any of the scanned dirs, If so,
   * uninstall them and remove from deployed map.
   */
  void scanForLostFiles()
  {
    synchronized (deployedFiles) {
      // check, uninstall and copy to removed vector as
      // necessary
      for (final Iterator<Entry<File, DeployedFile>> it =
        deployedFiles.entrySet().iterator(); it.hasNext();) {
        final Entry<File, DeployedFile> entry = it.next();
        final File f = entry.getKey();
        if (!f.exists() || !isInScannedDirs(f)) {
          try {
            final DeployedFile dp = entry.getValue();
            log("uninstalling since file is removed " + dp);
            dp.uninstall();
            it.remove();
          } catch (final Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Check if any of the currently installed bundles that has been installed by
   * the dirdeployer does not correspond to a file in the scanned directories.
   * If so uninstall the bundle.
   */
  void scanForStrayBundles()
  {
    final Bundle[] bl = Activator.bc.getBundles();
    for (final Bundle bundle : bl) {
      final String location = bundle.getLocation();
      if (location.startsWith(LOCATION_PREFIX)) {
        try {
          final String filePath = location.substring(LOCATION_PREFIX.length());
          final File f = new File(filePath);
          final DeployedFile dp = deployedFiles.get(f);
          if (null == dp) {
            // Found a stray bundle; uninstall it.
            log("uninstalling stray bundle not in deploy dirs, " + f);
            bundle.uninstall();
          }
        } catch (final Exception ex) {
          log("Exception during scan for stray bundles: bundle "
                  + bundle.getBundleId() + " with location '" + location
                  + "'; " + ex, ex);
        }
      }
    }
  }

  /**
   * do a refresh bundles when needed.
   */
  void refreshBundles()
  {
    if (0 < updatedBundles.size()) {
      synchronized (updatedBundles) {
        log("Bundles has been updated; refreshing " + updatedBundles);

        final FrameworkWiring fw =
          Activator.bc.getBundle(0).adapt(FrameworkWiring.class);
        fw.refreshBundles(updatedBundles, this);
        // Refresh request sent, clear before releasing monitor.
        updatedBundles.clear();
        // Wait for the package refresh to finish.
        try {
          updatedBundles.wait();
          log("refresh completed");
        } catch (final InterruptedException ie) {
          log("refresh interrupted", ie);
        }
      }
    }
  }

  // Called by the framework when a the refresh bundles operation is done.
  public void frameworkEvent(FrameworkEvent event)
  {
    if (FrameworkEvent.PACKAGES_REFRESHED == event.getType()) {
      synchronized (updatedBundles) {
        updatedBundles.notifyAll();
      }
    }
  }

  /**
   * Start bundles marked that shall be started but is not yet.
   */
  void startDelayed()
  {
    for (final Object element : deployedFiles.values()) {
      final DeployedFile dp = (DeployedFile) element;
      try {
        dp.start();
      } catch (final Exception ex) {
        log("Failed to start " + dp + "; " + ex, ex);
      }
    }
  }

  /**
   * Uninstall all deployed bundles and clear deploy map
   */
  void uninstallAll()
  {
    synchronized (deployedFiles) {
      for (final Iterator<DeployedFile> it = deployedFiles.values().iterator(); it
          .hasNext();) {
        final DeployedFile dp = it.next();
        try {
          log("uninstall " + dp);
          dp.uninstall();
        } catch (final Exception ex) {
          log("Failed to uinstall " + dp + "; " + ex, ex);
        }
        it.remove();
      }
      deployedFiles.clear();
    }
  }

  /**
   * Same as <tt>log(msg, null)</tt>
   */
  void log(String s)
  {
    log(s, null);
  }

  /**
   * Log msg to log service if available and to stdout if not available.
   */
  void log(String msg, Throwable t)
  {
    final int level = t == null ? LogService.LOG_INFO : LogService.LOG_WARNING;

    final LogService log = logTracker.getService();
    if (log == null) {
      System.out.println("[dirdeployer " + level + "] " + msg);
      if (t != null) {
        t.printStackTrace();
      }
    } else {
      log.log(level, msg, t);
    }
  }

  /**
   * Check if a file is in one of the scanned dirs
   */
  boolean isInScannedDirs(File f)
  {
    synchronized (config) {
      for (final File dir : config.dirs) {
        if (dir.equals(new File(f.getParent()))) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Check if a file seems to be a bundle jar file.
   */
  static boolean isBundleFile(File f)
  {
    return f.toString().toLowerCase().endsWith(".jar");
  }

  /**
   * Utility class to store info about a deployed bundle.
   */
  class DeployedFile
  {
    final String location;
    final File file;
    Bundle bundle;
    long lastUpdate = -1;
    boolean start = false;

    /**
     * Create a deployedfile instance from a specified file.
     *
     * @throws RuntimeException
     *           if the specified does not exists
     */
    public DeployedFile(File f)
    {
      if (!f.exists()) {
        throw new RuntimeException("No file " + f);
      }
      this.file = f;

      // location URL to be used for for installing bundle
      location = LOCATION_PREFIX + file.getPath();
    }

    /**
     * Check if deployed file is installed, if not install it.
     *
     * <p>
     * If the location for this DeployFile already is installed in the
     * framework, re-use the installed Bundle instance, otherwise install using
     * <tt>BundleContext.installBundle</tt>
     */
    public void installIfNeeded()
        throws BundleException
    {
      final Bundle[] bl = Activator.bc.getBundles();
      for (final Bundle element : bl) {
        if (location.equals(element.getLocation())) {
          bundle = element;
          log("already installed " + this);
        }
      }
      if (bundle == null) {
        InputStream is = null;
        try {
          is = new FileInputStream(file);
          bundle = Activator.bc.installBundle(location, is);

          // Set bundle start level if possible
          final BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
          if (bsl != null) {
            bsl.setStartLevel(config.startLevel);
          }

          log("installed " + this);
        } catch (final Exception ioe) {
          log("Failed to install " + this + "; " + ioe, ioe);
        } finally {
          if (null != is) {
            try {
              is.close();
            } catch (final IOException ioe) {
            }
          }
        }
      }

      // Check if this bundle shall be started or not.
      if (null != bundle) {
        start =
          bundle.adapt(BundleRevision.class).getTypes() != BundleRevision.TYPE_FRAGMENT;
      }
    }

    public void start()
        throws BundleException
    {
      if (null != bundle) {
        final int state = bundle.getState();
        if (start && (Bundle.INSTALLED == state || Bundle.RESOLVED == state)) {
          log("starting " + this);
          bundle.start();
        }
      }
    }

    public void updateIfNeeded()
        throws BundleException
    {
      if (needUpdate()) {
        log("update " + this);
        InputStream is = null;
        try {
          is = new FileInputStream(file);
          bundle.update(is);
          synchronized (updatedBundles) {
            updatedBundles.add(bundle);
          }

          // Check if the updated bundle shall be started or not.
          start = null == bundle.getHeaders().get(Constants.FRAGMENT_HOST);
        } catch (final IOException ioe) {
        } finally {
          if (null != is) {
            try {
              is.close();
            } catch (final IOException ioe) {
            }
          }
        }
      }
    }

    public void uninstall()
        throws BundleException
    {
      if (bundle != null) {
        if (Bundle.UNINSTALLED != bundle.getState()) {
          log("uninstall " + this);
          bundle.uninstall();
        }
        bundle = null;
      }
    }

    /**
     * A deployed bundle should be updated if the bundle file is newer than tha
     * latest update time.
     */
    public boolean needUpdate()
    {
      return file.lastModified() > (bundle == null ? -1 : bundle
          .getLastModified());
    }

    @Override
    public String toString()
    {
      return "DeployedFile[" + "location=" + location + ", bundle=" + bundle
             + "]";
    }
  }
}
