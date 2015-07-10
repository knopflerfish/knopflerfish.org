/*
 * Copyright (c) 2009-2015, KNOPFLERFISH project
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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;

/**
 * Interface for managing bundle contents.
 *
 * @author Jan Stein
 */
public interface FileArchive {

  /**
   * Get a byte array containing the contents of named file from a bundle archive.
   *
   * @param component File to get.
   * @return Byte array with contents of file or null if file doesn't exist.
   * @exception IOException if failed to read jar entry.
   */
  byte[] getClassBytes(String component) throws IOException;


  /**
   * Load class bytes into specified class loader for named file from a bundle archive.
   *
   * @param component File to get.
   * @param classloader Class loader to use for component.
   * @return Class for loaded bytes or null if no fil doesn't exist.
   * @exception IOException if failed to read jar entry.
   */
  Class loadClassBytes(String path, ClassLoader classloader);

  /**
   * Get a BundleResourceStream to named entry inside a bundle. Leading '/' is
   * stripped.
   *
   * @param component Entry to get reference to.
   * @param ix index of sub archives. A postive number is the classpath entry
   *          index. 0 means look in the main bundle.
   * @return BundleResourceStream to entry or null if it doesn't exist.
   */
  BundleResourceStream getBundleResourceStream(String component);


  /**
   * Returns an Enumeration of all the paths (<code>String</code> objects) to
   * entries within the bundle whose longest sub-path matches the supplied path
   * argument.
   *
   * @param name
   * @return
   */
  Enumeration<String> findResourcesPath(String path);


  /**
   * Check for native library in archive.
   *
   * @param path Name of native code file to get.
   * @return If native library exist return libname, otherwise null.
   */
  String checkNativeLibrary(String path);


  /**
   * Get native code library filename.
   *
   * @param libNameKey Key for native lib to get.
   * @return A string with the path to the native library.
   */
  String getNativeLibrary(String libNameKey);


  /**
   * Get BundleGeneration object for this archive.
   */
  BundleGeneration getBundleGeneration();


  /**
   * Get sub-archive id for this archive.
   */
  int getSubId();


  /**
   * Check if a file exists (or dir, if dirs is true).
   *
   * @param path
   * @param dirs
   * @return
   */
  boolean exists(String path, boolean dirs);


  /**
   * List all entries for named directory.
   *
   * @param path
   * @return
   */
  Set<String> listDir(String path);

}
