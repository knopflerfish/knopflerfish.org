/*
 * Copyright (c) 2003-2014, KNOPFLERFISH project
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

package org.knopflerfish.framework.bundlestorage.file;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Constants;
import org.knopflerfish.framework.BundleArchive;
import org.knopflerfish.framework.BundleStorage;
import org.knopflerfish.framework.FWProps;
import org.knopflerfish.framework.FileTree;
import org.knopflerfish.framework.FrameworkContext;
import org.knopflerfish.framework.Util;


/**
 * Storage of all bundles jar content.
 *
 * @author Jan Stein, Mats-Ola Persson, Gunnar Ekolin
 */
public class BundleStorageImpl implements BundleStorage {

  public final static String ALWAYS_UNPACK_PROP =
    "org.knopflerfish.framework.bundlestorage.file.always_unpack";
  public final static String REFERENCE_PROP =
    "org.knopflerfish.framework.bundlestorage.file.reference";
  public final static String TRUSTED_PROP =
    "org.knopflerfish.framework.bundlestorage.file.trusted";
  public final static String UNPACK_PROP =
    "org.knopflerfish.framework.bundlestorage.file.unpack";
  public final static String JAR_VERIFIER_BUG_PROP =
    "org.knopflerfish.framework.bundlestorage.file.jar_verifier_bug";

  /**
   * Controls if we should try to unpack bundles with sub-jars and
   * native code.
   */
  boolean alwaysUnpack;

  /**
   * Controls if file: URLs should be referenced only, not copied
   * to bundle storage dir
   */
  boolean fileReference;

  /**
   * Controls if we should trust file storage to be secure.
   */
  boolean trustedStorage;

  /**
   * Controls if we should try to unpack bundles with sub-jars and
   * native code.
   */
  boolean unpack;

  /**
   * Optional OS-command to set executable permission on native code.
   */
  String execPermCmd;

  /**
   * Is current OS a Windows OS.
   */
  boolean isWindows;

  /**
   * Is JarVerifier bug present.
   */
  boolean jarVerifierBug;

  /**
   * Top directory for storing all jar data for bundles.
   */
  private FileTree bundlesDir;

  /**
   * Next available bundle id.
   */
  private long nextFreeId = 1;

  /**
   * Bundle id sorted list of all active bundle archives.
   */
  private final ArrayList<BundleArchive> archives = new ArrayList<BundleArchive>();

  /**
   * Framework handle.
   * Package protected to allow BundleArchiveImpl to get framework.
   */
  FrameworkContext     framework;

  /**
   * If we should check if bundles are signed
   */
  boolean checkSigned;

  /**
   * True if we shouldn't write any files.
   */
  private boolean readOnly;

  /**
   * Create a container for all bundle data in this framework.
   * Try to restore all saved bundle archive state.
   *
   */
  public BundleStorageImpl(FrameworkContext framework) {
    this.framework = framework;
    initProps(framework.props);
    // See if we have a storage directory
    bundlesDir = Util.getFileStorage(framework, "bs", !isReadOnly());
    if (bundlesDir == null) {
      throw new RuntimeException("No bundle storage area available!");
    }
    // Restore all saved bundles
    final String [] list = bundlesDir.list();
    for (int i = 0; list != null && i < list.length; i++) {
      long id;
      try {
        id = Long.parseLong(list[i]);
      } catch (final NumberFormatException e) {
        continue;
      }
      if (id == 0) {
        System.err.println("Saved bundle with illegal id 0 is ignored.");
      }
      final int pos = find(id);
      if (pos < archives.size() && archives.get(pos).getBundleId() == id) {
        System.err.println("There are two bundle directories with id: " + id);
        break;
      }
      final FileTree dir = new FileTree(bundlesDir, list[i]);
      if (dir.isDirectory()) {
        try {
          final boolean bUninstalled = BundleArchiveImpl.isUninstalled(dir);
          if(bUninstalled) {
            // silently remove any bundle marked as uninstalled
            dir.delete();
          } else {
            final BundleArchive ba = new BundleArchiveImpl(this, dir, id);
            archives.add(pos, ba);
          }
          if (id >= nextFreeId) {
            nextFreeId = id + 1;
          }
        } catch (final Exception e) {
          dir.delete();
          System.err.println("Removed corrupt bundle dir (" + e.getMessage() + "): " + dir);
        }
      }
    }
  }


  /**
   * Insert bundle into persistent storage
   *
   * @param location Location of bundle.
   * @param is Inputstrem with bundle content.
   * @return Bundle archive object.
   */
  public BundleArchive insertBundleJar(String location, InputStream is)
    throws Exception
  {
    final long id = nextFreeId++;
    final FileTree dir = isReadOnly() ? null : new FileTree(bundlesDir, String.valueOf(id));
    if (dir != null) {
      if (dir.exists()) {
        // remove any old garbage
        dir.delete();
      }
      dir.mkdir();
    }
    try {
      final BundleArchive ba = new BundleArchiveImpl(this, dir, is, location, id);
      archives.add(ba);
      return ba;
    } catch (final Exception e) {
      if (dir != null) {
        dir.delete();
      }
      throw e;
    }
  }


