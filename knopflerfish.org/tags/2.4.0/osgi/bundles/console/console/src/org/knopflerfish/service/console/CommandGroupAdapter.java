/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Adapter class for CommandGroup. Simplifies the creations of command groups.
 * It takes away the work of parsing command lines and generates help text. It
 * uses reflection to check which commands you have created.
 * <p>
 * To create your own command group you extend this class and add two variables
 * and a method.
 * <dl>
 * <dt><code>USAGE_{NAME}</code></dt>
 * <dd> A static String (NAME must be capital letters) that describes the
 * command line options of the command. The following is valid usage string
 * (only space can be used as whitespace):
 * 
 * <pre>
 *      usage : [ flags ] [ args [ '...' ]]
 *      flags : flag [ flags ]
 *      flags : oflag [ flags ]
 *      flag  : flag '|' flag
 *      flag  : '-'FLAGNAME [ '#'[TEXT['#']] ]
 *      oflag : '[' flag ']'
 *      args  : '&lt;' ARGNAME '&gt;'
 *      args  : '[' args ']'
 * </pre>
 * 
 * Note: The flag "-help" is automatically added and handled by
 * CommandGroupAdapter. </dd>
 * <dt><code>HELP_{NAME}</code></dt>
 * <dd> A static String array (NAME must be capital letters) that gives the help
 * text for the command. Each element is printed on its own line. The first
 * element should be a short description of the command as it is used to
 * describe the command when we generate the help text for the command group.
 * </dd>
 * <dt><code>int cmd{Name}(Dictionary, Reader, PrintWriter, Session)</code></dt>
 * <dd> A method (the first letter in the command name must be capital, and the
 * rest must be lowercase) that is called when the CommandGroupAdapter has
 * matched the command and decode the command flags. The Dictionary contains the
 * parsed commands arguments. If a flag is present the key "-FLAGNAME" is
 * present and any value as the object (type String) connected to the key. The
 * same goes for "ARGNAME". If the usage string ends with "...", then the last
 * ARGNAME key is connected with a String array object that contains all the
 * remaining arguments on the command line. The method parameters Reader,
 * PrintWriter and Session are the same as for the <tt>execute</tt> method.
 * The method should return a 0 if the command executed okey. </dd>
 * </dl>
 * <p>
 * The object must then be registered under the class name <br>
 * <code>org.knopflerfish.service.console.CommandGroup</code> with the
 * property "groupName" set to the command group name.
 * <p>
 * Example:
 * 
 * <pre>
 * package com.apa;
 * 
 * import java.io.*;
 * import java.util.*;
 * 
 * import org.knopflerfish.service.console.*;
 * 
 * public class MyCommandGroup extends CommandGroupAdapter {
 * 
 *     MyCommandGroup() {
 *         super(&quot;echocommands&quot;, &quot;Echo commands&quot;);
 *     }
 * 
 *     public final static String USAGE_ECHO = &quot;[-n] &lt;text&gt; ...&quot;;
 * 
 *     public final static String[] HELP_ECHO = new String[] {
 *             &quot;Echo command arguments&quot;, &quot;-n     Don't add newline at end&quot;,
 *             &quot;&lt;text&gt; Text to echo&quot; };
 * 
 *     public int cmdEcho(Dictionary opts, Reader in, PrintWriter out,
 *             Session session) {
 *         String[] t = (String[]) opts.get(&quot;text&quot;);
 *         for (int i = 0; i &lt; t.length; i++) {
 *             out.print(t[i]);
 *         }
 *         if (opts.get(&quot;-n&quot;) == null) {
 *             out.println();
 *         }
 *         return 0;
 *     }
 * }
 * </pre>
 * 
 * @author Gatespace AB
 */

public abstract class CommandGroupAdapter implements CommandGroup {

    /**
     * Full class name of CommandGroup interface.
     */
    public final static String COMMAND_GROUP = org.knopflerfish.service.console.CommandGroup.class
            .getName();

    String groupName;

    String shortHelp;

    /**
     * Constructs a command group. This should be called via super() if you
     * extend this class.
     * 
     * @param groupName
     *            the name for this command group
     * @param shortHelp
     *            one line description of this command group
     */
    public CommandGroupAdapter(String groupName, String shortHelp) {
        this.groupName = groupName;
        this.shortHelp = shortHelp;
    }

    /**
     * Returns the command group name. This methods returns the group name
     * registered via the constructor.
     * 
     * @return Group name.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns short command group help. Returns the short help message
     * registered via the constructor.
     * 
     * @return short command group help.
     */
    public String getShortHelp() {
        return shortHelp;
    }

