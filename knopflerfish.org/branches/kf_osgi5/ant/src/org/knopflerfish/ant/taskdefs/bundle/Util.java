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

package org.knopflerfish.ant.taskdefs.bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.Version;


public class Util {

  // Type names used for types OSGi attributes in manifest headers.
  private static final String DOUBLE_TYPE = "Double";
  private static final String LONG_TYPE = "Long";
  private static final String LIST_TYPE = "List";
  private static final String STRING_TYPE = "String";
  private static final String VERSION_TYPE = "Version";


  /**
   * Parse strings of format:
   *
   * ENTRY (, ENTRY)*
   *
   * @param d Directive being parsed
   * @param s String to parse
   * @return A HashSet with enumeration or null if enumeration string was null.
   * @exception IllegalArgumentException If syntax error in input string.
   */
  public static Set<String> parseEnumeration(String d, String s)
  {
    final HashSet<String> result = new HashSet<String>();
    if (s != null) {
      final AttributeTokenizer at = new AttributeTokenizer(s);
      do {
        final String key = at.getKey(true);
        if (key == null) {
          throw new IllegalArgumentException("Directive " + d
                                             + ", unexpected character at: "
                                             + at.getRest());
        }
        if (!at.getEntryEnd()) {
          throw new IllegalArgumentException("Directive " + d
                                             + ", expected end of entry at: "
                                             + at.getRest());
        }
        result.add(key);
      } while (!at.getEnd());
      return result;
    } else {
      return null;
    }
  }


  /**
   * Parse manifest header values on format:
   * <pre>
   * ENTRY (',' ENTRY)*
   * ENTRY = key (';' key)* (';' PARAM)*
   * PARAM = attribute (':' TYPE)? '=' value
   * PARAM = directive ':=' value
   * TYPE = SCALAR | LIST
   * SCALAR = 'String' | 'Version' | 'Long' | 'Double'
   * LIST = 'List<' SCALAR '>'
   * </pre>
   *
   * The default attribute value type is 'String'. For list values the 'List'
   * and its following '<' are treated as separate tokens to comply with the
   * OSGi TCK.
   *
   * The parse result is one {@link HeaderEntry}-instance for each entry.
   * If {@code single} is true then the entry only contains one key that can be
   * accesses by calling {@link HeaderEntry#getKey()}.
   *
   * If {@code unique} is true the attribute values in the map are scalars
   * otherwise the values from different attribute definitions with the same
   * name are wrapped in a {@code List<?>}.
   *
   * @param a Name of attribute being parsed, for error messages.
   * @param s String to parse.
   * @param single If true, only allow one key per ENTRY.
   * @param unique Only allow unique attributes for each ENTRY.
   * @param single_entry If true, only allow one ENTRY in {@code s}.
   *
   * @return List of {@link HeaderEntry}-object, one per entry in {@code s}.
   *
   * @exception IllegalArgumentException If syntax error in input string.
   */
  public static List<HeaderEntry> parseManifestHeader(String a,
                                                      String s,
                                                      boolean single,
                                                      boolean unique,
                                                      boolean single_entry)
  {
    final List<HeaderEntry> res = new ArrayList<Util.HeaderEntry>();

    if (s != null) {
      final AttributeTokenizer at = new AttributeTokenizer(s);
      do {
        final HeaderEntry he = new HeaderEntry(a, single);
        String key = at.getKey(single);
        if (key == null) {
          final String msg = "Definition, " + a + ", expected key at: "
              + at.getRest() + ". Key values are terminated "
              + "by a ';' or a ',' and may not "
              + "contain unquoted ':', '=' if multiple keys are allowed.";
          throw new IllegalArgumentException(msg);
        }
        he.keys.add(key);
        if (!single) {
          while ((key = at.getKey(false)) != null) {
            he.keys.add(key);
          }
        }
        String param;
        while ((param = at.getParam()) != null) {
          final boolean is_directive = at.isDirective();
          if (is_directive) {
            if (he.directives.containsKey(param)) {
              final String msg = "Definition, " + a + ", duplicate directive: "
                    + param;
              throw new IllegalArgumentException(msg);
            }
            final String valueStr = at.getValue(false);
            if (valueStr == null) {
              final String msg = "Definition, " + a + ", expected value for "
                  + " directive " + param + " at: " + at.getRest();
              throw new IllegalArgumentException(msg);
            }
            he.directives.put(param, valueStr);
          } else {
            // Attribute definition with optional type
            final Object old = he.attributes.get(param);
            if (old != null && unique) {
              final String msg = "Definition, " + a + ", duplicate attribute: "
                                + param;
              throw new IllegalArgumentException(msg);
            }
            final String paramType = at.getParamType();
            final boolean keepEscape = paramType != null
                                       && paramType.startsWith("List");
            final String valueStr = at.getValue(keepEscape);
            if (valueStr == null) {
              final String msg = "Definition, " + a + ", expected value for "
                  + " attribute " + param + " at: " + at.getRest();
              throw new IllegalArgumentException(msg);
            }
            final Object value = toValue(a, param, paramType, valueStr);
            if (unique) {
              he.attributes.put(param, value);
            } else {
              @SuppressWarnings("unchecked")
              List<Object> oldValues = (List<Object>) old;
              if (oldValues == null) {
                oldValues = new ArrayList<Object>();
                he.attributes.put(param, oldValues);
              }
              oldValues.add(value);
            }
          }
        }
        if (at.getEntryEnd()) {
          res.add(he);
        } else {
          throw new IllegalArgumentException("Definition, " + a
              + ", expected end of entry at: " + at.getRest());
        }
        if (single_entry && !at.getEnd()) {
          throw new IllegalArgumentException("Definition, " + a
              + ", expected end of single entry at: " + at.getRest());
        }
      } while (!at.getEnd());
    }

    return res;
  }


