package org.knopflerfish.service.repository;

public interface XmlBackedRepositoryFactory {
  public Object create(String url) throws Exception;
  public void destroy(Object handle) throws Exception;
}
