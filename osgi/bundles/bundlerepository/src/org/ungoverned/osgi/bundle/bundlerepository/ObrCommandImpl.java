/*
 * Oscar Bundle Repository
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
package org.ungoverned.osgi.bundle.bundlerepository;

import java.io.*;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.ungoverned.osgi.service.bundlerepository.BundleRecord;
import org.ungoverned.osgi.service.bundlerepository.BundleRepositoryService;
import org.ungoverned.osgi.service.shell.Command;

public class ObrCommandImpl implements Command
{
    private static final String HELP_CMD = "help";
    private static final String URLS_CMD = "urls";
    private static final String LIST_CMD = "list";
    private static final String INFO_CMD = "info";
    private static final String DEPLOY_CMD = "deploy";
    private static final String INSTALL_CMD = "install";
    private static final String START_CMD = "start";
    private static final String UPDATE_CMD = "update";
    private static final String SOURCE_CMD = "source";

    private static final String NODEPS_SWITCH = "-nodeps";
    private static final String CHECK_SWITCH = "-check";
    private static final String EXTRACT_SWITCH = "-x";

    private BundleContext m_context = null;
    private BundleRepositoryService m_brs = null;

    public ObrCommandImpl(BundleContext context, BundleRepositoryService brs)
    {
        m_context = context;
        m_brs = brs;
    }

    public String getName()
    {
        return "obr";
    }

    public String getUsage()
    {
        return "obr help";
    }

    public String getShortDescription()
    {
        return "Oscar bundle repository.";
    }

    public synchronized void execute(String commandLine, PrintStream out, PrintStream err)
    {
        try
        {
            // Parse the commandLine to get the OBR command.
            StringTokenizer st = new StringTokenizer(commandLine);
            // Ignore the invoking command.
            st.nextToken();
            // Try to get the OBR command, default is HELP command.
            String command = HELP_CMD;
            try
            {
                command = st.nextToken();
            }
            catch (Exception ex)
            {
                // Ignore.
            }
            
            // Perform the specified command.
            if ((command == null) || (command.equals(HELP_CMD)))
            {
                help(out, st);
            }
            else
            {
                if (command.equals(URLS_CMD))
                {
                    urls(commandLine, command, out, err);
                }
                else if (command.equals(LIST_CMD))
                {
                    list(commandLine, command, out, err);
                }
                else if (command.equals(INFO_CMD))
                {
                    info(commandLine, command, out, err);
                }
                else if (command.equals(DEPLOY_CMD))
                {
                    deploy(commandLine, command, out, err);
                }
                else if (command.equals(INSTALL_CMD) || command.equals(START_CMD))
                {
                    install(commandLine, command, out, err);
                }
                else if (command.equals(UPDATE_CMD))
                {
                    update(commandLine, command, out, err);
                }
                else if (command.equals(SOURCE_CMD))
                {
                    source(commandLine, command, out, err);
                }
                else
                {
                    err.println("Unknown command: " + command);
                }
            }
        }
        catch (InvalidSyntaxException ex)
        {
            err.println("Syntax error: " + ex.getMessage());
        }
        catch (IOException ex)
        {
            err.println("Error: " + ex);
        }
    }

    private void urls(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException
    {
        // Parse the commandLine.
        StringTokenizer st = new StringTokenizer(commandLine);
        // Ignore the "obr" command.
        st.nextToken();
        // Ignore the "urls" command.
        st.nextToken();

        int count = st.countTokens();
        String[] urls = new String[count];
        for (int i = 0; i < count; i++)
        {
            urls[i] = st.nextToken();
        }
    
        if (count > 0)
        {
            m_brs.setRepositoryURLs(urls);
        }
        else
        {
            urls = m_brs.getRepositoryURLs();
            if (urls != null)
            {
                for (int i = 0; i < urls.length; i++)
                {
                    out.println(urls[i]);
                }
            }
            else
            {
                out.println("No repository URLs are set.");
            }
        }
    }

    private void list(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');

                    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();

        String substr = null;
    
        for (type = tokenizer.nextToken();
            type != StreamTokenizer.TT_EOF;
            type = tokenizer.nextToken())
        {
            // Add a space in between tokens.
            if (substr == null)
            {
                substr = "";
            }
            else
            {
                substr += " ";
            }
                        
            if ((type == StreamTokenizer.TT_WORD) ||
                (type == '\'') || (type == '"'))
            {
                substr += tokenizer.sval.toLowerCase();
            }
        }

        boolean printed = false;
        for (int i = 0; i < m_brs.getBundleRecordCount(); i++)
        {
            BundleRecord record = m_brs.getBundleRecord(i);
            String name = (String) record.getAttribute(BundleRecord.BUNDLE_NAME);
            if (name != null)
            {
                if ((substr == null) ||
                    (name.toLowerCase().indexOf(substr) >= 0))
                {
                    if (!printed)
                    {
                        printed = true;
                        out.println("");
                    }
                    String version =
                        (String) record.getAttribute(BundleRecord.BUNDLE_VERSION);
                    if (version != null)
                    {
                        out.println(name + " (" + version + ")");
                    }
                    else
                    {
                        out.println(name);
                    }
                }
            }
        }
    
        if (printed)
        {
            out.println("");
        }
        else
        {
            out.println("No matching bundles.");
        }
    }

    private void info(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        ParsedCommand pc = parseInfo(commandLine);
        for (int i = 0; (pc != null) && (i < pc.getTargetCount()); i++)                
        {
            out.println("");

            BundleRecord record = null;

            // If there is no version, then try to retrieve by
            // name, but error if there are multiple versions.
            if (pc.getTargetVersion(i) == null)
            {
                BundleRecord[] records =
                    m_brs.getBundleRecords(pc.getTargetName(i));

                if (records.length == 1)
                {
                    record = records[0];
                }
            }
            else
            {
                record = m_brs.getBundleRecord(
                    pc.getTargetName(i),
                    Util.parseVersionString(
                        pc.getTargetVersion(i)));
            }

            if (record != null)
            {
                record.printAttributes(out);
            }
            else
            {
                err.println("Unknown bundle or amiguous version: "
                    + pc.getTargetName(i));
            }
        }
        out.println("");
    }

    private void deploy(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        ParsedCommand pc = parseInstallStart(commandLine);
        for (int i = 0; (pc != null) && (i < pc.getTargetCount()); i++)                
        {
            // Find either the local bundle or the bundle
            // record so we can get the update location attribute.
            String updateLocation = null;

            // First look for update location locally.            
            Bundle bundle =
                findLocalBundle(pc.getTargetName(i), pc.getTargetVersion(i));
            if (bundle != null)
            {
                updateLocation = (String)
                    bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
            }

            // If update location wasn't found locally, look in repository.            
            if (updateLocation == null)
            {
                BundleRecord record =
                    findBundleRecord(pc.getTargetName(i), pc.getTargetVersion(i));
                if (record != null)
                {
                    updateLocation = (String)
                        record.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
                }
            }

            if (updateLocation != null)
            {
                m_brs.deployBundle(
                    out, // Output stream.
                    err, // Error stream.
                    updateLocation, // Update location.
                    pc.isResolve(), // Resolve dependencies.
                    false); // Start.
            }
            else
            {
                err.println("Unknown bundle or amiguous version: "
                    + pc.getTargetName(i));
            }
        }
    }

    private void install(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        // Parse the command line to get all local targets to install.
        ParsedCommand pc = parseInstallStart(commandLine);
        
        // Loop through each local target and try to find
        // the corresponding bundle record from the repository.
        for (int targetIdx = 0;
            (pc != null) && (targetIdx < pc.getTargetCount());
            targetIdx++)                
        {
            // Get the current target's name and version.
            String targetName = pc.getTargetName(targetIdx);
            String targetVersionString = pc.getTargetVersion(targetIdx);

            // Make sure the bundle is not already installed.
            Bundle bundle = findLocalBundle(targetName, targetVersionString);
            if (bundle == null)
            {
                // Find the targets bundle record.
                BundleRecord record = findBundleRecord(targetName, targetVersionString);
            
                // If we found a record, try to install it.
                if (record != null)
                {
                    m_brs.deployBundle(
                        out, // Output stream.
                        err, // Error stream.
                        (String) record.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION), // Update location.
                        pc.isResolve(), // Resolve dependencies.
                        command.equals(START_CMD)); // Start.
                }
                else
                {
                    err.println("Not in repository: " + targetName);
                }
            }
            else
            {
                err.println("Already installed: " + targetName);
            }
        }
    }

    private void update(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        // Parse the command line to get all local targets to update.
        ParsedCommand pc = parseUpdate(commandLine);

        if (pc.isCheck())        
        {
            updateCheck(out, err);
        }
        else
        {
            // Loop through each local target and try to find
            // the corresponding locally installed bundle.
            for (int targetIdx = 0;
                (pc != null) && (targetIdx < pc.getTargetCount());
                targetIdx++)                
            {
                // Get the current target's name and version.
                String targetName = pc.getTargetName(targetIdx);
                String targetVersionString = pc.getTargetVersion(targetIdx);

                // Find corresponding locally installed bundle.
                Bundle bundle = findLocalBundle(targetName, targetVersionString);
            
                // If we found a locally installed bundle, then
                // try to update it.
                if (bundle != null)
                {
                    m_brs.deployBundle(
                        out, // Output stream.
                        err, // Error stream.
                        (String) bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION), // Local bundle to update.
                        pc.isResolve(), // Resolve dependencies.
                        false); // Start.
                }
                else
                {
                    err.println("Not installed: " + targetName);
                }
            }
        }
    }

    private void updateCheck(PrintStream out, PrintStream err)
        throws IOException
    {
        Bundle[] bundles = m_context.getBundles();

        // Loop through each local target and try to find
        // the corresponding locally installed bundle.
        for (int bundleIdx = 0;
            (bundles != null) && (bundleIdx < bundles.length);
            bundleIdx++)
        {
            // Ignore the system bundle.
            if (bundles[bundleIdx].getBundleId() == 0)
            {
                continue;
            }

            // Get the local bundle's update location.
            String localLoc = (String)
                bundles[bundleIdx].getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
            if (localLoc == null)
            {
                // Without an update location, there is no way to
                // check for an update, so ignore the bundle.
                continue;
            }

            // Get the local bundle's version.
            String localVersion = (String)
                bundles[bundleIdx].getHeaders().get(Constants.BUNDLE_VERSION);
            localVersion = (localVersion == null) ? "0.0.0" : localVersion;

            // Get the matching repository bundle records.
            BundleRecord[] records = m_brs.getBundleRecords(
                (String) bundles[bundleIdx].getHeaders().get(Constants.BUNDLE_NAME));

            // Loop through all records to see if there is an update.
            for (int recordIdx = 0;
                (records != null) && (recordIdx < records.length);
                recordIdx++)
            {
                String remoteLoc = (String)
                    records[recordIdx].getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
                if (remoteLoc == null)
                {
                    continue;
                }

                // If the update locations are equal, then compare versions.
                if (remoteLoc.equals(localLoc))
                {
                    String remoteVersion = (String)
                        records[recordIdx].getAttribute(BundleRecord.BUNDLE_VERSION);
                    if (remoteVersion != null)
                    {
                        int result = Util.compareVersion(
                            Util.parseVersionString(remoteVersion),
                            Util.parseVersionString(localVersion));
                        if (result > 0)
                        {
                            out.println(
                                records[recordIdx].getAttribute(BundleRecord.BUNDLE_NAME)
                                + " update available.");
                            break;
                        }
                    }
                }
            }
        }
    }

    private void source(
        String commandLine, String command, PrintStream out, PrintStream err)
        throws IOException, InvalidSyntaxException
    {
        // Parse the command line to get all local targets to update.
        ParsedCommand pc = parseSource(commandLine);
        
        for (int i = 0; i < pc.getTargetCount(); i++)
        {
            BundleRecord record = findBundleRecord(
                pc.getTargetName(i), pc.getTargetVersion(i));
            if (record != null)
            {
                String srcURL = (String)
                    record.getAttribute(BundleRecord.BUNDLE_SOURCEURL);
                if (srcURL != null)
                {
                    FileUtil.downloadSource(
                        out, err, srcURL, pc.getDirectory(), pc.isExtract());
                }
                else
                {
                    err.println("Missing source URL: " + pc.getTargetName(i));
                }
            }
            else
            {
                err.println("Not in repository: " + pc.getTargetName(i));
            }
        }
    }
    
    private BundleRecord findBundleRecord(String name, String versionString)
    {
        BundleRecord record = null;

        // If there is no version, then try to retrieve by
        // name, but error if there are multiple versions.
        if (versionString == null)
        {
            BundleRecord[] records =
                m_brs.getBundleRecords(name);
            if (records.length == 1)
            {
                record = records[0];
            }
        }
        else
        {
            record = m_brs.getBundleRecord(
                name, Util.parseVersionString(versionString));
        }
        
        return record;
    }

    private Bundle findLocalBundle(String name, String versionString)
    {
        Bundle bundle = null;

        // Get the name only if there is no version, but error
        // if there are multiple matches for the same name.
        if (versionString == null)
        {
            // Perhaps the target name is a bundle ID and
            // not a name, so try to interpret as a long.
            try
            {
                bundle = m_context.getBundle(Long.parseLong(name));
            }
            catch (NumberFormatException ex)
            {
                // The bundle is not a number, so look for a local
                // bundle with the same name.
                Bundle[] matchingBundles = findLocalBundlesByName(name);

                // If only one matches, then select is.
                if (matchingBundles.length == 1)
                {
                    bundle = matchingBundles[0];
                }
            }
        }
        else
        {
            // Find the local bundle by name and version.
            bundle = findLocalBundleByVersion(
                name, Util.parseVersionString(versionString));
        }

        return bundle;
    }

    private Bundle findLocalBundleByVersion(String name, int[] version)
    {
        // Get bundles with matching name.
        Bundle[] targets = findLocalBundlesByName(name);

        // Find bundle with matching version.
        if (targets.length > 0)
        {
            for (int i = 0; i < targets.length; i++)
            {
                String targetName = (String)
                    targets[i].getHeaders().get(BundleRecord.BUNDLE_NAME);
                int[] targetVersion = Util.parseVersionString((String)
                    targets[i].getHeaders().get(BundleRecord.BUNDLE_VERSION));
            
                if ((targetName != null) &&
                    targetName.equalsIgnoreCase(name) &&
                    (Util.compareVersion(targetVersion, version) == 0))
                {
                    return targets[i];
                }
            }
        }

        return null;
    }

    private Bundle[] findLocalBundlesByName(String name)
    {
        // Get local bundles.
        Bundle[] bundles = m_context.getBundles();

        // Find bundles with matching name.
        Bundle[] targets = new Bundle[0];
        for (int i = 0; i < bundles.length; i++)
        {
            String targetName = (String)
                bundles[i].getHeaders().get(BundleRecord.BUNDLE_NAME);
            if (targetName == null)
            {
                targetName = bundles[i].getLocation();
            }
            if ((targetName != null) && targetName.equalsIgnoreCase(name))
            {
                Bundle[] newTargets = new Bundle[targets.length + 1];
                System.arraycopy(targets, 0, newTargets, 0, targets.length);
                newTargets[targets.length] = bundles[i];
                targets = newTargets;
            }
        }

        return targets;
    }

    private ParsedCommand parseInfo(String commandLine)
        throws IOException, InvalidSyntaxException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');
    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();
    
        int EOF = 1;
        int SWITCH = 2;
        int TARGET = 4;
        int VERSION = 8;
        int VERSION_VALUE = 16;

        // Construct an install record.
        ParsedCommand pc = new ParsedCommand();
        String currentTargetName = null;

        // The state machine starts by expecting either a
        // SWITCH or a TARGET.
        int expecting = (TARGET);
        while (true)
        {
            // Get the next token type.
            type = tokenizer.nextToken();
            switch (type)
            {
                // EOF received.
                case StreamTokenizer.TT_EOF:
                    // Error if we weren't expecting EOF.
                    if ((expecting & EOF) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Expecting more arguments.", null);
                    }
                    // Add current target if there is one.
                    if (currentTargetName != null)
                    {
                        pc.addTarget(currentTargetName, null);
                    }
                    // Return cleanly.
                    return pc;

                // WORD or quoted WORD received.
                case StreamTokenizer.TT_WORD:
                case '\'':
                case '\"':
                    // If we are expecting a target, the record it.
                    if ((expecting & TARGET) > 0)
                    {
                        // Add current target if there is one.
                        if (currentTargetName != null)
                        {
                            pc.addTarget(currentTargetName, null);
                        }
                        // Set the new target as the current target.
                        currentTargetName = tokenizer.sval;
                        expecting = (EOF | TARGET | VERSION);
                    }
                    else if ((expecting & VERSION_VALUE) > 0)
                    {
                        pc.addTarget(currentTargetName, tokenizer.sval);
                        currentTargetName = null;
                        expecting = (EOF | TARGET);
                    }
                    else
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting '" + tokenizer.sval + "'.", null);
                    }
                    break;

                // Version separator character received.
                case ';':
                    // Error if we weren't expecting the version separator.
                    if ((expecting & VERSION) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting version.", null);
                    }
                    // Otherwise, we will only expect a version value next.
                    expecting = (VERSION_VALUE);
                    break;
            }
        }
    }

    private ParsedCommand parseInstallStart(String commandLine)
        throws IOException, InvalidSyntaxException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');
    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();
    
        int EOF = 1;
        int SWITCH = 2;
        int TARGET = 4;
        int VERSION = 8;
        int VERSION_VALUE = 16;

        // Construct an install record.
        ParsedCommand pc = new ParsedCommand();
        String currentTargetName = null;

        // The state machine starts by expecting either a
        // SWITCH or a TARGET.
        int expecting = (SWITCH | TARGET);
        while (true)
        {
            // Get the next token type.
            type = tokenizer.nextToken();
            switch (type)
            {
                // EOF received.
                case StreamTokenizer.TT_EOF:
                    // Error if we weren't expecting EOF.
                    if ((expecting & EOF) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Expecting more arguments.", null);
                    }
                    // Add current target if there is one.
                    if (currentTargetName != null)
                    {
                        pc.addTarget(currentTargetName, null);
                    }
                    // Return cleanly.
                    return pc;

                // WORD or quoted WORD received.
                case StreamTokenizer.TT_WORD:
                case '\'':
                case '\"':
                    // If we are expecting a command SWITCH and the token
                    // equals a command SWITCH, then record it.
                    if (((expecting & SWITCH) > 0) && tokenizer.sval.equals(NODEPS_SWITCH))
                    {
                        pc.setResolve(false);
                        expecting = (EOF | TARGET);
                    }
                    // If we are expecting a target, the record it.
                    else if ((expecting & TARGET) > 0)
                    {
                        // Add current target if there is one.
                        if (currentTargetName != null)
                        {
                            pc.addTarget(currentTargetName, null);
                        }
                        // Set the new target as the current target.
                        currentTargetName = tokenizer.sval;
                        expecting = (EOF | TARGET | VERSION);
                    }
                    else if ((expecting & VERSION_VALUE) > 0)
                    {
                        pc.addTarget(currentTargetName, tokenizer.sval);
                        currentTargetName = null;
                        expecting = (EOF | TARGET);
                    }
                    else
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting '" + tokenizer.sval + "'.", null);
                    }
                    break;

                // Version separator character received.
                case ';':
                    // Error if we weren't expecting the version separator.
                    if ((expecting & VERSION) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting version.", null);
                    }
                    // Otherwise, we will only expect a version value next.
                    expecting = (VERSION_VALUE);
                    break;
            }
        }
    }

    private ParsedCommand parseUpdate(String commandLine)
        throws IOException, InvalidSyntaxException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');
    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();
    
        int EOF = 1;
        int SWITCH = 2;
        int TARGET = 4;
        int VERSION = 8;
        int VERSION_VALUE = 16;

        // Construct an install record.
        ParsedCommand pc = new ParsedCommand();
        String currentTargetName = null;

        // The state machine starts by expecting either a
        // SWITCH or a TARGET.
        int expecting = (SWITCH | TARGET);
        while (true)
        {
            // Get the next token type.
            type = tokenizer.nextToken();
            switch (type)
            {
                // EOF received.
                case StreamTokenizer.TT_EOF:
                    // Error if we weren't expecting EOF.
                    if ((expecting & EOF) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Expecting more arguments.", null);
                    }
                    // Add current target if there is one.
                    if (currentTargetName != null)
                    {
                        pc.addTarget(currentTargetName, null);
                    }
                    // Return cleanly.
                    return pc;

                // WORD or quoted WORD received.
                case StreamTokenizer.TT_WORD:
                case '\'':
                case '\"':
                    // If we are expecting a command SWITCH and the token
                    // equals a NODEPS switch, then record it.
                    if (((expecting & SWITCH) > 0) && tokenizer.sval.equals(NODEPS_SWITCH))
                    {
                        pc.setResolve(false);
                        expecting = (EOF | TARGET);
                    }
                    // If we are expecting a command SWITCH and the token
                    // equals a CHECK swithc, then record it.
                    else if (((expecting & SWITCH) > 0) && tokenizer.sval.equals(CHECK_SWITCH))
                    {
                        pc.setCheck(true);
                        expecting = (EOF);
                    }
                    // If we are expecting a target, the record it.
                    else if ((expecting & TARGET) > 0)
                    {
                        // Add current target if there is one.
                        if (currentTargetName != null)
                        {
                            pc.addTarget(currentTargetName, null);
                        }
                        // Set the new target as the current target.
                        currentTargetName = tokenizer.sval;
                        expecting = (EOF | TARGET | VERSION);
                    }
                    else if ((expecting & VERSION_VALUE) > 0)
                    {
                        pc.addTarget(currentTargetName, tokenizer.sval);
                        currentTargetName = null;
                        expecting = (EOF | TARGET);
                    }
                    else
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting '" + tokenizer.sval + "'.", null);
                    }
                    break;

                // Version separator character received.
                case ';':
                    // Error if we weren't expecting the version separator.
                    if ((expecting & VERSION) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting version.", null);
                    }
                    // Otherwise, we will only expect a version value next.
                    expecting = (VERSION_VALUE);
                    break;
            }
        }
    }

    private ParsedCommand parseSource(String commandLine)
        throws IOException, InvalidSyntaxException
    {
        // Create a stream tokenizer for the command line string,
        // since the syntax for install/start is more sophisticated.
        StringReader sr = new StringReader(commandLine);
        StreamTokenizer tokenizer = new StreamTokenizer(sr);
        tokenizer.resetSyntax();
        tokenizer.quoteChar('\'');
        tokenizer.quoteChar('\"');
        tokenizer.whitespaceChars('\u0000', '\u0020');
        tokenizer.wordChars('A', 'Z');
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('0', '9');
        tokenizer.wordChars('\u00A0', '\u00FF');
        tokenizer.wordChars('.', '.');
        tokenizer.wordChars('-', '-');
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars('/', '/');
    
        // Ignore the invoking command name and the OBR command.
        int type = tokenizer.nextToken();
        type = tokenizer.nextToken();
    
        int EOF = 1;
        int SWITCH = 2;
        int DIRECTORY = 4;
        int TARGET = 8;
        int VERSION = 16;
        int VERSION_VALUE = 32;

        // Construct an install record.
        ParsedCommand pc = new ParsedCommand();
        String currentTargetName = null;

        // The state machine starts by expecting either a
        // SWITCH or a DIRECTORY.
        int expecting = (SWITCH | DIRECTORY);
        while (true)
        {
            // Get the next token type.
            type = tokenizer.nextToken();
            switch (type)
            {
                // EOF received.
                case StreamTokenizer.TT_EOF:
                    // Error if we weren't expecting EOF.
                    if ((expecting & EOF) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Expecting more arguments.", null);
                    }
                    // Add current target if there is one.
                    if (currentTargetName != null)
                    {
                        pc.addTarget(currentTargetName, null);
                    }
                    // Return cleanly.
                    return pc;

                // WORD or quoted WORD received.
                case StreamTokenizer.TT_WORD:
                case '\'':
                case '\"':
                    // If we are expecting a command SWITCH and the token
                    // equals a command SWITCH, then record it.
                    if (((expecting & SWITCH) > 0) && tokenizer.sval.equals(EXTRACT_SWITCH))
                    {
                        pc.setExtract(true);
                        expecting = (DIRECTORY);
                    }
                    // If we are expecting a directory, the record it.
                    else if ((expecting & DIRECTORY) > 0)
                    {
                        // Set the directory for the command.
                        pc.setDirectory(tokenizer.sval);
                        expecting = (TARGET);
                    }
                    // If we are expecting a target, the record it.
                    else if ((expecting & TARGET) > 0)
                    {
                        // Add current target if there is one.
                        if (currentTargetName != null)
                        {
                            pc.addTarget(currentTargetName, null);
                        }
                        // Set the new target as the current target.
                        currentTargetName = tokenizer.sval;
                        expecting = (EOF | TARGET | VERSION);
                    }
                    else if ((expecting & VERSION_VALUE) > 0)
                    {
                        pc.addTarget(currentTargetName, tokenizer.sval);
                        currentTargetName = null;
                        expecting = (EOF | TARGET);
                    }
                    else
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting '" + tokenizer.sval + "'.", null);
                    }
                    break;

                // Version separator character received.
                case ';':
                    // Error if we weren't expecting the version separator.
                    if ((expecting & VERSION) == 0)
                    {
                        throw new InvalidSyntaxException(
                            "Not expecting version.", null);
                    }
                    // Otherwise, we will only expect a version value next.
                    expecting = (VERSION_VALUE);
                    break;
            }
        }
    }

    private void help(PrintStream out, StringTokenizer st)
    {
        String command = HELP_CMD;
        if (st.hasMoreTokens())
        {
            command = st.nextToken();
        }
        if (command.equals(URLS_CMD))
        {
            out.println("");
            out.println("obr " + URLS_CMD + " [<repository-file-url> ...]");
            out.println("");
            out.println(
                "This command gets or sets the URLs to the repository files\n" +                "used by OBR. Specify no arguments to get the current repository\n" + 
                "URLs or specify a space-delimited list of URLs to change the\n" +
                "URLs. Each URL should point to a file containing meta-data about\n" +                "available bundles in XML format.");
            out.println("");
        }
        else if (command.equals(LIST_CMD))
        {
            out.println("");
            out.println("obr " + LIST_CMD + " [<string> ...]");
            out.println("");
            out.println(
                "This command lists bundles available in the bundle repository.\n" +
                "If no arguments are specified, then all available bundles are\n" +
                "listed, otherwise any arguments are concatenated with spaces\n" +
                "and used as a substring filter on the bundle names.");
            out.println("");
        }
        else if (command.equals(INFO_CMD))
        {
            out.println("");
            out.println("obr " + INFO_CMD
                + " <bundle-name>[;<version>] ...");
            out.println("");
            out.println(
                "This command displays the meta-data for the specified bundles.\n" +
                "If a bundle's name contains spaces, then it must be surrounded\n" +
                "by quotes. It is also possible to specify a precise version\n" +
                "if more than one version exists, such as:\n" +
                "\n" +
                "    obr info \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example retrieves the meta-data for version \"1.0.0\"\n" +
                "of the bundle named \"Bundle Repository\".");
            out.println("");
        }
        else if (command.equals(DEPLOY_CMD))
        {
            out.println("");
            out.println("obr " + DEPLOY_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ... | <bundle-id> ...");
            out.println("");
            out.println(
                "This command tries to install or update the specified bundles\n" +
                "and all of their dependencies by default; use the \"" + NODEPS_SWITCH + "\" switch\n" +
                "to ignore dependencies. You can specify either the bundle name or\n" +
                "the bundle identifier. If a bundle's name contains spaces, then\n" +
                "it must be surrounded by quotes. It is also possible to specify a\n" +                "precise version if more than one version exists, such as:\n" +
                "\n" +
                "    obr deploy \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "For the above example, if version \"1.0.0\" of \"Bundle Repository\" is\n" +
                "already installed locally, then the command will attempt to update it\n" +
                "and all of its dependencies; otherwise, the command will install it\n" +
                "and all of its dependencies.");
            out.println("");
        }
        else if (command.equals(INSTALL_CMD))
        {
            out.println("");
            out.println("obr " + INSTALL_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ...");
            out.println("");
            out.println(
                "This command installs the specified bundles and all of their\n" +
                "dependencies by default; use the \"" + NODEPS_SWITCH + "\" switch to ignore\n" +
                "dependencies. If a bundle's name contains spaces, then it\n" +
                "must be surrounded by quotes. If a specified bundle is already\n" +                "installed, then this command has no effect. It is also possible\n" +                "to specify a precise version if more than one version exists,\n" +                "such as:\n" +
                "\n" +
                "    obr install \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example installs version \"1.0.0\" of the bundle\n" +
                "named \"Bundle Repository\" and its dependencies. ");
            out.println("");
        }
        else if (command.equals(START_CMD))
        {
            out.println("");
            out.println("obr " + START_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ...");
            out.println("");
            out.println(
                "This command installs and starts the specified bundles and all\n" +
                "of their dependencies by default; use the \"" + NODEPS_SWITCH + "\" switch to\n" +
                "ignore dependencies. If a bundle's name contains spaces, then\n" +
                "it must be surrounded by quotes. If a specified bundle is already\n" +                "installed, then this command has no effect. It is also possible\n" +                "to specify a precise version if more than one version exists,\n" +                "such as:\n" +
                "\n" +
                "    obr start \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example installs and starts version \"1.0.0\" of the\n" +
                "bundle named \"Bundle Repository\" and its dependencies.");
            out.println("");
        }
        else if (command.equals(UPDATE_CMD))
        {
            out.println("");
            out.println("obr " + UPDATE_CMD + " " + CHECK_SWITCH);
            out.println("");
            out.println("obr " + UPDATE_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ... | <bundle-id> ...");
            out.println("");
            out.println(
                "The first form of the command above checks for available updates\n" +                "and the second updates the specified locally installed bundles\n" +
                "and all of their dependencies by default; use the \"" + NODEPS_SWITCH + "\" switch\n" +
                "to ignore dependencies. You can specify either the bundle name or\n" +
                "the bundle identifier. If a bundle's name contains spaces, then\n" +
                "it must be surrounded by quotes. If a specified bundle is not\n" +                "already installed, then this command has no effect. It is also\n" +                "possible to specify a precise version if more than one version\n" +                "exists, such as:\n" +
                "\n" +
                "    obr update \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example updates version \"1.0.0\" of the bundle named\n" +
                "\"Bundle Repository\" and its dependencies. The update command may\n" +
                "install new bundles if the updated bundles have new dependencies.");
            out.println("");
        }
        else if (command.equals(SOURCE_CMD))
        {
            out.println("");
            out.println("obr " + SOURCE_CMD
                + " [" + EXTRACT_SWITCH
                + "] <local-dir> <bundle-name>[;<version>] ...");
            out.println("");
            out.println(
                "This command retrieves the source archives of the specified\n" +
                "bundles and saves them to the specified local directory; use\n" +
                "the \"" + EXTRACT_SWITCH + "\" switch to automatically extract the source archives.\n" +
                "If a bundle name contains spaces, then it must be surrounded\n" +
                "by quotes. It is also possible to specify a precise version if\n" +                "more than one version exists, such as:\n" +
                "\n" +
                "    obr source /home/rickhall/tmp \"Bundle Repository\";1.0.0\n" +
                "\n" +
                "The above example retrieves the source archive of version \"1.0.0\"\n" +
                "of the bundle named \"Bundle Repository\" and saves it to the\n" +
                "specified local directory.");
            out.println("");
        }
        else
        {
            out.println("obr " + HELP_CMD
                + " [" + URLS_CMD + " | " + LIST_CMD
                + " | " + INFO_CMD + " | " + INSTALL_CMD
                + " | " + DEPLOY_CMD + " | " + START_CMD
                + " | " + UPDATE_CMD + " | " + SOURCE_CMD + "]");
            out.println("obr " + URLS_CMD + " [<repository-file-url> ...]");
            out.println("obr " + LIST_CMD + " [<string> ...]");
            out.println("obr " + INFO_CMD
                + " <bundle-name>[;<version>] ...");
            out.println("obr " + DEPLOY_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ... | <bundle-id> ...");
            out.println("obr " + INSTALL_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ...");
            out.println("obr " + START_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ...");
            out.println("obr " + UPDATE_CMD + " " + CHECK_SWITCH);
            out.println("obr " + UPDATE_CMD
                + " [" + NODEPS_SWITCH
                + "] <bundle-name>[;<version>] ... | <bundle-id> ...");
            out.println("obr " + SOURCE_CMD
                + " [" + EXTRACT_SWITCH
                + "] <local-dir> <bundle-name>[;<version>] ...");
        }
    }

    private static class ParsedCommand
    {
        private static final int NAME_IDX = 0;
        private static final int VERSION_IDX = 1;

        private boolean m_isResolve = true;
        private boolean m_isCheck = false;
        private boolean m_isExtract = false;
        private String m_dir = null;
        private String[][] m_targets = new String[0][];
        
        public boolean isResolve()
        {
            return m_isResolve;
        }
        
        public void setResolve(boolean b)
        {
            m_isResolve = b;
        }

        public boolean isCheck()
        {
            return m_isCheck;
        }
        
        public void setCheck(boolean b)
        {
            m_isCheck = b;
        }

        public boolean isExtract()
        {
            return m_isExtract;
        }
        
        public void setExtract(boolean b)
        {
            m_isExtract = b;
        }

        public String getDirectory()
        {
            return m_dir;
        }
        
        public void setDirectory(String s)
        {
            m_dir = s;
        }

        public int getTargetCount()
        {
            return m_targets.length;
        }
        
        public String getTargetName(int i)
        {
            if ((i < 0) || (i >= getTargetCount()))
            {
                return null;
            }
            return m_targets[i][NAME_IDX];
        }
        
        public String getTargetVersion(int i)
        {
            if ((i < 0) || (i >= getTargetCount()))
            {
                return null;
            }
            return m_targets[i][VERSION_IDX];
        }

        public void addTarget(String name, String version)
        {
            String[][] newTargets = new String[m_targets.length + 1][];
            System.arraycopy(m_targets, 0, newTargets, 0, m_targets.length);
            newTargets[m_targets.length] = new String[] { name, version };
            m_targets = newTargets;
        }
    }
}
