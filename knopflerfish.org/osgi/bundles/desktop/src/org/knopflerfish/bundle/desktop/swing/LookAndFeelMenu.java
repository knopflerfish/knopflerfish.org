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

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;
import javax.swing.LookAndFeel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.Component;
import java.awt.Cursor;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.*;

/**
 ** A menu that handles Look & Feel changes. for a Java GUI.
 **
 ** This menu contains radio button menu items, one for each look and
 ** feel returned by the method UIManager.getInstalledLookAndFeels.
 ** The name of the menu item is the name of the look and feel and the
 ** action command is the full class name of the JAva class
 ** implementing the look and feel.
 **
 ** @author Gunnar Ekolin, Gatespace AB
 ** @version $Revision: 1.2 $
 **/
public class LookAndFeelMenu
  extends JMenu
  implements ItemListener
{

  final ButtonGroup lfGroup = new ButtonGroup();

  //  Component root = null;
  LFManager lfManager;

  /**
   * <tt>setRoot</tt> must be called manually when the owning frame is
   * up and visible.
   */
  public LookAndFeelMenu( final String menuName, LFManager lfManager ) {
    super( menuName );

    id++;

    this.lfManager = lfManager;

    final LookAndFeel currentLF = UIManager.getLookAndFeel();
    final String currentLFName  = currentLF!=null ? currentLF.getName() : null;

    final UIManager.LookAndFeelInfo[] iLF
      = UIManager.getInstalledLookAndFeels();

    Vector classes = new Vector();
    Vector names   = new Vector();

    for(Enumeration e = lfManager.customLF.keys(); e.hasMoreElements(); ) { 
      String className = (String)e.nextElement();
      LookAndFeel lf   = (LookAndFeel)lfManager.customLF.get(className);
      String name      = lf.getName();

      // Check if custom LF is included in installed LFs
      boolean bIncluded = false;
      for (int i=0;  i < iLF.length; i++) {
	if(iLF[i].getClassName().equals(className)) {
	  bIncluded = true;
	  break;
	}
      }

      // If not included, add to my list
      if(!bIncluded) {
	classes.addElement(className);
	names.addElement(name);
      }
    }

    // Add all installed LFs
    for (int i=0;  i<iLF.length; i++) {
      {
	classes.addElement(iLF[i].getClassName());
	names.addElement(iLF[i].getName());
      }
    }

    // Create menu
    for (int i=0;  i<classes.size(); i++) {
      String className  = (String)classes.elementAt(i);
      String name       = (String)names.elementAt(i);
      JRadioButtonMenuItem rbMenuItem =
	(JRadioButtonMenuItem) add( new JRadioButtonMenuItem( name ) );
      rbMenuItem.setActionCommand( className );

      /*
      rbMenuItem.setAccelerator
	( KeyStroke.getKeyStroke( KeyEvent.VK_0+ i, ActionEvent.ALT_MASK ) );
      */

      rbMenuItem.getAccessibleContext().setAccessibleDescription
	("The look and feel option for '"+name+"'.");
      rbMenuItem.setSelected( name.equals( currentLFName ) );
      rbMenuItem.addItemListener( this ); 

      lfGroup.add( rbMenuItem );
    }

  }//LookAndFeelMenu

  Set roots = new HashSet();

  void addRoot(Component root) {
    roots.add(root);
  }

  static int id = 0;

  public String toString() {
    return "LookAndFeelMenu[id=" + id + "]";
  }

  /** Activate selected L&F */
  public void itemStateChanged(ItemEvent e) {
    /*
    if(root == null) {
      Activator.log.warn("No root component set in LookAndFeelMenu");
    }
    */

    //Ignore all events but selection.
    if (e.getStateChange() != ItemEvent.SELECTED) return;

    // Change to wait cursor

    /*
    if (root!=null) {
      root.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
    */

    final JRadioButtonMenuItem rb   = (JRadioButtonMenuItem) e.getSource();
    final String newLFClassName     = rb.getActionCommand();
    final String newLFName          = rb.getText();
    final String currentLFClassName = UIManager.getLookAndFeel().getName();


    LookAndFeel cLF = (LookAndFeel)lfManager.customLF.get(newLFClassName);

    if(cLF != null) {     // First, check if it any of the custom LFs
      try {

	
	Activator.log.debug("set custom LF classloader to" + cLF.getClass().getClassLoader());
	UIManager.getLookAndFeelDefaults().put("ClassLoader", cLF.getClass().getClassLoader());

	Activator.log.debug("set custom LF " + newLFClassName);
	UIManager.setLookAndFeel(cLF);
	
	for(Iterator it = roots.iterator(); it.hasNext();) {
	  Component root = (Component)it.next();
	  SwingUtilities.updateComponentTreeUI( root );
	}

      } catch (Exception ex) {
	Activator.log.error("Failed to set LF " + newLFClassName, ex);
      }
    } else {  // Otherwise, go for one of the installed LFs
      if (newLFClassName!=null && !newLFClassName.equals(currentLFClassName)) {
	try {
	  Activator.log.debug("set installed LF " + newLFClassName);
	  UIManager.setLookAndFeel(newLFClassName);
	  try {
	    for(Iterator it = roots.iterator(); it.hasNext();) {
	      Component root = (Component)it.next();
	      SwingUtilities.updateComponentTreeUI( root );
	    }

	  } catch (NullPointerException npe) {
	    Activator.log.error
	      ( "Unexpected error while applying new look and feel", npe); 
	  }
	} catch (UnsupportedLookAndFeelException exc) {
	  rb.setEnabled( false );
	  Activator.log.error( "Unsupported LookAndFeel: " + newLFName
			       +" ("+newLFClassName+")", exc );
	  
	  // Fallback: Set L&F to cross platform L&F
	  Activator.log.error( "Reverting to the cross platform LookAndFeel." );
	  final String cpLFcn = UIManager.getCrossPlatformLookAndFeelClassName();
	  // Find the menu item with the cross platform L&F and select it
	  for (Enumeration lfBEnum = lfGroup.getElements();
	       lfBEnum.hasMoreElements(); ) {
	    final AbstractButton ab =(AbstractButton)lfBEnum.nextElement();
	    if (cpLFcn.equals( ab.getActionCommand() )) {
	      SwingUtilities.invokeLater( new Runnable(){
		  public void run() { ab.setSelected(true); }} );
	      break;
	    }
	  }
	} catch (Exception exc) {
	  rb.setEnabled(false);
	  Activator.log.error("Could not load LookAndFeel: " +rb.getText(), exc);
	}
      } else {
	// Selected current LF, noop
      }
    }
    /*
    if(root != null) {
      root.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    */
  }

}// class LookAndFeelMenu

