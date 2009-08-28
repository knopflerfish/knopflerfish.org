/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import org.osgi.framework.*;
import org.knopflerfish.framework.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.Dictionary;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;

/**
 * Interface for managing bundle data.
 *
 * @author Jan Stein
 * @author Erik Wistrand
 * @author Robert Shelley
 */
class BundleArchiveImpl implements BundleArchive
{

  /**
   * Bundle status files
   */
  private final static String LOCATION_FILE   = "location";
  private final static String REV_FILE        = "revision";
  private final static String STOP_FILE       = "stopped";
  private final static String STARTLEVEL_FILE = "startlevel";
  private final static String PERSISTENT_FILE = "persistent";

  /**
   * Method mapLibraryName if we run in a Java 2 environment.
   */
  private static Method mapLibraryName;


  private Archive archive;

  private long id;

  private String location;

  private boolean startOnLaunch;

  private FileTree bundleDir;

  private BundleStorageImpl storage;

  private Archive [] archives;

  private Map nativeLibs;

  //XXX - start L-3 modification
  private Map renameLibs;
  //XXX - end L-3 modification

  private boolean bFake = false;

  private int startLevel = -1;

  private boolean bPersistent = false;

  static {
    try {
      mapLibraryName = System.class.getDeclaredMethod("mapLibraryName", new Class [] { String.class });
    } catch (NoSuchMethodException ignore) {
      mapLibraryName = null;
    }
  }


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
    bundleDir       = dir;
    archive         = new Archive(bundleDir, 0, is, source);
    storage         = bundleStorage;
    id              = bundleId;
    location        = bundleLocation;
    startOnLaunch   = false;
    nativeLibs      = getNativeCode();

    bFake           = isFake();

    setClassPath();

