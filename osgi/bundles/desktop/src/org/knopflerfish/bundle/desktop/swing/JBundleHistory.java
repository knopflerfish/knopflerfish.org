/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.knopflerfish.service.desktop.*;

public class JBundleHistory extends JPanel {

  BundleContext bc;

  BundleSelectionModel bundleSelModel;
  int nMax;


  GridLayout grid;
  JPanel panel;

  public JBundleHistory(BundleContext bc, 
                        JComponent eastComponent,
                        BundleSelectionModel bundleSelModel,
                        int nMax,
                        int height) {
    super(new BorderLayout());

    this.bc = bc;    
    this.nMax = nMax;
    this.bundleSelModel = bundleSelModel;

    grid = new GridLayout(0, 1, 3, 3);
    panel = new JPanel(grid);
    panel.setOpaque(false);

    add(panel, BorderLayout.WEST);
    if(eastComponent != null) {
      add(eastComponent, BorderLayout.EAST);
    }
    setPreferredSize(new Dimension(200, height));
  }

  
  Bundle lastBundle;
  public Bundle getLastBundle() {
    return lastBundle;
  }

  public void addBundle(Bundle b) {
    lastBundle = b;
    Component[] cl = panel.getComponents();
    if(cl.length > 0) {
      if(((JBundle)cl[cl.length-1]).b.equals(b)) {
        return;
      }
    }
    JBundle jb = new JBundle(b);
    jb.setBackground(getBackground());
    int n = panel.getComponentCount();
    if(n > nMax) {
      panel.remove(0);
    }
    panel.add(jb);
    resizeBundles();
  }

  public boolean removeBundle(Bundle b) {
    Component[] cl = panel.getComponents();
    for(int i = 0; i < cl.length; i++) {
      JBundle jb = (JBundle)cl[i];
      if(b.equals(jb.b)) {        
        panel.remove(cl[i]);
        cl = panel.getComponents();
        if(cl.length > 0) {
          lastBundle = ((JBundle)cl[cl.length-1]).b;
        } else {
          lastBundle = null;
        }
        resizeBundles();
        return true;
      }
    }
    return false;
  }

  public void setBounds(int x, int y, int w, int h) {
    super.setBounds(x, y, w, h);
    
    resizeBundles();
  }            

  void resizeBundles() {
    Dimension size = getSize();
    Component[] cl = panel.getComponents();
    int n = cl.length;
    if(n > 0) {
      for(int i = 0; i < n; i++) {
        JComponent jc = (JComponent)cl[i];
        int w = Math.max(20, Math.min(size.height, size.width/(n+2)));
        Dimension s = new Dimension(w, w);
        jc.setPreferredSize(s);
      }
      grid.setColumns(n);
    }
    
    revalidate();
    repaint();
  }

  void bundleClicked(Bundle b) {
  }

  void bundleSelected(Bundle b) {
  }

  AlphaComposite alphaLow 
    = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .3f);

  AlphaComposite alphaHigh
    = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .7f);
  


  class JBundle extends JPanel {
    Icon icon;
    Bundle b;
    AlphaComposite alpha = alphaLow;

    JBundle(Bundle _b) {
      super();
      this.b = _b;
      this.icon = Util.getBundleIcon(b);
      setToolTipText("#" + b.getBundleId() + " " + Util.getBundleName(b));

      int h = JBundleHistory.this.getSize().height;
      Dimension s = new Dimension(h, h);
      setPreferredSize(s);
      // setMinimumSize(new Dimension(20, 20));
      // setMaximumSize(s);

      addMouseListener(new MouseListener() {
          public void mouseEntered(MouseEvent ev) {
            alpha = alphaHigh;
            repaint();
          }

          public void mouseExited(MouseEvent ev) {
            alpha = alphaLow;
            repaint();
          }
          public void 	mousePressed(MouseEvent e) {
          }
          public void 	mouseReleased(MouseEvent e) {
          }

          public void mouseClicked(MouseEvent ev) {
            if(ev.getClickCount() >= 2) {
              bundleClicked(b);
            } else {
              bundleSelected(b);
            }
          }
        });

    }

    public void paintComponent(Graphics _g) {
      Graphics2D g = (Graphics2D)_g;
      Dimension size = getSize();


      g.setColor(getBackground());
      g.fillRect(0, 0, size.width, size.height);

      int width  = Math.min(size.width, size.height);
      int height = width; 
      
      double fx = (double)width / icon.getIconWidth();
      double fy = (double)height / icon.getIconHeight();
      
      AffineTransform oldTrans = g.getTransform();
      g.scale(fx, fy);

      Composite oldComp = g.getComposite();
      g.setComposite(alpha);
      Util.setAntialias(g, true);
      icon.paintIcon(this, g, 0, 0);

      g.setTransform(oldTrans);

      g.setComposite(oldComp);

      Util.setAntialias(g, true);
      String s = "#" + b.getBundleId();
      

      g.setColor(Color.black);
      g.drawString(s, 2, g.getFont().getSize() + 2);

      g.setColor(Color.white);
      g.drawString(s, 3, g.getFont().getSize() + 3);

      Util.setAntialias(g, false);

    }
  }
}


