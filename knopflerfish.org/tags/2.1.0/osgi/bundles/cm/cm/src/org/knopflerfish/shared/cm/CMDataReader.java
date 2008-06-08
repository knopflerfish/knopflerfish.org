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

package org.knopflerfish.shared.cm;

import java.io.PushbackReader;
import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.osgi.framework.Constants;

public class CMDataReader extends XmlReader {
    public final static String ENCODING = "ISO-8859-1";

    public final static String FACTORY_PID = "service.factoryPid";

    public final static String SERVICE_PID = "service.pid";

    private final static String PID = "pid";

    private final static String CONFIGURATION = "configuration";

    private final static String FACTORY_CONFIGURATION = "factoryconfiguration";

    private final static String PROPERTY = "property";

    private final static String VECTOR = "vector";

    private final static String ARRAY = "array";

    private final static String VALUE = "value";

    private final static String PRIMITIVE_VALUE = "primitiveValue";

    private final static String NULL = "null";

    private final static Object NULL_ELEMENT = new Object();

    private final static String NAME = "name";

    private final static String TYPE = "type";

    private final static String LENGTH = "length";

    private final static String ELEMENT_TYPE = "elementType";

    private static Class classBigDecimal = null;
    static {
        try {
            classBigDecimal = Class.forName("java.math.BigDecimal");
        } catch (Throwable ignore) {
            classBigDecimal = null;
        }
    }

    private static Class classBigInteger = null;
    static {
        try {
            classBigInteger = Class.forName("java.math.BigInteger");
        } catch (Throwable ignore) {
            classBigInteger = null;
        }
    }

    private final static Hashtable stringToClass = new Hashtable();
    static {
        stringToClass.put("Vector", Vector.class);
        stringToClass.put("Integer", Integer.class);
        stringToClass.put("Short", Short.class);
        stringToClass.put("Long", Long.class);
        stringToClass.put("String", String.class);
        stringToClass.put("Float", Float.class);
        stringToClass.put("Double", Double.class);
        stringToClass.put("Byte", Byte.class);
        if (classBigInteger != null) {
            stringToClass.put("BigInteger", classBigInteger);
        }
        stringToClass.put("Character", Character.class);
        stringToClass.put("Boolean", Boolean.class);
        if (classBigDecimal != null) {
            stringToClass.put("BigDecimal", classBigDecimal);
        }
    }

    private final static Hashtable stringToPrimitiveType = new Hashtable();
    static {
        stringToPrimitiveType.put("int", Integer.TYPE);
        stringToPrimitiveType.put("short", Short.TYPE);
        stringToPrimitiveType.put("long", Long.TYPE);
        stringToPrimitiveType.put("float", Float.TYPE);
        stringToPrimitiveType.put("double", Double.TYPE);
        stringToPrimitiveType.put("byte", Byte.TYPE);
        stringToPrimitiveType.put("char", Character.TYPE);
        stringToPrimitiveType.put("boolean", Boolean.TYPE);
    }

    private final static Hashtable primitiveTypeToWrapperType = new Hashtable();
    static {
        primitiveTypeToWrapperType.put("int", Integer.class);
        primitiveTypeToWrapperType.put("short", Short.class);
        primitiveTypeToWrapperType.put("long", Long.class);
        primitiveTypeToWrapperType.put("float", Float.class);
        primitiveTypeToWrapperType.put("double", Double.class);
        primitiveTypeToWrapperType.put("byte", Byte.class);
        primitiveTypeToWrapperType.put("char", Character.class);
        primitiveTypeToWrapperType.put("boolean", Boolean.class);
    }

    private final Stack objects = new Stack();

    public CMDataReader() {
    }

    public Hashtable readCMData(PushbackReader r) throws Exception {
        if (objects.size() > 0) {
            objects.removeAllElements();
        }
        read(r);
        if (objects.size() != 1) {
            throwMessage("Failed creating Hashtable from cm_data stream.");
        }
        return (Hashtable) objects.peek();
    }

    public Hashtable[] readCMDatas(PushbackReader r) throws Exception {
        if (objects.size() > 0) {
            objects.removeAllElements();
        }
        read(r);
        Hashtable[] configs = new Hashtable[objects.size()];
        for (int i=0; i<objects.size(); i++) {
            configs[i] = (Hashtable) objects.elementAt(i);
        }
        return configs;
    }

