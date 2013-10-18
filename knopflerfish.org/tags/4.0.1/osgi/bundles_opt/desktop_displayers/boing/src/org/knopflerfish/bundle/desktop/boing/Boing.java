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

package org.knopflerfish.bundle.desktop.boing;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import java.util.*;


/**
 * Instanciate, add and start.
 */
public class Boing extends JComponent implements Icon, Runnable {

  int width  = -1;
  int height = -1;
  Graphics2D offG;
  Image      offImage;
  Thread     runner = null;
  boolean    bRun = false;
  int        delay = 50;
  int        count = 0;
  
  
  Color      color1;
  Color      color2;

  Point3D    light      = new Point3D(Math.PI / 2, 0.6, 0.0); // Spherical
  Point3D    deltaLight = new Point3D(0.00, 0.00, 0.0);       // light rotation
  Point3D    lightPos;  // calculated from light

  int divP = 8;   // Phi steps
  int divT = 8;   // Theta steps

  boolean doLight     = true;
  boolean doAntiAlias = true;
  boolean bRebuild    = false;

  boolean bCanDrag    = false;

  // Screen rotation in radians
  double rotation     = 15 * Math.PI / 180;

  AffineTransform transform = new AffineTransform();

  public Boing() {
    this(-1, -1);
  }

  public Boing(int w, int h) {
    super();

    color1 = new Color(255, 0, 0);
    color2 = new Color(255, 255, 255);

    if(w != -1 && h != -1) {
      this.width  = w;
      this.height = h;
      
      setPreferredSize(new Dimension(w, h));
    }
    setRotation(rotation);
    roll();
  }

  void setAntiAlias(boolean b) {
    doAntiAlias = b;
    makeOff(this, getSize(), true);
    repaint();
  }

  void setLight(boolean b) {
    doLight = b;
    repaint();
  }

  void setRotation(double rot) {
    rotation = rot;
    transform = new AffineTransform();
    transform.rotate(rot, width / 2, height/2);
    if(!bRun) repaint();
  }

  void setResolution(int x, int y) {
    divP = x;
    divT = y;
  }

  // Implements Icon
  public int getIconHeight() {
    return width;
  }
  
  // Implements Icon
  public int getIconWidth() {
    return height;
  }
  
