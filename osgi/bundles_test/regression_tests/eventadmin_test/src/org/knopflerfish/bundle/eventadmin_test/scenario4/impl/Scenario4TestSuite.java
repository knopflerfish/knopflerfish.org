/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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
package org.knopflerfish.bundle.eventadmin_test.scenario4.impl;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.knopflerfish.bundle.eventadmin_test.scenario4.Scenario4;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite for testing the requirements specified in the test specification
 * for the EventAdmin service.
 *
 * Check the filter both faulty and correctly, also check if the events are
 * received in the order that they were sent both synchronously and
 * asynchronously.
 *
 * @author Martin Berg
 *
 */
public class Scenario4TestSuite extends TestSuite implements Scenario4 {
  /** bundle context variable */
  BundleContext bundleContext;

  /**
   * Constructor for the TestSuite class.
   *
   * @param context
   *            the handle to the frame work
   */
  public Scenario4TestSuite(BundleContext context) {
    super("Scenario 4");
    /* assign the bundelContext variable */
    bundleContext = context;

    /* keys and properties to be used in the EventProducers*/
    String [] keysAndProps1 = {"year", "2004", "month", "12"};
    String [] keysAndProps2 = {"year", "2005", "month", "12"};
    String [] keysAndProps3 = {"YEAR", "2005", "month", "11"}; // Won't year filters match because year is not present?

    /*Topics to be used in the EventConsumers*/
    String[] scenario4_topics1 = { "com/acme/timer" };
    /*Filters to be used in the EventConsumers*/
    String scenario4_filter1 = "(year=2004)";
    String scenario4_filter2 = "(year=2005)";
    String scenario4_filter3 = "(year:2004)";
    String scenario4_filter4 = null;
    String scenario4_filter5 = "(month=12)";

    /* add the setup */
    addTest(new Setup());
    /* add the event consumers to the test suite */
    EventConsumer[] eventConsumer = new EventConsumer[] {
      new EventConsumer(bundleContext, scenario4_topics1,
                        1, 1, scenario4_filter1, "Scenario 4 EventConsumer1", 4),
      new EventConsumer(bundleContext, scenario4_topics1,
                        2, 2, scenario4_filter2, "Scenario 4 EventConsumer2", 4),
      new EventConsumer(bundleContext, scenario4_topics1,
                        0, 0, scenario4_filter3, "Scenario 4 EventConsumer3", 4),
      new EventConsumer(bundleContext, scenario4_topics1,
                        3, 3, scenario4_filter4, "Scenario 4 EventConsumer4", 4),
      new EventConsumer(bundleContext, scenario4_topics1,
                        2, 2, scenario4_filter5, "Scenario 4 EventConsumer5", 4) };
    addTest(eventConsumer[0]);
    addTest(eventConsumer[1]);
    addTest(eventConsumer[2]);
    addTest(eventConsumer[3]);
    addTest(eventConsumer[4]);

    /* add the event publisher to the test suite */
    addTest(new EventPublisher(bundleContext, "Scenario 4 EventPublisher1",
                               "com/acme/timer", keysAndProps1, 4, 1));
    addTest(new EventPublisher(bundleContext, "Scenario 4 EventPublisher2",
                               "com/acme/timer", keysAndProps2, 4, 1));
    addTest(new EventPublisher(bundleContext, "Scenario 4 EventPublisher3",
                               "com/acme/timer", keysAndProps3, 4, 1));
    /* add the cleanup class */
    addTest(new Cleanup(eventConsumer));
  }

  /**
   * Sets up neccessary environment
   *
   *@author Magnus Klack
   */
  class Setup extends TestCase {
    public Setup(){

    }
    public void runTest() throws Throwable {

    }
    public String getName() {
      String name = getClass().getName();
      int ix = name.lastIndexOf("$");
      if(ix == -1) {
        ix = name.lastIndexOf(".");
      }
      if(ix != -1) {
        name = name.substring(ix + 1);
      }
      return name;
    }
  }

  /**
   * Clean up the test suite
   *
   * @author Magnus Klack
   */
  class Cleanup extends TestCase {
    private EventConsumer[] eventConsumer;

