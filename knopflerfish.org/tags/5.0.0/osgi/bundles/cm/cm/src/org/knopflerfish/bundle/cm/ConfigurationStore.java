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

package org.knopflerfish.bundle.cm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

import org.knopflerfish.shared.cm.CMDataReader;
import org.knopflerfish.shared.cm.CMDataWriter;

/**
 * Persistent storage of configuration data
 *
 * @author Per Gustafson
 */

class ConfigurationStore {
    private final static String storeDataFile = "store_data";

    private final static String pidDataFile = "pid_to_file";

    private final static String factoryPidDataFile = "fpid_to_pids";

    private final static String generatedPids = "generated_pids";

    private final File storeDir;

    private final Properties storeData;

    private final Properties pidToFileName;

    private Hashtable<String, Object> factoryPidToPids;

    private final Hashtable<String, ConfigurationDictionary> cache
      = new Hashtable<String, ConfigurationDictionary>();

    private final CMDataReader cmDataReader = new CMDataReader();

    public ConfigurationStore(File storeDir) throws IOException {
        this.storeDir = storeDir;
        storeDir.mkdirs();

        storeData = createAndLoadProperties(storeDataFile);
        pidToFileName = createAndLoadProperties(pidDataFile);
        factoryPidToPids = loadHashtable(factoryPidDataFile);
        if (factoryPidToPids == null) {
            factoryPidToPids = new Hashtable<String, Object>();
        }
    }

    public synchronized Enumeration<Object> listPids() {
        return pidToFileName.keys();
    }

    public synchronized ConfigurationDictionary load(String pid)
            throws IOException {
        String fileName = pidToFileName.getProperty(pid);
        if (fileName == null) {
            return null;
        }
        return loadConfigurationDictionary(fileName);
    }

    public synchronized ConfigurationDictionary[] loadAll(String factoryPid)
            throws IOException {
        @SuppressWarnings("unchecked")
        Vector<String> v = (Vector<String>) factoryPidToPids.get(factoryPid);
        if (v == null) {
            return null;
        }
        Vector<ConfigurationDictionary> loaded
          = new Vector<ConfigurationDictionary>();
        for (int i = 0; i < v.size(); ++i) {
            ConfigurationDictionary d = load(v.elementAt(i));
            if (d != null) {
                loaded.addElement(d);
            }
        }
        ConfigurationDictionary[] configurations = new ConfigurationDictionary[loaded
                .size()];
        loaded.copyInto(configurations);
        return configurations;
    }

  public synchronized void store(String pid,
                                 String factoryPid,
                                 ConfigurationDictionary configuration,
                                 boolean incrementChangeCount)
      throws IOException
  {
    String fileName = fileNameOf(pid, factoryPid);

    storeConfigurationDictionary(configuration, fileName, incrementChangeCount);
  }

  public synchronized ConfigurationDictionary delete(String pid)
      throws IOException
  {
    if (pid == null || "".equals(pid)) {
      return null;
    }
    String fileName = (String) pidToFileName.remove(pid);
    if (fileName == null || "".equals(fileName)) {
      return null;
    }
    storeProperties(pidToFileName, pidDataFile);

    ConfigurationDictionary d = loadConfigurationDictionary(fileName);
    uncacheConfigurationDictionary(fileName);
    String fpid = (String) d.get(ConfigurationAdmin.SERVICE_FACTORYPID);
    if (fpid != null) {
      @SuppressWarnings("unchecked")
      Vector<String> v = (Vector<String>) factoryPidToPids.get(fpid);
      if (v.removeElement(pid)) {
        if (v.isEmpty()) {
          factoryPidToPids.remove(fpid);
        }
        storeHashtable(factoryPidToPids, factoryPidDataFile);
      }
    }

    File f = new File(storeDir, fileName);
    if (f.exists()) {
      f.delete();
    }
    return d;
  }

    public synchronized String generatePid(String targetedFactoryPid)
        throws IOException
    {
      // Remove the target specification from the factory PID before using it as a
      // base for the generated PID.
      final int barPos = targetedFactoryPid.indexOf('|');
      final boolean isTargetedPID = barPos > 0; // At least one char in the PID.
      final String factoryPid = isTargetedPID ? targetedFactoryPid.substring(0, barPos) : targetedFactoryPid;

      String suffix = null;
      Properties p = new Properties();
      File pidFile = new File(storeDir, generatedPids);
      if (pidFile.exists()) {
        InputStream is = new BufferedInputStream(new FileInputStream(pidFile));
        p.load(is);
        if (is != null) {
          is.close();
        }
        suffix = p.getProperty(factoryPid);
      }
      if (suffix == null) {
        suffix = new Long(0).toString();
      } else {
        long l = Long.parseLong(suffix) + 1;
        suffix = new Long(l).toString();
      }
      p.put(factoryPid, suffix);
      FileOutputStream os = new FileOutputStream(pidFile);
      p.save(os, "Suffixes used for generating pids");
      if (os != null) {
        os.close();
      }
      return factoryPid + "." + suffix;
    }

