/*
 * Copyright (c) 2016-2016, KNOPFLERFISH project
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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentException;


/**
 * This class needs to be a Dictionary and a Map.
 * TBD, check that this class is immutable
 */
class ComponentPropertyProxy implements InvocationHandler
{
  final private HashMap<String,Object> results = new HashMap<String, Object>();
  final private Dictionary<String, Object> properties;
  final private Bundle bundle;

  ComponentPropertyProxy(ComponentContextImpl cci) {
    this.properties = cci.getProperties();
    this.bundle = cci.getBundleContext().getBundle();
  }


  @Override
  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable {
    String mName = method.getName();
    if (args != null && args.length > 0) {
      throw new NoSuchMethodError(mName + " can't have arguments");
    }
    Object res = results.get(mName);
    if (res == null && !results.containsKey(mName)) {
      Class<?> retType = method.getReturnType();
      String pName = getPropertyName(mName);
      res = properties.get(pName);
      if (res != null) {
        if (!retType.isInstance(res)) {
          res = coerceResult(res, 0, retType);
        }
      } else {
        res = method.getDefaultValue();
        if (res == null) {
          res = coerceResult(null, 0, retType);
        }
      }
      results.put(mName, res);
    }
    return res;
  }


  @SuppressWarnings("unchecked")
  private Object coerceResult(Object res, int idx, Class<?> retType)
  {
    try {
      if (retType == String.class) {
        res = getPossibleElement(res, idx, null);
        if (res == null || res instanceof String) {
          return res;
        } else if (res instanceof Boolean || res instanceof Character
            || res instanceof Number) {
          return res.toString();
        } else {
          throw new ComponentException("String");
        }
      } else if (retType == Boolean.class || retType == boolean.class) {
        res = getPossibleElement(res, idx, false);
        if (res instanceof Boolean) {
          return res;
        } else if (res instanceof String) {
          return Boolean.parseBoolean((String) res);
        } else if (res instanceof Character) {
          return ((Character) res).charValue() != 0;
        } else if (res instanceof Number) {
          String s = res.toString();
          return !(s.equals("0") || s.equals("0.0") || s.equals("-0.0"));
        } else {
          throw new ComponentException("Boolean");
        }
      } else if (retType == Character.class || retType == char.class) {
        res = getPossibleElement(res, idx, "");
        if (res instanceof Character) {
          return res;
        } else if (res instanceof String) {
          return ((String) res).length() > 0 ? ((String) res).charAt(0) : (char)0;
        } else if (res instanceof Boolean) {
          return (char)(((Boolean) res).booleanValue() ? 1 : 0);
        } else if (res instanceof Number) {
          return (char)((Number) res).intValue();
        } else {
          throw new ComponentException("Character");
        }
      } else if (retType == Byte.class || retType == byte.class) {
        res = getPossibleElement(res, idx, 0);
        if (res instanceof Byte) {
          return res;
        } else if (res instanceof String) {
          return Byte.parseByte((String) res);
        } else if (res instanceof Boolean) {
          return (byte)((Boolean) res ? 1 : 0);
        } else if (res instanceof Character) {
          return (byte)((Character) res).charValue();
        } else if (res instanceof Number) {
          return ((Number) res).byteValue();
        } else {
          throw new ComponentException("Byte");
        }
      } else if (retType == Short.class || retType == short.class) {
        res = getPossibleElement(res, idx, 0);
        if (res instanceof Short) {
          return res;
        } else if (res instanceof String) {
          return Short.parseShort((String) res);
        } else if (res instanceof Boolean) {
          return (short)((Boolean) res ? 1 : 0);
        } else if (res instanceof Character) {
          return (short)((Character) res).charValue();
        } else if (res instanceof Number) {
          return ((Number) res).shortValue();
        } else {
          throw new ComponentException("Short");
        }
      } else if (retType == Integer.class || retType == int.class) {
        res = getPossibleElement(res, idx, 0);
        return coerceInteger(res);
      } else if (retType == Long.class || retType == long.class) {
        res = getPossibleElement(res, idx, 0);
        if (res instanceof Long) {
          return res;
        } else if (res instanceof String) {
          return Long.parseLong((String) res);
        } else if (res instanceof Boolean) {
          return (long)((Boolean) res ? 1 : 0);
        } else if (res instanceof Character) {
          return (long)((Character) res).charValue();
        } else if (res instanceof Number) {
          return ((Number) res).longValue();
        } else {
          throw new ComponentException("Long");
        }
      } else if (retType == Float.class || retType == float.class) {
        res = getPossibleElement(res, idx, 0);
        if (res instanceof Float) {
          return res;
        } else if (res instanceof String) {
          return Float.parseFloat((String) res);
        } else if (res instanceof Boolean) {
          return (float)((Boolean) res ? 1 : 0);
        } else if (res instanceof Character) {
          return (float)((Character) res).charValue();
        } else if (res instanceof Number) {
          return ((Number) res).floatValue();
        } else {
          throw new ComponentException("Float");
        }
      } else if (retType == Double.class || retType == double.class) {
        res = getPossibleElement(res, idx, 0);
        if (res instanceof Double) {
          return res;
        } else if (res instanceof String) {
          return Double.parseDouble((String) res);
        } else if (res instanceof Boolean) {
          return (double)((Boolean) res ? 1 : 0);
        } else if (res instanceof Character) {
          return (double)((Character) res).charValue();
        } else if (res instanceof Number) {
          return ((Number) res).doubleValue();
        } else {
          throw new ComponentException("Double");
        }
      } else if (retType == Class.class) {
        res = getPossibleElement(res, idx, null);
        if (res == null) {
          return res;
        } else if (res instanceof String) {
          return bundle.loadClass((String) res);
        } else {
          throw new ComponentException("Class");
        }
      } else if (retType.isEnum()) {
        res = getPossibleElement(res, idx, null);
        if (res == null) {
          return res;
        } else if (res instanceof String) {
          @SuppressWarnings({ "rawtypes" })
          Class cls = (Class)retType;
          return coerceEnum((String)res, cls);
        } else {
          throw new ComponentException("Enum");
        }
      } else if (retType.isAnnotation()) {
        throw new ComponentException("Unexpected annotation type " + retType);        
      } else if (retType.isArray()) {
        if (res == null) {
          return res;
        }
        Class<?> elemType = retType.getComponentType();
        Class<?> resCls = res.getClass();
        int len;
        if (resCls.isArray()) {
          len = Array.getLength(res);
        } else if (Collection.class.isAssignableFrom(resCls)) {
          len = ((Collection<?>)res).size();
        } else {
          len = 1;
        }
        Object arrayRes = Array.newInstance(elemType, len);
        for (int i = 0; i < len; i++) {
          Array.set(arrayRes, i, coerceResult(res, i, elemType));
        }
        return arrayRes;
      } else {
        throw new ComponentException("Unexpected type " + retType);
      }
    } catch (NumberFormatException nfe) {
      throw new ComponentException(nfe);
    } catch (ClassNotFoundException cnfe) {
      throw new ComponentException(cnfe);
    }
  }


