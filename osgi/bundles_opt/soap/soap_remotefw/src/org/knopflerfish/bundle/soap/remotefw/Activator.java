package org.knopflerfish.bundle.soap.remotefw;

import org.osgi.framework.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;

public class Activator implements BundleActivator {
  
  public static BundleContext bc;
  static LogRef        log;

  RemoteFWImpl remoteFW;

  public void start(BundleContext bc) {
    this.bc = bc;
    log = new LogRef(bc);

    remoteFW = new RemoteFWImpl();
    remoteFW.start();
  }

  public void stop(BundleContext bc) {
    remoteFW.stop();

    this.log = null;
    this.bc  = null;
  }
}
