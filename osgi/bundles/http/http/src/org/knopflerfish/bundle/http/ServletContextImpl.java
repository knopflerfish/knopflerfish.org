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
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.knopflerfish.service.log.LogRef;
import org.osgi.service.http.HttpContext;

public class ServletContextImpl implements ServletContext {

    // private fields

    private final HttpContext httpContext;

    private final String realPath;

    private final HttpConfig httpConfig;

    private final LogRef log;

    private final Registrations registrations;

    private final Attributes attributes = new Attributes();

    // constructors

    ServletContextImpl(final HttpContext httpContext, final String realPath,
            final HttpConfig httpConfig, final LogRef log,
            final Registrations registrations) {

        this.httpContext = httpContext;
        this.realPath = realPath;
        this.httpConfig = httpConfig;
        this.log = log;
        this.registrations = registrations;
    }

    // implements ServletContext

    public ServletContext getContext(final String uri) {
        return null; // NYI: OK
    }

    public int getMajorVersion() {
        return 2;
    }

    public int getMinorVersion() {
        return 1;
    }

    public String getMimeType(final String file) {

        String mimeType = httpContext.getMimeType(file);

        if (mimeType == null)
            mimeType = httpConfig.getMimeType(file);

        return mimeType;
    }

    public URL getResource(final String path) {
        return httpContext.getResource(realPath + path);
    }

    public InputStream getResourceAsStream(final String path) {

        final URL url = getResource(path);
        if (url == null) {
            return null;
        }
        try {
            return url.openStream();
        } catch (IOException ioe) {
            return null;
        }
    }

    public RequestDispatcher getRequestDispatcher(final String uri) {
        return registrations.getRequestDispatcher(uri);
    }

    public RequestDispatcher getNamedDispatcher(final String name) {
        return null; // NYI: OK, but could be implemented
    }

    public Servlet getServlet(final String name) {
        return null; // deprecated
    }

    public Enumeration getServlets() {
        return HttpUtil.EMPTY_ENUMERATION; // deprecated
    }

    public Enumeration getServletNames() {
        return HttpUtil.EMPTY_ENUMERATION; // deprecated
    }

    public void log(final String message) {
        if (log.doInfo())
            log.info(message);
    }

    public void log(final Exception exception, final String message) {
        log(message, exception); // deprecated
    }

    public void log(final String message, final Throwable throwable) {
        if (log.doWarn())
            log.warn(message, throwable);
    }

    public String getRealPath(final String path) {
        return null;
    }

    public String getServerInfo() {
        return httpConfig.getServerInfo();
    }

    public String getInitParameter(final String name) {
        return null; // NYI: OK
    }

    public Enumeration getInitParameterNames() {
        return HttpUtil.EMPTY_ENUMERATION; // NYI: OK
    }

    public Object getAttribute(final String name) {
        return attributes.getAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return attributes.getAttributeNames();
    }

    public void setAttribute(final String name, final Object value) {
        attributes.setAttribute(name, value);
    }

    public void removeAttribute(final String name) {
        attributes.removeAttribute(name);
    }

} // ServletContextImpl
