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

package org.knopflerfish.service.console;

import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Utility methods for building CommandGroups to the console. This is a set of
 * methods that does some useful things for displaying and sorting framework
 * information.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */

public class Util {

    /**
     * Select bundles from an array of Bundle objects. All the bundles in the
     * bundle array is tested against all the strings in the selection array. If
     * none of the strings match the element in the bundle array is set to NULL.
     * A selection string matches if: It matches the bundle ID. It matches the
     * location of the bundle. It matches the short name of the bundle. It is
     * also possible to start or end the selection string with the wildcard
     * character '*' (Does not work for bundle id).
     * 
     * @param bundles
     *            array of bundles to be checked, modified with result
     * @param selection
     *            array of selection string to match against bundles
     */
    public static void selectBundles(Bundle[] bundles, String[] selection) {
        if (selection != null) {
            for (int i = 0; i < bundles.length; i++) {
                int j = 0;
                String bn = shortName(bundles[i]);
                String l = bundles[i].getLocation();
                for (; j < selection.length; j++) {
                    try {
                        long id = Long.parseLong(selection[j]);
                        if (bundles[i].getBundleId() == id) {
                            break;
                        }
                    } catch (NumberFormatException e) {
                    }
                    if (bn.equals(selection[j]) || l.equals(selection[j])) {
                        break;
                    }
                    if (selection[j].startsWith("*")) {
                        String s = selection[j].substring(1);
                        if (bn.endsWith(s) || l.endsWith(s)) {
                            break;
                        }
                    } else if (selection[j].endsWith("*")) {
                        String s = selection[j].substring(0, selection[j]
                                .length() - 1);
                        if (bn.startsWith(s)) {
                            break;
                        }
                        if (s.indexOf(':') != -1 && l.startsWith(s)) {
                            break;
                        }
                    } else {
                        if (l.endsWith("/" + selection[j] + ".jar")
                                || l.endsWith("\\" + selection[j] + ".jar")) {
                            break;
                        }
                    }
                }
                if (j == selection.length) {
                    bundles[i] = null;
                }
            }
        }
    }

    /**
     * Sort an array of bundle objects based on their location or shortname. All
     * entries containing NULL is placed at the end of the array.
     * 
     * @param b
     *            array of bundles to be sorted, modified with result
     * @param longName
     *            if true sort on location otherwise on shortname
     */
    public static void sortBundles(Bundle[] b, boolean longName) {
        int x = b.length;

        for (int l = x; x > 0;) {
            x = 0;
            String p = null;
            if (b[0] != null)
                p = longName ? b[0].getLocation() : shortName(b[0]);
            for (int i = 1; i < l; i++) {
                String n = null;
                if (b[i] != null)
                    n = longName ? b[i].getLocation() : shortName(b[i]);
                if (n != null && (p == null || p.compareTo(n) > 0)) {
                    x = i - 1;
                    Bundle t = b[x];
                    b[x] = b[i];
                    b[i] = t;
                } else {
                    p = n;
                }
            }
        }
    }

    /**
     * Sort an array of bundle objects based on their Bundle Id. All entries
     * containing NULL is placed at the end of the array.
     * 
     * @param b
     *            array of bundles to be sorted, modified with result
     */
    public static void sortBundlesId(Bundle[] b) {
        int x = b.length;

        for (int l = x; x > 0;) {
            x = 0;
            long p = b[0] != null ? b[0].getBundleId() : Long.MAX_VALUE;
            for (int i = 1; i < l; i++) {
                long n = b[i] != null ? b[i].getBundleId() : Long.MAX_VALUE;
                if (p > n) {
                    x = i - 1;
                    Bundle t = b[x];
                    b[x] = b[i];
                    b[i] = t;
                } else {
                    p = n;
                }
            }
        }
    }

