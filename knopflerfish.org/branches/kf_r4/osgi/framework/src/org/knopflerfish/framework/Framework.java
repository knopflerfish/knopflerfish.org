/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Dictionary;

import org.osgi.framework.*;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class contains references to all common data structures
 * inside the framework.
 *
 * @author Jan Stein, Erik Wistrand, Philippe Laporte, Mats-Ola Persson
 */
public class Framework {

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
  protected Bundles bundles;

  /**
   * All listeners in this framework.
   */
  Listeners listeners = new Listeners();

  /**
   * All exported and imported packages in this framework.
   */
  Packages packages;

  /**
   * All registered services in this framework.
   */
  Services services = new Services();

  /**
   * PermissionAdmin service
   */
  PermissionAdminImpl permissions /*= null*/;


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
//#ifdef USESTARTLEVEL
  StartLevelImpl                 startLevelService;
//#endif  
 

  /**
   * Factory for handling service-based URLs
   */
  ServiceURLStreamHandlerFactory urlStreamHandlerFactory;

  /**
   * Factory for handling service-based URL contents
   */
  ServiceContentHandlerFactory   contentHandlerFactory;

  /**
   * Magic handler for "bundle:" URLs
   */
  URLStreamHandler bundleURLStreamhandler;

  /**
   * Property constants for the framework.
   */
  final static String TRUE   = "true";
  final static String FALSE  = "false";

  final static String osArch = System.getProperty("os.arch");
  final static String osName = System.getProperty("os.name");
  static String osVersion;

  // If set to true, then during the UNREGISTERING event the Listener
  // can use the ServiceReference to receive an instance of the service.
  public final static boolean UNREGISTERSERVICE_VALID_DURING_UNREGISTERING =
	  TRUE.equals(System.getProperty("org.knopflerfish.servicereference.valid.during.unregistering",
				     FALSE));

  // Some tests conflict with the R3 spec. If testcompliant=true
  // prefer the tests, not the spec
  public final static boolean R3_TESTCOMPLIANT =
    TRUE.equals(System.getProperty("org.knopflerfish.osgi.r3.testcompliant",
				     FALSE));

  // If set to true, set the bundle startup thread's context class
  // loader to the bundle class loader. This is useful for tests
  // but shouldn't really be used in production.
  final static boolean SETCONTEXTCLASSLOADER =
    TRUE.equals(System.getProperty("org.knopflerfish.osgi.setcontextclassloader", FALSE));

  final static boolean REGISTERBUNDLEURLHANDLER =
    TRUE.equals(System.getProperty("org.knopflerfish.osgi.registerbundleurlhandler", FALSE));

  final static boolean REGISTERSERVICEURLHANDLER =
    TRUE.equals(System.getProperty("org.knopflerfish.osgi.registerserviceurlhandler", TRUE));



  // Accepted execution environments. 
  static String defaultEE = "CDC-1.0/Foundation-1.0,OSGi/Minimum-1.0";

  static boolean bIsMemoryStorage /*= false*/;
  
  private static final String USESTARTLEVEL_PROP = "org.knopflerfish.startlevel.use";


  /**
   * Whether the framework supports extension bundles or not.
   * This will be false if bIsMemoryStorage is false.
   */
  static boolean SUPPORTS_EXTENSION_BUNDLES;
  final static boolean EXIT_ON_SHUTDOWN = "true".equals(System.getProperty(Main.EXITONSHUTDOWN_PROP, "true"));

  final static int EXIT_CODE_NORMAL  = 0;
  final static int EXIT_CODE_RESTART = 200;

  final static boolean USING_WRAPPER_SCRIPT = "true".equals(System.getProperty(Main.USINGWRAPPERSCRIPT_PROP, "false"));

  /**
   * Contruct a framework.
   *
   */
  public Framework(Object m) throws Exception {

    // guard this for profiles without System.setProperty 
    try {
      System.setProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, defaultEE);
    } catch (Throwable e) {
      if(Debug.packages) {
	Debug.println("Failed to set execution environment: " + e);
      }
    }

