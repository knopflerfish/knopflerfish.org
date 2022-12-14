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

package org.knopflerfish.bundle.log;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import org.knopflerfish.service.log.LogConfig;
import org.knopflerfish.service.log.LogUtil;

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
 * If both exist the property is read-write.
 */

class LogConfigImpl
    implements ManagedService, LogConfig, BundleListener
{

  private static final String DEFAULT_TIMESTAMP_PATTERN = "yyyyMMdd HH:mm:ss";
  private static final String pid = "org.knopflerfish.bundle.log.LogConfig";

  static BundleContext bc;

  /*
   * Variables indicating whether CM configuration has been received.
   */
  boolean isDefaultConfig = true;

  static final String PROP_LOG_FILE = "org.knopflerfish.log.file";
  static final String PROP_LOG_FILE_DIR = "org.knopflerfish.log.file.dir";
  static final String PROP_LOG_GRABIO = "org.knopflerfish.log.grabio";
  static final String PROP_LOG_LEVEL = "org.knopflerfish.log.level";
  static final String PROP_LOG_MEMORY_SIZE = "org.knopflerfish.log.memory.size";
  static final String PROP_LOG_OUT = "org.knopflerfish.log.out";
  static final String PROP_LOG_TIMESTAMP_PATTERN
    = "org.knopflerfish.log.timestamp.pattern";

  // Keys for properties in the configuration
  final static String DIR = "file.dir";
  final static String FILE = "file";
  final static String GEN = "file.generations";
  final static String MEM = "memory.size";
  final static String TIMESTAMP_PATTERN = "timestamp.pattern";
  private final static String BL_FILTERS = "bundle.log.level";
  private final static String FILE_S = "file.size";
  private final static String FLUSH = "file.flush";
  private final static String GRABIO = "log.grabio";
  private final static String L_FILTER = "default.level";
  private final static String OUT = "log.out";
  private final static String PID = "service.pid";
  private final static int FILTER_POS = 1;
  private final static int LOCATION_POS = 0;


  // Local constant copies to avoid having to write fully qulified
  // names in the code below (the LogService here is
  // org.knopflerfish.service.LogService).
  static final int LOG_ERROR = org.osgi.service.log.LogService.LOG_ERROR;
  static final int LOG_WARNING = org.osgi.service.log.LogService.LOG_WARNING;
  static final int LOG_INFO = org.osgi.service.log.LogService.LOG_INFO;
  static final int LOG_DEBUG = org.osgi.service.log.LogService.LOG_DEBUG;


  /* Variables containing configuration. */

  /** The directory that the file log is written to. */
  private File dir;

  private final Hashtable<String,Object> configCollection = new Hashtable<>();

  // Mapping from bundle pattern
  // (location/BundleSymbolicName/BundleName) to log level (Integer),
  // each entry in this map corresponds to an entry in the
  // actual configuration.
  private final HashMap<String, Integer> blFilters = new HashMap<>();

  // Mapping from bundle id to log level. This is a cache computed
  // by evaluate all installed bundles against the current blFilters
  private final Map<Long, Integer> bidFilters = new HashMap<>();

  private LogReaderServiceFactory logReaderCallback;

  public LogConfigImpl(BundleContext bc) {
    LogConfigImpl.bc = bc;

    // Initialize with default values.
    applyConfig(getDefault());

    bc.addBundleListener(this);
    start();
  }

  synchronized void start() {
    dir = initDir((String) configCollection.get(DIR));
    final String[] clazzes = new String[]{ManagedService.class.getName(),
        LogConfig.class.getName()};
    bc.registerService(clazzes, this, configCollection);
  }

  private File initDir(final String d) {
    if (d != null && 0 < d.length()) {
      // location from config
      final File cfgFile = new File(d.trim());
      if (cfgFile.exists()) {
        if (cfgFile.isDirectory()) {
          return cfgFile;
        }
      } else {
        if (cfgFile.mkdirs()) {
          return cfgFile;
        }
      }
    }
    // Fallback to the default location
    return bc.getDataFile("");
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
  @Override
  public synchronized void commit() {
    updateConfig();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#isDefaultConfig()
   */

  @Override
  public boolean isDefaultConfig() {
    return isDefaultConfig;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setMemorySize(int)
   */

  @Override
  public synchronized void setMemorySize(int size) {
    final int oldSize = getMemorySize();
    if (size != oldSize) {
      final Integer newSize = size;
      notify(MEM, newSize);
      set(MEM, newSize);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getMemorySize()
   */

  @Override
  public int getMemorySize() {
    return (Integer) get(MEM);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFilter(int)
   */

  @Override
  public synchronized void setFilter(int filter) {
    final int oldFilter = getFilter();
    if (filter == 0) {
      filter = LOG_WARNING;
    }
    if (filter != oldFilter) {
      set(L_FILTER, LogUtil.fromLevel(filter));
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFilter()
   */

  @Override
  public int getFilter() {
    return LogUtil.toLevel((String) get(L_FILTER), LOG_WARNING);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFilter(java.lang.String,
   *      int)
   */

  @Override
  public synchronized void setFilter(String bundleLocation, int filter) {
    bundleLocation = bundleLocation.trim();

    final Integer f = blFilters.get(bundleLocation);
    if (filter == 0 && f != null) {
      blFilters.remove(bundleLocation);
      setCollection(true, bundleLocation, filter);
    } else if (f == null || filter != f) {
      blFilters.put(bundleLocation, filter);
      setCollection(false, bundleLocation, filter);
    }
    computeBidFilters();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFilters()
   */

  @Override
  public synchronized HashMap<String, Integer> getFilters() {
    return new HashMap<>(blFilters);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setOut(boolean)
   */

  @Override
  public synchronized void setOut(boolean b) {
    final boolean oldOut = getOut();
    if (b != oldOut) {
      set(OUT, b);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getOut()
   */

  @Override
  public boolean getOut() {
    return (Boolean) get(OUT);
  }

  /**
   * ***** Configuration for filing log entries. ************
   */
  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFile(boolean)
   */
  @Override
  public synchronized void setFile(boolean f) {
    if ((dir != null)) {
      final boolean oldFile = getFile();
      if (f != oldFile) {
        final Boolean newFile = f ? Boolean.TRUE : Boolean.FALSE;
        notify(FILE, newFile);
        set(FILE, newFile);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFile()
   */

  @Override
  public boolean getFile() {
    return ((Boolean) get(FILE) && (getDir() != null));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getDir()
   */

  @Override
  public synchronized File getDir() {
    return dir;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFileSize(int)
   */

  @Override
  public synchronized void setFileSize(int fS) {
    final int oldSize = getFileSize();
    if (oldSize != fS) {
      set(FILE_S, fS);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFileSize()
   */

  @Override
  public int getFileSize() {
    return (Integer) get(FILE_S);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setMaxGen(int)
   */

  @Override
  public synchronized void setMaxGen(final int maxGen) {
    final int oldGen = getMaxGen();
    if (oldGen != maxGen) {
      final Integer newGen = maxGen;
      notify(GEN, newGen);
      set(GEN, newGen);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getMaxGen()
   */

  @Override
  public int getMaxGen() {
    final int gen = (Integer) get(GEN);
    return Math.max(gen, 1);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setFlush(boolean)
   */

  @Override
  public synchronized void setFlush(boolean f) {
    final boolean oldFlush = getFlush();
    if (f != oldFlush) {
      set(FLUSH, f);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getFlush()
   */

  @Override
  public boolean getFlush() {
    return (Boolean) get(FLUSH);
  }


  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#setTimestampPattern(String)
   */
  @Override
  public void setTimestampPattern(String pattern)
  {
    final String oldPattern = getTimestampPattern();
    if (null==pattern) {
      pattern = DEFAULT_TIMESTAMP_PATTERN;
    }
    if (!pattern.equals(oldPattern)) {
      try {
        // Pattern must be valid for set to do anything
        new SimpleDateFormat(pattern);
        notify(TIMESTAMP_PATTERN, pattern);
        set(TIMESTAMP_PATTERN, pattern);
      } catch (final Throwable t) {
        // Keep the old pattern.
      }
    }
  }


  /*
   * (non-Javadoc)
   *
   * @see org.knopflerfish.service.log.LogConfig#getTimestampPattern()
   */
  @Override
  public String getTimestampPattern()
  {
    return (String) get(TIMESTAMP_PATTERN);
  }

  /**
   * Return the log filter level for the given bundle.
   */
  int getLevel(final Bundle bundle) {
    final Long key = bundle.getBundleId();
    Integer level;
    synchronized (bidFilters) {
      level = bidFilters.get(key);
    }

    // final PrintStream out = (null!=origOut) ? origOut : System.out;
    // out.println("LogConfigImpl.getLevel(" +key +"): " +level);

    return (level != null) ? level : getFilter();
  }

  static String[] getBL(final String bundleStr) {
    final String[] bundle = new String[]{null, null};
    final int ix = bundleStr.indexOf(";");
    try {
      bundle[LOCATION_POS] = bundleStr.substring(0, ix).trim();
      bundle[FILTER_POS] = bundleStr.substring(ix + 1).trim();
    } catch (final Exception e) {
      throw new IllegalArgumentException
          ("Bundle entries must be in the format location;level");
    }
    return bundle;
  }

  // (Re-)compute the cache bidFilters from blFilters and the current
  // set of bundles.

  private void computeBidFilters()
  {
    // Use a temporary map to avoid holding lock during the computation.
    final Map<Long, Integer> bidFiltersTmp = new HashMap<>();

    final Bundle[] bundles = bc.getBundles();
    for (int i = bundles.length - 1; 0 <= i; i--) {
      final Bundle bundle = bundles[i];
      computeBidFilter(bidFiltersTmp, bundle);
    }

    // Atomic update...
    synchronized (bidFilters) {
      bidFilters.clear();
      bidFilters.putAll(bidFiltersTmp);
    }
  }

  /**
   * Compute and cache a bidFilter entry from blFilters for the given
   * bundle.
   *
   * @param bidFilters The bundle id to level map to cache the result in
   * @param bundle The bundle to update the cached log level for.
   */
  private void computeBidFilter(final Map<Long, Integer> bidFilters,
                                final Bundle bundle)
  {
    Integer level = blFilters.get(bundle.getLocation());
    if (null == level) {
      String l = getSymbolicName(bundle);
      if (null == l || 0 == l.length()) {
        l = bundle.getHeaders("").get("Bundle-Name");
      }

      if (null != l) {
        level = blFilters.get(l);
      }
    }

    // Finally try with the bundle id as key.
    if (level == null) {
      level = blFilters.get(String.valueOf(bundle.getBundleId()));
    }

    if (null != level) {
      final Long key = bundle.getBundleId();
      synchronized (bidFilters) {
        bidFilters.put(key, level);
      }
    }
  }

  /**
   * Get the symbolic name of the specified bundle. All directives and
   * parameters attached to the symbolic name attribute will be
   * stripped.
   *
   * @param bundle
   *            the bundle
   * @return The bundles symbolic name or null if not specified.
   */
  private static String getSymbolicName(Bundle bundle) {
    if (bundle == null) {
      return null;
    }

    final Dictionary<String, String> d = bundle.getHeaders("");
    String bsn = d.get("Bundle-SymbolicName");
    if (bsn != null && bsn.length() >0) {
      // Remove parameters and directives from the value
      final int semiPos = bsn.indexOf(';');
      if (-1<semiPos) {
        bsn = bsn.substring(0, semiPos).trim();
      }
    }
    return bsn;
  }


  // Implements BundleListener

  @Override
  public void bundleChanged(BundleEvent event) {
    switch (event.getType()) {
      case BundleEvent.INSTALLED: // Fall through
      case BundleEvent.UPDATED:
        computeBidFilter(bidFilters, event.getBundle());
        break;
      case BundleEvent.UNINSTALLED:
        final Long key = event.getBundle().getBundleId();
        synchronized (bidFilters) {
          bidFilters.remove(key);
        }
        break;
      default:
    }
  }

  private void setCollection(boolean remove, String bundleLocation, int filter) {
    synchronized (configCollection) {
      @SuppressWarnings("unchecked")
      final
      Vector<String> v = (Vector<String>) configCollection.get(BL_FILTERS);
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
      if (v != null && notFound && !remove) {
        v.addElement(bundleLocation + ";" + LogUtil.fromLevel(filter));
      }
    }
  }

  boolean getGrabIO() {
    return (Boolean) get(GRABIO);
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
      final ServiceReference<ConfigurationAdmin> sr = bc
          .getServiceReference(ConfigurationAdmin.class);
      if (sr != null) {
        final ConfigurationAdmin ca = bc.getService(sr);
        if (ca != null) {
          final Configuration conf = ca.getConfiguration(pid);
          if (conf != null) {
            conf.update(configCollection);
          }
        }
        bc.ungetService(sr);
      }
    } catch (final IOException | IllegalArgumentException | IllegalStateException ignored) {
    }
  }

  /* ======================================================================== */
  /* Initializes configuration */
  /* ======================================================================== */

  private Hashtable<String, Serializable> getDefault() {
    final Hashtable<String, Serializable> ht = new Hashtable<>();
    final Vector<String> bundleLogFilters = new Vector<>();
    final String o = getProperty(PROP_LOG_OUT, "false");

    final String levelStr = getProperty(PROP_LOG_LEVEL,
                                        LogUtil.fromLevel(LOG_WARNING));

    ht.put(L_FILTER, levelStr);
    ht.put(MEM, getIntegerProperty(PROP_LOG_MEMORY_SIZE, 250));
    ht.put(OUT, ("true".equalsIgnoreCase(o)) ? Boolean.TRUE : Boolean.FALSE);
    ht.put(GRABIO,
        ("true".equalsIgnoreCase(getProperty(PROP_LOG_GRABIO, "false")) ? Boolean.TRUE : Boolean.FALSE));
    ht.put(FILE, ("true".equalsIgnoreCase(getProperty(PROP_LOG_FILE, "false")) ? Boolean.TRUE : Boolean.FALSE));
    ht.put(DIR, getProperty(PROP_LOG_FILE_DIR, ""));
    ht.put(FILE_S, 20000);
    ht.put(GEN, 4);
    ht.put(FLUSH, Boolean.TRUE);
    ht.put(BL_FILTERS, bundleLogFilters);
    ht.put(PID, LogConfigImpl.pid);

    final String timestampPattern = getProperty(PROP_LOG_TIMESTAMP_PATTERN,
                                                DEFAULT_TIMESTAMP_PATTERN);
    try {
      new SimpleDateFormat(timestampPattern);
      ht.put(TIMESTAMP_PATTERN, timestampPattern);
    } catch (final Throwable t) {
      ht.put(TIMESTAMP_PATTERN, DEFAULT_TIMESTAMP_PATTERN);
    }

    return ht;
  }

  static String getProperty(final String key, final String def) {
    String result;
    try {
      result = bc.getProperty(key);
      if (result == null) {
        result = def;
      }
    } catch (final Exception e) {
      System.err.println("Failed to get property " + key + " : " + e);
      result = def;
    }
    return result;
  }

  static Integer getIntegerProperty(final String key, final Integer def) {
    Integer result = def;
    try {
      final String str = bc.getProperty(key);
      if (str != null && 0 < str.length()) {
        try {
          result = new Integer(str);
        } catch (final NumberFormatException nfe) {
          System.err.println("Failed to parse integer property with key '"
              + key + "' and value '" + str + "': " + nfe);
        }
      }
    } catch (final Exception e) {
      System.err.println("Failed to get property " + key + " : " + e);
    }
    return result;
  }

  /* ======================================================================== */
  /* Method implementing the ManagedService interface */
  /* gets called whenever the configuration for this */
  /* ManagedService is updated. */
  /* ======================================================================== */

  @Override
  public synchronized void updated(Dictionary<String, ?> cfg)
      throws ConfigurationException, IllegalArgumentException {
    if (cfg == null) {
      isDefaultConfig = true;
      applyConfig(getDefault());
    } else {
      checkApplyConfig(cfg);
    }

    updateGrabIO();
  }

  boolean bGrabbed = false;
  PrintStream origOut = null;
  PrintStream origErr = null;

  void updateGrabIO() {
    final boolean bDebugClass = "true".equals
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

    @Override
    public void println(String s) {
      super.print(prefix);
      super.println(s);
      log(s);
    }

    @Override
    public void print(String s) {
      super.print(s);
    }

    @Override
    public void println(Object x) {
      super.print(prefix);
      super.println(x);
      log("" + x);
    }

    @Override
    public void print(Object x) {
      super.print(x);
    }

    void log(String s) {
      if (s != null && s.contains(prefix)) {
        return;
      }

      if (logReaderCallback != null) {
        logReaderCallback.log(new LogEntryImpl(bc.getBundle(0),
                                               level,
                                               prefix + s,
                                               null));
      }
    }
  }

  /*------------------------------------------------------------------------*/
  /* Methods for checking incoming configuration, */
  /* Methods for setting incoming configuration locally. */
  /*------------------------------------------------------------------------*/

  /**
   * Check and activate the specified configuration.
   *
   * If the given configuration is invalid an exception will be thrown
   * and the default configuration or the former configuration will be
   * used for ALL properties, i.e., no property will be set if one
   * item in the new configuration is invalid.
   *
   * @param cfg new configuration to validate and apply.
   */
  @SuppressWarnings("unchecked")
  private void checkApplyConfig(Dictionary<String, ?> cfg)
      throws ConfigurationException, IllegalArgumentException
  {
    boolean valid = false;
    final Hashtable<String,Object> rollBack
      = new Hashtable<>(configCollection);
    try {
      checkLogLevel((Dictionary<String, Object>) cfg);
      checkBundleLogLevel(cfg);
      checkTimestampPattern(cfg);
      valid = true;
    } finally {
      if (valid) {
        valid = false;
        try {
          applyConfig(cfg);
          valid = true;
        } catch (final Exception all) {
          throw new ConfigurationException(null,
                                           "Fault occurred when "
                                           +"setting configuration. "
                                           +"Check that all properties "
                                           +"are valid.");
        } finally {
          if (!valid) {
            // Rollback. E.g., configCollection = rollBack;
            synchronized (configCollection) {
              for (final Enumeration<String> keys = rollBack.keys(); keys.hasMoreElements();) {
                final String key = keys.nextElement();
                final Object value = rollBack.remove(key);
                configCollection.put(key, value);
              }
            }
          }
        }
      }
    }
  }

  /* Check log level property for faults. */

  private void checkLogLevel(Dictionary<String, Object> cfg)
      throws ConfigurationException, IllegalArgumentException {
    final Object obj = cfg.get(L_FILTER);
    if (obj == null) {
      throw new IllegalArgumentException(
          "No log level given. Please supply a valid log level.");
    }
    String filter;
    try {
      filter = ((String) obj).trim();
    } catch (final ClassCastException cce) {
      throw new IllegalArgumentException(
          "Wrong type supplied when attempting to set log level."
              + " Correct type to use is String. " + obj + " "
              + obj.getClass().getName());
    }
    final int filterVal = LogUtil.toLevel(filter, -1);
    if (filterVal == -1) {
      throw new ConfigurationException(L_FILTER, "Undefined log level <"
          + filter + ">.");
    }
    if (filterVal == 0) {
      cfg.put(L_FILTER, LogUtil.fromLevel(LOG_WARNING));
    }
  }

  /* Check bundle log level property for faults. */
  private void checkBundleLogLevel(Dictionary<String, ?> cfg)
      throws ConfigurationException, IllegalArgumentException {
    Vector<String> v;
    try {
      @SuppressWarnings("unchecked")
      final Vector<String> v1 = (Vector<String>) cfg.get(BL_FILTERS);
      v = v1;
    } catch (final ClassCastException cce) {
      throw new IllegalArgumentException
          ("Wrong type supplied when attempting to set log level for "
              + "specific bundle. Correct type to use is Vector of String.");
    }
    if (v != null) {
      String[] bundle;
      for (int i = 0; i < v.size(); i++) {
        try {
          bundle = getBL(v.elementAt(i));
        } catch (final ClassCastException cce) {
          throw new IllegalArgumentException
              ("Wrong type supplied when attempting to set log level for "
                  + "specific bundle. Correct type to use is String.");
        }
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
        int testFilter = LogUtil.toLevel(bundle[FILTER_POS], -1);
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


  // Valide the new timestamp pattern
  private void checkTimestampPattern(final Dictionary<String, ?> cfg)
      throws ConfigurationException, IllegalArgumentException {
    final Object obj = cfg.get(TIMESTAMP_PATTERN);
    try {
      final String pattern = (String) obj;
      try {
        new SimpleDateFormat(pattern);
      } catch (final Throwable t) {
        throw new ConfigurationException(TIMESTAMP_PATTERN,
                                         "Invalid timestamp pattern: '"
                                         +pattern +"' "+t.getMessage());
      }
    } catch (final ClassCastException cce) {
      throw new IllegalArgumentException(
          "Wrong type supplied when attempting to set timestamp pattern."
              + " Correct type to use is String. " + obj + " "
              + obj.getClass().getName());
    }
  }

  /**
   * Copy changed values from the specified configuration to the active
   * configuration object. Notify about changed properties (those that the
   * LogReaderServiceFactory needs to handle explicitly).
   *
   * Called once the validity of the given configuration has been checked.
   *
   * @param cfg
   *          The new, validated configuration to apply.
   */
  private void applyConfig(final Dictionary<String, ?> cfg) {
    @SuppressWarnings("unchecked")
    final
    Vector<String> bls = (Vector<String>) cfg.get(BL_FILTERS);
    setFilterCfg(bls);

    Object newV;
    if ((newV = diff(L_FILTER, cfg)) != null) {
      set(L_FILTER, newV);
    }
    if ((newV = diff(MEM, cfg)) != null) {
      notify(MEM, newV);
      set(MEM, newV);
    }
    if ((newV = diff(OUT, cfg)) != null) {
      set(OUT, newV);
    }
    if ((newV = diff(DIR, cfg)) != null) {
      final File newDir = initDir((String) newV);
      if (dir != null) {
        if (!dir.getAbsolutePath().equals(newDir.getAbsolutePath())) {
          synchronized(this) {
            dir = newDir;
          }
          notify(DIR, newV);
        }
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
        notify(FILE, newV);
      }
      set(FILE, newV);
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

    if ((newV = diff(TIMESTAMP_PATTERN, DEFAULT_TIMESTAMP_PATTERN, cfg)) != null) {
      notify(TIMESTAMP_PATTERN, newV);
      set(TIMESTAMP_PATTERN, newV);
    }
  }

  /*
   * Check bundle log level and see if changes has been made. If so change
   * internal representation of bundle log levels.
   */

  private void setFilterCfg(Vector<String> newV) {
    if (newV != null) {
      String[] bundle;
      int newFilter;
      final Set<String> newKeys = new HashSet<>();
      for (int i = 0; (i < newV.size()); i++) {
        bundle = getBL(newV.elementAt(i));
        newFilter = LogUtil.toLevel((bundle[FILTER_POS]), LOG_WARNING);
        blFilters.put(bundle[LOCATION_POS], newFilter);
        newKeys.add(bundle[LOCATION_POS]);
      }
      // Remove obsolete bl filter mappings.
      final Set<String> obsoleteKeys = new HashSet<>(blFilters.keySet());
      obsoleteKeys.removeAll(newKeys);
      for (final String string : obsoleteKeys) {
        blFilters.remove(string);
      }
      set(BL_FILTERS, newV);
      computeBidFilters();
    }
  }

  private Object diff(String key, Dictionary<String, ?> cfg) {
    Object newV;
    return ((newV = cfg.get(key)) != null
        && !newV.equals(configCollection.get(key))) ? newV : null;
  }

  private Object diff(String key, Object defV, Dictionary<String, ?> cfg) {
    Object newV = cfg.get(key);
    if (null==newV) {
      newV = defV;
    }
    return (newV != null
        && !newV.equals(configCollection.get(key))) ? newV : null;
  }


  /**
   * Notify the log reader service factory about a changed
   * configuration value. Should only be called for configuration
   * properties that the log reader service factory reacts too, see
   * {@link LogReaderServiceFactory#configChange(String, Object, Object)}
   *
   * This method must be called before {@link #set(String,Object)}
   * since it needs to get the old value from the current configuration.
   *
   * @param key the key of the configuration property that has changed
   * @param newV the new value of the property.
   */
  private void notify(String key, Object newV) {
    if (logReaderCallback != null) {
      logReaderCallback
          .configChange(key, configCollection.get(key), newV);
    }
  }

}
