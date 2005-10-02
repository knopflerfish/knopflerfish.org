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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.knopflerfish.service.log.LogRef;

public class Transaction implements Runnable, PoolableObject {

    // private fields

    private HttpConfigWrapper httpConfig;

    private final Registrations registrations;

    private final ObjectPool requestPool;

    private final ObjectPool responsePool;

    private InputStream is = null;

    private OutputStream os = null;

    // protected fields

    protected LogRef log;

    protected Socket client = null;

    // constructors

    public Transaction(final LogRef log, final Registrations registrations,
            final ObjectPool requestPool, final ObjectPool responsePool) {

        this.log = log;
        this.registrations = registrations;
        this.requestPool = requestPool;
        this.responsePool = responsePool;
    }

    // public methods

    public void init(final Socket client, final HttpConfigWrapper httpConfig) {
        this.httpConfig = httpConfig;
        this.client = client;
    }

    public void init(final InputStream is, final OutputStream os,
            final HttpConfigWrapper httpConfig) {
        this.httpConfig = httpConfig;
        this.is = is;
        this.os = os;
    }

    // implements Runnable

    public void run() {

        final RequestImpl request = (RequestImpl) requestPool.get();
        final ResponseImpl response = (ResponseImpl) responsePool.get();

        InetAddress remoteAddress = null;

        try {

            if (client != null) {
                client.setSoTimeout(1000 * httpConfig.getConnectionTimeout());
                is = client.getInputStream();
                os = client.getOutputStream();
                remoteAddress = client.getInetAddress();
            }

            while (true) {

                try {

                    request.init(is, remoteAddress, httpConfig);
                    response.init(os, request, httpConfig);

                    String uri = request.getRequestURI();
                    RequestDispatcherImpl dispatcher = registrations
                            .getRequestDispatcher(uri);
                    if (dispatcher != null) {
                        try {
                            request.setServletPath(dispatcher.getServletPath());
                            dispatcher.forward(request, response);
                        } catch (ServletException se) {
                            response
                                    .sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }

                } catch (HttpException he) {

                    response.init(os, httpConfig);
                    response.sendError(he.getCode(), he.getMessage());
                }

                response.commit();

                if (response.getKeepAlive()) {
                    InputStream is = request.getRawInputStream();
                    if (is != null && is.markSupported()) {
                        is.mark(4);
                        if (is.read() != '\r' || is.read() != '\n')
                            is.reset();
                    }
                } else
                    break;

                request.destroy();
                response.destroy();
            }

        } catch (SocketException se) {
            // ignore: client closed socket
        } catch (InterruptedIOException iioe) {
            // ignore: keep alive socket timeout
        } catch (IOException ioe) {
            // ignore: broken pipe
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            if (log.doError())
                log.error("Internal error", t);
            try {
                response.init(os, request, httpConfig);
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal error: " + t);
            } catch (IOException ignore) {
            }
        } finally {

            requestPool.put(request);
            responsePool.put(response);

            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignore) {
                }
            }
            if (os != null) {
                try {
                    // os.flush();
                    os.close();
                } catch (Exception ignore) {
                }
            }
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    // implements PoolableObject

    public void init() {
    }

    public void destroy() {

        client = null;
        is = null;
        os = null;
    }

} // Transaction
