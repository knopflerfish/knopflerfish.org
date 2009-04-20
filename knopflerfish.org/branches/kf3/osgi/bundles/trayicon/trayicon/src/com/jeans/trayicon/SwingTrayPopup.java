
package com.jeans.trayicon;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class SwingTrayPopup extends JPopupMenu {

	// Swing popup menu for a TrayIcon
	WindowsTrayIcon m_Icon;
	MouseListener m_Listener;

	public SwingTrayPopup() {
	}
	
	// Attach the menu to a TrayIcon
	public void setTrayIcon(WindowsTrayIcon icon) {
		if (icon != null) {
			m_Icon = icon;            
			m_Icon.initJAWT();
			m_Icon.initHook();
			m_Listener = new ActivateListener();
			m_Icon.addMouseListener(m_Listener);
		} else {
			if (m_Icon != null) {
				m_Icon.removeMouseListener(m_Listener);
				m_Icon = null;
			}
        	}            
    	}
    	
	// Show the popup menu (internal use only)
	public void showMenu(int xp, int yp) {
		SwingUtilities.invokeLater(new InvokeMenu(xp, yp));
	}		

	// Test if mouse is in menu or submenu
	private boolean componentContains(JComponent comp, int xp, int yp) {
		if (!comp.isVisible()) return false;
		Point pt = comp.getLocationOnScreen();
		Dimension s = comp.getSize();
		boolean contains = xp > pt.x && xp < pt.x+s.width && yp > pt.y && yp < pt.y+s.height;
		if (contains) return true;
		for (int i = 0; i < comp.getComponentCount(); i++) {			
			JComponent child = (JComponent)comp.getComponent(i);
			if (child instanceof JMenu) {
				JMenu submenu = (JMenu)child;
				if (componentContains(submenu.getPopupMenu(), xp, yp)) {
					return true;
				}
			}
		}
		return false;
	}

	// Test if mouse is in menu or submenu	
	private boolean menuContains(int xp, int yp) {
		return componentContains(this, xp, yp);
	}

	// Callback listener handles icon events (Mouse hook)
	private class ClickListener extends MouseAdapter {

		public void mousePressed(MouseEvent evt) {
			if (!menuContains(evt.getX(), evt.getY())) {
				setVisible(false);
				WindowsTrayIcon.setMouseClickHook(null);
			}
		}
	}
	
	// Callback listener handles icon events
	private class ActivateListener extends MouseAdapter {

		public void mousePressed(MouseEvent evt) {
		    if (evt.isPopupTrigger() && (evt.getModifiers() & MouseEvent.BUTTON2_MASK) != 0 && evt.getClickCount() == 1) {
				showMenu(evt.getX(), evt.getY());
			}
		}
	}
	
	private class InvokeMenu implements Runnable {
	
		int m_Xp, m_Yp;
	
		public InvokeMenu(int x, int y) {
			m_Xp = x; m_Yp = y;
		}
	
		public void run() {
			TrayDummyComponent frame = WindowsTrayIcon.getDummyComponent();

			// This should show the menu at a better location :-)
			//  * Thanks to Danny <danny@isfantastisch.nl> for the 
			//    setAlwaysOnTop and updateUI() hint
        
			WindowsTrayIcon.setMouseClickHook(new ClickListener());
			Dimension d = getPreferredSize();
			show(frame, m_Xp-d.width, m_Yp-d.height);
			WindowsTrayIcon.setAlwaysOnTop(frame, true);		
			updateUI();        			
		}	
	}
}
