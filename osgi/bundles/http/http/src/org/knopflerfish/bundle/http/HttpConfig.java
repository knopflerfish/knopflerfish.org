/*
 * Copyright (c) 2003-2015 KNOPFLERFISH project
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.knopflerfish.util.Text;

public class HttpConfig
{

  // public constants

  public final static String HTTP_PORT_KEY = "port.http";
  public final static String HTTPS_PORT_KEY = "port.https";
  public final static String HOST_KEY = "host";
  public final static String MIME_PROPS_KEY = "mime.map";
  public final static String SESSION_TIMEOUT_KEY = "session.timeout.default";
  public final static String CONNECTION_TIMEOUT_KEY = "connection.timeout";
  public final static String CONNECTION_MAX_KEY = "connection.max";
  public final static String DNS_LOOKUP_KEY = "dns.lookup";
  public final static String RESPONSE_BUFFER_SIZE_DEFAULT_KEY =
    "response.buffer.size.default";
  public final static String SERVICE_RANKING_KEY = "service.ranking";
  public final static String HTTP_ENABLED_KEY = "http.enabled";
  public final static String HTTPS_ENABLED_KEY = "https.enabled";
  public final static String REQ_CLIENT_AUTH_KEY = "req.client.auth";
  private static final int HTTP_PORT_DEFAULT = 80;
  private static final int HTTPS_PORT_DEFAULT = 443;
  public final static String DEFAULT_CHAR_ENCODING_KEY =
    "org.knopflerfish.http.encoding.default";
  public final static String TRACE_ENABLED =
    "org.knopflerfish.http.trace.enabled";
  public final static String LIMIT_REQUEST_LINE =
    "org.knopflerfish.http.limit.requestline";
  public final static String LIMIT_POST_SIZE =
    "org.knopflerfish.http.limit.postsize";
  public final static String LIMIT_REQUEST_HEADERS =
    "org.knopflerfish.http.limit.requestheaders";
  
  // Maximum number of request worker threads
  public final static String MAX_WORKER_THREADS_KEY = "org.knopflerfish.http.threads.max";
  private final static int DEFAULT_THREADS_MAX = 5;
  private int threadsMax = DEFAULT_THREADS_MAX;
  
//Maximum number of keep-alive worker threads. Should always be at least one less than max threads
  public final static String KEEP_ALIVE_THREADS_KEY = "org.knopflerfish.http.threads.keep_alive";
  private final static int DEFAULT_THREADS_KEEP_ALIVE = 4;
  private int threadsKeepAlive = DEFAULT_THREADS_KEEP_ALIVE;
  
  //  Timeout in milliseconds after which an idle thread stops
  public final static String IDLE_WORKER_THREAD_TIMEOUT_KEY = "org.knopflerfish.http.threads.idle_timeout";
  private final static int DEFAULT_THREADS_IDLE_TIMEOUT = 15000;  
  private int threadsIdleTimeout = DEFAULT_THREADS_IDLE_TIMEOUT;
  
  // Comma-separated list of mime types that will always be compressed.  
  // Just specifying the type and leaving out the subtype applies compression to all subtypes
  // E.g. "text" will apply to text/html, text/plain etc
  public final static String ALWAYS_ZIP_MIME_TYPES_KEY = "org.knopflerfish.http.always_compress.mime_types";
  private final static String DEFAULT_ALWAYS_ZIP_MIME_TYPES = "text";
  private final Hashtable<String, String> compressMimeTypes = new Hashtable<String, String>(10);
  //
  public HttpConfigWrapper HTTP = new HttpConfigWrapper(false, this);
  public HttpConfigWrapper HTTPS = new HttpConfigWrapper(true, this);

  // private fields
  private final Dictionary<String, Object> configuration;
  private int httpPort = HTTP_PORT_DEFAULT;
  private int httpsPort = HTTPS_PORT_DEFAULT;
  private String host = "";
  private final Hashtable<Object, Object> mimeMap =
    new Hashtable<Object, Object>();
  private int defaultSessionTimeout = 1200;
  private int connectionTimeout = 30;
  private int connectionMax = 50;
  
  private final static boolean DNS_LOOKUP_DEFAULT = false;
  private boolean dnsLookup = DNS_LOOKUP_DEFAULT;
  
  private int defaultResponseBufferSize = 8192;
  
  // private int serviceRanking = 1000; // NYI
  private boolean httpsEnabled = true;
  private boolean httpEnabled = true;
  private String defaultCharEncoding = "ISO-8859-1";
  private boolean requireClientAuth = false;
  private boolean traceEnabled = false;
  private int limitRequestLine = 8190;
  private int limitPostSize = -1; // no limit
  private int limitRequestHeaders = 100;

  // constructor(s)

  public HttpConfig(BundleContext bc, Dictionary<String, ?> configuration)
    throws ConfigurationException
  {
    this.configuration = HttpConfig.getDefaultConfig(bc);
    updated(configuration);
  }

  // public methods
  public static Dictionary<String, Object> getDefaultConfig(BundleContext bc)
  {
    final Dictionary<String, Object> config = new Hashtable<String, Object>();

    config
        .put(HttpConfig.HTTP_ENABLED_KEY,
             getPropertyAsBoolean(bc, "org.knopflerfish.http.enabled", true));
    config.put(HttpConfig.HTTPS_ENABLED_KEY,
               getPropertyAsBoolean(bc, "org.knopflerfish.http.secure.enabled",
                                    true));
    config.put(HttpConfig.HTTP_PORT_KEY,
               getPropertyAsInteger(bc, "org.osgi.service.http.port",
                                    HTTP_PORT_DEFAULT));
    config.put(HttpConfig.HTTPS_PORT_KEY,
               getPropertyAsInteger(bc, "org.osgi.service.http.secure.port",
                                    HTTPS_PORT_DEFAULT));
    config.put(HttpConfig.HOST_KEY,
               getPropertyAsString(bc, "org.osgi.service.http.hostname", ""));

    final Properties mimeProps = new Properties();
    try {
      final InputStream mis = HttpConfig.class.getResourceAsStream("/mime.default");
      if (mis != null) {
        try {
          mimeProps.load(mis);
        } finally {
          mis.close();
        }
        final String propurl =
          getPropertyAsString(bc, "org.knopflerfish.http.mime.props", "");
        if (propurl.length() > 0) {
          final URL url = new URL(propurl);
          final Properties userMimeProps = new Properties();
          final InputStream pis = url.openStream();
          userMimeProps.load(pis);
          final Enumeration<?> e = userMimeProps.keys();
          while (e.hasMoreElements()) {
            final String key = (String) e.nextElement();
            mimeProps.put(key, userMimeProps.getProperty(key));
          }
          pis.close();
        }
      }
    } catch (final MalformedURLException ignore) {
    } catch (final IOException ignore) {
    }
    final Vector<String[]> mimeVector = new Vector<String[]>(mimeProps.size());
    @SuppressWarnings({ "rawtypes", "unchecked" })
    final Enumeration<String> e = ((Hashtable) mimeProps).keys();
    while (e.hasMoreElements()) {
      final String key = e.nextElement();
      mimeVector.addElement(new String[] { key, mimeProps.getProperty(key) });
    }
    config.put(HttpConfig.MIME_PROPS_KEY, mimeVector);

    config
        .put(HttpConfig.SESSION_TIMEOUT_KEY,
             getPropertyAsInteger(bc,
                                  "org.knopflerfish.http.session.timeout.default",
                                  1200));

    config.put(HttpConfig.CONNECTION_TIMEOUT_KEY,
               getPropertyAsInteger(bc,
                                    "org.knopflerfish.http.connection.timeout",
                                    30));
    config.put(HttpConfig.CONNECTION_MAX_KEY,
               getPropertyAsInteger(bc, "org.knopflerfish.http.connection.max",
                                    50));

    config.put(HttpConfig.DNS_LOOKUP_KEY,
               getPropertyAsBoolean(bc, "org.knopflerfish.http.dnslookup",
                                    DNS_LOOKUP_DEFAULT));
    config
        .put(HttpConfig.RESPONSE_BUFFER_SIZE_DEFAULT_KEY,
             getPropertyAsInteger(bc,
                                  "org.knopflerfish.http.response.buffer.size.default",
                                  16384));

    config.put(HttpConfig.DEFAULT_CHAR_ENCODING_KEY,
               getPropertyAsString(bc, HttpConfig.DEFAULT_CHAR_ENCODING_KEY,
                                   "ISO-8859-1"));

    config.put(HttpConfig.TRACE_ENABLED,
               getPropertyAsBoolean(bc, HttpConfig.TRACE_ENABLED, false));

    config.put(HttpConfig.LIMIT_REQUEST_LINE,
               getPropertyAsInteger(bc, HttpConfig.LIMIT_REQUEST_LINE, 8190));
    config.put(HttpConfig.LIMIT_POST_SIZE,
               getPropertyAsInteger(bc, HttpConfig.LIMIT_POST_SIZE, -1));
    config.put(HttpConfig.LIMIT_REQUEST_HEADERS,
               getPropertyAsInteger(bc, HttpConfig.LIMIT_REQUEST_HEADERS, 100));
    config.put(HttpConfig.REQ_CLIENT_AUTH_KEY,
               getPropertyAsBoolean(bc,
                                    "org.knopflerfish.http.req.client.auth",
                                    false));
    config.put(HttpConfig.MAX_WORKER_THREADS_KEY,
               getPropertyAsInteger(bc, HttpConfig.MAX_WORKER_THREADS_KEY, DEFAULT_THREADS_MAX));
    config.put(HttpConfig.KEEP_ALIVE_THREADS_KEY,
               getPropertyAsInteger(bc, HttpConfig.KEEP_ALIVE_THREADS_KEY, DEFAULT_THREADS_KEEP_ALIVE));
    config.put(HttpConfig.IDLE_WORKER_THREAD_TIMEOUT_KEY,
               getPropertyAsInteger(bc, HttpConfig.IDLE_WORKER_THREAD_TIMEOUT_KEY, DEFAULT_THREADS_IDLE_TIMEOUT));
    config.put(HttpConfig.ALWAYS_ZIP_MIME_TYPES_KEY,
               getPropertyAsString(bc, HttpConfig.ALWAYS_ZIP_MIME_TYPES_KEY, DEFAULT_ALWAYS_ZIP_MIME_TYPES));
               
    return config;
  }

  public void mergeConfiguration(Dictionary<String, ?> configuration)
      throws ConfigurationException
  {
    if (configuration == null) {
      return;
    }

    final Enumeration<String> e = configuration.keys();
    while (e.hasMoreElements()) {
      final String key = e.nextElement();
      final Object value = configuration.get(key);
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
          final Enumeration<?> pairs = ((Vector<?>) value).elements();
          while (pairs.hasMoreElements()) {
            final Object o = pairs.nextElement();
            if (o instanceof Object[]) {
              final Object[] pair = (Object[]) o;
              mimeMap.put(pair[0], pair[1]);
            } else if (o instanceof Vector) {
              final Vector<?> pair = (Vector<?>) o;
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
        } else if (key.equals(TRACE_ENABLED)) {
          this.traceEnabled = ((Boolean) value).booleanValue();
          this.configuration.put(key, value);
        } else if (key.equals(LIMIT_REQUEST_LINE)) {
          this.limitRequestLine = ((Integer) value).intValue();
          ServletInputStreamImpl.setLimitRequestLine(this.limitRequestLine); // set
                                                                             // globally
          this.configuration.put(key, value);
        } else if (key.equals(LIMIT_POST_SIZE)) {
          this.limitPostSize = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(LIMIT_REQUEST_HEADERS)) {
          this.limitRequestHeaders = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(REQ_CLIENT_AUTH_KEY)) {
          this.requireClientAuth = ((Boolean) value).booleanValue();
          this.configuration.put(key, value);
        } else if (key.equals(MAX_WORKER_THREADS_KEY)) {
          this.threadsMax = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(KEEP_ALIVE_THREADS_KEY)) {
          this.threadsKeepAlive = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(IDLE_WORKER_THREAD_TIMEOUT_KEY)) {
          this.threadsIdleTimeout = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(ALWAYS_ZIP_MIME_TYPES_KEY)) {
          parseCompressMimeTypes((String)value);
          this.configuration.put(key, value);
        } else {
          this.configuration.put(key, value);
        }
      } catch (final IndexOutOfBoundsException ioobe) {
        throw new ConfigurationException(key, "Wrong type");
      } catch (final ClassCastException cce) {
        throw new ConfigurationException(key, "Wrong type: "
                                              + value.getClass().getName());
      }
    }
  }

  public Dictionary<String, Object> getConfiguration()
  {
    return configuration;
  }

  public int getMaxConnections()
  {
    return connectionMax;
  }

  public String getServerInfo()
  {
    return "The Knopflerfish HTTP Server"; // NYI
  }

  public String getMimeType(String file)
  {
    String mimeType = null;

    final int index = file.lastIndexOf('.');
    if (index != -1) {
      mimeType = (String) mimeMap.get(file.substring(index + 1));
    }

    return mimeType;
  }

  public int getDefaultSessionTimeout()
  {
    return defaultSessionTimeout; // NYI
  }

  public int getDefaultBufferSize()
  {
    return defaultResponseBufferSize; // NYI
  }

  public int getConnectionTimeout()
  {
    return connectionTimeout;
  }

  public boolean isHttpEnabled()
  {
    return httpEnabled;
  }

  public boolean isHttpsEnabled()
  {
    return httpsEnabled;
  }

  public boolean isTraceEnabled()
  {
    return traceEnabled;
  }

  public int getLimitPostSize()
  {
    return limitPostSize;
  }

  public int getLimitRequestHeaders()
  {
    return limitRequestHeaders;
  }

  public String getHost()
  {
    return host;
  }

  public int getHttpPort()
  {
    return httpPort;
  }

  public int getHttpsPort()
  {
    return httpsPort;
  }

  public void setHttpPort(int port)
  {
    this.httpPort = port;
    configuration.put(HTTP_PORT_KEY, new Integer(port));
  }

  public void setHttpsPort(int port)
  {
    this.httpsPort = port;
    configuration.put(HTTPS_PORT_KEY, new Integer(port));
  }

  public boolean getDNSLookup()
  {
    return dnsLookup;
  }

  public boolean requireClientAuth()
  {
    return requireClientAuth;
  }

  public String getDefaultCharacterEncoding()
  {
    return defaultCharEncoding;
  }

  public int getMaxThreads() {
    return threadsMax;
  }
  
  public int getKeepAliveThreads() {
    return threadsKeepAlive;
  }
  
  public int getThreadIdleTimeout() {
    return threadsIdleTimeout;
  }
  
  public void updated(Dictionary<String, ?> configuration)
      throws ConfigurationException
  {
    mergeConfiguration(configuration);
  }

  
  public boolean checkCompressMimeType(String type) {
    
    if (compressMimeTypes.containsKey(type))
      return true;
    final int ix = type.indexOf('/');
    if (ix == -1)
      return false;
    return compressMimeTypes.containsKey(type.substring(0,ix));
  }
  
  // private helper methods
  private static Boolean getPropertyAsBoolean(BundleContext bc,
                                              String name,
                                              boolean defVal)
  {
    final String val = bc.getProperty(name);
    return val == null ? defVal : Boolean.valueOf(val);
  }

  private static Integer getPropertyAsInteger(BundleContext bc,
                                              String name,
                                              int defVal)
  {
    final String val = bc.getProperty(name);
    return val == null ? new Integer(defVal) : Integer.valueOf(val);
  }

  private static String getPropertyAsString(BundleContext bc,
                                            String name,
                                            String defVal)
  {
    final String val = bc.getProperty(name);
    return val == null ? defVal : val;
  }
  
  private void parseCompressMimeTypes(String value) {
    compressMimeTypes.clear();
    String[] types = Text.splitwords(value, ",", '"');
    for (int i = 0; i < types.length; i++) {
      compressMimeTypes.put(types[i], types[i]);
    }
  }

} // HttpConfig
