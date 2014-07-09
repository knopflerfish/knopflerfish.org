/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.consoletelnet;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import org.knopflerfish.service.log.LogRef;

/**
 * The telnet console server listens to a port for connections and upon accept,
 * creates a telnet session to handle that connection.
 */
public class TelnetServer
  implements BundleActivator, Runnable, ManagedService
{
  private static BundleContext bc;

  private static LogRef log = null;

  private static TelnetConfig telnetConfig = null;

  private static ServiceRegistration<ManagedService> configServReg = null;

  private static Thread telnetServerThread;

  private static Hashtable<TelnetSession, Thread> telnetSessions = null;

  private TelnetSession telnetSession = null;

  private boolean accept = true; // control of main loop running

  private boolean updated = true; // control of main loop server update

  private boolean first = true; // first time server start

  public TelnetServer()
  {

  }

  // BundleActivator methods implementation

  public void start(BundleContext bc)
  {
    TelnetServer.bc = bc;

    log = new LogRef(bc, true);

    telnetSessions = new Hashtable<TelnetSession, Thread>();

    final Dictionary<String, String> conf = new Hashtable<String, String>();
    try {
      telnetConfig = new TelnetConfig(bc);
      conf.put(Constants.SERVICE_PID, getClass().getName());

      configServReg =
        bc.registerService(ManagedService.class, this, conf);
    } catch (final ConfigurationException cexp) {
      log.error("Consoletelnet configuration error " + cexp.toString());
    }

    telnetServerThread = new Thread(this, "ConsoleTelnetServer");
    telnetServerThread.setDaemon(true);
    telnetServerThread.start();
  }

  public void stop(BundleContext bc)
  {
    // Stop accepting new connections
    accept = false;
    telnetServerThread.interrupt();

    // Close all pending sessions

    final Enumeration<TelnetSession> e = telnetSessions.keys();
    while (e.hasMoreElements()) {
      telnetSession = e.nextElement();
      telnetSession.close();
    }

    configServReg.unregister();
    configServReg = null;

    log.close();
    log = null;
    bc = null;
  }

  // Telnet server main loop, listen for connections and
  // also look for configuration updates.

  public void run()
  {
    ServerSocket serverSocket = null;
    // int socketTimeout = 10000;
    // int backlog = 100;

    boolean bFailed = false;

    while (accept && !bFailed) {
      if (updated) {
        log.info("updating in server main loop");
        if (serverSocket != null) {
          try {
            serverSocket.close();
          } catch (final IOException e) {
            log.error("Server socket exception for "
                          + telnetConfig.getAddress() + ":"
                          + telnetConfig.getPort() + ": " + e, e);
            bFailed = true;
          }
        }
        try {
          serverSocket =
            new ServerSocket(telnetConfig.getPort(), telnetConfig.getBacklog(),
                             telnetConfig.getAddress());

          log.info("Listening on " + telnetConfig.getAddress() + ":"
                   + telnetConfig.getPort());

          updated = false;
        } catch (final IOException iox) {
          log.error("Server socket exception for "
              + telnetConfig.getAddress() + ":"
              + telnetConfig.getPort() + ": " + iox, iox);
          updated = true;
          bFailed = true;
        }
      } else {
        // System.out.print(":");
        try {
          serverSocket.setSoTimeout(telnetConfig.getSocketTimeout());
          final Socket socket = serverSocket.accept();

          // System.out.println("accepting on " + socket);
          final TelnetSession telnetSession =
            new TelnetSession(socket, telnetConfig, log, bc, this);
          final Thread telnetSessionThread =
            new Thread(telnetSession, "TelnetConsoleSession");
          telnetSessions.put(telnetSession, telnetSessionThread);
          telnetSessionThread.setDaemon(false);
          telnetSessionThread.start();
        } catch (final InterruptedIOException iox) { // Timeout
          // iox.printStackTrace();
        } catch (final IOException iox) {
          // iox.printStackTrace();
          log.error("Server exception", iox);
        }
      }
    } // end while (accept)

    // Accept no connections, shut down server socket

    if (serverSocket != null) {
      try {
        serverSocket.close();
      } catch (final IOException e) {
        log.error("Server socket exception for " + telnetConfig.getAddress()
                  + ":" + telnetConfig.getPort() + ": " + e, e);
      }
    }
  } // end run

  public void removeSession(TelnetSession tels)
  {
    telnetSessions.remove(tels);
  }

  public TelnetConfig getTelnetConfig()
  {
    return telnetConfig;
  }

  public void updated(Dictionary<String, ?> dict)
  {
    // Get port and host if present config
    // If they differ from the new configuration,
    // set semaphore updated = true
    // so that the main loop may detect the change and
    // close and reopen the server socket

    try {
      boolean portupdated;
      boolean hostupdated = false;

      final int oldport = telnetConfig.getPort();
      final InetAddress oldinet = telnetConfig.getAddress();

      telnetConfig.updated(dict);

      final int port = telnetConfig.getPort();
      final InetAddress inet = telnetConfig.getAddress();

      // port change check
      if (oldport != port) {
        portupdated = true;
      } else {
        portupdated = false;
      }

      // inet address change check
      if (oldinet == null && inet == null) {
        hostupdated = false;
      } else if (oldinet == null && inet != null) {
        hostupdated = true;
      } else if (oldinet != null && inet == null) {
        hostupdated = true;
      } else if (oldinet != null && inet != null) {
        if (oldinet.equals(inet)) {
          hostupdated = false;
        } else {
          hostupdated = true;
        }
      }

      // Set updated to true if we need to open a new server socket.
      updated = portupdated || hostupdated || first;
      first = false;

      log.info("New config: " + (portupdated ? "port changed " : "")
               + (hostupdated ? "host changed " : "")
               + (updated ? "new server socket needed." : ""));
    } catch (final Exception ex) {
      log.error("Consoltelnet config exception " + ex, ex);
    }
  }
} // TelnetServer
