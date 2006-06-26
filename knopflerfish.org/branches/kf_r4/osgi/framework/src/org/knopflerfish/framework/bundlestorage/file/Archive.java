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

import org.knopflerfish.framework.*;
import org.osgi.framework.*;
import java.io.*;
import java.net.*;
//import java.security.*;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Vector;
import java.util.jar.*;
import java.util.zip.*;
import java.util.Hashtable;

/**
 * JAR file handling.
 *
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
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

  // Directory where optional bundle files are stored
  // these need not be unpacked locally
  final private static String OSGI_OPT_DIR = "OSGI-OPT/";

  /**
   * Controls if we should try to unpack bundles with sub-jars and
   * native code.
   */
  final private static boolean unpack = new Boolean(System.getProperty("org.knopflerfish.framework.bundlestorage.file.unpack", "true")).booleanValue();

  /**
   * Controls if we should try to unpack bundles with sub-jars and
   * native code.
   */
  final private static boolean alwaysUnpack = new Boolean(System.getProperty("org.knopflerfish.framework.bundlestorage.file.always_unpack", "false")).booleanValue();

  /**
   * Controls if file: URLs should be referenced only, not copied
   * to bundle storage dir
   */
  boolean bReference = new Boolean(System.getProperty("org.knopflerfish.framework.bundlestorage.file.reference", "false")).booleanValue();

  /**
   * File handle for file that contains current archive.
   */
  private FileTree file;

  /**
   * JAR file handle for file that contains current archive.
   */
  private ZipFile jar;

  /**
   * Archive's manifest
   */
  Manifest manifest /*= null*/;

  /**
   * JAR Entry handle for file that contains current archive.
   * If not null, it is a sub jar instead.
   */
  private ZipEntry subJar /*= null*/;
  
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
    this(dir, rev, is, null);
  }

  FileTree refFile = null;

  Archive(File dir, int rev, InputStream is, URL source) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(is, 8192);
    ZipInputStream zi = null;

    // Handle reference: URLs by overriding global flag
    if(source != null && "reference".equals(source.getProtocol())) {
      bReference = true;
    }

    if (alwaysUnpack) {
      zi = new ZipInputStream(bis);
    } else if (unpack) {
      // Is 16000 enough?
      bis.mark(16000);
      JarInputStream ji = new JarInputStream(bis);
      manifest = ji.getManifest();
      if (manifest != null) {
        if (checkManifest()) {
          zi = ji;
        } else {
          bis.reset();
        }
      } else {
        // Manifest probably not first in JAR, should we complain?
        // Now, try to use the jar anyway. Maybe the manifest is there.
        bis.reset();
      }
    }
    file = new FileTree(dir, ARCHIVE + rev);
  
  
    if (zi != null) {
      File f;
      file.mkdirs();
      if (manifest != null) {
        f = new File(file, "META-INF");
        f.mkdir();
        BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(new File(f, "MANIFEST.MF")));
        try {
          manifest.write(o);
        } finally {
          o.close();
        }
      }
      ZipEntry ze;
      while ((ze = zi.getNextEntry()) != null) {
        if (!ze.isDirectory()) {
          if (isSkipped(ze.getName())) {
            // Optional files are not copied to disk
          } else {
            StringTokenizer st = new StringTokenizer(ze.getName(), "/");
            f = new File(file, st.nextToken());
            while (st.hasMoreTokens()) {
              f.mkdir();
              f = new File(f, st.nextToken());
            }
            loadFile(f, zi, true);
          }
        }
      }
      jar = null;
    } else {
      // Only copy to storage when applicable, otherwise
      // use reference
      if (source != null && bReference && "file".equals(source.getProtocol())) {
        refFile = new FileTree(source.getFile());
        jar = new ZipFile(refFile);
      } else {
        loadFile(file, bis, true);
        jar = new ZipFile(file);
      }
    }
    if (manifest == null) {
      manifest = getManifest();
      checkManifest();
    }
  }


  /**
   * Return true if the specified path name should be
   * skipped when unpacking.
   */
  boolean isSkipped(String pathName) {
    return pathName.startsWith(OSGI_OPT_DIR);
  }


  /**
   * Create an Archive based on contents of a saved
   * archive in the specified directory.
   * Take lowest versioned archive and remove rest.
   *
   */
  Archive(File dir, int rev, String location) throws IOException {
    String [] f = dir.list();
    file = null;
    if (rev != -1) {
      file = new FileTree(dir, ARCHIVE + rev);
    } else {
      rev = Integer.MAX_VALUE;
      for (int i = 0; i < f.length; i++) {
        if (f[i].startsWith(ARCHIVE)) {
          try {
            int c = Integer.parseInt(f[i].substring(ARCHIVE.length()));
            if (c < rev) {
              rev = c;
              file = new FileTree(dir, f[i]);
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
            (new FileTree(dir, f[i])).delete();
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
      if(bReference && (location != null)) {
        try {
          URL source = new URL(location);
          if("file".equals(source.getProtocol())) {
            refFile = file = new FileTree(source.getFile());
          }
        } catch (Exception e) {
          throw new IOException("Bad file URL stored in referenced jar in: " +
                                dir.getAbsolutePath() + 
                                ", location=" + location);
        }
      }
      if(file == null || !file.exists()) {
        throw new IOException("No saved jar file found in: " + dir.getAbsolutePath() + ", old location=" + location);
      }
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
          throw new IOException("No such JAR component: " + path);
      }
      // If directory make sure that it ends with "/"
      if (subJar.isDirectory() && !path.endsWith("/")) {
        subJar = jar.getEntry(path + "/");
      }
      file = a.file;
    } else {
      file = findFile(a.file, path);
      if (file.isDirectory()) {
        jar = null;
      } else {
        jar = new ZipFile(file);
      }
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
   * Get a byte array containg the contents of named class file from
   * the archive.
   *
   * @param Class File to get.
   * @return Byte array with contents of class file or null if file doesn't exist.
   * @exception IOException if failed to read jar entry.
   */
  byte[] getClassBytes(String classFile) throws IOException {
    if(bClosed) {
        return null;
    }
    InputFlow cif = getInputFlow(classFile);
    if (cif != null) {
      byte[] bytes;
      if (cif.length >= 0) {
        bytes = new byte[(int)cif.length];
        DataInputStream dis = new DataInputStream(cif.is);
        dis.readFully(bytes);
      } else {
        bytes = new byte[0];
        byte[] tmp = new byte[8192];
        try {
          int len;
          while ((len = cif.is.read(tmp)) > 0) {
            byte[] oldbytes = bytes;
            bytes = new byte[oldbytes.length + len];
            System.arraycopy(oldbytes, 0, bytes, 0, oldbytes.length);
            System.arraycopy(tmp, 0, bytes, oldbytes.length, len);
          }
        } 
        catch (EOFException ignore) {
          // On Pjava we somtimes get a mysterious EOF excpetion,
          // but everything seems okey. (SUN Bug 4040920)
        }
      }
      cif.is.close();
      return bytes;
    } else {
      return null;
    }
  }


  /**
   * Get an InputFlow to named entry inside an Archive.
   *
   * @param component Entry to get reference to.
   * @return InputFlow to entry or null if it doesn't exist.
   */
  InputFlow getInputFlow(String component) {
    if(bClosed) {
      return null;
    }
    if (component.startsWith("/")) {
      throw new RuntimeException("Assert! Path should never start with / here");
    }
    ZipEntry ze;
    try {
      if (jar != null) {
        if (subJar != null) {
          if (subJar.isDirectory()) {
            ze = jar.getEntry(subJar.getName() + component);
          } else {
            JarInputStream ji = new JarInputStream(jar.getInputStream(subJar));
            do {
              ze = ji.getNextJarEntry();
              if (ze == null) {
                ji.close();
                return null;
              }
            } while (!component.equals(ze.getName()));
            return new InputFlow((InputStream)ji, ze.getSize());
          }
        } else {
          ze = jar.getEntry(component);
        }
        return ze != null ? new InputFlow(jar.getInputStream(ze), ze.getSize()) : null;
      } else {
        File f = findFile(file, component);
        return f.exists() ? new InputFlow(new FileInputStream(f), f.length()) : null;
      }
    } catch (IOException ignore) {
      return null;
    }
  }


  //TODO not extensively tested
  Enumeration findResourcesPath(String path) {
    Vector answer = new Vector();
    if (jar != null) { 
      ZipEntry entry; 
      //"normalize" + erroneous path check: be generous
      path.replace('\\', '/');
      if (path.startsWith("/")){
        path =  path.substring(1);   
      }  
      if (!path.endsWith("/")/*in case bad argument*/){
        if (path.length() > 1){
          path += "/";
        }       
      } 
                
      Enumeration entries = jar.entries();
      while (entries.hasMoreElements()){
        entry = (ZipEntry) entries.nextElement();
        String name = entry.getName();
        if (name.startsWith(path)){
          int idx = name.lastIndexOf('/');
          if (entry.isDirectory()){
            idx = name.substring(0, idx).lastIndexOf('/');
          }
          if (idx > 0){
            if (name.substring(0, idx + 1).equals(path)){
              answer.add(name);
            }
          } else if (path.equals("")){
            answer.add(name);
          }
        }       
      }
    } else {
      File f = findFile(file, path);
      if (!f.exists()) {
        return null;
      }
      if (!f.isDirectory()) {
        return null;
      }
      File[] files = f.listFiles();
      int length = files.length;
      for(int i = 0; i < length; i++){
        String filePath = files[i].getPath();
        filePath = filePath.substring(file.getPath().length() + 1);
        filePath.replace(File.separatorChar, '/');
        if(files[i].isDirectory()){
          filePath += "/";
        }
        answer.add(filePath);
      }
    }
    
    if(answer.size() == 0){
      return null;
    }
    return answer.elements();
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
    if(bClosed) {
      return null;
    }
    return new Archive(this, path);
  }


  /**
   * Extract native library from JAR.
   *
   * @param key Name of Jar file to get.
   * @return A string with path to native library.
   */
  String getNativeLibrary(String path) throws IOException {
    if(bClosed) {
      throw new IOException("Archive is closed");
    }
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
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
//XXX - start L-3 modification
      if (!lib.exists() && (lib.getParent() != null)) {
        final String libname = lib.getName();
        File[] list = lib.getParentFile().listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            int pos = name.lastIndexOf(libname);
            return ((pos > 1) && (name.charAt(pos - 1) == '_'));
          }
        });
        if (list.length > 0) {
          list[0].renameTo(lib);
        }
      }
//XXX - end L-3 modification
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

  boolean bClosed = false;

  /**
   * Close archive and all open sub-archives.
   * If close fails it is silently ignored.
   */
  void close() {
    bClosed = true; // Mark as closed to safely handle referenced files
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
   * @return true if bundle needs to be unpacked.
   * @exception IllegalArgumentException if we have a broken manifest.
   */
  private boolean checkManifest() {
    Attributes a = manifest.getMainAttributes();
    Util.parseEntries(Constants.EXPORT_PACKAGE, a.getValue(Constants.EXPORT_PACKAGE),
                      false, true, false);
    Util.parseEntries(Constants.IMPORT_PACKAGE, a.getValue(Constants.IMPORT_PACKAGE),
                      false, true, false);
    Iterator nc = Util.parseEntries(Constants.BUNDLE_NATIVECODE,
                                    a.getValue(Constants.BUNDLE_NATIVECODE),
                                    false, false, false);
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
  private FileTree findFile(File root, String path) {
    return new FileTree(root, path.replace('/', File.separatorChar));
  }

  /**
   * Get the manifest for this archive.
   *
   * @return The manifest for this Archive
   */
  private Manifest getManifest() throws IOException {
    // TBD: Should recognize entry with lower case?
    InputFlow mif = getInputFlow("META-INF/MANIFEST.MF");
    if (mif != null) {
      return new Manifest(mif.is);
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
   * @return File for components cache file.
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
        // On Pjava we sometimes get a mysterious EOF exception,
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

  /**
   * Returns the path to this bundle.
   */
  String getPath() {
    return file.getAbsolutePath();    
  }

  /**
   * InputFlow represents an InputStream with a known length
   */
  class InputFlow {
    final InputStream is;
    final long length;

    InputFlow(InputStream is, long length) {
      this.is = is;
      this.length = length;
    }
  }

}
