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

package org.knopflerfish.bundle.cm.commands.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Hashtable;
import java.util.Vector;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.shared.cm.CMDataReader;
import org.knopflerfish.shared.cm.DictionaryUtils;
import org.knopflerfish.util.sort.Sort;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

// ******************** CMCommands ********************
/**
 * * Console interface to the CM. * *
 * 
 * @author Per Gustafson *
 * @version $Id: CMCommands.java,v 1.1.1.1 2004/03/05 20:34:54 wistrand Exp $
 */

public class CMCommands extends CommandGroupAdapter implements ServiceListener {
    /***************************************************************************
     * Key in the session properties dictionary used to store the current (open)
     * configuration.*
     **************************************************************************/
    private static final String CURRENT = "org.knopflerfish.bundle.cm.commands.impl.current";

    /***************************************************************************
     * Key in the session properties dictionary used to store the dictionary
     * that is edited for the current configuration.*
     **************************************************************************/
    private static final String EDITED = "org.knopflerfish.bundle.cm.commands.impl.edited";

    /***************************************************************************
     * Key in the session properties dictionary used to store the * result of
     * the latest list command for later reference using -i options to several
     * commands. *
     **************************************************************************/
    private static final String LISTED_CONFIGS = "org.knopflerfish.bundle.cm.commands.impl.listed.configs";

    BundleContext bc;

    ServiceReference refCA = null;

    private static Class classBigDecimal;

    private static Constructor consBigDecimal;

    static {
        try {
            classBigDecimal = Class.forName("java.math.BigDecimal");
            consBigDecimal = classBigDecimal
                    .getConstructor(new Class[] { String.class });
        } catch (Exception ignore) {
            classBigDecimal = null;
            consBigDecimal = null;
        }
    }

    public CMCommands(BundleContext bc) {
        super("configuration", "Configuration commands");
        this.bc = bc;
        refCA = bc.getServiceReference(ConfigurationAdmin.class.getName());
        try {
            bc.addServiceListener(this, "(objectClass="
                    + ConfigurationAdmin.class.getName() + ")");
        } catch (InvalidSyntaxException ignored) {
        }
    }

    public final static String USAGE_LIST = "[<selection>] ...";

    public final static String[] HELP_LIST = new String[] {
            "List the pids of existing configurations.",
            "<selection>  A pid that can contain wildcards '*',",
            "             or an ldap filter, or an index in output",
            "             from the latest use of this command.",
            "             If no selection is given all existing pids",
            "             will be listed." };

    public int cmdList(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        ConfigurationAdmin srvCA = null;
        try {
            srvCA = getCA();

            String[] selection = (String[]) opts.get("selection");

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
                out.println("Available configurations:");
                for (int i = 0; i < cs.length; ++i) {
                    out.println("[" + i + "] " + cs[i].getPid());
                }
            }

            retcode = 0; // Success!
        } catch (Exception e) {
            out.println("List failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);

        } finally {
            if (srvCA != null) {
                bc.ungetService(refCA);
            }
        }
        return retcode;
    }

    public final static String USAGE_SHOW = "[<selection>] ...";

    public final static String[] HELP_SHOW = new String[] {
            "Show the saved versions of configurations.",
            "<selection>  A pid that can contain wildcards '*',",
            "             or an ldap filter, or an index in output",
            "             from the latest use of the 'list' command.",
            "             If no selection is given all configurations",
            "             will be shown.",
            "             Use 'current' command to see the properties",
            "             of the currently edited configuration." };

