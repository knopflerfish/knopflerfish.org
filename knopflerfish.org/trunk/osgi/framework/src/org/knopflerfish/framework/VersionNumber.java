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

package org.knopflerfish.framework;

import java.io.*;
import java.util.StringTokenizer;

public class VersionNumber implements Comparable
{
  final private int x;
  final private int y;
  final private int z;

  static boolean bFuzzy = "true".equals(System.getProperty("org.knopflerfish.framework.version.fuzzy", "true"));

  /**
   * Construct a VersionNumber object
   *
   * @param ver string in X.Y.Z format.
   */
  public VersionNumber(String ver) throws NumberFormatException {
    if (ver != null) {
      StringTokenizer st = new StringTokenizer(ver,".");
      x = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
      y = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
      z = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : -1;
      if (st.hasMoreTokens()) {
	throw new NumberFormatException("Too many '.' in version number");
      }
    } else {
      x = -1;
      y = -1;
      z = -1;
    }
  }


  public boolean isSpecified() {
    return x != -1;
  }


  /**
   * Compare object to another VersionNumber.
   *
   * @param obj Version to compare to.
   * @return Return 0 if equals, negative if this object is less than obj
   *         and positive if this object is larger then obj.
   * @exception ClassCastException if object is not a VersionNumber object.
   */
  public int compareTo(Object obj) throws ClassCastException {
    return bFuzzy ? compareToFuzzy(obj) : compareToStrict(obj);
  }

  public int compareToFuzzy(Object obj) throws ClassCastException {
    VersionNumber v2 = (VersionNumber)obj;

    int a = x != -1 ? x : 0;
    int b = y != -1 ? y : 0;
    int c = z != -1 ? z : 0;

    int res = x - v2.x;

    if (res == 0) {
      res = b - (v2.y != -1 ? v2.y : 0);
      if (res == 0) {
	res = c - (v2.z != -1 ? v2.z : 0);
      }
    }

    return res;
  }

  public int compareToStrict(Object obj) throws ClassCastException {
    VersionNumber v2 = (VersionNumber)obj;

    int res = x - v2.x;

    if (res == 0) {
      res = y - v2.y;
      if (res == 0) {
	res = z - v2.z;
      }
    }
    return res;
  }


  /**
   * String with version number. If version is not specified return
   * an empty string.
   *
   * @return String.
   */
  public String toString() {
    StringBuffer res = new StringBuffer();
    if (x != -1) {
      res.append(x);
      if (y != -1) {
	res.append("." + y);
	if (z != -1) {
	  res.append("." + z);
	}
      }
      return res.toString();
    } else {
      return "";
    }
  }


  /**
   * Check if object is equal to this object.
   *
   * @param obj Package entry to compare to.
   * @return true if equal, otherwise false.
   */
  public boolean equals(Object obj) throws ClassCastException {
    VersionNumber o = (VersionNumber)obj;
    return x == o.x && y == o.y && z == o.z;
  }


  /**
   * Hash code for this package entry.
   *
   * @return int value.
   */
  public int hashCode() {
    return x << 6 + y << 3 + z;
  }
}
