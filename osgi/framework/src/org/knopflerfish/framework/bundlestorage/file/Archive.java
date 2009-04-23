/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
import java.security.cert.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;


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

  /**
   * File suffix for certificates.
   */
  final private static String CERTS_SUFFIX = ".crt";

  // Directory where optional bundle files are stored
  // these need not be unpacked locally
  final private static String OSGI_OPT_DIR = "OSGI-OPT/";

  /**
   * Controls if we should try to unpack bundles with sub-jars and
   * native code.
   */
  private boolean unpack;

  /**
   * Controls if we should try to unpack bundles with sub-jars and
   * native code.
   */
  private boolean alwaysUnpack;

  /**
   * Controls if file: URLs should be referenced only, not copied
   * to bundle storage dir
   */
  private boolean fileReference;

  /**
   * Controls if we should trust file storage to be secure.
   */
  private boolean trustedStorage;

  /**
   * Controls if we should require signed bundles.
   */
  private boolean allSigned;

  /**
   * File handle for file that contains current archive.
   */
  private FileTree file;

  /**
   * Set to true if above file should be kept at purge()
   */
  private boolean bKeepFile = false;

  /**
   * JAR file handle for file that contains current archive.
   */
  private ZipFile jar;


  final private String location;

  /**
   * Certificates for this archive.
   */
  private Certificate [] certs = null;

  /**
   * Archive's manifest
   */
  AutoManifest manifest /*= null*/;

  /**
   * JAR Entry handle for file that contains current archive.
   * If not null, it is a sub jar instead.
   */
  private ZipEntry subJar /*= null*/;

  /**
   * Is Archive closed.
   */
  private boolean bClosed = false;

  BundleStorageImpl storage;

  void initProps() {
    unpack = new Boolean(storage.framework.props.getProperty("org.knopflerfish.framework.bundlestorage.file.unpack", "true")).booleanValue();
    alwaysUnpack = new Boolean(storage.framework.props.getProperty("org.knopflerfish.framework.bundlestorage.file.always_unpack", "false")).booleanValue();
    fileReference = new Boolean(storage.framework.props.getProperty("org.knopflerfish.framework.bundlestorage.file.reference", "false")).booleanValue();    
    trustedStorage = new Boolean(storage.framework.props.getProperty("org.knopflerfish.framework.bundlestorage.file.trusted", "true")).booleanValue();
    allSigned = new Boolean(storage.framework.props.getProperty("org.knopflerfish.framework.bundlestorage.file.all_signed", "false")).booleanValue();
  }

  /**
   * Create an Archive based on contents of an InputStream,
   * the archive is saved as local copy in the specified
   * directory.
   *
   * @param storage BundleStorageImpl for this archive.
   * @param dir Directory to save data in.
   * @param rev Revision of bundle content (used for updates).
   * @param is Jar file data in an InputStream.
   * @param url URL to use to CodeSource.
   * @param location Location for archive
   */
  Archive(BundleStorageImpl storage, File dir, int rev, InputStream is, URL source, String location) throws IOException
  {
    final boolean doVerify = System.getSecurityManager() != null;
    this.location = location;
    this.storage  = storage;
    initProps();
    Manifest mf = null;

    boolean isDirectory = false;
    final FileTree sourceFile;
    if(isFile(source) || isReference(source)) {
      sourceFile = new FileTree(getFile(source));
      isDirectory = sourceFile.isDirectory();
      if(isDirectory){
        final String manifestPath = sourceFile.getAbsolutePath() + File.separatorChar + "META-INF" + File.separatorChar + "MANIFEST.MF";
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(manifestPath));
        mf = new Manifest(bis);
      }
    } else {
      sourceFile = null;
    }

    BufferedInputStream bis =  null;
    JarInputStream ji = null;
    //Dodge Skelmir-specific problem. Not a great solution, since problem not well understood at this time. Passes KF test suite
    if (mf == null) {
      if (storage.framework.props.getProperty("java.vendor").startsWith("Skelmir")){
        bis = new BufferedInputStream(is, is.available());
      } else {
        bis = new BufferedInputStream(is, 8192);
      }

      if (alwaysUnpack) {
        ji = new JarInputStream(bis, doVerify);
        mf = ji.getManifest();
      } else if (unpack) {
        // Is 100000 enough, Must be big enough to hold the MANIFEST.MF entry
        // NYI, implement a dynamic BufferedInputStream to handle this.
        bis.mark(100000);
        ji = new JarInputStream(bis, doVerify);
        mf = ji.getManifest();
        // Manifest probably not first in JAR, should we complain?
        // Now, try to use the jar anyway. Maybe the manifest is there.
        if (mf == null || !needUnpack(mf.getMainAttributes())) {
          ji = null;
          bis.reset();
        }
      }
      file = new FileTree(dir, ARCHIVE + rev);
      if (ji != null) {
        if (doVerify && mf == null) {
          // TBD? Which exception to use?
          throw new IOException("MANIFEST.MF must be first in archive when using signatures.");
        }
        File f;
        file.mkdirs();
        if (mf != null) {
          f = new File(file, "META-INF");
          f.mkdir();
          BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(new File(f, "MANIFEST.MF")));
          try {
            mf.write(o);
          } finally {
            o.close();
          }
        }
        JarEntry je;
        boolean foundUnsigned = false;
        while ((je = ji.getNextJarEntry()) != null) {
          if (!je.isDirectory()) {
            String name = je.getName();
            if (name.startsWith(OSGI_OPT_DIR)) {
              // Optional files are not copied to disk
              // TBD? Check if it is verified
              // NYI! Handle local permissions
            } else {
              StringTokenizer st = new StringTokenizer(name, "/");
              f = new File(file, st.nextToken());
              while (st.hasMoreTokens()) {
                f.mkdir();
                f = new File(f, st.nextToken());
              }
              loadFile(f, ji);
            }
            if (doVerify) {
              if (!name.startsWith("META-INF/")) {
                Certificate [] c = je.getCertificates();
                if (c != null) {
                  if (certs != null) {
                    certs = intersectCerts(certs, c);
                  } else {
                    certs = c;
                  }
                } else {
                  foundUnsigned = true;
                }
              }
            } else {
              foundUnsigned = true;
            }
          }
        }
        certs = checkCertificates(certs, foundUnsigned);
        jar = null;
      }
      saveCertificates();
    }
    if (ji == null) {
      // Only copy to storage when applicable, otherwise
      // use reference
      if (isReference(source)) {
        file = new FileTree(getFile(source));

        // this tells the purge() method not to remove the original
        bKeepFile = true;
      } else {
        if(isDirectory){
          // if directory, copy the directory to bundle storage
          file = new FileTree(dir, ARCHIVE + rev);
          file.mkdirs();
          loadFileTree(file, sourceFile);
        } else{
          loadFile(file, bis);
        }
      }
      if(! isDirectory){
        if (doVerify) {
          // TBD, should we allow signed to be reference files?
          mf = processSignedJar(file);
        }
        jar = new ZipFile(file);
      }
    }
    if (mf != null) {
      manifest = new AutoManifest(storage.framework, mf, location);
    } else {
      manifest = getManifest();
    }
    checkManifest();

    handleAutoManifest();
  }

  /**
   * Get the file path from an URL, handling the case where it's
   * a reference:file: URL
   */
  String getFile(URL source) {
    String file = source.getFile();
    if(file.startsWith("file:")) {
      return file.substring(5);
    } else {
      return file;
    }
  }

  boolean isFile(URL source) {
    return source != null && "file".equals(source.getProtocol());
  }

  /**
   * Check if an URL is a reference: URL or if we have global references on all file: URLs
   */
  boolean isReference(URL source) {
    return (source != null) && 
      ("reference".equals(source.getProtocol()) 
       || (fileReference && isFile(source)));
  }
  
  /**
   * Create an Archive based on contents of a saved
   * archive in the specified directory.
   * Take lowest versioned archive and remove rest.
   *
   */
  Archive(BundleStorageImpl storage, File dir, int rev, String location) throws IOException {
    final boolean doVerify = System.getSecurityManager() != null;
    this.location = location;
    this.storage  = storage;
    initProps();
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
    if (file == null) {
      if (location != null) {
        try {
          URL url = new URL(location);
          file = new FileTree(getFile(url));
        } catch (Exception e) {
          throw new IOException("Bad file URL stored in referenced jar in: " +
                                dir.getAbsolutePath() +
                                ", location=" + location);
        }
      }
      if (file == null || !file.exists()) {
        throw new IOException("No saved jar file found in: " + dir.getAbsolutePath() + ", old location=" + location);
      }
    }

    if (file.isDirectory()) {
      jar = null;
    } else {
      jar = new ZipFile(file);
    }
    if (doVerify) {
      if (jar == null) {
        loadCertificates();
      } else {
        manifest = new AutoManifest(storage.framework, processSignedJar(file), location);
      }
    }
    if (manifest == null) {
      manifest = getManifest();
    }
    handleAutoManifest();
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
    this.location = a.location;
    this.storage  = a.storage;
    initProps();
    if (a.jar != null) {
      jar = a.jar;
      // Try a directory first, make sure that path ends with "/"
      if (!path.endsWith("/")) {
        path += "/";
      }
      subJar = jar.getEntry(path);
      if (subJar == null) {
        subJar = jar.getEntry(path.substring(0,path.length()-1));
      }
      if (subJar == null) {
        throw new IOException("No such JAR component: " + path);
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
            if (component.equals("")) {
              // Return a stream to the entire Jar.
              return new InputFlow(jar.getInputStream(subJar), subJar.getSize());
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
          }
        } else {
          if (component.equals("")) {
            // Return a stream to the entire Jar.
            File f = new File(jar.getName());
            return new InputFlow(new FileInputStream(f), f.length() );
          } else {
            ze = jar.getEntry(component);
          }
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
        filePath = filePath.replace(File.separatorChar, '/');
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
            loadFile(lib, is);
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
    // Remove archive if not  flagged as keep
    if(!bKeepFile) {
      file.delete();
    }

    // Remove any cached sub files
    getSubFileTree(this).delete();

    // Remove any saved certificates.
    removeCertificates();
  }


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


  /**
   * Returns the path to this bundle.
   */
  String getPath() {
    return file.getAbsolutePath();
  }


  /**
   * Return certificates for signed bundle, otherwise null.
   *
   * @return An array of certificates or null.
   */
  Certificate [] getCertificates() {
    return certs;
  }


  //
  // Private methods
  //

  /**
   * Check that we have a valid manifest.
   *
   * @exception IllegalArgumentException if we have a broken manifest.
   */
  private void checkManifest() {
    Attributes a = manifest.getMainAttributes();
    Util.parseEntries(Constants.EXPORT_PACKAGE, a.getValue(Constants.EXPORT_PACKAGE),
                      false, true, false);
    Util.parseEntries(Constants.IMPORT_PACKAGE, a.getValue(Constants.IMPORT_PACKAGE),
                      false, true, false);
    // NYI, more checks?
  }


  /**
   * Check if we need to unpack bundle,
   * i.e has native code file or subjars.
   *
   * @return true if bundle needs to be unpacked.
   * @exception IllegalArgumentException if we have a broken manifest.
   */
  private boolean needUnpack(Attributes a) {
    Iterator nc = Util.parseEntries(Constants.BUNDLE_NATIVECODE,
                                    a.getValue(Constants.BUNDLE_NATIVECODE),
                                    false, false, false);
    String bc = a.getValue(Constants.BUNDLE_CLASSPATH);
    return (bc != null && !bc.trim().equals(".")) || nc.hasNext();
  }


  /**
   * Handle automanifest stuff.
   * Archive manifest must be set.
   */
  private void handleAutoManifest() throws IOException {
    if (manifest.isAuto()) {
      if (jar != null) {
        manifest.addZipFile(jar);
      } else if (file != null && file.isDirectory()) {
        manifest.addFile(file.getAbsolutePath(), file);
      }
    }
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
  private AutoManifest getManifest() throws IOException {
    // TBD: Should recognize entry with lower case?
    InputFlow mif = getInputFlow("META-INF/MANIFEST.MF");
    if (mif != null) {
      return new AutoManifest(storage.framework, new Manifest(mif.is), location);
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
  private void loadFile(File output, InputStream is) throws IOException {
    OutputStream os = null;
    try {
      os = new FileOutputStream(output);
      byte[] buf = new byte[8192];
      int n;
      try {
        while ((n = is.read(buf)) >= 0) {
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

  private void loadFileTree(File output, final FileTree sourceFile) throws IOException {
    try {
      sourceFile.copyTo(output);
    }
    catch (IOException e) {
      output.delete();
      throw e;
    }
  }



  /**
   *
   */
  private Certificate [] intersectCerts(Certificate [] a, Certificate [] b) {
    int len = 0;
    for (int i = 0; i < a.length; i++) {
      boolean ok = false;
      for (int j = 0; j < b.length; j++) {
        if (a[i] == b[j]) {
          ok = true;
          break;
        }
      }
      if (ok) {
        len++;
      } else {
        certs[i] = null;
      }
    }
    if (len != a.length) {
      Certificate [] nc = new Certificate[len];
      int j = 0;
      for (int i = 0; i < a.length; i++) {
        if (a[i] != null) {
          nc[j++] = a[i];
        }
      }
      return nc;
    } else {
      return a;
    }
  }


  /**
   * Go through jar file and check signatures.
   */
  private Manifest processSignedJar(File file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    BufferedInputStream bis = new BufferedInputStream(fis, 8192);
    JarInputStream ji = new JarInputStream(bis);
    JarEntry je;
    boolean foundUnsigned = false;
    Manifest manifest = ji.getManifest();

    while ((je = ji.getNextJarEntry()) != null) {
      if (!je.isDirectory()) {
        String name = je.getName();
        if (!name.startsWith("META-INF/")) {
          ji.closeEntry();
          Certificate [] c = je.getCertificates();
          if (c != null) {
            if (certs != null) {
              certs = intersectCerts(certs, c);
            } else {
              certs = c;
            }
          } else {
            foundUnsigned = true;
          }
        }
      }
    }
    checkCertificates(certs, foundUnsigned);
    return manifest;
  }


  /**
   * Check that certificates are valid:
   * 
   */
  private Certificate [] checkCertificates(Certificate [] certs, boolean foundUnsigned)
    throws IOException {
    Certificate [] res = null;
    if (certs != null) {
      if (foundUnsigned) {
        // NYI! Log, ("All entry must be signed in a signed bundle.");
      } else if (certs.length == 0) { 
        // NYI! Log ("All entry must be signed by a common certificate.");
      } else {
        int ok = 0;
        for (int i = 0; i < certs.length; i++) {
          if (certs[i] instanceof X509Certificate) {
            try {
              ((X509Certificate)certs[i]).checkValidity();
              // NYI! Check cert chain
              ok++;
            } catch (Exception ignore) {
              certs[i] = null;
            }
          } else {
            // Certificate type not handled, remove.
            certs[i] = null;
          }
        }
        if (ok == certs.length) {
          res = certs;
        } else if (ok > 0) {
          res = new Certificate[ok];
          int j = 0;
          for (int i = 0; i < certs.length; i++) {
            if (certs[i] != null) {
              res[j++] = certs[i];
            }
          }
        }
      }
    }
    if (res == null && allSigned) {
      // TBD? Which exception to use?
      throw new IOException("Install requires signed bundle.");
    }
    return res;
  }


  /**
   * 
   */
  public void saveCertificates() throws IOException {
    File f = new File(getPath() + CERTS_SUFFIX);
    if (certs != null) {
      try {
        FileOutputStream fos = new FileOutputStream(f);
        for (int i = 0; i < certs.length; i++) {
          fos.write(certs[i].getEncoded());
        }
        fos.close();
      } catch (CertificateEncodingException e) {
        // NYI! Log or fail
      }
    }
  }


  /**
   * 
   */
  private void loadCertificates() throws IOException {
    File f = new File(getPath() + CERTS_SUFFIX);
    if (f.canRead()) {
      try {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        FileInputStream fis = new FileInputStream(f);
        Collection c = cf.generateCertificates(fis);
        if (c.size() > 0) {
          certs = new Certificate[c.size()];
          c.toArray(certs);
        }
      } catch (CertificateException ioe) {
        // NYI! Log or fail
      }
    }
    // NYI! load certificates from both trusted and untrusted storage.
  }


  /**
   * 
   */
  public void removeCertificates() {
    File f = new File(getPath() + CERTS_SUFFIX);
    f.delete();
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