    public Cleanup(EventConsumer[] eventConsumer) {
      this.eventConsumer = eventConsumer;
    }
    public void runTest() throws Throwable {
      Throwable error = null;
      for (int i=0; i<eventConsumer.length; i++) {
        try {
          eventConsumer[i].cleanup();
        } catch (Throwable e) {
          error = e;
        }
      }
      if (error != null) throw error;
    }
    public String getName() {
      String name = getClass().getName();
      int ix = name.lastIndexOf("$");
      if(ix == -1) {
        ix = name.lastIndexOf(".");
      }
      if(ix != -1) {
        name = name.substring(ix + 1);
      }
      return name;
    }
  }


  class EventPublisher extends TestCase {

    /** A reference to a service */
    private ServiceReference serviceReference;

    /** The admin which delivers the events */
    private EventAdmin eventAdmin;

    /** A calendar used to get the system time */
    private Calendar calendar;

    /** a variable indicating if the publisher is running */
    private boolean running;

    /** class variable holding bundle context */
    private BundleContext bundleContext;

    /** variable holding messages to send */
    private int messageTosend;

    /** variable holding the topic to use */
    private String topicToSend;

    /** variable holding the parameters to use */
    private String[] propertiesToSend;

    public EventPublisher(BundleContext context, String name,
                          String topic, String[] properties, int id, int numOfMessage) {
      /* call super class */
      super(name + ":" + id);
      /* assign number of messages */
      messageTosend = numOfMessage;
      /* assign bundleContext */
      bundleContext = context;
      /* assign topicToSend */
      topicToSend = topic;
      /* assign propertiesToSend */
      propertiesToSend = properties;
    }

    public void runTest() throws Throwable {
      /* Claims the reference of the EventAdmin Service */
      serviceReference = bundleContext
        .getServiceReference(EventAdmin.class.getName());

      /* assert that a reference is aquired */
      assertNotNull(getName()
                    + " Should be able to get reference to EventAdmin service",
                    serviceReference);

      if (serviceReference == null) {
        fail(getName() + " service reference should not be null");
      }

      eventAdmin = (EventAdmin) bundleContext
        .getService(serviceReference);

      assertNotNull(getName()
                    + " Should be able to get instance to EventAdmin object");

      if (eventAdmin == null) {
        fail(getName() + " event admin should not be null");
      }

      /* a Hash table to store message in */
      Dictionary message = new Hashtable();
      for(int j = 0; j < propertiesToSend.length; j += 2) {
        /*fill the propstable*/
        System.out.println("Adding the following to the propsTable:" + propertiesToSend[j] + " and " + propertiesToSend[j+1]);
        message.put(propertiesToSend[j], propertiesToSend[j+1]);
      }

      for (int i = 0; i < messageTosend; i++) {
        message.put("Synchronus message",new Integer(i));
        /* test print out */
        System.out.println(getName() + " sending a Synchronus event with message:" +
                           message.toString() + "and the topic:" + topicToSend);
        /* send the message */
        eventAdmin.sendEvent(new Event(topicToSend, message));
      }


      /* a Hash table to store message in */
      message = new Hashtable();
      for(int j = 0; j < propertiesToSend.length;  j += 2) {
        /*fill the propstable*/
        System.out.println("Adding the following to the propsTable:" + propertiesToSend[j] + " and " + propertiesToSend[j+1]);
        message.put(propertiesToSend[j], propertiesToSend[j+1]);
      }

      for (int i = 0; i < messageTosend; i++) {
        message.put("Asynchronus message",new Integer(i));
        /* test print out */
        System.out.println(getName() + " sending an Asynchronus event with message:" +
                           message.toString() + "and the topic:" + topicToSend);
        /* send the message */
        eventAdmin.sendEvent(new Event(topicToSend, message));
      }

    }
  }

  class EventConsumer extends TestCase implements EventHandler {
    /** class variable for service registration */
    private ServiceRegistration serviceRegistration;

    /** class variable indicating the instance name */
    private int instanceId;

    /** class variable indicating the topics */
    private String[] topicsToConsume;

    /** class variable indicating the topics */
    private String filterToConsume;

    /** class variable keeping number of asynchronus message */
    private int asynchMessages=0;

