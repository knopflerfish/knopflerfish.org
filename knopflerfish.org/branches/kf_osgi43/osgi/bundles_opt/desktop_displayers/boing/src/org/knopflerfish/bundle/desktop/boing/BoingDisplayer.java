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

import org.osgi.framework.*;
import java.util.*;
import org.knopflerfish.service.desktop.*;

import java.awt.*;
import javax.swing.*;

public class BoingDisplayer extends DefaultSwingBundleDisplayer {

  boolean bClear = true;
  boolean bLabel = true;

  int w = 80;
  int h = 80;

  String txt = "";

  Icon smallIcon;

  public BoingDisplayer(BundleContext bc, boolean bDetail) {
    super(bc, "Boing", "We all go boing", bDetail);
    bUseListeners = false;

    smallIcon = 
      new ImageIcon(getClass().getResource("/boing22x22.png"));
  }

  public JComponent newJComponent() {
    return new JBoing();
  }

  public void  disposeJComponent(JComponent comp) {
    JBoing boing = (JBoing)comp;
    boing.boing.stop();

    super.disposeJComponent(comp);
  }

  void closeComponent(JComponent comp) {
    JBoing boing = (JBoing)comp;
    boing.boing.stop();    
  }

  public void valueChanged(long bid) {
    super.valueChanged(bid);

    Bundle[] bl = bc.getBundles();
    
    for(int i = 0; i < bl.length; i++) {
      if(bundleSelModel.isSelected(bl[i].getBundleId())) {
	txt = bl[i].getLocation();
      }
    }
  }

  public Icon getSmallIcon() {
    return smallIcon;
  }

  class JBoing extends JPanel {
    Boing boing;
    
    double x = 0; 
    double y = 0;
    double dx = 2.1;
    double dy = 1;
    double grav = 0.2;

    Random rand = new Random();


    public JBoing() {
      setLayout(null);

      boing = new Boing(w, h) {
	  public void roll() {

	    try {
	      Dimension size = JBoing.this.getSize();

	      dy += grav;
	      dx += (rand.nextDouble() - .5) / 20.0;

	      x += dx;
	      y += dy;
	      
	      if(x >= size.width - w)  { 
		x = size.width - w;
		dx = -dx;
	      }
	      if(y >= size.height - h) { 
		y = size.height - h; 
		dy = -dy; 
	      }
	      
	      if(x <= 0) { x = 0;  dx = -dx; }
	      if(y <= 0) { y = 0;  dy = -dy; }
	      
	      
	      //	      JBoing.this.invalidate();
	      JBoing.this.repaint();
	      super.roll();
	    } catch (Exception e) {
	      //
	    }
	  }
	};
      
      boing.start();
    }

    public void paint(Graphics g) {

      makeOff(this, getSize(), false);
      
      Dimension size = getSize();

      if(offG != null) {
	if(bClear) {
	  offG.setColor(getBackground());
	  offG.fillRect(0, 0, width, height);
	}
	try {
	  boing.paintBoing(this, offG, (int)x, (int)y);
	} catch (Exception ignored) {
	}
	if(bLabel) {
	  offG.setColor(Color.black);
	  offG.drawString(txt, (int)x, (int)y - 12);
	}

	g.drawImage(offImage, 0, 0, null);
      } else {
	System.out.println("No offg");
      }
    }

    boolean doAntiAlias = true;

    public Object AntiAlias  = RenderingHints.VALUE_ANTIALIAS_ON;
    public Object Rendering  = RenderingHints.VALUE_RENDER_SPEED;
    
    
    Image offImage = null;
    int width = 0;
    int height = 0;

    Graphics2D offG = null;

    public void makeOff(Component c, Dimension d, boolean bForce) {
      if (bForce || offG == null || d.width != width || d.height != height) {
	
	if(d.width > 0 && d.height > 0) {
	  width    = d.width;
	  height   = d.height;
	  try {
	    offImage = c.createImage(width, height);
	    offG     = (Graphics2D)offImage.getGraphics();	
	    //	    setRotation(rotation);
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
  }
}
