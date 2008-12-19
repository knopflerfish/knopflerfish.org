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

package org.knopflerfish.bundle.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.knopflerfish.service.console.ConsoleService;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleContext;

/**
 * Implementation of the service interface ConsoleService.
 * 
 * @see org.knopflerfish.service.console.ConsoleService
 * @author Jan Stein
 */
public class ConsoleServiceImpl implements ConsoleService {

    final BundleContext bc;

    Alias aliases;

    public ConsoleServiceImpl(BundleContext bc) {
        this.bc = bc;

        aliases = new Alias();
        aliases.setDefault();
    }

    public String[] setAlias(final String key, final String[] val) {
        String oldVal[] = (String[]) AccessController
                .doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return aliases.put(key, val.clone());
                    }
                });

        return oldVal;
    }

    /**
     * Start a command session
     * 
     * @param command
     *            Command string to match against
     * @return Session object
     */
    public Session runSession(String name, Reader in, PrintWriter out) {
        SessionImpl s = new SessionImpl(bc, name, in, out, aliases);
        s.start();
        return s;
    }

    /**
     * Run a single command
     * 
     * @param commands
     *            String with command to run
     * @return Result of commands
     */
    public String runCommand(String command) {

        Reader in = new StringReader(command);
        StringWriter buf = new StringWriter();
        PrintWriter out = new PrintWriter(buf);

        try {
            (new Command(bc, "", null, SessionImpl.setupTokenizer(in), in, out,
                    null)).run();
            out.flush();
            return buf.toString();
        } catch (IOException e) {
            return "Command failed; " + e.getMessage();
        }
    }
}
