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

package org.knopflerfish.bundle.desktopawt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

class DesktopPanel extends Panel implements BundleListener {
  ScrollPane scroll;
  Toolbar    toolbar;
  Container  bundlePanel;
  Panel  cardPanel;
  CardLayout cardLayout;
  Console    console;
  StatusBar  statusBar;
  Container  openPanel;

  public static final int SORT_ID    = 0;
  public static final int SORT_NAME  = 1;
  public static final int SORT_STATE = 2;

  int       sortMode = SORT_ID;
  private final Hashtable<Bundle, BundleC> bundles  = new Hashtable<>();

  LF lf = LF.getLF();

  DesktopPanel() {
    super();
    setLayout(new BorderLayout());

    scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);

    scroll.setSize(new Dimension(250, 300));
    cardLayout   = new CardLayout();
    cardPanel    = new Panel();
    cardPanel.setLayout(cardLayout);

    bundlePanel  = new Panel();
    bundlePanel.setLayout(new GridLayout(0,1));
    console      = new Console();

    console.panel.text.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent ev) {
          String s = console.panel.text.getSelectedText();
          selectBundleFromText(s);
        }
      });

    openPanel = new InstallPanel();

    //    bundlePanel.setSize(new Dimension(200, 400));
    toolbar = new Toolbar();

    scroll.add(bundlePanel);

    cardPanel.add(scroll, "bundles");
    cardLayout.addLayoutComponent(scroll, "bundles");

    cardPanel.add(console.panel, "console");
    cardLayout.addLayoutComponent(console.panel, "console");

    cardPanel.add(openPanel, "open");
    cardLayout.addLayoutComponent(openPanel, "open");

    statusBar = new StatusBar();

    add(cardPanel, BorderLayout.CENTER);
    add(toolbar, BorderLayout.NORTH);
    add(statusBar, BorderLayout.SOUTH);


    setBackground(lf.bgColor);
    bundlePanel.setBackground(Color.white);

    Activator.bc.addBundleListener(this);
    
    Bundle[] bl = Activator.bc.getBundles();
    for(int i = 0; bl != null && i < bl.length; i++) {
      this.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bl[i]));
    }

    // This will sort all displayed bundles
    showBundles(sortMode);
  }

  public void bundleChanged(BundleEvent ev) {
    switch(ev.getType()) {
    case BundleEvent.INSTALLED:
      addBundle(ev.getBundle());
      rebuildBundles();
      break;
    case BundleEvent.UNINSTALLED:
      removeBundle(ev.getBundle());
      rebuildBundles();
      break;
    }
    //          System.out.println(ev.getBundle() + ", " + ev.getType());
    bundleUpdated(ev.getBundle());
  }
  
  public void removeBundle(Bundle b) {
    synchronized(bundles) {
      BundleC bc = bundles.get(b);
      if(bc != null) {
        bundles.remove(b);
        rebuildBundles();
      }
    }
  }

  public void addBundle(Bundle b) {
    synchronized(bundles) {
      if(!bundles.containsKey(b)) {
        BundleC bc = new BundleC(b);
        bundles.put(b, bc);
        rebuildBundles();
      }
    }
  }


  void insertBundle(Vector<Bundle> v, Bundle b, Comp comp) {
    for(int i = 0; i < v.size(); i++) {
      Bundle b0 = v.elementAt(i);
      if(comp.compare(b, b0) <= 0) {
        v.insertElementAt(b, i);
        return;
      }
    }
    v.addElement(b);
  }

  void rebuildBundles() {
    synchronized(bundles) {
      int sort = sortMode;
      try {
        Comp comp = null;
        if(sort == SORT_ID) {
          comp = compId;
        } else if(sort == SORT_NAME) {
          comp = compName;
        } else if(sort == SORT_STATE) {
          comp = compState;
        }
        
        bundlePanel.removeAll();
        Vector<Bundle> sorted = new Vector<>();
        for (Enumeration<Bundle> e = bundles.keys(); e.hasMoreElements();) {
          Bundle b = e.nextElement();
          insertBundle(sorted, b, comp);
        }
        
        for(int i = 0; i < sorted.size(); i++) {
          Bundle  b  = sorted.elementAt(i);
          BundleC bc = bundles.get(b);
          if(bc == null) {
            bc = new BundleC(b);
            bundles.put(b, bc);
          }
          bundlePanel.add(bc);
        }
        invalidate();
        bundlePanel.doLayout();
        bundlePanel.repaint();
        repaint();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  void bundleUpdated(Bundle b) {
    BundleC bc = bundles.get(b);
    if(bc != null) {
      //      System.out.println("update " + bc);
      bc.bundleUpdated();
    }
  }

  private Stream<BundleC> streamSelectedBundles() {
    if (bundles.size() == 0) {
      statusBar.setMessage("No bundles selected");
      return Stream.empty();
    }
    return streamSelectedBundlesWithoutCheck();
  }

  private Stream<BundleC> streamSelectedBundlesWithoutCheck() {
    return bundles.values().stream().filter(BundleC::isSelected);
  }

  void unselectAll() {
    streamSelectedBundlesWithoutCheck().forEach(bc -> {
      bc.bFocus = false;
      bc.setSelected(false);
    });
  }

  public void startBundles() {
    streamSelectedBundles().forEach(bc -> {
      try {
        bc.getBundle().start();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public void openBundle() {
    cardLayout.show(cardPanel, "open");
  }
  
  public void stopBundles() {
    streamSelectedBundles().forEach(bc -> {
      try {
        bc.getBundle().stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public void uninstallBundles() {
    streamSelectedBundles().forEach(bc -> {
      try {
        bc.getBundle().uninstall();
        rebuildBundles();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public void updateBundles() {
    streamSelectedBundles().forEach(bc -> {
      try {
        bc.getBundle().update();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }


  void selectBundleFromText(String s) {
    unselectAll();
    if(s != null && s.length() > 0) {
      s = s.trim();
      try {
        long bid = Long.parseLong(s);
        for(Enumeration<Bundle> e = bundles.keys(); e.hasMoreElements();) {
          Bundle b = e.nextElement();
          BundleC bc = bundles.get(b);
          if(b.getBundleId() == bid) {
            bc.setSelected(true);
          }
        }
      } catch (Exception ignore) {

      }
    }
  }

  void showBundles(int sort) {
    sortMode = sort;
    rebuildBundles();
    cardLayout.show(cardPanel, "bundles");
  }

  void showConsole() {
    cardLayout.show(cardPanel, "console");
  }

  void showOBR() {
    cardLayout.show(cardPanel, "obr");
  }

  class BundleC extends DBContainer {
    boolean      bSelected = false;
    boolean      bFocus = false;
    String       lab;
    String       lab2;
    Bundle       b;

    Image        bundleImage;
    Dimension    imgSize;


    long maxPaintTime = 30;

    BundleC(Bundle _b) {
      super();

      this.b = _b;

      bundleImage = Util.hasActivator(b) 
        ? Desktop.bundleImage
        : Desktop.libImage;

      imgSize = new Dimension(bundleImage.getWidth(null),
                              bundleImage.getHeight(null));
      
      MouseListener mouseListener = new MouseAdapter() {
          public void mouseClicked(MouseEvent ev) {
            unselectAll();
            setSelected(true);
          }
          public void mouseEntered(MouseEvent ev) {
            if(paintTime < maxPaintTime) {
              setFocus(true);
            }
          }
          public void mouseExited(MouseEvent ev) {
            if(paintTime < maxPaintTime) {
              setFocus(false);
            }
          }
        };
      

      lab  = Util.getBundleName(b);
      lab2 = "";

      addMouseListener(mouseListener);

      bundleUpdated();
    }

    public Bundle getBundle() {
      return b;
    }

    Color alphaCol = new Color(0,0,0,255);
    
    void paintImage(Graphics g) {
      Image overlay = null;
      
      switch(b.getState()) {
      case Bundle.ACTIVE:
        overlay = Desktop.activeIcon;
        break;
      case Bundle.INSTALLED:
        break;
      case Bundle.RESOLVED:
        break;
      case Bundle.STARTING:
        break;
      case Bundle.STOPPING:
        break;
      case Bundle.UNINSTALLED:
        break;
      default:
      }
      
      int x = 0;
      int y = 0;
        
      g.drawImage(bundleImage, x, y, null);

      if(overlay != null) {
        int w = overlay.getWidth(null);
        int h = overlay.getHeight(null);
        
        
        int x1 = x + (imgSize.width - w - 2);
        int y1 = y + (imgSize.height -  h - 2);
        
        
        g.drawImage(overlay, x1, y1, alphaCol, null);
      }
    }
    
    public void paintComponent(Graphics g) {
      count++;

      Dimension size = getSize();

      g.setColor(getBackground());
      g.fillRect(0,0, size.width, size.height);
        
      int left = 2 + bundleImage.getWidth(null);
      int ypos = 2;
      
      paintImage(g);
      
      g.setColor(Color.black);
      g.setFont(lf.defaultFont);
      ypos += g.getFont().getSize();
      g.drawString(lab,
                   left, 
                   ypos);
      
      ypos += g.getFont().getSize() + 2;
      
      g.setColor(Color.gray);
      g.setFont(lf.smallFont);
      g.drawString(lab2,
                   left,
                   ypos);
    }
    
    public Dimension getPreferredSize() {
      return imgSize;
    }

    void bundleUpdated() {
      int maxLen = 25;
      String desc = Util.getHeader(b, "Bundle-Description", "");
      if(desc == null) {
        desc = "";
      }

      if(desc.length() > maxLen) {
        desc = desc.substring(0, maxLen) + "...";
      }

      lab2 = desc + 
        " (#" + b.getBundleId() + ", " + 
        Util.stateName(b.getState()) + ")";

      bNeedRedraw = true;
      repaint();
    }

    public boolean isSelected() {
      return bSelected;
    }
    
    public void setFocus(boolean b) {
      if(bFocus == b) {
        return;
      }
      bFocus = b;
      bNeedRedraw = true;
      setSelected(isSelected());
    }

    public boolean isFocus() {
      return bFocus;
    }


    public void setSelected(boolean b) {
      if(bSelected != b) {
        bNeedRedraw = true;
      }
      bSelected = b;
      
      Color c = isSelected() ? LF.getLF().stdSelectedCol : Color.white;
      if(isFocus()) {
        c = Util.rgbInterpolate(c, Color.gray, .1);
      }
      setBackground(c);
      
      repaint();
      statusBar.showBundles();
    }

  }

  Container shutdownPanel = null;

  void shutdown() {
    if(shutdownPanel == null) {
      shutdownPanel = new ShutdownPanel();
      cardPanel.add(shutdownPanel, "shutdown");
      shutdownPanel.doLayout();
      shutdownPanel.invalidate();
      cardLayout.addLayoutComponent(shutdownPanel, "shutdown");
    }
    cardLayout.show(cardPanel, "shutdown");
  }

  static final int ACTION_ID_BUNDLES_ID = 0;
  static final int ACTION_ID_BUNDLES_NAME = 1;
  static final int ACTION_ID_BUNDLES_STATE = 2;
  static final int ACTION_ID_CONSOLE = 3;
  static final int ACTION_ID_OBR = 4;
  static final int ACTION_ID_INSTALL = 5;
  static final int ACTION_ID_SHUTDOWN = 6;
  static final int ACTION_ID_REFRESH = 7;
  static final int ACTION_ID_BUNDLE_START = 20;
  static final int ACTION_ID_BUNDLE_STOP = 21;
  static final int ACTION_ID_BUNDLE_UPDATE = 22;
  static final int ACTION_ID_BUNDLE_UNINSTALL = 23;

  void handleAction(int actionId) {
    switch(actionId) {
    case ACTION_ID_BUNDLES_ID:
      showBundles(SORT_ID);
      break;
    case ACTION_ID_BUNDLES_NAME:
      showBundles(SORT_NAME);
      break;
    case ACTION_ID_BUNDLES_STATE:
      showBundles(SORT_STATE);
      break;
    case ACTION_ID_CONSOLE:
      showConsole();
      break;
    case ACTION_ID_OBR:
      showOBR();
      break;
    case ACTION_ID_INSTALL:
      openBundle();
      break;
    case ACTION_ID_REFRESH:
      refreshBundles();
      break;
    case ACTION_ID_SHUTDOWN:
      shutdown();
      break;      
    case ACTION_ID_BUNDLE_START:
      startBundles();
      break;
    case ACTION_ID_BUNDLE_STOP:
      stopBundles();
      break;
    case ACTION_ID_BUNDLE_UPDATE:
      updateBundles();
      break;
    case ACTION_ID_BUNDLE_UNINSTALL:
      uninstallBundles();
      break;
    }
  }

  class ActionMenuItem extends MenuItem implements ActionListener {
    int actionId;
    ActionMenuItem(String name, int actionId) {
      super(name);
      this.actionId = actionId;
      addActionListener(this);
    }
    public void actionPerformed(ActionEvent ev) {
      handleAction(actionId);
    }
  }

  class ActionImageLabel extends ImageLabel implements ActionListener {
    int actionId;
    String tt;
    ActionImageLabel(String name, Color bg, String tt,int actionId) {
      super(name, 2, bg);
      this.actionId = actionId;
      this.tt       = tt;
      addActionListener(this);
    }
    public void actionPerformed(ActionEvent ev) {
      handleAction(actionId);
    }
    public void setFocus(boolean b) {
      super.setFocus(b);
      statusBar.setMessage(b ? tt : "");
    }
  }

  class Toolbar extends Panel {
    LF lf = LF.getLF();
    
    Toolbar() {
      super(new FlowLayout(FlowLayout.LEFT));
      
      setBackground(lf.bgColor);
      
      ImageLabel viewButton = new ImageLabel("/view-select.png", 2, getBackground()) {
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Select view" : "");
          }
        };
      
      viewPopupMenu = new PopupMenu();
      add(viewPopupMenu);

      viewPopupMenu.add(new ActionMenuItem("Bundles (id)",
                                           ACTION_ID_BUNDLES_ID));
      viewPopupMenu.add(new ActionMenuItem("Bundles (name)", 
                                           ACTION_ID_BUNDLES_NAME));
      viewPopupMenu.add(new ActionMenuItem("Bundles (state)", 
                                           ACTION_ID_BUNDLES_STATE));
      viewPopupMenu.add(new ActionMenuItem("Console", 
                                           ACTION_ID_CONSOLE));
      viewPopupMenu.add(new ActionMenuItem("OBR (NYI)", 
                                           ACTION_ID_OBR));
      viewPopupMenu.add(new ActionMenuItem("Shutdown", 
                                           ACTION_ID_SHUTDOWN));
      viewPopupMenu.add(new ActionMenuItem("Refresh", 
                                           ACTION_ID_REFRESH));

      add(viewButton);

      viewButton.addMouseListener(new MouseAdapter() 
        {
          public void mousePressed(MouseEvent e) {
            showPopup(e);
          }
          
          public void mouseReleased(MouseEvent e) {
            showPopup(e);
          }
          
          private void showPopup(MouseEvent e) {
            if (viewPopupMenu != null) {
              Component comp = e.getComponent();
              viewPopupMenu.show(comp, 0, comp.getSize().height);
            }
          }
        });

      add(new ActionImageLabel("/openurl.png", getBackground(), 
                               "Install bundles",
                               ACTION_ID_INSTALL));
      add(new ActionImageLabel("/player_play.png", getBackground(), 
                               "Start bundles",
                               ACTION_ID_BUNDLE_START));
      add(new ActionImageLabel("/player_stop.png", getBackground(), 
                               "Stop bundles",
                               ACTION_ID_BUNDLE_STOP));
      add(new ActionImageLabel("/system-software-update.png", getBackground(), 
                               "Update bundles",
                               ACTION_ID_BUNDLE_UPDATE));
      add(new ActionImageLabel("/player_eject.png", getBackground(), 
                               "Uninstall bundles",
                               ACTION_ID_BUNDLE_UNINSTALL));

    }
  }  

  void refreshBundles() {
    ServiceReference<PackageAdmin> sr = Activator.bc.getServiceReference(PackageAdmin.class);
    if(sr != null) {
      PackageAdmin packageAdmin = null;
      try {
        packageAdmin = Activator.bc.getService(sr);
        if(packageAdmin != null) {
          packageAdmin.refreshPackages(null);
        }
      } finally {
        if(packageAdmin != null) {
          Activator.bc.ungetService(sr);
        }
      }
    }
  }

  PopupMenu viewPopupMenu;

  class StatusBar extends Container {
    Dimension pSize = new Dimension(100, lf.defaultFont.getSize() + 4);
    
    String msg = "";

    public Dimension getPreferredSize() {
      return pSize;
    }

    public void showBundles() {
      int nTotal    = bundles.size();
      int nSelected = 0;

      StringBuilder sb = new StringBuilder();
      for(Enumeration<Bundle> e = bundles.keys(); e.hasMoreElements();) {
        Bundle b = e.nextElement();
        BundleC bc = bundles.get(b);
        if(bc.isSelected()) {
          nSelected++;
          if(sb.length() > 0) {
            sb.append(", ");
          }
          sb.append("#").append(b.getBundleId()).append("/").append(Util.getBundleName(b));
        }
      }
      if(sb.length() > 20) {
        sb.setLength(20);
        sb.append("...");
      }
      String msg = "Total " + nTotal + ", " + nSelected + " selected" + 
        " " + sb;
      setMessage(msg);
    }
    
    public void setMessage(String msg) {
      this.msg = msg;
      repaint();
    }

    public void paint(Graphics g) {
      super.paint(g);

      g.setFont(lf.defaultFont);
      g.setColor(lf.ttText);
      if(msg != null) {
        g.drawString(msg, 2, lf.defaultFont.getSize() + 2);
      }
      g.setColor(Color.gray);
    }
    
  }

  interface Comp {
    int compare(Bundle b0, Bundle b1);
  }
  Comp compName = (b0, b1) -> {
    String s0 = Util.getBundleName(b0).toLowerCase();
    String s1 = Util.getBundleName(b1).toLowerCase();
    return s0.compareTo(s1);
  };
  
  Comp compId = (b0, b1) -> (int)(b0.getBundleId() - b1.getBundleId());
  
  Comp compState = (b0, b1) -> (int)(b0.getState() - b1.getState());
  
}

