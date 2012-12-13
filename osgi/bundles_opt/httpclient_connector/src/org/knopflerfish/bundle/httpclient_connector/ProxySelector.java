/*
 * Copyright (c) 2006-2012, KNOPFLERFISH project
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
package org.knopflerfish.bundle.httpclient_connector;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

import org.osgi.framework.BundleContext;

/**
 * Replacement for java.net.ProxySelector that was added in
 * Java 5.
 *
 * Try to get proxy settings from
 * <ol>
 *  <li>the {@link #PROXY_SERVER}, {@link #PROXY_PORT}, and
 *      {@link #NON_PROXY_HOSTS} properties,
 *  <li>the java.net.ProxySelector-class,
 *  <li>the "http.ProxyHost", "http.ProxyPort" and
 *      "http.nonProxyHosts" properties.
 * </ol>
 * and set them on the given HttpClient-object.
 */
class ProxySelector
{
  public final static String PROXY_SERVER
    = "org.knopflerfish.httpclient_connector.proxy.server";
  public final static String PROXY_PORT
    = "org.knopflerfish.httpclient_connector.proxy.port";
  public final static String PROXY_USERNAME
    = "org.knopflerfish.httpclient_connector.proxy.username";
  public final static String PROXY_PASSWORD
    = "org.knopflerfish.httpclient_connector.proxy.password";
  public final static String PROXY_REALM
    = "org.knopflerfish.httpclient_connector.proxy.realm";
  public final static String PROXY_SCHEME
    = "org.knopflerfish.httpclient_connector.proxy.scheme";
  public final static String NON_PROXY_HOSTS
    = "org.knopflerfish.httpclient_connector.proxy.nonProxyHosts";


  /**
   * Set proxy properties for connecting to the given uri on the given
   * client-object.
   *
   * @param bc bundle context to use to fetch property values.
   * @param client the http-client to configure.
   * @param url the URL to configure connection for. The url string
   *            is assumed to be in non-escaped form, that is not RFC
   *            2396 compatible. I.e., it should not contain any
   *            %-encoded chars.
   */
  static void configureProxy(final BundleContext bc,
                             final HttpClient client,
                             final String url)
    throws URIException
  {
    final HostConfiguration conf = client.getHostConfiguration();
    final URI uri;
    Object[] proxy = null;

    try {
      // java.net.URI requires proper encoding accoring to RFC 2396,
      // java.net.URL does not do that. To handle url-strings with
      // spaces and other non RFC 2396 compatible chars we first
      // create an URL then build an URI from the parts of the
      // URL. Note that URL.toURI() requires that the initial URL is
      // RFC 2396 compliant, but the URI constructor below does not
      // it encodes characters when required.
      final URL url1 = new URL(url);
      uri = new URI(url1.getProtocol(),
                    url1.getUserInfo(), url1.getHost(), url1.getPort(),
                    url1.getPath(), url1.getQuery(), url1.getRef());
    } catch (Exception e) {
      Activator.log.error("Invalid URL, " +url +", in http connection: " +e, e);
      throw new URIException(e.toString());
    }

    final String proxyServerKF = bc.getProperty(PROXY_SERVER);

    if (null!=proxyServerKF && 0<proxyServerKF.length()) {
      final String proxyPortStr = bc.getProperty(PROXY_PORT);
      final String nonProxyHosts = bc.getProperty(NON_PROXY_HOSTS);

      proxy = configureProxy(conf, uri, proxyServerKF, proxyPortStr,
                             nonProxyHosts);
    } else {
      try {
        Class ps5Cls = Class.forName
          ("org.knopflerfish.bundle.httpclient_connector.ProxySelector5");
        Method configureProxyMethod
          = ps5Cls.getMethod("configureProxy",
                             new Class[]{HttpClient.class, String.class});
        proxy = (Object[]) configureProxyMethod
          .invoke(null, new Object[]{client, url});
      } catch (Throwable t) {
        // Ignore Exception, fallback to pre Java 5 system properties
        final String proxyServer = bc.getProperty("http.proxyHost");
        final String proxyPortStr = bc.getProperty("http.proxyPort");
        final String nonProxyHosts = bc.getProperty("http.nonProxyHosts");

        proxy = configureProxy(conf, uri, proxyServer, proxyPortStr,
                               nonProxyHosts);
      }
    }

    // Proxy authentification
    final String proxyUsername = bc.getProperty(PROXY_USERNAME);
    if (null != proxy && null != proxyUsername) {
      client.getState().setProxyCredentials
        ( new AuthScope((String) proxy[0],
                        ((Integer) proxy[1]).intValue(),
                        bc.getProperty(PROXY_REALM),
                        bc.getProperty(PROXY_SCHEME)),
          new UsernamePasswordCredentials(proxyUsername,
                                          bc.getProperty(PROXY_PASSWORD)));
    }
  }

  private static Object[] configureProxy(final HostConfiguration conf,
                                         final URI uri,
                                         final String proxyServer,
                                         final String proxyPortStr,
                                         final String nonProxyHosts)
  {
    if (null!=proxyServer && 0<proxyServer.length()) {
      if (!isNonProxyHost(nonProxyHosts, uri)) {
        try {
          int proxyPort = null!=proxyPortStr && 0<proxyPortStr.length()
            ? Integer.parseInt(proxyPortStr) : 80;
          conf.setProxy(proxyServer, proxyPort);
          return new Object[]{proxyServer, Integer.valueOf(proxyPort)};
        } catch (NumberFormatException e) {
          throw new RuntimeException("Invalid proxy: " +proxyServer +":"
                                     +proxyPortStr);
        }
      }
    }
    return null;
  }

  private static final boolean isNonProxyHost(final String nonProxyHosts,
                                              final URI uri)
  {
    if (null!=nonProxyHosts && 0<nonProxyHosts.length()) {
      boolean matchesSupported = false;
      try {
        String.class.getMethod("matches", new Class[]{String.class});
        matchesSupported = true;
      } catch (Exception nsme) {
        // NoSuchMethodException => no mathces(String) method => false
        // Any other exception => underminable => false
      }
      try {
        final String host = uri.getHost();
        final StringTokenizer st = new StringTokenizer(nonProxyHosts, "|");
        while (st.hasMoreTokens()) {
          final String regexp = st.nextToken().trim();
          if (matchesSupported) {
            if (host.matches(regexp)) {
              return true;
            }
          } else {
            if (host.equals(regexp)) {
              return true;
            }
          }
        }
      } catch (Exception e) {
        // URIException => no host in uri, assume not a non-proxy host.
      }
    }

    return false;
  }

}
