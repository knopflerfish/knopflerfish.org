/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.bundle.command;

import java.util.*;
import java.lang.reflect.*;
import org.osgi.service.command.*;

public class JavaLangConverter implements Converter {
  
  static Class[] CLASSES = new Class[] {
    Double.class,
    Double.TYPE,

    Byte.class,
    Byte.TYPE,

    Integer.class,
    Integer.TYPE,

    Boolean.class,
    Boolean.TYPE,

    Character.class,
    Character.TYPE,

    Long.class,
    Long.TYPE,

    String.class,

    Number.class,

  };
  
  static String[] CLASSES_STRINGS;
  static {
    CLASSES_STRINGS = new String[CLASSES.length];
    for(int i = 0; i < CLASSES.length; i++) {
      CLASSES_STRINGS[i] = CLASSES[i].getName();
    }
  }
  
  public static void main(String argv[]) {
    try {
      String cname = argv[0];
      Class clazz = null;
      for(int i = 0; i < CLASSES.length; i++) {
        if(CLASSES[i].getName().equals(cname)) {
          clazz = CLASSES[i];
        }
      }
      if(clazz == null) {
        throw new IllegalArgumentException("Unsupported class " + cname);
      }
      JavaLangConverter c = new JavaLangConverter();
      
      Object r = c.convert(clazz, argv[1]);
      System.out.println("r=" + r);
      if(r != null) {
        System.out.println(r.getClass().getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Object convert(Class desiredType, Object in) {
    if(in == null) {
      return null;
    }
    if(desiredType.isAssignableFrom(in.getClass())) {
      return in;
    }
    if(desiredType == String.class) {
      return in.toString();
    }
    if(desiredType == Character.class || desiredType == Character.TYPE) {
      String s = in.toString();
      if(s.length() != 1) {
        throw new IllegalArgumentException("input must be a single char");
      }
      return new Character(s.charAt(0));
    }
    if(desiredType == Integer.class || desiredType == Integer.TYPE) {
      if(in instanceof Number) {
        return new Integer(((Number)in).intValue());
      } else {
        return new Integer(Integer.parseInt(in.toString().trim()));
      }
    }
    if(desiredType == Boolean.class || desiredType == Boolean.TYPE) {
      if(in instanceof Number) {        
        return (0 != ((Number)in).intValue()) ? Boolean.TRUE : Boolean.FALSE;
      } else {
        return "true".equals(in.toString()) ? Boolean.TRUE : Boolean.FALSE;
      }
    }
    if(desiredType == Long.class || desiredType == Long.TYPE) {
      if(in instanceof Number) {
        return new Long(((Number)in).longValue());
      } else {
        return Long.valueOf(in.toString().trim());
      }
    }
    if(desiredType == Double.class || desiredType == Double.TYPE) {
      if(in instanceof Number) {
        return new Double(((Number)in).doubleValue());
      } else {
        return Double.valueOf(in.toString().trim());
      }
    }
    if(desiredType == Byte.class || desiredType == Byte.TYPE) {
      if(in instanceof Number) {
        return new Byte(((Number)in).byteValue());
      } else {
        return Byte.valueOf(in.toString().trim());
      }
    }

    try {
      Constructor cons = desiredType.getConstructor(new Class[] { in.getClass() });
      return cons.newInstance(new Object[] { in });
    } catch (NoSuchMethodException e) {
      try {
        Constructor cons = desiredType.getConstructor(new Class[] { String.class });
        return cons.newInstance(new Object[] { in.toString() });
      } catch (Exception e2) {
        // return null;
        // throw new IllegalArgumentException("Cannot convert '" + in + "' to " + desiredType.getName() + ", " + e2);
      }
    } catch (Exception e) {
      // throw new IllegalArgumentException("Cannot convert '" + in + "' to " + desiredType.getName() + ", " + e);      
    }
    return null;
    // throw new IllegalArgumentException("Cannot convert '" + in + "' to " + desiredType.getName());
  }

  public CharSequence format(Object target, int level, Converter escape) {
    throw new RuntimeException("NYI");
  }
}
