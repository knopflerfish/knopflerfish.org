/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.util.cm;

import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.io.InputStream;
import java.net.URL;

import org.osgi.service.cm.ManagedService;
import org.osgi.framework.*;

import org.knopflerfish.service.log.LogRef;

/**
 * This is the nuclear power plant 
 * (<a href="http://www.ringhals.se/">Ringhals</a>) that basically all CM 
 * configurable services want to implement.
 *
 * <p>
 * <b>Note</b>: If you use this utility class, you must still make
 * sure your bundle have an <code>Import-package</code> manifest entry for
 * <code>org.osgi.service.cm</code>. 
 * </p>
 *
 * <p>
 * <b>Example 1</b>: A simple config.
 * <pre>
 * 
 * public class MyConfig extends Ringhals {
 * 
 *   public MyConfig(BundleContext bc, LogRef log) {
 *     super(bc, log, "com.acme.myconfig", 0);
 *   }
 * 
 *   int    CM_PROP_value1 = 0;
 *   int    CM_PROP_value2 = 2;
 *   String CM_PROP_value3 = "apa";
 *
 *   public void updated(String checkpoint, Dictionary props) {
 *     // ...handle checkpoint
 *   }
 * }
 * 
 * ...
 *
 * // Create and register.
 * MyConfig conf = new MyConfig(bc, log);
 * conf.register();
 *</pre>
 *
 * <b>Example 2</b>: A verifying config.
 * <pre>
 * 
 * public class MyConfig extends Ringhals {
 * 
 *   public MyConfig(BundleContext bc, LogRef log) {
 *     super(bc, log, "com.acme.myconfig", 0);
 *   }
 * 
 *   int    CM_PROP_value1 = 0;
 *   int    CM_PROP_value2 = 2;
 *   String CM_PROP_value3 = "apa";
 *
 *   // This method will be called whenever "value1" is changed.
 *   // If it throws any exception, update will fail.
 *   public void cmVerify_value1(Integer i) {
 *     if(i.intValue() &lt; 0 || i.intValue() &gt; 100) 
 *       throw new RuntimeException("value must be in the range [0-100]"); 
 *   }
 *
 *   public void updated(String checkpoint, Dictionary props) {
 *     // ...handle checkpoint
 *   }
 * }
 * </pre>
 *
 * <b>Example 3</b>: A config with a checkpoint.
 * <pre>
 * 
 * public class MyConfig extends Ringhals {
 * 
 *   public MyConfig(BundleContext bc, LogRef log) {
 *     super(bc, log, "com.acme.myconfig", 0);
 *   }
 * 
 *   int    CM_PROP_value1 = 0;
 *   int    CM_PROP_value2 = 2;
 *   String CM_PROP_value3 = "apa";
 *
 *   // Define a checkpoint "STAGE1" which will be 
 *   // generate notifocation whenever "value1" and 
 *   // "value2" are updated (after verification).
 *   public String[] checkpoint_STAGE1   = { "value1", "value2" };
 *
 *   public void updated(String checkpoint, Dictionary props) {
 *     if("STAGE1".equals(checkpoint) {
 *       System.out.println("value1=" + CM_PROP_VALUE1);
 *       System.out.println("value2=" + CM_PROP_VALUE2);
 *     }
 *   }
 * }
 * </pre>
 *
 * <b>Example 4</b>: A config depending on service properties
 * <pre>
 * 
 * public class MyConfig extends Ringhals {
 * 
 *   public MyConfig(BundleContext bc, LogRef log) {
 *     super(bc, log, "com.acme.myconfig", 0);
 *   }
 * 
 *   public int      CM_PROP_httpPort = 0;
 *   public boolean  CM_PROP_httpDNS  = false;
 * 
 *  // This creates a listener for the HTTP service.
 *  // The "messenger" part is just an id string and should
 *  // macth the mapping name below.
 *  public String     SRCLASS_messenger  = HttpService.class.getName();
 *
 *  // Map the service reference property "port" to the member "httpPort"
 *  // and "dns.lookup" to the member "httpDNS"
 *  public String[][] SRPARAMS_messenger = new String[][] {
 *    {"port",       "httpPort"},
 *    {"dns.lookup", "httpDNS"},
 *   };
 *
 *  // ..and make a checkpoint for the http stuff. Not really necessary, 
 *  // but nice
 *  public String[] checkpoint_HTTP      = { "httpPort", "httpDNS" };
 *
 *   public void updated(String checkpoint, Dictionary props) {
 *    if("HTTP".equals(checkpoint)) {
 *      System.out.println(" httpPort=" + CM_PROP_httpPort);
 *      System.out.println(" httpDNS="  + CM_PROP_httpDNS);
 *    }
 *   }
 * }
 * </pre>
 *
 * @see ASEA
 */
