/*
 * Copyright (c) 2003-2013,2015 KNOPFLERFISH project
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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.knopflerfish.service.log.LogRef;

public class HttpServer
{
  // private fields

  private final HttpConfig httpConfig;

  private final Registrations registrations;

  private final ServletContextManager contextManager;

  private final HttpServiceFactory serviceFactory;

  private final HttpSessionManager sessionManager;

  private final TransactionManager transactionManager;

  private final SocketListener httpSocketListener;

  private final SocketListener httpsSocketListener;

  private final LogRef log;

  private ServiceRegistration<?> httpReg = null;

  private BundleContext bc = null;

  // constructors

  public HttpServer(BundleContext bc, final HttpConfig httpConfig,
                    final LogRef log)
  {
    this.bc = bc;
    this.log = log;
    this.httpConfig = httpConfig;
    registrations = new Registrations();

    contextManager = new ServletContextManager(httpConfig, log, registrations);
    serviceFactory = new HttpServiceFactory(log, registrations, contextManager);

    sessionManager = new HttpSessionManager(httpConfig);
    transactionManager =
      new TransactionManager(httpConfig, log, registrations, sessionManager);

    httpSocketListener =
      new SocketListener(httpConfig.HTTP, log, transactionManager, bc, this);
    httpsSocketListener =
      new SocketListener(httpConfig.HTTPS, log, transactionManager, bc, this);
  }

  // public methods

  public HttpConfig getHttpConfig()
  {
    return httpConfig;
  }

  public HttpServiceFactory getHttpServiceFactory()
  {
    return serviceFactory;
  }

  public void updated()
      throws ConfigurationException
  {

    try {
      httpSocketListener.updated();
      httpsSocketListener.updated();
      doHttpReg();
    } catch (final ConfigurationException e) {
      // If configuration failed, make sure we don't have
      // any registered service
      if (httpReg != null) {
        httpReg.unregister();
        httpReg = null;
      }
      // and rethrow the exception
      throw e;
    }
  }

  public void destroy()
  {
    if (httpSocketListener != null) {
      httpSocketListener.destroy();
    }
    if (httpsSocketListener != null) {
      httpsSocketListener.destroy();
    }
    if (httpReg != null) {
      httpReg.unregister();
      httpReg = null;
    }
    if (transactionManager != null) {
      try {
        transactionManager.destroy();
      } catch (final IllegalThreadStateException itse) {
        log.warn("All HTTP transaction threads did not terminate; "
                 + "unable to destroy transaction manager thread group.", itse);
      }
    }
    if (sessionManager != null) {
      sessionManager.destroy();
    }
  }

  synchronized void doHttpReg()
  {
    final Dictionary<String, Object> conf = httpConfig.getConfiguration();

    final Hashtable<String, Object> props = new Hashtable<String, Object>();
    for (final Enumeration<String> e = conf.keys(); e.hasMoreElements();) {
      final String key = e.nextElement();
      // Properties with a key starting on '.' are private and should not be
      // published as service properties. See Compendium 104.4.4.
      if (key.charAt(0) != '.') {
        final Object val = conf.get(key);
        props.put(key, val);
      }
    }
    // UPnP Ref impl need this
    props.put("openPort", props.get(HttpConfig.HTTP_PORT_KEY));

    // register and/or update service properties
    if (httpReg == null) {
      httpReg =
        bc.registerService(HttpServiceImpl.HTTP_INTERFACES,
                           getHttpServiceFactory(), props);
    } else {
      httpReg.setProperties(props);
    }
  }

  public Registrations getRegistrations()
  {
    return registrations;
  }

  public TransactionManager getTransactionManager()
  {
    return transactionManager;
  }

  public boolean isHttpOpen()
  {
    return httpSocketListener.isOpen();
  }

  public boolean isHttpsOpen()
  {
    return httpsSocketListener.isOpen();
  }

  public boolean portConfigMatch(HttpConfig config) {
    // TODO Auto-generated method stub
    return httpSocketListener.portConfigMatch(new HttpConfigWrapper(false, config))
        && httpsSocketListener.portConfigMatch(new HttpConfigWrapper(true, config));
  }

} // HttpServer
