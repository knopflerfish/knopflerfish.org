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

import java.util.LinkedList;
import java.util.Map;

/**
 * This class will queue the events and deliver them to the event
 * handlers in the apropriate order.
 *
 * @author Magnus Klack (refactoring by Bj\u00f6rn Andersson et.all.)
 */
public class QueueHandler extends Thread {

  /**
   * The mapping from key to active QueueHandler instance owned by the
   * EventAdmin service implementation.
   */
  final private Map queueHandlers;
  /** The key for this queue handler in the map with active queue handlers.*/
  private Object key;

  /** The queue with events to be delivered by this thread.*/
  private LinkedList syncQueue = new LinkedList();

  /** The state of this queue handler thread.*/
  private boolean running;

  private int maxQueueSize = 0;
  private int eventCnt = 0;

  /**
   * Creates a QueueHandler-thread with the specified key.
   *
   * @param key The key for this queue handler.
   */
  public QueueHandler(Map queueHandlers, Object key)
  {
    super("EventAdmin-QueueHandler "+key);

    this.queueHandlers = queueHandlers;
    this.key= key;

    if (Activator.log.doDebug()) {
      Activator.log.debug(getName() +" created.");
    }
  }

  public Object getKey()
  {
    return key;
  }

  /**
   * This adds a new InternalAdminEvent to the que
   *
   * @param event the new InternalAdminEvent
   */
  public void addEvent(InternalAdminEvent event) {
    if (event.getHandlers() == null) {
      // Noone to deliver to
      return;
    }
    synchronized (this) {
      syncQueue.add(event);
      final int queueSize = syncQueue.size();
      if (queueSize>maxQueueSize) maxQueueSize = queueSize;
      notifyAll();
    }
  }

  /**
   * Inherited from Thread, starts the thread.
   */
  public void run() {
    running = true;
    while (running) {
      InternalAdminEvent event = null;
      synchronized (this) {
        if (!syncQueue.isEmpty()) {
          event = ((InternalAdminEvent) syncQueue.removeFirst());
        }
      }
      if (event != null) {
        // Must be outside synchronized since the delivery can cause
        // new events.
        event.deliver();
        eventCnt++;
      } else {
        long duration = 0;
        synchronized (this) {
          try {
            final long start = System.currentTimeMillis();
            wait(Activator.queueHandlerTimeout);
            final long end = System.currentTimeMillis();
            duration = end - start;
          } catch (InterruptedException e) {
            Activator.log.error("QueueHandler was interrupted unexpectedly");
          }
        }
        if (0<Activator.queueHandlerTimeout
            && duration > Activator.queueHandlerTimeout) {
          // Must allways lock on queueHandlers before this when both
          // are needed.
          synchronized(queueHandlers) {
            synchronized (this) {
              if (syncQueue.isEmpty()) {
                running = false;
                queueHandlers.remove(getKey());
                if (Activator.log.doDebug()) {
                  Activator.log.debug(getName() +" time out.");
                }
              }
            }
          }
        }
      }
    }//end while...
    if (Activator.log.doDebug()) {
      final String msg = getName() +" terminated (max queue size "
        +maxQueueSize+"), total number of events " +eventCnt +".";
      Activator.log.debug(msg);
    }
    synchronized(queueHandlers) {
      queueHandlers.remove(getKey());
    }
  }// end run()

  /**
   * Stop this thread.
   */
  void stopIt() {
    synchronized (this) {
      running = false;
      notifyAll();
    }
    try {
      join();
    } catch (InterruptedException e) {
      // Ignore
    }
  }

}// end class QueueHandler
