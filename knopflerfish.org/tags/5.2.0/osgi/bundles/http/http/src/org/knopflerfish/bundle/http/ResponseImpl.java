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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ResponseImpl
  implements Response 
{
  // private fields

  private HttpConfigWrapper httpConfig;

  private int statusCode = SC_OK;

  private String statusMsg = null;

  private boolean keepAlive = false;
  
  private boolean chunked = false;

  private final Hashtable<String, List<String>> headers =
    new Hashtable<String, List<String>>();

  private final Vector<Cookie> cookies = new Vector<Cookie>();

  private RequestImpl request = null;

  private BodyOutputStreamZip bodyOut = null;

  private ServletOutputStream sos = null;

  private PrintWriter pw = null;

  // The character encoding to use for the response contents.
  private String charEncoding = null;

  // The content type of the response, without charset parameter.
  private String contentTypeBare = null;

  private Locale locale;

  private boolean useGzip = false;
    
  private boolean emptyBody = false;
  
  byte[] copyBuffer = new byte[4096];
  
  
  // private methods

  void reset(boolean reset_headers)
  {
    
    if (isCommitted()) {
      throw new IllegalStateException("Cannot reset committed buffer");
    }

    bodyOut.reset(false);
    
    emptyBody = false;
    useGzip = false;
    
    if (reset_headers) {
      statusCode = SC_OK;
      statusMsg = null;

      headers.clear();
      cookies.removeAllElements();

      contentTypeBare = null;
      charEncoding = null;
      locale = null;
      sos = null;
      pw = null;
    }
  }
  
  void resetHard() {
    bodyOut.reset(false);
    useGzip = false;
    
    statusCode = SC_OK;
    statusMsg = null;

    headers.clear();
    cookies.removeAllElements();

    contentTypeBare = null;
    charEncoding = null;
    locale = null;
    sos = null;
    pw = null;
    emptyBody = false;
    
  }

  // public methods

  public void init(OutputStream os, final HttpConfigWrapper httpConfig)
  {
    this.init(os, null, httpConfig);
  }

  public void init(OutputStream os,
                   RequestImpl request,
                   final HttpConfigWrapper httpConfig)
  {
    this.httpConfig = httpConfig;
    this.request = request;
    useGzip = false;
    
    if (bodyOut == null)
      try {
        bodyOut = new BodyOutputStreamZip(os, this, httpConfig.getDefaultBufferSize());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    else {
      bodyOut.init(os, this);
    }

  }
  
  public void handle() {
    keepAlive = request.getKeepAlive();
    chunked = request.isHTTP_1_1();
  }

  public String getContentType()
  {
    return getHeader(HeaderBase.CONTENT_TYPE_HEADER_KEY);
  }

  public void resetBuffer()
  {
    reset(false);
  }

  public void setKeepAlive(boolean b) {
    keepAlive = b;
  }

  public boolean getKeepAlive()
  {
    return keepAlive;
  }

  public byte[] getHeaders()
  {
    if (statusMsg == null) {
      setStatus(statusCode);
    }
    
    if (statusCode == SC_NOT_MODIFIED) {
      chunked = false;
    }

    // We only set keep-alive for OK and NOT_MODIFIED. Not for errors
    if (keepAlive) {
      if (statusCode >= 200 && statusCode < 300) {
        if (chunked || containsHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY)) {
          keepAlive = true;
        }
      }
      else if (statusCode == SC_NOT_MODIFIED) { // No body response
        keepAlive = true;
      }
    }
    
    // Only set Keep-Alive for HTTP/1.0 clients. HTTP/1.1 assumes it's the default
    // Also in HTTP/1.1 close is set to indicate we will close the connection
    if (!keepAlive) {
      setHeader(HeaderBase.CONNECTION_HEADER_KEY, "Close");
    }
    else if (!request.isHTTP_1_1()) {
      setHeader(HeaderBase.CONNECTION_HEADER_KEY, "Keep-Alive");
    }
    
    if (useGzip)
      setHeader(HeaderBase.CONTENT_ENCODING, "Gzip");
    
    setDateHeader(HeaderBase.DATE_HEADER_KEY, System.currentTimeMillis());
    setHeader("MIME-Version", "1.0");
    setHeader("Server", httpConfig.getServerInfo());
    
    if (chunked) {
      setHeader(HeaderBase.TRANSFER_ENCODING_KEY, HeaderBase.TRANSFER_ENCODING_VALUE_CHUNKED);
    }

    // set session cookie
    HttpSession session = null;
    if (request != null && (session = request.getSession(false)) != null) {
      addCookie(new Cookie(HttpUtil.SESSION_COOKIE_KEY, session.getId()));
    }

    // set cookies
    final Enumeration<Cookie> e_cookies = cookies.elements();
    while (e_cookies.hasMoreElements()) {
      setCookieHeader(e_cookies.nextElement());
    }

    final StringBuffer head = new StringBuffer(128);

    // append response line
    head.append(request != null
      ? request.getProtocol()
      : RequestBase.HTTP_1_0_PROTOCOL);
    head.append(" ");
    head.append(statusCode);
    head.append(" ");
    head.append(statusMsg);
    head.append("\r\n");

    // append headers
    final Enumeration<String> e = headers.keys();
    while (e.hasMoreElements()) {
      final String name = e.nextElement();
      final Iterator<?> values = ((ArrayList<?>) headers.get(name)).iterator();
      while (values.hasNext()) {
        head.append(name);
        head.append(": ");
        head.append(values.next());
        head.append("\r\n");
      }
    }

    // mark end of header with empty line
    head.append("\r\n");

    // return byte buffer
    final int length = head.length();
    final byte[] headBytes = new byte[length];
    for (int i = 0; i < length; i++) {
      headBytes[i] = (byte) head.charAt(i);
    }

    return headBytes;
  }

  // SimpleDateFormat and GregorianCalendar are expensive to create
  // and not thread safe; thus we try to share one instance of each
  // amongst all response object. Also note that RFC2616, 3.3.1 states
  // that all dates in HTTP-headers must be given in GMT.
  private static final SimpleDateFormat cookieExpiresDateFormatter =
    new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z");
  static {
    cookieExpiresDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private static final GregorianCalendar cookieExpiresCalendar =
    new GregorianCalendar();

  private void appendCookieExpires(final StringBuffer header, final int maxAge)
  {
    synchronized (cookieExpiresDateFormatter) {
      cookieExpiresCalendar.setTimeInMillis(System.currentTimeMillis());
      cookieExpiresCalendar.add(Calendar.SECOND, maxAge);
      header.append(";Expires=");
      header.append(cookieExpiresDateFormatter.format(cookieExpiresCalendar
          .getTime()));
    }
  }

  public void setCookieHeader(Cookie cookie)
  {
    if (cookie == null) {
      return;
    }

    final StringBuffer header = new StringBuffer(32);
    String attrValue;
    int maxAge;
    header.append(cookie.getName() + "=" + cookie.getValue());
    if ((attrValue = cookie.getComment()) != null) {
      header.append(";Comment=" + attrValue);
    }
    if ((attrValue = cookie.getDomain()) != null) {
      header.append(";Domain=" + attrValue);
    }
    if ((maxAge = cookie.getMaxAge()) != -1) {
      if (maxAge > 0) {
        appendCookieExpires(header, maxAge);
      }
      header.append(";Max-Age=" + maxAge);
    }
    if ((attrValue = cookie.getPath()) != null) {
      header.append(";Path=" + attrValue);
    } else {
      header.append(";Path=/");
    }
    if (cookie.getSecure()) {
      header.append(";Secure");
    }
    header.append(";Version=" + cookie.getVersion());

    setHeader("Set-Cookie", header.toString());
  }

  public void commit()
      throws IOException
  {
    if (sos != null) {
      sos.flush();
    }

    if (pw != null) {
      pw.flush();
    }

    if (!isChunked() && !containsHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY) && !isCommitted()) {
      setContentLength(bodyOut.getBufferByteCount());
    }

    bodyOut.flushBuffer(true);
  }


  public void destroy()
  {
    statusCode = SC_OK;
    statusMsg = null;
    keepAlive = false;

    headers.clear();
    cookies.removeAllElements();

    request = null;
   
    try {
      bodyOut.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    bodyOut = null;
    
    sos = null;
    pw = null;
    charEncoding = null;
    contentTypeBare = null;
    locale = null;
  }


  // implements HttpServletResponse

  public boolean containsHeader(String name)
  {
    return headers.get(name) != null;
  }

  public void setIntHeader(String name, int value)
  {
    setHeader(name, Integer.toString(value));
  }

  public void setDateHeader(String name, long value)
  {
    setHeader(name, HttpUtil.formatDate(value));
  }

  public void setHeader(String name, String value)
  {
    if (value == null) { // NYI: is this allowed?
      if (name.equals(HeaderBase.CONTENT_LENGTH_HEADER_KEY)) {
        bodyOut.setContentLength(Integer.MAX_VALUE);
      }
      headers.remove(name);
    } else {
      if (name.equals(HeaderBase.CONTENT_LENGTH_HEADER_KEY)) {
        bodyOut.setContentLength(Integer.parseInt(value));
      }
      final ArrayList<String> values = new ArrayList<String>(5);
      values.add(value);
      headers.put(name, values);
    }
  }

  public String getHeader(String name)
  {
    final ArrayList<?> values = (ArrayList<?>) headers.get(name);
    if (null != values) {
      return (String) values.get(0);
    }
    return null;
  }

  public void addIntHeader(String name, int value)
  {
    addHeader(name, Integer.toString(value));
  }

  public void addDateHeader(String name, long value)
  {
    addHeader(name, HttpUtil.formatDate(value));
  }

  public void addHeader(String name, String value)
  {
    final List<String> values = headers.get(name);
    if (values == null) {
      setHeader(name, value);
    } else if (value != null) {
      values.add(value);
    }
  }

  public void addCookie(Cookie cookie)
  {
    cookies.addElement(cookie);
  }

  public String encodeRedirectURL(String url)
  {
    // Only encodes relative URLs.
    if (url.startsWith("http:") || url.startsWith("https:")) {
      return url;
    } else {
      return encodeURL(url);
    }
  }

  public String encodeRedirectUrl(String url)
  {
    return encodeRedirectURL(url); // deprecated
  }

  public String encodeURL(String url)
  {
    // Append session id ";jsessionid=1234" to path of the URL when
    // session is present and client does not support cookies.
    HttpSession session = null;
    if (request != null && (session = request.getSession(false)) != null
        && !request.isRequestedSessionIdFromCookie()) {
      // NYI: Don't add jsessionid to external URLs.
      String path = HttpUtil.toAbsoluteURL(url, request);
      if (null != path) {
        String query = "";
        String ref = "";

        final int qPos = path.indexOf("?");
        if (-1 < qPos) {
          query = path.substring(qPos);
          path = path.substring(0, qPos);
        } else {
          final int hPos = path.indexOf("#");
          if (-1 < hPos) {
            ref = path.substring(hPos);
            path = path.substring(0, hPos);
          }
        }
        final StringBuffer sb = new StringBuffer(path);
        if (0 < sb.length()) {
          sb.append(HttpUtil.SESSION_PARAMETER_KEY).append(session.getId());
        }
        sb.append(query);
        sb.append(ref);
        return sb.toString();
      }
    }

    return url; // No rewrite needed.
  }

  public String encodeUrl(String url)
  {
    return encodeURL(url); // deprecated
  }

  public void sendError(int statusCode)
      throws IOException
  {
    sendError(statusCode, null);
  }

  public void sendError(int statusCode, String statusMsg)
      throws IOException
  {
    Activator.log.info(Thread.currentThread().getName() + " send error: " + statusMsg);
    reset(false);
    
    setStatus(statusCode, statusMsg);
    setContentType("text/html");

    @SuppressWarnings("resource")
    final ServletOutputStream out = new ServletOutputStreamImpl(bodyOut);
    out.println("<html>");
    out.println("<head>");
    out.print("<title>");
    out.print(this.statusCode);
    out.print(" ");
    out.print(this.statusMsg);
    out.println("</title>");
    out.println("</head>");
    out.println("<body>");
    out.print("<h1>");
    out.print(this.statusCode);
    out.print(" ");
    out.print(this.statusMsg);
    out.println("</h1>");
    out.println("</body>");
    out.println("</html>");

    commit();
  }

  // HACK SMA added the method to handle 100-continue
  public void sendContinue()
      throws IOException
  {
    sendError(SC_CONTINUE);
  }

  public void sendRedirect(String url)
      throws IOException
  {
    // Activator.log.info("sendRedirect() url=" + url);
    setHeader("Location", url);
    sendError(SC_MOVED_TEMPORARILY);
  }

  public void setStatus(int statusCode)
  {
    setStatus(statusCode, null);
  }

  public void setStatus(int statusCode, String statusMsg)
  { // deprecated
    // Activator.log.info("setStatus() statusCode=" + statusCode);
    if (statusMsg == null) {
      statusMsg = HttpUtil.getStatusMessage(statusCode);
    }

    this.statusCode = statusCode;
    this.statusMsg = statusMsg;
    
    updateResponseFromStatus();
  }

  // implements ServletResponse

  public void setContentLength(int contentLength)
  {
     // Activator.log.info("setContentLength(), length=" + contentLength);
    chunked = false;
    useGzip = false;
    bodyOut.reset(false);
    
    if (contentLength < 0) {
      setHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY, null);
    } else {
      setIntHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY, contentLength);
    }
  }

  public boolean isCommitted()
  {
    return bodyOut.isCommitted();
  }

  public void reset()
  {
    reset(true);
  }

  public int getBufferSize()
  {
    return bodyOut.getBufferSize();
  }

  public void setBufferSize(int size)
  {

    if (isCommitted()) {
      throw new IllegalStateException("Cannot set size of committed buffer");
    }

    bodyOut.setBufferSize(size);
  }

  public void flushBuffer()
      throws IOException
  {
    bodyOut.flushBuffer();
    // responseBody.flushBuffer();
  }

  public ServletOutputStream getOutputStream()
  {

    if (pw != null) {
      throw new IllegalStateException("getWriter() already called");
    }

    if (sos == null) {
      sos = new ServletOutputStreamImpl(bodyOut);
      // sos = new ServletOutputStreamImpl(bodyOut);
    }

    return sos;
  }

  public PrintWriter getWriter()
      throws IOException
  {

    if (sos != null) {
      throw new IllegalStateException("getOutputStream() already called");
    }

    if (pw == null) {
      pw =
        new PrintWriter(new OutputStreamWriter(bodyOut, getCharacterEncoding()));
    }

    return pw;
  }

  public Locale getLocale()
  {
    return locale;
  }

  public void setLocale(Locale locale)
  {
    if (isCommitted()) {
      return;
    }
    this.locale = locale;
    if (null == pw) {
      // Set the character encoding to use based on locale here.
      // In a web-app this is determined from the
      // <tt>locale-encoding-mapping-list</tt> element in the
      // deployment descriptor (<tt>web.xml</tt>). Here we need to
      // implement some other mechanism...
      // setCharacterEncoding(characterEncoding);
    }
    if (null != contentTypeBare) {
      setHeader(HeaderBase.CONTENT_TYPE_HEADER_KEY,
                HttpUtil.buildContentType(contentTypeBare,
                                          getCharacterEncoding()));
    }
  }

  public void setContentType(String contentType)
  {
    if (isCommitted()) {
      return;
    }
    final StringBuffer sb = new StringBuffer(contentType.length());
    final String newCharEncoding = HttpUtil.parseContentType(contentType, sb);
    contentTypeBare = sb.toString();
    if (null == pw) {
      if (null != newCharEncoding) {
        setCharacterEncoding(newCharEncoding);
      }
    }
    if (null != contentTypeBare) {
      setHeader(HeaderBase.CONTENT_TYPE_HEADER_KEY,
                HttpUtil.buildContentType(contentTypeBare,
                                          getCharacterEncoding()));
    }
    
    if (!bodyOut.inProgress() &&
        httpConfig.checkCompressMimeType(contentTypeBare) &&
        HttpUtil.useGZIPEncoding(request)) {
      useGzip = true;
      bodyOut.useGzip();
    }
  }

  public void setCharacterEncoding(String enc)
  {
    charEncoding = enc;
    if (null != contentTypeBare) {
      setHeader(HeaderBase.CONTENT_TYPE_HEADER_KEY,
                HttpUtil.buildContentType(contentTypeBare,
                                          getCharacterEncoding()));
    }
  }

  public String getCharacterEncoding()
  {
    // Use response specified encoding if present
    // otherwise use default encoding
    // Default encoding can be specified via CM
    // or system properties. See HttpConfig for
    // details
    if ((charEncoding == null) || (charEncoding.length() == 0)) {
      return httpConfig.getDefaultCharacterEncoding();
    }
    return charEncoding;
  }
  
  public boolean isChunked() {
    return chunked;
  }

  private void log(String s) {
    Activator.log.info(Thread.currentThread().getName() + " - " + s);
  }

  public void setEmptyBody(boolean b) {
    this.emptyBody = b;
    if (emptyBody) {
      useGzip = false;
      chunked = false;
      bodyOut.reset(false);
    }
  }
  
  public boolean useGzip()
  {
    return false;
    // return useGzip;
  }
  
  private void updateResponseFromStatus() {
    if (isEmptyBodyStatus()) {
      setEmptyBody(true);
    }
  }
  
  private boolean isEmptyBodyStatus() {
    if (statusCode == SC_NOT_MODIFIED)
      return true;
    return false;
  }
  
  
  // Implements Response
  @Override
  public OutputStream getRawOutputStream() {
    return bodyOut;
  }
  
  @Override
  public void copy(InputStream is) throws IOException {
    int bytesRead;
    
    while ((bytesRead = is.read(copyBuffer)) != -1) {
      bodyOut.write(copyBuffer, 0, bytesRead);
    }
  }
} // ResponseImpl
