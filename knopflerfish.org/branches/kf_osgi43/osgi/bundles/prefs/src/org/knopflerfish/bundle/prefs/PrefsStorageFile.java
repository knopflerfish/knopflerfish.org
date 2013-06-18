/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.knopflerfish.util.Text;


/**
 *
 *  baseDir
 *    |
 *   nodeDir
 *    |
 *    +-- .keys
 *    |
 *    +-- child
 *    |
 *    +-- child
 *
 *
 */
public class  PrefsStorageFile implements PrefsStorage {

  /** Name of the subdir holding user preferences. */
  private static final String USERS_BASE  = "users";
  /** Name of the subdir holding system preferences. */
  private static final String SYSTEM_BASE = "system";

  /**
   * The root directory of the preferences file storage.
   *
   * Contains one subdir for each bundle that fetches a preferences
   * service instance. The bundle ID is the name of the subdir.
   *
   * Default location is a directory named <tt>prefsdir</tt> in the
   * current working directory (as given by the system property
   * <tt>user.dir</tt>). The root directory may be specified by setting
   * the system property <tt>org.knopflerfish.prefs.dir</tt>.
   */
  private static File getRootDir()
  {
    if (null==Activator.bc) {
      throw new IllegalStateException("PreferencesService has been stopped!");
    }
    String prefsDir = Activator.bc.getProperty("org.knopflerfish.prefs.dir");
    if (null==prefsDir || prefsDir.length()==0 ) {
      prefsDir = "prefsdir";
    }
    return new File(prefsDir);
  }

  /**
   * Creates a prefs storage object for the system tree.
   * @param bundle the bundle owning the prefs tree.
   */
  static PrefsStorage createPrefsStorageSystem(Bundle bundle)
  {
    File baseDir = new File(getRootDir(), SYSTEM_BASE);
    baseDir      = new File(baseDir, String.valueOf(bundle.getBundleId()));
    return new PrefsStorageFile(baseDir);
  }

  // User names must be encoded to avoid creating invalid file names
  // Note that specification does not forbid '/' in user names
  /**
   * Creates a prefs storage object for the a user tree.
   * @param bundle the bundle owning the prefs tree.
   * @param user   the user that this tree applies too.
   */
  static PrefsStorage createPrefsStorageUser(Bundle bundle, String user)
  {
    File baseDir = new File(getRootDir(), USERS_BASE);
    baseDir      = new File(baseDir, String.valueOf(bundle.getBundleId()));
    baseDir      = new File(baseDir, encodeUser(user));
    return new PrefsStorageFile(baseDir);
  }

  static String[] getPrefsStorageUsers(Bundle bundle)
  {
    File baseDir = new File(getRootDir(), USERS_BASE);
    baseDir      = new File(baseDir, String.valueOf(bundle.getBundleId()));
    return new PrefsStorageFile(baseDir).getChildrenNames("");
  }


  /**
   * Removes all preferences trees for the given bundle.
   */
  static void cleanup(Bundle bundle)
  {
    File baseDir = new File(getRootDir(), USERS_BASE);
    baseDir      = new File(baseDir, String.valueOf(bundle.getBundleId()));
    deleteTree(baseDir);

    baseDir = new File(getRootDir(), SYSTEM_BASE);
    baseDir = new File(baseDir, String.valueOf(bundle.getBundleId()));
    deleteTree(baseDir);
  }


  final File baseDir;

  static final String KEYFILE_NAME = ".keys";

  final Object lock = new Object();

  /** Set to true when the entire prefs tree repesented by this storage
   ** have been removed.
   **/
  boolean bStale = false;

  public boolean isStale()
  {
    return bStale;
  }


  boolean bDebug = false;

