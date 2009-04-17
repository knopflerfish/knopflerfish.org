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

package org.knopflerfish.service.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * Console service for OSG platforms. This service is meant for building an
 * extensible debug console for OSG systems. The ConsoleService has two methods
 * one for running a single command and one for running a session of commands.
 * The console service can execute any command from all the exported
 * CommandGroup services in the platform.
 * 
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */
public interface ConsoleService {

    /**
     * Start a command session. Returns a Session object that can be used to
     * control the session.
     * 
     * @param name
     *            name of session
     * @param in
     *            input to session
     * @param out
     *            output from session
     * @return Session object
     * @exception IOException
     *                if we fail to use input or output
     */
    public Session runSession(String name, Reader in, PrintWriter out)
            throws IOException;

    /**
     * Run a command string. Here we can only execute a single command. There is
     * no possiblity to supply any input data other than command line options
     * and the output from the command is returned as a string. The return value
     * from the command is discarded.
     * 
     * @param command
     *            command and arguments as string
     * @return Resulting output of command
     */
    public String runCommand(String command);

    /**
     * Set alias value for a shortcut key.
     */
    public String[] setAlias(String key, String[] val);
}
