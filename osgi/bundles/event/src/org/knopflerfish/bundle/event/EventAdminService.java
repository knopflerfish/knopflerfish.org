/*
 * Copyright (c) 2005-2013, KNOPFLERFISH project
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

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the EventAdmin interface. This is a singleton
 * class and should always be active.
 *
 * The implementation is responsible for track event handlers and check
 * their permissions. It will also host two threads sending different
 * types of data. If an event handler is subscribed to the published
 * event the EventAdmin service will put the event on one of the two
 * internal send-stacks depending on what type of deliverance the event
 * requires.
 *
 * @author Magnus Klack (refactoring by Bj\u00f6rn Andersson)
 */
public class EventAdminService
  implements EventAdmin
{
  final private Map<Object, QueueHandler> queueHandlers
    = new HashMap<Object, QueueHandler>();
  final private MultiListener ml;
  final private ConfigurationListenerImpl cli;
  private ServiceRegistration<EventAdmin> reg;

  public EventAdminService() {
    ml = new MultiListener();
    cli = new ConfigurationListenerImpl();
  }

  public void postEvent(Event event) {
    try {
      final InternalAdminEvent iae
        = new InternalAdminEvent(event, getMatchingHandlers(event.getTopic()));
      if (iae.getHandlers() == null) { // No-one to deliver to
        return;
      }

      QueueHandler queueHandler = null;
      boolean newQueueHandlerCreated = false;
      synchronized(queueHandlers) {
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof QueueHandler) {
          // Event posted by event handler, queue it on the queue that
          // called the event handler to keep the number of queue
          // handlers down.
          queueHandler = (QueueHandler) currentThread;
        } else {
          final Object key = Activator.useMultipleQueueHandlers
            ? (Object) currentThread : (Object) this;
          queueHandler = queueHandlers.get(key);
          if (null==queueHandler) {
            queueHandler = new QueueHandler(queueHandlers, key);
            queueHandler.start();
            queueHandlers.put(queueHandler.getKey(), queueHandler);
            newQueueHandlerCreated = true;
          }
        }
        queueHandler.addEvent(iae);
      }
      // Must not do logging from within synchronized code since that
      // may cause deadlock between the Log-service and the
      // EventAdmin-service; each new log entry is sent out as an
      // event via EventAdmin...
      if (newQueueHandlerCreated && Activator.log.doDebug()) {
        Activator.log.debug(queueHandler.getName() +" created.");
      }
    } catch(Exception e){
      Activator.log.error("Unknown exception in postEvent():", e);
    }
  }

  public void sendEvent(Event event) {
    try {
      final InternalAdminEvent iae
        = new InternalAdminEvent(event, getMatchingHandlers(event.getTopic()));
      if (iae.getHandlers() == null) { // No-one to deliver to
        return;
      }
      iae.deliver();
    } catch(Exception e){
      Activator.log.error("Unknown exception in sendEvent():", e);
    }
  }

  Set<TrackedEventHandler> getMatchingHandlers(String topic) {
    return Activator.handlerTracker.getHandlersMatching(topic);
  }

  synchronized void start() {
    ml.start();
    cli.start();
    reg = Activator.bc.registerService(EventAdmin.class, this, null);
  }

  synchronized void stop() {
    reg.unregister();;
    reg = null;

    cli.stop();
    ml.stop();

    Set<QueueHandler> activeQueueHandlers = null;
    synchronized(queueHandlers) {
      activeQueueHandlers = new HashSet<QueueHandler>(queueHandlers.values());
    }
    for (Iterator<QueueHandler> it = activeQueueHandlers.iterator(); it.hasNext(); ) {
      final QueueHandler queueHandler = it.next();
      queueHandler.stopIt();
    }
  }
}
