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

// TODO
// - Add ldapfilter to factory configuration
// - OutputStreamWriter and BufferendWriter

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class CMDataWriter {
    public final static String ENCODING = "ISO-8859-1";

    // Element names
    public final static String CONFIGURATION = "configuration";

    public final static String FACTORY_CONFIGURATION = "factoryconfiguration";

    public final static String PROPERTY = "property";

    public final static String VECTOR = "vector";

    public final static String ARRAY = "array";

    public final static String VALUE = "value";

    public final static String PRIMITIVE_VALUE = "primitiveValue";

    // Attribute names
    public final static String NAME = "name";

    public final static String TYPE = "type";

    public final static String LENGTH = "length";

    public final static String ELEMENT_TYPE = "elementType";

    private static Class classBigDecimal = null;
    static {
        try {
            classBigDecimal = Class.forName("java.math.BigDecimal");
        } catch (Throwable ignore) {
            classBigDecimal = null;
        }
    }

    final static String[] PRE = {
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>",
            "<!DOCTYPE cm_data PUBLIC '-//Gatespace//DTD cm_data 0.1//EN' 'cm_data.dtd'>",
            "<cm_data version=\"0.1\">",
            "<!-- EDITING THIS FILE IS NOT GUARANTEED TO WORK  -->" };

    final static String[] POST = { "</cm_data>" };

    public static void writeConfiguration(String pid, Dictionary d,
            PrintWriter w) {
        writeLines(PRE, w);
        w.println("<configuration pid=\"" + pid + "\" mode=\"new\">");
        writeProperties(d, w);
        w.println("</configuration>");
        writeLines(POST, w);
    }

    public static void writeFactoryConfiguration(String fpid, String pid,
            Dictionary d, PrintWriter w) {
        writeLines(PRE, w);
        w.println("<factoryconfiguration factorypid=\"" + fpid
                + "\" mode=\"update\">");
        writeProperties(d, w);
        w.println("</factoryconfiguration>");
        writeLines(POST, w);
    }

    static void writeLines(String[] lines, PrintWriter w) {
        for (int i = 0; i < lines.length; ++i) {
            w.println(lines[i]);
        }
    }

    static void writeProperties(Dictionary dictionary, PrintWriter w) {
        if (dictionary == null) {
            return;
        }

        Enumeration keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            w.println("<property name=\"" + escapeIfNeccesary(key) + "\">");
            writeValue(dictionary.get(key), w);
            w.println("</property>");
        }
    }

    static private void writeValue(Object value, PrintWriter w) {
        if (value == null) {
            return;
        }

        Class valueClass = value.getClass();
        if (valueClass.isArray()) {
            writeArray(value, w);
        } else if (valueClass == Vector.class) {
            writeVector((Vector) value, w);
        } else {
            w.println("<value type=\"" + typeOf(value) + "\">"
                    + escapeIfNeccesary(value.toString()) + "</value>");
        }
    }

    static private String escapeIfNeccesary(String s) {
        if (needsEscaping(s)) {
            StringBuffer escaped = new StringBuffer();
            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);
                if (c == '&') {
                    escaped.append("&amp;");
                } else if (c == '<') {
                    escaped.append("&lt;");
                } else if (c == '>') {
                    escaped.append("&gt;");
                } else if (c == '\'') {
                    escaped.append("&apos;");
                } else if (c == '\"') {
                    escaped.append("&quot;");
                } else {
                    escaped.append(c);
                }
            }
            return escaped.toString();
        }
        return s;
    }

    static private boolean needsEscaping(String s) {
        return s.indexOf('&') != -1 || s.indexOf('<') != -1
                || s.indexOf('>') != -1 || s.indexOf('\'') != -1
                || s.indexOf('\"') != -1;
    }

    static private void writeArray(Object array, PrintWriter w) {
        Class componentType = array.getClass().getComponentType();
        int length = Array.getLength(array);

        w.println("<array length=\"" + length + "\" elementType=\""
                + elementTypeOf(array) + "\">");

        if (componentType.isPrimitive()) {
            for (int i = 0; i < length; ++i) {
                Object o = Array.get(array, i);
                writePrimitiveValue(componentType, o, w);
            }
        } else {
            for (int i = 0; i < length; ++i) {
                Object o = Array.get(array, i);
                if (o == null) {
                    w.println("<null/>");
                } else {
                    writeValue(o, w);
                }
            }
        }

        w.println("</array>");
    }

    static private void writeVector(Vector vector, PrintWriter w) {
        int length = vector.size();
        w.println("<vector length=\"" + length + "\">");
        for (int i = 0; i < vector.size(); ++i) {
            Object element = vector.elementAt(i);
            writeValue(element, w);
        }
        w.println("</vector>");
    }

    static private void writePrimitiveValue(Class type, Object value,
            PrintWriter w) {
        w.println("<primitiveValue type=\"" + primitiveTypeOf(type) + "\">");
        w.println(escapeIfNeccesary(value.toString()));
        w.println("</primitiveValue>");
    }

    static private String elementTypeOf(Object array) {
        String elementType = "";
        Class c = array.getClass().getComponentType();
        while (c.isArray()) {
            elementType = elementType + "[]";
            c = c.getComponentType();
        }
        if (c.isPrimitive()) {
            elementType = primitiveTypeOf(c) + elementType;
        } else {
            elementType = typeOf(c) + elementType;
        }
        return elementType;
    }

    static private String typeOf(Class c) {
        return (String) classToString.get(c);
    }

    static private String typeOf(Object value) {
        Class c = value.getClass();
        return (String) classToString.get(c);
    }

    private final static Hashtable classToString = new Hashtable();
    static {
        classToString.put(Vector.class, "Vector");
        classToString.put(Integer.class, "Integer");
        classToString.put(Short.class, "Short");
        classToString.put(Long.class, "Long");
        classToString.put(String.class, "String");
        classToString.put(Float.class, "Float");
        classToString.put(Double.class, "Double");
        classToString.put(Byte.class, "Byte");
        classToString.put(BigInteger.class, "BigInteger");
        classToString.put(Character.class, "Character");
        classToString.put(Boolean.class, "Boolean");
        if (classBigDecimal != null) {
            classToString.put(classBigDecimal, "BigDecimal");
        }
    }

    static private String primitiveTypeOf(Class type) {
        return (String) primitiveTypeToString.get(type);
    }

    private final static Hashtable primitiveTypeToString = new Hashtable();
    static {
        primitiveTypeToString.put(Integer.TYPE, "int");
        primitiveTypeToString.put(Short.TYPE, "short");
        primitiveTypeToString.put(Long.TYPE, "long");
        primitiveTypeToString.put(Float.TYPE, "float");
        primitiveTypeToString.put(Double.TYPE, "double");
        primitiveTypeToString.put(Byte.TYPE, "byte");
        primitiveTypeToString.put(Character.TYPE, "char");
        primitiveTypeToString.put(Boolean.TYPE, "boolean");
    }
}
