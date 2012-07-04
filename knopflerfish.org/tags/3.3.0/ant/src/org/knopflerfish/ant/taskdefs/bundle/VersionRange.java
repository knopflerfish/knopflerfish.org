/*
 * Copyright (c) 2008-2008, KNOPFLERFISH project
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

import org.osgi.framework.Version;

public class VersionRange {

  Version lowerBound;
  Version upperBound;
  boolean lowerBoundInclusive;
  boolean upperBoundInclusive;

  public VersionRange() {
    this(null);
  }

  public VersionRange(String s) {
    if (s==null) s = "0";
    s = s.trim();

    char c = s.charAt(0);
    if ('['==c) {
      lowerBoundInclusive = true;
      s = s.substring(1);
    } else if ('('==c) {
      lowerBoundInclusive = false;
      s = s.substring(1);
    } else {
      lowerBoundInclusive = true;
    }

    c = s.charAt(s.length()-1);
    if (']'==c) {
      upperBoundInclusive = true;
      s = s.substring(0,s.length()-1);
    } else if (')'==c) {
      upperBoundInclusive = false;
      s = s.substring(0,s.length()-1);
    } else {
      upperBoundInclusive = false;
    }

    int splitPos = s.indexOf(",");
    if (-1==splitPos) {
      lowerBound = new Version(s);
      upperBound = null; // Infinity
    } else {
      lowerBound = new Version(s.substring(0,splitPos).trim());
      upperBound = new Version(s.substring(splitPos+1,s.length()).trim());
    }
  }

  /**
   * Check if the specified version is contained in this version
   * range.
   * @param ver The version to check.
   * @return <tt>true</tt> if the specified version is in this range,
   *         <tt>false</tt> otherwise.
   */
  public boolean contains(Version ver)
  {
    boolean res = false;
    if (null==upperBound) {
      res = lowerBound.compareTo(ver) <= 0;
    } else {
      if (lowerBoundInclusive) {
        res = lowerBound.compareTo(ver)<=0;
      } else {
        res = lowerBound.compareTo(ver)<0;
      }
      if (upperBoundInclusive) {
        res &= ver.compareTo(upperBound)<=0;
      } else {
        res &= ver.compareTo(upperBound)<0;
      }
    }
    return res;
  }


  public String toString() {
    StringBuffer sb = new StringBuffer();
    if (null==upperBound) {
      sb.append(lowerBound.toString());
    } else {
      sb.append(lowerBoundInclusive ? '[' : '(')
        .append(lowerBound.toString())
        .append(',')
        .append(upperBound.toString())
        .append(upperBoundInclusive ? ']' : ')');
    }
    return sb.toString();
  }
}