    /**
     * Get short name of specified bundle. First, try to get the BUNDLE-NAME
     * header. If it fails use the location of the bundle with all characters
     * upto and including the last '/' or '\' and any trailing ".jar" stripped
     * off.
     * 
     * @param bundle
     *            the bundle
     * @return The bundles shortname or null if input was null
     */
    public static String shortName(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        String n = (String) bundle.getHeaders().get("Bundle-Name");
        if (n == null) {
            n = bundle.getLocation();
            int x = n.lastIndexOf('/');
            int y = n.lastIndexOf('\\');
            if (y > x) {
                x = y;
            }
            if (x != -1) {
                n = n.substring(x + 1);
            }
            if (n.endsWith(".jar")) {
                n = n.substring(0, n.length() - 4);
            }
        }
        return n;
    }

    /**
     * Get bundle state as a constant length string. Show state left adjusted as
     * 12 character string.
     * 
     * @param bundle
     *            the bundle
     * @return The bundles state
     */
    public static String showState(Bundle bundle) {
        switch (bundle.getState()) {
        case Bundle.INSTALLED:
            return "installed   ";
        case Bundle.RESOLVED:
            return "resolved    ";
        case Bundle.STARTING:
            return "starting    ";
        case Bundle.ACTIVE:
            return "active      ";
        case Bundle.STOPPING:
            return "stopping    ";
        case Bundle.UNINSTALLED:
            return "uninstalled ";
        default:
            return "ILLEGAL <" + bundle.getState() + "> ";
        }
    }

    /**
     * Get bundle identifier as a constant length string. As long as the id is 5
     * digits or less it will return a string with length 6.
     * 
     * @param bundle
     *            the bundle
     * @return The bundles identifier
     */
    public static String showId(Bundle bundle) {
        return showRight(5, String.valueOf(bundle.getBundleId())) + " ";
    }

    /**
     * Get Service class list as a string.
     * 
     * @param sr
     *            The service
     * @return The bundles identifier
     */
    public static String showServiceClasses(ServiceReference sr) {
        StringBuffer sb = new StringBuffer();
        String[] c = (String[]) sr.getProperty("objectClass");
        if (c.length >= 2) {
            sb.append("[");
        }
        for (int i = 0; i < c.length; i++) {
            if (i > 0)
                sb.append(",");
            int x = c[i].lastIndexOf('.');
            sb.append(x != -1 ? c[i].substring(x + 1) : c[i]);
        }
        if (c.length >= 2) {
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Get string representation of an object. If it is an array or Enumeration,
     * do toString on each element. All other objects are shown with toString.
     * 
     * @param o
     *            the object to show
     * @return A string showing the object
     */
    public static String showObject(Object o) {
        if (o == null) {
            return "null";
        }
        if (o.getClass().isArray()) {
            StringBuffer sb = new StringBuffer();
            int len = Array.getLength(o);
            sb.append("[");
            if (len > 0) {
                sb.append(showObject(Array.get(o, 0)));
            }
            for (int i = 1; i < len; i++) {
                sb.append(", " + showObject(Array.get(o, i)));
            }
            sb.append("]");
            o = sb;
        } else if (o instanceof Vector) {
            StringBuffer sb = new StringBuffer();
            Enumeration e = ((Vector) o).elements();
            sb.append("[");
            if (e.hasMoreElements()) {
                sb.append(showObject(e.nextElement()));
            }
            while (e.hasMoreElements()) {
                sb.append(", " + showObject(e.nextElement()));
            }
            sb.append("]");
            o = sb;
        }
        return o.toString();
    }

    private final static String BLANKS = "                                                                  ";

    /**
     * Show a string left adjusted in constant length string. If the string is
     * shorter then the desired length the string is padded with blanks. If it
     * is longer it is cut.
     * 
     * @param str
     *            the object to show
     * @param width
     *            the desired width of the result
     * @return A constant width string
     */
    public static String showLeft(int width, String str) {
        if (str.length() < width) {
            return str + BLANKS.substring(0, width - str.length());
        }
        return str.substring(0, width);
    }

    /**
     * Show a string right adjusted in constant length string. If the string is
     * shorter then the desired length the string is padded with blanks. If it
     * is longer it is cut.
     * 
     * @param str
     *            the object to show
     * @param width
     *            the desired width of the result
     * @return A constant width string
     */
    public static String showRight(int width, String str) {
        if (str.length() < width) {
            return BLANKS.substring(0, width - str.length()) + str;
        }
        return str.substring(0, width);
    }

}
