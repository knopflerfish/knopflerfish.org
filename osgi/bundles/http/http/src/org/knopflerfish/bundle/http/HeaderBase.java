/*
 * Copyright (c) 2003-2022 KNOPFLERFISH project
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
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.knopflerfish.bundle.http.HttpUtil.ParamQ;

public class HeaderBase
{

  // private constants

  private static final int NO_VALUE = Integer.MIN_VALUE;

  // protected constants

  protected static final String CONNECTION_HEADER_KEY = "Connection";

  protected static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";

  protected static final String CONTENT_LENGTH_HEADER_KEY = "Content-Length";

  protected static final String COOKIE_HEADER_KEY = "Cookie";

  protected static final String DATE_HEADER_KEY = "Date";

  protected static final String LANGUAGE_HEADER_KEY = "Accept-Language";

  protected static final String ACCEPT_ENCODING = "Accept-Encoding";

  protected static final String CONTENT_ENCODING = "Content-Encoding";

  protected static final String HOST_HEADER_KEY = "Host";

  protected static final String TRANSFER_ENCODING_KEY = "transfer-encoding";

  protected static final String TRANSFER_ENCODING_VALUE_CHUNKED = "chunked";

  // HACK SMA Expect: 100-Continue
  protected static final String EXPECT_HEADER_KEY = "expect";
  protected static final String EXPECT_100_CONTINUE_VALUE = "100-Continue";
  // END HACK

  // private fields

  private HttpConfigWrapper httpConfig;

  private final Hashtable<String, Cloneable> headers =
    new Hashtable<String, Cloneable>();

  private String characterEncoding;

  private String contentType = null;

  private int contentLength = NO_VALUE;

  private final Hashtable<String, Cloneable> cookies =
    new Hashtable<String, Cloneable>();

  private final ArrayList<Locale> locales = new ArrayList<Locale>(3);

  // constructors

  HeaderBase()
  {
  }

  // public methods

  public void init(/* ServletInputStreamImpl in,*/ HttpConfigWrapper httpConfig)
      throws HttpException, IOException
  {
    this.httpConfig = httpConfig;
    // parseHeaders(in);
  }
  
  public void handle(ServletInputStreamImpl in) throws HttpException, IOException {
    parseHeaders(in);
  }

  public void destroy()
  {
    headers.clear();

    contentType = null;
    contentLength = NO_VALUE;
    characterEncoding = null;

    cookies.clear();
    locales.clear();
  }

  public void reset()
  {
    headers.clear();

    contentType = null;
    contentLength = NO_VALUE;
    characterEncoding = null;

    cookies.clear();
    locales.clear();
  }
  
  public String getHeader(String name)
  {
    final ArrayList<?> values = (ArrayList<?>) headers.get(name.toLowerCase());
    if (values == null) {
      return null;
    }
    final Iterator<?> i = values.iterator();
    if (i.hasNext()) {
      return (String) i.next();
    }
    return null;
  }

  public Enumeration<?> getHeaders(String name)
  {
    final ArrayList<?> values =
      (ArrayList<?>) headers.get(name.toLowerCase());
    if (values == null) {
      return HttpUtil.EMPTY_ENUMERATION;
    }

    return HttpUtil.enumeration(values);
  }

  public Dictionary<String, Cloneable> getHeaders()
  {
    return headers;
  }

  public void setCharacterEncoding(String enc)
      throws UnsupportedEncodingException
  {
    if (contentType == null) {
      parseContentType();
    }

    // Do a dummy String conversion to trigger exception.
    if (true) {
      final String dummy = new String(new byte[] { 32, 31, 32, 33 }, enc);
      dummy.charAt(0);
    }
    characterEncoding = enc;
  }

  public String getCharacterEncoding()
  {
    if (contentType == null) {
      parseContentType();
    }

    if ((characterEncoding == null) || (characterEncoding.length() == 0)) {
      return httpConfig.getDefaultCharacterEncoding();
    }
    return characterEncoding;
  }

  public String getContentType()
  {
    if (contentType == null) {
      parseContentType();
    }

    return contentType;
  }

  public int getContentLength()
  {
    if (contentLength == NO_VALUE) {
      contentLength = parseContentLength();
    }

    return contentLength;
  }

  public Dictionary<String, Cloneable> getCookies()
  {
    if (cookies.isEmpty()) {
      parseCookies();
    }

    return cookies;
  }

  public Enumeration<Locale> getLocales()
  {
    if (locales.isEmpty()) {
      parseLocales();
    }

    return HttpUtil.enumeration(locales);
  }

  // private methods

  void parseHeaders(ServletInputStreamImpl in)
      throws HttpException, IOException
  {
    String name = null;
    String value = null;
    ArrayList<String> values = null;
    final int limit = httpConfig.getLimitRequestHeaders() - 1;

    String line = in.readLine();
    while (line != null && line.length() > 0) {
//      Activator.log.info(Thread.currentThread().getName() + " - Attempting to parse: " + line);
      if (headers.size() > limit) {
        throw new HttpException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      }

      final char c = line.charAt(0);
      if (c == ' ' || c == '\t') { // continued header value

        // check not first line
        if (!(name != null && value != null && values != null)) {
          throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
        }

        // set concatenated header value
        value = value + line.substring(1);
        values.remove(values.size() - 1);
        values.add(value);

      } else {

        final int index = line.indexOf(": ");

        // get header name
        name = line.substring(0, index).trim().toLowerCase();

        // get header value
        @SuppressWarnings("unchecked")
        final ArrayList<String> newValues =
          (ArrayList<String>) headers.get(name);
        values = newValues;
        if (values == null) {
          values = new ArrayList<String>();
        }

        // add header value to vector
        value = line.substring(index + 2);
        values.add(value);
        headers.put(name, values);
      }

      // read next line
      line = in.readLine();
    }
  }

  private void parseContentType()
  {
    contentType = getHeader(CONTENT_TYPE_HEADER_KEY);
    if (null != contentType) {
      final StringBuilder sb = new StringBuilder(contentType.length());
      characterEncoding = HttpUtil.parseContentType(contentType, sb);
    }
  }

  private int parseContentLength()
  {

    final String headerValue = getHeader(CONTENT_LENGTH_HEADER_KEY);

    if (headerValue == null) {
      return -1;
    }

    try {
      return Integer.parseInt(headerValue);
    } catch (final NumberFormatException nfe) {
      return -1;
    }
  }

  private void parseCookies()
  {

    final Enumeration<?> cookieEnumeration = getHeaders(COOKIE_HEADER_KEY);
    while (cookieEnumeration.hasMoreElements()) {
      final String cookieString = (String) cookieEnumeration.nextElement();

      int version = 0;
      String previousToken = "";
      boolean isValue = false;
      Cookie cookie = null;

      final StringTokenizer st = new StringTokenizer(cookieString, "=;,", true);
      while (st.hasMoreElements()) {
        String token = ((String) st.nextElement()).trim();

        if (token.equals("=")) {
          isValue = true;
        } else if (";,".indexOf(token) != -1) {
          isValue = false;
        } else {
          if (isValue) {
            if (token.charAt(0) == '\"') {
              final int index = token.indexOf('\"', 1);
              if (index == -1) {
                break;
              }
              token = token.substring(1, index);
            }
            if (previousToken.equals("$Version")) {
              try {
                version = Integer.parseInt(token);
              } catch (final Exception e) {
                break;
              }
            } else if (previousToken.equals("$Path")) {
              if (cookie == null) {
                break;
              }
              cookie.setPath(token);
            } else if (previousToken.equals("$Domain")) {
              if (cookie == null) {
                break;
              }
              cookie.setDomain(token);
            } else {
              cookie = new Cookie(previousToken, token);
              cookie.setVersion(version);
              cookies.put(cookie.getName(), cookie);
            }
          } else {
            previousToken = token;
          }
        }
      }

    }
  }

  private void parseLocales()
  {
    final StringBuilder sb = new StringBuilder();
    for (final Enumeration<?> e = getHeaders(LANGUAGE_HEADER_KEY); e.hasMoreElements();) {
      if (sb.length()>0) {
        sb.append(',');
      }
      sb.append(e.nextElement());
    }
    final String localeString = sb.toString();
    final List<ParamQ> params =  HttpUtil.parseAccessHeader(localeString);

    for (final ParamQ pq : params) {
      int ix = pq.param.indexOf('-');
      if (ix == -1) {
        // No country part. Language length should be 2 ISO-639 (or 3 if
        // ISO-639-3)
        try {
          final Locale l = new Locale(pq.param);
          locales.add(l);
        } catch (final Exception e) {
          // Ignore unknown /invalid language tag.
        }
      } else if (ix>0) {
        // Found language, ix should be 2 (ISO-639-2) or 3 (ISO-639-3)
        final String language = pq.param.substring(0, ix);
        final String reminder = pq.param.substring(ix + 1);
        ix = reminder.indexOf('-');
        if (ix==-1) {
          // Found country, ix should be 3 (ISO-3166)
          final String country = reminder;
          final Locale l = new Locale(language, country);
          locales.add(l);
        } else if (ix>0) {
          // Found country, ix should be 3 (ISO-3166)
          final String country = reminder.substring(0, ix);
          final String variant = reminder.substring(ix+1, reminder.length());
          final Locale l = new Locale(language, country, variant);
          locales.add(l);
        }
      }
    }

    if (locales.size() == 0) {
      locales.add(Locale.getDefault());
    }
  }

} // HeaderBase
