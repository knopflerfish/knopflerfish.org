/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/bundles/http/http/src/org/osgi/service/http/HttpService.java,v 1.1.1.1 2004/03/05 20:35:10 wistrand Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2000, 2002).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.service.http;

import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 * The Http Service allows other bundles in the OSGi environment to dynamically
 * register resources and servlets into the URI namespace of Http Service. A
 * bundle may later unregister its resources or servlets.
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 * @see HttpContext
 */
public abstract interface HttpService {
    /**
     * Registers a servlet into the URI namespace.
     * 
     * <p>
     * The alias is the name in the URI namespace of the Http Service at which
     * the registration will be mapped.
     * 
     * <p>
     * An alias must begin with slash ('/') and must not end with slash ('/'),
     * with the exception that an alias of the form &quot;/&quot; is used to
     * denote the root alias. See the specification text for details on how HTTP
     * requests are mapped to servlet and resource registrations.
     * 
     * <p>
     * The Http Service will call the servlet's <tt>init</tt> method before
     * returning.
     * 
     * <pre>
     * httpService.registerServlet(&quot;/myservlet&quot;, servlet, initparams, context);
     * </pre>
     * 
     * <p>
     * Servlets registered with the same <tt>HttpContext</tt> object will
     * share the same <tt>ServletContext</tt>. The Http Service will call the
     * <tt>context</tt> argument to support the <tt>ServletContext</tt>
     * methods <tt>getResource</tt>, <tt>getResourceAsStream</tt> and
     * <tt>getMimeType</tt>, and to handle security for requests. If the
     * <tt>context</tt> argument is <tt>null</tt>, a default
     * <tt>HttpContext</tt> object is used (see
     * {@link #createDefaultHttpContext}).
     * 
     * @param alias
     *            name in the URI namespace at which the servlet is registered
     * @param servlet
     *            the servlet object to register
     * @param initparams
     *            initialization arguments for the servlet or <tt>null</tt> if
     *            there are none. This argument is used by the servlet's
     *            <tt>ServletConfig</tt> object.
     * @param context
     *            the <tt>HttpContext</tt> object for the registered servlet,
     *            or <tt>null</tt> if a default <tt>HttpContext</tt> is to
     *            be created and used.
     * @exception NamespaceException
     *                if the registration fails because the alias is already in
     *                use.
     * @exception javax.servlet.ServletException
     *                if the servlet's <tt>init</tt> method throws an
     *                exception, or the given servlet object has already been
     *                registered at a different alias.
     * @exception java.lang.IllegalArgumentException
     *                if any of the arguments are invalid
     */
    public abstract void registerServlet(String alias, Servlet servlet,
            Dictionary initparams, HttpContext context)
            throws ServletException, NamespaceException;

    /**
     * Registers resources into the URI namespace.
     * 
     * <p>
     * The alias is the name in the URI namespace of the Http Service at which
     * the registration will be mapped. An alias must begin with slash ('/') and
     * must not end with slash ('/'), with the exception that an alias of the
     * form &quot;/&quot; is used to denote the root alias. The name parameter
     * must also not end with slash ('/'). See the specification text for
     * details on how HTTP requests are mapped to servlet and resource
     * registrations.
     * <p>
     * For example, suppose the resource name /tmp is registered to the alias
     * /files. A request for /files/foo.txt will map to the resource name
     * /tmp/foo.txt.
     * 
     * <pre>
     * httpservice.registerResources(&quot;/files&quot;, &quot;/tmp&quot;, context);
     * </pre>
     * 
     * The Http Service will call the <tt>HttpContext</tt> argument to map
     * resource names to URLs and MIME types and to handle security for
     * requests. If the <tt>HttpContext</tt> argument is <tt>null</tt>, a
     * default <tt>HttpContext</tt> is used (see
     * {@link #createDefaultHttpContext}).
     * 
     * @param alias
     *            name in the URI namespace at which the resources are
     *            registered
     * @param name
     *            the base name of the resources that will be registered
     * @param context
     *            the <tt>HttpContext</tt> object for the registered
     *            resources, or <tt>null</tt> if a default
     *            <tt>HttpContext</tt> is to be created and used.
     * @exception NamespaceException
     *                if the registration fails because the alias is already in
     *                use.
     * @exception java.lang.IllegalArgumentException
     *                if any of the parameters are invalid
     */
    public abstract void registerResources(String alias, String name,
            HttpContext context) throws NamespaceException;

    /**
     * Unregisters a previous registration done by <tt>registerServlet</tt> or
     * <tt>registerResources</tt> methods.
     * 
     * <p>
     * After this call, the registered alias in the URI name-space will no
     * longer be available. If the registration was for a servlet, the Http
     * Service must call the <tt>destroy</tt> method of the servlet before
     * returning.
     * <p>
     * If the bundle which performed the registration is stopped or otherwise
     * "unget"s the Http Service without calling {@link #unregister} then Http
     * Service must automatically unregister the registration. However, if the
     * registration was for a servlet, the <tt>destroy</tt> method of the
     * servlet will not be called in this case since the bundle may be stopped.
     * {@link #unregister} must be explicitly called to cause the
     * <tt>destroy</tt> method of the servlet to be called. This can be done
     * in the {@link org.osgi.framework.BundleActivator#stop} method of the
     * bundle registering the servlet.
     * 
     * @param alias
     *            name in the URI name-space of the registration to unregister
     * @exception java.lang.IllegalArgumentException
     *                if there is no registration for the alias or the calling
     *                bundle was not the bundle which registered the alias.
     */
    public abstract void unregister(String alias);

    /**
     * Creates a default <tt>HttpContext</tt> for registering servlets or
     * resources with the HttpService, a new <tt>HttpContext</tt> object is
     * created each time this method is called.
     * 
     * <p>
     * The behavior of the methods on the default <tt>HttpContext</tt> is
     * defined as follows:
     * <ul>
     * <li><tt>getMimeType</tt> - Does not define any customized MIME types
     * for the Content-Type header in the response, and always returns
     * <tt>null</tt>.
     * <li><tt>handleSecurity</tt> - Performs implementation-defined
     * authentication on the request.
     * <li><tt>getResource</tt> - Assumes the named resource is in the
     * context bundle; this method calls the context bundle's
     * <tt>Bundle.getResource</tt> method, and returns the appropriate URL to
     * access the resource. On a Java runtime environment that supports
     * permissions, the Http Service needs to be granted the
     * <tt>org.osgi.framework.AdminPermission</tt>.
     * </ul>
     * 
     * @return a default <tt>HttpContext</tt> object.
     * @since 1.1
     */
    public abstract HttpContext createDefaultHttpContext();
}
