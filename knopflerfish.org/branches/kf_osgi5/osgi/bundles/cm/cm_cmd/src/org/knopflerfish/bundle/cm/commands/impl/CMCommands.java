/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.cm.commands.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.shared.cm.CMDataReader;
import org.knopflerfish.shared.cm.CMDataWriter;
import org.knopflerfish.util.sort.Sort;

// ******************** CMCommands ********************
/**
 * Console interface to the CM.
 *
 * @author Per Gustafson
 */

public class CMCommands
  extends CommandGroupAdapter
  implements ServiceListener
{
  /***************************************************************************
   * Key in the session properties dictionary used to store the current (open)
   * configuration.
   **************************************************************************/
  private static final String CURRENT =
    "org.knopflerfish.bundle.cm.commands.impl.current";

  /***************************************************************************
   * Key in the session properties dictionary used to store the dictionary that
   * is edited for the current configuration.
   **************************************************************************/
  private static final String EDITED =
    "org.knopflerfish.bundle.cm.commands.impl.edited";

  /***************************************************************************
   * Key in the session properties dictionary used to store the change count of
   * the edited configuration when starting an edit operation.
   **************************************************************************/
  private static final String EDITED_VERSION =
    "org.knopflerfish.bundle.cm.commands.impl.edited.version";

  /***************************************************************************
   * Key in the session properties dictionary used to store the result of the
   * latest list command for later reference using -i options to several
   * commands.
   **************************************************************************/
  private static final String LISTED_CONFIGS =
    "org.knopflerfish.bundle.cm.commands.impl.listed.configs";

  /***************************************************************************
   * Key in the session properties dictionary used to store a mapping from PID
   * to index number of the result from the latest list command for later
   * reference by the show commands.
   **************************************************************************/
  private static final String LISTED_CONFIGS_PID_TO_INDEX =
    "org.knopflerfish.bundle.cm.commands.impl.listed.configs.pid.to.ix";

  BundleContext bc;

  ServiceReference<ConfigurationAdmin> refCA = null;

  private static Class<?> classBigDecimal;

  private static Constructor<?> consBigDecimal;

  static {
    try {
      classBigDecimal = Class.forName("java.math.BigDecimal");
      consBigDecimal =
        classBigDecimal.getConstructor(new Class[] { String.class });
    } catch (final Exception ignore) {
      classBigDecimal = null;
      consBigDecimal = null;
    }
  }

  public CMCommands(BundleContext bc)
  {
    super("configuration", "Configuration commands");
    this.bc = bc;
    refCA = bc.getServiceReference(ConfigurationAdmin.class);
    try {
      bc.addServiceListener(this,
                            "(objectClass="
                                + ConfigurationAdmin.class.getName() + ")");
    } catch (final InvalidSyntaxException ignored) {
    }
  }

  public final static String USAGE_LIST = "[<selection>] ...";

  public final static String[] HELP_LIST =
    new String[] { "List the PIDs of existing configurations.",
                  "<selection>  A PID that can contain wildcards '*',",
                  "             or an ldap filter, or an index in output",
                  "             from the latest use of this command.",
                  "             If no selection is given all existing PIDs",
                  "             will be listed." };

  public int cmdList(Dictionary<?, ?> opts,
                     Reader in,
                     PrintWriter out,
                     Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    ConfigurationAdmin srvCA = null;
    try {
      srvCA = getCA();

      final String[] selection = (String[]) opts.get("selection");

      Configuration[] cs = null;
      if (selection == null) {
        cs = srvCA.listConfigurations(null);
      } else {
        cs = getConfigurations(session, srvCA, selection);
      }
      if (cs == null || cs.length == 0) {
        out.println("No configurations available");
      } else {
        sortConfigurationArray(cs);
        setSessionProperty(session, LISTED_CONFIGS, cs);
        final Map<String,Integer> pidToIndex = new HashMap<String, Integer>();
        for (int i = 0; i<cs.length; i++) {
          pidToIndex.put(cs[i].getPid(), new Integer(i));
        }
        setSessionProperty(session, LISTED_CONFIGS_PID_TO_INDEX, pidToIndex);
        out.println("Available configurations:");
        for (int i = 0; i < cs.length; ++i) {
          out.println("[" + i + "] " + cs[i].getPid());
        }
      }

      retcode = 0; // Success!
    } catch (final Exception e) {
      out.print("List failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);

    } finally {
      if (srvCA != null) {
        bc.ungetService(refCA);
      }
    }
    return retcode;
  }

  public final static String USAGE_SHOW = "[-t] [<selection>] ...";

  public final static String[] HELP_SHOW =
    new String[] { "Show the saved versions of configurations.",
                  "-t           Include type for each property in the output.",
                  "<selection>  A pid that can contain wildcards '*',",
                  "             or an ldap filter, or an index in output",
                  "             from the latest use of the 'list' command.",
                  "             If no selection is given all configurations",
                  "             will be shown.",
                  "             Use 'current' command to see the properties",
                  "             of the currently edited configuration." };

  public int cmdShow(Dictionary<?, ?> opts,
                     Reader in,
                     PrintWriter out,
                     Session session)
  {
    ConfigurationAdmin srvCA = null;
    final boolean printTypes = opts.get("-t") != null;

    try {
      srvCA = getCA();
      final String[] selection = (String[]) opts.get("selection");
      final Configuration[] cs = getConfigurations(session, srvCA, selection);
      if (cs == null || cs.length == 0) {
        throw new Exception("No matching configurations for selection.");
      }
      sortConfigurationArray(cs);
      for (final Configuration cfg : cs) {
        printConfiguration(out, session, cfg, printTypes);
      }
    } catch (final Exception e) {
      out.print("Show failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } finally {
      if (srvCA != null) {
        bc.ungetService(refCA);
      }
    }
    return 0;
  }

  public final static String USAGE_CREATE = "[-f] <pid> [<template>]";

  public final static String[] HELP_CREATE =
    new String[] { "Create a configuration and open it for editing.",
                  "-f     If specified the pid argument is a factory pid.",
                  "<pid>  PID or factory pid of configuration to create",
                  "       depending on if -f flag is specified.",
                  "<template> Template configuration that the new configuration",
                  "           will be a clone of. The value is either a pid that",
                  "           can contain wildcards '*', or an ldap filter,",
                  "           or an index in output from the latest use of",
                  "           the 'list' command.",
                  "           If the template selectio doesn't match exactly one",
                  "           configuration it will have to be refined." };

  public int cmdCreate(Dictionary<?, ?> opts,
                       Reader in,
                       PrintWriter out,
                       Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    setCurrent(session, null);
    setEditingDict(session);
    final String pid = (String) opts.get("pid");
    final boolean createFactoryConfiguration = opts.get("-f") != null;

    ConfigurationAdmin srvCA = null;
    Configuration templateCfg = null;
    try {
      srvCA = getCA();

      final String template = (String) opts.get("template");
      if (template != null) {
        final Configuration[] templates = getConfigurations(session, srvCA, template);
        if (templates == null || templates.length == 0) {
          throw new Exception("Template didn't match any configurations. "
                              + "Remove the template parameter or change "
                              + "your it to match exactly one "
                              +"configuration.");
        } else if (templates.length == 1) {
          templateCfg = templates[0];
          if (pid.equals(templateCfg.getPid())) {
            throw new Exception("template configuration has the same PID as "
                                +"the new one.");
          }
        } else {
          throw new Exception("Template matched " + templates.length
                              + " configurations. Refine your selection "
                              +"to match exactly one configuration.");
        }
      }

      Configuration cfg = null;
      if (createFactoryConfiguration) {
        cfg = srvCA.createFactoryConfiguration(pid, null);
      } else {
        final Configuration[] exisitingCfgs =
          srvCA
              .listConfigurations("(" + Constants.SERVICE_PID + "=" + pid + ")");
        if (exisitingCfgs != null && exisitingCfgs.length == 1) {
          throw new Exception("A configuration with PID '" + pid
                              + "' already exists.");
        }
        cfg = srvCA.getConfiguration(pid, null);
      }

      if (cfg == null) {
        throw new Exception("Failed creating configuration with PID: '" + pid +"'.");
      }
      if (templateCfg != null) {
        cfg.update(templateCfg.getProperties());
      }
      setCurrent(session, cfg);
      retcode = 0; // Success!
    } catch (final Exception e) {
      out.print("Create failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);

    } finally {
      if (srvCA != null) {
        bc.ungetService(refCA);
      }
    }
    return retcode;
  }

  public final static String USAGE_DELETE = "<selection>";

  public final static String[] HELP_DELETE =
    new String[] { "Delete an existing configuration.",
                  "<selection>  A pid that can contain wildcards '*',",
                  "             or an ldap filter, or an index in output",
                  "             from the latest use of the 'list' command.",
                  "             If the selection doesn't match exactly one",
                  "             configuration it will have to be refined." };

  public int cmdDelete(Dictionary<?, ?> opts,
                       Reader in,
                       PrintWriter out,
                       Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    ConfigurationAdmin srvCA = null;
    try {
      final String selection = (String) opts.get("selection");

      srvCA = getCA();

      final Configuration[] cs = getConfigurations(session, srvCA, selection);
      if (cs == null || cs.length == 0) {
        throw new Exception("Selection didn't match any configurations. "
                            + "Change your selection to match exactly "
                            + "one configuration.");
      } else if (cs.length == 1) {
        out.println("Deleting " + cs[0].getPid());
        final Configuration current = getCurrent(session);
        if (current != null && current.getPid().equals(cs[0].getPid())) {
          setCurrent(session, null);
          setEditingDict(session);
        }
        cs[0].delete();
      } else {
        throw new Exception(
                            "Selection matched "
                                + cs.length
                                + " configurations. "
                                + "Refine your selection to match exactly one configuration.");
      }
      retcode = 0; // Success!
    } catch (final Exception e) {
      out.print("Delete failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);

    } finally {
      if (srvCA != null) {
        bc.ungetService(refCA);
      }
    }
    return retcode;
  }

  public final static String USAGE_EDIT = "<selection>";

  public final static String[] HELP_EDIT =
    new String[] { "Edit an existing configuration.",
                  "<selection>  A pid that can contain wildcards '*',",
                  "             or an ldap filter, or an index in output",
                  "             from the latest use of the 'list' command.",
                  "             If the selection doesn't match exactly one",
                  "             configuration it will have to be refined." };

  public int cmdEdit(Dictionary<?, ?> opts,
                     Reader in,
                     PrintWriter out,
                     Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    setEditingDict(session);
    setCurrent(session, null);

    ConfigurationAdmin srvCA = null;
    try {
      final String selection = (String) opts.get("selection");

      srvCA = getCA();

      final Configuration[] cs = getConfigurations(session, srvCA, selection);
      if (cs == null || cs.length == 0) {
        throw new Exception(
                            "Selection didn't match any configurations. "
                                + "Use 'create' to create the configuration you want to edit "
                                + "if it doesnt exist, or change your selection to match "
                                + "exactly one configuration.");
      } else if (cs.length == 1) {
        out.println("Editing " + cs[0].getPid());
        setCurrent(session, cs[0]);
      } else {
        throw new Exception(
                            "Selection matched "
                                + cs.length
                                + " configurations. "
                                + "Refine your selection to match exactly one configuration.");
      }

      retcode = 0; // Success!
    } catch (final Exception e) {
      out.print("Edit failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);

    } finally {
      if (srvCA != null) {
        bc.ungetService(refCA);
      }
    }
    return retcode;
  }

  public final static String USAGE_CURRENT = "[-t]";

  public final static String[] HELP_CURRENT =
    new String[] { "Show the currently open configuration.",
                   "-t           Include type for each property in the output."};

  public int cmdCurrent(Dictionary<?, ?> opts,
                        Reader in,
                        PrintWriter out,
                        Session session)
  {
    final boolean printTypes = opts.get("-t") != null;
    final Configuration cfg = getCurrent(session);
    if (cfg == null) {
      out.println("No configuration open currently");
    } else {
      try {
        printConfiguration(out, session, cfg, printTypes);
      } catch (final Exception e) {
        out.print("Current failed. Details:");
        final String reason = e.getMessage();
        out.println(reason == null ? "<unknown>" : reason);
      }
    }
    return 0;
  }

  public final static String USAGE_SAVE = "[-force]";

  public final static String[] HELP_SAVE =
    new String[] { "Save the currently open configuration in the CM.",
                  "-force   Force the save" };

  public int cmdSave(Dictionary<?, ?> opts,
                     Reader in,
                     PrintWriter out,
                     Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    final boolean forceOptionNotSpecified = opts.get("-force") == null;
    ConfigurationAdmin srvCA = null;
    try {
      final Configuration cfg = getCurrent(session);
      if (cfg == null) {
        throw new Exception("No configuration open currently");
      }
      srvCA = getCA();

      if (isEditing(session)) {
        final long oldVersion = getEditingVersion(session);
        final long currentVersion = cfg.getChangeCount();

        if (forceOptionNotSpecified && currentVersion > oldVersion) {
          throw new Exception("The configuration has changed in CM since "
                              + "it was opened. Use -force option if you "
                              + "want to force saving of your changes.");
        }
        cfg.update(getEditingDict(session));
        setEditingDict(session);
      } else {
        throw new Exception("No changes to save");
      }
      retcode = 0; // Success!
    } catch (final Exception e) {
      out.print("Save failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } finally {
      if (srvCA != null) {
        bc.ungetService(refCA);
      }
    }
    return retcode;
  }

  public final static String USAGE_SET = "<property> <value> [<type>]";

  public final static String[] HELP_SET =
    new String[] { "Set a property in the currently open configuration.",
                  "<property> Name of property to set in configuration",
                  "<value>    New value of property",
                  "<type>     Type of value", "Allowed types:",
                  "  String|Integer|Long|Float|Double|Byte|Short|",
                  "  Character|Boolean|BigInteger|BigDecimal" };

  public int cmdSet(Dictionary<?, ?> opts,
                    Reader in,
                    PrintWriter out,
                    Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    try {
      if (getCurrent(session) == null) {
        throw new Exception("No configuration open currently");
      }

      final String p = (String) opts.get("property");
      final String v = (String) opts.get("value");
      final String t = (String) opts.get("type");
      final Dictionary<String, Object> dict = getEditingDict(session);
      final Object ov = dict.get(p);

      if (t == null) {
        if (ov == null) {
          dict.put(p, v);
        } else {
          final Class<? extends Object> ovc = ov.getClass();
          final Object nv = stringToObjectOfClass(v, ovc);
          if (nv == null) {
            throw new Exception(
                                "Unable to convert argument to the same type as old value of property");
          }
          dict.put(p, nv);
        }
      } else {
        Object o = null;
        try {
          o = createValue(t, v);
        } catch (final Exception e) {
          o = null;
        }
        if (o == null) {
          throw new Exception("Unable to convert " + v + " to " + t);
        }
        dict.put(p, o);
      }
      retcode = 0; // Success!
    } catch (final Exception e) {
      out.print("Set failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } finally {
      // bluerg
    }

    return retcode;
  }

  public final static String USAGE_UNSET = "<property>";

  public final static String[] HELP_UNSET =
    new String[] { "Remove a property from the currently open configuration.",
                  "<property> Name of property to remove from the configuration." };

  public int cmdUnset(Dictionary<?, ?> opts,
                      Reader in,
                      PrintWriter out,
                      Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    try {
      if (getCurrent(session) == null) {
        throw new Exception("No configuration open currently");
      }
      final String p = (String) opts.get("property");
      final Dictionary<String, Object> dict = getEditingDict(session);
      final Object o = dict.remove(p);
      if (o == null) {
        throw new Exception("No property named " + p
                            + " in current configuration.");
      }
      retcode = 0; // Success!
    } catch (final Exception e) {
      out.print("Unset failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } finally {

    }

    return retcode;
  }

  public final static String USAGE_IMPORT = "<url>";

  public final static String[] HELP_IMPORT =
    new String[] { "Import configuration data from xml file at url.",
                  "<url>   URL to an xml file containing configuration data" };

  public int cmdImport(Dictionary<?, ?> opts,
                       Reader in,
                       final PrintWriter out,
                       Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try block
    try {
      final String spec = (String) opts.get("url");
      final URL url = new URL(spec);

      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        @Override
        public Object run()
            throws Exception
        {
          ConfigurationAdmin configAdmin = null;
          PushbackReader reader = null;
          try {
            configAdmin = getCA();

            final CMDataReader cmDataReader = new CMDataReader();
            reader =
              new PushbackReader(new BufferedReader(new InputStreamReader(url
                  .openStream(), CMDataReader.ENCODING), 8192), 8);
            final Hashtable<String, Object>[] configs =
              cmDataReader.readCMDatas(reader);

            for (final Hashtable<String, Object> props : configs) {
              final String pid = (String) props.get(CMDataReader.SERVICE_PID);
              final String fpid =
                (String) props.get(CMDataReader.FACTORY_PID);
              Configuration config;
              if (fpid == null) {
                config = configAdmin.getConfiguration(pid, null);
              } else {
                config = configAdmin.createFactoryConfiguration(fpid, null);
              }
              config.update(props);
            }
          } finally {
            if (reader != null) {
              reader.close();
            }
            if (configAdmin != null) {
              bc.ungetService(refCA);
            }
          }
          return null;
        }
      });

      retcode = 0; // Success!
    } catch (final MalformedURLException e) {
      out.print("Could not create URL. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } catch (final PrivilegedActionException pae) {
      out.print("Import failed. Details:");
      final Exception reason = pae.getException();
      // Android don't supply nested exception
      out.println(reason == null ? "<unknown>" : reason.toString());
    } catch (final Exception e) {
      out.print("Import failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } finally {

    }
    return retcode;
  }


  public final static String USAGE_EXPORT =
    "<file> [<selection>] ...";

  public final static String[] HELP_EXPORT =
    new String[] { "Export configurations in xml format to file.",
                  "<file>      Name of the file to write xml for the selected ",
                  "            configurations to.",
                  "<selection> A pid that can contain wildcards '*',",
                  "            or an ldap filter, or an index in output",
                  "            from the latest use of the 'list' command.",
                  "            If no selection is given all existing ",
                  "            configurations will be exported." };

  public int cmdExport(Dictionary<String, ?> opts,
                       Reader in,
                       PrintWriter out,
                       Session session)
  {
    int retcode = 1; // 1 initially not set to 0 until end of try
    PrintWriter pw = null;
    ConfigurationAdmin srvCA = null;
    try {
      srvCA = getCA();

      final String[] selection = (String[]) opts.get("selection");
      Configuration[] cs = null;
      if (selection == null || selection.length == 0) {
        cs = srvCA.listConfigurations(null);
      } else {
        cs = getConfigurations(session, srvCA, selection);
      }

      if (cs == null || cs.length == 0) {
        throw new Exception("No configurations matching selection.");
      }

      final String fileName = (String) opts.get("file");
      final File file =
        new File(fileName.endsWith(".xml") ? fileName : (fileName + ".xml"));
        pw =
          AccessController
              .doPrivileged(new PrivilegedExceptionAction<PrintWriter>() {
                @Override
                public PrintWriter run()
                    throws Exception
                {
                  final OutputStream out = new FileOutputStream(file);
                  final OutputStreamWriter ow =
                    new OutputStreamWriter(out, CMDataReader.ENCODING);
                  return new PrintWriter(ow);
                }
              });
        try {
          CMDataWriter.writeConfigurations(cs, pw);
        } finally {
          pw.close();
        }
      retcode = 0; // Success!
    } catch (final IOException e) {
      out.print("Export failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } catch (final PrivilegedActionException pae) {
      out.print("Export failed. Details:");
      final String reason = pae.getException().getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } catch (final Exception e) {
      out.print("Export failed. Details:");
      final String reason = e.getMessage();
      out.println(reason == null ? "<unknown>" : reason);
    } finally {
      if (srvCA != null) {
        bc.ungetService(refCA);
      }
    }
    return retcode;
  }

  /** Helper method that get the CA service. */
  ConfigurationAdmin getCA()
      throws Exception
  {
    ConfigurationAdmin srvCA = null;
    if (refCA != null) {
      try {
        srvCA =
          AccessController
              .doPrivileged(new PrivilegedExceptionAction<ConfigurationAdmin>() {
                @Override
                public ConfigurationAdmin run()
                    throws Exception
                {
                  return bc.getService(refCA);
                }
              });
      } catch (final PrivilegedActionException pae) {
        // Re-throw wrapped exception
        final Exception ee = pae.getException();
        // Android don't supply nested exception
        if (ee != null) {
          throw ee;
        }
      }
    }
    if (srvCA == null) {
      throw new Exception("CM service is not available");
    }
    return srvCA;
  }

  /***************************************************************************
   * Helper method that gets the current configuration from the session. Returns
   * <code>null</code> if not available.
   **************************************************************************/
  private Configuration getCurrent(Session session)
  {
    return (Configuration) session.getProperties().get(CURRENT);
  }

  /***************************************************************************
   * Helper method that sets the current configuration in the session.
   **************************************************************************/
  private void setSessionProperty(Session session, String key, Object value)
  {
    if (value == null) {
      session.getProperties().remove(key);
    } else {
      session.getProperties().put(key, value);
    }
  }

  /***************************************************************************
   * Helper method that sets the current configuration in the session.
   **************************************************************************/
  private void setCurrent(Session session, Configuration cfg)
  {
    setSessionProperty(session, CURRENT, cfg);
  }

  /***************************************************************************
   * Helper method that returns true if the current configuration is set and its
   * dictionary has been fetched. I.e. it returns true if the EDITING property
   * of the session is set.
   **************************************************************************/
  private boolean isEditing(Session session)
  {
    return session.getProperties().get(EDITED) != null;
  }

  /***************************************************************************
   * Helper method that gets the editing dictionary of the current configuration
   * from the session. Returns a new empty dictionary if current is set but have
   * no dictionary set yet.
   **************************************************************************/
  private Dictionary<String, Object> getEditingDict(Session session)
  {
    @SuppressWarnings("unchecked")
    Dictionary<String, Object> dict =
      (Dictionary<String, Object>) session.getProperties().get(EDITED);
    if (dict == null) {
      final Configuration cfg = getCurrent(session);
      long changeCount = Long.MIN_VALUE;
      if (cfg != null) {
        changeCount = cfg.getChangeCount();
        dict = cfg.getProperties();
      }
      if (dict == null) {
        dict = new Hashtable<String, Object>();
      }
      setEditingDict(session, dict, changeCount);
    }
    return dict;
  }

  /***************************************************************************
   * Helper method that gets the editing version of the current configuration
   * from the session.
   **************************************************************************/
  private long getEditingVersion(Session session)
  {
    final Long version = (Long) session.getProperties().get(EDITED_VERSION);
    return (version == null) ? Long.MIN_VALUE : version.longValue();
  }

  private Configuration[] getConfigurations(Session session,
                                            ConfigurationAdmin cm,
                                            String[] selection)
      throws Exception
  {
    final Filter[] filters = convertToFilters(session, selection);
    return getConfigurationsMatchingFilters(cm, filters);
  }

  private Configuration[] getConfigurations(Session session,
                                            ConfigurationAdmin cm,
                                            String selection)
      throws Exception
  {
    return getConfigurations(session, cm, new String[] { selection });
  }

  private Filter[] convertToFilters(Session session, String[] selection)
      throws Exception
  {
    if (selection == null) {
      return null;
    }
    final Filter[] filters = new Filter[selection.length];
    for (int i = 0; i < selection.length; ++i) {
      final String current = selection[i];
      Filter filter = null;
      if (isInteger(current)) {
        filter = tryToCreateFilterFromIndex(session, current);
      } else if (startsWithParenthesis(current)) {
        filter = tryToCreateFilterFromLdapExpression(current);
      } else {
        filter = tryToCreateFilterFromPidContainingWildcards(current);
      }
      if (filter == null) {
        throw new Exception("Unable to handle selection argument " + current);
      }
      filters[i] = filter;
    }
    return filters;
  }

  private boolean isInteger(String possiblyAnInteger)
  {
    try {
      Integer.parseInt(possiblyAnInteger);
    } catch (final NumberFormatException e) {
      return false;
    }
    return true;
  }

  private boolean startsWithParenthesis(String selection)
  {
    return selection.startsWith("(");
  }

  private Filter tryToCreateFilterFromIndex(Session session, String index)
      throws Exception
  {
    final String pid = getPidWithIndexInLastList(session, index);
    return tryToCreateFilterFromPidContainingWildcards(pid);
  }

  private Filter tryToCreateFilterFromPidContainingWildcards(String pidContainingWildcards)
      throws Exception
  {
    return tryToCreateFilterFromLdapExpression("(" + Constants.SERVICE_PID
                                               + "=" + pidContainingWildcards
                                               + ")");
  }

  private Filter tryToCreateFilterFromLdapExpression(String ldapExpression)
      throws Exception
  {
    return bc.createFilter(ldapExpression);
  }

  private String getPidWithIndexInLastList(Session session, String index)
      throws Exception
  {
    final Configuration[] cs =
      (Configuration[]) session.getProperties().get(LISTED_CONFIGS);
    if (cs == null) {
      throw new Exception(
                          "The 'list' command has not been used yet to create a list.");
    }
    if (cs.length == 0) {
      throw new Exception("No configurations listed by latest 'list' call.");
    }
    final int i = Integer.parseInt(index);
    if (i < 0 || cs.length <= i) {
      throw new Exception("Invalid index."
                          + ((cs.length == 1)
                            ? "0 is the only valid index."
                            : ("Valid indices are 0 to " + (cs.length - 1))));
    }

    final String pid = cs[i].getPid();
    if (pid == null) {
      throw new Exception("Unable to retrieve pid with index " + index
                          + " from last 'list'.");
    }

    return pid;
  }

  /**
   * Find the index of a PID from the last call to the list command.
   *
   * @param session
   *          the console session to fetch last list result from.
   * @param pid
   *          the PID that we are looking for.
   * @return the index of the specified PID or -1 if not present in the list.
   * @throws Exception
   */
  private int getIndexOfPidInLastList(Session session, String pid)
      throws Exception
  {
    int res = -1;
    @SuppressWarnings("unchecked")
    final Map<String, Integer> pidToIndex =
      (Map<String, Integer>) session.getProperties()
          .get(LISTED_CONFIGS_PID_TO_INDEX);
    final Integer ix = pidToIndex.get(pid);
    if (ix != null) {
      res = ix.intValue();
    }
    return res;
  }

  private Configuration[] getConfigurationsMatchingFilters(ConfigurationAdmin cm,
                                                           Filter[] filters)
      throws Exception
  {
    final Configuration[] cs = cm.listConfigurations(null);
    if (cs == null || cs.length == 0) {
      return new Configuration[0];
    }
    if (filters == null || filters.length == 0) {
      return cs;
    }

    final Vector<Configuration> matching = new Vector<Configuration>();
    for (final Configuration element : cs) {
      for (final Filter filter : filters) {
        if (filter.match(element.getProperties())) {
          matching.addElement(element);
          break;
        }
      }
    }

    final Configuration[] result = new Configuration[matching.size()];
    matching.copyInto(result);
    return result;
  }

  /***************************************************************************
   * Helper method that clears the editing dictionary in the session.
   *
   * @param session
   *          the console session to update.
   **************************************************************************/
  private void setEditingDict(Session session)
  {
    setEditingDict(session, null, Long.MIN_VALUE);
  }

  /***************************************************************************
   * Helper method that sets the editing dictionary of the current configuration
   * in the session.
   *
   * @param session
   *          the console session to update.
   * @param dict
   *          the configuration dictionary to edit.
   * @param version
   *          The change count from the configuration.
   **************************************************************************/
  private void setEditingDict(Session session, Dictionary<String, Object> dict, long version)
  {
    if (dict == null) {
      session.getProperties().remove(EDITED);
      session.getProperties().remove(EDITED_VERSION);
    } else {
      session.getProperties().put(EDITED, dict);
      session.getProperties().put(EDITED_VERSION, new Long(version));
    }
  }

  private void printConfiguration(PrintWriter out,
                                  Session session,
                                  final Configuration cfg,
                                  boolean printTypes)
      throws Exception
  {
    final String pid = cfg.getPid();
    final int listIndex = getIndexOfPidInLastList(session, pid);
    out.print('[');
    out.print(listIndex > -1 ? String.valueOf(listIndex) : "-" );
    out.print("] ");
    out.println(pid);

    final String factoryPid = cfg.getFactoryPid();
    if (factoryPid != null) {
      out.print(" factory PID: ");
      out.println(factoryPid);
    }

    out.print(" location: ");
    final String location = cfg.getBundleLocation();
    out.println(location != null ? location : "-");

    out.print(" change count: ");
    out.println(cfg.getChangeCount());

    final Dictionary<String, Object> d = cfg.getProperties();
    out.println(" properties:");
    if (d == null) {
      out.println("  -");
    } else {
      printDictionary(out, d, printTypes);
    }
  }

  private void printDictionary(PrintWriter out,
                               Dictionary<String, Object> d,
                               boolean printTypes)
  {
    final String[] keyNames = new String[d.size()];
    int i = 0;
    for (final Enumeration<String> keys = d.keys(); keys.hasMoreElements();) {
      keyNames[i++] = keys.nextElement();
    }
    Sort.sortStringArray(keyNames);
    for (i = 0; i < keyNames.length; i++) {
      out.print("  ");
      out.print(keyNames[i]);

      final Object value = d.get(keyNames[i]);
      if (printTypes) {
        out.print(":");
        printValueType(out, value);
      }

      out.print("= ");
      printValue(out, value);
      out.println();
    }
  }

  private void printValueType(PrintWriter out, Object value)
  {
    try {
      if (value == null) {
        out.print('?');
      } else {
        final Class<?> c = value.getClass();
        if (c == String.class) {
          out.print("String");
        } else if (c == Integer.class) {
          out.print("Integer");
        } else if (c == Long.class) {
          out.print("Long");
        } else if (c == Float.class) {
          out.print("Float");
        } else if (c == Double.class) {
          out.print("Double");
        } else if (c == Byte.class) {
          out.print("Byte");
        } else if (c == Short.class) {
          out.print("Short");
        } else if (c == BigInteger.class) {
          out.print("BigInteger");
        } else if (classBigDecimal != null && c == classBigDecimal) {
          out.print("BigDecimal");
        } else if (c == Character.class) {
          out.print("Character");
        } else if (c == Boolean.class) {
          out.print("Boolean");
        } else if (c.isArray()) {
          out.print("[");
          printValueType(out, Array.get(value, 0));
          out.print("]");
        } else if (c.isInstance(Vector.class)) {
          final Vector<?> v = (Vector<?>) value;
          out.print("{");
          printValueType(out, v.elementAt(0));
          out.print("}");
        }
      }
    } catch (final Exception ignored) {
      out.print('?');
    }
  }

  private void printValue(PrintWriter out, Object val)
  {
    if (val instanceof Vector) {
      final Vector<?> v = (Vector<?>) val;
      out.print("{");
      for (int i = 0; i < v.size(); i++) {
        if (i > 0) {
          out.print(", ");
        }
        printValue(out, v.elementAt(i));
      }
      out.print("}");
    } else if (val.getClass().isArray()) {
      final int length = Array.getLength(val);
      out.print("[");
      for (int i = 0; i < length; i++) {
        if (i > 0) {
          out.print(", ");
        }
        printValue(out, Array.get(val, i));
      }
      out.print("]");
    } else {
      out.print(val.toString());
    }
  }

  private Object stringToObjectOfClass(String str, Class<? extends Object> c)
  {
    if (str == null) {
      return null;
    }
    Object o = null;
    try {
      if (c == null || c == String.class) {
        o = str;
      } else if (str.length() == 0) {
        // None of the other Classes can handle a zero length String
        o = null;
      } else if (c == Integer.class) {
        return new Integer(str);
      } else if (c == Long.class) {
        o = new Long(str);
      } else if (c == Float.class) {
        o = new Float(str);
      } else if (c == Double.class) {
        o = new Double(str);
      } else if (c == Byte.class) {
        o = new Byte(str);
      } else if (c == Short.class) {
        o = new Short(str);
      } else if (c == BigInteger.class) {
        o = new BigInteger(str);
      } else if (classBigDecimal != null && c == classBigDecimal) {
        if (consBigDecimal != null) {
          o = consBigDecimal.newInstance(new Object[] { str });
        } else {
          o = null;
        }
      } else if (c == Character.class) {
        o = new Character(str.charAt(0));
      } else if (c == Boolean.class) {
        o = new Boolean(str);
      } else {
        o = null;
      }
    } catch (final Exception ignored) {
      o = null;
    }
    return o;
  }

  Object createValue(String type, String def)
  {
    def = def.equals("") ? null : def;
    if (type.equals("String")) {
      return def == null ? new String() : new String(def);
    } else if (type.equals("Integer")) {
      return def == null ? new Integer(0) : new Integer(def);
    } else if (type.equals("Long")) {
      return def == null ? new Long(0) : new Long(def);
    } else if (type.equals("Float")) {
      return def == null ? new Float(0) : new Float(def);
    } else if (type.equals("Double")) {
      return def == null ? new Double(0) : new Double(def);
    } else if (type.equals("Byte")) {
      return def == null ? new Byte("0") : new Byte(def);
    } else if (type.equals("Short")) {
      return def == null ? new Short("0") : new Short(def);
    } else if (type.equals("BigInteger")) {
      return def == null ? new BigInteger("0") : new BigInteger(def);
    } else if (type.equals("BigDecimal")) {
      Object o = null;
      if (classBigDecimal != null && consBigDecimal != null) {
        def = def == null ? "0" : def;
        try {
          o = consBigDecimal.newInstance(new Object[] { def });
        } catch (final Exception ignored) {
          o = null;
        }
      }
      return o;
    } else if (type.equals("Character")) {
      return def == null ? new Character('a') : new Character(def.charAt(0));
    } else if (type.equals("Boolean")) {
      return def == null ? new Boolean(false) : new Boolean(def);
    } else {
      // Unsupported type

      return null;
    }
  }

  // ////////////////
  private static void sortConfigurationArray(Configuration[] a)
  {
    sortConfigurationArray(a, 0, a.length);
  }

  private static void sortConfigurationArray(Configuration[] a,
                                             int fromIndex,
                                             int toIndex)
  {
    int middle;
    if (a == null) {
      return;
    }

    if (fromIndex + 1 < toIndex) {
      middle = (fromIndex + toIndex) / 2;
      sortConfigurationArray(a, fromIndex, middle);
      sortConfigurationArray(a, middle, toIndex);
      mergeConfigurationArray(a, fromIndex, toIndex);
    }
  }

  private static void mergeConfigurationArray(Configuration[] a,
                                              int fromIndex,
                                              int toIndex)
  {
    int i, j, k, middle, n;
    n = toIndex - fromIndex;
    final Configuration[] b = new Configuration[n]; // temporary array

    k = 0;
    middle = (fromIndex + toIndex) / 2;

    // Copy lower half to array b
    for (i = fromIndex; i < middle; i++) {
      b[k++] = a[i];
    }
    // Copy upper half to array b in opposite order
    for (j = toIndex - 1; j >= middle; j--) {
      b[k++] = a[j];
    }

    i = 0;
    j = n - 1;
    k = fromIndex;

    // Copy back next-greatest element at each time
    // until i and j cross
    while (i <= j) {
      if (b[i].getPid().compareTo(b[j].getPid()) < 0) {
        a[k++] = b[i++];
      } else {
        a[k++] = b[j--];
      }
    }
  }

  // //////////////////////////////////
  @Override
  public void serviceChanged(ServiceEvent event)
  {
    switch (event.getType()) {
    case ServiceEvent.REGISTERED:
      @SuppressWarnings("unchecked")
      final ServiceReference<ConfigurationAdmin> sr =
        (ServiceReference<ConfigurationAdmin>) event.getServiceReference();
      if (refCA != sr) {
        refCA = sr;
      }
      break;
    case ServiceEvent.MODIFIED:
      break;
    case ServiceEvent.UNREGISTERING:
      if (refCA != null) {
        refCA = null;
      }
      break;
    default:
      break;
    }
  }
}
