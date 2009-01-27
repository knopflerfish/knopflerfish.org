package org.knopflerfish.bundle.desktop.swing;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.awt.geom.AffineTransform;
import javax.swing.border.*;
import org.osgi.framework.*;
import java.awt.geom.Point2D;
import org.knopflerfish.bundle.desktop.swing.graph.*;
import java.awt.geom.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.util.tracker.ServiceTracker;
import org.knopflerfish.service.desktop.*;

public abstract class JSoftGraphBundle extends JSoftGraph {

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
    bc.addBundleListener(bundleListener);
    bc.addServiceListener(serviceListener);
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
        boolean bNeedUpdate = false;
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
    bc.removeBundleListener(bundleListener);
    bc.removeServiceListener(serviceListener);
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
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                JSoftGraphBundle.this.bundleChanged();
                startFade();
              }
            });
        }
      }
    };

  ServiceListener serviceListener = new ServiceListener() {
      public void serviceChanged(ServiceEvent ev) {
        if(jmb.isAutoRefresh()) {
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                startFade();
              }
            });
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
    Dimension size = getSize();
    
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
          int d = 10;
          Composite oldComp = g.getComposite();
          g.setComposite(alphaHalf);
          g.setColor(selectedColor);
          
          g.fillOval(-w/2, -h/2, w, h);
          g.setComposite(oldComp);
        } else {
          if(node.getDepth() < 2 && node.getOutLinks().size() == 0) {
            int d = 10;
            Composite oldComp = g.getComposite();
            g.setComposite(alphaHalf);
            g.setColor(unselectedColor);
            
            g.fillOval(-w/2, -h/2, w, h);
            g.setComposite(oldComp);
          }
        }
        if(hoverNode == node) {
          int d = 10;
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

    if(bNeedText) {
      g.scale(1.0/f, 1.0/f);
      paintString(g, "#" + b.getBundleId() + " " + Util.getBundleName(b), 
                  20, icon.getIconHeight() + 4, 
                  false);
    }
    
    g.setTransform(oldTrans);
    Util.setAntialias(g, false);
  }


}

