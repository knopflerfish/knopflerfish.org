/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.URL;
import java.text.SimpleDateFormat;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.startlevel.StartLevel;
import javax.swing.border.*;

import org.knopflerfish.bundle.desktop.swing.graph.*;
import org.knopflerfish.service.desktop.*;
public class GraphDisplayer extends DefaultSwingBundleDisplayer {

  ButtonModel autorefreshModel = new JToggleButton.ToggleButtonModel();

  public GraphDisplayer(BundleContext bc) {
    super(bc, "Graph", "Graph display of bundles", false);

    autorefreshModel.setSelected(true);
  }



  public void bundleChanged(BundleEvent ev) {
    super.bundleChanged(ev);

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JMainBundles comp = (JMainBundles)it.next();
      switch(ev.getType()) {
      case BundleEvent.INSTALLED:
        comp.addBundle(ev.getBundle());
        break;
      case BundleEvent.UNINSTALLED:
        comp.removeBundle(ev.getBundle());
        break;
      default:
        comp.updateBundleComp(ev.getBundle());
        break;
      }
    }

    repaintComponents();
  }

  public void showBundle(Bundle b) {
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JMainBundles comp = (JMainBundles)it.next();
      comp.bundleHistory.addBundle(b);
      comp.setBundle(b);
    }
  }

  public JComponent newJComponent() {
    return new JMainBundles();
  }

  public void  disposeJComponent(JComponent comp) {
    if(comp instanceof JMainBundles) {
      ((JMainBundles)comp).close();
    }
    super.disposeJComponent(comp);
  }

  public void valueChanged(long bid) {
    super.valueChanged(bid);
  }

  BundleSelectionModel bsmProxy = new BundleSelectionModel() {
      public void    clearSelection() {
        bundleSelModel.clearSelection();
      }
      public boolean isSelected(long bid) {
        return bundleSelModel.isSelected(bid);
      }
      public void    setSelected(long bid, boolean bSelected) {
        bundleSelModel.setSelected(bid, bSelected);
      }
      public void    addBundleSelectionListener(BundleSelectionListener l) {
        bundleSelModel.addBundleSelectionListener(l);
      }
      public void    removeBundleSelectionListener(BundleSelectionListener l) {
        bundleSelModel.removeBundleSelectionListener(l);
      }
      public int getSelectionCount() {
        return bundleSelModel.getSelectionCount();
      }
      public long getSelected() {
        return bundleSelModel.getSelected();
      }
    };

  class JMainBundles extends JPanel {
    JPanel panel;

    Set views = new LinkedHashSet();
    // JSoftGraphBundle serviceView = null;
    // JSoftGraphBundle packageView = null;

    JBundleHistory bundleHistory;

    JPopupMenu    contextPopupMenu;
    JCheckBoxMenuItem autorefreshCB;

    MouseAdapter contextMenuListener = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          maybeShowPopup(e);
        }
        public void mouseReleased(MouseEvent e) {
          maybeShowPopup(e);
        }
        private void maybeShowPopup(MouseEvent e) {
          if(e.isPopupTrigger() ||
             ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) ||
             e.getButton() > 1) {
            contextPopupMenu = makePopup();
            Component comp = e.getComponent();
            contextPopupMenu.show(comp, e.getX(), e.getY());
          }
        }
      };

    JPopupMenu makePopup() {
      JPopupMenu     menu    = new JPopupMenu();


      Map            bundles = new TreeMap();
      final Bundle[] bl      = Activator.getBundles();

      Bundle[] selbl = Activator.desktop.getSelectedBundles();
      if(selbl != null && selbl.length > 0) {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        Bundle b = selbl[0];
        JMenuItem item = makeBundleItem(b, "#" + b.getBundleId() + " " + Util.getBundleName(b));
        menu.add(item);
        menu.add(new JPopupMenu.Separator());
      }

      {
        JMenuItem item = new JMenuItem("New window");

        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              addWindow();
            }
          });

        menu.add(item);
        menu.add(new JPopupMenu.Separator());
      }

      menu.add(new JMenuItem(Activator.desktop.actionStopBundles));
      menu.add(new JMenuItem(Activator.desktop.actionStartBundles));
      menu.add(new JMenuItem(Activator.desktop.actionUpdateBundles));
      menu.add(new JMenuItem(Activator.desktop.actionUninstallBundles));
      menu.add(new JMenuItem(Activator.desktop.actionRefreshBundles));

      menu.add(new JPopupMenu.Separator());

      autorefreshCB = new JCheckBoxMenuItem("Automatic view refresh", true);
      autorefreshCB.setModel(autorefreshModel);

      JMenuItem refreshItem = new JMenuItem("Refresh view");
      refreshItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            if(Activator.desktop != null &&
               Activator.desktop.pm != null) {
              Activator.desktop.pm.refresh();
              for(Iterator it = views.iterator(); it.hasNext(); ) {
                JSoftGraphBundle view = (JSoftGraphBundle)it.next();
                view.startFade();
              }
            }
          }
        });

      menu.add(autorefreshCB);
      menu.add(refreshItem);

      menu.add(new JPopupMenu.Separator());


      for(int i = 0; bl != null && i < bl.length; i++) {
        bundles.put(new Long(bl[i].getBundleId()), bl[i]);
      }


      Map buckets = Activator.desktop.makeBundleBuckets();

      for(Iterator it = buckets.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();
        Object val = buckets.get(key);
        if(val instanceof Collection) {
          Collection bucket = (Collection)val;
          JMenu subMenu = new JMenu(key.toString());
          for(Iterator it2 = bucket.iterator(); it2.hasNext(); ) {
            Bundle bundle = (Bundle)it2.next();
            JMenuItem item = makeBundleItem(bundle, null);
            subMenu.add(item);
          }
          menu.add(subMenu);
        } else if(val instanceof Bundle) {
          Bundle bundle = (Bundle)val;
          JMenuItem item = makeBundleItem(bundle, null);
          menu.add(item);
        } else {
          throw new RuntimeException("Unknown object=" + val);
        }
      }

      return menu;
    }

    boolean isAutoRefresh() {
      return autorefreshCB == null || autorefreshCB.isSelected();
    }

    JMenuItem makeBundleItem(final Bundle bundle, String txt) {
      JMenuItem item = new JMenuItem(txt != null
                                     ? txt
                                     : (bundle.getBundleId() + " " + Util.getBundleName(bundle)));

      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            for(Iterator it = views.iterator(); it.hasNext(); ) {
              JSoftGraphBundle view = (JSoftGraphBundle)it.next();
              view.setBundle(bundle);
            }
            bundleHistory.addBundle(bundle);
            bundleSelModel.clearSelection();
            bundleSelModel.setSelected(bundle.getBundleId(), true);
          }
        });
      return item;
    }

    JFrame frame;

    Set windows = new HashSet();

    public void close() {
      for(Iterator it = windows.iterator(); it.hasNext(); ) {
        JMainBundles comp = (JMainBundles)it.next();
        comp.close();
      }
      windows.clear();

      if(frame != null) {
        frame.setVisible(false);
        frame.dispose();
      }
      frame = null;
    }


    public JMainBundles() {
      this(null);
    }

    void addWindow() {
      Bundle newB;
      Bundle[] bl = Activator.desktop.getSelectedBundles();
      if(bl != null && bl.length > 0) {
        newB = bl[0];
      } else {
        newB = Activator.getTargetBC().getBundle(0);
      }
      final JMainBundles comp = new JMainBundles(newB);
      comp.frame = new JFrame(makeTitle(newB));
      comp.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      comp.frame.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
            comp.close();
          }
        });
      comp.frame.getContentPane().add(comp);
      comp.frame.pack();
      comp.frame.setVisible(true);

      windows.add(comp);
    }

    public JMainBundles(Bundle b) {
      if(b == null) {
        b = bc.getBundle(0);
      }
      setLayout(new BorderLayout());
      panel = new JPanel(new BorderLayout());

      JButton newButton = new JButton("+") {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              addWindow();
            }
          });
      }};
      newButton.setToolTipText("Open new window");
      // newButton.setBorder(null);
      newButton.setBorderPainted(false);
      newButton.setOpaque(false);

      bundleHistory = new JBundleHistory(bc, null, bsmProxy, 10, 40) {
          void bundleClicked(Bundle b) {
            for(Iterator it = views.iterator(); it.hasNext(); ) {
              JSoftGraphBundle view = (JSoftGraphBundle)it.next();
              setBundle(b);
              view.setBundle(b);
            }
          }

          void bundleSelected(Bundle b) {
            bsmProxy.clearSelection();
            bsmProxy.setSelected(b.getBundleId(), true);
            for(Iterator it = views.iterator(); it.hasNext(); ) {
              JSoftGraphBundle view = (JSoftGraphBundle)it.next();
              view.repaint();
            }

          }
        };

      final JSoftGraphBundle view1 = new JServiceView(this, bc, b, bsmProxy) {
          public void nodeClicked(Node node, MouseEvent ev) {
            super.nodeClicked(node, ev);
            if(node instanceof BundleNode) {
              BundleNode bn = (BundleNode)node;
              bundleHistory.addBundle(bn.getBundle());
              setBundle(bn.getBundle());
              for(Iterator it = views.iterator(); it.hasNext(); ) {
                JSoftGraphBundle view = (JSoftGraphBundle)it.next();
                if(view != this) {
                  view.setBundle(bn.getBundle());
                }
              }
            }
          }
        };
      view1.setMaxDepth(8);
      view1.addMouseListener(contextMenuListener);


      final JSoftGraphBundle view2 = new JPackageView(this, bc, b, bsmProxy) {
          public void nodeClicked(Node node, MouseEvent ev) {
            super.nodeClicked(node, ev);
            if(node instanceof BundleNode) {
              BundleNode bn = (BundleNode)node;
              bundleHistory.addBundle(bn.getBundle());
              setBundle(bn.getBundle());
              for(Iterator it = views.iterator(); it.hasNext(); ) {
                JSoftGraphBundle view = (JSoftGraphBundle)it.next();
                if(view != this) {
                  view.setBundle(bn.getBundle());
                }
              }
            }
          }
        };
      // view2.setMaxDepth(9);
      view2.addMouseListener(contextMenuListener);
      view2.setPaintRootName(true);

      views.add(view2);
      views.add(view1);


      bundleHistory.setBackground(view1.bottomColor);
      JPanel vp = new JPanel();
      vp.setLayout(new BoxLayout(vp, BoxLayout.X_AXIS));

      for(Iterator it = views.iterator(); it.hasNext(); ) {
        JSoftGraphBundle view = (JSoftGraphBundle)it.next();
        vp.add(view);
      }


      panel.add(vp, BorderLayout.CENTER);
      panel.add(bundleHistory, BorderLayout.SOUTH);

      bundleHistory.addBundle(view1.getBundle());

      add(panel, BorderLayout.CENTER);
      setBundle(b);
    }

    Bundle bundle;

    String makeTitle(Bundle b) {
      return "OSGi Garden: " +
        (b != null
         ? ("#" + b.getBundleId() + " " + Util.getBundleName(b))
         : "no bundle");
    }

    void setBundle(Bundle b) {
      bundle = b;
      setTitle(b);
      for(Iterator it = views.iterator(); it.hasNext(); ) {
        JSoftGraphBundle view = (JSoftGraphBundle)it.next();
        view.setBundle(b);
      }
    }

    void setTitle(Bundle b) {
      Container comp = this;
      while(null != comp) {
        if(comp instanceof JFrame) {
          JFrame frame = (JFrame)comp;
          if(frame != Activator.desktop.frame) {
            frame.setTitle(makeTitle(b));
          }
        }
        comp = comp.getParent();
      }
    }


    public void addBundle(Bundle b) {
    }

    public void updateBundleComp(Bundle b) {
    }

    public void removeBundle(Bundle b) {
      if(b.equals(bundle)) {
        if(bundleHistory.removeBundle(b)) {
          Bundle last = bundleHistory.getLastBundle();
          if(last != null) {
            setBundle(last);
          }
        }
      }
    }

    // Bundle selection has changed
    public void valueChanged(long bid) {
      for(Iterator it = views.iterator(); it.hasNext(); ) {
        JSoftGraphBundle view = (JSoftGraphBundle)it.next();
        view.repaint();
      }
    }
  }
}
