/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.axis.transport.http;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.server.AxisServer;
import org.apache.axis.session.Session;
import org.apache.axis.session.SimpleSession;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.Options;
import org.apache.commons.logging.Log;

import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;


import org.knopflerfish.bundle.axis.Activator;

/**
 * This is a simple implementation of an HTTP server for processing
 * SOAP requests via Apache's xml-axis.  This is not intended for production
 * use.  Its intended uses are for demos, debugging, and performance
 * profiling.
 *
 * @author Sam Ruby (ruby@us.ibm.com)
 * @author Rob Jellinghaus (robj@unrealities.com)
 */
public class SimpleAxisServer implements Runnable {
    protected static Log log =
            LogFactory.getLog(SimpleAxisServer.class.getName());

    // session state.
    // This table maps session keys (random numbers) to SimpleAxisSession objects.
    //
    // There is NO CLEANUP of this table at present, and if clients are not
    // passing cookies, then a new session will be created for *every* request.
    // This is the biggest impediment to any kind of real SimpleAxisServer use.
    // So, if this becomes objectionable, we will implement some simpleminded
    // cleanup (perhaps just a cap on max # of sessions, and some kind of LRU
    // cleanup policy).
    private Hashtable sessions = new Hashtable();

    // Are we doing threads?
    private static boolean doThreads = true;

    // Are we doing sessions?
    // Set this to false if you don't want any session overhead.
    private static boolean doSessions = true;

    protected boolean isSessionUsed() {
        return doSessions;
    }

    public void setDoThreads(boolean value) {
        doThreads = value ;
    }

    public boolean getDoThreads() {
        return doThreads ;
    }

    protected Session createSession(String cooky) {
        // is there a session already?
        Session session = null;
        if (sessions.containsKey(cooky)) {
            session = (Session) sessions.get(cooky);
        } else {
            // no session for this cooky, bummer
            session = new SimpleSession();

            // ADD CLEANUP LOGIC HERE if needed
            sessions.put(cooky, session);
        }
        return session;
    }

    // What is our current session index?
    // This is a monotonically increasing, non-thread-safe integer
    // (thread safety not considered crucial here)
    public static int sessionIndex = 0;

    // Axis server (shared between instances)

    protected static AxisServer getAxisServer() {
        return Activator.getAxisServer();
    }

    // are we stopped?
    // latch to true if stop() is called
    private boolean stopped = false;

    /**
     * Accept requests from a given TCP port and send them through the
     * Axis engine for processing.
     */
    public void run() {
        log.info(Messages.getMessage("start00", "SimpleAxisServer",
                new Integer(getServerSocket().getLocalPort()).toString()));

        // Accept and process requests from the socket
        while (!stopped) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (java.io.InterruptedIOException iie) {
            } catch (Exception e) {
                log.debug(Messages.getMessage("exception00"), e);
                break;
            }
            if (socket != null) {
                SimpleAxisWorker worker = new SimpleAxisWorker(this, socket);
                if (doThreads) {
                    Thread thread = new Thread(worker);
                    thread.setDaemon(true);
                    thread.start();
                } else {
                    worker.run();
                }
            }
        }
        log.info(Messages.getMessage("quit00", "SimpleAxisServer"));
    }

    // per thread socket information
    private ServerSocket serverSocket;

    /**
     * Obtain the serverSocket that that SimpleAxisServer is listening on.
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * Set the serverSocket this server should listen on.
     * (note : changing this will not affect a running server, but if you
     *  stop() and then start() the server, the new socket will be used).
     */
    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Start this server.
     *
     * Spawns a worker thread to listen for HTTP requests.
     *
     * @param daemon a boolean indicating if the thread should be a daemon.
     */
    public void start(boolean daemon) throws Exception {
        if (doThreads) {
            Thread thread = new Thread(this);
            thread.setDaemon(daemon);
            thread.start();
        } else {
            run();
        }
    }

    /**
     * Start this server as a NON-daemon.
     */
    public void start() throws Exception {
        start(false);
    }

    /**
     * Stop this server.
     *
     * This will interrupt any pending accept().
     */
    public void stop() throws Exception {
        /* 
         * Close the server socket cleanly, but avoid fresh accepts while
         * the socket is closing.
         */
        stopped = true;
        try {
            serverSocket.close();
        } catch (Exception e) {
            log.info(Messages.getMessage("exception00"), e);
        }

        log.info(Messages.getMessage("quit00", "SimpleAxisServer"));

        // Kill the JVM, which will interrupt pending accepts even on linux.
        System.exit(0);
    }

    /**
     * Server process.
     */
    public static void main(String args[]) {

        SimpleAxisServer sas = new SimpleAxisServer();

        Options opts = null;
        try {
            opts = new Options(args);
        } catch (MalformedURLException e) {
            log.error(Messages.getMessage("malformedURLException00"), e);
            return;
        }

        try {
            doThreads = (opts.isFlagSet('t') > 0);

            int port = opts.getPort();
            ServerSocket ss = null;
            // Try five times
            for (int i = 0; i < 5; i++) {
                try {
                    ss = new ServerSocket(port);
                    break;
                } catch (java.net.BindException be){
                    log.debug(Messages.getMessage("exception00"), be);
                    if (i < 4) {
                        // At 3 second intervals.
                        Thread.sleep(3000);
                    } else {
                        throw new Exception(Messages.getMessage("unableToStartServer00",
                                           Integer.toString(port)));
                    }
                }
            }
            sas.setServerSocket(ss);
            sas.start();
        } catch (Exception e) {
            log.error(Messages.getMessage("exception00"), e);
            return;
        }

    }
}
