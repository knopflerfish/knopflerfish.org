/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;
import org.knopflerfish.util.Text;
import java.util.*;
import java.lang.reflect.*;
import java.io.*;


/**
 * Implementation calss for AttributeDefinition.
 *
 * <p>
 * This class contains get and parse methods for operations
 * related to constructing AttributeDefinition.
 * </p>
 */
public class AD implements AttributeDefinition, Comparable {
  int      type;
  int      card;
  String[] defValue;
  String   desc;
  String   id;
  String   name;
  String[] optLabels;
  String[] optValues;


  boolean  bOptional = false;

  /**
   * String used for separating array and vector string 
   * representations.
   */
  static final String SEQUENCE_SEP = ",";

  /**
   * Create an AttributeDefinition with empty descrition and no option 
   * labels or option values.
   */
  public AD( String id, 
	     int type,
	     int card,
	     String name,
	     String[] defValue) {
    this(id, 
	 type, 
	 card, 
	 name, 
	 "",       // desc
	 defValue, // defValue
	 null, // optLabels
	 null  // optValues
	 );
  }

  /**
   * Create a new attribute definition.
   *
   * @param id Unique id of the definition
   * @param card cardinality of the definition
   * @param type One of the type constants <tt>STRING...BOOLEAN</tt>
   * @throws IllegalArgumentException if <i>type</i> is not supported.
   * @throws IllegalArgumentException if <i>id</i> is <tt>null</tt> or empty
   * @throws IllegalArgumentException if <i>desc</i> is <tt>null</tt>
   */
  public AD(  String   id,
	      int      type,
	      int      card,
	      String   name,
	      String   desc,
	      String[] defValue,
	      String[] optLabels,
	      String[] optValues) {
    if(type < STRING || type > BOOLEAN) {
      throw new IllegalArgumentException("Unsupported type " + type);
    }

    if(id == null || "".equals(id)) {
      throw new IllegalArgumentException("Bad id '" + id + "'");
    }

    if(desc == null) {
      throw new IllegalArgumentException("Description cannot be null");
    }
    
    if(defValue == null) {
      String s = "";
      switch(type) {
      case STRING:
	s = "";
	break;
      case INTEGER: 
      case LONG: 
      case SHORT: 
      case BYTE:  
      case BIGINTEGER:
	s = "0";
	break;
      case BIGDECIMAL:
      case DOUBLE:
      case FLOAT: 
	s = "0.0"; 
	break;
      case CHARACTER: 
	s = "-"; 
	break;
      case BOOLEAN:
	s = "false"; 
	break;
      }
      defValue = new String[] { s };
    }
    
    this.type      = type;
    this.card      = card;
    this.desc      = desc;
    this.id        = id;
    this.name      = name;
    this.optLabels = optLabels;
    this.optValues = optValues;

    setDefaultValue(defValue);
  }

  private AD() {
    throw new RuntimeException("Not supported");
  }

  public int getCardinality() {
    return card;
  }

  public String[] getDefaultValue() {
    return defValue;
  }

  public void setDescription(String s) {
    this.desc = s;
  }

  /**
   * Set the default value.
   *
   * @throws IllegalArgumentException if any of the values cannot be validated.
   */
  public void setDefaultValue(String[] value) {
    String s = validate(toString(value));
    
    if(s != null && !"".equals(s)) {
      throw new IllegalArgumentException("Bad default value '" + 
					 toString(value) + "' " + 
					 ", id=" + id + 
					 ", class=" + getClass(type) + 
					 ", err=" + s);
    }

    defValue = value;
  }
  
  public String getDescription() {
    return desc;
  }

  public String getID() {
    return id;
  }

  public String getName() {
    return name;
  }


  /**
   * Set values returned by <tt>getOptionValues</tt> and 
   * <tt>getOptionLabels</tt>.
   *
   * @param optValues Values to be return by <tt>getOptionValues</tt>. Can
   *                  be <tt>null</tt>.
   * @param optLabels Values to be return by <tt>getOptionLabels</tt>. Can
   *                  be <tt>null</tt> iff <tt>optValues</tt> is <tt>null</tt>.
   * @throws IllegalArgumentException if optValues and optLabels are not the
   *                                  same length.
   */
  public void setOptions(String[] optValues, 
			 String[] optLabels) {
    if(optValues != null) {
      if(optLabels == null || optValues.length != optLabels.length) {
	throw new IllegalArgumentException("Values must be same length as labels");
      }
    }
    this.optValues = optValues;
    this.optLabels = optLabels;
  }

