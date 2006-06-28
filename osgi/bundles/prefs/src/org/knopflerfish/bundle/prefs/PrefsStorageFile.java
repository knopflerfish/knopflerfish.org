/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

import org.osgi.framework.*;
import org.osgi.service.prefs.*;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.Text;
import java.util.*;

import java.io.*;


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
  File baseDir;

  static final String KEYFILE_NAME = ".keys";

  Object lock = new Object();

  boolean bDebug = false;

  PrefsStorageFile(String base) {
    File top = new File(System.getProperty("org.knopflerfish.prefs.dir",
                                           "prefsdir"));
    baseDir  = new File(top, base);
    baseDir.mkdirs();

    if(!baseDir.exists() || !baseDir.isDirectory()) {
      throw new RuntimeException("Failed to create base=" + base);
    }

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
      File file = new File(baseDir, encode(path));
      if(bCreate) {
        file.mkdirs();
        if(!file.exists() || !file.isDirectory()) {
          throw new RuntimeException("Failed to create node dir=" +
                                     file.getAbsolutePath() + " from path " +
                                     path);
        }
      }
      return file;
    }
  }


  Dictionary loadProps(String path) throws IOException {
    synchronized(lock) {
      Dictionary dict = (Dictionary)propsMap.get(path);
      if(dict != null) {
        return dict;
      }
      Properties props = new Properties();
      InputStream in = null;
      try {
        File f = getKeyFile(path);
        if(f.exists()) {
          in = new FileInputStream(f);
          props.load(in);

          // We might need to decode some keys
          Properties p2 = new Properties();
          for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
            String key        = (String)e.nextElement();
            String decodedKey = decode(key);
            String val        = (String)props.get(key);
            p2.put(decodedKey, val);
          }

          propsMap.put(path, p2);
          return p2;
        } else {
          throw new IllegalStateException("No keys for path=" + path +
                                          ", file=" + f.getAbsolutePath());
        }
      } finally {
        try { in.close(); } catch (Exception ignored) {  }
      }
    }
  }

  void saveProps(String path, Dictionary p) throws IOException {
    synchronized(lock) {

      Properties props = new Properties();
      for(Enumeration e = p.keys(); e.hasMoreElements(); ) {
        String key = (String)e.nextElement();
        Object val = p.get(key);
        props.put(encode(key), val);
      }

      OutputStream out = null;
      try {
        File f = getKeyFile(path);
        out = new FileOutputStream(f);
        props.save(out, "keys for " + path);
      } catch (IOException e) {
        e.printStackTrace();
        throw e;
      } finally {
        try { out.close(); } catch (Exception ignored) {
        }
      }
    }
  }

  public void   put(String path, String key, String val) {
    try {
      dirtySet.add(path);
      Dictionary p = loadProps(path);

      p.put(key, val);

      // System.out.println("dirty " + path);
      //      saveProps(path, p);
    } catch (Exception e) {
      Activator.log.warn("Failed to put path=" + path +
                         ", key=" + key + ", val=" + val,
                         e);
    }
  }



  public String[] getChildrenNames(String path) {
    synchronized(lock) {
      try {
        File dir = getNodeDir(path, false);
        String[] f = dir.list();
        Vector v = new Vector();
        for(int i = 0; i < f.length; i++) {
          if(!f[i].startsWith(".")) {
            v.addElement(decode(f[i]));
          }
        }
        String[] f2 = new String[v.size()];
        v.copyInto(f2);
        return f2;
      } catch (Exception e) {
        throw new IllegalStateException("Failed to get children from '"
                                        + path + "'");
      }
    }
  }

  public String[] getKeys(String path) {
    try {
      Dictionary props = loadProps(path);
      String[]   keys  = new String[props.size()];

      int i = 0;
      for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
        keys[i++] = (String) e.nextElement();
      }
      return keys;
    } catch (Exception e) {
      return new String[0];
    }
  }

  public String get(String path, String key, String def) {
    synchronized(lock) {
      try {
        Dictionary props = loadProps(path);
        String val = (String)props.get(key);

        return val != null ? val : def;

      } catch (IOException e) {
        Activator.log.warn("Failed to read " + path + ", key=" + key);
        return def;
      }
    }
  }


  public void removeAllKeys(String path) {
    synchronized(lock) {
      try {
        Dictionary props = new Hashtable();
        propsMap.put(path, props);
        dirtySet.add(path);
        //        saveProps(path, props);
      } catch (Exception e) {
        Activator.log.warn("Failed to clear " + path);
      }
    }
  }

  public void removeKey(String path, String key) {
    synchronized(lock) {
      try {
        Dictionary props = loadProps(path);
        props.remove(key);
        propsMap.put(path, props);
        dirtySet.add(path);
        //        saveProps(path, props);
      } catch (Exception e) {
        Activator.log.warn("Failed to remove " + path + ", key=" + key);
      }
    }
  }

  public void removeNode(String path) {
    synchronized(lock) {
      try {
        for(Iterator it = prefs.keySet().iterator(); it.hasNext(); ) {
          String p = (String)it.next();
          PreferencesImpl pi = (PreferencesImpl)prefs.get(p);
          if(p.startsWith(path + "/")) {
            pi.bStale = true;
          }
        }

        File f = getNodeDir(path, false);
        deleteTree(f);
      } catch (Exception e) {
        e.printStackTrace();
        Activator.log.warn("Failed to remove node " + path);
      }
    }
  }

  // String (path) -> PreferencesImpl
  Map prefs = new HashMap();
  Set dirtySet = new HashSet();
  Map propsMap = new HashMap();

  public Preferences getNode(String path, boolean bCreate) {
    try {
      PreferencesImpl pi = (PreferencesImpl)prefs.get(path);
      if(pi != null) {
        return pi;
      }

      File nodeDir = getNodeDir(path, bCreate);
      File keyFile = getKeyFile(path);

      if(!keyFile.exists()) {
        Dictionary props = new Hashtable();
        propsMap.put(path, props);
        saveProps(path, props);
      }

      pi = new PreferencesImpl(this, path);
      prefs.put(path, pi);
      return pi;
    } catch (Exception e) {
      throw new IllegalStateException("getNode " + path + " failed: " + e);
    }
  }

  public boolean     nodeExists(String path) {
    synchronized(lock) {
      boolean b = false;

      File f = getNodeDir(path, false);
      int ix = path.lastIndexOf('/');

      if(ix != -1 && path.length() > 0) {
        String last  = decode(path.substring(ix + 1));
        String fname = f.getAbsolutePath();

        try {
          fname = f.getCanonicalPath();
        } catch (IOException e) {
          Activator.log.warn("failed to get canonical path of " + path, e);
        }

        fname = decode(fname);
        if(fname.endsWith(last)) {
          b = f.exists();
        }
      } else {
        b = f.exists();
      }
      return b;
    }
  }

  public void flush(String path) {
    synchronized(lock) {
      // save any cached data to storage
      if(path == null) {
        // save all
      } else {
        synchronized(dirtySet) {
          // System.out.println("flushing " + dirtySet.size() + " items");
          for(Iterator it = dirtySet.iterator(); it.hasNext();) {
            String p = (String)it.next();
            Dictionary props = (Dictionary)propsMap.get(p);
            if(props != null) {
              //              System.out.println("flush '" + p + "'");
              try {
                saveProps(p, props);
              } catch (Exception e) {
              }
            }
          }
          dirtySet.clear();
        }
      }
    }
  }

  /**
   * Get file which store key/value pairs
   */
  File getKeyFile(String path) {
    return new File(getNodeDir(path, true), KEYFILE_NAME);
  }

  void deleteTree(File f) {
    synchronized(lock) {
      if(f.exists()) {
        if(f.isDirectory()) {
          String[] children = f.list();
          for(int i = 0; i < children.length; i++) {
            deleteTree(new File(f, children[i]));
          }
        }
        f.delete();
      }
    }
  }

  String encode(String s) {
    s = Text.replace(s, ".", "__dot__");
    s = Text.replace(s, " ", "__space__");
    s = Text.replace(s, "?", "__q__");
    s = Text.replace(s, "\\", "__bslash__");
    return s;
  }

  String decode(String s) {
    s = Text.replace(s, "__space__",   " ");
    s = Text.replace(s, "__dot__",   ".");
    s = Text.replace(s, "__?__",     "?");
    s = Text.replace(s, "__bslash__", "\\");
    return s;
  }

}
