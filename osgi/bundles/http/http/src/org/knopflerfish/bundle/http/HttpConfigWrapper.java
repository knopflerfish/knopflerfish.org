/*
 * Created on Aug 13, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
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