  private PrefsStorageFile(final File baseDir) {
    this.baseDir = (File) AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        baseDir.mkdirs();

        if(!baseDir.exists() || !baseDir.isDirectory()) {
          throw new RuntimeException
          ("Failed to create root directory for preferences storage: '"
              +baseDir +"'.");
        }
        return baseDir;
      }
    });
  }

  File getNodeDir(String path, boolean bCreate) {
    synchronized(lock) {
      if("".equals(path)) {
        // root
      } else {
        if(!path.startsWith("/")) {
          throw new IllegalArgumentException("Path must be absolute, is '" +
                                             path + "'");
        }
        path = path.substring(1);
      }
      final File file = new File(baseDir, encode(path));
      final String path2 = path;
      if(bCreate) {
        AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            file.mkdirs();
            if(!file.exists() || !file.isDirectory()) {
              throw new RuntimeException("Failed to create node dir=" +
                  file.getAbsolutePath() + " from path " + path2);
            }
            return null;
          }
        });
      }
      return file;
    }
  }


  Dictionary<String, String> loadProps(final String path) throws IOException {
    synchronized(lock) {
      final Dictionary<String, String> dict = propsMap.get(path);
      if(dict != null) {
        return dict;
      }

      try {
        return (Dictionary) AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run() throws IOException {
            final Properties props = new Properties();
            InputStream in = null;
            try {
              final File f = getKeyFile(path);
              if(f.exists()) {
                in = new FileInputStream(f);
                props.load(in);

                // We might need to decode some keys
                final Dictionary<String, String> p2 = new Hashtable<String, String>();
                for(final Entry<Object,Object> e : props.entrySet()) {
                  final String decodedKey = decode((String)e.getKey());
                  final String val        = (String)e.getValue();
                  p2.put(decodedKey, val);
                }

                propsMap.put(path, p2);
                return p2;
              } else {
                throw new IllegalStateException("No keys for path=" + path +
                                                ", file=" + f.getAbsolutePath());
              }
            } finally {
              try { in.close(); } catch (final Exception ignored) {  }
            }
          }
        });
      } catch (final PrivilegedActionException pae) {
        throw (IOException) pae.getCause();
      }
    }
  }

  void saveProps(final String path, final Dictionary<String, String> p) throws IOException {
    synchronized(lock) {

      final Properties props = new Properties();
      for(final Enumeration<String> e = p.keys(); e.hasMoreElements(); ) {
        final String key = e.nextElement();
        final Object val = p.get(key);
        props.put(encode(key), val);
      }

      try {
        AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run() throws IOException {
            OutputStream out = null;
            try {
              final File f = getKeyFile(path);
              out = new FileOutputStream(f);
              props.save(out, "keys for " + path);
            } catch (final IOException e) {
              e.printStackTrace();
              throw e;
            } finally {
              try { out.close(); } catch (final Exception ignored) {
              }
            }
            return null;
          }
        });
      } catch (final PrivilegedActionException pae) {
        throw (IOException) pae.getCause();
      }
    }
  }

  public void put(final String path, final String key, final String val) {
    try {
      dirtySet.add(path);
      final Dictionary<String, String> p = loadProps(path);

      p.put(key, val);

      // System.out.println("dirty " + path);
      //      saveProps(path, p);
    } catch (final Exception e) {
      logWarn("Failed to put path=" + path +", key=" + key + ", val=" + val,
              e);
    }
  }



  public String[] getChildrenNames(final String path) {
    synchronized(lock) {
      try {
        final File dir = getNodeDir(path, false);
        final String[] f = dir.list();
        final Vector<String> v = new Vector<String>();
        for(int i = 0; i < f.length; i++) {
          if(!f[i].startsWith(".")) {
            // Use decodeUser() here since it may be user names and
            // its safe to use that decoder on this type of encoded
            // strings.
            v.addElement(decodeUser(f[i]));
          }
        }
        final String[] f2 = new String[v.size()];
        v.copyInto(f2);
        return f2;
      } catch (final Exception e) {
        throw new IllegalStateException("Failed to get children from '"
                                        + path + "'");
      }
    }
  }

  public String[] getKeys(final String path) {
    try {
      final Dictionary<String, String> props = loadProps(path);
      final String[]   keys  = new String[props.size()];

      int i = 0;
      for(final Enumeration<String> e = props.keys(); e.hasMoreElements(); ) {
        keys[i++] = e.nextElement();
      }
      return keys;
    } catch (final Exception e) {
      return new String[0];
    }
  }

  public String get(final String path, final String key, final String def) {
    synchronized(lock) {
      try {
        final Dictionary<String, String> props = loadProps(path);
        final String val = props.get(key);

        return val != null ? val : def;

      } catch (final IOException e) {
        logWarn("Failed to read " + path + ", key=" + key, e);
        return def;
      }
    }
  }


  public void removeAllKeys(final String path) {
    synchronized(lock) {
      try {
        final Dictionary<String, String> props = new Hashtable<String, String>();
        propsMap.put(path, props);
        dirtySet.add(path);
        //        saveProps(path, props);
      } catch (final Exception e) {
        logWarn("Failed to clear " + path, e);
      }
    }
  }

  public void removeKey(final String path, final String key) {
    synchronized(lock) {
      try {
        final Dictionary<String, String> props = loadProps(path);
        props.remove(key);
        propsMap.put(path, props);
        dirtySet.add(path);
        //        saveProps(path, props);
      } catch (final Exception e) {
        logWarn("Failed to remove " + path + ", key=" + key, e);
      }
    }
  }

  public void removeNode(final String path) {
    synchronized(lock) {
      try {
        if("".equals(path)) {
          bStale = true;
        }

        final Iterator<Entry<String,PreferencesImpl>>
          it = prefs.entrySet().iterator();
        while (it.hasNext()) {
          final Entry<String,PreferencesImpl> entry = it.next();
          final String p = entry.getKey();
          final PreferencesImpl pi = entry.getValue();

          if(bStale || p.equals(path) || p.startsWith(path + "/")) {
            pi.bStale = true;
            it.remove();
          }
        }

        final File f = getNodeDir(path, false);
        deleteTree(f);
      } catch (final Exception e) {
        e.printStackTrace();
        logWarn("Failed to remove node " + path, e);
      }
    }
  }

  // String (path) -> PreferencesImpl
  final Map<String, PreferencesImpl> prefs = new HashMap<String, PreferencesImpl>();
  final Set<String> dirtySet = new HashSet<String>();
  final Map<String, Dictionary<String,String>> propsMap
    = new HashMap<String, Dictionary<String,String>>();

  public Preferences getNode(final String path, boolean bCreate) {
    try {
      PreferencesImpl pi = prefs.get(path);
      if(pi != null) {
        return pi;
      }

      getNodeDir(path, bCreate);
      final File keyFile = getKeyFile(path);

      AccessController.doPrivileged(new PrivilegedExceptionAction() {
        public Object run() throws Exception {
          if(!keyFile.exists()) {
            final Dictionary<String, String> props = new Hashtable<String, String>();
            propsMap.put(path, props);
            saveProps(path, props);
          }
          return null;
        }
      });

      pi = new PreferencesImpl(this, path);
      prefs.put(path, pi);
      return pi;
    } catch (Exception e) {
      if (e instanceof PrivilegedActionException) {
        e = (Exception) e.getCause();
      }
      throw new IllegalStateException("getNode " + path + " failed: " + e);
    }
  }

  public boolean nodeExists(final String path) {
    synchronized(lock) {
      final File f = getNodeDir(path, false);
      final int ix = path.lastIndexOf('/');

      return ((Boolean) AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          boolean b = false;
          if(ix != -1 && path.length() > 0) {
            final String last  = decode(path.substring(ix + 1));
            String fname = f.getAbsolutePath();

            try {
              fname = f.getCanonicalPath();
            } catch (final IOException e) {
              logWarn("failed to get canonical path of " + path, e);
            }

            fname = decode(fname);
            if(fname.endsWith(last)) {
              b = f.exists();
            }
          } else {
            b = f.exists();
          }
          return new Boolean(b);
        }
      })).booleanValue();
    }
  }

  public void flush(final String path)
    throws BackingStoreException
  {
    synchronized(lock) {
      // allways save all dirty nodes to the storage
      synchronized(dirtySet) {
        // System.out.println("flushing " + dirtySet.size() + " items");
        for (final String p : dirtySet) {
          final Dictionary<String, String> props = propsMap.get(p);
          if(props != null) {
            //              System.out.println("flush '" + p + "'");
            try {
              saveProps(p, props);
            } catch (final Exception e) {
              final String msg = "Failed to flush, baseDir="
                +baseDir +"; "+e;
              throw new BackingStoreException(msg);
            }
          }
        }
        dirtySet.clear();
      }
    }
  }

  public void sync(final String path)
    throws BackingStoreException
  {
    // Save any local changes
    flush(path);
    // Fetch changes from backing store.
  }

  public void logWarn(final String msg, final Throwable t)
  {
    Activator.log.warn(msg, t);
  }

  /**
   * Get file which store key/value pairs
   */
  File getKeyFile(final String path) {
    return new File(getNodeDir(path, true), KEYFILE_NAME);
  }

  static void deleteTree(final File f) {
    AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        PrefsStorageFile.deleteTree0(f);
        return null;
      }
    });
  }

  // note: needs to be called in privileged action
  private static void deleteTree0(final File f) {
    if(f.exists()) {
      if(f.isDirectory()) {
        final String[] children = f.list();
        for (final String element : children) {
          deleteTree0(new File(f, element));
        }
      }
      f.delete();
    }
  }

  static String encode(final String s) {
    String res = s;
    // Must encode '__' since they are used as encoding marker.
    res = Text.replace(res, "__", "__us__");
    res = Text.replace(res, ".", "__dot__");
    res = Text.replace(res, " ", "__space__");
    res = Text.replace(res, "?", "__q__");
    res = Text.replace(res, "\\", "__bslash__");
    return res;
  }

  static String decode(final String s) {
    String res = s;
    res = Text.replace(res, "__space__",   " ");
    res = Text.replace(res, "__dot__",   ".");
    res = Text.replace(res, "__?__",     "?");
    res = Text.replace(res, "__bslash__", "\\");
    res = Text.replace(res, "__us__", "__");
    return res;
  }

  /** Same encoding as <code>encode()</code> with the addition that
   ** '/' is also encoded.
   ** @param s The string to encode
   ** @return String safe to use as file name.
   **/
  static String encodeUser(final String s) {
    String res = s;
    res = encode(res);
    res = Text.replace(res, "/", "__slash__");
    return res;
  }

  /** Decodes a string encoded by <code>encodeUser()</code>.
   ** @see #encodeUser(String)
   ** @param s The encoded string to decode
   ** @return String decoded string.
   **/
  static String decodeUser(final String s) {
    String res = s;
    res = Text.replace(res, "__slash__", "/" );
    res = decode(res);
    return res;
  }

}
