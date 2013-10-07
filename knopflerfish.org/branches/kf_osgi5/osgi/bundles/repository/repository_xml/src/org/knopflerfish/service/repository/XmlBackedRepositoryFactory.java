package org.knopflerfish.service.repository;

public interface XmlBackedRepositoryFactory {
  public Object create(String url, Object handle) throws Exception;
  public void destroy(Object handle) throws Exception;
}
