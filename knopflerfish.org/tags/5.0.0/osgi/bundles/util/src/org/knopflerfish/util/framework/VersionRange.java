/*
 * Copyright (c) 2005-2013, KNOPFLERFISH project
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

package org.knopflerfish.util.framework;

import org.osgi.framework.Version;

/**
 * Class representing OSGi version ranges.
 *
 * @author Jan Stein
 */
public class VersionRange implements Comparable<VersionRange>
{
  final private Version low;
  final private Version high;
  final private boolean lowIncluded;
  final private boolean highIncluded;

  /**
   * The empty version range "[0.0.0,inf)".
   */
  public static final VersionRange defaultVersionRange = new VersionRange();

  /**
   * Construct a VersionRange object.
   * Format for a range:
   *   ( "(" | "[" ) LOW_VERSION ","  HIGH_VERSION ( ")" | "]" )
   * Format for at least a version:
   *   VERSION
   *
   * @param vr Input string.
   */
  public VersionRange(String vr) throws NumberFormatException {
    final boolean op = vr.startsWith("(");
    final boolean ob = vr.startsWith("[");

    if (op || ob) {
      final boolean cp = vr.endsWith(")");
      final boolean cb = vr.endsWith("]");
      final int comma = vr.indexOf(',');

      if (comma > 0 && (cp || cb)) {
        low = new Version(vr.substring(1, comma).trim());
        high = new Version(vr.substring(comma + 1, vr.length() - 1).trim());
        lowIncluded = ob;
        highIncluded = cb;
      } else  {
        throw new NumberFormatException("Illegal version range: " + vr);
      }
    } else {
      low = new Version(vr);
      high = null;
      lowIncluded = true;
      highIncluded = false;
    }
  }


  /**
   * Construct the default VersionRange object.
   *
   */
  protected VersionRange() {
    low = Version.emptyVersion;
    high = null;
    lowIncluded = true;
    highIncluded = false;
  }


  /**
   * Extract a version range from an OSGi LDAP filter.
   *
   * E.g. from a require capability filter like
   *
   * <pre>
   *  (&amp;(osgi.wiring.package=org.kxml.io)(&amp;(version>=0.0.0)(!(version>=1.0.0))))
   * </pre>
   *
   * @param filter
   *          The filter string to process.
   * @param key
   *          The attribute name of the version.
   * @param defaultIfNotSpecified
   *          if no version is found in the filter then this controls whether to
   *          return the default version range or throw
   *          {@link IllegalArgumentException}.
   *
   * @throws IllegalArgumentException
   *           when no version range was found in the filter.
   */
  public VersionRange(final String filter, final String key,
                      boolean defaultIfNotSpecified)
  {
    Version low = null;
    Version high = null;
    boolean lowIncluded = true;
    boolean highIncluded = false;

    boolean negated = false;
    String op = null;
    int start = filter.indexOf(key);
    while (start>-1) {
      int end = start + key.length();

      // Check to the left for '(' and '!'
      --start;
      while (start>=0 && Character.isWhitespace(filter.charAt(start))) {
        --start;
      }
      if (filter.charAt(start) == '(') {
        --start;
        while (start>=0 && Character.isWhitespace(filter.charAt(start))) {
          --start;
        }
        negated = filter.charAt(start) == '!';
      }

      while (end<filter.length() && Character.isWhitespace(filter.charAt(end))) {
        ++end;
      }
      if (filter.charAt(end) == '=') {
        op = "eq";
      } else if (filter.charAt(end) == '<' && filter.charAt(++end) == '=') {
        op = negated ? "gt" : "le";
      } else if (filter.charAt(end) == '>' && filter.charAt(++end) == '=') {
        op = negated ? "lt" : "ge";
      }
      start = ++end;
      while (start<filter.length() && Character.isWhitespace(filter.charAt(start))) {
        ++start;
      }
      end = filter.indexOf(')', start);
      if (end > -1) {
        try {
          final Version v = new Version(filter.substring(start, end));
          if ("eq".equals(op)) {
            low = v;
            high = v;
            highIncluded = true;
            lowIncluded = true;
            break;
          } else if (op.charAt(0) == 'g') {
            low = v;
            lowIncluded = op.charAt(1) == 'e';
          } else if (op.charAt(0) == 'l') {
            high = v;
            highIncluded = op.charAt(1) == 'e';
          }
        } catch (final IllegalArgumentException eae) {
        }
      }
      start = filter.indexOf(key, end);
    }

    if (low!=null) {
      this.low = low;
      this.lowIncluded = lowIncluded;
      this.high = high;
      this.highIncluded = highIncluded;
    } else if (defaultIfNotSpecified) {
      // The default empty version
      this.low = Version.emptyVersion;
      this.lowIncluded = true;
      this.high = null;
      this.highIncluded = false;
    } else {
      throw new IllegalArgumentException("no version range found in " +filter);
    }

  }


