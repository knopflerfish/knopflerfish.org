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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.TreeSet;

import org.knopflerfish.service.console.CommandGroup;
import org.knopflerfish.service.console.Session;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Interface for commands to be handled by the console.
 * 
 * @author Gatespace AB
 */

public class SessionCommandGroup implements CommandGroup {

    static final String NAME = "session";
    private static final String ALIAS_SAVE = "aliases";

    private static final String help_alias   = "alias [<alias>] [<val>] ...  - Set or show aliases";
    private static final String help_help    = "help [<command group> | all] - Help about specific command group or all command groups";
    private static final String help_echo    = "echo [-n] ...                - Echo command arguments";
    private static final String help_enter   = "enter <command group>        - Enter command group mode";
    private static final String help_leave   = "leave                        - Leave command group mode";
    private static final String help_prompt  = "prompt <command prompt>      - Set command prompt";
    private static final String help_quit    = "quit                         - Exit this session";
    private static final String help_save    = "save [<file>]                - Save current aliases as a property file";
    private static final String help_restore = "restore [<file>]             - Restore aliases from a property file or from default aliases";
    private static final String help_source  = "source <URL>                 - Source commands at URL";
    private static final String help_unalias = "unalias <alias name>         - Remove an alias";

    private BundleContext bc;

    public SessionCommandGroup(BundleContext bc) {
        this.bc = bc;
    }

    /**
     * Returns the command group name.
     * 
     * @return command name.
     */
    public String getGroupName() {
        return NAME;
    }

    /**
     * Returns short command group help.
     * 
     * @return short command group help.
     */
    public String getShortHelp() {
        return "Session commands built into the console";
    }

    /**
     * Returns long command group help.
     * 
     * @return long command group help.
     */
    public String getLongHelp() {
        return "Available " + NAME + " commands:\n" + "  " + help_alias + "\n"
                + "  " + help_echo + "\n" + "  " + help_enter + "\n" + "  "
                + help_help + "\n" + "  " + help_leave + "\n" + "  "
                + help_prompt + "\n" + "  " + help_quit + "\n" + "  "
                + help_save + "\n" + "  " + help_restore + "\n" + "  "
                + help_source + "\n" + "  " + help_unalias + "\n";
    }

    /**
     * Executes the command.
     *
     * @param args
     *            argument list passed to the command
     * @param out
     *            output device to print result
     * @param in
     *            input for command
     * @return params logical format parameters
     */
    public int execute(String[] args, Reader in, PrintWriter out,
            Session session) {
        SessionImpl sessionImpl = (SessionImpl) session;
        if (args.length > 0) {
            if ("help".startsWith(args[0])) {
                return executeHelp(args, out, sessionImpl);
            } else if ("alias".startsWith(args[0])) {
                return executeAlias(args, out, sessionImpl);
            } else if ("enter".startsWith(args[0])) {
                return executeEnter(args, out, sessionImpl);
            } else if ("echo".startsWith(args[0])) {
                return executeEcho(args, out);
            } else if ("leave".startsWith(args[0])) {
                return executeLeave(args, out, sessionImpl);
            } else if ("prompt".startsWith(args[0])) {
                return executePrompt(args, out, sessionImpl);
            } else if ("quit".startsWith(args[0])) {
                return executeQuit(args, out, sessionImpl);
            } else if ("save".startsWith(args[0])) {
                return executeSave(args, out, sessionImpl);
            } else if ("restore".startsWith(args[0])) {
                return executeRestore(args, out, sessionImpl);
            } else if ("source".startsWith(args[0])) {
                return executeSource(args, out);
            } else if ("unalias".startsWith(args[0])) {
                return executeUnalias(args, out, sessionImpl);
            }
        }
        printUsage(out, getLongHelp());
        return -1;
    }

    private int executeHelp(String[] args, PrintWriter out, SessionImpl session) {
        if (args.length == 1) {
            if ("".equals(session.currentGroup)) {
                return help(out);
            }
            return helpAbout(session.currentGroup, out);
        } else if (args.length == 2) {
            if (args[1].equals("all")) {
                return help(out);
            }
            return helpAbout(args[1], out);
        }
        printUsage(out, help_help);
        return -1;
    }

    private int executeAlias(String[] args, PrintWriter out, SessionImpl session) {
        if (session == null) {
            out.println("Alias not available from runCommand method");
            return 1;
        }
        if (args.length == 1) {
            session.aliases.keySet().stream()
                .map(aliasName -> aliasName + " = " + session.aliases.getString(aliasName))
                .forEach(out::println);
        } else if (args.length == 2) {
            String aliasString = session.aliases.getString(args[1]);
            if (aliasString != null) {
                out.println(args[1] + " = " + aliasString);
            } else {
                out.println("No alias for: " + args[1]);
            }
        } else {
            String[] newAlias = new String[args.length - 2];
            System.arraycopy(args, 2, newAlias, 0, newAlias.length);
            session.aliases.put(args[1], newAlias);
        }
        return 0;
    }

    private int executeEnter(String[] args, PrintWriter out, SessionImpl session) {
        if (args.length == 2) {
            try {
                ServiceReference<CommandGroup> ref
                  = Command.matchCommandGroup(bc, args[1]);
                if (ref != null) {
                    session.currentGroup = (String) ref
                            .getProperty("groupName");
                } else {
                    session.currentGroup = NAME;
                }
                return 0;
            } catch (IOException e) {
                out.println(e.getMessage());
                return 1;
            }
        }
        printUsage(out, help_enter);
        return 1;
    }