    String whichStorageImpl = "org.knopflerfish.framework.bundlestorage." + 
      System.getProperty("org.knopflerfish.framework.bundlestorage", "file") +
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
    
    // We just happens to know that the memory storage impl isn't R3
    // compatible. And it never will be since it isn't persistent
    // by design.
    if(R3_TESTCOMPLIANT && bIsMemoryStorage) {
      throw new RuntimeException("Memory bundle storage is not compatible " + 
				                 "with R3 compliance");
    }

    String ver = System.getProperty("os.version");
    if (ver != null) {
      int dots = 0;
      boolean skipDelim = false;
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
        

    Class storageImpl = Class.forName(whichStorageImpl);
    storage           = (BundleStorage)storageImpl.newInstance();

    dataStorage       = Util.getFileStorage("data");
    packages          = new Packages(this);
    

    systemBundle      = new SystemBundle(this);

    systemBC          = new BundleContextImpl(systemBundle);
    bundles           = new Bundles(this);

    systemBundle.setBundleContext(systemBC);

    if (System.getSecurityManager() != null) { 	
      permissions       = new PermissionAdminImpl(this);
      String [] classes = new String [] { PermissionAdmin.class.getName() };
      services.register(systemBundle,
			classes,
			permissions,
			null);

      Policy.setPolicy(new FrameworkPolicy(permissions));
    }
    /* for testing
    permissions.setPermissions("file:///C:\\devel\\ubicore2\\out\\dmt_gst_plugin.jar",
    		                   new PermissionInfo[]{new PermissionInfo("org.knopflerfish.framework.AdminPermission", null, AdminPermission.STARTLEVEL)})
*/
    String[] classes = new String [] { PackageAdmin.class.getName() };
    services.register(systemBundle,
		      classes,
		      new PackageAdminImpl(this),
		      null);
    
//#ifdef USESTARTLEVEL
    registerStartLevel();
//#endif
    urlStreamHandlerFactory = new ServiceURLStreamHandlerFactory(this);
    contentHandlerFactory   = new ServiceContentHandlerFactory(this);
    bundleURLStreamhandler  = new BundleURLStreamHandler(bundles);

    // Only register bundle: URLs publicly if explicitly told so
    // Note: registering bundle: URLs exports way to much. 
    if(REGISTERBUNDLEURLHANDLER) {
      urlStreamHandlerFactory
        .setURLStreamHandler(BundleURLStreamHandler.PROTOCOL,
                             bundleURLStreamhandler);
    }

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

//#ifdef USESTARTLEVEL

  private void registerStartLevel(){
	  String useStartLevel = System.getProperty(USESTARTLEVEL_PROP, TRUE);
      
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

//#endif
  
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
      systemBundle.systemActive();

      // start level open is delayed to this point to 
      // correctly work at restart
//#ifdef USESTARTLEVEL      
      if(startLevelService != null) {
        startLevelService.open();
      }
//#endif      

      listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, systemBundle, null));
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
//    #ifdef USESTARTLEVEL      
      if(startLevelService != null) {
    	  startLevelService.close();
      }
//#endif      
      systemBundle.systemShuttingdown();
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
    BundleImpl b = bundles.getBundle(id);
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
    BundleImpl b = bundles.getBundle(id);
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
    BundleImpl b = bundles.getBundle(id);
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
    BundleImpl b = bundles.getBundle(id);
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
    BundleImpl b = bundles.getBundle(id);
    if (b != null) {
      return b.location;
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
    BundleImpl b = bundles.getBundle(location);
    if (b != null) {
      return b.id;
    } else {
      return -1;
    }
  }

  /**
   * Get private bundle data storage file handle.
   */
  FileTree getDataStorage() {
    return dataStorage;
  }

