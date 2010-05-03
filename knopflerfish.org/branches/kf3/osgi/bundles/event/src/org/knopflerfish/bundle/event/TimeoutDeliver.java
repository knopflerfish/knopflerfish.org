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

/**
 * This class will try to update the EventHandler if it succeed an interrupt
 * will be performed on the 'owner' class.
 * 
 * @author Magnus Klack, Johnny Baveras
 * 
 */
public class TimeoutDeliver extends Thread {
  /** The object to notify when done */
  private Object caller;

  /** The service reference of the handler to call. */
  private TrackedEventHandler handler;
  private Event event;

  private boolean delivered = true;
  private boolean timedOut = false;
  private boolean closed = false;

  public synchronized boolean isDelivered()
  {
    return delivered;
  }

  private synchronized void setDelivered(boolean delivered)
  {
    this.delivered = delivered;
  }

  private synchronized boolean isTimedOut()
  {
    return timedOut;
  }

  /**
   * Called to indicate that the caller does not wait for the completion of
   * current deliver job, i.e. the caller will not be notified if delivery is
   * finished after this call.
   * 
   * @return true if delivery job not finished yet
   */
  public synchronized boolean stopDeliveryNotification()
  {
    timedOut = true;
    return !delivered;
  }

  public synchronized void close()
  {
    closed = true;
    notifyAll();
  }

  public synchronized boolean isActive()
  {
    return !delivered;
  }

  public synchronized void deliver(final Object caller,
                                   final Event event,
                                   final TrackedEventHandler handler)
  {
    if (isActive()) {
      throw new IllegalStateException("Delivery already in progress");
    }

    timedOut = false;
    delivered = false;
    this.caller = caller;
    this.event = event;
    this.handler = handler;
    notifyAll();
  }

  /**
   * Inherited from Thread, starts the thread.
   */
  public void run()
  {
    while (!closed) {
      if (!isDelivered()) {
        synchronized (this) {
          // Synchronized to ensure that this thread sees the current values
          // of the instance fields
          handler.isBlacklisted();
        }
        try {
          handler.handleEventSubjectToFilter(event);
        } catch (Throwable e) {
          Activator.log
              .error("Handler threw exception in handleEvent: " + e, e);
        } finally {
          synchronized (caller) {
            setDelivered(true);
          }
        }

        /* tell the owner that notification is done */
        if (!isTimedOut()) {
          synchronized (caller) {
            caller.notifyAll();
          }
        }
      } else {
        synchronized (this) {
          try {
            wait();
          } catch (InterruptedException e) {
            // Ignore
          }
        }
      }
    }
  }
}
