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

package org.knopflerfish.bundle.device;

import java.io.InputStream;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Device;
import org.osgi.service.device.DriverLocator;
import org.osgi.service.device.Match;

public class MatchImpl implements Match {
    static final int UNKNOWN = -1;

    private Activator act;

    private ServiceReference dev;

    private String id;

    private DriverRef ref;

    private int match;

    private Vector /* DriverLocator */dls;

    private Bundle b;

    MatchImpl(Activator act, ServiceReference dev, String id) {
        this.act = act;
        this.dev = dev;
        this.id = id;
        match = act.getCachedMatch(id, dev);
    }

    MatchImpl(Activator act, ServiceReference dev, DriverRef dr) {
        this.act = act;
        this.dev = dev;
        id = dr.id;
        ref = dr;
        match = act.getCachedMatch(id, dev);
    }

    boolean equals(String id) {
        return this.id.equals(id);
    }

    boolean connect(DriverRef dr) {
        if (id.equals(dr.id) && (ref == dr || ref == null)) {
            ref = dr;
            return true;
        }
        return false;
    }

    int compare(MatchImpl m) {
        int cmp = getMatchValue() - m.getMatchValue();
        if (cmp == 0)
            cmp = ref.ranking - m.ref.ranking;
        if (cmp == 0)
            cmp = ref.servid < m.ref.servid ? 1 : -1;
        return cmp;
    }

    void addDriverLocator(DriverLocator dl) {
        if (dls == null)
            dls = new Vector();
        dls.addElement(dl);
    }

    Bundle getBundle() {
        return b;
    }

    private boolean load() {
        String name = Activator.DYNAMIC_DRIVER_TAG + id;
        Bundle[] ba = act.bc.getBundles();
        if (ba != null) {
            for (int i = 0; i < ba.length; i++) {
                if (name.equals(ba[i].getLocation())) {
                    return false;
                }
            }
        }
        if (dls != null) {
            for (int i = 0; i < dls.size(); i++) {
                if (load1(name, (DriverLocator) dls.elementAt(i)))
                    return true;
            }
        } else {
            DriverLocator[] locs = act.locators;
            for (int i = 0; i < locs.length; i++) {
                if (load1(name, locs[i]))
                    return true;
            }
        }
        return false;
    }

    private boolean load1(String name, DriverLocator dl) {
        InputStream is = null;
        try {
            is = dl.loadDriver(id);
            b = act.installBundle(name, is);
            if (b != null)
                return true;
        } catch (Exception e) {
        } finally {
            try {
                is.close();
            } catch (Exception e1) {
            }
        }
        return false;
    }

    String attach() throws Exception {
        if (match > Device.MATCH_NONE && ref == null && b == null && load()) {
            return null;
        }
        match = Device.MATCH_NONE;
        return ref.drv.attach(dev);
    }

    public ServiceReference getDriver() {
        return ref.sr;
    }

    public int getMatchValue() {
        if (match == UNKNOWN) {
            if (ref != null || b != null) {
                try {
                    match = ref.drv.match(dev);
                    act.putCachedMatch(id, dev, match);
                } catch (Exception e) {
                    match = Device.MATCH_NONE;
                }
            } else if (!load()) {
                match = Device.MATCH_NONE;
            }
        }
        return match;
    }
}
