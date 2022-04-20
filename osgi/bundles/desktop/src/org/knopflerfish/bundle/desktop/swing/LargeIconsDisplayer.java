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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
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
import org.osgi.framework.startlevel.BundleStartLevel;


public class LargeIconsDisplayer extends DefaultSwingBundleDisplayer {

  public static final String NAME = "Icons";
  public static final String PREFS_KEY_SORT = "sort";
  public static final String SORT_ORDER_PROPERTY
    = "org.knopflerfish.desktop.display.large_icons.sort";

  public LargeIconsDisplayer(BundleContext bc) {
    super(bc, NAME, "Icon display of bundles", false);
  }

  @Override
  public void bundleChanged(final BundleEvent ev) {
    super.bundleChanged(ev);
    SwingUtilities.invokeLater(() -> {
      for (final JComponent jComponent : components) {
        final JLargeIcons comp = (JLargeIcons) jComponent;
        switch(ev.getType()) {
        case BundleEvent.INSTALLED:
          comp.addBundle(new Bundle[]{ev.getBundle()});
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
  public void showBundle(final Bundle b) {
    // Must post to EDT since this shall be done after the addition of
    // the bundles component, which in turn is triggered by another
    // job posted to the EDT.
    SwingUtilities.invokeLater(() -> {
      for (final JComponent jComponent : components) {
        final JLargeIcons comp = (JLargeIcons)jComponent;
        comp.showBundle(b);
      }
    });
  }


  @Override
  public JComponent newJComponent()
  {
    final JLargeIcons res = new JLargeIcons();
    // Inject the bundles that already exists since there will be no bundle
    // events for them.
    res.addBundle(getBundleArray());
    return res;
  }

  class JLargeIcons extends JPanel {
    private final String CLIENT_PROPERTY_BID = LargeIconsDisplayer.class
        .getName() + ".bid";

    private static final long serialVersionUID = 1L;

    Map<Long, JLabel> bundleMap = new TreeMap<>();
    GridLayout  grid;
    JPanel      panel;
    JScrollPane scroll;

    MouseListener contextMenuListener;
    JPopupMenu    contextPopupMenu;

    static final String moveSelUpAction    = "moveSelUpAction";
    static final String moveSelDownAction  = "moveSelDownAction";
    static final String moveSelLeftAction  = "moveSelLeftAction";
    static final String moveSelRightAction = "moveSelRightAction";

    class MoveSelectionAction extends AbstractAction
    {
      private static final long serialVersionUID = 1L;

      final int dir;

      public MoveSelectionAction(int dir)
      {
        this.dir = dir;
      }

      @Override
      public void actionPerformed(ActionEvent e)
      {
        final long bid = bundleSelModel.getSelected();
        if (2>bundleSelModel.getSelectionCount()) {
          JComponent cNew = null;
          final int columns = grid.getColumns();
          int delta = dir;
          if (-2==delta) {
            delta = -columns;
          }
          if (2==delta) {
            delta = columns;
          }

          if (0<=bid) {
            final JComponent cOld = getBundleComponent(bid);
            final Component[] comps = panel.getComponents();
            for (int i=0; i<comps.length; i++) {
              if (cOld==comps[i]) {
                int iNew = i+delta;
                if (iNew>=comps.length || iNew<0) {
                  iNew = i;
                }
                cNew = (JComponent) comps[iNew];
              }
            }
          } else {
            cNew = getBundleComponent(0L);
          }
          Long newBid = null;
          if (null != cNew) {
            newBid = (Long) cNew.getClientProperty(CLIENT_PROPERTY_BID);
          }
          if (null!=newBid) {
            if (0<=bid) { // De-select old.
              bundleSelModel.setSelected(bid, false);
            }
            bundleSelModel.setSelected(newBid, true);
            compToShow = cNew;
          }
        }
      }
    }

    class SelectAllAction extends AbstractAction
    {
      private static final long serialVersionUID = 1L;

      static final String SELECT_ALL = "selectAll";

      public SelectAllAction()
      {
      }

      @Override
      public void actionPerformed(ActionEvent e)
      {
        final List<Long> selectedBundleIds = new ArrayList<>();
        final Component[] comps = panel.getComponents();
        for (final Component comp2 : comps) {
          final JComponent comp = (JComponent) comp2;
          final Long bidL = (Long) comp.getClientProperty(CLIENT_PROPERTY_BID);
          if (null!=bidL) {
            selectedBundleIds.add(bidL);
          }
        }
        bundleSelModel.setSelected(selectedBundleIds, true);
      }
    }


    final Action resolveBundleAction = new AbstractAction(
        Strings.get("item_resolvebundle"), Desktop.resolveIcon) {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final Bundle bundle = getBundlePopuMenuInvokedFor();
        if (bundle != null) {
          Desktop.theDesktop.resolveBundles(new Bundle[] { bundle });
        }
      }
    };

    final Action startBundleAction = new AbstractAction(
        Strings.get("item_startbundle"), Desktop.startIcon) {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final Bundle bundle = getBundlePopuMenuInvokedFor();
        if (bundle != null) {
          Desktop.theDesktop.startBundles(new Bundle[] { bundle });
        }
      }
    };

    final Action stopBundleAction = new AbstractAction(
        Strings.get("item_stopbundle"), Desktop.stopIcon) {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final Bundle bundle = getBundlePopuMenuInvokedFor();
        if (bundle != null) {
          Desktop.theDesktop.stopBundles(new Bundle[] { bundle });
        }
      }
    };

    final Action updateBundleAction = new AbstractAction(
        Strings.get("item_updatebundle"), Desktop.updateIcon) {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final Bundle bundle = getBundlePopuMenuInvokedFor();
        if (bundle != null) {
          Desktop.theDesktop.updateBundles(new Bundle[] { bundle });
        }
      }
    };

    final Action refreshBundleAction = new AbstractAction(
        Strings.get("item_refreshbundle"), Desktop.refreshIcon) {
      private static final long serialVersionUID = 1L;

      {
        putValue(SHORT_DESCRIPTION, Strings.get("item_refreshbundle.descr"));
      }

      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final Bundle bundle = getBundlePopuMenuInvokedFor();
        if (bundle != null) {
          Desktop.theDesktop.refreshBundles(new Bundle[] { bundle });
        }
      }
    };

    final Action uninstallBundleAction = new AbstractAction(
        Strings.get("item_uninstallbundle"), Desktop.uninstallIcon) {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent ev)
      {
        final Bundle bundle = getBundlePopuMenuInvokedFor();
        if (bundle != null) {
          Desktop.theDesktop.uninstallBundles(new Bundle[] { bundle });
        }
      }
    };

