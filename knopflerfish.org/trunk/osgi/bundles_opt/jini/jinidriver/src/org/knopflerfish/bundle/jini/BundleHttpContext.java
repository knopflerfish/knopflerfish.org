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

import org.osgi.service.http.HttpContext;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

import java.net.URL;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * DOCUMENT ME!
 *
 * @author Nico Goeminne
 */
class BundleHttpContext implements HttpContext {
    private PackageAdmin packageAdmin = null;
    private ArrayList exportedPackages = new ArrayList();

    /**
     * Creates a new BundleHttpContext object.
     */
    public BundleHttpContext() {
        ServiceReference ref = Activator.bc.getServiceReference(
                "org.osgi.service.packageadmin.PackageAdmin");
        packageAdmin = (PackageAdmin) Activator.bc.getService(ref);
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getMimeType(String name) {
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param req DOCUMENT ME!
     * @param resp DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean handleSecurity(HttpServletRequest req,
        HttpServletResponse resp) {
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public URL getResource(String name) {
        name = name.substring(1);

        String temp = name.substring(0, name.lastIndexOf("/"));
        String packageName = temp.replace('/', '.');

        for (int i = 0; i < exportedPackages.size(); i++) {
            Bundle b = ((Bundle) exportedPackages.get(i));
            ExportedPackage[] exp = packageAdmin.getExportedPackages(b);

            if (exp != null) {
                for (int j = 0; j < exp.length; j++) {
                    if (exp[j].getName().equals(packageName)) {
                        return b.getResource(name);
                    }
                }
            }
        }

        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param b DOCUMENT ME!
     */
    void ensureCodebaseFor(Bundle b) {
        exportedPackages.add(b);
    }

    /**
     * DOCUMENT ME!
     *
     * @param b DOCUMENT ME!
     */
    void removeCodebaseFor(Bundle b) {
        exportedPackages.remove(b);
    }
}
