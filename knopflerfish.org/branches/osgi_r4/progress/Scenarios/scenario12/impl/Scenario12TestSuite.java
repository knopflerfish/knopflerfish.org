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
package com.gstm.test.eventadmin.scenarios.scenario12.impl;

import java.util.Hashtable;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.knopflerfish.service.log.LogRef;
import org.osgi.framework.BundleContext;
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
public class Scenario12TestSuite extends TestSuite {
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
    public Scenario12TestSuite(BundleContext context) {
        /* call super class */
        super("Scenario 12");
        /* assign the bundle context to local variable */
        bundleContext = context;
        /* create a topic string array */
        String[] topics = { "org/osgi/service/log/LogEntry/*" };

        /* add the setup */
        addTest(new Setup());
        /* add the event consumer */
        addTest(new EventConsumer(topics, "Scenario 12 Consumer", 1));
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
        /** the log */
        private LogRef log;

        

        /**
         * Consturctor to initialize a dummy bundle 
         * @param context
         */
        public EventGenerator(BundleContext context) {
        	/* call the super class */
            super("Event generator");
            /* assign the log */
            log = new LogRef(context);
            assertNotNull(getName()+ " can't get the log service",log);
            bundleContext=context;
        }

        public void runTest() throws Throwable {
        	Object verify=null;
        	 while(verify==null){
        		verify= bundleContext.getServiceReferences("org.osgi.service.event.EventHandler", null);
        	 
        	 } 
        	
        	
        	try{//0
        		synchronized(this){
        			wait(3000);
        		}
        		
        		log.error("Testing LOG_ERROR");
	        	//log.log(LogRef.LOG_ERROR,"Testing LOG_ERROR");
	        	synchronized(this){
	        		wait(200);
	        	}//1
	        	
	        	log.warn("Testing LOG_WARNING");
	        	synchronized(this){
	        		wait(200);
	        	}//2
	        	//log.log(LogRef.LOG_INFO,"Testing LOG_INFO");
	        	synchronized(this){
	        		wait(200);
	        	}//3
	        	log.debug("Testing LOG_DEBUG");
	        	synchronized(this){
	        		wait(200);
	        	}
	          	
        	}catch(InterruptedException e){
        		System.out.println(getName()+ " was interrupted during the test of scenario 12");
        		fail(getName()+ " was interrupted during the test of scenario 12");
        		
        	}
        	
        	
         
            
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
        	
        	 
        if(!event.getTopic().equals("org/osgi/service/log/LogEntry/LOG_INFO")){
        	
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
	                    + " org/osgi/service/log/LogEntry/LOG_ERROR got " +(String) event.getTopic() ,
	                    (String) event.getTopic(),
	                    "org/osgi/service/log/LogEntry/LOG_ERROR");
	           
	            /* increase event counter */
	            eventCounter++;
	
	            break;
	        case 1:
	            System.out.println(displayName + " receviced topic:"
	                    + event.getTopic());
	            
	            /* Assert */
	            assertEquals("Should have an event with topic "
	                    + " org/osgi/service/log/LogEntry/LOG_WARNING got "+(String) event.getTopic()
	                    , (String) event.getTopic(), "org/osgi/service/log/LogEntry/LOG_WARNING");
	            
	            /* increase event counter */
	            eventCounter++;
	            break;
	        case 2:
	            System.out.println(displayName + " receviced topic:"
	                    + event.getTopic());
	            
	            /* Assert */
	            assertEquals("Should have an event with topic "
	                    + " org/osgi/service/log/LogEntry/LOG_DEBUG got "+(String) event.getTopic(),
	                    (String) event.getTopic(),
	                    "org/osgi/service/log/LogEntry/LOG_DEBUG");
	           
	            /* increase event counter */
	            eventCounter++;
	            break;
	            
	        case 3:
	            System.out.println(displayName + " receviced topic:"
	                    + event.getTopic());
	            
	            /* Assert */
	            assertEquals("Should have an event with topic "
	                    + " org/osgi/service/log/LogEntry/LOG_OTHER got " +(String) event.getTopic(),
	                    (String) event.getTopic(),
	                    "org/osgi/service/log/LogEntry/LOG_OTHER");
	           
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
	
	    }else if(event.getTopic().equals("org/osgi/service/log/LogEntry/LOG_INFO")){
	    	System.out.println("Got LOG_INFO as expected ");
        	
        }else{
        	fail(getName()+" got unexpected event");
        	
        }
        
        }// end handleEvent(....

    }
}
