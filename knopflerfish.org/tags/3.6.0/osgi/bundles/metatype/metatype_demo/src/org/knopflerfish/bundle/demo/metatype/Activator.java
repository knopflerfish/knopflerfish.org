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

package org.knopflerfish.bundle.demo.metatype;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.knopflerfish.service.log.*;

import java.util.*;

import org.knopflerfish.util.metatype.*;
import org.osgi.service.metatype.*;

public class Activator implements BundleActivator {

  static public LogRef        log;
  static public BundleContext bc;

  
  public void start(BundleContext _bc) {
    this.bc  = _bc;
    this.log = new LogRef(bc);

    doSetup();
  }

  void doSetup() {
    ManagedService ms = new ManagedService() {
	public void updated(Dictionary props) {
	  System.out.println("managedservice service got " + props);
	}
      };

    ManagedServiceFactory mf = new ManagedServiceFactory() {
	public void deleted(java.lang.String pid) {
	  log.info("factory deleted " + pid);
	}
	public java.lang.String getName() {
	  return "my factory";
	}
	
	public void updated(String pid, Dictionary props) {
	  log.info("factory updated pid=" + pid + 
		   ", props=" + props);
	}
      };

    Hashtable props = new Hashtable();
    props.put("service.pid", "service1");
    bc.registerService(ManagedService.class.getName(), ms, props);

    props.put("service.pid", "testconfig");
    bc.registerService(ManagedService.class.getName(), ms, props);

    props = new Hashtable();
    props.put("service.pid", "factory1");
    bc.registerService(ManagedServiceFactory.class.getName(), mf, props);
  }

  public void stop(BundleContext bc) {
    try {
      if(log != null) {
	log = null;
      }
	
      this.bc = null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