    /**
     * Returns long command group help. This is built using the
     * <code>HELP_{CMD}</code> and <code>USAGE_{CMD}</code> variables of the
     * sub-class.
     * 
     * @return long command group help.
     */
    public String getLongHelp() {
        StringBuffer res = new StringBuffer();
        res.append("Available " + groupName + " commands:\n");
        Field[] f = getClass().getFields();
        for (int i = 0; i < f.length; i++) {
            String name = f[i].getName();
            if (name.startsWith("HELP_")) {
                try {
                    name = name.substring(5).toLowerCase();
                    DynamicCmd cmd = new DynamicCmd(this, name);
                    res.append("  " + name + " [-help] " + cmd.usage + " - "
                            + cmd.help[0] + "\n");
                } catch (Exception ignore) {
                }
            }
        }
        return res.toString();
    }

    /**
     * Executes a command in the command group. This parses the command line,
     * matches it with commands from the sub-class, check the
     * <code>USAGE_{CMD}</code> string of that command and calls its
     * <code>cmd{Cmd}</code> method of the sub-class.
     * 
     * @param args
     *            argument list passed to the command
     * @param out
     *            output device to print result
     * @param in
     *            input for command
     * @param session
     *            a handle to command session or null if single command
     * @return status from execution, 0 means okey
     */
    public int execute(String[] args, Reader in, PrintWriter out,
            Session session) {
        if (args.length == 0 || args[0] == null || args[0].length() == 0) {
            return -1;
        }
        DynamicCmd cmd;
        try {
            cmd = new DynamicCmd(this, args[0]);
        } catch (Exception e) {
            out.println(e.getMessage());
            return -2;
        }
        for (int i = 0; i < args.length; i++) {
            if ("-help".equals(args[i])) {
                out.println("Usage: " + args[0] + " [-help] " + cmd.usage);
                for (int j = 0; j < cmd.help.length; j++) {
                    out.println("  " + cmd.help[j]);
                }
                return 0;
            }
        }
        try {
            Integer res = (Integer) cmd.cmd.invoke(this, new Object[] {
                    getOpt(args, cmd.usage), in, out, session });
            return res.intValue();
        } catch (IllegalAccessException e) {
            out.println("Command failed: " + e.getMessage());
            return -1;
        } catch (InvocationTargetException e) {
            out.println("Command execution failed, stack trace follows:");
            e.getTargetException().printStackTrace(out);
            return -1;
        } catch (Exception e) {
            out.println(e.getMessage());
            return -1;
        }
    }

