
/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import org.knopflerfish.framework.*;
import org.knopflerfish.framework.bundlestorage.Util;
import java.io.*;
import java.security.cert.Certificate;
import java.util.*;
import java.net.URL;

/**
 * Interface for managing bundle data.
 *
 * @author Jan Stein
 * @author Erik Wistrand
 * @author Robert Shelley
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */
class BundleArchiveImpl implements BundleArchive
{

  /**
   * Bundle status files
   */
  private final static String LOCATION_FILE      = "location";
  private final static String REV_FILE           = "revision";
  private final static String AUTOSTART_FILE     = "autostart";
  private final static String STARTLEVEL_FILE    = "startlevel";
  private final static String LAST_MODIFIED_FILE = "lastModifed";


  final BundleStorageImpl storage;

  private Archive archive;

  private BundleGeneration bundleGeneration = null;

  private long id;

  final private String location;

  private int autostartSetting = -1; // => not started.

  private FileTree bundleDir;

  private ArrayList /* FileArchive */ archives;

  private int startLevel = -1;

  private long lastModified = 0;

  private ArrayList /* List(X509Certificate) */ trustedCerts = null;

  private ArrayList /* List(X509Certificate) */ untrustedCerts = null;

  private boolean checkCerts = true;

  /**
   * Construct new bundle archive.
   *
   */
  BundleArchiveImpl(BundleStorageImpl bundleStorage,
                    FileTree          dir,
                    InputStream       is,
                    String            bundleLocation,
                    long              bundleId)
    throws Exception
  {
    URL source = null;
    try {
      source = new URL(bundleLocation);
    } catch (Exception e) {
    }
    bundleDir        = dir;
    storage          = bundleStorage;
    id               = bundleId;
    location         = bundleLocation;
    archive          = new Archive(this, bundleDir, 0, is, source, location);
    putContent(LOCATION_FILE, location);
    //    putContent(STARTLEVEL_FILE, Integer.toString(startLevel));
  }

  /**
   * Construct new bundle archive based on saved data.
   *
   */
  BundleArchiveImpl(BundleStorageImpl bundleStorage, FileTree dir, long bundleId)
    throws Exception
  {
    bundleDir = dir;
    location = getContent(LOCATION_FILE);
    if (location == null || location.length() == 0) {
      throw new IOException("No bundle location information found");
    }
    int rev = -1;
    String s = getContent(REV_FILE);
    if (s != null) {
      try {
        rev = Integer.parseInt(s);
      } catch (NumberFormatException e) { }
    }

    s = getContent(STARTLEVEL_FILE);
    if (s != null) {
      try {
        startLevel = Integer.parseInt(s);
      } catch (NumberFormatException e) { }
    }

    s = getContent(LAST_MODIFIED_FILE);
    if (s != null) {
        try {
                lastModified = Long.parseLong(s);
        }
        catch (NumberFormatException ignore) {}
    }

    s = getContent(AUTOSTART_FILE);
    if (s != null) {
      try {
        autostartSetting = Integer.parseInt(s);
      } catch (NumberFormatException ignore) {}
    }

    id            = bundleId;
    storage       = bundleStorage;
    archive       = new Archive(this, bundleDir, rev, location);
  }


  /**
   * Construct new bundle archive in an existing bundle archive.
   *
   */
  BundleArchiveImpl(BundleArchiveImpl old, InputStream is)
    throws Exception
  {
    bundleDir = old.bundleDir;
    location = old.location;
    storage = old.storage;
    id = old.id;
    autostartSetting = old.autostartSetting;
    int rev = old.archive.getRevision() + 1;
    URL source = null;

    boolean bReference = (is == null);
    if(bReference) {
      source = new URL(location);
    }
    archive = new Archive(this, bundleDir, rev, is, source, location);
    if(!bReference) {
      putContent(REV_FILE, Integer.toString(rev));
    }
  }


