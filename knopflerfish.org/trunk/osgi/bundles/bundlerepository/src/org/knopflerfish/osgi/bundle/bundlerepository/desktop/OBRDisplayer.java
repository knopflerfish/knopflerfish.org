/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

import org.osgi.framework.*;
import org.osgi.util.tracker.*;
import java.util.*;
import org.knopflerfish.service.desktop.*;
import org.ungoverned.osgi.service.bundlerepository.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.net.*;
import java.io.*;

import java.lang.reflect.Array;

public class OBRDisplayer extends DefaultSwingBundleDisplayer {


  static ServiceTracker obrTracker;
  

  static Icon infoIcon = null;
  public OBRDisplayer(BundleContext bc) {
    super(bc, "Bundle Repository", "Bundle Repository", true);
    
    bUseListeners = true;

    try {
      if(infoIcon == null) {
	infoIcon     = new ImageIcon(getClass().getResource("/info16x16.png"));
      }
    } catch (Exception e) {
      //
    }
    obrTracker = new ServiceTracker(bc, 
				    BundleRepositoryService.class.getName(), 
				    null);
    obrTracker.open();
  }

  static BundleRepositoryService getOBR() {
    return (BundleRepositoryService)OBRDisplayer.obrTracker.getService();
  }

  public JComponent newJComponent() {
    return new OBRAdmin();
  }

  public void  disposeJComponent(JComponent comp) {
    OBRAdmin obrAdmin = (OBRAdmin)comp;
    obrAdmin.stop();

    super.disposeJComponent(comp);
  }

  void closeComponent(JComponent comp) {
    OBRAdmin obrAdmin = (OBRAdmin)comp;
    obrAdmin.stop();    
  }

