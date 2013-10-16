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
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.knopflerfish.service.repositorymanager.RepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;
import org.osgi.util.tracker.ServiceTracker;

class Repositories implements ServiceListener
{

  final private TreeMap<RepositoryInfo, RepositoryInfo> repos = new TreeMap<RepositoryInfo, RepositoryInfo>();
  final private ArrayList<RepositoryListener> listeners = new ArrayList<RepositoryListener>(2);
  final private BundleContext bc;

  private ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory> xmlRepoFactoryTracker = null;
  private int changeCount = 0;

  Repositories(BundleContext bc) {
    this.bc = bc;
    synchronized (this) {
      final String clazz = Repository.class.getName();
      try {
        bc.addServiceListener(this, "(objectClass=" + clazz + ")");
        for (ServiceReference<Repository> sr : bc.getServiceReferences(Repository.class, null)) {
          RepositoryInfo ri = new RepositoryInfo(sr);
          repos.put(ri, ri);
        }
      } catch (InvalidSyntaxException _ignore) { }
    }
    xmlRepoFactoryTracker = new ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory>(bc, XmlBackedRepositoryFactory.class.getName(), null);
    xmlRepoFactoryTracker.open();
  }

  RepositoryInfo addXmlRepository(String url, Dictionary<String,?> properties) throws Exception {
    XmlBackedRepositoryFactory xf = xmlRepoFactoryTracker.getService();
    if (xf != null) {
      ServiceReference<Repository> sr = xf.create(url, properties, null);
      if (sr != null) {
        RepositoryInfo ri = new RepositoryInfo(sr);
        synchronized (this) {
          repos.put(ri, ri);
        }
        return ri;
      }
    }
    return null;
  }

  synchronized RepositoryInfo get(long id) {
    for (RepositoryInfo ri : repos.keySet()) {
      if (ri.getId() == id) {
        return ri;
      }
    }
    return null;
  }

  synchronized TreeSet<RepositoryInfo> getAll() {
    return new TreeSet<RepositoryInfo>(repos.keySet());
  }

  @SuppressWarnings("unchecked")
  TreeMap<RepositoryInfo,RepositoryInfo> getAllMap() {
    return (TreeMap<RepositoryInfo, RepositoryInfo>) repos.clone();
  }

  Repository getRepository(RepositoryInfo ri) {
    return bc.getService(ri.getServiceReference());
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
    p.put(RepositoryManager.CHANGE_COUNT, new Integer(changeCount));
    p.put(RepositoryManager.NUM_REPOSITORIES, new Integer(repos.size()));
    return p;
  }

  int getChangeCount() {
    return changeCount;
  }

  @Override
  public void serviceChanged(ServiceEvent event) {
    @SuppressWarnings("unchecked")
    RepositoryInfo ri = new RepositoryInfo((ServiceReference<Repository>) event.getServiceReference());
    synchronized (this) {
      switch (event.getType()) {
      case ServiceEvent.REGISTERED:
        repos.put(ri, ri);
        break;
      case ServiceEvent.MODIFIED:
        for (Iterator<RepositoryInfo> i = repos.keySet().iterator(); i.hasNext();) {
          RepositoryInfo old = i.next();
          if (old.getId() == ri.getId()) {
            i.remove();
            repos.put(ri, ri);
          }
        }
        break;
      case ServiceEvent.MODIFIED_ENDMATCH:
      case ServiceEvent.UNREGISTERING:
        repos.remove(ri);
        break;      
      }
      changeCount++;
    }
    for (RepositoryListener rl : listeners) {
      switch (event.getType()) {
      case ServiceEvent.REGISTERED:
        rl.addedRepo(ri);
        repos.put(ri, ri);
        break;
      case ServiceEvent.MODIFIED:
        rl.modifiedRepo(ri);
        break;
      case ServiceEvent.MODIFIED_ENDMATCH:
      case ServiceEvent.UNREGISTERING:
        rl.removedRepo(ri);
        break;      
      }
    }
  }

}
