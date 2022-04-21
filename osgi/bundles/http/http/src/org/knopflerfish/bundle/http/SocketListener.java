/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.knopflerfish.service.log.LogRef;

// NOTE: Use raw types to avoid explicit dependency on SSLServerSocketFactory.
@SuppressWarnings("rawtypes")
public class SocketListener
  implements Runnable, ServiceTrackerCustomizer
{

  // private constants

  private final static String zeroAddress = "0.0.0.0";

  // private fields

  private final HttpConfigWrapper httpConfig;

  private final LogRef log;

  private final TransactionManager transactionManager;

  private final BundleContext bc;

  private final HttpServer httpServer;

  private ServiceTracker<?, ?> securityTracker = null;

  private int port = -1;

  private String host = null;

  private int maxConnections = -1;

  private boolean isSecure = false;

  private boolean isEnabled = false;

  private Boolean requireClientAuth = null;

  private boolean done = false;

  private ServerSocket socket = null;

  private Thread thread = null;

  // constructors

  public SocketListener(final HttpConfigWrapper httpConfig, final LogRef log,
                        final TransactionManager transactionManager,
                        final BundleContext bc, final HttpServer httpServer)
  {
    this.httpConfig = httpConfig;
    this.log = log;
    this.transactionManager = transactionManager;
    this.bc = bc;
    this.httpServer = httpServer;
  }

  @SuppressWarnings("unchecked")
  public void updated()
      throws ConfigurationException
  {
    // the following if statements prevents unnecessary calls to init
    // (when nothing changed)
    if (portConfigMatch(httpConfig)) {
      return;
    }

    isSecure = httpConfig.isSecure();
    requireClientAuth = httpConfig.requireClientAuth();
    port = httpConfig.getPort();
    host = httpConfig.getHost();
    maxConnections = httpConfig.getMaxConnections();
    isEnabled = httpConfig.isEnabled();

    destroy();

    if (!isEnabled) {
      return;
    }

    /*
     * We want to be able to do either HTTPS or HTTP. The latter would always be
     * executed synchronously, e.g. in this invocation. This might not be the
     * case for HTTPs, which might happen on different threads. Therefore, try
     * to throw any ConfigurationEx as early as possible
     */
    if ((port < 0) || (port > 0xFFFF)) {
      throw new ConfigurationException((httpConfig.isSecure()
        ? HttpConfig.HTTPS_PORT_KEY
        : HttpConfig.HTTP_PORT_KEY), "invalid value=" + port);

    }
    if (maxConnections < 1) {
      throw new ConfigurationException("maxConnections", "invalid value="
                                                         + maxConnections);

    }

    if (!isSecure) {
      // for HTTP create the socket right away AND start
      try {
        if (log.doDebug()) {
          log.debug("Creating socket");
        }
        if (host == null || host.length() == 0) {
          socket = new ServerSocket(port, maxConnections);
        } else {
          try {
            socket =
              new ServerSocket(port, maxConnections,
                               InetAddress.getByName(host));
          } catch (final UnknownHostException uhe) {
            socket = new ServerSocket(port, maxConnections);
          }
        }

        init();

      } catch (final Exception e) {
        final String msg =
          "Failed to open HTTP Server Socket on "
              + (host == null || host.length() == 0 ? "*" : host) + ":" + port
              + " reason: " + e.toString();
        if (log.doDebug()) {
          log.debug(msg, e);
        }
        throw new ConfigurationException(HttpConfig.HTTP_PORT_KEY, msg, e);
      }

    } else // secure case, can not create socket by myself, need to get
           // service
    {
      // NOTE: Specify the class using a string to avoid explicit dependency.
      securityTracker =
        new ServiceTracker(this.bc, "javax.net.ssl.SSLServerSocketFactory",
                           this);
      securityTracker.open();
    }
  }

  @SuppressWarnings("unchecked")
  public Object addingService(ServiceReference sRef)
  {
    if (this.socket != null) {
      if (log.doWarn()) {
        log.warn("SEVERAL  SSLServerSocketFactories are available,"
                 + " selection random");
      }
      return null; // do not track
    }

    // TE this class must not explicitly reference SSLServerFactory.
    Object factory = this.bc.getService(sRef);

    // find the two methods using reflection
    Method create2 = null;
    Method create3 = null;

    try {
      create2 = factory.getClass().getMethod("createServerSocket", int.class, int.class);
      create3 = factory.getClass().getMethod("createServerSocket", int.class, int.class, InetAddress.class);
    } catch (final Exception cmethE) {
      log.error("not an SSL factory, or no access : " + factory, cmethE);
    }

    if (host == null || host.length() == 0) {
      try {
        //noinspection ConstantConditions
        socket = (ServerSocket) create2.invoke(factory, port, maxConnections);
      } catch (final Exception ex) {
        final Throwable cause = ex.getCause();
        final String msg =
          "Failed to open HTTPS Server Socket on *:" + port + " reason: "
              + cause.toString();
        if (log.doWarn()) {
          log.warn(msg, ex);
        }
      }
    } else {
      try {
        try {
          //noinspection ConstantConditions
          socket = (ServerSocket) create3.invoke(factory, port, maxConnections, InetAddress.getByName(host));
        } catch (final UnknownHostException uhe) {
          final String msg =
            "Failed to open HTTPS Server Socket on " + host + ":" + port
                + " reason: " + uhe.toString() +". Trying on *:" +port;
          if (log.doWarn()) {
            log.warn(msg, uhe);
          }
          //noinspection ConstantConditions
          socket = (ServerSocket) create2.invoke(factory, port, maxConnections);
        }
      } catch (final Exception ex) {
        final String msg =
          "Failed to open HTTPS Server Socket on " + host + ":" + port
              + " reason: " + ex.toString();
        if (log.doWarn()) {
          log.warn(msg, ex);
        }
      }
    }

    if (socket != null) {
      try {
        final Class<?> sslSockClass =
          Class.forName("javax.net.ssl.SSLServerSocket");
        final Method auth =
          sslSockClass.getMethod("setNeedClientAuth", boolean.class);
        auth.invoke(socket, requireClientAuth);
      } catch (final Exception exc) {
        final String msg =
          "Failed to configure client authentification for HTTPS server on "
              + host + ":" + port + " reason: " + exc.getMessage();
        log.error(msg, exc);
      }
    }

    if (socket != null) {
      try {
        init();
      } catch (final Exception e) {
        log.error("Failed to initialize HTTPS server on " + host + ":" + port
                  + " reason: " + e.getMessage(), e);
        socket = null;
      }
    }

    if (socket == null) {
      this.bc.ungetService(sRef);
      factory = null;

    }

    return factory;
  }

  public void modifiedService(ServiceReference arg0, Object arg1)
  {
  }

  public void removedService(ServiceReference sRef, Object arg1)
  {
    if (log.doDebug()) {
      log.debug("SSLFactory Security service was removed.");
    }

    uninit();

    this.bc.ungetService(sRef);

  }

  private synchronized void init()
  {
    // socket exists, otherwise not called
    done = false;

    port = socket.getLocalPort();
    if (port != httpConfig.getPort()) {
      // Update config to reflect the actual port used (if port was
      // specified as 0 this will make a difference).
      httpConfig.setPort(port);

      if (isSecure) {
        // Make sure that the service property port.https is
        // correct. Only needed for https, since init() is called
        // asynchronous in that case.
        httpServer.doHttpReg();
      }
    }

    final String sch = httpConfig.getScheme().toUpperCase();

    transactionManager.initialize();
    if (log.doInfo()) {
      log.info(sch + " server started on port " + port);
    }

    thread = new Thread(this, sch + " server:" + port);
    thread.start();

  }

  private synchronized void uninit()
  {
    done = true;

    transactionManager.shutdown();
    
//    Thread[] threads = new Thread[transactionManager.activeCount()];
//    transactionManager.enumerate(threads);
//    for (final Thread thread2 : threads) {
//      if (thread2 != null) {
//        thread2.interrupt();
//      }
//    }
//
//    threads = new Thread[transactionManager.activeCount()];
//    transactionManager.enumerate(threads);
//    for (final Thread thread2 : threads) {
//      if (thread2 != null) {
//        try {
//          thread2.join(5000);
//        } catch (final InterruptedException ignore) {
//        }
//        if (thread2.isAlive()) {
//          // TBD, threads[i].stop();
//          log.error("Thread " + thread2 + ", refuse to stop");
//        }
//      }
//    }

    if (thread != null) {
      thread.interrupt();
    }

    if (socket != null) {
      // Try different ways to find out local address.
      final int port = socket.getLocalPort();
      InetAddress address = socket.getInetAddress();
      if (address.getHostAddress().equals(zeroAddress)) {
        try {
          address = InetAddress.getLocalHost();
        } catch (final UnknownHostException ignore) {
          try {
            address = InetAddress.getByName("localhost");
          } catch (final UnknownHostException ignore2) {
            try {
              address = InetAddress.getByName("127.0.0.1");
            } catch (final UnknownHostException e) {
              address = null;
              log.error("Failed to get local address", e);
            }
          }
        }
      }
      try {
        if (address != null) {
          (new Socket(address, port)).close();
        }
      } catch (final IOException ignore) {
        // Socket could already be closed?!
      } finally {
        socket = null;
      }
    }

    if (thread != null) {
      try {
        thread.join(60000);
      } catch (final InterruptedException ignore) {
      } finally {
        if (thread.isAlive()) {
          log.error("Failed to stop socket listener thread, " + thread);
        }
        thread = null;
      }
    }
  }

  public void destroy()
  {
    if (!httpConfig.isSecure()) {
      uninit();

    } else {
      final ServiceTracker<?, ?> tempTr = this.securityTracker;
      this.securityTracker = null;
      if (tempTr != null) {
        try {
          tempTr.close();

        } catch (final Exception ignored) {
        }
      }
    }
  }

  public void run()
  {
    ServerSocket socket = this.socket;
    Socket client = null;
    log.debug("starting up");
    
    while (!done) {

      try {

        while (transactionManager.getActiveTransactionCount() >= httpConfig.getMaxConnections()) {
          try {
            Thread.sleep(50);
            if (done) {
              break;
            }
          } catch (final Exception ignored) {
          }
        }

        if (!done) {
          client = socket.accept();
          if (done) {
            client.close();
            break;
          }
        }
        
        transactionManager.startTransaction(client, httpConfig);

      } catch (final IOException e) {
        if (!done && log.doDebug()) {
          log.debug("Communication error on "
                    + (host != null ? (host + ":") : "") + port, iioe);
        }
      } catch (final ThreadDeath td) {
        throw td;
      } catch (final Throwable t) {
        if (!done && log.doDebug()) {
          log.debug("Internal error on" + (host != null ? (host + ":") : "")
                    + port, t);
        }
      }

    }

    try {
      if (socket != null) {
        socket.close();
      }
    } catch (final IOException ignore) {
    }
  }

  public boolean isOpen()
  {
    return socket != null;
  }

  public boolean portConfigMatch(HttpConfigWrapper config) {
    return isSecure == config.isSecure() && (requireClientAuth != null)
        && (requireClientAuth == config.requireClientAuth())
        && port == config.getPort() && config.getHost().equals(host)
        && config.getMaxConnections() == maxConnections
        && isEnabled == config.isEnabled();
  }

} // SocketListener