    public JLargeIcons() {
      setLayout(new BorderLayout());
      setBackground(Color.white);

      grid  = new GridLayout(0, 4);
      panel = new JPanel(grid);

      panel.setBackground(SystemColor.text);
      panel.setFocusable(true);

      final ActionMap actionMap = panel.getActionMap();
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


      final ButtonGroup       group         = new ButtonGroup();

      loadPrefs();

      JCheckBoxMenuItem item = new JCheckBoxMenuItem("Sort by name");
      item.setState(iconComparator == nameComparator);
      item.addActionListener(ev -> {
        iconComparator = nameComparator;
        storePrefs();
        rebuildPanel();
      });
      contextPopupMenu.add(item);
      group.add(item);

      item = new JCheckBoxMenuItem("Sort by start-level");
      item.setState(iconComparator == startLevelComparator);
      item.addActionListener(ev -> {
        iconComparator = startLevelComparator;
        storePrefs();
        rebuildPanel();
      });
      contextPopupMenu.add(item);
      group.add(item);

      item = new JCheckBoxMenuItem("Sort by bundle ID");
      item.setState(iconComparator == null);
      item.addActionListener(ev -> {
        iconComparator = null;
        storePrefs();
        rebuildPanel();
      });
      contextPopupMenu.add(item);
      group.add(item);

      contextPopupMenu.addSeparator();
      contextPopupMenu.add(new JMenuItem(resolveBundleAction));
      contextPopupMenu.add(new JMenuItem(startBundleAction));
      contextPopupMenu.add(new JMenuItem(stopBundleAction));
      contextPopupMenu.add(new JMenuItem(updateBundleAction));
      contextPopupMenu.add(new JMenuItem(refreshBundleAction));
      contextPopupMenu.add(new JMenuItem(uninstallBundleAction));

      contextMenuListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e)
        {
          maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
          maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e)
        {
          if (isPopupTrigger(e)) {
            final Component comp = e.getComponent();

            final Bundle bundle = getBundle(comp);
            if (bundle != null) {
              final Desktop d = Desktop.theDesktop;
              resolveBundleAction.setEnabled(d.resolveBundlePossible(bundle));
              startBundleAction.setEnabled(d.startBundlePossible(bundle));
              stopBundleAction.setEnabled(d.stopBundlePossible(bundle));
              updateBundleAction.setEnabled(true);
              refreshBundleAction.setEnabled(d.refreshBundleNeeded(bundle));
              uninstallBundleAction.setEnabled(true);
            } else {
              resolveBundleAction.setEnabled(false);
              startBundleAction.setEnabled(false);
              stopBundleAction.setEnabled(false);
              updateBundleAction.setEnabled(false);
              refreshBundleAction.setEnabled(false);
              uninstallBundleAction.setEnabled(false);
            }
            contextPopupMenu.show(comp, e.getX(), e.getY());
          }
        }
      };

      panel.addMouseListener(contextMenuListener);

      // handle scroll panel resizing to be able to set grid size
      scroll = new JScrollPane(panel) {
          private static final long serialVersionUID = 1L;
          int oldW = -1;
          int oldH = -1;
          @Override
          public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);

            // avoid looping when rebuilding panel
            if(w != oldW || h != oldH) {
              oldW = w;
              oldH = h;
              rebuildPanel();
            }
          }

          @Override
          public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(compToShow != null) {
              final JComponent c = compToShow;
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

    /**
     * Check if a mouse event should pop-up the context pop-up menu or not.
     * @param e The event to check.
     * @return true if this event should trigger the pop-up menu.
     */
    boolean isPopupTrigger(MouseEvent e) {
      return contextPopupMenu != null &&
          (e.isPopupTrigger() ||
           ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0));
      }

    public Bundle getBundlePopuMenuInvokedFor()
    {
      final Component invoker = contextPopupMenu.getInvoker();
      return getBundle(invoker);
    }

    public Bundle getBundle(Component comp) {
      Bundle res = null;
      if (comp instanceof JComponent) {
        final JComponent jcomp = (JComponent) comp;
        final Long bid = (Long) jcomp.getClientProperty(CLIENT_PROPERTY_BID);
        if (bid != null) {
          res = Activator.getTargetBC_getBundle(bid);
        }
      }
      return res;
    }

      public void showBundle(final Bundle b) {
      if(SwingUtilities.isEventDispatchThread()) {
        showBundle0(b);
      } else {
        SwingUtilities.invokeLater(() -> showBundle0(b));
      }
    }

    volatile JComponent compToShow = null;
    public void showBundle0(final Bundle b) {
      final JComponent c = getBundleComponent(b);
      if(c != null) {
        compToShow = c;
        revalidate();
        invalidate();
        repaint();
      }
    }


    public void addBundle(final Bundle[] bundles) {
      if(SwingUtilities.isEventDispatchThread()) {
        addBundle0(bundles);
      } else {
        SwingUtilities.invokeLater(() -> addBundle0(bundles));
      }
    }

    public void addBundle0(final Bundle[] bundles) {
      if (bundles != null) {
        for (final Bundle bundle : bundles) {
          if(null == getBundleComponent(bundle)) {
            final JLabel c = createJLabelForBundle(bundle);

            c.setToolTipText(Util.bundleInfo(bundle));
            c.setVerticalTextPosition(SwingConstants.BOTTOM);
            c.setHorizontalTextPosition(SwingConstants.CENTER);

            c.setPreferredSize(new Dimension(96, 64));
            c.setBorder(null);
            c.setFont(getFont());

            bundleMap.put(bundle.getBundleId(), c);

            updateBundleComp(bundle);
          }
        }
      }
      rebuildPanel();
    }

    private JLabel createJLabelForBundle(final Bundle bundle)
    {
      final long bid = bundle.getBundleId();
      return new JLabel(Util.getBundleName(bundle), null, SwingConstants.CENTER) {
        private static final long serialVersionUID = 1L;
        {
          addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev)
            {
              if (!isPopupTrigger(ev)) {
                final int mask =
                  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

                if ((ev.getModifiers() & mask) != 0) {
                  bundleSelModel.setSelected(bid,
                                             !bundleSelModel.isSelected(bid));
                } else {
                  bundleSelModel.clearSelection();
                  bundleSelModel.setSelected(bid, true);
                }
                setBackground(getBackground());
                panel.requestFocus();
              }
            }
          });
          addMouseListener(contextMenuListener);
          setBorder(null);
          setOpaque(true);
          setBackground(Color.yellow);
          putClientProperty(CLIENT_PROPERTY_BID, bid);
        }

        @Override
        public boolean isBackgroundSet()
        {
          return true;
        }

        @Override
        public Color getBackground()
        {
          try {
            final boolean bSel = bundleSelModel != null && bundleSelModel.isSelected(bundle.getBundleId());

            return bSel ? SystemColor.textHighlight : JLargeIcons.this
                .getBackground();
          } catch (final Exception e) {
            return Color.black;
          }
        }

        @Override
        public boolean isForegroundSet()
        {
          return true;
        }

        @Override
        public Color getForeground()
        {
          try {
            final boolean bSel = bundleSelModel != null && bundleSelModel.isSelected(bundle.getBundleId());

            return bSel ? SystemColor.textHighlightText : JLargeIcons.this
                .getForeground();
          } catch (final Exception e) {
            return Color.black;
          }
        }

        @Override
        public String getToolTipText()
        {
          if (getClientProperty(TOOL_TIP_TEXT_KEY) != null) {
            // If a tool tip text is set, return an up to date version of
            // the text since we do not have events that can be used to
            // trigger update of the text for some parts of the text.
            // E.g., the start level and the persistently started property.
            return Util.bundleInfo(bundle);
          }
          return null;
        }
      };
    }


    Comparator<Long> iconComparator = null;

    void rebuildPanel() {
      if(SwingUtilities.isEventDispatchThread()) {
        rebuildPanel0();
      } else {
        SwingUtilities.invokeLater(this::rebuildPanel0);
      }
    }

    void rebuildPanel0() {
      panel.removeAll();

      final Set<Long> set = new TreeSet<>(iconComparator);
      set.addAll(bundleMap.keySet());

      int w = 0; // Width of widest icon
      int h = 0; // Height of highest icon
      for (final Long bid : set) {
        final JComponent c   = bundleMap.get(bid);
        final Dimension size = c.getPreferredSize();
        w = Math.max(w, size.width);
        h = Math.max(h, size.height);
      }
      w = 0==w ? 30 : w; // Avoid division by zero.

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

      for (final Long bid : set) {
        final Component c = bundleMap.get(bid);
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
        SwingUtilities.invokeLater(() -> removeBundle0(b));
      }
    }

    public void removeBundle0(Bundle b) {
      bundleMap.remove(b.getBundleId());
      icons.remove(b);
      rebuildPanel();
    }

    JComponent getBundleComponent(long bid) {
      return bundleMap.get(bid);
    }

    JComponent getBundleComponent(Bundle b) {
      return bundleMap.get(b.getBundleId());
    }

    Map<Bundle, BundleImageIcon> icons = new HashMap<>();

    public void updateBundleComp(Bundle b) {
      final JLabel c = (JLabel) getBundleComponent(b);

      if(c == null) {
        addBundle(new Bundle[]{b});
        return;
      }

      c.setToolTipText(Util.bundleInfo(b));

      final Icon icon = Util.getBundleIcon(b);

      c.setIcon(icon);

      c.invalidate();
      c.repaint();
      invalidate();
      repaint();
    }

    /**
     * Stores user preferences for this displayer.
     * I.e., the selected sort order.
     */
    public void storePrefs() {
      try {
        final Preferences prefs = getPrefs();

        String sortOrder;
        if (iconComparator == nameComparator) {
          sortOrder = "name";
        } else if (iconComparator == startLevelComparator) {
          sortOrder = "start_level";
        } else {
          // Default sort order is bundle id
          sortOrder = "id";
        }

        prefs.put(PREFS_KEY_SORT, sortOrder);
        prefs.flush();
      } catch (final Exception e) {
        errCount++;
        if(errCount < maxErr) {
          Activator.log.warn("Failed to store prefs for Large Icons Displayer: "
                             +"sort order.", e);
        }
      }
    }

    /**
     * Loads user preferences for this displayer.
     * I.e, the selected sort order.
     */
    public void loadPrefs() {
      try {
        final Preferences prefs = getPrefs();

        String sortOrder = Util.getProperty(SORT_ORDER_PROPERTY, null);
        if (null==sortOrder) {
          sortOrder = prefs.get(PREFS_KEY_SORT, "id");
        }

        if (sortOrder.equals("name")) {
          iconComparator = nameComparator;
        } else if (sortOrder.equals("start_level")) {
          iconComparator = startLevelComparator;
        } else {
          // Default sort order is bundle id
          iconComparator = null;
        }
      } catch (final Exception e) {
        errCount++;
        if(errCount < maxErr) {
          Activator.log.warn("Failed to load prefs for Large Icons Displayer: "
                             +"sort order.", e);
        }
      }
    }
  }


  static Comparator<Long> nameComparator = (l1, l2) -> {
    final Bundle b1 = Activator.getTargetBC_getBundle(l1);
    final Bundle b2 = Activator.getTargetBC_getBundle(l2);

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
  };

  static Comparator<Long> startLevelComparator = (l1, l2) -> {
    final Bundle b1 = Activator.getTargetBC_getBundle(l1);
    final Bundle b2 = Activator.getTargetBC_getBundle(l2);

    if (b1 == b2) {
      return 0;
    }
    if (b1 == null) {
      return -1;
    }
    if (b2 == null) {
      return 1;
    }
    final long bidDiff = b1.getBundleId() - b2.getBundleId();
    final int bidRes = bidDiff < 0 ? -1 : (bidDiff == 0 ? 0 : 1);

    final BundleStartLevel bsl1 = b1.adapt(BundleStartLevel.class);
    final BundleStartLevel bsl2 = b2.adapt(BundleStartLevel.class);
    if (bsl1 != null && bsl2 != null) {
      final int sl1 = bsl1.getStartLevel();
      final int sl2 = bsl2.getStartLevel();
      return sl1 == sl2 ? bidRes : sl1 - sl2;
    }
    return bidRes;
  };
}
