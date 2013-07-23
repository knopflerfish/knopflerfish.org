/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.cm;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.metatype.SystemMetatypeProvider;

public class Activator
  implements BundleActivator
{

  static public LogRef log;
  static public BundleContext bc;

  static ServiceTracker<SystemMetatypeProvider, SystemMetatypeProvider> mtpTracker;
  // static SystemMetatypeProvider sysMTP;

  static CMDisplayer disp;

  public void start(BundleContext _bc)
  {
    Activator.bc = _bc;
    Activator.log = new LogRef(bc);

    // bundle displayers
    disp = new CMDisplayer(bc);
    disp.open();
    disp.register();

    mtpTracker =
      new ServiceTracker<SystemMetatypeProvider, SystemMetatypeProvider>(
                                                                         bc,
                                                                         SystemMetatypeProvider.class,
                                                                         null);
    mtpTracker.open();

    /*
     * try {
     *
     *
     * test(); } catch (Exception e) { e.printStackTrace(); }
     */
  }

  static MetaTypeInformation getMTP(Bundle b)
  {
    final SystemMetatypeProvider sysMTP = mtpTracker.getService();
    if (sysMTP != null) {
      return sysMTP.getMTP(b);
    } else {
      log.warn("No SystemMetatypeProvider found");
      return null;
    }
  }

  void test()
  {
    final ManagedService ms = new ManagedService() {
      public void updated(Dictionary<String, ?> props)
      {
        System.out.println("managedservice service got " + props);
      }
    };

    final ManagedServiceFactory mf = new ManagedServiceFactory() {
      public void deleted(java.lang.String pid)
      {
        System.out.println("factory deleted " + pid);
      }

      public java.lang.String getName()
      {
        return "my factory";
      }

      public void updated(String pid, Dictionary<String, ?> props)
      {
        System.out.println("factory updated pid=" + pid + ", props=" + props);
      }
    };

    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put("service.pid", "service1");
    bc.registerService(ManagedService.class.getName(), ms, props);

    props.put("service.pid", "testconfig");
    bc.registerService(ManagedService.class.getName(), ms, props);

    props = new Hashtable<String, Object>();
    props.put("service.pid", "factory1");
    bc.registerService(ManagedServiceFactory.class.getName(), mf, props);
  }

  public void stop(BundleContext bc)
  {
    try {
      if (log != null) {
        log = null;
      }

      disp.close();
      disp = null;

      mtpTracker.close();
      mtpTracker = null;

      Activator.bc = null;
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

}
