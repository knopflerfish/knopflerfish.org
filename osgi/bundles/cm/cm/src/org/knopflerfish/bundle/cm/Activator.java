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

package org.knopflerfish.bundle.cm;

import java.io.File;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * CM bundle activator implementation
 * 
 * @author Per Gustafson
 * @version $Revision: 1.1.1.1 $
 */

public class Activator implements BundleActivator {
    private static final String STORE_DIR_PROP = "com.gatespace.bundle.cm.store";

    private static final String DEFAULT_STORE_DIR = "cm_store";

    static BundleContext bc;

    static LogRef log;

    private ServiceRegistration serviceRegistration;

    static boolean R3_TESTCOMPLIANT = false;

    public void start(BundleContext bc) {
        Activator.bc = bc;

        R3_TESTCOMPLIANT = "true".equals(System.getProperty(
                "org.knopflerfish.osgi.r3.testcompliant", "false")
                .toLowerCase());

        throwIfBundleContextIsNull();
        createLogRef();
        createAndRegisterConfigurationAdminFactory();
    }

    public void stop(BundleContext bc) {
        unregisterConfigurationAdminFactory();
        closeLogRef();
    }

    private void createLogRef() {
        throwIfBundleContextIsNull();
        log = new LogRef(bc);
    }

    private void closeLogRef() {
        if (log != null) {
            log.close();
            log = null;
        }
    }

    private void createAndRegisterConfigurationAdminFactory() {
        throwIfBundleContextIsNull();
        File storeDir = getStoreDir();
        serviceRegistration = bc.registerService(ConfigurationAdmin.class
                .getName(), new ConfigurationAdminFactory(storeDir), null);
    }

    private void unregisterConfigurationAdminFactory() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    private File getStoreDir() {
        throwIfBundleContextIsNull();
        String storeDirName = System.getProperty(STORE_DIR_PROP);
        File storeDir = null;
        if (storeDirName == null || "".equals(storeDirName)) {
            storeDir = bc.getDataFile(DEFAULT_STORE_DIR);
        } else {
            storeDir = new File(storeDirName);
        }
        return storeDir;
    }

    private void throwIfBundleContextIsNull() {
        if (bc == null) {
            throw new NullPointerException("Null BundleContext in Activator");
        }
    }

    static boolean r3TestCompliant() {
        return R3_TESTCOMPLIANT;
    }
}
