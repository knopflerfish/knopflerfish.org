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

import org.osgi.service.event.Event;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Class which handles the event deliveries to event handlers.
 *
 * @author Magnus Klack, Martin Berg (refactoring by Bj\u00f6rn Andersson)
 */
public class DeliverSession {

  private static final String TIMEOUT_PROP
    = "org.knopflerfish.eventadmin.timeout";

  /** local event variable */
  private Event event;

  /** internal admin event */
  private InternalAdminEvent internalEvent;

  /** local array of service references */
  private Set handlers;

  /** the wildcard char */
  private final static String WILD_CARD = "*";

  /** The timeout variable. Default: no timeout */
  private long timeout = 0;

  /** the references to the blacklisted handlers */
  private static HashSet blacklisted = new HashSet();

  /**
   * Standard constructor for DeliverSession.
   *
   * @param evt the event to be delivered
   */
  public DeliverSession(InternalAdminEvent evt) {
    internalEvent = evt;
    event = internalEvent.getEvent();
    handlers = internalEvent.getHandlers();

    /* Tries to get the timeout property from the system*/
    try {
      String timeoutS = Activator.bundleContext.getProperty(TIMEOUT_PROP);
      if (null!=timeoutS && 0<timeoutS.length()) {
        timeout = Long.parseLong(timeoutS);
      }
    } catch (NumberFormatException ignore) {}
  }

  /**
   *  Initiates the delivery.
   */
  public void deliver() {
    if (handlers == null || handlers.isEmpty()) {
      return;
    }

    Iterator i = handlers.iterator();
    while(i.hasNext()) {
      TrackedEventHandler handler = (TrackedEventHandler)i.next();

      if (timeout == 0) {
        try {
          handler.handleEventSubjectToFilter(event);
        } catch (Throwable e) {
          Activator.log.error("Handler threw exception in handleEvent", e);
        }
      } else { // use timeout
        try {
          synchronized (this) {
            final TimeoutDeliver timeoutDeliver
              = new TimeoutDeliver(Thread.currentThread(), handler);
            timeoutDeliver.start();
            wait(timeout);
            /*
            Activator.log.error
              ("Event delivery to event handler with service.id "
               +sid +" timed out: "+ timeoutDeliver.getName(),
               handlerSR);
            */
            /* check if already blacklisted by another thread */
            handler.blacklist();
            /*
            if (!blacklisted.contains(handlerSR)) {
              blacklisted.add(handlerSR);
              if (Activator.log.doDebug()) {
                Activator.log.debug("The event handler with service id "
                                    +sid +" was blacklisted due to timeout",
                                    handlerSR);
              }
            }
            */
          }
        } catch (InterruptedException e) {
          /* this will happen if a deliverance succeeded */
        }
      }//end if(!isBlacklisted.....
    }//end for
  } 




  /**
   * This class will try to update the EventHandler if it succeed an interrupt
   * will be performed on the 'owner' class.
   *
   * @author Magnus Klack, Johnny Baveras
   *
   */
  private class TimeoutDeliver extends Thread {
    /** The thread to interrupt when done */
    private final Thread caller;

    /** The service reference of the handler to call. */
    private final TrackedEventHandler handler;

    /**
     * Constructor of the TimeoutDeliver object
     *
     * @param caller  the thread to interrupt when done.
     * @param handler the event handler to be updated.
     */
    public TimeoutDeliver(final Thread caller,
                          final TrackedEventHandler handler)
    {
      this.caller = caller;
      this.handler = handler;
    }

    /**
       Inherited from Thread, starts the thread.
    */
    public void run() {
      if (Activator.log.doDebug()) Activator.log.debug("TimeOutDeliver.run()");

      try {
        handler.handleEventSubjectToFilter(event);
      } catch (Throwable e) {
        Activator.log.error("Handler threw exception in handleEvent: "+e, e);
      }

      /* tell the owner that notification is done */
      caller.interrupt();
    }
  }
}
