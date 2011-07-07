/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SingleThreadModel;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

import org.knopflerfish.service.log.LogRef;

public class RequestDispatcherImpl
  implements RequestDispatcher
{
  // private constants

  private final static Dictionary threadStacks = new Hashtable();

  // private fields

  private final String servletPath;

  private final Servlet servlet;

  private final HttpContext httpContext;

  private final ServletConfig config;

  private String uri = null;

  private String queryString = null;

  private String pathInfo = null;

  /**
   * HACK CSM
   */
  private long lastModificationDate;


  // constructors

  /**
   * HACK CSM
   */
  RequestDispatcherImpl(final String servletPath,
                        final Servlet servlet,
                        final HttpContext httpContext,
                        long newDate)
  {
    this(servletPath, servlet, httpContext, null, newDate);
  }

  /**
   * HACK CSM
   */
  RequestDispatcherImpl(final String servletPath,
                        final Servlet servlet,
                        final HttpContext httpContext,
                        final ServletConfig config,
                        long newDate)
  {
    this.servletPath = servletPath;
    this.servlet = servlet;
    this.httpContext = httpContext;
    this.config = config;
    lastModificationDate = newDate;
  }

  // private methods

  private void service(HttpServletRequest request,
                       HttpServletResponse response)
    throws IOException, ServletException
  {
    if (httpContext.handleSecurity(request, response)) {

      Thread t = Thread.currentThread();
      Stack usedURIStack = (Stack) threadStacks.get(t);
      if (usedURIStack == null) {
        usedURIStack = new Stack();
        threadStacks.put(t, usedURIStack);
      }
      String uri = (String) request
        .getAttribute("javax.servlet.include.request_uri");
      if (uri == null)
        uri = request.getRequestURI();
      if (usedURIStack.contains(uri))
        throw new ServletException("Recursive include of \"" + uri
                                   + "\"");

      usedURIStack.push(uri);
      try {
        if (servlet instanceof SingleThreadModel) {
          synchronized (servlet) {
            if (config == null)
              servlet.service(request, response);
            else
              serviceResource(request, response, config);
          }
        } else {
          if (config == null)
            servlet.service(request, response);
          else
            serviceResource(request, response, config);
        }
      } finally {
        usedURIStack.pop();
        if (usedURIStack.empty())
          threadStacks.remove(t);
      }

    }
  }

  private void serviceResource(HttpServletRequest request,
                               HttpServletResponse response,
                               ServletConfig config)
    throws IOException
  {
    String uri = (String) request
      .getAttribute("javax.servlet.include.request_uri");
    if (uri == null) {
      uri = request.getRequestURI();
    }

    final boolean useGzip = HttpUtil.useGZIPEncoding(request);

    if (uri.endsWith(".shtml")) {
      serviceSSIResource(uri, response, config, useGzip);
    } else {
      final String target = HttpUtil.makeTarget(uri, servletPath);
      final ServletContext context = config.getServletContext();
      final URL url = context.getResource(target);
      //HACK CSM
      final long date = getLastModified(url);
      if (date > -1) {
        try {
          long if_modified = request.getDateHeader("If-Modified-Since");
          if (if_modified>0 && date/1000<=if_modified/1000) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
          }
        } catch (IllegalArgumentException iae){
          // An 'If-Modified-Since' header is present but the value
          // can not be parsed; ignore it.
          final LogRef log = Activator.log;
          if (null!=log && log.doDebug()) {
            log.debug("Ignoring broken 'If-Modified-Since' header: "
                      +iae.getMessage(), iae);
          }
        }
        response.setDateHeader("Last-Modified", date);
      }
      //END HACK CSM
      final URLConnection resource = url.openConnection();

      String contentType = context.getMimeType(target);
      if (contentType == null)
        contentType = resource.getContentType();
      if (contentType != null) {
        final String encoding = resource.getContentEncoding();
        if (encoding != null)
          contentType += "; charset=" + encoding;
        response.setContentType(contentType);
      }

      // NOTE: When useGzip is true (Content-Encoding gzip) buffering
      //       is required, since the actual content-length is not
      //       available untill after gzip:ing the contents.

      // HACK CSM for linux: contentLength will allways be 0.  To
      //       handle that, we must also buffer the data read from
      //       the URL-connection.

      int totalBytesRead = 0;
      // Output stream to write to when buffering is needed.
      OutputStream buos = null;
      // The buffer.
      ByteArrayOutputStream baos = null;
      // Filter on top of the buffer when gzip:ing.
      GZIPOutputStream gzos = null;

      final int contentLength = resource.getContentLength();
      if (contentLength > 0 && !useGzip) {
        response.setContentLength(contentLength);
      } else {
        final int size = contentLength>0 ? contentLength+25 : 512;
        buos = baos = new ByteArrayOutputStream(size);
        if (useGzip) {
          gzos = new GZIPOutputStream(baos);
          buos = gzos;
        }
      }

      final InputStream is = resource.getInputStream();
      final OutputStream os = response.getOutputStream();

      int bytesRead = 0;
      final byte buffer[] = new byte[512];
      while((bytesRead = is.read(buffer)) != -1) {
        if (null==buos) {
          os.write(buffer, 0, bytesRead);
        } else {
          // Buffer data to be able to compute the true content length
          buos.write(buffer, 0, bytesRead);
          totalBytesRead += bytesRead;
        }
      }
      if (null!=buos) {
        if (null!=gzos) { //Content-Encoding: gzip
          gzos.finish();
          response.addHeader(HeaderBase.CONTENT_ENCODING, "gzip");
          response.setContentLength(baos.size());
          baos.writeTo(os);
        } else { // Initially unknown content length.
          if(totalBytesRead > 0) {
            response.setContentLength(totalBytesRead);
            baos.writeTo(os);
          }
        }
      }
      is.close();
    }
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
    if (contentType != null)
      response.setContentType(contentType);

    ServletOutputStream os = response.getOutputStream();
    if (useGzip) {
      os = new GZIPServletOutputStreamImpl(os);
      response.addHeader(HeaderBase.CONTENT_ENCODING, "gzip");
    }
    try {
      parseHtml(target, context, os, new Stack());
    } catch (IOException ioe) {
      throw ioe;
    } catch (Exception e) {
      os.print("<b><font color=\"red\">SSI Error: " + e + "</font></b>");
    }
    // An explicit flush is needed here, since flushing the
    // underlaying servlet output stream will not finish the gzip
    // process! Note flush() should only be called once on a
    // GZIPServletOutputStreamImpl.
    os.flush();
  }

  private void parseHtml(final String uri,
                         final ServletContext context,
                         final ServletOutputStream os,
                         final Stack usedFiles)
    throws IOException
  {
    if (usedFiles.contains(uri)) {
      os.print("<b><font color=\"red\">SSI Error: Recursive include: "
               + uri + "</font></b>");
      return;
    }
    usedFiles.push(uri);
    InputStream raw;
    try {
      raw = context.getResourceAsStream(uri);
    } catch (Exception e) {
      raw = null;
    }
    if (raw == null) {
      os.print("<b><font color=\"red\">SSI Error: Error reading file: "
               + uri + "</font></b>");
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
            restOfTag = restOfTag.substring(include_pattern
                                            .length());

            final String file_pattern = "file=\"";
            int index = restOfTag.indexOf(file_pattern);
            if (index > 0
                && Character.isWhitespace(restOfTag.charAt(0))) {
              restOfTag = restOfTag.substring(index
                                              + file_pattern.length());
              String file = restOfTag.substring(0, restOfTag
                                                .indexOf('\"'));
              parseHtml(uri
                        .substring(0, uri.lastIndexOf("/") + 1)
                        + file, context, os, usedFiles);
            } else {
              os
                .print("<b><font color=\"red\">SSI Error: Unsupported directive</font></b>");
            }
          } else {
            os
              .print("<b><font color=\"red\">SSI Error: Unsupported directive</font></b>");
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

  public String getServletPath() {
    return servletPath;
  }

  public Servlet getServlet() {
    return servlet;
  }

  public HttpContext getHttpContext() {
    return httpContext;
  }

  public void setURI(String uri) {

    int index = uri.indexOf('?');
    if (index != -1) {
      this.uri = uri.substring(0, index);
      this.queryString = uri.substring(index + 1);
    } else {
      this.uri = uri;
      this.queryString = null;
    }

    String decodedURI = HttpUtil.decodeURLEncoding(uri);
    if (decodedURI != null && decodedURI.length() > servletPath.length()
        && decodedURI.startsWith(servletPath))
      this.pathInfo = HttpUtil.makeTarget(decodedURI, servletPath);
    else
      this.pathInfo = null;
  }

  // implements RequestDispatcher

  public void forward(ServletRequest request, ServletResponse response)
    throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse))
      throw new ServletException("Must be http request");

    if (response.isCommitted()) {
      throw new IllegalStateException(
                                      "Cannot forward request after response is committed");
    }
    response.reset();

    service((HttpServletRequest) request, (HttpServletResponse) response);

    response.flushBuffer();
  }

  public void include(ServletRequest request, ServletResponse response)
    throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse))
      throw new ServletException("Must be http request");

    HttpServletRequest wrappedRequest = new RequestWrapper(
                                                           (HttpServletRequest) request);
    HttpServletResponse wrappedResponse = new ResponseWrapper(
                                                              (HttpServletResponse) response);

    wrappedRequest.setAttribute("javax.servlet.include.request_uri", uri);
    wrappedRequest.setAttribute("javax.servlet.include.context_path", "");
    wrappedRequest.setAttribute("javax.servlet.include.servlet_path",
                                servletPath);
    if (pathInfo != null)
      wrappedRequest.setAttribute("javax.servlet.include.path_info",
                                  pathInfo);
    if (queryString != null)
      wrappedRequest.setAttribute("javax.servlet.include.query_string",
                                  queryString);

    service(wrappedRequest, wrappedResponse);

    response.flushBuffer();
  }

  // HACK CSM
  public long getLastModificationDate() {
    return lastModificationDate;
  }

  // HACK CSM
  /**
   * Gets the last modified value for file modification detection.
   * Aids in "conditional get" and intermediate proxy/node cacheing.
   *
   * Approach used follows that used by Sun for JNLP handling to
   * workaround an apparent issue where file URLs do not correctly
   * return a last modified time.
   *
   */
  protected long getLastModified (URL resUrl)
  {
    long lastModified = 0;

    try {
      // Get last modified time
      URLConnection conn = resUrl.openConnection();
      lastModified = conn.getLastModified();
    } catch (Exception e) {
      // do nothing
    }

    if (lastModified == 0) {
      // Arguably a bug in the JRE will not set the lastModified
      // for file URLs, and always return 0. This is a
      // workaround for that problem.
      String filepath = resUrl.getPath();

      if (filepath != null) {
        File f = new File(filepath);
        if (f.exists()) {
          lastModified = f.lastModified();
        }
      }
    }

    if (lastModified == 0) {
      // HCK CSM we assume that the resource is in the bundle
      lastModified = lastModificationDate ;
    }

    return lastModified;
  }

} // RequestDispatcherImpl
