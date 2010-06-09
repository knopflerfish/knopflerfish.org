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
package org.knopflerfish.bundle.eventadmin_test.scenario2.impl;

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
import org.osgi.service.event.TopicPermission;

import org.knopflerfish.bundle.eventadmin_test.scenario2.Scenario2;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite for testing the requirements specified in the test specification
 * for the EventAdmin service.
 *
 * Check the Topic sorting of events, both synchronous and asynchronous.
 *
 * @author Martin Berg
 *
 */
public class Scenario2TestSuite extends TestSuite implements Scenario2 {
  /** bundle context variable */
  BundleContext bundleContext;

  /**
   * Constructor for the TestSuite class.
   *
   * @param context
   *            the handle to the frame work
   */
  public Scenario2TestSuite(BundleContext context) {
    super("Scenario 2");
    /* assign the bundelContext variable */
    bundleContext = context;
    /* create a topic string */
    String[] scenario2_topics1 = { "com/acme/timer" };
    String[] scenario2_topics2 = { "*" };

    String[] scenario2_topicsToPublish =  {
      "",
      "cOM/AcMe/TiMeR",
      "com.acme.timer",
      "com/acme/timer"
    };

    /* add the setup */
    addTest(new Setup());
    /* add the event consumers to the test suite */
    EventConsumer[] eventConsumer = new EventConsumer[] {
      new EventConsumer(bundleContext, scenario2_topics1,
                        1,1, "Scenario 2 EventConsumer1", 2),
      new EventConsumer(bundleContext, scenario2_topics2,
                        2,2, "Scenario 2 EventConsumer2", 2) };
    addTest(eventConsumer[1]);
    addTest(eventConsumer[0]);
    /* add the event publisher to the test suite */
    addTest(new EventPublisher(bundleContext, "Scenario 2 EventPublisher",
                               scenario2_topicsToPublish, 2, 4));
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

    /** variable holding the topic to send */
    private String[] topicsToSend;

    public EventPublisher(BundleContext context, String name, String[] topics,
                          int id, int numOfMessage) {
      /* call super class */
      super(name + ":" + id);
      /* assign number of messages */
      messageTosend = numOfMessage;
      /* assign bundleContext */
      bundleContext = context;
      /* assign topicsToSend */
      topicsToSend = topics;
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

      Thread synchDeliver = new Thread() {
          public void run() {
            for (int i = 0; i < messageTosend; i++) {
              try{
                /* a Hash table to store message in */
                Dictionary message = new Hashtable();
                /* put some properties into the messages */
                message.put("Synchronus message",new Integer(i));
                /* send the message */
                System.out.println(getName() + " sending a synchronus event with message:" +
                                   message.toString() + "and the topic:" + topicsToSend[i]);
                eventAdmin.sendEvent(new Event(topicsToSend[i], message));
              }catch(Exception e){
                System.out.println("Error when publishing synhronous" + e);
              }
            }
          }
        };
      synchDeliver.start();
      /* wait until thread is dead */
      synchDeliver.join();

      Thread asynchDeliver = new Thread() {
          public void run() {
            for (int i = 0; i < messageTosend; i++) {
              try{
                /* create the hasht table */
                Dictionary message = new Hashtable();
                /* create the message */
                message.put("Asynchronus message",new Integer(i));
                /* Sends a synchronous event to the admin */
                System.out.println(getName() + " sending an Asynchronus event with message:" +
                                   message.toString() + "and the topic:" + topicsToSend[i]);
                eventAdmin.postEvent(new Event(topicsToSend[i], message));
              }catch(Exception e){
                System.out.println("Error when publishing asynhronous:" +e );
              }
            }
          }
        };
      asynchDeliver.start();
      /* wait until thread is dead */
      asynchDeliver.join();

      bundleContext.ungetService(serviceReference);

      try {
        Thread.sleep(500); // allow for delivery
      } catch (Exception ignore) {}
    }
  }

  class EventConsumer extends TestCase implements EventHandler {
    /** class variable for service registration */
    private ServiceRegistration serviceRegistration;

    /** class variable indicatinghthe instance name */
    private int instanceId;

    /** class variable indicating the topics */
    private String[] topicsToConsume;

    /** class variable keeping number of asynchronus message */
    private int asynchMessages=0;

    /** class variable keeping number of asynchronus message */
    private int synchMessages=0;

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
      asynchMessages=0;
      synchMessages=0;
      /* create the hashtable to put properties in */
      Dictionary props = new Hashtable();
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

        /* get the topic from the event*/
        String eventTopic = event.getTopic();
        /* make a topic permission from the received topic in order to check it*/
        TopicPermission permissionAccuired = new TopicPermission(eventTopic, "SUBSCRIBE");
        /* make a topic permission from the topic to consume in order to check it*/
        TopicPermission actualPermission = new TopicPermission(topicsToConsume[0], "SUBSCRIBE");
        /* assert if the topic in the event is the same as the topic to listen fore including wildcard */
        assertTrue("The topics was not equal", actualPermission.implies(permissionAccuired));

        Object message;
        /* try to get the message */
        message = event.getProperty("Synchronus message");

        if(message != null){
          /* its an asyncronous message */
          System.out.println(getName() + " recived an Synchronus event with message:" + message.toString());
          /* assert that the messages of syncronous type are not to many */
          assertTrue("to many synchronous messages", synchMessages < numSyncMessages);
          synchMessages++;
          System.out.println("Max number of Sync messages is:"+numSyncMessages+"and number of received Sync messages is:"+ synchMessages);
        }else{
          message = event.getProperty("Asynchronus message");
          if(message!=null){
            System.out.println(getName() + " recived an Asynchronus event with message:" + message.toString());
            /* assert that the messsage of the asyncronous type are not to many */
            assertTrue("to many asynchronous messages", asynchMessages < numAsyncMessages);
            asynchMessages++;
            System.out.println("Max number of Async messages is:"+numAsyncMessages+"and number of received Async messages is:"+ asynchMessages);
          }
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
