/*
 * @(#)Scenario6TestSuite.java        1.0 2005/06/28
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
package com.gstm.test.eventadmin.scenarios.scenario6.impl;

import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;


import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.eventadmin.scenarios.scenario6.Scenario6;

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

    /** the first consumer */
    private EventConsumer consumer1;

    /** the second consumer */
    private EventConsumer consumer2;

    /** the third consumer */
    private EventConsumer consumer3;

    /** the fourth consumer */
    private EventConsumer consumer4;

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

        /* create the first consumer */
        consumer1 = new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 1);
        /* create the second consumer */
        consumer2 = new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 2);
        /* create the third consumer */
        consumer3 = new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 3);
        /* create the third consumer */
        consumer4 = new EventConsumer(bundleContext, topics, "Scenario 6 Consumer", 4);

        /* add set up to the testsuite */
        addTest(new Setup());
        /* add the monitor */
        addTest(new Monitor());
        /* add the first consumer */
        addTest(consumer1);
        /* add the second consumer */
        addTest(consumer2);
        /* add the third consumer */
        addTest(consumer3);
        /* add the fourth consumer */
        addTest(consumer4);
        /* add the cleanup class */
        addTest(new Cleanup());

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
            consumer1.register();
            /* registe consumer 2 */
            consumer2.register();
        
            /*
             * set this will tell the publisher to save the last published
             * message
             */
            shouldRegister = true;
            /* start to publish synchronus */
            eventPublisher.startSendSynchronus();
            /* print that publication is started */
            System.out
                    .println("\n ************* Starting to publish synchronus ************ \n");
            
            
            /* lock the object */
            synchronized (this) {
                /* wait a few msec */
                wait(2000);
            }

            /* tell the publisher to stop register */
            synchronized(dummySemaphore){
                shouldRegister = false;
            }

            /* register the service */
            consumer3.register(true, ASSERT_SYNCHRONUS);
            /* register a third consumer */
            System.out
                    .println("\n\n ************** Register the third consumer ************\n\n");

            /* lock this object */
            synchronized (this) {
                /* wait */
                wait(50);
            }

            /* lock the publisher */
            synchronized (eventPublisher) {
                eventPublisher.stopSend();
            }

            System.out
                    .println("\n ************* Starting to publish asynchronus ************ \n");

            /*
             * this will tell the publisher to save the last sent asyncrhonus
             * message
             */
            synchronized(dummySemaphore){
                shouldRegister = true;
            }
            /* this will start the publication */
            eventPublisher.startSendAsynchronus();

            /* lock this object */
            synchronized (this) {
                /* wait */
                wait(10);

            }

            /* tell the publisher to stop register */
            synchronized(dummySemaphore){
                shouldRegister = false;
            }

            /* register the fourth consumer */
            consumer4.register(true, ASSERT_ASYNCHRONUS);
            /* print that the comsumer is registered */
            System.out
                    .println("\n\n *************** Register the fourth consumer ****************\n\n");

            /* lock this object */
            synchronized (this) {
                wait(10);
            }

            /* stop the publisher */
            eventPublisher.stopSend();

            /* print that the test is done */
            System.out
                    .println("\n****************** All messages sent test done ***************'\n");
            
            
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
                        Hashtable message = new Hashtable();
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

                        /* increase the variable */
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
                        Hashtable message = new Hashtable();
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
                        /* increase the variable */
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
        
        /**
         * Used to run the test  
         */
        public void runTest() throws Throwable {
            /* do nothing here */
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
            Hashtable props = new Hashtable();
            /* put service.pid property in hashtable */
            props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
            /* register the service */
            Object service = bundleContext.registerService(EventHandler.class.getName(), this,
                    props);
            assertNotNull(displayName + " Can't get service",service);
            
        }

        /**
         * use this to register a consumer without assertion
         */
        public void register() {
            shouldAssert = false;
            assertType = 3;
            /* create the hashtable to put properties in */
            Hashtable props = new Hashtable();
            /* put service.pid property in hashtable */
            props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
            /* register the service */
            bundleContext.registerService(EventHandler.class.getName(), this,
                    props);

        }

        /**
         * This method takes events from the event admin service.
         */
        public void handleEvent(Event event) {
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
                     boolean expected = (aquiredNumber.intValue() >=lastPublished+1);
                    
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
                         boolean expected = (aquiredNumber.intValue()>=lastPublished+1);
                         
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
