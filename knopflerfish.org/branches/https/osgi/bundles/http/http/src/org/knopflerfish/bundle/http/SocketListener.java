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

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.osgi.service.cm.ConfigurationException;
import org.knopflerfish.service.log.LogRef;


public class SocketListener implements Runnable {

  // private constants

  private final static String zeroAddress = "0.0.0.0";


  // private fields

  private final HttpConfig httpConfig;
  private final LogRef log;
  private final TransactionManager transactionManager;

  private int port = -1;
  private String host = null;
  private int maxConnections = -1;
  private Boolean isSecure = null;
  private Boolean requireClientAuth = null;
  private String keystoreUrl = null;
  private String keystorePass = null;

  private boolean done = false;
  private ServerSocket socket = null;
  private Thread thread = null;


  // constructors

  public SocketListener(final HttpConfig httpConfig,
                        final LogRef log,
                        final TransactionManager transactionManager) {

    this.httpConfig = httpConfig;
    this.log = log;
    this.transactionManager = transactionManager;
  }

  public void updated() throws ConfigurationException {

    final Boolean isSecure = new Boolean(httpConfig.isSecure());
    final Boolean requireClientAuth =
        new Boolean(httpConfig.getClientAuthentication());
    int cm_port = httpConfig.getPort();
    if (((port != -1 && cm_port == 0) ||
         cm_port == port) &&
        httpConfig.getHost().equals(host) &&
        httpConfig.getMaxConnections() == maxConnections &&
        isSecure.equals(this.isSecure)) {
      if (cm_port == 0)
      	httpConfig.setPort(port);
      if (this.isSecure.booleanValue()) {
        if (requireClientAuth.equals(this.requireClientAuth) &&
            httpConfig.getKeyStore().equals(keystoreUrl) &&
            httpConfig.getKeyStorePass().equals(keystorePass)) {
          return;
        }
      } else {
        return;
      }
    }

    port = httpConfig.getPort();
    host = httpConfig.getHost();
    maxConnections = httpConfig.getMaxConnections();
    this.isSecure = isSecure;
    this.requireClientAuth = requireClientAuth;
    keystoreUrl = httpConfig.getKeyStore();
    keystorePass = httpConfig.getKeyStorePass();

    if (thread != null)
      destroy();
    init();
  }

  public void init() throws ConfigurationException {

    done = false;

    createSocket();

    port = socket.getLocalPort();
    httpConfig.setPort(port);

    if (log.doInfo()) log.info("Http server started on port " + port);

    thread = new Thread(this, "HttpServer:" + port);
    thread.start();
  }

  public void createSocket() throws ConfigurationException {

    try {

      if (isSecure.booleanValue()) {
        if (log.doDebug()) log.debug("Creating secure socket");
        try {
          Class clazz = getClass().getClassLoader().loadClass("org.knopflerfish.bundle.http.SecureSocketListener");
          Method method = clazz.getMethod("createSecureSocket", new Class[] { HttpConfig.class, LogRef.class });
          socket = (ServerSocket) method.invoke(null, new Object[] { httpConfig, log });
        } catch (ClassNotFoundException cnfe) {
          if (log.doDebug()) log.debug("ClassNotFoundException in createSocket", cnfe);
          throw new ConfigurationException(HttpConfig.SECURE_KEY, cnfe.toString());
        } catch (NoSuchMethodException nsme) {
          if (log.doDebug()) log.debug("NoSuchMethodException in createSocket", nsme);
          throw new ConfigurationException(HttpConfig.SECURE_KEY, nsme.toString());
        } catch (IllegalAccessException iae) {
          if (log.doDebug()) log.debug("IllegalAccessException in createSocket", iae);
          throw new ConfigurationException(HttpConfig.SECURE_KEY, iae.toString());
        } catch (InvocationTargetException ite) {
          if (log.doDebug()) log.debug("InvocationTargetException in createSocket", ite);
          throw new ConfigurationException(HttpConfig.SECURE_KEY, ite.getTargetException().toString());
        }
      } else {
        if (log.doDebug()) log.debug("Creating socket");
        if (host == null || host.length() == 0) {
          socket = new ServerSocket(port, maxConnections);
        } else {
          try {
            socket = new ServerSocket(port,
                                      maxConnections,
                                      InetAddress.getByName(host));
          } catch (UnknownHostException uhe) {
            socket = new ServerSocket(port, maxConnections);
          }
        }
      }
    } catch (ConfigurationException ce) {
      done = true;
      throw ce;
    } catch (IOException ioe) {
      done = true;
      if (log.doDebug()) log.debug("IOException in createSocket", ioe);
      throw new ConfigurationException(HttpConfig.PORT_KEY, ioe.toString());
    } catch (Exception e) {
      done = true;
      if (log.doDebug()) log.debug("Exception in createSocket", e);
      throw new ConfigurationException(HttpConfig.PORT_KEY, e.toString());
    }
  }

  public void destroy() {

    done = true;

    Thread[] threads = new Thread[transactionManager.activeCount()];
    transactionManager.enumerate(threads);
    for (int i = 0; i < threads.length; i++)
      if (threads[i] != null)
        threads[i].interrupt();

    threads = new Thread[transactionManager.activeCount()];
    transactionManager.enumerate(threads);
    for (int i = 0; i < threads.length; i++) {
      if (threads[i] != null) {
        try {
          threads[i].join(5000);
        } catch (InterruptedException ignore) { }
	if (threads[i].isAlive()) {
	  // TBD, threads[i].stop();
	  log.error("Thread "  + threads[i] + ", refuse to stop");
	}
      }
    }

    if (thread != null)
      thread.interrupt();

    try {
      if (socket != null) {
        int port = socket.getLocalPort();
        InetAddress address = socket.getInetAddress();
        if (address.getHostAddress().equals(zeroAddress))
          address = InetAddress.getLocalHost();
        (new Socket(address, port)).close();
      }
    } catch (IOException ignore) { }

    try {
      if (thread != null)
        thread.join();
    } catch (InterruptedException ignore) { }
    thread = null;
  }


  // implements Runnable

  public void run() {

    ServerSocket socket = this.socket;
    Socket client = null;

    while (!done) {

      try {

        while (transactionManager.activeCount() >=
               httpConfig.getMaxConnections()) {
          try {
            Thread.sleep(50);
            if (done)
              break;
          } catch (Exception e) {}
        }

        if (!done) {
          client = socket.accept();
          if (done) {
            client.close();
            break;
          }
        }

        transactionManager.startTransaction(client);

      } catch (InterruptedIOException iioe) {
        if (!done && log.doDebug())
          log.debug("Communication error on "+(host!=null?(host+":"):"")+port, iioe);
      } catch (IOException ioe) {
        if (!done && log.doDebug())
          log.debug("Communication error on "+(host!=null?(host+":"):"")+port, ioe);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t) {
        if (!done && log.doDebug())
          log.debug("Internal error on"+(host!=null?(host+":"):"")+port, t);
      }

    }

    try {
      if (socket != null)
        socket.close();
    } catch (IOException ignore) { }
    socket = null;
  }

} // SocketListener