  public void valueChanged(final long bid) {
    super.valueChanged(bid);

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      OBRAdmin obrAdmin = (OBRAdmin)(JComponent)it.next();
      obrAdmin.valueChanged(bid);
    }
  }
  
  public Icon getSmallIcon() {
    return null;
  }

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
  
  String obrErr = "";

  class OBRAdmin extends JPanel {

    DefaultTreeModel treeModel;
    JTree     recordTree;
    JPanel    recordPanel;
    JToolBar  recordCmds;
    JButton   installButton;
    JButton   refreshButton;
    JButton   startButton;
    JButton   updateButton;
    JTextPane html;
    JScrollPane htmlScroll;

    TopNode rootNode;

    class BRComparator implements Comparator {
      public int compare(Object o1, Object o2) {
	BundleRecord br1 = (BundleRecord)o1;
	BundleRecord br2 = (BundleRecord)o2;
	String s1 = getBRName(br1).toLowerCase();
	String s2 = getBRName(br2).toLowerCase();
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

    ImageIcon startIcon;
    ImageIcon sortIcon;
    ImageIcon installIcon;
    ImageIcon updateIcon;
    ImageIcon bundleIcon;

    OBRNode brSelected = null;

    String topName = "Bundle Repository";



    public OBRAdmin() {
      setLayout(new BorderLayout());

      startIcon   = new ImageIcon(getClass().getResource("/player_play.png"));
      installIcon = new ImageIcon(getClass().getResource("/player_install.png"));
      updateIcon  = new ImageIcon(getClass().getResource("/update.png"));
      sortIcon    = new ImageIcon(getClass().getResource("/sort_select.png"));
      bundleIcon  = new ImageIcon(getClass().getResource("/lib16x16.png"));

      recordTree = new JTree();
      recordTree.setRootVisible(true);

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
		setIcon(bundleIcon);
		String loc = (String)obrNode.getBundleRecord().getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
		tt = loc;

		boolean bInstalled = isInstalled(obrNode.getBundleRecord());
		obrNode.setInstalled(bInstalled);
		if(bInstalled) {
		  setForeground(Color.gray);
		}
	      } else {
		//		setIcon(null);
	      }
	      setToolTipText(tt);
	    } catch (Exception ignored ) {
	    }
	    return this;
	  }
	};

      recordTree.setCellRenderer(renderer);

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
      
      html = new JTextPane();
      html.setText("");
      html.setContentType("text/html");
      
      html.setEditable(false);
      
      html.addHyperlinkListener(new HyperlinkListener() 
	{
	  public void hyperlinkUpdate(HyperlinkEvent ev) {
	    if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	      URL url = ev.getURL();
	      try {
		openExternalURL(url);
	      } catch (Exception e) {
		// Activator.log.warn("Failed to open external url=" + url, e);
	      }
	    }
	  }
	});
      
      htmlScroll = 
	new JScrollPane(html, 
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      
      htmlScroll.setPreferredSize(new Dimension(300, 300));
      
      //      recordTree.setPreferredSize(new Dimension(200, 300));

      JScrollPane treeScroll = 
	new JScrollPane(recordTree, 
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      treeScroll.setPreferredSize(new Dimension(200, 300));

      JButton repoButton = new JButton("URLs") {{
	
	setToolTipText("Set repository URLs");
	addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
	      askRepoURls();
	    }
	  });
      }
	};
      
      
      installButton = new JButton(installIcon);
      installButton.setToolTipText("Install from OBR");
      installButton.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    installOrStart(brSelected, false);
	  }
	});
      installButton.setEnabled(false);

      startButton = new JButton(startIcon);
      startButton.setToolTipText("Install and start from OBR");
      startButton.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    installOrStart(brSelected, true);
	  }
	});
      startButton.setEnabled(false);

      refreshButton = new JButton(updateIcon);
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


      JSplitPane panel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					left,
					recordPanel);
      
      panel.setDividerLocation(200);
      
      recordCmds = new JToolBar();
      
      //      recordPanel.add(recordCmds, BorderLayout.SOUTH);
      
      add(panel, BorderLayout.CENTER);

      refreshList(true);
    }

    void valueChanged(long bid) {
      try {
	if(bid >= 0) {
	  Bundle b = bc.getBundle(bid);
	  if(b != null) {
	    OBRNode obrNode = getOBRNode(b);
	    if(obrNode != null && obrNode != brSelected) {
	      TreePath tp = new TreePath(obrNode.getPath());
	      showPath(tp, false);
	    }
	  }
	}
      } catch (Exception e) {
      }
    }

    OBRNode getOBRNode(Bundle b) {
      OBRNode node = (OBRNode)locationMap.get(b.getLocation());
      
      if(node != null) {
	return node;
      }

      for(Iterator it = locationMap.keySet().iterator(); it.hasNext(); ) {
	String  loc  = (String)it.next();
	node = (OBRNode)locationMap.get(loc);
	if(bundleEqual(b, node.getBundleRecord())) {
	  return node;
	}
      }
      return null;
    }

    JButton makeSortSelectionButton() {
      JButton sortButton = new JButton(sortIcon);
      sortButton.setToolTipText("Select sorting");

      makeSortPopupMenu();
      
      sortButton.addMouseListener(new MouseAdapter() 
	{
	  public void mousePressed(MouseEvent e) {
	    showPopup(e);
	  }
	  
	  public void mouseReleased(MouseEvent e) {
	    showPopup(e);
	  }
	  
	  private void showPopup(MouseEvent e) {
	    if (sortPopupMenu != null) {
	      Component comp = e.getComponent();
	      sortPopupMenu.show(comp, 0, comp.getSize().height);
	    }
	  }
	});
      
      
      return sortButton;
    }
    
    
    JPopupMenu sortPopupMenu;
    
    void makeSortPopupMenu() {
      
      sortPopupMenu = new JPopupMenu();

      final ButtonGroup group = new ButtonGroup();

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
    }
    
    void installOrStart(OBRNode obrNode, boolean bStart) {
      BundleRecord br = obrNode.getBundleRecord();
      if(br == null) {
	return;
      }

      BundleRepositoryService obr = getOBR();

      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      PrintStream outStream = new PrintStream(bout, true);
      
      boolean bResolve = true;

      try {
	boolean bOK = obr.deployBundle(outStream, // Output stream.
				       outStream, // Error stream.
				       (String) br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION), // Update location.
				       bResolve, // Resolve dependencies.
				       bStart);
	if(bOK) {
	  if(sortCategory == SORT_STATUS) {
	    refreshList(false);
	  }
	}
      } catch (Exception e) {
	e.printStackTrace(outStream);
      } finally {
	try {  outStream.close();} catch (Exception ignored) {	}
	String s = new String(bout.toByteArray());
	obrNode.appendLog(s);
      }

      setSelected(obrNode);
    }

    int sortCategory = SORT_CATEGORY;

    // String (location) -> OBRNode
    Map locationMap = new HashMap();

    synchronized void refreshList(final boolean bReload) { 
      Thread t = new Thread() {
	  public void run() {
	    locationMap.clear();
	    BundleRecord brOld = brSelected != null ? brSelected.getBundleRecord() : null;
	    
	    rootNode = new TopNode(topName);
	    treeModel = new DefaultTreeModel(rootNode);

	    BundleRepositoryService obr = getOBR();
	    
	    if(obr != null) {
	      if(bReload) {
		obrErr = "";
		try {
		  obr.setRepositoryURLs(obr.getRepositoryURLs());
		} catch (Exception e) {
		  obrErr = "" + e;
		}
	      }
	      int count = obr.getBundleRecordCount();
	      Map categories = new TreeMap();

	      for(int i = 0; i < count; i++) {
		BundleRecord br = obr.getBundleRecord(i);

		String loc = (String)br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);

		String category = "other";
		if(sortCategory == SORT_CATEGORY) {
		  category = (String)br.getAttribute(BundleRecord.BUNDLE_CATEGORY);
		} else if(sortCategory == SORT_VENDOR) {
		  category = (String)br.getAttribute(BundleRecord.BUNDLE_VENDOR);
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
		//		System.out.println(i + ": " + host + " " + getBRName(br));
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

	      showPath(selPath, true);
	    }
	  }
	  
	};
      t.start();
    }

    void showPath(final TreePath selPath, final boolean bSetModel) {
      SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    if(bSetModel) {
	      recordTree.setModel(treeModel);
	    }

	    recordTree.expandPath(selPath);
	    recordTree.setSelectionPath(selPath);
	    recordTree.scrollPathToVisible(selPath);
	  }
	});
    }

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
	//	text.setPreferredSize(new Dimension(300, 100));
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

      installButton.setEnabled(brSelected != null);
      startButton.setEnabled(brSelected != null);

      StringBuffer sb = new StringBuffer();
    
      sb.append("<html>\n");

      sb.append("<table border=\"0\" width=\"100%\">\n");
      sb.append("<tr><td bgcolor=\"#eeeeee\">");
      startFont(sb, "-1");

      if(node != null  && (node instanceof HTMLable)) {
	HTMLable htmlNode = (HTMLable)node;
	sb.append(htmlNode.getTitle());
      } else {
	sb.append("");
      }

      sb.append("</font>\n");
      sb.append("</td>\n");
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
    getBundleSelectionModel().clearSelection();
    getBundleSelectionModel().setSelected(bid, true);
  }
  
  Bundle getBundle(BundleRecord br) {

    
    Bundle[] bl = bc.getBundles();
    
    for(int i = 0; bl != null && i < bl.length; i++) {
      if(bundleEqual(bl[i], br)) {
	return bl[i];
      }
    }
    return null;
  }

  boolean bundleEqual(Bundle b, BundleRecord br) {
    String loc = (String)br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
    if(loc.equals(b.getLocation())) {
      return true;
    }
    
    if(bundleAttrEqual(b, br, "Bundle-Name") &&
       bundleAttrEqual(b, br, "Bundle-Version")) {
      return true;
    }
    return false;
  }

  boolean bundleAttrEqual(Bundle b, BundleRecord br, String attr) {
    String val  = (String)br.getAttribute(attr);
    String val2 = (String)b.getHeaders().get(attr);
    return 
      (val2 == val) ||
      (val2 != null && val2.equals(val));
  }
  
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
  
  interface HTMLable {
    public String getTitle();
    public String toHTML();
  }


  class TopNode extends DefaultMutableTreeNode implements HTMLable {
    
    String name;
    
    public TopNode(String name) {
      this.name = name;
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
	startFont(sb);
	sb.append("<p><b>");
	sb.append(obrErr);
	sb.append("</b></p>");
	sb.append("</font>");
      }

      startFont(sb);


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
      
      return sb.toString();
    }


  }
  
  class CategoryNode extends DefaultMutableTreeNode implements HTMLable {
    String category;

    public CategoryNode(String category) {
      this.category = category;
    }

    public String toString() {
      return category + " (" + getChildCount() + ")";
    }

    public String getTitle() {
      return toString();
    }


    public String toHTML() {
      StringBuffer sb = new StringBuffer();



      startFont(sb);

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

  class OBRNode extends DefaultMutableTreeNode implements HTMLable {
    String       name;
    StringBuffer log = new StringBuffer();
    BundleRecord br;
    boolean bInstalled = false;
    
    public OBRNode(BundleRecord br) {
      super(null);
      this.br = br;

      name = getBRName(br);
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
      StringBuffer sb = new StringBuffer();
      sb.append(name);
      if(bInstalled) {
	sb.append(" (installed)");
      }
      return sb.toString();
    }

    public String toHTML() {
      StringBuffer sb = new StringBuffer();

      String[]     attrs = br.getAttributes();
      
      Map map = new TreeMap();
      for(int i = 0; i < attrs.length; i++) {
	Object obj = br.getAttribute(attrs[i]);
	map.put(attrs[i], OBRDisplayer.toHTML(obj));
      }

      String desc = (String)br.getAttribute(BundleRecord.BUNDLE_DESCRIPTION);
      if(desc != null) {
	startFont(sb);
	sb.append("<p>");
	sb.append(desc);
	sb.append("</p>");
	sb.append("</font>");

	map.remove(BundleRecord.BUNDLE_DESCRIPTION);
      }
      
      sb.append("<table border=0>");


      String log = getLog().trim();
      if(log != null && !"".equals(log)) {
	sb.append("<tr>");
	sb.append("<td bgcolor=\"#eeeeee\" colspan=\"2\" valign=\"top\">");
	sb.append("<pre>");
	OBRDisplayer.startFont(sb, "-2");
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
	OBRDisplayer.startFont(sb);
	sb.append(key);
	sb.append("</b></font>");
	sb.append("</td>");
	
	sb.append("<td valign=\"top\">");
	OBRDisplayer.startFont(sb);
	sb.append(val);
	sb.append("</font>");
	sb.append("</td>");
	
	sb.append("</tr>");
      }
      
      sb.append("</table>\n");

      return sb.toString();
    }

    public String getTitle() {
      return toString();
    }

  }


  static String toHTML(Object obj) {
    if(obj == null) {
      return "null";
    }
    if(obj instanceof String) {
      String s = (String)obj;
      try {
	URL url = new URL(s);
	return "<a href=\"" + s + "\">" + s + "</a>";
      } catch (Exception e) {
	
      }
      return s;
    } else if(obj.getClass().isArray()) {
      StringBuffer sb = new StringBuffer();
      int len = Array.getLength(obj);

      for(int i = 0; i < len; i++) {
	sb.append(toHTML(Array.get(obj, i)));
	if(i < len - 1) {
	  sb.append("<br>\n");
	}
      }
      return sb.toString();
    } else {
      return obj.toString();
    }
  }
  
  static String getBRName(BundleRecord br) {
    String s = (String)br.getAttribute(BundleRecord.BUNDLE_NAME);
    if(s == null) {
      s = (String)br.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
      int ix = s.lastIndexOf('/');
      if(ix != -1) {
	s = s.substring(ix + 1);
      }
    }
    return s;
  }

  static void startFont(StringBuffer sb) {
    startFont(sb, "-2");
  }
  
  static void stopFont(StringBuffer sb) {
    sb.append("</font>");
  }
  
  static void startFont(StringBuffer sb, String size) {
    sb.append("<font size=\"" + size + "\" face=\"Verdana, Arial, Helvetica, sans-serif\">");
  }

  static public void openExternalURL(URL url) throws IOException {
    if(isWindows()) {
      // Yes, this only works on windows
      String systemBrowser = "explorer.exe";
      Runtime rt = Runtime.getRuntime();
      Process proc = rt.exec(new String[] {
	systemBrowser, 
	"\"" + url.toString() + "\"",
      });
    } else {
      throw new IOException("Only windows browsers are yet supported");
    }
  }
  
  public static boolean isWindows() {
    String os = System.getProperty("os.name");
    if(os != null) {
      return -1 != os.toLowerCase().indexOf("win");
    }
    return false;
  }
}
