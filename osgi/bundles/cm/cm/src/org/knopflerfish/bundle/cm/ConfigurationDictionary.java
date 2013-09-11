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

package org.knopflerfish.bundle.cm;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * This class implements the data-type restrictions, and case insensitive
 * lookup, required of configuration dictionaries in the CM.
 *
 * @author Gatespace AB *
 */

final class ConfigurationDictionary
  extends Dictionary<String, Object>
{

  private final static String IS_NULL_DICTIONARY = "org.knopflerfish.is.null.dictionary";
  private final static String KEY_CHANGE_COUNT = "org.knopflerfish.bundle.cm.changeCount";

  /**
   * * Use BigDecimal if available.
   */

  private static Class<?> classBigDecimal = null;
  static {
    try {
      classBigDecimal = Class.forName("java.math.BigDecimal");
    } catch (final Throwable ignore) {
      classBigDecimal = null;
    }
  }

  /**
   * * Allowed object types.
   */

  private final static Hashtable<Class<?>, Class<?>> allowedObjectTypes = new Hashtable<Class<?>, Class<?>>();
  static {
    allowedObjectTypes.put(Integer.class, Integer.class);
    allowedObjectTypes.put(Short.class, Short.class);
    allowedObjectTypes.put(Long.class, Long.class);
    allowedObjectTypes.put(String.class, String.class);
    allowedObjectTypes.put(Float.class, Float.class);
    allowedObjectTypes.put(Double.class, Double.class);
    allowedObjectTypes.put(Byte.class, Byte.class);
    allowedObjectTypes.put(BigInteger.class, BigInteger.class);
    if (classBigDecimal != null) {
      allowedObjectTypes.put(classBigDecimal, classBigDecimal);
    }
    allowedObjectTypes.put(Character.class, Character.class);
    allowedObjectTypes.put(Boolean.class, Boolean.class);
  }

  /**
   * * Allowed primitive types in arrays.
   */

  private final static Hashtable<Class<?>, Class<?>> allowedPrimitiveTypes
    = new Hashtable<Class<?>, Class<?>>();
  static {
    allowedPrimitiveTypes.put(Integer.TYPE, Integer.TYPE);
    allowedPrimitiveTypes.put(Short.TYPE, Short.TYPE);
    allowedPrimitiveTypes.put(Long.TYPE, Long.TYPE);
    allowedPrimitiveTypes.put(Float.TYPE, Float.TYPE);
    allowedPrimitiveTypes.put(Double.TYPE, Double.TYPE);
    allowedPrimitiveTypes.put(Byte.TYPE, Byte.TYPE);
    allowedPrimitiveTypes.put(Character.TYPE, Character.TYPE);
    allowedPrimitiveTypes.put(Boolean.TYPE, Boolean.TYPE);
  }

  private final static Hashtable<Class<?>, Class<?>> classToPrimitiveType
    = new Hashtable<Class<?>, Class<?>>();
  static {
    classToPrimitiveType.put(Integer.class, Integer.TYPE);
    classToPrimitiveType.put(Short.class, Short.TYPE);
    classToPrimitiveType.put(Long.class, Long.TYPE);
    classToPrimitiveType.put(Float.class, Float.TYPE);
    classToPrimitiveType.put(Double.class, Double.TYPE);
    classToPrimitiveType.put(Byte.class, Byte.TYPE);
    classToPrimitiveType.put(Character.class, Character.TYPE);
    classToPrimitiveType.put(Boolean.class, Boolean.TYPE);
  }

  /**
   * Mapping of keys in lower-case to the case they had when added to the
   * dictionary.
   */
  final Hashtable<String, String> lowercaseToOriginalCase;

  /**
   * A dictionary holding the key-value pairs of the configuration with the keys
   * in the original case they were entered in.
   */
  final Hashtable<String, Object> originalCase;


  public ConfigurationDictionary()
  {
    this(new Hashtable<String, Object>());
    setNullDictionary(true);
  }

  /**
   * Construct a ConfigurationDictionary wrapping an ordinary Dictionary. I.e.,
   * the ownership of the given dictionary will be taken over by the new
   * configuration dictionary instance.
   *
   * @param dictionary
   *          The original dictionary.
   */
  public ConfigurationDictionary(Hashtable<String, Object> dictionary)
  {
    this.lowercaseToOriginalCase = new Hashtable<String, String>();
    this.originalCase = dictionary;
    updateLowercaseToOriginalCase();
  }

  /**
   * Construct a ConfigurationDictionary by cloning another one. I.e., a clone
   * of the given dictionary will be used in the new configuration dictionary
   * instance.
   *
   * @param original
   *          The original configuration dictionary to clone.
   *
   */
  private ConfigurationDictionary(ConfigurationDictionary original)
  {
    this.originalCase = copyDictionary(original.originalCase);
    this.lowercaseToOriginalCase =
      new Hashtable<String, String>(original.lowercaseToOriginalCase);
  }

  @Override
  public Enumeration<Object> elements()
  {
    return originalCase.elements();
  }

  @Override
  public Object get(Object key)
  {
    Object val = originalCase.get(key);

    if (val != null) {
      return val;
    }

    final String lowercaseKey = ((String) key).toLowerCase();
    final String originalCaseKey = lowercaseToOriginalCase.get(lowercaseKey);
    if (originalCaseKey != null) {
      key = originalCaseKey;
    }

    val = originalCase.get(key);

    return val;
  }

  @Override
  public boolean isEmpty()
  {
    return originalCase.isEmpty();
  }

  @Override
  public Enumeration<String> keys()
  {
    return originalCase.keys();
  }

  @Override
  public String toString()
  {
    final StringBuffer sb = new StringBuffer();
    sb.append("ConfigurationDictionary{");
    for (final Enumeration<String> e = keys(); e.hasMoreElements();) {
      final String key = e.nextElement();
      final Object val = get(key);

      sb.append(key + "=" + val);
      if (e.hasMoreElements()) {
        sb.append(", ");
      }
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  public Object put(String key, Object value)
  {
    final String lowercaseKey = key.toLowerCase();
    final String originalCaseKey = lowercaseToOriginalCase.get(lowercaseKey);
    if (originalCaseKey != null) {
      key = originalCaseKey;
    }
    final Object o = originalCase.put(key, value);
    if (originalCaseKey == null) {
      updateLowercaseToOriginalCase(key);
    }
    return o;
  }

  @Override
  public Object remove(Object key)
  {
    final String lowercaseKey = ((String) key).toLowerCase();
    if (!lowercaseToOriginalCase.containsKey(lowercaseKey)) {
      return null;
    }
    final String originalCaseKey = lowercaseToOriginalCase.remove(lowercaseKey);
    return originalCase.remove(originalCaseKey);
  }

  @Override
  public int size()
  {
    return originalCase.size();
  }

  Hashtable<String, Object> getOriginal()
  {
    return originalCase;
  }

  ConfigurationDictionary createCopy()
  {
    return new ConfigurationDictionary(this);
  }

  ConfigurationDictionary createCopyIfRealAndRemoveLocation()
  {
    if (isNullDictionary()) {
      return null;
    }
    return createCopyAndRemoveLocation();
  }

  ConfigurationDictionary createCopyAndRemoveLocation()
  {
    final ConfigurationDictionary cd = createCopy();
    cd.removeLocation();
    return cd;
  }

  boolean isNullDictionary()
  {
    final Boolean b = (Boolean) get(IS_NULL_DICTIONARY);
    return b != null && b.booleanValue();
  }

  void setNullDictionary(boolean b)
  {
    if (b) {
      put(IS_NULL_DICTIONARY, Boolean.TRUE);
    } else {
      remove(IS_NULL_DICTIONARY);
    }
  }

  private void updateLowercaseToOriginalCase()
  {
    final Enumeration<String> keys = originalCase.keys();
    while (keys.hasMoreElements()) {
      final String originalKey = keys.nextElement();
      updateLowercaseToOriginalCase(originalKey);
    }
  }

  private void updateLowercaseToOriginalCase(String originalKey)
  {
    if (originalKey == null) {
      return;
    }
    final String lowercaseKey = originalKey.toLowerCase();
    if (!lowercaseToOriginalCase.containsKey(lowercaseKey)) {
      lowercaseToOriginalCase.put(lowercaseKey, originalKey);
    }
  }

  static public ConfigurationDictionary createDeepCopy(Dictionary<String, ?> properties)
  {
    final Hashtable<String, Object> h = copyDictionary(properties);
    final ConfigurationDictionary res = new ConfigurationDictionary(h);
    return res;
  }

  /**
   * Make a deep clone of the given dictionary.
   *
   * @param properties
   *          dictionary to clone.
   * @return A clone of the given dictionary.
   */
  static public Hashtable<String, Object> copyDictionary(final Dictionary<String, ?> properties)
  {
    if (properties == null) {
      return null;
    }

    final Hashtable<String, Object> res = new Hashtable<String, Object>();
    final Enumeration<String> keys = properties.keys();
    while (keys.hasMoreElements()) {
      final String key = keys.nextElement();
      final Object val = copyValue(properties.get(key));
      res.put(key, val);
    }
    return res;
  }

  static private Object copyValue(Object in)
  {
    if (in == null) {
      return null;
    }
    if (in.getClass().isArray()) {
      return copyArray(in);
    } else if (in instanceof Collection) {
      return copyCollection((Collection<?>) in);
    } else {
      return in;
    }
  }

  static private Collection<Object> copyCollection(Collection<?> in)
  {
    if (in == null) {
      return null;
    }
    final Vector<Object> out = new Vector<Object>();
    final Iterator<?> i = in.iterator();
    while (i.hasNext()) {
      out.addElement(copyValue(i.next()));
    }
    return out;
  }

  static private Object copyArray(Object in)
  {
    if (in == null) {
      return null;
    }
    final int length = Array.getLength(in);
    final Object out = Array.newInstance(in.getClass().getComponentType(), length);
    for (int i = 0; i < length; ++i) {
      Array.set(out, i, copyValue(Array.get(in, i)));
    }
    return out;
  }

  static void validateDictionary(Dictionary<?, ?> dictionary)
      throws IllegalArgumentException
  {
    if (dictionary == null) {
      return;
    }

    final Enumeration<?> keys = dictionary.keys();
    while (keys.hasMoreElements()) {
      final Object key = keys.nextElement();
      if (key.getClass() != String.class) {
        throw new IllegalArgumentException(
                                           "The key "
                                               + key
                                               + " is not of type java.lang.String.");
      }
      final Object val = dictionary.get(key);
      try {
        validateValue(val);
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException("The value for key " + key
                                           + " is not of correct type: "
                                           + e.getMessage());
      }

      final String s = (String) key;
      final String lower = s.toLowerCase();
      if (!s.equals(lower)) {
        final Object val2 = dictionary.get(lower);
        if (null != val2 && val != val2) {
          throw new IllegalArgumentException("key '" + s + "'"
                                             + " also appears with different "
                                             + "case '" + lower + "'");
        }
      }
    }
  }

  static private void validateValue(Object value)
      throws IllegalArgumentException
  {
    if (value == null) {
      return;
    }

    final Class<? extends Object> valueClass = value.getClass();
    if (valueClass.isArray()) {
      validateArray(value);
    } else if (value instanceof Collection) {
      validateCollection((Collection<?>) value);
    } else {
      if (!allowedObjectTypes.containsKey(valueClass)) {
        throw new IllegalArgumentException(valueClass.toString()
                                           + " is not an allowed type.");
      }
    }
  }

  static private void validateArray(Object array)
  {
    final Class<?> componentType = array.getClass().getComponentType();
    final int length = Array.getLength(array);
    if (componentType.isArray()
        || Collection.class.isAssignableFrom(componentType)) {
      for (int i = 0; i < length; ++i) {
        final Object o = Array.get(array, i);
        if (o != null) {
          final Class<? extends Object> objectClass = o.getClass();
          if (objectClass != componentType) {
            throw new IllegalArgumentException(
                                               "Objects with different type in array. "
                                                   + "Found "
                                                   + objectClass.toString()
                                                   + " " + "Expected "
                                                   + componentType.toString());
          }
          validateValue(o);
        }
      }
    } else {
      if (!allowedPrimitiveTypes.containsKey(componentType)
          && !allowedObjectTypes.containsKey(componentType)) {
        throw new IllegalArgumentException(
                                           "Illegal component type for arrays: "
                                               + componentType.toString());
      }
      for (int i = 0; i < length; ++i) {
        final Object o = Array.get(array, i);
        if (o != null) {
          Class<? extends Object> objectClass = o.getClass();
          if (componentType.isPrimitive()) {
            objectClass = classToPrimitiveType.get(objectClass);
          }
          if (objectClass != componentType) {
            throw new IllegalArgumentException(
                                               "Objects with different type in array. "
                                                   + "Found "
                                                   + objectClass.toString()
                                                   + " " + "Expected "
                                                   + componentType.toString());
          }
        }
      }
    }
  }

  static private void validateCollection(Collection<?> collection)
  {
    final Iterator<?> i = collection.iterator();
    while (i.hasNext()) {
      validateValue(i.next());
    }
  }

  /**
   * Removes properties from this dictionary that should not be included in a
   * Configuration-object handed out to Managed Services.
   */
  public void removeLocation()
  {
    remove(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
    remove(ConfigurationAdminFactory.DYNAMIC_BUNDLE_LOCATION);
    remove(KEY_CHANGE_COUNT);
  }

  /**
   * Get the location that this configuration dictionary is bound to.
   * @return current location.
   */
  String getLocation() {
    return (String) get(ConfigurationAdmin.SERVICE_BUNDLELOCATION);
  }

  /**
   * Get the PID that this configuration dictionary is bound to.
   * @return current PID.
   */
  String getPid() {
    return (String) get(Constants.SERVICE_PID);
  }

  /**
   * Get the factory PID that this configuration dictionary is bound to.
   * @return current factory PID or {@code null} if not a factory configuration.
   */
  String getFactoryPid() {
    return (String) get(ConfigurationAdmin.SERVICE_FACTORYPID);
  }

  private final Object changeCountLock = new Object();
  public long getChangeCount()
  {
    synchronized (changeCountLock) {
      final Long cc = (Long) originalCase.get(KEY_CHANGE_COUNT);
      return cc == null ? 0L : cc.longValue();
    }
  }

  void setChangeCount(long changeCount)
  {
    synchronized (changeCountLock) {
      put(KEY_CHANGE_COUNT, new Long(changeCount));
    }
  }

  void incrementChangeCount()
  {
    synchronized (changeCountLock) {
      setChangeCount(getChangeCount()+1);
    }
  }

}