    /** class variable keeping number of asynchronus message */
    private int synchMessages=0;

    /** class variable keeping number of unidentified message */
    private int unidentMessages=0;

    /** class variable indication the number of synchronous messages to be received */
    private int numSyncMessages;

    /** class variable indication the number of asynchronous messages to be received */
    private int numAsyncMessages;

    private Throwable error;

    /**
     * Constructor creates a consumer service
     *
     * @param bundleContext
     * @param topics
     */
    public EventConsumer(BundleContext bundleContext, String[] topics,
                         int numSyncMsg, int numAsyncMsg, String filter, String name,
                         int id) {
      /* call super class */
      super(name + ":" + id);
      /* assign the instance id */
      instanceId = id;
      /* assign the consume topics */
      topicsToConsume = topics;
      /* assign the consume filter */
      filterToConsume = filter;
      /*assign the number of synchronous messages to consume*/
      numSyncMessages = numSyncMsg;
      /*assign the number of asynchronous messages to consume*/
      numAsyncMessages = numAsyncMsg;

    }

    public void runTest() throws Throwable {
      asynchMessages=0;
      synchMessages=0;
      unidentMessages=0;
      /* create the hashtable to put properties in */
      Dictionary props = new Hashtable();
      /* put service.pid property in hashtable */
      props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
      /*if the filter to consume isn't null */
      if (filterToConsume != null){
        /* put service.pid property in hashtable */
        props.put(EventConstants.EVENT_FILTER, filterToConsume);
      }

      /* register the service */
      serviceRegistration = bundleContext.registerService(
                                                          EventHandler.class.getName(), this, props);

      assertNotNull(getName()
                    + " service registration should not be null",
                    serviceRegistration);

      if (serviceRegistration == null) {
        fail("Could not get Service Registration ");
      }
    }

    public void cleanup() throws Throwable {
      try {
        serviceRegistration.unregister();
      } catch (IllegalStateException ignore) {}
      if (error != null) {
        throw error;
      }
      assertTrue("Not all synch messages recieved", synchMessages == numSyncMessages);
      assertTrue("Not all asynch messages recieved", asynchMessages == numAsyncMessages);
    }

    /**
     * This method takes events from the event admin service.
     */
    public void handleEvent(Event event) {
      try {
        /* try to get the message */
        Object message = event.getProperty("Synchronus message");

        Object filter1 = event.getProperty("year");
        Object filter2 = event.getProperty("month");
        String eventTopic = event.getTopic();
        if(message != null){
          /* its an asyncronous message */
          synchMessages++;

          System.out.println(getName() + " recived an Synchronus event with message:" +
                             message.toString() + ", topic:"+ eventTopic + ", property_year:" +
                             filter1 + ", property_month:" + filter2 + " number of sync messages received:" + synchMessages);

        } else {
          message = event.getProperty("Asynchronus message");
          if (message != null) {
            asynchMessages++;
            System.out.println(getName() + " recived an Asynchronus event with message:" +
                               message.toString() + ", topic:"+ eventTopic + ", property_year:" +
                               filter1 + ", property_month:" + filter2 + " number of async messages received:" + asynchMessages);
          } else {
            unidentMessages++;
            System.out.println(getName() + " recived an Unidentified event with message:null, topic:" +
                               eventTopic + ", property_year:" +
                               filter1 + ", property_month:" + filter2 + " number of unidentified messages received:" + unidentMessages);
          }
        }

        /* assert that the messages property is not null */
        assertNotNull("Message should not be null in handleEvent()",message);
        /* assert that the messages of syncronous type are not to many */
        assertTrue("to many synchronous messages in:" + getName() + " (" + synchMessages + " >= " + (numSyncMessages+1) + ")", synchMessages<numSyncMessages+1);
        /* assert that the messsage of the asyncronous type are not to many */
        assertTrue("to many asynchronous messages in:" + getName() + " (" + asynchMessages + " >= " + (numAsyncMessages+1) + ")", asynchMessages<numAsyncMessages+1);
      } catch (RuntimeException e) {
        error = e;
        throw e;
      } catch (Throwable e) {
        error = e;
      }
    }
  }
}
