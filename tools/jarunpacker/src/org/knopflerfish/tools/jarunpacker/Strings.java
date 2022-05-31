/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

package org.knopflerfish.tools.jarunpacker;

import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Enumeration;

import java.util.*;

/**
 * Utility class for string localization.
 *
 * Conventions:
 * <pre>
 * button_xx      String for xx to be placed on a graphical button
 * menu_xx        String for xx to be placed in a window menu
 * tooltip_xx     String for tooltip xx text
 * str_xx         Generic string for xx
 * </pre>
 */
public class Strings {

  private static ResourceBundle rb     = null;
  private static boolean bDebug        = false;
  private static String  RESOURCE_NAME = "strings";

  static {
    load();
  }

  static ResourceBundle identityResourceBundle = new ResourceBundle() {
      Hashtable keys = new Hashtable();
      public Object handleGetObject(String key) {
        keys.put(key, "");
        return key;
      }
      public Enumeration getKeys() {
        return  keys.keys();
      }
  };


  static void load() {

    Locale locale = null;

    if(rb == null) {
      if(bDebug) {
        System.out.println("loading resource " + RESOURCE_NAME + ", locale=" +
                           locale);
      }
      try {
        if(locale == null) {
          rb = ResourceBundle.getBundle(RESOURCE_NAME);
        } else {
          rb = ResourceBundle.getBundle(RESOURCE_NAME, locale);
        }
        if(bDebug) {
          System.out.println("loaded resource " + RESOURCE_NAME +
                             " and locale=" + locale);
          for(Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            System.out.println(key + " = '" + rb.getString(key) + "'");
          }
        }
      } catch (Exception e) {
        System.out.println("Failed to load resources for " + RESOURCE_NAME +
                           " and locale=" + locale);
      }
    }
    if(rb == null) {
      System.out.println("reverting to identity resource " +
                         RESOURCE_NAME +" and locale=" + locale);
      rb = identityResourceBundle;
    }
  }


  /**
   * Utility method to get a resource string.
   */
  static public String get(String key) {
    try {
      if(rb != null) {
        return rb.getString(key);
      }
    } catch (Exception e) {
      System.out.println("Failed to get " + key + ": " + e);
    }
    return key;
  }

  /**
   * Utility method to check existance of a resource string.
   */
  static public boolean exists(String key) {
    try {
      if(rb != null) {
        String val = rb.getString(key);
        return true;
      }
    } catch (Exception e) {
      // This means the key disn't exist
    }
    return false;
  }



  /**
   * Get a localized array.
   *
   * This is equivalent to
   * <pre>
   *   String[] array = Strings.splitwords(getString(key), ",");
   * </pre>
   */
  public static String[] getArray(String key) {
    String s = get(key);
    return splitwords(s, ", ");
  }



  /**
   * Factor to use when computing binary bytes.
   */
  static final long binaryByteFactor = 1024;

  /**
   * Separator between the number and the unit.
   */
  static final String sep = " ";

  /**
   * The unit.
   */
  static final String unit = "B";

  /**
   * Format value using ki, Mi, Gi, ... using multiples of 1024. I.e.,
   * binary "bytes".
   *
   * @param value the byte value to format.
   */
  public static String fmtByte(long value) {
    String formatedValue = "";
    String[] suffixes = new String[]{ "",  "Ki","Mi",
                                      "Gi","Ti","Pi",
                                      "Ei","Zi","Yi"};
    int ix = 0;
    long factor = 1;

    while(value/factor>binaryByteFactor && ix<suffixes.length) {
      factor *= binaryByteFactor;
      ix++;
    }

    long i = value/factor;    // Integral part of reduced value
    long r = value -i*factor; // Remainder.
    // Convert r to a decimal fraction
    double fraction  = ((double) r)/((double) factor);
    formatedValue = formatValue(i, fraction);

    return formatedValue +sep +suffixes[ix] +unit;
  }

  private static String formatValue( long integral, double fraction)
  {
    String res = String.valueOf(integral);
    if (integral<10) { // Append two digits from the fraction
      int dec = (int) (fraction*100);
      res = res +"." +(dec<10?"0":"") +String.valueOf(dec);
    } else if (integral<100) { // Append one digit from the fraction
      int dec = (int) (fraction*10);
      res = res +"." +String.valueOf(dec);
    }
    return res;
  }


  /**
   * Format a string with argument.
   *
   * @param key Key to lookup using <code>get(key)<code>.
   *            The resulting string is used as source string for
   *            argument substitution.
   *            <code>$(1)</code> is replaced with <code>arg1</code>
   * @param arg1 Replacement string.
   */
  public static String fmt(String key, Object arg1) {
    return replace(get(key), "$(1)",
                   arg1 != null ? arg1.toString() : "null");
  }

