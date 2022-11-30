/*
 * Copyright (c) 2018-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.datastorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.osgi.framework.Bundle;

import org.knopflerfish.service.datastorage.DataStorageNode;
import org.knopflerfish.service.datastorage.DataStorageNode.DataStorageNodeType;
import org.knopflerfish.service.datastorage.JsonGenericStorageNode;
import org.knopflerfish.service.datastorage.JsonStorageNode;
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
public class  JsonFileStorage implements AbstractDataStorage {

 
  private static final String JSON_FILENAME = ".data.json";
  private static final String DATA_STORAGE_NODE_TYPE_FILE = ".type";
  

  /**
   * The root directory of the preferences file storage.
   *
   * Contains one subdir for each bundle that fetches a preferences
   * service instance. The bundle ID is the name of the subdir.
   *
   * Default location is a directory named <tt>datastoragedir</tt> in the
   * current working directory (as given by the system property
   * <tt>user.dir</tt>). The root directory may be specified by setting
   * the system property <tt>org.knopflerfish.datastorage.dir</tt>.
   */
  private static File getDataStorageRootDir()
  {
    if (null==Activator.bc) {
      throw new IllegalStateException("DataStorage has been stopped!");
    }
    String datastorageDir = Activator.bc.getProperty("org.knopflerfish.datastorage.dir");
    if (null==datastorageDir || datastorageDir.length()==0 ) {
      datastorageDir = "datastoragedir";
    }
    return new File(datastorageDir);
  }

   static JsonFileStorage createJsonStorage(Bundle bundle)  {
    File baseDir = new File(getDataStorageRootDir(), bundle.getSymbolicName());
    Activator.log.info("Creating JsonStorage handle at: " + baseDir);
    return new JsonFileStorage(baseDir);
  }
  
  
  /**
   * Removes all preferences trees for the given bundle.
   */
  @SuppressWarnings("unused")
  static void cleanup(Bundle bundle)
  {
    
  }

  final File baseDir;

  final Object lock = new Object();

  /** Set to true when the entire datastorage tree repesented by this storage
   ** have been removed.
   **/
  boolean bStale = false;

  
  private JsonFileStorage(final File baseDir) {
    this.baseDir = AccessController.doPrivileged((PrivilegedAction<File>) () -> {
      //noinspection ResultOfMethodCallIgnored
      baseDir.mkdirs();

      if(!baseDir.exists() || !baseDir.isDirectory()) {
        throw new RuntimeException
            ("Failed to create root directory for datastorage: '"
                + baseDir +"'.");
      }
      return baseDir;
    });
  }

  public boolean isStale()  {
    return bStale;
  }

  
  public Collection<String> getChildrenNames(final String path) {
    synchronized(lock) {
      try {
        final File dir = getNodeDir(path, false);
        final String[] listing = dir.list();
        if (listing == null) {
          return Collections.emptyList();
        }

        final ArrayList<String> list = new ArrayList<>();
        for (String s : listing) {
          if (s.startsWith("."))
            continue;
          list.add(s);
        }
        // return Arrays.asList(listing).iterator();
        // return list.iterator();
        return list;
        
//        final Vector<String> v = new Vector<String>();
//        for(int i = 0; i < f.length; i++) {
//          if(!f[i].startsWith(".")) {
//            // Use decodeUser() here since it may be user names and
//            // its safe to use that decoder on this type of encoded
//            // strings.
//            v.addElement(decodeUser(f[i]));
//          }
//        }
//        final String[] f2 = new String[v.size()];
//        v.copyInto(f2);
//        return f2;
      } catch (final Exception e) {
        throw new IllegalStateException("Failed to get children from '"
            + path + "'");
      }
    }
  }

  public Collection<String> getChildrenPathNames(final String path) {
    synchronized(lock) {
      try {
        final File dir = getNodeDir(path, false);
        
        final String[] listing = dir.list();
        if (listing == null) {
          return Collections.emptyList();
        }

        final ArrayList<String> list = new ArrayList<>();
        for (String s : listing) {
          if (s.startsWith("."))
            continue;
          list.add(path + "/" + s);
        }
        return list;
      } catch (final Exception e) {
        throw new IllegalStateException("Failed to get children from '"
            + path + "'");
      }
    }
  }
 
  public void removeNode(final String path) {
    synchronized(lock) {
      try {
        if("".equals(path)) {
          bStale = true;
        }

        final Iterator<Entry<String, DataStorageNodeImpl>>
          it = datastorage.entrySet().iterator();
        while (it.hasNext()) {
          final Entry<String, DataStorageNodeImpl> entry = it.next();
          final String p = entry.getKey();
          final DataStorageNodeImpl pi = entry.getValue();

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
  final Map<String, DataStorageNodeImpl> datastorage = new HashMap<>();
  
  @Override
  public DataStorageNode getNode(String path) {
    return datastorage.get(path);  
  }
  
  public <T> JsonGenericStorageNode<T> getNode(String path, Type typeOfT, boolean create) {
    DataStorageNodeImpl node = datastorage.get(path);
    
    if (node != null) {
      if (node instanceof JsonGenericStorageNodeImpl<?>) {
        //noinspection unchecked
        return (JsonGenericStorageNode<T>) node;
      }
      else {
        throw new IllegalArgumentException("Not the right type of node: " + path);
      }
    }
    try {
      File nodeDir = getNodeDir(path, create);
      DataStorageNodeType nodeType = readTypeInfo(nodeDir);
      if (nodeType != null && nodeType != DataStorageNodeType.JSON_GENERIC) {
        throw new IllegalStateException("Unexpected type: " + nodeType);
      }
      final JsonGenericStorageNodeImpl<T> nodeImpl = new JsonGenericStorageNodeImpl<>(this, path, typeOfT);
      datastorage.put(path, nodeImpl);
      writeTypeInfo(nodeDir, DataStorageNodeType.JSON_GENERIC);
      return nodeImpl;
    } catch (Exception e) {
      if (e instanceof PrivilegedActionException) {
        e = (Exception) e.getCause();
      }
      throw new IllegalStateException("getNode " + path + " failed: " + e, e);
    }
  }
  
  @Override
  public <T> JsonStorageNode<T> getNode(final String path, final Class<T> classOfNode, boolean create) {
    try {
      DataStorageNodeImpl node = (DataStorageNodeImpl)getNode(path);

      if (node != null) {
        if (node instanceof JsonStorageNodeImpl<?>) {
          //noinspection unchecked
          return (JsonStorageNode<T>) node;
        }
        else {
          throw new IllegalArgumentException("Not the right type of node: " + path);
        }
      }

      File nodeDir = getNodeDir(path, create);
      DataStorageNodeType nodeType = readTypeInfo(nodeDir);
      if (nodeType != null && nodeType != DataStorageNodeType.JSON) {
        throw new IllegalStateException("Unexpected type: " + nodeType);
      }
     
      node = new JsonStorageNodeImpl<>(this, path, classOfNode);
      datastorage.put(path, node);
      writeTypeInfo(nodeDir, DataStorageNodeType.JSON);
      //noinspection unchecked
      return (JsonStorageNode<T>) node;
    } catch (Exception e) {
      if (e instanceof PrivilegedActionException) {
        e = (Exception) e.getCause();
      }
      throw new IllegalStateException("getNode " + path + " failed: " + e, e);
    }
  }

 
  public boolean nodeExists(final String path) {
    synchronized(lock) {
      final File f = getNodeDir(path, false);
      final int ix = path.lastIndexOf('/');

      return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
        boolean b = false;
        if (ix != -1 && path.length() > 0) {
          final String last = decode(path.substring(ix + 1));
          String fname = f.getAbsolutePath();

          try {
            fname = f.getCanonicalPath();
          } catch (final IOException e) {
            logWarn("failed to get canonical path of " + path, e);
          }

          fname = decode(fname);
          if (fname.endsWith(last)) {
            b = f.exists();
          }
        } else {
          b = f.exists();
        }
        return b;
      });
    }
  }

  

  public void logWarn(final String msg, final Throwable t)
  {
    Activator.log.warn(msg, t);
  }

 

  static void deleteTree(final File f) {
    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
      JsonFileStorage.deleteTree0(f);
      return null;
    });
  }

  // note: needs to be called in privileged action
  private static void deleteTree0(final File f) {
    if(f.exists()) {
      final String[] children = f.list();
      if (children != null) {
        for (final String element : children) {
          deleteTree0(new File(f, element));
        }
      }
      //noinspection ResultOfMethodCallIgnored
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

  
//  public DataStorageNode<JsonNode> node(String pathName) {
//    return node(pathName, true);
//  }
//
//  DataStorageNode<JsonNode> node(String pathName, boolean create) {
//    File f = getNodeDir(pathName, create);
//    return JsonFileStorage.createJsonStorage(pathName);
//  }
  
  
  File getNodeDir(String path, boolean bCreate) {
    synchronized(lock) {
      if (!"".equals(path)) {
        if(!path.startsWith("/")) {
          throw new IllegalArgumentException("Path must be absolute, is '" +
                                             path + "'");
        }
        path = path.substring(1);
      }
      //final File file = new File(baseDir, encode(path));
      final File file = new File(baseDir, path);
      final String path2 = path;
      if(bCreate) {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
          //noinspection ResultOfMethodCallIgnored
          file.mkdirs();
          if (!file.exists() || !file.isDirectory()) {
            throw new RuntimeException("Failed to create node dir=" +
                file.getAbsolutePath() + " from path " + path2);
          }
          return null;
        });
      }
      return file;
    }
  }

  @Override
  public Collection<DataStorageNode> getChildren(String path) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void clear(String path) {
    // TODO Auto-generated method stub
    
  }

