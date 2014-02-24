/*
 * Copyright (c) 2006-2013, KNOPFLERFISH project
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

/**
 * @author Erik Wistrand
 * @author Philippe Laporte
 */

package org.knopflerfish.bundle.metatype;

import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;

import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.metatype.KFLegacyMetaTypeParser;
import org.knopflerfish.util.metatype.MTP;
import org.knopflerfish.util.metatype.OCD;
import org.knopflerfish.util.metatype.SystemMetatypeProvider;

public class Activator
  implements BundleActivator
{
  BundleContext bc;

  LogRef log;

  SystemMetatypeProvider sysMTP;
  SysPropMetatypeProvider spMTP;
  Map<ServiceRegistration<MetaTypeProvider>, MTP> confMtpRegs =
      new HashMap<ServiceRegistration<MetaTypeProvider>, MTP>();

  public void start(BundleContext _bc)
  {

    this.bc = _bc;
    this.log = new LogRef(bc);

    sysMTP = new SystemMetatypeProvider(bc, confMtpRegs);
    sysMTP.open();

    bc.registerService(new String[] { SystemMetatypeProvider.class.getName(),
                                     MetaTypeProvider.class.getName(),
                                     MetaTypeService.class.getName() }, sysMTP,
                       (Dictionary<String, ?>) null);

    final ManagedService config = new ManagedService() {

      public void updated(Dictionary<String, ?> props)
      {

        synchronized (confMtpRegs) {
          Vector<String> urls = null;
          if (props != null) {
            @SuppressWarnings("unchecked")
            final
            Vector<String> value =
              (Vector<String>) props.get("external.metatype.urls");
            urls = value;
          }
          if (urls == null) {
            urls = new Vector<String>();
          }

          final MTP[] mtp = new MTP[urls.size()];

          try {
            for (int i = 0; i < urls.size(); i++) {
              final URL url = new URL(urls.elementAt(i));
              mtp[i] = KFLegacyMetaTypeParser.loadMTPFromURL(bc.getBundle(), url);
            }

            for (final ServiceRegistration<?> reg : confMtpRegs.keySet()) {
              reg.unregister();
            }
            confMtpRegs.clear();

            for (int i = 0; i < mtp.length; i++) {
              final Dictionary<String, Object> prop =
                new Hashtable<String, Object>();
              prop.put("source.url", urls.elementAt(i));
              String[] pids = mtp[i].getPids();
              if (pids != null) {
                prop.put("service.pids", pids);
              }
              pids = mtp[i].getFactoryPids();
              if (pids != null) {
                prop.put("factory.pids", pids);
              }

              final ServiceRegistration<MetaTypeProvider> reg =
                bc.registerService(MetaTypeProvider.class, mtp[i], prop);

              confMtpRegs.put(reg, mtp[i]);
            }
          } catch (final Exception e) {
            log.error("Failed to set values", e);
          }
        } // synchronized
      } // method
    };

    final Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("service.pid",
              "org.knopflerfish.util.metatype.SystemMetatypeProvider");
    bc.registerService(ManagedService.class, config, props);

    setupSystemProps();

  }

  void setupSystemProps()
  {

    final ManagedService config = new ManagedService() {

      public void updated(Dictionary<String, ?> props)
      {
        if (props != null) {
          for (final Enumeration<String> e = props.keys(); e.hasMoreElements();) {
            final String key = e.nextElement();
            final Object val = props.get(key);

            if (val != null) {
              try {
                System.setProperty(key, val.toString());
              } catch (final Exception ex) {
                log.error("Failed to set system property '" + key + "' to "
                          + val, ex);
              }
            }
          }
        }
      }
    };

    final Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("service.pid", SysPropMetatypeProvider.PID);

    bc.registerService(ManagedService.class, config, props);

    spMTP = new SysPropMetatypeProvider(bc);

    bc.registerService(new String[] { MetaTypeProvider.class.getName() },
                       spMTP, new Hashtable<String,Object>() {
                        private static final long serialVersionUID = 1L;
                        {
                           put("service.pids", spMTP.getPids());
                         }
                       });

  }

  public void stop(BundleContext bc)
  {
    sysMTP.close();
    this.log.close();
    this.log = null;
    this.bc = null;
  }
}

class SysPropMetatypeProvider
  extends MTP
{
  OCD spOCD;
  static final String PID = "java.system.properties";

  SysPropMetatypeProvider(BundleContext bc)
  {
    super("System properties");

    final Dictionary<String, Object> defProps = new Hashtable<String, Object>();

    final Properties sysProps = System.getProperties();

    for (final Enumeration<?> e = sysProps.keys(); e.hasMoreElements();) {
      final String key = (String) e.nextElement();
      // Use the local value for the current framework instance; props
      // that have not been exported with some value as system
      // properties will not be visible due to the limitation of
      // BundleContex.getProprty() on OSGi R4.
      final Object val = bc.getProperty(key);
      if (key.startsWith("java.") || key.startsWith("os.")
          || key.startsWith("sun.") || key.startsWith("awt.")
          || key.startsWith("user.")) {
        continue;
      }
      if (null != val) {
        defProps.put(key, val);
      }
    }

    spOCD = new OCD(PID, PID, "Java system properties", defProps);

    addService(PID, spOCD);
  }
}
