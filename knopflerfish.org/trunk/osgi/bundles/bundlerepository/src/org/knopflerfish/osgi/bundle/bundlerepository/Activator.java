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

package org.knopflerfish.osgi.bundle.bundlerepository;

import java.util.*;
import org.osgi.framework.*;
import org.ungoverned.osgi.bundle.bundlerepository.*;
import org.ungoverned.osgi.service.bundlerepository.BundleRepositoryService;
import org.knopflerfish.osgi.bundle.bundlerepository.*;

public class Activator implements BundleActivator
{
  private transient BundleContext               m_context = null;
  private transient BundleRepositoryServiceImpl m_brs = null;
  
  public void start(BundleContext context) {
    m_context = context;
    
    
    m_brs = new BundleRepositoryServiceImpl(m_context);

    try {
      // when running on KF, default to KF repo if nothing else is specified
      if("knopflerfish".equals(context.getProperty(Constants.FRAMEWORK_VENDOR).toLowerCase())) {
	String repoURL = System.getProperty("oscar.repository.url");
	if(repoURL == null || "".equals(repoURL)) {      
	  m_brs.setRepositoryURLs(new String[] {
	    "http://www.knopflerfish.org/repo/repository.xml"
	  });
	}
      }
    } catch (Throwable ignored) { 
    }


    m_context.
      registerService(BundleRepositoryService.class.getName(),
		      m_brs, 
		      null);
    
    // We dynamically import the necessary shell/command APIs, so
    // they might not actually be available, so be ready to catch
    // the exception
    try
      {
	// Register "obr" shell command service as a
	// wrapper for the bundle repository service.
	context.registerService(
				org.ungoverned.osgi.service.shell.Command.class.getName(),
				new ObrCommandImpl(m_context, m_brs), null);
      }
    catch (Throwable th)  {
      // Ignore.
    }
    
    try {
      ObrCommandGroup cg = new ObrCommandGroup(m_context, m_brs);
      cg.register();
    } catch (Throwable th) {
      // Ignore.
    }
  }
  
  public void stop(BundleContext context) {
  }


}
