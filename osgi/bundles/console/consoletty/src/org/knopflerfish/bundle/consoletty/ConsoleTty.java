/*
 * Copyright (c) 2003-2020, KNOPFLERFISH project
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

package org.knopflerfish.bundle.consoletty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Hashtable;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Bundle activator implementation.
 *
 * @author Jan Stein
 * @version $Revision: 1.1.1.1 $
 */
public class ConsoleTty
  implements BundleActivator, ManagedService, ServiceTrackerCustomizer<ConsoleService, ConsoleService>
{
    private static final String logServiceName = LogService.class.getName();
    private static final String consoleServiceName = ConsoleService.class.getName();
    final static private String NONBLOCKING = "nonblocking";

    private boolean nonblocking = false;
    private ServiceTracker<ConsoleService, ConsoleService> consoleTracker;
    private Session consoleSession = null;
    private BundleContext bc;
    private Reader reader;
    private PrintWriter writer;

    /*---------------------------------------------------------------------------*
     *			  BundleActivator implementation
     *---------------------------------------------------------------------------*/

    // ==================== start ====================
    /**
     * Called by the framework when this bundle is started.
     *
     * @param bc Bundle context.
     */
    @Override
    public void start(BundleContext bc) {
        this.bc = bc;

        log(LogService.LOG_INFO, "Starting");

        // Get config
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, getClass().getName());
        bc.registerService(ManagedService.class.getName(), this, props);

        PrintStream out = null;
        try {
            ServiceReference<?>[] srl = bc.getServiceReferences(PrintStream.class
                    .getName(), "(service.pid=java.lang.System.out)");
            if (srl != null && srl.length == 1) {
                out = (PrintStream) bc.getService(srl[0]);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        if (out == null) {
            out = System.out;
        }

        reader = new InputStreamReader(new SystemIn(bc));
        writer = new PrintWriter(out);

        consoleTracker = new ServiceTracker<>(bc, consoleServiceName, this);
        consoleTracker.open();
    }

    // ==================== stop ====================
    /**
     * Called by the framework when this bundle is stopped.
     *
     * @param bc Bundle context.
     */
    @Override
    public synchronized void stop(BundleContext bc) {
        log(LogService.LOG_INFO, "Stopping");
        consoleTracker.close();
        try {
            if (consoleSession != null) {
                consoleSession.close();
            }
        } catch (Exception e) {
            log(LogService.LOG_ERROR, "Failed to close session", e);
        }
    }

    /*---------------------------------------------------------------------------*
     *			  ManagedService implementation
     *---------------------------------------------------------------------------*/

    // ==================== updated ====================
    /**
     * Called by CM when it got this bundles configuration.
     *
     * @param config contains the new configuration properties.
     */
    @Override
    public synchronized void updated(Dictionary<String, ?> config)
            throws IllegalArgumentException {
        if (config != null) {
            Boolean b = (Boolean) config.get(NONBLOCKING);
            if (b != null) {
                nonblocking = b;
            }
        } else {
            nonblocking = false;
        }
    }

    /*---------------------------------------------------------------------------*
     *			  ServiceTrackerCustomizer implementation
     *---------------------------------------------------------------------------*/

    @Override
    public ConsoleService addingService(ServiceReference<ConsoleService> reference) {
        ConsoleService console = bc.getService(reference);
        try {
            consoleSession = console.runSession("console tty", reader, writer);
        } catch (IOException ioe) {
            log(LogService.LOG_ERROR,
                "Failed to start console session, can not continue");
        }
        return console;
    }

    @Override
    public void modifiedService(ServiceReference<ConsoleService> reference, ConsoleService service) {
    }

    @Override
    public void removedService(ServiceReference<ConsoleService> reference, ConsoleService service) {
        if (consoleSession != null) {
            consoleSession.close();
            consoleSession = null;
        }
    }

    /*---------------------------------------------------------------------------*
     *			  Protected utility methods
     *---------------------------------------------------------------------------*/

    // ==================== log ====================
    /**
     * Utility method used for logging.
     *
     * @param level Log level
     * @param msg   Log message
     */
    public void log(int level, String msg) {
        log(level, msg, null);
    }

    public void log(int level, String msg, Exception e) {
        ServiceReference<?> srLog = bc.getServiceReference(logServiceName);
        if (srLog != null) {
            LogService sLog = (LogService) bc.getService(srLog);
            if (sLog != null) {
                if (e == null) {
                    sLog.log(level, msg);
                } else {
                    sLog.log(level, msg, e);
                }
            }
            bc.ungetService(srLog);
        }
    }

    /*
     * ------------------------------------------------------------ Help class
     * for VMs that when blocking in read() om System.in block other threads
     * too. ------------------------------------------------------------
     */
    class SystemIn extends InputStream {

        private InputStream in;

        SystemIn(BundleContext bc) {
            try {
                ServiceReference<?>[] srl = bc.getServiceReferences(
                        InputStream.class.getName(),
                        "(service.pid=java.lang.System.in)");
                if (srl != null && srl.length == 1) {
                    in = (InputStream) bc.getService(srl[0]);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            if (in == null) {
                in = System.in;
            }
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            if (read(b) == 1) {
                return b[0];
            }
            return -1;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            if (nonblocking) {
                int nap = 50;
                while (in.available() == 0) {
                    try {
                        Thread.sleep(nap);
                    } catch (InterruptedException ignored) {
                    }
                    nap = 200;
                }
            }
            return in.read(buf, off, len);
        }
    }

}
