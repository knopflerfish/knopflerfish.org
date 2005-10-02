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

package org.knopflerfish.bundle.logcommands;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.knopflerfish.service.log.LogConfig;
import org.knopflerfish.service.log.LogUtil;
import org.osgi.framework.Bundle;

/**
 * * Command group for console configuration of the Log. * *
 * 
 * @author Gatespace AB *
 * @version $Revision: 1.1.1.1 $
 */
public class LogConfigCommandGroup extends CommandGroupAdapter {

    LogConfigCommandGroup() {
        super("logconfig", "Configuration commands for the log.");
    }

    //
    // Set memory size command
    //
    public final static String USAGE_MEMORY = "[<int>]";

    public final static String[] HELP_MEMORY = new String[] {
            "Number of log entries to keep in memory.",
            "The no argument version prints the current setting.",
            "<int>   The new number of log entries to keep.", };

    public int cmdMemory(Dictionary opts, Reader in, PrintWriter out,
            Session session) {

        // Get log configuration service
        LogConfig configuration = (LogConfig) LogCommands.logConfigTracker
                .getService();
        if (configuration == null) {
            out.println("Unable to get a LogConfigService");
            return 1;
        }

        String value = (String) opts.get("int");

        if (value != null) {
            try {
                configuration.setMemorySize(Integer.parseInt(value));
            } catch (NumberFormatException nfe) {
                out.println("Can not set log memory size (" + nfe + ").");
            }
        } else {
            out.println("  log memory size: " + configuration.getMemorySize());
        }
        return 0;
    }

    //
    // Set level command
    //
    public final static String USAGE_SETLEVEL = "<level> [<bundle>] ...";

    public final static String[] HELP_SETLEVEL = new String[] {
            "Set log level",
            "<level>   The new log level (one of error,warning,info,debug or default)",
            "<bundle>  The bundle(s) that the new level applies to. If no bundles are",
            "          given the default level is changed. The bundle may be given as",
            "          the bundle id, the file location of the bundle or the bundle's",
            "          short-name. If the bundle's short-name is given then the default",
            "          configuration for all bundles with the given short-name will be set.",
            "          This means that if wanting to set the configuration of a specific",
            "          bundle the bundle id or the bundle location has to be given. ", };

    public int cmdSetlevel(Dictionary opts, Reader in, PrintWriter out,
            Session session) {

        // Get log configuration service
        LogConfig configuration = (LogConfig) LogCommands.logConfigTracker
                .getService();
        if (configuration == null) {
            out.println("Unable to get a LogConfigService");
            return 1;
        }

        String l = (String) opts.get("level");
        int level = LogUtil.toLevel((l.trim()), -1);
        if (level == -1) {
            out.println("Unknown level: " + l);
            return 1;
        }
        String[] selection = (String[]) opts.get("bundle");
        if (selection != null) {
            setValidBundles(configuration, selection, level);
            configuration.commit();
        } else {
            configuration.setFilter(level);
        }
        return 0;
    }

    private void setValidBundles(LogConfig configuration,
            String[] givenBundles, int level) {
        String location = null;
        for (int i = givenBundles.length - 1; i >= 0; i--) {
            location = givenBundles[i].trim();
            try {
                long id = Long.parseLong(location);
                location = (LogCommands.bc.getBundle(id) != null) ? (LogCommands.bc
                        .getBundle(id)).getLocation()
                        : null;
            } catch (NumberFormatException nfe) {
            }
            if (location != null && location.length() > 0) {
                configuration.setFilter(location, level);
            }
        }
    }

    //
    // Show level command
    //
    public final static String USAGE_SHOWLEVEL = "[<bundle>] ...";

    public final static String[] HELP_SHOWLEVEL = new String[] {
            "Show current log levels.",
            "All existing default configurations are marked with (default).",
            "Bundles not yet installed, which are not a default configuration,",
            "are given with their full path to differentiate the bundles. ",
            "<bundle>     Show level for the specified bundles only. The bundle",
            "may be given as the bundle id, bundle's short-name, or the bundle ",
            "location. If the bundle's short-name is supplied then all bundles ",
            "configured with that name will be shown.", };

    public int cmdShowlevel(Dictionary opts, Reader in, PrintWriter out,
            Session session) {

        // Get log configuration service
        LogConfig configuration = (LogConfig) LogCommands.logConfigTracker
                .getService();
        if (configuration == null) {
            out.println("Unable to get a LogConfigService");
            return 1;
        }

        String[] selections = (String[]) opts.get("bundle");
        if (selections == null) {
            HashMap filters = configuration.getFilters();
            selections = (String[]) ((filters).keySet())
                    .toArray(new String[filters.size()]);
        }
        // Print the default filter level.
        out.println("    *  " + LogUtil.fromLevel(configuration.getFilter(), 8)
                + "(default)");
        printValidBundles(configuration, selections, out);
        return 0;
    }

