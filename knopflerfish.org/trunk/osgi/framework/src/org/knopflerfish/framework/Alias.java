/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import java.util.ArrayList;

/**
 * This class contains aliases for system properties.
 *
 * @author Jan Stein
 */
public class Alias {

  /**
   * List of processor aliases. The first entry is the true name.
   */
  final public static String[][] processorAliases = {
    { "Ignite", "psc1k" },
    { "PowerPC", "power", "ppc", "ppcbe" },
    { "x86", "pentium", "i386", "i486", "i586", "i686" },
    { "x86-64", "amd64", "em64t", "x86_64" }
  };


  /**
   * List of OS name aliases. The first entry is the true name.
   * All aliases must be in lowercase.
   */
  final public static String[][] osNameAliases = {
    { "Epoc32",    "symbianos" },
    { "HPUX",      "hp-ux" },
    { "MacOSX",    "mac os x" },
    { "OS2",       "os/2" },
    { "QNX",       "procnto" },
    { "windows95", "windows 95", "win95" },
    { "Windows98", "windows 98", "win98" },
    { "WindowsNT", "windows nt", "winnt" },
    { "WindowsCE", "windows ce", "wince" },
    { "Windows2000", "windows 2000", "win2000" },
    { "WindowsXP", "windows xp", "winxp" },
    { "Windows2003", "windows 2003", "win2003", "windows server 2003" },
    { "WindowsVista", "windows vista", "winvista" },
    { "Win32", "win*" },
  };


  /**
   * Unify processor names.
   *
   * @param name Processor name.
   * @return The unified name.
   */
  static public ArrayList unifyProcessor(String name) {
    ArrayList res = new ArrayList(2);
    for (int i = 0; i < processorAliases.length; i++) {
      for (int j = 1; j < processorAliases[i].length; j++) {
        if (name.equalsIgnoreCase(processorAliases[i][j])) {
          res.add(processorAliases[i][0]);
          break;
        }
      }
    }
    res.add(name);
    return res;
  }


  /**
     * Unify OS names.
     *
     * @param name OS name.
     * @return The unified name.
     */
  static public ArrayList unifyOsName(String name) {
    ArrayList res = new ArrayList(3);
    String lname = name.toLowerCase();
    for (int i = 0; i < osNameAliases.length; i++) {
      for (int j = 1; j < osNameAliases[i].length; j++) {
        int last = osNameAliases[i][j].length() - 1;
        if (lname.equals(osNameAliases[i][j]) ||
            osNameAliases[i][j].charAt(last) == '*' &&
            lname.startsWith(osNameAliases[i][j].substring(0, last))) {
          if (!lname.equals(osNameAliases[i][0])) {
            res.add(osNameAliases[i][0]);
          }
          break;
        }
      }
    }
    res.add(name);
    return res;
  }

}
