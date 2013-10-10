package org.knopflerfish.service.repository;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;

public interface XmlBackedRepositoryFactory {
  // TODO add possibility to make repo permanent by using persistent CM config.
  public ServiceReference<Repository> create(String url, Dictionary<String, ?> properties, Object handle) throws Exception;
  public void destroy(Object handle) throws Exception;
}
