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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

public class RequestBase
                        extends HeaderBase
{
  // private constants

  // The URL to use as context when parsing the request URI.
  private static URL                  BASE_HTTP_URL      = null;
  static {
    try {
      BASE_HTTP_URL = new URL("http://localhost/");
    } catch (final MalformedURLException _mfue) {
      // Should not happen!
    }
  }

  // protected constants

  protected static final String       GET_METHOD         = "GET";

  protected static final String       HEAD_METHOD        = "HEAD";

  protected static final String       POST_METHOD        = "POST";

  protected static final String       HTTP_1_0_PROTOCOL  = "HTTP/1.0";

  protected static final String       HTTP_1_1_PROTOCOL  = "HTTP/1.1";

  protected static final String       FORM_MIME_TYPE     = "application/x-www-form-urlencoded";

  // private fields

  private String                      method             = null;

  // The protocol on the request line.
  private String                      protocol           = null;

  // The abs path part of the request URI
  private String                      uri                = null;

  // The query part of the request URI
  private String                      queryString        = null;

  // The session id parameter of the request URI
  private String                      sessionIdParameter = null;

  private Hashtable<String, String[]> queryParameters    = null;

  private Hashtable<String, String[]> parameters         = null;

  private ServletInputStreamImpl      body               = null;

  // constructors

  RequestBase()
  {
  }

  // public methods

  public void init(InputStream is, HttpConfigWrapper httpConfig)
      throws HttpException, IOException
  {
    body = new ServletInputStreamImpl(is);
    super.init(httpConfig);
  }

  public void handle()
      throws HttpException, IOException
  {
    parseRequestLine(body);
    super.handle(body);
  }


  @Override
  public void destroy()
  {
    // Activator.log.info("destroy() - " + this);
    method = null;
    protocol = null;
    uri = null;
    sessionIdParameter = null;
    queryString = null;

    queryParameters = null;
    parameters = null;
    try {
      body.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    body = null;

    super.destroy();
  }

  public void reset(boolean keepAlive)
  {
    // Activator.log.info(Thread.currentThread().getName() +
    // " - RequestBase.reset() - keepAlive=" + keepAlive);
    method = null;
    protocol = null;
    uri = null;
    sessionIdParameter = null;
    queryString = null;

    queryParameters = null;
    parameters = null;

    // body = null;

    body.setLimit(-1);
    if (!keepAlive)
      body.init();
    super.reset();
  }

  public String getMethod()
  {
    return method;
  }

  public String getURI()
  {
    return uri;
  }

  public String getProtocol()
  {
    return protocol;
  }

  public String getQueryString()
  {
    return queryString;
  }

  public Hashtable<String, String[]> getQueryParameters()
  {
    if (queryParameters == null) {
      queryParameters = parseQueryString();
    }

    return queryParameters;
  }

  public String getSessionIdParameter()
  {
    return sessionIdParameter;
  }

  public ServletInputStreamImpl getBody()
  {
    return body;
  }

  void setBody(ServletInputStreamImpl newBody)
  {
    this.body = newBody;
  }

  public Hashtable<String, String[]> getParameters()
  {
    if (parameters == null) {
      parameters = parseParameters();
    }

    return parameters;
  }

  // private methods

  private void parseRequestLine(ServletInputStreamImpl in)
      throws HttpException, IOException
  {
    String line;
    int index;

    // parse method
    line = in.readLine();
    index = line.indexOf(' ');
    if (index == -1) {
      throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
    }
    method = line.substring(0, index).toUpperCase();

    // get uri
    line = line.substring(index + 1);
    index = line.indexOf(' ');
    if (index == -1 || index == 0) {
      throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
    }
    uri = line.substring(0, index);

    // get protocol
    protocol = line.substring(index + 1).trim();

    // Parse the uri string. It may be '*', absoluteURI, abs_path,
    // authority. Proxies are allowed to send requests with an
    // absolute URI on the request line, clients should only send
    // requests with an absolute path. Currently we only support
    // absoluteURI and abs_path.
    try {
      if (uri.charAt(0) == '/') {
        // uri is path
        // Some URL implementations look for a schema even when uri starts
        // with a slash -> Malformed URL Exception if uri contains a ':'
        // (which is not OK but still happens in the real world)
        uri = "http:" + uri; // add schema to help URL constructor
      }
      final URL url = new URL(BASE_HTTP_URL, uri);
      uri = url.getPath();
      final int sessionPos = uri.lastIndexOf(HttpUtil.SESSION_PARAMETER_KEY);
      if (-1 < sessionPos) {
        sessionIdParameter =
            uri.substring(sessionPos + HttpUtil.SESSION_PARAMETER_KEY.length());
        uri = uri.substring(0, sessionPos);
      }
      queryString = url.getQuery();
    } catch (final MalformedURLException mue) {
      Activator.log.warn("Could not make URI from request path", mue);
      throw new HttpException(HttpServletResponse.SC_BAD_REQUEST,
                              mue.getMessage());
    }
  }

  private Hashtable<String, String[]> parseQueryString()
  {
    if (queryString != null) {
      try {
        @SuppressWarnings("unchecked")
        final Hashtable<String, String[]> res =
            HttpUtils.parseQueryString(queryString);
        return res;
      } catch (final IllegalArgumentException ignore) {
      }
    }

    return new Hashtable<String, String[]>();
  }

  private Hashtable<String, String[]> parseParameters()
  {
    final Hashtable<String, String[]> parameters = getQueryParameters();
    final String contentType = getContentType();

    if (POST_METHOD.equals(method) && null != contentType
        && contentType.startsWith(FORM_MIME_TYPE)) {
      // Check that the input stream has not been touched

      // Can not use HttpUtils.parsePostData() here since it
      // does not honor the character encoding.
      byte[] bodyBytes = null;
      try {
        final int length = getContentLength();
        final InputStream in = getBody();
        bodyBytes = new byte[length];
        int offset = 0;

        do {
          final int readLength = in.read(bodyBytes, offset, length - offset);
          if (readLength <= 0) {
            // Bytes are missing, skip.
            throw new IOException("Body data to shoort!");
          }
          offset += readLength;
        } while (length - offset > 0);
      } catch (final IOException ioe) {
        return parameters;
      }
      String paramData = null;
      try {
        paramData = new String(bodyBytes, getCharacterEncoding());
      } catch (final UnsupportedEncodingException usee) {
        // Fallback to use the default character encoding.
        paramData = new String(bodyBytes);
      }
      // Note that HttpUtils.parseQueryString() does not handle UTF-8
      // characters that are '%' encoded! Encoding UTF-8 chars in that
      // way should not be needed, since the body may contain UTF-8
      // chars as long as the correct character encoding has been used
      // for the post.
      @SuppressWarnings("unchecked")
      final Hashtable<String, String[]> p = HttpUtils.parseQueryString(paramData);
      // Merge posted parameters with URL parameters.
      final Enumeration<String> e = p.keys();
      while (e.hasMoreElements()) {
        final String key = e.nextElement();
        final String[] val = p.get(key);
        String[] valArray;
        final String oldVals[] = parameters.get(key);
        if (oldVals == null) {
          valArray = val;
        } else {
          valArray = new String[oldVals.length + val.length];
          for (int i = 0; i < val.length; i++) {
            valArray[i] = val[i];
          }
          for (int i = 0; i < oldVals.length; i++) {
            valArray[val.length + i] = oldVals[i];
          }
        }
        parameters.put(key, valArray);
      }
    }
    return parameters;
  }

} // RequestBase
