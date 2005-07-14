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
package com.gstm.test.eventadmin.scenarios.scenario8.impl;

import java.util.Calendar;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.gstm.test.eventadmin.scenarios.scenario8.Scenario8;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * 
 * Resources needed 
 * EventConsumer1 - no ServicePermission and no TopicPermission
 * EventConsumer2 - no TopicPermission 
 * EventConsumer3 - no ServicePermission
 * EventConsumer4
 * 
 * EventProducer1 - no ServicePermission and no TopicPermission 
 * EventProducer2 - no TopicPermission 
 * EventProducer3 - no ServicePermission 
 * EventProducer4
 * 
 * @author Martin Berg
 *  
 */
public class Scenario8TestSuite extends TestSuite implements Scenario8 {
    /** bundle context variable */
    BundleContext bundleContext;
    
    Bundle buU;
    
    String test_url_base;

    /**
	 * Constructor for the TestSuite class.
	 * 
	 * @param context
	 *            the handle to the frame work
	 */
    public Scenario8TestSuite(BundleContext context) {
        super("Scenario 8");
        /* assign the bundelContext variable */
        bundleContext = context;
        
        /* add the setup */
        addTest(new Setup());
        
        /* add the external Event consumers */
        
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventConsumer1", 8, 
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventConsumer1.jar"));
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventConsumer2", 8,
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventConsumer2.jar"));
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventConsumer3", 8,
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventConsumer3.jar"));
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventConsumer4", 8,
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventConsumer4.jar"));
        
        /* ADD ONE INTERNAL EVENT PUBLISHER FOR TESTING*/
        /* create a topic string */
//        String[] scenario8_topics1 = { "com/acme/timer" };
//        addTest(new EventConsumer(bundleContext, scenario8_topics1,
//                "Scenario 7 EventConsumer1", 7));
//        addTest(new EventPublisher(bundleContext, "Scenario 8 EventPublisherT",
//                8, "com/acme/timer"));
        
        /* add the external Event Publishers */
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventProducer1", 8,
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventProducer1.jar"));
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventProducer2", 8,
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventProducer2.jar"));
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventProducer3", 8,
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventProducer3.jar"));
        addTest(new ExternalJAR(bundleContext, "Scenario 8 EventProducer4", 8,
        		"file:///C:/Program Files/eclipse-SDK-3.0.2-win32/workspace/Scenarios/src/com/gstm/test/eventadmin/scenarios/lib/EventProducer4.jar"));
        /* add the clean up */
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
    
    class ExternalJAR extends TestCase{
    	/* The bundle xontext */
    	BundleContext bundleContext;
    	/* The path to the jar file to be installed*/
    	String pathToJAR;
        public ExternalJAR(BundleContext bc, String name, int id, 
        		String path) {
            /* call super class */
            super(name + ":" + id);
            /*assign the instance name */
            
            /* assign the instance bc */
            bundleContext = bc;
            /* assign the instance path */
            pathToJAR = path;
        }
        
        public void runTest() throws Throwable {
        	/* install the bundle */
        	buU = bundleContext.installBundle(pathToJAR);
        	System.out.print("Starting:" + getName() + "\n");
        	System.out.print("The BundleID:" + buU.getBundleId() + " State:" + buU.getState() + "\n");
        	System.out.print("The BundleID:" + buU.getLocation()+ "\n");
        	System.out.print("Services in use:" + buU.getServicesInUse().toString() + "\n");
        	buU.start();
        	System.out.print("The BundleID:" + buU.getBundleId() + " State:" + buU.getState() + "\n");
        	System.out.print("Services in use:" + buU.getServicesInUse().toString() + "\n");
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

           /** variable holding the topic to use */
           private String topicToSend;
           
           public EventPublisher(BundleContext context, String name, int id,
                   String topic) {
               /* call super class */
               super(name + ":" + id);
               /* assign bundleContext */
               bundleContext = context;
               /* assign topic */
               topicToSend = topic;
               /* assign localCopy */
           }

           public void runTest() throws Throwable {
           	/* determine if to use a local copy of EventAdmin or not */
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
               		int i = 0;
                   	while ( true){ 
                   		
                   			/* a Hash table to store message in */
                               Hashtable message = new Hashtable();
                               /* put some properties into the messages */
                               message.put("Synchronus message",new Integer(i));
                               /* send the message */
                               System.out.println(getName() + " sending a synchronus event with message:" + 
                               		message.toString() + "and the topic:" + topicToSend);
                               eventAdmin.sendEvent(new Event(topicToSend, message));
                   			/* Puts the thread to sleep for 10 seconds */
                               i++;
//                               try {
//                   			//Thread.sleep(1000);
//                   		} 
//                   		catch( InterruptedException e ) {
//                   			//ignored, treated by while loop
//                   		}
                   	}
                   }
               };

               synchDeliver.start();

               Thread asynchDeliver = new Thread() {
                   public void run() {
                   	int i = 0;
                   	while ( ! Thread.interrupted()){ 
                   		try {
                   			/* a Hash table to store message in */
                               Hashtable message = new Hashtable();
                               /* put some properties into the messages */
                               message.put("Asynchronus message",new Integer(i));
                               /* send the message */
                               System.out.println(getName() + " sending a synchronus event with message:" + message.toString() + "and the topic:" + topicToSend);
                               eventAdmin.sendEvent(new Event(topicToSend, message));
                   			/* Puts the thread to sleep for 10 seconds */
                               i++;
                   			Thread.sleep(10000);
                   		} 
                   		catch( InterruptedException e ) {
                   			//ignored, treated by while loop
                   		}
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

        /** class variable indicating the topics correct version */
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
			/* determine what topicType to use */
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
        }
    }    
}
