package org.knopflerfish.bundle.repositorymanager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.SortedSet;
import java.util.TreeSet;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class Repositories implements ServiceTrackerCustomizer<Repository, Repository>
{

  private ServiceTracker<Repository, Repository> repoTracker = null;
  private ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory> xmlRepoFactoryTracker = null;
  final private ArrayList<RepositoryListener> listeners = new ArrayList<RepositoryListener>(2);


  Repositories(BundleContext bc) {
    repoTracker = new ServiceTracker<Repository, Repository>(bc, Repository.class.getName(), null);
    repoTracker.open();
    xmlRepoFactoryTracker = new ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory>(bc, XmlBackedRepositoryFactory.class.getName(), null);
    xmlRepoFactoryTracker.open();
  }

  RepositoryInfo addXmlRepository(String url, Dictionary<String,?> properties) throws Exception {
    XmlBackedRepositoryFactory xf = xmlRepoFactoryTracker.getService();
    if (xf != null) {
      ServiceReference<Repository> sr = xf.create(url, properties, null);
      if (sr != null) {
        return new RepositoryInfo(sr);
      }
    }
    return null;
  }

  RepositoryInfo get(long id) {
    final ServiceReference<Repository>[] srs = repoTracker.getServiceReferences();
    if (srs != null) {
      for (ServiceReference<Repository> sr : srs) {
        RepositoryInfo ri = new RepositoryInfo(sr);
        if (ri.getId() == id) {
          return ri;
        }
      }
    }
    return null;
  }

  TreeSet<RepositoryInfo> getAll() {
    TreeSet<RepositoryInfo> res = new TreeSet<RepositoryInfo>();
    final ServiceReference<Repository>[] srs = repoTracker.getServiceReferences();
    if (srs != null) {
      for (ServiceReference<Repository> sr : srs) {
        res.add(new RepositoryInfo(sr));
      }
    }
    return res;
  }

  Repository getRepository(RepositoryInfo ri) {
    return repoTracker.getService(ri.getServiceReference());
  }

  void addListener(RepositoryListener rl) {
    listeners.add(rl);
  }

  void removeListener(RepositoryListener rl) {
    listeners.remove(rl);
  }

  @Override
  public synchronized Repository addingService(ServiceReference<Repository> sr) {
    RepositoryInfo ri = new RepositoryInfo(sr);
    for (RepositoryListener rl : listeners) {
      rl.addingRepo(ri);
    }
    return null;
  }

  @Override
  public synchronized void modifiedService(ServiceReference<Repository> sr, Repository service) {
    RepositoryInfo ri = new RepositoryInfo(sr);
    for (RepositoryListener rl : listeners) {
      rl.modifiedRepo(ri);
    }
  }

  @Override
  public synchronized void removedService(ServiceReference<Repository> sr, Repository service) {
    RepositoryInfo ri = new RepositoryInfo(sr);
    for (RepositoryListener rl : listeners) {
      rl.removedRepo(ri);
    }
  }

}
