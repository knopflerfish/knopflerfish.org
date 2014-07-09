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

import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.knopflerfish.service.repositorymanager.RepositoryManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Bundle activator implementation.
 *
 * @author Jan Stein
 */
public class Activator
  implements BundleActivator,  ServiceFactory<RepositoryManager>, RepositoryListener
{
  private BundleContext bc;
  private Repositories repos = null;
  private ServiceRegistration<?> rms = null;


  /**
   * Called by the framework when this bundle is started.
   * 
   * @param bc  Bundle context.
   */
  @Override
  public void start(BundleContext bc)
  {
	this.bc = bc;
    repos = new Repositories(bc);
    repos.addListener(this);
    synchronized (repos) {
      rms = bc.registerService(RepositoryManager.class.getName(), this, repos.getServiceProperties());
    }
  }

  /**
   * Called by the framework when this bundle is stopped.
   * 
   * @param bc  Bundle context.
   */
  @Override
  public void stop(BundleContext bc)
  {
    rms.unregister();
    repos.removeListener(this);
    rms = null;
    repos = null;
  }

  @Override
  public RepositoryManager getService(Bundle bundle,
                                      ServiceRegistration<RepositoryManager> registration) {
    return new RepositoryManagerImpl(bc, repos);
  }

  @Override
  public void ungetService(Bundle bundle, ServiceRegistration<RepositoryManager> registration,
                           RepositoryManager service) {
  }

  @Override
  public void addedRepo(RepositoryInfo ri) {
    if (rms != null) {
      rms.setProperties(repos.getServiceProperties());
    }
  }

  @Override
  public void modifiedRepo(RepositoryInfo ri) {
    if (rms != null) {
      rms.setProperties(repos.getServiceProperties());
    }
  }

  @Override
  public void removedRepo(RepositoryInfo ri) {
    if (rms != null) {
      rms.setProperties(repos.getServiceProperties());
    }
  }

}
