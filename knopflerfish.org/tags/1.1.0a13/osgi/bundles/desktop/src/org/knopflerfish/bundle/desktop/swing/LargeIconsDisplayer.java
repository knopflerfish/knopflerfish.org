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

import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.event.*;
import java.awt.Container;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

import java.util.List;
import java.util.Dictionary;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.*;
import java.net.URL;


public class LargeIconsDisplayer extends DefaultSwingBundleDisplayer {

  public LargeIconsDisplayer(BundleContext bc) {
    super(bc, "Large Icons", "Large icon display of bundles", false);
  }

  public void bundleChanged(BundleEvent ev) {
    super.bundleChanged(ev);

    //    System.out.println(getClass().getName() + ": #" + ev.getBundle().getBundleId());

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JLargeIcons comp = (JLargeIcons)it.next();
      switch(ev.getType()) {
      case BundleEvent.INSTALLED:
	comp.addBundle(ev.getBundle());
	break;
      case BundleEvent.UNINSTALLED:
	comp.removeBundle(ev.getBundle());
	break;
      }
      comp.updateBundleComp(ev.getBundle());
    }
    
    repaintComponents();
  }

  public JComponent newJComponent() {
    return new JLargeIcons();
  }

  
  class JLargeIcons extends JPanel {
    Map         bundleMap = new TreeMap();
    GridLayout  grid;
    ImageIcon   bundleIcon;
    JPanel      panel;
    JScrollPane scroll;

    Color       selColor = new Color(200, 200, 255);

    public JLargeIcons() {
      setLayout(new BorderLayout());
      setBackground(Color.white);
      
      grid  = new GridLayout(4, 0);
      panel = new JPanel(grid);

      panel.setBackground(getBackground());

      JScrollPane scroll = new JScrollPane(panel);
      
      add(scroll, BorderLayout.CENTER);
    }

    public void addBundle(final Bundle b) {
      
      final long bid = b.getBundleId();

      if(null == getBundleComponent(b)) {
	JLabel c = new JLabel(Util.getBundleName(b),  
			      bundleIcon,
			      SwingConstants.CENTER) { 
	    {
	      addMouseListener(new MouseAdapter() {
		  public void mousePressed(MouseEvent ev) {
		    if((ev.getModifiers() & InputEvent.CTRL_MASK) != 0) {
		      bundleSelModel
			.setSelected(bid, !bundleSelModel.isSelected(bid));
		    } else {
		      bundleSelModel.clearSelection();
		      bundleSelModel.setSelected(bid, true);
		    }
		    setBackground(getBackground());
		  }
		});
	      setBorder(null);
	      setOpaque(true);
	      setBackground(Color.yellow);
	    }
	    
	    
	    public Color getBackground() {

	      try {
		boolean bSel = bundleSelModel != null 
		  ? bundleSelModel.isSelected(b.getBundleId())
		  : false;
		
		return bSel 
		  ? selColor
		  : JLargeIcons.this.getBackground();
	      } catch (Exception e) {
		return Color.black;
	      }
	    }
	  };
	
	//	System.out.println("created icon " + c.getText());

	c.setToolTipText(Util.bundleInfo(b));
	c.setVerticalTextPosition(AbstractButton.BOTTOM);
	c.setHorizontalTextPosition(AbstractButton.CENTER);
	
	c.setPreferredSize(new Dimension(96, 64));
	c.setBorder(null);
	c.setFont(getFont());
	
	bundleMap.put(new Long(b.getBundleId()), c);
	updateBundleComp(b);
	panel.add(c);
	grid.setColumns(4);
	grid.setRows(0);

	revalidate();
	repaint();
      }
      
    }
    
    public void removeBundle(Bundle b) {

      JComponent c = getBundleComponent(b);

      if(c != null) {
	panel.remove(c);
	revalidate();
	repaint();
      }
      icons.remove(b);
    }
   
    JComponent getBundleComponent(Bundle b) {
      return (JComponent)bundleMap.get(new Long(b.getBundleId()));
    }
        
    // Bundle -> BundleIMageIcon
    Map icons = new HashMap();
    
    public void updateBundleComp(Bundle b) {
      
      JLabel c = (JLabel)getBundleComponent(b);

      if(c == null) {
	addBundle(b);
	return;
      }

      c.setToolTipText(Util.bundleInfo(b));
      
      Icon icon = (Icon)icons.get(b);
      
      if(icon == null) {
	URL appURL = null;
	String iconName = (String)b.getHeaders().get("Application-Icon");
	if(iconName == null) {
	  iconName = "";
	}
	iconName = iconName.trim();
	
	if(iconName != null && !"".equals(iconName)) {
	  try {
	    appURL = new URL("bundle://" + b.getBundleId() + "/" + iconName);
	  } catch (Exception e) {
	    Activator.log.error("Failed to load icon", e);
	  }
	}

	//	System.out.println("#" + b.getBundleId() + ", appURL=" + appURL);
	try {
	  if(Util.hasMainClass(b)) {
	    icon = new BundleImageIcon(b, 
				       appURL != null ? appURL : getClass().getResource("/jarexec.gif"));
	  } else if(Util.hasActivator(b)) {
	    icon = new BundleImageIcon(b, 
				       appURL != null ? appURL : getClass().getResource("/bundle.png"));
	  } else {
	    icon = new BundleImageIcon(b, 
				       appURL != null ? appURL : getClass().getResource("/lib.png"));
	  }
	} catch (Exception e) {
	  Activator.log.error("Failed to load icon, appURL=" + appURL);
	  icon = new BundleImageIcon(b, getClass().getResource("/bundle.png"));
	}
	icons.put(b, icon);
      }
      
      c.setIcon(icon);
      
      
      c.invalidate();
      c.repaint();
      invalidate();
      repaint();
    }
  }
}


