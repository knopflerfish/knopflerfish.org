/*
 * Copyright (c) 2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.prefs;

import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.Color;

/**
 * color names to set to java colors instances.
 */
public class Colors extends Hashtable {
  
  /**
   * The color map. Keys are lower case string, values are java.awt.Color
   */
  public static Colors COLORS = new Colors();
  

  /**
   * Get a color, either defined by #RRGGBB or an X11 name as "ivory"
   */
  public static Color getColor(String name) {
    Color c = null;
    if(name.startsWith("#")) {
      c = parseColor(name);
    } else {
      c = (Color)COLORS.get(name.toLowerCase());
    }
    if(c == null) {
      System.err.println("X11Colors: No color '" + name + "'");
    }
    return c;

  }

  public static Color parseColor(String rgb) {
    Color c = Color.black;
    if(rgb.startsWith("#")) {
      rgb = rgb.substring(1);
    }
    if(rgb.length() < 6) {
      throw new IllegalArgumentException("Color string must be a color name or RRGGBB format, found '" + rgb + "'");
      
    }
    int r = Integer.parseInt(rgb.substring(0,2), 16);
    int g = Integer.parseInt(rgb.substring(2,4), 16);
    int b = Integer.parseInt(rgb.substring(4,6), 16);
    
    c = new Color(r, g, b);
    return c;
  }  

  /**
   * Get color name if it exists in lookup map, otherwise #RRGGBB
   */
  public static String toString(Color c) {
    StringBuffer sb = new StringBuffer();
    
    for(Enumeration e = COLORS.keys(); e.hasMoreElements();) {
      String name = (String)e.nextElement();
      Color  col  = (Color)COLORS.get(name);
      if(col.equals(c)) {
        return name;
      }
    }

    sb.append("#");
    int r = c.getRed();
    int g = c.getGreen();
    int b = c.getBlue();

    sb.append(toHex(r, 2));
    sb.append(toHex(g, 2));
    sb.append(toHex(b, 2));

    return sb.toString();
  }

  /**
   * Convert an integer to a (zero-padded) hex string.
   */
  public static String toHex(int n, int len) {
    StringBuffer s = new StringBuffer(Integer.toHexString(n));
    while(s.length() < len) {
      s.insert(0, "0");
    }
    return s.toString();
  }

  public static Color rgbInterpolate(Color c1, Color c2, double k) {
    
    if(c1 == null || c2 == null) {
      return Color.gray;
    }

    if(k <= 0.0) return c1;
    if(k >= 1.0) return c2;

    int r1 = c1.getRed();
    int g1 = c1.getGreen();
    int b1 = c1.getBlue();
    int r2 = c2.getRed();
    int g2 = c2.getGreen();
    int b2 = c2.getBlue();

    int r = (int)(r1 + k * (double)(r2 - r1));
    int g = (int)(g1 + k * (double)(g2 - g1));
    int b = (int)(b1 + k * (double)(b2 - b1));

    Color c = new Color(r, g, b);
    return c;
  }