  /**
   * Check if an execution environment string is accepted
   */
  boolean isValidEE(String ee) {

    if(ee == null || "".equals(ee)) {
      return true;
    }

    String fwEE = System.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

    if(fwEE == null) {
      eeCacheSet.clear();
    } else {
      if(!fwEE.equals(eeCache)) {
	eeCacheSet.clear();

	String[] l = Util.splitwords(fwEE, ",", '\"');
	for(int i = 0 ; i < l.length; i++) {
	  eeCacheSet.add(l[i]);
	}
      }
    }
    eeCache = fwEE;

    String[] eel   = Util.splitwords(ee, ",", '\"');
    
    for(int i = 0 ; i < eel.length; i++) {
      if(eeCacheSet.contains(eel[i])) {
	return true;
      }
    }

    return false;
  }


  // Cached value of 
  // System.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)
  // Used and updated by isValidEE()
  Set    eeCacheSet = new HashSet();
  String eeCache = null;

  //
  // Static package methods
  //

  /**
   * Retrieve the value of the named framework property.
   *
   */
  public static String getProperty(String key) {
    if (Constants.FRAMEWORK_VERSION.equals(key)) {
      // The version of the framework. 
      return SPEC_VERSION;
    } else if (Constants.FRAMEWORK_VENDOR.equals(key)) {
      // The vendor of this framework implementation. 
      return "Knopflerfish";
    } else if (Constants.FRAMEWORK_LANGUAGE.equals(key)) {
      // The language being used. See ISO 639 for possible values. 
      return Locale.getDefault().getLanguage();
    } else if (Constants.FRAMEWORK_OS_NAME.equals(key)) {
      // The name of the operating system of the hosting computer. 
      return osName;
    } else if (Constants.FRAMEWORK_OS_VERSION.equals(key)) {
      // The version number of the operating system of the hosting computer. 
      return osVersion;
    } else if (Constants.FRAMEWORK_PROCESSOR.equals(key)) {
      // The name of the processor of the hosting computer. 
      return osArch;
    } else if (Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE.equals(key)) {
      return TRUE;
    } else if (Constants.SUPPORTS_FRAMEWORK_FRAGMENT.equals(key)) {
      return TRUE;
    } else if (Constants.SUPPORTS_FRAMEWORK_EXTENSION.equals(key)) {

      /* System.out.println("TODO!");

      return FALSE; */
      return SUPPORTS_EXTENSION_BUNDLES ? TRUE : FALSE;

    } else if (Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION.equals(key)) {
      /* System.out.println("TODO!");
         return FALSE; */
      return SUPPORTS_EXTENSION_BUNDLES ? TRUE : FALSE;

    } else {
      return System.getProperty(key);
    }
  }
  
  public static Dictionary getProperties(){
    Dictionary props = System.getProperties();
    props.put(Constants.FRAMEWORK_VERSION, SPEC_VERSION);
    props.put(Constants.FRAMEWORK_VENDOR, "Knopflerfish");
    props.put(Constants.FRAMEWORK_LANGUAGE, Locale.getDefault().getLanguage());
    props.put(Constants.FRAMEWORK_OS_NAME, osName);
    props.put(Constants.FRAMEWORK_OS_VERSION, osVersion);
    props.put(Constants.FRAMEWORK_PROCESSOR, osArch);
    props.put(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, TRUE);
    props.put(Constants.SUPPORTS_FRAMEWORK_FRAGMENT, TRUE);
    /* System.out.println("TODO! Change to TRUE");
    props.put(Constants.SUPPORTS_FRAMEWORK_EXTENSION, FALSE);
    props.put(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION, FALSE); */
    props.put(Constants.SUPPORTS_FRAMEWORK_EXTENSION, TRUE);
    props.put(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION, TRUE);
    return props;
  }

  /**
   * Get the bundle context used by the system bundle.
   */
  public BundleContext getSystemBundleContext() {
    return (BundleContext)
      AccessController.doPrivileged(new  PrivilegedAction() {
	  public Object run() {
	    return systemBC;
	  }});
  }
}
