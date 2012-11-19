/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

import java.util.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class LDAPExpr {
  public static final int AND = 0;
  public static final int OR = 1;
  public static final int NOT = 2;
  public static final int EQ = 4;
  public static final int LE = 8;
  public static final int GE = 16;
  public static final int APPROX = 32;
  public static final int COMPLEX = AND | OR | NOT;
  public static final int SIMPLE = EQ | LE | GE | APPROX;

  private static final char WILDCARD = 65535;
  private static final String WILDCARD_STRING = new String(new char[] { WILDCARD });

  private static final String NULL = "Null query";
  private static final String GARBAGE = "Trailing garbage";
  private static final String EOS = "Unexpected end of query";
  private static final String MALFORMED = "Malformed query";
  private static final String OPERATOR = "Undefined operator";

  private static Class classBigDecimal;
  private static Constructor consBigDecimal;
  private static Method compBigDecimal;

  private static Class classBigInteger;
  private static Constructor consBigInteger;
  private static Method compBigInteger;

  public int operator;
  public LDAPExpr[] args;
  public String attrName;
  public String attrValue;


  public LDAPExpr(String filter) throws InvalidSyntaxException {

    ParseState ps = new ParseState(filter);
    LDAPExpr expr = null;
    try {
      expr = parseExpr(ps);
    } catch (StringIndexOutOfBoundsException e) {
      ps.error(EOS);
    }
    if (ps.rest().trim().length() != 0)
      ps.error(GARBAGE + " '" + ps.rest() + "'");
    operator = expr.operator;
    args = expr.args;
    attrName = expr.attrName;
    attrValue = expr.attrValue;
  }


  private LDAPExpr(int operator, LDAPExpr[] args) {
    this.operator = operator;
    this.args = args;
    this.attrName = null;
    this.attrValue = null;
  }


  private LDAPExpr(int operator, String attrName, String attrValue) {
    this.operator = operator;
    this.args = null;
    this.attrName = attrName;
    this.attrValue = attrValue;
  }


  /**
   * Get object class set matched by this LDAP expression. This will not work
   * with wildcards and NOT expressions. If a set can not be determined return
   * null.
   * 
   * @return Set of classes matched, otherwise <code>null</code>.
   */
  public Set getMatchedObjectClasses() {
    Set objClasses = null;
    if (operator == EQ) {
      if (attrName.equalsIgnoreCase(Constants.OBJECTCLASS) && attrValue.indexOf(WILDCARD) < 0) {
        objClasses = new OneSet(attrValue);
      }
    } else if (operator == AND) {
      for (int i = 0; i < args.length; i++) {
        Set r = args[i].getMatchedObjectClasses();
        if (r != null) {
          if (objClasses == null) {
            objClasses = r;
          } else {
            // if AND op and classes in several operands,
            // then only the intersection is possible.
            if (objClasses instanceof OneSet) {
              objClasses = new TreeSet(objClasses);
            }
            objClasses.retainAll(r);
          }
        }
      }
    } else if (operator == OR) {
      for (int i = 0; i < args.length; i++) {
        Set r = args[i].getMatchedObjectClasses();
        if (r != null) {
          if (objClasses == null) {
            objClasses = new TreeSet();
          }
          objClasses.addAll(r);
        } else {
          objClasses = null;
          break;
        }
      }
    }
    return objClasses;
  }


  /**
   * Checks if this LDAP expression is "simple". The definition of a simple
   * filter is:
   * <ul>
   * <li><code>(<it>name</it>=<it>value</it>)</code> is simple if <it>name</it>
   * is a member of the provided <code>keywords</code>, and <it>value</it> does
   * not contain a wildcard character;</li>
   * <li><code>(| EXPR+ )</code> is simple if all <code>EXPR</code> expressions
   * are simple;</li>
   * <li>No other expressions are simple.</li>
   * </ul>
   * If the filter is found to be simple, the <code>cache</code> is filled with
   * mappings from the provided keywords to lists of attribute values. The
   * keyword-value-pairs are the ones that satisfy this expression, for the
   * given keywords.
   * 
   * @param keywords The keywords to look for.
   * @param cache An array (indexed by the keyword indexes) of lists to fill in
   *          with values saturating this expression.
   * @return <code>true</code> if this expression is simple, <code>false</code>
   *         otherwise.
   */
  public boolean isSimple(List keywords, List[] cache, boolean matchCase) {
    if (operator == EQ) {
      int index;
      if ((index = keywords.indexOf(matchCase ? attrName : attrName.toLowerCase())) >= 0
          && attrValue.indexOf(WILDCARD) < 0) {
        if (cache[index] == null) {
          cache[index] = new ArrayList();
        }
        cache[index].add(attrValue);
        return true;
      }
    } else if (operator == OR) {
      for (int i = 0; i < args.length; i++) {
        if (!args[i].isSimple(keywords, cache, matchCase))
          return false;
      }
      return true;
    }
    return false;
  }


  public static boolean query(String filter, Dictionary pd) throws InvalidSyntaxException {
    return new LDAPExpr(filter).evaluate(pd, false);
  }


  /**
   * Evaluate this LDAP filter.
   */
  public boolean evaluate(Dictionary p, boolean matchCase) {
    if ((operator & SIMPLE) != 0) {
      return compare(p.get(matchCase ? attrName : attrName.toLowerCase()), operator, attrValue);
    } else { // (operator & COMPLEX) != 0
      switch (operator) {
      case AND:
        for (int i = 0; i < args.length; i++) {
          if (!args[i].evaluate(p, matchCase))
            return false;
        }
        return true;
      case OR:
        for (int i = 0; i < args.length; i++) {
          if (args[i].evaluate(p, matchCase))
            return true;
        }
        return false;
      case NOT:
        return !args[0].evaluate(p, matchCase);
      default:
        return false; // Cannot happen
      }
    }
  }


  /**** Private methods ****/

  protected boolean compare(Object obj, int op, String s) {
    if (obj == null)
      return false;
    if (op == EQ && s.equals(WILDCARD_STRING))
      return true;
    try {
      if (obj instanceof String) {
        return compareString((String)obj, op, s);
      } else if (obj instanceof Character) {
        return compareString(obj.toString(), op, s);
      } else if (obj instanceof Boolean) {
        if (op == LE || op == GE)
          return false;
        if (((Boolean)obj).booleanValue()) {
          return s.equalsIgnoreCase("true");
        } else {
          return s.equalsIgnoreCase("false");
        }
      } else if (obj instanceof Number) {
        if (obj instanceof Byte) {
          switch (op) {
          case LE:
            return ((Byte)obj).byteValue() <= Byte.parseByte(s);
          case GE:
            return ((Byte)obj).byteValue() >= Byte.parseByte(s);
          default: /* APPROX and EQ */
            return (new Byte(s)).equals(obj);
          }
        } else if (obj instanceof Integer) {
          switch (op) {
          case LE:
            return ((Integer)obj).intValue() <= Integer.parseInt(s);
          case GE:
            return ((Integer)obj).intValue() >= Integer.parseInt(s);
          default: /* APPROX and EQ */
            return (new Integer(s)).equals(obj);
          }
        } else if (obj instanceof Short) {
          switch (op) {
          case LE:
            return ((Short)obj).shortValue() <= Short.parseShort(s);
          case GE:
            return ((Short)obj).shortValue() >= Short.parseShort(s);
          default: /* APPROX and EQ */
            return (new Short(s)).equals(obj);
          }
        } else if (obj instanceof Long) {
          switch (op) {
          case LE:
            return ((Long)obj).longValue() <= Long.parseLong(s);
          case GE:
            return ((Long)obj).longValue() >= Long.parseLong(s);
          default: /* APPROX and EQ */
            return (new Long(s)).equals(obj);
          }
        } else if (obj instanceof Float) {
          switch (op) {
          case LE:
            return ((Float)obj).floatValue() <= (new Float(s)).floatValue();
          case GE:
            return ((Float)obj).floatValue() >= (new Float(s)).floatValue();
          default: /* APPROX and EQ */
            return (new Float(s)).equals(obj);
          }
        } else if (obj instanceof Double) {
          switch (op) {
          case LE:
            return ((Double)obj).doubleValue() <= (new Double(s)).doubleValue();
          case GE:
            return ((Double)obj).doubleValue() >= (new Double(s)).doubleValue();
          default: /* APPROX and EQ */
            return (new Double(s)).equals(obj);
          }
        } else if (classBigInteger != null && classBigInteger.isInstance(obj)) {
          Object n = consBigInteger.newInstance(new Object[] { s });
          int c = ((Integer)compBigInteger.invoke(obj, new Object[] { n })).intValue();

          switch (op) {
          case LE:
            return c <= 0;
          case GE:
            return c >= 0;
          default: /* APPROX and EQ */
            return c == 0;
          }
        } else if (classBigDecimal != null && classBigDecimal.isInstance(obj)) {
          Object n = consBigDecimal.newInstance(new Object[] { s });
          int c = ((Integer)compBigDecimal.invoke(obj, new Object[] { n })).intValue();

          switch (op) {
          case LE:
            return c <= 0;
          case GE:
            return c >= 0;
          default: /* APPROX and EQ */
            return c == 0;
          }
        }
      } else if (obj instanceof Collection) {
        for (Iterator i = ((Collection)obj).iterator(); i.hasNext();)
          if (compare(i.next(), op, s))
            return true;
      } else if (obj.getClass().isArray()) {
        int len = Array.getLength(obj);
        for (int i = 0; i < len; i++)
          if (compare(Array.get(obj, i), op, s))
            return true;
      } else {
        // Extended comparison
        // Allow simple EQ comparison on all classes having
        // a string constructor, and use compareTo if they
        // implement Comparable
        Class clazz = obj.getClass();
        Constructor cons = getConstructor(clazz);

        if (cons != null) {
          Object other = cons.newInstance(new Object[] { s });
          if (obj instanceof Comparable) {
            int c = ((Comparable)obj).compareTo(other);
            switch (op) {
            case LE:
              return c <= 0;
            case GE:
              return c >= 0;
            default: /* APPROX and EQ */
              return c == 0;
            }
          } else {
            boolean b = false;
            if (op == LE || op == GE || op == EQ || op == APPROX) {
              b = obj.equals(other);
            }
            return b;
          }
        }
      }
    } catch (Exception ignored_but_evals_to_false) {
      // This might happen if a string-to-datatype conversion fails
      // Just consider it a false match and ignore the exception
    }
    return false;
  }

  // Clazz -> Constructor(String)
  private static HashMap constructorMap = new HashMap();


  /**
   * Get cached String constructor for a class
   */
  private static Constructor getConstructor(Class clazz) {
    synchronized (constructorMap) {

      // This might be null
      Constructor cons = (Constructor)constructorMap.get(clazz);

      // ...check if we have tried before. A failed try
      // is stored as null
      if (!constructorMap.containsKey(clazz)) {
        try {
          cons = clazz.getConstructor(new Class[] { String.class });
        } catch (Exception e) {
          // remember by storing null in map
        }
        constructorMap.put(clazz, cons);
      }
      return cons;
    }
  }

  static {
    try {
      classBigDecimal = Class.forName("java.math.BigDecimal");
      consBigDecimal = getConstructor(classBigDecimal);
      compBigDecimal = classBigDecimal.getMethod("compareTo", new Class[] { classBigDecimal });
    } catch (Exception ignore) {
      classBigDecimal = null;
    }
    try {
      classBigInteger = Class.forName("java.math.BigInteger");
      consBigInteger = getConstructor(classBigInteger);
      compBigInteger = classBigInteger.getMethod("compareTo", new Class[] { classBigInteger });
    } catch (Exception ignore) {
      classBigInteger = null;
    }
  }


  private static boolean compareString(String s1, int op, String s2) {
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


  private static String fixupString(String s) {
    StringBuffer sb = new StringBuffer();
    int len = s.length();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (!Character.isWhitespace(c)) {
        if (Character.isUpperCase(c))
          c = Character.toLowerCase(c);
        sb.append(c);
      }
    }
    return sb.toString();
  }


  private static boolean patSubstr(String s, String pat) {
    return s == null ? false : patSubstr(s.toCharArray(), 0, pat.toCharArray(), 0);
  }


  private static boolean patSubstr(char[] s, int si, char[] pat, int pi) {
    if (pat.length - pi == 0)
      return s.length - si == 0;
    if (pat[pi] == WILDCARD) {
      pi++;
      for (;;) {
        if (patSubstr(s, si, pat, pi))
          return true;
        if (s.length - si == 0)
          return false;
        si++;
      }
    } else {
      if (s.length - si == 0) {
        return false;
      }
      if (s[si] != pat[pi]) {
        return false;
      }
      return patSubstr(s, ++si, pat, ++pi);
    }
  }


  private static LDAPExpr parseExpr(ParseState ps) throws InvalidSyntaxException {
    ps.skipWhite();
    if (!ps.prefix("("))
      ps.error(MALFORMED);

    int operator;
    ps.skipWhite();
    switch (ps.peek()) {
    case '&':
      operator = AND;
      break;
    case '|':
      operator = OR;
      break;
    case '!':
      operator = NOT;
      break;
    default:
      return parseSimple(ps);
    }
    ps.skip(1); // Ignore the operator
    List v = new ArrayList();
    do {
      v.add(parseExpr(ps));
      ps.skipWhite();
    } while (ps.peek() == '(');
    int n = v.size();
    if (!ps.prefix(")") || n == 0 || (operator == NOT && n > 1))
      ps.error(MALFORMED);
    LDAPExpr[] args = new LDAPExpr[n];
    v.toArray(args);
    return new LDAPExpr(operator, args);
  }


  private static LDAPExpr parseSimple(ParseState ps) throws InvalidSyntaxException {
    String attrName = ps.getAttributeName();
    if (attrName == null)
      ps.error(MALFORMED);
    int operator = 0;
    if (ps.prefix("="))
      operator = EQ;
    else if (ps.prefix("<="))
      operator = LE;
    else if (ps.prefix(">="))
      operator = GE;
    else if (ps.prefix("~="))
      operator = APPROX;
    else {
      // System.out.println("undef op='" + ps.peek() + "'");
      ps.error(OPERATOR); // Does not return
    }
    String attrValue = ps.getAttributeValue();
    if (!ps.prefix(")"))
      ps.error(MALFORMED);
    return new LDAPExpr(operator, attrName, attrValue);
  }


  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("(");
    if ((operator & SIMPLE) != 0) {
      res.append(attrName);
      switch (operator) {
      case EQ:
        res.append("=");
        break;
      case LE:
        res.append("<=");
        break;
      case GE:
        res.append(">=");
        break;
      case APPROX:
        res.append("~=");
        break;
      }
      for (int i = 0; i < attrValue.length(); i++) {
        char c = attrValue.charAt(i);
        if (c == '(' || c == ')' || c == '*' || c == '\\') {
          res.append('\\');
        } else if (c == WILDCARD) {
          c = '*';
        }
        res.append(c);
      }
    } else {
      switch (operator) {
      case AND:
        res.append("&");
        break;
      case OR:
        res.append("|");
        break;
      case NOT:
        res.append("!");
        break;
      }
      for (int i = 0; i < args.length; i++) {
        res.append(args[i].toString());
      }
    }
    res.append(")");
    return res.toString();
  }

  /**
   * Contains the current parser position and parsing utility methods.
   */
  private static class ParseState {
    int pos;
    String str;


    public ParseState(String str) throws InvalidSyntaxException {
      this.str = str;
      if (str.length() == 0)
        error(NULL);
      pos = 0;
    }


    public boolean prefix(String pre) {
      if (!str.startsWith(pre, pos))
        return false;
      pos += pre.length();
      return true;
    }


    public char peek() {
      return str.charAt(pos);
    }


    public void skip(int n) {
      pos += n;
    }


    public String rest() {
      return str.substring(pos);
    }


    public void skipWhite() {
      while (Character.isWhitespace(str.charAt(pos))) {
        pos++;
      }
    }


    public String getAttributeName() {
      int start = pos;
      int end = -1;
      for (;; pos++) {
        char c = str.charAt(pos);
        if (c == '(' || c == ')' || c == '<' || c == '>' || c == '=' || c == '~') {
          break;
        } else if (!Character.isWhitespace(c)) {
          end = pos;
        }
      }
      if (end == -1) {
        return null;
      }
      return str.substring(start, end + 1);
    }


    public String getAttributeValue() {
      StringBuffer sb = new StringBuffer();
      label: for (;; pos++) {
        char c = str.charAt(pos);
        switch (c) {
        case '(':
        case ')':
          break label;
        case '*':
          sb.append(WILDCARD);
          break;
        case '\\':
          sb.append(str.charAt(++pos));
          break;
        default:
          sb.append(c);
          break;
        }
      }
      return sb.toString();
    }


    public void error(String m) throws InvalidSyntaxException {
      throw new InvalidSyntaxException(m, (str == null) ? "" : str.substring(pos));
    }
  }

  /**
   * Set with one element maximum.
   */
  private static class OneSet extends AbstractSet {

    final private Object elem;


    OneSet(Object o) {
      elem = o;
    }


    public Iterator iterator() {
      return new Iterator() {
        Object ielem = elem;


        public boolean hasNext() {
          return ielem != null;
        }


        public Object next() {
          if (ielem != null) {
            Object r = ielem;
            ielem = null;
            return r;
          } else {
            throw new NoSuchElementException();
          }
        }


        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }


    public int size() {
      return elem == null ? 0 : 1;
    }
  }

}
