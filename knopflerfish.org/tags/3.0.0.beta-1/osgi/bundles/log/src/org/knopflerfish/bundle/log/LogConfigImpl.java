/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import org.knopflerfish.service.log.LogConfig;
import org.knopflerfish.service.log.LogUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * * This class implements the log configuration of the log * service.
 * Properties are defined using * set<propertyName>() and get<propertyName>()
 * methods.<br> * Ex. defining integer property Foo<br> * void setFoo(int
 * value)<br> * int getFoo()<br> * <br> * <br> * If only set method exists the
 * property is write-only.<br> * If only get method exists the property is
 * read-only.<br> * If both exist the property is read-write. *
 */

class LogConfigImpl implements ManagedService, LogConfig {

  static BundleContext bc;

  /*
   * Variables indicating whether CM configuration has been received.
   */
  boolean DEFAULT_CONFIG = true;

  boolean firstValid = false;

  static final String PROP_LOG_OUT = "org.knopflerfish.log.out";

  static final String PROP_LOG_GRABIO = "org.knopflerfish.log.grabio";

  static final String PROP_LOG_LEVEL = "org.knopflerfish.log.level";

  static final String PROP_LOG_FILE = "org.knopflerfish.log.file";

  static final String PROP_LOG_FILE_DIR = "org.knopflerfish.log.file.dir";

  private final static String OUT = "log.out";

  private final static String GRABIO = "log.grabio";

  private final static String L_FILTER = "default.level";

  private final static String BL_FILTERS = "bundle.log.level";

  private final static String DIR = "file.dir";

  private final static String FILE_S = "file.size";

  private final static String FLUSH = "file.flush";

  private final static String PID = "service.pid";

  private String pid;

  final static String MEM = "memory.size";

  final static String GEN = "file.generations";

  final static String FILE = "file";

  private final static int LOCATION_POS = 0;

  private final static int FILTER_POS = 1;

  /* Variables containing configuration. */
  private File dir;

  private Hashtable configCollection;

  private HashMap blFilters = new HashMap();

  private LogReaderServiceFactory logReaderCallback;

  public LogConfigImpl(BundleContext bc) {
    this.bc = bc;
    start();
  }

  synchronized void start() {
    this.pid = this.getClass().getName();
    configCollection = getDefault();
    initDir();
    String[] clazzes = new String[] { ManagedService.class.getName(),
                                      LogConfig.class.getName() };
    bc.registerService(clazzes, this, configCollection);
  }

  private void initDir() {
    dir = bc.getDataFile(""); // default location
    String d = (String)configCollection.get(DIR);
    if (d != null) {
      dir = new File(d);     // location from config
    }
  }

  void init(LogReaderServiceFactory lr) {
    this.logReaderCallback = lr;
  }

  void stop() {
    this.logReaderCallback = null;
  }

