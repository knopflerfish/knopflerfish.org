package org.knopflerfish.bundle.test.memtest.lifecycle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Bundle;


public class Activator implements BundleActivator, Runnable {

  // private fields

  private static final String TEST_BUNDLE_LOCATION_KEY = "org.knopflerfish.bundle.test.memtest.lifecycle.bundle";
  private static final String TEST_BUNDLE_LOCATION = System.getProperty(TEST_BUNDLE_LOCATION_KEY);

  private BundleContext context;
  private Thread thread;


  // implements BundleActivator

  public void start(BundleContext context) {
    this.context = context;

    thread = new Thread(this);
    thread.start();
  }

  public void stop(BundleContext context) {
    thread = null;
  }


  // implements Runnable

  public void run() {
    Thread currentThread = Thread.currentThread();
    try {
      while (currentThread == thread) {
        try {
          Bundle bundle = context.installBundle(TEST_BUNDLE_LOCATION);
          Thread.sleep(100);
          bundle.start();
          Thread.sleep(100);
          bundle.stop();
          Thread.sleep(100);
          bundle.uninstall();
          Thread.sleep(100);
        } catch (InterruptedException ignore) { }
      }
    } catch (BundleException be) {
      be.printStackTrace(System.err);
    }
  }

} // Activator
