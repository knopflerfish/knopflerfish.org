/*
 * Copyright (c) 2003-2006, Knopflerfish project
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

package org.knopflerfish.framework.bundlestorage.memory;

import org.osgi.framework.*;
import org.knopflerfish.framework.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;



/**
 * Interface for managing bundle data.
 *
 * @author Jan Stein
 * @author Philippe Laporte
 * @version $Revision: 1.2 $
 */
class BundleArchiveImpl implements BundleArchive
{

  private Archive archive;

  private long id;

  private String location;

  private boolean startOnLaunch;

  private BundleStorageImpl storage;

  private Archive [] archives;

  private int startLevel = -1;
  private boolean bPersistent = false;
  private long lastModified;

  private ArrayList failedPath = null;

  /**
   * Construct new bundle archive.
   *
   */
  BundleArchiveImpl(BundleStorageImpl bundleStorage, 
                    InputStream       is,
                    String            bundleLocation, 
                    long bundleId)
    throws Exception
  {
    archive       = new Archive(is);
    storage       = bundleStorage;
    id            = bundleId;
    location      = bundleLocation;
    startOnLaunch = false;
    setClassPath();
  }


  /**
   * Construct new bundle archive in an existing bundle archive.
   *
   */
  BundleArchiveImpl(BundleArchiveImpl old, InputStream is)
    throws Exception
  {
    location = old.location;
    storage = old.storage;
    id = old.id;
    startOnLaunch = old.startOnLaunch;
    bPersistent = old.bPersistent;
    archive = new Archive(is);
    setClassPath();
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
    InputStream is = archive.getInputStream(localeFile);
    if (is != null) {
      Properties l = new Properties();
      try {
        l.load(is);
      } catch (IOException _ignore) { }
      try {
        is.close();
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

  
  public void setStartLevel(int level) {
    startLevel = level;
  }


  public void setPersistent(boolean b) {
    bPersistent = b;
  }


  public boolean isPersistent() {
    return bPersistent;
  }

  
  public long getLastModified() {
    return lastModified;
  }


  public void setLastModified(long timemillisecs) throws IOException{
          lastModified = timemillisecs;
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
   * Check if named entry exist in bundles classpath.
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
      InputStream is = archives[i].getInputStream(component);
      if (is != null) {
        if(v == null) {
          v = new Vector();
        }
        v.addElement(new Integer(i));
        try {
            is.close();
        } catch (IOException ignore) { }
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

    if(ix == -1) {
      return archive.getInputStream(component);
    } else {
      return archives[ix].getInputStream(component);
    }
  }


  /**
   * Get native library from JAR.
   *
   * @param libName Name of Jar file to get.
   * @return A string with path to native library.
   */
  public String getNativeLibrary(String libName) {
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
    }
  }


  /**
   * Remove bundle archive from persistent storage. If we removed
   * the active revision also remove bundle status files.
   */
  public void purge() {
    storage.removeArchive(this);
  }


  /**
   * Close archive for further access. It should still be possible
   * to get attributes.
   */
  public void close() {
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

  private void setClassPath() throws IOException {
    String bcp = getAttribute(Constants.BUNDLE_CLASSPATH);
    
    if (bcp != null) {
      ArrayList a = new ArrayList();
      StringTokenizer st = new StringTokenizer(bcp, ",");
      while (st.hasMoreTokens()) {
        String path = st.nextToken().trim();
        if (".".equals(path)) {
                  a.add(archive);
          } 
          else if (path.endsWith(".jar")){
            try {
              a.add(archive.getSubArchive(path));
            } catch (IOException ioe) {
              if (failedPath == null) {
                failedPath = new ArrayList(1);
              }
              failedPath.add(path);
            }
          }
          else{
            if(archive.subDirs == null){
              archive.subDirs = new ArrayList(1);
            }
          // NYI Check that it exists!
            archive.subDirs.add(path);
          }
      }
      archives = (Archive [])a.toArray(new Archive[a.size()]);
    } else {
      archives = new Archive[] { archive };
    }
  }

  
  public Enumeration findResourcesPath(String path) {
    return archive.findResourcesPath(path);
  }

  public String getJarLocation() {
    return null;
  }
}
