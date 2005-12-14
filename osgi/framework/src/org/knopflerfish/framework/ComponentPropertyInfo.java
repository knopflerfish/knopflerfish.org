/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

import java.util.ArrayList;

/**
 * @author Administrator (refactoring by Björn Andersson)
 *
 * This class is a helper classes that supply additional information to the
 * component class
 */
public class ComponentPropertyInfo {
  /* Property name */
  private String name;

  /* Property value */
  private ArrayList values = new ArrayList();

  private transient Object realValue = null;

  /* Property type */
  private String type;

  /**
   * @return Returns the name.
   */
  public String getName() {
    return name;
  }

  /**
   * @param name The name to set.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return Returns the type.
   */
  public String getType() {
    return type;
  }

  /**
   * @param type The type to set.
   */
  public void setType(String type) {
    this.type = type;
  }

  public ArrayList getValueList() {
    return values;
  }

  public Object getValue() {
    if (realValue == null) {
      realValue = makeValue();
    }
    return realValue;
  }

  private Object makeValue() {
    if (values.size() == 0) {
      return null;
    } else if (values.size() == 1) {
      return parseValue((String) values.get(0));
    } else if (type == null || "String".equals(type)) {
      return (String[]) values.toArray(new String[values.size()]);
    } else if ("Long".equals(type)) {
      long[] array = new long[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = Long.parseLong((String) values.get(i));
      }
      return array;
    } else if ("Double".equals(type)) {
      double[] array = new double[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = Double.parseDouble((String) values.get(i));
      }
      return array;
    } else if ("Float".equals(type)) {
      float[] array = new float[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = Float.parseFloat((String) values.get(i));
      }
      return array;
    } else if ("Integer".equals(type)) {
      int[] array = new int[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = Integer.parseInt((String) values.get(i));
      }
      return array;
    } else if ("Byte".equals(type)) {
      byte[] array = new byte[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = Byte.parseByte((String) values.get(i));
      }
      return array;
    } else if ("Char".equals(type)) {
      char[] array = new char[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = ((String) values.get(i)).charAt(0);
      }
      return array;
    } else if ("Boolean".equals(type)) {
      boolean[] array = new boolean[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = Boolean.valueOf((String) values.get(i)).booleanValue();
      }
      return array;
    } else if ("Short".equals(type)) {
      short[] array = new short[values.size()];
      for (int i=0; i<array.length; i++) {
        array[i] = Short.parseShort((String) values.get(i));
      }
      return array;
    } else {
      return null;
    }
  }

  private Object parseValue(String value) {
    if (value == null) {
      return null;
    } else if (type == null || "String".equals(type)) {
      return value;
    } else if ("Long".equals(type)) {
      return Long.valueOf(value);
    } else if ("Double".equals(type)) {
      return Double.valueOf(value);
    } else if ("Float".equals(type)) {
      return Float.valueOf(value);
    } else if ("Integer".equals(type)) {
      return Integer.valueOf(value);
    } else if ("Byte".equals(type)) {
      return Byte.valueOf(value);
    } else if ("Char".equals(type)) {
      return new Character(value.charAt(0));
    } else if ("Boolean".equals(type)) {
      return Boolean.valueOf(value);
    } else if ("Short".equals(type)) {
      return Short.valueOf(value);
    } else {
      return value;
    }
  }

  public void addValue(String value) {
    value = value.trim();
    if (value != null && value.length() > 0) {
      values.add(value);
    }
  }

}
