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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import javax.servlet.http.HttpServletResponse;

public class HttpUtil {

    // private constants

    private final static Dictionary statusCodes = new Hashtable();

    // public constants

    public final static String SERVLET_NAME_KEY = "org.knopflerfish.service.http.servlet.name";

    // Key strings for sessions according to the servlet specification
    public final static String SESSION_COOKIE_KEY = "JSESSIONID";

    public final static String SESSION_PARAMETER_KEY = "jsessionid";

    // Acceptable Time/Date formats according to the HTTP specification
    public final static SimpleDateFormat[] DATE_FORMATS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

    static {
        for (int i = 0; i < DATE_FORMATS.length; i++)
            DATE_FORMATS[i].setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public final static Enumeration EMPTY_ENUMERATION = new Enumeration() {
        public boolean hasMoreElements() {
            return false;
        }

        public Object nextElement() {
            throw new NoSuchElementException();
        }
    };

    // private methods

    private static void setupStatusCodes() {

        statusCodes.put(new Integer(HttpServletResponse.SC_CONTINUE),
                "Continue");
        statusCodes.put(
                new Integer(HttpServletResponse.SC_SWITCHING_PROTOCOLS),
                "Switching Protocols");
        statusCodes.put(new Integer(HttpServletResponse.SC_OK), "OK");
        statusCodes.put(new Integer(HttpServletResponse.SC_CREATED), "Created");
        statusCodes.put(new Integer(HttpServletResponse.SC_ACCEPTED),
                "Accepted");
        statusCodes.put(new Integer(
                HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION),
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
        statusCodes.put(new Integer(HttpServletResponse.SC_SEE_OTHER),
                "See Other");
        statusCodes.put(new Integer(HttpServletResponse.SC_NOT_MODIFIED),
                "Not Modified");
        statusCodes.put(new Integer(HttpServletResponse.SC_USE_PROXY),
                "Use Proxy");
        statusCodes.put(new Integer(HttpServletResponse.SC_BAD_REQUEST),
                "Bad Request");
        statusCodes.put(new Integer(HttpServletResponse.SC_UNAUTHORIZED),
                "Unauthorized");
        statusCodes.put(new Integer(HttpServletResponse.SC_PAYMENT_REQUIRED),
                "Payment Required");
        statusCodes.put(new Integer(HttpServletResponse.SC_FORBIDDEN),
                "Forbidden");
        statusCodes.put(new Integer(HttpServletResponse.SC_NOT_FOUND),
                "Not Found");
        statusCodes.put(new Integer(HttpServletResponse.SC_METHOD_NOT_ALLOWED),
                "Method Not Allowed");
        statusCodes.put(new Integer(HttpServletResponse.SC_NOT_ACCEPTABLE),
                "Not Acceptable");
        statusCodes.put(new Integer(
                HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED),
                "Proxy Authentication Required");
        statusCodes.put(new Integer(HttpServletResponse.SC_REQUEST_TIMEOUT),
                "Request Time-out");
        statusCodes.put(new Integer(HttpServletResponse.SC_CONFLICT),
                "Conflict");
        statusCodes.put(new Integer(HttpServletResponse.SC_GONE), "Gone");
        statusCodes.put(new Integer(HttpServletResponse.SC_LENGTH_REQUIRED),
                "Length Required");
        statusCodes.put(
                new Integer(HttpServletResponse.SC_PRECONDITION_FAILED),
                "Precondition Failed");
        statusCodes.put(new Integer(
                HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE),
                "Request Entity Too Large");
        statusCodes.put(
                new Integer(HttpServletResponse.SC_REQUEST_URI_TOO_LONG),
                "Request-URI Too Large");
        statusCodes.put(new Integer(
                HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE),
                "Unsupported Media Type");
        statusCodes.put(new Integer(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
                "Internal Server Error");
        statusCodes.put(new Integer(HttpServletResponse.SC_NOT_IMPLEMENTED),
                "Not Implemented");
        statusCodes.put(new Integer(HttpServletResponse.SC_BAD_GATEWAY),
                "Bad Gateway");
        statusCodes.put(
                new Integer(HttpServletResponse.SC_SERVICE_UNAVAILABLE),
                "Service Unavailable");
        statusCodes.put(new Integer(HttpServletResponse.SC_GATEWAY_TIMEOUT),
                "Gateway Time-out");
        statusCodes.put(new Integer(
                HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED),
                "HTTP Version not supported");
    }

    // public methods

    public static String getStatusMessage(int statusCode) {

        if (statusCodes.isEmpty())
            setupStatusCodes();

        String statusMessage = (String) statusCodes
                .get(new Integer(statusCode));

        if (statusMessage == null)
            statusMessage = "Unknown Status Code";

        return statusMessage;
    }

    public static String newString(byte[] ascii, int hibyte, int offset,
            int count) {

        char[] value = new char[count];

        if (hibyte == 0) {
            for (int i = count; i-- > 0;)
                value[i] = (char) (ascii[i + offset] & 0xff);
        } else {
            hibyte <<= 8;
            for (int i = count; i-- > 0;)
                value[i] = (char) (hibyte | (ascii[i + offset] & 0xff));
        }

        return new String(value);
    }

    public static String encodeURLEncoding(String s) {

        if (s == null)
            return null;

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ')
                sb.append('+');
            else if (c > 127) {
                sb.append('%');
                if (c > 255)
                    throw new IllegalArgumentException("Illegal character: "
                            + c);
                sb.append(Integer.toHexString(c));
            } else
                sb.append(c);
        }
        return sb.toString();
    }

    public static String decodeURLEncoding(String s) {

        if (s == null)
            return null;

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '+':
                sb.append(' ');
                break;
            case '%':
                try {
                    sb.append((char) Integer.parseInt(
                            s.substring(i + 1, i + 3), 16));
                    i += 2;
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Invalid URL encoding: "
                            + s.substring(i, i + 3));
                } catch (StringIndexOutOfBoundsException sioobe) {
                    String rest = s.substring(i);
                    sb.append(rest);
                    if (rest.length() == 2)
                        i++;
                }
                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }

    public static void removeAll(Dictionary dictionary) {

        Enumeration e = dictionary.keys();
        while (e.hasMoreElements())
            dictionary.remove(e.nextElement());
    }

    public static Enumeration enumeration(final Collection c) {

        return new Enumeration() {

            Iterator i = c.iterator();

            public boolean hasMoreElements() {
                return i.hasNext();
            }

            public Object nextElement() {
                return i.next();
            }

        };
    }

} // HttpUtil
