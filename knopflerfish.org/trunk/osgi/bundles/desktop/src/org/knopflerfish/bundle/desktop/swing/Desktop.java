/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.prefs.Preferences;
import javax.swing.ToolTipManager;

import org.knopflerfish.bundle.desktop.swing.console.ConsoleSwing;
import org.knopflerfish.service.desktop.BundleSelectionListener;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.knopflerfish.service.desktop.DefaultBundleSelectionModel;
import org.knopflerfish.service.desktop.SwingBundleDisplayer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The big one. This class displays the main desktop frame, menues, console
 * and listens for all registered
 * <tt>SwingBundleDisplayer</tt> services. These are used to create
 * JComponents, which are attached to the main panels.
 */
public class Desktop
  implements
    BundleListener,
    FrameworkListener,
    DropTargetListener,
    BundleSelectionListener
{

  PackageManager   pm;
  JFrame           frame;

  Container        contentPane;

  boolean alive = false;

  JCardPane          bundlePanel;
  JTabbedPane        detailPanel;
  JTabbedPane        consolePanel;

  static Icon          updateIcon;
  static Icon          startIcon;
  static ImageIcon          emptyIcon;
  static Icon          stopIcon;
  static ImageIcon          uninstallIcon;
  static ImageIcon          installIcon;

  static ImageIcon          magPlusIcon;
  static ImageIcon          magMinusIcon;
  static ImageIcon          magFitIcon;
  static ImageIcon          mag1to1Icon;
  static ImageIcon          reloadIcon;

  static ImageIcon          arrowUpIcon;
  static ImageIcon          arrowDownIcon;

  static ImageIcon          arrowUp2Icon;
  static ImageIcon          arrowDown2Icon;

  static ImageIcon          viewIcon;

  static ImageIcon          openIcon;
  static ImageIcon          openURLIcon;
  static ImageIcon          saveIcon;

  static ImageIcon          prevIcon;
  static ImageIcon          nextIcon;
  static ImageIcon          connectIcon;
  static ImageIcon          connectIconLarge;

  static ImageIcon          tipIcon;
  static ImageIcon          floatIcon;

  JToolBar           toolBar;
  StatusBar          statusBar;
  JMenuBar           menuBar;

  JMenuItem          menuRemote;
  JButton            buttonRemote;

  public JCheckBoxMenuItem  logCheckBox = null;

  BundleSelectionModel bundleSelModel = new DefaultBundleSelectionModel();

  ListSelectionModel bundleSelection;

  ConsoleSwing       consoleSwing;

  JSplitPane          splitPane;
  JSplitPane          splitPaneHoriz;
  //  BundleInfoDisplayer displayHTML;

  LFManager lfManager;
  LookAndFeelMenu lfMenu;

  ServiceTracker dispTracker;
  ServiceTracker slTracker;
  ServiceTracker pkgTracker;

  JButton viewSelection;

  static Desktop theDesktop;

  Set sizesavers = new HashSet();

  // Check that we are on Mac OS X.  This is crucial to loading and
  // using the OSXAdapter class.
  public static boolean bMacOS = OSXAdapter.isMacOSX();

  public Desktop() {
    theDesktop = this;
  }

  /* We use a comparator to make sure that the ordering of
   * the different displays is constant. O/w we would end
   * up with a UI where menu item changes place from time
   * to time.
   */
  private Comparator referenceComparator = new Comparator() {
        public int compare(Object obj1, Object obj2) {
                ServiceReference ref1 = (ServiceReference)obj1;
                ServiceReference ref2 = (ServiceReference)obj2;
                Long l1 = (Long)ref1.getProperty(Constants.SERVICE_ID);
                Long l2 = (Long)ref2.getProperty(Constants.SERVICE_ID);
                return l1.compareTo(l2);
        }
  };

  Map displayMap = new TreeMap(referenceComparator);
  Map menuMap    = new HashMap();
  Map detailMap  = new HashMap();

  Object macApp;
  Method macAppStop;

  public void start() {

    slTracker =
      new ServiceTracker(Activator.getTargetBC(), StartLevel.class.getName(), null);
    slTracker.open();

    pkgTracker =
      new ServiceTracker(Activator.getTargetBC(), PackageAdmin.class.getName(), null);
    pkgTracker.open();



    if(emptyIcon == null) {
      emptyIcon     = new ImageIcon(getClass().getResource("/empty.gif"));
      startIcon     = new ImageIcon(getClass().getResource("/player_play.png"));
      stopIcon      = new ImageIcon(getClass().getResource("/player_stop.png"));
      uninstallIcon = new ImageIcon(getClass().getResource("/player_eject.png"));
      installIcon = new ImageIcon(getClass().getResource("/player_install.png"));
      updateIcon    = new ImageIcon(getClass().getResource("/update.png"));

      viewIcon      = new ImageIcon(getClass().getResource("/view_select.png"));

      magPlusIcon    = new ImageIcon(getClass().getResource("/viewmag+.png"));
      magMinusIcon   = new ImageIcon(getClass().getResource("/viewmag-.png"));
      magFitIcon     = new ImageIcon(getClass().getResource("/viewmagfit.png"));
      mag1to1Icon    = new ImageIcon(getClass().getResource("/viewmag1.png"));

      reloadIcon     = new ImageIcon(getClass().getResource("/reload_green.png"));

      arrowUpIcon    = new ImageIcon(getClass().getResource("/1uparrow.png"));
      arrowUp2Icon   = new ImageIcon(getClass().getResource("/2uparrow.png"));
      arrowDownIcon  = new ImageIcon(getClass().getResource("/1downarrow.png"));
      arrowDown2Icon = new ImageIcon(getClass().getResource("/2downarrow.png"));

      openIcon     = new ImageIcon(getClass().getResource("/open.png"));
      openURLIcon  = new ImageIcon(getClass().getResource("/bundle_small.png"));
      saveIcon     = new ImageIcon(getClass().getResource("/save.png"));

      connectIcon  = new ImageIcon(getClass().getResource("/connect.png"));
      connectIconLarge  = new ImageIcon(getClass().getResource("/connect48x48.png"));

      prevIcon  = new ImageIcon(getClass().getResource("/player_prev.png"));
      nextIcon  = new ImageIcon(getClass().getResource("/player_next.png"));


      tipIcon     = new ImageIcon(getClass().getResource("/idea.png"));
      floatIcon   = new ImageIcon(getClass().getResource("/float.png"));
    }

    lfManager = new LFManager();
    lfManager.init();

    consoleSwing = new ConsoleSwing(Activator.getTargetBC());
    consoleSwing.start();

    toolBar       = makeToolBar();
    statusBar     = new StatusBar("");

    String rName = Activator.remoteHost;
    Map    props = Activator.getSystemProperties();
    String spid  = (String)props.get("org.osgi.provisioning.spid");

    if(spid == null) {
      spid = "";
    }


    try {
      ToolTipManager.sharedInstance().setInitialDelay(50);
    } catch (Exception e) {
      Activator.log.warn("Failed to change tooltip manager", e);
    }

    frame       = new JFrame(Strings.fmt("frame_title", rName, spid));

    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          stopFramework();
        }
      });

    // If running on Mac OS, create eawt Application to catch Mac OS
    // quit and about events
    if(bMacOS) {
      try {
        OSXAdapter.setQuitHandler(this,
                                  getClass().getDeclaredMethod("stopFramework",
                                                               (Class[])null));
        OSXAdapter.setAboutHandler(this,
                                   getClass().getDeclaredMethod("showVersion",
                                                                (Class[])null));
      } catch (Exception e) {
        Activator.log.warn("Error while loading the OSXAdapter", e);
        bMacOS = false;
      }
    }

    contentPane = frame.getContentPane();
    contentPane.setLayout(new BorderLayout());

    SizeSaver ss = new SizeSaver("top", new Dimension(900, 600), -1);

    ss.attach(frame);
    sizesavers.add(ss);

    bundlePanel = new JCardPane();
    // bundlePanel.setPreferredSize(new Dimension(400, 300));

    toolBar       = makeToolBar();


    detailPanel   = new JTabbedPane();
    // detailPanel.setPreferredSize(new Dimension(400, 300));

    detailPanel.setTabPlacement(JTabbedPane.BOTTOM);

    detailPanel.setBorder(null);

    detailPanel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          for(Iterator it = detailMap.keySet().iterator(); it.hasNext();) {
            ServiceReference sr = (ServiceReference)it.next();
            Object obj = detailMap.get(sr);

            if(obj instanceof DefaultSwingBundleDisplayer) {

              ((DefaultSwingBundleDisplayer)obj).setTabSelected();
            }
          }
        }
      });

    //    displayHTML = new BundleInfoDisplayerHTML(this);


    contentPane.add(toolBar,      BorderLayout.NORTH);
    contentPane.add(statusBar,    BorderLayout.SOUTH);


    splitPaneHoriz = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                    bundlePanel,
                                    detailPanel);

    ss = new SizeSaver("splitPaneHoriz",
                       null, // new Dimension(700, 400),
                       350);
    ss.attach(splitPaneHoriz);
    sizesavers.add(ss);
    // splitPaneHoriz.setDividerLocation(bundlePanel.getPreferredSize().width);

    splitPaneHoriz.setOneTouchExpandable(false);


    JFloatable consoleWrapper =
      new JFloatable(consoleSwing.getJComponent(), "Console");

    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                               splitPaneHoriz,
                               consoleWrapper);

    ss = new SizeSaver("splitPaneVertical",
                       null, // new Dimension(800, 600),
                       300);
    ss.attach(splitPane);
    sizesavers.add(ss);

    // splitPane.setDividerLocation(300);
    splitPane.setOneTouchExpandable(false);


    contentPane.add(splitPane,  BorderLayout.CENTER);


    DropTarget dt =
      new DropTarget(contentPane,
                     DnDConstants.ACTION_COPY_OR_MOVE, // actions
                     this,
                     true);



    alive = true;

    // Catch up the set of bundles; no need to call bundleChanged for
    // each bundle since that method ignores the actual event!
    // Bundle[]  bl = Activator.getTargetBC().getBundles();
    // for(int i = 0; i  < bl.length; i++) {
    //   bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bl[i]));
    // }
    bundleChanged((BundleEvent) null);

    pm = new PackageManager(pkgTracker);

    frame.setJMenuBar(menuBar = makeMenuBar());


    setRemote(Activator.remoteTracker.getService() != null);

    setIcon(frame, "/kf_");

    // frame.pack() not used since SizeSaver(frame) does a setSize()
    // frame.pack();
    frame.setVisible(true);
    frame.toFront();