public class Ringhals implements ManagedService {

  /** 
   * Prefix string for CM controlled variables. 
   *
   * Default value is "CM_PROP_"
   */
  protected              String str_CM_PROP         = "CM_PROP_";

  /** 
   * Prefix string for varible defaults. 
   *
   * Value is "DEFAULT_"
   */
  protected static final String str_CM_PROP_DEFAULT = "DEFAULT_";

  /** 
   * Prefix stringt for variable CM name. 
   *
   * Default is same as CM_PROP variable. Value is "CM_PROP_NAME_".
   * Type is String. 
   */
  protected static final String str_CM_PROP_NAME    = "CM_PROP_NAME_";

  /**
   * Prefix string for CM verify methods. 
   *
   * Value is "cmVerify_"
   */
  protected static final String str_CM_PROPVERIFY   = "cmVerify_";

  /**
   * Prefix string for checkpoint property lists. 
   *
   * Value is "checkpoint_". Type is String[]
   */
  protected static final String str_CM_CHECKPOINT   = "checkpoint_";

  /**
   * Prefix string for service class name.
   *
   * Value is "SRCLASS_"
   */
  protected static final String str_SRCLASS    = "SRCLASS_";

  /**
   * Prefix string for service property mapping. 
   * 
   * Value is "SRPARAMS_". Type is String[][2].
   */
  protected static final String str_SRPARAMS   = "SRPARAMS_";

  /**
   * Value is "NOOP"
   */
  public static final String CHECKPOINT_NOOP = "NOOP";

  /**
   * If no checkpoints are defined, you will still get
   * a notification - CHECKPOINT_SOME.
   *
   * Value is "SOME"
   */
  public static final String CHECKPOINT_SOME = "SOME";

  /** BundleContext used when instanciating class */
  protected BundleContext       bc  = null;

  /** LogRef used when instanciating class. May be <code>null</code> */
  protected LogRef              log = null;

  /** Service PID used when instanciating class */
  protected String              pid = null;

  private   Class               clazz   = null;
  private   long                timeout = 0;

  private ServiceRegistration reg = null;

  /** String (property name) - Object (default value) */
  private Hashtable           defaults    = null;

  /** String (checkpoint name) - String[] (required property names) */
  private Hashtable           checkpoints = null;

  /** String (class name) - ServiceListener */
  private   Hashtable           listeners   = null;


  /** count number of calls to update(Dict) */
  private  int                  updateCount = 0;   

  /**
   * Create the power plant.
   *
   * @param bc BundleContext used for registering config.
   * @param log Optional LogRef. Can be null.
   * @param pid service.pid to register config as.
   * @param timeout If &gt; 0, set up timer which will call
   *                <code>updated</code> with default values after 
   *                timout milliseconds.
   */
  public Ringhals(BundleContext bc, 
		  LogRef        log, 
		  String        pid,
		  long          timeout) {
    this.bc      = bc;
    this.log     = log;
    this.pid     = pid;
    this.timeout = timeout;
  }

