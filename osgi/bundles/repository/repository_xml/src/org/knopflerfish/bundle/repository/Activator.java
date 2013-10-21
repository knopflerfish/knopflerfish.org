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
import java.util.StringTokenizer;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {
  final static String REPOSITORY_XML_PID = "org.knopflerfish.repository.xml.MSF";
  final static String REPOSITORY_XML_URLS = "org.knopflerfish.repository.xml.urls";
  BundleContext bc;
  FactoryImpl factory;
  ServiceRegistration<XmlBackedRepositoryFactory> sr;
  ServiceRegistration<ManagedServiceFactory> msfr;
  ServiceTracker<String, String> tckTracker;

  @Override
  public void start(final BundleContext bc) throws Exception {
    this.bc  = bc;
    factory = new FactoryImpl(bc);
    final String commaSeparatedUrls = bc.getProperty(REPOSITORY_XML_URLS);
    if(commaSeparatedUrls != null && !"".equals(commaSeparatedUrls)) {
      final StringTokenizer urls = new StringTokenizer(commaSeparatedUrls, ",");
      while(urls.hasMoreTokens()) {
        final String url = urls.nextToken().trim();

        new Thread(new Runnable() {

          @Override
          public void run() {
            try {
            factory.create(url, null, null);
          } catch (final Exception e) {
         // TODO: Add logging
          }

            
          }}).run(); 
      }

    }

    sr = bc.registerService(XmlBackedRepositoryFactory.class, factory, null);


    final Hashtable<String, String> h = new Hashtable<String, String>();
    h.put(Constants.SERVICE_PID, REPOSITORY_XML_PID);
    msfr = bc.registerService(ManagedServiceFactory.class, new MSF(), h);
 
    if ("true".equals(bc.getProperty("org.knopflerfish.repository.ct.prime"))) {
      tckTracker = new ServiceTracker<String, String>(bc, String.class,
          new ServiceTrackerCustomizer<String, String>() {

            @Override
            public String addingService(ServiceReference<String> reference) {
              if (reference.getProperty("repository-xml") == null) {
                return null;
              }
              final String xml = bc.getService(reference);
              new Thread(new Runnable() {

                @Override
                public void run() {
                  Dictionary<String, String> p = new Hashtable<String, String>();
                  p.put("repository-populated",
                      "org.osgi.test.cases.repository.junit.RepositoryTest");
                  try {
                    factory.createFromString(xml);
                  } catch (final Exception e) {
                    // TODO: Add logging
                  }
                  bc.registerService(Object.class, new Object(), p);

                }
              }).run();
              return xml;
            }

            @Override
            public void modifiedService(ServiceReference<String> r, String s) {
            }

            @Override
            public void removedService(ServiceReference<String> r, String s) {
            }
          });

      tckTracker.open();
    }
    
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
        } catch (final Exception e) {
          e.printStackTrace();
        }
    }

    @Override
    public synchronized void deleted(String pid) {
      factory.destroy(pid);
    }

  }
}
