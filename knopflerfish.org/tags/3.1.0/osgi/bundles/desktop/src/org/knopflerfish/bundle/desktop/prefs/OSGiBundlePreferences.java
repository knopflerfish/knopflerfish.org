/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.prefs;

import java.util.*;
import java.util.prefs.*;

import org.knopflerfish.bundle.desktop.swing.Activator;
import org.knopflerfish.bundle.desktop.swing.Util;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.log.LogRef;

import org.osgi.service.prefs.PreferencesService;


/**
 *
 */
public class OSGiBundlePreferences extends MountedPreferences
{
  // A local copy of the log owned by our Activator since the EDT
  // scheduled jobs may refer to Activator.log after it has been set
  // to null.
  static public LogRef    log = Activator.log;

  protected Bundle        bundle;
  protected BundleContext bc;
  OSGiPreferences         sysNode;
  OSGiUsersPreferences    usersNode;
  ServiceTracker          psTracker;
  PreferencesService      ps;

  String SYS_NAME   = "sys";
  String USERS_NAME = "users";

  public OSGiBundlePreferences(Bundle bundle) {
    super();
    this.bundle = bundle;
    this.bc     = Util.getBundleContext(bundle);

    if(bc == null) {
      log.debug("No BC for " + Util.getBundleName(bundle));
    } else {
      psTracker = new ServiceTracker(bc,
                                     PreferencesService.class.getName(),
                                     null) {
          public Object addingService(ServiceReference sr) {
            Object obj = super.addingService(sr);
            ps = (PreferencesService)obj;

            mountService();

            return obj;
          }
          public void removedService(ServiceReference sr, Object service) {
            ps = null;
            unmountService();
            super.removedService(sr, service);
          }
        };
    }
  }

  public Bundle getBundle() {
    return bundle;
  }

  public void open() {
    if(bundle == null) {
      throw new IllegalStateException("Bundle = null");
    }
    if(psTracker != null) {
      psTracker.open();
    }
  }

  public void close() {
    if(psTracker != null) {
      int state = bundle.getState();
      if(0 != (state & (Bundle.ACTIVE | Bundle.STARTING | Bundle.STOPPING))) {
        try {
          log.debug("close tracker for " + bundle + ", state=" + state);
          psTracker.close();
        } catch (Exception e) {
          log.debug("Failed to close tracker", e);
        }
      } else {
        log.debug("skip tracker close since state=" + state);
      }
    }
    bundle    = null;
    bc        = null;
    psTracker = null;
    ps        = null;
    sysNode   = null;
    usersNode = null;
  }

  void unmountService() {

    if(sysNode != null) {
      unmount(SYS_NAME);
      sysNode   = null;
    }
    if(usersNode != null) {
      unmount(USERS_NAME);
      usersNode = null;
    }
  }

  void mountService() {

    if(ps != null) {
      if(sysNode == null) {
        sysNode   = new OSGiPreferences(null, ps.getSystemPreferences());
        mount(sysNode, SYS_NAME);
      }

      if(usersNode == null) {
        usersNode = new OSGiUsersPreferences(null, ps);
        mount(usersNode, USERS_NAME);
      }
    } else {
      log.warn("mount failed, no PreferencesService, "
               +Util.getBundleName(bundle));
    }
  }
}
