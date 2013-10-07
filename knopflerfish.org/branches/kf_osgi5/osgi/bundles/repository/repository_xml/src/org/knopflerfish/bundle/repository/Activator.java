package org.knopflerfish.bundle.repository;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
  final static String REPOSITORY_XML_URL = "org.knopflerfish.repository.xml.url";
  BundleContext bc;
  FactoryImpl factory;
  ServiceRegistration<XmlBackedRepositoryFactory> sr;
  
  @Override
  public void start(BundleContext bc) throws Exception {
    this.bc  = bc;
    factory = new FactoryImpl(bc);
    
    String url = bc.getProperty(REPOSITORY_XML_URL);
    if(url != null && !"".equals(url)) {
      factory.create(url);
    }
    
    sr = bc.registerService(XmlBackedRepositoryFactory.class, factory, null);
  }



  @Override
  public void stop(BundleContext bc) throws Exception {
    if(sr != null) {
      sr.unregister();
    }
    factory.destroyAll();
  }

}
