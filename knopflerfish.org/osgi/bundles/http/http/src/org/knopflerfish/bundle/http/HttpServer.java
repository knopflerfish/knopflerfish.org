/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.http;

import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import org.knopflerfish.service.log.LogRef;
import org.osgi.service.cm.ConfigurationException;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Dictionary;

public class HttpServer {

  // private fields

  private final HttpConfig httpConfig;
  private final LogRef log;

  private final Registrations registrations;

  private final ServletContextManager contextManager;
  private final HttpServiceFactory serviceFactory;

  private final HttpSessionManager sessionManager;
  private final TransactionManager transactionManager;

  private final SocketListener socketListener;

  private ServiceRegistration httpReg = null;


  // constructors

  public HttpServer(final HttpConfig httpConfig, final LogRef log) {

    this.httpConfig = httpConfig;
    this.log = log;

    registrations = new Registrations();

    contextManager = new ServletContextManager(httpConfig, log, registrations);
    serviceFactory = new HttpServiceFactory(log,
                                            registrations,
                                            contextManager);

    sessionManager = new HttpSessionManager(httpConfig);
    transactionManager = new TransactionManager(httpConfig,
                                                log,
                                                registrations,
                                                sessionManager);

    socketListener = new SocketListener(httpConfig, log, transactionManager);
  }


  // public methods

  public HttpConfig getHttpConfig() {
    return httpConfig;
  }

  public HttpServiceFactory getHttpServiceFactory() {
    return serviceFactory;
  }

  public void setServiceRegistration(ServiceRegistration httpReg) {
    this.httpReg = httpReg;
  }

  public void updated() throws ConfigurationException {

    transactionManager.updated();
    socketListener.updated();
    Dictionary conf = httpConfig.getConfiguration();
    
    Hashtable props = new Hashtable();
    for(Enumeration e = conf.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      Object val = conf.get(key);
      props.put(key, val);
    }
    props.put("openPort", props.get(HttpConfig.PORT_KEY));
    httpReg.setProperties(props);
  }

  public void destroy() {

    socketListener.destroy();
    httpReg.unregister();
  }

} // HttpServer