    private void printValidBundles(LogConfig configuration, String[] selection,
            PrintWriter out) {
        String ref = null, short_name, full_name, comparable_name, name;
        Bundle[] bundles = null;
        Bundle bundle;
        long id = 0;
        HashSet names = new HashSet();
        for (int i = selection.length - 1; i >= 0; i--) {
            ref = selection[i].trim();
            if (ref.length() > 0) {
                try {
                    id = Long.parseLong(ref);
                    bundle = LogCommands.bc.getBundle(id);
                    if (bundle != null) {
                        full_name = bundle.getLocation();
                        short_name = Util.shortName(bundle);
                        if (!names.contains(full_name)) {
                            out.println(Util.showId(bundle)
                                    + " "
                                    + LogUtil.fromLevel(getLevel(configuration,
                                            full_name, short_name + ".jar"), 8)
                                    + short_name);
                            names.add(full_name);
                        }
                    }
                } catch (NumberFormatException nfe) {
                    id = -1;
                    comparable_name = getCommonLocation(ref);
                    bundles = LogCommands.bc.getBundles();
                    for (int j = bundles.length - 1; j >= 0; j--) {
                        short_name = Util.shortName(bundles[j]);
                        full_name = bundles[j].getLocation();
                        if ((full_name).equals(comparable_name)
                                && (!names.contains(full_name))) {
                            out.println(Util.showId(bundles[j])
                                    + " "
                                    + LogUtil.fromLevel(getLevel(configuration,
                                            full_name, short_name + ".jar"), 8)
                                    + short_name);
                            names.add(full_name);
                            break;
                        } else if ((short_name + ".jar")
                                .equals(comparable_name)
                                && !names.contains(full_name)) {
                            out.println(Util.showId(bundles[j])
                                    + " "
                                    + LogUtil.fromLevel(getLevel(configuration,
                                            full_name, short_name + ".jar"), 8)
                                    + short_name);
                            names.add(full_name);
                        }
                    }
                    HashMap filters = configuration.getFilters();
                    // If a full path is given
                    if (((comparable_name.lastIndexOf("/") != -1 || comparable_name
                            .lastIndexOf("\\") != -1))
                            && filters.containsKey(comparable_name)
                            && !names.contains(comparable_name)) {
                        out.println("    -  "
                                + LogUtil.fromLevel(getLevel(configuration,
                                        comparable_name, ""), 8)
                                + getFullName(comparable_name)
                                + " (Bundle not yet installed)");
                        names.add(comparable_name);
                    }
                    // A short name is supplied
                    else {
                        if (filters.containsKey(comparable_name)
                                && !names.contains(comparable_name)) {
                            out.println("    -  "
                                    + LogUtil.fromLevel(getLevel(configuration,
                                            "", comparable_name), 8)
                                    + getShortName(comparable_name)
                                    + " (default)");
                            names.add(comparable_name);
                        }
                        for (Iterator it = filters.keySet().iterator(); it
                                .hasNext();) {
                            name = (String) it.next();
                            if (name.endsWith(comparable_name)
                                    && !names.contains(name)) {
                                out.println("    -  "
                                        + LogUtil.fromLevel(getLevel(
                                                configuration, name, ""), 8)
                                        + getFullName(name)
                                        + " (Bundle not yet installed)");
                                names.add(name);
                            }
                        }
                    }
                }
            }
        }
    }

    private int getLevel(LogConfig configuration, String full_name,
            String short_name) {
        HashMap filters = configuration.getFilters();
        Integer level_to_use = (Integer) filters.get(full_name);
        if (level_to_use == null) {
            level_to_use = (Integer) filters.get(short_name);
        }
        return (level_to_use != null) ? level_to_use.intValue() : configuration
                .getFilter();
    }

    private String getFullName(String bundle) {
        return fillName(new StringBuffer(bundle), 30);
    }

    private String getShortName(String bundle) {
        return fillName(new StringBuffer(bundle.substring(0, bundle
                .indexOf(".jar"))), 17);
    }

