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

package org.knopflerfish.bundle.junit;

import junit.framework.*;
import org.osgi.framework.*;
import java.util.*;
import org.knopflerfish.service.junit.*;
import java.lang.reflect.*;

public class Activator implements BundleActivator {
  static BundleContext       bc;
  static LogRef              log;

  HttpExporter httpExporter;
  JUnitService junitService;
  

  public void start(BundleContext bc) {
    this.bc = bc;
    this.log = new LogRef();

    {
      junitService = new JUnitServiceImpl();

      Hashtable props = new Hashtable();
      bc.registerService(JUnitService.class.getName(), junitService, props);
    }

    tryObject("org.knopflerfish.bundle.junit.HttpExporter", "open");
    //    httpExporter = new HttpExporter();
    //    httpExporter.open();

    // register to Knopflerfish console service if possible
    tryObject("org.knopflerfish.bundle.junit.JUnitCommandGroup",
	      "register");
    

  }

  public void stop(BundleContext bc) {
    this.bc = null;
  }

  class LogRef {
    public void info(String msg) {
      System.out.println("INFO: " + msg);
    }

    public void error(String msg, Throwable t) {
      System.out.println("ERROR: " + msg);
      if(t != null) {
	t.printStackTrace();
      }
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

