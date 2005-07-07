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
package com.gstm.test.eventadmin.scenarios.scenario1.impl;

import java.util.Calendar;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.eventadmin.scenarios.scenario1.Scenario1;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test sute for testing the requirements specified in the test specification
 * for the EventAdmin service.
 * 
 * @author Magnus Klack
 *  
 */
public class Scenario1TestSuite extends TestSuite implements Scenario1 {
    /** bundle context variable */
    BundleContext bundleContext;

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
        setUp();
    }

    public void setUp() {
        /* create a topic string */
        String[] scenario1_topics = { "com/acme/*" };

        /* add the event consumer to the test suite */
        addTest(new EventConsumer(bundleContext, scenario1_topics,
                "Scenario 1 EventConsumer", 1));
        /* add the event publisher to the test suite */
        addTest(new EventPublisher(bundleContext, "Scenario 1 EventPublisher",
                1, 7));

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
                        Hashtable message = new Hashtable();
                        /* put some properties into the messages */
                        message.put("Synchronus message",new Integer(i));
                        /* send the message */
                        eventAdmin.sendEvent(new Event("com/acme/timer", message));

                    }
                }

              
            };

            synchDeliver.start();

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
            //asynchDeliver.start();

        }
    }

    class EventConsumer extends TestCase implements EventHandler {
        /** class variable for service registration */
        private ServiceRegistration serviceRegistration;

        /** class variable indicating the instance name */
        private int instanceId;

        /** class variable indicating the topics */
        private String[] topicsToConsume;
        
        /** class variable keeping number of asynchronus message */
        private int asynchMessages=0;
        
        /** class variable keeping number of asynchronus message */
        private int synchMessages=0;
        
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

        /**
         * This method takes events from the event admin service.
         */
        public void handleEvent(Event event) {
            //System.out.println(getName() + " recived an event");
           
            
           
            Object message;
            /* try to get the message */
            message = event.getProperty("Synchronus message");
            
            if(message != null){
                /* its an asyncronous message */
                synchMessages++;
                
                System.out.println(getName() + " recived an Synchronus event with message:" + message.toString());
                
            }else{
              message = event.getProperty("Asynchronus message");
              if(message!=null){
                  asynchMessages++;
                  System.out.println(getName() + " recived an Asynchronus event with message:" + message.toString());
              }
            }
            
            /* assert that the messages property is not null */
            assertNotNull("Message should not be null in handleEvent()",message);
            /* assert that the messages of syncronous type are not to many */
            assertTrue("to many synchronous messages", synchMessages<7);
            /* assert that the messsage of the asyncronous type are not to many */
            assertTrue("to many asynchronous messages", asynchMessages<7);

        }

    }

}
