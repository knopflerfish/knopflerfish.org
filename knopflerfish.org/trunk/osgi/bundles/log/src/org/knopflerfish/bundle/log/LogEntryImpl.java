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

package org.knopflerfish.bundle.log;

import java.text.SimpleDateFormat;

import org.knopflerfish.service.log.LogUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;

/**
 * * This class implements the LogEntry interface defined by OSGi. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.1.1.1 $
 */
public final class LogEntryImpl implements LogEntry {

    // Date formater used in toString().
    private static SimpleDateFormat simpleDateFormat = null;

    // Log entry data.
    private Bundle bundle;

    private ServiceReference sr;

    private int level;

    private String msg;

    private Throwable e;

    private long millis;

    public LogEntryImpl(Bundle bc, int l, String m) {
        this(bc, null, l, m, null);
    }

    public LogEntryImpl(Bundle bc, int l, String m, Throwable e) {
        this(bc, null, l, m, e);
    }

    public LogEntryImpl(Bundle bc, ServiceReference sd, int l, String m) {
        this(bc, sd, l, m, null);
    }

    public LogEntryImpl(Bundle bc, ServiceReference sd, int l, String m,
            Throwable e) {
        this.bundle = bc;
        this.sr = sd;
        this.level = l;
        this.msg = m;
        this.e = e;
        this.millis = System.currentTimeMillis();
    }

    /**
     * * Returns a string representing this log entry. * The format is: *
     * 
     * <pre>
     *   * level    YYYYMMDD HH:MM:ss bid#NR       - [Service] - Message (Exception)
     *   *
     * </pre>
     */
    public String toString() {

        StringBuffer sb = new StringBuffer(100);
        sb.append(LogUtil.fromLevel(level, 8));
        sb.append(" ");
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        }
        sb.append(simpleDateFormat.format(new Long(millis)));
        sb.append(" ");
        sb.append("bid#");
        if (bundle != null) {
            sb.append(bundle.getBundleId());
        }
        // Ensure that the service/message part start in pos 40
        if (sb.length() < 40) {
            sb.append("          ");
            sb.setLength(40);
        }
        sb.append("- ");
        if (sr != null) {
            sb.append("[");
            sb.append(sr.getProperty(Constants.SERVICE_ID).toString());
            sb.append(";");
            String[] clazzes = (String[]) sr.getProperty(Constants.OBJECTCLASS);
            for (int i = 0; i < clazzes.length; i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(clazzes[i]);
            }
            sb.append("] ");
        }
        sb.append(msg);
        if (e != null) {
            sb.append(" (");
            sb.append(e.toString());
            sb.append(")");
        }

        return sb.toString();
    }

    public Bundle getBundle() {
        return bundle;
    }

    public ServiceReference getServiceReference() {
        return sr;
    }

    public int getLevel() {
        return level;
    }

    public String getMessage() {
        return msg;
    }

    public Throwable getException() {
        return e;
    }

    public long getTime() {
        return millis;
    }
}
