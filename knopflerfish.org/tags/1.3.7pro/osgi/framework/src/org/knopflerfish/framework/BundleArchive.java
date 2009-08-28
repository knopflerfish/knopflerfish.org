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

package org.knopflerfish.framework;

import java.io.InputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Vector;

/**
 * Interface for managing bundle data.
 *
 * @author Jan Stein
 */
public interface BundleArchive {

  /**
   * Get an attribute from the manifest of a bundle.
   *
   * @param key Name of attribute to get.
   * @return A string with result or null if the entry doesn't exists.
   */
  String getAttribute(String key);

  /**
   * Get all attributes from the manifest of a bundle.
   *
   * @return All attributes, null if bundle doesn't exists.
   */
  Dictionary getAttributes();

  /**
   * Get bundle identifier for this bundle archive.
   *
   * @return Bundle identifier.
   */
  long getBundleId();

  /**
   * Get bundle location for this bundle archive.
   *
   * @return Bundle location.
   */
  String getBundleLocation();

  /**
   * Get stored bundle start level.
   */
  int getStartLevel(); 

  /**
   * Set stored bundle start level.
   */
  void setStartLevel(int level) throws IOException; 

  void setPersistent(boolean b)  throws IOException;

  boolean isPersistent();

  /**
   * Get a byte array containg the contents of named file from a bundle
   * archive.
   *
   * @param component File to get.
   * @return Byte array with contents of file or null if file doesn't exist.
   * @exception IOException if failed to read jar entry.
   */
  byte[] getClassBytes(String component) throws IOException;



  /**
   * Check if named entry exist in bundles archive.
   * Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @return Vector or entry numbers, or null if it doesn't exist.
   */
  Vector componentExists(String component);


  /**
   * Same as getInputStream(component, -1)
   */
  InputStream getInputStream(String component);

  /**
   * Get an specific InputStream to named entry inside a bundle.
   * Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @param ix index of jar. 0 means the top level. -1 means any archive.
   * @return InputStream to entry or null if it doesn't exist.
   */
  InputStream getInputStream(String component, int ix);

  /**
   * Extract native library from JAR.
   *
   * @param component Name of Jar file to get.
   * @return A string with path to native library.
   */
  String getNativeLibrary(String component);

  /**
   * Get state of start on launch flag.
   *
   * @return Boolean value for start on launch flag.
   */
  boolean getStartOnLaunchFlag();

  /**
   * Set state of start on launch flag.
   *
   * @param value Boolean value for start on launch flag.
   */
  void setStartOnLaunchFlag(boolean value) throws IOException;

  /**
   * Remove bundle archive from persistent storage.
   */
  void purge();

  /**
   * Close archive and all its open files.
   */
  void close();

}
