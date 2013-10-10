package org.knopflerfish.bundle.repositorymanager;

import org.knopflerfish.service.repositorymanager.RepositoryInfo;

interface RepositoryListener {

  void addingRepo(RepositoryInfo ri);

  void modifiedRepo(RepositoryInfo ri);

  void removedRepo(RepositoryInfo ri);

}
