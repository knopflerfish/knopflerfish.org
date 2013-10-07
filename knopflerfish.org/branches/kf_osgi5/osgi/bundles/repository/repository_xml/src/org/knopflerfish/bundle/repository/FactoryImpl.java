package org.knopflerfish.bundle.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

import org.knopflerfish.bundle.repository.xml.RepositoryXmlParser;
import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class FactoryImpl implements XmlBackedRepositoryFactory {
  private final BundleContext bc;
  private final HashMap<Object, ServiceRegistration<Repository>> repositoryRegistrations = new HashMap<Object, ServiceRegistration<Repository>>();

  FactoryImpl(BundleContext bc) {
    this.bc = bc;
  }
  
  @Override
  public Object create(String url) throws Exception {
    if(url != null && !"".equals(url)) {
      InputStream is = new URL(url).openStream();
      Collection<Resource> rs = RepositoryXmlParser.parse(is);
      RepositoryXmlParser.debug(rs);
      if(!rs.isEmpty()) {
        RepositoryImpl repo = new RepositoryImpl(bc, rs);
        Hashtable<String, String> h = new Hashtable<String, String>();
        h.put("service.pid", "org.knopflerfish.repository.xml");
        //h.put("service.description", "org.knopflerfish.repository.xml");
        //h.put("repository.url", url);
        ServiceRegistration<Repository> sr = bc.registerService(Repository.class, repo, h);
        repositoryRegistrations.put(url, sr);
        return url;
      }
    }
    return null;
  }

  @Override
  public void destroy(Object handle) {
    ServiceRegistration<Repository> sr = repositoryRegistrations.remove(handle);
    if(sr != null) {
      sr.unregister();
    }
  }
  
  void destroyAll() {
    for(Object h : repositoryRegistrations.keySet()) {
      destroy(h);
    }
  }

}
