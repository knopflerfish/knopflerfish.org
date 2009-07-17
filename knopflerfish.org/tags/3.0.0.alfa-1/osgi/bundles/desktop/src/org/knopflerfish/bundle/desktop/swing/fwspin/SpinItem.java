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

import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;


public abstract class SpinItem {
  double x = 0;
  double y = 0;
  int    sx;
  int    sy;
  double fac = 1.0;
  double angle = 0;

  Color textColor = Color.gray.brighter().brighter().brighter().brighter();

  public void   setPos(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public void   setSPos(int sx, int sy, double fac) {
    this.sx = sx;
    this.sy = sy;
    this.fac = fac;
  }

  public void setAngle(double a) { this.angle = a; };

  public double getAngle() { return angle; };

  public double dist2(int x0, int y0) {
    double dx = sx - x0;
    double dy = sy - y0;
    
    return dx * dx + dy * dy;
  }

  public double getX() {
    return x;
  }
  public double getY() {
    return y;
  }

  public double getSX() {
    return sx;
  }
  public double getSY() {
    return sy;
  }

  abstract public void paint(Graphics g);

  abstract public void paintDependencies(Graphics g);

  abstract public void paintInfo(Graphics g, double x, double y);

  abstract public boolean isActive();

  /**
   * Draw cubic spline using de Casteljau's method.
   */
  static public void drawSpline(Graphics g, 
				double p1_x, double p1_y,
				double p2_x, double p2_y,
				double p3_x, double p3_y,
				double p4_x, double p4_y,
				int depth) {
    
    if(depth > 0) {
      double l1_x = (p1_x + p2_x) / 2;
      double l1_y = (p1_y + p2_y) / 2;

      double m_x = (p2_x + p3_x) / 2;
      double m_y = (p2_y + p3_y) / 2;

      double l2_x = (l1_x + m_x) / 2;
      double l2_y = (l1_y + m_y) / 2;

      double r1_x = (p3_x + p4_x) / 2;
      double r1_y = (p3_y + p4_y) / 2;

      double r2_x = (r1_x + m_x) / 2;
      double r2_y = (r1_y + m_y) / 2;

      double m2_x = (l2_x + r2_x) / 2;
      double m2_y = (l2_y + r2_y) / 2;

      drawSpline(g, 
		 p1_x, p1_y,
		 l1_x, l1_y,
		 l2_x, l2_y,
		 m2_x, m2_y,
		 depth - 1);

      drawSpline(g, 
		 m2_x, m2_y,
		 r2_x, r2_y,
		 r1_x, r1_y,
		 p4_x, p4_y,
		 depth - 1);

    } else {
      g.drawLine((int)p1_x, (int)p1_y, (int)p4_x, (int)p4_y);
    }
  }

  public static final int DIR_FROM = 1;
  public static final int DIR_TO   = 2;

  Vector getNext(int dir) { return null; };
}
