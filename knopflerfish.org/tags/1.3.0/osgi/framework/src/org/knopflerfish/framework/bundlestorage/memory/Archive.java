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

import org.knopflerfish.framework.*;
import org.osgi.framework.Constants;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * JAR file handling.
 *
 * @author Jan Stein
 * @version $Revision: 1.1.1.1 $
 */
class Archive {

  /**
   * Archives manifest
   */
  Manifest manifest;

  /**
   * JAR Entry handle for file that contains current archive.
   * If not null, it is a sub jar instead.
   */
  protected HashMap /* String -> byte[] */ content;

  /**
   * Create an Archive based on contents of an InputStream,
   * get file object for the stream and use it. Native code
   * is not allowed.
   *
   * @param is Jar file data in an InputStream.
   */
  Archive(InputStream is) throws IOException {
    JarInputStream ji = new JarInputStream(is);
    manifest = ji.getManifest();
    if (manifest == null) {
      throw new IOException("Bundle manifest is missing");
    }
    if (manifest.getMainAttributes().getValue(Constants.BUNDLE_NATIVECODE) != null) {
      throw new IOException("Native code not allowed by memory storage");
    }
    content = loadJarStream(ji);
  }


  /**
   * Create a Sub-Archive based on a path to in an already
   * existing Archive. The new archive is saved in a subdirectory
   * below local copy of the existing Archive.
   *
   * @param a Parent Archive.
   * @param path Path of new Archive inside old Archive.
   * @exception FileNotFoundException if no such Jar file in archive.
   * @exception IOException if failed to read Jar file.
   */
  Archive(Archive a, String path) throws IOException {
    byte [] bs = (byte [])a.content.remove(path);
    if (bs != null) {
      JarInputStream ji = new JarInputStream(new ByteArrayInputStream(bs));
      content = loadJarStream(ji);
    } else {
      throw new FileNotFoundException("No such file: " + path);
    }
  }


  /**
   * Get an attribute from the manifest of the archive.
   *
   * @param key Name of attribute to get.
   * @return A string with result or null if the entry doesn't exists.
   */
  String getAttribute(String key) {
    return manifest.getMainAttributes().getValue(key);
  }
  

  /**
   * Get all attributes from the manifest of the archive.
   *
   * @return All attributes.
   */
  Attributes getAttributes() {
    return manifest.getMainAttributes();
  }
  

  /**
   * Get a byte array containg the contents of named file from
   * the archive.
   *
   * @param component File to get.
   * @return Byte array with contents of file or null if file doesn't exist.
   * @exception IOException if failed to read jar entry.
   */
  byte[] getBytes(String component) throws IOException {
    return (byte [])content.remove(component);
  }


  /**
   * Get an InputStream to named entry inside an Archive.
   *
   * @param component Entry to get reference to.
   * @return InputStream to entry or null if it doesn't exist.
   */
  InputStream getInputStream(String component) {
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    byte [] b = (byte [])content.get(component);
    if (b != null) {
      return new ByteArrayInputStream(b);
    } else {
      return null;
    }
  }


  /**
   * Get an Archive handle to a named Jar file within this archive.
   *
   * @param path Name of Jar file to get.
   * @return An Archive object representing new archive.
   * @exception FileNotFoundException if no such Jar file in archive.
   * @exception IOException if failed to read Jar file.
   */
  Archive getSubArchive(String path) throws IOException {
    return new Archive(this, path);
  }


  //
  // Private methods
  //

  /**
   * Loads all files in a JarInputStream and stores it in a HashMap.
   *
   * @param ji JarInputStream to read from.
   */
  private HashMap loadJarStream(JarInputStream ji) throws IOException {
    HashMap files = new HashMap();
    JarEntry je;
    while ((je = ji.getNextJarEntry()) != null) {
      if (!je.isDirectory()) {
	int len = (int)je.getSize();
	if (len == -1) {
	  len = 8192;
	}
	byte [] b = new byte[len];
	int pos = 0;
	do {
	  if (pos == len) {
	    len *= 2;
	    byte[] oldb = b;
	    b = new byte[len];
	    System.arraycopy(oldb, 0, b, 0, oldb.length);
	  }
	  int n;
	  while ((n = ji.read(b, pos, len - pos)) > 0) {
	    pos += n;
	  }
	} while (ji.available() > 0);
	if (pos != b.length) {
	    byte[] oldb = b;
	    b = new byte[pos];
	    System.arraycopy(oldb, 0, b, 0, pos);
	}
	files.put(je.getName(), b);
      }
    }
    return files;
  }

}
