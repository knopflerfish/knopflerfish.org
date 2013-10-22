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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Class that represents a deployed bundle file.
 */
class DeployedBundle implements DeployedFile
{
  /**
   * Map from bundle id to file path that the bundle was installed from. Used to
   * detect stray bundles (bundles that are installed, but whose file has been
   * removed while the dir deployer was not running).
   */
  static final Map<Long,File> installedBundles = new HashMap<Long, File>();

  /**
   * Get the file to save and load the state from.
   */
  private static File getStateFile()
  {
    final File stateFile =
      Activator.bc.getDataFile(DeployedBundle.class.getName());
    return stateFile;
  }

  /**
   * Saves the state, i.e., the contents of the {@code installedBundles}-Map.
   */
  static void saveState()
  {
    final File stateFile = getStateFile();
    ObjectOutputStream oout = null;
    try {
      final FileOutputStream out = new FileOutputStream(stateFile);
      oout = new ObjectOutputStream(out);
      oout.writeObject(installedBundles);
    } catch (final IOException ioe) {
      DirDeployerImpl.log("Failed to save state of deployed bundles: "
                              + ioe.getMessage(), ioe);
    } finally {
      if (oout!=null) {
        try {
          oout.close();
        } catch (final IOException _ioe) {
          // Ingore.
        }
      }
    }
  }

  /**
   * Loads the state, i.e., the restores the {@code installedBundles}-Map from
   * the previous run.
   */
  static void loadState()
  {
    final File stateFile = getStateFile();
    if (stateFile.canRead()) {
      DirDeployerImpl.log("Loading state of deployed bundles from: "
                          + stateFile);
      ObjectInputStream oin = null;
      try {
        final FileInputStream in = new FileInputStream(stateFile);
        oin = new ObjectInputStream(in);
        @SuppressWarnings("unchecked")
        final Map<Long, File> state = (Map<Long, File>) oin.readObject();
        installedBundles.clear();
        installedBundles.putAll(state);
      } catch (final Exception e) {
        DirDeployerImpl.log("Failed to load saved state of deployed bundles: "
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
    installedBundles.clear();
  }

  /**
   * List with bundles that has been updated, if non-empty a refresh bundles
   * operation is needed.
   */
  static final List<Bundle> updatedBundles = new ArrayList<Bundle>();


  /**
   * Check if a file seems to be a bundle jar file.
   *
   * @param f The file to validate.
   */
  static boolean isBundleFile(File f)
  {
    return f.toString().toLowerCase().endsWith(".jar");
  }

  final Config config;
  final File file;
  final String location;

  long fileLastModified = -1;
  Bundle bundle;
  boolean isFragment = false;
  boolean started = false;

  /**
   * Create a {@link DeployedBundle} instance from a specified file.
   *
   * @throws RuntimeException
   *           if the specified does not exists
   */
  public DeployedBundle(final Config config, final File f)
  {
    if (!f.exists()) {
      throw new RuntimeException("No file " + f);
    }
    if (!f.canRead()) {
      throw new RuntimeException("File '" + f + "' is not readable.");
    }
    this.config = config;
    this.file = f;

    // location URL to be used for for installing bundle
    location = "file:" + file.getPath();
  }

  public File getFile()
  {
    return file;
  }

  /* (non-Javadoc)
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#installIfNeeded()
   */
  public void installIfNeeded()
      throws BundleException
  {
    bundle = Activator.bc.getBundle(location);
    if (bundle == null) {
      InputStream is = null;
      try {
        // Save last modified for the file to avoid installing the same file
        // several times.
        fileLastModified = file.lastModified();

        is = new FileInputStream(file);
        bundle = Activator.bc.installBundle(location, is);
        started = false; // Start attempt is needed.

        // Set bundle start level if requested and possible
        if (!config.useInitialStartLevel) {
          final BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
          if (bsl != null) {
            bsl.setStartLevel(config.startLevel);
          }
        }

        DirDeployerImpl.log("installed " + this);
      } catch (final Exception ioe) {
        DirDeployerImpl.log("Failed to install " + this + "; " + ioe, ioe);
      } finally {
        if (null != is) {
          try {
            is.close();
          } catch (final IOException ioe) {
          }
        }
      }
    } else {
      DirDeployerImpl.log("already installed " + this);
    }

    if (null != bundle) {
      installedBundles.put(bundle.getBundleId(), file);
      saveState();

      // Check if this bundle is a fragment or not.
      final BundleRevision br = bundle.adapt(BundleRevision.class);
      isFragment = (br.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
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
    if (null != bundle && !started) {
      final int state = bundle.getState();
      if (isFragment) {
        if (Bundle.INSTALLED == state) {
          started = true; // Try to start once per install/update
          // Try to attach it by requesting a resolve.
          DirDeployerImpl.log("resolving fragment " + this);
          final FrameworkWiring fw =
            Activator.bc.getBundle(0).adapt(FrameworkWiring.class);
          fw.resolveBundles(Collections.singleton(bundle));
        }
      } else {
        if (Bundle.INSTALLED == state || Bundle.RESOLVED == state) {
          started = true; // Try to start once per install/update
          DirDeployerImpl.log("starting " + this);
          bundle.start(Bundle.START_ACTIVATION_POLICY);
        }
      }
    }
  }

  /* (non-Javadoc)
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#updateIfNeeded()
   */
  public void updateIfNeeded()
      throws BundleException
  {
    if (needUpdate()) {
      if (bundle == null) {
        // The initial file was not a valid bundle, must try an install here.
        DirDeployerImpl.log("installing updated bundle " + this);
        installIfNeeded();
      } else {
        DirDeployerImpl.log("updating " + this);
        InputStream is = null;
        try {
          is = new FileInputStream(file);
          bundle.update(is);
          synchronized (updatedBundles) {
            updatedBundles.add(bundle);
          }

          // Check if the updated bundle is fragment or not.
          final BundleRevision br = bundle.adapt(BundleRevision.class);
          isFragment = (br.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
          started = false; // Start attempt may be needed.
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
  }

  /* (non-Javadoc)
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#uninstall()
   */
  public void uninstall()
      throws BundleException
  {
    if (bundle != null) {
      if (Bundle.UNINSTALLED != bundle.getState()) {
        DirDeployerImpl.log("uninstall " + this);
        bundle.uninstall();
        installedBundles.remove(bundle.getBundleId());
        saveState();
      }
      bundle = null;
    }
  }

  /* (non-Javadoc)
   * @see org.knopflerfish.bundle.dirdeployer.DeployedFile#needUpdate()
   */
  public boolean needUpdate()
  {
    if (bundle != null) {
      return file.lastModified() > bundle.getLastModified();
    }
    return file.lastModified() > fileLastModified;
  }

  @Override
  public String toString()
  {
    return "DeployedBundle[" + "location=" + location + ", bundle=" + bundle
           + "]";
  }

}
