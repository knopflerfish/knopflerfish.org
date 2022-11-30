/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;

import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Class for command parsing and execution.
 * 
 * @author Jan Stein
 */
public class Command implements ServiceListener, Runnable {

    private static SessionCommandGroup sessionCommandGroup;

    private final BundleContext bc;
    private String[] args;
    private Alias aliases;
    private String groupName;
    private Session session;
    private ServiceReference<CommandGroup> commandGroupRef;

    Reader in;
    Writer out;
    Thread thread = null;
    boolean isPiped = false;
    boolean isBackground = false;

    public Command(BundleContext bc, String group, Alias aliases,
            final StreamTokenizer cmd, Reader in, PrintWriter out,
            Session session) throws IOException {
        this.bc = bc;
        this.in = in;
        this.out = out;
        this.aliases = aliases;
        this.session = session;
        if (sessionCommandGroup == null) {
            sessionCommandGroup = new SessionCommandGroup(bc);
        }
        groupName = group;
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                parseCommand(cmd);
                return null;
            });
        } catch (PrivilegedActionException e) {
            Exception ee = e.getException();
            // Android don't supply nested exception
            if (ee != null) {
                throw (IOException) ee;
            } else {
                throw new IOException("Failed to parse command");
            }
        }
    }

    void parseCommand(StreamTokenizer in) throws IOException {
        if (in.nextToken() != StreamTokenizer.TT_WORD) {
            throw new IOException("Unexpected token: " + in.ttype);
        }
        String word;
        String[] aliasBuf = null;
        int aliasPos;
        if (aliases != null
                && (aliasBuf = aliases.get(in.sval)) != null) {
            word = aliasBuf[0];
            aliasPos = 1;
        } else {
            word = in.sval;
            aliasPos = 0;
        }
        if ("".equals(groupName) || word.startsWith("/")) {
            if (word.startsWith("/"))
                word = word.substring(1);
            commandGroupRef = matchCommandGroup(bc, word);
            word = null;
        } else {
            if (SessionCommandGroup.NAME.equals(groupName)) {
                commandGroupRef = null;
            } else {
                try {
                    Collection<ServiceReference<CommandGroup>> refs = bc.getServiceReferences(CommandGroup.class,
                        "(groupName=" + groupName + ")");
                    if (refs.isEmpty()) {
                        throw new IOException("No such command group: " + groupName);
                    }

                    commandGroupRef = refs.iterator().next();
                } catch (InvalidSyntaxException ignore) {
                    // 2020-03-31: before refactoring, NPE was thrown in this case
                }
            }
        }
        ArrayList<String> vargs = new ArrayList<>();
        boolean done = false;
        while (!done) {
            if (word != null) {
                vargs.add(word);
                word = null;
            } else if (aliasPos > 0) {
                if (aliasPos < aliasBuf.length) {
                    word = aliasBuf[aliasPos++];
                } else {
                    aliasPos = 0;
                }
            } else {
                switch (in.nextToken()) {
                case '|':
                    out = new Pipe();
                    isPiped = true;
                    done = true;
                    break;
                case '&':
                    isBackground = true;
                case ';':
                    done = true;
                    break;
                case '<':
                    if (in.nextToken() != StreamTokenizer.TT_WORD) {
                        throw new IOException("Empty input redirect");
                    }
                    // Set input to file
                    break;
                case '>':
                    if (in.nextToken() != StreamTokenizer.TT_WORD) {
                        throw new IOException("Empty output redirect");
                    }
                    // Set output to file
                    break;
                case StreamTokenizer.TT_EOL:
                case StreamTokenizer.TT_EOF:
                    in.pushBack();
                    done = true;
                    break;
                case '"':
                case '\'':
                case StreamTokenizer.TT_WORD:
                    word = in.sval;
                    break;
                default:
                    throw new IOException("Unknown token: " + in.ttype);
                }
            }
        }
        args = vargs.toArray(new String[0]);
    }

    public void runThreaded() {
        String gname = commandGroupRef != null ? (String) commandGroupRef
                .getProperty("groupName") : SessionCommandGroup.NAME;

        thread = new Thread(this, "Console cmd: " + gname
                + (args.length == 0 ? "" : "/" + args[0]));

        thread.start();

    }
    
    public synchronized void run() {
        bc.addServiceListener(this);
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            CommandGroup cg;
            if (commandGroupRef != null) {
                cg = bc.getService(commandGroupRef);
                if (cg == null) {
                    return null;
                }
            } else {
                cg = sessionCommandGroup;
            }
            if (out instanceof PrintWriter) {
                cg.execute(args, in, (PrintWriter) out, session);
            } else {
                cg.execute(args, in, new PrintWriter(out), session);
            }
            if (commandGroupRef != null) {
                try {
                    bc.ungetService(commandGroupRef);
                } catch (IllegalStateException ignore) {
                    // Can happen if the command has invalidated our bc (e.g. update)
                    // TODO: Warn about this? The input can be screwed up.
                }
            }
            if (out instanceof Pipe) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
            return null;
        });
        try {
            bc.removeServiceListener(this);
        } catch (IllegalStateException ignore) {
            // Can happen if the command has invalidated our bc (e.g. update)
        }
    }

    public void serviceChanged(ServiceEvent e) {
        if (e.getServiceReference() == commandGroupRef) {
            synchronized (this) {
                // Wait for run command
            }
        }
    }

  static ServiceReference<CommandGroup> matchCommandGroup(BundleContext bc,
                                                          String word)
      throws IOException
  {
    ServiceReference<CommandGroup> res = null;
    Collection<ServiceReference<CommandGroup>> refs = null;
    try {
      refs = bc.getServiceReferences(CommandGroup.class,
                                     "(groupName=" + word + "*)");
    } catch (InvalidSyntaxException ignore) {
    }
    if (refs == null) {
        throw new IOException("No such command group: " + word);
    }
    if (refs.size()==1) {
      // A single match, use it.
      res = refs.iterator().next();
    } else {
      // Multiple matches, check for exact match.
      for (ServiceReference<CommandGroup> srcg : refs) {
        if (word.equals(srcg.getProperty("groupName"))) {
          res = srcg;
          break;
        }
      }
    }
    if (SessionCommandGroup.NAME.startsWith(word)) {
      if (refs.isEmpty() || SessionCommandGroup.NAME.equals(word)) {
        return null;
      }
    } else if (res != null) {
      return res;
    } else if (refs.isEmpty()) {
      throw new IOException("No such command group: " + word);
    }
    throw new IOException("Several command groups starting with: " + word);
  }

}
