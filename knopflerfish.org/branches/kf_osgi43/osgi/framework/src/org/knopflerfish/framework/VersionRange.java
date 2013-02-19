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

package org.knopflerfish.framework;

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
   * Check if objects range intersect another VersionRange.
   *
   * @param range VersionRange to compare to.
   * @return Return true if within range, otherwise false.
   */
  public boolean intersectRange(VersionRange range) {
    if (this == range) {
      return true;
    }
    boolean low_below_high = range.high == null;
    if (!low_below_high) {
      final int c = low.compareTo(range.high);
      low_below_high = c < 0 || (c == 0 && lowIncluded && range.highIncluded);
    }
    boolean high_above_low = high == null;
    if (!high_above_low) {
      final int c = high.compareTo(range.low);
      high_above_low = c > 0 || (c == 0 && highIncluded && range.lowIncluded);
    }
    return low_below_high && high_above_low;
  }


  /**
   * Compare object to another VersionRange. VersionRanges are compared on the
   * lower bound.
   *
   * @param obj VersionRange to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   * @exception ClassCastException if object is not a VersionRange object.
   */
  public int compareTo(VersionRange o) {
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
   * Append a filter expression for the lower and upper bound of this version
   * range to the specified string buffer. Note that this method does not add
   * the outer {@code and} to the buffer.
   *
   * @param sb OSGi filter string to append conditions to.
   * @param key the name of the attribute to filter on.
   * @return {@code true} if any condition was appended to the string buffer,
   * {@code false} otherwise.
   */
  boolean appendFilterString(final StringBuffer sb, final String key)
  {
    if (this == defaultVersionRange) {
      // No version requirement to add to the filter.
      return false;
    }

    if (!lowIncluded) {
      sb.append("(!");
    }
    sb.append('(');
    sb.append(key);
    sb.append(lowIncluded ? '>' : '<');
    sb.append('=');
    sb.append(low.toString());
    sb.append(')');
    if (!lowIncluded) {
      sb.append(')');
    }

    if (high != null) {
      if (!highIncluded) {
        sb.append("(!");
      }
      sb.append('(');
      sb.append(key);
      sb.append(highIncluded ? '<' : '>');
      sb.append('=');
      sb.append(high.toString());
      sb.append(')');
      if (!highIncluded) {
        sb.append(')');
      }
    }

    return true;
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
