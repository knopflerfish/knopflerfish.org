/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.metatype;

import org.osgi.framework.*;
import org.osgi.service.metatype.*;

import java.net.URL;
import java.io.*;
import java.util.*;
import org.osgi.service.cm.*;

import org.knopflerfish.util.metatype.*;

import org.knopflerfish.service.log.LogRef;


public class Activator implements BundleActivator {
  static BundleContext bc = null;

  static LogRef log;

  SystemMetatypeProvider sysMTP;
  SysPropMetatypeProvider spMTP;

  public void start(BundleContext _bc) {
    this.bc = _bc;
    this.log = new LogRef(bc);

    sysMTP = new SystemMetatypeProvider(bc);
    sysMTP.open();

    bc.registerService(new String[] {
      SystemMetatypeProvider.class.getName(), 
      MetaTypeProvider.class.getName(),
      PIDProvider.class.getName(),
    },
		       sysMTP,
		       new Hashtable());

    ManagedService config = new ManagedService() {
	Map mtpRegs = new HashMap();

	public void updated(Dictionary props) {

	  synchronized(mtpRegs) {
	    Vector v = null;
	    if(props != null) {
	      v = (Vector)props.get("external.metatype.urls");
	    } 
	    if(v == null) {
	      v = new Vector();
	    }

	    MTP[] mtp = new MTP[v.size()];

	    try {
	      for(int i = 0; i < v.size(); i++) {
		URL url = new URL(v.elementAt(i).toString());
		mtp[i] = Loader.loadMTPFromURL(bc.getBundle(), url);
	      }
	      
	      for(Iterator it = mtpRegs.keySet().iterator(); it.hasNext();) {
		ServiceRegistration reg = (ServiceRegistration)it.next();
		reg.unregister();
	      }
	      mtpRegs.clear();
	      
	      for(int i = 0; i < mtp.length; i++) {
		Hashtable prop = new Hashtable();
		prop.put("source.url", v.elementAt(i).toString());
		String [] pids = mtp[i].getPids();
		if(pids != null) {
		  prop.put("service.pids", pids);
		}
		pids = mtp[i].getFactoryPids();
		if(pids != null) {
		  prop.put("factory.pids", pids);
		}
		//		System.out.println("register " + mtp[i].getId() + ", props=" + prop);
		ServiceRegistration reg = 
		  bc.registerService(MetaTypeProvider.class.getName(),
				     mtp[i], prop);
		mtpRegs.put(reg, mtp[i]);
	      }
	    } catch (Exception e) {
	      log.error("Failed to set values",e);
	    }
	  }
	}
      };

    Hashtable props = new Hashtable();
    props.put("service.pid", "org.knopflerfish.util.metatype.SystemMetatypeProvider");

    bc.registerService(ManagedService.class.getName(), config,
		       props);


    setupSystemProps();

  }

  void setupSystemProps() {
    ManagedService config = new ManagedService() {
	public void updated(Dictionary props) {
	  if(props != null) {
	    for(Enumeration e = props.keys(); e.hasMoreElements();) {
	      String key = (String)e.nextElement();
	      Object val = props.get(key);

	      if(val != null) {
		try {
		  System.setProperty(key, val.toString());
		} catch (Exception ex) {
		  log.error("Failed to set system property '" + key + "' to " + val, ex);
		}
	      }
	    }
	  }
	}
      };
    
    Hashtable props = new Hashtable();
    props.put("service.pid", SysPropMetatypeProvider.PID);
    
    bc.registerService(ManagedService.class.getName(), config,
		       props);

    
    spMTP = new SysPropMetatypeProvider();

    bc.registerService(new String[] {
      MetaTypeProvider.class.getName(),
      PIDProvider.class.getName()
    },
		       spMTP,
		       new Hashtable() {
			 {
			   put("service.pids", spMTP.getPids());
			 }
			 });
    
  }

  public void stop(BundleContext bc) {
    sysMTP.close();
    this.log.close();
    this.log = null;
    this.bc = null;
  }
}

class SysPropMetatypeProvider extends MTP {
  OCD spOCD;
  static final String PID = "java.system.properties";

  SysPropMetatypeProvider() {
    super("System properties");

    Hashtable defProps = new Hashtable();

    Properties sysProps = System.getProperties();

    for(Enumeration e = sysProps.keys(); e.hasMoreElements();) {
      String key = (String)e.nextElement();
      Object val = (String)sysProps.get(key);
      if(key.startsWith("java.") ||
	 key.startsWith("os.") ||
	 key.startsWith("sun.") ||
	 key.startsWith("awt.") ||
	 key.startsWith("user.")) {
	continue;
      }
      defProps.put(key, val);
    }


    spOCD = new OCD(PID,
		    PID,
		    "Java system properties",
		    defProps);

    addService(PID, spOCD);
  }
}

