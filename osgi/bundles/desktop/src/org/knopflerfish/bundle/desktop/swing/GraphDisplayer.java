/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

import org.knopflerfish.bundle.desktop.swing.graph.BundleNode;
import org.knopflerfish.bundle.desktop.swing.graph.Node;
import org.knopflerfish.service.desktop.BundleSelectionListener;
import org.knopflerfish.service.desktop.BundleSelectionModel;
public class GraphDisplayer extends DefaultSwingBundleDisplayer {

  ButtonModel autoRefreshModel = new JToggleButton.ToggleButtonModel();

  public GraphDisplayer(BundleContext bc) {
    super(bc, "Graph", "Graph display of bundles", false);

    autoRefreshModel.setSelected(true);
  }

  @Override
  public void bundleChanged(final BundleEvent ev) {
    super.bundleChanged(ev);
    SwingUtilities.invokeLater(() -> {
      for (final JComponent jComponent : components) {
        final JMainBundles comp = (JMainBundles)jComponent;
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
    });
  }

  @Override
  public void showBundle(Bundle b) {
    for (final JComponent jComponent : components) {
      final JMainBundles comp = (JMainBundles)jComponent;
      comp.bundleHistory.addBundle(b);
      comp.setBundle(b);
    }
  }

  @Override
  public JComponent newJComponent() {
    return new JMainBundles();
  }

  @Override
  public void  disposeJComponent(JComponent comp) {
    if(comp instanceof JMainBundles) {
      ((JMainBundles)comp).close();
    }
    super.disposeJComponent(comp);
  }

  @Override
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
    public void    setSelected(List<Long> bids, boolean bSelected)
    {
      bundleSelModel.setSelected(bids, bSelected);
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

    private static final long serialVersionUID = 1L;

    private Set<JSoftGraphBundle> views = new LinkedHashSet<>();

    private JBundleHistory bundleHistory;

    private JCheckBoxMenuItem autoRefreshCB;

    MouseAdapter contextMenuListener = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
      }
      @Override
      public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
      }
      private void maybeShowPopup(MouseEvent e) {
        if(e.isPopupTrigger() ||
           ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) ||
           e.getButton() > 1) {
          JPopupMenu contextPopupMenu = makePopup();
          final Component comp = e.getComponent();
          contextPopupMenu.show(comp, e.getX(), e.getY());
        }
      }
    };

    JPopupMenu makePopup() {
      final JPopupMenu menu = new JPopupMenu();

      final Bundle[] selbl = Activator.desktop.getSelectedBundles();
      if (selbl != null && selbl.length > 0) {
        final Bundle b = selbl[0];
        final JMenuItem item = makeBundleItem(b, "#" + makeBundleItemText(b));
        menu.add(item);
        menu.add(new JPopupMenu.Separator());
      }

      {
        final JMenuItem item = new JMenuItem("New window");

        item.addActionListener(ev -> addWindow());

        menu.add(item);
        menu.add(new JPopupMenu.Separator());
      }

      menu.add(new JMenuItem(Activator.desktop.actionStopBundles));
      menu.add(new JMenuItem(Activator.desktop.actionStartBundles));
      menu.add(new JMenuItem(Activator.desktop.actionUpdateBundles));
      menu.add(new JMenuItem(Activator.desktop.actionUninstallBundles));
      menu.add(new JMenuItem(Activator.desktop.actionResolveBundles));
      menu.add(new JMenuItem(Activator.desktop.actionRefreshBundles));

      menu.add(new JPopupMenu.Separator());

      autoRefreshCB = new JCheckBoxMenuItem("Automatic view refresh", true);
      autoRefreshCB.setModel(autoRefreshModel);

      final JMenuItem refreshItem = new JMenuItem("Refresh view");
      refreshItem.addActionListener(ev -> {
        if(Activator.desktop != null &&
           Activator.desktop.getPackageManager() != null) {
          Activator.desktop.getPackageManager().refresh();
          for (final JSoftGraphBundle view : views) {
            view.startFade();
          }
        }
      });

      menu.add(autoRefreshCB);
      menu.add(refreshItem);

      menu.add(new JPopupMenu.Separator());

      final Map<String, Collection<Bundle>> buckets = Activator.desktop.makeBundleBuckets();

      for (final String key : buckets.keySet()) {
        final Collection<Bundle> bucket = buckets.get(key);
        if(bucket.size()>1) {
          final JMenu subMenu = new JMenu(key);
          for (final Bundle bundle : bucket) {
            final JMenuItem item = makeBundleItem(bundle);
            subMenu.add(item);
          }
          menu.add(subMenu);
        } else if(bucket.size()==1) {
          final Bundle bundle = bucket.iterator().next();
          final String text = key + " - " + makeBundleItemText(bundle);
          final JMenuItem item = makeBundleItem(bundle, text);
          menu.add(item);
        }
      }

      return menu;
    }

    boolean isAutoRefresh() {
      return autoRefreshCB == null || autoRefreshCB.isSelected();
    }

    String makeBundleItemText(final Bundle bundle)
    {
      return Util.getBundleName(bundle) + " #" + bundle.getBundleId();
    }

    JMenuItem makeBundleItem(final Bundle bundle)
    {
      return makeBundleItem(bundle, makeBundleItemText(bundle));
    }

    JMenuItem makeBundleItem(final Bundle bundle, String txt)
    {
      final JMenuItem item = new JMenuItem(txt);

      item.addActionListener(ev -> {
        for (final JSoftGraphBundle view : views) {
          view.setBundle(bundle);
        }
        bundleHistory.addBundle(bundle);
        bundleSelModel.clearSelection();
        bundleSelModel.setSelected(bundle.getBundleId(), true);
      });
      return item;
    }

    JFrame frame;

    Set<JMainBundles> windows = new HashSet<>();

    public void close() {
      for (final JMainBundles comp : windows) {
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
      final Bundle[] bl = Activator.desktop.getSelectedBundles();
      if(bl != null && bl.length > 0) {
        newB = bl[0];
      } else {
        newB = Activator.getTargetBC_getBundle(0);
      }
      final JMainBundles comp = new JMainBundles(newB);
      comp.frame = new JFrame(makeTitle(newB));
      comp.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      comp.frame.addWindowListener(new WindowAdapter() {
          @Override
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

      final JButton newButton = new JButton("+") {
        private static final long serialVersionUID = 1L;
        {
          addActionListener(ev -> addWindow());
      }};
      newButton.setToolTipText("Open new window");
      // newButton.setBorder(null);
      newButton.setBorderPainted(false);
      newButton.setOpaque(false);

      bundleHistory = new JBundleHistory(bc, null, bsmProxy, 10, 40) {
          private static final long serialVersionUID = 1L;
          @Override
          void bundleClicked(Bundle b) {
            for (final JSoftGraphBundle view : views) {
              setBundle(b);
              view.setBundle(b);
            }
          }

          @Override
          void bundleSelected(Bundle b) {
            bsmProxy.clearSelection();
            bsmProxy.setSelected(b.getBundleId(), true);
            for (final JSoftGraphBundle view : views) {
              view.repaint();
            }

          }
        };

      final JSoftGraphBundle view1 = new JServiceView(this, bc, b, bsmProxy) {
          private static final long serialVersionUID = 1L;

          @Override
          public void nodeClicked(Node node, MouseEvent ev) {
            super.nodeClicked(node, ev);
            if(node instanceof BundleNode) {
              final BundleNode bn = (BundleNode)node;
              bundleHistory.addBundle(bn.getBundle());
              setBundle(bn.getBundle());
              for (final JSoftGraphBundle view : views) {
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
          private static final long serialVersionUID = 1L;

          @Override
          public void nodeClicked(Node node, MouseEvent ev) {
            super.nodeClicked(node, ev);
            if(node instanceof BundleNode) {
              final BundleNode bn = (BundleNode)node;
              bundleHistory.addBundle(bn.getBundle());
              setBundle(bn.getBundle());
              for (final JSoftGraphBundle view : views) {
                if(view != this) {
                  view.setBundle(bn.getBundle());
                }
              }
            }
          }
        };
      // view2.setMaxDepth(9);
      view2.addMouseListener(contextMenuListener);
      view2.setPaintRootName();

      views.add(view2);
      views.add(view1);


      bundleHistory.setBackground(view1.bottomColor);
      final JPanel vp = new JPanel();
      vp.setLayout(new BoxLayout(vp, BoxLayout.X_AXIS));

      for (final JSoftGraphBundle view : views) {
        vp.add(view);
      }

      JPanel panel = new JPanel(new BorderLayout());

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
      for (final JSoftGraphBundle view : views) {
        view.setBundle(b);
      }
    }

    void setTitle(Bundle b) {
      Container comp = this;
      while(null != comp) {
        if(comp instanceof JFrame) {
          final JFrame frame = (JFrame)comp;
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
          final Bundle last = bundleHistory.getLastBundle();
          if(last != null) {
            setBundle(last);
          }
        }
      }
    }

    // Bundle selection has changed
    public void valueChanged(long bid) {
      for (final JSoftGraphBundle view : views) {
        view.repaint();
      }
    }
  }
}
