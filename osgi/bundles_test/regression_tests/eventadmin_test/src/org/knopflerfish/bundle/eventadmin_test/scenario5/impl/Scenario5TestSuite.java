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
package org.knopflerfish.bundle.eventadmin_test.scenario5.impl;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.knopflerfish.bundle.eventadmin_test.scenario5.Scenario5;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite for testing the requirements specified in the test specification
 * for the EventAdmin service.
 *
 * This test tests if it is possible to have several EventAdmins running.
 * And check if the events are published and received anyway.
 *
 * In order to run the test an additonal EventAdmin needs to be started on the
 * framework.
 *
 * @author Martin Berg
 *
 */
public class Scenario5TestSuite extends TestSuite implements Scenario5 {
  /** bundle context variable */
  BundleContext bundleContext;

  /**
   * Constructor for the TestSuite class.
   *
   * @param context
   *            the handle to the frame work
   */
  public Scenario5TestSuite(BundleContext context) {
    super("Scenario 5");
    /* assign the bundelContext variable */
    bundleContext = context;
    /* create a topic string */
    String[] scenario5_topics1 = { "com/acme/timer", "com/acme/log" };

    /* add the setup */
    addTest(new Setup());
    /* add the event consumers to the test suite */
    EventConsumer[] eventConsumer = new EventConsumer[] {
      new EventConsumer(bundleContext, scenario5_topics1, 8, 8,
                        "Scenario 5 EventConsumer1", 5),
      new EventConsumer(bundleContext, scenario5_topics1, 8, 8,
                        "Scenario 5 EventConsumer2", 5) };
    addTest(eventConsumer[0]);
    addTest(eventConsumer[1]);

    /* add the event publisher to the test suite */
    addTest(new EventPublisher(bundleContext, "Scenario 5 EventPublisher1",
                               5, 4, "com/acme/timer"));
    addTest(new EventPublisher(bundleContext, "Scenario 5 EventPublisher2",
                               5, 4, "com/acme/log"));
    /* add the cleanup class */
    addTest(new Cleanup(eventConsumer));
  }

  /**
   * Sets up neccessary environment
   *
   *@author Magnus Klack
   */
  class Setup extends TestCase {
    public Setup() {

    }

    public void runTest() throws Throwable {

    }

    public String getName() {
      String name = getClass().getName();
      int ix = name.lastIndexOf("$");
      if (ix == -1) {
        ix = name.lastIndexOf(".");
      }
      if (ix != -1) {
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
      if (ix == -1) {
        ix = name.lastIndexOf(".");
      }
      if (ix != -1) {
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

    public EventPublisher(BundleContext context, String name, int id,
                          int numOfMessage, String topic) {
      /* call super class */
      super(name + ":" + id);
      /* assign number of messages */
      messageTosend = numOfMessage;
      /* assign bundleContext */
      bundleContext = context;
      /*assign topic */
      topicToSend = topic;
      /*assign localCopy*/
    }

    public void runTest() throws Throwable {
      /* determine if to use a local copy of EventAdmin or not */
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

      Thread synchDeliver = new Thread() {
          public void run() {

            for (int i = 0; i < messageTosend; i++) {
              /* a Hash table to store message in */
              Dictionary message = new Hashtable();
              /* put some properties into the messages */
              message.put("Synchronus message", new Integer(i));
              /* send the message */
              System.out.println(getName()
                                 + " sending a synchronus event with message:"
                                 + message.toString() + "and the topic:"
                                 + topicToSend);
              eventAdmin.sendEvent(new Event(topicToSend, message));
            }
          }
        };

      synchDeliver.start();
      synchDeliver.join();

      Thread asynchDeliver = new Thread() {
          public void run() {

            for (int i = 0; i < messageTosend; i++) {
              /* create the hasht table */
              Dictionary message = new Hashtable();
              /* create the message */
              message.put("Asynchronus message", new Integer(i));
              /* Sends a synchronous event to the admin */
              System.out.println(getName()
                                 + " sending an Asynchronus event with message:"
                                 + message.toString() + "and the topic:"
                                 + topicToSend);
              eventAdmin.postEvent(new Event(topicToSend, message));
            }
          }
        };
      asynchDeliver.start();
      asynchDeliver.join();

      try {
        Thread.sleep(500); // allow for delivery
      } catch (Exception ignore) {}
    }
  }

  class EventConsumer extends TestCase implements EventHandler {
    /** class variable for service registration */
    private ServiceRegistration serviceRegistration;

    /** class variable indicating the instance name */
    private int instanceId;

    /** class variable indicating the topics correct version*/
    private String[] topicsToConsume;

    /** class variable keeping number of asynchronus message */
    private int asynchMessages = 0;

    /** class variable keeping number of asynchronus message */
    private int synchMessages = 0;

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
                         int numSyncMsg, int numAsyncMsg, String name, int id) {
      /* call super class */
      super(name + ":" + id);
      /* assign the instance id */
      instanceId = id;
      /* assign the consume topics */
      topicsToConsume = topics;
      /*assign the number of synchronous messages to consume*/
      numSyncMessages = numSyncMsg;
      /*assign the number of asynchronous messages to consume*/
      numAsyncMessages = numAsyncMsg;
    }

    public void runTest() throws Throwable {
      asynchMessages = 0;
      synchMessages = 0;
      System.out
        .println("!!! TO RUN THIS TEST CORRECTLY TWO EVENTADMINS NEEDS TO RUN IN THE FRAMEWORK!!!");
      /* create the hashtable to put properties in */
      Dictionary props = new Hashtable();
      /* determine what topicType to use */
      /* put service.pid property in hashtable */
      props.put(EventConstants.EVENT_TOPIC, topicsToConsume);

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
        //System.out.println(getName() + " recived an event");

        Object message;
        /* try to get the message */
        message = event.getProperty("Synchronus message");

        if (message != null) {
          /* its an asyncronous message */
          synchMessages++;

          System.out.println(getName()
                             + " recived an Synchronus event with message:"
                             + message.toString() + " the nr of syncmsgs:" + synchMessages);

        } else {
          message = event.getProperty("Asynchronus message");
          if (message != null) {
            asynchMessages++;
            System.out.println(getName()
                               + " recived an Asynchronus event with message:"
                               + message.toString() + " the nr of asyncmsgs:" + asynchMessages);
          }
        }

        /* assert that the messages property is not null */
        assertNotNull("Message should not be null in handleEvent()",
                      message);
        /* assert that the messages of syncronous type are not to many */
        assertTrue("to many synchronous messages",
                   synchMessages < numSyncMessages + 1);
        /* assert that the messsage of the asyncronous type are not to many */
        assertTrue("to many asynchronous messages",
                   asynchMessages < numAsyncMessages + 1);
      } catch (RuntimeException e) {
        error = e;
        throw e;
      } catch (Throwable e) {
        error = e;
      }

    }
  }
}
