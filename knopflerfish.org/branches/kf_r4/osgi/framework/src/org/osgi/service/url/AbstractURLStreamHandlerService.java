/*
 * $Header: /cvshome/build/org.osgi.service.url/src/org/osgi/service/url/AbstractURLStreamHandlerService.java,v 1.6 2005/05/13 20:32:35 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2002, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.service.url;

import java.net.*;

/**
 * Abstract implementation of the <code>URLStreamHandlerService</code> interface.
 * All the methods simply invoke the corresponding methods on
 * <code>java.net.URLStreamHandler</code> except for <code>parseURL</code> and
 * <code>setURL</code>, which use the <code>URLStreamHandlerSetter</code>
 * parameter. Subclasses of this abstract class should not need to override the
 * <code>setURL</code> and <code>parseURL(URLStreamHandlerSetter,...)</code>
 * methods.
 * 
 * @version $Revision: 1.6 $
 */
public abstract class AbstractURLStreamHandlerService extends URLStreamHandler
		implements URLStreamHandlerService {
	/**
	 * @see "java.net.URLStreamHandler.openConnection"
	 */
	public abstract URLConnection openConnection(URL u)
			throws java.io.IOException;

	/**
	 * The <code>URLStreamHandlerSetter</code> object passed to the parseURL
	 * method.
	 */
	protected URLStreamHandlerSetter	realHandler;

	/**
	 * Parse a URL using the <code>URLStreamHandlerSetter</code> object. This
	 * method sets the <code>realHandler</code> field with the specified
	 * <code>URLStreamHandlerSetter</code> object and then calls
	 * <code>parseURL(URL,String,int,int)</code>.
	 * 
	 * @param realHandler The object on which the <code>setURL</code> method must
	 *        be invoked for the specified URL.
	 * @see "java.net.URLStreamHandler.parseURL"
	 */
	public void parseURL(URLStreamHandlerSetter realHandler, URL u,
			String spec, int start, int limit) {
		this.realHandler = realHandler;
		parseURL(u, spec, start, limit);
	}

	/**
	 * This method calls <code>super.toExternalForm</code>.
	 * 
	 * @see "java.net.URLStreamHandler.toExternalForm"
	 */
	public String toExternalForm(URL u) {
		return super.toExternalForm(u);
	}

	/**
	 * This method calls <code>super.equals(URL,URL)</code>.
	 * 
	 * @see "java.net.URLStreamHandler.equals(URL,URL)"
	 */
	public boolean equals(URL u1, URL u2) {
		return super.equals(u1, u2);
	}

	/**
	 * This method calls <code>super.getDefaultPort</code>.
	 * 
	 * @see "java.net.URLStreamHandler.getDefaultPort"
	 */
	public int getDefaultPort() {
		return super.getDefaultPort();
	}

	/**
	 * This method calls <code>super.getHostAddress</code>.
	 * 
	 * @see "java.net.URLStreamHandler.getHostAddress"
	 */
	public InetAddress getHostAddress(URL u) {
		return super.getHostAddress(u);
	}

	/**
	 * This method calls <code>super.hashCode(URL)</code>.
	 * 
	 * @see "java.net.URLStreamHandler.hashCode(URL)"
	 */
	public int hashCode(URL u) {
		return super.hashCode(u);
	}

	/**
	 * This method calls <code>super.hostsEqual</code>.
	 * 
	 * @see "java.net.URLStreamHandler.hostsEqual"
	 */
	public boolean hostsEqual(URL u1, URL u2) {
		return super.hostsEqual(u1, u2);
	}

	/**
	 * This method calls <code>super.sameFile</code>.
	 * 
	 * @see "java.net.URLStreamHandler.sameFile"
	 */
	public boolean sameFile(URL u1, URL u2) {
		return super.sameFile(u1, u2);
	}

	/**
	 * This method calls
	 * <code>realHandler.setURL(URL,String,String,int,String,String)</code>.
	 * 
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String)"
	 * @deprecated This method is only for compatibility with handlers written
	 *             for JDK 1.1.
	 */
	protected void setURL(URL u, String proto, String host, int port,
			String file, String ref) {
		realHandler.setURL(u, proto, host, port, file, ref);
	}

	/**
	 * This method calls
	 * <code>realHandler.setURL(URL,String,String,int,String,String,String,String)</code>.
	 * 
	 * @see "java.net.URLStreamHandler.setURL(URL,String,String,int,String,String,String,String)"
	 */
	protected void setURL(URL u, String proto, String host, int port,
			String auth, String user, String path, String query, String ref) {
		realHandler.setURL(u, proto, host, port, auth, user, path, query, ref);
	}
}