  /**
   * Format a string with arguments.
   *
   * @param key Key to lookup using <code>get(key)<code>.
   *            The resulting string is used as source string for
   *            argument substitution.
   *            <code>$(1)</code> is replaced with <code>arg1</code><br>
   *            <code>$(2)</code> is replaced with <code>arg2</code>
   * @param arg1 Replacement string.
   * @param arg2 Replacement string.
   */
  public static String fmt(String key, Object arg1, Object arg2) {
    return replace(fmt(key, arg1), "$(2)",
                   arg2 != null ? arg2.toString() : "null");
  }

  public static String fmt(String key, Object arg1, Object arg2, Object arg3) {
    return replace(fmt(key, arg1, arg2), "$(3)",
                   arg3 != null ? arg3.toString() : "null");
  }


/**
   * Replace occurances of substrings.
   *
   * @param s  Source string.
   * @param v1 String to be replaced with <code>v2</code>.
   * @param v2 String replacing <code>v1</code>.
   * @return Modified string. If any of the input strings are <tt>null</tt>,
   *         the source string will be returned unmodified.
   */
  public static String replace(final String s,
                               final String v1,
                               final String v2) {

    // return quick when nothing to do
    if(s == null
       || v1 == null
       || v2 == null
       || v1.length() == 0
       || v1.equals(v2)) {
      return s;
    }

    int ix       = 0;
    int v1Len    = v1.length();
    int n        = 0;

    // count number of occurances to be able to correctly size
    // the resulting output char array
    while(-1 != (ix = s.indexOf(v1, ix))) {
      n++;
      ix += v1Len;
    }

    // No occurances at all, just return source string
    if(n == 0) {
      return s;
    }

    // Set up an output char array of correct size
    int     start  = 0;
    int     v2Len  = v2.length();
    char[]  r      = new char[s.length() + n * (v2Len - v1Len)];
    int     rPos   = 0;

    // for each occurance, copy v2 where v1 used to be
    while(-1 != (ix = s.indexOf(v1, start))) {
      while(start < ix) r[rPos++] = s.charAt(start++);
      for(int j = 0; j < v2Len; j++) {
        r[rPos++] = v2.charAt(j);
      }
      start += v1Len;
    }

    // ...and add all remaining chars
    ix = s.length();
    while(start < ix) r[rPos++] = s.charAt(start++);

    // ..ouch. this hurts.
    return new String(r);
  }

  /** default whitespace for splitwords() (<code>" \t\n\r"</code>) */
  protected static String  WHITESPACE = " \t\n\r";

  /** citation char (<code>'"'</code>) for word grouping. Used by splitwords() */
  protected static char   CITCHAR    = '"';


  /**
   * Split a string into words separated by whitespace
   * (<code>Text.WHITESPACE</code>).
   * <p>
   * Citation chars <code>Text.CITCHAR</code> may be used to group words
   * with embedded whitespace.
   * </p>
   */
  public static String [] splitwords(String s) {
    return splitwords(s, WHITESPACE);
  }

  /**
   * Split a string into words separated by whitespace.
   * Citation chars '"' may be used to group words with embedded
   * whitespace.
   *
   * @param s String to split.
   * @param whiteSpace whitespace to use for splitting. Any of the
   *                   characters in the whiteSpace string are considered
   *                   whitespace between words and will be removed
   *                   from the result.
   */
  public static String [] splitwords(String s, String whiteSpace) {
    boolean       bCit  = false;        // true when inside citation chars.
    Vector        v     = new Vector(); // (String) individual words after splitting
    StringBuilder buf   = null;
    int           i     = 0;

    while(i < s.length()) {
      char c = s.charAt(i);

      if(bCit || whiteSpace.indexOf(c) == -1) {
        // Build up word until we breaks on either a citation char or whitespace
        if(c == CITCHAR) {
          bCit = !bCit;
        } else {
          if(buf == null) {
            buf = new StringBuilder();
          }
          buf.append(c);
        }
        i++;
      } else {
        // found whitespace or end of citation, append word if we have one
        if(buf != null) {
          v.addElement(buf.toString());
          buf = null;
        }

        // and skip whitespace so we start clean on a word or citation char
        while((i < s.length()) && (-1 != whiteSpace.indexOf(s.charAt(i)))) {
          i++;
        }
      }
    }
    // Add possible remaining word
    if(buf != null) {
      v.addElement(buf.toString());
    }

    // Copy back into an array
    String [] r = new String[v.size()];
    v.copyInto(r);

    return r;
  }



}