  public String[] getOptionLabels() {
    return optLabels;
  }

  public String[] getOptionValues() {
    return optValues;
  }

  public int getType() {
    return type;
  }

  /**
   * Return true if this attribute is optional.
   */
  public boolean isOptional() {
    return bOptional;
  }

  /**
   * Get the attribute type given any suported java object.
   *
   * @param val Any java object, including arrays of primitive types.
   *            If <i>val</i> is a Vector, it must
   *            contain at least one element.
   * @return <tt>STRING...BOOLEAN</tt>
   * @throws IllegalArgumentException if type cannot be derived.
   */
  public static int getType(Object val) {
    if(val instanceof Vector) {
      Vector v = (Vector)val;
      if(v.size() == 0) {
	throw new IllegalArgumentException("Vector is empty " + 
					   "-- no type can be derived");
      } else {
	return getType(v.elementAt(0));
      }
    } else if(val.getClass().isArray()) {
      return getArrayType(val);
    } else {
      return getPrimitiveType(val);
    }
  }

  static Class BIGDECIMAL_PRIMITIVE = Double.TYPE;
  static Class BIGDECIMAL_OBJECT    = Double.class;

  static Class BIGINTEGER_PRIMITIVE = Integer.TYPE;
  static Class BIGINTEGER_OBJECT    = Integer.class;


  static final Class[] ARRAY_CLASSES     = new Class[BOOLEAN-STRING + 1];
  static final Class[] PRIMITIVE_CLASSES = new Class[] {
    String.class,  
    Long.TYPE,  
    Integer.TYPE,  
    Short.TYPE,  
    Character.TYPE,  
    Byte.TYPE,  
    Double.TYPE,      
    Float.TYPE,  
    BIGINTEGER_PRIMITIVE,  
    BIGDECIMAL_PRIMITIVE,  
    Boolean.TYPE,  
  };
  static final Class[] OBJECT_CLASSES = new Class[] {
    String.class,  
    Long.class,  
    Integer.class,  
    Short.class,  
    Character.class,  
    Byte.class,  
    Double.class,      
    Float.class,  
    BIGINTEGER_OBJECT,  
    BIGDECIMAL_OBJECT,  
    Boolean.class,  
  };

