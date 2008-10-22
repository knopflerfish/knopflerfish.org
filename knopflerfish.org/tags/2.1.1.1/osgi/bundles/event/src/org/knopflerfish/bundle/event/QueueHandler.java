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

import java.util.LinkedList;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * this class will send the events synchronus and asynchronus to the event handlers it contains
 * one internal class doing a producer consumer algorithm between itself and the mainclass.
 * the internal worker class will pick the first element in the queue and create a deliver session.
 *
 * @author Magnus Klack (refactoring by Björn Andersson)
 */
public class QueueHandler extends Thread {

  private LinkedList syncQueue = new LinkedList();
  private boolean running;

  /**
   * This adds a new InternalAdminEvent to the que
   *
   * @param event the new InternalAdminEvent
   */
  public void addEvent(InternalAdminEvent event) {
    if (event.getReferences() == null) {
      // Noone to deliver to
      return;
    }
    synchronized (this) {
      syncQueue.add(event);
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
        event.deliver(); // Must be outside synchronized since the delivery can cause new events.
      } else {
        synchronized (this) {
          try {
            wait();
          } catch (InterruptedException e) {
            Activator.log.error("QueueHandler was interrupted unexpectedly");
          }
        }
      }
    }//end while...
  }// end run()

  /**
   * Stop this thread.
   */
  synchronized void stopIt() {
    running = false;
    notifyAll();
    try {
      join();
    } catch (InterruptedException e) {
      // Ignore
    }
  }

}// end class QueueHandler
