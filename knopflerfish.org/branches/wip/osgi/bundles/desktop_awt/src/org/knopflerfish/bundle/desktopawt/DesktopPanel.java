/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;
import java.awt.*;
import java.awt.event.*;

import java.util.*;

class DesktopPanel extends Panel {
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
  Hashtable bundles  = new Hashtable();

  LF lf = LF.getLF();

  DesktopPanel() {
    super();
    setLayout(new BorderLayout());

    scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);

    scroll.setSize(new Dimension(250, 300));
    cardLayout   = new CardLayout();
    cardPanel    = new Panel();
    cardPanel.setLayout(cardLayout);

    bundlePanel  = new DBContainer();
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
    cardLayout.addLayoutComponent("bundles", scroll);

    cardPanel.add(console.panel, "console");
    cardLayout.addLayoutComponent("console", console.panel);

    cardPanel.add(openPanel, "opem");
    cardLayout.addLayoutComponent("open", openPanel);

    statusBar = new StatusBar();

    add(cardPanel, BorderLayout.CENTER);
    add(toolbar, BorderLayout.NORTH);
    add(statusBar, BorderLayout.SOUTH);


    setBackground(lf.bgColor);
    bundlePanel.setBackground(Color.white);

    BundleListener listener = new BundleListener() {
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
      };
    
    Activator.bc.addBundleListener(listener);
    