    public int cmdShow(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        ConfigurationAdmin srvCA = null;

        try {
            srvCA = getCA();
            String[] selection = (String[]) opts.get("selection");
            Configuration[] cs = getConfigurations(session, srvCA, selection);
            if (cs == null || cs.length == 0) {
                throw new Exception("No matching configurations for selection.");
            }
            for (int i = 0; i < cs.length; ++i) {
                if (i > 0) {
                    out.println();
                }
                Dictionary d = cs[i].getProperties();
                if (d == null) {
                    out.println("No properties set in " + cs[i].getPid());
                } else {
                    out.println("Properties for " + cs[i].getPid());
                    printDictionary(out, d);
                }
            }
        } catch (Exception e) {
            out.println("Show failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);
        } finally {
            if (srvCA != null) {
                bc.ungetService(refCA);
            }
        }
        return 0;
    }

    public final static String USAGE_CREATE = "[-f] <pid>";

    public final static String[] HELP_CREATE = new String[] {
            "Create a configuration and open it for editing.",
            "-f     If specified the pid argument is a factory pid.",
            "<pid>  Pid or factory pid of configuration to create",
            "       depending on if -f flag is specified." };

    public int cmdCreate(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        setCurrent(session, null);
        setEditingDict(session, null);
        ConfigurationAdmin srvCA = null;
        try {
            srvCA = getCA();
            String pid = (String) opts.get("pid");
            boolean createFactoryConfiguration = opts.get("-f") != null;
            Configuration cfg = null;
            if (createFactoryConfiguration) {
                cfg = srvCA.createFactoryConfiguration(pid, null);
            } else {
                cfg = srvCA.getConfiguration(pid, null);
            }

            if (cfg == null) {
                throw new Exception("Failed creating configuration for " + pid);
            }
            setCurrent(session, cfg);
            retcode = 0; // Success!
        } catch (Exception e) {
            out.println("Create failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);

        } finally {
            if (srvCA != null) {
                bc.ungetService(refCA);
            }
        }
        return retcode;
    }

    public final static String USAGE_DELETE = "<selection>";

    public final static String[] HELP_DELETE = new String[] {
            "Delete an existing configuration.",
            "<selection>  A pid that can contain wildcards '*',",
            "             or an ldap filter, or an index in output",
            "             from the latest use of the 'list' command.",
            "             If the selection doesn't match exactly one",
            "             configuration it will have to be refined." };

    public int cmdDelete(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        ConfigurationAdmin srvCA = null;
        try {
            String selection = (String) opts.get("selection");

            srvCA = getCA();

            Configuration[] cs = getConfigurations(session, srvCA, selection);
            if (cs == null || cs.length == 0) {
                throw new Exception(
                        "Selection didn't match any configurations. "
                                + "Change your selection to match exactly "
                                + "one configuration.");
            } else if (cs.length == 1) {
                out.println("Deleting " + cs[0].getPid());
                Configuration current = getCurrent(session);
                if (current != null && current.getPid().equals(cs[0].getPid())) {
                    setCurrent(session, null);
                    setEditingDict(session, null);
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
        } catch (Exception e) {
            out.println("Delete failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);

        } finally {
            if (srvCA != null) {
                bc.ungetService(refCA);
            }
        }
        return retcode;
    }

    public final static String USAGE_EDIT = "<selection>";

    public final static String[] HELP_EDIT = new String[] {
            "Edit an existing configuration.",
            "<selection>  A pid that can contain wildcards '*',",
            "             or an ldap filter, or an index in output",
            "             from the latest use of the 'list' command.",
            "             If the selection doesn't match exactly one",
            "             configuration it will have to be refined." };

    public int cmdEdit(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        setEditingDict(session, null);
        setCurrent(session, null);

        ConfigurationAdmin srvCA = null;
        try {
            String selection = (String) opts.get("selection");

            srvCA = getCA();

            Configuration[] cs = getConfigurations(session, srvCA, selection);
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
        } catch (Exception e) {
            out.println("Edit failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);

        } finally {
            if (srvCA != null) {
                bc.ungetService(refCA);
            }
        }
        return retcode;
    }

    public final static String USAGE_CURRENT = "";

    public final static String[] HELP_CURRENT = new String[] { "Show the currently open configuration." };

    public int cmdCurrent(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        Configuration cfg = getCurrent(session);
        if (cfg == null) {
            out.println("No configuration open currently");
        } else {
            if (isEditing(session)) {
                printDictionary(out, getEditingDict(session));
            } else {
                Dictionary d = cfg.getProperties();
                if (d == null) {
                    out.println("No properties set in current configuration");
                } else {
                    printDictionary(out, d);
                }
            }
        }
        return 0;
    }

    public final static String USAGE_SAVE = "[-force]";

    public final static String[] HELP_SAVE = new String[] {
            "Save the currently open configuration in the CM.",
            "-force   Force the save" };

    public int cmdSave(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        boolean forceOptionNotSpecified = opts.get("-force") == null;
        ConfigurationAdmin srvCA = null;
        try {
            Configuration cfg = getCurrent(session);
            if (cfg == null) {
                throw new Exception("No configuration open currently");
            }
            srvCA = getCA();

            if (forceOptionNotSpecified && configurationHasChanged(srvCA, cfg)) {
                throw new Exception(
                        "The configuration has changed in CM since it was opened."
                                + "Use -force option if you want to force saving of your changes.");
            }

            if (isEditing(session)) {
                cfg.update(getEditingDict(session));
                setEditingDict(session, null);
            } else {
                throw new Exception("No changes to save");
            }
            retcode = 0; // Success!
        } catch (Exception e) {
            out.println("Save failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);
        } finally {
            if (srvCA != null) {
                bc.ungetService(refCA);
            }
        }
        return retcode;
    }

    public final static String USAGE_SET = "<property> <value> [<type>]";

    public final static String[] HELP_SET = new String[] {
            "Set a property in the currently open configuration.",
            "<property> Name of property to set in configuration",
            "<value>    New value of property", "<type>     Type of value",
            "Allowed types:", "  String|Integer|Long|Float|Double|Byte|Short|",
            "  Character|Boolean|BigInteger|BigDecimal" };

    public int cmdSet(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        try {
            if (getCurrent(session) == null)
                throw new Exception("No configuration open currently");

            String p = (String) opts.get("property");
            String v = (String) opts.get("value");
            String t = (String) opts.get("type");
            Dictionary dict = getEditingDict(session);
            Object ov = dict.get(p);

            if (t == null) {
                if (ov == null) {
                    dict.put(p, v);
                } else {
                    Class ovc = ov.getClass();
                    Object nv = stringToObjectOfClass(v, ovc);
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
                } catch (Exception e) {
                    o = null;
                }
                if (o == null) {
                    throw new Exception("Unable to convert " + v + " to " + t);
                }
                dict.put(p, o);
            }
            retcode = 0; // Success!
        } catch (Exception e) {
            out.println("Set failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);
        } finally {
            // bluerg
        }

        return retcode;
    }

    public final static String USAGE_UNSET = "<property>";

    public final static String[] HELP_UNSET = new String[] {
            "Remove a property from the currently open configuration.",
            "<property> Name of property to remove from the configuration." };

    public int cmdUnset(Dictionary opts, Reader in, PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        try {
            if (getCurrent(session) == null) {
                throw new Exception("No configuration open currently");
            }
            String p = (String) opts.get("property");
            Dictionary dict = getEditingDict(session);
            Object o = dict.remove(p);
            if (o == null) {
                throw new Exception("No property named " + p
                        + " in current configuration.");
            }
            retcode = 0; // Success!
        } catch (Exception e) {
            out.println("Unset failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);
        } finally {

        }

        return retcode;
    }

    public final static String USAGE_IMPORT = "<url>";

    public final static String[] HELP_IMPORT = new String[] {
            "Import configuration data from xml file at url.",
            "<url>   URL to an xml file containing configuration data" };

    public int cmdImport(Dictionary opts, Reader in, final PrintWriter out,
            Session session) {
        int retcode = 1; // 1 initially not set to 0 until end of try block
        try {
            final String spec = (String) opts.get("url");
            final URL url = new URL(spec);
            if (url == null) {
                throw new Exception("URL Object construction failed");
            }

            AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    ConfigurationAdmin configAdmin = null;
                    PushbackReader reader = null;
                    try {
                        configAdmin = getCA();

                        CMDataReader cmDataReader = new CMDataReader();
                        reader = new PushbackReader(new BufferedReader(
                                new InputStreamReader(url.openStream(),
                                        CMDataReader.ENCODING), 8192), 8);
                        Hashtable[] configs = cmDataReader.readCMDatas(reader);

                        for (int i = 0; i < configs.length; i++) {
                            String pid = (String) configs[i]
                                    .get(CMDataReader.SERVICE_PID);
                            String fpid = (String) configs[i]
                                    .get(CMDataReader.FACTORY_PID);
                            Configuration config;
                            if (fpid == null) {
                                config = configAdmin
                                        .getConfiguration(pid, null);
                            } else {
                                config = configAdmin
                                        .createFactoryConfiguration(fpid, null);
                            }
                            if (config.getBundleLocation() != null) {
                                config.setBundleLocation(null);
                            }
                            if (configs[i].get("service.bundleLocation") != null) {
                                configs[i].remove("service.bundleLocation");
                            }
                            config.update(configs[i]);
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
        } catch (MalformedURLException e) {
            out.println("Could not create URL. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);
        } catch (IOException e) {
            out.println("Import failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);
        } catch (PrivilegedActionException pae) {
            out.println("Import failed. Details:");
            String reason = pae.getException().toString();
            out.println(reason == null ? "<unknown>" : reason);
        } catch (Exception e) {
            out.println("Import failed. Details:");
            String reason = e.getMessage();
            out.println(reason == null ? "<unknown>" : reason);
        } finally {

        }
        return retcode;
    }

    /*
     * public final static String USAGE_EXPORT = "[-template] <file> [<selection>]
     * ...";
     * 
     * public final static String[] HELP_EXPORT = new String[] { "Export
     * configuration data in xml format to a file.", "-template If the output
     * should be a template.", "<file> Path to file to write xml to.", "<selection>
     * A pid that can contain wildcards '*',", " or an ldap filter, or an index
     * in output", " from the latest use of the 'list' command.", " If no
     * selection is given all existing ", " configurations will be exported." };
     * 
     * public int cmdExport(Dictionary opts, Reader in, PrintWriter out, Session
     * session) { int retcode = 1; // 1 initially not set to 0 until end of try
     * block OutputStream os = null; ConfigurationAdmin srvCA = null; try {
     * srvCA = getCA();
     * 
     * boolean isTemplate = opts.get("-template") != null; String[] selection =
     * (String[]) opts.get("selection"); boolean deleteAllOldConfigs = false;
     * Configuration[] cs = null; if (selection == null || selection.length ==
     * 0) { deleteAllOldConfigs = true; cs = srvCA.listConfigurations(null); }
     * else { cs = getConfigurations(session, srvCA, selection); }
     * 
     * if (cs == null || cs.length == 0) { throw new Exception("No
     * configurations matching selection."); }
     * 
     * final String fileName = (String) opts.get("file"); final File f = new
     * File(fileName); os = (FileOutputStream) AccessController
     * .doPrivileged(new PrivilegedExceptionAction() { public Object run()
     * throws Exception { return new FileOutputStream(f); } }); try {
     * CMDataManager.exportCMData(cs, deleteAllOldConfigs, isTemplate, srvCA,
     * os); } finally { os.close(); } retcode = 0; // Success! } catch
     * (IOException e) { out.println("Export failed. Details:"); String reason =
     * e.getMessage(); out.println(reason == null ? "<unknown>" : reason); }
     * catch (PrivilegedActionException pae) { out.println("Export failed.
     * Details:"); String reason = pae.getException().getMessage();
     * out.println(reason == null ? "<unknown>" : reason); } catch (Exception
     * e) { out.println("Export failed. Details:"); String reason =
     * e.getMessage(); out.println(reason == null ? "<unknown>" : reason); }
     * finally { if (srvCA != null) { bc.ungetService(refCA); } } return
     * retcode; }
     */

    /** Helper method that get the CA service. */
    ConfigurationAdmin getCA() throws Exception {
        ConfigurationAdmin srvCA = null;
        if (refCA == null) {
            throw new Exception("CM service is not available");
        }
        try {
            srvCA = (ConfigurationAdmin) AccessController
                    .doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() throws Exception {
                            return bc.getService(refCA);
                        }
                    });
        } catch (PrivilegedActionException pae) {
            // Rethrow wrapped exception
            throw pae.getException();
        }
        if (srvCA == null) {
            throw new Exception("CM service is not available");
        }
        return srvCA;
    }

    /***************************************************************************
     * Helper method that gets the current configuration from the session.
     * Returns <code>null</code> if not availble.*
     **************************************************************************/
    private Configuration getCurrent(Session session) {
        return (Configuration) session.getProperties().get(CURRENT);
    }

    /***************************************************************************
     * Helper method that sets the current configuration in the session.*
     **************************************************************************/
    private void setSessionProperty(Session session, String key, Object value) {
        if (value == null) {
            session.getProperties().remove(key);
        } else {
            session.getProperties().put(key, value);
        }
    }

    /***************************************************************************
     * Helper method that sets the current configuration in the session.*
     **************************************************************************/
    private void setCurrent(Session session, Configuration cfg) {
        setSessionProperty(session, CURRENT, cfg);
    }

    /***************************************************************************
     * Helper method that returns true if the current configuration is * set and
     * its dictionary has been fetched. I.e. it returns true if the EDITING
     * property of the session is set.*
     **************************************************************************/
    private boolean isEditing(Session session) {
        return session.getProperties().get(EDITED) != null;
    }

    /***************************************************************************
     * Helper method that gets the editing dictionary of the current *
     * configuration from the session. Returns a new empty dictionary if current
     * is set but have no dictionary set yet.*
     **************************************************************************/
    private Dictionary getEditingDict(Session session) {
        Dictionary dict = (Dictionary) session.getProperties().get(EDITED);
        if (dict == null) {
            Configuration cfg = getCurrent(session);
            if (cfg != null) {
                dict = cfg.getProperties();
            }
            if (dict == null) {
                dict = new Hashtable();
            }
            setEditingDict(session, dict);
        }
        return dict;
    }

    private boolean configurationHasChanged(ConfigurationAdmin ca,
            Configuration c1) throws Exception {
        String pid = c1.getPid();
        Configuration c2 = ca.getConfiguration(pid, null);
        return DictionaryUtils.dictionariesAreNotEqual(c1.getProperties(), c2
                .getProperties());
    }

    private Configuration[] getConfigurations(Session session,
            ConfigurationAdmin cm, String[] selection) throws Exception {
        Filter[] filters = convertToFilters(session, selection);
        return getConfigurationsMatchingFilters(cm, filters);
    }

    private Configuration[] getConfigurations(Session session,
            ConfigurationAdmin cm, String selection) throws Exception {
        return getConfigurations(session, cm, new String[] { selection });
    }

    private Filter[] convertToFilters(Session session, String[] selection)
            throws Exception {
        if (selection == null) {
            return null;
        }
        Filter[] filters = new Filter[selection.length];
        for (int i = 0; i < selection.length; ++i) {
            String current = selection[i];
            Filter filter = null;
            if (isInteger(current)) {
                filter = tryToCreateFilterFromIndex(session, current);
            } else if (startsWithParenthesis(current)) {
                filter = tryToCreateFilterFromLdapExpression(current);
            } else {
                filter = tryToCreateFilterFromPidContainingWildcards(current);
            }
            if (filter == null) {
                throw new Exception("Unable to handle selection argument "
                        + current);
            }
            filters[i] = filter;
        }
        return filters;
    }

    private boolean isInteger(String possiblyAnInteger) {
        try {
            Integer.parseInt(possiblyAnInteger);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean startsWithParenthesis(String selection) {
        return selection.startsWith("(");
    }

    private Filter tryToCreateFilterFromIndex(Session session, String index)
            throws Exception {
        String pid = getPidWithIndexInLastList(session, index);
        return tryToCreateFilterFromPidContainingWildcards(pid);
    }

    private Filter tryToCreateFilterFromPidContainingWildcards(
            String pidContainingWildcards) throws Exception {
        return tryToCreateFilterFromLdapExpression("(" + Constants.SERVICE_PID
                + "=" + pidContainingWildcards + ")");
    }

    private Filter tryToCreateFilterFromLdapExpression(String ldapExpression)
            throws Exception {
        return bc.createFilter(ldapExpression);
    }

    private String getPidWithIndexInLastList(Session session, String index)
            throws Exception {
        Configuration[] cs = (Configuration[]) session.getProperties().get(
                LISTED_CONFIGS);
        if (cs == null) {
            throw new Exception(
                    "The 'list' command has not been used yet to create a list.");
        }
        if (cs.length == 0) {
            throw new Exception(
                    "No configurations listed by latest 'list' call.");
        }
        int i = Integer.parseInt(index);
        if (i < 0 || cs.length <= i) {
            throw new Exception("Invalid index."
                    + ((cs.length == 1) ? "0 is the only valid index."
                            : ("Valid indices are 0 to " + (cs.length - 1))));
        }

        String pid = cs[i].getPid();
        if (pid == null) {
            throw new Exception("Unable to retrieve pid with index " + index
                    + " from last 'list'.");
        }

        return pid;
    }

    private Configuration[] getConfigurationsMatchingFilters(
            ConfigurationAdmin cm, Filter[] filters) throws Exception {
        Configuration[] cs = cm.listConfigurations(null);
        if (cs == null || cs.length == 0) {
            return new Configuration[0];
        }
        if (filters == null || filters.length == 0) {
            return cs;
        }

        Vector matching = new Vector();
        for (int i = 0; i < cs.length; ++i) {
            for (int j = 0; j < filters.length; ++j) {
                if (filters[j].match(cs[i].getProperties())) {
                    matching.addElement(cs[i]);
                    break;
                }
            }
        }

        Configuration[] result = new Configuration[matching.size()];
        matching.copyInto(result);
        return result;
    }

    /***************************************************************************
     * Helper method that sets the editing dictionary of the current
     * configuration in the session.*
     **************************************************************************/
    private void setEditingDict(Session session, Dictionary dict) {
        if (dict == null)
            session.getProperties().remove(EDITED);
        else
            session.getProperties().put(EDITED, dict);
    }

    private void printDictionary(PrintWriter out, Dictionary d) {
        String[] keyNames = new String[d.size()];
        int i = 0;
        for (Enumeration keys = d.keys(); keys.hasMoreElements();) {
            keyNames[i++] = (String) keys.nextElement();
        }
        Sort.sortStringArray(keyNames);
        for (i = 0; i < keyNames.length; i++) {
            out.print(" ");
            out.print(keyNames[i]);
            out.print(": ");
            printValue(out, d.get(keyNames[i]));
            out.println();
        }
    }

    private void printValue(PrintWriter out, Object val) {
        if (val instanceof Vector) {
            Vector v = (Vector) val;
            out.print("{");
            for (int i = 0; i < v.size(); i++) {
                if (i > 0)
                    out.print(", ");
                printValue(out, v.elementAt(i));
            }
            out.print("}");
        } else if (val.getClass().isArray()) {
            int length = Array.getLength(val);
            out.print("[");
            for (int i = 0; i < length; i++) {
                if (i > 0)
                    out.print(", ");
                printValue(out, Array.get(val, i));
            }
            out.print("]");
        } else {
            out.print(val.toString());
        }
    }

    private Object stringToObjectOfClass(String str, Class c) {
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
        } catch (Exception ignored) {
            o = null;
        }
        return o;
    }

    Object createValue(String type, String def) {
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
                } catch (Exception ignored) {
                    o = null;
                }
            }
            return o;
        } else if (type.equals("Character")) {
            return def == null ? new Character('a') : new Character(def
                    .charAt(0));
        } else if (type.equals("Boolean")) {
            return def == null ? new Boolean(false) : new Boolean(def);
        } else {
            // Unsupported type
            return null;
        }
    }

    // ////////////////
    private static void sortConfigurationArray(Configuration[] a) {
        sortConfigurationArray(a, 0, a.length);
    }

    private static void sortConfigurationArray(Configuration[] a,
            int fromIndex, int toIndex) {
        int middle;
        if (a == null)
            return;

        if (fromIndex + 1 < toIndex) {
            middle = (fromIndex + toIndex) / 2;
            sortConfigurationArray(a, fromIndex, middle);
            sortConfigurationArray(a, middle, toIndex);
            mergeConfigurationArray(a, fromIndex, toIndex);
        }
    }

    private static void mergeConfigurationArray(Configuration[] a,
            int fromIndex, int toIndex) {
        int i, j, k, middle, n;
        n = toIndex - fromIndex;
        Configuration[] b = new Configuration[n]; // temporary array

        k = 0;
        middle = (fromIndex + toIndex) / 2;

        // Copy lower half to array b
        for (i = fromIndex; i < middle; i++)
            b[k++] = a[i];
        // Copy upper half to array b in oppsite order
        for (j = toIndex - 1; j >= middle; j--)
            b[k++] = a[j];

        i = 0;
        j = n - 1;
        k = fromIndex;

        // Copy back next-greatest element at each time
        // until i and j cross
        while (i <= j) {
            if (b[i].getPid().compareTo(b[j].getPid()) < 0)
                a[k++] = b[i++];
            else
                a[k++] = b[j--];
        }
    }

    // //////////////////////////////////
    public void serviceChanged(ServiceEvent event) {
        switch (event.getType()) {
        case ServiceEvent.REGISTERED:
            ServiceReference sr = event.getServiceReference();
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
