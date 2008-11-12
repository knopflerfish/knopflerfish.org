/*
 * Copyright (c) 2005-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.event;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

import org.knopflerfish.service.log.LogRef;

/**
 * Listen for ConfigurationEvent
 *
 * @author Bj\u00f6rn Andersson
 */
public class ConfigurationListenerImpl implements ConfigurationListener {

  /**  hashtable of eventhandlers and timestamps */
  static Hashtable eventHandlers = new Hashtable();

  private LogRef log;
  private EventAdmin eventAdmin;
  private BundleContext bundleContext;

  public ConfigurationListenerImpl(EventAdmin eventAdmin, BundleContext bundleContext) {
    this.eventAdmin = eventAdmin;
    this.bundleContext = bundleContext;
    log = new LogRef(bundleContext);
    bundleContext.registerService(ConfigurationListener.class.getName(), this, null);
  }

  public void configurationEvent(ConfigurationEvent event) {
    Dictionary props = new Hashtable();
    String topic = "org/osgi/service/cm/ConfigurationEvent/";
    boolean knownMessageType = true;
    switch (event.getType()) {
    case ConfigurationEvent.CM_UPDATED:
      topic += "CM_UPDATED";
      break;
    case ConfigurationEvent.CM_DELETED:
      topic += "CM_DELETED";
      break;
    default:
      knownMessageType = false;
      break;
    }

    /* Stores the properties of the event in the dictionary, if the event is known */
    if (knownMessageType) {
      putProp(props, EventConstants.EVENT, event);
      putProp(props, "cm.factoryPid", event.getFactoryPid());
      putProp(props, "cm.pid", event.getPid());
      putProp(props, "service", event.getReference());
      putProp(props, "service.id", event.getReference().getProperty(Constants.SERVICE_ID));
      putProp(props, "service.objectClass", event.getReference().getProperty(Constants.OBJECTCLASS));
      putProp(props, "service.pid", event.getReference().getProperty(Constants.SERVICE_PID));

      try {
        eventAdmin.postEvent(new Event(topic, props));
      } catch (Exception e) {
        log.error("EXCEPTION in configurationEvent()", e);
      }
    } else {
      log.error("Recieved unknown configuration event (type="
                +event.getType() +"), discarding");
    }
  }

  private void putProp(Dictionary props, Object key, Object value) {
    if (value != null) {
      props.put(key, value);
    }
  }

}
