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

package org.knopflerfish.bundle.log;

import org.knopflerfish.service.log.LogConfig;
import org.knopflerfish.service.log.LogUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This class implements the log configuration of the log service.
 * <p/>
 * Properties are defined using <tt>set&lt;propertyName&gt;()</tt> and
 * <tt>get&lt;propertyName&gt;()</tt> methods.<br>
 * <p/>
 * Ex. defining integer property Foo<br>
 * <tt>void setFoo(int value)</tt><br>
 * <tt>int getFoo()</tt><br>
 * <p/>
 * If only set method exists the property is write-only.<br>
 * If only get method exists the property is read-only.<br>
 * If both exist the property is read-write. *
 */

class LogConfigImpl
    implements ManagedService, LogConfig, BundleListener {

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

  static final String PROP_LOG_MEMORY_SIZE = "org.knopflerfish.log.memory.size";

  private final static String OUT = "log.out";

  private final static String GRABIO = "log.grabio";

  private final static String L_FILTER = "default.level";

  private final static String BL_FILTERS = "bundle.log.level";

  private final static String DIR = "file.dir";

  private final static String FILE_S = "file.size";

  private final static String FLUSH = "file.flush";

  private final static String PID = "service.pid";

  private final String pid = "org.knopflerfish.bundle.log.LogConfig";

  final static String MEM = "memory.size";

  final static String GEN = "file.generations";

  final static String FILE = "file";

  private final static int LOCATION_POS = 0;

  private final static int FILTER_POS = 1;


  // Local constant copies to avoid having to write fully qulified
  // names in the code below (the LogService here is
  // org.knopflerfish.service.LogService).
  static final int LOG_ERROR = org.osgi.service.log.LogService.LOG_ERROR;
  static final int LOG_WARNING = org.osgi.service.log.LogService.LOG_WARNING;
  static final int LOG_INFO = org.osgi.service.log.LogService.LOG_INFO;
  static final int LOG_DEBUG = org.osgi.service.log.LogService.LOG_DEBUG;


  /* Variables containing configuration. */
  private File dir;

  private final Hashtable configCollection = new Hashtable();

  // Mapping from bundle pattern
  // (location/BundleSymbolicName/BundleName) to log level (Integer),
  // each entry in this map corresponds to an entry in the
  // actual configuration.
  private final HashMap blFilters = new HashMap();

  // Mapping from bundle id to log level. This is a cache computed
  // by evaluate all installed bundles against the current blFilters
  private final Map bidFilters = new HashMap();

  private LogReaderServiceFactory logReaderCallback;

  public LogConfigImpl(BundleContext bc) {
    LogConfigImpl.bc = bc;

    // Initialize with default values.
    checkChange(getDefault());

    bc.addBundleListener(this);
    start();
  }

  synchronized void start() {
    initDir();
    String[] clazzes = new String[]{ManagedService.class.getName(),
        LogConfig.class.getName()};
    bc.registerService(clazzes, this, configCollection);
  }

  private void initDir() {
    dir = bc.getDataFile(""); // default location
    String d = (String) configCollection.get(DIR);
    if (d != null) {
      dir = new File(d);     // location from config
    }
  }

  void init(LogReaderServiceFactory lr) {
    this.logReaderCallback = lr;
  }

  void stop() {
    bc.removeBundleListener(this);
    this.logReaderCallback = null;
  }

  /**
   * ************************************************************************
   * org.knopflerfish.service.log.LogConfig methods
   * ************************************************************************
   */

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
      filter = LOG_WARNING;
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
    return LogUtil.toLevel((String) get(L_FILTER), LOG_WARNING);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFilter(java.lang.String,
   *      int)
   */

  public synchronized void setFilter(String bundleLocation, int filter) {
    bundleLocation = bundleLocation.trim();

    Integer f = (Integer) blFilters.get(bundleLocation);
    if (filter == 0 && f != null) {
      blFilters.remove(bundleLocation);
      setCollection(true, bundleLocation, filter);
    } else if ((f != null && filter != f.intValue()) || f == null) {
      blFilters.put(bundleLocation, new Integer(filter));
      setCollection(false, bundleLocation, filter);
    }
    computeBidFilters();
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

  /**
   * ***** Configuration for filing log entries. ************
   */
  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFile(boolean)
   */
  public synchronized void setFile(boolean f) {
    if ((dir != null)) {
      boolean oldFile = getFile();
      if (f != oldFile) {
        Boolean newFile = f ? Boolean.TRUE : Boolean.FALSE;
        set(FILE, newFile);
        if (logReaderCallback != null) {
          logReaderCallback.configChange(FILE, oldFile ? Boolean.TRUE : Boolean.FALSE,
              newFile);
        }
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

  /**
   * Return the log filter level for the given bundle.
   */
  int getLevel(Bundle bundle) {
    final Long key = new Long(bundle.getBundleId());
    Integer level = null;
    synchronized (bidFilters) {
      level = (Integer) bidFilters.get(key);
    }

    // final PrintStream out = (null!=origOut) ? origOut : System.out;
    // out.println("LogConfigImpl.getLevel(" +key +"): " +level);

    return (level != null) ? level.intValue() : getFilter();
  }

  static String[] getBL(Object obj) {
    String bundleStr = (String) obj;
    String[] bundle = new String[]{null, null};
    int ix = bundleStr.indexOf(";");
    try {
      bundle[0] = bundleStr.substring(0, ix).trim();
      bundle[1] = bundleStr.substring(ix + 1).trim();
    } catch (Exception e) {
      throw new IllegalArgumentException
          ("Bundle entries must be in the format location;level");
    }
    return bundle;
  }

  // (Re-)compute the cache bidFilters from blFilters and the current
  // set of bundles.

  private void computeBidFilters() {
    final Bundle[] bundles = bc.getBundles();
    synchronized (bidFilters) {
      bidFilters.clear();
      for (int i = bundles.length - 1; 0 <= i; i--) {
        final Bundle bundle = bundles[i];
        computeBidFilter(bundle);
      }
    }
  }

  /**
   * Compute and cache a bidFilter entry from blFilters for the given
   * bundle.
   *
   * @param bundle The bundle to update the cached log level for.
   */
  private void computeBidFilter(final Bundle bundle) {
    Integer level = (Integer) blFilters.get(bundle.getLocation());
    if (null == level) {
      final Dictionary d = bundle.getHeaders("");
      String l = (String) d.get("Bundle-SymbolicName");
      if (null == l) {
        l = (String) d.get("Bundle-Name");
      }
      if (null != l) {
        level = (Integer) blFilters.get(l);
      }
    }
    if (null != level) {
      final Long key = new Long(bundle.getBundleId());
      bidFilters.put(key, level);
    }
  }

  // Implements BundleListener

  public void bundleChanged(BundleEvent event) {
    switch (event.getType()) {
      case BundleEvent.INSTALLED: // Fall through
      case BundleEvent.UPDATED:
        synchronized (bidFilters) {
          computeBidFilter(event.getBundle());
        }
        break;
      case BundleEvent.UNINSTALLED:
        final Long key = new Long(event.getBundle().getBundleId());
        synchronized (bidFilters) {
          bidFilters.remove(key);
        }
        break;
      default:
    }
  }

  private void setCollection(boolean remove, String bundleLocation, int filter) {
    synchronized (configCollection) {
      Vector v = (Vector) configCollection.get(BL_FILTERS);
      String[] bundF;
      boolean notFound = true;
      if (v != null && v.size() > 0) {
        for (int i = (v.size() - 1); i >= 0; i--) {
          bundF = getBL(v.elementAt(i));
          if (bundF[LOCATION_POS].equals(bundleLocation)) {
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

  /**
   * *************** Called from set methods *********************
   */

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

    String levelStr = getProperty(PROP_LOG_LEVEL,
        LogUtil.fromLevel(LOG_WARNING));

    ht.put(L_FILTER, levelStr);
    ht.put(MEM, getIntegerProperty(PROP_LOG_MEMORY_SIZE, new Integer(250)));
    ht.put(OUT, ("true".equalsIgnoreCase(o)) ? Boolean.TRUE : Boolean.FALSE);
    ht.put(GRABIO,
        ("true".equalsIgnoreCase(getProperty(PROP_LOG_GRABIO, "false")) ? Boolean.TRUE : Boolean.FALSE));
    ht.put(FILE, ("true".equalsIgnoreCase(getProperty(PROP_LOG_FILE, "false")) ? Boolean.TRUE : Boolean.FALSE));
    String dirStr = getProperty(PROP_LOG_FILE_DIR, null);
    if (dirStr != null)
      ht.put(DIR, dirStr);
    ht.put(FILE_S, new Integer(20000));
    ht.put(GEN, new Integer(4));
    ht.put(FLUSH, Boolean.TRUE);
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

  static Integer getIntegerProperty(String key, Integer def) {
    Integer result = def;
    try {
      final String str = bc.getProperty(key);
      if (str != null && 0 < str.length()) {
        try {
          result = new Integer(str);
        } catch (NumberFormatException nfe) {
          System.err.println("Failed to parse integer property with key '"
              + key + "' and value '" + str + "': " + nfe);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to get property " + key + " : " + e);
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
        System.setOut(new WrapStream("[stdout] ", System.out, LOG_INFO));
        System.setErr(new WrapStream("[stderr] ", System.out, LOG_ERROR));
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

  class WrapStream
      extends PrintStream {
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
  /* Methods for setting incoming configuration locally. */
  /*------------------------------------------------------------------------*/
  /*
   * Check that incoming configuration is correct. If not an exception will be
   * thrown and the default configuration or the former configuration will be
   * used for ALL properties, i.e., no property will be set if one item in the
   * configuration is invalid.
   */

  private void checkValidity(Dictionary cfg)
      throws ConfigurationException, IllegalArgumentException {
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
            // Rollback. E.g., configCollection = rollBack;
            synchronized (configCollection) {
              for (Enumeration keys = rollBack.keys(); keys.hasMoreElements();) {
                final Object key = keys.nextElement();
                final Object value = rollBack.remove(key);
                configCollection.put(key, value);
              }
            }
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

  private void checkLogLevel(Dictionary cfg)
      throws ConfigurationException, IllegalArgumentException {
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
      cfg.put(L_FILTER, LogUtil.fromLevel(LOG_WARNING));
    }
  }

  /* Check bundle log level property for faults. */

  private void checkBundleLogLevel(Dictionary cfg)
      throws ConfigurationException, IllegalArgumentException {
    Vector v = null;
    try {
      v = (Vector) cfg.get(BL_FILTERS);
    } catch (ClassCastException cce) {
      throw new IllegalArgumentException
          ("Wrong type supplied when attempting to set log level for "
              + "specific bundle. Correct type to use is Vector of String[].");
    }
    if (v != null) {
      String[] bundle = null;
      for (int i = 0; i < v.size(); i++) {
        try {
          bundle = getBL(v.elementAt(i));
        } catch (ClassCastException cce) {
          throw new IllegalArgumentException
              ("Wrong type supplied when attempting to set log level for "
                  + "specific bundle. Correct type to use is String.");
        }
        if (bundle == null) {
          throw new IllegalArgumentException
              ("Empty configuration supplied when attempting to set log level "
                  + " for specific bundle.");
        }
        bundle[LOCATION_POS] = bundle[LOCATION_POS].trim();
        if (bundle[LOCATION_POS] == null
            || bundle[LOCATION_POS].length() <= 0) {
          throw new IllegalArgumentException
              ("No bundle location given when setting log level for specific "
                  + "bundle.");
        }
        if (bundle[FILTER_POS] == null) {
          throw new IllegalArgumentException
              ("No log level given for bundle: "
                  + bundle[LOCATION_POS] + ". "
                  + "Please supply a valid log level.");
        }
        int testFilter = 0;
        testFilter = LogUtil.toLevel((bundle[FILTER_POS].trim()), -1);
        if (testFilter == -1) {
          throw new ConfigurationException
              (BL_FILTERS,
                  "Undefined log level <" + bundle[FILTER_POS]
                      + "> specified for bundle <"
                      + bundle[LOCATION_POS] + ">.");
        }
        if (testFilter == 0) {
          v.removeElementAt(i);
          i--;
        } else {
          bundle[FILTER_POS] = LogUtil.fromLevel(testFilter);
          // There is no safe way to do any further validation of the
          // location since any string is a valid location.
        }
      }
    }
  }

  /*
   * Called when an incoming configuration seems correct. Should the
   * checkChange method throw an exception the configuration will be
   * reset to the former valid state.
   */

  private void acceptConfig(Dictionary cfg) {
    firstValid = DEFAULT_CONFIG;
    DEFAULT_CONFIG = false;
    checkChange(cfg);
  }

  /*
   * Checking which property actually changed. Called once the
   * validity of the incoming configuration has been checked. If some
   * property changed notify about change.
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
        final String pValue = getProperty(PROP_LOG_OUT, "false");
        set(OUT,
            "true".equalsIgnoreCase(pValue) ? Boolean.TRUE : Boolean.FALSE);
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

    if ((newV = diff(GRABIO, cfg)) != null) {
      set(GRABIO, newV);
    }

    if ((newV = diff(PID, cfg)) != null) {
      set(PID, newV);
    }
  }

  /*
   * Check bundle log level and see if changes has been made. If so change
   * internal representation of bundle log levels.
   */

  private void setFilterCfg(Vector newV) {
    if (newV != null) {
      String[] bundle = null;
      int newFilter = -1;
      Set newKeys = new HashSet();
      HashMap tmpFilters = new HashMap();
      for (int i = 0; (i < newV.size()); i++) {
        bundle = getBL(newV.elementAt(i));
        newFilter = LogUtil.toLevel((bundle[FILTER_POS].trim()), LOG_WARNING);
        blFilters.put(bundle[LOCATION_POS], new Integer(newFilter));
        newKeys.add(bundle[LOCATION_POS]);
      }
      // Remove obsolete bl filter mappings.
      Set obsoleteKeys = new HashSet(blFilters.keySet());
      obsoleteKeys.removeAll(newKeys);
      for (Iterator okit = obsoleteKeys.iterator(); okit.hasNext();) {
        blFilters.remove(okit.next());
      }
      set(BL_FILTERS, newV);
      computeBidFilters();
    }
  }

  private Object diff(String key, Dictionary cfg) {
    Object newV = null;
    return ((newV = cfg.get(key)) != null
        && !newV.equals(configCollection.get(key))) ? newV : null;
  }

  private void notify(String key, Object newV) {
    if (logReaderCallback != null) {
      logReaderCallback
          .configChange(key, configCollection.get(key), newV);
    }
  }

  private void log(String msg) {
    if (logReaderCallback != null) {
      logReaderCallback.log(new LogEntryImpl(bc.getBundle(), LOG_INFO, msg));
    }
  }
}
