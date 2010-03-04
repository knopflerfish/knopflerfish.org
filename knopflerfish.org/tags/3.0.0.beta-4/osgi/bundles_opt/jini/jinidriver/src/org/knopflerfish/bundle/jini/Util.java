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
package org.knopflerfish.bundle.jini;

import net.jini.core.lookup.ServiceID;

import java.util.Vector;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
public class Util {
    protected static String WHITESPACE = " \t\n\r";
    protected static char CITCHAR = '"';

    /**
     * DOCUMENT ME!
     *
     * @param serviceIDString DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static ServiceID getServiceID(String serviceIDString) {
        if (serviceIDString == null) {
            return null;
        }

        java.util.StringTokenizer t = new java.util.StringTokenizer(serviceIDString,
                "-");

        if (t.countTokens() != 5) {
            return null;
        }

        String time_low = t.nextToken();

        if (time_low.length() != 8) {
            return null;
        }

        String time_mid = t.nextToken();

        if (time_mid.length() != 4) {
            return null;
        }

        String version_and_time_hi = t.nextToken();

        if (version_and_time_hi.length() != 4) {
            return null;
        }

        String variant_and_clock_seq = t.nextToken();

        if (variant_and_clock_seq.length() != 4) {
            return null;
        }

        String node = t.nextToken();

        if (node.length() != 12) {
            return null;
        }

        try {
            long m = Long.parseLong(time_low + time_mid + version_and_time_hi,
                    16);
            long l = Long.parseLong(variant_and_clock_seq + node, 16);

            return new ServiceID(m, l);
        } catch (NumberFormatException e) {
        }

        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param s DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static String[] splitwords(String s) {
        return splitwords(s, WHITESPACE);
    }

    /**
     * DOCUMENT ME!
     *
     * @param s DOCUMENT ME!
     * @param whiteSpace DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static String[] splitwords(String s, String whiteSpace) {
        return splitwords(s, WHITESPACE, CITCHAR);
    }

    /**
     * DOCUMENT ME!
     *
     * @param s DOCUMENT ME!
     * @param whiteSpace DOCUMENT ME!
     * @param citChar DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static String[] splitwords(String s, String whiteSpace, char citChar) {
        boolean bCit = false; // true when inside citation chars.
        Vector v = new Vector(); // (String) individual words after splitting
        StringBuffer buf = null;
        int i = 0;

        while (i < s.length()) {
            char c = s.charAt(i);

            if (bCit || (whiteSpace.indexOf(c) == -1)) {
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
                while ((i < s.length()) &&
                        (-1 != whiteSpace.indexOf(s.charAt(i)))) {
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
}
