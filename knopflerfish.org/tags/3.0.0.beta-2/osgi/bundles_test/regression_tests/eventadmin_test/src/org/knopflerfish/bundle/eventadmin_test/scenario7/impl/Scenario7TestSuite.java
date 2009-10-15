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
package org.knopflerfish.bundle.eventadmin_test.scenario7.impl;

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

import org.knopflerfish.bundle.eventadmin_test.scenario7.Scenario7;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite for testing the requirements specified in the test specification
 * for the EventAdmin service.
 *
 * Check the blacklist functionality in the EventAdmin.
 * This is done with one publisher that produces an event with the topic
 * "com/acme/timer/" every 10 second and two consumers that listens for this
 * topic. One consumer that stalls the handling of the event and therefor
 * should be blacklisted and one consumer that works correctly.
 *
 * @author Martin Berg
 *
 */
public class Scenario7TestSuite extends TestSuite implements Scenario7 {
  /** bundle context variable */
  BundleContext bundleContext;

  /**
   * Constructor for the TestSuite class.
   *
   * @param context
   *            the handle to the frame work
   */
  public Scenario7TestSuite(BundleContext context) {
    super("Scenario 7");
    /* assign the bundelContext variable */
    bundleContext = context;
    /* create a topic string */
    String[] scenario7_topics1 = { "com/acme/timer" };

    /* add the setup */
    addTest(new Setup());

    /* add the event consumers to the test suite */
    EventConsumer eventConsumer = new EventConsumer(bundleContext, scenario7_topics1,
                                                    "Scenario 7 EventConsumer1", 7);
    EventConsumer2 eventConsumer2 = new EventConsumer2(bundleContext, scenario7_topics1,
                                                       "Scenario 7 EventConsumer2", 7);
    addTest(eventConsumer);
    addTest(eventConsumer2);

    /* add the event publisher to the test suite */
    EventPublisher eventPublisher = new EventPublisher(bundleContext, "Scenario 7 EventPublisher",
                                                       7, "com/acme/timer");
    addTest(eventPublisher);
    /* add the cleanup class */
    addTest(new Cleanup(eventConsumer, eventConsumer2, eventPublisher));
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
    private EventConsumer eventConsumer;
    private EventConsumer2 eventConsumer2;
    private EventPublisher eventPublisher;

    public Cleanup(EventConsumer eventConsumer, EventConsumer2 eventConsumer2, EventPublisher eventPublisher) {
      this.eventConsumer = eventConsumer;
      this.eventConsumer2 = eventConsumer2;
      this.eventPublisher = eventPublisher;
    }
    public void runTest() throws Throwable {
      eventConsumer.cleanup();
      eventConsumer2.cleanup();
      eventPublisher.cleanup();
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

    /** variable holding the topic to use */
    private String topicToSend;

    private Thread synchDeliver;
    private Thread asynchDeliver;
    private boolean stopped;

    public EventPublisher(BundleContext context, String name, int id,
                          String topic) {
      /* call super class */
      super(name + ":" + id);
      /* assign bundleContext */
      bundleContext = context;
      /* assign topic */
      topicToSend = topic;
      /* assign localCopy */
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

      stopped = false;

      synchDeliver = new Thread() {
          public void run() {
            int i = 0;
            while (!stopped && !Thread.interrupted()) {
              try {
                /* a Hash table to store message in */
                Dictionary message = new Hashtable();
                /* put some properties into the messages */
                message.put("Synchronus message", new Integer(i));
                /* send the message */
                System.out
                  .println(getName()
                           + " sending a synchronus event with message:"
                           + message.toString()
                           + "and the topic:" + topicToSend);
                eventAdmin
                  .sendEvent(new Event(topicToSend, message));
                /* Puts the thread to sleep for 10 seconds */
                i++;
                Thread.sleep(10000);
              } catch (InterruptedException e) {
                //ignored, treated by while loop
              }
            }
          }
        };

      synchDeliver.start();

      asynchDeliver = new Thread() {
          public void run() {
            int i = 0;
            while (!stopped && !Thread.interrupted()) {
              try {
                /* a Hash table to store message in */
                Dictionary message = new Hashtable();
                /* put some properties into the messages */
                message.put("Asynchronus message", new Integer(i));
                /* send the message */
                System.out
                  .println(getName()
                           + " sending a asynchronus event with message:"
                           + message.toString()
                           + "and the topic:" + topicToSend);
                eventAdmin
                  .sendEvent(new Event(topicToSend, message));
                /* Puts the thread to sleep for 10 seconds */
                i++;
                Thread.sleep(10000);
              } catch (InterruptedException e) {
                //ignored, treated by while loop
              }
            }
          }
        };
      asynchDeliver.start();

    }

    public void cleanup() {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignore) {}
      stopped = true;
    }

  }

