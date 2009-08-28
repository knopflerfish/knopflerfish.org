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

import org.osgi.framework.*;

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.*;
import java.awt.Container;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import java.util.List;
import java.util.Dictionary;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Comparator;
import java.io.*;
import java.net.URL;


public class LargeIconsDisplayer extends DefaultSwingBundleDisplayer {

  public LargeIconsDisplayer(BundleContext bc) {
    super(bc, "Large Icons", "Large icon display of bundles", false);
  }

  public void bundleChanged(BundleEvent ev) {
    super.bundleChanged(ev);

    //    System.out.println(getClass().getName() + ": #" + ev.getBundle().getBundleId());

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

    Color       selColor = new Color(200, 200, 255);

    public JLargeIcons() {
      setLayout(new BorderLayout());
      setBackground(Color.white);

      grid  = new GridLayout(0, 4);
      panel = new JPanel(grid);

      panel.setBackground(getBackground());

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
        };

      add(scroll, BorderLayout.CENTER);
    }

    public void addBundle(final Bundle b) {

      final long bid = b.getBundleId();

      if(null == getBundleComponent(b)) {
        JLabel c = new JLabel(Util.getBundleName(b),
                              bundleIcon,
                              SwingConstants.CENTER) {
            {
              addMouseListener(new MouseAdapter() {
                  public void mousePressed(MouseEvent ev) {
                    if((ev.getModifiers() & InputEvent.CTRL_MASK) != 0) {
                      bundleSelModel
                        .setSelected(bid, !bundleSelModel.isSelected(bid));
                    } else {
                      bundleSelModel.clearSelection();
                      bundleSelModel.setSelected(bid, true);
                    }
                    setBackground(getBackground());
                  }
                });
              addMouseListener(contextMenuListener);
              setBorder(null);
              setOpaque(true);
              setBackground(Color.yellow);
            }


            public Color getBackground() {

              try {
                boolean bSel = bundleSelModel != null
                  ? bundleSelModel.isSelected(b.getBundleId())
                  : false;

                return bSel
                  ? selColor
                  : JLargeIcons.this.getBackground();
              } catch (Exception e) {
                return Color.black;
              }
            }
          };

        //        System.out.println("created icon " + c.getText());

        c.setToolTipText(Util.bundleInfo(b));
        c.setVerticalTextPosition(AbstractButton.BOTTOM);
        c.setHorizontalTextPosition(AbstractButton.CENTER);

        c.setPreferredSize(new Dimension(96, 64));
        c.setBorder(null);
        c.setFont(getFont());

        synchronized(bundleMap) {
          bundleMap.put(new Long(b.getBundleId()), c);
        }

        updateBundleComp(b);

        rebuildPanel();

      }

    }

    Comparator nameComparator = new Comparator() {
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

    Comparator iconComparator = null;

    void rebuildPanel() {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            synchronized(bundleMap) {
              panel.removeAll();

              //            Comparator comp = nameComparator;

              Set set = new TreeSet(iconComparator);
              set.addAll(bundleMap.keySet());

              int w = 0;
              int h = 0;
              for(Iterator it = set.iterator(); it.hasNext(); ) {
                Long      bid = (Long)it.next();
                JComponent c   = (JComponent)bundleMap.get(bid);
                Dimension size = c.getPreferredSize();
                w = Math.max(w, size.width);
                h = Math.max(h, size.height);
              }


              Dimension size = scroll.getViewport().getExtentSize();

              if(size.width != 0) {
                grid.setColumns(size.width / w);
                grid.setRows(0);
              }

              for(Iterator it = set.iterator(); it.hasNext(); ) {
                Long      bid = (Long)it.next();
                Component c   = (Component)bundleMap.get(bid);
                panel.add(c);
              }
            }


            revalidate();
            repaint();
          }
        });
    }

    public void removeBundle(Bundle b) {
      synchronized(bundleMap) {
        bundleMap.remove(new Long(b.getBundleId()));
        icons.remove(b);
      }
      rebuildPanel();
    }

    JComponent getBundleComponent(Bundle b) {
      synchronized(bundleMap) {
        return (JComponent)bundleMap.get(new Long(b.getBundleId()));
      }
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

      Icon icon = (Icon)icons.get(b);

      if(icon == null) {
        URL appURL = null;
        String iconName = (String)b.getHeaders().get("Application-Icon");
        if(iconName == null) {
          iconName = "";
        }
        iconName = iconName.trim();

        if(iconName != null && !"".equals(iconName)) {
          try {
            appURL = b.getResource(iconName);
          } catch (Exception e) {
            Activator.log.error("Failed to load icon", e);
          }
        }

        //        System.out.println("#" + b.getBundleId() + ", appURL=" + appURL);
        try {
          if(Util.hasMainClass(b)) {
            icon = new BundleImageIcon(b,
                                       appURL != null ? appURL : getClass().getResource("/jarexec.gif"));
          } else if(Util.hasActivator(b)) {
            icon = new BundleImageIcon(b,
                                       appURL != null ? appURL : getClass().getResource("/bundle.png"));
          } else {
            icon = new BundleImageIcon(b,
                                       appURL != null ? appURL : getClass().getResource("/lib.png"));
          }
        } catch (Exception e) {
          Activator.log.error("Failed to load icon, appURL=" + appURL);
          icon = new BundleImageIcon(b, getClass().getResource("/bundle.png"));
        }
        icons.put(b, icon);
      }

      c.setIcon(icon);


      c.invalidate();
      c.repaint();
      invalidate();
      repaint();
    }
  }
}