    Bundle[] bl = Activator.bc.getBundles();
    for(int i = 0; bl != null && i < bl.length; i++) {
      listener.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bl[i]));
    }

    // This will sort all displayed bundles
    showBundles(sortMode);
  }



  public void removeBundle(Bundle b) {
    synchronized(bundles) {
      BundleC bc = (BundleC)bundles.get(b);
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


  interface Comp {
    public int compare(Bundle b0, Bundle b1);
  }

  Comp compName = new Comp() {
      public int compare(Bundle b0, Bundle b1) {
        String s0 = Util.getBundleName(b0).toLowerCase();
        String s1 = Util.getBundleName(b1).toLowerCase();

        return s0.compareTo(s1);
      }
    };

  Comp compId = new Comp() {
      public int compare(Bundle b0, Bundle b1) {
        return (int)(b0.getBundleId() - b1.getBundleId());
      }
    };

  Comp compState = new Comp() {
      public int compare(Bundle b0, Bundle b1) {
        return (int)(b0.getState() - b1.getState());
      }
    };
  
  void insertBundle(Vector v, Bundle b, Comp comp) {
    for(int i = 0; i < v.size(); i++) {
      Bundle b0 = (Bundle)v.elementAt(i);
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
        Comp comp = compId;
        if(sort == SORT_ID) {
          comp = compId;
        } else if(sort == SORT_NAME) {
          comp = compName;
        } else if(sort == SORT_STATE) {
          comp = compState;
        }
        
        bundlePanel.removeAll();
        Vector sorted = new Vector();
        for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
          Bundle  b  = (Bundle)e.nextElement();
          BundleC bc = (BundleC)bundles.get(b);
          
          insertBundle(sorted, b, comp);
        }
        
        for(int i = 0; i < sorted.size(); i++) {
          Bundle  b  = (Bundle)sorted.elementAt(i);
          BundleC bc = (BundleC)bundles.get(b);
          if(bc == null) {
            bc = new BundleC(b);
            bundles.put(b, bc);
          }
          bundlePanel.add(bc);
        }
        invalidate();
        bundlePanel.doLayout();
        repaint();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  void bundleUpdated(Bundle b) {
    BundleC bc = (BundleC)bundles.get(b);
    if(bc != null) {
      //      System.out.println("update " + bc);
      bc.bundleUpdated();
    }
  }

  void unselectAll() {
    for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
      Bundle  b  = (Bundle)e.nextElement();
      BundleC bc = (BundleC)bundles.get(b);
      if(bc.isSelected()) {
        bc.bFocus = false;
        bc.setSelected(false);
      }
    }
  }

  public void startBundles() {
    if(bundles.size() == 0) {
      statusBar.setMessage("No bundles selected");
      return;
    }
    for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
      Bundle b = (Bundle)e.nextElement();
      BundleC bc = (BundleC)bundles.get(b);
      if(bc.isSelected()) {
        try {
          b.start();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  public void openBundle() {
     cardLayout.show(cardPanel, "open");
  }

  public void stopBundles() {
    if(bundles.size() == 0) {
      statusBar.setMessage("No bundles selected");
      return;
    }
    for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
      Bundle b = (Bundle)e.nextElement();
      BundleC bc = (BundleC)bundles.get(b);
      if(bc.isSelected()) {
        try {
          b.stop();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  public void uninstallBundles() {
    if(bundles.size() == 0) {
      statusBar.setMessage("No bundles selected");
      return;
    }
    for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
      Bundle b = (Bundle)e.nextElement();
      BundleC bc = (BundleC)bundles.get(b);
      if(bc.isSelected()) {
        try {
          b.uninstall();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  public void updateBundles() {
    if(bundles.size() == 0) {
      statusBar.setMessage("No bundles selected");
      return;
    }
    for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
      Bundle b = (Bundle)e.nextElement();
      BundleC bc = (BundleC)bundles.get(b);
      if(bc.isSelected()) {
        try {
          b.update();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }


  void selectBundleFromText(String s) {
    unselectAll();
    if(s != null && s.length() > 0) {
      s = s.trim();
      try {
        long bid = Long.parseLong(s);
        for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
          Bundle b = (Bundle)e.nextElement();
          BundleC bc = (BundleC)bundles.get(b);
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

  void showInstall() {
    cardLayout.show(cardPanel, "install");
  }


  void showInfo(BundleC bc) {
    if(bc != null) {
    }
  }

  class BundleC extends DBContainer {
    BundleImageC bic;
    boolean      bSelected = false;
    boolean      bFocus = false;
    BundleImageC bc;
    String       lab;
    String       lab2;
    Bundle       b;

    BundleC(Bundle _b) {
      super();

      this.b = _b;

      bic = new BundleImageC(b, 
                             Util.hasActivator(b) 
                             ? "/bundle.gif"
                             : "/lib.gif");
                             
      
      MouseListener mouseListener = new MouseAdapter() {
          public void mouseClicked(MouseEvent ev) {
            if(ev.getSource() != bic) {
              if(0 != (ev.getModifiers() & ActionEvent.CTRL_MASK)) {
                setSelected(!isSelected());
              } else {
                unselectAll();
                setSelected(true);
              }
            }
          }
          public void mouseEntered(MouseEvent ev) {
            showInfo(BundleC.this);
            setFocus(true);
          }
          public void mouseExited(MouseEvent ev) {
            showInfo(null);
            setFocus(false);
          }
        };
      

      lab  = Util.getBundleName(b);
      lab2 = "";

      addMouseListener(mouseListener);

      bundleUpdated();
    }



    public void paintComponent(Graphics g) {

      Dimension size = getSize();

      g.setColor(getBackground());
      g.fillRect(0,0, size.width, size.height);
      
      int left = 2 + bic.img.getWidth(null);
      int ypos = 2;
      
      bic.setSize(bic.img.getWidth(null),
                  bic.img.getHeight(null));
      bic.setLocation(0, 0);
      bic.setBackground(getBackground());
      bic.paint(g);
      
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
      
      ypos += g.getFont().getSize() + 2;
      
    }
    
    public Dimension preferredSize() {
      return getPreferredSize();
    }

    public Dimension getPreferredSize() {
      Dimension d = bic.getPreferredSize();
      return d;
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


  class Toolbar extends Panel {
    LF lf = LF.getLF();
    
    Toolbar() {
      super(new FlowLayout(FlowLayout.LEFT));
      
      setBackground(lf.bgColor);
      
      
      
      ImageLabel viewButton = new ImageLabel("/view_select.gif", 2, getBackground()) {
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Select view" : "");
          }
        };
      
      viewPopupMenu = new PopupMenu();
      add(viewPopupMenu);

      MenuItem item;
      item = new MenuItem("Bundles (id)");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            showBundles(SORT_ID);
          }
        });
      viewPopupMenu.add(item);

      item = new MenuItem("Bundles (name)");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            showBundles(SORT_NAME);
          }
        });
      viewPopupMenu.add(item);
      item = new MenuItem("Bundles (state)");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            showBundles(SORT_STATE);
          }
        });
      viewPopupMenu.add(item);


      item = new MenuItem("Console");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            showConsole();
          }
        });
      viewPopupMenu.add(item);

      item = new MenuItem("OBR (NYI)");
      item.setEnabled(false);
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            showOBR();
          }
        });
      viewPopupMenu.add(item);

      item = new MenuItem("Shutdown");
      item.setEnabled(true);
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            try {
              Activator.bc.getBundle(0).stop();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
      viewPopupMenu.add(item);

      item = new MenuItem("Refresh");
      item.setEnabled(true);
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            refreshBundles(null);
          }
        });
      viewPopupMenu.add(item);
 
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

            add(new ImageLabel("/open.gif", 2, getBackground()) {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              openBundle();
            }
          });
      }
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Install bundles" : "");
          }
        });
      add(new ImageLabel("/player_play.gif", 2, getBackground()) {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              startBundles();
            }
          });
      }
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Start bundles" : "");
          }
        });
      add(new ImageLabel("/player_stop.gif", 2, getBackground()) {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              stopBundles();
            }
          });
      }
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Stop bundles" : "");
          }
        });
      add(new ImageLabel("/update.gif", 2, getBackground())  {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              updateBundles();
            }
          });
      }
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Update bundles" : "");
          }
        });
      add(new ImageLabel("/player_eject.gif", 2, getBackground()) {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              uninstallBundles();
            }
          });
      }
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Uninstall bundles" : "");
          }
        });

    }
  }  

  void refreshBundles(Bundle[] bl) {
    ServiceReference sr = Activator.bc.getServiceReference(PackageAdmin.class.getName());
    if(sr != null) {
      PackageAdmin packageAdmin = null;
      try {
        packageAdmin = (PackageAdmin)Activator.bc.getService(sr);
        if(packageAdmin != null) {
          if(bl != null && bl.length == 0) {
            bl = null;
          }
          packageAdmin.refreshPackages(bl);
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

    public Dimension preferredSize() {
      return getPreferredSize();
    }
    
    public Dimension getPreferredSize() {
      return pSize;
    }

    public void showBundles() {
      int nTotal    = bundles.size();
      int nSelected = 0;

      StringBuffer sb = new StringBuffer();
      for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
        Bundle b = (Bundle)e.nextElement();
        BundleC bc = (BundleC)bundles.get(b);
        if(bc.isSelected()) {
          nSelected++;
          if(sb.length() > 0) {
            sb.append(", ");
          }
          sb.append("#" + b.getBundleId() + "/" + Util.getBundleName(b));
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
  
}
  

