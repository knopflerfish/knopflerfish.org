/*
 * Oscar Shell Service
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.ungoverned.osgi.service.shell;

import java.io.PrintStream;

import org.osgi.framework.ServiceReference;

/**
 * This interface defines the Oscar shell service. The shell service
 * is an extensible, user interface neutral shell for controlling and
 * interacting with Oscar. In general, the shell service assumes that
 * it is operating in a command line fashion, i.e., it receives a
 * complete command line, parses it, and executes the corresponding
 * command, but graphical interfaces are also possible.
 * <p>
 * All commands in the shell service are actually implemented as OSGi
 * services; these services implement the <tt>Command</tt> service
 * interface. Any bundle can implement custom commands by creating
 * command services and registering them with the OSGi framework.
**/
public interface ShellService
{
    /**
     * Returns an array of command names available in the shell service.
     * @return an array of available command names or an empty array.
    **/
    public String[] getCommands();

    /**
     * Returns the usage string associated with the specified command name.
     * @param name the name of the target command.
     * @return the usage string of the specified command or null.
    **/
    public String getCommandUsage(String name);

    /**
     * Returns the description associated with the specified command name.
     * @param name the name of the target command.
     * @return the description of the specified command or null.
    **/
    public String getCommandDescription(String name);

    /**
     * Returns the service reference associated with the specified
     * command name.
     * @param name the name of the target command.
     * @return the description of the specified command or null.
    **/
    public ServiceReference getCommandReference(String name);

    /**
     *
     * This method executes the supplied command line using the
     * supplied output and error print stream. The assumption of
     * this method is that a command line will be typed by the user
     * (or perhaps constructed by a GUI) and passed into it for
     * execution. The command line is interpretted in a very
     * simplistic fashion; it takes the leading string of characters
     * terminated by a space character (not including it) and
     * assumes that this leading token is the command name. For an
     * example, consider the following command line:
     * </p>
     * <pre>
     *     update 3 http://www.foo.com/bar.jar
     * </pre>
     * <p>
     * This is interpretted as an <tt>update</tt> command; as a
     * result, the entire command line (include command name) is
     * passed into the <tt>execute()</tt> method of the command
     * service with the name <tt>update</tt> if one exists. If the
     * corresponding command service is not found, then an error
     * message is printed to the error print stream.
     * @param commandLine the command line to execute.
     * @param out the standard output print stream.
     * @param err the standard error print stream.
    **/
    public void executeCommand(
        String commandLine, PrintStream out, PrintStream err)
        throws Exception;
}