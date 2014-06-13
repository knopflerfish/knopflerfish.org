/*
 * Copyright (c) 2003,2013, KNOPFLERFISH project
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

package org.knopflerfish.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Timeout version of a <code>HashMap</code>.
 *
 * <p>
 * Entries in the map have a limited life-length, and the <code>get</code>
 * method will only return the stored object during a limited time period.
 * </p>
 *
 * <p>
 * After the timeout period, the object will be removed.
 * </p>
 *
 * @see CachedObject
 */
public class CacheMap<K, V>
  implements Map<K, V>, Serializable
{
    private static final long serialVersionUID = 1L;
    private final HashMap<K,CachedObject<V>> map
      = new HashMap<K, CachedObject<V>>();

    // Timeout period in milliseconds for stored objects
    protected long timeout;

    /**
     * Create a map with default timeout period.
     *
     * <p>
     * Equivalent to <code>CacheMap(CachedObject.DEFAULT_TIMEOUT)</code>
     * </p>
     *
     * @see CachedObject#DEFAULT_TIMEOUT
     * @see CachedObject
     */
    public CacheMap() {
        this(CachedObject.DEFAULT_TIMEOUT);
    }

    /**
     * Create a map with a specified timeout period.
     *
     * <p>
     * After an object has been stored in the map using <code>put</code>, it
     * will only be available for <code>timeout</code> milliseconds. After
     * that period, <code>get</code> will return <code>null</code>
     * </p>
     *
     * @param timeout
     *            timeout period in milliseconds for cached objects.
     */
    public CacheMap(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Get an object from the map. If the object's timeout period has expired,
     * remove it from the map and return <code>null</code>.
     *
     * <p>
     * <b>Note</b>: Since the timeout check is done for each cached object, be
     * careful when looping over a map using iterators or other sequences of
     * <code>get</code>. The result from a new <code>get</code> can become
     * <code>null</code> faster than you might think. Thus <b>always</b>
     * check for <code>null</code> return values from <code>get</code>.
     * </p>
     */
    public V get(Object key) {

        CachedObject<V> cache = map.get(key);

        if (cache == null) {
            return null;
        }
        V obj = cache.get();
        if (obj == null) {
            remove(key);
        }
        return obj;
    }

    /**
     * Check if an object exists in the map. If non-existent or expired, return
     * <code>false</code>.
     */
    public boolean containsKey(Object key) {
        return null != get(key);
    }

    /**
     * Store an object in the map. The object's timeout period will start from
     * the current system time.
     */
    public V put(K key, V value) {

        CachedObject<V> cache = map.get(key);

        if (cache == null) {
            map.put(key, new CachedObject<V>(value, timeout));
            return null;
        }
        V old = cache.get();
        cache.set(value);
        return old;
    }

  /**
   * Force timeout check on all items in map and remove all expired keys and
   * objects.
   *
   * <p>
   * Normally, the timeout check is only done at <code>get</code> calls, but if
   * necessary (for example at periodic complete printouts or before loops) you
   * can (and probably want to) force a timeout check on all cached objects.
   * </p>
   */
  public void update()
  {
    for (Iterator<Entry<K, CachedObject<V>>> it = map.entrySet().iterator();
         it.hasNext();) {
      final Entry<K, CachedObject<V>> entry = it.next();
      if (entry.getValue().get() == null) {
        it.remove();
      }
    }
  }

  public int size()
  {
    // Note that some elements may be "removed"
    return map.size();
  }

  public boolean isEmpty()
  {
    // Note that some elements may be "removed"
    return map.isEmpty();
  }

  public boolean containsValue(Object value)
  {
    if (null != value) {
      for (CachedObject<V> cache : map.values()) {
        if (value.equals(cache.get()))
          return true;
      }
    }
    return false;
  }

    public V remove(Object key)
    {
      CachedObject<V> cache = map.remove(key);

      if (cache == null) {
          return null;
      }
      V obj = cache.get();
      return obj;
    }

    public void putAll(Map<? extends K, ? extends V> m)
    {
       for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
         put(entry.getKey(), entry.getValue());
       }
    }

    public void clear()
    {
      map.clear();
    }

    public Set<K> keySet()
    {
      return new HashSet<K>(map.keySet());
    }

    public Collection<V> values()
    {
      List<V> res = new ArrayList<V>();
      for (K key : map.keySet()) {
        V val = get(key);
        if (null!=val) res.add(val);
      }
      return res;
    }

    public Set<java.util.Map.Entry<K, V>> entrySet()
    {
      // Not very useful since map.entrySet() will behave strange!
      throw new UnsupportedOperationException("entrySet()");
    }

} // CacheMap
