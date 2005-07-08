/*
 * @(#)DeliverSession.java        1.0 2005/06/28
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

import java.util.Vector;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Private class to deliver events ascynchronous to the event handlers.
 * 
 * @author Magnus Klack,Martin Berg
 */
public class DeliverSession extends Thread {
    /** local event variable */
    private Event event;

    /** internal admin event */
    private InternalAdminEvent internalEvent;

    /** local array of service references */
    private ServiceReference[] serviceReferences;

    /** bundle context */
    private BundleContext bundleContext;

    /** the wildcard char */
    private final static String WILD_CARD = "*";

    /** the timeout variable */
    private long timeOut = 5000;

    /** the references to the blacklisted handlers */
    private static Vector blacklisted = new Vector();

    /** the log reference */
    private LogRef log;

    /** variable indicating thread owner **/
    private Thread ownerThread;

    /**
     * Constructor for the DeliverSession. Use this to deliver events
     * 
     * 
     * @param evt
     *            the event to be delivered
     * @param refs
     *            an array of service references
     */
    public DeliverSession(InternalAdminEvent evt, BundleContext context,
            ServiceReference[] refs, LogRef logRef, Thread owner) {
        internalEvent = evt;
        /* assign the 'evt' argument */
        event = (Event) internalEvent.getElement();
        /* assign the context */
        bundleContext = context;
        /* assign the serviceReferences variable */
        serviceReferences = refs;
        /* assign the log */
        log = logRef;
        /* assign the owner */
        ownerThread = owner;

    }

    /**
     * Start the thread in this case deliver events
     */
    public void run() {
        /* start the deliverance in asynchronus mode */
        if (serviceReferences != null) {
            startDeliver();
        } else {
            internalEvent.setAsDelivered();
        }
    }

    public void startDeliver() {
        /* method variable indicating that the topic mathces */
        boolean isSubscribed = false;
        /* method variable indicating that the filter matches */
        boolean filterMatch = false;
        /* method variable indicating if the handler is blacklisted */
        boolean isBlacklisted = false;
        /* method variable indicating that the topic is right formatted */
        boolean topicIsRight = false;

        /* iterate through all service references */
        for (int i = 0; i < serviceReferences.length; i++) {
            /* get the EventHandler by using its references */
            EventHandler currentHandler = (EventHandler) bundleContext
                    .getService(serviceReferences[i]);

            /* assign the blacklist value */
            isBlacklisted = blacklisted.contains(currentHandler);

            if (!isBlacklisted) {
                try {
                    /* get the filter String */
                    String filterString = (String) serviceReferences[i]
                            .getProperty(EventConstants.EVENT_FILTER);

                    /* check that filterString is not null */
                    if (filterString != null) {
                        /* get the filter */
                        Filter filter = bundleContext
                                .createFilter(filterString);
                        /* assign the filterMatch variable */
                        filterMatch = event.matches(filter);
                    } else {
                        /* this means no filter */
                        filterMatch = true;
                    }

                } catch (InvalidSyntaxException err) {
                    /* print the message */
                    if (log != null) {
                        /* log the error */
                        log.error("Invalid Syntax when matching filter");
                    }
                }

                try {
                    /* get the topics */
                    String[] topics = (String[]) serviceReferences[i]
                            .getProperty(EventConstants.EVENT_TOPIC);
                    /* check if topic is null */
                    if (topics != null) {

                        /* check the lenght of the topic */
                        if (topics.length > 0) {
                            /* assign the isSubscribed variable */
                            isSubscribed = anyTopicMatch(topics, event);
                        } else {
                            /* an empty array is set as topic, i.e, {} */
                            isSubscribed = true;
                        }
                    } else {
                        /* no topic given from the handler -> all topic */
                        isSubscribed = true;
                    }
                } catch (ClassCastException e) {
                    
                    if (log != null) {
                        /* log the error */
                        log.error("invalid format of topic the EventHandler:"
                                + currentHandler.toString()
                                + " will be blacklisted");
                    }
                    /* blacklist the handler */
                    blacklisted.add(currentHandler);

                }

                /*
                 * assign the blacklist value again because it could have been
                 * blacklisted on the way
                 */
                isBlacklisted = blacklisted.contains(currentHandler);

                /* check that all indicating variables fulfills the condition */
                if (isSubscribed && filterMatch && !isBlacklisted) {

                    /* check that the service is still registered */
                    if (bundleContext.getService(serviceReferences[i]) != null) {
                        /* start a thread to notify the EventHandler */
                        Thread notifier = new Notifier(currentHandler, this);
                        /* start the thread */
                        notifier.start();
                        try {
                            /* lock this session */
                            synchronized (this) {
                                /* wait for notification */
                                wait();
                            }

                        } catch (InterruptedException e) {
                            System.out.println("Deliver Session interrupted"
                                    + e.getMessage());
                        }

                        /* lock the owner */
                        synchronized (ownerThread) {
                            /* notify the owner */
                            ownerThread.notify();
                        }

                    } else {
                        /* this will happen if service is no longer available */
                        if (log != null) {
                            /* log the error */
                            log.error("Deliverance failed due to"
                                    + " Service is no longer in registry");
                        }

                    }

                } else {
                    /* check if blacklisted */
                    if (isBlacklisted) {
                        /* check the log */
                        if (log != null) {
                            /* log the error */
                            log
                                    .error("Deliverance failed due to a recently blacklisted handler"
                                            + " ,i.e, mallformatted topic");
                        }
                    }

                    /* check if no topic match */
                    if (!isSubscribed) {
                        /* check the log */
                        if (log != null) {
                            /* log the error */
                            log
                                    .error("Deliverance failed due to no match on topic");
                        }
                    }

                    /* check if match no match on filter */
                    if (!filterMatch) {
                        /* check the log */
                        if (log != null) {
                            /* log the error */
                            log
                                    .error("Deliverance failed due to no match on filter");
                        }
                    }

                }

            } else {
                /* this will happen if the handler is already blacklisted */
                log.error("Deliverance failed due to a blacklisted handler");
            }//end if(!isBlacklisted.....
        }

        /* set the event to delivered  */
        internalEvent.setAsDelivered();
    }

