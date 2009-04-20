/*
 * Copyright (c) 2003-2005, KNOPFLERFISH project
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

package org.knopflerfish.service.um.useradmin.impl;

import java.util.Vector;
import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;


/**
 * This class is responsible for dispatching user admin event jobs.
 * The dispatch thread will only live as long as there is some work
 * for it to do.
 *
 * @author Gunnar Ekolin
 * @version 1.0
 */

final class EventQueue implements Runnable {

  /**
   * The thread running this object.
   */
  private Thread thread;
  

  private final Object threadLock = new Object();

  private LogRef log;
  
  /**
   * The queue of events to dispatch.
   */
  private Vector queue = new Vector();


  /**
   * Construct an EventQueue.
   * 
   */
  EventQueue( BundleContext bc ) {
    this.log = new LogRef(bc);
  }


  /**
   * Overide of Thread.run().
   */
  public void run() {
    while (true) {
      Runnable job = dequeue();
      if (job == null) {
        return;
      }
      try {
        job.run();
      } catch (Throwable t) {
        log.error("Error while executing event dispatch job ", t);
      }
    }
  }

  /**
   * Add an entry to the end of the queue.
   * 
   * @param job The Runnable to add to the queue.
   * @throws java.lang.IllegalArgumentException
   *             If given a null argument.
   */
  public synchronized void enqueue(Runnable job) {
    if (job == null) {
      throw new IllegalArgumentException("null job");
    }
    queue.addElement(job);
    attachNewThreadIfNeccesary();
    notifyAll();
  }

  /**
   * Get and remove the next entry from the queue.
   * If the queue is empty this method waits until an entry is available. 
   * 
   * @return The Hashtable entry removed from the queue.
   */
  private synchronized Runnable dequeue() {
    if (queue.isEmpty()) {
      try {
        wait(5000);
      } catch (InterruptedException ignored) {
      }
    }
    if (queue.isEmpty()) {
      detachCurrentThread();
      return null;
    }
    Runnable job = (Runnable) queue.elementAt(0);
    queue.removeElementAt(0);
    return job;
  }


  void attachNewThreadIfNeccesary() {
    synchronized (threadLock) {
      if (thread == null) {
        thread = new Thread(this,"UserAdminEventDispatchThread");
        thread.setPriority(Thread.NORM_PRIORITY);
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
}
