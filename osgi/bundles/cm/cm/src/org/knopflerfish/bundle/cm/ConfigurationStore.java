/*
 * Copyright (c) 2003-2014, KNOPFLERFISH project
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.knopflerfish.shared.cm.CMDataReader;
import org.knopflerfish.shared.cm.CMDataWriter;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Persistent storage of configuration data
 *
 * @author Per Gustafson
 */

class ConfigurationStore {
  private static final String VERSION = "1";

  private static final String VERSION_PROP = "version";

  private static final String CHKSUM_PROP = "##KF#CHK_SUM##";

  private final static String STORE_DATA_FILE = "store_data";

  private final static String PID_DATA_FILE = "pid_to_file";

  private final static String FACTORY_PID_DATA_FILE = "fpid_to_pids";

  private final static String GENERATED_PIDS_FILE = "generated_pids";

  private static final String NEXT_SUFFIX = ".next";

  private static final String OLD_SUFFIX = ".old";

  private static final String FACTORY_PID_SUFFIX_SEPARATOR = "._";

  private final File storeDir;

  private final Properties storeData = new  Properties();

  private final Properties pidToFileName = new Properties();

  private final Properties generatedPids = new Properties();

  private Hashtable<String, Object> factoryPidToPids;

  private final Hashtable<String, ConfigurationDictionary> cache
  = new Hashtable<String, ConfigurationDictionary>();

  private final CMDataReader cmDataReader = new CMDataReader();


  public ConfigurationStore(File storeDir) throws IOException {
    this.storeDir = storeDir;
    storeDir.mkdirs();
    boolean doInit = true;
 
    if (loadProperties(STORE_DATA_FILE, storeData)) {
      if (VERSION.equals(storeData.getProperty(VERSION_PROP))) {
        if (loadProperties(PID_DATA_FILE, pidToFileName)) {
          if (loadProperties(GENERATED_PIDS_FILE, generatedPids)) {
            try {
              factoryPidToPids = loadHashtable(FACTORY_PID_DATA_FILE);
              doInit = false;
            } catch (IOException e) {
              Activator.log.error("Failed to load " + FACTORY_PID_DATA_FILE, e);
            }
          } else {
            Activator.log.error("Failed to load " + GENERATED_PIDS_FILE);
          }
        } else {
          Activator.log.error("Failed to load " + PID_DATA_FILE);
        }
      } else {
        Activator.log.error("Found wrong version of CM data " + PID_DATA_FILE);
      }
    }
    if (doInit) {
      init();
    }
  }

