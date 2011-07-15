/*
 * Copyright (c) 2004-2010, KNOPFLERFISH project
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

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.Class;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;
import org.osgi.service.startlevel.*;

import java.awt.event.*;
// import java.awt.TrayIcon;
import java.awt.Toolkit;

import java.awt.PopupMenu;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.CheckboxMenuItem;
import java.awt.Image;

import java.io.File;
import org.knopflerfish.service.log.LogRef;

public class FrameworkTrayIcon {

  ServiceTracker      slsTracker;
  CheckboxMenuItem[] slsItems = new CheckboxMenuItem[22];
  static Object trayIcon;
  static Object systemTray;
  static Class trayIconClass;
  static Class systemTrayClass;
  static FrameworkTrayIcon frameworkTrayIcon;

  public FrameworkTrayIcon() throws UnsupportedOperationException {
    final StringBuffer toolTipText = new StringBuffer("Knopflerfish OSGi");
    final String servicePlatformId
      = Activator.bc.getProperty("org.osgi.provisioning.spid");
    if (null!=servicePlatformId && 0<servicePlatformId.length()) {
      toolTipText.append(" (").append(servicePlatformId).append(")");
    }

    try {
      trayIconClass = Class.forName("java.awt.TrayIcon");
      Constructor con = trayIconClass.getDeclaredConstructor(new Class[] {Image.class, String.class});
      trayIcon = con.newInstance(new Object[] {
          Toolkit.getDefaultToolkit().getImage(FrameworkTrayIcon.class.getResource(getIconForOS())),
          toolTipText.toString()});

      Method m = trayIconClass.getDeclaredMethod("setPopupMenu", new Class[] {PopupMenu.class});
      m.invoke(trayIcon, new Object[] {makeMenu()});

      slsTracker = new ServiceTracker(Activator.bc,
                                      StartLevel.class.getName(), null);
      slsTracker.open();


      updateStartLevelItems();

      Activator.bc.addFrameworkListener(new FrameworkListener() {
          public void frameworkEvent(FrameworkEvent ev) {
            if(FrameworkEvent.STARTLEVEL_CHANGED  == ev.getType() ||
               FrameworkEvent.STARTED  == ev.getType()) {
              updateStartLevelItems();
            }
          }
        });
    }
    catch (Exception e) {
      Activator.log.error("Failed to create FrameworkTrayIcon: "+e, e);
      throw new UnsupportedOperationException(e.getMessage());
    }
  }

  public static FrameworkTrayIcon getFrameworkTrayIcon() throws UnsupportedOperationException {
    if (frameworkTrayIcon != null)
      return frameworkTrayIcon;

    try {
      if (systemTray == null) {
        systemTrayClass  = Class.forName("java.awt.SystemTray");
        Method m = systemTrayClass.getDeclaredMethod("isSupported", null);
        Boolean is_supported = (Boolean)m.invoke(null, null);
        if (!is_supported.booleanValue())
          throw new UnsupportedOperationException("System Tray not supported");

        m = systemTrayClass.getDeclaredMethod("getSystemTray", null);
        systemTray = m.invoke(null,null);
        frameworkTrayIcon = new FrameworkTrayIcon();
        return frameworkTrayIcon;
      }
    }
    catch (UnsupportedOperationException e) {
      throw e;
    }
    catch (Exception e) {
      Activator.log.error("Error in SystemTray invokation: " + e);
      throw new UnsupportedOperationException(e.getMessage());
    }
    return null; // dummy
  }


  void show() {
    Activator.log.info("Showing tray icon");
    try {
      Method m = systemTrayClass.getMethod("add", new Class[] {trayIconClass});
      m.invoke(systemTray, new Object[] {trayIcon});
    }
    catch (Exception e){
      Activator.log.error("Failed to add TrayIcon to SystemTray", e);
    }
  }

  void close() {
    try {
      Activator.log.info("Removing tray icon");
      Method m = systemTrayClass.getMethod("remove", new Class[] {trayIconClass});
      m.invoke(systemTray, new Object[] {trayIcon});
    }
    catch (Exception e){
      Activator.log.error("Failed to remove TrayIcon from SystemTray", e);
    }

    slsTracker.close();
    // unregister();
  }

  PopupMenu makeMenu() {
    final PopupMenu popup = new PopupMenu();

    popup.add(new MenuItem("Shutdown framework") {
        {
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                shutdown();
              }
            });
        }
      });

    final Menu slsMenu = new Menu("Start level");
    for(int i = 1; i < slsItems.length-1; i++) {
      final int level = i;
      slsItems[i] = new CheckboxMenuItem("" + i) {
          {
            addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                  setStartLevel(level);
                }
              });
          }
        };

      slsMenu.add(slsItems[i]);
    }

    popup.add(slsMenu);
    return popup;
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

  static String getProperty(String key, String def)
  {
    String sValue = Activator.bc.getProperty(key);
    if (null!=sValue && 0<sValue.length()) {
      return sValue;
    }
    return def;
  }

  static String getIconForOS() {
    if (System.getProperty("os.name", "").toLowerCase().startsWith("mac os x"))
      return "/kfbones-rev-tr-22x22.png";
    else
      return "/kf_16x16.png";
  }

}