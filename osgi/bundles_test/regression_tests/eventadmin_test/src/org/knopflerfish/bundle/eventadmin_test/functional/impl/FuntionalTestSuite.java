/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.eventadmin_test.functional.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class will stresstest the eventAdmin service
 * and assert that it works during intensive
 * conditions and periods.
 *
 * @author Magnus Klack
 */
public class FuntionalTestSuite extends TestSuite {
  /**
   * the bundle context
   */
  private BundleContext bundleContext;

  /**
   * Constructor
   *
   * @param context the bundle context of the instance creator
   */
  public FuntionalTestSuite(BundleContext context) {
    super("Functional Stress");

    bundleContext = context;

    String[] topic = {"com/acme/*"};
    addTest(new EventConsumer(topic, "EventConsumer", 1));

    String[] topic2 = {"com/acme/timer/*"};
    addTest(new EventConsumer(topic2, "EventConsumer", 2));

    String[] topic3 = {"COM/acme/LOCAL"};
    addTest(new EventConsumer(topic3, "EventConsumer", 3));

    String[] topic4 = {"*"};
    addTest(new EventConsumer(topic4, "EventConsumer", 4));

    String[] topic5 = {"com/Roland/timer"};
    addTest(new EventConsumer(topic5, "EventConsumer", 5));

    String[] topic6 = {"/com/acme/timer"};
    addTest(new EventConsumer(topic6, "EventConsumer", 6));

    String[] topic7 = {"*"};
    addTest(new EventConsumer(topic7, "EventConsumer", 7));

    EventPublisher p1 = new EventPublisher(bundleContext, "com/acme/timer", "EventPublisher", 1);
    addTest(p1);

    EventPublisher p2 = new EventPublisher(bundleContext, "com/acme/timer", "EventPublisher", 2);
    addTest(p2);

    EventPublisher p3 = new EventPublisher(bundleContext, "com/acme/timer", "EventPublisher", 3);
    addTest(p3);

    EventPublisher p4 = new EventPublisher(bundleContext, "com/Roland/timer", "EventPublisher", 4);
    addTest(p4);

    EventPublisher p5 = new EventPublisher(bundleContext, "com/acme/timer", "EventPublisher", 5);
    addTest(p5);

    EventPublisher p6 = new EventPublisher(bundleContext, "com/acme/timer/*", "EventPublisher", 6);
    addTest(p6);

    EventPublisher p7 = new EventPublisher(bundleContext, "COM/acme/LOCAL", "EventPublisher", 7);
    addTest(p7);

    EventPublisher p8 = new EventPublisher(bundleContext, "LOCAL/APACHE/MAGNUS", "EventPublisher", 8);
    addTest(p8);


    EventPublisher p9 = new EventPublisher(bundleContext, "COM/KLACK/System", "EventPublisher", 9);
    addTest(p9);

    EventPublisher p10 = new EventPublisher(bundleContext, "SYSTEM/apache", "EventPublisher", 10);
    addTest(p10);


  }

  public void runTest() throws Throwable {

  }


  /**
   * Sets up neccessary environment
   *
   * @author Magnus Klack
   */
  static class Setup extends TestCase {
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
  static class Cleanup extends TestCase {
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

  private static class EventPublisher extends TestCase {
    /**
     * The admin which delivers the events
     */
    private EventAdmin eventAdmin;

    /**
     * the private bundle context
     */
    private BundleContext bundleContext;

    private String topicToPublish;

    /**
     * the id of the publisher
     */
    private int publisherId;


    public EventPublisher(BundleContext context, String topic, String name, int id) {
      /* call super class */
      super(name + ":" + id);
      /* assign bundleContext */
      bundleContext = context;
      /* assign the topic to publish */
      topicToPublish = topic;
      /* assign the publisherID */
      publisherId = id;
    }

    public void runTest() throws Throwable {
      /* Claims the reference of the EventAdmin Service */
      ServiceReference<EventAdmin> serviceReference = bundleContext
          .getServiceReference(EventAdmin.class);

      /* assert that a reference is aquired */
      assertNotNull(getName()
              + " Should be able to get reference to EventAdmin service",
          serviceReference);

      /* get the service  */
      eventAdmin = bundleContext.getService(serviceReference);

      /* assert that service is available */
      assertNotNull(getName() + " Should be able to get instance to "
              + "EventAdmin object",
          eventAdmin);

      publish();
    }

    private void publish() {
      int i = 0;
      while (true) {
        /* a Hash table to store message in */
        Dictionary<String, Object> message = new Hashtable<>();
        /* put some properties into the messages */
        message.put("Synchronus message", i);
        /* put the sender */
        message.put("FROM", this);

        if (publisherId > 5) {
          /* send the message */
          eventAdmin.sendEvent(new Event(topicToPublish, message));
        } else {
          /* send the message */
          eventAdmin.postEvent(new Event(topicToPublish, message));
        }

        i++;
      }
    }
  }

  /**
   * Class consumes events
   *
   * @author magnus
   */
  @SuppressWarnings("UnconstructableJUnitTestCase")
  class EventConsumer extends TestCase implements EventHandler {
    /**
     * class variable indicating the topics
     */
    private String[] topicsToConsume;

    /**
     * Constructor creates a consumer service
     */
    public EventConsumer(String[] topics, String name, int id) {
      /* call super class */
      super(name + ":" + id);
      /* assign the consume topics */
      topicsToConsume = topics;
    }

    /**
     * run the test
     */
    public void runTest() throws Throwable {
      /* create the hashtable to put properties in */
      Hashtable<String, Object> props = new Hashtable<>();
      /* put service.pid property in hashtable */
      props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
      /* register the service */
      ServiceRegistration<EventHandler> serviceRegistration = bundleContext.registerService
          (EventHandler.class, this, props);

      assertNotNull(getName()
              + " service registration should not be null",
          serviceRegistration);
    }

    /**
     * This method takes events from the event admin service.
     */
    public void handleEvent(Event event) {
      System.out.println("****************************  RECEVIED "
          + "***********************************");
      String from = (String) event.getProperty("FROM");
      System.err.println(getName() + " recived an event from:" + from
          + " with topic:" + event.getTopic());
    }

  }

}
