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

package org.knopflerfish.util;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Misc static text utility methods.
 */
public class Text {

    /**
     * Retrieves a parameter value from a parameter string. If the parameter is
     * not found the default value is returned.
     * 
     * @param s
     *            Parameter string, format
     *            &apos;&lt;param1&gt;=data1::&lt;param2&gt;=data2&apos;
     * @param param
     *            Parameter to retrieve.
     * @param def
     *            Default value to return, if the parameter is not found.
     * @return parameter value or default value.
     */
    public static String getParam(String s, String param, String def) {
        if (s == null || param == null)
            return def;
        int ix = s.indexOf(param + "=");
        if (ix < 0)
            return def;

        int startIdx = ix + param.length() + 1;
        int endIdx = s.indexOf("::", startIdx);
        if (endIdx < 0)
            endIdx = s.length();

        try {
            return s.substring(startIdx, endIdx);
        } catch (Exception e) {
            return def;
        }
    }

    // String replace functions below by Erik Wistrand

    /**
     * Replace all occurances of a substring with another string.
     * 
     * <p>
     * The returned string will shrink or grow as necessary, depending on the
     * lengths of <tt>v1</tt> and <tt>v2</tt>.
     * </p>
     * 
     * <p>
     * Implementation note: This method avoids using the standard String
     * manipulation methods to increase execution speed. Using the
     * <tt>replace</tt> method does however include two <tt>new</tt>
     * operations in the case when matches are found.
     * </p>
     * 
     * 
     * @param s
     *            Source string.
     * @param v1
     *            String to be replaced with <code>v2</code>.
     * @param v2
     *            String replacing <code>v1</code>.
     * @return Modified string. If any of the input strings are <tt>null</tt>,
     *         the source string <tt>s</tt> will be returned unmodified. If
     *         <tt>v1.length == 0</tt>, <tt>v1.equals(v2)</tt> or no
     *         occurances of <tt>v1</tt> is found, also return <tt>s</tt>
     *         unmodified.
     */
    public static String replace(final String s, final String v1,
            final String v2) {

        // return quick when nothing to do
        if (s == null || v1 == null || v2 == null || v1.length() == 0
                || v1.equals(v2)) {
            return s;
        }

        int ix = 0;
        int v1Len = v1.length();
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
        int v2Len = v2.length();
        char[] r = new char[s.length() + n * (v2Len - v1Len)];
        int rPos = 0;

        // for each occurance, copy v2 where v1 used to be
        while (-1 != (ix = s.indexOf(v1, start))) {
            while (start < ix)
                r[rPos++] = s.charAt(start++);
            for (int j = 0; j < v2Len; j++) {
                r[rPos++] = v2.charAt(j);
            }
            start += v1Len;
        }

        // ...and add all remaining chars
        ix = s.length();
        while (start < ix)
            r[rPos++] = s.charAt(start++);

        // ..ouch. this hurts.
        return new String(r);
    }

    /**
     * Utility method for replacing substrings with an integer.
     * 
     * <p>
     * Equivalent to <tt>replace(s, v1, Integer.toString(v2))</tt>
     * </p>
     */
    public static String replace(String s, String v1, int v2) {
        return replace(s, v1, Integer.toString(v2));
    }

    /**
     * Utility method for replacing substrings with a boolean.
     * 
     * <p>
     * Equivalent to <tt>replace(s, v1, v2 ? "true" : "false")</tt>
     * </p>
     */
    public static String replace(String s, String v1, boolean v2) {
        return replace(s, v1, v2 ? "true" : "false");
    }

