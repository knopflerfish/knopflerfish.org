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
import java.awt.*;
import java.awt.event.*;

import java.util.*;

class DesktopPanel extends Panel {
  ScrollPane scroll;
  Toolbar    toolbar;
  Panel      bundlePanel;
  Panel      cardPanel;
  CardLayout cardLayout;
  Console    console;
  StatusBar  statusBar;

  public static final int SORT_ID   = 0;
  public static final int SORT_NAME = 1;

  LF lf = LF.getLF();

  DesktopPanel() {
    super(new BorderLayout());

    scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);

    scroll.setSize(new Dimension(250, 300));
    cardLayout   = new CardLayout();
    cardPanel    = new Panel(cardLayout);

    bundlePanel  = new Panel(new GridLayout(0,1));
    console = new Console();

    console.panel.text.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent ev) {
          String s = console.panel.text.getSelectedText();
          selectBundleFromText(s);
        }
      });
                                  

    //    bundlePanel.setSize(new Dimension(200, 400));
    toolbar = new Toolbar();

    scroll.add(bundlePanel);

    cardPanel.add(scroll, "bundles");
    cardLayout.addLayoutComponent("bundles", scroll);

    cardPanel.add(console.panel, "console");
    cardLayout.addLayoutComponent("console", console.panel);

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
            bundlePanel.invalidate();
            bundlePanel.repaint();
            break;
          case BundleEvent.UNINSTALLED:
            removeBundle(ev.getBundle());
            bundlePanel.invalidate();
            bundlePanel.repaint();
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

  }

  Hashtable bundles = new Hashtable();

  public void removeBundle(Bundle b) {
    BundleC bc = (BundleC)bundles.get(b);
    if(bc != null) {
      bundles.remove(b);
      bundlePanel.remove(bc);
      bundlePanel.invalidate();
      bundlePanel.repaint();
    }
  }

  public void addBundle(Bundle b) {
    if(!bundles.containsKey(b)) {
      BundleC bc = new BundleC(b);
      bundles.put(b, bc);
      bundlePanel.add(bc);
      bundlePanel.invalidate();
      bundlePanel.repaint();

    }
  }
  
  void bundleUpdated(Bundle b) {
    BundleC bc = (BundleC)bundles.get(b);
    if(bc != null) {
      //      System.out.println("update " + bc);
      bc.update();
    }
  }

  void unselectAll() {
    for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
      Bundle  b  = (Bundle)e.nextElement();
      BundleC bc = (BundleC)bundles.get(b);
      if(bc.isSelected()) {
        bc.setSelected(false);
      }
    }
  }

  public void startBundles() {
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
  }

  public void stopBundles() {
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

  class BundleC extends Panel {
    BundleImageC bic;
    boolean      bSelected = false;
    boolean      bFocus = false;
    BundleImageC bc;
    Label        lab;
    Label        lab2;
    Bundle       b;

    public void repaint() {
      //      System.out.println("BC.repaint() " + this.b.getBundleId());
      bic.repaint();
      lab.repaint();
      lab2.repaint();
    }

    public void paint(Graphics g) {
      super.paint(g);
      //      System.out.println("BC.repaint() " + this.b.getBundleId());
    }

    BundleC(Bundle _b) {
      super(new BorderLayout());
      this.b = _b;

      bic = new BundleImageC(b, 
                             Util.hasActivator(b) 
                             ? "/bundle.png"
                             : "/lib.png");
                             
      
      ActionListener actionListener = new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            if(0 != (ev.getModifiers() & ActionEvent.CTRL_MASK)) {
              setSelected(!isSelected());
            } else {
              unselectAll();
              setSelected(true);
            }
          }
        };
      
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
      

      Panel mainP = new Panel(new BorderLayout());

      lab  = new Label(Util.getBundleName(b));
      lab.setFont(lf.defaultFont);
      lab.setForeground(Color.black);

      lab2 = new Label();

      lab2.setFont(lf.smallFont);
      lab2.setForeground(Color.gray);

      bic.addMouseListener(mouseListener);
      bic.addActionListener(actionListener);
      lab.addMouseListener(mouseListener);
      lab2.addMouseListener(mouseListener);
      
      add(bic, BorderLayout.WEST);
      mainP.add(lab, BorderLayout.CENTER);
      mainP.add(lab2, BorderLayout.SOUTH);
      add(mainP, BorderLayout.CENTER);

      update();
    }
    
    void update() {
      int maxLen = 25;
      String desc = Util.getHeader(b, "Bundle-Description", "");
      if(desc == null) {
        desc = "";
      }

      if(desc.length() > maxLen) {
        desc = desc.substring(0, maxLen) + "...";
      }

      lab2.setText(desc + 
                   " (#" + b.getBundleId() + ", " + 
                   Util.stateName(b.getState()) + ")");
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
      setSelected(isSelected());
      bic.setFocus(b);
      repaint();
    }

    public boolean isFocus() {
      return bFocus;
    }


    public void setSelected(boolean b) {
      if(bSelected == b) {
        repaint();
      }
      bSelected = b;
      
      Color c = isSelected() ? LF.getLF().stdSelectedCol : Color.white;
      if(isFocus()) {
        c = Util.rgbInterpolate(c, Color.gray, .1);
      }
      bic.setBackground(c);
      lab.setBackground(c);
      lab2.setBackground(c);
      
      repaint();
      statusBar.showBundles();
    }
  }


  class Toolbar extends Panel {
    LF lf = LF.getLF();
    
    Toolbar() {
      super(new FlowLayout(FlowLayout.LEFT));
      
      setBackground(lf.bgColor);
      
      add(new ImageLabel("/open.png", 2, getBackground()) {{
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              openBundle();
            }
          });
      }
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Install bundles (NYI)" : "");
          }
        });
      add(new ImageLabel("/player_play.png", 2, getBackground()) {{
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
      add(new ImageLabel("/player_stop.png", 2, getBackground()) {{
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
      add(new ImageLabel("/update.png", 2, getBackground())  {{
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
      add(new ImageLabel("/player_eject.png", 2, getBackground()) {{
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
      
      
      ImageLabel viewButton = new ImageLabel("/view_select.png", 2, getBackground()) {
          public void setFocus(boolean b) {
            super.setFocus(b);
            statusBar.setMessage(b ? "Select view" : "");
          }
        };
      
      viewPopupMenu = new PopupMenu();
      add(viewPopupMenu);

      MenuItem item = new MenuItem("Bundles");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            showBundles(SORT_NAME);
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
    }
  }  

  PopupMenu viewPopupMenu;

  class StatusBar extends Panel {
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

      for(Enumeration e = bundles.keys(); e.hasMoreElements();) {
        Bundle b = (Bundle)e.nextElement();
        BundleC bc = (BundleC)bundles.get(b);
        if(bc.isSelected()) {
          nSelected++;
        }
      }
      String msg = "Total " + nTotal + ", selected " + nSelected;
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
  

