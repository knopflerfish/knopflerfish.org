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

package org.knopflerfish.bundle.repository;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import org.knopflerfish.bundle.repository.xml.RepositoryXmlParser;
import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class FactoryImpl implements XmlBackedRepositoryFactory {

  private final BundleContext bc;
  private final HashMap<Object, ServiceRegistration<Repository>> repositoryRegistrations = new HashMap<Object, ServiceRegistration<Repository>>();

  FactoryImpl(BundleContext bc) {
    this.bc = bc;
  }
  
  @Override
  public ServiceReference<Repository> create(String url, Dictionary<String, ?> properties, Object handle) throws Exception {
    if(url != null && !"".equals(url) && !repositoryRegistrations.containsKey(url)) {
      Collection<Resource> rs = RepositoryXmlParser.parse(url);
      if(!rs.isEmpty()) {
        RepositoryImpl repo = new RepositoryImpl(bc, rs);
        Hashtable<String, Object> h = new Hashtable<String, Object>();
        h.put(Constants.SERVICE_PID, "org.knopflerfish.repository.xml");
        h.put(Constants.SERVICE_DESCRIPTION, "XML repository from URL: " + url);
        //h.put("repository.url", url);
        if (properties != null) {
          for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            h.put(key, properties.get(key));
          }
        }
        ServiceRegistration<Repository> sr = bc.registerService(Repository.class, repo, h);

        repositoryRegistrations.put(url, sr);
        if(handle != null) {
          // User provided non-url custom handle
          repositoryRegistrations.put(handle, sr);
        }
        return sr.getReference();
      }
    }
    return null;
  }
  
  public ServiceReference<Repository> createFromString(String xml) throws Exception {
    System.out.println(xml);
    if(xml != null && !"".equals(xml) && !repositoryRegistrations.containsKey(xml)) {
      ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
      Collection<Resource> rs = RepositoryXmlParser.parse(is);
      if(!rs.isEmpty()) {
        RepositoryImpl repo = new RepositoryImpl(bc, rs);
        Hashtable<String, Object> h = new Hashtable<String, Object>();
        h.put(Constants.SERVICE_PID, "org.knopflerfish.repository.xml");
        h.put(Constants.SERVICE_DESCRIPTION, "XML repository from String");
        //h.put("repository.url", url);
 
        ServiceRegistration<Repository> sr = bc.registerService(Repository.class, repo, h);

        repositoryRegistrations.put(xml, sr);
        return sr.getReference();
      }
    }
    return null;
  }

  @Override
  public void destroy(Object handle) {
    ServiceRegistration<Repository> sr = repositoryRegistrations.remove(handle);
    if(sr != null) {
      sr.unregister();
      while(repositoryRegistrations.values().remove(sr)) {}; // Remove all mappings in case user provided custom handle
    }
  }
  
  void destroyAll() {
    while(!repositoryRegistrations.isEmpty()) {
      ServiceRegistration<Repository> sr = repositoryRegistrations.values().iterator().next();
      sr.unregister();
      while(repositoryRegistrations.values().remove(sr)) {}; // Remove all mappings in case user provided custom handle
    }
  }

}
