/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

import java.util.Calendar;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import org.knopflerfish.service.log.LogRef;

/**
 * Default implementation of the EventAdmin interface this is a singleton class
 * and should always be active. The implementation is responsible for track
 * eventhandlers and check their permissions. It will also
 * host two threads sending diffrent types of data. If an eventhandler is subscribed to the
 * published event the EventAdmin service will put the event on one of the two internal sendstacks
 * depending on what type of deliverance the event requires.
 *
 * @author Magnus Klack (refactoring by Björn Andersson)
 */
public class EventAdminService implements EventAdmin {

  /** the local representation of the bundle context */
  private BundleContext bundleContext;

  /** variable holding the synchronus send procedure */
  private QueueHandler queueHandlerSynch;

  /** variable holding the asynchronus send procedure */
  private QueueHandler queueHandlerAsynch;

  private Object semaphore = new Object();

  /**
   * the constructor use this to create a new Event admin service.
   *
   * @param context the BundleContext
   */
  public EventAdminService(BundleContext context) {
    synchronized(this){
      /* assign the context to the local variable */
      bundleContext = context;

      /* create the asynchronus queue handler */
      queueHandlerAsynch = new QueueHandler();
      /* start the handler */
      queueHandlerAsynch.start();

      new MultiListener(this, context);
      new ConfigurationListenerImpl(this, context);
    }
  }

  /**
   * This method should be used when an asynchronus events are to be
   * published.
   *
   * @param event the event to publish
   */
  public void postEvent(Event event) {
    try {
      queueHandlerAsynch.addEvent(new InternalAdminEvent(event, getReferences()));
    } catch(Exception e){
      Activator.log.error("Unknown exception in postEvent():", e);
    }
  }

  /**
   * This method should be used when synchronous events are to be published
   *
   * @param event the event to publish
   * @author Magnus Klack
   */
  public void sendEvent(Event event) {
    try {
      new InternalAdminEvent(event, getReferences()).deliver();
    } catch(Exception e){
      Activator.log.error("Unknown exception in sendEvent():", e);
    }
  }

  /**
   * returns the servicereferences
   *
   * @return ServiceReferences[] array if any else null
   * @throws InvalidSyntaxException if syntax error
   */
  ServiceReference[] getReferences() {
    try {
      ServiceReference[] refs = bundleContext.getServiceReferences(EventHandler.class.getName(), null);
      return refs;
    } catch (InvalidSyntaxException ignore) {
      // What? We're not even using a filter!
      return null;
    } catch (IllegalStateException e) {
      // The bundleContext is invalid. We're probably being stopped.
      return null;
    }
  }

  void stop() {
    //queueHandlerSynch.stopIt();
    queueHandlerAsynch.stopIt();
  }

}