  /***************************************************************************
   * org.knopflerfish.service.log.LogConfig methods
   **************************************************************************/

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#commit()
   */
  public synchronized void commit() {
    updateConfig();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#isDefaultConfig()
   */
  public boolean isDefaultConfig() {
    return DEFAULT_CONFIG;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setMemorySize(int)
   */
  public synchronized void setMemorySize(int size) {
    int oldSize = getMemorySize();
    if (size != oldSize) {
      Integer newSize = new Integer(size);
      set(MEM, newSize);
      if (logReaderCallback != null)
        logReaderCallback.configChange(MEM, new Integer(oldSize),
                                       newSize);
      updateConfig();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getMemorySize()
   */
  public int getMemorySize() {
    return ((Integer) get(MEM)).intValue();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFilter(int)
   */
  public synchronized void setFilter(int filter) {
    int oldFilter = getFilter();
    if (filter == 0) {
      filter = org.osgi.service.log.LogService.LOG_WARNING;
    }
    if (filter != oldFilter) {
      set(L_FILTER, LogUtil.fromLevel(filter));
      updateConfig();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFilter()
   */
  public int getFilter() {
    return LogUtil.toLevel((String) get(L_FILTER),
                           org.osgi.service.log.LogService.LOG_WARNING);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFilter(java.lang.String,
   *      int)
   */
  public synchronized void setFilter(String bundleLocation, int filter) {
    bundleLocation = bundleLocation.trim();
    synchronized (blFilters) {
      Integer f = (Integer) blFilters
        .get(getCommonLocation(bundleLocation));
      if (filter == 0 && f != null) {
        blFilters.remove(getCommonLocation(bundleLocation));
        setCollection(true, bundleLocation, filter);
      } else if ((f != null && filter != f.intValue()) || f == null) {
        blFilters.put(getCommonLocation(bundleLocation), new Integer(
                                                                     filter));
        setCollection(false, bundleLocation, filter);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFilters()
   */
  public synchronized HashMap getFilters() {
    return (HashMap) blFilters.clone();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setOut(boolean)
   */
  public synchronized void setOut(boolean b) {
    boolean oldOut = getOut();
    if (b != oldOut) {
      set(OUT, new Boolean(b));
      updateConfig();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getOut()
   */
  public boolean getOut() {
    return ((Boolean) get(OUT)).booleanValue();
  }

  /** ****** Configuration for filing log entries. ************ */
  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFile(boolean)
   */
  public synchronized void setFile(boolean f) {
    if ((dir != null)) {
      boolean oldFile = getFile();
      if (f != oldFile) {
        Boolean newFile = new Boolean(f);
        set(FILE, newFile);
        if (logReaderCallback != null)
          logReaderCallback.configChange(FILE, new Boolean(oldFile),
                                         newFile);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFile()
   */
  public boolean getFile() {
    return (((Boolean) get(FILE)).booleanValue() && (getDir() != null));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getDir()
   */
  public File getDir() {
    if (dir == null) {
      return dir;
    }
    synchronized (dir) {
      return dir;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFileSize(int)
   */
  public synchronized void setFileSize(int fS) {
    int oldSize = getFileSize();
    if (oldSize != fS) {
      set(FILE_S, new Integer(fS));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFileSize()
   */
  public int getFileSize() {
    return ((Integer) get(FILE_S)).intValue();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setMaxGen(int)
   */
  public synchronized void setMaxGen(int maxGen) {
    int oldGen = getMaxGen();
    if (oldGen != maxGen) {
      set(GEN, new Integer(maxGen));
      if (logReaderCallback != null)
        logReaderCallback.configChange(GEN, new Integer(oldGen),
                                       new Integer(maxGen));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getMaxGen()
   */
  public int getMaxGen() {
    int gen = ((Integer) get(GEN)).intValue();
    return (gen < 1) ? 1 : gen;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFlush(boolean)
   */
  public synchronized void setFlush(boolean f) {
    boolean oldFlush = getFlush();
    if (f != oldFlush) {
      set(FLUSH, new Boolean(f));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFlush()
   */
  public boolean getFlush() {
    return ((Boolean) get(FLUSH)).booleanValue();
  }

  /*
   * Implement method which checks if a bundle has a specific loglevel and
   * return to ReaderFactory.
   */

  int getLevel(Bundle bundle) {
    Integer level;
    synchronized (blFilters) {
      level = (Integer) blFilters.get(bundle.getLocation());
      if (level == null) {
        Dictionary d = bundle.getHeaders("");
        String l = (String)d.get("Bundle-SymbolicName");
        if (l == null) {
          l = (String)d.get("Bundle-Name");
        }
        if (l != null) {
          level = (Integer) blFilters.get(getCommonLocation(l));
        }
      }
    }
    return (level != null) ? level.intValue() : getFilter();
  }

  static String[] getBL(Object obj) {
    String bundleStr = (String) obj;
    String[] bundle = new String[] { null, null };
    int ix = bundleStr.indexOf(";");
    try {
      bundle[0] = bundleStr.substring(0, ix).trim();
      bundle[1] = bundleStr.substring(ix + 1).trim();
    } catch (Exception e) {
      throw new IllegalArgumentException(
                                         "Bundle entries must be in the format location;level");
    }
    return bundle;
  }

  private void setCollection(boolean remove, String bundleLocation, int filter) {
    synchronized (configCollection) {
      Vector v = (Vector) configCollection.get(BL_FILTERS);
      String[] bundF;
      boolean notFound = true;
      if (v != null && v.size() > 0) {
        for (int i = (v.size() - 1); i >= 0; i--) {
          bundF = getBL(v.elementAt(i));
          if (getCommonLocation(bundF[LOCATION_POS]).equals(
                                                            getCommonLocation(bundleLocation))) {
            if (remove) {
              v.removeElementAt(i);
            } else {
              v.setElementAt(bundleLocation + ";"
                             + LogUtil.fromLevel(filter), i);
            }
            notFound = false;
            break;
          }
        }
      }
      if (notFound && !remove) {
        v.addElement(bundleLocation + ";" + LogUtil.fromLevel(filter));
      }
    }
  }

  boolean getGrabIO() {
    return ((Boolean) get(GRABIO)).booleanValue();
  }

  private Object get(String key) {
    synchronized (configCollection) {
      return configCollection.get(key);
    }
  }

  private void set(String key, Object value) {
    synchronized (configCollection) {
      configCollection.put(key, value);
    }
  }

  /** **************** Called from set methods ********************* */

  private void updateConfig() {
    try {
      ServiceReference sr = bc
        .getServiceReference(ConfigurationAdmin.class.getName());
      if (sr != null) {
        ConfigurationAdmin ca = (ConfigurationAdmin) bc.getService(sr);
        if (ca != null) {
          Configuration conf = ca.getConfiguration(pid);
          if (conf != null) {
            conf.update(configCollection);
          }
        }
        bc.ungetService(sr);
      }
    } catch (IOException io) {
    } catch (java.lang.IllegalArgumentException iae) {
    } catch (java.lang.IllegalStateException ise) {
    }
  }

  /* ======================================================================== */
  /* Initializes configuration */
  /* ======================================================================== */

  private Hashtable getDefault() {
    Hashtable ht = new Hashtable();
    Vector bundleLogFilters = new Vector();
    String o = getProperty(PROP_LOG_OUT, "false");

    String levelStr = getProperty(PROP_LOG_LEVEL, LogUtil
                                  .fromLevel(org.osgi.service.log.LogService.LOG_WARNING));

    ht.put(L_FILTER, levelStr);
    ht.put(MEM, new Integer(250));
    ht.put(OUT, new Boolean(("true".equalsIgnoreCase(o)) ? true : false));
    ht.put(GRABIO, new Boolean(("true".equalsIgnoreCase(getProperty(
                                                                    PROP_LOG_GRABIO, "false")) ? true : false)));
    ht.put(FILE, new Boolean(("true".equalsIgnoreCase(getProperty(
                                                                  PROP_LOG_FILE, "false")) ? true : false)));
    String dirStr = getProperty(PROP_LOG_FILE_DIR,null);
    if (dirStr != null)
      ht.put(DIR, dirStr);
    ht.put(FILE_S, new Integer(20000));
    ht.put(GEN, new Integer(4));
    ht.put(FLUSH, new Boolean(true));
    ht.put(BL_FILTERS, bundleLogFilters);
    ht.put(PID, this.pid);

    return ht;
  }

  static String getProperty(String key, String def) {
    String result = def;
    try {
      result = bc.getProperty(key);
      if (result == null) {
        result = def;
      }
    } catch (Exception e) {
      System.err.println("Failed to get property " + key + " : " + e);
      result = def;
    }
    return result;
  }

  /* ======================================================================== */
  /* Method implementing the ManagedService interface */
  /* gets called whenever the configuration for this */
  /* ManagedService is updated. */
  /* ======================================================================== */

  public synchronized void updated(Dictionary cfg)
    throws ConfigurationException, IllegalArgumentException {
    if (cfg == null) {
      DEFAULT_CONFIG = true;
      checkChange(getDefault());
    } else {
      checkValidity(cfg);
    }

    updateGrabIO();
  }

  boolean bGrabbed = false;

  PrintStream origOut = null;

  PrintStream origErr = null;

  LogReaderServiceFactory lsrf;

  void setLogReaderServiceFactory(LogReaderServiceFactory lsrf) {
    this.lsrf = lsrf;
  }

  void updateGrabIO() {
    boolean bDebugClass = "true".equals
      (getProperty("org.knopflerfish.framework.debug.classloader", "false"));
    if (!bDebugClass) {
      if (getGrabIO()) {
        grabIO();
      } else {
        ungrabIO();
      }
    }
  }

  void grabIO() {
    if (!getOut()) {
      if (!bGrabbed) {
        origOut = System.out;
        origErr = System.err;
        System.setOut(new WrapStream("[stdout] ", System.out,
                                     org.osgi.service.log.LogService.LOG_INFO));
        System.setErr(new WrapStream("[stderr] ", System.out,
                                     org.osgi.service.log.LogService.LOG_ERROR));
        bGrabbed = true;
      }
    }
  }

  void ungrabIO() {
    if (bGrabbed) {
      System.setOut(origOut);
      System.setErr(origErr);
      bGrabbed = false;
    }
  }

  class WrapStream extends PrintStream {
    String prefix;

    int level;

    WrapStream(String prefix, PrintStream out, int level) {
      super(out);
      this.prefix = prefix;
      this.level = level;
    }

    public void println(String s) {
      super.print(prefix);
      super.println(s);
      log(s);
    }

    public void print(String s) {
      super.print(s);
    }

    public void println(Object x) {
      super.print(prefix);
      super.println(x);
      log("" + x);
    }

    public void print(Object x) {
      super.print(x);
    }

    void log(String s) {
      if (s != null && -1 != s.indexOf(prefix)) {
        return;
      }

      if (lsrf != null) {
        lsrf.log(new LogEntryImpl(bc.getBundle(0), level, prefix + s,
                                  null));
      }
    }
  }

  /*------------------------------------------------------------------------*/
  /* Methods for checking incoming configuration, */
  /* Methods for setting incoming configuration localy. */
  /*------------------------------------------------------------------------*/
  /*
   * Check that incoming configuration is correct. If not an exception will be
   * thrown and the default configuration or the former configuration will be
   * used for ALL properties, i.e., no property will be set if one item in the
   * configuration is invalid.
   */
  private void checkValidity(Dictionary cfg) throws ConfigurationException,
                                                    IllegalArgumentException {
    boolean valid = false;
    Hashtable rollBack = (Hashtable) configCollection.clone();
    try {
      checkLogLevel(cfg);
      checkBundleLogLevel(cfg);
      valid = true;
    } finally {
      if (!valid) {
        // Removed call to updateConfig() because all it accomplishes
        // is to cause an endless loop of ConfigurationAdmin
        // calling ManagedService.update(Dictionary) on this class
        // over and over and over with the same invalid Dictionary
        // updateConfig();
      } else {
        valid = false;
        try {
          acceptConfig(cfg);
          valid = true;
        } catch (Exception all) {
          throw new ConfigurationException(null,
                                           "Fault occurred when " + "setting configuration. "
                                           + "Check that all properties "
                                           + "are valid.");
        } finally {
          if (!valid) {
            configCollection = rollBack;
            // Removed call to updateConfig() because all it
            // accomplishes
            // is to cause an endless loop of ConfigurationAdmin
            // calling ManagedService.update(Dictionary) on this
            // class
            // over and over and over with the same invalid
            // Dictionary
            // updateConfig();
          }
        }
      }
    }
  }

  /* Check log level property for faults. */
  private void checkLogLevel(Dictionary cfg) throws ConfigurationException,
                                                    IllegalArgumentException {
    String filter = null;
    Object obj = cfg.get(L_FILTER);
    if (obj == null) {
      throw new IllegalArgumentException(
                                         "No log level given. Please supply a valid log level.");
    }
    try {
      filter = ((String) obj).trim();
    } catch (ClassCastException cce) {
      throw new IllegalArgumentException(
                                         "Wrong type supplied when attempting to set log level."
                                         + " Correct type to use is String. " + obj + " "
                                         + obj.getClass().getName());
    }
    int filterVal = LogUtil.toLevel(filter, -1);
    if (filterVal == -1) {
      throw new ConfigurationException(L_FILTER, "Undefined log level <"
                                       + filter + ">.");
    }
    if (filterVal == 0) {
      cfg.put(L_FILTER, LogUtil
              .fromLevel(org.osgi.service.log.LogService.LOG_WARNING));
    }
  }

  /* Check bundle log level property for faults. */
  private void checkBundleLogLevel(Dictionary cfg)
    throws ConfigurationException, IllegalArgumentException {
    Vector v = null;
    try {
      v = (Vector) cfg.get(BL_FILTERS);
    } catch (ClassCastException cce) {
      throw new IllegalArgumentException(
                                         "Wrong type supplied when attempting to set log level for "
                                         + "specific bundle."
                                         + " Correct type to use is Vector of String[].");
    }
    if (v != null) {
      String[] bundle = null;
      for (int i = 0; i < v.size(); i++) {
        try {
          bundle = getBL(v.elementAt(i));
        } catch (ClassCastException cce) {
          throw new IllegalArgumentException(
                                             "Wrong type supplied when attempting to set log level for "
                                             + "specific bundle."
                                             + " Correct type to use is String.");
        }
        if (bundle == null) {
          throw new IllegalArgumentException(
                                             "Empty configuration supplied when attempting to set log level "
                                             + " for specific bundle.");
        }
        bundle[LOCATION_POS] = bundle[LOCATION_POS].trim();
        if (bundle[LOCATION_POS] == null
            || bundle[LOCATION_POS].length() <= 0) {
          throw new IllegalArgumentException(
                                             "No bundle location given when setting log level for specific "
                                             + "bundle.");
        }
        if (bundle[FILTER_POS] == null) {
          throw new IllegalArgumentException(
                                             "No log level given for bundle: "
                                             + bundle[LOCATION_POS] + ". "
                                             + "Please supply a valid log level.");
        }
        int testFilter = 0;
        testFilter = LogUtil.toLevel((bundle[FILTER_POS].trim()), -1);
        if (testFilter == -1) {
          throw new ConfigurationException(BL_FILTERS,
                                           "Undefined log level <" + bundle[FILTER_POS]
                                           + "> specified for bundle <"
                                           + bundle[LOCATION_POS] + ">.");
        }
        if (testFilter == 0) {
          v.removeElementAt(i);
          i--;
        } else {
          bundle[FILTER_POS] = LogUtil.fromLevel(testFilter);
          checkLocation(bundle[LOCATION_POS]);
        }
      }
    }
  }

  /* Check whether given location exists. */
  private void checkLocation(String location) {
    try {
      if (location.indexOf("/") != -1 || location.indexOf("\\") != -1) {
        URL u = new URL(getCommonLocation(location));
        InputStream is = u.openStream();
        is.close();
      }
    } catch (IOException ignore) {
      log("File <" + location + "> set at configuration of logcomponent "
          + "does not exist at the given location. ");
    }
  }

  /*
   * Called when an incoming configuration seems correct. Should the
   * checkChange method throw an exception the configuration will be reset to
   * the former valid state.
   */
  private void acceptConfig(Dictionary cfg) {
    firstValid = DEFAULT_CONFIG;
    DEFAULT_CONFIG = false;
    checkChange(cfg);
  }

  /*
   * Checking which property actually changed. Called once the validity of the
   * incoming configuration has been checked. If some property changed notify
   * about change.
   */
  private void checkChange(Dictionary cfg) {
    setFilterCfg((Vector) cfg.get(BL_FILTERS));
    Object newV = null;
    if ((newV = diff(L_FILTER, cfg)) != null) {
      set(L_FILTER, newV);
    }
    if ((newV = diff(MEM, cfg)) != null) {
      notify(MEM, newV);
      set(MEM, newV);
    }
    if ((newV = diff(OUT, cfg)) != null) {
      if (DEFAULT_CONFIG) {
        set(OUT, "true".equalsIgnoreCase(getProperty(PROP_LOG_OUT,
                                                     "false")) ? Boolean.TRUE : Boolean.FALSE);
      } else {
        set(OUT, newV);
      }
    }
    if ((newV = diff(DIR, cfg)) != null) {
      File currentDir = bc.getDataFile("/");
      newV = ((String) newV).trim();
      if (currentDir != null && !(newV.equals(""))) {
        currentDir = new File((String) newV);
      }
      if (dir != null) {
        synchronized (dir) {
          dir = currentDir;
        }
      } else {
        dir = currentDir;
      }
      set(DIR, newV);
    }
    if ((newV = diff(FILE_S, cfg)) != null) {
      set(FILE_S, newV);
    }
    if ((newV = diff(FLUSH, cfg)) != null) {
      set(FLUSH, newV);
    }
    if ((newV = diff(FILE, cfg)) != null) {
      if (dir != null) {
        if (firstValid) {
          synchronized (configCollection) {
            configCollection.remove(FILE);
          }
          firstValid = false;
        }
        notify(FILE, newV);
      }
      set(FILE, (DEFAULT_CONFIG) ? new Boolean(false) : newV);
    }
    if ((newV = diff(GEN, cfg)) != null) {
      notify(GEN, newV);
      set(GEN, newV);
    }
  }

  /*
   * Check bundle log level and see if changes has been made. If so change
   * internal representation of bundle log levels.
   */
  private void setFilterCfg(Vector newV) {
    if (newV != null) {
      String[] bundle = null;
      String common;
      int newFilter = -1;
      HashMap tmpFilters = new HashMap();
      for (int i = 0; (i < newV.size()); i++) {
        bundle = getBL(newV.elementAt(i));
        common = getCommonLocation(bundle[LOCATION_POS]);
        newFilter = LogUtil.toLevel((bundle[FILTER_POS].trim()),
                                    org.osgi.service.log.LogService.LOG_WARNING);
        tmpFilters.put(common, new Integer(newFilter));
      }
      synchronized (blFilters) {
        blFilters = tmpFilters;
      }
      set(BL_FILTERS, newV);
    }
  }

  /* Utility methods serving this and LogConfigCommandGroup. */
  String getCommonLocation(String location) {
    if (location.endsWith(".jar")) {
      return location;
    }
    return location + ".jar";
  }

  private Object diff(String key, Dictionary cfg) {
    Object newV = null;
    return ((newV = cfg.get(key)) != null && !newV.equals(configCollection
                                                          .get(key))) ? newV : null;
  }

  private void notify(String key, Object newV) {
    if (logReaderCallback != null) {
      logReaderCallback
        .configChange(key, configCollection.get(key), newV);
    }
  }

  private void log(String msg) {
    if (logReaderCallback != null) {
      logReaderCallback.log(new LogEntryImpl(bc.getBundle(),
                                             org.osgi.service.log.LogService.LOG_INFO, msg));
    }
  }
}
