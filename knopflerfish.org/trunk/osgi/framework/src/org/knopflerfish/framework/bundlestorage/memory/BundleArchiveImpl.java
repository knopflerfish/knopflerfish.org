/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

  private boolean bFake;

  private int startLevel = -1;
  private boolean bPersistant = false;

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

    bFake = isFake();

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
    archive = new Archive(is);

    bFake = isFake();

    setClassPath();
  }

  boolean isFake() {
    // What the f**k is this? R3 rest case seem to require it!
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
  
  public void setStartLevel(int level) {
    startLevel = level;
  }

  public void setPersistent(boolean b) {
    bPersistant = b;
  }


  public boolean isPersistent() {
    return bPersistant;
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

  //
  // Private methods
  //

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

}
