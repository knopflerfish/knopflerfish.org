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
import java.util.List;
import java.util.SortedSet;
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
  private TreeSet<RepositoryInfo> myRepos;
  private boolean autoEnable;

  public RepositoryManagerImpl(Repositories repos) {
    this.repos = repos;
    autoEnable = true;
    myRepos = null;
  }

  public List<Capability> findProviders(Requirement requirement) {
    ArrayList<Capability> res = new ArrayList<Capability>();
    for (RepositoryInfo ri : myRepos != null ? myRepos : repos.getAll()) {
      Repository r = repos.getRepository(ri);
      if (r != null) {
        res.addAll(r.findProviders(Collections.singleton(requirement)).get(requirement));
      }
    }
    return res;
  }

  @Override
  public SortedSet<RepositoryInfo> getAllRepositories() {
    return new TreeSet<RepositoryInfo>(repos.getAll());
  }

  @Override
  public SortedSet<RepositoryInfo> getRepositories() {
    TreeSet<RepositoryInfo> res = new TreeSet<RepositoryInfo>(repos.getAll());
    if (myRepos != null) {
      
    }
    return res;
  }

  @Override
  public RepositoryInfo addXmlRepository(String url, Dictionary<String, Object> props) throws Exception {
    final RepositoryInfo repo = repos.addXmlRepository(url, props);
    if (repo != null && myRepos != null) {
      myRepos.add(repo);
    }
    return repo;
  }

  @Override
  public void setRepositoryEnabled(RepositoryInfo ri, boolean enabled) {
    synchronized (repos) {
      if (enabled) {
        // Get the registered RepositoryInfo entry
        ri = repos.get(ri.getId());
        if (myRepos != null && !myRepos.contains(ri)) {
          myRepos.add(ri);
        }
      } else {
        if (myRepos != null) {
          myRepos = repos.getAll();
        }
        myRepos.remove(ri);
      }
    }
  }

  @Override
  public void setRepositoryRank(RepositoryInfo ri, int rank) {
    synchronized (repos) {
      if (myRepos != null) {
        myRepos = repos.getAll();
      }
      for (RepositoryInfo old : myRepos) {
        if (old.equals(ri)) {
          myRepos.add(new RepositoryInfo(old, rank));
          break;
        }
      }
    }
  }

  @Override
  public boolean isEnabled(RepositoryInfo ri) {
    return myRepos == null || myRepos.contains(ri);
  }

  @Override
  public void addingRepo(RepositoryInfo ri) {
    if (myRepos != null && autoEnable) {
      myRepos.add(ri);
    }
  }

  @Override
  public void modifiedRepo(RepositoryInfo ri) {
    if (myRepos != null && autoEnable) {
      myRepos.add(ri);
    }
  }

  @Override
  public void removedRepo(RepositoryInfo ri) {
    if (myRepos != null) {
      myRepos.remove(ri);
    }
  }

}
