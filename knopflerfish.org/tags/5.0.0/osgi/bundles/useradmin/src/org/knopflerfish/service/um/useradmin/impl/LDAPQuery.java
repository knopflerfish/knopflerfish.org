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

package org.knopflerfish.service.um.useradmin.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

import org.osgi.framework.InvalidSyntaxException;

/**
 * LDAP filter functions.
 * 
 * @author Tommy Bohlin
 * @author Jan Stein
 * @version $Revision: 1.1.1.1 $
 */
public class LDAPQuery {
    private static final char WILDCARD = 65535;

    private static final String WILDCARD_STRING = new String(
            new char[] { WILDCARD });

    private static final String NULL = "Null query";

    private static final String GARBAGE = "Trailing garbage";

    private static final String MALFORMED = "Malformed query";

    private static final String EMPTY = "Empty list";

    private static final String SUBEXPR = "No subexpression";

    private static final String OPERATOR = "Undefined operator";

    // private static final String TRUNCATED="Truncated expression";
    // private static final String EQUALITY ="Only equality supported";

    private static final int EQ = 0;

    private static final int LE = 1;

    private static final int GE = 2;

    private static final int APPROX = 3;

    // private static final Class[] classArg=new Class[] { String.class };
    // private static final Class[] objectArg=new Class[] { Object.class };

    private static Class classBigDecimal;

    private static Constructor consBigDecimal;

    private static Method compBigDecimal;

    static {
        try {
            classBigDecimal = Class.forName("java.math.BigDecimal");
            consBigDecimal = classBigDecimal
                    .getConstructor(new Class[] { String.class });
            compBigDecimal = classBigDecimal.getMethod("compareTo",
                    new Class[] { classBigDecimal });
        } catch (Exception ignore) {
            classBigDecimal = null;
        }
    }

    boolean val;

    String tail;

    Dictionary prop;

    public static void check(String q) throws InvalidSyntaxException {
        query(q, null);
    }

    public static boolean query(String q, Dictionary p)
            throws InvalidSyntaxException {
        LDAPQuery lq = new LDAPQuery(q, p);
        lq.doQuery();
        if (lq.tail.length() > 0)
            lq.error(GARBAGE);
        return lq.val;
    }

    LDAPQuery(String q, Dictionary p) throws InvalidSyntaxException {
        if (q == null || q.length() == 0)
            error(NULL);
        tail = q;
        prop = p;
    }

    void doQuery() throws InvalidSyntaxException {
        if (tail.length() < 3 || !prefix("("))
            error(MALFORMED);

        switch (tail.charAt(0)) {
        case '&':
            doAnd();
            break;
        case '|':
            doOr();
            break;
        case '!':
            doNot();
            break;
        default:
            doSimple();
            break;
        }

        if (!prefix(")"))
            error(MALFORMED);
    }

    private void doAnd() throws InvalidSyntaxException {
        tail = tail.substring(1);
        boolean val1 = true;
        if (!tail.startsWith("("))
            error(EMPTY);
        do {
            doQuery();
            if (!val)
                val1 = false;
        } while (tail.startsWith("("));
        val = val1;
    }

    private void doOr() throws InvalidSyntaxException {
        tail = tail.substring(1);
        boolean val1 = false;
        if (!tail.startsWith("("))
            error(EMPTY);
        do {
            doQuery();
            if (val)
                val1 = true;
        } while (tail.startsWith("("));
        val = val1;
    }

    private void doNot() throws InvalidSyntaxException {
        tail = tail.substring(1);
        if (!tail.startsWith("("))
            error(SUBEXPR);
        doQuery();
        val = !val;
    }

    private void doSimple() throws InvalidSyntaxException {
        int op = 0;
        Object attr = getAttr();

        if (prefix("="))
            op = EQ;
        else if (prefix("<="))
            op = LE;
        else if (prefix(">="))
            op = GE;
        else if (prefix("~="))
            op = APPROX;
        else
            error(OPERATOR);

        val = compare(attr, op, getValue());
    }

    private boolean prefix(String pre) {
        if (!tail.startsWith(pre))
            return false;
        tail = tail.substring(pre.length());
        return true;
    }

    private Object getAttr() {
        int len = tail.length();
        int ix = 0;
        label: for (; ix < len; ix++) {
            switch (tail.charAt(ix)) {
            case '(':
            case ')':
            case '<':
            case '>':
            case '=':
            case '~':
            case '*':
            case '\\':
                break label;
            }
        }
        String attr = tail.substring(0, ix);
        tail = tail.substring(ix);
        return prop != null ? prop.get(attr) : null;
    }

    private String getValue() {
        StringBuffer sb = new StringBuffer();
        int len = tail.length();
        int ix = 0;
        label: for (; ix < len; ix++) {
            char c = tail.charAt(ix);
            switch (c) {
            case '(':
            case ')':
                break label;
            case '*':
                sb.append(WILDCARD);
                break;
            case '\\':
                if (ix == len - 1)
                    break label;
                sb.append(tail.charAt(++ix));
                break;
            default:
                sb.append(c);
                break;
            }
        }
        tail = tail.substring(ix);
        return sb.toString();
    }

    private void error(String m) throws InvalidSyntaxException {
        throw new InvalidSyntaxException(m, tail);
    }