  /**
   * Initialize all necessary internal stuff. Must be done
   * before <code>update</code> can be called.
   *
   * <p>
   * Note: The <code>register</code> method automatically calls 
   * <code>open</code>
   * </p>
   *
   * @throws Exception if initialization fails. This can happen if any
   *                   verification methods fails, or if the any
   *                   of the magic reflection analyzing fails.
   */
  public void open() throws Exception {
    init();
    checkManifest();
    verifyProperties(defaults);
  }

  /**
   * Open and register this config as <code>org.osgi.service.cm.ManagedService</code> 
   * so CM can find it. 
   *
   * <p>
   * Also starts timeout thread
   * if created with timeout &gt; 0 and registers any necessary listeners
   * if service property mapping is used.
   * </p>
   */
  public void register() {
    try {
      open();
      if(reg == null) {
	Hashtable props = new Hashtable();
	props.put("service.pid", pid);
	
	reg = bc.registerService(ManagedService.class.getName(), this, props);
	
	// Start thread which will call updated() with default values
	// if no other config has become available
	if(timeout > 0) startDefaultsThread();
      }
    } catch (Exception e) {
      if(log != null) log.error("Register " + pid + " failed", e);
    }
  }

  /**
   * Unregister and close this config.
   *
   * <p>
   * Unregister the <code>ManagedService</code> and removes all 
   * service listeners if service property mapping is used.
   * </p>
   */
  public void unregister() {
    if(reg != null) {
      reg.unregister();
      reg = null;
    }
    close();
  }

  /**
   * Override as needed - will be called when a change has occured. 
   * The default implementations just logs a few debug messages.
   *
   * <p>
   * If checkpoints are defined, the <code>checkpoint</code>
   * string will be the checkpoint name. If no checkpoints
   * are defined an unsorted <code>CHECKPOINT_SOME</code>
   * will still be sent.
   * </p>
   */
  public void updated(String checkpoint, Dictionary props) {
    if(log != null && log.doDebug()) {
      log.debug("updated checkpoint=" + checkpoint);
    }
  }

  /**
   * Implements ManagedService.
   *
   * <ul>
   *  <li>Verifies properties using any defined cmCheck methods
   *  <li>Set properties to instance variables and decide which
   *      ones were changed by comparing to old values.
   *  <li>Compare changed properties to checkpoint list and call
   *      <code>updated(String checkpoint, Dictionary changed)</code>
   *      as necessary.
   * </ul>
   */
  public void updated(Dictionary props) {
    if(defaults == null) {
      throw new RuntimeException("init() not called");
    }

    updateCount++;

    if(props == null) {
      updated((String)null, null);
      return;
    }

    try {
      verifyProperties(props);
      Dictionary changed = setProperties(props);
      handleCheckpoints(changed);

    } catch (Exception e) {
      if(log != null) log.error("update failed", e);
      throw new RuntimeException("update failed " + e);
    }
  }

  /**
   * Call <code>updated(checkpoint)</code> for all checkpoint names
   * which matches the the changed dictionary.
   */
  void handleCheckpoints(Dictionary changed) {
    if(changed.size() == 0) {
      updated(CHECKPOINT_NOOP, changed);
      return;
    }

    Dictionary remains = shallowCopy(changed);

    // Check all checkpoints if the are subsets of the changed
    // dictionary. If so, call updated(checkpoint name, changed)
    for(Enumeration e = checkpoints.keys(); e.hasMoreElements(); ) {
      String  cpName = (String)e.nextElement();
      String[] names = (String[])checkpoints.get(cpName);
      boolean bOK = true;

      // names[] contains *property names*, not member names
      for(int i = 0; i < names.length; i++) {
	Object val = changed.get(names[i]);
	if(val == null) {
	  bOK = false;
	  break;
	}
      }
      if(bOK) {
	for(int i = 0; i < names.length; i++) {
	  remains.remove(names[i]);
	}
	updated(cpName, changed);
      }
    }

    if(remains.size() > 0) {
      updated(CHECKPOINT_SOME, changed);
    }
  }

