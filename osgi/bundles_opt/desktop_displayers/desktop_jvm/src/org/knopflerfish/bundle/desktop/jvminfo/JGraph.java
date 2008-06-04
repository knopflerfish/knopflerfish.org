/*
 * Copyright (c) 2004-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.jvminfo;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class JGraph extends JPanel {
  Color bgColor   = new Color(255, 255, 255);
  Color gridColor = new Color(220, 220, 220);
  Color lineColor = new Color(0,   0,   0);
  Color textColor = new Color(100, 100, 100);

  long xMin  = 0;
  long xMax  = 100;
  long xGrid = 10;

  long yMin = 0;
  long yMax = 8 * 1024 * 1024;
  long yGrid = 1024 * 1024;

  long[] values = new long[100];
  int pos   = 0;
  int start = 0;


  public JGraph(String title, int size, long yMin, long yMax, long yGrid) {
    super(new BorderLayout());
    Border border = BorderFactory.createTitledBorder
      (BorderFactory.createEtchedBorder(), title);
    setBorder(border);

    init(size, yMin, yMax, yGrid);
    Inner inner = new Inner();
    add(inner, BorderLayout.CENTER);
  }

  public void init(int size, long yMin, long yMax, long yGrid) {
    this.values = new long[size];
    this.pos = 0;
    this.start = 0;
    this.yMin  = yMin;
    this.yMax  = yMax;
    this.yGrid = yGrid;
  }

  synchronized public void addValue(long value) {
    values[pos % values.length] = value;
    pos++;
    if(pos >= values.length) {
      start++;
    }
    if(value > yMax) {
      yMax = (long)(value * 1.1);
    }
  }
  /*
    public void xxpaint(Graphics g) {
    repaint(g);
    }
  */

  class Inner extends JPanel {
    public void paintComponent(Graphics g) {

      Dimension size = getSize();
      if(size == null) {
        return;
      }

      paintBg(g);
      paintGrid(g);

      g.setColor(lineColor);

      int lastX = -1;
      int lastY = -1;

      int max = Math.min(values.length - 1, pos);

      for(int i = 0; i < max; i++) {
        long y = values[(start + i) % values.length];
        int sx = (int)((double)size.width * i / values.length);
        int sy = size.height - 1 - (int)((y - yMin) * size.height / (yMax - yMin));

        if(lastX != -1) {
          g.drawLine(lastX, lastY, sx, sy);
        }
        lastX = sx;
        lastY = sy;
      }
    }

    void paintBg(Graphics g) {
      Dimension size = getSize();
      g.setColor(bgColor);

      g.fillRect(0, 0, size.width, size.height);
    }

    void paintGrid(Graphics g) {
      Dimension size = getSize();
      g.setColor(gridColor);

      /*
        for(long x = xMin; x <= xMax; x += xGrid) {
        int sx = (int)((x - xMin) * size.width / (xMax - xMin));

        g.drawLine(sx, 0, sx, size.height);
        }
      */

      for(long y = yMin; y <= yMax; y += yGrid) {
        int sy = size.height - 1 - (int)((y - yMin) * size.height / (yMax - yMin));

        g.drawLine(0, sy, size.width, sy);
        if(y + yGrid < yMax) {
          g.drawString(getYLabel(y), 0, sy);
        } else {
          g.drawString(getYLabel(y) + " " + getYUnit(), 0, sy);
        }
      }
    }
  }

  String getYUnit() {
    return "";
  }

  String getYLabel(long y) {
    return Long.toString(y);
  }

  String getXUnit() {
    return "";
  }

  String getXLabel(long x) {
    return Long.toString(x);
  }
}