  /**
   * Get an attribute from the manifest of a bundle.
   *
   * @param key Name of attribute to get.
   * @return A string with result or null if the entry doesn't exists.
   */
  public String getAttribute(String key) {
    return archive.getAttribute(key);
  }



  /**
   * Get a FileArchive handle to a named Jar file or directory
   * within this archive.
   *
   * @param path Name of Jar file or directory to get.
   * @return A FileArchive object representing new archive, null if not found.
   * @exception IOException if we failed to get top of file archive.
   */
  public FileArchive getFileArchive(String path) {
    if (".".equals(path)) {
      return archive;
    }
    if (archives == null) {
      archives = new ArrayList();
    }
    try {
      Archive a = new Archive(archive, path, archives.size() + 1);
      archives.add(a);
      return a;
    } catch (IOException io) {
      // TBD, Where to log this
      return null;
    }
  }


  /**
   * returns the localization entries of this archive.
   */
  public Hashtable getLocalizationEntries(String localeFile) {
    BundleResourceStream aif = archive.getBundleResourceStream(localeFile);
    if (aif != null) {
      Properties l = new Properties();
      try {
        l.load(aif);
      } catch (IOException _ignore) { }
      try {
        aif.close();
      } catch (IOException _ignore) { }
      return l;
    } else {
      return null;
    }
  }


  /**
   * returns the raw unlocalized entries of this archive.
   */
  public HeaderDictionary getUnlocalizedAttributes() {
    return new HeaderDictionary(archive.manifest.getMainAttributes());
  }


  /**
   * Get bundle generation associated with this bundle archive.
   *
   * @return BundleGeneration object.
   */
  public BundleGeneration getBundleGeneration() {
    return bundleGeneration;
  }


  /**
   * Set bundle generation associated with this bundle archive.
   *
   * @param BundleGeneration object.
   */
  public void setBundleGeneration(BundleGeneration bg) {
    bundleGeneration = bg; 
  }


  /**
   * Get bundle identifier for this bundle archive.
   *
   * @return Bundle identifier.
   */
  public long getBundleId() {
    return id;
  }


  /**
   * Get bundle location for this bundle archive.
   *
   * @return Bundle location.
   */
  public String getBundleLocation() {
    return location;
  }


  /**
   *
   */
  public int getStartLevel() {
    return startLevel;
  }


  /**
   *
   */
  public void setStartLevel(int level) throws IOException {
    if (startLevel != level) {
      startLevel = level;
      putContent(STARTLEVEL_FILE, Integer.toString(startLevel));
    }
  }


  /**
   *
   */
  public long getLastModified() {
    return lastModified;
  }


  /**
   *
   */
  public void setLastModified(long timemillisecs) throws IOException{
    lastModified = timemillisecs;
    putContent(LAST_MODIFIED_FILE, Long.toString(timemillisecs));
  }


  /**
   * Get a BundleResourceStream to named entry inside a bundle.
   * Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @param ix index of sub archives. A postive number is the classpath entry
   *            index. 0 means look in the main bundle.
   * @return BundleResourceStream to entry or null if it doesn't exist.
   */
  public BundleResourceStream getBundleResourceStream(String component, int ix) {
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    if (ix == 0) {
      return archive.getBundleResourceStream(component);
    } else {
      return ((FileArchive)archives.get(ix - 1)).getBundleResourceStream(component);
    }
  }


  /**
   * Get autostart setting.
   *
   * @return the autostart setting.
   */
  public int getAutostartSetting() {
    return autostartSetting;
  }


  /**
   * Set autostart setting.
   *
   * @param setting the new autostart setting.
   */
  public void setAutostartSetting(int setting) throws IOException {
    if (setting != autostartSetting) {
      autostartSetting = setting;
      putContent(AUTOSTART_FILE, String.valueOf(autostartSetting));
    }
  }


  /**
   */
  public Enumeration findResourcesPath(String path) {
    return archive.findResourcesPath(path);
  }


  /**
   */
  public String getJarLocation() {
    return archive.getPath();
  }


