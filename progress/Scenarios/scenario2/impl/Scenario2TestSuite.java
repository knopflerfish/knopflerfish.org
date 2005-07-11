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
package com.gstm.test.eventadmin.scenarios.scenario2.impl;

import java.util.Calendar;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.TopicPermission;

import com.gstm.test.eventadmin.scenarios.scenario2.Scenario2;

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
        /* call the setup to init the right state */
        setUp();
    }

    public void setUp() {
        /* create a topic string */
        String[] scenario2_topics1 = { "com/acme/timer" };
        String[] scenario2_topics2 = { "*" };
        
        String[] scenario2_topicsToPublish = 	{	
													"",
													"cOM/AcMe/TiMeR",
													"com.acme.timer",
													"com/acme/timer"
												};
       
        /* add the event consumers to the test suite */
        addTest(new EventConsumer(bundleContext, scenario2_topics1,
                1,1, "Scenario 2 EventConsumer1", 2));
        addTest(new EventConsumer(bundleContext, scenario2_topics2,
                2,2, "Scenario 2 EventConsumer2", 2));
        /* add the event publisher to the test suite */
        addTest(new EventPublisher(bundleContext, "Scenario 2 EventPublisher",
        		scenario2_topicsToPublish, 2, 4));
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
                        /* a Hash table to store message in */
                        Hashtable message = new Hashtable();
                        /* put some properties into the messages */
                        message.put("Synchronus message",new Integer(i));
                        /* send the message */
                        System.out.println(getName() + " sending a synchronus event with message:" + 
                        		message.toString() + "and the topic:" + topicsToSend[i]);
                        eventAdmin.sendEvent(new Event(topicsToSend[i], message));
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
                        System.out.println(getName() + " sending an Asynchronus event with message:" + 
                        		message.toString() + "and the topic:" + topicsToSend[i]);
                        eventAdmin.postEvent(new Event(topicsToSend[i], message));
                    }
                }
            };
            //asynchDeliver.start();

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
            System.out.println("Max number of Sync messages is:"+numSyncMessages+"and number of received Sync messages is:"+ synchMessages);
            assertTrue("to many synchronous messages", synchMessages<numSyncMessages+1);
            /* assert that the messsage of the asyncronous type are not to many */
            assertTrue("to many asynchronous messages", asynchMessages<numAsyncMessages+1);

        }
    }
}
