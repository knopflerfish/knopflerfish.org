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

package org.knopflerfish.bundle.trayicons.framework;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;
import org.osgi.service.startlevel.*;
import java.awt.event.*;

import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;

import java.io.File;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.service.trayicon.*;

public class FrameworkTrayIcon extends DefaultTrayIcon {
  

  ServiceTracker      slsTracker;
  JPopupMenu          popup;  
  JCheckBoxMenuItem[] slsItems = new JCheckBoxMenuItem[22];

  String initMsg = System.getProperty("org.knopflerfish.service.trayicon.fw.initmsg",
				      "The Knopflerfish OSGi framework is initialized");
  
  String restartMsg = System.getProperty("org.knopflerfish.service.trayicon.fw.restartmsg",
					 "The Knopflerfish OSGi framework is restarted");
  
  public FrameworkTrayIcon() {
    super(Activator.bc,
	  FrameworkTrayIcon.class.getName(),
	  System.getProperty("org.knopflerfish.service.trayicon.fw.title",
			     "Knopflerfish OSGi"),
	  FrameworkTrayIcon.class.getResource("/fish16x16.gif"));
  }
  
  void open() {
    slsTracker = new ServiceTracker(Activator.bc, 
				    StartLevel.class.getName(), null);
    slsTracker.open();

    makeMenu();

    updateStartLevelItems();

    Activator.bc.addFrameworkListener(new FrameworkListener() {
	public void frameworkEvent(FrameworkEvent ev) {
	  if(FrameworkEvent.STARTLEVEL_CHANGED  == ev.getType()) {
	    updateStartLevelItems();
	  }
	}
      });

    register();
  }

  void close() {
    slsTracker.close();
    unregister();
  }

  public JPopupMenu getTrayJPopupMenu() {
    return popup;
  }
  
  public String getStartupMessage() {
    File f = Activator.bc.getDataFile("firststart");
    if(f.exists()) {
      return restartMsg;
    } else {
      try {
	f.createNewFile();
      } catch (Exception e) {
	Activator.log.error("Failed to create file=" + f);
      }
      return initMsg;
    }
  }

  void makeMenu() {
    popup = new JPopupMenu();
    
    popup.add(new JMenuItem("Shutdown framework") {
	{
	  addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent e) {
		shutdown();
	      }
	    });
	}
      });

    JMenu slsMenu = new JMenu("Start level");
    for(int i = 1; i < slsItems.length-1; i++) {
      final int level = i;
      slsItems[i] = new JCheckBoxMenuItem("" + i) {
	  {
	    addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  setStartLevel(level);
		}
	      });
	  }
	};
      
      slsMenu.add(slsItems[i]);    
    }
    
    popup.add(slsMenu);
  }


  void updateStartLevelItems() {
    StartLevel sls = (StartLevel)slsTracker.getService();
    if(sls == null) {
      Activator.log.warn("No start level service found");
      return;      
    }

    int level = sls.getStartLevel();
    
    for(int i = 1; i < slsItems.length-1; i++) {
      slsItems[i].setState(level == i);
    }
  }


  void setStartLevel(int n) {
    StartLevel sls = (StartLevel)slsTracker.getService();
    if(sls == null) {
      Activator.log.warn("No start level service found");
      return;
    }

    sls.setStartLevel(n);
  }

  void shutdown() {
    try {
      Bundle systemBundle = Activator.bc.getBundle(0);
      systemBundle.stop();
    } catch (Exception e) {
      Activator.log.error("Failed to shutdown", e);
    }
  }

}
