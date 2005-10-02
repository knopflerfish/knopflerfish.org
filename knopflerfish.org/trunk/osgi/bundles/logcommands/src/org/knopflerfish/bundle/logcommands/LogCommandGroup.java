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

package org.knopflerfish.bundle.logcommands;

import java.io.PrintWriter;
import java.io.Reader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

// ******************** LogCommandGroup ********************
/**
 * * Interface for commands to be handled by the console. * *
 * 
 * @author Jan Stein *
 * @version $Revision: 1.1.1.1 $
 */

public class LogCommandGroup extends CommandGroupAdapter {

    BundleContext bc;

    private final static String LOGREADER = "org.osgi.service.log.LogReaderService";

    LogCommandGroup(BundleContext bc) {
        super("log", "Log commands");
        this.bc = bc;
    }

    //
    // Show command
    //

    public final static String USAGE_SHOW = "[-f] [-h #hours#] [-l #level#] [-n #count#] [-r] [-s] [<bundle>] ...";

    public final static String[] HELP_SHOW = new String[] {
            "Show log bundle entries",
            "If no parameters are given show all entries",
            "-f         Show framework events that aren't connected to a bundle",
            "-h #hours# Show only entries entered in the #hours# last hours",
            "-l #level# Show only entries with minimum level error,warning,info or debug",
            "-n #count# Show the #count# latest entries",
            "-r         Show entries in reversed order",
            "-s         Show stacktrace for exceptions",
            "<bundle>   Name or id of bundle" };

    public int cmdShow(final Dictionary opts, final Reader in,
            final PrintWriter out, final Session session) {
        Integer res = (Integer) AccessController
                .doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        ServiceReference sr = bc.getServiceReference(LOGREADER);
                        if (sr == null) {
                            out.println("Unable to get a LogReaderService");
                            return new Integer(1);
                        }
                        LogReaderService lr = (LogReaderService) bc
                                .getService(sr);
                        if (lr == null) {
                            out.println("Unable to get a LogReaderService");
                            return new Integer(1);
                        }

                        ArrayList bv = null;
                        String[] selection = (String[]) opts.get("bundle");
                        if (selection != null) {
                            Bundle[] b = bc.getBundles();
                            Util.selectBundles(b, selection);
                            bv = new ArrayList();
                            for (int i = 0; i < b.length; i++) {
                                if (b[i] != null) {
                                    bv.add(b[i]);
                                }
                            }
                        }

                        boolean fflag = opts.get("-f") != null;

                        String lflag = (String) opts.get("-l");
                        int level;
                        if (lflag != null) {
                            if (lflag.equalsIgnoreCase("error")) {
                                level = LogService.LOG_ERROR;
                            } else if (lflag.equalsIgnoreCase("warning")) {
                                level = LogService.LOG_WARNING;
                            } else if (lflag.equalsIgnoreCase("info")) {
                                level = LogService.LOG_INFO;
                            } else if (lflag.equalsIgnoreCase("debug")) {
                                level = LogService.LOG_DEBUG;
                            } else {
                                out.println("Unknown level: " + lflag);
                                return new Integer(1);
                            }
                        } else {
                            level = LogService.LOG_DEBUG;
                        }

                        String hflag = (String) opts.get("-h");
                        long startTime = 0;
                        if (hflag != null) {
                            try {
                                startTime = System.currentTimeMillis()
                                        - (long) (60 * 60 * 1000 * (new Double(
                                                hflag)).doubleValue());
                            } catch (NumberFormatException e) {
                                out
                                        .println("Illegal number of hours: "
                                                + hflag);
                                return new Integer(1);
                            }
                        }

                        String nflag = (String) opts.get("-n");
                        int count = Integer.MAX_VALUE;
                        if (nflag != null) {
                            try {
                                count = Integer.parseInt(nflag);
                            } catch (NumberFormatException e) {
                                out
                                        .println("Illegal number as count: "
                                                + nflag);
                                return new Integer(1);
                            }
                        }

                        Enumeration e = lr.getLog();
                        Vector lv = new Vector();
                        boolean rflag = opts.get("-r") == null;
                        while (e.hasMoreElements()) {
                            LogEntry le = (LogEntry) e.nextElement();
                            Bundle b = le.getBundle();
                            if (b == null && fflag || bv == null && !fflag
                                    || bv != null && bv.contains(b)) {
                                if (count-- <= 0) {
                                    break;
                                }
                                if (le.getLevel() > level) {
                                    continue;
                                }
                                if (le.getTime() < startTime) {
                                    break;
                                }
                                if (rflag) {
                                    lv.insertElementAt(le, 0);
                                } else {
                                    lv.addElement(le);
                                }
                            }
                        }

                        StringBuffer sb = new StringBuffer();
                        SimpleDateFormat tf = new SimpleDateFormat(
                                "MMM dd HH:mm:ss ");

                        for (e = lv.elements(); e.hasMoreElements();) {
                            LogEntry le = (LogEntry) e.nextElement();
                            sb.setLength(0);
                            sb.append(tf.format(new Date(le.getTime())));
                            pad(sb, 16);
                            switch (le.getLevel()) {
                            case LogService.LOG_INFO:
                                sb.append("INFO");
                                break;
                            case LogService.LOG_DEBUG:
                                sb.append("DEBUG");
                                break;
                            case LogService.LOG_WARNING:
                                sb.append("WARNING");
                                break;
                            case LogService.LOG_ERROR:
                                sb.append("ERROR");
                                break;
                            default:
                                sb.append("UNKNOWN");
                                break;
                            }
                            pad(sb, 23);
                            Bundle b = le.getBundle();
                            if (b != null) {
                                sb.append(" #" + b.getBundleId());
                                pad(sb, 28);
                                sb.append(Util.shortName(b));
                            } else {
                                sb.append(" FRAMEWORK");
                            }
                            pad(sb, 42);
                            sb.append(" - ");
                            sb.append(le.getMessage());
                            if (le.getServiceReference() != null) {
                                sb.append(", Service: "
                                        + Util.showServiceClasses(le
                                                .getServiceReference()));
                            }
                            out.println(sb.toString());
                            if (le.getException() != null
                                    && opts.get("-s") != null) {
                                le.getException().printStackTrace(out);
                            }
                        }
                        bc.ungetService(sr);
                        return new Integer(0);
                    }
                });
        return res.intValue();
    }

    void pad(StringBuffer sb, int n) {
        while (sb.length() < n)
            sb.append(" ");
    }
}
