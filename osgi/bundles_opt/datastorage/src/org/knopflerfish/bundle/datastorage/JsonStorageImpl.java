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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

import com.google.gson.reflect.TypeToken;

import org.osgi.framework.Bundle;

// import org.knopflerfish.service.datastorage.JsonNode;
import org.knopflerfish.service.datastorage.JsonStorageNode;
import org.knopflerfish.service.datastorage.JsonStorage;
import org.knopflerfish.service.datastorage.DataStorageNode;
import org.knopflerfish.service.datastorage.JsonGenericStorageNode;


public class JsonStorageImpl implements JsonStorage {

  final Bundle bundle;
  
  final JsonFileStorage jsonStorage;
   
  JsonStorageImpl(final Bundle bundle) {
    this.bundle = bundle;
    this.jsonStorage = JsonFileStorage.createJsonStorage(bundle);
  }


   @Override
   public void removeNode(String pathName) {
     jsonStorage.removeNode(pathName);
   }
   
   @Override
   public boolean nodeExists(String pathName) {
    return jsonStorage.nodeExists(pathName);
   }

  @Override
  public <T> JsonStorageNode<T> getNode(String pathName, Class<T> classOfT) {
    return (JsonStorageNode<T>) jsonStorage.getNode(pathName, classOfT, true);
  }

  @Override
  public <T> JsonGenericStorageNode<T> getNode(String pathName, Type typeOfT) {
    return (JsonGenericStorageNode<T>) jsonStorage.getNode(pathName, typeOfT, true);
  }

  @Override
  public Collection<String> getChildNodePathNames(String pathName) {
    return jsonStorage.getChildrenPathNames(pathName);
  }
  
}