//  @Override
//  public void put(String path, String value) {
//    File f = getNodeDir(path, false);
//    if (f == null) {
//      throw new IllegalArgumentException("Can not store value in non-existing node: " + path);
//    }
//    
//    try {
//      PrintWriter pw = new PrintWriter(f.getAbsolutePath() + "/.value.json", "UTF-8");
//      pw.write(value);
//    } catch (FileNotFoundException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (UnsupportedEncodingException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//    
//  }

//  @Override
//  public String get(String path) {
//    // TODO Auto-generated method stub
//    return null;
//  }

  @Override
  public Writer getWriter(final String path) {
    Activator.log.info("Returning writer for node: " + path);
    File f = getNodeDir(path, false);
    try {
      Activator.log.info("Returning writer for: " + f.getAbsolutePath() + JSON_FILENAME);
      return new FileWriter(f.getAbsolutePath() + "/" + JSON_FILENAME);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Reader getReader(final String path) {
    File f = getNodeDir(path, false);
    
    try {
      return new FileReader(f.getAbsolutePath() + "/" + JSON_FILENAME);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  private void writeTypeInfo(final File node, final DataStorageNodeType type) {
    // File f = getNodeDir(path, false);
    try {
      PrintWriter pw = new PrintWriter(node.getAbsolutePath() + "/" + DATA_STORAGE_NODE_TYPE_FILE);
      pw.println(type.name());
      pw.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private DataStorageNodeType readTypeInfo(final File node) {
    File typeFile = getNodeTypeFile(node);
    if (!typeFile.exists()) {
      return null;
    }
    
    try {
      BufferedReader br = new BufferedReader(new FileReader(typeFile));
      String val = br.readLine();
      br.close();
        
      if (val != null)
        return DataStorageNodeType.valueOf(val);
      else
        throw new IllegalArgumentException("No type info present at: " + node);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Failed to read type info at: " + node);
    }
  }
  
  private File getNodeTypeFile(final File node) {
    return new File(node.getAbsolutePath() + "/" + DATA_STORAGE_NODE_TYPE_FILE);
  }
  
}
