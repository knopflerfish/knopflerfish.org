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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import com.google.gson.Gson;

import org.knopflerfish.service.datastorage.JsonGenericStorageNode;

public class JsonGenericStorageNodeImpl<T> extends DataStorageNodeImpl implements JsonGenericStorageNode<T> {
  
  private final Gson gson;
  private final Type type;

  // AbstractDataStorage storage;

  JsonGenericStorageNodeImpl(AbstractDataStorage storage, String path, Type type) {
    super(storage, path, DataStorageNodeType.JSON);
    Activator.log.info("Created Storage for: " + type.getClass());
    this.type = type;
    this.gson = new Gson();
  }


  @Override
  public void put(Object o) {
    Writer w = storage.getWriter(path);
    gson.toJson(o, this.type, w);
    try {
      w.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }       
  }


  @Override
  public T get() {
    // TODO Auto-generated method stub
    Reader r = storage.getReader(path);
    T val = gson.fromJson(r, this.type);
    //    T val = gson.fromJson(r, classOfNode);
    try {
      r.close();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return val;
  }

  @Override
  public Type getGenericType() {
    return type;
  }

}

 