package org.knopflerfish.test.framework;

import org.knopflerfish.framework.Main;
import org.osgi.framework.*;
import java.io.*;
import java.util.*;

public class Activator implements BundleActivator {

  public void start(BundleContext bc) {
    TestFW.tester.log("bundle", "started " +
                      "(vendor=" +
                      bc.getProperty(Constants.FRAMEWORK_VENDOR) +
                      " version=" +
                      bc.getProperty(Constants.FRAMEWORK_VERSION) +
                      ")");

    synchronized(TestFW.tester.notifier) {
      //      TestFW.tester.log("bundle", "send notify");
      TestFW.tester.notifier.notifyAll();
    }
  }

  public void stop(BundleContext bc) {
  }
}
