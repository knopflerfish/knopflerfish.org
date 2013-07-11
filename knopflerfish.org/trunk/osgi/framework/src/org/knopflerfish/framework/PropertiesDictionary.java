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

package org.knopflerfish.framework;

import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Constants;

/**
 * Creates a copy of the properties associated with a service registration.
 * Checks that all the keys are strings and adds the class names.
 * Note! Creation of PropertiesDictionary must be synchronized.
 *
 * @author Jan Stein
 */
class PropertiesDictionary extends Dictionary<String, Object>
{
  private final String [] keys;
  private final Object [] values;
  private int size;

  private int ocIndex = -1;
  private int sidIndex = -1;

  private static long nextServiceID = 1;

  PropertiesDictionary(@SuppressWarnings("rawtypes") Dictionary in) {
    final int max_size = in != null ? in.size() + 2 : 2;
    keys = new String[max_size];
    values = new Object[max_size];
    size = 0;
    if (in != null) {
      try {
        for (@SuppressWarnings("rawtypes")
        final Enumeration e = in.keys(); e.hasMoreElements();) {
          final String key = (String) e.nextElement();
          if (ocIndex == -1 && key.equalsIgnoreCase(Constants.OBJECTCLASS)) {
            ocIndex = size;
          } else if (sidIndex == -1
                     && key.equalsIgnoreCase(Constants.SERVICE_ID)) {
            sidIndex = size;
          } else {
            for (int i = size - 1; i >= 0; i--) {
              if (key.equalsIgnoreCase(keys[i])) {
                throw new IllegalArgumentException(
                                                   "Several entries for property: "
                                                       + key);
              }
            }
          }
          keys[size] = key;
          values[size++] = in.get(key);
        }
      } catch (final ClassCastException ignore) {
        throw new IllegalArgumentException(
                                           "Properties contains key that is not of type java.lang.String");
      }
    }
  }

  PropertiesDictionary(@SuppressWarnings("rawtypes") Dictionary in,
                       String[] classes, Long sid)
  {
    this(in);
    if (ocIndex == -1) {
      keys[size] = Constants.OBJECTCLASS;
      ocIndex = size++;
    }
    values[ocIndex] = classes;
    if (sidIndex == -1) {
      keys[size] = Constants.SERVICE_ID;
      sidIndex = size++;
    }
    values[sidIndex] = sid != null ? sid : new Long(nextServiceID++);
  }

  @Override
  public Object get(Object key) {
    if (key == Constants.OBJECTCLASS) {
      return (ocIndex >= 0) ? values[ocIndex] : null;
    } else if (key == Constants.SERVICE_ID) {
      return (sidIndex >= 0) ? values[sidIndex] : null;
    }
    for (int i = size - 1; i >= 0; i--) {
      if (((String)key).equalsIgnoreCase(keys[i])) {
	return values[i];
      }
    }
    return null;
  }


  public String [] keyArray() {
    final String [] nkeys = new String[size];
    System.arraycopy(keys, 0, nkeys, 0, size);
    return nkeys;
  }


  @Override
  public int size() {
    return size;
  }

  // These aren't used but we implement to fulfill Dictionary class

  @Override
  public Enumeration<Object> elements() { throw new UnsupportedOperationException("Not implemented"); }

  @Override
  public boolean isEmpty() { throw new UnsupportedOperationException("Not implemented"); }

  @Override
  public Enumeration<String> keys() { throw new UnsupportedOperationException("Not implemented"); }

  @Override
  public Object put(String k, Object v) { throw new UnsupportedOperationException("Not implemented"); }

  @Override
  public Object remove(Object k) { throw new UnsupportedOperationException("Not implemented"); }
}
