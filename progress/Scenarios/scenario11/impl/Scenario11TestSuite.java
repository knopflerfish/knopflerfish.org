/*
 * @(#)Scenario11TestSuite.java        1.0 2005/06/28
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
package com.gstm.test.eventadmin.scenarios.scenario11.impl;

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

import com.gstm.test.eventadmin.scenarios.util.Util;

/**
 * This class will assert that the event admin delivers service events 
 * it will create an event generator which installs a bundle that registers
 * a service and performs several actions on it, i.e, 
 * REGISTERED, MODIFIED, UNREGISTERING
 * 
 * @author Magnus Klack, Martin Berg
 *
 */
public class Scenario11TestSuite extends TestSuite {
    /** class variable holding the bundle context */
    private BundleContext bundleContext;

    /** variable indicating that consumers are present */
    private boolean consumerIsPresent = false;

    /** variable representing a sempahore for the consumer is present variable */
    public Object semaphore = new Object();

    EventGenerator eventGenerator;

    /**
     * Constructor for the Scenario11 TestSuite
     * 
     * @param context the bundlecontext, i.e, the handle
     *         to the framework
     */
    public Scenario11TestSuite(BundleContext context) {
        /* call super class */
        super("Scenario 11");
        /* assign the bundle context to local variable */
        bundleContext = context;
        /* create a topic string array */
        String[] topics = { "org/osgi/framework/ServiceEvent/*" };
        /* add the setup */
        addTest(new Setup());
        /* add the event consumer */
        addTest(new EventConsumer(topics, "Scenario 11 Consumer", 1));
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
            
        	/*Wait to make sure that the consumer are pressent*/
            synchronized(this){
            	try{
            		wait(5000);
            	} catch (Exception e){
            		fail("Can't wait:" + e.getMessage());
            	}
        	
            try {            	
                /* install the bundle **/
            	bundle = Util.installBundle(bundleContext, "testlibs/TestServiceBundle.jar");
            	//assertNotNull("Install bundle is shall not be null",bundle);
            	//bundle = Util.installBundle(bundleContext, "testlibs/EventConsumer1.jar");
            } catch (Exception e) {
                fail("Can't install bundle:" + e.getMessage());
            }
            
            /* start the bundle */
            System.out.println("Starting the TestServiceBundle");
            try {
            	bundle.start();
            } catch (Exception e){
            	fail("Can't start bundle:" + e.getMessage());
            }
            /* stop the bundle */
            System.out.println("Stopping the TestServiceBundle");
            try {
            	bundle.stop();
            } catch (Exception e){
            	fail("Can't stop bundle:" + e.getMessage());
            }
        }      
    }
}

    /**
     * Class consumes events
     * 
     * @author Magnus Klack, Martin Berg
     */
    private class EventConsumer extends TestCase implements EventHandler {
        /** variable holding the topics to consume */
        private String[] topicsToConsume;

        /** variable holding the instance name */
        private String displayName;

        /** variable counting  events */
        private int eventCounter = 0;
        
        /** variable counting number of REGISTERED Events */
        private int registeredCounter = 0;

        /** variable counting number of MODIFIED Events */
        private int modifiedCounter = 0;

        /** variable counting number of UNREGISTERING Events */
        private int unregisteringCounter = 0;

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
            System.out.println(displayName + " receviced topic:"
                    + event.getTopic());
            
            /* The topics that the consumer should receive*/
        	String registered = "org/osgi/framework/ServiceEvent/REGISTERED";
        	String modified = "org/osgi/framework/ServiceEvent/MODIFIED";
        	String unregistering = "org/osgi/framework/ServiceEvent/UNREGISTERING";
        	
        	/* count the number of received messages with the topic org/osgi/framework/ServiceEvent/REGISTERED*/
			if(event.getTopic().equals(registered)){
				registeredCounter++;
				System.out.println("The number of org/osgi/framework/ServiceEvent/REGISTERED events received is:" + 
						registeredCounter);
			}

			/* count the number of received messages with the topic org/osgi/framework/ServiceEvent/MODIFIED*/
			if(event.getTopic().equals(modified)){
				modifiedCounter++;
				System.out.println("The number of org/osgi/framework/ServiceEvent/MODIFIED events received is:" + 
						modifiedCounter);
			}
			
			/* count the number of received messages with the topic org/osgi/framework/ServiceEvent/UNREGISTERING*/
			if(event.getTopic().equals(unregistering)){
				unregisteringCounter++;
				System.out.println("The number of org/osgi/framework/ServiceEvent/MODIFIED events received is:" + 
						unregisteringCounter);
			}
			
			eventCounter++;
			System.out.println("Number of received events:\t" + eventCounter + "\nNumber of received REGISTERED events:\t" + 
					registeredCounter + "\nNumber of received MODIFIED events:\t" + modifiedCounter +
					"\nNumber of received UNREGISTERING events:\t" + unregisteringCounter);
			
			/* Assert that only REGISTERED, MODIFIED and UNREGISTERED is received */
			assertTrue("Only REGISTERED, MODIFIED or UNREGISTERING events are to be received", 
					(event.getTopic().equals(registered)) || (event.getTopic().equals(modified)) || 
					(event.getTopic().equals(unregistering)));
        }
    }
}
