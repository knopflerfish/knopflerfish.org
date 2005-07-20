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
package com.gstm.test.eventadmin.scenarios.scenario4.impl;

import java.util.Calendar;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.eventadmin.scenarios.scenario4.Scenario4;

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
        String [] keysAndProps3 = {"YEAR", "2005", "month", "11"};

        /*Topics to be used in the EventConsumers*/
        String[] scenario4_topics1 = { "com/acme/timer" };
        /*Filters to be used in the EventConsumers*/
        String scenario4_filter1 = "(year=2004)";
        String scenario4_filter2 = "(year=2005)";
        String scenario4_filter3 = "(year:2004)";
        String scenario4_filter5 = "(month=12)";
        
        /* add the setup */
        addTest(new Setup());
        /* add the event consumers to the test suite */
        addTest(new EventConsumer(bundleContext, scenario4_topics1, 
        		1, 1, scenario4_filter1, "Scenario 4 EventConsumer1", 4));
        addTest(new EventConsumer(bundleContext, scenario4_topics1, 
        		1, 1, scenario4_filter2, "Scenario 4 EventConsumer2", 4));
        addTest(new EventConsumer(bundleContext, scenario4_topics1, 
        		0, 0, scenario4_filter3, "Scenario 4 EventConsumer3", 4));
        addTest(new EventConsumer(bundleContext, scenario4_topics1, 
        		3, 3, null, "Scenario 4 EventConsumer4", 4));
        addTest(new EventConsumer(bundleContext, scenario4_topics1, 
        		2, 2, scenario4_filter5, "Scenario 4 EventConsumer5", 4));
        
        /* add the event publisher to the test suite */
        addTest(new EventPublisher(bundleContext, "Scenario 4 EventPublisher1", 
        		"com/acme/timer", keysAndProps1, 4, 1));
        addTest(new EventPublisher(bundleContext, "Scenario 4 EventPublisher2",
        		"com/acme/timer", keysAndProps2, 4, 1));
        addTest(new EventPublisher(bundleContext, "Scenario 4 EventPublisher3",
        		"com/acme/timer", keysAndProps3, 4, 1));
        /* add the cleanup class */
        addTest(new Cleanup());
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

            Thread synchDeliver = new Thread() {
                public void run() {
                    /* a Hash table to store message in */
                	Hashtable message = new Hashtable();
                	for(int j = 0; j < propertiesToSend.length; )
                	{
                		/*fill the propstable*/
                		System.out.println("Adding the following to the propsTable:" + propertiesToSend[j] + " and " + propertiesToSend[j+1]);
                		message.put(propertiesToSend[j], propertiesToSend[j+1]);
                		j = j+2;	
                	}
                	
                    for (int i = 0; i < messageTosend; i++) {
                        message.put("Synchronus message",new Integer(i));
                        /* test print out */
                        System.out.println(getName() + " sending a Synchronus event with message:" + 
                        		message.toString() + "and the topic:" + topicToSend);
                        /* send the message */
                        eventAdmin.sendEvent(new Event(topicToSend, message));
                    }
                }
            };

            synchDeliver.start();
            synchDeliver.join();

            Thread asynchDeliver = new Thread() {
                public void run() {
                    /* a Hash table to store message in */
                	Hashtable message = new Hashtable();
                	for(int j = 0; j < propertiesToSend.length; )
                	{
                		/*fill the propstable*/
                		System.out.println("Adding the following to the propsTable:" + propertiesToSend[j] + " and " + propertiesToSend[j+1]);
                		message.put(propertiesToSend[j], propertiesToSend[j+1]);
                		j = j+2;	
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
            };
            asynchDeliver.start();
            asynchDeliver.join();

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
            /* create the hashtable to put properties in */
            Hashtable props = new Hashtable();
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

        /**
         * This method takes events from the event admin service.
         */
        public void handleEvent(Event event) {
            Object message;
            /* try to get the message */
            message = event.getProperty("Synchronus message");
            
            Object filter1 = event.getProperty("year");
            Object filter2 = event.getProperty("month");
            String eventTopic = event.getTopic();
            if(message != null){
                /* its an asyncronous message */
                synchMessages++;
                
                System.out.println(getName() + " recived an Synchronus event with message:" + 
                		message.toString() + ", topic:"+ eventTopic + ", property_year:" + 
						filter1 + ", property_month:" + filter2 + " number of sync messages received:" + synchMessages);
                
            }else {
              message = event.getProperty("Asynchronus message");
              if(message!=null){
                  asynchMessages++;
                  System.out.println(getName() + " recived an Asynchronus event with message:" + 
                		message.toString() + ", topic:"+ eventTopic + ", property_year:" + 
						filter1 + ", property_month:" + filter2 + " number of async messages received:" + asynchMessages);
              }
              else{
              	unidentMessages++;
              	System.out.println(getName() + " recived an Unidentified event with message:" + 
                		message.toString() + ", topic:"+ eventTopic + ", property_year:" + 
						filter1 + ", property_month:" + filter2 + " number of unidentified messages received:" + unidentMessages);
              }
            }
            
            /* assert that the messages property is not null */
            assertNotNull("Message should not be null in handleEvent()",message);
            /* assert that the messages of syncronous type are not to many */
            assertTrue("to many synchronous messages in:" + getName(), synchMessages<numSyncMessages+1);
            /* assert that the messsage of the asyncronous type are not to many */
            assertTrue("to many asynchronous messages in:" + getName(), asynchMessages<numAsyncMessages+1);
        }
    }
}