  // Implements Icon
  public void paintBoing(Component c, Graphics _g, int x, int y) {
    Graphics2D g = (Graphics2D)_g;
    if(doAntiAlias) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AntiAlias);
    }
    paintBoing(g, 
	       x + width / 2,  y + height / 2, width / 2,
	       color1, color2);
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    makeOff(c, new Dimension(width, height), false);

    if(offG != null) {
      offG.setColor(Color.white);
      offG.fillRect(0,0,width, height);

      paintBoing(offG, 
		 width / 2,  height / 2, width / 2,
		 color1, color2);
      
      g.drawImage(offImage, x, y, Color.red, null);
    } else {
      System.err.println("No offG!");
    }
  } 

  public void update(Graphics g) {
    paint(g);
  }



  public void paint(Graphics g) {
    makeOff(this, getSize(), false);

    if(offImage == null) {
      return;
    }

    offG.setColor(Color.white);
    offG.fillRect(0,0,width, height);

    paintBoing(offG, 
	       width/2, height/2, Math.min(width, height)/2,
	       color1, color2);

    g.drawImage(offImage, 0, 0, Color.red, null);
  }

  Point3D[] pl = new Point3D[4];
  int [] xl    = new int[pl.length];
  int [] yl    = new int[pl.length];

  boolean isPainting = false;

  void paintBoing(Graphics2D g, 
		  int cx, int cy, int r,
		  Color c1, Color c2) {
    if(isPainting) {
      return;
    }

    AffineTransform oldTransform = g.getTransform();
    g.setTransform(transform);
    isPainting = true;

    double minP = 0;
    double maxP = Math.PI;
    
    double minT  = -Math.PI / 2;
    double maxT  =  Math.PI / 2;

    double dT = (maxT - minT) / divT;
    double dP = (maxP - minP) / divP;

    g.setColor(c1);

    for(int p = 0; p < divP; p++) {
      double phi = minP + (maxP - minP) * (double)p / divP;
      for(int t = -2; t < divT; t++) {
	double theta = minT + (maxT - minT) * (double)t / divT;

	double k = (double)(count % (divT * 2)) / (divT * 2);
	double t2 = theta + k * dT * 2;

	pl[0] = fromSphere(phi,    t2,    1.0);
	pl[1] = fromSphere(phi,    t2+dT, 1.0);
	pl[2] = fromSphere(phi+dP, t2+dT, 1.0);
	pl[3] = fromSphere(phi+dP, t2,    1.0);

	Color c = ((p+t) & 1) != 0 ? c1 : c2;
	fillPoly(g, cx, cy, r, pl, 0, 4, c);
      }
    }
    g.setTransform(oldTransform);

    isPainting = false;
  }

  void fillPoly(Graphics g, int cx, int cy, int r, Point3D[] pl, int offset, int n, Color c) {

    for(int i = 0; i < n; i++) {
      double x = r * pl[(i + offset) % pl.length].y;
      double y = r * pl[(i + offset) % pl.length].z;

      xl[i] = (int)(cx + x);
      yl[i] = (int)(cy + y);
    }
    
    if(doLight) {
      double l = Math.max(0, lightPos.dot(pl[0]));      
      c = interpolateRGB(Color.black, c, .5 + l / 2);
    }
    
    g.setColor(c);
    g.fillPolygon(xl, yl, n);
  }

  Point3D fromSphere(double phi, double theta, double radius) {

    theta = Math.max(-Math.PI / 2, Math.min(theta, Math.PI / 2));

    double sinPhi   = Math.sin(phi);
    double cosPhi   = Math.cos(phi);
    double sinTheta = Math.sin(theta);
    double cosTheta = Math.cos(theta);

    double x = radius * sinPhi * cosTheta;
    double y = radius * sinPhi * sinTheta;
    double z = radius * cosPhi;

    Point3D p = new Point3D(x, y, z);
    return p;
  }

  public void recalc() {
    count++;
    light.add(deltaLight);
    lightPos = fromSphere(light.x, light.y, 1.0);
  }


  public void roll() {
    recalc();
    if(false) {
      Graphics g = getGraphics();
      if(g != null) {
	paint(g);
	g.dispose();
      }
    }
  }

  public void stop() {
    if(runner == null) {
      return;
    }
    bRun = false;
    try {
      runner.join(1000);
    } catch (Exception e) {
	e.printStackTrace();
    }
    runner = null;
  }

  public void start() {
    if(runner != null) {
      return;
    }

    bRun   = true;
    runner = new Thread(this, "Boing update");
    runner.start();

    if(bCanDrag) {
      addMouseMotionListener(new MouseMotionAdapter() {
	  public void mouseDragged(MouseEvent e) {
	    int x = e.getX();
	    int y = e.getY();
	    
	    if(width > 0) {
	      setRotation(Math.PI * x / width);
	    }
	  }
	});
    }
  }

  public void run() {
    while(bRun) {
      try {
	roll();
	Thread.sleep(delay);
      } catch (Exception e) {
	e.printStackTrace();
      }
    }
  }

  public Object AntiAlias  = RenderingHints.VALUE_ANTIALIAS_ON;
  public Object Rendering  = RenderingHints.VALUE_RENDER_SPEED;

  public void makeOff(Component c, Dimension d, boolean bForce) {
    if (bForce || offG == null || d.width != width || d.height != height) {

      if(d.width > 0 && d.height > 0) {
	width    = d.width;
	height   = d.height;
	try {
	  offImage = c.createImage(width, height);
	  offG     = (Graphics2D)offImage.getGraphics();	
	  setRotation(rotation);
	  if(doAntiAlias) {
	    offG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, AntiAlias);
	    offG.setRenderingHint(RenderingHints.KEY_RENDERING, Rendering);
	  }

	} catch (Exception e) {
	  e.printStackTrace();
	}
      }
    }
  }

  void log(String s) {
    System.out.println(s);
  }

  static public Color interpolateRGB(Color c1, Color c2, double k) {

	double r1 = c1.getRed();
	double g1 = c1.getGreen();
	double b1 = c1.getBlue();

	double r2 = c2.getRed();
	double g2 = c2.getGreen();
	double b2 = c2.getBlue();
	
	double r = r1 + k * (r2 - r1);
	double g = g1 + k * (g2 - g1);
	double b = b1 + k * (b2 - b1);

	return new Color((int)r, (int)g, (int)b);
  }

}

class Point3D {
  double x, y, z;
  Point3D(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  double scalMul(Point3D p) {
    return x * p.x + y * p.y + z * p.z;
  }

  double dot(Point3D p) {
    return scalMul(p) / (length() * p.length());
  }

  double length() {
    return Math.sqrt(x * x + y * y + z * z);
  }

  void add(Point3D delta) {
    x += delta.x;
    y += delta.y;
    z += delta.z;
  }


}
