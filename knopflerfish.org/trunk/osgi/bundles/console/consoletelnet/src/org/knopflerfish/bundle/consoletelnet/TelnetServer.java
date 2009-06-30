/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * The telnet console server listens to a port for connections and
 * upon accept, creates a telnet session to handle that connection.
 */
public class TelnetServer implements org.osgi.framework.BundleActivator,
        Runnable, ManagedService {
    private static BundleContext bc;

    private static LogRef log = null;

    private static TelnetConfig telnetConfig = null;

    private static ServiceRegistration configServReg = null;

    private static Thread telnetServerThread;

    private static Hashtable telnetSessions = null;

    private TelnetSession telnetSession = null;

    private boolean accept = true; // control of main loop running

    private boolean updated = true; // control of main loop server update

    private boolean first = true; // first time server start

    public TelnetServer() {

    }

    // BundleActivator methods implementation

    public void start(BundleContext bc) {
        TelnetServer.bc = bc;

        log = new LogRef(bc, true);

        telnetSessions = new Hashtable();

        Hashtable conf = new Hashtable();
        try {
            telnetConfig = new TelnetConfig(bc);
            conf.put(Constants.SERVICE_PID, getClass().getName());

            configServReg = bc.registerService(ManagedService.class.getName(),
                    this, conf);
        } catch (ConfigurationException cexp) {
            log.error("Consoletelnet configuration error " + cexp.toString());
        }

        telnetServerThread = new Thread(this, "ConsoleTelnetServer");
        telnetServerThread.setDaemon(true);
        telnetServerThread.start();
    }

    public void stop(BundleContext bc) {
        // Stop accepting new connections
        accept = false;

        // Close all pending sessions

        Enumeration e = telnetSessions.keys();
        while (e.hasMoreElements()) {
            telnetSession = (TelnetSession) e.nextElement();
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

    public void run() {
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
                    } catch (IOException e) {
                        log.error("Server socket exception ", e);
                        log.error("Port: " + telnetConfig.getPort());
                        bFailed = true;
                    }
                }
                try {
                    // serverSocket = new ServerSocket(telnetConfig.getPort());
                    serverSocket = new ServerSocket(telnetConfig.getPort(),
                                                    telnetConfig.getBacklog(),
                                                    telnetConfig.getAddress());

                    log.info("listening on port " + telnetConfig.getPort());

                    updated = false;
                } catch (IOException iox) {
                    log.error("Server socket exception", iox);
                    updated = true;
                    bFailed = true;
                }
            } else {
                // System.out.print(":");
                try {
                    serverSocket.setSoTimeout(telnetConfig.getSocketTimeout());
                    Socket socket = serverSocket.accept();

                    // System.out.println("accepting on " + socket);
                    TelnetSession telnetSession = new TelnetSession(socket,
                            telnetConfig, log, bc, this);
                    Thread telnetSessionThread = new Thread(telnetSession,
                            "TelnetConsoleSession");
                    telnetSessions.put(telnetSession, telnetSessionThread);
                    telnetSessionThread.setDaemon(false);
                    telnetSessionThread.start();
                } catch (InterruptedIOException iox) { // Timeout
                    // iox.printStackTrace();
                } catch (IOException iox) {
                    // iox.printStackTrace();
                    log.error("Server exception", iox);
                }
            }
        } // end while (accept)

        // Accept no connections, shut down server socket

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("Server socket exception " + e);
                log.error("Port: " + telnetConfig.getPort());
            }
        }
    } // end run

    public void removeSession(TelnetSession tels) {
        telnetSessions.remove(tels);
    }

    public TelnetConfig getTelnetConfig() {
        return telnetConfig;
    }

    public void updated(Dictionary dict) {
        // Get port and host of present config
        // If they differ from the new configuration,
        // set semaphore updated = true
        // so that the main loop may detect the change and
        // close and reopen the server socket

        try {
            boolean portupdated;
            boolean hostupdated = false;

            int oldport = telnetConfig.getPort();
            InetAddress oldinet = telnetConfig.getAddress();

            telnetConfig.updated(dict);

            int port = telnetConfig.getPort();
            InetAddress inet = telnetConfig.getAddress();

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
            updated = portupdated || hostupdated || first;
            first = false;

            log.info("p " + portupdated);
            log.info("h " + hostupdated);
            log.info("upda " + updated);
        } catch (Exception ex) {
            log.error("Consoltelnet config exception " + ex);
            ex.printStackTrace();
        }
    }
} // TelnetServer
