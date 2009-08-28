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

package org.knopflerfish.bundle.cm;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * * This class implements the datatype restrictions, and case insensitive *
 * lookup, required of configuration dictionaries in the CM. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.2 $
 */

final class ConfigurationDictionary extends Dictionary {

    /**
     * * Use BigDecimal if available.
     */

    private static Class classBigDecimal = null;
    static {
        try {
            classBigDecimal = Class.forName("java.math.BigDecimal");
        } catch (Throwable ignore) {
            classBigDecimal = null;
        }
    }

    /**
     * * Allowed object types.
     */

    private final static Hashtable allowedObjectTypes = new Hashtable();
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

    private final static Hashtable allowedPrimitiveTypes = new Hashtable();
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

    private final static Hashtable classToPrimitiveType = new Hashtable();
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
     * * Mapping of keys in lowercase to the case they had when * added to the
     * dictionary.
     */

    final Hashtable lowercaseToOriginalCase;

    /**
     * * A dictionary holding the key-value pairs of the configuration * with
     * the keys in the original case they were entered in.
     */

    final Hashtable originalCase;

    public ConfigurationDictionary() {
        this(new Hashtable());
        put(ConfigurationAdminFactory.DUMMY_PROPERTY,
                ConfigurationAdminFactory.DUMMY_PROPERTY);
    }

    /**
     * * Construct a ConfigurationDictionary given an ordinary Dictionary. * *
     * 
     * @param dictionary
     *            The original dictionary.
     */

    public ConfigurationDictionary(Hashtable dictionary) {
        this.lowercaseToOriginalCase = new Hashtable();
        this.originalCase = dictionary;
        updateLowercaseToOriginalCase();
    }

    private ConfigurationDictionary(ConfigurationDictionary original) {
        this.originalCase = copyDictionary(original.originalCase);
        this.lowercaseToOriginalCase = (Hashtable) original.lowercaseToOriginalCase
                .clone();
    }

    public Enumeration elements() {
        return originalCase.elements();
    }

    public Object get(Object key) {

        Object val = originalCase.get(key);

        if (val != null) {
            return val;
        }

        String lowercaseKey = ((String) key).toLowerCase();
        String originalCaseKey = (String) lowercaseToOriginalCase
                .get(lowercaseKey);
        if (originalCaseKey != null) {
            key = originalCaseKey;
        }

        val = originalCase.get(key);

        return val;
    }

    public boolean isEmpty() {
        return originalCase.isEmpty();
    }