  /**
   * Verify values in props. If any value is null, use default value instead.
   */
  private void verifyProperties(Dictionary props) throws Exception {
    Method[] methods = clazz.getMethods();

    for(int i = 0; i < methods.length; i++) {
      String mName = methods[i].getName();
      if(methods[i].getParameterTypes().length == 1) {
	if(mName.startsWith(str_CM_PROPVERIFY)) {
	  String name  = mName.substring(str_CM_PROPVERIFY.length());
	  String pName = getCMPropName(clazz, str_CM_PROP+name);
	  Object val   = props.get(pName);

	  // Make sure we have something valid to verify.
	  // Also has the effekt that all default values are verified
	  if(val == null) val = defaults.get(pName);
	  try {
	    methods[i].invoke(this, new Object[] { val });
	  } catch (Exception e) {
	    if(log != null) log.warn("verify failed", e);
	    throw e;
	  }
	}
      }
    }
  }

  
  
  /**
   * Initialize this.defaults with default values for 
   * property keys, using config member values.
   */
  private void init() throws Exception {
    clazz = getClass();
    if(log != null) log.debug("init from class " + clazz.getName() + ", pid=" + pid);
    Field[] fields = clazz.getFields();

    initDefaults(fields);
    initCheckpoints(fields);
    initServiceParams(fields);
  }

  /**
   * Close and cleanup as necessary.
   */
  public void close() {
    closeServiceParams();
    closeCheckpoints();
    closeDefaults();

    clazz = null;
    bc    = null;
    log   = null;
  }

  private void initCheckpoints(Field[] fields) throws Exception {
    checkpoints = new Hashtable();

    // ...and, initialize all checkpoints
    for(int i = 0; i < fields.length; i++) {
      String name = fields[i].getName();
      if(niceProp(fields[i], str_CM_CHECKPOINT)) {
	String   cpName = name.substring(str_CM_CHECKPOINT.length());
	String[] val    = (String[])fields[i].get(this);
	checkpoints.put(cpName, val);
      }
    }
    checkpoints.put("SYSTEM", new String[] { 
      "service.pid", 
      "service.bundleLocation",
    });
  }

  private void closeCheckpoints() {
    checkpoints = null;
  }

  private void closeServiceParams() {
    for(Enumeration e = listeners.keys(); e.hasMoreElements(); ) {
      String srClass = (String)e.nextElement();
      ServiceListener sl = (ServiceListener)listeners.get(srClass);
      bc.removeServiceListener(sl);
    }
    listeners.clear();
  }

