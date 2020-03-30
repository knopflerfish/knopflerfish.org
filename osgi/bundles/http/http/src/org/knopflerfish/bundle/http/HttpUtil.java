/*
 * Copyright (c) 2003-2017, KNOPFLERFISH project
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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpUtil
{
  // private constants

  private final static Dictionary<Integer, String> statusCodes =
    new Hashtable<Integer, String>();

  // public constants

  public final static String SERVLET_NAME_KEY =
    "org.knopflerfish.service.http.servlet.name";

  // Key strings for sessions according to the servlet specification
  public final static String SESSION_COOKIE_KEY = "JSESSIONID";

  public final static String SESSION_PARAMETER_KEY = ";jsessionid=";

  // Acceptable Time/Date formats according to the HTTP specification
  private final static SimpleDateFormat[] DATE_FORMATS =
    { new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
     new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
     new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

  static boolean OLD_STYLE_ROOT_ALIAS = false;

  static {
    for (final SimpleDateFormat element : DATE_FORMATS) {
      element.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    try {
      OLD_STYLE_ROOT_ALIAS =
        "true".equals(Activator.bc
            .getProperty("org.knopflerfish.service.http.oldstylerootalias"));
    } catch (final Exception ignored) {
    }
  }

  /**
   * Format the given epoch offset value as a date according to the preferred
   * date time format in RFC 2616 section 3.3.1. This is the date format to use
   * in all HTTP-headers.
   */
  public static String formatDate(final long value)
  {
    synchronized (DATE_FORMATS[0]) {
      return DATE_FORMATS[0].format(new Date(value));
    }
  }

  /**
   * Parse the given date according to the one of the date time format in RFC
   * 2616 section 3.3.1.
   *
   * @param date
   *          the string to parse.
   * @return the epoch offset value of the given date.
   * @throws IllegalArgumentException
   *           if the given string does not parse using any of the formats.
   */
  public static long parseDate(final String date)
  {
    synchronized (DATE_FORMATS[0]) {
      for (final SimpleDateFormat element : DATE_FORMATS) {
        try {
          return element.parse(date).getTime();
        } catch (final Exception ignore) {
        }
      }
    }
    throw new IllegalArgumentException("Invalid date value: " + date);
  }

  @SuppressWarnings("rawtypes")
  public final static Enumeration<?> EMPTY_ENUMERATION = new Enumeration() {
    public boolean hasMoreElements()
    {
      return false;
    }

    public Object nextElement()
    {
      throw new NoSuchElementException();
    }
  };

  // private methods

  private static void setupStatusCodes()
  {
    statusCodes.put(new Integer(HttpServletResponse.SC_CONTINUE), "Continue");
    // HACK SMA
    statusCodes.put(new Integer(HttpServletResponse.SC_EXPECTATION_FAILED),
                    "Expectation Failed");
    // END HACK SMA
    statusCodes.put(new Integer(HttpServletResponse.SC_SWITCHING_PROTOCOLS),
                    "Switching Protocols");
    statusCodes.put(new Integer(HttpServletResponse.SC_OK), "OK");
    statusCodes.put(new Integer(HttpServletResponse.SC_CREATED), "Created");
    statusCodes.put(new Integer(HttpServletResponse.SC_ACCEPTED), "Accepted");
    statusCodes
        .put(new Integer(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION),
             "Non-Authoritative Information");
    statusCodes.put(new Integer(HttpServletResponse.SC_NO_CONTENT),
                    "No Content");
    statusCodes.put(new Integer(HttpServletResponse.SC_RESET_CONTENT),
                    "Reset Content");
    statusCodes.put(new Integer(HttpServletResponse.SC_PARTIAL_CONTENT),
                    "Partial Content");
    statusCodes.put(new Integer(HttpServletResponse.SC_MULTIPLE_CHOICES),
                    "Multiple Choices");
    statusCodes.put(new Integer(HttpServletResponse.SC_MOVED_PERMANENTLY),
                    "Moved Permanently");
    statusCodes.put(new Integer(HttpServletResponse.SC_MOVED_TEMPORARILY),
                    "Moved Temporarily");
    statusCodes.put(new Integer(HttpServletResponse.SC_SEE_OTHER), "See Other");
    statusCodes.put(new Integer(HttpServletResponse.SC_NOT_MODIFIED),
                    "Not Modified");
    statusCodes.put(new Integer(HttpServletResponse.SC_USE_PROXY), "Use Proxy");
    statusCodes.put(new Integer(HttpServletResponse.SC_BAD_REQUEST),
                    "Bad Request");
    statusCodes.put(new Integer(HttpServletResponse.SC_UNAUTHORIZED),
                    "Unauthorized");
    statusCodes.put(new Integer(HttpServletResponse.SC_PAYMENT_REQUIRED),
                    "Payment Required");
    statusCodes.put(new Integer(HttpServletResponse.SC_FORBIDDEN), "Forbidden");
    statusCodes.put(new Integer(HttpServletResponse.SC_NOT_FOUND), "Not Found");
    statusCodes.put(new Integer(HttpServletResponse.SC_METHOD_NOT_ALLOWED),
                    "Method Not Allowed");
    statusCodes.put(new Integer(HttpServletResponse.SC_NOT_ACCEPTABLE),
                    "Not Acceptable");
    statusCodes
        .put(new Integer(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED),
             "Proxy Authentication Required");
    statusCodes.put(new Integer(HttpServletResponse.SC_REQUEST_TIMEOUT),
                    "Request Time-out");
    statusCodes.put(new Integer(HttpServletResponse.SC_CONFLICT), "Conflict");
    statusCodes.put(new Integer(HttpServletResponse.SC_GONE), "Gone");
    statusCodes.put(new Integer(HttpServletResponse.SC_LENGTH_REQUIRED),
                    "Length Required");
    statusCodes.put(new Integer(HttpServletResponse.SC_PRECONDITION_FAILED),
                    "Precondition Failed");
    statusCodes
        .put(new Integer(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE),
             "Request Entity Too Large");
    statusCodes.put(new Integer(HttpServletResponse.SC_REQUEST_URI_TOO_LONG),
                    "Request-URI Too Large");
    statusCodes.put(new Integer(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE),
                    "Unsupported Media Type");
    statusCodes.put(new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
                    "Internal Server Error");
    statusCodes.put(new Integer(HttpServletResponse.SC_NOT_IMPLEMENTED),
                    "Not Implemented");
    statusCodes.put(new Integer(HttpServletResponse.SC_BAD_GATEWAY),
                    "Bad Gateway");
    statusCodes.put(new Integer(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
                    "Service Unavailable");
    statusCodes.put(new Integer(HttpServletResponse.SC_GATEWAY_TIMEOUT),
                    "Gateway Time-out");
    statusCodes
        .put(new Integer(HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED),
             "HTTP Version not supported");
  }

  // public methods

  public static String getStatusMessage(int statusCode)
  {
    if (statusCodes.isEmpty()) {
      setupStatusCodes();
    }

    String statusMessage = statusCodes.get(new Integer(statusCode));

    if (statusMessage == null) {
      statusMessage = "Unknown Status Code";
    }

    return statusMessage;
  }

  public static String newString(byte[] ascii, int hibyte, int offset, int count)
  {
    final char[] value = new char[count];

    if (hibyte == 0) {
      for (int i = count; i-- > 0;) {
        value[i] = (char) (ascii[i + offset] & 0xff);
      }
    } else {
      hibyte <<= 8;
      for (int i = count; i-- > 0;) {
        value[i] = (char) (hibyte | (ascii[i + offset] & 0xff));
      }
    }

    return new String(value);
  }

  public static String encodeURLEncoding(String s)
  {
    if (s == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c == ' ') {
        sb.append('+');
      } else if (c > 127) {
        sb.append('%');
        if (c > 255) {
          throw new IllegalArgumentException("Illegal character: " + c);
        }
        sb.append(Integer.toHexString(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public static String decodeURLEncoding(String s)
  {
    if (s == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      switch (c) {
      case '+':
        sb.append(' ');
        break;
      case '%':
        try {
          sb.append((char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
          i += 2;
        } catch (final NumberFormatException nfe) {
          throw new IllegalArgumentException("Invalid URL encoding: "
                                             + s.substring(i, i + 3));
        } catch (final StringIndexOutOfBoundsException sioobe) {
          final String rest = s.substring(i);
          sb.append(rest);
          if (rest.length() == 2) {
            i++;
          }
        }
        break;
      default:
        sb.append(c);
        break;
      }
    }
    return sb.toString();
  }

  public static <E> Enumeration<E> enumeration(final Collection<E> c)
  {
    return new Enumeration<E>() {

      Iterator<E> i = c.iterator();

      public boolean hasMoreElements()
      {
        return i.hasNext();
      }

      public E nextElement()
      {
        return i.next();
      }

    };
  }

  /**
   * Extract the <tt>charset</tt> specification from the given content type
   * string and save the content type without charset parameter to the given
   * StringBuilder. The <tt>charset</tt> value is returned.
   *
   * @param contentType
   *          The content type string to parse.
   * @param contentTypeBare
   *          The content type string without the charset parameter will be
   *          appended.
   * @return the embedded character encoding or <tt>null</tt>.
   */
  public static String parseContentType(String contentType,
                                        StringBuilder contentTypeBare)
  {
    final int initialSbLength = contentTypeBare.length();
    String res = null;

    // Only parse if there seems to be any params at all
    if (-1 != contentType.indexOf(";")) {
      final StringTokenizer st = new StringTokenizer(contentType, ";");
      int count = 0;
      while (st.hasMoreTokens()) {
        final String param = st.nextToken().trim();
        final int ix = param.indexOf("=");
        if (ix != -1 && count > 0) { // the first token is the mime
          // type itself
          final String attrib = param.substring(0, ix).toLowerCase();
          final String token = param.substring(ix + 1);

          if ("charset".equals(attrib)) {
            res = token;
          } else {
            if (contentTypeBare.length() > initialSbLength) {
              contentTypeBare.append(";");
            }
            contentTypeBare.append(param);
          }
        } else {
          if (contentTypeBare.length() > initialSbLength) {
            contentTypeBare.append(";");
          }
          contentTypeBare.append(param);
        }
        count++;
      }
    } else {
      // No params present in the content type specification; retain it.
      contentTypeBare.append(contentType);
    }
    return res;
  }

  /**
   * Builds the content type by adding a <tt>charset</tt> specification to the
   * bare content type string.
   *
   * @param contentTypeBare
   *          The content type specification without charset.
   * @param characterEncoding
   *          The charset parameter value to add.
   *
   * @return the content type specification with embedded character encoding.
   */
  public static String buildContentType(String contentTypeBare,
                                        String characterEncoding)
  {
    final StringBuilder sb =
      new StringBuilder(contentTypeBare.length() + characterEncoding.length());
    final StringTokenizer st = new StringTokenizer(contentTypeBare, ";");
    int count = 0;
    while (st.hasMoreTokens()) {
      final String param = st.nextToken().trim();
      if (sb.length() > 0) {
        sb.append(";");
      }
      sb.append(param);
      if (0 == count) {
        sb.append(";charset=").append(characterEncoding);
      }
      count++;
    }
    return sb.toString();
  }

  public static String buildContentTypeSimple(String contentTypeBare,
                                              String characterEncoding)
  {
    if (characterEncoding == null)
      return contentTypeBare;
    
    return contentTypeBare + ";charset=" + characterEncoding;
  }

  /**
   * Class representing a parameter value in an HTTP {@code Access-XXX}-header.
   */
  public static class ParamQ
  {
    public final String param;
    public final Double q;

    /**
     * @param param
     * @param q
     */
    ParamQ(String param, Double q)
    {
      this.param = param;
      this.q = q;
    }

    /**
     * Construct a {@link ParamQ} from one token of an {@code Access-XXX}-header.
     * @param val the token to parse.
     */
    ParamQ(String val)
    {
      val = val.trim();
      if (val.length()==0) {
        throw new IllegalArgumentException("Found empty token in header");
      }

      final StringTokenizer st = new StringTokenizer(val, ";");
      // First token is the param name
      param = st.nextToken().trim();
      // Find the q-value (default is 1.0d)
      double qvalue = 1.0d;
      while (st.hasMoreTokens()) {
        final String paramDef = st.nextToken().trim();
        final int eqIx = paramDef.indexOf('=');
        if (-1 < eqIx) {
          final String paramName = paramDef.substring(0, eqIx).trim();
          final String paramValue = paramDef.substring(eqIx + 1).trim();
          if ("q".equals(paramName)) {
            try {
              qvalue = new Double(paramValue).doubleValue();
            } catch (final NumberFormatException ignore) {
            }
          }
        }
      }
      this.q = qvalue;
    }


    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((param == null) ? 0 : param.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final ParamQ other = (ParamQ) obj;
      if (param == null) {
        if (other.param != null) {
          return false;
        }
      } else if (!param.equals(other.param)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return param + "; q=" +q;
    }
  }


  /**
   * Parse {@code Access-XXX}-header and return a list of the params sorted on
   * q-values (highest first).
   *
   * @param value
   *          the header value to parse.
   *
   * @return List of params from the given header value in priority order.
   */
  public static List<ParamQ> parseAccessHeader(final String value)
  {
    final ArrayList<ParamQ> res = new ArrayList<ParamQ>();

    if (value!=null && value.length()>0) {
      final StringTokenizer st = new StringTokenizer(value, ",");
      while (st.hasMoreTokens()) {
        try {
          final ParamQ pq = new ParamQ(st.nextToken());
          insertParamQ(res, pq);
        } catch (final Exception e) {
          // ignore invalid items
        }
      }
    }
    return res;
  }

  /**
   * Inserts {@code pqNew} into the sorted list of {@code ParamQ} in q-value
   * order. I.e., Parameter with the highest q-value first. If list contains an
   * item with the same param-name as {@code pqNew} then the list is only
   * changed if the new items has a q-value that is better then the current one.
   *
   * @param params
   *          Ordered list of {@code ParamQ} items to insert into.
   * @param pqNew
   *          The new value to insert
   */
  private static void insertParamQ(final ArrayList<ParamQ> params,
                                   final ParamQ pqNew)
  {
    boolean insert = true;
    // If an old value is present either ignore insert or remove old value.
    for (final Iterator<ParamQ> pqIt = params.iterator(); pqIt.hasNext();) {
      final ParamQ pq = pqIt.next();
      if (pq.param.equals(pqNew.param)) {
        if (pq.q < pqNew.q) {
          // New value is better, remove the old one and insert new
          pqIt.remove();
        } else {
          // Old value is better ignore the new one.
          insert = false;
        }
      }
    }
    if (insert) {
      // Insert the new unique value into the list.
      int i = 0;
      while (i < params.size()) {
        final ParamQ pq = params.get(i);
        if (pqNew.q > pq.q) {
          params.add(i, pqNew);
          insert = false;
          break;
        }
        i++;
      }
      if (insert) {
        params.add(pqNew);
      }
    }
  }

  /**
   * Use the request header 'Accept-Encoding' to determine which encoding of
   * "gzip" and "identity" to prefer for the response.
   *
   * @param acceptEncoding
   *          The accept-encoding header to parse.
   * @return <code>true</code> if "gzip" is the preferred content encoding.
   */
  public static boolean useGZIPEncoding(HttpServletRequest request)
  {
    final String acceptEncoding = request.getHeader(HeaderBase.ACCEPT_ENCODING);
    if (null == acceptEncoding || 0 == acceptEncoding.length()) {
      return false;
    }

    // default quality values.
    double qGzip = 0d; // Must be present to get a 1.
    double qIdent = 1d; // Always one unless present with lower value.

    final List<ParamQ> params = parseAccessHeader(acceptEncoding);
    for (final ParamQ pq : params) {
      if ("gzip".equalsIgnoreCase(pq.param)
          || "x-gzip".equalsIgnoreCase(pq.param)) {
        qGzip = pq.q;
      } else if ("identity".equalsIgnoreCase(pq.param)) {
        qIdent = pq.q;
      }
    }
    return qGzip >= qIdent;
  }

  static String toAbsoluteURL(String loc, HttpServletRequest request)
  {
    if (null == loc) {
      return loc;
    }

    URL url = null;
    try {
      url = new URL(loc);
      if (null == url.getAuthority()) {
        return loc;
      }
    } catch (final MalformedURLException mfe1) {
      final String baseUrl = request.getRequestURL().toString();
      try {
        if (0 < loc.length() && ('?' == loc.charAt(0) || '#' == loc.charAt(0))) {
          url = new URL(baseUrl + loc);
        } else {
          url = new URL(new URL(baseUrl), loc);
        }
      } catch (final MalformedURLException mfe2) {
        throw new IllegalArgumentException(loc);
      }
    }
    return url.toExternalForm();
  }

  /**
   * Get the resource target string from an URI, removing the initial alias
   * part, except in the case when the alias is equal to "/".
   *
   * @param uri
   *          URI path to convert to a target path
   * @param alias
   *          initial alias part of {@code uri}.
   * @return if alias equals "/" return {@code uri} unchanged, otherwise return
   *         {@code uri} with initial alias part stripped.
   */
  public static String makeTarget(String uri, String alias)
  {
    return (!OLD_STYLE_ROOT_ALIAS && "/".equals(alias)) ? uri : uri
        .substring(alias.length());
  }

} // HttpUtil