  /**
   * Convert an attribute value from string to the requested type.
   *
   * The types supported are described in
   * {@link #parseEntries(String, String, boolean, boolean, boolean)}.
   *
   * @param a Name of attribute being parsed, for error messages.
   * @param p Name of parameter to assign the value to, for error messages.
   * @param type the type to convert to.
   * @param value the value to convert.
   * @return
   */
  private static Object toValue(String a,
                                String param,
                                String type,
                                String value)
  {
    Object res;

    type = type == null ? STRING_TYPE : type.intern();
    if (STRING_TYPE == type) {
      res = value;
    } else if (LONG_TYPE == type) {
      try {
        res = new Long(value.trim());
      } catch (final Exception e) {
        throw (IllegalArgumentException) new
        IllegalArgumentException("Definition, " +a
                                 +", expected value of type Long but found '"
                                 +value +"' for attribute '"
                                 +param + "'.").initCause(e);
      }
    } else if (DOUBLE_TYPE == type) {
      try {
        res = new Double(value.trim());
      } catch (final Exception e) {
        throw (IllegalArgumentException) new
        IllegalArgumentException("Definition, " +a
                                 +", expected value of type Double but found '"
                                 +value +"' for attribute '"
                                 +param + "'.").initCause(e);
      }
    } else if (VERSION_TYPE == type) {
      try {
        res = new Version(value);
      } catch (final Exception e) {
        throw (IllegalArgumentException) new
        IllegalArgumentException("Definition, " +a
                                 +", expected value of type Version but found '"
                                 +value +"' for attribute '"
                                 +param + "'.").initCause(e);
      }
    } else if (type.startsWith(LIST_TYPE)) {
      String elemType = type.substring(LIST_TYPE.length()).trim();
      // Let "List" without any "<type>" default to "List<String>"
      if (elemType.length()>0) {
        if ('<' != elemType.charAt(0)
            || elemType.charAt(elemType.length() - 1) != '>') {
          throw new IllegalArgumentException
            ("Definition, " + a + ", expected List type definition '"
                + type + "' for attribute '" + param + "'.");
        }
        elemType = elemType.substring(1, elemType.length() - 1).trim().intern();
      }
      // The default element type is STRING.
      if (elemType.length()==0) {
        elemType = STRING_TYPE;
      }

      try {
        final List<String> elements = splitWords(value, ',', STRING_TYPE!=elemType);
        final List<Object> l = new ArrayList<Object>(elements.size());
        for (final String elem : elements) {
          l.add(toValue(a, param, elemType, elem));
        }
        res = l;
      } catch (final Exception e) {
        throw (IllegalArgumentException) new IllegalArgumentException
          ("Definition, " + a + ", expected '" + type + "' value but found '"
              + value + "' for attribute '" + param + "'.").initCause(e);
      }
    } else {
        throw new IllegalArgumentException("Definition, " +a
                                 +", unknown type '" +type +"' for attribute '"
                                 +param + "'.");
    }
    return res;
  }

