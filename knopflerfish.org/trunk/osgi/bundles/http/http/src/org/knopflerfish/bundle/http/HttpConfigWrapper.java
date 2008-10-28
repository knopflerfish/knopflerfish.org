/*
 * Copyright (c) 2004-2008, KNOPFLERFISH project
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

/**
 * @author tenderes
 *
 * There is a relationship of 1 : 1 between HttpServer objects and Http Config
 * instances. Each HttpServer instance should be able to expose all registered
 * resources both via HTTP and/or via HTTPS. This class enables us to provide
 * two different views to the same HttpConfig object, one for HTTP, one for
 * HTTPS. This keeps the processing of HTTPS-aware processing in other classes
 * to a minimum.
 */
public class HttpConfigWrapper {
    private final HttpConfig config;

    private boolean isSecure;

    public HttpConfigWrapper(boolean isSecure, HttpConfig config) {
        this.isSecure = isSecure;
        this.config = config;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public String getScheme() {
        return isSecure ? "https" : "http";
    }

    public int getConnectionTimeout() {
        return config.getConnectionTimeout();
    }

    public int getDefaultBufferSize() {
        return config.getDefaultBufferSize();
    }

    public int getDefaultSessionTimeout() {
        return config.getDefaultSessionTimeout();
    }

    public boolean getDNSLookup() {
        return config.getDNSLookup();
    }

    public String getHost() {
        return config.getHost();
    }

    public int getPort() {
        return isSecure ? config.getHttpsPort() : config.getHttpPort();
    }

    public void setPort(int port) {
        if (isSecure) {
            config.setHttpsPort(port);
        } else {
            config.setHttpPort(port);
        }
    }

    public int getMaxConnections() {
        return config.getMaxConnections();
    }

    public String getMimeType(String file) {
        return config.getMimeType(file);
    }

    public String getServerInfo() {
        return config.getServerInfo();
    }

    public boolean isEnabled() {
        return isSecure ? config.isHttpsEnabled() : config.isHttpEnabled();
    }

    public String getDefaultCharacterEncoding() {
        return config.getDefaultCharacterEncoding();
    }

    public boolean requireClientAuth() {
        return config.requireClientAuth();
    }

}
