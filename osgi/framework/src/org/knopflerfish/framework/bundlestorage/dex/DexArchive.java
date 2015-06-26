/*
 * Copyright (c) 2015-2015, KNOPFLERFISH project
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

package org.knopflerfish.framework.bundlestorage.dex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.knopflerfish.framework.FileTree;
import org.knopflerfish.framework.bundlestorage.file.Archive;
import org.knopflerfish.framework.bundlestorage.file.BundleArchiveImpl;

import dalvik.system.DexFile;

/**
 * JAR with classes.dex file handling.
 *
 */
public class DexArchive extends Archive {

  private static final String CLASSES_DEX = "classes.dex";
  private static final String CLASS_SUFFIX = ".class";
  private DexFile dexFile = null;


  /**
   * Create an Archive based on contents of an InputStream, the archive is saved
   * as local copy in the specified directory.
   *
   * @param ba BundleArchiveImpl for this archive.
   */
  DexArchive(BundleArchiveImpl ba, FileTree dir, int rev) {
    super(ba, dir, rev);
  }

  @Override
  protected void downloadArchive(InputStream is, URL source)
      throws IOException {
    super.downloadArchive(is, source);
    initDexFile();
  }

  @Override
  protected void restoreArchive() throws IOException {
    super.restoreArchive();
    initDexFile();
  }

  @Override
  protected Archive subArchive(String path, int id) throws IOException {
    Archive res = super.subArchive(path, id);
    if (res.isJar()) {
      ((DexArchive)res).initDexFile();
    }
    return res;
  }

  @Override
  public boolean exists(String path, boolean onlyDirs) {
    if (path.endsWith(CLASS_SUFFIX) && !onlyDirs) {
      if (dexFile == null) {
        throw new RuntimeException("No " + CLASSES_DEX + " found for archive: " + file);
      }
      String name = path.substring(0, path.length() - CLASS_SUFFIX.length()).replace('/', '.');
      for (Enumeration<String> e = dexFile.entries(); e.hasMoreElements(); ) {
        String ee = e.nextElement();
        if (name.equals(ee)) {
          return true;
        }
      }
      return false;
    } else {
      return super.exists(path, onlyDirs);
    }
  }

  /**
   * Return null to indicate that we can not get class bytes from DEX files
   */
  @Override
  public byte[] getClassBytes(String name) {
    return null;
  }

  /**
   * Load a class using the Dalvik DexFile API.
   * <p>
   * This relies in the bundle having a "classes.dex" in its root
   * <p>
   * TODO: We should create a specific bundle storage module for DEX files.
   * <p>
   * 
   * To create such a bundle, do
   * <ol>
   * <li><code>dx --dex --output=classes.dex bundle.jar</code>
   * <li><code>aapt add bundle.jar classes.dex</code>
   * </ol>
   */
  @Override
  public Class<?> loadClassBytes(String name, ClassLoader cl) {
    return dexFile.loadClass(name.replace('.', '/'), cl);
  }

  private void initDexFile() {
    String dexopt = new File(bundleDir, "dexopt" + revision + (subId > 0 ? "_" + subId : "")).getAbsolutePath();
    if (jar != null) {
      if (jar.getEntry(CLASSES_DEX) != null) {
        dexFile = DexFile.loadDex(file.getAbsolutePath(),  dexopt, 0);
      }
    } else {
      dexFile = DexFile.loadDex(new File(file, CLASSES_DEX).getAbsolutePath(), dexopt, 0);
    }
  }

}
