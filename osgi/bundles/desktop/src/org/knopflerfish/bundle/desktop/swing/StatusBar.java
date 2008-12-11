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

package org.knopflerfish.bundle.desktop.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;


public class StatusBar extends JComponent 
  implements Runnable, MouseListener
{

  static int MODE_UNKNOWN    = 0;
  static int MODE_PERCENTAGE = 1;
    
  int        perc      = 0;
  int        delta     = 5;  
  String     msg       = "";
  int        block     = 6;
  int        pad       = 2;
  int        barWidth  = 150;
  boolean    bShowPerc = false;
  Thread     runner    = null;
  int        delay     = 50;
  int        mode      = MODE_UNKNOWN;

  boolean    bIsBroken = false; // Set to true if user clicks in progress bar
  boolean    bRun      = false; // run() will only loop while this is set

  Color bgColor  = Color.lightGray;
  Color fgColor  = Color.black;
  Color txtColor = Color.black;
  
  Color lowColor  = Color.black;
  Color highColor = Color.blue;

  String name;

  boolean bIsAlive = false;

  public StatusBar(String name) {
    this.name = name;
    msg = "";
    Dimension d = new Dimension(400, 17);

    setMinimumSize(d);
    setPreferredSize(d);

    addMouseListener(this); 

    bgColor = getBackground();

    highColor = UIManager.getColor("ScrollBar.thumb");
  }

  public void setBackground(Color c) {
    super.setBackground(c);
    bgColor = c;
  }

  public void setForeground(Color c) {
    super.setForeground(c);
    fgColor = c;
  }

  protected void setUI(ComponentUI newUI) {
    super.setUI(newUI);
    highColor = UIManager.getColor("ScrollBar.thumb");
  }

  public void run() {
    bIsAlive = true;
    while(bRun && !isBroken()) {
      updateProgress(-1);
      try {
	Thread.sleep(delay);
      } catch (Exception e) { }
    }
    bIsAlive = false;
    runner = null;
  }

  Object lock = new Object();

  private void stopRunner() {
    //    synchronized(lock) 
    {
      bRun = false;
      if(runner != null) {
	try {
	  runner.join(20 * 1000);
	} catch (Exception ignored) { }
      }
    }
  }
    
  public void updateProgress(int percent) {
    //    System.out.print("updateProgress " + msg + " " + percent + "%");
    if(percent == -1) {
      mode = MODE_UNKNOWN;
      bShowPerc = false;
      block     = 2;
      pad       = 1;
      perc     += delta;
      if(perc > 100) {
	perc = 100;
	delta = -delta;
      }
      if(perc < 0) {
	perc = 0;
	delta = -delta;
      }
    } else {
      perc = Math.min(Math.max(percent, 0), 100);
      mode = MODE_PERCENTAGE;
    }
    Graphics g = getGraphics();
    if(g != null) {
      paint(g);
      g.dispose();
    } else {
    }
  }
  
  public boolean isBroken() {
    return bIsBroken;
  }
  
  public void startProgress(String msg, int delay) {

    if(runner != null && mode == MODE_UNKNOWN) {
      this.msg = msg;
      return;
    }

    this.delta = 5;
    this.perc  = 0;
    this.msg   = msg;
    this.delay = delay;
    
    if(runner == null) {
      //      synchronized(lock) 
	{	  
	  bIsBroken = false;
	  bRun      = true;
	  runner= new Thread(this, "StatusBar update " + name);
	  runner.start();
	}
    }
  }
  
  public void startProgress(String msg) {
    if(runner != null && mode == MODE_UNKNOWN) {
      this.msg = msg;
      return;
    }
    
    this.delta = 5;
    this.perc  = 0;
    this.msg   = msg;
    
    repaint();
    
    setCursor(Cursor.WAIT_CURSOR); 
  }

  public void stopProgress() {
    stopRunner();
    
    updateProgress(0);
    perc = 0;
    msg  = "";
    repaint();
    
    setCursor(Cursor.DEFAULT_CURSOR);
  }

  
  public void showStatus(String msg) {
    this.msg = msg;
    Graphics g = getGraphics();
    if(g != null) {
      paint(g);
      g.dispose();
    } else {
      //      No graphics in showStatus
    }
  }
  
  public void update(Graphics g) {
    // Override this method, we do not need any background handling
    paint(g);
  }

  
  public void paint(Graphics g) {
    highColor = UIManager.getColor("ScrollBar.thumb");
    if(highColor == null && highColor.equals(getBackground())) {
      highColor = UIManager.getColor("controlShadow");
    }
    // Canvas size
    Dimension d = getSize();
    if (d.width==0||d.height==0) return; //Called before added to visible frame
    
    // Center
    Dimension center = new Dimension(d.width/2, d.height/2);
    
    // Create memory image, for double buffering
    Image memImage = createImage(d.width, d.height);
    if (memImage==null) return; //Called before added to visible frame
    Graphics memG  = memImage.getGraphics();
    
    // Set background
    memG.setColor(getBackground());
    memG.fillRect(0,0,d.width,d.height);

    memG.setColor(txtColor);
    
    String s = msg;
    if(bShowPerc) {
      s = s + " " + perc + "%";
    }
    Shape clip = memG.getClip();
    memG.setClip(0, 0, d.width-barWidth-12, d.height - 1);
    memG.drawString(s, 5, 14);
    memG.setClip(clip);

    memG.setColor(getBackground());
    memG.draw3DRect(0,0,d.width-barWidth-12,d.height-1, false);
    memG.draw3DRect(d.width-barWidth-10,0,barWidth+9,d.height-1, false);
    
    int x0 = d.width - barWidth - 5;
    int x1 = d.width - 5;
    int diff  = x1 - x0;
    
    int xmax = x0 + diff * perc / 100;

    int h = d.height;
    
    if(mode == MODE_PERCENTAGE) {
      for(int x = x0; x < xmax; x = x + block + pad) {
	double k = (x - x0) / (double)diff;
	Color c = Util.rgbInterpolate(lowColor, highColor, k);
	memG.setColor(c);
	
	memG.fillRect(x, 3, block, h - 6);
      }
    } else {
      int trail = diff / 3;
      if(delta > 0) {
	int xstart = xmax - trail;
	if(xstart < x0) xstart = x0;
	for(int x = xstart; x < xmax; x++) {
	  double k = (x - xstart) / (double)trail;
	  Color c = Util.rgbInterpolate(getBackground(), highColor, k);
	  memG.setColor(c);
	  memG.fillRect(x, 3, 1, h - 6);
	}
      } else if(delta < 0) {
	int xend = xmax + trail;
	if(xend > x1) xend = x1;
	for(int x = xend; x > xmax; x--) {
	  double k = (xend - x) / (double)trail;
	  Color c = Util.rgbInterpolate(getBackground(), highColor, k);
	  memG.setColor(c);
	  memG.fillRect(x, 3, 1, h - 6);
	}
      }
    }
    
    // Copy image to canvas
    g.drawImage(memImage, 0,0, this);
  }
  

  public void mouseClicked(MouseEvent e) {
  } 
  
  public void mouseEntered(MouseEvent e) {
  }
  public void mouseExited(MouseEvent e) {
  }
  
  public void mousePressed(MouseEvent e) {
    bIsBroken = true;
    showStatus("");
  }
  
  public void mouseReleased(MouseEvent e) {
  }

  void setCursor(int c) {
    final Component root = SwingUtilities.getRoot(this);
    if (root!=null)
      root.setCursor(Cursor.getPredefinedCursor(c));
  }
}

