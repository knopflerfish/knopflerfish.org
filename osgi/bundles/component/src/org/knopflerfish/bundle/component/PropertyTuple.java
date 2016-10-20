/*
 * Copyright (c) 2016-2016, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import java.util.Map;
import java.util.Map.Entry;


class PropertyTuple
  implements Entry<Map<String,Object>,Object>, Comparable<Entry<Map<String,Object>,Object>>
{

  private final PropertyDictionary key;
  private final Object val;


  PropertyTuple(PropertyDictionary properties,
                Object service) {
    key = properties;
    val = service;
  }


  @Override
  public Map<String,Object> getKey() {
    return key;
  }


  @Override
  public Object getValue() {
    return val;
  }


  @Override
  public Object setValue(Object key) {
    throw new UnsupportedOperationException("Operation not supported.");
  }


  @Override
  public boolean equals(Object o) {
    if (o instanceof PropertyTuple) {
      PropertyTuple p = (PropertyTuple)o;
      return val.equals(p.getValue()) && key.equals(p.getKey());
    }
    return false;
  }


  @Override
  public int hashCode() {
    return key.hashCode() ^ val.hashCode();
  }


  @Override
  public int compareTo(Entry<Map<String,Object>,Object> that) {
    return key.compareTo(that.getKey());
  }

}