  private void init() throws IOException {
    // Remove STORE_DATA_FILE to indicate that we are re-init dir.
    fdelete(STORE_DATA_FILE);
    storeData.clear();
    storeData.put(VERSION_PROP, VERSION);
    pidToFileName.clear();
    generatedPids.clear();
    factoryPidToPids = new Hashtable<String, Object>();
    String [] files = storeDir.list(); 
    HashSet<String> pidFiles = new HashSet<String>();
    int recovered = 0;
    for (String f : files) {
      String n = null;
      if (f.endsWith(NEXT_SUFFIX)) {
        n = f.substring(0, f.length() - NEXT_SUFFIX.length());
      } else if (f.endsWith(OLD_SUFFIX)) {
        n = f.substring(0, f.length() - OLD_SUFFIX.length());
      } else if (f.startsWith(STORE_DATA_FILE) ||
                 f.startsWith(PID_DATA_FILE) ||
                 f.startsWith(GENERATED_PIDS_FILE) ||
                 f.startsWith(FACTORY_PID_DATA_FILE)) {
        continue;
      } else {
        n = f;
      }
      try {
        Long.parseLong(n);
        pidFiles.add(n);
      } catch (NumberFormatException _) {
        Activator.log.warn("Found unknown file in CM data dir: " + f);
      }
    }
    for (String f : pidFiles) {
      try {
        Hashtable<String, Object> d = loadHashtable(f);
        String pid = (String) d.get(CMDataReader.SERVICE_PID);
        String factoryPid = (String) d.get(CMDataReader.FACTORY_PID);
        if (pid != null) {
          if (factoryPid != null) {
            registerFactoryPid(pid, factoryPid);
            int h = pid.lastIndexOf(FACTORY_PID_SUFFIX_SEPARATOR);
            try {
              if (h > 0) {
                String suffix = pid.substring(h + FACTORY_PID_SUFFIX_SEPARATOR.length());
                long found = Long.parseLong(suffix);
                final String key = factoryKey(factoryPid);
                if (key.length() == h) {
                  String old = generatedPids.getProperty(key);
                  if (old == null || Long.parseLong(old) < found) {
                    generatedPids.put(key, Long.toString(found));
                  }
                }
              }
            } catch (NumberFormatException _ignore) { }
          }
          String old = (String) pidToFileName.put(pid, f);
          // If we have conflict keep newest
          if (old != null) {
            if (Long.parseLong(old) > Long.parseLong(f)) {
              pidToFileName.put(pid, old);
              fdelete(f);
            } else {
              fdelete(old);
            }
          } else {
            recovered++;
          }
        } else {
          Activator.log.warn("Found CM data without pid in: " + f);
        }
      } catch (Exception e) {
        Activator.log.warn("Found corrupted CM data in: " + f, e);
      }
    }
    storeHashtable(factoryPidToPids, FACTORY_PID_DATA_FILE);
    storeProperties(generatedPids, GENERATED_PIDS_FILE);
    storeProperties(pidToFileName, PID_DATA_FILE);
    storeProperties(storeData, STORE_DATA_FILE);
    if (recovered > 0) {
      Activator.log.info("Recovered data from corrupted CM data dir, entries found " + recovered);
    } else {
      Activator.log.info("Initialized CM data dir");
    }
  }

  private String factoryKey(String factoryPid) {
    // Remove the target specification from the factory PID before using it as a
    // base for the generated PID.
    final int barPos = factoryPid.indexOf('|');
    final boolean isTargetedPID = barPos > 0; // At least one char in the PID.
    return isTargetedPID ? factoryPid.substring(0, barPos) : factoryPid;
  }

  public synchronized Enumeration<Object> listPids() {
    return pidToFileName.keys();
  }

  public synchronized ConfigurationDictionary load(String pid) throws IOException {
    String fileName = pidToFileName.getProperty(pid);
    if (fileName == null) {
      return null;
    }
    try {
      return loadConfigurationDictionary(fileName);
    } catch (IOException e) {
      pidToFileName.remove(pid);
      Activator.log.error("Removed faulty CM data for: " + pid, e);
      throw e;
    }
  }

  public synchronized ConfigurationDictionary[] loadAll(String factoryPid)
      throws IOException {
    @SuppressWarnings("unchecked")
    Vector<String> v = (Vector<String>) factoryPidToPids.get(factoryPid);
    if (v == null) {
      return null;
    }
    Vector<ConfigurationDictionary> loaded = new Vector<ConfigurationDictionary>();
    for (int i = 0; i < v.size(); ++i) {
      ConfigurationDictionary d = load(v.elementAt(i));
      if (d != null) {
        loaded.addElement(d);
      }
    }
    ConfigurationDictionary[] configurations = new ConfigurationDictionary[loaded.size()];
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
    storeProperties(pidToFileName, PID_DATA_FILE);

    ConfigurationDictionary d = loadConfigurationDictionary(fileName);
    uncacheConfigurationDictionary(fileName);
    if (d != null) {
      String fpid = (String) d.get(ConfigurationAdmin.SERVICE_FACTORYPID);
      if (fpid != null) {
        @SuppressWarnings("unchecked")
        Vector<String> v = (Vector<String>) factoryPidToPids.get(fpid);
        if (v != null && v.removeElement(pid)) {
          if (v.isEmpty()) {
            factoryPidToPids.remove(fpid);
          }
          storeHashtable(factoryPidToPids, FACTORY_PID_DATA_FILE);
        }
      }
    }
    fdelete(fileName);
    return d;
  }

