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

package org.knopflerfish.bundle.desktopawt;

import java.awt.*;

public class LF {
  
  public Font defaultFont = new Font("Dialog", Font.PLAIN, 11);
  public Font smallFont   = new Font("Dialog", Font.PLAIN, 10);

  public Font defaultFixedFont = new Font("Monospaced", Font.PLAIN, 11);
  public Font smallFixedFont   = new Font("Monospaced", Font.PLAIN, 10);

  Color bgColor     = new Color(240, 240, 240);
  Color textColor   = new Color(0,0,0);
  //  Color hiliteColor = new Color(240, 240, 240);

  Color stdHiliteCol1  = new Color(140, 140, 200);
  Color stdHiliteCol2  = new Color(255, 255, 255);
  Color stdSelectedCol = new Color(255, 200, 120);
  
  Color ttBg     = new Color(255, 255, 200);
  Color ttText   = new Color(0,0,0);
  Color ttBorder = new Color(120,120,120);

  int imgHSpace = 5;

  static LF lf = new LF();

  static public LF getLF() {
    return lf;
  }


  public void paintButton(Graphics g, 
			  Dimension size,
			  Color bgColor,
			  Color hiliteCol1,
			  Color hiliteCol2,
			  Color selectedCol,
			  boolean bSelected) {
    Color loColor  = Util.rgbInterpolate(bgColor, hiliteCol1, .4);
    Color hiColor  = Util.rgbInterpolate(bgColor, hiliteCol2, .5);
    
    Color medColor = Util.rgbInterpolate(loColor, hiColor, .5);

    int w = size.width - 1;
    int h = size.height - 1;

    int bH = 5;

    int tH = h - bH;
    for(int i = 0; i <= tH; i++) {
      double k = (double)i / (double)tH - .2;
      Color c = Util.rgbInterpolate(hiColor, medColor, k);
      g.setColor(c);
      g.drawLine(1, i, w-1, i);
    }

    for(int i = 0; i <= bH; i++) {
      double k = (double)i / (double)bH;
      Color c = Util.rgbInterpolate(loColor, medColor, k);
      g.setColor(c);
      int x = i == 0 ? 3 : (i == 1 ? 2 : 1);

      g.drawLine(x, h - i, w-x, h - i);
    }

    if(bSelected) {
      Color sel1 = Util.rgbInterpolate(bgColor, selectedCol, .2);
      Color sel2 = Util.rgbInterpolate(bgColor, selectedCol, .5);
      Color sel3 = Util.rgbInterpolate(bgColor, selectedCol, .7);

      int i;
      for(i = 1; i <= 3; i++) {
	g.setColor(sel3);
	
	g.drawRect(i, i, w-i*2,h-i*2);
      }
      pixel(g, sel2, i, i);
      pixel(g, sel2, w-i, i);
      pixel(g, sel2, w-i, h-i);
      pixel(g, sel2, i, h-i);
    }
    
    // left
    g.setColor(loColor);
    g.drawLine(0, 3, 0, h - 3);

    // top
    g.setColor(loColor);
    g.drawLine(3, 0, w-3, 0);

    // right
    g.setColor(loColor);
    g.drawLine(w, 3, w, h - 3);


    Color fade1 = Util.rgbInterpolate(bgColor, loColor, .6);
    Color fade2 = Util.rgbInterpolate(bgColor, loColor, .4);
    Color fade3 = Util.rgbInterpolate(bgColor, loColor, .2);

    pixel(g, fade1,   1, 2);
    pixel(g, fade1,   2, 1);
    pixel(g, fade1,   3, 0);
    pixel(g, fade1,   0, 3);
    pixel(g, fade3,   1, 1);

    pixel(g, fade1,   w-1, 2);
    pixel(g, fade1,   w-2, 1);
    pixel(g, fade1,   w-3, 0);
    pixel(g, fade1,   w-0, 3);
    pixel(g, fade3,   w-1, 1);

    pixel(g, fade1,   1, h-2);
    pixel(g, fade1,   2, h-1);
    pixel(g, fade1,   3, h-0);
    pixel(g, fade1,   0, h-3);
    pixel(g, fade3,   1, h-1);


    pixel(g, fade1,   w-1, h-2);
    pixel(g, fade1,   w-2, h-1);
    pixel(g, fade1,   w-3, h-0);
    pixel(g, fade1,   w-0, h-3);
    pixel(g, fade3,   w-1, h-1);

  }

  public void paintLabel(Graphics g, 
			 Dimension size,
			 String s,
			 Image img,
			 int horizontalAlign,
			 int verticalAlign,
			 Color color,
			 boolean bEnabled) {

    try {
      Font font = g.getFont();
      FontMetrics fm = g.getFontMetrics();
      
      int sWidth = fm.stringWidth(s);
      
      if(img != null) {
	sWidth += img.getWidth(null) + imgHSpace;
      }
      
      g.setColor(color);
      
      int x = 4;
      int y = size.height / 2 + font.getSize() / 2;
      
      switch(horizontalAlign) {
      case HORIZONTAL_CENTER:
	x = size.width / 2 - sWidth / 2;
	break;
      case HORIZONTAL_LEFT:
	x = 4;
	break;
      case HORIZONTAL_RIGHT:
	x = size.width - sWidth - 4;
	break;
      }
      if(img != null) {
	
	int imgW = img.getWidth(null) + imgHSpace;
	int imgH = img.getHeight(null);
	
	g.drawImage(img, x, size.height / 2 - imgH/2, null);
	x += imgW;
      }
      g.drawString(s, x, y);
    } finally {
      //
    }
  }

  public void pixel(Graphics g, Color c, int x, int y) {
    g.setColor(c);
    g.drawLine(x, y, x, y);
  }

  public static final int HORIZONTAL_LEFT   = 1;
  public static final int HORIZONTAL_CENTER = 2;
  public static final int HORIZONTAL_RIGHT  = 3;

  public static final int VERTICAL_TOP     = 1;
  public static final int VERTICAL_CENTER  = 2;
  public static final int VERTICAL_RIGHT   = 3;


  

}