  class EventConsumer extends TestCase implements EventHandler {
    /** class variable for service registration */
    private ServiceRegistration serviceRegistration;

    /** class variable indicating the instance name */
    private int instanceId;

    /** class variable indicating the topics correct version */
    private String[] topicsToConsume;

    /** class variable keeping number of asynchronus message */
    private int asynchMessages = 0;

    /** class variable keeping number of asynchronus message */
    private int synchMessages = 0;

    private Throwable error;

    /**
     * Constructor creates a consumer service
     *
     * @param bundleContext
     * @param topics
     */
    public EventConsumer(BundleContext bundleContext, String[] topics,
                         String name, int id) {
      /* call super class */
      super(name + ":" + id);
      /* assign the instance id */
      instanceId = id;
      /* assign the consume topics */
      topicsToConsume = topics;
    }

    public void runTest() throws Throwable {
      asynchMessages = 0;
      synchMessages = 0;
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
                             + message.toString());

        } else {
          message = event.getProperty("Asynchronus message");
          if (message != null) {
            asynchMessages++;
            System.out.println(getName()
                               + " recived an Asynchronus event with message:"
                               + message.toString());
          }
        }

        /* assert that the messages property is not null */
        assertNotNull("Message should not be null in handleEvent()",
                      message);
      } catch (RuntimeException e) {
        error = e;
        throw e;
      } catch (Throwable e) {
        error = e;
      }
    }
  }

  class EventConsumer2 extends TestCase implements EventHandler {
    /** class variable for service registration */
    private ServiceRegistration serviceRegistration;

    /** class variable indicating the instance name */
    private int instanceId;

    /** class variable indicating the topics correct version */
    private String[] topicsToConsume;

    /** class variable keeping number of asynchronus message */
    private int asynchMessages = 0;

    /** class variable keeping number of asynchronus message */
    private int synchMessages = 0;

    private Throwable error;

    /**
     * Constructor creates a consumer service
     *
     * @param bundleContext
     * @param topics
     */
    public EventConsumer2(BundleContext bundleContext, String[] topics,
                          String name, int id) {
      /* call super class */
      super(name + ":" + id);
      /* assign the instance id */
      instanceId = id;
      /* assign the consume topics */
      topicsToConsume = topics;
    }

    public void runTest() throws Throwable {
      asynchMessages = 0;
      synchMessages = 0;
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
          /* its an syncronous message */
          synchMessages++;
          System.out.println(getName()
                             + " recived an Synchronus event with message:"
                             + message.toString());
        } else {
          message = event.getProperty("Asynchronus message");
          if (message != null) {
            asynchMessages++;
            System.out.println(getName()
                               + " recived an Asynchronus event with message:"
                               + message.toString());
          }
        }

        /* assert that the messages property is not null */
        assertNotNull("Message should not be null in handleEvent()",
                      message);

        /* assert that the messsage of the asyncronous type are not to many */
        assertTrue("to many synchronous messages", synchMessages < 2);
        /* assert that the messsage of the asyncronous type are not to many */
        assertTrue("to many asynchronous messages", asynchMessages < 2);

        /* Infinit loop in order to force a blacklist on the listener from the EventAdmin */
        while (true) {
          try {
            Thread.sleep(1000);
          } catch (Throwable ignore) {}
        }
      } catch (RuntimeException e) {
        error = e;
        throw e;
      } catch (Throwable e) {
        error = e;
      }
    }
  }

}
