package org.knopflerfish.bundle.bundleS_test;

import org.osgi.framework.*;
import org.knopflerfish.service.bundleS_test.*;
// import org.knopflerfish.service.console.*;

public class BundleActivator implements org.osgi.framework.BundleActivator {
  BundleContext bc;
  BundS s;

  public void start(BundleContext bc) {
    this.bc = bc;
    try {
       s = new BundS (bc);
    }

   catch (Throwable ru) {
      System.out.println ("Exception " + ru + " in BundleS start");
      ru.printStackTrace();
   }

  }

  public void stop(BundleContext bc) {
    s=null;
  }
}
