/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import java.util.StringTokenizer;
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

  final static ImageIcon emptyIcon
    = new ImageIcon(Desktop.class.getResource("/empty.gif"));
  final static Icon          updateIcon
    = new ImageIcon(Desktop.class.getResource("/update.png"));
  final static Icon startIcon
    = new ImageIcon(Desktop.class.getResource("/player_play.png"));
  final static Icon          stopIcon
    = new ImageIcon(Desktop.class.getResource("/player_stop.png"));

  final static ImageIcon uninstallIcon
    = new ImageIcon(Desktop.class.getResource("/player_eject.png"));
  final static ImageIcon installIcon
    = new ImageIcon(Desktop.class.getResource("/player_install.png"));

  /* REMOVED
    final static ImageIcon magPlusIcon
    = new ImageIcon(Desktop.class.getResource("/viewmag+.png"));
  final static ImageIcon magMinusIcon
    = new ImageIcon(Desktop.class.getResource("/viewmag-.png"));
  final static ImageIcon magFitIcon
    = new ImageIcon(Desktop.class.getResource("/viewmagfit.png"));
  final static ImageIcon mag1to1Icon
    = new ImageIcon(Desktop.class.getResource("/viewmag1.png"));
  */
  // final static ImageIcon reloadIcon
  // = new ImageIcon(Desktop.class.getResource("/reload_green.png"));

  final static ImageIcon arrowUpIcon
    = new ImageIcon(Desktop.class.getResource("/1uparrow.png"));

  final static ImageIcon arrowDownIcon
    = new ImageIcon(Desktop.class.getResource("/1downarrow.png"));

  final static ImageIcon arrowUp2Icon
    = new ImageIcon(Desktop.class.getResource("/2uparrow.png"));
  final static ImageIcon arrowDown2Icon
    = new ImageIcon(Desktop.class.getResource("/2downarrow.png"));

  final static ImageIcon viewIcon
    = new ImageIcon(Desktop.class.getResource("/view_select.png"));

  final static ImageIcon openIcon
    = new ImageIcon(Desktop.class.getResource("/open.png"));
  final static ImageIcon openURLIcon
    = new ImageIcon(Desktop.class.getResource("/bundle_small.png"));
  final static ImageIcon saveIcon
    = new ImageIcon(Desktop.class.getResource("/save.png"));

  final static ImageIcon prevIcon
    = new ImageIcon(Desktop.class.getResource("/player_prev.png"));
  final static ImageIcon nextIcon
    = new ImageIcon(Desktop.class.getResource("/player_next.png"));
  final static ImageIcon connectIcon
    = new ImageIcon(Desktop.class.getResource("/connect.png"));
  final static ImageIcon connectIconLarge
    = new ImageIcon(Desktop.class.getResource("/connect48x48.png"));

  final static ImageIcon tipIcon
    = new ImageIcon(Desktop.class.getResource("/idea.png"));
  final static ImageIcon floatIcon
    = new ImageIcon(Desktop.class.getResource("/float.png"));

  final static int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

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



  public void start() {

    slTracker =
      new ServiceTracker(Activator.getTargetBC(),
                         StartLevel.class.getName(), null);
    slTracker.open();

    pkgTracker =
      new ServiceTracker(Activator.getTargetBC(),
                         PackageAdmin.class.getName(), null);
    pkgTracker.open();


    lfManager = new LFManager();
    lfManager.init();

    consoleSwing = new ConsoleSwing(Activator.getTargetBC());
    consoleSwing.start();

    toolBar       = makeToolBar();
    statusBar     = new StatusBar("");

    String rName = Activator.remoteHost;
    String spid  = Activator.getBC().getProperty("org.osgi.provisioning.spid");

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
    // Bundle[]  bl = Activator.getTargetBCbundles();
    // for(int i = 0; i  < bl.length; i++) {
    //   bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bl[i]));
    // }
    bundleChanged((BundleEvent) null);

    pm = new PackageManager(pkgTracker);

    frame.setJMenuBar(menuBar = makeMenuBar());


    setRemote(Activator.remoteTracker.getService() != null);

    setIcon(frame, "/kf-");

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
          public Object addingService(final ServiceReference sr) {
            final SwingBundleDisplayer disp =
              (SwingBundleDisplayer)super.addingService(sr);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {

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

                }
              });
            return disp;
          }

          public void removedService(final ServiceReference sr,
                                     final Object service) {
            final SwingBundleDisplayer disp = (SwingBundleDisplayer)service;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
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
                }
              });
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

    updateBundleViewSelections();
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

  final Action actionStartBundles
    = new AbstractAction(Strings.get("item_startbundles"), startIcon) {
        public void actionPerformed(ActionEvent ev) {
          startBundles(getSelectedBundles());
        }
      };
  final Action actionStopBundles
    = new AbstractAction(Strings.get("item_stopbundles"), stopIcon) {
        public void actionPerformed(ActionEvent ev) {
          stopBundles(getSelectedBundles());
        }
      };
  final Action actionUpdateBundles =
      new AbstractAction(Strings.get("item_updatebundles"), updateIcon) {
        public void actionPerformed(ActionEvent ev) {
          updateBundles(getSelectedBundles());
        }
      };

  final Action actionUninstallBundles =
      new AbstractAction(Strings.get("item_uninstallbundles"), uninstallIcon) {
        public void actionPerformed(ActionEvent ev) {
          uninstallBundles(getSelectedBundles());
        }
      };

  final Action actionRefreshBundles =
      new AbstractAction(Strings.get("menu_refreshbundles")) {
        {
          putValue(ACCELERATOR_KEY,
                   KeyStroke.getKeyStroke(KeyEvent.VK_R, mask));
          putValue(SHORT_DESCRIPTION, Strings.get("menu_refreshbundles.descr"));
        }
        public void actionPerformed(ActionEvent ev) {
          refreshBundle(getSelectedBundles());
        }
      };

  final Action actionResolveBundles =
      new AbstractAction(Strings.get("menu_resolvebundles")) {
        {
          putValue(SHORT_DESCRIPTION, Strings.get("menu_resolvebundles.descr"));
        }
        public void actionPerformed(ActionEvent ev) {
          resolveBundles(getSelectedBundles());
        }
      };


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

          add(new JToolbarButton(actionStartBundles));
          add(new JToolbarButton(actionStopBundles));
          add(new JToolbarButton(actionUpdateBundles));
          add(new JToolbarButton(actionUninstallBundles));

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
    final JRadioButtonMenuItem item = (JRadioButtonMenuItem) menuMap.get(sr);
    if (null!=item) {
      item.setSelected(true);
    }
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

    actionStartBundles.setEnabled(bEnabled);
    actionStopBundles.setEnabled(bEnabled);
    actionUninstallBundles.setEnabled(bEnabled);
    actionUpdateBundles.setEnabled(bEnabled);

    toolBar.invalidate();
    menuBar.invalidate();

    if (null!=startLevelMenu) {
      startLevelMenu.setEnabled(bEnabled);
    }

    if(levelMenuLabel != null) {
      levelMenuLabel.setText(Strings.get("startlevel.noSel"));
      noStartLevelSelected.setSelected(true);
    }

    final StartLevel sls = (StartLevel) slTracker.getService();
    if (null!=sls) {
      final Set levels = new HashSet();
      final Set bids = new HashSet();
      for (int i=0; i<bl.length; i++) {
        try {
          //levels.add(new Integer(sls.getBundleStartLevel(bl[i])));
          final Integer lvl = new Integer(sls.getBundleStartLevel(bl[i]));
          levels.add(lvl);
          bids.add(new Long(bl[i].getBundleId()));
        } catch (Exception e) {
        }
      }
      levelMenuLabel.setText("Bundle " + bids);
      if (1==levels.size()) {
        final Integer level = (Integer) levels.iterator().next();
        final AbstractButton jrb = (AbstractButton) levelCheckBoxes.get(level);
        if (null!=jrb) jrb.setSelected(true);
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
  JMenuItem itemResolveBundles;
  JMenuItem itemRefreshBundles;
  JMenu     startLevelMenu;


  JMenu makeBundleMenu() {

    return new JMenu(Strings.get("menu_bundles")) {
        {
          add(itemStopBundles   = new JMenuItem(actionStopBundles));
          add(makeStopOptionsMenu());
          addSeparator();
          add(itemStartBundles  = new JMenuItem(actionStartBundles));
          add(makeStartOptionsMenu());
          addSeparator();
          add(itemUpdateBundles = new JMenuItem(actionUpdateBundles));
          add(itemUninstallBundles = new JMenuItem(actionUninstallBundles));
          add(itemResolveBundles   = new JMenuItem(actionResolveBundles));
          add(itemRefreshBundles   = new JMenuItem(actionRefreshBundles));

          StartLevel sls = (StartLevel)slTracker.getService();
          if(sls != null) {
            add(startLevelMenu = makeStartLevelMenu());
          }
        }
      };
  }

  // Integer -> AbstractButton
  Map    levelCheckBoxes = new HashMap();
  // Use a menu item here even though a label should suffice, but the
  // MacOSX mapping to native (AWT) menu items requires a menu item to
  // work.
  JMenuItem levelMenuLabel  = null;
  // Invisible menu item that when selected represents no selection.
  final JRadioButtonMenuItem noStartLevelSelected
    = new JRadioButtonMenuItem("no selection");

  JMenu makeStartLevelMenu() {
    return new JMenu(Strings.get("menu_startlevel")) {
        {
          setToolTipText(Strings.get("startlevel.descr"));
          final ButtonGroup group = new ButtonGroup();
          group.add(noStartLevelSelected);

          add(levelMenuLabel = new JMenuItem(Strings.get("startlevel.noSel")));
          add(new JSeparator());

          for(int i = levelMin; i <= levelMax; i++) {
            final AbstractButton jrb = new JRadioButtonMenuItem(Integer.toString(i));
            group.add(jrb);
            add(jrb);
            jrb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                  final StartLevel sls = (StartLevel)slTracker.getService();

                  final Bundle[] bl = getSelectedBundles();

                  if(null != sls) {
                    final int level = Integer.parseInt(jrb.getText());
                    for (int i=0; i<bl.length; i++) {
                      sls.setBundleStartLevel(bl[i], level);
                    }
                    updateBundleViewSelections();
                  }
                }
              });

            levelCheckBoxes.put(new Integer(i), jrb);
          }
        }
      };
  }


  JCheckBoxMenuItem itemStopOptionsTransient;

  JMenu makeStopOptionsMenu() {
    return new JMenu(Strings.get("menu_stopOptions")) {
        {
          setToolTipText(Strings.get("menu_stopOptions.descr"));

          final ItemListener itemListener = new ItemListener(){
              public void itemStateChanged(ItemEvent e){
                updateNameOfActionStopBundles();
              }
            };

          itemStopOptionsTransient = new JCheckBoxMenuItem
            (Strings.get("stop_option_transient"), false);
          itemStopOptionsTransient.setToolTipText
            (Strings.get("stop_option_transient.descr"));
          itemStopOptionsTransient.addItemListener(itemListener);

          add(itemStopOptionsTransient);
          updateNameOfActionStopBundles();
        }
      };
  }

  void updateNameOfActionStopBundles()
  {
    final boolean trans = itemStopOptionsTransient.getState();
    final String name = Strings.get("item_stopbundles")
      +(trans ? " (" : "")
      +(trans ? Strings.get("stop_option_transient") : "")
      +(trans ? ")" : "");

    actionStopBundles.putValue(Action.NAME, name);
  }

  // Get stop option settings from the menu.
  int getStopOptions()
  {
    int options = 0;
    if (itemStopOptionsTransient.getState()) {
      options |= Bundle.STOP_TRANSIENT;
    }
    return options;
  }


  JCheckBoxMenuItem itemStartOptionsTransient;
  JCheckBoxMenuItem itemStartOptionsPolicy;

  JMenu makeStartOptionsMenu() {
    return new JMenu(Strings.get("menu_startOptions")) {
        {
          setToolTipText(Strings.get("menu_startOptions.descr"));

          final ItemListener itemListener = new ItemListener(){
              public void itemStateChanged(ItemEvent e){
                updateNameOfActionStartBundles();
              }
            };

          itemStartOptionsTransient = new JCheckBoxMenuItem
            (Strings.get("start_option_transient"), false);
          itemStartOptionsTransient.setToolTipText
            (Strings.get("start_option_transient.descr"));
          itemStartOptionsTransient.addItemListener(itemListener);

          itemStartOptionsPolicy = new JCheckBoxMenuItem
            (Strings.get("start_option_policy"), true);
          itemStartOptionsPolicy.setToolTipText
            (Strings.get("start_option_policy.descr"));
          itemStartOptionsPolicy.addItemListener(itemListener);

          add(itemStartOptionsTransient);
          add(itemStartOptionsPolicy);
          updateNameOfActionStartBundles();
        }
      };
  }

  void updateNameOfActionStartBundles()
  {
    boolean trans  = itemStartOptionsTransient.getState();
    boolean policy = itemStartOptionsPolicy.getState();

    final String name = Strings.get("item_startbundles")
      +" ("
      +(trans  ? Strings.get("start_option_transient") : "")
      +(trans  ? ", " : "")
      +Strings.get(policy ? "start_option_policy" : "start_option_eager")
      +")";
    actionStartBundles.putValue(Action.NAME, name);
  }


  // Get start option settings from the menu.
  int getStartOptions()
  {
    int options = 0;
    if (itemStartOptionsTransient.getState()) {
      options |= Bundle.START_TRANSIENT;
    }
    if (itemStartOptionsPolicy.getState()) {
      options |= Bundle.START_ACTIVATION_POLICY;
    }
    return options;
  }



  JMenu viewMenu = null;
  JMenu editMenu = null;

  JMenu makeViewMenu(JMenu oldMenu) {
    if (consoleSwing==null) return null; // Desktop already stopped

    JMenu menu;

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
            menuMap.put(sr, this);
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                  bundlePanelShowTab(sr);
                }
              });
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

  static final String FILE_PROTO = "file:";

  /**
   * Open a file dialog and ask for jar files to install as bundles.
   */
  void addBundle() {
    if(openFC == null) {
      openFC = new JFileChooser();
      File cwd = new File(Util.getProperty("user.dir", "."));
      final String jarsProp=Util.getProperty("org.knopflerfish.gosg.jars",null);
      if (jarsProp!=null) {
        final StringTokenizer st = new StringTokenizer(jarsProp,";");
        while (st.hasMoreTokens()) {
          final String url = st.nextToken().trim();
          if(url.startsWith(FILE_PROTO)) {
            final File dir = new File(url.substring(FILE_PROTO.length()));
            if (dir.exists()) {
              cwd = dir;
              break;
            }
          }
        }
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

    final PackageAdmin pkgAdmin = (PackageAdmin) pkgTracker.getService();
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
    final ArrayList res = new ArrayList(bundleCache.length);

    for(int i = 0; i < bundleCache.length; i++) {
      final Bundle b = bundleCache[i];
      if(isSelected(b)) {
        res.add(b);
      }
    }
    return (Bundle[]) res.toArray(new Bundle[res.size()]);
  }

  void startBundle(final Bundle b) {
    // Must not call start() from the EDT, since that will block the
    // EDT untill the start()-call completes.
    new Thread("Desktop-StartBundle "+b.getBundleId())
    {
      public void run()
      {
        try {
          b.start(getStartOptions());
        } catch (Exception e) {
          showErr("Failed to start bundle " +Util.getBundleName(b) +": " +e, e);
        }
      }
    }.start();
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



  void stopBundle(final Bundle b) {
    // Special handling needed when stopping the desktop itself.
    final boolean stoppingSelf = b.getBundleId() == 0
      || b.getBundleId() == Activator.getTargetBC().getBundle().getBundleId();

    int n = 0;
    if(stoppingSelf) {
      final Object[] options = { Strings.get("yes"),
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
      // Must not call stop() from the EDT, since that will block the
      // EDT untill the stop()-call completes.
      new Thread("Desktop-StopBundle " +b.getBundleId())
      {
        public void run()
        {
          try {
            b.stop(getStopOptions());
          } catch (Exception e) {
            showErr("Failed to stop bundle " +Util.getBundleName(b) +": " +e,
                    e);
          }
        }
      }.start();
    }
  }

  void refreshBundle(final Bundle[] b) {
    final ServiceReference sr
      = Activator.getTargetBC_getServiceReference(PackageAdmin.class.getName());

    if(sr != null) {
      final PackageAdmin packageAdmin = (PackageAdmin)
        Activator.getTargetBC_getService(sr);

      if(packageAdmin != null) {
        final boolean refreshAll = b==null || 0==b.length;
        // Must not call refreshPackages() from the EDT, since that
        // will may the EDT.

        final StringBuffer sb = new StringBuffer("Desktop-RefreshPackages ");
        if (refreshAll) {
          sb.append("all packages pending removal");
        } else {
          sb.append("bundle packages for ");
          for (int i=0; i<b.length; i++) {
            if (i>0) {
              sb.append(", ");
            }
            sb.append(b[i].getBundleId());
          }
        }

        new Thread(sb.toString())
        {
          public void run()
          {
            try {
              packageAdmin.refreshPackages( refreshAll ? null : b);
            } catch (Exception e) {
              showErr(sb.toString() + " failed to refresh bundles: "+e , e);
            } finally {
              Activator.getTargetBC().ungetService(sr);
            }
          }
        }.start();

      }
    }
  }

  void resolveBundles(final Bundle[] b) {
    final ServiceReference sr = Activator.getTargetBC()
      .getServiceReference(PackageAdmin.class.getName());

    if(sr != null) {
      final PackageAdmin packageAdmin = (PackageAdmin)
        Activator.getTargetBC().getService(sr);

      if(packageAdmin != null) {
        final boolean resolveAll = b==null || 0==b.length;
        // Must not call resolve() from the EDT, since that will block
        // the EDT.

        final StringBuffer sb = new StringBuffer("Desktop-ResolveBundles: ");
        if (resolveAll) {
          sb.append("all bundles needing to be resolved ");
        } else {
          sb.append("selected bundle(s) ");
          for (int i=0; i<b.length; i++) {
            if (i>0) {
              sb.append(", ");
            }
            sb.append(b[i].getBundleId());
          }
        }

        new Thread(sb.toString())
        {
          public void run()
          {
            try {
              if (!packageAdmin.resolveBundles( resolveAll ? null : b)) {
                showErr(sb.toString() + "; could not resolve all of them.",
                        null);
              }
            } catch (Exception e) {
              showErr(sb.toString() + " failed to resolve bundles: "+e , e);
            } finally {
              Activator.getTargetBC().ungetService(sr);
            }
          }
        }.start();

      }
    }
  }

  void updateBundle(final Bundle b) {
    new Thread("Desktop-UpdateBundle " +b.getBundleId())
    {
      public void run()
      {
        try {
          b.update();
        } catch (Exception e) {
          showErr("Failed to update bundle " +Util.getBundleName(b) +": " +e ,
                  e);
        }
      }
    }.start();
  }

  boolean uninstallBundle(final Bundle b, boolean bAsk) {
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
      // Must not call uninstall() from the EDT, since that will block
      // the EDT untill the uninstall()-call completes.
      new Thread("Desktop-UninstallBundle " +b.getBundleId())
      {
        public void run()
        {
          try {
            b.uninstall();
          } catch (Exception e) {
            showErr("failed to uninstall bundle " + Util.getBundleName(b), e);
          }
        }
      }.start();
    }
    return false;
  }




  void showErr(String msg, Exception e) {
    Throwable t = e;
    if (null!=t) {
      while(t instanceof BundleException &&
            ((BundleException) t).getNestedException() != null) {
        t = ((BundleException) t).getNestedException();
      }
    }
    if (Util.getBooleanProperty("org.knopflerfish.desktop.dontuseerrordialog",
                                false)) {
      if(msg != null && !"".equals(msg)) {
        System.out.println(msg);
      }
      if (null!=t) {
        t.printStackTrace();
      }
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

  volatile Bundle[] bundleCache;


  public void bundleChanged(final BundleEvent ev) {
    if(!alive) {
      return;
    }

    // This callback method is marked as "NotThreadSafe" in the
    // javadoc, that implies that the thread calling it may hold
    // internal framework locks, thus since we need to call into the
    // framework all work must be done on a separate thread to avoid
    // dead-locks.
    final Bundle bundle = null!=ev ? ev.getBundle() : null;
    final String threadName = "Desktop.bundleChanged("
      +(null==ev ? "" :
        (Util.bundleEventName(ev.getType()) +", "
         +(null==bundle ? "*" : String.valueOf(bundle.getBundleId()))))
      +")";

    final Thread bct = new Thread(threadName) {
        public void run()
        {
          if (null!=ev) {
            if (BundleEvent.UPDATED==ev.getType()) {
              // An updated bundle may have changed icon...
              Util.clearBundleIcon(ev.getBundle());
            }
          }

          if(pm != null) {
            pm.refresh();
          }
          bundleCache = Activator.getBundles();

          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                if (ev!=null && BundleEvent.INSTALLED==ev.getType()) {
                  // Select bundle when installed
                  setSelected(bundle);
                  showBundle(bundle);
                } else if (null!=bundle && isSelected(bundle)) {
                  if (BundleEvent.UNINSTALLED==ev.getType()) {
                    bundleSelModel.setSelected(bundle.getBundleId(), false);
                  } else {
                    // Trigger a selection change notification to tell
                    // displayers to update their contents
                    if (bundleSelModel instanceof DefaultBundleSelectionModel) {
                      final DefaultBundleSelectionModel dbsm
                        = (DefaultBundleSelectionModel) bundleSelModel;
                      dbsm.fireChange(bundle.getBundleId());
                    }
                  }
                }
                updateStatusBar();
                updateMenus();
                toolBar.revalidate();
                toolBar.repaint();
              }
            });
        }
      };
    bct.start();
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
    // Frame icon
    final String iconName1
      = baseName +(Util.isWindows() ? "16x16.png" : "32x32.png");
    // Max OS X dock icon
    final String iconName2 = baseName +"128x128.png";
    final MediaTracker tracker = new MediaTracker(frame);
    try {
      final URL[] urls = new URL[]{
        getClass().getResource(iconName1),
        getClass().getResource(iconName2)
      };

      final Image[] images = new Image[urls.length];
      for (int i=0; i<urls.length; i++) {
        if(urls[i] != null) {
          images[i] = frame.getToolkit().getImage(urls[i]);
          tracker.addImage(images[i], 0);
        }
      }
      tracker.waitForID(0);

      if (null!=images[0]) frame.setIconImage(images[0]);
      // Set the dock icon.
      if(null!=images[1] && bMacOS) {
        try {
          OSXAdapter.setDockIconImage(images[1]);
        } catch (Exception e) {
          Activator.log.warn("Error while loading the OSXAdapter", e);
        }
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
