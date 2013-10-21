package org.knopflerfish.bundle.repository.index;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.indexer.ResourceAnalyzer;

public class Activator implements BundleActivator {
  ServiceRegistration<ResourceAnalyzer> sr;

  @Override
  public void start(BundleContext context) throws Exception {
    System.out.println("STARTED!!!");
    sr = context.registerService(ResourceAnalyzer.class, new KnopflerfishExtentions(), null);
    
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    System.out.println("STOPPED!!!");
    if(sr != null) sr.unregister();
    sr = null;
  }
  
}