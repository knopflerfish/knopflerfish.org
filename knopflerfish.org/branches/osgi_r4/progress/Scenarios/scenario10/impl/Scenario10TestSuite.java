/*
 * @(#)Scenario10TestSuite.java        1.0 2005/06/28
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
package com.gstm.test.eventadmin.scenarios.scenario10.impl;

import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class will assert that the event admin delivers bundle events 
 * it will create an event generator which installs a bundle and performs
 * several actions on it, i.e, INSTALL,START,STOP,UPDATED,UNINSTALLED
 * 
 * @author Magnus Klack
 *
 */
public class Scenario10TestSuite extends TestSuite {
    /** class variable holding the bundle context */
    private BundleContext bundleContext;

    /** variable indicating that consumers are present */
    private boolean consumerIsPresent = false;

    /** variable representing a sempahore for the consumer is present variable */
    public Object semaphore = new Object();

    EventGenerator eventGenerator;

    /**
     * Constructor for the Scenario9 TestSuite
     * 
     * @param context the bundlecontext, i.e, the handle
     *         to the framework
     */
    public Scenario10TestSuite(BundleContext context) {
        /* call super class */
        super("Scenario 10");
        /* assign the bundle context to local variable */
        bundleContext = context;
        /* create a topic string array */
        String[] topics = { "org/osgi/framework/BundleEvent/*" };

        /* add the setup */
        addTest(new Setup());
        /* add the event consumer */
        addTest(new EventConsumer(topics, "Scenario 10 Consumer", 1));
        /*  add the event generator */
        addTest( new EventGenerator(bundleContext));
        /* add the clean up */
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

    private class EventGenerator extends TestCase {
        /** local variable holding the bundleContext */
        private BundleContext bundleContext;

        /** the dummy bundle */
        private Bundle bundle;

        /**
         * Consturctor to initialize a dummy bundle 
         * @param context
         */
        public EventGenerator(BundleContext context) {
            super("Event generator");
            /* assign the context */
            bundleContext = context;
        }

        public void runTest() throws Throwable {

            try {
                /* install the bundle **/
                bundle = bundleContext
                        .installBundle("file:///opt/OSGi/knopflerfish_osgi_1.4.0/osgi/jars/device/device_all-1.0.0.jar");
            } catch (Exception e) {
                fail("Can't install bundle:" + e.getMessage());
            }

            /* must be conumers */
            Thread surveyThread = new Thread() {
                public void run() {
                    try {
                        ServiceReference[] references = null;
                        while (references == null) {
                            /* wait until consumers are present */
                            references = bundleContext
                                    .getServiceReferences(
                                            "org.osgi.service.event.EventHandler",
                                            null);
                        }

                        /* lock the semaphore */
                        synchronized (semaphore) {
                            /* set the consumer as present */
                            consumerIsPresent = true;
                        }

                    } catch (InvalidSyntaxException e) {
                        System.out
                                .println("\n************** FATAL ERROR IN SCENARIO 10 ***************\n");

                    }
                }
            };
            /* start wait process */
            surveyThread.start();

            /* wait for the semaphore to be true */
            boolean waiting = true;
            while (waiting) {
                synchronized (semaphore) {
                    waiting = consumerIsPresent;
                }

            }

            /* start the bundle */
            bundle.start();
            /* stop the bundle */
            bundle.stop();
            /* update the bundle */
            bundle.update();
            /* uninstall the bundle */
            bundle.uninstall();
            
            
            
        }

 

    }

    /**
     * Class consumes events
     * 
     * @author Magnus Klack
     */
    private class EventConsumer extends TestCase implements EventHandler {
        /** variable holding the topics to consume */
        private String[] topicsToConsume;

        /** variable holding the instance name */
        private String displayName;

        /** variable counting  events */
        private int eventCounter = 0;

        public EventConsumer(String[] topics, String name, int id) {
            /* call the super class */
            super(name + " " + id);
            /* create a display name */
            displayName = name + ":" + id;
            /* assign the topicsToConsume variable */
            topicsToConsume = topics;
        }

        public void runTest() throws Throwable {
            /* create the hashtable to put properties in */
            Hashtable props = new Hashtable();
            /* put service.pid property in hashtable */
            props.put(EventConstants.EVENT_TOPIC, topicsToConsume);
            /* register the service */
            Object service = bundleContext.registerService(EventHandler.class
                    .getName(), this, props);
            /* assert not null */
            assertNotNull(displayName + " Can't get service", service);
        }

        public void handleEvent(Event event) {
            /*
             * use the case statement to determine that the right event has arrived 
             * if not an assertment error will occurr.
             */
            switch (eventCounter) {
            case 0:
                System.out.println(displayName + " receviced topic:"
                        + event.getTopic());
                
                /* Assert */
                assertEquals("Should have an event with topic "
                        + " org/osgi/framework/BundleEvent/INSTALLED",
                        (String) event.getTopic(),
                        "org/osgi/framework/BundleEvent/INSTALLED");
               
                /* increase event counter */
                eventCounter++;

                break;
            case 1:
                System.out.println(displayName + " receviced topic:"
                        + event.getTopic());
                
                /* Assert */
                assertEquals("Should have an event with topic "
                        + " org/osgi/framework/BundleEvent/S", (String) event
                        .getTopic(), "org/osgi/framework/BundleEvent/STARTED");
                
                /* increase event counter */
                eventCounter++;
                break;
            case 2:
                System.out.println(displayName + " receviced topic:"
                        + event.getTopic());
                
                /* Assert */
                assertEquals("Should have an event with topic "
                        + " org/osgi/framework/BundleEvent/STOPPED",
                        (String) event.getTopic(),
                        "org/osgi/framework/BundleEvent/STOPPED");
               
                /* increase event counter */
                eventCounter++;
                break;
                
            case 3:
                System.out.println(displayName + " receviced topic:"
                        + event.getTopic());
                
                /* Assert */
                assertEquals("Should have an event with topic "
                        + " org/osgi/framework/BundleEvent/UPDATED",
                        (String) event.getTopic(),
                        "org/osgi/framework/BundleEvent/UPDATED");
               
                /* increase event counter */
                eventCounter++;
                break;
                
            case 4:
                System.out.println(displayName + " receviced topic:"
                        + event.getTopic());
                
                /* Assert */
                assertEquals("Should have an event with topic "
                        + " org/osgi/framework/BundleEvent/UNINSTALLED",
                        (String) event.getTopic(),
                        "org/osgi/framework/BundleEvent/UNINSTALLED");
               
                /* increase event counter */
                eventCounter++;
                break;
                
                
            default:
                /* this should not happen */
                System.out.println(displayName + " receviced topic:"
                        + event.getTopic());
               
                /* register a fail */
                fail("Order not granted in event admin service");

            }

        }

    }
}
