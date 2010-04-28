/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.knopflerfish.service.log.LogConfig;
import org.knopflerfish.service.log.LogUtil;
import org.osgi.framework.Bundle;

/**
 * Command group for console configuration of the Log.
 *
 * @author Gatespace AB, Knopflerfish Project
 */
public class LogConfigCommandGroup extends CommandGroupAdapter {

  LogConfigCommandGroup() {
    super("logconfig", "Configuration commands for the log.");
  }

  //
  // Set memory size command
  //
  public final static String USAGE_MEMORY = "[-c] [<int>]";

  public final static String[] HELP_MEMORY = new String[] {
    "Number of log entries to keep in memory.",
    "The no argument version prints the current setting.",
    "<int>   The new number of log entries to keep.",
    "-c      Clear the in memory log.",
  };

  public int cmdMemory(final Dictionary opts,
                       final Reader in,
                       final PrintWriter out,
                       final Session session) {

    // Get log configuration service
    final LogConfig configuration = (LogConfig)
      LogCommands.logConfigTracker.getService();
    if (configuration == null) {
      out.println("Unable to get a LogConfigService");
      return 1;
    }

    final boolean clear = null != opts.get("-c");

    int newSize = -1;
    final String sizeArg = (String) opts.get("int");
    if (sizeArg != null) {
      try {
        newSize = Integer.parseInt(sizeArg);
      } catch (NumberFormatException nfe) {
        out.println("Can not set log memory size (" + nfe + ").");
        return 2;
      }
    }

    final int curSize = configuration.getMemorySize();
    if (clear) {
      configuration.setMemorySize(1);// This will throw away all old entries
      configuration.setMemorySize(-1<newSize ? newSize : curSize);
    } else if (-1<newSize) {
      configuration.setMemorySize(curSize);
    } else {
      out.println("  log memory size: " + curSize);
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
    "          bundle the bundle id or the bundle location has to be given. ",
  };

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
        final long id = Long.parseLong(location);
        final Bundle bundle = LogCommands.bc.getBundle(id);
        if (null!=bundle) {
          final Dictionary d = bundle.getHeaders("");
          location = (String) d.get("Bundle-SymbolicName");
          if (location == null) {
            location = bundle.getLocation();
          }
        } else {
          location = null;
        }
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
    "Show current log levels for bundles.",
    "When called without an argument, all bundles with a log level configuration",
    "will be listed followed by all configurations currently not matching a",
    "bundle.",
    "<bundle>     Show level for the specified bundles only. The bundle",
    "             may be given as the bundle id, bundle's short-name,",
    "             bundles symbolic name or the bundle location. If the bundle",
    "             uses the default log level its line will end with the text \"(default)\".",
  };

  public int cmdShowlevel(Dictionary opts, Reader in, PrintWriter out,
                          Session session) {

    // Get log configuration service
    LogConfig configuration = (LogConfig) LogCommands.logConfigTracker
      .getService();
    if (configuration == null) {
      out.println("Unable to get a LogConfigService");
      return 1;
    }

    final Bundle[] bundles = LogCommands.bc.getBundles();

    String[] selections = (String[]) opts.get("bundle");
    final boolean showAll = null==selections;
    if (selections == null) {
      HashMap filters = configuration.getFilters();
      selections = (String[])
        filters.keySet().toArray(new String[filters.size()]);
    }
    // Print the default filter level.
    out.println("    *  " + LogUtil.fromLevel(configuration.getFilter(), 8)
                + "(default)");

    final Set matchedSelectors = new HashSet();
    Util.selectBundles(bundles, selections, matchedSelectors);
    Util.sortBundlesId(bundles);

    printBundleLogLevels(configuration, bundles, out);
    if (showAll) {
      printConfiguredLogLevels(configuration, selections, matchedSelectors,
                               out);
    }

    return 0;
  }

  private void printBundleLogLevels(final LogConfig configuration,
                                    final Bundle[] bundles,
                                    final PrintWriter out)
  {
    for (int i = 0; i < bundles.length; i++) {
      final Bundle bundle = bundles[i];
      if (bundle != null) {
        final String full_name = bundle.getLocation();
        final String short_name = Util.shortName(bundle);
        int level = getLevel(configuration, bundle);
        final boolean isDefaultLevel = level < 0;
        level = isDefaultLevel ? configuration.getFilter() : level;
        out.println(Util.showId(bundle)
                    + " "
                    + LogUtil.fromLevel(level, 8)
                    + short_name
                    + (isDefaultLevel ? " (default)" : ""));
      }
    }
  }

  private void printConfiguredLogLevels(final LogConfig configuration,
                                        final String[] selections,
                                        final Set weedOut,
                                        final PrintWriter out) {
    final SortedSet selectionSet = new TreeSet();
    for (int i = selections.length - 1; i >= 0; i--) {
      final String selection = selections[i];
      if (null!=selection && 0<selection.length()
          && !weedOut.contains(selection)) {
        selectionSet.add(selection);
      }
    }

    for (Iterator it = selectionSet.iterator(); it.hasNext(); ) {
      final String selection = (String) it.next();
      out.println("    -  "
                  + LogUtil.fromLevel(getLevel(configuration,
                                               selection, ""), 8)
                  + getFullName(selection)
                  + " (Bundle not yet installed)");
    }
  }

  private int getLevel(final LogConfig configuration, final Bundle bundle)
  {
    final HashMap filters = configuration.getFilters();
    Integer level;

    level = (Integer) filters.get(bundle.getLocation());
    if (level == null) {
      Dictionary d = bundle.getHeaders("");
      String l = (String)d.get("Bundle-SymbolicName");
      if (l == null) {
        l = (String)d.get("Bundle-Name");
      }
      if (l != null) {
        level = (Integer) filters.get(l);
      }
    }
    return (level != null) ? level.intValue() : -1;
  }


  private int getLevel(final LogConfig configuration,
                       final String full_name,
                       final String short_name)
  {
    final HashMap filters = configuration.getFilters();
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
    return fillName(new StringBuffer(bundle), 17);
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

}
