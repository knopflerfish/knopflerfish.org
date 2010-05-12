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
package org.knopflerfish.bundle.eventadmin_test.scenario6.impl;

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

import org.knopflerfish.bundle.eventadmin_test.scenario6.Scenario6;

/**
 * this test will test if the EventAdmin doesn't send events to EventConsumers
 * that is registered after the event is generated. Also test if EventConsumers
 * that are killed isn't called by the EventAdmin. The class will create a
 * publisher that will start to publish messages,in a thread, after N seconds a
 * new EventConsumer will be registered into the framework. This EventConsumer
 * should not receive any messages that was published before registration.
 * Because of the synchronized feature it might seem on the printouts like a
 * test should have a message of a given number. For example a message with
 * number 12 is before registration of the new EventConsumers the EventConsumers
 * will recive this message because it has been registered into the framework
 * yet, even if the print out says that isn't the case. The handleEvent function in
 * class EventConsumer will acceept the variable last published messages - 1
 * this is due to rendevouz synchronization matter.
 *
 *
 * @author Magnus Klack
 */
public class Scenario6TestSuite extends TestSuite implements Scenario6 {
  /** bundle context variable */
  private BundleContext bundleContext;

  /**
   * last published messages the newly registered consumer should not have
   * this message
   */
  private int lastPublishedIDSynch;

  /**
   * last published messages the newly registered consumer should not have
   * this message
   */
  private int lastPublishedIDAsynch;

  /** should register banned messages or not */
  private boolean shouldRegister = false;

  /** constant type representing what to assert */
  private final int ASSERT_SYNCHRONUS = 0;

  /** constant type representing what to assert */
  private final int ASSERT_ASYNCHRONUS = 1;

  private EventConsumer[] eventConsumer;

  /** the publisher */
  private EventPublisher eventPublisher;

  /** dummy object */
  private Object dummySemaphore = new Object();

