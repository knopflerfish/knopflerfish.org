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

import java.util.Dictionary;
import java.util.Vector;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class HttpServiceImpl
  implements HttpService
{
    // public constants

    public final static String[] HTTP_INTERFACES
      = new String[] { HttpService.class.getName() };

    // private fields

    private final Bundle bundle;

    private final LogRef log;

    private final Registrations registrations;

    private final ServletContextManager contextManager;

    private final Vector bundleRegistrations = new Vector();

    private boolean closed = false;

    private HttpContext defaultBundleContext = null;

    // constructors

    public HttpServiceImpl(final Bundle bundle, final LogRef log,
                           final Registrations registrations,
                           final ServletContextManager contextManager)
    {
        this.bundle = bundle;
        this.log = log;
        this.registrations = registrations;
        this.contextManager = contextManager;
    }

    // public methods

    public void register(String alias, Registration registration)
            throws NamespaceException {

        int length = alias.length();

        if (length == 0 || alias.charAt(0) != '/')
            throw new IllegalArgumentException(
                    "The alias parameter must begin with slash: " + alias);

        if (length > 1 && alias.charAt(length - 1) == '/')
            throw new IllegalArgumentException(
                    "The alias parameter must not end with slash: " + alias);

        if (registrations.get(alias) != null)
            throw new NamespaceException("The alias is already in use: "
                    + alias);

        bundleRegistrations.addElement(alias);
        registrations.put(alias, registration);

        if (log.doDebug())
            log.debug("Alias \"" + alias + "\" was registered by bundle "
                    + bundle.getBundleId());
    }

    public void unregister(String alias, boolean destroy) {

        Registration registration = registrations.remove(alias);
        bundleRegistrations.removeElement(alias);

        if (destroy)
            registration.destroy();

        if (log.doDebug())
            log.debug("Alias \"" + alias + "\" was unregistered by bundle "
                    + bundle.getBundleId());
    }

    public void unregisterBundle() {

        closed = true;

        while (!bundleRegistrations.isEmpty())
            unregister((String) bundleRegistrations.lastElement(), true);
    }

    // implements HttpService

    public void registerResources(String alias,
                                  String realPath,
                                  HttpContext httpContext)
        throws NamespaceException
    {
        if (closed)
            throw new IllegalStateException("Service has been unget");

        if (realPath.length() > 0
                && realPath.charAt(realPath.length() - 1) == '/')
            throw new IllegalArgumentException(
                    "The name parameter must not end with slash: " + realPath);

        if (httpContext == null) {
            if (defaultBundleContext == null)
                defaultBundleContext = createDefaultHttpContext();
            httpContext = defaultBundleContext;
        }

        //HACK CSM now caching "last updated" time
        register(alias, new ResourceRegistration(alias, realPath, httpContext,
                                                 contextManager,
                                                 System.currentTimeMillis()));
    }

    public void registerServlet(String alias,
                                Servlet servlet,
                                Dictionary parameters,
                                HttpContext httpContext)
        throws NamespaceException, ServletException
    {
        if (closed)
            throw new IllegalStateException("Service has been unget");

        if (servlet == null)
            throw new IllegalArgumentException("Servlet parameter is null");

        if (httpContext == null) {
            if (defaultBundleContext == null)
                defaultBundleContext = createDefaultHttpContext();
            httpContext = defaultBundleContext;
        }

        Registration registration = null;
        try {
            registration = new ServletRegistration(alias, servlet, parameters,
                    httpContext, contextManager, registrations);
            register(alias, registration);
        } catch (NamespaceException ne) {
            if (registration != null)
                registration.destroy();
            throw ne;
        } catch (ServletException se) {
            if (registration != null)
                registration.destroy();
            throw se;
        } catch (RuntimeException re) {
            if (registration != null)
                registration.destroy();
            throw re;
        }
    }

    public void unregister(String alias)
    {
        if (closed)
            throw new IllegalStateException("Service has been unget");

        if (registrations.get(alias) == null)
            throw new IllegalArgumentException("The alias was not registered: "
                    + alias);

        if (!bundleRegistrations.contains(alias))
            throw new IllegalArgumentException(
                    "The alias was not registered by the calling bundle: "
                            + alias);

        unregister(alias, true);
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContext(bundle);
    }

} // HttpServiceImpl
