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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

// ******************** BundleDependentsCommand ********************
/**
 * 
 * 
 * @author Jan Stein
 */
class Alias extends Hashtable<String, String[]> {

    private static final long serialVersionUID = 1L;

    public String getString(String key) {
        String[] aliasArray = get(key);
        if (aliasArray != null) {
            return String.join(" ", aliasArray);
        }
        return null;
    }

    void save(File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            Properties props = new Properties();
            for (Map.Entry<String, String[]> entry : entrySet()) {
                String aliasName = entry.getKey();
                String[] aliasArray = entry.getValue();
                String aliasString = Arrays.stream(aliasArray)
                    .map(String::trim)
                    .collect(Collectors.joining(" "));
                props.put(aliasName.trim(), aliasString);
            }
            props.store(out, "aliases");
        }
    }

    void restore(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String aliasName = entry.getKey().toString();
                String aliasString = entry.getValue().toString();
                String[] aliasArray = aliasString.split(" ");
                put(aliasName, aliasArray);
            }
        }
    }

    void createGroupAliases(String group, String[] commands) {
        for (String command : commands) {
            put(command, new String[] { group, command });
        }
    }

    void setDefault() {
        clear();

        createGroupAliases("/session", new String[] { "alias", "echo", "enter", "leave",
                "help", "quit", "source", "unalias" });

        String aliasFile = System .getProperty("org.knopflerfish.console.alias.file");
        if (aliasFile != null && !"".equals(aliasFile)) {
            File file = new File(aliasFile);
            if (file.exists()) {
                try {
                    restore(file);
                } catch (Exception e) {
                    System.err.println("Failed to restore aliases from "
                            + aliasFile);
                }
                return;
            }
            System.out.println("default alias file " + file.getAbsolutePath()
                    + " does not exists, using internal defaults");
        }

        createGroupAliases("/framework", new String[] { "bundles", "install", "start",
                "stop", "update", "refresh", "services", "startlevel",
                "shutdown", });
        // shortcuts
        put("fw", new String[] { "/session", "enter", "framework" });

        // backward compatibility
        put("log", new String[] { "/log", "show" });
        put("lsb", new String[] { "/framework", "bundles", "-i" });
        put("lss", new String[] { "/framework", "services" });

        // Oscar compatibility
        put("exports", new String[] { "/framework", "package", "-b" });
        put("ps", new String[] { "/framework", "bundles", "-i" });

        // JES compatibility
        put("manifest", new String[] { "/framework", "headers" });
    }
}