    /**
     * Expand all tabs in a string. Tab stop positions are placed at the columns
     * which are multiples of <code>tabSize</code>.
     * 
     * @param s
     *            String to untabify.
     * @param tabSize
     *            Tab stop interval.
     * @return String with expanded tabs.
     */
    public static String untabify(String s, int tabSize) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\t') {
                String toinsert = "        ".substring(0, tabSize
                        - (i % tabSize));
                if (toinsert.length() == 0) {
                    sb = new StringBuffer(sb.toString().substring(0, i)
                            + sb.toString().substring(i + 1));
                } else {
                    sb.setCharAt(i, ' ');
                    if (toinsert.length() > 1) {
                        sb.insert(i, toinsert.substring(1));
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Format a java type in human readable way.
     * 
     * @param s
     *            Type string to format.
     * @param prefixIgnore
     *            Prefix strings to ignore in output.
     * @return Nice-to-read string.
     */
    public static String formatJavaType(String s, String[] prefixIgnore) {
        for (int i = 0; i < prefixIgnore.length; i++) {
            if (s.startsWith(prefixIgnore[i])) {
                return s.substring(prefixIgnore[i].length());
            }
        }

        // Handle array types
        if (s.startsWith("[L") && s.endsWith(";")) {
            return formatJavaType(s.substring(2, s.length() - 1), prefixIgnore)
                    + "[]";
        }
        return s;
    }

    /**
     * Make first (and only) character in string upper case.
     * 
     * @param s
     *            String to capitalize.
     * @return Capitalized string. If <tt>s</tt> is <tt>null</tt> or equals
     *         the empty string, return <tt>s</tt>.
     */
    public static String capitalize(String s) {
        return (s.equals("") || s == null) ? s : s.substring(0, 1)
                .toUpperCase()
                + s.substring(1).toLowerCase();
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
     * Equivalent to <tt>splitwords(s, Text.WHITESPACE)</tt>
     * </p>
     */
    public static String[] splitwords(String s) {
        return splitwords(s, Text.WHITESPACE);
    }

    /**
     * Utility method to split a string into words separated by whitespace.
     * 
     * <p>
     * Equivalent to <tt>splitwords(s, Text.WHITESPACE, Text.CITCHAR)</tt>
     * </p>
     */
    public static String[] splitwords(String s, String whiteSpace) {
        return splitwords(s, Text.WHITESPACE, Text.CITCHAR);
    }

    /**
     * Split a string into words separated by whitespace.
     * <p>
     * Citation chars may be used to group words with embedded whitespace.
     * </p>
     * 
     * @param s
     *            String to split.
     * @param whiteSpace
     *            whitespace to use for splitting. Any of the characters in the
     *            whiteSpace string are considered whitespace between words and
     *            will be removed from the result. If no words are found, return
     *            an array of length zero.
     * @param citChar
     *            Citation character used for grouping words with embedded
     *            whitespace. Typically '"'
     */
    public static String[] splitwords(String s, String whiteSpace, char citChar) {
        boolean bCit = false; // true when inside citation chars.
        Vector v = new Vector(); // (String) individual words after splitting
        StringBuffer buf = null;
        int i = 0;

        while (i < s.length()) {
            char c = s.charAt(i);

            if (bCit || whiteSpace.indexOf(c) == -1) {
                // Build up word until we breaks on either a citation char or
                // whitespace
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
                // found whitespace or end of citation, append word if we have
                // one
                if (buf != null) {
                    v.addElement(buf.toString());
                    buf = null;
                }

                // and skip whitespace so we start clean on a word or citation
                // char
                while ((i < s.length())
                        && (-1 != whiteSpace.indexOf(s.charAt(i)))) {
                    i++;
                }
            }
        }

        // Add possible remaining word
        if (buf != null) {
            v.addElement(buf.toString());
        }

        // Copy back into an array
        String[] r = new String[v.size()];
        v.copyInto(r);

        return r;
    }

    /**
     * Splits a string into words, using the <code>StringTokenizer</code>
     * class.
     */
    public static String[] split(String s, String sep) {
        StringTokenizer st = new StringTokenizer(s, sep);
        int ntok = st.countTokens();
        String[] res = new String[ntok];
        for (int i = 0; i < ntok; i++) {
            res[i] = st.nextToken();
        }
        return res;
    }

    /**
     * Join an array into a single string with a given separator.
     */
    public static String join(Object[] s, String sep) {
        StringBuffer buf = new StringBuffer();
        int l = s.length;
        if (l > 0) {
            buf.append(s[0].toString());
        }

        for (int i = 1; i < l; i++) {
            buf.append(sep);
            buf.append(s[i].toString());
        }
        return buf.toString();
    }

    public static Object[] toArray(Vector v) {
        int size = v.size();
        Object[] o = new Object[size];

        // Why not v.copyInto(o) ??

        for (int i = 0; i < size; i++) {
            o[i] = v.elementAt(i);
        }
        return o;
    }

    /*
     * // Testing purposes static public void main(String[] argv) { String[] s =
     * splitwords(argv[0], " ,"); for(int i = 0; i < s.length; i++) {
     * System.out.println(s[i]); }
     * 
     * int n = 100000; { Hashtable h = new Hashtable(); long start =
     * System.currentTimeMillis(); for(int i = 0; i < n; i++) { String ss =
     * replace(argv[0], argv[1], argv[2]); h.put(ss, ss); } long stop =
     * System.currentTimeMillis(); double delta = ((double)stop - start) / 1000;
     * 
     * System.out.println("total = " + delta + ", replace/sec=" + (n / delta)); } }
     */

}
