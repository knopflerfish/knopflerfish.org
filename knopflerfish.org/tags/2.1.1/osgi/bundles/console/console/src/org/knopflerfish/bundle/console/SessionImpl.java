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

    final static String ALIAS_SAVE = "aliases";

    final BundleContext bc;

    private Dictionary properties = new Hashtable();

    Vector listeners = new Vector();

    Vector cmdline = new Vector();

    String currentGroup = "";

    String prompt = "%> ";

    String name = "UNKNOWN";

    boolean closed = false;

    StreamTokenizer cmd;

    Reader in;

    PrintWriter out;

    Alias aliases;

    ReadThread readT;

    public SessionImpl(BundleContext bcontext, String name, Reader in,
            PrintWriter out, Alias aliases) {
        super(name);
        this.bc = bcontext;
        this.out = out;
        this.readT = new ReadThread(in, this);
        this.in = readT.getReader();
        this.cmd = setupTokenizer(this.in);

        if (aliases != null) {
            this.aliases = aliases;
        } else {
            this.aliases = new Alias();
            this.aliases.setDefault();
        }

        /*
         * AccessController.doPrivileged( new PrivilegedAction() { public Object
         * run() { File as = bc.getDataFile(ALIAS_SAVE); if (as.exists()) { try {
         * aliases.restore(new FileReader(as)); return null; } catch
         * (IOException e) { // NYI! log failure } } aliases.setDefault();
         * return null; } });
         */
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
            for (Enumeration e = cmdline.elements(); e.hasMoreElements();) {
                Command c = (Command) e.nextElement();
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
        return readT.escapeChar;
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
        readT.escapeChar = ch;
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
        return readT.interruptString;
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
        readT.interruptString = str;
    }

    /**
     * Close session
     * 
     */
    public void close() {
        if (closed) {
            return;
        }
        readT.close();
        abortCommand();
        closed = true;
        try {
            readT.join(2000);
        } catch (InterruptedException ignore) {
        }
        if (readT.isAlive()) {
            // TBD log error, readT.stop();
        }
        for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
            SessionListener l = (SessionListener) e.nextElement();
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
    public Dictionary getProperties() {
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
        readT.start();
        while (true) {
            cmdline.removeAllElements();
            Command c = null;
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
                    if (c != null && c.isPiped) {
                        c = new Command(bc, currentGroup, aliases, cmd,
                                ((Pipe) c.out).getReader(), out, this);
                    } else {
                        c = new Command(bc, currentGroup, aliases, cmd, in,
                                out, this);
                    }
                    cmdline.addElement(c);
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
                if (!c.isPiped) {
                    int first = 0;
                    for (int i = 0; i < cmdline.size(); i++) {
                        c = (Command) cmdline.elementAt(i);
                        c.runThreaded();
                        if (c.isPiped) {
                            continue;
                        }
                        if (c.isBackground) {
                            // Need to close input for this command chain
                            Command f = (Command) cmdline.elementAt(first);
                            f.in = null;
                            first = i + 1;
                            continue;
                        }
                        try {
                            for (int j = first; j <= i; j++) {
                                c.thread.join();
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
