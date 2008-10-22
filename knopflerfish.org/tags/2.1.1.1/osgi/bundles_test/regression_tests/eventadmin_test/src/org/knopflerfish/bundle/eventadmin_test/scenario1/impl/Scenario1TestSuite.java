/*
 * @(#)Scenario1TestSuite.java        1.0 2005/06/28
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
package org.knopflerfish.bundle.eventadmin_test.scenario1.impl;

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

import org.knopflerfish.bundle.eventadmin_test.scenario1.Scenario1;

/**
 * Test sute for testing the requirements specified in the test specification
 * for the EventAdmin service. It will ensure that the events arrive in time and order
 * it will even check that the wildcards according to the topics works properly.
 *
 *
 * @author Magnus Klack
 *
 */
public class Scenario1TestSuite extends TestSuite implements Scenario1 {
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
    public Scenario1TestSuite(BundleContext context) {
        super("Scenario 1");
        /* assign the bundelContext variable */
        bundleContext = context;
        /* call the setup to init the right state */

        String[] scenario1_topics = { "com/acme/*" };
        /* add the setup */
        addTest(new Setup());
        /* add the event consumer to the test suite */
        EventConsumer eventConsumer = new EventConsumer(bundleContext, scenario1_topics,
                                                        "Scenario 1 EventConsumer", 1);
        addTest(eventConsumer);
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
        private EventConsumer eventConsumer;

        public Cleanup(EventConsumer eventConsumer) {
          this.eventConsumer = eventConsumer;
        }
        public void runTest() throws Throwable {
          eventConsumer.cleanup();
System.out.println("End of Scenario 1");
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
                        Hashtable message = new Hashtable();
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

            Thread asynchDeliver = new Thread() {
                public void run() {

                    for (int i = 0; i < messageTosend; i++) {
                        /* create the hasht table */
                        Hashtable message = new Hashtable();
                        /* create the message */
                        message.put("Asynchronus message",new Integer(i));
                        /* Sends a synchronous event to the admin */
                        eventAdmin.postEvent(new Event("com/acme/timer", message));

                    }
                }


            };
            /* print that the test has started */
            System.out.println("Testing asynchronus delivery");
            /* start the test */
            asynchDeliver.start();

            /* wait until thread is dead */
            asynchDeliver.join();

            bundleContext.ungetService(serviceReference);

            try {
              Thread.sleep(500); // allow for delivery
            } catch (Exception ignore) {}
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

        }

        /**
         * run the test
         */
        public void runTest() throws Throwable {
            numOfasynchMessages=0;
            numOfsynchMessages=0;
            synchMessageExpectedNumber=0;
            asynchMessageExpectedNumber=0;

            /* create the hashtable to put properties in */
            Hashtable props = new Hashtable();
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
            assertTrue("Not all synch messages recieved", MESSAGES_SENT == synchMessageExpectedNumber);
            assertTrue("Not all asynch messages recieved", MESSAGES_SENT == asynchMessageExpectedNumber);
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
          try {
            //System.out.println(getName() + " recived an event");

            TopicPermission permissionAquired = new TopicPermission((String)event.getProperty(EventConstants.EVENT_TOPIC),"subscribe");
            TopicPermission actualPermission = new TopicPermission("com/acme/*","subscribe");

            assertTrue(getName()+"Should not recevice this topic:"+(String)event.getProperty(EventConstants.EVENT_TOPIC)
                    , actualPermission.implies(permissionAquired));


            Object message;
            /* try to get the message */
            message = event.getProperty("Synchronus message");
            /* check if message is null */
            if(message != null){
                /* its an asyncronous message */
                numOfsynchMessages++;
                /* print that a message is received */
                System.out.println(getName() + " recived an Synchronus event with message:" + message.toString());
                /* get the message number */
                int aquiredNumber= Integer.parseInt(message.toString());
                /* assert that the message is the expected one */
                assertTrue(getName()+" Expected syncronus messagenumber:" + synchMessageExpectedNumber + " got:"
                        + aquiredNumber + " order NOT granted", synchMessageExpectedNumber==aquiredNumber );

                /* the next messages of this type should be +1 */
                synchMessageExpectedNumber++;


            }else{
              message = event.getProperty("Asynchronus message");
              if(message!=null){
                  numOfasynchMessages++;
                  System.out.println(getName() + " recived an Asynchronus event with message:" + message.toString());
                  /* get the message number */
                  int aquiredNumber= Integer.parseInt(message.toString());
                  /* assert that the message is the expected one */
                  assertTrue(getName()+" Expected asyncronus messagenumber:" + asynchMessageExpectedNumber + " got:"
                          + aquiredNumber + " order NOT granted", asynchMessageExpectedNumber==aquiredNumber );

                  /* the next messages of this type should be +1 */
                  asynchMessageExpectedNumber++;

              }
            }

            /* assert that the messages property is not null */
            assertNotNull("Message should not be null in "+ getName() + " handleEvent()",message);
            /* assert that the messages of syncronous type are not to many */
            assertTrue("to many synchronous messages in "+ getName() +" handleEvent()", numOfsynchMessages<MESSAGES_SENT+1);
            /* assert that the messsage of the asyncronous type are not to many */
            assertTrue("to many asynchronous messages in "+ getName() + "handleEvent()", numOfasynchMessages<MESSAGES_SENT+1);
          } catch (RuntimeException e) {
            error = e;
            throw e;
          } catch (Throwable e) {
            error = e;
          }
        }

    }

}
