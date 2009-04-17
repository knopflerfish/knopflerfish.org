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
import org.osgi.util.tracker.*;
import org.osgi.service.packageadmin.*;
import org.knopflerfish.service.log.*;

import java.util.*;
import org.knopflerfish.service.desktop.*;

import org.knopflerfish.service.remotefw.RemoteFramework;

public class Activator implements BundleActivator {

  static public LogRef        log;

  static private BundleContext bc;
  static private BundleContext remoteBC;
  static public Desktop desktop;

  static ServiceTracker pkgTracker;
  static Activator      myself;

  public static BundleContext getBC() {
    return bc;
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

  public static Map getSystemProperties() {
    if(getTargetBC() != getBC()) {
      RemoteFramework rc = (RemoteFramework)remoteTracker.getService();
      return rc.getSystemProperties(getTargetBC());
    } else {
      Properties props = System.getProperties();
      Map map = new HashMap();

      for(Enumeration e = props.keys(); e.hasMoreElements();) {
        String key = (String)e.nextElement();
        String val = (String)props.get(key);
        map.put(key, val);
      }
      return map;
    }
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
    this.bc        = _bc;
    this.log       = new LogRef(bc);
    this.myself    = this;

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

    pkgTracker = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
    pkgTracker.open();

    // Spawn to avoid race conditions in resource loading
    Thread t = new Thread() {
        public void run() {
          openDesktop();
        }
      };
    t.start();
  }

  void openDesktop() {
    if(desktop != null) {
      System.out.println("openDesktop: desktop already open");
      return;
    }

    desktop = new Desktop();
    desktop.start();

    DefaultSwingBundleDisplayer disp;

    ServiceRegistration reg;

    // bundle displayers
    disp = new LargeIconsDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    disp = new TimeLineDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    disp = new TableDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    disp = new SpinDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    // detail displayers
    disp = new ManifestHTMLDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    disp = new ClosureHTMLDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    disp = new ServiceHTMLDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    disp = new PackageHTMLDisplayer(getTargetBC());
    disp.open();
    reg = disp.register();
    displayers.put(disp, reg);

    //if(getBC() == getTargetBC()) {
      disp = new LogDisplayer(getTargetBC());
      disp.open();
      reg = disp.register();
      displayers.put(disp, reg);
    //}


    // We really want this one to be displayed.
    desktop.bundlePanel.showTab("Large Icons");
    int ix = desktop.detailPanel.indexOfTab("Manifest");
    if(ix != -1) {
      desktop.detailPanel.setSelectedIndex(ix);
    }
  }

  void closeDesktop() {
    try {

      if(desktop != null) {
        desktop.stop();
        desktop = null;
      }

      for(Iterator it = displayers.keySet().iterator(); it.hasNext();) {
        DefaultSwingBundleDisplayer disp
          = (DefaultSwingBundleDisplayer)it.next();
        ServiceRegistration reg = (ServiceRegistration)displayers.get(disp);

        disp.unregister();
        disp.close();
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

      if(log != null) {
        log = null;
      }

      if(remoteTracker != null) {
        remoteTracker.close();
        remoteTracker = null;
      }

      if(pkgTracker != null) {
        pkgTracker.close();
        pkgTracker = null;
      }

      this.bc     = null;
      this.myself = null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
