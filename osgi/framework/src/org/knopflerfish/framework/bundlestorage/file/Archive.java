/*
 * Copyright (c) 2003-2016, KNOPFLERFISH project
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.knopflerfish.framework.AutoManifest;
import org.knopflerfish.framework.BundleGeneration;
import org.knopflerfish.framework.BundleResourceStream;
import org.knopflerfish.framework.FileArchive;
import org.knopflerfish.framework.FileTree;
import org.knopflerfish.framework.Util;
import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.Constants;

/**
 * JAR file handling.
 *
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 * @author Gunnar Ekolin
 */
public class Archive implements FileArchive {

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

  /**
   * Directory where JAR meta data are stored.
   */
  final private static String META_INF_DIR = "META-INF/";

  /**
   * Directory where optional bundle files are stored these need not be unpacked
   * locally.
   */
  final private static String OSGI_OPT_DIR = "OSGI-OPT/";

  /**
   * FileTree handle for bundle storage directory.
   */
  protected FileTree bundleDir;

  /**
   * File handle for file that contains current archive.
   */
  protected FileTree file;

  /**
   * Set to true if above file is a reference outside framework storage.
   */
  private boolean fileIsReference = false;

  /**
   * JAR file handle for file that contains current archive.
   */
  protected ZipFile jar = null;

  /**
   * Certificates for this archive.
   */
  private Certificate[] certs = null;

  /**
   * Archive's manifest
   */
  Manifest manifest /* = null */;

  /**
   * JAR Entry handle for file that contains current archive. If not null, it is
   * a sub jar instead.
   */
  private ZipEntry subJar /* = null */;

  /**
   * Is Archive closed.
   */
  private boolean bClosed = false;

  /**
   *
   */
  private Map<String, String> nativeLibs;

  /**
   *
   */
  private Map<String, String> renameLibs;

  /**
   *
   */
  final private BundleArchiveImpl ba;

  /**
   *
   */
  protected int subId;

  protected int revision;



  /**
   * Create an Archive based on contents of an InputStream, the archive is saved
   * as local copy in the specified directory.
   *
   * @param ba BundleArchiveImpl for this archive.
   * @param dir Directory to save data in.
   * @param rev Revision of bundle content (used for updates).
   */
  protected Archive(BundleArchiveImpl ba, FileTree dir, int rev) {
    this.ba = ba;
    this.bundleDir = dir;
    this.revision = rev;
  }


  /**
   * Create an Archive based on contents of an InputStream, the archive is saved
   * as local copy in the specified directory.
   *
   * @param is Jar file data in an InputStream.
   * @param url URL to use to CodeSource.
   */
  protected void downloadArchive(InputStream is, URL source)
      throws IOException {
    subId = 0;

    boolean isDirectory = false;
    final FileTree sourceFile;
    final FileTree bsFile = new FileTree(bundleDir, ARCHIVE + revision);

    if (isReference(source)) {
      fileIsReference = true;
      sourceFile = new FileTree(getFile(source));
      file = sourceFile;
    } else {
      sourceFile = isFile(source) ? new FileTree(getFile(source)) : null;
      file = bsFile;
    }
    if (sourceFile != null) {
      isDirectory = sourceFile.isDirectory();
      if (isDirectory) {
        final File mfd = new File(sourceFile.getAbsolutePath(), META_INF_DIR);
        final File mf = new File(mfd, "MANIFEST.MF");
        final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(mf));
        try {
          manifest = new Manifest(bis);
        } finally {
          bis.close();
        }
      }
    }

