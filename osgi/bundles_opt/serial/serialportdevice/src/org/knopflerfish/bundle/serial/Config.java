/**
 * Copyright (c) 2001 Gatespace AB. All Rights Reserved.
 *
 * $Header: /cvs/gs/gosg/gatespace_bundles/serialport/serialport/impl_src/Config.java,v 1.7 2002/04/03 14:06:33 ar Exp $
 * $Revision: 1.7 $
 */

package org.knopflerfish.bundle.serial;

import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

import org.knopflerfish.service.log.LogRef;

class Config implements ManagedServiceFactory {
  private final static String msfPid=
    "org.knopflerfish.serialport.factory";

  private final static String clazz=
    ManagedServiceFactory.class.getName();

  private final static String PID=
    "service.pid";

  private BundleContext bc;
  private final LogRef log;

  private ServiceRegistration sreg;
  private boolean dying;
  private Dictionary /*String->SPD*/ spds=new Hashtable();
  
  Config(LogRef log)
  {
    this.log = log;
  }

  void start(BundleContext bc) {
    this.bc=bc;

    Dictionary props=new Hashtable();
    props.put(PID,msfPid);

    if(dying) return;
    sreg=bc.registerService(clazz,this,props);
    synchronized(this) {
      if(!dying) return;
    }
    stop();
  }

  void stop() {
    synchronized(this) {
      if(sreg==null) {
	dying=true;
	return;
      }
    }
    sreg.unregister();
    synchronized(this) {
      for(Enumeration e=spds.elements();e.hasMoreElements();) {
	SPD s=(SPD)e.nextElement();
	s.close();
      }
      spds=null;
    }
  }

  public synchronized void
  updated(String pid, Dictionary conf) {
    String port=(String)conf.get("port");
    String product=(String)conf.get("product");
    String revision=(String)conf.get("revision");

    if (log.doDebug())
      log.debug("Confiured port="+port+
		", product="+product+
		", revision="+revision);
    
    deleted(pid);
    if(port!=null && product!=null) {
      SPD spd=new SPD();
      try {
	spds.put(pid,spd);
	spd.start(bc,port,product,revision);
      } catch(Exception e) {
	if (log.doError())
	  log.error("Failure starting serial port device", e);
	spds.remove(pid);	
      }
    }
  }

  public synchronized void deleted(String pid) {
    SPD spd=(SPD)spds.remove(pid);
    if(spd!=null) spd.close();
  }

  public String getName() {
    return "Serial port";
  }
}
