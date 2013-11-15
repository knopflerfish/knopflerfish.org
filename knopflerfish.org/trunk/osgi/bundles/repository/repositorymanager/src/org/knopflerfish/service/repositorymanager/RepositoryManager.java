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
import java.util.Set;
import java.util.SortedSet;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;



/**
 * KF repository manager service for interaction with OSGI repositories.
 * Registered as service factory. The service factory has two properties
 * <tt>num_repositories</tt> that tells how many repositories are
 * available and <tt>change_count</tt> that tells how many times the
 * configuration of repositories has changed.
 * The services is updated whenever there is change in any repository.
 * 
 * When a service is fetched you get a service local configuration of how
 * the repositories are used and searched.
 */

public interface RepositoryManager
{
  
  /**
   * Service property that is incremented whenever the
   * configuration changes. Value type is {@link Integer}.
   */
  String CHANGE_COUNT = "change_count";

  /**
   * Service property that tells the number of available
   * repositories. Value type is {@link Integer}.
   */
  String NUM_REPOSITORIES = "num_repositories";

  /**
   * Find providers for a requirement.
   * 
   * @see org.osgi.service.resolve.ResolverContext.findProviders
   * @param requirement {@link Requirement} to find providers for.
   * @return
   */
  List<Capability> findProviders(Requirement requirement);
  
  /**
   * Find a set of resources given the current state of the framework and
   * using the currently enabled Repositories that will allow a given 
   * list of resources to resolve.
   * 
   * @param resources List of {@link Resource} to find a resolution for.
   * @return
   */
  Set<Resource> findResolution(List<Resource> resources) throws Exception;

  /**
   * Add a repository based on a repository XML file.
   * 
   * @param url The URL to the repository file.
   * @param props Optional service properties that are registered
   *              with the Repository service. If <code>null</code>
   *              then no extra properties are added.
   * @return A RepositoryInfo entry for the added repository.
   * @throws Exception If we failed to get or parse the XML file.
   */
  RepositoryInfo addXmlRepository(String url, Dictionary<String, Object> props) throws Exception;

  /**
   * Get all repositories available.
   * 
   * @return A sorted set with all available repositories.
   *         The first element is this the highest ranked
   *         with the lowest id.
   */
  SortedSet<RepositoryInfo> getAllRepositories();

  /**
   * Get all enabled repositories.
   * 
   * @return A sorted set with all enabled repositories. The
   *         first element is this the highest ranked with
   *         the lowest id.
   */
  SortedSet<RepositoryInfo> getRepositories();

  /**
   * Check if repository is enabled.
   * 
   * @param ri Repository to check.
   * @return <code>True</code> if repository is enabled, otherwise
   *         <code>false</code> if repository is disabled or wasn't
   *         found.
   */
  boolean isEnabled(RepositoryInfo ri);

  /**
   * Enable or disable repository.
   * 
   * @param ri
   * @param enabled
   * @return <code>True</code> if repository was enabled/disabled,
   *         otherwise <code>false</code> if repository wasn't
   *         found.
   */
  boolean setRepositoryEnabled(RepositoryInfo ri, boolean enabled);

  /**
   * Change ranking of a repository.
   * 
   * @param ri Repository to update, this {@link RepositoryInfo} will
   *           become defunct if rank change succeed.
   * @param rank New rank for repository.
   * @return <code>True</code> if repository was updated with new
   *         rank, otherwise <code>false</code> if repository wasn't
   *         found.
   */
  boolean setRepositoryRank(RepositoryInfo ri, int rank);

}
