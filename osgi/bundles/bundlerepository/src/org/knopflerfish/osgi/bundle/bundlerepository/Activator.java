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
import java.lang.reflect.*;
import org.osgi.framework.*;
import org.ungoverned.osgi.bundle.bundlerepository.*;
import org.ungoverned.osgi.service.bundlerepository.BundleRepositoryService;


public class Activator implements BundleActivator
{
  private transient BundleContext               bc = null;
  private transient BundleRepositoryServiceImpl m_brs = null;
  
  public void start(BundleContext context) {
    bc = context;
    
    
    m_brs = new BundleRepositoryServiceImpl(bc);

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


    bc.
      registerService(BundleRepositoryService.class.getName(),
		      m_brs, 
		      null);

    registerOscarShell();

    // register Knopflerfish console service if possible
    tryObject("org.knopflerfish.osgi.bundle.bundlerepository.ObrCommandGroup",
	      "register");
    
    // register Knopflerfish desktop plugin service if possible
    tryObject("org.knopflerfish.osgi.bundle.bundlerepository.desktop.OBRDisplayer",
	      "register");
  }

  public void stop(BundleContext context) {
  }

  void registerOscarShell() {
    // Register Oscar shell service if possible
    try {
      Class clazz = Class.forName("org.ungoverned.osgi.bundle.bundlerepository.ObrCommandImpl");
      Constructor cons = clazz.getConstructor(new Class[] {
	BundleContext.class,
	BundleRepositoryService.class,
      });
      Object obj = cons.newInstance(new Object[] { bc, m_brs });
      
      bc.registerService("org.ungoverned.osgi.service.shell.Command", obj, null);
      //      System.out.println("Registered oscar shell service");
    }  catch (Throwable th)  {
      //      System.out.println("No oscar shell service: " + th);
    }
  }

  /**
   * Try to create an instance of a named class and call a method in it.
   * <p>
   * This is done using reflection, since DynamicImport-Package won't
   * resolve external bundles at the same time a bundle itself is in
   * progress of being resolved.
   * </p>
   */
  void tryObject(String className, String methodName) {
    try {
      Class clazz = Class.forName(className);
      Constructor cons = clazz.getConstructor(new Class[] {
	BundleContext.class 
      });
      Object obj = cons.newInstance(new Object[] { bc });
      Method m = clazz.getMethod(methodName, null);
      m.invoke(obj, null);

      //      System.out.println("invoked " + m);
    }  catch (Throwable th) {
      //      System.out.println("No " + className + " available: " + th);
    }    
  }
}
