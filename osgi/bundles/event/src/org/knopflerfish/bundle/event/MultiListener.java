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
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import org.knopflerfish.service.log.LogRef;

/**
 * Listen for LogEntries, ServiceEvents, FrameworkEvents, BundleEvents
 *
 * @author Magnus Klack, Martin Berg (refactoring by Bj\u00f6rn Andersson)
 */
public class MultiListener implements LogListener,
                                      ServiceListener,
                                      FrameworkListener,
                                      BundleListener {

  /**  hashtable of eventhandlers and timestamps */
  static Hashtable eventHandlers = new Hashtable();

  private LogRef log;
  private EventAdmin eventAdmin;
  private BundleContext bundleContext;

  public MultiListener(EventAdmin eventAdmin, BundleContext bundleContext) {
    this.eventAdmin = eventAdmin;
    this.bundleContext = bundleContext;
    log = new LogRef(bundleContext);

    bundleContext.addBundleListener(this);
    bundleContext.addServiceListener(this);
    bundleContext.addFrameworkListener(this);

    ServiceReference sr
      = bundleContext.getServiceReference(LogReaderService.class.getName());
    if (sr != null) {
      LogReaderService logReader = (LogReaderService) bundleContext.getService(sr);
      if (logReader != null) {
        logReader.addLogListener(this);
      }
    }
  }

  /**
   * A listener for events sent by bundles
   * @param bundleEvent The event sent by the bundle
   * @author Johnny Baveras
   */
  public void bundleChanged(BundleEvent bundleEvent) {
    //synchronized(this){
    /* A dictionary to store properties in */
    Dictionary props = new Hashtable();
    /* The bundle included in the bundleEvent */
    Bundle bundle = bundleEvent.getBundle();
    /* The prefix of the topic of the event to be posted*/
    String topic = "org/osgi/framework/BundleEvent/";
    /* boolean determining whether the bundleEvent is of a known topic */
    boolean knownMessageType = true;
    /* Determining the suffix of the topic of the event to be posted */
    switch (bundleEvent.getType()) {
    case BundleEvent.INSTALLED:
      topic += "INSTALLED";
      break;
    case BundleEvent.STARTED:
      topic += "STARTED";
      break;
    case BundleEvent.STOPPED:
      topic += "STOPPED";
      break;
    case BundleEvent.UPDATED:
      topic += "UPDATED";
      break;
    case BundleEvent.UNINSTALLED:
      topic += "UNINSTALLED";
      break;
    case BundleEvent.RESOLVED:
      topic += "RESOLVED";
      break;
    case BundleEvent.UNRESOLVED:
      topic += "UNRESOLVED";
      break;
    default:
      /* Setting the boolean to false if an unknown event arrives */
      knownMessageType = false;
      break;
    }

    if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
      return;
    }
    /* Stores the properties of the event in the dictionary, if the event is known */
    if (knownMessageType) {
      putProp(props, EventConstants.EVENT, bundleEvent);
      putProp(props, "bundle.id", new Long(bundle.getBundleId()));
      putProp(props, EventConstants.BUNDLE_SYMBOLICNAME, bundle.getLocation());//os?ker p? denna
      putProp(props, "bundle", bundle);
      /* Tries posting the event once the properties are set */
      try {
        eventAdmin.postEvent(new Event(topic, props));
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

  /**
   * A listener for entries in the log
   * @param logEntry the entry of the log
   * @author Johnny Baveras
   */
  public void logged(LogEntry logEntry) {
    /* A dictionary to store properties in */
    Dictionary props = new Hashtable();
    /* The bundle included in the bundleEvent */
    Bundle bundle = logEntry.getBundle();
    /* The prefix of the topic of the event to be posted*/
    String topic = "org/osgi/service/log/LogEntry/";
    /* Determining the suffix of the topic of the event to be posted */
    switch (logEntry.getLevel()) {
    case LogRef.LOG_ERROR:
      topic += "LOG_ERROR";
      break;
    case LogRef.LOG_WARNING:
      topic += "LOG_WARNING";
      break;
    case LogRef.LOG_INFO:
      topic += "LOG_INFO";
      break;
    case LogRef.LOG_DEBUG:
      topic += "LOG_DEBUG";
      break;
    default:
      /* if an unknown event arrives, it should be logged as a LOG_OTHER event */
      topic += "LOG_OTHER";
      break;
    }

    if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
      return;
    }

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
      eventAdmin.postEvent(new Event(topic, props));
    } catch (Exception e) {
      log.error("EXCEPTION in logged(LogEntry logEntry):", e);
    }
  }

  /**
   * A listener for service events
   * @param serviceEvent the event sent by the service
   * @author Johnny Baveras
   */
  public void serviceChanged(ServiceEvent serviceEvent) {
    /* A dictionary to store properties in */
    Dictionary props = new Hashtable();
    /* The prefix of the topic of the event to be posted*/
    String topic = "org/osgi/framework/ServiceEvent/";
    /* boolean determining whether the bundleEvent is of a known topic */
    boolean knownMessageType = true;
    /* Determining the suffix of the topic gof the event to be posted */
    switch (serviceEvent.getType()) {
    case ServiceEvent.REGISTERED:
      topic += "REGISTERED";
      /* Look at the service sending the event and determine whether its an eventHandler */
      String[] objectClass = (String[]) serviceEvent.getServiceReference().getProperty(Constants.OBJECTCLASS);
      boolean isEventHandler = false;
      boolean isLogReaderService = false;
      if (objectClass != null) {
        for (int i=0; i<objectClass.length; i++) {
          if (EventHandler.class.getName().equals(objectClass[i])) {
            isEventHandler = true;
          } else if (LogReaderService.class.getName().equals(objectClass[i])) {
            isLogReaderService = true;
          }
        }
      }
      if (isEventHandler) {
        /* Adds the service reference as a key in the hashtable of handlers, and the timestamp as value */
        eventHandlers.put(serviceEvent.getServiceReference(), new Long(System.currentTimeMillis()));
      }
      if (isLogReaderService) {
        LogReaderService logReader = (LogReaderService)
          bundleContext.getService(serviceEvent.getServiceReference());
        if (logReader != null) {
          logReader.addLogListener(this);
        }
      }
      break;
    case ServiceEvent.MODIFIED:
      topic += "MODIFIED";
      break;
    case ServiceEvent.UNREGISTERING:
      topic += "UNREGISTERING";
      /* Tries to removes the entry in the hashtable */
      try {
        eventHandlers.remove(serviceEvent.getServiceReference());
      } catch (NullPointerException e) {
        log.error("Tried to unregister a service which was not registered");
      }
      break;
    default:
      /* Setting the boolean to false if an unknown event arrives */
      knownMessageType = false;
      break;
    }

    if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
      return;
    }

    /* Stores the properties of the event in the dictionary, if the event is known */
    if (knownMessageType) {
      putProp(props, EventConstants.EVENT, serviceEvent);
      putProp(props, EventConstants.SERVICE, serviceEvent.getServiceReference());
      putProp(props, EventConstants.SERVICE_PID, serviceEvent.getServiceReference().getProperty(Constants.SERVICE_PID));
      putProp(props, EventConstants.SERVICE_ID, serviceEvent.getServiceReference().getProperty(Constants.SERVICE_ID));
      putProp(props, EventConstants.SERVICE_OBJECTCLASS, serviceEvent.getServiceReference().getProperty(Constants.OBJECTCLASS));

      /* Tries posting the event once the properties are set */
      try {
        eventAdmin.postEvent(new Event(topic, props));
      } catch (Exception e) {
        log.error("EXCEPTION in serviceChanged() :", e);
      }
    } else {
      /* Logs an error if the event, which arrived, were of an unknown type */
      log.error("Recieved unknown service event message (type="
                +serviceEvent.getType() +"), discarding");
    }
  }

  /**
   * A listener of framework events
   * @param frameworkEvent the event sent by the framework
   * @author Johnny Baveras
   */
  public void frameworkEvent(FrameworkEvent frameworkEvent) {
    /* A dictionary to store properties in */
    Dictionary props = new Hashtable();
    /* The bundle included in the bundleEvent */
    Bundle bundle = frameworkEvent.getBundle();
    /* The prefix of the topic of the event to be posted*/
    String topic = "org/osgi/framework/FrameworkEvent/";
    /* boolean determining whether the bundleEvent is of a known topic */
    boolean knownMessageType = true;
    switch (frameworkEvent.getType()) {
    case FrameworkEvent.STARTED:
      topic += "STARTED";
      break;
    case FrameworkEvent.ERROR:
      topic += "ERROR";
      break;
    case FrameworkEvent.INFO:
      topic += "INFO";
      break;
    case FrameworkEvent.PACKAGES_REFRESHED:
      topic += "PACKAGES_REFRESHED";
      break;
    case FrameworkEvent.STARTLEVEL_CHANGED:
      topic += "STARTLEVEL_CHANGED";
      break;
    default:
      /* Setting the boolean to false if an unknown event arrives */
      knownMessageType = false;
      break;
    }

    if(!Activator.handlerTracker.anyHandlersMatching(topic)) {
      return;
    }

    /* Stores the properties of the event in the dictionary, if the
     * event is known */
    if (knownMessageType) {
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
        eventAdmin.postEvent(new Event(topic, props));
      } catch (Exception e) {
        log.error("Exception in frameworkEvent() :", e);
      }
    } else {
      log.error("Recieved unknown framework event (type="
                +frameworkEvent.getType() +"), discarding");
    }
  }

  private void putProp(Dictionary props, Object key, Object value) {
    //try {
    if (value != null) {
      props.put(key, value);
    }// catch (NullPointerException ignore) {}
  }

}
