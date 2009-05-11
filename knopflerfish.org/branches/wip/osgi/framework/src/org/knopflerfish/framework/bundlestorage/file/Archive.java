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

package org.knopflerfish.framework.bundlestorage.file;

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
   * Base for filename used to store copy of archive.
   */
  final private static String ARCHIVE = "jar";

  /**
   * Directory base name to use for sub-archives.
   */
  final private static String SUBDIR = "sub";

  /**
   * Controls if we should try to unpack bundles with sub-jars and
   * native code.
   */
  final private static boolean unpack = new Boolean(System.getProperty("org.knopflerfish.framework.bundlestorage.file.unpack", "true")).booleanValue();

  /**
   * File handle for file that contains current archive.
   */
  private File file;

  /**
   * JAR file handle for file that contains current archive.
   */
  private ZipFile jar;

  /**
   * Archives manifest
   */
  Manifest manifest = null;

  /**
   * JAR Entry handle for file that contains current archive.
   * If not null, it is a sub jar instead.
   */
  private ZipEntry subJar = null;

  /**
   * Create an Archive based on contents of an InputStream,
   * the archive is saved as local copy in the specified
   * directory.
   *
   * @param dir Directory to save data in.
   * @param rev Revision of bundle content (used for updates).
   * @param is Jar file data in an InputStream.
   * @param url URL to use to CodeSource.
   */
  Archive(File dir, int rev, InputStream is) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(is, 8192);
    JarInputStream ji = null;
    boolean doUnpack = false;
    if (unpack) {
      bis.mark(8192);
      ji = new JarInputStream(bis);
      manifest = ji.getManifest();
      if (manifest != null) {
	if (checkManifest()) {
	  doUnpack = true;
	} else {
	  try {
	    bis.reset();
	  } catch (IOException fail) {
	    doUnpack = true;
	  }
	}
      } else {
	// The manifest is probably not first in the file. We do not
	// unpack to minimize disk footprint. Should we warn?
	try {
	  bis.reset();
	} catch (IOException fail) {
	  doUnpack = true;
	}
      }
    }
    file = new File(dir, ARCHIVE + rev);
    if (doUnpack) {
      File f = new File(file, "META-INF");
      f.mkdirs();
      BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(new File(f, "MANIFEST.MF")));
      try {
	manifest.write(o);
      } finally {
	o.close();
      }
      ZipEntry ze;
      // NYI! Filter out unused files
      while ((ze = ji.getNextJarEntry()) != null) {
	if (!ze.isDirectory()) {
	  StringTokenizer st = new StringTokenizer(ze.getName(), "/");
	  f = new File(file, st.nextToken());
	  while (st.hasMoreTokens()) {
	    f.mkdir();
	    f = new File(f, st.nextToken());
	  }
	  loadFile(f, ji, true);
	}
      }
      jar = null;
    } else {
      loadFile(file, bis, true);
      jar = new ZipFile(file);
      if (manifest == null) {
	manifest = getManifest();
	checkManifest();
      }
    }
  }


  /**
   * Create an Archive based on contents of a saved
   * archive in the specified directory.
   * Take lowest versioned archive and remove rest.
   *
   * @param dir Directory with saved Archive.
   * @param url URL to use to CodeSource.
   */
  Archive(File dir, int rev) throws IOException {
    String [] f = dir.list();
    file = null;
    if (rev != -1) {
      file = new File(dir, ARCHIVE + rev);
    } else {
      rev = Integer.MAX_VALUE;
      for (int i = 0; i < f.length; i++) {
	if (f[i].startsWith(ARCHIVE)) {
	  try {
	    int c = Integer.parseInt(f[i].substring(ARCHIVE.length()));
	    if (c < rev) {
	      rev = c;
	      file = new File(dir, f[i]);
	    }
	  } catch (NumberFormatException ignore) { }
	}
      }
    }
    for (int i = 0; i < f.length; i++) {
      if (f[i].startsWith(ARCHIVE)) {
	try {
	  int c = Integer.parseInt(f[i].substring(ARCHIVE.length()));
	  if (c != rev) {
	    (new File(dir, f[i])).delete();
	  }
	} catch (NumberFormatException ignore) { }
      }
      if (f[i].startsWith(SUBDIR)) {
	try {
	  int c = Integer.parseInt(f[i].substring(SUBDIR.length()));
	  if (c != rev) {
	    (new FileTree(dir, f[i])).delete();
	  }
	} catch (NumberFormatException ignore) { }
      }
    }
    if (file == null || !file.exists()) {
      throw new IOException("No saved jar file found in: " + dir.getAbsolutePath());
    }
    if (file.isDirectory()) {
      jar = null;
    } else {
      jar = new ZipFile(file);
    }
    manifest = getManifest();
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
    if (a.jar != null) {
      jar = a.jar;
      subJar = jar.getEntry(path);
      if (subJar == null) {
	throw new IOException("No such JAR componenet: " + path);
      }
      file = a.file;
    } else {
      file = findFile(a.file, path);
      jar = new ZipFile(file);
    }
  }


  /**
   * Show file name for archive, if zip show if it is sub archive.
   *
   * @return A string with result.
   */
  public String toString() {
    if (subJar != null) {
      return file.getAbsolutePath() + "(" + subJar.getName() + ")";
    } else {
      return file.getAbsolutePath();
    }
  }


  /**
   * Get revision number this archive.
   *
   * @return Archive revision number
   */
  int getRevision() {
    try {
      return Integer.parseInt(file.getName().substring(ARCHIVE.length()));
    } catch (NumberFormatException ignore) {
      // assert?
      return -1;
    }
  }


  /**
   * Get an attribute from the manifest of the archive.
   *
   * @param key Name of attribute to get.
   * @return A string with result or null if the entry doesn't exists.
   */
  String getAttribute(String key) {
    Attributes a = manifest.getMainAttributes();
    if (a != null) {
      return a.getValue(key);
    }
    return null;
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
    ZipEntry ze;
    InputStream is;
    int len;
    if (jar != null) {
      if (subJar != null) {
	JarInputStream ji = new JarInputStream(jar.getInputStream(subJar));
	do {
	  ze = ji.getNextJarEntry();
	  if (ze == null) {
	    ji.close();
	    return null;
	  }
	} while (!component.equals(ze.getName()));
	is = (InputStream)ji;
      } else {
	ze = jar.getEntry(component);
	if (ze == null) {
	  return null;
	}
	is = jar.getInputStream(ze);
      }
      len = (int)ze.getSize();
    } else {
      File f = findFile(file, component);
      if (f.exists()) {
	is = new FileInputStream(f);
	len = is.available();
      } else {
	return null;
      }
    }
    byte[] bytes;
    if (len >= 0) {
      bytes = new byte[len];
      DataInputStream dis = new DataInputStream(is);
      dis.readFully(bytes);
    } else {
      bytes = new byte[0];
      byte[] tmp = new byte[8192];
      try {
	while ((len = is.read(tmp)) > 0) {
	  byte[] oldbytes = bytes;
	  bytes = new byte[oldbytes.length + len];
	  System.arraycopy(oldbytes, 0, bytes, 0, oldbytes.length);
	  System.arraycopy(tmp, 0, bytes, oldbytes.length, len);
	}
      } catch (EOFException ignore) {
	// On Pjava we somtimes get a mysterious EOF excpetion,
	// but everything seems okey. (SUN Bug 4040920)
      }
    }
    is.close();
    return bytes;
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
    ZipEntry ze;
    InputStream is;
    try {
      if (jar != null) {
	if (subJar != null) {
	  JarInputStream ji = new JarInputStream(jar.getInputStream(subJar));
	  do {
	    ze = ji.getNextJarEntry();
	    if (ze == null) {
	      ji.close();
	      return null;
	    }
	  } while (!component.equals(ze.getName()));
	  is = (InputStream)ji;
	} else {
	  ze = jar.getEntry(component);
	  is = (ze != null) ? jar.getInputStream(ze) : null;
	}
      } else {
	File f = findFile(file, component);
	is = f.exists() ? new FileInputStream(f) : null;
      }
      return is;
    } catch (IOException ignore) {
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


  /**
   * Extract native library from JAR.
   *
   * @param key Name of Jar file to get.
   * @return A string with path to native library.
   */
  String getNativeLibrary(String path) throws IOException {
    File lib;
    if (jar != null) {
      lib = getSubFile(this, path);
      if (!lib.exists()) {
	(new File(lib.getParent())).mkdirs();
	ZipEntry ze = jar.getEntry(path);
	if (ze != null) {
	  InputStream is = jar.getInputStream(ze);
	  try {
	    loadFile(lib, is, false);
	  } finally {
	    is.close();
	  }
	} else {
	  throw new FileNotFoundException("No such sub-archive: " + path);
	}
      }
    } else {
      lib = findFile(file, path);
    }
    return lib.getAbsolutePath();
  }


  /**
   * Remove archive and any unpacked sub-archives.
   */
  void purge() {
    close();
    // Remove archive
    file.delete();
    // Remove any cached sub files
    getSubFileTree(this).delete();
  }


  /**
   * Close archive and all open sub-archives.
   * If close fails it is silently ignored.
   */
  void close() {
    if (subJar == null && jar != null) {
      try {
	jar.close();
      } catch (IOException ignore) {}
    }
  }

  //
  // Private methods
  //

  /**
   * Check that we have a valid manifest for the bundle
   * and if we need to unpack bundle for fast performance,
   * i.e has native code file or subjars.
   *
   * @return true if bundle needs to be unpack.
   * @exception IllegalArgumentException if we have a broken manifest.
   */
  private boolean checkManifest() {
    Attributes a = manifest.getMainAttributes();
    Util.parseEntries(Constants.EXPORT_PACKAGE, a.getValue(Constants.EXPORT_PACKAGE), true);
    Util.parseEntries(Constants.IMPORT_PACKAGE, a.getValue(Constants.IMPORT_PACKAGE), true);
    Iterator nc = Util.parseEntries(Constants.BUNDLE_NATIVECODE, a.getValue(Constants.BUNDLE_NATIVECODE), false);
    String bc = a.getValue(Constants.BUNDLE_CLASSPATH);
    return (bc != null && !bc.trim().equals(".")) || nc.hasNext();
  }


  /**
   * Get file handle for file inside a directory structure.
   * The path for the file is always specified with a '/'
   * separated path.
   *
   * @param root Directory structure to search.
   * @param path Path to file to find.
   * @return The File object for file <code>path</code>.
   */
  private File findFile(File root, String path) {
    return new File(root, path.replace('/', File.separatorChar));
  }

  /**
   * Get the manifest for this archive.
   *
   * @return The manifest for this Archive
   */
  private Manifest getManifest() throws IOException {
    // TBD: Should recognize entry with lower case?
    InputStream is = getInputStream("META-INF/MANIFEST.MF");
    if (is != null) {
      return new Manifest(is);
    } else {
      throw new IOException("Manifest is missing");
    }
  }


  /**
   * Get dir for unpacked components.
   *
   * @param archive Archive which contains the components.
   * @return FileTree for archives component cache directory.
   */
  private FileTree getSubFileTree(Archive archive) {
    return new FileTree(archive.file.getParent(),
			SUBDIR + archive.file.getName().substring(ARCHIVE.length()));
  }


  /**
   * Get file for an unpacked component.
   *
   * @param archive Archive which contains the component.
   * @param path Name of the component to get.
   * @return File for componets cache file.
   */
  private File getSubFile(Archive archive, String path) {
    return new File(getSubFileTree(archive), path.replace('/', '-'));
  }


  /**
   * Loads a file from an InputStream and stores it in a file.
   *
   * @param output File to save data in.
   * @param is InputStream to read from.
   */
  private void loadFile(File output, InputStream is, boolean verify) throws IOException {
    OutputStream os = null;
    // NYI! Verify
    try {
      os = new FileOutputStream(output);
      byte[] buf = new byte[8192];
      int n;
      try {
	while ((n = is.read(buf)) > 0) {
	  os.write(buf, 0, n);
	}
      } catch (EOFException ignore) {
	// On Pjava we somtimes get a mysterious EOF excpetion,
	// but everything seems okey. (SUN Bug 4040920)
      }
    } catch (IOException e) {
      output.delete();
      throw e;
    } finally {
      if (os != null) {
	os.close();
      }
    }
  }

}