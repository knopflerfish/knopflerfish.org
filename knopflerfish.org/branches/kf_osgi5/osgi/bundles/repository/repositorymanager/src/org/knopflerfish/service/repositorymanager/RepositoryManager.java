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

package org.knopflerfish.service.repositorymanager;

import java.util.Dictionary;
import java.util.List;
import java.util.SortedSet;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;



/**
 * KF manager for interaction with OSGI repositories.
 * Registered as service factory. The service factory has one property
 * <tt>num_repositories</tt> of type <tt>Integer</tt> that tells how
 * many repositories are available. The services is updated whenever
 * any repository changes state.
 */
public interface RepositoryManager
{
  
  String CHANGE_COUNT = "change_count";
  String NUM_REPOSITORIES = "num_repositories";

  /**
   * Find providers for a requirement.
   * 
   * @see org.osgi.service.resolve.ResolverContext.findProviders
   * @param requirement
   * @return
   */
  List<Capability> findProviders(Requirement requirement);

  /**
   * Add a repository based on a repository XML file.
   * 
   * @param url
   * @param props
   * @return
   * @throws Exception
   */
  RepositoryInfo addXmlRepository(String url, Dictionary<String, Object> props) throws Exception;

  /**
   * Get all repositories available.
   * 
   * @return
   */
  SortedSet<RepositoryInfo> getAllRepositories();

  /**
   * Get all enabled repositories.
   * 
   * @return
   */
  SortedSet<RepositoryInfo> getRepositories();

  /**
   * Check if repository is enabled.
   * 
   * @param ri
   * @return
   */
  boolean isEnabled(RepositoryInfo ri);

  /**
   * Enable or disable repository.
   * 
   * @param ri
   * @param enabled
   * @return
   */
  boolean setRepositoryEnabled(RepositoryInfo ri, boolean enabled);

  /**
   * Change ranking of a repository.
   * 
   * @param ri
   * @param rank
   * @return
   */
  boolean setRepositoryRank(RepositoryInfo ri, int rank);

}
