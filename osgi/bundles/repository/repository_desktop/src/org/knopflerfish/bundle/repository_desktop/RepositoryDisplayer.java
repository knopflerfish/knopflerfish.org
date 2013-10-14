/*
 * Copyright (c) 2004-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repository_desktop;

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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
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
import javax.swing.ScrollPaneConstants;
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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;

/**
 * Desktop plugin for the visualizing Repository services and their offerings.
 * With functionality for install/start and detail information.
 */
public class RepositoryDisplayer
  extends DefaultSwingBundleDisplayer
  implements ServiceTrackerCustomizer<Repository, Repository>
{
  // Repository services tracker
  private final ServiceTracker<Repository, Repository> repoTracker;
  /**
   * Map of tracked repositories.
   */
  Map<String, Repository> repos = new HashMap<String, Repository>();

  private final ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory> xmlRepoFactoryTracker;

  /**
   * Mapping from repository URL to Repository service holding repository URLs
   * configured using this displayer.
   */
  Map<String, ServiceReference<Repository>> userRepos =
    new HashMap<String, ServiceReference<Repository>>();

  static Map<String, NsToHtml> ns2html = new HashMap<String, NsToHtml>();
  static {
    NsToHtml nth = new NsToHtmlBundle();
    ns2html.put(nth.getNs(), nth);
    nth = new NsToHtmlHost();
    ns2html.put(nth.getNs(), nth);
    nth = new NsToHtmlEE();
    ns2html.put(nth.getNs(), nth);
    nth = new NsToHtmlPackage();
    ns2html.put(nth.getNs(), nth);
    nth = new NsToHtmlContent();
    ns2html.put(nth.getNs(), nth);
  }
  static NsToHtml ns2htmlGeneric = new NsToHtmlGeneric();
  public NsToHtml getNsToHtml(String ns)
  {
    final NsToHtml res = ns2html.get(ns);
    return res != null ? res : ns2htmlGeneric;
  }


  // Icons shared by all instances of JRepositoryAdmin
  static ImageIcon startIcon =
    new ImageIcon(
                  RepositoryDisplayer.class
                      .getResource("/bundle-install-start.png"));
  static ImageIcon sortIcon =
    new ImageIcon(RepositoryDisplayer.class.getResource("/view-select.png"));
  static ImageIcon installIcon =
    new ImageIcon(RepositoryDisplayer.class.getResource("/bundle-install.png"));
  // static ImageIcon updateIcon = new
  // ImageIcon(RepositoryDisplayer.class.getResource("/update.png"));
  static ImageIcon bundleIcon =
    new ImageIcon(RepositoryDisplayer.class.getResource("/osgi-bundle.png"));
  static ImageIcon bundleJarIcon =
    new ImageIcon(RepositoryDisplayer.class.getResource("/osgi-bundle-jar.png"));
  static ImageIcon reloadIcon =
    new ImageIcon(RepositoryDisplayer.class.getResource("/view-refresh.png"));
  static ImageIcon settingsIcon =
    new ImageIcon(
                  RepositoryDisplayer.class
                      .getResource("/preferences-system.png"));

  static final int SORT_NONE = 0;
  static final int SORT_HOST = 1;
  static final int SORT_CATEGORY = 2;
  static final int SORT_VENDOR = 3;
  static final int SORT_APIVENDOR = 4;
  static final int SORT_STATUS = 5;

  static int[] SORT_ARRAY = new int[] { SORT_NONE, SORT_HOST, SORT_CATEGORY,
                                       SORT_VENDOR, SORT_STATUS, };

  static String[] SORT_NAMES = new String[] { "All", "Host", "Category",
                                             "Vendor", "Install status", };

  // Message while loading repo URLs
  static String STR_LOADING = "Loading...";

  // Name of top node
  static String STR_TOPNAME = "Repository";

  public RepositoryDisplayer(BundleContext bc)
  {
    super(bc, "Repository", "View and install bundles from OSGi Repositories",
          true);

    repoTracker =
      new ServiceTracker<Repository, Repository>(bc, Repository.class, this);
    repoTracker.open();

    xmlRepoFactoryTracker =
      new ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory>(
                                                                                 bc,
                                                                                 XmlBackedRepositoryFactory.class,
                                                                                 null);
    xmlRepoFactoryTracker.open();
  }

  @Override
  public JComponent newJComponent()
  {
    return new JRepositoryAdmin();
  }

  @Override
  public void disposeJComponent(JComponent comp)
  {
    final JRepositoryAdmin repositoryAdmin = (JRepositoryAdmin) comp;
    repositoryAdmin.stop();

    super.disposeJComponent(comp);
  }

  @Override
  public void close()
  {
    repoTracker.close();
    super.close();
  }

  @Override
  public void valueChanged(final long bid)
  {
    super.valueChanged(bid);
    // If unselecting and multiple selected bundles, choose another one.
    final long selectedBid =
      bundleSelModel.getSelectionCount() > 1
        ? bundleSelModel.getSelected()
        : bid;
    for (final Object element : components) {
      final JRepositoryAdmin repositoryAdmin = (JRepositoryAdmin) (JComponent) element;
      repositoryAdmin.valueChanged(selectedBid);
    }
  }

  @Override
  public Icon getSmallIcon()
  {
    return null;
  }

  /*------------------------------------------------------------------------*
   *			  ServiceTrackerCustomizer implementation
   *------------------------------------------------------------------------*/

  @Override
  public Repository addingService(ServiceReference<Repository> sr)
  {
    final Repository repo = bc.getService(sr);
    repos.put(getKey(sr), repo);
    refreshRepositoryDisplayers();
    return repo;
  }

  @Override
  public void modifiedService(ServiceReference<Repository> sr, Repository repo)
  {
    refreshRepositoryDisplayers();
  }

  @Override
  public void removedService(ServiceReference<Repository> sr, Repository repo)
  {
    repos.remove(getKey(sr));
    refreshRepositoryDisplayers();
  }

  /**
   * We use the persistent service identity as the repository key.
   *
   * @param sr
   *          The repository service reference to get a key for.
   * @return string used as key in the {@link RepositoryDisplayer#repos}-map.
   */
  private static String getKey(ServiceReference<Repository> sr)
  {
    Object o = sr.getProperty(Constants.SERVICE_PID);
    if (o != null) {
      return o.toString();
    }
    o = sr.getProperty(Repository.URL);
    if (o != null) {
      return o.toString();
    }
    return sr.getProperty(Constants.SERVICE_ID).toString();
  }

  /**
   * Inform all {@link JRepositoryAdmin}s that the set of repository services
   * has changed so that they can refresh their contents.
   */
  private void refreshRepositoryDisplayers()
  {
    for (final JComponent component : components) {
      final JRepositoryAdmin repositoryAdmin = (JRepositoryAdmin) component;
      repositoryAdmin.refreshList();
      ;
    }
  }

  /**
   * If possible, get the bundle for a resource.
   *
   * @param resource
   *          the resource to search for
   * @return an installed bundle, if the resource seem to be installed
   */
  static Bundle getBundle(Resource resource)
  {
    final Bundle[] bl = bc.getBundles();

    for (int i = 0; bl != null && i < bl.length; i++) {
      if (Util.bundleEqual(bl[i], resource)) {
        return bl[i];
      }
    }
    return null;
  }

  /**
   * Compute the category string for the given resource and given sort type.
   *
   * @param resource
   *          The resource to compute a category for.
   * @param sortCategory
   *          The type of category to derive.
   * @return
   */
  private String deriveCatagory(final Resource resource, final int sortCategory)
  {
    String category = "other";

    if (sortCategory == SORT_CATEGORY) {
      category = Util.getResourceCategory(resource);
    } else if (sortCategory == SORT_VENDOR) {
      category = Util.getResourceVendor(resource);
    } else if (sortCategory == SORT_STATUS) {
      if (getBundle(resource) != null) {
        category = "Installed";
      } else {
        category = "Not installed";
      }
    } else if (sortCategory == SORT_NONE) {
      category = SORT_NAMES[SORT_NONE];
    } else {
      // Bundle location
      final String loc = Util.getLocation(resource);

      int ix = loc.indexOf(":");
      if (ix != -1) {
        category = loc.substring(0, ix);
        if (loc.startsWith("http://")) {
          ix = loc.indexOf("/", 8);
          if (ix != -1) {
            category = loc.substring(0, ix);
          }
        } else {
          ix = loc.indexOf("/", ix + 1);
          if (ix != -1) {
            category = loc.substring(0, ix);
          } else {
            ix = loc.indexOf("\\", ix + 1);
            if (ix != -1) {
              category = loc.substring(0, ix);
            }
          }
        }
      }
    }
    return category;
  }

  /**
   * The actual repository displayer component returned by newJComponent.
   */
  class JRepositoryAdmin
    extends JPanel
  {
    private static final long serialVersionUID = 1L;

    DefaultTreeModel treeModel;
    JTree resourceTree;
    JPanel resourcePanel;
    JButton installButton;
    JButton refreshButton;
    JButton startButton;
    JButton updateButton;
    JTextPane html;
    JScrollPane htmlScroll;

    TopNode rootNode;

    // currently selected node
    RepositoryNode selectedRepositoryNode = null;

    JMenuItem contextItem;
    JPopupMenu contextPopupMenu;

    // Category used for grouping bundle records
    int sortCategory = SORT_CATEGORY;

    // Map holding all bundles in repository tree
    // location -> RepositoryNode
    // For resources without any content URL we use the
    // value of the mandatory attribute osgi.content in the osgi.content
    // name-space.
    Map<String, RepositoryNode> locationMap =
      new HashMap<String, RepositoryNode>();

    public JRepositoryAdmin()
    {
      setLayout(new BorderLayout());

      resourceTree = new JTree(new TopNode("[not loaded]", this));
      resourceTree.setRootVisible(true);

      // Must be registered for renderer tool tips to work
      ToolTipManager.sharedInstance().registerComponent(resourceTree);

      // Load leaf icon for the tree cell renderer.
      final TreeCellRenderer renderer = new DefaultTreeCellRenderer() {
        private static final long serialVersionUID = 2L;

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus)
        {
          super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                                             row, hasFocus);

          final TreePath tp = tree.getPathForRow(row);

          try {
            final Object node = tp.getLastPathComponent();
            String tt = null;
            if (node instanceof RepositoryNode) {
              final RepositoryNode repoNode = (RepositoryNode) node;

              final URL url = Util.getResourceUrl(repoNode.resource);
              tt =
                repoNode.bBusy ? "busy..." : (url == null ? "?" : url
                    .toString());

              final Bundle bundle = repoNode.getBundle();
              if (repoNode.bBusy) {
                setIcon(reloadIcon);
              } else if (bundle != null) {
                setIcon(getBundleIcon(repoNode.getIconURL()));
                setForeground(Color.gray);
              } else {
                setIcon(bundleJarIcon);
              }
            } else if (node instanceof TopNode) {
              final TopNode topNode = (TopNode) node;
              if (STR_LOADING.equals(topNode.name)) {
                setIcon(reloadIcon);
              }
            } else {
              // setIcon(null);
            }
            setToolTipText(tt);
          } catch (final Exception ignored) {
          }
          return this;
        }

        // TODO: Use resource specific icons when available.
//        Map<String,Icon> bundleIconCache = new HashMap<String, Icon>();
        private Icon getBundleIcon(String iconURL)
        {
//          if (iconURL == null) {
            return bundleIcon;
//          }
//          Icon res = bundleIconCache.get(iconURL);
//          if (res == null) {
//            try {
//            res = new ImageIcon(iconURL);
//            } catch (final Exception e) {
//              // Fallback to the standard icon.
//              res = bundleIcon;
//              Activator.log.info("Failed to load icon with URL: " +iconURL, e);
//            }
//            bundleIconCache.put(iconURL, res);
//          }
//          return res;
        }
      };

      resourceTree.setCellRenderer(renderer);

      // call setSelected() when user selects nodes/leafs in the tree
      resourceTree.addTreeSelectionListener(new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent e)
        {
          final TreePath[] sel = resourceTree.getSelectionPaths();
          if (sel != null && sel.length == 1) {
            setSelected((TreeNode) sel[0].getLastPathComponent());
          } else {
            setSelected(null);
          }
        }
      });

      // Create the HTML text pane for detail view.
      // The node's getTitle()/toHTML() methods
      // will be called whenever a node is HTML-able
      html = new JTextPane();
      html.setText("");
      html.setContentType("text/html");

      html.setEditable(false);

      html.addHyperlinkListener(new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent ev)
        {
          if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            final URL url = ev.getURL();
            Activator.log.info("Link : " + url + " activated");
            try {
              Util.openExternalURL(url);
            } catch (final Exception e) {
              Activator.log.warn("Failed to open external url=" + url
                                 + " reason: " + e);
            }
          }
        }
      });

      htmlScroll =
        new JScrollPane(html, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      htmlScroll.setPreferredSize(new Dimension(300, 300));

      final JScrollPane treeScroll =
        new JScrollPane(resourceTree,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      treeScroll.setPreferredSize(new Dimension(200, 300));

      final JButton repoButton = new JButton(settingsIcon);
      repoButton.setToolTipText("Configure repository URLs");
      repoButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
          askRepoURls();
        }
      });

      installButton = new JButton(installIcon);
      installButton.setToolTipText("Install from repository");
      ActionListener installAction;
      installButton.addActionListener(installAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
          installOrStart(selectedRepositoryNode, false);
        }
      });
      installButton.setEnabled(false);

      startButton = new JButton(startIcon);
      startButton.setToolTipText("Install and start from repository");
      ActionListener startAction;
      startButton.addActionListener(startAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
          installOrStart(selectedRepositoryNode, true);
        }
      });
      startButton.setEnabled(false);

      refreshButton = new JButton(reloadIcon);
      refreshButton.setToolTipText("Refresh repository list");
      refreshButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ev)
        {
          refreshList();
        }
      });

      resourcePanel = new JPanel(new BorderLayout());
      resourcePanel.add(htmlScroll, BorderLayout.CENTER);

      final JPanel left = new JPanel(new BorderLayout());

      left.add(treeScroll, BorderLayout.CENTER);

      final JToolBar leftTools = new JToolBar();
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

      item = new JMenuItem(startButton.getToolTipText(), startButton.getIcon());
      item.addActionListener(startAction);
      contextPopupMenu.add(item);

      item =
        new JMenuItem(installButton.getToolTipText(), installButton.getIcon());
      item.addActionListener(installAction);
      contextPopupMenu.add(item);

      // add listener for tree context menu, which selects the
      // item below the mouse and pops up the context menu.
      resourceTree.addMouseListener(new MouseAdapter() {
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
          if (contextPopupMenu != null
              && (e.isPopupTrigger() || ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0))) {
            final TreePath tp =
              resourceTree.getPathForLocation(e.getX(), e.getY());
            if (tp != null) {
              final TreeNode node = (TreeNode) tp.getLastPathComponent();
              if (node instanceof RepositoryNode) {
                contextItem.setText(((RepositoryNode) node).name);
                resourceTree.setSelectionPath(tp);
                setSelected(node);
                final Component comp = e.getComponent();
                contextPopupMenu.show(comp, e.getX(), e.getY());
              }
            }
          }
        }
      });

      final JSplitPane panel =
        new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, resourcePanel);

      panel.setDividerLocation(200);

      add(panel, BorderLayout.CENTER);

      refreshList();
    }

    /**
     * Called when BundleSelectionListener distributes events.
     *
     * <p>
     * If possible, select and show the bundle with id <tt>bid</tt> in the tree.
     * </p>
     */
    void valueChanged(long bid)
    {
      try {
        if (bid >= 0) {
          final Bundle b = bc.getBundle(bid);
          if (b != null) {
            final RepositoryNode repositoryNode = getRepositoryNode(b);
            if (repositoryNode != null) {
              if (repositoryNode != selectedRepositoryNode) {
                final TreePath tp = new TreePath(repositoryNode.getPath());
                showPath(tp, null);
              }
            } else {
              setSelected(null);
            }
          }
        }
      } catch (final Exception e) {
      }
    }

    /**
     * Get the repository node which matches a bundle.
     *
     * @return repository node for the specified bundle if it exists,
     *         <tt>null</tt> otherwise.
     */
    RepositoryNode getRepositoryNode(Bundle b)
    {
      final RepositoryNode node = locationMap.get(b.getLocation());

      if (node != null) {
        return node;
      }

      for (final RepositoryNode rn : locationMap.values()) {
        if (Util.bundleEqual(b, rn.getResource())) {
          return rn;
        }
      }
      return null;
    }

    /**
     * Return the resource that the given bundle originates from is any.
     */
    Resource getResource(Bundle b)
    {
      for (final RepositoryNode rn : locationMap.values()) {
        final Resource resource = rn.getResource();
        final String rLoc = Util.getLocation(resource);
        if (rLoc.equals(b.getLocation())) {
          return resource;
        }
      }
      return null;
    }

    /**
     * Create the sort selection button, with items defined in
     * SORT_ARRAY/SORT_NAMES
     */
    JButton makeSortSelectionButton()
    {
      final JButton sortButton = new JButton(sortIcon);
      sortButton.setToolTipText("Select sorting");

      final JPopupMenu sortPopupMenu = new JPopupMenu();
      final ButtonGroup group = new ButtonGroup();

      for (int i = 0; i < SORT_ARRAY.length; i++) {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(SORT_NAMES[i]);
        final int cat = SORT_ARRAY[i];
        item.setState(cat == sortCategory);
        item.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent ev)
          {
            sortCategory = cat;
            refreshList();
          }
        });
        sortPopupMenu.add(item);
        group.add(item);
      }

      sortButton.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e)
        {
          showPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
          showPopup(e);
        }

        private void showPopup(MouseEvent e)
        {
          final Component comp = e.getComponent();
          sortPopupMenu.show(comp, 0, comp.getSize().height);
        }
      });

      return sortButton;
    }

    /**
     * Flag indicating is an install operation is in progress or not.
     */
    boolean bBusy = false;

    private String repositoryErr;

    /**
     * Install or install+start a bundle specified by a {@link RepositoryNode}.
     * Run an a new thread to avoid blocking the swing thread for potentially
     * long operations.
     *
     * @param node
     *          Bundle to install/start
     * @param bStart
     *          if {@code true}, start, otherwise just install
     */
    void installOrStart(final RepositoryNode node, final boolean bStart)
    {
      if (bBusy) {
        return;
      }
      new Thread() {
        {
          start();
        }

        @Override
        public void run()
        {
          try {
            bBusy = node.bBusy = true;
            setSelected(node);
            installOrStartSync(node, bStart);
          } catch (final Exception e) {
            node.appendLog("" + e + "\n");
          } finally {
            bBusy = node.bBusy = false;
            setSelected(node);
          }
        }
      };
    }

    void installOrStartSync(RepositoryNode node, boolean bStart)
    {
      final Resource resource = node.getResource();
      if (resource == null) {
        return;
      }

      // Check if bundle already is installed. If so,
      // ask user if it should be updated, installed again or
      // if the operation should be cancelled.
      final Bundle b = getBundle(resource);
      if (b != null) {

        final boolean bIsRepoBundle = getResource(b) != null;

        final String[] options =
          bIsRepoBundle
            ? (new String[] { "Update", "Cancel", })
            : (new String[] { "Update", "Install again", "Cancel", });

        final String msg =
          bIsRepoBundle
            ? "The selected bundle is already installed\n"
              + "from the repository.\n" + "\n"
              + "It can be updated from the repository."
            : "The selected bundle is already installed.\n" + "\n"
              + "It can be updated from the repository, or\n"
              + "a new instance can be installed from the\n" + "repository";

        final int n =
          JOptionPane.showOptionDialog(resourceTree, msg,
                                       "Bundle is installed", // title
                                       JOptionPane.YES_NO_CANCEL_OPTION,
                                       JOptionPane.QUESTION_MESSAGE, null, // icon
                                       options, options[0]);

        if (bIsRepoBundle) {
          if (n == 0) { // update
            node.appendLog("Update.\n");
          } else if (n == 1) { // Cancel
            return;
          }
        } else {
          if (n == 0) { // Update
            InputStream in = null;
            try {
              in = ((RepositoryContent) node.getResource()).getContent();
              b.update(in);
              node.appendLog("Updated from "
                             + Util.getLocation(node.getResource()) + "\n");
            } catch (final Exception e) {
              node.appendLog("Update failed: " + e + "\n");
            } finally {
              try {
                in.close();
              } catch (final Exception ignored) {
              }
            }
            return;
          } else if (n == 1) { // install new
            node.appendLog("Install new instance.\n");
          } else if (n == 2) { // cancel
            return;
          }
        }
      }

      // Install the bundle from the repository resource

      // TODO: Use resolver service to find and install other bundles that are
      // needed to resolve and start the selected bundle.
      InputStream in = null;
      String message = "";
      try {
        final String location = Util.getLocation(resource);
        in = ((RepositoryContent) node.getResource()).getContent();
        final Bundle bundle = bc.installBundle(location, in);
        node.setInstalled(b);
        message += "Installed #" + bundle.getBundleId() + " from " + location;

        if (bStart) {
          try {
            b.start();
          } catch (final Exception es) {
            message += ". Start failed: " + es.getMessage();
            Activator.log.error(message, es);
          }
        }
        final boolean bOK = true; // resolve and install went ok.
        if (bOK) {
          if (sortCategory == SORT_STATUS) {
            refreshList();
          }
        }
      } catch (final Exception ei) {
        message += "Installation failed: " + ei.getMessage();
        Activator.log.error(message, ei);
      } finally {
        node.appendLog(message);
      }

      setSelected(node);
    }

    /**
     * Refresh the bundle repository tree and try to show/reselect any
     * previously selected bundle node.
     */
    synchronized void refreshList()
    {
      final Thread t = new Thread() {
        @Override
        public void run()
        {
          try {
            // Make sure that the service tracker customizer are done before
            // doing the update.
            Thread.sleep(2L);
          } catch (final InterruptedException ie) {
            return;
          }
          locationMap.clear();
          final Resource oldSelection =
            selectedRepositoryNode != null ? selectedRepositoryNode
                .getResource() : null;

          setRootText(STR_LOADING);

          rootNode = new TopNode(STR_TOPNAME, JRepositoryAdmin.this);
          treeModel = new DefaultTreeModel(rootNode);

          // String (category) -> Set (Resource)
          final Map<String, Set<Resource>> categories =
            new TreeMap<String, Set<Resource>>(new Comparator<String>() {
              @Override
              public int compare(String s1, String s2)
              {
                return s1.compareToIgnoreCase(s2);
              }
            });

          // move all resource into a sorted
          // category map of sets
          // First find all bundle resources in all repos.
          final Requirement bundleReq = new DownloadableBundleRequirement();
          final Set<Resource> bundleResources = new HashSet<Resource>();
          for (final Repository repo : repos.values()) {
            final Map<Requirement, Collection<Capability>> answer =
              repo.findProviders(Collections.singleton(bundleReq));
            for (final Entry<Requirement, Collection<Capability>> entry : answer
                .entrySet()) {
              for (final Capability capability : entry.getValue()) {
                bundleResources.add(capability.getResource());
              }
            }
          }
          // Categorize each resource
          for (final Resource resource : bundleResources) {
            final String category = deriveCatagory(resource, sortCategory);

            Set<Resource> set = categories.get(category);
            if (set == null) {
              set = new TreeSet<Resource>(new ResourceComparator());
              categories.put(category, set);
            }
            set.add(resource);
          }

          DefaultMutableTreeNode selNode = null;
          for (final Entry<String, Set<Resource>> entry : categories.entrySet()) {
            final String category = entry.getKey();
            final DefaultMutableTreeNode categoryNode =
              new CategoryNode(category);

            for (final Resource resource : entry.getValue()) {
              final RepositoryNode resourceNode = new RepositoryNode(resource);
              categoryNode.add(resourceNode);
              final String loc = Util.getLocation(resource);
              locationMap.put(loc, resourceNode);
              if (oldSelection != null
                  && loc.equals(Util.getLocation(oldSelection))) {
                selNode = resourceNode;
              } else {
                // If this resource has a selected bundle, then make it
                // selected. Needed to set up the selection when this displayer
                // is (re)started.
                final Bundle b = resourceNode.getBundle();
                final BundleSelectionModel bsm = getBundleSelectionModel();
                if (b != null && bsm.isSelected(b.getBundleId())) {
                  selNode = resourceNode;
                }
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
      };
      t.start();
    }

    /**
     * Show the specified path and possible model (on the Swing thread)
     *
     * @param selPath
     *          path to show
     * @param model
     *          if not <tt>null</tt>, set as tree's model before setting
     *          selected path.
     */
    void showPath(final TreePath selPath, final TreeModel model)
    {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run()
        {
          if (model != null) {
            resourceTree.setModel(model);
          }

          resourceTree.expandPath(selPath);
          resourceTree.setSelectionPath(selPath);
          resourceTree.scrollPathToVisible(selPath);
        }
      });
    }

    /**
     * Assert that the URLs seem to be valid ULRs.
     *
     * @throws RuntimeException
     *           if any of the strings in <tt>urls</tt> fails to resolve to
     *           valid URL.
     */
    void assertRepoURLs(String[] urls)
    {
      if (urls == null) {
        throw new RuntimeException("No URLs set");
      }

      final StringBuffer sb = new StringBuffer();
      int nConnectionErrs = 0;

      // for each of the strings, try to create an URL and
      // do an initial connect()
      for (final String url2 : urls) {
        URLConnection conn = null;
        try {
          final URL url = new URL(url2);
          conn = url.openConnection();
          conn.connect();
        } catch (final Exception e) {
          sb.append(" " + url2 + ": " + e);
          sb.append("\n");
          nConnectionErrs++;
        } finally {
          // close?
        }
      }
      if (nConnectionErrs > 0) {
        final String msg = "URL connection errors:\n" + sb.toString();
        throw new RuntimeException(msg);
      }
    }

    /**
     * Clear the tree and set the text of the tree's root node.
     */
    void setRootText(final String s)
    {
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
          @Override
          public void run()
          {
            resourceTree.setModel(new DefaultTreeModel(new TopNode(s, JRepositoryAdmin.this)));
          }
        });
      } catch (final Exception e) {
      }
    }

    /**
     * Query the user for a list of repo URLs
     */
    void askRepoURls()
    {
      final StringBuffer sb = new StringBuffer();

      try {
        for (final String url : userRepos.keySet()) {
          sb.append(url);
          sb.append("\n");
        }

        final JPanel panel = new JPanel(new BorderLayout());

        final JTextArea text = new JTextArea(Math.min(3, userRepos.size()), 40);
        text.setText(sb.toString());
        // text.setPreferredSize(new Dimension(300, 100));
        final JScrollPane scroll =
          new JScrollPane(text,
                          ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scroll.setPreferredSize(new Dimension(300, 100));

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(new JLabel("Repository URLs."), BorderLayout.NORTH);
        final int option =
          JOptionPane.showConfirmDialog(this, panel, "Repository URLs",
                                        JOptionPane.OK_CANCEL_OPTION);

        final String r2 = text.getText();
        if (option == JOptionPane.OK_OPTION && !r2.equals(sb.toString())) {
          final StringTokenizer st = new StringTokenizer(r2, "\n");
          final List<String> urls = new ArrayList<String>();
          while (st.hasMoreTokens()) {
            urls.add(st.nextToken().trim());
          }
          try {
            for (final String url : urls) {
              final XmlBackedRepositoryFactory repoFactory =
                xmlRepoFactoryTracker.getService();
              final ServiceReference<Repository> sr =
                repoFactory.create(url, null, url);
              userRepos.put(url, sr);
            }
            refreshList();
          } catch (final Exception e) {
            repositoryErr = "" + e;
          }
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * Set the selected bundle by setting the HTML detail string.
     */
    void setSelected(TreeNode node)
    {
      if (node != null && (node instanceof RepositoryNode)) {
        selectedRepositoryNode = (RepositoryNode) node;
        final Bundle b = selectedRepositoryNode.getBundle();
        if (b != null) {
          gotoBid(b.getBundleId());
        } else {
          getBundleSelectionModel().clearSelection();
        }
      } else {
        selectedRepositoryNode = null;
      }

      installButton.setEnabled(selectedRepositoryNode != null && !bBusy);
      startButton.setEnabled(selectedRepositoryNode != null && !bBusy);

      final StringBuffer sb = new StringBuffer();

      sb.append("<html>\n");

      sb.append("<table border=\"0\" width=\"100%\">\n");
      sb.append("<tr>");

      if (node != null && (node instanceof HTMLAble)) {
        final HTMLAble htmlNode = (HTMLAble) node;
        sb.append("<td valign=\"top\" bgcolor=\"#eeeeee\">");

        Util.startFont(sb, "-1");

        sb.append(htmlNode.getTitle());

        sb.append("</font>\n");
        sb.append("</td>\n");

        // jar:-URLs are not handled by the HTML-document...
//        final String iconURL = htmlNode.getIconURL();
//        if (iconURL != null && !"".equals(iconURL.trim())) {
//          System.out.println("iconURL: " +iconURL);
//          sb.append("<td valign=\"top\" bgcolor=\"#eeeeee\">");
//          sb.append("<img align=\"left\" src=\"" + iconURL + "\">");
//          sb.append("</td>");
//        }

      } else {
        sb.append("");
      }

      sb.append("</tr>\n");
      sb.append("</table>\n");

      if (node != null && (node instanceof HTMLAble)) {
        final HTMLAble htmlNode = (HTMLAble) node;
        sb.append(htmlNode.toHTML());
      }

      sb.append("</html>");
      setHTML(sb.toString());
      resourceTree.invalidate();
      resourceTree.repaint();
    }

    public void setBundle(Bundle b)
    {
    }

    public void stop()
    {
      if (resourceTree != null) {
        ToolTipManager.sharedInstance().registerComponent(resourceTree);
      }
    }

    void setHTML(String s)
    {
      html.setText(s);

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run()
        {
          try {
            final JViewport vp = htmlScroll.getViewport();
            if (vp != null) {
              vp.setViewPosition(new Point(0, 0));
              htmlScroll.setViewport(vp);
            }
            html.repaint();
          } catch (final Exception e) {
            e.printStackTrace();
          }
        }
      });
    }
  }

  void gotoBid(long bid)
  {
    if (!getBundleSelectionModel().isSelected(bid)) {
      getBundleSelectionModel().clearSelection();
      getBundleSelectionModel().setSelected(bid, true);
    }
  }

  void appendHelp(StringBuffer sb)
  {
    // final String urlPrefix = "bundle://" + bc.getBundle().getBundleId();

    sb.append("<p>" + "Select a bundle from the bundle repository list, then "
              + "select the install or start icons." + "</p>");

    /*
     * sb.append("<p>" + "<img src=\"" + urlPrefix + "/player_play.png\">" +
     * "Install and start a bundle and its dependencies." + "</p>" +
     * "<img src=\"" + urlPrefix + "/player_install.png\">" +
     * "Install a bundle and its dependencies." + "</p>" + "<p>" + "<img src=\""
     * + urlPrefix + "/update.png\">" + "Reload the bundle repository list." +
     * "</p>" + "<p>" + "<img src=\"" + urlPrefix + "/sort_select.png\"> " +
     * "Change category sorting." + "</p>" );
     */
  }

  /**
   * Simple interface for things that can produce HTML
   */
  interface HTMLAble
  {
    public String getTitle();

    public String toHTML();

    public String getIconURL();
  }

  /**
   * Comparator class for comparing two bundle resources
   *
   * <p>
   * Sort first by case-insensitive name, then by version
   * </p>
   */
  class ResourceComparator
    implements Comparator<Resource>
  {
    @Override
    public int compare(Resource r1, Resource r2)
    {
      final String s1 = Util.getResourceName(r1).toLowerCase();
      final String s2 = Util.getResourceName(r2).toLowerCase();
      int n = 0;

      try {
        n = s1.compareTo(s2);
        if (n == 0) {
          final Version v1 = Util.getResourceVersion(r1);
          final Version v2 = Util.getResourceVersion(r2);
          n = v1.compareTo(v2);
        }
      } catch (final Exception e) {
      }
      return n;
    }
  }

  /**
   * TreeNode to top of repository tree.
   */
  class TopNode
    extends DefaultMutableTreeNode
    implements HTMLAble
  {
    private static final long serialVersionUID = 4L;
    JRepositoryAdmin repositoryAdmin;
    String name;

    public TopNode(String name, JRepositoryAdmin repositoryAdmin)
    {
      this.name = name;
      this.repositoryAdmin = repositoryAdmin;
    }

    @Override
    public String getIconURL()
    {
      return null;
    }

    @Override
    public String toString()
    {
      return name;
    }

    @Override
    public String getTitle()
    {
      return toString();
    }

    @Override
    public String toHTML()
    {
      final StringBuffer sb = new StringBuffer();

      if (repositoryAdmin.repositoryErr != null
          && repositoryAdmin.repositoryErr.length() > 0) {
        Util.startFont(sb);
        sb.append("<pre>");
        sb.append(repositoryAdmin.repositoryErr);
        sb.append("</pre>");
        sb.append("</font>");
      } else {
        Util.startFont(sb);

        final ServiceReference<Repository>[] repoSrs =
          repoTracker.getServiceReferences();
        if (repoSrs != null && repoSrs.length > 0) {
          sb.append("<p><b>Repositories</b></p><ul>");

          for (final ServiceReference<Repository> sr : repoSrs) {
            sb.append("<li><p>");
            toHTML(sb, sr, Constants.SERVICE_DESCRIPTION, "");
            toHTML(sb, sr, Constants.SERVICE_PID, "PID: ");
            toHTML(sb, sr, Repository.URL, "URLs: ");
            sb.append("</p>");
          }
          sb.append("</ul>");

          sb.append("<p>");
          final Set<Resource> bundleResources = new HashSet<Resource>();
          final Requirement bundleReq = new DownloadableBundleRequirement();
          for (final Repository repo : repos.values()) {
            final Map<Requirement, Collection<Capability>> answer =
              repo.findProviders(Collections.singleton(bundleReq));
            for (final Entry<Requirement, Collection<Capability>> entry : answer
                .entrySet()) {
              for (final Capability capability : entry.getValue()) {
                bundleResources.add(capability.getResource());
              }
            }
          }
          sb.append("Total number of bundles: " + bundleResources.size());
          sb.append("</p>");

          appendHelp(sb);
        } else {
          sb.append("<p><em>No</em> repositories available.</p>");
          sb.append("<p>Press the settings button (rightmost button under "
                    + "the repository tree) to add repositories.</p>");
        }

        sb.append("</font>");
      }
      Activator.log.info("TopNode.toHTML: " +sb);
      return sb.toString();
    }

    private void toHTML(final StringBuffer sb,
                        final ServiceReference<Repository> sr,
                        final String key,
                        final String label)
    {
      final String val = (String) sr.getProperty(key);
      if (val != null && val.length() > 0) {
        sb.append(label);
        sb.append(val);
        sb.append("<br>");
      }
    }

  }

  /**
   * Tree node for grouping BundleRecords into categories in bundle resource
   * tree
   */
  class CategoryNode
    extends DefaultMutableTreeNode
    implements HTMLAble
  {
    private static final long serialVersionUID = 5L;
    String category;

    public CategoryNode(String category)
    {
      this.category = category;
    }

    @Override
    public String getIconURL()
    {
      return null;
    }

    @Override
    public String toString()
    {
      return category + " (" + getChildCount() + ")";
    }

    @Override
    public String getTitle()
    {
      return toString();
    }

    @Override
    public String toHTML()
    {
      final StringBuffer sb = new StringBuffer();

      Util.startFont(sb);

      sb.append("<p>");
      sb.append("Bundles in this category: " + getChildCount());
      sb.append("</p>");

      appendHelp(sb);

      sb.append("</font>");

      return sb.toString();
    }
  }

  /**
   * Tree Node for wrapping a repository resource in the repository tree.
   */
  class RepositoryNode
    extends DefaultMutableTreeNode
    implements HTMLAble
  {
    private static final long serialVersionUID = 3L;

    String name;
    StringBuffer log = new StringBuffer();
    // The bundle that corresponds to the resource that this node represents.
    Bundle bundle = null;
    boolean bBusy;
    Resource resource;
    // Sorted cache of capabilities provided by the given resource
    SortedMap<String, Set<Capability>> ns2caps =
      new TreeMap<String, Set<Capability>>();
    final Set<Capability> idCaps;
    final Set<Capability> kfExtraCaps;

    // Sorted cache of requirements required by the given resource
    SortedMap<String, Set<Requirement>> ns2reqs =
      new TreeMap<String, Set<Requirement>>();

    public RepositoryNode(Resource resource)
    {
      super(null);
      this.resource = resource;

      name =
        Util.getResourceName(resource) + " "
            + Util.getResourceVersion(resource) + " "
            + Util.getResourceType(resource);

      for (final Capability capability : resource.getCapabilities(null)) {
        final String ns = capability.getNamespace();
        Set<Capability> caps = ns2caps.get(ns);
        if (caps == null) {
          caps = new HashSet<Capability>();
          ns2caps.put(ns, caps);
        }
        caps.add(capability);
      }
      idCaps = ns2caps.remove(IdentityNamespace.IDENTITY_NAMESPACE);
      kfExtraCaps = ns2caps.remove(Util.KF_EXTRAS_NAMESPACE);

      for (final Requirement requirement : resource.getRequirements(null)) {
        final String ns = requirement.getNamespace();
        Set<Requirement> reqs = ns2reqs.get(ns);
        if (reqs == null) {
          reqs = new HashSet<Requirement>();
          ns2reqs.put(ns, reqs);
        }
        reqs.add(requirement);
      }

      // setIcon(bundleIcon);
    }

    public Bundle getBundle()
    {
      if (bundle==null) {
        // Check to see if there is a bundle now.
        bundle = RepositoryDisplayer.getBundle(resource);
      }
      return bundle;
    }

    public Resource getResource()
    {
      return resource;
    }

    public void appendLog(String s)
    {
      log.append(s);
    }

    public String getLog()
    {
      return log.toString();
    }

    void setInstalled(Bundle bundle)
    {
      this.bundle = bundle;
    }

    @Override
    public String toString()
    {
      // This is displayed in the tree to the left.
      return name;
    }

    @Override
    public String getIconURL()
    {
      String iconURL = Util.getResourceIcon(resource);
      if (iconURL != null) {
        // Simply take the first icon defined, ignoring size and other attributes.
        // TODO: Re-use the desktop's BundleImageIcon class.

        // Keep the first entry only.
        final int commaPos = iconURL.indexOf(',');
        if (commaPos > -1) {
          iconURL = iconURL.substring(0, commaPos);
        }
        // Remove all parameters.
        final int semiPos = iconURL.indexOf(';');
        if (semiPos > -1) {
          iconURL = iconURL.substring(0, semiPos);
        }

        if (iconURL.startsWith("!")) {
          if (!iconURL.startsWith("!/")) {
            iconURL = "!/" + iconURL.substring(1);
          }
          iconURL = "jar:" + Util.getLocation(resource) + iconURL;
        } else if (-1 == iconURL.indexOf(":")) {
          iconURL = "jar:" + Util.getLocation(resource) + "!/" + iconURL;
        }
      }
      return iconURL;
    }

    @Override
    public String toHTML()
    {
      final StringBuffer sb = new StringBuffer();

      sb.append("<table border='0' width='100%'>\n");

      // osgi.identity name space is handled specially
      if (idCaps.size() == 0) {
        Util.toHTMLtrError_2(sb, "No osgi.identity capabilities in this node!");
      } else {
        Util.toHTMLtrHeading_2(sb, getIdAttribute(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE));
        for (final Capability idCap : idCaps) {
          // Make a copy of the map so that we can remove processed entries.
          final SortedMap<String, Object> attrs =
            new TreeMap<String, Object>(idCap.getAttributes());

          // These attributes are presented in the title.
          attrs.remove(IdentityNamespace.IDENTITY_NAMESPACE);
          attrs.remove(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
          attrs.remove(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
          // This attributes is presented above.
          attrs.remove(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE);

          Util.toHTMLtr_2(sb,
                   "Copyright",
                   getIdAttribute(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE));
          attrs.remove(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE);

          Util.toHTMLtr_2(sb,
                   "Documentation",
                   getIdAttribute(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE));
          attrs.remove(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE);

          Util.toHTMLtr_2(sb,
                   "License",
                   getIdAttribute(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE));
          attrs.remove(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE);

          // Any other attribute
          for (final Entry<String,Object> entry : attrs.entrySet()) {
            Util.toHTMLtr_2(sb, entry.getKey(), entry.getValue());
          }
        }
      }

      Util.toHTMLtrLog_2(sb, getLog().trim());

      sb.append("</table>\n");
      sb.append("<table border='0' width='100%'>\n");

      // Capabilities
      Util.toHTMLtrHeading1_1234_4(sb, "Capabilites");
      for (final Entry<String, Set<Capability>> entry : ns2caps.entrySet()) {
        final String ns = entry.getKey();
        final NsToHtml nsToHtml = getNsToHtml(ns);
        Util.toHTMLtrHeading2_1234_4(sb, ns);

        final Set<Capability> caps = entry.getValue();
        for (final Capability cap : caps) {
          Util.toHTMLtr234_4(sb, nsToHtml.toHTML(cap));
        }
      }

      // Requirements
      Util.toHTMLtrHeading1_1234_4(sb, "Requirements");
      for (final Entry<String, Set<Requirement>> entry : ns2reqs.entrySet()) {
        final String ns = entry.getKey();
        final NsToHtml nsToHtml = getNsToHtml(ns);
        Util.toHTMLtrHeading2_1234_4(sb, ns);

        final Set<Requirement> reqs = entry.getValue();
        for (final Requirement req : reqs) {
          Util.toHTMLtr234_4(sb, nsToHtml.toHTML(req));
        }
      }

      sb.append("</table>\n");

      return sb.toString();
    }

    /**
     * BND generated XML repository files never contains a value for the
     * optional attributes in the identity name space, also look for it in the
     * KF-name space.
     *
     * @param key
     *          Attribute key to look up.
     *
     * @return the attribute value of the resource that this node represents.
     */
    private String getIdAttribute(final String key)
    {
      for (final Capability idCap : idCaps) {
        final Map<String, Object> attrs = idCap.getAttributes();

        final String description = (String) attrs.remove(key);
        if (description != null && description.length() > 0) {
          return description;
        }
      }
      if (kfExtraCaps != null && kfExtraCaps.size() > 0) {
        return (String) kfExtraCaps.iterator().next().getAttributes().get(key);
      }
      return null;
    }

    @Override
    public String getTitle()
    {
      // This is displayed in the HTML to the right.
      if (bundle != null) {
        return "#" + bundle.getBundleId() +" " + name;
      }
      return name;
    }

  }

}
