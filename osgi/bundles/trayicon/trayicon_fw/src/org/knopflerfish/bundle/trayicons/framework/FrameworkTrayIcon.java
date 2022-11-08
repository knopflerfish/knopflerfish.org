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

package org.knopflerfish.bundle.trayicons.framework;

import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.startlevel.FrameworkStartLevel;

public class FrameworkTrayIcon
{
  final FrameworkStartLevel frameworkStartLevel;
  CheckboxMenuItem[] slsItems = new CheckboxMenuItem[22];
  static Object trayIcon;
  static Object systemTray;
  static Class<?> trayIconClass;
  static Class<?> systemTrayClass;
  static FrameworkTrayIcon frameworkTrayIcon;

  public FrameworkTrayIcon() throws UnsupportedOperationException
  {
    final StringBuilder toolTipText = new StringBuilder("Knopflerfish OSGi");
    final String servicePlatformId =
      Activator.bc.getProperty("org.osgi.provisioning.spid");
    if (null != servicePlatformId && 0 < servicePlatformId.length()) {
      toolTipText.append(" (").append(servicePlatformId).append(")");
    }

    try {
      trayIconClass = Class.forName("java.awt.TrayIcon");
      final Constructor<?> con =
        trayIconClass.getDeclaredConstructor(Image.class, String.class);
      trayIcon =
        con.newInstance(Toolkit.getDefaultToolkit()
                .getImage(FrameworkTrayIcon.class.getResource(getIconForOS())),
            toolTipText.toString());

      final Method m =
        trayIconClass.getDeclaredMethod("setPopupMenu", PopupMenu.class);
      m.invoke(trayIcon, makeMenu());

      frameworkStartLevel =
        Activator.bc.getBundle(0L).adapt(FrameworkStartLevel.class);

      updateStartLevelItems();

      Activator.bc.addFrameworkListener(ev -> {
        if (FrameworkEvent.STARTLEVEL_CHANGED == ev.getType()
            || FrameworkEvent.STARTED == ev.getType()) {
          updateStartLevelItems();
        }
      });
    } catch (final Exception e) {
      Activator.log.error("Failed to create FrameworkTrayIcon: " + e, e);
      throw new UnsupportedOperationException(e.getMessage());
    }
  }

  public static FrameworkTrayIcon getFrameworkTrayIcon()
      throws UnsupportedOperationException
  {
    if (frameworkTrayIcon != null) {
      return frameworkTrayIcon;
    }

    try {
      if (systemTray == null) {
        systemTrayClass = Class.forName("java.awt.SystemTray");
        Method m =
          systemTrayClass.getDeclaredMethod("isSupported");
        final Boolean is_supported = (Boolean) m.invoke(null, (Object[]) null);
        if (!is_supported) {
          throw new UnsupportedOperationException("System Tray not supported");
        }

        m = systemTrayClass.getDeclaredMethod("getSystemTray");
        systemTray = m.invoke(null, (Object[]) null);
        frameworkTrayIcon = new FrameworkTrayIcon();
        return frameworkTrayIcon;
      }
    } catch (final UnsupportedOperationException e) {
      throw e;
    } catch (final Exception e) {
      Activator.log.error("Error in SystemTray invocation: " + e);
      throw new UnsupportedOperationException(e.getMessage());
    }
    return null; // dummy
  }

  void show()
  {
    Activator.log.info("Showing tray icon");
    try {
      final Method m =
        systemTrayClass.getMethod("add", trayIconClass);
      m.invoke(systemTray, trayIcon);
    } catch (final Exception e) {
      Activator.log.error("Failed to add TrayIcon to SystemTray", e);
    }
  }

  void close()
  {
    try {
      Activator.log.info("Removing tray icon");
      final Method m =
        systemTrayClass.getMethod("remove", trayIconClass);
      m.invoke(systemTray, trayIcon);
    } catch (final Exception e) {
      Activator.log.error("Failed to remove TrayIcon from SystemTray", e);
    }

    // unregister();
  }

  PopupMenu makeMenu()
  {
    final PopupMenu popup = new PopupMenu();

    popup.add(new MenuItem("Shutdown framework") {
      /**
       *
       */
      private static final long serialVersionUID = 1L;

      {
        addActionListener(e -> shutdown());
      }
    });

    final Menu slsMenu = new Menu("Start level");
    for (int i = 1; i < slsItems.length - 1; i++) {
      final int level = i;
      slsItems[i] = new CheckboxMenuItem("" + i) {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        {
          addItemListener(e -> setStartLevel(level));
        }
      };

      slsMenu.add(slsItems[i]);
    }

    popup.add(slsMenu);
    return popup;
  }

  void updateStartLevelItems()
  {
    if (frameworkStartLevel == null) {
      Activator.log.warn("No start level service found");
      return;
    }

    final int level = frameworkStartLevel.getStartLevel();

    for (int i = 1; i < slsItems.length - 1; i++) {
      slsItems[i].setState(level == i);
    }
  }

  void setStartLevel(int n)
  {
    if (frameworkStartLevel == null) {
      Activator.log.warn("No start level service found");
      return;
    }
    frameworkStartLevel.setStartLevel(n);
  }

  void shutdown()
  {
    try {
      final Bundle systemBundle = Activator.bc.getBundle(0);
      systemBundle.stop();
    } catch (final Exception e) {
      Activator.log.error("Failed to shutdown", e);
    }
  }

  @SuppressWarnings("unused")
  static String getProperty(String key, String def)
  {
    final String sValue = Activator.bc.getProperty(key);
    if (null != sValue && 0 < sValue.length()) {
      return sValue;
    }
    return def;
  }

  static String getIconForOS()
  {
    if (System.getProperty("os.name", "").toLowerCase().startsWith("mac os x")) {
      return "/kfbones-rev-tr-22x22.png";
    } else {
      return "/kf_16x16.png";
    }
  }

}
