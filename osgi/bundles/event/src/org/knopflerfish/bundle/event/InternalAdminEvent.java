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

import java.security.AccessControlException;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.TopicPermission;

/**
 * A wrapper class for events. Connects an event with ServiceReferences to the
 * EventHandlers it should be delivered to.
 * 
 * @author Magnus Klack (refactoring by Bj\u00f6rn Andersson)
 */
public class InternalAdminEvent {

  private Event event;

  private Set handlers;
  private TimeoutDeliver timeoutDeliver;

  /**
   * Standard constructor of the InternalAdminEvent
   * 
   * @param event
   *          the event to be stored
   * @param handlers
   *          ServiceReference to the EventHandlers this event should be
   *          delivered to.
   */
  public InternalAdminEvent(Event event, Set handlers)
  {
    this.event = event;
    this.handlers = handlers;
  }

  /**
   * Returns the event
   * 
   * @return the event
   */
  protected Event getEvent()
  {
    return event;
  }

  public Set getHandlers()
  {
    return handlers;
  }

  public void deliver()
  {
    SecurityManager securityManager = getSecurityManager();

    // variable indicates if the handler is allowed to publish
    boolean canPublish = true;

    // variable indicates if handlers are granted access to topic
    boolean canSubscribe = true;

    // check if security is applied
    if (securityManager != null) {
      // check if there are any security limitation
      canPublish = checkPermission(event, securityManager,
          TopicPermission.PUBLISH);
      canSubscribe = checkPermission(event, securityManager,
          TopicPermission.SUBSCRIBE);
    }

    if (canPublish && canSubscribe) {
      deliverToHandles();
    } else if (canSubscribe) {
      // no publish permission
      Activator.log.error("No permission to publishto topic:"
          + event.getTopic());
    } else if (canPublish) {
      // no subscribe permission
      Activator.log.error("No permission to granted for subscription to topic:"
          + event.getTopic());
    } else {
      // no permissions at all are given
      Activator.log.error("No permission to publish and subscribe top topic:"
          + event.getTopic());
    }
  }

  private void log(TrackedEventHandler handler, String txt)
  {
    ServiceReference sr = handler.getServiceReference();
    Activator.log.error(txt + "  Service.id="
        + sr.getProperty(Constants.SERVICE_ID) + "  bundle.id="
        + sr.getBundle().getBundleId() + "  bundle.name="
        + sr.getBundle().getSymbolicName() + "  topic=" + event.getTopic(), sr);
  }

  private void deliverToHandles()
  {
    Iterator i = handlers.iterator();
    try {
      while (i.hasNext()) {
        TrackedEventHandler handler = (TrackedEventHandler) i.next();
        if (Activator.timeout == 0) {
          try {
            if (Activator.timeWarning == 0) {
              handler.handleEventSubjectToFilter(event);
            } else {
              final long tickStart = System.currentTimeMillis();
              handler.handleEventSubjectToFilter(event);
              final long tickEnd = System.currentTimeMillis();
              if (tickEnd - tickStart > Activator.timeWarning) {
                log(handler, "Slow eventhandler " + (tickEnd - tickStart)
                    + " ms.");
              }
            }
          } catch (Throwable e) {
            log(handler, "Exception in eventhandler " + e.getMessage());
            Activator.log.error("Handler threw exception in handleEvent.", e);
          }
        } else { // use timeout
          // Check if thread is available
          synchronized (this) {
            TimeoutDeliver localDeliver = deliver(this, event, handler);
            try {
              wait(Activator.timeout);
            } catch (InterruptedException e) {
              // Ignore
            }

            // Check if delivery was successful
            if (localDeliver.stopDeliveryNotification()) {
              handler.setBlacklist(true);
              log(handler,
                  "Event delivery to event handler timed out, blacklisting event handler.");
            }
          }
        }// end if(!isBlacklisted.....
      }// end for
    } finally {
      close();
    }
  }

  public synchronized TimeoutDeliver deliver(final Object caller,
                                             final Event event,
                                             final TrackedEventHandler handler)
  {
    if (timeoutDeliver == null) {
      timeoutDeliver = new TimeoutDeliver();
      timeoutDeliver.start();
    }

    if (timeoutDeliver.isActive()) {
      timeoutDeliver.close();
      timeoutDeliver = new TimeoutDeliver();
      timeoutDeliver.start();
    }

    timeoutDeliver.deliver(caller, event, handler);
    return timeoutDeliver;
  }

  public synchronized void close()
  {
    if (timeoutDeliver != null) {
      timeoutDeliver.close();
      timeoutDeliver = null;
    }
  }

  /**
   * checks the permission "permissionName" to this subject. OBS! this one will
   * only se if there are any permissions granted for all objects.
   * 
   * @param event
   *          the event
   * @param securityManager
   *          the system securitymanager
   * @param action
   *          The action: subscribe or publish
   * @return true if the object is permitted, false otherwise
   */
  private boolean checkPermission(Event event,
                                  SecurityManager securityManager,
                                  String action)
  {
    try {
      TopicPermission permission = new TopicPermission(event.getTopic(), action);
      securityManager.checkPermission(permission);
      return true;
    } catch (AccessControlException e) {
      return false;
    }
  }

  /**
   * returns the security manager
   * 
   * @return the security manager if any else null
   */
  private SecurityManager getSecurityManager()
  {
    // return the security manager
    return System.getSecurityManager();
  }

}
