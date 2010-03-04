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

import java.io.PrintWriter;
import java.io.Reader;

/**
 * Interface for a command group service. This service will be used by the
 * console service to present the commands to the user.
 * <p>
 * The service object must then be registered under the class name <br>
 * <code>org.knopflerfish.service.console.CommandGroup</code> with the
 * property "groupName" set to the command group name.
 * 
 * @see CommandGroupAdapter
 * @see ConsoleService
 * @author Gatespace AB
 * @version $Revision: 1.1.1.1 $
 */

public interface CommandGroup {

    /**
     * Property key for group name to use when registering a CommandGroup.
     */
    public final static String GROUP_NAME = "groupName";

    /**
     * Returns a string with the command group name.
     * 
     * @return Command group name.
     */
    public String getGroupName();

    /**
     * Returns a string containing a short help text for the command group.
     * 
     * @return Short command group help.
     */
    public String getShortHelp();

    /**
     * Returns a string containing a long help text for the command group.
     * 
     * @return Long command group help.
     */
    public String getLongHelp();

    /**
     * Executes a command in the command group. The command and its arguments
     * are passed in the args parameter.
     * 
     * @param args
     *            argument list
     * @param out
     *            output write for command to print result to
     * @param in
     *            input reader for command to read interactively
     * @param session
     *            a handle to command session or null if single command
     * @return Status from execution, 0 means okey
     */
    public int execute(String[] args, Reader in, PrintWriter out,
            Session session);

}
