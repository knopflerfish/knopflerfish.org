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

package org.knopflerfish.bundle.desktop.swing.fwspin;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Vector;



/**
 * @author Erik Wistrand
 */
public class Console {

  Spin spin;

  int x;
  int y;
  int width;
  int height;

  Vector lines = new Vector();

  public Console(Spin spin) {
    super();
    this.spin = spin;
  }

  public void setBounds(int x, int y, int w, int h) {
    this.x = x;
    this.y = y;
    this.width  = w;
    this.height = h;
  }

  public void repaint(Graphics g) {
    paint(g);
  }

  public void addLine(String line) {
    lines.addElement(line);
  }
  
  public void paint(Graphics g) {
    Object oldComp = null;
    if(spin.use2D) {
      Graphics2D g2 = (Graphics2D)g;
      oldComp = g2.getComposite();
      g2.setComposite((AlphaComposite)spin.alphaHalf);
    }


    paintBox(lines, g, Color.white, Color.black, x, y, 1.0, width, height);
    
    if(spin.use2D) {
      if(oldComp != null) {
	Graphics2D g2 = (Graphics2D)g;
	g2.setComposite((AlphaComposite)oldComp);
      }
    }
  }
  
  
  void paintBox(Vector lines,
		Graphics g, 
		Color    bgCol, 
		Color    fgCol,
		int      x, 
		int      y,
		double   size,
		int width, int height) {
    
    int maxCols = 0;
    for(int i = 0; i <lines.size(); i++) {
      String line = (String)lines.elementAt(i);
      if(line.length() > maxCols) {
	maxCols = line.length();
      }
    }
    
    Font font = spin.getFont(size);

    g.setColor(bgCol);
    
    g.fill3DRect(x, y, 
		 Math.max(width, font.getSize() * maxCols / 2 + 30), 
		 Math.max(height, (font.getSize() + 3) * lines.size() + 10), 
		 true);


    g.setFont(font);
    
    g.setColor(fgCol);

    x += 10;
    y += font.getSize() + 5;

    int x2 = x + font.getSize() * 8;

    for(int i = 0; i <lines.size(); i++) {
      String line = (String)lines.elementAt(i);
      int ix = line.indexOf("\t");
      if(ix != -1) {
	g.drawString(line.substring(0, ix),  x, y);
	g.drawString(line.substring(ix + 1), x+font.getSize()*4, y);
      } else {
	g.drawString(line, x, y);
      }
      y += font.getSize() + 3;
    }
     
  }
}
