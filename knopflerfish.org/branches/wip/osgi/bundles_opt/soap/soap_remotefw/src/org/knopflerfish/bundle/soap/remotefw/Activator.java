package org.knopflerfish.bundle.soap.remotefw;

import org.osgi.framework.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;

import org.knopflerfish.bundle.soap.remotefw.client.*;
import org.knopflerfish.service.soap.remotefw.*;

public class Activator implements BundleActivator {
  
  public static BundleContext bc;
  static LogRef        log;

  RemoteFWServer remoteFW;

  public void start(BundleContext bc) {
    this.bc = bc;
    log = new LogRef(bc);

    remoteFW = new RemoteFWServer();
    remoteFW.start();

    RemoteConnectionImpl rc = new RemoteConnectionImpl();

    bc.registerService(RemoteConnection.class.getName(),
		       rc,
		       new Hashtable());
  }

  public void stop(BundleContext bc) {
    remoteFW.stop();

    this.log = null;
    this.bc  = null;
  }
}
