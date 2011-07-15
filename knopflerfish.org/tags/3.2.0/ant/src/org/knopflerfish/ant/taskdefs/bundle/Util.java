/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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

package org.knopflerfish.ant.taskdefs.bundle;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;

/**
 * Miscellaneous static utility code.
 */
public class Util {
  public static byte [] loadURL(URL url) throws IOException {
    int     bufSize = 1024 * 2;
    byte [] buf     = new byte[bufSize];

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    BufferedInputStream   in   = new BufferedInputStream(url.openStream());
    int n;
    while ((n = in.read(buf)) > 0) {
      bout.write(buf, 0, n);
    }
    try { in.close(); } catch (Exception ignored) { }
    return bout.toByteArray();
  }

  public static String loadFile(String fname) throws IOException {
    byte[] bytes = loadURL(new URL("file:" + fname));
    return new String(bytes, "UTF-8");
  }


  /**
   * Load entire contents of a file or URL into a string.
   */
  public static String load(String fileOrURL) throws IOException {
    try {
      URL url = new URL(fileOrURL);

      return new String(loadURL(url));
    } catch (Exception e) {
      return loadFile(fileOrURL);
    }
  }

  /**
   * Load an XML-formated file into a DOM-document.
   *
   * @param file The XML file to load.
   * @return DOM document.
   */
  public static Document loadXML(final File file) {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      return db.parse(file);
    } catch (Exception e) {
      throw new BuildException("Failed to parse XML file '" +file +"': " +e, e);
    }
  }

  /**
   * Create an empty DOM-document.
   *
   * @param rootElement The name of the root element of the new document.
   * @return DOM document with a root element.
   */
  public static Document createXML(final String rootElement) {
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      final DocumentBuilder db = dbf.newDocumentBuilder();
      final Document res = db.newDocument();
      res.appendChild(res.createElement(rootElement));
      return res;
    } catch (Exception e) {
      throw new BuildException("Failed to create new DOM-document:" +e, e);
    }
  }

  /**
   * Create a Set from a comma-separated string.
   */
  public static Set makeSetFromStringList(String s) {
    Set set = new HashSet();

    String[] sa = Util.splitwords(s, ",", '"');
    for(int i = 0; i < sa.length; i++) {
      set.add(sa[i]);
    }

    return set;
  }



  public static String getRelativePath(File fromFile,
                                       File toFile) {
    File fromDir = fromFile.isDirectory()
      ? fromFile
      : fromFile.getParentFile();

    File toDir = toFile.isDirectory()
      ? toFile
      : toFile.getParentFile();


    File dir = fromDir;

    String relPath = "";

    while(dir != null && !dir.equals(toDir)) {
      relPath += "../";
      dir = dir.getParentFile();
    }

    if(dir == null) {
      throw new RuntimeException(toFile + " is not in parent of " + fromFile);
    }
    return relPath + toFile.toString();
  }

  // String manipulation functions below by Erik Wistrand


  /**
   * Replace all occurrences of a substring with another string.
   *
   * <p>
   * If no replacements are needed, the methods returns the original string.
   * </p>
   *
   * @param s  Source string which will be scanned and modified.
   *           If <tt>null</tt>, return <tt>null</tt>
   * @param v1 String to be replaced with <tt>v2</tt>.
   *           If <tt>null</tt>, return original string.
   * @param v2 String replacing <tt>v1</tt>.
   *           If <tt>null</tt>, return original string.
   * @return   Modified string.
   */
  public static String replace(final String s,
                               final String v1,
                               final String v2) {

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

    while(-1 != (ix = s.indexOf(v1, ix))) {
      n++;
      ix += v1Len;
    }

    if(n == 0) {
      return s;
    }

    int     start  = 0;
    int     v2Len  = v2.length();
    char[]  r      = new char[s.length() + n * (v2Len - v1Len)];
    int     rPos   = 0;

    while(-1 != (ix = s.indexOf(v1, start))) {
      while(start < ix) r[rPos++] = s.charAt(start++);
      for(int j = 0; j < v2Len; j++) {
        r[rPos++] = v2.charAt(j);
      }
      start += v1Len;
    }

    ix = s.length();
    while(start < ix) r[rPos++] = s.charAt(start++);

    return new String(r);
  }

  /**
   * Split a string into words separated by whitespace
   * SPACE | TAB | NEWLINE | CR

   * Citation chars '"' may be used to group words
   * with embedded whitespace.
   * </p>
   */
  public static String [] splitwords(String s) {
    return splitwords(s, " \t\n\r", '"');
  }

  /**
   * Split a string into words separated by whitespace.
   * Citation chars '"' may be used to group words with embedded
   * whitespace.
   *
   * @param s          String to split.
   * @param whiteSpace whitespace to use for splitting. Any of the
   *                   characters in the whiteSpace string are considered
   *                   whitespace between words and will be removed
   *                   from the result.
   * @param citchar    citation char used for enclosing words containing
   *                   whitespace
   */
  public static String [] splitwords(String s,
                                     String whiteSpace,
                                     char   citchar) {
    boolean       bCit  = false;        // true when inside citation chars.
    Vector        v     = new Vector(); // (String) individual words after splitting
    StringBuffer  buf   = null;
    int           i     = 0;

    while(i < s.length()) {
      char c = s.charAt(i);

      if(bCit || whiteSpace.indexOf(c) == -1) {
        // Build up word until we breaks on
        // either a citation char or whitespace
        if(c == citchar) {
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


  // Always write files using UTF-8.
  static void writeStringToFile(File outFile, String s) throws IOException {
    OutputStreamWriter writer = null;
    try {
      outFile.getParentFile().mkdirs();
      final OutputStream out = new FileOutputStream(outFile);
      writer = new OutputStreamWriter(out, "UTF-8");
      writer.write(s, 0, s.length());
      //      System.out.println("wrote " + outFile);
    } finally {
      try { writer.close(); } catch (Exception ignored) { }
    }
  }

  public static void writeDocumentToFile(final File outFile, final Document doc) {
    final Source source = new DOMSource(doc);
    final Result result = new StreamResult(outFile);

    // Write the DOM document to the file
    Transformer xformer;
    try {
      xformer = TransformerFactory.newInstance().newTransformer();
      xformer.transform(source, result);
    } catch (Exception e) {
      throw new BuildException("Failed to write XML to '" + outFile +"', "+e, e);
    }
  }


  /**
   * Parse strings of format:
   *
   *   ENTRY (, ENTRY)*
   *   ENTRY = key (; key)* (; PARAM)*
   *   PARAM = attribute '=' value
   *   PARAM = directive ':=' value
   *
   * @param a Name of attribute being parsed (for error messages).
   * @param s The attribute value to parse.
   * @param single If true, only allow one key per ENTRY.
   * @param unique Only allow unique parameters for each ENTRY.
   * @param single_entry If true, only allow one ENTRY is allowed.
   * @return Iterator(Map(param -> value)) or null if input string is null.
   * @exception IllegalArgumentException If syntax error in input string.
   */
  public static Iterator parseEntries(String a, String s, boolean single, boolean unique, boolean single_entry) {
    ArrayList result = new ArrayList();
    if (s != null) {
      AttributeTokenizer at = new AttributeTokenizer(s);
      do {
        ArrayList keys = new ArrayList();
        HashMap params = new HashMap();
        Set directives = new TreeSet();
        params.put("$directives", directives); // $ is not allowed in
                                               // param names...
        String key = at.getKey();
        if (key == null) {
          throw new IllegalArgumentException
            ("Definition, " + a + ", expected key at: " + at.getRest()
             +". Key values are terminated by a ';' or a ',' and may not "
             +"contain ':', '='.");
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
          boolean is_directive = at.isDirective();
          if (old != null && unique) {
            throw new IllegalArgumentException("Definition, " + a + ", duplicate " +
                                               (is_directive ? "directive" : "attribute") +
                                               ": " + param);
          }
          String value = at.getValue();
          if (value == null) {
            throw new IllegalArgumentException("Definition, " + a + ", expected value at: " + at.getRest());
          }
          if (is_directive) {
            // NYI Handle directives and check them
            // This method has become very ugly, please rewrite.
            directives.add(param);
          }
          if (unique) {
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
            params.put("$key", key);
          } else {
            params.put("$keys", keys);
          }
          result.add(params);
        } else {
          throw new IllegalArgumentException("Definition, " + a + ", expected end of entry at: " + at.getRest());
        }
        if (single_entry && !at.getEnd()) {
          throw new IllegalArgumentException("Definition, " + a + ", expected end of single entry at: " + at.getRest());
        }
      } while (!at.getEnd());
    }
    return result.iterator();
  }

}


/**
 * Class makes tokens of an attribute string.
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
        case ',': case ':': case ';': case '=':
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
    if (res != null) {
      if (pos < length && s.charAt(pos) == '=') {
        return res;
      } if (pos + 1 < length && s.charAt(pos) == ':' && s.charAt(pos+1) == '=') {
        return res;
      }
    }
    pos = save;
    return null;
  }

  boolean isDirective() {
    if (pos + 1 < length && s.charAt(pos) == ':') {
      pos++;
      return true;
    } else {
      return false;
    }
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
