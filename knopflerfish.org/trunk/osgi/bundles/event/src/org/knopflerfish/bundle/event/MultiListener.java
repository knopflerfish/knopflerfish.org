/*
 * Copyright (c) 2005-2010, KNOPFLERFISH project
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

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Listen for LogEntries, ServiceEvents, FrameworkEvents, BundleEvents
 *
 * @author Magnus Klack, Martin Berg (refactoring by Bj\u00f6rn Andersson)
 */
public class MultiListener implements LogListener,
                                      ServiceListener,
                                      FrameworkListener,
                                      BundleListener {
  private LogRef log;

  public MultiListener() {
    log = new LogRef(Activator.bundleContext);

    Activator.bundleContext.addBundleListener(this);
    Activator.bundleContext.addServiceListener(this);
    Activator.bundleContext.addFrameworkListener(this);

    ServiceReference sr
      = Activator.bundleContext.getServiceReference(LogReaderService.class.getName());
    if (sr != null) {
      LogReaderService logReader = (LogReaderService) Activator.bundleContext.getService(sr);
      if (logReader != null) {
        logReader.addLogListener(this);
      }
    }
  }

  private static final String BUNDLE_EVENT_PREFIX = "org/osgi/framework/BundleEvent/";
  private static final String INSTALLED_TOPIC = BUNDLE_EVENT_PREFIX + "INSTALLED";
  private static final String STARTED_TOPIC = BUNDLE_EVENT_PREFIX + "STARTED";
  private static final String STOPPED_TOPIC = BUNDLE_EVENT_PREFIX + "STOPPED";
  private static final String UPDATED_TOPIC = BUNDLE_EVENT_PREFIX + "UPDATED";
  private static final String UNINSTALLED_TOPIC = BUNDLE_EVENT_PREFIX + "UNINSTALLED";
  private static final String RESOLVED_TOPIC = BUNDLE_EVENT_PREFIX + "RESOLVED";
  private static final String UNRESOLVED_TOPIC = BUNDLE_EVENT_PREFIX + "UNRESOLVED";

  /**
   * A listener for events sent by bundles
   * @param bundleEvent The event sent by the bundle
   * @author Johnny Baveras
   */


  public void bundleChanged(BundleEvent bundleEvent) {
    String topic = null;
    boolean knownMessageType = true;

    switch (bundleEvent.getType()) {
    case BundleEvent.INSTALLED:
      topic = INSTALLED_TOPIC;
      break;
    case BundleEvent.STARTED:
      topic = STARTED_TOPIC;
      break;
    case BundleEvent.STOPPED:
      topic = STOPPED_TOPIC;
      break;
    case BundleEvent.UPDATED:
      topic = UPDATED_TOPIC;
      break;
    case BundleEvent.UNINSTALLED:
      topic = UNINSTALLED_TOPIC;
      break;
    case BundleEvent.RESOLVED:
      topic = RESOLVED_TOPIC;
      break;
    case BundleEvent.UNRESOLVED:
      topic = UNRESOLVED_TOPIC;
      break;
    default:
      /* Setting the boolean to false if an unknown event arrives */
      knownMessageType = false;
      break;
    }


    /* Stores the properties of the event in the dictionary, if the event is known */
    if (knownMessageType) {
      if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
        return;
      }
      Dictionary props = new Hashtable();
      Bundle bundle = bundleEvent.getBundle();
      putProp(props, EventConstants.EVENT, bundleEvent);
      putProp(props, "bundle.id", new Long(bundle.getBundleId()));
      putProp(props, EventConstants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
      putProp(props, "bundle", bundle);
      /* Tries posting the event once the properties are set */
      try {
        Activator.eventAdmin.postEvent(new Event(topic, props));
      } catch (Exception e) {
        log.error("EXCEPTION in bundleChanged()", e);
      }
    } else {
      /* Logs an error if the event, which arrived, were of an unknown type */
      log.error("Recieved unknown bundle event message (type="
                +bundleEvent.getType() +"), discarding");
    }
    // }
  }

  private static final String LOG_PREFIX = "org/osgi/service/log/LogEntry/";
  private static final String LOG_ERROR_TOPIC = LOG_PREFIX + "LOG_ERROR";
  private static final String LOG_WARNING_TOPIC = LOG_PREFIX + "LOG_WARNING";
  private static final String LOG_INFO_TOPIC = LOG_PREFIX + "LOG_INFO";
  private static final String LOG_DEBUG_TOPIC = LOG_PREFIX + "LOG_DEBUG";
  private static final String LOG_OTHER_TOPIC = LOG_PREFIX + "LOG_OTHER";

  /**
   * A listener for entries in the log
   * @param logEntry the entry of the log
   * @author Johnny Baveras
   */
  public void logged(LogEntry logEntry) {
    String topic = null;

    switch (logEntry.getLevel()) {
    case LogRef.LOG_ERROR:
      topic = LOG_ERROR_TOPIC;
      break;
    case LogRef.LOG_WARNING:
      topic = LOG_WARNING_TOPIC;
      break;
    case LogRef.LOG_INFO:
      topic = LOG_INFO_TOPIC;
      break;
    case LogRef.LOG_DEBUG:
      topic = LOG_DEBUG_TOPIC;
      break;
    default:
      /* if an unknown event arrives, it should be logged as a LOG_OTHER event */
      topic = LOG_OTHER_TOPIC;
      break;
    }

    if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
      return;
    }


    Bundle bundle = logEntry.getBundle();
    Dictionary props = new Hashtable();

    /* Stores the properties of the event in the dictionary */
    if (bundle != null) {
      putProp(props, "bundle.id", new Long(bundle.getBundleId()));
      putProp(props, EventConstants.BUNDLE_SYMBOLICNAME, bundle.getLocation());
      putProp(props, "bundle", bundle);
    }
    if (logEntry != null) {
      putProp(props, "log.level", new Integer(logEntry.getLevel()));
      putProp(props, EventConstants.MESSAGE, logEntry.getMessage());
      putProp(props, EventConstants.TIMESTAMP, new Long(logEntry.getTime()));
      putProp(props, "log.entry", logEntry);
    }

    /* If the event contains an exception, further properties shall be set */
    if (logEntry.getException() != null) {
      Throwable e = logEntry.getException();
      putProp(props, EventConstants.EXECPTION_CLASS, Throwable.class.getName());
      putProp(props, EventConstants.EXCEPTION_MESSAGE, e.getMessage());
      putProp(props, EventConstants.EXCEPTION, e);
    }

    /* If the event contains a service reference, further properties shall be set */
    if (logEntry.getServiceReference() != null) {
      putProp(props, EventConstants.SERVICE, logEntry.getServiceReference());
      putProp(props, EventConstants.SERVICE_ID, logEntry.getServiceReference().getProperty(Constants.SERVICE_ID));
      putProp(props, EventConstants.SERVICE_OBJECTCLASS, logEntry.getServiceReference().getProperty(Constants.OBJECTCLASS));
      putProp(props, EventConstants.SERVICE_PID, logEntry.getServiceReference().getProperty(Constants.SERVICE_PID));
    }

    /* Tries posting the event once the properties are set */
    try {
      Activator.eventAdmin.postEvent(new Event(topic, props));
    } catch (Exception e) {
      log.error("EXCEPTION in logged(LogEntry logEntry):", e);
    }
  }

  private static final String SERVICE_EVENT_PREFIX = "org/osgi/framework/ServiceEvent/";
  private static final String SERVICE_EVENT_REGISTERED_TOPIC = SERVICE_EVENT_PREFIX + "REGISTERED";
  private static final String SERVICE_EVENT_MODIFIED_TOPIC = SERVICE_EVENT_PREFIX + "MODIFIED";
  private static final String SERVICE_EVENT_UNREGISTERING_TOPIC = SERVICE_EVENT_PREFIX + "UNREGISTERING";

  /**
   * A listener for service events
   * @param serviceEvent the event sent by the service
   * @author Johnny Baveras
   */
  public void serviceChanged(ServiceEvent serviceEvent) {
    String topic = null;
    boolean knownMessageType = true;

    switch (serviceEvent.getType()) {
    case ServiceEvent.REGISTERED:
      topic = SERVICE_EVENT_REGISTERED_TOPIC;

      
      String[] objectClass = (String[]) serviceEvent.getServiceReference().getProperty(Constants.OBJECTCLASS);
      boolean isLogReaderService = false;
      if (objectClass != null) {
        for (int i=0; i<objectClass.length; i++) {
          if (LogReaderService.class.getName().equals(objectClass[i])) {
            isLogReaderService = true;
          }
        }
      }

      if (isLogReaderService) {
        LogReaderService logReader = (LogReaderService)
          Activator.bundleContext.getService(serviceEvent.getServiceReference());
        if (logReader != null) {
          logReader.addLogListener(this);
        }
      }
      break;
    case ServiceEvent.MODIFIED:
      topic = SERVICE_EVENT_MODIFIED_TOPIC;
      break;
    case ServiceEvent.UNREGISTERING:
      topic = SERVICE_EVENT_UNREGISTERING_TOPIC;
      break;
    default:
      /* Setting the boolean to false if an unknown event arrives */
      knownMessageType = false;
      break;
    }



    /* Stores the properties of the event in the dictionary, if the event is known */
    if (knownMessageType) {
      if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
        return;
      }
      Dictionary props = new Hashtable();
      putProp(props, EventConstants.EVENT, serviceEvent);
      putProp(props, EventConstants.SERVICE, serviceEvent.getServiceReference());
      putProp(props, EventConstants.SERVICE_PID, serviceEvent.getServiceReference().getProperty(Constants.SERVICE_PID));
      putProp(props, EventConstants.SERVICE_ID, serviceEvent.getServiceReference().getProperty(Constants.SERVICE_ID));
      putProp(props, EventConstants.SERVICE_OBJECTCLASS, serviceEvent.getServiceReference().getProperty(Constants.OBJECTCLASS));

      /* Tries posting the event once the properties are set */
      try {
        Activator.eventAdmin.postEvent(new Event(topic, props));
      } catch (Exception e) {
        log.error("EXCEPTION in serviceChanged() :", e);
      }
    } else {
      /* Logs an error if the event, which arrived, were of an unknown type */
      log.error("Recieved unknown service event message (type="
                +serviceEvent.getType() +"), discarding");
    }
  }

  private static final String FW_EVT_PREFIX = "org/osgi/framework/FrameworkEvent/";
  private static final String FW_EVT_STARTED = FW_EVT_PREFIX + "STARTED";
  private static final String FW_EVT_ERROR = FW_EVT_PREFIX + "ERROR";
  private static final String FW_EVT_INFO = FW_EVT_PREFIX + "INFO";
  private static final String FW_EVT_PACKAGES_REFRESHED = FW_EVT_PREFIX + "PACKAGES_REFRESHED";
  private static final String FW_EVT_STARTLEVEL_CHANGED = FW_EVT_PREFIX + "STARTLEVEL_CHANGED";
  /**
   * A listener of framework events
   * @param frameworkEvent the event sent by the framework
   * @author Johnny Baveras
   */
  public void frameworkEvent(FrameworkEvent frameworkEvent) {
    String topic = null;
    boolean knownMessageType = true;
    
    switch (frameworkEvent.getType()) {
    case FrameworkEvent.STARTED:
      topic = FW_EVT_STARTED;
      break;
    case FrameworkEvent.ERROR:
      topic = FW_EVT_ERROR;
      break;
    case FrameworkEvent.INFO:
      topic = FW_EVT_INFO;
      break;
    case FrameworkEvent.PACKAGES_REFRESHED:
      topic = FW_EVT_PACKAGES_REFRESHED;
      break;
    case FrameworkEvent.STARTLEVEL_CHANGED:
      topic = FW_EVT_STARTLEVEL_CHANGED;
      break;
    default:
      /* Setting the boolean to false if an unknown event arrives */
      knownMessageType = false;
      break;
    }

    if (knownMessageType) {
      if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
        return;
      }
      Dictionary props = new Hashtable();
      Bundle bundle = frameworkEvent.getBundle();
      putProp(props, "event", frameworkEvent);
      /* If the event contains a bundle, further properties shall be set */
      if (frameworkEvent.getBundle() != null) {
        putProp(props, "bundle.id", new Long(bundle.getBundleId()));
        putProp(props,
                EventConstants.BUNDLE_SYMBOLICNAME,
                bundle.getLocation());
        putProp(props, "bundle", bundle);
      }

      /* If the event contains an exception, further properties shall be set */
      if (frameworkEvent.getThrowable() != null) {
        Throwable e = frameworkEvent.getThrowable();
        putProp(props,
                EventConstants.EXECPTION_CLASS,
                Throwable.class.getName());
        putProp(props, EventConstants.EXCEPTION_MESSAGE, e.getMessage());
        putProp(props, EventConstants.EXCEPTION, e);
      }

      /* Tries posting the event once the properties are set */
      try {
        Activator.eventAdmin.postEvent(new Event(topic, props));
      } catch (Exception e) {
        log.error("Exception in frameworkEvent() :", e);
      }
    } else {
      log.error("Recieved unknown framework event (type="
                +frameworkEvent.getType() +"), discarding");
    }
  }

  private void putProp(Dictionary props, Object key, Object value) {
    if (value != null) {
      props.put(key, value);
    }
  }
}