    BufferedInputStream bis = null;
    boolean doUnpack = false;
    JarInputStream ji = null;
    if (manifest == null) {
      bis = new BufferedInputStream(is);
      if (ba.storage.alwaysUnpack) {
        ji = new JarInputStream(bis, ba.storage.checkSigned);
        manifest = ji.getManifest();
        doUnpack = true;
      } else if (ba.storage.unpack) {
        // Is 1000000 enough, Must be big enough to hold the MANIFEST.MF entry
        // Hope implement of BufferedInputStream allocates dynamicly.
        bis.mark(1000000);
        ji = new JarInputStream(bis, ba.storage.checkSigned);
        manifest = ji.getManifest();
        // If manifest == null then, Manifest probably not first in JAR, should
        // we complain? Or if on Android there is a problem with JAR in JAR reading.
        // So we unpack to be sure we can access everything.
        if (manifest != null && !needUnpack(manifest.getMainAttributes())) {
          bis.reset();
        } else {
          doUnpack = true;
        }
      }
      if (doUnpack) {
        if (ba.storage.isReadOnly()) {
          throw new IOException("Bundle storage is read-only, no archive unpack.");
        }
        if (fileIsReference) {
          fileIsReference = false;
          file = bsFile;
        }
        file.mkdirs();
        if (manifest == null) {
          if (ba.storage.checkSigned) {
            // TBD? Which exception to use?
            throw new IOException("MANIFEST.MF must be first in archive when using signatures.");
          }
        } else {
          final File f = new File(file, META_INF_DIR);
          f.mkdir();
          final FileOutputStream fo = new FileOutputStream(new File(f, "MANIFEST.MF"));
          final BufferedOutputStream o = new BufferedOutputStream(fo);
          try {
            manifest.write(o);
          } finally {
            o.close();
          }
        }
        boolean verify = ba.storage.checkSigned;
        int verifiedEntries = 0;
        while (processNextJarEntry(ji, verify, file)) {
          if (verify) {
            if (isArchiveSigned()) {
              verifiedEntries++;
            } else {
              verify = false;
            }
          }
        }
        if (verify) {
          checkCertificates(verifiedEntries);
        }
      }
    }
    if (!doUnpack) {
      if (isDirectory) {
        if (!fileIsReference) {
          if (ba.storage.isReadOnly()) {
            throw new IOException("Bundle storage is read-only, unable to save archive.");
          }
          sourceFile.copyTo(file);
        }
        if (ba.storage.checkSigned) {
          // NYI! Verify signed directory
        }
      } else {
        if (!fileIsReference) {
          loadFile(file, bis);
        }
        if (ba.storage.checkSigned) {
          processSignedJar(file);
        }
        jar = new ZipFile(file);
      }
    }
    if (manifest != null) {
      manifest = new AutoManifest(ba.storage.framework, manifest, ba.location);
    } else {
      manifest = getManifest();
    }
    checkManifest();

    handleAutoManifest();

    saveCertificates();

