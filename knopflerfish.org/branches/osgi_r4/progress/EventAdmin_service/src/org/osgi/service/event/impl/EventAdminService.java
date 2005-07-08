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
package org.osgi.service.event.impl;

import java.security.AccessControlException;

import java.util.Calendar;
import java.util.LinkedList;

import org.knopflerfish.service.log.LogRef;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.TopicPermission;

/**
 * Default implementation of the EventAdmin interface this is a singleton class
 * and should always be active. The implementation is responsible for track
 * eventhandlers and check their permissions. It will also
 * host two threads sending diffrent types of data. If an eventhandler is subscribed to the
 * published event the EventAdmin service will put the event on one of the two internal sendstacks
 * depending on what type of deliverance the event requires.  
 * 
 * @author Magnus Klack
 */
public class EventAdminService implements EventAdmin {
    /** the local representation of the bundle context */
    private BundleContext bundleContext;

    /** the log * */
    private LogRef log;

    /** variable holding the synchronus send procedure */
    private QueueHandler queueHandlerSynch;

    /** variable holding the asynchronus send procedure */
    private QueueHandler queueHandlerAsynch;

    /**
     * the constructor use this to create a new Event admin service.
     * 
     * @param context
     *            the BundleContext
     */
    public EventAdminService(BundleContext context) {
        /* assign the context to the local variable */
        bundleContext = context;
        /* create the log */
        log = new LogRef(context);
        /* create the synchronus sender process */
        queueHandlerSynch = new QueueHandler();
        /* start the asynchronus sender */
        queueHandlerSynch.start();
        /* create the asynchronus queue handler */
        queueHandlerAsynch = new QueueHandler();
        /* start the handler */
        queueHandlerAsynch.start();

    }

    /**
     * This method should be used when an asynchronus events are to be
     * published.
     * 
     * @param event
     *            the event to publish
     */
    public void postEvent(Event event) {
        /* create a calendar */
        Calendar time = Calendar.getInstance();
        /* create an internal admin event */
        InternalAdminEvent adminEvent = new InternalAdminEvent(event, time,
                this);
        /* add the admin event to the queueHandlers send queue */
        queueHandlerAsynch.addEvent(adminEvent);
        /* print that we are finsished */
        System.out.println("Event has been sent to the send queue");

    }

    /**
     * This method should be used when synchronous events are to be published
     * 
     * @author Martin Berg, Magnus Klack
     * @param event
     *            the event to publish
     */
    public void sendEvent(Event event) {
        System.out.println("INCOMMING SYNCHRONOUS EVENT");
        /* create a calendar */
        Calendar time = Calendar.getInstance();
        /* create an internal admin event */
        InternalAdminEvent adminEvent = new InternalAdminEvent(event, time,
                this);
        /* add the admin event to the queueHandlers send queue */
        queueHandlerSynch.addEvent(adminEvent);
        /* wait untill the object have been finished */

        try {
            /* lock this object */
            synchronized (this) {
                /* wait until delivered */
                wait();
            }

        } catch (InterruptedException e) {
            /* write the exception */
            System.out.println(e);
        }

        /* print that we are finsished */
        System.out.println("FINISHED");

    }

    /**
     * checks the permission to subscribe to this subject. OBS! this one will
     * only se if there are any permissions granted for all objects to
     * subscribe.
     * 
     * @param event
     *            the subscription event
     * @param securityManager
     *            the system securitymanager
     * @return true if the object is permitted else false
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
     * @param event
     *            the event to be published
     * @param securityManager
     *            the system security manager
     * @return true if the publisher can publish the subject else false
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

    /**
     * returns the servicereferences
     * 
     * @return ServiceReferences[] array if any else null
     * @throws InvalidSyntaxException
     *             if syntax error
     */
    private synchronized ServiceReference[] getReferences()
            throws InvalidSyntaxException {
        try {
            return bundleContext.getServiceReferences(
                    "org.osgi.service.event.EventHandler", null);
        } catch (InvalidSyntaxException e) {
            /* print the error */
            throw e;
        }

    }

    /**
     * this class will send the events synchronus to the event handlers it will
     * pick the first element in the queue and assure that the events will
     * arrive in order.
     * 
     * @author Magnus Klack
     */
    private class QueueHandler extends Thread {
        /** the queue */
        private LinkedList syncQueue = new LinkedList();

        /** variable indicating that this is running */
        private boolean running;

        /** class variable indicating deliverance * */
        private boolean delivering;

        /**
         * Constructor for the QueueHandler
         */
        public QueueHandler() {
            running = true;
            delivering = false;
        }

        /**
         * This adds a new InternalAdminEvent to the que
         * 
         * @param event
         *            the new InternalAdminEvent
         */
        public void addEvent(Object event) {
            /* add the event */
            syncQueue.add(event);
        }

        public void run() {
            /* as long as the thread is running */
            while (running) {
                /*
                 * if the queue has events and the Admin class is not receiving
                 * anything
                 */
                if (!syncQueue.isEmpty()) {
                    /* lock the object */
                    synchronized (this) {
                        /* set state to delivering */
                        delivering = true;

                        try {
                            /*
                             * get the service references do this now to ensure
                             * that only handlers registered now will recive the
                             * event
                             */

                            /* method variable holding all service references */
                            ServiceReference[] serviceReferences;

                            /* get the references */
                            serviceReferences = getReferences();
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

                            if (canPublish && canSubscribe) {

                                /*
                                 * create an instance of the deliver session to
                                 * deliver events
                                 */
                                DeliverSession deliverSession = new DeliverSession(
                                        event, bundleContext,
                                        serviceReferences, log, this);

                                /* start deliver events */
                                deliverSession.start();

                                try {
                                    /* wait for notification */
                                    wait();
                                } catch (InterruptedException e) {
                                    /* print the error message */
                                    System.out
                                            .println("Exception in SynchDeliverThread:"
                                                    + e);
                                }

                            } else {
                                /* no permissions at all are set */
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

                        } catch (InvalidSyntaxException e) {
                            /*
                             * this will happen if an error occures in
                             * getReferences()
                             */
                            if (log != null) {
                                /* log the error */
                                log.error("Can't get any service references");
                            }

                        }
                        synchronized (this) {
                            delivering = false;
                        }
                    }// end synchronized
                }// end if(!asyncQueue.isEmpty....

            }// end while ...
        }// end run()

    }// end class QueueHandler

}
