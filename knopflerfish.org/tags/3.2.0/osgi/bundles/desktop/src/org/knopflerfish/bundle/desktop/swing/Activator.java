/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.knopflerfish.service.desktop.BundleFilter;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.service.remotefw.RemoteFramework;
import org.knopflerfish.util.Text;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;


public class Activator
  implements BundleActivator
{
  static public LogRef        log;

  static private BundleContext bc;
  static private BundleContext remoteBC;
  static public Desktop desktop;

  static Activator      myself;

  public static BundleContext getBC() {
    return bc;
  }

  static BundleFilter bundleFilter = null;

  public static void setBundleFilter(BundleFilter bf) {
    bundleFilter = bf;
  }

  public static Bundle[] getBundles() {
    BundleContext bc = getTargetBC();
    if (null==bc) {
      // May happen during stop, when processing events that has been queued.
      return new Bundle[0];
    }
    Bundle[] bl = bc.getBundles();
    if(bundleFilter != null) {
      ArrayList al = new ArrayList();
      for(int i = 0; bl != null && i < bl.length; i++) {
        if(bundleFilter.accept(bl[i])) {
          al.add(bl[i]);
        }
      }
      Bundle[] bl2 = new Bundle[al.size()];
      al.toArray(bl2);
      bl = bl2;
    }
    return bl;
  }

  /**
   * Get target BC for bundle control.
   *
   * <p>
   * This in preparation for the event of the desktop
   * being able to control a remote framework.
   * </p>
   */
  public static BundleContext getTargetBC() {
    if(remoteBC == null) {
      return getBC();
    }

    return remoteBC;
  }

  /**
   * Get all service references from the target BC.
   */
  public static ServiceReference[] getTargetBC_getServiceReferences()
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      try {
        return tbc.getServiceReferences(null, null);
      } catch (InvalidSyntaxException ise) {
        // Will not happen in this case!
      }
    }
    return null;
  }

  /**
   * Get specified service references from the target BC.
   */
  public static ServiceReference[]
    getTargetBC_getServiceReferences(final String clazz, final String filter)
    throws InvalidSyntaxException
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      return tbc.getServiceReferences(clazz, filter);
    }
    return null;
  }

  /**
   * Get default service reference from the target BC.
   */
  public static ServiceReference
    getTargetBC_getServiceReference(final String clazz)
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      return tbc.getServiceReference(clazz);
    }
    return null;
  }

  /**
   * Get default service reference from the target BC.
   */
  public static Object getTargetBC_getService(final ServiceReference sr)
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      return tbc.getService(sr);
    }
    return null;
  }

  /**
   * Unget a service from the target BC.
   */
  public static boolean getTargetBC_ungetService(final ServiceReference sr)
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      return tbc.ungetService(sr);
    }
    return false;
  }

  /**
   * Get a list with all bundles on the target BC.
   */
  public static Bundle[] getTargetBC_getBundles()
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      return tbc.getBundles();
    }
    return null;
  }

  /**
   * Get a specific bundle on the target BC.
   */
  public static Bundle getTargetBC_getBundle(final long bid)
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      return tbc.getBundle(bid);
    }
    return null;
  }

  /**
   * Get a property value from the target BC.
   */
  public static String getTargetBC_getProperty(final String key)
  {
    final BundleContext tbc = getTargetBC();
    if(null != tbc) {
      return tbc.getProperty(key);
    }
    return null;
  }

  // The key under which the KF-framework keeps a comma-separated list
  // of all framework property keys.
  public static final String fwPropKeysKey
    = "org.knopflerfish.framework.bundleprops.keys";

  public static Map getSystemProperties() {
    if(getTargetBC() != getBC()) {
      RemoteFramework rc = (RemoteFramework)remoteTracker.getService();
      return rc.getSystemProperties(getTargetBC());
    } else {
      // There is no method in BundleContext that enumerates
      // properties, thus use the set of keys from the system properties.
      final Properties props = System.getProperties();
      final Map map = new HashMap();

      for(Enumeration e = props.keys(); e.hasMoreElements();) {
        final String key = (String)e.nextElement();
        // We want local property values that applies to this instance
        // of the framework.
        final String val = Util.getProperty(key, (String) props.get(key));
        map.put(key, val);
      }

      // The Knopflerfish Framework has a framework property that
      // enumerates the keys all framework properties. Use this to
      // improve the properties map.
      final String fwPropKeys = getBC().getProperty(fwPropKeysKey);
      if (null!=fwPropKeys) {
        final StringTokenizer st = new StringTokenizer(fwPropKeys,",");
        while (st.hasMoreTokens()) {
          final String key = ((String) st.nextToken()).trim();
          final String val = getBC().getProperty(key);
          map.put(key, val);
        }
      }

      return map;
    }
  }

  static StartLevel getStartLevelService()
  {
    if (null!=desktop) {
      final ServiceTracker slTracker = (ServiceTracker) desktop.slTracker;
      if (null!=slTracker) {
        return (StartLevel) slTracker.getService();
      }
    }
    return null;
  }

  static Vector remoteHosts = new Vector() {
      {
        addElement("http://localhost:8080");
        addElement("http://localhost:8081");
        addElement("http://localhost:80");
      }
    };

  static String remoteHost = "";

  public static BundleContext openRemote(String host) {
    if(host.equals(remoteHost)) {
      return remoteBC;
    }
    RemoteFramework rc = (RemoteFramework)remoteTracker.getService();
    if(rc != null) {
      try {
        Activator.myself.closeDesktop();
        if("".equals(host) || "local".equals(host)) {
          remoteBC = null;
        } else {
          remoteBC = rc.connect(host);
        }
        remoteHost = host;
      } catch (Exception e) {
        log.error("Failed to connect to " + host);
      }

      Activator.myself.openDesktop();
    }
    return remoteBC;
  }


  static ServiceTracker remoteTracker;

  Map displayers = new HashMap();

  public void start(BundleContext _bc) {
    try {
      // try to move Mac OS menu bar
      if(null == System.getProperty("apple.laf.useScreenMenuBar")) {
        System.setProperty("apple.laf.useScreenMenuBar","true");
      }
      // try to enable swing antialiased text
      if(null == System.getProperty("swing.aatext")) {
        System.setProperty("swing.aatext","true");
      }
    } catch (Exception ignored) {
    }


    Activator.bc        = _bc;
    Activator.log       = new LogRef(bc);
    Activator.myself    = this;

    remoteTracker = new ServiceTracker(bc, RemoteFramework.class.getName(), null) {
        public Object addingService(ServiceReference sr) {
          Object obj = super.addingService(sr);
          try {  desktop.setRemote(true); } catch (Exception e) { }
          return obj;
        }
        public void removedService(ServiceReference sr, Object service) {
          try {  desktop.setRemote(false); } catch (Exception e) { }
          super.removedService(sr, service);
        }
      };
    remoteTracker.open();

    // Spawn to avoid race conditions in resource loading
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          openDesktop();
        }
      });
  }

  void openDesktop() {
    if(desktop != null) {
      Activator.log.warn("openDesktop: desktop already open");
      return;
    }

    desktop = new Desktop();
    desktop.start();

    DefaultSwingBundleDisplayer disp;

    ServiceRegistration reg;

    String[] dispClassNames = new String[] {
      LargeIconsDisplayer.class.getName(),
      GraphDisplayer.class.getName(),
      TableDisplayer.class.getName(),
      // SpinDisplayer.class.getName(),
      ManifestHTMLDisplayer.class.getName(),
      ClosureHTMLDisplayer.class.getName(),
      ServiceHTMLDisplayer.class.getName(),
      PackageHTMLDisplayer.class.getName(),
      LogDisplayer.class.getName(),
      EventDisplayer.class.getName(),
      PrefsDisplayer.class.getName(),
    };

    String dispsS = Util.getProperty("org.knopflerfish.desktop.displays", "").trim();

    if(dispsS != null && dispsS.length() > 0) {
      dispClassNames = Text.splitwords(dispsS, "\n\t ", '\"');
    }

    for(int i = 0; i < dispClassNames.length; i++) {
      String className = dispClassNames[i];
      try {
        Class       clazz = Class.forName(className);
        Constructor cons  = clazz.getConstructor(new Class[] { BundleContext.class });

        disp = (DefaultSwingBundleDisplayer)cons.newInstance(new Object[] { getTargetBC() });
        reg = disp.register();
        displayers.put(disp, reg);
      } catch (Exception e) {
        log.warn("Failed to create displayer " + className, e);
      }
    }

    // Must be executed even later, to allow for plugin comps to be ready.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          String defDisp = Util.getProperty("org.knopflerfish.desktop.display.main",
                                            LargeIconsDisplayer.NAME);
          // We really want this one to be displayed.
          desktop.bundlePanelShowTab(defDisp);

          int ix = desktop.detailPanel.indexOfTab("Manifest");
          if(ix != -1) {
            desktop.detailPanel.setSelectedIndex(ix);
          }
        }
      });
  }

  // Shutdown code that must exectue on the EDT
  private void closeDesktop0()
  {
    desktop.stop();
    desktop.theDesktop = null;
    desktop = null;
  }

  void closeDesktop() {
    try {

      if(desktop != null) {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
          closeDesktop0();
        } else {
          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
              public void run() {
                closeDesktop0();
              }
            });
        }
      }

      for(Iterator it = displayers.keySet().iterator(); it.hasNext();) {
        DefaultSwingBundleDisplayer disp
          = (DefaultSwingBundleDisplayer)it.next();
        ServiceRegistration reg = (ServiceRegistration)displayers.get(disp);

        disp.unregister();
      }
      displayers.clear();

      if(remoteBC != null) {
        RemoteFramework rc = (RemoteFramework)remoteTracker.getService();
        if(rc != null) {
          rc.disconnect(remoteBC);
        }
        remoteBC = null;
      }

    } catch (Exception e) {
      log.error("Failed to close desktop", e);
    }
  }

  public void stop(BundleContext bc) {
    try {
      closeDesktop();

      if(remoteTracker != null) {
        remoteTracker.close();
        remoteTracker = null;
      }

      Activator.bc     = null;
      Activator.myself = null;
      if (null!=Activator.log) {
        Activator.log.close();
        Activator.log = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
