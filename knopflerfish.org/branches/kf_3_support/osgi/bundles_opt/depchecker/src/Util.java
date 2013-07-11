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

/*
 * Edited by Goeminne Nico :
 * Selected the methodes needed for parsing Manifest Headers
 * removed the other methods.
 */

import java.io.*;
import java.util.*;

public class Util
{

  /**
   * Compare to strings formatted as '<int>[.<int>[.<int>]]'.
   * If string is null, then it counts as ZERO.
   *
   * @param ver1 First version string.
   * @param ver2 Second version string.
   * @return Return 0 if equals, -1 if ver1 < ver2 and 1 if ver1 > ver2.
   * @exception NumberFormatException on syntax error in input.
   */
  public static int compareStringVersion(String ver1, String ver2)
    throws NumberFormatException {
    int i1, i2;
    while (ver1 != null || ver2 != null) {
      if (ver1 != null) {
        int d1 = ver1.indexOf(".");
        if (d1 == -1) {
          i1 = Integer.parseInt(ver1.trim());
          ver1 = null;
        } else {
          i1 = Integer.parseInt(ver1.substring(0, d1).trim());
          ver1 = ver1.substring(d1 + 1);
        }
      } else {
        i1 = 0;
      }
      if (ver2 != null) {
        int d2 = ver2.indexOf(".");
        if (d2 == -1) {
          i2 = Integer.parseInt(ver2.trim());
          ver2 = null;
        } else {
          i2 = Integer.parseInt(ver2.substring(0, d2).trim());
          ver2 = ver2.substring(d2 + 1);
        }
      } else {
        i2 = 0;
      }
      if (i1 < i2) {
        return -1;
      }
      if (i1 > i2) {
        return 1;
      }
    }
    return 0;
  }

  /**
   * Parse strings of format:
   *
   *   ENTRY (, ENTRY)*
   *   ENTRY = key (; key)* (; OP)*
   *   OP = param '=' value
   *
   * @param a Attribute being parsed
   * @param s String to parse
   * @param single If true, only allow one key per ENTRY
   *        and only allow unique parmeters for each ENTRY.
   * @return Iterator(Map(param -> value)).
   * @exception IllegalArgumentException If syntax error in input string.
   */
  public static Iterator parseEntries(String a, String s, boolean single) {
    ArrayList result = new ArrayList();
    if (s != null) {
      AttributeTokenizer at = new AttributeTokenizer(s);
      do {
        ArrayList keys = new ArrayList();
        HashMap params = new HashMap();
        boolean doingKeys = true;
        String key = at.getKey();
        if (key == null) {
          throw new IllegalArgumentException("Attribute, " + a + ", expected key at: " + at.getRest());
        }
        if (!single) {
          keys.add(key);
          while ((key = at.getKey()) != null) {
            keys.add(key);
          }
        }
        String param;
        while ((param = at.getParam()) != null) {
          List old = (List)params.get(param);
          if (old != null && single) {
            throw new IllegalArgumentException("Attribute, " + a + ", duplicate parameter: " + param);
          }
          String value = at.getValue();
          if (value == null) {
            throw new IllegalArgumentException("Attribute, " + a + ", expected value at: " + at.getRest());
          }
          if (single) {
            params.put(param, value);
          } else {
            if (old == null) {
              old = new ArrayList();
              params.put(param, old);
            }
            old.add(value);
          }
        }
        if (at.getEntryEnd()) {
          if (single) {
            params.put("key", key);
          } else {
            params.put("keys", keys);
          }
          result.add(params);
        } else {
          throw new IllegalArgumentException("Attribute, " + a + ", expected end of entry at: " + at.getRest());
        }
      } while (!at.getEnd());
    }
    return result.iterator();
  }


  /**
   * Default whitespace string for splitwords().
   * Value is <tt>" \t\n\r"</tt>)
   */
  protected static String  WHITESPACE = " \t\n\r";

  /**
   * Default citation char for splitwords().
   * Value is <tt>'"'</tt>
   */
  protected static char   CITCHAR    = '"';


  /**
   * Utility method to split a string into words separated by whitespace.
   *
   * <p>
   * Equivalent to <tt>splitwords(s, WHITESPACE)</tt>
   * </p>
   */
  public static String [] splitwords(String s) {
    return splitwords(s, WHITESPACE);
  }

  /**
   * Utility method to split a string into words separated by whitespace.
   *
   * <p>
   * Equivalent to <tt>splitwords(s, WHITESPACE, CITCHAR)</tt>
   * </p>
   */
  public static String [] splitwords(String s, String whiteSpace) {
    return splitwords(s, WHITESPACE, CITCHAR);
  }


  /**
   * Split a string into words separated by whitespace.
   * <p>
   * Citation chars may be used to group words with embedded
   * whitespace.
   * </p>
   *
   * @param s          String to split.
   * @param whiteSpace whitespace to use for splitting. Any of the
   *                   characters in the whiteSpace string are considered
   *                   whitespace between words and will be removed
   *                   from the result. If no words are found, return an
   *                   array of length zero.
   * @param citChar    Citation character used for grouping words with
   *                   embedded whitespace. Typically '"'
   */
  public static String [] splitwords(String s,
                                     String whiteSpace,
                                     char   citChar) {
    boolean       bCit  = false;        // true when inside citation chars.
    Vector        v     = new Vector(); // (String) individual words after splitting
    StringBuffer  buf   = null;
    int           i     = 0;

    while(i < s.length()) {
      char c = s.charAt(i);

      if(bCit || whiteSpace.indexOf(c) == -1) {
        // Build up word until we breaks on either a citation char or whitespace
        if(c == citChar) {
          bCit = !bCit;
        } else {
          if(buf == null) {
            buf = new StringBuffer();
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


/**
 * Class for tokenize an attribute string.
 */
class AttributeTokenizer {

  String s;
  int length;
  int pos = 0;

  AttributeTokenizer(String input) {
    s = input;
    length = s.length();
  }

  String getWord() {
    skipWhite();
    boolean backslash = false;
    boolean quote = false;
    StringBuffer val = new StringBuffer();
    int end = 0;
  loop:
    for (; pos < length; pos++) {
      if (backslash) {
        backslash = false;
        val.append(s.charAt(pos));
      } else {
        char c = s.charAt(pos);
        switch (c) {
        case '"':
          quote = !quote;
          end = val.length();
          break;
        case '\\':
          backslash = true;
          break;
        case ',': case ';': case '=':
          if (!quote) {
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
    char [] res = new char [end];
    val.getChars(0, end, res, 0);
    return new String(res);
  }

  String getKey() {
    if (pos >= length) {
      return null;
    }
    int save = pos;
    if (s.charAt(pos) == ';') {
      pos++;
    }
    String res = getWord();
    if (res != null) {
      if (pos == length) {
        return res;
      }
      char c = s.charAt(pos);
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
    int save = pos++;
    String res = getWord();
    if (res != null && pos < length && s.charAt(pos) == '=') {
      return res;
    }
    pos = save;
    return null;
  }

  String getValue() {
    if (s.charAt(pos) != '=') {
      return null;
    }
    int save = pos++;
    skipWhite();
    String val = getWord();
    if (val == null) {
      pos = save;
      return null;
    }
    return val;
  }

  boolean getEntryEnd() {
    int save = pos;
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
    int save = pos;
    skipWhite();
    if (pos == length) {
      return true;
    } else {
      pos = save;
      return false;
    }
  }

  String getRest() {
    String res = s.substring(pos).trim();
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
