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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

import javax.servlet.http.HttpSession;

public class HttpSessionManager implements Runnable {

    // private constants

    private final static int CHECK_INTERVAL = 60;

    // private fields

    private final HttpConfig httpConfig;

    private final Thread sessionTimeoutThread;

    private static int count = 0;

    private boolean stopped = false;

    private final Dictionary sessions = new Hashtable();

    private final Stack recycledSessions = new Stack();

    // constructors

    public HttpSessionManager(final HttpConfig httpConfig) {

        this.httpConfig = httpConfig;

        sessionTimeoutThread = new Thread(this, "HttpServer-SessionTimeout");
        sessionTimeoutThread.setDaemon(true);
        sessionTimeoutThread.start();
    }

    // private methods

    private void removeSession(HttpSession session) {

        synchronized (sessions) {
            sessions.remove(session.getId());
        }
        try {
            session.invalidate();
        } catch (IllegalStateException ignore) {
        }
    }

    private void reuseSession(HttpSessionImpl session) {

        session.destroy();
        synchronized (recycledSessions) {
            recycledSessions.push(session);
        }
    }

    // public methods

    public HttpSession createHttpSession() {

        HttpSessionImpl session;
        if (recycledSessions.empty()) {
            session = new HttpSessionImpl();
        } else {
            synchronized (recycledSessions) {
                session = (HttpSessionImpl) recycledSessions.pop();
            }
        }

        session.init(count++);
        session.setMaxInactiveInterval(httpConfig.getDefaultSessionTimeout());
        synchronized (sessions) {
            sessions.put(session.getId(), session);
        }

        return session;
    }

    public HttpSession getHttpSession(String sessionId) {

        if (sessionId == null)
            return null;

        HttpSessionImpl session;
        synchronized (sessions) {
            session = (HttpSessionImpl) sessions.get(sessionId);
        }
        if (session != null) {
            synchronized (session) {
                if (session.isExpired()) {
                    removeSession(session);
                    reuseSession(session);
                    session = null;
                } else {
                    session.join();
                }
            }
        }

        return session;
    }

    public void destroy() {

        if (sessionTimeoutThread != null) {
            stopped = true;
            sessionTimeoutThread.interrupt();
            try {
                sessionTimeoutThread.join();
            } catch (InterruptedException ignore) {
            }
        }
    }

    // implements Runnable

    public void run() {

        long sleepTime = CHECK_INTERVAL * 1000;

        while (!stopped) {

            Enumeration sessionEnum;
            synchronized (sessions) {
                sessionEnum = ((Hashtable) ((Hashtable) sessions).clone())
                        .elements();
            }

            while (sessionEnum.hasMoreElements()) {
                HttpSessionImpl session = (HttpSessionImpl) sessionEnum
                        .nextElement();
                synchronized (session) {
                    if (session.isExpired()) {
                        removeSession(session);
                        reuseSession(session);
                    }
                }
            }

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException ignore) {
            }
        }
    }

} // HttpSessionManager
