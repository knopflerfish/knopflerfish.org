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

package org.knopflerfish.bundle.consoletelnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Dictionary;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.service.um.useradmin.ContextualAuthorization;
import org.knopflerfish.service.um.useradmin.PasswdAuthenticator;
import org.knopflerfish.service.um.useradmin.PasswdSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Authorization;

/**
 * * This is the session object that binds the socket, * the input and the
 * output streams together. * * Here is where the telnet commands are executed
 * and * where they affect the ConsoleService session. *
 */

public class TelnetSession implements Runnable,
        org.knopflerfish.service.console.SessionListener {

    private Socket socket;

    private TelnetConfig telnetConfig;

    private ConsoleService consoleService;

    private Session s;

    private TelnetServer tserv;

    private TelnetCommand[] telnetCommands = new TelnetCommand[256];

    private InputStream is;

    private OutputStream os;

    private TelnetInputStream telnetInputStream;

    private TelnetOutputStream telnetOutputStream;

    private TelnetReader reader;

    private TelnetLogin telnetLogin = null;

    private PrintWriter printWriter;

    private char mask = '\177'; // Normal 7 bit mask

    private boolean enableEcho = true;

    private LogRef log;

    private BundleContext bc;

    // private String DEFAULT_USER_NAME = "admin";
    // private String DEFAULT_PASSWORD = "admin";

    public TelnetSession(Socket socket, TelnetConfig telnetConfig,
            ConsoleService consoleService, LogRef log, BundleContext bc,
            TelnetServer tserv) {
        this.telnetConfig = telnetConfig;
        this.socket = socket;
        this.consoleService = consoleService;
        this.log = log;
        this.bc = bc;
        this.tserv = tserv;

        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
        } catch (IOException iox) {
            log.error("Session socket opening exception " + iox.toString());
        }

        telnetInputStream = new TelnetInputStream(is, this);
        telnetOutputStream = new TelnetOutputStream(os, this);

        printWriter = new PrintWriter(telnetOutputStream);
        reader = new TelnetReader(telnetInputStream, this);

        // Instantiate the supported options, with default state
        // ts code, do show
        telnetCommands[TCC.ECHO] = new TelnetCommandEcho(this, TCC.ECHO, false,
                false);

        telnetCommands[TCC.SUPGA] = new TelnetCommandSupga(this, TCC.SUPGA,
                false, true);

        telnetCommands[TCC.STATUS] = new TelnetCommandStatus(this, TCC.STATUS,
                false, true);

    }

    public void run() {
        try {
            // Telnet initial option negotiaion
            initialNegotiation(reader, printWriter, telnetOutputStream);

            // Platform login processing

            printWriter.println("Knopflerfish OSGi console");

            telnetLogin = login(reader, printWriter, telnetOutputStream);

            // System.out.println("telnetLogin state: " +
            // telnetLogin.isPermitted());

            if (telnetLogin.isPermitted() == true) {
                Authorization authorization = telnetLogin.getAuthorization();
                s = consoleService.runSession("telnet session", reader,
                        printWriter);
                Dictionary props = s.getProperties();

                if (authorization != null) {
                    props.put(Session.PROPERTY_AUTHORIZATION, authorization);
                }

                printWriter.println("'quit' to end session");

                s.addSessionListener(this);
            } else {
                printWriter.println("Login incorrect");
                printWriter.flush();
                printWriter.close();

                reader.close();
                telnetInputStream.close();
                telnetOutputStream.close();
                is.close();
                os.close();
                socket.close();
                tserv.removeSession(this);
            }
        } catch (IOException ioe) {
            log.error("Session exception " + ioe.toString());
            ioe.printStackTrace();
            tserv.removeSession(this);
        }
    }

    public void sessionEnd(Session s) { // called if the console session
        // terminates
        try {
            close();
        } catch (Exception iox) {
            log.error("Session end exception " + iox.toString());
        }
    }

    public void close() {
        try {
            s.close();
            printWriter.close();
            reader.close();
            telnetInputStream.close();
            telnetOutputStream.close();
            is.close();
            os.close();
            socket.close();
            log.info("User " + telnetLogin.getUser() + " logged out");
            tserv.removeSession(this);
            s = null;
        } catch (IOException iox) {
            log.error("Session close exception " + iox.toString());
        }
    }

    /**
     * * Get the character mask * This is to support binary mode, * that is 7 or
     * 8 bit data in the output data stream
     */

    public char getMask() {
        return mask;
    }

    /**
     * * Get all instantiated commands in this session
     */

    public TelnetCommand[] getCommands() {
        return telnetCommands;
    }

    /**
     * * Get the TelnetOutputStream
     */

    public TelnetOutputStream getTelnetOutputStream() {
        return telnetOutputStream;
    }

    /**
     * * Method to do echo to the output stream * This also looks at the
     * enableEcho flag
     */

    public void echoChar(int character) {
        TelnetCommand tc = telnetCommands[TCC.ECHO];

        // System.out.println("echo=" + character + ", enable=" + enableEcho);
        if (tc != null) {
            if (tc.getDoStatus() && enableEcho) {
                printWriter.print((char) character);
                printWriter.flush();
            }
        }
    }

    public void execSB() {
    }

    public void execGA() {
    }

    public void execEL() {
    }

    public void execEC() {
    }

    public void execAYT() {
        try {
            telnetOutputStream.writeCommand("["
                    + socket.getLocalAddress().getHostName() + ": yes]");
        } catch (IOException ex) {
            log.error("Command AYT exception " + ex.toString());
        }
    }

    public void execAO() {
        s.abortCommand();
    }

    public void execIP() {
        s.abortCommand();
    }

    public void execBRK() {
        s.abortCommand();
    }

    public void execDM() {
    }

    public void execNOP() {
    }

    /**
     * * Execution of standard telnet negotiation commmands * DO, DONT, WILL and
     * WONT and also * the execution of the command via SB code .... SE * option
     * command structure. *
     */

    /**
     * * DONT * *
     * 
     * @parameter code, the optional command code
     */

    public void execDONT(int code) {
        String response = execCommand(TCC.DONT, code, null);
        try {
            telnetOutputStream.writeCommand(response);
        } catch (IOException ex) {
            log.error("Command DONT exception " + ex.toString());
        }
    }

    /**
     * * DO * *
     * 
     * @parameter code, the optional command code
     */

    public void execDO(int code) {
        String response = execCommand(TCC.DO, code, null);
        try {
            telnetOutputStream.writeCommand(response);
        } catch (IOException ex) {
            log.error("Command DO exception " + ex.toString());
        }
    }

    /**
     * * WONT * *
     * 
     * @parameter code, the optional command code
     */

    public void execWONT(int code) {
        String response = execCommand(TCC.WONT, code, null);
        try {
            telnetOutputStream.writeCommand(response);
        } catch (IOException ex) {
            log.error("Command WONT exception " + ex.toString());
        }
    }

    /**
     * * WILL * *
     * 
     * @parameter code, the optional command code
     */

    public void execWILL(int code) {
        String response = execCommand(TCC.WILL, code, null);
        try {
            telnetOutputStream.writeCommand(response);
        } catch (IOException ex) {
            log.error("Command WILL exception " + ex.toString());
        }
    }

    /**
     * * On SE command, execute optional sub negotiated command. * *
     * 
     * @parameter code, the optional command code *
     * @parameter param, the optional parameters
     */

    public void execSE(int code, byte[] param) {
        String response = execCommand(TCC.SE, code, param);
        try {
            telnetOutputStream.writeCommand(response);
        } catch (IOException ex) {
            log.error("Command SE exception " + ex.toString());
        }
    }

    // Individual command execution code

    /**
     * * Execute optional sub command. * * In the case that there is no support
     * for the option, * the WILL and DO will be responded with DONT and WONT *
     * respectively, to inform the requestor that the * option is not supported. * *
     * 
     * @parameter action, the negotiation code *
     * @parameter code, the optional command code *
     * @parameter params, the optional parameters
     */

    private String execCommand(int action, int code, byte[] params) {
        String response = null;
        TelnetCommand tc = telnetCommands[code];

        if (tc != null) {
            response = tc.execute(action, code, params);
        } else { // Refuse unknown options
            if (action == TCC.WILL) {
                response = TCC.IAC_string + TCC.DONT_string
                        + String.valueOf((char) code);
            }

            if (action == TCC.DO) {
                response = TCC.IAC_string + TCC.WONT_string
                        + String.valueOf((char) code);
            }
        }
        return response;
    }

    /**
     * * A login procedure (primitive and ugly) * *
     * 
     * @parameter in Telnetreader *
     * @parameter out Printwriter *
     * @return a TelnetLogin object with the result of the login process. *
     */

    private TelnetLogin login(TelnetReader in, PrintWriter out,
            TelnetOutputStream tos) {
        String userName = null;
        String password = null;
        TelnetCommand echo = telnetCommands[TCC.ECHO];

        try {

            // System.out.println("TelnetLogin.login()");

            out.print("login: ");
            out.flush();

            // 1. Convince the client to do remote echo
            // and make sure the server does echo

            tos.writeCommand(echo.getWILL());
            tos.writeCommand(echo.getDO());

            if (echo.getDoStatus() == false) {
                echo.setDoStatus(true);
            }

            enableEcho = true;

            userName = in.readLine();

            out.print("password: ");
            out.flush();

            // 2. Convince the client to do remote echo
            // but cheat when the server is to do remote echo

            enableEcho = false;

            // out.println();

            password = in.readLine();

            out.println();

            // 3a. Enable the echo in the server again

            enableEcho = true;

            /*
             * Login processing depends on:
             * 
             * 1. um is required in the configuration and exist in the system =>
             * um is used 2. um is required in the configuration but does NOT
             * exist in the system => login is denied 3. um is not required in
             * configuration => use default user admin, default passwd admin
             */

            boolean requireUM = telnetConfig.umRequired();
            String requiredGroup = telnetConfig.getRequiredGroup();
            String forbiddenGroup = telnetConfig.getForbiddenGroup();

            ServiceReference sr = bc
                    .getServiceReference(PasswdAuthenticator.class.getName());
            TelnetLogin telnetLogin = new TelnetLogin(false, null, userName);

            // Only one of the following if cases (1,2 or 3) is executed

            // 1. um required in configuration and exists in the system

            if (requireUM && sr != null) {
                // System.out.println("require UM = true, sr != null");
                PasswdAuthenticator pa = (PasswdAuthenticator) bc
                        .getService(sr);
                if (pa == null) {
                    log.warn("Failed to get PasswdAuthenticator service.");
                    telnetLogin = new TelnetLogin(false, null, userName);
                } else {
                    PasswdSession ps = pa.createSession();
                    ps.setUsername(userName);
                    ps.setPassword(password);
                    ContextualAuthorization ca = null;

                    try {
                        ca = ps.getAuthorization();
                    } catch (IllegalStateException ex) {
                        log.warn("Failed to get UserAdmin service.");
                    }

                    if (ca != null) {
                        log.info("ContextualAuthorization used.");
                        if (!"".equals(requiredGroup)
                                && !ca.hasRole(requiredGroup)) {
                            telnetLogin = new TelnetLogin(false, null, userName);
                            log.info("User " + ca.getName()
                                    + " has not required group "
                                    + requiredGroup);
                        } else if (!"".equals(forbiddenGroup)
                                && ca.hasRole(forbiddenGroup)) {
                            telnetLogin = new TelnetLogin(false, null, userName);
                            log.info("User " + ca.getName()
                                    + " is in forbidden group "
                                    + forbiddenGroup);
                        } else {
                            telnetLogin = new TelnetLogin(true, ca, userName);
                        }
                    }
                }

                bc.ungetService(sr);

                // Set context

                if (telnetLogin.getAuthorization() instanceof ContextualAuthorization) {
                    String inputPath = telnetConfig.getInputPath();
                    String authMethod = telnetConfig.getAuthorizationMethod();

                    ((ContextualAuthorization) telnetLogin.getAuthorization())
                            .setIPAMContext(inputPath, authMethod);
                    Dictionary context = ((ContextualAuthorization) telnetLogin
                            .getAuthorization()).getContext();
                    log.info("User " + telnetLogin.getAuthorization().getName()
                            + " logged in, authentication context is "
                            + context + ".");
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
                log
                        .warn("User management required but not available, login denied");
                telnetLogin = new TelnetLogin(false, null, userName);
            }

            // 3. um is not required in configuration =>
            // use default user and password

            if (requireUM == false) {
                if (telnetConfig.getDefaultUser().equals(userName)
                        && telnetConfig.getDefaultPassword().equals(password)) {
                    telnetLogin = new TelnetLogin(true, null, userName);
                } else {
                    telnetLogin = new TelnetLogin(false, null, userName);
                }
            }
            return telnetLogin;
        } catch (Exception e) {
            log.error("Login error", e);
        }

        log.error("failed to login");
        return new TelnetLogin(false, null, "noone");
    }

    /**
     * * Initial option setup * *
     * 
     * @parameter in, *
     * @parameter out, *
     */

    private void initialNegotiation(TelnetReader in, PrintWriter out,
            TelnetOutputStream tos) {
        try {
            Thread.sleep(20);
        } catch (Exception ex) {
            log.error("Fail during Thread sleep" + ex.toString());
        }

        // Offer all telnet options that should be shown.
        for (int i = 0; i < telnetCommands.length; i++) {
            TelnetCommand tc = telnetCommands[i];
            if (tc != null && tc.getShow()) {
                try {
                    tos.writeCommand(tc.getWILL());
                } catch (IOException ex) {
                    log.error("Fail during initial option negotiation"
                            + ex.toString());
                }
            }
        }
    }
} // TelnetSession