  /**
   * Default whitespace string for splitwords(). Value is <tt>" \t\n\r"</tt>)
   */
  protected static String WHITESPACE = " \t\n\r";

  /**
   * Default citation char for splitwords(). Value is <tt>'"'</tt>
   */
  protected static char CITCHAR = '"';


  /**
   * Utility method to split a string into words separated by whitespace.
   *
   * <p>
   * Equivalent to <tt>splitwords(s, WHITESPACE)</tt>
   * </p>
   */
  public static String[] splitwords(String s) {
    return splitwords(s, WHITESPACE);
  }


  /**
   * Utility method to split a string into words separated by whitespace.
   *
   * <p>
   * Equivalent to <tt>splitwords(s, WHITESPACE, CITCHAR)</tt>
   * </p>
   */
  public static String[] splitwords(String s, String whiteSpace) {
    return splitwords(s, whiteSpace, CITCHAR);
  }


  /**
   * Split a string into words separated by whitespace.
   * <p>
   * Citation chars may be used to group words with embedded whitespace.
   * </p>
   *
   * @param s String to split.
   * @param whiteSpace whitespace to use for splitting. Any of the characters in
   *          the whiteSpace string are considered whitespace between words and
   *          will be removed from the result. If no words are found, return an
   *          array of length zero.
   * @param citChar Citation character used for grouping words with embedded
   *          whitespace. Typically '"'
   */
  public static String[] splitwords(String s,
                                     String whiteSpace,
                                     char citChar) {
    boolean bCit = false; // true when inside citation chars.
    final Vector<String> v = new Vector<String>(); // (String) individual words after splitting
    StringBuffer buf = new StringBuffer();
    int i = 0;

    while (i < s.length()) {
      final char c = s.charAt(i);

      if (bCit || whiteSpace.indexOf(c) == -1) {
        // Build up word until we breaks on either a citation char or whitespace
        if (c == citChar) {
          bCit = !bCit;
        } else {
          if (buf == null) {
            buf = new StringBuffer();
          }
          buf.append(c);
        }
        i++;
      } else {
        // found whitespace or end of citation, append word if we have one
        if (buf != null) {
          v.addElement(buf.toString());
          buf = null;
        }

        // and skip whitespace so we start clean on a word or citation char
        while ((i < s.length()) && (-1 != whiteSpace.indexOf(s.charAt(i)))) {
          i++;
        }
      }
    }

    // Add possible remaining word
    if (buf != null) {
      v.addElement(buf.toString());
    }

    // Copy back into an array
    final String[] r = new String[v.size()];
    v.copyInto(r);

    return r;
  }


