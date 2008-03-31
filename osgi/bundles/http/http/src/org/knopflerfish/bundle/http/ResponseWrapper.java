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
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class ResponseWrapper implements Response {

    // private fields

    private final HttpServletResponse response;

    private ServletOutputStream sos = null;

    private PrintWriter pw = null;

    // constructors

    public ResponseWrapper(final HttpServletResponse response) {
        this.response = response;
    }

    // implements Response

    public OutputStream getRawOutputStream() {

        if (response instanceof Response) {
            return ((Response) response).getRawOutputStream();
        }
        return null;
    }

    // implements ServletResponse

    public void flushBuffer() throws IOException {
        response.flushBuffer();
    }

    public int getBufferSize() {
        return response.getBufferSize();
    }

    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    public Locale getLocale() {
        return response.getLocale();
    }

    public ServletOutputStream getOutputStream() throws IOException {

        if (pw != null)
            throw new IllegalStateException("getWriter() already called");

        if (sos == null) {
            final OutputStream os = getRawOutputStream();
            if (os == null)
                sos = response.getOutputStream();
            else
                sos = new ServletOutputStreamImpl(os);
        }

        return sos;
    }

    public PrintWriter getWriter() throws IOException {

        if (sos != null)
            throw new IllegalStateException("getOutputStream() already called");

        if (pw == null) {
            final OutputStream os = getRawOutputStream();
            if (os == null)
                pw = response.getWriter();
            else
                pw = new PrintWriter(new OutputStreamWriter(os,
                        getCharacterEncoding()));
        }

        return pw;
    }

    public boolean isCommitted() {
        return response.isCommitted();
    }

    public void reset() {
        response.reset();
    }

    public void setBufferSize(int size) {
        response.setBufferSize(size);
    }

    public void setContentLength(int length) {
        response.setContentLength(length);
    }

    public void setContentType(String contentType) {
        response.setContentType(contentType);
    }

    public void setLocale(Locale locale) {
        response.setLocale(locale);
    }

    // implements HttpServletResponse

    public void addCookie(Cookie cookie) {
        response.addCookie(cookie);
    }

    public void addDateHeader(String name, long value) {
        response.addDateHeader(name, value);
    }

    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        response.addIntHeader(name, value);
    }

    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    public String encodeRedirectURL(String url) {
        return response.encodeRedirectURL(url);
    }

    public String encodeRedirectUrl(String url) {
        return response.encodeRedirectUrl(url); // deprecated
    }

    public String encodeURL(String url) {
        return response.encodeURL(url);
    }

    public String encodeUrl(String url) {
        return response.encodeUrl(url); // deprecated
    }

    public void sendError(int code) throws IOException {
        response.sendError(code);
    }

    public void sendError(int code, String message) throws IOException {
        response.sendError(code, message);
    }

    public void sendRedirect(String uri) throws IOException {
        response.sendRedirect(uri);
    }

    public void setDateHeader(String name, long value) {
        response.setDateHeader(name, value);
    }

    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        response.setIntHeader(name, value);
    }

    public void setStatus(int code) {
        response.setStatus(code);
    }

    public void setStatus(int code, String message) {
        response.setStatus(code, message); // deprecated
    }

} // ResponseWrapper
