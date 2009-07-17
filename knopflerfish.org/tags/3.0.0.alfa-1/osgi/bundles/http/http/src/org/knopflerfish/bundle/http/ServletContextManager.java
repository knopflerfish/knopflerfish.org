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
import java.util.Hashtable;

import javax.servlet.ServletContext;

import org.knopflerfish.service.log.LogRef;
import org.osgi.service.http.HttpContext;

public class ServletContextManager {

    // private classes

    private final static class ContextKey {
        int referenceCount = 1;

        final HttpContext httpContext;

        final String realPath;

        ContextKey(HttpContext httpContext, String realPath) {
            this.httpContext = httpContext;
            this.realPath = realPath;
        }

        public int hashCode() {
            return httpContext.hashCode() ^ realPath.hashCode();
        }

        public boolean equals(Object object) {
            if (object instanceof ContextKey) {
                ContextKey key = (ContextKey) object;
                return httpContext.equals(key.httpContext)
                        && realPath.equals(key.realPath);
            }
            return false;
        }
    }

    // private fields

    private final Dictionary keyMap = new Hashtable();

    private final Dictionary contextMap = new Hashtable();

    private final HttpConfig httpConfig;

    private final LogRef log;

    private final Registrations registrations;

    // constructors

    public ServletContextManager(final HttpConfig httpConfig, final LogRef log,
            final Registrations registrations) {

        this.httpConfig = httpConfig;
        this.log = log;
        this.registrations = registrations;
    }

    // public methods

    public synchronized ServletContext getServletContext(
            final HttpContext httpContext, String realPath) {

        if (realPath == null)
            realPath = "";

        final ContextKey key = new ContextKey(httpContext, realPath);
        ServletContextImpl context = (ServletContextImpl) contextMap.get(key);
        if (context == null) {
            context = new ServletContextImpl(httpContext, realPath, httpConfig,
                    log, registrations);
            keyMap.put(context, key);
            contextMap.put(key, context);
        } else {
            ((ContextKey) keyMap.get(context)).referenceCount++;
        }

        return context;
    }

    public synchronized void ungetServletContext(ServletContext context) {

        final ContextKey key = (ContextKey) keyMap.get(context);
        if (key != null && --key.referenceCount <= 0) {
            contextMap.remove(key);
            keyMap.remove(context);
        }
    }

} // ServletContextManager
