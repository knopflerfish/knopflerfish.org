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

package org.knopflerfish.bundle.log;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import org.osgi.framework.ServiceReference;

import org.osgi.service.log.LogService;

import org.knopflerfish.service.console.CommandGroup;

/**
 ** Bundle activator for <code>log_gs</code>.
 **
 ** @author  Gatespace AB
 ** @version $Revision: 1.1 $
 **/
public class Activator implements BundleActivator {

  static final String[] LOG_SERVICE_CLASSES = 
  { 
    org.osgi.service.log.LogService.class.getName(), 
    org.knopflerfish.service.log.LogService.class.getName() 
  };

  static final String LOG_READER_SERVICE_CLASS
    = org.osgi.service.log.LogReaderService.class.getName();

  static final String COMMAND_GROUP
    = org.knopflerfish.service.console.CommandGroup.class.getName();
  

  private LogServiceFactory   lsf;
  private ServiceRegistration lsfReg;

  private LogReaderServiceFactory lrsf;
  private ServiceRegistration     lrsfReg;

  private LogConfig           lc;

  private CommandGroup        logConfigCommandGroup;
  private ServiceRegistration logConfigCommandGroupReg;

  /** BundleActivator callback.*/
  public void start(BundleContext bc) throws BundleException {
      

    lc   = new LogConfig(bc);
    lrsf = new LogReaderServiceFactory(bc, lc);
    lsf  = new LogServiceFactory(lrsf);

    // Catch all framework error and place in the log.
    LogFrameworkListener lfl = new LogFrameworkListener(lrsf);
    bc.addFrameworkListener( lfl );
    bc.addBundleListener( lfl );
    bc.addServiceListener( lfl );

    // Register our services
    lsfReg  = bc.registerService(LOG_READER_SERVICE_CLASS, lrsf, null);
    lrsfReg = bc.registerService(LOG_SERVICE_CLASSES, lsf, null);

    lc.setLogReaderServiceFactory(lrsf);

    try {
      logConfigCommandGroup = new LogConfigCommandGroup( bc, lc );
      java.util.Hashtable props = new java.util.Hashtable();
      props.put("groupName", logConfigCommandGroup.getGroupName());
      logConfigCommandGroupReg
	= bc.registerService(COMMAND_GROUP, logConfigCommandGroup, props);
    } catch (Exception e) {
      lrsf.log( new LogEntryImpl
	(bc.getBundle(),
	 LogService.LOG_ERROR,
	 "Failed to register logconfig console command group.",
	 e ) );
    }
  }


  /** BundleActivator callback.*/
  public void stop(BundleContext bc) {
    lrsf.log( new LogEntryImpl(bc.getBundle(),
			       LogService.LOG_INFO,
			       "Log stopped."));
    lrsf.stop();
  }

}
