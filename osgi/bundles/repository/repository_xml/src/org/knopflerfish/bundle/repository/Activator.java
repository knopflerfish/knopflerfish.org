package org.knopflerfish.bundle.repository;

import java.util.Dictionary;
import java.util.Hashtable;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class Activator implements BundleActivator {
  final static String REPOSITORY_XML_PID = "org.knopflerfish.repository.xml.MSF";
  final static String REPOSITORY_XML_URL = "org.knopflerfish.repository.xml.url";
  BundleContext bc;
  FactoryImpl factory;
  ServiceRegistration<XmlBackedRepositoryFactory> sr;
  ServiceRegistration<ManagedServiceFactory> msfr;
  
  @Override
  public void start(BundleContext bc) throws Exception {
    this.bc  = bc;
    factory = new FactoryImpl(bc);
    
    String url = bc.getProperty(REPOSITORY_XML_URL);
    if(url != null && !"".equals(url)) {
      factory.create(url, null);
    }
    
    sr = bc.registerService(XmlBackedRepositoryFactory.class, factory, null);
    
    
    Hashtable<String, String> h = new Hashtable<String, String>();
    h.put(Constants.SERVICE_PID, REPOSITORY_XML_PID);
    msfr = bc.registerService(ManagedServiceFactory.class, new MSF(), h);
  }



  @Override
  public void stop(BundleContext bc) throws Exception {
    if(msfr != null) {
      msfr.unregister();
    }
    if(sr != null) {
      sr.unregister();
    }
    factory.destroyAll();
  }
  
  class MSF implements ManagedServiceFactory {

    @Override
    public String getName() {
      return "Xml-backed Repository Factory";
    }

    @Override
    public synchronized void updated(String pid, Dictionary<String, ?> p)
        throws ConfigurationException {

        try {
          factory.create((String)p.get("url"), pid);
        } catch (Exception e) {
          e.printStackTrace();
        }  
    }

    @Override
    public synchronized void deleted(String pid) {
      factory.destroy(pid);
    }
    
  }
}
