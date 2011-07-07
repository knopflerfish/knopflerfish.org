/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

public class HttpConfig {

  // public constants

  public final static String HTTP_PORT_KEY = "port.http";
  public final static String HTTPS_PORT_KEY = "port.https";
  public final static String HOST_KEY = "host";
  public final static String MIME_PROPS_KEY = "mime.map";
  public final static String SESSION_TIMEOUT_KEY = "session.timeout.default";
  public final static String CONNECTION_TIMEOUT_KEY = "connection.timeout";
  public final static String CONNECTION_MAX_KEY = "connection.max";
  public final static String DNS_LOOKUP_KEY = "dns.lookup";
  public final static String RESPONSE_BUFFER_SIZE_DEFAULT_KEY
    = "response.buffer.size.default";
  public final static String SERVICE_RANKING_KEY = "service.ranking";
  public final static String HTTP_ENABLED_KEY = "http.enabled";
  public final static String HTTPS_ENABLED_KEY = "https.enabled";
  public final static String REQ_CLIENT_AUTH_KEY = "req.client.auth";
  private static final int HTTP_PORT_DEFAULT = 80;
  private static final int HTTPS_PORT_DEFAULT = 443;
  public final static String DEFAULT_CHAR_ENCODING_KEY
    = "org.knopflerfish.http.encoding.default";

  //
  public HttpConfigWrapper HTTP  = new HttpConfigWrapper(false, this);
  public HttpConfigWrapper HTTPS = new HttpConfigWrapper(true, this);

  // private fields
  private BundleContext bc;
  private Dictionary configuration;
  private int httpPort = HTTP_PORT_DEFAULT;
  private int httpsPort = HTTPS_PORT_DEFAULT;
  private String host = "";
  private final Hashtable mimeMap = new Hashtable();
  private int defaultSessionTimeout = 1200;
  private int connectionTimeout = 30;
  private int connectionMax = 50;
  private boolean dnsLookup = true;
  private int defaultResponseBufferSize = 16384;

  // private int serviceRanking = 1000; // NYI
  private boolean httpsEnabled = true;
  private boolean httpEnabled = true;
  private String defaultCharEncoding = "ISO-8859-1";
  private boolean requireClientAuth = false;

  // constructor(s)

  public HttpConfig(BundleContext bc, Dictionary configuration)
    throws ConfigurationException
  {
    this.bc = bc;
    this.configuration = HttpConfig.getDefaultConfig(bc);
    updated(configuration);
  }

  // public methods
  public static Dictionary getDefaultConfig(BundleContext bc) {

    final Dictionary config = new Hashtable();

    config.put(HttpConfig.HTTP_ENABLED_KEY,
               getPropertyAsBoolean(bc,
                                    "org.knopflerfish.http.enabled",
                                    "true"));
    config.put(HttpConfig.HTTPS_ENABLED_KEY,
               getPropertyAsBoolean(bc,
                                    "org.knopflerfish.http.secure.enabled",
                                    "true"));
    config.put(HttpConfig.HTTP_PORT_KEY,
               getPropertyAsInteger(bc,
                                    "org.osgi.service.http.port",
                                    HTTP_PORT_DEFAULT));
    config.put(HttpConfig.HTTPS_PORT_KEY,
               getPropertyAsInteger(bc,
                                    "org.osgi.service.http.secure.port",
                                    HTTPS_PORT_DEFAULT));
    config.put(HttpConfig.HOST_KEY,
               getPropertyAsString(bc,
                                   "org.osgi.service.http.hostname",
                                   ""));

    Properties mimeProps = new Properties();
    try {
      mimeProps.load(HttpConfig.class
                     .getResourceAsStream("/mime.default"));
      String propurl = getPropertyAsString(bc,
                                           "org.knopflerfish.http.mime.props",
                                           "");
      if (propurl.length() > 0) {
        URL url = new URL(propurl);
        Properties userMimeProps = new Properties();
        userMimeProps.load(url.openStream());
        Enumeration e = userMimeProps.keys();
        while (e.hasMoreElements()) {
          String key = (String) e.nextElement();
          mimeProps.put(key, userMimeProps.getProperty(key));
        }
      }
    } catch (MalformedURLException ignore) {
    } catch (IOException ignore) {
    }
    Vector mimeVector = new Vector(mimeProps.size());
    Enumeration e = mimeProps.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      mimeVector.addElement(new String[] { key,
                                           mimeProps.getProperty(key) });
    }
    config.put(HttpConfig.MIME_PROPS_KEY, mimeVector);

    config.put(HttpConfig.SESSION_TIMEOUT_KEY,
               getPropertyAsInteger(bc,
                                    "org.knopflerfish.http.session.timeout.default",
                                    1200));

    config.put(HttpConfig.CONNECTION_TIMEOUT_KEY,
               getPropertyAsInteger(bc,
                                    "org.knopflerfish.http.connection.timeout",
                                    30));
    config.put(HttpConfig.CONNECTION_MAX_KEY,
               getPropertyAsInteger(bc,
                                    "org.knopflerfish.http.connection.max",
                                    50));

    config.put(HttpConfig.DNS_LOOKUP_KEY,
               getPropertyAsBoolean(bc,
                                    "org.knopflerfish.http.dnslookup",
                                    "false"));
    config.put(HttpConfig.RESPONSE_BUFFER_SIZE_DEFAULT_KEY,
               getPropertyAsInteger(bc,
                                    "org.knopflerfish.http.response.buffer.size.default",
                                    16384));

