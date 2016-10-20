/*
 * Copyright (c) 2004-2016, KNOPFLERFISH project
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
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
// import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.knopflerfish.service.dirdeployer.DeployedBundleControl;
import org.knopflerfish.service.dirdeployer.DirDeployerService;
import org.knopflerfish.service.log.LogRef;

/**
 * Implementation of <tt>DirDeployerService</tt> which scans a set of
 * directories for files to deploy regularly on a thread.
 */
class DirDeployerImpl
  implements DirDeployerService, FrameworkListener
{

  final HashMap<File, DeployedFile> deployedFiles =
    new HashMap<File, DeployedFile>();

  Thread runner = null; // scan thread
  boolean bRun = false; // flag for stopping scan thread

  // static ServiceTracker<LogService,LogService> logTracker;
  static ServiceTracker<ConfigurationAdmin,ConfigurationAdmin> caTracker;
  
  Config config;

  public DirDeployerImpl()
  {
//    logTracker =
//      new ServiceTracker<LogService, LogService>(Activator.bc,
//                                                 LogService.class, null);
//    logTracker.open();

    caTracker =
      new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
                                                                 Activator.bc,
                                                                 ConfigurationAdmin.class,
                                                                 null);
    caTracker.open();

    // create and register the configuration class
    config = new Config();
  }

  /**
   * Start the scanner thread if not already started. Also register the
   * {@code config} object.
   */
  void start()
  {
    config.register();
    DeployedBundle.loadState();
    DeployedCMData.loadState();

    if (runner != null) {
      return;
    }

    // TODO: Java SE 7 use java.nio.file.WatchService to avoid polling
    runner = new Thread("Directory deployer") {
      @Override
      public void run()
      {
        Activator.logger.info("start scan of " +Arrays.asList(config.dirs));
        while (bRun) {
          try {
            doScan();
            Thread.sleep(Math.max(100, config.interval));
          } catch (final InterruptedException ie) {
            Activator.logger.info("scaning interrupted");
          } catch (final Exception e) {
            Activator.logger.error("scan failed: " +e.getMessage(), e);
          }
        }
        Activator.logger.info("stopped scaning");

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
      runner.interrupt();
      runner.join(config.interval * 2);
    } catch (final Exception ignored) {
    }
    runner = null;
    DeployedBundle.clearState();
    DeployedCMData.clearState();
    caTracker.close();
    // logTracker.close();
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
    scanForStrayFiles();
    refreshUpdatedBundles();
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
   * New files are installed and marked for delayed start if that is applicable.
   * Files newer than a previously deployed files are updated.
   * </p>
   */
  void scanForNewFiles(final File dir)
  {
    synchronized (deployedFiles) {
      // log("scanForNewFiles " + dir);

      if (dir != null && dir.exists() && dir.isDirectory()) {
        final String[] files = dir.list();

        for (final String file : files) {
          try {
            final File f = new File(dir, file);
            if (Activator.logger.doDebug())
              Activator.logger.debug("Examining: " + f.getName());
            if (MarkerFile.isMarkerFile(f))
              continue;
            DeployedFile df = deployedFiles.get(f);
            if (df!=null) {
              if (Activator.logger.doDebug())
                Activator.logger.debug("We already have the file deployed: " + f.getName());
              if (config.useFileMarkers && df instanceof DeployedBundle) {
                DeployedBundle deployed = (DeployedBundle)df;
                Activator.logger.debug("Deployment State: " + deployed.getDeploymentState());
           
                switch (deployed.getDeploymentState()) {
                case STAGED: {
                  if (MarkerFile.isMarkedForDeployment(f)) {
                    if (Activator.logger.doDebug())
                      Activator.logger.debug("Staged, but marked for (re)deployment, attempting to install: " + f.getName());
                    df.installIfNeeded();
                  }
                  break;
                }
                case DEPLOYED:
                  if (!MarkerFile.isMarkedAsDeployed(f)) {
                    deployed.uninstall();
                    deployedFiles.remove(df.getFile());
                  }
                }
              }
              else {
                df.updateIfNeeded();
              }
            } else {
              if (DeployedBundle.isBundleFile(f)) { 
                if (Activator.logger.doDebug())
                Activator.logger.debug("New deployment: " + f.getName());
                df = new DeployedBundle(config, f);
                // Config.useFileMarkers && MarkerFile.isMarkedForDeployment(f)) {
              } else if (DeployedCMData.isCMDataFile(f)) {
                df = new DeployedCMData(config, f);
              }
              if (df!=null) {
                if (Activator.logger.doDebug())
                  Activator.logger.debug("Starting deployment: " + df.getFile().getName());
                deployedFiles.put(df.getFile(), df);
                df.installIfNeeded();
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
      for (final Iterator<DeployedFile> it =
        deployedFiles.values().iterator(); it.hasNext();) {
        final DeployedFile df = it.next();
        final File f = df.getFile();
        if (!f.exists() || !isInScannedDirs(f)) {
          try {
            log("uninstalling since file is removed " + df);
            df.uninstall();
            it.remove();
          } catch (final Exception ex) {
            log("Failed to unininstall " +df +": "+ ex.getMessage(), ex);
          }
        }
      }
    }
  }

  /**
   * Check if any of the currently installed files that has been installed by
   * the dir deployer does not correspond to a file in the scanned directories.
   * If so uninstall the file.
   *
   * <p>
   * Stray files appears for at least two reasons:
   * <ul>
   * <li>The file was removed from one of the scanned directories while the dir
   * deployer bundles is not running.
   * <li>The directory that the file was found in was removed from the list of
   * scanned directories by a configuration update.
   * </ul>
   * </p>
   */
  void scanForStrayFiles()
  {
    // Look for stray deployed bundles:
    for (final Bundle bundle : Activator.bc.getBundles()) {
      final File f = DeployedBundle.installedBundles.get(bundle.getBundleId());
      if (f != null) {
        // Found a bundle deployed by the dir deployer, is it still active?
        final DeployedFile df = deployedFiles.get(f);
        if (df == null) {
          // This is a stray bundle, since it has not been deployed this run.
          try {
            log("uninstalling stray bundle #" + bundle.getBundleId()
                + " not in deploy dirs, location '" + f + "'.");
            bundle.uninstall();
            DeployedBundle.installedBundles.remove(bundle.getBundleId());
            DeployedBundle.saveState();
          } catch (final Exception ex) {
            log("Exception when uninstalling stray bundle #"
                    + bundle.getBundleId() + ", location '" + f + "'; " + ex,
                ex);
          }
        }
      }
    } // Scan for stray bundles

    // Look for stray deployed configurations:
    final ConfigurationAdmin ca = caTracker.getService();
    if (ca != null) {
      try {
        final Configuration[] cfgs = ca.listConfigurations(null);
        if (cfgs != null) {
          for (final Configuration cfg : cfgs) {
            final String pid = cfg.getPid();
            final File f = DeployedCMData.installedCfgs.get(pid);
            if (f != null) {
              // Found a configuration deployed by the dir deployer, is it still
              // active?
              final DeployedFile df = deployedFiles.get(f);
              if (df == null) {
                // This is a stray configuration, since it is currently not
                // deployed!
                try {
                  log("uninstalling stray configuration with pid '" + pid
                      + "', from '" + f + "'.");
                  cfg.delete();
                  DeployedCMData.installedCfgs.remove(pid);
                  DeployedCMData.saveState();
                } catch (final Exception ex) {
                  log("Exception when uninstalling stray configuration with pid '"
                          + pid + "', location '" + f + "'; " + ex, ex);
                }
              }
            }

          }
        }
      } catch (final IOException e) {
        log("Failed to list configurations: " + e.getMessage(), e);
      } catch (final InvalidSyntaxException e) {
        // Will not happen with a null filter.
      }
    } // Scan for stray configurations
  } // scanForStrayFiles()

  /**
   * Set to true during a refresh to avoid triggering a second refresh operation
   * while one is already running.
   */
  boolean refreshRunning = false;

  /**
   * Do a refresh bundles operation on all bundles that has been updated since
   * the last call.
   */
  void refreshUpdatedBundles()
  {
    if (!refreshRunning && DeployedBundle.updatedBundles.size() > 0) {
      synchronized (DeployedBundle.updatedBundles) {
        log("Bundles has been updated; refreshing "
            + DeployedBundle.updatedBundles);
        refreshRunning = true;

        final FrameworkWiring fw =
          Activator.bc.getBundle(0).adapt(FrameworkWiring.class);
        fw.refreshBundles(DeployedBundle.updatedBundles, this);
        // Refresh request sent, clear before releasing monitor.
        DeployedBundle.updatedBundles.clear();
      }
    }
  }

  // Called by the framework when the refresh bundles operation is done.
  public void frameworkEvent(FrameworkEvent event)
  {
    if (FrameworkEvent.PACKAGES_REFRESHED == event.getType()) {
      log("refresh bundles completed");
      refreshRunning = false;
      // We need to run another refresh bundles here to handle updates during
      // the previous refresh.
      refreshUpdatedBundles();
    }
  }

  /**
   * Start installed files marked for start but not yet started.
   */
  void startDelayed()
  {
    for (final DeployedFile df : deployedFiles.values()) {
      try {
        df.start();
      } catch (final Exception ex) {
        log("Failed to start " + df + "; " + ex.getMessage(), ex);
      }
    }
  }

  /**
   * Uninstall all deployed files and clear the deploy map
   */
  void uninstallAll()
  {
    synchronized (deployedFiles) {
      for (final DeployedFile df : deployedFiles.values()) {
        try {
          log("uninstalling " + df);
          df.uninstall();
        } catch (final Exception ex) {
          log("Failed to uinstall " + df + "; " + ex.getMessage(), ex);
        }
      }
      deployedFiles.clear();
    }
  }

  /**
   * Check if a file is in one of the scanned dirs
   *
   * @param f The file to check.
   * @return {@code true} if {@code f} is in one of the scanned directories.
   */
  boolean isInScannedDirs(File f)
  {
    final File fDir = new File(f.getParent());
    synchronized (config) {
      for (final File dir : config.dirs) {
        if (dir.equals(fDir)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Same as {@code log(s, null)}.
   * @param s the message to log.
   */
  static void log(String s)
  {
    Activator.logger.info(s);
  }

  /**
   * Log msg to log service if available and to stdout if not available.
   *
   * @param msg The message to log.
   * @param t optional throwable  to include in the log entry.
   */
  static void log(String msg, Throwable t)
  {
//    final int level = t == null ? LogService.LOG_INFO : LogService.LOG_WARNING;
//
//    log(level, msg, t);
    
    if (t == null) {
      Activator.logger.info(msg);
    } else {
      Activator.logger.warn(msg, t);
    }
  }

  /**
   * Log msg to log service on ERROR level if available and to stdout if not
   * available.
   *
   * @param msg
   *          The message to log.
   * @param t
   *          optional throwable to include in the log entry.
   */
  static void logErr(String msg, Throwable t)
  {
    // log(LogService.LOG_ERROR, msg, t);
    Activator.logger.error(msg, t);
  }

  /**
   * Log msg to log service if available and to stdout if not available.
   *
   * @param level The log level to log on.
   * @param msg The message to log.
   * @param t optional throwable  to include in the log entry.
   */
//  static void log(final int level, final String msg, final Throwable t)
//  {
//    final LogService log = logTracker != null ? logTracker.getService() : null;
//    if (log == null) {
//      final PrintStream out =
//        level == LogService.LOG_ERROR ? System.err : System.out;
//      out.println("[dirdeployer " + level + "] " + msg);
//      if (t != null) {
//        t.printStackTrace(out);
//      }
//    } else {
//      log.log(level, msg, t);
//    }
//  }

}
