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

package org.knopflerfish.bundle.desktop.swing.console;

import java.awt.Color;

public class Config {

  int     fontSize           = 11;
  String  fontName           = "Monospaced";
  String  bgColor            = "E7E7DE";
  String  textColor          = "000000";
  boolean grabSystemIO       = true;
  boolean multiplexSystemOut = true;
  boolean multiplexSystemErr = true;


  /**
   * Parse color in hex format "RRGGBB"
   */
  public static Color parseColor(String s) {
    int r = 0;
    int g = 0;
    int b = 0;

    if(s != null && s.length() >= 6) {
      String sr = s.substring(0, 2);
      String sg = s.substring(2, 4);
      String sb = s.substring(4, 6);

      try {
        r = Integer.parseInt(sr, 16);
      } catch(Exception e) { }
      try {
        g = Integer.parseInt(sg, 16);
      } catch(Exception e) { }
      try {
        b = Integer.parseInt(sb, 16);
      } catch(Exception e) { }
    }
    return new java.awt.Color(r, g, b);
  }

}
