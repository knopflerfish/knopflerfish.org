/*
 * Copyright (c) 2005-2008, KNOPFLERFISH project
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

import java.util.HashSet;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Class which handles the event deliveries to event handlers.
 *
 * @author Magnus Klack,Martin Berg (refactoring by Björn Andersson)
 */
public class DeliverSession {

    private static final String TIMEOUT_PROP
      = "org.knopflerfish.eventadmin.timeout";

    /** local event variable */
    private Event event;

    /** internal admin event */
    private InternalAdminEvent internalEvent;

    /** local array of service references */
    private ServiceReference[] serviceReferences;

    /** the wildcard char */
    private final static String WILD_CARD = "*";

    /** The timeout variable. Default: no timeout */
    private long timeout = 0;

    /** the references to the blacklisted handlers */
    private static HashSet blacklisted = new HashSet();

    /**
     * Standard constructor for DeliverSession.
     *
     * @param evt the event to be delivered
     * @param context the bundle context
     * @param owner The thread which launched the deliver session
     * @param name the type of delivery which is being made, either
     *             synchronous or asynchronous
     */
    public DeliverSession(InternalAdminEvent evt) {
      internalEvent = evt;
      event = internalEvent.getEvent();
      serviceReferences = internalEvent.getReferences();

      /* Tries to get the timeout property from the system*/
      try {
        String timeoutS = Activator.bundleContext.getProperty(TIMEOUT_PROP);
        if (null!=timeoutS && 0<timeoutS.length()) {
          timeout = Long.parseLong(timeoutS);
        }
      } catch (NumberFormatException ignore) {}
    }

    /**
     *  Initiates the delivery.
     */
    public void deliver() {
      if (serviceReferences == null) {
        return;
      }

      /* method variable indicating that the topic mathces */
      boolean isSubscribed = false;
      /* method variable indicating that the filter matches */
      boolean filterMatch = false;
      /* method variable indicating if the handler is blacklisted */
      boolean isBlacklisted = false;

      for (int i = 0; i < serviceReferences.length; i++) {
        ServiceReference currentHandlerSR = serviceReferences[i];
        EventHandler currentHandler = (EventHandler)
          Activator.bundleContext.getService(currentHandlerSR);
        isBlacklisted = blacklisted.contains(currentHandlerSR);
        if (!isBlacklisted) {
          String filterString = null;
          try {
            filterString = (String)
              serviceReferences[i].getProperty(EventConstants.EVENT_FILTER);
            if (filterString != null) {
              Filter filter
                = Activator.bundleContext.createFilter(filterString);
              filterMatch = filter==null || filterMatched(event, filter);
            } else {
              filterMatch = true;
            }
          } catch(NullPointerException e) {
            filterMatch = true; // why is this OK??? /EW
          } catch (Exception err) {
            blacklisted.add(currentHandlerSR);
            isBlacklisted = true;
            filterMatch = false;

            // log after blacklisting, in case logging in itself triggers the filter...
            if (Activator.log.doDebug()) {
              Activator.log.warn("Failure when matching filter ("
                                 + filterString + ") of " + currentHandler, err);
            }

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
            if (Activator.log.doDebug()) {
              Activator.log.debug("Invalid topic in handler:" + currentHandler);
            }
            /* blacklist the handler */
            if(!blacklisted.contains(currentHandlerSR)){
              blacklisted.add(currentHandlerSR);
              isBlacklisted=true;
            }
          }

          /* check that all indicating variables fulfills the condition */
          /* and check that the service is still registered */
          if (isSubscribed && filterMatch && !isBlacklisted
              && Activator.bundleContext.getService(serviceReferences[i]) != null) {
            if (timeout == 0) {
              try {
                currentHandler.handleEvent(event);
              } catch (Throwable e) {
                Activator.log.error("Handler threw exception in handleEvent", e);
              }
            } else { // use timeout
              try {
                synchronized (this) {
                  TimeoutDeliver timeoutDeliver = new TimeoutDeliver(Thread.currentThread(), currentHandler);
                  timeoutDeliver.start();
                  wait(timeout);
                  Activator.log.error("NOTIFER TIMED OUT: "+ timeoutDeliver.getName());
                  /* check if already blacklisted by another thread */
                  if (!blacklisted.contains(currentHandlerSR)) {
                    blacklisted.add(currentHandlerSR);
                    if (Activator.log.doDebug()) {
                      Activator.log.debug("The handler " + currentHandler.toString()
                                          + " was blacklisted due to timeout");
                    }
                  }
                }
              } catch (InterruptedException e) {
                /* this will happen if a deliverance succeeded */
              }
            }//end use timeout
          }
        }//end if(!isBlacklisted.....
      }//end for
    }// end deliver...

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
        String[] eventTopic = splitPath(event.getTopic());
        /* Split the desired topic into a string array */
        String[] desiredTopic = splitPath(topic);

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
        if (Activator.log.doDebug()) Activator.log.debug("TimeOutDeliver.run()");
        try {
          currentHandler.handleEvent(event);
        } catch (Throwable e) {
          Activator.log.error("Handler threw exception in handleEvent", e);
        }
        /* tell the owner that notification is done */
        owner.interrupt();
      }
    }


    /**
     * Split a string into words separated by "/".
     *
     * @param s  String to split.
     * @return   String array of path components.
     */
    public static String [] splitPath(String s) {
      Vector        v    = new Vector();
      int           si   = 0; 
      int           ei   = s.indexOf('/');

      while (ei != -1) {
        v.addElement(s.substring(si, ei));
        si = ei + 1;
        ei = s.indexOf('/', si);
      }
      if (si < s.length()) {
        v.addElement(s.substring(si));
      }
      String [] r = new String[v.size()];
      v.copyInto(r);

      return r;
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
      Activator.log.debug(message);
    }
  }

}
