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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.knopflerfish.service.repositorymanager.RepositoryManager;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

/**
 *
 */

public class RepositoryManagerImpl
  implements RepositoryManager, RepositoryListener
{

  final private Repositories repos;
  private TreeMap<RepositoryInfo,RepositoryInfo> myRepos;
  private boolean autoEnable;

  public RepositoryManagerImpl(Repositories repos) {
    this.repos = repos;
    autoEnable = true;
    repos.addListener(this);
  }

  public List<Capability> findProviders(Requirement requirement) {
    ArrayList<Capability> res = new ArrayList<Capability>();
    for (RepositoryInfo ri : myRepos != null ? myRepos.keySet() : repos.getAll()) {
      Repository r = repos.getRepository(ri);
      if (r != null) {
        res.addAll(r.findProviders(Collections.singleton(requirement)).get(requirement));
      }
    }
    return res;
  }

  @Override
  public SortedSet<RepositoryInfo> getAllRepositories() {
    TreeSet<RepositoryInfo> res = repos.getAll();
    if (myRepos != null) {
      // Add myRepos to get correct ranking.
      res.removeAll(myRepos.values());
      res.addAll(myRepos.keySet());
    }
    return res;
  }

  @Override
  public SortedSet<RepositoryInfo> getRepositories() {
    return new TreeSet<RepositoryInfo>(myRepos != null ? myRepos.keySet() : repos.getAll());
  }

  @Override
  public RepositoryInfo addXmlRepository(String url, Dictionary<String, Object> props) throws Exception {
    final RepositoryInfo repo = repos.addXmlRepository(url, props);
    if (repo != null && myRepos != null) {
      myRepos.put(repo, repo);
    }
    return repo;
  }

  @Override
  public boolean setRepositoryEnabled(RepositoryInfo ri, boolean enabled) {
    synchronized (repos) {
      // Get the registered RepositoryInfo entry
      ri = repos.get(ri.getId());
      if (ri == null) {
        return false;
      }
      if (enabled) {
        if (myRepos != null && !myRepos.containsValue(ri)) {
          myRepos.put(ri, ri);
        }
      } else {
        if (myRepos == null) {
          myRepos = repos.getAllMap();
        }
        myRepos.values().remove(ri);
      }
    }
    return true;
  }

  @Override
  public boolean setRepositoryRank(RepositoryInfo ri, int rank) {
    synchronized (repos) {
      ri = repos.get(ri.getId());
      if (ri != null) {
        if (myRepos == null) {
          myRepos = repos.getAllMap();
        }
        if (myRepos.values().remove(ri)) {
          myRepos.put(new RepositoryInfo(ri, rank), ri);
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isEnabled(RepositoryInfo ri) {
    return myRepos == null || myRepos.containsKey(ri) || myRepos.containsValue(ri);
  }

  @Override
  public void addedRepo(RepositoryInfo ri) {
    if (myRepos != null && autoEnable && myRepos.containsValue(ri)) {
      myRepos.put(ri, ri);
    }
  }

  @Override
  public void modifiedRepo(RepositoryInfo ri) {
    // We replace it we haven't changed ranking
    if (myRepos != null) {
      final long id = ri.getId();
      for (Iterator<Entry<RepositoryInfo, RepositoryInfo>> i = myRepos.entrySet().iterator(); i.hasNext();) {
        Entry<RepositoryInfo,RepositoryInfo> ers = i.next();
        final RepositoryInfo my = ers.getKey();
        if (my.getId() == id) {
          // Only modify if we haven't changed rank
          if (my == ers.getValue()) {
            i.remove();
            myRepos.put(ri, ri);
            return;
          }
        }
      }
    }
  }

  @Override
  public void removedRepo(RepositoryInfo ri) {
    if (myRepos != null) {
      myRepos.values().remove(ri);
    }
  }

}
