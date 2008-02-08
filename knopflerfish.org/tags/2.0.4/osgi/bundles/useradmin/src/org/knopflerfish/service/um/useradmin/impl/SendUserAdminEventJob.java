/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

package org.knopflerfish.service.um.useradmin.impl;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.log.LogRef;

/**
 * An object that sends a given UserAdminEvent to all registered listeners.
 * 
 * @author Gunnar Ekolin
 * @version 
 */
final public class SendUserAdminEventJob implements Runnable {

  LogRef log;
  BundleContext  bc;
  ServiceTracker eventAdminTracker;
  UserAdminEvent event;
  Vector         listeners;
  
  SendUserAdminEventJob( BundleContext bc,
                         ServiceTracker eventAdminTracker,
                         UserAdminEvent event,
                         Vector listeners )
  {
    this.bc = bc;
    this.eventAdminTracker = eventAdminTracker;
    this.event = event;
    // Call only listeners that are present when the event happened.
    this.listeners = (Vector) listeners.clone();
    this.log = new LogRef(bc);
  }
  

  /** The base of the event admin path for user admin events. **/
  static final String basePath = "org/osgi/service/useradmin/UserAdmin/";

  /**
   * Derive event admin path for a user admin event.
   */
  String getEventAdminPath() {
    String evtType = "?";
    
    switch (event.getType()) {
      case UserAdminEvent.ROLE_CREATED:
        evtType = "ROLE_CREATED";
        break;
      case UserAdminEvent.ROLE_CHANGED:
        evtType = "ROLE_CHANGED";
        break;
      case UserAdminEvent.ROLE_REMOVED:
        evtType = "ROLE_REMOVED";
        break;
    }
    return basePath + evtType;
  }

  /**
   * Add key value pair to dictionary if value is non-null.
   *
   * @param dict The hashtable to add the key value pair to.
   * @param key  The key to put.
   * @param val  The value to put.
   */
  void put( Hashtable dict, String key, Object val)
  {
    if (null!=val)
      dict.put( key, val );
  }
  
  /**
   * Creates an event admin event for the user admin event object in
   * this job.
   */
  Event getEvent() {
    String    path = getEventAdminPath();
    Hashtable dict = new Hashtable();

    put( dict, EventConstants.EVENT_TOPIC, path );
    put( dict, EventConstants.EVENT, event );
    put( dict, EventConstants.TIMESTAMP,
         new Long(System.currentTimeMillis()) );
    put( dict, "role", event.getRole() );
    put( dict, "role.name", event.getRole().getName() );
    put( dict, "role.type", new Integer(event.getRole().getType()) );
    put( dict, EventConstants.SERVICE, event.getServiceReference() );
    put( dict, EventConstants.SERVICE_ID,
         event.getServiceReference().getProperty(Constants.SERVICE_ID) );
    put( dict, EventConstants.SERVICE_OBJECTCLASS,
         event.getServiceReference().getProperty(Constants.OBJECTCLASS) );
    put( dict, EventConstants.SERVICE_PID,
         event.getServiceReference().getProperty(Constants.SERVICE_PID) );

    return new Event( path, dict );
  }
  
  public void run()
  {
    EventAdmin ea = (EventAdmin) eventAdminTracker.getService();
    if (null!=ea) {
      ea.postEvent( getEvent() );
    }
    
    for (Enumeration en = listeners.elements(); en.hasMoreElements();) {
      ServiceReference sr = (ServiceReference) en.nextElement();
      UserAdminListener ual = (UserAdminListener) bc.getService(sr);
      if (ual != null) {
        try {
          ual.roleChanged(event);
        } catch (Throwable t) {
          log.error("[UserAdmin] Error while sending roleChanged event to"+sr,
                    t);
        }
      }
      bc.ungetService(sr);
    }
  }
  
}