    // ////////////////////

    private String fileNameOf(String pid, String factoryPid) throws IOException {
        if (pid == null) {
            return null;
        }
        String s = pidToFileName.getProperty(pid);
        if (s == null) {
            s = generateNewFileName(pid, factoryPid);
        }
        return s;
    }

    private String generateNewFileName(String pid, String factoryPid)
            throws IOException {
        final String lastUsedFileNumber = "lastUsedFileNumber";

        String fileNumber = storeData.getProperty(lastUsedFileNumber);
        if (fileNumber == null) {
            fileNumber = new Long(0).toString();
        } else {
            long l = Long.parseLong(fileNumber) + 1;
            fileNumber = new Long(l).toString();
        }
        storeData.put(lastUsedFileNumber, fileNumber);
        storeProperties(storeData, storeDataFile);

        if (factoryPid != null) {
            @SuppressWarnings("unchecked")
            Vector<String> v = (Vector<String>) factoryPidToPids.get(factoryPid);
            if (v == null) {
                v = new Vector<String>();
                factoryPidToPids.put(factoryPid, v);
            }
            v.addElement(pid);
            storeHashtable(factoryPidToPids, factoryPidDataFile);
        }

        pidToFileName.put(pid, fileNumber);
        storeProperties(pidToFileName, pidDataFile);

        return fileNumber;
    }

    private Properties createAndLoadProperties(String fileName)
            throws IOException {
        Properties p = new Properties();
        File f = new File(storeDir, fileName);
        if (f.exists()) {
            InputStream is = new BufferedInputStream(new FileInputStream(f));
            p.load(is);
            if (is != null) {
                is.close();
            }
        }
        return p;
    }

    private void storeProperties(Properties p, String fileName)
            throws IOException {
        File f = new File(storeDir, fileName);
        OutputStream os = new FileOutputStream(f);
        p.save(os, "Private data of the ConfigurationStore");
        if (os != null) {
            os.close();
        }
    }

    // ////////////////////
    private Hashtable<String, Object> loadHashtable(String fileName) throws IOException {
        if (fileName == null) {
            return null;
        }

        File f = new File(storeDir, fileName);
        if (!f.exists()) {
            return null;
        }

        PushbackReader r = new PushbackReader(new BufferedReader(
                new InputStreamReader(new FileInputStream(f),
                        CMDataReader.ENCODING), 8192), 8);

        Hashtable<String,Object> h = null;
        try {
            h = cmDataReader.readCMData(r);
        } catch (Exception e) {
            Activator.log.error("Failed reading file " + f.toString(), e);
            h = null;
        }

        if (r != null) {
            r.close();
        }

        return h;
    }

    private void storeHashtable(Hashtable<String, Object> h, String fileName)
            throws IOException {
        File f = new File(storeDir, fileName);
        PrintWriter w = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(f), CMDataWriter.ENCODING));

        CMDataWriter.writeConfiguration(fileName, h, w);
        if (w != null) {
            w.close();
        }
    }

    // /////////////

    private ConfigurationDictionary getCachedConfigurationDictionary(
            String fileName) {
        return cache.get(fileName);
    }

    private void readAndCacheConfigurationDictionary(String fileName)
            throws IOException {
        Hashtable<String, Object> h = loadHashtable(fileName);
        if (h != null) {
            cache.put(fileName, new ConfigurationDictionary(h));
        }
    }

    private void uncacheConfigurationDictionary(String fileName) {
      cache.remove(fileName);
  }

    private void cacheConfigurationDictionary(String fileName, ConfigurationDictionary d) {
      cache.put(fileName, d);
  }

    private ConfigurationDictionary loadConfigurationDictionary(String fileName)
            throws IOException {
        if (fileName == null) {
            return null;
        }

        ConfigurationDictionary d = getCachedConfigurationDictionary(fileName);
        if (d == null) {
            readAndCacheConfigurationDictionary(fileName);
            d = getCachedConfigurationDictionary(fileName);
        }
        return d;
    }

  private void storeConfigurationDictionary(ConfigurationDictionary d,
                                            String fileName,
                                            boolean incrementChangeCount)
      throws IOException
  {
    // uncacheConfigurationDictionary(fileName);
    // using uncache here will loose the change count since it is not persisted.
    cacheConfigurationDictionary(fileName, d);
    File f = new File(storeDir, fileName);
    PrintWriter w =
      new PrintWriter(new OutputStreamWriter(new FileOutputStream(f),
                                             CMDataWriter.ENCODING));

    if (incrementChangeCount) {
      d.incrementChangeCount();
    }
    String pid = (String) d.get(Constants.SERVICE_PID);
    String fpid = (String) d.get(ConfigurationAdmin.SERVICE_FACTORYPID);
    if (fpid == null) {
      CMDataWriter.writeConfiguration(pid, d, w);
    } else {
      CMDataWriter.writeFactoryConfiguration(fpid, pid, d, w);
    }

    if (w != null) {
      w.close();
    }
  }
}
