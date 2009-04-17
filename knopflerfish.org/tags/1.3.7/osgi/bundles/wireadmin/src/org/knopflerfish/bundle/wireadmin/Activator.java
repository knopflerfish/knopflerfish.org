package org.knopflerfish.bundle.wireadmin;

import org.osgi.framework.*;

public class Activator implements BundleActivator {

  static BundleContext bc;

  public void start(BundleContext bc) {

    System.out.println("WireAdmin implementation not yet done");
    this.bc = bc;
  }

  public void stop(BundleContext bc) {
    this.bc = null;
  }
}
