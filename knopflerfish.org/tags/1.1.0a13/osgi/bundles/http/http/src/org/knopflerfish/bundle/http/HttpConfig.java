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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ConfigurationException;


public class HttpConfig implements ManagedService {

  // public constants

  public final static String PORT_KEY = "port";
  public final static String HOST_KEY = "host";
  public final static String MIME_PROPS_KEY = "mime.map";
  public final static String SESSION_TIMEOUT_KEY = "session.timeout.default";
  public final static String CONNECTION_TIMEOUT_KEY = "connection.timeout";
  public final static String CONNECTION_MAX_KEY = "connection.max";
  public final static String DNS_LOOKUP_KEY = "dns.lookup";
  public final static String RESPONSE_BUFFER_SIZE_DEFAULT_KEY =
      "response.buffer.size.default";
  public final static String SERVICE_RANKING_KEY = "service.ranking";
  public final static String SECURE_KEY = "secure";
  public final static String CLIENT_AUTH_KEY = "client.authentication";
  public final static String KEYSTORE_URL_KEY = "keystore.url";
  public final static String KEYSTORE_PASS_KEY = "keystore.pass";

  private static final int PORT_DEFAULT        = 80;
  private static final int PORT_DEFAULT_SECURE = 443;


  // private fields

  private Dictionary configuration;

  private int      port = PORT_DEFAULT;
  private String   host = "";
  private final    Hashtable mimeMap = new Hashtable();
  private int      defaultSessionTimeout = 1200;
  private int      connectionTimeout = 30;
  private int      connectionMax = 50;
  private boolean  dnsLookup = true;
  private int      defaultResponseBufferSize = 16384;
  private int      serviceRanking = 1000; // NYI
  private boolean  isSecure = false;
  private boolean  clientAuth = false; // NYI
  private String   keystoreUrl = "";
  private String   keystorePass = "";


  // constructors

  public HttpConfig() throws ConfigurationException {
    this(null);
  }

  public HttpConfig(Dictionary configuration) throws ConfigurationException {

    this.configuration = HttpConfig.getDefaultConfig();

    updated(configuration);
  }


  // public methods

  public static Dictionary getDefaultConfig() {

    final Dictionary config = new Hashtable();

    boolean isSecure = Boolean.getBoolean("org.knopflerfish.http.secure");

    config.put(HttpConfig.PORT_KEY, 
	       isSecure 
	       ? Integer.getInteger("org.osgi.service.http.port.secure", 
				    PORT_DEFAULT_SECURE) 
	       : Integer.getInteger("org.osgi.service.http.port", 
				    PORT_DEFAULT));

    config.put(HttpConfig.HOST_KEY, 
	       System.getProperty("org.osgi.service.http.hostname", ""));

    Properties mimeProps = new Properties();
    try {
      mimeProps.load(HttpConfig.class.getResourceAsStream("/mime.default"));
      String propurl = System.getProperty("org.knopflerfish.http.mime.props", "");
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
    } catch (IOException ignore) { }
    Vector mimeVector = new Vector(mimeProps.size());
    Enumeration e = mimeProps.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      mimeVector.addElement(new String[] { key, mimeProps.getProperty(key) });
    }
    config.put(HttpConfig.MIME_PROPS_KEY, mimeVector);

    config.put(HttpConfig.SESSION_TIMEOUT_KEY, 
	       Integer.getInteger("org.knopflerfish.http.session.timeout.default", 1200));

    config.put(HttpConfig.CONNECTION_TIMEOUT_KEY, 
	       Integer.getInteger("org.knopflerfish.http.connection.timeout", 30));
    config.put(HttpConfig.CONNECTION_MAX_KEY, 
	       Integer.getInteger("org.knopflerfish.http.connection.max", 50));

    config.put(HttpConfig.DNS_LOOKUP_KEY, 
	       Boolean.valueOf(System.getProperty("org.knopflerfish.http.dnslookup", "false")));
    config.put(HttpConfig.RESPONSE_BUFFER_SIZE_DEFAULT_KEY, 
	       Integer.getInteger("org.knopflerfish.http.response.buffer.size.default", 16384));
    if (HttpConfig.class.getResource("/bundle_keystore") != null) {
      config.put(HttpConfig.SECURE_KEY, new Boolean(isSecure));

      config.put(HttpConfig.CLIENT_AUTH_KEY, 
		 Boolean.valueOf(System.getProperty("org.knopflerfish.http.secure.client.auth", "false")));

      config.put(HttpConfig.KEYSTORE_URL_KEY, 
		 System.getProperty("org.knopflerfish.http.secure.keystore.file", ""));

      config.put(HttpConfig.KEYSTORE_PASS_KEY, 
		 System.getProperty("org.knopflerfish.http.secure.keystore.password", ""));
    }

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
        if (key.equals(PORT_KEY)) {
          port = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(HOST_KEY)) {
          host = (String) value;
          this.configuration.put(key, value);
        } else if (key.equals(MIME_PROPS_KEY)) {
          Enumeration pairs = ((Vector) value).elements();
          while (pairs.hasMoreElements()) {
            Object o = (Object) pairs.nextElement();
            if (o instanceof Object[]) {
              Object[] pair = (Object[]) o;
              mimeMap.put((String) pair[0], (String) pair[1]);
            } else if (o instanceof Vector) {
              Vector pair = (Vector) o;
              mimeMap.put((String) pair.elementAt(0), (String) pair.elementAt(1));
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
          serviceRanking = ((Integer) value).intValue();
          this.configuration.put(key, value);
        } else if (key.equals(SECURE_KEY)) {
          isSecure = ((Boolean) value).booleanValue();
          this.configuration.put("scheme", isSecure ? "https" : "http");
        } else if (key.equals(CLIENT_AUTH_KEY)) {
          clientAuth = ((Boolean) value).booleanValue();
          this.configuration.put(key, value);
        } else if (key.equals(KEYSTORE_URL_KEY)) {
          keystoreUrl = (String) value;
        } else if (key.equals(KEYSTORE_PASS_KEY)) {
          keystorePass = (String) value;
        } else
          this.configuration.put(key, value);
      } catch (IndexOutOfBoundsException ioobe) {
        throw new ConfigurationException(key, "Wrong type");
      } catch (ClassCastException cce) {
        throw new ConfigurationException(key, "Wrong type: " +
                                              value.getClass().getName());
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

  public boolean isSecure() {
    return isSecure;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {

    this.port = port;

    configuration.put(PORT_KEY, new Integer(port));
  }

  public String getKeyStore() {
    return keystoreUrl; // NYI
  }

  public String getKeyStorePass() {
    return keystorePass; // NYI
  }

  public boolean getClientAuthentication() {
    return clientAuth;
  }

  public boolean getDNSLookup() {
    return dnsLookup;
  }


  // implements ManagedService

  public void updated(Dictionary configuration) throws ConfigurationException {
    mergeConfiguration(configuration); // NYI
  }

} // HttpConfig
