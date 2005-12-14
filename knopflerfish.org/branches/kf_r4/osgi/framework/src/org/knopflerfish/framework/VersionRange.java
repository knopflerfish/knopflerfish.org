/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

import java.io.*;
import java.util.StringTokenizer;

public class VersionRange implements Comparable
{
  final private Version low;
  final private Version high;
  final private boolean lowIncluded;
  final private boolean highIncluded;

  /**
   * Construct a VersionRanger object
   *
   * @param vr string in "X.Y.Z" format.
   */
  public VersionRange(String vr) throws NumberFormatException {
    if (vr != null) {
      boolean op = vr.startsWith("(");
      boolean ob = vr.startsWith("[");

      if (op || ob) {
	boolean cp = vr.endsWith(")");
	boolean cb = vr.endsWith("]");
	int comma = vr.indexOf(',');

	if (comma > 0 && (cp || cb)) {
	  low = new Version(vr.substring(1, comma));
	  high = new Version(vr.substring(comma + 1, vr.length() - 1));
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
    } else {
      low = Version.emptyVersion;
      high = null;
      lowIncluded = true;
      highIncluded = false;
    }
  }


  public boolean isSpecified() {
    return lowIncluded && high == null && low == Version.emptyVersion;
  }


  /**
   * Compare object to another VersionRange.
   *
   * @param obj Version to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   * @exception ClassCastException if object is not a Version object.
   */
  public boolean withinRange(Version ver) {
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
   * Compare object to another VersionRange. VersionRanges are compared on the
   * lower bound.
   *
   * @param obj VersionRange to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   * @exception ClassCastException if object is not a VersionRange object.
   */
  public int compareTo(Object obj) throws ClassCastException {
    VersionRange o = (VersionRange)obj;
    return low.compareTo(o.low);
  }

  /*
  public int compareToFuzzy(Object obj) throws ClassCastException {
    throw new RuntimeException("NYI");
  }

  public int compareToStrict(Object obj) throws ClassCastException {
    throw new RuntimeException("NYI");
  }
  */


  /**
   * String with version number. If version is not specified return
   * an empty string.
   *
   * @return String.
   */
  public String toString() {
    if (high != null) {
      StringBuffer res = new StringBuffer();
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
   * Check if object is equal to this object.
   *
   * @param obj Package entry to compare to.
   * @return true if equal, otherwise false.
   */
  public boolean equals(Object obj) throws ClassCastException {
    throw new RuntimeException("NYI");
  }


  /**
   * Hash code for this package entry.
   *
   * @return int value.
   */
  public int hashCode() {
    if (high != null) {
      return low.hashCode() + high.hashCode();
    } else {
      return low.hashCode();
    }
  }
}
