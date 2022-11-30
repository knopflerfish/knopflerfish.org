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

package org.knopflerfish.service.datastorage;

import java.lang.reflect.Type;

/**
 * Datastorage service for storing json objects.
 * @author Makewave AB
 */
public interface JsonStorage extends DataStorage {
  
  
  /**
   * Returns a JsonStorageNode. If it does not exist a new node is created.
   * It is not suitable to use this method of class is of a generic type. In that case 
   * use the {@link #getNode(String, Type)} instead.
   * @param pathName Path to the node
   * @param classOfT class of T
   */
  <T> JsonStorageNode<T> getNode(String pathName, Class<T> classOfT);
  
  
  /**
   * Returns a JsonStorageNode. If it does not exist a new node is created. 
   * This method should be used for generic objects. For non-generic objects
   *  {@link #getNode(String, Class)} may be used instead.
   * @param pathName path to node
   * @param typeOfT The type of the node
   */
  <T> JsonGenericStorageNode<T> getNode(String pathName, Type typeOfT);
}