    putContent(STOP_FILE, new Boolean(!startOnLaunch).toString());
    putContent(LOCATION_FILE, location);
    //    putContent(STARTLEVEL_FILE, Integer.toString(startLevel));
  }

  /**
   * Construct new bundle archive based on saved data.
   *
   */
  BundleArchiveImpl(BundleStorageImpl bundleStorage,
                    FileTree dir,
                    long bundleId)
    throws Exception
  {
    bundleDir = dir;
    location = getContent(LOCATION_FILE);
    if (location == null || location.length() == 0) {
      throw new IOException("No bundle location information found");
    }
    int rev = -1;
    String revS = getContent(REV_FILE);
    if (revS != null) {
      try {
        rev = Integer.parseInt(revS);
      } catch (NumberFormatException e) { }
    }

    String slS = getContent(STARTLEVEL_FILE);
    if (slS != null) {
      try {
        startLevel = Integer.parseInt(slS);
      } catch (NumberFormatException e) { }
    }

    String pS = getContent(PERSISTENT_FILE);
    if (pS != null) {
      bPersistent = "true".equals(pS);
    }

    archive       = new Archive(bundleDir, rev, location);
    id            = bundleId;
    storage       = bundleStorage;
    startOnLaunch = !(new Boolean(getContent(STOP_FILE))).booleanValue();
    nativeLibs    = getNativeCode();

    bFake         = isFake();

    setClassPath();
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
    startOnLaunch = old.startOnLaunch;
    int rev = old.archive.getRevision() + 1;
    archive = new Archive(bundleDir, rev, is);
    nativeLibs = getNativeCode();

    bFake = isFake();

    setClassPath();
    putContent(REV_FILE, Integer.toString(rev));
  }

  boolean isFake() {
    // What the f**k is this? Test case seem to require it!
    //
    // OK. Some background story might me good here:
    //
    // The R3 tests are not compatible with the R3 spec (sic)
    //
    // However to be R3, you have to pass the tests.
    // Thus, KF uses a system property to determine if
    // it should be compartible with the spec or the tests.
    // Framework.R3_TESTCOMPLIANT reflects this state.
    //
    // One such difference is the "fakeheader" manifest
    // (another on is the buggy filter test, see LDAPEpr.java)
    // attribute that the test suite at one stage uses
    // to read a "bad" manifest, but still pass the
    // installBundle stage. When this header is present
    // AND we run in test compliant mode, we skip some
    // sanity checks on manifests
    if(Framework.R3_TESTCOMPLIANT) {
      String fake = getAttribute("fakeheader");
      if(fake != null) {
        return true;
      }
    }
    return false;
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
   * Get all attributes from the manifest of a bundle.
   *
   * @return All attributes, null if bundle doesn't exists.
   */
  public Dictionary getAttributes() {
    return new HeaderDictionary(archive.getAttributes());
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

  public int getStartLevel() {
    return startLevel;
  }

  public void setStartLevel(int level) throws IOException {
    if (startLevel != level) {
      startLevel = level;
      putContent(STARTLEVEL_FILE, Integer.toString(startLevel));
    }
  }

  public void setPersistent(boolean b) throws IOException {
    if (bPersistent != b) {
      bPersistent = b;
      putContent(PERSISTENT_FILE, b ? "true" : "false");
    }
  }


  public boolean isPersistent() {
    return bPersistent;
  }



  /**
   * Get a byte array containg the contents of named file from a bundle
   * archive.
   *
   * @param clazz Class to get.
   * @return Byte array with contents of file or null if file doesn't exist.
   * @exception IOException if failed to read jar entry.
   */
  public byte[] getClassBytes(String clazz) throws IOException {
    String cp = clazz.replace('.', '/') + ".class";
    for (int i = 0; i < archives.length; i++) {
      byte [] res = archives[i].getBytes(cp);
      if (res != null) {
        return res;
      }
    }
    return null;
  }


  /**
   * Check if named entry exist in bundles archive.
   * Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @return Vector or entry numbers, or null if it doesn't exist.
   */
  public Vector componentExists(String component) {
    Vector v = null;

    if (component.startsWith("/")) {
      component = component.substring(1);
    }


    for (int i = 0; i < archives.length; i++) {
      InputStream is = archives[i].getInputStream(component);
      if (is != null) {
        if(v == null) {
           v = new Vector();
        }
        v.addElement(new Integer(i));
        try {
          is.close();
        } catch (IOException ignore) { }
      }
    }
    return v;
  }


  /**
   * Same as getInputStream(component, -1)
   */
  public InputStream getInputStream(String component) {
    return getInputStream(component, -1);
  }

  /**
   * Get an specific InputStream to named entry inside a bundle.
   * Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @param ix index of jar. 0 means the top level. -1 means any
   * @return InputStream to entry or null if it doesn't exist.
   */
  public InputStream getInputStream(String component, int ix) {
    if (component.startsWith("/")) {
      component = component.substring(1);
    }

    if(ix == -1) {
      for (int i = 0; i < archives.length; i++) {
        InputStream res = archives[i].getInputStream(component);
        if (res != null) {
          return res;
        }
      }
      return null;
    } else {
      return archives[ix].getInputStream(component);
    }
  }


  /**
   * Get native library from JAR.
   *
   * @param component Name of Jar file to get.
   * @return A string with path to native library.
   */
  public String getNativeLibrary(String component) {
    if (nativeLibs != null) {
      try {
//XXX - start L-3 modification
        String key = (String)mapLibraryName.invoke(null, new Object[] {component});
        String val = (String)nativeLibs.get(key);
        File file1 = new File(val);
        if (file1.exists() && file1.isFile()) {
          if (renameLibs.containsKey(key)) {
            File file2 = new File((String)renameLibs.get(key));
            if (file1.renameTo(file2)) {
              val = file2.getAbsolutePath();
              nativeLibs.put(key, val);
            }
          }
          StringBuffer rename = new StringBuffer(val);
          int index0 = val.lastIndexOf(File.separatorChar) + 1;
          int index1 = val.indexOf("_", index0);
          if((index1 > index0) && (index1 == val.length() - key.length() - 1)) {
            try {
              int prefix = Integer.parseInt(val.substring(index0, index1));
              rename.replace(index0, index1, Integer.toString(prefix + 1));
            } catch (Throwable t) {
              rename.insert(index0, "0_");
            }
          } else {
            rename.insert(index0, "0_");
          }
          renameLibs.put(key, rename.toString());
        }
        return val;
//XXX - end L-3 modification
      } catch (Exception ignore) { }
    }
    return null;
  }


  /**
   * Get state of start on launch flag.
   *
   * @return Boolean value for start on launch flag.
   */
  public boolean getStartOnLaunchFlag() {
    return startOnLaunch;
  }


  /**
   * Set state of start on launch flag.
   *
   * @param value Boolean value for start on launch flag.
   */
  public void setStartOnLaunchFlag(boolean value) throws IOException {
    if (startOnLaunch != value) {
      startOnLaunch = value;
      putContent(STOP_FILE, new Boolean(!startOnLaunch).toString());
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
      (new File(bundleDir, STOP_FILE)).delete();
      (new File(bundleDir, REV_FILE)).delete();
      (new File(bundleDir, STARTLEVEL_FILE)).delete();
      (new File(bundleDir, PERSISTENT_FILE)).delete();
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
    for (int i = 0; i < archives.length; i++) {
      archives[i].close();
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
   * @return contents of the file as a single string
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
   * Uninstalled is marked via a startlevel of -2
   * </p>
   */
  static boolean isUninstalled(File dir) {
    String s = getContent(dir, STARTLEVEL_FILE);
    int n = -1;
    try {
      n = Integer.parseInt(s);
    } catch (Exception e) {
    }
    return n == -2;
  }

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


  private void setClassPath() throws IOException {
    String bcp = getAttribute(Constants.BUNDLE_CLASSPATH);

    if (!bFake && (bcp != null)) {
      ArrayList a = new ArrayList();
      StringTokenizer st = new StringTokenizer(bcp, ",");
      while (st.hasMoreTokens()) {
        String path = st.nextToken().trim();
        if (".".equals(path)) {
          a.add(archive);
        } else {
          a.add(archive.getSubArchive(path));
        }
      }
      archives = (Archive [])a.toArray(new Archive[a.size()]);
    } else {
      archives = new Archive[] { archive };
    }

  }

  /**
   * Check for native code libraries.
   *
   * @param bnc Is the Bundle-NativeCode string.
   * @return A List of Strings with pathname to native code libraries or
   *         null if input was null.
   * @exception IllegalArgumentException If syntax error in input string.
   * @exception Exception If can not find an entry that match this JVM.
   */
  private Map getNativeCode() throws Exception {
    String bnc = getAttribute(Constants.BUNDLE_NATIVECODE);
    if (bnc != null) {
      if (mapLibraryName == null) {
        throw new Exception("Native-Code: Not supported on non Java 2 platforms.");
      }
      Map best = null;
      List perfectVer = new ArrayList();
      List okVer = new ArrayList();
      List noVer = new ArrayList();
      for (Iterator i = Util.parseEntries(Constants.BUNDLE_NATIVECODE, bnc, false); i.hasNext(); ) {
        Map params = (Map)i.next();
        String p = Framework.getProperty(Constants.FRAMEWORK_PROCESSOR);
        List pl = (List)params.get(Constants.BUNDLE_NATIVECODE_PROCESSOR);
        String o =  Framework.getProperty(Constants.FRAMEWORK_OS_NAME);
        List ol = (List)params.get(Constants.BUNDLE_NATIVECODE_OSNAME);
        if ((containsIgnoreCase(pl, p) ||
             containsIgnoreCase(pl, Alias.unifyProcessor(p))) &&
            (containsIgnoreCase(ol, o) ||
             containsIgnoreCase(ol, Alias.unifyOsName(o)))) {
          String fosVer = Framework.getProperty(Constants.FRAMEWORK_OS_VERSION);
          List ver = (List)params.get(Constants.BUNDLE_NATIVECODE_OSVERSION);
          // Skip if we require a newer OS version.
          if (ver != null) {
            for (Iterator v = ver.iterator(); v.hasNext(); ) {
              String nov = (String)v.next();
              int cmp = Util.compareStringVersion(nov, fosVer);
              if (cmp == 0) {
                // Found perfect OS version
                perfectVer.add(params);
                break;
              }
              if (cmp < 0 && !okVer.contains(params)) {
                // Found lower OS version
                okVer.add(params);
              }
            }
          } else {
            // Found unspecfied OS version
            noVer.add(params);
          }
        }
      }

      List langSearch = null;
      if (perfectVer.size() == 1) {
        best = (Map)perfectVer.get(0);
      } else if (perfectVer.size() > 1) {
        langSearch = perfectVer;
      } else if (okVer.size() == 1) {
        best = (Map)okVer.get(0);
      } else if (okVer.size() > 1) {
        langSearch = okVer;
      } else if (noVer.size() == 1) {
        best = (Map)noVer.get(0);
      } else if (noVer.size() > 1) {
        langSearch = noVer;
      }
      if (langSearch != null) {
        String fosLang = Framework.getProperty(Constants.FRAMEWORK_LANGUAGE);
        lloop: for (Iterator i = langSearch.iterator(); i.hasNext(); ) {
          Map params = (Map)i.next();
          List lang = (List)params.get(Constants.BUNDLE_NATIVECODE_LANGUAGE);
          if (lang != null) {
            for (Iterator l = lang.iterator(); l.hasNext(); ) {
              if (fosLang.equalsIgnoreCase((String)l.next())) {
                // Found specfied language version, search no more
                best = params;
                break lloop;
              }
            }
          } else {
            // Found unspecfied language version
            best = params;
          }
        }
      }
      if (best == null) {
        throw new Exception("Native-Code: No matching libraries found.");
      }
//XXX - start L-3 modification
      renameLibs  = new HashMap();
//XXX - end L-3 modification
      HashMap res = new HashMap();
      for (Iterator p = ((List)best.get("keys")).iterator(); p.hasNext();) {
        String name = (String)p.next();
        int sp = name.lastIndexOf('/');
        String key = (sp != -1) ? name.substring(sp+1) : name;
        res.put(key, archive.getNativeLibrary(name));
      }
      return res;
    } else {
      // No native code in this bundle
      return null;
    }
  }

  /**
   * Check if a string exists in a list. Ignore case when comparing.
   */
  private boolean containsIgnoreCase(List l, String s) {
    for (Iterator i = l.iterator(); i.hasNext(); ) {
      if (s.equalsIgnoreCase((String)i.next())) {
        return true;
      }
    }
    return false;
  }

}
