/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
  private final static String STOP_FILE          = "stopped";
  private final static String STARTLEVEL_FILE    = "startlevel";
  private final static String PERSISTENT_FILE    = "persistent";
  private final static String LAST_MODIFIED_FILE = "lastModifed";

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

  private Map renameLibs;

  private int startLevel = -1;

  private boolean bPersistent;
  
  private long lastModified = 0;

  private ArrayList failedPath = null;

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
    setClassPath();
    putContent(STOP_FILE, new Boolean(!startOnLaunch).toString());
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

    bPersistent = "true".equals(getContent(PERSISTENT_FILE));
    
    s = getContent(LAST_MODIFIED_FILE);
    if (s != null) {
        try {
                lastModified = Long.parseLong(s);
        } 
        catch (NumberFormatException ignore) {}
    }

    archive       = new Archive(bundleDir, rev, location);
    id            = bundleId;
    storage       = bundleStorage;
    startOnLaunch = !(new Boolean(getContent(STOP_FILE))).booleanValue();
    nativeLibs    = getNativeCode();
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
    bPersistent = old.bPersistent;
    int rev = old.archive.getRevision() + 1;
    archive = new Archive(bundleDir, rev, is);
    nativeLibs = getNativeCode();
    setClassPath();
    putContent(REV_FILE, Integer.toString(rev));
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
   * returns the localization entries of this archive.
   */
  public Hashtable getLocalizationEntries(String localeFile) {
    Archive.InputFlow aif = archive.getInputFlow(localeFile);
    if (aif != null) {
      Properties l = new Properties();
      try {
        l.load(aif.is);
      } catch (IOException _ignore) { }
      try {
        aif.is.close();
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


  public long getLastModified() {
                return lastModified;
  }

  
  public void setLastModified(long timemillisecs) throws IOException{
          lastModified = timemillisecs;
          putContent(LAST_MODIFIED_FILE, Long.toString(timemillisecs));
  }
  

  /**
   * Get a byte array containg the contents of named file from a bundle
   * archive.
   *
   * @param sub index of jar, 0 means the top level.
   * @param path Path to class file.
   * @return Byte array with contents of file or null if file doesn't exist.
   * @exception IOException if failed to read jar entry.
   */
  public byte[] getClassBytes(Integer sub, String path) throws IOException {
    return archives[sub.intValue()].getClassBytes(path);
  }


  /**
   * Check if named entry exist in bundles archive.
   * Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @param onlyFirst End search when we find first entry if this is true.
   * @return Vector or entry numbers, or null if it doesn't exist.
   */
  public Vector componentExists(String component, boolean onlyFirst) {
    Vector v = null;
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    for (int i = 0; i < archives.length; i++) {
      Archive.InputFlow aif = archives[i].getInputFlow(component);
      if (aif != null) {
        if (v == null) {
          v = new Vector();
        }
        v.addElement(new Integer(i));
        try {
          aif.is.close();
        } 
        catch (IOException ignore) { }
        if (onlyFirst) {
          break;
        }
      }
    }
    return v;
  }


  /**
   * Get an specific InputStream to named entry inside a bundle.
   * Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @param ix index of sub archives. A postive number is the classpath entry
   *            index. -1 means look in the main bundle.
   * @return InputStream to entry or null if it doesn't exist.
   */
  public InputStream getInputStream(String component, int ix) {
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    Archive.InputFlow aif;
    if (ix == -1) {
      aif = archive.getInputFlow(component);
    } else {
      aif = archives[ix].getInputFlow(component);
    }
    return aif != null ? aif.is : null;
  }


  /**
   * Get native library from JAR.
   *
   * @param libName Name of Jar file to get.
   * @return A string with the path to the native library.
   */
  public String getNativeLibrary(String libName) {
    if (nativeLibs != null) {
      try {
        String key = (String)mapLibraryName.invoke(null, new Object[] {libName});
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
            } 
            catch (Throwable t) {
              rename.insert(index0, "0_");
            }
          } 
          else {
            rename.insert(index0, "0_");
          }
          renameLibs.put(key, rename.toString());
        }
        return val;
      } 
      catch (Exception ignore) {      
      }
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
    for (int i = 0; i < archives.length; i++) {
      archives[i].close();
    }
    archive.close();
  }


  /**
   * Get a list with all classpath entries we failed to locate.
   *
   * @return A List with all failed classpath entries, null if no failures.
   */
  public List getFailedClassPathEntries() {
    return failedPath;
  }

  //
  // Private methods
  //

  /**
   * Read content of file as a string.
   *
   * @param f File to write too
   * @return content String to write
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
    
    if (bcp != null) {
      ArrayList a = new ArrayList();
      StringTokenizer st = new StringTokenizer(bcp, ",");
      while (st.hasMoreTokens()) {
        String path = st.nextToken().trim();
        if (".".equals(path)) {
          a.add(archive);
        } else {
          try {
            a.add(archive.getSubArchive(path));
          } catch (IOException ioe) {
            if (failedPath == null) {
              failedPath = new ArrayList(1);
            }
            failedPath.add(path);
          }
        }
      }
      archives = (Archive [])a.toArray(new Archive[a.size()]);
    } 
    else {
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
      String proc = Framework.getProperty(Constants.FRAMEWORK_PROCESSOR);
      String os =  Framework.getProperty(Constants.FRAMEWORK_OS_NAME);
      Version osVer = new Version(Framework.getProperty(Constants.FRAMEWORK_OS_VERSION));
      String osLang = Framework.getProperty(Constants.FRAMEWORK_LANGUAGE);
      boolean optional = false;
      List best = null;
      VersionRange bestVer = null;
      boolean bestLang = false;

      for (Iterator i = Util.parseEntries(Constants.BUNDLE_NATIVECODE, bnc, false, false, false); i.hasNext(); ) {
        VersionRange matchVer = null;
        boolean matchLang = false;
        Map params = (Map)i.next();

        List keys = (List)params.get("keys");
        if (keys.size() == 1 && "*".equals(keys.get(0)) && !i.hasNext()) {
          optional = true;
          break;
        }

        List pl = (List)params.get(Constants.BUNDLE_NATIVECODE_PROCESSOR);
        if (pl != null) {
          if (!containsIgnoreCase(pl, Alias.unifyProcessor(proc))) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        List ol = (List)params.get(Constants.BUNDLE_NATIVECODE_OSNAME);
        if (ol != null) {
          if (!containsIgnoreCase(ol, Alias.unifyOsName(os))) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        List ver = (List)params.get(Constants.BUNDLE_NATIVECODE_OSVERSION);
        if (ver != null) {
          boolean okVer = false;
          for (Iterator v = ver.iterator(); v.hasNext(); ) {
            // NYI! Handle format Exception
            matchVer = new VersionRange((String)v.next());
            if (matchVer.withinRange(osVer)) {
              okVer = true;
              break;
            }
          }
          if (!okVer) {
            continue;
          }
        }

        List lang = (List)params.get(Constants.BUNDLE_NATIVECODE_LANGUAGE);
        if (lang != null) {
          for (Iterator l = lang.iterator(); l.hasNext(); ) {
            if (osLang.equalsIgnoreCase((String)l.next())) {
              // Found specfied language version, search no more
              matchLang = true;
              break;
            }
          }
          if (!matchLang) {
            continue;
          }
        } 

        List sf = (List)params.get(Constants.SELECTION_FILTER_ATTRIBUTE);
        if (sf != null) {
          if (sf.size() == 1) {
            FilterImpl filter = new FilterImpl((String)sf.get(0));
            if (!filter.match(Framework.getProperties())) {
              continue;
            }
          } else {
            //NYI! complain about faulty selection
          }
        }

        // Compare to previous best
        if (best != null) {
          boolean verEqual = false;
          if (bestVer != null) {
            if (matchVer == null) {
              continue;
            }
            int d = bestVer.compareTo(matchVer);
            if (d == 0) {
              verEqual = true;
            } else if (d > 0) {
              continue;
            }
          } else if (matchVer == null) {
            verEqual = true;
          }
          if (verEqual && (!matchLang || bestLang)) {
            continue;
          }
        }
        best = keys;
        bestVer = matchVer;
        bestLang = matchLang;
      }
      if (best == null) {
        if (optional) {
          return null;
        } else {
          throw new BundleException("Native-Code: No matching libraries found.");
        }
      }
      renameLibs  = new HashMap();
      HashMap res = new HashMap();
      for (Iterator p = best.iterator(); p.hasNext();) {
        String name = (String)p.next();
        int sp = name.lastIndexOf('/');
        String key = (sp != -1) ? name.substring(sp+1) : name;
        res.put(key, archive.getNativeLibrary(name));
      }
      return res;
    }  else {
      // No native code in this bundle
      return null;
    }
  }

  /**
   * Check if a string exists in a list. Ignore case when comparing.
   */
  private boolean containsIgnoreCase(List l, List l2) {
    for (Iterator i = l.iterator(); i.hasNext(); ) {
      String s = (String)i.next();
      for (Iterator j = l2.iterator(); j.hasNext(); ) {
        if (s.equalsIgnoreCase((String)j.next())) {
          return true;
        }
      }
    }
    return false;
  }


  public Enumeration findResourcesPath(String path) {
    return archive.findResourcesPath(path);
  }


  public String getJarLocation() {
    return archive.getPath();
  }
  
}//class
