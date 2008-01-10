/*
 * Copyright (c) 2003-2006, KNOPFLERFISH project
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

package org.knopflerfish.bundle.cm;

import java.util.Vector;

import org.osgi.framework.BundleContext;

/**
 * @author js
 * @author Philippe Laporte
 */
final public class ListenerEventQueue implements Runnable {

  /**
   ** The thread running this object.
   **/
  private Thread thread;

  private final Object threadLock = new Object();

  /**
   ** The queue of events.
   **/
  private Vector queue = new Vector();

  /**
   * The bundle context
   */
  private BundleContext bc;

  /**
   * The queue has been stopped.
   */
  private boolean quit = false;

  /**
   ** Construct an UpdateQueue given a  
   ** BundleContext.
   **
   ** @param bc The BundleContext to use.
   **/
  ListenerEventQueue(BundleContext bc) {
    this.bc = bc;
  }

  /**
   ** Overide of Thread.run().
   **/
  public void run() {
    while (true) {
      ListenerEvent update = dequeue();
      if (update == null) {
        return;
      } 
      else {
        try {
          update.sendEvent(bc);
					
        } catch (Throwable t) {
          Activator.log.error("[CM] Error while sending event", t);
        }
      }
    }
  }

  /**
   ** Add an entry to the end of the queue.
   **
   ** @param update The Update to add to the queue.
   **
   **/
  public synchronized void enqueue(ListenerEvent update) {
    if (update == null || quit) {
      return;
    }	
    queue.addElement(update);
    attachNewThreadIfNeccesary();
    notifyAll();
  }

  /**
   ** Get and remove the next entry from the queue.
   ** 
   ** If the queue is empty this method waits until an
   ** entry is available.
   **
   ** @return The Hashtable entry removed from the queue.
   **/
  private synchronized ListenerEvent dequeue() {
    if (!quit && queue.isEmpty()) {
      try {
        wait(5000);
      } catch (InterruptedException ignored) {
      }
    }
    if (queue.isEmpty()) {
      detachCurrentThread();
      return null;
    } else {
      ListenerEvent u = (ListenerEvent) queue.elementAt(0);
      queue.removeElementAt(0);
      return u;
    }
  }

  void attachNewThreadIfNeccesary() {
    synchronized (threadLock) {
      if (thread == null && !quit) {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
      }
    }
  }

  void detachCurrentThread() {
    synchronized (threadLock) {
      thread = null;
    }
  }

  synchronized void stop() {
    quit = true;
    notifyAll();
    synchronized (threadLock) {
      if (thread != null) {
        try {
          thread.join(3000);
        } catch (InterruptedException _ignore) { }
      }
    }
  }
}
