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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

// ******************** BundleDependentsCommand ********************
/**
 * 
 * 
 * @author Jan Stein
 * @version $Revision: 1.1.1.1 $
 */
class Alias extends Hashtable {

    private static final long serialVersionUID = 1L;

    public String getString(String key) {
        String[] a = (String[]) get(key);
        if (a != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(a[0]);
            for (int i = 1; i < a.length; i++) {
                sb.append(" ");
                sb.append(a[i]);
            }
            return sb.toString();
        }
        return null;
    }

    void save(OutputStream out) {
        try {
            Properties props = new Properties();
            for (Enumeration e = keys(); e.hasMoreElements();) {
                String k = (String) e.nextElement();
                String[] a = (String[]) get(k);

                StringBuffer sb = new StringBuffer();

                for (int i = 0; i < a.length; i++) {
                    sb.append(a[i].trim());
                    if (i < a.length - 1) {
                        sb.append(" ");
                    }
                }
                props.put(k.trim(), sb.toString());
            }
            props.save(out, "aliases");
        } finally {
            try {
                out.close();
            } catch (Exception ignored) {
            }
        }
    }

    void restore(InputStream in) throws IOException {
        try {
            Properties props = new Properties();
            props.load(in);
            for (Enumeration e = props.keys(); e.hasMoreElements();) {
                String k = (String) e.nextElement();
                String args = (String) props.get(k);

                StringTokenizer st = new StringTokenizer(args, " ");
                String[] a = new String[st.countTokens()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = st.nextToken();
                }
                put(k, a);
            }
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    void cgalias(String group, String[] cmds) {
        for (int i = 0; i < cmds.length; i++) {
            put(cmds[i], new String[] { group, cmds[i] });
        }
    }

    void setDefault() {
        clear();

        cgalias("/session", new String[] { "alias", "echo", "enter", "leave",
                "help", "prompt", "quit", "source", "unalias", "save",
                "restore", });

        String aliasFile = System
                .getProperty("org.knopflerfish.console.alias.file");
        if (aliasFile != null && !"".equals(aliasFile)) {
            File file = new File(aliasFile);
            if (file.exists()) {
                try {
                    restore(new FileInputStream(aliasFile));
                } catch (Exception e) {
                    System.err.println("Failed to restore aliases from "
                            + aliasFile);
                }
                return;
            }
            System.out.println("default alias file " + file.getAbsolutePath()
                    + " does not exists, using internal defaults");
        }

        cgalias("/framework", new String[] { "bundles", "install", "start",
                "stop", "update", "refresh", "services", "startlevel",
                "shutdown", });
        // shortcuts
        put("fw", new String[] { "/session", "enter", "framework" });

        // backward compability
        put("log", new String[] { "/log", "show" });
        put("lsb", new String[] { "/framework", "bundles", "-i" });
        put("lss", new String[] { "/framework", "services" });

        // Oscar compatability
        put("exports", new String[] { "/framework", "package", "-b" });
        put("ps", new String[] { "/framework", "bundles", "-i" });
        put("cd", new String[] { "/framework", "cd" });

        // JES compatability
        put("manifest", new String[] { "/framework", "headers" });
    }
}
