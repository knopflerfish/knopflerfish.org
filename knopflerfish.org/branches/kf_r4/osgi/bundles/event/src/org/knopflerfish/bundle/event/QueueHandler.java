/*
 * @(#)EventAdminService.java        1.0 2005/06/28
 *
 * Copyright (c) 2003-2005 Gatespace telematics AB
 * Otterhallegatan 2, 41670,Gothenburgh, Sweden.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Gatespace telematics AB. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Gatespace telematics AB.
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
  /** the queue */
  private LinkedList syncQueue = new LinkedList();

  /** variable indicating that this is running */
  private boolean running;

  /**
   * Constructor for the QueueHandler
   */
  public QueueHandler() {
    running = true;
  }

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
    synchronized(this) {
      syncQueue.add(event);
      notifyAll();
    }
  }

  /**
   * Inherited from Thread, starts the thread.
   */
  public void run() {
    while (running) {
      synchronized (this) {
        if (!syncQueue.isEmpty()) {
          ((InternalAdminEvent) syncQueue.removeFirst()).deliver();
        } else {
          try {
            wait();
          } catch (InterruptedException e) {
            Activator.log.error("Worker was interrupted unexpected");
          }
        }
      }// end synchronized
    }//end while...
  }// end run()

  /**
   * Stop this thread.
   */
  synchronized void stopIt() {
    running = false;
    notifyAll();
  }

}// end class QueueHandler
