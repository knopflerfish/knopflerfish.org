/**
 ** Copyright (c) 2001 Gatespace AB. All Rights Reserved.
 **/

package org.knopflerfish.bundle.cm;

import org.knopflerfish.service.log.*;

import org.osgi.service.cm.*;
import org.osgi.framework.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

/**
 ** This class is responsible for dispatching configurations
 ** to ManagedService(Factories).
 **
 ** It is also responsible for calling <code>ConfigurationPlugins</code>.
 **
 ** @author Per Gustafson
 ** @version 1.0
 **/

final class UpdateQueue implements Runnable {
  /**
   ** The PluginManager to use.
   **/

  private PluginManager pm;

  /**
   ** The thread running this object.
   **/

  private Thread thread;

  /**
   ** The thread running this object.
   **/

  private final Object threadLock = new Object();

  /**
   ** The queue of updates.
   **/

  private Vector queue = new Vector();

  /**
   ** Construct an UpdateQueue given a  
   ** BundleContext.
   **
   ** @param tracker The BundleContext to use.
   **/

  UpdateQueue(PluginManager pm) {
    this.pm = pm;
  }

  /**
   ** Overide of Thread.run().
   **/

  public void run() {
    while(true) {
      if(doUpdateQueueLogging()) {
        Activator.log.debug("[UpdateQueue] Getting next Update from queue");
      }
      Update update = dequeue();
      if(update == null) {
        if(doUpdateQueueLogging()) {
          Activator.log.debug("[UpdateQueue] Got null Update from queue");
        }
        return;
      } else {
        if(doUpdateQueueLogging()) {
          Activator.log.debug("[UpdateQueue] Got an Update from queue");
        }
        try {
          if(doUpdateQueueLogging()) {
            Activator.log.debug("[UpdateQueue] Calling Update.doUpdate");
          }
          update.doUpdate(pm);
          if(doUpdateQueueLogging()) {
            Activator.log.debug("[UpdateQueue] Update.doUpdate returned");
          }
        } catch(ConfigurationException ce) {
          Activator.log.error("[CM] Error in configuration for " + update.pid, ce);
        } catch(Throwable t) {
          Activator.log.error("[CM] Error while updating " + update.pid, t);
        }
      }
    }
  }

  /**
   ** Add an entry to the end of the queue.
   **
   ** @param update The Update to add to the queue.
   **
   ** @throws java.lang.Exception If given a null argument.
   **/

  public synchronized void enqueue(Update update) {
    if(update == null) {
      throw new IllegalArgumentException("ConfigurationDispatcher.enqueue(Update) needs a non-null argument.");
    }
    if(doUpdateQueueLogging()) {
      Activator.log.debug("[UpdateQueue] Adding update for " + update.pid + " to queue");
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

  private synchronized Update dequeue() {
    if(queue.isEmpty()) {
      try {
        if(doUpdateQueueLogging()) {
          Activator.log.debug("[UpdateQueue] Queue is empty. Waiting 5000 ms");
        }
	      wait(5000);
      } catch(InterruptedException ignored) {}
    } 
    if(queue.isEmpty()) {
      if(doUpdateQueueLogging()) {
        Activator.log.debug("[UpdateQueue] Queue is still empty. Detaching thread.");
      }
      detachCurrentThread();
      return null;
    } else {
      Update u = (Update)queue.elementAt(0);
      queue.removeElementAt(0);
      return u;
    }
  }

  void attachNewThreadIfNeccesary() {
    synchronized(threadLock) {
      if(thread == null) {
        if(doUpdateQueueLogging()) {
          Activator.log.debug("[UpdateQueue] Attaching new thread.");
        }
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
      }
    }
  }

  void detachCurrentThread() {
    synchronized(threadLock) {
      if(doUpdateQueueLogging()) {
        Activator.log.debug("[UpdateQueue] Detaching thread because queue is empty.");
      }
      thread = null;
    }
  }

  boolean doUpdateQueueLogging() {
    //return Activator.log.doDebug()
    return false;
  }
}