    config.put(HttpConfig.DEFAULT_CHAR_ENCODING_KEY,
               getPropertyAsString(bc,
                                   HttpConfig.DEFAULT_CHAR_ENCODING_KEY,
                                   "ISO-8859-1"));

    config.put(HttpConfig.REQ_CLIENT_AUTH_KEY,
               getPropertyAsBoolean(bc,
                                    "org.knopflerfish.http.req.client.auth",
                                    "false"));

    return config;
  }

  public void mergeConfiguration(Dictionary configuration)
    throws ConfigurationException {

    if (configuration == null)
      return;

    Enumeration e = configuration.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      Object value = configuration.get(key);
      try {
        if (key.equals(HTTP_PORT_KEY)) {
          httpPort = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(HTTPS_PORT_KEY)) {
          httpsPort = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(HOST_KEY)) {
          host = (String) value;
          this.configuration.put(key, value);
        } else if (key.equals(MIME_PROPS_KEY)) {
          Enumeration pairs = ((Vector) value).elements();
          while (pairs.hasMoreElements()) {
            Object o = pairs.nextElement();
            if (o instanceof Object[]) {
              Object[] pair = (Object[]) o;
              mimeMap.put(pair[0], pair[1]);
            } else if (o instanceof Vector) {
              Vector pair = (Vector) o;
              mimeMap.put(pair.elementAt(0), pair.elementAt(1));
            }
          }
          this.configuration.put(key, value);
        } else if (key.equals(SESSION_TIMEOUT_KEY)) {
          defaultSessionTimeout = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(CONNECTION_TIMEOUT_KEY)) {
          connectionTimeout = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(CONNECTION_MAX_KEY)) {
          connectionMax = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(DNS_LOOKUP_KEY)) {
          dnsLookup = ((Boolean) value).booleanValue();
          this.configuration.put(key, value);
        } else if (key.equals(RESPONSE_BUFFER_SIZE_DEFAULT_KEY)) {
          defaultResponseBufferSize = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(SERVICE_RANKING_KEY)) {
          // serviceRanking = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(HTTP_ENABLED_KEY)) {
          this.httpEnabled = ((Boolean) value).booleanValue();
          this.configuration.put(key, value);
        } else if (key.equals(HTTPS_ENABLED_KEY)) {
          this.httpsEnabled = ((Boolean) value).booleanValue();
          this.configuration.put(key, value);
        } else if (key.equals(DEFAULT_CHAR_ENCODING_KEY)) {
          this.defaultCharEncoding = (String) value;
          this.configuration.put(key, value);
        } else if (key.equals(REQ_CLIENT_AUTH_KEY)) {
          this.requireClientAuth = ((Boolean) value).booleanValue();
          this.configuration.put(key, value);
        } else
          this.configuration.put(key, value);
      } catch (IndexOutOfBoundsException ioobe) {
        throw new ConfigurationException(key, "Wrong type");
      } catch (ClassCastException cce) {
        throw new ConfigurationException(key, "Wrong type: "
                                         + value.getClass().getName());
      }
    }
  }

  public Dictionary getConfiguration() {
    return configuration;
  }

  public int getMaxConnections() {
    return connectionMax;
  }

  public String getServerInfo() {
    return "The Knopflerfish HTTP Server"; // NYI
  }

  public String getMimeType(String file) {

    String mimeType = null;

    int index = file.lastIndexOf('.');
    if (index != -1)
      mimeType = (String) mimeMap.get(file.substring(index + 1));

    return mimeType;
  }

  public int getDefaultSessionTimeout() {
    return defaultSessionTimeout; // NYI
  }

  public int getDefaultBufferSize() {
    return defaultResponseBufferSize; // NYI
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public boolean isHttpEnabled() {
    return httpEnabled;
  }

  public boolean isHttpsEnabled() {
    return httpsEnabled;
  }

  public String getHost() {
    return host;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public int getHttpsPort() {
    return httpsPort;
  }

  public void setHttpPort(int port) {
    this.httpPort = port;
    configuration.put(HTTP_PORT_KEY, new Integer(port));
  }

  public void setHttpsPort(int port) {
    this.httpsPort = port;
    configuration.put(HTTPS_PORT_KEY, new Integer(port));
  }

  public boolean getDNSLookup() {
    return dnsLookup;
  }

  public boolean requireClientAuth() {
    return requireClientAuth;
  }

  public String getDefaultCharacterEncoding() {
    return defaultCharEncoding;
  }

  public void updated(Dictionary configuration) throws ConfigurationException {
    mergeConfiguration(configuration); // NYI
  }

  // private helper methods
  private static Boolean getPropertyAsBoolean(BundleContext bc,
                                              String name,
                                              String defVal )
  {
    String val = bc.getProperty(name);
    return Boolean.valueOf(val==null? defVal : val);
  }

  private static Integer getPropertyAsInteger(BundleContext bc,
                                              String name,
                                              int    defVal )
  {
    String val = bc.getProperty(name);
    return val==null ? new Integer(defVal) : Integer.valueOf(val);
  }

  private static String getPropertyAsString(BundleContext bc,
                                            String name,
                                            String defVal )
  {
    String val = bc.getProperty(name);
    return val==null ? defVal : val;
  }

} // HttpConfig