    public Enumeration keys() {
        return originalCase.keys();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ConfigurationDictionary{");
        for (Enumeration e = keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object val = get(key);

            sb.append(key + "=" + val);
            if (e.hasMoreElements()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public Object put(Object key, Object value) {
        String lowercaseKey = ((String) key).toLowerCase();
        String originalCaseKey = (String) lowercaseToOriginalCase
                .get(lowercaseKey);
        if (originalCaseKey != null) {
            key = originalCaseKey;
        }
        Object o = originalCase.put(key, value);
        if (originalCaseKey == null) {
            updateLowercaseToOriginalCase((String) key);
        }
        return o;
    }

    public Object remove(Object key) {
        String lowercaseKey = ((String) key).toLowerCase();
        if (!lowercaseToOriginalCase.containsKey(lowercaseKey)) {
            return null;
        }
        String originalCaseKey = (String) lowercaseToOriginalCase
                .remove(lowercaseKey);
        return originalCase.remove(originalCaseKey);
    }

    public int size() {
        return originalCase.size();
    }

    Hashtable getOriginal() {
        return originalCase;
    }

    ConfigurationDictionary createCopy() {
        return new ConfigurationDictionary(this);
    }

    ConfigurationDictionary createCopyIfRealAndRemoveLocation() {
        if (doesNotContainRealProperties()) {
            return null;
        }
        return createCopyAndRemoveLocation();
    }

    ConfigurationDictionary createCopyAndRemoveLocation() {
        ConfigurationDictionary cd = createCopy();
        cd.remove(ConfigurationAdminFactory.BUNDLE_LOCATION);
        cd.remove(ConfigurationAdminFactory.DYNAMIC_BUNDLE_LOCATION);
        return cd;
    }

    boolean doesNotContainRealProperties() {
        int numberOfProperties = size();
        if (numberOfProperties > 5) {
            return false;
        }
        if (get(ConfigurationAdminFactory.SERVICE_PID) != null)
            --numberOfProperties;
        if (get(ConfigurationAdminFactory.FACTORY_PID) != null)
            --numberOfProperties;
        if (get(ConfigurationAdminFactory.BUNDLE_LOCATION) != null)
            --numberOfProperties;
        if (get(ConfigurationAdminFactory.DYNAMIC_BUNDLE_LOCATION) != null)
            --numberOfProperties;
        if (get(ConfigurationAdminFactory.DUMMY_PROPERTY) != null)
            --numberOfProperties;
        return numberOfProperties == 0;
    }

    private void updateLowercaseToOriginalCase() {
        Enumeration keys = originalCase.keys();
        while (keys.hasMoreElements()) {
            String originalKey = (String) keys.nextElement();
            updateLowercaseToOriginalCase(originalKey);
        }
    }

    private void updateLowercaseToOriginalCase(String originalKey) {
        if (originalKey == null) {
            return;
        }
        String lowercaseKey = originalKey.toLowerCase();
        if (!lowercaseToOriginalCase.containsKey(lowercaseKey)) {
            lowercaseToOriginalCase.put(lowercaseKey, originalKey);
        }
    }

    static public ConfigurationDictionary createDeepCopy(Dictionary in) {
        Hashtable h = copyDictionary(in);
        return new ConfigurationDictionary(h);
    }

    static public Hashtable copyDictionary(Dictionary in) {
        if (in == null) {
            return null;
        }

        Hashtable out = new Hashtable();
        Enumeration keys = in.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object origVal = in.get(key);
            Object val = copyValue(origVal);

            // The R3 tests prefers keys with different case to be
            // silently unified. We really prefer IllegalArgumentException
            String s = (String) key;
            String lower = s.toLowerCase();
            if (!s.equals(lower)) {
                Object lowerVal = in.get(lower);
                if (null != lowerVal) {
                    if (Activator.r3TestCompliant()) {
                        key = lower;
                    } else {
                        // Accept different case when actual value id
                        // reference-equal
                        // This solves problem when incoming dictionary has
                        // case-insensitive get/lookup
                        // If not reference-equal, throw
                        // IllegalArgumentException
                        if (lowerVal != origVal) {
                            throw new IllegalArgumentException(
                                    "same key exists with different case: "
                                            + key + "/" + lower);
                        }
                    }
                }
            }
            out.put(key, val);
        }
        return out;
    }

    static private Object copyValue(Object in) {
        if (in == null) {
            return null;
        }
        if (in.getClass().isArray()) {
            return copyArray(in);
        } else if (in instanceof Vector) {
            return copyVector((Vector) in);
        } else {
            return in;
        }
    }

    static private Vector copyVector(Vector in) {
        if (in == null) {
            return null;
        }
        Vector out = new Vector();
        Enumeration elements = in.elements();
        while (elements.hasMoreElements()) {
            out.addElement(copyValue(elements.nextElement()));
        }
        return out;
    }

    static private Object copyArray(Object in) {
        if (in == null) {
            return null;
        }
        int length = Array.getLength(in);
        Object out = Array
                .newInstance(in.getClass().getComponentType(), length);
        for (int i = 0; i < length; ++i) {
            Array.set(out, i, copyValue(Array.get(in, i)));
        }
        return out;
    }

    static void validateDictionary(Dictionary dictionary)
            throws IllegalArgumentException {
        if (dictionary == null) {
            return;
        }

        Enumeration keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key.getClass() != String.class) {
                throw new IllegalArgumentException("The key " + key
                        + " is not of type java.lang.String.");
            }
            try {
                validateValue(dictionary.get(key));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("The value for key " + key
                        + " is not of correct type: " + e.getMessage());
            }

            /*
             * if(Activator.r3TestCompliant()) { String s = (String)key; String
             * lower = s.toLowerCase(); if(!s.equals(lower)) { if(null !=
             * dictionary.get(lower)) { throw new IllegalArgumentException("key '" +
             * s + "'" + " also appears with different " + "case '" + lower +
             * "'"); } } }
             */

        }

    }

    static private void validateValue(Object value)
            throws IllegalArgumentException {
        if (value == null) {
            return;
        }

        Class valueClass = value.getClass();
        if (valueClass.isArray()) {
            validateArray(value);
        } else if (valueClass == Vector.class) {
            validateVector((Vector) value);
        } else {
            if (!allowedObjectTypes.containsKey(valueClass)) {
                throw new IllegalArgumentException(valueClass.toString()
                        + " is not an allowed type.");
            }
        }
    }

    static private void validateArray(Object array) {
        Class componentType = array.getClass().getComponentType();
        int length = Array.getLength(array);
        if (componentType.isArray() || componentType == Vector.class) {
            for (int i = 0; i < length; ++i) {
                Object o = Array.get(array, i);
                if (o != null) {
                    Class objectClass = o.getClass();
                    if (objectClass != componentType) {
                        throw new IllegalArgumentException(
                                "Objects with different type in array. "
                                        + "Found " + objectClass.toString()
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
                Object o = Array.get(array, i);
                if (o != null) {
                    Class objectClass = o.getClass();
                    if (componentType.isPrimitive()) {
                        objectClass = (Class) classToPrimitiveType
                                .get(objectClass);
                    }
                    if (objectClass != componentType) {
                        throw new IllegalArgumentException(
                                "Objects with different type in array. "
                                        + "Found " + objectClass.toString()
                                        + " " + "Expected "
                                        + componentType.toString());
                    }
                }
            }
        }
    }

    static private void validateVector(Vector vector) {
        for (int i = 0; i < vector.size(); ++i) {
            Object element = vector.elementAt(i);
            validateValue(element);
        }
    }
}
