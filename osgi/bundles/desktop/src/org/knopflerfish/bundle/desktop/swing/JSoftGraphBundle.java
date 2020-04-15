/*
 * Copyright (c) 2003-2020, KNOPFLERFISH project
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
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.knopflerfish.bundle.desktop.swing.graph.BundleNode;
import org.knopflerfish.bundle.desktop.swing.graph.EmptyNode;
import org.knopflerfish.bundle.desktop.swing.graph.Node;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

public abstract class JSoftGraphBundle extends JSoftGraph {

  private static final long serialVersionUID = 1L;
  Bundle b;
  BundleContext bc;
  BundleSelectionModel bundleSelModel;
  GraphDisplayer.JMainBundles jmb;

  public JSoftGraphBundle(GraphDisplayer.JMainBundles jmb, BundleContext _bc, Bundle b, BundleSelectionModel bundleSelModel) {
    super();
    this.jmb = jmb;
    this.b = b;
    this.bc = _bc;
    this.bundleSelModel = bundleSelModel;

    open();
  }

  void open() {
    try {
      bc.addBundleListener(bundleListener);
      bc.addServiceListener(serviceListener);
    } catch (IllegalStateException ise) {
      // BundleContext no longer valid, nothing to do.
    }
  }

  public void keyPressed(KeyEvent ev) {
    if(currentNode instanceof BundleNode) {
      Bundle b = ((BundleNode)currentNode).getBundle();
      long id = b.getBundleId();
      boolean bUpdate = false;
      switch(ev.getKeyCode()) {
      case KeyEvent.VK_LEFT:
        id--;
        bUpdate = true;
        break;
      case KeyEvent.VK_RIGHT:
        id++;
        bUpdate = true;
        break;
      }

      if(bUpdate) {
        try {
          Bundle b2 =  bc.getBundle(id);
          Activator.desktop.setSelected(b2);
          jmb.setBundle(b2);
        } catch (Exception e) {
          Activator.log.warn("id=" + id, e);
        }
      }
    }
  }


  BundleNode selectedNode = null;

  public void nodeClicked(Node node, MouseEvent ev) {
    if(node instanceof BundleNode) {
      BundleNode bn = (BundleNode)node;
      selectedNode = bn;
      setBundle(bn.getBundle());
    }
  }

  public void nodeSelected(Node node, MouseEvent ev) {
    if(node instanceof BundleNode) {
      long t0 = System.currentTimeMillis();
      BundleNode bn = (BundleNode)node;
      if(bundleSelModel != null) {
        long bid = bn.getBundle().getBundleId();
        if((ev.getModifiers() & InputEvent.CTRL_MASK) != 0) {
          bundleSelModel
            .setSelected(bid, !bundleSelModel.isSelected(bid));
        } else {
          bundleSelModel.clearSelection();
          bundleSelModel.setSelected(bid, true);
        }
        selectedNode = bundleSelModel.isSelected(bid) ? bn : null;
        repaint();
      }
      long t1 = System.currentTimeMillis();
      if(t1 - t0 > 1000) {
        Activator.log.info("nodeSelected done " + (t1 - t0) + "ms");
      }
    }
  }

  public void noneSelected(MouseEvent ev) {
    if(bundleSelModel != null) {
      selectedNode = null;
      bundleSelModel.clearSelection();
      repaint();
    }
  }

  public void close() {
    try {
      bc.removeBundleListener(bundleListener);
      bc.removeServiceListener(serviceListener);
    } catch (IllegalStateException ise) {
      // BundleContext no longer valid, nothing to do.
    }
  }

  void setBundle(Bundle b) {
    jmb.setTitle(b);
    if(b != this.b) {
      this.b = b;
      startFade();
    }
  }

  public Bundle getBundle() {
    return b;
  }

  void paintNode(Graphics2D g, Node node) {
    if(node instanceof BundleNode) {
      BundleNode bn = (BundleNode)node;
      paintBundleNode(g, bundleSelModel, bn, hoverNode);
    } else if(node instanceof EmptyNode) {
      paintEmptyNode(g, bundleSelModel, node, hoverNode);
    }
  }

  void bundleChanged() {
  }

  BundleListener bundleListener = new BundleListener() {
      public void bundleChanged(BundleEvent ev) {
        if(jmb.isAutoRefresh()) {
          SwingUtilities.invokeLater(() -> {
            JSoftGraphBundle.this.bundleChanged();
            startFade();
          });
        }
      }
    };

  ServiceListener serviceListener = new ServiceListener() {
      public void serviceChanged(ServiceEvent ev) {
        if(jmb.isAutoRefresh()) {
          SwingUtilities.invokeLater(() -> startFade());
        }
      }
    };

  Color selectedColor      = new Color(100, 100, 255);
  Color unselectedColor    = new Color(100, 200, 200, 180);

  void paintEmptyNode(Graphics2D g,
                      BundleSelectionModel bundleSelModel,
                      Node node,
                      Node hoverNode) {
    // noop
  }

  void paintBundleNode(Graphics2D g,
                       BundleSelectionModel bundleSelModel,
                       BundleNode node,
                       Node hoverNode) {
    Point2D p = node.getPoint();
    if(p == null) {
      return;
    }

    Bundle b = node.getBundle();
    AffineTransform oldTrans = g.getTransform();

    Icon icon = Util.getBundleIcon(b);

    int w = icon.getIconWidth();
    int h = icon.getIconHeight();

    double x = p.getX();
    double y = p.getY();

    float f = (float)(1.0 / (1 + 2*node.getDepth()));

    g.translate(x, y);
    g.scale(f, f);

    boolean bSelected = bundleSelModel != null && bundleSelModel.isSelected(b.getBundleId());

    paintNodeStart(g, node);

    if(fadestate != STATE_FADE) {
      Util.setAntialias(g, true);
      if(node.getDepth() < 3) {
        if(bSelected) {
          Composite oldComp = g.getComposite();
          g.setComposite(alphaHalf);
          g.setColor(selectedColor);

          g.fillOval(-w/2, -h/2, w, h);
          g.setComposite(oldComp);
        } else {
          if(node.getDepth() < 2 && node.getOutLinks().size() == 0) {
            Composite oldComp = g.getComposite();
            g.setComposite(alphaHalf);
            g.setColor(unselectedColor);

            g.fillOval(-w/2, -h/2, w, h);
            g.setComposite(oldComp);
          }
        }
        if(hoverNode == node) {
          Composite oldComp = g.getComposite();
          g.setComposite(alphaHalf);
          g.setColor(new Color(50, 50, 255));

          g.drawOval(-w/2, -h/2, w, h);
          g.setComposite(oldComp);
        }
      }
      Util.setAntialias(g, false);
    }

    Util.setAntialias(g, true);
    if(node.getDepth() < 1) {
      icon.paintIcon(this, g, -w/2, -h/2);
    }
    boolean bNeedText = false; // hoverNode == node;
    /*
    if(bSelected) {
      if(node.getDepth() <= 1) {
        bNeedText = node == selectedNode;
      }
    }

    if(fadestate == STATE_FADE) {
      bNeedText = false;
    }
    */

    if (bNeedText) {
      g.scale(1.0/f, 1.0/f);
      paintString(g, "#" + b.getBundleId() + " " + Util.getBundleName(b),
                  20, icon.getIconHeight() + 4,
                  false);
    }

    g.setTransform(oldTrans);
    Util.setAntialias(g, false);
  }


}
