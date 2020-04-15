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

package org.knopflerfish.bundle.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.SessionListener;
import org.osgi.framework.BundleContext;

/**
 * Implementation of the service interface ConsoleService.
 *
 * @see org.knopflerfish.service.console.ConsoleService
 * @author Jan Stein
 */
public class SessionImpl extends Thread implements Session {
    private final BundleContext bc;
    private Dictionary<String, Object> properties = new Hashtable<>();
    private Vector<SessionListener> listeners = new Vector<>();
    private final Vector<Command> cmdline = new Vector<>();
    private boolean closed = false;
    private boolean stopped = false;
    private StreamTokenizer cmd;
    private Reader in;
    private PrintWriter out;
    private ReadThread readThread;

    String currentGroup = "";
    String prompt = "%> ";
    Alias aliases;


    public SessionImpl(BundleContext bcontext, String name, Reader in,
            PrintWriter out, Alias aliases) {
        super(name);
        this.bc = bcontext;
        this.out = out;
        readThread = new ReadThread(in, this);
        this.in = readThread.getReader();
        this.cmd = setupTokenizer(this.in);

        if (aliases != null) {
            this.aliases = aliases;
        } else {
            this.aliases = new Alias();
            this.aliases.setDefault();
        }

        Activator.sessions.add(this);
        /*
         * AccessController.doPrivileged( new PrivilegedAction() { public Object
         * run() { File as = bc.getDataFile(ALIAS_SAVE); if (as.exists()) { try {
         * aliases.restore(new FileReader(as)); return null; } catch
         * (IOException e) { // NYI! log failure } } aliases.setDefault();
         * return null; } });
         */
    }

    // Called when the console bundle is stopped.
    void bundleStopped()
    {
        Activator.sessions.remove(this);
        stopped = true;
        this.interrupt();
    }


    //
    // Session implementation
    //

    /**
     * Abort current command in session
     *
     */
    public void abortCommand() {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        synchronized (cmdline) {
            for (Enumeration<Command> e = cmdline.elements(); e.hasMoreElements();) {
                Command c = e.nextElement();
                if (c.thread != null) {
                    c.thread.interrupt();
                }
            }
        }
    }

    /**
     * Get escape character.
     *
     * @return Current escape character
     */
    public char getEscapeChar() {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        return readThread.escapeChar;
    }

    /**
     * Set escape character.
     *
     * @param ch
     *            New escape character
     */
    public void setEscapeChar(char ch) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        readThread.escapeChar = ch;
    }

    /**
     * Get interrupt string.
     *
     * @return Current interrupt string
     */
    public String getInterruptString() {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        return readThread.interruptString;
    }

    /**
     * Set interrupt string.
     *
     * @param str
     *            New interrupt string
     */
    public void setInterruptString(String str) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        readThread.interruptString = str;
    }

    /**
     * Close session
     *
     */
    public void close() {
        if (closed) {
            return;
        }
        readThread.close();
        abortCommand();
        closed = true;
        for (Enumeration<SessionListener> e = listeners.elements(); e.hasMoreElements();) {
            SessionListener l = e.nextElement();
            l.sessionEnd(this);
        }
    }

    /**
     * Add session event listener.
     *
     * @param l
     *            Session listener
     */
    public void addSessionListener(SessionListener l) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        listeners.addElement(l);
    }

    /**
     * Remove session event listener.
     *
     * @param l
     *            Session listener
     */
    public void removeSessionListener(SessionListener l) {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }
        listeners.removeElement(l);
    }

    /**
     * Returns the property information tied to this session.
     *
     * @return Property Dictionary.
     */
    public Dictionary<String, Object> getProperties() {
        return properties;
    }

    //
    // Main Thread
    //

    /**
     * Run command dispatcher thread
     *
     */
    public void run() {
        readThread.start();
        while (!stopped) {
            cmdline.removeAllElements();
            Command command = null;
            if (prompt != null) {
                int i = prompt.indexOf('%');
                if (i != -1) {
                    out.print(prompt.substring(0, i) + currentGroup
                            + prompt.substring(i + 1));
                } else {
                    out.print(prompt);
                }
                out.flush();
            }
            try {
                while (cmd.nextToken() != StreamTokenizer.TT_EOF
                        && cmd.ttype != StreamTokenizer.TT_EOL) {
                    cmd.pushBack();
                    if (command != null && command.isPiped) {
                        command = new Command(bc, currentGroup, aliases, cmd,
                                ((Pipe) command.out).getReader(), out, this);
                    } else {
                        command = new Command(bc, currentGroup, aliases, cmd, in,
                                out, this);
                    }
                    cmdline.addElement(command);
                }
            } catch (IOException e) {
                out.println("ERROR: " + e.getMessage());
                try {
                    while (cmd.ttype != StreamTokenizer.TT_EOF
                            && cmd.ttype != StreamTokenizer.TT_EOL) {
                        cmd.nextToken();
                    }
                } catch (IOException ignore) {
                    break;
                }
                cmdline.removeAllElements();
            }
            if (cmd.ttype == StreamTokenizer.TT_EOF) {
                break;
            }
            if (cmdline.size() > 0) {
                if (command == null || !command.isPiped) {
                    int first = 0;
                    for (int i = 0; i < cmdline.size(); i++) {
                        command = cmdline.elementAt(i);
                        command.runThreaded();
                        if (command.isPiped) {
                            continue;
                        }
                        if (command.isBackground) {
                            // Need to close input for this command chain
                            Command f = cmdline.elementAt(first);
                            f.in = null;
                            first = i + 1;
                            continue;
                        }
                        try {
                            for (int j = first; j <= i; j++) {
                                command.thread.join();
                            }
                        } catch (InterruptedException e) {
                            // TODO: cleanup
                        }
                        first = i + 1;
                    }
                } else {
                    out.println("ERROR: Ends with pipe without command!");
                }
            }
        }
        close();
    }

    //
    // Private utility methods
    //

    /**
     * Setup a tokenizer with following properties:
     *
     * @param in
     *            Input reader
     * @return Configured StreamTokenizer
     */
    static StreamTokenizer setupTokenizer(Reader in) {
        StreamTokenizer st = new StreamTokenizer(in);
        st.resetSyntax();
        st.whitespaceChars(0, ' ');
        // '!' token
        st.quoteChar('"');
        st.commentChar('#');
        // '$' token
        st.wordChars('%', '%');
        // '&' token
        st.quoteChar('\'');
        st.wordChars('(', ':');
        // '<' token
        st.wordChars('=', '=');
        // '>' token
        st.wordChars('?', '{');
        // '|' token
        st.wordChars('}', 255);
        st.eolIsSignificant(true);
        return st;
    }

}
