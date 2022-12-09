/*
 * Copyright (c) 2006-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import java.io.IOException;
import java.util.Hashtable;

import org.knopflerfish.bundle.component.CMConfig.CMPid;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Service Component Runtime
 */
class CMHandler implements ConfigurationListener
{
  private final SCR scr;
  private final Hashtable<String, CMPid[]> configSubscriber = new Hashtable<>();
  private final ServiceTracker<ConfigurationAdmin,ConfigurationAdmin> cmAdminTracker;

  private ServiceRegistration<ConfigurationListener> cmListener = null;


  /**
   *
   */
  CMHandler(SCR scr) {
    this.scr = scr;
    cmAdminTracker = new ServiceTracker<>(scr.bc, ConfigurationAdmin.class, null);
  }


  /**
   * Start SCR.
   *
   */
  void start() {
    cmAdminTracker.open();
    cmListener = scr.bc.registerService(ConfigurationListener.class, this, null);
  }


  /**
   * Stop CM handler.
   *
   */
  void stop() {
    if (cmListener != null) {
      cmListener.unregister();
      cmListener = null;
    }
    cmAdminTracker.close();
  }


  //
  // ConfigurationListener method
  //

  /**
   *
   */
  public void configurationEvent(ConfigurationEvent evt) {
    scr.postponeCheckin();
    try {
      final String factoryPid = evt.getFactoryPid();
      final String pid = evt.getPid();
      final CMPid [] cmps = configSubscriber.get(factoryPid != null ? factoryPid : pid);
      if (cmps != null) {
        switch (evt.getType()) {
        case ConfigurationEvent.CM_DELETED:
          for (final CMPid cmp : cmps) {
            cmp.configDeleted(factoryPid, pid);
          }
          break;
        case ConfigurationEvent.CM_UPDATED:
          for (final CMPid cmp : cmps) {
            cmp.configUpdated(factoryPid, pid);
          }
          break;
        case ConfigurationEvent.CM_LOCATION_CHANGED:
          // TODO should we log this?
          break;
        default:
          Activator.logDebug("Unknown ConfigurationEvent type: " + evt.getType());
          break;
        }
      }
    } finally {
      scr.postponeCheckout();
    }
  }


  void addSubscriberCMPid(final String id, CMPid cmp) {
    final CMPid [] old = configSubscriber.get(id);
    if (old != null) {
      final CMPid [] n = new CMPid[old.length + 1];
      System.arraycopy(old, 0, n, 0, old.length);
      n[old.length] = cmp;
      configSubscriber.put(id, n);
    } else {
      configSubscriber.put(id, new CMPid [] {cmp});
    }
  }


  void removeSubscriberCMPid(String id, CMPid cmp) {
    final CMPid [] old = configSubscriber.remove(id);
    if (old != null) {
      if (old.length > 1) {
        final CMPid [] n = new CMPid[old.length - 1];
        int j = 0;
        for (final CMPid element : old) {
          if (element != cmp) {
            n[j++] = element;
          }
        }
        configSubscriber.put(id, n);
      }
    } else {
      Activator.logError("Removed unknown subscriber: " + cmp);
    }
  }


  /**
   * Get all CM configurations for specified CM pid or factory pid.
   */
  Configuration [] listConfigurations(String key, String pid) {
    final ConfigurationAdmin cm = cmAdminTracker.getService();
    if (cm != null) {
      try {
        return cm.listConfigurations("(" + key + "=" + pid + ")");
      } catch (final InvalidSyntaxException e) {
        Activator.logError("Strange CM PID: " + pid, e);
      } catch (final IOException e) {
        Activator.logError("SCR could not retrieve the configuration for pid: " +
                           pid + ". Got IOException.", e);
      }
    }
    return null;
  }


}
