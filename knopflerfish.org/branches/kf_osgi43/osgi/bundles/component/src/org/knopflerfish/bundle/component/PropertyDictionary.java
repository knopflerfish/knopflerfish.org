/*
 * Copyright (c) 2010-2013, KNOPFLERFISH project
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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.service.component.ComponentConstants;


/**
 * This class needs to be a Dictionary and a Map.
 * TBD, check that this class is immutable
 */
class PropertyDictionary extends Dictionary<String,Object>
{

  final private Hashtable<String,Object> props;

  /**
   *
   */
  PropertyDictionary(Component comp,
                     Dictionary<String,Object> cm,
                     Dictionary<String,Object> instance,
                     boolean service) {
    props = new Hashtable<String,Object>();
    final ComponentDescription cd = comp.compDesc;
    addDict(cd.getProperties(), service);
    if (cm != null) {
      addDict(cm, service);
    }
    if (instance != null) {
      addDict(instance, service);
    }
    props.put(ComponentConstants.COMPONENT_ID, comp.id);
    props.put(ComponentConstants.COMPONENT_NAME, cd.getName());
  }


  /**
   *
   */
  PropertyDictionary(Hashtable<String,Object> props) {
    this.props = props;
  }


  /**
   *
   */
  @Override
  public Enumeration<Object> elements() {
    return props.elements();
  }


  /**
   *
   */
  @Override
  public Object get(Object key) {
    return props.get(key);
  }


  /**
   *
   */
  @Override
  public boolean isEmpty() {
    return props.isEmpty();
  }


  /**
   *
   */
  @Override
  public Enumeration<String> keys() {
    return props.keys();
  }


  /**
   *
   */
  @Override
  public Object put(String key, Object value) {
    throw new RuntimeException("Operation not supported.");
  }


  /**
   *
   */
  @Override
  public Object remove(Object key) {
    throw new RuntimeException("Operation not supported.");
  }


  /**
   *
   */
  @Override
  public int size() {
    return props.size();
  }

  //
  // Package methods
  //

  /**
   *
   */
  Dictionary<String,Object> writeableCopy() {
    return new Hashtable<String, Object>(props);
  }

  //
  // Private methods
  //

  /**
   * Add all properties in the given properties object to the props dictionary.
   *
   * @param properties The properties object to insert the contents of.
   * @param service If the component is a service skip non-public properties.
   *                I.e., those with a key starting with '.'.
   */
  private void addDict(Properties properties, boolean service) {
    for (final Enumeration<Object> e = properties.keys(); e.hasMoreElements(); ) {
      final String key = (String) e.nextElement();
      addDict(key, properties.get(key), service);
    }
  }
  /**
   * Add all properties in the given dictionary to the props dictionary.
   *
   * @param properties The dictionary to insert the contents of.
   * @param service If the component is a service skip non-public properties.
   *                I.e., those with a key starting with '.'.
   */
  private void addDict(Dictionary<String,Object> d, boolean service) {
    for (final Enumeration<String> e = d.keys(); e.hasMoreElements(); ) {
      final String key = e.nextElement();
      addDict(key, d.get(key), service);
    }
  }
  /**
   * Add a property in the given dictionary to the props dictionary.
   *
   * @param key     The key of the property to insert.
   * @param value   The value of the property to insert.
   * @param service If the component is a service skip non-public properties.
   *                I.e., those with a key starting with '.'.
   */
  private void addDict(String key, Object value, boolean service) {
    if (!service || !key.startsWith(".")) {
      props.put(key, value);
    }
  }
}
