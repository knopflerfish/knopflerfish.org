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

/**
 * @author Erik Wistrand
 * @author Philippe Laporte
 */

package org.knopflerfish.util.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;
import org.osgi.service.cm.*;
import org.osgi.util.tracker.*;
import org.knopflerfish.service.log.LogRef;

import java.util.*;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class SystemMetatypeProvider extends MTP implements MetaTypeService {

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
  Map            providers = new Hashtable();

  ServiceTracker cmTracker;


  // Special MTP which tracks CM configuration instances
  MTP            cmMTP;


  // String (pid) -> OCD
  Map cmOCDMap = new HashMap();

  /**
   * Create a SystemMetatypeProvider, using the specified bundle context
   * for listeners.
   */
  public SystemMetatypeProvider(BundleContext bc) {
    super("system");
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
          case BundleEvent.RESOLVED:
          case BundleEvent.UNRESOLVED:
          case BundleEvent.UPDATED:
            //NYI! Reduce the number of loadMTPs by combining U* events.
            //We can't read properties from the system bundle
            if(ev.getBundle().getBundleId() != 0) {
              try {
                loadMTP(ev.getBundle());
              } 
              catch (Exception e) {
                log.error("Failed to handle bundle " + ev.getBundle().getBundleId(), e);
                //e.printStackTrace(System.out);
              }
            }
            break;
          case BundleEvent.UNINSTALLED:
            Bundle b = ev.getBundle();
            if(b.getBundleId() != 0) {
              providers.remove(b);
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

    
    cmTracker = new ServiceTracker(bc, ConfigurationAdmin.class.getName(), null);
    cmTracker.open();
    
    // track CM configuration instances as MTPs on the system bundle.

    cmMTP = new MTP("[CM]") {
	  public String[] getPids() {
	    return MTP.toStringArray(getCMServicePIDs());
	  }

	  public String[] getFactoryPids() {
	    return  MTP.toStringArray(getCMFactoryPIDs());
	  }
	  
	  public String[] getLocales() {
	    return null;
	  }
	  
	  public ObjectClassDefinition getObjectClassDefinition(String pid, String locale) {
		  
	    OCD ocd = (OCD)cmOCDMap.get(pid);
	    if(ocd != null) {
	      //cached
	      return ocd;
	    }
	    
	    ConfigurationAdmin ca = (ConfigurationAdmin)cmTracker.getService();
	    if(ca != null) {
	    	try {
	    		Configuration[] configs = ca.listConfigurations(null);
	    		Configuration conf = null;
	    		for(int i = 0; configs != null && i < configs.length; i++) {
	    			if(pid.equals(configs[i].getPid()) ||
	    			   pid.equals(configs[i].getFactoryPid())) {
	    				conf = configs[i];
	    			}
	    		}
	    		if(conf != null) {
	    			Dictionary props = conf.getProperties();
	    			ocd = new OCD(pid, pid, pid + " from CM", props);
	    			cmOCDMap.put(pid, ocd);
	    			return ocd;
	    		} 
	    		else {
	    			throw new RuntimeException("No config for pid " + pid);
	    		}
	    	} 
	    	catch (Exception e) {
	    		log.error("Failed to get service pid " + pid, e);
	    		return null;
	    	}
	    } 
	    else {
	      log.warn("Failed to get CA when getting pid " + pid);
	      return null;
	    }
	  }
	};
	
    setupMTListener(); 
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
   * Explictly load a metatype provider from a bundle and cache
   * it for later retrival by <tt>getMTP</tt>.
   *
   * @throws Exception if loading fails
   */
  public void loadMTP(Bundle b) throws Exception {

    URL url;
    
    //try R4 first
    Enumeration metaTypeFiles;
    if (b.getState() == Bundle.INSTALLED) {
      Enumeration p = b.getEntryPaths(MetaTypeService.METATYPE_DOCUMENTS_LOCATION);
      if (p != null) {
        Vector tmp = new Vector();
    	while(p.hasMoreElements()){ 
          tmp.addElement(b.getEntry((String)p.nextElement()));
    	}
        metaTypeFiles = tmp.elements();
      } else {
        metaTypeFiles = null;
      }
    } else {
      metaTypeFiles = b.findEntries(MetaTypeService.METATYPE_DOCUMENTS_LOCATION, "*", false);
    }
    if(metaTypeFiles != null){
    	BundleMetaTypeResource bmtr = new BundleMetaTypeResource(b);
    	
    	while(metaTypeFiles.hasMoreElements()){ 
    		url = (URL)metaTypeFiles.nextElement();
    		bmtr.mergeWith(Loader.loadBMTIfromUrl(bc, b, url));	
    	}
    	
    	bmtr.prepare();
    	
    	providers.put(b, bmtr);
    }
    else{
    	//proprietary legacy
    	
    	MTP mtp = null;
    
    	String defStr = (String)b.getHeaders().get(ATTRIB_METATYPEURL);

    	if(defStr == null || "".equals(defStr)) {
    		defStr = METATYPE_RESOURCE;
    	}

    	if(defStr.startsWith("!")) {
    		url = b.getEntry(defStr.substring(1));
    	} 
    	else if(defStr.startsWith("/")) {
    		url = b.getEntry(defStr);
    	} 
    	else {
    		url = new URL(defStr);
    	}
    
    	if(url != null) {
    		try {
    			mtp = Loader.loadMTPFromURL(b, url);
    			providers.put(b, mtp);
    		} 
    		catch (Exception e) {
    			log.info("Failed to load metatype XML from bundle " + b.getBundleId(), e);
    			//throw e;
    		}
    	}
    
    	//defaults are specified in the file itself in R4
    
    	String valStr = (String)b.getHeaders().get(ATTRIB_CMDEFAULTSURL);

    	if(valStr == null || "".equals(valStr)) {
    		valStr = CMDEFAULTS_RESOURCE;
    	}

    	if(valStr.startsWith("!")) {
    		url = b.getEntry(valStr.substring(1));
    	} 
    	else if(valStr.startsWith("/")) {
    		url = b.getEntry(valStr);
    	} 
    	else {
    		url = new URL(valStr);
    	}
    
    	if(url != null) {
    		try {
    			Loader.loadDefaultsFromURL(mtp, url);
    			log.info("Bundle " + b.getBundleId() + ": loaded default values");
    		} 
    		catch (Exception e) {
    			log.info("Failed to load cm defaults XML from bundle " + b.getBundleId(), e);
    			//throw e;
    		}
    	}
    } //proprietary legacy
    
  }

  public String[] getPids() {
    synchronized(providers) {
      Set set = new HashSet();
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
    	  Bundle b   = (Bundle)it.next();
    	  MetaTypeInformation    mtp = (MetaTypeInformation)providers.get(b);
    	  String[] pids = mtp.getPids();
    	  for(int i = 0; pids != null && i < pids.length; i++) {
    		  set.add(pids[i]);
    	  }
      }
      for(Iterator it = mtMap.keySet().iterator(); it.hasNext();) {
    	  ServiceReference sr = (ServiceReference)it.next();
    	  try {
    		  String[] pids = (String[])sr.getProperty("service.pids");
    		  for(int i = 0; pids != null && i < pids.length; i++) {
    			  set.add(pids[i]);
    		  }
    	  } 
    	  catch (Exception e) {
    		  log.warn("No service.pids property on " + sr);
    	  }
      }
      return MTP.toStringArray(set);
    }
  }
  
  public String[] getFactoryPids() {
    synchronized(providers) {
      Set set = new HashSet();
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
	Bundle b   = (Bundle)it.next();
	MetaTypeInformation mtp = (MetaTypeInformation)providers.get(b);
	String[] pids = mtp.getFactoryPids();
	for(int i = 0; pids != null && i < pids.length; i++) {
	  set.add(pids[i]);
	}
      }
      for(Iterator it = mtMap.keySet().iterator(); it.hasNext();) {
	ServiceReference sr = (ServiceReference)it.next();
	try {
	  String[] pids = (String[])sr.getProperty("factory.pids");
	  for(int i = 0; pids != null && i < pids.length; i++) {
	    set.add(pids[i]);
	  }
	} catch (Exception e) {
	  log.warn("No factory.pids property on " + sr);
	}
      }
      return MTP.toStringArray(set);
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
  public MetaTypeInformation getMTP(Bundle b) {
    ServiceReference cmSR = cmTracker.getServiceReference();

    MetaTypeInformation mti = null;
    
    if(cmSR != null && cmSR.getBundle() == b) {
      mti = cmMTP;
    } 
    else if(b.getBundleId() == 0) {
      mti = this;
    } 
    else {
      mti = (MetaTypeInformation)providers.get(b);
    }
    
    return mti;
  }

  /**
   * Get an ObjectClassDefinition given a PID.
   *
   * @return ObjectClassDefinition if PID exists, otherwise <tt>null</tt>.
   */
  public ObjectClassDefinition getObjectClassDefinition(String pid, String locale) {
    
    synchronized(providers) {
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
    	  Bundle b   = (Bundle)it.next();
    	  MetaTypeProvider    mtp = (MetaTypeProvider)providers.get(b);
	
    	  ObjectClassDefinition  ocd = mtp.getObjectClassDefinition(pid, locale);
    	  if(ocd != null) {
    		  return ocd;
    	  }
      }

      synchronized(mtMap) {
    	  for(Iterator it = mtMap.keySet().iterator(); it.hasNext();) {
    		  ServiceReference sr = (ServiceReference)it.next();
    		  MetaTypeProvider mt = (MetaTypeProvider)mtMap.get(sr);
    		  ObjectClassDefinition ocd = mt.getObjectClassDefinition(pid, locale);
    		  if(ocd != null) {
    			  return ocd;
    		  }	
    	  }
      }
      return null;
    }
  }

  ServiceListener mtListener = null;

  // ServiceReference -> MetatypeProvider
  Map mtMap = new HashMap();

  void setupMTListener() {
	  
    mtListener = new ServiceListener() {
	
    	public void serviceChanged(ServiceEvent ev) {
    		ServiceReference sr = ev.getServiceReference();
    		synchronized(mtMap) {
    			switch(ev.getType()) {
    				case ServiceEvent.REGISTERED: 
    					MetaTypeProvider mt = (MetaTypeProvider)bc.getService(sr);
    					if(mt != SystemMetatypeProvider.this) {
    						mtMap.put(sr, mt);
    					}
    					break;
    				case ServiceEvent.UNREGISTERING:
    					bc.ungetService(sr);
    					mtMap.remove(sr);
    					break;
    			}
    		}
    	}
    };
    
    try {
      String filter = "(objectclass=" + MetaTypeProvider.class.getName() + ")";
      
      ServiceReference[] srl = bc.getServiceReferences(null, filter);
      
      for(int i = 0; srl != null && i < srl.length; i++) {
    	  mtListener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
      }
      bc.addServiceListener(mtListener, filter);
      
    } 
    catch(Exception e) {
      log.error("Failed to get other providers", e);
    }
  
  }

  Set getCMServicePIDs() {
    Set pids = new HashSet();
    ConfigurationAdmin ca = (ConfigurationAdmin)cmTracker.getService();
    if(ca != null) {
      try {
    	  Configuration[] configs = ca.listConfigurations("(service.pid=*)");
    	  for(int i = 0; configs != null && i < configs.length; i++) {
    		  if(configs[i].getFactoryPid() == null) {
    			  pids.add(configs[i].getPid());
    		  }
    	  }
      } 
      catch (Exception e) {
    	  log.error("Failed to get service pids", e);
      }
    }
    return pids;
  }

  Set getCMFactoryPIDs() {
    Set pids = new HashSet();
    ConfigurationAdmin ca = (ConfigurationAdmin)cmTracker.getService();
    if(ca != null) {
      try {
    	  Configuration[] configs = ca.listConfigurations("(service.pid=*)");
    	  for(int i = 0; configs != null && i < configs.length; i++) {
    		  if(configs[i].getFactoryPid() != null) {
    			  pids.add(configs[i].getFactoryPid());
    		  }
    	  }
      } 
      catch (Exception e) {
    	  log.error("Failed to get service pids", e);
      }
    }
    return pids;
  }

  public MetaTypeInformation getMetaTypeInformation(Bundle bundle) {
		  
	  MetaTypeInformation mti;
	  
	  mti = (MetaTypeInformation)providers.get(bundle);
	  if(mti != null){
		  return mti;		
	  }

	  synchronized(mtMap) {	  
		  for(Iterator it = mtMap.keySet().iterator(); it.hasNext();) {
			  ServiceReference sr = (ServiceReference)it.next();
			  if(sr.getBundle() == bundle){
				  MetaTypeProvider mtp = (MetaTypeProvider)mtMap.get(sr);
				  if(!(mtp instanceof MetaTypeInformation)){
					  return new BundleMetaTypeProvider(mtp, sr);
				  }
				  else{
					  return (MetaTypeInformation) mtp;
				  }		
			  }
	      }
	      return null;
	  }
  }
}

class BundleMetaTypeProvider implements MetaTypeInformation{
	
	private MetaTypeProvider mtp;
	private Bundle bundle;
	
	//id -> MetaData
	private String[] pids;
	private String[] factoryPids;
	
	public BundleMetaTypeProvider(MetaTypeProvider mtp, ServiceReference sr){
		this.mtp = mtp;
		this.bundle = sr.getBundle();
		
		if(mtp instanceof ManagedService){
			pids = new String[1];
			pids[0] = (String) sr.getProperty(Constants.SERVICE_PID);
			factoryPids = new String[0];
		}
		else if(mtp instanceof ManagedServiceFactory){
			factoryPids = new String[1];
			factoryPids[0] = (String) sr.getProperty(Constants.SERVICE_PID);
			pids = new String[0];
		}
	}

	public Bundle getBundle() {
		return bundle;
	}
	
	public String[] getFactoryPids() {
		return factoryPids;  
	}

	public String[] getPids() {
		return pids;
	}
		
	public String[] getLocales() {
		return mtp.getLocales();
	}

	public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
		return mtp.getObjectClassDefinition(id, locale);		
	}
	
} //class