  public synchronized String generatePid(String targetedFactoryPid)
      throws IOException
  {
    final String factoryPid = factoryKey(targetedFactoryPid);
    String suffix = null;
    suffix = generatedPids.getProperty(factoryPid);
    if (suffix == null) {
      suffix = new Long(0).toString();
    } else {
      long l = Long.parseLong(suffix) + 1;
      suffix = Long.toString(l);
    }
    generatedPids.put(factoryPid, suffix);
    storeProperties(generatedPids, GENERATED_PIDS_FILE);
    return factoryPid + FACTORY_PID_SUFFIX_SEPARATOR + suffix;
  }

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
    storeProperties(storeData, STORE_DATA_FILE);

    if (factoryPid != null) {
      registerFactoryPid(pid, factoryPid);
      storeHashtable(factoryPidToPids, FACTORY_PID_DATA_FILE);
    }

    pidToFileName.put(pid, fileNumber);
    storeProperties(pidToFileName, PID_DATA_FILE);

    return fileNumber;
  }

  private void registerFactoryPid(String pid, String factoryPid) {
    @SuppressWarnings("unchecked")
    Vector<String> v = (Vector<String>) factoryPidToPids.get(factoryPid);
    if (v == null) {
      v = new Vector<String>();
      factoryPidToPids.put(factoryPid, v);
    }
    v.addElement(pid);
  }

  private boolean loadProperties(String fileName, Properties p) {
    File file;
    while ((file = finput(fileName)) != null) {
      InputStream is = null;
      try {
        is = new BufferedInputStream(new FileInputStream(file));
        p.load(is);
        is.close();
        is = null;
        String chkSum = p.getProperty(CHKSUM_PROP);
        if (chkSum != null) {
          if (Integer.parseInt(chkSum) == checkSum(p)) {
            return true;
          }
          Activator.log.warn("FAIL! Checksum wrong for properties file: " + file);
        } else {
          // Ignore warning if CM upgrade
          if (fileName.equals(STORE_DATA_FILE) &&
              !p.isEmpty() && !p.containsKey(VERSION_PROP)) {
            Activator.log.info("Old CM data format detected, upgrade");
          } else {
            Activator.log.warn("FAIL! Missing checksum for properties file: " + file);
          }
        }
      } catch (Exception e) {
        if (is != null) {
          try {
            is.close();
          } catch (IOException _ignore) { }
        }
        Activator.log.warn("FAIL! Read properties: " + file, e);
      }
      file.delete();
      p.clear();
    }
    return false;
  }

  private void storeProperties(Properties p, String fileName)
      throws IOException {
    FileOutputStream fo = null;
    try {
      fo = foutput(fileName);
      p.put(CHKSUM_PROP, Integer.toString(checkSum(p)));
      p.store(fo, "Private data of the ConfigurationStore");
      fo.close();
      fo = null;
    } finally {
      fclose(fileName, fo, true);
      p.remove(CHKSUM_PROP);
    }
  }

  private Hashtable<String, Object> loadHashtable(String fileName) throws IOException {
    File f;
    IOException savedException = null;
    while ((f = finput(fileName)) != null) {
      PushbackReader r = null;
      try {
        r = new PushbackReader(new BufferedReader(
            new InputStreamReader(new FileInputStream(f),
                CMDataReader.ENCODING), 8192), 8);

        Hashtable<String,Object> h = cmDataReader.readCMData(r);
        r.close();
        r = null;
        if (f.getName().length() != fileName.length()) {
          // We have recovered a file
          f.renameTo(new File(storeDir, fileName));
          Activator.log.info("Recovered CM data from: " + fileName);
        } else {
          // Succeeded reading a file, remove backup
          deleteBackup(fileName);
        }
        return h;
      } catch (Exception e) {
        Activator.log.warn("Failed reading file " + f.toString(), e);
        if (e instanceof IOException) {
          savedException = (IOException) e;
        } else {
          savedException  = new IOException("Parsing error, " + fileName + ": " + e);
        }
        if (r != null) {
          try {
            r.close();
          } catch (IOException _ignore) { }
        }
        f.delete();
      }
    }
    if (savedException != null) {
      throw savedException;
    }
    throw new FileNotFoundException(fileName.toString());
  }

  private void storeHashtable(Hashtable<String, Object> h, String fileName)
      throws IOException {
    FileOutputStream fo = null;
    try {
      fo = foutput(fileName);
      PrintWriter w = new PrintWriter(new OutputStreamWriter(fo, CMDataWriter.ENCODING));

      CMDataWriter.writeConfiguration(fileName, h, w);
      w.close();
      fo = null;
    } finally {
      fclose(fileName, fo, true);
    }
  }

  private ConfigurationDictionary getCachedConfigurationDictionary(
      String fileName) {
    return cache.get(fileName);
  }

  private ConfigurationDictionary readAndCacheConfigurationDictionary(String fileName) throws IOException {
    Hashtable<String, Object> h = loadHashtable(fileName);
    if (h != null) {
      ConfigurationDictionary d = new ConfigurationDictionary(h);
      cache.put(fileName, d);
      return d;
    }
    return null;
  }

  private void uncacheConfigurationDictionary(String fileName) {
    cache.remove(fileName);
  }

  private void cacheConfigurationDictionary(String fileName, ConfigurationDictionary d) {
    cache.put(fileName, d);
  }

  private ConfigurationDictionary loadConfigurationDictionary(String fileName)
      throws IOException
  {
    ConfigurationDictionary d = getCachedConfigurationDictionary(fileName);
    if (d == null) {
      d = readAndCacheConfigurationDictionary(fileName);
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
    FileOutputStream fo = null;
    try {
      fo = foutput(fileName);
      PrintWriter w = new PrintWriter(new OutputStreamWriter(fo, CMDataWriter.ENCODING));

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
      w.close();
      fo = null;
    } finally {
      fclose(fileName, fo, false);
    }
  }

  private File finput(String fileName) {
    File res = new File(storeDir, fileName);
    if (!res.exists()) {
      res = new File(storeDir, fileName + NEXT_SUFFIX);
      if (!res.exists()) {
        res = new File(storeDir, fileName + OLD_SUFFIX);
        if (!res.exists()) {
          res = null;
        }
      }
    }
    return res;
  }

  private FileOutputStream foutput(String fileName) throws FileNotFoundException {
    File f = new File(storeDir, fileName);
    if (f.exists()) {
      File fold = new File(storeDir, fileName + OLD_SUFFIX);
      fold.delete();
      f.renameTo(fold);
    }
    File fnext = new File(storeDir, fileName + NEXT_SUFFIX);
    if (fnext.exists()) {
      fnext.renameTo(f);
    }
    return new FileOutputStream(fnext);
  }

  private void fclose(String fileName, FileOutputStream fo, boolean purgeOld) {
    File fnext = new File(storeDir, fileName + NEXT_SUFFIX);
    if (fo == null) {
      File f = new File(storeDir, fileName);
      if (f.exists()) {
        f.delete();
      }
      fnext.renameTo(f);
      if (purgeOld) {
        new File(storeDir, fileName + OLD_SUFFIX).delete();
      }
    } else {
      try {
        fo.close();
      } catch (IOException _ignore) { }
      fnext.delete();
    }
  }

  private void fdelete(String fileName) {
    new File(storeDir, fileName).delete();
    new File(storeDir, fileName + NEXT_SUFFIX).delete();
    new File(storeDir, fileName + OLD_SUFFIX).delete();
  }

  private boolean deleteBackup(String fileName) {
    return new File(storeDir, fileName + OLD_SUFFIX).delete();
  }

  private int checkSum(Properties p) {
    int res = 0;
    p.remove(CHKSUM_PROP);
    for (Object o : p.values()) {
      if (o instanceof String) {
        res += 997 + 11 * ((String)o).length();
      }
    }
    return res;
  }

}
