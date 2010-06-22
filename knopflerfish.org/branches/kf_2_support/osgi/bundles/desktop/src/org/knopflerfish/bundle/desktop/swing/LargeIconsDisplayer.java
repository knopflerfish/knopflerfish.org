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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

import org.osgi.service.startlevel.StartLevel;


public class LargeIconsDisplayer extends DefaultSwingBundleDisplayer {

  public static final String NAME = "Large Icons";

  public LargeIconsDisplayer(BundleContext bc) {
    super(bc, NAME, "Large icon display of bundles", false);
  }

  public void bundleChanged(BundleEvent ev) {
    super.bundleChanged(ev);

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JLargeIcons comp = (JLargeIcons)it.next();
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
      JLargeIcons comp = (JLargeIcons)it.next();
      comp.showBundle(b);
    }
  }


  public JComponent newJComponent() {
    return new JLargeIcons();
  }


  class JLargeIcons extends JPanel {
    Map         bundleMap = new TreeMap();
    GridLayout  grid;
    ImageIcon   bundleIcon;
    JPanel      panel;
    JScrollPane scroll;

    MouseListener contextMenuListener = null;
    JPopupMenu    contextPopupMenu;

    static final String moveSelUpAction    = "moveSelUpAction";
    static final String moveSelDownAction  = "moveSelDownAction";
    static final String moveSelLeftAction  = "moveSelLeftAction";
    static final String moveSelRightAction = "moveSelRightAction";

    class MoveSelectionAction extends AbstractAction
    {
      final int dir;

      public MoveSelectionAction(int dir)
      {
        this.dir = dir;
      }

      public void actionPerformed(ActionEvent e)
      {
        final long bid = bundleSelModel.getSelected();
        if (2>bundleSelModel.getSelectionCount()) {
          JComponent cNew = null;
          final int columns = grid.getColumns();
          int delta = dir;
          if (-2==delta) delta = -columns;
          if (2==delta)  delta = columns;

          if (0<=bid) {
            final JComponent cOld = getBundleComponent(bid);
            final Component[] comps = panel.getComponents();
            for (int i=0; i<comps.length; i++) {
              if (cOld==comps[i]) {
                int iNew = i+delta;
                if (iNew>=comps.length || iNew<0) iNew = i;
                cNew = (JComponent) comps[iNew];
              }
            }
          } else {
            cNew = getBundleComponent(0L);
          }
          Long newBid = null;
          if (null!=cNew) {
            newBid = (Long)
              cNew.getClientProperty(LargeIconsDisplayer.class.getName()
                                     +".bid");
          }
          if (null!=newBid) {
            if (0<=bid) { // De-select old.
              bundleSelModel.setSelected(bid, false);
            }
            bundleSelModel.setSelected(newBid.longValue(), true);
            compToShow = cNew;
          }
        }
      }
    }

    class SelectAllAction extends AbstractAction
    {
      static final String SELECT_ALL = "selectAll";

      public SelectAllAction()
      {
      }

      public void actionPerformed(ActionEvent e)
      {
        final Component[] comps = panel.getComponents();
        final String bidKey = LargeIconsDisplayer.class.getName() +".bid";
        for (int i=0; i<comps.length; i++) {
          final JComponent comp = (JComponent) comps[i];
          final Long bidL = (Long) comp.getClientProperty(bidKey);
          final long bid = null==bidL ? -1 : bidL.longValue();
          if (0<=bid) {
            bundleSelModel.setSelected(bid, true);
          }
        }
      }
    }

    public JLargeIcons() {
      setLayout(new BorderLayout());
      setBackground(Color.white);

      grid  = new GridLayout(0, 4);
      panel = new JPanel(grid);

      panel.setBackground(SystemColor.text);
      panel.setFocusable(true);

      ActionMap actionMap = panel.getActionMap();
      actionMap.put(moveSelUpAction,    new MoveSelectionAction(-2));
      actionMap.put(moveSelDownAction,  new MoveSelectionAction(2));
      actionMap.put(moveSelLeftAction,  new MoveSelectionAction(-1));
      actionMap.put(moveSelRightAction, new MoveSelectionAction(1));
      actionMap.put(SelectAllAction.SELECT_ALL, new SelectAllAction());

      final InputMap inputMap
        = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      final KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
      inputMap.put(up, moveSelUpAction);
      final KeyStroke kpUp = KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0);
      inputMap.put(kpUp, moveSelUpAction);
      final KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
      inputMap.put(down, moveSelDownAction);
      final KeyStroke kpDown = KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0);
      inputMap.put(kpDown, moveSelDownAction);
      final KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
      inputMap.put(left, moveSelLeftAction);
      final KeyStroke kpLeft = KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0);
      inputMap.put(kpLeft, moveSelLeftAction);
      final KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
      inputMap.put(right, moveSelRightAction);
      final KeyStroke kpRight = KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0);
      inputMap.put(kpRight, moveSelRightAction);

      final int mask = getToolkit().getMenuShortcutKeyMask();
      final KeyStroke ctrlA = KeyStroke.getKeyStroke(KeyEvent.VK_A, mask);
      inputMap.put(ctrlA, SelectAllAction.SELECT_ALL);

      contextPopupMenu = new JPopupMenu();

      ButtonGroup       group         = new ButtonGroup();

      JCheckBoxMenuItem item = new JCheckBoxMenuItem("Sort by name");
      item.setState(iconComparator == nameComparator);
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            iconComparator = nameComparator;
            rebuildPanel();
          }
        });
      contextPopupMenu.add(item);
      group.add(item);

      item = new JCheckBoxMenuItem("Sort by start-level");
      item.setState(iconComparator == startLevelComparator);
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            iconComparator = startLevelComparator;
            rebuildPanel();
          }
        });
      contextPopupMenu.add(item);
      group.add(item);

      item = new JCheckBoxMenuItem("Sort by bundle ID");
      item.setState(iconComparator == null);
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            iconComparator = null;
            rebuildPanel();
          }
        });
      contextPopupMenu.add(item);
      group.add(item);

      contextMenuListener = new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
          }
          public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
          }
          private void maybeShowPopup(MouseEvent e) {
            if(contextPopupMenu != null &&
               (e.isPopupTrigger() ||
                ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0))) {
              Component comp = e.getComponent();
              contextPopupMenu.show(comp, e.getX(), e.getY());
            }
          }
        };

      panel.addMouseListener(contextMenuListener);

      // handle scroll panel resizing to be able to set grid size
      scroll = new JScrollPane(panel) {
          int oldW = -1;
          int oldH = -1;
          public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);

            // avoid looping when rebuilding panel
            if(w != oldW || h != oldH) {
              oldW = w;
              oldH = h;
              rebuildPanel();
            }
          }

          public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(compToShow != null) {
              JComponent c = compToShow;
              final Rectangle cBounds = c.getBounds();
              scroll.getViewport().scrollRectToVisible(cBounds);
              compToShow = null;
              // Work around for viewport.scrollRectToVisible
              // which never scrolls to the right or up...
              final Rectangle viewRect = scroll.getViewport().getViewRect();

              if (!viewRect.contains(cBounds)) {
                final Point p = new Point(viewRect.x, viewRect.y);

                if (cBounds.x < viewRect.x) {
                  p.x = cBounds.x;
                } else if (cBounds.x+cBounds.width>viewRect.x+viewRect.width) {
                  p.x = cBounds.x+cBounds.width - viewRect.width;
                }
                if (cBounds.y < viewRect.y) {
                  p.y = cBounds.y;
                } else if (cBounds.y+cBounds.height>viewRect.y+viewRect.height){
                  p.y = cBounds.y+cBounds.height - viewRect.height;
                }
                scroll.getViewport().setViewPosition(p);
              }
            }
          }

        };

      add(scroll, BorderLayout.CENTER);
    }

    public void showBundle(final Bundle b) {
      if(SwingUtilities.isEventDispatchThread()) {
        showBundle0(b);
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              showBundle0(b);
            }
          });
      }
    }

    JComponent compToShow = null;
    public void showBundle0(final Bundle b) {
      JComponent c = getBundleComponent(b);
      if(c != null) {
        compToShow = c;
        revalidate();
        invalidate();
        repaint();
      }
    }


    public void addBundle(final Bundle b) {
      if(SwingUtilities.isEventDispatchThread()) {
        addBundle0(b);
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              addBundle0(b);
            }
          });
      }
    }

    public void addBundle0(final Bundle b) {
      final long bid = b.getBundleId();

      if(null == getBundleComponent(b)) {
        JLabel c = new JLabel(Util.getBundleName(b),
                              bundleIcon,
                              SwingConstants.CENTER) {
            {
              addMouseListener(new MouseAdapter() {
                  public void mousePressed(MouseEvent ev) {
                    final int mask
                      = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

                    if((ev.getModifiers() & mask) != 0) {
                      bundleSelModel
                        .setSelected(bid, !bundleSelModel.isSelected(bid));
                    } else {
                      bundleSelModel.clearSelection();
                      bundleSelModel.setSelected(bid, true);
                    }
                    setBackground(getBackground());
                    panel.requestFocus();
                  }
                });
              addMouseListener(contextMenuListener);
              setBorder(null);
              setOpaque(true);
              setBackground(Color.yellow);
              putClientProperty(LargeIconsDisplayer.class.getName()+".bid",
                                new Long(bid));
            }

            public boolean isBackgroundSet() { return true;}
            public Color   getBackground() {
              try {
                boolean bSel = bundleSelModel != null
                  ? bundleSelModel.isSelected(b.getBundleId())
                  : false;

                return bSel
                  ? SystemColor.textHighlight
                  : JLargeIcons.this.getBackground();
              } catch (Exception e) {
                return Color.black;
              }
            }

            public boolean isForegroundSet() { return true; }
            public Color   getForeground() {
              try {
                boolean bSel = bundleSelModel != null
                  ? bundleSelModel.isSelected(b.getBundleId())
                  : false;

                return bSel
                  ? SystemColor.textHighlightText
                  : JLargeIcons.this.getForeground();
              } catch (Exception e) {
                return Color.black;
              }
            }
          };


        c.setToolTipText(Util.bundleInfo(b));
        c.setVerticalTextPosition(AbstractButton.BOTTOM);
        c.setHorizontalTextPosition(AbstractButton.CENTER);

        c.setPreferredSize(new Dimension(96, 64));
        c.setBorder(null);
        c.setFont(getFont());

        bundleMap.put(new Long(b.getBundleId()), c);

        updateBundleComp(b);

        rebuildPanel();
      }
    }


    Comparator iconComparator = null;

    void rebuildPanel() {
      if(SwingUtilities.isEventDispatchThread()) {
        rebuildPanel0();
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              rebuildPanel0();
            }
          });
      }
    }

    void rebuildPanel0() {

      panel.removeAll();

      Set set = new TreeSet(iconComparator);
      set.addAll(bundleMap.keySet());

      int w = 0; // Width of widest icon
      int h = 0; // Height of higest icon
      for(Iterator it = set.iterator(); it.hasNext(); ) {
        Long      bid = (Long)it.next();
        JComponent c   = (JComponent)bundleMap.get(bid);
        Dimension size = c.getPreferredSize();
        w = Math.max(w, size.width);
        h = Math.max(h, size.height);
      }
      w = 0==w ? 30 : w; // Avoid division by zero.
      h = 0==h ? 30 : h;

      // The viewport extent and size will be 0 during the first
      // layout (i.e., first call here) but the size of the panel will
      // be set, thus use it to guestimate the number of columns to
      // use.
      final JViewport viewport = scroll.getViewport();
      final Dimension viewportExtent = viewport.getExtentSize();
      final Dimension viewportSize = viewport.getSize();
      final Dimension size = getSize();

      if(viewportExtent.width != 0) {
        grid.setColumns(viewportExtent.width<w ? 1 : viewportExtent.width / w);
        grid.setRows(0);
      } else if (0!=viewportSize.width) {
        // Use viewport width as approximation to viewport extent width.
        grid.setColumns(viewportSize.width<w ? 1 : viewportSize.width / w);
        grid.setRows(0);
      } else if (0!=size.width) {
        // Use panel width as approximation to viewport extent width.
        grid.setColumns(size.width<w ? 1 : size.width / w);
        grid.setRows(0);
      }

      for(Iterator it = set.iterator(); it.hasNext(); ) {
        final Long bid = (Long)it.next();
        final Component c = (Component)bundleMap.get(bid);
        panel.add(c);
      }

      revalidate();
      panel.requestFocus();
      repaint();
    }

    public void removeBundle(final Bundle b) {
      if(SwingUtilities.isEventDispatchThread()) {
        removeBundle0(b);
      } else {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              removeBundle0(b);
            }
          });
      }
    }

    public void removeBundle0(Bundle b) {
      bundleMap.remove(new Long(b.getBundleId()));
      icons.remove(b);
      rebuildPanel();
    }

    JComponent getBundleComponent(long bid) {
      return (JComponent) bundleMap.get(new Long(bid));
    }

    JComponent getBundleComponent(Bundle b) {
      return (JComponent)bundleMap.get(new Long(b.getBundleId()));
    }

    // Bundle -> BundleImageIcon
    Map icons = new HashMap();

    public void updateBundleComp(Bundle b) {
      JLabel c = (JLabel)getBundleComponent(b);

      if(c == null) {
        addBundle(b);
        return;
      }

      c.setToolTipText(Util.bundleInfo(b));

      Icon icon = Util.getBundleIcon(b);

      c.setIcon(icon);

      c.invalidate();
      c.repaint();
      invalidate();
      repaint();
    }
  }

  static Comparator nameComparator = new Comparator() {
      public int compare(Object o1, Object o2) {
        Bundle b1 =
          Activator.getTargetBC().getBundle(((Long)o1).longValue());
        Bundle b2 =
          Activator.getTargetBC().getBundle(((Long)o2).longValue());

        if(b1 == b2) {
          return 0;
        }
        if(b1 == null) {
          return -1;
        }
        if(b2 == null) {
          return 1;
        }

        return
          Util.getBundleName(b1).compareToIgnoreCase(Util.getBundleName(b2));
      }
    };

  static Comparator startLevelComparator = new Comparator() {
      public int compare(Object o1, Object o2) {
        Bundle b1 =
          Activator.getTargetBC().getBundle(((Long)o1).longValue());
        Bundle b2 =
          Activator.getTargetBC().getBundle(((Long)o2).longValue());

        if(b1 == b2) {
          return 0;
        }
        if(b1 == null) {
          return -1;
        }
        if(b2 == null) {
          return 1;
        }
        long bidDiff = b1.getBundleId() - b2.getBundleId();
        int  bidRes = bidDiff < 0 ? -1 : (bidDiff==0 ? 0 : 1);

        StartLevel sls = (StartLevel)Activator.desktop.slTracker.getService();
        if(sls != null) {
          try {
            int sl1 = sls.getBundleStartLevel(b1);
            int sl2 = sls.getBundleStartLevel(b2);
            return sl1==sl2 ? bidRes : sl1 - sl2;
          } catch (IllegalArgumentException _e) {
          }
        }
        return bidRes;
      }
    };
}
