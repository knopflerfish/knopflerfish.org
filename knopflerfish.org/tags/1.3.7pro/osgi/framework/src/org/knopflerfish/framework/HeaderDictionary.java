/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

import java.io.*;

import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.util.jar.*;

/**
 * Dictonary for Bundle Manifest headers.
 *
 * @author Jan Stein
 */
public class HeaderDictionary extends Dictionary
{

  private Hashtable headers;

  /**
   * Create an empty dictionary for manifest attributes.
   */
  public HeaderDictionary() {
    headers = new Hashtable();
  }


  /**
   * Create a dictionary from manifest attributes.
   */
  public HeaderDictionary(Attributes in) {
    headers = new Hashtable();
    for (Iterator i = in.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry)i.next();
      headers.put(e.getKey(), e.getValue());
    }
  }


  /**
   * Create a clone of an existing HeaderDictionary
   */
  HeaderDictionary(HeaderDictionary in) {
    if (in != null) {
      headers = (Hashtable)in.headers.clone();
    } else {
      headers = new Hashtable();
    }
  }


  /**
   * Returns an enumeration of the values in this dictionary.
   */
  public Enumeration elements() {
    return headers.elements();
  }


  /**
   * Returns the value to which the key is mapped in this dictionary.
   */
  public Object get(Object key) {
    return headers.get(new Attributes.Name((String)key));
  }


  /**
   * Tests if this dictionary maps no keys to value.
   */
  public boolean isEmpty() {
    return headers.isEmpty();
  }


  /**
   *  Returns an enumeration of the keys in this dictionary.
   */
  public Enumeration keys() {
    final Enumeration keys = headers.keys();
    return new Enumeration() {
      public boolean hasMoreElements() {
	return keys.hasMoreElements();
      }
      public Object nextElement() {
	return keys.nextElement().toString();
      }
    };
  }


  /**
   * Maps the specified key to the specified value in this dictionary.
   */
  public Object put(Object key, Object value) {
    return headers.put(new Attributes.Name((String)key), value);
  }


  /**
   * Removes the key (and its corresponding value) from this dictionary.
   */
  public Object remove(Object key) {
    return headers.remove(new Attributes.Name((String)key));
  }

  
  /** 
   * Returns the number of entries (dinstint keys) in this dictionary.
   */
  public int size() {
    return headers.size();
  }

}