    private String fillName(StringBuffer sb, int length) {
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    //
    // Set out command
    //
    public final static String USAGE_OUT = "[-on | -off]";

    public final static String[] HELP_OUT = new String[] {
            "Configures logging to standard out",
            "-on          Turns on writing of log entries to standard out.",
            "-off         Turns off writing of log entries to standard out.", };

    public int cmdOut(Dictionary opts, Reader in, PrintWriter out,
            Session session) {

        // Get log configuration service
        LogConfig configuration = (LogConfig) LogCommands.logConfigTracker
                .getService();
        if (configuration == null) {
            out.println("Unable to get a LogConfigService");
            return 1;
        }

        if (!configuration.isDefaultConfig()) {
            out
                    .println("  This command is no persistent. (No valid configuration has been received)");
        }

        boolean optionFound = false;
        // System.out logging on/off
        if (opts.get("-on") != null) {
            optionFound = true;
            configuration.setOut(true);
        } else if (opts.get("-off") != null) {
            optionFound = true;
            configuration.setOut(false);
        }
        // Show current config
        if (!optionFound) {
            boolean isOn = configuration.getOut();
            out.println("  Logging to standard out is " + (isOn ? "on" : "off")
                    + ".");
        }
        return 0;
    }

    //
    // Set file command
    //
    public final static String USAGE_FILE = "[-on | -off] [-size #size#] [-gen #gen#] [-flush | -noflush]";

    public final static String[] HELP_FILE = new String[] {
            "Configures the file logging (the no argument version prints the current settings)",
            "-on          Turns on writing of log entries to file.",
            "-off         Turns off writing of log entries to file.",
            "-size #size# Set the maximum size of one log file (characters).",
            "-gen #gen#   Set the number of log file generations that are kept.",
            "-flush       Turns on log file flushing after each log entry.",
            "-noflush     Turns off log file flushing after each log entry.", };

    public int cmdFile(Dictionary opts, Reader in, PrintWriter out,
            Session session) {

        // Get log configuration service
        LogConfig configuration = (LogConfig) LogCommands.logConfigTracker
                .getService();
        if (configuration == null) {
            out.println("Unable to get a LogConfigService");
            return 1;
        }

        if (!configuration.isDefaultConfig()) {
            out.println("  This command is not persistent. "
                    + "(No valid configuration has been received)");
        }

        if (configuration.getDir() != null) {
            boolean optionFound = false;
            // File logging on/off
            if (opts.get("-on") != null) {
                optionFound = true;
                configuration.setFile(true);
            } else if (opts.get("-off") != null) {
                optionFound = true;
                configuration.setFile(false);
            }
            // Flush
            if (opts.get("-flush") != null) {
                optionFound = true;
                if (!configuration.getFile()) {
                    out
                            .println("Cannot activate flush (file logging disabled).");
                } else {
                    configuration.setFlush(true);
                }
            } else if (opts.get("-noflush") != null) {
                optionFound = true;
                if (!configuration.getFile()) {
                    out
                            .println("Cannot deactivate flush (file logging disabled).");
                } else {
                    configuration.setFlush(false);
                }
            }
            // Log size
            String value = (String) opts.get("-size");
            if (value != null) {
                optionFound = true;
                if (!configuration.getFile()) {
                    out.println("Cannot set log size (file logging disabled).");
                } else {
                    try {
                        configuration.setFileSize(Integer.parseInt(value));
                    } catch (NumberFormatException nfe1) {
                        out.println("Cannot set log size (" + nfe1 + ").");
                    }
                }
            }
            // Log generations
            value = (String) opts.get("-gen");
            if (value != null) {
                optionFound = true;
                if (!configuration.getFile()) {
                    out
                            .println("Cannot set generation count (file logging disabled).");
                } else {
                    try {
                        configuration.setMaxGen(Integer.parseInt(value));
                    } catch (NumberFormatException nfe2) {
                        out.println("Cannot set generation count (" + nfe2
                                + ").");
                    }
                }
            }
            // Update configuration only once
            if (optionFound)
                configuration.commit();
            // Show current config
            if (!optionFound) {
                boolean isOn = configuration.getFile();
                out.println("  file logging is " + (isOn ? "on" : "off") + ".");
                if (isOn) {
                    out.println("  file size:    "
                            + configuration.getFileSize());
                    out.println("  generations:  " + configuration.getMaxGen());
                    out.println("  flush:        " + configuration.getFlush());
                    out.println("  log location: " + configuration.getDir());
                }
            }
            return 0;
        }

        out.println(" This command is disabled. "
                + "(No filesystem support is available. )");
        return 0;
    }

    /***************************************************************************
     * Utility methods
     **************************************************************************/

    private String getCommonLocation(String location) {
        if (location.endsWith(".jar")) {
            return location;
        }
        return location + ".jar";
    }

}
