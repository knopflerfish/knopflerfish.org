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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.service.http.HttpContext;

public class ServletRegistration implements Registration {

    // private fields

    private final ServletContextManager contextManager;

    private final Registrations registrations;

    private final RequestDispatcherImpl dispatcher;

    // constructors

    public ServletRegistration(final String alias, final Servlet servlet,
            Dictionary parameters, final HttpContext httpContext,
            final ServletContextManager contextManager,
            final Registrations registrations) throws ServletException {

        if (parameters == null)
            parameters = new Hashtable();

        if (parameters.get(HttpUtil.SERVLET_NAME_KEY) == null)
            parameters.put(HttpUtil.SERVLET_NAME_KEY, servlet.getClass()
                    .getName());

        this.contextManager = contextManager;
        this.registrations = registrations;

        final ServletContext context = contextManager.getServletContext(
                httpContext, null);
        final ServletConfig config = new ServletConfigImpl(parameters, context);
        servlet.init(config);

        dispatcher = new RequestDispatcherImpl(alias, servlet, httpContext);

        registrations.addServlet(servlet);
    }

    // implements Registration

    public RequestDispatcherImpl getRequestDispatcher(final String uri) {

        dispatcher.setURI(uri);

        return dispatcher;
    }

    public void destroy() {

        final Servlet servlet = dispatcher.getServlet();
        final ServletContext context = servlet.getServletConfig()
                .getServletContext();
        servlet.destroy();
        contextManager.ungetServletContext(context);

        registrations.removeServlet(servlet);
    }

} // ServletRegistration
