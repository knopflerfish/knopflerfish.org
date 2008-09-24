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

package org.knopflerfish.framework;

import java.io.*;
import java.net.*;
import java.security.*;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class contains references to all common data structures
 * inside the framework.
 *
 * @author Jan Stein, Erik Wistrand, Philippe Laporte,
 *         Mats-Ola Persson, Gunnar Ekolin
 */
public class Framework {

  /**
   * The "System" properties for this framework instance.  Allways use
   * <tt>Framework.setProperty(String,String)</tt> to add values to
   * this map.
   */
  protected static Map/*<String, String>*/ props
    = new HashMap/*<String, String>*/();

  /**
   * The set of properties that must not be present in props, since a
   * bundle is allowed to update them and such updates are required to
   * be visible when calling <tt>BundleContext.getProperty(String)</tt>.
   */
  private static Set volatileProperties = new HashSet();
  static
  {
    // See last paragraph of section 3.3.1 in the R4.0.1 and R4.1 core spec.
    volatileProperties.add(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
  }


  /**
   * Specification version for this framework.
   */
  static final String SPEC_VERSION = "1.3";

  /**
   * Boolean indicating that framework is running.
   */
  boolean active;

  /**
   * Set during shutdown process.
   */
  boolean shuttingdown /*= false*/;

  /**
   * All bundle in this framework.
   */
  public Bundles bundles;

  /**
   * All listeners in this framework.
   */
  Listeners listeners;

  /**
   * All exported and imported packages in this framework.
   */
  Packages packages;

  /**
   * All registered services in this framework.
   */
  Services services;

  /**
   * PermissionOps handle.
   */
  PermissionOps perm;


  /**
   * System bundle
   */
  SystemBundle systemBundle;

  BundleContextImpl systemBC;

  /**
   * Bundle Storage
   */
  BundleStorage storage;

  /**
   * Private Bundle Data Storage
   */
  FileTree dataStorage /*= null*/;

  /**
   * Main handle so that main doesn't get GCed.
   */
  Object mainHandle;

  /**
   * The start level service.
   */
  StartLevelImpl startLevelService;


  /**
   * Factory for handling service-based URLs
   */
  ServiceURLStreamHandlerFactory urlStreamHandlerFactory;

  /**
   * Factory for handling service-based URL contents
   */
  ServiceContentHandlerFactory   contentHandlerFactory;

  /**
   * Property constants for the framework.
   */
  final static String TRUE   = "true";
  final static String FALSE  = "false";

  // EXIT_ON_SHUTDOWN and USING_WRAPPER_SCRIPT  must be initialized 
  // before initProperties(). Thus, they are *not* possible
  // to set on a per-framework basis (which wouldn't make sense anyway).
  final static boolean EXIT_ON_SHUTDOWN =
    TRUE.equals(Framework.getProperty(Main.EXITONSHUTDOWN_PROP, TRUE));

  final static boolean USING_WRAPPER_SCRIPT
    = TRUE.equals(Framework.getProperty(Main.USINGWRAPPERSCRIPT_PROP, FALSE));


  static {
    initProperties();
  }

  final static String osArch = Framework.getProperty("os.arch");
  final static String osName = Framework.getProperty("os.name");
  static String osVersion;

  // If set to true, then during the UNREGISTERING event the Listener
  // can use the ServiceReference to receive an instance of the service.
  public final static boolean UNREGISTERSERVICE_VALID_DURING_UNREGISTERING =
    TRUE.equals(Framework.getProperty("org.knopflerfish.servicereference.valid.during.unregistering",
                                     TRUE));

  // If set to true, set the bundle startup thread's context class
  // loader to the bundle class loader. This is useful for tests
  // but shouldn't really be used in production.
  final static boolean SETCONTEXTCLASSLOADER =
    TRUE.equals(Framework.getProperty("org.knopflerfish.osgi.setcontextclassloader", FALSE));

  final static boolean REGISTERSERVICEURLHANDLER =
    TRUE.equals(Framework.getProperty("org.knopflerfish.osgi.registerserviceurlhandler", TRUE));



  static boolean bIsMemoryStorage /*= false*/;

  static String whichStorageImpl;

  private static final String USESTARTLEVEL_PROP = "org.knopflerfish.startlevel.use";

  /**
   * The file where we store the class path
   */
  private final static String CLASSPATH_DIR = "classpath";
  private final static String BOOT_CLASSPATH_FILE = "boot";
  private final static String FRAMEWORK_CLASSPATH_FILE = "framework";

  /** Cached value of
   * Framework.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)
   * Used and updated by isValidEE()
   */
  private Set    eeCacheSet = new HashSet();
  private String eeCache = null;

  /**
   * Whether the framework supports extension bundles or not.
   * This will be false if bIsMemoryStorage is false.
   */
  static boolean SUPPORTS_EXTENSION_BUNDLES;


  final static int EXIT_CODE_NORMAL  = 0;
  final static int EXIT_CODE_RESTART = 200;


  public static int javaVersionMajor = -1;
  public static int javaVersionMinor = -1;
  public static int javaVersionMicro = -1;
  static {
    String javaVersion = Framework.getProperty("java.version");
    // Value is on the form M.N.U_P[-xxx] where M,N,U,P are decimal integers
    if (null!=javaVersion) {
      int startPos = 0;
      int endPos   = 0;
      int max      = javaVersion.length();
      while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))) {
        endPos++;
      }
      if (startPos<endPos) {
        try {
          javaVersionMajor
            = Integer.parseInt(javaVersion.substring(startPos,endPos));
          startPos = endPos +1;
          endPos   = startPos;
          while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))) {
            endPos++;
          }
          if (startPos<endPos) {
            javaVersionMinor
              = Integer.parseInt(javaVersion.substring(startPos,endPos));
            startPos = endPos +1;
            endPos   = startPos;
            while (endPos<max && Character.isDigit(javaVersion.charAt(endPos))){
              endPos++;
            }
            if (startPos<endPos) {
              javaVersionMicro
                = Integer.parseInt(javaVersion.substring(startPos,endPos));
            }
          }
        } catch (NumberFormatException _nfe) {
        }
      }
    }
  }

  /**
   * Is it safe to use double-checked locking or not.
   * It is safe if JSR 133 is included in the running JRE.
   * I.e., for Java SE if version is 1.5 or higher.
   */
  public final static boolean isDoubleCheckedLockingSafe
    = "true".equals(Framework.getProperty
                    ("org.knopflerfish.framework.is_doublechecked_locking_safe",
                     (javaVersionMajor>=1 && javaVersionMinor>=5
                      ? "true" : "false")));



  /**
   * Contruct a framework.
   *
   */
  public Framework(Object m) throws Exception {



    ProtectionDomain pd = null;
    if (System.getSecurityManager() != null) {
      try {
        pd = getClass().getProtectionDomain();
      } catch (Throwable t) {
        if(Debug.classLoader) {
          Debug.println("Failed to get protection domain: " + t);
        }
      }
      perm = new SecurePermissionOps(this);
    } else {
      perm = new PermissionOps();
    }


    Class storageImpl = Class.forName(whichStorageImpl);
    storage           = (BundleStorage)storageImpl.newInstance();

    dataStorage       = Util.getFileStorage("data");
    packages          = new Packages(this);

    listeners         = new Listeners(perm);
    services          = new Services(perm);

    systemBundle      = new SystemBundle(this, pd);
    systemBC          = new BundleContextImpl(systemBundle);
    bundles           = new Bundles(this);

    systemBundle.setBundleContext(systemBC);

    perm.registerService();

    String[] classes = new String [] { PackageAdmin.class.getName() };
    services.register(systemBundle,
                      classes,
                      new PackageAdminImpl(this),
                      null);

    registerStartLevel();

    urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
    contentHandlerFactory   = new ServiceContentHandlerFactory(this);

    urlStreamHandlerFactory
      .setURLStreamHandler(BundleURLStreamHandler.PROTOCOL,
                           new BundleURLStreamHandler(bundles, perm));
    urlStreamHandlerFactory
      .setURLStreamHandler(ReferenceURLStreamHandler.PROTOCOL,
                           new ReferenceURLStreamHandler());

    // Install service based URL stream handler. This can be turned
    // off if there is need
    if(REGISTERSERVICEURLHANDLER) {
      try {
        URL.setURLStreamHandlerFactory(urlStreamHandlerFactory);

        URLConnection.setContentHandlerFactory(contentHandlerFactory);
      } catch (Throwable e) {
        Debug.println("Cannot set global URL handlers, continuing without OSGi service URL handler (" + e + ")");
        e.printStackTrace();
      }
    }
    bundles.load();

    mainHandle = m;
  }




  private void registerStartLevel(){
    String useStartLevel = Framework.getProperty(USESTARTLEVEL_PROP, TRUE);

    if(TRUE.equals(useStartLevel)) {
      if(Debug.startlevel) {
        Debug.println("[using startlevel service]");
      }
      startLevelService = new StartLevelImpl(this);

      // restoreState just reads from persistent storage
      // open() needs to be called to actually do the work
      // This is done after framework has been launched.
      startLevelService.restoreState();

      services.register(systemBundle,
                        new String [] { StartLevel.class.getName() },
                        startLevelService,
                        null);
    }
  }


  /**
   * Start this Framework.
   * This method starts all the bundles that were started at
   * the time of the last shutdown.
   *
   * <p>If the Framework is already started, this method does nothing.
   * If the Framework is not started, this method will:
   * <ol>
   * <li>Enable event handling. At this point, events can be delivered to
   * listeners.</li>
   * <li>Attempt to start all bundles marked for starting as described in the
   * {@link Bundle#start} method.
   * Reports any exceptions that occur during startup using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Set the state of the Framework to <i>active</i>.</li>
   * <li>Broadcasting a <code>FrameworkEvent</code> through the
   * <code>FrameworkListener.frameworkStarted</code> method.</li>
   * </ol></p>
   *
   * <p>If this Framework is not launched, it can still install,
   * uninstall, start and stop bundles.  (It does these tasks without
   * broadcasting events, however.)  Using Framework without launching
   * it allows for off-line debugging of the Framework.</p>
   *
   * @param startBundle If it is specified with a value larger than 0,
   *                    then the bundle with that id is started.
   *                    Otherwise start all suspended bundles.
   */
  public void launch(long startBundle) throws BundleException {
    if (!active) {
      active = true;
      if (startBundle > 0) {
        startBundle(startBundle);
      } else {
        for (Iterator i = storage.getStartOnLaunchBundles().iterator(); i.hasNext(); ) {
          Bundle b = bundles.getBundle((String)i.next());
          try {
            b.start();
          } catch (BundleException be) {
            listeners.frameworkError(b, be);
          }
        }
      }
      // start level open is delayed to this point to
      // correctly work at restart
      if (startLevelService != null) {
        startLevelService.open();
      }

      systemBundle.systemActive();
      listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED,
                                                  systemBundle, null));
    }
  }


  /**
   * Stop this Framework, suspending all started contexts.
   * This method suspends all started contexts so that they can be
   * automatically restarted when this Framework is next launched.
   *
   * <p>If the framework is not started, this method does nothing.
   * If the framework is started, this method will:
   * <ol>
   * <li>Set the state of the Framework to <i>inactive</i>.</li>
   * <li>Suspended all started bundles as described in the
   * {@link Bundle#stop} method except that the persistent
   * state of the bundle will continue to be started.
   * Reports any exceptions that occur during stopping using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol></p>
   *
   */
  public void shutdown() {
    if (active) {
      // No shuttingdown event specified
      // listeners.frameworkChanged(new FrameworkEvent(FrameworkEvent.SHUTTING_DOWN));
      active = false;
      List slist = storage.getStartOnLaunchBundles();
      shuttingdown = true;
      systemBundle.systemShuttingdown();
      if (startLevelService != null) {
        startLevelService.shutdown();
      }
      // Stop bundles, in reverse start order
      for (int i = slist.size()-1; i >= 0; i--) {
        Bundle b = bundles.getBundle((String)slist.get(i));
        try {
          if(b != null) {
            synchronized (b) {
              if (b.getState() == Bundle.ACTIVE) {
                b.stop();
              }
            }
          }
        } catch (BundleException be) {
          listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, b, be));
        }
      }
      shuttingdown = false;
      // Purge any unrefreshed bundles
      List all = bundles.getBundles();
      for (Iterator i = all.iterator(); i.hasNext(); ) {
        ((BundleImpl)i.next()).purge();
      }
    }

    StringBuffer bootClasspath = new StringBuffer();
    StringBuffer frameworkClasspath = new StringBuffer();
    for (Iterator i = bundles.getFragmentBundles(systemBundle).iterator(); i.hasNext(); ) {
      BundleImpl eb = (BundleImpl)i.next();
      String path = eb.archive.getJarLocation();
      StringBuffer sb = eb.isBootClassPathExtension() ? bootClasspath : frameworkClasspath;
      sb.append(path);
      if (i.hasNext()) {
        sb.append(File.pathSeparator);
      }
    }

    try {
      FileTree storage = Util.getFileStorage(CLASSPATH_DIR);
      File bcpf = new File(storage, BOOT_CLASSPATH_FILE);
      File fcpf = new File(storage, FRAMEWORK_CLASSPATH_FILE);
      if (bootClasspath.length() > 0) {
        saveStringBuffer(bcpf, bootClasspath);
      } else {
        bcpf.delete();
      }
      if (frameworkClasspath.length() > 0) {
        saveStringBuffer(fcpf, frameworkClasspath);
      } else {
        fcpf.delete();
      }
    } catch (IOException e) {
      System.err.println("Could not save classpath " + e);
    }
  }


  private void saveStringBuffer(File f, StringBuffer content) throws IOException {
    PrintStream out = null;
    try {
      out = new PrintStream(new FileOutputStream(f));
      out.println(content.toString());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }


  /**
   * Install a bundle from the given location.
   *
   * @param location The location identifier of the bundle to install.
   * @param in The InputStream from which the bundle will be read.
   * @return The BundleImpl object of the installed bundle.
   * @exception BundleException If the install failed.
   */
  public long installBundle(String location, InputStream in) throws BundleException {
    return bundles.install(location, in).id;
  }


  /**
   * Start a bundle.
   *
   * @param id Id of bundle to start.
   * @exception BundleException If start failed.
   */
  public void startBundle(long id) throws BundleException {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.start();
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Stop a bundle.
   *
   * @param id Id of bundle to stop.
   * @exception BundleException If stop failed.
   */
  public void stopBundle(long id) throws BundleException {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.stop();
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Uninstall a bundle.
   *
   * @param id Id of bundle to stop.
   * @exception BundleException If uninstall failed.
   */
  public void uninstallBundle(long id) throws BundleException {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.uninstall();
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Update a bundle.
   *
   * @param id Id of bundle to update.
   * @exception BundleException If update failed.
   */
  public void updateBundle(long id) throws BundleException {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      b.update();
    } else {
      throw new BundleException("No such bundle: " + id);
    }
  }


  /**
   * Retrieve location of the bundle that has the given
   * unique identifier.
   *
   * @param id The identifier of the bundle to retrieve.
   * @return A location as a string, or <code>null</code>
   * if the identifier doesn't match any installed bundle.
   */
  public String getBundleLocation(long id) {
    Bundle b = bundles.getBundle(id);
    if (b != null) {
      return b.getLocation();
    } else {
      return null;
    }
  }

  /**
   * Retrieve bundle id of the bundle that has the given
   * unique location.
   *
   * @param location The location of the bundle to retrieve.
   * @return The unique identifier of the bundle, or <code>-1</code>
   * if the location doesn't match any installed bundle.
   */
  public long getBundleId(String location) {
    Bundle b = bundles.getBundle(location);
    if (b != null) {
      return b.getBundleId();
    } else {
      return -1;
    }
  }

  /**
   * Get private bundle data storage file handle.
   */
  public FileTree getDataStorage(long id) {
        if (dataStorage != null) {
          return new FileTree(dataStorage, Long.toString(id));
        }
        return null;
  }

  /**
   * Check if an execution environment string is accepted
   */
  boolean isValidEE(String ee) {
    ee = ee.trim();
    if(ee == null || "".equals(ee)) {
      return true;
    }

    String fwEE = Framework.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

    if(fwEE == null) {
      // If EE is not set, allow everything
      return true;
    } else if (!fwEE.equals(eeCache)) {
      eeCacheSet.clear();

      String[] l = Util.splitwords(fwEE, ",");
      for(int i = 0 ; i < l.length; i++) {
        eeCacheSet.add(l[i]);
      }
      eeCache = fwEE;
    }

    String[] eel   = Util.splitwords(ee, ",");
    for(int i = 0 ; i < eel.length; i++) {
      if(eeCacheSet.contains(eel[i])) {
        return true;
      }
    }
    return false;
  }

  //
  // Static package methods
  //


  /**
   * Retrieve the value of the named framework property.
   *
   */
  public static String getProperty(String key) {
    return getProperty(key, null);
  }

  /**
   * Retrieve the value of the named framework property, with a default value.
   *
   */
  public static String getProperty(String key, String def) {
    String v = (String)props.get(key);
    if(v != null) {
      return v;
    } else {
      // default to system property
      return System.getProperty(key, def);
    }
  }

  public static void setProperty(String key, String val) {
    if (volatileProperties.contains(key)) {
      System.setProperty(key,val);
    } else {
      props.put(key, val);
    }
  }

  public static void setProperties(Dictionary newProps) {
    for(Enumeration it = newProps.keys(); it.hasMoreElements(); ) {
      String key = (String)it.nextElement();
      setProperty(key, (String)newProps.get(key));
    }
  }

  public static Dictionary getProperties(){
    Hashtable p = new Hashtable();
    p.putAll(System.getProperties());
    p.putAll(props);
    return p;
  }

  /**
   * Get a copy of the current system properties.
   */
  public static java.util.Properties getSystemProperties() {
    return (java.util.Properties)System.getProperties().clone();
  }


  protected static void initProperties() {
    props = new HashMap();




    whichStorageImpl = "org.knopflerfish.framework.bundlestorage." +
      Framework.getProperty("org.knopflerfish.framework.bundlestorage", "file") +
      ".BundleStorageImpl";

    bIsMemoryStorage = whichStorageImpl.equals("org.knopflerfish.framework.bundlestorage.memory.BundleStorageImpl");
    if (bIsMemoryStorage ||
        !EXIT_ON_SHUTDOWN ||
        !USING_WRAPPER_SCRIPT) {
      SUPPORTS_EXTENSION_BUNDLES = false;
      // we can not support this in this mode.
    } else {
      SUPPORTS_EXTENSION_BUNDLES = true;
    }
    // The name of the operating system of the hosting computer.
    setProperty(Constants.FRAMEWORK_OS_NAME, System.getProperty("os.name"));


    // The name of the processor of the hosting computer.
    setProperty(Constants.FRAMEWORK_PROCESSOR, System.getProperty("os.arch"));

    String ver = System.getProperty("os.version");
    if (ver != null) {
      int dots = 0;
      int i = 0;
      for ( ; i < ver.length(); i++) {
        char c = ver.charAt(i);
        if (Character.isDigit(c)) {
          continue;
        } else if (c == '.') {
          if (++dots < 3) {
            continue;
          }
        }
        break;
      }
      osVersion = ver.substring(0, i);
    }
    setProperty(Constants.FRAMEWORK_OS_VERSION, osVersion);
    setProperty(Constants.FRAMEWORK_VERSION,   SPEC_VERSION);
    setProperty(Constants.FRAMEWORK_VENDOR,   "Knopflerfish");
    setProperty(Constants.FRAMEWORK_LANGUAGE,
                Locale.getDefault().getLanguage());

    // Various framework properties
    setProperty(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, TRUE);
    setProperty(Constants.SUPPORTS_FRAMEWORK_FRAGMENT, TRUE);
    setProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION,
                SUPPORTS_EXTENSION_BUNDLES ? TRUE : FALSE);
    setProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION,
                SUPPORTS_EXTENSION_BUNDLES ? TRUE : FALSE);

    Dictionary sysProps = getSystemProperties();

    setProperties(sysProps);
  }

  /**
   * Get the bundle context used by the system bundle.
   */
  public BundleContext getSystemBundleContext() {
    return systemBC;
  }
}
