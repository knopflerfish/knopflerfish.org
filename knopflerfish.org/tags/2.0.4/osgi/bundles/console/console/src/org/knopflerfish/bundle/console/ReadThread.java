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
import java.io.InterruptedIOException;
import java.io.Reader;

import org.knopflerfish.service.console.Session;

/**
 * Implementation of the service interface ConsoleService.
 * 
 * @see org.knopflerfish.service.console.ConsoleService
 * @author Jan Stein
 * @version $Revision: 1.1.1.1 $
 */
public class ReadThread extends Thread {

    Reader in;

    Session session;

    Pipe pipe;

    boolean open = true;

    char escapeChar = '\026'; // default CTRL-V

    String interruptString = "\003"; // default CTRL-C

    public ReadThread(Reader in, Session session) {
        super("Reader thread " + session.getName());
        this.in = in;
        this.session = session;
        pipe = new Pipe();
    }

    /**
     * 
     * 
     */
    public Reader getReader() {
        return pipe.getReader();
    }

    //
    // Thread main
    //

    /**
     * Run
     * 
     * @param commands
     *            String with command to run
     * @return Result of commands
     */
    public void run() {
        boolean escape = false;
        int ipos = 0;
        int c;
        while (open) {
            try {
                c = in.read();
            } catch (InterruptedIOException ignore) {
                // If we are interrupted, we check open flag and try to read
                // again.
                continue;
            } catch (IOException ignore) {
                // Treat every IO problem as EOF.
                c = -1;
            }
            if (c == -1) {
                close();
            } else if (!escape && c == escapeChar) {
                escape = true;
                ipos = 0;
            } else if (!escape && c == interruptString.charAt(ipos)) {
                if (++ipos == interruptString.length()) {
                    ipos = 0;
                    session.abortCommand();
                }
            } else {
                if (ipos > 0) {
                    pipe.write(interruptString, 0, ipos);
                    ipos = 0;
                }
                pipe.write(c);
                escape = false;
            }
        }
        pipe.close();
    }

    /**
     * Close this thread
     */
    public void close() {
        open = false;
        this.interrupt();
    }
}
