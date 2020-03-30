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

package org.knopflerfish.tools.jarunpacker;

import java.util.*;
import java.awt.Color;

public class Util {

  // Integer (red.green.blue) -> Color
  static Hashtable colors = new Hashtable();
  
  static int maxK = 256;

  static Color rgbInterPolate(Color c1, Color c2, double k) {

    int K = (int)(maxK * k);

    if(c1 == null || c2 == null) {
      return Color.gray;
    }

    if(k == 0.0) return c1;
    if(k == 1.0) return c2;

    int r1 = c1.getRed();
    int g1 = c1.getGreen();
    int b1 = c1.getBlue();
    int r2 = c2.getRed();
    int g2 = c2.getGreen();
    int b2 = c2.getBlue();

    int r = (int)(r1 + (double)K * (r2 - r1) / maxK);
    int g = (int)(g1 + (double)K * (g2 - g1) / maxK);
    int b = (int)(b1 + (double)K * (b2 - b1) / maxK);

    Integer key = new Integer((r << 16) | (g << 8) | g);

    Color c = (Color)colors.get(key);
    if(c == null) {
      c = new Color(r, g, b);
      colors.put(key, c);
    }
    return c;
  }

  static Color rgbInterPolate2(Color c1, Color c2, double k) {

    if(c1 == null || c2 == null) {
      return Color.gray;
    }

    if(k == 0.0) return c1;
    if(k == 1.0) return c2;

    int r1 = c1.getRed();
    int g1 = c1.getGreen();
    int b1 = c1.getBlue();
    int r2 = c2.getRed();
    int g2 = c2.getGreen();
    int b2 = c2.getBlue();

    int r = (int)(r1 + (double)(r2 - r1));
    int g = (int)(g1 + (double)(g2 - g1));
    int b = (int)(b1 + (double)(b2 - b1));

    Color c = new Color(r, g, b);
    return c;
  }


  public static boolean isWindows() {
    String os = System.getProperty("os.name");
    if(os != null) {
      return -1 != os.toLowerCase().indexOf("win");
    }
    return false;
  }
}

