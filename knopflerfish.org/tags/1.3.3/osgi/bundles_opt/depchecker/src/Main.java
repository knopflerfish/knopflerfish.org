/*
 * Copyright (c) 2003-2004, Goeminne Nico
 * All rights reserved.
 *
 * Granted permission to the KNOPFLERFISH Project to use this source code.
 *
 */

import org.osgi.framework.*;

public class Main implements BundleActivator {

  private CheckerBundleEventListener listener = null;

  public void start(BundleContext bundleContext) {
    listener = new CheckerBundleEventListener(bundleContext);
    bundleContext.addBundleListener(listener);
  }

  public void stop(BundleContext bundleContext) {
    bundleContext.removeBundleListener(listener);
    listener = null;
  }
}