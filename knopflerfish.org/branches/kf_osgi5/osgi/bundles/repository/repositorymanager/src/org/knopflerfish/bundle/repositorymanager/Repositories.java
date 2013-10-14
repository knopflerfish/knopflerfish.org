/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knopflerfish.bundle.repositorymanager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.TreeSet;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.knopflerfish.service.repositorymanager.RepositoryManager;
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
  private int size_offset  = 0;


  Repositories(BundleContext bc) {
    repoTracker = new ServiceTracker<Repository, Repository>(bc, Repository.class.getName(), this);
    repoTracker.open();
    xmlRepoFactoryTracker = new ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory>(bc, XmlBackedRepositoryFactory.class.getName(), null);
  }

  void open() {
    xmlRepoFactoryTracker.open();
  }

  void close() {
    xmlRepoFactoryTracker.close();
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

  TreeMap<RepositoryInfo,RepositoryInfo> getAllMap() {
    TreeMap<RepositoryInfo,RepositoryInfo> res = new TreeMap<RepositoryInfo,RepositoryInfo>();
    final ServiceReference<Repository>[] srs = repoTracker.getServiceReferences();
    if (srs != null) {
      for (ServiceReference<Repository> sr : srs) {
        final RepositoryInfo ri = new RepositoryInfo(sr);
        res.put(ri, ri);
      }
    }
    return res;
  }

  Repository getRepository(RepositoryInfo ri) {
    return repoTracker.getService(ri.getServiceReference());
  }

  void addListener(RepositoryListener rl) {
    // We prepend to get Activator listener called last;
    listeners.add(0, rl);
  }

  void removeListener(RepositoryListener rl) {
    listeners.remove(rl);
  }

  Dictionary<String, ?> getServiceProperties() {
    Hashtable<String, Object> p = new Hashtable<String, Object>(); 
    p.put(RepositoryManager.NUM_REPOSITORIES, new Integer(repoTracker.size() + size_offset));
    return p;
  }

  @Override
  public synchronized Repository addingService(ServiceReference<Repository> sr) {
    RepositoryInfo ri = new RepositoryInfo(sr);
    size_offset = 1;
    for (RepositoryListener rl : listeners) {
      rl.addingRepo(ri);
    }
    size_offset = 0;
    return repoTracker.addingService(sr);
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
    repoTracker.removedService(sr, service);
  }

}