    if (ji != null) {
      ji.close();
    } else if (bis != null) {
      bis.close();
    }
  }


  /**
   * Create an Archive based on contents of a saved archive in the specified
   * directory. Take lowest versioned archive and remove rest.
   *
   */
  protected void restoreArchive() throws IOException {
    subId = 0;
    final String[] f = bundleDir.list();
    file = null;
    if (revision != -1) {
      file = new FileTree(bundleDir, ARCHIVE + revision);
    } else {
      revision = Integer.MAX_VALUE;
      for (final String element : f) {
        if (element.startsWith(ARCHIVE)) {
          try {
            final int c = Integer.parseInt(element.substring(ARCHIVE.length()));
            if (c < revision) {
              revision = c;
              file = new FileTree(bundleDir, element);
            }
          } catch (final NumberFormatException ignore) {
          }
        }
      }
    }
    for (final String element : f) {
      if (element.startsWith(ARCHIVE)) {
        try {
          final int c = Integer.parseInt(element.substring(ARCHIVE.length()));
          if (c != revision) {
            (new FileTree(bundleDir, element)).delete();
          }
        } catch (final NumberFormatException ignore) {
        }
      }
      if (element.startsWith(SUBDIR)) {
        try {
          final int c = Integer.parseInt(element.substring(SUBDIR.length()));
          if (c != revision) {
            (new FileTree(bundleDir, element)).delete();
          }
        } catch (final NumberFormatException ignore) {
        }
      }
    }
    if (file == null) {
      try {
        final URL url = new URL(ba.location);
        if (isReference(url)) {
          file = new FileTree(getFile(url));
        }
      } catch (final Exception e) {
        throw new IOException("Bad file URL stored in referenced jar in: "
            + bundleDir.getAbsolutePath() + ", location=" + ba.location + ", e=" + e);
      }
      if (file == null || !file.exists()) {
        throw new IOException("No saved jar file found in: " + bundleDir.getAbsolutePath()
            + ", old location=" + ba.location);
      }
      fileIsReference = true;
    }

    if (!file.isDirectory()) {
      jar = new ZipFile(file);
    }
    if (ba.storage.checkSigned) {
      loadCertificates();
    }
    if (manifest == null) {
      manifest = getManifest();
    }
    handleAutoManifest();
  }


  /**
   * Create a Sub-Archive based on a path to in an already existing Archive. The
   * new archive is saved in a subdirectory below local copy of the existing
   * Archive.
   *
   * @param path Path of new Archive inside old Archive.
   * @param id Sub-id of Archive.
   * @exception FileNotFoundException if no such Jar file in archive.
   * @exception IOException if failed to read Jar file.
   */
  protected Archive subArchive(String path, int id) throws IOException {
    Archive res = ba.storage.createArchive(ba, bundleDir, revision);
    res.subId = id;
    if (jar != null) {
      res.jar = jar;
      // Try a directory first, make sure that path ends with "/"
      if (!path.endsWith("/")) {
        path += "/";
      }
      res.subJar = jar.getEntry(path);
      if (res.subJar == null) {
        res.subJar = jar.getEntry(path.substring(0, path.length() - 1));
      }
      if (res.subJar == null) {
        throw new IOException("No such JAR component: " + path);
      }
      res.file = file;
    } else {
      res.file = findFile(file, path);
      if (!res.file.isDirectory()) {
        res.jar = new ZipFile(res.file);
      }
    }
    return res;
  }


  /**
   * Show file name for archive, if zip show if it is sub archive.
   *
   * @return A string with result.
   */
  @Override
  public String toString() {
    if (subJar != null) {
      return file.getAbsolutePath() + "(" + subJar.getName() + ")";
    } else {
      return file.getAbsolutePath();
    }
  }


  /**
   * Get the file path from an URL, handling the case where it's a
   * reference:file: URL
   */
  private String getFile(URL source) {
    final String sfile = source.getFile();
    if (sfile.startsWith("file:")) {
      return sfile.substring(5);
    } else {
      return sfile;
    }
  }


  private boolean isFile(URL source) {
    return source != null && "file".equals(source.getProtocol());
  }


  /**
   * Check if an URL is a reference: URL or if we have global references on all
   * file: URLs
   */
  private boolean isReference(URL source) {
    return (source != null)
        && ("reference".equals(source.getProtocol()) || (ba.storage.fileReference && isFile(source)));
  }


  /**
   * Get revision number this archive.
   *
   * @return Archive revision number
   */
  int getRevision() {
    try {
      return Integer.parseInt(file.getName().substring(ARCHIVE.length()));
    } catch (final NumberFormatException ignore) {
      // assert?
      return -1;
    }
  }


  /**
   * Get bundle id for this archive.
   */
  public BundleGeneration getBundleGeneration() {
    return ba.getBundleGeneration();
  }


  /**
   * Get sub-archive id for this archive.
   */
  public int getSubId() {
    return subId;
  }


  /**
   * Get an attribute from the manifest of the archive.
   *
   * @param key Name of attribute to get.
   * @return A string with result or null if the entry doesn't exists.
   */
  String getAttribute(String key) {
    final Attributes a = manifest.getMainAttributes();
    if (a != null) {
      return a.getValue(key);
    }
    return null;
  }


  /**
   * Get a byte array containg the contents of named class file from the
   * archive.
   *
   * @param Class File to get.
   * @return Byte array with contents of class file or null if file doesn't
   *         exist.
   * @exception IOException if failed to read jar entry.
   */
  public byte[] getClassBytes(String classFile) throws IOException {
    if (bClosed) {
      return null;
    }
    final BundleResourceStream cif = getBundleResourceStream(classFile);
    if (cif != null) {
      byte[] bytes;
      final long ilen = cif.getContentLength();
      if (ilen >= 0) {
        bytes = new byte[(int)ilen];
        final DataInputStream dis = new DataInputStream(cif);
        dis.readFully(bytes);
      } else {
        bytes = new byte[0];
        final byte[] tmp = new byte[8192];
        try {
          int len;
          while ((len = cif.read(tmp)) > 0) {
            final byte[] oldbytes = bytes;
            bytes = new byte[oldbytes.length + len];
            System.arraycopy(oldbytes, 0, bytes, 0, oldbytes.length);
            System.arraycopy(tmp, 0, bytes, oldbytes.length, len);
          }
        } catch (final EOFException ignore) {
          // On Pjava we somtimes get a mysterious EOF excpetion,
          // but everything seems okey. (SUN Bug 4040920)
        }
      }
      cif.close();
      return bytes;
    } else {
      return null;
    }
  }


  /**
   * Get a BundleResourceStream to named entry inside an Archive.
   *
   * @param component Entry to get reference to.
   * @return BundleResourceStream to entry or null if it doesn't exist.
   */
  @SuppressWarnings("resource")
  public BundleResourceStream getBundleResourceStream(String component) {
    if (bClosed) {
      return null;
    }
    ZipEntry ze;
    try {
      if (jar != null) {
        if (subJar != null) {
          if (subJar.isDirectory()) {
            ze = jar.getEntry(subJar.getName() + component);
            if (null != ze) {
              InputStream is = jar.getInputStream(ze);
              if (null != is) {
                return new BundleResourceStream(is, ze.getSize());
              } else {
                // Workaround for directories given without trailing
                // "/"; they will not yield an input-stream.
                if (!component.endsWith("/")) {
                  final ZipEntry ze2 = jar.getEntry(subJar.getName() + component + "/");
                  is = jar.getInputStream(ze2);
                }
                return new BundleResourceStream(is, ze.getSize());
              }
            }
          } else {
            if (component.equals("")) {
              // Return a stream to the entire Jar.
              return new BundleResourceStream(jar.getInputStream(subJar), subJar.getSize());
            } else {
              final JarInputStream ji = new JarInputStream(jar.getInputStream(subJar));
              do {
                ze = ji.getNextJarEntry();
                if (ze == null) {
                  ji.close();
                  return null;
                }
              } while (!component.equals(ze.getName()));
              return new BundleResourceStream(ji, ze.getSize());
            }
          }
        } else {
          if (component.equals("")) {
            // Return a stream to the entire Jar.
            final File f = new File(jar.getName());
            return new BundleResourceStream(new FileInputStream(f), f.length());
          } else {
            ze = jar.getEntry(component);
            if (null != ze) {
              InputStream is = jar.getInputStream(ze);
              if (null != is) {
                return new BundleResourceStream(is, ze.getSize());
              } else {
                // Workaround for directories given without trailing
                // "/"; they will not yield an input-stream.
                if (!component.endsWith("/")) {
                  final ZipEntry ze2 = jar.getEntry(component + "/");
                  is = jar.getInputStream(ze2);
                }
                return new BundleResourceStream(is, ze.getSize());
              }
            }
          }
        }
      } else {
        final File f = findFile(file, component);
        return f.exists() ? new BundleResourceStream(new FileInputStream(f), f.length()) : null;
      }
    } catch (final IOException ignore) {
    }
    return null;
  }


  public Enumeration<String> findResourcesPath(String path) {
    if (bClosed) {
      return null;
    }
    final Vector<String> answer = new Vector<String>();
    if (jar != null) {
      ZipEntry entry;
      // "normalize" + erroneous path check: be generous
      path = path.replace('\\', '/');
      if (path.startsWith("/")) {
        path = path.substring(1);
      }
      if (!path.endsWith("/")/* in case bad argument */) {
        if (path.length() > 1) {
          path += "/";
        }
      }

      final Enumeration<? extends ZipEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        entry = entries.nextElement();
        final String name = entry.getName();
        if (name.startsWith(path)) {
          int idx = name.lastIndexOf('/');
          if (entry.isDirectory()) {
            idx = name.substring(0, idx).lastIndexOf('/');
          }
          if (idx > 0) {
            if (name.substring(0, idx + 1).equals(path)) {
              answer.add(name);
            }
          } else if (path.equals("")) {
            answer.add(name);
          }
        }
      }
    } else {
      final File f = findFile(file, path);
      if (!f.exists()) {
        return null;
      }
      if (!f.isDirectory()) {
        return null;
      }
      final File[] files = f.listFiles();
      final int length = files.length;
      for (int i = 0; i < length; i++) {
        String filePath = files[i].getPath();
        filePath = filePath.substring(file.getPath().length() + 1);
        filePath = filePath.replace(File.separatorChar, '/');
        if (files[i].isDirectory()) {
          filePath += "/";
        }
        answer.add(filePath);
      }
    }

    if (answer.size() == 0) {
      return null;
    }
    return answer.elements();
  }


  public boolean isJar() {
    return jar != null;
  }


  @Override
  public Class<?> loadClassBytes(String name, ClassLoader cl) {
    return null;
  }


  /**
   * Get a BundleResourceStream to named entry inside an Archive.
   *
   * @param component Entry to get reference to.
   * @return BundleResourceStream to entry or null if it doesn't exist.
   */
  public Set<String> listDir(String path) {
    if (bClosed) {
      return null;
    }
    if (path.startsWith("/")) {
      throw new RuntimeException("Assert! Path should never start with / here");
    }
    try {
      if (jar != null) {
        if (path.length() > 0 && !path.endsWith("/")) {
          path = path + "/";
        }
        if (subJar != null) {
          if (subJar.isDirectory()) {
            path = subJar.getName() + path;
          } else {
            final JarInputStream ji = new JarInputStream(jar.getInputStream(subJar));
            return listZipDir(path, ji);
          }
        }
        Set<String> res = new HashSet<String>();
        for (Enumeration<? extends ZipEntry> ize = jar.entries(); ize.hasMoreElements(); ) {
          ZipEntry ze = ize.nextElement();
          String e = matchPath(path, ze.getName());
          if (e != null) {
            res.add(e);
          }
        }
        return res;
      } else {
        final File f = findFile(file, path);
        if (f.isDirectory()) {
          Set<String> res = new HashSet<String>();
          for (File fl : f.listFiles()) {
            if (fl.isDirectory()) {
              res.add(fl.getName() + "/");
            } else {
              res.add(fl.getName());
            }
          }
          return res;
        }
      }
    } catch (final IOException ignore) {
    }
    return null;
  }


  private Set<String> listZipDir(String path, final JarInputStream ji) {
    ZipEntry ze;
    HashSet<String> res = new HashSet<String>();
    try {
      for (ze = ji.getNextJarEntry(); ze != null; ) {
        String e = matchPath(path, ze.getName());
        if (e != null) {
          res.add(e);
        }
      }
    } catch (IOException ioe) {
      try {
        ji.close();
      } catch (IOException _ignore) {
      }
    }
    return res;
  }


  private String matchPath(String basePath, String path) {
    final int len = basePath.length();
    if (path.length() > len && path.startsWith(basePath)) {
      int i = path.indexOf('/', len);
      if (i == -1) {
        return path.substring(len);
      } else {
        return path.substring(len, i + 1);
      }
    }
    return null;
  }


  /**
   * Get a BundleResourceStream to named entry inside an Archive.
   *
   * @param component Entry to get reference to.
   * @return BundleResourceStream to entry or null if it doesn't exist.
   */
  public boolean exists(String path, boolean onlyDirs) {
    if (bClosed) {
      return false;
    }
    if (path.startsWith("/")) {
      throw new RuntimeException("Assert! Path should never start with / here");
    }
    if (path.equals("")) {
      return true;
    }
    ZipEntry ze;
    try {
      if (jar != null) {
        if (onlyDirs && !path.endsWith("/")) {
          path = path + "/";
        }
        if (subJar != null) {
          if (subJar.isDirectory()) {
            path = subJar.getName() + path;
            ze = jar.getEntry(path);
            if (null != ze) {
              return ze.isDirectory() || !onlyDirs;
            }
            if (onlyDirs) {
              return checkMatch(path);
            }
          } else {
            final JarInputStream ji = new JarInputStream(jar.getInputStream(subJar));
            try {
              String n;
              if (onlyDirs && !path.endsWith("/")) {
                path = path + "/";
              }
              for (ze = ji.getNextJarEntry(); ze != null; ) {
                n = ze.getName();
                if (onlyDirs) {
                  if (n.startsWith(path)) {
                    return true;
                  }
                } else if (n.equals(path)) {
                  return true;
                }
              }
            } finally {
              ji.close();
            }
          }
        } else {
          ze = jar.getEntry(path);
          if (null != ze) {
            return ze.isDirectory() || !onlyDirs;
          }
          if (onlyDirs) {
            return checkMatch(path);
          }
        }
      } else {
        final File f = findFile(file, path);
        if (f.exists()) {
          return f.isDirectory() || !onlyDirs;
        }
      }
    } catch (final IOException ignore) {
    }
    return false;
  }


  private boolean checkMatch(String path) {
    ZipEntry ze;
    if (!path.endsWith("/")) {
      path = path + "/";
    }
    for (Enumeration<? extends ZipEntry> ize = jar.entries(); ize.hasMoreElements(); ) {
      ze = ize.nextElement();
      String e = matchPath(path, ze.getName());
      if (e != null) {
        return true;
      }
    }
    return false;
  }


  /**
   * Check for native library in archive.
   *
   * @param path Name of native code file to get.
   * @return If native library exist return libname, otherwise null.
   */
  public String checkNativeLibrary(String path) {
    if (bClosed) {
      return null; // throw new IOException("Archive is closed");
    }
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    File lib;
    if (jar != null) {
      lib = getSubFile(this, path);
      if (!lib.exists()) {
        (new File(lib.getParent())).mkdirs();
        final ZipEntry ze = jar.getEntry(path);
        if (ze != null) {
          InputStream is = null;
          try {
            is = jar.getInputStream(ze);
            loadFile(lib, is);
          } catch (final IOException _ignore) {
            // TBD log this
            if (is != null) {
              try {
                is.close();
              } catch (final IOException _ignore2) {
              }
            }
            return null;
          }
        } else {
          return null;
        }
      }
    } else {
      lib = findFile(file, path);
      if (!lib.exists()) {
        if (lib.getParent() != null) {
          final String libname = lib.getName();
          final File[] list = lib.getParentFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              final int pos = name.lastIndexOf(libname);
              return ((pos > 1) && (name.charAt(pos - 1) == '_'));
            }
          });
          if (list.length > 0) {
            list[0].renameTo(lib);
          } else {
            return null;
          }
        } else {
          return null;
        }
      }
    }
    setPerm(lib);
    final String libstr = lib.getAbsolutePath();
    final int sp = libstr.lastIndexOf(File.separatorChar);
    final String key = (sp != -1) ? libstr.substring(sp + 1) : libstr;
    if (nativeLibs == null) {
      nativeLibs = new HashMap<String, String>();
      renameLibs = new HashMap<String, String>();
    }
    // TBD, What to do if entry already exists?
    nativeLibs.put(key, libstr);
    return key;
  }


  /**
   * Get native code library filename.
   *
   * @param libNameKey Key for native lib to get.
   * @return A string with the path to the native library.
   */
  public String getNativeLibrary(String libNameKey) {
    final String file = nativeLibs.get(libNameKey);
    if (file != null) {
      final File f = new File(file);
      if (f.isFile()) {
        return doRename(libNameKey, f);
      }
    }
    return null;
  }


  /**
   * Renaming to allow multiple versions of the lib when there are more than one
   * classloader for this bundle. E.g., after a bundle update.
   */
  private String doRename(String key, File file1) {
    String val = file1.getAbsolutePath();
    // TODO should we fail when we are readonly
    if (!ba.storage.isReadOnly() && renameLibs.containsKey(key)) {
      final File file2 = new File(renameLibs.get(key));
      if (file1.renameTo(file2)) {
        val = file2.getAbsolutePath();
        nativeLibs.put(key, val);
      }
    }
    final StringBuilder rename = new StringBuilder(val);
    final int index0 = val.lastIndexOf(File.separatorChar) + 1;
    final int index1 = val.indexOf("_", index0);
    if ((index1 > index0) && (index1 == val.length() - key.length() - 1)) {
      try {
        final int prefix = Integer.parseInt(val.substring(index0, index1));
        rename.replace(index0, index1, Integer.toString(prefix + 1));
      } catch (final Throwable t) {
        rename.insert(index0, "0_");
      }
    } else {
      rename.insert(index0, "0_");
    }
    renameLibs.put(key, rename.toString());
    return val;
  }


  /**
   *
   */
  private void setPerm(File f) {
    // No OS-cmd for setting permissions given.
    if (ba.storage.isReadOnly() || ba.storage.execPermCmd.length() == 0) {
      return;
    }
    final String abspath = f.getAbsolutePath();
    final String[] execarray = Util.splitwords(ba.storage.execPermCmd);
    final String[] cmdarray;
    int start;

    // Windows systems need a "cmd /c" at the begin
    if (ba.storage.isWindows && !execarray[0].equalsIgnoreCase("cmd")) {
      cmdarray = new String[execarray.length + 2];
      cmdarray[0] = "cmd";
      cmdarray[1] = "/c";
      start = 2;
    } else {
      cmdarray = new String[execarray.length];
      start = 0;
    }
    for (int i = 0; i < execarray.length; i++) {
      cmdarray[i + start] = Util.replace(execarray[i], "${abspath}", abspath);
    }
    try {
      final Process p = Runtime.getRuntime().exec(cmdarray);
      final Thread ti = new InputGlobber(null, p.getInputStream());
      final Thread te = new InputGlobber(cmdarray, p.getErrorStream());
      ti.start();
      te.start();
      while (true) {
        try {
          p.waitFor();
          break;
        } catch (final InterruptedException _ie) {
          _ie.printStackTrace();
        }
      }
      while (true) {
        try {
          ti.join();
          break;
        } catch (final InterruptedException _ie) {
          _ie.printStackTrace();
        }
      }
      while (true) {
        try {
          te.join();
          break;
        } catch (final InterruptedException _ie) {
          _ie.printStackTrace();
        }
      }
    } catch (final IOException _ioe) {
      _ioe.printStackTrace();
    }
  }

  // A thread class that consumes all data on an input stream and then
  // terminates.
  static class InputGlobber extends Thread {
    String[] cmd;
    final InputStream in;
    boolean copyToStdout;


    InputGlobber(String[] cmd, InputStream in) {
      this.cmd = cmd;
      this.in = in;
      copyToStdout = cmd != null;
    }


    @Override
    public void run() {
      final BufferedReader br = new BufferedReader(new InputStreamReader(in));
      try {
        String line = br.readLine();
        while (null != line) {
          if (null != cmd) {
            final StringBuilder sb = new StringBuilder();
            for (final String element : cmd) {
              if (sb.length() > 0)
                sb.append(" ");
              sb.append(element);
            }
            // NYI! Log error
            System.err.println("Failed to execute: '" + sb.toString() + "':");
            cmd = null;
          }
          if (copyToStdout)
            System.out.println(line);
          line = br.readLine();
        }
      } catch (final IOException _ioe) {
        _ioe.printStackTrace();
      }
    }
  }


  /**
   * Remove archive and any unpacked sub-archives.
   */
  void purge() {
    close();
    // Remove archive if not flagged as keep
    if (!fileIsReference) {
      file.delete();
    }

    // Remove any cached sub files
    getSubFileTree(this).delete();

    // Remove any saved certificates.
    removeCertificates();
  }


  /**
   * Close archive and all open sub-archives. If close fails it is silently
   * ignored.
   */
  void close() {
    bClosed = true; // Mark as closed to safely handle referenced files
    if (subJar == null && jar != null) {
      try {
        jar.close();
      } catch (final IOException ignore) {
      }
    }
  }


  /**
   * Returns the File object for this bundle.
   */
  protected File getFile() {
    return file;
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
  Certificate[] getCertificates() {
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
    final Attributes a = manifest.getMainAttributes();
    Util.parseManifestHeader(Constants.EXPORT_PACKAGE,
                             a.getValue(Constants.EXPORT_PACKAGE), false, true,
                             false);
    Util.parseManifestHeader(Constants.IMPORT_PACKAGE,
                             a.getValue(Constants.IMPORT_PACKAGE), false, true,
                             false);
    if (ba.storage.isReadOnly() && !file.isDirectory() && needUnpack(a)) {
      throw new IllegalArgumentException("Framework is in read-only mode, we can not " +
                                         "install bundles that needs to be downloaded " +
                                         "(e.g. has native code or an internal Bundle-ClassPath)");
    }
    // NYI, more checks?
  }


  /**
   * Check if we should unpack bundle, i.e has native code file or subjars. This
   * decision is based on minimizing the expected size.
   *
   * @return true if bundle needs to be unpacked.
   * @exception IllegalArgumentException if we have a broken manifest.
   */
  private boolean needUnpack(Attributes a) {
    final List<HeaderEntry> nc = Util
        .parseManifestHeader(Constants.BUNDLE_NATIVECODE,
                             a.getValue(Constants.BUNDLE_NATIVECODE), false,
                             false, false);
    final String bc = a.getValue(Constants.BUNDLE_CLASSPATH);
    return (bc != null && !bc.trim().equals(".")) || !nc.isEmpty();
  }


  /**
   * Handle automanifest stuff. Archive manifest must be set.
   */
  private void handleAutoManifest() throws IOException {
    // TBD Should we check this, should it not always be true!
    if (manifest instanceof AutoManifest) {
      final AutoManifest mf = (AutoManifest)manifest;
      if (mf.isAuto()) {
        if (jar != null) {
          mf.addZipFile(jar);
        } else if (file != null && file.isDirectory()) {
          mf.addFile(file.getAbsolutePath(), file);
        }
      }
    }
  }


  /**
   * Get file handle for file inside a directory structure. The path for the
   * file is always specified with a '/' separated path.
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
    final BundleResourceStream mi = getBundleResourceStream("META-INF/MANIFEST.MF");
    if (mi != null) {
      return new AutoManifest(ba.storage.framework, new Manifest(mi), ba.location);
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
    return new FileTree(archive.file.getParent(), SUBDIR
        + archive.file.getName().substring(ARCHIVE.length()));
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
   * @param output File to save data in, if <code>null</code>, discard output.
   * @param is InputStream to read from.
   */
  private void loadFile(File output, InputStream is) throws IOException {
    if (output != null && ba.storage.isReadOnly()) {
      throw new IOException("Bundle storage is read-only, unable to save: " + output);
    }
    OutputStream os = null;
    try {
      if (output != null) {
        os = new FileOutputStream(output);
      }
      final byte[] buf = new byte[8192];
      int n;
      try {
        while ((n = is.read(buf)) >= 0) {
          if (os != null) {
            os.write(buf, 0, n);
          }
        }
      } catch (final EOFException ignore) {
        // On Pjava we sometimes get a mysterious EOF exception,
        // but everything seems okey. (SUN Bug 4040920)
      }
    } catch (final IOException e) {
      if (os != null) {
        output.delete();
      }
      throw e;
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }


  /**
   * Check if archive is signed
   */
  private boolean isArchiveSigned() {
    return certs != null;
  }


  /**
   *
   */
  private boolean processNextJarEntry(JarInputStream ji, boolean verify, File saveDir)
      throws IOException {
    JarEntry je;
    while ((je = ji.getNextJarEntry()) != null) {
      if (je.isDirectory()) {
        continue;
      }
      final String name = je.getName();
      if (saveDir != null && !startsWithIgnoreCase(name, OSGI_OPT_DIR)) {
        final StringTokenizer st = new StringTokenizer(name, "/");
        File f = new File(saveDir, st.nextToken());
        while (st.hasMoreTokens()) {
          f.mkdir();
          f = new File(f, st.nextToken());
        }
        loadFile(f, ji);
      } else {
        // Read entry to update certificates.
        loadFile(null, ji);
      }
      ji.closeEntry();
      if (startsWithIgnoreCase(name, META_INF_DIR)) {
        String sub = name.substring(META_INF_DIR.length());
        if (sub.indexOf('/') == -1) {
          if (startsWithIgnoreCase(sub, "SIG-")) {
            continue;
          }
          int idx = sub.lastIndexOf('.');
          if (idx != -1) {
            sub = sub.substring(idx + 1);
            if (sub.equalsIgnoreCase("DSA") ||
                sub.equalsIgnoreCase("RSA") ||
                sub.equalsIgnoreCase("SF")) {
              continue;
            }
          }
        }
        if (ba.storage.jarVerifierBug) {
          // There is a bug in Oracles java library.
          // JavaInputStream will verify files in
          // META-INF if they directly follow the META-INF
          // signature related files.
          if (certs == null) {
            certs = new Certificate[0];
            verify = false;
          } else if (certs.length == 0) {
            verify = false;
          }
        }
      }
      if (verify) {
        final Certificate[] c = je.getCertificates();
        if (c != null) {
          if (certs != null && certs.length > 0) {
            // TBD, perhaps we should allow permuted chains.
            if (!Arrays.equals(c, certs)) {
              certs = null;
            }
          } else {
            certs = c;
          }
        } else {
          certs = null;
        }
      }
      return true;
    }
    return false;
  }


  /**
   * Check if <code>name</code> start with <code>prefix</code> ignoring
   * case of letters.
   * 
   * @param name String to check
   * @param prefix Prefix string, must be in upper case.
   * 
   * @return <code>true</code> if name start with prefix, otherwise
   *         <code>false</code>
   */
  private boolean startsWithIgnoreCase(final String name, final String prefix) {
    if (name.length() >= prefix.length()) {
      for (int i = 0; i < prefix.length(); i++) {
        if (Character.toUpperCase(name.charAt(i)) != prefix.charAt(i)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }


  /**
   * Go through jar file and check signatures.
   */
  private void processSignedJar(File file) throws IOException {
    final FileInputStream fis = new FileInputStream(file);
    JarInputStream ji = null;
    try {
      final BufferedInputStream bis = new BufferedInputStream(fis);
      ji = new JarInputStream(bis);
      int count = 0;

      manifest = ji.getManifest();

      while (processNextJarEntry(ji, true, null)) {
        if (isArchiveSigned()) {
          count++;
        } else {
          break;
        }
      }
      checkCertificates(count);
    } finally {
      if (ji != null) {
        ji.close();
      } else {
        fis.close();
      }
    }
  }


  /**
   *
   */
  private void checkCertificates(int filesVerified) {
    if (filesVerified > 0) {
      Exception warn = null;
      if (certs != null) {
        if (certs.length > 0) {
          // TODO, check that we have all entries in .SF file
          return;
        } else {
          warn = new IOException("Only contained META-INF entries with no certs due to JRE bug. Entries found " + filesVerified);          
        }
      } else {
        warn = new IOException("All entries in bundle not completly signed, scan aborted");
      }
      certs = null;
      ba.frameworkWarning(warn);
    }
  }


  /**
   *
   */
  private void saveCertificates() throws IOException {
    if (!ba.storage.isReadOnly()) {
      final File f = new File(getPath() + CERTS_SUFFIX);
      if (certs != null) {
        try {
          final FileOutputStream fos = new FileOutputStream(f);
          for (final Certificate cert : certs) {
            fos.write(cert.getEncoded());
          }
          fos.close();
        } catch (final CertificateEncodingException e) {
          ba.frameworkWarning(e);
        }
      }
    }
  }


  /**
   * TBD improve this.
   */
  private void loadCertificates() throws IOException {
    final File f = new File(getPath() + CERTS_SUFFIX);
    if (f.canRead()) {
      try {
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final FileInputStream fis = new FileInputStream(f);
        final Collection<? extends Certificate> c = cf.generateCertificates(fis);
        // TBD, check if order is preserved
        if (c.size() > 0) {
          certs = new Certificate[c.size()];
          certs = c.toArray(certs);
        }
      } catch (final CertificateException e) {
        ba.frameworkWarning(e);
      }
    }
    // TODO, load certificates from both trusted and untrusted storage!?
  }


  /**
   *
   */
  private void removeCertificates() {
    final File f = new File(getPath() + CERTS_SUFFIX);
    f.delete();
  }

}