  public boolean isSpecified() {
    return this != defaultVersionRange;
  }


  /**
   * Check if specified version is within our range.
   *
   * @param ver Version to compare to.
   * @return Return true if within range, otherwise false.
   */
  public boolean withinRange(Version ver) {
    if (this == defaultVersionRange) {
      return true;
    }
    int c = low.compareTo(ver);

    if (c < 0 || (c == 0 && lowIncluded)) {
      if (high == null) {
	return true;
      }
      c = high.compareTo(ver);
      return c > 0 || (c == 0 && highIncluded);
    }
    return false;
  }


  /**
   * Check if objects range is within another VersionRange.
   *
   * @param range VersionRange to compare to.
   * @return Return true if within range, otherwise false.
   */
  public boolean withinRange(VersionRange range) {
    if (this == range) {
      return true;
    }
    int c = low.compareTo(range.low);

    if (c < 0 || (c == 0 && lowIncluded == range.lowIncluded)) {
      if (high == null) {
	return true;
      }
      c = high.compareTo(range.high);
      return c > 0 || (c == 0 && highIncluded == range.highIncluded);
    }
    return false;
  }


  /**
   * Compare object to another VersionRange. VersionRanges are compared on the
   * lower bound.
   *
   * @param o VersionRange to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   * @exception ClassCastException if object is not a VersionRange object.
   */
  public int compareTo(VersionRange o)
  {
    return low.compareTo(o.low);
  }

  /**
   * String with version number. If version is not specified return
   * an empty string.
   *
   * @return String.
   */
  @Override
  public String toString()
  {
    if (high != null) {
      final StringBuffer res = new StringBuffer();
      if (lowIncluded) {
        res.append('[');
      } else {
        res.append('(');
      }
      res.append(low.toString());
      res.append(',');
      res.append(high.toString());
      if (highIncluded) {
        res.append(']');
      } else {
        res.append(')');
      }
      return res.toString();
    } else {
      return low.toString();
    }
  }

  /**
   * HTML-formated range that allays has both a lower and an upper limit.
   *
   * @return String.
   */
  public String toHtmlString()
  {
    final StringBuffer res = new StringBuffer();
    if (high != null) {
      if (lowIncluded) {
        res.append('[');
      } else {
        res.append('(');
      }
      res.append(low.toString());
      res.append(',');
      res.append(high.toString());
      if (highIncluded) {
        res.append(']');
      } else {
        res.append(')');
      }
    } else {
      res.append('[');
      res.append(low.toString());
      res.append(",&infin;)");
    }
    return res.toString();
  }

  /**
   * Check if object is equal to this object.
   *
   * @param obj Package entry to compare to.
   * @return true if equal, otherwise false.
   */
  @Override
  public boolean equals(Object obj) throws ClassCastException {
    final VersionRange o = (VersionRange)obj;
    if (low.equals(o.low)) {
      if (high != null) {
        return high.equals(o.high)  &&
          lowIncluded == o.lowIncluded &&
          highIncluded == o.highIncluded;
      } else {
        return true;
      }
    }
    return false;
  }


  /**
   * Hash code for this package entry.
   *
   * @return int value.
   */
  @Override
  public int hashCode() {
    if (high != null) {
      return low.hashCode() + high.hashCode();
    } else {
      return low.hashCode();
    }
  }
}
