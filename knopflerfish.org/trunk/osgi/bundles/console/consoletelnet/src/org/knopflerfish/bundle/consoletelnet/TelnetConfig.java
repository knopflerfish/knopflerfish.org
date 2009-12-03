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

package org.knopflerfish.bundle.consoletelnet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

public class TelnetConfig {

  // public constants

  public static final String PORT_KEY = "port";

  public static final String HOST_KEY = "host";

  public static final String UM_KEY = "um";

  public static final String REQUIRED_GROUP_KEY = "requiredGroup";

  public static final String FORBIDDEN_GROUP_KEY = "forbiddenGroup";

  // private fields

  private static Dictionary configuration;

  private static int port = 23;

  private static String host = "";

  private static boolean um = false;

  private static String requiredGroup = "";

  private static String forbiddenGroup = "";

  private static String defaultUser = "admin";

  private static String defaultPassword = "admin";

  private static boolean busyWait = false;

  // constructors

  public TelnetConfig(BundleContext bc)
    throws ConfigurationException
  {
    this(bc,null);
  }

  public TelnetConfig(BundleContext bc, Dictionary configuration)
    throws ConfigurationException
  {
    final String user = bc.getProperty("org.knopflerfish.consoletelnet.user");
    if (null!=user && 0<user.length()) {
      defaultUser = user;
    }

    final String pswd = bc.getProperty("org.knopflerfish.consoletelnet.pwd");
    if (null!=pswd && 0<pswd.length()) {
      defaultPassword = pswd;
    }

    final String bw = bc.getProperty("org.knopflerfish.consoletelnet.busywait");
    if (null!=bw) {
      busyWait = bw.trim().equalsIgnoreCase("true");
    }

    final String po = bc.getProperty("org.knopflerfish.consoletelnet.port");
    if (null!=po && 0<po.length()) {
      try {
        port = Integer.parseInt(po);
      } catch (Exception _e) {
      }
    }

    TelnetConfig.configuration = TelnetConfig.getDefaultConfig();
    updated(configuration);
  }

  // public methods

  public static Dictionary getDefaultConfig() {

    final Dictionary config = new Hashtable();

    config.put(TelnetConfig.PORT_KEY, new Integer(port));
    config.put(TelnetConfig.HOST_KEY, host);
    config.put(TelnetConfig.UM_KEY, new Boolean(um));
    config.put(TelnetConfig.REQUIRED_GROUP_KEY, requiredGroup);
    config.put(TelnetConfig.FORBIDDEN_GROUP_KEY, forbiddenGroup);

    return config;
  }

  // implements ManagedService

  public void updated(Dictionary configuration) throws ConfigurationException {
    mergeConfiguration(configuration);
  }

  public void mergeConfiguration(Dictionary configuration)
    throws ConfigurationException {
    if (configuration == null) {
      return;
    }

    Enumeration e = configuration.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      Object value = configuration.get(key);
      try {
        if (key.equals(PORT_KEY)) {
          port = ((Integer) value).intValue();
          TelnetConfig.configuration.put(key, value);
        } else if (key.equals(HOST_KEY)) {
          host = ((String) value);
          TelnetConfig.configuration.put(key, value);
        } else if (key.equals(UM_KEY)) {
          um = ((Boolean) value).booleanValue();
          TelnetConfig.configuration.put(key, value);
        } else if (key.equals(REQUIRED_GROUP_KEY)) {
          requiredGroup = ((String) value).trim();
          TelnetConfig.configuration.put(key, value);
        } else if (key.equals(FORBIDDEN_GROUP_KEY)) {
          forbiddenGroup = ((String) value).trim();
          TelnetConfig.configuration.put(key, value);
        } else
          TelnetConfig.configuration.put(key, value);
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

  public String getServerInfo() {
    return "The Knopflerfish Telnet Console Server";
  }

  public int getPort() {
    return port;
  }

  public InetAddress getAddress() {
    InetAddress inetAddress = null;
    String addressStr = null;
    try {
      addressStr = (String) configuration.get(HOST_KEY);
      if (addressStr != null) {
        addressStr = addressStr.trim();
        if ("".equals(addressStr)) {
          inetAddress = null;
        } else {
          inetAddress = InetAddress.getByName(addressStr);
        }
      } else {
        inetAddress = null;
      }
    } catch (ClassCastException cce) {
      throw new IllegalArgumentException
        ("Wrong type for " + HOST_KEY +", expected String found '"
         +configuration.get(HOST_KEY).getClass().getName() +"'.");
    } catch (UnknownHostException uhe) {
      throw new IllegalArgumentException
        ("Cannot resolve " + HOST_KEY +" '" +addressStr +"'.");
    }

    return inetAddress;
  }

  public boolean umRequired() {
    return um;
  }

  public String getRequiredGroup() {
    return requiredGroup;
  }

  public String getForbiddenGroup() {
    return forbiddenGroup;
  }

  /*
   * The following methods return a number of "configuration" parameters whose
   * values are contained in here as it is the most suitable place for them
   *
   * They have only packet visibility
   */

  int getSocketTimeout() {
    return 30000;
  }

  int getBacklog() {
    return 100;
  }

  String getDefaultUser() {
    return defaultUser;
  }

  String getDefaultPassword() {
    return defaultPassword;
  }

  String getInputPath() {
    return "telnet"; // To be defined
  }

  String getAuthorizationMethod() {
    return "passwd"; // To be defined
  }

  boolean getBusyWait() {
    return busyWait;
  }

} // TelnetConfig
