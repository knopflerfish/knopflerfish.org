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

package org.knopflerfish.bundle.basicdriverlocator;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
import org.osgi.service.device.DriverLocator;

/**
 * Basic implementation of a DriverLocator.
 *
 * Uses a text-based driver database in Properties format. The driver is named
 * <code>driverDB.props</code> and should reside in the bundle jar file.
 *
 * <p>
 * Database file format:
 *
 * <pre>
 *       count=&lt;number of drivers&gt;
 *       &lt;number&gt;.id=&lt;driver id&gt;
 *       &lt;number&gt;.category=&lt;driver category&gt;
 *       &lt;number&gt;.product=&lt;driver product&gt;
 *       &lt;number&gt;.vendor=&lt;driver vendor&gt;
 *       &lt;number&gt;.url=&lt;driver url&gt;
 *       &lt;number&gt;.desc=&lt;driver description&gt;
 * </pre>
 *
 *
 */
public class BasicDriverLocator implements DriverLocator {

    /** Context service is running in */
    BundleContext bc = null;

    String dbResourceName = "/data/driverDB.props";

    // Utility class for logging
    LogRef log;

    String jarbase = "";

    BasicDriverLocator(BundleContext bc) {
        this.bc = bc;

        // Try to get log service and handle it's life cycle
        log = new LogRef(bc);

        String jars = bc.getProperty("org.knopflerfish.gosg.jars");
        jarbase = (null==jars || jars.length()==0) ? "file:" : jars;
    }

    /**
     * Implements DriverLocator.
     *
     * Loads driver DB, then calls match()
     */
    public String[] findDrivers(Dictionary dict) {
        Hashtable drivers = loadDriverDB();

        return match(drivers, dict);
    }

    /**
     * Implements DriverLocator.
     *
     * Load a driver bundle using URL info.
     *
     * @return Stream to driver bundle, null if no driver is found.
     */
    public InputStream loadDriver(String id) throws IOException {
        Hashtable drivers = loadDriverDB();

        DriverInfo di = (DriverInfo) drivers.get(id);
        InputStream stream = null;

        if (di == null) {
            log.warn("No id" + id);
            throw new IOException("No driver id '" + id + "'");
        }
        try {
            String strURL = di.url;
            if (strURL.indexOf(":") == -1) {
                strURL = jarbase + strURL;
            }

            URL url = new URL(strURL);

            stream = url.openStream();
        } catch (MalformedURLException e) {
            log.error("bad URL for in " + di, e);
            throw new IOException("Bad driver URL " + e);
        } catch (IOException e) {
            log.error("can't connect to URL in " + di, e);

            // rethrow exception
            throw e;
        }
        return stream;
    }

    /**
     * Load driver database.
     *
     * @return Hashtable String (driver id) -> DriverInfo
     */
    Hashtable loadDriverDB() {
        Hashtable d = new Hashtable();
        InputStream instream = null;

        try {
            String dbURL = System
                    .getProperty("org.osgi.service.basicdriverlocator.dburl");

            // If property exists, try to open URL, otherwise use internal
            // props.
            if (dbURL != null && !dbURL.equals("")) {
                if (dbURL.indexOf(":") == -1) {
                    dbURL = jarbase + dbURL;
                }
                URL url = new URL(dbURL);
                log.info("read external props file: " + dbURL);
                instream = url.openStream();
            } else {
                log.info("read internal props file: " + dbResourceName);
                instream = getClass().getResourceAsStream(dbResourceName);
            }

            Properties db = new Properties();

            db.load(instream);

            int nCount = Integer.parseInt((String) db.get("count"));

            for (int i = 0; i < nCount; i++) {
                try {
                    DriverInfo di = new DriverInfo(db, i);
                    d.put(di.id, di);
                } catch (Exception e) {
                    log.error("Failed to initialize driver entry " + i, e);
                }
            }

        } catch (Exception e) {
            log.error("Can't load driverDB: ", e);
            e.printStackTrace();
        } finally {
            try {
                if (instream != null)
                    instream.close();
            } catch (IOException e2) {
                // silently ignore
            }
        }

        return d;
    }

    /**
     * Check all Drivers in drivers for match agains props.
     *
     * @param drivers
     *            String (driver ID) -> DriverInfo
     * @param props
     *            Device properties to match
     * @return String array od driver IDs
     */
    String[] match(Hashtable drivers, Dictionary dict) {
        Vector r = new Vector();

        for (Enumeration e = drivers.elements(); e.hasMoreElements();) {
            DriverInfo di = (DriverInfo) e.nextElement();

            if (di.match(dict)) {
                r.addElement(di.id);
            }
        }

        // Create string array, since API says so
        String[] rl = new String[r.size()];

        r.copyInto(rl);

        return rl;
    }
}

/**
 * Utility class for holding driver database
 */
class DriverInfo {
    String url;

    String id;

    String category;

    String vendor;

    String product;

    String desc;

    /**
     * Create from Properties.
     *
     * Values of the form <code>number</code>.key are get() from props to
     * build the DriverInfo.
     *
     * @param p
     *            Properties containing driver info
     * @param number
     *            count number inte named properties
     */
    DriverInfo(Properties p, int number) {
        id = (String) p.get("" + number + ".id");
        url = (String) p.get("" + number + ".url");
        desc = (String) p.get("" + number + ".desc");
        vendor = (String) p.get("" + number + ".vendor");
        product = (String) p.get("" + number + ".product");
        category = (String) p.get("" + number + ".category");
    }

    /**
     * Matching method used by locator.
     *
     * @return true if DriverInfo matches props, false otherwise
     */
    boolean match(Dictionary props) {
        boolean b = cmp(category, (String) props.get("category"))
                && cmp(product, (String) props.get("product"))
                && cmp(vendor, (String) props.get("vendor"));

        return b;
    }

    /**
     * Compare method which allows empty strings and simple wildcards.
     *
     * null, "" and "*" are match-all wildcards.
     *
     * @param s1
     *            string to compare to s2
     * @param s2
     *            string to compare to s1
     * @return true id s1 compares nicely to s2
     */
    boolean cmp(String s1, String s2) {
        return s1 == null || s2 == null || s1.equals("") || s2.equals("")
                || s1.equals("*") || s2.equals("*") || s1.equalsIgnoreCase(s2);
    }

    /**
     * Convert to string.
     *
     * @return Human-readable string representation
     */
    public String toString() {
        return "id=" + id + ", url=" + url + ", desc=" + desc + ", product="
                + product + ", vendor=" + vendor;
    }

}
