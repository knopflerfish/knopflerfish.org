package org.knopflerfish.bundle.repository;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;

import org.knopflerfish.bundle.repository.xml.RepositoryXmlParser;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class Activator implements BundleActivator {
  final static String REPOSITORY_XML_URL = "org.knopflerfish.repository.xml.url";
  BundleContext bc;
  ServiceRegistration<Repository> sr = null;
  @Override
  public void start(BundleContext bc) throws Exception {
    this.bc  = bc;
    String url = bc.getProperty(REPOSITORY_XML_URL);
    if(url != null && !"".equals(url)) {
      // TODO: Put in thread
      InputStream is = new URL(url).openStream();
      Collection<Resource> rs = RepositoryXmlParser.parse(is);
      RepositoryXmlParser.debug(rs);
      if(!rs.isEmpty()) {
        RepositoryImpl repo = new RepositoryImpl(bc, rs);
        Hashtable<String, String> h = new Hashtable<String, String>();
        h.put("service.pid", "org.knopflerfish.repository.xml");
        //h.put("service.description", "org.knopflerfish.repository.xml");
        //h.put("repository.url", url);
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
