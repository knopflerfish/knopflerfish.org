package org.knopflerfish.bundle.x10.tj;

import org.osgi.framework.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.cm.*;
import java.util.*;

public class Activator implements BundleActivator {
  static BundleContext bc;
  static LogRef        log;

  Config config;
  public void start(BundleContext bc) {
    this.bc  = bc;
    this.log = new LogRef(bc);

    config = new Config();

    Hashtable props = new Hashtable();
    props.put("service.pid", "org.knopflerfish.tjx10.controllers");
    bc.registerService(ManagedServiceFactory.class.getName(),
		       config, 
		       props);
		       
  }

  public void stop(BundleContext bc) {
    config.stop();
    this.log = null;
    this.bc  = null;
  }
}