  /**
   * Insert a new jar file into persistent storagedata as an update
   * to an existing bundle archive. To commit this data a call to
   * <code>replaceBundleArchive</code> is needed.
   *
   * @param old BundleArchive to be replaced.
   * @param is Inputstrem with bundle content.
   * @return Bundle archive object.
   */
  public BundleArchive updateBundleArchive(BundleArchive old, InputStream is)
    throws Exception
  {
    return new BundleArchiveImpl((BundleArchiveImpl)old, is);
  }


  /**
   * Replace old bundle archive with a new updated bundle archive, that
   * was created with updateBundleArchive.
   *
   * @param oldBA BundleArchive to be replaced.
   * @param newBA Inputstrem with bundle content.
   * @return New bundle archive object.
   */
  public void replaceBundleArchive(BundleArchive oldBA, BundleArchive newBA)
    throws Exception
  {
    int pos;
    final long id = oldBA.getBundleId();
    synchronized (archives) {
      pos = find(id);
      if (pos >= archives.size() || archives.get(pos) != oldBA) {
        throw new Exception("replaceBundleJar: Old bundle archive not found, pos=" + pos);
      }
      archives.set(pos, newBA);
    }
  }


  /**
   * Get all bundle archive objects.
   *
   * @return Private array of all BundleArchives.
   */
  public BundleArchive [] getAllBundleArchives() {
    synchronized (archives) {
      return archives.toArray(new BundleArchive[archives.size()]);
    }
  }


  /**
   * Get all bundles to start at next launch of framework.
   * This list is sorted in increasing bundle id order.
   *
   * @return Private copy of a List with bundle id's.
   */
  public List<String> getStartOnLaunchBundles() {
    final ArrayList<String> res = new ArrayList<String>();
    for (final BundleArchive ba : archives) {
      if (ba.getAutostartSetting()!=-1) {
        res.add(ba.getBundleLocation());
      }
    }
    return res;
  }


  /**
   * Close bundle storage.
   *
   */
  public void close()
  {
    for (final Iterator<BundleArchive> i = archives.iterator(); i.hasNext(); ) {
      final BundleArchive ba = i.next();
      ba.close();
      i.remove();
    }
    framework = null;
    bundlesDir = null;
  }

  //
  // Package methods
  //

  boolean isReadOnly() {
    return readOnly;
  }


  /**
   * Remove bundle archive from archives list.
   *
   * @param id Bundle archive id to find.
   * @return true if element was removed.
   */
  boolean removeArchive(BundleArchive ba) {
    synchronized (archives) {
      final int pos = find(ba.getBundleId());
      if (pos < archives.size() && archives.get(pos) == ba) {
        archives.remove(pos);
        return true;
      } else {
        return false;
      }
    }
  }


  //
  // Private methods
  //

  /**
   * Initialize values for properties.
   *
   */
  private void initProps(FWProps props) {
    props.setPropertyDefault(ALWAYS_UNPACK_PROP, FWProps.FALSE);
    props.setPropertyDefault(REFERENCE_PROP, FWProps.FALSE);
    props.setPropertyDefault(TRUSTED_PROP, FWProps.TRUE);
    props.setPropertyDefault(UNPACK_PROP, FWProps.TRUE);
    props.setPropertyDefault(JAR_VERIFIER_BUG_PROP, FWProps.FALSE);
    alwaysUnpack = props.getBooleanProperty(ALWAYS_UNPACK_PROP);
    fileReference = props.getBooleanProperty(REFERENCE_PROP);
    trustedStorage = props.getBooleanProperty(TRUSTED_PROP);
    unpack = props.getBooleanProperty(UNPACK_PROP);
    execPermCmd = props.getProperty(Constants.FRAMEWORK_EXECPERMISSION).trim();
    checkSigned = props.getBooleanProperty(FWProps.BUNDLESTORAGE_CHECKSIGNED_PROP);
    isWindows = props.getProperty(Constants.FRAMEWORK_OS_NAME).startsWith("Windows");
    jarVerifierBug = props.getBooleanProperty(JAR_VERIFIER_BUG_PROP);
    readOnly = props.getBooleanProperty(FWProps.READ_ONLY_PROP);
   }


  /**
   * Find posisition for BundleArchive with specified id
   *
   * @param id Bundle archive id to find.
   * @return String to write
   */
  private int find(long id) {
    int lb = 0;
    int ub = archives.size() - 1;
    int x = 0;
    while (lb < ub) {
      x = (lb + ub) / 2;
      final long xid = archives.get(x).getBundleId();
      if (id == xid) {
        return x;
      } else if (id < xid) {
        ub = x;
      } else {
        lb = x+1;
      }
    }
    if (lb < archives.size() && archives.get(lb).getBundleId() < id) {
      return lb + 1;
    }
    return lb;
  }

}