    private boolean compare(Object obj, int op, String s) {
        if (obj == null)
            return false;
        if (op == EQ && s.equals(WILDCARD_STRING))
            return true;
        try {
            if (obj instanceof String) {
                return compareString((String) obj, op, s);
            } else if (obj instanceof Character) {
                return compareString(obj.toString(), op, s);
            } else if (obj instanceof Boolean) {
                if (op == LE || op == GE)
                    return false;
                return ((Boolean) obj).equals(new Boolean(s));
            } else if (obj instanceof Number) {
                if (obj instanceof Byte) {
                    switch (op) {
                    case LE:
                        return ((Byte) obj).byteValue() <= Byte.parseByte(s);
                    case GE:
                        return ((Byte) obj).byteValue() >= Byte.parseByte(s);
                    default: /* APPROX and EQ */
                        return (new Byte(s)).equals(obj);
                    }
                } else if (obj instanceof Integer) {
                    switch (op) {
                    case LE:
                        return ((Integer) obj).intValue() <= Integer
                                .parseInt(s);
                    case GE:
                        return ((Integer) obj).intValue() >= Integer
                                .parseInt(s);
                    default: /* APPROX and EQ */
                        return (new Integer(s)).equals(obj);
                    }
                } else if (obj instanceof Short) {
                    switch (op) {
                    case LE:
                        return ((Short) obj).shortValue() <= Short
                                .parseShort(s);
                    case GE:
                        return ((Short) obj).shortValue() >= Short
                                .parseShort(s);
                    default: /* APPROX and EQ */
                        return (new Short(s)).equals(obj);
                    }
                } else if (obj instanceof Long) {
                    switch (op) {
                    case LE:
                        return ((Long) obj).longValue() <= Long.parseLong(s);
                    case GE:
                        return ((Long) obj).longValue() >= Long.parseLong(s);
                    default: /* APPROX and EQ */
                        return (new Long(s)).equals(obj);
                    }
                } else if (obj instanceof Float) {
                    switch (op) {
                    case LE:
                        return ((Float) obj).floatValue() <= (new Float(s))
                                .floatValue();
                    case GE:
                        return ((Float) obj).floatValue() >= (new Float(s))
                                .floatValue();
                    default: /* APPROX and EQ */
                        return (new Float(s)).equals(obj);
                    }
                } else if (obj instanceof Double) {
                    switch (op) {
                    case LE:
                        return ((Double) obj).doubleValue() <= (new Double(s))
                                .doubleValue();
                    case GE:
                        return ((Double) obj).doubleValue() >= (new Double(s))
                                .doubleValue();
                    default: /* APPROX and EQ */
                        return (new Double(s)).equals(obj);
                    }
                } else if (obj instanceof BigInteger) {
                    int c = ((BigInteger) obj).compareTo(new BigInteger(s));
                    switch (op) {
                    case LE:
                        return c <= 0;
                    case GE:
                        return c >= 0;
                    default: /* APPROX and EQ */
                        return c == 0;
                    }
                } else if (classBigDecimal != null
                        && classBigDecimal.isInstance(obj)) {
                    Object n = consBigDecimal.newInstance(new Object[] { s });
                    int c = ((Integer) compBigDecimal.invoke(obj,
                            new Object[] { n })).intValue();
                    switch (op) {
                    case LE:
                        return c <= 0;
                    case GE:
                        return c >= 0;
                    default: /* APPROX and EQ */
                        return c == 0;
                    }
                }
            } else if (obj instanceof Vector) {
                for (Enumeration e = ((Vector) obj).elements(); e
                        .hasMoreElements();)
                    if (compare(e.nextElement(), op, s))
                        return true;
            } else if (obj.getClass().isArray()) {
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++)
                    if (compare(Array.get(obj, i), op, s))
                        return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    static boolean compareString(String s1, int op, String s2) {
        switch (op) {
        case LE:
            return s1.compareTo(s2) <= 0;
        case GE:
            return s1.compareTo(s2) >= 0;
        case EQ:
            return patSubstr(s1, s2);
        case APPROX:
            return fixupString(s2).equals(fixupString(s1));
        default:
            return false;
        }
    }

    static String fixupString(String s) {
        StringBuffer sb = new StringBuffer();
        int len = s.length();
        boolean isStart = true;
        boolean isWhite = false;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                isWhite = true;
            } else {
                if (!isStart && isWhite)
                    sb.append(' ');
                if (Character.isUpperCase(c))
                    c = Character.toLowerCase(c);
                sb.append(c);
                isStart = false;
                isWhite = false;
            }
        }
        return sb.toString();
    }

    static boolean patSubstr(String s, String pat) {
        if (s == null)
            return false;
        if (pat.length() == 0)
            return s.length() == 0;
        if (pat.charAt(0) == WILDCARD) {
            pat = pat.substring(1);
            for (;;) {
                if (patSubstr(s, pat))
                    return true;
                if (s.length() == 0)
                    return false;
                s = s.substring(1);
            }
        }
        if (s.length() == 0 || s.charAt(0) != pat.charAt(0)) {
            return false;
        }
        return patSubstr(s.substring(1), pat.substring(1));
    }
}
