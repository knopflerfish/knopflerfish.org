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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class HeaderBase {

  // private constants

  private static final int NO_VALUE = Integer.MIN_VALUE;

  // protected constants

  protected static final String CONNECTION_HEADER_KEY = "Connection";

  protected static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";

  protected static final String CONTENT_LENGTH_HEADER_KEY = "Content-Length";

  protected static final String COOKIE_HEADER_KEY = "Cookie";

  protected static final String DATE_HEADER_KEY = "Date";

  protected static final String LANGUAGE_HEADER_KEY = "Accept-Language";

  protected static final String ACCEPT_ENCODING   = "Accept-Encoding";

  protected static final String CONTENT_ENCODING = "Content-Encoding";

  protected static final String HOST_HEADER_KEY = "Host";

  protected static final String TRANSFER_ENCODING_KEY = "transfer-encoding";

  protected static final String TRANSFER_ENCODING_VALUE_CHUNKED = "chunked";

  //HACK SMA Expect: 100-Continue
  protected static final String EXPECT_HEADER_KEY = "expect";
  protected static final String EXPECT_100_CONTINUE_VALUE = "100-Continue";
  //END HACK

  // private fields

  private HttpConfigWrapper httpConfig;

  private final Dictionary headers = new Hashtable();

  private String characterEncoding;

  private String contentType = null;

  private int contentLength = NO_VALUE;

  private final Dictionary cookies = new Hashtable();

  private final ArrayList locales = new ArrayList(3);

  // constructors

  HeaderBase() {
  }

  // public methods

  public void init(ServletInputStreamImpl in, HttpConfigWrapper httpConfig)
    throws HttpException, IOException
  {
    this.httpConfig = httpConfig;
    parseHeaders(in);
  }

  public void destroy() {

    HttpUtil.removeAll(headers);

    contentType = null;
    contentLength = NO_VALUE;
    characterEncoding = null;

    HttpUtil.removeAll(cookies);
    locales.clear();
  }

  public String getHeader(String name) {

    ArrayList values = (ArrayList) headers.get(name.toLowerCase());
    if (values == null)
      return null;
    Iterator i = values.iterator();
    if (i.hasNext())
      return (String) i.next();
    return null;
  }

  public Enumeration getHeaders(String name) {

    ArrayList values = (ArrayList) headers.get(name.toLowerCase());
    if (values == null)
      return HttpUtil.EMPTY_ENUMERATION;

    return HttpUtil.enumeration(values);
  }

  public Dictionary getHeaders() {
    return headers;
  }

  public void setCharacterEncoding(String enc)
    throws UnsupportedEncodingException
  {
    if (contentType == null)
      parseContentType();

    // Do a dummy String conversion to trigger exception.
    if (true) {
      String dummy = new String(new byte[]{ 32,31,32,33 }, enc);
      dummy.charAt(0);
    }
    characterEncoding = enc;
  }

  public String getCharacterEncoding() {
    if (contentType == null)
      parseContentType();

    if ((characterEncoding == null) || (characterEncoding.length() == 0)) {
      return httpConfig.getDefaultCharacterEncoding();
    }
    return characterEncoding;
  }

  public String getContentType() {

    if (contentType == null)
      parseContentType();

    return contentType;
  }

  public int getContentLength() {

    if (contentLength == NO_VALUE)
      contentLength = parseContentLength();

    return contentLength;
  }

  public Dictionary getCookies() {

    if (cookies.isEmpty())
      parseCookies();

    return cookies;
  }

  public Enumeration getLocales() {

    if (locales.isEmpty())
      parseLocales();

    return HttpUtil.enumeration(locales);
  }

  // private methods

  void parseHeaders(ServletInputStreamImpl in)
    throws HttpException, IOException
  {

    String name = null;
    String value = null;
    ArrayList values = null;

    String line = in.readLine();
    while (line != null && line.length() > 0) {

      char c = line.charAt(0);
      if (c == ' ' || c == '\t') { // continued header value

        // check not first line
        if (!(name != null && value != null && values != null))
          throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);

        // set concatenated header value
        value = value + line.substring(1);
        values.remove(values.size() - 1);
        values.add(value);

      } else {

        int index = line.indexOf(": ");

        // get header name
        name = line.substring(0, index).trim().toLowerCase();

        // get header value
        values = (ArrayList) headers.get(name);
        if (values == null)
          values = new ArrayList();

        // add header value to vector
        value = line.substring(index + 2);
        values.add(value);
        headers.put(name, values);
      }

      // read next line
      line = in.readLine();
    }
  }

  private void parseContentType() {
    contentType = getHeader(CONTENT_TYPE_HEADER_KEY);
    if (null!=contentType) {
      StringBuffer sb = new StringBuffer(contentType.length());
      characterEncoding = HttpUtil.parseContentType(contentType, sb);
    }
  }

  private int parseContentLength() {

    String headerValue = getHeader(CONTENT_LENGTH_HEADER_KEY);

    if (headerValue == null)
      return -1;

    try {
      return Integer.parseInt(headerValue);
    } catch (NumberFormatException nfe) {
      return -1;
    }
  }

  private void parseCookies() {

    Enumeration cookieEnumeration = getHeaders(COOKIE_HEADER_KEY);
    while (cookieEnumeration.hasMoreElements()) {
      String cookieString = (String) cookieEnumeration.nextElement();

      int version = 0;
      String previousToken = "";
      boolean isValue = false;
      Cookie cookie = null;

      StringTokenizer st = new StringTokenizer(cookieString, "=;,", true);
      while (st.hasMoreElements()) {
        String token = ((String) st.nextElement()).trim();

        if (token.equals("="))
          isValue = true;
        else if (";,".indexOf(token) != -1)
          isValue = false;
        else {
          if (isValue) {
            if (token.charAt(0) == '\"') {
              int index = token.indexOf('\"', 1);
              if (index == -1)
                break;
              token = token.substring(1, index);
            }
            if (previousToken.equals("$Version")) {
              try {
                version = Integer.parseInt(token);
              } catch (Exception e) {
                break;
              }
            } else if (previousToken.equals("$Path")) {
              if (cookie == null)
                break;
              cookie.setPath(token);
            } else if (previousToken.equals("$Domain")) {
              if (cookie == null)
                break;
              cookie.setDomain(token);
            } else {
              cookie = new Cookie(previousToken, token);
              cookie.setVersion(version);
              cookies.put(cookie.getName(), cookie);
            }
          } else
            previousToken = token;
        }
      }

    }
  }

  private void parseLocales() {

    Enumeration localeEnumeration = getHeaders(LANGUAGE_HEADER_KEY);
    while (localeEnumeration.hasMoreElements()) {
      // String localeString = (String) localeEnumeration.nextElement();

      // NYI: parse locale strings
    }

    if (locales.size() == 0)
      locales.add(Locale.getDefault());
  }

} // HeaderBase
