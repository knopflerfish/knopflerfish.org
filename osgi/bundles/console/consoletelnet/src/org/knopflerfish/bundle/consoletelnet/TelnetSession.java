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

package org.knopflerfish.bundle.consoletelnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.SessionListener;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.service.um.useradmin.ContextualAuthorization;
import org.knopflerfish.service.um.useradmin.PasswdAuthenticator;
import org.knopflerfish.service.um.useradmin.PasswdSession;

/**
 * This is the session object that binds the socket, the input and the output
 * streams together. Here is where the telnet commands are executed and where
 * they affect the ConsoleService session.
 */
public class TelnetSession
  implements Runnable,
    SessionListener,
    ServiceTrackerCustomizer<ConsoleService, ConsoleService> {

  private static final char MASK = '\177'; // Normal 7 bit mask

  private final Socket socket;
  private final TelnetConfig telnetConfig;
  private final ServiceTracker<?, ?> consoleTracker;
  private ConsoleService consoleService;
  private Session session;
  private final TelnetServer telnetServer;
  private final TelnetCommand[] telnetCommands = new TelnetCommand[256];
  private InputStream is;
  private OutputStream os;
  private final TelnetInputStream telnetInputStream;
  private final TelnetOutputStream telnetOutputStream;
  private final TelnetReader reader;
  private TelnetLogin telnetLogin = null;
  private final PrintWriter printWriter;
  private boolean enableEcho = true;
  private final LogRef log;
  private final BundleContext bc;

  public TelnetSession(Socket socket, TelnetConfig telnetConfig, LogRef log,
                       BundleContext bc, TelnetServer telnetServer)
  {
    this.telnetConfig = telnetConfig;
    this.socket = socket;
    this.log = log;
    this.bc = bc;
    this.telnetServer = telnetServer;

    log.info("Connection request from " + socket.getInetAddress());

    // Set up service tracker for the console service.
    consoleTracker =
        new ServiceTracker<>(bc,
            ConsoleService.class,
            this);
    consoleTracker.open();

    try {
      is = socket.getInputStream();
      os = socket.getOutputStream();
    } catch (final IOException iox) {
      log.error("Session socket opening exception " + iox.toString(), iox);
    }

    telnetInputStream = new TelnetInputStream(is, this);
    telnetOutputStream = new TelnetOutputStream(os, this);

    printWriter = new PrintWriter(telnetOutputStream);
    reader =
      new TelnetReader(telnetInputStream, this, telnetConfig.getBusyWait());

    // Instantiate the supported options, with default state
    // ts code, do show
    telnetCommands[TCC.ECHO] =
      new TelnetCommandEcho(this, TCC.ECHO, false, false);

    telnetCommands[TCC.SUPGA] =
      new TelnetCommandSupga(this, TCC.SUPGA, false, true);

    telnetCommands[TCC.STATUS] =
      new TelnetCommandStatus(this, TCC.STATUS, false, true);

  }

  @Override
  public void run()
  {
    try {
      // Telnet initial option negotiation
      initialNegotiation(telnetOutputStream);

      // Platform login processing

      printWriter.println("Knopflerfish OSGi console");

      telnetLogin = login(reader, printWriter, telnetOutputStream);

      if (telnetLogin.isPermitted()) {
        if (null == consoleService) {
          printWriter.println("Console service not available, "
                              + "closing telnet session.");
          printWriter.flush();
          log.info("User " + telnetLogin.getUser()
                   + " logged in, but was logged out since "
                   + " no console service is available.");
          sessionEnd(null);
        } else {
          log.info("User " + telnetLogin.getUser() + " logged in");
          final Authorization authorization = telnetLogin.getAuthorization();
          session = consoleService.runSession("telnet session", reader, printWriter);
          final Dictionary<String, Object> props = session.getProperties();

          if (authorization != null) {
            props.put(Session.PROPERTY_AUTHORIZATION, authorization);
          }

          printWriter.println("'quit' to end session");

          session.addSessionListener(this);
        }
      } else {
        printWriter.println("Login incorrect");
        printWriter.flush();
        log.info("Login incorrect, user name was: '" + telnetLogin.getUser()
                 + "'.");
        sessionEnd(null);
      }
    } catch (final IOException ioe) {
      log.error("Session exception " + ioe.toString(), ioe);
      ioe.printStackTrace();
      telnetServer.removeSession(this);
    }
  }

  // Session listener callback.
  // 1) Called from the console session when it is closed.
  // 2) Called directly (s==null) to release resources in
  // cases where a session is never started.
  @Override
  public void sessionEnd(Session s)
  {
    try {
      printWriter.close();
      reader.close();
      telnetInputStream.close();
      telnetOutputStream.close();
      is.close();
      os.close();
      socket.close();
      if (s != null) {
        log.info("User " + telnetLogin.getUser() + " logged out");
      }
      telnetServer.removeSession(this);
      this.session = null;
    } catch (final Exception iox) {
      log.error("Session end exception " + iox.toString(), iox);
    }
    consoleTracker.close();
  }

  public void close()
  {
    if (session == null) {
      log.warn("Console session already closed.");
    } else {
      session.close(); // This will trigger a call to sessionEnd(s).
    }
  }

  /**
   * Get the character mask This is to support binary mode, that is 7 or 8 bit
   * data in the output data stream
   */
  public char getMask()
  {
    return MASK;
  }

  /**
   * Get all instantiated commands in this session
   */
  public TelnetCommand[] getCommands()
  {
    return telnetCommands;
  }

  /**
   * Get the TelnetOutputStream
   */
  public TelnetOutputStream getTelnetOutputStream()
  {
    return telnetOutputStream;
  }

  /**
   * Method to do echo to the output stream This also looks at the enableEcho
   * flag
   */
  public void echoChar(int character)
  {
    final TelnetCommand tc = telnetCommands[TCC.ECHO];

    if (tc != null) {
      if (tc.getDoStatus() && enableEcho) {
        printWriter.print((char) character);
        printWriter.flush();
      }
    }
  }

  public void execSB()
  {
  }

  public void execGA()
  {
  }

  public void execEL()
  {
  }

  public void execEC()
  {
  }

  public void execAYT()
  {
    try {
      telnetOutputStream.writeCommand("["
                                      + socket.getLocalAddress().getHostName()
                                      + ": yes]");
    } catch (final IOException ex) {
      log.error("Command AYT exception " + ex.toString(), ex);
    }
  }

  public void execAO()
  {
    session.abortCommand();
  }

  public void execIP()
  {
    session.abortCommand();
  }

  public void execBRK()
  {
    session.abortCommand();
  }

  public void execDM()
  {
  }

  public void execNOP()
  {
  }

  /*
   * Execution of standard telnet negotiation commmands DO, DONT, WILL and WONT
   * and also the execution of the command via SB code .... SE option command
   * structure.
   */

  /**
   * DONT
   *
   * @param code, the optional command code
   */

  public void execDONT(int code)
  {
    final String response = execCommand(TCC.DONT, code, null);
    try {
      telnetOutputStream.writeCommand(response);
    } catch (final IOException ex) {
      log.error("Command DONT exception " + ex.toString(), ex);
    }
  }

  /**
   * DO
   *
   * @param code, the optional command code
   */

  public void execDO(int code)
  {
    final String response = execCommand(TCC.DO, code, null);
    try {
      telnetOutputStream.writeCommand(response);
    } catch (final IOException ex) {
      log.error("Command DO exception " + ex.toString(), ex);
    }
  }

  /**
   * WONT
   *
   * @param code, the optional command code
   */

  public void execWONT(int code)
  {
    final String response = execCommand(TCC.WONT, code, null);
    try {
      telnetOutputStream.writeCommand(response);
    } catch (final IOException ex) {
      log.error("Command WONT exception " + ex.toString(), ex);
    }
  }

  /**
   * WILL
   *
   * @param code, the optional command code
   */

  public void execWILL(int code)
  {
    final String response = execCommand(TCC.WILL, code, null);
    try {
      telnetOutputStream.writeCommand(response);
    } catch (final IOException ex) {
      log.error("Command WILL exception " + ex.toString(), ex);
    }
  }

  /**
   * On SE command, execute optional sub negotiated command.
   *
   * @param code, the optional command code.
   * @param param, the optional parameters.
   */

  public void execSE(int code, byte[] param)
  {
    final String response = execCommand(TCC.SE, code, param);
    try {
      telnetOutputStream.writeCommand(response);
    } catch (final IOException ex) {
      log.error("Command SE exception " + ex.toString(), ex);
    }
  }

  // Individual command execution code

  /**
   * Execute optional sub command. In the case that there is no support for the
   * option, the WILL and DO will be responded with DONT and WONT respectively,
   * to inform the requestor that the option is not supported.
   *
   * @param action, the negotiation code
   * @param code, the optional command code
   * @param params, the optional parameters
   */
  private String execCommand(int action, int code, byte[] params)
  {
    String response = null;
    final TelnetCommand tc = telnetCommands[code];

    if (tc != null) {
      response = tc.execute(action, code, params);
    } else { // Refuse unknown options
      if (action == TCC.WILL) {
        response =
          TCC.IAC_string + TCC.DONT_string + (char) code;
      }

      if (action == TCC.DO) {
        response =
          TCC.IAC_string + TCC.WONT_string + (char) code;
      }
    }
    return response;
  }

  /**
   * A login procedure (primitive and ugly).
   *
   * @param in Telnetreader
   * @param out Printwriter
   * @return a TelnetLogin object with the result of the login process.
   */
  private TelnetLogin login(TelnetReader in,
                            PrintWriter out,
                            TelnetOutputStream tos)
  {
    final TelnetCommand echo = telnetCommands[TCC.ECHO];

    try {
      out.print("login: ");
      out.flush();

      // 1. Convince the client to do remote echo
      // and make sure the server does echo

      tos.writeCommand(echo.getWILL());
      tos.writeCommand(echo.getDO());

      if (!echo.getDoStatus()) {
        echo.setDoStatus(true);
      }

      enableEcho = true;

      String userName = in.readLine();

      out.print("password: ");
      out.flush();

      // 2. Convince the client to do remote echo
      // but cheat when the server is to do remote echo

      enableEcho = false;

      String password = in.readLine();

      out.println();

      // 3a. Enable the echo in the server again

      enableEcho = true;

      /*
       * Login processing depends on:
       *
       * 1. um is required in the configuration and exist in the system => um is
       * used 2. um is required in the configuration but does NOT exist in the
       * system => login is denied 3. um is not required in configuration => use
       * default user admin, default passwd admin
       */

      final boolean requireUM = telnetConfig.umRequired();
      final String requiredGroup = telnetConfig.getRequiredGroup();
      final String forbiddenGroup = telnetConfig.getForbiddenGroup();

      final ServiceReference<?> sr =
        bc.getServiceReference(PasswdAuthenticator.class.getName());
      TelnetLogin telnetLogin = new TelnetLogin(false, null, userName);

      // Only one of the following if cases (1,2 or 3) is executed

      // 1. um required in configuration and exists in the system

      if (requireUM && sr != null) {
        final PasswdAuthenticator passwdAuthenticator = (PasswdAuthenticator) bc.getService(sr);
        if (passwdAuthenticator == null) {
          log.warn("Failed to get PasswdAuthenticator service.");
          telnetLogin = new TelnetLogin(false, null, userName);
        } else {
          final PasswdSession passwdSession = passwdAuthenticator.createSession();
          passwdSession.setUsername(userName);
          passwdSession.setPassword(password);
          ContextualAuthorization ca = null;

          try {
            ca = passwdSession.getAuthorization();
          } catch (final IllegalStateException e) {
            log.warn("Failed to get UserAdmin service.", e);
          }

          if (ca != null) {
            log.info("ContextualAuthorization used.");
            if (!"".equals(requiredGroup) && !ca.hasRole(requiredGroup)) {
              telnetLogin = new TelnetLogin(false, null, userName);
              log.info("User " + ca.getName() + " has not required group "
                       + requiredGroup);
            } else if (!"".equals(forbiddenGroup) && ca.hasRole(forbiddenGroup)) {
              telnetLogin = new TelnetLogin(false, null, userName);
              log.info("User " + ca.getName() + " is in forbidden group "
                       + forbiddenGroup);
            } else {
              telnetLogin = new TelnetLogin(true, ca, userName);
            }
          }
        }

        bc.ungetService(sr);

        // Set context

        if (telnetLogin.getAuthorization() instanceof ContextualAuthorization) {
          final String inputPath = telnetConfig.getInputPath();
          final String authMethod = telnetConfig.getAuthorizationMethod();

          ((ContextualAuthorization) telnetLogin.getAuthorization())
              .setIPAMContext(inputPath, authMethod);
          final Dictionary<?, ?> context =
            ((ContextualAuthorization) telnetLogin.getAuthorization())
                .getContext();
          log.info("User " + telnetLogin.getAuthorization().getName()
                   + " logged in, authentication context is " + context + ".");
        } else if (telnetLogin.getAuthorization() == null) {
          log.info("Default user " + telnetConfig.getDefaultUser()
                   + " not logged in.");
        } else {
          log.info("User " + telnetLogin.getAuthorization().getName()
                   + " not logged in.");
        }
      }

      // 2. um required in configuration but does NOT exists in the system
      // =>
      // login is always denied

      if (requireUM && sr == null) {
        log.warn("User management required but not available, login denied");
        telnetLogin = new TelnetLogin(false, null, userName);
      }

      // 3. um is not required in configuration =>
      // use default user and password

      if (!requireUM) {
        if (telnetConfig.getDefaultUser().equals(userName)
            && telnetConfig.getDefaultPassword().equals(password)) {
          telnetLogin = new TelnetLogin(true, null, userName);
        } else {
          telnetLogin = new TelnetLogin(false, null, userName);
        }
      }
      return telnetLogin;
    } catch (final Exception e) {
      log.error("Login error", e);
    }

    log.error("failed to login");
    return new TelnetLogin(false, null, "noone");
  }

  /**
   * Initial option setup
   *
   */
  private void initialNegotiation(TelnetOutputStream telnetOutputStream)
  {
    try {
      Thread.sleep(20);
    } catch (final Exception e) {
      log.error("Fail during Thread sleep" + e.toString(), e);
    }

    // Offer all telnet options that should be shown.
    for (final TelnetCommand telnetCommand : telnetCommands) {
      if (telnetCommand != null && telnetCommand.getShow()) {
        try {
          telnetOutputStream.writeCommand(telnetCommand.getWILL());
        } catch (final IOException e) {
          log.error("Fail during initial option negotiation" + e.toString(), e);
        }
      }
    }
  }

  /*------------------------------------------------------------------------*
   *			  ServiceTrackerCustomizer implementation
   *------------------------------------------------------------------------*/

  @Override
  public ConsoleService addingService(ServiceReference<ConsoleService> reference)
  {
    if (null == consoleService) {
      consoleService = bc.getService(reference);
      return consoleService;
    } else {
      return null;
    }
  }

  @Override
  public void modifiedService(ServiceReference<ConsoleService> reference,
                              ConsoleService service)
  {
  }

  @Override
  public void removedService(ServiceReference<ConsoleService> reference,
                             ConsoleService service)
  {
    if (consoleService == service) {
      if (null != session) {
        log.info("Console service closed.");
        printWriter.println("Console service closed,"
                            + " terminating telnet session.");
        printWriter.flush();
        close();
      }
      consoleService = null;
    }
  }
} // TelnetSession