  public Scenario6TestSuite(BundleContext context) {
    /* call superclass */
    super("Scenario 6");
    /* the handle to the framework */
    bundleContext = context;

    /* create the topics */
    String[] topics = { "com/acme/timer" };

    /* create the publisher */
    eventPublisher = new EventPublisher(bundleContext, "EventPublisher", 1,
                                        "com/acme/timer");

    eventConsumer = new EventConsumer[] {
      new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 1),
      new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 2),
      new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 3),
      new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 4) };

    /* add set up to the testsuite */
    addTest(new Setup());
    /* add the monitor */
    addTest(new Monitor());
    /* add the first consumer */
    //addTest(consumer1);
    /* add the second consumer */
    //addTest(consumer2);
    /* add the third consumer */
    //addTest(consumer3);
    /* add the fourth consumer */
    //addTest(consumer4);
    /* add the cleanup class */
    addTest(new Cleanup(eventConsumer));

  }

  /**
   * the set up class add listeners and stuff here
   *
   * @author Magnus Klack
   */
  class Setup extends TestCase {
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
   * the Clean up class
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

  /**
   * this is the controller process of the test it will coordinate the
   * diffrent threads into a correct behavior
   *
   * @author Magnus Klack
   */
  private class Monitor extends TestCase {

    /**
     * Constructor
     *
     */
    public Monitor() {
      /* call super class */
      super("Test Monitor - Timer test");

    }

    public void runTest() throws Throwable {

      /* register the consumer 1 */
      eventConsumer[0].register();
      /* registe consumer 2 */
      eventConsumer[1].register();

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /*
       * set this will tell the publisher to save the last published
       * message
       */
      shouldRegister = true;
      /* start to publish synchronus */
      eventPublisher.startSendSynchronus();
      /* print that publication is started */
      System.out.println("************* Starting to publish synchronus ************");


      /* lock the object */
      synchronized (this) {
        /* wait a few msec */
        wait(2000);
      }

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /* tell the publisher to stop register */
      synchronized(dummySemaphore){
        shouldRegister = false;
      }

      /* register the service */
      eventConsumer[2].register(true, ASSERT_SYNCHRONUS);
      /* register a third consumer */
      System.out.println("************** Register the third consumer ************");

      /* lock this object */
      synchronized (this) {
        /* wait */
        wait(50);
      }

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /* lock the publisher */
      synchronized (eventPublisher) {
        eventPublisher.stopSend();
      }

      System.out.println("************* Starting to publish asynchronus ************");

      /*
       * this will tell the publisher to save the last sent asyncrhonus
       * message
       */
      synchronized(dummySemaphore){
        shouldRegister = true;
      }

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /* this will start the publication */
      eventPublisher.startSendAsynchronus();

      /* lock this object */
      synchronized (this) {
        /* wait */
        wait(10);

      }

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /* tell the publisher to stop register */
      synchronized(dummySemaphore){
        shouldRegister = false;
      }

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /* register the fourth consumer */
      eventConsumer[3].register(true, ASSERT_ASYNCHRONUS);
      /* print that the comsumer is registered */
      System.out.println("*************** Register the fourth consumer ****************");

      /* lock this object */
      synchronized (this) {
        wait(10);
      }

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /* stop the publisher */
      eventPublisher.stopSend();

      for (int i=0; i<eventConsumer.length; i++)
        if (eventConsumer[i].getError() != null) throw eventConsumer[i].getError();

      /* print that the test is done */
      System.out.println("****************** All messages sent test done ***************");
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

  private class EventPublisher {

    /** A reference to a service */
    private ServiceReference serviceReference;

    /** The admin which delivers the events */
    private EventAdmin eventAdmin;

    /** class variable holding bundle context */
    private BundleContext bundleContext;

    /** variable holding the topic to use */
    private String topicToSend;

    /** variable indicating number of messages */
    private boolean running;

    public EventPublisher(BundleContext context, String name, int id,
                          String topic) {
      /* assign bundleContext */
      bundleContext = context;
      /* assign topic */
      topicToSend = topic;

      /* Claims the reference of the EventAdmin Service */
      serviceReference = bundleContext
        .getServiceReference(EventAdmin.class.getName());

      /* get the service */
      eventAdmin = (EventAdmin) bundleContext
        .getService(serviceReference);
    }

    /**
     * metohod start to send events synchronus until stopped
     */
    public void startSendSynchronus() {
      running = true;
      /* create a deliver thread */
      Thread thread = new Thread() {
          public void run() {
            int i = 0;
            while (running && i<500) {
              /* a Hash table to store message in */
              Dictionary message = new Hashtable();
              /* put some properties into the messages */
              message.put("Synchronus message", new Integer(i));
              /* print for the console */
              System.out.println(getName()
                                 + " sending a synchronus event with message:"
                                 + message.toString() + "and the topic:"
                                 + topicToSend);

              /* send the message */
              eventAdmin.sendEvent(new Event(topicToSend, message));

              synchronized (dummySemaphore) {
                if (shouldRegister) {
                  lastPublishedIDSynch = i;
                }
              }
              i++;
            }
          }
        };
      thread.start();
    }

    /**
     * Method stops the publisher to send synchronus or asyncrhonus
     *
     */
    public void stopSend() {
      synchronized (this) {
        running = false;
      }
    }

    /**
     * metohod start to send events synchronus until stopped
     */
    public void startSendAsynchronus() {
      running = true;
      /* create a deliver thread */
      Thread thread = new Thread() {
          public void run() {
            int i = 0;
            while (running && i<500) {
              /* a Hash table to store message in */
              Dictionary message = new Hashtable();
              /* put some properties into the messages */
              message.put("Asynchronus message", new Integer(i));
              /* print for the console */
              System.out.println(getName()
                                 + " sending a asynchronus event with message:"
                                 + message.toString() + "and the topic:"
                                 + topicToSend);
              /* send the message */
              eventAdmin.postEvent(new Event(topicToSend, message));
              synchronized (dummySemaphore) {
                if (shouldRegister) {
                  lastPublishedIDAsynch = i;
                }
              }
              i++;
            }
          }
        };
      thread.start();
    }
  }

  /**
   * Class consumes events
   *
   * @author Magnus Klack
   */
  class EventConsumer extends TestCase implements EventHandler {

    /** class variable indicating the topics */
    private String[] topicsToConsume;

    /** class variable indicating the instance name */
    private String displayName;

    /** class variable indicating if the class should assert */
    private boolean shouldAssert;

    /** this is the assert type constant */
    private int assertType;

    /** variable holding the last published */
    private int lastPublished;

    private ServiceRegistration serviceRegistration;

    private Throwable error = null;

    /**
     * Constructor for the EventConsumer class this class will listen for
     * events and notify the Monitor class if changes have been made.
     *
     * @param owner
     *            the monitor class,i.e, the class creating this instance
     * @param bundleContext
     *            the bundle context
     * @param topics
     *            topic to listen to
     * @param name
     *            the name
     * @param id
     *            the numeric id
     */
    public EventConsumer(BundleContext bundleContext, String[] topics,
                         String name, int id) {
      /* compose a name */
      displayName = name + ":" + id;
      /* assign the consume topics */
      topicsToConsume = topics;
    }

    public Throwable getError() {
      return error;
    }

    /**
     * Used to run the test
     */
    public void runTest() throws Throwable {
      /* do nothing here */
    }

    public void cleanup() {
      try {
        serviceRegistration.unregister();
      } catch (IllegalStateException ignore) {}
    }

    /**
     * use this method to register a consumer with assertion
     *
     * @param value
     *            if the consumer should assert
     * @param type
     *            what type it should assert
     */
    public void register(boolean value, int type) {

      shouldAssert = value;
      assertType = type;
      /* create the hashtable to put properties in */
      Dictionary props = new Hashtable();
      /* put service.pid property in hashtable */
      props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
      /* register the service */
      serviceRegistration = bundleContext.registerService(EventHandler.class.getName(), this,
                                                          props);
      assertNotNull(displayName + " Can't get service", serviceRegistration);

    }

    /**
     * use this to register a consumer without assertion
     */
    public void register() {
      shouldAssert = false;
      assertType = 3;
      /* create the hashtable to put properties in */
      Dictionary props = new Hashtable();
      /* put service.pid property in hashtable */
      props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
      /* register the service */
      serviceRegistration = bundleContext.registerService(EventHandler.class.getName(), this,
                                                          props);
    }

    /**
     * This method takes events from the event admin service.
     */
    public void handleEvent(Event event)  {
      try {
        /* try to get the message */
        Object message = event.getProperty("Synchronus message");

        /* check if message is null */
        if (message != null) {
          /* its an syncronous message */
          System.out.println(displayName
                             + " received an Synchronus event with message:"
                             + message.toString());

          if (shouldAssert && assertType == ASSERT_SYNCHRONUS ) {
            lastPublished = lastPublishedIDSynch;
            /* get the message number */
            Integer aquiredNumber = (Integer)message;

            assertNotNull("Aquired property should not be null",aquiredNumber);

            /*
             * assert the value use last published variable sometimes +1
             * because of rendevouz fact
             */
            boolean expected = (aquiredNumber.intValue() >=lastPublished-20);

            if(! (lastPublished-20>aquiredNumber.intValue())){
              expected = (aquiredNumber.intValue() >=lastPublished-15);
            }

            /* if not expected */
            if(!expected){
              /* this can happen sometimes beacause of rendevouz */
              expected = (aquiredNumber.intValue() ==lastPublished-1);

            }

            /* create a message string */
            String errorMessage=displayName
              + " Should not have Synchronus message:"
              + aquiredNumber
              + " first message to aquire is:"
              + lastPublished;

            assertEquals(errorMessage,expected,true);

          }

        } else {
          message = event.getProperty("Asynchronus message");
          if (message != null) {
            /* its an asyncronus message */
            System.out.println(displayName
                               + " received an Asynchronus event with message:"
                               + message.toString());

            /* check if an assertsion should be performed */
            if (shouldAssert && assertType == ASSERT_ASYNCHRONUS) {
              lastPublished = lastPublishedIDAsynch;
              /* get the message number */
              Integer aquiredNumber = (Integer)message;
              /* assert not null */
              assertNotNull("Aquired property should not be null",aquiredNumber);

              /* assert the value use last published variable */
              boolean expected = (aquiredNumber.intValue()>=lastPublished-20);

              if(! (lastPublished-20>aquiredNumber.intValue())){
                expected = (aquiredNumber.intValue() >=lastPublished-15);
              }

              /* check if expected */
              if(!expected){
                /* this can happen sometimes because of rendevouz issues */
                expected = (aquiredNumber.intValue() ==lastPublished-1);

              }

              /* the message */
              String errorMessage=displayName
                + " Should not have Asynchronus message:"
                + aquiredNumber  + " first message to aquire is:"
                + lastPublished;

              assertEquals(errorMessage,expected,true);
            }
          }
        }
      } catch (RuntimeException e) {
        error = e;
        throw e;
      } catch (Throwable e) {
        error = e;
      }

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
}
