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

package org.knopflerfish.bundle.http;

import java.net.MalformedURLException;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.osgi.service.http.HttpContext;

public class ResourceRegistration implements Registration {

    // private fields

    private final String alias;

    private final HttpContext httpContext;

    private final ServletContextManager contextManager;

    private final ServletContext context;

    private final ServletConfig config;

    // HACK CSM
    private long lastModificationDate;

    // constructors

    // HACK CSM
    public ResourceRegistration(final String alias,
                                final String realPath,
                                final HttpContext httpContext,
                                final ServletContextManager contextManager,
                                long newDate)
    {
        this.alias = alias;
        this.httpContext = httpContext;
        this.contextManager = contextManager;
        //HACK CSM
        lastModificationDate = newDate;

        context = contextManager.getServletContext(httpContext, realPath);
        config = new ServletConfigImpl(new Hashtable(), context);
    }

    // private methods

    private boolean exists(String path) {

        if (path.startsWith(alias)) {
            try {
                return context.getResource(HttpUtil.makeTarget(path, alias)) != null;
            } catch (MalformedURLException ignore) {
            }
        }

        return false;
    }

    // implements Registration

    public RequestDispatcherImpl getRequestDispatcher(String uri) {

        if (!exists(uri))
            return null;

        RequestDispatcherImpl dispatcher = new RequestDispatcherImpl(alias,
                null, httpContext, config, lastModificationDate);
        dispatcher.setURI(uri);

        return dispatcher;
    }

    // HACK CSM
    public long getLastModificationDate() {
        return lastModificationDate;
    }

    public void destroy() {
        contextManager.ungetServletContext(context);
    }

} // ResourceRegistration
