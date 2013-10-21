package org.knopflerfish.bundle.desktopawt;

import org.osgi.framework.*;

public class Activator implements BundleActivator {
  static BundleContext bc;
  Desktop desktop;
  public void start(BundleContext bc) {
    this.bc = bc;

    desktop = new Desktop();
    desktop.open();
  }

  public void stop(BundleContext bc) {
    desktop.close();
    this.bc = null;
  }
}
