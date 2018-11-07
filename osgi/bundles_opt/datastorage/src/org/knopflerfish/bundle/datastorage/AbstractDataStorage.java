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

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;

import org.knopflerfish.service.datastorage.DataStorageNode;
// import org.knopflerfish.service.datastorage.JsonNode;
import org.knopflerfish.service.datastorage.JsonGenericStorageNode;
import org.knopflerfish.service.datastorage.JsonStorageNode;

/**
 * Abstraction interface for actual storage of datastorage data
 *
 * <p>
 * All paths are absolute
 * </p>
 */
public interface AbstractDataStorage  {
  /**
   * @returns <tt>true</tt> if the preferences tree that the storage
   * object represents have been removed.
   */
  boolean     isStale();
  
  /**
   * Get list of childrens to a given path.
   */
  Collection<String>    getChildrenNames(final String path);
  
  Collection<String>    getChildrenPathNames(final String path);
  
  Collection<DataStorageNode> getChildren(final String path);
  

  void        removeNode(final String path);

  <T> JsonStorageNode<T> getNode(final String path, final Class<T> classOfNode, boolean create);
  
  <T> JsonGenericStorageNode<T> getNode(final String path, Type t, boolean create);
  
  DataStorageNode getNode(final String path);
  
  boolean     nodeExists(final String path);
  
  void        clear(final String path);
  
//  void put(final String path, final String value);
//  
//  String get(final String path);
  
  Writer getWriter(final String path);
  
  Reader getReader(final String path);

}
