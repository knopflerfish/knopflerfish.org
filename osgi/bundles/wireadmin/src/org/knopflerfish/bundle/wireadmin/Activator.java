package org.knopflerfish.bundle.wiredmin;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

  static BundleContext bc;

  public void start(BundleContext bc) {

    this.bc = bc;
  }

  public void stop(BundleContext bc) {
    this.bc = null;
  }
}