  /**
   * Split a string into words separated by a separator char.
   *
   * If the separator char shall be part of a word it must be escaped with a
   * '\' (\u005C). One level of escaping is consumed by this method.
   *
   * @param s String to split.
   * @param sepChar separator char to split on.
   * @param trim trim whitespace from the words if {@code true}.
   *
   * @return List with the words of the specified string.
   */
  public static List<String> splitWords(String s,
                                        char sepChar,
                                        boolean trim)
  {
    final List<String> res = new ArrayList<String>();
    final StringBuffer buf = new StringBuffer();
    int pos = 0;
    final int length = s.length();

    boolean esc = false;
    int end = 0;
    for (; pos < length; pos++) {
      if (esc) {
        esc = false;
        buf.append(s.charAt(pos));
        end = buf.length();
      } else {
        final char c = s.charAt(pos);
        if (c == '\\') {
          esc = true;
        } else if (c == sepChar) {
          // trim trailing whitespace.
          if (trim) {
            buf.setLength(end);
          }
          res.add(buf.toString());
          buf.setLength(0);
          end = 0;
        } else if (Character.isWhitespace(c)) {
          if (buf.length()>0 || !trim) {
            buf.append(c);
          }
        } else {
          buf.append(c);
          end = buf.length();
        }
      }
    }
    if (esc) {
      throw new IllegalArgumentException("Value ends on escape character");
    }
    // The last element.
    if (trim) {
      buf.setLength(end);
    }
    res.add(buf.toString());

    return res;
  }


  /**
   * Replace all occurrences of a substring with another string.
   *
   * <p>
   * The returned string will shrink or grow as necessary depending on the
   * lengths of <tt>v1</tt> and <tt>v2</tt>.
   * </p>
   *
   * <p>
   * Implementation note: This method avoids using the standard String
   * manipulation methods to increase execution speed. Using the
   * <tt>replace</tt> method does however include two <tt>new</tt> operations in
   * the case when matches are found.
   * </p>
   *
   *
   * @param s
   *          Source string.
   * @param v1
   *          String to be replaced with <code>v2</code>.
   * @param v2
   *          String replacing <code>v1</code>.
   * @return Modified string. If any of the input strings are <tt>null</tt>, the
   *         source string <tt>s</tt> will be returned unmodified. If
   *         <tt>v1.length == 0</tt>, <tt>v1.equals(v2)</tt> or no occurrences
   *         of <tt>v1</tt> is found, also return <tt>s</tt> unmodified.
   *
   * @author Erik Wistrand
   */
  public static String replace(final String s,
                               final String v1,
                               final String v2) {

    // return quick when nothing to do
    if (s == null
        || v1 == null
        || v2 == null
        || v1.length() == 0
        || v1.equals(v2)) {
      return s;
    }

    int ix = 0;
    final int v1Len = v1.length();
    int n = 0;

    // count number of occurances to be able to correctly size
    // the resulting output char array
    while (-1 != (ix = s.indexOf(v1, ix))) {
      n++;
      ix += v1Len;
    }

    // No occurances at all, just return source string
    if (n == 0) {
      return s;
    }

    // Set up an output char array of correct size
    int start = 0;
    final int v2Len = v2.length();
    final char[] r = new char[s.length() + n * (v2Len - v1Len)];
    int rPos = 0;

    // for each occurance, copy v2 where v1 used to be
    while (-1 != (ix = s.indexOf(v1, start))) {
      while (start < ix) {
        r[rPos++] = s.charAt(start++);
      }
      for (int j = 0; j < v2Len; j++) {
        r[rPos++] = v2.charAt(j);
      }
      start += v1Len;
    }

    // ...and add all remaining chars
    ix = s.length();
    while (start < ix) {
      r[rPos++] = s.charAt(start++);
    }

    // ..ouch. this hurts.
    return new String(r);
  }


  /**
   * Class for tokenize an attribute string.
   */
  static class AttributeTokenizer {

    final String s;
    int length;
    int pos = 0;


    AttributeTokenizer(final String input) {
      s = input;
      length = s.length();
    }

    // get word (non-whitespace chars) up to the next non-quoted
    // ',', ';' or ':', '=' if not valueWord is set
    String getWord(boolean keepEscapse, boolean valueWord) {
      skipWhite();
      boolean backslash = false;
      boolean quote = false;
      final StringBuffer val = new StringBuffer();
      int end = 0;
      loop: for (; pos < length; pos++) {
        if (backslash) {
          backslash = false;
          if (keepEscapse) {
            val.append('\\');
          }
          val.append(s.charAt(pos));
          end = val.length();
        } else {
          final char c = s.charAt(pos);
          switch (c) {
          case '"':
            quote = !quote;
            end = val.length();
            break;
          case '\\':
            backslash = true;
            break;
          case ':':
          case ',':
          case ';':
          case '=':
            if (!quote && !(valueWord && (c==':' || c=='='))) {
              break loop;
            }
            // Fall through
          default:
            val.append(c);
            if (!Character.isWhitespace(c)) {
              end = val.length();
            }
            break;
          }
        }
      }
      if (quote || backslash || end == 0) {
        return null;
      }
      val.setLength(end);
      return val.toString();
    }


