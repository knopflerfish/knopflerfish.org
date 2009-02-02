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

package org.knopflerfish.bundle.prefs;

import org.osgi.framework.*;
import org.osgi.service.prefs.*;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.Base64;
import java.util.*;
import java.io.*;

public class PreferencesImpl implements Preferences {

  PrefsStorage storage;
  String       path;
  String       parentPath;
  String       name;

  public boolean bStale = false;

  public PreferencesImpl(PrefsStorage storage, String path) {
    this.storage = storage;
    this.path    = path;

    if("/".equals(path)) {
      path = "";
    }
    int ix = path.lastIndexOf("/");
    if(ix != -1) {
      name       = path.substring(ix + 1);
      parentPath = path.substring(0, ix);
    } else {
      name       = "";
      parentPath = null;
    }
  }

  public String absolutePath() {
    return "".equals(path) ? "/" : path;
  }

  public String[] childrenNames() {
    assertValid();
    return storage.getChildrenNames(path);
  }

  public void clear() {
    assertValid();
    storage.removeAllKeys(path);
  }

  public void flush() throws BackingStoreException {
    assertValid();
    storage.flush(path);
  }


  public String get(String key, String def) {
    if(key == null) {
      throw new NullPointerException("Null key");
    }
    assertValid();
    return storage.get(path, key, def);
  }

  public boolean getBoolean(String key, boolean def) {
    return "true".equals(get(key, def ? "true" : "false"));
  }

  public byte[] getByteArray(String key, byte[] def) {

    String s = get(key, null);
    if(s == null) {
      return def;
    } else {
      try {
        return Base64.decode(s.getBytes());
      } catch (IOException e) {
        storage.logWarn("Failed to decode byte array", e);
        return def;
      }
    }
  }

  public double getDouble(String key, double def) {
    try {
      return Double.parseDouble(get(key, Double.toString(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }

  public float getFloat(String key, float def) {
    try {
      return Float.parseFloat(get(key, Float.toString(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }

  public int getInt(String key, int def) {
    try {
      return Integer.parseInt(get(key, Integer.toString(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }
  public long getLong(String key, long def) {
    try {
      return Long.parseLong(get(key, Long.toString(def)));
    } catch (NumberFormatException e) {
      return def;
    }
  }

  public String[] keys() throws BackingStoreException {
    assertValid();
    try {
      return storage.getKeys(path);
    } catch (Exception e) {
      storage.logWarn("keys: Failed to get keys for " + path, null);
      throw new BackingStoreException(e.getMessage());
    }
  }

  public String name() {
    return name;
  }



  public Preferences node(String pathName)  {

    assertPath(pathName);
    assertValid();

    return storage.getNode(absPath(pathName), true);
  }


  public boolean nodeExists(String pathName) {
    assertPath(pathName);

    if(bStale) {
      if(!"".equals(pathName)) {
        throw new IllegalStateException("node removed");
      }
    }

    return storage.nodeExists(absPath(pathName));
  }

  public Preferences parent() {
    assertValid();

    if(parentPath == null) {
      return null;
    }

    return storage.getNode(parentPath, false);
  }

  public void put(String key, String value)  {
    if(key == null) {
      throw new NullPointerException("null key");
    }
    if(value == null) {
      throw new NullPointerException("null value");
    }
    assertValid();

    try {
      storage.put(path, key, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to store " + key + ", " + e);
    }
  }

  public void putByteArray(String key, byte[] value)  {
    if(key == null) {
      throw new NullPointerException("null key");
    }
    if(value == null) {
      throw new NullPointerException("null value");
    }
    assertValid();

    try {
      put(key, Base64.encode(value));
    } catch (IOException e) {
      throw new RuntimeException("Failed to encode byte array, " + e);
    }
  }


  public void putBoolean(String key, boolean value)  {
    put(key, value ? "true" : "false");
  }


  public void putDouble(String key, double value)   {
    put(key, Double.toString(value));
  }

  public void putFloat(String key, float value)   {
    put(key, Float.toString(value));
  }

  public void putInt(String key, int value)   {
    put(key, Integer.toString(value));
  }

  public void putLong(String key, long value)  {
    put(key, Long.toString(value));
  }

  public void remove(String key)  {
    assertValid();
    storage.removeKey(path, key);
  }

  public void removeNode() {
    assertValid();
    storage.removeNode(path);
    bStale = true;
  }

  public void sync() throws BackingStoreException {
    assertValid();
    storage.sync(path);
  }

  protected String absPath(String pathName) {
    if(pathName.startsWith("/")) {
      return pathName;
    }
    if("".equals(pathName)) {
      return path;
    } else {
      return path + "/" + pathName;
    }
  }

  protected void assertValid() {
    if(bStale || !storage.nodeExists(path)) {
      throw new IllegalStateException("no node at '" + path + "'");
    }
  }

  protected static void assertPath(String pathName) {
    if(-1 != pathName.indexOf("//")) {
      throw new IllegalArgumentException("Illegal // in path name '" + pathName + "'");
    }
    if(pathName.length() > 1 && pathName.endsWith("/")) {
      throw new IllegalArgumentException("Trailing / in path name '" +
                                         pathName + "'");
    }
  }

  public String toString() {
    return "Preferences[" +
      "path=" + path +
      ", name=" + name +
      ", parentPath=" + parentPath +
      ", bStale=" + bStale +
      "]";
  }

  public int hashCode() {
    return path.hashCode();
  }

  public boolean equals(Object other) {
    if(other == null || !(other instanceof PreferencesImpl)) {
      return false;
    }
    PreferencesImpl pi = (PreferencesImpl)other;
    return path.equals(pi.path);
  }
}

