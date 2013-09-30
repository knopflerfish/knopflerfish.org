package org.knopflerfish.bundle.repository;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.Hashtable;

import org.knopflerfish.bundle.repository.xml.RepositoryXmlParser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class Activator implements BundleActivator {
  final static String REPOSITORY_XML = "org.knopflerfish.repository.xml";
  BundleContext bc;
  ServiceRegistration<Repository> sr = null;
  @Override
  public void start(BundleContext bc) throws Exception {
    this.bc  = bc;
    String f = bc.getProperty(REPOSITORY_XML);
    if(f != null && !"".equals(f)) {
      FileInputStream fs = new FileInputStream(f);
      Collection<Resource> rs = RepositoryXmlParser.parse(fs);
      if(!rs.isEmpty()) {
        RepositoryImpl repo = new RepositoryImpl(bc, rs);
        Hashtable<String, String> h = new Hashtable<String, String>();
        h.put("service.pid", "org.knopflerfish.repository.xml");
        sr = bc.registerService(Repository.class, repo, h);
      }
    }
  }



  @Override
  public void stop(BundleContext bc) throws Exception {
    if(sr != null) {
      sr.unregister();
    }
  }

}
