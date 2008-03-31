/*
 * Copyright (c) 2003, KNOPFLERFISH project
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

public class ResponseImpl implements Response, PoolableObject {

    // private fields

    private HttpConfigWrapper httpConfig;

    private int statusCode = SC_OK;

    private String statusMsg = null;

    private boolean keepAlive = false;

    private final Dictionary headers = new Hashtable();

    private final Vector cookies = new Vector();

    private RequestImpl request = null;

    private BodyOutputStream bodyOut = null;

    private ServletOutputStream sos = null;

    private PrintWriter pw = null;

    private String charEncoding = null;

    // private methods

    private void reset(boolean reset_headers) {

        if (isCommitted())
            throw new IllegalStateException("Cannot reset committed buffer");

        bodyOut.reset();

        if (reset_headers) {
            statusCode = SC_OK;
            statusMsg = null;

            HttpUtil.removeAll(headers);
            cookies.removeAllElements();

            sos = null;
            pw = null;
        }
    }

    // public methods

    public void init(OutputStream os, final HttpConfigWrapper httpConfig) {
        this.init(os, null, httpConfig);
    }

    public void init(OutputStream os, RequestImpl request,
            final HttpConfigWrapper httpConfig) {
        this.httpConfig = httpConfig;
        this.request = request;

        if (request != null)
            keepAlive = request.getKeepAlive();

        bodyOut = new BodyOutputStream(os, this, httpConfig
                .getDefaultBufferSize());
    }

    public boolean getKeepAlive() {
        return keepAlive;
    }

    public byte[] getHeaders() {

        if (statusMsg == null)
            setStatus(statusCode);

        if (keepAlive && containsHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY)) {
            setHeader(HeaderBase.CONNECTION_HEADER_KEY, "Keep-Alive");
            keepAlive = true;
        } else {
            setHeader(HeaderBase.CONNECTION_HEADER_KEY, "Close");
            keepAlive = false;
        }

        setDateHeader(HeaderBase.DATE_HEADER_KEY, System.currentTimeMillis());
        setHeader("MIME-Version", "1.0");
        setHeader("Server", httpConfig.getServerInfo());

        // set session cookie
        HttpSession session = null;
        if (request != null && (session = request.getSession(false)) != null)
            addCookie(new Cookie(HttpUtil.SESSION_COOKIE_KEY, session.getId()));

        // set cookies
        Enumeration e_cookies = cookies.elements();
        while (e_cookies.hasMoreElements())
            setCookieHeader((Cookie) e_cookies.nextElement());

        StringBuffer head = new StringBuffer(128);

        // append response line
        head.append(request != null ? request.getProtocol()
                : RequestBase.HTTP_1_0_PROTOCOL);
        head.append(" ");
        head.append(statusCode);
        head.append(" ");
        head.append(statusMsg);
        head.append("\r\n");

        // append headers
        Enumeration e = headers.keys();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Iterator values = ((ArrayList) headers.get(name)).iterator();
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
        int length = head.length();
        byte[] headBytes = new byte[length];
        for (int i = 0; i < length; i++)
            headBytes[i] = (byte) head.charAt(i);

        return headBytes;
    }

    public void setCookieHeader(Cookie cookie) {

        if (cookie == null)
            return;

        StringBuffer header = new StringBuffer(32);
        String attrValue;
        int maxAge;
        header.append(cookie.getName() + "=" + cookie.getValue());
        if ((attrValue = cookie.getComment()) != null)
            header.append(";Comment=" + attrValue);
        if ((attrValue = cookie.getDomain()) != null)
            header.append(";Domain=" + attrValue);
        if ((maxAge = cookie.getMaxAge()) != -1) {
            if (maxAge > 0) {
                SimpleDateFormat s = new SimpleDateFormat(
                        "EEE, dd-MMM-yyyy HH:mm:ss z");
                GregorianCalendar cal = new GregorianCalendar();
                cal.add(Calendar.SECOND, maxAge);
                header.append(";Expires=" + s.format(cal.getTime()));
            }
            header.append(";Max-Age=" + maxAge);
        }
        if ((attrValue = cookie.getPath()) != null)
            header.append(";Path=" + attrValue);
        else
            header.append(";Path=/");
        if (cookie.getSecure())
            header.append(";Secure");
        header.append(";Version=" + cookie.getVersion());

        setHeader("Set-Cookie", header.toString());
    }

    public void commit() throws IOException {

        if (sos != null)
            sos.flush();

        if (pw != null)
            pw.flush();

        if (!containsHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY)
                && !isCommitted())
            setContentLength(bodyOut.getBufferByteCount());

        flushBuffer();
    }

    // implements PoolableObject

    public void init() {
    }

    public void destroy() {

        statusCode = SC_OK;
        statusMsg = null;
        keepAlive = false;

        HttpUtil.removeAll(headers);
        cookies.removeAllElements();

        request = null;
        bodyOut = null;
        sos = null;
        pw = null;
        charEncoding = null;
    }

    // implements Response

    public OutputStream getRawOutputStream() {
        return bodyOut;
    }

    // implements HttpServletResponse

    public boolean containsHeader(String name) {
        return headers.get(name) != null;
    }

    public void setIntHeader(String name, int value) {
        setHeader(name, Integer.toString(value));
    }

    public void setDateHeader(String name, long value) {
        setHeader(name, HttpUtil.DATE_FORMATS[0].format(new Date(value)));
    }

    public void setHeader(String name, String value) {

        if (value == null) { // NYI: is this allowed?
            if (name.equals(HeaderBase.CONTENT_LENGTH_HEADER_KEY))
                bodyOut.setContentLength(Integer.MAX_VALUE);
            headers.remove(name);
        } else {
            if (name.equals(HeaderBase.CONTENT_LENGTH_HEADER_KEY))
                bodyOut.setContentLength(Integer.parseInt(value));
            ArrayList values = new ArrayList(5);
            values.add(value);
            headers.put(name, values);
        }
    }

    public void addIntHeader(String name, int value) {
        addHeader(name, Integer.toString(value));
    }

    public void addDateHeader(String name, long value) {
        addHeader(name, HttpUtil.DATE_FORMATS[0].format(new Date(value)));
    }

    public void addHeader(String name, String value) {

        ArrayList values = (ArrayList) headers.get(name);
        if (values == null)
            setHeader(name, value);
        else if (value != null) // NYI: is this allowed?
            values.add(value);
    }

    public void addCookie(Cookie cookie) {
        cookies.addElement(cookie);
    }

    public String encodeRedirectURL(String url) {
        return HttpUtil.encodeURLEncoding(url); // NYI: cookies, sessions
    }

    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url); // deprecated
    }

    public String encodeURL(String url) {
        return HttpUtil.encodeURLEncoding(url); // NYI: cookies, sessions
    }

    public String encodeUrl(String url) {
        return encodeURL(url); // deprecated
    }

    public void sendError(int statusCode) throws IOException {
        sendError(statusCode, null);
    }

    public void sendError(int statusCode, String statusMsg) throws IOException {

        reset(false);

        setStatus(statusCode, statusMsg);
        setContentType("text/html");

        ServletOutputStream out = new ServletOutputStreamImpl(bodyOut);
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

    public void sendRedirect(String url) throws IOException {

        setHeader("Location", url);
        sendError(SC_MOVED_TEMPORARILY);
    }

    public void setStatus(int statusCode) {
        setStatus(statusCode, null);
    }

    public void setStatus(int statusCode, String statusMsg) { // deprecated

        if (statusMsg == null)
            statusMsg = HttpUtil.getStatusMessage(statusCode);

        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
    }

    // implements ServletResponse

    public void setContentLength(int contentLength) {

        if (contentLength < 0) {
            setHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY, null);
        } else {
            setIntHeader(HeaderBase.CONTENT_LENGTH_HEADER_KEY, contentLength);
        }
    }

    public void setContentType(String contentType) {
        setHeader(HeaderBase.CONTENT_TYPE_HEADER_KEY, contentType);

        // default encoding is what HttpConfig says
        charEncoding = null;

        // Parse mime type for charset etc
        // Only parse if there seems to be any params at all
        if (-1 != contentType.indexOf(";")) {
            StringTokenizer st = new StringTokenizer(contentType, ";");
            int count = 0;
            while (st.hasMoreTokens()) {
                String param = st.nextToken().trim();
                int ix = param.indexOf("=");
                if (ix != -1 && count > 0) { // the first token is the mime
                                                // type itself
                    String attrib = param.substring(0, ix).toLowerCase();
                    String token = param.substring(ix + 1);

                    if ("charset".equals(attrib)) {
                        charEncoding = token;
                    }
                }
                count++;
            }
        }
    }

    public boolean isCommitted() {
        return bodyOut.isCommitted();
    }

    public void reset() {
        reset(true);
    }

    public int getBufferSize() {
        return bodyOut.getBufferSize();
    }

    public void setBufferSize(int size) {

        if (isCommitted())
            throw new IllegalStateException(
                    "Cannot set size of committed buffer");

        bodyOut.setBufferSize(size);
    }

    public void flushBuffer() throws IOException {
        bodyOut.flushBuffer();
    }

    public ServletOutputStream getOutputStream() {

        if (pw != null)
            throw new IllegalStateException("getWriter() already called");

        if (sos == null)
            sos = new ServletOutputStreamImpl(bodyOut);

        return sos;
    }

    public PrintWriter getWriter() throws IOException {

        if (sos != null)
            throw new IllegalStateException("getOutputStream() already called");

        if (pw == null)
            pw = new PrintWriter(new OutputStreamWriter(bodyOut,
                    getCharacterEncoding()));

        return pw;
    }

    public Locale getLocale() {
        return null;
    }

    public void setLocale(Locale locale) {
    }

    public String getCharacterEncoding() {
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

} // ResponseImpl
