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

package org.knopflerfish.bundle.dirdeployer;

import java.util.*;
import java.io.File;
import org.osgi.framework.*;
import org.osgi.service.cm.ManagedService;

/**
 * Configuration class for the directory deployer.
 *
 * This class first initalizes itself with values from
 * system properties. If it is registered as a 
 * <tt>ManagedService</tt>, it also handles calls
 * to the <tt>update</tt> method. The <tt>register</tt> method
 * does this registration.
 */
public class Config implements ManagedService {

  // PID string used for service.pid
  static final String PID             = "org.osgi.fileinstall";

  // Property names used both as system properties and as CM properties
  static final String PROP_DIRS       = "org.osgi.fileinstall.dir";
  static final String PROP_INTERVAL   = "org.osgi.fileinstall.poll";
  static final String PROP_STARTLEVEL = "org.osgi.fileinstall.startlevel";

  static final String DEFAULT_DIR = "./load";

  // directories to scan
  File[] dirs = new File[0];

  // sleep interval for scan thread
  long      interval = 1000;    
  
  // if true, uninstall all bundles when scan thread stops
  boolean bUninstallOnStop = true;  

  // start level for installed bundles
  int     startLevel = 1;

  // framework registration
  ServiceRegistration reg;

  public Config() {
    // init with default values
    updated(getDefaults());
  }
  
  
  void register() {
    if(reg != null) {
      return;
    }

    Hashtable props = new Hashtable();
    props.put("service.pid", PID);
    
    reg = Activator.bc.registerService(ManagedService.class.getName(),
				       this,
				       props);
    
  }

  void unregister() {
    if(reg == null) {
      return;
    }

    reg.unregister();
    reg = null;
  }


  public void updated(Dictionary props) {

    if(props == null) {
      props = getDefaults();
    }

    String dirPaths = (String)props.get(PROP_DIRS);
    if(dirPaths != null) {
      StringTokenizer st = new StringTokenizer(dirPaths, ",");
      dirs = new File[st.countTokens()];
      
      int i = 0;
      while (st.hasMoreTokens()) {
	File dir = new File(st.nextToken().trim());
	dirs[i++] = dir;
      }
    } else {
      dirs = new File[0];
    }

    Integer iVal = (Integer)props.get(PROP_STARTLEVEL);
    if(iVal != null) {
      startLevel = iVal.intValue();
    }

    Long lVal = (Long)props.get(PROP_INTERVAL);
    if(lVal != null) {
      interval = lVal.longValue();
    }
  }

  Dictionary getDefaults() {

    Hashtable props = new Hashtable();

    props.put(PROP_DIRS, 
	      System.getProperty(PROP_DIRS, DEFAULT_DIR));
    
    props.put(PROP_STARTLEVEL, 
	      Integer.getInteger(PROP_STARTLEVEL, new Integer(startLevel)));
    
    props.put(PROP_INTERVAL,
	      Long.getLong(PROP_INTERVAL,  new Long(interval)));

    return props;
  }
}

