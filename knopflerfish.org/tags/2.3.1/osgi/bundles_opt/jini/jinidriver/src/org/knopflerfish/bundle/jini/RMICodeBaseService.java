/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
package org.knopflerfish.bundle.jini;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import org.osgi.service.http.HttpService;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import java.util.Properties;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
public class RMICodeBaseService {
    public static final String RMI_SERVER_CODEBASE = "java.rmi.server.codebase";
    private static RMICodeBaseService rmiCodeBaseService = null;
    private static BundleHttpContext bundleHttpContext = null;
    private static HttpService httpService = null;
    private static String mountpoint = null;
    private static URL codebase = null;

    /**
     * Creates a new RMICodeBaseService object.
     *
     * @param mountpoint DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    private RMICodeBaseService(String mountpoint) throws Exception {
        this.mountpoint = mountpoint;

        ServiceReference ref = Activator.bc.getServiceReference(
                "org.osgi.service.http.HttpService");
        httpService = (HttpService) Activator.bc.getService(ref);

        if (setCodeBase(prepareCodeBase(ref, httpService, mountpoint))) {
            bundleHttpContext = new BundleHttpContext();

            try {
                httpService.registerResources(mountpoint, "", bundleHttpContext);
            } catch (Exception ex) {
                Debug.printDebugInfo(10,
                    "Could not register mointpount " + mountpoint);
                throw new Exception("Mountpoint already in use");
            }
        } else {
            Debug.printDebugInfo(10,
                "Could not set " + RMI_SERVER_CODEBASE + " property");
            throw new Exception("Unable to set " + RMI_SERVER_CODEBASE +
                " property");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param mountpoint DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public static RMICodeBaseService getRMICodeBaseService(String mountpoint)
        throws Exception {
        if (rmiCodeBaseService == null) {
            rmiCodeBaseService = new RMICodeBaseService(mountpoint);
        }

        return rmiCodeBaseService;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static RMICodeBaseService getRMICodeBaseService() {
        return rmiCodeBaseService;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getCodebase() {
        return codebase.toString();
    }

    /**
     * DOCUMENT ME!
     */
    public void destroyService() {
        httpService.unregister(mountpoint);
        rmiCodeBaseService = null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param b DOCUMENT ME!
     */
    public void setCodebaseForBundle(Bundle b) {
        bundleHttpContext.ensureCodebaseFor(b);
    }

    /**
     * DOCUMENT ME!
     *
     * @param b DOCUMENT ME!
     */
    public void removeCodebaseForBundle(Bundle b) {
        bundleHttpContext.removeCodebaseFor(b);
    }

    /**
     * DOCUMENT ME!
     *
     * @param codebase DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private boolean setCodeBase(URL codebase) {
        if (codebase == null) {
            return false;
        }

        Properties props = System.getProperties();

        if (props.getProperty(RMICodeBaseService.RMI_SERVER_CODEBASE) != null) {
            if (!props.getProperty(RMICodeBaseService.RMI_SERVER_CODEBASE)
                          .equals(codebase.toString())) {
                return false;
            }
        }

        this.codebase = codebase;
        props.setProperty(RMICodeBaseService.RMI_SERVER_CODEBASE,
            codebase.toString());

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param ref DOCUMENT ME!
     * @param httpService DOCUMENT ME!
     * @param mountpoint DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private URL prepareCodeBase(ServiceReference ref, HttpService httpService,
        String mountpoint) {
        String host = null;

        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return null;
        }

        if (httpService == null) {
            return null;
        }

        if (ref == null) {
            return null;
        }

        int port = ((Integer) (ref.getProperty("port"))).intValue();
        URL url = null;

        try {
            url = new URL("http://" + host + ":" + port + mountpoint + "/");
        } catch (MalformedURLException ex1) {
            return null;
        }

        return url;
    }
}
