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

package org.knopflerfish.bundle.consoletcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.service.useradmin.Authorization;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.SessionListener;
import org.knopflerfish.service.um.useradmin.ContextualAuthorization;
import org.knopflerfish.service.um.useradmin.PasswdAuthenticator;
import org.knopflerfish.service.um.useradmin.PasswdSession;

/**
 * TCP based console user interface implementation.
 *
 * @author Jan Stein
 */
public class ConsoleTcp
  implements BundleActivator, ManagedService
{

  private static final String PORT_PROPERTY = "port";
  private static final String ADDRESS_PROPERTY = "address";
  private static final String LOGIN_PROPERTY = "login";
  private static final String UM_PROPERTY = "um";
  private static final String EXIT_ON_LOGOUT_PROPERTY = "exitOnLogout";
  private static final String REQUIRED_GROUP_PROPERTY = "requiredGroup";
  private static final String FORBIDDEN_GROUP_PROPERTY = "forbiddenGroup";
  private static final String USE_TIMEOUT_PROPERTY = "useTimeout";

  private static final String DEFAULT_USER_NAME = "admin";
  private static final String DEFAULT_USER_PROPERTY = "defaultUser";
  private static final String DEFAULT_PASSWORD = "admin";
  private static final String DEFAULT_PASSWORD_PROPERTY = "defaultPassword";

  static private final String TELNET_INTERRUPT = "\377\364";

  private ConsoleService sConsole = null;

  private AcceptThread tAccept = null;

  BundleContext bc;

  boolean quit = false;

  Short port;

  InetAddress address;

  Boolean login;

  Boolean requireUM;

  String defaultUser;
  String defaultPassword;

  Boolean exitOnLogout;

  Boolean useTimeout;

  String requiredGroup;

  String forbiddenGroup;

  /*---------------------------------------------------------------------------*
   *			  BundleActivator implementation
   *---------------------------------------------------------------------------*/

  // ==================== start ====================
  /**
   * Called by the framework when this bundle is started.
   *
   * @param bc
   *          Bundle context.
   */
  @Override
  public void start(BundleContext bc)
  {
    this.bc = bc;

    // Get config
    final Dictionary<String, Object> p = new Hashtable<>();
    p.put(Constants.SERVICE_PID, getClass().getName());
    bc.registerService(ManagedService.class.getName(), this, p);
  }

  // ==================== stop ====================
  /**
   * Called by the framework when this bundle is stopped.
   *
   * @param bc
   *          Bundle context.
   */
  @Override
  synchronized public void stop(BundleContext bc)
  {
    stopAccept();
  }

  /*---------------------------------------------------------------------------*
   *			  ManagedService implementation
   *---------------------------------------------------------------------------*/

  private <T> T getConfig(Dictionary<String,?> config, String property) {
    try {
      //noinspection unchecked
      return (T) config.get(property);
    } catch (final ClassCastException e) {
      throw new IllegalArgumentException("Wrong type for " + property);
    }
  }

  // ==================== updated ====================
  /**
   * Called by CM when it got this bundles configuration.
   */
  @Override
  public synchronized void updated(Dictionary<String,?> config)
      throws ConfigurationException, IllegalArgumentException
  {
    if (config == null) {
      stopAccept();
      config = new Hashtable<>();
    }
    final Short oldport = port;
    final InetAddress oldAddress = address;
    final Boolean oldUseTimeout = useTimeout;
    port = getConfig(config, PORT_PROPERTY);

    setAdress(getConfig(config, ADDRESS_PROPERTY));

    login = getConfig(config, LOGIN_PROPERTY);
    requireUM = getConfig(config, UM_PROPERTY);
    exitOnLogout = getConfig(config, EXIT_ON_LOGOUT_PROPERTY);
    defaultUser = getConfig(config, DEFAULT_USER_PROPERTY);
    defaultPassword = getConfig(config, DEFAULT_PASSWORD_PROPERTY);
    requiredGroup = getConfig(config, REQUIRED_GROUP_PROPERTY);
    forbiddenGroup = getConfig(config, FORBIDDEN_GROUP_PROPERTY);
    useTimeout = getConfig(config, USE_TIMEOUT_PROPERTY);

    if (port == null) {
      port = (short) 8999;
    }
    if (port < 0) {
      log(LogService.LOG_ERROR, "Illegal port number");
      throw new IllegalArgumentException("Illegal port number");
    }
    if (login == null) {
      login = Boolean.TRUE;
    }
    if (requireUM == null) {
      requireUM = Boolean.FALSE;
    }
    if (exitOnLogout == null) {
      exitOnLogout = Boolean.FALSE;
    }
    if (defaultUser == null || defaultUser.length() == 0) {
      defaultUser = DEFAULT_USER_NAME;
    }
    if (defaultPassword == null || defaultPassword.length() == 0) {
      defaultPassword = DEFAULT_PASSWORD;
    }
    if ("".equals(requiredGroup)) {
      requiredGroup = null;
    }
    if ("".equals(forbiddenGroup)) {
      forbiddenGroup = null;
    }
    if (useTimeout == null) {
      useTimeout = Boolean.TRUE;
    }

    final boolean addressChanged =
        (oldAddress == null && address != null)
            || (oldAddress != null && !oldAddress.equals(address));

    final boolean useTimeoutChanged =
        (oldUseTimeout == null && !useTimeout)
            || (oldUseTimeout != null && !oldUseTimeout.equals(useTimeout));

    if (tAccept != null) {
      if (port.equals(oldport) && !addressChanged && !useTimeoutChanged) {
        // No need to restart
        return;
      }
      stopAccept();
    } else {
      // Get console service
      ServiceReference<ConsoleService> srefConsole = bc.getServiceReference(ConsoleService.class);
      sConsole = bc.getService(srefConsole);
      if (sConsole == null) {
        log(LogService.LOG_ERROR, "Failed to get ConsoleService");
        throw new ConfigurationException(null, "Failed to get ConsoleService");
      }
    }
    tAccept = new AcceptThread(sConsole);
    tAccept.setDaemon(false);
    quit = false;
    tAccept.start();
  }

  private void setAdress(String addressStr) {
    try {
      if (addressStr != null) {
        addressStr = addressStr.trim();
        if ("".equals(addressStr)) {
          address = null;
        } else {
          address = InetAddress.getByName(addressStr);
        }
      } else {
        address = null;
      }
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Cannot resolve " + ADDRESS_PROPERTY
                                         + ": " + addressStr);
    }
  }

  /**
   * * Utility to stop accept thread. *
   */

  private void stopAccept()
  {
    if (tAccept != null) {
      tAccept.quit();
      try {
        tAccept.join();
      } catch (final InterruptedException ignore) {
      }
      tAccept = null;
    }
  }

  // ==================== log ====================
  /**
   * Utility method used for logging.
   *
   * @param level
   *          Log level
   * @param msg
   *          Log message
   */
  void log(int level, String msg)
  {
    try {
      final ServiceReference<LogService> srLog = bc.getServiceReference(LogService.class);
      if (srLog != null) {
        final LogService sLog = bc.getService(srLog);
        if (sLog != null) {
          sLog.log(level, msg);
        }
        bc.ungetService(srLog);
      }
    } catch (final IllegalStateException exp) {
      // if the thread has survived the stop of the bundle we get this
      // which we unfortunately have to ignore
    }
  }

  // ******************** AcceptThread ********************
  /**
   * Waits for tcp connetions.
   */
  class AcceptThread
    extends Thread
    implements SessionListener
  {
    Hashtable<Session,Socket> sessions = new Hashtable<>();

    ConsoleService cs;

    ThreadGroup tg;

    AcceptThread(ConsoleService cs)
    {
      super(new ThreadGroup("Console TCP"), "Console TCP accept thread");
      this.cs = cs;
    }

    public void quit()
    {
      quit = true;
      log(LogService.LOG_INFO, "Stop listening for connections");
      for (final Session s : sessions.keySet()) {
        s.close();
      }
      if (tg != null) {
        final Thread[] threads = new Thread[tg.activeCount()];
        tg.enumerate(threads);
        for (final Thread thread : threads) {
          thread.interrupt();
        }
        // BUG workaround! Connect so that socket is closed properly
        try {
          (new Socket("localhost", port.intValue())).close();
        } catch (final IOException ignore) {
        }
        try {
          join();
        } catch (final InterruptedException ignore) {
        }
      }
    }

    @Override
    public void run()
    {
      ServerSocket serverSock = null;
      tg = getThreadGroup();
      try {
        log(LogService.LOG_INFO, "Listening for connections on port " + port);
        serverSock = new ServerSocket(port.intValue(), 50, address);
        final SessionListener thisSessionListener = this; // used in
                                                          // thread
        // below
        while (!quit) {
          // Listen for incoming requests:
          final Socket client = serverSock.accept();
          if (quit) {
            client.close();
            break;
          }

          // Create new Thread to be able to handle simultaneous
          // logins:
          final Thread startSessionThread = new Thread(() -> {
            final String sessionName =
              "console session " + client.getInetAddress().getHostName();
            PrintWriter out = null;
            try {
              out = new PrintWriter(client.getOutputStream(), true);
              final InputStream is = client.getInputStream();
              final InputStreamReader isr = new InputStreamReader(is);
              final BufferedReader in = new BufferedReader(isr);

              Authorization authorization = null;
              if (login) {
                final LoginResult loginResult = doLogin(client);
                if (loginResult.loginOK) {
                  authorization = loginResult.authorization;
                } else {
                  return;
                }
              }

              // To avoid bug with hanging sockets we set
              // SO_TIMEOUT
              // addition: to avoid a bug in j9 it is now
              // possible to
              // configure the console to not use the timeout
              if (useTimeout) {
                client.setSoTimeout(3000);
              }
              final Session session = cs.runSession(sessionName, in, out);
              final Dictionary<String, Object> props = session.getProperties();
              if (authorization != null) {
                props.put(Session.PROPERTY_AUTHORIZATION, authorization);
              }
              props.put(Session.PROPERTY_TCP, Boolean.TRUE);
              if (exitOnLogout) {
                props.put(Session.PROPERTY_EXIT_ON_LOGOUT, Boolean.TRUE);
                /*
                 * Add properties to session to be able to exit on login? if
                 * (requiredGroup != null) {
                 * props.put(Session.PROPERTY_REQUIRED_GROUP, requiredGroup);
                 * } if (forbiddenGroup != null) {
                 * props.put(Session.PROPERTY_FORBIDDEN_GROUP,
                 * forbiddenGroup); }
                 */
              }

              sessions.put(session, client);
              session.addSessionListener(thisSessionListener);
              session.setInterruptString(TELNET_INTERRUPT);
              log(LogService.LOG_INFO, "Started " + sessionName);
            } catch (final IOException e) {
              log(LogService.LOG_ERROR,
                  "Failed to get input or output from socket to console session: "
                      + e);
              try {
                if (out != null && client.getSoTimeout() > 0) {
                  out.println("\nSession timed out (" + client.getSoTimeout()
                              + " ms).");
                }
              } catch (final SocketException ignore) {
              }
              try {
                client.close();
              } catch (final IOException e2) {
                log(LogService.LOG_ERROR, "Failed to close socket: " + e);
              }
            }
          });
          startSessionThread.start();

        } // while (!quit)
      } catch (final Exception e) {
        if (!quit) {
          log(LogService.LOG_ERROR, "Communication error in ConsoleTcp: " + e);
        }
      }
      if (serverSock != null) {
        try {
          serverSock.close();
        } catch (final IOException ignore) {
        }
      }
    }

    // SessionListener

    @Override
    public void sessionEnd(Session s)
    {
      try {
        sessions.remove(s).close();
      } catch (final IOException ignore) {
      }
    }

    LoginResult doLogin(Socket client)
        throws IOException
    {
      final PrintWriter out = new PrintWriter(client.getOutputStream(), true);
      final InputStream is = client.getInputStream();
      final InputStreamReader isr = new InputStreamReader(is);
      final BufferedReader in = new BufferedReader(isr);

      // To avoid bug with hanging sockets we set SO_TIMEOUT
      // 60 secs to login
      // addition: to avoid a bug in j9 it is now possible to configure
      // the console to not use the timeout
      if (useTimeout) {
        client.setSoTimeout(60000);
      }

      out.print("login: ");
      out.flush();
      final String userName = in.readLine();
      out.print("password: ");

      // Turn off echo:
      out.print((char) 255); // IAC
      out.print((char) 251); // WILL
      out.print((char) 1); // TELOPT_ECHO
      out.print((char) 0);
      out.flush();
      in.read();
      in.read();
      in.read(); // Read answer.

      // Read password:
      final String password = in.readLine();

      // Turn echo back on:
      out.print((char) 255); // IAC
      out.print((char) 252); // WONT
      out.print((char) 1); // TELOPT_ECHO
      out.print((char) 0);
      out.flush();
      in.read();
      in.read();
      in.read(); // Read answer.
      out.println();

      ContextualAuthorization authorization = null;
      boolean loginOK = false;
      final ServiceReference<PasswdAuthenticator> sr =
        bc.getServiceReference(PasswdAuthenticator.class);
      if (sr == null) {
        if (requireUM) {
          log(LogService.LOG_WARNING,
              "Failed to get PasswdAuthenticator reference. UM required but not present.");
        } else {
          loginOK =
            (defaultUser.equals(userName) && defaultPassword.equals(password));
        }
      } else {
        final PasswdAuthenticator pa = bc.getService(sr);
        if (pa != null) {
          final PasswdSession ps = pa.createSession();
          ps.setUsername(userName);
          ps.setPassword(password);
          ContextualAuthorization ca = null;
          try {
            ca = ps.getAuthorization();
          } catch (final IllegalStateException ex) {
            log(LogService.LOG_WARNING, "UserAdmin service not available.");
          }
          if (ca != null) {
            if (requiredGroup != null && !ca.hasRole(requiredGroup)) {
              loginOK = false;
              log(LogService.LOG_INFO,
                  userName + " tried to login, but did not have required role "
                      + requiredGroup);
            } else if (forbiddenGroup != null && ca.hasRole(forbiddenGroup)) {
              loginOK = false;
              log(LogService.LOG_INFO,
                  userName + " tried to login, but had forbidden role "
                      + forbiddenGroup);
            } else {
              authorization = ca;
              loginOK = true;
              out.println("Logged in.");
            }
          }
        } else {
          log(LogService.LOG_WARNING,
              "Failed to get PasswdAuthenticator service.");
        }
        bc.ungetService(sr);
      }

      // Check if login successful:
      if (!loginOK) {
        out.println("Login failed!");
        client.close();
        log(LogService.LOG_INFO, "Login failed for " + userName);
        return new LoginResult();
      }

      // Set context:
      if (authorization != null) {
        final String authMethod = "passwd"; // TBD
        final String inputPath = "tcp"; // TBD
        authorization.setIPAMContext(inputPath, authMethod);
        final Dictionary<?, ?> context = authorization.getContext();
        log(LogService.LOG_INFO, "User " + authorization.getName()
                                 + " logged in, authentication context is "
                                 + context + ".");
      } else {
        log(LogService.LOG_INFO, "Default user " + defaultUser + " logged in.");
      }
      return new LoginResult(authorization);
    }

    class LoginResult
    {
      Authorization authorization;

      boolean loginOK;

      LoginResult()
      {
        loginOK = false;
      }

      LoginResult(Authorization authorization)
      {
        this.authorization = authorization;
        loginOK = true;
      }
    }

  }

}