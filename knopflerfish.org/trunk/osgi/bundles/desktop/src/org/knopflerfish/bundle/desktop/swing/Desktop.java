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
import org.osgi.service.packageadmin.*;
import org.osgi.service.startlevel.*;
import org.osgi.util.tracker.*;

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.io.*;
import java.net.URL;

import java.util.jar.*;
import java.util.zip.*;

import org.knopflerfish.bundle.desktop.swing.console.*;

import org.knopflerfish.service.desktop.*;
import org.knopflerfish.bundle.log.window.impl.*;
import org.knopflerfish.util.*;

import javax.swing.plaf.ComponentUI;

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

  JFrame           frame;
  
  Container        contentPane;
  
  boolean alive = false;
  
  JCardPane          bundlePanel;
  JTabbedPane        detailPanel;
  JTabbedPane        consolePanel;

  ImageIcon          updateIcon;
  ImageIcon          startIcon;
  ImageIcon          emptyIcon;
  ImageIcon          stopIcon;
  ImageIcon          uninstallIcon;
  ImageIcon          installIcon;
  
  ImageIcon          magPlusIcon;
  ImageIcon          magMinusIcon;
  ImageIcon          magFitIcon;
  ImageIcon          mag1to1Icon;
  ImageIcon          reloadIcon;

  ImageIcon          arrowUpIcon;
  ImageIcon          arrowDownIcon;

  ImageIcon          arrowUp2Icon;
  ImageIcon          arrowDown2Icon;

  ImageIcon          viewIcon;

  ImageIcon          openIcon;
  ImageIcon          saveIcon;

  ImageIcon          prevIcon;
  ImageIcon          nextIcon;

  JToolBar           toolBar;
  StatusBar          statusBar;
  JMenuBar           menuBar;

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

  JButton viewSelection;

  public Desktop() {
  }

  Map displayMap = new HashMap();
  Map detailMap  = new HashMap();

  public void start() {

    slTracker = 
      new ServiceTracker(Activator.bc, StartLevel.class.getName(), null);

    slTracker.open();
    
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

    openIcon  = new ImageIcon(getClass().getResource("/open.png"));
    saveIcon  = new ImageIcon(getClass().getResource("/save.png"));

    prevIcon  = new ImageIcon(getClass().getResource("/player_prev.png"));
    nextIcon  = new ImageIcon(getClass().getResource("/player_next.png"));

    lfManager = new LFManager();
    lfManager.init();

    consoleSwing = new ConsoleSwing(Activator.bc);
    consoleSwing.start();

    toolBar       = makeToolBar();
    statusBar     = new StatusBar("");


    frame       = new JFrame(Strings.get("frame_title"));

    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  stopFramework();
	}
      });

    
    
    contentPane = frame.getContentPane();
    
    contentPane.setLayout(new BorderLayout());
    contentPane.setSize(new Dimension(600, 400));

    bundlePanel = new JCardPane();
    bundlePanel.setPreferredSize(new Dimension(450, 300));

    toolBar       = makeToolBar();

    
    detailPanel   = new JTabbedPane();

    detailPanel.setPreferredSize(new Dimension(350, 300));

    detailPanel.setTabPlacement(JTabbedPane.BOTTOM);
    
    detailPanel.setBorder(null);


    //    displayHTML = new BundleInfoDisplayerHTML(this);

    
    contentPane.add(toolBar,      BorderLayout.NORTH);
    contentPane.add(statusBar,    BorderLayout.SOUTH);


    splitPaneHoriz = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				    bundlePanel,
				    detailPanel);

    splitPaneHoriz.setDividerLocation(bundlePanel.getPreferredSize().width);
    splitPaneHoriz.setOneTouchExpandable(false);


    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
			       splitPaneHoriz,
			       consoleSwing.getJComponent());

    splitPane.setDividerLocation(300);
    splitPane.setOneTouchExpandable(false);


    contentPane.add(splitPane,  BorderLayout.CENTER);


    DropTarget dt = 
      new DropTarget(contentPane,
		     DnDConstants.ACTION_COPY_OR_MOVE, // actions
		     this,
		     true);



    alive = true;

    Bundle[]  bl = Activator.bc.getBundles();
    for(int i = 0; i  < bl.length; i++) {
      bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bl[i]));
    }

    
    frame.setJMenuBar(menuBar = makeMenuBar());

    setIcon(frame, "/fish");

    frame.pack();
    frame.show();

    String dispFilter1 = 
      "(&" + 
      "(" + Constants.OBJECTCLASS + "=" + 
      SwingBundleDisplayer.class.getName() + 
      ")" + 
      "(" + SwingBundleDisplayer.PROP_ISDETAIL + "=false" + ")" + 
      ")"; 

    String dispFilter = 
      "(" + Constants.OBJECTCLASS + "=" + 
      SwingBundleDisplayer.class.getName() + 
      ")";
    
    try {
      dispTracker = 
	new ServiceTracker(Activator.bc, 
			   Activator.bc.createFilter(dispFilter),
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
	    
	    disp.setBundleSelectionModel(bundleSelModel);

	    if(bDetail) {
	      detailMap.put(sr, disp);

	      detailPanel.addTab(name, icon, comp, desc);
	    } else {
	      displayMap.put(sr, disp);
	      
	      bundlePanel.addTab(name, comp);
	      
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
		detailPanel.remove(comp);
	      }
	      detailMap.remove(sr);

	    } else {

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
    Activator.bc.addBundleListener(this);
    Activator.bc.addFrameworkListener(this);

    consoleSwing.getJComponent().requestFocus();
  }

  JButton toolStartBundles;
  JButton toolStopBundles;
  JButton toolUpdateBundles;
  JButton toolUninstallBundles;


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
	  
	  //	  add(new JToolBar.Separator());
	  
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
      myLevel = sls.getBundleStartLevel(Activator.bc.getBundle());
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

    for(Iterator it = displayMap.keySet().iterator(); it.hasNext(); ) {

      ServiceReference     sr   = (ServiceReference)it.next();
      SwingBundleDisplayer disp = (SwingBundleDisplayer)displayMap.get(sr);

      final String key          = (String)sr.getProperty(SwingBundleDisplayer.PROP_NAME);
      
      JMenuItem item = new JMenuItem(key);
      item.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    bundlePanel.showTab(key);
	  }
	});
      viewPopupMenu.add(item);
    }
  }
  
  
  void updateLevelItems() {
    StartLevel sls = 
      (StartLevel)slTracker.getService();

    levelItems = new String[levelMax - levelMin + 1]; 
    
    Bundle[] bundles = Activator.bc.getBundles();
    
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
	    //	      Text.replace(Util.shortLocation(bundles[j].getLocation()), ".jar", "");
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
      /*
      if(selObj != null) {
	System.out.println("model with selected " + selObj);
	model.setSelectedItem(selObj);
      }
      */
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
  ServiceTracker slTracker;
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
	  add(makeEditMenu());
	  add(makeBundleMenu());
	  add(viewMenu = makeViewMenu(null));
	  add(makeHelpMenu());
	}
      };
  }
  
  JMenu makeFileMenu() {
    return new JMenu(Strings.get("menu_file")) {
	{
	  add(new JMenuItem(Strings.get("menu_openbundles"), openIcon) {
	      { 
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
						      ActionEvent.CTRL_MASK));
		setMnemonic(KeyEvent.VK_O);
		
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      addBundle();
		    }
		  });
	      }});
	  add(new JMenuItem(Strings.get("menu_save"), saveIcon) {
	      { 
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
						      ActionEvent.CTRL_MASK));
		setMnemonic(KeyEvent.VK_S);
		
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      save();
		    }
		  });
	      }});
	  
	  
	  add(new JMenuItem("Quit framework...") {
	      { 
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
						      ActionEvent.CTRL_MASK));
		setMnemonic(KeyEvent.VK_Q);
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      stopFramework();
		    }
		  });
	      }});
	}
      };
  }

  JMenuItem itemStopBundles;
  JMenuItem itemStartBundles;
  JMenuItem itemUpdateBundles;
  JMenuItem itemUninstallBundles;

  JMenu makeBundleMenu() {
    return new JMenu(Strings.get("menu_bundles")) {
	{
	  add(itemStopBundles = new JMenuItem(Strings.get("item_stopbundles"),
					      stopIcon) {
	      { 
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      stopBundles(getSelectedBundles());
		    }
		  });
	      }
	    });
	  
	  add(itemStartBundles = new JMenuItem(Strings.get("item_startbundles"),
					       startIcon) {
	      { 
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      startBundles(getSelectedBundles());
		    }
		  });
	      }
	    });
	  
	  add(itemUpdateBundles = new JMenuItem(Strings.get("item_updatebundles"),
						updateIcon) {
	      { 
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      updateBundles(getSelectedBundles());
		    }
		  });
	      }
	    });
	  
	  add(itemUninstallBundles = new JMenuItem(Strings.get("item_uninstallbundles"),
						   uninstallIcon) {
	      { 
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      uninstallBundles(getSelectedBundles());
		    }
		  });
	      }
	    });

	  add(new JMenuItem(Strings.get("menu_refreshbundles")) {
	      { 
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
						      ActionEvent.CTRL_MASK));
		setMnemonic(KeyEvent.VK_R);
		
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      refreshBundle(getSelectedBundles());
		    }
		  });
	      }});

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

  JMenu makeViewMenu(JMenu oldMenu) {
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
    for(Iterator it = displayMap.keySet().iterator(); it.hasNext(); ) {
      ServiceReference     sr   = (ServiceReference)it.next();
      SwingBundleDisplayer disp = (SwingBundleDisplayer)displayMap.get(sr);
      final String   name = (String)sr.getProperty(SwingBundleDisplayer.PROP_NAME);
      final int c2 = count++;
      menu.add(new JRadioButtonMenuItem(name) {
	  {
	    setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + c2,
						  ActionEvent.CTRL_MASK));
	    setMnemonic(KeyEvent.VK_1 + c2);
	    addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent ev) {
		  
		  bundlePanel.showTab(name);
		}
	      });
	    group.add(this);
	  }});
    }
    
    lfMenu = new LookAndFeelMenu(Strings.get("menu_lookandfeel"), lfManager);
    
    lfMenu.addRoot(SwingUtilities.getRoot(frame));  
    menu.add(new JSeparator());
    menu.add(lfMenu);

    return menu;
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
	}
      };
  }
  
  JMenu makeEditMenu() {
    return new JMenu(Strings.get("menu_edit")) {
	{
	  add(new JMenuItem(Strings.get("item_selectall")) {
	      { 
		setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
						      ActionEvent.CTRL_MASK));
		setMnemonic(KeyEvent.VK_A);
		
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      contentPane.invalidate();
		    }
		  });
	      }
	    });
	  add(new JMenuItem(Strings.get("item_unselectall")) {
	      { 
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      bundleSelModel.clearSelection();
		      contentPane.invalidate();
		    }
		  });
	      }
	    });
	  add(new JMenuItem(Strings.get("item_clear_console")) {
	      { 
		addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent ev) {
		      consoleSwing.clearConsole();
		    }
		  });
	      }
	    });
		  
	}
      };
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
	System.out.println("stopping framework");
	Bundle sysBundle = Activator.bc.getBundle((long)0);
	sysBundle.stop();
      } catch (Exception e) {
	e.printStackTrace();
      }
    }
  }

  JFileChooser openFC = null;

  /**
   * Open a file dialog and ask for jar files to install as bundles.
   */
  void addBundle() {
    if(openFC == null) {
      openFC = new JFileChooser();
      openFC.setCurrentDirectory(new File("."));
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

    PackageAdmin pkgAdmin = (PackageAdmin)Activator.pkgTracker.getService();
    
    if(pkgAdmin == null) {
      Activator.log.error("No pkg admin available for save");
      return;
    }
    
    Bundle[] allBundles = Activator.bc.getBundles();

    
    Set pkgClosure = new TreeSet(Util.bundleIdComparator);
    
    for(int i = 0; i < targets.length; i++) {
      pkgClosure.addAll(Util.getPackageClosure(pkgAdmin, 
					       allBundles, 
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
    all.remove(Activator.bc.getBundle(0));
    
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
	  //	  System.out.println("using local jarunpacker");
	} else if(jarunpackerFile.exists()) {
	  jarunpacker_in = new FileInputStream(jarunpackerFile);
	  //	  System.out.println("using file jarunpacker");
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
      for(Iterator it = all.iterator(); it.hasNext(); ) {
	Bundle b   = (Bundle)it.next();
	String loc = b.getLocation();
	
	bid++;
	
	URL srcURL = new URL(loc);
	
	String name = Util.shortLocation(loc);
	
	ZipEntry entry = new ZipEntry(base + "/" + name);
	
	int level = -1;
	try {
	  level = sl.getBundleStartLevel(b);
	} catch (Exception ignored) {
	}
	if(level != -1) {
	  xargs.append("-initlevel " + level + "\n");
	  levelMax = Math.max(level, levelMax);
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
	System.out.println("No jarunpacker available");
      }
      // end of jarunpacker copy

    } catch (Exception e) {
      e.printStackTrace();
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
    if(b.getBundleId() == Activator.bc.getBundle().getBundleId() ||
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
    ServiceReference sr = Activator.bc.getServiceReference(PackageAdmin.class.getName());
    if(sr != null) {
      PackageAdmin packageAdmin = (PackageAdmin)Activator.bc.getService(sr);
      if(packageAdmin != null) {
	packageAdmin.refreshPackages(b);
      }
      Activator.bc.ungetService(sr);
    }
  }

  void updateBundle(Bundle b) {
    try {
      b.update();
    } catch (Exception e) {
      showErr("failed to update bundle " + Util.getBundleName(b), e);
    }
  }

  void uninstallBundle(Bundle b, boolean bAsk) {
    Object[] options = {Strings.get("yes"),
			Strings.get("no")};
    
    
    int n = bAsk 
      ? JOptionPane
      .showOptionDialog(frame,
			Strings.fmt("q_uninstallbundle", 
				     Util.getBundleName(b)),
			Strings.get("msg_uniunstallbundle"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			options[1])
      : 0;

    if(n == 0) {
      try {
	b.uninstall();
      } catch (Exception e) {
	showErr("failed to uninstall bundle " + Util.getBundleName(b), e);
      }
    }
  }




  void showErr(String msg, Exception e) {
    if(msg != null && !"".equals(msg)) {
      System.out.println(msg);
    }
    e.printStackTrace();
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
    System.out.println("add file " + file);

    try {
      if(file.getName().toUpperCase().endsWith(".JAR")) {
	try {
	  String location = "file:" + file.getAbsolutePath();
	  Bundle b = Activator.bc.installBundle(location);
	  Dictionary headers = b.getHeaders();
	  if(Util.canBeStarted(b)) {
	    startBundle(b);
	  }
	} catch (Exception e) {
	  showErr(null, e);
	}
      }
    } catch (Exception e) {
      Activator.log.error("Failed to add file", e);
    }
  }
  
  public void stop() {
    if(consoleSwing != null) {
      consoleSwing.stop();
      consoleSwing = null;
    }

    alive = false;
    if(frame != null) {
      frame.setVisible(false);
      frame = null;
    }
    Activator.bc.removeBundleListener(this);
  }

  public void valueChanged(long bid) {
    updateBundleViewSelections();
  }

  public void frameworkEvent(FrameworkEvent ev) {
    switch(ev.getType()) {
    case FrameworkEvent.STARTLEVEL_CHANGED:
      updateStartLevel();
      break;
    }
  }

  Bundle[] bundleCache;

  public void bundleChanged(BundleEvent ev) {
    Bundle b = ev.getBundle();

    if(!alive) {
      return;
    }

    bundleCache = Activator.bc.getBundles();

    boolean bMyself = 
      b.getBundleId() == Activator.bc.getBundle().getBundleId();


    updateStatusBar();
    toolBar.revalidate();
    toolBar.repaint();
  }

  void updateStatusBar() {
  }


  void showVersion() {
    String version = "1.1.1";
    String txt = Strings.fmt("str_abouttext", version);
    
    ImageIcon icon = 
      new ImageIcon(getClass().getResource("/knopflerfish-gold3.jpg"));
    
    JOptionPane.showMessageDialog(frame, 
				  txt, 
				  Strings.get("str_about"), 
				  JOptionPane.INFORMATION_MESSAGE,
				  icon);
  }

  public void setIcon(JFrame frame, String baseName) {
    String iconName = baseName + "32x32.gif";
    if (System.getProperty( "os.name", "" ).startsWith("Win")) {
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
      } else {
      }
    } catch (Exception e) {
    }
  }

  public ImageIcon getBundleEventIcon(int type) {
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
