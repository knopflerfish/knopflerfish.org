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

package org.knopflerfish.bundle.consoletelnet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

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

    // constructors

    public TelnetConfig() throws ConfigurationException {
        this(null);

    }

    public TelnetConfig(Dictionary configuration) throws ConfigurationException {
        defaultUser = System.getProperty("org.knopflerfish.consoletelnet.user",
                defaultUser);
        defaultPassword = System.getProperty(
                "org.knopflerfish.consoletelnet.pwd", defaultPassword);
        port = Integer.getInteger("org.knopflerfish.consoletelnet.port", port)
                .intValue();

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
        try {
            String addressStr = (String) configuration.get(HOST_KEY);
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
            throw new IllegalArgumentException("Wrong type for " + HOST_KEY);
        } catch (UnknownHostException uhe) {
            throw new IllegalArgumentException("Cannot resolve " + HOST_KEY);
        } finally {

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

} // TelnetConfig
