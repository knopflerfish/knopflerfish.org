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

package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;
import org.osgi.service.cm.*;
import org.osgi.util.tracker.*;
import org.knopflerfish.service.log.LogRef;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Class which monitors installed bundles for metatype and CM default data.
 *
 * <p>
 * When instanciated, SystemMetatypeProvider will listen for installed
 * bundles and try to extract metatype and Cm defaults XML from the
 * bundle jar files. This data will then be available using the 
 * <tt>getServicePIDs</tt>, <tt>getFactoryPIDs</tt> and 
 * <tt>getObjectClassDefinition</tt> methods.
 * </p>
 */
public class SystemMetatypeProvider 
  implements MetaTypeProvider, PIDProvider {

  /**
   * Default URL to metatype XML.
   * 
   * <p>
   * Value is "!/metatype.xml"
   * </p>
   */
  public static final String METATYPE_RESOURCE    = "/metatype.xml";

  /**
   * Default URL to default CM values
   * 
   * <p>
   * Value is "!/cmdefaults.xml"
   * </p>
   */
  public static final String CMDEFAULTS_RESOURCE  = "/cmdefaults.xml";

  /**
   * Manifest attribute name specifying metatype XML URL.
   * 
   * <p>
   * Value is "Bundle-MetatypeURL"
   * </p>
   */
  public static final String ATTRIB_METATYPEURL   = "Bundle-MetatypeURL";

  /**
   * Manifest attribute name specifying CM defaults XML URL.
   * 
   * <p>
   * Value is "Bundle-CMDefaultsURL"
   * </p>
   */
  public static final String ATTRIB_CMDEFAULTSURL = "Bundle-CMDefaultsURL";

  BundleContext  bc;
  LogRef         log;

  // Bundle -> MTP
  Map            providers = new HashMap();

  ServiceTracker cmTracker;

  // If set to true, track CM configuration instances
  // as MTPs on the system bundle.
  boolean        bTrackCM = true;

  // Special MTP which tracks CM configuration instances
  MTP            cmMTP;


  // String (pid) -> OCD
  Map cmOCDMap = new HashMap();

  /**
   * Create a SystemMetatypeProvider, using the specified bundle context
   * for listeners.
   */
  public SystemMetatypeProvider(BundleContext bc) {
    this.bc = bc;
    log = new LogRef(bc);
  }

  BundleListener bl = null;

  /**
   * Start listening for bundles.
   */
  public void open() {
    if(bl != null) {
      return;
    }

    bl = new BundleListener() {
	public void bundleChanged(BundleEvent ev) {
	  switch(ev.getType()) {
	  case BundleEvent.INSTALLED:

	    if(ev.getBundle().getBundleId() == 0) {
	      // We can't read properties from the system bundle
	    } else {
	      try {
		MTP mtp = loadMTP(ev.getBundle());
	      } catch (Exception e) {
		log.error("Failed to handle bundle " + 
			  ev.getBundle().getBundleId(), e);
	      }
	    }
	    break;
	  }
	}
      };

    Bundle[] bs = bc.getBundles(); 
    for(int i = 0; i < bs.length; i++) {
      bl.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bs[i]));
    }
    bc.addBundleListener(bl);

    if(bTrackCM) {
      cmTracker = new ServiceTracker(bc, 
				     ConfigurationAdmin.class.getName(), 
				     null);
      cmTracker.open();

      cmMTP = new MTP("[CM]") {
	  public String[] getPids() {
	    return (String[])getCMServicePIDs().toArray();
	  }

	  public String[] getFactoryPids() {
	    return (String[])getCMFactoryPIDs().toArray();
	  }
	  
	  public String[] getLocales() {
	    return null;
	  }
	  
	  public ObjectClassDefinition getObjectClassDefinition(String pid, 
								String locale) {
	    System.out.println("sysMTP.getOCD " + pid);

	    OCD ocd = (OCD)cmOCDMap.get(pid);
	    if(ocd != null) {
	      System.out.println(" cached");
	      return ocd;
	    }
	    
	    ConfigurationAdmin ca = getCA();
	    if(ca != null) {
	      try {
		Configuration[] configs = 
		  ca.listConfigurations(null);
		Configuration conf = null;
		for(int i = 0; configs != null && i < configs.length; i++) {
		  if(pid.equals(configs[i].getPid()) ||
		     pid.equals(configs[i].getFactoryPid())) {
		    conf = configs[i];
		  }
		}
		if(conf != null) {
		  Dictionary props = conf.getProperties();
		  System.out.println(" props=" + props);
		  ocd = new OCD(pid, pid, pid + " from CM", props);
		  cmOCDMap.put(pid, ocd);
		  return ocd;
		} else {
		  throw new RuntimeException("No config for pid " + pid);
		}
	      } catch (Exception e) {
		log.error("Failed to get service pid " + pid, e);
		return null;
	      }
	    } else {
	      log.warn("Failed to get CA when getting pid " + pid);
	      return null;
	    }
	  }
	};
    }
    
  }


  /**
   * Stop listening for bundles.
   */
  public void close() {
    if(cmTracker != null) {
      cmTracker.close();
      cmTracker = null;
    }
    if(bl != null) {
      bc.removeBundleListener(bl);
    }
    bl = null;
  }

  /**
   * Explictly load a metatype provider from a bundle.
   *
   * @throws Exception if loading fails
   */
  public MTP loadMTP(Bundle b) throws Exception {

    String defStr = (String)b.getHeaders().get(ATTRIB_METATYPEURL);

    if(defStr == null || "".equals(defStr)) {
      defStr = METATYPE_RESOURCE;
    }

    String valStr = (String)b.getHeaders().get(ATTRIB_CMDEFAULTSURL);

    if(valStr == null || "".equals(valStr)) {
      valStr = CMDEFAULTS_RESOURCE;
    }

    URL url;

    MTP mtp = null;
    
    if(defStr.startsWith("!")) {
      url = b.getResource(defStr.substring(1));
    } else if(defStr.startsWith("/")) {
      url = b.getResource(defStr);
    } else {
      url = new URL(defStr);
    }
    if(url == null) {
      if(log.doDebug()) {
	log.debug("No metatype xml in bundle " + b.getBundleId());
      }
    } else {
      
      try {
	mtp = Loader.loadMTPFromURL(b, url);
      
	addMTP(b, mtp);
	if(log.doDebug()) {
	  log.debug("Bundle " + b.getBundleId() + ": loaded mtp " + mtp.getId());
	}
      } catch (Exception e) {
	log.error("Faiiled to load metatype XML from bundle " + b.getBundleId(), e);
	throw e;
      }
    }

    if(valStr.startsWith("!")) {
      url = b.getResource(valStr.substring(1));
    } else {
      url = new URL(valStr);
    }
    if(url == null) {
      if(log.doDebug()) {
	log.debug("No cm defaults xml in bundle " + b.getBundleId());
      }
    } else {
      
      try {
	Loader.loadDefaultsFromURL(mtp, url);
      	if(log.doDebug()) {
	  log.debug("Bundle " + b.getBundleId() + ": loaded default values");
	}
      } catch (Exception e) {
	log.error("Failed to load cm defaults XML from bundle " + b.getBundleId(), e);
	throw e;
      }
    }

    return mtp;
  }

  void addMTP(Bundle b, MTP mtp) {
    synchronized(providers) {
      providers.put(b, mtp);
    }
  }

  public String[] getPids() {
    synchronized(providers) {
      Set set = new HashSet();
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
	Bundle b   = (Bundle)it.next();
	MTP    mtp = (MTP)providers.get(b);
	String[] pids = mtp.getPids();
	for(int i = 0; i < pids.length; i++) {
	  set.add(pids[i]);
	}
      }
      return (String[])set.toArray();
    }
  }
  
  public String[] getFactoryPids() {
    synchronized(providers) {
      Set set = new HashSet();
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
	Bundle b   = (Bundle)it.next();
	MTP    mtp = (MTP)providers.get(b);
	String[] pids = mtp.getFactoryPids();
	for(int i = 0; i < pids.length; i++) {
	  set.add(pids[i]);
	}
      }
      return (String[])set.toArray();
    }
  }

  public String[] getLocales() {
    return null;
  }


  /**
   * Get a loaded metatype provider, given a bundle.
   *
   * @return Provider if such provider is found, otherwise <tt>null</tt>.
   */
  public MTP getMTP(Bundle b) {
    if(b == null) {
      return cmMTP;
    }
    MTP mtp = (MTP)providers.get(b);
    if(mtp != null) {
      return mtp;
    }
    if(b.getBundleId() == 0) {
      return cmMTP;
    }
    return null;
  }

  /**
   * Get an ObjectClassDefinition given a PID.
   *
   * @return ObjectClassDefinition if PID exists, otherwise <tt>null</tt>.
   */
  public ObjectClassDefinition getObjectClassDefinition(String pid, 
							String locale) {
    
    synchronized(providers) {
      Set set = new HashSet();
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
	Bundle b   = (Bundle)it.next();
	MTP    mtp = (MTP)providers.get(b);
	
	ObjectClassDefinition  ocd = 
	  mtp.getObjectClassDefinition(pid, null);
	if(ocd != null) {
	  return ocd;
	}
      }
      return null;
    }
  }

  Set getCMServicePIDs() {
    Set pids = new HashSet();
    ConfigurationAdmin ca = getCA();
    if(ca != null) {
      try {
	Configuration[] configs = ca.listConfigurations("(service.pid=*)");
	for(int i = 0; configs != null && i < configs.length; i++) {
	  if(configs[i].getFactoryPid() == null) {
	    pids.add(configs[i].getPid());
	  }
	}
      } catch (Exception e) {
	log.error("Failed to get service pids", e);
      }
    }
    return pids;
  }

  Set getCMFactoryPIDs() {
    Set pids = new HashSet();
    ConfigurationAdmin ca = getCA();
    if(ca != null) {
      try {
	Configuration[] configs = ca.listConfigurations("(service.pid=*)");
	for(int i = 0; configs != null && i < configs.length; i++) {
	  if(configs[i].getFactoryPid() != null) {
	    pids.add(configs[i].getFactoryPid());
	  }
	}
      } catch (Exception e) {
	log.error("Failed to get service pids", e);
      }
    }
    return pids;
  }

  ConfigurationAdmin getCA() {
    return (ConfigurationAdmin)cmTracker.getService();
  }
}
