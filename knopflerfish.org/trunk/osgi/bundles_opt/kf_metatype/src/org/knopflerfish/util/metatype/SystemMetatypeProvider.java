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
import org.knopflerfish.service.log.LogRef;

import java.util.*;
import java.io.*;
import java.net.*;

public class SystemMetatypeProvider 
  implements MetaTypeProvider, PIDProvider {

  BundleContext bc;
  LogRef        log;

  Map providers = new HashMap();

  public static final String METATYPE_RESOURCE    = "!/metatype.xml";
  public static final String CMDEFAULTS_RESOURCE  = "!/cmdefaults.xml";
  public static final String ATTRIB_METATYPEURL   = "Bundle-MetatypeURL";
  public static final String ATTRIB_CMDEFAULTSURL = "Bundle-CMDefaultsURL";

  public SystemMetatypeProvider(BundleContext bc) {
    this.bc = bc;
    log = new LogRef(bc);

    BundleListener bl = new BundleListener() {
	public void bundleChanged(BundleEvent ev) {
	  switch(ev.getType()) {
	  case BundleEvent.INSTALLED:
	    try {
	      MTP mtp = loadMTP(ev.getBundle());
	    } catch (Exception e) {
	      log.error("Failed to handle bundle " + 
			ev.getBundle().getBundleId(), e);
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
    
  }

  MTP loadMTP(Bundle b) throws Exception {

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
    } else {
      url = new URL(defStr);
    }
    if(url == null) {
      if(log.doDebug()) {
	log.debug("No metatype xml in bundle " + b.getBundleId());
      }
    } else {
      
      try {
	mtp = Loader.loadMTPFromURL(url);
      
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
	log.error("Faiiled to load cm defaults XML from bundle " + b.getBundleId(), e);
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

  public Set getServicePIDs() {
    synchronized(providers) {
      Set set = new HashSet();
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
	Bundle b   = (Bundle)it.next();
	MTP    mtp = (MTP)providers.get(b);
	set.addAll(mtp.getServicePIDs());
      }
      return set;
    }
  }
  
  public Set getFactoryPIDs() {
    synchronized(providers) {
      Set set = new HashSet();
      for(Iterator it = providers.keySet().iterator(); it.hasNext();) {
	Bundle b   = (Bundle)it.next();
	MTP    mtp = (MTP)providers.get(b);
	set.addAll(mtp.getFactoryPIDs());
      }
      return set;
    }
  }

  public String[] getLocales() {
    return null;
  }


  public MTP getMTP(Bundle b) {
    return (MTP)providers.get(b);
  }

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
}
