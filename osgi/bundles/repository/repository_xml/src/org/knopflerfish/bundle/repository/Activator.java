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

import java.util.Dictionary;
import java.util.Hashtable;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class Activator implements BundleActivator {
  final static String REPOSITORY_XML_PID = "org.knopflerfish.repository.xml.MSF";
  final static String REPOSITORY_XML_URL = "org.knopflerfish.repository.xml.url";
  BundleContext bc;
  FactoryImpl factory;
  ServiceRegistration<XmlBackedRepositoryFactory> sr;
  ServiceRegistration<ManagedServiceFactory> msfr;
  
  @Override
  public void start(BundleContext bc) throws Exception {
    this.bc  = bc;
    factory = new FactoryImpl(bc);
    
    String url = bc.getProperty(REPOSITORY_XML_URL);
    if(url != null && !"".equals(url)) {
      factory.create(url, null, null);
    }
    
    sr = bc.registerService(XmlBackedRepositoryFactory.class, factory, null);
    
    
    Hashtable<String, String> h = new Hashtable<String, String>();
    h.put(Constants.SERVICE_PID, REPOSITORY_XML_PID);
    msfr = bc.registerService(ManagedServiceFactory.class, new MSF(), h);
  }



  @Override
  public void stop(BundleContext bc) throws Exception {
    if(msfr != null) {
      msfr.unregister();
    }
    if(sr != null) {
      sr.unregister();
    }
    factory.destroyAll();
  }
  
  class MSF implements ManagedServiceFactory {

    @Override
    public String getName() {
      return "Xml-backed Repository Factory";
    }

    @Override
    public synchronized void updated(String pid, Dictionary<String, ?> p)
        throws ConfigurationException {

        try {
          factory.create((String)p.get("url"), p, pid);
        } catch (Exception e) {
          e.printStackTrace();
        }  
    }

    @Override
    public synchronized void deleted(String pid) {
      factory.destroy(pid);
    }
    
  }
}