  private Colors() {
    put("lightgrey", parseColor("#e2e2e2"));
    put("grey", parseColor("#cccccc"));
    put("gray", parseColor("#cccccc"));
    put("darkgrey", parseColor("#999999"));
    put("greydark", parseColor("#999999"));
    put("graydark", parseColor("#999999"));
    put("verydarkgrey", parseColor("#333333"));
    put("brightgrey", 	parseColor("#ededed"));
    put("greybright", 	parseColor("#ededed"));
    put("graybright", 	parseColor("#ededed"));
    
    put("lightbrown", parseColor("#ffcc99"));
    put("brown", parseColor("#cc9966"));
    put("darkbrown", parseColor("#cc6600"));
    put("verydarkbrown", parseColor("#996633"));
    put("brightbrown", parseColor("#ff6633"));
    put("lightred", parseColor("#ffcccc"));
    put("red", parseColor("#ff9999"));
    put("darkred", parseColor("#cc3366"));
    put("verydarkred", parseColor("#990033"));
    put("brightred", parseColor("#ff0033"));

    put("lightyellow", parseColor("#ffffcc"));
    put("yellow", parseColor("#ffff99"));
    put("darkyellow", parseColor("#ffff66"));
    put("verydarkyellow", parseColor("#ffcc33"));
    put("brightyellow", parseColor("#ffff00"));

    put("lightgreen", parseColor("#ccffcc"));
    put("green", parseColor("#99cc99"));
    put("darkgreen", parseColor("#669966"));
    put("verydarkgreen", parseColor("#006600"));
    put("brightgreen", parseColor("#00ff00"));

    put("lightorange", parseColor("#ffcc00"));
    put("orange", parseColor("#ff9966"));
    put("darkorange", parseColor("#ff6600"));
    put("verydarkorange", parseColor("#ff3333"));
    put("brightorange", parseColor("#ff9900"));

    put("lightcyan", parseColor("#ccffff"));
    put("cyan", parseColor("#99cccc"));
    put("darkcyan", parseColor("#009999"));
    put("verydarkcyan", parseColor("#006666"));
    put("brightcyan", parseColor("#00ffff"));

    put("lightblue", parseColor("#99ccff"));
    put("bluelight", parseColor("#99ccff"));
    put("blue", parseColor("#6699ff"));
    put("darkblue", parseColor("#3333ff"));
    put("bluedark", parseColor("#3333ff"));
    put("verydarkblue", parseColor("#3333cc"));
    put("brightblue", parseColor("#0099ff"));

    put("lightpurple", parseColor("#ffccff"));
    put("purple", parseColor("#cc99ff"));
    put("darkpurple", parseColor("#cc66ff"));
    put("verydarkpurple", parseColor("#660099"));
    put("brightpurple", parseColor("#ff00ff"));

    put("lightslate", parseColor("#ccccff"));
    put("slate", parseColor("#9999cc"));
    put("darkslate", parseColor("#666699"));
    put("verydarkslate", parseColor("#333366"));
    put("brightslate", parseColor("#b3b3e2"));


    // std X11 color database
    put("snow", new Color(255, 250, 250));
    put("ghost white", new Color(248, 248, 255));
    put("white smoke", new Color(245, 245, 245));
    put("gainsboro", new Color(220, 220, 220));
    put("floral white", new Color(255, 250, 240));
    put("old lace", new Color(253, 245, 230));
    put("linen", new Color(250, 240, 230));
    put("antique white", new Color(250, 235, 215));
    put("papaya whip", new Color(255, 239, 213));
    put("blanched almond", new Color(255, 235, 205));
    put("bisque", new Color(255, 228, 196));
    put("peach puff", new Color(255, 218, 185));
    put("navajo white", new Color(255, 222, 173));
    put("moccasin", new Color(255, 228, 181));
    put("cornsilk", new Color(255, 248, 220));
    put("ivory", new Color(255, 255, 240));
    put("lemon chiffon", new Color(255, 250, 205));
    put("seashell", new Color(255, 245, 238));
    put("honeydew", new Color(240, 255, 240));
    put("mint cream", new Color(245, 255, 250));
    put("azure", new Color(240, 255, 255));
    put("alice blue", new Color(240, 248, 255));
    put("lavender", new Color(230, 230, 250));
    put("lavender blush", new Color(255, 240, 245));
    put("misty rose", new Color(255, 228, 225));
    put("white", new Color(255, 255, 255));
    put("black", new Color(0, 0, 0));
    put("dark slate gray", new Color(47, 79, 79));
    put("dark slate grey", new Color(47, 79, 79));
    put("dim gray", new Color(105, 105, 105));
    put("dim grey", new Color(105, 105, 105));
    put("slate gray", new Color(112, 128, 144));
    put("slate grey", new Color(112, 128, 144));
    put("light slate gray", new Color(119, 136, 153));
    put("light slate grey", new Color(119, 136, 153));
    put("gray", new Color(190, 190, 190));
    put("grey", new Color(190, 190, 190));
    put("light grey", new Color(211, 211, 211));
    put("light gray", new Color(211, 211, 211));
    put("midnight blue", new Color(25, 25, 112));
    put("navy", new Color(0, 0, 128));
    put("navy blue", new Color(0, 0, 128));
    put("cornflower blue", new Color(100, 149, 237));
    put("dark slate blue", new Color(72, 61, 139));
    put("slate blue", new Color(106, 90, 205));
    put("medium slate blue", new Color(123, 104, 238));
    put("light slate blue", new Color(132, 112, 255));
    put("medium blue", new Color(0, 0, 205));
    put("royal blue", new Color(65, 105, 225));
    put("blue", new Color(0, 0, 255));
    put("dodger blue", new Color(30, 144, 255));
    put("deep sky blue", new Color(0, 191, 255));
    put("sky blue", new Color(135, 206, 235));
    put("light sky blue", new Color(135, 206, 250));
    put("steel blue", new Color(70, 130, 180));
    put("light steel blue", new Color(176, 196, 222));
    put("light blue", new Color(173, 216, 230));
    put("powder blue", new Color(176, 224, 230));
    put("pale turquoise", new Color(175, 238, 238));
    put("dark turquoise", new Color(0, 206, 209));
    put("medium turquoise", new Color(72, 209, 204));
    put("turquoise", new Color(64, 224, 208));
    put("cyan", new Color(0, 255, 255));
    put("light cyan", new Color(224, 255, 255));
    put("cadet blue", new Color(95, 158, 160));
    put("medium aquamarine", new Color(102, 205, 170));
    put("aquamarine", new Color(127, 255, 212));
    put("dark green", new Color(0, 100, 0));
    put("dark olive green", new Color(85, 107, 47));
    put("dark sea green", new Color(143, 188, 143));
    put("sea green", new Color(46, 139, 87));
    put("medium sea green", new Color(60, 179, 113));
    put("light sea green", new Color(32, 178, 170));
    put("pale green", new Color(152, 251, 152));
    put("spring green", new Color(0, 255, 127));
    put("lawn green", new Color(124, 252, 0));
    put("green", new Color(0, 255, 0));
    put("chartreuse", new Color(127, 255, 0));
    put("medium spring green", new Color(0, 250, 154));
    put("green yellow", new Color(173, 255, 47));
    put("lime green", new Color(50, 205, 50));
    put("yellow green", new Color(154, 205, 50));
    put("forest green", new Color(34, 139, 34));
    put("olive drab", new Color(107, 142, 35));
    put("dark khaki", new Color(189, 183, 107));
    put("khaki", new Color(240, 230, 140));
    put("pale goldenrod", new Color(238, 232, 170));
    put("light goldenrod yellow", new Color(250, 250, 210));
    put("light yellow", new Color(255, 255, 224));
    put("yellow", new Color(255, 255, 0));
    put("gold", new Color(255, 215, 0));
    put("light goldenrod", new Color(238, 221, 130));
    put("goldenrod", new Color(218, 165, 32));
    put("dark goldenrod", new Color(184, 134, 11));
    put("rosy brown", new Color(188, 143, 143));
    put("indian red", new Color(205, 92, 92));
    put("saddle brown", new Color(139, 69, 19));
    put("sienna", new Color(160, 82, 45));
    put("peru", new Color(205, 133, 63));
    put("burlywood", new Color(222, 184, 135));
    put("beige", new Color(245, 245, 220));
    put("wheat", new Color(245, 222, 179));
    put("sandy brown", new Color(244, 164, 96));
    put("tan", new Color(210, 180, 140));
    put("chocolate", new Color(210, 105, 30));
    put("firebrick", new Color(178, 34, 34));
    put("brown", new Color(165, 42, 42));
    put("dark salmon", new Color(233, 150, 122));
    put("salmon", new Color(250, 128, 114));
    put("light salmon", new Color(255, 160, 122));
    put("orange", new Color(255, 165, 0));
    put("dark orange", new Color(255, 140, 0));
    put("coral", new Color(255, 127, 80));
    put("light coral", new Color(240, 128, 128));
    put("tomato", new Color(255, 99, 71));
    put("orange red", new Color(255, 69, 0));
    put("red", new Color(255, 0, 0));
    put("hot pink", new Color(255, 105, 180));
    put("deep pink", new Color(255, 20, 147));
    put("pink", new Color(255, 192, 203));
    put("light pink", new Color(255, 182, 193));
    put("pale violet red", new Color(219, 112, 147));
    put("maroon", new Color(176, 48, 96));
    put("medium violet red", new Color(199, 21, 133));
    put("violet red", new Color(208, 32, 144));
    put("magenta", new Color(255, 0, 255));
    put("violet", new Color(238, 130, 238));
    put("plum", new Color(221, 160, 221));
    put("orchid", new Color(218, 112, 214));
    put("medium orchid", new Color(186, 85, 211));
    put("dark orchid", new Color(153, 50, 204));
    put("dark violet", new Color(148, 0, 211));
    put("blue violet", new Color(138, 43, 226));
    put("purple", new Color(160, 32, 240));
    put("medium purple", new Color(147, 112, 219));
    put("thistle", new Color(216, 191, 216));
    put("snow1", new Color(255, 250, 250));
    put("snow2", new Color(238, 233, 233));
    put("snow3", new Color(205, 201, 201));
    put("snow4", new Color(139, 137, 137));
    put("seashell1", new Color(255, 245, 238));
    put("seashell2", new Color(238, 229, 222));
    put("seashell3", new Color(205, 197, 191));
    put("seashell4", new Color(139, 134, 130));
    put("antiquewhite1", new Color(255, 239, 219));
    put("antiquewhite2", new Color(238, 223, 204));
    put("antiquewhite3", new Color(205, 192, 176));
    put("antiquewhite4", new Color(139, 131, 120));
    put("bisque1", new Color(255, 228, 196));
    put("bisque2", new Color(238, 213, 183));
    put("bisque3", new Color(205, 183, 158));
    put("bisque4", new Color(139, 125, 107));
    put("peachpuff1", new Color(255, 218, 185));
    put("peachpuff2", new Color(238, 203, 173));
    put("peachpuff3", new Color(205, 175, 149));
    put("peachpuff4", new Color(139, 119, 101));
    put("navajowhite1", new Color(255, 222, 173));
    put("navajowhite2", new Color(238, 207, 161));
    put("navajowhite3", new Color(205, 179, 139));
    put("navajowhite4", new Color(139, 121, 94));
    put("lemonchiffon1", new Color(255, 250, 205));
    put("lemonchiffon2", new Color(238, 233, 191));
    put("lemonchiffon3", new Color(205, 201, 165));
    put("lemonchiffon4", new Color(139, 137, 112));
    put("cornsilk1", new Color(255, 248, 220));
    put("cornsilk2", new Color(238, 232, 205));
    put("cornsilk3", new Color(205, 200, 177));
    put("cornsilk4", new Color(139, 136, 120));
    put("ivory1", new Color(255, 255, 240));
    put("ivory2", new Color(238, 238, 224));
    put("ivory3", new Color(205, 205, 193));
    put("ivory4", new Color(139, 139, 131));
    put("honeydew1", new Color(240, 255, 240));
    put("honeydew2", new Color(224, 238, 224));
    put("honeydew3", new Color(193, 205, 193));
    put("honeydew4", new Color(131, 139, 131));
    put("lavenderblush1", new Color(255, 240, 245));
    put("lavenderblush2", new Color(238, 224, 229));
    put("lavenderblush3", new Color(205, 193, 197));
    put("lavenderblush4", new Color(139, 131, 134));
    put("mistyrose1", new Color(255, 228, 225));
    put("mistyrose2", new Color(238, 213, 210));
    put("mistyrose3", new Color(205, 183, 181));
    put("mistyrose4", new Color(139, 125, 123));
    put("azure1", new Color(240, 255, 255));
    put("azure2", new Color(224, 238, 238));
    put("azure3", new Color(193, 205, 205));
    put("azure4", new Color(131, 139, 139 ));
    put("slateblue1", new Color(131, 111, 255 ));
    put("slateblue2", new Color(122, 103, 238 ));
    put("slateblue3", new Color(105, 89, 205 ));
    put("slateblue4", new Color(71, 60, 139 ));
    put("royalblue1", new Color(72, 118, 255 ));
    put("royalblue2", new Color(67, 110, 238 ));
    put("royalblue3", new Color(58, 95, 205 ));
    put("royalblue4", new Color(39, 64, 139 ));
    put("blue1", new Color(0, 0, 255 ));
    put("blue2", new Color(0, 0, 238 ));
    put("blue3", new Color(0, 0, 205 ));
    put("blue4", new Color(0, 0, 139 ));
    put("dodgerblue1", new Color(30, 144, 255 ));
    put("dodgerblue2", new Color(28, 134, 238));
    put("dodgerblue3", new Color(24, 116, 205));
    put("dodgerblue4", new Color(16, 78, 139));
    put("steelblue1", new Color(99, 184, 255));
    put("steelblue2", new Color(92, 172, 238));
    put("steelblue3", new Color(79, 148, 205));
    put("steelblue4", new Color(54, 100, 139));
    put("deepskyblue1", new Color( 0, 191, 255));
    put("deepskyblue2", new Color( 0, 178, 238));
    put("deepskyblue3", new Color( 0, 154, 205));
    put("deepskyblue4", new Color( 0, 104, 139));
    put("skyblue1", new Color(135, 206, 255));
    put("skyblue2", new Color(126, 192, 238));
    put("skyblue3", new Color(108, 166, 205));
    put("skyblue4", new Color(74, 112, 139));
    put("lightskyblue1", new Color(176, 226, 255));
    put("lightskyblue2", new Color(164, 211, 238));
    put("lightskyblue3", new Color(141, 182, 205));
    put("lightskyblue4", new Color(96, 123, 139));
    put("slategray1", new Color(198, 226, 255));
    put("slategray2", new Color(185, 211, 238));
    put("slategray3", new Color(159, 182, 205));
    put("slategray4", new Color(108, 123, 139));
    put("lightsteelblue1", new Color(202, 225, 255));
    put("lightsteelblue2", new Color(188, 210, 238));
    put("lightsteelblue3", new Color(162, 181, 205));
    put("lightsteelblue4", new Color(110, 123, 139));
    put("lightblue1", new Color(191, 239, 255));
    put("lightblue2", new Color(178, 223, 238));
    put("lightblue3", new Color(154, 192, 205));
    put("lightblue4", new Color(104, 131, 139));
    put("lightcyan1", new Color(224, 255, 255));
    put("lightcyan2", new Color(209, 238, 238));
    put("lightcyan3", new Color(180, 205, 205));
    put("lightcyan4", new Color(122, 139, 139));
    put("paleturquoise1", new Color(187, 255, 255));
    put("paleturquoise2", new Color(174, 238, 238));
    put("paleturquoise3", new Color(150, 205, 205));
    put("paleturquoise4", new Color(102, 139, 139));
    put("cadetblue1", new Color(152, 245, 255));
    put("cadetblue2", new Color(142, 229, 238));
    put("cadetblue3", new Color(122, 197, 205));
    put("cadetblue4", new Color(83, 134, 139));
    put("turquoise1", new Color( 0, 245, 255));
    put("turquoise2", new Color( 0, 229, 238));
    put("turquoise3", new Color( 0, 197, 205));
    put("turquoise4", new Color( 0, 134, 139));
    put("cyan1", new Color( 0, 255, 255));
    put("cyan2", new Color( 0, 238, 238));
    put("cyan3", new Color( 0, 205, 205));
    put("cyan4", new Color( 0, 139, 139));
    put("darkslategray1", new Color(151, 255, 255));
    put("darkslategray2", new Color(141, 238, 238));
    put("darkslategray3", new Color(121, 205, 205));
    put("darkslategray4", new Color(82, 139, 139));
    put("aquamarine1", new Color(127, 255, 212));
    put("aquamarine2", new Color(118, 238, 198));
    put("aquamarine3", new Color(102, 205, 170));
    put("aquamarine4", new Color(69, 139, 116));
    put("darkseagreen1", new Color(193, 255, 193));
    put("darkseagreen2", new Color(180, 238, 180));
    put("darkseagreen3", new Color(155, 205, 155));
    put("darkseagreen4", new Color(105, 139, 105));
    put("seagreen1", new Color(84, 255, 159));
    put("seagreen2", new Color(78, 238, 148));
    put("seagreen3", new Color(67, 205, 128));
    put("seagreen4", new Color(46, 139,	 87));
    put("palegreen1", new Color(154, 255, 154));
    put("palegreen2", new Color(144, 238, 144));
    put("palegreen3", new Color(124, 205, 124));
    put("palegreen4", new Color(84, 139,	 84));
    put("springgreen1", new Color( 0, 255, 127));
    put("springgreen2", new Color( 0, 238, 118));
    put("springgreen3", new Color( 0, 205, 102));
    put("springgreen4", new Color( 0, 139,	 69));
    put("green1", new Color( 0, 255,	  0));
    put("green2", new Color( 0, 238,	  0));
    put("green3", new Color( 0, 205,	  0));
    put("green4", new Color( 0, 139,	  0));
    put("chartreuse1", new Color(127, 255,	  0));
    put("chartreuse2", new Color(118, 238,	  0));
    put("chartreuse3", new Color(102, 205,	  0));
    put("chartreuse4", new Color(69, 139,	  0));
    put("olivedrab1", new Color(192, 255,	 62));
    put("olivedrab2", new Color(179, 238,	 58));
    put("olivedrab3", new Color(154, 205,	 50));
    put("olivedrab4", new Color(105, 139,	 34));
    put("darkolivegreen1", new Color(202, 255, 112));
    put("darkolivegreen2", new Color(188, 238, 104));
    put("darkolivegreen3", new Color(162, 205,	 90));
    put("darkolivegreen4", new Color(110, 139,	 61));
    put("khaki1", new Color(255, 246, 143));
    put("khaki2", new Color(238, 230, 133));
    put("khaki3", new Color(205, 198, 115));
    put("khaki4", new Color(139, 134,	 78));
    put("lightgoldenrod1", new Color(255, 236, 139));
    put("lightgoldenrod2", new Color(238, 220, 130));
    put("lightgoldenrod3", new Color(205, 190, 112));
    put("lightgoldenrod4", new Color(139, 129,	 76));
    put("lightyellow", new Color(255, 255, 224));
    put("lightyellow1", new Color(255, 255, 224));
    put("lightyellow2", new Color(238, 238, 209));
    put("lightyellow3", new Color(205, 205, 180));
    put("lightyellow4", new Color(139, 139, 122));
    put("yellow1", new Color(255, 255,	  0));
    put("yellow2", new Color(238, 238,	  0));
    put("yellow3", new Color(205, 205,	  0));
    put("yellow4", new Color(139, 139,	  0));
    put("gold1", new Color(255, 215,	  0));
    put("gold2", new Color(238, 201,	  0));
    put("gold3", new Color(205, 173,	  0));
    put("gold4", new Color(139, 117,	  0));
    put("goldenrod1", new Color(255, 193,	 37));
    put("goldenrod2", new Color(238, 180,	 34));
    put("goldenrod3", new Color(205, 155,	 29));
    put("goldenrod4", new Color(139, 105,	 20));
    put("darkgoldenrod1", new Color(255, 185,	 15));
    put("darkgoldenrod2", new Color(238, 173,	 14));
    put("darkgoldenrod3", new Color(205, 149,	 12));
    put("darkgoldenrod4", new Color(139, 101,	  8));
    put("rosybrown1", new Color(255, 193, 193));
    put("rosybrown2", new Color(238, 180, 180));
    put("rosybrown3", new Color(205, 155, 155));
    put("rosybrown4", new Color(139, 105, 105));
    put("indianred1", new Color(255, 106, 106));
    put("indianred2", new Color(238, 99,	 99));
    put("indianred3", new Color(205, 85,	 85));
    put("indianred4", new Color(139, 58,	 58));
    put("sienna1", new Color(255, 130,	 71));
    put("sienna2", new Color(238, 121,	 66));
    put("sienna3", new Color(205, 104,	 57));
    put("sienna4", new Color(139, 71,	 38));
    put("burlywood1", new Color(255, 211, 155));
    put("burlywood2", new Color(238, 197, 145));
    put("burlywood3", new Color(205, 170, 125));
    put("burlywood4", new Color(139, 115,	 85));
    put("wheat1", new Color(255, 231, 186));
    put("wheat2", new Color(238, 216, 174));
    put("wheat3", new Color(205, 186, 150));
    put("wheat4", new Color(139, 126, 102));
    put("tan1", new Color(255, 165,	 79));
    put("tan2", new Color(238, 154,	 73));
    put("tan3", new Color(205, 133,	 63));
    put("tan4", new Color(139, 90,	 43));
    put("chocolate1", new Color(255, 127,	 36));
    put("chocolate2", new Color(238, 118,	 33));
    put("chocolate3", new Color(205, 102,	 29));
    put("chocolate4", new Color(139, 69,	 19));
    put("firebrick1", new Color(255, 48,	 48));
    put("firebrick2", new Color(238, 44,	 44));
    put("firebrick3", new Color(205, 38,	 38));
    put("firebrick4", new Color(139, 26,	 26));
    put("brown1", new Color(255, 64,	 64));
    put("brown2", new Color(238, 59,	 59));
    put("brown3", new Color(205, 51,	 51));
    put("brown4", new Color(139, 35,	 35));
    put("salmon1", new Color(255, 140, 105));
    put("salmon2", new Color(238, 130,	 98));
    put("salmon3", new Color(205, 112,	 84));
    put("salmon4", new Color(139, 76,	 57));
    put("lightsalmon1", new Color(255, 160, 122));
    put("lightsalmon2", new Color(238, 149, 114));
    put("lightsalmon3", new Color(205, 129,	 98));
    put("lightsalmon4", new Color(139, 87,	 66));
    put("orange1", new Color(255, 165,	  0));
    put("orange2", new Color(238, 154,	  0));
    put("orange3", new Color(205, 133,	  0));
    put("orange4", new Color(139, 90,	  0));
    put("darkorange1", new Color(255, 127,	  0));
    put("darkorange2", new Color(238, 118,	  0));
    put("darkorange3", new Color(205, 102,	  0));
    put("darkorange4", new Color(139, 69,	  0));
    put("coral1", new Color(255, 114,	 86));
    put("coral2", new Color(238, 106,	 80));
    put("coral3", new Color(205, 91,	 69));
    put("coral4", new Color(139, 62,	 47));
    put("tomato1", new Color(255, 99,	 71));
    put("tomato2", new Color(238, 92,	 66));
    put("tomato3", new Color(205, 79,	 57));
    put("tomato4", new Color(139, 54,	 38));
    put("orangered1", new Color(255, 69,	  0));
    put("orangered2", new Color(238, 64,	  0));
    put("orangered3", new Color(205, 55,	  0));
    put("orangered4", new Color(139, 37,	  0));
    put("red1", new Color(255, 0,	  0));
    put("red2", new Color(238, 0,	  0));
    put("red3", new Color(205, 0,	  0));
    put("red4", new Color(139, 0,	  0));
    put("deeppink1", new Color(255, 20, 147));
    put("deeppink2", new Color(238, 18, 137));
    put("deeppink3", new Color(205, 16, 118));
    put("deeppink4", new Color(139, 10,	 80));
    put("hotpink1", new Color(255, 110, 180));
    put("hotpink2", new Color(238, 106, 167));
    put("hotpink3", new Color(205, 96, 144));
    put("hotpink4", new Color(139, 58, 98));
    put("pink1", new Color(255, 181, 197));
    put("pink2", new Color(238, 169, 184));
    put("pink3", new Color(205, 145, 158));
    put("pink4", new Color(139, 99, 108));
    put("lightpink1", new Color(255, 174, 185));
    put("lightpink2", new Color(238, 162, 173));
    put("lightpink3", new Color(205, 140, 149));
    put("lightpink4", new Color(139, 95, 101));
    put("palevioletred1", new Color(255, 130, 171));
    put("palevioletred2", new Color(238, 121, 159));
    put("palevioletred3", new Color(205, 104, 137));
    put("palevioletred4", new Color(139, 71,	 93));
    put("maroon1", new Color(255, 52, 179));
    put("maroon2", new Color(238, 48, 167));
    put("maroon3", new Color(205, 41, 144));
    put("maroon4", new Color(139, 28,	 98));
    put("violetred1", new Color(255, 62, 150));
    put("violetred2", new Color(238, 58, 140));
    put("violetred3", new Color(205, 50, 120));
    put("violetred4", new Color(139, 34,	 82));
    put("magenta1", new Color(255, 0, 255));
    put("magenta2", new Color(238, 0, 238));
    put("magenta3", new Color(205, 0, 205));
    put("magenta4", new Color(139, 0, 139));
    put("orchid1", new Color(255, 131, 250));
    put("orchid2", new Color(238, 122, 233));
    put("orchid3", new Color(205, 105, 201));
    put("orchid4", new Color(139, 71, 137));
    put("plum1", new Color(255, 187, 255));
    put("plum2", new Color(238, 174, 238));
    put("plum3", new Color(205, 150, 205));
    put("plum4", new Color(139, 102, 139));
    put("mediumorchid1", new Color(224, 102, 255));
    put("mediumorchid2", new Color(209, 95, 238));
    put("mediumorchid3", new Color(180, 82, 205));
    put("mediumorchid4", new Color(122, 55, 139));
    put("darkorchid1", new Color(191, 62, 255));
    put("darkorchid2", new Color(178, 58, 238));
    put("darkorchid3", new Color(154, 50, 205));
    put("darkorchid4", new Color(104, 34, 139));
    put("purple1", new Color(155, 48, 255));
    put("purple2", new Color(145, 44, 238));
    put("purple3", new Color(125, 38, 205));
    put("purple4", new Color(85, 26, 139));
    put("mediumpurple1", new Color(171, 130, 255));
    put("mediumpurple2", new Color(159, 121, 238));
    put("mediumpurple3", new Color(137, 104, 205));
    put("mediumpurple4", new Color(93, 71, 139));
    put("thistle1", new Color(255, 225, 255));
    put("thistle2", new Color(238, 210, 238));
    put("thistle3", new Color(205, 181, 205));
    put("thistle4", new Color(139, 123, 139));
    put("gray0", new Color( 0, 0, 0));
    put("grey0", new Color( 0, 0, 0));
    put("gray1", new Color( 3, 3, 3));
    put("grey1", new Color( 3, 3, 3));
    put("gray2", new Color( 5, 5, 5));
    put("grey2", new Color( 5, 5, 5));
    put("gray3", new Color( 8, 8, 8));
    put("grey3", new Color( 8, 8, 8));
    put("gray4", new Color(10, 10, 10));
    put("grey4", new Color(10, 10, 10));
    put("gray5", new Color(13, 13, 13));
    put("grey5", new Color(13, 13, 13));
    put("gray6", new Color(15, 15, 15));
    put("grey6", new Color(15, 15, 15));
    put("gray7", new Color(18, 18, 18));
    put("grey7", new Color(18, 18, 18));
    put("gray8", new Color(20, 20, 20));
    put("grey8", new Color(20, 20, 20));
    put("gray9", new Color(23, 23, 23));
    put("grey9", new Color(23, 23, 23));
    put("gray10", new Color(26, 26, 26));
    put("grey10", new Color(26, 26, 26));
    put("gray11", new Color(28, 28, 28));
    put("grey11", new Color(28, 28, 28));
    put("gray12", new Color(31, 31, 31));
    put("grey12", new Color(31, 31, 31));
    put("gray13", new Color(33, 33, 33));
    put("grey13", new Color(33, 33, 33));
    put("gray14", new Color(36, 36, 36));
    put("grey14", new Color(36, 36, 36));
    put("gray15", new Color(38, 38, 38));
    put("grey15", new Color(38, 38, 38));
    put("gray16", new Color(41, 41, 41));
    put("grey16", new Color(41, 41, 41));
    put("gray17", new Color(43, 43, 43));
    put("grey17", new Color(43, 43, 43));
    put("gray18", new Color(46, 46, 46));
    put("grey18", new Color(46, 46, 46));
    put("gray19", new Color(48, 48, 48));
    put("grey19", new Color(48, 48, 48));
    put("gray20", new Color(51, 51, 51));
    put("grey20", new Color(51, 51, 51));
    put("gray21", new Color(54, 54, 54));
    put("grey21", new Color(54, 54, 54));
    put("gray22", new Color(56, 56, 56));
    put("grey22", new Color(56, 56, 56));
    put("gray23", new Color(59, 59, 59));
    put("grey23", new Color(59, 59, 59));
    put("gray24", new Color(61, 61, 61));
    put("grey24", new Color(61, 61, 61));
    put("gray25", new Color(64, 64, 64));
    put("grey25", new Color(64, 64, 64));
    put("gray26", new Color(66, 66, 66));
    put("grey26", new Color(66, 66, 66));
    put("gray27", new Color(69, 69, 69));
    put("grey27", new Color(69, 69, 69));
    put("gray28", new Color(71, 71, 71));
    put("grey28", new Color(71, 71, 71));
    put("gray29", new Color(74, 74, 74));
    put("grey29", new Color(74, 74, 74));
    put("gray30", new Color(77, 77, 77));
    put("grey30", new Color(77, 77, 77));
    put("gray31", new Color(79, 79, 79));
    put("grey31", new Color(79, 79, 79));
    put("gray32", new Color(82, 82, 82));
    put("grey32", new Color(82, 82, 82));
    put("gray33", new Color(84, 84, 84));
    put("grey33", new Color(84, 84, 84));
    put("gray34", new Color(87, 87, 87));
    put("grey34", new Color(87, 87, 87));
    put("gray35", new Color(89, 89, 89));
    put("grey35", new Color(89, 89, 89));
    put("gray36", new Color(92, 92, 92));
    put("grey36", new Color(92, 92, 92));
    put("gray37", new Color(94, 94, 94));
    put("grey37", new Color(94, 94, 94));
    put("gray38", new Color(97, 97, 97));
    put("grey38", new Color(97, 97, 97));
    put("gray39", new Color(99, 99, 99));
    put("grey39", new Color(99, 99, 99));
    put("gray40", new Color(102, 102, 102));
    put("grey40", new Color(102, 102, 102));
    put("gray41", new Color(105, 105, 105));
    put("grey41", new Color(105, 105, 105));
    put("gray42", new Color(107, 107, 107));
    put("grey42", new Color(107, 107, 107));
    put("gray43", new Color(110, 110, 110));
    put("grey43", new Color(110, 110, 110));
    put("gray44", new Color(112, 112, 112));
    put("grey44", new Color(112, 112, 112));
    put("gray45", new Color(115, 115, 115));
    put("grey45", new Color(115, 115, 115));
    put("gray46", new Color(117, 117, 117));
    put("grey46", new Color(117, 117, 117));
    put("gray47", new Color(120, 120, 120));
    put("grey47", new Color(120, 120, 120));
    put("gray48", new Color(122, 122, 122));
    put("grey48", new Color(122, 122, 122));
    put("gray49", new Color(125, 125, 125));
    put("grey49", new Color(125, 125, 125));
    put("gray50", new Color(127, 127, 127));
    put("grey50", new Color(127, 127, 127));
    put("gray51", new Color(130, 130, 130));
    put("grey51", new Color(130, 130, 130));
    put("gray52", new Color(133, 133, 133));
    put("grey52", new Color(133, 133, 133));
    put("gray53", new Color(135, 135, 135));
    put("grey53", new Color(135, 135, 135));
    put("gray54", new Color(138, 138, 138));
    put("grey54", new Color(138, 138, 138));
    put("gray55", new Color(140, 140, 140));
    put("grey55", new Color(140, 140, 140));
    put("gray56", new Color(143, 143, 143));
    put("grey56", new Color(143, 143, 143));
    put("gray57", new Color(145, 145, 145));
    put("grey57", new Color(145, 145, 145));
    put("gray58", new Color(148, 148, 148));
    put("grey58", new Color(148, 148, 148));
    put("gray59", new Color(150, 150, 150));
    put("grey59", new Color(150, 150, 150));
    put("gray60", new Color(153, 153, 153));
    put("grey60", new Color(153, 153, 153));
    put("gray61", new Color(156, 156, 156));
    put("grey61", new Color(156, 156, 156));
    put("gray62", new Color(158, 158, 158));
    put("grey62", new Color(158, 158, 158));
    put("gray63", new Color(161, 161, 161));
    put("grey63", new Color(161, 161, 161));
    put("gray64", new Color(163, 163, 163));
    put("grey64", new Color(163, 163, 163));
    put("gray65", new Color(166, 166, 166));
    put("grey65", new Color(166, 166, 166));
    put("gray66", new Color(168, 168, 168));
    put("grey66", new Color(168, 168, 168));
    put("gray67", new Color(171, 171, 171));
    put("grey67", new Color(171, 171, 171));
    put("gray68", new Color(173, 173, 173));
    put("grey68", new Color(173, 173, 173));
    put("gray69", new Color(176, 176, 176));
    put("grey69", new Color(176, 176, 176));
    put("gray70", new Color(179, 179, 179));
    put("grey70", new Color(179, 179, 179));
    put("gray71", new Color(181, 181, 181));
    put("grey71", new Color(181, 181, 181));
    put("gray72", new Color(184, 184, 184));
    put("grey72", new Color(184, 184, 184));
    put("gray73", new Color(186, 186, 186));
    put("grey73", new Color(186, 186, 186));
    put("gray74", new Color(189, 189, 189));
    put("grey74", new Color(189, 189, 189));
    put("gray75", new Color(191, 191, 191));
    put("grey75", new Color(191, 191, 191));
    put("gray76", new Color(194, 194, 194));
    put("grey76", new Color(194, 194, 194));
    put("gray77", new Color(196, 196, 196));
    put("grey77", new Color(196, 196, 196));
    put("gray78", new Color(199, 199, 199));
    put("grey78", new Color(199, 199, 199));
    put("gray79", new Color(201, 201, 201));
    put("grey79", new Color(201, 201, 201));
    put("gray80", new Color(204, 204, 204));
    put("grey80", new Color(204, 204, 204));
    put("gray81", new Color(207, 207, 207));
    put("grey81", new Color(207, 207, 207));
    put("gray82", new Color(209, 209, 209));
    put("grey82", new Color(209, 209, 209));
    put("gray83", new Color(212, 212, 212));
    put("grey83", new Color(212, 212, 212));
    put("gray84", new Color(214, 214, 214));
    put("grey84", new Color(214, 214, 214));
    put("gray85", new Color(217, 217, 217));
    put("grey85", new Color(217, 217, 217));
    put("gray86", new Color(219, 219, 219));
    put("grey86", new Color(219, 219, 219));
    put("gray87", new Color(222, 222, 222));
    put("grey87", new Color(222, 222, 222));
    put("gray88", new Color(224, 224, 224));
    put("grey88", new Color(224, 224, 224));
    put("gray89", new Color(227, 227, 227));
    put("grey89", new Color(227, 227, 227));
    put("gray90", new Color(229, 229, 229));
    put("grey90", new Color(229, 229, 229));
    put("gray91", new Color(232, 232, 232));
    put("grey91", new Color(232, 232, 232));
    put("gray92", new Color(235, 235, 235));
    put("grey92", new Color(235, 235, 235));
    put("gray93", new Color(237, 237, 237));
    put("grey93", new Color(237, 237, 237));
    put("gray94", new Color(240, 240, 240));
    put("grey94", new Color(240, 240, 240));
    put("gray95", new Color(242, 242, 242));
    put("grey95", new Color(242, 242, 242));
    put("gray96", new Color(245, 245, 245));
    put("grey96", new Color(245, 245, 245));
    put("gray97", new Color(247, 247, 247));
    put("grey97", new Color(247, 247, 247));
    put("gray98", new Color(250, 250, 250));
    put("grey98", new Color(250, 250, 250));
    put("gray99", new Color(252, 252, 252));
    put("grey99", new Color(252, 252, 252));
    put("gray100", new Color(255, 255, 255));
    put("grey100", new Color(255, 255, 255));
    put("dark grey", new Color(169, 169, 169));
    put("dark gray", new Color(169, 169, 169));
    put("dark blue", new Color(0, 0, 139));
    put("dark cyan", new Color(0, 139, 139));
    put("dark magenta", new Color(139, 0, 139));
    put("dark red", new Color(139, 0, 0));
    put("light green", new Color(144, 238, 144));
    
  }};
