package org.knopflerfish.bundle.threadio;

import java.util.Hashtable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.threadio.ThreadIO;

public class Activator implements BundleActivator {

  static BundleContext bc;

  ThreadIOFactory     factory;
  ServiceRegistration factoryReg;

  public void start(BundleContext bc) {
    this.bc = bc;
    factory = new ThreadIOFactory();
    Hashtable props = new Hashtable();
    factoryReg = bc.registerService(ThreadIO.class.getName(), factory, props);
  }
  
  public void stop(BundleContext bc) {
    factory.stop();
    factory    = null;
    factoryReg = null;
    this.bc = null;    
  }

}