  private Object getPossibleElement(Object res, int idx, Object emptyVal)
  {
    if (res == null) {
      return emptyVal;
    }
    if (res.getClass().isArray()) {
      return Array.getLength(res) > idx ? Array.get(res, idx) : emptyVal;
    } else if (Collection.class.isAssignableFrom(res.getClass())) {
      Iterator<?> ci = ((Collection<?>)res).iterator();
      for (int i = 0; i <= idx; i++) {
        if (ci.hasNext()) {
          res = ci.next();
        } else {
          return emptyVal;
        }
      }
    }
    return res;
  }


  private <T extends Enum<T>> T coerceEnum(String s, Class<T> clazz)
  {
    return Enum.valueOf(clazz, s);
  }
  

  private static final Pattern replacePattern = Pattern.compile("(\\$\\$)|(\\$)|(__)|(_)");

  private String getPropertyName(String name) {
    Matcher match = replacePattern.matcher(name);
    if (match.find()) {
      StringBuffer res = new StringBuffer();
      do {
        if (match.group(1) != null) {
          match.appendReplacement(res, "\\$");
        } else if (match.group(2) != null) {
          match.appendReplacement(res, "");
        } else if (match.group(3) != null) {
          match.appendReplacement(res, "_");
        } else {
          match.appendReplacement(res, ".");
        }
      } while (match.find());
      match.appendTail(res);
      return res.toString();
    } else {
      return name;
    }
  }


  static Integer coerceInteger(Object res)
  {
    if (res instanceof Integer) {
      return (Integer) res;
    } else if (res instanceof String) {
      return Integer.parseInt((String)res);
    } else if (res instanceof Boolean) {
      return (int)((Boolean) res ? 1 : 0);
    } else if (res instanceof Character) {
      return (int)((Character) res).charValue();
    } else if (res instanceof Number) {
      return ((Number) res).intValue();
    } else {
      throw new ComponentException("Integer");
    }
  }

}
