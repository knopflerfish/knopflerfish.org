/*
 * Copyright (c) 2003-2013,2015 KNOPFLERFISH project
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Stack;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.knopflerfish.service.log.LogRef;
import org.osgi.service.http.HttpContext;

public class RequestDispatcherImpl
  implements RequestDispatcher
{
  // private constants

  private final static Dictionary<Thread, Stack<String>> threadStacks =
    new Hashtable<Thread, Stack<String>>();

  // private fields

  private final String servletPath;

  private final Servlet servlet;

  private final HttpContext httpContext;

  private final ServletConfig config;

  private String uri = null;

  private String queryString = null;

  private String pathInfo = null;

  private URL resourceURL = null;
  
  /**
   * HACK CSM
   */
  private final long lastModificationDate;
  
   // constructors

  /**
   * HACK CSM
   */
  RequestDispatcherImpl(final String servletPath, final Servlet servlet,
                        final HttpContext httpContext, long newDate)
  {
    this(servletPath, servlet, httpContext, null, newDate, null);
  }

  /**
   * HACK CSM
   */
  RequestDispatcherImpl(final String servletPath, final Servlet servlet,
                        final HttpContext httpContext,
                        final ServletConfig config, long newDate, URL resourceURL)
  {
    this.servletPath = servletPath;
    this.servlet = servlet;
    this.httpContext = httpContext;
    this.config = config;
    lastModificationDate = newDate;
    this.resourceURL = resourceURL;
  }

  // private methods

  private void service(Request request, Response response)
      throws IOException, ServletException
  {
    if (httpContext.handleSecurity(request, response)) {

      final Thread t = Thread.currentThread();
      Stack<String> usedURIStack = threadStacks.get(t);
      if (usedURIStack == null) {
        usedURIStack = new Stack<String>();
        threadStacks.put(t, usedURIStack);
      }
      String uri =
        (String) request.getAttribute("javax.servlet.include.request_uri");
      if (uri == null) {
        uri = request.getRequestURI();
      }
      if (usedURIStack.contains(uri)) {
        throw new ServletException("Recursive include of \"" + uri + "\"");
      }

      usedURIStack.push(uri);
      try {
        if (servlet instanceof SingleThreadModel) {
          synchronized (servlet) {
            if (config == null) {
              // Activator.log.info("Serving: " + uri);
              servlet.service(request, response);
              // Activator.log.info("Served: " + uri);
            } else {
              serviceResource(request, response, config);
            }
          }
        } else {
          if (config == null) {
            // Activator.log.info(Thread.currentThread().getName() + " Serving: " + uri);
            servlet.service(request, response);
            // Activator.log.info(Thread.currentThread().getName() + " Served: " + uri);
          } else {
            serviceResource(request, response, config);
          }
        }
      } finally {
        usedURIStack.pop();
        if (usedURIStack.empty()) {
          threadStacks.remove(t);
        }
      }

    }
  }

  private void serviceResource(Request request,
                               Response response,
                               ServletConfig config)
      throws IOException, ServletException
  {
    final String method = request.getMethod();
    if ("HEAD".equalsIgnoreCase(method)) {
      final NoBodyResponse nb_response = new NoBodyResponse(response);
      serviceGet(request, nb_response, config);
      nb_response.setContentLength();
    } else if ("GET".equalsIgnoreCase(method)) {
      serviceGet(request, response, config);
    } else if ("TRACE".equalsIgnoreCase(method)) {
      serviceTrace(request, response);
    } else { // unsupported
      response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return;
    }
  }

  private void serviceGet(Request request,
                          Response response,
                          ServletConfig config)
      throws IOException
  {
    // Activator.log.info("serviceGet()");
    String uri =
      (String) request.getAttribute("javax.servlet.include.request_uri");
    if (uri == null) {
      uri = request.getRequestURI();
    }

    final boolean useGzip = HttpUtil.useGZIPEncoding(request);

    if (uri.endsWith(".shtml")) {
      serviceSSIResource(uri, response, config, useGzip);
    } else {


      final String target = HttpUtil.makeTarget(uri, servletPath);
      final ServletContext context = config.getServletContext();
      // final URL url = context.getResource(target);
      final URLConnection resource = resourceURL.openConnection();

      // HACK CSM
      final long date = getLastModified(resource);
      if (date > -1) {
        try {
          final long if_modified = request.getDateHeader("If-Modified-Since");
          if (if_modified > 0 && date / 1000 <= if_modified / 1000) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
          }
        } catch (final IllegalArgumentException iae) {
          // An 'If-Modified-Since' header is present but the value
          // can not be parsed; ignore it.
          final LogRef log = Activator.log;
          if (null != log && log.doDebug()) {
            log.debug("Ignoring broken 'If-Modified-Since' header: "
                          + iae.getMessage(), iae);
          }
        }
        response.setDateHeader("Last-Modified", date);
      }
      // END HACK CSM
      
      String contentType = context.getMimeType(target);
      
      if (contentType == null) {
        contentType = resource.getContentType();
      }
      
      if (contentType != null) {
        final String encoding = resource.getContentEncoding();
        if (encoding != null) {
          contentType += "; charset=" + encoding;
        }
        response.setContentType(contentType);
      }

      final InputStream is = resource.getInputStream();
      response.copy(is);
      is.close();
    }
  }

  private void serviceTrace(HttpServletRequest req, HttpServletResponse resp)
      throws IOException
  {
    int responseLength;

    final String CRLF = "\r\n";
    String responseString =
      "TRACE " + req.getRequestURI() + " " + req.getProtocol();

    @SuppressWarnings("unchecked")
    final Enumeration<String> reqHeaderEnum = req.getHeaderNames();

    while (reqHeaderEnum.hasMoreElements()) {
      final String headerName = reqHeaderEnum.nextElement();
      responseString += CRLF + headerName + ": " + req.getHeader(headerName);
    }

    responseString += CRLF;

    responseLength = responseString.length();

    resp.setContentType("message/http");
    resp.setContentLength(responseLength);
    final ServletOutputStream out = resp.getOutputStream();
    out.print(responseString);
    out.close();
  }

  private void serviceSSIResource(final String uri,
                                  final HttpServletResponse response,
                                  final ServletConfig config,
                                  final boolean useGzip)
      throws IOException
  {
    final String target = HttpUtil.makeTarget(uri, servletPath);
    final ServletContext context = config.getServletContext();

    final String contentType = context.getMimeType(target);
    if (contentType != null) {
      response.setContentType(contentType);
    }

    ServletOutputStream os = response.getOutputStream();
    if (useGzip) {
      os = new GZIPServletOutputStreamImpl(os);
      response.addHeader(HeaderBase.CONTENT_ENCODING, "gzip");
    }
    try {
      parseHtml(target, context, os, new Stack<String>());
    } catch (final IOException ioe) {
      throw ioe;
    } catch (final Exception e) {
      os.print("<b><font color=\"red\">SSI Error: " + e + "</font></b>");
    }
    // An explicit flush is needed here, since flushing the
    // underlying servlet output stream will not finish the gzip
    // process! Note flush() should only be called once on a
    // GZIPServletOutputStreamImpl.
    os.flush();
  }

  private void parseHtml(final String uri,
                         final ServletContext context,
                         final ServletOutputStream os,
                         final Stack<String> usedFiles)
      throws IOException
  {
    if (usedFiles.contains(uri)) {
      os.print("<b><font color=\"red\">SSI Error: Recursive include: " + uri
               + "</font></b>");
      return;
    }
    usedFiles.push(uri);
    InputStream raw;
    try {
      raw = context.getResourceAsStream(uri);
    } catch (final Exception e) {
      raw = null;
    }
    if (raw == null) {
      os.print("<b><font color=\"red\">SSI Error: Error reading file: " + uri
               + "</font></b>");
      return;
    }
    final InputStream is = new BufferedInputStream(raw);

    byte c;
    boolean tagBegin = false;
    final StringBuffer buf = new StringBuffer(20);
    while ((c = (byte) is.read()) != -1) {
      if (c == '<') {
        buf.setLength(0);
        tagBegin = true;
      } else if (tagBegin && c == '>') {
        String restOfTag = buf.toString();

        final String ssi_pattern = "!--#";
        if (restOfTag.length() > ssi_pattern.length()
            && restOfTag.startsWith(ssi_pattern)) { // is this an
          // ssi tag?
          restOfTag = restOfTag.substring(ssi_pattern.length());

          final String include_pattern = "include";
          if (restOfTag.length() > include_pattern.length()
              && restOfTag.startsWith(include_pattern)) { // is
            // this
            // an
            // include
            // directive?
            restOfTag = restOfTag.substring(include_pattern.length());

            final String file_pattern = "file=\"";
            final int index = restOfTag.indexOf(file_pattern);
            if (index > 0 && Character.isWhitespace(restOfTag.charAt(0))) {
              restOfTag = restOfTag.substring(index + file_pattern.length());
              final String file = restOfTag.substring(0, restOfTag.indexOf('\"'));
              parseHtml(uri.substring(0, uri.lastIndexOf("/") + 1) + file,
                        context, os, usedFiles);
            } else {
              os.print("<b><font color=\"red\">SSI Error: Unsupported directive</font></b>");
            }
          } else {
            os.print("<b><font color=\"red\">SSI Error: Unsupported directive</font></b>");
          }
        } else {
          os.print('<');
          os.print(restOfTag);
          os.print('>');
        }

        tagBegin = false;
      } else if (tagBegin) {
        buf.append((char) c);
      } else {
        os.write(c);
      }
    }

    is.close();
    usedFiles.pop();
  }

  // public methods

  public String getServletPath()
  {
    return servletPath;
  }

  public Servlet getServlet()
  {
    return servlet;
  }

  public HttpContext getHttpContext()
  {
    return httpContext;
  }

  public void setURI(String uri)
  {

    final int index = uri.indexOf('?');
    if (index != -1) {
      this.uri = uri.substring(0, index);
      this.queryString = uri.substring(index + 1);
    } else {
      this.uri = uri;
      this.queryString = null;
    }

    final String decodedURI = HttpUtil.decodeURLEncoding(uri);
    if (decodedURI != null && decodedURI.length() > servletPath.length()
        && decodedURI.startsWith(servletPath)) {
      this.pathInfo = HttpUtil.makeTarget(decodedURI, servletPath);
    } else {
      this.pathInfo = null;
    }
  }

  // implements RequestDispatcher

  @Override
  public void forward(ServletRequest request, ServletResponse response)
      throws IOException, ServletException
  {

    if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
      throw new ServletException("Must be http request");
    }

    if (response.isCommitted()) {
      throw new IllegalStateException(
                                      "Cannot forward request after response is committed");
    }
    response.reset();

    service((RequestImpl) request, (ResponseImpl) response);

    response.flushBuffer();
  }

  @Override
  public void include(ServletRequest request, ServletResponse response)
      throws IOException, ServletException
  {

    if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
      throw new ServletException("Must be http request");
    }

    final RequestWrapper wrappedRequest =
      new RequestWrapper((HttpServletRequest) request);
    final ResponseWrapper wrappedResponse =
      new ResponseWrapper((HttpServletResponse) response);

    wrappedRequest.setAttribute("javax.servlet.include.request_uri", uri);
    wrappedRequest.setAttribute("javax.servlet.include.context_path", "");
    wrappedRequest.setAttribute("javax.servlet.include.servlet_path",
                                servletPath);
    if (pathInfo != null) {
      wrappedRequest.setAttribute("javax.servlet.include.path_info", pathInfo);
    }
    if (queryString != null) {
      wrappedRequest.setAttribute("javax.servlet.include.query_string",
                                  queryString);
    }

    service(wrappedRequest, wrappedResponse);

    response.flushBuffer();
  }

  // HACK CSM
  public long getLastModificationDate()
  {
    return lastModificationDate;
  }

  // HACK CSM
  /**
   * Gets the last modified value for file modification detection. Aids in
   * "conditional get" and intermediate proxy/node cacheing.
   *
   * Approach used follows that used by Sun for JNLP handling to workaround an
   * apparent issue where file URLs do not correctly return a last modified
   * time.
   *
   */
  protected long getLastModified(URLConnection conn)
  {
    long lastModified = 0;

     
    try {
      // Get last modified time
      // final URLConnection conn = resUrl.openConnection();
      lastModified = conn.getLastModified();
      //conn.getInputStream().close();
      //((HttpURLConnection)conn).disconnect();
    } catch (final Exception e) {
      // do nothing
    }

    if (lastModified == 0) {
      // Arguably a bug in the JRE will not set the lastModified
      // for file URLs, and always return 0. This is a
      // workaround for that problem.
      //final String filepath = resUrl.getPath();
      final String filepath = conn.getURL().getPath();
      if (filepath != null) {
        final File f = new File(filepath);
        if (f.exists()) {
          lastModified = f.lastModified();
        }
      }
    }

    if (lastModified == 0) {
      // HCK CSM we assume that the resource is in the bundle
      lastModified = lastModificationDate;
    }

    return lastModified;
  }

} // RequestDispatcherImpl