    String getKey(boolean singleKey) {
      if (pos >= length) {
        return null;
      }
      final int save = pos;
      if (s.charAt(pos) == ';') {
        pos++;
      }
      final String res = getWord(false, singleKey);
      if (res != null) {
        if (pos == length) {
          return res;
        }
        final char c = s.charAt(pos);
        if (c == ';' || c == ',') {
          return res;
        }
      }
      pos = save;
      return null;
    }


    String getParam() {
      if (pos == length || s.charAt(pos) != ';') {
        return null;
      }
      final int save = pos++;
      final String res = getWord(false, false);
      if (res != null) {
        if (pos < length && s.charAt(pos) == '=') {
          // Untyped parameter
          return res;
        }
        if (pos < length && s.charAt(pos) == ':') {
          // Typed parameter or directive
          return res;
        }
      }
      pos = save;
      return null;
    }


    boolean isDirective() {
      if (pos + 1 < length && s.charAt(pos) == ':' && s.charAt(pos + 1) == '=') {
        pos++;
        return true;
      } else {
        return false;
      }
    }


    String getParamType() {
      if (pos == length || s.charAt(pos) != ':') {
        return null;
      }
      final int save = pos++;
      final String res = getWord(false, false);
      if (res != null) {
        if (pos < length && s.charAt(pos) == '=') {
          return res;
        }
      }
      pos = save;
      return null;
    }


    String getValue() {
      return getValue(false);
    }

    String getValue(boolean keepEscapes) {
      if (s.charAt(pos) != '=') {
        return null;
      }
      final int save = pos++;
      skipWhite();
      final String val = getWord(keepEscapes, true);
      if (val == null) {
        pos = save;
        return null;
      }
      return val;
    }


    boolean getEntryEnd() {
      final int save = pos;
      skipWhite();
      if (pos == length) {
        return true;
      } else if (s.charAt(pos) == ',') {
        pos++;
        return true;
      } else {
        pos = save;
        return false;
      }
    }


    boolean getEnd() {
      final int save = pos;
      skipWhite();
      if (pos == length) {
        return true;
      } else {
        pos = save;
        return false;
      }
    }


    String getRest() {
      final String res = s.substring(pos).trim();
      return res.length() == 0 ? "<END OF LINE>" : res;
    }


    private void skipWhite() {
      for (; pos < length; pos++) {
        if (!Character.isWhitespace(s.charAt(pos))) {
          break;
        }
      }
    }
  }

  /**
   * A class that holds the parse result for one entry of a manifest header
   * following the general OSGi manifest header syntax. See
   * {@link Util#parseManifestHeader()} for
   * details on the syntax.
   */
  public static class HeaderEntry
  {
    final String headerName;
    final boolean singleKey;
    final List<String> keys = new ArrayList<String>();
    final Map<String, Object> attributes = new HashMap<String, Object>();
    final Map<String, String> directives = new HashMap<String, String>();

    /**
     * @param singleKey
     */
    HeaderEntry(String headerName, boolean singleKey)
    {
      this.headerName = headerName;
      this.singleKey = singleKey;
    }

    public String getKey()
    {
      if  (singleKey) {
        return keys.get(0);
      }
      throw new IllegalArgumentException("Requesting single key for multi key header clause");
    }

    public List<String> getKeys()
    {
      return keys;
    }

    public Map<String, Object> getAttributes()
    {
      return attributes;
    }

    public Map<String, String> getDirectives()
    {
      return directives;
    }
  }

}
