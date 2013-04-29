/*
 * Copyright (c) 2013, KNOPFLERFISH project
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import org.knopflerfish.shared.cm.CMDataReader;

/**
 * Class that represents one deployed CM_Data XML file.
 */
class DeployedCMData
  implements DeployedFile
{
  /**
   * Map from configuration PID to file path that the configuration was read
   * from. Used to detect stray configurations (configurations that are
   * installed, but whose XML-file is not in any of the scanned directories).
   */
  static final Map<String, File> installedCfgs = new HashMap<String, File>();

  /**
   * For factory configurations we need a mapping from the description of it in
   * the file to the actual PID it is saved under in CM. The key in this mapping
   * defined by the return value of {@link #getFilePidForPid(String)}.
   */
  static final Map<String, String> filePidToCmPid =
    new HashMap<String, String>();

  /**
   * Build the key in {@link DeployedCMData#filePidToCmPid} for a factory
   * configuration PID. The key in this mapping is the name of the file followed
   * by '::' and then the value of the service.pid property of the factory
   * configuration. The file path is included in the key to allow the same
   * service.pid value to be re-used in different XML-files.
   *
   * @param pid
   *          the value of the service.pid property in the configuration.
   * @return key value for the given {@code pid.}
   */
  private String getFilePidForPid(final String pid)
  {
    return file.toString() + "::" + pid;
  }

  /**
   * Delete the entry with the given CM PID as value from the
   * {@code filePidToCmPid} mapping.
   *
   * @param cmPid
   *          The CM-PID to remove from the mapping.
   */
  private static void deleteFilePidToCmPidEntry(String cmPid)
  {
    for (final Iterator<Entry<String, String>> it =
      filePidToCmPid.entrySet().iterator(); it.hasNext();) {
      final Entry<String, String> entry = it.next();
      if (entry.getValue().equals(cmPid)) {
        it.remove();
        break;
      }
    }
  }

  /**
   * Get the file to save and load the state from.
   */
  private static File getStateFile()
  {
    final File stateFile =
      Activator.bc.getDataFile(DeployedCMData.class.getName());
    return stateFile;
  }

  /**
   * Saves the state, i.e., the contents of the {@code installedCfgs}-Map.
   */
  static void saveState()
  {
    final File stateFile = getStateFile();
    ObjectOutputStream oout = null;
    try {
      final FileOutputStream out = new FileOutputStream(stateFile);
      oout = new ObjectOutputStream(out);
      oout.writeObject(installedCfgs);
      oout.writeObject(filePidToCmPid);
    } catch (final IOException ioe) {
      DirDeployerImpl.log("Failed to save state of deployed configurations: "
                          + ioe.getMessage(), ioe);
    } finally {
      if (oout != null) {
        try {
          oout.close();
        } catch (final IOException _ioe) {
          // Ingore.
        }
      }
    }
  }

  /**
   * Loads the state, i.e., the restores the {@code installedCfgs}-Map from the
   * previous run.
   */
  static void loadState()
  {
    final File stateFile = getStateFile();
    if (stateFile.canRead()) {
      DirDeployerImpl.log("Loading state of deployed configurations from: "
                          + stateFile);
      ObjectInputStream oin = null;
      try {
        final FileInputStream in = new FileInputStream(stateFile);
        oin = new ObjectInputStream(in);
        @SuppressWarnings("unchecked")
        final Map<String, File> state = (Map<String, File>) oin.readObject();
        installedCfgs.clear();
        installedCfgs.putAll(state);

        @SuppressWarnings("unchecked")
        final Map<String, String> pidToPid =
          (Map<String, String>) oin.readObject();
        filePidToCmPid.clear();
        filePidToCmPid.putAll(pidToPid);
      } catch (final Exception e) {
        DirDeployerImpl
            .log("Failed to load saved state of deployed configurations: "
                     + e.getMessage(), e);
      } finally {
        if (oin != null) {
          try {
            oin.close();
          } catch (final IOException _ioe) {
            // Ingore.
          }
        }
      }
    }
  }

  static void clearState()
  {
    installedCfgs.clear();
  }

  /**
   * Check if a file seems to be a CM_Data XML file.
   *
   * @param f
   *          The file to validate.
   */
  static boolean isCMDataFile(File f)
  {
    // TODO: Also check that the document type is cm_data
    return f.toString().toLowerCase().endsWith(".xml");

  }

  /**
   * Load the contents of a CM_Data-XML file into an array of configurations.
   *
   * @param f
   *          The file to load.
   * @return array of configurations loaded from the file.
   */
  static private Hashtable<String, Object>[] loadCMDataFile(final File f)
  {
    PushbackReader reader = null;
    try {
      final CMDataReader cmDataReader = new CMDataReader();
      final InputStream is = new FileInputStream(f);
      final InputStreamReader isr =
        new InputStreamReader(is, CMDataReader.ENCODING);
      reader = new PushbackReader(new BufferedReader(isr, 8192), 8);

      final Hashtable<String, Object>[] configs =
        cmDataReader.readCMDatas(reader);
      return configs;
    } catch (final Exception e) {
      DirDeployerImpl.log("Failed to load configurations from '" + f + "'; "
                          + e, e);
    } finally {
      if (null != reader) {
        try {
          reader.close();
        } catch (final IOException ioe) {
        }
      }
    }
    @SuppressWarnings("unchecked")
    final Hashtable<String, Object>[] res = new Hashtable[0];
    return res;
  }

  // The current directory deployer configuration.
  final Config config;
  // The file that this instance handles.
  final File file;
  // The PIDs that was created from the file that this instance handles.
  String[] pids = null;

  // Time-stamp of the file when last processed.
  long fileLastModified = -1;

  /**
   * Create a {@link DeployedCMData} instance handling the specified file.
   *
   * @param config
   *          The current Directory Deployer configuration.
   * @param f
   *          The file that this instance shall handle.
   *
   * @throws RuntimeException
   *           if the specified file can not be read.
   */
  public DeployedCMData(final Config config, final File f)
  {
    if (!f.exists()) {
      throw new RuntimeException("No file " + f);
    }
    if (!f.canRead()) {
      throw new RuntimeException("File '" + f + "' is not readable.");
    }
    this.config = config;
    this.file = f;

  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#getFile()
   */
  public File getFile()
  {
    return file;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#installIfNeeded()
   */
  public void installIfNeeded()
      throws BundleException
  {
    final ConfigurationAdmin ca = DirDeployerImpl.caTracker.getService();
    if (pids == null && ca != null) {
      try {
        fileLastModified = file.lastModified();
        // List with all PIDs that is created from the current file.
        final List<String> pidList = new ArrayList<String>();

        // List with all PIDs for factory configurations that are defined in the
        // current file, used for duplicate detection.
        final List<String> pidListFactory = new ArrayList<String>();

        final Hashtable<String, Object>[] configs = loadCMDataFile(file);
        for (final Hashtable<String, Object> config : configs) {
          final String pid = (String) config.get(CMDataReader.SERVICE_PID);
          config.remove("service.bundleLocation");
          final String fpid = (String) config.get(CMDataReader.FACTORY_PID);

          Configuration cfg;
          if (fpid == null) {
            // Non-factory Configuration
            if (pidList.contains(pid)) {
              DirDeployerImpl.logErr("Skipping dupplicated configuration "
                                     + "with pid='" + pid + "' found in "
                                     + file, null);
              continue;
            }
            final File otherFile = installedCfgs.get(pid);
            if (!file.equals(otherFile)) {
              DirDeployerImpl.log("Overwriting configuration with pid='" + pid
                                  + "' defined in '" + otherFile + "'.");
            }
            cfg = ca.getConfiguration(pid, null);
            // Make sure that an existing configuration is unbound from
            // location.
            if (cfg.getBundleLocation() != null) {
              cfg.setBundleLocation(null);
            }
          } else {
            // Factory configuration
            if (pidListFactory.contains(pid)) {
              DirDeployerImpl.logErr("Skipping non-unique factory "
                                     + "configuration with service.pid='" + pid
                                     + "' found in " + file, null);
              continue;
            }
            pidListFactory.add(pid);
            cfg = ca.createFactoryConfiguration(fpid, null);
            DirDeployerImpl.log("Created factory config with pid '"
                                + cfg.getPid()
                                + "' for file configuration with service.pid '"
                                + pid + "'.");
            filePidToCmPid.put(getFilePidForPid(pid), cfg.getPid());
          }

          cfg.update(config);
          pidList.add(cfg.getPid());
          installedCfgs.put(cfg.getPid(), file);
        }
        pids = pidList.toArray(new String[pidList.size()]);

        DirDeployerImpl.log("installed " + this);
      } catch (final Exception e) {
        DirDeployerImpl.log("Failed to install " + this + "; " + e, e);
      }
    } else {
      DirDeployerImpl.log("already installed " + this);
    }

    if (pids != null) {
      saveState();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#start()
   */
  public void start()
      throws BundleException
  {
    // Start is a no-operation for configurations.
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#updateIfNeeded()
   */
  public void updateIfNeeded()
      throws BundleException
  {
    if (needUpdate()) {
      if (pids == null) {
        // The initial file was not a valid CM_Data XML file, must try an
        // install here.
        DirDeployerImpl.log("installing updated configuration " + this);
        installIfNeeded();
      } else {
        final ConfigurationAdmin ca = DirDeployerImpl.caTracker.getService();
        if (ca != null) {
          DirDeployerImpl.log("updating " + this);
          fileLastModified = file.lastModified();

          try {
            // List with all PIDs that has been created or updated from the
            // current file.
            final List<String> pidList = new ArrayList<String>();

            // List with all PIDs for factory configurations that are defined in
            // the current file.
            final List<String> pidListFactory = new ArrayList<String>();

            final Hashtable<String, Object>[] configs = loadCMDataFile(file);
            for (final Hashtable<String, Object> config : configs) {
              final String pid = (String) config.get(CMDataReader.SERVICE_PID);
              config.remove("service.bundleLocation");

              final String fpid = (String) config.get(CMDataReader.FACTORY_PID);
              Configuration cfg  = null;
              if (fpid == null) {
                // Non-factory configuration
                if (pidList.contains(pid)) {
                  DirDeployerImpl
                      .logErr("Skipping dupplicate configuration with "
                              + "pid='" + pid + "' found in " + file, null);
                  continue;
                }
                final File otherFile = installedCfgs.get(pid);
                if (!file.equals(otherFile)) {
                  DirDeployerImpl.log("Overwriting configuration with pid='"
                                      + pid + "' defined in '" + otherFile
                                      + "'.");
                }
                cfg = ca.getConfiguration(pid, null);
                if (cfg.getBundleLocation() != null) {
                  cfg.setBundleLocation(null);
                }
              } else {
                // Factory configuration
                if (pidListFactory.contains(pid)) {
                  DirDeployerImpl
                      .logErr("Skipping non-unique factory configuration "
                              + "with service.pid='" + pid + "' found in "
                              + file, null);
                  continue;
                }
                pidListFactory.add(pid);
                String cmPid = filePidToCmPid.get(getFilePidForPid(pid));
                if (cmPid != null) {
                  // Get the existing factory configuration instance.
                  cfg = ca.getConfiguration(cmPid, null);
                  // Check that fpid matches the factory pid in the
                  // configuration.
                  if (!fpid.equals(cfg.getFactoryPid())) {
                    // Something is wrong; e.g., the old factory cfg instance
                    // has been deleted by somebody else...
                    DirDeployerImpl
                        .log("The factory configuration with instance pid, '"
                             + cmPid + "' does not belong to the factory pid '"
                             + fpid + "', its factory pid is '"
                             + cfg.getFactoryPid()
                             + "', will create a new configuration.");
                    cfg = null;
                    cmPid = null;
                    filePidToCmPid.remove(getFilePidForPid(pid));
                  }
                }
                if (cmPid == null) {
                  // Create a new Factory configuration instance
                  cfg = ca.createFactoryConfiguration(fpid, null);
                  DirDeployerImpl
                      .log("Created factory config with pid '" + cfg.getPid()
                           + "' for file configuration with service.pid='"
                           + pid + "'.");
                  filePidToCmPid.put(getFilePidForPid(pid), cfg.getPid());
                }
              }

              if (cfg != null) {
                cfg.update(config);
                pidList.add(cfg.getPid());
                installedCfgs.put(cfg.getPid(), file);
              }
            }

            // Remove configurations that has been removed from the file
            for (final String pid : pids) {
              if (!pidList.contains(pid)) {
                DirDeployerImpl.log("Deleting configuration with pid '" + pid
                                    + "' since no longer present in '" + file
                                    + "'.");
                final Configuration cfg = ca.getConfiguration(pid, null);
                cfg.delete();
                deleteFilePidToCmPidEntry(pid);
              }
            }

            saveState();
            pids = pidList.toArray(new String[pidList.size()]);

            DirDeployerImpl.log("updated " + this);
          } catch (final Exception e) {
            DirDeployerImpl.log("Failed to update " + this + "; " + e, e);
          }
        } else {
          DirDeployerImpl.log("Can not update configuration, "
                              + "no Configuration Admin service available "
                              + this);
        }
      }
    }
  }// updateIfNeeded()

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#uninstall()
   */
  public void uninstall()
      throws BundleException
  {
    final ConfigurationAdmin ca = DirDeployerImpl.caTracker.getService();
    if (pids != null && ca != null) {
      for (final String pid : pids) {
        try {
          final Configuration cfg = ca.getConfiguration(pid, null);
          cfg.delete();
          installedCfgs.remove(pid);
          deleteFilePidToCmPidEntry(pid);
        } catch (final IOException e) {
          DirDeployerImpl.log("Failed to uninstall configuration with pid '"
                              + pid + "': " + e.getMessage(), e);
        }
      }
      saveState();
      pids = null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#needUpdate()
   */
  public boolean needUpdate()
  {
    return file.lastModified() > fileLastModified;
  }

  @Override
  public String toString()
  {
    return "DeployedCMData[file=" + file + ", pids="
           + (pids == null ? "-" : Arrays.asList(pids).toString()) + "]";
  }

}
