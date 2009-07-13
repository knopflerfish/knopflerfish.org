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
package org.knopflerfish.bundle.eventadmin_test.scenario13.impl;

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
import org.osgi.service.event.TopicPermission;

import org.knopflerfish.bundle.eventadmin_test.scenario13.Scenario13;

/**
 * Test sute for testing the requirements specified in the test
 * specification for the EventAdmin service. It will ensure that the
 * events arrive and that the EventAdmin service do not crash if
 * something goes wrong in the method handleEvents()
 *
 *
 * @author Magnus Klack
 *
 */
public class Scenario13TestSuite extends TestSuite implements Scenario13 {
  /** bundle context variable */
  private BundleContext bundleContext;
  /** the messages to be deliverd */
  private final int MESSAGES_SENT=10;

  /**
   * Constuructor for the TestSuite class.
   *
   * @param context
   *            the handle to the frame work
   */
  public Scenario13TestSuite(BundleContext context) {
    super("Scenario 13");
    /* assign the bundelContext variable */
    bundleContext = context;
    /* call the setup to init the right state */

    String[] scenario13_topics = { "com/acme/timer" };
    /* add the setup */
    addTest(new Setup());
    /* add the event consumer to the test suite */
    EventConsumer[] eventConsumer = new EventConsumer[] {
      new EventConsumer(bundleContext, scenario13_topics,
                        "Scenario 13 EventConsumer", 1),
      new EventConsumer(bundleContext, scenario13_topics,
                        "Scenario 13 EventConsumer", 2) };
    addTest(eventConsumer[0]);

    /* add the second event consumer to the test suite */
    addTest(eventConsumer[1]);

    /* add the event publisher to the test suite */
    addTest(new EventPublisher(bundleContext, "Scenario 1 EventPublisher",
                               1, MESSAGES_SENT));

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

  /**
   * Class publish events
   *
   * @author Magnus Klack
   */
  class EventPublisher extends TestCase {

    /** A reference to a service */
    private ServiceReference serviceReference;

    /** The admin which delivers the events */
    private EventAdmin eventAdmin;

    /** class variable holding bundle context */
    private BundleContext bundleContext;

    /** variable holding messages to send */
    private int messageTosend;

    public EventPublisher(BundleContext context, String name, int id,
                          int numOfMessage) {
      /* call super class */
      super(name + ":" + id);
      /* assign number of messages */
      messageTosend = numOfMessage;
      /* assign bundleContext */
      bundleContext = context;
    }

    public void runTest() throws Throwable {
      /* Claims the reference of the EventAdmin Service */
      serviceReference = bundleContext
        .getServiceReference(EventAdmin.class.getName());

      /* assert that a reference is aquired */
      assertNotNull(getName()
                    + " Should be able to get reference to EventAdmin service",
                    serviceReference);
      /* check the service reference */
      if (serviceReference == null) {
        /* set fail */
        fail(getName() + " service reference should not be null");
      }

      /* get the service  */
      eventAdmin = (EventAdmin) bundleContext
        .getService(serviceReference);

      /* assert that service is available */
      assertNotNull(getName()
                    + " Should be able to get instance to EventAdmin object",eventAdmin);

      /* check if null */
      if (eventAdmin == null) {
        /* set a fail */
        fail(getName() + " event admin should not be null");
      }

      /* create an anonymous thread */
      Thread synchDeliver = new Thread() {
          public void run() {
            /* deliver the messages */
            for (int i = 0; i < messageTosend; i++) {
              /* a Hash table to store message in */
              Dictionary message = new Hashtable();
              /* put some properties into the messages */
              message.put("Synchronus message",new Integer(i));
              /* send the message */
              eventAdmin.sendEvent(new Event("com/acme/timer", message));

            }
          }


        };

      /* print that the test has started */
      System.out.println("Testing synchronus delivery");
      /* start the thread */
      synchDeliver.start();

      /* wait until thread is dead */
      synchDeliver.join();
      /* unget the service */
      bundleContext.ungetService(serviceReference);
    }
  }

  /**
   * Class consumes events
   *
   * @author magnus
   */
  class EventConsumer extends TestCase implements EventHandler {
    /** class variable for service registration */
    private ServiceRegistration serviceRegistration;

    /** class variable indicating the topics */
    private String[] topicsToConsume;

    /** class variable keeping number of asynchronus message */
    private int numOfasynchMessages=0;

    /** class variable keeping number of asynchronus message */
    private int numOfsynchMessages=0;

    /** class variable holding the old syncronus message nummber */
    private int synchMessageExpectedNumber=0;

    /** class variable holding the old asyncronus message nummber */
    private int asynchMessageExpectedNumber=0;

    /** variable indicates the id of the consumer */
    private int numericId;

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
      /* assign the consume topics */
      topicsToConsume = topics;
      /* assign the id */
      numericId = id;

    }

    /**
     * run the test
     */
    public void runTest() throws Throwable {
      /* create the hashtable to put properties in */
      Dictionary props = new Hashtable();
      /* put service.pid property in hashtable */
      props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
      /* register the service */
      serviceRegistration = bundleContext.registerService
        (EventHandler.class.getName(), this, props);

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

    public void reset(){
      numOfasynchMessages=0;
      numOfsynchMessages=0;
      synchMessageExpectedNumber=0;
      asynchMessageExpectedNumber=0;
    }


    /**
     * This method takes events from the event admin service.
     */
    public void handleEvent(Event event) {
      if(numericId==2){
        /* try to crash the consumer */
        Long eventId = (Long)event.getProperty("Synchronus message");
      }else{
        try {
          /* normal phase */
          Integer eventId = (Integer)event.getProperty("Synchronus message");
          /* print that we received the message */
          System.out.println(this.getName() +" recevived an event:" +eventId );
        } catch (RuntimeException e) {
          error = e;
          throw e;
        } catch (Throwable e) {
          error = e;
        }

      }



    }

  }

}