/*
 * Methods below reused/copied from JSDK, HttpServlet.java Provided under a
 * Apache-2 license
 */

/*
 * A response that includes no body, for use in (dumb) "HEAD" support. This just
 * swallows that body, counting the bytes in order to set the content length
 * appropriately. All other methods delegate directly to the HTTP Servlet
 * Response object used to construct this one.
 */
// file private
class NoBodyResponse
  implements Response
{
  private final Response resp;
  private final NoBodyOutputStream noBody;
  private PrintWriter writer;
  private boolean didSetContentLength;

  // file private
  NoBodyResponse(Response r)
  {
    resp = r;
    noBody = new NoBodyOutputStream();
  }

  // file private
  void setContentLength()
  {
    if (!didSetContentLength) {
      resp.setContentLength(noBody.getContentLength());
    }
  }

  // SERVLET RESPONSE interface methods

  @Override
  public void setContentLength(int len)
  {
    resp.setContentLength(len);
    didSetContentLength = true;
  }

  @Override
  public void setCharacterEncoding(String charset)
  {
    resp.setCharacterEncoding(charset);
  }

  @Override
  public void setContentType(String type)
  {
    resp.setContentType(type);
  }

  @Override
  public String getContentType()
  {
    return resp.getContentType();
  }

  @Override
  public ServletOutputStream getOutputStream()
      throws IOException
  {
    return noBody;
  }

  @Override
  public String getCharacterEncoding()
  {
    return resp.getCharacterEncoding();
  }

  @Override
  public PrintWriter getWriter()
      throws UnsupportedEncodingException
  {
    if (writer == null) {
      OutputStreamWriter w;

      w = new OutputStreamWriter(noBody, getCharacterEncoding());
      writer = new PrintWriter(w);
    }
    return writer;
  }

  @Override
  public void setBufferSize(int size)
      throws IllegalStateException
  {
    resp.setBufferSize(size);
  }

  @Override
  public int getBufferSize()
  {
    return resp.getBufferSize();
  }

  @Override
  public void reset()
      throws IllegalStateException
  {
    resp.reset();
  }

  @Override
  public void resetBuffer()
      throws IllegalStateException
  {
    resp.resetBuffer();
  }

  @Override
  public boolean isCommitted()
  {
    return resp.isCommitted();
  }

  @Override
  public void flushBuffer()
      throws IOException
  {
    resp.flushBuffer();
  }

  @Override
  public void setLocale(Locale loc)
  {
    resp.setLocale(loc);
  }

  @Override
  public Locale getLocale()
  {
    return resp.getLocale();
  }

  // HTTP SERVLET RESPONSE interface methods

  @Override
  public void addCookie(Cookie cookie)
  {
    resp.addCookie(cookie);
  }

  @Override
  public boolean containsHeader(String name)
  {
    return resp.containsHeader(name);
  }

  /* @deprecated */
  @Override
  public void setStatus(int sc, String sm)
  {
    resp.setStatus(sc, sm);
  }

  @Override
  public void setStatus(int sc)
  {
    resp.setStatus(sc);
  }

  @Override
  public void setHeader(String name, String value)
  {
    resp.setHeader(name, value);
  }

  @Override
  public void setIntHeader(String name, int value)
  {
    resp.setIntHeader(name, value);
  }

  @Override
  public void setDateHeader(String name, long date)
  {
    resp.setDateHeader(name, date);
  }

  @Override
  public void sendError(int sc, String msg)
      throws IOException
  {
    resp.sendError(sc, msg);
  }

  @Override
  public void sendError(int sc)
      throws IOException
  {
    resp.sendError(sc);
  }

  @Override
  public void sendRedirect(String location)
      throws IOException
  {
    resp.sendRedirect(location);
  }

  @Override
  public String encodeURL(String url)
  {
    return resp.encodeURL(url);
  }

  @Override
  public String encodeRedirectURL(String url)
  {
    return resp.encodeRedirectURL(url);
  }

  @Override
  public void addHeader(String name, String value)
  {
    resp.addHeader(name, value);
  }

  @Override
  public void addDateHeader(String name, long value)
  {
    resp.addDateHeader(name, value);
  }

  @Override
  public void addIntHeader(String name, int value)
  {
    resp.addIntHeader(name, value);
  }

  @Override
  public String encodeUrl(String url)
  {
    return this.encodeURL(url);
  }

  @Override
  public String encodeRedirectUrl(String url)
  {
    return this.encodeRedirectURL(url);
  }
  
  @Override
  public OutputStream getRawOutputStream() {
    return null;
  }
  
  @Override
  public void copy(InputStream is) throws IOException {
    ; // DOES NOTHING
  }

}

/*
 * Servlet output stream that gobbles up all its data.
 */

// file private
class NoBodyOutputStream
  extends ServletOutputStream
{

  private int contentLength = 0;

  // file private
  NoBodyOutputStream()
  {
  }

  // file private
  int getContentLength()
  {
    return contentLength;
  }

  @Override
  public void write(int b)
  {
    contentLength++;
  }

  @Override
  public void write(byte buf[], int offset, int len)
      throws IOException
  {
    if (len >= 0) {
      contentLength += len;
    } else {
      // XXX
      // isn't this really an IllegalArgumentException?
      throw new IOException("negative length");
    }
  }
}
