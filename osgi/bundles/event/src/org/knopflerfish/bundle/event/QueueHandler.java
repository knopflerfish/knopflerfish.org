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

import java.security.AccessControlException;
import java.util.LinkedList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.TopicPermission;

import org.knopflerfish.service.log.LogRef;

/**
 * this class will send the events synchronus and asynchronus to the event handlers it contains
 * one internal class doing a producer consumer algorithm between itself and the mainclass.
 * the internal worker class will pick the first element in the queue and create a deliver session.
 *
 * @author Magnus Klack
 */
public class QueueHandler extends Thread {
  /** constant for synchronous handlers */
  public final static int SYNCHRONUS_HANDLER=0;

  /** constant for asynchronous handlers */
  public final static int ASYNCHRONUS_HANDLER=1;

  /** the queue */
  private LinkedList syncQueue = new LinkedList();

  /** variable indicating that this is running */
  private boolean running;

  /** private variable holding the queue type */
  private int queueType;

  /** the session counter */
  private long sessionCounter=0;

  private EventAdminService eventAdmin;
  private BundleContext bundleContext;
  private LogRef log;

  /**
   * Constructor for the QueueHandler
   */
  public QueueHandler(EventAdminService eventAdmin, BundleContext bundleContext, int type) {
    this.eventAdmin = eventAdmin;
    this.bundleContext = bundleContext;
    log = new LogRef(bundleContext);
    running = true;
    queueType = type;
  }

  /**
   * This adds a new InternalAdminEvent to the que
   *
   * @param event the new InternalAdminEvent
   */
  public void addEvent(Object event) {
    synchronized(this) {
      syncQueue.add(event);
      notifyAll();
    }
  }

  /**
   * Inherited from Thread, starts the thread.
   */
  public void run() {
    // as long as the thread is running
    while (running) {

      if (!syncQueue.isEmpty()) {
        synchronized (this) {
          // get the service references do this now to ensure
          // that only handlers registered now will recive the event

          // method variable holding all service references
          ServiceReference[] serviceReferences = eventAdmin.getReferences();
          if(serviceReferences!=null){
            // get the 'Event' not the InternalAdminEvent
            // and remove it from the list
            InternalAdminEvent event = (InternalAdminEvent) syncQueue.removeFirst();

            SecurityManager securityManager = getSecurityManager();

            // variable indicates if the handler is allowed to publish
            boolean canPublish;

            // variable indicates if handlers are granted access to topic
            boolean canSubscribe;

            // variable indicates if the topic is well formatted
            boolean isWellFormatted;

            // check if security is applied
            if (securityManager != null) {
              // check if there are any security limitation
              canPublish = checkPermissionToPublish((Event) event.getElement(),
                                                    securityManager);
              canSubscribe = checkPermissionToSubscribe((Event) event.getElement(),
                                                        securityManager);
            } else {
              // no security here
              canPublish = true;
              canSubscribe = true;
            }

            // get if the topic is wellformatted
            isWellFormatted = topicIsWellFormatted( ((Event)event.getElement()).getTopic());

            if (canPublish && canSubscribe && isWellFormatted) {
              // create an instance of the deliver session to deliver events
              DeliverSession deliverSession;
              String sessionName;
              if (queueType==ASYNCHRONUS_HANDLER) {
                sessionName = "ASYNCRONOUS_SESSION:" + sessionCounter;
              } else {
                sessionName = "SYNCRONOUS_SESSION:" + sessionCounter;
              }
              deliverSession = new DeliverSession(event, bundleContext,
                                                  serviceReferences, log,
                                                  this,sessionName);
              sessionCounter++;

              // start deliver events
              deliverSession.start();

              try {
                // wait for notification
                wait();
              } catch (InterruptedException ignore) {
              } catch(Exception e){
                log.error("Exception in SynchDeliverThread:", e);
              }

            // this will happen if an error occures in getReferences():
            } else if (canSubscribe) {
              // no publish permission
              log.error("No permission to publishto topic:"
                        + ((Event) event.getElement()).getTopic());
            } else if (canPublish) {
              // no subscribe permission
              log.error("No permission to granted for subscription to topic:"
                        + ((Event) event.getElement()).getTopic());
            } else {
              // no permissions at all are given
              log.error("No permission to publish and subscribe top topic:"
                        + ((Event) event.getElement()).getTopic());
            }
          } else { // (serviceReferences == null)
            syncQueue.clear();
          }

        }// end synchronized
      } else {
        try {
          synchronized (this) {
            wait();
          }
        } catch (InterruptedException e) {
          log.error("Worker was interrupted unexpected");
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
  }

  /**
   * this function checks for invalid topics
   * like null topics and "" topics.
   *
   * @param topic the topic string
   * @return true if well formatted else false if null or "" formatted
   */
  private boolean topicIsWellFormatted(String topic) {
    if(topic!=null){
      // this is the "*" topic
      if(topic.length()==1 && topic.equals("*")){
        return true;
      }

      // this is the "" topic
      if(topic.length()==0){
        return false;
      }

      // this is a topic with length >1
      if(topic.length()> 1){
        return true;
      }
    }
    // this is the null topic
    return false;
  }

  /**
   * checks the permission to subscribe to this subject. OBS! this one will
   * only se if there are any permissions granted for all objects to
   * subscribe.
   *
   * @param event the subscription event
   * @param securityManager the system securitymanager
   * @return true if the object is permitted, false otherwise
   */
  private boolean checkPermissionToSubscribe(Event event,
                                             SecurityManager securityManager) {
    try {
      // create a topic
      TopicPermission subscribePermission = new TopicPermission(event
          .getTopic(), "subscribe");
      // check the permission
      securityManager.checkPermission(subscribePermission);
      // return true
      return true;
    } catch (AccessControlException e) {
      // return false
      return false;
    }

  }

  /**
   * This function checks if the publisher is permitted to publish the event.
   *
   * @param event the event to be published
   * @param securityManager the system security manager
   * @return true if the publisher can publish the subject, false otherwise
   */
  private boolean checkPermissionToPublish(Event event,
                                           SecurityManager securityManager) {
    try {
      // create a topic
      TopicPermission publishPermission = new TopicPermission(event
          .getTopic(), "publish");
      // check the permission
      securityManager.checkPermission(publishPermission);
      // return true
      return true;

    } catch (AccessControlException e) {
      // return false
      return false;
    }
  }

  /**
   * returns the security manager
   *
   * @return the security manager if any else null
   */
  private SecurityManager getSecurityManager() {
    // return the security manager
    return System.getSecurityManager();
  }

}// end class QueueHandler
