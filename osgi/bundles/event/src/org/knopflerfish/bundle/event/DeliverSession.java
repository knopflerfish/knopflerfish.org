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
package org.knopflerfish.bundle.event;

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
 * Thread which handles the event deliveries to event handlers.
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
    private long timeOut = 20000;

    /** the references to the blacklisted handlers */
    private static Vector blacklisted = new Vector();

    /** the log reference */
    private LogRef log;

    /** variable indicating thread owner **/
    private Thread ownerThread;

    /**
     * Standard constructor for DeliverSession.
     *
     * @param evt the event to be delivered
     * @param refs an array of service references
     * @param context the bundle context
     * @param logRef reference to a LogRef
     * @param owner The thread which launched the deliver session
     * @param name the type of delivery which is being made, either synchronous or asynchronous
     */
    public DeliverSession(InternalAdminEvent evt, BundleContext context,
            ServiceReference[] refs, LogRef logRef, Thread owner,String name) {
      super(name);
      internalEvent = evt;
      event = (Event) internalEvent.getElement();
      bundleContext = context;
      serviceReferences = refs;
      log = logRef;
      ownerThread = owner;

      /* Tries to get the timeout property from the system*/
      try {
        timeOut=  Long.parseLong(System.getProperty("org.knopflerfish.eventadmin.timeout"));
      } catch (NumberFormatException ignore) {}
    }

    /**
     * Inherited from Thread, starts the thread.
     */
    public void run() {
        /* start the deliverance in asynchronus mode */
        if (serviceReferences != null) {
            startDeliver();
        } else {
            internalEvent.setAsDelivered();
            /* lock the owner */
            synchronized (ownerThread) {
                /* notify the owner */
                ownerThread.notify();
            }
        }
    }

    /**
     *  Initiates the delivery.
     */
    protected void startDeliver() {
      /* method variable indicating that the topic mathces */
      boolean isSubscribed = false;
      /* method variable indicating that the filter matches */
      boolean filterMatch = false;
      /* method variable indicating if the handler is blacklisted */
      boolean isBlacklisted = false;
      /* method variable indicating that the eventhandler should have this event */
      boolean isInTime = false;

      for (int i = 0; i < serviceReferences.length; i++) {
        EventHandler currentHandler = (EventHandler) bundleContext
                .getService(serviceReferences[i]);
        isBlacklisted = blacklisted.contains(currentHandler);
        if (!isBlacklisted) {
          try {
            String filterString = (String) serviceReferences[i].getProperty(EventConstants.EVENT_FILTER);
            if (filterString != null) {
              Filter filter = bundleContext.createFilter(filterString);
              if (filter!=null) {
                filterMatch = filterMatched(event,filter);
              } else {
                filterMatch = true;
              }
            } else {
              filterMatch = true;
            }
          } catch(NullPointerException e) {
            filterMatch = true;
          } catch (InvalidSyntaxException err) {
            if (log != null && log.doDebug()) {
              log.debug("Invalid Syntax when matching filter of " + currentHandler);
            }
            blacklisted.add(currentHandler);
            isBlacklisted=true;
            /* this means no filter match */
            filterMatch = false;
          }

          try {
            /* get the topics */
            String[] topics = (String[]) serviceReferences[i]
                    .getProperty(EventConstants.EVENT_TOPIC);
            /* check if topic is null */
            if (topics != null) {
              /* check the lenght of the topic */
              if (topics.length > 0 ) {
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
            if (log != null && log.doDebug()) {
              log.debug("Invalid topic in handler:" + currentHandler);
            }
            /* blacklist the handler */
            if(!blacklisted.contains(currentHandler)){
              blacklisted.add(currentHandler);
              isBlacklisted=true;
            }
          }

          isInTime=this.isInTime(serviceReferences[i],internalEvent);

          /* check that all indicating variables fulfills the condition */
          if (isSubscribed && filterMatch && !isBlacklisted && isInTime) {
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
                log.error("DeliverSession object was interrupted this is not expected", e);
              }
            }
          }
        }//end  if(!isBlacklisted.....

        /* set the event as delivered  */
        synchronized(internalEvent){
          internalEvent.setAsDelivered();
        }

        /* lock the owner */
        synchronized (ownerThread) {
            /* notify the owner */
          ownerThread.notify();
        }
     }
    }// end startDeliver...

    /**
     * isInTime determines whether a handler is eligable for a certain message or not.
     * The handler has to be registered before the event was registered.
     * @param handler the handler to receive the message
     * @param event the event to be sent
     * @return true if the handler should receive the message, false otherwise
     * @author Johnny Baveras
     */
    private boolean isInTime(ServiceReference handler,  InternalAdminEvent event) {
         /* Gets the registration time of the handler. */
         Long handlerTime = (Long) MultiListener.eventHandlers.get(handler);
         /* Gets the timestamp stored in the internal event and converts it to the standard used by the handler */
         long eventTime = event.getTimeStamp().getTimeInMillis();
         /* Determines the value of the boolean to be returned */
         return (handlerTime==null || eventTime<=handlerTime.longValue()) ? false : true;
    }

    /**
     * This method should be used when matching from an event against a specific
     * filter in an Eventhandler
     * @author Martin Berg
     * @param event the event to compare
     * @param filter the filter the listener is interested in
     */
    private boolean filterMatched(Event event, Filter filter) {
        /* return the functions return value */
        if (filter == null) {
            return true;
        } else {
            return event.matches(filter);
        }
    }

    /**
     * Iterates through a set of topics and if any element matches the events
     * topic it will return true.
     *
     * @param topics the event topic
     * @param event the event
     * @return true if any element matche else false
     */
    private synchronized boolean anyTopicMatch(String[] topics, Event event) {
        /* variable if we have a match */
        boolean haveMatch = false;

        /* iterate through the topics array */
        for (int i = 0; i < topics.length; i++) {
          if (!haveMatch) {
                /* check if this topic matches */
                if (topicMatch(event, topics[i])) {
                  /* have a match */
                  return true;
                }
            }
        }
        return false;
    }

    /**
     * This method should be used when matching a topic on an event against a
     * specific topic in an Eventhandler
     *
     * @author Martin Berg
     * @param event the event to compare
     * @param topic the topic the listener is interested in
     */
    private synchronized boolean topicMatch(Event event, String topic) {
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
         * @param handler the EventHandler class to be notified
         */
        public Notifier(EventHandler handler, DeliverSession owner) {
            currentHandler = handler;
            deliverSession = owner;
        }
        /**
         * Inherited from Thread, starts the thread.
         */
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
                    log.error("NOTIFER TIMED OUT: "+ deliverSession.getName());
                    /* check if already blacklisted by another thread */
                    if (!blacklisted.contains(currentHandler)) {
                        /* add it to the vector */
                        blacklisted.addElement(currentHandler);

                        if (log != null && log.doDebug()) {
                          log.debug("The handler " + currentHandler.toString()
                                    + " was blacklisted due to timeout");
                        }
                    }
               }
            } catch (InterruptedException e) {
                /* this will happen if a deliverance succeeded */
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
        private Thread owner;

        /** local representation of the EventHandler to be updated */
        private EventHandler currentHandler;

        /**
         * Constructor of the TimeoutDeliver object
         *
         * @param main the owner object
         * @param handler the event handler to be updated
         */
        public TimeoutDeliver(Thread main, EventHandler handler) {
            owner = main;
            currentHandler = handler;
        }

        /**
        Inherited from Thread, starts the thread.
        */
        public void run() {
            try {
              synchronized(currentHandler){
                currentHandler.handleEvent(event);
              }

                /* tell the owner that notification is done */
                owner.interrupt();
            } catch (Exception e) {
                if (log != null && log.doDebug()) {
                  log.debug("TimeOutDeliver.run() caught an Exception "
                            + e.getMessage()
                            + "while delivering event with topic "
                            + event.getTopic());
                }
                /* interrupt the owner thread to avoid blacklist */
                owner.interrupt();
            }
        }
    }



  // The following class is not used. It's a nice class, though, isn't it?

  /**
   * A class used to log messages.
   * @author Magnus Klack
   */
  private class CustomDebugLogger extends Thread{
    /** the log message */
    private String message;
    public CustomDebugLogger(String msg){
      /* assign message */
      message=msg;

    }
    /**
     * Inherited from Thread, starts the thread.
     */
    public void run(){
      log.debug(message);
    }
  }
}
