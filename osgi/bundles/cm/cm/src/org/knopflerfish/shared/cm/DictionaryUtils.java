/**
 ** Copyright (c) 2002 Gatespace AB. All Rights Reserved.
 **/

package org.knopflerfish.shared.cm;

import java.util.Enumeration;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import java.lang.reflect.Array;

/**
 ** Some utility methods for objects that implement Dictionary.
 **
 ** @author Gatespace AB
 ** @version $Revision: 1.1.1.1 $
 **/

public final class DictionaryUtils {

  /**
   ** Copy a dictionary.
   **
   ** Deep copy where values aren't immutable. I.e. Vector and arrays.
   **
   ** @param in The Dictionary to create a copy of.
   **
   ** @return A copy of the Dictionary.
   **/

  static public Dictionary copyDictionary(Dictionary in) {
    if(in == null) {
      return null;
    }
    Hashtable out = new Hashtable();
    Enumeration keys = in.keys();
    while(keys.hasMoreElements()) {
      Object key = keys.nextElement();
      out.put(key, copyValue(in.get(key)));
    }
    return out;
  }

  /**
   ** Recursive comparison of two dictionaries for equality.
   **
   ** Equality is defined as (o1 == null && 02 == null) || o1.equals(o2)
   **
   ** @param first The first Dictionary to compare.
   ** @param second The second Dictionary to compare.
   **
   ** @return true if both dictonaries are either null or recursively equal.
   **/

  static public boolean dictionariesAreEqual(Dictionary first, Dictionary second) {
    if(bothAreNull(first, second)) return true;
    if(onlyOneIsNull(first, second)) return false;
    if(sizeIsNotEqual(first, second)) return false;

    boolean result = true;
    Enumeration keys = first.keys();
    while(result && keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object v1 = first.get(key);
      Object v2 = second.get(key);
      result = valuesAreEqual(v1,v2);
    }
    return result;
  }

  static public boolean dictionariesAreNotEqual(Dictionary first, Dictionary second) {
    return !dictionariesAreEqual(first, second);
  }

  static private Object copyValue(Object in) {
    if(in == null) {
      return null;
    }
    if(in.getClass().isArray()) {
      return copyArray(in);
    } else if(in instanceof Vector) {
      return copyVector((Vector)in);
    } else {
      return in;
    }
  }

  static private Vector copyVector(Vector in) {
    if(in == null) {
      return null;
    }
    Vector out = new Vector();
    Enumeration elements = in.elements();
    while(elements.hasMoreElements()) {
      out.addElement(copyValue(elements.nextElement()));
    }
    return out;
  }

  static private Object copyArray(Object in) {
    if(in == null) {
      return null;
    }
    int length = Array.getLength(in);
    Object out = Array.newInstance(in.getClass().getComponentType(), length);
    for(int i = 0; i < length; ++i) {
      Array.set(out, i, copyValue(Array.get(in, i))); 
    }
    return out;
  }

  static private boolean valuesAreEqual(Object first, Object second) {
    if(bothAreNull(first, second)) return true;
    if(onlyOneIsNull(first, second)) return false;
    if(classesAreNotEqual(first, second)) return false;

    if(first.getClass().isArray()) {
      return arraysAreEqual(first, second);
    } else if(first instanceof Vector) {
      return vectorsAreEqual((Vector)first, (Vector)second);
    } else {
      return first.equals(second);
    }
  }

  static private boolean vectorsAreEqual(Vector first, Vector second) {
    if(bothAreNull(first, second)) return true;
    if(onlyOneIsNull(first, second)) return false;
    if(sizeIsNotEqual(first, second)) return false;
    boolean result = true;
    for(int i = first.size(); result && i < first.size(); ++i) {
      result = valuesAreEqual(first.elementAt(i), second.elementAt(i));
    }
    return result;
  }

  static private boolean arraysAreEqual(Object first, Object second) {
    if(bothAreNull(first, second)) return true;
    if(onlyOneIsNull(first, second)) return false;
    if(lengthIsNotEqual(first, second)) return false;
    int length = Array.getLength(first);
    boolean result = true;
    for(int i = 0; result && i < length; ++i) {
      result = valuesAreEqual(Array.get(first, i), Array.get(second, i));
    }
    return result;
  }

  static private boolean bothAreNull(Object first, Object second) {
    return first == null && second == null;
  }

  static private boolean onlyOneIsNull(Object first, Object second) {
    return !bothAreNull(first, second) && (first == null || second == null);
  }

  static private boolean sizeIsNotEqual(Dictionary first, Dictionary second) {
    return first.size() != second.size();
  }

  static private boolean sizeIsNotEqual(Vector first, Vector second) {
    return first.size() != second.size();
  }

  static private boolean lengthIsNotEqual(Object first, Object second) {
    return Array.getLength(first) != Array.getLength(second);
  }

  static private boolean classesAreNotEqual(Object first, Object second) {
    return !first.getClass().equals(second.getClass());
  }
}
