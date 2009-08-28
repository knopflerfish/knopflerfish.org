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
    { "x86-64", "amd64" }
  };


  /**
   * List of OS name aliases. The first entry is the true name.
   */
  final public static String[][] osNameAliases = {
    { "OS2", "OS/2" },
    { "QNX", "procnto" },
    { "Windows95", "Windows 95", "Win95" },
    { "Windows98", "Windows 98", "Win98" },
    { "WindowsNT", "Windows NT", "WinNT" },
    { "WindowsCE", "Windows CE", "WinCE" },
    { "Windows2000", "Windows 2000", "Win2000" },
    { "WindowsXP", "Windows XP", "WinXP" },
  };


  /**
   * Unify processor names.
   *
   * @param name Processor name.
   * @return The unified name.
   */
  static public String unifyProcessor(String name) {
    for (int i = 0; i < processorAliases.length; i++) {
      for (int j = 0; j < processorAliases[i].length; j++) {
	if (name.equalsIgnoreCase(processorAliases[i][j])) {
	  return processorAliases[i][0];
	}
      }
    }
    return name;
  }


  /**
     * Unify OS names.
     *
     * @param name OS name.
     * @return The unified name.
     */
  static public String unifyOsName(String name) {
    for (int i = 0; i < osNameAliases.length; i++) {
      for (int j = 0; j < osNameAliases[i].length; j++) {
	if (name.equalsIgnoreCase(osNameAliases[i][j])) {
	  return osNameAliases[i][0];
	}
      }
    }
    return name;
  }

}
