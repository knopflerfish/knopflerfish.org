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

package org.knopflerfish.bundle.trayicon;

import org.osgi.framework.*;
import com.jeans.trayicon.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.*;
import java.net.URL;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.JMenuItem;

import org.knopflerfish.service.trayicon.*;

public class TrayIconWrapper {

  WindowsTrayIcon windowsTrayIcon;
  TrayIconManager manager;
  TrayIcon        trayIcon;

  public TrayIconWrapper(TrayIcon trayIcon) {
    this.manager  = Activator.manager;
    this.trayIcon = trayIcon;
    if(trayIcon == null) {
      throw new IllegalArgumentException("TrayIcon argument cannot be null");
    }
  }

  public void open() {
    if(windowsTrayIcon != null) {
      Activator.log.info("id=" + trayIcon.getId() + " already open");
      return;
    }
    
    if(WindowsTrayIcon.isRunning(trayIcon.getId())) {
      Activator.log.warn("id=" + trayIcon.getId() + " already running");
    }
    
    try {
      
      Activator.log.info("open trayicon id=" + trayIcon.getId() + 
			 ", name=" + trayIcon.getName());
      
      WindowsTrayIcon.initTrayIcon(trayIcon.getId());
      
      Image img = loadImage(trayIcon.getImageURL());

      windowsTrayIcon = new WindowsTrayIcon(img, 
					    img.getWidth(null), 
					    img.getHeight(null));

      manager.logErr();

      windowsTrayIcon.setToolTipText(trayIcon.getName());
      manager.logErr();

      windowsTrayIcon.addMouseListener(trayIcon);
      manager.logErr();

      windowsTrayIcon.addActionListener(trayIcon);
      manager.logErr();

      /*
      windowsTrayIcon.addBalloonListener(trayIcon);
      manager.logErr();
      */

      /*
      TrayIconPopup popup = trayIcon.getTrayIconPopup();
      if(popup != null) {
	windowsTrayIcon.setPopup(popup);
	manager.logErr();
      }
      */

      JPopupMenu jPopup = trayIcon.getTrayJPopupMenu();

      if(jPopup != null) {

	// Convert top level of menu to something the original
	// code understands.
	SwingTrayPopup swingPopup = new SwingTrayPopup();
	MenuElement[] subMenus= jPopup.getSubElements();
	
	for(int i = 0; i < subMenus.length; i++) {
	  if(subMenus[i] instanceof JMenuItem) {
	    swingPopup.add((JMenuItem)subMenus[i]);
	  }
	}

	swingPopup.setTrayIcon(windowsTrayIcon); 
      }

      windowsTrayIcon.setVisible(true);
      manager.logErr();

      String startupMsg = trayIcon.getStartupMessage();
      if(startupMsg != null) {
	if(WindowsTrayIcon.supportsBalloonMessages()) {
	  windowsTrayIcon.showBalloon(startupMsg,
				      trayIcon.getName(),
				      5 * 1000,
				      WindowsTrayIcon.BALLOON_INFO);
	  
	}
      }
      
      sendEvent(new TrayEvent(TrayEvent.OPENED));
    } catch (Exception e) {
      Activator.log.error("Failed to set up TrayIcon", e);
    }
  }

  void sendEvent(TrayEvent ev) {
    try {
      trayIcon.trayEvent(ev);
    } catch (Exception e) {
      Activator.log.error("event send failed", e);
    }
  }
  
  void close() {
    Activator.log.info("TrayIconWrapper.close() trayicon id=" + trayIcon.getId());
    if(trayIcon != null) {
      sendEvent(new TrayEvent(TrayEvent.CLOSED));
    }

    if(windowsTrayIcon != null) {
      Activator.log.info(" call freeIcon");
      windowsTrayIcon.freeIcon();
      Activator.log.info(" called freeIcon");
    }
    Activator.log.info("TrayIconWrapper.closed trayicon id=" + trayIcon.getId());

    trayIcon        = null;
    windowsTrayIcon = null;
  }
  
  Object imageLock = new Object();
  boolean bGotImage = false;

  Image loadImage(URL url) throws IOException {
    synchronized(imageLock) {
      Toolkit tk = Toolkit.getDefaultToolkit();
      Image img = tk.createImage(loadURL(url));
      
      img.getHeight(new ImageObserver() {
	  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
	    if(0 != (infoflags & ImageObserver.HEIGHT)) {
	      synchronized(imageLock) {
		bGotImage = true;
		imageLock.notifyAll();
	      }
	      return true;
	    }
	    return false;
	  }
	});
      
      
      if(!bGotImage) {
	try {
	  imageLock.wait(1000 * 10);
	} catch (InterruptedException e) {
	  throw new IOException("Image load timed out: " + e);
	}
      }
      return img;
    }
  }

  /**
   * Load an URL into a byte array.
   */
  public static byte [] loadURL(URL url) throws IOException {
    byte [] buf     = new byte[1024];

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    BufferedInputStream   in   = new BufferedInputStream(url.openStream());
    int n;
    while ((n = in.read(buf)) > 0) {
      bout.write(buf, 0, n);
    }
    try { in.close(); } catch (Exception ignored) { } 
    return bout.toByteArray();
  }

  public String toString() {
    return 
      "TrayIconWrapper[" + 
      "trayIcon=" + trayIcon + 
      "windowsTrayIcon=" + windowsTrayIcon + 
      "]";
  }
}

