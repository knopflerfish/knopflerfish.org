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
