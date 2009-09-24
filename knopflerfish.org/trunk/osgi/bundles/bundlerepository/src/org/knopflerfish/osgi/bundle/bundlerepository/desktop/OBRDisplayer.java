/*
 * Copyright (c) 2004-2009, KNOPFLERFISH project
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

package org.knopflerfish.osgi.bundle.bundlerepository.desktop;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import java.lang.reflect.Array;

import java.net.URL;
import java.net.URLConnection;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.ungoverned.osgi.service.bundlerepository.BundleRecord;
import org.ungoverned.osgi.service.bundlerepository.BundleRepositoryService;


/**
 * Desktop plugin for the BundleRepositoryService
 * with functionality for install/start and detail information.
 */
public class OBRDisplayer
  extends DefaultSwingBundleDisplayer
  implements ServiceTrackerCustomizer
{

  static ServiceTracker obrTracker;


  // Shared by all instances of JOBRAdmin
  static ImageIcon startIcon;
  static ImageIcon sortIcon;
  static ImageIcon installIcon;
  static ImageIcon updateIcon;
  static ImageIcon bundleIcon;
  static ImageIcon reloadIcon;

  static final int SORT_NONE        = 0;
  static final int SORT_HOST        = 1;
  static final int SORT_CATEGORY    = 2;
  static final int SORT_VENDOR      = 3;
  static final int SORT_APIVENDOR   = 4;
  static final int SORT_STATUS      = 5;

  static int[] SORT_ARRAY = new int[] {
    SORT_NONE,
    SORT_HOST,
    SORT_CATEGORY,
    SORT_VENDOR,
    SORT_STATUS,
  };

  static String[] SORT_NAMES = new String[] {
    "All",
    "Host",
    "Category",
    "Vendor",
    "Install status",
  };


  // Error message for all instances.
  // since the OBR service is shared, the err message
  // can be shared too
  String obrErr = "";

  // Message while loading repo URLs
  static String STR_LOADING   = "Loading...";

  // Name of top node
  static String  STR_TOPNAME  = "Bundle Repository";

  public OBRDisplayer(BundleContext bc) {
    super(bc, "Bundle Repository", "View and install bundles from Bundle Repository", true);

    try {
      // share icon instances between instances
      if(startIcon == null) {
        startIcon   = new ImageIcon(getClass().getResource("/player_play.png"));
        installIcon = new ImageIcon(getClass().getResource("/player_install.png"));
        updateIcon  = new ImageIcon(getClass().getResource("/update.png"));
        sortIcon    = new ImageIcon(getClass().getResource("/sort_select.png"));
        bundleIcon  = new ImageIcon(getClass().getResource("/lib16x16.png"));
        reloadIcon     = new ImageIcon(getClass().getResource("/reload_green.png"));

      }
    } catch (Exception e) {

      System.err.println("icon load failed: " + e);
    }

    obrTracker = new ServiceTracker(bc,
                                    BundleRepositoryService.class.getName(),
                                    this);
    obrTracker.open();
  }


  BundleRepositoryService getOBR() {
    return (BundleRepositoryService)OBRDisplayer.obrTracker.getService();
  }

  public JComponent newJComponent() {
    return new JOBRAdmin();
  }

  public void  disposeJComponent(JComponent comp) {
    JOBRAdmin obrAdmin = (JOBRAdmin)comp;
    obrAdmin.stop();

    super.disposeJComponent(comp);
  }

  void closeComponent(JComponent comp) {
    JOBRAdmin obrAdmin = (JOBRAdmin)comp;
    obrAdmin.stop();
  }

  public void valueChanged(final long bid) {
    super.valueChanged(bid);
    // If unselection adn multiple selected bundles, choose another one.
    long selectedBid = bundleSelModel.getSelectionCount()>1
      ? bundleSelModel.getSelected() : bid;
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JOBRAdmin obrAdmin = (JOBRAdmin)(JComponent)it.next();
      obrAdmin.valueChanged(selectedBid);
    }
  }

  public Icon getSmallIcon() {
    return null;
  }

  /*------------------------------------------------------------------------*
   *			  ServiceTrackerCustomizer implementation
   *------------------------------------------------------------------------*/

  public Object addingService(ServiceReference reference) {
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JOBRAdmin obrAdmin = (JOBRAdmin)(JComponent)it.next();
      obrAdmin.refreshList(false);
    }
    return bc.getService(reference);
  }

  public void modifiedService(ServiceReference reference, Object service) {
  }

  public void removedService(ServiceReference reference, Object service) {
  }


  /**
   * The actual component returned by newJComponent
   */
  class JOBRAdmin extends JPanel {

    DefaultTreeModel treeModel;
    JTree       recordTree;
    JPanel      recordPanel;
    JButton     installButton;
    JButton     refreshButton;
    JButton     startButton;
    JButton     updateButton;
    JTextPane   html;
    JScrollPane htmlScroll;


    TopNode    rootNode;

    // currently selected node
    OBRNode    brSelected = null;

    JMenuItem  contextItem;
    JPopupMenu contextPopupMenu;

    // Category used for grouping bubdle records
    int sortCategory = SORT_CATEGORY;

    // Map of all bundles in OBR tree
    // String (location) -> OBRNode
    Map locationMap = new HashMap();

    public JOBRAdmin() {
      setLayout(new BorderLayout());


      recordTree = new JTree(new TopNode("[not loaded]"));
      recordTree.setRootVisible(true);

      // Must be registered for renderer tooltips to work
      ToolTipManager.sharedInstance().registerComponent(recordTree);

      // Load leaf icon for the tree cell renderer.
      TreeCellRenderer renderer = new DefaultTreeCellRenderer() {
          public Component getTreeCellRendererComponent(JTree tree,
                                                        Object value,
                                                        boolean sel,
                                                        boolean expanded,
                                                        boolean leaf,
                                                        int row,
                                                        boolean hasFocus) {

            Component c =
              super.getTreeCellRendererComponent(tree, value, sel,
                                                 expanded, leaf, row,
                                                 hasFocus);

            TreePath tp = tree.getPathForRow(row);

            try {
              Object node = tp.getLastPathComponent();
              String tt = null;
              if(node instanceof OBRNode) {
                OBRNode obrNode = (OBRNode)node;
                setIcon(obrNode.bBusy ? reloadIcon : bundleIcon);
                String loc = (String)obrNode.getBundleRecord().getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
                tt = obrNode.bBusy ? "busy..." : loc;

                boolean bInstalled = isInstalled(obrNode.getBundleRecord());
                obrNode.setInstalled(bInstalled);
                if(bInstalled) {
                  setForeground(Color.gray);
                }
              } else if(node instanceof TopNode) {
                TopNode topNode = (TopNode)node;
                if(STR_LOADING.equals(topNode.name)) {
                  setIcon(reloadIcon);
                }
              } else {
                //                setIcon(null);
              }
              setToolTipText(tt);
            } catch (Exception ignored ) {
            }
            return this;
          }
        };

      recordTree.setCellRenderer(renderer);


      // call setSelected() when user selects nodes/leafs in the tree
      recordTree.addTreeSelectionListener(new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
            TreePath[] sel = recordTree.getSelectionPaths();
            if(sel != null && sel.length == 1) {
              setSelected((TreeNode)sel[0].getLastPathComponent());
            } else {
              setSelected(null);
            }
          }
        });

      // Create the HTML text pane for detail view.
      // The node's getTitle()/toHTML() methods
      // will be called whenever a node is HTMLAble
      html = new JTextPane();
      html.setText("");
      html.setContentType("text/html");

      html.setEditable(false);

      html.addHyperlinkListener(new HyperlinkListener() {
          public void hyperlinkUpdate(HyperlinkEvent ev) {
            if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
              URL url = ev.getURL();
              System.out.println("Link : "+url +" activated");
              try {
                Util.openExternalURL(url);
              } catch (Exception e) {
                System.out.println("Failed to open external url=" + url
                                   +" reason: " +e);
              }
            }
          }
        });

      htmlScroll =
        new JScrollPane(html,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      htmlScroll.setPreferredSize(new Dimension(300, 300));

      JScrollPane treeScroll =
        new JScrollPane(recordTree,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      treeScroll.setPreferredSize(new Dimension(200, 300));

      JButton repoButton = new JButton("URLs");
      repoButton.setToolTipText("Show/set repository URLs");
      repoButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            askRepoURls();
          }
        });

      installButton = new JButton(installIcon);
      installButton.setToolTipText("Install from OBR");
      ActionListener installAction;
      installButton.addActionListener(installAction = new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            installOrStart(brSelected, false);
          }
        });
      installButton.setEnabled(false);

      startButton = new JButton(startIcon);
      startButton.setToolTipText("Install and start from OBR");
      ActionListener startAction;
      startButton.addActionListener(startAction = new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            installOrStart(brSelected, true);
          }
        });
      startButton.setEnabled(false);

      refreshButton = new JButton(reloadIcon);
      refreshButton.setToolTipText("Refresh OBR list");
      refreshButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            refreshList(true);
          }
        });

      recordPanel = new JPanel(new BorderLayout());
      recordPanel.add(htmlScroll, BorderLayout.CENTER);

      JPanel left = new JPanel(new BorderLayout());

      left.add(treeScroll, BorderLayout.CENTER);

      JToolBar leftTools = new JToolBar();
      leftTools.add(makeSortSelectionButton());
      leftTools.add(refreshButton);

      leftTools.add(installButton);
      leftTools.add(startButton);
      leftTools.add(repoButton);

      left.add(leftTools, BorderLayout.SOUTH);

      // create a context menu which copies the names
      // and actions from the start and stop buttons.
      contextPopupMenu = new JPopupMenu();
      contextItem = new JMenuItem("------------");
      contextItem.setEnabled(false);

      contextPopupMenu.add(contextItem);
      contextPopupMenu.add(new JPopupMenu.Separator());

      JMenuItem item;

      item = new JMenuItem(startButton.getToolTipText(),
                           startButton.getIcon());
      item.addActionListener(startAction);
      contextPopupMenu.add(item);

      item = new JMenuItem(installButton.getToolTipText(),
                           installButton.getIcon());
      item.addActionListener(installAction);
      contextPopupMenu.add(item);


      // add listener for tree context menu, which selects the
      // item belo the mouse and pops up the context menu.
      recordTree.addMouseListener(new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
          }
          public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
          }
          private void maybeShowPopup(MouseEvent e) {
            int mod = e.getModifiers();
            if(contextPopupMenu != null &&
               (e.isPopupTrigger() ||
                ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0))) {
              TreePath tp = recordTree.getPathForLocation(e.getX(), e.getY());
              if(tp != null) {
                TreeNode node = (TreeNode)tp.getLastPathComponent();
                if(node instanceof OBRNode) {
                  contextItem.setText(((OBRNode)node).name);
                  recordTree.setSelectionPath(tp);
                  setSelected(node);
                  Component comp = e.getComponent();
                  contextPopupMenu.show(comp, e.getX(), e.getY());
                }
              }
            }
          }
        });


      JSplitPane panel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                        left,
                                        recordPanel);

      panel.setDividerLocation(200);


      add(panel, BorderLayout.CENTER);

      refreshList(true);
    }

    /**
     * Called when BundleSelectionListener distributes events.
     *
     * <p>
     * If possible, select and show the bundle with id <tt>bid</tt>
     * in the tree.
     * </p>
     */
    void valueChanged(long bid) {
      try {
        if(bid >= 0) {
          Bundle b = bc.getBundle(bid);
          if(b != null) {
            OBRNode obrNode = getOBRNode(b);
            if(obrNode != null) {
              if (obrNode != brSelected) {
                TreePath tp = new TreePath(obrNode.getPath());
                showPath(tp, null);
              }
            } else {
              setSelected(null);
            }
          }
        }
      } catch (Exception e) {
      }
    }

    /**
     * Get the OBRNode tree node which matches a bundle.
     *
     * @return OBRNode for the specified bundle if it exists,
     *         <tt>null</tt> otherwise.
     */
    OBRNode getOBRNode(Bundle b) {
      OBRNode node = (OBRNode)locationMap.get(b.getLocation());

      if(node != null) {
        return node;
      }

      for(Iterator it = locationMap.keySet().iterator(); it.hasNext(); ) {
        String  loc  = (String)it.next();
        node = (OBRNode)locationMap.get(loc);
        if(Util.bundleEqual(b, node.getBundleRecord())) {
          return node;
        }
      }
      return null;
    }


    /**
     * Create the sort selection button, with items
     * defined in SORT_ARRAY/SORT_NAMES
     */
    JButton makeSortSelectionButton() {
      JButton sortButton = new JButton(sortIcon);
      sortButton.setToolTipText("Select sorting");

      final JPopupMenu  sortPopupMenu = new JPopupMenu();
      ButtonGroup       group         = new ButtonGroup();

      for(int i = 0; i < SORT_ARRAY.length; i++) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(SORT_NAMES[i]);
        final int cat = SORT_ARRAY[i];
        item.setState(cat == sortCategory);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              sortCategory = cat;
              refreshList(false);
            }
          });
        sortPopupMenu.add(item);
        group.add(item);
      }

      sortButton.addMouseListener(new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            showPopup(e);
          }
          public void mouseReleased(MouseEvent e) {
            showPopup(e);
          }
          private void showPopup(MouseEvent e) {
            Component comp = e.getComponent();
            sortPopupMenu.show(comp, 0, comp.getSize().height);
          }
        });

      return sortButton;
    }


    boolean bBusy = false;

    /**
     * Install or install+start a bundle specified by an OBRNode
     * using the BundleRepositoryService. Run an a new thread to
     * avoid blocking the swing thread for potentially long operations.
     *
     * @param obrNode Bundle to install/start
     * @param bStart if <tt>true</t>, start, otherewise just install
     */
    void installOrStart(final OBRNode obrNode, final boolean bStart) {
      if(bBusy) {
        return;
      }
      new Thread() {
        { start(); }
        public void run() {
          try {
            bBusy = obrNode.bBusy = true;
            setSelected(obrNode);
            installOrStartSync(obrNode, bStart);
          } catch (Exception e) {
            obrNode.appendLog("" + e + "\n");
          } finally {
            bBusy = obrNode.bBusy = false;
            setSelected(obrNode);
          }
        }
      };
    }

    void installOrStartSync(OBRNode obrNode, boolean bStart) {
      BundleRecord br = obrNode.getBundleRecord();
      if(br == null) {
        return;
      }

      String updateURL =
        (String)br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);

      // Check if bundle already is installed. If so,
      // ask user if it should be updated, installed again or
      // if the operation should be cancelled.
      Bundle b = getBundle(br);
      if(b != null) {

        boolean bIsRepoBundle = Util.isInRepo(b, updateURL);

        String[] options = bIsRepoBundle
          ? (new String[] { "Update", "Cancel",          })
          : (new String[] { "Update", "Install again", "Cancel",  });

        String msg = bIsRepoBundle
          ?
          "The selected bundle is already installed\n" +
          "from the repository.\n" +
          "\n" +
          "It can be updated from the repository."
          :
          "The selected bundle is already installed.\n" +
          "\n" +
          "It can be updated from the repository, or\n" +
          "a new instance can be installed from the\n" +
          "repository";

        int n = JOptionPane
          .showOptionDialog(recordTree,
                            msg,
                            "Bundle is installed", // title
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, // icon
                            options,
                            options[0]);

        if(bIsRepoBundle) {
          if(n == 0) {        // update
            obrNode.appendLog("update.\n");
          } else if(n == 1) { // Cancel
            return;
          }
        } else {
          if(n == 0) { // Update
            InputStream in = null;
            try {
              URL url = new URL(updateURL);
              in = new BufferedInputStream(url.openStream());
              b.update(in);
              obrNode.appendLog("Updated from " + url + "\n");
            } catch (Exception e) {
              obrNode.appendLog("Update failed: " + e + "\n");
            } finally {
              try { in.close(); } catch (Exception ignored) { }
            }
            return;
          } else if(n == 1) { // install new
            obrNode.appendLog("Install new instance.\n");
          } else if(n == 2) { // cancel
            return;
          }
        }
      }

      // Install the budle from repo

      BundleRepositoryService obr = getOBR();

      // deployBundle writes all info to streams, so we have to
      // catch that info. The text will be displayed by
      // the node's toHTML method
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      PrintStream outStream = new PrintStream(bout, true);

      // We always resolve.
      boolean bResolve = true;

      try {
        boolean bOK = obr.deployBundle(outStream, // Output stream.
                                       outStream, // Error stream.
                                       updateURL,
                                       bResolve,
                                       bStart);
        if(bOK) {
          if(sortCategory == SORT_STATUS) {
            refreshList(false);
          }
        }
      } catch (Exception e) {
        e.printStackTrace(outStream);
      } finally {
        try {  outStream.close();} catch (Exception ignored) {        }
        String s = new String(bout.toByteArray());
        obrNode.appendLog(s);
      }

      setSelected(obrNode);
    }

    /**
     * Refresh the OBR tree and try to reshow/reselect
     * any previously selected bundle node.
     *
     * @param bReload if <tt>true</tt>, reload the bundle repository
     *                XML files, otherewise, just rebuild the tree.
     */
    synchronized void refreshList(final boolean bReload) {
      Thread t = new Thread() {
          public void run() {
            locationMap.clear();
            BundleRecord brOld = brSelected != null ? brSelected.getBundleRecord() : null;

            setRootText(STR_LOADING);

            rootNode = new TopNode(STR_TOPNAME);
            treeModel = new DefaultTreeModel(rootNode);

            BundleRepositoryService obr = getOBR();

            if(obr != null) {
              if(bReload) {
                obrErr = "";
                try {
                  assertRepoURLs(obr.getRepositoryURLs());
                  obr.setRepositoryURLs(obr.getRepositoryURLs());
                } catch (Exception e) {
                  obrErr =
                    "<b>" + e.getClass().getName() + "</b>"+
                    "<pre>\n" +
                    e.getMessage() +
                    "</pre>";
                }
              }
              int count = obr.getBundleRecordCount();

              // String (category) -> Set (BundleRecord)
              Map categories = new TreeMap(new Comparator() {
                  public int compare(Object o1, Object o2) {
                    return o1.toString().compareToIgnoreCase(o2.toString());
                  }
                });

              // move all bundle records into a sorted
              // category map of sets
              for(int i = 0; i < count; i++) {
                BundleRecord br = obr.getBundleRecord(i);

                String loc = (String)br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);

                String category = "other";
                if(sortCategory == SORT_CATEGORY) {
                  category = Util.getAttribute(br,
                                               BundleRecord.BUNDLE_CATEGORY,
                                               "[no category]");
                } else if(sortCategory == SORT_VENDOR) {
                  category = Util.getAttribute(br,
                                               BundleRecord.BUNDLE_VENDOR,
                                               "[no vendor]");
                } else if(sortCategory == SORT_STATUS) {
                  if(isInstalled(br)) {
                    category = "Installed";
                  } else {
                    category = "Not installed";
                  }
                } else if(sortCategory == SORT_NONE) {
                  category = SORT_NAMES[SORT_NONE];
                } else {
                  int ix = loc.indexOf(":");
                  if(ix != -1) {
                    category = loc.substring(0, ix);
                    if(loc.startsWith("http://")) {
                      ix = loc.indexOf("/", 8);
                      if(ix != -1) {
                        category = loc.substring(0, ix);
                      }
                    } else {
                      ix = loc.indexOf("/", ix + 1);
                      if(ix != -1) {
                        category = loc.substring(0, ix);
                      } else {
                        ix = loc.indexOf("\\", ix + 1);
                        if(ix != -1) {
                          category = loc.substring(0, ix);
                        }
                      }
                    }
                  }
                }
                Set set = (Set)categories.get(category);
                if(set == null) {
                  set = new TreeSet(new BRComparator());
                  categories.put(category, set);
                }
                set.add(br);
              }


              int i = 0;
              DefaultMutableTreeNode selNode = null;

              for(Iterator it = categories.keySet().iterator(); it.hasNext();) {
                String category = (String)it.next();
                Set    set      = (Set)categories.get(category);

                final DefaultMutableTreeNode categoryNode =
                  new CategoryNode(category);

                for(Iterator it2 = set.iterator(); it2.hasNext();) {
                  BundleRecord br = (BundleRecord)it2.next();

                  DefaultMutableTreeNode brNode =  new OBRNode(br);
                  categoryNode.add(brNode);
                  i++;

                  String loc = (String)br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);

                  locationMap.put(loc, brNode);
                  if(brOld != null && loc.equals(brOld.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION))) {
                    selNode = brNode;

                  }

                }

                rootNode.add(categoryNode);
              }

              final TreePath selPath =
                new TreePath(selNode != null
                             ? selNode.getPath()
                             : rootNode.getPath());

              showPath(selPath, treeModel);
            }
          }

        };
      t.start();
    }


    /**
     * Show the specified path and possible model (on the Swing thread)
     *
     * @param selPath path to show
     * @param model   if not <tt>null</tt>, set as tree's model before setting
     *                selected path.
     */
    void showPath(final TreePath selPath, final TreeModel model) {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if(model != null) {
              recordTree.setModel(model);
            }

            recordTree.expandPath(selPath);
            recordTree.setSelectionPath(selPath);
            recordTree.scrollPathToVisible(selPath);
          }
        });
    }




    /**
     * Assert that the URLs seem to be valid ULRs.
     *
     * @throws RuntimeException if any of the strings in <tt>urls</tt>
     *                          fails to resolve to valid URL.
     */
    void assertRepoURLs(String[] urls) {
      if(urls == null) {
        throw new RuntimeException("No URLs set");
      }

      StringBuffer sb = new StringBuffer();
      int nConnectionErrs = 0;

      // for each of the strings, try to create an URL and
      // do an initial connect()
      for(int i = 0; i < urls.length; i++) {
        URLConnection conn = null;
        try {
          URL url = new URL(urls[i]);
          conn = url.openConnection();
          conn.connect();
        } catch (Exception e) {
          sb.append(" " + urls[i] + ": " + e);
          sb.append("\n");
          nConnectionErrs++;
        } finally {
          // close?
        }
      }
      if(nConnectionErrs > 0) {
        String msg =
          "URL connection errors:\n" +
          sb.toString();
        throw new RuntimeException(msg);
      }
    }


    /**
     * Clear the tree and set the text of the tree's root node.
     */
    void setRootText(final String s) {
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
              recordTree.setModel(new DefaultTreeModel(new TopNode(s)));
            }
          });
      } catch (Exception e) {
      }
    }


    /**
     * Query the user to a list of repo URLs
     */
    void askRepoURls() {
      try {
        StringBuffer sb = new StringBuffer();
        String[] urls = getOBR().getRepositoryURLs();
        for(int i = 0; i < urls.length; i++) {
          sb.append(urls[i]);
          if(i < urls.length - 1) {
            sb.append("\n");
          }
        }

        JPanel panel = new JPanel(new BorderLayout());

        JTextArea text = new JTextArea(Math.min(3, urls.length), 40);
        text.setText(sb.toString());
        //        text.setPreferredSize(new Dimension(300, 100));
        JScrollPane scroll =
          new JScrollPane(text,
                          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scroll.setPreferredSize(new Dimension(300, 100));

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(new JLabel("Repository URLs."), BorderLayout.NORTH);
        int option = JOptionPane.showConfirmDialog(this,
                                                   panel,
                                                   "Repository URLs",
                                                   JOptionPane.YES_NO_OPTION);

        String r2 = text.getText();
        if(option == 0 && !r2.equals(sb.toString())) {
          StringTokenizer st = new StringTokenizer(r2, "\n");
          urls = new String[st.countTokens()];
          int i = 0;
          while (st.hasMoreTokens()) {
            urls[i++] = st.nextToken();
          }
          try {
            getOBR().setRepositoryURLs(urls);
            refreshList(true);
          } catch (Exception e) {
            obrErr = "" + e;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }


    /**
     * Set the selected bundle by settingt the HTML detail string.
     */
    void setSelected(TreeNode node) {
      if(node != null && (node instanceof OBRNode)) {
        brSelected = (OBRNode)node;
        Bundle b = getBundle(brSelected.getBundleRecord());
        if(b != null) {
          gotoBid(b.getBundleId());
        } else {
          getBundleSelectionModel().clearSelection();
        }
      } else {
        brSelected = null;
      }

      installButton.setEnabled(brSelected != null && !bBusy);
      startButton.setEnabled(brSelected != null && !bBusy);

      StringBuffer sb = new StringBuffer();



      sb.append("<html>\n");

      sb.append("<table border=\"0\" width=\"100%\">\n");
      sb.append("<tr>");



      if(node != null  && (node instanceof HTMLable)) {
        HTMLable htmlNode = (HTMLable)node;
        sb.append("<td valign=\"top\" bgcolor=\"#eeeeee\">");

        Util.startFont(sb, "-1");

        sb.append(htmlNode.getTitle());

        sb.append("</font>\n");
        sb.append("</td>\n");

        String   iconURL  = htmlNode.getIconURL();
        if(iconURL != null && !"".equals(iconURL.trim())) {
          sb.append("<td valign=\"top\" bgcolor=\"#eeeeee\">");
          sb.append("<img align=\"left\" src=\"" + iconURL + "\">");
          sb.append("</td>");
        }

      } else {
        sb.append("");
      }

      sb.append("</tr>\n");
      sb.append("</table>\n");


      if(node != null  && (node instanceof HTMLable)) {
        HTMLable htmlNode = (HTMLable)node;
        sb.append(htmlNode.toHTML());
      }

      sb.append("</html>");
      setHTML(sb.toString());
      recordTree.invalidate();
      recordTree.repaint();
    }


    public void setBundle(Bundle b) {
    }

    public void stop() {
      if(recordTree != null) {
        ToolTipManager.sharedInstance().registerComponent(recordTree);
      }
    }


    void setHTML(String s) {
      html.setText(s);

      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            try {
              JViewport vp = htmlScroll.getViewport();
              if(vp != null) {
                vp.setViewPosition(new Point(0,0));
                htmlScroll.setViewport(vp);
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
    }
  }

  void gotoBid(long bid) {
    if (!getBundleSelectionModel().isSelected(bid)) {
      getBundleSelectionModel().clearSelection();
      getBundleSelectionModel().setSelected(bid, true);
    }
  }

  /**
   * If possible, get the bundle matchin a bundle record.
   *
   * @param br record to search for
   * @return an installed bundle, if the bbudle record seem to be installed
   */
  Bundle getBundle(BundleRecord br) {

    Bundle[] bl = bc.getBundles();

    for(int i = 0; bl != null && i < bl.length; i++) {
      if(Util.bundleEqual(bl[i], br)) {
        return bl[i];
      }
    }
    return null;
  }


  /**
   * Check if a bundle record seem to be installed.
   */
  boolean isInstalled(BundleRecord br) {
    return getBundle(br) != null;
  }

  void appendHelp(StringBuffer sb) {
    String urlPrefix = "bundle://" + bc.getBundle().getBundleId();

    sb.append("<p>" +
              "Select a bundle from the bundle repository list, then " +
              "select the install or start icons." +
              "</p>");

    /*
      sb.append("<p>" +
      "<img src=\"" + urlPrefix + "/player_play.png\">" +
      "Install and start a bundle and its dependencies." +
      "</p>" +
      "<img src=\"" + urlPrefix + "/player_install.png\">" +
      "Install a bundle and its dependencies." +
      "</p>" +
      "<p>" +
      "<img src=\"" + urlPrefix + "/update.png\">" +
      "Reload the bundle repository list." +
      "</p>" +
      "<p>" +
      "<img src=\"" + urlPrefix + "/sort_select.png\"> " +
      "Change category sorting." +
      "</p>"
      );
    */
  }

  /**
   * Simple interface for things that can produce HTML
   */
  interface HTMLable {
    public String getTitle();
    public String toHTML();
    public String getIconURL();
  }


  /**
   * Comparator class for comparing two BundleRecords
   *
   * <p>
   * Sort first by case-insensite name, tehn by version
   * </p>
   */
  class BRComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      BundleRecord br1 = (BundleRecord)o1;
      BundleRecord br2 = (BundleRecord)o2;
      String s1 = Util.getBRName(br1).toLowerCase();
      String s2 = Util.getBRName(br2).toLowerCase();
      int n = 0;

      try {
        n = s1.compareTo(s2);
        if(n == 0) {
          s1 = (String)br1.getAttribute(BundleRecord.BUNDLE_VERSION);
          s2 = (String)br2.getAttribute(BundleRecord.BUNDLE_VERSION);
        }

        n = s1.compareTo(s2);
      } catch (Exception e) {
      }
      return n;
    }
  }

  /**
   * TreeNode to top of OBR tree.
   */
  class TopNode extends DefaultMutableTreeNode implements HTMLable {

    String name;

    public TopNode(String name) {
      this.name = name;
    }

    public String getIconURL() {
      return null;
    }

    public String toString() {
      return name;
    }

    public String getTitle() {
      return toString();
    }

    public String toHTML() {
      StringBuffer sb = new StringBuffer();

      if(!"".equals(obrErr)) {
        Util.startFont(sb);
        sb.append("<pre>");
        sb.append(obrErr);
        sb.append("</pre>");
        sb.append("</font>");
      } else {
        Util.startFont(sb);

        BundleRepositoryService obr = getOBR();

        if(obr != null) {
          sb.append("<p>");
          sb.append("<b>Repository URLs</b><br>");
          String[] urls = obr.getRepositoryURLs();
          for(int i = 0; i < urls.length; i++) {
            sb.append(urls[i]);
            if(i < urls.length - 1) {
              sb.append("<br>");
            }
          }
          sb.append("</p>");
          sb.append("<p>");
          sb.append("Total number of bundles: " + obr.getBundleRecordCount());
          sb.append("</p>");
        }

        appendHelp(sb);

        sb.append("</font>");
      }
      return sb.toString();
    }


  }

  /**
   * Tree node for grouping BundleRecords into categories in OBR tree
   */
  class CategoryNode extends DefaultMutableTreeNode implements HTMLable {
    String category;

    public CategoryNode(String category) {
      this.category = category;
    }

    public String getIconURL() {
      return null;
    }

    public String toString() {
      return category + " (" + getChildCount() + ")";
    }

    public String getTitle() {
      return toString();
    }


    public String toHTML() {
      StringBuffer sb = new StringBuffer();



      Util.startFont(sb);

      BundleRepositoryService obr = getOBR();
      if(obr != null) {
        sb.append("<p>");
        sb.append("Bundles in this category: " + getChildCount());
        sb.append("</p>");
      }

      appendHelp(sb);

      sb.append("</font>");

      return sb.toString();
    }
  }

  /**
   * Tree Node for wrapping a BundleRecord in the OBR tree.
   */
  class OBRNode extends DefaultMutableTreeNode implements HTMLable {
    String       name;
    StringBuffer log = new StringBuffer();
    BundleRecord br;
    boolean      bInstalled = false;
    boolean      bBusy;

    public OBRNode(BundleRecord br) {
      super(null);
      this.br = br;

      name   = Util.getBRName(br) +" " +Util.getBRVersion(br);
      //      setIcon(bundleIcon);
    }

    public BundleRecord getBundleRecord() {
      return br;
    }

    public void appendLog(String s) {
      log.append(s);
    }

    public String getLog() {
      return log.toString();
    }

    void setInstalled(boolean bInstalled) {
      this.bInstalled = bInstalled;
    }

    public String toString() {
      // This is displayed in the tree to the left.
      return name;
    }

    public String getIconURL() {
      String iconURL = (String)br.getAttribute("Application-Icon");

      if(iconURL != null && !"".equals(iconURL)) {
        StringBuffer sb = new StringBuffer();

        if(iconURL.startsWith("!")) {
          if(!iconURL.startsWith("!/")) {
            iconURL = "!/" + iconURL.substring(1);
          }
          iconURL = "jar:" +
            br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION) +  iconURL;
        } else if(-1 == iconURL.indexOf(":")) {
          iconURL = "jar:" +
            br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION) + "!/" + iconURL;
        }
        return iconURL;
      }

      return null;
    }

    public String toHTML() {
      StringBuffer sb = new StringBuffer();

      String[]     attrs = br.getAttributes();

      Map map = new TreeMap();
      for(int i = 0; i < attrs.length; i++) {
        Object obj = br.getAttribute(attrs[i]);
        map.put(attrs[i], Util.toHTML(obj));
      }


      String desc = (String)br.getAttribute(BundleRecord.BUNDLE_DESCRIPTION);
      if(desc != null) {
        Util.startFont(sb);
        sb.append("<p>");
        sb.append(desc);
        sb.append("</p>");
        sb.append("</font>");

        map.remove(BundleRecord.BUNDLE_DESCRIPTION);
        map.remove(BundleRecord.BUNDLE_DESCRIPTION.toLowerCase());
      }



      sb.append("<table border=0>");


      String log = getLog().trim();
      if(log != null && !"".equals(log)) {
        sb.append("<tr>");
        sb.append("<td bgcolor=\"#eeeeee\" colspan=\"2\" valign=\"top\">");
        sb.append("<pre>");
        Util.startFont(sb, "-2");
        sb.append(log);
        sb.append("</font>");
        sb.append("<pre>");
        sb.append("</td>");
        sb.append("</tr>");
      }

      for(Iterator it = map.keySet().iterator(); it.hasNext();) {
        String key = (String)it.next();
        String val = (String)map.get(key);

        sb.append("<tr>");
        sb.append("<td valign=\"top\"><b>");
        Util.startFont(sb);
        sb.append(key);
        sb.append("</b></font>");
        sb.append("</td>");

        sb.append("<td valign=\"top\">");
        Util.startFont(sb);
        sb.append(val);
        sb.append("</font>");
        sb.append("</td>");

        sb.append("</tr>");
      }

      sb.append("</table>\n");

      return sb.toString();
    }

    public String getTitle() {
      // This is displayed in the HTML to the right.
      if(bInstalled) {
        return name + " (installed)";
      }
      return name;
    }

  }
}