  static {    
    try {
      BIGDECIMAL_PRIMITIVE = Class.forName("java.math.BigDecimal");
      BIGDECIMAL_OBJECT    = BIGDECIMAL_PRIMITIVE;
    } catch (Throwable t) {
      //      System.out.println("no BigDecimal");
    }
    try {
      BIGINTEGER_PRIMITIVE = Class.forName("java.math.BigInteger");
      BIGINTEGER_OBJECT    = BIGINTEGER_PRIMITIVE;
    } catch (Throwable t) {
      //      System.out.println("no BigInteger");
    }
    
    /*
    System.out.println("BIGINTEGER_PRIMITIVE=" + BIGINTEGER_PRIMITIVE);
    System.out.println("BIGINTEGER_OBJECT=" + BIGINTEGER_OBJECT);
    System.out.println("BIGDECIMAL_PRIMITIVE=" + BIGDECIMAL_PRIMITIVE);
    System.out.println("BIGDECIMAL_OBJECT=" + BIGDECIMAL_OBJECT);
    */

    try {
      for(int i = STRING; i <= BOOLEAN; i++) {
	ARRAY_CLASSES[i-STRING] = 
	  (Array.newInstance(getPrimitiveClass(i),0)).getClass();
      }
      /*
      for(int i = STRING; i <= BOOLEAN; i++) {
	Object array = Array.newInstance(getPrimitiveClass(i), 0);

	System.out.println(i + ": " + getPrimitiveClass(i).getName() + 
			   ", " + getClass(i).getName() + 
			   ", " + array.getClass().getName() + 
			   ", " + getArrayType(array));
	
      }
      */
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  /**
   * Get type from an array object.
   *
   * @param val an array object
   * @return <tt>STRING...BOOLEAN</tt>
   * @throws IllegalArgumentException if type cannot be derived.
   */
  public static int getArrayType(Object val) {

    // Isn't there an easier way of doing this? Like
    // Array.getElementClass() or similar?
    for(int i = STRING; i <= BOOLEAN; i++) {
      if(ARRAY_CLASSES[i-STRING].equals(val.getClass())) {
	return i;
      }
    }
    throw new IllegalArgumentException("Unsupported type " + val.getClass().getName());
  }

  /**
   * Get type from primitive object.
   *
   * @param val an object of one of the boxed primitive java object classes.
   * @return <tt>STRING...BOOLEAN</tt>
   * @throws IllegalArgumentException if type cannot be derived.
   */
  public static int getPrimitiveType(Object val) {
    if(val instanceof String) {
      return STRING;
    } else if(val instanceof Integer) {
      return INTEGER;
    } else if(val instanceof Double) {
      return DOUBLE;
    } else if(val instanceof Float) {
      return FLOAT;
    } else if(val instanceof Integer) {
      return INTEGER;
    } else if(val instanceof Long) {
      return LONG;
    } else if(val instanceof Boolean) {
      return BOOLEAN;
    } else if(val instanceof Short) {
      return SHORT;
    } else if(val instanceof Character) {
      return CHARACTER;
    } else if(BIGINTEGER_OBJECT.isAssignableFrom(val.getClass())) {
      return BIGINTEGER;
    } else if(BIGDECIMAL_OBJECT.isAssignableFrom(val.getClass())) {
      return BIGDECIMAL;
    } else if(val instanceof String) {
      return STRING;
    } else {
      throw new IllegalArgumentException("Unsupported type " + val.getClass().getName());
    }
  }

  /**
   * Implementation of validation function.
   *
   * <p>
   * Validation of primitive types is performed by trying to create an'
   * object from the corresponding String constructor.
   *</p>
   * <p>
   * Validation of arrays and vectors is performed by splitting
   * the input string into comma-separated words.
   * </p>
   */
  public String validate(String value) {
    if(card == Integer.MIN_VALUE) {
      return validateMany(value, type, Integer.MAX_VALUE);
    } else if(card == Integer.MAX_VALUE) {
      return validateMany(value, type, Integer.MAX_VALUE);
    } else if(card < 0) {
      return validateMany(value, type, -card);
    } else if(card > 0) {
      return validateMany(value, type, card);
    } else {
      return validateSingle(value, type);
    }
  }

  /**
   * Parse a string value to an object given a cardinality and type.
   */
  public static Object parse(String value, int card, int type) {
    if(card < 0) {
      return parseMany(value, type);
    } else if(card > 0) {
      Vector v = parseMany(value, type);
      Object array = Array.newInstance(getPrimitiveClass(type), 
				       v.size());
      for(int i = 0; i < v.size(); i++) {
	Array.set(array, i, v.elementAt(i));
      }
      return array;
    } else {
      return parseSingle(value, type);
    }
  }


  static Vector parseMany(String value, 
			  int type) {
    String[] items = Text.splitwords(value, SEQUENCE_SEP, '\"');

    //    System.out.println("AD.parseMany '" + value + "', item count=" + items.length);
    Vector v = new Vector();
    for(int i = 0; i < items.length; i++) {
      v.addElement(parseSingle(items[i], type));
    }
    return v;
  }


  static String validateMany(String value, 
			     int type,
			     int maxItems) {

    int n = 0;

    String[] items = Text.splitwords(value, SEQUENCE_SEP, '\"');

    if(maxItems == 0) {
      if(items.length != 1) {
	return "Expected one item, found " + items.length;
      }
    }
    
    if(items.length > maxItems) {
      return "Max # of items are " + maxItems + ", found " + items.length;
    }

    StringBuffer sb = new StringBuffer();
    for(int i = 0; i < items.length; i++) {
      String s = validateSingle(items[i], type);
      if(s != null && !"".equals(s)) {
	if(sb.length() != 0) {
	  sb.append(", ");
	}
	sb.append(s);
      }
    }

    return sb.toString();
  }

  static String validateSingle(String value, int type) {
    try {
      switch(type) {
      case STRING: 
	if(value == null) {
	  throw new IllegalArgumentException("Strings cannot be null");
	}
	break;
      case INTEGER: 
	Integer.parseInt(value.trim());
	break;
      case LONG: 
	Long.parseLong(value.trim());
	break;
      case BYTE: 
	Byte.parseByte(value.trim());
	break;
      case SHORT: 
	Short.parseShort(value.trim());
	break;
      case CHARACTER: 
	if(value.length() != 1) {
	  throw new IllegalArgumentException("Character strings must be of length 1");
	}
	break;
      case DOUBLE: 
	Double.parseDouble(value.trim());
	break;
      case FLOAT: 
	Float.parseFloat(value.trim());
	break;
      case BOOLEAN:
	if(!("true".equals(value.trim()) || "false".equals(value.trim()))) {
	  throw new IllegalArgumentException("Booleans must be 'true' or 'false'");
	}
	break;
      default:
	break;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
    return "";
  }

  /**
   * Parse a single value into an object given a type.
   */
  public static Object parseSingle(String value, int type) {

    switch(type) {
    case STRING: 
      return value;
    case INTEGER: 
      return new Integer(value.trim());
    case LONG: 
      return new Long(value.trim());
    case BYTE: 
      return new Byte(value.trim());
    case SHORT: 
      return new Short(value.trim());
    case CHARACTER: 
      return new Character(value.charAt(0));
    case DOUBLE: 
      return new Double(value.trim());
    case FLOAT: 
      return new Float(value.trim());
    case BOOLEAN:
      return "true".equals(value.trim()) ? Boolean.TRUE : Boolean.FALSE;
    default:
      throw new IllegalArgumentException("Cannot parse '" + value + "' to type=" + type);
    }
  }

  /**
   * Get java class corresponding to AttributeDefinition type.
   *
   * @throws IllegalArgumentException if type is not supporte.d
   */
  public static Class getClass(int type) {
    return OBJECT_CLASSES[type - STRING];
  }

  /**
   * Get the primitive java class from a specificed type.
   *
   * @throws IllegalArgumentException if type is not supported.
   */
  public static Class getPrimitiveClass(int type) {
    return PRIMITIVE_CLASSES[type - STRING];
  }

  /**
   * Convert to human-readable string.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append("AD[");

    sb.append("id=" + id);
    sb.append(", type=" + type);
    sb.append(", name=" + name);
    sb.append(", desc=" + desc);
    sb.append(", cardinality=" + card);

    sb.append(", defValue=" + toString(defValue));
    sb.append(", optLabels=" + toString(optLabels));
    sb.append(", optValues=" + toString(optValues));

    sb.append("]");

    return sb.toString();
  }

  /**
   * Convert a object to string  that can be parsed by <tt>parse</tt>
   */
  public static String toString(Object obj) {
    if(obj.getClass().isArray()) {
      return toStringFromArray(obj);
    } else if(obj instanceof Vector) {
      StringBuffer sb = new StringBuffer();
      Vector v = (Vector)obj;
      for(int i = 0; i < v.size(); i++) {
	String s = (String)v.elementAt(i);
	sb.append(escape(s));
	if(i < v.size() - 1) {
	  sb.append(SEQUENCE_SEP);
	}
      }
      return sb.toString();
    } else {
      return obj.toString();
    }
  }

  /**
   * Escape a string so that it'll be parsed as one item, even
   * if it contains SEQUENCE_SEP.
   */
  public static String escape(String s) {
    boolean bNeedEscape = s.indexOf(SEQUENCE_SEP) != -1;
    if(bNeedEscape) {
      if(s.length() > 1 && s.startsWith("\"") && s.endsWith("\"")) {
	bNeedEscape = false;
      }
    }
    if(bNeedEscape) {
      return "\"" + s + "\"";
    } else {
      return s;
    }
  }

  public static String toStringFromArray(Object array) {
    StringBuffer sb = new StringBuffer();

    if(array == null) {
      sb.append("null");
    } else {
      for(int i = 0; i < Array.getLength(array); i++) {
	String s = escape(Array.get(array, i).toString());
	
	sb.append(s);

	if(i < Array.getLength(array) - 1) {
	  sb.append(SEQUENCE_SEP);
	}
      }
    }
    return sb.toString();
  }

  public static String toString(Object[] values) {
    StringBuffer sb = new StringBuffer();

    if(values == null) {
      sb.append("null");
    } else {
      for(int i = 0; i < values.length; i++) {
	String s = escape(values[i].toString());
	
	sb.append(s);
	
	if(i < values.length - 1) {
	  sb.append(SEQUENCE_SEP);
	}
      }
    }
    return sb.toString();
  }

  public static String toString(Vector values) {
    StringBuffer sb = new StringBuffer();

    if(values == null) {
      sb.append("null");
    } else {
      for(int i = 0; i < values.size(); i++) {
	sb.append(values.elementAt(i));
	if(i < values.size() - 1) {
	  sb.append(SEQUENCE_SEP);
	}
      }
    }
    return sb.toString();
  }


  public int compareTo(Object other) {
    return id.compareTo(((AD)other).id);
  }

  public int hashCode() {
    return id.hashCode();
  }

  public boolean equals(Object other) {
    if(other == null || !(other instanceof AD)) {
      return false;
    }

    return id.equals(((AD)other).id);
  }
}
