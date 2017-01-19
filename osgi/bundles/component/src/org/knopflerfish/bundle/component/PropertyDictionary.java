/*
 * Copyright (c) 2010-2017, KNOPFLERFISH project
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

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;


/**
 * This class needs to be a Dictionary and a Map.
 * TBD, check that this class is immutable
 */
class PropertyDictionary extends Dictionary<String, Object>
  implements Map<String, Object>, Comparable<Map<String, Object>>
{

  final private Hashtable<String,Object> props;

  /**
   *
   */
  PropertyDictionary(Component comp,
                     Map<String,Object> cm,
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
  PropertyDictionary(ServiceReference<?> sr) {
    props = new Hashtable<String,Object>();
    for (String key : sr.getPropertyKeys()) {
      props.put(key, sr.getProperty(key));
    }
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
  public boolean equals(Object o) {
    return props.equals(o);
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
  public int hashCode() {
    return props.hashCode();
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
    throw new UnsupportedOperationException();
  }


  /**
   *
   */
  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }


  /**
   *
   */
  @Override
  public int size() {
    return props.size();
  }


  /**
   *
   */
  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }


  /**
   *
   */
  @Override
  public boolean containsKey(Object key) {
    return props.containsKey(key);
  }


  /**
   *
   */
  @Override
  public boolean containsValue(Object value) {
    return props.containsValue(value);
  }


  /**
   *
   */
  @Override
  public Set<Map.Entry<String, Object>> entrySet() {
    return Collections.unmodifiableSet(props.entrySet());
  }


  /**
   *
   */
  @Override
  public Set<String> keySet() {
    return Collections.unmodifiableSet(props.keySet());
  }


  /**
   *
   */
  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException();
  }


  /**
   *
   */
  @Override
  public Collection<Object> values() {
    return Collections.unmodifiableCollection(props.values());
  }


  /**
   *
   */
  @Override
  public int compareTo(Map<String, Object> that) {
    final Object ro1 = this.get(Constants.SERVICE_RANKING);
    final Object ro2 = that.get(Constants.SERVICE_RANKING);
    final int r1 = (ro1 instanceof Integer) ? ((Integer)ro1).intValue() : 0;
    final int r2 = (ro2 instanceof Integer) ? ((Integer)ro2).intValue() : 0;

    if (r1 != r2) {
      // use ranking if ranking differs
      return r1 < r2 ? -1 : 1;
    } else {
      final Long id1 = (Long)this.get(Constants.SERVICE_ID);
      Object id2 = that.get(Constants.SERVICE_ID);

      if (!(id2 instanceof Long)) {
        id2 = new Long(0);
      }
      // otherwise compare using IDs,
      // is less than if it has a higher ID.
      return -id1.compareTo((Long)id2);
    }
  }

  /**
   *
   */
  Map<String,Object> getMap() {
    return new HashMap<String, Object>(props);
  }

  //
  // Private methods
  //


  /**
   * Add all properties in the given properties object to the props dictionary.
   *
   * @param m The properties object to insert the contents of.
   * @param service If the component is a service skip non-public properties.
   *                I.e., those with a key starting with '.'.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void addDict(Map m, boolean service) {
    for (final Iterator<Map.Entry> i = m.entrySet().iterator(); i.hasNext(); ) {
      final Map.Entry<String, Object> e = i.next();
      addDict(e.getKey(), e.getValue(), service);
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