    protected void startElement(String elementType, Dictionary attributes)
            throws Exception {
        if (VALUE.equals(elementType)) {
            String type = (String) attributes.get(TYPE);
            throwIfMissingAttribute(elementType, TYPE, type);
        } else if (PROPERTY.equals(elementType)) {
            String name = (String) attributes.get(NAME);
            throwIfMissingAttribute(elementType, NAME, name);
            objects.push(name);
        } else if (PRIMITIVE_VALUE.equals(elementType)) {
            String type = (String) attributes.get(TYPE);
            throwIfMissingAttribute(elementType, TYPE, type);
        } else if (ARRAY.equals(elementType)) {
            String length = (String) attributes.get(LENGTH);
            throwIfMissingAttribute(elementType, LENGTH, length);
            String type = (String) attributes.get(ELEMENT_TYPE);
            throwIfMissingAttribute(elementType, ELEMENT_TYPE, type);
        } else if (VECTOR.equals(elementType)) {
            String length = (String) attributes.get(LENGTH);
            throwIfMissingAttribute(elementType, LENGTH, length);
        } else if (FACTORY_CONFIGURATION.equals(elementType)
                || CONFIGURATION.equals(elementType)) {
            objects.push(new Hashtable());
        } else {
        }
    }

    protected void endElement(String elementType, Dictionary attributes,
            String content) throws Exception {
        if (VALUE.equals(elementType)) {
            String type = (String) attributes.get(TYPE);
            Object o = createValue(type, content);
            objects.push(o);
        } else if (PROPERTY.equals(elementType)) {
            Object value = objects.pop();
            Object key = objects.pop();
            ((Dictionary) objects.peek()).put(key, value);
        } else if (PRIMITIVE_VALUE.equals(elementType)) {
            String type = (String) attributes.get(TYPE);
            Object o = createWrappedPrimitiveValue(type, content);
            objects.push(o);
        } else if (NULL.equals(elementType)) {
            objects.push(NULL_ELEMENT);
        } else if (ARRAY.equals(elementType)) {
            String componentType = (String) attributes.get(ELEMENT_TYPE);
            int length = Integer.parseInt((String) attributes.get(LENGTH));
            Object array = createArray(length, componentType);
            for (int i = length - 1; -1 < i; --i) {
                Object o = objects.pop();
                if (o == NULL_ELEMENT) {
                    Array.set(array, i, null);
                } else {
                    Array.set(array, i, o);
                }
            }
            objects.push(array);
        } else if (VECTOR.equals(elementType)) {
            Vector v = new Vector();
            int length = Integer.parseInt((String) attributes.get(LENGTH));
            for (int i = 0; i < length; ++i) {
                v.insertElementAt(objects.pop(), 0);
            }
            objects.push(v);
        } else if (FACTORY_CONFIGURATION.equals(elementType)) {
            Object o = attributes.get(FACTORY_PID);
            if (o != null) {
                ((Hashtable) objects.peek()).put(FACTORY_PID, o);
            }
        } else if (CONFIGURATION.equals(elementType)) {
            Object o = attributes.get(SERVICE_PID);
            if (o == null) {
                o = attributes.get(PID);
            }
            if (o != null) {
                ((Hashtable) objects.peek()).put(Constants.SERVICE_PID, o);
            }
        } else {
        }
    }

    static Object createValue(String type, String arg) throws Exception {
        Class c = toJavaType(type);
        return createValue(c, arg);
    }

    static Object createValue(Class c, String arg) throws Exception {
        if (c == String.class) {
            return arg;
        } else if (c == Character.class) {
            return new Character(arg.charAt(0));
        } else if (c != null) {
            return c.getConstructor(new Class[] { String.class }).newInstance(
                    new Object[] { arg });
        } else {
            return null;
        }
    }

    static Object createWrappedPrimitiveValue(String type, String arg)
            throws Exception {
        Class c = toWrapperType(type);
        return createValue(c, arg);
    }

    Object createArray(int length, String elementType) throws Exception {
        if (elementType == null || elementType.length() == 0) {
            throwMessage("null or empty elementType attribute");
        }

        int numberOfArrayDimensions = 1;
        while (elementType.endsWith("[]")) {
            ++numberOfArrayDimensions;
            elementType = elementType.substring(0, elementType.length() - 2);
        }
        int[] dimensions = new int[numberOfArrayDimensions];
        dimensions[0] = length;
        for (int i = 1; i < numberOfArrayDimensions; ++i) {
            dimensions[i] = 0;
        }

        Class componentType = toJavaType(elementType);
        if (componentType == null) {
            return null;
        }
        Object ret = null;
        // (sparud) This clumsy way of creating the array is used to get
        // around a bug in CVM, which has problems with zero-sized arrays.
        for (int i = numberOfArrayDimensions - 1; i >= 0; i--) {
            ret = Array.newInstance(componentType, dimensions[i]);
            componentType = ret.getClass();
        }
        return ret;
    }

    private static Class toJavaType(String type) throws Exception {
        Class c = (Class) stringToClass.get(type);
        if (c == null) {
            c = (Class) stringToPrimitiveType.get(type);
        }
        return c;
    }

    private static Class toWrapperType(String type) throws Exception {
        return (Class) primitiveTypeToWrapperType.get(type);
    }
}
