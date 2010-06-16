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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

public class RequestBase extends HeaderBase {

  // private constants

  // The URL to use as context when parsing the request URI.
  private static URL BASE_HTTP_URL = null;
  static {
    try {
      BASE_HTTP_URL = new URL("http://localhost/");
    } catch (MalformedURLException _mfue) {
      // Should not happen!
    }
  }

  // protected constants

  protected static final String POST_METHOD = "POST";

  protected static final String HTTP_1_0_PROTOCOL = "HTTP/1.0";

  protected static final String HTTP_1_1_PROTOCOL = "HTTP/1.1";

  protected static final String FORM_MIME_TYPE = "application/x-www-form-urlencoded";

  // private fields

  private String method = null;

  // The protocol on the request line.
  private String protocol = null;

  // The abs path part of the request URI
  private String uri = null;

  // The query part of the request URI
  private String queryString = null;

  // The session id parameter of the request URI
  private String sessionIdParameter = null;

  private Hashtable queryParameters = null;

  private Hashtable parameters = null;

  private ServletInputStreamImpl body = null;

  // constructors

  RequestBase() {
  }

  // public methods

  public void init(InputStream is, HttpConfigWrapper httpConfig)
    throws HttpException, IOException
  {
    ServletInputStreamImpl in
      = new ServletInputStreamImpl(new BufferedInputStream(is));

    parseRequestLine(in);
    super.init(in, httpConfig);

    body = in;
  }

  public void destroy() {

    method = null;
    protocol = null;
    uri = null;
    sessionIdParameter = null;
    queryString = null;

    queryParameters = null;
    parameters = null;

    body = null;

    super.destroy();
  }

  public String getMethod() {
    return method;
  }

  public String getURI() {
    return uri;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getQueryString() {
    return queryString;
  }

  public Hashtable getQueryParameters() {

    if (queryParameters == null)
      queryParameters = parseQueryString();

    return queryParameters;
  }

  public String getSessionIdParameter() {
    return sessionIdParameter;
  }

  public ServletInputStreamImpl getBody() {
    return body;
  }

  void setBody(ServletInputStreamImpl newBody) {
    this.body = newBody;
  }

  public Hashtable getParameters() {

    if (parameters == null)
      parameters = parseParameters();

    return parameters;
  }

  // private methods

  private void parseRequestLine(ServletInputStreamImpl in)
    throws HttpException, IOException {

    String line;
    int index;

    // parse method
    line = in.readLine();
    index = line.indexOf(' ');
    if (index == -1)
      throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
    method = line.substring(0, index);

    // get uri
    line = line.substring(index + 1);
    index = line.indexOf(' ');
    if (index == -1)
      throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
    uri = line.substring(0, index);

    // get protocol
    protocol = line.substring(index + 1).trim();

    // Parse the uri string. It may be '*', absoluteURI, abs_path,
    // authority. Proxies are allowed to send requests with an
    // aboslute URI on the request line, clients should only send
    // requests with an absolute path. Currently we only support
    // absoluteURI and abs_path.
    try {
      URL url = new URL(BASE_HTTP_URL, uri);
      uri = url.getPath();
      int sessionPos = uri.lastIndexOf(HttpUtil.SESSION_PARAMETER_KEY);
      if (-1<sessionPos) {
        sessionIdParameter = uri
          .substring(sessionPos+HttpUtil.SESSION_PARAMETER_KEY.length());
        uri = uri.substring(0, sessionPos);
      }
      queryString = url.getQuery();
    } catch (MalformedURLException mue) {
      throw new HttpException(HttpServletResponse.SC_BAD_REQUEST,
                              mue.getMessage());
    }
  }

  private Hashtable parseQueryString() {

    if (queryString != null) {
      try {
        return HttpUtils.parseQueryString(queryString);
      } catch (IllegalArgumentException ignore) {
      }
    }

    return new Hashtable();
  }

  private Hashtable parseParameters() {

    Hashtable parameters = getQueryParameters();
    String contentType = getContentType();

    if (POST_METHOD.equals(method)
        && null!=contentType && contentType.startsWith(FORM_MIME_TYPE)) {
      // Check that the input stream has not been touched

      // Can not use HttpUtils.parsePostData() here since it
      // does not honor the character encoding.
      byte[] bodyBytes = null;
      try {
        int length = getContentLength();
        InputStream in = getBody();
        bodyBytes = new byte[length];
        int offset = 0;

        do {
          int readLength = in.read( bodyBytes, offset, length-offset);
          if (readLength<=0) {
            // Bytes are missing, skip.
            throw new IOException("Body data to shoort!");
          }
          offset += readLength;
        } while (length-offset>0);
      } catch (IOException ioe) {
        return parameters;
      }
      String paramData = null;
      try {
        paramData = new String(bodyBytes, getCharacterEncoding());
      } catch (UnsupportedEncodingException usee) {
        // Fallback to use the default character encoding.
        paramData = new String(bodyBytes);
      }
      Hashtable p = HttpUtils.parseQueryString(paramData);
      // Merge posted paramters with URL parameters.
      Enumeration e = p.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        String[] val = (String[]) p.get(key);
        String[] valArray;
        String oldVals[] = (String[]) parameters.get(key);
        if (oldVals == null) {
          valArray = val;
        } else {
          valArray = new String[oldVals.length + val.length];
          for (int i = 0; i < val.length; i++)
            valArray[i] = val[i];
          for (int i = 0; i < oldVals.length; i++)
            valArray[val.length + i] = oldVals[i];
        }
        parameters.put(key, valArray);
      }
    }
    return parameters;
  }

} // RequestBase