    /**
     * Method to do argument parsing. See parsed syntax in class
     * <tt>CommandGroupAdapter</tt> description.
     * 
     * @param args
     *            argument list passed to the command
     * @param usage
     *            usage string
     * @return Dictionary with parsed arguments
     * @exception Exception
     *                Thrown if it fails to parse args or usage
     */
    public Dictionary getOpt(String[] args, String usage) throws Exception {
        Hashtable res = new Hashtable();
        res.put("command", args[0]);
        args[0] = null;
        parseUsage(usage.trim(), 0, args, res, 0);
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                throw new Exception("Unknown argument: " + args[i]);
            }
        }
        return res;
    }

    //
    // Internal methods
    //

    private final static String DOING_ARGS = "_D_";

    private final static String LAST = "_L_";

    private int parseUsage(String usage, int pos, String[] args, Hashtable res,
            int level) throws Exception {
        int ulen = usage.length();
        while (pos < ulen) {
            switch (usage.charAt(pos)) {
            case '-':
                if (res.containsKey(DOING_ARGS)) {
                    throw new Exception("Can not mix flags and args: "
                            + usage.substring(pos));
                }
                boolean found = false;
                String flag = "";
                boolean alt;
                do {
                    alt = false;
                    int start = pos++;
                    while (pos < ulen && usage.charAt(pos) != ' '
                            && usage.charAt(pos) != ']')
                        pos++;
                    flag = usage.substring(start, pos);
                    Object val = new Integer(1);
                    int fpos = -1;
                    for (int i = 0; i < args.length; i++) {
                        if (flag.equals(args[i])) {
                            fpos = i;
                            break;
                        }
                    }
                    while (pos < ulen && usage.charAt(pos) == ' ')
                        pos++;
                    if (pos < ulen) {
                        if (usage.charAt(pos) == '#') {
                            while (pos < ulen && usage.charAt(pos) != ' '
                                    && usage.charAt(pos) != ']')
                                pos++;
                            if (fpos >= 0) {
                                if (fpos + 1 < args.length) {
                                    val = args[fpos + 1];
                                    args[fpos + 1] = null;
                                } else {
                                    throw new Exception("No value for: " + flag);
                                }
                            }
                            while (pos < ulen && usage.charAt(pos) == ' ')
                                pos++;
                        }
                        if (pos < ulen) {
                            if (usage.charAt(pos) == '|') {
                                pos++;
                                while (pos < ulen && usage.charAt(pos) == ' ')
                                    pos++;
                                if (usage.charAt(pos) == '-') {
                                    alt = true;
                                } else {
                                    throw new Exception(
                                            "Missing flag in OR-expression: "
                                                    + usage.substring(pos));
                                }
                            }
                        }
                    }
                    if (fpos >= 0) {
                        Object old = res.put(args[fpos], val);
                        if (old != null) {
                            if (old instanceof Integer) {
                                res.put(args[fpos], new Integer(((Integer) old)
                                        .intValue() + 1));
                            } else {
                                throw new Exception(
                                        "Duplicate flagname with value in usage: "
                                                + args[fpos]);
                            }
                        }
                        args[fpos] = null;
                        found = true;
                    }
                } while (alt);
                if (!found && level == 0) {
                    throw new Exception("Mandatory flag not set, flags: "
                            + usage);
                }
                break;
            case '<':
                res.put(DOING_ARGS, "");
                int wstart = ++pos;
                pos = usage.indexOf('>', ++pos);
                if (pos == -1) {
                    throw new Exception("Unmatched: "
                            + usage.substring(wstart - 1));
                }
                String key = usage.substring(wstart, pos++);
                int i;
                for (i = 0; i < args.length; i++) {
                    if (args[i] != null) {
                        if (args[i].startsWith("-")) {
                            // '--' means '-' at begining of args
                            if (args[i].startsWith("--")) {
                                args[i] = args[i].substring(1);
                            } else {
                                throw new Exception("Unknown flag: " + args[i]);
                            }
                        }
                        if (res.put(key, args[i]) != null) {
                            throw new Exception("Duplicate argname in usage: "
                                    + key);
                        }
                        args[i] = null;
                        break;
                    }
                }
                if (i == args.length && level == 0) {
                    throw new Exception("Mandatory argument not set: " + key);
                }
                res.put(LAST, key);
                break;
            case '[':
                pos = parseUsage(usage, pos + 1, args, res, level + 1);
                break;
            case ']':
                if (level == 0) {
                    throw new Exception("Unmatched: " + usage.substring(pos));
                }
                return pos + 1;
            case '.':
                if (usage.substring(pos).equals("...")) {
                    String repeat = (String) res.get(LAST);
                    if (repeat != null && res.containsKey(repeat)) {
                        ArrayList v = new ArrayList();
                        do {
                            v.add(res.remove(repeat));
                            parseUsage("<" + repeat + ">]", 0, args, res, 1);
                        } while (res.containsKey(repeat));
                        String[] vres = new String[v.size()];
                        res.put(repeat, v.toArray(vres));
                    }
                    pos = ulen;
                } else {
                    throw new Exception("Unexpected usage end: "
                            + usage.substring(pos));
                }
                break;
            case ' ':
                pos++;
                break;
            default:
                throw new Exception("Unexpected character: "
                        + usage.charAt(pos));
            }
        }
        if (level > 0) {
            throw new Exception("Missing " + level + " closing ']'");
        }
        res.remove(DOING_ARGS);
        res.remove(LAST);
        return pos;
    }

}

class DynamicCmd {

    Method cmd;

    String usage;

    String[] help;

    DynamicCmd(CommandGroup cg, String name) throws Exception {
        try {
            Class cls = cg.getClass();
            String hname = "HELP_" + name.toUpperCase();
            Field[] f = cls.getFields();
            int match = -1;
            boolean multiple = false;
            for (int i = 0; i < f.length; i++) {
                String fname = f[i].getName();
                if (fname.equals(hname)) {
                    match = i;
                    name = fname.substring(5);
                    multiple = false;
                    break;
                } else if (fname.startsWith(hname)) {
                    if (match != -1) {
                        multiple = true;
                        continue;
                    }
                    match = i;
                    name = fname.substring(5);
                }
            }
            if (match == -1) {
                throw new Exception("No such command: " + name);
            }
            if (multiple) {
                throw new Exception("Multiple matching commands for: "
                                    + hname.substring(5));
            }
            help = (String[]) f[match].get(cg);
            usage = (String) cls.getField("USAGE_" + name.toUpperCase())
                    .get(cg);
            cmd = cls.getMethod("cmd" + name.substring(0, 1).toUpperCase()
                    + name.substring(1).toLowerCase(), new Class[] {
                    java.util.Dictionary.class, java.io.Reader.class,
                    java.io.PrintWriter.class, Session.class });
            if (!cmd.getReturnType().getName().equals("int")) {
                throw new Exception("No such command: " + name);
            }
        } catch (ClassNotFoundException e) {
            throw new Exception("Internal error");
        } catch (NoSuchFieldException e) {
            throw new Exception(
                    "Command implementation incomplete (USAGE string missing): "
                            + name);
        } catch (NoSuchMethodException e) {
            throw new Exception(
                    "Command implementation incomplete (cmd method missing): "
                            + name);
        }
    }

}
