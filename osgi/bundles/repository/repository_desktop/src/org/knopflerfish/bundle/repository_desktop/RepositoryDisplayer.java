/*
 * Copyright (c) 2004-2022, KNOPFLERFISH project
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
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.TableColumnModel;
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
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.knopflerfish.service.repositorymanager.RepositoryManager;
import org.knopflerfish.service.repositorymanager.RepositoryManager.InstallationResult;

/**
 * Desktop plugin for the visualizing Repository services and their offerings.
 * With functionality for install/start and detail information.
 */
public class RepositoryDisplayer
extends DefaultSwingBundleDisplayer
implements ServiceTrackerCustomizer<RepositoryManager, RepositoryManager>
{
  // Repository manager services tracker
  private final ServiceTracker<RepositoryManager, RepositoryManager> repoManagerTracker;

  static Map<String, NsToHtml> ns2html = new HashMap<>();
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
  // static final int SORT_APIVENDOR = 4;
  static final int SORT_STATUS = 5;

  static int[] SORT_ARRAY = new int[] { SORT_NONE, SORT_HOST, SORT_CATEGORY,
    SORT_VENDOR, SORT_STATUS, };

  static String[] SORT_NAMES = new String[] { "All", "Host", "Category",
    "Vendor", "Install status", };

  // Message while loading repo URLs
  static String STR_LOADING = "Loading...";

  // Name of top node
  static String STR_TOPNAME = "Repository";

  /** The currently selected bundle. */
  long currentSelection = -1;

  public RepositoryDisplayer(BundleContext bc)
  {
    super(bc, "Repository", "View and install bundles from OSGi Repositories",
        true);

    repoManagerTracker =
        new ServiceTracker<>(
            bc,
            RepositoryManager.class,
            this);
    repoManagerTracker.open();
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
    repoManagerTracker.close();
    super.close();
  }

  @Override
  protected void valueChangedLazy(long bid)
  {
    // Currently this displayer only support a single selection...
    //noinspection StatementWithEmptyBody
    if (bundleSelModel.isSelected(currentSelection)) {
      // The bundle we have selected is still amongst the selected bundles;
      // nothing to do.
    } else if (!selectionChangeInProgress) {
      // Choose another selected bundle...
      currentSelection = bundleSelModel.getSelected();
      for (final JComponent component : components) {
        final JRepositoryAdmin repositoryAdmin = (JRepositoryAdmin) component;
        repositoryAdmin.valueChanged(currentSelection);
      }
    }
    super.valueChangedLazy(bid);
  }

  @Override
  public Icon getSmallIcon()
  {
    return null;
  }

  /*------------------------------------------------------------------------*
   *       ServiceTrackerCustomizer implementation
   *------------------------------------------------------------------------*/

  @Override
  public RepositoryManager addingService(ServiceReference<RepositoryManager> sr)
  {
    final RepositoryManager repo = bc.getService(sr);
    refreshRepositoryDisplayers();
    return repo;
  }

  @Override
  public void modifiedService(ServiceReference<RepositoryManager> sr,
      RepositoryManager repoManager)
  {
    refreshRepositoryDisplayers();
  }

  @Override
  public void removedService(ServiceReference<RepositoryManager> sr,
      RepositoryManager repoManager)
  {
    refreshRepositoryDisplayers();
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
      if (Util.isBundleFromResource(bl[i], resource)) {
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
   *          The category string.
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
      if (loc == null) {
        return "[no location]";
      }
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
        final StringBuilder sb = new StringBuilder();

        Util.startFont(sb);

        sb.append("<p><b>Repositories</b></p><ul>");
        final RepositoryManager repoMgr = repoManagerTracker.getService();
        if (repoMgr != null) {
          final Set<RepositoryInfo> ris = repoMgr.getAllRepositories();
          for (final RepositoryInfo ri : ris) {
            final ServiceReference<Repository> sr = ri.getServiceReference();
            sb.append("<li><p>");
            toHTML(sb, sr, Constants.SERVICE_DESCRIPTION, "");
            toHTML(sb, sr, Constants.SERVICE_PID, "PID: ");
            toHTML(sb, sr, Repository.URL, "URLs: ");
            sb.append("</p>");
          }
          sb.append("</ul>");

          sb.append("<p>");
          sb.append("Total number of bundles: ").append(JRepositoryAdmin.this.locationMap.size());
          sb.append("</p>");

          appendHelp(sb);
        } else {
          sb.append("<p><em>No</em> repositories available.</p>");
          sb.append("<p>Press the settings button (rightmost button under "
              + "the repository tree) to add repositories.</p>");
        }

        sb.append("</font>");
        return sb.toString();
      }

      private void toHTML(final StringBuilder sb,
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
        final StringBuilder sb = new StringBuilder();

        Util.startFont(sb);

        sb.append("<p>");
        sb.append("Bundles in this category: ").append(getChildCount());
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
      SortedMap<String, Set<Capability>> ns2caps = new TreeMap<>();
      final Set<Capability> idCaps;
      final Set<Capability> kfExtraCaps;

      // Sorted cache of requirements required by the given resource
      SortedMap<String, Set<Requirement>> ns2reqs = new TreeMap<>();

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
          Set<Capability> caps = ns2caps.computeIfAbsent(ns, k -> new HashSet<>());
          caps.add(capability);
        }
        idCaps = ns2caps.remove(IdentityNamespace.IDENTITY_NAMESPACE);
        kfExtraCaps = ns2caps.remove(Util.KF_EXTRAS_NAMESPACE);

        for (final Requirement requirement : resource.getRequirements(null)) {
          final String ns = requirement.getNamespace();
          Set<Requirement> reqs = ns2reqs.computeIfAbsent(ns, k -> new HashSet<>());
          reqs.add(requirement);
        }

        // setIcon(bundleIcon);
      }

      public Bundle getBundle()
      {
        if (bundle != null && bundle.getState() == Bundle.UNINSTALLED) {
          // Clear stale bundle reference.
          bundle = null;
        }
        if (bundle == null) {
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

      public String getIconURL()
      {
        String iconURL = Util.getResourceIcon(resource);
        if (iconURL != null) {
          // Simply take the first icon defined, ignoring size and other
          // attributes.
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
          } else if (!iconURL.contains(":")) {
            iconURL = "jar:" + Util.getLocation(resource) + "!/" + iconURL;
          }
        }
        return iconURL;
      }

      @Override
      public String toHTML()
      {
        final StringBuilder sb = new StringBuilder();

        sb.append("<table border='0' width='100%'>\n");

        // osgi.identity name space is handled specially
        if (idCaps.size() == 0) {
          Util.toHTMLtrError_2(sb,
              "No osgi.identity capabilities in this node!");
        } else {
          Util.toHTMLtrHeading_2(sb,
              getIdAttribute(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE));
          for (final Capability idCap : idCaps) {
            // Make a copy of the map so that we can remove processed entries.
            final SortedMap<String, Object> attrs =
                new TreeMap<>(idCap.getAttributes());

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
            for (final Entry<String, Object> entry : attrs.entrySet()) {
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
          return (String) kfExtraCaps.iterator().next().getAttributes()
              .get(key);
        }
        return null;
      }

      @Override
      public String getTitle()
      {
        // This is displayed in the HTML to the right.
        if (bundle != null) {
          if (bundle.getState() != Bundle.UNINSTALLED) {
            return "#" + bundle.getBundleId() + " " + name;
          } else {
            bundle = null;
          }
        }
        return name;
      }

    }

    private static final long serialVersionUID = 1L;

    DefaultTreeModel treeModel;
    JTree resourceTree;
    JPanel resourcePanel;
    JButton installButton;
    JButton refreshButton;
    JButton startButton;
    JTextPane html;
    JScrollPane htmlScroll;

    TopNode rootNode;

    // Currently selected bundle in this JRepositoryAdmin.
    Bundle selectedBundle = null;

    // Currently selected tree node. Will differ from node of the selected
    // bundle when the user has selected a node for an uninstalled resource.
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
    final Map<String, RepositoryNode> locationMap = new HashMap<>();

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
            }
            setToolTipText(tt);
          } catch (final Exception ignored) {
          }
          return this;
        }

        // TODO: Use resource specific icons when available.
        // Map<String,Icon> bundleIconCache = new HashMap<String, Icon>();
        @SuppressWarnings("unused")
        private Icon getBundleIcon(String iconURL)
        {
          // if (iconURL == null) {
          return bundleIcon;
          // }
          // Icon res = bundleIconCache.get(iconURL);
          // if (res == null) {
          // try {
          // res = new ImageIcon(iconURL);
          // } catch (final Exception e) {
          // // Fallback to the standard icon.
          // res = bundleIcon;
          // Activator.log.info("Failed to load icon with URL: " +iconURL, e);
          // }
          // bundleIconCache.put(iconURL, res);
          // }
          // return res;
        }
      };

      resourceTree.setCellRenderer(renderer);

      // call setSelected() when user selects nodes/leafs in the tree
      resourceTree.addTreeSelectionListener(e -> {
        final TreePath[] sel = resourceTree.getSelectionPaths();
        if (sel != null && sel.length == 1) {
          setSelected((TreeNode) sel[0].getLastPathComponent());
        } else {
          setSelected(null);
        }
      });

      // Create the HTML text pane for detail view.
      // The node's getTitle()/toHTML() methods
      // will be called whenever a node is HTML-able
      html = new JTextPane();
      html.setText("");
      html.setContentType("text/html");

      html.setEditable(false);

      html.addHyperlinkListener(ev -> {
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
      repoButton.addActionListener(ev -> showSettingsDialog());

      installButton = new JButton(installIcon);
      installButton.setToolTipText("Install from repository");
      ActionListener installAction;
      installButton.addActionListener(installAction = ev ->
          installOrStart(selectedRepositoryNode, false));
      installButton.setEnabled(false);

      startButton = new JButton(startIcon);
      startButton.setToolTipText("Install and start from repository");
      ActionListener startAction;
      startButton.addActionListener(startAction = ev ->
          installOrStart(selectedRepositoryNode, true));
      startButton.setEnabled(false);

      refreshButton = new JButton(reloadIcon);
      refreshButton.setToolTipText("Refresh repository list");
      refreshButton.addActionListener(ev -> refreshList());

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
          selectedBundle = bc.getBundle(bid);
        } else {
          selectedBundle = null;
        }
        updateTreeSelection(null);
      } catch (final Exception ignored) {
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
        if (Util.isBundleFromResource(b, rn.getResource())) {
          return rn;
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
        item.addActionListener(ev -> {
          sortCategory = cat;
          refreshList();
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

      // Check if bundle already is installed.
      if (node.getBundle() != null) {
        // Offer to update the bundle with data from the repository.
        final String[] options = new String[] { "Update", "Cancel" };

        final String msg =
            "The selected resource is already installed.\n"
                + "It can be updated from the repository.";

        final int n =
            JOptionPane.showOptionDialog(resourceTree,
                msg,
                "Bundle is installed", // title
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, // icon
                options, options[0]);

        if (n == 0) { // Update
          try (InputStream in = ((RepositoryContent) node.getResource()).getContent()) {
            node.getBundle().update(in);
            node.appendLog("Updated from "
                + Util.getLocation(node.getResource()) + "\n");
          } catch (final Exception e) {
            node.appendLog("Update failed: " + e + "\n");
          }
          return;
        }
      }

      // Install the bundle from the repository resource

      String message = "";
      try {
        boolean bResolve = false;

        // If we detect a Resolver we offer to resolve and install dependencies.
        if(repoManagerTracker.getService().resolverAvailable()) {
          Set<Resource> resolved = repoManagerTracker.getService().findResolution(Collections.singletonList(resource));
          resolved.remove(resource);
          if(!resolved.isEmpty()) { // Additional dependencies exsist
            final String[] options = new String[] { "Yes", "No" };
  
            String msg =
                "Do you want to resolve the selected resource\n"
                    + "and install the following dependencies:\n";
            for (Resource dependency : resolved) {
              //noinspection StringConcatenationInLoop
              msg += dependency.getCapabilities(ContentNamespace.CONTENT_NAMESPACE)
                  .get(0).getAttributes()
                  .get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE) + "\n";
            }
  
            final int n =
                JOptionPane.showOptionDialog(resourceTree,
                    msg,
                    "Resolve the selected resource?", // title
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, // icon
                    options, options[0]);
            bResolve = n == 0;
          }
        }
        InstallationResult ir = 
            repoManagerTracker.getService().install(Collections.singletonList(resource), bResolve, bStart);

        node.setInstalled(getBundle(resource));

        for (String m : ir.userFeedback) {
          //noinspection StringConcatenationInLoop
          message += m + "\n";
        }


        if (sortCategory == SORT_STATUS) {
          refreshList();
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
      final Thread t = new Thread(() -> {
        // Protect against synchronous refresh operations.
        synchronized (locationMap) {
          locationMap.clear();
          setRootText(STR_LOADING);

          rootNode = new TopNode(STR_TOPNAME, JRepositoryAdmin.this);
          treeModel = new DefaultTreeModel(rootNode);

          // String (category) -> Set (Resource)
          final Map<String, Set<Resource>> categories =
              new TreeMap<>(String::compareToIgnoreCase);

          // move all resource into a sorted
          // category map of sets
          // First find all downloadable resources in all repos.
          final Set<Resource> resources = new HashSet<>();
          final Requirement downloadableReq =
              new DownloadableBundleRequirement();
          final RepositoryManager repoMgr = repoManagerTracker.getService();
          if (repoMgr != null) {
            final List<Capability> capabilities =
                repoManagerTracker.getService().findProviders(downloadableReq);
            for (final Capability capability : capabilities) {
              resources.add(capability.getResource());
            }
          }
          // Categorize each resource
          for (final Resource resource : resources) {
            final String category = deriveCatagory(resource, sortCategory);

            Set<Resource> set = categories.get(category);
            if (set == null) {
              set = new TreeSet<>(new ResourceComparator());
              categories.put(category, set);
            }
            set.add(resource);
          }

          for (final Entry<String, Set<Resource>> entry : categories
              .entrySet()) {
            final String category = entry.getKey();
            final DefaultMutableTreeNode categoryNode =
                new CategoryNode(category);

            for (final Resource resource : entry.getValue()) {
              final RepositoryNode resourceNode =
                  new RepositoryNode(resource);
              categoryNode.add(resourceNode);
              final String loc = Util.getLocation(resource);
              if (loc != null) {
                locationMap.put(loc, resourceNode);
              }
            }
            rootNode.add(categoryNode);
          }

          updateTreeSelection(treeModel);
        }
      });
      t.start();
    }

    /**
     * Update the resource tree to show the node for the selected bundle.
     *
     * @param model
     *          if not <tt>null</tt>, set as tree's model before setting
     *          selected path.
     */
    void updateTreeSelection(final TreeModel model)
    {
      SwingUtilities.invokeLater(() -> {
        // Get the currently selected bundle to show and select the node for.
        final Bundle selectedBundle = JRepositoryAdmin.this.selectedBundle;

        // Make the new model active.
        if (model != null) {
          resourceTree.setModel(model);
        }
        final TreeModel treeModel = resourceTree.getModel();

        final RepositoryNode bundleNode =
            selectedBundle != null ? getRepositoryNode(selectedBundle) : null;

        TreePath tp = null;
        if (bundleNode != null) {
          if (bundleNode != selectedRepositoryNode) {
            tp = new TreePath(bundleNode.getPath());
          }
        } else {
          // No node for the selected bundle, select the root node.
          tp =
              new TreePath(((DefaultMutableTreeNode) treeModel.getRoot())
                  .getPath());
        }

        if (tp != null) {
          resourceTree.expandPath(tp);
          resourceTree.setSelectionPath(tp);
          resourceTree.scrollPathToVisible(tp);
        }
      });
    }

    /**
     * Clear the tree and set the text of the tree's root node.
     */
    void setRootText(final String s)
    {
      try {
        SwingUtilities.invokeAndWait(() -> resourceTree
        .setModel(new DefaultTreeModel(
            new TopNode(
                s,
                JRepositoryAdmin.this))));
      } catch (final Exception ignored) {
      }
    }

    /**
     * Configure repositories.
     */
    void showSettingsDialog()
    {
      try {
        final RepositoryManager repoMgr = repoManagerTracker.getService();
        final RepositoriesTableModel tm = new RepositoriesTableModel(repoMgr);

        final JPanel panel = new JPanel(new BorderLayout());

        final JTable table = new JTable(tm);
        // table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        final TableColumnModel tcm = table.getColumnModel();
        // The longest item in column 1 and 2 is the title...
        tcm.getColumn(1).setMinWidth(2 * 12);
        tcm.getColumn(1).setMaxWidth(RepositoriesTableModel.COLUMN_NAMES[1]
            .length() * 12);
        tcm.getColumn(2).setMinWidth(2 * 12);
        tcm.getColumn(2).setMaxWidth(RepositoriesTableModel.COLUMN_NAMES[2]
            .length() * 12);
        final JScrollPane scroll =
            new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scroll.setPreferredSize(new Dimension(700, 200));

        panel.add(scroll, BorderLayout.CENTER);
        final String[] options = new String[] { "Cancel", "Add...", "Apply" };
        final int option =
            JOptionPane.showOptionDialog(this, panel, "Configure Repositories",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null,
                options, options[2]);

        if (option == 1) {
          // Add new XML repository; ask for the URL.
          final Object urlString =
              JOptionPane.showInputDialog(this, "URL to the XML-file",
                  "Add XML-based Repository",
                  JOptionPane.PLAIN_MESSAGE, null, null,
                  null);
          if (urlString != null) {
            try {
              repoMgr.addXmlRepository((String) urlString, null);
              // The service modified event for the repMgr will trigger refresh.
            } catch (final Exception e) {
              JOptionPane.showMessageDialog(this,
                  "Failed to create XML repository for URL: '"
                      + urlString + "': " + e);
            }
          }
        } else if (option == 2) {
          tm.applyChanges();
          refreshList();
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
      if (node instanceof RepositoryNode) {
        selectedRepositoryNode = (RepositoryNode) node;
        final Bundle b = selectedRepositoryNode.getBundle();
        if (b != null) {
          selectBid(b.getBundleId());
        }
      } else {
        selectedRepositoryNode = null;
      }

      installButton.setEnabled(selectedRepositoryNode != null && !bBusy);
      startButton.setEnabled(selectedRepositoryNode != null && !bBusy);

      final StringBuilder sb = new StringBuilder();

      sb.append("<html>\n");

      sb.append("<table border=\"0\" width=\"100%\">\n");
      sb.append("<tr>");

      if (node instanceof HTMLAble) {
        final HTMLAble htmlNode = (HTMLAble) node;
        sb.append("<td valign=\"top\" bgcolor=\"#eeeeee\">");

        Util.startFont(sb, "-1");

        sb.append(htmlNode.getTitle());

        sb.append("</font>\n");
        sb.append("</td>\n");

        // jar:-URLs are not handled by the HTML-document...
        // final String iconURL = htmlNode.getIconURL();
        // if (iconURL != null && !"".equals(iconURL.trim())) {
        // System.out.println("iconURL: " +iconURL);
        // sb.append("<td valign=\"top\" bgcolor=\"#eeeeee\">");
        // sb.append("<img align=\"left\" src=\"" + iconURL + "\">");
        // sb.append("</td>");
        // }
      }

      sb.append("</tr>\n");
      sb.append("</table>\n");

      if (node instanceof HTMLAble) {
        final HTMLAble htmlNode = (HTMLAble) node;
        sb.append(htmlNode.toHTML());
      } else {
        appendHelp(sb);
      }

      sb.append("</html>");
      setHTML(sb.toString());
      resourceTree.invalidate();
      resourceTree.repaint();
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

      SwingUtilities.invokeLater(() -> {
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
      });
    }
  }

  /**
   * Set to true before clearing the selection when it will be immediately
   * followed by a new selection. Reset to false when the clear is done.
   */
  boolean selectionChangeInProgress = false;

  /**
   * Set the given bundle as selected in the global selection model.
   *
   * @param bid
   *          The bundle to select.
   *
   */
  void selectBid(long bid)
  {
    if (!getBundleSelectionModel().isSelected(bid)) {
      if (getBundleSelectionModel().getSelectionCount() > 0) {
        selectionChangeInProgress = true;
        getBundleSelectionModel().clearSelection();
        selectionChangeInProgress = false;
      }
      getBundleSelectionModel().setSelected(bid, true);
    }
  }

  void appendHelp(StringBuilder sb)
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
    String getTitle();

    String toHTML();
  }

  /**
   * Comparator class for comparing two bundle resources
   *
   * <p>
   * Sort first by case-insensitive name, then by version
   * </p>
   */
  static class ResourceComparator
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

          if (n == 0) {
            final String loc1 = Util.getLocation(r1);
            final String loc2 = Util.getLocation(r2);
            if (loc1 != null && loc2 != null) {
              n = loc1.compareTo(loc2);
            } else if (loc1 == null) {
              n = -1;
            }
          }
        }
      } catch (final Exception ignored) {
      }
      return n;
    }
  }

}