    private int executeEcho(String[] args, PrintWriter out) {
        int pos = 1;
        boolean nl = true;
        if (args.length >= 2 && "-n".equals(args[1])) {
            nl = false;
            pos = 2;
        }
        while (pos < args.length) {
            out.print(args[pos]);
            if (++pos < args.length) {
                out.print(" ");
            }
        }
        if (nl) {
            out.println();
        }
        return 0;
    }

    private int executeLeave(String[] args, PrintWriter out, SessionImpl session) {
        if (args.length == 1) {
            session.currentGroup = "";
            return 0;
        }
        printUsage(out, help_leave);
        return -1;
    }

    private int executePrompt(String[] args, PrintWriter out, SessionImpl session) {
        if (args.length == 2) {
            session.prompt = args[1];
            return 0;
        }
        printUsage(out, help_prompt);
        return -1;
    }

    private int executeQuit(String[] args, PrintWriter out, SessionImpl session) {
        if (args.length == 1) {
            session.close();
            return 0;
        }
        printUsage(out, help_quit);
        return -1;
    }

    private int executeSave(String[] args, PrintWriter out, SessionImpl session) {
        File file = null;
        if (args.length == 1) {
            file = bc.getDataFile(ALIAS_SAVE);
        } else if (args.length == 2) {
            file = new File(args[1]);
        }
        if (file != null) {
            try {
                session.aliases.save(file);
            } catch (IOException e) {
                out.println("Failed to save aliases: " + e);
                return 1;
            }
            return 0;
        }
        printUsage(out, help_save);
        return -1;
    }

    private int executeRestore(String[] args, PrintWriter out, SessionImpl session) {
        if (args.length == 1) {
            session.aliases.setDefault();
            return 0;
        } else if (args.length == 2) {
            try {
                session.aliases.clear();
                session.aliases.restore(new File(args[1]));
            } catch (IOException e) {
                out.println("Failed to restore aliases from " + args[1]
                        + ": " + e);
                return 1;
            }
            return 0;
        }
        printUsage(out, help_restore);
        return -1;
    }

    private int executeSource(String[] args, PrintWriter out) {
        if (args.length == 2) {
            InputStreamReader sin = null;
            try {
                URL surl = new URL(args[1]);
                sin = new InputStreamReader(surl.openStream());
                SessionImpl ss = new SessionImpl(bc, "source: "
                        + args[1], sin, out, null);
                ss.prompt = null;
                ss.start();
                try {
                    ss.join();
                } catch (InterruptedException ignore) {
                }
                ss.close();
            } catch (IOException e) {
                out.println("Failed to source URL: " + e.getMessage());
                return 1;
            } finally {
                if (sin != null) {
                    try {
                        sin.close();
                    } catch (IOException ignore) {
                    }
                }
            }
            return 0;
        }
        printUsage(out, help_source);
        return -1;
    }

    private int executeUnalias(String[] args, PrintWriter out, SessionImpl session) {
        if (session == null) {
            out.println("Unalias not available from runCommand method");
            return 1;
        }
        if (args.length == 2) {
            if (session.aliases.remove(args[1]) != null) {
                return 0;
            }
            return 1;
        }
        printUsage(out, help_unalias);
        return -1;
    }

    private void printUsage(PrintWriter out, String usage) {
        if (usage != null) {
            usage = "Usage: " + usage;
        } else {
            usage = getLongHelp();
        }
        out.println(usage);
    }

    /**
   * Help about available command groups.
   * 
   * @param out
   *          output device to print result
   */
  int help(PrintWriter out) {
    final TreeSet<String> commandGroupInfos = new TreeSet<>();
    commandGroupInfos.add(helpLine(this));

    try {
      final Collection<ServiceReference<CommandGroup>> services = bc
          .getServiceReferences(CommandGroup.class, null);

      for (ServiceReference<CommandGroup> service : services) {
        final CommandGroup commandGroup = bc.getService(service);
        if (commandGroup != null) {
          commandGroupInfos.add(helpLine(commandGroup));
          bc.ungetService(service);
        }
      }

      out.println("Available command groups (type 'enter' to enter a group):");
      commandGroupInfos.forEach(out::println);
      commandGroupInfos.clear();
    } catch (InvalidSyntaxException ignore) {
    }

    return 0;
  }

  private String helpLine(final CommandGroup commandGroup) {
    return commandGroup.getGroupName() + " - " + commandGroup.getShortHelp();
  }

  /**
   * Help about a command group.
   * 
   * @param out
   *          output device to print result
   */
  int helpAbout(String group, PrintWriter out) {
    try {
      ServiceReference<CommandGroup> ref = Command.matchCommandGroup(bc, group);
      if (ref != null) {
        CommandGroup commandGroup = bc.getService(ref);
        if (commandGroup != null) {
          out.print(commandGroup.getLongHelp());
          bc.ungetService(ref);
          return 0;
        }
      } else {
        // Null means session command group
        out.print(getLongHelp());
        return 0;
      }
    } catch (IOException e) {
      out.println(e.getMessage());
      return 1;
    }
    out.println("No such command group: " + group);
    return 1;
  }

}
