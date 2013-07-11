/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.http.HttpContext;

public class RequestImpl
  implements Request, PoolableObject
{
  // private fields

  private final RequestBase base;

  private HttpConfigWrapper httpConfig;

  private final Registrations registrations;

  private final HttpSessionManager sessionManager;

  private InetAddress localAddress = null;
  private InetAddress remoteAddress = null;

  private int localPort = 0;
  private int remotePort = 0;

  private final Attributes attributes = new Attributes();

  private boolean keepAlive = false;

  private HttpSession session = null;

  private String requestedSessionId = null;

  private boolean requestedSessionIdFromURL = false;

  private boolean requestedSessionIdFromCookie = false;

  private String servletPath = null;

  private Cookie[] cookies = null;

  // constructors

  public RequestImpl(final Registrations registrations,
                     final HttpSessionManager sessionManager)
  {
    base = new RequestBase();

    this.registrations = registrations;
    this.sessionManager = sessionManager;
  }

  // public methods

  public StringBuffer getRequestURL()
  {
    final StringBuffer sb =
      new StringBuffer((httpConfig.isSecure() ? "https" : "http") + "://"
                       + getLocalName() + ":" + getLocalPort() + base.getURI());
    return sb;
  }

  public java.util.Map<String, String[]> getParameterMap()
  {
    return base.getParameters();
  }

  public String getLocalAddr()
  {
    return localAddress.getHostAddress();
  }

  public String getLocalName()
  {
    if (httpConfig.getDNSLookup()) {
      return localAddress.getHostName();
    }
    String hostAddress = localAddress.getHostAddress();
    if (-1 < hostAddress.indexOf(":")) {
      final int pPos = hostAddress.indexOf("%");
      if (-1 < pPos) {
        hostAddress = hostAddress.substring(0, pPos);
      }
      return "[" + hostAddress + "]";
    }
    return hostAddress;
  }

  public int getLocalPort()
  {
    return localPort;
  }

  public void init(InputStream is,
                   InetAddress localAddress,
                   int localPort,
                   InetAddress remoteAddress,
                   int remotePort,
                   HttpConfigWrapper httpConfig)
      throws HttpException, IOException
  {
    base.init(is, httpConfig);

    this.httpConfig = httpConfig;
    this.localAddress = localAddress;
    this.localPort = localPort;
    this.remoteAddress = remoteAddress;
    this.remotePort = remotePort;

    final boolean http_1_1 = base.getProtocol().equals(RequestBase.HTTP_1_1_PROTOCOL);

    if (http_1_1
        && !base.getHeaders(HeaderBase.HOST_HEADER_KEY).hasMoreElements()) {
      throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
    }

    final int available = getContentLength();
    String connection = getHeader(HeaderBase.CONNECTION_HEADER_KEY);
    // For details on the Proxy-Connection header please read:
    // http://homepage.ntlworld.com/jonathan.deboynepollard/FGA/web-proxy-connection-header.html
    if (null == connection || 0 == connection.length()) {
      // We handle Proxy-Connection in the same way as Connection
      connection = getHeader("Proxy-Connection");
    }

    final String method = base.getMethod();
    // TODO: OPTIONS and DELETE -> lengthKnown
    final boolean lengthKnown =
      method.equals(RequestBase.GET_METHOD)
          || method.equals(RequestBase.HEAD_METHOD) || available != -1;
    if (lengthKnown) {
      if (http_1_1) {
        if (!"Close".equals(connection)) {
          keepAlive = true;
        }
      } else if ("Keep-Alive".equals(connection)) {
        keepAlive = true;
      }
    }

    if (available != -1) {
      if (keepAlive) {
        base.getBody().setLimit(available);
      } else if (!http_1_1) {
        // perhaps not according to spec, but works better :)
        base.getBody().setLimit(available);
      }
    } else {
      if (method.equals(RequestBase.POST_METHOD)) {
        final String transfer_encoding = getHeader(HeaderBase.TRANSFER_ENCODING_KEY);
        if (HeaderBase.TRANSFER_ENCODING_VALUE_CHUNKED
            .equals(transfer_encoding)) {
          // Handle chunked body the by reading every chunk and creating
          // a new servletinputstream for the decoded body
          // The only client (so far) that seems to be doing this is the
          // Axis2 library
          base.setBody(readChunkedBody(base.getBody()));
        } else {
          throw new HttpException(HttpServletResponse.SC_LENGTH_REQUIRED);
        }
      }
    }

    handleSession();
  }

  ServletInputStreamImpl readChunkedBody(ServletInputStreamImpl is)
      throws IOException, HttpException
  {
    int chunkSize = 0;
    String line;
    final StringBuffer sb = new StringBuffer();

    // Build stringbuffer containing all chunk contents, then
    // make a byte array using request encoding.
    // Yes, one could likely be more memory effective
    do {
      line = is.readLine().trim();
      final int ix = line.indexOf(";");
      if (ix != -1) {
        line = line.substring(0, ix);
      }
      chunkSize = Integer.parseInt(line, 16);
      int n = 0;
      while (n < chunkSize) {
        line = is.readLine();
        n += line.length();
        sb.append(line);
        sb.append("\r\n");
      }
    } while (chunkSize > 0);

    // remaining stuff should be parsed as headers
    base.parseHeaders(is);

    String enc = getCharacterEncoding();
    if (enc == null || enc.length() == 0) {
      enc = "UTF-8";
    }
    final byte[] bytes = sb.toString().getBytes(enc);
    final ByteArrayInputStream bin = new ByteArrayInputStream(bytes);

    return new ServletInputStreamImpl(bin, bytes.length);
  }

  public boolean getKeepAlive()
  {
    return keepAlive;
  }

  // package methods

  public void setServletPath(String servletPath)
  {
    this.servletPath = servletPath;
  }

  // private methods

  private void handleSession()
  {
    final Cookie sessionCookie =
      (Cookie) base.getCookies().get(HttpUtil.SESSION_COOKIE_KEY);
    if (sessionCookie != null) {
      requestedSessionIdFromCookie = true;
      requestedSessionId = sessionCookie.getValue();
    } else {
      requestedSessionId = base.getSessionIdParameter();
      if (null != requestedSessionId) {
        requestedSessionIdFromURL = true;
      }
    }
  }

  // implements PoolableObject

  public void init()
  {
  }

  public void destroy()
  {
    base.destroy();

    localAddress = null;
    remoteAddress = null;

    localPort = 0;
    remotePort = 0;

    attributes.removeAll();

    keepAlive = false;

    session = null;
    requestedSessionId = null;
    requestedSessionIdFromURL = false;
    requestedSessionIdFromCookie = false;

    servletPath = null;

    cookies = null;
  }

  // implements Request

  public InputStream getRawInputStream()
  {
    return base.getBody();
  }

  // implements HttpServletRequest

  public String getAuthType()
  {
    final Object o = getAttribute(HttpContext.AUTHENTICATION_TYPE);
    if (o instanceof String) {
      return (String) o;
    }
    return null;
  }

  public String getRemoteUser()
  {
    final Object o = getAttribute(HttpContext.REMOTE_USER);
    if (o instanceof String) {
      return (String) o;
    }
    return null;
  }

  public String getContextPath()
  {
    return "";
  }

  public Cookie[] getCookies()
  {
    Dictionary<String, Cloneable> d;
    if (cookies == null && !(d = base.getCookies()).isEmpty()) {
      cookies = new Cookie[d.size()];
      final Enumeration<Cloneable> e = d.elements();
      for (int i = 0; i < cookies.length; i++) {
        cookies[i] = (Cookie) e.nextElement();
      }
    }

    return cookies;
  }

  public long getDateHeader(String name)
  {
    final String value = base.getHeader(name);
    if (value == null)
     {
      return -1; // NYI: throw new IllegalArgumentException()???
    }

    return HttpUtil.parseDate(value);
  }

  public String getHeader(String name)
  {
    return base.getHeader(name);
  }

  public Enumeration<String> getHeaderNames()
  {
    return base.getHeaders().keys();
  }

  public Enumeration<?> getHeaders(String name)
  {
    return base.getHeaders(name);
  }

  public int getIntHeader(String name)
  {
    final String value = base.getHeader(name);
    if (value == null)
     {
      return -1; // NYI: throw new NumberFormatException()???
    }

    return Integer.parseInt(value);
  }

  public String getMethod()
  {
    return base.getMethod();
  }

  public String getPathInfo()
  {
    final String decodedURI = HttpUtil.decodeURLEncoding(base.getURI());
    if (decodedURI != null && decodedURI.length() > servletPath.length()
        && decodedURI.startsWith(servletPath)) {
      return HttpUtil.makeTarget(decodedURI, servletPath);
    }
    return null;
  }

  public String getPathTranslated()
  {
    return getPathInfo();
  }

  public String getQueryString()
  {
    return base.getQueryString();
  }

  public String getRequestURI()
  {
    return base.getURI();
  }

  public String getRequestedSessionId()
  {
    return requestedSessionId;
  }

  public String getServletPath()
  {
    return servletPath;
  }

  public HttpSession getSession()
  {
    return getSession(true);
  }

  public HttpSession getSession(boolean create)
  {
    if (session == null) {
      session = sessionManager.getHttpSession(requestedSessionId);
      if (create && session == null) {
        session = sessionManager.createHttpSession();
      }
    }

    return session;
  }

  public Principal getUserPrincipal()
  {
    return null;
  }

  public boolean isRequestedSessionIdFromCookie()
  {
    return requestedSessionIdFromCookie;
  }

  public boolean isRequestedSessionIdFromURL()
  {
    return requestedSessionIdFromURL;
  }

  public boolean isRequestedSessionIdFromUrl()
  {
    return isRequestedSessionIdFromURL(); // deprecated
  }

  public boolean isRequestedSessionIdValid()
  {
    if (requestedSessionId == null) {
      return false;
    }

    final HttpSession session = getSession(false);
    if (session == null) {
      return false;
    }
    return requestedSessionId.equals(session.getId());
  }

  public boolean isUserInRole(String role)
  {
    return false;
  }

  // implements ServletRequest

  public Object getAttribute(String name)
  {
    return attributes.getAttribute(name);
  }

  public Enumeration<String> getAttributeNames()
  {
    return attributes.getAttributeNames();
  }

  public void setAttribute(String name, Object value)
  {
    attributes.setAttribute(name, value);
  }

  public void removeAttribute(String name)
  {
    attributes.removeAttribute(name);
  }

  public void setCharacterEncoding(String enc)
      throws UnsupportedEncodingException
  {
    base.setCharacterEncoding(enc);
  }

  public String getCharacterEncoding()
  {
    return base.getCharacterEncoding();
  }

  public int getContentLength()
  {
    return base.getContentLength();
  }

  public String getContentType()
  {
    return base.getContentType();
  }

  public ServletInputStream getInputStream()
  {
    return base.getBody(); // NYI: should be wrapped
  }

  public Locale getLocale()
  {
    return base.getLocales().nextElement();
  }

  public Enumeration<Locale> getLocales()
  {
    return base.getLocales();
  }

  public String getParameter(String name)
  {
    final String[] values = getParameterValues(name);
    if (values == null || values.length == 0) {
      return null;
    }
    return values[0];
  }

  public Enumeration<String> getParameterNames()
  {
    return base.getParameters().keys();
  }

  public String[] getParameterValues(String name)
  {
    return base.getParameters().get(name);
  }

  public String getProtocol()
  {
    return base.getProtocol();
  }

  public BufferedReader getReader()
  {
    InputStreamReader isr = null;
    try {
      final String enc = getCharacterEncoding();
      isr =
        null == enc
          ? new InputStreamReader(base.getBody())
          : new InputStreamReader(base.getBody(), enc);
    } catch (final UnsupportedEncodingException use) {
      // Fallback to use the local default encoding...
      isr = new InputStreamReader(base.getBody());
    }
    return new BufferedReader(isr); // NYI:
    // should
    // be
    // wrapped
  }

  public String getRealPath(String path)
  {
    return null; // deprecated
  }

  public String getRemoteAddr()
  {
    return remoteAddress.getHostAddress();
  }

  public String getRemoteHost()
  {
    if (httpConfig.getDNSLookup()) {
      return remoteAddress.getHostName();
    }
    return remoteAddress.getHostAddress();
  }

  public int getRemotePort()
  {
    return remotePort;
  }

  public RequestDispatcher getRequestDispatcher(String uri)
  {
    final RequestDispatcher rd = registrations.getRequestDispatcher(uri);
    return rd;
  }

  public String getServerName()
  {
    String host = base.getHeader(HeaderBase.HOST_HEADER_KEY);
    if (host == null || host.length() == 0) {
      host = httpConfig.getHost();
    } else {
      final int index = host.indexOf(':');
      if (index != -1) {
        host = host.substring(0, index);
      }
    }
    if (host == null || host.length() == 0) {
      try {
        host = InetAddress.getLocalHost().getHostName();
      } catch (final UnknownHostException uhe) {
        host = null;
      }
    }

    return host;
  }

  public int getServerPort()
  {
    final String host = base.getHeader(HeaderBase.HOST_HEADER_KEY);
    if (host == null || host.length() == 0) {
      return httpConfig.getPort();
    }
    final int index = host.indexOf(':');
    if (index != -1) {
      try {
        return Integer.parseInt(host.substring(index + 1));
      } catch (final Exception ignore) {
      }
    }
    return isSecure() ? 443 : 80;
  }

  public boolean isSecure()
  {
    return httpConfig.isSecure();
  }

  /*
   * (non-Javadoc)
   *
   * @see javax.servlet.ServletRequest#getScheme()
   */
  public String getScheme()
  {
    return httpConfig.getScheme();
  }

} // RequestImpl