  private void initServiceParams(Field[] fields) throws Exception {
    listeners = new Hashtable();

    for(int i = 0; i < fields.length; i++) {
      String name = fields[i].getName();
      if(niceProp(fields[i], str_SRCLASS)) {
	final String     srName  = name.substring(str_SRCLASS.length());
	final String     srClass = (String)fields[i].get(this);

	try {
	  Field paramF = clazz.getField(str_SRPARAMS + srName);
	  
	  final String[][] map     = (String[][])paramF.get(this);
	  
	  final String filter = "(objectclass=" + srClass + ")";
	  
	  ServiceListener sl = new ServiceListener() {
	      public void serviceChanged(ServiceEvent ev) {
		ServiceReference sr = ev.getServiceReference();
		switch(ev.getType()) {
		case ServiceEvent.REGISTERED:
		case ServiceEvent.MODIFIED:
		  putSRMap(sr);
		  break;
		case ServiceEvent.UNREGISTERING:
		default:
		  break;
		}
	      }

	      void putSRMap(ServiceReference sr) {
		Dictionary props = new Hashtable();
		for(int j = 0; j < map.length; j++) {
		  String srPropName  = map[j][0];
		  String propName    = map[j][1];
		  
		  Object val = sr.getProperty(srPropName);
		  if(val != null) {
		    props.put(propName, val);
		  } else {
		    if(log != null) {
		      log.warn("No SR property " + srPropName + " defined");
		    }
		  }
		}
		// Wow. This works just like any other update.
		updated(props);
	      }
	    };
	  try {
	    ServiceReference[] srl = bc.getServiceReferences(null, filter);
	    for(int j = 0; srl != null && j < srl.length; j++) {
	      sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[j]));
	    }
	    bc.addServiceListener(sl, filter);
	    listeners.put(srClass, sl);
	  } catch (Exception e) {
	    if(log != null) {
	      log.error("bluerg", e);
	    }
	  }
	} catch (Exception e) {
	  if(log != null) {
	    log.error("No param map for " + fields[i].getName());
	  }
	}
      }
    }
  }

  
  private void closeDefaults() {
    defaults = null;
  }

  private void initDefaults(Field[] fields) throws Exception {
    defaults    = new Hashtable();

    // First, set all defaults from current values in config
    for(int i = 0; i < fields.length; i++) {
      String name = fields[i].getName();
      if(niceProp(fields[i], str_CM_PROP)) {
	Object val  = fields[i].get(this);
	String pName = getCMPropName(clazz, name);

	if(val != null) {
	  defaults.put(pName, val);
	
	} else {
	  if(log != null) log.warn("ouch. std default " + clazz.getName() + "." + pName + "=" + val);
	}
      }
    }

    // Then, override default values from CM_PROP_DEFAULT_xxx values
    for(int i = 0; i < fields.length; i++) {
      String name = fields[i].getName();
      if(niceProp(fields[i], str_CM_PROP_DEFAULT)) {
	Object val  = fields[i].get(this);
	
	String pName = name.substring(str_CM_PROP_DEFAULT.length());

	if(val != null) {
	  defaults.put(pName, val);
	  
	} else {
	  if(log != null) log.warn("ouch. default " + pName + "=" + val);
	}
      }
    }
  }

  /**
   * Set member variables from a dictionary. 
   * 
   * <p>
   * Only set those members
   * which have been modified. Return a table of which property names
   * have been changed.
   * </p>
   *
   * @return String (property key string) -&gt; Object (value)
   */
  private Dictionary setProperties(Dictionary props) 
    throws Exception {
    Dictionary changed = new Hashtable();
    Dictionary handled = null; // save unhandled properties in this one

    // Save some memory if logging is not used
    if(log != null && log.doDebug()) {
      handled = new Hashtable();
    }

    if(props == null) {
      props = new Hashtable();
    }

    // Fill in props with default values
    for(Enumeration e = defaults.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      if(null == props.get(key)) {
	props.put(key, defaults.get(key));
      }
    }

    Field[] fields = clazz.getFields();
  
    // Set the values and store which ones were changed in dictionary.
    for(int i = 0; i < fields.length; i++) {
      String name      = fields[i].getName();
      if(niceProp(fields[i], str_CM_PROP)) {
	String pName   = getCMPropName(clazz, name);

	if(handled != null) handled.put(pName, name);

	Object newVal  = props.get(pName);
	Object oldVal  = fields[i].get(this);
	if((oldVal == null && newVal != null) ||
	   (oldVal != null && newVal == null) ||
	   (oldVal != null && newVal != null && !oldVal.equals(newVal))) {

	  changed.put(pName, newVal);

	  fields[i].set(this, newVal);
	  if(log != null && log.doDebug()) {
	    log.debug("set " +name+"="+newVal+" from property "+pName);
	  }
	}
      }
    }
    // Warn if there are unhandled properties
    if(log != null && log.doDebug()) {
      if(handled != null && handled.size() < props.size()) {
	for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
	  Object key = e.nextElement();
	  if(null == handled.get(key)) {
	    log.debug("Unhandled property " + key + "=" + props.get(key));
	    changed.put(key, props.get(key));
	  }
	}
      }
    }
    return changed;
  }

  /**
   * Start thread which will call updated() with default values
   * if no other config has become available.
   */
  private void startDefaultsThread() {
    Thread delayThread = new Thread(new Runnable() {
	public void run() {
	  try {
	    Thread.sleep(timeout);

	    // Make sure we don't call this unecessesarily
	    if(reg != null && updateCount == 0) {
	      updated(defaults);
	    }
	  } catch (Exception e) {
	    if(log != null) log.warn("startDefaultsThread failed");
	  }
	}
      }, "delay thread for " + pid);
    delayThread.start();
  }


  /**
   * Return true if a field starts with a given string and is public.
   */
  private boolean niceProp(Field f, String startString) {
    if(Modifier.isPublic(f.getModifiers())) {
      String name = f.getName();
      if(name.startsWith(startString)) {
	return true;
      }
    }
    return false;
  }

  /**
   * Get the property name of a given member name.
   */
  private String getCMPropName(Class clazz, String name) {
    String pName = name.substring(str_CM_PROP.length());
    try {
      Field nameF = clazz.getField(str_CM_PROP_NAME + pName);
      
      if(nameF != null && nameF.getType().equals(String.class)) {
	pName = (String)nameF.get(this);
      }
    } catch (Exception e) {
    }
    return pName;
  }

  static private Dictionary shallowCopy(Dictionary d) {
    Hashtable h = new Hashtable();
    for(Enumeration e = d.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      Object val = d.get(key);
      h.put(key, val);
    }
    return h;
  }

  /**
   * Check if manifest has config.xml reference. If not, create a dummy one
   * and log it at warning level.
   */
  private void checkManifest() {
    try {
      ClassLoader cl = getClass().getClassLoader();
      
      URL mfURL = cl.getResource("/META-INF/MANIFEST.MF");
      
      InputStream is = mfURL.openStream();
      
      Manifest mf = new Manifest(is);
      Attributes attrs = mf.getMainAttributes();
      is.close();

      String cfFile = (String)attrs.getValue("Bundle-Config");
      if(cfFile == null) {
	if(log != null) {
	  log.warn("No config xml file referenced in manifest");

	  String s = makeConfigXML(pid, defaults);
	  log.warn("Try adding something like this XML to your bundle (and add a Bundle-Config entry to you manifest): \n" + s);
	}
      } else {
	if(log != null) {
	  if(log.doDebug()) log.debug("Have CF file " + cfFile);
	}
      }
    } catch (Exception e) {
      if(log != null) {
	log.warn("Failed to check manifest", e);
      }
    }
  }
  
  static String makeConfigXML(String pid, Dictionary props) {
    StringBuffer sb = new StringBuffer();
    sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
    sb.append("<!DOCTYPE cfg PUBLIC \"-//knopflerfish//DTD cfg 1.0//EN\" \"http://www.knopflerfish.org/dtd/config.dtd\">\n");

    sb.append("<cfg version=\"0.1\">\n");
    
    sb.append(" <managedService pid=\"" + pid + "\"\n" + 
	      "                 descr=\"" + ""  + "\">\n");

    for(Enumeration e = props.keys(); e.hasMoreElements(); ) {
      String name = (String)e.nextElement();
      Object val  = props.get(name);
      String tName = val.getClass().getName();
      if(tName.startsWith("java.lang.")) {

	sb.append(" <property name=\"" + name + "\">\n");
	sb.append("  <description></description>\n");
	
	tName = tName.substring("java.lang.".length());
	sb.append("  <value type=\"" + tName + "\"");
	sb.append(" default=\"" + val + "\"/>\n");
	sb.append(" </property>\n");
      }
    }
    sb.append(" </managedService>\n");
    sb.append("</cfg>\n");

    return sb.toString();
  }


  /*
  static String toString(Dictionary d) {
    StringBuffer sb = new StringBuffer("{");
    for(Enumeration e = d.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      Object val = d.get(key);
      sb.append((key.toString() + "=" + val));
      if(e.hasMoreElements()) {
	sb.append("; ");
      }
    }
    sb.append("}");
    return sb.toString();
  }
  */
}


