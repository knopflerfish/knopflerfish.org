package org.knopflerfish.bundle.soap.remotefw;

import org.osgi.framework.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;

import org.knopflerfish.bundle.soap.remotefw.client.*;
import org.knopflerfish.service.remotefw.*;

public class Activator implements BundleActivator {
  
  public static BundleContext bc;
  static LogRef        log;

  RemoteFWServer remoteFW;

  public void start(BundleContext bc) {
    this.bc = bc;
    log = new LogRef(bc);

    if("true".equals(System.getProperty("org.knopflerfish.soap.remotefw.server", "true"))) {
      remoteFW = new RemoteFWServer();
      remoteFW.start();
    }
    
    if("true".equals(System.getProperty("org.knopflerfish.soap.remotefw.client", "true"))) {
      RemoteFrameworkImpl rc = new RemoteFrameworkImpl();
      
      bc.registerService(RemoteFramework.class.getName(),
			 rc,
			 new Hashtable());
    }
  }
  
  public void stop(BundleContext bc) {
    if(remoteFW != null) {
      remoteFW.stop();
      remoteFW = null;
    } 

    this.log = null;
    this.bc  = null;
  }
}
