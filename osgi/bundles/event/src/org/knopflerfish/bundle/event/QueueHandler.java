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

  /** thread worker **/
  private Thread workerThread;

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
    queueType=type;
  }

  /**
   * This adds a new InternalAdminEvent to the que
   *
   * @param event the new InternalAdminEvent
   */
  public void addEvent(Object event) {
    /* lock the synchQueue */
//      System.out.println("Adding new event");
    syncQueue.add(event);
    if (workerThread != null) {
      /* lock the worker object */
      synchronized (workerThread) {
//          System.out.println("notifiying worker thread");
        /* wake him up */
        workerThread.notify();
      }
    }
    // else{
    // System.out.println("worker thread is not started ignoring
    // event");
    // }

  }
      /**
       * Inherited from Thread, starts the thread.
       */
  public void run() {

    /*
     * This is the worker thread
     * starting delivering sessions and check
     * things such as permissions.
     */
    workerThread = new Thread() {
      public void run() {

        /* as long as the thread is running */
        while (running) {

          if (!syncQueue.isEmpty()) {
            synchronized(syncQueue){
              /* lock the object */
              synchronized (this) {
                try {
                  /*
                   * get the service references do this now to ensure
                   * that only handlers registered now will recive the
                   * event
                   */

                  /* method variable holding all service references */
                  ServiceReference[] serviceReferences;

                  /* get the references */
                  serviceReferences = eventAdmin.getReferences();
                  if(serviceReferences!=null){
                    /* get the 'Event' not the InternalAdminEvent */
                    InternalAdminEvent event = (InternalAdminEvent) syncQueue
                        .getFirst();

                    /* remove it from the list */
                    syncQueue.removeFirst();

                    /* get the securityManager */
                    SecurityManager securityManager = getSecurityManager();
                    /*
                     * variable indicates if the handler is allowed to
                     * publish
                     */
                    boolean canPublish;

                    /*
                     * variable indicates if handlers are granted access
                     * to topic
                     */
                    boolean canSubscribe;


                    /* variable indicates if the topic is well formatted
                     *
                     */
                    boolean isWellFormatted;

                    /* check if security is applied */
                    if (securityManager != null) {
                      /* check if there are any security limitation */
                      canPublish = checkPermissionToPublish(
                          (Event) event.getElement(),
                          securityManager);
                    } else {
                      /* no security here */
                      canPublish = true;
                    }

                    if (securityManager != null) {
                      /* check if there are any security limitation */
                      canSubscribe = checkPermissionToSubscribe(
                          (Event) event.getElement(),
                          securityManager);
                    } else {
                      /* no security here */
                      canSubscribe = true;
                    }

                    /* get if the topic is wellformatted */
                    isWellFormatted = topicIsWellFormatted( ((Event)event.getElement()).getTopic());



                    if (canPublish && canSubscribe && isWellFormatted) {

                      /*
                       * create an instance of the deliver session to
                       * deliver events
                       */
                      DeliverSession deliverSession;
                      String sessionName;
                      if(queueType==ASYNCHRONUS_HANDLER){
                         sessionName="ASYNCRONOUS_SESSION:" + sessionCounter;
                         deliverSession = new DeliverSession(
                            event, bundleContext,
                            serviceReferences, log,
                            this,sessionName);
                      }else{
                        sessionName="SYNCRONOUS_SESSION:" + sessionCounter;
                        deliverSession = new DeliverSession(
                            event, bundleContext,
                            serviceReferences, log,
                            this,sessionName);
                      }

                      sessionCounter++;

                      /* start deliver events */
                      deliverSession.start();

                      try {
                        /* wait for notification */

                        wait();
//                          System.out.println("\n***********************************************"
//                                            +"\n** DELIVER SESSION DONE :"+sessionName+"**"
//                                            +"\n***********************************************\n");



                      } catch (InterruptedException e) {
                        /* print the error message */
//                          System.out
//                              .println("Exception in SynchDeliverThread:"
//                                  + e);
                      }catch(Exception e){
                        System.out
                        .println("Exception in SynchDeliverThread:"
                            + e);
                      }

                    } else {
                      /* no permissions at all are given */
                      if (!canPublish && !canSubscribe) {
                        /*
                         * this will happen if an error occurres in
                         * getReferences()
                         */
                        if (log != null) {
                          /* log the error */
                          log
                              .error("No permission to publish and subscribe top topic:"
                                  + ((Event) event
                                      .getElement())
                                      .getTopic());
                        }
                      }

                      /* no publish permission */
                      if (!canPublish && canSubscribe) {
                        /*
                         * this will happen if an error occures in
                         * getReferences()
                         */
                        if (log != null) {
                          /* log the error */
                          log
                              .error("No permission to publishto topic:"
                                  + ((Event) event
                                      .getElement())
                                      .getTopic());
                        }
                      }

                      /* no subscribe permission */
                      if (canPublish && !canSubscribe) {
                        /*
                         * this will happen if an error occures in
                         * getReferences()
                         */
                        if (log != null) {
                          /* log the error */
                          log
                              .error("No permission to granted for subscription to topic:"
                                  + ((Event) event
                                      .getElement())
                                      .getTopic());
                        }
                      }
                    }
                  }else{

                    for(int i=0;i<syncQueue.size();i++){
                      synchronized(syncQueue){
                        syncQueue.remove(i);
                      }
                    }
                  }
                } catch (InvalidSyntaxException e) {
                  /*
                   * this will happen if an error occures in
                   * getReferences()
                   */
                  if (log != null) {
                    /* log the error */
                    log
                        .error("Can't get any service references");
                  }
                }

              }// end synchronized
          }
          } else {
            try {
              /* lock this object */
              synchronized (this) {
                /* wait until notified */
                wait();
              }
            } catch (InterruptedException e) {
              /* this shouldn't happen */
              System.err.println("Worker was interrupted unexpected");
            }
          }

        }//end while...

      }// end run

    };//end worker thread

    /* start the worker */
//      System.out.println("starting worker thread");
    workerThread.start();
  }// end run()

  /**
   * this function checks for invalid topics
   * like null topics and "" topics.
   *
   * @param topic the topic string
   * @return true if well formatted else false if null or "" formatted
   */
  private boolean topicIsWellFormatted(String topic){

    if(topic!=null){
      /* this is the "*" topic  */
      if(topic.length()==1 && topic.equals("*")){
        return true;
      }

      /* this is the "" topic */
      if(topic.length()==0){
        return false;
      }

      /* this is a topic with length >1 */
      if(topic.length()> 1){
        return true;
      }
    }
    /* this is the null topic */
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
      /* create a topic */
      TopicPermission subscribePermission = new TopicPermission(event
          .getTopic(), "subscribe");
      /* check the permission */
      securityManager.checkPermission(subscribePermission);
      /* return true */
      return true;
    } catch (AccessControlException e) {
      /* return false */
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
      /* create a topic */
      TopicPermission publishPermission = new TopicPermission(event
          .getTopic(), "publish");
      /* check the permission */
      securityManager.checkPermission(publishPermission);
      /* return true */
      return true;

    } catch (AccessControlException e) {
      /* return false */
      return false;
    }
  }

  /**
   * returns the security manager
   *
   * @return the security manager if any else null
   */
  private SecurityManager getSecurityManager() {
    /* return the security manager */
    return System.getSecurityManager();
  }

}// end class QueueHandler