  /**
   * Return certificates for signed bundle, otherwise null.
   *
   * @return An array of certificates or null.
   */
  public ArrayList getCertificateChains(boolean onlyTrusted) {
    if (checkCerts) {
      Certificate [] c = archive.getCertificates();
      checkCerts = false;
      if (c != null) {
        ArrayList failed = new ArrayList();
        untrustedCerts = Util.getCertificateChains(c, failed);
        if (!failed.isEmpty()) {
          // NYI, log Bundle archive has invalid certificates
          untrustedCerts = null;
        }
      }
    }
    ArrayList res = trustedCerts;
    if (!onlyTrusted && untrustedCerts != null) {
      if (res == null) {
        res = untrustedCerts;
      } else {
        res = new ArrayList(trustedCerts.size() + untrustedCerts.size());
        res.addAll(trustedCerts);
        res.addAll(untrustedCerts);
      }        
    }
    return res;
  }


  /**
   * Mark certificate chain as trusted.
   *
   */
  public void trustCertificateChain(List trustedChain) {
    if (trustedCerts == null) {
      trustedCerts = new ArrayList(untrustedCerts.size());
    }
    trustedCerts.add(trustedChain);
    untrustedCerts.remove(trustedChain);
    if (untrustedCerts.isEmpty()) {
      untrustedCerts = null;
    }
  }


  /**
   * Remove bundle archive from persistent storage. If we removed
   * the active revision also remove bundle status files.
   */
  public void purge() {
    close();
    if (storage.removeArchive(this)) {
      (new File(bundleDir, LOCATION_FILE)).delete();
      (new File(bundleDir, AUTOSTART_FILE)).delete();
      (new File(bundleDir, REV_FILE)).delete();
      (new File(bundleDir, STARTLEVEL_FILE)).delete();
      (new File(bundleDir, LAST_MODIFIED_FILE)).delete();
    }
    archive.purge();
    if (bundleDir.list().length == 0) {
      bundleDir.delete();
    }
  }


  /**
   * Close archive for further access. It should still be possible
   * to get attributes.
   */
  public void close() {
    if (archives != null) {
      for (Iterator i = archives.iterator(); i.hasNext(); ) {
        ((Archive)i.next()).close();
      }
      archives = null;
    }
    archive.close();
  }

  //
  // Private methods
  //

  /**
   * Read content of file as a string.
   *
   * @param f File to read from
   * @return contents of the file as a single String
   */
  private String getContent(String f) {
    DataInputStream in = null;
    try {
      in = new DataInputStream(new FileInputStream(new File(bundleDir, f)));
      return in.readUTF();
    } catch (IOException ignore) {
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) { }
      }
    }
    return null;
  }


  /**
   * Statically check if a directory contains info that a bundle
   * is uninstalled.
   *
   * <p>
   * Uninstalled is marked via a startlevel of -2. If last modified
   * file is not available then the bundle is not complete.
   * </p>
   */
  static boolean isUninstalled(File dir) {
    String s = getContent(dir, LAST_MODIFIED_FILE);
    if (s == null || s.length() == 0) {
      return true;
    }
    s = getContent(dir, STARTLEVEL_FILE);
    int n = -1;
    try {
      n = Integer.parseInt(s);
    } catch (Exception e) {
    }
    return n == -2;
  }


  /**
   *
   */
  static String getContent(File dir, String f) {
    DataInputStream in = null;
    try {
      in = new DataInputStream(new FileInputStream(new File(dir, f)));
      return in.readUTF();
    } catch (IOException ignore) {
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignore) { }
      }
    }
    return null;
  }


  /**
   * Write string to named file.
   *
   * @param f File to write too
   * @param contenet String to write
   * @exception IOException if we fail to save our string
   */
  private void putContent(String f, String content) throws IOException {
    DataOutputStream out = null;
    try {
      out = new DataOutputStream(new FileOutputStream(new File(bundleDir, f)));
      out.writeUTF(content);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

}//class