//    String dispFilter1 =
//      "(&" +
//      "(" + Constants.OBJECTCLASS + "=" +
//      SwingBundleDisplayer.class.getName() +
//      ")" +
//      "(" + SwingBundleDisplayer.PROP_ISDETAIL + "=false" + ")" +
//      ")";

    String dispFilter =
      "(" + Constants.OBJECTCLASS + "=" +
      SwingBundleDisplayer.class.getName() +
      ")";

    try {
      dispTracker =
        new ServiceTracker(Activator.getBC(),
                           Activator.getBC().createFilter(dispFilter),
                           null)
        {
          public Object addingService(ServiceReference sr) {
            SwingBundleDisplayer disp =
              (SwingBundleDisplayer)super.addingService(sr);

            Icon   icon = disp.getSmallIcon();

            String  name =
              Util.getStringProp(sr,
                                 SwingBundleDisplayer.PROP_NAME,
                                 disp.getClass().getName());
            String  desc =
              Util.getStringProp(sr,
                                 SwingBundleDisplayer.PROP_DESCRIPTION,
                                 "");

            boolean bDetail    =
              Util.getBooleanProp(sr,
                                  SwingBundleDisplayer.PROP_ISDETAIL,
                                  false);

            JComponent comp = disp.createJComponent();

            JFloatable wrapper = new JFloatable(comp, name);

            // floating windows shouldn't be closed when
            // the tabbed pane swaps components
            wrapper.setAutoClose(false);

            disp.setBundleSelectionModel(bundleSelModel);

            if(bDetail) {
              detailMap.put(sr, disp);

              //              JPanel wrapper2 = new JPanel(new BorderLayout());
              //              wrapper2.add(wrapper, BorderLayout.CENTER);

              detailPanel.addTab(name, icon, wrapper, desc);
            } else {
              displayMap.put(sr, disp);

              bundlePanel.addTab(name, wrapper);

              makeViewPopupMenu();

              viewMenu = makeViewMenu(viewMenu);
            }

            return disp;
          }

          public void removedService(ServiceReference sr, Object service) {
            SwingBundleDisplayer disp = (SwingBundleDisplayer)service;

            String  name =
              Util.getStringProp(sr,
                                 SwingBundleDisplayer.PROP_NAME,
                                 disp.getClass().getName());
            boolean bDetail    =
              Util.getBooleanProp(sr,
                                  SwingBundleDisplayer.PROP_ISDETAIL,
                                  false);


            if(bDetail) {
              Component comp = null;
              for(int i = 0; i < detailPanel.getTabCount(); i++) {
                if(detailPanel.getTitleAt(i).equals(name)) {
                  comp = detailPanel.getComponentAt(i);
                }
              }
              if(comp != null) {
                // Make sure floating windows are closed
                if(comp instanceof JFloatable) {
                  ((JFloatable)comp).setAutoClose(true);
                  ((JFloatable)comp).doUnfloat();
                }
                detailPanel.remove(comp);
                detailMap.remove(sr);
              }
            } else {
              Component comp = bundlePanel.getTab(name);
              if(comp != null) {
                if(comp instanceof JFloatable) {
                  ((JFloatable)comp).setAutoClose(true);
                  ((JFloatable)comp).doUnfloat();
                }
              }

              displayMap.remove(sr);
              bundlePanel.removeTab(name);

              makeViewPopupMenu();
              viewMenu = makeViewMenu(viewMenu);

            }
            super.removedService(sr, service);

          }
        };
      dispTracker.open();
    } catch (Exception e) {
      Activator.log.error("Failed to create tracker", e);
    }


    bundleSelModel.addBundleSelectionListener(this);
    Activator.getTargetBC().addBundleListener(this);
    Activator.getTargetBC().addFrameworkListener(this);

    consoleSwing.getJComponent().requestFocus();

    checkUpdate(false);
  }

  // Assumes that the current Knopflerfish version can be found
  // directly after the text "Knopflerfish " on the first line of the
  // release note at
  // http://www.knopflerfish.org/releases/current/release_notes.txt
  void checkUpdate(final boolean bForce) {
    // Run in threads, to avoid startup delay caused by network problems.
    new Thread() {
      public void run() {
        try {
          if(bForce) {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            prefs.remove(KEY_UPDATEVERSION);
            prefs.flush();
          }

          String versionURL =
            Util.getProperty("org.knopflerfish.desktop.releasenotesurl",
                             "http://www.knopflerfish.org/releases/current/release_notes.txt");

          URL url  = new URL(versionURL);
          URLConnection conn = url.openConnection();
          InputStream is = conn.getInputStream();

          String notes = new String(Util.readStream(is), "ISO-8859-1");

          int ix = notes.indexOf("\n");
          if(ix != -1) {
            String line = notes.substring(0, ix);
            final String keyWord = "Knopflerfish ";
            ix = line.lastIndexOf(keyWord);
            if(ix != -1) {
              line = line.substring(ix + keyWord.length()).trim();
              ix = line.indexOf(" ");
              if (ix!=-1) {
                line = line.substring(0, ix);
              }
              Version version = new Version(line);
              Bundle sysBundle = Activator.getBC().getBundle(0);
              Version sysVersion = new Version((String)sysBundle.getHeaders().get("Bundle-Version"));

              Activator.log.info("sysVersion=" + sysVersion
                                 +", version=" + version);
              if(sysVersion.compareTo(version) < 0) {
                showUpdate(sysVersion, version, notes);
              }
            } else {
              Activator.log.warn("No version info in " + line);
            }
          } else {
            Activator.log.warn("No version line");
          }
        } catch (Exception e) {
          Activator.log.warn("Failed to read update info", e);
        }
      }
    }.start();
  }

  JButton toolStartBundles;
  JButton toolStopBundles;
  JButton toolUpdateBundles;
  JButton toolUninstallBundles;

  void setRemote(boolean b) {
    menuRemote.setEnabled(b);
    buttonRemote.setEnabled(b);
  }

  JToolBar makeToolBar() {
    return new JToolBar() {
        {
          add(toolStartBundles = new JButton(openIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      addBundle();
                    }
                  });
                setToolTipText(Strings.get("menu_openbundles"));
              }
            });

          add(new JButton(openURLIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      addBundleURL();
                    }
                  });
                setToolTipText(Strings.get("menu_openbundleurl"));
              }
            });

          add(toolStartBundles = new JButton(saveIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      save();
                    }
                  });
                setToolTipText(Strings.get("menu_save"));
              }
            });


          add(buttonRemote = new JButton(connectIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      doConnect();
                    }
                  });
                setToolTipText(Strings.get("menu_remotefw"));
              }
            });
          //          add(new JToolBar.Separator());

          add(toolStartBundles = new JButton(startIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      Bundle[] bl = getSelectedBundles();
                      for(int i = 0; i < bl.length; i++) {
                        Bundle b = bl[i];
                        startBundle(b);
                      }
                    }
                  });
                setToolTipText(Strings.get("tt_startbundle"));
              }
            });

          add(toolStopBundles = new JButton(stopIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      if(isEnabled()) {
                        stopBundles(getSelectedBundles());
                      }
                    }
                  });
                setToolTipText(Strings.get("tt_stopbundle"));
              }
            });

          add(toolUpdateBundles = new JButton(updateIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      Bundle[] bl = getSelectedBundles();
                      for(int i = 0; i < bl.length; i++) {
                        updateBundle(bl[i]);
                      }
                    }
                  });
                setToolTipText(Strings.get("tt_updatebundle"));
              }
            });

          add(toolUninstallBundles = new JButton(uninstallIcon) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      Bundle[] bl = getSelectedBundles();
                      for(int i = 0; i < bl.length; i++) {
                        uninstallBundle(bl[i], true);
                      }
                    }
                  });
                setToolTipText(Strings.get("tt_uninstallbundle"));
              }
            });


          add(viewSelection = makeViewSelectionButton());

          StartLevel sls = (StartLevel)slTracker.getService();

          if(null == sls) {
            add(new JLabel(Strings.get("nostartlevel.label")));
          } else {
            add(makeStartLevelSelector());
            add(levelBox);

          }
        }
      };
  }

  JComponent makeStartLevelSelector() {
    StartLevel sls =
      (StartLevel)slTracker.getService();

    Activator.log.debug("has start level service");

    JPanel panel = new JPanel();

    panel.add(new JLabel(Strings.get("startlevel.label")));

    levelBox = new JComboBox();

    updateLevelItems();

    levelBox.setSelectedIndex(sls.getStartLevel() - levelMin);

    levelBox.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent ev) {

          if(levelBox.getSelectedIndex() == -1) {
            return;
          }

          // Delay actual setting to avoid flipping thru
          // levels quickly.
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                Thread t = new Thread() {
                  public void run() {
                    try {
                      Thread.sleep(500);
                      setFWStartLevel();
                    } catch (Exception e) {
                      Activator.log.error("Failed to set start level");
                    }
                  }
                };
                t.start();
              }
            });
        }
      });

    panel.add(levelBox);

    return panel;

  }

  void setFWStartLevel() {
    int level = levelBox.getSelectedIndex() + levelMin;

    StartLevel sls =
      (StartLevel)slTracker.getService();


    if(sls != null) {
      if(sls.getStartLevel() == level) {
        return;
      }
    }

    int myLevel = level;
    try {
      myLevel = sls.getBundleStartLevel(Activator.getTargetBC().getBundle());
    } catch (IllegalArgumentException ignored) {
    }

    boolean bOK = true;

    if(level < myLevel) {
      bOK = false;
      Object[] options = {Strings.get("yes"),
                          Strings.get("cancel")};


      int n =JOptionPane
        .showOptionDialog(frame,
                          Strings.get("q_stopdesktop"),
                          Strings.get("msg_stopdesktop"),
                          JOptionPane.YES_NO_OPTION,
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                                options,
                          options[1]);
      if(n == 0) {
        bOK = true;
      }
    }
    if(bOK) {
      setStartLevel(level);
    } else {
      if(sls != null) {
        levelBox.setSelectedIndex(sls.getStartLevel() - levelMin);
      }
    }
  }

  JButton makeViewSelectionButton() {
    // view selection button
    JButton viewButton = new JButton(viewIcon);

    makeViewPopupMenu();

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

    // end view selection button

    return viewButton;
  }


  JPopupMenu viewPopupMenu;

  void makeViewPopupMenu() {

    viewPopupMenu = new JPopupMenu();
    menuMap.clear();

    for(Iterator it = displayMap.keySet().iterator(); it.hasNext(); ) {
      final ServiceReference     sr   = (ServiceReference)it.next();
      final String key = (String)sr.getProperty(SwingBundleDisplayer.PROP_NAME);

      JMenuItem item = new JMenuItem(key);
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            bundlePanelShowTab(sr);
          }
        });
      viewPopupMenu.add(item);
    }
  }

  void bundlePanelShowTab(String name) {
    ServiceReference[] sr;
    try {
      sr = Activator.getBC().getServiceReferences(SwingBundleDisplayer.class.getName(),
          "("+SwingBundleDisplayer.PROP_NAME+"=" + name +")");
      if (sr != null) {
        bundlePanelShowTab(sr[0]);
      }
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  void bundlePanelShowTab(ServiceReference sr) {
    final String key = (String)sr.getProperty(SwingBundleDisplayer.PROP_NAME);
    bundlePanel.showTab(key);
    JRadioButtonMenuItem item = (JRadioButtonMenuItem) menuMap.get(sr);
    item.setSelected(true);
  }


  void updateLevelItems() {
    StartLevel sls =
      (StartLevel)slTracker.getService();

    if(sls != null) {
      levelMax = Math.max(levelMax, sls.getStartLevel());
    }
    levelItems = new String[levelMax - levelMin + 1];

    Bundle[] bundles = Activator.getTargetBC().getBundles();

    Object selObj = null;

    for(int i = levelMin; i <= levelMax; i++) {
      StringBuffer sb = new StringBuffer();
      int level = i;
      boolean bOverflow = false;
      for(int j = 0; j < bundles.length; j++) {
        try {
          if(sls != null && sls.getBundleStartLevel(bundles[j]) == level) {
            if(sb.length() > 0) {
              sb.append(", ");
            }
            String name = Util.getBundleName(bundles[j]);
            //              Text.replace(Util.shortLocation(bundles[j].getLocation()), ".jar", "");
            sb.append(name);
          }
        } catch (IllegalArgumentException e) {
        }
      }
      String txt = sb.toString();
      int maxLen = 50;
      if(txt.length() > maxLen) {
        txt = txt.substring(0, maxLen) + "...";
      }
      levelItems[i - levelMin] = i + " " + txt;

      if(i == sls.getStartLevel()) {
        selObj = levelItems[i - levelMin];
      }
    }

    if(levelBox != null) {
      DefaultComboBoxModel model = new DefaultComboBoxModel(levelItems);
      levelBox.setModel(model);
    }
  }


  void setStartLevel(final int level) {

    Thread t = new Thread() {
        public void run() {
          StartLevel sls = (StartLevel)slTracker.getService();

          if(null != sls) {
            sls.setStartLevel(level);
          }
        }
      };
    t.start();
  }

  void updateStartLevel() {
    if(slTracker == null) {
      return;
    }

    StartLevel sls = (StartLevel)slTracker.getService();

    if(sls == null) {
      return;
    }

    updateLevelItems();
    if(levelBox != null) {
      levelBox.setSelectedIndex(sls.getStartLevel() - levelMin);
    }
    updateBundleViewSelections();
  }

  // items handling start level stuff. Only used if a StartLevel
  // service is available at startup
  Object[]       levelItems;
  JComboBox      levelBox = null;
  int            levelMin = 1;
  int            levelMax = 20;


  int      baActive    = 0;
  int      baInstalled = 0;
  int      other       = 0;
  Bundle[] bl          = new Bundle[0];


  boolean stopActive() {
    return
      baActive == bl.length ||
      baActive + other == bl.length;
  }

  boolean startActive() {
    return
      baInstalled == bl.length ||
      baInstalled + other== bl.length;
  }

  public void setSelected(Bundle b) {
    bundleSelModel.clearSelection();
    ensureSelected(b);
  }

  public void ensureSelected(Bundle b) {
    bundleSelModel.setSelected(b.getBundleId(), true);
    updateStatusBar();
  }

  public void toggleSelected(Bundle b) {
    bundleSelModel.setSelected(b.getBundleId(),
                               !bundleSelModel.isSelected(b.getBundleId()));
    updateStatusBar();
  }

  public boolean isSelected(Bundle b) {
    return bundleSelModel.isSelected(b.getBundleId());
  }

  void updateBundleViewSelections() {

    Bundle[] bl = getSelectedBundles();


    //    displayHTML.selectionUpdated(bl);

    boolean bEnabled = bl.length > 0;

    itemStartBundles.setEnabled(bEnabled);
    itemStopBundles.setEnabled(bEnabled);
    itemUninstallBundles.setEnabled(bEnabled);
    itemUpdateBundles.setEnabled(bEnabled);

    toolStartBundles.setEnabled(bEnabled);
    toolStopBundles.setEnabled(bEnabled);
    toolUninstallBundles.setEnabled(bEnabled);
    toolUpdateBundles.setEnabled(bEnabled);
    toolBar.invalidate();
    menuBar.invalidate();


    StartLevel sls = (StartLevel)slTracker.getService();
    if(bl.length == 1 && sls != null) {

      if(levelMenuLabel != null) {
        levelMenuLabel.setText("No bundle selected");
        for(Iterator it = levelCheckBoxes.keySet().iterator(); it.hasNext(); ) {
          Integer I = (Integer)it.next();
          AbstractButton jrb = (AbstractButton)levelCheckBoxes.get(I);
          jrb.setSelected(false);
        }
      }

      try {
        Integer I = new Integer(sls.getBundleStartLevel(bl[0]));
        AbstractButton jrb = (AbstractButton)levelCheckBoxes.get(I);
        jrb.setSelected(true);
        levelMenuLabel.setText("Bundle #" + bl[0].getBundleId());
      } catch (Exception e) {
        if(levelMenuLabel != null) {
          levelMenuLabel.setText("Not managed");
        }
      }
    }
  }

  int divloc = 0;

  JMenuBar makeMenuBar() {
    return new JMenuBar() {
        {
          add(makeFileMenu());
          add(editMenu = makeEditMenu());
          add(makeBundleMenu());
          add(viewMenu = makeViewMenu(null));
          add(makeHelpMenu());
        }
      };
  }

  void updateMenus() {
    if(editMenu != null) {
      editMenu.removeAll();
      updateEditMenu(editMenu);
    }
  }

  JMenu makeFileMenu() {
    final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    return new JMenu(Strings.get("menu_file")) {
        {
          add(new JMenuItem(Strings.get("menu_openbundles"), openIcon) {
              {
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                                                      mask));
                setMnemonic(KeyEvent.VK_O);

                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      addBundle();
                    }
                  });
              }});
          add(new JMenuItem(Strings.get("menu_openbundleurl"), openURLIcon) {
              {
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
                                                      mask));
                setMnemonic(KeyEvent.VK_U);

                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      addBundleURL();
                    }
                  });
              }});
          add(new JMenuItem(Strings.get("menu_save"), saveIcon) {
              {
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                                      mask));
                setMnemonic(KeyEvent.VK_S);

                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      save();
                    }
                  });
              }});

          add(menuRemote = new JMenuItem(Strings.get("menu_remotefw"), connectIcon) {
              {
                setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                                                      mask));
                setMnemonic(KeyEvent.VK_F);

                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      doConnect();
                    }
                  });
              }
            });

          if(!bMacOS) {
            add(new JMenuItem("Quit framework...") {
                {
                  setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                                        mask));
                  setMnemonic(KeyEvent.VK_Q);
                  addActionListener(new ActionListener() {
                      public void actionPerformed(ActionEvent ev) {
                        stopFramework();
                      }
                    });
                }});
          }
        }
      };
  }

  JMenuItem itemStopBundles;
  JMenuItem itemStartBundles;
  JMenuItem itemUpdateBundles;
  JMenuItem itemUninstallBundles;
  JMenuItem itemRefreshBundles;

  Action actionStartBundles;
  Action actionStopBundles;
  Action actionUninstallBundles;
  Action actionRefreshBundles;
  Action actionUpdateBundles;

  JMenu makeBundleMenu() {
    final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    actionStartBundles =
      new AbstractAction(Strings.get("item_startbundles"), startIcon) {
        public void actionPerformed(ActionEvent ev) {
          startBundles(getSelectedBundles());
        }
      };
    actionStopBundles =
      new AbstractAction(Strings.get("item_stopbundles"), stopIcon) {
        public void actionPerformed(ActionEvent ev) {
          stopBundles(getSelectedBundles());
        }
      };
    actionUpdateBundles =
      new AbstractAction(Strings.get("item_updatebundles"), updateIcon) {
        public void actionPerformed(ActionEvent ev) {
          updateBundles(getSelectedBundles());
        }
      };
    actionUninstallBundles =
      new AbstractAction(Strings.get("item_uninstallbundles"), uninstallIcon) {
        public void actionPerformed(ActionEvent ev) {
          uninstallBundles(getSelectedBundles());
        }
      };

    actionRefreshBundles =
      new AbstractAction(Strings.get("menu_refreshbundles")) {
        {
          putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, mask));
        }
        public void actionPerformed(ActionEvent ev) {
          refreshBundle(getSelectedBundles());
        }
      };


    return new JMenu(Strings.get("menu_bundles")) {
        {
          add(itemStopBundles   = new JMenuItem(actionStopBundles));
          add(itemStartBundles  = new JMenuItem(actionStartBundles));
          add(itemUpdateBundles = new JMenuItem(actionUpdateBundles));
          add(itemUninstallBundles = new JMenuItem(actionUninstallBundles));
          add(itemRefreshBundles   = new JMenuItem(actionRefreshBundles));

          StartLevel sls = (StartLevel)slTracker.getService();
          if(sls != null) {
            add(makeStartLevelMenu());
          }
        }
      };
  }

  // Integer -> AbstractButton
  Map    levelCheckBoxes = new HashMap();
  JLabel levelMenuLabel  = null;

  JMenu makeStartLevelMenu() {
    return new JMenu(Strings.get("menu_startlevel")) {
        {
          ButtonGroup group = new ButtonGroup();

          add(levelMenuLabel = new JLabel(""));
          add(new JSeparator());

          for(int i = levelMin; i <= levelMax; i++) {
            final AbstractButton jrb = new JRadioButtonMenuItem(Integer.toString(i));
            group.add(jrb);
            add(jrb);
            jrb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                  StartLevel sls = (StartLevel)slTracker.getService();

                  Bundle[] bl = getSelectedBundles();

                  if(bl.length == 1 && null != sls) {
                    int level = Integer.parseInt(jrb.getText());

                    sls.setBundleStartLevel(bl[0], level);

                  }
                }
              });

            levelCheckBoxes.put(new Integer(i), jrb);
          }
        }
      };
  }



  JMenu viewMenu = null;
  JMenu editMenu = null;

  JMenu makeViewMenu(JMenu oldMenu) {
    JMenu menu;
    final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    if(oldMenu != null) {
      oldMenu.removeAll();
      menu = oldMenu;
    } else {
      menu = new JMenu(Strings.get("menu_view"));
    }

    final ButtonGroup group = new ButtonGroup();

    menu.add(new JCheckBoxMenuItem(Strings.get("menu_view_console")) {
        {
          setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1,
                                                ActionEvent.ALT_MASK));
          addActionListener(new SplitAction(splitPane,
                                            consoleSwing.getJComponent()));
          setState(true);
        }
      });

    menu.add(new JCheckBoxMenuItem(Strings.get("menu_view_bundles")) {
        {
          setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2,
                                                ActionEvent.ALT_MASK));
          addActionListener(new SplitAction(splitPaneHoriz,
                                            bundlePanel));
          setState(true);
        }
      });

    menu.add(new JCheckBoxMenuItem(Strings.get("menu_view_info")) {
        {
          setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3,
                                                ActionEvent.ALT_MASK));
          addActionListener(new SplitAction(splitPaneHoriz,
                                            detailPanel));
          setState(true);
        }
      });

    menu.add(new JCheckBoxMenuItem(Strings.get("menu_view_toolbar")) {
        {
          setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4,
                                                ActionEvent.ALT_MASK));
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                toolBar.setVisible(getState());
              }
            });
          setState(true);
        }
      });

    menu.add(new JCheckBoxMenuItem(Strings.get("menu_view_statusbar")) {
        {
          setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5,
                                                ActionEvent.ALT_MASK));
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                statusBar.setVisible(getState());
              }
            });
          setState(false);
        }
      });
    statusBar.setVisible(false);

    menu.add(new JSeparator());

    int count = 0;
    menuMap.clear();
    for(Iterator it = displayMap.keySet().iterator(); it.hasNext(); ) {
      final ServiceReference     sr   = (ServiceReference)it.next();
      final String   name = (String)sr.getProperty(SwingBundleDisplayer.PROP_NAME);
      final int c2 = count++;

      menu.add(new JRadioButtonMenuItem(name) {
          {
            setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + c2,
                                                  mask));
            setMnemonic(KeyEvent.VK_1 + c2);
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                  bundlePanelShowTab(sr);
                }
              });
            menuMap.put(sr, this);
            group.add(this);
          }});
    }

    lfMenu = new LookAndFeelMenu(Strings.get("menu_lookandfeel"), lfManager);

    lfMenu.addRoot(SwingUtilities.getRoot(frame));
    menu.add(new JSeparator());
    menu.add(lfMenu);

    menu.add(makeErrorDialogMenu());

    return menu;
  }


  JMenu edlMenu = null;

  JMenu makeErrorDialogMenu() {
    return new JMenu(Strings.get("menu_errordialog")) {
      {
        add(new JCheckBoxMenuItem(Strings.get("menu_errordialog_use")) {
          {
            addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                System.setProperty("org.knopflerfish.desktop.dontuseerrordialog", String.valueOf(!getState()));
                edlMenu.setEnabled(getState());
              }
            });
            setState(!Util.getBooleanProperty("org.knopflerfish.desktop.dontuseerrordialog", false));
          }
        });
        edlMenu = new JMenu(Strings.get("menu_errordialoglevel")) {
          {
            ButtonGroup group = new ButtonGroup();

            AbstractButton jrbn = new JRadioButtonMenuItem(Strings.get("menu_errordialoglevel_normal"));
            group.add(jrbn);
            add(jrbn);
            jrbn.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                System.setProperty("org.knopflerfish.desktop.errordialogfriendliness", "normal");
              }
            });

            AbstractButton jrbm = new JRadioButtonMenuItem(Strings.get("menu_errordialoglevel_more"));
            group.add(jrbm);
            add(jrbm);
            jrbm.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                System.setProperty("org.knopflerfish.desktop.errordialogfriendliness", "more");
              }
            });

            AbstractButton jrba = new JRadioButtonMenuItem(Strings.get("menu_errordialoglevel_advanced"));
            group.add(jrba);
            add(jrba);
            jrba.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                System.setProperty("org.knopflerfish.desktop.errordialogfriendliness", "advanced");
              }
            });

            String curr = Util.getProperty("org.knopflerfish.desktop.errordialogfriendliness", null);
            if ("more".equals(curr)) {
              group.setSelected(jrbm.getModel(), true);
            } else if ("advanced".equals(curr)) {
              group.setSelected(jrba.getModel(), true);
            } else {
              group.setSelected(jrbn.getModel(), true);
            }

            setEnabled(!Util.getBooleanProperty("org.knopflerfish.desktop.dontuseerrordialog", false));

          }
        };
        add(edlMenu);
      }
    };
  }


  JMenu makeHelpMenu() {
    return new JMenu(Strings.get("menu_help")) {
        {
          add(new JMenuItem(Strings.get("str_about")) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      showVersion();
                    }
                  });
              }
            });

          add(new JMenuItem(Strings.get("menu_tips")) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      showTips();
                    }
                  });
              }
            });


          add(new JSeparator());

          add(new JMenuItem(Strings.get("str_fwinfo")) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      showInfo();
                    }
                  });
              }
            });

          add(new JMenuItem(Strings.get("str_checkupdate")) {
              {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      checkUpdate(true);
                    }
                  });
              }
            });


        }
      };
  }

  Map makeBundleBuckets() {
    Map buckets;

    Bundle[] bl      = Activator.getBundles();
    Map bundles = new HashMap();
    for(int i = 0; bl != null && i < bl.length; i++) {
      Object key = new Long(bl[i].getBundleId());
      bundles.put(key, bl[i]);
    }

    if(bundles.size() > 12) {
      // make alphabetical submenu grouping
      // if number of bundles is large
      buckets = new TreeMap();
      for(Iterator it = bundles.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();
        Bundle bundle = (Bundle)bundles.get(key);
        String s = Util.getBundleName(bundle);
        String f = s.length() > 0
          ? s.substring(0, 1).toUpperCase()
          : "--";
        Collection bucket = (Collection)buckets.get(f);
        if(bucket == null) {
          bucket = new ArrayList();
          buckets.put(f, bucket);
        }
        bucket.add(bundle);
      }
    } else {
      buckets = new LinkedHashMap();
      for(Iterator it = bundles.keySet().iterator(); it.hasNext(); ) {
        Object key = it.next();
        Bundle bundle = (Bundle)bundles.get(key);

        String f = "#" + bundle.getBundleId() + " " + Util.getBundleName(bundle);
        buckets.put(f, bundle);
      }
    }
    return buckets;
  }


  JMenu makeEditMenu() {
    JMenu menu = new JMenu(Strings.get("menu_edit"));
    updateEditMenu(menu);
    return menu;
  }

  void updateEditMenu(JMenu editMenu) {
    editMenu.add(new JMenuItem(Strings.get("item_unselectall")) {
        {
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                bundleSelModel.clearSelection();
                contentPane.invalidate();
              }
            });
        }
      });
    editMenu.add(new JMenuItem(Strings.get("item_clear_console")) {
        {
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                consoleSwing.clearConsole();
              }
            });
        }
      });

    editMenu.add(new JSeparator());

    JMenu selectMenu = new JMenu("Select bundle");
    editMenu.add(selectMenu);

    Map buckets = makeBundleBuckets();

    for(Iterator it = buckets.keySet().iterator(); it.hasNext(); ) {
      Object key = it.next();
      Object val = buckets.get(key);
      if(val instanceof Collection) {
        Collection bucket = (Collection)val;
        JMenu subMenu = new JMenu(key.toString());
        for(Iterator it2 = bucket.iterator(); it2.hasNext(); ) {
          Bundle bundle = (Bundle)it2.next();
          JMenuItem item = makeSelectBundleItem(bundle);
          subMenu.add(item);
        }
        selectMenu.add(subMenu);
      } else if(val instanceof Bundle) {
        Bundle bundle = (Bundle)val;
        JMenuItem item = makeSelectBundleItem(bundle);
        selectMenu.add(item);
      } else {
        throw new RuntimeException("Unknown object=" + val);
      }
    }
  }


  JMenuItem makeSelectBundleItem(final Bundle bundle) {
    JMenuItem item = new JMenuItem(bundle.getBundleId() + " " + Util.getBundleName(bundle));

    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          bundleSelModel.clearSelection();
          bundleSelModel.setSelected(bundle.getBundleId(), true);
        }
      });
    return item;
  }

  /**
   * Helper command class to show/hide splitpane components
   */
  class SplitAction implements ActionListener {
    int        divloc = 0;
    JSplitPane pane;
    JComponent target;

    SplitAction(JSplitPane pane, JComponent target) {
      this.pane   = pane;
      this.target = target;
    }

    public void actionPerformed(ActionEvent ev) {
      boolean b = target.isVisible();

      if(b) {
        divloc = pane.getDividerLocation();
      }

      target.setVisible(!b);

      if(!b) {
        pane.setDividerLocation(divloc);
      }
      pane.getParent().invalidate();
    }
  }


  public void stopFramework() {
    if(SwingUtilities.isEventDispatchThread()) {
      stopFramework0();
    } else {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            stopFramework0();
          }
        });
    }
  }

  protected void stopFramework0() {

    Object[] options = {Strings.get("yes"),
                        Strings.get("cancel")};


    int n =JOptionPane
      .showOptionDialog(frame,
                        Strings.get("q_stopframework"),
                        Strings.get("msg_stopframework"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1]);
    if(n == 0) {
      try {
        Bundle sysBundle = Activator.getBC().getBundle((long)0);
        sysBundle.stop();
      } catch (Exception e) {
        showErr("Failed to stop bundle.", e);
      }
    }
  }

  String lastBundleLocation = "http://";

  void addBundleURL() {
    try {
      lastBundleLocation = (String)
        JOptionPane.showInputDialog(frame,
                                    Strings.get("dialog_addbundleurl_msg"),
                                    Strings.get("dialog_addbundleurl_title"),
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    null,
                                    lastBundleLocation);

      if(lastBundleLocation != null && !"".equals(lastBundleLocation)) {
        Bundle b = Activator.getTargetBC().installBundle(lastBundleLocation);
        setSelected(b);
        showBundle(b);
        Dictionary headers = b.getHeaders();
        if(Util.doAutostart() && Util.canBeStarted(b)) {
          startBundle(b);
        }
      }
    } catch (Exception e) {
      showErr(null, e);
    }
  }

  JFileChooser openFC = null;

  /**
   * Open a file dialog and ask for jar files to install as bundles.
   */
  void addBundle() {
    if(openFC == null) {
      openFC = new JFileChooser();
      File cwd = new File(Util.getProperty("user.dir", "."));
      String jarsProp=Util.getProperty("org.knopflerfish.gosg.jars",null);
      if (jarsProp!=null && jarsProp.startsWith("file:")) {
        cwd = new File(jarsProp.substring(5));
      }
      // The argument to setCurrentDirectory() must be an ablsolute
      // path on MacOSX!
      openFC.setCurrentDirectory(cwd.getAbsoluteFile());
      openFC.setMultiSelectionEnabled(true);
      FileFilterImpl filter = new FileFilterImpl();
      filter.addExtension("jar");
      filter.setDescription("Jar files");
      openFC.setFileFilter(filter);
      openFC.setDialogTitle("Open bundle jar file");
      openFC.setApproveButtonText("Open");
    }

    int returnVal = openFC.showOpenDialog(frame);

    if(returnVal == JFileChooser.APPROVE_OPTION) {
      File[] files = openFC.getSelectedFiles();

      for(int i = 0; i < files.length; i++) {
        addFile(files[i]);
      }
    }
  }

  void doConnect() {
    String[] options = new String[Activator.remoteHosts.size()];
    Activator.remoteHosts.copyInto(options);

    // The selection comp I want in the dialog
    JComboBox combo = new JComboBox(options);
    combo.setEditable(true);


    // Mindboggling complicate way of creating an option dialog
    // without the auto-generated input field

    JLabel msg    = new JLabel(Strings.get("remote_connect_msg"));
    JPanel panel  = new JPanel(new BorderLayout());

    panel.add(combo, BorderLayout.SOUTH);
    panel.add(msg,   BorderLayout.NORTH);

    JOptionPane optionPane = new JOptionPane(panel,
                                             JOptionPane.QUESTION_MESSAGE);
    optionPane.setIcon(connectIconLarge);
    optionPane.setRootFrame(frame);
    optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
    optionPane.setWantsInput(false);
    optionPane.setOptions(new String[]
      {
        Strings.get("ok"),
        Strings.get("cancel"),
        Strings.get("local"),
      });

    optionPane.selectInitialValue();

    JDialog dialog =
      optionPane.createDialog(frame,
                              Strings.get("remote_connect_title"));
    dialog.setVisible(true);
    dialog.dispose();

    if (!(optionPane.getValue() instanceof String)) { // We'll get an Integer if the user pressed Esc
      return;
    }

    String value = (String)optionPane.getValue();

    if (Strings.get("cancel").equals(value)) {
      return;
    }

    String s = (String)combo.getSelectedItem();

    if (Strings.get("local").equals(value)) {
      s = "";
    }

    if(!Activator.remoteHosts.contains(s)) {
      Activator.remoteHosts.addElement(s);
    }

    if ((s != null)) {
      Activator.openRemote(s);
    }
  }

  JFileChooser saveFC = null;

  void save() {
    if(saveFC == null) {
      saveFC = new JFileChooser();
      saveFC.setCurrentDirectory(new File("."));
      saveFC.setMultiSelectionEnabled(false);
      FileFilterImpl filter = new FileFilterImpl();
      filter.addExtension("jar");
      filter.addExtension("zip");
      filter.setDescription("Deploy archives");
      saveFC.setFileFilter(filter);
      saveFC.setDialogTitle("Save deploy archive");
      saveFC.setApproveButtonText("Save");
    }

    Bundle[] targets    = getSelectedBundles();

    StringBuffer title = new StringBuffer();
    title.append("Save deploy archive of: ");

    for(int i = 0; i < targets.length; i++) {
      title.append(Util.getBundleName(targets[i]));
      if(i < targets.length - 1) {
        title.append(", ");
      }
    }
    saveFC.setDialogTitle(title.toString());

    int returnVal = saveFC.showSaveDialog(frame);

    if(returnVal == JFileChooser.APPROVE_OPTION) {
      File file = saveFC.getSelectedFile();

      doSave(file, targets);
    }
  }

  void doSave(File file, Bundle[] targets) {
    byte[] buf = new byte[1024 * 5];

    if(file.getName().endsWith(".jar") ||
       file.getName().endsWith(".zip")) {
      // OK
    } else {
      file = new File(file.getAbsolutePath() + ".jar");
    }

    if(file.exists()) {
      Object[] options = { Strings.get("yes"),
                           Strings.get("cancel")};

      int n = JOptionPane.showOptionDialog(frame,
                                           file.getAbsolutePath() + "\n" +
                                           "already exist.\n\n" +
                                           "Overwrite file?",
                                           "File exists",
                                           JOptionPane.YES_NO_OPTION,
                                           JOptionPane.QUESTION_MESSAGE,
                                           null,
                                           options,
                                           options[1]);
      if(n == 1) {
        return;
      }
    }


    String base = file.getName();
    int ix = base.lastIndexOf(".");
    if(ix != -1) {
      base = base.substring(0, ix);
    }

    PackageAdmin pkgAdmin = (PackageAdmin)pkgTracker.getService();

    if(pkgAdmin == null) {
      Activator.log.error("No pkg admin available for save");
      return;
    }

    Bundle[] allBundles = Activator.getBundles();


    Set pkgClosure = new TreeSet(Util.bundleIdComparator);

    for(int i = 0; i < targets.length; i++) {
      pkgClosure.addAll(Util.getPackageClosure(pm,
                                               targets[i],
                                               null));
    }

    Set serviceClosure = new TreeSet(Util.bundleIdComparator);

    for(int i = 0; i < targets.length; i++) {
      serviceClosure.addAll(Util.getServiceClosure(targets[i], null));
    }

    Set all = new TreeSet(Util.bundleIdComparator);
    all.addAll(pkgClosure);
    all.addAll(serviceClosure);

    for(int i = 0; i < targets.length; i++) {
      all.add(targets[i]);
    }

    // remove system bundle.
    all.remove(Activator.getTargetBC().getBundle(0));

    ZipOutputStream out = null;

    StartLevel sl = (StartLevel)slTracker.getService();

    File jarunpackerFile = new File("../tools/jarunpacker/out/jarunpacker/jarunpacker.jar");

    URL jarunpackerURL = null;

    try {
      jarunpackerURL = getClass().getResource("/jarunpacker.jar");
    } catch (Exception ignored) {
    }

    InputStream jarunpacker_in = null;

    try {
      if(file.getName().endsWith(".jar")) {


        if(jarunpackerURL != null) {
          jarunpacker_in = jarunpackerURL.openStream();
        } else if(jarunpackerFile.exists()) {
          jarunpacker_in = new FileInputStream(jarunpackerFile);
        }

        if(jarunpacker_in != null) {
          // Construct a string version of a manifest
          StringBuffer sb = new StringBuffer();
          sb.append("Manifest-Version: 1.0\n");
          sb.append("Main-class: org.knopflerfish.tools.jarunpacker.Main\n");
          sb.append("jarunpacker-optbutton: base\n");
          sb.append("jarunpacker-destdir: .\n");
          sb.append("knopflerfish-version: " + base + "\n");
          sb.append("jarunpacker-opendir: " + base + "\n");

          // Convert the string to a input stream
          InputStream is = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
          Manifest mf = new Manifest(is);

          out = new JarOutputStream(new FileOutputStream(file), mf);
        } else {
          out = new JarOutputStream(new FileOutputStream(file));
        }
      } else if(file.getName().endsWith(".zip")) {
        out = new ZipOutputStream(new FileOutputStream(file));
      }

      StringBuffer xargs = new StringBuffer();

      int levelMax = -1;

      int bid = 0;
      int lastLevel = -1;
      for(Iterator it = all.iterator(); it.hasNext(); ) {
        Bundle b   = (Bundle)it.next();
        String loc = b.getLocation();

        bid++;

        URL srcURL = new URL(loc);

        String name = Util.shortLocation(loc);

        ZipEntry entry = new ZipEntry(base + "/" + name);

        int level     = -1;

        try {
          level = sl.getBundleStartLevel(b);
        } catch (Exception ignored) {        }

        levelMax = Math.max(level, levelMax);

        if(level != -1 && level != lastLevel) {
          xargs.append("-initlevel " + level + "\n");
          lastLevel = level;
        }

        xargs.append("-install file:" + name + "\n");

        out.putNextEntry(entry);

        InputStream in = null;
        try {
          in = srcURL.openStream();
          int n = 0;
          while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
          }
        } finally {
          try {
            in.close();
          } catch (Exception ignored) { }
        }
      }

      bid = 0;
      for(Iterator it = all.iterator(); it.hasNext(); ) {
        Bundle b   = (Bundle)it.next();
        bid++;

        if(b.getState() == Bundle.ACTIVE) {
          xargs.append("-start " + bid + "\n");
        }
      }

      if(levelMax != -1) {
        xargs.append("-startlevel " + levelMax + "\n");
      }

      ZipEntry entry = new ZipEntry(base + "/" + "init.xargs");
      out.putNextEntry(entry);
      out.write(xargs.toString().getBytes());

      entry = new ZipEntry(base + "/" + "framework.jar");
      out.putNextEntry(entry);

      InputStream in = null;

      File fwFile = new File("framework.jar");
      if(fwFile.exists()) {
        try {
          in = new FileInputStream(fwFile);
          int n = 0;
          while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
          }
        } finally {
          try {
            in.close();
          } catch (Exception ignored) { }
        }
      } else {
        Activator.log.warn("No framework.jar file found");
      }


      // Copy jarunpacker files, if availbale
      if(jarunpacker_in != null) {
        JarInputStream jar_in = null;

        try {
          jar_in = new JarInputStream(new BufferedInputStream(jarunpacker_in));

          ZipEntry srcEntry;
          while(null != (srcEntry = jar_in.getNextEntry())) {

            // Skip unused files from jarunpacker
            if(srcEntry.getName().startsWith("META-INF") ||
               srcEntry.getName().startsWith("OSGI-OPT")) {
              continue;
            }

            ZipEntry destEntry = new ZipEntry(srcEntry.getName());

            out.putNextEntry(destEntry);

            long nTotal = 0;
            int n = 0;
            while (-1 != (n = jar_in.read(buf, 0, buf.length))) {
              out.write(buf, 0, n);
              nTotal += n;
            }
          }
        } finally {
          try { jar_in.close();  } catch (Exception ignored) {  }
        }
      } else {
        Activator.log.warn("No jarunpacker available");
      }
      // end of jarunpacker copy

    } catch (Exception e) {
      showErr("Failed to write to " + file, e);
      Activator.log.error("Failed to write to " + file, e);
    } finally {
      try { out.close(); } catch (Exception ignored) { }
    }


    String txt =
      "Saved deploy archive as\n\n" +
      "  " + file.getAbsolutePath() + "\n\n" +
      "To run, unpack the archive and run with\n\n" +
      "  java -jar framwork.jar\n";

    JOptionPane.showMessageDialog(frame,
                                  txt,
                                  "Saved deploy archive",
                                  JOptionPane.INFORMATION_MESSAGE,
                                  null);
  }



  public Bundle[] getSelectedBundles() {
    int n = 0;

    for(int i = 0; i < bundleCache.length; i++) {
      if(bundleSelModel.isSelected(bundleCache[i].getBundleId())) {
        n++;
      }
    }
    Bundle[] bl = new Bundle[n];

    n = 0;
    for(int i = 0; i < bundleCache.length; i++) {
      if(bundleSelModel.isSelected(bundleCache[i].getBundleId())) {
        bl[n++] = bundleCache[i];
      }
    }

    return bl;
  }

  void startBundle(Bundle b) {
    try {
      b.start();
    } catch (Exception e) {
      showErr("failed to start bundle " +
              Util.getBundleName(b), e);
    }
  }

  void stopBundles(Bundle[] bl) {
    for(int i = 0; bl != null && i < bl.length; i++) {
      Bundle b = bl[i];
      stopBundle(b);
    }
  }
  void startBundles(Bundle[] bl) {
    for(int i = 0; bl != null && i < bl.length; i++) {
      Bundle b = bl[i];
      startBundle(b);
    }
  }

  void updateBundles(Bundle[] bl) {
    for(int i = 0; bl != null && i < bl.length; i++) {
      Bundle b = bl[i];
      updateBundle(b);
    }
  }

  void uninstallBundles(Bundle[] bl) {
    for(int i = 0; bl != null && i < bl.length; i++) {
      Bundle b = bl[i];
      uninstallBundle(b, true);
    }
  }



  void stopBundle(Bundle b) {
    int n = 0;
    if(b.getBundleId() == Activator.getTargetBC().getBundle().getBundleId() ||
       b.getBundleId() == 0) {
      Object[] options = { Strings.get("yes"),
                           Strings.get("no")};


      n = JOptionPane
        .showOptionDialog(frame,
                          Strings.fmt("fmt_q_stopdesktop",
                                      Util.getBundleName(b)),
                          Strings.get("yes"),
                          JOptionPane.YES_NO_OPTION,
                          JOptionPane.QUESTION_MESSAGE,
                          null,
                          options,
                          options[1]);
    }

    if(n == 0) {
      try {
        b.stop();
      } catch (Exception e) {
        showErr("failed to stop bundle " +
                Util.getBundleName(b), e);
      }
    }
  }

  void refreshBundle(Bundle[] b) {
    ServiceReference sr = Activator.getTargetBC().getServiceReference(PackageAdmin.class.getName());
    if(sr != null) {
      PackageAdmin packageAdmin = (PackageAdmin)Activator.getTargetBC().getService(sr);
      if(packageAdmin != null) {
        if(b != null && b.length == 0) {
          b = null;
        }
        packageAdmin.refreshPackages(b);
      }
      Activator.getTargetBC().ungetService(sr);
    }
  }

  void updateBundle(Bundle b) {
    try {
      boolean wasSelected = isSelected(b);
      boolean bUpdateIsUpdate
        = Util.getBooleanProperty("org.knopflerfish.desktop.updateisupdate",
                                  true);
      if(bUpdateIsUpdate || b == Activator.getBC().getBundle()) {
        b.update();
        if (wasSelected) ensureSelected(b);
      } else {
        String location = (String)b.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
        if(location == null || "".equals(location)) {
          location = b.getLocation();
        }

        if(uninstallBundle(b, true)) {
          refreshBundle(new Bundle[] { b });
          Bundle newBundle = Activator.getTargetBC().installBundle(location);
          if(Util.canBeStarted(newBundle)) {
            startBundle(newBundle);
          }
          if (wasSelected) ensureSelected(newBundle);
        }
      }
    } catch (Exception e) {
      showErr("failed to update bundle " + Util.getBundleName(b), e);
    }
  }

  boolean uninstallBundle(Bundle b, boolean bAsk) {
    Object[] options = {Strings.get("yes"),
                        Strings.get("no")};


    int n = bAsk
      ? JOptionPane
      .showOptionDialog(frame,
                        Strings.fmt("q_uninstallbundle",
                                     Util.getBundleName(b)),
                        Strings.get("msg_uninstallbundle"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[1])
      : 0;

    if(n == 0) {
      try {
        b.uninstall();
        try { // Try to unselect uninstalled bundle
          if (isSelected(b)) {
            toggleSelected(b);
            contentPane.invalidate();
          }
        } catch (Throwable ignore) {}
        return true;
      } catch (Exception e) {
        showErr("failed to uninstall bundle " + Util.getBundleName(b), e);
      }
    }
    return false;
  }




  void showErr(String msg, Exception e) {
    Throwable t = e;
    while(t instanceof BundleException &&
          ((BundleException) t).getNestedException() != null) {
      t = ((BundleException) t).getNestedException();
    }
    if (Util.getBooleanProperty("org.knopflerfish.desktop.dontuseerrordialog",
                                false)) {
      if(msg != null && !"".equals(msg)) {
        System.out.println(msg);
      }
      t.printStackTrace();
    } else {
      new ErrorMessageDialog(frame, null, msg, null, t).setVisible(true);
    }
  }



  // DropTargetListener
  public void drop(DropTargetDropEvent e) {

    // This code is f***ing unbelievable.
    // How is anyone supposed to create it from scratch?
    try {
      DataFlavor[] dfl = e.getCurrentDataFlavors();
      Transferable tr  = e.getTransferable();

      if(e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        java.util.List files = (java.util.List)tr.getTransferData(DataFlavor.javaFileListFlavor);
        for(Iterator it = files.iterator(); it.hasNext();) {
          File file = (File)it.next();
          addFile(file);
        }
        e.dropComplete(true);
      } else if(e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        String filename = (String)tr.getTransferData(DataFlavor.stringFlavor);

        addFile(new File(filename));
        e.dropComplete(true);
      } else {
        // Reject drop
      }
    }
    catch(IOException ioe) {
      showErr(null, ioe);
    }
    catch(UnsupportedFlavorException ufe) {
      showErr("Unsupported data type", ufe);
    }
  }

  // DropTargetListener
  public void dragEnter(DropTargetDragEvent e) {
    //    System.out.println("dragEnter " + e);
  }

  // DropTargetListener
  public void dragExit(DropTargetEvent e) {
    //    System.out.println("dragExit " + e);
  }

  // DropTargetListener
  public void dragOver(DropTargetDragEvent e) {
    //    System.out.println("dragOver " + e);
  }

  // DropTargetListener
  public void dropActionChanged(DropTargetDragEvent e) {
    //    System.out.println("dropActionChanged " + e);
  }


  void addFile(File file) {
    try {
      String location = "file:" + file.getAbsolutePath();
      Bundle b = Activator.getTargetBC().installBundle(location);

      setSelected(b);
      showBundle(b);
      if(Util.doAutostart() && Util.canBeStarted(b)) {
        startBundle(b);
      }
    } catch (Exception e) {
      if(file.getName().toUpperCase().endsWith(".JAR")) {
        showErr("Failed to open bundle.", e);
      } else {
        showErr("The file is not a bundle.", e);
      }
    }
  }

  // try to show a bundle in displayers
  public void showBundle(Bundle b) {
    Object[] disps = dispTracker.getServices();
    for(int i = 0; disps != null && i < disps.length; i++) {
      SwingBundleDisplayer disp = (SwingBundleDisplayer)disps[i];
      disp.showBundle(b);
    }
  }

  public void stop() {
    for(Iterator it = sizesavers.iterator(); it.hasNext(); ) {
      SizeSaver ss = (SizeSaver)it.next();
      ss.detach();
    }
    sizesavers.clear();

    if(tips != null) {
      tips.setVisible(false);
      tips = null;
    }

    alive = false;

    slTracker.close();
    dispTracker.close();
    pkgTracker.close();

    // Make sure floating windows are closed
    for(int i = 0; i < detailPanel.getTabCount(); i++) {
      Component comp = detailPanel.getComponentAt(i);
      if(comp instanceof JFloatable) {
        ((JFloatable)comp).setAutoClose(true);
      }
    }

    Activator.getTargetBC().removeBundleListener(this);

    if(consoleSwing != null) {
      consoleSwing.stop();
      consoleSwing = null;
    }


    if(frame != null) {
      frame.setVisible(false);
      frame = null;
    }

    // If running on Mac OS, remove eawt Application handlers.
    if(bMacOS) {
      try {
        OSXAdapter.clearApplicationListeners();
      } catch (Exception e) {
        Activator.log.warn("Error while using the OSXAdapter", e);
        bMacOS = false;
      }
    }

  }

  public void valueChanged(long bid) {
    if(!alive) {
      return;
    }

    updateBundleViewSelections();
  }

  public void frameworkEvent(FrameworkEvent ev) {
    if(!alive) {
      return;
    }
    switch(ev.getType()) {
    case FrameworkEvent.STARTLEVEL_CHANGED:
      updateStartLevel();
      break;
    }
  }

  Bundle[] bundleCache;

  public void bundleChanged(BundleEvent ev) {
    if(!alive) {
      return;
    }

    if(pm != null) {
      pm.refresh();
    }
    bundleCache = Activator.getBundles();

    updateStatusBar();
    updateMenus();
    toolBar.revalidate();
    toolBar.repaint();
  }

  void updateStatusBar() {
  }


  JTips tips = null;
  void showTips() {
    if(tips == null) {
      tips = new JTips("/tips.html");
    }
    tips.setVisible(true);
  }

  static final String KEY_UPDATEVERSION = "updateVersion";

 void showUpdate(Version sysVersion, Version version, String notes) {
   Preferences prefs = Preferences.userNodeForPackage(getClass());

   Activator.log.info("showUpdate sysVersion=" + sysVersion + ", version=" + version);
   try {
     String prefsVersionS = prefs.get(KEY_UPDATEVERSION, "");
     if(prefsVersionS != null && !"".equals(prefsVersionS)) {
       Version prefsVersion = new Version(prefsVersionS);
       Activator.log.info("prefsVersion=" + prefsVersion);
       if(prefsVersion.compareTo(version) >= 0) {
         Activator.log.info("skip showUpdate " + version);
         return;
       }
     }
   } catch (Exception e) {
     Activator.log.warn("Failed to compare prefs version", e);
   }

   JTextPane html = new JTextPane();

    html.setContentType("text/html");

    html.setEditable(false);

    html.setText("<pre>\n" +
                 notes +
                 "\n</pre>");

    final JScrollPane scroll = new JScrollPane(html);
    scroll.setPreferredSize(new Dimension(500, 300));
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          JViewport vp = scroll.getViewport();
          if(vp != null) {
            vp.setViewPosition(new Point(0,0));
            scroll.setViewport(vp);
          }
        }
      });

    JOptionPane.showMessageDialog(frame,
                                  scroll,
                                  "Update available",
                                  JOptionPane.INFORMATION_MESSAGE,
                                  null);

    try {
      prefs.put(KEY_UPDATEVERSION, version.toString());
      Activator.log.info("saved version " + version);
      prefs.flush();
    } catch (Exception e) {
      Activator.log.warn("Failed to store prefs", e);
    }
 }


  void showInfo() {
    JTextPane html = new JTextPane();

    html.setContentType("text/html");

    html.setEditable(false);

    html.setText(Util.getSystemInfo());

    final JScrollPane scroll = new JScrollPane(html);
    scroll.setPreferredSize(new Dimension(420, 300));
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          JViewport vp = scroll.getViewport();
          if(vp != null) {
            vp.setViewPosition(new Point(0,0));
            scroll.setViewport(vp);
          }
        }
      });

    JOptionPane.showMessageDialog(frame,
                                  scroll,
                                  "Framework info",
                                  JOptionPane.INFORMATION_MESSAGE,
                                  null);
  }

  public void showVersion() {
    BundleContext bc = Activator.getBC();
    String version = (String)bc.getBundle().getHeaders().get("Bundle-Version");
    String txt = Strings.fmt("str_abouttext",
                             version,
                             bc.getProperty(org.osgi.framework.Constants.FRAMEWORK_VENDOR),
                             bc.getBundle(0).getHeaders().get("Bundle-Version"));

    ImageIcon icon =
      new ImageIcon(getClass().getResource("/kf_300x170.png"));

    JOptionPane.showMessageDialog(frame,
                                  txt,
                                  Strings.get("str_about"),
                                  JOptionPane.INFORMATION_MESSAGE,
                                  icon);
  }

  public void setIcon(JFrame frame, String baseName) {
    String iconName = baseName + "32x32.gif";
    if (Util.isWindows()) {
      iconName = baseName + "16x16.gif";
    }
    String strURL = iconName;
    try {
      MediaTracker tracker = new MediaTracker(frame);

      URL url = getClass().getResource(strURL);

      if(url != null) {
        Image image = frame.getToolkit().getImage(url);
        tracker.addImage(image, 0);
        tracker.waitForID(0);

        frame.setIconImage(image);
        if(bMacOS) {
          try {
            OSXAdapter.setDockIconImage(image);
          } catch (Exception e) {
            Activator.log.warn("Error while loading the OSXAdapter", e);
          }
        }
      } else {
      }
    } catch (Exception e) {
    }
  }

  public Icon getBundleEventIcon(int type) {
    switch(type) {
    case BundleEvent.INSTALLED:   return installIcon;
    case BundleEvent.STARTED:     return startIcon;
    case BundleEvent.STOPPED:     return stopIcon;
    case BundleEvent.UNINSTALLED: return uninstallIcon;
    case BundleEvent.UPDATED:     return updateIcon;
    default:                      return null;
    }
  }
}