    private boolean isInTime(InternalAdminEvent event) {

        return true;

    }

    /**
     * This method should be used when matching from an event against a specific
     * filter in an Eventhandler
     * 
     * @author Martin Berg
     * @param event
     *            the event to compare
     * @param filter
     *            the filter the listener is interested in
     */
    private boolean filterMatched(Event event, Filter filter) {
        /* return the functions return value */
        if (filter == null) {
            return true;
        } else {
            /* return the match value */
            return event.matches(filter);
        }
    }

    /**
     * Iterates through a set of topics and if any element matches the events
     * topic it will return true.
     * 
     * @param topics
     *            the event topic
     * @param event
     *            the event
     * @return true if any element matche else false
     */
    private boolean anyTopicMatch(String[] topics, Event event) {
        /* variable if we have a match */
        boolean haveMatch = false;

        /* iterate through the topics array */
        for (int i = 0; i < topics.length; i++) {
            if (!haveMatch) {

                /* check if this topic matches */
                if (topicMatch(event, topics[i])) {
                    /* have a match */
                    haveMatch = true;
                }

            } else {
                /* leave the iteration */
                i = topics.length;
            }
        }

        return haveMatch;

    }

    /**
     * This method should be used when matching a topic on an event against a
     * specific topic in an Eventhandler
     * 
     * @author Martin Berg
     * @param event
     *            the event to compare
     * @param topic
     *            the topic the listener is interested in
     */
    private boolean topicMatch(Event event, String topic) {
        /* Split the event topic into an string array */
        String[] eventTopic = event.getTopic().split("/");
        /* Split the desired topic into a string array */
        String[] desiredTopic = topic.split("/");

        /* iterator value */
        int i = 0;
        /* If wildCard "*" is found */
        boolean wildCard = false;
        /* If topic matches */
        boolean topicMatch = true;
        /* iterate and check if there is a match */
        while ((i < eventTopic.length) && (wildCard == false)
                && (topicMatch == true)) {
            if (!(eventTopic[i].equals(desiredTopic[i]))) {
                if (desiredTopic[i].equals(WILD_CARD)) {
                    wildCard = true;
                } else {
                    topicMatch = false;
                }
            }
            i++;
        }
        return topicMatch;
    }

    /**
     * This class will start a deliver thread, i.e, instance of 'TimeoutDeliver'
     * and wait for N seconds, if N seconds have passed the class will blacklist
     * the current EventHandler class. if the deliver thread returns in N
     * seconds an InterruptedException will be catched and the thread exits.
     * 
     * @author Magnus Klack & Johnny Baveras
     */
    private class Notifier extends Thread {
        /** local representation of the eventhandler */
        private EventHandler currentHandler;

        /** local variable represents the owner of the process */
        private DeliverSession deliverSession;

        /**
         * Constructor of the Notifier class
         * 
         * @param handler
         *            the EventHandler class to be notified
         */
        public Notifier(EventHandler handler, DeliverSession owner) {
            currentHandler = handler;
            deliverSession = owner;
        }

        public void run() {
            /* try to start a deliver session */
            try {
                synchronized (this) {
                    /* create a deliver object */
                    TimeoutDeliver timeoutDeliver = new TimeoutDeliver(this,
                            currentHandler);
                    /* start the thread */
                    timeoutDeliver.start();
                    /* wait for N seconds */
                    wait(timeOut);
                    /* check if already blacklisted by another thread */
                    if (!blacklisted.contains(currentHandler)) {
                        /* add it to the vector */
                        blacklisted.addElement(currentHandler);

                        if (log != null) {
                            log.error("The handler "
                                    + currentHandler.toString()
                                    + " was blacklisted due to timeout");
                        }
                    }
                   

                }

            } catch (InterruptedException e) {
                /* this will happen if a deliverance succeeded */
                if (log != null) {
                    log.info("Deliverance of event with topic "
                            + event.getTopic() + "was done");
                }

            }

            synchronized (deliverSession) {
                deliverSession.notify();
            }

        }

    }

    /**
     * This class will try to update the EventHandler if it succeed an interrupt
     * will be performed on the 'owner' class.
     * 
     * @author Magnus Klack & Johnny Baveras
     *  
     */
    private class TimeoutDeliver extends Thread {
        /** local representation of the main class */
        private Object owner;

        /** local representation of the EventHandler to be updated */
        private EventHandler currentHandler;

        /**
         * Constructor of the TimeoutDeliver object
         * 
         * @param main
         *            the owner object
         * @param handler
         *            the event handler to be updated
         */
        public TimeoutDeliver(Object main, EventHandler handler) {
            owner = main;
            currentHandler = handler;
        }

        public void run() {
            try {

                /* call the handlers 'update' function */
                currentHandler.handleEvent(event);

                /* tell the owner that notification is done */
                ((Thread) owner).interrupt();
            } catch (Exception e) {
                if (log != null) {
                    log.error("TimeOutDeliver.run() caught an Exception "
                            + e.getMessage()
                            + "while delivering event with topic "
                            + event.getTopic());
                }
                /* interrupt the owner thread to avoid blacklist */
                ((Thread) owner).interrupt();
            }
        }

    }

}
