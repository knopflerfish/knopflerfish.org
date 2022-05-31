/*
 * Copyright (c) 2018, KNOPFLERFISH project
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.knopflerfish.service.datastorage.DataStorage;
import org.knopflerfish.service.datastorage.DataStorageNode;
// import org.knopflerfish.util.Base64;

public abstract class DataStorageNodeImpl implements DataStorageNode {

  AbstractDataStorage storage;
  String       path;
  String       parentPath;
  String       name;
  DataStorageNodeType nodeType;
  
  // Class<?>     classOfNode;
  
  public boolean bStale = false;

  public DataStorageNodeImpl(AbstractDataStorage storage, String path, DataStorageNodeType type) {
    this.path    = path;
    this.storage = storage;
    this.nodeType = type;
    
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

  public DataStorageNodeType getType() {
    return nodeType;
  }
  
  public String getAbsolutePath() {
    return "".equals(path) ? "/" : path;
  }


  public void clear() {
    assertValid();
    storage.clear(path);
  }

  

  public String getName() {
    return name;
  }

//  public Data node()  {
//    return (T) storage.getNode(path, true);
//  }


//  public boolean nodeExists(String pathName) {
//    assertPath(pathName);
//
//    if(bStale) {
//      if(!"".equals(pathName)) {
//        throw new IllegalStateException("node removed");
//      }
//    }
//
//    return storage.nodeExists(absPath(pathName));
//  }

  public DataStorageNode getParent() {
    assertValid();

    if(parentPath == null) {
      return null;
    }

   return storage.getNode(parentPath);
  }

 


//  public void removeNode() {
//    assertValid();
//    storage.removeNode(path);
//    bStale = true;
//  }



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

  @Override
  public String toString() {
    return "Preferences[" +
      "path=" + path +
      ", name=" + name +
      ", parentPath=" + parentPath +
      ", bStale=" + bStale +
      "]";
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if(other == null || !(other instanceof DataStorageNodeImpl)) {
      return false;
    }
    DataStorageNodeImpl pi = (DataStorageNodeImpl)other;
    return path.equals(pi.path);
  }


  @Override
  public Collection<DataStorageNode> children() {
//    return (Iterator<DataStorageNode>) Collections.unmodifiableCollection(storage.getChildren(path));
    return Collections.unmodifiableCollection(storage.getChildren(path));
  }

  @Override
  public Collection<String> childNames() {
    return storage.getChildrenNames(path);
  }

}

